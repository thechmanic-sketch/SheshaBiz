"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { formatCurrency } from "@/lib/currency";
import { useAppData } from "@/lib/store";
import { lineItemsTotal } from "@/lib/types";

const methodLabel = { cash: "Cash", card: "Card", eft: "EFT" } as const;

export default function SalesHistoryPage() {
  const { data, visibleSales } = useAppData();
  const router = useRouter();

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex items-center gap-3">
        <Link
          href="/pos"
          className="flex h-9 w-9 items-center justify-center rounded-full border border-line text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
        >
          <ArrowLeft size={16} />
        </Link>
        <h1 className="text-xl font-bold">Sales history</h1>
      </div>

      <div className="mt-5 overflow-x-auto rounded-2xl border border-line bg-surface">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-line text-left text-ink-faint">
              <th className="px-5 py-3 font-medium">Receipt</th>
              <th className="px-5 py-3 font-medium">Date</th>
              <th className="px-5 py-3 font-medium">Items</th>
              <th className="px-5 py-3 font-medium">Payment</th>
              <th className="px-5 py-3 text-right font-medium">Total</th>
            </tr>
          </thead>
          <tbody>
            {visibleSales.map((sale) => (
              <tr
                key={sale.id}
                onClick={() => router.push(`/pos/receipt?id=${sale.id}`)}
                className="cursor-pointer border-b border-line last:border-0 hover:bg-black/[.02] dark:hover:bg-white/[.03]"
              >
                <td className="px-5 py-4 font-mono text-xs text-ink-faint">{sale.number}</td>
                <td className="px-5 py-4 text-ink-soft">{sale.createdAt}</td>
                <td className="px-5 py-4 text-ink-soft">{sale.lineItems.length} item(s)</td>
                <td className="px-5 py-4 text-ink-soft">{methodLabel[sale.paymentMethod]}</td>
                <td className="px-5 py-4 text-right font-bold tabular">
                  {formatCurrency(lineItemsTotal(sale.lineItems), data.business.country)}
                </td>
              </tr>
            ))}
            {visibleSales.length === 0 && (
              <tr>
                <td colSpan={5} className="px-5 py-10 text-center text-ink-faint">
                  No sales yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
