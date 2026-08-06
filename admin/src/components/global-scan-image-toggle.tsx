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
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#ece9f1] bg-[#fffdfd] px-4 py-3">
          <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#716c80]">Image view</span>
          <div className="flex rounded-xl border border-[#dcd8e4] p-1" role="group" aria-label="Choose scan image view">
            <button
              type="button"
              aria-pressed={!showAnnotated}
              onClick={() => setShowAnnotated(false)}
              className={`focus-ring rounded-lg px-3 py-1.5 text-xs font-semibold ${!showAnnotated ? "bg-[#f1ecf8] text-[#5b3295]" : "text-[#716c80]"}`}
            >
              Original
            </button>
            <button
              type="button"
              aria-pressed={showAnnotated}
              onClick={() => setShowAnnotated(true)}
              className={`focus-ring rounded-lg px-3 py-1.5 text-xs font-semibold ${showAnnotated ? "bg-[#f1ecf8] text-[#5b3295]" : "text-[#716c80]"}`}
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
      {showAnnotated && annotatedUrl ? <p className="border-t border-[#ece9f1] px-4 py-2 text-[11px] font-semibold text-[#716c80]">AI screening overlay • confidence shown from the scan result</p> : null}
    </>
  );
}
