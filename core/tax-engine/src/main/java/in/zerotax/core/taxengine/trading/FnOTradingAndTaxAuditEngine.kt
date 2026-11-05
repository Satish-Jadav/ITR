package `in`.zerotax.core.taxengine.trading

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Section 31: ITR-3 Lite: Intraday Equity, F&O Trading Turnover & Tax Audit (Sec 43(5), Sec 44AB, Sec 73) Engine.
 *
 * Implements:
 * 1. Statutory Classification & Set-Off Rules:
 *    - Intraday Speculative Equity Trading u/s 43(5): Set-off ring-fence u/s 73 (only against speculative profit; 4-yr carry-forward u/s 73(4)).
 *    - Non-Speculative F&O Trading u/s 43(5) Proviso (d): Set-off u/s 70 & 71 (offset HP, CG, Other Sources; CANNOT offset Salary u/s 71(2A); 8-yr carry-forward u/s 72).
 * 2. Deterministic F&O Absolute Turnover Formula under ICAI Guidance Note on Tax Audit u/s 44AB:
 *    Turnover = Sum(|Profit/Loss on Trade i|) + Premium Received on Sale of Options.
 * 3. Complete Section 44AB Tax Audit Decision Tree for F&O / Business Traders (>= 95% digital transactions threshold ₹10 Cr, Sec 44AD(4)/(5) lock-in checks).
 *
 * Pure JVM Kotlin — Zero Android SDK dependencies, Zero floating-point currency math.
 */
enum class TradeType {
    INTRADAY_EQUITY_SPECULATIVE,          // Settled without delivery on same day u/s 43(5)
    FUTURES_AND_OPTIONS_NON_SPECULATIVE,  // Exchange-traded derivatives u/s 43(5) Proviso (d)
    DELIVERY_EQUITY                       // Covered under Capital Gains (Sec 45 / 111A / 112A)
}

data class TradeLot(
    val tradeId: String,
    val symbol: String,
    val tradeType: TradeType,
    val buyValueRupees: Long,
    val sellValueRupees: Long,
    val netProfitLossRupees: Long,
    val optionPremiumReceivedRupees: Long = 0L,
    val brokerageAndChargesRupees: Long = 0L
)

data class TradingTurnoverSummary(
    val intradaySpeculativeTurnoverRupees: Long,
    val intradaySpeculativeNetPnLRupees: Long,
    val fnoTurnoverRupees: Long,
    val fnoNetPnLRupees: Long,
    val totalBusinessTradingTurnoverRupees: Long,
    val totalBrokerageExpensesRupees: Long,
    val plainLanguageSummary: String
)

data class LossSetOffResult(
    val allowedSetOffAgainstOtherBusinessRupees: Long,
    val allowedSetOffAgainstHousePropertyRupees: Long,
    val allowedSetOffAgainstCapitalGainsRupees: Long,
    val allowedSetOffAgainstOtherSourcesRupees: Long,
    val barredSetOffAgainstSalaryRupees: Long,
    val unabsorbedSpeculativeLossCarryForward4YearsRupees: Long,
    val unabsorbedFnoLossCarryForward8YearsRupees: Long,
    val netTaxableBusinessTradingIncomeRupees: Long,
    val setOffExplanation: String
)

enum class TaxAuditRequirementStatus {
    AUDIT_NOT_REQUIRED_BELOW_THRESHOLD,
    AUDIT_NOT_REQUIRED_44AD_APPLIED,
    AUDIT_MANDATORY_TURNOVER_EXCEEDS_10_CRORE,
    AUDIT_MANDATORY_SECTION_44AD_OPT_OUT_LOCK_IN
}

data class TaxAuditEvaluationResult(
    val auditStatus: TaxAuditRequirementStatus,
    val isCharteredAccountantAuditMandatory: Boolean,
    val statutorySection: String,
    val applicableItrForm: String, // "ITR-3" or "ITR-4 (Sugam)" or "ITR-2"
    val auditDueDate: String,      // September 30 of AY
    val itrFilingDueDate: String,  // October 31 of AY if audit, else July 31
    val auditReasonMarkdown: String,
    val form3CdAnnexureSummary: String
)

object FnOTradingAndTaxAuditEngine {

