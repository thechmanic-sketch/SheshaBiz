"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { InvoiceForm, type InvoiceFormValues } from "@/components/invoices/InvoiceForm";
import { useAppData } from "@/lib/store";
import { TRIAL_LOCK_MESSAGE, useTrialGate } from "@/lib/trial";

export default function NewInvoicePage() {
  const router = useRouter();
  const { addInvoice } = useAppData();
  const { locked } = useTrialGate();

  function handleSave(values: InvoiceFormValues) {
    const invoice = addInvoice(values);
    router.push(`/invoices/view?id=${invoice.id}`);
  }

  if (locked) {
    return (
      <div className="mx-auto max-w-md py-16 text-center">
        <p className="text-ink-soft">{TRIAL_LOCK_MESSAGE}</p>
        <Link href="/invoices" className="mt-3 inline-block font-semibold text-brand-deep">
          Back to Invoices
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h1 className="mb-5 text-xl font-bold">New Invoice</h1>
      <InvoiceForm onSave={handleSave} submitLabel="Create invoice" />
    </div>
  );
}
