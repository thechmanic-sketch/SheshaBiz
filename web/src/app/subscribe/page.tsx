"use client";

import { useAuth } from "@/lib/auth";
import { SubscribeLoggedOut, SubscribePanel } from "@/components/subscription/SubscribePanel";

export default function SubscribePage() {
  const { isLoggedIn, loaded } = useAuth();

  if (!loaded) {
    return <div className="min-h-[40vh]" />;
  }

  if (!isLoggedIn) {
    return <SubscribeLoggedOut />;
  }

  return <SubscribePanel />;
}
