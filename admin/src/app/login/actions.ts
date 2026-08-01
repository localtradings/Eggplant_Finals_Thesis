"use server";

import { redirect } from "next/navigation";
import { createSupabaseServerClient } from "@/lib/supabase/server";
import { parseAdminLoginFormData, readAdminLoginConfig } from "./credentials";

const INVALID_CREDENTIALS_PATH = "/login?error=invalid_credentials";
const CONFIGURATION_ERROR_PATH = "/login?error=config";

export async function signInAdmin(formData: FormData) {
  const input = parseAdminLoginFormData(formData);
  const config = readAdminLoginConfig(process.env);

  if (!input) {
    redirect(INVALID_CREDENTIALS_PATH);
  }
  if (!config) {
    redirect(CONFIGURATION_ERROR_PATH);
  }
  if (input.loginName !== config.loginName) {
    redirect(INVALID_CREDENTIALS_PATH);
  }

  const supabase = await createSupabaseServerClient();
  const { error } = await supabase.auth.signInWithPassword({
    email: config.email,
    password: input.password,
  });

  if (error) {
    redirect(INVALID_CREDENTIALS_PATH);
  }

  redirect("/overview");
}
