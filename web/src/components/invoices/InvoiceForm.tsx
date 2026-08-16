"use client";

import { useState } from "react";
import { LineItemsEditor, emptyLineItem } from "@/components/ui/LineItemsEditor";
import { formatCurrency } from "@/lib/currency";
import { useAppData } from "@/lib/store";
import { quoteTotals, type Invoice, type InvoiceStatus, type LineItem } from "@/lib/types";

const inputCls =
  "w-full rounded-xl border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-brand";
const labelCls = "text-xs font-semibold text-ink-faint";

function defaultDueDate(): string {
  return new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
}

export interface InvoiceFormValues {
  customerId: string | null;
  customerName: string;
  status: InvoiceStatus;
  dueDate: string;
  lineItems: LineItem[];
}

export function InvoiceForm({
  initial,
  onSave,
  submitLabel = "Save invoice",
}: {
  initial?: Invoice;
  onSave: (values: InvoiceFormValues) => void;
  submitLabel?: string;
}) {
  const { data, visibleCustomers, visibleProducts } = useAppData();
  const [customerId, setCustomerId] = useState<string | null>(initial?.customerId ?? null);
  const [customerName, setCustomerName] = useState(initial?.customerName ?? "");
  const [status, setStatus] = useState<InvoiceStatus>(initial?.status ?? "unpaid");
  const [dueDate, setDueDate] = useState(initial?.dueDate ?? defaultDueDate());
  const [lineItems, setLineItems] = useState<LineItem[]>(
    initial?.lineItems?.length ? initial.lineItems : [emptyLineItem()]
  );
  const [error, setError] = useState<string | null>(null);

  const totals = quoteTotals({ lineItems }, data.business.vatRate);

  function handleCustomerSelect(id: string) {
    if (!id) {
      setCustomerId(null);
      return;
    }
    const customer = visibleCustomers.find((c) => c.id === id);
    if (customer) {
      setCustomerId(customer.id);
      setCustomerName(customer.name);
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!customerName.trim()) {
      setError("Add a customer name.");
      return;
    }
    const cleanItems = lineItems.filter((item) => item.description.trim() !== "");
    if (cleanItems.length === 0) {
      setError("Add at least one line item.");
      return;
    }
    setError(null);
    onSave({ customerId, customerName: customerName.trim(), status, dueDate, lineItems: cleanItems });
  }

  return (
    <form onSubmit={handleSubmit} className="mx-auto max-w-3xl">
      <div className="rounded-2xl border border-line bg-surface p-5">
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className={labelCls}>Existing customer</label>
            <select
              className={`${inputCls} mt-1`}
              value={customerId ?? ""}
              onChange={(e) => handleCustomerSelect(e.target.value)}
            >
              <option value="">— Type a new name —</option>
              {visibleCustomers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className={labelCls}>Customer name</label>
            <input
              className={`${inputCls} mt-1`}
              value={customerName}
              onChange={(e) => {
                setCustomerName(e.target.value);
                setCustomerId(null);
              }}
              placeholder="Customer name"
              required
            />
          </div>
        </div>

        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <div>
            <label className={labelCls}>Due date</label>
            <input
              type="date"
              className={`${inputCls} mt-1`}
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
            />
          </div>
          <div>
            <label className={labelCls}>Status</label>
            <select
              className={`${inputCls} mt-1`}
              value={status}
              onChange={(e) => setStatus(e.target.value as InvoiceStatus)}
            >
              <option value="unpaid">Unpaid</option>
              <option value="paid">Paid</option>
              <option value="overdue">Overdue</option>
              <option value="cancelled">Cancelled</option>
            </select>
          </div>
        </div>
      </div>

      <div className="mt-5">
        <h2 className="mb-2 text-sm font-bold">Line items</h2>
        <LineItemsEditor items={lineItems} onChange={setLineItems} products={visibleProducts} />
      </div>

      <div className="mt-5 ml-auto max-w-xs rounded-2xl border border-line bg-surface p-4 text-sm">
        <div className="flex justify-between text-ink-soft">
          <span>Subtotal</span>
          <span className="tabular">{formatCurrency(totals.subtotal, data.business.country)}</span>
        </div>
        <div className="mt-1 flex justify-between text-ink-soft">
          <span>VAT ({data.business.vatRate}%)</span>
          <span className="tabular">{formatCurrency(totals.vat, data.business.country)}</span>
        </div>
        <div className="mt-2 flex justify-between border-t border-line pt-2 text-base font-bold">
          <span>Total</span>
          <span className="tabular">{formatCurrency(totals.total, data.business.country)}</span>
        </div>
      </div>

      {error && <p className="mt-4 text-sm font-medium text-error">{error}</p>}

      <div className="mt-5 flex justify-end">
        <button type="submit" className="rounded-xl bg-brand px-5 py-2.5 text-sm font-semibold text-white">
          {submitLabel}
        </button>
      </div>
    </form>
  );
}
