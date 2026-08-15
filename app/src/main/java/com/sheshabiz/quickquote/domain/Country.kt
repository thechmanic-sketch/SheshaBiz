package com.sheshabiz.quickquote.domain

/** Countries SheshaBiz supports pricing/currency for. Switching country only changes the
 * currency symbol shown throughout the app — VAT rate stays separately user-editable in Settings. */
enum class Country(
    val displayName: String,
    val currencyCode: String,
    val currencySymbol: String
) {
    SOUTH_AFRICA("South Africa", "ZAR", "R"),
    ZIMBABWE("Zimbabwe", "USD", "$"),
    KENYA("Kenya", "KES", "KSh"),
    GHANA("Ghana", "GHS", "GH₵")
}
