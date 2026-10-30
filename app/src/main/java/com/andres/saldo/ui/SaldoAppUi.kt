package com.andres.saldo.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

private data class Tab(val icon: ImageVector, val label: String)

private val TABS = listOf(
    Tab(Icons.Rounded.Receipt, "Movimientos"),
    Tab(Icons.Rounded.DonutLarge, "Resumen"),
    Tab(Icons.Rounded.AccountBalanceWallet, "Cuentas"),
    Tab(Icons.Rounded.Tune, "Ajustes"),
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

    AnimatedContent(
        targetState = overlays.lastOrNull(),
        transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 12 }) togetherWith
                (fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 16 })
        },
        label = "overlay",
    ) { top ->
        when (top) {
            is Overlay.EditMovement -> key(top) {
                MovementEditor(
                    vm = vm,
                    initial = top.movement,
                    initialType = top.type,
                    onClose = { pop() },
                    onSaved = {
                        pop()
                        if (top.closeAppAfterSave) {
                            Toast.makeText(context, "Movimiento guardado", Toast.LENGTH_SHORT).show()
                            (context as? Activity)?.finish()
                        }
                    },
                )
            }
            Overlay.Categories -> CategoriesScreen(vm, onBack = { pop() })
            Overlay.RecurringList -> RecurringScreen(vm, onBack = { pop() })
            null -> MainScaffold(
                tab = tab,
                onTab = { tab = it },
                vm = vm,
                open = { overlays.add(it) },
            )
        }
    }
}

@Composable
private fun MainScaffold(tab: Int, onTab: (Int) -> Unit, vm: AppViewModel, open: (Overlay) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { onTab(i) },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab != 3) {
                FloatingActionButton(
                    onClick = { open(Overlay.EditMovement(null, if (tab == 2) TxType.TRANSFER else TxType.EXPENSE)) },
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Añadir movimiento")
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "tabs",
            ) { t ->
                when (t) {
                    0 -> MovementsScreen(vm, onEdit = { m -> open(Overlay.EditMovement(m, m.type)) })
                    1 -> SummaryScreen(vm)
                    2 -> AccountsScreen(vm, onTransfer = { open(Overlay.EditMovement(null, TxType.TRANSFER)) })
                    else -> SettingsScreen(
                        vm,
                        openCategories = { open(Overlay.Categories) },
                        openRecurring = { open(Overlay.RecurringList) },
                    )
                }
            }
        }
    }
}
