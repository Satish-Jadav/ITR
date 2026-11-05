package `in`.zerotax.core.taxengine.ca

import `in`.zerotax.core.model.*
import kotlin.math.max
import kotlin.math.min

/**
 * "Think Like a CA" Heuristic Deduction & Maximum Refund Optimizer.
 *
 * Implements the aggressive legal tax-planning strategies utilized by experienced
 * Chartered Accountants to discover every unexhausted statutory deduction under the
 * Income Tax Act, 1961, bringing taxable income below Section 87A rebate ceilings
 * to convert tax payable into INR 0 and trigger 100% refund of prepaid TDS.
 *
 * Invariant: Pure JVM Kotlin (zero Android imports).
 */
object ThinkLikeACaDeductionMaximizerEngine {

    /**
     * Input parameters for CA deduction review.
     */
    data class CaReviewInput(
        val pan: String,
        val grossSalary: Long = 0L,
        val rentalIncome: Long = 0L,
        val businessOrFreelanceReceipts: Long = 0L,
        val otherSourcesInterest: Long = 0L,
        val capitalGains: Long = 0L,
        val existingDeductionsClaimed: Map<String, Long> = emptyMap(),
        val tdsPrepaid: Long = 0L,
        val isSeniorCitizen: Boolean = false,
        val parentsAreSeniorCitizens: Boolean = true,
        val paysRent: Boolean = false,
        val annualRentPaid: Long = 0L,
        val receivesHraInSalary: Boolean = false,
        val hasHomeLoan: Boolean = false,
        val homeLoanInterestPaid: Long = 0L,
        val homeLoanPrincipalPaid: Long = 0L,
        val hasEducationLoan: Boolean = false,
        val educationLoanInterestPaid: Long = 0L,
        val hasDisabledDependent: Boolean = false,
        val employerOffersNps80CCD2: Boolean = true,
        val basicSalary: Long = 0L
    )

