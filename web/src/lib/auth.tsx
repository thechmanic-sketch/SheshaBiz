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

interface AuthContextValue {
  isLoggedIn: boolean;
  email: string | null;
  loaded: boolean;
  sendCode: (email: string) => Promise<{ ok: boolean; error?: string }>;
  verifyCode: (email: string, code: string) => Promise<{ ok: boolean; error?: string }>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [email, setEmail] = useState<string | null>(null);
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
      return { ok: true };
    } catch {
      return { ok: false, error: "That code is incorrect or expired." };
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
    () => ({ isLoggedIn, email, loaded, sendCode, verifyCode, signOut }),
    [isLoggedIn, email, loaded, sendCode, verifyCode, signOut]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
