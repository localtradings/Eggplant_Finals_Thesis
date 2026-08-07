import Link from "next/link";
import Image from "next/image";
import { ArrowUpRight, ClipboardCheck, Globe2, Leaf, UsersRound } from "lucide-react";
import type { ReactNode } from "react";
import type { DashboardData } from "@/lib/dashboard-data";
import { RefreshDashboardButton } from "@/components/refresh-dashboard-button";
import { LiveActivityTimestamp } from "@/components/live-activity-timestamp";
import { scanStatusLabel, scanStatusTone } from "@/lib/admin-copy";

function Metric({ label, value, icon: Icon, href }: { label: string; value: number; icon: typeof Globe2; href?: string }) {
  const content = (
    <div className="metric-card">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="metric-card-label">{label}</p>
          <p className="metric-card-value">{value.toLocaleString()}</p>
        </div>
        <span className="metric-card-icon" aria-hidden="true"><Icon size={18} /></span>
      </div>
      <span className="metric-sparkline" aria-hidden="true" />
    </div>
  );
  return href ? <Link href={href} className="focus-ring block rounded-[13px]">{content}</Link> : content;
}

export function Dashboard({ data }: { data: DashboardData }) {
  const maxRank = Math.max(1, ...data.rankings.map((r) => r.count));
  return (
    <div className="admin-page fade-up mx-auto max-w-[1240px]">
      <header className="page-header flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="page-kicker">Planta workspace</p>
          <h1 className="page-title mt-2">Overview</h1>
          <p className="page-description mt-2">Shared scans, requests, and reports in one place.</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <RefreshDashboardButton />
        </div>
      </header>
      <section className="metric-grid mt-8" aria-label="Operational metrics">
        <Metric label="Reports received" value={data.reportsReceived} icon={ClipboardCheck} href="/reports" />
        <Metric label="Shared scans" value={data.sharedScans} icon={Globe2} />
        <Metric label="Contributing installs" value={data.installations} icon={UsersRound} />
        <Metric label="Open requests" value={data.openRequests} icon={Leaf} />
      </section>
      <div className="workspace-status mt-8">
        <section className="status-panel" aria-label="System status">
          <Ops label="Mobile submissions" value={data.cloudWritesEnabled ? "On" : "Paused"} hint="Global scans and disease requests" />
          <Ops label="Photo storage" value={formatBytes(data.storageBytes)} hint="Private review images" />
          <Ops label="Last app activity" value={<LiveActivityTimestamp timestamp={data.lastInstallationSeenAt} />} hint="Most recent cloud activity · auto-refreshes" />
        </section>
        <div className="visual-anchor" aria-hidden="true"><Image src="/design-references/botanical-texture.png" alt="" fill sizes="(min-width: 800px) 30vw, 100vw" className="object-cover" /><div className="visual-anchor-copy"><strong>Keep the catalog current.</strong><span>Review the latest field activity from one workspace.</span></div></div>
      </div>
      <div className="workspace-layout mt-3">
        <section className="workspace-panel">
          <div className="workspace-panel-header"><h2>Recent shared scans</h2><Link href="/global-scans">View all <ArrowUpRight className="ml-1 inline" size={15} /></Link></div>
          {data.recent.length === 0 ? <Empty text="No community photos have been shared yet." /> : <div className="overflow-x-auto"><table className="data-table"><thead><tr><th>Scan</th><th>Confidence</th><th>Status</th><th><span className="sr-only">Open</span></th></tr></thead><tbody>{data.recent.map((scan) => <tr key={scan.id}><td><span className="block font-semibold text-[#274e35]">{scan.disease}</span><span className="mt-1 block text-[11px] text-[#879084]">Community contribution</span></td><td className="font-mono font-semibold text-[#3e7849]">{scan.confidence}%</td><td><span className={`rounded-full px-2 py-1 text-[11px] font-semibold ${scanStatusTone(scan.status)}`}>{scanStatusLabel(scan.status)}</span></td><td><Link href={`/global-scans/${scan.id}`} aria-label={`Review ${scan.disease}`} className="text-[#2f6b3c]"><ArrowUpRight size={17} aria-hidden="true" /></Link></td></tr>)}</tbody></table></div>}
        </section>
        <section className="workspace-panel">
          <div className="workspace-panel-header"><h2>Disease ranking</h2><Link href="/global-scans">Full analysis <ArrowUpRight className="ml-1 inline" size={15} /></Link></div>
          {data.rankings.length === 0 ? <Empty text="Rankings will appear after users share confirmed scans." /> : <ol className="ranking-list">{data.rankings.map((rank, index) => <li key={rank.diseaseId} className="ranking-row"><span className="ranking-row-label"><span className="mr-2 font-mono text-[10px] text-[#9aa594]">{String(index + 1).padStart(2, "0")}</span>{rank.name}</span><span className="ranking-row-value">{rank.count}</span><span className="ranking-row-track"><span style={{ width: `${rank.count / maxRank * 100}%` }} /></span></li>)}</ol>}
        </section>
      </div>
    </div>
  );
}

function Empty({ text }: { text: string }) { return <div className="m-5 rounded-xl border border-dashed border-[#d6dec9] bg-[#fafbf4] p-8 text-center text-sm text-[#6b7469]">{text}</div>; }
function Ops({ label, value, hint }: { label: string; value: ReactNode; hint: string }) { return <div className="status-cell"><p className="status-cell-label">{label}</p><p className="status-cell-value truncate">{value}</p><p className="status-cell-hint truncate">{hint}</p></div>; }
function formatBytes(bytes: number) { if (bytes < 1024) return `${bytes} B`; if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`; return `${(bytes / 1024 / 1024).toFixed(1)} MB`; }
