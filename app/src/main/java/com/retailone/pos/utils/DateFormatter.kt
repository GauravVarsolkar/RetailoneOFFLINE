package com.retailone.pos.utils

import com.retailone.pos.models.ZModels.DateModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


import java.math.BigDecimal
import java.text.DecimalFormat



class DateFormatter(dateString: String) {
    private val dateObject: Date = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(dateString)
    private val calendar: Calendar = Calendar.getInstance().apply { time = dateObject }

    fun formatDateModel(): DateModel {
        val monthNames = arrayOf(
            "JAN", "FEB", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        val formattedMonth = monthNames[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return DateModel(year, formattedMonth, day)
    }


    // Converts "16.5", "16,5", "16.50%", or "165" -> "16.5"
    private fun formatTaxForDisplay(raw: Any?): String {
        val s0 = raw?.toString()?.trim().orEmpty()
        if (s0.isEmpty()) return "0"

        // keep digits and one decimal separator; accept comma or dot
        val s1 = s0.replace(Regex("[^0-9.,]"), "").replace(',', '.')
        if (s1.isEmpty() || s1 == ".") return "0"

        // already has decimal separator — format nicely
        if (s1.contains('.')) {
            return try {
                BigDecimal(s1).stripTrailingZeros().toPlainString()
            } catch (_: Exception) {
                s1
            }
        }

        // no decimal point -> legacy value (e.g., 165 should be 16.5)
        val n = s1.toLongOrNull() ?: return s1

        // Heuristic: older code multiplied % by 10 (16.5 -> 165)
        // If your data was multiplied by 100 instead, change 10.0 to 100.0 below.
        val scaled = n / 10.0
        return DecimalFormat("#.##").format(scaled)
    }

}