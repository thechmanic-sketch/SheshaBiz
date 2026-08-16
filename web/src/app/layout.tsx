import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { AppShell } from "@/components/layout/AppShell";
import { AppDataProvider } from "@/lib/store";
import { AuthProvider } from "@/lib/auth";
import { TrialGateProvider } from "@/lib/trial";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "SheshaBiz — Business Suite",
  description: "Quotes, invoices, and point-of-sale for South African small business.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full bg-paper text-ink">
        <AuthProvider>
          <AppDataProvider>
            <TrialGateProvider>
              <AppShell>{children}</AppShell>
            </TrialGateProvider>
          </AppDataProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
