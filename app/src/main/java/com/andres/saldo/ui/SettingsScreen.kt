package com.andres.saldo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: AppViewModel, openCategories: () -> Unit, openRecurring: () -> Unit) {
    var confirmImport by remember { mutableStateOf(false) }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportJson(uri)
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri)
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importJson(uri)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        SectionTitle("Organización")
        SettingsItem("🏷️", "Categorías y presupuestos", "Crea, edita y ponles límite mensual", openCategories)
        SettingsItem("🔁", "Movimientos recurrentes", "Nómina, alquiler, suscripciones... se apuntan solos", openRecurring)

        SectionTitle("Copia de seguridad")
        SettingsItem("💾", "Exportar copia (JSON)", "Guárdala en Drive o en el móvil. Sirve para restaurar todo") {
            exportJson.launch("saldo-backup-${LocalDate.now()}.json")
        }
        SettingsItem("📥", "Restaurar copia", "Sustituye los datos actuales por los de un archivo") {
            confirmImport = true
        }
        SettingsItem("📄", "Exportar movimientos (CSV)", "Para abrirlos en Excel o Google Sheets") {
            exportCsv.launch("saldo-movimientos-${LocalDate.now()}.csv")
        }

        Text(
            "Tus datos se guardan solo en este móvil. Android también los incluye en la copia de seguridad automática de Google si la tienes activada, pero haz una exportación de vez en cuando por si acaso.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        Text(
            "Saldo · hecha a medida para Andrés",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("¿Restaurar copia?") },
            text = { Text("Se borrarán todos los datos actuales y se sustituirán por los del archivo. Si quieres, exporta antes una copia.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    importJson.launch(arrayOf("*/*"))
                }) { Text("Elegir archivo") }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SettingsItem(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { EmojiBadge(emoji, MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
    )
}
