import { createClient } from "@supabase/supabase-js";

// Safe to commit: this is the anon/publishable key, not the service_role
// key. Supabase designs it to be exposed client-side — access control is
// enforced by Row Level Security policies on the database, not by keeping
// this key secret.
const supabaseUrl = "https://djeipbwcyfaxjlllazbl.supabase.co";
const supabaseAnonKey = "sb_publishable_nuoqwOSuf6pJcTaj9lrrDg_hxgLHiHK";

export const supabase = createClient(supabaseUrl, supabaseAnonKey);
