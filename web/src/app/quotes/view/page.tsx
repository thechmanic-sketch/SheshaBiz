"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Pencil, Printer, Send, CheckCircle2, XCircle, ArrowRightLeft, Trash2 } from "lucide-react";
import { QuoteBadge } from "@/components/ui/Badge";
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
  icon: typeof Send;
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

function QuoteViewInner() {
  const router = useRouter();
  const params = useSearchParams();
  const id = params.get("id") ?? "";
  const { data, updateQuote, deleteQuote, convertQuoteToInvoice } = useAppData();
  const [confirmDelete, setConfirmDelete] = useState(false);

  const quote = data.quotes.find((q) => q.id === id);

  if (!quote) {
    return (
      <div className="mx-auto max-w-md py-16 text-center">
        <p className="text-ink-faint">Quote not found.</p>
        <Link href="/quotes" className="mt-3 inline-block font-semibold text-brand-deep">
          Back to Quotes
        </Link>
      </div>
    );
  }

  const totals = quoteTotals(quote, data.business.vatRate);
  const linkedInvoice = quote.convertedToInvoiceId
    ? data.invoices.find((i) => i.id === quote.convertedToInvoiceId)
    : null;

  function handleConvert() {
    const dueDate = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
    const invoice = convertQuoteToInvoice(quote!.id, dueDate);
    if (invoice) router.push(`/invoices/view?id=${invoice.id}`);
  }

  return (
    <div>
      <div className="mx-auto flex max-w-2xl flex-wrap items-center justify-between gap-3 no-print">
        <div className="flex items-center gap-2">
          <h1 className="text-xl font-bold">{quote.number}</h1>
          <QuoteBadge status={quote.status} />
        </div>
        <div className="flex flex-wrap gap-2">
          <ActionButton onClick={() => router.push(`/quotes/edit?id=${quote.id}`)} icon={Pencil} label="Edit" />
          {quote.status === "draft" && (
            <ActionButton onClick={() => updateQuote(quote.id, { status: "sent" })} icon={Send} label="Mark sent" />
          )}
          {quote.status === "sent" && (
            <>
              <ActionButton
                onClick={() => updateQuote(quote.id, { status: "accepted" })}
                icon={CheckCircle2}
                label="Mark accepted"
              />
              <ActionButton
                onClick={() => updateQuote(quote.id, { status: "rejected" })}
                icon={XCircle}
                label="Mark rejected"
              />
            </>
          )}
          {quote.status === "accepted" && !quote.convertedToInvoiceId && (
            <ActionButton onClick={handleConvert} icon={ArrowRightLeft} label="Convert to invoice" primary />
          )}
          <ActionButton onClick={() => window.print()} icon={Printer} label="Print" />
          <ActionButton onClick={() => setConfirmDelete(true)} icon={Trash2} label="Delete" danger />
        </div>
      </div>

      {linkedInvoice && (
        <p className="mx-auto mt-2 max-w-2xl text-sm text-ink-faint no-print">
          Converted to{" "}
          <Link href={`/invoices/view?id=${linkedInvoice.id}`} className="font-semibold text-brand-deep">
            {linkedInvoice.number}
          </Link>
          .
        </p>
      )}

      <div className="mt-5">
        <DocumentPreview
          kind="Quotation"
          number={quote.number}
          date={quote.createdAt}
          customerName={quote.customerName}
          customerMeta={quote.description}
          lineItems={quote.lineItems}
          subtotal={totals.subtotal}
          vat={totals.vat}
          vatRate={data.business.vatRate}
          total={totals.total}
          footerNote={data.business.paymentTerms}
        />
      </div>

      <ConfirmDialog
        open={confirmDelete}
        title="Delete this quote?"
        message={`${quote.number} for ${quote.customerName} will be permanently removed.`}
        confirmLabel="Delete"
        danger
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => {
          deleteQuote(quote.id);
          router.push("/quotes");
        }}
      />
    </div>
  );
}

export default function QuoteViewPage() {
  return (
    <Suspense fallback={null}>
      <QuoteViewInner />
    </Suspense>
  );
}
