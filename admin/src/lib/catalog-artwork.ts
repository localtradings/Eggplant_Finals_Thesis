const LOCAL_CATALOG_ARTWORK: Record<string, string> = {
  "fruit-rot": "/disease-artwork/fruit-rot.jpg",
  "fruit-borer": "/disease-artwork/fruit-borer.jpg",
  "insect-pest": "/disease-artwork/insect-pest.jpg",
  "leaf-spot": "/disease-artwork/leaf-spot.jpg",
  "melon-thrips": "/disease-artwork/melon-thrips-v3.jpg",
  "mosaic-virus": "/disease-artwork/mosaic-virus.jpg",
  "white-molds": "/disease-artwork/white-molds.jpg",
  wilt: "/disease-artwork/wilt.jpg",
};

const CATALOG_ARTWORK_PLACEHOLDER = "/design-references/artwork-placeholder.png";

export function getCatalogArtworkUrl(id: string, remoteUrl?: string | null) {
  return LOCAL_CATALOG_ARTWORK[id] ?? remoteUrl ?? CATALOG_ARTWORK_PLACEHOLDER;
}

export function getCatalogDisplayCategory(id: string, category: string) {
  return id === "melon-thrips" ? "FRUIT_DISEASE" : category;
}
