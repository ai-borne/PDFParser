package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun HistoricalLedgerCard(
    ledgerRecords: List<LedgerRecordEntity>,
    modifier: Modifier = Modifier,
) {
    if (ledgerRecords.isEmpty()) return
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { isExpanded = !isExpanded })
                        .padding(horizontal = AppDimensions.PaddingMedium, vertical = AppDimensions.PaddingLarge),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = AppStrings.historyLedgerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription =
                        if (isExpanded) AppStrings.historyLedgerCollapseDesc else AppStrings.historyLedgerExpandDesc,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            if (isExpanded) {
                Column(modifier = Modifier.padding(horizontal = AppDimensions.PaddingMedium)) {
                    LedgerTable(ledgerRecords = ledgerRecords)
                    Spacer(modifier = Modifier.height(AppDimensions.PaddingMedium))
                }
            }
        }
    }
}

@Composable
private fun LedgerTable(
    ledgerRecords: List<LedgerRecordEntity>,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
    ) {
        LedgerTableHeader()
        ledgerRecords.forEach { record ->
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            LedgerTableRow(record = record)
        }
    }
}

@Composable
private fun LedgerTableHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimensions.SpacingSmall),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        val cellModifier = Modifier.width(AppDimensions.LedgerCellWidth)
        Text(text = AppStrings.historyLedgerHeaderMonth, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = AppStrings.historyLedgerHeaderBasic, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = AppStrings.historyLedgerHeaderGross, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = AppStrings.historyLedgerHeaderNet, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = AppStrings.historyLedgerHeaderDsop, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = AppStrings.historyLedgerHeaderTax, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LedgerTableRow(record: LedgerRecordEntity) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimensions.SpacingSmall),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        val cellModifier = Modifier.width(AppDimensions.LedgerCellWidth)
        Text(text = record.dateStr, modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
        Text(text = "₹${formatAmount(record.basicPay)}", modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
        Text(text = "₹${formatAmount(record.grossPay)}", modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
        Text(text = "₹${formatAmount(record.netPay)}", modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
        Text(text = "₹${formatAmount(record.dsopSubscription)}", modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
        Text(text = "₹${formatAmount(record.incomeTax)}", modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
    }
}
