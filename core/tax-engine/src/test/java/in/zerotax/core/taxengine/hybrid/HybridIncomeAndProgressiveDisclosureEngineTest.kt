package `in`.zerotax.core.taxengine.hybrid

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HybridIncomeAndProgressiveDisclosureEngineTest {

    @Test
    fun optimizeHybridIncomeStreams_combinesSalaryAndFreelance44AdaAndCreator44Ad() {
        val streams = listOf(
            SideIncomeStreamInput(
                streamId = "S1",
                description = "Freelance UX Design",
                nature = SideIncomeNature.NOTIFIED_PROFESSION_44ADA_50PCT,
                grossReceiptsDigitalRupees = 10_00_000L
            ),
            SideIncomeStreamInput(
                streamId = "S2",
                description = "YouTube Channel",
                nature = SideIncomeNature.DIGITAL_BUSINESS_OR_CREATOR_44AD_6PCT,
                grossReceiptsDigitalRupees = 5_00_000L
            ),
            SideIncomeStreamInput(
                streamId = "S3",
                description = "University Guest Lecture",
                nature = SideIncomeNature.CASUAL_ONE_OFF_GIG_OTHER_SOURCES_SEC_56,
                grossReceiptsDigitalRupees = 25_000L,
                actualDocumentedExpensesRupees = 5_000L
            )
        )

        val result = HybridIncomeAndProgressiveDisclosureEngine.optimizeHybridIncomeStreams(
            primaryGrossSalaryRupees = 15_00_000L,
            sideStreams = streams
        )

        assertEquals(15_00_000L, result.combinedGrossSalaryRupees)
        // 44ADA: 50% of 10L = 5L
        assertEquals(10_00_000L, result.grossReceipts44AdaRupees)
        assertEquals(5_00_000L, result.taxableProfit44AdaRupees)
        // 44AD: 6% of 5L = 30k
        assertEquals(5_00_000L, result.grossTurnover44AdDigitalRupees)
        assertEquals(30_000L, result.taxableProfit44AdRupees)
        // Casual gig: 25k - 5k = 20k
        assertEquals(20_000L, result.sideGigAddedToOtherSourcesSec56Rupees)
        // Total presumptive tax-free deemed expense saved: (10L - 5L) + (5L - 30k) = 5L + 4.7L = 9.7L
        assertEquals(9_70_000L, result.totalTaxFreeDeemedExpenseSavedRupees)
        assertTrue(result.requiresBusinessFormItr4OrItr3)
    }

    @Test
    fun computeProgressiveDisclosureState_salariedOnly_showsMinimalUi() {
        val emptyHybrid = HybridIncomeAndProgressiveDisclosureEngine.optimizeHybridIncomeStreams(
            primaryGrossSalaryRupees = 12_00_000L,
            sideStreams = emptyList()
        )

        val uiState = HybridIncomeAndProgressiveDisclosureEngine.computeProgressiveDisclosureState(
            employerCount = 1,
            hybridResult = emptyHybrid,
            hasHousePropertyInterestOrRent = false,
            hasCapitalGainsLots = false,
            netTaxableStcgRupees = 0L,
            netLtcg112ABeforeExemptionRupees = 0L,
            hasCryptoVdaTrades = false,
            hasForeignAssetsOrRsu = false,
            hasBroughtForwardLosses = false,
            computedTotalIncomeRupees = 11_25_000L,
            isOldRegimeSelected = false,
            netTaxPayableAfterTdsAndTcsRupees = 0L
        )

        assertFalse(uiState.showMultiEmployerMergedBanner)
        assertFalse(uiState.showSideIncomeFreelanceCard)
        assertFalse(uiState.showHousePropertyCard)
        assertFalse(uiState.showCapitalGainsCard)
        assertFalse(uiState.showCryptoVdaCard)
        assertFalse(uiState.showForeignAssetRsuCard)
        assertFalse(uiState.showScheduleAlHighEarnerCard)
        assertFalse(uiState.showForm10IeaModal)
        assertFalse(uiState.showChallan280UpiPaymentCard)
        assertEquals("ITR-1 (Sahaj)", uiState.behindTheScenesItrFormCode)
    }

    @Test
    fun computeProgressiveDisclosureState_freelanceWithCapitalGains_routesToItr3Lite() {
        val hybridWithBusiness = HybridIncomeAndProgressiveDisclosureEngine.optimizeHybridIncomeStreams(
            primaryGrossSalaryRupees = 8_00_000L,
            sideStreams = listOf(
                SideIncomeStreamInput(
                    streamId = "S1",
                    description = "Tech Consulting",
                    nature = SideIncomeNature.NOTIFIED_PROFESSION_44ADA_50PCT,
                    grossReceiptsDigitalRupees = 6_00_000L
                )
            )
        )

        val uiState = HybridIncomeAndProgressiveDisclosureEngine.computeProgressiveDisclosureState(
            employerCount = 1,
            hybridResult = hybridWithBusiness,
            hasHousePropertyInterestOrRent = true,
            hasCapitalGainsLots = true,
            netTaxableStcgRupees = 50_000L, // Taxable STCG
            netLtcg112ABeforeExemptionRupees = 2_00_000L,
            hasCryptoVdaTrades = false,
            hasForeignAssetsOrRsu = false,
            hasBroughtForwardLosses = false,
            computedTotalIncomeRupees = 14_00_000L,
            isOldRegimeSelected = false,
            netTaxPayableAfterTdsAndTcsRupees = 15_000L
        )

        assertTrue(uiState.showSideIncomeFreelanceCard)
        assertTrue(uiState.showHousePropertyCard)
        assertTrue(uiState.showCapitalGainsCard)
        assertTrue(uiState.showChallan280UpiPaymentCard)
        assertEquals("ITR-3 Lite (Presumptive + CG Auto-Managed)", uiState.behindTheScenesItrFormCode)
    }
}
