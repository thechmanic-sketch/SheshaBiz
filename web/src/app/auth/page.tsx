"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Mail, KeyRound, ArrowLeft } from "lucide-react";
import { useAuth } from "@/lib/auth";

const inputCls =
  "w-full rounded-xl border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-brand";
const labelCls = "text-xs font-semibold text-ink-faint";
const primaryButtonCls =
  "flex w-full items-center justify-center gap-1.5 rounded-xl bg-brand px-3.5 py-2.5 text-sm font-semibold text-white disabled:opacity-50";
const secondaryButtonCls =
  "flex items-center gap-1.5 text-sm font-semibold text-ink-soft hover:text-ink disabled:opacity-50";

type Step = "email" | "code";

export default function AuthPage() {
  const router = useRouter();
  const { sendCode, verifyCode } = useAuth();
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  async function handleSendCode(e?: React.FormEvent) {
    e?.preventDefault();
    if (!email.trim()) return;
    setBusy(true);
    setError(null);
    setNotice(null);
    const result = await sendCode(email.trim());
    setBusy(false);
    if (result.ok) {
      setStep("code");
      setNotice(`We sent a 6-digit code to ${email.trim()}.`);
    } else {
      setError(result.error ?? "Couldn't send the code. Please try again.");
    }
  }

  async function handleVerifyCode(e?: React.FormEvent) {
    e?.preventDefault();
    if (!code.trim()) return;
    setBusy(true);
    setError(null);
    const result = await verifyCode(email.trim(), code.trim());
    setBusy(false);
    if (result.ok) {
      router.push("/settings");
    } else {
      setError(result.error ?? "That code is incorrect or expired.");
    }
  }

  return (
    <div className="mx-auto max-w-sm py-10">
      <div className="rounded-2xl border border-line bg-surface p-6">
        <h1 className="text-lg font-bold">
          {step === "email" ? "Log in or create an account" : "Enter your code"}
        </h1>
        <p className="mt-1 text-sm text-ink-soft">
          {step === "email"
            ? "Create an account to secure future access to SheshaBiz. Your data stays on this device."
            : `Enter the 6-digit code we sent to ${email}.`}
        </p>

        {step === "email" ? (
          <form className="mt-5 flex flex-col gap-3" onSubmit={handleSendCode}>
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
              {busy ? "Sending…" : "Send code"}
            </button>
          </form>
        ) : (
          <form className="mt-5 flex flex-col gap-3" onSubmit={handleVerifyCode}>
            <div>
              <label className={labelCls}>6-digit code</label>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={6}
                autoComplete="one-time-code"
                required
                className={`${inputCls} mt-1 tabular tracking-widest`}
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/[^0-9]/g, ""))}
                placeholder="123456"
              />
            </div>
            {notice && !error && <p className="text-sm font-medium text-brand-deep">{notice}</p>}
            {error && <p className="text-sm font-medium text-error">{error}</p>}
            <button type="submit" disabled={busy || code.trim().length < 6} className={primaryButtonCls}>
              <KeyRound size={15} />
              {busy ? "Verifying…" : "Verify"}
            </button>
            <div className="mt-1 flex items-center justify-between">
              <button
                type="button"
                disabled={busy}
                onClick={() => {
                  setStep("email");
                  setCode("");
                  setError(null);
                  setNotice(null);
                }}
                className={secondaryButtonCls}
              >
                <ArrowLeft size={14} />
                Use a different email
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={handleSendCode}
                className={secondaryButtonCls}
              >
                Resend code
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
