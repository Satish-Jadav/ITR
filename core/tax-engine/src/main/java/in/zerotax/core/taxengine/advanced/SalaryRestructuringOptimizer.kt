package `in`.zerotax.core.taxengine.advanced

import `in`.zerotax.core.taxengine.calculator.DeterministicIndianTaxEngine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * CTC Salary Restructuring & Flexi-Benefits Optimizer
 *
 * Implements deterministic tax reduction models for salaried Indian taxpayers under:
 * 1. Section 80CCD(2): Corporate Employer NPS contribution up to 14% of Basic (available in BOTH Old and New Regimes).
 * 2. Rule 3(7)(ix): Telephony & Broadband reimbursement (100% tax-free against bills).
 * 3. Section 10(14)(i) & Rule 3: Books, periodicals, and professional development allowance.
 * 4. Rule 3(7)(iii): Meal / Sodexo vouchers (tax-exempt up to ₹50/meal, ~₹26,400/year).
 * 5. Rule 3(2): Official car lease / fuel & chauffeur perks.
 *
 * Provides a side-by-side comparison of Current vs Optimized CTC and generates
 * a ready-to-submit corporate HR proposal letter.
 */
object SalaryRestructuringOptimizer {

    data class SalaryComponents(
        val basicSalary: Long,
        val hraReceived: Long = 0L,
        val specialAllowance: Long = 0L,
        val employerNps80CCD2: Long = 0L,
        val telecomReimbursement: Long = 0L,
        val booksAndPeriodicals: Long = 0L,
        val foodVouchers: Long = 0L,
        val otherAllowances: Long = 0L
    ) {
        val totalGrossCtc: Long
            get() = basicSalary + hraReceived + specialAllowance + employerNps80CCD2 +
                    telecomReimbursement + booksAndPeriodicals + foodVouchers + otherAllowances
    }

    data class OptimizationNudge(
        val title: String,
        val sectionCode: String,
        val annualBenefitAmount: Long,
        val taxSavingEstimated: Long,
        val description: String,
        val regimeApplicability: String
    )

    data class RestructuringReport(
        val currentComponents: SalaryComponents,
        val optimizedComponents: SalaryComponents,
        val currentTax: Long,
        val currentRecommendedRegime: String,
        val optimizedTax: Long,
        val optimizedRecommendedRegime: String,
        val annualTaxSavings: Long,
        val monthlyTakeHomeIncrease: Long,
        val nudges: List<OptimizationNudge>,
        val hrProposalLetterMarkdown: String
    )

