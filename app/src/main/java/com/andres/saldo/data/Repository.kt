package com.andres.saldo.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import com.andres.saldo.util.Money
import com.andres.saldo.util.endDay
import com.andres.saldo.util.nextOccurrence
import com.andres.saldo.util.startDay
import com.andres.saldo.widget.QuickAddWidget
import com.andres.saldo.widget.SummaryWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

val CATEGORY_PALETTE: List<Int> = listOf(
    0xFFE57373, 0xFFF06292, 0xFFBA68C8, 0xFF9575CD, 0xFF7986CB, 0xFF64B5F6, 0xFF4DD0E1,
    0xFF4DB6AC, 0xFF81C784, 0xFFDCE775, 0xFFFFD54F, 0xFFFFB74D, 0xFFA1887F, 0xFF90A4AE,
).map { it.toInt() }

data class MonthSnapshot(
    val month: YearMonth,
    val income: Long,
    val expense: Long,
    val budget: Long,
    val budgetSpent: Long,
)

class Repository(private val context: Context, private val db: AppDatabase) {
    private val dao = db.dao()
    private val mutex = Mutex()

    val accounts: Flow<List<Account>> = dao.observeAccounts()
    val allAccounts: Flow<List<Account>> = dao.observeAllAccounts()
    val categories: Flow<List<Category>> = dao.observeCategories()
    val allCategories: Flow<List<Category>> = dao.observeAllCategories()
    val recurring: Flow<List<Recurring>> = dao.observeRecurring()

    fun movements(start: Long, end: Long): Flow<List<Movement>> = dao.observeMovements(start, end)

    /** Saldo actual por cuenta: saldo inicial + ingresos − gastos − transferencias salientes + entrantes. */
    val balances: Flow<Map<Long, Long>> = combine(
        dao.observeAllAccounts(), dao.observeAccountTypeSums(), dao.observeTransferIns(),
    ) { accs, sums, ins ->
        accs.associate { a ->
            var b = a.initialBalance
            for (s in sums) if (s.accountId == a.id) {
                b += if (s.type == TxType.INCOME) s.total else -s.total
            }
            for (i in ins) if (i.accountId == a.id) b += i.total
            a.id to b
        }
    }

    // ---------- Escrituras ----------
    suspend fun saveMovement(m: Movement) {
        if (m.id == 0L) dao.insertMovement(m) else dao.updateMovement(m)
        refreshWidgets()
    }

    suspend fun deleteMovement(m: Movement) {
        dao.deleteMovement(m)
        refreshWidgets()
    }

    suspend fun saveAccount(a: Account) {
        if (a.id == 0L) dao.insertAccount(a.copy(sortOrder = dao.getAccounts().size)) else dao.updateAccount(a)
    }

    /** Devuelve true si se borró; false si se archivó porque tenía movimientos. */
    suspend fun deleteAccount(a: Account): Boolean {
        return if (dao.countMovementsForAccount(a.id) == 0) {
            dao.deleteAccount(a); true
        } else {
            dao.updateAccount(a.copy(archived = true)); false
        }
    }

    suspend fun saveCategory(c: Category) {
        if (c.id == 0L) dao.insertCategory(c.copy(sortOrder = dao.getCategories().size)) else dao.updateCategory(c)
        refreshWidgets()
    }

    suspend fun deleteCategory(c: Category) {
        db.withTransaction {
            dao.detachCategoryFromMovements(c.id)
            dao.detachCategoryFromRecurring(c.id)
            dao.deleteCategory(c)
        }
        refreshWidgets()
    }

    suspend fun saveRecurring(r: Recurring) {
        if (r.id == 0L) dao.insertRecurring(r) else dao.updateRecurring(r)
        generateRecurring()
    }

    suspend fun deleteRecurring(r: Recurring) = dao.deleteRecurring(r)

    // ---------- Recurrentes ----------
    /** Crea los movimientos recurrentes que ya tocan (hasta hoy incluido). */
    suspend fun generateRecurring(today: LocalDate = LocalDate.now()) {
        var changed = false
        mutex.withLock {
            db.withTransaction {
                for (r in dao.getActiveRecurring()) {
                    var next = LocalDate.ofEpochDay(r.nextDate)
                    var guard = 0
                    while (!next.isAfter(today) && guard < 36) {
                        dao.insertMovement(
                            Movement(
                                amount = r.amount, type = r.type, categoryId = r.categoryId,
                                accountId = r.accountId, toAccountId = r.toAccountId,
                                date = next.toEpochDay(), note = r.note, recurringId = r.id,
                            )
                        )
                        next = nextOccurrence(next, r.dayOfMonth)
                        guard++
                        changed = true
                    }
                    if (next.toEpochDay() != r.nextDate) dao.updateRecurring(r.copy(nextDate = next.toEpochDay()))
                }
            }
        }
        if (changed) refreshWidgets()
    }

