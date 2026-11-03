package in.zerotax.core.taxengine.advanced

import kotlin.math.abs
import kotlin.math.min

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
        return RevisedReturnDelta(
            originalTotalTax = originalTaxPayable,
            revisedTotalTax = revisedTaxPayable,
            netDifferentialPayable = if (diff > 0) diff else 0L,
            netRefundDue = if (diff < 0) abs(diff) else 0L,
            requiresSec140APenaltyInterest = diff > 0
        )
    }
}
