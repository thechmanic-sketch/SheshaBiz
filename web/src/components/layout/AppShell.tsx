"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import {
  LayoutDashboard,
  ReceiptText,
  FileText,
  ShoppingCart,
  Package,
  Users,
  BarChart3,
  Settings,
  Search,
  Bell,
  Menu,
  X,
  AlertTriangle,
  Clock,
} from "lucide-react";
import { useAppData } from "@/lib/store";
import { useAuth } from "@/lib/auth";
import { useSyncEngine } from "@/lib/sync";
import { TRIAL_LOCK_MESSAGE } from "@/lib/trial";
import { OnboardingWizard } from "@/components/onboarding/OnboardingWizard";
import type { ComponentType } from "react";

const MS_PER_DAY = 24 * 60 * 60 * 1000;
const TRIAL_DAYS = 7;
const SOON_THRESHOLD_DAYS = 2;

function daysRemaining(trialStartedAt: string): number {
  const end = new Date(trialStartedAt).getTime() + TRIAL_DAYS * MS_PER_DAY;
  return (end - Date.now()) / MS_PER_DAY;
}

function greeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 18) return "Good afternoon";
  return "Good evening";
}

function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase())
    .join("");
}

interface NavItem {
  href: string;
  label: string;
  icon: ComponentType<{ size?: number; className?: string }>;
}

const basePath = process.env.NEXT_PUBLIC_BASE_PATH ?? "";

