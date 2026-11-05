package `in`.zerotax.feature.aiextractor

import `in`.zerotax.core.model.TaxRegime
import `in`.zerotax.core.taxengine.calculator.TaxpayerFinancialInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineWhatIfTaxSimulatorTest {

    private val baseSalaryInput = TaxpayerFinancialInput(
        grossSalary = 12_00_000L,
        annualHouseRentPaid = 0L,
        hraExemptionClaimed = 0L,
        section80CInvestments = 1_00_000L,
        section80DSelfAndFamily = 0L,
        section80CCD1BSelfNps = 0L,
        selfOccupiedHomeLoanInterestSec24b = 0L
    )

    @Test
    fun testNpsInvestmentScenario_CalculatesCorrectSavings() {
        val result = OfflineWhatIfTaxSimulator.simulateWhatIfScenario(
            baseSalaryInput,
            "What if I invest 50000 in NPS under 80CCD?"
        )

        assertTrue(result.naturalLanguageSummary.contains("NPS Tier-I"))
        assertTrue(result.actionableGuidance.isNotEmpty())
    }

    @Test
    fun testHealthInsuranceScenario_ParsesAmountAndEvaluates80D() {
        val result = OfflineWhatIfTaxSimulator.simulateWhatIfScenario(
            baseSalaryInput,
            "What if I take 25k health insurance mediclaim for parents?"
        )

        assertTrue(result.naturalLanguageSummary.contains("health insurance"))
        assertTrue(result.actionableGuidance.any { it.contains("Section 80D") })
    }

    @Test
    fun testHomeLoanInterestScenario_Parses2Lakhs() {
        val result = OfflineWhatIfTaxSimulator.simulateWhatIfScenario(
            baseSalaryInput,
            "What if I pay 2 lakh home loan interest on housing loan?"
        )

        assertTrue(result.naturalLanguageSummary.contains("home loan interest"))
        assertTrue(result.actionableGuidance.any { it.contains("Section 24(b)") })
    }

    @Test
    fun testFreelanceReceiptsScenario_AppliesPresumptive44ADA() {
        val freelanceInput = TaxpayerFinancialInput(
            grossSalary = 0L,
            presumptiveGrossReceipts44ADA = 10_00_000L
        )

        val result = OfflineWhatIfTaxSimulator.simulateWhatIfScenario(
            freelanceInput,
            "What if I do freelance consulting for 5 lakhs under 44ada?"
        )

        assertTrue(result.naturalLanguageSummary.contains("freelance consulting"))
        assertTrue(result.actionableGuidance.any { it.contains("50%") })
        assertTrue(result.simulatedTotalTaxRupees >= result.originalTotalTaxRupees)
    }

    @Test
    fun testAmountParsingFormats() {
        val res1 = OfflineWhatIfTaxSimulator.simulateWhatIfScenario(baseSalaryInput, "Add 1.5 lakh elss")
        assertTrue(res1.naturalLanguageSummary.contains("₹150000"))

        val res2 = OfflineWhatIfTaxSimulator.simulateWhatIfScenario(baseSalaryInput, "Add 50k nps")
        assertTrue(res2.naturalLanguageSummary.contains("₹50000"))
    }
}
