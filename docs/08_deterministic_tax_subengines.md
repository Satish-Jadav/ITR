08 — Deterministic Tax Sub-Engines (ITR Auto-Router, HRA Rule 2A & Statutory Interest)
1. Deterministic ITR Form Auto-Router (ITR-1 vs. ITR-4 vs. ITR-2 vs. ITR-3)
Under the Income-tax Rules, 1962, filing on an ineligible form renders a return defective under Section 139(9). The DeterministicItrFormRouter evaluates 14 statutory conditions deterministically in pure Kotlin to route the taxpayer to the simplest legally valid form.

package in.zerotax.core.taxengine.routing

enum class ItrFormType {

    ITR_1_SAHAJ,

    ITR_4_SUGAM,

    ITR_2,

    ITR_3,

    UNSUPPORTED_REQUIRES_CA_AUDIT_44AB

}

enum class ResidentialStatus {

    RESIDENT_ORDINARILY_RESIDENT,      // ROR

    RESIDENT_NOT_ORDINARILY_RESIDENT,  // RNOR

    NON_RESIDENT                       // NR

}

data class TaxpayerRoutingProfile(

    val residentialStatus: ResidentialStatus,

    val totalIncomeRupees: Long,

    val housePropertyCount: Int,

    val hasBroughtForwardOrCarriedForwardLosses: Boolean,

    val hasCapitalGainsOrLosses: Boolean,

    val equity112ALtcgRupees: Long,

    val hasOtherCapitalGains: Boolean,

    val agriculturalIncomeRupees: Long,

    val isCompanyDirector: Boolean,

    val holdsUnlistedEquityShares: Boolean,

    val holdsForeignAssetsOrForeignIncome: Boolean,

    val hasPresumptiveProfession44ADA: Boolean,

    val grossReceipts44ADARupees: Long,

    val cashReceiptsPercentage44ADA: Double,

    val declaredProfitPercentage44ADA: Double,

    val hasRegularBusinessOrIntradaySpeculativeOrFnO: Boolean,

    val requiresStatutoryAudit44AB: Boolean

)

data class FormRoutingDecision(

    val selectedForm: ItrFormType,

    val routingReasonCodes: List<String>,

    val requiresScheduleFA: Boolean,

    val requiresScheduleAL: Boolean // Asset & Liability schedule if Total Income > ₹50,00,000

)

object DeterministicItrFormRouter {

    private const val ITR_1_AND_4_INCOME_CAP = 50_00_000L

    private const val AGRI_INCOME_CAP_ITR1_4 = 5_000L

    private const val SEC_44ADA_STANDARD_LIMIT = 50_00_000L

    private const val SEC_44ADA_ENHANCED_DIGITAL_LIMIT = 75_00_000L

    private const val LTCG_112A_EXEMPT_THRESHOLD = 1_25_000L

