package com.andres.saldo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.andres.saldo.ui.theme.SaldoColors
import com.andres.saldo.util.Money

@Composable
fun AccountsScreen(vm: AppViewModel) {
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Account?>(null) }
    var creating by remember { mutableStateOf(false) }
    val total = accounts.sumOf { balances[it.id] ?: it.initialBalance }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
        ElevatedCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Patrimonio total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    Money.format(total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (total >= 0) MaterialTheme.colorScheme.onSurface else SaldoColors.expense(),
                )
                Text(
                    "Suma de todas tus cuentas. Usa + para mover dinero entre ellas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionTitle("Cuentas")
        accounts.forEach { a ->
            val b = balances[a.id] ?: a.initialBalance
            ListItem(
                modifier = Modifier.clickable { editing = a },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { EmojiBadge(a.emoji, MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(a.name) },
                supportingContent = { Text("Saldo inicial ${Money.format(a.initialBalance)}") },
                trailingContent = {
                    Text(
                        Money.format(b),
                        fontWeight = FontWeight.SemiBold,
                        color = if (b >= 0) MaterialTheme.colorScheme.onSurface else SaldoColors.expense(),
                    )
                },
            )
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(onClick = { creating = true }) { Text("+ Nueva cuenta") }
        }
        Text(
            "Consejo: pon como saldo inicial lo que tenías en cada cuenta el día que empezaste a usar la app; así los saldos cuadran con tu banco.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    if (creating) {
        AccountDialog(null, onDismiss = { creating = false }, onSave = { vm.saveAccount(it); creating = false }, onDelete = null)
    }
    editing?.let { acc ->
        AccountDialog(
            acc,
            onDismiss = { editing = null },
            onSave = { vm.saveAccount(it); editing = null },
            onDelete = if (accounts.size > 1) ({ vm.deleteAccount(acc); editing = null }) else null,
        )
    }
}

@Composable
private fun AccountDialog(initial: Account?, onDismiss: () -> Unit, onSave: (Account) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "💳") }
    var balance by remember { mutableStateOf(initial?.let { Money.toInput(it.initialBalance) } ?: "") }
    var askDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva cuenta" else "Editar cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = emoji, onValueChange = { emoji = it.take(8) },
                        label = { Text("Icono") }, singleLine = true, modifier = Modifier.width(88.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Nombre") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = balance, onValueChange = { balance = it },
                    label = { Text("Saldo inicial (€)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (onDelete != null) {
                    if (askDelete) {
                        Text("Si tiene movimientos se archivará en lugar de borrarse.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onDelete) { Text("Sí, eliminar", color = MaterialTheme.colorScheme.error) }
                    } else {
                        TextButton(onClick = { askDelete = true }) { Text("Eliminar cuenta", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val base = initial ?: Account(name = "", emoji = "")
                    onSave(base.copy(name = name.trim(), emoji = emoji.trim().ifBlank { "💳" }, initialBalance = Money.parse(balance) ?: 0L))
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
