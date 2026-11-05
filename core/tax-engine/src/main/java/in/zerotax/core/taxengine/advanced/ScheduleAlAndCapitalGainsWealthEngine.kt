package `in`.zerotax.core.taxengine.advanced

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Feature F-09: Schedule AL (Assets and Liabilities at Cost) Auto-Builder.
 * Mandatory under Section 139 for any individual or HUF whose Total Income exceeds ₹50,00,000.
 */
data class ImmovableAssetCostEntry(
    val description: String,
    val address: String,
    val costOfAcquisitionRupees: Long
)

data class MovableAssetCostEntry(
    val assetCategory: String, // SHARES_AND_MFS, JEWELRY_BULLION, VEHICLES_BOATS_AIRCRAFT, BANK_DEPOSITS_CASH
    val costOrBalanceAsOnMarch31Rupees: Long
)

data class LiabilityCostEntry(
    val liabilityType: String, // HOUSING_LOAN, VEHICLE_LOAN, OTHER_DEBTS
    val outstandingPrincipalAsOnMarch31Rupees: Long
)

data class ScheduleAlPayload(
    val isScheduleAlMandatory: Boolean,
    val totalImmovableAssetsAtCost: Long,
    val totalMovableAssetsAtCost: Long,
    val totalAssetsAtCost: Long,
    val totalLiabilitiesAtCost: Long,
    val netWealthAtCostRupees: Long,
    val cbdtJsonScheduleAlNode: String
)

/**
 * Feature F-03: Section 50C & Section 43CA 10% Stamp Duty Safe-Harbor Shield.
 */
data class Section50CSafeHarborResult(
    val actualAgreementConsideration: Long,
    val stampDutyCircleRateValue: Long,
    val isProtectedUnder10PercentSafeHarbor: Boolean,
    val effectiveFullValueOfConsiderationAdopted: Long,
    val statutoryExplanation: String
)

/**
 * Share Buyback Tax Calculator under Finance (No. 2) Act, 2024 (Effective Oct 1, 2024).
 * Buyback proceeds are taxed as Deemed Dividend u/s 2(22)(f) in Schedule OS, while the original
 * purchase cost is treated as a Capital Loss u/s 46A in Schedule CG!
 */
data class ShareBuybackInput(
    val companyName: String,
    val sharesTendered: Long,
    val originalPurchaseCostPerShareRupees: Long,
    val buybackPricePerShareRupees: Long,
    val isHoldingPeriodOver12Months: Boolean
)

data class ShareBuybackResult(
    val deemedDividendIncomeScheduleOs: Long,
    val capitalLossTreatedScheduleCg: Long,
    val isLongTermLoss: Boolean,
    val statutoryExplanation: String
)

/**
 * Section 54 / 54EC / 54F Reinvestment Planner with CGAS 1988 Compliance.
 */
data class CapitalGainsReinvestmentPlanInput(
    val netSaleConsiderationRupees: Long,
    val grossLongTermCapitalGainRupees: Long,
    val actualAmountReinvestedInNewHouseRupees: Long = 0L,
    val amountDepositedInCgas1988Rupees: Long = 0L,
    val amountInvestedInSec54EcBondsRupees: Long = 0L,
    val monthsElapsedFromSaleDateTo54EcInvestment: Int = 0
)

data class CapitalGainsReinvestmentPlanResult(
    val section54ResidentialExemptionClaimed: Long,
    val section54EcBondExemptionClaimed: Long,
    val totalExemptionsClaimed: Long,
    val netTaxableLtcgAfterExemptions: Long,
    val cgas1988DepositCompliant: Boolean,
    val auditDirectives: List<String>
)

object ScheduleAlAndCapitalGainsWealthEngine {

    const val SECTION_54_STATUTORY_CAP = 10_00_00_000L // ₹10 Crores cap u/s 54 and 54F
    const val SECTION_54EC_STATUTORY_CAP = 50_00_000L  // ₹50 Lakhs cap u/s 54EC

    // --- FEATURE F-09: SCHEDULE AL AUTO-BUILDER ---

