package `in`.zerotax.core.taxengine

import `in`.zerotax.core.taxengine.advanced.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScheduleAlAndCapitalGainsWealthEngineTest {

    @Test
    fun testScheduleAlBuilder_MandatoryAbove50Lakhs() {
        val immovable = listOf(
            ImmovableAssetCostEntry("Apartment", "Indiranagar, Bengaluru", 80_00_000L),
            ImmovableAssetCostEntry("Plot", "Whitefield, Bengaluru", 40_00_000L)
        )
        val movable = listOf(
            MovableAssetCostEntry("SHARES_AND_MFS", 25_00_000L),
            MovableAssetCostEntry("BANK_DEPOSITS_CASH", 15_00_000L)
        )
        val liabilities = listOf(
            LiabilityCostEntry("HOUSING_LOAN", 50_00_000L)
        )

        val resultAbove50L = ScheduleAlAndCapitalGainsWealthEngine.buildScheduleAl(
            totalIncomeRupees = 65_00_000L,
            immovableAssets = immovable,
            movableAssets = movable,
            liabilities = liabilities
        )

        assertTrue(resultAbove50L.isScheduleAlMandatory)
        assertEquals(1_20_00_000L, resultAbove50L.totalImmovableAssetsAtCost)
        assertEquals(40_00_000L, resultAbove50L.totalMovableAssetsAtCost)
        assertEquals(1_60_00_000L, resultAbove50L.totalAssetsAtCost)
        assertEquals(50_00_000L, resultAbove50L.totalLiabilitiesAtCost)
        assertEquals(1_10_00_000L, resultAbove50L.netWealthAtCostRupees)
        assertTrue(resultAbove50L.cbdtJsonScheduleAlNode.contains("ScheduleAL"))

        val resultBelow50L = ScheduleAlAndCapitalGainsWealthEngine.buildScheduleAl(
            totalIncomeRupees = 30_00_000L,
            immovableAssets = immovable,
            movableAssets = movable,
            liabilities = liabilities
        )
        assertFalse(resultBelow50L.isScheduleAlMandatory)
    }

    @Test
    fun testSection50CSafeHarbor_10PercentToleranceShield() {
        // Case 1: Circle rate is ₹1.08 Cr, agreement price is ₹1.00 Cr (8% variance <= 10% safe harbor)
        val protectedResult = ScheduleAlAndCapitalGainsWealthEngine.verifySection50CSafeHarbor(
            actualAgreementConsideration = 1_00_00_000L,
            stampDutyCircleRateValue = 1_08_00_000L
        )
        assertTrue(protectedResult.isProtectedUnder10PercentSafeHarbor)
        assertEquals(1_00_00_000L, protectedResult.effectiveFullValueOfConsiderationAdopted)
        assertTrue(protectedResult.statutoryExplanation.contains("Third Proviso to Section 50C(1)"))

        // Case 2: Circle rate is ₹1.25 Cr, agreement price is ₹1.00 Cr (25% variance > 10% safe harbor)
        val unprotectedResult = ScheduleAlAndCapitalGainsWealthEngine.verifySection50CSafeHarbor(
            actualAgreementConsideration = 1_00_00_000L,
            stampDutyCircleRateValue = 1_25_00_000L
        )
        assertFalse(unprotectedResult.isProtectedUnder10PercentSafeHarbor)
        assertEquals(1_25_00_000L, unprotectedResult.effectiveFullValueOfConsiderationAdopted)
    }

    @Test
    fun testPostOct2024ShareBuyback_MapsToDeemedDividendAndCapitalLoss() {
        val buybackInput = ShareBuybackInput(
            companyName = "Infosys Ltd",
            sharesTendered = 100L,
            originalPurchaseCostPerShareRupees = 1_400L, // Total cost = ₹1,40,000
            buybackPricePerShareRupees = 1_800L,         // Total proceeds = ₹1,80,000
            isHoldingPeriodOver12Months = true
        )

        val result = ScheduleAlAndCapitalGainsWealthEngine.computePostOct2024ShareBuyback(buybackInput)

        // Deemed dividend u/s 2(22)(f) = full proceeds ₹1,80,000
        assertEquals(1_80_000L, result.deemedDividendIncomeScheduleOs)
        // Capital loss u/s 46A = original cost ₹1,40,000
        assertEquals(1_40_000L, result.capitalLossTreatedScheduleCg)
        assertTrue(result.isLongTermLoss)
        assertTrue(result.statutoryExplanation.contains("Section 2(22)(f)"))
        assertTrue(result.statutoryExplanation.contains("Section 46A"))
    }

    @Test
    fun testCapitalGainsReinvestmentPlanner_Enforces10CrAnd50LStatutoryCaps() {
        val planInput = CapitalGainsReinvestmentPlanInput(
            netSaleConsiderationRupees = 15_00_00_000L,
            grossLongTermCapitalGainRupees = 12_00_00_000L, // ₹12 Crores LTCG
            actualAmountReinvestedInNewHouseRupees = 11_00_00_000L, // ₹11 Cr reinvested (exceeds ₹10 Cr cap)
            amountDepositedInCgas1988Rupees = 0L,
            amountInvestedInSec54EcBondsRupees = 75_00_000L, // ₹75 Lakhs in 54EC (exceeds ₹50L cap)
            monthsElapsedFromSaleDateTo54EcInvestment = 4 // within 6 months
        )

        val result = ScheduleAlAndCapitalGainsWealthEngine.planCapitalGainsReinvestments(planInput)

        // Section 54 must be clamped to ₹10 Crores
        assertEquals(10_00_00_000L, result.section54ResidentialExemptionClaimed)
        // Section 54EC must be clamped to ₹50 Lakhs
        assertEquals(50_00_000L, result.section54EcBondExemptionClaimed)
        // Total exemptions = 10.5 Crores
        assertEquals(10_50_00_000L, result.totalExemptionsClaimed)
        // Net taxable LTCG = 12 Cr - 10.5 Cr = 1.5 Crores
        assertEquals(1_50_00_000L, result.netTaxableLtcgAfterExemptions)

        assertTrue(result.auditDirectives.any { it.contains("capped at ₹10 Crores") })
        assertTrue(result.auditDirectives.any { it.contains("capped at ₹50 Lakhs") })
    }
}