    /**
     * Optimizes a given gross salary / CTC by re-allocating taxable special allowances
     * into statutory exempt flexi-benefit buckets.
     */
    fun optimizeSalaryStructure(
        grossSalaryCtc: Long,
        currentBasicPercent: Double = 0.50,
        rentPaidAnnual: Long = 0L,
        isMetroCity: Boolean = true,
        existing80CDeduction: Long = 150000L,
        existing80DDeduction: Long = 25000L
    ): RestructuringReport {
        val clampedGross = max(300000L, grossSalaryCtc)
        val basicSalary = (clampedGross * currentBasicPercent.coerceIn(0.35, 0.50)).roundToLong()
        val hraReceived = (basicSalary * (if (isMetroCity) 0.50 else 0.40)).roundToLong()
        val remainingPool = max(0L, clampedGross - basicSalary - hraReceived)

        // Baseline: Entire remaining pool is categorized as fully taxable Special Allowance
        val currentComponents = SalaryComponents(
            basicSalary = basicSalary,
            hraReceived = hraReceived,
            specialAllowance = remainingPool,
            employerNps80CCD2 = 0L,
            telecomReimbursement = 0L,
            booksAndPeriodicals = 0L,
            foodVouchers = 0L
        )

        // Optimized Allocation:
        // 1. Max Employer NPS (14% of Basic under Section 80CCD(2))
        val maxNps14Percent = (basicSalary * 0.14).roundToLong()
        val actualNpsToAllocate = min(remainingPool, maxNps14Percent)
        var remainingAfterNps = remainingPool - actualNpsToAllocate

        // 2. Telephony & Broadband reimbursement (₹30,000/yr)
        val telecomAllocate = min(remainingAfterNps, 30000L)
        remainingAfterNps -= telecomAllocate

        // 3. Books & Professional Periodicals (₹24,000/yr)
        val booksAllocate = min(remainingAfterNps, 24000L)
        remainingAfterNps -= booksAllocate

        // 4. Meal vouchers / Sodexo coupons (₹26,400/yr)
        val foodAllocate = min(remainingAfterNps, 26400L)
        val finalSpecialAllowance = remainingAfterNps - foodAllocate

        val optimizedComponents = SalaryComponents(
            basicSalary = basicSalary,
            hraReceived = hraReceived,
            specialAllowance = max(0L, finalSpecialAllowance),
            employerNps80CCD2 = actualNpsToAllocate,
            telecomReimbursement = telecomAllocate,
            booksAndPeriodicals = booksAllocate,
            foodVouchers = foodAllocate
        )

        // Compute Tax for Baseline Current Structure
        val currentTaxOld = computeTaxForComponents(
            components = currentComponents,
            isNewRegime = false,
            rentPaidAnnual = rentPaidAnnual,
            isMetroCity = isMetroCity,
            deduction80C = existing80C,
            deduction80D = existing80D
        )
        val currentTaxNew = computeTaxForComponents(
            components = currentComponents,
            isNewRegime = true,
            rentPaidAnnual = rentPaidAnnual,
            isMetroCity = isMetroCity,
            deduction80C = 0L,
            deduction80D = 0L
        )
        val currentOptimalTax = min(currentTaxOld, currentTaxNew)
        val currentRegimeName = if (currentTaxNew <= currentTaxOld) "New Regime" else "Old Regime"

        // Compute Tax for Optimized Structure
        val optimizedTaxOld = computeTaxForComponents(
            components = optimizedComponents,
            isNewRegime = false,
            rentPaidAnnual = rentPaidAnnual,
            isMetroCity = isMetroCity,
            deduction80C = existing80C,
            deduction80D = existing80D
        )
        val optimizedTaxNew = computeTaxForComponents(
            components = optimizedComponents,
            isNewRegime = true,
            rentPaidAnnual = rentPaidAnnual,
            isMetroCity = isMetroCity,
            deduction80C = 0L,
            deduction80D = 0L
        )
        val optimizedOptimalTax = min(optimizedTaxOld, optimizedTaxNew)
        val optimizedRegimeName = if (optimizedTaxNew <= optimizedTaxOld) "New Regime" else "Old Regime"

        val annualSavings = max(0L, currentOptimalTax - optimizedOptimalTax)
        val monthlyTakeHomeIncrease = annualSavings / 12

        // Build list of optimization nudges
        val nudges = mutableListOf<OptimizationNudge>()
        if (actualNpsToAllocate > 0L) {
            val estimatedRate = if (clampedGross > 1500000L) 0.312 else if (clampedGross > 1000000L) 0.208 else 0.104
            nudges.add(
                OptimizationNudge(
                    title = "Employer NPS Contribution u/s 80CCD(2)",
                    sectionCode = "Sec 80CCD(2)",
                    annualBenefitAmount = actualNpsToAllocate,
                    taxSavingEstimated = (actualNpsToAllocate * estimatedRate).roundToLong(),
                    description = "14% of Basic paid directly to your NPS PRAN by employer. 100% tax-free under BOTH New & Old Regimes without any ₹1.5L cap.",
                    regimeApplicability = "New & Old Regimes"
                )
            )
        }
        if (telecomAllocate > 0L) {
            nudges.add(
                OptimizationNudge(
                    title = "Remote Work Internet & Phone Reimbursement",
                    sectionCode = "Rule 3(7)(ix)",
                    annualBenefitAmount = telecomAllocate,
                    taxSavingEstimated = (telecomAllocate * 0.312).roundToLong(),
                    description = "Actual mobile, broadband, and data-card expenses incurred for official duties reimbursed tax-free.",
                    regimeApplicability = "New & Old Regimes"
                )
            )
        }
        if (booksAllocate > 0L) {
            nudges.add(
                OptimizationNudge(
                    title = "Professional Books & Learning Subscriptions",
                    sectionCode = "Sec 10(14)(i)",
                    annualBenefitAmount = booksAllocate,
                    taxSavingEstimated = (booksAllocate * 0.312).roundToLong(),
                    description = "Subscriptions to technical journals, industry portals, books, and courses reimbursed against bills.",
                    regimeApplicability = "Old Regime / Non-Perk"
                )
            )
        }
        if (foodAllocate > 0L) {
            nudges.add(
                OptimizationNudge(
                    title = "Prepaid Meal Vouchers / Sodexo Cards",
                    sectionCode = "Rule 3(7)(iii)",
                    annualBenefitAmount = foodAllocate,
                    taxSavingEstimated = (foodAllocate * 0.312).roundToLong(),
                    description = "Tax-exempt allowance of ₹50 per meal (₹2,200/month) for food and non-alcoholic beverages.",
                    regimeApplicability = "Old Regime"
                )
            )
        }

        val hrLetter = generateHrProposalLetter(
            grossCtc = clampedGross,
            currentComponents = currentComponents,
            optimizedComponents = optimizedComponents,
            annualTaxSavings = annualSavings,
            monthlyInHandIncrease = monthlyTakeHomeIncrease
        )

        return RestructuringReport(
            currentComponents = currentComponents,
            optimizedComponents = optimizedComponents,
            currentTax = currentOptimalTax,
            currentRecommendedRegime = currentRegimeName,
            optimizedTax = optimizedOptimalTax,
            optimizedRecommendedRegime = optimizedRegimeName,
            annualTaxSavings = annualSavings,
            monthlyTakeHomeIncrease = monthlyTakeHomeIncrease,
            nudges = nudges,
            hrProposalLetterMarkdown = hrLetter
        )
    }

