package `in`.zerotax.core.taxengine.lifecycle

import kotlin.math.abs
import kotlin.math.max

/**
 * Section 33.1: Automated Section 143(1) Intimation PDF vs. Filed ITR Diff Analyzer
 * & Section 154 Rectification Builder.
 *
 * Compares:
 * - Column A: "As Provided by Taxpayer in Return of Income"
 * - Column B: "As Computed under Section 143(1)"
 *
 * Verdicts:
 * 1. Zero Discrepancy Accepted: Column A == Column B (Celebratory card & Sec 244A refund tracker).
 * 2. Tax Credit Mismatch: Column B dropped TDS/TCS/Challan 280 -> Generates CBDT Section 154 Rectification JSON.
 * 3. Omitted Income / AIS Variance: Column B added AIS bank interest / dividend -> Generates Section 139(5) Revised Return guidance.
 * 4. Demand Notice u/s 143(1)(a): Generates response checklist and UPI payment link.
 *
 * Pure JVM Kotlin — Zero Android SDK imports.
 */
enum class IntimationDiscrepancyVerdict {
    ZERO_DISCREPANCY_ACCEPTED,
    REFUND_DETERMINED_WITH_244A_INTEREST,
    TAX_CREDIT_MISMATCH_SECTION_154,
    INCOME_OMISSION_SECTION_139_5,
    DEMAND_NOTICE_SECTION_143_1_A
}

data class IntimationColumnValues(
    val grossSalaryRupees: Long = 0L,
    val incomeFromHousePropertyRupees: Long = 0L,
    val businessOrProfessionProfitsRupees: Long = 0L,
    val capitalGainsRupees: Long = 0L,
    val otherSourcesIncomeRupees: Long = 0L,
    val grossTotalIncomeRupees: Long = 0L,
    val chapterViaDeductionsRupees: Long = 0L,
    val totalTaxableIncomeRupees: Long = 0L,
    val netTaxPayableBeforeCreditsRupees: Long = 0L,
    val tdsClaimedRupees: Long = 0L,
    val tcsClaimedRupees: Long = 0L,
    val advanceTaxAndSelfAssessmentPaidRupees: Long = 0L,
    val totalTaxCreditsAllowedRupees: Long = 0L,
    val interest234ARupees: Long = 0L,
    val interest234BRupees: Long = 0L,
    val interest234CRupees: Long = 0L,
    val interest244ARefundRupees: Long = 0L,
    val netPayableOrRefundableRupees: Long = 0L // Positive = Tax Payable, Negative = Refund Due
)

data class IntimationFieldDiff(
    val fieldName: String,
    val columnAAsFiledRupees: Long,
    val columnBAsComputedRupees: Long,
    val varianceRupees: Long,
    val plainLanguageExplanation: String
)

data class Section143_1DiffReport(
    val verdict: IntimationDiscrepancyVerdict,
    val acknowledgmentNumber: String,
    val assessmentYear: String,
    val diffItems: List<IntimationFieldDiff>,
    val isRectificationSection154Recommended: Boolean,
    val isRevisionSection139_5Recommended: Boolean,
    val headlineMessage: String,
    val actionSummaryMarkdown: String,
    val section154RectificationJsonPayload: String? = null
)

object Section143_1IntimationDiffAnalyzer {

