package `in`.zerotax.core.taxengine.hybrid

import kotlin.math.max
import kotlin.math.min

/**
 * Section 38: Hybrid Multi-Source Income Combinator (Salaried + Part-Time Job + Freelancing 44ADA
 * + Creator/Side-Business 44AD + Casual Gig Sec 56 + Capital Gains + Rental) AND
 * Progressive Disclosure UI State Machine (ensuring complex cards are ONLY shown when needed).
 *
 * Pure JVM Kotlin — Zero Android SDK imports, Zero floating-point currency math.
 */
enum class SideIncomeNature {
    SECOND_SALARIED_JOB_SEC_192,            // Taxed under Salary Head (Sec 15); merges with primary job
    NOTIFIED_PROFESSION_44ADA_50PCT,        // IT, Software, Engineering, Medical, Legal, CA, Design, Interior, Film (50% taxable)
    DIGITAL_BUSINESS_OR_CREATOR_44AD_6PCT,  // YouTuber, Influencer, E-Commerce, Coaching, Trading (6% digital / 8% cash taxable)
    COMMISSION_OR_BROKERAGE_194H_REGULAR,   // Barred from 44AD u/s 44AD(6); taxed on net profit (Receipts - Expenses)
    CASUAL_ONE_OFF_GIG_OTHER_SOURCES_SEC_56 // Guest lecture, exam evaluation, casual honorarium <= ₹50,000 (Stays on ITR-1!)
}

data class SideIncomeStreamInput(
    val streamId: String,
    val description: String,
    val nature: SideIncomeNature,
    val grossReceiptsDigitalRupees: Long,
    val grossReceiptsCashRupees: Long = 0L,
    val actualDocumentedExpensesRupees: Long = 0L,
    val tdsDeductedRupees: Long = 0L,
    val deductorTan: String? = null
)

data class HybridIncomeOptimizationResult(
    val combinedGrossSalaryRupees: Long,
    val grossReceipts44AdaRupees: Long,
    val taxableProfit44AdaRupees: Long,
    val grossTurnover44AdDigitalRupees: Long,
    val grossTurnover44AdCashRupees: Long,
    val taxableProfit44AdRupees: Long,
    val netRegularBusinessCommissionRupees: Long,
    val sideGigAddedToOtherSourcesSec56Rupees: Long,
    val totalTaxFreeDeemedExpenseSavedRupees: Long,
    val requiresBusinessFormItr4OrItr3: Boolean,
    val plainLanguageExplanation: String
)

/**
 * Progressive Disclosure UI Flags: Every flag defaults to FALSE so a simple salaried user
 * sees ZERO clutter. Flags flip to TRUE only when underlying data actually exists.
 */
data class ProgressiveDisclosureUiState(
    val showMultiEmployerMergedBanner: Boolean = false,
    val showSideIncomeFreelanceCard: Boolean = false,
    val showHousePropertyCard: Boolean = false,
    val showCapitalGainsCard: Boolean = false,
    val showCryptoVdaCard: Boolean = false,
    val showForeignAssetRsuCard: Boolean = false,
    val showBroughtForwardLossBadge: Boolean = false,
    val showScheduleAlHighEarnerCard: Boolean = false,
    val showForm10IeaModal: Boolean = false,
    val showChallan280UpiPaymentCard: Boolean = false,
    val behindTheScenesItrFormCode: String = "ITR-1 (Sahaj)"
)

object HybridIncomeAndProgressiveDisclosureEngine {

