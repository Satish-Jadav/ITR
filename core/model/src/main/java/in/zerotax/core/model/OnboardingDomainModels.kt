package `in`.zerotax.core.model

import kotlinx.serialization.Serializable

/**
 * Lifecycle steps for the Dynamic Automated Zero-Friction Onboarding Wizard.
 */
@Serializable
enum class OnboardingWizardStep(val stepNumber: Int, val title: String) {
    TRUST_AND_AUTH(1, "Government Auth & Trust Shield"),
    STREAM_HYDRATION_RADAR(2, "Automated Data Hydration"),
    SELECT_PLAN(3, "Select Filing Plan"),
    PLAN_SPECIFIC_INPUTS(4, "Plan-Specific Requirements"),
    UNIFIED_DOSSIER_REVIEW(5, "Pre-Filing Master Tax Dossier")
}

/**
 * Execution status for each parallel data stream in the hydration radar.
 */
@Serializable
enum class IngestionStreamStatus {
    IDLE,
    IN_PROGRESS,
    SUCCESS,
    SKIPPED,
    FAILED
}

/**
 * Individual data stream state tracking item.
 */
@Serializable
data class IngestionStreamItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val status: IngestionStreamStatus = IngestionStreamStatus.IDLE,
    val details: String = "",
    val badgeText: String = ""
)

/**
 * Plan-specific detailed summary sealed hierarchy attached to the Master Tax Dossier.
 */
@Serializable
sealed class PlanSpecificDossierSummary {

    @Serializable
    data class SahajSummary(
        val employerName: String = "",
        val employerTan: String = "",
        val form16Discovered: Boolean = false,
        val standardDeductionRupees: Long = 75000L,
        val chapterViaDeductionsRupees: Long = 0L,
        val hraExemptionRupees: Long = 0L,
        val savingsInterestRupees: Long = 0L
    ) : PlanSpecificDossierSummary()

    @Serializable
    data class InvestorSummary(
        val stcg111ARupees: Long = 0L,
        val ltcg112ARupees: Long = 0L,
        val grandFatheredIsinCount: Int = 0,
        val foreignAssetsCount: Int = 0,
        val foreignAssetsPeakValueInr: Long = 0L,
        val foreignDividendsInr: Long = 0L,
        val cryptoGainRupees: Long = 0L,
        val crypto194sTdsRupees: Long = 0L
    ) : PlanSpecificDossierSummary()

    @Serializable
    data class SugamSummary(
        val professionOrBusinessTitle: String = "Technical & IT Consultancy (Sec 44ADA)",
        val grossTurnoverRupees: Long = 0L,
        val declaredProfitRupees: Long = 0L,
        val declaredProfitPercent: Double = 50.0,
        val digitalReceiptsPercent: Double = 98.4,
        val isElevatedLimitEligible: Boolean = true,
        val sundryDebtorsRupees: Long = 0L,
        val sundryCreditorsRupees: Long = 0L,
        val stockInTradeRupees: Long = 0L,
        val cashAndBankBalanceRupees: Long = 0L
    ) : PlanSpecificDossierSummary()

    @Serializable
    data class EnterpriseAuditSummary(
        val businessName: String = "",
        val gstin: String = "",
        val auditedTurnoverRupees: Long = 0L,
        val gstr3bTurnoverRupees: Long = 0L,
        val gstr2bEligibleItcRupees: Long = 0L,
        val msme43BhDisallowanceRupees: Long = 0L,
        val overdueMsmeCreditorsCount: Int = 0,
        val clause44RegisteredExpensePercent: Double = 84.5,
        val clause21CashDisallowanceRupees: Long = 0L,
        val caMembershipNumber: String = "",
        val caAuditorName: String = "",
        val udinDraft: String = "",
        val auditStatusText: String = "Ready for Statutory Sign-Off"
    ) : PlanSpecificDossierSummary()
}

/**
 * Unified Pre-Filing Master Tax Dossier synthesizing all streams into an encrypted,
 * cryptographically sealed compliance artifact.
 */
@Serializable
data class UnifiedTaxDossier(
    val assesseePan: String,
    val assesseeName: String,
    val maskedAadhaar: String,
    val assessmentYear: String = "2026-27",
    val residentialStatus: String = "Resident Ordinarily Resident (ROR)",
    val filingTier: FilingTierMode,
    val recommendedItrForm: ItrFormType,
    val refundBankName: String,
    val refundBankAccountMasked: String,
    val refundBankIfsc: String,
    val grossIncomeRupees: Long,
    val totalDeductionsRupees: Long,
    val taxableIncomeRupees: Long,
    val computedTaxNewRegime: Long,
    val computedTaxOldRegime: Long,
    val recommendedRegime: TaxRegime,
    val taxSavingsAmount: Long,
    val totalTdsCreditsDiscovered: Long,
    val advanceTaxPaid: Long,
    val netPayableOrRefund: Long,
    val isRefund: Boolean,
    val threeWayTdsMatch: Boolean = true,
    val planSpecificSummary: PlanSpecificDossierSummary,
    val cryptographicAuditHash: String,
    val generatedAtEpochMs: Long = System.currentTimeMillis()
)
