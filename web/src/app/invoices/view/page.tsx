"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Pencil, Printer, CheckCircle2, RotateCcw, Trash2 } from "lucide-react";
import { InvoiceBadge } from "@/components/ui/Badge";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { DocumentPreview } from "@/components/documents/DocumentPreview";
import { useAppData } from "@/lib/store";
import { quoteTotals } from "@/lib/types";

function ActionButton({
  onClick,
  icon: Icon,
  label,
  primary,
  danger,
}: {
  onClick: () => void;
  icon: typeof Pencil;
  label: string;
  primary?: boolean;
  danger?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex items-center gap-1.5 rounded-xl px-3.5 py-2 text-sm font-semibold ${
        primary
          ? "bg-brand text-white"
          : danger
            ? "text-error hover:bg-error/10"
            : "border border-line text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
      }`}
    >
      <Icon size={15} />
      {label}
    </button>
  );
}

function InvoiceViewInner() {
  const router = useRouter();
  const params = useSearchParams();
  const id = params.get("id") ?? "";
  const { data, updateInvoice, deleteInvoice } = useAppData();
  const [confirmDelete, setConfirmDelete] = useState(false);

  const invoice = data.invoices.find((i) => i.id === id);

  if (!invoice) {
    return (
      <div className="mx-auto max-w-md py-16 text-center">
        <p className="text-ink-faint">Invoice not found.</p>
        <Link href="/invoices" className="mt-3 inline-block font-semibold text-brand-deep">
          Back to Invoices
        </Link>
      </div>
    );
  }

  const totals = quoteTotals(invoice, data.business.vatRate);

  return (
    <div>
      <div className="mx-auto flex max-w-2xl flex-wrap items-center justify-between gap-3 no-print">
        <div className="flex items-center gap-2">
          <h1 className="text-xl font-bold">{invoice.number}</h1>
          <InvoiceBadge status={invoice.status} />
        </div>
        <div className="flex flex-wrap gap-2">
          <ActionButton
            onClick={() => router.push(`/invoices/edit?id=${invoice.id}`)}
            icon={Pencil}
            label="Edit"
          />
          {invoice.status !== "paid" && (
            <ActionButton
              onClick={() => updateInvoice(invoice.id, { status: "paid" })}
              icon={CheckCircle2}
              label="Mark paid"
              primary
            />
          )}
          {invoice.status === "paid" && (
            <ActionButton
              onClick={() => updateInvoice(invoice.id, { status: "unpaid" })}
              icon={RotateCcw}
              label="Mark unpaid"
            />
          )}
          <ActionButton onClick={() => window.print()} icon={Printer} label="Print" />
          <ActionButton onClick={() => setConfirmDelete(true)} icon={Trash2} label="Delete" danger />
        </div>
      </div>

      <div className="mt-5">
        <DocumentPreview
          kind="Tax Invoice"
          number={invoice.number}
          date={invoice.createdAt}
          customerName={invoice.customerName}
          customerMeta={`Due ${invoice.dueDate}`}
          lineItems={invoice.lineItems}
          subtotal={totals.subtotal}
          vat={totals.vat}
          vatRate={data.business.vatRate}
          total={totals.total}
          footerNote={data.business.paymentTerms}
        />
      </div>

      <ConfirmDialog
        open={confirmDelete}
        title="Delete this invoice?"
        message={`${invoice.number} for ${invoice.customerName} will be permanently removed.`}
        confirmLabel="Delete"
        danger
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => {
          deleteInvoice(invoice.id);
          router.push("/invoices");
        }}
      />
    </div>
  );
}

export default function InvoiceViewPage() {
  return (
    <Suspense fallback={null}>
      <InvoiceViewInner />
    </Suspense>
  );
}
