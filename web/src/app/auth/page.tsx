"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { KeyRound, ArrowLeft, Sparkles, Lock } from "lucide-react";
import { useAuth } from "@/lib/auth";
import { PLAN_TIERS, type PlanTierId } from "@/lib/plans";

const inputCls =
  "w-full rounded-xl border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-brand";
const labelCls = "text-xs font-semibold text-ink-faint";
const primaryButtonCls =
  "flex w-full items-center justify-center gap-1.5 rounded-xl bg-brand px-3.5 py-2.5 text-sm font-semibold text-white disabled:opacity-50";
const secondaryButtonCls =
  "flex items-center gap-1.5 text-sm font-semibold text-ink-soft hover:text-ink disabled:opacity-50";

type Step = "intent" | "email" | "code" | "set-password";

/** What the signup was for — either the free trial, or one of the paid
 * plan tiers picked up-front. This is purely an intent signal: everyone
 * still gets the same real 7-day trial via bootstrap_business() on first
 * login. It only decides which screen they land on right after verifying,
 * so someone who wants to pay immediately sees exactly how to. */
type SignupIntent = "trial" | PlanTierId;

export default function AuthPage() {
  const router = useRouter();
  const { sendCode, verifyCode, signUpWithPassword, signInWithPassword, setPassword } = useAuth();
  const [step, setStep] = useState<Step>("intent");
  const [intent, setIntent] = useState<SignupIntent>("trial");
  const [email, setEmail] = useState("");
  const [password, setPasswordField] = useState("");
  const [fullName, setFullName] = useState("");
  const [phone, setPhone] = useState("");
  const [businessName, setBusinessName] = useState("");
  const [city, setCity] = useState("");
  const [code, setCode] = useState("");
  const [isReturningUser, setIsReturningUser] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  function handleChooseIntent(choice: SignupIntent) {
    setIntent(choice);
    setStep("email");
  }

  /** Where a fully-authenticated user lands, based on the plan intent they
   * picked on the very first screen. Shared by the create-account, login,
   * and set-password success paths. */
  function routeAfterAuth() {
    if (intent !== "trial") {
      router.push(`/subscribe?plan=${intent}`);
    } else {
      router.push("/settings");
    }
  }

  function toggleReturningUser() {
    setIsReturningUser((prev) => !prev);
    setError(null);
    setNotice(null);
  }

  async function handleCreateAccount(e?: React.FormEvent) {
    e?.preventDefault();
    if (!email.trim() || !password || !fullName.trim() || !phone.trim() || !businessName.trim()) return;
    setBusy(true);
    setError(null);
    const result = await signUpWithPassword(email.trim(), password, {
      full_name: fullName.trim(),
      phone: phone.trim(),
      business_name: businessName.trim(),
      city: city.trim() || undefined,
    });
    setBusy(false);
    if (result.ok) {
      routeAfterAuth();
    } else if (result.accountExists) {
      setIsReturningUser(true);
      setError(result.error ?? "An account already exists for this email — log in instead.");
    } else {
      setError(result.error ?? "Couldn't create the account. Please try again.");
    }
  }

  async function handleLogin(e?: React.FormEvent) {
    e?.preventDefault();
    if (!email.trim() || !password) return;
    setBusy(true);
    setError(null);
    const result = await signInWithPassword(email.trim(), password);
    setBusy(false);
    if (result.ok) {
      routeAfterAuth();
    } else {
      setError(result.error ?? "Incorrect email or password.");
    }
  }

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
      setPasswordField("");
      setStep("set-password");
    } else {
      setError(result.error ?? "That code is incorrect or expired.");
    }
  }

  async function handleSetPassword(e?: React.FormEvent) {
    e?.preventDefault();
    if (!password) return;
    setBusy(true);
    // Never block on this — proceed to the app regardless of whether
    // setPassword() itself succeeded.
    await setPassword(password);
    setBusy(false);
    routeAfterAuth();
  }

  function handleSkipSetPassword() {
    routeAfterAuth();
  }

  if (step === "intent") {
    return (
      <div className="mx-auto max-w-sm py-10">
        <div className="rounded-2xl border border-line bg-surface p-6">
          <h1 className="text-lg font-bold">How would you like to start?</h1>
          <p className="mt-1 text-sm text-ink-soft">
            Pick an option, then create your account with your email address.
          </p>

          <div className="mt-5 flex flex-col gap-3">
            <button
              type="button"
              onClick={() => handleChooseIntent("trial")}
              className="flex items-center justify-between rounded-2xl border-2 border-brand bg-brand/5 p-4 text-left"
            >
              <div className="flex items-center gap-2">
                <Sparkles size={17} className="shrink-0 text-brand-deep" />
                <div>
                  <p className="font-bold">Free 7-Day Trial</p>
                  <p className="text-sm text-ink-soft">Try everything, no payment needed</p>
                </div>
              </div>
              <p className="text-xs font-bold uppercase tracking-wide text-brand-deep">Recommended</p>
            </button>

            {PLAN_TIERS.map((tier) => (
              <button
                key={tier.id}
                type="button"
                onClick={() => handleChooseIntent(tier.id)}
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
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-sm py-10">
      <div className="rounded-2xl border border-line bg-surface p-6">
        {step !== "set-password" && (
          <button
            type="button"
            onClick={() => {
              setStep("intent");
              setError(null);
              setNotice(null);
            }}
            className={`${secondaryButtonCls} mb-3`}
          >
            <ArrowLeft size={14} />
            Back
          </button>
        )}

        <h1 className="text-lg font-bold">
          {step === "email"
            ? isReturningUser
              ? "Log in"
              : "Create your account"
            : step === "code"
              ? "Enter your code"
              : "Set a password (optional)"}
        </h1>
        <p className="mt-1 text-sm text-ink-soft">
          {step === "email"
            ? isReturningUser
              ? "Enter your email and password."
              : "Set a password so you're always signed in — even on a new device."
            : step === "code"
              ? `Enter the 6-digit code we sent to ${email}.`
              : "Skip the wait next time — set a password now and log in instantly."}
        </p>

        {step === "email" ? (
          <form
            className="mt-5 flex flex-col gap-3"
            onSubmit={isReturningUser ? handleLogin : handleCreateAccount}
          >
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
            {!isReturningUser && (
              <>
                <div>
                  <label className={labelCls}>Full Name</label>
                  <input
                    type="text"
                    autoComplete="name"
                    required
                    className={`${inputCls} mt-1`}
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Jane Dlamini"
                  />
                </div>
                <div>
                  <label className={labelCls}>Phone Number</label>
                  <input
                    type="tel"
                    autoComplete="tel"
                    required
                    className={`${inputCls} mt-1`}
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="082 123 4567"
                  />
                </div>
                <div>
                  <label className={labelCls}>Business Name</label>
                  <input
                    type="text"
                    autoComplete="organization"
                    required
                    className={`${inputCls} mt-1`}
                    value={businessName}
                    onChange={(e) => setBusinessName(e.target.value)}
                    placeholder="Jane's Catering"
                  />
                </div>
                <div>
                  <label className={labelCls}>City/Town</label>
                  <input
                    type="text"
                    autoComplete="address-level2"
                    className={`${inputCls} mt-1`}
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="Johannesburg"
                  />
                </div>
              </>
            )}
            <div>
              <label className={labelCls}>Password</label>
              <input
                type="password"
                autoComplete={isReturningUser ? "current-password" : "new-password"}
                required
                className={`${inputCls} mt-1`}
                value={password}
                onChange={(e) => setPasswordField(e.target.value)}
                placeholder="••••••••"
              />
            </div>
            {error && <p className="text-sm font-medium text-error">{error}</p>}
            <button
              type="submit"
              disabled={
                busy ||
                !email.trim() ||
                !password ||
                (!isReturningUser && (!fullName.trim() || !phone.trim() || !businessName.trim()))
              }
              className={primaryButtonCls}
            >
              <Lock size={15} />
              {busy ? (isReturningUser ? "Logging in…" : "Creating account…") : isReturningUser ? "Log in" : "Create account"}
            </button>
            <button type="button" onClick={toggleReturningUser} className={`${secondaryButtonCls} justify-center`}>
              {isReturningUser ? "New here? Create an account" : "Already have an account? Log in"}
            </button>
            {isReturningUser && (
              <div className="flex flex-col items-center gap-1.5">
                <Link
                  href="/forgot-password"
                  className="text-center text-xs font-semibold text-brand-deep hover:underline"
                >
                  Forgot password?
                </Link>
                <button
                  type="button"
                  disabled={busy}
                  onClick={handleSendCode}
                  className="text-center text-xs font-semibold text-ink-faint hover:text-ink-soft disabled:opacity-50"
                >
                  Forgot your password? Log in with a code instead
                </button>
              </div>
            )}
          </form>
        ) : step === "code" ? (
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
        ) : (
          <form className="mt-5 flex flex-col gap-3" onSubmit={handleSetPassword}>
            <div>
              <label className={labelCls}>Password</label>
              <input
                type="password"
                autoComplete="new-password"
                required
                className={`${inputCls} mt-1`}
                value={password}
                onChange={(e) => setPasswordField(e.target.value)}
                placeholder="••••••••"
              />
            </div>
            <button type="submit" disabled={busy || !password} className={primaryButtonCls}>
              <Lock size={15} />
              {busy ? "Saving…" : "Save password"}
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={handleSkipSetPassword}
              className={`${secondaryButtonCls} justify-center disabled:opacity-50`}
            >
              Skip for now
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
