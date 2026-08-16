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
} from "lucide-react";
import { useAppData } from "@/lib/store";
import type { ComponentType } from "react";

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
  const { data } = useAppData();
  // Time-of-day greeting is computed after mount only: it depends on the
  // viewer's clock, which never matches the build machine's clock, so
  // rendering it during SSR would cause a hydration mismatch.
  const [greetingText, setGreetingText] = useState("Hello");
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setGreetingText(greeting());
  }, []);

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
        <header className="flex items-center justify-between border-b border-line bg-surface px-6 py-4 no-print">
          <div>
            <p className="text-sm text-ink-faint">{greetingText}</p>
            <h1 className="text-lg font-bold">{data.business.name}</h1>
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
    </div>
  );
}
