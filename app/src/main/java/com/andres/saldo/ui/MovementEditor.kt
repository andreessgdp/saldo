package com.andres.saldo.ui

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andres.saldo.data.Movement
import com.andres.saldo.data.TxType
import com.andres.saldo.ui.theme.AmountStyle
import com.andres.saldo.ui.theme.SaldoColors
import com.andres.saldo.util.Money
import com.andres.saldo.util.shortDate
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
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
    val haptics = LocalHapticFeedback.current

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
    val accent by animateColorAsState(SaldoColors.forType(type), label = "accent")

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
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onSaved()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
        ) {
            // Barra superior
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(Icons.Rounded.Close, "Cerrar", onClose)
                Text(
                    if (initial == null) "Nuevo movimiento" else "Editar movimiento",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                if (initial != null) {
                    RoundIconButton(Icons.Rounded.DeleteOutline, "Borrar", { confirmDelete = true })
                } else {
                    Spacer(Modifier.size(42.dp))
                }
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillSelector(
                    options = listOf(TxType.EXPENSE to "Gasto", TxType.INCOME to "Ingreso", TxType.TRANSFER to "Transferencia"),
                    selected = type,
                    onSelect = { t -> if (type != t) { type = t; categoryId = null } },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Importe
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        if (amountText.isEmpty()) "0" else amountText,
                        style = AmountStyle.copy(fontSize = 56.sp),
                        color = if (amountText.isEmpty()) MaterialTheme.colorScheme.outline else accent,
                        maxLines = 1,
                    )
                    Text(
                        " €",
                        style = AmountStyle.copy(fontSize = 28.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                if (type == TxType.TRANSFER) {
                    MiniLabel("Desde")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { a ->
                            SaldoChip(a.name, accountId == a.id, { accountId = a.id }, AppIcons.account(a.icon))
                        }
                    }
                    MiniLabel("Hacia")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.filter { it.id != accountId }.forEach { a ->
                            SaldoChip(a.name, toAccountId == a.id, { toAccountId = a.id }, AppIcons.account(a.icon))
                        }
                    }
                } else {
                    MiniLabel("Categoría")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        typeCategories.forEach { c ->
                            CategoryOption(
                                name = c.name,
                                color = Color(c.color),
                                iconKey = c.icon,
                                selected = categoryId == c.id,
                                onClick = {
                                    categoryId = c.id
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            )
                        }
                    }
                    if (accounts.size > 1) {
                        MiniLabel("Cuenta")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            accounts.forEach { a ->
                                SaldoChip(a.name, accountId == a.id, { accountId = a.id }, AppIcons.account(a.icon))
                            }
                        }
                    }
                }

                MiniLabel("Fecha")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SaldoChip("Hoy", date == today, { date = today })
                    SaldoChip("Ayer", date == today - 1, { date = today - 1 })
                    val custom = date != today && date != today - 1
                    SaldoChip(
                        if (custom) shortDate(date) else "Otra fecha",
                        custom,
                        {
                            val d = LocalDate.ofEpochDay(date)
                            DatePickerDialog(
                                context,
                                { _, y, m, dayOfMonth -> date = LocalDate.of(y, m + 1, dayOfMonth).toEpochDay() },
                                d.year, d.monthValue - 1, d.dayOfMonth,
                            ).show()
                        },
                        Icons.Rounded.Event,
                    )
                }

                // Nota
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (note.isEmpty()) {
                            Text("Añadir nota", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        BasicTextField(
                            value = note,
                            onValueChange = { note = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Keypad(
                onKey = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    amountText = appendKey(amountText, it)
                },
                onDelete = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    amountText = amountText.dropLast(1)
                },
            )

            // Guardar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(58.dp)
                    .clip(CircleShape)
                    .background(if (canSave) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(enabled = canSave) { save() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (initial == null) "Guardar" else "Guardar cambios",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (canSave) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
private fun CategoryOption(name: String, color: Color, iconKey: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(78.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconTile(AppIcons.category(iconKey), color, size = 50.dp, selected = selected)
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { if (key == '<') onDelete() else onKey(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == '<') {
                            Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.onSurface)
                        } else {
                            Text(key.toString(), style = AmountStyle.copy(fontSize = 24.sp), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
