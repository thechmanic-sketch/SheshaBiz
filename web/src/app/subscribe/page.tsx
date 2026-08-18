"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { SubscribeLoggedOut, SubscribePanel } from "@/components/subscription/SubscribePanel";
import { PLAN_TIERS, type PlanTierId } from "@/lib/plans";

function isPlanTierId(value: string | null): value is PlanTierId {
  return !!value && PLAN_TIERS.some((tier) => tier.id === value);
}

function SubscribePageInner() {
  const { isLoggedIn, loaded } = useAuth();
  const params = useSearchParams();
  const planParam = params.get("plan");
  const initialPlan = isPlanTierId(planParam) ? planParam : null;

  if (!loaded) {
    return <div className="min-h-[40vh]" />;
  }

  if (!isLoggedIn) {
    return <SubscribeLoggedOut />;
  }

  return <SubscribePanel initialPlan={initialPlan} />;
}

export default function SubscribePage() {
  return (
    <Suspense fallback={<div className="min-h-[40vh]" />}>
      <SubscribePageInner />
    </Suspense>
  );
}
