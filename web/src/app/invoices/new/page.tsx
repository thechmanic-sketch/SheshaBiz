"use client";

import { useRouter } from "next/navigation";
import { InvoiceForm, type InvoiceFormValues } from "@/components/invoices/InvoiceForm";
import { useAppData } from "@/lib/store";

export default function NewInvoicePage() {
  const router = useRouter();
  const { addInvoice } = useAppData();

  function handleSave(values: InvoiceFormValues) {
    const invoice = addInvoice(values);
    router.push(`/invoices/view?id=${invoice.id}`);
  }

  return (
    <div>
      <h1 className="mb-5 text-xl font-bold">New Invoice</h1>
      <InvoiceForm onSave={handleSave} submitLabel="Create invoice" />
    </div>
  );
}
