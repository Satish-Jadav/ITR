package `in`.zerotax.feature.aiextractor

import `in`.zerotax.core.model.DualRegimeComparison
import `in`.zerotax.core.model.TaxRegime
import `in`.zerotax.core.taxengine.calculator.DeterministicIndianTaxEngine
import `in`.zerotax.core.taxengine.calculator.TaxpayerFinancialInput
import kotlin.math.abs
import kotlin.math.max

/**
 * Feature F-15: 100% Offline Natural-Language "What-If" Tax Scenario Simulator.
 * Evaluates hypothetical financial decisions in real-time (< 5ms) against the deterministic tax engine.
 */
data class WhatIfSimulationResult(
    val originalTotalTaxRupees: Long,
    val simulatedTotalTaxRupees: Long,
    val taxDeltaRupees: Long, // negative = tax saved, positive = additional tax
    val isTaxSaved: Boolean,
    val recommendedRegimeBefore: TaxRegime,
    val recommendedRegimeAfter: TaxRegime,
    val naturalLanguageSummary: String,
    val actionableGuidance: List<String>
)

object OfflineWhatIfTaxSimulator {

    private val RUPEE_AMOUNT_REGEX = Regex("""(?i)(?:rs\.?|₹|inr)?\s*([0-9]+(?:,[0-9]+)*(?:\.[0-9]+)?)\s*(?:lakhs?|lacs?|k|crores?|cr)?""")

