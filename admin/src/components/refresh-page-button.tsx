"use client";

import { RefreshCw } from "lucide-react";
import { useRouter } from "next/navigation";
import { useTransition } from "react";

export function RefreshPageButton() {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();

  return (
    <button
      type="button"
      className="focus-ring inline-flex min-h-10 items-center gap-2 rounded-xl border border-white/15 bg-white/[.06] px-3 text-xs font-semibold text-[#d9d1e8] hover:bg-white/[.1] hover:text-white"
      onClick={() => startTransition(() => router.refresh())}
      disabled={isPending}
      aria-busy={isPending}
      aria-label="Refresh current page data"
    >
      <RefreshCw size={15} className={isPending ? "animate-spin" : undefined} aria-hidden="true" />
      {isPending ? "Refreshing" : "Refresh data"}
    </button>
  );
}
