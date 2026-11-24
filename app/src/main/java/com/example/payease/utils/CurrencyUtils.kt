package com.example.payease.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {

    // Strict regex for positive monetary amounts with up to 2 decimal places: e.g. "500", "500.5", "500.50"
    private val STRICT_AMOUNT_REGEX = "^(?:[1-9]\\d{0,11}|0)(?:\\.\\d{1,2})?$".toRegex()

    fun formatPaiseToRupees(paise: Long): String {
        val rupeesDecimal = BigDecimal(paise).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        return format.format(rupeesDecimal)
    }

    fun rupeesToPaise(rupeesStr: String): Long {
        if (rupeesStr.isBlank()) return 0L

        // Strip currency symbol '₹', spaces, and thousand separator ','
        val clean = rupeesStr.replace("₹", "").replace(",", "").trim()
        if (!STRICT_AMOUNT_REGEX.matches(clean)) return 0L

        return try {
            val decimal = BigDecimal(clean)
            if (decimal <= BigDecimal.ZERO) return 0L

            val paiseDecimal = decimal.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)

            val maxLongAsDecimal = BigDecimal(Long.MAX_VALUE)
            if (paiseDecimal > maxLongAsDecimal) return 0L

            paiseDecimal.longValueExact()
        } catch (_: Exception) {
            0L
        }
    }
}
