"use client";

import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, Loader2, ShieldAlert, XCircle } from "lucide-react";
import { useAuth } from "@/lib/auth";
import { supabase } from "@/lib/supabase";
import { SearchInput } from "@/components/ui/SearchInput";
import { Modal } from "@/components/ui/Modal";
import { PLAN_TIERS, planById, type PlanTierId } from "@/lib/plans";

// Kept in sync with the email hard-coded into `is_admin()` in
// supabase/migrations/0003_admin_dashboard.sql. This client-side check is
// only a first-pass UX gate (skip the RPC round-trip and the "flash of
// someone else's data" for the 99.9% of users who aren't the admin) — the
// real enforcement lives server-side in `is_admin()`, which every RPC below
// re-checks on its own regardless of what this page believes.
const ADMIN_EMAIL = "thechmanic@gmail.com";

type DisplayStatus = "trialing" | "active" | "lapsed";

interface AdminBusinessRow {
  business_id: string;
  owner_email: string;
  business_name: string;
  status: string;
  plan_tier: string | null;
  trial_started_at: string | null;
  valid_until: string | null;
  is_active: boolean;
  created_at: string;
}

const statusPillCls: Record<DisplayStatus, string> = {
  trialing: "bg-blue-500/10 text-blue-600 dark:text-blue-400",
  active: "bg-brand-tint text-brand-deep",
  lapsed: "bg-error/10 text-error",
};

const statusLabel: Record<DisplayStatus, string> = {
  trialing: "Trialing",
  active: "Active",
  lapsed: "Lapsed",
};

function displayStatus(row: AdminBusinessRow): DisplayStatus {
  if (!row.is_active) return "lapsed";
  return row.status === "active" ? "active" : "trialing";
}

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