    /**
     * Compares Column A (As Filed) vs Column B (As Computed under Section 143(1))
     */
    fun analyzeIntimation(
        pan: String,
        assessmentYear: String,
        acknowledgmentNumber: String,
        columnAAsFiled: IntimationColumnValues,
        columnBAsComputed: IntimationColumnValues
    ): Section143_1DiffReport {
        val diffs = mutableListOf<IntimationFieldDiff>()

        fun checkField(name: String, valA: Long, valB: Long, explanationGenerator: (Long) -> String) {
            val variance = valB - valA
            if (variance != 0L) {
                diffs.add(
                    IntimationFieldDiff(
                        fieldName = name,
                        columnAAsFiledRupees = valA,
                        columnBAsComputedRupees = valB,
                        varianceRupees = variance,
                        plainLanguageExplanation = explanationGenerator(variance)
                    )
                )
            }
        }

        checkField("Gross Total Income", columnAAsFiled.grossTotalIncomeRupees, columnBAsComputed.grossTotalIncomeRupees) { v ->
            if (v > 0) "CPC added ₹$v to Gross Total Income (likely unreported AIS interest, dividend, or SFT transactions)."
            else "CPC reduced Gross Total Income by ₹${abs(v)}."
        }

        checkField("Chapter VI-A Deductions", columnAAsFiled.chapterViaDeductionsRupees, columnBAsComputed.chapterViaDeductionsRupees) { v ->
            if (v < 0) "CPC disallowed ₹${abs(v)} in deductions (e.g. 80C/80D/80G missing receipts or invalid 10BA/10-IA forms)."
            else "CPC enhanced allowable deductions by ₹$v."
        }

        checkField("TDS Credit Allowed", columnAAsFiled.tdsClaimedRupees, columnBAsComputed.tdsClaimedRupees) { v ->
            if (v < 0) "CPC disallowed ₹${abs(v)} TDS credit. Employer or deductor may not have filed Form 24Q/26Q or TAN was mismatched in 26AS."
            else "CPC allowed additional ₹$v TDS credit."
        }

        checkField("TCS Credit Allowed", columnAAsFiled.tcsClaimedRupees, columnBAsComputed.tcsClaimedRupees) { v ->
            if (v < 0) "CPC disallowed ₹${abs(v)} TCS credit (foreign remittance or vehicle purchase not matched in 26AS Part B)."
            else "CPC allowed additional ₹$v TCS credit."
        }

        checkField("Challan 280 Tax Paid", columnAAsFiled.advanceTaxAndSelfAssessmentPaidRupees, columnBAsComputed.advanceTaxAndSelfAssessmentPaidRupees) { v ->
            if (v < 0) "CPC did not credit ₹${abs(v)} in Challan 280 tax. BSR code, challan serial number, or date may differ."
            else "CPC recognized ₹$v additional tax credit."
        }

        checkField("Net Tax Payable / Refundable", columnAAsFiled.netPayableOrRefundableRupees, columnBAsComputed.netPayableOrRefundableRupees) { v ->
            if (v > 0) "CPC determined additional tax demand of ₹$v."
            else "CPC determined additional refund of ₹${abs(v)}."
        }

        // Determine statutory verdict
        val hasIncomeAddition = columnBAsComputed.grossTotalIncomeRupees > columnAAsFiled.grossTotalIncomeRupees
        val hasCreditDisallowance = (columnBAsComputed.tdsClaimedRupees < columnAAsFiled.tdsClaimedRupees) ||
                (columnBAsComputed.tcsClaimedRupees < columnAAsFiled.tcsClaimedRupees) ||
                (columnBAsComputed.advanceTaxAndSelfAssessmentPaidRupees < columnAAsFiled.advanceTaxAndSelfAssessmentPaidRupees)
        val hasPayableDemand = columnBAsComputed.netPayableOrRefundableRupees > 0L

        val verdict: IntimationDiscrepancyVerdict
        val headline: String
        var isSec154 = false
        var isSec139_5 = false
        var sec154Payload: String? = null

        when {
            diffs.isEmpty() && columnBAsComputed.netPayableOrRefundableRupees < 0L -> {
                verdict = IntimationDiscrepancyVerdict.REFUND_DETERMINED_WITH_244A_INTEREST
                headline = "Return Processed with Zero Discrepancy — Refund of ₹${abs(columnBAsComputed.netPayableOrRefundableRupees)} Confirmed!"
            }
            diffs.isEmpty() -> {
                verdict = IntimationDiscrepancyVerdict.ZERO_DISCREPANCY_ACCEPTED
                headline = "Return Processed & Accepted 100% by CPC Bengaluru with Zero Variance!"
            }
            hasCreditDisallowance && !hasIncomeAddition -> {
                verdict = IntimationDiscrepancyVerdict.TAX_CREDIT_MISMATCH_SECTION_154
                isSec154 = true
                headline = "Tax Credit Mismatch: CPC did not match ₹${abs(columnAAsFiled.totalTaxCreditsAllowedRupees - columnBAsComputed.totalTaxCreditsAllowedRupees)} in TDS/Challan credits."
                sec154Payload = buildSection154Json(pan, assessmentYear, acknowledgmentNumber, columnAAsFiled, columnBAsComputed)
            }
            hasIncomeAddition -> {
                verdict = IntimationDiscrepancyVerdict.INCOME_OMISSION_SECTION_139_5
                isSec139_5 = true
                headline = "Unreported Income Detected by CPC Bengaluru: Difference of ₹${columnBAsComputed.grossTotalIncomeRupees - columnAAsFiled.grossTotalIncomeRupees}."
            }
            hasPayableDemand -> {
                verdict = IntimationDiscrepancyVerdict.DEMAND_NOTICE_SECTION_143_1_A
                headline = "Demand Notice u/s 143(1)(a): CPC Bengaluru has determined tax payable of ₹${columnBAsComputed.netPayableOrRefundableRupees}."
            }
            else -> {
                verdict = IntimationDiscrepancyVerdict.ZERO_DISCREPANCY_ACCEPTED
                headline = "Return Processed by CPC Bengaluru."
            }
        }

        val actionSummary = buildActionSummary(verdict, diffs, columnBAsComputed)

        return Section143_1DiffReport(
            verdict = verdict,
            acknowledgmentNumber = acknowledgmentNumber,
            assessmentYear = assessmentYear,
            diffItems = diffs,
            isRectificationSection154Recommended = isSec154,
            isRevisionSection139_5Recommended = isSec139_5,
            headlineMessage = headline,
            actionSummaryMarkdown = actionSummary,
            section154RectificationJsonPayload = sec154Payload
        )
    }

