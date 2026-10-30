package com.andres.saldo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andres.saldo.data.Account
import com.andres.saldo.data.IconKeys
import com.andres.saldo.ui.theme.AmountStyle
import com.andres.saldo.ui.theme.Brand
import com.andres.saldo.ui.theme.SaldoColors
import com.andres.saldo.util.Money

@Composable
fun AccountsScreen(vm: AppViewModel, onTransfer: () -> Unit) {
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Account?>(null) }
    var creating by remember { mutableStateOf(false) }
    val total = accounts.sumOf { balances[it.id] ?: it.initialBalance }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
        Spacer(Modifier.height(8.dp))
        // Patrimonio
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(Brand.heroBrush),
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Brand.Lime.copy(alpha = 0.08f), radius = size.height * 0.9f, center = Offset(size.width, size.height))
            }
            Column(Modifier.padding(22.dp)) {
                Text("Patrimonio total", style = MaterialTheme.typography.labelLarge, color = Brand.OnHeroMuted)
                Spacer(Modifier.height(6.dp))
                Text(Money.format(total), style = AmountStyle.copy(fontSize = 38.sp), color = Brand.OnHero, maxLines = 1)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroAction(Icons.Rounded.SwapHoriz, "Transferir", onTransfer, Modifier.weight(1f))
                    HeroAction(Icons.Rounded.Add, "Nueva cuenta", { creating = true }, Modifier.weight(1f))
                }
            }
        }

        SectionTitle("Tus cuentas")
        SaldoCard(padding = 4.dp) {
            accounts.forEachIndexed { i, a ->
                if (i > 0) ThinDivider(Modifier.padding(start = 72.dp, end = 16.dp))
                val b = balances[a.id] ?: a.initialBalance
                val share = if (total > 0 && b > 0) b.toFloat() / total else 0f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { editing = a }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile(AppIcons.account(a.icon), MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.name, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        ProgressBar(share, MaterialTheme.colorScheme.primary, Modifier.width(110.dp), height = 4.dp)
                    }
                    Text(
                        Money.format(b),
                        style = AmountStyle.copy(fontSize = 17.sp),
                        color = if (b >= 0) MaterialTheme.colorScheme.onSurface else SaldoColors.expense(),
                    )
                }
            }
        }
        Text(
            "Pon como saldo inicial lo que tenía cada cuenta el día que empezaste a usar Saldo; así cuadra con tu banco.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
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
private fun HeroAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Brand.Lime, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Brand.OnHero)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountDialog(initial: Account?, onDismiss: () -> Unit, onSave: (Account) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: "card") }
    var balance by remember { mutableStateOf(initial?.let { Money.toInput(it.initialBalance) } ?: "") }
    var askDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(if (initial == null) "Nueva cuenta" else "Editar cuenta", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                MiniLabel("Icono")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconKeys.account.forEach { k ->
                        IconTile(
                            AppIcons.account(k), MaterialTheme.colorScheme.primary, size = 44.dp, selected = icon == k,
                            modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { icon = k },
                        )
                    }
                }
                OutlinedTextField(
                    value = balance, onValueChange = { balance = it },
                    label = { Text("Saldo inicial (€)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
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
                    val base = initial ?: Account(name = "", icon = "card")
                    onSave(base.copy(name = name.trim(), icon = icon, initialBalance = Money.parse(balance) ?: 0L))
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
