import { KeyRound, UserRound } from "lucide-react";
import { FormSubmitButton } from "@/components/form-submit-button";
import { ADMIN_LOGIN_NAME_HTML_PATTERN } from "@/lib/admin-credentials";
import { signInAdmin } from "./actions";

export function LoginForm() {
  return (
    <form action={signInAdmin} className="admin-login-form">
      <label>
        <span>Admin name</span>
        <div className="admin-login-field">
          <UserRound size={17} aria-hidden="true" />
          <input
            required
            name="name"
            type="text"
            autoComplete="username"
            maxLength={64}
            pattern={ADMIN_LOGIN_NAME_HTML_PATTERN}
            placeholder="admin"
          />
        </div>
      </label>
      <label>
        <span>Password</span>
        <div className="admin-login-field">
          <KeyRound size={17} aria-hidden="true" />
          <input
            required
            name="password"
            type="password"
            autoComplete="current-password"
            maxLength={128}
            placeholder="Enter your password"
          />
        </div>
      </label>
      <FormSubmitButton
        label="Sign in"
        pendingLabel="Signing in"
        className="admin-login-submit w-full text-white"
        icon={<KeyRound size={17} aria-hidden="true" />}
      />
      <p className="admin-login-note">
        Only configured administrators can sign in. Dashboard access still requires an allowlisted Supabase admin membership.
      </p>
    </form>
  );
}
