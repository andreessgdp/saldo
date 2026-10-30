package com.andres.saldo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.andres.saldo.data.TxType

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9E6CF),
    onPrimaryContainer = Color(0xFF002115),
    secondary = Color(0xFF4D6357),
    background = Color(0xFFFBFDF9),
    surface = Color(0xFFFBFDF9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DD3B6),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005237),
    onPrimaryContainer = Color(0xFFB9E6CF),
    secondary = Color(0xFFB4CCBE),
    background = Color(0xFF101411),
    surface = Color(0xFF101411),
)

@Composable
fun SaldoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

object SaldoColors {
    @Composable
    fun income(): Color = if (isSystemInDarkTheme()) Color(0xFF7FD69A) else Color(0xFF1E8E3E)

    @Composable
    fun expense(): Color = if (isSystemInDarkTheme()) Color(0xFFFF8A80) else Color(0xFFC62828)

    @Composable
    fun warning(): Color = if (isSystemInDarkTheme()) Color(0xFFFFCC80) else Color(0xFFE08600)

    @Composable
    fun forType(type: TxType): Color = when (type) {
        TxType.INCOME -> income()
        TxType.EXPENSE -> expense()
        TxType.TRANSFER -> MaterialTheme.colorScheme.primary
    }
}
