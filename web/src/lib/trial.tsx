"use client";

import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { useAuth } from "./auth";

export const TRIAL_LOCK_MESSAGE =
  "Your trial has ended. WhatsApp or call 063 353 1662 to activate SheshaBiz.";

interface TrialGateContextValue {
  /** True once the trial/subscription has lapsed. A logged-out user
   * (subscriptionState === null) is never locked — this app stays fully
   * offline and ungated for them, same as before this feature existed. */
  locked: boolean;
  /** Runs `action` unless locked, in which case it shows the shared
   * "trial has ended" notice instead. Use this to wrap every
   * create/edit entry point (new quote, save edits, complete sale, ...) —
   * view/print/share on existing records should call the plain handler
   * instead of going through this gate. */
  guard: (action: () => void) => void;
}

const TrialGateContext = createContext<TrialGateContextValue | null>(null);

export function TrialGateProvider({ children }: { children: ReactNode }) {
  const { subscriptionState } = useAuth();
  const locked = subscriptionState === "lapsed";
  const [noticeOpen, setNoticeOpen] = useState(false);

  const guard = useCallback(
    (action: () => void) => {
      if (locked) {
        setNoticeOpen(true);
        return;
      }
      action();
    },
    [locked]
  );

  const value = useMemo<TrialGateContextValue>(() => ({ locked, guard }), [locked, guard]);

  return (
    <TrialGateContext.Provider value={value}>
      {children}
      {noticeOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 no-print">
          <div className="w-full max-w-sm rounded-2xl border border-line bg-surface p-5 shadow-xl">
            <h2 className="text-base font-bold">Trial ended</h2>
            <p className="mt-2 text-sm text-ink-soft">{TRIAL_LOCK_MESSAGE}</p>
            <div className="mt-5 flex justify-end">
              <button
                type="button"
                onClick={() => setNoticeOpen(false)}
                className="rounded-xl bg-brand px-4 py-2 text-sm font-semibold text-white"
              >
                Got it
              </button>
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
