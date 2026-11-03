package in.zerotax.core.taxengine.compiler

import in.zerotax.core.model.BankAccountDetails
import in.zerotax.core.model.FilingSection
import in.zerotax.core.model.ReconciledSalarySummary
import in.zerotax.core.model.TaxRegime
import in.zerotax.core.model.TaxpayerDeclaration
import in.zerotax.core.taxengine.calculator.RegimeComputationResult
import org.json.JSONArray
import org.json.JSONObject

object CbdtItr1JsonCompiler {
    fun compileItr1Json(
        pan: String,
        assessmentYear: String = "2026-27",
        salarySummary: ReconciledSalarySummary,
        computation: RegimeComputationResult,
        bankAccount: BankAccountDetails? = null,
        filingSection: FilingSection = FilingSection.ON_OR_BEFORE_DUE_DATE_139_1,
        declaration: TaxpayerDeclaration? = null
    ): String {
        val root = JSONObject()
        val formItr1 = JSONObject()

        // 1. Personal Information & Header
        val personalInfo = JSONObject()
        personalInfo.put("PAN", pan)
        personalInfo.put("AssessmentYear", assessmentYear)
        personalInfo.put("AssesseeName", declaration?.fullName ?: "Taxpayer")
        personalInfo.put("ResidentialStatus", "RES")
        formItr1.put("PersonalInfo", personalInfo)

        // 2. Filing Status
        val filingStatus = JSONObject()
        filingStatus.put("ReturnFileSec", filingSection.code)
        filingStatus.put("OptOutNewRegime", if (computation.regime == TaxRegime.OLD_REGIME) "Y" else "N")
        filingStatus.put("ResidentialStatus", "RES")
        formItr1.put("FilingStatus", filingStatus)

        // 3. Gross Salary Details (Schedule S)
        val grossSalaryObj = JSONObject()
        grossSalaryObj.put("GrossSalary", salarySummary.aggregateGrossSalary)
        grossSalaryObj.put("SalarySec17_1", salarySummary.aggregateGrossSalary)
        grossSalaryObj.put("PerquisitesSec17_2", 0L)
        grossSalaryObj.put("ProfitsInLieuSec17_3", 0L)
        grossSalaryObj.put("Sec10Exemptions", salarySummary.aggregateSec10Exemptions)
        grossSalaryObj.put("StandardDeduction", computation.standardDeduction)
        grossSalaryObj.put("NetSalary", salarySummary.aggregateNetTaxableSalary)
        formItr1.put("GrossSalaryDetails", grossSalaryObj)

        // 4. Tax Computation (Part B-TI & Part B-TTI)
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

        // 5. TDS on Salaries (Schedule TDS1)
        val tdsDetailsArray = JSONArray()
        for (emp in salarySummary.employers) {
            val tdsRow = JSONObject()
            tdsRow.put("EmployerTAN", emp.employerTan)
            tdsRow.put("EmployerName", emp.employerName)
            tdsRow.put("GrossPaymentCredited", emp.grossSalary)
            tdsRow.put("TdsClaimed", emp.tdsAllowed)
            tdsDetailsArray.put(tdsRow)
        }
        formItr1.put("TDSOnSalaries", tdsDetailsArray)

        // 6. Tax Payments & Refund Ledger (Schedule IT & Part B-TTI Summary)
        val taxesPaidObj = JSONObject()
        val totalTds = salarySummary.aggregateTdsAllowed
        taxesPaidObj.put("TotalTdsClaimed", totalTds)
        taxesPaidObj.put("AdvanceTaxPaid", 0L)
        taxesPaidObj.put("SelfAssessmentTaxPaid", 0L)
        taxesPaidObj.put("TotalTaxesPaid", totalTds)
        formItr1.put("TaxesPaid", taxesPaidObj)

        val refundOrPayableObj = JSONObject()
        val netRefund = if (totalTds > computation.totalTaxLiabilityRounded288B) {
            totalTds - computation.totalTaxLiabilityRounded288B
        } else {
            0L
        }
        val taxPayable = if (computation.totalTaxLiabilityRounded288B > totalTds) {
            computation.totalTaxLiabilityRounded288B - totalTds
        } else {
            0L
        }
        refundOrPayableObj.put("RefundDue", netRefund)
        refundOrPayableObj.put("BalanceTaxPayable", taxPayable)
        formItr1.put("RefundOrPayable", refundOrPayableObj)

        // 7. Bank Account Details for Electronic Credit of Refund
        val bankObj = JSONObject()
        val bank = bankAccount ?: BankAccountDetails(
            bankName = "State Bank of India",
            accountNumber = "XXXXXX1234",
            ifscCode = "SBIN0001234",
            accountType = "SAVINGS",
            isRefundAccount = true,
            isPreValidated = true
        )
        bankObj.put("BankName", bank.bankName)
        bankObj.put("AccountNumber", bank.accountNumber)
        bankObj.put("IFSC", bank.ifscCode)
        bankObj.put("AccountType", bank.accountType)
        bankObj.put("SelectedForRefund", if (bank.isRefundAccount) "Y" else "N")
        bankObj.put("PreValidatedFlag", if (bank.isPreValidated) "Y" else "N")
        formItr1.put("RefundBankAccount", bankObj)

        // 8. Taxpayer Verification Declaration u/s 140
        val verDecl = JSONObject()
        val dec = declaration ?: TaxpayerDeclaration(
            fullName = "Taxpayer",
            pan = pan,
            place = "Bengaluru",
            capacity = "SELF"
        )
        verDecl.put("DeclarationName", dec.fullName)
        verDecl.put("FatherName", dec.fatherName)
        verDecl.put("DeclarationPAN", dec.pan)
        verDecl.put("DeclarationPlace", dec.place)
        verDecl.put("DeclarationCapacity", dec.capacity)
        verDecl.put("DeclarationDate", dec.filingDate)
        formItr1.put("Verification", verDecl)

        root.put("ITR1", formItr1)
        root.put("SchemaVersion", "AY2026-27_v1.0")
        root.put("CompiledBy", "ZeroTax-Local-Deterministic-Engine")
        return root.toString(2)
    }
}
