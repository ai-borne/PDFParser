package com.payslipmax.pdfparser.subscription

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
actual fun isDebugBuild(): Boolean {
    return Platform.isDebugBinary
}

/**
 * StoreKit issues a receipt named "sandboxReceipt" to TestFlight and Xcode-sandbox installs, and
 * "receipt" to real App Store production installs — the standard, App-Review-safe way to tell them
 * apart without a compile-time flag. No receipt at all (rare, pre-first-launch) counts as not
 * TestFlight, which is the safe default.
 */
actual fun isTestFlightBuild(): Boolean {
    return NSBundle.mainBundle.appStoreReceiptURL?.lastPathComponent == "sandboxReceipt"
}
