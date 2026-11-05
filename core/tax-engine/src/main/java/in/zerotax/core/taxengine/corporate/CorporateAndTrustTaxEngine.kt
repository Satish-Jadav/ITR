package `in`.zerotax.core.taxengine.corporate

import `in`.zerotax.core.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * High-performance deterministic statutory tax engine for Non-Individual Corporate Entities:
 * 1. ITR-5: Partnership Firms & LLPs (Section 40(b), AMT 115JC)
 * 2. ITR-6: Domestic & Foreign Companies (Section 115BAA 22%, 115BAB 15%, Normal 25%/30%, MAT 115JB)
 * 3. ITR-7: Charitable & Religious Trusts, NGOs, Political Parties (Sections 11, 12, 13, 13A, 115BBI)
 */
object CorporateAndTrustTaxEngine {

    // ========================================================================
    // 1. ITR-5: PARTNERSHIP FIRMS & LLPs (SECTIONS 40(b), 115JC)
    // ========================================================================
    fun computeItr5FirmTax(input: Itr5FirmInput): Itr5FirmResult {
        val notes = mutableListOf<String>()

        // 1. Partner Interest on Capital u/s 40(b)(iv) - Cap at 12% simple interest p.a.
        val maxAllowableInterestRate = 12.0
        val effectiveInterestRate = min(input.partnerInterestRatePercent, maxAllowableInterestRate)
        val maxAllowableInterest = ((input.partnerCapitalTotalRupees * (effectiveInterestRate / 100.0))).roundToLong()
        val admissibleInterest = min(input.partnerInterestOnCapitalClaimed, maxAllowableInterest)
        val interestDisallowed = max(0L, input.partnerInterestOnCapitalClaimed - admissibleInterest)

        if (interestDisallowed > 0L) {
            notes.add("Section 40(b)(iv) Disallowance: ₹$interestDisallowed excess interest on capital (capped at 12% p.a.).")
        } else {
            notes.add("Partner interest on capital is fully compliant with Section 40(b)(iv) (within 12% p.a.).")
        }

        // 2. Adjusted Book Profit for Remuneration Calculation
        // Book profit = profit before remuneration + disallowed interest
        val adjustedBookProfit = max(0L, input.bookProfitBeforePartnerPayments + interestDisallowed)

        // 3. Partner Remuneration Limit u/s 40(b)(v)
        // First 3,00,000 of book profit: ₹1,50,000 or 90% of book profit (whichever is greater)
        // Balance book profit: 60%
        val maxRemuneration = if (adjustedBookProfit <= 3_00_000L) {
            max(1_50_000L, (adjustedBookProfit * 0.90).roundToLong())
        } else {
            val onFirst3L = (3_00_000L * 0.90).roundToLong() // 2,70,000
            val onBalance = ((adjustedBookProfit - 3_00_000L) * 0.60).roundToLong()
            onFirst3L + onBalance
        }

        val admissibleRemuneration = if (input.isWorkingPartnerAuthorizedByDeed) {
            min(input.partnerRemunerationClaimed, maxRemuneration)
        } else {
            0L // Unauthorized by deed
        }
        val remunerationDisallowed = max(0L, input.partnerRemunerationClaimed - admissibleRemuneration)

        if (remunerationDisallowed > 0L) {
            notes.add("Section 40(b)(v) Disallowance: ₹$remunerationDisallowed excess partner remuneration (statutory cap was ₹$maxRemuneration).")
        }

        // 4. Taxable Business Profit
        val taxableBusinessProfit = max(0L, adjustedBookProfit - admissibleRemuneration)
        val grossTotalIncome = taxableBusinessProfit + input.otherIncomeRupees
        val totalTaxableIncome = max(0L, grossTotalIncome - input.deductionsChapterVIAExcept10AA)

        // 5. Normal Tax @ Flat 30%
        val normalBaseTax = (totalTaxableIncome * 0.30).roundToLong()

        // Surcharge: 12% if Total Income > ₹1 Crore (with marginal relief)
        val surchargeRate = if (totalTaxableIncome > 1_00_00_000L) 0.12 else 0.0
        var surcharge = (normalBaseTax * surchargeRate).roundToLong()

        // Marginal relief for income slightly exceeding ₹1 Crore
        if (totalTaxableIncome > 1_00_00_000L) {
            val taxOn1Cr = (1_00_00_000L * 0.30).roundToLong()
            val excessIncome = totalTaxableIncome - 1_00_00_000L
            val maxTaxPayable = taxOn1Cr + excessIncome
            if ((normalBaseTax + surcharge) > maxTaxPayable) {
                surcharge = max(0L, maxTaxPayable - normalBaseTax)
                notes.add("Section 112 Marginal Relief applied to Surcharge: Saved excess surcharge above ₹1 Cr threshold.")
            }
        }

        val cess = ((normalBaseTax + surcharge) * 0.04).roundToLong()
        val totalNormalTax = normalBaseTax + surcharge + cess

        // 6. Alternate Minimum Tax (AMT u/s 115JC @ 18.5%)
        val adjustedTotalIncome = totalTaxableIncome + input.deductionsChapterVIAExcept10AA
        val amtBase = (adjustedTotalIncome * 0.185).roundToLong()
        val amtSurcharge = (amtBase * surchargeRate).roundToLong()
        val amtCess = ((amtBase + amtSurcharge) * 0.04).roundToLong()
        val totalAmt = amtBase + amtSurcharge + amtCess

        val isAmtApplicable = totalAmt > totalNormalTax
        val finalTax = if (isAmtApplicable) totalAmt else totalNormalTax

        if (isAmtApplicable) {
            val amtCredit = totalAmt - totalNormalTax
            notes.add("AMT Triggered: Tax payable under Section 115JC is ₹$totalAmt. Generated AMT Credit of ₹$amtCredit carry-forward u/s 115JD (valid 15 years).")
        }

        return Itr5FirmResult(
            admissibleInterestSection40b = admissibleInterest,
            interestDisallowanceSection40b = interestDisallowed,
            maxPermissibleRemuneration40b = maxRemuneration,
            admissibleRemuneration40b = admissibleRemuneration,
            remunerationDisallowance40b = remunerationDisallowed,
            taxableBusinessProfit = taxableBusinessProfit,
            grossTotalIncomeRupees = grossTotalIncome,
            totalTaxableIncomeRupees = totalTaxableIncome,
            normalTaxAt30Percent = normalBaseTax,
            surchargeRupees = surcharge,
            healthAndEducationCessRupees = cess,
            totalNormalTaxPayableRupees = totalNormalTax,
            alternateMinimumTax115JC = totalAmt,
            isAmtPayable = isAmtApplicable,
            finalTaxPayableRupees = finalTax,
            statutoryNotes = notes
        )
    }

