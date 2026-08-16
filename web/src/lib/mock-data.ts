// Placeholder data source. Shaped to match what the eventual backend will
// return, so swapping this module out for real API calls later shouldn't
// require touching the screens that consume it.

export type QuoteStatus = "draft" | "sent" | "accepted" | "rejected";
export type InvoiceStatus = "unpaid" | "paid" | "overdue" | "cancelled";

export interface Quote {
  id: string;
  number: string;
  customerName: string;
  description: string;
  total: number;
  status: QuoteStatus;
  createdAt: string;
}

export interface Invoice {
  id: string;
  number: string;
  customerName: string;
  total: number;
  status: InvoiceStatus;
  dueDate: string;
}

export const business = {
  name: "Sipho's Plumbing",
  greeting: "Good morning",
};

export const dashboardStats = {
  totalQuoted: 24600,
  quotesCreated: 18,
  quotesSent: 14,
  quotesAccepted: 9,
};

export const recentQuotes: Quote[] = [
  {
    id: "q-0042",
    number: "Q-0042",
    customerName: "Nomvula Dlamini",
    description: "Kitchen tap install",
    total: 1150,
    status: "accepted",
    createdAt: "2026-08-14",
  },
  {
    id: "q-0041",
    number: "Q-0041",
    customerName: "Themba Nkosi",
    description: "Geyser replacement",
    total: 4780,
    status: "sent",
    createdAt: "2026-08-13",
  },
  {
    id: "q-0040",
    number: "Q-0040",
    customerName: "Lindiwe Zulu",
    description: "Bathroom re-fit",
    total: 8300,
    status: "accepted",
    createdAt: "2026-08-11",
  },
  {
    id: "q-0039",
    number: "Q-0039",
    customerName: "Kagiso Molefe",
    description: "Pipe repair",
    total: 2050,
    status: "draft",
    createdAt: "2026-08-10",
  },
];

export const invoices: Invoice[] = [
  {
    id: "inv-0018",
    number: "INV-0018",
    customerName: "Nomvula Dlamini",
    total: 1150,
    status: "paid",
    dueDate: "2026-08-10",
  },
  {
    id: "inv-0017",
    number: "INV-0017",
    customerName: "Themba Nkosi",
    total: 4780,
    status: "unpaid",
    dueDate: "2026-08-20",
  },
  {
    id: "inv-0016",
    number: "INV-0016",
    customerName: "Sizwe Khumalo",
    total: 990,
    status: "overdue",
    dueDate: "2026-08-05",
  },
];

export function formatCurrency(amount: number): string {
  return `R ${amount.toLocaleString("en-ZA", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}
