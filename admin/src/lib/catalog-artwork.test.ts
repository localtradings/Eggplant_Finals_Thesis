import { describe, expect, it } from "vitest";
import { getCatalogArtworkUrl, getCatalogDisplayCategory } from "./catalog-artwork";

describe("catalog artwork presentation", () => {
  it("uses the fruit-focused local Melon Thrips artwork", () => {
    expect(getCatalogArtworkUrl("melon-thrips", "https://example.test/old-artwork.jpg")).toBe("/disease-artwork/melon-thrips-v3.jpg");
  });

  it("shows Melon Thrips as fruit disease without changing its stable id", () => {
    expect(getCatalogDisplayCategory("melon-thrips", "LEAF_DISEASE")).toBe("FRUIT_DISEASE");
    expect(getCatalogDisplayCategory("leaf-spot", "LEAF_DISEASE")).toBe("LEAF_DISEASE");
  });

  it("falls back to remote artwork or a local placeholder", () => {
    expect(getCatalogArtworkUrl("custom-library-disease", "https://example.test/artwork.jpg")).toBe("https://example.test/artwork.jpg");
    expect(getCatalogArtworkUrl("custom-library-disease")).toBe("/design-references/artwork-placeholder.png");
  });
});
