package `in`.zerotax.core.taxengine.senior

import kotlin.math.min

/**
 * Section 34: Senior Citizen (60+ / 80+) Tax Deductions & Section 194P Exemption Engine.
 *
 * Implements:
 * 1. Proactive Section 194P Specified Senior Citizen (Age 75+) Filing Exemption:
 *    - Resident Senior Citizen aged >= 75 during FY.
 *    - Has ONLY Pension income + Interest income from the SAME Specified Bank.
 *    - Bank deducts tax under Sec 194P after Chapter VI-A and Sec 87A rebate.
 *    - Statutorily EXEMPT from filing Income Tax Return under Section 139!
 * 2. Form 12BBA Generator:
 *    - Declaration by Specified Senior Citizen to the Specified Bank under Rule 26D.
 * 3. Section 80TTB Deductions:
 *    - Up to ₹50,000 deduction on Savings, Fixed Deposit (FD), and Recurring Deposit (RD) interest.
 * 4. Section 207(2) Advance Tax Immunity:
 *    - Resident senior citizen without business/profession income has zero liability for Advance Tax (₹0 Sec 234B/234C interest).
 *
 * Pure JVM Kotlin — Zero Android SDK imports.
 */
enum class SeniorCitizenAgeCategory {
    REGULAR_BELOW_60,
    SENIOR_CITIZEN_60_TO_79,
    SUPER_SENIOR_80_PLUS
}

data class Form12BbaDeclaration(
    val seniorCitizenName: String,
    val pan: String,
    val assessmentYear: String,
    val dateOfBirth: String,
    val ageYears: Int,
    val specifiedBankName: String,
    val specifiedBankBranch: String,
    val pensionAccountNumber: String,
    val totalPensionReceiptsRupees: Long,
    val totalInterestFromSameBankRupees: Long,
    val chapterViaDeductionsClaimedRupees: Long,
    val isEligibleFor194PExemption: Boolean,
    val statutoryExemptionMessage: String
)

data class SeniorCitizenTaxBenefitsResult(
    val ageCategory: SeniorCitizenAgeCategory,
    val isEligibleForSection194PExemption: Boolean,
    val section80TtbDeductionAllowedRupees: Long,
    val section207AdvanceTaxImmune: Boolean,
    val section80DMedicalLimitRupees: Long,
    val form12Bba: Form12BbaDeclaration?,
    val benefitExplanationMarkdown: String
)

object SeniorCitizenTaxAndExemptionEngine {

    const val SEC_80TTB_MAX_LIMIT_RUPEES = 50_000L
    const val SEC_80D_SENIOR_MEDICAL_LIMIT_RUPEES = 50_000L

    fun getAgeCategory(ageYears: Int): SeniorCitizenAgeCategory = when {
        ageYears >= 80 -> SeniorCitizenAgeCategory.SUPER_SENIOR_80_PLUS
        ageYears >= 60 -> SeniorCitizenAgeCategory.SENIOR_CITIZEN_60_TO_79
        else -> SeniorCitizenAgeCategory.REGULAR_BELOW_60
    }

