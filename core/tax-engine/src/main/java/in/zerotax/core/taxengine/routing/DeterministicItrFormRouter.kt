package in.zerotax.core.taxengine.routing

import in.zerotax.core.model.FormRoutingDecision
import in.zerotax.core.model.ItrFormType
import in.zerotax.core.model.ResidentialStatus
import in.zerotax.core.model.TaxpayerRoutingProfile

object DeterministicItrFormRouter {
    private const val ITR_1_AND_4_INCOME_CAP = 50_00_000L
    private const val AGRI_INCOME_CAP_ITR1_4 = 5_000L
    private const val SEC_44ADA_STANDARD_LIMIT = 50_00_000L
    private const val SEC_44ADA_ENHANCED_DIGITAL_LIMIT = 75_00_000L
    private const val LTCG_112A_EXEMPT_THRESHOLD = 1_25_000L

    fun routeTaxpayer(profile: TaxpayerRoutingProfile): FormRoutingDecision {
        val reasons = mutableListOf<String>()

        if (profile.requiresStatutoryAudit44AB) {
            reasons.add("SEC_44AB_STATUTORY_AUDIT_REQUIRED")
            return FormRoutingDecision(
                selectedForm = ItrFormType.UNSUPPORTED_REQUIRES_CA_AUDIT_44AB,
                routingReasonCodes = reasons,
                requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome,
                requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP
            )
        }

        if (profile.hasPresumptiveProfession44ADA && profile.declaredProfitPercentage44ADA < 50.0) {
            reasons.add("SEC_44ADA_PROFIT_BELOW_50_PERCENT_REQUIRES_AUDIT")
            return FormRoutingDecision(
                selectedForm = ItrFormType.UNSUPPORTED_REQUIRES_CA_AUDIT_44AB,
                routingReasonCodes = reasons,
                requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome,
                requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP
            )
        }

        val max44AdaTurnover = if (profile.cashReceiptsPercentage44ADA <= 5.0) {
            SEC_44ADA_ENHANCED_DIGITAL_LIMIT
        } else {
            SEC_44ADA_STANDARD_LIMIT
        }

        if (profile.hasRegularBusinessOrIntradaySpeculativeOrFnO ||
            (profile.hasPresumptiveProfession44ADA && profile.grossReceipts44ADARupees > max44AdaTurnover)
        ) {
            reasons.add("BUSINESS_OR_PROFESSION_REQUIRES_ITR3")
            return FormRoutingDecision(
                selectedForm = ItrFormType.ITR_3,
                routingReasonCodes = reasons,
                requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome,
                requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP
            )
        }

        val disqualifiedFromSimpleForms = mutableListOf<String>()
        if (profile.residentialStatus != ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT) {
            disqualifiedFromSimpleForms.add("NON_ROR_RESIDENCY_STATUS")
        }
        if (profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP) {
            disqualifiedFromSimpleForms.add("TOTAL_INCOME_EXCEEDS_50_LAKHS")
        }
        if (profile.housePropertyCount > 1) {
            disqualifiedFromSimpleForms.add("MULTIPLE_HOUSE_PROPERTIES")
        }
        if (profile.holdsForeignAssetsOrForeignIncome) {
            disqualifiedFromSimpleForms.add("HOLDS_FOREIGN_ASSETS_SCHEDULE_FA_MANDATORY")
        }
        if (profile.isCompanyDirector) {
            disqualifiedFromSimpleForms.add("TAXPAYER_IS_COMPANY_DIRECTOR")
        }
        if (profile.holdsUnlistedEquityShares) {
            disqualifiedFromSimpleForms.add("HOLDS_UNLISTED_EQUITY_SHARES")
        }
        if (profile.agriculturalIncomeRupees > AGRI_INCOME_CAP_ITR1_4) {
            disqualifiedFromSimpleForms.add("AGRICULTURAL_INCOME_EXCEEDS_5000")
        }
        if (profile.hasBroughtForwardOrCarriedForwardLosses) {
            disqualifiedFromSimpleForms.add("HAS_BROUGHT_OR_CARRIED_FORWARD_LOSSES")
        }
        if (profile.hasOtherCapitalGains ||
            (profile.hasCapitalGainsOrLosses && profile.equity112ALtcgRupees > LTCG_112A_EXEMPT_THRESHOLD)
        ) {
            disqualifiedFromSimpleForms.add("TAXABLE_CAPITAL_GAINS_REQUIRES_SCHEDULE_CG")
        }

        if (disqualifiedFromSimpleForms.isNotEmpty()) {
            val targetForm = if (profile.hasPresumptiveProfession44ADA) ItrFormType.ITR_3 else ItrFormType.ITR_2
            return FormRoutingDecision(
                selectedForm = targetForm,
                routingReasonCodes = disqualifiedFromSimpleForms,
                requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome &&
                    profile.residentialStatus == ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,
                requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP
            )
        }

        return if (profile.hasPresumptiveProfession44ADA) {
            FormRoutingDecision(
                selectedForm = ItrFormType.ITR_4_SUGAM,
                routingReasonCodes = listOf("PRESUMPTIVE_44ADA_WITHIN_ELIGIBLE_LIMITS"),
                requiresScheduleFA = false,
                requiresScheduleAL = false
            )
        } else {
            FormRoutingDecision(
                selectedForm = ItrFormType.ITR_1_SAHAJ,
                routingReasonCodes = listOf("SALARIED_AND_OTHER_SOURCES_WITHIN_SAHAJ_LIMITS"),
                requiresScheduleFA = false,
                requiresScheduleAL = false
            )
        }
    }
}
