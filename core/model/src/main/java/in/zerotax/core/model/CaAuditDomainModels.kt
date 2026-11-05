package `in`.zerotax.core.model

/**
 * Domain models for Chartered Accountant (CA) & Auditor Verification Portal,
 * Client Pairing Workflows, and "Think Like a CA" Deduction Maximization.
 *
 * Invariant: Pure JVM Kotlin (zero Android dependencies).
 */

/**
 * Verified profile of an auditor / Chartered Accountant registered with ICAI.
 */
data class CaProfile(
    val membershipNumber: String,
    val caName: String,
    val firmRegistrationNumber: String? = null,
    val email: String,
    val phone: String,
    val isIcaiVerified: Boolean = true,
    val activeClientsCount: Int = 0
)

/**
 * Lifecycle status of an audit assignment under CA review.
 */
enum class ClientAuditStatus(val label: String) {
    PENDING_REVIEW("Pending Audit Review"),
    IN_AUDIT("Audit In Progress"),
    QUERIES_RAISED("CA Clarification Queries Raised"),
    APPROVED_SIGNED_OFF("Statutory Sign-Off Approved"),
    REJECTED("Audit Non-Compliant / Rejected")
}

/**
 * Scope of statutory audit requirement.
 */
enum class AuditType(val statutoryCode: String, val title: String) {
    TAX_AUDIT_44AB("Sec 44AB", "Tax Audit (Form 3CD/3CA/3CB)"),
    COMPANY_AUDIT_ITR6("ITR-6", "Corporate Statutory Company Audit"),
    TRUST_AUDIT_ITR7("ITR-7", "Trust / Section 12A / 10(23C) Audit"),
    LLP_AUDIT_ITR5("ITR-5", "Partnership Firm / LLP Section 40(b) Audit"),
    SALARIED_HIGH_NET_WORTH("ITR-2", "HNI / Multi-Source Wealth Audit")
}

/**
 * Single-use secure client pairing session.
 * Taxpayer generates this on-device; CA inputs the pairing code to attach the client.
 */
data class ClientPairingSession(
    val pairingCode: String,
    val clientPan: String,
    val clientName: String,
    val grossTurnoverOrIncome: Long,
    val auditType: AuditType,
    val itrForm: String,
    val taxPayableOrRefund: Long,
    val isRefund: Boolean,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val isLinked: Boolean = false,
    val auditStatus: ClientAuditStatus = ClientAuditStatus.PENDING_REVIEW
)

/**
 * Severity of an auditor observation in Form 3CD or return schedules.
 */
enum class ObservationSeverity {
    INFO,
    ADVISORY,
    STATUTORY_QUALIFICATION
}

/**
 * Individual auditor observation or note on client compliance.
 */
data class CaAuditObservation(
    val clauseOrSection: String,
    val description: String,
    val severity: ObservationSeverity = ObservationSeverity.INFO
)

/**
 * Formal audit sign-off payload with Unique Document Identification Number (UDIN) draft.
 */
data class CaAuditSignOffPayload(
    val caMembershipNumber: String,
    val clientPan: String,
    val assessmentYear: String = "2026-27",
    val auditType: AuditType,
    val udinDraft: String,
    val isSignedOff: Boolean = true,
    val auditObservations: List<CaAuditObservation> = emptyList(),
    val signedAtEpochMs: Long,
    val signatureHash: String
)

/**
 * Category of legal statutory deductions analyzed by CA heuristic engine.
 */
enum class DeductionCategory(val title: String) {
    SALARY_PERQUISITE("Salary Allowances & Standard Exemption"),
    CHAPTER_VIA_INVESTMENT("Chapter VI-A Investments & Savings"),
    HEALTH_AND_MEDICAL("Health & Medical Care"),
    HOME_AND_RENT("Housing, Home Loan & Rent Relief"),
    BUSINESS_AND_DEPRECIATION("Business Expenses & Accelerated Depreciation"),
    FAMILY_ARBITRAGE("Family & Multi-Member Tax Arbitrage")
}

/**
 * Actionable legal statutory deduction opportunity surfaced by the "Think Like a CA" engine.
 */
data class DeductionOpportunity(
    val sectionCode: String,
    val title: String,
    val category: DeductionCategory,
    val maxEligibleAmount: Long,
    val currentClaimedAmount: Long,
    val potentialAdditionalClaim: Long,
    val estimatedTaxSavedRupees: Long,
    val legalExplanation: String,
    val requiredDocumentation: String,
    val isApplicableInNewRegime: Boolean = false,
    val isRebate87ATarget: Boolean = false
)

/**
 * Comprehensive CA deduction and maximum refund optimization report.
 */
data class ThinkLikeACaOptimizationReport(
    val pan: String,
    val grossTotalIncome: Long,
    val baselineTaxLiability: Long,
    val baselineTdsPrepaid: Long,
    val optimizedTaxLiability: Long,
    val optimizedRefundDue: Long,
    val totalTaxSavedRupees: Long,
    val bringsBelowRebate87A: Boolean,
    val recommendations: List<DeductionOpportunity>,
    val executiveSummary: String
)

