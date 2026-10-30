package com.andres.saldo

import android.app.Application
import com.andres.saldo.data.AppDatabase
import com.andres.saldo.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SaldoApp : Application() {
    lateinit var repository: Repository
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this, AppDatabase.build(this))
        appScope.launch {
            repository.seedIfEmpty()
            repository.generateRecurring()
        }
    }
}
