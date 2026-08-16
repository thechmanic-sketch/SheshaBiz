"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { QuoteBadge } from "@/components/ui/Badge";
import { SearchInput } from "@/components/ui/SearchInput";
import { FilterChips } from "@/components/ui/FilterChips";
import { formatCurrency } from "@/lib/currency";
import { useAppData } from "@/lib/store";
import { quoteTotals, type QuoteStatus } from "@/lib/types";

type StatusFilter = QuoteStatus | "all";

const statusOptions: { value: StatusFilter; label: string }[] = [
  { value: "all", label: "All" },
  { value: "draft", label: "Draft" },
  { value: "sent", label: "Sent" },
  { value: "accepted", label: "Accepted" },
  { value: "rejected", label: "Rejected" },
];

export default function QuotesPage() {
  const { data } = useAppData();
  const router = useRouter();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<StatusFilter>("all");

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return data.quotes.filter((quote) => {
      if (status !== "all" && quote.status !== status) return false;
      if (!q) return true;
      return (
        quote.number.toLowerCase().includes(q) ||
        quote.customerName.toLowerCase().includes(q) ||
        quote.description.toLowerCase().includes(q)
      );
    });
  }, [data.quotes, search, status]);

  return (
    <div className="mx-auto max-w-5xl">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">Quotes</h1>
        <Link
          href="/quotes/new"
          className="flex items-center gap-1.5 rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white"
        >
          <Plus size={16} />
          New Quote
        </Link>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2.5">
        <SearchInput value={search} onChange={setSearch} placeholder="Search quotes…" />
        <FilterChips options={statusOptions} value={status} onChange={setStatus} />
      </div>

      <div className="mt-4 overflow-x-auto rounded-2xl border border-line bg-surface">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-line text-left text-ink-faint">
              <th className="px-5 py-3 font-medium">Quote</th>
              <th className="px-5 py-3 font-medium">Customer</th>
              <th className="px-5 py-3 font-medium">Description</th>
              <th className="px-5 py-3 font-medium">Status</th>
              <th className="px-5 py-3 text-right font-medium">Total</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((quote) => (
              <tr
                key={quote.id}
                onClick={() => router.push(`/quotes/view?id=${quote.id}`)}
                className="cursor-pointer border-b border-line last:border-0 hover:bg-black/[.02] dark:hover:bg-white/[.03]"
              >
                <td className="px-5 py-4 font-mono text-xs text-ink-faint">
                  <Link href={`/quotes/view?id=${quote.id}`} className="hover:underline">
                    {quote.number}
                  </Link>
                </td>
                <td className="px-5 py-4 font-semibold">{quote.customerName}</td>
                <td className="px-5 py-4 text-ink-soft">{quote.description}</td>
                <td className="px-5 py-4">
                  <QuoteBadge status={quote.status} />
                </td>
                <td className="px-5 py-4 text-right font-bold tabular">
                  {formatCurrency(quoteTotals(quote, data.business.vatRate).total, data.business.country)}
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={5} className="px-5 py-10 text-center text-ink-faint">
                  {data.quotes.length === 0 ? "No quotes yet." : "No quotes match your search."}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