    private fun buildActionSummary(
        verdict: IntimationDiscrepancyVerdict,
        diffs: List<IntimationFieldDiff>,
        colB: IntimationColumnValues
    ): String = buildString {
        when (verdict) {
            IntimationDiscrepancyVerdict.ZERO_DISCREPANCY_ACCEPTED -> {
                append("Your return has been accepted exactly as filed. No further action is required from your side.")
            }
            IntimationDiscrepancyVerdict.REFUND_DETERMINED_WITH_244A_INTEREST -> {
                append("Refund of ₹${abs(colB.netPayableOrRefundableRupees)} has been credited/dispatched by CPC. ")
                if (colB.interest244ARefundRupees > 0L) {
                    append("Includes ₹${colB.interest244ARefundRupees} interest u/s 244A (0.5% per month).")
                }
            }
            IntimationDiscrepancyVerdict.TAX_CREDIT_MISMATCH_SECTION_154 -> {
                append("Recommended Action: Submit an online Rectification Request under Section 154 on the e-Filing portal for 'Tax Credit Mismatch Correction'. You can use the auto-generated Section 154 payload from ZeroTax.")
            }
            IntimationDiscrepancyVerdict.INCOME_OMISSION_SECTION_139_5 -> {
                append("Recommended Action: File a Revised Return under Section 139(5) to incorporate the omitted income before the December 31 statutory cut-off to avoid penalty u/s 270A.")
            }
            IntimationDiscrepancyVerdict.DEMAND_NOTICE_SECTION_143_1_A -> {
                append("Recommended Action: Review the variances listed above. If correct, pay via UPI Challan 280 within 30 days to stop Section 220(2) 1% per month interest.")
            }
        }
    }

    private fun buildSection154Json(
        pan: String,
        ay: String,
        ackNo: String,
        colA: IntimationColumnValues,
        colB: IntimationColumnValues
    ): String {
        return """
        {
          "rectificationRequest": {
            "section": "154",
            "pan": "$pan",
            "assessmentYear": "$ay",
            "originalAcknowledgmentNumber": "$ackNo",
            "rectificationReason": "TAX_CREDIT_MISMATCH",
            "schedulesToReprocess": ["TDS1", "TDS2", "TCS", "IT"],
            "claimedCredits": {
              "tdsClaimed": ${colA.tdsClaimedRupees},
              "tcsClaimed": ${colA.tcsClaimedRupees},
              "challanPaid": ${colA.advanceTaxAndSelfAssessmentPaidRupees}
            },
            "cpcAllowedCredits": {
              "tdsAllowed": ${colB.tdsClaimedRupees},
              "tcsAllowed": ${colB.tcsClaimedRupees},
              "challanAllowed": ${colB.advanceTaxAndSelfAssessmentPaidRupees}
            },
            "prayer": "Please re-credit the deposited TDS/TCS/Challan 280 appearing in Form 26AS and recalculate tax refund."
          }
        }
        """.trimIndent()
    }
}