    private const val existing80C = 150000L
    private const val existing80D = 25000L

    private fun computeTaxForComponents(
        components: SalaryComponents,
        isNewRegime: Boolean,
        rentPaidAnnual: Long,
        isMetroCity: Boolean,
        deduction80C: Long,
        deduction80D: Long
    ): Long {
        // Standard Deduction: ₹75,000 for New Regime, ₹50,000 for Old Regime (AY 2026-27 / FY 2025-26)
        val standardDeduction = if (isNewRegime) 75000L else 50000L

        // HRA Exemption (only in Old Regime)
        val hraExemption = if (!isNewRegime && rentPaidAnnual > 0L && components.hraReceived > 0L) {
            val metroLimit = if (isMetroCity) 0.50 else 0.40
            val limit1 = components.hraReceived
            val limit2 = (components.basicSalary * metroLimit).roundToLong()
            val limit3 = max(0L, rentPaidAnnual - (components.basicSalary * 0.10).roundToLong())
            min(limit1, min(limit2, limit3))
        } else 0L

        // Taxable salary components
        val taxableSalary = if (isNewRegime) {
            // In New Regime, employer NPS is deductible u/s 80CCD(2). Food vouchers and books are not exempt if treated as flexi cash,
            // but official reimbursements (telecom) are exempt business expenses.
            val exemptOfficialReimbursements = components.telecomReimbursement
            val grossTaxable = components.totalGrossCtc - components.employerNps80CCD2 - exemptOfficialReimbursements
            max(0L, grossTaxable - standardDeduction)
        } else {
            // In Old Regime, all flexi perks (HRA exemption, telecom, books, food vouchers, employer NPS) are exempt
            val exemptTotal = hraExemption + components.employerNps80CCD2 + components.telecomReimbursement +
                    components.booksAndPeriodicals + components.foodVouchers
            val grossTaxable = components.totalGrossCtc - exemptTotal
            max(0L, grossTaxable - standardDeduction - deduction80C - deduction80D)
        }

        return if (isNewRegime) {
            DeterministicIndianTaxEngine.computeNewRegimeTaxAY2026_27(
                grossTotalIncome = taxableSalary,
                standardDeduction = 0L, // already factored above
                section80CCD2 = 0L // factored above
            ).totalTaxLiabilityRounded288B
        } else {
            DeterministicIndianTaxEngine.computeOldRegimeTaxAY2026_27(
                grossTotalIncome = taxableSalary,
                standardDeduction = 0L, // already factored above
                section80C = 0L, // factored above
                section80D = 0L, // factored above
                section80TTA = 0L,
                section24bHomeLoanInterest = 0L
            ).totalTaxLiabilityRounded288B
        }
    }

