-- Admin dashboard support: lets exactly one designated account (identified by
-- email, not by any client-settable flag) see every business's trial/plan
-- status and activate a paid subscription, without opening up the RLS model
-- that keeps every other account locked to its own data.
--
-- Deliberately implemented as SECURITY DEFINER functions with their own
-- internal admin check, not as broad "admin can select/update all rows" RLS
-- policies — this keeps the privileged path narrow, auditable in one place,
-- and consistent with how bootstrap_business()/enforce_business_active()
-- already bypass RLS for a specific, well-scoped reason.
--
-- Change the email below if the admin account ever changes; nothing else
-- about this design assumes a particular email address.

create function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(
    (select email from auth.users where id = auth.uid()) = 'thechmanic@gmail.com',
    false
  );
$$;

create type public.admin_business_row as (
  business_id uuid,
  owner_email text,
  business_name text,
  status text,
  plan_tier text,
  trial_started_at timestamptz,
  valid_until timestamptz,
  is_active boolean,
  created_at timestamptz
);

create function public.admin_list_businesses()
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
      b.id,
      u.email,
      coalesce(bp.business_name, bp.name, ''),
      b.status,
      b.plan_tier,
      b.trial_started_at,
      b.valid_until,
      public.business_is_active(b),
      b.created_at
    from public.businesses b
    join auth.users u on u.id = b.owner_user_id
    left join public.business_profiles bp on bp.business_id = b.id
    order by b.created_at desc;
end;
$$;

-- period_days is passed in (rather than switching on plan_tier here) so the
-- exact tier-to-duration mapping lives in one place on the client (matching
-- the pricing poster) instead of being duplicated into this function too.
create function public.admin_activate_business(
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
    valid_until = greatest(now(), coalesce(valid_until, now())) + make_interval(days => period_days)
  where id = target_business_id;
end;
$$;
