package com.andres.saldo.data

/**
 * Claves de icono que se guardan en base de datos. La traducción a gráficos vive en ui/AppIcons.
 * Se guarda una clave (no el icono) para que el backup siga siendo legible y estable.
 */
object IconKeys {
    val category = listOf(
        "cart", "restaurant", "coffee", "bar", "train", "car", "fuel", "flight", "hotel",
        "home", "bolt", "wifi", "phone", "subscriptions", "games", "movie", "music", "shirt",
        "gym", "health", "pharmacy", "beauty", "gift", "pets", "kids", "education", "book",
        "tools", "savings", "other",
        "work", "payments", "insights", "refund", "sell", "plus",
    )

    val account = listOf("card", "wallet", "bank", "savings", "cash", "invest")

    val all: Set<String> = (category + account).toSet()

    /** Iconos de la versión 1 (emojis) → claves nuevas. */
    private val legacy = mapOf(
        "🛒" to "cart", "🍔" to "restaurant", "☕" to "coffee", "🚆" to "train", "🚗" to "car",
        "🏠" to "home", "📱" to "phone", "🎮" to "games", "👕" to "shirt", "🏋️" to "gym", "🏋" to "gym",
        "💊" to "health", "🎁" to "gift", "📦" to "other", "💼" to "work", "💰" to "payments",
        "↩️" to "refund", "↩" to "refund", "➕" to "plus", "💳" to "card", "💶" to "cash",
        "🏦" to "bank", "🏷️" to "other", "🏷" to "other",
    )

    fun normalizeCategory(key: String): String =
        if (key in all) key else legacy[key] ?: "other"

    fun normalizeAccount(key: String): String =
        if (key in all) key else legacy[key] ?: "wallet"
}
