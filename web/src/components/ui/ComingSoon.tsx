import type { ComponentType } from "react";

export function ComingSoon({
  icon: Icon,
  title,
  description,
}: {
  icon: ComponentType<{ size?: number; className?: string }>;
  title: string;
  description: string;
}) {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-3 py-24 text-center">
      <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-tint text-brand-deep">
        <Icon size={26} />
      </span>
      <h1 className="text-lg font-bold">{title}</h1>
      <p className="text-sm text-ink-faint">{description}</p>
    </div>
  );
}
