"use client";

import { useCallback, useEffect, useRef } from "react";
import { supabase } from "./supabase";
import { useAppData, type RemoteDataPayload } from "./store";
import type { BusinessProfile, Customer, Invoice, LineItem, Product, Quote, Sale } from "./types";

const LAST_SYNC_KEY = "sheshabiz-web-last-sync-v1";
const EPOCH = "1970-01-01T00:00:00.000Z";
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function isUuid(id: string | null | undefined): id is string {
  return !!id && UUID_RE.test(id);
}

/** Local ids not shaped like a UUID (leftover demo/seed data predating this
 * feature, or the pre-`crypto.randomUUID` id fallback) can't satisfy a
 * `uuid` primary key — skip pushing those rows rather than erroring the
 * whole pass. Anything created through the app's real add/update actions
 * already gets a proper UUID (see `newId()` in `./store`). */
function safeUuid(id: string | null): string | null {
  return isUuid(id) ? id : null;
}

// ----------------------------------------------------------------------------
// Sync bus: bridges AuthProvider (outside AppDataProvider, so it can't call
// mergeRemoteData itself) to whichever useSyncEngine instance is mounted.
// ----------------------------------------------------------------------------

type SyncListener = () => void;
let activeListener: SyncListener | null = null;

/** Called by auth.tsx right after a successful verifyCode. */
export function requestSync(): void {
  activeListener?.();
}

/** The pull watermark used internally to ask for "changes since ...". */
function getSyncWatermark(): string {
  try {
    return window.localStorage.getItem(LAST_SYNC_KEY) ?? EPOCH;
  } catch {
    return EPOCH;
  }
}

/** Exposed for the Settings page's Cloud sync card. Returns `null` if a
 * sync pass has never completed successfully on this device. */
export function getLastSyncedAt(): string | null {
  const raw = getSyncWatermark();
  return raw === EPOCH ? null : raw;
}

function setLastSyncedAt(iso: string): void {
  try {
    window.localStorage.setItem(LAST_SYNC_KEY, iso);
  } catch {
    // ignore — worst case, next pass re-pulls a bit more than it needs to
  }
}

// ----------------------------------------------------------------------------
// Push (parent-before-child: business_profiles/customers/products →
// quotes/invoices/sales → *_items)
// ----------------------------------------------------------------------------

async function pushBusinessProfile(bizId: string, business: BusinessProfile): Promise<void> {
  const row = {
    business_id: bizId,
    name: business.name,
    logo_data_url: business.logoDataUrl,
    reg_number: business.regNumber,
    bank_name: business.bankName,
    account_number: business.accountNumber,
    branch_code: business.branchCode,
    vat_rate: business.vatRate,
    payment_terms: business.paymentTerms,
    country: business.country,
    quote_prefix: business.quotePrefix,
    invoice_prefix: business.invoicePrefix,
    receipt_prefix: business.receiptPrefix,
    delete_pin_hash: business.deletePinHash,
    updated_at: business.updatedAt,
  };
  const { error } = await supabase.from("business_profiles").upsert(row, { onConflict: "business_id" });
  if (error) throw error;
}

async function pushCustomers(bizId: string, customers: Customer[]): Promise<void> {
  const rows = customers
    .filter((c) => isUuid(c.id))
    .map((c) => ({
      id: c.id,
      business_id: bizId,
      name: c.name,
      phone: c.phone,
      email: c.email,
      address: c.address,
      updated_at: c.updatedAt,
      deleted_at: c.deletedAt,
    }));
  if (rows.length === 0) return;
  const { error } = await supabase.from("customers").upsert(rows, { onConflict: "id" });
  if (error) throw error;
}

async function pushProducts(bizId: string, products: Product[]): Promise<void> {
  const rows = products
    .filter((p) => isUuid(p.id))
    .map((p) => ({
      id: p.id,
      business_id: bizId,
      name: p.name,
      price: p.price,
      stock_qty: p.stockQty,
      image_data_url: p.imageDataUrl,
      unit_price: p.price,
      stock_quantity: p.stockQty,
      image_uri: p.imageDataUrl,
      updated_at: p.updatedAt,
      deleted_at: p.deletedAt,
    }));
  if (rows.length === 0) return;
  const { error } = await supabase.from("products").upsert(rows, { onConflict: "id" });
  if (error) throw error;
}

async function pushQuotes(bizId: string, quotes: Quote[]): Promise<Quote[]> {
  const valid = quotes.filter((q) => isUuid(q.id));
  if (valid.length === 0) return valid;
  // Phase 1: everything except the forward link to invoices, which may not
  // exist server-side yet (invoices are pushed after quotes in this pass).
  const rows = valid.map((q) => ({
    id: q.id,
    business_id: bizId,
    customer_id: safeUuid(q.customerId),
    customer_name: q.customerName,
    status: q.status,
    created_at: q.createdAt,
    number: q.number,
    description: q.description,
    updated_at: q.updatedAt,
    deleted_at: q.deletedAt,
  }));
  const { error } = await supabase.from("quotes").upsert(rows, { onConflict: "id" });
  if (error) throw error;
  return valid;
}

