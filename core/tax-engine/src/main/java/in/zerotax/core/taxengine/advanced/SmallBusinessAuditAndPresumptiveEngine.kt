package `in`.zerotax.core.taxengine.advanced

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Small Business Tax Audit & Presumptive Compliance Engine
 *
 * Implements deterministic statutory evaluation under the Income-tax Act, 1961 for:
 * 1. Section 44AB (Mandatory Statutory Tax Audit by Chartered Accountant):
 *    - Business turnover > ₹1 Crore standard limit, OR
 *    - Business turnover > ₹10 Crores under Proviso to Sec 44AB(a) if cash receipts <= 5% AND cash payments <= 5%.
 *    - Section 271B penalty protection (0.5% of turnover up to ₹1,50,000).
 * 2. Section 44AD (Presumptive Scheme for Eligible Small Businesses):
 *    - Turnover up to ₹2 Crores standard (or ₹3 Crores if cash receipts <= 5%).
 *    - Minimum deemed profit: 6% for digital/banking receipts, 8% for cash receipts.
 *    - Section 44AD(4) 5-year lock-in penalty rule.
 * 3. Section 44ADA (Presumptive Scheme for Specified Professionals):
 *    - Gross receipts up to ₹50 Lakhs standard (or ₹75 Lakhs if cash receipts <= 5%).
 *    - Minimum deemed profit: 50% of gross receipts.
 * 4. Form 3CD Key Disallowance Diagnostic Scanner:
 *    - Clause 22 / Section 43B(h): MSME 45-day overdue payments disallowance.
 *    - Clause 21(d) / Section 40A(3): Cash payments > ₹10,000/day.
 *    - Clause 21(a) / Section 40(a)(ia): 30% expense disallowance for non-deduction of TDS.
 *    - Clause 26 / Section 43B: Unpaid statutory dues (GST, EPF, ESIC).
 */
object SmallBusinessAuditAndPresumptiveEngine {

    const val SEC_44AB_STANDARD_BUSINESS_LIMIT = 1_00_00_000L // ₹1 Crore
    const val SEC_44AB_ENHANCED_DIGITAL_LIMIT = 10_00_00_000L // ₹10 Crores (<= 5% cash)
    const val SEC_44AD_STANDARD_LIMIT = 2_00_00_000L // ₹2 Crores
    const val SEC_44AD_ENHANCED_DIGITAL_LIMIT = 3_00_00_000L // ₹3 Crores (<= 5% cash)
    const val SEC_44ADA_STANDARD_LIMIT = 50_00_000L // ₹50 Lakhs
    const val SEC_44ADA_ENHANCED_DIGITAL_LIMIT = 75_00_000L // ₹75 Lakhs (<= 5% cash)
    const val BASIC_EXEMPTION_LIMIT_AY2026_27 = 3_00_000L // ₹3 Lakhs under Sec 115BAC

    enum class BusinessType {
        ELIGIBLE_BUSINESS_44AD, // Traders, Retailers, Wholesalers, Manufacturers, Contractors
        SPECIFIED_PROFESSION_44ADA, // Doctors, CAs, Lawyers, Engineers, Architects, IT Consultants
        OTHER_REGULAR_BUSINESS // Commission, Agency, Logistics, Manufacturing not under 44AD
    }

    data class BusinessFinancialsInput(
        val businessType: BusinessType,
        val totalTurnoverRupees: Long,
        val digitalTurnoverRupees: Long,
        val cashTurnoverRupees: Long,
        val totalBusinessExpensesRupees: Long = 0L,
        val totalPaymentsRupees: Long = 0L,
        val cashPaymentsRupees: Long = 0L,
        val declaredNetProfitRupees: Long,
        val optedOut44ADInPast5Years: Boolean = false,
        // Form 3CD Disallowance items
        val msmeOverdueUnpaid43Bh: Long = 0L,
        val cashExpensesOver10k40A3: Long = 0L,
        val expensesWithoutTds40aIa: Long = 0L,
        val unpaidStatutoryDues43B: Long = 0L
    ) {
        val cashReceiptPercentage: Double
            get() = if (totalTurnoverRupees > 0L) (cashTurnoverRupees.toDouble() / totalTurnoverRupees.toDouble()) * 100.0 else 0.0

        val cashPaymentPercentage: Double
            get() = if (totalPaymentsRupees > 0L) (cashPaymentsRupees.toDouble() / totalPaymentsRupees.toDouble()) * 100.0 else 0.0

        val isEligibleFor10CroreAuditLimit: Boolean
            get() = cashReceiptPercentage <= 5.0 && cashPaymentPercentage <= 5.0
    }

