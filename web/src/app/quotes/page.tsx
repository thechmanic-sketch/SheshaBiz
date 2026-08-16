import Link from "next/link";
import { Plus } from "lucide-react";
import { QuoteBadge } from "@/components/ui/Badge";
import { formatCurrency, recentQuotes } from "@/lib/mock-data";

export default function QuotesPage() {
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

      <div className="mt-5 overflow-x-auto rounded-2xl border border-line bg-surface">
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
            {recentQuotes.map((quote) => (
              <tr key={quote.id} className="border-b border-line last:border-0">
                <td className="px-5 py-4 font-mono text-xs text-ink-faint">{quote.number}</td>
                <td className="px-5 py-4 font-semibold">{quote.customerName}</td>
                <td className="px-5 py-4 text-ink-soft">{quote.description}</td>
                <td className="px-5 py-4">
                  <QuoteBadge status={quote.status} />
                </td>
                <td className="px-5 py-4 text-right font-bold tabular">
                  {formatCurrency(quote.total)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
