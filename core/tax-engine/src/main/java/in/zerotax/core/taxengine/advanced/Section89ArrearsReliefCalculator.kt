package `in`.zerotax.core.taxengine.advanced

import `in`.zerotax.core.taxengine.calculator.DeterministicIndianTaxEngine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

/**
 * Section 89(1) Salary Arrears Tax Relief Calculator & Form 10E Compiler
 *
 * Implements deterministic statutory computation under Section 89(1) and Rule 21AA
 * of the Income-tax Rules, 1962:
 *
 * When an employee receives salary in arrears or in advance, the progressive tax slabs
 * cause a tax spike in the year of receipt. Section 89(1) equalizes the tax by computing:
 *   Relief = max(0, Incremental Tax in Receipt Year - Sum of Incremental Taxes in Accrual Years)
 *
 * Statutory Requirement (Rule 21AA):
 * Form 10E must be submitted electronically on the e-filing portal before furnishing the ITR;
 * otherwise, CPC Bengaluru automatically disallows the relief u/s 143(1).
 */
object Section89ArrearsReliefCalculator {

    data class AccrualYearParticulars(
        val assessmentYear: String, // e.g. "2024-25"
        val originalTotalIncome: Long,
        val originalTaxPaid: Long,
        val arrearsAllocatedToThisYear: Long
    )

    data class Section89ComputationInput(
        val receiptAssessmentYear: String = "2026-27",
        val currentYearNormalSalaryExcludingArrears: Long,
        val totalArrearsReceivedInCurrentYear: Long,
        val accrualYears: List<AccrualYearParticulars>,
        val isNewRegimeInReceiptYear: Boolean = true
    )

    data class AccrualYearTaxComparison(
        val assessmentYear: String,
        val originalIncome: Long,
        val originalTax: Long,
        val revisedIncomeWithArrears: Long,
        val revisedTax: Long,
        val incrementalTaxInAccrualYear: Long
    )

    data class Section89ReliefResult(
        val currentYearIncomeWithoutArrears: Long,
        val currentYearTaxWithoutArrears: Long,
        val currentYearIncomeWithArrears: Long,
        val currentYearTaxWithArrears: Long,
        val incrementalTaxInReceiptYear: Long, // Difference A
        val accrualYearBreakdown: List<AccrualYearTaxComparison>,
        val totalIncrementalTaxInAccrualYears: Long, // Difference B
        val reliefAllowableUnderSection89: Long, // max(0, A - B)
        val form10EJsonPayload: String,
        val cpcRejectionWarning: String = "Rule 21AA: Form 10E must be submitted electronically before filing ITR to avoid CPC Section 89 disallowance."
    ) {
        val eligibleSection89ReliefRupees: Long get() = reliefAllowableUnderSection89
    }

    fun computeSection89Relief(input: Section89ComputationInput, pan: String = "ABCDE1234F"): Section89ReliefResult =
        computeRelief(input, pan)

