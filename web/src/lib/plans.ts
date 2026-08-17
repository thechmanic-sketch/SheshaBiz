// Pricing tiers and EFT/WhatsApp constants shared between the customer-facing
// subscribe screen (SubscribePanel) and the admin activation dashboard.
// `id`/`periodDays` map 1:1 onto the `tier`/`period_days` arguments expected
// by the `admin_activate_business` RPC (see supabase/migrations/0003_admin_dashboard.sql).

export type PlanTierId = "monthly" | "quarterly" | "biannual" | "annual";

export interface PlanTier {
  id: PlanTierId;
  label: string;
  priceLabel: string;
  /** Short blurb shown under the price on the plan picker card. */
  detail: string;
  periodDays: number;
}

export const PLAN_TIERS: PlanTier[] = [
  { id: "monthly", label: "Monthly", priceLabel: "R30", detail: "Billed monthly", periodDays: 30 },
  { id: "quarterly", label: "3 Months", priceLabel: "R70", detail: "R70 total", periodDays: 90 },
  { id: "biannual", label: "6 Months", priceLabel: "R120", detail: "R120 total", periodDays: 180 },
  { id: "annual", label: "1 Year", priceLabel: "R200", detail: "R200 total", periodDays: 365 },
];

export function planById(id: PlanTierId): PlanTier {
  const plan = PLAN_TIERS.find((p) => p.id === id);
  if (!plan) throw new Error(`Unknown plan tier: ${id}`);
  return plan;
}

export const EFT_BANKING_DETAILS = {
  bank: "ABSA Bank",
  accountNumber: "9401 3815 23",
  branchCode: "632005",
};

export const WHATSAPP_NUMBER = "27633531662";

export function buildWhatsAppLink(message: string): string {
  return `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(message)}`;
}
