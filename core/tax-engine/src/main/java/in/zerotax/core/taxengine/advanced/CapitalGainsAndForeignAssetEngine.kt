package `in`.zerotax.core.taxengine.advanced

import `in`.zerotax.core.model.Equity112ALot
import `in`.zerotax.core.model.ImmovablePropertyLtcgLot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class PropertyLtcgTaxComparisonResult(
    val gainWithoutIndexation: Long,
    val taxAt12Point5PercentNoIndexation: Long,
    val gainWithCiiIndexation: Long,
    val taxAt20PercentWithIndexation: Long,
    val optimalTaxPayableForResidentIndividual: Long
)

data class ImmovableLtcgResult(
    val grossLtcgRupees: Long,
    val exemptCapitalGainsUs54: Long,
    val netTaxableLtcgRupees: Long
)

data class CapitalGainsSetOffSummary(
    val netTaxableStcg15Or20Percent: Long,
    val netTaxableLtcg112A: Long,
    val carriedForwardStclToNext8Years: Long,
    val carriedForwardLtclToNext8Years: Long
)

data class CapitalGainsExemptionPlan(
    val grossLtcg: Long,
    val allowedSec54ResidentialExemption: Long,
    val allowedSec54EcBondExemption: Long,
    val netTaxableLtcgAfterExemptions: Long,
    val sec54EcDeadlineIso: String,
    val cgasDepositAlert: String?
)

data class VdaTokenTrade(
    val tokenSymbol: String,
    val saleConsideration: Long,
    val costOfAcquisition: Long,
    val transferExpenses: Long = 0L
)

data class ScheduleVdaComputationResult(
    val grossVdaGainsTaxable30Percent: Long,
    val grossVdaLossesDisallowedForSetOff: Long,
    val taxLiability30Percent: Long,
    val cessAmount: Long,
    val totalVdaTaxPayable: Long,
    val tdsCredit194SAllowed: Long,
    val netPayableAfterTds: Long
)

data class ShareBuybackTaxResult(
    val deemedDividendScheduleOs: Long,
    val capitalLossScheduleCgAndCfl: Long,
    val tdsWithheld194: Long
)

object CapitalGainsAndForeignAssetEngine {
    private const val JAN_31_2018_EPOCH_DAY = 17562L // 2018-01-31

    fun computeGrandfathered112AGain(lot: Equity112ALot): Long {
        return computeGrandfathered112AGain(
            acquisitionDateEpochDay = lot.acquisitionDateEpochDay,
            actualCostTotal = lot.actualCostTotal,
            fmvJan31_2018Total = lot.fmvJan31_2018Total,
            saleConsiderationTotal = lot.saleConsiderationTotal,
            transferExpenses = lot.transferExpenses
        )
    }

    fun computeGrandfathered112AGain(
        acquisitionDateEpochDay: Long,
        actualCostTotal: Long,
        fmvJan31_2018Total: Long,
        saleConsiderationTotal: Long,
        transferExpenses: Long
    ): Long {
        val effectiveCost = if (acquisitionDateEpochDay <= JAN_31_2018_EPOCH_DAY) {
            max(actualCostTotal, min(fmvJan31_2018Total, saleConsiderationTotal))
        } else actualCostTotal
        val gain = saleConsiderationTotal - effectiveCost - transferExpenses
        return max(0L, gain)
    }

    fun computeImmovablePropertyLtcgTax(lot: ImmovablePropertyLtcgLot): PropertyLtcgTaxComparisonResult {
        val netSaleConsideration = max(0L, lot.saleConsiderationOrStampValue50C - lot.transferExpenses)
        val unindexedCost = lot.purchaseCostRupees + lot.improvementCostRupees
        val rawGainNoIndex = max(0L, netSaleConsideration - unindexedCost - lot.section54ReinvestmentExemption)
        val tax12Point5 = (rawGainNoIndex * 0.125).roundToLong()

        val ciiAcq = max(1, lot.acquisitionFyCii)
        val indexedCost = ((lot.purchaseCostRupees.toDouble() * lot.transferFyCii) / ciiAcq).roundToLong()
        val rawGainIndexed = max(0L, netSaleConsideration - indexedCost - lot.improvementCostRupees - lot.section54ReinvestmentExemption)
        val tax20 = (rawGainIndexed * 0.20).roundToLong()

        val optimalTax = if (lot.acquiredBeforeJuly23_2024) min(tax12Point5, tax20) else tax12Point5

        return PropertyLtcgTaxComparisonResult(
            gainWithoutIndexation = rawGainNoIndex,
            taxAt12Point5PercentNoIndexation = tax12Point5,
            gainWithCiiIndexation = rawGainIndexed,
            taxAt20PercentWithIndexation = tax20,
            optimalTaxPayableForResidentIndividual = optimalTax
        )
    }

