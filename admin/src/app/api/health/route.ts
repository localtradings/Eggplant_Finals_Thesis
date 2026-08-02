import { NextResponse } from "next/server";
import { readPublicMobileConfig } from "@/lib/mobile-config";
import { getAdminClient } from "@/lib/supabase/admin";

export const dynamic = "force-dynamic";

type HealthSchemaChecks = {
  appConfig: boolean;
  diseaseCatalog: boolean;
  scanContributions: boolean;
  diseaseRequests: boolean;
  rankings: boolean;
  installations: boolean;
  storageUsage: boolean;
};

const EMPTY_SCHEMA_CHECKS: HealthSchemaChecks = {
  appConfig: false,
  diseaseCatalog: false,
  scanContributions: false,
  diseaseRequests: false,
  rankings: false,
  installations: false,
  storageUsage: false,
};

function allChecksPassed(checks: HealthSchemaChecks) {
  return Object.values(checks).every(Boolean);
}

async function readSchemaChecks(
  supabase: ReturnType<typeof getAdminClient>,
): Promise<HealthSchemaChecks> {
  const [appConfig, diseaseCatalog, scanContributions, diseaseRequests, rankings, installations, storageUsage] = await Promise.all([
    supabase.from("app_config").select("id").eq("id", true).maybeSingle(),
    supabase.from("disease_catalog").select("id").limit(1),
    supabase.from("scan_contributions").select("id").limit(1),
    supabase.from("disease_requests").select("id").limit(1),
    supabase.from("global_disease_rankings").select("disease_id").limit(1),
    supabase.from("installations").select("owner_id").limit(1),
    supabase.rpc("admin_storage_usage"),
  ]);
  return {
    appConfig: !appConfig.error && appConfig.data !== null,
    diseaseCatalog: !diseaseCatalog.error,
    scanContributions: !scanContributions.error,
    diseaseRequests: !diseaseRequests.error,
    rankings: !rankings.error,
    installations: !installations.error,
    storageUsage: !storageUsage.error,
  };
}

export async function GET() {
  const writesConfigured = Boolean(process.env.SUPABASE_SECRET_KEY);
  let schemaChecks: HealthSchemaChecks = EMPTY_SCHEMA_CHECKS;

  if (writesConfigured) {
    try {
      const supabase = getAdminClient();
      schemaChecks = await readSchemaChecks(supabase);
      if (!allChecksPassed(schemaChecks)) {
        // A newly-warmed serverless instance can lose one initial pooled
        // request. Retry the complete probe once, while still surfacing a
        // persistent schema or credential failure as degraded.
        await new Promise((resolve) => setTimeout(resolve, 150));
        schemaChecks = await readSchemaChecks(supabase);
      }
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
