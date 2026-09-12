package com.payslipmax.pdfparser.billing

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RevenueCatApiKeyTest {
    @Test
    fun iosShipsRealProductionKeyNeverSandboxTestKey() {
        val key = revenueCatApiKey()

        assertFalse(
            key.startsWith("test_"),
            "iOS must never ship the sandbox RevenueCat key to production — this exact mistake " +
                "caused the earlier Guideline 2.1 App Store rejection.",
        )
        assertTrue(key.startsWith("appl_"), "Expected a real RevenueCat public SDK key (appl_...)")
    }
}
