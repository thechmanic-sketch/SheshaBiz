import Link from "next/link";
import { Plus } from "lucide-react";
import { InvoiceBadge } from "@/components/ui/Badge";
import { formatCurrency, invoices } from "@/lib/mock-data";

export default function InvoicesPage() {
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

      <div className="mt-5 overflow-x-auto rounded-2xl border border-line bg-surface">
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
            {invoices.map((invoice) => (
              <tr key={invoice.id} className="border-b border-line last:border-0">
                <td className="px-5 py-4 font-mono text-xs text-ink-faint">{invoice.number}</td>
                <td className="px-5 py-4 font-semibold">{invoice.customerName}</td>
                <td className="px-5 py-4 text-ink-soft">{invoice.dueDate}</td>
                <td className="px-5 py-4">
                  <InvoiceBadge status={invoice.status} />
                </td>
                <td className="px-5 py-4 text-right font-bold tabular">
                  {formatCurrency(invoice.total)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
