"use client";

import { formatCurrency } from "@/lib/currency";
import { useAppData } from "@/lib/store";
import type { LineItem } from "@/lib/types";

export function DocumentPreview({
  id,
  kind,
  number,
  date,
  customerName,
  customerMeta,
  lineItems,
  subtotal,
  vat,
  vatRate,
  total,
  extraRows,
  footerNote,
}: {
  id?: string;
  kind: string;
  number: string;
  date: string;
  customerName?: string;
  customerMeta?: string;
  lineItems: LineItem[];
  subtotal: number;
  vat: number;
  vatRate: number;
  total: number;
  extraRows?: { label: string; value: number }[];
  footerNote?: string;
}) {
  const { data } = useAppData();
  const { business } = data;

  return (
    <div
      id={id}
      className="mx-auto max-w-2xl rounded-2xl border border-line bg-surface p-8 print:rounded-none print:border-0 print:p-0"
    >
      <div className="flex items-start justify-between border-b border-line pb-5">
        <div className="flex items-center gap-3">
          {business.logoDataUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={business.logoDataUrl} alt="" className="h-12 w-12 rounded-full object-cover" />
          ) : (
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-brand-tint text-sm font-bold text-brand-deep">
              {business.name.slice(0, 2).toUpperCase()}
            </div>
          )}
          <div>
            <p className="font-bold">{business.name}</p>
            {business.regNumber && <p className="text-xs text-ink-faint">Reg {business.regNumber}</p>}
          </div>
        </div>
        <div className="text-right">
          <p className="text-xs font-semibold uppercase tracking-wide text-brand-deep">{kind}</p>
          <p className="mt-0.5 text-sm font-mono text-ink-faint">{number}</p>
          <p className="text-xs text-ink-faint">{date}</p>
        </div>
      </div>

      {customerName && (
        <div className="mt-5">
          <p className="text-xs font-semibold uppercase tracking-wide text-ink-faint">Billed to</p>
          <p className="mt-1 font-semibold">{customerName}</p>
          {customerMeta && <p className="text-sm text-ink-faint">{customerMeta}</p>}
        </div>
      )}

      <div className="mt-5 overflow-x-auto rounded-xl border border-line">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-line text-left text-ink-faint">
              <th className="px-3 py-2 font-medium">Description</th>
              <th className="px-3 py-2 text-right font-medium">Qty</th>
              <th className="px-3 py-2 text-right font-medium">Unit price</th>
              <th className="px-3 py-2 text-right font-medium">Total</th>
            </tr>
          </thead>
          <tbody>
            {lineItems.map((item) => (
              <tr key={item.id} className="border-b border-line last:border-0">
                <td className="px-3 py-2">{item.description}</td>
                <td className="px-3 py-2 text-right tabular">{item.qty}</td>
                <td className="px-3 py-2 text-right tabular">
                  {formatCurrency(item.unitPrice, business.country)}
                </td>
                <td className="px-3 py-2 text-right font-semibold tabular">
                  {formatCurrency(item.qty * item.unitPrice, business.country)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-4 ml-auto max-w-xs text-sm">
        {vatRate > 0 && (
          <>
            <div className="flex justify-between text-ink-soft">
              <span>Subtotal</span>
              <span className="tabular">{formatCurrency(subtotal, business.country)}</span>
            </div>
            <div className="mt-1 flex justify-between text-ink-soft">
              <span>VAT ({vatRate}%)</span>
              <span className="tabular">{formatCurrency(vat, business.country)}</span>
            </div>
          </>
        )}
        <div className="mt-2 flex items-center justify-between rounded-xl bg-brand px-3 py-2.5 text-base font-bold text-white">
          <span>Total</span>
          <span className="tabular">{formatCurrency(total, business.country)}</span>
        </div>
        {extraRows?.map((row) => (
          <div key={row.label} className="mt-2 flex justify-between text-ink-soft">
            <span>{row.label}</span>
            <span className="tabular">{formatCurrency(row.value, business.country)}</span>
          </div>
        ))}
      </div>

      {(footerNote || business.bankName) && (
        <div className="mt-6 border-t border-line pt-4 text-xs text-ink-faint">
          {business.bankName && (
            <p>
              Banking details: {business.bankName} · Acc {business.accountNumber} · Branch code{" "}
              {business.branchCode}
            </p>
          )}
          {footerNote && <p className="mt-1">{footerNote}</p>}
        </div>
      )}
    </div>
  );
}
