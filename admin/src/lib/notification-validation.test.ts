import { describe, expect, it } from "vitest";
import { parseNotificationFormData } from "./notification-validation";

function form(fields: Record<string, string>) {
  const result = new FormData();
  Object.entries(fields).forEach(([key, value]) => result.set(key, value));
  return result;
}

describe("parseNotificationFormData", () => {
  it("normalizes a bilingual notification", () => {
    expect(parseNotificationFormData(form({
      category: "announcement",
      en_title: "  New catalog  ",
      en_body: "  A new disease guide is available. ",
      fil_title: "  Bagong catalogo  ",
      fil_body: "  May bagong gabay sa sakit. ",
    }))).toEqual({
      category: "announcement",
      english: { title: "New catalog", body: "A new disease guide is available." },
      filipino: { title: "Bagong catalogo", body: "May bagong gabay sa sakit." },
    });
  });

  it("allows an English-only notification", () => {
    expect(parseNotificationFormData(form({
      category: "tip",
      en_title: "Scanning tip",
      en_body: "Use even lighting for a clearer result.",
    })).filipino).toBeNull();
  });

  it("rejects a half-complete Filipino translation", () => {
    expect(() => parseNotificationFormData(form({
      category: "update",
      en_title: "Update",
      en_body: "The app has a new update.",
      fil_title: "Update sa app",
    }))).toThrow("Add both Filipino fields or leave both empty.");
  });

  it("rejects an invalid category", () => {
    expect(() => parseNotificationFormData(form({
      category: "other",
      en_title: "Update",
      en_body: "The app has a new update.",
    }))).toThrow("Choose a valid notification type.");
  });
});