    private fun generateHrProposalLetter(
        grossCtc: Long,
        currentComponents: SalaryComponents,
        optimizedComponents: SalaryComponents,
        annualTaxSavings: Long,
        monthlyInHandIncrease: Long
    ): String {
        return """
# FORMAL REQUEST FOR CTC RESTRUCTURING & FLEXI-BENEFITS ALLOCATION
**Date:** ${java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy"))}  
**To:** Head of Human Resources & Payroll Operations  
**Subject:** Request for Opt-in to Corporate Tax-Efficient Salary Structure (Sec 80CCD(2) & Flexi-Reimbursements)

Dear HR & Payroll Team,

I am writing to formally request a restructuring of my annual Cost-to-Company (CTC) package of ₹${"%,d".format(grossCtc)}. 
In accordance with the statutory provisions of the **Income-tax Act, 1961** and the **Finance Act, 2024**, I would like to re-allocate a portion of my currently fully-taxable **Special Allowance** into approved statutory tax-exempt components, with **zero additional cost or liability to the company**.

### 1. Proposed Restructuring Breakdown
| CTC Component | Current Structure (₹/yr) | Proposed Structure (₹/yr) | Statutory Exemption Reference |
| :--- | :--- | :--- | :--- |
| **Basic Salary** | ₹${"%,d".format(currentComponents.basicSalary)} | ₹${"%,d".format(optimizedComponents.basicSalary)} | Base statutory pay |
| **House Rent Allowance (HRA)** | ₹${"%,d".format(currentComponents.hraReceived)} | ₹${"%,d".format(optimizedComponents.hraReceived)} | Section 10(13A) & Rule 2A |
| **Employer NPS Contribution** | ₹0 | ₹${"%,d".format(optimizedComponents.employerNps80CCD2)} | **Section 80CCD(2)** (14% of Basic) |
| **Broadband & Telephony** | ₹0 | ₹${"%,d".format(optimizedComponents.telecomReimbursement)} | **Rule 3(7)(ix)** (Official expenses) |
| **Books & Technical Periodicals**| ₹0 | ₹${"%,d".format(optimizedComponents.booksAndPeriodicals)} | **Section 10(14)(i)** |
| **Meal Coupons (Sodexo/Zeta)** | ₹0 | ₹${"%,d".format(optimizedComponents.foodVouchers)} | **Rule 3(7)(iii)** |
| **Special Allowance (Balance)** | ₹${"%,d".format(currentComponents.specialAllowance)} | ₹${"%,d".format(optimizedComponents.specialAllowance)} | Fully taxable residual |
| **Total Annual CTC** | **₹${"%,d".format(currentComponents.totalGrossCtc)}** | **₹${"%,d".format(optimizedComponents.totalGrossCtc)}** | *Cost-Neutral to Employer* |

### 2. Taxpayer Impact & Compliance Safeguards
- **Estimated Annual Tax Savings:** ₹${"%,d".format(annualTaxSavings)}
- **Monthly Net Take-Home Increase:** ₹${"%,d".format(monthlyInHandIncrease)}
- **Statutory Audit Ready:** I undertake to provide original valid tax invoices and periodic self-declarations for telecommunication, broadband, and learning subscriptions during the annual tax proof submission cycle.
- **NPS PRAN Account:** My active PRAN details will be shared with the payroll desk for seamless corporate NPS remittal u/s 80CCD(2).

Kindly accept this restructuring proposal for implementation in the upcoming payroll cycle.

Yours sincerely,  
**Employee Name:** _______________________  
**Employee ID:** _________________________  
**Designation:** __________________________  
**Signature:** ___________________________  
        """.trimIndent()
    }
}

