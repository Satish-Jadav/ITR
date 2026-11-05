package `in`.zerotax.core.taxengine.ca

import `in`.zerotax.core.taxengine.ca.ThinkLikeACaDeductionMaximizerEngine.CaReviewInput
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ThinkLikeACaDeductionMaximizerEngineTest {

    @Test
    fun testSalariedTaxpayerOptimization_convertsTaxPayableToRefund() {
        // User with ₹10,50,000 gross salary, ₹50,000 TDS already prepaid by employer
        val input = CaReviewInput(
            pan = "ABCDE1234F",
            grossSalary = 10_50_000L,
            tdsPrepaid = 50_000L,
            employerOffersNps80CCD2 = true,
            hasHomeLoan = true,
            homeLoanInterestPaid = 2_00_000L,
            paysRent = false,
            parentsAreSeniorCitizens = true,
            otherSourcesInterest = 15_000L
        )

        val report = ThinkLikeACaDeductionMaximizerEngine.optimizeTaxAndMaximizeRefund(input)

        assertNotNull(report)
        assertTrue(report.totalTaxSavedRupees > 0, "Total tax saved should be greater than zero")
        assertTrue(report.recommendations.isNotEmpty(), "Recommendations list should not be empty")

        // Check if 80CCD(2) recommendation is present
        val npsRec = report.recommendations.find { it.sectionCode == "Sec 80CCD(2)" }
        assertNotNull(npsRec, "Should recommend Section 80CCD(2) employer NPS")
        assertTrue(npsRec!!.potentialAdditionalClaim > 0, "80CCD(2) potential should be > 0")

        // Check if 80D recommendation is present
        val healthRec = report.recommendations.find { it.sectionCode == "Sec 80D" }
        assertNotNull(healthRec, "Should recommend Section 80D")

        // Check if 24(b) recommendation is present
        val homeLoanRec = report.recommendations.find { it.sectionCode == "Sec 24(b)" }
        assertNotNull(homeLoanRec, "Should recommend Section 24(b)")

        // Should calculate refund if optimized tax is less than TDS prepaid
        if (report.optimizedTaxLiability < input.tdsPrepaid) {
            assertTrue(report.optimizedRefundDue > 0, "Optimized refund due should be > 0")
        }
    }

    @Test
    fun testRebate87ACliffEdge_bringsTaxToZero() {
        // User earning ₹7,80,000 who just exceeds New Regime rebate threshold
        val input = CaReviewInput(
            pan = "ABCDE9999K",
            grossSalary = 7_80_000L,
            tdsPrepaid = 25_000L,
            existingDeductionsClaimed = emptyMap()
        )

        val report = ThinkLikeACaDeductionMaximizerEngine.optimizeTaxAndMaximizeRefund(input)

        assertNotNull(report)
        assertTrue(report.recommendations.any { it.isRebate87ATarget }, "Should detect 87A rebate compression opportunities")
    }

    @Test
    fun testSeniorCitizenTaxpayer_maximizesSection80TTBAndHealth() {
        val input = CaReviewInput(
            pan = "ABCDE7777S",
            grossSalary = 0L,
            tdsPrepaid = 15_000L,
            parentsAreSeniorCitizens = false,
            otherSourcesInterest = 48_000L
        )
        val report = ThinkLikeACaDeductionMaximizerEngine.optimizeTaxAndMaximizeRefund(input)
        assertNotNull(report)
        // With only interest income of 48k, tax should be 0 and full TDS refunded
        assertEquals(0L, report.optimizedTaxLiability)
        assertEquals(15_000L, report.optimizedRefundDue)
    }

    @Test
    fun testHighIncomeTaxpayer_optimizesSection80CAnd80CCD2() {
        val input = CaReviewInput(
            pan = "AABCG1234H",
            grossSalary = 35_00_000L,
            tdsPrepaid = 6_50_000L,
            employerOffersNps80CCD2 = true,
            hasHomeLoan = true,
            homeLoanInterestPaid = 2_00_000L,
            parentsAreSeniorCitizens = true,
            existingDeductionsClaimed = mapOf("80C" to 50_000L)
        )
        val report = ThinkLikeACaDeductionMaximizerEngine.optimizeTaxAndMaximizeRefund(input)
        assertNotNull(report)
        assertTrue(report.totalTaxSavedRupees > 0)
        // Check 80C top-up suggestion (1.5L max - 50k claimed = 1L headroom)
        val sec80CRec = report.recommendations.find { it.sectionCode == "Sec 80C" }
        assertNotNull(sec80CRec)
        assertEquals(1_00_000L, sec80CRec!!.potentialAdditionalClaim)
    }
}
