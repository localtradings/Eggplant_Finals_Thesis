"use client";

import Image from "next/image";
import { useState } from "react";

export function CatalogArtwork({ src, alt, version, priority = false }: { src: string | null; alt: string; version?: number | null; priority?: boolean }) {
  const [state, setState] = useState<"loading" | "loaded" | "error">(src ? "loading" : "error");
  const imageSrc = src && version ? `${src}?v=${version}` : src;

  return (
    <div className="catalog-artwork">
      {state === "loading" && <div className="catalog-artwork-loading" aria-label="Loading artwork" />}
      {state === "error" ? (
        <div className="catalog-artwork-fallback">{src ? "Artwork unavailable" : "Artwork pending"}</div>
      ) : (
        <Image
          src={imageSrc as string}
          alt={alt}
          fill
          priority={priority}
          sizes="(min-width: 1024px) 30vw, (min-width: 640px) 45vw, 100vw"
          unoptimized
          className={state === "loaded" ? "is-loaded" : "is-loading"}
          onLoad={() => setState("loaded")}
          onError={() => setState("error")}
        />
      )}
    </div>
  );
}