    fun routeTaxpayer(profile: TaxpayerRoutingProfile): FormRoutingDecision {

        val reasons = mutableListOf<String>()

        if (profile.requiresStatutoryAudit44AB) {

            reasons.add("SEC_44AB_STATUTORY_AUDIT_REQUIRED")

            return FormRoutingDecision(

                selectedForm = ItrFormType.UNSUPPORTED_REQUIRES_CA_AUDIT_44AB,

                routingReasonCodes = reasons,

                requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome,

                requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP

            )

        }

        if (profile.hasPresumptiveProfession44ADA && profile.declaredProfitPercentage44ADA < 50.0) {

            reasons.add("SEC_44ADA_PROFIT_BELOW_50_PERCENT_REQUIRES_AUDIT")

            return FormRoutingDecision(

                selectedForm = ItrFormType.UNSUPPORTED_REQUIRES_CA_AUDIT_44AB,

                routingReasonCodes = reasons,

                requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome,

                requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP

            )

        }

        val max44AdaTurnover = if (profile.cashReceiptsPercentage44ADA <= 5.0) {

            SEC_44ADA_ENHANCED_DIGITAL_LIMIT

        } else {

            SEC_44ADA_STANDARD_LIMIT

        }

        if (profile.hasRegularBusinessOrIntradaySpeculativeOrFnO ||

            (profile.hasPresumptiveProfession44ADA && profile.grossReceipts44ADARupees > max44AdaTurnover)

        ) {

            reasons.add("BUSINESS_OR_PROFESSION_REQUIRES_ITR3")

            return FormRoutingDecision(

                selectedForm = ItrFormType.ITR_3,

                routingReasonCodes = reasons,

                requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome,

                requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP

            )

        }

        val disqualifiedFromSimpleForms = mutableListOf<String>()

        if (profile.residentialStatus != ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT) {

            disqualifiedFromSimpleForms.add("NON_ROR_RESIDENCY_STATUS")

        }

        if (profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP) {

            disqualifiedFromSimpleForms.add("TOTAL_INCOME_EXCEEDS_50_LAKHS")

        }

        if (profile.housePropertyCount > 1) {

            disqualifiedFromSimpleForms.add("MULTIPLE_HOUSE_PROPERTIES")

        }

        if (profile.holdsForeignAssetsOrForeignIncome) {

            disqualifiedFromSimpleForms.add("HOLDS_FOREIGN_ASSETS_SCHEDULE_FA_MANDATORY")

        }

        if (profile.isCompanyDirector) {

            disqualifiedFromSimpleForms.add("TAXPAYER_IS_COMPANY_DIRECTOR")

        }

        if (profile.holdsUnlistedEquityShares) {

            disqualifiedFromSimpleForms.add("HOLDS_UNLISTED_EQUITY_SHARES")

        }

        if (profile.agriculturalIncomeRupees > AGRI_INCOME_CAP_ITR1_4) {

            disqualifiedFromSimpleForms.add("AGRICULTURAL_INCOME_EXCEEDS_5000")

        }

        if (profile.hasBroughtForwardOrCarriedForwardLosses) {

            disqualifiedFromSimpleForms.add("HAS_BROUGHT_OR_CARRIED_FORWARD_LOSSES")

        }

        if (profile.hasOtherCapitalGains ||

            (profile.hasCapitalGainsOrLosses && profile.equity112ALtcgRupees > LTCG_112A_EXEMPT_THRESHOLD)

        ) {

            disqualifiedFromSimpleForms.add("TAXABLE_CAPITAL_GAINS_REQUIRES_SCHEDULE_CG")

        }

        if (disqualifiedFromSimpleForms.isNotEmpty()) {

            val targetForm = if (profile.hasPresumptiveProfession44ADA) ItrFormType.ITR_3 else ItrFormType.ITR_2

            return FormRoutingDecision(

                selectedForm = targetForm,

                routingReasonCodes = disqualifiedFromSimpleForms,

                requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome &&

                    profile.residentialStatus == ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,

                requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP

            )

        }

        return if (profile.hasPresumptiveProfession44ADA) {

            FormRoutingDecision(

                selectedForm = ItrFormType.ITR_4_SUGAM,

                routingReasonCodes = listOf("PRESUMPTIVE_44ADA_WITHIN_ELIGIBLE_LIMITS"),

                requiresScheduleFA = false,

                requiresScheduleAL = false

            )

        } else {

            FormRoutingDecision(

                selectedForm = ItrFormType.ITR_1_SAHAJ,

                routingReasonCodes = listOf("SALARIED_AND_OTHER_SOURCES_WITHIN_SAHAJ_LIMITS"),

                requiresScheduleFA = false,

                requiresScheduleAL = false

            )

        }

    }

}


2. House Rent Allowance (Section 10(13A) & Rule 2A) & Statutory Interest Engine (Sections 234A, 234B, 234C, 234F)
package in.zerotax.core.taxengine.subengines

import kotlin.math.max

import kotlin.math.min

import kotlin.math.roundToLong

enum class ResidenceCityCategory {

    METRO_50_PERCENT,     // Delhi, Mumbai, Kolkata, Chennai

    NON_METRO_40_PERCENT  // All other cities (including Bengaluru, Hyderabad, Ahmedabad, Pune)

}

data class HraCalculationInput(

    val basicSalaryAnnual: Long,

    val dearnessAllowanceFormingPartOfRetirement: Long,

    val actualHraReceivedAnnual: Long,

    val actualRentPaidAnnual: Long,

    val cityCategory: ResidenceCityCategory,

    val landlordPanProvided: Boolean

)

data class HraCalculationResult(

    val exemptHraRupees: Long,

    val taxableHraRupees: Long,

    val blockedDueToMissingLandlordPan: Boolean,

    val potentialTaxableReductionIfPanAdded: Long

)

data class StatutoryInterestBreakdown(

    val interest234A: Long,

    val interest234B: Long,

    val lateFee234F: Long

)

object HraAndInterestEngine {

    private const val LANDLORD_PAN_MANDATORY_RENT_THRESHOLD = 1_00_000L

    private val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")

