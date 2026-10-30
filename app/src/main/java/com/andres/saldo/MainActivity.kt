package com.andres.saldo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.andres.saldo.ui.SaldoAppUi
import com.andres.saldo.ui.theme.SaldoTheme
import kotlinx.coroutines.launch

const val ACTION_ADD_EXPENSE = "com.andres.saldo.ADD_EXPENSE"
const val ACTION_ADD_INCOME = "com.andres.saldo.ADD_INCOME"
const val ACTION_ADD_TRANSFER = "com.andres.saldo.ADD_TRANSFER"
const val ACTION_OPEN = "com.andres.saldo.OPEN"

/** Petición de abrir el editor desde un widget o atajo. closeAfterSave: volver al escritorio tras guardar. */
data class LaunchRequest(val action: String, val closeAfterSave: Boolean)

class MainActivity : ComponentActivity() {
    private val launchRequest = mutableStateOf<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            intent?.action?.let { launchRequest.value = LaunchRequest(it, closeAfterSave = true) }
        }
        setContent {
            SaldoTheme {
                SaldoAppUi(
                    launchRequest = launchRequest.value,
                    onLaunchHandled = { launchRequest.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.action?.let { launchRequest.value = LaunchRequest(it, closeAfterSave = false) }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { (application as SaldoApp).repository.generateRecurring() }
    }
}