async function pushQuoteInvoiceLinks(bizId: string, quotes: Quote[]): Promise<void> {
  // Phase 2, run after invoices have been pushed: fill in
  // converted_to_invoice_id for quotes that have one.
  const rows = quotes
    .filter((q) => isUuid(q.id) && isUuid(q.convertedToInvoiceId))
    .map((q) => ({ id: q.id, business_id: bizId, converted_to_invoice_id: q.convertedToInvoiceId }));
  if (rows.length === 0) return;
  const { error } = await supabase.from("quotes").upsert(rows, { onConflict: "id" });
  if (error) throw error;
}

async function pushInvoices(bizId: string, invoices: Invoice[]): Promise<Invoice[]> {
  const valid = invoices.filter((i) => isUuid(i.id));
  if (valid.length === 0) return valid;
  const rows = valid.map((i) => ({
    id: i.id,
    business_id: bizId,
    customer_id: safeUuid(i.customerId),
    from_quote_id: safeUuid(i.fromQuoteId),
    customer_name: i.customerName,
    status: i.status,
    created_at: i.createdAt,
    due_date: i.dueDate,
    number: i.number,
    updated_at: i.updatedAt,
    deleted_at: i.deletedAt,
  }));
  const { error } = await supabase.from("invoices").upsert(rows, { onConflict: "id" });
  if (error) throw error;
  return valid;
}

async function pushSales(bizId: string, sales: Sale[]): Promise<Sale[]> {
  const valid = sales.filter((s) => isUuid(s.id));
  if (valid.length === 0) return valid;
  const rows = valid.map((s) => ({
    id: s.id,
    business_id: bizId,
    payment_method: s.paymentMethod,
    amount_tendered: s.amountTendered,
    change_given: s.changeGiven,
    created_at: s.createdAt,
    number: s.number,
    sale_number: s.number,
    updated_at: s.updatedAt,
    deleted_at: s.deletedAt,
  }));
  const { error } = await supabase.from("sales").upsert(rows, { onConflict: "id" });
  if (error) throw error;
  return valid;
}

function lineItemRows(
  bizId: string,
  parentIdKey: "quote_id" | "invoice_id" | "sale_id",
  parentId: string,
  parentUpdatedAt: string,
  items: LineItem[]
) {
  return items.map((item, index) => ({
    id: isUuid(item.id) ? item.id : (typeof crypto !== "undefined" ? crypto.randomUUID() : `${parentId}-${index}`),
    business_id: bizId,
    [parentIdKey]: parentId,
    description: item.description,
    unit_price: item.unitPrice,
    qty: item.qty,
    quantity: item.qty,
    sort_order: index,
    line_total: item.qty * item.unitPrice,
    updated_at: parentUpdatedAt,
    deleted_at: null as string | null,
  }));
}

async function pushQuoteItems(bizId: string, quotes: Quote[]): Promise<void> {
  const rows = quotes
    .filter((q) => isUuid(q.id))
    .flatMap((q) => lineItemRows(bizId, "quote_id", q.id, q.updatedAt, q.lineItems));
  if (rows.length === 0) return;
  const { error } = await supabase.from("quote_items").upsert(rows, { onConflict: "id" });
  if (error) throw error;
}

async function pushInvoiceItems(bizId: string, invoices: Invoice[]): Promise<void> {
  const rows = invoices
    .filter((i) => isUuid(i.id))
    .flatMap((i) => lineItemRows(bizId, "invoice_id", i.id, i.updatedAt, i.lineItems));
  if (rows.length === 0) return;
  const { error } = await supabase.from("invoice_items").upsert(rows, { onConflict: "id" });
  if (error) throw error;
}

async function pushSaleItems(bizId: string, sales: Sale[]): Promise<void> {
  const rows = sales
    .filter((s) => isUuid(s.id))
    .flatMap((s) => lineItemRows(bizId, "sale_id", s.id, s.updatedAt, s.lineItems));
  if (rows.length === 0) return;
  const { error } = await supabase.from("sale_items").upsert(rows, { onConflict: "id" });
  if (error) throw error;
}

interface LocalSnapshot {
  business: BusinessProfile;
  customers: Customer[];
  products: Product[];
  quotes: Quote[];
  invoices: Invoice[];
  sales: Sale[];
}

