package `in`.zerotax.core.taxengine.reconciliation

import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Feature F-05 & F-06: Section 244A Refund Interest Auditor & Section 154 Rectification Builder.
 * Audits CPC Bengaluru Section 143(1) intimations to ensure 0.5% per month (6% p.a.) statutory
 * interest is properly granted on income tax refunds.
 */
data class Refund244AAuditInput(
    val refundPrincipalDetermined: Long,
    val totalTaxLiabilityDetermined: Long,
    val filedOnOrBeforeDueDateSec139_1: Boolean = true,
    val monthsElapsedFromApril1OfAy: Int,
    val cpcGrantedInterestRupees: Long
)

data class Refund244AAuditResult(
    val isEligibleFor244AInterest: Boolean,
    val expectedStatutoryInterestRupees: Long,
    val cpcGrantedInterestRupees: Long,
    val shortPaidInterestRupees: Long,
    val isUnderPaid: Boolean,
    val statutoryExplanation: String,
    val section154RectificationPayload: String?
)

/**
 * Feature F-08: Freelancer / Creator GST & Export LUT / e-FIRA Compliance Radar.
 * Tracks gross receipts against the ₹20L GST registration threshold and monitors foreign export e-FIRAs.
 */
data class FreelancerGstInput(
    val domesticReceipts194JRupees: Long,
    val foreignExportReceiptsRupees: Long,
    val isSpecialCategoryState: Boolean = false, // NE states: ₹10L threshold
    val hasValidLutArnForCurrentFy: Boolean = false,
    val eFiraReceiptCount: Int = 0,
    val totalForeignExportInvoicesCount: Int = 0
)

data class FreelancerGstComplianceReport(
    val totalGrossReceiptsRupees: Long,
    val gstRegistrationThresholdRupees: Long,
    val isGstRegistrationMandatory: Boolean,
    val exportLutCompliant: Boolean,
    val missingEFiraCount: Int,
    val actionableDirectives: List<String>
)

object RefundInterest244AAndGstAuditor {

    // --- FEATURE F-05: SECTION 244A REFUND INTEREST AUDITOR ---

    fun auditSection244ARefundInterest(input: Refund244AAuditInput): Refund244AAuditResult {
        // Condition 1: Refund must be >= 10% of total tax determined u/s 244A(2)
        val tenPercentTax = (input.totalTaxLiabilityDetermined * 0.10).roundToLong()
        val meets10PercentFloor = input.refundPrincipalDetermined >= tenPercentTax

        if (!input.filedOnOrBeforeDueDateSec139_1 || !meets10PercentFloor || input.refundPrincipalDetermined <= 0L) {
            val reason = when {
                !input.filedOnOrBeforeDueDateSec139_1 -> "Return filed belatedly u/s 139(4); interest runs only from actual date of filing rather than April 1."
                !meets10PercentFloor -> "Refund amount (₹${input.refundPrincipalDetermined}) is less than 10% of tax determined (₹$tenPercentTax) u/s 244A(2)."
                else -> "Zero refund determined."
            }
            return Refund244AAuditResult(
                isEligibleFor244AInterest = false,
                expectedStatutoryInterestRupees = 0L,
                cpcGrantedInterestRupees = input.cpcGrantedInterestRupees,
                shortPaidInterestRupees = 0L,
                isUnderPaid = false,
                statutoryExplanation = reason,
                section154RectificationPayload = null
            )
        }

        // Section 244A(1)(a): 0.5% simple interest per month or part of month
        // Principal truncated down to multiple of ₹100 u/s Rule 119A
        val truncatedPrincipal = (input.refundPrincipalDetermined / 100L) * 100L
        val expectedInterest = (truncatedPrincipal * 0.005 * input.monthsElapsedFromApril1OfAy).roundToLong()

        val shortPaid = max(0L, expectedInterest - input.cpcGrantedInterestRupees)
        val isUnderPaid = shortPaid > 50L // Tolerance threshold

        val rectificationPayload = if (isUnderPaid) {
            """
            {
              "rectificationRequest": {
                "section": "154",
                "subType": "REPROCESS_RETURN_WITH_STATUTORY_INTEREST",
                "statutoryRef": "Section 244A(1)(a) read with Rule 119A",
                "refundPrincipal": ${input.refundPrincipalDetermined},
                "monthsElapsedFromApril1": ${input.monthsElapsedFromApril1OfAy},
                "statutoryInterestRateMonthly": 0.005,
                "expectedInterestRupees": $expectedInterest,
                "cpcGrantedInterestRupees": ${input.cpcGrantedInterestRupees},
                "claimableShortfallRupees": $shortPaid,
                "justification": "Return was filed on or before due date u/s 139(1). Under Section 244A(1)(a), interest @ 0.5% per month is payable from 1st April of AY to date of refund order. CPC granted interest falls short by ₹$shortPaid."
              }
            }
            """.trimIndent()
        } else null

        return Refund244AAuditResult(
            isEligibleFor244AInterest = true,
            expectedStatutoryInterestRupees = expectedInterest,
            cpcGrantedInterestRupees = input.cpcGrantedInterestRupees,
            shortPaidInterestRupees = shortPaid,
            isUnderPaid = isUnderPaid,
            statutoryExplanation = "Eligible for 0.5% per month simple interest on ₹$truncatedPrincipal from April 1 across ${input.monthsElapsedFromApril1OfAy} months = ₹$expectedInterest.",
            section154RectificationPayload = rectificationPayload
        )
    }

    // --- FEATURE F-08: FREELANCER / CREATOR GST & LUT / e-FIRA RADAR ---

    fun reconcileFreelancerGstAndEfira(input: FreelancerGstInput): FreelancerGstComplianceReport {
        val totalGross = input.domesticReceipts194JRupees + input.foreignExportReceiptsRupees
        val threshold = if (input.isSpecialCategoryState) 10_00_000L else 20_00_000L
        val isGstMandatory = totalGross > threshold

        val missingEfira = max(0, input.totalForeignExportInvoicesCount - input.eFiraReceiptCount)
        val isLutCompliant = if (input.foreignExportReceiptsRupees > 0L) {
            input.hasValidLutArnForCurrentFy && missingEfira == 0
        } else {
            true
        }

        val directives = mutableListOf<String>()

        if (isGstMandatory) {
            directives.add("CRITICAL: Aggregate receipts (₹$totalGross) exceed GST threshold of ₹$threshold. Apply for GSTIN (Form GST REG-01) immediately.")
        } else {
            directives.add("Within statutory threshold: GST registration is NOT mandatory for receipts under ₹$threshold.")
        }

        if (input.foreignExportReceiptsRupees > 0L) {
            if (!input.hasValidLutArnForCurrentFy) {
                directives.add("EXPORT RISK: Zero-rated export of services requires an active Letter of Undertaking (LUT) filed in Form GST RFD-11 on the GST portal before receiving funds.")
            }
            if (missingEfira > 0) {
                directives.add("FIRC / e-FIRA GAP: $missingEfira export receipts lack inward remittance certificates. Download e-FIRAs via your bank/Stripe/PayPal portal to prove foreign currency receipt.")
            }
        }

        return FreelancerGstComplianceReport(
            totalGrossReceiptsRupees = totalGross,
            gstRegistrationThresholdRupees = threshold,
            isGstRegistrationMandatory = isGstMandatory,
            exportLutCompliant = isLutCompliant,
            missingEFiraCount = missingEfira,
            actionableDirectives = directives
        )
    }
}

