"use client";

import { useState } from "react";
import Image from "next/image";
import { ArrowRight, Package } from "lucide-react";
import { fileToCompressedDataUrl } from "@/lib/files";
import { useAppData } from "@/lib/store";
import { COUNTRIES, type Country } from "@/lib/types";

const basePath = process.env.NEXT_PUBLIC_BASE_PATH ?? "";

const inputCls =
  "w-full rounded-xl border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-brand";
const labelCls = "text-xs font-semibold text-ink-faint";

export function OnboardingWizard() {
  const { completeOnboarding } = useAppData();
  const [name, setName] = useState("");
  const [country, setCountry] = useState<Country>("south_africa");
  const [vatRate, setVatRate] = useState("15");
  const [logoDataUrl, setLogoDataUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      setError("Add your business name to continue.");
      return;
    }
    completeOnboarding({
      name: name.trim(),
      country,
      vatRate: Number(vatRate) || 0,
      logoDataUrl,
    });
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4 py-10">
      <div className="w-full max-w-md">
        <div className="flex items-center gap-2.5 mb-8 justify-center">
          <Image src={`${basePath}/logo-mark.png`} alt="" width={32} height={32} />
          <span className="font-bold text-xl">
            Shesha<span className="text-brand-deep">Biz</span>
          </span>
        </div>

        <div className="rounded-2xl border border-line bg-surface p-6">
          <h1 className="text-lg font-bold">Welcome — let&apos;s set up your business</h1>
          <p className="mt-1 text-sm text-ink-faint">
            This takes a minute. You can change any of this later in Settings.
          </p>

          <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
            <div className="flex items-center gap-3">
              {logoDataUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={logoDataUrl} alt="" className="h-14 w-14 rounded-full object-cover" />
              ) : (
                <span className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-tint text-brand-deep">
                  <Package size={22} />
                </span>
              )}
              <label className="cursor-pointer rounded-lg border border-line px-3 py-1.5 text-xs font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]">
                Upload logo (optional)
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={async (e) => {
                    const file = e.target.files?.[0];
                    if (file) setLogoDataUrl(await fileToCompressedDataUrl(file, 400));
                  }}
                />
              </label>
            </div>

            <div>
              <label className={labelCls}>Business name</label>
              <input
                className={`${inputCls} mt-1`}
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Sipho's Plumbing"
                autoFocus
              />
            </div>

            <div>
              <label className={labelCls}>Country</label>
              <select
                className={`${inputCls} mt-1`}
                value={country}
                onChange={(e) => setCountry(e.target.value as Country)}
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
                value={vatRate}
                onChange={(e) => setVatRate(e.target.value)}
              />
            </div>

            {error && <p className="text-sm font-medium text-error">{error}</p>}

            <button
              type="submit"
              className="mt-2 flex items-center justify-center gap-2 rounded-xl bg-brand py-3 text-sm font-semibold text-white"
            >
              Get started
              <ArrowRight size={16} />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
