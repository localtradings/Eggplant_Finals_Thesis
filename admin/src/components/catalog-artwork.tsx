"use client";

import Image from "next/image";
import { useState } from "react";

export function CatalogArtwork({ src, alt, version, priority = false }: { src: string | null; alt: string; version?: number | null; priority?: boolean }) {
  const [failed, setFailed] = useState(false);
  const imageSrc = src && version != null ? `${src}${src.includes("?") ? "&" : "?"}v=${version}` : src;

  return (
    <div className="catalog-artwork">
      {imageSrc && !failed ? (
        <Image
          src={imageSrc}
          alt={alt}
          fill
          priority={priority}
          loading={priority ? undefined : "eager"}
          sizes="(min-width: 1024px) 30vw, (min-width: 640px) 45vw, 100vw"
          unoptimized
          onError={() => setFailed(true)}
        />
      ) : (
        <div className="catalog-artwork-fallback">Artwork unavailable</div>
      )}
    </div>
  );
}
