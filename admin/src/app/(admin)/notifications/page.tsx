import { CheckCircle2 } from "lucide-react";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { randomUUID } from "node:crypto";
import { ConfirmNotificationButton } from "@/components/confirm-notification-button";
import { requireAdmin } from "@/lib/auth";
import { requireIdempotencyKey } from "@/lib/action-idempotency";
import {
  NOTIFICATION_CATEGORIES,
  parseNotificationFormData,
} from "@/lib/notification-validation";
import { getAdminClient } from "@/lib/supabase/admin";

export const dynamic = "force-dynamic";

type PublishedNotification = {
  id: string;
  category: string;
  title_en: string;
  body_en: string;
  title_fil: string | null;
  body_fil: string | null;
  published_at: string;
};

async function publishNotification(formData: FormData) {
  "use server";

  const admin = await requireAdmin(["owner", "admin"]);
  const idempotencyKey = requireIdempotencyKey(formData);
  const payload = parseNotificationFormData(formData);
  const supabase = getAdminClient();
  const filipino = payload.filipino;
  const publishedAt = new Date().toISOString();
  const { error } = await supabase.from("admin_notifications").insert({
    idempotency_key: idempotencyKey,
    category: payload.category,
    title_en: payload.english.title,
    body_en: payload.english.body,
    title_fil: filipino?.title ?? payload.english.title,
    body_fil: filipino?.body ?? payload.english.body,
    status: "published",
    published_at: publishedAt,
    created_by: admin.user.id,
  });

  if (error && error.code !== "23505") {
    throw new Error("The notification could not be published. Try again.");
  }

  revalidatePath("/notifications");
  redirect("/notifications?published=1");
}

export default async function NotificationsPage({
  searchParams,
}: {
  searchParams: Promise<{ published?: string }>;
}) {
  await requireAdmin(["owner", "admin"]);
  const supabase = getAdminClient();
  const { data, error } = await supabase
    .from("admin_notifications")
    .select("id,category,title_en,body_en,title_fil,body_fil,published_at")
    .eq("status", "published")
    .order("published_at", { ascending: false })
    .limit(50);
  const notifications = (data ?? []) as PublishedNotification[];
  const params = await searchParams;

  return (
    <div className="admin-page notifications-page fade-up mx-auto max-w-[1240px]">
      <header className="flex flex-wrap items-end justify-between gap-5">
        <h1 className="text-3xl font-bold tracking-[-.03em]">Notifications</h1>
      </header>

      {params.published === "1" && (
        <p role="status" className="status-banner mt-5 flex items-center gap-2 rounded-xl border border-[#bfe4c5] bg-[#f1fbf2] p-3 text-sm font-semibold text-[#247936]">
          <CheckCircle2 size={17} /> Notification published. It will appear after an app cloud refresh.
        </p>
      )}

      <form action={publishNotification} className="mt-6 grid gap-5">
        <input type="hidden" name="idempotency_key" value={randomUUID()} />
        <section className="surface p-5 sm:p-6">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h2 className="text-xl font-bold">Write an update</h2>
              <p className="mt-1 max-w-2xl text-sm leading-6 text-[#647166]">English is required; Filipino is optional.</p>
            </div>
          </div>

          <div className="mt-5 grid gap-4 sm:grid-cols-[minmax(0,240px)_1fr]">
            <label className="grid gap-1.5 text-sm font-semibold">
              Notification type
              <select name="category" defaultValue="announcement" required className="focus-ring min-h-11 rounded-xl border border-[#d5e2d3] bg-white px-3 font-normal">
                {NOTIFICATION_CATEGORIES.map((category) => (
                  <option key={category} value={category}>{category[0].toUpperCase() + category.slice(1)}</option>
                ))}
              </select>
            </label>
            <p className="self-end rounded-xl border border-[#e1ebe0] bg-[#f8fbf7] p-3 text-sm leading-6 text-[#5e6d61]">Published notices are read-only in the app. Use a new notice for a correction or follow-up.</p>
          </div>

          <div className="mt-5 grid gap-5 lg:grid-cols-2">
            <LanguageFields language="en" label="English" required />
            <LanguageFields language="fil" label="Filipino (optional)" />
          </div>
        </section>

        <div className="surface flex flex-wrap items-center justify-between gap-4 p-5">
          <p className="max-w-2xl text-sm leading-6 text-[#647166]">The app keeps the message locally once it syncs, so users can read it even if they go offline later.</p>
          <ConfirmNotificationButton />
        </div>
      </form>

      <section className="mt-8" aria-labelledby="published-notifications-heading">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p className="page-kicker">Message history</p>
            <h2 id="published-notifications-heading" className="mt-2 text-xl font-bold">Published notifications</h2>
          </div>
          <p className="text-sm text-[#647166]">{notifications.length} recent message{notifications.length === 1 ? "" : "s"}</p>
        </div>

        {error ? (
          <p role="alert" className="mt-4 rounded-xl bg-[#fff0f2] p-4 text-sm text-[#a92f40]">Notifications are temporarily unavailable. Apply the notification database migration, then refresh this page.</p>
        ) : notifications.length === 0 ? (
          <div className="surface mt-4 grid place-items-center p-10 text-center">
            <h3 className="mt-4 text-lg font-bold">No notifications yet</h3>
            <p className="mt-1 max-w-md text-sm leading-6 text-[#647166]">Your first published update will appear here and in the app after its next cloud refresh.</p>
          </div>
        ) : (
          <div className="mt-4 grid gap-3">
            {notifications.map((notification) => (
              <article key={notification.id} className="surface p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <span className="inline-flex rounded-full bg-[#eaf4e8] px-2.5 py-1 text-xs font-semibold capitalize text-[#1f6b3a]">{notification.category}</span>
                    <h3 className="mt-3 text-lg font-bold">{notification.title_en}</h3>
                  </div>
                  <time dateTime={notification.published_at} className="text-xs font-semibold text-[#718075]">{new Date(notification.published_at).toLocaleString()}</time>
                </div>
                <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-[#4f6155]">{notification.body_en}</p>
                {notification.title_fil && notification.body_fil && (notification.title_fil !== notification.title_en || notification.body_fil !== notification.body_en) && (
                  <details className="mt-4 rounded-xl border border-[#e1ebe0] bg-[#f8fbf7] p-3">
                    <summary className="cursor-pointer text-sm font-semibold text-[#31563a]">Filipino translation</summary>
                    <h4 className="mt-3 font-semibold">{notification.title_fil}</h4>
                    <p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-[#4f6155]">{notification.body_fil}</p>
                  </details>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function LanguageFields({ language, label, required = false }: { language: "en" | "fil"; label: string; required?: boolean }) {
  return (
    <fieldset className="rounded-2xl border border-[#e1ebe0] bg-[#fbfdf9] p-4">
      <legend className="px-1 text-sm font-bold text-[#203a28]">{label}</legend>
      <label className="mt-2 grid gap-1.5 text-sm font-semibold">
        Title
        <input name={`${language}_title`} required={required} maxLength={120} placeholder={required ? "For example: New disease guide" : "Optional translation"} className="focus-ring min-h-11 rounded-xl border border-[#d5e2d3] bg-white px-3 font-normal" />
      </label>
      <label className="mt-4 grid gap-1.5 text-sm font-semibold">
        Message
        <textarea name={`${language}_body`} required={required} maxLength={2_000} rows={5} placeholder={required ? "Write the update users should see." : "Optional translation"} className="focus-ring rounded-xl border border-[#d5e2d3] bg-white p-3 font-normal leading-6" />
      </label>
    </fieldset>
  );
}
