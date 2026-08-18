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
  /** Primary signup path now that "Confirm email" is OFF in the Supabase
   * dashboard: supabase.auth.signUp() with a password creates the account
   * and logs the user in immediately, with zero email sent — so it isn't
   * subject to the shared free-tier email sender's rate limit the way
   * sendCode()/OTP is. */
  signUpWithPassword: (
    email: string,
    password: string,
    metadata?: { full_name?: string; phone?: string; business_name?: string; city?: string }
  ) => Promise<{ ok: boolean; error?: string; accountExists?: boolean }>;
  /** Password login for accounts created via signUpWithPassword, or legacy
   * OTP accounts that have since set a password via setPassword(). */
  signInWithPassword: (email: string, password: string) => Promise<{ ok: boolean; error?: string }>;
  /** Lets a user who just logged in via OTP (a legacy account with no
   * password yet) set one for next time. This is a convenience, not a
   * gate: it must never be allowed to block the caller from proceeding —
   * the caller decides what to do with a failure (e.g. just move on). */
  setPassword: (password: string) => Promise<{ ok: boolean; error?: string }>;
  /** Sends a password-reset email via Supabase's resetPasswordForEmail, for
   * an account that has a password but forgot it — separate from the
   * legacy no-password OTP fallback. Supabase's reset endpoint is
   * anti-enumeration by design: this always resolves ok:true for any
   * non-network outcome, never revealing whether the email has an account. */
  requestPasswordReset: (email: string) => Promise<{ ok: boolean; error?: string }>;
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

  const signUpWithPassword = useCallback(
    async (
      emailAddress: string,
      password: string,
      metadata?: { full_name?: string; phone?: string; business_name?: string; city?: string }
    ) => {
      try {
        const { data, error } = await supabase.auth.signUp({
          email: emailAddress,
          password,
          options: metadata ? { data: metadata } : undefined,
        });
        if (!error && data.session) {
          // Real new account, already logged in (no email sent — "Confirm
          // email" is OFF). Same post-login bootstrap as verifyCode().
          await bootstrapBusiness();
          requestSync();
          return { ok: true };
        }
        if (!error && !data.session && data.user?.identities?.length === 0) {
          // Supabase's documented anti-enumeration signal: signUp() on an
          // email that already has an account returns no error and no
          // session, with an empty identities array, instead of an error
          // that would reveal the email is taken.
          return {
            ok: false,
            accountExists: true,
            error: "An account already exists for this email — log in instead.",
          };
        }
        return {
          ok: false,
          error: error?.message ?? "Couldn't create the account automatically. Try logging in, or use a code.",
        };
      } catch {
        return {
          ok: false,
          error: "Couldn't create the account automatically. Try logging in, or use a code.",
        };
      }
    },
    [bootstrapBusiness]
  );

  const signInWithPassword = useCallback(
    async (emailAddress: string, password: string) => {
      try {
        const { error } = await supabase.auth.signInWithPassword({
          email: emailAddress,
          password,
        });
        if (error) {
          return { ok: false, error: "Incorrect email or password." };
        }
        await bootstrapBusiness();
        requestSync();
        return { ok: true };
      } catch {
        return { ok: false, error: "Incorrect email or password." };
      }
    },
    [bootstrapBusiness]
  );

  const setPassword = useCallback(async (password: string) => {
    // Best-effort convenience after a legacy OTP login — never treat a
    // failure here as blocking; the caller decides what to do with it
    // (typically: show the error but proceed to the app regardless).
    try {
      const { error } = await supabase.auth.updateUser({ password });
      if (error) return { ok: false, error: error.message };
      return { ok: true };
    } catch {
      return { ok: false, error: "Couldn't save the password. You can set one later from settings." };
    }
  }, []);

  const requestPasswordReset = useCallback(async (emailAddress: string) => {
    try {
      const { error } = await supabase.auth.resetPasswordForEmail(emailAddress, {
        redirectTo: `${window.location.origin}/reset-password`,
      });
      // Anti-enumeration: Supabase's own endpoint never reveals whether the
      // email has an account, so neither do we — any outcome short of a
      // genuine network/unexpected failure reports ok:true.
      if (error) {
        const status = "status" in error ? (error as { status?: number }).status : undefined;
        if (status && status >= 500) {
          return { ok: false, error: "Couldn't send the reset link. Check your connection and try again." };
        }
        return { ok: true };
      }
      return { ok: true };
    } catch {
      return { ok: false, error: "Couldn't send the reset link. Check your connection and try again." };
    }
  }, []);

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
      signUpWithPassword,
      signInWithPassword,
      setPassword,
      requestPasswordReset,
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
      signUpWithPassword,
      signInWithPassword,
      setPassword,
      requestPasswordReset,
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