    fun computeHraExemption(input: HraCalculationInput, landlordPan: String?): HraCalculationResult {

        val salaryForHra = input.basicSalaryAnnual + input.dearnessAllowanceFormingPartOfRetirement

        val tenPercentSalary = (salaryForHra * 0.10).roundToLong()

        val rentExcessOver10Percent = max(0L, input.actualRentPaidAnnual - tenPercentSalary)

        val cityLimitRate = if (input.cityCategory == ResidenceCityCategory.METRO_50_PERCENT) 0.50 else 0.40

        val cityLimitAmount = (salaryForHra * cityLimitRate).roundToLong()

        val rawExemptHra = min(

            input.actualHraReceivedAnnual,

            min(rentExcessOver10Percent, cityLimitAmount)

        )

        val validLandlordPan = !landlordPan.isNullOrBlank() && PAN_REGEX.matches(landlordPan.trim().uppercase())

        val requiresLandlordPan = input.actualRentPaidAnnual > LANDLORD_PAN_MANDATORY_RENT_THRESHOLD

        return if (requiresLandlordPan && !validLandlordPan) {

            HraCalculationResult(

                exemptHraRupees = 0L,

                taxableHraRupees = input.actualHraReceivedAnnual,

                blockedDueToMissingLandlordPan = rawExemptHra > 0L,

                potentialTaxableReductionIfPanAdded = rawExemptHra

            )

        } else {

            HraCalculationResult(

                exemptHraRupees = rawExemptHra,

                taxableHraRupees = max(0L, input.actualHraReceivedAnnual - rawExemptHra),

                blockedDueToMissingLandlordPan = false,

                potentialTaxableReductionIfPanAdded = 0L

            )

        }

    }

    fun computeStatutoryInterestAndLateFee(

        totalTaxLiabilityAfterCess: Long,

        totalTdsAndTcsCredit: Long,

        advanceTaxPaidByMarch31: Long,

        monthsDelayedAfterDueDate234A: Int,

        monthsFromApril1ToFilingMonth234B: Int,

        netTaxableIncome: Long,

        isSeniorCitizenWithNoBusinessIncome: Boolean

    ): StatutoryInterestBreakdown {

        val assessedTax = max(0L, totalTaxLiabilityAfterCess - totalTdsAndTcsCredit)

        if (assessedTax < 10_000L) {

            val lateFeeOnly = if (monthsDelayedAfterDueDate234A > 0 && netTaxableIncome > 3_00_000L) {

                if (netTaxableIncome <= 5_00_000L) 1_000L else 5_000L

            } else 0L

            return StatutoryInterestBreakdown(0L, 0L, lateFeeOnly)

        }

        val unpaidTaxAfterAdvance = max(0L, assessedTax - advanceTaxPaidByMarch31)

        val roundedPrincipal119A = (unpaidTaxAfterAdvance / 100L) * 100L

        val interest234A = if (monthsDelayedAfterDueDate234A > 0) {

            (roundedPrincipal119A * 0.01 * monthsDelayedAfterDueDate234A).roundToLong()

        } else 0L

        val ninetyPercentAssessed = (assessedTax * 0.90).roundToLong()

        val interest234B = if (!isSeniorCitizenWithNoBusinessIncome && advanceTaxPaidByMarch31 < ninetyPercentAssessed) {

            (roundedPrincipal119A * 0.01 * max(1, monthsFromApril1ToFilingMonth234B)).roundToLong()

        } else 0L

        val fee234F = if (monthsDelayedAfterDueDate234A > 0) {

            if (netTaxableIncome <= 5_00_000L) 1_000L else 5_000L

        } else 0L

        return StatutoryInterestBreakdown(

            interest234A = interest234A,

            interest234B = interest234B,

            lateFee234F = fee234F

        )

    }

}


