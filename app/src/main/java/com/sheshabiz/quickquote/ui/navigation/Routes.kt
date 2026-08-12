package com.sheshabiz.quickquote.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val BUSINESS_SETUP = "business_setup"

    const val DASHBOARD = "dashboard"
    const val QUOTES = "quotes"
    const val CUSTOMERS = "customers"
    const val SETTINGS = "settings"

    const val CREATE_QUOTE = "quote_edit"
    const val QUOTE_ARG = "quoteId"
    fun createQuote(quoteId: Long? = null) =
        if (quoteId == null) "$CREATE_QUOTE?$QUOTE_ARG=-1" else "$CREATE_QUOTE?$QUOTE_ARG=$quoteId"
    const val CREATE_QUOTE_PATTERN = "$CREATE_QUOTE?$QUOTE_ARG={$QUOTE_ARG}"

    const val QUOTE_PREVIEW = "quote_preview"
    fun quotePreview(quoteId: Long) = "$QUOTE_PREVIEW/$quoteId"
    const val QUOTE_PREVIEW_PATTERN = "$QUOTE_PREVIEW/{$QUOTE_ARG}"

    const val CUSTOMER_EDIT = "customer_edit"
    const val CUSTOMER_ARG = "customerId"
    fun customerEdit(customerId: Long? = null) =
        if (customerId == null) "$CUSTOMER_EDIT?$CUSTOMER_ARG=-1" else "$CUSTOMER_EDIT?$CUSTOMER_ARG=$customerId"
    const val CUSTOMER_EDIT_PATTERN = "$CUSTOMER_EDIT?$CUSTOMER_ARG={$CUSTOMER_ARG}"

    const val EDIT_BUSINESS_PROFILE = "edit_business_profile"
}
