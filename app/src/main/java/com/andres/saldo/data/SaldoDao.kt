package com.andres.saldo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SaldoDao {

    // ---------- Cuentas ----------
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun observeAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    suspend fun getAccounts(): List<Account>

    @Insert
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT COUNT(*) FROM movements WHERE accountId = :id OR toAccountId = :id")
    suspend fun countMovementsForAccount(id: Long): Int

    @Query("SELECT accountId, type, SUM(amount) AS total FROM movements GROUP BY accountId, type")
    fun observeAccountTypeSums(): Flow<List<AccountTypeSum>>

    @Query(
        "SELECT toAccountId AS accountId, SUM(amount) AS total FROM movements " +
            "WHERE type = 'TRANSFER' AND toAccountId IS NOT NULL GROUP BY toAccountId"
    )
    fun observeTransferIns(): Flow<List<AccountSum>>

    // ---------- Categorías ----------
    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    suspend fun getCategories(): List<Category>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("UPDATE movements SET categoryId = NULL WHERE categoryId = :id")
    suspend fun detachCategoryFromMovements(id: Long)

    @Query("UPDATE recurring SET categoryId = NULL, active = 0 WHERE categoryId = :id")
    suspend fun detachCategoryFromRecurring(id: Long)

    // ---------- Movimientos ----------
    @Query("SELECT * FROM movements WHERE date BETWEEN :start AND :end ORDER BY date DESC, createdAt DESC")
    fun observeMovements(start: Long, end: Long): Flow<List<Movement>>

    @Query("SELECT * FROM movements WHERE date BETWEEN :start AND :end ORDER BY date DESC, createdAt DESC")
    suspend fun getMovements(start: Long, end: Long): List<Movement>

    @Query("SELECT * FROM movements ORDER BY date, createdAt")
    suspend fun getAllMovements(): List<Movement>

    @Insert
    suspend fun insertMovement(movement: Movement): Long

    @Update
    suspend fun updateMovement(movement: Movement)

    @Delete
    suspend fun deleteMovement(movement: Movement)

    // ---------- Recurrentes ----------
    @Query("SELECT * FROM recurring ORDER BY dayOfMonth, id")
    fun observeRecurring(): Flow<List<Recurring>>

    @Query("SELECT * FROM recurring ORDER BY dayOfMonth, id")
    suspend fun getRecurring(): List<Recurring>

    @Query("SELECT * FROM recurring WHERE active = 1")
    suspend fun getActiveRecurring(): List<Recurring>

    @Insert
    suspend fun insertRecurring(recurring: Recurring): Long

    @Update
    suspend fun updateRecurring(recurring: Recurring)

    @Delete
    suspend fun deleteRecurring(recurring: Recurring)

    // ---------- Copia de seguridad ----------
    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM movements") suspend fun clearMovements()
    @Query("DELETE FROM recurring") suspend fun clearRecurring()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAccounts(items: List<Account>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategories(items: List<Category>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMovements(items: List<Movement>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRecurringList(items: List<Recurring>)
}
