package in.zerotax.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SourceKind {
    FORM_16,
    AIS_TDS_192,
    PREFILL
}

@Serializable
enum class ItrFormType {
    ITR_1_SAHAJ,
    ITR_4_SUGAM,
    ITR_2,
    ITR_3,
    UNSUPPORTED_REQUIRES_CA_AUDIT_44AB
}

@Serializable
enum class ResidentialStatus {
    RESIDENT_ORDINARILY_RESIDENT,      // ROR
    RESIDENT_NOT_ORDINARILY_RESIDENT,  // RNOR
    NON_RESIDENT                       // NR
}

@Serializable
enum class AssessmentYearProfile {
    AY_2025_26,
    AY_2026_27
}

@Serializable
enum class CitizenCategory {
    REGULAR_BELOW_60,
    SENIOR_60_TO_79,
    SUPER_SENIOR_80_PLUS
}

@Serializable
enum class TaxRegime {
    NEW_REGIME_115BAC,
    OLD_REGIME
}

@Serializable
enum class DonationCategory {
    HUNDRED_PERCENT_NO_LIMIT,
    FIFTY_PERCENT_NO_LIMIT,
    HUNDRED_PERCENT_WITH_10PCT_CAP,
    FIFTY_PERCENT_WITH_10PCT_CAP
}

@Serializable
data class Section80GDonationInput(
    val doneePan: String,
    val donationInCashRupees: Long,
    val donationOtherModeRupees: Long,
    val category: DonationCategory
)

@Serializable
data class EmployerSalarySource(
    val employerTan: String,
    val employerName: String,
    val source: SourceKind = SourceKind.FORM_16,
    val grossSalary: Long,
    val sec10Exemptions: Long,
    val sec16DeductionsClaimedInDoc: Long,
    val netTaxableSalaryInDoc: Long,
    val tdsReported: Long
)

@Serializable
data class EmployerReconciliationItem(
    val employerTan: String,
    val employerName: String,
    val selectedSourceKind: SourceKind,
    val grossSalary: Long,
    val sec10Exemptions: Long,
    val sec16Deductions: Long,
    val netSalary: Long,
    val tdsReportedInSource: Long,
    val tdsIn26As: Long,
    val tdsAllowed: Long,
    val hasDiscrepancy: Boolean,
    val unreflectedTdsAmount: Long
)

@Serializable
data class ReconciledSalarySummary(
    val employers: List<EmployerReconciliationItem>,
    val aggregateGrossSalary: Long,
    val aggregateSec10Exemptions: Long,
    val aggregateNetTaxableSalary: Long,
    val aggregateTdsAllowed: Long,
    val totalDiscrepancyAmount: Long
)

@Serializable
data class BankAccountDetails(
    val bankName: String,
    val accountNumber: String,
    val ifscCode: String,
    val accountType: String = "SAVINGS",
    val isRefundAccount: Boolean = true,
    val isPreValidated: Boolean = true
)

@Serializable
enum class FilingSection(val code: String, val description: String) {
    ON_OR_BEFORE_DUE_DATE_139_1("11", "Filed on or before due date (u/s 139(1))"),
    BELATED_RETURN_139_4("12", "Belated return (u/s 139(4))"),
    REVISED_RETURN_139_5("13", "Revised return (u/s 139(5))")
}

@Serializable
data class TaxpayerDeclaration(
    val fullName: String,
    val fatherName: String = "",
    val pan: String,
    val place: String = "Bengaluru",
    val capacity: String = "SELF",
    val filingDate: String = "2026-07-15"
)

@Serializable
data class DirectFilingSubmissionResult(
    val isSuccess: Boolean,
    val acknowledgmentNumber: String,
    val token: String,
    val timestamp: String,
    val verificationStatus: String,
    val message: String
)

@Serializable
data class AadhaarVerificationResult(
    val isSuccess: Boolean,
    val itrVAcknowledgmentNumber: String,
    val timestamp: String,
    val cpcVerificationRef: String,
    val receiptDownloadToken: String
)

@Serializable
enum class AisCategory(val code: String, val displayName: String) {
    SAVINGS_INTEREST("INT-SB", "Savings Bank Account Interest"),
    TIME_DEPOSIT_INTEREST("INT-TD", "Fixed / Term Deposit Interest"),
    DIVIDENDS("DIV", "Dividend from Shares / Mutual Funds"),
    SALE_OF_SECURITIES("SFT-006", "Mutual Fund / Equity Sale Proceeds"),
    RENT_RECEIVED("RENT", "Rent Received (Sec 194-IB / 194-I)"),
    HIGH_VALUE_CASH("SFT-005", "High-Value Cash Deposit / Withdrawal")
}

@Serializable
data class AisTransactionRecord(
    val transactionId: String,
    val category: AisCategory,
    val reportingEntity: String,
    val accountOrFolioNumber: String,
    val grossAmount: Long,
    val tdsWithheld: Long = 0L,
    val quarterOrDate: String,
    val suggestedSchedule: String,
    val suggestedExemptionSection: String? = null,
    val suggestedExemptionAmount: Long = 0L,
    val isAccepted: Boolean = true
)

@Serializable
data class Form60ClientProfile(
    val declarantName: String,
    val fatherName: String,
    val dateOfBirth: String,
    val flatDoorBlock: String,
    val premisesName: String = "",
    val roadStreet: String,
    val cityDistrict: String,
    val state: String,
    val pincode: String,
    val mobileNumber: String,
    val email: String,
    val estimatedAgriculturalIncome: Long = 0L,
    val estimatedOtherIncome: Long = 0L,
    val identityDocumentType: String = "AADHAAR_CARD",
    val identityDocumentNumber: String = "••••••••5819",
    val transactionType: String = "OPENING_BANK_ACCOUNT_AND_TAX_FILING"
)

@Serializable
data class SmartTransactionNudge(
    val title: String,
    val description: String,
    val statutoryReference: String,
    val taxBenefitAmount: Long,
    val category: AisCategory,
    val isApplied: Boolean = true
)
