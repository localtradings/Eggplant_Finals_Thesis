"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

const REFRESH_INTERVAL_MS = 30_000;

function localTimestampLabel(timestamp: string | null) {
  if (!timestamp) return "No sync yet";
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) return "Unknown";
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(date);
}

export function LiveActivityTimestamp({ timestamp }: { timestamp: string | null }) {
  const router = useRouter();

  useEffect(() => {
    const refreshWhenVisible = () => {
      if (document.visibilityState === "visible") router.refresh();
    };
    const interval = window.setInterval(refreshWhenVisible, REFRESH_INTERVAL_MS);
    document.addEventListener("visibilitychange", refreshWhenVisible);
    return () => {
      window.clearInterval(interval);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [router]);

  return (
    <time dateTime={timestamp ?? undefined} title={timestamp ?? undefined} suppressHydrationWarning>
      {timestamp ? localTimestampLabel(timestamp) : "No sync yet"}
    </time>
  );
}
