package com.andres.saldo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andres.saldo.data.Account
import com.andres.saldo.data.Category
import com.andres.saldo.data.Movement
import com.andres.saldo.data.TxType
import com.andres.saldo.ui.theme.AmountStyle
import com.andres.saldo.ui.theme.Brand
import com.andres.saldo.ui.theme.SaldoColors
import com.andres.saldo.util.ES
import com.andres.saldo.util.Money
import com.andres.saldo.util.dayLabel
import com.andres.saldo.util.monthOnlyLabel

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
    val budgetCats = categories.filter { it.type == TxType.EXPENSE && !it.archived && (it.budget ?: 0L) > 0L }
    val budget = budgetCats.sumOf { it.budget ?: 0L }
    val budgetIds = budgetCats.map { it.id }.toSet()
    val budgetSpent = movements.filter { it.type == TxType.EXPENSE && it.categoryId in budgetIds }.sumOf { it.amount }
    val grouped = remember(movements) { movements.groupBy { it.date } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        item { MonthSwitcher(month, vm::prevMonth, vm::nextMonth, vm::currentMonth) }
        item {
            HeroCard(
                title = "Balance de ${monthOnlyLabel(month).lowercase(ES)}",
                income = income,
                expense = expense,
                budget = budget,
                budgetSpent = budgetSpent,
            )
        }

        if (movements.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.Receipt,
                    "Nada registrado este mes",
                    "Pulsa + para apuntar tu primer gasto o ingreso. Tarda tres segundos.",
                )
            }
        }

        grouped.forEach { (day, list) ->
            item(key = "d$day") {
                val net = list.sumOf {
                    when (it.type) { TxType.INCOME -> it.amount; TxType.EXPENSE -> -it.amount; TxType.TRANSFER -> 0L }
                }
                SectionTitle(dayLabel(day)) {
                    if (net != 0L) {
                        Text(
                            (if (net > 0) "+" else "") + Money.format(net),
                            style = AmountStyle.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SaldoCard(padding = 4.dp) {
                    list.forEachIndexed { i, m ->
                        if (i > 0) ThinDivider(Modifier.padding(start = 72.dp, end = 16.dp))
                        MovementRow(m, catMap, accMap, onClick = { onEdit(m) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(title: String, income: Long, expense: Long, budget: Long, budgetSpent: Long) {
    val balance = income - expense
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brand.heroBrush),
    ) {
        // Círculos decorativos (no afectan al tamaño de la tarjeta)
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Brand.Lime.copy(alpha = 0.08f), radius = size.height * 0.85f, center = Offset(size.width * 0.98f, 0f))
            drawCircle(Color.White.copy(alpha = 0.04f), radius = size.height * 0.45f, center = Offset(size.width * 0.78f, size.height * 1.05f))
        }
        Column(Modifier.padding(22.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = Brand.OnHeroMuted)
            Spacer(Modifier.height(6.dp))
            Text(
                (if (balance > 0) "+" else "") + Money.format(balance),
                style = AmountStyle.copy(fontSize = 40.sp),
                color = Brand.OnHero,
                maxLines = 1,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStat(Icons.Rounded.SouthWest, "Ingresos", income, Modifier.weight(1f))
                HeroStat(Icons.Rounded.NorthEast, "Gastos", expense, Modifier.weight(1f))
            }
            if (budget > 0) {
                val ratio = budgetSpent.toFloat() / budget
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Presupuesto", style = MaterialTheme.typography.labelMedium, color = Brand.OnHeroMuted, modifier = Modifier.weight(1f))
                    Text(
                        "${Money.format(budgetSpent)} de ${Money.format(budget)}",
                        style = AmountStyle.copy(fontSize = 12.sp),
                        color = Brand.OnHero,
                    )
                }
                Spacer(Modifier.height(8.dp))
                ProgressBar(
                    ratio,
                    color = when {
                        ratio > 1f -> Color(0xFFFF8A75)
                        ratio > 0.8f -> Color(0xFFFFC56B)
                        else -> Brand.Lime
                    },
                    track = Color.White.copy(alpha = 0.14f),
                    height = 6.dp,
                )
            }
        }
    }
}

@Composable
private fun HeroStat(icon: ImageVector, label: String, value: Long, modifier: Modifier) {
    Row(
        modifier = modifier.clip(MaterialTheme.shapes.medium).background(Color.White.copy(alpha = 0.08f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Brand.Lime), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Brand.Forest, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Brand.OnHeroMuted)
            Text(Money.format(value), style = AmountStyle.copy(fontSize = 15.sp), color = Brand.OnHero, maxLines = 1)
        }
    }
}

@Composable
fun MovementRow(m: Movement, catMap: Map<Long, Category>, accMap: Map<Long, Account>, onClick: () -> Unit) {
    val cat = m.categoryId?.let { catMap[it] }
    val acc = accMap[m.accountId]
    val isTransfer = m.type == TxType.TRANSFER
    val title = if (isTransfer) "Transferencia" else cat?.name ?: "Sin categoría"
    val icon = if (isTransfer) Icons.Rounded.SwapHoriz else AppIcons.category(cat?.icon)
    val color = if (isTransfer) SaldoColors.transfer() else cat?.let { Color(it.color) } ?: UNCATEGORIZED_COLOR
    val subtitle = buildList {
        if (m.note.isNotBlank()) add(m.note)
        if (isTransfer) add("${acc?.name ?: "?"} a ${m.toAccountId?.let { accMap[it]?.name } ?: "?"}")
        else acc?.let { add(it.name) }
    }.joinToString(" · ")
    val amountText = when (m.type) {
        TxType.EXPENSE -> "−" + Money.format(m.amount)
        TxType.INCOME -> "+" + Money.format(m.amount)
        TxType.TRANSFER -> Money.format(m.amount)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, color)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (m.recurringId != null) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.Repeat, contentDescription = "Recurrente", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            amountText,
            style = AmountStyle.copy(fontSize = 16.sp),
            color = if (m.type == TxType.EXPENSE) MaterialTheme.colorScheme.onSurface else SaldoColors.forType(m.type),
        )
    }
}
