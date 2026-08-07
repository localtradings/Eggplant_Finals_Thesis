import { FormSubmitButton } from "@/components/form-submit-button";
import { ConfirmDeleteDiseaseButton } from "@/components/confirm-delete-disease-button";
import { hashActionPayload, requireIdempotencyKey } from "@/lib/action-idempotency";
import { requireAdmin } from "@/lib/auth";
import { getAdminClient } from "@/lib/supabase/admin";
import { ArrowLeft, LockKeyhole } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { revalidatePath } from "next/cache";
import { notFound, redirect } from "next/navigation";
import { randomUUID } from "node:crypto";

export const dynamic = "force-dynamic";

async function saveContent(formData: FormData) {
  "use server";
  const admin = await requireAdmin(["owner", "admin"]);
  const id = String(formData.get("id") ?? "");
  const idempotencyKey = requireIdempotencyKey(formData);
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(id)) throw new Error("Invalid disease ID.");
  const payloads = (["en", "fil"] as const).map((language) => {
    const fields = ["name", "description", "symptom_preview", "causes", "recommended_action", "prevention", "guidance", "when_to_act", "disclaimer"] as const;
    const content = Object.fromEntries(fields.map((field) => [field, String(formData.get(`${language}_${field}`) ?? "").trim()]));
    if (Object.values(content).some((value) => value.length < 2 || value.length > 4_000)) throw new Error(`Complete all ${language} content fields within the allowed length.`);
    const reference = {
      publisher: String(formData.get(`${language}_reference_publisher`) ?? "").trim(),
      title: String(formData.get(`${language}_reference_title`) ?? "").trim(),
      url: String(formData.get(`${language}_reference_url`) ?? "").trim(),
    };
    if (!isSafeReference(reference)) throw new Error("Each reference requires a publisher, title, and public HTTPS URL.");
    const signs = String(formData.get(`${language}_signs`) ?? "")
      .split("\n")
      .map((sign) => sign.trim())
      .filter(Boolean);
    if (signs.length < 1 || signs.length > 20 || signs.some((sign) => sign.length < 2 || sign.length > 500)) {
      throw new Error(`Provide 1 to 20 ${language} symptoms, one per line.`);
    }
    return { language, content, reference, signs };
  });
  const [english, filipino] = payloads;
  const payloadHash = hashActionPayload({
    diseaseId: id,
    english: { content: english.content, signs: english.signs, reference: english.reference },
    filipino: { content: filipino.content, signs: filipino.signs, reference: filipino.reference },
  });
  const { data: outcome, error } = await getAdminClient().rpc("update_disease_catalog_content_v2", {
    p_disease_id: id,
    p_en_content: english.content,
    p_fil_content: filipino.content,
    p_en_signs: english.signs,
    p_fil_signs: filipino.signs,
    p_en_reference: english.reference,
    p_fil_reference: filipino.reference,
    p_admin_id: admin.user.id,
    p_idempotency_key: idempotencyKey,
    p_payload_hash: payloadHash,
  });
  if (error || !["applied", "unchanged"].includes(outcome ?? "")) throw new Error("The bilingual disease content could not be published.");
  revalidatePath("/disease-catalog");
  revalidatePath(`/disease-catalog/${id}`);
  redirect(`/disease-catalog?published=${encodeURIComponent(id)}&outcome=${encodeURIComponent(outcome)}`);
}

