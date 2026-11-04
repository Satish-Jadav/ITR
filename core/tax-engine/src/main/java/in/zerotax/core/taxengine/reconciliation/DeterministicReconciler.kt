package `in`.zerotax.core.taxengine.reconciliation

import `in`.zerotax.core.model.EmployerReconciliationItem
import `in`.zerotax.core.model.EmployerSalarySource
import `in`.zerotax.core.model.ReconciledSalarySummary
import `in`.zerotax.core.model.SftAndTcs206CEntry
import `in`.zerotax.core.model.SourceKind
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class InterestAndDividendReconciliationResult(
    val savingsInterestTaxable: Long,
    val fdInterestTaxable: Long,
    val grossDividends: Long,
    val allowableSec57iExpense: Long,
    val netTaxableDividends: Long,
    val totalOtherSourcesReconciled: Long,
    val highWaterMarkApplied: Boolean
)

data class TcsAndSftAuditResult(
    val totalTcsClaimedInScheduleTcs: Long,
    val tcsForeignRemittance206C1G: Long,
    val tcsMotorVehicle206C1F: Long,
    val highValueSftSpendTotal: Long,
    val isDisproportionateSpendRisk: Boolean,
    val auditRadarAlerts: List<String>
)

data class EpfRule9DReconciliationResult(
    val exemptEpfInterestScheduleEi: Long,
    val taxableEpfInterestScheduleOs: Long,
    val tdsCreditClaimed194A: Long
)

data class MinorChildClubbingResult(
    val grossMinorIncome: Long,
    val exemptPerChildSec10_32: Long,
    val netClubbedIncomeSec64_1A: Long
)

object DeterministicReconciler {
    /**
     * Verifies that Gross Salary - Sec 10 Exemptions - Sec 16 Deductions == Net Taxable Salary
     * within the strict statutory ±₹5 tolerance firewall.
     */
    fun passesForm16SanityCheck(record: EmployerSalarySource, toleranceRupees: Long = 5L): Boolean {
        val computedNet = record.grossSalary - record.sec10Exemptions - record.sec16DeductionsClaimedInDoc
        return abs(computedNet - record.netTaxableSalaryInDoc) <= toleranceRupees
    }

    /**
     * Reconciles multiple Form 16 / AIS employer entries using TAN de-duplication
     * and strictly caps claimable TDS to the Form 26AS deposited amount.
     */
    fun reconcileSalaries(
        sources: List<EmployerSalarySource>,
        form26AsTdsByTan: Map<String, Long>
    ): ReconciledSalarySummary {
        val normalized26As = form26AsTdsByTan.mapKeys { it.key.trim().uppercase() }
        val groupedByTan = sources.groupBy { it.employerTan.trim().uppercase() }
        val reconciledItems = mutableListOf<EmployerReconciliationItem>()

        for ((tan, records) in groupedByTan) {
            val primary = records.firstOrNull { it.source == SourceKind.FORM_16 } ?: records.first()
            val deposited26AsTds = normalized26As[tan] ?: 0L
            val allowedTds = min(primary.tdsReported, deposited26AsTds)
            val discrepancy = primary.tdsReported > deposited26AsTds
            val unreflectedTds = if (discrepancy) primary.tdsReported - deposited26AsTds else 0L

            reconciledItems.add(
                EmployerReconciliationItem(
                    employerTan = tan,
                    employerName = primary.employerName,
                    selectedSourceKind = primary.source,
                    grossSalary = primary.grossSalary,
                    sec10Exemptions = primary.sec10Exemptions,
                    sec16Deductions = primary.sec16DeductionsClaimedInDoc,
                    netSalary = primary.netTaxableSalaryInDoc,
                    tdsReportedInSource = primary.tdsReported,
                    tdsIn26As = deposited26AsTds,
                    tdsAllowed = allowedTds,
                    hasDiscrepancy = discrepancy,
                    unreflectedTdsAmount = unreflectedTds
                )
            )
        }

        return ReconciledSalarySummary(
            employers = reconciledItems,
            aggregateGrossSalary = reconciledItems.sumOf { it.grossSalary },
            aggregateSec10Exemptions = reconciledItems.sumOf { it.sec10Exemptions },
            aggregateNetTaxableSalary = reconciledItems.sumOf { it.netSalary },
            aggregateTdsAllowed = reconciledItems.sumOf { it.tdsAllowed },
            totalDiscrepancyAmount = reconciledItems.sumOf { it.unreflectedTdsAmount }
        )
    }

