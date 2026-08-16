import type { InvoiceStatus, QuoteStatus } from "@/lib/mock-data";

const quoteStyles: Record<QuoteStatus, string> = {
  draft: "bg-black/[.06] text-ink-soft dark:bg-white/[.08]",
  sent: "bg-blue-500/10 text-blue-600 dark:text-blue-400",
  accepted: "bg-brand-tint text-brand-deep",
  rejected: "bg-error/10 text-error",
};

const quoteLabels: Record<QuoteStatus, string> = {
  draft: "Draft",
  sent: "Sent",
  accepted: "Accepted",
  rejected: "Rejected",
};

const invoiceStyles: Record<InvoiceStatus, string> = {
  paid: "bg-brand-tint text-brand-deep",
  unpaid: "bg-blue-500/10 text-blue-600 dark:text-blue-400",
  overdue: "bg-error/10 text-error",
  cancelled: "bg-black/[.06] text-ink-soft dark:bg-white/[.08]",
};

const invoiceLabels: Record<InvoiceStatus, string> = {
  paid: "Paid",
  unpaid: "Unpaid",
  overdue: "Overdue",
  cancelled: "Cancelled",
};

export function QuoteBadge({ status }: { status: QuoteStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${quoteStyles[status]}`}
    >
      {quoteLabels[status]}
    </span>
  );
}

export function InvoiceBadge({ status }: { status: InvoiceStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${invoiceStyles[status]}`}
    >
      {invoiceLabels[status]}
    </span>
  );
}
