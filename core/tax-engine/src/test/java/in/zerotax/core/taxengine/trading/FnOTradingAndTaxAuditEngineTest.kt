package `in`.zerotax.core.taxengine.trading

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FnOTradingAndTaxAuditEngineTest {

    @Test
    fun computeTradingTurnover_icaiGuidanceNote_calculatesAbsoluteDifferencePlusPremium() {
        val trades = listOf(
            TradeLot(
                tradeId = "T1",
                symbol = "NIFTY24DEC24000CE",
                tradeType = TradeType.FUTURES_AND_OPTIONS_NON_SPECULATIVE,
                buyValueRupees = 1_00_000L,
                sellValueRupees = 1_40_000L,
                netProfitLossRupees = 40_000L,
                optionPremiumReceivedRupees = 10_000L
            ),
            TradeLot(
                tradeId = "T2",
                symbol = "BANKNIFTY24DEC50000PE",
                tradeType = TradeType.FUTURES_AND_OPTIONS_NON_SPECULATIVE,
                buyValueRupees = 2_00_000L,
                sellValueRupees = 1_35_000L,
                netProfitLossRupees = -65_000L,
                optionPremiumReceivedRupees = 5_000L
            ),
            TradeLot(
                tradeId = "T3",
                symbol = "RELIANCE-EQ",
                tradeType = TradeType.INTRADAY_EQUITY_SPECULATIVE,
                buyValueRupees = 5_00_000L,
                sellValueRupees = 4_80_000L,
                netProfitLossRupees = -20_000L
            )
        )

        val summary = FnOTradingAndTaxAuditEngine.computeTradingTurnover(trades)

        // F&O Turnover: |40,000| + 10,000 + |-65,000| + 5,000 = 50,000 + 70,000 = 1,20,000
        assertEquals(1_20_000L, summary.fnoTurnoverRupees)
        assertEquals(-25_000L, summary.fnoNetPnLRupees)

        // Intraday Turnover: |-20,000| = 20,000
        assertEquals(20_000L, summary.intradaySpeculativeTurnoverRupees)
        assertEquals(-20_000L, summary.intradaySpeculativeNetPnLRupees)

        // Total Turnover: 1,20,000 + 20,000 = 1,40,000
        assertEquals(1_40_000L, summary.totalBusinessTradingTurnoverRupees)
    }

    @Test
    fun computeTradingLossSetOff_ringFencesSpeculativeLossAndBlocksSalarySetOff() {
        val summary = TradingTurnoverSummary(
            intradaySpeculativeTurnoverRupees = 50_000L,
            intradaySpeculativeNetPnLRupees = -30_000L, // Speculative loss
            fnoTurnoverRupees = 5_00_000L,
            fnoNetPnLRupees = -80_000L,                 // Non-speculative F&O loss
            totalBusinessTradingTurnoverRupees = 5_50_000L,
            totalBrokerageExpensesRupees = 2_500L,
            plainLanguageSummary = "Test Summary"
        )

        val setOff = FnOTradingAndTaxAuditEngine.computeTradingLossSetOff(
            turnoverSummary = summary,
            grossSalaryRupees = 12_00_000L,
            housePropertyIncomeRupees = 20_000L,
            capitalGainsRupees = 30_000L,
            otherSourcesIncomeRupees = 10_000L
        )

        // Speculative loss must be ring-fenced for 4 years carry-forward u/s 73
        assertEquals(30_000L, setOff.unabsorbedSpeculativeLossCarryForward4YearsRupees)

        // F&O loss of ₹80,000:
        // Offsets HP: ₹20,000 (remaining ₹60,000)
        // Offsets CG: ₹30,000 (remaining ₹30,000)
        // Offsets OS: ₹10,000 (remaining ₹20,000)
        // Cannot offset Salary u/s 71(2A) -> barredSetOffAgainstSalary = ₹20,000
        assertEquals(20_000L, setOff.allowedSetOffAgainstHousePropertyRupees)
        assertEquals(30_000L, setOff.allowedSetOffAgainstCapitalGainsRupees)
        assertEquals(10_000L, setOff.allowedSetOffAgainstOtherSourcesRupees)
        assertEquals(20_000L, setOff.barredSetOffAgainstSalaryRupees)
        assertEquals(20_000L, setOff.unabsorbedFnoLossCarryForward8YearsRupees)
    }

    @Test
    fun evaluateTaxAuditRequirement_triggersMandatoryAuditWhenTurnoverExceeds10Crore() {
        val summary = TradingTurnoverSummary(
            intradaySpeculativeTurnoverRupees = 1_00_00_000L,
            intradaySpeculativeNetPnLRupees = 5_00_000L,
            fnoTurnoverRupees = 11_00_00_000L, // 11 Cr > 10 Cr limit
            fnoNetPnLRupees = 15_00_000L,
            totalBusinessTradingTurnoverRupees = 12_00_00_000L,
            totalBrokerageExpensesRupees = 10_000L,
            plainLanguageSummary = ""
        )

        val eval = FnOTradingAndTaxAuditEngine.evaluateTaxAuditRequirement(
            turnoverSummary = summary,
            totalTaxpayerIncomeRupees = 20_00_000L,
            optedOut44ADInPast5Years = false
        )

        assertTrue(eval.isCharteredAccountantAuditMandatory)
        assertEquals(TaxAuditRequirementStatus.AUDIT_MANDATORY_TURNOVER_EXCEEDS_10_CRORE, eval.auditStatus)
        assertEquals("Section 44AB(a)", eval.statutorySection)
        assertEquals("ITR-3", eval.applicableItrForm)
        assertEquals("September 30", eval.auditDueDate)
        assertEquals("October 31", eval.itrFilingDueDate)
    }

    @Test
    fun evaluateTaxAuditRequirement_triggersAuditWhen44ADOptOutLockInApplies() {
        val summary = TradingTurnoverSummary(
            intradaySpeculativeTurnoverRupees = 0L,
            intradaySpeculativeNetPnLRupees = 0L,
            fnoTurnoverRupees = 1_00_00_000L, // 1 Cr turnover
            fnoNetPnLRupees = 2_00_000L,      // 2% declared profit (< 6%)
            totalBusinessTradingTurnoverRupees = 1_00_00_000L,
            totalBrokerageExpensesRupees = 5_000L,
            plainLanguageSummary = ""
        )

        val eval = FnOTradingAndTaxAuditEngine.evaluateTaxAuditRequirement(
            turnoverSummary = summary,
            totalTaxpayerIncomeRupees = 8_00_000L, // Exceeds basic exemption
            optedOut44ADInPast5Years = true        // Opted out of 44AD in last 5 years
        )

        assertTrue(eval.isCharteredAccountantAuditMandatory)
        assertEquals(TaxAuditRequirementStatus.AUDIT_MANDATORY_SECTION_44AD_OPT_OUT_LOCK_IN, eval.auditStatus)
    }

    @Test
    fun evaluateTaxAuditRequirement_exemptWhenTurnoverUnder10CrAndNo44ADLockIn() {
        val summary = TradingTurnoverSummary(
            intradaySpeculativeTurnoverRupees = 50_00_000L,
            intradaySpeculativeNetPnLRupees = -5_00_000L,
            fnoTurnoverRupees = 3_00_00_000L,
            fnoNetPnLRupees = -10_00_000L,
            totalBusinessTradingTurnoverRupees = 3_50_00_000L, // 3.5 Cr (< 10 Cr digital)
            totalBrokerageExpensesRupees = 25_000L,
            plainLanguageSummary = ""
        )

        val eval = FnOTradingAndTaxAuditEngine.evaluateTaxAuditRequirement(
            turnoverSummary = summary,
            totalTaxpayerIncomeRupees = 12_00_000L,
            optedOut44ADInPast5Years = false
        )

        assertFalse(eval.isCharteredAccountantAuditMandatory)
        assertEquals(TaxAuditRequirementStatus.AUDIT_NOT_REQUIRED_BELOW_THRESHOLD, eval.auditStatus)
        assertEquals("July 31", eval.itrFilingDueDate)
    }
}
