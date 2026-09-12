package com.payslipmax.pdfparser.billing

import com.revenuecat.purchases.kmp.models.Package

/**
 * Outcome of resolving the purchasable yearly package, keeping the three failure modes distinct.
 *
 * They collapsed into a single `null` before, so every one surfaced as the same opaque "package
 * unavailable" error while the paywall silently kept showing its hardcoded price — a dead product
 * rendered as a healthy paywall. Each failure needs a different fix (retry/credentials, mark an
 * offering Current, attach a product to the annual slot), so each names itself.
 */
sealed class YearlyPackageResolution {
    data class Resolved(val yearlyPackage: Package) : YearlyPackageResolution()

    /** A resolution failure whose [message] is surfaced verbatim in the purchase error. */
    sealed class Failure(val message: String) : YearlyPackageResolution()

    /** `getOfferings` itself failed — no offerings were fetched at all. */
    class OfferingsUnavailable(message: String) : Failure(message)

    /** Offerings fetched, but no offering is marked Current/Default in the RevenueCat dashboard. */
    data object NoCurrentOffering : Failure("No current offering is configured in RevenueCat")

    /** A current offering exists, but its annual (`$rc_annual`) slot holds no package. */
    data object NoAnnualPackage : Failure("The current RevenueCat offering has no annual package")
}
