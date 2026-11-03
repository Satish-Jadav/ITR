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

    @Test
    fun testNonResidentTaxAndResidencyEngine_Section6AndNreExemption() {
        // Case 1: NRI - 80 days in India, citizen visiting India
        val nriInput = in.zerotax.core.taxengine.advanced.NonResidentTaxAndResidencyEngine.ResidencyEvaluationInput(
            daysInIndiaCurrentFy = 80,
            daysInIndiaPast4Fys = 150,
            isIndianCitizenOrPersonOfIndianOrigin = true,
            isVisitingIndia = true,
            totalIndianIncomeExcludingForeignRupees = 500000L
        )
        val decision = in.zerotax.core.taxengine.advanced.NonResidentTaxAndResidencyEngine.evaluateResidency(nriInput)
        assertEquals(in.zerotax.core.taxengine.advanced.NonResidentTaxAndResidencyEngine.ResidencyStatus.NON_RESIDENT_INDIAN, decision.status)
        assertFalse(decision.isGlobalIncomeTaxableInIndia)
        assertTrue(decision.isEligibleForNreTaxExemption)
        assertFalse(decision.isEligibleForSection44AD)

        // Case 2: NRI Portfolio Calculation with NRE Interest 100% Tax-Free
        val portfolio = in.zerotax.core.taxengine.advanced.NonResidentTaxAndResidencyEngine.NriTaxPortfolioInput(
            residencyStatus = decision.status,
            nreAccountInterestRupees = 300000L, // ₹3L NRE interest (Tax-free u/s 10(4))
            fcnrDepositInterestRupees = 100000L, // ₹1L FCNR interest (Tax-free u/s 10(4))
            nroAccountInterestRupees = 200000L, // ₹2L NRO interest (30% TDS)
            foreignSourcedIncomeRupees = 5000000L, // ₹50L Foreign salary (Exempt in India)
            dtaaCountry = "United States",
            dtaaTreatyRatePercent = 15.0
        )
        val result = in.zerotax.core.taxengine.advanced.NonResidentTaxAndResidencyEngine.computeNriTax(portfolio, "ABCDE1234F")

        assertEquals(5000000L, result.grossForeignIncomeExcludedFromIndia)
        assertEquals(300000L, result.nreInterestExemptUnderSec10_4)
        assertEquals(100000L, result.fcnrInterestExemptUnderSec10_4)
        assertEquals(200000L, result.taxableIndianIncome)
        assertTrue(result.nroTdsDeductedAt30Percent > 0L)
        assertTrue(result.form10FMarkdown.contains("FORM NO. 10F"))
    }

    @Test
    fun testForm10IeaBusinessRegimeEngine_BusinessOptOutAnd1TimeLockIn() {
        // Case 1: Business taxpayer opting out of New Regime to Old Regime
        val optOutInput = in.zerotax.core.taxengine.advanced.Form10IeaBusinessRegimeEngine.Form10IeaInput(
            pan = "AABBC1234D",
            taxpayerName = "Vikram Sharma",
            filingReason = in.zerotax.core.taxengine.advanced.Form10IeaBusinessRegimeEngine.Form10IeaFilingReason.OPT_OUT_OF_NEW_REGIME,
            hasOptedBackInLifetimeBefore = false
        )
        val decision = in.zerotax.core.taxengine.advanced.Form10IeaBusinessRegimeEngine.processForm10Iea(
            hasBusinessOrProfessionIncome = true,
            chosenRegimeIsOld = true,
            input = optOutInput,
            requiresTaxAudit = false
        )
        assertTrue(decision.isForm10IeaMandatory)
        assertTrue(decision.isEligibleToOptOut)
        assertEquals("31 July 2026", decision.statutoryFilingDeadline)
        assertTrue(decision.cbdtJsonPayload.contains("Form 10-IEA"))
        assertTrue(decision.cbdtJsonPayload.contains("115BAC(3)"))

        // Case 2: Taxpayer previously opted back in lifetime - permanently barred from Old Regime
        val barredInput = optOutInput.copy(hasOptedBackInLifetimeBefore = true)
        val barredDecision = in.zerotax.core.taxengine.advanced.Form10IeaBusinessRegimeEngine.processForm10Iea(
            hasBusinessOrProfessionIncome = true,
            chosenRegimeIsOld = true,
            input = barredInput,
            requiresTaxAudit = false
        )
        assertTrue(barredDecision.isForm10IeaMandatory)
        assertFalse(barredDecision.isEligibleToOptOut)
        assertTrue(barredDecision.lockInWarning.contains("STATUTORY PROHIBITION"))
        assertEquals("DEBARRED", barredDecision.schedule115BacAckCode)
    }

    @Test
    fun testSection89ArrearsRelief_EqualizesTaxAndGeneratesForm10E() {
        val input = in.zerotax.core.taxengine.advanced.Section89ArrearsReliefCalculator.Section89ComputationInput(
            receiptAssessmentYear = "2026-27",
            currentYearNormalSalaryExcludingArrears = 1200000L,
            totalArrearsReceivedInCurrentYear = 300000L,
            accrualYears = listOf(
                in.zerotax.core.taxengine.advanced.Section89ArrearsReliefCalculator.AccrualYearParticulars(
                    assessmentYear = "2024-25",
                    originalTotalIncome = 700000L,
                    originalTaxPaid = 25000L,
                    arrearsAllocatedToThisYear = 300000L
                )
            ),
            isNewRegimeInReceiptYear = true
        )

        val result = in.zerotax.core.taxengine.advanced.Section89ArrearsReliefCalculator.computeSection89Relief(input, "ABCDE1234F")

        assertEquals(1200000L, result.currentYearIncomeWithoutArrears)
        assertEquals(1500000L, result.currentYearIncomeWithArrears)
        assertTrue(result.currentYearTaxWithArrears >= result.currentYearTaxWithoutArrears)
        assertTrue(result.eligibleSection89ReliefRupees >= 0L)
        assertTrue(result.form10EJsonPayload.contains("Form10E"))
        assertTrue(result.form10EJsonPayload.contains("ABCDE1234F"))
        assertTrue(result.cpcRejectionWarning.contains("Rule 21AA"))
    }

    @Test
    fun testItrUAdditionalTaxCalculator_Calculates25And50PercentSurcharge() {
        // Case 1: Within 12 months (25% Additional Tax)
        val inputWithin12m = in.zerotax.core.taxengine.advanced.ItrUAdditionalTaxCalculator.ItrUInput(
            relevantAssessmentYear = "2024-25",
            additionalIncomeDeclaredRupees = 500000L,
            monthsElapsedFromEndOfAy = 8,
            interest234A = 5000L,
            interest234B = 5000L
        )
        val resultWithin12m = in.zerotax.core.taxengine.advanced.ItrUAdditionalTaxCalculator.computeItrUAdditionalTax(inputWithin12m)
        assertTrue(resultWithin12m.isEligibleToFileItrU)
        assertEquals(in.zerotax.core.taxengine.advanced.ItrUAdditionalTaxCalculator.ItrUTimeWindow.WITHIN_12_MONTHS, resultWithin12m.timeWindow)
        // Additional tax surcharge must be exactly 25% of (Tax + Interest)
        val expected25Surcharge = (resultWithin12m.taxAndInterestBaseFor140B * 0.25).toLong()
        assertEquals(expected25Surcharge, resultWithin12m.additionalTaxSurcharge140B)
        assertEquals(resultWithin12m.taxAndInterestBaseFor140B + expected25Surcharge, resultWithin12m.totalPayableUnderSection140B)

        // Case 2: Between 12 and 24 months (50% Additional Tax)
        val inputBetween12And24m = inputWithin12m.copy(monthsElapsedFromEndOfAy = 18)
        val resultBetween12And24m = in.zerotax.core.taxengine.advanced.ItrUAdditionalTaxCalculator.computeItrUAdditionalTax(inputBetween12And24m)
        assertTrue(resultBetween12And24m.isEligibleToFileItrU)
        assertEquals(in.zerotax.core.taxengine.advanced.ItrUAdditionalTaxCalculator.ItrUTimeWindow.BETWEEN_12_AND_24_MONTHS, resultBetween12And24m.timeWindow)
        val expected50Surcharge = (resultBetween12And24m.taxAndInterestBaseFor140B * 0.50).toLong()
        assertEquals(expected50Surcharge, resultBetween12And24m.additionalTaxSurcharge140B)

        // Case 3: Beyond 24 months (Statutorily barred)
        val inputBarred = inputWithin12m.copy(monthsElapsedFromEndOfAy = 28)
        val resultBarred = in.zerotax.core.taxengine.advanced.ItrUAdditionalTaxCalculator.computeItrUAdditionalTax(inputBarred)
        assertFalse(resultBarred.isEligibleToFileItrU)
        assertEquals(in.zerotax.core.taxengine.advanced.ItrUAdditionalTaxCalculator.ItrUTimeWindow.TIME_BARRED, resultBarred.timeWindow)
    }

    @Test
    fun testPropertyLtcgIndexation_DualOptionComparisonUnderSecondProviso() {
        val propertyInput = in.zerotax.core.taxengine.advanced.PropertyLtcgIndexationEngine.PropertySaleInput(
            acquisitionDateType = in.zerotax.core.taxengine.advanced.PropertyLtcgIndexationEngine.PropertyAcquisitionDate.BEFORE_JULY_23_2024,
            acquisitionFinancialYear = "2012-13",
            saleFinancialYear = "2025-26",
            actualCostOfAcquisitionRupees = 3000000L, // ₹30 Lakhs
            saleConsiderationRupees = 10000000L,      // ₹1 Crore
            transferExpensesRupees = 100000L,        // ₹1 Lakh
            section54ReinvestmentRupees = 120000000L // ₹12 Crores (exceeds ₹10 Cr statutory cap)
        )

        val result = in.zerotax.core.taxengine.advanced.PropertyLtcgIndexationEngine.computeDualOptionPropertyLtcg(propertyInput)

        // Section 54 must be clamped to ₹10 Crores
        assertEquals(100000000L, result.section54ExemptionClaimed)
        assertTrue(result.statutoryNotes.any { it.contains("capped at ₹10 Crores") })

        // Check indexation math: 2012-13 CII = 200, 2025-26 CII = 376
        // Indexed cost = 30L * (376 / 200) = 56.4L
        val unindexedCost = 3000000L
        val testNo54Input = propertyInput.copy(section54ReinvestmentRupees = 0L)
        val testNo54Result = in.zerotax.core.taxengine.advanced.PropertyLtcgIndexationEngine.computeDualOptionPropertyLtcg(testNo54Input)

        assertEquals(5640000L, testNo54Result.option2Indexed.indexedCostOfAcquisition)
        assertTrue(testNo54Result.option1Unindexed.netTaxPayableWithCess > 0L)
        assertTrue(testNo54Result.option2Indexed.netTaxPayableWithCess > 0L)
        assertNotNull(testNo54Result.recommendedOption)
    }

    @Test
    fun testUniversalBrokerPnLParser_ParsesZerodhaAndGrowwCsv() {
        val sampleCsv = """
            Symbol,ISIN,TradeType,Quantity,BuyPrice,SellPrice,PnL
            INFY,INE009A01021,EQ,100,1400,1600,20000
            TCS,INE467B01029,EQ,50,3800,3500,-15000
            NIFTY24DEC24000CE,,FNO,1,150,220,7000
        """.trimIndent()

        val parsed = in.zerotax.core.taxengine.advanced.UniversalBrokerPnLParser.parseBrokerCsv(sampleCsv)

        assertEquals(3, parsed.rowCountParsed)
        // Capital gains: INFY +20k, TCS -15k
        assertEquals(20000L, parsed.totalStcg111AGains)
        assertEquals(15000L, parsed.totalStcl111ALosses)
        // F&O derivative: 7000 profit, absolute turnover = abs(7000) = 7000
        assertEquals(7000L, parsed.totalFnoAbsoluteTurnover)
        assertEquals(7000L, parsed.totalFnoNetProfitOrLoss)
    }
}


