package in.zerotax.core.taxengine.calculator

import in.zerotax.core.model.AssessmentYearProfile
import in.zerotax.core.model.CitizenCategory
import in.zerotax.core.model.DonationCategory
import in.zerotax.core.model.Section80GDonationInput
import in.zerotax.core.model.TaxRegime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class TaxpayerFinancialInput(
    val citizenCategory: CitizenCategory = CitizenCategory.REGULAR_BELOW_60,
    val isResidentIndividual: Boolean = true,
    val grossSalary: Long = 0L,
    val exemptHraAndAllowancesSec10: Long = 0L,
    val professionalTaxSec16iii: Long = 0L,
    val selfOccupiedHomeLoanInterestSec24b: Long = 0L,
    val savingsBankInterest: Long = 0L,
    val fixedDepositInterest: Long = 0L,
    val familyPensionReceived: Long = 0L,
    val section80CInvestments: Long = 0L,
    val section80DSelfAndFamily: Long = 0L,
    val section80DParentsSenior: Long = 0L,
    val section80CCD1BSelfNps: Long = 0L,
    val section80CCD2EmployerNps: Long = 0L,
    val basicPlusDaForNps: Long = 0L,
    val isCentralOrStateGovtEmployee: Boolean = false,
    val donations80G: List<Section80GDonationInput> = emptyList(),
    val presumptiveGrossReceipts44ADA: Long = 0L,
    val presumptiveDeclaredProfitRatePercent: Double = 50.0,
    val netTaxableStcg111ARupees: Long = 0L,
    val netTaxableLtcg112ARupees: Long = 0L,
    val assessmentYearProfile: AssessmentYearProfile = AssessmentYearProfile.AY_2025_26
)

data class RegimeComputationResult(
    val regime: TaxRegime,
    val grossTotalIncome: Long,
    val standardDeduction: Long,
    val totalChapterVIADeductions: Long,
    val netTaxableNormalIncomeRounded288A: Long,
    val netTaxableSpecialRateIncome: Long,
    val baseSlabTaxOnNormalIncome: Long,
    val specialRateTax111AAnd112A: Long,
    val rebate87A: Long,
    val marginalRelief87A: Long,
    val taxAfterRebateAndRelief: Long,
    val surchargeAfterMarginalRelief: Long,
    val healthAndEducationCess: Long,
    val totalTaxLiabilityRounded288B: Long
)

object DeterministicIndianTaxEngine {
    private const val NEW_REGIME_STD_DEDUCTION = 75_000L
    private const val OLD_REGIME_STD_DEDUCTION = 50_000L
    private const val NEW_REGIME_87A_THRESHOLD = 7_00_000L
    private const val NEW_REGIME_87A_MAX_REBATE = 25_000L
    private const val OLD_REGIME_87A_THRESHOLD = 5_00_000L
    private const val OLD_REGIME_87A_MAX_REBATE = 12_500L
    private const val SEC_112A_EXEMPT_THRESHOLD = 1_25_000L

