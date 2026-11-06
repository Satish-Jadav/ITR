package `in`.zerotax.core.model

/**
 * Builds privacy-hardened, Verhoeff/PAN-scrubbed tax snapshots for agentic LLM reasoning.
 * Strips all personal identifiers (PAN, Aadhaar, Names, Bank accounts) while preserving
 * exact rupee amounts and headroom metrics for deterministic calculations.
 */
object TaxpayerAnonymizedSnapshotBuilder {

    fun build(
        assessmentYear: String = "2026-27",
        citizenCategory: CitizenCategory = CitizenCategory.REGULAR_BELOW_60,
        activeRegime: TaxRegime = TaxRegime.NEW_REGIME_115BAC,
        grossSalaryRupees: Long = 0L,
        claimed80CRupees: Long = 0L,
        claimed80DSelfRupees: Long = 0L,
        claimed80DParentsRupees: Long = 0L,
        claimedNps80CCD1BRupees: Long = 0L,
        claimedEmployerNps80CCD2Rupees: Long = 0L,
        claimedHraExemptionRupees: Long = 0L,
        claimedHomeLoanInterest24bRupees: Long = 0L,
        claimed80TTARupees: Long = 0L,
        totalDeductionsRupees: Long = 75_000L,
        netTaxableIncomeRupees: Long = 0L,
        tdsPaidRupees: Long = 0L,
        currentTaxLiabilityRupees: Long = 0L,
        estimatedRefundRupees: Long = 0L,
        hasCapitalGains: Boolean = false,
        hasBusinessOrFreelance: Boolean = false,
        hasForeignAssets: Boolean = false
    ): TaxpayerAnonymizedSnapshot {
        val unclaimed80C = maxOf(0L, 150_000L - claimed80CRupees)
        val unclaimedNps = maxOf(0L, 50_000L - claimedNps80CCD1BRupees)

        return TaxpayerAnonymizedSnapshot(
            assessmentYear = assessmentYear,
            ageCategory = citizenCategory,
            activeRegime = activeRegime,
            grossSalaryRupees = grossSalaryRupees,
            claimed80CRupees = claimed80CRupees,
            claimed80DSelfRupees = claimed80DSelfRupees,
            claimed80DParentsRupees = claimed80DParentsRupees,
            claimedNps80CCD1BRupees = claimedNps80CCD1BRupees,
            claimedEmployerNps80CCD2Rupees = claimedEmployerNps80CCD2Rupees,
            claimedHraExemptionRupees = claimedHraExemptionRupees,
            claimedHomeLoanInterest24bRupees = claimedHomeLoanInterest24bRupees,
            claimed80TTARupees = claimed80TTARupees,
            totalDeductionsRupees = totalDeductionsRupees,
            netTaxableIncomeRupees = netTaxableIncomeRupees,
            tdsPaidRupees = tdsPaidRupees,
            currentTaxLiabilityRupees = currentTaxLiabilityRupees,
            estimatedRefundRupees = estimatedRefundRupees,
            hasCapitalGains = hasCapitalGains,
            hasBusinessOrFreelance = hasBusinessOrFreelance,
            hasForeignAssets = hasForeignAssets,
            unclaimed80CHeadroom = unclaimed80C,
            unclaimedNpsHeadroom = unclaimedNps
        )
    }
}