    enum class AuditVerdict {
        EXEMPT_PRESUMPTIVE_44AD, // Eligible for ITR-4 without books or CA audit
        EXEMPT_PRESUMPTIVE_44ADA, // Eligible for ITR-4 without books or CA audit
        EXEMPT_BELOW_TURNOVER_THRESHOLD, // Turnover below ₹1 Cr / ₹10 Cr, regular ITR-3 without audit
        MANDATORY_TAX_AUDIT_44AB // Section 44AB tax audit by CA is legally compulsory
    }

    data class Form3CdDisallowances(
        val section43BhMsmeAddback: Long,
        val section40A3CashExpenseAddback: Long,
        val section40aIaTdsDisallowance30Pct: Long,
        val section43BUnpaidStatutoryDues: Long,
        val totalStatutoryAddbacks: Long
    )

    data class SmallBusinessAuditReport(
        val verdict: AuditVerdict,
        val isTaxAuditMandatory: Boolean,
        val statutoryAuditReason: String,
        val applicableItrForm: String,
        val statutoryAuditDueDate: String, // September 30
        val itrDueDateWithAudit: String, // October 31
        val potentialSection271BPenalty: Long,
        val minPresumptiveProfitRequired: Long,
        val effectiveBusinessNetProfit: Long,
        val disallowances: Form3CdDisallowances,
        val caHandoverDossierMarkdown: String
    )

