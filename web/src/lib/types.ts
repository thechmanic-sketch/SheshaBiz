export type Country = "south_africa" | "zimbabwe" | "kenya" | "ghana";

export interface CountryInfo {
  code: Country;
  displayName: string;
  currencyCode: string;
  currencySymbol: string;
}

export const COUNTRIES: Record<Country, CountryInfo> = {
  south_africa: {
    code: "south_africa",
    displayName: "South Africa",
    currencyCode: "ZAR",
    currencySymbol: "R",
  },
  zimbabwe: {
    code: "zimbabwe",
    displayName: "Zimbabwe",
    currencyCode: "USD",
    currencySymbol: "$",
  },
  kenya: {
    code: "kenya",
    displayName: "Kenya",
    currencyCode: "KES",
    currencySymbol: "KSh",
  },
  ghana: {
    code: "ghana",
    displayName: "Ghana",
    currencyCode: "GHS",
    currencySymbol: "GH₵",
  },
};

export interface BusinessProfile {
  name: string;
  logoDataUrl: string | null;
  regNumber: string;
  bankName: string;
  accountNumber: string;
  branchCode: string;
  vatRate: number;
  paymentTerms: string;
  country: Country;
  quotePrefix: string;
  invoicePrefix: string;
  receiptPrefix: string;
  deletePinHash: string | null;
  updatedAt: string;
}

export interface Customer {
  id: string;
  name: string;
  phone: string;
  email: string;
  address: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface Product {
  id: string;
  name: string;
  price: number;
  stockQty: number;
  imageDataUrl: string | null;
  updatedAt: string;
  deletedAt: string | null;
}

export interface LineItem {
  id: string;
  description: string;
  qty: number;
  unitPrice: number;
}

export type QuoteStatus = "draft" | "sent" | "accepted" | "rejected";
export type InvoiceStatus = "unpaid" | "paid" | "overdue" | "cancelled";
export type PaymentMethod = "cash" | "card" | "eft";

export interface Quote {
  id: string;
  number: string;
  customerId: string | null;
  customerName: string;
  description: string;
  lineItems: LineItem[];
  status: QuoteStatus;
  createdAt: string;
  convertedToInvoiceId: string | null;
  updatedAt: string;
  deletedAt: string | null;
}

export interface Invoice {
  id: string;
  number: string;
  customerId: string | null;
  customerName: string;
  lineItems: LineItem[];
  status: InvoiceStatus;
  createdAt: string;
  dueDate: string;
  fromQuoteId: string | null;
  updatedAt: string;
  deletedAt: string | null;
}

export interface Sale {
  id: string;
  number: string;
  lineItems: LineItem[];
  paymentMethod: PaymentMethod;
  amountTendered: number | null;
  changeGiven: number | null;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

/**
 * The invoice's displayed status. "overdue" is derived, not stored: an
 * invoice that's still "unpaid" past its due date reads as overdue
 * everywhere in the UI without needing anything to actively flip its
 * stored status (which stays "unpaid" so the mark-paid/unpaid toggle
 * keeps working normally).
 *
 * `today` is required (no `new Date()` default) on purpose: this runs
 * during render, including the prerendered static HTML, and the build
 * machine's clock never matches the viewer's — get it from useToday(),
 * which returns "" until after mount so this safely resolves to the
 * non-overdue stored status until the real date is available.
 */
export function effectiveInvoiceStatus(
  invoice: Pick<Invoice, "status" | "dueDate">,
  today: string
): InvoiceStatus {
  if (invoice.status === "unpaid" && invoice.dueDate < today) return "overdue";
  return invoice.status;
}

export function lineItemsTotal(items: LineItem[]): number {
  return items.reduce((sum, item) => sum + item.qty * item.unitPrice, 0);
}

export function quoteTotals(quote: Pick<Quote, "lineItems">, vatRate: number) {
  const subtotal = lineItemsTotal(quote.lineItems);
  const vat = subtotal * (vatRate / 100);
  return { subtotal, vat, total: subtotal + vat };
}
