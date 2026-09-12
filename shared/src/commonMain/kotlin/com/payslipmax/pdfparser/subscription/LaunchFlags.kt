package com.payslipmax.pdfparser.subscription

/**
 * Single source of truth for the v1.0 "free launch" strategy (see
 * `docs/Launch/05_launch_strategy_and_resolution.md`), split per-platform per
 * `docs/Launch/08_ios_monetization_phaseplan.md` Phase 1 so iOS and Android can flip
 * independently — Android is still blocked by Google Play's mandatory 14-day closed-testing
 * window before BillDesk merchant KYC can be completed, while iOS's RevenueCat/StoreKit
 * rollout proceeds separately. Call sites read the platform-specific constant via
 * [isFreeLaunchModePlatform] rather than either constant directly.
 */
object LaunchFlags {
    const val FREE_LAUNCH_MODE_IOS: Boolean = true
    const val FREE_LAUNCH_MODE_ANDROID: Boolean = true
}
