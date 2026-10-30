package com.andres.saldo.ui

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.andres.saldo.SaldoApp
import com.andres.saldo.data.Account
import com.andres.saldo.data.Category
import com.andres.saldo.data.Movement
import com.andres.saldo.data.Recurring
import com.andres.saldo.util.endDay
import com.andres.saldo.util.startDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SaldoApp).repository

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val month = MutableStateFlow(YearMonth.now())

    val accounts = repo.accounts.state(emptyList())
    val allAccounts = repo.allAccounts.state(emptyList())
    val categories = repo.categories.state(emptyList())
    val allCategories = repo.allCategories.state(emptyList())
    val recurring = repo.recurring.state(emptyList())
    val balances = repo.balances.state(emptyMap())

    val monthMovements: StateFlow<List<Movement>> =
        month.flatMapLatest { ym -> repo.movements(ym.startDay(), ym.endDay()) }.state(emptyList())

    /** Los 6 meses que terminan en el mes seleccionado (para las gráficas). */
    val history: StateFlow<List<Movement>> =
        month.flatMapLatest { ym -> repo.movements(ym.minusMonths(5).startDay(), ym.endDay()) }.state(emptyList())

    fun prevMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun currentMonth() { month.value = YearMonth.now() }

    fun saveMovement(m: Movement) = viewModelScope.launch { repo.saveMovement(m) }
    fun deleteMovement(m: Movement) = viewModelScope.launch { repo.deleteMovement(m) }

    fun saveAccount(a: Account) = viewModelScope.launch { repo.saveAccount(a) }
    fun deleteAccount(a: Account) = viewModelScope.launch {
        val deleted = repo.deleteAccount(a)
        if (!deleted) toast("La cuenta tiene movimientos: se ha archivado (deja de aparecer, pero el historial se conserva)")
    }

    fun saveCategory(c: Category) = viewModelScope.launch { repo.saveCategory(c) }
    fun deleteCategory(c: Category) = viewModelScope.launch { repo.deleteCategory(c) }

    fun saveRecurring(r: Recurring) = viewModelScope.launch { repo.saveRecurring(r) }
    fun deleteRecurring(r: Recurring) = viewModelScope.launch { repo.deleteRecurring(r) }

    fun exportJson(uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = repo.exportJson()
            write(uri, text)
        }.onSuccess { toast("Copia de seguridad guardada") }
            .onFailure { toast("No se pudo guardar: ${it.message}") }
    }

    fun exportCsv(uri: Uri) = viewModelScope.launch {
        runCatching { write(uri, repo.exportCsv()) }
            .onSuccess { toast("CSV exportado") }
            .onFailure { toast("No se pudo exportar: ${it.message}") }
    }

    fun importJson(uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            } ?: error("No se pudo leer el archivo")
            repo.importJson(text)
        }.onSuccess { toast("Datos restaurados") }
            .onFailure { toast("Importación fallida: ${it.message}") }
    }

    private suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val out = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
            ?: error("No se pudo abrir el archivo")
        out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private fun toast(msg: String) {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
        }
    }
}