    /**
     * Evaluates Section 44AB, 44AD, 44ADA, and Form 3CD disallowances.
     */
    fun evaluateBusinessAudit(input: BusinessFinancialsInput): SmallBusinessAuditReport {
        val cashReceiptPct = input.cashReceiptPercentage
        val cashPaymentPct = input.cashPaymentPercentage

        // Compute Form 3CD Addbacks
        val tdsDisallowance = (input.expensesWithoutTds40aIa * 0.30).roundToLong()
        val totalAddbacks = input.msmeOverdueUnpaid43Bh + input.cashExpensesOver10k40A3 +
                tdsDisallowance + input.unpaidStatutoryDues43B

        val disallowances = Form3CdDisallowances(
            section43BhMsmeAddback = input.msmeOverdueUnpaid43Bh,
            section40A3CashExpenseAddback = input.cashExpensesOver10k40A3,
            section40aIaTdsDisallowance30Pct = tdsDisallowance,
            section43BUnpaidStatutoryDues = input.unpaidStatutoryDues43B,
            totalStatutoryAddbacks = totalAddbacks
        )

        val adjustedNetProfit = input.declaredNetProfitRupees + totalAddbacks

        // Section 271B penalty: 0.5% of turnover up to ₹1,50,000
        val penalty271B = min(150000L, (input.totalTurnoverRupees * 0.005).roundToLong())

        // -------------------------------------------------------------
        // SCENARIO 1: SPECIFIED PROFESSIONALS (SECTION 44ADA)
        // -------------------------------------------------------------
        if (input.businessType == BusinessType.SPECIFIED_PROFESSION_44ADA) {
            val max44AdaLimit = if (cashReceiptPct <= 5.0) SEC_44ADA_ENHANCED_DIGITAL_LIMIT else SEC_44ADA_STANDARD_LIMIT
            val min50PctProfit = (input.totalTurnoverRupees * 0.50).roundToLong()

            return if (input.totalTurnoverRupees <= max44AdaLimit) {
                if (adjustedNetProfit >= min50PctProfit) {
                    SmallBusinessAuditReport(
                        verdict = AuditVerdict.EXEMPT_PRESUMPTIVE_44ADA,
                        isTaxAuditMandatory = false,
                        statutoryAuditReason = "Presumptive profession u/s 44ADA. Declared profit (₹${"%,d".format(adjustedNetProfit)}) is >= 50% threshold. Zero books of accounts & zero audit required.",
                        applicableItrForm = "ITR-4 (Sugam)",
                        statutoryAuditDueDate = "N/A",
                        itrDueDateWithAudit = "July 31",
                        potentialSection271BPenalty = 0L,
                        minPresumptiveProfitRequired = min50PctProfit,
                        effectiveBusinessNetProfit = adjustedNetProfit,
                        disallowances = disallowances,
                        caHandoverDossierMarkdown = generateCaDossier(input, disallowances, false, "ITR-4 u/s 44ADA")
                    )
                } else if (adjustedNetProfit > BASIC_EXEMPTION_LIMIT_AY2026_27) {
                    SmallBusinessAuditReport(
                        verdict = AuditVerdict.MANDATORY_TAX_AUDIT_44AB,
                        isTaxAuditMandatory = true,
                        statutoryAuditReason = "Section 44AB(d) triggered: Declared professional profit (${(adjustedNetProfit.toDouble() / input.totalTurnoverRupees * 100).toInt()}%) is below the mandatory 50% statutory benchmark, and total income exceeds basic exemption ₹3,00,000.",
                        applicableItrForm = "ITR-3 + Form 3CB-3CD",
                        statutoryAuditDueDate = "September 30",
                        itrDueDateWithAudit = "October 31",
                        potentialSection271BPenalty = penalty271B,
                        minPresumptiveProfitRequired = min50PctProfit,
                        effectiveBusinessNetProfit = adjustedNetProfit,
                        disallowances = disallowances,
                        caHandoverDossierMarkdown = generateCaDossier(input, disallowances, true, "Section 44AB(d) Audit")
                    )
                } else {
                    SmallBusinessAuditReport(
                        verdict = AuditVerdict.EXEMPT_BELOW_TURNOVER_THRESHOLD,
                        isTaxAuditMandatory = false,
                        statutoryAuditReason = "Profit is below 50%, but total income does not exceed basic exemption limit (₹3,00,000). Tax audit not triggered.",
                        applicableItrForm = "ITR-3",
                        statutoryAuditDueDate = "N/A",
                        itrDueDateWithAudit = "July 31",
                        potentialSection271BPenalty = 0L,
                        minPresumptiveProfitRequired = min50PctProfit,
                        effectiveBusinessNetProfit = adjustedNetProfit,
                        disallowances = disallowances,
                        caHandoverDossierMarkdown = generateCaDossier(input, disallowances, false, "ITR-3 Non-Audit")
                    )
                }
            } else {
                // Gross receipts exceed ₹50L/₹75L
                val isAuditMandatory = input.totalTurnoverRupees > 50_00_000L // Professional audit threshold
                SmallBusinessAuditReport(
                    verdict = if (isAuditMandatory) AuditVerdict.MANDATORY_TAX_AUDIT_44AB else AuditVerdict.EXEMPT_BELOW_TURNOVER_THRESHOLD,
                    isTaxAuditMandatory = isAuditMandatory,
                    statutoryAuditReason = "Professional receipts (₹${"%,d".format(input.totalTurnoverRupees)}) exceed Section 44ADA limit (₹${"%,d".format(max44AdaLimit)}). Section 44AB(b) tax audit mandatory.",
                    applicableItrForm = "ITR-3 + Form 3CB-3CD",
                    statutoryAuditDueDate = "September 30",
                    itrDueDateWithAudit = "October 31",
                    potentialSection271BPenalty = penalty271B,
                    minPresumptiveProfitRequired = 0L,
                    effectiveBusinessNetProfit = adjustedNetProfit,
                    disallowances = disallowances,
                    caHandoverDossierMarkdown = generateCaDossier(input, disallowances, true, "Section 44AB(b) Professional Audit")
                )
            }
        }

        // -------------------------------------------------------------
        // SCENARIO 2: ELIGIBLE SMALL BUSINESSES (SECTION 44AD)
        // -------------------------------------------------------------
        if (input.businessType == BusinessType.ELIGIBLE_BUSINESS_44AD) {
            val max44AdLimit = if (cashReceiptPct <= 5.0) SEC_44AD_ENHANCED_DIGITAL_LIMIT else SEC_44AD_STANDARD_LIMIT
            val minDigitalProfit = (input.digitalTurnoverRupees * 0.06).roundToLong()
            val minCashProfit = (input.cashTurnoverRupees * 0.08).roundToLong()
            val minRequired44AdProfit = minDigitalProfit + minCashProfit

            // Check Section 44AD(4) 5-year lock-in penalty
            if (input.optedOut44ADInPast5Years && adjustedNetProfit > BASIC_EXEMPTION_LIMIT_AY2026_27) {
                return SmallBusinessAuditReport(
                    verdict = AuditVerdict.MANDATORY_TAX_AUDIT_44AB,
                    isTaxAuditMandatory = true,
                    statutoryAuditReason = "Section 44AD(4) 5-Year Lock-In Penalty Triggered: Taxpayer opted out of Section 44AD in a preceding year. Debarred from 44AD for 5 years; Section 44AB(e) tax audit mandatory.",
                    applicableItrForm = "ITR-3 + Form 3CB-3CD",
                    statutoryAuditDueDate = "September 30",
                    itrDueDateWithAudit = "October 31",
                    potentialSection271BPenalty = penalty271B,
                    minPresumptiveProfitRequired = minRequired44AdProfit,
                    effectiveBusinessNetProfit = adjustedNetProfit,
                    disallowances = disallowances,
                    caHandoverDossierMarkdown = generateCaDossier(input, disallowances, true, "Section 44AD(4) Lock-In Audit")
                )
            }

            if (input.totalTurnoverRupees <= max44AdLimit) {
                if (adjustedNetProfit >= minRequired44AdProfit) {
                    return SmallBusinessAuditReport(
                        verdict = AuditVerdict.EXEMPT_PRESUMPTIVE_44AD,
                        isTaxAuditMandatory = false,
                        statutoryAuditReason = "Presumptive business u/s 44AD. Declared profit (₹${"%,d".format(adjustedNetProfit)}) meets minimum 6% digital / 8% cash deemed rates. No maintenance of books u/s 44AA or CA audit u/s 44AB required.",
                        applicableItrForm = "ITR-4 (Sugam)",
                        statutoryAuditDueDate = "N/A",
                        itrDueDateWithAudit = "July 31",
                        potentialSection271BPenalty = 0L,
                        minPresumptiveProfitRequired = minRequired44AdProfit,
                        effectiveBusinessNetProfit = adjustedNetProfit,
                        disallowances = disallowances,
                        caHandoverDossierMarkdown = generateCaDossier(input, disallowances, false, "ITR-4 u/s 44AD")
                    )
                } else if (adjustedNetProfit > BASIC_EXEMPTION_LIMIT_AY2026_27) {
                    return SmallBusinessAuditReport(
                        verdict = AuditVerdict.MANDATORY_TAX_AUDIT_44AB,
                        isTaxAuditMandatory = true,
                        statutoryAuditReason = "Section 44AB(e) Triggered: Small business declaring profit below 6%/8% deemed rate with total income above basic exemption ₹3,00,000. Statutory tax audit mandatory.",
                        applicableItrForm = "ITR-3 + Form 3CB-3CD",
                        statutoryAuditDueDate = "September 30",
                        itrDueDateWithAudit = "October 31",
                        potentialSection271BPenalty = penalty271B,
                        minPresumptiveProfitRequired = minRequired44AdProfit,
                        effectiveBusinessNetProfit = adjustedNetProfit,
                        disallowances = disallowances,
                        caHandoverDossierMarkdown = generateCaDossier(input, disallowances, true, "Section 44AB(e) Low-Profit Audit")
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // SCENARIO 3: REGULAR TURNOVER THRESHOLDS (SECTION 44AB(a))
        // -------------------------------------------------------------
        val isEnhancedLimitEligible = input.isEligibleFor10CroreAuditLimit
        val effectiveThreshold = if (isEnhancedLimitEligible) SEC_44AB_ENHANCED_DIGITAL_LIMIT else SEC_44AB_STANDARD_BUSINESS_LIMIT

        return if (input.totalTurnoverRupees > effectiveThreshold) {
            val limitLabel = if (isEnhancedLimitEligible) "₹10 Crores (<=5% cash)" else "₹1 Crore (cash exceeded 5%)"
            SmallBusinessAuditReport(
                verdict = AuditVerdict.MANDATORY_TAX_AUDIT_44AB,
                isTaxAuditMandatory = true,
                statutoryAuditReason = "Turnover of ₹${"%,d".format(input.totalTurnoverRupees)} exceeds Section 44AB(a) statutory limit ($limitLabel). Tax audit by Chartered Accountant is mandatory.",
                applicableItrForm = "ITR-3 + Form 3CB-3CD",
                statutoryAuditDueDate = "September 30",
                itrDueDateWithAudit = "October 31",
                potentialSection271BPenalty = penalty271B,
                minPresumptiveProfitRequired = 0L,
                effectiveBusinessNetProfit = adjustedNetProfit,
                disallowances = disallowances,
                caHandoverDossierMarkdown = generateCaDossier(input, disallowances, true, "Section 44AB(a) Turnover Audit")
            )
        } else {
            SmallBusinessAuditReport(
                verdict = AuditVerdict.EXEMPT_BELOW_TURNOVER_THRESHOLD,
                isTaxAuditMandatory = false,
                statutoryAuditReason = "Turnover is within statutory limits (₹${"%,d".format(input.totalTurnoverRupees)} <= ₹${"%,d".format(effectiveThreshold)}). Tax audit under Section 44AB not required.",
                applicableItrForm = "ITR-3",
                statutoryAuditDueDate = "N/A",
                itrDueDateWithAudit = "July 31",
                potentialSection271BPenalty = 0L,
                minPresumptiveProfitRequired = 0L,
                effectiveBusinessNetProfit = adjustedNetProfit,
                disallowances = disallowances,
                caHandoverDossierMarkdown = generateCaDossier(input, disallowances, false, "ITR-3 Non-Audit Regular")
            )
        }
    }

    private fun generateCaDossier(
        input: BusinessFinancialsInput,
        disallowances: Form3CdDisallowances,
        isAuditMandatory: Boolean,
        category: String
    ): String {
        return """
# SMALL BUSINESS TAX AUDIT & COMPLIANCE DOSSIER (CA HANDOVER PACK)
**Generated by ZeroTax-Local On-Device Engine**  
**Assessment Year:** 2026-27 | **Financial Year:** 2025-26  
**Compliance Profile:** $category  
**Section 44AB Audit Status:** ${if (isAuditMandatory) "MANDATORY TAX AUDIT REQUIRED" else "AUDIT EXEMPT / PRESUMPTIVE"}

---

### 1. Business Turnover & 5% Cash Ratio Test (Sec 44AB Proviso)
| Particulars | Amount (₹) | % of Total Turnover / Payments | Statutory Benchmarks |
| :--- | :--- | :--- | :--- |
| **Gross Business Turnover** | ₹${"%,d".format(input.totalTurnoverRupees)} | 100.0% | General Limit: ₹1 Cr / Enhanced: ₹10 Cr |
| **Digital / Banking Turnover** | ₹${"%,d".format(input.digitalTurnoverRupees)} | ${if (input.totalTurnoverRupees > 0) "%.1f".format((input.digitalTurnoverRupees.toDouble() / input.totalTurnoverRupees) * 100) else "0.0"}% | 6% Deemed Profit u/s 44AD |
| **Cash Turnover** | ₹${"%,d".format(input.cashTurnoverRupees)} | ${if (input.totalTurnoverRupees > 0) "%.1f".format(input.cashReceiptPercentage) else "0.0"}% | Must be <= 5.0% for ₹10 Cr limit |
| **Total Expenditure / Payments**| ₹${"%,d".format(input.totalPaymentsRupees)} | 100.0% | Base for Payment Cash Test |
| **Cash Payments** | ₹${"%,d".format(input.cashPaymentsRupees)} | ${if (input.totalPaymentsRupees > 0) "%.1f".format(input.cashPaymentPercentage) else "0.0"}% | Must be <= 5.0% for ₹10 Cr limit |

### 2. Form 3CD Statutory Disallowances & Income Add-Backs
| Form 3CD Clause | Statutory Section | Description | Disallowed Amount (₹) |
| :--- | :--- | :--- | :--- |
| **Clause 22** | **Sec 43B(h)** | Overdue MSME payments outstanding > 45 days | ₹${"%,d".format(disallowances.section43BhMsmeAddback)} |
| **Clause 21(d)** | **Sec 40A(3)** | Cash payments exceeding ₹10,000/day | ₹${"%,d".format(disallowances.section40A3CashExpenseAddback)} |
| **Clause 21(a)** | **Sec 40(a)(ia)** | 30% expense disallowance for non-deduction of TDS | ₹${"%,d".format(disallowances.section40aIaTdsDisallowance30Pct)} |
| **Clause 26** | **Sec 43B** | Statutory dues (GST/PF/ESIC) unpaid before due date | ₹${"%,d".format(disallowances.section43BUnpaidStatutoryDues)} |
| **TOTAL ADD-BACKS** | | **Net Addition to Business Profits** | **₹${"%,d".format(disallowances.totalStatutoryAddbacks)}** |

### 3. Net Taxable Business Income Computation
- **Declared Net Profit (Books / P&L):** ₹${"%,d".format(input.declaredNetProfitRupees)}
- **Add: Form 3CD Statutory Addbacks:** +₹${"%,d".format(disallowances.totalStatutoryAddbacks)}
- **Effective Adjusted Business Income:** **₹${"%,d".format(input.declaredNetProfitRupees + disallowances.totalStatutoryAddbacks)}**

### 4. Statutory Deadlines & Penal Safeguards
- **Tax Audit Report Filing Due Date (Form 3CA/3CB-3CD):** ${if (isAuditMandatory) "30 September 2026" else "N/A"}
- **ITR Filing Due Date:** ${if (isAuditMandatory) "31 October 2026" else "31 July 2026"}
- **Penalty for Non-Compliance u/s 271B:** ${if (isAuditMandatory) "0.5% of turnover (₹${"%,d".format(min(150000L, (input.totalTurnoverRupees * 0.005).roundToLong()))})" else "₹0 (Compliant)"}

*Note: Hand this verified dossier directly to your Chartered Accountant to ensure zero Section 143(1) mismatch intimations.*
        """.trimIndent()
    }
}

