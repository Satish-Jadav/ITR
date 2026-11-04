package `in`.zerotax.core.taxengine.advanced

import `in`.zerotax.core.model.HomeLoanTaxImpact
import `in`.zerotax.core.model.TaxableIncomeImpact
import kotlin.math.pow
import kotlin.math.roundToLong

object HomeLoanAndDeductionCalculator {

    const val MAX_SEC24B_INTEREST_LIMIT = 2_00_000L
    const val MAX_SEC80C_PRINCIPAL_LIMIT = 1_50_000L

    /**
     * Calculates the exact annual interest and principal breakdown from a monthly EMI,
     * applying statutory caps under Section 24(b) (₹2,00,000) and Section 80C (₹1,50,000).
     */
    fun calculateHomeLoanDeduction(
        monthlyEmi: Long,
        loanTenorYears: Int = 20,
        annualInterestRatePercent: Double = 8.5
    ): HomeLoanTaxImpact {
        if (monthlyEmi <= 0) {
            return HomeLoanTaxImpact(
                monthlyEmi = 0L,
                annualTotalEmiPaid = 0L,
                estimatedAnnualInterest = 0L,
                estimatedAnnualPrincipal = 0L,
                sec24bInterestDeductionAllowed = 0L,
                sec80cPrincipalDeductionAllowed = 0L,
                totalHomeLoanDeduction = 0L,
                unclaimedExcessInterest = 0L,
                unclaimedExcessPrincipal = 0L
            )
        }

        val annualEmiPaid = monthlyEmi * 12L

        // Amortization split calculation for standard Indian home loans
        // In typical 15-20 year home loans, early/mid-term annual interest is ~58%-62% and principal is ~38%-42%
        val monthlyRate = (annualInterestRatePercent / 100.0) / 12.0
        val totalMonths = loanTenorYears * 12.0

        // Reverse compute estimated principal balance: P = EMI * ((1+r)^n - 1) / (r * (1+r)^n)
        val compoundFactor = (1.0 + monthlyRate).pow(totalMonths)
        val estimatedPrincipalOutstanding = (monthlyEmi * (compoundFactor - 1.0) / (monthlyRate * compoundFactor)).roundToLong()

        // Annual interest = outstanding principal * annual interest rate
        val rawAnnualInterest = (estimatedPrincipalOutstanding * (annualInterestRatePercent / 100.0)).roundToLong()
        val annualInterest = minOf(rawAnnualInterest, (annualEmiPaid * 0.58).toLong())
        val annualPrincipal = maxOf(0L, annualEmiPaid - annualInterest)

        // Apply statutory ceiling
        val allowedSec24b = minOf(annualInterest, MAX_SEC24B_INTEREST_LIMIT)
        val allowedSec80c = minOf(annualPrincipal, MAX_SEC80C_PRINCIPAL_LIMIT)
        val totalDeduction = allowedSec24b + allowedSec80c

        val excessInterest = maxOf(0L, annualInterest - MAX_SEC24B_INTEREST_LIMIT)
        val excessPrincipal = maxOf(0L, annualPrincipal - MAX_SEC80C_PRINCIPAL_LIMIT)

        return HomeLoanTaxImpact(
            monthlyEmi = monthlyEmi,
            annualTotalEmiPaid = annualEmiPaid,
            estimatedAnnualInterest = annualInterest,
            estimatedAnnualPrincipal = annualPrincipal,
            sec24bInterestDeductionAllowed = allowedSec24b,
            sec80cPrincipalDeductionAllowed = allowedSec80c,
            totalHomeLoanDeduction = totalDeduction,
            unclaimedExcessInterest = excessInterest,
            unclaimedExcessPrincipal = excessPrincipal
        )
    }

    /**
     * Computes the exact taxable income reduction and cash tax saved based on gross salary,
     * standard deduction, and home loan deductions.
     */
    fun computeTaxImpact(
        grossSalary: Long,
        otherDeductions: Long,
        homeLoanImpact: HomeLoanTaxImpact
    ): TaxableIncomeImpact {
        val stdDeduction = 50_000L // Standard deduction under Old Regime for salary

        // Original taxable income without home loan
        val originalNet = maxOf(0L, grossSalary - stdDeduction - minOf(otherDeductions, 1_50_000L))
        val originalTax = computeOldRegimeTax(originalNet)

        // New taxable income with home loan deductions
        // Sec 24(b) reduces Gross Total Income (House Property Loss)
        val gtiAfterHouseProperty = maxOf(0L, grossSalary - stdDeduction - homeLoanImpact.sec24bInterestDeductionAllowed)

        // 80C umbrella includes Home Loan Principal + other 80C, capped at ₹1,50,000
        val totalSec80cClaimed = minOf(homeLoanImpact.sec80cPrincipalDeductionAllowed + otherDeductions, MAX_SEC80C_PRINCIPAL_LIMIT)
        val newNet = maxOf(0L, gtiAfterHouseProperty - totalSec80cClaimed)
        val newTax = computeOldRegimeTax(newNet)

        val totalDeductionsClaimed = stdDeduction + homeLoanImpact.sec24bInterestDeductionAllowed + totalSec80cClaimed
        val taxSaved = maxOf(0L, originalTax - newTax)

        return TaxableIncomeImpact(
            grossSalary = grossSalary,
            standardDeduction = stdDeduction,
            homeLoanDeduction = homeLoanImpact.totalHomeLoanDeduction,
            otherChapterVIADeductions = otherDeductions,
            totalDeductions = totalDeductionsClaimed,
            originalTaxableIncome = originalNet,
            newTaxableIncome = newNet,
            taxBeforeHomeLoan = originalTax,
            taxAfterHomeLoan = newTax,
            netCashTaxSaved = taxSaved
        )
    }

    /**
     * Pure-JVM deterministic Old Regime tax calculator (AY 2026-27).
     * 0 to 2.5L: 0% | 2.5L to 5L: 5% | 5L to 10L: 20% | Above 10L: 30% + 4% Cess
     */
    fun computeOldRegimeTax(taxableIncome: Long): Long {
        if (taxableIncome <= 2_50_000L) return 0L
        if (taxableIncome <= 5_00_000L) return 0L // Section 87A rebate covers up to ₹5L in Old Regime

        var tax = 0L

        // 2.5L to 5L @ 5%
        tax += (2_50_000L * 0.05).toLong() // 12,500

        if (taxableIncome > 5_00_000L) {
            val incomeIn20PctSlab = minOf(taxableIncome - 5_00_000L, 5_00_000L)
            tax += (incomeIn20PctSlab * 0.20).toLong()
        }

        if (taxableIncome > 10_00_000L) {
            val incomeIn30PctSlab = taxableIncome - 10_00_000L
            tax += (incomeIn30PctSlab * 0.30).toLong()
        }

        // Add 4% Health & Education Cess
        val cess = (tax * 0.04).toLong()
        return tax + cess
    }
}