    /**
     * Executes the comprehensive CA heuristic deduction and refund maximization analysis.
     */
    fun optimizeTaxAndMaximizeRefund(input: CaReviewInput): ThinkLikeACaOptimizationReport {
        val grossTotalIncome = input.grossSalary + input.rentalIncome +
                input.businessOrFreelanceReceipts + input.otherSourcesInterest + input.capitalGains

        val estimatedBasic = if (input.basicSalary > 0L) input.basicSalary else (input.grossSalary * 0.50).toLong()

        val recommendations = mutableListOf<DeductionOpportunity>()

        // 1. SECTION 80CCD(2): Employer NPS Contribution (Allowed in BOTH New & Old Regimes!)
        if (input.employerOffersNps80CCD2 && input.grossSalary > 0L) {
            val maxEligible80CCD2 = (estimatedBasic * 0.14).toLong() // 14% of Basic under Finance Act
            val currentlyClaimed = input.existingDeductionsClaimed["80CCD(2)"] ?: 0L
            val additionalPotential = max(0L, maxEligible80CCD2 - currentlyClaimed)
            if (additionalPotential > 0L) {
                recommendations.add(
                    DeductionOpportunity(
                        sectionCode = "Sec 80CCD(2)",
                        title = "Employer National Pension System (14% of Basic)",
                        category = DeductionCategory.SALARY_PERQUISITE,
                        maxEligibleAmount = maxEligible80CCD2,
                        currentClaimedAmount = currentlyClaimed,
                        potentialAdditionalClaim = additionalPotential,
                        estimatedTaxSavedRupees = (additionalPotential * 0.20).toLong(),
                        legalExplanation = "Allowed under BOTH Old and New Tax Regimes (Sec 115BAC(1A)). Employer contributes directly up to 14% of Basic + DA with zero tax perquisite.",
                        requiredDocumentation = "Request HR/Payroll corporate flexi-benefit restructure before next payroll cycle.",
                        isApplicableInNewRegime = true,
                        isRebate87ATarget = true
                    )
                )
            }
        }

        // 2. SECTION 80D: Health Insurance Mediclaim & Preventive Checkup
        val maxSelfFamily80D = if (input.isSeniorCitizen) 50_000L else 25_000L
        val maxParents80D = if (input.parentsAreSeniorCitizens) 50_000L else 25_000L
        val totalPotential80D = maxSelfFamily80D + maxParents80D
        val current80D = input.existingDeductionsClaimed["80D"] ?: 0L
        val additional80D = max(0L, totalPotential80D - current80D)
        if (additional80D > 0L) {
            recommendations.add(
                DeductionOpportunity(
                    sectionCode = "Sec 80D",
                    title = "Mediclaim & Senior Parents Health Cover (Up to ₹1,00,000)",
                    category = DeductionCategory.HEALTH_AND_MEDICAL,
                    maxEligibleAmount = totalPotential80D,
                    currentClaimedAmount = current80D,
                    potentialAdditionalClaim = additional80D,
                    estimatedTaxSavedRupees = (additional80D * 0.20).toLong(),
                    legalExplanation = "Claim up to ₹25,000 (Self/Family) + ₹50,000 (Senior Citizen Parents) plus ₹5,000 preventive health check-up.",
                    requiredDocumentation = "Health insurance premium payment receipts (paid via online/cheque, not cash).",
                    isApplicableInNewRegime = false,
                    isRebate87ATarget = true
                )
            )
        }

        // 3. SECTION 24(b): Self-Occupied Home Loan Interest
        if (input.hasHomeLoan || input.homeLoanInterestPaid > 0L) {
            val maxHomeLoanCap = 2_00_000L
            val eligibleInterest = if (input.homeLoanInterestPaid > 0L) min(maxHomeLoanCap, input.homeLoanInterestPaid) else maxHomeLoanCap
            val currentInterestClaimed = input.existingDeductionsClaimed["24(b)"] ?: 0L
            val additionalInterest = max(0L, eligibleInterest - currentInterestClaimed)
            if (additionalInterest > 0L) {
                recommendations.add(
                    DeductionOpportunity(
                        sectionCode = "Sec 24(b)",
                        title = "Home Loan Interest Exemption (Self-Occupied ₹2,00,000 Cap)",
                        category = DeductionCategory.HOME_AND_RENT,
                        maxEligibleAmount = eligibleInterest,
                        currentClaimedAmount = currentInterestClaimed,
                        potentialAdditionalClaim = additionalInterest,
                        estimatedTaxSavedRupees = (additionalInterest * 0.20).toLong(),
                        legalExplanation = "Interest on capital borrowed for acquisition or construction of self-occupied house property deductible up to ₹2 Lakhs.",
                        requiredDocumentation = "Annual Home Loan Provisional/Final Interest Certificate from lending bank/NBFC.",
                        isApplicableInNewRegime = false,
                        isRebate87ATarget = true
                    )
                )
            }
        }

        // 4. SECTION 80CCD(1B): Additional NPS Tier-I (Exclusive ₹50,000)
        val current80CCD1B = input.existingDeductionsClaimed["80CCD(1B)"] ?: 0L
        val additional80CCD1B = max(0L, 50_000L - current80CCD1B)
        if (additional80CCD1B > 0L) {
            recommendations.add(
                DeductionOpportunity(
                    sectionCode = "Sec 80CCD(1B)",
                    title = "Voluntary NPS Tier-I Investment (Exclusive ₹50,000 Cap)",
                    category = DeductionCategory.CHAPTER_VIA_INVESTMENT,
                    maxEligibleAmount = 50_000L,
                    currentClaimedAmount = current80CCD1B,
                    potentialAdditionalClaim = additional80CCD1B,
                    estimatedTaxSavedRupees = (additional80CCD1B * 0.20).toLong(),
                    legalExplanation = "Exclusive deduction for self-contribution to NPS Tier-I, over and above the ₹1.5 Lakh limit of Section 80C.",
                    requiredDocumentation = "NPS Transaction Statement / PRAN Contribution Receipt from Protean (NSDL) / KFintech.",
                    isApplicableInNewRegime = false,
                    isRebate87ATarget = true
                )
            )
        }

        // 5. SECTION 80C: PPF, EPF, ELSS, Life Insurance & Tuition
        val current80C = input.existingDeductionsClaimed["80C"] ?: 0L
        val additional80C = max(0L, 1_50_000L - current80C)
        if (additional80C > 0L) {
            recommendations.add(
                DeductionOpportunity(
                    sectionCode = "Sec 80C",
                    title = "Chapter VI-A Investments (ELSS, EPF, PPF, Tuition Fee)",
                    category = DeductionCategory.CHAPTER_VIA_INVESTMENT,
                    maxEligibleAmount = 1_50_000L,
                    currentClaimedAmount = current80C,
                    potentialAdditionalClaim = additional80C,
                    estimatedTaxSavedRupees = (additional80C * 0.20).toLong(),
                    legalExplanation = "Exhaust the ₹1,50,000 statutory basket using ELSS Mutual Funds (3-year lock-in), EPF employee contribution, or child school tuition fees.",
                    requiredDocumentation = "ELSS statement, school fee receipts, life insurance premium receipts.",
                    isApplicableInNewRegime = false,
                    isRebate87ATarget = true
                )
            )
        }

        // 6. SECTION 80E: Higher Education Loan Interest (Unlimited 8-Year Deduction)
        if (input.hasEducationLoan || input.educationLoanInterestPaid > 0L) {
            val eligible80E = if (input.educationLoanInterestPaid > 0L) input.educationLoanInterestPaid else 60_000L
            val current80E = input.existingDeductionsClaimed["80E"] ?: 0L
            val additional80E = max(0L, eligible80E - current80E)
            if (additional80E > 0L) {
                recommendations.add(
                    DeductionOpportunity(
                        sectionCode = "Sec 80E",
                        title = "Higher Education Loan Interest (100% Unlimited Deduction)",
                        category = DeductionCategory.CHAPTER_VIA_INVESTMENT,
                        maxEligibleAmount = eligible80E,
                        currentClaimedAmount = current80E,
                        potentialAdditionalClaim = additional80E,
                        estimatedTaxSavedRupees = (additional80E * 0.20).toLong(),
                        legalExplanation = "Interest paid on loan taken for higher education of self, spouse, or children deductible for up to 8 consecutive years with NO upper ceiling.",
                        requiredDocumentation = "Bank Section 80E Interest Certificate.",
                        isApplicableInNewRegime = false,
                        isRebate87ATarget = false
                    )
                )
            }
        }

        // 7. SECTION 80GG: Rent Paid Relief for Non-HRA Filers
        if (input.paysRent && !input.receivesHraInSalary && input.annualRentPaid > 0L) {
            val max80GG = 60_000L // ₹5,000/month statutory cap
            val current80GG = input.existingDeductionsClaimed["80GG"] ?: 0L
            val additional80GG = max(0L, max80GG - current80GG)
            if (additional80GG > 0L) {
                recommendations.add(
                    DeductionOpportunity(
                        sectionCode = "Sec 80GG",
                        title = "Rent Paid Deduction for Non-HRA Employees & Freelancers",
                        category = DeductionCategory.HOME_AND_RENT,
                        maxEligibleAmount = max80GG,
                        currentClaimedAmount = current80GG,
                        potentialAdditionalClaim = additional80GG,
                        estimatedTaxSavedRupees = (additional80GG * 0.20).toLong(),
                        legalExplanation = "If you pay rent but do not receive HRA from an employer, claim up to ₹60,000/year under Section 80GG by filing Form 10BA.",
                        requiredDocumentation = "Form 10BA declaration and rent receipts.",
                        isApplicableInNewRegime = false,
                        isRebate87ATarget = true
                    )
                )
            }
        }

        // 8. SECTION 80TTA / 80TTB: Bank Interest Exemption
        val maxInterestExemption = if (input.isSeniorCitizen) 50_000L else 10_000L
        val sectionInterestCode = if (input.isSeniorCitizen) "Sec 80TTB" else "Sec 80TTA"
        val currentInterestClaimed = input.existingDeductionsClaimed[sectionInterestCode] ?: 0L
        val eligibleInterestClaim = min(maxInterestExemption, input.otherSourcesInterest)
        val additionalInterest = max(0L, eligibleInterestClaim - currentInterestClaimed)
        if (additionalInterest > 0L) {
            recommendations.add(
                DeductionOpportunity(
                    sectionCode = sectionInterestCode,
                    title = if (input.isSeniorCitizen) "Senior Citizen FD & Savings Interest Exemption (₹50,000)" else "Savings Bank Interest Exemption (₹10,000)",
                    category = DeductionCategory.CHAPTER_VIA_INVESTMENT,
                    maxEligibleAmount = maxInterestExemption,
                    currentClaimedAmount = currentInterestClaimed,
                    potentialAdditionalClaim = additionalInterest,
                    estimatedTaxSavedRupees = (additionalInterest * 0.20).toLong(),
                    legalExplanation = if (input.isSeniorCitizen) "Seniors can deduct up to ₹50,000 on both Savings and Fixed Deposit interest under Section 80TTB." else "Deduct up to ₹10,000 savings bank account interest under Section 80TTA.",
                    requiredDocumentation = "Bank passbook interest entries or Form 26AS Part F.",
                    isApplicableInNewRegime = false,
                    isRebate87ATarget = true
                )
            )
        }

        // 9. FAMILY TAX ARBITRAGE: Legal Gifting to Senior Parents
        if (input.parentsAreSeniorCitizens && grossTotalIncome > 7_00_000L) {
            recommendations.add(
                DeductionOpportunity(
                    sectionCode = "Sec 56(2)(x) & 10(13A)",
                    title = "Family Tax Arbitrage: Legal Rent / Gift to Senior Parents",
                    category = DeductionCategory.FAMILY_ARBITRAGE,
                    maxEligibleAmount = 3_00_000L,
                    currentClaimedAmount = 0L,
                    potentialAdditionalClaim = 3_00_000L,
                    estimatedTaxSavedRupees = 60_000L,
                    legalExplanation = "Gifts to parents are 100% tax-free u/s 56(2)(x). Paying rent to house-owning parents allows HRA exemption for you, while parents utilize basic exemption (₹3L) + Sec 80TTB (₹50k).",
                    requiredDocumentation = "Rent agreement, bank transfer proofs, parents' ITR acknowledging rental receipt.",
                    isApplicableInNewRegime = false,
                    isRebate87ATarget = true
                )
            )
        }

        // Baseline tax calculation (New Regime default baseline without optimizations)
        val baselineTax = computeSimpleTax(grossTotalIncome, isNewRegime = true, standardDeduction = 75_000L)

        // Optimized calculation applying CA deductions in Old Regime or New Regime
        val totalPotentialDeductions = recommendations.sumOf { it.potentialAdditionalClaim }
        val oldRegimeDeductions = 50_000L + totalPotentialDeductions // Standard deduction 50k + all discovered
        val netOldRegimeTaxable = max(0L, grossTotalIncome - oldRegimeDeductions)
        val optimizedOldTax = computeSimpleTax(netOldRegimeTaxable, isNewRegime = false, standardDeduction = 0L)

        val optimizedTax = min(baselineTax, optimizedOldTax)
        val totalSaved = max(0L, baselineTax - optimizedTax)

        // Calculate expected refund based on prepaid TDS
        val optimizedRefund = if (input.tdsPrepaid > optimizedTax) input.tdsPrepaid - optimizedTax else 0L
        val bringsBelow87A = netOldRegimeTaxable <= 5_00_000L || (grossTotalIncome - 75_000L) <= 7_00_000L

        val summary = if (optimizedRefund > 0L) {
            "CA Strategy unlocks ₹${"%,d".format(totalSaved)} in statutory tax savings, converting tax payable into a full ₹${"%,d".format(optimizedRefund)} refund directly into your bank account."
        } else {
            "CA Strategy reduces your statutory tax liability from ₹${"%,d".format(baselineTax)} down to ₹${"%,d".format(optimizedTax)}, saving you ₹${"%,d".format(totalSaved)}."
        }

        return ThinkLikeACaOptimizationReport(
            pan = input.pan,
            grossTotalIncome = grossTotalIncome,
            baselineTaxLiability = baselineTax,
            baselineTdsPrepaid = input.tdsPrepaid,
            optimizedTaxLiability = optimizedTax,
            optimizedRefundDue = optimizedRefund,
            totalTaxSavedRupees = totalSaved,
            bringsBelowRebate87A = bringsBelow87A,
            recommendations = recommendations,
            executiveSummary = summary
        )
    }

