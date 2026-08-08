import Image from "next/image";
import { LoginForm } from "./login-form";

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ error?: string }> }) {
  const { error } = await searchParams;
  const errorMessage = error === "config"
    ? "Admin authentication is not configured."
    : error === "invalid_credentials"
      ? "Invalid admin name or password."
      : error === "not_authorized"
        ? "This account is not authorized for the dashboard."
        : error === "unavailable"
          ? "Admin authentication is temporarily unavailable. Try again shortly."
          : error === "callback"
            ? "That sign-in link is invalid or expired."
            : null;

  return (
    <main className="admin-login">
      <section className="admin-login-visual" aria-label="Planta Admin workspace introduction">
        <Image
          src="/design-references/admin-login-hero.png"
          alt="Eggplant plants in a field"
          fill
          priority
          sizes="(min-width: 1024px) 52vw, 0px"
          className="object-cover"
        />
        <div className="admin-login-visual-content">
          <div className="admin-login-brand">
            <Image src="/planta-logo.png" width={44} height={44} alt="" />
            <div>
              <strong>Planta</strong>
              <span>Admin workspace</span>
            </div>
          </div>
          <div className="admin-login-message">
            <span className="admin-login-message-kicker">A clearer view of every scan</span>
            <h2>Keep the garden moving.</h2>
            <p>
              Review shared photos, publish disease guidance, and keep the mobile catalog current from one calm, controlled workspace.
            </p>
          </div>
          <div className="admin-login-footnote">Authenticated access · audited actions</div>
        </div>
      </section>
      <section className="admin-login-panel">
        <div className="admin-login-mobile-brand">
          <Image src="/planta-logo.png" width={38} height={38} alt="" />
          <strong>Planta Admin</strong>
        </div>
        <div className="admin-login-heading">
          <p className="admin-login-heading-kicker">Sign in</p>
          <h1>Welcome back.</h1>
          <p>Use your admin name and password to open the workspace.</p>
        </div>
        {errorMessage && <p role="alert" className="admin-login-error">{errorMessage}</p>}
        <LoginForm />
      </section>
    </main>
  );
}
