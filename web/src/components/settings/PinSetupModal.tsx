"use client";

import { useState } from "react";
import { PinDots, PinKeypad, PIN_LENGTH } from "@/components/ui/PinKeypad";
import { hashPin } from "@/lib/pin";

export function PinSetupModal({
  open,
  onClose,
  onSet,
}: {
  open: boolean;
  onClose: () => void;
  onSet: (hash: string) => void;
}) {
  const [step, setStep] = useState<"enter" | "confirm">("enter");
  const [firstPin, setFirstPin] = useState("");
  const [pin, setPin] = useState("");
  const [error, setError] = useState<string | null>(null);

  function reset() {
    setStep("enter");
    setFirstPin("");
    setPin("");
    setError(null);
  }

  function close() {
    reset();
    onClose();
  }

  async function handleDigit(digit: string) {
    if (pin.length >= PIN_LENGTH) return;
    const next = pin + digit;
    setPin(next);
    if (next.length < PIN_LENGTH) return;

    if (step === "enter") {
      setFirstPin(next);
      setPin("");
      setStep("confirm");
      return;
    }

    if (next === firstPin) {
      const hash = await hashPin(next);
      onSet(hash);
      reset();
    } else {
      setError("PINs didn't match. Try again.");
      setPin("");
      setFirstPin("");
      setStep("enter");
    }
  }

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-xs rounded-2xl bg-surface border border-line p-5 shadow-xl">
        <h2 className="text-center text-base font-bold">
          {step === "enter" ? "Set a delete PIN" : "Confirm your PIN"}
        </h2>
        <p className="mt-1 text-center text-sm text-ink-faint">
          {step === "enter"
            ? "You'll need this before deleting a quote, invoice, product, or customer."
            : "Enter it once more to confirm."}
        </p>
        <div className="mt-5">
          <PinDots filled={pin.length} />
        </div>
        {error && <p className="mt-2 text-center text-xs font-semibold text-error">{error}</p>}
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
