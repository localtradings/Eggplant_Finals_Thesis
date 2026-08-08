import { describe, expect, it } from "vitest";
import {
  CATALOG_CONTENT_FIELDS,
  parseCreateDiseaseFormData,
  validateJpegBytes,
} from "./disease-catalog-validation";

function validForm() {
  const form = new FormData();
  form.set("disease_id", "purple-leaf-blight");
  form.set("category", "LEAF_DISEASE");
  form.set("artwork", new File([new Uint8Array([0xff, 0xd8, 0xff, 0x00, 0xff, 0xd9])], "artwork.jpg", { type: "image/jpeg" }));
  for (const language of ["en", "fil"] as const) {
    for (const field of CATALOG_CONTENT_FIELDS) form.set(`${language}_${field}`, `${language} ${field} content`);
    form.set(`${language}_signs`, `${language} sign one\n${language} sign two`);
    form.set(`${language}_reference_publisher`, "Department of Agriculture");
    form.set(`${language}_reference_title`, "Eggplant disease guidance");
    form.set(`${language}_reference_url`, "https://example.org/eggplant-guidance");
  }
  return form;
}

describe("disease catalog creation validation", () => {
  it("parses complete bilingual library content and JPEG artwork", () => {
    const result = parseCreateDiseaseFormData(validForm());
    expect(result.diseaseId).toBe("purple-leaf-blight");
    expect(result.english.signs).toHaveLength(2);
    expect(result.filipino.reference.url).toMatch(/^https:\/\//);
  });

  it.each([
    ["invalid ID", "disease_id", "Not valid"],
    ["unsafe reference", "en_reference_url", "http://example.org/source"],
    ["missing symptom", "fil_signs", ""],
  ])("rejects %s", (_, key, value) => {
    const form = validForm();
    form.set(key, value);
    expect(() => parseCreateDiseaseFormData(form)).toThrow();
  });

  it("requires JPEG content boundaries, not only a filename", () => {
    expect(validateJpegBytes(new Uint8Array([0xff, 0xd8, 0xff, 0x00, 0xff, 0xd9]))).toBe(true);
    expect(validateJpegBytes(new Uint8Array([0x89, 0x50, 0x4e, 0x47]))).toBe(false);
    expect(validateJpegBytes(new Uint8Array([0xff, 0xd8, 0xff]))).toBe(false);
  });
});
