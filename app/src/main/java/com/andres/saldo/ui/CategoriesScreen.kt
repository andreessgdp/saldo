package com.andres.saldo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andres.saldo.data.CATEGORY_PALETTE
import com.andres.saldo.data.Category
import com.andres.saldo.data.TxType
import com.andres.saldo.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(vm: AppViewModel, onBack: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    val list = categories.filter { it.type == type }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, "Nueva categoría") }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == TxType.EXPENSE, onClick = { type = TxType.EXPENSE }, label = { Text("Gastos") })
                    FilterChip(selected = type == TxType.INCOME, onClick = { type = TxType.INCOME }, label = { Text("Ingresos") })
                }
            }
            items(list, key = { it.id }) { c ->
                ListItem(
                    modifier = Modifier.clickable { editing = c },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { EmojiBadge(c.emoji, Color(c.color)) },
                    headlineContent = { Text(c.name) },
                    supportingContent = {
                        val budgetText = c.budget?.takeIf { it > 0 }?.let { "Presupuesto: ${Money.format(it)}/mes" }
                        Text(if (c.type == TxType.EXPENSE) budgetText ?: "Sin presupuesto" else "Categoría de ingreso")
                    },
                )
            }
        }
    }

    if (creating) {
        CategoryDialog(null, type, onDismiss = { creating = false }, onSave = { vm.saveCategory(it); creating = false }, onDelete = null)
    }
    editing?.let { c ->
        CategoryDialog(
            c, c.type,
            onDismiss = { editing = null },
            onSave = { vm.saveCategory(it); editing = null },
            onDelete = { vm.deleteCategory(c); editing = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDialog(
    initial: Category?,
    type: TxType,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🏷️") }
    var color by remember { mutableIntStateOf(initial?.color ?: CATEGORY_PALETTE.random()) }
    var budget by remember { mutableStateOf(initial?.budget?.takeIf { it > 0 }?.let { Money.toInput(it) } ?: "") }
    var askDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva categoría" else "Editar categoría") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Text("Pega un emoji en Icono (desde el teclado de emojis).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORY_PALETTE.forEach { c ->
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(Color(c))
                                .then(if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { color = c },
                        )
                    }
                }
                if (type == TxType.EXPENSE) {
                    OutlinedTextField(
                        value = budget, onValueChange = { budget = it },
                        label = { Text("Presupuesto mensual (€)") },
                        placeholder = { Text("Vacío = sin límite") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (onDelete != null) {
                    if (askDelete) {
                        Text("Los movimientos de esta categoría quedarán como \"Sin categoría\" y sus recurrentes se pausarán.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onDelete) { Text("Sí, eliminar", color = MaterialTheme.colorScheme.error) }
                    } else {
                        TextButton(onClick = { askDelete = true }) { Text("Eliminar categoría", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val base = initial ?: Category(name = "", emoji = "", color = color, type = type)
                    onSave(
                        base.copy(
                            name = name.trim(),
                            emoji = emoji.trim().ifBlank { "🏷️" },
                            color = color,
                            budget = if (type == TxType.EXPENSE) Money.parse(budget)?.takeIf { it > 0 } else null,
                        )
                    )
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