async function pushAll(bizId: string, snapshot: LocalSnapshot): Promise<void> {
  await pushBusinessProfile(bizId, snapshot.business);
  await pushCustomers(bizId, snapshot.customers);
  await pushProducts(bizId, snapshot.products);

  const pushedQuotes = await pushQuotes(bizId, snapshot.quotes);
  const pushedInvoices = await pushInvoices(bizId, snapshot.invoices);
  await pushSales(bizId, snapshot.sales);

  await pushQuoteInvoiceLinks(bizId, pushedQuotes);

  await pushQuoteItems(bizId, pushedQuotes);
  await pushInvoiceItems(bizId, pushedInvoices);
  await pushSaleItems(bizId, snapshot.sales.filter((s) => isUuid(s.id)));
}

// ----------------------------------------------------------------------------
// Pull (rows changed since the last successful sync), reassembling
// quote_items/invoice_items/sale_items back onto their parent's lineItems —
// the local model embeds line items, it doesn't track them as separate
// arrays the way the server schema (shared with a future Android client)
// does.
// ----------------------------------------------------------------------------

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Row = Record<string, any>;

function compact<T extends object>(obj: T): Partial<T> {
  const out: Partial<T> = {};
  for (const [key, value] of Object.entries(obj)) {
    if (value !== null && value !== undefined) (out as Record<string, unknown>)[key] = value;
  }
  return out;
}

function itemsFor(rows: Row[], parentIdKey: string, parentId: string): LineItem[] {
  return rows
    .filter((r) => r[parentIdKey] === parentId)
    .sort((a, b) => (a.sort_order ?? 0) - (b.sort_order ?? 0))
    .map((r) => ({
      id: r.id,
      description: r.description ?? "",
      qty: Number(r.qty ?? r.quantity ?? 0),
      unitPrice: Number(r.unit_price ?? 0),
    }));
}

async function pullChanges(
  bizId: string,
  mergeRemoteData: (payload: RemoteDataPayload) => void
): Promise<void> {
  const since = getSyncWatermark();
  const requestedAt = new Date().toISOString();

  const [profile, customers, products, quotes, invoices, sales, quoteItems, invoiceItems, saleItems] =
    await Promise.all([
      supabase.from("business_profiles").select("*").eq("business_id", bizId).gt("updated_at", since).maybeSingle(),
      supabase.from("customers").select("*").eq("business_id", bizId).gt("updated_at", since),
      supabase.from("products").select("*").eq("business_id", bizId).gt("updated_at", since),
      supabase.from("quotes").select("*").eq("business_id", bizId).gt("updated_at", since),
      supabase.from("invoices").select("*").eq("business_id", bizId).gt("updated_at", since),
      supabase.from("sales").select("*").eq("business_id", bizId).gt("updated_at", since),
      supabase.from("quote_items").select("*").eq("business_id", bizId).gt("updated_at", since),
      supabase.from("invoice_items").select("*").eq("business_id", bizId).gt("updated_at", since),
      supabase.from("sale_items").select("*").eq("business_id", bizId).gt("updated_at", since),
    ]);

  for (const res of [profile, customers, products, quotes, invoices, sales, quoteItems, invoiceItems, saleItems]) {
    if (res.error) throw res.error;
  }

  const payload: RemoteDataPayload = {};

  if (profile.data) {
    const row = profile.data as Row;
    payload.business = compact({
      name: row.name,
      logoDataUrl: row.logo_data_url,
      regNumber: row.reg_number,
      bankName: row.bank_name,
      accountNumber: row.account_number,
      branchCode: row.branch_code,
      vatRate: row.vat_rate != null ? Number(row.vat_rate) : null,
      paymentTerms: row.payment_terms,
      country: row.country,
      quotePrefix: row.quote_prefix,
      invoicePrefix: row.invoice_prefix,
      receiptPrefix: row.receipt_prefix,
      deletePinHash: row.delete_pin_hash,
    }) as Partial<BusinessProfile> & { updatedAt: string };
    payload.business.updatedAt = row.updated_at;
  }

  if (customers.data) {
    payload.customers = (customers.data as Row[]).map((r) => ({
      id: r.id,
      name: r.name ?? "",
      phone: r.phone ?? "",
      email: r.email ?? "",
      address: r.address ?? "",
      updatedAt: r.updated_at,
      deletedAt: r.deleted_at,
    }));
  }

  if (products.data) {
    payload.products = (products.data as Row[]).map((r) => ({
      id: r.id,
      name: r.name ?? "",
      price: Number(r.price ?? r.unit_price ?? 0),
      stockQty: Number(r.stock_qty ?? r.stock_quantity ?? 0),
      imageDataUrl: r.image_data_url ?? r.image_uri ?? null,
      updatedAt: r.updated_at,
      deletedAt: r.deleted_at,
    }));
  }

  const quoteItemRows = (quoteItems.data as Row[]) ?? [];
  if (quotes.data) {
    payload.quotes = (quotes.data as Row[]).map((r) => ({
      id: r.id,
      number: r.number ?? r.quote_number ?? "",
      customerId: r.customer_id,
      customerName: r.customer_name ?? "",
      description: r.description ?? "",
      lineItems: itemsFor(quoteItemRows, "quote_id", r.id),
      status: (r.status ?? "draft") as Quote["status"],
      createdAt: typeof r.created_at === "string" ? r.created_at.slice(0, 10) : r.created_at,
      convertedToInvoiceId: r.converted_to_invoice_id,
      updatedAt: r.updated_at,
      deletedAt: r.deleted_at,
    }));
  }

  const invoiceItemRows = (invoiceItems.data as Row[]) ?? [];
  if (invoices.data) {
    payload.invoices = (invoices.data as Row[]).map((r) => ({
      id: r.id,
      number: r.number ?? r.invoice_number ?? "",
      customerId: r.customer_id,
      customerName: r.customer_name ?? "",
      lineItems: itemsFor(invoiceItemRows, "invoice_id", r.id),
      status: (r.status ?? "unpaid") as Invoice["status"],
      createdAt: typeof r.created_at === "string" ? r.created_at.slice(0, 10) : r.created_at,
      dueDate: typeof r.due_date === "string" ? r.due_date.slice(0, 10) : r.due_date,
      fromQuoteId: r.from_quote_id ?? r.source_quote_id ?? null,
      updatedAt: r.updated_at,
      deletedAt: r.deleted_at,
    }));
  }

  const saleItemRows = (saleItems.data as Row[]) ?? [];
  if (sales.data) {
    payload.sales = (sales.data as Row[]).map((r) => ({
      id: r.id,
      number: r.number ?? r.sale_number ?? "",
      lineItems: itemsFor(saleItemRows, "sale_id", r.id),
      paymentMethod: (r.payment_method ?? "cash") as Sale["paymentMethod"],
      amountTendered: r.amount_tendered != null ? Number(r.amount_tendered) : null,
      changeGiven: r.change_given != null ? Number(r.change_given) : null,
      createdAt: typeof r.created_at === "string" ? r.created_at.slice(0, 10) : r.created_at,
      updatedAt: r.updated_at,
      deletedAt: r.deleted_at,
    }));
  }

  mergeRemoteData(payload);
  setLastSyncedAt(requestedAt);
}

