"use client";

import { Plus, Trash2 } from "lucide-react";
import type { LineItem, Product } from "@/lib/types";
import { formatCurrency } from "@/lib/currency";
import { useAppData } from "@/lib/store";

const inputCls =
  "w-full rounded-lg border border-line bg-paper px-2.5 py-1.5 text-sm outline-none focus:border-brand";

let counter = 0;
/** A real UUID, not just a locally-unique string: quote_items/invoice_items
 * /sale_items has a `uuid` primary key, and the sync engine pushes each
 * line item's local id verbatim (see `web/src/lib/sync.ts`). */
function newLineId(): string {
  counter += 1;
  if (typeof crypto !== "undefined" && crypto.randomUUID) return crypto.randomUUID();
  return `id-${Date.now()}-${counter}`;
}

export function emptyLineItem(): LineItem {
  return { id: newLineId(), description: "", qty: 1, unitPrice: 0 };
}

export function LineItemsEditor({
  items,
  onChange,
  products = [],
}: {
  items: LineItem[];
  onChange: (items: LineItem[]) => void;
  products?: Product[];
}) {
  const { data } = useAppData();

  function update(id: string, patch: Partial<LineItem>) {
    onChange(items.map((item) => (item.id === id ? { ...item, ...patch } : item)));
  }

  function remove(id: string) {
    onChange(items.filter((item) => item.id !== id));
  }

  function addBlank() {
    onChange([...items, emptyLineItem()]);
  }

  function addFromProduct(productId: string) {
    const product = products.find((p) => p.id === productId);
    if (!product) return;
    onChange([...items, { id: newLineId(), description: product.name, qty: 1, unitPrice: product.price }]);
  }

  return (
    <div>
      <div className="overflow-x-auto rounded-xl border border-line">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-line text-left text-ink-faint">
              <th className="px-3 py-2 font-medium">Description</th>
              <th className="px-3 py-2 font-medium w-20">Qty</th>
              <th className="px-3 py-2 font-medium w-28">Unit price</th>
              <th className="px-3 py-2 text-right font-medium w-28">Total</th>
              <th className="w-9" />
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id} className="border-b border-line last:border-0">
                <td className="px-3 py-2">
                  <input
                    className={inputCls}
                    value={item.description}
                    placeholder="Item description"
                    onChange={(e) => update(item.id, { description: e.target.value })}
                  />
                </td>
                <td className="px-3 py-2">
                  <input
                    type="number"
                    min={0}
                    step="0.01"
                    className={`${inputCls} tabular`}
                    value={item.qty}
                    onChange={(e) => update(item.id, { qty: Number(e.target.value) || 0 })}
                  />
                </td>
                <td className="px-3 py-2">
                  <input
                    type="number"
                    min={0}
                    step="0.01"
                    className={`${inputCls} tabular`}
                    value={item.unitPrice}
                    onChange={(e) => update(item.id, { unitPrice: Number(e.target.value) || 0 })}
                  />
                </td>
                <td className="px-3 py-2 text-right font-semibold tabular">
                  {formatCurrency(item.qty * item.unitPrice, data.business.country)}
                </td>
                <td className="px-1 py-2 text-center">
                  <button
                    type="button"
                    aria-label="Remove line"
                    onClick={() => remove(item.id)}
                    className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-faint hover:bg-error/10 hover:text-error"
                  >
                    <Trash2 size={15} />
                  </button>
                </td>
              </tr>
            ))}
            {items.length === 0 && (
              <tr>
                <td colSpan={5} className="px-3 py-6 text-center text-ink-faint">
                  No line items yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={addBlank}
          className="flex items-center gap-1.5 rounded-lg border border-line px-3 py-1.5 text-xs font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
        >
          <Plus size={14} />
          Add line
        </button>
        {products.length > 0 && (
          <select
            defaultValue=""
            onChange={(e) => {
              if (e.target.value) addFromProduct(e.target.value);
              e.target.value = "";
            }}
            className="rounded-lg border border-line bg-paper px-3 py-1.5 text-xs font-semibold text-ink-soft outline-none"
          >
            <option value="" disabled>
              Add from catalog…
            </option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} — {formatCurrency(p.price, data.business.country)}
              </option>
            ))}
          </select>
        )}
      </div>
    </div>
  );
}