    /**
     * Reconciles Interest and Domestic Dividend income taking the deterministic high-water mark
     * (max(AIS, 26AS, AA)) to eliminate Sec 143(1)(a) discrepancy notices, and enforces
     * the Proviso to Sec 57(i) capping interest expense at 20% of gross dividend income.
     */
    fun reconcileInterestAndDividends(
        aisSft005Savings: Long,
        aisSft005Fd: Long,
        form26As194A: Long,
        aaBankSavings: Long,
        aaBankFd: Long,
        aisDividends: Long,
        marginInterestSec57i: Long
    ): InterestAndDividendReconciliationResult {
        val reconciledSavings = max(aisSft005Savings, aaBankSavings)
        val reconciledFd = max(aisSft005Fd, max(form26As194A, aaBankFd))

        val isHighWaterMarkApplied = (reconciledSavings > aaBankSavings) || (reconciledFd > form26As194A)

        // Proviso to Sec 57(i): Expense deduction on dividend income capped strictly at 20% of gross dividend
        val maxAllowable57i = (aisDividends * 0.20).roundToLong()
        val allowable57i = min(marginInterestSec57i, maxAllowable57i)
        val netDividends = max(0L, aisDividends - allowable57i)

        val totalOtherSources = reconciledSavings + reconciledFd + netDividends

        return InterestAndDividendReconciliationResult(
            savingsInterestTaxable = reconciledSavings,
            fdInterestTaxable = reconciledFd,
            grossDividends = aisDividends,
            allowableSec57iExpense = allowable57i,
            netTaxableDividends = netDividends,
            totalOtherSourcesReconciled = totalOtherSources,
            highWaterMarkApplied = isHighWaterMarkApplied
        )
    }

    /**
     * Sweeps TCS u/s 206C(1G) (foreign remittance/travel) and 206C(1F) (motor vehicle purchase)
     * into Schedule TCS so no refundable tax credits are left unclaimed, while alerting
     * if high-value SFT transactions appear disproportionate to declared gross income.
     */
    fun sweepTcsAndHighValueSftCredits(
        entries: List<SftAndTcs206CEntry>,
        declaredGrossTotalIncome: Long
    ): TcsAndSftAuditResult {
        var tcs1G = 0L
        var tcs1F = 0L
        var totalTcs = 0L
        var highValueSftSpend = 0L
        val alerts = mutableListOf<String>()

        for (entry in entries) {
            val code = entry.sectionOrSftCode.trim().uppercase()
            when {
                code.contains("206C(1G)") || code.contains("206C_1G") -> {
                    tcs1G += entry.taxCollectedOrPaid
                    totalTcs += entry.taxCollectedOrPaid
                }
                code.contains("206C(1F)") || code.contains("206C_1F") -> {
                    tcs1F += entry.taxCollectedOrPaid
                    totalTcs += entry.taxCollectedOrPaid
                }
                code.startsWith("SFT") -> {
                    highValueSftSpend += entry.grossAmount
                    if (code == "SFT-012" && entry.grossAmount > 10_00_000L) {
                        alerts.add("High-value credit card spend of ₹${entry.grossAmount} detected (SFT-012).")
                    } else if (code in listOf("SFT-003", "SFT-004") && entry.grossAmount > 30_00_000L) {
                        alerts.add("High-value immovable property transaction of ₹${entry.grossAmount} detected (${entry.sectionOrSftCode}).")
                    }
                }
            }
        }

        val isDisproportionate = highValueSftSpend > 0L && highValueSftSpend > declaredGrossTotalIncome
        if (isDisproportionate) {
            alerts.add("Total SFT spends (₹$highValueSftSpend) exceed declared Gross Total Income (₹$declaredGrossTotalIncome). Ensure documented source of funds.")
        }

        return TcsAndSftAuditResult(
            totalTcsClaimedInScheduleTcs = totalTcs,
            tcsForeignRemittance206C1G = tcs1G,
            tcsMotorVehicle206C1F = tcs1F,
            highValueSftSpendTotal = highValueSftSpend,
            isDisproportionateSpendRisk = isDisproportionate,
            auditRadarAlerts = alerts
        )
    }