    fun computeNewRegime(input: TaxpayerFinancialInput): RegimeComputationResult {
        val stdDed = min(input.grossSalary, NEW_REGIME_STD_DEDUCTION)
        val netSalary = max(0L, input.grossSalary - stdDed)
        val presumptiveIncome = (input.presumptiveGrossReceipts44ADA * (input.presumptiveDeclaredProfitRatePercent / 100.0)).roundToLong()
        val familyPensionDed = min((input.familyPensionReceived / 3.0).roundToLong(), 25_000L)
        val netFamilyPension = max(0L, input.familyPensionReceived - familyPensionDed)
        val otherSources = input.savingsBankInterest + input.fixedDepositInterest + netFamilyPension
        val normalGti = netSalary + presumptiveIncome + otherSources
        val specialIncome = input.netTaxableStcg111ARupees + input.netTaxableLtcg112ARupees

        val max80CCD2 = (input.basicPlusDaForNps * 0.14).roundToLong()
        val allowed80CCD2 = min(input.section80CCD2EmployerNps, max80CCD2)
        val normalTaxable288A = roundToNearestTen(max(0L, normalGti - allowed80CCD2))
        val totalIncome288A = roundToNearestTen(normalTaxable288A + specialIncome)

        val baseNormalTax = calculateProgressiveSlabs(
            income = normalTaxable288A,
            brackets = if (input.assessmentYearProfile == AssessmentYearProfile.AY_2026_27) {
                listOf(
                    4_00_000L to 0.00, 8_00_000L to 0.05, 12_00_000L to 0.10,
                    16_00_000L to 0.15, 20_00_000L to 0.20, 24_00_000L to 0.25, Long.MAX_VALUE to 0.30
                )
            } else {
                listOf(
                    3_00_000L to 0.00, 7_00_000L to 0.05, 10_00_000L to 0.10,
                    12_00_000L to 0.15, 15_00_000L to 0.20, Long.MAX_VALUE to 0.30
                )
            }
        )

        val stcgTax = (input.netTaxableStcg111ARupees * 0.20).roundToLong()
        val ltcgTax = (max(0L, input.netTaxableLtcg112ARupees - SEC_112A_EXEMPT_THRESHOLD) * 0.125).roundToLong()
        val specialTax = stcgTax + ltcgTax

        val rebateThreshold87A = if (input.assessmentYearProfile == AssessmentYearProfile.AY_2026_27) 12_00_000L else NEW_REGIME_87A_THRESHOLD
        val maxRebate87A = if (input.assessmentYearProfile == AssessmentYearProfile.AY_2026_27) 60_000L else NEW_REGIME_87A_MAX_REBATE
        val rebate87A = if (input.isResidentIndividual && totalIncome288A <= rebateThreshold87A) {
            min(baseNormalTax + stcgTax, maxRebate87A)
        } else 0L

        val normalTaxAfterRebate = max(0L, baseNormalTax + stcgTax - rebate87A)
        val marginalRelief87A = if (input.isResidentIndividual && normalTaxable288A > rebateThreshold87A) {
            val excessOverThreshold = normalTaxable288A - rebateThreshold87A
            if (normalTaxAfterRebate > excessOverThreshold) normalTaxAfterRebate - excessOverThreshold else 0L
        } else 0L

        val totalTaxBeforeSurcharge = normalTaxAfterRebate - marginalRelief87A + ltcgTax
        val surcharge = computeSurchargeWithMarginalRelief(totalIncome288A, totalTaxBeforeSurcharge, true)
        val cess = ((totalTaxBeforeSurcharge + surcharge) * 0.04).roundToLong()
        val finalTax288B = roundToNearestTen(totalTaxBeforeSurcharge + surcharge + cess)

        return RegimeComputationResult(
            regime = TaxRegime.NEW_REGIME_115BAC,
            grossTotalIncome = normalGti + specialIncome,
            standardDeduction = stdDed,
            totalChapterVIADeductions = allowed80CCD2,
            netTaxableNormalIncomeRounded288A = normalTaxable288A,
            netTaxableSpecialRateIncome = specialIncome,
            baseSlabTaxOnNormalIncome = baseNormalTax,
            specialRateTax111AAnd112A = specialTax,
            rebate87A = rebate87A,
            marginalRelief87A = marginalRelief87A,
            taxAfterRebateAndRelief = totalTaxBeforeSurcharge,
            surchargeAfterMarginalRelief = surcharge,
            healthAndEducationCess = cess,
            totalTaxLiabilityRounded288B = finalTax288B
        )
    }

