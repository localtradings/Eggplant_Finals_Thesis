import { NextResponse } from "next/server";
import { apiError, authorizeMobile } from "@/lib/mobile-api";
import { getAdminClient } from "@/lib/supabase/admin";

export const dynamic = "force-dynamic";

type NotificationRow = {
  id: string;
  category: string;
  title_en: string;
  body_en: string;
  title_fil: string | null;
  body_fil: string | null;
  published_at: string;
  expires_at: string | null;
};

export async function GET(request: Request) {
  const auth = await authorizeMobile(request);
  if ("response" in auth) return auth.response;

  const now = new Date().toISOString();
  const { data, error } = await getAdminClient()
    .from("admin_notifications")
    .select("id,category,title_en,body_en,title_fil,body_fil,published_at,expires_at")
    .eq("status", "published")
    .lte("published_at", now)
    .order("published_at", { ascending: false })
    .limit(100);

  if (error) {
    return apiError("Could not load notifications.", 500, "notifications_failed");
  }

  const items = ((data ?? []) as NotificationRow[])
    .filter((notification) => !notification.expires_at || notification.expires_at > now)
    .map(({ expires_at: expiresAt, ...notification }) => ({
      ...notification,
      expires_at: expiresAt,
    }));

  return NextResponse.json(
    { items, generatedAt: now },
    { headers: { "Cache-Control": "private, no-cache" } },
  );
}