    /**
     * Splits EPF interest on employee contributions exceeding ₹2.5L u/s Rule 9D into
     * exempt Schedule EI and taxable Schedule OS.
     */
    fun reconcileRule9DEpfInterest(
        totalEmployeeContribution: Long,
        totalInterestAccrued: Long,
        tdsReported194A: Long
    ): EpfRule9DReconciliationResult {
        val nonTaxableThreshold = 2_50_000L
        if (totalEmployeeContribution <= nonTaxableThreshold) {
            return EpfRule9DReconciliationResult(
                exemptEpfInterestScheduleEi = totalInterestAccrued,
                taxableEpfInterestScheduleOs = 0L,
                tdsCreditClaimed194A = tdsReported194A
            )
        }
        val excessFraction = (totalEmployeeContribution - nonTaxableThreshold).toDouble() / totalEmployeeContribution
        val taxableInterest = (totalInterestAccrued * excessFraction).roundToLong()
        val exemptInterest = max(0L, totalInterestAccrued - taxableInterest)

        return EpfRule9DReconciliationResult(
            exemptEpfInterestScheduleEi = exemptInterest,
            taxableEpfInterestScheduleOs = taxableInterest,
            tdsCreditClaimed194A = tdsReported194A
        )
    }

    /**
     * Computes minor child income clubbed under Section 64(1A) with the statutory
     * ₹1,500 exemption per child under Section 10(32).
     */
    fun computeMinorChildClubbingSec64_1A(
        minorIncomesPerChild: List<Long>
    ): MinorChildClubbingResult {
        var totalGross = 0L
        var totalExempt = 0L
        var totalNet = 0L

        for (inc in minorIncomesPerChild) {
            totalGross += inc
            val exempt = min(inc, 1_500L)
            totalExempt += exempt
            totalNet += max(0L, inc - exempt)
        }

        return MinorChildClubbingResult(
            grossMinorIncome = totalGross,
            exemptPerChildSec10_32 = totalExempt,
            netClubbedIncomeSec64_1A = totalNet
        )
    }

    /**
     * Generates a formal, statutory payroll discrepancy follow-up letter citing Sections 199, 201(1A),
     * and Section 205 of the Income-tax Act, 1961 alongside the Delhi High Court binding precedent.
     */
    fun generateEmployerTdsDiscrepancyNotice(
        item: EmployerReconciliationItem,
        pan: String,
        employeeName: String,
        assessmentYear: String
    ): String {
        return """
            # URGENT: Statutory Follow-Up on Form 26AS TDS Deposit Mismatch
            
            **To:** Payroll & Accounts Department, ${item.employerName}  
            **TAN:** ${item.employerTan}  
            **From:** $employeeName (PAN: $pan)  
            **Date:** ${java.time.LocalDate.now()}  
            **Subject:** Discrepancy between Form 16 TDS and Form 26AS Part A for AY $assessmentYear
            
            Dear Payroll Team,
            
            During the preparation of my Income Tax Return for Assessment Year $assessmentYear, a statutory discrepancy was detected between the tax deducted at source by your organization and the credits reflected in the Income Tax Department's Form 26AS:
            
            - **Employer Name:** ${item.employerName} (TAN: ${item.employerTan})
            - **Gross Salary Reported:** ₹${item.grossSalary}
            - **TDS Deducted in Form 16 / Payslips:** ₹${item.tdsReportedInSource}
            - **TDS Deposited & Visible in Form 26AS Part A:** ₹${item.tdsIn26As}
            - **Unreflected Shortfall in Form 26AS:** ₹${item.unreflectedTdsAmount}
            
            ### Statutory Legal References:
            1. **Section 205 of the Income-tax Act, 1961:**
               *"Where tax is deductible at the source under sections 192 to 194, the assessee shall not be called upon to pay the tax himself to the extent to which tax has been deducted from that income."*
            2. **CBDT Instruction No. 275/29/2014-IT(B) & Delhi High Court Precedent (*Court On Its Own Motion vs. CIT* [2013] 352 ITR 273):**
               The Income Tax Department and CPC Bengaluru are statutorily barred from raising demands or enforcing recovery upon the employee for TDS deducted by an employer but not deposited into the Central Government treasury.
            3. **Section 201(1A) & Form 24Q Correction:**
               As the deductor, the responsibility to deposit deducted amounts and furnish accurate quarterly Form 24Q statements rests solely with your organization.
            
            Kindly file a **Revised / Correction Form 24Q Statement** with NSDL/TIN within 7 business days to update my PAN's Form 26AS ledger and prevent automated CPC defect proceedings under Section 139(9).
            
            Sincerely,  
            **$employeeName**  
            PAN: $pan
        """.trimIndent()
    }
}