const navItems: NavItem[] = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/quotes", label: "Quotes", icon: ReceiptText },
  { href: "/invoices", label: "Invoices", icon: FileText },
  { href: "/pos", label: "Point of Sale", icon: ShoppingCart },
  { href: "/products", label: "Products", icon: Package },
  { href: "/customers", label: "Customers", icon: Users },
  { href: "/reports", label: "Reports", icon: BarChart3 },
  { href: "/settings", label: "Settings", icon: Settings },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { data, loaded } = useAppData();
  const { isLoggedIn, subscriptionState, trialStartedAt, bootstrapBusiness, refreshStatus } = useAuth();
  // Mounted unconditionally (before the `!loaded`/`!onboarded` early
  // returns below) so its hook order never changes between renders. It
  // no-ops internally whenever `isLoggedIn` is false.
  useSyncEngine({ isLoggedIn, bootstrapBusiness, refreshStatus });
  // Time-of-day greeting is computed after mount only: it depends on the
  // viewer's clock, which never matches the build machine's clock, so
  // rendering it during SSR would cause a hydration mismatch.
  const [greetingText, setGreetingText] = useState("Hello");
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setGreetingText(greeting());
  }, []);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [trialBannerDismissed, setTrialBannerDismissed] = useState(false);

  if (!loaded) {
    // Briefly shown while the localStorage load effect resolves. Its
    // absence would otherwise flash a "not onboarded" wizard for anyone
    // who has already set up their business, since seed data (used for
    // this component's very first render, before the load effect runs)
    // defaults onboarded to false.
    return <div className="min-h-screen bg-paper" />;
  }

  if (!data.onboarded) {
    return <OnboardingWizard />;
  }

  const trialDaysLeft = isLoggedIn && trialStartedAt ? daysRemaining(trialStartedAt) : null;
  const showSoonBanner =
    isLoggedIn &&
    subscriptionState === "trialing" &&
    trialDaysLeft !== null &&
    trialDaysLeft > 0 &&
    trialDaysLeft < SOON_THRESHOLD_DAYS &&
    !trialBannerDismissed;
  const showLapsedBanner = isLoggedIn && subscriptionState === "lapsed";

  return (
    <div className="flex min-h-screen">
      <aside className="hidden md:flex w-60 shrink-0 flex-col border-r border-line bg-surface px-4 py-6 no-print">
        <div className="flex items-center gap-2.5 px-2 mb-8">
          <Image src={`${basePath}/logo-mark.png`} alt="" width={28} height={28} />
          <span className="font-bold text-lg">
            Shesha<span className="text-brand-deep">Biz</span>
          </span>
        </div>
        <nav className="flex flex-col gap-1">
          {navItems.map((item) => {
            const active = pathname === item.href;
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors ${
                  active
                    ? "bg-brand text-white"
                    : "text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
                }`}
              >
                <Icon size={18} />
                {item.label}
              </Link>
            );
          })}
        </nav>
      </aside>

      <div className="flex flex-1 flex-col min-w-0">
        {showLapsedBanner && (
          <div className="flex items-center justify-between gap-2.5 bg-error px-4 py-2.5 text-sm font-medium text-white no-print sm:px-6">
            <span className="flex items-center gap-2.5">
              <AlertTriangle size={16} className="shrink-0" />
              {TRIAL_LOCK_MESSAGE}
            </span>
            <Link
              href="/subscribe"
              className="shrink-0 rounded-full bg-white/15 px-3 py-1 text-xs font-semibold hover:bg-white/25"
            >
              Subscribe now
            </Link>
          </div>
        )}
        {!showLapsedBanner && showSoonBanner && trialDaysLeft !== null && (
          <div className="flex items-center justify-between gap-2.5 bg-brand-tint px-4 py-2.5 text-sm font-medium text-brand-deep no-print sm:px-6">
            <span className="flex items-center gap-2.5">
              <Clock size={16} className="shrink-0" />
              Your trial ends in {trialDaysLeft < 1 ? "less than a day" : `${Math.ceil(trialDaysLeft)} day${Math.ceil(trialDaysLeft) === 1 ? "" : "s"}`}
              . WhatsApp or call 063 353 1662 to activate SheshaBiz.
            </span>
            <button
              type="button"
              aria-label="Dismiss"
              onClick={() => setTrialBannerDismissed(true)}
              className="shrink-0 rounded-full p-1 hover:bg-black/[.06]"
            >
              <X size={14} />
            </button>
          </div>
        )}
        <header className="flex items-center justify-between border-b border-line bg-surface px-4 py-4 sm:px-6 no-print">
          <div className="flex items-center gap-2">
            <button
              type="button"
              aria-label="Open menu"
              onClick={() => setMobileMenuOpen(true)}
              className="flex h-9 w-9 items-center justify-center rounded-full text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06] md:hidden"
            >
              <Menu size={20} />
            </button>
            <div>
              <p className="text-sm text-ink-faint">{greetingText}</p>
              <h1 className="text-lg font-bold">{data.business.name}</h1>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              aria-label="Search"
              className="flex h-9 w-9 items-center justify-center rounded-full text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
            >
              <Search size={18} />
            </button>
            <button
              type="button"
              aria-label="Notifications"
              className="flex h-9 w-9 items-center justify-center rounded-full text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
            >
              <Bell size={18} />
            </button>
            {data.business.logoDataUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={data.business.logoDataUrl}
                alt=""
                className="ml-1 h-9 w-9 rounded-full object-cover"
              />
            ) : (
              <div className="ml-1 flex h-9 w-9 items-center justify-center rounded-full bg-brand text-sm font-bold text-white">
                {initials(data.business.name) || "SB"}
              </div>
            )}
          </div>
        </header>

        <main className="flex-1 px-6 py-6 print-area">{children}</main>

        <nav className="md:hidden sticky bottom-0 flex items-center justify-around border-t border-line bg-surface py-2 no-print">
          {navItems.slice(0, 5).map((item) => {
            const active = pathname === item.href;
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex flex-col items-center gap-0.5 px-2 py-1 text-[11px] font-medium ${
                  active ? "text-brand" : "text-ink-faint"
                }`}
              >
                <Icon size={20} />
                {item.label === "Point of Sale" ? "POS" : item.label}
              </Link>
            );
          })}
        </nav>
      </div>

      {mobileMenuOpen && (
        <div className="fixed inset-0 z-50 md:hidden no-print">
          <button
            type="button"
            aria-label="Close menu"
            onClick={() => setMobileMenuOpen(false)}
            className="absolute inset-0 bg-black/40"
          />
          <div className="absolute inset-y-0 left-0 flex w-72 max-w-[80vw] flex-col bg-surface px-4 py-6 shadow-xl">
            <div className="flex items-center justify-between px-2 mb-8">
              <div className="flex items-center gap-2.5">
                <Image src={`${basePath}/logo-mark.png`} alt="" width={28} height={28} />
                <span className="font-bold text-lg">
                  Shesha<span className="text-brand-deep">Biz</span>
                </span>
              </div>
              <button
                type="button"
                aria-label="Close menu"
                onClick={() => setMobileMenuOpen(false)}
                className="flex h-9 w-9 items-center justify-center rounded-full text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
              >
                <X size={20} />
              </button>
            </div>
            <nav className="flex flex-col gap-1">
              {navItems.map((item) => {
                const active = pathname === item.href;
                const Icon = item.icon;
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    onClick={() => setMobileMenuOpen(false)}
                    className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors ${
                      active
                        ? "bg-brand text-white"
                        : "text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
                    }`}
                  >
                    <Icon size={18} />
                    {item.label}
                  </Link>
                );
              })}
            </nav>
          </div>
        </div>
      )}
    </div>
  );
}