function StatusPill({ row }: { row: AdminBusinessRow }) {
  const status = displayStatus(row);
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${statusPillCls[status]}`}>
      {statusLabel[status]}
    </span>
  );
}

function ActivateModal({
  row,
  onClose,
  onActivated,
}: {
  row: AdminBusinessRow;
  onClose: () => void;
  onActivated: (businessId: string) => void;
}) {
  const [tier, setTier] = useState<PlanTierId>("monthly");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleConfirm() {
    setBusy(true);
    setError(null);
    try {
      const { error: rpcError } = await supabase.rpc("admin_activate_business", {
        target_business_id: row.business_id,
        tier,
        period_days: planById(tier).periodDays,
      });
      if (rpcError) {
        setError(rpcError.message || "Couldn't activate this business. Please try again.");
        return;
      }
      onActivated(row.business_id);
      onClose();
    } catch {
      setError("Couldn't reach the server. Check your connection and try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal open title={`Activate ${row.business_name || row.owner_email}`} onClose={onClose}>
      <div className="flex flex-col gap-3">
        <p className="text-sm text-ink-soft">Pick the plan this payment covers.</p>
        <div className="flex flex-col gap-2">
          {PLAN_TIERS.map((plan) => (
            <button
              key={plan.id}
              type="button"
              onClick={() => setTier(plan.id)}
              className={`flex items-center justify-between rounded-xl border px-3.5 py-2.5 text-left text-sm ${
                tier === plan.id ? "border-brand bg-brand-tint" : "border-line hover:bg-black/[.04] dark:hover:bg-white/[.06]"
              }`}
            >
              <span className="font-semibold">{plan.label}</span>
              <span className="text-ink-soft">{plan.priceLabel}</span>
            </button>
          ))}
        </div>
        {error && <p className="text-sm font-medium text-error">{error}</p>}
        <div className="mt-2 flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            className="rounded-xl px-4 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06] disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={busy}
            className="rounded-xl bg-brand px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
          >
            {busy ? "Activating…" : "Activate"}
          </button>
        </div>
      </div>
    </Modal>
  );
}

function AdminDashboard() {
  const [rows, setRows] = useState<AdminBusinessRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [activatingRow, setActivatingRow] = useState<AdminBusinessRow | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // No synchronous setState before the first `await` here — the initial
  // `loading`/`fetchError` state values already cover the mount call, so
  // this stays safe to invoke directly from the effect below (mirrors the
  // `loadSession` pattern in lib/auth.tsx).
  async function fetchBusinesses() {
    try {
      const { data, error } = await supabase.rpc("admin_list_businesses");
      if (error) {
        setFetchError(error.message || "Couldn't load businesses.");
        return;
      }
      setFetchError(null);
      // RPC already orders by created_at desc — preserve that order as-is.
      setRows((data as AdminBusinessRow[] | null) ?? []);
    } catch {
      setFetchError("Couldn't reach the server. Check your connection and try again.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    // Loading the admin list is an intentional one-shot data fetch on
    // mount, same shape as the sync engine's initial pass — see
    // `useSyncEngine` in lib/sync.ts.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchBusinesses();
  }, []);

  const filteredRows = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter(
      (row) =>
        row.business_name.toLowerCase().includes(q) || row.owner_email.toLowerCase().includes(q)
    );
  }, [rows, query]);

  return (
    <div className="mx-auto max-w-5xl">
      <h1 className="text-xl font-bold">Control dashboard</h1>
      <p className="mt-1 text-sm text-ink-soft">All businesses, their trial/subscription status, and activation.</p>

      <div className="mt-5 flex flex-wrap items-center gap-3">
        <SearchInput value={query} onChange={setQuery} placeholder="Search by business name or email…" />
        <span className="text-xs text-ink-faint">
          {filteredRows.length} of {rows.length} business{rows.length === 1 ? "" : "es"}
        </span>
      </div>

      {successMessage && (
        <div className="mt-3 flex items-center gap-2 rounded-xl bg-brand-tint px-3.5 py-2.5 text-sm font-medium text-brand-deep">
          <CheckCircle2 size={16} className="shrink-0" />
          {successMessage}
        </div>
      )}

      {fetchError && (
        <div className="mt-3 flex items-center gap-2 rounded-xl bg-error/10 px-3.5 py-2.5 text-sm font-medium text-error">
          <XCircle size={16} className="shrink-0" />
          {fetchError}
        </div>
      )}

      <div className="mt-4 overflow-x-auto rounded-2xl border border-line bg-surface">
        <table className="w-full min-w-[860px] text-left text-sm">
          <thead>
            <tr className="border-b border-line text-xs font-semibold uppercase tracking-wide text-ink-faint">
              <th className="px-4 py-3">Business</th>
              <th className="px-4 py-3">Owner email</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Plan</th>
              <th className="px-4 py-3">Trial started</th>
              <th className="px-4 py-3">Valid until</th>
              <th className="px-4 py-3">Active</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-ink-faint">
                  <Loader2 size={18} className="mx-auto animate-spin" />
                </td>
              </tr>
            )}
            {!loading && filteredRows.length === 0 && !fetchError && (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-ink-faint">
                  {rows.length === 0 ? "No businesses yet." : "No businesses match your search."}
                </td>
              </tr>
            )}
            {!loading &&
              filteredRows.map((row) => (
                <tr key={row.business_id} className="border-b border-line last:border-0">
                  <td className="px-4 py-3 font-semibold">{row.business_name || "—"}</td>
                  <td className="px-4 py-3 text-ink-soft">{row.owner_email}</td>
                  <td className="px-4 py-3">
                    <StatusPill row={row} />
                  </td>
                  <td className="px-4 py-3 text-ink-soft">{row.plan_tier ?? "—"}</td>
                  <td className="px-4 py-3 text-ink-soft tabular">{formatDate(row.trial_started_at)}</td>
                  <td className="px-4 py-3 text-ink-soft tabular">{formatDate(row.valid_until)}</td>
                  <td className="px-4 py-3">{row.is_active ? "Yes" : "No"}</td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={() => {
                        setSuccessMessage(null);
                        setActivatingRow(row);
                      }}
                      className="rounded-lg border border-line px-3 py-1.5 text-xs font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
                    >
                      Activate
                    </button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {activatingRow && (
        <ActivateModal
          row={activatingRow}
          onClose={() => setActivatingRow(null)}
          onActivated={async () => {
            setSuccessMessage(`Activated ${activatingRow.business_name || activatingRow.owner_email}.`);
            await fetchBusinesses();
          }}
        />
      )}
    </div>
  );
}

export default function AdminPage() {
  const { email, loaded } = useAuth();

  if (!loaded) {
    return <div className="min-h-[40vh]" />;
  }

  if (email !== ADMIN_EMAIL) {
    return (
      <div className="mx-auto max-w-sm py-16 text-center">
        <ShieldAlert size={28} className="mx-auto text-ink-faint" />
        <h1 className="mt-3 text-lg font-bold">Not authorized</h1>
        <p className="mt-1 text-sm text-ink-soft">This page is only available to the SheshaBiz admin account.</p>
      </div>
    );
  }

  return <AdminDashboard />;
}
