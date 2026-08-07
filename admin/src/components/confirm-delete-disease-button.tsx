"use client";

import { AlertTriangle, LoaderCircle, Trash2, X } from "lucide-react";
import { useEffect, useState } from "react";
import { useFormStatus } from "react-dom";

const APPROVAL_PHRASE = "I approve this destructive database action.";

type DeleteAction = (formData: FormData) => void | Promise<void>;

export function ConfirmDeleteDiseaseButton({
  action,
  diseaseName,
  diseaseId,
  idempotencyKey,
}: {
  action: DeleteAction;
  diseaseName: string;
  diseaseId: string;
  idempotencyKey: string;
}) {
  const [open, setOpen] = useState(false);
  const [approval, setApproval] = useState("");
  const approved = approval === APPROVAL_PHRASE;

  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open]);

  return (
    <>
      <button
        type="button"
        onClick={() => {
          setApproval("");
          setOpen(true);
        }}
        className="focus-ring action-button inline-flex min-h-11 items-center gap-2 rounded-xl border border-[#e7c8c2] bg-[#fff8f6] px-4 text-sm font-semibold text-[#a43c32] hover:bg-[#fff1ed]"
      >
        <Trash2 size={16} aria-hidden="true" />
        Delete disease
      </button>
      {open && (
        <div
          className="fixed inset-0 z-50 grid place-items-center bg-[#10251a]/70 p-4 backdrop-blur-[2px]"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) setOpen(false);
          }}
        >
          <form
            action={action}
            className="w-full max-w-lg overflow-hidden rounded-2xl border border-[#d9e6d5] bg-[#fffefa] shadow-[0_24px_70px_rgba(15,45,25,.25)]"
          >
            <input type="hidden" name="id" value={diseaseId} />
            <input type="hidden" name="idempotency_key" value={idempotencyKey} />
            <div className="flex items-start justify-between gap-4 border-b border-[#e2eadf] bg-[#f5faf2] px-5 py-4">
              <div className="flex items-start gap-3">
                <span className="mt-0.5 grid size-9 shrink-0 place-items-center rounded-xl bg-[#fbe9e4] text-[#a43c32]">
                  <AlertTriangle size={18} aria-hidden="true" />
                </span>
                <div>
                  <h2 className="text-lg font-bold text-[#173322]">Delete this library disease?</h2>
                  <p className="mt-1 text-sm text-[#647166]">This action removes the entry and its bilingual catalog content.</p>
                </div>
              </div>
              <button type="button" onClick={() => setOpen(false)} className="focus-ring rounded-lg p-1.5 text-[#657066] hover:bg-white hover:text-[#173322]" aria-label="Close delete dialog">
                <X size={18} aria-hidden="true" />
              </button>
            </div>
            <div className="space-y-4 px-5 py-5">
              <p className="rounded-xl border border-[#eadfca] bg-[#fffaf0] p-3 text-sm leading-6 text-[#6f5c35]">
                <span className="font-semibold">{diseaseName}</span> is a library-only entry. Detector classes are protected and cannot be deleted. Existing scans or catalog references will also block this action.
              </p>
              <label className="grid gap-1.5 text-sm font-semibold text-[#27392c]">
                Type the exact approval phrase to continue
                <input
                  autoFocus
                  required
                  value={approval}
                  onChange={(event) => setApproval(event.target.value)}
                  name="destructive_approval"
                  spellCheck={false}
                  className="focus-ring min-h-11 rounded-xl border border-[#d4e1d1] bg-white px-3 font-normal"
                  placeholder={APPROVAL_PHRASE}
                />
              </label>
              <p className="text-xs leading-5 text-[#68766b]">Nothing is deleted until you submit this confirmation. Press Escape or Cancel to keep the entry.</p>
            </div>
            <div className="flex flex-wrap justify-end gap-3 border-t border-[#e2eadf] bg-[#fbfdf9] px-5 py-4">
              <button type="button" onClick={() => setOpen(false)} className="focus-ring min-h-11 rounded-xl border border-[#d4e1d1] px-4 text-sm font-semibold text-[#3b5040] hover:bg-white">
                Cancel
              </button>
              <DeleteSubmitButton disabled={!approved} />
            </div>
          </form>
        </div>
      )}
    </>
  );
}

function DeleteSubmitButton({ disabled }: { disabled: boolean }) {
  const { pending } = useFormStatus();
  return (
    <button
      type="submit"
      disabled={disabled || pending}
      className="focus-ring inline-flex min-h-11 items-center gap-2 rounded-xl bg-[#a43c32] px-4 text-sm font-semibold text-white hover:bg-[#8b3028] disabled:cursor-not-allowed disabled:opacity-45"
    >
      {pending && <LoaderCircle className="animate-spin" size={16} aria-hidden="true" />}
      {pending ? "Deleting..." : "Delete permanently"}
    </button>
  );
}