    /**
     * Automatically classifies and optimizes all part-time, moonlighting, freelance, and creator
     * income streams alongside primary salary so the user pays the lowest legal tax with zero accounting headache.
     */
    fun optimizeHybridIncomeStreams(
        primaryGrossSalaryRupees: Long,
        sideStreams: List<SideIncomeStreamInput>,
        userPrefersSimpleItr1ForTinyGigs: Boolean = true
    ): HybridIncomeOptimizationResult {
        var totalGrossSalary = primaryGrossSalaryRupees
        var gross44Ada = 0L
        var taxable44Ada = 0L
        var digital44Ad = 0L
        var cash44Ad = 0L
        var taxable44Ad = 0L
        var netCommissionBusiness = 0L
        var casualOtherSources56 = 0L
        val notes = mutableListOf<String>()

        for (stream in sideStreams) {
            val totalStreamGross = stream.grossReceiptsDigitalRupees + stream.grossReceiptsCashRupees
            if (totalStreamGross <= 0L) continue

            when (stream.nature) {
                SideIncomeNature.SECOND_SALARIED_JOB_SEC_192 -> {
                    totalGrossSalary += totalStreamGross
                    notes.add("Merged part-time salary (₹$totalStreamGross) under Salary Head with unified Standard Deduction.")
                }
                SideIncomeNature.CASUAL_ONE_OFF_GIG_OTHER_SOURCES_SEC_56 -> {
                    val netCasual = max(0L, totalStreamGross - stream.actualDocumentedExpensesRupees)
                    casualOtherSources56 += netCasual
                    notes.add("Reported casual gig (₹$netCasual) under Other Sources (Sec 56) to keep return on simple ITR-1.")
                }
                SideIncomeNature.NOTIFIED_PROFESSION_44ADA_50PCT -> {
                    gross44Ada += totalStreamGross
                    val fiftyPct = (totalStreamGross * 5_000L + 5_000L) / 10_000L
                    taxable44Ada += fiftyPct
                    notes.add("Applied Sec 44ADA 50% Presumptive Profession: Only ₹$fiftyPct of ₹$totalStreamGross is taxed (₹${totalStreamGross - fiftyPct} tax-free).")
                }
                SideIncomeNature.DIGITAL_BUSINESS_OR_CREATOR_44AD_6PCT -> {
                    digital44Ad += stream.grossReceiptsDigitalRupees
                    cash44Ad += stream.grossReceiptsCashRupees
                    val sixPctDigital = (stream.grossReceiptsDigitalRupees * 600L + 5_000L) / 10_000L
                    val eightPctCash = (stream.grossReceiptsCashRupees * 800L + 5_000L) / 10_000L
                    val deemedProfit = sixPctDigital + eightPctCash
                    taxable44Ad += deemedProfit
                    notes.add("Applied Sec 44AD Presumptive Business (6% digital / 8% cash): Only ₹$deemedProfit of ₹$totalStreamGross is taxed.")
                }
                SideIncomeNature.COMMISSION_OR_BROKERAGE_194H_REGULAR -> {
                    val netComm = max(0L, totalStreamGross - stream.actualDocumentedExpensesRupees)
                    netCommissionBusiness += netComm
                    notes.add("Commission income (Sec 194H) taxed on net profit (₹$netComm) as Sec 44AD(6) excludes commission agents.")
                }
            }
        }

        val totalPresumptiveSaved = (gross44Ada - taxable44Ada) + ((digital44Ad + cash44Ad) - taxable44Ad)
        val needsBusinessForm = (gross44Ada > 0L) || (digital44Ad + cash44Ad > 0L) || (netCommissionBusiness > 0L)

        return HybridIncomeOptimizationResult(
            combinedGrossSalaryRupees = totalGrossSalary,
            grossReceipts44AdaRupees = gross44Ada,
            taxableProfit44AdaRupees = taxable44Ada,
            grossTurnover44AdDigitalRupees = digital44Ad,
            grossTurnover44AdCashRupees = cash44Ad,
            taxableProfit44AdRupees = taxable44Ad,
            netRegularBusinessCommissionRupees = netCommissionBusiness,
            sideGigAddedToOtherSourcesSec56Rupees = casualOtherSources56,
            totalTaxFreeDeemedExpenseSavedRupees = totalPresumptiveSaved,
            requiresBusinessFormItr4OrItr3 = needsBusinessForm,
            plainLanguageExplanation = notes.joinToString(" ")
        )
    }

    /**
     * Computes the exact Progressive Disclosure UI State so the user ONLY sees cards that apply to them,
     * while seamlessly routing between ITR-1, ITR-4, ITR-2, and ITR-3 Lite behind the scenes.
     */
    fun computeProgressiveDisclosureState(
        employerCount: Int,
        hybridResult: HybridIncomeOptimizationResult,
        hasHousePropertyInterestOrRent: Boolean,
        hasCapitalGainsLots: Boolean,
        netTaxableStcgRupees: Long,
        netLtcg112ABeforeExemptionRupees: Long,
        hasCryptoVdaTrades: Boolean,
        hasForeignAssetsOrRsu: Boolean,
        hasBroughtForwardLosses: Boolean,
        computedTotalIncomeRupees: Long,
        isOldRegimeSelected: Boolean,
        netTaxPayableAfterTdsAndTcsRupees: Long
    ): ProgressiveDisclosureUiState {
        val hasSimpleExempt112AOnly = hasCapitalGainsLots &&
                netTaxableStcgRupees == 0L &&
                netLtcg112ABeforeExemptionRupees <= 1_25_000L &&
                !hasCryptoVdaTrades &&
                !hasBroughtForwardLosses

        val needsComplexCgForm = (hasCapitalGainsLots && !hasSimpleExempt112AOnly) ||
                hasCryptoVdaTrades ||
                hasForeignAssetsOrRsu ||
                hasBroughtForwardLosses ||
                computedTotalIncomeRupees > 50_00_000L

        val selectedFormCode = when {
            hybridResult.netRegularBusinessCommissionRupees > 0L -> "ITR-3 (Auto-Managed)"
            hybridResult.requiresBusinessFormItr4OrItr3 && needsComplexCgForm -> "ITR-3 Lite (Presumptive + CG Auto-Managed)"
            hybridResult.requiresBusinessFormItr4OrItr3 -> "ITR-4 (Sugam)"
            needsComplexCgForm -> "ITR-2 (Auto-Managed)"
            else -> "ITR-1 (Sahaj)"
        }

        return ProgressiveDisclosureUiState(
            showMultiEmployerMergedBanner = employerCount >= 2,
            showSideIncomeFreelanceCard = hybridResult.requiresBusinessFormItr4OrItr3 || hybridResult.sideGigAddedToOtherSourcesSec56Rupees > 0L,
            showHousePropertyCard = hasHousePropertyInterestOrRent,
            showCapitalGainsCard = hasCapitalGainsLots,
            showCryptoVdaCard = hasCryptoVdaTrades,
            showForeignAssetRsuCard = hasForeignAssetsOrRsu,
            showBroughtForwardLossBadge = hasBroughtForwardLosses,
            showScheduleAlHighEarnerCard = computedTotalIncomeRupees > 50_00_000L,
            showForm10IeaModal = hybridResult.requiresBusinessFormItr4OrItr3 && isOldRegimeSelected,
            showChallan280UpiPaymentCard = netTaxPayableAfterTdsAndTcsRupees > 0L,
            behindTheScenesItrFormCode = selectedFormCode
        )
    }
}

