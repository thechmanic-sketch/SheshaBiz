"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { QuoteForm, type QuoteFormValues } from "@/components/quotes/QuoteForm";
import { useAppData } from "@/lib/store";
import { TRIAL_LOCK_MESSAGE, useTrialGate } from "@/lib/trial";

export default function NewQuotePage() {
  const router = useRouter();
  const { addQuote } = useAppData();
  const { locked } = useTrialGate();

  function handleSave(values: QuoteFormValues) {
    const quote = addQuote(values);
    router.push(`/quotes/view?id=${quote.id}`);
  }

  if (locked) {
    return (
      <div className="mx-auto max-w-md py-16 text-center">
        <p className="text-ink-soft">{TRIAL_LOCK_MESSAGE}</p>
        <Link href="/quotes" className="mt-3 inline-block font-semibold text-brand-deep">
          Back to Quotes
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h1 className="mb-5 text-xl font-bold">New Quote</h1>
      <QuoteForm onSave={handleSave} submitLabel="Create quote" />
    </div>
  );
}