3. Multi-Property Schedule HP, Section 234C Quarterly Advance Tax & 14-Gate Routing Matrix
The following 14 statutory conditions are evaluated deterministically to establish eligibility across ITR forms (ITR-1 Sahaj, ITR-4 Sugam, ITR-2, and ITR-3) under Section 139(9):
Gate 1 — Section 44AB Statutory Tax Audit Requirement: Disqualifies the taxpayer from self-filing utility forms (UNSUPPORTED_REQUIRES_CA_AUDIT_44AB) when turnover/receipts or profit declarations mandate a Chartered Accountant audit report in Form 3CA/3CB-3CD.
Gate 2 — Section 44ADA(4) Sub-50% Presumptive Profit Floor: Where a specified professional declares net profit below 50% of gross receipts and total income exceeds the basic exemption limit, Section 44ADA(4) mandates books of account under Section 44AA and tax audit under Section 44AB.
Gate 3 — Section 44ADA Gross Receipts Ceiling (₹50L Standard / ₹75L Enhanced Digital): Verifies whether professional receipts exceed ₹50,00,000 (or ₹75,00,000 where aggregate cash receipts do not exceed 5% of total gross receipts under the proviso to Section 44ADA(1)), routing excess turnover to ITR-3.
Gate 4 — Regular Business, Intraday Speculative (Sec 43(5)), or Derivatives (F&O) Income: Disqualifies the taxpayer from ITR-1, ITR-2, and ITR-4, routing deterministically to ITR-3.
Gate 5 — Residential Status Gate (Section 6): Under Rule 12(1)(a)/(ca), only a Resident and Ordinarily Resident (ROR) individual is eligible for ITR-1 (Sahaj) or ITR-4 (Sugam); RNOR and NR taxpayers are routed to ITR-2.
Gate 6 — Total Income Threshold (> ₹50,00,000): Disqualifies the taxpayer from ITR-1 and ITR-4 under Rule 12 and simultaneously activates mandatory Schedule AL (Assets and Liabilities at year-end).
Gate 7 — Multiple House Properties (Schedule HP): Ownership of more than one house property (whether Let-Out or Deemed Let-Out under Section 23(4)) bars ITR-1/ITR-4 and routes to ITR-2.
Gate 8 — Foreign Assets or Foreign Income (Schedule FA / Schedule FSI): Holding any foreign asset (including vested US RSUs/ESPPs or foreign custodial accounts) or earning foreign-sourced income bars ITR-1/ITR-4 and mandates ITR-2 with Schedule FA for ROR assessees (Sections 42 & 43 of the Black Money Act, 2015).
Gate 9 — Company Directorship Disqualification: Holding a DIN as a director in any domestic or foreign company at any time during the previous year bars ITR-1/ITR-4 and routes to ITR-2.
Gate 10 — Unlisted Equity Shares Disqualification: Holding unlisted equity shares at any time during the financial year bars ITR-1/ITR-4 and mandates ITR-2 (Part A-General shareholding table).
Gate 11 — Agricultural Income Cap (> ₹5,000): Agricultural income exceeding ₹5,000 requires Schedule EI and partial integration of agricultural income in ITR-2.
Gate 12 — Brought-Forward or Carried-Forward Losses (Schedule BFLA / Schedule CFL): Any loss brought forward from prior assessment years or sought to be carried forward under Sections 71B or 74 disqualifies ITR-1/ITR-4 and routes to ITR-2.
Gate 13 — Taxable Capital Gains (Schedule CG / Schedule 112A > ₹1,25,000): Short-term capital gains (Sec 111A/50AA), immovable property LTCG (Sec 112), or long-term equity gains (Sec 112A) exceeding the ₹1,25,000 tax-free threshold require Schedule CG in ITR-2.
Gate 14 — Final Sahaj vs. Sugam Bifurcation: When none of Gates 1–13 disqualify the assessee, presence of eligible Section 44ADA income routes to ITR-4 (Sugam); pure salary/pension/one-house-property/other-sources routes to ITR-1 (Sahaj).

The following Kotlin function calculates statutory interest under Section 234C across four quarterly installments with safe harbor relief and windfall capital gains adjustments:

package in.zerotax.core.taxengine.subengines

import kotlin.math.max
import kotlin.math.roundToLong

data class AdvanceTaxQuarterlyPayments(
    val paidByJune15: Long,
    val paidBySeptember15: Long,
    val paidByDecember15: Long,
    val paidByMarch15: Long
)

data class CapitalGainsWindfallDates(
    val capitalGainsTaxAfterJune15: Long = 0L,
    val capitalGainsTaxAfterSept15: Long = 0L,
    val capitalGainsTaxAfterDec15: Long = 0L
)

data class Section234CResult(
    val q1InterestJune15: Long,
    val q2InterestSept15: Long,
    val q3InterestDec15: Long,
    val q4InterestMarch15: Long,
    val totalInterest234C: Long
)