    fun computeImmovablePropertyLtcgTax(
        grossLtcg: Long,
        section54ReinvestmentExemption: Long
    ): ImmovableLtcgResult {
        val allowedExemption = minOf(grossLtcg, section54ReinvestmentExemption)
        return ImmovableLtcgResult(
            grossLtcgRupees = grossLtcg,
            exemptCapitalGainsUs54 = allowedExemption,
            netTaxableLtcgRupees = grossLtcg - allowedExemption
        )
    }

    fun computeSection54And54EcExemptionPlan(
        grossLtcg: Long,
        saleDateIso: String,
        reinvestedInHouseSec54: Long,
        investedIn54EcBonds: Long
    ): CapitalGainsExemptionPlan {
        val allowed54 = min(10_00_00_000L, max(0L, reinvestedInHouseSec54))
        val allowed54Ec = min(50_00_000L, max(0L, investedIn54EcBonds))
        val totalExemption = min(grossLtcg, allowed54 + allowed54Ec)
        val netLtcg = max(0L, grossLtcg - totalExemption)

        val saleDate = try { java.time.LocalDate.parse(saleDateIso) } catch (e: Exception) { java.time.LocalDate.now() }
        val deadline54Ec = saleDate.plusMonths(6).toString()

        val cgasAlert = if (grossLtcg > totalExemption && reinvestedInHouseSec54 < grossLtcg) {
            "Deposit unutilized capital gains of ₹${grossLtcg - totalExemption} in PSU Bank Capital Gains Account Scheme (CGAS 1988) before July 31 due date."
        } else null

        return CapitalGainsExemptionPlan(
            grossLtcg = grossLtcg,
            allowedSec54ResidentialExemption = allowed54,
            allowedSec54EcBondExemption = allowed54Ec,
            netTaxableLtcgAfterExemptions = netLtcg,
            sec54EcDeadlineIso = deadline54Ec,
            cgasDepositAlert = cgasAlert
        )
    }

    fun applyCapitalLossSetOff(
        currentYearStcg: Long,
        currentYearStcl: Long,
        currentYearLtcg: Long,
        currentYearLtcl: Long,
        broughtForwardStcl: Long = 0L,
        broughtForwardLtcl: Long = 0L
    ): CapitalGainsSetOffSummary {
        // Step 1: Intra-head set-off for Current Year
        // STCL can set off against both STCG and LTCG (Sec 70(2))
        // LTCL can set off ONLY against LTCG (Sec 70(3))
        var remLtcg = max(0L, currentYearLtcg - currentYearLtcl)
        var unabsorbedLtcl = if (currentYearLtcl > currentYearLtcg) currentYearLtcl - currentYearLtcg else 0L

        var remStcg = currentYearStcg
        var unabsorbedStcl = currentYearStcl

        if (unabsorbedStcl > 0L) {
            val offsetAgainstStcg = min(remStcg, unabsorbedStcl)
            remStcg -= offsetAgainstStcg
            unabsorbedStcl -= offsetAgainstStcg

            if (unabsorbedStcl > 0L && remLtcg > 0L) {
                val offsetAgainstLtcg = min(remLtcg, unabsorbedStcl)
                remLtcg -= offsetAgainstLtcg
                unabsorbedStcl -= offsetAgainstLtcg
            }
        }

        // Step 2: Brought Forward Loss Set-Off (Sec 74)
        if (broughtForwardStcl > 0L) {
            val offsetAgainstStcg = min(remStcg, broughtForwardStcl)
            remStcg -= offsetAgainstStcg
            val remainingBfStcl = broughtForwardStcl - offsetAgainstStcg

            if (remainingBfStcl > 0L && remLtcg > 0L) {
                val offsetAgainstLtcg = min(remLtcg, remainingBfStcl)
                remLtcg -= offsetAgainstLtcg
                unabsorbedStcl += (remainingBfStcl - offsetAgainstLtcg)
            } else {
                unabsorbedStcl += remainingBfStcl
            }
        }

        if (broughtForwardLtcl > 0L) {
            val offsetAgainstLtcg = min(remLtcg, broughtForwardLtcl)
            remLtcg -= offsetAgainstLtcg
            unabsorbedLtcl += (broughtForwardLtcl - offsetAgainstLtcg)
        }

        return CapitalGainsSetOffSummary(
            netTaxableStcg15Or20Percent = remStcg,
            netTaxableLtcg112A = remLtcg,
            carriedForwardStclToNext8Years = unabsorbedStcl,
            carriedForwardLtclToNext8Years = unabsorbedLtcl
        )
    }

