"use client";

import { useState } from "react";
import { useFormStatus } from "react-dom";

export function ConfirmNotificationButton() {
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
        Review and publish
      </button>
      {open && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-[#10251a]/70 p-4 backdrop-blur-[2px]" role="presentation">
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="publish-notification-title"
            aria-describedby="publish-notification-description"
            className="w-full max-w-md overflow-hidden rounded-2xl border border-[#d9e6d5] bg-[#fffefa] shadow-[0_24px_70px_rgba(15,45,25,.25)]"
          >
            <div className="border-b border-[#e2eadf] bg-[#f5faf2] px-6 py-5">
              <h2 id="publish-notification-title" className="text-xl font-bold text-[#173322]">Publish this notification?</h2>
            </div>
            <div className="px-6 py-5">
              <p id="publish-notification-description" className="text-sm leading-6 text-[#5e6d61]">
                This message will appear in the app&apos;s Notifications area after an installed app completes its next cloud refresh.
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
