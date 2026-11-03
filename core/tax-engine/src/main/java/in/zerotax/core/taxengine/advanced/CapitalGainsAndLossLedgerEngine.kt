package in.zerotax.core.taxengine.advanced

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Multi-Broker Capital Gains & 8-Year Loss Carry-Forward Ledger Engine
 *
 * Implements deterministic statutory computation for:
 * 1. Finance (No. 2) Act, 2024 revised tax rates:
 *    - Section 111A STCG (Equity/STT): Flat 20.0%
 *    - Section 112A LTCG (Equity/STT): Flat 12.5% on gains exceeding ₹1,25,000 statutory exemption threshold
 *    - Section 112 LTCG (Unlisted/Other): Flat 12.5% without indexation
 * 2. Section 70 (Intra-Head Set-off):
 *    - STCL can be set off against BOTH STCG and LTCG.
 *    - LTCL can ONLY be set off against LTCG.
 * 3. Schedule CYLA & CFL (Section 74):
 *    - 8-Assessment-Year historical loss tracking with expiration year calculation.
 *    - Section 139(1) mandatory timely filing check for valid carry-forward rights.
 */
object CapitalGainsAndLossLedgerEngine {

    const val SECTION_112A_EXEMPTION_LIMIT = 125000L // ₹1.25 Lakhs per FY
    const val STCG_111A_TAX_RATE = 0.20 // 20%
    const val LTCG_112A_TAX_RATE = 0.125 // 12.5%
    const val LTCG_112_TAX_RATE = 0.125 // 12.5%
    const val CESS_RATE = 0.04 // 4% Health & Education Cess

    enum class LossType {
        STCL, // Short Term Capital Loss
        LTCL  // Long Term Capital Loss
    }

    data class BroughtForwardLoss(
        val assessmentYear: String,
        val lossType: LossType,
        val originalAmount: Long,
        val previouslySetOff: Long,
        val filedBeforeDueDate: Boolean = true
    ) {
        val unabsorbedBalance: Long
            get() = max(0L, originalAmount - previouslySetOff)

        val expiryAssessmentYear: String
            get() {
                val startAyYear = assessmentYear.take(4).toIntOrNull() ?: 2026
                val expiryAyYear = startAyYear + 8
                val nextYearSuffix = (expiryAyYear + 1).toString().takeLast(2)
                return "AY $expiryAyYear-$nextYearSuffix"
            }
    }

    data class CapitalGainsPortfolioInput(
        val stcg111AListedGain: Long = 0L,
        val stcl111AListedLoss: Long = 0L,
        val ltcg112AListedGain: Long = 0L,
        val ltcl112AListedLoss: Long = 0L,
        val otherStcgGain: Long = 0L,
        val otherStclLoss: Long = 0L,
        val otherLtcg112Gain: Long = 0L,
        val otherLtcl112Loss: Long = 0L,
        val broughtForwardLosses: List<BroughtForwardLoss> = emptyList()
    )

    data class LossSetOffStep(
        val lossSource: String,
        val setOffAgainstGain: String,
        val setOffAmount: Long,
        val statutorySection: String
    )

    data class LossBucketStatus(
        val assessmentYear: String,
        val lossType: LossType,
        val openingUnabsorbed: Long,
        val utilizedThisYear: Long,
        val closingCarryForward: Long,
        val expiryYear: String,
        val isLapsed: Boolean
    )

    data class CapitalGainsComputationResult(
        val grossStcg111A: Long,
        val grossLtcg112A: Long,
        val grossOtherStcg: Long,
        val grossOtherLtcg: Long,
        val totalCurrentYearStcl: Long,
        val totalCurrentYearLtcl: Long,
        val setOffAuditLog: List<LossSetOffStep>,
        val netTaxableStcg111A: Long,
        val netTaxableOtherStcg: Long,
        val netTaxableLtcg112AExemptExceeded: Long,
        val exemption112AClaimed: Long,
        val netTaxableOtherLtcg112: Long,
        val taxOnStcg111A: Long,
        val taxOnLtcg112A: Long,
        val taxOnOtherLtcg112: Long,
        val totalSpecialTaxBeforeCess: Long,
        val cessAmount: Long,
        val totalCapitalGainsTax: Long,
        val carriedForwardToFutureAy: List<LossBucketStatus>,
        val totalUnabsorbedLossCarriedForward: Long
    )

