package in.zerotax.core.taxengine.reconciliation

import in.zerotax.core.model.EmployerReconciliationItem
import in.zerotax.core.model.EmployerSalarySource
import in.zerotax.core.model.ReconciledSalarySummary
import in.zerotax.core.model.SourceKind
import kotlin.math.abs
import kotlin.math.min

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
}
