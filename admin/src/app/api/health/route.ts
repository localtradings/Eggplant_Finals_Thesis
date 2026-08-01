import { NextResponse } from "next/server";
import { readPublicMobileConfig } from "@/lib/mobile-config";
import { getAdminClient } from "@/lib/supabase/admin";

export const dynamic = "force-dynamic";

export async function GET() {
  const writesConfigured = Boolean(process.env.SUPABASE_SECRET_KEY);
  const schemaChecks = {
    appConfig: false,
    diseaseCatalog: false,
    scanContributions: false,
    diseaseRequests: false,
    rankings: false,
    installations: false,
    storageUsage: false,
  };

  if (writesConfigured) {
    try {
      const supabase = getAdminClient();
      const [appConfig, diseaseCatalog, scanContributions, diseaseRequests, rankings, installations, storageUsage] = await Promise.all([
        supabase.from("app_config").select("id").eq("id", true).maybeSingle(),
        supabase.from("disease_catalog").select("id").limit(1),
        supabase.from("scan_contributions").select("id").limit(1),
        supabase.from("disease_requests").select("id").limit(1),
        supabase.from("global_disease_rankings").select("disease_id").limit(1),
        supabase.from("installations").select("owner_id").limit(1),
        supabase.rpc("admin_storage_usage"),
      ]);
      schemaChecks.appConfig = !appConfig.error && appConfig.data !== null;
      schemaChecks.diseaseCatalog = !diseaseCatalog.error;
      schemaChecks.scanContributions = !scanContributions.error;
      schemaChecks.diseaseRequests = !diseaseRequests.error;
      schemaChecks.rankings = !rankings.error;
      schemaChecks.installations = !installations.error;
      schemaChecks.storageUsage = !storageUsage.error;
    } catch {
      // Keep the public health response safe and useful when the server client
      // cannot be initialized; never return database error text or secrets.
    }
  }
  const databaseConfigured = Object.values(schemaChecks).every(Boolean);
  return NextResponse.json({
    status: writesConfigured && databaseConfigured ? "ok" : "degraded",
    service: "eggplant-disease-admin",
    writesConfigured,
    mobileConfigConfigured: readPublicMobileConfig() !== null,
    databaseConfigured,
    schemaChecks,
  });
}
