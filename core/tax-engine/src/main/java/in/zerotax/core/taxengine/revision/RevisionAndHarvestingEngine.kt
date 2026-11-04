package `in`.zerotax.core.taxengine.revision

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

enum class ItrUWindow(val rate: Double) {
    WITHIN_12_MONTHS_FROM_AY_END(0.25),
    BETWEEN_12_AND_24_MONTHS_FROM_AY_END(0.50),
    BETWEEN_24_AND_36_MONTHS(0.60),
    BETWEEN_36_AND_48_MONTHS(0.70)
}

data class ItrUUpdatedReturnResult(
    val baseAdditionalTax: Long,
    val totalInterest: Long,
    val additionalTaxUs140B: Long,
    val totalUpdatedReturnPayable: Long
)

data class TaxHarvestRecommendation(
    val scripSymbol: String,
    val unrealizedLossRupees: Long,
    val harvestableAgainstGain: Long,
    val projectedTaxSavings: Long
)

data class RevisedReturnDelta(
    val originalTotalTax: Long,
    val revisedTotalTax: Long,
    val netDifferentialPayable: Long,
    val netRefundDue: Long,
    val requiresSec140APenaltyInterest: Boolean
)

object RevisionAndHarvestingEngine {

    fun computeUpdatedReturnItrU(
        baseAdditionalTax: Long,
        totalInterest234A_B_C: Long,
        filingWindow: ItrUWindow
    ): ItrUUpdatedReturnResult {
        val baseTaxAndInterest = baseAdditionalTax + totalInterest234A_B_C
        val additionalTax = (baseTaxAndInterest * filingWindow.rate).roundToLong()
        val totalPayable = baseTaxAndInterest + additionalTax

        return ItrUUpdatedReturnResult(
            baseAdditionalTax = baseAdditionalTax,
            totalInterest = totalInterest234A_B_C,
            additionalTaxUs140B = additionalTax,
            totalUpdatedReturnPayable = totalPayable
        )
    }

    fun computeMarchLtcgHarvestingHeadroom(
        realizedLtcgSoFarRupees: Long,
        unrealizedLtcgAvailableRupees: Long
    ): Pair<Long, Long> {
        val sec112AExemptionCap = 1_25_000L
        val remainingHeadroom = max(0L, sec112AExemptionCap - realizedLtcgSoFarRupees)
        val recommendedHarvest = min(remainingHeadroom, unrealizedLtcgAvailableRupees)
        val taxSaved = (recommendedHarvest * 0.125).roundToLong()
        return recommendedHarvest to taxSaved
    }

    fun calculateHarvestingOpportunities(
        realizedShortTermGains: Long,
        unrealizedScripLosses: Map<String, Long>,
        stcgRatePercent: Double = 20.0
    ): List<TaxHarvestRecommendation> {
        var remainingGainToOffset = realizedShortTermGains
        val recommendations = mutableListOf<TaxHarvestRecommendation>()

        for ((symbol, loss) in unrealizedScripLosses) {
            if (remainingGainToOffset <= 0L) break
            val usableLoss = min(loss, remainingGainToOffset)
            val savings = (usableLoss * (stcgRatePercent / 100.0)).toLong()
            recommendations.add(
                TaxHarvestRecommendation(
                    scripSymbol = symbol,
                    unrealizedLossRupees = loss,
                    harvestableAgainstGain = usableLoss,
                    projectedTaxSavings = savings
                )
            )
            remainingGainToOffset -= usableLoss
        }
        return recommendations
    }

    fun computeRevisionDifferential(
        originalTaxPayable: Long,
        revisedTaxPayable: Long
    ): RevisedReturnDelta {
        val diff = revisedTaxPayable - originalTaxPayable
        return if (diff >= 0) {
            RevisedReturnDelta(
                originalTotalTax = originalTaxPayable,
                revisedTotalTax = revisedTaxPayable,
                netDifferentialPayable = diff,
                netRefundDue = 0L,
                requiresSec140APenaltyInterest = diff > 0L
            )
        } else {
            RevisedReturnDelta(
                originalTotalTax = originalTaxPayable,
                revisedTotalTax = revisedTaxPayable,
                netDifferentialPayable = 0L,
                netRefundDue = -diff,
                requiresSec140APenaltyInterest = false
            )
        }
    }
}

