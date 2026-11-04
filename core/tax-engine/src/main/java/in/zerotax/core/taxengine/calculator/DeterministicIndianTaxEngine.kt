package `in`.zerotax.core.taxengine.calculator

import `in`.zerotax.core.model.AssessmentYearProfile
import `in`.zerotax.core.model.CitizenCategory
import `in`.zerotax.core.model.DonationCategory
import `in`.zerotax.core.model.DualRegimeComparison
import `in`.zerotax.core.model.RegimeComputationResult
import `in`.zerotax.core.model.Section80GDonationInput
import `in`.zerotax.core.model.TaxRegime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

typealias RegimeComputationResult = `in`.zerotax.core.model.RegimeComputationResult

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

        // Basic Exemption Absorption (Proviso to Sec 111A & 112A)
        val basicExemptionLimit = if (input.assessmentYearProfile == AssessmentYearProfile.AY_2026_27) 4_00_000L else 3_00_000L
        var unexhaustedExemption = if (input.isResidentIndividual && normalTaxable288A < basicExemptionLimit) {
            basicExemptionLimit - normalTaxable288A
        } else 0L

        val stcgAbsorbed = min(input.netTaxableStcg111ARupees, unexhaustedExemption)
        val effectiveStcg = input.netTaxableStcg111ARupees - stcgAbsorbed
        unexhaustedExemption -= stcgAbsorbed

        val rawLtcgAboveThreshold = max(0L, input.netTaxableLtcg112ARupees - SEC_112A_EXEMPT_THRESHOLD)
        val ltcgAbsorbed = min(rawLtcgAboveThreshold, unexhaustedExemption)
        val effectiveLtcgAboveThreshold = rawLtcgAboveThreshold - ltcgAbsorbed

        val stcgTax = (effectiveStcg * 0.20).roundToLong()
        val ltcgTax = (effectiveLtcgAboveThreshold * 0.125).roundToLong()
        val specialTax = stcgTax + ltcgTax

        // Statutory Sec 87A Rebate: Strictly barred against Special Rate incomes (Sec 111A & 112A)
        val rebateThreshold87A = if (input.assessmentYearProfile == AssessmentYearProfile.AY_2026_27) 12_00_000L else NEW_REGIME_87A_THRESHOLD
        val maxRebate87A = if (input.assessmentYearProfile == AssessmentYearProfile.AY_2026_27) 60_000L else NEW_REGIME_87A_MAX_REBATE
        val rebate87A = if (input.isResidentIndividual && normalTaxable288A <= rebateThreshold87A) {
            min(baseNormalTax, maxRebate87A)
        } else 0L

        val normalTaxAfterRebate = max(0L, baseNormalTax - rebate87A)
        val marginalRelief87A = if (input.isResidentIndividual && normalTaxable288A > rebateThreshold87A) {
            val excessOverThreshold = normalTaxable288A - rebateThreshold87A
            if (normalTaxAfterRebate > excessOverThreshold) normalTaxAfterRebate - excessOverThreshold else 0L
        } else 0L

        val netNormalTax = max(0L, normalTaxAfterRebate - marginalRelief87A)
        val totalTaxBeforeSurcharge = netNormalTax + specialTax
        val surcharge = computeSurchargeWithMarginalRelief(
            totalIncome = totalIncome288A,
            normalTax = netNormalTax,
            specialTax = specialTax,
            isNewRegime = true
        )
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

        // Basic Exemption Absorption (Proviso to Sec 111A & 112A)
        var unexhausted = if (input.isResidentIndividual && normalTaxable288A < exemptionLimit) {
            exemptionLimit - normalTaxable288A
        } else 0L

        val stcgAbsorbed = min(input.netTaxableStcg111ARupees, unexhausted)
        val effectiveStcg = input.netTaxableStcg111ARupees - stcgAbsorbed
        unexhausted -= stcgAbsorbed

        val rawLtcgAboveThreshold = max(0L, input.netTaxableLtcg112ARupees - 1_00_000L)
        val ltcgAbsorbed = min(rawLtcgAboveThreshold, unexhausted)
        val effectiveLtcgAboveThreshold = rawLtcgAboveThreshold - ltcgAbsorbed

        val stcgTax = (effectiveStcg * 0.15).roundToLong()
        val ltcgTax = (effectiveLtcgAboveThreshold * 0.10).roundToLong()
        val specialTax = stcgTax + ltcgTax

        val rebate87A = if (input.isResidentIndividual && totalIncome288A <= OLD_REGIME_87A_THRESHOLD) {
            min(baseNormalTax, OLD_REGIME_87A_MAX_REBATE)
        } else 0L

        val normalTaxAfterRebate = max(0L, baseNormalTax - rebate87A)
        val totalTaxBeforeSurcharge = normalTaxAfterRebate + specialTax
        val surcharge = computeSurchargeWithMarginalRelief(
            totalIncome = totalIncome288A,
            normalTax = normalTaxAfterRebate,
            specialTax = specialTax,
            isNewRegime = false
        )
        val cess = ((totalTaxBeforeSurcharge + surcharge) * 0.04).roundToLong()
        val finalTax288B = roundToNearestTen(totalTaxBeforeSurcharge + surcharge + cess)

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
            taxAfterRebateAndRelief = totalTaxBeforeSurcharge,
            surchargeAfterMarginalRelief = surcharge,
            healthAndEducationCess = cess,
            totalTaxLiabilityRounded288B = finalTax288B
        )
    }

    fun compareRegimes(input: TaxpayerFinancialInput): DualRegimeComparison {
        val newResult = computeNewRegime(input)
        val oldResult = computeOldRegime(input)
        val recommended = if (newResult.totalTaxLiabilityRounded288B <= oldResult.totalTaxLiabilityRounded288B) {
            TaxRegime.NEW_REGIME_115BAC
        } else {
            TaxRegime.OLD_REGIME
        }
        val taxDiff = abs(newResult.totalTaxLiabilityRounded288B - oldResult.totalTaxLiabilityRounded288B)

        var breakEvenDeduction = 0L
        if (newResult.totalTaxLiabilityRounded288B < oldResult.totalTaxLiabilityRounded288B) {
            var low = 0L
            var high = 20_00_000L
            var ans = high
            while (low <= high) {
                val mid = (low + high) / 2
                val testOld = computeOldRegime(input.copy(
                    section80CInvestments = input.section80CInvestments + mid
                ))
                if (testOld.totalTaxLiabilityRounded288B <= newResult.totalTaxLiabilityRounded288B) {
                    ans = mid
                    high = mid - 1
                } else {
                    low = mid + 1
                }
            }
            breakEvenDeduction = ans
        }

        val max80CCD2 = (input.basicPlusDaForNps * 0.14).roundToLong()
        val unused80CCD2 = max(0L, max80CCD2 - input.section80CCD2EmployerNps)
        val unusedPreventive = if (input.section80DSelfAndFamily < 25_000L) min(5_000L, 25_000L - input.section80DSelfAndFamily) else 0L
        val unusedSeniorMedical = max(0L, 50_000L - input.section80DParentsSenior)
        val unused80C = max(0L, 1_50_000L - input.section80CInvestments)
        val unused80CCD1B = max(0L, 50_000L - input.section80CCD1BSelfNps)

        val nudges = mutableListOf<String>()
        if (recommended == TaxRegime.NEW_REGIME_115BAC) {
            if (unused80CCD2 > 0L) {
                nudges.add("Opt into Section 80CCD(2) Corporate NPS via your employer for up to ₹$unused80CCD2 tax-free deduction in the New Regime.")
            }
            if (breakEvenDeduction > 0L) {
                nudges.add("You would need ₹$breakEvenDeduction in additional Chapter VI-A / HRA deductions for the Old Regime to match the New Regime.")
            }
        } else {
            if (unused80C > 0L) {
                nudges.add("You have ₹$unused80C unused Section 80C headroom (ELSS, PPF, Term Insurance).")
            }
            if (unusedPreventive > 0L) {
                nudges.add("Claim up to ₹$unusedPreventive for Preventive Health Checkups under Section 80D(2A) without medical policy.")
            }
            if (unusedSeniorMedical > 0L) {
                nudges.add("Claim up to ₹$unusedSeniorMedical under Section 80D(2)(d) for medical bills of senior citizen uninsured parents.")
            }
            if (unused80CCD1B > 0L) {
                nudges.add("Invest up to ₹$unused80CCD1B in voluntary Tier 1 NPS for exclusive Section 80CCD(1B) deduction.")
            }
        }

        return DualRegimeComparison(
            newRegimeResult = newResult,
            oldRegimeResult = oldResult,
            recommendedRegime = recommended,
            taxDifferenceRupees = taxDiff,
            breakEvenAdditionalDeductionForOldRegime = breakEvenDeduction,
            unused80CCD2EmployerNpsHeadroom = unused80CCD2,
            unused80DPreventiveCheckup5kHeadroom = unusedPreventive,
            seniorParentsUninsuredMedical80DHeadroom = unusedSeniorMedical,
            unused80CHeadroom = unused80C,
            unused80CCD1BHeadroom = unused80CCD1B,
            actionableNudges = nudges
        )
    }

    // Convenience delegates for sub-engines and simulators
    fun computeNewRegimeTaxAY2026_27(
        grossTotalIncome: Long,
        standardDeduction: Long = 75_000L,
        section80CCD2: Long = 0L
    ): RegimeComputationResult {
        val input = if (standardDeduction == 0L) {
            TaxpayerFinancialInput(
                grossSalary = 0L,
                savingsBankInterest = max(0L, grossTotalIncome - section80CCD2),
                assessmentYearProfile = AssessmentYearProfile.AY_2026_27
            )
        } else {
            TaxpayerFinancialInput(
                grossSalary = grossTotalIncome,
                section80CCD2EmployerNps = section80CCD2,
                basicPlusDaForNps = if (section80CCD2 > 0) (section80CCD2 / 0.14).roundToLong() else 0L,
                assessmentYearProfile = AssessmentYearProfile.AY_2026_27
            )
        }
        return computeNewRegime(input)
    }

    fun computeOldRegimeTaxAY2026_27(
        grossTotalIncome: Long,
        standardDeduction: Long = 50_000L,
        section80C: Long = 0L,
        section80D: Long = 0L,
        section80TTA: Long = 0L,
        section24bHomeLoanInterest: Long = 0L
    ): RegimeComputationResult {
        val input = if (standardDeduction == 0L) {
            val netNormal = max(0L, grossTotalIncome - section80C - section80D - section80TTA - section24bHomeLoanInterest)
            TaxpayerFinancialInput(
                grossSalary = 0L,
                savingsBankInterest = netNormal,
                assessmentYearProfile = AssessmentYearProfile.AY_2026_27
            )
        } else {
            TaxpayerFinancialInput(
                grossSalary = grossTotalIncome,
                selfOccupiedHomeLoanInterestSec24b = section24bHomeLoanInterest,
                savingsBankInterest = section80TTA,
                section80CInvestments = section80C,
                section80DSelfAndFamily = section80D,
                assessmentYearProfile = AssessmentYearProfile.AY_2026_27
            )
        }
        return computeOldRegime(input)
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

    private fun computeSurchargeWithMarginalRelief(
        totalIncome: Long,
        normalTax: Long,
        specialTax: Long,
        isNewRegime: Boolean
    ): Long {
        if (totalIncome <= 50_00_000L) return 0L
        val (threshold, normalRate, prevRate) = when {
            totalIncome <= 1_00_00_000L -> Triple(50_00_000L, 0.10, 0.00)
            totalIncome <= 2_00_00_000L -> Triple(1_00_00_000L, 0.15, 0.10)
            totalIncome <= 5_00_00_000L -> Triple(2_00_00_000L, 0.25, 0.15)
            else -> Triple(5_00_00_000L, if (isNewRegime) 0.25 else 0.37, 0.25)
        }

        // Proviso to Finance Act: Capped at 15% on Capital Gains (111A, 112A) and Dividends
        val specialSurchargeRate = min(normalRate, 0.15)
        val rawSurcharge = (normalTax * normalRate).roundToLong() + (specialTax * specialSurchargeRate).roundToLong()
        val baseTax = normalTax + specialTax
        val totalTaxWithSurcharge = baseTax + rawSurcharge
        val excessIncomeOverThreshold = totalIncome - threshold

        // Marginal relief calculation
        val taxAtThreshold = baseTax - (excessIncomeOverThreshold * 0.30).roundToLong()
        val surchargeAtThreshold = (taxAtThreshold * min(prevRate, 0.15)).roundToLong()
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
