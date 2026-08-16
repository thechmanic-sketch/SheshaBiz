"use client";

export function FilterChips<T extends string>({
  options,
  value,
  onChange,
}: {
  options: { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
}) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          onClick={() => onChange(opt.value)}
          className={`rounded-full px-3.5 py-1.5 text-xs font-semibold whitespace-nowrap ${
            value === opt.value ? "bg-brand text-white" : "border border-line text-ink-soft"
          }`}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}