    // ---------- Lecturas puntuales (widget) ----------
    suspend fun monthSnapshot(ym: YearMonth): MonthSnapshot {
        val list = dao.getMovements(ym.startDay(), ym.endDay())
        val budgetCats = dao.getCategories().filter { it.type == TxType.EXPENSE && !it.archived && (it.budget ?: 0) > 0 }
        val ids = budgetCats.map { it.id }.toSet()
        return MonthSnapshot(
            month = ym,
            income = list.filter { it.type == TxType.INCOME }.sumOf { it.amount },
            expense = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
            budget = budgetCats.sumOf { it.budget ?: 0L },
            budgetSpent = list.filter { it.type == TxType.EXPENSE && it.categoryId in ids }.sumOf { it.amount },
        )
    }

    // ---------- Datos iniciales ----------
    suspend fun seedIfEmpty() {
        mutex.withLock {
            if (dao.countCategories() > 0) return@withLock
            db.withTransaction {
                if (dao.getAccounts().isEmpty()) {
                    dao.insertAccount(Account(name = "Tarjeta", emoji = "💳", sortOrder = 0))
                    dao.insertAccount(Account(name = "Efectivo", emoji = "💶", sortOrder = 1))
                    dao.insertAccount(Account(name = "Ahorro", emoji = "🏦", sortOrder = 2))
                }
                val expenses = listOf(
                    "🛒" to "Supermercado", "🍔" to "Comer fuera", "🚆" to "Transporte", "🚗" to "Coche",
                    "🏠" to "Casa", "📱" to "Suscripciones", "🎮" to "Ocio", "👕" to "Ropa",
                    "🏋️" to "Deporte", "💊" to "Salud", "🎁" to "Regalos", "📦" to "Otros",
                )
                expenses.forEachIndexed { i, (e, n) ->
                    dao.insertCategory(
                        Category(name = n, emoji = e, color = CATEGORY_PALETTE[i % CATEGORY_PALETTE.size], type = TxType.EXPENSE, sortOrder = i)
                    )
                }
                val incomes = listOf("💼" to "Nómina", "💰" to "Extras", "🎁" to "Regalos", "↩️" to "Devoluciones", "➕" to "Otros ingresos")
                incomes.forEachIndexed { i, (e, n) ->
                    dao.insertCategory(
                        Category(name = n, emoji = e, color = CATEGORY_PALETTE[(i + 7) % CATEGORY_PALETTE.size], type = TxType.INCOME, sortOrder = 100 + i)
                    )
                }
            }
        }
    }

    // ---------- Copia de seguridad ----------
    suspend fun exportJson(): String {
        val accounts = dao.getAccounts()
        val categories = dao.getCategories()
        val movements = dao.getAllMovements()
        val recurring = dao.getRecurring()
        val root = JSONObject()
        root.put("app", "saldo")
        root.put("version", 1)
        root.put("exportedAt", LocalDateTime.now().toString())
        root.put("accounts", JSONArray().also { arr ->
            accounts.forEach { a ->
                arr.put(
                    JSONObject().put("id", a.id).put("name", a.name).put("emoji", a.emoji)
                        .put("initialBalance", a.initialBalance).put("sortOrder", a.sortOrder).put("archived", a.archived)
                )
            }
        })
        root.put("categories", JSONArray().also { arr ->
            categories.forEach { c ->
                arr.put(
                    JSONObject().put("id", c.id).put("name", c.name).put("emoji", c.emoji).put("color", c.color)
                        .put("type", c.type.name).put("budget", c.budget ?: JSONObject.NULL)
                        .put("sortOrder", c.sortOrder).put("archived", c.archived)
                )
            }
        })
        root.put("movements", JSONArray().also { arr ->
            movements.forEach { m ->
                arr.put(
                    JSONObject().put("id", m.id).put("amount", m.amount).put("type", m.type.name)
                        .put("categoryId", m.categoryId ?: JSONObject.NULL).put("accountId", m.accountId)
                        .put("toAccountId", m.toAccountId ?: JSONObject.NULL).put("date", m.date)
                        .put("note", m.note).put("recurringId", m.recurringId ?: JSONObject.NULL)
                        .put("createdAt", m.createdAt)
                )
            }
        })
        root.put("recurring", JSONArray().also { arr ->
            recurring.forEach { r ->
                arr.put(
                    JSONObject().put("id", r.id).put("amount", r.amount).put("type", r.type.name)
                        .put("categoryId", r.categoryId ?: JSONObject.NULL).put("accountId", r.accountId)
                        .put("toAccountId", r.toAccountId ?: JSONObject.NULL).put("dayOfMonth", r.dayOfMonth)
                        .put("note", r.note).put("nextDate", r.nextDate).put("active", r.active)
                )
            }
        })
        return root.toString(2)
    }

