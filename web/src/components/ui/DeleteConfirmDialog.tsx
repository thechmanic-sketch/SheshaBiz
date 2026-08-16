"use client";

import { useState } from "react";
import { PinDots, PinKeypad, PIN_LENGTH } from "@/components/ui/PinKeypad";
import { useAppData } from "@/lib/store";
import { verifyPin } from "@/lib/pin";

/**
 * Drop-in replacement for ConfirmDialog on destructive actions. If the
 * business has set a delete PIN (Settings > Security), this requires it
 * before calling onConfirm; otherwise it behaves exactly like a plain
 * yes/no confirm.
 */
export function DeleteConfirmDialog({
  open,
  title,
  message,
  onConfirm,
  onCancel,
}: {
  open: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const { data } = useAppData();
  const [pin, setPin] = useState("");
  const [error, setError] = useState(false);

  if (!open) return null;

  function close() {
    setPin("");
    setError(false);
    onCancel();
  }

  const pinHash = data.business.deletePinHash;

  if (!pinHash) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
        <div className="w-full max-w-sm rounded-2xl bg-surface border border-line p-5 shadow-xl">
          <h2 className="text-base font-bold">{title}</h2>
          <p className="mt-2 text-sm text-ink-soft">{message}</p>
          <div className="mt-5 flex justify-end gap-2">
            <button
              type="button"
              onClick={close}
              className="rounded-xl px-4 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={onConfirm}
              className="rounded-xl bg-error px-4 py-2 text-sm font-semibold text-white"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    );
  }

  async function handleDigit(digit: string) {
    if (pin.length >= PIN_LENGTH) return;
    const next = pin + digit;
    setPin(next);
    setError(false);
    if (next.length === PIN_LENGTH) {
      const ok = await verifyPin(next, pinHash!);
      if (ok) {
        setPin("");
        onConfirm();
      } else {
        setError(true);
        setTimeout(() => setPin(""), 400);
      }
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-xs rounded-2xl bg-surface border border-line p-5 shadow-xl">
        <h2 className="text-center text-base font-bold">{title}</h2>
        <p className="mt-1 text-center text-sm text-ink-faint">Enter your PIN to confirm.</p>
        <div className="mt-5">
          <PinDots filled={pin.length} />
        </div>
        {error && <p className="mt-2 text-center text-xs font-semibold text-error">Incorrect PIN</p>}
        <PinKeypad onDigit={handleDigit} onBackspace={() => setPin((p) => p.slice(0, -1))} />
        <button
          type="button"
          onClick={close}
          className="mt-4 w-full rounded-xl py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