    fun buildScheduleAl(
        totalIncomeRupees: Long,
        immovableAssets: List<ImmovableAssetCostEntry>,
        movableAssets: List<MovableAssetCostEntry>,
        liabilities: List<LiabilityCostEntry>
    ): ScheduleAlPayload {
        val isMandatory = totalIncomeRupees > 50_00_000L

        val totalImmovable = immovableAssets.sumOf { it.costOfAcquisitionRupees }
        val totalMovable = movableAssets.sumOf { it.costOrBalanceAsOnMarch31Rupees }
        val totalAssets = totalImmovable + totalMovable
        val totalLiabilities = liabilities.sumOf { it.outstandingPrincipalAsOnMarch31Rupees }
        val netWealth = max(0L, totalAssets - totalLiabilities)

        val jsonNode = """
        {
          "ScheduleAL": {
            "isMandatory": $isMandatory,
            "totalImmovableAtCost": $totalImmovable,
            "totalMovableAtCost": $totalMovable,
            "totalAssets": $totalAssets,
            "totalLiabilities": $totalLiabilities,
            "netAssetPositionAtCost": $netWealth
          }
        }
        """.trimIndent()

        return ScheduleAlPayload(
            isScheduleAlMandatory = isMandatory,
            totalImmovableAssetsAtCost = totalImmovable,
            totalMovableAssetsAtCost = totalMovable,
            totalAssetsAtCost = totalAssets,
            totalLiabilitiesAtCost = totalLiabilities,
            netWealthAtCostRupees = netWealth,
            cbdtJsonScheduleAlNode = jsonNode
        )
    }

    // --- FEATURE F-03: SECTION 50C 10% STAMP DUTY SAFE-HARBOR SHIELD ---

    fun verifySection50CSafeHarbor(
        actualAgreementConsideration: Long,
        stampDutyCircleRateValue: Long
    ): Section50CSafeHarborResult {
        if (stampDutyCircleRateValue <= 0L || stampDutyCircleRateValue <= actualAgreementConsideration) {
            return Section50CSafeHarborResult(
                actualAgreementConsideration = actualAgreementConsideration,
                stampDutyCircleRateValue = stampDutyCircleRateValue,
                isProtectedUnder10PercentSafeHarbor = true,
                effectiveFullValueOfConsiderationAdopted = actualAgreementConsideration,
                statutoryExplanation = "Agreement value (₹$actualAgreementConsideration) is greater than or equal to circle rate. Accepted u/s 50C."
            )
        }

        val safeHarborLimit = (actualAgreementConsideration * 1.10).roundToLong()
        val isProtected = stampDutyCircleRateValue <= safeHarborLimit

        return if (isProtected) {
            Section50CSafeHarborResult(
                actualAgreementConsideration = actualAgreementConsideration,
                stampDutyCircleRateValue = stampDutyCircleRateValue,
                isProtectedUnder10PercentSafeHarbor = true,
                effectiveFullValueOfConsiderationAdopted = actualAgreementConsideration,
                statutoryExplanation = "Circle rate (₹$stampDutyCircleRateValue) exceeds agreement price by <= 10% (tolerance cap ₹$safeHarborLimit). Protected under Third Proviso to Section 50C(1); agreement price adopted with ₹0 deemed addition!"
            )
        } else {
            Section50CSafeHarborResult(
                actualAgreementConsideration = actualAgreementConsideration,
                stampDutyCircleRateValue = stampDutyCircleRateValue,
                isProtectedUnder10PercentSafeHarbor = false,
                effectiveFullValueOfConsiderationAdopted = stampDutyCircleRateValue,
                statutoryExplanation = "Circle rate (₹$stampDutyCircleRateValue) exceeds agreement price by more than 10% (safe-harbor tolerance limit was ₹$safeHarborLimit). Circle rate statutorily adopted as Full Value of Consideration under Section 50C."
            )
        }
    }

    // --- POST-OCT 1, 2024 SHARE BUYBACK TAX CALCULATOR ---