    // ========================================================================
    // 2. ITR-6: CORPORATE TAX & MAT (SECTIONS 115BAA, 115BAB, 115JB)
    // ========================================================================
    fun computeItr6CorporateTax(input: Itr6CorporateInput): Itr6CorporateResult {
        val notes = mutableListOf<String>()

        val (baseRate, surchargeRate, isMatExempt) = when {
            input.isNewManufacturingCompany115BAB -> {
                notes.add("Opted for Section 115BAB: Concessional 15% rate for new manufacturing companies (Effective rate 17.16%). Exempt from MAT.")
                Triple(0.15, 0.10, true)
            }
            input.hasOptedForSection115BAA -> {
                notes.add("Opted for Section 115BAA: Concessional 22% rate for domestic companies (Effective rate 25.168%). Exempt from MAT.")
                Triple(0.22, 0.10, true)
            }
            input.turnoverInPriorYearExceeding400Cr -> {
                notes.add("Normal Corporate Tax (Turnover > ₹400 Cr): 30% base rate. Subject to Section 115JB MAT.")
                val sur = when {
                    input.taxableIncomeNormalRupees > 10_00_00_000L -> 0.12
                    input.taxableIncomeNormalRupees > 1_00_00_000L -> 0.07
                    else -> 0.0
                }
                Triple(0.30, sur, false)
            }
            else -> {
                notes.add("Normal Corporate Tax (Turnover <= ₹400 Cr): 25% base rate. Subject to Section 115JB MAT.")
                val sur = when {
                    input.taxableIncomeNormalRupees > 10_00_00_000L -> 0.12
                    input.taxableIncomeNormalRupees > 1_00_00_000L -> 0.07
                    else -> 0.0
                }
                Triple(0.25, sur, false)
            }
        }

        // Normal tax computation
        val normalBaseTax = (input.taxableIncomeNormalRupees * baseRate).roundToLong()
        val normalSurcharge = (normalBaseTax * surchargeRate).roundToLong()
        val normalCess = ((normalBaseTax + normalSurcharge) * 0.04).roundToLong()
        val totalNormalTax = normalBaseTax + normalSurcharge + normalCess

        // MAT computation u/s 115JB (15% on book profits)
        val matRate = 0.15
        val matBaseTax = if (!isMatExempt) (input.bookProfitsMatSection115JB * matRate).roundToLong() else 0L
        val matSurcharge = (matBaseTax * surchargeRate).roundToLong()
        val matCess = ((matBaseTax + matSurcharge) * 0.04).roundToLong()
        val totalMat = matBaseTax + matSurcharge + matCess

        val isMatPayableHigher = !isMatExempt && (totalMat > totalNormalTax)

        var matCreditGen = 0L
        var matCreditUtil = 0L
        var finalTax = totalNormalTax

        if (!isMatExempt) {
            if (isMatPayableHigher) {
                finalTax = totalMat
                matCreditGen = totalMat - totalNormalTax
                notes.add("MAT Higher than Normal Tax: Corporate pays ₹$totalMat under Section 115JB. ₹$matCreditGen MAT Credit generated u/s 115JAA.")
            } else if (input.broughtForwardMatCredit115JAA > 0L) {
                // Can utilize MAT credit up to (Normal Tax - MAT Tax)
                val maxUtilizable = max(0L, totalNormalTax - totalMat)
                matCreditUtil = min(input.broughtForwardMatCredit115JAA, maxUtilizable)
                finalTax = totalNormalTax - matCreditUtil
                notes.add("Utilized ₹$matCreditUtil brought-forward MAT Credit u/s 115JAA against normal tax liability.")
            }
        }

        val closingMatCredit = input.broughtForwardMatCredit115JAA - matCreditUtil + matCreditGen

        return Itr6CorporateResult(
            applicableTaxRatePercent = baseRate * 100.0,
            applicableSurchargePercent = surchargeRate * 100.0,
            normalTaxBeforeCessRupees = normalBaseTax,
            normalSurchargeRupees = normalSurcharge,
            normalCessRupees = normalCess,
            totalNormalTaxRupees = totalNormalTax,
            matTaxRatePercent = if (isMatExempt) 0.0 else 15.0,
            matBookProfitsTaxRupees = totalMat,
            isMatApplicable = !isMatExempt,
            isMatPayableHigherThanNormal = isMatPayableHigher,
            matCreditGeneratedCurrentYear = matCreditGen,
            matCreditUtilizedCurrentYear = matCreditUtil,
            closingMatCreditCarryForward = closingMatCredit,
            totalCorporateTaxPayableRupees = finalTax,
            statutoryNotes = notes
        )
    }