fun computeSection234CQuarterlyInstallmentInterest(
    assessedTax: Long,
    payments: AdvanceTaxQuarterlyPayments,
    isPresumptive44ADA: Boolean,
    windfalls: CapitalGainsWindfallDates = CapitalGainsWindfallDates()
): Section234CResult {
    if (assessedTax < 10_000L) {
        return Section234CResult(0L, 0L, 0L, 0L, 0L)
    }

    if (isPresumptive44ADA) {
        val shortfallQ4 = max(0L, assessedTax - payments.paidByMarch15)
        val roundedShortfall = (shortfallQ4 / 100L) * 100L
        val interest = (roundedShortfall * 0.01).roundToLong()
        return Section234CResult(0L, 0L, 0L, interest, interest)
    }

    val baseAssessedTaxQ1 = max(0L, assessedTax - windfalls.capitalGainsTaxAfterJune15)
    val q1SafeHarborThreshold = (baseAssessedTaxQ1 * 0.12).roundToLong()
    val q1Target15 = (baseAssessedTaxQ1 * 0.15).roundToLong()
    val q1Interest = if (payments.paidByJune15 < q1SafeHarborThreshold) {
        val shortfall = (max(0L, q1Target15 - payments.paidByJune15) / 100L) * 100L
        (shortfall * 0.01 * 3).roundToLong()
    } else 0L

    val baseAssessedTaxQ2 = max(0L, assessedTax - windfalls.capitalGainsTaxAfterSept15)
    val q2SafeHarborThreshold = (baseAssessedTaxQ2 * 0.36).roundToLong()
    val q2Target45 = (baseAssessedTaxQ2 * 0.45).roundToLong()
    val q2Interest = if (payments.paidBySeptember15 < q2SafeHarborThreshold) {
        val shortfall = (max(0L, q2Target45 - payments.paidBySeptember15) / 100L) * 100L
        (shortfall * 0.01 * 3).roundToLong()
    } else 0L

    val baseAssessedTaxQ3 = max(0L, assessedTax - windfalls.capitalGainsTaxAfterDec15)
    val q3Target75 = (baseAssessedTaxQ3 * 0.75).roundToLong()
    val q3Interest = if (payments.paidByDecember15 < q3Target75) {
        val shortfall = (max(0L, q3Target75 - payments.paidByDecember15) / 100L) * 100L
        (shortfall * 0.01 * 3).roundToLong()
    } else 0L

    val q4Target100 = assessedTax
    val q4Interest = if (payments.paidByMarch15 < q4Target100) {
        val shortfall = (max(0L, q4Target100 - payments.paidByMarch15) / 100L) * 100L
        (shortfall * 0.01 * 1).roundToLong()
    } else 0L

    val total = q1Interest + q2Interest + q3Interest + q4Interest
    return Section234CResult(q1Interest, q2Interest, q3Interest, q4Interest, total)
}

The sub-engine implementation below provides statutory computations for multi-property Schedule HP and presumptive Section 44ADA:

package in.zerotax.core.taxengine.subengines

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

enum class PropertyType {
    SELF_OCCUPIED,
    LET_OUT,
    DEEMED_LET_OUT
}

data class HousePropertyInput(
    val propertyType: PropertyType,
    val grossRentReceived: Long = 0L,
    val municipalTaxesPaid: Long = 0L,
    val homeLoanInterest24B: Long = 0L
)

data class HousePropertyResult(
    val netAnnualValue: Long,
    val standardDeduction24A: Long,
    val allowableInterest24B: Long,
    val netIncomeOrLoss: Long
)

data class ScheduleHpAggregateResult(
    val individualProperties: List<HousePropertyResult>,
    val aggregateIncomeOrLoss: Long,
    val lossAllowedForInterHeadSetOff71_3A: Long,
    val lossCarriedForwardScheduleCFL: Long
)

data class Presumptive44AdaResult(
    val grossReceipts: Long,
    val declaredProfitPercentage: Double,
    val presumptiveProfit: Long,
    val isValidPresumptiveDeclaration: Boolean
)

object ScheduleHpAnd44AdaSubEngine {
    private const val MAX_SELF_OCCUPIED_INTEREST_24B = 2_00_000L
    private const val MAX_INTER_HEAD_LOSS_SETOFF_71_3A = 2_00_000L
    private const val MIN_44ADA_PROFIT_RATIO = 0.50