async function deleteDisease(formData: FormData) {
  "use server";
  const admin = await requireAdmin(["owner", "admin"]);
  const id = String(formData.get("id") ?? "");
  const idempotencyKey = requireIdempotencyKey(formData);
  const approval = String(formData.get("destructive_approval") ?? "");
  const exactApproval = "I approve this destructive database action.";
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(id)) throw new Error("Invalid disease ID.");
  if (approval !== exactApproval) throw new Error("Type the exact destructive-action approval phrase to delete this disease.");

  const supabase = getAdminClient();
  const { data: existing, error: existingError } = await supabase
    .from("disease_catalog")
    .select("id,detector_supported,artwork_path")
    .eq("id", id)
    .maybeSingle();
  if (existingError) throw new Error("The catalog entry could not be checked. Try again.");
  if (!existing) throw new Error("That catalog entry no longer exists.");
  if (existing.detector_supported !== false) throw new Error("Detector-supported classes cannot be deleted.");

  const payloadHash = hashActionPayload({ diseaseId: id, action: "catalog_delete" });
  const { data: outcome, error } = await supabase.rpc("delete_disease_catalog_entry_v1", {
    p_disease_id: id,
    p_admin_id: admin.user.id,
    p_idempotency_key: idempotencyKey,
    p_payload_hash: payloadHash,
    p_destructive_approval: approval,
  });
  const result = outcome && typeof outcome === "object" && !Array.isArray(outcome)
    ? outcome as { outcome?: string; artwork_path?: string | null }
    : null;
  if (error || !result || !["applied", "unchanged", "missing"].includes(result.outcome ?? "")) {
    throw new Error(error?.message || "The disease could not be deleted. It may still be referenced by scan data.");
  }

  let cleanup = "complete";
  if (result.outcome === "applied" && existing.artwork_path) {
    const { error: cleanupError } = await supabase.storage.from("disease-catalog-artwork").remove([existing.artwork_path]);
    if (cleanupError) {
      cleanup = "pending";
      console.error("Disease artwork cleanup failed after catalog deletion", { id, path: existing.artwork_path, error: cleanupError.message });
    }
  }

  revalidatePath("/disease-catalog");
  redirect(`/disease-catalog?deleted=${encodeURIComponent(id)}&outcome=${encodeURIComponent(result.outcome ?? "missing")}&cleanup=${cleanup}`);
}

