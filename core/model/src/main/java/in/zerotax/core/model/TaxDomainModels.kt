package `in`.zerotax.core.model

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
    val tdsReported: Long,
    val sec80C: Long = 0L,
    val sec80CCD1B: Long = 0L,
    val sec80CCD2EmployerNps: Long = 0L,
    val sec80D: Long = 0L,
    val exemptHra10_13A: Long = 0L,
    val basicPlusDa: Long = 0L
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

@Serializable
enum class DeductionRegimeScope {
    OLD_REGIME_ONLY,
    BOTH_REGIMES
}

@Serializable
enum class LifeEventTag(val label: String, val icon: String) {
    HOME_LOAN("Running Home Loan", "🏠"),
    HEALTH_INSURANCE("Health Insurance & Checkup", "🏥"),
    NPS_INVESTMENT("NPS / Pension Scheme", "📈"),
    EDUCATION_LOAN("Higher Education Loan", "🎓"),
    CHILDREN_FEES("Children School Fees", "🎒"),
    RENT_NO_HRA("Paying Rent (No HRA)", "🔑"),
    CHARITABLE_DONATION("Charity / Donations", "🤝"),
    EV_CAR_LOAN("Electric Vehicle Loan", "⚡"),
    SAVINGS_INTEREST("Bank Savings Interest", "🏦"),
    DISABLED_DEPENDENT("Medical Care / Disability", "♿"),
    SELF_DISABILITY("Self-Disability (Sec 80U)", "🧑‍🦽"),
    CRITICAL_ILLNESS("Specified Chronic Illness (80DDB)", "💊"),
    POLITICAL_DONATION("Political & Scientific Donation (80GGC/GGA)", "🏛️"),
    SENIOR_DEPOSITS("Senior Citizen Savings & FDs (80TTB)", "👴")
}

/**
 * Plan-based user experience tiers tailored for taxpayers from small-money simple salaried to multi-crore enterprise audit.
 */
@Serializable
enum class FilingTierMode(
    val title: String,
    val subtitle: String,
    val targetPersona: String,
    val badgeIcon: String,
    val recommendedItrForms: List<ItrFormType>
) {
    SAHAJ_SIMPLE(
        title = "Sahaj Simple",
        subtitle = "Salaried & Pensioners (Under ₹50L)",
        targetPersona = "Single/dual Form 16, interest, 1 house property. 1-tap instant refund filing.",
        badgeIcon = "🌱",
        recommendedItrForms = listOf(ItrFormType.ITR_1_SAHAJ)
    ),
    INVESTOR_PRO(
        title = "Growth & Investor",
        subtitle = "Stocks, MFs, Freelancers & 44ADA",
        targetPersona = "Capital gains (111A/112A), Sec 44ADA consulting, multiple properties, deductions optimization.",
        badgeIcon = "📈",
        recommendedItrForms = listOf(ItrFormType.ITR_2, ItrFormType.ITR_4_SUGAM)
    ),
    ENTERPRISE_AUDIT(
        title = "Enterprise Audit Shield",
        subtitle = "Big Money, HNIs, Sec 44AB & RSUs",
        targetPersona = "Turnover > ₹1 Cr, Schedule FA foreign assets, Black Money Act protection, 3-way discrepancy radar, CA dossier.",
        badgeIcon = "🛡️",
        recommendedItrForms = listOf(ItrFormType.ITR_2, ItrFormType.ITR_3, ItrFormType.UNSUPPORTED_REQUIRES_CA_AUDIT_44AB)
    );

    companion object {
        fun autoDetectTier(
            grossIncome: Long,
            hasCapitalGains: Boolean,
            hasPresumptiveBusiness: Boolean,
            hasForeignAssets: Boolean,
            hasAuditExposure: Boolean
        ): FilingTierMode {
            return when {
                hasForeignAssets || hasAuditExposure || grossIncome > 50_00_000L -> ENTERPRISE_AUDIT
                hasCapitalGains || hasPresumptiveBusiness || grossIncome > 20_00_000L -> INVESTOR_PRO
                else -> SAHAJ_SIMPLE
            }
        }
    }
}

/**
 * Available payment channels for discharging tax liabilities under CBDT TIN 2.0 system.
 */
