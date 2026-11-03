package in.zerotax.core.taxengine.advanced

import kotlin.math.max
import kotlin.math.min

data class HraComputationInput(
    val actualHraReceived: Long,
    val rentPaidTotal: Long,
    val salaryBasicPlusDa: Long,
    val isMetroCity: Boolean
)

data class Interest234Breakdown(
    val interest234A: Long,
    val interest234B: Long,
    val interest234C: Long,
    val totalInterest: Long
)

object HraAndInterestEngine {
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

    fun computeMandatoryInterests(
        assessedTaxLiability: Long,
        advanceTaxPaidByQuarter: List<Long>,
        tdsTcsCredit: Long,
        filingDelayMonthsSec234A: Int
    ): Interest234Breakdown {
        val netAssessedTax = max(0L, assessedTaxLiability - tdsTcsCredit)
        val totalAdvancePaid = advanceTaxPaidByQuarter.sum()

        // Sec 234A: 1% per month or part of month on shortfall
        val shortfall234A = max(0L, netAssessedTax - totalAdvancePaid)
        val int234A = (shortfall234A * 0.01 * filingDelayMonthsSec234A).toLong()

        // Sec 234B: 1% per month from April 1 to assessment date if advance tax < 90% of assessed tax
        val int234B = if (totalAdvancePaid < (netAssessedTax * 0.90).toLong()) {
            val shortfall234B = max(0L, netAssessedTax - totalAdvancePaid)
            (shortfall234B * 0.01 * 12).toLong() // Normalized full AY deferral
        } else 0L

        // Sec 234C: Cumulative quarterly milestones (15%, 45%, 75%, 100%)
        val q1Paid = advanceTaxPaidByQuarter.getOrElse(0) { 0L }
        val q2Paid = q1Paid + advanceTaxPaidByQuarter.getOrElse(1) { 0L }
        val q3Paid = q2Paid + advanceTaxPaidByQuarter.getOrElse(2) { 0L }
        val q4Paid = q3Paid + advanceTaxPaidByQuarter.getOrElse(3) { 0L }

        var int234C = 0L
        val m1Target = (netAssessedTax * 0.15).toLong()
        if (q1Paid < (netAssessedTax * 0.12).toLong()) int234C += ((m1Target - q1Paid) * 0.03).toLong()

        val m2Target = (netAssessedTax * 0.45).toLong()
        if (q2Paid < (netAssessedTax * 0.36).toLong()) int234C += ((m2Target - q2Paid) * 0.03).toLong()

        val m3Target = (netAssessedTax * 0.75).toLong()
        if (q3Paid < m3Target) int234C += ((m3Target - q3Paid) * 0.03).toLong()

        val m4Target = netAssessedTax
        if (q4Paid < m4Target) int234C += ((m4Target - q4Paid) * 0.01).toLong()

        return Interest234Breakdown(
            interest234A = int234A,
            interest234B = int234B,
            interest234C = int234C,
            totalInterest = int234A + int234B + int234C
        )
    }
}