    fun computeScheduleHpMultiProperty(properties: List<HousePropertyInput>): ScheduleHpAggregateResult {
        var totalSelfOccupiedInterest = 0L
        val propertyResults = mutableListOf<HousePropertyResult>()

        for (prop in properties) {
            if (prop.propertyType == PropertyType.SELF_OCCUPIED) {
                val remainingCap = max(0L, MAX_SELF_OCCUPIED_INTEREST_24B - totalSelfOccupiedInterest)
                val allowedInterest = min(prop.homeLoanInterest24B, remainingCap)
                totalSelfOccupiedInterest += allowedInterest
                val netIncome = -allowedInterest
                propertyResults.add(
                    HousePropertyResult(
                        netAnnualValue = 0L,
                        standardDeduction24A = 0L,
                        allowableInterest24B = allowedInterest,
                        netIncomeOrLoss = netIncome
                    )
                )
            } else {
                val nav = max(0L, prop.grossRentReceived - prop.municipalTaxesPaid)
                val stdDeduction = (nav * 0.30).roundToLong()
                val allowableInterest = prop.homeLoanInterest24B
                val netIncome = nav - stdDeduction - allowableInterest
                propertyResults.add(
                    HousePropertyResult(
                        netAnnualValue = nav,
                        standardDeduction24A = stdDeduction,
                        allowableInterest24B = allowableInterest,
                        netIncomeOrLoss = netIncome
                    )
                )
            }
        }

        val aggregateSum = propertyResults.sumOf { it.netIncomeOrLoss }
        val setOffLoss = if (aggregateSum < 0L) {
            min(-aggregateSum, MAX_INTER_HEAD_LOSS_SETOFF_71_3A)
        } else 0L

        val carriedForwardLoss = if (aggregateSum < 0L) {
            max(0L, -aggregateSum - MAX_INTER_HEAD_LOSS_SETOFF_71_3A)
        } else 0L

        return ScheduleHpAggregateResult(
            individualProperties = propertyResults,
            aggregateIncomeOrLoss = aggregateSum,
            lossAllowedForInterHeadSetOff71_3A = setOffLoss,
            lossCarriedForwardScheduleCFL = carriedForwardLoss
        )
    }

    fun computePresumptive44Ada(grossReceipts: Long, declaredProfitRatio: Double): Presumptive44AdaResult {
        val isValid = declaredProfitRatio >= MIN_44ADA_PROFIT_RATIO
        val profitRatioToApply = max(MIN_44ADA_PROFIT_RATIO, declaredProfitRatio)
        val presumptiveProfit = (grossReceipts * profitRatioToApply).roundToLong()
        return Presumptive44AdaResult(
            grossReceipts = grossReceipts,
            declaredProfitPercentage = declaredProfitRatio * 100.0,
            presumptiveProfit = presumptiveProfit,
            isValidPresumptiveDeclaration = isValid
        )
    }
}


4. CA Statutory Commentary on Rule 2A HRA, Rule 119A Rounding & Sections 234A/234B/234C/234F
Rule 2A Metro vs. Non-Metro Statutory Classification (Section 10(13A)):
Under the Explanation to Rule 2A of the Income-tax Rules, 1962, the 50% of (Basic + DA) ceiling applies strictly to residential accommodation situated at Mumbai, Kolkata, Delhi, or Chennai. All other cities—including Bengaluru, Hyderabad, Ahmedabad, Pune, and Gurugram—are statutorily governed by the 40% ceiling. Furthermore, pursuant to CBDT Circular No. 8/2013, where aggregate annual rent paid exceeds ₹1,00,000, HraAndInterestEngine enforces a hard gate requiring a valid 10-character Landlord PAN (^[A-Z]{5}[0-9]{4}[A-Z]{1}$) before granting any Section 10(13A) exemption.
Rule 119A(b) Principal Rounding & Section 207(2) Senior Citizen Immunity:
Under Rule 119A(b), the amount of tax, penalty, or other sum in respect of which interest is to be calculated under Sections 234A, 234B, or 234C is first rounded off by ignoring any fraction of a hundred rupees ((unpaidTax / 100L) * 100L).
Under Section 207(2), a Resident individual aged 60 years or more at any time during the previous year who does not have any income chargeable under the head "Profits and gains of business or profession" is statutorily exempt from paying Advance Tax; consequently, interest234B is deterministically forced to ₹0.
Section 234C Safe-Harbor Tolerances (12% Q1 / 36% Q2), Windfall Carve-Out & Section 44ADA Single-Installment Rule:
Under the First Proviso to Section 234C(1), no quarterly installment interest is levied for Q1 (June 15, target 15%) if advance tax paid is at least 12% of assessed tax, or for Q2 (September 15, target 45%) if advance tax paid is at least 36% of assessed tax.
Under the Proviso to Section 234C(1) for Capital Gains, any shortfall in Q1, Q2, or Q3 installment caused by capital gains arising after the respective installment due date (capitalGainsTaxAfterJune15, capitalGainsTaxAfterSept15, capitalGainsTaxAfterDec15) is excluded from that quarter's shortfall base.
For presumptive freelancers under Section 44ADA, Section 234C(1)(b) requires only a single 100% installment on or before March 15, with ₹0 interest for Q1, Q2, and Q3.


