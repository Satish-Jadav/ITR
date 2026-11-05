package `in`.zerotax.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class CorporateEntityType(val code: Char, val label: String, val defaultItr: ItrFormType) {
    PARTNERSHIP_FIRM('F', "Partnership Firm (Partnership Act, 1932)", ItrFormType.ITR_5),
    LIMITED_LIABILITY_PARTNERSHIP('F', "Limited Liability Partnership (LLP Act, 2008)", ItrFormType.ITR_5),
    ASSOCIATION_OF_PERSONS('A', "Association of Persons (AOP) / Body of Individuals (BOI)", ItrFormType.ITR_5),
    DOMESTIC_COMPANY_115BAA('C', "Domestic Company u/s 115BAA (Concessional 22%)", ItrFormType.ITR_6),
    DOMESTIC_COMPANY_115BAB('C', "New Manufacturing Company u/s 115BAB (Concessional 15%)", ItrFormType.ITR_6),
    DOMESTIC_COMPANY_NORMAL('C', "Domestic Company under Normal Provisions (25% / 30%)", ItrFormType.ITR_6),
    FOREIGN_COMPANY('C', "Foreign Company / Branch Office (40%)", ItrFormType.ITR_6),
    CHARITABLE_RELIGIOUS_TRUST_12AB('T', "Charitable / Religious Trust u/s 12AB & 11", ItrFormType.ITR_7),
    POLITICAL_PARTY_13A('T', "Political Party registered u/s 29A RPA 1951 (Sec 13A)", ItrFormType.ITR_7),
    SCIENTIFIC_RESEARCH_INSTITUTION('T', "Research Institution / College / Hospital u/s 10(23C) / 35", ItrFormType.ITR_7)
}

/**
 * Feature ITR-5: Firm & LLP Tax Computation Input u/s 40(b), 115JC
 */
@Serializable
data class Itr5FirmInput(
    val firmPan: String,
    val entityType: CorporateEntityType = CorporateEntityType.LIMITED_LIABILITY_PARTNERSHIP,
    val grossTurnoverRupees: Long = 0L,
    val bookProfitBeforePartnerPayments: Long = 0L,
    val partnerCapitalTotalRupees: Long = 0L,
    val partnerInterestOnCapitalClaimed: Long = 0L,
    val partnerInterestRatePercent: Double = 12.0,
    val partnerRemunerationClaimed: Long = 0L,
    val isWorkingPartnerAuthorizedByDeed: Boolean = true,
    val otherIncomeRupees: Long = 0L,
    val deductionsChapterVIAExcept10AA: Long = 0L
)

@Serializable
data class Itr5FirmResult(
    val admissibleInterestSection40b: Long,
    val interestDisallowanceSection40b: Long,
    val maxPermissibleRemuneration40b: Long,
    val admissibleRemuneration40b: Long,
    val remunerationDisallowance40b: Long,
    val taxableBusinessProfit: Long,
    val grossTotalIncomeRupees: Long,
    val totalTaxableIncomeRupees: Long,
    val normalTaxAt30Percent: Long,
    val surchargeRupees: Long,
    val healthAndEducationCessRupees: Long,
    val totalNormalTaxPayableRupees: Long,
    val alternateMinimumTax115JC: Long,
    val isAmtPayable: Boolean,
    val finalTaxPayableRupees: Long,
    val statutoryNotes: List<String>
)

/**
 * Feature ITR-6: Corporate Tax & MAT Computation Input u/s 115BAA, 115BAB, 115JB
 */
@Serializable
data class Itr6CorporateInput(
    val companyPan: String,
    val companyName: String = "",
    val turnoverInPriorYearExceeding400Cr: Boolean = false,
    val isNewManufacturingCompany115BAB: Boolean = false,
    val hasOptedForSection115BAA: Boolean = true,
    val grossRevenueRupees: Long = 0L,
    val taxableIncomeNormalRupees: Long = 0L,
    val bookProfitsMatSection115JB: Long = 0L,
    val broughtForwardMatCredit115JAA: Long = 0L
)

@Serializable
data class Itr6CorporateResult(
    val applicableTaxRatePercent: Double,
    val applicableSurchargePercent: Double,
    val normalTaxBeforeCessRupees: Long,
    val normalSurchargeRupees: Long,
    val normalCessRupees: Long,
    val totalNormalTaxRupees: Long,
    val matTaxRatePercent: Double,
    val matBookProfitsTaxRupees: Long,
    val isMatApplicable: Boolean,
    val isMatPayableHigherThanNormal: Boolean,
    val matCreditGeneratedCurrentYear: Long,
    val matCreditUtilizedCurrentYear: Long,
    val closingMatCreditCarryForward: Long,
    val totalCorporateTaxPayableRupees: Long,
    val statutoryNotes: List<String>
)

/**
 * Feature ITR-7: Trust, Political Party & Section 11/12/13A Computation Input
 */
@Serializable
data class Itr7TrustInput(
    val trustPan: String,
    val trustName: String = "",
    val entityType: CorporateEntityType = CorporateEntityType.CHARITABLE_RELIGIOUS_TRUST_12AB,
    val totalVoluntaryContributionsRupees: Long = 0L,
    val corpusDonationsExemptSec11_1_d: Long = 0L,
    val otherIncomeRupees: Long = 0L,
    val charitableApplicationInIndiaRupees: Long = 0L,
    val deemedApplicationForm9ARupees: Long = 0L,
    val accumulatedUnderForm10Rupees: Long = 0L,
    val unapprovedSection13ViolationsRupees: Long = 0L
)

@Serializable
data class Itr7TrustResult(
    val grossReceiptsForApplication: Long,
    val statutory15PercentAccumulationRupees: Long,
    val mandatory85PercentApplicationRequired: Long,
    val totalEffectiveApplicationClaimed: Long,
    val applicationShortfallRupees: Long,
    val section13ViolationTaxAtMMR: Long,
    val normalTaxOnShortfallRupees: Long,
    val totalTaxPayableRupees: Long,
    val isCompletelyTaxExempt: Boolean,
    val statutoryNotes: List<String>
)
