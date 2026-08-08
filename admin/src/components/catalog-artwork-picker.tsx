"use client";

import { useEffect, useState } from "react";
import Image from "next/image";

export function CatalogArtworkPicker() {
  const [preview, setPreview] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => () => {
    if (preview) URL.revokeObjectURL(preview);
  }, [preview]);

  return (
    <label className="grid gap-2 text-sm font-semibold">
      Disease artwork (JPEG, up to 5 MB)
      <input
        required
        name="artwork"
        type="file"
        accept="image/jpeg,.jpg,.jpeg"
        onChange={(event) => {
          const file = event.target.files?.[0];
          if (preview) URL.revokeObjectURL(preview);
          if (!file) {
            setPreview(null);
            setMessage(null);
            return;
          }
          if (file.type !== "image/jpeg" || file.size > 5 * 1024 * 1024) {
            event.currentTarget.value = "";
            setPreview(null);
            setMessage("Choose a JPEG image no larger than 5 MB.");
            return;
          }
          setMessage(null);
          setPreview(URL.createObjectURL(file));
        }}
        className="focus-ring min-h-11 rounded-xl border border-[#d5e2d3] bg-white px-3 py-2 font-normal"
      />
      {message && <span role="alert" className="text-sm font-normal text-[#a92f40]">{message}</span>}
      {preview && (
        <div className="mt-1 overflow-hidden rounded-2xl border border-[#d5e2d3] bg-[#f2f7ef] p-3">
          <Image
            src={preview}
            alt="Selected disease artwork preview"
            width={960}
            height={720}
            unoptimized
            className="mx-auto max-h-[28rem] w-full rounded-xl object-contain"
          />
          <p className="mt-2 text-xs font-normal text-[#68766b]">Whole-image preview. The uploaded JPEG will be shown without cropping.</p>
        </div>
      )}
    </label>
  );
}
