package `in`.zerotax.core.taxengine

import `in`.zerotax.core.taxengine.reconciliation.FreelancerGstInput
import `in`.zerotax.core.taxengine.reconciliation.Refund244AAuditInput
import `in`.zerotax.core.taxengine.reconciliation.RefundInterest244AAndGstAuditor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RefundInterest244AAndGstAuditorTest {

    @Test
    fun testSection244AInterest_UnderpaidByCpc_BuildsSection154Payload() {
        val input = Refund244AAuditInput(
            refundPrincipalDetermined = 1_00_000L,
            totalTaxLiabilityDetermined = 3_00_000L, // 100k > 30k (10% floor met)
            filedOnOrBeforeDueDateSec139_1 = true,
            monthsElapsedFromApril1OfAy = 8, // 8 months @ 0.5% = 4% = ₹4,000
            cpcGrantedInterestRupees = 2_500L // CPC underpaid by ₹1,500
        )

        val result = RefundInterest244AAndGstAuditor.auditSection244ARefundInterest(input)

        assertTrue(result.isEligibleFor244AInterest)
        assertEquals(4_000L, result.expectedStatutoryInterestRupees)
        assertEquals(1_500L, result.shortPaidInterestRupees)
        assertTrue(result.isUnderPaid)
        assertNotNull(result.section154RectificationPayload)
        assertTrue(result.section154RectificationPayload!!.contains("154"))
        assertTrue(result.section154RectificationPayload!!.contains("244A(1)(a)"))
    }

    @Test
    fun testSection244AInterest_RefundBelow10PercentFloor_Ineligible() {
        val input = Refund244AAuditInput(
            refundPrincipalDetermined = 15_000L,
            totalTaxLiabilityDetermined = 2_00_000L, // 15k < 20k (10% floor not met)
            filedOnOrBeforeDueDateSec139_1 = true,
            monthsElapsedFromApril1OfAy = 6,
            cpcGrantedInterestRupees = 0L
        )

        val result = RefundInterest244AAndGstAuditor.auditSection244ARefundInterest(input)

        assertFalse(result.isEligibleFor244AInterest)
        assertEquals(0L, result.expectedStatutoryInterestRupees)
        assertFalse(result.isUnderPaid)
        assertTrue(result.statutoryExplanation.contains("less than 10%"))
    }

    @Test
    fun testFreelancerGstRadar_Exceeds20Lakhs_FlagsMandatoryGstinAndLut() {
        val input = FreelancerGstInput(
            domesticReceipts194JRupees = 15_00_000L,
            foreignExportReceiptsRupees = 10_00_000L, // Total = ₹25L > ₹20L
            isSpecialCategoryState = false,
            hasValidLutArnForCurrentFy = false, // Missing LUT!
            eFiraReceiptCount = 2,
            totalForeignExportInvoicesCount = 5 // Missing 3 e-FIRAs!
        )

        val report = RefundInterest244AAndGstAuditor.reconcileFreelancerGstAndEfira(input)

        assertEquals(25_00_000L, report.totalGrossReceiptsRupees)
        assertTrue(report.isGstRegistrationMandatory)
        assertFalse(report.exportLutCompliant)
        assertEquals(3, report.missingEFiraCount)
        assertTrue(report.actionableDirectives.any { it.contains("GSTIN") })
        assertTrue(report.actionableDirectives.any { it.contains("Letter of Undertaking") })
        assertTrue(report.actionableDirectives.any { it.contains("e-FIRA") })
    }
}

