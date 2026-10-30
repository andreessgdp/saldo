package com.andres.saldo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        Text(
            "Ajustes",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
        )

        SectionTitle("Organización")
        SaldoCard(padding = 4.dp) {
            SettingsItem(Icons.Rounded.LocalOffer, Color(0xFF2F9E6E), "Categorías y presupuestos", "Crea, edita y ponles límite mensual", openCategories)
            ThinDivider(Modifier.padding(start = 72.dp, end = 16.dp))
            SettingsItem(Icons.Rounded.Repeat, Color(0xFF3D7EDB), "Movimientos recurrentes", "Nómina, renting, suscripciones: se apuntan solos", openRecurring)
        }

        SectionTitle("Copia de seguridad")
        SaldoCard(padding = 4.dp) {
            SettingsItem(Icons.Rounded.CloudUpload, Color(0xFFD6A21E), "Exportar copia", "Archivo JSON para guardar en Drive o en el móvil") {
                exportJson.launch("saldo-backup-${LocalDate.now()}.json")
            }
            ThinDivider(Modifier.padding(start = 72.dp, end = 16.dp))
            SettingsItem(Icons.Rounded.Restore, Color(0xFFE4683F), "Restaurar copia", "Sustituye los datos actuales por los de un archivo") {
                confirmImport = true
            }
            ThinDivider(Modifier.padding(start = 72.dp, end = 16.dp))
            SettingsItem(Icons.Rounded.TableChart, Color(0xFF8E5BD8), "Exportar a CSV", "Para abrir tus movimientos en Excel o Sheets") {
                exportCsv.launch("saldo-movimientos-${LocalDate.now()}.csv")
            }
        }

        Text(
            "Tus datos se guardan solo en este móvil. Android también los incluye en la copia automática de Google si la tienes activada, pero exporta una copia de vez en cuando.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
        Text(
            "Saldo",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
private fun SettingsItem(icon: ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, color)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
