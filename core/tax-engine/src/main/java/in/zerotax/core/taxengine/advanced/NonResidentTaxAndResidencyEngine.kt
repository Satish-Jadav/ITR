package `in`.zerotax.core.taxengine.advanced

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Dual-Region Tax Residency & NRI Statutory Compliance Engine
 *
 * Implements deterministic statutory evaluation under the Income-tax Act, 1961 for:
 * 1. Section 6(1) & 6(6) Residential Status Determination:
 *    - 182-day primary rule.
 *    - 60-day + 365-day secondary rule.
 *    - 120-day / 182-day rule for Indian Citizens / PIO visiting India (Finance Act 2020 amendment for > ₹15L Indian income).
 *    - Section 6(1A) Deemed Resident status for stateless income > ₹15L.
 *    - Classification into ROR (Resident Ordinarily Resident), RNOR (Resident Not Ordinarily Resident), or NRI (Non-Resident).
 * 2. Section 10(4)(ii) Statutory Exemptions:
 *    - NRE (Non-Resident External) account interest: 100% TAX-FREE.
 *    - FCNR (Foreign Currency Non-Resident) deposit interest: 100% TAX-FREE.
 * 3. NRO Account & Section 195 TDS Credit Reconciliation:
 *    - Taxable under domestic law, with standard 30% (+ cess) withholding.
 * 4. Section 90 / 90A DTAA Relief & Electronic Form 10F Generator:
 *    - Computes treaty relief (e.g., 10% or 15% DTAA cap vs 30% domestic rate).
 *    - Generates statutory Form 10F self-declaration markdown.
 */
object NonResidentTaxAndResidencyEngine {

    enum class ResidencyStatus(val title: String, val shortCode: String) {
        RESIDENT_ORDINARILY_RESIDENT("Resident and Ordinarily Resident", "ROR"),
        RESIDENT_NOT_ORDINARILY_RESIDENT("Resident but Not Ordinarily Resident", "RNOR"),
        NON_RESIDENT_INDIAN("Non-Resident Indian", "NRI")
    }

    data class ResidencyEvaluationInput(
        val daysInIndiaCurrentFy: Int, // FY 2025-26
        val daysInIndiaPast4Fys: Int = 0, // Sum of 4 preceding FYs
        val isIndianCitizenOrPersonOfIndianOrigin: Boolean = true,
        val isVisitingIndia: Boolean = false,
        val isLeavingIndiaForEmploymentOrCrew: Boolean = false,
        val totalIndianIncomeExcludingForeignRupees: Long = 0L,
        val isNotLiableToTaxInAnyOtherCountry: Boolean = false,
        val residentInAtLeast2Of10PastYears: Boolean = false,
        val daysInIndiaPast7Fys: Int = 0
    )

    data class ResidencyDecision(
        val status: ResidencyStatus,
        val statutoryClause: String,
        val explanation: String,
        val isGlobalIncomeTaxableInIndia: Boolean,
        val isEligibleForNreTaxExemption: Boolean,
        val isEligibleForSection44AD: Boolean // NRIs are barred from 44AD
    )

    data class NriTaxPortfolioInput(
        val residencyStatus: ResidencyStatus,
        val indianSalaryIncomeRupees: Long = 0L,
        val indianRentalIncomeRupees: Long = 0L,
        val indianCapitalGainsRupees: Long = 0L,
        val nreAccountInterestRupees: Long = 0L,
        val fcnrDepositInterestRupees: Long = 0L,
        val nroAccountInterestRupees: Long = 0L,
        val foreignSourcedIncomeRupees: Long = 0L, // Salary or business earned outside India
        val dtaaCountry: String = "United States",
        val dtaaTreatyRatePercent: Double = 15.0 // DTAA beneficial rate
    )

    data class NriTaxComputationResult(
        val residencyStatus: ResidencyStatus,
        val grossForeignIncomeExcludedFromIndia: Long,
        val nreInterestExemptUnderSec10_4: Long,
        val fcnrInterestExemptUnderSec10_4: Long,
        val taxableIndianIncome: Long,
        val nroTdsDeductedAt30Percent: Long,
        val dtaaReliefClaimedSec90: Long,
        val finalIndianTaxLiability: Long,
        val estimatedRefundOrBalancePayable: Long,
        val form10FMarkdown: String
    )

