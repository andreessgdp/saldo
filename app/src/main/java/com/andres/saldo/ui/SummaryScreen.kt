package com.andres.saldo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andres.saldo.data.Category
import com.andres.saldo.data.Movement
import com.andres.saldo.data.TxType
import com.andres.saldo.ui.theme.SaldoColors
import com.andres.saldo.util.Money
import com.andres.saldo.util.monthOnlyLabel
import com.andres.saldo.util.shortMonth
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

private data class CatTotal(val category: Category?, val total: Long)
private data class MonthBar(val month: YearMonth, val income: Long, val expense: Long)

@Composable
fun SummaryScreen(vm: AppViewModel) {
    val month by vm.month.collectAsStateWithLifecycle()
    val movements by vm.monthMovements.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val categories by vm.allCategories.collectAsStateWithLifecycle()
    val catMap = remember(categories) { categories.associateBy { it.id } }

    val expenses = movements.filter { it.type == TxType.EXPENSE }
    val totalExpense = expenses.sumOf { it.amount }
    val byCategory = remember(expenses, catMap) {
        expenses.groupBy { it.categoryId }
            .map { (id, list) -> CatTotal(id?.let { catMap[it] }, list.sumOf { it.amount }) }
            .sortedByDescending { it.total }
    }
    val bars = remember(history, month) { buildBars(history, month) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp),
    ) {
        MonthSwitcher(month, vm::prevMonth, vm::nextMonth, vm::currentMonth)

        // ---- Gasto por categoría ----
        SectionTitle("¿En qué se va el dinero?")
        ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            if (byCategory.isEmpty()) {
                EmptyState("📊", "Sin gastos este mes", "Cuando registres gastos verás aquí el reparto por categoría.")
            } else {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Donut(
                        slices = byCategory.map { (it.category?.let { c -> Color(c.color) } ?: UNCATEGORIZED_COLOR) to it.total },
                        top = "Gastado",
                        bottom = Money.format(totalExpense),
                    )
                    Spacer(Modifier.height(16.dp))
                    byCategory.forEach { ct ->
                        val pct = if (totalExpense > 0) ct.total * 100.0 / totalExpense else 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(ct.category?.let { Color(it.color) } ?: UNCATEGORIZED_COLOR))
                            Spacer(Modifier.width(10.dp))
                            Text("${ct.category?.emoji ?: "❔"} ${ct.category?.name ?: "Sin categoría"}", modifier = Modifier.weight(1f))
                            Text("%.0f%%".format(pct), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(12.dp))
                            Text(Money.format(ct.total), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ---- Presupuestos ----
        val budgeted = categories.filter { it.type == TxType.EXPENSE && !it.archived && (it.budget ?: 0L) > 0L }
        SectionTitle("Presupuestos")
        ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            if (budgeted.isEmpty()) {
                Text(
                    "Aún no tienes presupuestos. Ponle un límite mensual a una categoría en Ajustes → Categorías.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val totalBudget = budgeted.sumOf { it.budget ?: 0L }
                    val totalSpent = budgeted.sumOf { c -> expenses.filter { it.categoryId == c.id }.sumOf { it.amount } }
                    BudgetRow("🎯 Total con presupuesto", totalSpent, totalBudget, bold = true)
                    budgeted.forEach { c ->
                        val spent = expenses.filter { it.categoryId == c.id }.sumOf { it.amount }
                        BudgetRow("${c.emoji} ${c.name}", spent, c.budget ?: 0L)
                    }
                }
            }
        }

        // ---- Evolución ----
        SectionTitle("Últimos 6 meses")
        ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                MonthBars(bars, SaldoColors.income(), SaldoColors.expense())
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(SaldoColors.income(), "Ingresos")
                    LegendDot(SaldoColors.expense(), "Gastos")
                }
                Spacer(Modifier.height(12.dp))
                val prev = bars.getOrNull(bars.size - 2)
                val current = bars.lastOrNull()
                if (prev != null && current != null && prev.expense > 0) {
                    val diff = current.expense - prev.expense
                    val pct = abs(diff) * 100.0 / prev.expense
                    val text = when {
                        diff > 0 -> "Llevas ${Money.format(diff)} más de gasto que en ${monthOnlyLabel(prev.month).lowercase()} (+%.0f%%)".format(pct)
                        diff < 0 -> "Llevas ${Money.format(-diff)} menos de gasto que en ${monthOnlyLabel(prev.month).lowercase()} (−%.0f%%)".format(pct)
                        else -> "Mismo gasto que en ${monthOnlyLabel(prev.month).lowercase()}"
                    }
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
                val withData = bars.filter { it.expense > 0 || it.income > 0 }
                if (withData.isNotEmpty()) {
                    val avgExpense = withData.sumOf { it.expense } / withData.size
                    val avgSaving = withData.sumOf { it.income - it.expense } / withData.size
                    Text(
                        "Media mensual: gastas ${Money.format(avgExpense)} y ahorras ${Money.format(avgSaving)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        // ---- Ahorro del mes ----
        val income = movements.filter { it.type == TxType.INCOME }.sumOf { it.amount }
        if (income > 0) {
            SectionTitle("Tasa de ahorro")
            ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val rate = (income - totalExpense) * 100.0 / income
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "%.0f%%".format(rate),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (rate >= 0) SaldoColors.income() else SaldoColors.expense(),
                    )
                    Text(
                        "de lo que has ingresado este mes se queda sin gastar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun buildBars(history: List<Movement>, month: YearMonth): List<MonthBar> {
    val byMonth = history.groupBy { YearMonth.from(LocalDate.ofEpochDay(it.date)) }
    return (5 downTo 0).map { back ->
        val ym = month.minusMonths(back.toLong())
        val list = byMonth[ym].orEmpty()
        MonthBar(
            ym,
            income = list.filter { it.type == TxType.INCOME }.sumOf { it.amount },
            expense = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
        )
    }
}

@Composable
private fun BudgetRow(label: String, spent: Long, budget: Long, bold: Boolean = false) {
    val ratio = if (budget > 0) spent.toFloat() / budget else 0f
    val color = when {
        ratio > 1f -> SaldoColors.expense()
        ratio > 0.8f -> SaldoColors.warning()
        else -> MaterialTheme.colorScheme.primary
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
            Text("${Money.format(spent)} / ${Money.format(budget)}", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
        )
        val left = budget - spent
        Text(
            if (left >= 0) "Te quedan ${Money.format(left)}" else "Te has pasado ${Money.format(-left)}",
            style = MaterialTheme.typography.labelSmall,
            color = if (left >= 0) MaterialTheme.colorScheme.onSurfaceVariant else SaldoColors.expense(),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun Donut(slices: List<Pair<Color, Long>>, top: String, bottom: String) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1L)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.15f
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            var start = -90f
            val gap = if (slices.size > 1) 1.5f else 0f
            slices.forEach { (color, value) ->
                val sweep = 360f * value / total
                drawArc(
                    color = color,
                    startAngle = start + gap / 2,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(top, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(bottom, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MonthBars(data: List<MonthBar>, incomeColor: Color, expenseColor: Color) {
    val max = (data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L).coerceAtLeast(1L)
    Column {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            if (data.isEmpty()) return@Canvas
            val groupW = size.width / data.size
            val barW = groupW * 0.3f
            data.forEachIndexed { i, d ->
                val x0 = i * groupW + groupW * 0.15f
                val hI = size.height * (d.income.toFloat() / max)
                val hE = size.height * (d.expense.toFloat() / max)
                if (hI > 0f) drawRoundRect(incomeColor, Offset(x0, size.height - hI), Size(barW, hI), CornerRadius(6f, 6f))
                if (hE > 0f) drawRoundRect(expenseColor, Offset(x0 + barW + groupW * 0.1f, size.height - hE), Size(barW, hE), CornerRadius(6f, 6f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            data.forEach {
                Text(shortMonth(it.month), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
