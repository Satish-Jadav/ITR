package `in`.zerotax.core.taxengine.advanced

import `in`.zerotax.core.model.ResidenceCityCategory
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class HraComputationInput(
    val actualHraReceived: Long,
    val rentPaidTotal: Long,
    val salaryBasicPlusDa: Long,
    val isMetroCity: Boolean
)

data class HraCalculationInput(
    val basicSalaryAnnual: Long,
    val dearnessAllowanceFormingPartOfRetirement: Long = 0L,
    val actualHraReceivedAnnual: Long,
    val actualRentPaidAnnual: Long,
    val cityCategory: ResidenceCityCategory = ResidenceCityCategory.NON_METRO_40_PERCENT,
    val landlordPanProvided: Boolean = false
)

data class HraExemptionResult(
    val exemptHraRupees: Long,
    val blockedDueToMissingLandlordPan: Boolean,
    val potentialTaxableReductionIfPanAdded: Long
)

data class HraCalculationResult(
    val eligibleExemptionRupees: Long,
    val actualHraReceived: Long,
    val rentExcessOverTenPercent: Long,
    val salaryCapAmount: Long,
    val isBlockedDueToMissingLandlordPan: Boolean = false,
    val taxSavingsLostDueToMissingPan: Long = 0L
)

data class Interest234Breakdown(
    val interest234A: Long,
    val interest234B: Long,
    val interest234C: Long,
    val fee234F: Long = 0L,
    val totalInterest: Long
)

data class Section234CInstallmentBreakdown(
    val q1Interest: Long = 0L,
    val q2Interest: Long = 0L,
    val q3Interest: Long = 0L,
    val q4Interest: Long = 0L,
    val totalInterest234C: Long = 0L
)

object HraAndInterestEngine {
    private val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")

    fun computeHraExemptionRule2A(input: HraComputationInput): Long {
        if (input.rentPaidTotal <= 0L || input.salaryBasicPlusDa <= 0L) return 0L
        val tenPercentSalary = (input.salaryBasicPlusDa * 0.10).toLong()
        val rentOverTenPercent = max(0L, input.rentPaidTotal - tenPercentSalary)
        val salaryPercentageCap = if (input.isMetroCity) {
            (input.salaryBasicPlusDa * 0.50).toLong()
        } else {
            (input.salaryBasicPlusDa * 0.40).toLong()
        }
        return min(input.actualHraReceived, min(rentOverTenPercent, salaryPercentageCap))
    }

    fun computeHraExemption(
        input: HraComputationInput,
        landlordPan: String? = null
    ): HraCalculationResult {
        val baseExemption = computeHraExemptionRule2A(input)
        val isRentAbove1Lakh = input.rentPaidTotal > 1_00_000L
        val isValidPan = !landlordPan.isNullOrBlank() && PAN_REGEX.matches(landlordPan.trim().uppercase())

        val isBlocked = isRentAbove1Lakh && !isValidPan
        val allowedExemption = if (isBlocked) 0L else baseExemption
        val lostSavings = if (isBlocked) (baseExemption * 0.312).roundToLong() else 0L

        val tenPercentSalary = (input.salaryBasicPlusDa * 0.10).toLong()
        val rentOverTenPercent = max(0L, input.rentPaidTotal - tenPercentSalary)
        val salaryCap = if (input.isMetroCity) (input.salaryBasicPlusDa * 0.50).toLong() else (input.salaryBasicPlusDa * 0.40).toLong()

        return HraCalculationResult(
            eligibleExemptionRupees = allowedExemption,
            actualHraReceived = input.actualHraReceived,
            rentExcessOverTenPercent = rentOverTenPercent,
            salaryCapAmount = salaryCap,
            isBlockedDueToMissingLandlordPan = isBlocked,
            taxSavingsLostDueToMissingPan = lostSavings
        )
    }

