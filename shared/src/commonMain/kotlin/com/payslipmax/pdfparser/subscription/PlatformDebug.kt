package com.payslipmax.pdfparser.subscription

expect fun isDebugBuild(): Boolean

/**
 * True only for a TestFlight or Xcode-sandbox install (StoreKit sandbox receipt on iOS), never for
 * a real App Store production binary. Lets Phase 7 of `docs/Launch/08_ios_monetization_phaseplan.md`
 * make the paywall reachable for sandbox purchase testing via [DevOverride] without touching
 * [LaunchFlags.FREE_LAUNCH_MODE_IOS] itself. Always `false` on Android.
 */
expect fun isTestFlightBuild(): Boolean