    // ========================================================================
    // 3. ITR-7: TRUSTS, NGOS, POLITICAL PARTIES (SECTIONS 11, 12, 13, 13A, 115BBI)
    // ========================================================================
    fun computeItr7TrustTax(input: Itr7TrustInput): Itr7TrustResult {
        val notes = mutableListOf<String>()

        // Case 1: Political Parties u/s 13A
        if (input.entityType == CorporateEntityType.POLITICAL_PARTY_13A) {
            notes.add("Section 13A Political Party: 100% tax exemption on voluntary contributions (subject to statutory CA audit and RPA 1951 Section 29C filing).")
            return Itr7TrustResult(
                grossReceiptsForApplication = input.totalVoluntaryContributionsRupees,
                statutory15PercentAccumulationRupees = 0L,
                mandatory85PercentApplicationRequired = 0L,
                totalEffectiveApplicationClaimed = input.charitableApplicationInIndiaRupees,
                applicationShortfallRupees = 0L,
                section13ViolationTaxAtMMR = 0L,
                normalTaxOnShortfallRupees = 0L,
                totalTaxPayableRupees = 0L,
                isCompletelyTaxExempt = true,
                statutoryNotes = notes
            )
        }

        // Case 2: Charitable & Religious Trusts u/s 11 & 12
        // Corpus donations u/s 11(1)(d) are 100% exempt from the application mandate
        val grossForApplication = input.totalVoluntaryContributionsRupees + input.otherIncomeRupees

        // 15% statutory accumulation u/s 11(1)(a)
        val statutory15Percent = (grossForApplication * 0.15).roundToLong()

        // 85% mandatory application required
        val mandatory85Percent = grossForApplication - statutory15Percent

        // Effective application = actual in India + Form 9A (deemed) + Form 10 (accumulation)
        val totalEffectiveApplication = input.charitableApplicationInIndiaRupees +
                input.deemedApplicationForm9ARupees +
                input.accumulatedUnderForm10Rupees

        val shortfall = max(0L, mandatory85Percent - totalEffectiveApplication)

        // Violations u/s 13 (private benefits to trustees or unapproved investments)
        // Taxed at Maximum Marginal Rate (MMR = 30% + 12% surcharge + 4% cess = 34.944%) u/s 115BBI
        val mmrTaxRate = 0.34944
        val violationTax = (input.unapprovedSection13ViolationsRupees * mmrTaxRate).roundToLong()

        // Normal slab tax on application shortfall
        val normalTaxOnShortfall = (shortfall * 0.30 * 1.04).roundToLong()

        val totalTax = violationTax + normalTaxOnShortfall
        val isExempt = totalTax == 0L

        if (isExempt) {
            notes.add("100% Tax-Exempt u/s 11(1): Trust met the mandatory 85% application requirement (₹${mandatory85Percent}) with zero Section 13 violations.")
        } else {
            if (shortfall > 0L) {
                notes.add("Application Shortfall: Trust applied ₹$totalEffectiveApplication against mandatory ₹$mandatory85Percent. Shortfall of ₹$shortfall is taxable.")
            }
            if (input.unapprovedSection13ViolationsRupees > 0L) {
                notes.add("Section 115BBI MMR Tax: ₹${input.unapprovedSection13ViolationsRupees} in Section 13 violations taxed at Maximum Marginal Rate (34.944%).")
            }
        }

        return Itr7TrustResult(
            grossReceiptsForApplication = grossForApplication,
            statutory15PercentAccumulationRupees = statutory15Percent,
            mandatory85PercentApplicationRequired = mandatory85Percent,
            totalEffectiveApplicationClaimed = totalEffectiveApplication,
            applicationShortfallRupees = shortfall,
            section13ViolationTaxAtMMR = violationTax,
            normalTaxOnShortfallRupees = normalTaxOnShortfall,
            totalTaxPayableRupees = totalTax,
            isCompletelyTaxExempt = isExempt,
            statutoryNotes = notes
        )
    }
}
