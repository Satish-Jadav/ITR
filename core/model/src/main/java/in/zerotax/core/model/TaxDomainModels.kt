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
