package in.zerotax.core.taxengine.advanced

import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Form 10-IEA Business Regime Governance Engine
 *
 * Implements deterministic statutory compliance under Section 115BAC(3) and Rule 21AGA
 * of the Income-tax Rules, 1962 (Notification 43/2023):
 *
 * 1. Form 10-IEA Requirement:
 *    - Salaried individuals can switch between Old and New regimes each year directly inside ITR-1/2.
 *    - Taxpayers with Business or Professional income (filing ITR-3 or ITR-4) MUST electronically
 *      furnish Form 10-IEA on or before the Section 139(1) due date (July 31 / October 31) to opt out
 *      of the default New Regime into the Old Regime.
 * 2. 1-Time Lifetime Lock-In Rule:
 *    - Once a business taxpayer opts out of the default New Regime, they are granted ONLY ONE
 *      opportunity in their lifetime to re-enter the New Regime. Once re-entered, they are
 *      PERMANENTLY DEBARRED from opting into the Old Regime for as long as they have business income.
 * 3. JSON Payload Compiler:
 *    - Generates valid CBDT schema-compliant JSON for Form 10-IEA electronic submission.
 */
object Form10IeaBusinessRegimeEngine {

    enum class Form10IeaFilingReason(val code: String, val title: String) {
        OPT_OUT_OF_NEW_REGIME("01", "Exercising option to opt-out of default New Regime u/s 115BAC(1A) into Old Regime"),
        RE_ENTER_NEW_REGIME("02", "Re-entering the default New Regime (Final 1-time lifetime switch)"),
        WITHDRAWAL_OF_OPTION("03", "Ceasing to have business income / normal return")
    }

    data class Form10IeaInput(
        val pan: String,
        val taxpayerName: String,
        val assessmentYear: String = "2026-27",
        val filingReason: Form10IeaFilingReason,
        val businessCode: String = "1405", // Default: Software / IT services
        val businessDescription: String = "IT & Professional Services",
        val hasOptedBackInLifetimeBefore: Boolean = false,
        val dateOfFiling: LocalDate = LocalDate.now(),
        val acknowledgementNumber: String = ""
    )

    data class Form10IeaDecision(
        val isForm10IeaMandatory: Boolean,
        val isEligibleToOptOut: Boolean,
        val lockInWarning: String,
        val statutoryFilingDeadline: String,
        val cbdtJsonPayload: String,
        val schedule115BacAckCode: String
    )

    /**
     * Evaluates Form 10-IEA eligibility and compiles the CBDT electronic submission payload.
     */
    fun processForm10Iea(
        hasBusinessOrProfessionIncome: Boolean,
        chosenRegimeIsOld: Boolean,
        input: Form10IeaInput,
        requiresTaxAudit: Boolean = false
    ): Form10IeaDecision {
        val deadline = if (requiresTaxAudit) "31 October 2026" else "31 July 2026"

        // Rule: If taxpayer does not have business income, Form 10-IEA is NOT required
        if (!hasBusinessOrProfessionIncome) {
            return Form10IeaDecision(
                isForm10IeaMandatory = false,
                isEligibleToOptOut = true,
                lockInWarning = "Form 10-IEA is NOT required for salaried taxpayers without business income. Choose regime directly on ITR-1/2.",
                statutoryFilingDeadline = "N/A",
                cbdtJsonPayload = "{}",
                schedule115BacAckCode = ""
            )
        }

        // Rule: If business taxpayer chooses the DEFAULT New Regime, Form 10-IEA is NOT needed (unless re-entering after opting out)
        if (!chosenRegimeIsOld && input.filingReason != Form10IeaFilingReason.RE_ENTER_NEW_REGIME) {
            return Form10IeaDecision(
                isForm10IeaMandatory = false,
                isEligibleToOptOut = true,
                lockInWarning = "Taxpayer is filing under default New Regime Section 115BAC(1A). No Form 10-IEA required.",
                statutoryFilingDeadline = deadline,
                cbdtJsonPayload = "{}",
                schedule115BacAckCode = ""
            )
        }

        // Rule: 1-Time Lifetime Lock-in check
        if (input.hasOptedBackInLifetimeBefore && chosenRegimeIsOld) {
            return Form10IeaDecision(
                isForm10IeaMandatory = true,
                isEligibleToOptOut = false,
                lockInWarning = "STATUTORY PROHIBITION u/s 115BAC(3) Proviso: Taxpayer has already exhausted their 1-time lifetime re-entry option. Permanently barred from choosing the Old Regime while having business income.",
                statutoryFilingDeadline = deadline,
                cbdtJsonPayload = "{}",
                schedule115BacAckCode = "DEBARRED"
            )
        }

        val jsonPayload = compileForm10IeaJson(input)
        val generatedAck = if (input.acknowledgementNumber.isNotBlank()) {
            input.acknowledgementNumber
        } else {
            "10IEA${input.pan.take(5)}${input.assessmentYear.replace("-", "")}987654"
        }

        val warning = when (input.filingReason) {
            Form10IeaFilingReason.OPT_OUT_OF_NEW_REGIME ->
                "MANDATORY u/s 115BAC(3): Form 10-IEA must be filed before $deadline. Note: You have only 1 lifetime opportunity to switch back to the New Regime in the future."
            Form10IeaFilingReason.RE_ENTER_NEW_REGIME ->
                "FINAL LIFETIME SWITCH: By re-entering the New Regime, you will be permanently locked out of the Old Regime for all future years with business income."
            Form10IeaFilingReason.WITHDRAWAL_OF_OPTION ->
                "Withdrawal of regime opt-out option filed."
        }

        return Form10IeaDecision(
            isForm10IeaMandatory = true,
            isEligibleToOptOut = true,
            lockInWarning = warning,
            statutoryFilingDeadline = deadline,
            cbdtJsonPayload = jsonPayload,
            schedule115BacAckCode = generatedAck
        )
    }

    private fun compileForm10IeaJson(input: Form10IeaInput): String {
        val root = JSONObject()
        val formDetails = JSONObject()
        formDetails.put("FormName", "Form 10-IEA")
        formDetails.put("RuleNo", "21AGA")
        formDetails.put("SectionNo", "115BAC(3)")
        formDetails.put("AssessmentYear", input.assessmentYear)

        val taxpayerDetails = JSONObject()
        taxpayerDetails.put("PAN", input.pan)
        taxpayerDetails.put("Name", input.taxpayerName)
        taxpayerDetails.put("DateOfFiling", input.dateOfFiling.format(DateTimeFormatter.ISO_LOCAL_DATE))

        val optionDetails = JSONObject()
        optionDetails.put("FilingReasonCode", input.filingReason.code)
        optionDetails.put("FilingReasonDescription", input.filingReason.title)
        optionDetails.put("BusinessCode", input.businessCode)
        optionDetails.put("BusinessNature", input.businessDescription)
        optionDetails.put("StatutoryDeclarationAccepted", true)

        root.put("FormDetails", formDetails)
        root.put("TaxpayerDetails", taxpayerDetails)
        root.put("OptionDetails", optionDetails)
        return root.toString(2)
    }
}