export default async function DiseaseContentEditor({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ outcome?: string }> }) {
  await requireAdmin(["owner", "admin"]);
  const { id } = await params;
  const supabase = getAdminClient();
  const query = await searchParams;
  const outcome = query.outcome;
  const { data: disease, error } = await supabase.from("disease_catalog").select("id,detector_supported,model_class_index,model_label,category,artwork_path,content_version,disease_localizations(*),disease_signs(*),disease_references(*)").eq("id", id).maybeSingle();
  if (error) throw new Error("The disease content could not be loaded.");
  if (!disease) notFound();
  const artworkUrl = disease.artwork_path ? supabase.storage.from("disease-catalog-artwork").getPublicUrl(disease.artwork_path).data.publicUrl : null;
  const english = disease.disease_localizations?.find((row) => row.language_tag === "en");

  return (
    <div className="fade-up mx-auto max-w-5xl">
      <Link href="/disease-catalog" className="inline-flex items-center gap-2 text-sm font-semibold text-[#1f6b3a]"><ArrowLeft size={17} />Back to catalog</Link>
      {outcome && <p role="status" className="status-banner mt-4 rounded-xl border border-[#bfe4c5] bg-[#f1fbf2] p-3 text-sm font-semibold text-[#247936]">{outcome === "unchanged" ? "This content was already up to date." : "Content update published."}</p>}
      <header className="mt-4 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold capitalize">{disease.id.replaceAll("-", " ")}</h1>
          <p className="mt-1 text-sm text-[#647166]">Edit bilingual educational content, symptoms, and citations.</p>
        </div>
        <div className="flex flex-wrap items-center justify-end gap-3">
          <div className={`rounded-xl px-3 py-2 font-mono text-xs ${disease.detector_supported === false ? "bg-[#edf7ee] text-[#247936]" : "bg-[#e7f2e6] text-[#1f6b3a]"}`}>
            <LockKeyhole className="mr-1 inline" size={14} />{disease.detector_supported === false ? "Library-only entry" : `Class ${disease.model_class_index}: ${disease.model_label} (read-only)`}
          </div>
          {disease.detector_supported === false && <ConfirmDeleteDiseaseButton action={deleteDisease} diseaseName={english?.name ?? disease.id} diseaseId={disease.id} idempotencyKey={randomUUID()} />}
        </div>
      </header>

      {artworkUrl && (
        <section className="surface mt-6 p-5">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div><h2 className="text-lg font-bold">Disease artwork</h2><p className="mt-1 text-sm text-[#68766b]">Whole-image preview of the public catalog artwork.</p></div>
            <span className="rounded-full bg-[#edf7ee] px-3 py-1 text-xs font-semibold text-[#247936]">JPEG · library asset</span>
          </div>
          <div className="mt-4 overflow-hidden rounded-2xl border border-[#d5e2d3] bg-[#f2f7ef] p-3">
            <Image src={artworkUrl} alt={`${english?.name ?? disease.id} artwork`} width={1200} height={900} unoptimized className="mx-auto max-h-[34rem] w-full rounded-xl object-contain" />
          </div>
        </section>
      )}

      <form action={saveContent} className="mt-6 grid gap-5">
        <input type="hidden" name="id" value={disease.id} />
        <input type="hidden" name="idempotency_key" value={randomUUID()} />
        {(["en", "fil"] as const).map((language) => {
          const content = disease.disease_localizations?.find((row) => row.language_tag === language);
          const signs = (disease.disease_signs ?? []).filter((row) => row.language_tag === language).sort((a, b) => a.position - b.position).map((row) => row.text);
          const reference = disease.disease_references?.find((row) => row.language_tag === language && row.position === 0);
          return <section className="surface p-5" key={language}><h2 className="text-xl font-bold">{language === "en" ? "English" : "Filipino"}</h2><div className="mt-4 grid gap-4 sm:grid-cols-2"><Field name={`${language}_name`} label="Name" value={content?.name} /><Field name={`${language}_symptom_preview`} label="Symptom preview" value={content?.symptom_preview} /></div><SignsArea name={`${language}_signs`} value={signs.join("\n")} />{(["description", "causes", "recommended_action", "prevention", "guidance", "when_to_act", "disclaimer"] as const).map((field) => <Area key={field} name={`${language}_${field}`} label={field === "recommended_action" ? "Recommended action" : field.replaceAll("_", " ")} value={content?.[field]} />)}<h3 className="mt-5 font-bold">Primary authoritative reference</h3><div className="mt-3 grid gap-4 sm:grid-cols-2"><Field name={`${language}_reference_publisher`} label="Publisher" value={reference?.publisher} /><Field name={`${language}_reference_title`} label="Title" value={reference?.title} /></div><Field name={`${language}_reference_url`} label="HTTPS URL" value={reference?.url} /></section>;
        })}
        <FormSubmitButton label="Publish content update" pendingLabel="Publishing content" className="h-12 bg-[#1f6b3a] px-5 text-white" />
      </form>
    </div>
  );
}

function Field({name,label,value}:{name:string;label:string;value?:string}){return <label className="mt-3 grid gap-1.5 text-sm font-semibold">{label}<input required maxLength={label === "HTTPS URL" ? 2_048 : 500} name={name} defaultValue={value ?? ""} className="focus-ring min-h-11 rounded-xl border border-[#d5e2d3] px-3 font-normal"/></label>}
function Area({name,label,value}:{name:string;label:string;value?:string}){return <label className="mt-4 grid gap-1.5 text-sm font-semibold capitalize">{label}<textarea required maxLength={4_000} name={name} defaultValue={value ?? ""} rows={3} className="focus-ring rounded-xl border border-[#d5e2d3] p-3 font-normal leading-6"/></label>}
function SignsArea({name,value}:{name:string;value:string}){return <label className="mt-4 grid gap-1.5 text-sm font-semibold">Symptoms (one per line)<textarea required maxLength={10_019} name={name} defaultValue={value} rows={5} className="focus-ring rounded-xl border border-[#d5e2d3] p-3 font-normal leading-6"/></label>}

function isSafeReference(reference: {publisher:string;title:string;url:string}) {
  if (!reference.publisher || reference.publisher.length > 500 || !reference.title || reference.title.length > 500 || reference.url.length > 2_048) return false;
  try {
    const url = new URL(reference.url);
    return url.protocol === "https:" && !url.username && !url.password && Boolean(url.hostname);
  } catch {
    return false;
  }
}
