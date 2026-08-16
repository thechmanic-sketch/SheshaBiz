"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { InvoiceBadge } from "@/components/ui/Badge";
import { SearchInput } from "@/components/ui/SearchInput";
import { FilterChips } from "@/components/ui/FilterChips";
import { formatCurrency } from "@/lib/currency";
import { useAppData } from "@/lib/store";
import { effectiveInvoiceStatus, quoteTotals, type InvoiceStatus } from "@/lib/types";
import { useToday } from "@/lib/useToday";

type StatusFilter = InvoiceStatus | "all";

const statusOptions: { value: StatusFilter; label: string }[] = [
  { value: "all", label: "All" },
  { value: "unpaid", label: "Unpaid" },
  { value: "paid", label: "Paid" },
  { value: "overdue", label: "Overdue" },
  { value: "cancelled", label: "Cancelled" },
];

export default function InvoicesPage() {
  const { data } = useAppData();
  const router = useRouter();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<StatusFilter>("all");
  const today = useToday();

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return data.invoices.filter((invoice) => {
      if (status !== "all" && effectiveInvoiceStatus(invoice, today) !== status) return false;
      if (!q) return true;
      return (
        invoice.number.toLowerCase().includes(q) || invoice.customerName.toLowerCase().includes(q)
      );
    });
  }, [data.invoices, search, status, today]);

  return (
    <div className="mx-auto max-w-5xl">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">Invoices</h1>
        <Link
          href="/invoices/new"
          className="flex items-center gap-1.5 rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white"
        >
          <Plus size={16} />
          New Invoice
        </Link>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2.5">
        <SearchInput value={search} onChange={setSearch} placeholder="Search invoices…" />
        <FilterChips options={statusOptions} value={status} onChange={setStatus} />
      </div>

      <div className="mt-4 overflow-x-auto rounded-2xl border border-line bg-surface">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-line text-left text-ink-faint">
              <th className="px-5 py-3 font-medium">Invoice</th>
              <th className="px-5 py-3 font-medium">Customer</th>
              <th className="px-5 py-3 font-medium">Due date</th>
              <th className="px-5 py-3 font-medium">Status</th>
              <th className="px-5 py-3 text-right font-medium">Total</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((invoice) => (
              <tr
                key={invoice.id}
                onClick={() => router.push(`/invoices/view?id=${invoice.id}`)}
                className="cursor-pointer border-b border-line last:border-0 hover:bg-black/[.02] dark:hover:bg-white/[.03]"
              >
                <td className="px-5 py-4 font-mono text-xs text-ink-faint">
                  <Link href={`/invoices/view?id=${invoice.id}`} className="hover:underline">
                    {invoice.number}
                  </Link>
                </td>
                <td className="px-5 py-4 font-semibold">{invoice.customerName}</td>
                <td className="px-5 py-4 text-ink-soft">{invoice.dueDate}</td>
                <td className="px-5 py-4">
                  <InvoiceBadge status={effectiveInvoiceStatus(invoice, today)} />
                </td>
                <td className="px-5 py-4 text-right font-bold tabular">
                  {formatCurrency(quoteTotals(invoice, data.business.vatRate).total, data.business.country)}
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={5} className="px-5 py-10 text-center text-ink-faint">
                  {data.invoices.length === 0 ? "No invoices yet." : "No invoices match your search."}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
