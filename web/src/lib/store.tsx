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
}

const seedData: AppData = {
  business: seedBusiness,
  customers: seedCustomers,
  products: seedProducts,
  quotes: seedQuotes,
  invoices: seedInvoices,
  sales: seedSales,
  ...seedCounters,
};

function newId(): string {
  if (typeof crypto !== "undefined" && crypto.randomUUID) return crypto.randomUUID();
  return `id-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function pad(n: number): string {
  return String(n).padStart(4, "0");
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

interface AppDataContextValue {
  data: AppData;
  loaded: boolean;

  updateBusiness: (patch: Partial<BusinessProfile>) => void;

  addCustomer: (input: Omit<Customer, "id">) => Customer;
  updateCustomer: (id: string, patch: Partial<Customer>) => void;
  deleteCustomer: (id: string) => void;

  addProduct: (input: Omit<Product, "id">) => Product;
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

  exportJson: () => string;
  importJson: (json: string) => boolean;
  resetDemoData: () => void;
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
      // eslint-disable-next-line react-hooks/set-state-in-effect
      if (raw) setData(JSON.parse(raw) as AppData);
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

  const updateBusiness = useCallback((patch: Partial<BusinessProfile>) => {
    setData((d) => ({ ...d, business: { ...d.business, ...patch } }));
  }, []);

  const addCustomer = useCallback(
    (input: Omit<Customer, "id">) => {
      const customer: Customer = { ...input, id: newId() };
      setData({ ...data, customers: [customer, ...data.customers] });
      return customer;
    },
    [data]
  );

  const updateCustomer = useCallback((id: string, patch: Partial<Customer>) => {
    setData((d) => ({
      ...d,
      customers: d.customers.map((c) => (c.id === id ? { ...c, ...patch } : c)),
    }));
  }, []);

  const deleteCustomer = useCallback((id: string) => {
    setData((d) => ({ ...d, customers: d.customers.filter((c) => c.id !== id) }));
  }, []);

  const addProduct = useCallback(
    (input: Omit<Product, "id">) => {
      const product: Product = { ...input, id: newId() };
      setData({ ...data, products: [product, ...data.products] });
      return product;
    },
    [data]
  );

  const updateProduct = useCallback((id: string, patch: Partial<Product>) => {
    setData((d) => ({
      ...d,
      products: d.products.map((p) => (p.id === id ? { ...p, ...patch } : p)),
    }));
  }, []);

  const deleteProduct = useCallback((id: string) => {
    setData((d) => ({ ...d, products: d.products.filter((p) => p.id !== id) }));
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
      };
      setData({ ...data, quotes: [created, ...data.quotes], quoteCounter: counter });
      return created;
    },
    [data]
  );

  const updateQuote = useCallback((id: string, patch: Partial<Quote>) => {
    setData((d) => ({
      ...d,
      quotes: d.quotes.map((q) => (q.id === id ? { ...q, ...patch } : q)),
    }));
  }, []);

  const deleteQuote = useCallback((id: string) => {
    setData((d) => ({ ...d, quotes: d.quotes.filter((q) => q.id !== id) }));
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
      };
      setData({ ...data, invoices: [created, ...data.invoices], invoiceCounter: counter });
      return created;
    },
    [data]
  );

  const updateInvoice = useCallback((id: string, patch: Partial<Invoice>) => {
    setData((d) => ({
      ...d,
      invoices: d.invoices.map((i) => (i.id === id ? { ...i, ...patch } : i)),
    }));
  }, []);

  const deleteInvoice = useCallback((id: string) => {
    setData((d) => ({ ...d, invoices: d.invoices.filter((i) => i.id !== id) }));
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
      };
      setData({
        ...data,
        invoices: [created, ...data.invoices],
        invoiceCounter: counter,
        quotes: data.quotes.map((q) =>
          q.id === quoteId ? { ...q, convertedToInvoiceId: created.id } : q
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
            ? { ...p, stockQty: Math.max(0, p.stockQty - (stockDelta.get(p.name) ?? 0)) }
            : p
        ),
      });
      return created;
    },
    [data]
  );

  const exportJson = useCallback(() => JSON.stringify(data, null, 2), [data]);

  const importJson = useCallback((json: string) => {
    try {
      const parsed = JSON.parse(json) as AppData;
      if (!parsed.business || !Array.isArray(parsed.quotes)) return false;
      setData(parsed);
      return true;
    } catch {
      return false;
    }
  }, []);

  const resetDemoData = useCallback(() => setData(seedData), []);

  const value = useMemo<AppDataContextValue>(
    () => ({
      data,
      loaded,
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
      exportJson,
      importJson,
      resetDemoData,
    }),
    [
      data,
      loaded,
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
      exportJson,
      importJson,
      resetDemoData,
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