    fun computeCapitalGainsAndLosses(
        input: CapitalGainsPortfolioInput,
        currentAssessmentYear: String = "2026-27"
    ): CapitalGainsComputationResult {
        var currentStcg111A = max(0L, input.stcg111AListedGain)
        var currentOtherStcg = max(0L, input.otherStcgGain)
        var currentLtcg112A = max(0L, input.ltcg112AListedGain)
        var currentOtherLtcg = max(0L, input.otherLtcg112Gain)

        var currentStcl = max(0L, input.stcl111AListedLoss) + max(0L, input.otherStclLoss)
        var currentLtcl = max(0L, input.ltcl112AListedLoss) + max(0L, input.otherLtcl112Loss)

        val setOffAuditLog = mutableListOf<LossSetOffStep>()

        // -------------------------------------------------------------
        // STEP 1: Intra-Head Set-off u/s 70 (Current Year Losses)
        // -------------------------------------------------------------
        // A) LTCL can only be set off against LTCG (112A and 112)
        if (currentLtcl > 0L && currentLtcg112A > 0L) {
            val absorbed = min(currentLtcl, currentLtcg112A)
            currentLtcl -= absorbed
            currentLtcg112A -= absorbed
            setOffAuditLog.add(LossSetOffStep("CY LTCL", "CY LTCG Sec 112A", absorbed, "Sec 70(3)"))
        }
        if (currentLtcl > 0L && currentOtherLtcg > 0L) {
            val absorbed = min(currentLtcl, currentOtherLtcg)
            currentLtcl -= absorbed
            currentOtherLtcg -= absorbed
            setOffAuditLog.add(LossSetOffStep("CY LTCL", "CY LTCG Sec 112", absorbed, "Sec 70(3)"))
        }

        // B) STCL can be set off against STCG first, then LTCG
        if (currentStcl > 0L && currentStcg111A > 0L) {
            val absorbed = min(currentStcl, currentStcg111A)
            currentStcl -= absorbed
            currentStcg111A -= absorbed
            setOffAuditLog.add(LossSetOffStep("CY STCL", "CY STCG Sec 111A", absorbed, "Sec 70(2)"))
        }
        if (currentStcl > 0L && currentOtherStcg > 0L) {
            val absorbed = min(currentStcl, currentOtherStcg)
            currentStcl -= absorbed
            currentOtherStcg -= absorbed
            setOffAuditLog.add(LossSetOffStep("CY STCL", "CY Other STCG", absorbed, "Sec 70(2)"))
        }
        if (currentStcl > 0L && currentLtcg112A > 0L) {
            val absorbed = min(currentStcl, currentLtcg112A)
            currentStcl -= absorbed
            currentLtcg112A -= absorbed
            setOffAuditLog.add(LossSetOffStep("CY STCL", "CY LTCG Sec 112A", absorbed, "Sec 70(2)"))
        }
        if (currentStcl > 0L && currentOtherLtcg > 0L) {
            val absorbed = min(currentStcl, currentOtherLtcg)
            currentStcl -= absorbed
            currentOtherLtcg -= absorbed
            setOffAuditLog.add(LossSetOffStep("CY STCL", "CY LTCG Sec 112", absorbed, "Sec 70(2)"))
        }

        // -------------------------------------------------------------
        // STEP 2: Brought Forward Loss Set-off u/s 74 (Schedule BFLA)
        // -------------------------------------------------------------
        val carriedForwardBuckets = mutableListOf<LossBucketStatus>()

        for (bfLoss in input.broughtForwardLosses) {
            var unabsorbed = bfLoss.unabsorbedBalance
            var utilized = 0L

            if (!bfLoss.filedBeforeDueDate) {
                // Section 80 / 139(3): Loss cannot be carried forward if return was filed after due date
                carriedForwardBuckets.add(
                    LossBucketStatus(
                        assessmentYear = bfLoss.assessmentYear,
                        lossType = bfLoss.lossType,
                        openingUnabsorbed = unabsorbed,
                        utilizedThisYear = 0L,
                        closingCarryForward = 0L,
                        expiryYear = bfLoss.expiryAssessmentYear,
                        isLapsed = true
                    )
                )
                continue
            }

            if (bfLoss.lossType == LossType.LTCL) {
                // B/F LTCL can only be set off against LTCG
                if (unabsorbed > 0L && currentLtcg112A > 0L) {
                    val abs = min(unabsorbed, currentLtcg112A)
                    unabsorbed -= abs
                    currentLtcg112A -= abs
                    utilized += abs
                    setOffAuditLog.add(LossSetOffStep("B/F LTCL (${bfLoss.assessmentYear})", "CY LTCG Sec 112A", abs, "Sec 74(1)(b)"))
                }
                if (unabsorbed > 0L && currentOtherLtcg > 0L) {
                    val abs = min(unabsorbed, currentOtherLtcg)
                    unabsorbed -= abs
                    currentOtherLtcg -= abs
                    utilized += abs
                    setOffAuditLog.add(LossSetOffStep("B/F LTCL (${bfLoss.assessmentYear})", "CY LTCG Sec 112", abs, "Sec 74(1)(b)"))
                }
            } else {
                // B/F STCL can be set off against STCG first, then LTCG
                if (unabsorbed > 0L && currentStcg111A > 0L) {
                    val abs = min(unabsorbed, currentStcg111A)
                    unabsorbed -= abs
                    currentStcg111A -= abs
                    utilized += abs
                    setOffAuditLog.add(LossSetOffStep("B/F STCL (${bfLoss.assessmentYear})", "CY STCG Sec 111A", abs, "Sec 74(1)(a)"))
                }
                if (unabsorbed > 0L && currentOtherStcg > 0L) {
                    val abs = min(unabsorbed, currentOtherStcg)
                    unabsorbed -= abs
                    currentOtherStcg -= abs
                    utilized += abs
                    setOffAuditLog.add(LossSetOffStep("B/F STCL (${bfLoss.assessmentYear})", "CY Other STCG", abs, "Sec 74(1)(a)"))
                }
                if (unabsorbed > 0L && currentLtcg112A > 0L) {
                    val abs = min(unabsorbed, currentLtcg112A)
                    unabsorbed -= abs
                    currentLtcg112A -= abs
                    utilized += abs
                    setOffAuditLog.add(LossSetOffStep("B/F STCL (${bfLoss.assessmentYear})", "CY LTCG Sec 112A", abs, "Sec 74(1)(a)"))
                }
                if (unabsorbed > 0L && currentOtherLtcg > 0L) {
                    val abs = min(unabsorbed, currentOtherLtcg)
                    unabsorbed -= abs
                    currentOtherLtcg -= abs
                    utilized += abs
                    setOffAuditLog.add(LossSetOffStep("B/F STCL (${bfLoss.assessmentYear})", "CY LTCG Sec 112", abs, "Sec 74(1)(a)"))
                }
            }

            carriedForwardBuckets.add(
                LossBucketStatus(
                    assessmentYear = bfLoss.assessmentYear,
                    lossType = bfLoss.lossType,
                    openingUnabsorbed = bfLoss.unabsorbedBalance,
                    utilizedThisYear = utilized,
                    closingCarryForward = unabsorbed,
                    expiryYear = bfLoss.expiryAssessmentYear,
                    isLapsed = false
                )
            )
        }

        // Add Current Year unabsorbed losses to the carry forward ledger (Schedule CFL)
        if (currentStcl > 0L) {
            val currAyStart = currentAssessmentYear.take(4).toIntOrNull() ?: 2026
            val expiry = "AY ${currAyStart + 8}-${(currAyStart + 9).toString().takeLast(2)}"
            carriedForwardBuckets.add(
                LossBucketStatus(
                    assessmentYear = "AY $currentAssessmentYear",
                    lossType = LossType.STCL,
                    openingUnabsorbed = currentStcl,
                    utilizedThisYear = 0L,
                    closingCarryForward = currentStcl,
                    expiryYear = expiry,
                    isLapsed = false
                )
            )
        }
        if (currentLtcl > 0L) {
            val currAyStart = currentAssessmentYear.take(4).toIntOrNull() ?: 2026
            val expiry = "AY ${currAyStart + 8}-${(currAyStart + 9).toString().takeLast(2)}"
            carriedForwardBuckets.add(
                LossBucketStatus(
                    assessmentYear = "AY $currentAssessmentYear",
                    lossType = LossType.LTCL,
                    openingUnabsorbed = currentLtcl,
                    utilizedThisYear = 0L,
                    closingCarryForward = currentLtcl,
                    expiryYear = expiry,
                    isLapsed = false
                )
            )
        }

        // -------------------------------------------------------------
        // STEP 3: Tax Computation with Sec 112A Exemption Threshold
        // -------------------------------------------------------------
        // Section 112A statutory ₹1.25 Lakhs basic exemption
        val exemption112AClaimed = min(SECTION_112A_EXEMPTION_LIMIT, currentLtcg112A)
        val taxableLtcg112A = max(0L, currentLtcg112A - exemption112AClaimed)

        val tax111A = (currentStcg111A * STCG_111A_TAX_RATE).roundToLong()
        val tax112A = (taxableLtcg112A * LTCG_112A_TAX_RATE).roundToLong()
        val tax112 = (currentOtherLtcg * LTCG_112_TAX_RATE).roundToLong()

        val subtotalTax = tax111A + tax112A + tax112
        val cess = (subtotalTax * CESS_RATE).roundToLong()
        val finalTax = subtotalTax + cess

        val totalUnabsorbed = carriedForwardBuckets.filter { !it.isLapsed }.sumOf { it.closingCarryForward }

        return CapitalGainsComputationResult(
            grossStcg111A = input.stcg111AListedGain,
            grossLtcg112A = input.ltcg112AListedGain,
            grossOtherStcg = input.otherStcgGain,
            grossOtherLtcg = input.otherLtcg112Gain,
            totalCurrentYearStcl = input.stcl111AListedLoss + input.otherStclLoss,
            totalCurrentYearLtcl = input.ltcl112AListedLoss + input.otherLtcl112Loss,
            setOffAuditLog = setOffAuditLog,
            netTaxableStcg111A = currentStcg111A,
            netTaxableOtherStcg = currentOtherStcg,
            netTaxableLtcg112AExemptExceeded = taxableLtcg112A,
            exemption112AClaimed = exemption112AClaimed,
            netTaxableOtherLtcg112 = currentOtherLtcg,
            taxOnStcg111A = tax111A,
            taxOnLtcg112A = tax112A,
            taxOnOtherLtcg112 = tax112,
            totalSpecialTaxBeforeCess = subtotalTax,
            cessAmount = cess,
            totalCapitalGainsTax = finalTax,
            carriedForwardToFutureAy = carriedForwardBuckets,
            totalUnabsorbedLossCarriedForward = totalUnabsorbed
        )
    }
}
