package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.ui.theme.AppStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Phase 8 regression tests for the Phase 7 debt where a missing store price fell back to a
 * hardcoded amount, so a dead product rendered as a healthy, purchasable paywall
 * (`docs/Launch/08_ios_monetization_phaseplan.md`).
 *
 * The sibling debt — a success confirmation written into a sheet the same branch dismissed — is
 * covered in `PremiumUpgradeBottomSheetTest` via [dismissDelayMsFor], because asserting the timing
 * through a real ModalBottomSheet means driving three Robolectric windows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PremiumUpgradeBottomSheetUiTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun unlockIsNotOfferedWhenTheStoreReturnedNoPrice() =
        runComposeUiTest {
            setContent {
                PremiumUpgradeBottomSheet(
                    onDismissRequest = {},
                    onUnlockClick = { error("Unlock must not be reachable without a live store price") },
                    price = null,
                )
            }

            onNodeWithText(AppStrings.settingsPremiumPlanPriceUnavailable).assertExists()
            onNodeWithText(AppStrings.settingsPremiumUpgradeBtn).assertIsNotEnabled()
            // Restore stays available: an existing subscriber must still recover entitlement when
            // it is the product fetch that failed, not their subscription.
            onNodeWithText(AppStrings.settingsRestorePurchasesTitle).assertIsEnabled()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun livePriceIsShownAndUnlockIsOfferedWhenTheStoreResolves() =
        runComposeUiTest {
            setContent {
                PremiumUpgradeBottomSheet(
                    onDismissRequest = {},
                    onUnlockClick = {},
                    price = "₹ 199.00",
                )
            }

            onNodeWithText("₹ 199.00").assertExists()
            onNodeWithText(AppStrings.settingsPremiumUpgradeBtn).assertIsEnabled()
        }
}
