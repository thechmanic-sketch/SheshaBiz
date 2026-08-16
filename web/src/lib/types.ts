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
}

export interface Customer {
  id: string;
  name: string;
  phone: string;
  email: string;
  address: string;
}

export interface Product {
  id: string;
  name: string;
  price: number;
  stockQty: number;
  imageDataUrl: string | null;
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
}

export interface Sale {
  id: string;
  number: string;
  lineItems: LineItem[];
  paymentMethod: PaymentMethod;
  amountTendered: number | null;
  changeGiven: number | null;
  createdAt: string;
}

export function lineItemsTotal(items: LineItem[]): number {
  return items.reduce((sum, item) => sum + item.qty * item.unitPrice, 0);
}

export function quoteTotals(quote: Pick<Quote, "lineItems">, vatRate: number) {
  const subtotal = lineItemsTotal(quote.lineItems);
  const vat = subtotal * (vatRate / 100);
  return { subtotal, vat, total: subtotal + vat };
}
