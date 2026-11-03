package in.zerotax.core.taxengine.advanced

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Pre-July 23, 2024 Property LTCG Dual-Option Indexation Engine
 *
 * Implements deterministic statutory computation under the Second Proviso to Section 112(1)(a)
 * of the Income-tax Act, 1961 (introduced by Finance (No. 2) Act, 2024):
 *
 * For land or building acquired before July 23, 2024 by a resident individual or HUF:
 * Taxpayers are granted the statutory right to choose the LOWER tax liability between:
 *   - Option 1 (New Regime for Property): 12.5% tax without indexation
 *   - Option 2 (Grandfathered Benefit): 20.0% tax with Cost Inflation Index (CII)
 *
 * Also factors in:
 *   - Section 54 / 54F reinvestment exemption capped at ₹10 Crores
 *   - 25-Year Cost Inflation Index (CII) statutory seed table (2001-02 = 100 ... 2025-26 = 376)
 */
object PropertyLtcgIndexationEngine {

    val CII_TABLE = mapOf(
        "2001-02" to 100, "2002-03" to 105, "2003-04" to 109, "2004-05" to 113, "2005-06" to 117,
        "2006-07" to 122, "2007-08" to 129, "2008-09" to 137, "2009-10" to 148, "2010-11" to 167,
        "2011-12" to 184, "2012-13" to 200, "2013-14" to 220, "2014-15" to 240, "2015-16" to 254,
        "2016-17" to 264, "2017-18" to 272, "2018-19" to 280, "2019-20" to 289, "2020-21" to 301,
        "2021-22" to 317, "2022-23" to 331, "2023-24" to 348, "2024-25" to 363, "2025-26" to 376
    )

    const val SECTION_54_CAP_RUPEES = 10_00_00_000L // ₹10 Crores cap

    enum class PropertyAcquisitionDate {
        BEFORE_JULY_23_2024,
        ON_OR_AFTER_JULY_23_2024
    }

    data class PropertySaleInput(
        val acquisitionDateType: PropertyAcquisitionDate = PropertyAcquisitionDate.BEFORE_JULY_23_2024,
        val acquisitionFinancialYear: String = "2012-13",
        val saleFinancialYear: String = "2025-26",
        val actualCostOfAcquisitionRupees: Long,
        val costOfImprovementRupees: Long = 0L,
        val saleConsiderationRupees: Long,
        val transferExpensesRupees: Long = 0L,
        val section54ReinvestmentRupees: Long = 0L
    )

    data class DualOptionTaxComparison(
        val isEligibleForGrandfatheredOption: Boolean,
        val indexedCostOfAcquisition: Long,
        val capitalGainWithIndexation: Long,
        val capitalGainWithoutIndexation: Long,
        val section54ExemptionClaimed: Long,
        val netGainOption1Unindexed: Long,
        val taxOption1Unindexed12_5Pct: Long,
        val netGainOption2Indexed: Long,
        val taxOption2Indexed20Pct: Long,
        val recommendedOption: String,
        val finalTaxPayableWithCess: Long,
        val taxSavingsByGrandfathering: Long
    )

    fun computePropertyLtcg(input: PropertySaleInput): DualOptionTaxComparison {
        val ciiSale = CII_TABLE[input.saleFinancialYear] ?: 376
        val ciiAcquisition = CII_TABLE[input.acquisitionFinancialYear] ?: 100

        // 1. Indexed Cost = Actual Cost * (CII Sale / CII Acquisition)
        val indexedCost = ((input.actualCostOfAcquisitionRupees.toDouble() * ciiSale) / ciiAcquisition).roundToLong()

        // 2. Gross Gains
        val netConsideration = max(0L, input.saleConsiderationRupees - input.transferExpensesRupees)
        val grossGainWithoutIndexation = max(0L, netConsideration - input.actualCostOfAcquisitionRupees - input.costOfImprovementRupees)
        val grossGainWithIndexation = max(0L, netConsideration - indexedCost - input.costOfImprovementRupees)

        // 3. Section 54 / 54F Exemption (Capped at ₹10 Crores)
        val clampedSec54 = min(SECTION_54_CAP_RUPEES, max(0L, input.section54ReinvestmentRupees))
        val exemptionOption1 = min(clampedSec54, grossGainWithoutIndexation)
        val exemptionOption2 = min(clampedSec54, grossGainWithIndexation)

        val netGainOption1 = max(0L, grossGainWithoutIndexation - exemptionOption1)
        val netGainOption2 = max(0L, grossGainWithIndexation - exemptionOption2)

        // Option 1: 12.5% without indexation + 4% cess
        val taxOpt1Base = (netGainOption1 * 0.125).roundToLong()
        val taxOpt1Total = (taxOpt1Base * 1.04).roundToLong()

        // Option 2: 20.0% with indexation + 4% cess
        val taxOpt2Base = (netGainOption2 * 0.20).roundToLong()
        val taxOpt2Total = (taxOpt2Base * 1.04).roundToLong()

        val isEligible = input.acquisitionDateType == PropertyAcquisitionDate.BEFORE_JULY_23_2024

        val (recOption, finalTax, savings) = if (!isEligible) {
            Triple("Option 1 (12.5% Flat without indexation)", taxOpt1Total, 0L)
        } else if (taxOpt2Total <= taxOpt1Total) {
            Triple(
                "Option 2 (20% with CII Indexation) - SAVES TAX",
                taxOpt2Total,
                taxOpt1Total - taxOpt2Total
            )
        } else {
            Triple(
                "Option 1 (12.5% without Indexation) - LOWER TAX",
                taxOpt1Total,
                taxOpt2Total - taxOpt1Total
            )
        }

        return DualOptionTaxComparison(
            isEligibleForGrandfatheredOption = isEligible,
            indexedCostOfAcquisition = indexedCost,
            capitalGainWithIndexation = grossGainWithIndexation,
            capitalGainWithoutIndexation = grossGainWithoutIndexation,
            section54ExemptionClaimed = exemptionOption1,
            netGainOption1Unindexed = netGainOption1,
            taxOption1Unindexed12_5Pct = taxOpt1Total,
            netGainOption2Indexed = netGainOption2,
            taxOption2Indexed20Pct = taxOpt2Total,
            recommendedOption = recOption,
            finalTaxPayableWithCess = finalTax,
            taxSavingsByGrandfathering = savings
        )
    }
}
