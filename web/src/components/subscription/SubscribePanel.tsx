"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, Check, Copy, MessageCircle } from "lucide-react";
import { useAppData } from "@/lib/store";
import { useAuth } from "@/lib/auth";
import { supabase } from "@/lib/supabase";
import { EFT_BANKING_DETAILS, PLAN_TIERS, buildWhatsAppLink, type PlanTierId } from "@/lib/plans";

function CopyableField({ label, value }: { label: string; value: string }) {
  const [copied, setCopied] = useState(false);

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // Clipboard API unavailable (e.g. insecure context) — the value is
      // still shown as plain text, so the user can select/copy manually.
    }
  }

  return (
    <div className="flex items-center justify-between gap-3 border-b border-line py-2.5 last:border-0">
      <div>
        <p className="text-xs font-semibold text-ink-faint">{label}</p>
        <p className="mt-0.5 font-mono text-sm font-semibold tabular">{value}</p>
      </div>
      <button
        type="button"
        onClick={handleCopy}
        aria-label={`Copy ${label}`}
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
      >
        {copied ? <Check size={15} className="text-brand-deep" /> : <Copy size={15} />}
      </button>
    </div>
  );
}

export function SubscribePanel({ initialPlan = null }: { initialPlan?: PlanTierId | null }) {
  const { data } = useAppData();
  const { email } = useAuth();
  const [selected, setSelected] = useState<PlanTierId | null>(initialPlan);

  // Best-effort signal for the admin dashboard — records which tier the
  // user is looking at as soon as this component shows the EFT/WhatsApp
  // pay-confirm view for it (manual pick or `initialPlan` pre-fill alike).
  // Never blocks or interrupts the actual payment flow: fire-and-forget,
  // errors are logged and swallowed.
  useEffect(() => {
    if (!selected) return;
    supabase.rpc("set_pending_plan", { tier: selected }).then(({ error }) => {
      if (error) {
        console.error("set_pending_plan failed:", error.message);
      }
    });
  }, [selected]);

  const businessName = data.business.name || "your business";
  const plan = PLAN_TIERS.find((p) => p.id === selected) ?? null;

  if (plan) {
    const message = `Hi, I've paid for the ${plan.label} (${plan.priceLabel}) plan for ${businessName}. My account email is ${email ?? ""}.`;
    return (
      <div className="mx-auto max-w-md">
        <button
          type="button"
          onClick={() => setSelected(null)}
          className="flex items-center gap-1.5 text-sm font-semibold text-ink-soft hover:text-ink"
        >
          <ArrowLeft size={15} />
          Choose a different plan
        </button>

        <div className="mt-4 rounded-2xl border border-line bg-surface p-5">
          <h2 className="text-xs font-bold uppercase tracking-wide text-ink-faint">Selected plan</h2>
          <p className="mt-1 text-lg font-bold">
            {plan.label} <span className="text-ink-soft font-semibold">— {plan.priceLabel}</span>
          </p>
        </div>

        <div className="mt-4 rounded-2xl border border-line bg-surface p-5">
          <h2 className="text-xs font-bold uppercase tracking-wide text-ink-faint">Pay by EFT</h2>
          <p className="mt-2 text-sm text-ink-soft">
            Make a bank transfer using these details. Use the reference exactly as shown so we can
            match your payment.
          </p>
          <div className="mt-3">
            <CopyableField label="Bank" value={EFT_BANKING_DETAILS.bank} />
            <CopyableField label="Account number" value={EFT_BANKING_DETAILS.accountNumber} />
            <CopyableField label="Branch code" value={EFT_BANKING_DETAILS.branchCode} />
            <CopyableField label="Reference" value={businessName} />
          </div>
        </div>

        <div className="mt-4 rounded-2xl border border-line bg-surface p-5">
          <h2 className="text-xs font-bold uppercase tracking-wide text-ink-faint">Notify us</h2>
          <p className="mt-2 text-sm text-ink-soft">
            Once you&apos;ve made the transfer, let us know on WhatsApp so we can confirm it and
            activate your account.
          </p>
          <a
            href={buildWhatsAppLink(message)}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl bg-brand px-4 py-3 text-sm font-semibold text-white"
          >
            <MessageCircle size={17} />
            I&apos;ve paid — Notify us on WhatsApp
          </a>
          <p className="mt-3 text-xs text-ink-faint">
            We&apos;ll activate your account once we&apos;ve confirmed the payment — this can take a
            little while, we&apos;re not automatic yet.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md">
      <h1 className="text-xl font-bold">Choose a plan</h1>
      <p className="mt-1 text-sm text-ink-soft">
        Pick a plan below, then pay by EFT and let us know on WhatsApp.
      </p>
      <div className="mt-5 flex flex-col gap-3">
        {PLAN_TIERS.map((tier) => (
          <button
            key={tier.id}
            type="button"
            onClick={() => setSelected(tier.id)}
            className="flex items-center justify-between rounded-2xl border border-line bg-surface p-4 text-left hover:border-brand"
          >
            <div>
              <p className="font-bold">{tier.label}</p>
              <p className="text-sm text-ink-soft">{tier.detail}</p>
            </div>
            <p className="text-lg font-bold text-brand-deep">{tier.priceLabel}</p>
          </button>
        ))}
      </div>
    </div>
  );
}

export function SubscribeLoggedOut() {
  return (
    <div className="mx-auto max-w-sm py-10 text-center">
      <h1 className="text-xl font-bold">Log in to subscribe</h1>
      <p className="mt-2 text-sm text-ink-soft">
        You&apos;ll need a free account before picking a plan — it only takes a moment and starts
        with a 7-day free trial.
      </p>
      <Link
        href="/auth"
        className="mt-5 inline-flex items-center justify-center rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white"
      >
        Create account / Log in
      </Link>
    </div>
  );
}
