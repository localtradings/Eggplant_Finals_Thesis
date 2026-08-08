import { CatalogArtworkPicker } from "@/components/catalog-artwork-picker";
import { ConfirmPublishButton } from "@/components/confirm-publish-button";
import { hashActionPayload, requireIdempotencyKey } from "@/lib/action-idempotency";
import {
  CATALOG_CONTENT_FIELDS,
  parseCreateDiseaseFormData,
  validateJpegBytes,
} from "@/lib/disease-catalog-validation";
import { requireAdmin } from "@/lib/auth";
import { getAdminClient } from "@/lib/supabase/admin";
import { ArrowLeft, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { createHash, randomUUID } from "node:crypto";

export const dynamic = "force-dynamic";

async function createDisease(formData: FormData) {
  "use server";
  const admin = await requireAdmin(["owner", "admin"]);
  const idempotencyKey = requireIdempotencyKey(formData);
  const payload = parseCreateDiseaseFormData(formData);
  const supabase = getAdminClient();
  const { data: existing, error: existingError } = await supabase
    .from("disease_catalog")
    .select("id")
    .eq("id", payload.diseaseId)
    .maybeSingle();
  if (existingError) throw new Error("The catalog could not be checked. Try again.");
  if (existing) throw new Error("That disease ID already exists. Use a different ID.");

  const artworkBytes = new Uint8Array(await payload.artwork.arrayBuffer());
  if (!validateJpegBytes(artworkBytes)) throw new Error("The uploaded file is not a valid JPEG image.");
  const artworkPath = `catalog/${payload.diseaseId}.jpg`;
  const artworkHash = createHash("sha256").update(artworkBytes).digest("hex");
  const payloadHash = hashActionPayload({
    diseaseId: payload.diseaseId,
    category: payload.category,
    artworkPath,
    artworkSha256: artworkHash,
    english: { content: payload.english.content, signs: payload.english.signs, reference: payload.english.reference },
    filipino: { content: payload.filipino.content, signs: payload.filipino.signs, reference: payload.filipino.reference },
  });

  const bucket = supabase.storage.from("disease-catalog-artwork");
  const { error: uploadError } = await bucket.upload(artworkPath, Buffer.from(artworkBytes), {
    contentType: "image/jpeg",
    cacheControl: "31536000",
    upsert: false,
  });
  if (uploadError) throw new Error("The disease artwork could not be uploaded. Try again.");

  const { data: outcome, error: rpcError } = await supabase.rpc("create_disease_catalog_entry_v1", {
    p_disease_id: payload.diseaseId,
    p_category: payload.category,
    p_artwork_path: artworkPath,
    p_en_content: payload.english.content,
    p_fil_content: payload.filipino.content,
    p_en_signs: payload.english.signs,
    p_fil_signs: payload.filipino.signs,
    p_en_reference: payload.english.reference,
    p_fil_reference: payload.filipino.reference,
    p_admin_id: admin.user.id,
    p_idempotency_key: idempotencyKey,
    p_payload_hash: payloadHash,
  });
  if (rpcError || !["applied", "unchanged"].includes(outcome ?? "")) {
    await bucket.remove([artworkPath]);
    throw new Error("The disease could not be published. No catalog entry was created.");
  }
  revalidatePath("/disease-catalog");
  redirect(`/disease-catalog?published=${encodeURIComponent(payload.diseaseId)}&outcome=${encodeURIComponent(outcome)}`);
}

export default async function AddDiseasePage() {
  await requireAdmin(["owner", "admin"]);
  return (
    <div className="fade-up mx-auto max-w-5xl">
      <Link href="/disease-catalog" className="inline-flex items-center gap-2 text-sm font-semibold text-[#278b3d]"><ArrowLeft size={17} />Back to catalog</Link>
      <header className="mt-4">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="text-3xl font-bold tracking-[-.03em]">Add disease</h1>
          <span className="inline-flex items-center gap-1 rounded-full bg-[#edf7ee] px-3 py-1 text-xs font-semibold text-[#247936]"><ShieldCheck size={14} />Owner/admin only</span>
        </div>
        <p className="mt-1 max-w-3xl text-sm leading-6 text-[#686a69]">Add a library entry with complete English and Filipino educational content. New entries are library-only and do not change the on-device detector model.</p>
      </header>
      <form id="create-disease-form" action={createDisease} encType="multipart/form-data" className="mt-6 grid gap-5">
        <input type="hidden" name="idempotency_key" value={randomUUID()} />
        <section className="surface p-5">
          <h2 className="text-xl font-bold">Catalog identity</h2>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <Field name="disease_id" label="Disease ID (lowercase with hyphens)" placeholder="example-disease" />
            <label className="grid gap-1.5 text-sm font-semibold">Category<select required name="category" defaultValue="LEAF_DISEASE" className="focus-ring min-h-11 rounded-xl border border-[#d5e2d3] bg-white px-3 font-normal"><option value="LEAF_DISEASE">Leaf disease</option><option value="FRUIT_DISEASE">Fruit disease</option></select></label>
          </div>
          <div className="mt-4"><CatalogArtworkPicker /></div>
          <p className="mt-3 text-xs leading-5 text-[#686a69]">The artwork is stored in the dedicated public catalog-artwork bucket so phones can cache it during catalog sync. Use a clear crop with no private information.</p>
        </section>
        <LanguageSection language="en" title="English" />
        <LanguageSection language="fil" title="Filipino" />
        <div className="surface flex flex-wrap items-center justify-between gap-4 p-5">
          <p className="max-w-xl text-sm leading-6 text-[#686a69]">Publishing increases the catalog version. The app will show this entry in Library after its next successful catalog refresh; the existing detector remains unchanged.</p>
          <ConfirmPublishButton formId="create-disease-form" />
        </div>
      </form>
    </div>
  );
}

function LanguageSection({ language, title }: { language: "en" | "fil"; title: string }) {
  return (
    <section className="surface p-5">
      <h2 className="text-xl font-bold">{title}</h2>
      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <Field name={`${language}_name`} label="Name" />
        <Field name={`${language}_symptom_preview`} label="Symptom preview" />
      </div>
      <SignsArea name={`${language}_signs`} />
      <div className="mt-1 grid gap-4 sm:grid-cols-2">
        {CATALOG_CONTENT_FIELDS.filter((field) => !["name", "symptom_preview"].includes(field)).map((field) => (
          <Area key={field} name={`${language}_${field}`} label={field === "recommended_action" ? "Recommended action" : field.replaceAll("_", " ")} />
        ))}
      </div>
      <h3 className="mt-6 font-bold">Primary authoritative reference</h3>
      <div className="mt-3 grid gap-4 sm:grid-cols-2">
        <Field name={`${language}_reference_publisher`} label="Publisher" />
        <Field name={`${language}_reference_title`} label="Title" />
      </div>
      <Field name={`${language}_reference_url`} label="HTTPS URL" type="url" placeholder="https://example.org/source" />
    </section>
  );
}

function Field({ name, label, type = "text", placeholder }: { name: string; label: string; type?: string; placeholder?: string }) {
  return <label className="mt-3 grid gap-1.5 text-sm font-semibold">{label}<input required type={type} maxLength={label === "HTTPS URL" ? 2_048 : 500} name={name} placeholder={placeholder} className="focus-ring min-h-11 rounded-xl border border-[#d5e2d3] bg-white px-3 font-normal" /></label>;
}

function Area({ name, label }: { name: string; label: string }) {
  return <label className="mt-4 grid gap-1.5 text-sm font-semibold capitalize">{label}<textarea required maxLength={4_000} name={name} rows={4} className="focus-ring rounded-xl border border-[#d5e2d3] bg-white p-3 font-normal leading-6" /></label>;
}

function SignsArea({ name }: { name: string }) {
  return <label className="mt-4 grid gap-1.5 text-sm font-semibold">Symptoms (one per line)<textarea required maxLength={10_019} name={name} rows={5} className="focus-ring rounded-xl border border-[#d5e2d3] bg-white p-3 font-normal leading-6" /></label>;
}
