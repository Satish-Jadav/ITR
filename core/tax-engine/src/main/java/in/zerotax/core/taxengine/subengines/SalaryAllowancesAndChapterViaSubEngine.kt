package `in`.zerotax.core.taxengine.subengines

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

object SalaryAllowancesAndChapterViaSubEngine {

    // --- SECTION 10 SALARY ALLOWANCES ---

    fun computeGratuityExemptionSec10_10(
        isGovtEmployee: Boolean,
        actualGratuityReceived: Long,
        lastDrawnBasicPlusDa: Long,
        completedYearsOfService: Int
    ): Long {
        if (isGovtEmployee) return actualGratuityReceived
        val statutoryFormula = (15.0 / 26.0 * lastDrawnBasicPlusDa * completedYearsOfService).roundToLong()
        return min(actualGratuityReceived, min(statutoryFormula, 20_00_000L))
    }

    fun computeLeaveEncashmentExemptionSec10_10AA(
        isGovtEmployee: Boolean,
        actualLeaveEncashment: Long,
        averageMonthlySalaryLast10Months: Long,
        unavailedLeaveDays: Int
    ): Long {
        if (isGovtEmployee) return actualLeaveEncashment
        val tenMonthsSalary = averageMonthlySalaryLast10Months * 10L
        val cashEquivalentUnavailed = (unavailedLeaveDays / 30.0 * averageMonthlySalaryLast10Months).roundToLong()
        return min(actualLeaveEncashment, min(tenMonthsSalary, min(cashEquivalentUnavailed, 25_00_000L)))
    }

    fun computeCommutedPensionExemptionSec10_10A(
        isGovtEmployee: Boolean,
        commutedPensionReceived: Long,
        isGratuityAlsoReceived: Boolean
    ): Long {
        if (isGovtEmployee) return commutedPensionReceived
        return if (isGratuityAlsoReceived) {
            (commutedPensionReceived / 3.0).roundToLong() // 1/3rd exempt
        } else {
            (commutedPensionReceived * 0.50).roundToLong() // 1/2 exempt
        }
    }

    fun computeChildrenEducationAndHostelSec10_14(
        numberOfChildren: Int,
        hostelChildrenCount: Int
    ): Long {
        val eligibleEduChildren = min(2, max(0, numberOfChildren))
        val eligibleHostelChildren = min(2, max(0, hostelChildrenCount))
        val eduExemption = eligibleEduChildren * 100L * 12L // ₹100/mo/child (max 2)
        val hostelExemption = eligibleHostelChildren * 300L * 12L // ₹300/mo/child (max 2)
        return eduExemption + hostelExemption
    }

    fun computeDisabledTransportAllowanceSec10_14(isSpeciallyAbled: Boolean): Long {
        // ₹3,200/month = ₹38,400/year (Exempt in BOTH Old and New Regimes)
        return if (isSpeciallyAbled) 38_400L else 0L
    }

    fun computePostOfficeSavingsExemptionSec10_15_i(interestAmount: Long, isJointAccount: Boolean): Long {
        val limit = if (isJointAccount) 7_000L else 3_500L
        return min(interestAmount, limit)
    }

    // --- CHAPTER VI-A DEDUCTIONS ---

    fun computeSection80GG(rentPaidAnnual: Long, adjustedTotalIncome: Long): Long {
        if (rentPaidAnnual <= 0L || adjustedTotalIncome <= 0L) return 0L
        val c1 = 60_000L // ₹5,000/month
        val c2 = (adjustedTotalIncome * 0.25).roundToLong()
        val c3 = max(0L, rentPaidAnnual - (adjustedTotalIncome * 0.10).roundToLong())
        return min(c1, min(c2, c3))
    }

    fun computeSection80DComprehensive(
        selfHealthInsurance: Long,
        selfPreventiveCheckup: Long,
        isSelfOrSpouseSenior: Boolean,
        parentsHealthInsurance: Long,
        parentsPreventiveCheckup: Long,
        isParentsSenior: Boolean,
        parentsUninsuredMedicalExpenditure: Long
    ): Pair<Long, Long> {
        val maxSelf = if (isSelfOrSpouseSenior) 50_000L else 25_000L
        val allowableSelfCheckup = min(5_000L, selfPreventiveCheckup)
        val totalSelfClaim = min(maxSelf, selfHealthInsurance + allowableSelfCheckup)

        val maxParents = if (isParentsSenior) 50_000L else 25_000L
        val remainingCheckupCap = max(0L, 5_000L - allowableSelfCheckup)
        val allowableParentsCheckup = min(remainingCheckupCap, parentsPreventiveCheckup)

        val parentsSpend = if (isParentsSenior && parentsHealthInsurance == 0L) {
            parentsUninsuredMedicalExpenditure + allowableParentsCheckup
        } else {
            parentsHealthInsurance + allowableParentsCheckup
        }
        val totalParentsClaim = min(maxParents, parentsSpend)

        return totalSelfClaim to totalParentsClaim
    }

    fun computeSection80E(interestPaid: Long, repaymentYearNumber: Int): Long {
        // 100% of interest with NO monetary cap, up to 8 consecutive assessment years
        return if (repaymentYearNumber in 1..8) max(0L, interestPaid) else 0L
    }

    fun computeSection80EEA(
        loanSanctionYear: String,
        interestPaidExceeding24b: Long,
        stampDutyValue: Long
    ): Long {
        val isSanctionEligible = loanSanctionYear in listOf("2019-20", "2020-21", "2021-22")
        val isAffordable = stampDutyValue <= 45_00_000L
        return if (isSanctionEligible && isAffordable) {
            min(1_50_000L, max(0L, interestPaidExceeding24b))
        } else 0L
    }

    fun computeSection80EEB(evLoanInterestPaid: Long): Long {
        return min(1_50_000L, max(0L, evLoanInterestPaid))
    }

    fun computeSection80DDOr80U(isSevereDisability: Boolean): Long {
        return if (isSevereDisability) 1_25_000L else 75_000L
    }

    fun computeSection80DDB(
        actualMedicalSpend: Long,
        isPatientSeniorCitizen: Boolean,
        insuranceReimbursement: Long
    ): Long {
        val limit = if (isPatientSeniorCitizen) 1_00_000L else 40_000L
        val eligibleSpend = min(actualMedicalSpend, limit)
        return max(0L, eligibleSpend - insuranceReimbursement)
    }

    fun computeSection80TTAorTTB(
        savingsInterest: Long,
        fdInterest: Long,
        isSeniorCitizen: Boolean
    ): Long {
        return if (isSeniorCitizen) {
            min(savingsInterest + fdInterest, 50_00_000L.let { 50_000L })
        } else {
            min(savingsInterest, 10_000L)
        }
    }
}

