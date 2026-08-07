"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell, BookOpen, ChevronDown, CircleHelp, ClipboardList, Flag, Globe2, LayoutDashboard, LogOut, Search, Settings, ShieldCheck } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { adminRoleLabel } from "@/lib/admin-copy";

type NavVisibility = "all" | "admin" | "owner";
type NavItem = readonly [label: string, href: string, icon: LucideIcon, visibility: NavVisibility];

const items: readonly NavItem[] = [
  ["Overview", "/overview", LayoutDashboard, "all"],
  ["Global scans", "/global-scans", Globe2, "all"],
  ["Reports", "/reports", Flag, "all"],
  ["Disease requests", "/disease-requests", ClipboardList, "all"],
  ["Disease catalog", "/disease-catalog", BookOpen, "all"],
  ["Notifications", "/notifications", Bell, "admin"],
  ["Settings", "/settings", Settings, "all"],
  ["Admin access", "/admin-members", ShieldCheck, "owner"],
];

function canSeeNavItem(visibility: NavVisibility, role: string) {
  return visibility === "all" || (visibility === "owner" && role === "owner") || (visibility === "admin" && (role === "owner" || role === "admin"));
}

export function AdminShell({ children, role, loginName, reportCount = 0 }: { children: ReactNode; role: string; loginName?: string | null; reportCount?: number }) {
  const pathname = usePathname();

  return (
    <div className="admin-shell">
      <aside className="admin-rail">
        <Link href="/overview" className="admin-brand focus-ring">
          <span className="admin-brand-mark"><Image src="/planta-logo.png" alt="" width={42} height={42} priority className="object-contain" /></span>
          <span className="admin-brand-copy"><strong>Planta</strong><small>Disease monitoring</small></span>
        </Link>
        <nav className="admin-nav" aria-label="Admin navigation">
          {items.filter(([, , , visibility]) => canSeeNavItem(visibility, role)).map(([label, href, Icon]) => {
            const selected = pathname === href || pathname.startsWith(`${href}/`);
            return <Link prefetch={false} key={href} href={href} aria-current={selected ? "page" : undefined} className={`admin-nav-item focus-ring ${selected ? "is-active" : ""}`}><Icon size={19} strokeWidth={1.8}/><span>{label}</span>{label === "Reports" && reportCount > 0 && <span aria-label={`${reportCount} reports`} className="admin-nav-count">{reportCount > 99 ? "99+" : reportCount}</span>}</Link>;
          })}
        </nav>
        <div className="admin-rail-footer">
          <div className="admin-workspace-switcher"><span className="admin-status-dot" aria-hidden="true" /><div><small>Workspace</small><strong>Planta Admin</strong></div><ChevronDown size={15} aria-hidden="true" /></div>
          <div className="admin-account-row"><span className="admin-account-avatar">{(loginName || "A").slice(0, 1).toUpperCase()}</span><div className="min-w-0"><strong className="block truncate">{loginName || "Admin"}</strong><small className="block truncate">{adminRoleLabel(role)}</small></div><CircleHelp size={16} aria-hidden="true" /></div>
          <form action="/auth/signout" method="post"><button className="admin-signout focus-ring"><LogOut size={14}/>Sign out</button></form>
        </div>
      </aside>
      <div className="admin-workspace">
        <header className="admin-topbar">
          <div className="admin-mobile-brand"><span className="admin-brand-mark"><Image src="/planta-logo.png" alt="" width={34} height={34} /></span><strong>Planta Admin</strong></div>
          <div className="admin-search" aria-label="Workspace search"><Search size={17} aria-hidden="true"/><span>Search this workspace</span><kbd>⌘ K</kbd></div>
          <div className="admin-topbar-actions"><span className="admin-health"><span className="admin-status-dot" aria-hidden="true"/>Authenticated workspace</span>{(role === "owner" || role === "admin") && <Link href="/notifications" className="admin-icon-button focus-ring" aria-label="Notifications"><Bell size={18}/><span className="admin-notification-dot" aria-hidden="true"/></Link>}<div className="admin-topbar-user"><span className="admin-account-avatar">{(loginName || "A").slice(0, 1).toUpperCase()}</span><span className="hidden sm:block"><strong className="block text-sm">{loginName || "Admin"}</strong><small className="block text-xs">{adminRoleLabel(role)}</small></span><ChevronDown size={15} aria-hidden="true" /></div></div>
        </header>
        <main className="admin-main min-w-0">{children}</main>
      </div>
    </div>
  );
}