    /**
     * Deterministically evaluates Section 6 residential status under Indian Income Tax law.
     */
    fun evaluateResidency(input: ResidencyEvaluationInput): ResidencyDecision {
        val daysCurrent = input.daysCurrentFyClamped
        val daysPast4 = input.daysInIndiaPast4Fys
        val indianIncome = input.totalIndianIncomeExcludingForeignRupees

        // Step 1: Check Deemed Resident u/s 6(1A)
        if (input.isIndianCitizenOrPersonOfIndianOrigin &&
            input.isNotLiableToTaxInAnyOtherCountry &&
            indianIncome > 1500000L &&
            daysCurrent < 182
        ) {
            return ResidencyDecision(
                status = ResidencyStatus.RESIDENT_NOT_ORDINARILY_RESIDENT,
                statutoryClause = "Section 6(1A) read with Section 6(6)(d)",
                explanation = "Deemed Resident: Indian citizen with Indian income > ₹15 Lakhs not liable to tax in any other country. Statutorily classified as RNOR.",
                isGlobalIncomeTaxableInIndia = false,
                isEligibleForNreTaxExemption = false,
                isEligibleForSection44AD = false
            )
        }

        // Step 2: Test Primary 182-Day Rule (Section 6(1)(a))
        val isPrimaryResident = daysCurrent >= 182

        // Step 3: Test Secondary 60-Day + 365-Day Rule (Section 6(1)(c))
        val isSecondaryResident = if (input.isLeavingIndiaForEmploymentOrCrew) {
            // For crew/employment abroad, 60 days is statutorily substituted with 182 days
            false
        } else if (input.isIndianCitizenOrPersonOfIndianOrigin && input.isVisitingIndia) {
            // Citizen/PIO visiting India:
            if (indianIncome > 1500000L) {
                // If Indian income > ₹15L, substituted with 120 days (Finance Act 2020)
                daysCurrent >= 120 && daysPast4 >= 365
            } else {
                // If Indian income <= ₹15L, substituted with 182 days
                false
            }
        } else {
            daysCurrent >= 60 && daysPast4 >= 365
        }

        val isResident = isPrimaryResident || isSecondaryResident

        if (!isResident) {
            return ResidencyDecision(
                status = ResidencyStatus.NON_RESIDENT_INDIAN,
                statutoryClause = "Section 6(1) Non-Resident",
                explanation = "Present in India for $daysCurrent days in FY 2025-26 (below statutory thresholds). Classified as Non-Resident Indian (NRI).",
                isGlobalIncomeTaxableInIndia = false,
                isEligibleForNreTaxExemption = true,
                isEligibleForSection44AD = false // NRIs cannot file u/s 44AD
            )
        }

        // Step 4: Resident: Test for ROR vs RNOR under Section 6(6)
        val isVisiting120DaySpecialCase = input.isIndianCitizenOrPersonOfIndianOrigin &&
                input.isVisitingIndia &&
                indianIncome > 1500000L &&
                daysCurrent in 120..181

        if (isVisiting120DaySpecialCase) {
            return ResidencyDecision(
                status = ResidencyStatus.RESIDENT_NOT_ORDINARILY_RESIDENT,
                statutoryClause = "Section 6(6)(c) Proviso",
                explanation = "Resident visiting India between 120 and 181 days with Indian income > ₹15 Lakhs. Statutorily treated as RNOR (foreign income exempt).",
                isGlobalIncomeTaxableInIndia = false,
                isEligibleForNreTaxExemption = false,
                isEligibleForSection44AD = false
            )
        }

        val isRnor = !input.residentInAtLeast2Of10PastYears || input.daysInIndiaPast7Fys <= 729

        return if (isRnor) {
            ResidencyDecision(
                status = ResidencyStatus.RESIDENT_NOT_ORDINARILY_RESIDENT,
                statutoryClause = "Section 6(6)(a)",
                explanation = "Resident but Not Ordinarily Resident: Non-resident in 9 of past 10 years or stay in past 7 years <= 729 days. Foreign income exempt.",
                isGlobalIncomeTaxableInIndia = false,
                isEligibleForNreTaxExemption = false,
                isEligibleForSection44AD = false
            )
        } else {
            ResidencyDecision(
                status = ResidencyStatus.RESIDENT_ORDINARILY_RESIDENT,
                statutoryClause = "Section 6(1) ROR",
                explanation = "Resident and Ordinarily Resident: Worldwide global income is chargeable to tax under Section 5(1).",
                isGlobalIncomeTaxableInIndia = true,
                isEligibleForNreTaxExemption = false,
                isEligibleForSection44AD = true
            )
        }
    }

    private val ResidencyEvaluationInput.daysCurrentFyClamped: Int
        get() = min(366, max(0, daysInIndiaCurrentFy))

