package in.zerotax.core.taxengine

import in.zerotax.core.taxengine.advanced.CapitalGainsAndLossLedgerEngine
import in.zerotax.core.taxengine.advanced.CapitalGainsAndLossLedgerEngine.BroughtForwardLoss
import in.zerotax.core.taxengine.advanced.CapitalGainsAndLossLedgerEngine.CapitalGainsPortfolioInput
import in.zerotax.core.taxengine.advanced.CapitalGainsAndLossLedgerEngine.LossType
import in.zerotax.core.taxengine.advanced.SalaryRestructuringOptimizer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AdvancedIndustryTaxFeaturesTestSuite {

    @Test
    fun testSalaryRestructuringOptimizer_SavesTaxViaEmployerNpsAndFlexi() {
        val grossCtc = 2500000L // ₹25 Lakhs CTC

        val report = SalaryRestructuringOptimizer.optimizeSalaryStructure(
            grossSalaryCtc = grossCtc,
            currentBasicPercent = 0.50,
            rentPaidAnnual = 360000L,
            isMetroCity = true
        )

        // Basic salary should be 50% = 12,50,000
        assertEquals(1250000L, report.optimizedComponents.basicSalary)
        // 14% of Basic for 80CCD(2) = 1,75,000
        assertEquals(175000L, report.optimizedComponents.employerNps80CCD2)
        // Telecom reimbursement allocated
        assertEquals(30000L, report.optimizedComponents.telecomReimbursement)
        // Books & periodicals allocated
        assertEquals(24000L, report.optimizedComponents.booksAndPeriodicals)

        // Total optimized components should sum up to the original CTC
        assertEquals(grossCtc, report.optimizedComponents.totalGrossCtc)

        // Tax in optimized structure must be lower or equal to current structure
        assertTrue(report.optimizedTax <= report.currentTax, "Optimized tax should be less than or equal to current tax")
        assertTrue(report.annualTaxSavings >= 0L)
        assertTrue(report.monthlyTakeHomeIncrease >= 0L)
        assertTrue(report.nudges.isNotEmpty(), "Nudges should be generated")
        assertTrue(report.hrProposalLetterMarkdown.contains("Section 80CCD(2)"))
    }

    @Test
    fun testCapitalGainsAndLossLedger_IntraHeadSetOffAndSec112AExemption() {
        val input = CapitalGainsPortfolioInput(
            stcg111AListedGain = 300000L, // ₹3,00,000 STCG @ 20%
            stcl111AListedLoss = 100000L, // ₹1,00,000 STCL
            ltcg112AListedGain = 250000L, // ₹2,50,000 LTCG
            ltcl112AListedLoss = 50000L   // ₹50,000 LTCL
        )

        val result = CapitalGainsAndLossLedgerEngine.computeCapitalGainsAndLosses(input)

        // Current Year LTCL of 50k sets off against CY LTCG 112A (250k - 50k = 200k)
        // CY STCL of 100k sets off against CY STCG 111A (300k - 100k = 200k)
        assertEquals(200000L, result.netTaxableStcg111A)
        // Remaining LTCG 112A is 200k. Sec 112A statutory exemption is 1.25L.
        // Net taxable LTCG exceeding 1.25L = 200,000 - 125,000 = 75,000
        assertEquals(125000L, result.exemption112AClaimed)
        assertEquals(75000L, result.netTaxableLtcg112AExemptExceeded)

        // Tax on STCG 111A: 200,000 * 20% = 40,000
        assertEquals(40000L, result.taxOnStcg111A)
        // Tax on LTCG 112A: 75,000 * 12.5% = 9,375
        assertEquals(9375L, result.taxOnLtcg112A)

        // Subtotal = 49,375, Cess 4% = 1,975, Total = 51,350
        assertEquals(49375L, result.totalSpecialTaxBeforeCess)
        assertEquals(1975L, result.cessAmount)
        assertEquals(51350L, result.totalCapitalGainsTax)
    }

    @Test
    fun testCapitalGainsAndLossLedger_BroughtForwardLossSetOffAndExpiry() {
        val input = CapitalGainsPortfolioInput(
            stcg111AListedGain = 150000L,
            ltcg112AListedGain = 0L,
            broughtForwardLosses = listOf(
                BroughtForwardLoss(
                    assessmentYear = "2021-22",
                    lossType = LossType.STCL,
                    originalAmount = 200000L,
                    previouslySetOff = 50000L, // unabsorbed = 150,000
                    filedBeforeDueDate = true
                ),
                BroughtForwardLoss(
                    assessmentYear = "2022-23",
                    lossType = LossType.LTCL,
                    originalAmount = 80000L,
                    previouslySetOff = 0L,
                    filedBeforeDueDate = true
                )
            )
        )

        val result = CapitalGainsAndLossLedgerEngine.computeCapitalGainsAndLosses(input)

        // B/F STCL absorbs the entire 150,000 STCG
        assertEquals(0L, result.netTaxableStcg111A)
        assertEquals(0L, result.totalCapitalGainsTax)

        // B/F LTCL of 80k cannot be set off against STCG, so it carries forward
        val ltclBucket = result.carriedForwardToFutureAy.firstOrNull { it.lossType == LossType.LTCL }
        assertNotNull(ltclBucket)
        assertEquals(80000L, ltclBucket!!.closingCarryForward)
        assertEquals("AY 2030-31", ltclBucket.expiryYear)
        assertFalse(ltclBucket.isLapsed)
    }

    @Test
    fun testSmallBusinessAudit_Presumptive44ADAndLowProfitAuditTrigger() {
        // Case 1: Presumptive 44AD Compliant (₹1.8 Cr turnover, digital, profit >= 6%)
        val compliantInput = in.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.BusinessFinancialsInput(
            businessType = in.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.BusinessType.ELIGIBLE_BUSINESS_44AD,
            totalTurnoverRupees = 18000000L,
            digitalTurnoverRupees = 18000000L,
            cashTurnoverRupees = 0L,
            declaredNetProfitRupees = 1200000L // 6.67% > 6% deemed rate
        )
        val compliantReport = in.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.evaluateBusinessAudit(compliantInput)
        assertEquals(in.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.AuditVerdict.EXEMPT_PRESUMPTIVE_44AD, compliantReport.verdict)
        assertFalse(compliantReport.isTaxAuditMandatory)
        assertEquals("ITR-4 (Sugam)", compliantReport.applicableItrForm)

        // Case 2: Profit below 6% triggers mandatory Section 44AB(e) tax audit
        val lowProfitInput = compliantInput.copy(
            declaredNetProfitRupees = 600000L // 3.33% < 6% deemed profit
        )
        val auditReport = in.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.evaluateBusinessAudit(lowProfitInput)
        assertEquals(in.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.AuditVerdict.MANDATORY_TAX_AUDIT_44AB, auditReport.verdict)
        assertTrue(auditReport.isTaxAuditMandatory)
        assertEquals("ITR-3 + Form 3CB-3CD", auditReport.applicableItrForm)
        assertEquals("September 30", auditReport.statutoryAuditDueDate)
        assertEquals("October 31", auditReport.itrDueDateWithAudit)
        assertTrue(auditReport.potentialSection271BPenalty > 0L)
    }

    @Test
    fun testSmallBusinessAudit_Form3CDDisallowanceAddbacks() {
        val inputWithDisallowances = in.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.BusinessFinancialsInput(
            businessType = in.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.BusinessType.OTHER_REGULAR_BUSINESS,
            totalTurnoverRupees = 50000000L, // ₹5 Crores
            digitalTurnoverRupees = 48000000L,
            cashTurnoverRupees = 2000000L,
            totalPaymentsRupees = 40000000L,
            cashPaymentsRupees = 1000000L,
            declaredNetProfitRupees = 3500000L, // Books net profit ₹35 Lakhs
            msmeOverdueUnpaid43Bh = 350000L, // ₹3.5L MSME overdue u/s 43B(h)
            cashExpensesOver10k40A3 = 120000L, // ₹1.2L Cash > ₹10k u/s 40A(3)
            expensesWithoutTds40aIa = 500000L, // ₹5L without TDS => 30% = ₹1.5L
            unpaidStatutoryDues43B = 100000L // ₹1L unpaid GST/PF
        )

        val report = in.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.evaluateBusinessAudit(inputWithDisallowances)

        // 30% of ₹5,00,000 = ₹1,50,000
        assertEquals(150000L, report.disallowances.section40aIaTdsDisallowance30Pct)
        // Total addbacks = 3.5L + 1.2L + 1.5L + 1.0L = 7.2L
        assertEquals(720000L, report.disallowances.totalStatutoryAddbacks)
        // Adjusted net profit = 35L + 7.2L = 42.2L
        assertEquals(4220000L, report.effectiveBusinessNetProfit)

        // Cash % <= 5% (receipts 4%, payments 2.5%), so eligible for ₹10 Cr limit. Turnover ₹5 Cr is below ₹10 Cr limit.
        assertEquals(in.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.AuditVerdict.EXEMPT_BELOW_TURNOVER_THRESHOLD, report.verdict)
        assertFalse(report.isTaxAuditMandatory)
    }
}

