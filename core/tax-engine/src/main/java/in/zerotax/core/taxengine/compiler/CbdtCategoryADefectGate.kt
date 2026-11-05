package `in`.zerotax.core.taxengine.compiler

import `in`.zerotax.core.model.BankAccountDetails

data class DefectGateEvaluationResult(
    val isPassed: Boolean,
    val failedGates: List<String>,
    val canProceedWithSubmission: Boolean
)

object CbdtCategoryADefectGate {
    private val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")
    private val IFSC_REGEX = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
    // VUL-20 FIX: Strict 12-digit Aadhaar validation; "xRedacted" and similar strings are rejected.
    private val AADHAAR_12_DIGIT_REGEX = Regex("""\d{12}""")

    fun evaluateCategoryADefects(
        pan: String,
        aadhaarRedactedOrValid: String,
        grossSalary: Long,
        claimedTds: Long,
        allowed26AsTds: Long,
        grossTotalIncome: Long,
        chapterViaDeductions: Long,
        bankAccount: BankAccountDetails?,
        isSection89Claimed: Boolean = false,
        form10EAcknowledgment: String? = null
    ): DefectGateEvaluationResult {
        val failures = mutableListOf<String>()

        // Gate 1: PAN validation
        if (!PAN_REGEX.matches(pan.trim().uppercase())) {
            failures.add("GATE_1_INVALID_PAN_FORMAT")
        }

        // Gate 2: Aadhaar format validation
        // VUL-20 FIX: Must be either exact "[Aadhaar Redacted]" text or exactly 12 continuous digits.
        // Strings like "xRedacted", short strings, or garbage values are explicitly rejected.
        val cleanAadhaar = aadhaarRedactedOrValid.trim()
        val isOfficiallyRedacted = cleanAadhaar.equals("[Aadhaar Redacted]", ignoreCase = false)
        val isValid12Digit = AADHAAR_12_DIGIT_REGEX.matches(
            cleanAadhaar.replace(Regex("[\\s\\-.]"), "")
        )
        if (!isOfficiallyRedacted && !isValid12Digit) {
            failures.add("GATE_2_INVALID_AADHAAR_IDENTIFIER")
        }

        // Gate 3: TDS claimed cannot exceed Gross Total Income (not just salary) or Form 26AS Part A deposit.
        // VUL-19 FIX: Was incorrectly comparing against grossSalary, blocking all non-salaried filers
        // (freelancers, traders, business owners, capital gains filers) from filing. Fixed to use grossTotalIncome.
        if (claimedTds > grossTotalIncome || claimedTds > allowed26AsTds) {
            failures.add("GATE_3_TDS_EXCEEDS_STATUTORY_CEILING")
        }

        // Gate 4: Chapter VI-A deductions cannot exceed Gross Total Income
        if (chapterViaDeductions > grossTotalIncome) {
            failures.add("GATE_4_CHAPTER_VIA_EXCEEDS_GTI")
        }

        // Gate 5: Pre-validated Bank Account with IFSC required for refund
        if (bankAccount == null || !IFSC_REGEX.matches(bankAccount.ifscCode.trim().uppercase()) ||
            bankAccount.accountNumber.length !in 7..18 || !bankAccount.isRefundAccount
        ) {
            failures.add("GATE_5_INVALID_REFUND_BANK_ACCOUNT")
        }

        // Gate 6: Section 89 Relief requires prior Form 10E electronic submission acknowledgment
        if (isSection89Claimed && form10EAcknowledgment.isNullOrBlank()) {
            failures.add("GATE_6_FORM_10E_ELECTRONIC_ACKNOWLEDGMENT_MANDATORY")
        }

        return DefectGateEvaluationResult(
            isPassed = failures.isEmpty(),
            failedGates = failures,
            canProceedWithSubmission = failures.isEmpty()
        )
    }
}