    /**
     * Computes NRI tax liability, NRE 10(4) exemption, DTAA Section 90 relief, and Form 10F.
     */
    fun computeNriTax(input: NriTaxPortfolioInput, pan: String = "ABCDE1234F"): NriTaxComputationResult {
        val isNriOrRnor = input.residencyStatus != ResidencyStatus.RESIDENT_ORDINARILY_RESIDENT

        // 1. Foreign Income is 100% excluded for NRI/RNOR
        val grossForeignExcluded = if (isNriOrRnor) input.foreignSourcedIncomeRupees else 0L

        // 2. NRE & FCNR interest are 100% tax-free u/s 10(4)(ii) for NRIs
        val nreExempt = if (input.residencyStatus == ResidencyStatus.NON_RESIDENT_INDIAN) input.nreAccountInterestRupees else 0L
        val fcnrExempt = if (input.residencyStatus == ResidencyStatus.NON_RESIDENT_INDIAN) input.fcnrDepositInterestRupees else 0L

        // 3. Indian Taxable Income
        val taxableIndian = input.indianSalaryIncomeRupees + input.indianRentalIncomeRupees +
                input.indianCapitalGainsRupees + input.nroAccountInterestRupees +
                (if (!isNriOrRnor) input.foreignSourcedIncomeRupees + input.nreAccountInterestRupees + input.fcnrDepositInterestRupees else 0L)

        // 4. NRO Interest TDS: Indian banks deduct 30% + 4% cess = 31.2% u/s 195
        val nroTdsDeducted = (input.nroAccountInterestRupees * 0.312).roundToLong()

        // 5. DTAA Relief u/s 90: If treaty rate is lower than 30% domestic rate on NRO interest
        val domesticTaxOnNro = (input.nroAccountInterestRupees * 0.312).roundToLong()
        val treatyTaxOnNro = (input.nroAccountInterestRupees * (input.dtaaTreatyRatePercent / 100.0) * 1.04).roundToLong()
        val dtaaRelief = if (input.residencyStatus == ResidencyStatus.NON_RESIDENT_INDIAN && input.nroAccountInterestRupees > 0L) {
            max(0L, domesticTaxOnNro - treatyTaxOnNro)
        } else 0L

        // Baseline tax on taxable Indian income (standard slab approximation for AY 2026-27)
        val basicTax = if (taxableIndian <= 300000L) 0L
        else if (taxableIndian <= 700000L) ((taxableIndian - 300000L) * 0.05).roundToLong()
        else if (taxableIndian <= 1000000L) (20000L + (taxableIndian - 700000L) * 0.10).roundToLong()
        else (50000L + (taxableIndian - 1000000L) * 0.15).roundToLong()

        val cess = (basicTax * 0.04).roundToLong()
        val totalTaxBeforeRelief = basicTax + cess
        val finalIndianTax = max(0L, totalTaxBeforeRelief - dtaaRelief)

        // Net refund or balance: TDS deducted minus final tax liability
        val estimatedRefundOrPayable = nroTdsDeducted - finalIndianTax

        val form10F = generateForm10F(pan, input.dtaaCountry, input.dtaaTreatyRatePercent)

        return NriTaxComputationResult(
            residencyStatus = input.residencyStatus,
            grossForeignIncomeExcludedFromIndia = grossForeignExcluded,
            nreInterestExemptUnderSec10_4 = nreExempt,
            fcnrInterestExemptUnderSec10_4 = fcnrExempt,
            taxableIndianIncome = taxableIndian,
            nroTdsDeductedAt30Percent = nroTdsDeducted,
            dtaaReliefClaimedSec90 = dtaaRelief,
            finalIndianTaxLiability = finalIndianTax,
            estimatedRefundOrBalancePayable = estimatedRefundOrPayable,
            form10FMarkdown = form10F
        )
    }

    private fun generateForm10F(pan: String, country: String, treatyRate: Double): String {
        return """
# FORM NO. 10F
*[See sub-rule (1) of rule 21AB of the Income-tax Rules, 1962]*
**Electronic Self-Declaration for Claiming Benefits under Double Taxation Avoidance Agreement (DTAA)**

I, the assessee holding PAN **$pan**, do hereby solemnly declare that:
1. I am a resident of **$country** for the purposes of the Double Taxation Avoidance Agreement entered into between India and $country.
2. I hold a valid Tax Residency Certificate (TRC) issued by the competent tax authority of $country for the relevant financial year.
3. My physical presence in India during FY 2025-26 does not exceed the statutory limits under Section 6 of the Income-tax Act, 1961, and I do not have a Permanent Establishment (PE) in India.
4. I am the beneficial owner of the income arising from Indian financial assets, including interest on Non-Resident Ordinary (NRO) accounts and capital gains.
5. In accordance with Article 11 / 12 of the India-$country DTAA, the beneficial treaty tax withholding rate of **$treatyRate%** is claimed.

**Verification:**  
I solemnly declare that to the best of my knowledge and belief, the information given above is correct, complete, and truly stated.

**Assessee PAN:** $pan  
**Country of Tax Residence:** $country  
**Filing Mode:** CBDT Electronic Form 10F (Rule 21AB)
        """.trimIndent()
    }
}

