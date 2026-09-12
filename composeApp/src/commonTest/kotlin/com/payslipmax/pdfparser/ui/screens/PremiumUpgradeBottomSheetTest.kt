package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.billing.PurchaseResult
import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PremiumUpgradeBottomSheetTest {
    @Test
    fun purchaseSheetOutcome_dismisses_on_success() {
        val result = PurchaseResult.Success("tok_123")
        val outcome = purchaseSheetOutcome(result)
        assertEquals(PurchaseSheetOutcome.Success(AppStrings.statusPurchaseSuccess), outcome)
    }

    @Test
    fun purchaseSheetOutcome_stays_open_on_cancel_and_pending() {
        assertEquals(PurchaseSheetOutcome.StayOpen, purchaseSheetOutcome(PurchaseResult.UserCancelled))
        assertEquals(PurchaseSheetOutcome.StayOpen, purchaseSheetOutcome(PurchaseResult.Pending))
    }

    @Test
    fun purchaseSheetOutcome_shows_error_on_failure() {
        val outcome = purchaseSheetOutcome(PurchaseResult.Error("Card declined"))
        assertTrue(outcome is PurchaseSheetOutcome.ShowError)
        assertEquals("${AppStrings.statusPurchaseFailed}Card declined", outcome.message)
    }

    @Test
    fun restoreSheetOutcome_dismisses_on_success() {
        val result = PurchaseResult.Success("tok_123")
        val outcome = restoreSheetOutcome(result)
        assertEquals(PurchaseSheetOutcome.Success(AppStrings.statusRestorePurchasesSuccess), outcome)
    }

    @Test
    fun restoreSheetOutcome_stays_open_on_cancel_and_pending() {
        assertEquals(PurchaseSheetOutcome.StayOpen, restoreSheetOutcome(PurchaseResult.UserCancelled))
        assertEquals(PurchaseSheetOutcome.StayOpen, restoreSheetOutcome(PurchaseResult.Pending))
    }

    @Test
    fun restoreSheetOutcome_shows_error_on_failure() {
        val outcome = restoreSheetOutcome(PurchaseResult.Error("No active verified subscription found"))
        assertTrue(outcome is PurchaseSheetOutcome.ShowError)
        assertEquals("${AppStrings.statusRestorePurchasesFailed}No active verified subscription found", outcome.message)
    }

    // Both success branches used to set a confirmation message and dismiss the sheet in the same
    // breath, so a *successful* purchase or restore rendered its message into a sheet already being
    // torn down — the user saw a spinner, then silence. A success must therefore stay on screen for
    // a non-zero, readable interval; a zero here would silently restore the old bug.
    @Test
    fun success_defers_dismissal_so_the_confirmation_is_readable() {
        val purchaseDelay = dismissDelayMsFor(purchaseSheetOutcome(PurchaseResult.Success("tok_123")))
        val restoreDelay = dismissDelayMsFor(restoreSheetOutcome(PurchaseResult.Success("tok_123")))
        assertEquals(SUCCESS_FEEDBACK_VISIBLE_MS, purchaseDelay)
        assertEquals(SUCCESS_FEEDBACK_VISIBLE_MS, restoreDelay)
        assertTrue(SUCCESS_FEEDBACK_VISIBLE_MS > 0, "a success message dismissed instantly is never read")
    }

    // Failures and cancellations must leave the sheet open — the user needs to read the error and
    // retry without reopening the paywall.
    @Test
    fun non_success_outcomes_never_self_dismiss() {
        assertNull(dismissDelayMsFor(purchaseSheetOutcome(PurchaseResult.UserCancelled)))
        assertNull(dismissDelayMsFor(purchaseSheetOutcome(PurchaseResult.Pending)))
        assertNull(dismissDelayMsFor(purchaseSheetOutcome(PurchaseResult.Error("Card declined"))))
        assertNull(dismissDelayMsFor(restoreSheetOutcome(PurchaseResult.Error("No active verified subscription found"))))
    }
}
