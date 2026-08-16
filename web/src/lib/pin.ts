async function sha256Hex(text: string): Promise<string> {
  const bytes = new TextEncoder().encode(text);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

// Static salt is fine here: this PIN only ever gates actions within the
// same browser (there's no server to authenticate against yet), so it's
// a "did you mean to do this" speed bump, not a real access-control
// boundary. The point is protecting against an accidental tap, not an
// attacker with access to this device's storage.
const SALT = "sheshabiz-web-delete-pin-v1";

export function hashPin(pin: string): Promise<string> {
  return sha256Hex(SALT + pin);
}

export async function verifyPin(pin: string, hash: string): Promise<boolean> {
  return (await hashPin(pin)) === hash;
}
