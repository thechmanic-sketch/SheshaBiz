"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { InvoiceForm, type InvoiceFormValues } from "@/components/invoices/InvoiceForm";
import { useAppData } from "@/lib/store";
import { TRIAL_LOCK_MESSAGE, useTrialGate } from "@/lib/trial";

function EditInvoiceInner() {
  const router = useRouter();
  const params = useSearchParams();
  const id = params.get("id") ?? "";
  const { data, updateInvoice } = useAppData();
  const { locked } = useTrialGate();
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

  if (locked) {
    return (
      <div className="mx-auto max-w-md py-16 text-center">
        <p className="text-ink-soft">{TRIAL_LOCK_MESSAGE}</p>
        <Link
          href="/subscribe"
          className="mt-4 inline-flex items-center justify-center rounded-xl bg-brand px-4 py-2 text-sm font-semibold text-white"
        >
          Subscribe now
        </Link>
        <Link href={`/invoices/view?id=${invoice.id}`} className="mt-3 block font-semibold text-brand-deep">
          Back to {invoice.number}
        </Link>
      </div>
    );
  }

  function handleSave(values: InvoiceFormValues) {
    updateInvoice(id, values);
    router.push(`/invoices/view?id=${id}`);
  }

  return (
    <div>
      <h1 className="mb-5 text-xl font-bold">Edit Invoice {invoice.number}</h1>
      <InvoiceForm initial={invoice} onSave={handleSave} submitLabel="Save changes" />
    </div>
  );
}

export default function EditInvoicePage() {
  return (
    <Suspense fallback={null}>
      <EditInvoiceInner />
    </Suspense>
  );
}
