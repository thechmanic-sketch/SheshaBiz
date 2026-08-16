"use client";

import { Delete } from "lucide-react";

const PIN_LENGTH = 4;

export function PinDots({ filled }: { filled: number }) {
  return (
    <div className="flex justify-center gap-3">
      {Array.from({ length: PIN_LENGTH }).map((_, i) => (
        <span
          key={i}
          className={`h-3 w-3 rounded-full border-2 ${
            i < filled ? "border-brand bg-brand" : "border-line bg-transparent"
          }`}
        />
      ))}
    </div>
  );
}

export function PinKeypad({
  onDigit,
  onBackspace,
}: {
  onDigit: (digit: string) => void;
  onBackspace: () => void;
}) {
  return (
    <div className="mt-6 grid grid-cols-3 gap-3">
      {["1", "2", "3", "4", "5", "6", "7", "8", "9"].map((digit) => (
        <button
          key={digit}
          type="button"
          onClick={() => onDigit(digit)}
          className="rounded-2xl border border-line py-3.5 text-lg font-semibold text-ink hover:bg-black/[.04] dark:hover:bg-white/[.06]"
        >
          {digit}
        </button>
      ))}
      <div />
      <button
        type="button"
        onClick={() => onDigit("0")}
        className="rounded-2xl border border-line py-3.5 text-lg font-semibold text-ink hover:bg-black/[.04] dark:hover:bg-white/[.06]"
      >
        0
      </button>
      <button
        type="button"
        onClick={onBackspace}
        aria-label="Backspace"
        className="flex items-center justify-center rounded-2xl border border-line py-3.5 text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
      >
        <Delete size={18} />
      </button>
    </div>
  );
}

export { PIN_LENGTH };
