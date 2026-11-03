package in.zerotax.core.taxengine.advanced

import kotlin.math.max
import kotlin.math.roundToLong

data class ForeignEquityHolding(
    val companyName: String,
    val countryCode: String,
    val dateOfAcquisitionEpochDay: Long,
    val peakValueUsd: Double,
    val closingValueUsd: Double,
    val grossDividendsUsd: Double,
    val grossSalesProceedsUsd: Double
)

data class ScheduleFaTableRowA3(
    val countryCode: String,
    val entityName: String,
    val initialValueInr: Long,
    val peakValueInr: Long,
    val closingValueInr: Long,
    val grossDividendsPaidInr: Long,
    val proceedsFromSaleInr: Long
)

object ScheduleFaCalendarYearEngine {
    // Schedule FA follows calendar year ending 31-Dec preceding Assessment Year
    fun compileScheduleFaEntries(
        holdings: List<ForeignEquityHolding>,
        sbiTtbrClosingDateRate: Double,
        sbiTtbrPeakDateRate: Double
    ): List<ScheduleFaTableRowA3> {
        return holdings.map { holding ->
            val peakInr = (holding.peakValueUsd * sbiTtbrPeakDateRate).roundToLong()
            val closingInr = (holding.closingValueUsd * sbiTtbrClosingDateRate).roundToLong()
            val dividendsInr = (holding.grossDividendsUsd * sbiTtbrClosingDateRate).roundToLong()
            val salesInr = (holding.grossSalesProceedsUsd * sbiTtbrClosingDateRate).roundToLong()

            ScheduleFaTableRowA3(
                countryCode = holding.countryCode,
                entityName = holding.companyName,
                initialValueInr = max(0L, closingInr - salesInr),
                peakValueInr = peakInr,
                closingValueInr = closingInr,
                grossDividendsPaidInr = dividendsInr,
                proceedsFromSaleInr = salesInr
            )
        }
    }
}
