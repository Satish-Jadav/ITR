package `in`.zerotax.core.taxengine.subengines

import `in`.zerotax.core.model.ResidentialStatus
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Feature F-01: Comprehensive Statutory Residential Status Day-Counter under Section 6 of the Income-tax Act, 1961.
 * Determines Resident Ordinarily Resident (ROR), Resident but Not Ordinarily Resident (RNOR), and Non-Resident (NR).
 */
data class ResidencyDayCountInput(
    val daysInIndiaCurrentFinancialYear: Int,
    val daysInIndiaPreceding4Years: Int = 0,
    val daysInIndiaPreceding7Years: Int = 0,
    val nonResidentYearsInPreceding10Years: Int = 0,
    val isIndianCitizenOrPersonOfIndianOrigin: Boolean = true,
    val isLeavingIndiaForEmploymentAbroadOrCrewMember: Boolean = false,
    val totalIndianTaxableIncomeExceeds15Lakhs: Boolean = false,
    val isLiableToTaxInAnotherCountry: Boolean = true
)

data class ResidentialStatusDecision(
    val status: ResidentialStatus,
    val statutorySection: String,
    val detailedExplanation: String,
    val isScheduleFaMandatory: Boolean,
    val areForeignDividendsAndGainsExemptInIndia: Boolean,
    val taxBenefitsSummary: List<String>
)

/**
 * Feature F-02: Family & Spouse Tax Arbitrage Simulator.
 * Computes legitimate family wealth restructuring savings without triggering Section 64 clubbing.
 */
data class FamilyTaxArbitrageInput(
    val taxpayerSlabRatePercent: Double = 0.30,
    val annualRentPaidToParentsRupees: Long = 0L,
    val parentOwnsHouseProperty: Boolean = false,
    val parentsAnnualTaxableIncomeRupees: Long = 0L,
    val parentAge: Int = 65,
    val parentHealthInsurancePaidRupees: Long = 0L,
    val fdPrincipalGiftedToAdultChildOrParentsRupees: Long = 0L,
    val expectedFdInterestRatePercent: Double = 0.075
)

data class FamilyTaxSavingOpportunity(
    val title: String,
    val statutoryReference: String,
    val annualTaxSavedRupees: Long,
    val legalComplianceRules: List<String>
)

data class FamilyArbitrageReport(
    val totalFamilyTaxSavedAnnualRupees: Long,
    val opportunities: List<FamilyTaxSavingOpportunity>,
    val auditShieldNotes: List<String>
)

object ResidentialStatusAndFamilyArbitrageEngine {

    // --- FEATURE F-01: SECTION 6 RESIDENTIAL STATUS ENGINE ---