    /**
     * Compute approximate tax under standard slabs including Section 87A rebate and 4% cess.
     */
    private fun computeSimpleTax(taxableIncome: Long, isNewRegime: Boolean, standardDeduction: Long): Long {
        val net = max(0L, taxableIncome - standardDeduction)
        if (isNewRegime) {
            if (net <= 7_00_000L) return 0L // Full 87A rebate
            var tax = 0.0
            if (net > 3_00_000L) tax += min(net - 3_00_000L, 4_00_000L) * 0.05
            if (net > 7_00_000L) tax += min(net - 7_00_000L, 3_00_000L) * 0.10
            if (net > 10_00_000L) tax += min(net - 10_00_000L, 2_00_000L) * 0.15
            if (net > 12_00_000L) tax += min(net - 12_00_000L, 3_00_000L) * 0.20
            if (net > 15_00_000L) tax += (net - 15_00_000L) * 0.30
            return (tax * 1.04).toLong()
        } else {
            if (net <= 5_00_000L) return 0L // Full 87A rebate in Old Regime
            var tax = 0.0
            if (net > 2_50_000L) tax += min(net - 2_50_000L, 2_50_000L) * 0.05
            if (net > 5_00_000L) tax += min(net - 5_00_000L, 5_00_000L) * 0.20
            if (net > 10_00_000L) tax += (net - 10_00_000L) * 0.30
            return (tax * 1.04).toLong()
        }
    }
}
