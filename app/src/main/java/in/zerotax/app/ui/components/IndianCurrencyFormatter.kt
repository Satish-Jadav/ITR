package `in`.zerotax.app.ui.components

import java.text.DecimalFormat
import java.util.Locale

object IndianCurrencyFormatter {
    fun formatRupees(amount: Long): String {
        if (amount == 0L) return "₹0"
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val s = absAmount.toString()

        val formatted = if (s.length <= 3) {
            s
        } else {
            val lastThree = s.takeLast(3)
            val rest = s.dropLast(3)
            val chunks = mutableListOf<String>()
            var i = rest.length
            while (i > 0) {
                val start = kotlin.math.max(0, i - 2)
                chunks.add(0, rest.substring(start, i))
                i -= 2
            }
            chunks.joinToString(",") + "," + lastThree
        }

        return if (isNegative) "-₹$formatted" else "₹$formatted"
    }

    fun formatLakhsOrCrores(amount: Long): String {
        val absAmount = kotlin.math.abs(amount)
        return when {
            absAmount >= 1_00_00_000L -> "₹%.2f Cr".format(Locale.ENGLISH, amount / 1_00_00_000.0)
            absAmount >= 1_00_000L -> "₹%.2f L".format(Locale.ENGLISH, amount / 1_00_000.0)
            else -> formatRupees(amount)
        }
    }

    fun formatToInr(amount: Long): String = formatRupees(amount)

    fun formatLakhs(amount: Long): String = `in`.zerotax.core.model.Rupee.formatInr(amount)
}
