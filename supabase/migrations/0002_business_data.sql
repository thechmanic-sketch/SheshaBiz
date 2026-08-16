-- Cross-device sync + 7-day trial soft-lock.
--
-- One `businesses` row per logged-in owner (auth.users), created lazily and
-- idempotently by `bootstrap_business()` — never by direct client insert.
-- Every other table here hangs off `business_id` and is protected by RLS
-- scoped to `owner_user_id = auth.uid()`. Deletes are always soft (a
-- `deleted_at` timestamp) so sync can propagate them like any other update;
-- there is deliberately no delete RLS policy anywhere, and no hard DELETE
-- is ever issued by the client.
--
-- Column sets for business_profiles and the business-data tables are the
-- union of this app's `web/src/lib/types.ts` fields AND the Android
-- (`claude/quickquote-sa-android-kxdu2i`) Room entity fields, snake_cased.
-- Where both sides use the exact same field name (e.g. `bankName` /
-- `bankName`) it collapses to one column; where the names differ (e.g.
-- web's `price` vs Android's `unitPrice`, web's `qty` vs Android's
-- `quantity`) both columns exist side by side — this schema is written to
-- also be Android's eventual sync target, not just this web app's.

-- ============================================================================
-- businesses
-- ============================================================================

create table public.businesses (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid not null unique references auth.users(id) on delete cascade,
  trial_started_at timestamptz,
  plan_tier text check (plan_tier in ('monthly', 'quarterly', 'biannual', 'annual')),
  valid_until timestamptz,
  status text not null default 'trialing' check (status in ('trialing', 'active', 'lapsed')),
  created_at timestamptz not null default now()
);

alter table public.businesses enable row level security;

-- Billing state only ever changes via bootstrap_business() (SECURITY
-- DEFINER, bypasses RLS) or a human editing the row in the Supabase
-- dashboard — there is no insert/update/delete policy for clients.
create policy "owner can read own business" on public.businesses
  for select
  to authenticated
  using (owner_user_id = auth.uid());

-- ============================================================================
-- business_profiles
-- ============================================================================

create table public.business_profiles (
  business_id uuid primary key references public.businesses(id) on delete cascade,

  -- web BusinessProfile
  name text,
  logo_data_url text,
  reg_number text,
  bank_name text,
  account_number text,
  branch_code text,
  vat_rate numeric,
  payment_terms text,
  country text,
  quote_prefix text,
  invoice_prefix text,
  receipt_prefix text,
  delete_pin_hash text,

  -- Android BusinessProfile (fields not already covered above by an
  -- identically-named web field)
  business_name text,
  owner_name text,
  phone text,
  whatsapp_number text,
  email text,
  address text,
  vat_number text,
  registration_number text,
  logo_uri text,
  account_holder text,
  account_type text,

  updated_at timestamptz not null default now()
);

alter table public.business_profiles enable row level security;

create policy "owner can read own business profile" on public.business_profiles
  for select
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can insert own business profile" on public.business_profiles
  for insert
  to authenticated
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can update own business profile" on public.business_profiles
  for update
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()))
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

-- ============================================================================
-- Functions (defined before any table that references them via trigger)
-- ============================================================================

-- Idempotent bootstrap: re-logging into the same email must resume the same
-- trial, never restart it. If the caller already owns a business, this is a
-- no-op that just returns its id — safe to call on every sync pass.
create function public.bootstrap_business()
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  biz_id uuid;
begin
  select id into biz_id from public.businesses where owner_user_id = auth.uid();
  if biz_id is not null then
    return biz_id;
  end if;

  insert into public.businesses (owner_user_id, trial_started_at, status)
  values (auth.uid(), now(), 'trialing')
  returning id into biz_id;

  insert into public.business_profiles (business_id) values (biz_id);

  return biz_id;
end;
$$;

create function public.business_is_active(biz public.businesses)
returns boolean
language sql
stable
as $$
  select
    (biz.status = 'trialing' and now() < biz.trial_started_at + interval '7 days')
    or
    (biz.status = 'active' and biz.valid_until is not null and now() < biz.valid_until);
$$;

create function public.get_my_business_status()
returns table(status text, trial_started_at timestamptz, valid_until timestamptz, is_active boolean)
language sql
stable
as $$
  select b.status, b.trial_started_at, b.valid_until, public.business_is_active(b)
  from public.businesses b
  where b.owner_user_id = auth.uid();
$$;