    fun computeHraExemption(
        input: HraCalculationInput,
        landlordPan: String? = null
    ): HraExemptionResult {
        val computationInput = HraComputationInput(
            actualHraReceived = input.actualHraReceivedAnnual,
            rentPaidTotal = input.actualRentPaidAnnual,
            salaryBasicPlusDa = input.basicSalaryAnnual + input.dearnessAllowanceFormingPartOfRetirement,
            isMetroCity = input.cityCategory == ResidenceCityCategory.METRO_50_PERCENT
        )
        val baseExemption = computeHraExemptionRule2A(computationInput)
        val isRentAbove1Lakh = input.actualRentPaidAnnual > 1_00_000L
        val effectivePan = landlordPan ?: if (input.landlordPanProvided) "DUMMYPAN1A" else null
        val isValidPan = !effectivePan.isNullOrBlank() && (PAN_REGEX.matches(effectivePan.trim().uppercase()) || effectivePan == "DUMMYPAN1A")

        val isBlocked = isRentAbove1Lakh && !isValidPan
        val allowedExemption = if (isBlocked) 0L else baseExemption

        return HraExemptionResult(
            exemptHraRupees = allowedExemption,
            blockedDueToMissingLandlordPan = isBlocked,
            potentialTaxableReductionIfPanAdded = if (isBlocked) baseExemption else 0L
        )
    }

    fun computeStatutoryInterestAndLateFee(
        totalTaxLiabilityAfterCess: Long,
        totalTdsAndTcsCredit: Long,
        advanceTaxPaidByMarch31: Long,
        monthsDelayedAfterDueDate234A: Int,
        monthsFromApril1ToFilingMonth234B: Int,
        netTaxableIncome: Long,
        isSeniorCitizenWithNoBusinessIncome: Boolean = false
    ): Interest234Breakdown {
        val assessedTax = max(0L, totalTaxLiabilityAfterCess - totalTdsAndTcsCredit)

        // Section 208 Immunity Floor: If assessed tax < ₹10,000, no advance tax or 234B/C interest
        if (assessedTax < 10_000L) {
            val lateFee = computeLateFee234F(monthsDelayedAfterDueDate234A, netTaxableIncome)
            return Interest234Breakdown(0L, 0L, 0L, lateFee, lateFee)
        }

        // Section 207(2) Immunity: Resident Senior Citizen with no business income is exempt from Sec 234B
        val int234B = if (isSeniorCitizenWithNoBusinessIncome) {
            0L
        } else {
            val advanceTaxThreshold = (assessedTax * 0.90).roundToLong()
            if (advanceTaxPaidByMarch31 < advanceTaxThreshold) {
                val shortfall = max(0L, assessedTax - advanceTaxPaidByMarch31)
                val truncatedPrincipal = (shortfall / 100L) * 100L // Rule 119A(b) ₹100 truncation
                (truncatedPrincipal * 0.01 * monthsFromApril1ToFilingMonth234B).roundToLong()
            } else 0L
        }

        val shortfall234A = max(0L, assessedTax - advanceTaxPaidByMarch31)
        val truncatedPrincipal234A = (shortfall234A / 100L) * 100L
        val int234A = (truncatedPrincipal234A * 0.01 * monthsDelayedAfterDueDate234A).roundToLong()

        val lateFee = computeLateFee234F(monthsDelayedAfterDueDate234A, netTaxableIncome)

        return Interest234Breakdown(
            interest234A = int234A,
            interest234B = int234B,
            interest234C = 0L,
            fee234F = lateFee,
            totalInterest = int234A + int234B + lateFee
        )
    }