@Serializable
enum class TaxPaymentChannel(
    val title: String,
    val description: String,
    val processingTime: String,
    val statutoryFee: String,
    val iconEmoji: String
) {
    UPI_INSTANT(
        title = "Instant UPI (GPay / PhonePe / BHIM / Paytm)",
        description = "0% transaction charges. Instant Challan 280 receipt generated with CIN & BSR code.",
        processingTime = "Instant (Real-time)",
        statutoryFee = "₹0 (Zero Surcharge)",
        iconEmoji = "⚡"
    ),
    NET_BANKING(
        title = "Retail / Corporate NetBanking",
        description = "Direct payment through authorized banks via CBDT TIN 2.0 gateway.",
        processingTime = "5-10 minutes",
        statutoryFee = "₹0 (Selected banks)",
        iconEmoji = "🏦"
    ),
    DEBIT_CARD(
        title = "Debit Card Gateway",
        description = "Payment via authorized bank debit cards on the e-Filing portal.",
        processingTime = "Instant",
        statutoryFee = "Bank nominal charges",
        iconEmoji = "💳"
    ),
    RTGS_NEFT(
        title = "RTGS / NEFT (Virtual Account Mandate)",
        description = "For large tax payments (> ₹1 Lakh) from any scheduled/foreign bank using generated Mandate.",
        processingTime = "Same-day RTGS",
        statutoryFee = "Standard RBI NEFT/RTGS",
        iconEmoji = "🏛️"
    ),
    OVER_THE_COUNTER(
        title = "Over-The-Counter (Cash / Cheque / DD)",
        description = "Generate pre-filled Challan 280 mandate slip and pay at designated bank branches.",
        processingTime = "1-2 business days",
        statutoryFee = "Free",
        iconEmoji = "📝"
    )
}

@Serializable
data class StatutoryDeductionSpec(
    val sectionCode: String,
    val statutoryRef: String,
    val lifeEventTag: LifeEventTag,
    val deductionTitle: String,
    val maxLimitRupees: Long,
    val regimeScope: DeductionRegimeScope,
    val documentHolder: String,
    val requiredDocumentName: String,
    val whereToFetchInstructions: String,
    val howItAppliesExplanation: String,
    val defaultEligibleAmount: Long = 0L,
    val isSelected: Boolean = false,
    val userClaimedAmount: Long = 0L
)

@Serializable
data class HomeLoanTaxImpact(
    val monthlyEmi: Long,
    val annualTotalEmiPaid: Long,
    val estimatedAnnualInterest: Long,
    val estimatedAnnualPrincipal: Long,
    val sec24bInterestDeductionAllowed: Long,
    val sec80cPrincipalDeductionAllowed: Long,
    val totalHomeLoanDeduction: Long,
    val unclaimedExcessInterest: Long,
    val unclaimedExcessPrincipal: Long
)

@Serializable
data class TaxableIncomeImpact(
    val grossSalary: Long,
    val standardDeduction: Long,
    val homeLoanDeduction: Long,
    val otherChapterVIADeductions: Long,
    val totalDeductions: Long,
    val originalTaxableIncome: Long,
    val newTaxableIncome: Long,
    val taxBeforeHomeLoan: Long,
    val taxAfterHomeLoan: Long,
    val netCashTaxSaved: Long
)

@Serializable
enum class NoticeRiskLevel(val label: String) {
    LOW_RISK_SAFE("Low Risk (Notice-Proof)"),
    MODERATE_CAUTION("Moderate (Check Evidence)"),
    HIGH_RISK_SCRUTINY("High Scrutiny Risk")
}

@Serializable
data class AuditGateResult(
    val gateNumber: Int,
    val gateTitle: String,
    val description: String,
    val isPassed: Boolean,
    val statutoryRef: String,
    val recommendation: String
)

@Serializable
data class PreFilingAuditReport(
    val auditScorePercentage: Int,
    val riskLevel: NoticeRiskLevel,
    val gates: List<AuditGateResult>,
    val canFileDirectly: Boolean,
    val noticeProtectionGuarantee: String
)

@Serializable
data class CpcRefundTimelineStatus(
    val acknowledgmentNumber: String,
    val returnFiledTimestamp: String,
    val isEVerified: Boolean,
    val cpcProcessingStatus: String,
    val expectedRefundDays: String,
    val designatedBankMasked: String,
    val refundAmount: Long
)

