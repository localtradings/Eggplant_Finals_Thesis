"use client";

import Image from "next/image";
import { Minus, Plus, RotateCcw } from "lucide-react";
import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type PointerEvent as ReactPointerEvent,
} from "react";

export const MIN_IMAGE_ZOOM = 1;
export const MAX_IMAGE_ZOOM = 3;
export const IMAGE_ZOOM_STEP = 0.25;

export function clampImageZoom(value: number) {
  return Math.min(MAX_IMAGE_ZOOM, Math.max(MIN_IMAGE_ZOOM, Math.round(value * 100) / 100));
}

export function calculateDragScroll(startScroll: number, startPointer: number, currentPointer: number) {
  return Math.max(0, Math.round(startScroll + startPointer - currentPointer));
}

type ImageZoomViewerProps = {
  url: string | null;
  alt: string;
  className?: string;
  sizes?: string;
};

export function ImageZoomViewer({ url, alt, className = "h-80 w-full", sizes = "100vw" }: ImageZoomViewerProps) {
  const [zoom, setZoom] = useState(MIN_IMAGE_ZOOM);
  const [isDragging, setIsDragging] = useState(false);
  const viewportRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef<{
    pointerId: number;
    startX: number;
    startY: number;
    startScrollLeft: number;
    startScrollTop: number;
  } | null>(null);

  const centerImage = useCallback(() => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    viewport.scrollLeft = Math.max(0, (viewport.scrollWidth - viewport.clientWidth) / 2);
    viewport.scrollTop = Math.max(0, (viewport.scrollHeight - viewport.clientHeight) / 2);
  }, []);

  useEffect(() => {
    const frame = requestAnimationFrame(centerImage);
    return () => cancelAnimationFrame(frame);
  }, [centerImage, zoom]);

  function handlePointerDown(event: ReactPointerEvent<HTMLDivElement>) {
    if (zoom <= MIN_IMAGE_ZOOM || event.button !== 0 || (event.target as Element).closest("button")) return;
    const viewport = viewportRef.current;
    if (!viewport) return;
    dragRef.current = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      startScrollLeft: viewport.scrollLeft,
      startScrollTop: viewport.scrollTop,
    };
    viewport.setPointerCapture(event.pointerId);
    setIsDragging(true);
    event.preventDefault();
  }

  function handlePointerMove(event: ReactPointerEvent<HTMLDivElement>) {
    const drag = dragRef.current;
    const viewport = viewportRef.current;
    if (!drag || !viewport || drag.pointerId !== event.pointerId) return;
    viewport.scrollLeft = calculateDragScroll(drag.startScrollLeft, drag.startX, event.clientX);
    viewport.scrollTop = calculateDragScroll(drag.startScrollTop, drag.startY, event.clientY);
    event.preventDefault();
  }

  function endPointerDrag(event: ReactPointerEvent<HTMLDivElement>) {
    const drag = dragRef.current;
    const viewport = viewportRef.current;
    if (!drag || drag.pointerId !== event.pointerId) return;
    if (viewport?.hasPointerCapture(event.pointerId)) viewport.releasePointerCapture(event.pointerId);
    dragRef.current = null;
    setIsDragging(false);
  }

  return (
    <div className={`relative overflow-hidden bg-[#f1ecf8] ${className}`}>
      {url ? (
        <>
          <div className="absolute right-3 top-3 z-20 flex items-center gap-2" role="group" aria-label="Image zoom controls">
            <span aria-live="polite" className="rounded-xl border border-[#dcd8e4] bg-white/95 px-3 py-2 text-center font-mono text-xs font-semibold text-[#5e596e] shadow-sm backdrop-blur-sm">
              {Math.round(zoom * 100)}%
            </span>
            <button
              type="button"
              aria-label="Zoom out"
              title="Zoom out"
              disabled={zoom <= MIN_IMAGE_ZOOM}
              onClick={() => setZoom((current) => clampImageZoom(current - IMAGE_ZOOM_STEP))}
              className="focus-ring grid h-10 w-10 place-items-center rounded-xl border border-[#dcd8e4] bg-white/95 text-[#5b3295] shadow-sm backdrop-blur-sm hover:bg-[#f1ecf8] disabled:cursor-not-allowed disabled:opacity-35"
            >
              <Minus size={17} aria-hidden="true" />
            </button>
            <button
              type="button"
              aria-label="Zoom in"
              title="Zoom in"
              disabled={zoom >= MAX_IMAGE_ZOOM}
              onClick={() => setZoom((current) => clampImageZoom(current + IMAGE_ZOOM_STEP))}
              className="focus-ring grid h-10 w-10 place-items-center rounded-xl border border-[#dcd8e4] bg-white/95 text-[#5b3295] shadow-sm backdrop-blur-sm hover:bg-[#f1ecf8] disabled:cursor-not-allowed disabled:opacity-35"
            >
              <Plus size={17} aria-hidden="true" />
            </button>
            <button
              type="button"
              aria-label="Reset zoom"
              title="Reset zoom"
              disabled={zoom === MIN_IMAGE_ZOOM}
              onClick={() => setZoom(MIN_IMAGE_ZOOM)}
              className="focus-ring grid h-10 w-10 place-items-center rounded-xl border border-[#dcd8e4] bg-white/95 text-[#716c80] shadow-sm backdrop-blur-sm hover:bg-[#f1ecf8] disabled:cursor-not-allowed disabled:opacity-35"
            >
              <RotateCcw size={15} aria-hidden="true" />
            </button>
          </div>
          <div
            ref={viewportRef}
            className={`absolute inset-0 overflow-auto overscroll-contain ${zoom > MIN_IMAGE_ZOOM ? (isDragging ? "cursor-grabbing" : "cursor-grab") : "cursor-default"}`}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={endPointerDrag}
            onPointerCancel={endPointerDrag}
            style={{ touchAction: zoom > MIN_IMAGE_ZOOM ? "none" : "auto" }}
            aria-label={zoom > MIN_IMAGE_ZOOM ? "Drag the zoomed image to inspect it" : undefined}
          >
            <div
              className="relative flex shrink-0 items-center justify-center box-border p-5 transition-[width,height] duration-200"
              style={{ width: `${zoom * 100}%`, height: `${zoom * 100}%` }}
            >
              <div className="relative h-full w-full">
                <Image
                  src={url}
                  alt={alt}
                  fill
                  sizes={sizes}
                  unoptimized
                  draggable={false}
                  onDragStart={(event) => event.preventDefault()}
                  className="select-none object-contain"
                />
              </div>
            </div>
          </div>
        </>
      ) : (
        <div className="grid h-full place-items-center text-sm text-[#68687c]">Photo unavailable</div>
      )}
    </div>
  );
}