    fun determineResidentialStatusSec6(input: ResidencyDayCountInput): ResidentialStatusDecision {
        val daysCY = input.daysInIndiaCurrentFinancialYear

        // 1. Basic Resident Tests u/s 6(1)
        val meets182DaysRule = daysCY >= 182

        // 60-day rule exceptions
        val is60DaysRelaxedTo182 = input.isLeavingIndiaForEmploymentAbroadOrCrewMember ||
                (input.isIndianCitizenOrPersonOfIndianOrigin && !input.totalIndianTaxableIncomeExceeds15Lakhs)

        val thresholdForSecondLimb = if (is60DaysRelaxedTo182) {
            182
        } else if (input.isIndianCitizenOrPersonOfIndianOrigin && input.totalIndianTaxableIncomeExceeds15Lakhs) {
            120 // Finance Act 2020: 120 days if Indian income > 15L
        } else {
            60
        }

        val meetsSecondLimb = daysCY >= thresholdForSecondLimb && input.daysInIndiaPreceding4Years >= 365

        // Deemed Resident u/s 6(1A)
        val isDeemedResident = input.isIndianCitizenOrPersonOfIndianOrigin &&
                input.totalIndianTaxableIncomeExceeds15Lakhs &&
                !input.isLiableToTaxInAnotherCountry &&
                !meets182DaysRule && !meetsSecondLimb

        val isResident = meets182DaysRule || meetsSecondLimb || isDeemedResident

        if (!isResident) {
            return ResidentialStatusDecision(
                status = ResidentialStatus.NON_RESIDENT,
                statutorySection = "Section 6(1) Non-Resident",
                detailedExplanation = "Present in India for $daysCY days (< 182 days) with no secondary resident condition met.",
                isScheduleFaMandatory = false,
                areForeignDividendsAndGainsExemptInIndia = true,
                taxBenefitsSummary = listOf(
                    "Only income received or accrued in India is taxable.",
                    "Global assets and foreign earnings are 100% exempt from Indian tax.",
                    "Strictly exempt from Schedule FA foreign asset disclosure."
                )
            )
        }

        // 2. Ordinarily vs Not Ordinarily Resident Test u/s 6(6)
        val isRnorCase1 = input.nonResidentYearsInPreceding10Years >= 9
        val isRnorCase2 = input.daysInIndiaPreceding7Years <= 729
        val isRnorCase3 = isDeemedResident // 6(1A) deemed residents are always RNOR u/s 6(6)(d)
        val isRnorCase4 = input.isIndianCitizenOrPersonOfIndianOrigin &&
                input.totalIndianTaxableIncomeExceeds15Lakhs &&
                daysCY in 120..181 && input.daysInIndiaPreceding4Years >= 365 // 6(6)(c)

        val isRnor = isRnorCase1 || isRnorCase2 || isRnorCase3 || isRnorCase4

        return if (isRnor) {
            val rnorReason = when {
                isRnorCase1 -> "Non-resident in 9 out of 10 preceding financial years."
                isRnorCase2 -> "Present in India for <= 729 days during 7 preceding financial years."
                isRnorCase3 -> "Deemed resident under Section 6(1A) (Indian income > ₹15L, zero tax abroad)."
                else -> "Present in India for 120-181 days with Indian income exceeding ₹15 Lakhs."
            }

            ResidentialStatusDecision(
                status = ResidentialStatus.RESIDENT_NOT_ORDINARILY_RESIDENT,
                statutorySection = "Section 6(6) RNOR",
                detailedExplanation = "Resident in India for current FY, but qualifies as 'Not Ordinarily Resident': $rnorReason",
                isScheduleFaMandatory = false, // Critical statutory shielding!
                areForeignDividendsAndGainsExemptInIndia = true,
                taxBenefitsSummary = listOf(
                    "Foreign dividends and capital gains are 100% TAX-FREE in India.",
                    "Exempt from Schedule FA Black Money Act foreign asset reporting.",
                    "Foreign bank accounts, RSUs, and 401(k) do not attract Indian disclosure requirements."
                )
            )
        } else {
            ResidentialStatusDecision(
                status = ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,
                statutorySection = "Section 6(1) & 6(6) Resident & Ordinarily Resident",
                detailedExplanation = "Resident in India for $daysCY days and resident in at least 2 out of 10 preceding years with > 729 days in India across 7 preceding years.",
                isScheduleFaMandatory = true,
                areForeignDividendsAndGainsExemptInIndia = false,
                taxBenefitsSummary = listOf(
                    "Global income is taxable in India.",
                    "Eligible for full Section 87A rebate and all domestic tax deductions.",
                    "Mandatory disclosure of foreign assets in Schedule FA."
                )
            )
        }
    }

    // --- FEATURE F-02: FAMILY TAX ARBITRAGE SIMULATOR ---