    fun computeOldRegime(input: TaxpayerFinancialInput): RegimeComputationResult {
        val stdDed = min(input.grossSalary, OLD_REGIME_STD_DEDUCTION)
        val netSalary = max(0L, input.grossSalary - input.exemptHraAndAllowancesSec10 - input.professionalTaxSec16iii - stdDed)
        val lossFromHouseProperty = min(input.selfOccupiedHomeLoanInterestSec24b, 2_00_000L)
        val presumptiveIncome = (input.presumptiveGrossReceipts44ADA * (input.presumptiveDeclaredProfitRatePercent / 100.0)).roundToLong()
        val familyPensionDed = min((input.familyPensionReceived / 3.0).roundToLong(), 15_000L)
        val netFamilyPension = max(0L, input.familyPensionReceived - familyPensionDed)
        val otherSources = input.savingsBankInterest + input.fixedDepositInterest + netFamilyPension
        val normalGtiBeforeLoss = netSalary + presumptiveIncome + otherSources
        val normalGti = max(0L, normalGtiBeforeLoss - lossFromHouseProperty)
        val specialIncome = input.netTaxableStcg111ARupees + input.netTaxableLtcg112ARupees

        val sec80C = min(input.section80CInvestments, 1_50_000L)
        val maxSelfD = if (input.citizenCategory != CitizenCategory.REGULAR_BELOW_60) 50_000L else 25_000L
        val sec80DSelf = min(input.section80DSelfAndFamily, maxSelfD)
        val sec80DParents = min(input.section80DParentsSenior, 50_000L)
        val sec80CCD1B = min(input.section80CCD1BSelfNps, 50_000L)
        val npsPercent = if (input.isCentralOrStateGovtEmployee) 0.14 else 0.10
        val sec80CCD2 = min(input.section80CCD2EmployerNps, (input.basicPlusDaForNps * npsPercent).roundToLong())
        val sec80TTAorTTB = if (input.citizenCategory == CitizenCategory.REGULAR_BELOW_60) {
            min(input.savingsBankInterest, 10_000L)
        } else {
            min(input.savingsBankInterest + input.fixedDepositInterest, 50_000L)
        }
        val priorChapterVIA = sec80C + sec80DSelf + sec80DParents + sec80CCD1B + sec80CCD2 + sec80TTAorTTB
        val adjustedGtiFor80G = max(0L, normalGti - priorChapterVIA)
        val sec80G = computeSection80GDeduction(input.donations80G, adjustedGtiFor80G)
        val totalChapterVIA = priorChapterVIA + sec80G

        val normalTaxable288A = roundToNearestTen(max(0L, normalGti - totalChapterVIA))
        val totalIncome288A = roundToNearestTen(normalTaxable288A + specialIncome)

        val exemptionLimit = when (input.citizenCategory) {
            CitizenCategory.REGULAR_BELOW_60 -> 2_50_000L
            CitizenCategory.SENIOR_60_TO_79 -> 3_00_000L
            CitizenCategory.SUPER_SENIOR_80_PLUS -> 5_00_000L
        }

        val baseNormalTax = calculateProgressiveSlabs(
            income = normalTaxable288A,
            brackets = listOf(
                exemptionLimit to 0.00,
                5_00_000L to 0.05,
                10_00_000L to 0.20,
                Long.MAX_VALUE to 0.30
            )
        )

        val stcgTax = (input.netTaxableStcg111ARupees * 0.15).roundToLong()
        val ltcgTax = (max(0L, input.netTaxableLtcg112ARupees - 1_00_000L) * 0.10).roundToLong()
        val specialTax = stcgTax + ltcgTax

        val rebate87A = if (input.isResidentIndividual && totalIncome288A <= OLD_REGIME_87A_THRESHOLD) {
            min(baseNormalTax + stcgTax, OLD_REGIME_87A_MAX_REBATE)
        } else 0L

        val taxAfterRebate = max(0L, baseNormalTax + specialTax - rebate87A)
        val surcharge = computeSurchargeWithMarginalRelief(totalIncome288A, taxAfterRebate, false)
        val cess = ((taxAfterRebate + surcharge) * 0.04).roundToLong()
        val finalTax288B = roundToNearestTen(taxAfterRebate + surcharge + cess)

        return RegimeComputationResult(
            regime = TaxRegime.OLD_REGIME,
            grossTotalIncome = normalGti + specialIncome,
            standardDeduction = stdDed,
            totalChapterVIADeductions = totalChapterVIA,
            netTaxableNormalIncomeRounded288A = normalTaxable288A,
            netTaxableSpecialRateIncome = specialIncome,
            baseSlabTaxOnNormalIncome = baseNormalTax,
            specialRateTax111AAnd112A = specialTax,
            rebate87A = rebate87A,
            marginalRelief87A = 0L,
            taxAfterRebateAndRelief = taxAfterRebate,
            surchargeAfterMarginalRelief = surcharge,
            healthAndEducationCess = cess,
            totalTaxLiabilityRounded288B = finalTax288B
        )
    }