    fun computeScheduleVda115BBH(
        tokens: List<VdaTokenTrade>,
        tdsCredits194S: Long = 0L
    ): ScheduleVdaComputationResult {
        var totalGains = 0L
        var totalLosses = 0L

        for (token in tokens) {
            val netConsideration = max(0L, token.saleConsideration - token.transferExpenses)
            val pnl = netConsideration - token.costOfAcquisition
            if (pnl > 0L) {
                totalGains += pnl
            } else {
                // Section 115BBH(2)(b): Strict disallowance of VDA loss set-off across tokens
                totalLosses += abs(pnl)
            }
        }

        val baseTax = (totalGains * 0.30).roundToLong()
        val cess = (baseTax * 0.04).roundToLong()
        val totalTax = baseTax + cess
        val netPayable = max(0L, totalTax - tdsCredits194S)

        return ScheduleVdaComputationResult(
            grossVdaGainsTaxable30Percent = totalGains,
            grossVdaLossesDisallowedForSetOff = totalLosses,
            taxLiability30Percent = baseTax,
            cessAmount = cess,
            totalVdaTaxPayable = totalTax,
            tdsCredit194SAllowed = min(tdsCredits194S, totalTax),
            netPayableAfterTds = netPayable
        )
    }

    fun computeShareBuybackNewTaxRule(
        buybackProceeds: Long,
        originalCostOfAcquisition: Long,
        tdsDeducted194: Long = 0L
    ): ShareBuybackTaxResult {
        // Section 2(22)(f): Buyback proceeds treated 100% as deemed dividend in Schedule OS
        // Section 46A: Original cost of acquisition recorded as Capital Loss
        return ShareBuybackTaxResult(
            deemedDividendScheduleOs = buybackProceeds,
            capitalLossScheduleCgAndCfl = originalCostOfAcquisition,
            tdsWithheld194 = tdsDeducted194
        )
    }

    fun computeSgbRedemptionExemption(isRbiRedemption: Boolean, capitalGain: Long): Long {
        // Section 47(viic): SGB redemption with RBI is 100% exempt from capital gains
        return if (isRbiRedemption) 0L else capitalGain
    }

    fun computeForm67ForeignTaxCredit(
        foreignDividendUsd: Double,
        foreignTaxWithheldUsd: Double,
        precedingMonthEndSbiTtbr: Double,
        indianEffectiveTaxRateOnTotalIncome: Double
    ): Pair<Long, Long> {
        val grossDividendInr = (foreignDividendUsd * precedingMonthEndSbiTtbr).roundToLong()
        val foreignTaxPaidInr = (foreignTaxWithheldUsd * precedingMonthEndSbiTtbr).roundToLong()
        val indianTaxOnForeignIncome = (grossDividendInr * indianEffectiveTaxRateOnTotalIncome).roundToLong()
        return grossDividendInr to min(foreignTaxPaidInr, indianTaxOnForeignIncome)
    }
}
