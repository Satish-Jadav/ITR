package in.zerotax.core.taxengine.advanced

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

object CapitalGainsAndForeignAssetEngine {
    private const val JAN_31_2018_EPOCH_DAY = 17562L // 2018-01-31

    fun computeGrandfathered112AGain(
        acquisitionDateEpochDay: Long,
        actualCostTotal: Long,
        fmvJan31_2018Total: Long,
        saleConsiderationTotal: Long,
        transferExpenses: Long
    ): Long {
        val effectiveCost = if (acquisitionDateEpochDay <= JAN_31_2018_EPOCH_DAY) {
            max(actualCostTotal, min(fmvJan31_2018Total, saleConsiderationTotal))
        } else actualCostTotal
        return saleConsiderationTotal - effectiveCost - transferExpenses
    }

    fun computeForm67ForeignTaxCredit(
        foreignDividendUsd: Double,
        foreignTaxWithheldUsd: Double,
        precedingMonthEndSbiTtbr: Double,
        indianEffectiveTaxRateOnTotalIncome: Double
    ): Pair<Long, Long> {
        val grossDividendInr = (foreignDividendUsd * precedingMonthEndSbiTtbr).roundToLong()
        val foreignTaxPaidInr = (foreignTaxWithheldUsd * precedingMonthEndSbiTtbr).roundToLong()
        val indianTaxOnForeignIncome = (grossDividendInr * indianEffectiveTaxRateOnTotalIncome).roundToLong()
        return grossDividendInr to min(foreignTaxPaidInr, indianTaxOnForeignIncome)
    }
}
