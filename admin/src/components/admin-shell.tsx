import Image from "next/image";
import Link from "next/link";
import { BookOpen, ClipboardList, Globe2, LayoutDashboard, LogOut, Settings, ShieldCheck } from "lucide-react";
import { RefreshPageButton } from "@/components/refresh-page-button";

const items = [
  ["Overview", "/overview", LayoutDashboard, false],
  ["Global scans", "/global-scans", Globe2, false],
  ["Disease requests", "/disease-requests", ClipboardList, false],
  ["Disease catalog", "/disease-catalog", BookOpen, false],
  ["Audit & settings", "/settings", Settings, false],
  ["Admin access", "/admin-members", ShieldCheck, true],
] as const;

export function AdminShell({ children, active, role }: { children: React.ReactNode; active: string; role: string }) {
  return (
    <div className="min-h-screen bg-[#f7f7f4] lg:grid lg:grid-cols-[276px_1fr]">
      <aside className="border-b border-white/10 bg-[#1b152b] px-5 py-5 text-white lg:relative lg:sticky lg:top-0 lg:h-screen lg:border-b-0 lg:border-r lg:border-[#2d2442]">
        <div className="flex items-center gap-3 px-2">
          <Image src="/eggplant-logo.svg" alt="" width={46} height={46} priority />
          <div><p className="text-xl font-bold tracking-tight">Eggplant</p><p className="text-sm font-semibold text-[#8dd49b]">Disease operations</p></div>
        </div>
        <div className="mt-7 flex flex-wrap items-center justify-between gap-2 rounded-xl border border-white/10 bg-white/[.06] px-3 py-2 text-xs font-semibold text-[#d9d1e8]"><span className="flex items-center gap-2"><span className="h-2 w-2 rounded-full bg-[#70d58a] shadow-[0_0_0_4px_rgba(112,213,138,.12)]"/>Production workspace</span><RefreshPageButton /></div>
        <nav className="mt-5 flex gap-2 overflow-x-auto pb-1 lg:grid lg:grid-cols-1" aria-label="Admin navigation">
          {items.filter(([, , , ownerOnly]) => !ownerOnly || role === "owner").map(([label, href, Icon]) => {
            const selected = active === href;
            return <Link prefetch={false} key={href} href={href} aria-current={selected ? "page" : undefined} className={`focus-ring flex min-h-11 shrink-0 items-center gap-3 rounded-xl px-3.5 text-sm font-semibold transition-all lg:w-full ${selected ? "bg-[#7344a6] text-white shadow-[0_10px_24px_rgba(0,0,0,.18)]" : "text-[#c8bfd8] hover:bg-white/[.08] hover:text-white"}`}><Icon size={19} strokeWidth={1.9}/><span>{label}</span></Link>;
          })}
        </nav>
        <div className="mt-6 rounded-2xl border border-white/10 bg-white/[.05] p-4 lg:absolute lg:bottom-5 lg:left-5 lg:right-5">
          <div className="flex items-center gap-2 text-sm font-semibold"><ShieldCheck size={17} className="text-[#8dd49b]"/>Private admin</div>
          <p className="mt-1 text-xs leading-5 text-[#bdb4cc]">Role: {role}. Moderation and content changes are audited.</p>
          <form action="/auth/signout" method="post" className="mt-3"><button className="focus-ring flex items-center gap-2 text-xs font-semibold text-[#d8b7ff] hover:text-white"><LogOut size={14}/>Sign out</button></form>
        </div>
      </aside>
      <main className="min-w-0 p-5 sm:p-7 lg:p-10">{children}</main>
    </div>
  );
}
