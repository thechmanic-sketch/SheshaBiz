"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type {
  BusinessProfile,
  Customer,
  Invoice,
  InvoiceStatus,
  LineItem,
  Product,
  Quote,
  QuoteStatus,
  Sale,
} from "./types";
import { quoteTotals } from "./types";
import {
  seedBusiness,
  seedCounters,
  seedCustomers,
  seedInvoices,
  seedProducts,
  seedQuotes,
  seedSales,
} from "./seed-data";

const STORAGE_KEY = "sheshabiz-web-data-v1";

interface AppData {
  business: BusinessProfile;
  customers: Customer[];
  products: Product[];
  quotes: Quote[];
  invoices: Invoice[];
  sales: Sale[];
  quoteCounter: number;
  invoiceCounter: number;
  saleCounter: number;
  onboarded: boolean;
}

const seedData: AppData = {
  business: seedBusiness,
  customers: seedCustomers,
  products: seedProducts,
  quotes: seedQuotes,
  invoices: seedInvoices,
  sales: seedSales,
  ...seedCounters,
  onboarded: false,
};

function newId(): string {
  if (typeof crypto !== "undefined" && crypto.randomUUID) return crypto.randomUUID();
  return `id-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function nowIso(): string {
  return new Date().toISOString();
}

function pad(n: number): string {
  return String(n).padStart(4, "0");
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

/**
 * A document's display number is `${prefix}-${pad(counter)}` (e.g.
 * "Q-0042"). Pulling a quote/invoice/sale created on another device can
 * hand back a number sequenced ahead of this device's local counter; if
 * this device then created a new document off its stale counter, it could
 * mint a duplicate number and collide with the server's
 * unique(business_id, number) constraint on next push. Recovering the
 * highest counter implied by the merged set — after every pull — keeps
 * this device's next-minted number ahead of anything it has seen.
 */
function maxCounterFromNumbers(numbers: string[]): number {
  let max = 0;
  for (const number of numbers) {
    const match = number.match(/(\d+)$/);
    if (!match) continue;
    const n = Number(match[1]);
    if (Number.isFinite(n) && n > max) max = n;
  }
  return max;
}

/** Upsert-by-id, last-write-wins on `updatedAt`. Soft-delete tombstones
 * (`deletedAt` set) merge the same way as any other update — the row stays
 * in the array, just marked deleted, so its `id` keeps resolving for
 * anything that still references it (e.g. an invoice's `fromQuoteId`). */
function mergeById<T extends { id: string; updatedAt: string }>(local: T[], remote: T[]): T[] {
  if (remote.length === 0) return local;
  const byId = new Map(local.map((item) => [item.id, item]));
  for (const incoming of remote) {
    const existing = byId.get(incoming.id);
    if (!existing || incoming.updatedAt >= existing.updatedAt) {
      byId.set(incoming.id, incoming);
    }
  }
  // Preserve local ordering (newest-first) for anything already present,
  // then append genuinely new rows at the front.
  const existingIds = new Set(local.map((item) => item.id));
  const merged = local.map((item) => byId.get(item.id) as T);
  const added = remote.filter((item) => !existingIds.has(item.id)).map((item) => byId.get(item.id) as T);
  return [...added, ...merged];
}

/** Payload shape the sync engine's pull step hands to `mergeRemoteData`.
 * Line items are already assembled back onto their parent
 * quote/invoice/sale by the time this is called — the local data model
 * embeds `lineItems`, it doesn't track `*_items` as separate arrays. */
export interface RemoteDataPayload {
  business?: (Partial<BusinessProfile> & { updatedAt: string }) | null;
  customers?: Customer[];
  products?: Product[];
  quotes?: Quote[];
  invoices?: Invoice[];
  sales?: Sale[];
}

interface AppDataContextValue {
  data: AppData;
  loaded: boolean;

  // Filtered, soft-delete-aware views for anything that lists/displays
  // records — pickers, dropdowns, tables. `data.<entity>` itself still
  // holds tombstoned rows (deletedAt set), since ids must keep resolving
  // for cross-references and for sync merges.
  visibleCustomers: Customer[];
  visibleProducts: Product[];
  visibleQuotes: Quote[];
  visibleInvoices: Invoice[];
  visibleSales: Sale[];

  updateBusiness: (patch: Partial<BusinessProfile>) => void;

  addCustomer: (input: Omit<Customer, "id" | "updatedAt" | "deletedAt">) => Customer;
  updateCustomer: (id: string, patch: Partial<Customer>) => void;
  deleteCustomer: (id: string) => void;

  addProduct: (input: Omit<Product, "id" | "updatedAt" | "deletedAt">) => Product;
  updateProduct: (id: string, patch: Partial<Product>) => void;
  deleteProduct: (id: string) => void;

  addQuote: (input: {
    customerId: string | null;
    customerName: string;
    description: string;
    lineItems: LineItem[];
    status: QuoteStatus;
  }) => Quote;
  updateQuote: (id: string, patch: Partial<Quote>) => void;
  deleteQuote: (id: string) => void;
  convertQuoteToInvoice: (quoteId: string, dueDate: string) => Invoice | null;

  addInvoice: (input: {
    customerId: string | null;
    customerName: string;
    lineItems: LineItem[];
    status: InvoiceStatus;
    dueDate: string;
  }) => Invoice;
  updateInvoice: (id: string, patch: Partial<Invoice>) => void;
  deleteInvoice: (id: string) => void;

  addSale: (input: {
    lineItems: LineItem[];
    paymentMethod: Sale["paymentMethod"];
    amountTendered: number | null;
    changeGiven: number | null;
  }) => Sale;

  mergeRemoteData: (payload: RemoteDataPayload) => void;

  exportJson: () => string;
  importJson: (json: string) => boolean;
  resetDemoData: () => void;
  completeOnboarding: (profile: Partial<BusinessProfile>) => void;
}

const AppDataContext = createContext<AppDataContextValue | null>(null);

export function AppDataProvider({ children }: { children: ReactNode }) {
  const [data, setData] = useState<AppData>(seedData);
  // `loaded` (state, not a ref) gates the save effect below. It must be
  // state: React Strict Mode replays effect setup functions twice on mount,
  // and a ref survives that replay while state doesn't, so a ref-based gate
  // lets the save effect's second pass fire before the load has actually
  // committed — clobbering real localStorage data with the still-stale
  // seed-data closure. Gating on state means the save effect only sees
  // loaded=true once the load has genuinely committed and re-rendered.
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    // One-time sync from localStorage (an external system) on mount, so the
    // server-rendered seed data hydrates cleanly before this client-only read.
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as AppData;
        // Blobs saved before onboarding existed have no `onboarded` field.
        // Their presence at all means this browser already has real usage,
        // so treat that as already onboarded rather than re-prompting.
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setData({ ...parsed, onboarded: parsed.onboarded ?? true });
      }
    } catch {
      // ignore corrupt storage, keep seed data
    }
    setLoaded(true);
  }, []);

  useEffect(() => {
    if (!loaded) return;
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    } catch {
      // storage full or unavailable — ignore
    }
  }, [data, loaded]);

  const visibleCustomers = useMemo(() => data.customers.filter((c) => !c.deletedAt), [data.customers]);
  const visibleProducts = useMemo(() => data.products.filter((p) => !p.deletedAt), [data.products]);
  const visibleQuotes = useMemo(() => data.quotes.filter((q) => !q.deletedAt), [data.quotes]);
  const visibleInvoices = useMemo(() => data.invoices.filter((i) => !i.deletedAt), [data.invoices]);
  const visibleSales = useMemo(() => data.sales.filter((s) => !s.deletedAt), [data.sales]);

  const updateBusiness = useCallback((patch: Partial<BusinessProfile>) => {
    setData((d) => ({ ...d, business: { ...d.business, ...patch, updatedAt: nowIso() } }));
  }, []);

  const addCustomer = useCallback(
    (input: Omit<Customer, "id" | "updatedAt" | "deletedAt">) => {
      const customer: Customer = { ...input, id: newId(), updatedAt: nowIso(), deletedAt: null };
      setData({ ...data, customers: [customer, ...data.customers] });
      return customer;
    },
    [data]
  );

  const updateCustomer = useCallback((id: string, patch: Partial<Customer>) => {
    setData((d) => ({
      ...d,
      customers: d.customers.map((c) => (c.id === id ? { ...c, ...patch, updatedAt: nowIso() } : c)),
    }));
  }, []);

  const deleteCustomer = useCallback((id: string) => {
    setData((d) => ({
      ...d,
      customers: d.customers.map((c) => (c.id === id ? { ...c, deletedAt: nowIso(), updatedAt: nowIso() } : c)),
    }));
  }, []);

  const addProduct = useCallback(
    (input: Omit<Product, "id" | "updatedAt" | "deletedAt">) => {
      const product: Product = { ...input, id: newId(), updatedAt: nowIso(), deletedAt: null };
      setData({ ...data, products: [product, ...data.products] });
      return product;
    },
    [data]
  );

  const updateProduct = useCallback((id: string, patch: Partial<Product>) => {
    setData((d) => ({
      ...d,
      products: d.products.map((p) => (p.id === id ? { ...p, ...patch, updatedAt: nowIso() } : p)),
    }));
  }, []);

  const deleteProduct = useCallback((id: string) => {
    setData((d) => ({
      ...d,
      products: d.products.map((p) => (p.id === id ? { ...p, deletedAt: nowIso(), updatedAt: nowIso() } : p)),
    }));
  }, []);

  const addQuote = useCallback(
    (input: {
      customerId: string | null;
      customerName: string;
      description: string;
      lineItems: LineItem[];
      status: QuoteStatus;
    }) => {
      const counter = data.quoteCounter + 1;
      const created: Quote = {
        id: newId(),
        number: `${data.business.quotePrefix}-${pad(counter)}`,
        customerId: input.customerId,
        customerName: input.customerName,
        description: input.description,
        lineItems: input.lineItems,
        status: input.status,
        createdAt: todayIso(),
        convertedToInvoiceId: null,
        updatedAt: nowIso(),
        deletedAt: null,
      };
      setData({ ...data, quotes: [created, ...data.quotes], quoteCounter: counter });
      return created;
    },
    [data]
  );

  const updateQuote = useCallback((id: string, patch: Partial<Quote>) => {
    setData((d) => ({
      ...d,
      quotes: d.quotes.map((q) => (q.id === id ? { ...q, ...patch, updatedAt: nowIso() } : q)),
    }));
  }, []);

  const deleteQuote = useCallback((id: string) => {
    setData((d) => ({
      ...d,
      quotes: d.quotes.map((q) => (q.id === id ? { ...q, deletedAt: nowIso(), updatedAt: nowIso() } : q)),
    }));
  }, []);

  const addInvoice = useCallback(
    (input: {
      customerId: string | null;
      customerName: string;
      lineItems: LineItem[];
      status: InvoiceStatus;
      dueDate: string;
    }) => {
      const counter = data.invoiceCounter + 1;
      const created: Invoice = {
        id: newId(),
        number: `${data.business.invoicePrefix}-${pad(counter)}`,
        customerId: input.customerId,
        customerName: input.customerName,
        lineItems: input.lineItems,
        status: input.status,
        createdAt: todayIso(),
        dueDate: input.dueDate,
        fromQuoteId: null,
        updatedAt: nowIso(),
        deletedAt: null,
      };
      setData({ ...data, invoices: [created, ...data.invoices], invoiceCounter: counter });
      return created;
    },
    [data]
  );

  const updateInvoice = useCallback((id: string, patch: Partial<Invoice>) => {
    setData((d) => ({
      ...d,
      invoices: d.invoices.map((i) => (i.id === id ? { ...i, ...patch, updatedAt: nowIso() } : i)),
    }));
  }, []);

  const deleteInvoice = useCallback((id: string) => {
    setData((d) => ({
      ...d,
      invoices: d.invoices.map((i) => (i.id === id ? { ...i, deletedAt: nowIso(), updatedAt: nowIso() } : i)),
    }));
  }, []);

  const convertQuoteToInvoice = useCallback(
    (quoteId: string, dueDate: string) => {
      const quote = data.quotes.find((q) => q.id === quoteId);
      if (!quote) return null;
      const counter = data.invoiceCounter + 1;
      const created: Invoice = {
        id: newId(),
        number: `${data.business.invoicePrefix}-${pad(counter)}`,
        customerId: quote.customerId,
        customerName: quote.customerName,
        lineItems: quote.lineItems,
        status: "unpaid",
        createdAt: todayIso(),
        dueDate,
        fromQuoteId: quote.id,
        updatedAt: nowIso(),
        deletedAt: null,
      };
      setData({
        ...data,
        invoices: [created, ...data.invoices],
        invoiceCounter: counter,
        quotes: data.quotes.map((q) =>
          q.id === quoteId ? { ...q, convertedToInvoiceId: created.id, updatedAt: nowIso() } : q
        ),
      });
      return created;
    },
    [data]
  );

  const addSale = useCallback(
    (input: {
      lineItems: LineItem[];
      paymentMethod: Sale["paymentMethod"];
      amountTendered: number | null;
      changeGiven: number | null;
    }) => {
      const counter = data.saleCounter + 1;
      const created: Sale = {
        id: newId(),
        number: `${data.business.receiptPrefix}-${pad(counter)}`,
        lineItems: input.lineItems,
        paymentMethod: input.paymentMethod,
        amountTendered: input.amountTendered,
        changeGiven: input.changeGiven,
        createdAt: todayIso(),
        updatedAt: nowIso(),
        deletedAt: null,
      };
      const stockDelta = new Map<string, number>();
      for (const item of input.lineItems) {
        stockDelta.set(item.description, (stockDelta.get(item.description) ?? 0) + item.qty);
      }
      setData({
        ...data,
        sales: [created, ...data.sales],
        saleCounter: counter,
        products: data.products.map((p) =>
          stockDelta.has(p.name)
            ? { ...p, stockQty: Math.max(0, p.stockQty - (stockDelta.get(p.name) ?? 0)), updatedAt: nowIso() }
            : p
        ),
      });
      return created;
    },
    [data]
  );

  const mergeRemoteData = useCallback((payload: RemoteDataPayload) => {
    setData((d) => {
      const mergedQuotes = payload.quotes ? mergeById(d.quotes, payload.quotes) : d.quotes;
      const mergedInvoices = payload.invoices ? mergeById(d.invoices, payload.invoices) : d.invoices;
      const mergedSales = payload.sales ? mergeById(d.sales, payload.sales) : d.sales;

      const business =
        payload.business && (!d.business.updatedAt || payload.business.updatedAt >= d.business.updatedAt)
          ? { ...d.business, ...payload.business }
          : d.business;

      return {
        ...d,
        business,
        customers: payload.customers ? mergeById(d.customers, payload.customers) : d.customers,
        products: payload.products ? mergeById(d.products, payload.products) : d.products,
        quotes: mergedQuotes,
        invoices: mergedInvoices,
        sales: mergedSales,
        quoteCounter: Math.max(d.quoteCounter, maxCounterFromNumbers(mergedQuotes.map((q) => q.number))),
        invoiceCounter: Math.max(d.invoiceCounter, maxCounterFromNumbers(mergedInvoices.map((i) => i.number))),
        saleCounter: Math.max(d.saleCounter, maxCounterFromNumbers(mergedSales.map((s) => s.number))),
      };
    });
  }, []);

  const exportJson = useCallback(() => JSON.stringify(data, null, 2), [data]);

  const importJson = useCallback((json: string) => {
    try {
      const parsed = JSON.parse(json) as AppData;
      if (!parsed.business || !Array.isArray(parsed.quotes)) return false;
      setData({ ...parsed, onboarded: parsed.onboarded ?? true });
      return true;
    } catch {
      return false;
    }
  }, []);

  // Resetting demo data is something an already-onboarded user does from
  // Settings, not a fresh install, so it should not re-trigger onboarding.
  const resetDemoData = useCallback(() => setData({ ...seedData, onboarded: true }), []);

  const completeOnboarding = useCallback(
    (profile: Partial<BusinessProfile>) => {
      setData({
        ...data,
        business: { ...data.business, ...profile, updatedAt: nowIso() },
        // A fresh business starts with a clean slate, not the sample
        // "Sipho's Plumbing" quotes/invoices/customers/products used to
        // demonstrate the app before setup.
        customers: [],
        products: [],
        quotes: [],
        invoices: [],
        sales: [],
        quoteCounter: 0,
        invoiceCounter: 0,
        saleCounter: 0,
        onboarded: true,
      });
    },
    [data]
  );

  const value = useMemo<AppDataContextValue>(
    () => ({
      data,
      loaded,
      visibleCustomers,
      visibleProducts,
      visibleQuotes,
      visibleInvoices,
      visibleSales,
      updateBusiness,
      addCustomer,
      updateCustomer,
      deleteCustomer,
      addProduct,
      updateProduct,
      deleteProduct,
      addQuote,
      updateQuote,
      deleteQuote,
      convertQuoteToInvoice,
      addInvoice,
      updateInvoice,
      deleteInvoice,
      addSale,
      mergeRemoteData,
      exportJson,
      importJson,
      resetDemoData,
      completeOnboarding,
    }),
    [
      data,
      loaded,
      visibleCustomers,
      visibleProducts,
      visibleQuotes,
      visibleInvoices,
      visibleSales,
      updateBusiness,
      addCustomer,
      updateCustomer,
      deleteCustomer,
      addProduct,
      updateProduct,
      deleteProduct,
      addQuote,
      updateQuote,
      deleteQuote,
      convertQuoteToInvoice,
      addInvoice,
      updateInvoice,
      deleteInvoice,
      addSale,
      mergeRemoteData,
      exportJson,
      importJson,
      resetDemoData,
      completeOnboarding,
    ]
  );

  return <AppDataContext.Provider value={value}>{children}</AppDataContext.Provider>;
}

export function useAppData(): AppDataContextValue {
  const ctx = useContext(AppDataContext);
  if (!ctx) throw new Error("useAppData must be used within AppDataProvider");
  return ctx;
}

export function useQuoteTotals(quote: Pick<Quote, "lineItems">) {
  const { data } = useAppData();
  return quoteTotals(quote, data.business.vatRate);
}
