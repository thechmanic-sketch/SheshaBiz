"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { QuoteForm, type QuoteFormValues } from "@/components/quotes/QuoteForm";
import { useAppData } from "@/lib/store";

function EditQuoteInner() {
  const router = useRouter();
  const params = useSearchParams();
  const id = params.get("id") ?? "";
  const { data, updateQuote } = useAppData();
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

  function handleSave(values: QuoteFormValues) {
    updateQuote(id, values);
    router.push(`/quotes/view?id=${id}`);
  }

  return (
    <div>
      <h1 className="mb-5 text-xl font-bold">Edit Quote {quote.number}</h1>
      <QuoteForm initial={quote} onSave={handleSave} submitLabel="Save changes" />
    </div>
  );
}

export default function EditQuotePage() {
  return (
    <Suspense fallback={null}>
      <EditQuoteInner />
    </Suspense>
  );
}
