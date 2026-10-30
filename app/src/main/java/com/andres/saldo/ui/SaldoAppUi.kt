package com.andres.saldo.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andres.saldo.ACTION_ADD_EXPENSE
import com.andres.saldo.ACTION_ADD_INCOME
import com.andres.saldo.ACTION_ADD_TRANSFER
import com.andres.saldo.LaunchRequest
import com.andres.saldo.data.Movement
import com.andres.saldo.data.TxType

sealed interface Overlay {
    data class EditMovement(val movement: Movement?, val type: TxType, val closeAppAfterSave: Boolean = false) : Overlay
    data object Categories : Overlay
    data object RecurringList : Overlay
}

private data class Tab(val emoji: String, val label: String)

private val TABS = listOf(
    Tab("🧾", "Movimientos"),
    Tab("📊", "Resumen"),
    Tab("💳", "Cuentas"),
    Tab("⚙️", "Ajustes"),
)

@Composable
fun SaldoAppUi(
    launchRequest: LaunchRequest?,
    onLaunchHandled: () -> Unit,
    vm: AppViewModel = viewModel(),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val overlays = remember { mutableStateListOf<Overlay>() }
    val context = LocalContext.current

    LaunchedEffect(launchRequest) {
        val req = launchRequest ?: return@LaunchedEffect
        val type = when (req.action) {
            ACTION_ADD_EXPENSE -> TxType.EXPENSE
            ACTION_ADD_INCOME -> TxType.INCOME
            ACTION_ADD_TRANSFER -> TxType.TRANSFER
            else -> null
        }
        if (type != null) {
            overlays.removeAll { it is Overlay.EditMovement }
            overlays.add(Overlay.EditMovement(null, type, closeAppAfterSave = req.closeAfterSave))
        }
        onLaunchHandled()
    }

    BackHandler(enabled = overlays.isNotEmpty()) { overlays.removeAt(overlays.lastIndex) }

    fun pop() { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }

    when (val top = overlays.lastOrNull()) {
        is Overlay.EditMovement -> key(top) { MovementEditor(
            vm = vm,
            initial = top.movement,
            initialType = top.type,
            onClose = { pop() },
            onSaved = {
                pop()
                if (top.closeAppAfterSave) {
                    Toast.makeText(context, "Guardado ✔", Toast.LENGTH_SHORT).show()
                    (context as? Activity)?.finish()
                }
            },
        ) }
        Overlay.Categories -> CategoriesScreen(vm, onBack = { pop() })
        Overlay.RecurringList -> RecurringScreen(vm, onBack = { pop() })
        null -> Scaffold(
            bottomBar = {
                NavigationBar {
                    TABS.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Text(t.emoji, fontSize = 20.sp) },
                            label = { Text(t.label) },
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab != 3) {
                    FloatingActionButton(onClick = {
                        overlays.add(Overlay.EditMovement(null, if (tab == 2) TxType.TRANSFER else TxType.EXPENSE))
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir movimiento")
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    0 -> MovementsScreen(vm, onEdit = { m -> overlays.add(Overlay.EditMovement(m, m.type)) })
                    1 -> SummaryScreen(vm)
                    2 -> AccountsScreen(vm)
                    else -> SettingsScreen(
                        vm,
                        openCategories = { overlays.add(Overlay.Categories) },
                        openRecurring = { overlays.add(Overlay.RecurringList) },
                    )
                }
            }
        }
    }
}
