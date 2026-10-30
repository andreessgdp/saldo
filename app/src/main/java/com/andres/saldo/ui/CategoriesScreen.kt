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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.andres.saldo.data.IconKeys
import com.andres.saldo.data.TxType
import com.andres.saldo.util.Money

/** Cabecera común para pantallas secundarias. */
@Composable
fun SubScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", onBack)
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun CategoriesScreen(vm: AppViewModel, onBack: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    val list = categories.filter { it.type == type }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SubScreenHeader("Categorías", onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { creating = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) { Icon(Icons.Rounded.Add, "Nueva categoría") }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 110.dp)) {
            item {
                PillSelector(
                    listOf(TxType.EXPENSE to "Gastos", TxType.INCOME to "Ingresos"),
                    type, { type = it },
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                Spacer(Modifier.padding(top = 8.dp))
                SaldoCard(padding = 4.dp) {
                    list.forEachIndexed { i, c ->
                        if (i > 0) ThinDivider(Modifier.padding(start = 72.dp, end = 16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { editing = c }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconTile(AppIcons.category(c.icon), Color(c.color))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall)
                                val budgetText = c.budget?.takeIf { it > 0 }?.let { "Presupuesto ${Money.format(it)} al mes" }
                                Text(
                                    if (c.type == TxType.EXPENSE) budgetText ?: "Sin presupuesto" else "Ingreso",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
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
    var icon by remember { mutableStateOf(initial?.icon ?: "other") }
    var color by remember { mutableIntStateOf(initial?.color ?: CATEGORY_PALETTE.random()) }
    var budget by remember { mutableStateOf(initial?.budget?.takeIf { it > 0 }?.let { Money.toInput(it) } ?: "") }
    var askDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(AppIcons.category(icon), Color(color), size = 40.dp, selected = true)
                Spacer(Modifier.width(12.dp))
                Text(if (initial == null) "Nueva categoría" else "Editar categoría", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                MiniLabel("Color")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CATEGORY_PALETTE.forEach { c ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .then(if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { color = c },
                        )
                    }
                }
                MiniLabel("Icono")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconKeys.category.forEach { k ->
                        IconTile(
                            AppIcons.category(k), Color(color), size = 40.dp, selected = icon == k,
                            modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { icon = k },
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
                        shape = MaterialTheme.shapes.medium,
                    )
                }
                if (onDelete != null) {
                    if (askDelete) {
                        Text("Sus movimientos quedarán como \"Sin categoría\" y sus recurrentes se pausarán.", style = MaterialTheme.typography.bodySmall)
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
                    val base = initial ?: Category(name = "", icon = icon, color = color, type = type)
                    onSave(
                        base.copy(
                            name = name.trim(),
                            icon = icon,
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