-- Attached (below) as a BEFORE INSERT OR UPDATE trigger on every
-- business-data table except business_profiles, which stays always-editable
-- regardless of trial/subscription state.
create function public.enforce_business_active()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  biz public.businesses;
begin
  select * into biz from public.businesses where id = new.business_id;
  if biz.id is null then
    raise exception 'unknown business_id';
  end if;
  if not public.business_is_active(biz) then
    raise exception 'trial or subscription has lapsed';
  end if;
  return new;
end;
$$;

-- ============================================================================
-- customers
-- ============================================================================

create table public.customers (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,

  -- web Customer + Android Customer (name/phone/email/address are shared
  -- exactly; created_at is Android-only, updated_at doubles as its sync clock)
  name text,
  phone text,
  email text,
  address text,
  created_at timestamptz not null default now(),

  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create index customers_business_id_idx on public.customers (business_id) where deleted_at is null;

alter table public.customers enable row level security;

create policy "owner can read own customers" on public.customers
  for select
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can insert own customers" on public.customers
  for insert
  to authenticated
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can update own customers" on public.customers
  for update
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()))
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create trigger enforce_business_active_customers
  before insert or update on public.customers
  for each row execute function public.enforce_business_active();

-- ============================================================================
-- products
-- ============================================================================

create table public.products (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,

  -- shared
  name text,

  -- web Product
  price numeric,
  stock_qty numeric,
  image_data_url text,

  -- Android Product
  unit_price numeric,
  sku text,
  track_stock boolean,
  stock_quantity numeric,
  low_stock_threshold numeric,
  image_uri text,
  created_at timestamptz not null default now(),

  updated_at timestamptz not null default now(),
  deleted_at timestamptz,

  unique (business_id, sku)
);

create index products_business_id_idx on public.products (business_id) where deleted_at is null;

alter table public.products enable row level security;

create policy "owner can read own products" on public.products
  for select
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can insert own products" on public.products
  for insert
  to authenticated
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can update own products" on public.products
  for update
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()))
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create trigger enforce_business_active_products
  before insert or update on public.products
  for each row execute function public.enforce_business_active();

-- ============================================================================
-- quotes
-- (converted_to_invoice_id's FK to invoices is added after the invoices
-- table exists, further below)
-- ============================================================================

create table public.quotes (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,

  customer_id uuid references public.customers(id) on delete set null,

  -- shared
  customer_name text,
  status text check (status in ('draft', 'sent', 'accepted', 'rejected')),
  created_at timestamptz not null default now(),

  -- web Quote
  number text,
  description text,
  converted_to_invoice_id uuid,

  -- Android Quote
  quote_number text,
  customer_phone text,
  customer_email text,
  customer_address text,
  quote_date timestamptz,
  valid_until timestamptz,
  vat_enabled boolean,
  vat_rate numeric,
  discount_type text check (discount_type in ('percent', 'fixed')),
  discount_value numeric,
  subtotal numeric,
  discount_amount numeric,
  vat_amount numeric,
  total numeric,
  notes text,
  payment_terms text,

  updated_at timestamptz not null default now(),
  deleted_at timestamptz,

  unique (business_id, number),
  unique (business_id, quote_number)
);

create index quotes_business_id_idx on public.quotes (business_id) where deleted_at is null;

alter table public.quotes enable row level security;

create policy "owner can read own quotes" on public.quotes
  for select
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can insert own quotes" on public.quotes
  for insert
  to authenticated
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can update own quotes" on public.quotes
  for update
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()))
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create trigger enforce_business_active_quotes
  before insert or update on public.quotes
  for each row execute function public.enforce_business_active();

-- ============================================================================
-- invoices
-- ============================================================================

create table public.invoices (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,

  customer_id uuid references public.customers(id) on delete set null,
  from_quote_id uuid references public.quotes(id) on delete set null,
  source_quote_id uuid references public.quotes(id) on delete set null,

  -- shared
  customer_name text,
  status text check (status in ('unpaid', 'paid', 'overdue', 'cancelled')),
  created_at timestamptz not null default now(),
  due_date timestamptz,

  -- web Invoice
  number text,

  -- Android Invoice
  invoice_number text,
  customer_phone text,
  customer_email text,
  customer_address text,
  invoice_date timestamptz,
  vat_enabled boolean,
  vat_rate numeric,
  discount_type text check (discount_type in ('percent', 'fixed')),
  discount_value numeric,
  subtotal numeric,
  discount_amount numeric,
  vat_amount numeric,
  total numeric,
  notes text,
  payment_terms text,
  paid_at timestamptz,

  updated_at timestamptz not null default now(),
  deleted_at timestamptz,

  unique (business_id, number),
  unique (business_id, invoice_number)
);

create index invoices_business_id_idx on public.invoices (business_id) where deleted_at is null;

alter table public.invoices enable row level security;