5. Sub-Engine Statutory Boundary Matrix & Defect Code Prevention Ledger

Sub-Engine Component
Statutory Provision
Defect / Penalty Prevented
Deterministic Guardrail
DeterministicItrFormRouter
Section 139(9) & Rule 12
Defective Return Notice (wrong ITR form selection)
Evaluates 14 statutory gates in strict precedence (UNSUPPORTED_44AB -> ITR-3 -> ITR-2 -> ITR-4 -> ITR-1).
HraAndInterestEngine (HRA)
Section 10(13A), Rule 2A & Circular 8/2013
Section 270A(9) 200% misreporting penalty on unverified rent
Enforces 50% (4 metros) vs 40% (non-metro) limit and blocks HRA exemption if annual rent > ₹1,00,000 without a valid 10-char Landlord PAN.
HraAndInterestEngine (Interest)
Sections 234A, 234B, 234C, 234F, 207(2) & Rule 119A(b)
Section 143(1) short-interest demand intimation
Truncates unpaid principal to nearest ₹100 ((unpaidTax / 100L) * 100L), enforces ₹10,000 assessedTax floor, and grants Senior Citizens (Age >= 60, no business income) complete Section 234B immunity.
ScheduleHpAnd44AdaSubEngine
Sections 24(a), 24(b), 71(3A), 71B & 44ADA
Excess HP loss claim or sub-50% presumptive filing without audit
Caps self-occupied interest and inter-head HP loss set-off at ₹2,00,000 (carrying forward excess for 8 AYs) and enforces the 50% presumptive profit floor.



6. Deterministic CTC Salary Restructuring & Flexi-Benefits Optimizer (`SalaryRestructuringOptimizer`)
Under the Income-tax Act, 1961 and amendments introduced under the Finance Act, 2024:
- Section 80CCD(2): Employer contribution to the National Pension System (NPS) up to 14% of (Basic Salary + Dearness Allowance) is allowable as an above-the-line deduction in BOTH the Old and New Tax Regimes (AY 2026-27). This deduction is outside the Section 80C ₹1.5L cap and Section 80CCD(1B) ₹50,000 cap.
- Rule 3(7)(ix): Telephony and high-speed broadband reimbursements incurred for official purposes are 100% tax-free against valid invoices.
- Section 10(14)(i): Allowances or reimbursements for professional literature, books, journals, and certifications are exempt.
- Rule 3(7)(iii): Meal vouchers / Sodexo cards are tax-exempt up to ₹50 per meal (~₹26,400/year) under the Old Tax Regime.
The `SalaryRestructuringOptimizer` models these statutory provisions to compute tax-optimal CTC structures and auto-generates ready-to-submit HR proposal letters.


7. Multi-Broker Capital Gains & 8-Year Loss Carry-Forward Ledger (`CapitalGainsAndLossLedgerEngine`)
Statutory framework under Finance (No. 2) Act, 2024 (effective July 23, 2024) and Assessment Year 2026-27:
- Section 111A: Short-Term Capital Gains on listed equities/equity-oriented mutual funds subject to STT are taxed at 20.0%.
- Section 112A: Long-Term Capital Gains on listed equities/equity funds subject to STT are taxed at 12.5% for gains exceeding the ₹1,25,000 statutory exemption threshold.
- Section 70 & Section 71 Set-Off Mechanics:
  - Current Year STCL can be set off against BOTH current year STCG and LTCG.
  - Current Year LTCL can ONLY be set off against current year LTCG (cannot be set off against STCG or other income heads).
- Schedule CFL & CYLA (Section 74):
  - Unabsorbed STCL and LTCL can be carried forward for up to 8 Assessment Years from the assessment year in which they were first computed.
  - Section 80 / 139(3) Mandate: Loss carry-forward rights are strictly conditioned on filing the original ITR on or before the Section 139(1) due date (July 31). Returns filed belatedly under Section 139(4) forfeit the right to carry forward capital losses.


