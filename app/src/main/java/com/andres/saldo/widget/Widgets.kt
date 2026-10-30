package com.andres.saldo.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.andres.saldo.ACTION_ADD_EXPENSE
import com.andres.saldo.ACTION_ADD_INCOME
import com.andres.saldo.ACTION_OPEN
import com.andres.saldo.MainActivity
import com.andres.saldo.SaldoApp
import com.andres.saldo.data.MonthSnapshot
import com.andres.saldo.util.Money
import com.andres.saldo.util.monthLabel
import java.time.YearMonth

private val ExpenseColor = ColorProvider(Color(0xFFE5534B))
private val IncomeColor = ColorProvider(Color(0xFF34A853))
private val WarnColor = ColorProvider(Color(0xFFF2A100))

private fun launchIntent(context: Context, action: String): Intent =
    Intent(context, MainActivity::class.java)
        .setAction(action)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

// ----------------------------------------------------------------------------
// Widget 1: resumen del mes + botones
// ----------------------------------------------------------------------------
class SummaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Los recurrentes se generan en SaldoApp.onCreate (que también se ejecuta cuando
        // el sistema arranca la app para refrescar el widget) y al abrir la app.
        val repo = (context.applicationContext as SaldoApp).repository
        val snapshot = repo.monthSnapshot(YearMonth.now())
        provideContent {
            GlanceTheme { SummaryContent(context, snapshot) }
        }
    }
}

class SummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummaryWidget()
}

@Composable
private fun SummaryContent(context: Context, s: MonthSnapshot) {
    val balance = s.income - s.expense
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(launchIntent(context, ACTION_OPEN))),
    ) {
        Text(
            monthLabel(s.month),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
        Text(
            Money.format(s.expense),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold),
        )
        Text(
            "gastado · balance " + (if (balance > 0) "+" else "") + Money.format(balance),
            style = TextStyle(color = if (balance >= 0) IncomeColor else ExpenseColor, fontSize = 12.sp),
            maxLines = 1,
        )
        if (s.budget > 0) {
            val ratio = s.budgetSpent.toFloat() / s.budget
            Spacer(GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                progress = ratio.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = when {
                    ratio > 1f -> ExpenseColor
                    ratio > 0.8f -> WarnColor
                    else -> GlanceTheme.colors.primary
                },
                backgroundColor = GlanceTheme.colors.surfaceVariant,
            )
            Text(
                "Presupuesto: ${Money.format(s.budgetSpent)} de ${Money.format(s.budget)}",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            WidgetButton(context, "− Gasto", ACTION_ADD_EXPENSE, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            WidgetButton(context, "+ Ingreso", ACTION_ADD_INCOME, GlanceModifier.defaultWeight())
        }
    }
}

// ----------------------------------------------------------------------------
// Widget 2: solo botones de añadir (2x1)
// ----------------------------------------------------------------------------
class QuickAddWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(20.dp)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetButton(context, "− Gasto", ACTION_ADD_EXPENSE, GlanceModifier.defaultWeight())
                    Spacer(GlanceModifier.width(8.dp))
                    WidgetButton(context, "+ Ingreso", ACTION_ADD_INCOME, GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}

@Composable
private fun WidgetButton(context: Context, label: String, action: String, modifier: GlanceModifier) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(14.dp)
            .clickable(actionStartActivity(launchIntent(context, action))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 15.sp, fontWeight = FontWeight.Bold),
        )
    }
}
