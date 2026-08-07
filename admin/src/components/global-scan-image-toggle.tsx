"use client";

import { useState } from "react";
import { ImageZoomViewer } from "@/components/image-zoom-viewer";

type GlobalScanImageToggleProps = {
  originalUrl: string | null;
  annotatedUrl: string | null;
  alt: string;
};

export function GlobalScanImageToggle({
  originalUrl,
  annotatedUrl,
  alt,
}: GlobalScanImageToggleProps) {
  const [showAnnotated, setShowAnnotated] = useState(false);
  const activeUrl = showAnnotated && annotatedUrl ? annotatedUrl : originalUrl;

  return (
    <>
      {annotatedUrl ? (
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#e5ece2] bg-[#fffdfd] px-4 py-3">
          <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#647166]">Image view</span>
          <div className="flex rounded-xl border border-[#d5e2d3] p-1" role="group" aria-label="Choose scan image view">
            <button
              type="button"
              aria-pressed={!showAnnotated}
              onClick={() => setShowAnnotated(false)}
              className={`focus-ring rounded-lg px-3 py-1.5 text-xs font-semibold ${!showAnnotated ? "bg-[#eaf4e8] text-[#1f6b3a]" : "text-[#647166]"}`}
            >
              Original
            </button>
            <button
              type="button"
              aria-pressed={showAnnotated}
              onClick={() => setShowAnnotated(true)}
              className={`focus-ring rounded-lg px-3 py-1.5 text-xs font-semibold ${showAnnotated ? "bg-[#eaf4e8] text-[#1f6b3a]" : "text-[#647166]"}`}
            >
              Annotated
            </button>
          </div>
        </div>
      ) : null}
      <ImageZoomViewer
        url={activeUrl}
        alt={alt}
        sizes="(min-width: 1280px) 65vw, 100vw"
        className="h-[32rem] max-h-[70svh] min-h-[20rem] w-full"
      />
      {showAnnotated && annotatedUrl ? <p className="border-t border-[#e5ece2] px-4 py-2 text-[11px] font-semibold text-[#647166]">AI screening overlay • confidence shown from the scan result</p> : null}
    </>
  );
}
