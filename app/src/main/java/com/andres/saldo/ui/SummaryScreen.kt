package com.andres.saldo.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andres.saldo.data.Category
import com.andres.saldo.data.Movement
import com.andres.saldo.data.TxType
import com.andres.saldo.ui.theme.AmountStyle
import com.andres.saldo.ui.theme.SaldoColors
import com.andres.saldo.util.ES
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
    val income = movements.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val byCategory = remember(expenses, catMap) {
        expenses.groupBy { it.categoryId }
            .map { (id, list) -> CatTotal(id?.let { catMap[it] }, list.sumOf { it.amount }) }
            .sortedByDescending { it.total }
    }
    val bars = remember(history, month) { buildBars(history, month) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
        MonthSwitcher(month, vm::prevMonth, vm::nextMonth, vm::currentMonth)

        // ---- Reparto por categoría ----
        SectionTitle("En qué se va el dinero")
        SaldoCard {
            if (byCategory.isEmpty()) {
                EmptyState(Icons.Rounded.DonutLarge, "Sin gastos este mes", "Cuando registres gastos verás aquí el reparto por categoría.")
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Donut(
                        slices = byCategory.map { colorOf(it.category) to it.total },
                        top = "Gastado",
                        bottom = Money.format(totalExpense),
                    )
                }
                Spacer(Modifier.height(20.dp))
                val max = byCategory.first().total.coerceAtLeast(1L)
                byCategory.forEachIndexed { i, ct ->
                    if (i > 0) Spacer(Modifier.height(14.dp))
                    val pct = if (totalExpense > 0) ct.total * 100.0 / totalExpense else 0.0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(AppIcons.category(ct.category?.icon), colorOf(ct.category), size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    ct.category?.name ?: "Sin categoría",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(Money.format(ct.total), style = AmountStyle.copy(fontSize = 14.sp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProgressBar(ct.total.toFloat() / max, colorOf(ct.category), Modifier.weight(1f), height = 5.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "%.0f%%".format(pct),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(34.dp),
                                    textAlign = TextAlign.End,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- Presupuestos ----
        val budgeted = categories.filter { it.type == TxType.EXPENSE && !it.archived && (it.budget ?: 0L) > 0L }
        SectionTitle("Presupuestos")
        SaldoCard {
            if (budgeted.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Flag, MaterialTheme.colorScheme.primary, size = 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "Ponle un límite mensual a las categorías que quieras vigilar en Ajustes, Categorías.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val totalBudget = budgeted.sumOf { it.budget ?: 0L }
                val totalSpent = budgeted.sumOf { c -> expenses.filter { it.categoryId == c.id }.sumOf { it.amount } }
                BudgetRow(null, "Total", totalSpent, totalBudget, highlight = true)
                budgeted.forEach { c ->
                    Spacer(Modifier.height(16.dp))
                    BudgetRow(c, c.name, expenses.filter { it.categoryId == c.id }.sumOf { it.amount }, c.budget ?: 0L)
                }
            }
        }

        // ---- Evolución ----
        SectionTitle("Últimos 6 meses")
        SaldoCard {
            MonthBars(bars, SaldoColors.income(), SaldoColors.expense())
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                LegendDot(SaldoColors.income(), "Ingresos")
                LegendDot(SaldoColors.expense(), "Gastos")
            }
            val prev = bars.getOrNull(bars.size - 2)
            val current = bars.lastOrNull()
            if (prev != null && current != null && prev.expense > 0) {
                val diff = current.expense - prev.expense
                val pct = abs(diff) * 100.0 / prev.expense
                val prevName = monthOnlyLabel(prev.month).lowercase(ES)
                Spacer(Modifier.height(16.dp))
                ThinDivider()
                Spacer(Modifier.height(14.dp))
                InsightLine(
                    when {
                        diff > 0 -> "Gastas ${Money.format(diff)} más que en $prevName"
                        diff < 0 -> "Gastas ${Money.format(-diff)} menos que en $prevName"
                        else -> "Mismo gasto que en $prevName"
                    },
                    if (diff == 0L) "" else (if (diff > 0) "+" else "−") + "%.0f%%".format(pct),
                    if (diff > 0) SaldoColors.expense() else SaldoColors.income(),
                )
            }
            val withData = bars.filter { it.expense > 0 || it.income > 0 }
            if (withData.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                InsightLine("Gasto medio al mes", Money.format(withData.sumOf { it.expense } / withData.size), MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(10.dp))
                InsightLine("Ahorro medio al mes", Money.format(withData.sumOf { it.income - it.expense } / withData.size), MaterialTheme.colorScheme.onSurface)
            }
        }

        // ---- Tasa de ahorro ----
        if (income > 0) {
            val rate = ((income - totalExpense) * 100.0 / income)
            SectionTitle("Tasa de ahorro")
            SaldoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(
                        progress = (rate / 100.0).toFloat().coerceIn(0f, 1f),
                        color = if (rate >= 0) SaldoColors.income() else SaldoColors.expense(),
                        label = "%.0f%%".format(rate),
                    )
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text(
                            if (rate >= 20) "Vas muy bien" else if (rate >= 0) "En positivo" else "Gastas más de lo que entra",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "De cada 100 € que entran este mes, te quedan ${"%.0f".format(rate.coerceAtLeast(0.0))} €.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun colorOf(c: Category?): Color = c?.let { Color(it.color) } ?: UNCATEGORIZED_COLOR

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
private fun InsightLine(text: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, style = AmountStyle.copy(fontSize = 15.sp), color = valueColor)
    }
}

@Composable
private fun BudgetRow(category: Category?, label: String, spent: Long, budget: Long, highlight: Boolean = false) {
    val ratio = if (budget > 0) spent.toFloat() / budget else 0f
    val color = when {
        ratio > 1f -> SaldoColors.expense()
        ratio > 0.8f -> SaldoColors.warning()
        category != null -> Color(category.color)
        else -> MaterialTheme.colorScheme.primary
    }
    val left = budget - spent
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (category != null) {
                IconTile(AppIcons.category(category.icon), Color(category.color), size = 32.dp)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall)
                Text(
                    if (left >= 0) "Quedan ${Money.format(left)}" else "Te has pasado ${Money.format(-left)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (left >= 0) MaterialTheme.colorScheme.onSurfaceVariant else SaldoColors.expense(),
                )
            }
            Text(
                "${Money.format(spent)} / ${Money.format(budget)}",
                style = AmountStyle.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(ratio, color, height = if (highlight) 10.dp else 7.dp)
    }
}

@Composable
private fun Donut(slices: List<Pair<Color, Long>>, top: String, bottom: String) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1L)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val anim = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.11f
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            var start = -90f
            val gap = if (slices.size > 1) 3f else 0f
            slices.forEach { (color, value) ->
                val sweep = 360f * value / total * anim.value
                if (sweep > gap) {
                    drawArc(
                        color = color,
                        startAngle = start + gap / 2,
                        sweepAngle = sweep - gap,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(top, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(bottom, style = AmountStyle.copy(fontSize = 24.sp))
        }
    }
}

@Composable
private fun Ring(progress: Float, color: Color, label: String) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) { anim.animateTo(progress, tween(900, easing = FastOutSlowInEasing)) }
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val tl = Offset(stroke / 2, stroke / 2)
            val s = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, topLeft = tl, size = s, style = Stroke(stroke))
            drawArc(color, -90f, 360f * anim.value, false, topLeft = tl, size = s, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(label, style = AmountStyle.copy(fontSize = 18.sp))
    }
}

@Composable
private fun MonthBars(data: List<MonthBar>, incomeColor: Color, expenseColor: Color) {
    val max = (data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L).coerceAtLeast(1L)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val anim = remember { Animatable(0f) }
    LaunchedEffect(data) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    Column {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            if (data.isEmpty()) return@Canvas
            // Guías horizontales punteadas
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
            for (i in 1..3) {
                val y = size.height * i / 4f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f, pathEffect = dash)
            }
            val groupW = size.width / data.size
            val barW = groupW * 0.26f
            val r = CornerRadius(barW / 2, barW / 2)
            data.forEachIndexed { i, d ->
                val x0 = i * groupW + groupW * 0.2f
                val hI = size.height * (d.income.toFloat() / max) * anim.value
                val hE = size.height * (d.expense.toFloat() / max) * anim.value
                val alpha = if (i == data.lastIndex) 1f else 0.55f
                if (hI > 0f) drawRoundRect(incomeColor.copy(alpha = alpha), Offset(x0, size.height - hI), Size(barW, hI), r)
                if (hE > 0f) drawRoundRect(expenseColor.copy(alpha = alpha), Offset(x0 + barW + groupW * 0.08f, size.height - hE), Size(barW, hE), r)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            data.forEachIndexed { i, it ->
                Text(
                    shortMonth(it.month),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == data.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
