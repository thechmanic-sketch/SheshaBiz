"use client";

import { useMemo, useState } from "react";
import { Printer } from "lucide-react";
import { formatCurrency } from "@/lib/currency";
import { useAppData } from "@/lib/store";
import { effectiveInvoiceStatus, lineItemsTotal, quoteTotals } from "@/lib/types";
import { useToday } from "@/lib/useToday";

type Period = "today" | "week" | "month" | "all";

const periods: { value: Period; label: string }[] = [
  { value: "today", label: "Today" },
  { value: "week", label: "Week" },
  { value: "month", label: "Month" },
  { value: "all", label: "All" },
];

function cutoffFor(period: Period): Date | null {
  const now = new Date();
  if (period === "today") {
    return new Date(now.getFullYear(), now.getMonth(), now.getDate());
  }
  if (period === "week") {
    return new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
  }
  if (period === "month") {
    return new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
  }
  return null;
}

function StatRow({ label, value, warn }: { label: string; value: string; warn?: boolean }) {
  return (
    <div className="flex items-center justify-between border-b border-line py-3 last:border-0">
      <span className="text-sm text-ink-soft">{label}</span>
      <span className={`font-bold tabular ${warn ? "text-error" : ""}`}>{value}</span>
    </div>
  );
}

export default function ReportsPage() {
  const { data, visibleQuotes, visibleInvoices, visibleSales } = useAppData();
  const [period, setPeriod] = useState<Period>("month");
  const today = useToday();

  const stats = useMemo(() => {
    const cutoff = cutoffFor(period);
    const inRange = (dateStr: string) => (cutoff ? new Date(dateStr) >= cutoff : true);

    const quotes = visibleQuotes.filter((q) => inRange(q.createdAt));
    const invoices = visibleInvoices.filter((i) => inRange(i.createdAt));
    const sales = visibleSales.filter((s) => inRange(s.createdAt));

    const quotesTotal = quotes.reduce((sum, q) => sum + quoteTotals(q, data.business.vatRate).total, 0);
    const quotesAccepted = quotes.filter((q) => q.status === "accepted").length;

    const paid = invoices.filter((i) => i.status === "paid");
    const unpaid = invoices.filter((i) => effectiveInvoiceStatus(i, today) === "unpaid");
    const overdue = invoices.filter((i) => effectiveInvoiceStatus(i, today) === "overdue");

    const salesTotal = sales.reduce((sum, s) => sum + lineItemsTotal(s.lineItems), 0);

    return {
      quotesCreated: quotes.length,
      quotesAccepted,
      quotesTotal,
      invoicesPaidTotal: paid.reduce((sum, i) => sum + quoteTotals(i, data.business.vatRate).total, 0),
      invoicesPaidCount: paid.length,
      invoicesUnpaidTotal: unpaid.reduce((sum, i) => sum + quoteTotals(i, data.business.vatRate).total, 0),
      invoicesUnpaidCount: unpaid.length,
      invoicesOverdueCount: overdue.length,
      salesTotal,
      salesCount: sales.length,
    };
  }, [data, visibleQuotes, visibleInvoices, visibleSales, period, today]);

  const country = data.business.country;

  return (
    <div className="mx-auto max-w-2xl">
      <div className="flex items-center justify-between no-print">
        <h1 className="text-xl font-bold">Reports</h1>
        <button
          type="button"
          onClick={() => window.print()}
          className="flex items-center gap-1.5 rounded-xl border border-line px-3.5 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
        >
          <Printer size={15} />
          Print report
        </button>
      </div>

      <div className="mt-4 flex gap-2 no-print">
        {periods.map((p) => (
          <button
            key={p.value}
            type="button"
            onClick={() => setPeriod(p.value)}
            className={`rounded-full px-4 py-1.5 text-sm font-semibold ${
              period === p.value ? "bg-brand text-white" : "border border-line text-ink-soft"
            }`}
          >
            {p.label}
          </button>
        ))}
      </div>

      <div className="mt-2 hidden print:block">
        <h1 className="text-lg font-bold">{data.business.name} — {periods.find((p) => p.value === period)?.label} report</h1>
        <p className="text-sm text-ink-faint">{today}</p>
      </div>

      <div className="mt-5 rounded-2xl border border-line bg-surface p-5">
        <h2 className="text-xs font-bold uppercase tracking-wide text-ink-faint">Point of Sale</h2>
        <StatRow label="Sales completed" value={String(stats.salesCount)} />
        <StatRow label="Total sales" value={formatCurrency(stats.salesTotal, country)} />
      </div>

      <div className="mt-4 rounded-2xl border border-line bg-surface p-5">
        <h2 className="text-xs font-bold uppercase tracking-wide text-ink-faint">Quotations</h2>
        <StatRow label="Quotes created" value={String(stats.quotesCreated)} />
        <StatRow label="Quotes accepted" value={String(stats.quotesAccepted)} />
        <StatRow label="Total quoted" value={formatCurrency(stats.quotesTotal, country)} />
      </div>

      <div className="mt-4 rounded-2xl border border-line bg-surface p-5">
        <h2 className="text-xs font-bold uppercase tracking-wide text-ink-faint">Invoices</h2>
        <StatRow label="Paid" value={`${stats.invoicesPaidCount} · ${formatCurrency(stats.invoicesPaidTotal, country)}`} />
        <StatRow label="Unpaid" value={`${stats.invoicesUnpaidCount} · ${formatCurrency(stats.invoicesUnpaidTotal, country)}`} />
        <StatRow label="Overdue" value={String(stats.invoicesOverdueCount)} warn={stats.invoicesOverdueCount > 0} />
      </div>
    </div>
  );
}