// ----------------------------------------------------------------------------
// React wiring
// ----------------------------------------------------------------------------

interface SyncEngineParams {
  isLoggedIn: boolean;
  bootstrapBusiness: () => Promise<string | null>;
  refreshStatus: () => Promise<void>;
}

/** Mount once, near the app root, inside both AuthProvider and
 * AppDataProvider (AppShell is the natural spot). Wires up the sync pass
 * to fire on 'online', on the tab becoming visible again, and on demand via
 * `requestSync()` (used by auth.tsx right after a successful login) — plus
 * one initial pass on mount for a device that's already logged in, since
 * neither of those events necessarily fires just from opening the app. */
export function useSyncEngine({ isLoggedIn, bootstrapBusiness, refreshStatus }: SyncEngineParams): void {
  const { data, mergeRemoteData } = useAppData();
  const snapshotRef = useRef<LocalSnapshot>(data);
  useEffect(() => {
    snapshotRef.current = data;
  }, [data]);
  const runningRef = useRef(false);

  const runSyncPass = useCallback(async () => {
    if (!isLoggedIn || runningRef.current) return;
    runningRef.current = true;
    try {
      const bizId = await bootstrapBusiness();
      if (!bizId) return;
      await pushAll(bizId, snapshotRef.current);
      await pullChanges(bizId, mergeRemoteData);
      await refreshStatus();
    } catch {
      // Offline or a transient error — the next trigger (online,
      // visibility, or another manual request) retries from where the
      // last successful pass's watermark left off.
    } finally {
      runningRef.current = false;
    }
  }, [isLoggedIn, bootstrapBusiness, mergeRemoteData, refreshStatus]);

  useEffect(() => {
    activeListener = runSyncPass;
    return () => {
      if (activeListener === runSyncPass) activeListener = null;
    };
  }, [runSyncPass]);

  useEffect(() => {
    if (!isLoggedIn) return;
    runSyncPass();

    function handleOnline() {
      runSyncPass();
    }
    function handleVisibility() {
      if (document.visibilityState === "visible") runSyncPass();
    }

    window.addEventListener("online", handleOnline);
    document.addEventListener("visibilitychange", handleVisibility);
    return () => {
      window.removeEventListener("online", handleOnline);
      document.removeEventListener("visibilitychange", handleVisibility);
    };
  }, [isLoggedIn, runSyncPass]);
}
