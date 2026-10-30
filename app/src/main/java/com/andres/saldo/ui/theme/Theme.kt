package com.andres.saldo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andres.saldo.R
import com.andres.saldo.data.TxType

// ---------------------------------------------------------------------------
// Identidad: verde bosque profundo + lima, fondos cálidos, tipografía geométrica.
// ---------------------------------------------------------------------------

object Brand {
    val Forest = Color(0xFF0E3A2C)
    val ForestLight = Color(0xFF1D6B4F)
    val Lime = Color(0xFFD2F26B)
    val OnHero = Color(0xFFF3F6F1)
    val OnHeroMuted = Color(0xFFB7CBBF)

    val heroBrush: Brush
        get() = Brush.linearGradient(listOf(Forest, ForestLight))
}

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val SpaceGrotesk = FontFamily(
    variable(R.font.space_grotesk, 400),
    variable(R.font.space_grotesk, 500),
    variable(R.font.space_grotesk, 600),
    variable(R.font.space_grotesk, 700),
)

val Manrope = FontFamily(
    variable(R.font.manrope, 400),
    variable(R.font.manrope, 500),
    variable(R.font.manrope, 600),
    variable(R.font.manrope, 700),
    variable(R.font.manrope, 800),
)

/** Estilo para cifras: Space Grotesk con números tabulares (no "bailan" al cambiar). */
val AmountStyle = TextStyle(
    fontFamily = SpaceGrotesk,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
)

private val AppTypography: Typography = Typography().let { t ->
    fun TextStyle.display(w: FontWeight = FontWeight.Bold) = copy(fontFamily = SpaceGrotesk, fontWeight = w, letterSpacing = (-0.5).sp)
    fun TextStyle.body(w: FontWeight = FontWeight.Normal) = copy(fontFamily = Manrope, fontWeight = w)
    t.copy(
        displayLarge = t.displayLarge.display(),
        displayMedium = t.displayMedium.display(),
        displaySmall = t.displaySmall.display(),
        headlineLarge = t.headlineLarge.display(),
        headlineMedium = t.headlineMedium.display(),
        headlineSmall = t.headlineSmall.display(FontWeight.SemiBold),
        titleLarge = t.titleLarge.display(FontWeight.SemiBold),
        titleMedium = t.titleMedium.body(FontWeight.Bold),
        titleSmall = t.titleSmall.body(FontWeight.Bold),
        bodyLarge = t.bodyLarge.body(FontWeight.Medium),
        bodyMedium = t.bodyMedium.body(FontWeight.Medium),
        bodySmall = t.bodySmall.body(FontWeight.Medium),
        labelLarge = t.labelLarge.body(FontWeight.Bold),
        labelMedium = t.labelMedium.body(FontWeight.SemiBold),
        labelSmall = t.labelSmall.body(FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF14513D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F36A),
    onPrimaryContainer = Color(0xFF14251A),
    secondary = Color(0xFF3E5A4C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE8E0),
    onSecondaryContainer = Color(0xFF13211A),
    background = Color(0xFFF4F3EE),
    onBackground = Color(0xFF141A17),
    surface = Color(0xFFF4F3EE),
    onSurface = Color(0xFF141A17),
    surfaceVariant = Color(0xFFE8E7E0),
    onSurfaceVariant = Color(0xFF5E6661),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAF9F5),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEFEEE8),
    surfaceContainerHighest = Color(0xFFE8E7E0),
    outline = Color(0xFFC9C8C0),
    outlineVariant = Color(0xFFE3E2DB),
    error = Color(0xFFC8402D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD2F26B),
    onPrimary = Color(0xFF16220F),
    primaryContainer = Color(0xFF2D3B1B),
    onPrimaryContainer = Color(0xFFE6F7B4),
    secondary = Color(0xFFB6CCBF),
    onSecondary = Color(0xFF1F3329),
    secondaryContainer = Color(0xFF26352D),
    onSecondaryContainer = Color(0xFFD3E8DB),
    background = Color(0xFF0D110F),
    onBackground = Color(0xFFECEFEA),
    surface = Color(0xFF0D110F),
    onSurface = Color(0xFFECEFEA),
    surfaceVariant = Color(0xFF232A26),
    onSurfaceVariant = Color(0xFF9BA49E),
    surfaceContainerLowest = Color(0xFF0A0D0B),
    surfaceContainerLow = Color(0xFF121714),
    surfaceContainer = Color(0xFF161B18),
    surfaceContainerHigh = Color(0xFF1D2420),
    surfaceContainerHighest = Color(0xFF252C28),
    outline = Color(0xFF3A433E),
    outlineVariant = Color(0xFF262D29),
    error = Color(0xFFFF8A75),
)

@Composable
fun SaldoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

object SaldoColors {
    @Composable
    fun income(): Color = if (isSystemInDarkTheme()) Color(0xFF6EE7A8) else Color(0xFF1E8E5A)

    @Composable
    fun expense(): Color = if (isSystemInDarkTheme()) Color(0xFFFF8A75) else Color(0xFFD9503A)

    @Composable
    fun warning(): Color = if (isSystemInDarkTheme()) Color(0xFFFFC56B) else Color(0xFFD98A00)

    @Composable
    fun transfer(): Color = if (isSystemInDarkTheme()) Color(0xFF9BB8FF) else Color(0xFF3D6FD6)

    @Composable
    fun forType(type: TxType): Color = when (type) {
        TxType.INCOME -> income()
        TxType.EXPENSE -> expense()
        TxType.TRANSFER -> transfer()
    }
}
