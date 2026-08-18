"use client";

import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import Link from "next/link";
import { useAuth } from "./auth";

export const TRIAL_LOCK_MESSAGE =
  "Your trial has ended. WhatsApp or call 063 353 1662 to activate SheshaBiz.";

export const SIGN_UP_LOCK_MESSAGE =
  "Sign up for your free 7-day trial to create quotes, invoices, and more.";

/** Why create/edit actions are currently blocked, or null when allowed.
 * "needs-login": this device has never logged in — no trial has ever been
 * started, so there's nothing to subscribe to yet. Send them to /auth.
 * "needs-subscription": they logged in at some point and their trial/
 * subscription has since lapsed. Send them to /subscribe. */
export type LockReason = "needs-login" | "needs-subscription" | null;

interface TrialGateContextValue {
  /** See `LockReason`. View/print/share on existing records — and the
   * Settings data export/backup feature — should call the plain handler
   * instead of going through this gate; only creating or editing is ever
   * blocked. */
  lockReason: LockReason;
  /** Runs `action` unless gated, in which case it shows the matching
   * "sign up" / "trial has ended" notice instead. Use this to wrap every
   * create/edit entry point (new quote, save edits, complete sale, ...) —
   * view/print/share on existing records should call the plain handler
   * instead of going through this gate. */
  guard: (action: () => void) => void;
}

const TrialGateContext = createContext<TrialGateContextValue | null>(null);

export function TrialGateProvider({ children }: { children: ReactNode }) {
  const { isLoggedIn, subscriptionState, loaded } = useAuth();
  // While the initial session check hasn't resolved yet, don't gate —
  // mirrors the pre-existing behavior where subscriptionState started out
  // null (unlocked) until the first status fetch completed. This avoids a
  // flash of "sign up" for an already-logged-in user on first paint.
  const lockReason: LockReason = !loaded
    ? null
    : !isLoggedIn
      ? "needs-login"
      : subscriptionState === "lapsed"
        ? "needs-subscription"
        : null;
  const [noticeOpen, setNoticeOpen] = useState(false);

  const guard = useCallback(
    (action: () => void) => {
      if (lockReason) {
        setNoticeOpen(true);
        return;
      }
      action();
    },
    [lockReason]
  );

  const value = useMemo<TrialGateContextValue>(() => ({ lockReason, guard }), [lockReason, guard]);

  const noticeCopy =
    lockReason === "needs-login"
      ? {
          title: "Sign up to continue",
          message: SIGN_UP_LOCK_MESSAGE,
          href: "/auth",
          cta: "Sign up",
        }
      : {
          title: "Trial ended",
          message: TRIAL_LOCK_MESSAGE,
          href: "/subscribe",
          cta: "Subscribe now",
        };

  return (
    <TrialGateContext.Provider value={value}>
      {children}
      {noticeOpen && lockReason && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 no-print">
          <div className="w-full max-w-sm rounded-2xl border border-line bg-surface p-5 shadow-xl">
            <h2 className="text-base font-bold">{noticeCopy.title}</h2>
            <p className="mt-2 text-sm text-ink-soft">{noticeCopy.message}</p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setNoticeOpen(false)}
                className="rounded-xl px-4 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
              >
                Got it
              </button>
              <Link
                href={noticeCopy.href}
                onClick={() => setNoticeOpen(false)}
                className="rounded-xl bg-brand px-4 py-2 text-sm font-semibold text-white"
              >
                {noticeCopy.cta}
              </Link>
            </div>
          </div>
        </div>
      )}
    </TrialGateContext.Provider>
  );
}

export function useTrialGate(): TrialGateContextValue {
  const ctx = useContext(TrialGateContext);
  if (!ctx) throw new Error("useTrialGate must be used within TrialGateProvider");
  return ctx;
}
