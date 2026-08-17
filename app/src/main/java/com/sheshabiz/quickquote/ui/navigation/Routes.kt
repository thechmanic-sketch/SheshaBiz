package com.sheshabiz.quickquote.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val BUSINESS_SETUP = "business_setup"

    const val DASHBOARD = "dashboard"
    const val QUOTES = "quotes"
    const val INVOICES = "invoices"
    const val CUSTOMERS = "customers"
    const val SETTINGS = "settings"
    const val POS = "pos"
    const val PRODUCTS = "products"
    const val MORE = "more"
    const val SALES_HISTORY = "sales_history"
    const val REPORTS = "reports"
    const val APP_LOCK = "app_lock"
    const val PIN_SETUP = "pin_setup"
    const val ACCOUNT_LOGIN = "account_login"
    const val SUBSCRIPTION = "subscription"

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

    const val CREATE_INVOICE = "invoice_edit"
    const val INVOICE_ARG = "invoiceId"
    fun createInvoice(invoiceId: Long? = null) =
        if (invoiceId == null) "$CREATE_INVOICE?$INVOICE_ARG=-1" else "$CREATE_INVOICE?$INVOICE_ARG=$invoiceId"
    const val CREATE_INVOICE_PATTERN = "$CREATE_INVOICE?$INVOICE_ARG={$INVOICE_ARG}"

    const val INVOICE_PREVIEW = "invoice_preview"
    fun invoicePreview(invoiceId: Long) = "$INVOICE_PREVIEW/$invoiceId"
    const val INVOICE_PREVIEW_PATTERN = "$INVOICE_PREVIEW/{$INVOICE_ARG}"

    const val EDIT_BUSINESS_PROFILE = "edit_business_profile"

    const val PRODUCT_EDIT = "product_edit"
    const val PRODUCT_ARG = "productId"
    fun productEdit(productId: Long? = null) =
        if (productId == null) "$PRODUCT_EDIT?$PRODUCT_ARG=-1" else "$PRODUCT_EDIT?$PRODUCT_ARG=$productId"
    const val PRODUCT_EDIT_PATTERN = "$PRODUCT_EDIT?$PRODUCT_ARG={$PRODUCT_ARG}"

    const val SALE_PREVIEW = "sale_preview"
    const val SALE_ARG = "saleId"
    fun salePreview(saleId: Long) = "$SALE_PREVIEW/$saleId"
    const val SALE_PREVIEW_PATTERN = "$SALE_PREVIEW/{$SALE_ARG}"
}
