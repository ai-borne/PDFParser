package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.billing.FakeBillingManager
import com.payslipmax.pdfparser.billing.PurchaseResult
import com.payslipmax.pdfparser.billing.SubscriptionState
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.subscription.FeatureGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelBillingTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fakeBillingManager = FakeBillingManager(SubscriptionState.Inactive)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun launchPurchaseFlow_triggers_billingManager_and_grants_entitlement_on_success() =
        runTest {
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            fakeBillingManager.shouldPurchaseSucceed = true

            var purchaseResult: PurchaseResult? = null
            viewModel.launchPurchaseFlow { result ->
                purchaseResult = result
            }

            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(purchaseResult is PurchaseResult.Success)
            assertTrue(viewModel.subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        }

    @Test
    fun launchPurchaseFlow_handles_cancellation() =
        runTest {
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            fakeBillingManager.shouldPurchaseSucceed = false
            fakeBillingManager.mockErrorMessage = "USER_CANCELLED"

            var purchaseResult: PurchaseResult? = null
            viewModel.launchPurchaseFlow { result ->
                purchaseResult = result
            }

            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(purchaseResult is PurchaseResult.Error)
        }

    @Test
    fun restorePurchases_triggers_billingManager_and_grants_entitlement_on_success() =
        runTest {
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            fakeBillingManager.shouldRestoreSucceed = true

            var restoreResult: PurchaseResult? = null
            viewModel.restorePurchases { result ->
                restoreResult = result
            }

            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(restoreResult is PurchaseResult.Success)
            assertTrue(viewModel.subscriptionManager.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        }

    @Test
    fun restorePurchases_surfaces_error_without_granting_entitlement() =
        runTest {
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            fakeBillingManager.shouldRestoreSucceed = false
            fakeBillingManager.mockErrorMessage = "No active verified subscription found"

            var restoreResult: PurchaseResult? = null
            viewModel.restorePurchases { result ->
                restoreResult = result
            }

            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(restoreResult is PurchaseResult.Error)
        }

    @Test
    fun premiumPriceState_updates_from_live_billingManager_price() =
        runTest {
            fakeBillingManager.fakeFormattedPrice = "₹199 / year"
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("₹199 / year", viewModel.premiumPriceState.value)
        }

    // The store is the only source of truth for price. If it returns nothing, the app must say so
    // rather than invent a number — a hardcoded fallback made a dead product render as a healthy
    // paywall, which is how a user could be charged for a product that never resolved.
    @Test
    fun premiumPriceState_stays_null_when_billingManager_price_unavailable() =
        runTest {
            fakeBillingManager.fakeFormattedPrice = null
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(viewModel.premiumPriceState.value)
        }

    // The startup fetch can land before StoreKit has resolved the device's storefront, which
    // returns a price in the wrong currency (observed on TestFlight 1.2 (5): the paywall showed
    // "$9.99" while Apple's own purchase sheet charged "₹ 999 per year" on the same Indian
    // account). A price read once at init and never re-read freezes that wrong answer for the
    // whole session, so the app quotes a number it will not charge. Re-reading when the paywall
    // is presented is what makes the displayed price the one the user is actually offered.
    @Test
    fun refreshPremiumPrice_rereads_the_store_so_a_late_storefront_replaces_the_startup_price() =
        runTest {
            fakeBillingManager.fakeFormattedPrice = "$9.99"
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals("$9.99", viewModel.premiumPriceState.value)

            fakeBillingManager.fakeFormattedPrice = "₹999.00"
            viewModel.refreshPremiumPrice()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("₹999.00", viewModel.premiumPriceState.value)
        }

    // A transient offerings failure while the paywall is open must not blank a price the user is
    // already looking at — that would disable Unlock mid-decision. Losing a known-good price is a
    // worse failure than showing the last one the store gave us.
    @Test
    fun refreshPremiumPrice_keeps_the_last_known_price_when_the_store_returns_nothing() =
        runTest {
            fakeBillingManager.fakeFormattedPrice = "₹999.00"
            val repository =
                PayslipRepository(
                    com.payslipmax.pdfparser.testing.FakePayslipDao(),
                    com.payslipmax.pdfparser.testing.FakePdfParser(),
                    Dispatchers.Unconfined,
                )

            val viewModel =
                PayslipViewModel(
                    repository = repository,
                    billingManager = fakeBillingManager,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals("₹999.00", viewModel.premiumPriceState.value)

            fakeBillingManager.fakeFormattedPrice = null
            viewModel.refreshPremiumPrice()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("₹999.00", viewModel.premiumPriceState.value)
        }
}
