package com.payslipmax.pdfparser.subscription

/**
 * Single source of truth for the v1.0 "free launch" strategy (see
 * `docs/Launch/05_launch_strategy_and_resolution.md`).
 *
 * PayslipMax v1.0 ships with every [FeatureGate] unlocked on both platforms because neither
 * store's billing pipeline is ready yet (Android is blocked by Google Play's mandatory 14-day
 * closed-testing window before BillDesk merchant KYC can be completed; iOS was rejected under
 * App Store Guideline 2.1 because RevenueCat has no Apple app / StoreKit product configured).
 * Flip [FREE_LAUNCH_MODE] to `false` once section 4 of that doc (RevenueCat + StoreKit product
 * setup on both platforms) is complete, to re-enable the real paywall for v1.1.
 */
object LaunchFlags {
    const val FREE_LAUNCH_MODE: Boolean = true
}
