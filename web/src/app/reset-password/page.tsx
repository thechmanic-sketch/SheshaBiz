"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Lock } from "lucide-react";
import { supabase } from "@/lib/supabase";
import { useAuth } from "@/lib/auth";

const inputCls =
  "w-full rounded-xl border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-brand";
const labelCls = "text-xs font-semibold text-ink-faint";
const primaryButtonCls =
  "flex w-full items-center justify-center gap-1.5 rounded-xl bg-brand px-3.5 py-2.5 text-sm font-semibold text-white disabled:opacity-50";

type CheckState = "checking" | "ready" | "no-session";

export default function ResetPasswordPage() {
  const router = useRouter();
  const { setPassword } = useAuth();
  const [checkState, setCheckState] = useState<CheckState>("checking");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function checkSession() {
      try {
        // The Supabase client auto-detects the recovery tokens Supabase put
        // in the URL hash when it redirected here (detectSessionInUrl is on
        // by default) and exchanges them for a session before this resolves.
        const { data } = await supabase.auth.getSession();
        if (cancelled) return;
        setCheckState(data.session ? "ready" : "no-session");
      } catch {
        if (!cancelled) setCheckState("no-session");
      }
    }

    checkSession();

    return () => {
      cancelled = true;
    };
  }, []);

  async function handleSubmit(e?: React.FormEvent) {
    e?.preventDefault();
    if (newPassword.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Passwords don't match.");
      return;
    }
    setBusy(true);
    setError(null);
    const result = await setPassword(newPassword);
    setBusy(false);
    if (result.ok) {
      router.push("/settings");
    } else {
      setError(result.error ?? "Couldn't set the password. Please try again.");
    }
  }

  return (
    <div className="mx-auto max-w-sm py-10">
      <div className="rounded-2xl border border-line bg-surface p-6">
        <h1 className="text-lg font-bold">Set a new password</h1>

        {checkState === "checking" && (
          <p className="mt-4 text-sm text-ink-soft">Checking your reset link…</p>
        )}

        {checkState === "no-session" && (
          <>
            <p className="mt-1 text-sm text-ink-soft">
              This reset link is invalid or has expired.
            </p>
            <Link
              href="/forgot-password"
              className={`${primaryButtonCls} mt-5`}
            >
              Request a new reset link
            </Link>
          </>
        )}

        {checkState === "ready" && (
          <>
            <p className="mt-1 text-sm text-ink-soft">Choose a new password for your account.</p>
            <form className="mt-5 flex flex-col gap-3" onSubmit={handleSubmit}>
              <div>
                <label className={labelCls}>New password</label>
                <input
                  type="password"
                  autoComplete="new-password"
                  required
                  minLength={8}
                  className={`${inputCls} mt-1`}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="••••••••"
                />
              </div>
              <div>
                <label className={labelCls}>Confirm new password</label>
                <input
                  type="password"
                  autoComplete="new-password"
                  required
                  minLength={8}
                  className={`${inputCls} mt-1`}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                />
              </div>
              {error && <p className="text-sm font-medium text-error">{error}</p>}
              <button
                type="submit"
                disabled={busy || !newPassword || !confirmPassword}
                className={primaryButtonCls}
              >
                <Lock size={15} />
                {busy ? "Saving…" : "Set new password"}
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