    /** Sustituye TODOS los datos por los del backup. Lanza excepción si el archivo no es válido. */
    suspend fun importJson(text: String) {
        val root = JSONObject(text)
        require(root.optString("app") == "saldo") { "El archivo no es una copia de Saldo" }

        fun JSONObject.longOrNull(k: String): Long? = if (!has(k) || isNull(k)) null else getLong(k)
        fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

        val accounts = root.getJSONArray("accounts").objects().map {
            Account(it.getLong("id"), it.getString("name"), it.getString("emoji"), it.getLong("initialBalance"),
                it.optInt("sortOrder"), it.optBoolean("archived"))
        }
        val categories = root.getJSONArray("categories").objects().map {
            Category(it.getLong("id"), it.getString("name"), it.getString("emoji"), it.getInt("color"),
                TxType.valueOf(it.getString("type")), it.longOrNull("budget"), it.optInt("sortOrder"), it.optBoolean("archived"))
        }
        val movements = root.getJSONArray("movements").objects().map {
            Movement(it.getLong("id"), it.getLong("amount"), TxType.valueOf(it.getString("type")), it.longOrNull("categoryId"),
                it.getLong("accountId"), it.longOrNull("toAccountId"), it.getLong("date"), it.optString("note"),
                it.longOrNull("recurringId"), it.optLong("createdAt", System.currentTimeMillis()))
        }
        val recurring = root.optJSONArray("recurring")?.objects().orEmpty().map {
            Recurring(it.getLong("id"), it.getLong("amount"), TxType.valueOf(it.getString("type")), it.longOrNull("categoryId"),
                it.getLong("accountId"), it.longOrNull("toAccountId"), it.getInt("dayOfMonth"), it.optString("note"),
                it.getLong("nextDate"), it.optBoolean("active", true))
        }

        mutex.withLock {
            db.withTransaction {
                dao.clearMovements(); dao.clearRecurring(); dao.clearCategories(); dao.clearAccounts()
                dao.insertAccounts(accounts)
                dao.insertCategories(categories)
                dao.insertMovements(movements)
                dao.insertRecurringList(recurring)
            }
        }
        refreshWidgets()
    }

    /** CSV con ';' y coma decimal para que Excel en español lo abra bien. */
    suspend fun exportCsv(): String {
        val accs = dao.getAccounts().associateBy { it.id }
        val cats = dao.getCategories().associateBy { it.id }
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val sb = StringBuilder("﻿")
        sb.append("Fecha;Tipo;Importe;Categoría;Cuenta;Cuenta destino;Nota\n")
        for (m in dao.getAllMovements()) {
            val tipo = when (m.type) { TxType.EXPENSE -> "Gasto"; TxType.INCOME -> "Ingreso"; TxType.TRANSFER -> "Transferencia" }
            val signed = if (m.type == TxType.EXPENSE) -m.amount else m.amount
            sb.append(LocalDate.ofEpochDay(m.date)).append(';')
                .append(tipo).append(';')
                .append(Money.toInput(signed)).append(';')
                .append(esc(m.categoryId?.let { cats[it]?.name } ?: "")).append(';')
                .append(esc(accs[m.accountId]?.name ?: "")).append(';')
                .append(esc(m.toAccountId?.let { accs[it]?.name } ?: "")).append(';')
                .append(esc(m.note)).append('\n')
        }
        return sb.toString()
    }

    private suspend fun refreshWidgets() {
        try {
            SummaryWidget().updateAll(context)
            QuickAddWidget().updateAll(context)
        } catch (_: Exception) {
            // Sin widgets colocados o el sistema no está listo: no pasa nada.
        }
    }
}
