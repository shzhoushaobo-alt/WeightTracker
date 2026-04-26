package com.weighttracker.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class DisplayUnit(val label: String) {
    KG("公斤"),
    JIN("斤"),
}

fun Double.round2(): Double =
    BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).toDouble()

private val displayFormat: DecimalFormat by lazy {
    val sym = DecimalFormatSymbols(Locale.US)
    DecimalFormat("0.00", sym).apply { roundingMode = RoundingMode.HALF_UP }
}

fun Double.toDisplayString(): String = displayFormat.format(round2())

fun Double.kgToJin(): Double = (this * 2.0).round2()

fun Double.jinToKg(): Double = (this / 2.0).round2()

fun Double.toDisplayValue(unit: DisplayUnit): Double =
    when (unit) {
        DisplayUnit.KG -> round2()
        DisplayUnit.JIN -> kgToJin()
    }

fun parseWeightInput(text: String, unit: DisplayUnit): Double? {
    val t = text.trim().ifEmpty { return null }
    val v = t.toDoubleOrNull() ?: return null
    return when (unit) {
        DisplayUnit.KG -> v.round2()
        DisplayUnit.JIN -> v.jinToKg()
    }
}
