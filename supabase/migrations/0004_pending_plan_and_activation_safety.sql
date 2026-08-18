-- Adds visibility into what plan a customer picked (before they've paid),
-- and clears that signal once the admin actually activates them for it —
-- so the dashboard shows real intent and doesn't keep nagging about a
-- request that's already been fulfilled.

alter table public.businesses
  add column pending_plan_tier text
  check (pending_plan_tier in ('monthly', 'quarterly', 'biannual', 'annual'));

-- Called by the client whenever a logged-in user lands on the EFT/WhatsApp
-- pay-confirm screen for a specific tier (from the signup plan-picker, or
-- from Settings -> Subscribe). Scoped to the caller's own business only.
create function public.set_pending_plan(tier text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if tier not in ('monthly', 'quarterly', 'biannual', 'annual') then
    raise exception 'invalid plan tier: %', tier;
  end if;
  update public.businesses
  set pending_plan_tier = tier
  where owner_user_id = auth.uid();
end;
$$;

alter type public.admin_business_row add attribute pending_plan_tier text;

create or replace function public.admin_list_businesses()
returns setof public.admin_business_row
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'not authorized';
  end if;

  return query
    select
      b.id::uuid,
      u.email::text,
      coalesce(bp.business_name, bp.name, '')::text,
      b.status::text,
      b.plan_tier::text,
      b.trial_started_at::timestamptz,
      b.valid_until::timestamptz,
      public.business_is_active(b)::boolean,
      b.created_at::timestamptz,
      b.pending_plan_tier::text
    from public.businesses b
    join auth.users u on u.id = b.owner_user_id
    left join public.business_profiles bp on bp.business_id = b.id
    order by b.created_at desc;
end;
$$;

-- Activating fulfills the request, so clear pending_plan_tier — otherwise
-- the dashboard would keep showing "wants R70" forever after they've
-- already been given it.
create or replace function public.admin_activate_business(
  target_business_id uuid,
  tier text,
  period_days integer
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'not authorized';
  end if;
  if tier not in ('monthly', 'quarterly', 'biannual', 'annual') then
    raise exception 'invalid plan tier: %', tier;
  end if;

  update public.businesses
  set
    status = 'active',
    plan_tier = tier,
    valid_until = greatest(now(), coalesce(valid_until, now())) + make_interval(days => period_days),
    pending_plan_tier = null
  where id = target_business_id;
end;
$$;
