package `in`.zerotax.core.model

import kotlinx.serialization.Serializable
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Serializable
@JvmInline
value class Rupee(val amount: Long) : Comparable<Rupee> {

    operator fun plus(other: Rupee): Rupee = Rupee(this.amount + other.amount)
    operator fun minus(other: Rupee): Rupee = Rupee(this.amount - other.amount)
    operator fun times(multiplier: Long): Rupee = Rupee(this.amount * multiplier)
    operator fun times(multiplier: Double): Rupee = Rupee(kotlin.math.round(this.amount * multiplier).toLong())
    operator fun div(divisor: Long): Rupee = Rupee(this.amount / divisor)

    fun coerceAtLeast(minimumValue: Rupee): Rupee = Rupee(this.amount.coerceAtLeast(minimumValue.amount))
    fun coerceAtMost(maximumValue: Rupee): Rupee = Rupee(this.amount.coerceAtMost(maximumValue.amount))

    /**
     * Section 288A & 288B rounding: to the nearest multiple of ten rupees.
     * If the last digit is 5 or more, rounded up to the next ten; otherwise rounded down.
     */
    fun roundToNearestTen(): Rupee {
        val remainder = amount % 10L
        val rounded = if (remainder >= 5L) amount + (10L - remainder) else amount - remainder
        return Rupee(rounded)
    }

    /**
     * Formats the rupee value in Indian Lakh/Crore numbering style (₹XX,XX,XXX).
     */
    fun toIndianFormattedString(): String {
        val symbols = DecimalFormatSymbols(Locale("en", "IN")).apply {
            currencySymbol = "₹"
        }
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val formatted = if (absAmount < 1000) {
            absAmount.toString()
        } else {
            val lastThree = absAmount.toString().takeLast(3)
            val remaining = absAmount.toString().dropLast(3)
            val chunked = remaining.reversed().chunked(2).joinToString(",").reversed()
            "$chunked,$lastThree"
        }
        return if (isNegative) "-₹$formatted" else "₹$formatted"
    }

    override fun compareTo(other: Rupee): Int = this.amount.compareTo(other.amount)

    override fun toString(): String = toIndianFormattedString()

    companion object {
        val ZERO = Rupee(0L)
    }
}

@Serializable
@JvmInline
value class Paise(val value: Long) {
    fun toRupee(): Rupee = Rupee(value / 100L)

    companion object {
        fun fromRupee(rupee: Rupee): Paise = Paise(rupee.amount * 100L)
    }
}
