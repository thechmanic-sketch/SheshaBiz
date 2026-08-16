-- Minimal trial-signup capture. There is no login/auth built on top of this
-- yet — this table just records who signed up and when their trial started,
-- for the business owner's own tracking and anti-abuse review.

create table if not exists public.signups (
  id uuid primary key default gen_random_uuid(),
  business_name text not null,
  contact text not null,
  contact_type text not null check (contact_type in ('email', 'phone')),
  country text,
  platform text not null check (platform in ('web', 'android')),
  ip_address inet,
  trial_started_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

alter table public.signups enable row level security;

-- Anyone using the anon key can record a signup, but nobody can read them
-- back through the API — only the project owner via the Supabase dashboard.
create policy "anon can insert signups" on public.signups
  for insert
  to anon
  with check (true);
