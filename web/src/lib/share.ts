/**
 * Shares a text summary via the Web Share API (which on a phone opens the
 * native share sheet — WhatsApp included) where supported, falling back
 * to a wa.me deep link on desktop browsers that don't implement it.
 *
 * There's no PDF attachment here: this app has no backend yet to host a
 * document at a shareable URL, and generating one client-side is a
 * separate piece of work. This shares the same summary a person would
 * type by hand — good enough to notify a customer, not a substitute for
 * the printed/emailed document.
 */
export async function shareText(title: string, text: string): Promise<void> {
  if (typeof navigator !== "undefined" && navigator.share) {
    try {
      await navigator.share({ title, text });
    } catch {
      // User cancelled the share sheet, or the browser refused — either
      // way, silently do nothing rather than surprise them with a
      // fallback tab they didn't ask for.
    }
    return;
  }
  window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, "_blank", "noopener,noreferrer");
}
