package `in`.zerotax.core.taxengine.corporate

import `in`.zerotax.core.model.*
import `in`.zerotax.core.taxengine.routing.DeterministicItrFormRouter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CorporateAndTrustTaxEngineTest {

    @Test
    fun testItr5FirmRemunerationAndInterestSection40bCapping() {
        val input = Itr5FirmInput(
            firmPan = "AABCF1234F",
            entityType = CorporateEntityType.PARTNERSHIP_FIRM,
            grossTurnoverRupees = 1_50_00_000L,
            bookProfitBeforePartnerPayments = 20_00_000L,
            partnerCapitalTotalRupees = 50_00_000L,
            partnerInterestOnCapitalClaimed = 7_50_000L, // 15% claimed
            partnerInterestRatePercent = 15.0,
            partnerRemunerationClaimed = 15_00_000L,
            isWorkingPartnerAuthorizedByDeed = true,
            otherIncomeRupees = 0L,
            deductionsChapterVIAExcept10AA = 0L
        )

        val result = CorporateAndTrustTaxEngine.computeItr5FirmTax(input)

        // 1. Max allowed interest @ 12% on 50L = 6,00,000. Disallowance = 1,50,000
        assertEquals(6_00_000L, result.admissibleInterestSection40b)
        assertEquals(1_50_000L, result.interestDisallowanceSection40b)

        // 2. Adjusted book profit = 20,00,000 + 1,50,000 = 21,50,000
        // Max remuneration: 3L * 90% (2.7L) + 18.5L * 60% (11.1L) = 13,80,000
        assertEquals(13_80_000L, result.maxPermissibleRemuneration40b)
        assertEquals(13_80_000L, result.admissibleRemuneration40b)
        assertEquals(1_20_000L, result.remunerationDisallowance40b)

        // 3. Taxable profit = 21,50,000 - 13,80,000 = 7,70,000
        assertEquals(7_70_000L, result.taxableBusinessProfit)
        assertEquals(7_70_000L, result.totalTaxableIncomeRupees)

        // 4. Normal tax @ 30% = 2,31,000 + 4% cess (9,240) = 2,40,240
        assertEquals(2_31_000L, result.normalTaxAt30Percent)
        assertEquals(9_240L, result.healthAndEducationCessRupees)
        assertEquals(2_40_240L, result.totalNormalTaxPayableRupees)
        assertEquals(2_40_240L, result.finalTaxPayableRupees)
    }

    @Test
    fun testItr6CorporateSection115BAAAndMATComparison() {
        // Case A: 115BAA Domestic Company (Exempt from MAT u/s 115JB)
        val input115Baa = Itr6CorporateInput(
            companyPan = "AABCC1234F",
            companyName = "ZeroTax Tech Pvt Ltd",
            hasOptedForSection115BAA = true,
            taxableIncomeNormalRupees = 1_00_00_000L,
            bookProfitsMatSection115JB = 1_50_00_000L
        )
        val result115Baa = CorporateAndTrustTaxEngine.computeItr6CorporateTax(input115Baa)
        assertEquals(22.0, result115Baa.applicableTaxRatePercent, 0.01)
        assertEquals(10.0, result115Baa.applicableSurchargePercent, 0.01)
        assertFalse(result115Baa.isMatApplicable)
        // Normal tax: 22% of 1Cr = 22,00,000 + 10% sur (2,20,000) = 24,20,000 + 4% cess (96,800) = 25,16,800
        assertEquals(25_16_800L, result115Baa.totalCorporateTaxPayableRupees)

        // Case B: Normal Company where MAT exceeds normal tax
        val inputNormal = Itr6CorporateInput(
            companyPan = "AABCC5678G",
            companyName = "Traditional Mfg Ltd",
            hasOptedForSection115BAA = false,
            turnoverInPriorYearExceeding400Cr = false,
            taxableIncomeNormalRupees = 50_00_000L,
            bookProfitsMatSection115JB = 1_00_00_000L
        )
        val resultNormal = CorporateAndTrustTaxEngine.computeItr6CorporateTax(inputNormal)
        assertTrue(resultNormal.isMatApplicable)
        assertTrue(resultNormal.isMatPayableHigherThanNormal)
        // Normal: 25% of 50L = 12.5L + 4% cess (50k) = 13,00,000
        assertEquals(13_00_000L, resultNormal.totalNormalTaxRupees)
        // MAT: 15% of 1Cr = 15L + 4% cess (60k) = 15,60,000
        assertEquals(15_60_000L, resultNormal.matBookProfitsTaxRupees)
        assertEquals(15_60_000L, resultNormal.totalCorporateTaxPayableRupees)
        // MAT Credit generated = 15,60,000 - 13,00,000 = 2,60,000
        assertEquals(2_60_000L, resultNormal.matCreditGeneratedCurrentYear)
        assertEquals(2_60_000L, resultNormal.closingMatCreditCarryForward)
    }

    @Test
    fun testItr7TrustSection11ApplicationAndSection13MMR() {
        val input = Itr7TrustInput(
            trustPan = "AABCT9999K",
            trustName = "Hope & Care Charitable Foundation",
            entityType = CorporateEntityType.CHARITABLE_RELIGIOUS_TRUST_12AB,
            totalVoluntaryContributionsRupees = 1_00_00_000L,
            corpusDonationsExemptSec11_1_d = 20_00_000L,
            charitableApplicationInIndiaRupees = 85_00_000L,
            unapprovedSection13ViolationsRupees = 10_00_000L
        )

        val result = CorporateAndTrustTaxEngine.computeItr7TrustTax(input)

        // 15% accumulation = 15L
        assertEquals(15_00_000L, result.statutory15PercentAccumulationRupees)
        // 85% mandatory application = 85L
        assertEquals(85_00_000L, result.mandatory85PercentApplicationRequired)
        assertEquals(0L, result.applicationShortfallRupees)

        // Section 13 violation: 10,00,000 taxed at MMR 34.944% = 3,49,440
        assertEquals(3_49_440L, result.section13ViolationTaxAtMMR)
        assertEquals(3_49_440L, result.totalTaxPayableRupees)
        assertFalse(result.isCompletelyTaxExempt)
    }

    @Test
    fun testDeterministicRoutingNonIndividualEntities() {
        // 1. Company -> ITR-6
        val companyProfile = TaxpayerRoutingProfile(
            entityType = PanEntityType.COMPANY
        )
        val companyDecision = DeterministicItrFormRouter.routeTaxpayer(companyProfile)
        assertEquals(ItrFormType.ITR_6, companyDecision.selectedForm)

        // 2. Section 8 Charitable Company -> ITR-7
        val sec8Profile = TaxpayerRoutingProfile(
            entityType = PanEntityType.COMPANY,
            isTrustOrSection8Charitable = true
        )
        val sec8Decision = DeterministicItrFormRouter.routeTaxpayer(sec8Profile)
        assertEquals(ItrFormType.ITR_7, sec8Decision.selectedForm)

        // 3. Trust -> ITR-7
        val trustProfile = TaxpayerRoutingProfile(
            entityType = PanEntityType.TRUST
        )
        val trustDecision = DeterministicItrFormRouter.routeTaxpayer(trustProfile)
        assertEquals(ItrFormType.ITR_7, trustDecision.selectedForm)

        // 4. LLP -> ITR-5
        val llpProfile = TaxpayerRoutingProfile(
            entityType = PanEntityType.FIRM,
            isLlp = true
        )
        val llpDecision = DeterministicItrFormRouter.routeTaxpayer(llpProfile)
        assertEquals(ItrFormType.ITR_5, llpDecision.selectedForm)

        // 5. AOP / BOI -> ITR-5
        val aopProfile = TaxpayerRoutingProfile(
            entityType = PanEntityType.AOP
        )
        val aopDecision = DeterministicItrFormRouter.routeTaxpayer(aopProfile)
        assertEquals(ItrFormType.ITR_5, aopDecision.selectedForm)
    }
}
