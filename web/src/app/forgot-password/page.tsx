"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowLeft, Mail } from "lucide-react";
import { useAuth } from "@/lib/auth";

const inputCls =
  "w-full rounded-xl border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-brand";
const labelCls = "text-xs font-semibold text-ink-faint";
const primaryButtonCls =
  "flex w-full items-center justify-center gap-1.5 rounded-xl bg-brand px-3.5 py-2.5 text-sm font-semibold text-white disabled:opacity-50";
const secondaryButtonCls =
  "flex items-center gap-1.5 text-sm font-semibold text-ink-soft hover:text-ink disabled:opacity-50";

export default function ForgotPasswordPage() {
  const { requestPasswordReset } = useAuth();
  const [email, setEmail] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  async function handleSubmit(e?: React.FormEvent) {
    e?.preventDefault();
    if (!email.trim()) return;
    setBusy(true);
    setError(null);
    const result = await requestPasswordReset(email.trim());
    setBusy(false);
    if (result.ok) {
      // Generic on purpose — never reveal whether an account exists for
      // this email, matching Supabase's own anti-enumeration behavior.
      setSent(true);
    } else {
      setError(result.error ?? "Couldn't send the reset link. Please try again.");
    }
  }

  return (
    <div className="mx-auto max-w-sm py-10">
      <div className="rounded-2xl border border-line bg-surface p-6">
        <Link href="/auth" className={`${secondaryButtonCls} mb-3`}>
          <ArrowLeft size={14} />
          Back to log in
        </Link>

        <h1 className="text-lg font-bold">Reset your password</h1>
        <p className="mt-1 text-sm text-ink-soft">
          Enter the email address on your account and we&apos;ll send you a link to reset your password.
        </p>

        {sent ? (
          <p className="mt-5 rounded-xl border border-line bg-paper p-3 text-sm font-medium text-brand-deep">
            If an account exists for that email, we&apos;ve sent a reset link. Check your inbox (and spam
            folder).
          </p>
        ) : (
          <form className="mt-5 flex flex-col gap-3" onSubmit={handleSubmit}>
            <div>
              <label className={labelCls}>Email address</label>
              <input
                type="email"
                autoComplete="email"
                required
                className={`${inputCls} mt-1`}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@business.co.za"
              />
            </div>
            {error && <p className="text-sm font-medium text-error">{error}</p>}
            <button type="submit" disabled={busy || !email.trim()} className={primaryButtonCls}>
              <Mail size={15} />
              {busy ? "Sending…" : "Send reset link"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
