package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

/**
 * Regression coverage for the History screen's ledger table toggle: the whole header row must be
 * tappable, not just the small chevron icon (previously an IconButton with only the icon as the
 * touch target).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoricalLedgerCardUiTest {
    private val record =
        LedgerRecordEntity(
            dateStr = "05/2026",
            year = 2026,
            monthNum = 5,
            basicPay = 100000.0,
            dearnessAllowance = 50000.0,
            militaryServicePay = 15500.0,
            transportAllowance = 7200.0,
            transportAllowanceDa = 3600.0,
            houseRentAllowance = 24000.0,
            grossPay = 200300.0,
            dsopSubscription = 20000.0,
            incomeTax = 25000.0,
            netPay = 155300.0,
        )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tappingTitleTextExpandsTheTable() =
        runComposeUiTest {
            setContent {
                HistoricalLedgerCard(ledgerRecords = listOf(record))
            }

            // Table is collapsed initially — header cell not yet in the tree.
            onNodeWithText("Month").assertDoesNotExist()

            // Tapping the title label (not the chevron icon) must expand it — this is the row-wide
            // touch target the icon-only IconButton previously failed to provide.
            onNodeWithText("Historical Ledger Table").performClick()
            waitForIdle()

            onNodeWithText("Month").assertExists()
        }
}
