package `in`.zerotax.core.model

import kotlinx.serialization.Serializable

/**
 * Anonymized, PII-scrubbed snapshot of the taxpayer's live financial position.
 * Generated dynamically from ZeroTaxVaultDatabase to provide context to the Copilot
 * without sending PAN, Aadhaar, full name, or employer identity.
 */
@Serializable
data class TaxpayerAnonymizedSnapshot(
    val assessmentYear: String = "2026-27",
    val ageCategory: CitizenCategory = CitizenCategory.REGULAR_BELOW_60,
    val activeRegime: TaxRegime = TaxRegime.NEW_REGIME_115BAC,
    val grossSalaryRupees: Long = 0L,
    val standardDeductionRupees: Long = 75_000L,
    val claimed80CRupees: Long = 0L,
    val claimed80DSelfRupees: Long = 0L,
    val claimed80DParentsRupees: Long = 0L,
    val claimedNps80CCD1BRupees: Long = 0L,
    val claimedEmployerNps80CCD2Rupees: Long = 0L,
    val claimedHraExemptionRupees: Long = 0L,
    val claimedHomeLoanInterest24bRupees: Long = 0L,
    val claimed80TTARupees: Long = 0L,
    val totalDeductionsRupees: Long = 75_000L,
    val netTaxableIncomeRupees: Long = 0L,
    val tdsPaidRupees: Long = 0L,
    val currentTaxLiabilityRupees: Long = 0L,
    val estimatedRefundRupees: Long = 0L,
    val hasCapitalGains: Boolean = false,
    val hasBusinessOrFreelance: Boolean = false,
    val hasForeignAssets: Boolean = false,
    val unclaimed80CHeadroom: Long = 150_000L,
    val unclaimedNpsHeadroom: Long = 50_000L
)

@Serializable
data class WhatIfDeltas(
    val salaryDeltaRupees: Long = 0L,
    val sec80CRupees: Long = 0L,
    val sec80DSelfRupees: Long = 0L,
    val sec80DParentsSeniorRupees: Long = 0L,
    val nps80CCD1BRupees: Long = 0L,
    val employerNps80CCD2Rupees: Long = 0L,
    val monthlyRentRupees: Long = 0L,
    val isMetroCity: Boolean = false,
    val homeLoanInterest24bRupees: Long = 0L,
    val savingsInterestRupees: Long = 0L
)

@Serializable
data class WhatIfSimulationResult(
    val beforeOldRegimeTax: Long,
    val beforeNewRegimeTax: Long,
    val afterOldRegimeTax: Long,
    val afterNewRegimeTax: Long,
    val recommendedRegime: TaxRegime,
    val netTaxSavedRupees: Long,
    val totalDeductionsClaimed: Long,
    val deltaSummaryMarkdown: String,
    val statutoryCitations: List<String> = emptyList(),
    val appliedDeltas: WhatIfDeltas = WhatIfDeltas(),
    val actionableGuidance: List<String> = emptyList(),
    val originalTotalTaxRupees: Long = minOf(beforeOldRegimeTax, beforeNewRegimeTax),
    val simulatedTotalTaxRupees: Long = minOf(afterOldRegimeTax, afterNewRegimeTax),
    val taxDeltaRupees: Long = -netTaxSavedRupees,
    val isTaxSaved: Boolean = netTaxSavedRupees > 0L,
    val recommendedRegimeBefore: TaxRegime = if (beforeOldRegimeTax < beforeNewRegimeTax) TaxRegime.OLD_REGIME else TaxRegime.NEW_REGIME_115BAC,
    val recommendedRegimeAfter: TaxRegime = recommendedRegime,
    val naturalLanguageSummary: String = deltaSummaryMarkdown
)

@Serializable
sealed interface CopilotToolCall {
    @Serializable
    data class SimulateWhatIf(val deltas: WhatIfDeltas) : CopilotToolCall

    @Serializable
    data class OptimizeCtc(val grossCtc: Long, val basicSalary: Long) : CopilotToolCall

    @Serializable
    data class AnalyzeNotice143_1(val noticeType: String, val demandAmount: Long) : CopilotToolCall

    @Serializable
    data class CheckBusiness44(
        val grossTurnover: Long,
        val isDigital: Boolean,
        val professionCode: String? = null
    ) : CopilotToolCall

    @Serializable
    data class CalculatePropertyLtcg(
        val purchaseCost: Long,
        val purchaseYear: Int,
        val saleValue: Long,
        val saleYear: Int
    ) : CopilotToolCall

    @Serializable
    data class MutateReturnField(
        val fieldKey: String,
        val newValueRupees: Long,
        val targetSchedule: String
    ) : CopilotToolCall

    @Serializable
    data class TriggerUiAction(val action: String) : CopilotToolCall
}

@Serializable
sealed interface CopilotActionCardData {
    @Serializable
    data class RegimeComparison(
        val oldRegimeTax: Long,
        val newRegimeTax: Long,
        val recommendedRegime: TaxRegime,
        val netDifferenceRupees: Long,
        val rationaleMarkdown: String
    ) : CopilotActionCardData

    @Serializable
    data class WhatIfResult(val result: WhatIfSimulationResult) : CopilotActionCardData

    @Serializable
    data class UnclaimedDeduction(
        val title: String,
        val statutorySection: String,
        val eligibleAmount: Long,
        val potentialSavingsRupees: Long,
        val deductionKey: String
    ) : CopilotActionCardData

    @Serializable
    data class CtcOptimization(
        val grossCtc: Long,
        val currentTax: Long,
        val optimizedTax: Long,
        val employerNps80CCD2: Long,
        val annualTaxSavings: Long,
        val recommendations: List<String>
    ) : CopilotActionCardData

    @Serializable
    data class NoticeDefense(
        val sectionRef: String,
        val varianceAmount: Long,
        val reasonSummary: String,
        val recommendedAction: String
    ) : CopilotActionCardData
}