8. Small Business Tax Audit & Presumptive Compliance Engine (`SmallBusinessAuditAndPresumptiveEngine`)
Statutory architecture under Income-tax Act, 1961 for Small Businesses, Contractors & Specified Professionals:
- Section 44AB Statutory Tax Audit Mandate:
  - Clause (a): General business turnover exceeding ₹1 Crore.
  - Proviso to Clause (a): Enhanced ₹10 Crore threshold applicable strictly where aggregate cash receipts do not exceed 5% of total turnover AND aggregate cash payments do not exceed 5% of total payments. If cash transactions exceed 5%, the audit trigger threshold automatically resets to ₹1 Crore.
  - Clause (b): Gross receipts from specified profession exceeding ₹50 Lakhs (or ₹75 Lakhs under Section 44ADA if cash receipts <= 5%).
  - Clause (d): Professional declaring profit below 50% statutory deemed rate with total income exceeding the basic exemption limit (₹3,00,000 u/s 115BAC).
  - Clause (e): Business declaring profit below 6% (digital) or 8% (cash) deemed rates, or triggering the Section 44AD(4) 5-year lock-in debarment rule.
- Section 271B Penal Protection:
  - Penalty for failure to get accounts audited and furnish report u/s 44AB on or before September 30 is 0.5% of turnover/gross receipts, capped at ₹1,50,000.
- Form 3CD Key Disallowance Diagnostic Scanner:
  - Clause 22 / Section 43B(h): Strict disallowance of overdue sums payable to Micro and Small Enterprises beyond the 15-day / 45-day statutory limit under Section 15 of the MSMED Act, 2006.
  - Clause 21(d) / Section 40A(3): 100% disallowance of expenditure paid in cash exceeding ₹10,000 in a single day to a single person.
  - Clause 21(a) / Section 40(a)(ia): 30% disallowance on contractor, commission, or professional fees where TDS was not deducted or deposited before the Section 139(1) due date.
  - Clause 26 / Section 43B: Disallowance of unpaid statutory liabilities (GST, EPF, ESIC, bonus) not cleared before the ITR filing due date.
- CA Handover Package: Auto-compiles the pre-audit working trial balance, disallowance schedule, and Form 3CB-3CD draft pack for immediate submission to the Chartered Accountant.


9. Dual-Region Residency & Non-Resident Indian Engine (`NonResidentTaxAndResidencyEngine`)
Statutory architecture under Income-tax Act, 1961 for Residents and NRIs:
- Section 6(1) & 6(6) Physical Presence Determination:
  - Basic 182-day rule and 60-day + 365-day rule.
  - Indian citizen / PIO visiting India: 120-day threshold if Indian income > ₹15 Lakhs (Finance Act 2020), else 182-day threshold.
  - Section 6(1A) Deemed Resident rule for stateless citizens with Indian income > ₹15 Lakhs (treated as RNOR).
  - Classifies taxpayer into Resident and Ordinarily Resident (ROR), Resident but Not Ordinarily Resident (RNOR), or Non-Resident Indian (NRI).
- Statutory Exclusions for NRIs:
  - Global / foreign sourced income is 100% exempt from Indian tax.
  - Section 10(4)(ii): Interest on Non-Resident External (NRE) savings/fixed deposits and FCNR deposits is 100% tax-free.
  - Non-Resident Ordinary (NRO) accounts: Reconciles 30% (+ cess) TDS deducted under Section 195 against actual slab rates.
  - Section 90 / 90A DTAA Relief & Electronic Form 10F Generator: Computes beneficial treaty tax withholding and generates electronic Form 10F self-declarations.
  - NRI Ineligibility Protections: Bars NRIs from opting for Section 44AD presumptive taxation (restricted to Resident Individuals/Partnerships under Section 44AD(1)).

10. Dual-Regime & Form 10-IEA Business Governance Engine (`Form10IeaBusinessRegimeEngine`)
Statutory framework under Section 115BAC(3) and Rule 21AGA (Notification 43/2023):
- Salaried taxpayers (ITR-1 / ITR-2) exercise regime choice annually on the ITR utility without separate filings.
- Business & Professional Taxpayers (ITR-3 / ITR-4): The New Regime under Section 115BAC(1A) is the statutory default. Opting out to the Old Regime requires mandatory electronic filing of Form 10-IEA on or before the Section 139(1) due date (July 31 for non-audit, October 31 for tax audit).
- 1-Time Lifetime Lock-In Rule: Business taxpayers opting out to Old Regime are granted strictly ONE lifetime opportunity to re-enter the New Regime. Once re-entered, they are permanently debarred from choosing the Old Regime while having business income.
- Compiles CBDT schema-compliant Form 10-IEA JSON payloads with acknowledgement number mapping for Schedule 115BAC.
