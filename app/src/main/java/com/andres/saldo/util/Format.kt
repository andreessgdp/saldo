package com.andres.saldo.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

val ES: Locale = Locale.forLanguageTag("es-ES")

object Money {
    fun format(cents: Long): String {
        val f = NumberFormat.getCurrencyInstance(ES)
        f.currency = Currency.getInstance("EUR")
        return f.format(BigDecimal.valueOf(cents, 2))
    }

    /** Acepta "12,5", "12.5", "1.234,56", "-30". Devuelve céntimos o null. */
    fun parse(text: String): Long? {
        var t = text.trim().replace(" ", "").replace("€", "")
        if (t.isEmpty() || t == "-") return null
        t = if (t.contains(',')) t.replace(".", "").replace(',', '.') else t
        return t.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)?.movePointRight(2)?.toLong()
    }

    /** Céntimos → texto editable ("12,5"). */
    fun toInput(cents: Long): String =
        BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString().replace('.', ',')
}

private fun String.capFirst() = replaceFirstChar { it.titlecase(ES) }

fun monthLabel(ym: YearMonth): String =
    ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", ES)).capFirst()

fun monthOnlyLabel(ym: YearMonth): String =
    ym.format(DateTimeFormatter.ofPattern("MMMM", ES)).capFirst()

fun shortMonth(ym: YearMonth): String =
    ym.format(DateTimeFormatter.ofPattern("MMM", ES)).trimEnd('.').capFirst()

fun dayLabel(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    return when (d) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> d.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES)).capFirst()
    }
}

fun shortDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy", ES))

fun YearMonth.startDay(): Long = atDay(1).toEpochDay()
fun YearMonth.endDay(): Long = atEndOfMonth().toEpochDay()

/** Misma fecha el mes siguiente, ajustando a fin de mes (día 31 → 30/28...). */
fun nextOccurrence(from: LocalDate, dayOfMonth: Int): LocalDate {
    val ym = YearMonth.from(from).plusMonths(1)
    return ym.atDay(minOf(dayOfMonth, ym.lengthOfMonth()))
}

fun occurrenceIn(ym: YearMonth, dayOfMonth: Int): LocalDate =
    ym.atDay(minOf(dayOfMonth, ym.lengthOfMonth()))
