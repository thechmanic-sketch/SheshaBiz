import Link from "next/link";
import { FileText, Receipt, ShoppingCart } from "lucide-react";
import { StatCard } from "@/components/ui/StatCard";
import { QuoteBadge } from "@/components/ui/Badge";
import { dashboardStats, formatCurrency, recentQuotes } from "@/lib/mock-data";

const heroActions = [
  { href: "/quotes/new", label: "New Quote", icon: Receipt },
  { href: "/invoices/new", label: "New Invoice", icon: FileText },
  { href: "/pos", label: "New Sale", icon: ShoppingCart },
];

export default function DashboardPage() {
  return (
    <div className="mx-auto max-w-5xl">
      <div
        className="rounded-3xl p-6 text-white"
        style={{ background: "linear-gradient(135deg, var(--brand) 0%, var(--brand-deep) 100%)" }}
      >
        <p className="text-sm text-white/85">Total quoted</p>
        <p className="mt-1.5 text-3xl font-bold tabular">
          {formatCurrency(dashboardStats.totalQuoted)}
        </p>
        <div className="mt-5 flex gap-8">
          {heroActions.map((action) => {
            const Icon = action.icon;
            return (
              <Link
                key={action.label}
                href={action.href}
                className="flex flex-col items-center gap-1.5 text-xs font-medium"
              >
                <span className="flex h-11 w-11 items-center justify-center rounded-full bg-white/20">
                  <Icon size={20} />
                </span>
                {action.label}
              </Link>
            );
          })}
        </div>
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard value={String(dashboardStats.quotesCreated)} label="Quotes created" />
        <StatCard value={String(dashboardStats.quotesSent)} label="Quotes sent" />
        <StatCard value={String(dashboardStats.quotesAccepted)} label="Quotes accepted" />
        <StatCard value={formatCurrency(dashboardStats.totalQuoted)} label="Total quoted" />
      </div>

      <div className="mt-8 flex items-center justify-between">
        <h2 className="text-lg font-semibold">Recent Quotes</h2>
        <Link href="/quotes" className="text-sm font-semibold text-brand-deep">
          See all
        </Link>
      </div>
      <div className="mt-3 flex flex-col gap-2.5">
        {recentQuotes.map((quote) => (
          <div
            key={quote.id}
            className="flex items-center justify-between rounded-2xl border border-line bg-surface p-4"
          >
            <div>
              <p className="font-semibold">{quote.customerName}</p>
              <p className="text-sm text-ink-faint">
                {quote.number} · {quote.description}
              </p>
            </div>
            <div className="text-right">
              <p className="font-bold tabular">{formatCurrency(quote.total)}</p>
              <div className="mt-1">
                <QuoteBadge status={quote.status} />
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
