export const CATALOG_CONTENT_FIELDS = [
  "name",
  "description",
  "symptom_preview",
  "causes",
  "recommended_action",
  "prevention",
  "guidance",
  "when_to_act",
  "disclaimer",
] as const;

export type CatalogContentField = (typeof CATALOG_CONTENT_FIELDS)[number];
export type CatalogLanguage = "en" | "fil";
export type CatalogCategory = "LEAF_DISEASE" | "FRUIT_DISEASE";

export type CatalogLanguagePayload = {
  language: CatalogLanguage;
  content: Record<CatalogContentField, string>;
  signs: string[];
  reference: {
    publisher: string;
    title: string;
    url: string;
  };
};

export type CreateDiseaseCatalogPayload = {
  diseaseId: string;
  category: CatalogCategory;
  artwork: File;
  english: CatalogLanguagePayload;
  filipino: CatalogLanguagePayload;
};

export const MAX_ARTWORK_BYTES = 5 * 1024 * 1024;

export function parseCreateDiseaseFormData(formData: FormData): CreateDiseaseCatalogPayload {
  const diseaseId = text(formData, "disease_id").toLowerCase();
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(diseaseId) || diseaseId.length > 80) {
    throw new Error("Use a lowercase disease ID with letters, numbers, and hyphens only.");
  }

  const category = text(formData, "category");
  if (category !== "LEAF_DISEASE" && category !== "FRUIT_DISEASE") {
    throw new Error("Choose a valid disease category.");
  }

  const artwork = formData.get("artwork");
  if (!(artwork instanceof File) || artwork.size === 0) {
    throw new Error("Upload a disease artwork image.");
  }
  if (artwork.type.toLowerCase() !== "image/jpeg" || artwork.size > MAX_ARTWORK_BYTES) {
    throw new Error("Artwork must be a JPEG image no larger than 5 MB.");
  }

  return {
    diseaseId,
    category,
    artwork,
    english: parseLanguage(formData, "en"),
    filipino: parseLanguage(formData, "fil"),
  };
}

export function validateJpegBytes(bytes: Uint8Array): boolean {
  return bytes.length >= 4
    && bytes.length <= MAX_ARTWORK_BYTES
    && bytes[0] === 0xff
    && bytes[1] === 0xd8
    && bytes[2] === 0xff
    && bytes[bytes.length - 2] === 0xff
    && bytes[bytes.length - 1] === 0xd9;
}

export function isSafeReference(reference: { publisher: string; title: string; url: string }) {
  if (
    !reference.publisher
    || reference.publisher.length > 500
    || !reference.title
    || reference.title.length > 500
    || reference.url.length > 2_048
  ) return false;
  try {
    const url = new URL(reference.url);
    return url.protocol === "https:" && !url.username && !url.password && Boolean(url.hostname);
  } catch {
    return false;
  }
}

function parseLanguage(formData: FormData, language: CatalogLanguage): CatalogLanguagePayload {
  const content = Object.fromEntries(
    CATALOG_CONTENT_FIELDS.map((field) => [field, text(formData, `${language}_${field}`, 4_000)]),
  ) as Record<CatalogContentField, string>;
  if (Object.values(content).some((value) => value.length < 2)) {
    throw new Error(`Complete all ${language === "en" ? "English" : "Filipino"} content fields.`);
  }

  const signs = text(formData, `${language}_signs`, 10_019)
    .split(/\r?\n/)
    .map((sign) => sign.trim())
    .filter(Boolean);
  if (signs.length < 1 || signs.length > 20 || signs.some((sign) => sign.length < 2 || sign.length > 500)) {
    throw new Error(`Provide 1 to 20 ${language === "en" ? "English" : "Filipino"} symptoms, one per line.`);
  }

  const reference = {
    publisher: text(formData, `${language}_reference_publisher`, 500),
    title: text(formData, `${language}_reference_title`, 500),
    url: text(formData, `${language}_reference_url`, 2_048),
  };
  if (!isSafeReference(reference)) {
    throw new Error(`The ${language === "en" ? "English" : "Filipino"} reference needs a publisher, title, and public HTTPS URL.`);
  }
  return { language, content, signs, reference };
}

function text(formData: FormData, key: string, maxLength = 500): string {
  const value = String(formData.get(key) ?? "").trim();
  if (value.length > maxLength) throw new Error(`${key} is too long.`);
  return value;
}
