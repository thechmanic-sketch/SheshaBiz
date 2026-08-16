"use client";

import Link from "next/link";
import { AlertTriangle, FileText, Receipt, ShoppingCart } from "lucide-react";
import { StatCard } from "@/components/ui/StatCard";
import { QuoteBadge } from "@/components/ui/Badge";
import { formatCurrency } from "@/lib/currency";
import { useAppData } from "@/lib/store";
import { effectiveInvoiceStatus, quoteTotals } from "@/lib/types";
import { useToday } from "@/lib/useToday";

const heroActions = [
  { href: "/quotes/new", label: "New Quote", icon: Receipt },
  { href: "/invoices/new", label: "New Invoice", icon: FileText },
  { href: "/pos", label: "New Sale", icon: ShoppingCart },
];

export default function DashboardPage() {
  const { data } = useAppData();
  const { business, quotes, invoices } = data;
  const today = useToday();

  const totalQuoted = quotes.reduce(
    (sum, q) => sum + quoteTotals(q, business.vatRate).total,
    0
  );
  const quotesCreated = quotes.length;
  const quotesSent = quotes.filter((q) => q.status !== "draft").length;
  const quotesAccepted = quotes.filter((q) => q.status === "accepted").length;
  const recentQuotes = quotes.slice(0, 4);
  const overdueInvoices = invoices.filter((i) => effectiveInvoiceStatus(i, today) === "overdue");

  return (
    <div className="mx-auto max-w-5xl">
      <div
        className="rounded-3xl p-6 text-white"
        style={{ background: "linear-gradient(135deg, var(--brand) 0%, var(--brand-deep) 100%)" }}
      >
        <p className="text-sm text-white/85">Total quoted</p>
        <p className="mt-1.5 text-3xl font-bold tabular">
          {formatCurrency(totalQuoted, business.country)}
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

      {overdueInvoices.length > 0 && (
        <Link
          href="/invoices"
          className="mt-5 flex items-center gap-3 rounded-2xl border border-error/30 bg-error/10 p-4 hover:border-error/50"
        >
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-error/15 text-error">
            <AlertTriangle size={18} />
          </span>
          <div>
            <p className="text-sm font-semibold text-error">
              {overdueInvoices.length} invoice{overdueInvoices.length === 1 ? "" : "s"} overdue
            </p>
            <p className="text-xs text-ink-faint">
              {formatCurrency(
                overdueInvoices.reduce((sum, i) => sum + quoteTotals(i, business.vatRate).total, 0),
                business.country
              )}{" "}
              outstanding past due date — tap to review
            </p>
          </div>
        </Link>
      )}

      <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard value={String(quotesCreated)} label="Quotes created" />
        <StatCard value={String(quotesSent)} label="Quotes sent" />
        <StatCard value={String(quotesAccepted)} label="Quotes accepted" />
        <StatCard value={formatCurrency(totalQuoted, business.country)} label="Total quoted" />
      </div>

      <div className="mt-8 flex items-center justify-between">
        <h2 className="text-lg font-semibold">Recent Quotes</h2>
        <Link href="/quotes" className="text-sm font-semibold text-brand-deep">
          See all
        </Link>
      </div>
      <div className="mt-3 flex flex-col gap-2.5">
        {recentQuotes.length === 0 && (
          <p className="rounded-2xl border border-line bg-surface p-4 text-sm text-ink-faint">
            No quotes yet.{" "}
            <Link href="/quotes/new" className="font-semibold text-brand-deep">
              Create your first quote
            </Link>
            .
          </p>
        )}
        {recentQuotes.map((quote) => (
          <Link
            key={quote.id}
            href={`/quotes/view?id=${quote.id}`}
            className="flex items-center justify-between rounded-2xl border border-line bg-surface p-4 hover:border-brand/40"
          >
            <div>
              <p className="font-semibold">{quote.customerName}</p>
              <p className="text-sm text-ink-faint">
                {quote.number} · {quote.description}
              </p>
            </div>
            <div className="text-right">
              <p className="font-bold tabular">
                {formatCurrency(quoteTotals(quote, business.vatRate).total, business.country)}
              </p>
              <div className="mt-1">
                <QuoteBadge status={quote.status} />
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
