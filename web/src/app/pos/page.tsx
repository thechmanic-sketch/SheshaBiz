"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Plus, Minus, Trash2, History, Package } from "lucide-react";
import { formatCurrency } from "@/lib/currency";
import { useAppData } from "@/lib/store";
import type { LineItem, PaymentMethod } from "@/lib/types";
import { lineItemsTotal } from "@/lib/types";

const paymentMethods: { value: PaymentMethod; label: string }[] = [
  { value: "cash", label: "Cash" },
  { value: "card", label: "Card" },
  { value: "eft", label: "EFT" },
];

export default function PosPage() {
  const { data, addSale } = useAppData();
  const router = useRouter();
  const [cart, setCart] = useState<LineItem[]>([]);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("cash");
  const [tendered, setTendered] = useState("");
  const [error, setError] = useState<string | null>(null);

  const total = lineItemsTotal(cart);
  const tenderedAmount = Number(tendered);
  const changeDue =
    paymentMethod === "cash" && tendered !== "" && !Number.isNaN(tenderedAmount)
      ? tenderedAmount - total
      : null;

  const canComplete = cart.length > 0 && (paymentMethod !== "cash" || (changeDue !== null && changeDue >= 0));

  function addProduct(productId: string) {
    const product = data.products.find((p) => p.id === productId);
    if (!product) return;
    setCart((c) => {
      const existing = c.find((item) => item.description === product.name);
      if (existing) {
        return c.map((item) =>
          item.id === existing.id ? { ...item, qty: item.qty + 1 } : item
        );
      }
      return [...c, { id: `cart-${product.id}`, description: product.name, qty: 1, unitPrice: product.price }];
    });
  }

  function adjustQty(id: string, delta: number) {
    setCart((c) =>
      c
        .map((item) => (item.id === id ? { ...item, qty: item.qty + delta } : item))
        .filter((item) => item.qty > 0)
    );
  }

  function removeItem(id: string) {
    setCart((c) => c.filter((item) => item.id !== id));
  }

  function completeSale() {
    if (!canComplete) {
      setError("Add items to the cart and make sure the amount tendered covers the total.");
      return;
    }
    setError(null);
    const sale = addSale({
      lineItems: cart,
      paymentMethod,
      amountTendered: paymentMethod === "cash" && tendered !== "" ? tenderedAmount : null,
      changeGiven: paymentMethod === "cash" && changeDue !== null ? Math.max(0, changeDue) : null,
    });
    setCart([]);
    setTendered("");
    router.push(`/pos/receipt?id=${sale.id}`);
  }

  const availableProducts = useMemo(() => data.products, [data.products]);

  return (
    <div className="mx-auto max-w-5xl">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">Point of Sale</h1>
        <Link
          href="/pos/history"
          className="flex items-center gap-1.5 rounded-xl border border-line px-3.5 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
        >
          <History size={16} />
          Sales history
        </Link>
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-[1fr_360px]">
        <div>
          {availableProducts.length === 0 ? (
            <div className="flex flex-col items-center gap-2 rounded-2xl border border-line bg-surface py-16 text-center text-ink-faint">
              <Package size={24} />
              <p>No products yet.</p>
              <Link href="/products" className="font-semibold text-brand-deep">
                Add a product
              </Link>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              {availableProducts.map((product) => (
                <button
                  key={product.id}
                  type="button"
                  onClick={() => addProduct(product.id)}
                  className="flex flex-col items-start rounded-2xl border border-line bg-surface p-4 text-left hover:border-brand/50"
                >
                  {product.imageDataUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={product.imageDataUrl}
                      alt=""
                      className="mb-2 h-16 w-16 rounded-xl object-cover"
                    />
                  ) : (
                    <span className="mb-2 flex h-16 w-16 items-center justify-center rounded-xl bg-brand-tint text-brand-deep">
                      <Package size={22} />
                    </span>
                  )}
                  <p className="text-sm font-semibold">{product.name}</p>
                  <p className="mt-0.5 text-sm font-bold tabular text-brand-deep">
                    {formatCurrency(product.price, data.business.country)}
                  </p>
                  <p className="mt-0.5 text-xs text-ink-faint">{product.stockQty} in stock</p>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="rounded-2xl border border-line bg-surface p-4">
          <h2 className="text-sm font-bold">Cart</h2>
          <div className="mt-3 flex flex-col gap-2">
            {cart.length === 0 && <p className="text-sm text-ink-faint">Tap a product to add it.</p>}
            {cart.map((item) => (
              <div key={item.id} className="flex items-center justify-between gap-2 text-sm">
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">{item.description}</p>
                  <p className="text-xs text-ink-faint tabular">
                    {formatCurrency(item.unitPrice, data.business.country)} each
                  </p>
                </div>
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => adjustQty(item.id, -1)}
                    className="flex h-6 w-6 items-center justify-center rounded-full border border-line text-ink-soft"
                  >
                    <Minus size={12} />
                  </button>
                  <span className="w-5 text-center tabular">{item.qty}</span>
                  <button
                    type="button"
                    onClick={() => adjustQty(item.id, 1)}
                    className="flex h-6 w-6 items-center justify-center rounded-full border border-line text-ink-soft"
                  >
                    <Plus size={12} />
                  </button>
                  <button
                    type="button"
                    onClick={() => removeItem(item.id)}
                    className="ml-1 flex h-6 w-6 items-center justify-center rounded-full text-ink-faint hover:text-error"
                  >
                    <Trash2 size={13} />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-4 flex items-center justify-between border-t border-line pt-3 text-base font-bold">
            <span>Total</span>
            <span className="tabular">{formatCurrency(total, data.business.country)}</span>
          </div>

          <div className="mt-4">
            <p className="text-xs font-semibold text-ink-faint">Payment method</p>
            <div className="mt-1.5 grid grid-cols-3 gap-1.5">
              {paymentMethods.map((method) => (
                <button
                  key={method.value}
                  type="button"
                  onClick={() => setPaymentMethod(method.value)}
                  className={`rounded-lg px-2 py-1.5 text-xs font-semibold ${
                    paymentMethod === method.value
                      ? "bg-brand text-white"
                      : "border border-line text-ink-soft"
                  }`}
                >
                  {method.label}
                </button>
              ))}
            </div>
          </div>

          {paymentMethod === "cash" && (
            <div className="mt-4">
              <label className="text-xs font-semibold text-ink-faint">Amount tendered</label>
              <input
                type="number"
                min={0}
                step="0.01"
                value={tendered}
                onChange={(e) => setTendered(e.target.value)}
                placeholder="0.00"
                className="mt-1 w-full rounded-xl border border-line bg-paper px-3 py-2 text-sm tabular outline-none focus:border-brand"
              />
              {tendered !== "" && (
                <p
                  className={`mt-1.5 text-sm font-semibold tabular ${
                    (changeDue ?? 0) < 0 ? "text-error" : "text-brand-deep"
                  }`}
                >
                  {(changeDue ?? 0) < 0
                    ? `Short by ${formatCurrency(Math.abs(changeDue ?? 0), data.business.country)}`
                    : `Change due: ${formatCurrency(changeDue ?? 0, data.business.country)}`}
                </p>
              )}
            </div>
          )}

          {error && <p className="mt-3 text-sm font-medium text-error">{error}</p>}

          <button
            type="button"
            onClick={completeSale}
            disabled={!canComplete}
            className="mt-4 w-full rounded-xl bg-brand py-2.5 text-sm font-semibold text-white disabled:opacity-40"
          >
            Complete sale
          </button>
        </div>
      </div>
    </div>
  );
}
