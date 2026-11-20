package com.example.payease.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {

    fun formatPaiseToRupees(paise: Long): String {
        val rupeesDecimal = BigDecimal(paise).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        return format.format(rupeesDecimal)
    }

    fun rupeesToPaise(rupeesStr: String): Long {
        if (rupeesStr.isBlank()) return 0L

        val clean = rupeesStr.trim().replace("[^0-9.-]".toRegex(), "")
        if (clean.isBlank()) return 0L

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
