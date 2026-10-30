package com.andres.saldo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andres.saldo.data.Account
import com.andres.saldo.data.Category
import com.andres.saldo.data.Movement
import com.andres.saldo.data.TxType
import com.andres.saldo.ui.theme.SaldoColors
import com.andres.saldo.util.Money
import com.andres.saldo.util.dayLabel

@Composable
fun MovementsScreen(vm: AppViewModel, onEdit: (Movement) -> Unit) {
    val month by vm.month.collectAsStateWithLifecycle()
    val movements by vm.monthMovements.collectAsStateWithLifecycle()
    val categories by vm.allCategories.collectAsStateWithLifecycle()
    val accounts by vm.allAccounts.collectAsStateWithLifecycle()
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val accMap = remember(accounts) { accounts.associateBy { it.id } }

    val income = movements.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = movements.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val grouped = remember(movements) { movements.groupBy { it.date } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item { MonthSwitcher(month, vm::prevMonth, vm::nextMonth, vm::currentMonth) }
        item { TotalsCard(income, expense) }

        if (movements.isEmpty()) {
            item {
                EmptyState("🧾", "Nada registrado este mes", "Pulsa + para apuntar tu primer gasto o ingreso.")
            }
        }

        grouped.forEach { (day, list) ->
            item(key = "h$day") {
                val net = list.sumOf {
                    when (it.type) { TxType.INCOME -> it.amount; TxType.EXPENSE -> -it.amount; TxType.TRANSFER -> 0L }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dayLabel(day), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if (net != 0L) {
                        Text(
                            (if (net > 0) "+" else "") + Money.format(net),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            items(list, key = { it.id }) { m ->
                MovementRow(m, catMap, accMap, onClick = { onEdit(m) })
            }
        }
    }
}

@Composable
private fun TotalsCard(income: Long, expense: Long) {
    val balance = income - expense
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TotalCell("Ingresos", Money.format(income), SaldoColors.income())
            TotalCell("Gastos", Money.format(expense), SaldoColors.expense())
            TotalCell(
                "Balance",
                (if (balance > 0) "+" else "") + Money.format(balance),
                if (balance >= 0) SaldoColors.income() else SaldoColors.expense(),
            )
        }
    }
}

@Composable
private fun TotalCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
fun MovementRow(m: Movement, catMap: Map<Long, Category>, accMap: Map<Long, Account>, onClick: () -> Unit) {
    val cat = m.categoryId?.let { catMap[it] }
    val acc = accMap[m.accountId]
    val title: String
    val emoji: String
    val color: Color
    val subtitleParts = mutableListOf<String>()
    if (m.type == TxType.TRANSFER) {
        title = "Transferencia"
        emoji = "🔄"
        color = MaterialTheme.colorScheme.primary
        subtitleParts += "${acc?.name ?: "?"} → ${m.toAccountId?.let { accMap[it]?.name } ?: "?"}"
    } else {
        title = cat?.name ?: "Sin categoría"
        emoji = cat?.emoji ?: "❔"
        color = cat?.let { Color(it.color) } ?: UNCATEGORIZED_COLOR
        acc?.let { subtitleParts += "${it.emoji} ${it.name}" }
    }
    if (m.note.isNotBlank()) subtitleParts.add(0, m.note)
    if (m.recurringId != null) subtitleParts += "🔁"

    val amountText = when (m.type) {
        TxType.EXPENSE -> "-" + Money.format(m.amount)
        TxType.INCOME -> "+" + Money.format(m.amount)
        TxType.TRANSFER -> Money.format(m.amount)
    }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { EmojiBadge(emoji, color) },
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(subtitleParts.joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Text(amountText, fontWeight = FontWeight.SemiBold, color = SaldoColors.forType(m.type), style = MaterialTheme.typography.bodyLarge)
        },
    )
}