    fun computeSection234CQuarterlyInstallmentInterest(
        netAssessedTax: Long,
        advanceTaxPaidQ1UptoJune15: Long,
        advanceTaxPaidQ2UptoSep15: Long,
        advanceTaxPaidQ3UptoDec15: Long,
        advanceTaxPaidQ4UptoMar15: Long,
        isPresumptive44ADOr44ADA: Boolean = false,
        isSeniorCitizenNoBusiness: Boolean = false
    ): Section234CInstallmentBreakdown {
        if (netAssessedTax < 10_000L || isSeniorCitizenNoBusiness) {
            return Section234CInstallmentBreakdown()
        }

        if (isPresumptive44ADOr44ADA) {
            // Presumptive business/profession: Single installment on or before March 15 (100% of tax)
            val cumulativePaid = advanceTaxPaidQ1UptoJune15 + advanceTaxPaidQ2UptoSep15 +
                    advanceTaxPaidQ3UptoDec15 + advanceTaxPaidQ4UptoMar15
            val shortfall = max(0L, netAssessedTax - cumulativePaid)
            val truncated = (shortfall / 100L) * 100L
            val q4Interest = (truncated * 0.01).roundToLong()
            return Section234CInstallmentBreakdown(0L, 0L, 0L, q4Interest, q4Interest)
        }

        val q1Paid = advanceTaxPaidQ1UptoJune15
        val q2Paid = q1Paid + advanceTaxPaidQ2UptoSep15
        val q3Paid = q2Paid + advanceTaxPaidQ3UptoDec15
        val q4Paid = q3Paid + advanceTaxPaidQ4UptoMar15

        // Q1: Target 15%, Safe-Harbor 12% (1% x 3 months = 3%)
        val q1SafeHarbor = (netAssessedTax * 0.12).roundToLong()
        val q1Target = (netAssessedTax * 0.15).roundToLong()
        val q1Shortfall = if (q1Paid < q1SafeHarbor) max(0L, q1Target - q1Paid) else 0L
        val q1Int = (((q1Shortfall / 100L) * 100L) * 0.03).roundToLong()

        // Q2: Target 45%, Safe-Harbor 36% (1% x 3 months = 3%)
        val q2SafeHarbor = (netAssessedTax * 0.36).roundToLong()
        val q2Target = (netAssessedTax * 0.45).roundToLong()
        val q2Shortfall = if (q2Paid < q2SafeHarbor) max(0L, q2Target - q2Paid) else 0L
        val q2Int = (((q2Shortfall / 100L) * 100L) * 0.03).roundToLong()

        // Q3: Target 75% (1% x 3 months = 3%)
        val q3Target = (netAssessedTax * 0.75).roundToLong()
        val q3Shortfall = if (q3Paid < q3Target) max(0L, q3Target - q3Paid) else 0L
        val q3Int = (((q3Shortfall / 100L) * 100L) * 0.03).roundToLong()

        // Q4: Target 100% (1% x 1 month = 1%)
        val q4Target = netAssessedTax
        val q4Shortfall = if (q4Paid < q4Target) max(0L, q4Target - q4Paid) else 0L
        val q4Int = (((q4Shortfall / 100L) * 100L) * 0.01).roundToLong()

        return Section234CInstallmentBreakdown(
            q1Interest = q1Int,
            q2Interest = q2Int,
            q3Interest = q3Int,
            q4Interest = q4Int,
            totalInterest234C = q1Int + q2Int + q3Int + q4Int
        )
    }

    private fun computeLateFee234F(monthsDelayed: Int, netTaxableIncome: Long): Long {
        if (monthsDelayed <= 0) return 0L
        return if (netTaxableIncome <= 5_00_000L) 1_000L else 5_000L
    }

    fun computeMandatoryInterests(
        assessedTaxLiability: Long,
        advanceTaxPaidByQuarter: List<Long>,
        tdsTcsCredit: Long,
        filingDelayMonthsSec234A: Int
    ): Interest234Breakdown {
        val netAssessedTax = max(0L, assessedTaxLiability - tdsTcsCredit)
        val totalAdvancePaid = advanceTaxPaidByQuarter.sum()

        val shortfall234A = max(0L, netAssessedTax - totalAdvancePaid)
        val int234A = (shortfall234A * 0.01 * filingDelayMonthsSec234A).toLong()

        val int234B = if (totalAdvancePaid < (netAssessedTax * 0.90).toLong()) {
            val shortfall234B = max(0L, netAssessedTax - totalAdvancePaid)
            (shortfall234B * 0.01 * 12).toLong()
        } else 0L

        val q1 = advanceTaxPaidByQuarter.getOrElse(0) { 0L }
        val q2 = advanceTaxPaidByQuarter.getOrElse(1) { 0L }
        val q3 = advanceTaxPaidByQuarter.getOrElse(2) { 0L }
        val q4 = advanceTaxPaidByQuarter.getOrElse(3) { 0L }

        val cResult = computeSection234CQuarterlyInstallmentInterest(netAssessedTax, q1, q2, q3, q4)

        return Interest234Breakdown(
            interest234A = int234A,
            interest234B = int234B,
            interest234C = cResult.totalInterest234C,
            fee234F = 0L,
            totalInterest = int234A + int234B + cResult.totalInterest234C
        )
    }
}
