export const NOTIFICATION_CATEGORIES = [
  "announcement",
  "update",
  "tip",
  "alert",
] as const;

export type NotificationCategory = (typeof NOTIFICATION_CATEGORIES)[number];

export type NotificationDraft = {
  category: NotificationCategory;
  english: {
    title: string;
    body: string;
  };
  filipino: {
    title: string;
    body: string;
  } | null;
};

function requiredText(formData: FormData, name: string, minimum: number, maximum: number) {
  const value = formData.get(name);
  if (typeof value !== "string") {
    throw new Error(`The ${name.replaceAll("_", " ")} field is required.`);
  }
  const normalized = value.trim();
  if (normalized.length < minimum || normalized.length > maximum) {
    throw new Error(`The ${name.replaceAll("_", " ")} field must be ${minimum}-${maximum} characters.`);
  }
  return normalized;
}

function optionalText(formData: FormData, name: string, maximum: number) {
  const value = formData.get(name);
  if (value == null || value === "") return "";
  if (typeof value !== "string") {
    throw new Error(`The ${name.replaceAll("_", " ")} field is invalid.`);
  }
  const normalized = value.trim();
  if (normalized.length > maximum) {
    throw new Error(`The ${name.replaceAll("_", " ")} field is too long.`);
  }
  return normalized;
}

export function parseNotificationFormData(formData: FormData): NotificationDraft {
  const category = formData.get("category");
  if (
    typeof category !== "string" ||
    !NOTIFICATION_CATEGORIES.includes(category as NotificationCategory)
  ) {
    throw new Error("Choose a valid notification type.");
  }

  const filipinoTitle = optionalText(formData, "fil_title", 120);
  const filipinoBody = optionalText(formData, "fil_body", 2_000);
  if (Boolean(filipinoTitle) !== Boolean(filipinoBody)) {
    throw new Error("Add both Filipino fields or leave both empty.");
  }

  return {
    category: category as NotificationCategory,
    english: {
      title: requiredText(formData, "en_title", 2, 120),
      body: requiredText(formData, "en_body", 2, 2_000),
    },
    filipino: filipinoTitle && filipinoBody
      ? { title: filipinoTitle, body: filipinoBody }
      : null,
  };
}
