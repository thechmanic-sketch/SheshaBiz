package com.sheshabiz.quickquote.domain

import com.sheshabiz.quickquote.data.prefs.AuthPreferences
import com.sheshabiz.quickquote.data.prefs.SubscriptionState

/**
 * The single source of truth for the trial soft-lock: creating new quotes/invoices/customers/
 * products/sales or editing existing ones is blocked for two distinct reasons — see
 * [LockReason] — while [SubscriptionState.TRIALING]/[SubscriptionState.ACTIVE] accounts are
 * never blocked. Viewing, PDF/print/WhatsApp-sharing existing records, and the Settings backup/
 * export feature are never gated by any of this.
 */
object TrialGate {
    /** Why an action is currently blocked — each reason gets its own message and its own
     * "do something about it" destination, since they're not the same problem for the user. */
    enum class LockReason { LOGGED_OUT, LAPSED }

    const val LOGGED_OUT_MESSAGE = "Sign up for your free 7-day trial to create quotes, invoices, and more."
    const val LAPSED_MESSAGE = "Your trial has ended. WhatsApp or call 063 353 1662 to activate SheshaBiz."

    /** Kept as an alias of [LAPSED_MESSAGE] for existing callers (e.g. the Dashboard's
     * persistent trial-ended banner) that only ever cared about the lapsed case. */
    const val LOCKED_MESSAGE = LAPSED_MESSAGE

    /** Returns why create/edit should be blocked right now, or null if it's allowed. */
    fun lockReason(authPreferences: AuthPreferences): LockReason? =
        when (authPreferences.subscriptionState.value) {
            SubscriptionState.LOGGED_OUT -> LockReason.LOGGED_OUT
            SubscriptionState.LAPSED -> LockReason.LAPSED
            SubscriptionState.TRIALING, SubscriptionState.ACTIVE -> null
        }
}
