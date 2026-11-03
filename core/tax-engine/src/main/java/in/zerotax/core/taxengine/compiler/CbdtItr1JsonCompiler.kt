package in.zerotax.core.taxengine.compiler

import in.zerotax.core.model.ReconciledSalarySummary
import in.zerotax.core.model.TaxRegime
import in.zerotax.core.taxengine.calculator.RegimeComputationResult
import org.json.JSONArray
import org.json.JSONObject

object CbdtItr1JsonCompiler {
    fun compileItr1Json(
        pan: String,
        assessmentYear: String,
        salarySummary: ReconciledSalarySummary,
        computation: RegimeComputationResult
    ): String {
        val root = JSONObject()
        val formItr1 = JSONObject()

        val personalInfo = JSONObject()
        personalInfo.put("PAN", pan)
        personalInfo.put("AssessmentYear", assessmentYear)
        formItr1.put("PersonalInfo", personalInfo)

        val filingStatus = JSONObject()
        filingStatus.put("OptOutNewRegime", if (computation.regime == TaxRegime.OLD_REGIME) "Y" else "N")
        formItr1.put("FilingStatus", filingStatus)

        val grossSalaryObj = JSONObject()
        grossSalaryObj.put("GrossSalary", salarySummary.aggregateGrossSalary)
        grossSalaryObj.put("Sec10Exemptions", salarySummary.aggregateSec10Exemptions)
        grossSalaryObj.put("StandardDeduction", computation.standardDeduction)
        grossSalaryObj.put("NetSalary", salarySummary.aggregateNetTaxableSalary)
        formItr1.put("GrossSalaryDetails", grossSalaryObj)

        val taxComp = JSONObject()
        taxComp.put("GrossTotalIncome", computation.grossTotalIncome)
        taxComp.put("TotalChapterVIA", computation.totalChapterVIADeductions)
        taxComp.put("TotalIncome", computation.netTaxableNormalIncomeRounded288A)
        taxComp.put("BaseTax", computation.baseSlabTaxOnNormalIncome)
        taxComp.put("Rebate87A", computation.rebate87A)
        taxComp.put("MarginalRelief87A", computation.marginalRelief87A)
        taxComp.put("Surcharge", computation.surchargeAfterMarginalRelief)
        taxComp.put("Cess", computation.healthAndEducationCess)
        taxComp.put("TotalTaxLiability", computation.totalTaxLiabilityRounded288B)
        formItr1.put("TaxComputation", taxComp)

        val tdsDetailsArray = JSONArray()
        for (emp in salarySummary.employers) {
            val tdsRow = JSONObject()
            tdsRow.put("EmployerTAN", emp.employerTan)
            tdsRow.put("EmployerName", emp.employerName)
            tdsRow.put("TdsClaimed", emp.tdsAllowed)
            tdsDetailsArray.put(tdsRow)
        }
        formItr1.put("TDSOnSalaries", tdsDetailsArray)

        root.put("ITR1", formItr1)
        return root.toString(2)
    }
}
