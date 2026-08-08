import { FormSubmitButton } from "@/components/form-submit-button";
import { requireAdmin } from "@/lib/auth";
import { ADMIN_LOGIN_NAME_HTML_PATTERN } from "@/lib/admin-credentials";
import { getAdminClient } from "@/lib/supabase/admin";
import { adminRoleLabel } from "@/lib/admin-copy";
import { ShieldCheck, UserPlus } from "lucide-react";
import { createAdmin, claimMyAdminUsername } from "./actions";
import {
  ADMIN_LOGIN_NAME_HELP,
  ADMIN_PASSWORD_HELP,
} from "./validation";

export const dynamic = "force-dynamic";

const errorMessages: Record<string, string> = {
  activation: "The Auth account was created but could not be activated. Submit the same username again to retry activation.",
  config: "Admin identity configuration is invalid. Check ADMIN_AUTH_EMAIL_DOMAIN.",
  invalid_name: "Use 1–64 letters, numbers, dots, underscores, or hyphens, starting with a letter or number.",
  invalid_password: "Use a password with at least 12 characters and do not use the username as the password.",
  invalid_role: "Choose a supported admin role.",
  name_taken: "That username is already reserved or in use.",
  provisioning: "The Auth account could not be provisioned. Try again or contact the application owner.",
  unavailable: "Admin management is temporarily unavailable. Try again shortly.",
};

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Unknown" : date.toLocaleString();
}

export default async function AdminMembersPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string; created?: string; claimed?: string }>;
}) {
  const admin = await requireAdmin(["owner"]);
  const { data: members, error } = await getAdminClient()
    .from("admin_members")
    .select("user_id,login_name,role,created_at")
    .order("created_at", { ascending: true });
  if (error) throw new Error("Admin accounts could not be loaded.");

  const query = await searchParams;
  const currentMember = (members ?? []).find((member) => member.user_id === admin.user.id);
  const message = query.error ? errorMessages[query.error] : null;
  const created = query.created === "1";
  const claimed = query.claimed === "1";

  return <div className="admin-page admin-members-page fade-up mx-auto max-w-5xl">
      <header>
        <h1 className="text-3xl font-bold tracking-[-.03em]">Admin access</h1>
      </header>

      {message && <p role="alert" className="mt-5 rounded-xl bg-[#fff0f2] p-3 text-sm font-semibold text-[#a92f40]">{message}</p>}
      {created && <p role="status" className="status-banner mt-5 rounded-xl border border-[#bfe4c5] bg-[#f1fbf2] p-3 text-sm font-semibold text-[#247936]">The admin account is ready. They can sign in with the username and password you entered.</p>}
      {claimed && <p role="status" className="status-banner mt-5 rounded-xl border border-[#bfe4c5] bg-[#f1fbf2] p-3 text-sm font-semibold text-[#247936]">Your owner account now has a username login.</p>}

      <div className="admin-access-grid mt-6">
      <section className="admin-member-form surface p-6">
        <div className="flex items-start gap-4">
          <span className="rounded-full bg-[#e7f2e6] p-3 text-[#1f6b3a]"><UserPlus size={22} aria-hidden="true" /></span>
          <div>
            <h2 className="text-lg font-bold">Add an admin</h2>
            <p className="mt-1 text-sm leading-6 text-[#5e6d61]">No email is required in this form. Choose a username, password, and the least-privileged role they need.</p>
          </div>
        </div>
        <form action={createAdmin} className="mt-6 grid gap-4 sm:grid-cols-2">
          <label className="grid gap-1.5 text-sm font-semibold">Username
            <input name="loginName" required maxLength={64} pattern={ADMIN_LOGIN_NAME_HTML_PATTERN} autoComplete="username" className="focus-ring h-11 rounded-xl border border-[#d5e2d3] bg-white px-3 font-normal" placeholder="admin123" />
            <span className="text-xs font-normal text-[#68766b]">{ADMIN_LOGIN_NAME_HELP}</span>
          </label>
          <label className="grid gap-1.5 text-sm font-semibold">Password
            <input name="password" required minLength={12} maxLength={128} autoComplete="new-password" type="password" className="focus-ring h-11 rounded-xl border border-[#d5e2d3] bg-white px-3 font-normal" placeholder="Use a strong password" />
            <span className="text-xs font-normal text-[#68766b]">{ADMIN_PASSWORD_HELP}</span>
          </label>
          <label className="grid gap-1.5 text-sm font-semibold sm:col-span-2">Role
            <select name="role" defaultValue="admin" className="focus-ring h-11 rounded-xl border border-[#d5e2d3] bg-white px-3 font-normal sm:max-w-sm">
              <option value="admin">Admin — catalog and review</option>
              <option value="reviewer">Reviewer — moderation and requests</option>
            </select>
          </label>
          <div className="sm:col-span-2"><FormSubmitButton label="Create admin account" pendingLabel="Creating admin account" className="bg-[#1f6b3a] px-4 text-white" /></div>
        </form>
      </section>

      <div className="admin-access-side">
      {!currentMember?.login_name && <section className="surface p-6">
        <div className="flex items-start gap-4">
          <span className="rounded-full bg-[#e9f6eb] p-3 text-[#278b3d]"><ShieldCheck size={22} aria-hidden="true" /></span>
          <div>
            <h2 className="text-lg font-bold">Set your owner username</h2>
            <p className="mt-1 text-sm leading-6 text-[#5e6d61]">Your current account still uses the temporary configured login alias. Assign a username here so future sign-ins use the same username lookup as other admins.</p>
          </div>
        </div>
        <form action={claimMyAdminUsername} className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-end">
          <label className="grid flex-1 gap-1.5 text-sm font-semibold">Your username
            <input name="loginName" required maxLength={64} pattern={ADMIN_LOGIN_NAME_HTML_PATTERN} autoComplete="username" className="focus-ring h-11 rounded-xl border border-[#d5e2d3] bg-white px-3 font-normal" placeholder="admin123" />
          </label>
          <FormSubmitButton label="Save username" pendingLabel="Saving username" className="bg-[#278b3d] px-4 text-white" />
        </form>
      </section>}

      <section className="admin-member-list surface overflow-hidden">
        <div className="border-b border-[#e5ece2] p-6"><h2 className="text-lg font-bold">Current admin accounts</h2><p className="mt-1 text-sm text-[#5e6d61]">Membership is required in addition to a valid Supabase Auth session.</p></div>
        <div className="overflow-x-auto">
          <table className="data-table w-full min-w-[38rem] text-left text-sm">
            <thead className="bg-[#fbfdf9] text-xs uppercase tracking-[.1em] text-[#68766b]"><tr><th className="px-6 py-3 font-semibold">Username</th><th className="px-6 py-3 font-semibold">Role</th><th className="px-6 py-3 font-semibold">Added</th></tr></thead>
            <tbody className="divide-y divide-[#e5ece2]">
              {(members ?? []).map((member) => <tr key={member.user_id}><td className="safe-long-content px-6 py-4 font-semibold">{member.login_name ?? "Legacy configured owner"}</td><td className="px-6 py-4">{adminRoleLabel(member.role)}</td><td className="px-6 py-4 text-[#68766b]">{formatDate(member.created_at)}</td></tr>)}
            </tbody>
          </table>
        </div>
      </section>
      </div>
      </div>
    </div>;
}