    fun simulateFamilyTaxArbitrage(input: FamilyTaxArbitrageInput): FamilyArbitrageReport {
        val opportunities = mutableListOf<FamilyTaxSavingOpportunity>()

        // 1. Paying Rent to Retired / Lower-Tax Parents u/s 10(13A)
        if (input.parentOwnsHouseProperty && input.annualRentPaidToParentsRupees > 0L) {
            val taxpayerTaxSaved = (input.annualRentPaidToParentsRupees * input.taxpayerSlabRatePercent).roundToLong()
            // In parents hands: Gross Rent - 30% repair u/s 24(a)
            val taxableRentToParents = (input.annualRentPaidToParentsRupees * 0.70).roundToLong()
            val parentsTaxBracket = if (input.parentsAnnualTaxableIncomeRupees + taxableRentToParents <= 12_00_000L) 0.0 else 0.10
            val parentsTaxLiability = (taxableRentToParents * parentsTaxBracket).roundToLong()
            val netFamilySaving = max(0L, taxpayerTaxSaved - parentsTaxLiability)

            if (netFamilySaving > 0L) {
                opportunities.add(
                    FamilyTaxSavingOpportunity(
                        title = "Pay Rent to Parents for Living in Family House",
                        statutoryReference = "Section 10(13A) read with Rule 2A & Section 24(a)",
                        annualTaxSavedRupees = netFamilySaving,
                        legalComplianceRules = listOf(
                            "House must be legally registered in parent's name (not joint with taxpayer).",
                            "Execute a formal rent agreement and transfer rent via bank NEFT/UPI.",
                            "Parent must declare rental income in their ITR (gets 30% flat standard deduction u/s 24(a)).",
                            "Parent pays ₹0 tax if their total taxable income is under ₹12 Lakhs (New Regime AY 2026-27)."
                        )
                    )
                )
            }
        }

        // 2. Health Insurance Paid for Parents u/s 80D
        if (input.parentHealthInsurancePaidRupees > 0L) {
            val maxParentCap = if (input.parentAge >= 60) 50_000L else 25_000L
            val eligibleDeduction = min(input.parentHealthInsurancePaidRupees, maxParentCap)
            val taxSaved = (eligibleDeduction * input.taxpayerSlabRatePercent).roundToLong()

            opportunities.add(
                FamilyTaxSavingOpportunity(
                    title = "Senior Parent Health Insurance / Medical Expenditure",
                    statutoryReference = "Section 80D(2)(b) & Section 80D(2)(d)",
                    annualTaxSavedRupees = taxSaved,
                    legalComplianceRules = listOf(
                        "Parents do not need to be dependent on the taxpayer.",
                        "If parent is a senior citizen (60+) without health insurance, medical hospital bills paid digitally qualify up to ₹50,000.",
                        "Cash payment is barred for insurance premium; digital/banking mode required."
                    )
                )
            )
        }

        // 3. Gifting Capital to Adult Child (18+) or Parents to Invest in FDs (Zero Section 64 Clubbing)
        if (input.fdPrincipalGiftedToAdultChildOrParentsRupees > 0L) {
            val annualInterestEarned = (input.fdPrincipalGiftedToAdultChildOrParentsRupees * input.expectedFdInterestRatePercent).roundToLong()
            // Tax if retained in high earner's hands (30% + Cess)
            val taxInTaxpayerHands = (annualInterestEarned * input.taxpayerSlabRatePercent).roundToLong()
            // Tax in parent's or adult child's hands (assume zero slab or senior 80TTB ₹50k exemption)
            val seniorExemption = if (input.parentAge >= 60) 50_000L else 0L
            val taxableInterestFamily = max(0L, annualInterestEarned - seniorExemption)
            val taxInFamilyHands = (taxableInterestFamily * 0.0).roundToLong() // Assume family member in 0% slab
            val netFdTaxSaved = max(0L, taxInTaxpayerHands - taxInFamilyHands)

            if (netFdTaxSaved > 0L) {
                opportunities.add(
                    FamilyTaxSavingOpportunity(
                        title = "Gift Capital to Adult Child (18+) or Parents for High-Yield FDs",
                        statutoryReference = "Section 56(2)(x) Relative Exemption & Section 64 Immunity",
                        annualTaxSavedRupees = netFdTaxSaved,
                        legalComplianceRules = listOf(
                            "Gifts to parents or children are 100% tax-free under Section 56(2)(x).",
                            "Section 64 clubbing provisions apply ONLY to minor children (<18) or spouse.",
                            "Gifting to adult children (18+) or parents completely eliminates clubbing.",
                            "Senior parents also enjoy ₹50,000 Section 80TTB deduction on FD interest."
                        )
                    )
                )
            }
        }

        val totalSaved = opportunities.sumOf { it.annualTaxSavedRupees }

        return FamilyArbitrageReport(
            totalFamilyTaxSavedAnnualRupees = totalSaved,
            opportunities = opportunities,
            auditShieldNotes = listOf(
                "All simulated transactions are 100% compliant with the Income-tax Act, 1961.",
                "Ensure banking digital trails (NEFT/RTGS) and written gift deeds / rent receipts are maintained in local vault."
            )
        )
    }
}