@Serializable
enum class ResidenceCityCategory {
    METRO_50_PERCENT,
    NON_METRO_40_PERCENT
}

@Serializable
enum class PropertyType {
    SELF_OCCUPIED,
    LET_OUT,
    DEEMED_LET_OUT
}

@Serializable
data class HousePropertyInput(
    val propertyType: PropertyType = PropertyType.SELF_OCCUPIED,
    val annualRentReceived: Long = 0L,
    val municipalTaxesPaid: Long = 0L,
    val homeLoanInterestSec24b: Long = 0L,
    val coOwnerPercentage: Double = 100.0,
    val tenantPan: String? = null
)

@Serializable
data class AdvanceTaxQuarterlyPayments(
    val q1UptoJune15: Long = 0L,
    val q2UptoSeptember15: Long = 0L,
    val q3UptoDecember15: Long = 0L,
    val q4UptoMarch15: Long = 0L
)

@Serializable
data class CapitalGainsWindfallDates(
    val quarter1UptoJune15: Long = 0L,
    val quarter2UptoSep15: Long = 0L,
    val quarter3UptoDec15: Long = 0L,
    val quarter4UptoMar15: Long = 0L,
    val quarter5Mar16ToMar31: Long = 0L
)

@Serializable
data class Equity112ALot(
    val isin: String,
    val acquisitionDateEpochDay: Long,
    val actualCostTotal: Long,
    val fmvJan31_2018Total: Long,
    val saleConsiderationTotal: Long,
    val transferExpenses: Long = 0L
)

@Serializable
data class ImmovablePropertyLtcgLot(
    val acquiredBeforeJuly23_2024: Boolean,
    val purchaseCostRupees: Long,
    val improvementCostRupees: Long = 0L,
    val acquisitionFyCii: Int,
    val transferFyCii: Int,
    val saleConsiderationOrStampValue50C: Long,
    val transferExpenses: Long = 0L,
    val section54ReinvestmentExemption: Long = 0L
)

@Serializable
data class ForeignEquityLotInput(
    val lotId: String,
    val companyName: String,
    val companyAddress: String = "",
    val countryCodeCbdt: String = "2",
    val zipCode: String = "",
    val acquireDateIso: String,
    val sharesVestedOrBought: Double,
    val sharesSoldDuringCalendarYear: Double = 0.0,
    val acquisitionPricePerShareUsd: Double,
    val peakSharePriceDuringHoldingPeriodUsd: Double = 0.0,
    val peakSharePriceDateIso: String = "",
    val dec31ClosingSharePriceUsd: Double = 0.0,
    val grossDividendsReceivedUsd: Double = 0.0,
    val grossSaleProceedsUsd: Double = 0.0
)

@Serializable
data class SftAndTcs206CEntry(
    val sectionOrSftCode: String,
    val description: String,
    val reportingEntity: String,
    val transactionDateIso: String,
    val grossAmount: Long,
    val taxCollectedOrPaid: Long = 0L
)

@Serializable
data class RegimeComputationResult(
    val regime: TaxRegime,
    val grossTotalIncome: Long,
    val standardDeduction: Long,
    val totalChapterVIADeductions: Long,
    val netTaxableNormalIncomeRounded288A: Long,
    val netTaxableSpecialRateIncome: Long,
    val baseSlabTaxOnNormalIncome: Long,
    val specialRateTax111AAnd112A: Long,
    val rebate87A: Long,
    val marginalRelief87A: Long,
    val taxAfterRebateAndRelief: Long,
    val surchargeAfterMarginalRelief: Long,
    val healthAndEducationCess: Long,
    val totalTaxLiabilityRounded288B: Long
)

@Serializable
data class DualRegimeComparison(
    val newRegimeResult: RegimeComputationResult,
    val oldRegimeResult: RegimeComputationResult,
    val recommendedRegime: TaxRegime,
    val taxDifferenceRupees: Long,
    val breakEvenAdditionalDeductionForOldRegime: Long,
    val unused80CCD2EmployerNpsHeadroom: Long,
    val unused80DPreventiveCheckup5kHeadroom: Long,
    val seniorParentsUninsuredMedical80DHeadroom: Long,
    val unused80CHeadroom: Long,
    val unused80CCD1BHeadroom: Long,
    val actionableNudges: List<String>
)
