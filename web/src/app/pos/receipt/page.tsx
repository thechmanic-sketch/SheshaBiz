"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Printer, ShoppingCart } from "lucide-react";
import { DocumentPreview } from "@/components/documents/DocumentPreview";
import { useAppData } from "@/lib/store";
import { lineItemsTotal } from "@/lib/types";

const methodLabel = { cash: "Cash", card: "Card", eft: "EFT" } as const;

function ReceiptInner() {
  const params = useSearchParams();
  const id = params.get("id") ?? "";
  const { data } = useAppData();
  const sale = data.sales.find((s) => s.id === id);

  if (!sale) {
    return (
      <div className="mx-auto max-w-md py-16 text-center">
        <p className="text-ink-faint">Receipt not found.</p>
        <Link href="/pos" className="mt-3 inline-block font-semibold text-brand-deep">
          Back to POS
        </Link>
      </div>
    );
  }

  const total = lineItemsTotal(sale.lineItems);
  const extraRows =
    sale.paymentMethod === "cash" && sale.amountTendered != null
      ? [
          { label: "Cash tendered", value: sale.amountTendered },
          { label: "Change", value: sale.changeGiven ?? 0 },
        ]
      : undefined;

  return (
    <div>
      <div className="mx-auto flex max-w-2xl flex-wrap items-center justify-between gap-3 no-print">
        <h1 className="text-xl font-bold">{sale.number}</h1>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => window.print()}
            className="flex items-center gap-1.5 rounded-xl border border-line px-3.5 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
          >
            <Printer size={15} />
            Print
          </button>
          <Link
            href="/pos"
            className="flex items-center gap-1.5 rounded-xl bg-brand px-3.5 py-2 text-sm font-semibold text-white"
          >
            <ShoppingCart size={15} />
            New sale
          </Link>
        </div>
      </div>

      <div className="mt-5">
        <DocumentPreview
          kind="Receipt"
          number={sale.number}
          date={sale.createdAt}
          customerMeta={`Paid by ${methodLabel[sale.paymentMethod]}`}
          lineItems={sale.lineItems}
          subtotal={total}
          vat={0}
          vatRate={0}
          total={total}
          extraRows={extraRows}
        />
      </div>
    </div>
  );
}

export default function ReceiptPage() {
  return (
    <Suspense fallback={null}>
      <ReceiptInner />
    </Suspense>
  );
}