    /**
     * Evaluates whether a senior citizen qualifies for Section 194P filing exemption
     * and computes applicable senior statutory benefits (80TTB, 80D, 207(2)).
     */
    fun evaluateSeniorCitizenBenefits(
        ageYears: Int,
        isResident: Boolean,
        seniorCitizenName: String,
        pan: String,
        assessmentYear: String,
        dateOfBirth: String,
        specifiedBankName: String,
        specifiedBankBranch: String,
        pensionAccountNumber: String,
        pensionIncomeRupees: Long,
        bankInterestFromPensionBankRupees: Long,
        interestFromOtherBanksRupees: Long = 0L,
        hasBusinessIncome: Boolean = false,
        hasCapitalGainsOrRentalIncome: Boolean = false,
        chapterViaEligibleInvestmentsRupees: Long = 0L
    ): SeniorCitizenTaxBenefitsResult {
        val ageCategory = getAgeCategory(ageYears)
        val isAge75Plus = ageYears >= 75

        // Section 194P Criteria:
        // 1. Resident Individual aged >= 75
        // 2. Pension income present
        // 3. ONLY interest from the SAME bank receiving pension (zero other banks, zero business, zero CG/rental)
        val is194PEligible = isResident &&
                isAge75Plus &&
                pensionIncomeRupees > 0L &&
                interestFromOtherBanksRupees == 0L &&
                !hasBusinessIncome &&
                !hasCapitalGainsOrRentalIncome

        // Section 80TTB: Up to ₹50,000 for 60+ individuals on total bank interest
        val totalInterest = bankInterestFromPensionBankRupees + interestFromOtherBanksRupees
        val sec80TtbAllowed = if (ageCategory != SeniorCitizenAgeCategory.REGULAR_BELOW_60) {
            min(totalInterest, SEC_80TTB_MAX_LIMIT_RUPEES)
        } else {
            0L
        }

        // Section 207(2): Resident Senior Citizen with no business income is immune from advance tax
        val isAdvanceTaxImmune = isResident &&
                (ageCategory != SeniorCitizenAgeCategory.REGULAR_BELOW_60) &&
                !hasBusinessIncome

        // Section 80D: ₹50,000 for senior citizens (including preventive and uninsured medical expenditure)
        val sec80DLimit = if (ageCategory != SeniorCitizenAgeCategory.REGULAR_BELOW_60) {
            SEC_80D_SENIOR_MEDICAL_LIMIT_RUPEES
        } else {
            25_000L
        }

        val form12Bba = if (isAge75Plus) {
            val message = if (is194PEligible) {
                "Eligible for Section 194P Exemption! By submitting this Form 12BBA declaration to $specifiedBankName, you are legally EXEMPT from filing an Income Tax Return u/s 139."
            } else {
                "Not eligible for Section 194P exemption because you have income from other banks or additional income heads. Regular ITR filing is required."
            }
            Form12BbaDeclaration(
                seniorCitizenName = seniorCitizenName,
                pan = pan,
                assessmentYear = assessmentYear,
                dateOfBirth = dateOfBirth,
                ageYears = ageYears,
                specifiedBankName = specifiedBankName,
                specifiedBankBranch = specifiedBankBranch,
                pensionAccountNumber = pensionAccountNumber,
                totalPensionReceiptsRupees = pensionIncomeRupees,
                totalInterestFromSameBankRupees = bankInterestFromPensionBankRupees,
                chapterViaDeductionsClaimedRupees = chapterViaEligibleInvestmentsRupees,
                isEligibleFor194PExemption = is194PEligible,
                statutoryExemptionMessage = message
            )
        } else {
            null
        }

        val explanation = buildString {
            when (ageCategory) {
                SeniorCitizenAgeCategory.SUPER_SENIOR_80_PLUS -> {
                    append("Super-Senior Citizen (Age $ageYears): Eligible for ₹50,000 Sec 80TTB interest deduction, ₹50,000 Sec 80D medical deduction, and Advance Tax immunity u/s 207(2). ")
                }
                SeniorCitizenAgeCategory.SENIOR_CITIZEN_60_TO_79 -> {
                    append("Senior Citizen (Age $ageYears): Eligible for ₹50,000 Sec 80TTB deduction and Advance Tax immunity u/s 207(2). ")
                }
                SeniorCitizenAgeCategory.REGULAR_BELOW_60 -> {
                    append("Regular Taxpayer (Under 60). ")
                }
            }
            if (is194PEligible) {
                append("100% Statutorily Exempt from filing ITR under Section 194P via Form 12BBA.")
            }
        }

        return SeniorCitizenTaxBenefitsResult(
            ageCategory = ageCategory,
            isEligibleForSection194PExemption = is194PEligible,
            section80TtbDeductionAllowedRupees = sec80TtbAllowed,
            section207AdvanceTaxImmune = isAdvanceTaxImmune,
            section80DMedicalLimitRupees = sec80DLimit,
            form12Bba = form12Bba,
            benefitExplanationMarkdown = explanation.trim()
        )
    }
}

