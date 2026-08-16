package com.sheshabiz.quickquote.domain

import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import com.sheshabiz.quickquote.data.prefs.SubscriptionState

/**
 * The single source of truth for the trial soft-lock: once a logged-in account's trial has
 * lapsed, creating new quotes/invoices/customers/products/sales or editing existing ones is
 * blocked until payment is confirmed (an admin flips `businesses.status` in Supabase — this
 * app never collects payment itself). Viewing, PDF/print/WhatsApp-sharing existing records is
 * never gated, and a user who has never logged in is never gated by any of this — see
 * [AuthPreferences.subscriptionState]'s [SubscriptionState.LOGGED_OUT], which this treats the
 * same as [SubscriptionState.TRIALING]/[SubscriptionState.ACTIVE]: not locked.
 */
object TrialGate {
    const val LOCKED_MESSAGE = "Your trial has ended. WhatsApp or call 063 353 1662 to activate SheshaBiz."

    fun isLocked(authPreferences: AuthPreferences): Boolean =
        authPreferences.subscriptionState.value == SubscriptionState.LAPSED
}
