package com.jamsell.gethics.finance.presentation.finance_summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.jamsell.gethics.R
import com.jamsell.gethics.finance.data.FinancialSummaryResponse
import com.jamsell.gethics.finance.data.FinancialTransactionType
import com.jamsell.gethics.finance.data.TransactionResponse
import com.jamsell.gethics.shared.ui.components.ErrorMessage
import com.jamsell.gethics.shared.ui.components.GethicsCard
import com.jamsell.gethics.shared.ui.components.LoadingBox
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IncomeColor = Color(0xFF2E7D32)
private val ExpenseColor = Color(0xFFC62828)

@Composable
fun FinanceSummaryScreen(
    viewModel: FinanceSummaryViewModel,
    onRegisterTransaction: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state = viewModel.state.value
    val period = viewModel.period.value
    val summary = state.data

    // La pantalla vive en el back stack: al volver del registro hay que refrescar para ver el movimiento nuevo.
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.finance_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_open))
                }
            }

            PeriodSelector(
                period = period,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onToggleAll = viewModel::toggleAll
            )

            when {
                state.isLoading -> LoadingBox()
                summary != null -> SummaryContent(summary)
                state.message.isNotEmpty() -> ErrorMessage(message = state.message, onRetry = viewModel::load)
                else -> LoadingBox()
            }
        }

        FloatingActionButton(
            onClick = onRegisterTransaction,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.finance_register))
        }
    }
}

@Composable
private fun PeriodSelector(period: YearMonth?, onPrevious: () -> Unit, onNext: () -> Unit, onToggleAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (period != null) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }
            Text(
                text = period.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        } else {
            Text(
                text = stringResource(R.string.finance_all_history),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onToggleAll) {
                Text(stringResource(if (period != null) R.string.finance_see_all else R.string.finance_by_month))
            }
        }
    }
}

@Composable
private fun SummaryContent(summary: FinancialSummaryResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GethicsCard {
                Text(stringResource(R.string.finance_balance), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = formatMoney(summary.balance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.balance < 0) ExpenseColor else IncomeColor
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    TotalCard(stringResource(R.string.finance_income), summary.totalIncome, IncomeColor)
                }
                Box(modifier = Modifier.weight(1f)) {
                    TotalCard(stringResource(R.string.finance_expense), summary.totalExpense, ExpenseColor)
                }
            }
        }
        if (summary.transactions.isEmpty()) {
            item {
                Text(
                    text = summary.message ?: stringResource(R.string.finance_empty),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(summary.transactions, key = { it.id }) { TransactionRow(it) }
        }
    }
}

@Composable
private fun TotalCard(label: String, amount: Double, color: Color) {
    GethicsCard {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = formatMoney(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun TransactionRow(transaction: TransactionResponse) {
    val isIncome = transaction.type == FinancialTransactionType.INCOME
    GethicsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category, fontWeight = FontWeight.SemiBold)
                val detail = listOfNotNull(transaction.date, transaction.description?.takeIf { it.isNotBlank() })
                Text(detail.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = (if (isIncome) "+ " else "- ") + formatMoney(transaction.amount),
                fontWeight = FontWeight.Bold,
                color = if (isIncome) IncomeColor else ExpenseColor
            )
        }
    }
}
