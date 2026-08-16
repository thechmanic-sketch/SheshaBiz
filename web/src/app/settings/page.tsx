"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import { Download, Upload, RotateCcw, ShieldCheck, ShieldOff, LogIn, LogOut } from "lucide-react";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { DeleteConfirmDialog } from "@/components/ui/DeleteConfirmDialog";
import { PinSetupModal } from "@/components/settings/PinSetupModal";
import { SupabaseStatus } from "@/components/settings/SupabaseStatus";
import { fileToCompressedDataUrl } from "@/lib/files";
import { useAppData } from "@/lib/store";
import { useAuth } from "@/lib/auth";
import { COUNTRIES, type Country } from "@/lib/types";

const inputCls =
  "w-full rounded-xl border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-brand";
const labelCls = "text-xs font-semibold text-ink-faint";

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-2xl border border-line bg-surface p-5">
      <h2 className="text-xs font-bold uppercase tracking-wide text-ink-faint">{title}</h2>
      <div className="mt-3 flex flex-col gap-3">{children}</div>
    </div>
  );
}

export default function SettingsPage() {
  const { data, updateBusiness, exportJson, importJson, resetDemoData } = useAppData();
  const { isLoggedIn, email, signOut } = useAuth();
  const { business } = data;
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [importMessage, setImportMessage] = useState<string | null>(null);
  const [confirmReset, setConfirmReset] = useState(false);
  const [pinSetupOpen, setPinSetupOpen] = useState(false);
  const [confirmRemovePin, setConfirmRemovePin] = useState(false);

  function handleExport() {
    const blob = new Blob([exportJson()], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `sheshabiz-backup-${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  async function handleImportFile(file: File) {
    const text = await file.text();
    const ok = importJson(text);
    setImportMessage(ok ? "Data imported successfully." : "That file doesn't look like a valid SheshaBiz backup.");
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="text-xl font-bold">Settings</h1>

      <div className="mt-5 flex flex-col gap-4">
        <Card title="Business profile">
          <div className="flex items-center gap-3">
            {business.logoDataUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={business.logoDataUrl} alt="" className="h-14 w-14 rounded-full object-cover" />
            ) : (
              <span className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-tint text-lg font-bold text-brand-deep">
                {business.name.slice(0, 2).toUpperCase()}
              </span>
            )}
            <label className="cursor-pointer rounded-lg border border-line px-3 py-1.5 text-xs font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]">
              Upload logo
              <input
                type="file"
                accept="image/*"
                className="hidden"
                onChange={async (e) => {
                  const file = e.target.files?.[0];
                  if (file) updateBusiness({ logoDataUrl: await fileToCompressedDataUrl(file, 400) });
                }}
              />
            </label>
          </div>
          <div>
            <label className={labelCls}>Business name</label>
            <input
              className={`${inputCls} mt-1`}
              value={business.name}
              onChange={(e) => updateBusiness({ name: e.target.value })}
            />
          </div>
          <div>
            <label className={labelCls}>Registration number</label>
            <input
              className={`${inputCls} mt-1`}
              value={business.regNumber}
              onChange={(e) => updateBusiness({ regNumber: e.target.value })}
            />
          </div>
        </Card>

        <Card title="Banking details">
          <div>
            <label className={labelCls}>Bank name</label>
            <input
              className={`${inputCls} mt-1`}
              value={business.bankName}
              onChange={(e) => updateBusiness({ bankName: e.target.value })}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={labelCls}>Account number</label>
              <input
                className={`${inputCls} mt-1`}
                value={business.accountNumber}
                onChange={(e) => updateBusiness({ accountNumber: e.target.value })}
              />
            </div>
            <div>
              <label className={labelCls}>Branch code</label>
              <input
                className={`${inputCls} mt-1`}
                value={business.branchCode}
                onChange={(e) => updateBusiness({ branchCode: e.target.value })}
              />
            </div>
          </div>
        </Card>

        <Card title="Currency &amp; country">
          <div>
            <label className={labelCls}>Country</label>
            <select
              className={`${inputCls} mt-1`}
              value={business.country}
              onChange={(e) => updateBusiness({ country: e.target.value as Country })}
            >
              {Object.values(COUNTRIES).map((c) => (
                <option key={c.code} value={c.code}>
                  {c.displayName} ({c.currencyCode} {c.currencySymbol})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className={labelCls}>VAT rate (%)</label>
            <input
              type="number"
              min={0}
              max={100}
              step="0.1"
              className={`${inputCls} mt-1 tabular`}
              value={business.vatRate}
              onChange={(e) => updateBusiness({ vatRate: Number(e.target.value) || 0 })}
            />
          </div>
        </Card>

        <Card title="Document numbering">
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className={labelCls}>Quote prefix</label>
              <input
                className={`${inputCls} mt-1`}
                value={business.quotePrefix}
                onChange={(e) => updateBusiness({ quotePrefix: e.target.value })}
              />
            </div>
            <div>
              <label className={labelCls}>Invoice prefix</label>
              <input
                className={`${inputCls} mt-1`}
                value={business.invoicePrefix}
                onChange={(e) => updateBusiness({ invoicePrefix: e.target.value })}
              />
            </div>
            <div>
              <label className={labelCls}>Receipt prefix</label>
              <input
                className={`${inputCls} mt-1`}
                value={business.receiptPrefix}
                onChange={(e) => updateBusiness({ receiptPrefix: e.target.value })}
              />
            </div>
          </div>
        </Card>

        <Card title="Security">
          <p className="text-sm text-ink-soft">
            {business.deletePinHash
              ? "A PIN is required before deleting a quote, invoice, product, or customer."
              : "Optionally require a PIN before deleting anything, as a safeguard against accidental taps."}
          </p>
          <div className="flex flex-wrap gap-2">
            {business.deletePinHash ? (
              <>
                <button
                  type="button"
                  onClick={() => setPinSetupOpen(true)}
                  className="flex items-center gap-1.5 rounded-xl border border-line px-3.5 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
                >
                  <ShieldCheck size={15} />
                  Change PIN
                </button>
                <button
                  type="button"
                  onClick={() => setConfirmRemovePin(true)}
                  className="flex items-center gap-1.5 rounded-xl px-3.5 py-2 text-sm font-semibold text-error hover:bg-error/10"
                >
                  <ShieldOff size={15} />
                  Remove PIN
                </button>
              </>
            ) : (
              <button
                type="button"
                onClick={() => setPinSetupOpen(true)}
                className="flex items-center gap-1.5 rounded-xl border border-line px-3.5 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
              >
                <ShieldCheck size={15} />
                Set PIN
              </button>
            )}
          </div>
        </Card>

        <Card title="Payment terms">
          <textarea
            className={`${inputCls} min-h-20`}
            value={business.paymentTerms}
            onChange={(e) => updateBusiness({ paymentTerms: e.target.value })}
          />
          <p className="text-xs text-ink-faint">Shown on the footer of quotes and invoices.</p>
        </Card>

        <Card title="Account">
          {isLoggedIn ? (
            <>
              <p className="text-sm text-ink-soft">
                Logged in as <span className="font-semibold text-ink">{email}</span>.
              </p>
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => signOut()}
                  className="flex items-center gap-1.5 rounded-xl border border-line px-3.5 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
                >
                  <LogOut size={15} />
                  Log out
                </button>
              </div>
            </>
          ) : (
            <>
              <p className="text-sm text-ink-soft">
                Create an account to secure future access to SheshaBiz — for example, if you get a
                new phone. This doesn&apos;t sync or back up your data yet; it only lets you prove
                it&apos;s you later.
              </p>
              <div className="flex flex-wrap gap-2">
                <Link
                  href="/auth"
                  className="flex items-center gap-1.5 rounded-xl border border-line px-3.5 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
                >
                  <LogIn size={15} />
                  Create account / Log in
                </Link>
              </div>
            </>
          )}
        </Card>

        <Card title="Cloud sync">
          <SupabaseStatus />
          <p className="text-sm text-ink-soft">
            The app is now connected to Supabase, but it isn&apos;t used for storing anything yet —
            this only confirms the connection itself works. Your data still lives in this browser
            only until real cloud sync is built on top of it.
          </p>
        </Card>

        <Card title="Data">
          <p className="text-sm text-ink-soft">
            Your data lives in this browser only, for now — export a backup to keep it safe, or move it to
            another device. Cloud sync is coming soon.
          </p>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={handleExport}
              className="flex items-center gap-1.5 rounded-xl border border-line px-3.5 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
            >
              <Download size={15} />
              Export backup
            </button>
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="flex items-center gap-1.5 rounded-xl border border-line px-3.5 py-2 text-sm font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
            >
              <Upload size={15} />
              Import backup
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept="application/json"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) handleImportFile(file);
                e.target.value = "";
              }}
            />
            <button
              type="button"
              onClick={() => setConfirmReset(true)}
              className="flex items-center gap-1.5 rounded-xl px-3.5 py-2 text-sm font-semibold text-error hover:bg-error/10"
            >
              <RotateCcw size={15} />
              Reset demo data
            </button>
          </div>
          {importMessage && <p className="text-sm font-medium text-brand-deep">{importMessage}</p>}
        </Card>
      </div>

      <ConfirmDialog
        open={confirmReset}
        title="Reset all data?"
        message="This replaces everything in this browser with the original SheshaBiz demo data. This cannot be undone."
        confirmLabel="Reset"
        danger
        onCancel={() => setConfirmReset(false)}
        onConfirm={() => {
          resetDemoData();
          setConfirmReset(false);
        }}
      />

      <PinSetupModal
        open={pinSetupOpen}
        onClose={() => setPinSetupOpen(false)}
        onSet={(hash) => {
          updateBusiness({ deletePinHash: hash });
          setPinSetupOpen(false);
        }}
      />

      <DeleteConfirmDialog
        open={confirmRemovePin}
        title="Remove PIN protection?"
        message="Deletions will no longer require a PIN."
        onCancel={() => setConfirmRemovePin(false)}
        onConfirm={() => {
          updateBusiness({ deletePinHash: null });
          setConfirmRemovePin(false);
        }}
      />
    </div>
  );
}
