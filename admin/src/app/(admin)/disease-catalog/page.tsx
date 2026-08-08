import { BookOpen, CheckCircle2, ExternalLink } from "lucide-react";
import Link from "next/link";
import { CatalogArtwork } from "@/components/catalog-artwork";
import { StatusToast } from "@/components/status-toast";
import { requireAdmin } from "@/lib/auth";
import { getCatalogArtworkUrl, getCatalogDisplayCategory } from "@/lib/catalog-artwork";
import { HEALTHY_MODEL_CLASSES } from "@/lib/model-classes";
import { getAdminClient } from "@/lib/supabase/admin";

export const dynamic = "force-dynamic";

export default async function CatalogPage({ searchParams }: { searchParams: Promise<{ published?: string; deleted?: string; outcome?: string; cleanup?: string }> }) {
  const admin = await requireAdmin();
  const supabase = getAdminClient();
  const { data = [], error } = await supabase
    .from("disease_catalog")
    .select("id,category,detector_supported,model_class_index,model_label,artwork_path,content_version,disease_localizations(language_tag,name,symptom_preview,causes,guidance),disease_references(language_tag,publisher,title,url)")
    .order("model_class_index");
  const diseases = data ?? [];
  const params = await searchParams;
  const published = params.published;
  const deleted = params.deleted;
  const publishedName = diseases.find((disease) => disease.id === published)?.disease_localizations?.find((row) => row.language_tag === "en")?.name ?? published;
  const catalogEntries = [
    ...diseases.map((disease) => ({ kind: "disease" as const, disease })),
    ...HEALTHY_MODEL_CLASSES
      .filter(({ index }) => !diseases.some((disease) => disease.model_class_index === index))
      .map((modelClass) => ({ kind: "healthy" as const, modelClass })),
  ].sort((left, right) => {
    const leftIndex = left.kind === "disease" ? (left.disease.model_class_index ?? Number.MAX_SAFE_INTEGER) : left.modelClass.index;
    const rightIndex = right.kind === "disease" ? (right.disease.model_class_index ?? Number.MAX_SAFE_INTEGER) : right.modelClass.index;
    return leftIndex - rightIndex;
  });

  return (
    <div className="admin-page catalog-page fade-up mx-auto max-w-[1240px]">
      <header>
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div><h1 className="text-3xl font-bold tracking-[-.03em]">Disease catalog</h1><p className="mt-1 text-sm text-[#686a69]">Manage the model map and publish library-only disease education.</p></div>
          {(admin.role === "owner" || admin.role === "admin") && <Link href="/disease-catalog/new" className="focus-ring inline-flex h-11 items-center rounded-xl bg-[#278b3d] px-4 font-semibold text-white transition hover:bg-[#1e7131]">Add disease</Link>}
        </div>
      </header>
      {published && <StatusToast><CheckCircle2 size={17} />{publishedName || "Disease content"} {params.outcome === "unchanged" ? "was already published." : "published successfully."}</StatusToast>}
      {deleted && <p role="status" className={`status-banner mt-5 rounded-xl border p-3 text-sm font-semibold ${params.cleanup === "pending" ? "border-[#eadfca] bg-[#fffaf0] text-[#6f5c35]" : "border-[#bfe4c5] bg-[#f1fbf2] text-[#247936]"}`}>{params.cleanup === "pending" ? `${deleted} was deleted, but its artwork cleanup needs a retry.` : `${deleted} was deleted from the library catalog.`}</p>}
      {error ? <p role="alert" className="mt-6 rounded-xl bg-[#fff0f2] p-4 text-sm text-[#a92f40]">The disease catalog is temporarily unavailable.</p> : <section className="catalog-grid mt-6" aria-label="Disease and model class catalog">
        {catalogEntries.map((entry, index) => {
          if (entry.kind === "healthy") {
            return <div className="catalog-card catalog-card--locked surface border-[#d8e5d9] bg-[#fbfef9] p-5" key={`healthy-${entry.modelClass.index}`} aria-label={`Model class ${entry.modelClass.index}: ${entry.modelClass.label}`}><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-semibold uppercase tracking-[.12em] text-[#278b3d]">Healthy model output</p><h2 className="mt-1 text-xl font-bold">{entry.modelClass.label}</h2></div><span className="rounded-lg bg-[#edf2ed] px-2 py-1 font-mono text-xs text-[#2f4732]">Class {entry.modelClass.index}</span></div><p className="mt-3 text-sm leading-6 text-[#5e6d61]">{entry.modelClass.description}</p><p className="mt-4 text-xs font-semibold uppercase tracking-[.1em] text-[#6b756d]">Read-only model mapping</p></div>;
          }
          const disease = entry.disease;
          const en = disease.disease_localizations?.find((localization) => localization.language_tag === "en");
          const remoteArtworkUrl = disease.artwork_path ? supabase.storage.from("disease-catalog-artwork").getPublicUrl(disease.artwork_path).data.publicUrl : null;
          const artworkUrl = getCatalogArtworkUrl(disease.id, remoteArtworkUrl);
          const displayCategory = getCatalogDisplayCategory(disease.id, disease.category);
          return <Link href={`/disease-catalog/${disease.id}`} key={disease.id} className="catalog-card surface motion-card overflow-hidden"><CatalogArtwork src={artworkUrl} version={disease.content_version} alt={`${en?.name ?? disease.id} artwork`} priority={index < 3} /><div className="catalog-card-body"><div className="catalog-meta"><p className="text-xs font-semibold uppercase tracking-[.12em] text-[#278b3d]">{displayCategory.replaceAll("_", " ")}</p><span className={`shrink-0 rounded-lg px-2 py-1 font-mono text-xs ${disease.detector_supported === false ? "bg-[#edf7ee] text-[#247936]" : "bg-[#eaf4e8] text-[#1f6b3a]"}`}>{disease.detector_supported === false ? "Library only" : `Class ${disease.model_class_index}`}</span></div><h2 className="mt-3 font-bold">{en?.name ?? disease.id}</h2><p className="mt-3 text-sm leading-6 text-[#5e6d61]">{en?.symptom_preview || "Content will be published from the bundled offline catalog."}</p><div className="mt-4 flex flex-wrap gap-2">{disease.disease_references?.filter((reference) => reference.language_tag === "en").map((reference) => <span className="inline-flex items-center gap-1 text-xs font-semibold text-[#1f6b3a]" key={reference.url}>{reference.publisher}<ExternalLink size={12}/></span>)}</div></div></Link>;
        })}
      </section>}
      <div className="mt-5 rounded-xl border border-[#cfe1cb] bg-[#f3f8f1] p-4 text-sm text-[#536458]"><BookOpen className="mr-2 inline text-[#1f6b3a]" size={18}/>Publishing increases the catalog version while keeping disease IDs and model mappings stable. Library-only entries can be deleted only when no shared scan references them. Detector classes, including healthy outputs, remain read-only.</div>
    </div>
  );
}