    fun computePostOct2024ShareBuyback(input: ShareBuybackInput): ShareBuybackResult {
        val totalBuybackProceeds = input.sharesTendered * input.buybackPricePerShareRupees
        val totalOriginalCost = input.sharesTendered * input.originalPurchaseCostPerShareRupees

        // Under Section 2(22)(f), entire buyback proceeds are deemed dividend in Schedule OS
        val deemedDividend = totalBuybackProceeds

        // Under Section 46A, original purchase cost is treated as capital loss
        val capitalLoss = totalOriginalCost

        val explanation = """
            Under Finance (No. 2) Act 2024 (w.e.f. October 1, 2024):
            1. Total buyback proceeds of ₹$deemedDividend are taxed as Deemed Dividend under Section 2(22)(f) in Schedule OS at slab rate.
            2. Original purchase cost of ₹$capitalLoss is allowed as a Capital Loss (${if (input.isHoldingPeriodOver12Months) "LTCL" else "STCL"}) under Section 46A, eligible for set-off and 8-year carry-forward in Schedule CFL!
        """.trimIndent()

        return ShareBuybackResult(
            deemedDividendIncomeScheduleOs = deemedDividend,
            capitalLossTreatedScheduleCg = capitalLoss,
            isLongTermLoss = input.isHoldingPeriodOver12Months,
            statutoryExplanation = explanation
        )
    }

    // --- SECTION 54 / 54EC REINVESTMENT & CGAS 1988 PLANNER ---

    fun planCapitalGainsReinvestments(input: CapitalGainsReinvestmentPlanInput): CapitalGainsReinvestmentPlanResult {
        val rawSec54 = input.actualAmountReinvestedInNewHouseRupees + input.amountDepositedInCgas1988Rupees
        val clampedSec54 = min(SECTION_54_STATUTORY_CAP, min(input.grossLongTermCapitalGainRupees, rawSec54))

        // Section 54EC bonds: Must be invested within 6 months of sale date, capped at ₹50 Lakhs
        val is54EcWithin6Months = input.monthsElapsedFromSaleDateTo54EcInvestment <= 6
        val allowable54Ec = if (is54EcWithin6Months) {
            min(SECTION_54EC_STATUTORY_CAP, input.amountInvestedInSec54EcBondsRupees)
        } else {
            0L
        }

        val totalExemptions = min(input.grossLongTermCapitalGainRupees, clampedSec54 + allowable54Ec)
        val netLtcg = max(0L, input.grossLongTermCapitalGainRupees - totalExemptions)

        val directives = mutableListOf<String>()

        if (input.actualAmountReinvestedInNewHouseRupees > SECTION_54_STATUTORY_CAP) {
            directives.add("STATUTORY CEILING: Section 54/54F reinvestment is capped at ₹10 Crores. Excess ₹${input.actualAmountReinvestedInNewHouseRupees - SECTION_54_STATUTORY_CAP} cannot be claimed.")
        }
        if (input.amountInvestedInSec54EcBondsRupees > SECTION_54EC_STATUTORY_CAP) {
            directives.add("BOND CEILING: Section 54EC REC/PFC/NHAI bonds are capped at ₹50 Lakhs per financial year.")
        }
        if (input.amountInvestedInSec54EcBondsRupees > 0L && !is54EcWithin6Months) {
            directives.add("TIME-BARRED: Section 54EC investment made after 6 months from property transfer date; exemption statutorily disallowed.")
        }
        if (input.amountDepositedInCgas1988Rupees > 0L) {
            directives.add("CGAS 1988 COMPLIANCE: ₹${input.amountDepositedInCgas1988Rupees} must be deposited into an authorized Capital Gains Accounts Scheme (Account-A or B) with an authorized bank before the Section 139(1) due date.")
        }

        return CapitalGainsReinvestmentPlanResult(
            section54ResidentialExemptionClaimed = clampedSec54,
            section54EcBondExemptionClaimed = allowable54Ec,
            totalExemptionsClaimed = totalExemptions,
            netTaxableLtcgAfterExemptions = netLtcg,
            cgas1988DepositCompliant = input.amountDepositedInCgas1988Rupees > 0L,
            auditDirectives = directives
        )
    }
}

