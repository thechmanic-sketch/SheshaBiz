import { COUNTRIES, type Country } from "./types";

// Manual formatting (no Intl/toLocaleString) so server-rendered and
// client-hydrated output always match: Node's build environment doesn't
// ship full ICU locale data, so a locale string like "en-ZA" silently
// formats differently on the server than in a real browser, which breaks
// hydration.
//
// Zimbabwe trades in USD, which conventionally uses comma thousands +
// period decimal ("$1,234.56"); ZAR/KES/GHS use space thousands + comma
// decimal ("R 1 234,56"). Both are built from the same digits, just with
// the separators swapped.
const US_STYLE_COUNTRIES: ReadonlySet<Country> = new Set(["zimbabwe"]);

export function formatCurrency(amount: number, country: Country = "south_africa"): string {
  const symbol = COUNTRIES[country].currencySymbol;
  const sign = amount < 0 ? "-" : "";
  const [intPart, decPart] = Math.abs(amount).toFixed(2).split(".");
  const usStyle = US_STYLE_COUNTRIES.has(country);
  const thousandsSep = usStyle ? "," : " ";
  const grouped = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, thousandsSep);
  const decimalSep = usStyle ? "." : ",";
  return `${sign}${symbol}${usStyle ? "" : " "}${grouped}${decimalSep}${decPart}`;
}
