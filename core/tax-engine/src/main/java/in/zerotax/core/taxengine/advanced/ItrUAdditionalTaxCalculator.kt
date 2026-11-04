package `in`.zerotax.core.taxengine.advanced

import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Updated Return (ITR-U u/s 139(8A) & Section 140B) Additional Tax Engine
 *
 * Implements deterministic statutory computation under Section 139(8A) and Section 140B
 * of the Income-tax Act, 1961:
 *
 * 1. Eligibility Gates:
 *    - Allows filing within 24 months from the end of the relevant Assessment Year.
 *    - Prohibited if return results in a refund or lower tax liability than previously declared.
 * 2. Section 140B Additional Tax Surcharge:
 *    - 25% Additional Tax: If filed within 12 months from the end of relevant AY.
 *    - 50% Additional Tax: If filed between 12 and 24 months from the end of relevant AY.
 * 3. Total Outflow:
 *    - Additional Tax = (Tax on Additional Income + Statutory Interest 234A/B/C) * 25% or 50%.
 *    - Total Payable u/s 140B = Additional Tax + Underlying Tax + Interest.
 */
object ItrUAdditionalTaxCalculator {

    enum class ItrUTimeWindow(val label: String, val surchargePercent: Double) {
        WITHIN_12_MONTHS("Within 12 months from end of AY (25% Additional Tax)", 0.25),
        BETWEEN_12_AND_24_MONTHS("Between 12 and 24 months from end of AY (50% Additional Tax)", 0.50),
        TIME_BARRED("Exceeds 24 months (Statutorily Barred)", 0.0)
    }

    data class ItrUInput(
        val relevantAssessmentYear: String = "2024-25",
        val additionalIncomeDeclaredRupees: Long,
        val existingTaxPaidOriginallyRupees: Long = 0L,
        val monthsElapsedFromEndOfAy: Int,
        val interest234A: Long = 0L,
        val interest234B: Long = 0L,
        val interest234C: Long = 0L
    )

    data class ItrUResult(
        val timeWindow: ItrUTimeWindow,
        val isEligibleToFileItrU: Boolean,
        val eligibilityRemark: String,
        val baseTaxOnAdditionalIncome: Long,
        val cessAmount: Long,
        val taxPlusCess: Long,
        val totalStatutoryInterest: Long,
        val taxAndInterestBaseFor140B: Long,
        val additionalTaxSurcharge140B: Long,
        val totalPayableUnderSection140B: Long,
        val challan280MinorHead: String // Minor Head 390 (Self Assessment / Updated Return)
    )

    fun computeItrUAdditionalTax(input: ItrUInput): ItrUResult = computeItrU(input)

    fun computeItrU(input: ItrUInput): ItrUResult {
        val window = when {
            input.monthsElapsedFromEndOfAy in 0..12 -> ItrUTimeWindow.WITHIN_12_MONTHS
            input.monthsElapsedFromEndOfAy in 13..24 -> ItrUTimeWindow.BETWEEN_12_AND_24_MONTHS
            else -> ItrUTimeWindow.TIME_BARRED
        }

        if (window == ItrUTimeWindow.TIME_BARRED) {
            return ItrUResult(
                timeWindow = window,
                isEligibleToFileItrU = false,
                eligibilityRemark = "Section 139(8A) window has expired. Updated Return can only be filed within 24 months from the end of the relevant assessment year.",
                baseTaxOnAdditionalIncome = 0L,
                cessAmount = 0L,
                taxPlusCess = 0L,
                totalStatutoryInterest = 0L,
                taxAndInterestBaseFor140B = 0L,
                additionalTaxSurcharge140B = 0L,
                totalPayableUnderSection140B = 0L,
                challan280MinorHead = "N/A"
            )
        }

        if (input.additionalIncomeDeclaredRupees <= 0L) {
            return ItrUResult(
                timeWindow = window,
                isEligibleToFileItrU = false,
                eligibilityRemark = "Section 139(8A) Proviso (a): Updated Return cannot be filed to declare a loss, decrease total tax liability, or claim/increase a tax refund.",
                baseTaxOnAdditionalIncome = 0L,
                cessAmount = 0L,
                taxPlusCess = 0L,
                totalStatutoryInterest = 0L,
                taxAndInterestBaseFor140B = 0L,
                additionalTaxSurcharge140B = 0L,
                totalPayableUnderSection140B = 0L,
                challan280MinorHead = "N/A"
            )
        }

        // Slab rate computation on additional income (standard 20% / 30% incremental estimation)
        val rate = if (input.additionalIncomeDeclaredRupees > 1000000L) 0.30 else 0.20
        val baseTax = (input.additionalIncomeDeclaredRupees * rate).roundToLong()
        val cess = (baseTax * 0.04).roundToLong()
        val taxPlusCess = baseTax + cess

        val totalInterest = input.interest234A + input.interest234B + input.interest234C
        val baseFor140B = max(0L, taxPlusCess + totalInterest - input.existingTaxPaidOriginallyRupees)

        val additionalTax140B = (baseFor140B * window.surchargePercent).roundToLong()
        val totalPayable = baseFor140B + additionalTax140B

        return ItrUResult(
            timeWindow = window,
            isEligibleToFileItrU = true,
            eligibilityRemark = "Eligible to file ITR-U u/s 139(8A). Additional tax rate is ${(window.surchargePercent * 100).toInt()}% under Section 140B.",
            baseTaxOnAdditionalIncome = baseTax,
            cessAmount = cess,
            taxPlusCess = taxPlusCess,
            totalStatutoryInterest = totalInterest,
            taxAndInterestBaseFor140B = baseFor140B,
            additionalTaxSurcharge140B = additionalTax140B,
            totalPayableUnderSection140B = totalPayable,
            challan280MinorHead = "Minor Head 390 (Tax on Updated Return u/s 140B)"
        )
    }
}
