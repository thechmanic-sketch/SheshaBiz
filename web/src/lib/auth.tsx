"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { supabase } from "./supabase";
import { requestSync } from "./sync";

/**
 * `null` = not logged in (never gated). "trialing"/"active" come straight
 * off the business row's `status` column, but that column is never
 * auto-flipped to "lapsed" by anything — `business_is_active()` computes
 * expiry dynamically instead, since activation is a human editing the row
 * in the dashboard, not a job that runs on a schedule. So a lapsed trial
 * surfaces here as "lapsed" purely because `is_active` came back false,
 * even though the stored `status` still literally reads "trialing".
 */
export type SubscriptionState = "trialing" | "active" | "lapsed" | null;

interface BusinessStatusRow {
  status: string;
  trial_started_at: string | null;
  valid_until: string | null;
  is_active: boolean;
}

interface AuthContextValue {
  isLoggedIn: boolean;
  email: string | null;
  loaded: boolean;
  businessId: string | null;
  subscriptionState: SubscriptionState;
  trialStartedAt: string | null;
  validUntil: string | null;
  sendCode: (email: string) => Promise<{ ok: boolean; error?: string }>;
  verifyCode: (email: string, code: string) => Promise<{ ok: boolean; error?: string }>;
  signOut: () => Promise<void>;
  /** Idempotent — safe to call repeatedly. Returns the caller's business id,
   * creating the row (with a fresh trial_started_at) only the first time
   * any device ever calls it for that login. */
  bootstrapBusiness: () => Promise<string | null>;
  refreshStatus: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [email, setEmail] = useState<string | null>(null);
  const [businessId, setBusinessId] = useState<string | null>(null);
  const [subscriptionState, setSubscriptionState] = useState<SubscriptionState>(null);
  const [trialStartedAt, setTrialStartedAt] = useState<string | null>(null);
  const [validUntil, setValidUntil] = useState<string | null>(null);
  // `loaded` (state, not a ref) gates rendering-sensitive reads of auth
  // state, mirroring the AppDataProvider pattern in `./store.tsx`: React
  // Strict Mode replays effect setup functions twice on mount, and a ref
  // survives that replay while state doesn't, so a ref-based gate could let
  // a second pass observe the still-unresolved session before the first
  // pass's getSession() has actually committed. Gating on state means
  // consumers only see loaded=true once the initial session check has
  // genuinely resolved and re-rendered.
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadSession() {
      try {
        const { data } = await supabase.auth.getSession();
        if (cancelled) return;
        const session = data.session;
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setIsLoggedIn(!!session);
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setEmail(session?.user?.email ?? null);
      } catch {
        // ignore — treat as logged out
      } finally {
        if (!cancelled) setLoaded(true);
      }
    }

    loadSession();

    const { data: subscription } = supabase.auth.onAuthStateChange((_event, session) => {
      setIsLoggedIn(!!session);
      setEmail(session?.user?.email ?? null);
      if (!session) {
        // Signed out — this device's cached business/trial state no longer
        // applies to whoever uses the app next.
        setBusinessId(null);
        setSubscriptionState(null);
        setTrialStartedAt(null);
        setValidUntil(null);
      }
    });

    return () => {
      cancelled = true;
      subscription.subscription.unsubscribe();
    };
  }, []);

  const sendCode = useCallback(async (emailAddress: string) => {
    try {
      const { error } = await supabase.auth.signInWithOtp({
        email: emailAddress,
        options: { shouldCreateUser: true },
      });
      if (error) {
        const status = "status" in error ? (error as { status?: number }).status : undefined;
        if (status === 429 || /rate limit/i.test(error.message)) {
          return {
            ok: false,
            error: "Too many attempts — please wait a minute before requesting another code.",
          };
        }
        return { ok: false, error: error.message };
      }
      return { ok: true };
    } catch {
      return { ok: false, error: "Couldn't send the code. Check your connection and try again." };
    }
  }, []);

  const bootstrapBusiness = useCallback(async () => {
    try {
      const { data, error } = await supabase.rpc("bootstrap_business");
      if (error || !data) return null;
      const id = data as string;
      setBusinessId(id);
      return id;
    } catch {
      return null;
    }
  }, []);

  const refreshStatus = useCallback(async () => {
    try {
      const { data, error } = await supabase.rpc("get_my_business_status");
      if (error) return;
      const rows = data as BusinessStatusRow[] | null;
      const row = rows?.[0];
      if (!row) return;
      setSubscriptionState(row.is_active ? (row.status as "trialing" | "active") : "lapsed");
      setTrialStartedAt(row.trial_started_at);
      setValidUntil(row.valid_until);
    } catch {
      // offline — keep whatever was last cached
    }
  }, []);

  const verifyCode = useCallback(async (emailAddress: string, code: string) => {
    try {
      const { error } = await supabase.auth.verifyOtp({
        email: emailAddress,
        token: code,
        type: "email",
      });
      if (error) {
        return { ok: false, error: "That code is incorrect or expired." };
      }
      // Trial timing starts here, at first successful login — never at
      // first app use. bootstrap_business() is idempotent: re-logging into
      // the same email resumes the same trial rather than restarting it.
      await bootstrapBusiness();
      requestSync();
      return { ok: true };
    } catch {
      return { ok: false, error: "That code is incorrect or expired." };
    }
  }, [bootstrapBusiness]);

  const signOut = useCallback(async () => {
    try {
      await supabase.auth.signOut();
    } catch {
      // ignore — local state is reconciled via onAuthStateChange regardless
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      isLoggedIn,
      email,
      loaded,
      businessId,
      subscriptionState,
      trialStartedAt,
      validUntil,
      sendCode,
      verifyCode,
      signOut,
      bootstrapBusiness,
      refreshStatus,
    }),
    [
      isLoggedIn,
      email,
      loaded,
      businessId,
      subscriptionState,
      trialStartedAt,
      validUntil,
      sendCode,
      verifyCode,
      signOut,
      bootstrapBusiness,
      refreshStatus,
    ]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
