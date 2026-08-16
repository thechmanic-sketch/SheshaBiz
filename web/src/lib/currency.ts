import { COUNTRIES, type Country } from "./types";

// Manual formatting (no Intl/toLocaleString) so server-rendered and
// client-hydrated output always match: Node's build environment doesn't
// ship full ICU locale data, so "en-ZA" silently formats differently on
// the server than in a real browser, which breaks hydration.
export function formatCurrency(amount: number, country: Country = "south_africa"): string {
  const symbol = COUNTRIES[country].currencySymbol;
  const sign = amount < 0 ? "-" : "";
  const [intPart, decPart] = Math.abs(amount).toFixed(2).split(".");
  const grouped = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, " ");
  return `${sign}${symbol} ${grouped},${decPart}`;
}