    const val SEC_44AB_DIGITAL_TURNOVER_LIMIT_RUPEES = 10_00_00_000L // ₹10 Crores (>= 95% digital)
    const val SEC_44AD_MAX_ELIGIBLE_TURNOVER_RUPEES = 3_00_00_000L   // ₹3 Crores (>= 95% digital)
    const val BASIC_EXEMPTION_LIMIT_NEW_REGIME_RUPEES = 4_00_000L     // ₹4 Lakhs for AY 2026-27 (or ₹3L/₹2.5L)

    /**
     * Computes ICAI Guidance Note Statutory Turnover for F&O and Intraday trades.
     * Formula: Sum(|Profit or Loss|) + Premium Received on Sale of Options.
     */
    fun computeTradingTurnover(trades: List<TradeLot>): TradingTurnoverSummary {
        var intradayTurnover = 0L
        var intradayPnL = 0L
        var fnoTurnover = 0L
        var fnoPnL = 0L
        var totalBrokerage = 0L

        for (trade in trades) {
            totalBrokerage += trade.brokerageAndChargesRupees
            when (trade.tradeType) {
                TradeType.INTRADAY_EQUITY_SPECULATIVE -> {
                    // Intraday turnover = |P&L| of each trade
                    intradayTurnover += abs(trade.netProfitLossRupees)
                    intradayPnL += trade.netProfitLossRupees
                }
                TradeType.FUTURES_AND_OPTIONS_NON_SPECULATIVE -> {
                    // F&O Turnover = Sum of positive and negative differences + Option Sale Premium
                    fnoTurnover += abs(trade.netProfitLossRupees) + trade.optionPremiumReceivedRupees
                    fnoPnL += trade.netProfitLossRupees
                }
                TradeType.DELIVERY_EQUITY -> {
                    // Delivery is Schedule CG, not Schedule BP turnover
                }
            }
        }

        val totalBusinessTurnover = intradayTurnover + fnoTurnover
        val summaryText = "F&O Statutory Turnover (ICAI Guidance Note): ₹$fnoTurnover (Net P&L: ₹$fnoPnL) | " +
                "Intraday Speculative Turnover: ₹$intradayTurnover (Net P&L: ₹$intradayPnL). " +
                "Total Trading Business Turnover: ₹$totalBusinessTurnover."

        return TradingTurnoverSummary(
            intradaySpeculativeTurnoverRupees = intradayTurnover,
            intradaySpeculativeNetPnLRupees = intradayPnL,
            fnoTurnoverRupees = fnoTurnover,
            fnoNetPnLRupees = fnoPnL,
            totalBusinessTradingTurnoverRupees = totalBusinessTurnover,
            totalBrokerageExpensesRupees = totalBrokerage,
            plainLanguageSummary = summaryText
        )
    }

