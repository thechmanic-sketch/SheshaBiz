"use client";

import { useRouter } from "next/navigation";
import { QuoteForm, type QuoteFormValues } from "@/components/quotes/QuoteForm";
import { useAppData } from "@/lib/store";

export default function NewQuotePage() {
  const router = useRouter();
  const { addQuote } = useAppData();

  function handleSave(values: QuoteFormValues) {
    const quote = addQuote(values);
    router.push(`/quotes/view?id=${quote.id}`);
  }

  return (
    <div>
      <h1 className="mb-5 text-xl font-bold">New Quote</h1>
      <QuoteForm onSave={handleSave} submitLabel="Create quote" />
    </div>
  );
}
