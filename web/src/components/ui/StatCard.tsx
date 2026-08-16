export function StatCard({
  value,
  label,
  tone = "default",
}: {
  value: string;
  label: string;
  tone?: "default" | "warn";
}) {
  return (
    <div className="rounded-2xl bg-surface border border-line p-4">
      <div
        className={`text-xl font-bold tabular ${tone === "warn" ? "text-error" : "text-brand"}`}
      >
        {value}
      </div>
      <div className="mt-1 text-sm text-ink-faint">{label}</div>
    </div>
  );
}