    /**
     * Executes statutory loss set-off rules:
     * - Speculative Intraday loss u/s 73: Ring-fenced. Only offsets speculative profit. 4-year carry forward u/s 73(4).
     * - Non-speculative F&O loss u/s 70/71: Can offset HP, CG, Other Sources. Cannot offset Salary u/s 71(2A). 8-year carry forward u/s 72.
     */
    fun computeTradingLossSetOff(
        turnoverSummary: TradingTurnoverSummary,
        grossSalaryRupees: Long,
        housePropertyIncomeRupees: Long,
        capitalGainsRupees: Long,
        otherSourcesIncomeRupees: Long,
        otherNonTradingBusinessIncomeRupees: Long = 0L
    ): LossSetOffResult {
        val intradayPnL = turnoverSummary.intradaySpeculativeNetPnLRupees
        val fnoPnL = turnoverSummary.fnoNetPnLRupees

        // 1. Speculative Intraday Loss Set-Off u/s 73:
        var unabsorbedSpeculativeLoss = 0L
        var netSpeculativeIncome = 0L
        if (intradayPnL < 0L) {
            // Can ONLY offset speculative business profit; since this is the intraday pool, none can offset F&O or other heads
            unabsorbedSpeculativeLoss = abs(intradayPnL)
            netSpeculativeIncome = 0L
        } else {
            netSpeculativeIncome = intradayPnL
        }

        // 2. F&O Non-Speculative Loss Set-Off u/s 70 & 71:
        var unabsorbedFnoLoss = 0L
        var netFnoIncome = 0L
        var setOffOtherBusiness = 0L
        var setOffHp = 0L
        var setOffCg = 0L
        var setOffOs = 0L
        var barredSalarySetOff = 0L

        if (fnoPnL < 0L) {
            var remainingLoss = abs(fnoPnL)

            // Step A: Intra-head set-off u/s 70 against other non-speculative business income
            if (otherNonTradingBusinessIncomeRupees > 0L) {
                setOffOtherBusiness = min(remainingLoss, otherNonTradingBusinessIncomeRupees)
                remainingLoss -= setOffOtherBusiness
            }

            // Step B: Inter-head set-off u/s 71 against House Property
            if (remainingLoss > 0L && housePropertyIncomeRupees > 0L) {
                setOffHp = min(remainingLoss, housePropertyIncomeRupees)
                remainingLoss -= setOffHp
            }

            // Step C: Inter-head set-off u/s 71 against Capital Gains
            if (remainingLoss > 0L && capitalGainsRupees > 0L) {
                setOffCg = min(remainingLoss, capitalGainsRupees)
                remainingLoss -= setOffCg
            }

            // Step D: Inter-head set-off u/s 71 against Other Sources
            if (remainingLoss > 0L && otherSourcesIncomeRupees > 0L) {
                setOffOs = min(remainingLoss, otherSourcesIncomeRupees)
                remainingLoss -= setOffOs
            }

            // Step E: Statutory Bar u/s 71(2A) — Business loss CANNOT offset Salary!
            if (remainingLoss > 0L && grossSalaryRupees > 0L) {
                barredSalarySetOff = min(remainingLoss, grossSalaryRupees)
            }

            unabsorbedFnoLoss = remainingLoss
            netFnoIncome = 0L
        } else {
            netFnoIncome = fnoPnL
        }

        val netTaxableTrading = netSpeculativeIncome + netFnoIncome
        val explanation = buildString {
            if (unabsorbedSpeculativeLoss > 0L) {
                append("Speculative Intraday Loss of ₹$unabsorbedSpeculativeLoss ring-fenced u/s 73; carries forward for 4 years. ")
            }
            if (fnoPnL < 0L) {
                append("F&O Non-Speculative Loss of ₹${abs(fnoPnL)}: Set off ₹$setOffOtherBusiness vs Business, ₹$setOffHp vs HP, ₹$setOffCg vs CG, ₹$setOffOs vs Other Sources. ")
                if (barredSalarySetOff > 0L) {
                    append("₹$barredSalarySetOff barred from setting off Salary u/s 71(2A). ")
                }
                if (unabsorbedFnoLoss > 0L) {
                    append("Unabsorbed ₹$unabsorbedFnoLoss carries forward for 8 years u/s 72. ")
                }
            }
        }

        return LossSetOffResult(
            allowedSetOffAgainstOtherBusinessRupees = setOffOtherBusiness,
            allowedSetOffAgainstHousePropertyRupees = setOffHp,
            allowedSetOffAgainstCapitalGainsRupees = setOffCg,
            allowedSetOffAgainstOtherSourcesRupees = setOffOs,
            barredSetOffAgainstSalaryRupees = barredSalarySetOff,
            unabsorbedSpeculativeLossCarryForward4YearsRupees = unabsorbedSpeculativeLoss,
            unabsorbedFnoLossCarryForward8YearsRupees = unabsorbedFnoLoss,
            netTaxableBusinessTradingIncomeRupees = netTaxableTrading,
            setOffExplanation = explanation.trim()
        )
    }

