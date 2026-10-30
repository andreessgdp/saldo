package com.andres.saldo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andres.saldo.data.Account
import com.andres.saldo.data.Category
import com.andres.saldo.data.Recurring
import com.andres.saldo.data.TxType
import com.andres.saldo.ui.theme.AmountStyle
import com.andres.saldo.ui.theme.SaldoColors
import com.andres.saldo.util.Money
import com.andres.saldo.util.nextOccurrence
import com.andres.saldo.util.occurrenceIn
import com.andres.saldo.util.shortDate
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun RecurringScreen(vm: AppViewModel, onBack: () -> Unit) {
    val recurring by vm.recurring.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val allCategories by vm.allCategories.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val allAccounts by vm.allAccounts.collectAsStateWithLifecycle()
    val catMap = remember(allCategories) { allCategories.associateBy { it.id } }
    val accMap = remember(allAccounts) { allAccounts.associateBy { it.id } }
    var editing by remember { mutableStateOf<Recurring?>(null) }
    var creating by remember { mutableStateOf(false) }

    val active = recurring.filter { it.active }
    val fixedIncome = active.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val fixedExpense = active.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SubScreenHeader("Recurrentes", onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { creating = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) { Icon(Icons.Rounded.Add, "Nuevo recurrente") }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 110.dp)) {
            if (recurring.isEmpty()) {
                item { EmptyState(Icons.Rounded.Repeat, "Sin recurrentes", "Añade tu nómina, el renting, el gimnasio o tus suscripciones y se apuntarán solos cada mes.") }
            } else {
                item {
                    SaldoCard(modifier = Modifier.padding(top = 8.dp)) {
                        Row {
                            Column(Modifier.weight(1f)) {
                                MiniLabel("Ingresos fijos")
                                Text(Money.format(fixedIncome), style = AmountStyle.copy(fontSize = 20.sp), color = SaldoColors.income())
                            }
                            Column(Modifier.weight(1f)) {
                                MiniLabel("Gastos fijos")
                                Text(Money.format(fixedExpense), style = AmountStyle.copy(fontSize = 20.sp), color = SaldoColors.expense())
                            }
                            Column(Modifier.weight(1f)) {
                                MiniLabel("Margen")
                                Text(Money.format(fixedIncome - fixedExpense), style = AmountStyle.copy(fontSize = 20.sp))
                            }
                        }
                    }
                }
                item {
                    SectionTitle("Cada mes")
                    SaldoCard(padding = 4.dp) {
                        recurring.forEachIndexed { i, r ->
                            if (i > 0) ThinDivider(Modifier.padding(start = 72.dp, end = 16.dp))
                            RecurringRow(r, catMap, accMap, onClick = { editing = r }) { on ->
                                // Al reactivar, no rellenar meses atrasados: siguiente desde hoy.
                                val next = if (on) firstOccurrence(r.dayOfMonth, includeThisMonth = false) else r.nextDate
                                vm.saveRecurring(r.copy(active = on, nextDate = next))
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Se apuntan solos el día que toca (al abrir la app o cuando se refresca el widget).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                )
            }
        }
    }

    if (creating) {
        RecurringDialog(null, categories, accounts, onDismiss = { creating = false }, onSave = { vm.saveRecurring(it); creating = false }, onDelete = null)
    }
    editing?.let { r ->
        RecurringDialog(
            r, categories, accounts,
            onDismiss = { editing = null },
            onSave = { vm.saveRecurring(it); editing = null },
            onDelete = { vm.deleteRecurring(r); editing = null },
        )
    }
}

@Composable
private fun RecurringRow(
    r: Recurring,
    catMap: Map<Long, Category>,
    accMap: Map<Long, Account>,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val cat = r.categoryId?.let { catMap[it] }
    val isTransfer = r.type == TxType.TRANSFER
    val title = if (isTransfer) {
        "${accMap[r.accountId]?.name ?: "?"} a ${r.toAccountId?.let { accMap[it]?.name } ?: "?"}"
    } else {
        r.note.ifBlank { cat?.name ?: "Sin categoría" }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .alpha(if (r.active) 1f else 0.55f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(
            if (isTransfer) Icons.Rounded.SwapHoriz else AppIcons.category(cat?.icon),
            if (isTransfer) SaldoColors.transfer() else cat?.let { Color(it.color) } ?: UNCATEGORIZED_COLOR,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                "Día ${r.dayOfMonth} · " + if (r.active) "próximo ${shortDate(r.nextDate)}" else "pausado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            (if (r.type == TxType.INCOME) "+" else if (r.type == TxType.EXPENSE) "−" else "") + Money.format(r.amount),
            style = AmountStyle.copy(fontSize = 15.sp),
            color = if (r.type == TxType.EXPENSE) MaterialTheme.colorScheme.onSurface else SaldoColors.forType(r.type),
        )
        Spacer(Modifier.width(8.dp))
        Switch(checked = r.active, onCheckedChange = onToggle)
    }
}

private fun firstOccurrence(day: Int, includeThisMonth: Boolean): Long {
    val today = LocalDate.now()
    val thisMonth = occurrenceIn(YearMonth.from(today), day)
    return if (!includeThisMonth && thisMonth.isBefore(today)) nextOccurrence(thisMonth, day).toEpochDay()
    else thisMonth.toEpochDay()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurringDialog(
    initial: Recurring?,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (Recurring) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var type by remember { mutableStateOf(initial?.type ?: TxType.EXPENSE) }
    var amount by remember { mutableStateOf(initial?.let { Money.toInput(it.amount) } ?: "") }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var accountId by remember { mutableStateOf(initial?.accountId ?: accounts.firstOrNull()?.id) }
    var toAccountId by remember { mutableStateOf(initial?.toAccountId) }
    var day by remember { mutableStateOf(initial?.dayOfMonth?.toString() ?: LocalDate.now().dayOfMonth.toString()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var includeThisMonth by remember { mutableStateOf(true) }

    val dayInt = day.toIntOrNull()?.takeIf { it in 1..31 }
    val cents = Money.parse(amount) ?: 0L
    val typeCats = categories.filter { it.type == type }
    val valid = cents > 0 && dayInt != null && accountId != null && when (type) {
        TxType.TRANSFER -> toAccountId != null && toAccountId != accountId
        else -> typeCats.any { it.id == categoryId }
    }
    val today = LocalDate.now()
    val thisMonthPassed = dayInt != null && occurrenceIn(YearMonth.from(today), dayInt).isBefore(today)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(if (initial == null) "Nuevo recurrente" else "Editar recurrente", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PillSelector(
                    listOf(TxType.EXPENSE to "Gasto", TxType.INCOME to "Ingreso", TxType.TRANSFER to "Transf."),
                    type,
                    { t -> if (type != t) { type = t; categoryId = null } },
                    Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount, onValueChange = { amount = it },
                        label = { Text("Importe €") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = day, onValueChange = { day = it.filter(Char::isDigit).take(2) },
                        label = { Text("Día") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.6f), shape = MaterialTheme.shapes.medium,
                        isError = dayInt == null,
                    )
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Concepto (ej. Nómina, Netflix)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                )
                if (type == TxType.TRANSFER) {
                    MiniLabel("Desde")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        accounts.forEach { a -> SaldoChip(a.name, accountId == a.id, { accountId = a.id }, AppIcons.account(a.icon)) }
                    }
                    MiniLabel("Hacia")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        accounts.filter { it.id != accountId }.forEach { a -> SaldoChip(a.name, toAccountId == a.id, { toAccountId = a.id }, AppIcons.account(a.icon)) }
                    }
                } else {
                    MiniLabel("Categoría")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        typeCats.forEach { c -> SaldoChip(c.name, categoryId == c.id, { categoryId = c.id }, AppIcons.category(c.icon)) }
                    }
                    if (accounts.size > 1) {
                        MiniLabel("Cuenta")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            accounts.forEach { a -> SaldoChip(a.name, accountId == a.id, { accountId = a.id }, AppIcons.account(a.icon)) }
                        }
                    }
                }
                if (initial == null && thisMonthPassed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeThisMonth, onCheckedChange = { includeThisMonth = it })
                        Text("Apuntar ya el de este mes (el día $day ya ha pasado)", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Eliminar recurrente", color = MaterialTheme.colorScheme.error) }
                    Text("Los movimientos ya creados se mantienen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val d = dayInt ?: return@TextButton
                val acc = accountId ?: return@TextButton
                val nextDate = if (initial == null) {
                    firstOccurrence(d, includeThisMonth)
                } else {
                    occurrenceIn(YearMonth.from(LocalDate.ofEpochDay(initial.nextDate)), d).toEpochDay()
                }
                onSave(
                    Recurring(
                        id = initial?.id ?: 0,
                        amount = cents,
                        type = type,
                        categoryId = if (type == TxType.TRANSFER) null else categoryId,
                        accountId = acc,
                        toAccountId = if (type == TxType.TRANSFER) toAccountId else null,
                        dayOfMonth = d,
                        note = note.trim(),
                        nextDate = nextDate,
                        active = initial?.active ?: true,
                    )
                )
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