    fun computeSection80GDeduction(donations: List<Section80GDonationInput>, adjustedGti: Long): Long {
        var noLimitDeduction = 0L
        var qualifying100Pool = 0L
        var qualifying50Pool = 0L
        for (d in donations) {
            val eligibleCash = if (d.donationInCashRupees <= 2_000L) d.donationInCashRupees else 0L
            val eligibleTotal = eligibleCash + d.donationOtherModeRupees
            when (d.category) {
                DonationCategory.HUNDRED_PERCENT_NO_LIMIT -> noLimitDeduction += eligibleTotal
                DonationCategory.FIFTY_PERCENT_NO_LIMIT -> noLimitDeduction += (eligibleTotal * 0.50).roundToLong()
                DonationCategory.HUNDRED_PERCENT_WITH_10PCT_CAP -> qualifying100Pool += eligibleTotal
                DonationCategory.FIFTY_PERCENT_WITH_10PCT_CAP -> qualifying50Pool += eligibleTotal
            }
        }
        val tenPercentAgtiCap = (adjustedGti * 0.10).roundToLong()
        val allowed100Qualifying = min(qualifying100Pool, tenPercentAgtiCap)
        val remainingCapFor50 = max(0L, tenPercentAgtiCap - allowed100Qualifying)
        val allowed50Qualifying = (min(qualifying50Pool, remainingCapFor50) * 0.50).roundToLong()
        return noLimitDeduction + allowed100Qualifying + allowed50Qualifying
    }

    private fun computeSurchargeWithMarginalRelief(totalIncome: Long, baseTax: Long, isNewRegime: Boolean): Long {
        if (totalIncome <= 50_00_000L) return 0L
        val (threshold, rate, prevRate) = when {
            totalIncome <= 1_00_00_000L -> Triple(50_00_000L, 0.10, 0.00)
            totalIncome <= 2_00_00_000L -> Triple(1_00_00_000L, 0.15, 0.10)
            totalIncome <= 5_00_00_000L -> Triple(2_00_00_000L, 0.25, 0.15)
            else -> Triple(5_00_00_000L, if (isNewRegime) 0.25 else 0.37, 0.25)
        }
        val rawSurcharge = (baseTax * rate).roundToLong()
        val totalTaxWithSurcharge = baseTax + rawSurcharge
        val excessIncomeOverThreshold = totalIncome - threshold
        val taxAtThreshold = baseTax - (excessIncomeOverThreshold * 0.30).roundToLong()
        val surchargeAtThreshold = (taxAtThreshold * prevRate).roundToLong()
        val maxPayableUnderMarginalRelief = taxAtThreshold + surchargeAtThreshold + excessIncomeOverThreshold
        return if (totalTaxWithSurcharge > maxPayableUnderMarginalRelief) {
            max(0L, maxPayableUnderMarginalRelief - baseTax)
        } else rawSurcharge
    }

    private fun calculateProgressiveSlabs(income: Long, brackets: List<Pair<Long, Double>>): Long {
        var totalTax = 0.0
        var previousLimit = 0L
        for ((upperLimit, rate) in brackets) {
            if (income > previousLimit) {
                val taxableInSlab = min(income, upperLimit) - previousLimit
                totalTax += taxableInSlab * rate
                previousLimit = upperLimit
            } else break
        }
        return totalTax.roundToLong()
    }

    fun roundToNearestTen(amount: Long): Long {
        val remainder = amount % 10L
        return if (remainder >= 5L) amount + (10L - remainder) else amount - remainder
    }
}
