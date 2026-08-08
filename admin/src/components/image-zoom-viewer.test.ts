import { describe, expect, it } from "vitest";
import {
  calculateDragScroll,
  clampImageZoom,
  IMAGE_ZOOM_STEP,
  MAX_IMAGE_ZOOM,
  MIN_IMAGE_ZOOM,
} from "./image-zoom-viewer";

describe("clampImageZoom", () => {
  it("keeps zoom inside the supported range", () => {
    expect(clampImageZoom(MIN_IMAGE_ZOOM - IMAGE_ZOOM_STEP)).toBe(MIN_IMAGE_ZOOM);
    expect(clampImageZoom(MAX_IMAGE_ZOOM + IMAGE_ZOOM_STEP)).toBe(MAX_IMAGE_ZOOM);
  });

  it("rounds zoom changes to the control step precision", () => {
    expect(clampImageZoom(1.25)).toBe(1.25);
    expect(clampImageZoom(1.333)).toBe(1.33);
  });
});

describe("calculateDragScroll", () => {
  it("moves the viewport opposite to the pointer direction", () => {
    expect(calculateDragScroll(120, 500, 430)).toBe(190);
    expect(calculateDragScroll(80, 240, 300)).toBe(20);
  });

  it("does not produce negative scroll positions", () => {
    expect(calculateDragScroll(10, 100, 500)).toBe(0);
  });
});
