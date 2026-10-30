package com.andres.saldo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Importes siempre en céntimos (Long) para no arrastrar errores de coma flotante. */
enum class TxType { EXPENSE, INCOME, TRANSFER }

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Clave de icono (ver ui/AppIcons). La columna conserva el nombre antiguo "emoji". */
    @ColumnInfo(name = "emoji") val icon: String,
    val initialBalance: Long = 0,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "emoji") val icon: String,
    val color: Int,
    val type: TxType,
    /** Presupuesto mensual en céntimos (solo gastos). null = sin presupuesto. */
    val budget: Long? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

@Entity(
    tableName = "movements",
    indices = [Index("date"), Index("categoryId"), Index("accountId")],
)
data class Movement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val type: TxType,
    val categoryId: Long?,
    val accountId: Long,
    /** Solo para transferencias: cuenta de destino. */
    val toAccountId: Long? = null,
    /** Día como LocalDate.toEpochDay(). */
    val date: Long,
    val note: String = "",
    val recurringId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recurring")
data class Recurring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val type: TxType,
    val categoryId: Long?,
    val accountId: Long,
    val toAccountId: Long? = null,
    val dayOfMonth: Int,
    val note: String = "",
    /** Próxima fecha (epochDay) en la que hay que generar el movimiento. */
    val nextDate: Long,
    val active: Boolean = true,
)

data class AccountTypeSum(val accountId: Long, val type: TxType, val total: Long)

data class AccountSum(val accountId: Long, val total: Long)