    /**
     * Section 44AB Tax Audit Decision Tree for 100% digital F&O / Stock traders.
     */
    fun evaluateTaxAuditRequirement(
        turnoverSummary: TradingTurnoverSummary,
        totalTaxpayerIncomeRupees: Long,
        optedOut44ADInPast5Years: Boolean,
        userPrefersPresumptive44ADIfEligible: Boolean = false
    ): TaxAuditEvaluationResult {
        val totalTurnover = turnoverSummary.totalBusinessTradingTurnoverRupees
        val netTradingProfit = turnoverSummary.intradaySpeculativeNetPnLRupees + turnoverSummary.fnoNetPnLRupees

        // Condition 1: Turnover > ₹10 Crore under Proviso to Sec 44AB(a) (100% digital transactions)
        if (totalTurnover > SEC_44AB_DIGITAL_TURNOVER_LIMIT_RUPEES) {
            return TaxAuditEvaluationResult(
                auditStatus = TaxAuditRequirementStatus.AUDIT_MANDATORY_TURNOVER_EXCEEDS_10_CRORE,
                isCharteredAccountantAuditMandatory = true,
                statutorySection = "Section 44AB(a)",
                applicableItrForm = "ITR-3",
                auditDueDate = "September 30",
                itrFilingDueDate = "October 31",
                auditReasonMarkdown = "Trading turnover (₹$totalTurnover) exceeds the ₹10 Crore digital threshold under Proviso to Section 44AB(a). Tax Audit by a Chartered Accountant with Form 3CA/3CB-3CD is legally mandatory.",
                form3CdAnnexureSummary = "Form 3CD: Clause 8 (Turnover ₹$totalTurnover), Clause 10 (Books u/s 44AA), Clause 11 (Broker Ledger), Clause 21 (Trading Expenses)."
            )
        }

        // Condition 2: Section 44AB(e) read with Section 44AD(4)/(5) Lock-in Rule
        // If turnover <= ₹3 Crore and declared profit < 6% (or loss):
        // Audit is mandatory ONLY IF:
        // (a) Taxpayer opted into Sec 44AD in one of the past 5 years and is now declaring < 6% profit, AND
        // (b) Total taxable income exceeds basic exemption limit.
        if (totalTurnover <= SEC_44AD_MAX_ELIGIBLE_TURNOVER_RUPEES) {
            val min6PctProfit = (totalTurnover * 600L + 5_000L) / 10_000L
            val declaresSub6PercentOrLoss = netTradingProfit < min6PctProfit

            if (declaresSub6PercentOrLoss && optedOut44ADInPast5Years && totalTaxpayerIncomeRupees > BASIC_EXEMPTION_LIMIT_NEW_REGIME_RUPEES) {
                return TaxAuditEvaluationResult(
                    auditStatus = TaxAuditRequirementStatus.AUDIT_MANDATORY_SECTION_44AD_OPT_OUT_LOCK_IN,
                    isCharteredAccountantAuditMandatory = true,
                    statutorySection = "Section 44AB(e) r.w. Section 44AD(4)/(5)",
                    applicableItrForm = "ITR-3",
                    auditDueDate = "September 30",
                    itrFilingDueDate = "October 31",
                    auditReasonMarkdown = "Taxpayer opted out of Section 44AD within the 5-year lock-in period and declared profit < 6% (or loss) while total income (₹$totalTaxpayerIncomeRupees) exceeds the Basic Exemption Limit. Audit u/s 44AB(e) is mandatory.",
                    form3CdAnnexureSummary = "Form 3CD: Clause 8 (Presumptive Opt-out), Clause 10 (Books u/s 44AA), Clause 11 (Stock Broker Clearing Ledger)."
                )
            }

            if (userPrefersPresumptive44ADIfEligible && netTradingProfit >= min6PctProfit) {
                return TaxAuditEvaluationResult(
                    auditStatus = TaxAuditRequirementStatus.AUDIT_NOT_REQUIRED_44AD_APPLIED,
                    isCharteredAccountantAuditMandatory = false,
                    statutorySection = "Section 44AD",
                    applicableItrForm = "ITR-4 (Sugam)",
                    auditDueDate = "N/A",
                    itrFilingDueDate = "July 31",
                    auditReasonMarkdown = "Eligible for Section 44AD presumptive scheme (Turnover ₹$totalTurnover <= ₹3 Cr and profit >= 6%). Zero bookkeeping or CA audit required.",
                    form3CdAnnexureSummary = "No Form 3CD required. Report in Schedule BP of ITR-4."
                )
            }
        }

        // Default: Turnover between ₹3 Cr and ₹10 Cr (or <= ₹3 Cr without 44AD lock-in) -> No Audit required!
        return TaxAuditEvaluationResult(
            auditStatus = TaxAuditRequirementStatus.AUDIT_NOT_REQUIRED_BELOW_THRESHOLD,
            isCharteredAccountantAuditMandatory = false,
            statutorySection = "Section 44AA / 44AB(a) Proviso",
            applicableItrForm = "ITR-3",
            auditDueDate = "N/A",
            itrFilingDueDate = "July 31",
            auditReasonMarkdown = "Turnover (₹$totalTurnover) is within the ₹10 Crore digital threshold and Section 44AD(4) lock-in does not trigger audit. Maintain digital broker books u/s 44AA; CA Tax Audit is NOT required.",
            form3CdAnnexureSummary = "CA Audit not required. File standard Schedule BP in ITR-3."
        )
    }
}
