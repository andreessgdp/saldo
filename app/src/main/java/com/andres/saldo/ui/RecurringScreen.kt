package com.andres.saldo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andres.saldo.data.Account
import com.andres.saldo.data.Category
import com.andres.saldo.data.Recurring
import com.andres.saldo.data.TxType
import com.andres.saldo.ui.theme.SaldoColors
import com.andres.saldo.util.Money
import com.andres.saldo.util.nextOccurrence
import com.andres.saldo.util.occurrenceIn
import com.andres.saldo.util.shortDate
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
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

    val monthlyNet = recurring.filter { it.active }.sumOf {
        when (it.type) { TxType.INCOME -> it.amount; TxType.EXPENSE -> -it.amount; TxType.TRANSFER -> 0L }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurrentes") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, "Nuevo recurrente") }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Text(
                    "Se apuntan solos el día que toca cada mes (al abrir la app o al refrescarse el widget).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (recurring.isEmpty()) {
                item { EmptyState("🔁", "Sin recurrentes", "Añade tu nómina, el renting, el gimnasio o tus suscripciones.") }
            } else {
                item {
                    Text(
                        "Neto fijo mensual: " + (if (monthlyNet > 0) "+" else "") + Money.format(monthlyNet),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            items(recurring, key = { it.id }) { r ->
                val cat = r.categoryId?.let { catMap[it] }
                val title = if (r.type == TxType.TRANSFER) {
                    "${accMap[r.accountId]?.name ?: "?"} → ${r.toAccountId?.let { accMap[it]?.name } ?: "?"}"
                } else {
                    r.note.ifBlank { cat?.name ?: "Sin categoría" }
                }
                ListItem(
                    modifier = Modifier.clickable { editing = r },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        EmojiBadge(
                            if (r.type == TxType.TRANSFER) "🔄" else cat?.emoji ?: "❔",
                            cat?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = { Text(title) },
                    supportingContent = {
                        Text(
                            "Día ${r.dayOfMonth} · " +
                                (if (r.active) "próximo ${shortDate(r.nextDate)}" else "pausado") +
                                " · " + Money.format(r.amount),
                            color = if (r.active) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Switch(checked = r.active, onCheckedChange = { on ->
                            // Al reactivar, no rellenar meses atrasados: siguiente desde hoy.
                            val next = if (on) firstOccurrence(r.dayOfMonth, includeThisMonth = false) else r.nextDate
                            vm.saveRecurring(r.copy(active = on, nextDate = next))
                        })
                    },
                )
            }
        }
    }

    if (creating) {
        RecurringDialog(
            null, categories, accounts,
            onDismiss = { creating = false },
            onSave = { vm.saveRecurring(it); creating = false },
            onDelete = null,
        )
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
        title = { Text(if (initial == null) "Nuevo recurrente" else "Editar recurrente") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(TxType.EXPENSE to "Gasto", TxType.INCOME to "Ingreso", TxType.TRANSFER to "Transf.").forEach { (t, l) ->
                        FilterChip(selected = type == t, onClick = { if (type != t) { type = t; categoryId = null } }, label = { Text(l) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount, onValueChange = { amount = it },
                        label = { Text("Importe €") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = day, onValueChange = { day = it.filter(Char::isDigit).take(2) },
                        label = { Text("Día") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.6f),
                        isError = dayInt == null,
                    )
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Concepto (ej. Nómina Deloitte, Netflix)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == TxType.TRANSFER) {
                    Text("Desde", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        accounts.forEach { a -> FilterChip(selected = accountId == a.id, onClick = { accountId = a.id }, label = { Text("${a.emoji} ${a.name}") }) }
                    }
                    Text("Hacia", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        accounts.filter { it.id != accountId }.forEach { a -> FilterChip(selected = toAccountId == a.id, onClick = { toAccountId = a.id }, label = { Text("${a.emoji} ${a.name}") }) }
                    }
                } else {
                    Text("Categoría", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        typeCats.forEach { c -> FilterChip(selected = categoryId == c.id, onClick = { categoryId = c.id }, label = { Text("${c.emoji} ${c.name}") }) }
                    }
                    if (accounts.size > 1) {
                        Text("Cuenta", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            accounts.forEach { a -> FilterChip(selected = accountId == a.id, onClick = { accountId = a.id }, label = { Text("${a.emoji} ${a.name}") }) }
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
                    TextButton(onClick = onDelete) { Text("Eliminar recurrente", color = SaldoColors.expense()) }
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
                    // Mismo mes pendiente, con el nuevo día.
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