    fun simulateWhatIfScenario(
        baseInput: TaxpayerFinancialInput,
        scenarioQuery: String
    ): WhatIfSimulationResult {
        val q = scenarioQuery.lowercase()
        val extractedAmount = parseAmountFromQuery(scenarioQuery)

        // Compute baseline before scenario
        val baseline = DeterministicIndianTaxEngine.compareRegimes(baseInput)
        val baselineTax = minOf(
            baseline.newRegimeResult.totalTaxLiabilityRounded288B,
            baseline.oldRegimeResult.totalTaxLiabilityRounded288B
        )

        var modifiedInput = baseInput
        val guidance = mutableListOf<String>()
        var scenarioDescription = "Simulation based on your query"

        when {
            // Case 1: Health Insurance (80D)
            q.contains("health") || q.contains("mediclaim") || q.contains("80d") || q.contains("medical") -> {
                val add80D = if (extractedAmount > 0L) extractedAmount else 25_000L
                val current80D = baseInput.section80DSelfAndFamily
                val updated80D = minOf(current80D + add80D, 1_00_000L)
                modifiedInput = baseInput.copy(section80DSelfAndFamily = updated80D)
                scenarioDescription = "Purchased ₹$add80D additional health insurance for family/parents u/s 80D."
                guidance.add("Section 80D deduction applies in the Old Tax Regime up to ₹25,000 (below 60) or ₹50,000 (senior citizens).")
            }

            // Case 2: NPS Tier-I (80CCD(1B))
            q.contains("nps") || q.contains("80ccd") || q.contains("pension") -> {
                val addNps = if (extractedAmount > 0L) extractedAmount else 50_000L
                val updatedNps = minOf(baseInput.section80CCD1BSelfNps + addNps, 50_000L)
                modifiedInput = baseInput.copy(section80CCD1BSelfNps = updatedNps)
                scenarioDescription = "Contributed ₹$addNps to NPS Tier-I under Section 80CCD(1B)."
                guidance.add("NPS Tier-I offers an exclusive ₹50,000 deduction over and above Section 80C's ₹1.5L limit in the Old Regime.")
            }

            // Case 3: Section 80C Investments (PPF, ELSS, EPF, Life Insurance)
            q.contains("elss") || q.contains("ppf") || q.contains("80c") || q.contains("lic") -> {
                val add80C = if (extractedAmount > 0L) extractedAmount else 50_000L
                val updated80C = minOf(baseInput.section80CInvestments + add80C, 1_50_000L)
                modifiedInput = baseInput.copy(section80CInvestments = updated80C)
                scenarioDescription = "Invested ₹$add80C in Section 80C instruments (PPF/ELSS/Life Insurance)."
                guidance.add("Section 80C has an aggregate statutory cap of ₹1,50,000 in the Old Tax Regime.")
            }

            // Case 4: Home Loan Interest (Section 24(b))
            q.contains("home loan") || q.contains("housing loan") || q.contains("interest on loan") || q.contains("24b") -> {
                val addInterest = if (extractedAmount > 0L) extractedAmount else 2_00_000L
                val updatedInterest = minOf(baseInput.selfOccupiedHomeLoanInterestSec24b + addInterest, 2_00_000L)
                modifiedInput = baseInput.copy(selfOccupiedHomeLoanInterestSec24b = updatedInterest)
                scenarioDescription = "Claimed ₹$addInterest home loan interest on self-occupied house u/s 24(b)."
                guidance.add("Section 24(b) allows deduction of home loan interest on a self-occupied property up to ₹2,00,000 in the Old Tax Regime.")
            }

            // Case 5: Freelance / Presumptive Consulting (44ADA)
            q.contains("freelance") || q.contains("consult") || q.contains("44ada") || q.contains("contract") -> {
                val addGross = if (extractedAmount > 0L) extractedAmount else 5_00_000L
                val updatedGross = baseInput.presumptiveGrossReceipts44ADA + addGross
                modifiedInput = baseInput.copy(presumptiveGrossReceipts44ADA = updatedGross)
                scenarioDescription = "Added ₹$addGross in freelance consulting receipts under Section 44ADA."
                guidance.add("Under Section 44ADA, exactly 50% (₹${addGross / 2}) is taxable profit, and 50% is treated as statutory expense with zero receipt bookkeeping.")
            }

            // Default: Salary change / General income
            else -> {
                val addIncome = if (extractedAmount > 0L) extractedAmount else 1_00_000L
                val updatedSalary = baseInput.grossSalary + addIncome
                modifiedInput = baseInput.copy(grossSalary = updatedSalary)
                scenarioDescription = "Increased gross income by ₹$addIncome."
                guidance.add("Income is taxed according to your progressive marginal tax bracket.")
            }
        }

        // Compute simulated result after scenario
        val simulated = DeterministicIndianTaxEngine.compareRegimes(modifiedInput)
        val simulatedTax = minOf(
            simulated.newRegimeResult.totalTaxLiabilityRounded288B,
            simulated.oldRegimeResult.totalTaxLiabilityRounded288B
        )

        val delta = simulatedTax - baselineTax
        val isTaxSaved = delta < 0L

        val summary = if (isTaxSaved) {
            "$scenarioDescription Result: You SAVE ₹${abs(delta)} in total tax liability (Tax reduces from ₹$baselineTax to ₹$simulatedTax)!"
        } else if (delta > 0L) {
            "$scenarioDescription Result: Your tax increases by ₹$delta (from ₹$baselineTax to ₹$simulatedTax)."
        } else {
            "$scenarioDescription Result: No change in final tax liability (Remains ₹$baselineTax)."
        }

        return WhatIfSimulationResult(
            originalTotalTaxRupees = baselineTax,
            simulatedTotalTaxRupees = simulatedTax,
            taxDeltaRupees = delta,
            isTaxSaved = isTaxSaved,
            recommendedRegimeBefore = baseline.recommendedRegime,
            recommendedRegimeAfter = simulated.recommendedRegime,
            naturalLanguageSummary = summary,
            actionableGuidance = guidance
        )
    }

    private fun parseAmountFromQuery(query: String): Long {
        val q = query.replace(",", "").lowercase()

        // Check for Lakhs / Lacs
        val lakhMatch = Regex("""([0-9]+(?:\.[0-9]+)?)\s*(?:lakh|lac)""").find(q)
        if (lakhMatch != null) {
            val num = lakhMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            return (num * 1_00_000.0).toLong()
        }

        // Check for K (Thousands)
        val kMatch = Regex("""([0-9]+(?:\.[0-9]+)?)\s*(?:k|thousand)""").find(q)
        if (kMatch != null) {
            val num = kMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            return (num * 1_000.0).toLong()
        }

        // Check for Crores
        val crMatch = Regex("""([0-9]+(?:\.[0-9]+)?)\s*(?:cr|crore)""").find(q)
        if (crMatch != null) {
            val num = crMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            return (num * 1_00_00_000.0).toLong()
        }

        // Check for raw number
        val rawNumberMatch = Regex("""\b([0-9]{4,9})\b""").find(q)
        if (rawNumberMatch != null) {
            return rawNumberMatch.groupValues[1].toLongOrNull() ?: 0L
        }

        return 0L
    }
}
