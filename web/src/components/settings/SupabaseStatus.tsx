"use client";

import { useEffect, useState } from "react";
import { CheckCircle2, XCircle, Loader2 } from "lucide-react";
import { supabase } from "@/lib/supabase";

type Status = "checking" | "connected" | "unreachable";

export function SupabaseStatus() {
  const [status, setStatus] = useState<Status>("checking");

  useEffect(() => {
    let cancelled = false;

    async function check() {
      // Querying a table that doesn't exist yet is deliberate: no schema
      // has been created (that's a separate step), so the only thing this
      // checks is whether the client can reach the project at all. Any
      // structured response from PostgREST — even a "table not found"
      // error — proves that; only a network-level failure (bad URL/key,
      // no connectivity) means unreachable.
      let reachedApi: boolean;
      try {
        const { error } = await supabase.from("_connectivity_check").select("*").limit(1);
        reachedApi = !error || typeof error.code === "string";
      } catch {
        reachedApi = false;
      }
      if (!cancelled) setStatus(reachedApi ? "connected" : "unreachable");
    }

    check();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="flex items-center gap-2 text-sm">
      {status === "checking" && (
        <>
          <Loader2 size={15} className="animate-spin text-ink-faint" />
          <span className="text-ink-faint">Checking connection…</span>
        </>
      )}
      {status === "connected" && (
        <>
          <CheckCircle2 size={15} className="text-brand-deep" />
          <span className="text-brand-deep font-medium">Connected to Supabase</span>
        </>
      )}
      {status === "unreachable" && (
        <>
          <XCircle size={15} className="text-error" />
          <span className="text-error font-medium">Couldn&apos;t reach Supabase</span>
        </>
      )}
    </div>
  );
}
