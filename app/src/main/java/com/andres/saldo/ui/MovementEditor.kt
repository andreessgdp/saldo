package com.andres.saldo.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andres.saldo.data.Movement
import com.andres.saldo.data.TxType
import com.andres.saldo.ui.theme.SaldoColors
import com.andres.saldo.util.Money
import com.andres.saldo.util.shortDate
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MovementEditor(
    vm: AppViewModel,
    initial: Movement?,
    initialType: TxType,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var type by rememberSaveable { mutableStateOf(initial?.type ?: initialType) }
    var amountText by rememberSaveable { mutableStateOf(initial?.let { Money.toInput(it.amount) } ?: "") }
    var categoryId by rememberSaveable { mutableStateOf(initial?.categoryId) }
    var accountId by rememberSaveable { mutableStateOf(initial?.accountId) }
    var toAccountId by rememberSaveable { mutableStateOf(initial?.toAccountId) }
    var date by rememberSaveable { mutableLongStateOf(initial?.date ?: LocalDate.now().toEpochDay()) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (accountId == null && accounts.isNotEmpty()) accountId = accounts.first().id
        if (toAccountId == null && accounts.size > 1) toAccountId = accounts.firstOrNull { it.id != accountId }?.id
    }

    val typeCategories = categories.filter { it.type == type }
    val amount = Money.parse(amountText) ?: 0L
    val canSave = amount > 0 && accountId != null && when (type) {
        TxType.TRANSFER -> toAccountId != null && toAccountId != accountId
        else -> categoryId != null && typeCategories.any { it.id == categoryId }
    }
    val today = LocalDate.now().toEpochDay()

    fun save() {
        val accId = accountId ?: return
        vm.saveMovement(
            Movement(
                id = initial?.id ?: 0,
                amount = amount,
                type = type,
                categoryId = if (type == TxType.TRANSFER) null else categoryId,
                accountId = accId,
                toAccountId = if (type == TxType.TRANSFER) toAccountId else null,
                date = date,
                note = note.trim(),
                recurringId = initial?.recurringId,
                createdAt = initial?.createdAt ?: System.currentTimeMillis(),
            )
        )
        onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Nuevo movimiento" else "Editar movimiento") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") }
                },
                actions = {
                    if (initial != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Borrar")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TxType.EXPENSE to "Gasto", TxType.INCOME to "Ingreso", TxType.TRANSFER to "Transferencia").forEach { (t, label) ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                if (type != t) {
                                    type = t
                                    categoryId = null
                                }
                            },
                            label = { Text(label) },
                        )
                    }
                }

                Text(
                    text = (if (amountText.isEmpty()) "0" else amountText) + " €",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = SaldoColors.forType(type),
                )

                if (type == TxType.TRANSFER) {
                    Label("Desde")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { a ->
                            FilterChip(selected = accountId == a.id, onClick = { accountId = a.id }, label = { Text("${a.emoji} ${a.name}") })
                        }
                    }
                    Label("Hacia")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.filter { it.id != accountId }.forEach { a ->
                            FilterChip(selected = toAccountId == a.id, onClick = { toAccountId = a.id }, label = { Text("${a.emoji} ${a.name}") })
                        }
                    }
                } else {
                    Label("Categoría")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        typeCategories.forEach { c ->
                            FilterChip(selected = categoryId == c.id, onClick = { categoryId = c.id }, label = { Text("${c.emoji} ${c.name}") })
                        }
                    }
                    if (accounts.size > 1) {
                        Label("Cuenta")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            accounts.forEach { a ->
                                FilterChip(selected = accountId == a.id, onClick = { accountId = a.id }, label = { Text("${a.emoji} ${a.name}") })
                            }
                        }
                    }
                }

                Label("Fecha")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = date == today, onClick = { date = today }, label = { Text("Hoy") })
                    FilterChip(selected = date == today - 1, onClick = { date = today - 1 }, label = { Text("Ayer") })
                    FilterChip(
                        selected = date != today && date != today - 1,
                        onClick = {
                            val d = LocalDate.ofEpochDay(date)
                            DatePickerDialog(
                                context,
                                { _, y, m, dayOfMonth -> date = LocalDate.of(y, m + 1, dayOfMonth).toEpochDay() },
                                d.year, d.monthValue - 1, d.dayOfMonth,
                            ).show()
                        },
                        label = { Text(if (date != today && date != today - 1) shortDate(date) else "📅 Otra fecha") },
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            Keypad(
                onKey = { amountText = appendKey(amountText, it) },
                onDelete = { amountText = amountText.dropLast(1) },
            )
            Button(
                onClick = { save() },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(52.dp),
            ) {
                Text(if (initial == null) "Guardar" else "Guardar cambios", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Borrar movimiento?") },
            text = { Text("Se eliminará ${Money.format(initial.amount)} del ${shortDate(initial.date)}.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteMovement(initial)
                    confirmDelete = false
                    onClose()
                }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
}

private fun appendKey(current: String, c: Char): String {
    if (c == ',') return when {
        current.contains(',') -> current
        current.isEmpty() -> "0,"
        else -> "$current,"
    }
    val parts = current.split(',')
    if (parts.size == 2 && parts[1].length >= 2) return current
    if (parts.size == 1 && parts[0].length >= 7) return current
    if (current == "0") return c.toString()
    return current + c
}

@Composable
private fun Keypad(onKey: (Char) -> Unit, onDelete: () -> Unit) {
    val rows = listOf("123", "456", "789", ",0<")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                row.forEach { key ->
                    FilledTonalButton(
                        onClick = { if (key == '<') onDelete() else onKey(key) },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Text(if (key == '<') "⌫" else key.toString(), fontSize = 22.sp)
                    }
                }
            }
        }
    }
}