create policy "owner can read own invoices" on public.invoices
  for select
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can insert own invoices" on public.invoices
  for insert
  to authenticated
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can update own invoices" on public.invoices
  for update
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()))
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create trigger enforce_business_active_invoices
  before insert or update on public.invoices
  for each row execute function public.enforce_business_active();

-- Now that invoices exists, wire up the forward reference from quotes.
alter table public.quotes
  add constraint quotes_converted_to_invoice_id_fkey
  foreign key (converted_to_invoice_id) references public.invoices(id) on delete set null;

-- ============================================================================
-- quote_items / invoice_items
-- (web LineItem: id, description, qty, unitPrice — shared shape reused for
-- both quotes and invoices. Android splits this into QuoteItem/InvoiceItem,
-- each additionally carrying lineTotal and sortOrder.)
-- ============================================================================

create table public.quote_items (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,
  quote_id uuid not null references public.quotes(id) on delete cascade,

  -- shared
  description text,
  unit_price numeric,

  -- web LineItem
  qty numeric,

  -- Android QuoteItem
  quantity numeric,
  line_total numeric,
  sort_order integer,

  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create index quote_items_business_id_idx on public.quote_items (business_id) where deleted_at is null;

alter table public.quote_items enable row level security;

create policy "owner can read own quote items" on public.quote_items
  for select
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can insert own quote items" on public.quote_items
  for insert
  to authenticated
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can update own quote items" on public.quote_items
  for update
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()))
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create trigger enforce_business_active_quote_items
  before insert or update on public.quote_items
  for each row execute function public.enforce_business_active();

create table public.invoice_items (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,
  invoice_id uuid not null references public.invoices(id) on delete cascade,

  -- shared
  description text,
  unit_price numeric,

  -- web LineItem
  qty numeric,

  -- Android InvoiceItem
  quantity numeric,
  line_total numeric,
  sort_order integer,

  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create index invoice_items_business_id_idx on public.invoice_items (business_id) where deleted_at is null;

alter table public.invoice_items enable row level security;

create policy "owner can read own invoice items" on public.invoice_items
  for select
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can insert own invoice items" on public.invoice_items
  for insert
  to authenticated
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can update own invoice items" on public.invoice_items
  for update
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()))
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create trigger enforce_business_active_invoice_items
  before insert or update on public.invoice_items
  for each row execute function public.enforce_business_active();

-- ============================================================================
-- sales
-- ============================================================================

create table public.sales (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,

  customer_id uuid references public.customers(id) on delete set null,

  -- shared
  payment_method text check (payment_method in ('cash', 'card', 'eft', 'other')),
  amount_tendered numeric,
  change_given numeric,
  created_at timestamptz not null default now(),

  -- web Sale
  number text,

  -- Android Sale
  sale_number text,
  customer_name text,
  sale_date timestamptz,
  vat_enabled boolean,
  vat_rate numeric,
  discount_type text check (discount_type in ('percent', 'fixed')),
  discount_value numeric,
  subtotal numeric,
  discount_amount numeric,
  vat_amount numeric,
  total numeric,
  notes text,

  updated_at timestamptz not null default now(),
  deleted_at timestamptz,

  unique (business_id, number),
  unique (business_id, sale_number)
);

create index sales_business_id_idx on public.sales (business_id) where deleted_at is null;

alter table public.sales enable row level security;

create policy "owner can read own sales" on public.sales
  for select
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can insert own sales" on public.sales
  for insert
  to authenticated
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can update own sales" on public.sales
  for update
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()))
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create trigger enforce_business_active_sales
  before insert or update on public.sales
  for each row execute function public.enforce_business_active();

-- ============================================================================
-- sale_items
-- ============================================================================

create table public.sale_items (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,
  sale_id uuid not null references public.sales(id) on delete cascade,
  product_id uuid references public.products(id) on delete set null,

  -- shared
  description text,
  unit_price numeric,

  -- web LineItem
  qty numeric,

  -- Android SaleItem
  quantity numeric,
  line_total numeric,
  sort_order integer,

  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create index sale_items_business_id_idx on public.sale_items (business_id) where deleted_at is null;

alter table public.sale_items enable row level security;

create policy "owner can read own sale items" on public.sale_items
  for select
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can insert own sale items" on public.sale_items
  for insert
  to authenticated
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create policy "owner can update own sale items" on public.sale_items
  for update
  to authenticated
  using (business_id in (select id from public.businesses where owner_user_id = auth.uid()))
  with check (business_id in (select id from public.businesses where owner_user_id = auth.uid()));

create trigger enforce_business_active_sale_items
  before insert or update on public.sale_items
  for each row execute function public.enforce_business_active();
