import type { BusinessProfile, Customer, Invoice, Product, Quote, Sale } from "./types";

export const seedBusiness: BusinessProfile = {
  name: "Sipho's Plumbing",
  logoDataUrl: null,
  regNumber: "2019/123456/07",
  bankName: "Capitec Business",
  accountNumber: "1234567890",
  branchCode: "470010",
  vatRate: 15,
  paymentTerms: "Payment due within 14 days.",
  country: "south_africa",
  quotePrefix: "Q",
  invoicePrefix: "INV",
  receiptPrefix: "R",
};

export const seedCustomers: Customer[] = [
  { id: "cust-1", name: "Nomvula Dlamini", phone: "071 234 5678", email: "nomvula@example.com", address: "12 Bloem St, Pretoria" },
  { id: "cust-2", name: "Themba Nkosi", phone: "082 345 6789", email: "themba@example.com", address: "45 Church St, Polokwane" },
  { id: "cust-3", name: "Lindiwe Zulu", phone: "083 456 7890", email: "lindiwe@example.com", address: "8 Market Rd, Durban" },
  { id: "cust-4", name: "Kagiso Molefe", phone: "084 567 8901", email: "kagiso@example.com", address: "3 Voortrekker Ave, Bloemfontein" },
  { id: "cust-5", name: "Sizwe Khumalo", phone: "072 678 9012", email: "sizwe@example.com", address: "19 Kerk St, Nelspruit" },
];

export const seedProducts: Product[] = [
  { id: "prod-1", name: "Callout fee", price: 350, stockQty: 999, imageDataUrl: null },
  { id: "prod-2", name: "Tap replacement", price: 450, stockQty: 20, imageDataUrl: null },
  { id: "prod-3", name: "Geyser element", price: 620, stockQty: 8, imageDataUrl: null },
  { id: "prod-4", name: "PVC pipe (per meter)", price: 65, stockQty: 120, imageDataUrl: null },
  { id: "prod-5", name: "Labour (per hour)", price: 380, stockQty: 999, imageDataUrl: null },
];

export const seedQuotes: Quote[] = [
  {
    id: "q-42",
    number: "Q-0042",
    customerId: "cust-1",
    customerName: "Nomvula Dlamini",
    description: "Kitchen tap install",
    lineItems: [
      { id: "li-1", description: "Callout fee", qty: 1, unitPrice: 350 },
      { id: "li-2", description: "Tap replacement", qty: 1, unitPrice: 450 },
      { id: "li-3", description: "Labour (per hour)", qty: 0.9, unitPrice: 380 },
    ],
    status: "accepted",
    createdAt: "2026-08-14",
    convertedToInvoiceId: "inv-18",
  },
  {
    id: "q-41",
    number: "Q-0041",
    customerId: "cust-2",
    customerName: "Themba Nkosi",
    description: "Geyser replacement",
    lineItems: [
      { id: "li-4", description: "Geyser element", qty: 2, unitPrice: 620 },
      { id: "li-5", description: "Labour (per hour)", qty: 9.05, unitPrice: 380 },
    ],
    status: "sent",
    createdAt: "2026-08-13",
    convertedToInvoiceId: null,
  },
  {
    id: "q-40",
    number: "Q-0040",
    customerId: "cust-3",
    customerName: "Lindiwe Zulu",
    description: "Bathroom re-fit",
    lineItems: [
      { id: "li-6", description: "PVC pipe (per meter)", qty: 40, unitPrice: 65 },
      { id: "li-7", description: "Labour (per hour)", qty: 15, unitPrice: 380 },
    ],
    status: "accepted",
    createdAt: "2026-08-11",
    convertedToInvoiceId: null,
  },
  {
    id: "q-39",
    number: "Q-0039",
    customerId: "cust-4",
    customerName: "Kagiso Molefe",
    description: "Pipe repair",
    lineItems: [
      { id: "li-8", description: "Callout fee", qty: 1, unitPrice: 350 },
      { id: "li-9", description: "Labour (per hour)", qty: 4.5, unitPrice: 380 },
    ],
    status: "draft",
    createdAt: "2026-08-10",
    convertedToInvoiceId: null,
  },
];

export const seedInvoices: Invoice[] = [
  {
    id: "inv-18",
    number: "INV-0018",
    customerId: "cust-1",
    customerName: "Nomvula Dlamini",
    lineItems: [
      { id: "li-1b", description: "Callout fee", qty: 1, unitPrice: 350 },
      { id: "li-2b", description: "Tap replacement", qty: 1, unitPrice: 450 },
      { id: "li-3b", description: "Labour (per hour)", qty: 0.9, unitPrice: 380 },
    ],
    status: "paid",
    createdAt: "2026-08-14",
    dueDate: "2026-08-10",
    fromQuoteId: "q-42",
  },
  {
    id: "inv-17",
    number: "INV-0017",
    customerId: "cust-2",
    customerName: "Themba Nkosi",
    lineItems: [
      { id: "li-10", description: "Geyser element", qty: 2, unitPrice: 620 },
      { id: "li-11", description: "Labour (per hour)", qty: 9.05, unitPrice: 380 },
    ],
    status: "unpaid",
    createdAt: "2026-08-13",
    dueDate: "2026-08-20",
    fromQuoteId: null,
  },
  {
    id: "inv-16",
    number: "INV-0016",
    customerId: "cust-5",
    customerName: "Sizwe Khumalo",
    lineItems: [{ id: "li-12", description: "Callout fee + minor repair", qty: 1, unitPrice: 990 }],
    status: "overdue",
    createdAt: "2026-08-01",
    dueDate: "2026-08-05",
    fromQuoteId: null,
  },
];

export const seedSales: Sale[] = [
  {
    id: "sale-1",
    number: "R-0001",
    lineItems: [
      { id: "li-13", description: "Tap replacement", qty: 1, unitPrice: 450 },
      { id: "li-14", description: "Callout fee", qty: 1, unitPrice: 350 },
    ],
    paymentMethod: "cash",
    amountTendered: 900,
    changeGiven: 100,
    createdAt: "2026-08-15",
  },
];

export const seedCounters = {
  quoteCounter: 42,
  invoiceCounter: 18,
  saleCounter: 1,
};
