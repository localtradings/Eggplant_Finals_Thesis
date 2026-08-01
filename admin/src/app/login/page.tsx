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
  return <main className="grid min-h-screen place-items-center bg-[radial-gradient(circle_at_top,#f0eafa,transparent_45%)] p-5"><section className="surface fade-up w-full max-w-md p-7 sm:p-9"><div className="flex items-center gap-3"><Image src="/eggplant-logo.svg" width={50} height={50} alt=""/><div><h1 className="text-2xl font-bold tracking-tight">Eggplant Admin</h1><p className="text-sm font-semibold text-[#278b3d]">Private operations dashboard</p></div></div>{errorMessage && <p role="alert" className="mt-5 rounded-xl bg-[#fff0f2] p-3 text-sm text-[#a92f40]">{errorMessage}</p>}<LoginForm/></section></main>;
}
