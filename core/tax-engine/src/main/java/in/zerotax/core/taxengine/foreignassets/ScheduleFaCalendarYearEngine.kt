package `in`.zerotax.core.taxengine.foreignassets

import `in`.zerotax.core.model.ForeignEquityLotInput
import kotlin.math.max
import kotlin.math.roundToLong

interface SbiTtbrLookupRepository {
    fun getUsdTtbrOnOrBefore(isoDate: String): Double
}

data class ScheduleFaTableA3Row(
    val countryCode: String = "2",
    val entityName: String,
    val initialValueInr: Long,
    val peakValueInr: Long,
    val closingValueInr: Long,
    val grossDividendsPaidInr: Long,
    val proceedsFromSaleInr: Long
)

data class ForeignEquityHolding(
    val companyName: String,
    val countryCode: String,
    val dateOfAcquisitionEpochDay: Long,
    val peakValueUsd: Double,
    val closingValueUsd: Double,
    val grossDividendsUsd: Double,
    val grossSalesProceedsUsd: Double
)

class ScheduleFaCalendarYearEngine(
    private val sbiTtbrRepo: SbiTtbrLookupRepository? = null
) {
    fun buildTableA3RowForLot(
        lot: ForeignEquityLotInput,
        calendarYear: Int = 2025
    ): ScheduleFaTableA3Row {
        val repo = sbiTtbrRepo ?: object : SbiTtbrLookupRepository {
            override fun getUsdTtbrOnOrBefore(isoDate: String): Double = 86.80
        }

        // 1. Initial Value using Acquisition Date SBI TTBR (weekend/holiday rollback)
        val acquireTtbr = repo.getUsdTtbrOnOrBefore(lot.acquireDateIso)
        val initialUsd = lot.sharesVestedOrBought * lot.acquisitionPricePerShareUsd
        val initialInr = (initialUsd * acquireTtbr).roundToLong()

        // 2. Peak Value using Peak Date SBI TTBR
        val peakDate = if (lot.peakSharePriceDateIso.isNotBlank()) lot.peakSharePriceDateIso else "${calendarYear}-12-31"
        val peakTtbr = repo.getUsdTtbrOnOrBefore(peakDate)
        val peakPrice = if (lot.peakSharePriceDuringHoldingPeriodUsd > 0.0) lot.peakSharePriceDuringHoldingPeriodUsd else lot.acquisitionPricePerShareUsd
        val peakUsd = lot.sharesVestedOrBought * peakPrice
        val peakInr = (peakUsd * peakTtbr).roundToLong()

        // 3. Closing Value using Dec 31 SBI TTBR
        val dec31Ttbr = repo.getUsdTtbrOnOrBefore("${calendarYear}-12-31")
        val heldShares = max(0.0, lot.sharesVestedOrBought - lot.sharesSoldDuringCalendarYear)
        val closingUsd = heldShares * lot.dec31ClosingSharePriceUsd
        val closingInr = (closingUsd * dec31Ttbr).roundToLong()

        val dividendsInr = (lot.grossDividendsReceivedUsd * dec31Ttbr).roundToLong()
        val salesInr = (lot.grossSaleProceedsUsd * dec31Ttbr).roundToLong()

        return ScheduleFaTableA3Row(
            countryCode = lot.countryCodeCbdt,
            entityName = lot.companyName,
            initialValueInr = initialInr,
            peakValueInr = peakInr,
            closingValueInr = closingInr,
            grossDividendsPaidInr = dividendsInr,
            proceedsFromSaleInr = salesInr
        )
    }

    companion object {
        fun compileScheduleFaEntries(
            holdings: List<ForeignEquityHolding>,
            sbiTtbrClosingDateRate: Double,
            sbiTtbrPeakDateRate: Double
        ): List<ScheduleFaTableA3Row> {
            return holdings.map { holding ->
                val peakInr = (holding.peakValueUsd * sbiTtbrPeakDateRate).roundToLong()
                val closingInr = (holding.closingValueUsd * sbiTtbrClosingDateRate).roundToLong()
                val dividendsInr = (holding.grossDividendsUsd * sbiTtbrClosingDateRate).roundToLong()
                val salesInr = (holding.grossSalesProceedsUsd * sbiTtbrClosingDateRate).roundToLong()

                ScheduleFaTableA3Row(
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
}

