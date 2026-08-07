"use client";

import { useState } from "react";
import { useFormStatus } from "react-dom";

export function ConfirmPublishButton() {
  const [open, setOpen] = useState(false);
  const { pending } = useFormStatus();
  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="focus-ring h-12 rounded-xl bg-[#278b3d] px-5 font-semibold text-white transition hover:bg-[#1e7131] disabled:cursor-wait disabled:opacity-60"
        disabled={pending}
      >
        Review and publish disease
      </button>
      {open && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-[#10251a]/70 p-4 backdrop-blur-[2px]" role="presentation">
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="publish-disease-title"
            aria-describedby="publish-disease-description"
            className="w-full max-w-md overflow-hidden rounded-2xl border border-[#d9e6d5] bg-[#fffefa] shadow-[0_24px_70px_rgba(15,45,25,.25)]"
          >
            <div className="border-b border-[#e2eadf] bg-[#f5faf2] px-6 py-5">
              <h2 id="publish-disease-title" className="text-xl font-bold text-[#173322]">Publish this disease?</h2>
            </div>
            <div className="px-6 py-5">
            <p id="publish-disease-description" className="text-sm leading-6 text-[#5e6d61]">
              This adds the bilingual content and image to the Library catalog. It will not add a new detector model class.
            </p>
            </div>
            <div className="flex justify-end gap-3 border-t border-[#e2eadf] bg-[#fbfdf9] px-6 py-4">
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="focus-ring h-11 rounded-xl border border-[#d4e1d1] px-4 font-semibold text-[#3b5040]"
                disabled={pending}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="focus-ring h-11 rounded-xl bg-[#278b3d] px-4 font-semibold text-white disabled:cursor-wait disabled:opacity-60"
                disabled={pending}
              >
                {pending ? "Publishing..." : "Yes, publish"}
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}