    fun computeRelief(input: Section89ComputationInput, pan: String = "ABCDE1234F"): Section89ReliefResult {
        val incomeWithoutArrears = max(0L, input.currentYearNormalSalaryExcludingArrears)
        val incomeWithArrears = incomeWithoutArrears + max(0L, input.totalArrearsReceivedInCurrentYear)

        // Step 1 & 2: Current Year Tax with and without arrears
        val taxWithoutArrears = computeTaxForIncome(incomeWithoutArrears, input.isNewRegimeInReceiptYear)
        val taxWithArrears = computeTaxForIncome(incomeWithArrears, input.isNewRegimeInReceiptYear)
        val incrementalTaxReceiptYear = max(0L, taxWithArrears - taxWithoutArrears) // Difference A

        // Step 3: Compute incremental tax in each past accrual year
        val accrualComparisons = mutableListOf<AccrualYearTaxComparison>()
        var totalIncrementalAccrualTax = 0L

        for (accrual in input.accrualYears) {
            val revisedIncome = accrual.originalTotalIncome + accrual.arrearsAllocatedToThisYear
            // For past years, compute using Old Regime slabs (standard historical basis)
            val revisedTax = computeHistoricalOldRegimeTax(revisedIncome)
            val incrementalTax = max(0L, revisedTax - accrual.originalTaxPaid)

            accrualComparisons.add(
                AccrualYearTaxComparison(
                    assessmentYear = accrual.assessmentYear,
                    originalIncome = accrual.originalTotalIncome,
                    originalTax = accrual.originalTaxPaid,
                    revisedIncomeWithArrears = revisedIncome,
                    revisedTax = revisedTax,
                    incrementalTaxInAccrualYear = incrementalTax
                )
            )
            totalIncrementalAccrualTax += incrementalTax
        }

        // Step 4: Relief u/s 89(1) = max(0, A - B)
        val relief = max(0L, incrementalTaxReceiptYear - totalIncrementalAccrualTax)

        val form10EJson = compileForm10EJson(pan, input, relief, accrualComparisons)

        return Section89ReliefResult(
            currentYearIncomeWithoutArrears = incomeWithoutArrears,
            currentYearTaxWithoutArrears = taxWithoutArrears,
            currentYearIncomeWithArrears = incomeWithArrears,
            currentYearTaxWithArrears = taxWithArrears,
            incrementalTaxInReceiptYear = incrementalTaxReceiptYear,
            accrualYearBreakdown = accrualComparisons,
            totalIncrementalTaxInAccrualYears = totalIncrementalAccrualTax,
            reliefAllowableUnderSection89 = relief,
            form10EJsonPayload = form10EJson
        )
    }

    private fun computeTaxForIncome(income: Long, isNewRegime: Boolean): Long {
        return if (isNewRegime) {
            DeterministicIndianTaxEngine.computeNewRegimeTaxAY2026_27(
                grossTotalIncome = income,
                standardDeduction = 75000L
            ).totalTaxLiabilityRounded288B
        } else {
            DeterministicIndianTaxEngine.computeOldRegimeTaxAY2026_27(
                grossTotalIncome = income,
                standardDeduction = 50000L,
                section80C = 150000L
            ).totalTaxLiabilityRounded288B
        }
    }

    private fun computeHistoricalOldRegimeTax(income: Long): Long {
        val taxable = max(0L, income - 50000L) // Standard deduction
        val baseTax = if (taxable <= 250000L) 0L
        else if (taxable <= 500000L) ((taxable - 250000L) * 0.05).toLong()
        else if (taxable <= 1000000L) (12500L + (taxable - 500000L) * 0.20).toLong()
        else (112500L + (taxable - 1000000L) * 0.30).toLong()

        val cess = (baseTax * 0.04).toLong()
        return baseTax + cess
    }

    private fun compileForm10EJson(
        pan: String,
        input: Section89ComputationInput,
        relief: Long,
        comparisons: List<AccrualYearTaxComparison>
    ): String {
        val root = JSONObject()
        val formDetails = JSONObject()
        formDetails.put("FormName", "Form10E")
        formDetails.put("FormType", "Form 10E")
        formDetails.put("RuleNo", "21AA")
        formDetails.put("SectionNo", "89(1)")
        formDetails.put("AssessmentYear", input.receiptAssessmentYear)

        val taxpayerDetails = JSONObject()
        taxpayerDetails.put("PAN", pan)

        val tableA = JSONObject()
        tableA.put("TotalArrearsReceived", input.totalArrearsReceivedInCurrentYear)
        tableA.put("Section89ReliefClaimed", relief)

        val pastYearsArray = JSONArray()
        for (comp in comparisons) {
            val yr = JSONObject()
            yr.put("AssessmentYear", comp.assessmentYear)
            yr.put("OriginalIncome", comp.originalIncome)
            yr.put("OriginalTax", comp.originalTax)
            yr.put("RevisedIncome", comp.revisedIncomeWithArrears)
            yr.put("RevisedTax", comp.revisedTax)
            yr.put("IncrementalTax", comp.incrementalTaxInAccrualYear)
            pastYearsArray.put(yr)
        }
        tableA.put("AnnexureTableA", pastYearsArray)

        root.put("FormDetails", formDetails)
        root.put("TaxpayerDetails", taxpayerDetails)
        root.put("ReliefSchedule", tableA)
        return root.toString(2)
    }
}
