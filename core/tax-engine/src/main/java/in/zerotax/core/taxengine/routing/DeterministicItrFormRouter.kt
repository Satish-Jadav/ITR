package `in`.zerotax.core.taxengine.routing

import `in`.zerotax.core.model.FormRoutingDecision
import `in`.zerotax.core.model.ItrFormType
import `in`.zerotax.core.model.ResidentialStatus
import `in`.zerotax.core.model.TaxpayerRoutingProfile

object DeterministicItrFormRouter {
    private const val ITR_1_AND_4_INCOME_CAP = 50_00_000L
    private const val AGRI_INCOME_CAP_ITR1_4 = 5_000L
    private const val SEC_44ADA_STANDARD_LIMIT = 50_00_000L
    private const val SEC_44ADA_ENHANCED_DIGITAL_LIMIT = 75_00_000L
    private const val SEC_44AD_STANDARD_LIMIT = 2_00_00_000L
    private const val SEC_44AD_ENHANCED_DIGITAL_LIMIT = 3_00_00_000L
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

        // Gate 2B: Section 44ADA Presumptive Profession Audit Floor (50%)
        // Sec 44ADA(4) triggers mandatory 44AB audit ONLY if total income > basic exemption limit!
        if (profile.hasPresumptiveProfession44ADA && profile.declaredProfitPercentage44ADA < 50.0) {
            if (profile.totalIncomeRupees > profile.basicExemptionLimit) {
                reasons.add("SEC_44ADA_PROFIT_BELOW_50_PERCENT_REQUIRES_AUDIT")
                return FormRoutingDecision(
                    selectedForm = ItrFormType.UNSUPPORTED_REQUIRES_CA_AUDIT_44AB,
                    routingReasonCodes = reasons,
                    requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome,
                    requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP
                )
            } else {
                reasons.add("SEC_44ADA_SUB_THRESHOLD_PROFIT_EXEMPT_FROM_AUDIT")
            }
        }

        // Gate 2B: Section 44AD Presumptive Business Audit Floor (6% digital / 8% cash)
        val min44AdDeemedRate = if (profile.cashReceiptsPercentage44AD <= 5.0) 6.0 else 8.0
        if (profile.hasPresumptiveBusiness44AD && profile.declaredProfitPercentage44AD < min44AdDeemedRate) {
            if (profile.totalIncomeRupees > profile.basicExemptionLimit) {
                reasons.add("SEC_44AD_PROFIT_BELOW_${min44AdDeemedRate.toInt()}_PERCENT_REQUIRES_AUDIT")
                return FormRoutingDecision(
                    selectedForm = ItrFormType.UNSUPPORTED_REQUIRES_CA_AUDIT_44AB,
                    routingReasonCodes = reasons,
                    requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome,
                    requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP
                )
            } else {
                reasons.add("SEC_44AD_SUB_THRESHOLD_PROFIT_EXEMPT_FROM_AUDIT")
            }
        }

        // Section 44AE Goods Carriage Vehicle Cap (10 Vehicles)
        if (profile.hasPresumptiveGoodsCarriage44AE && profile.goodsVehicleCount44AE > 10) {
            reasons.add("SEC_44AE_VEHICLES_EXCEED_10_REQUIRES_ITR3")
            return FormRoutingDecision(
                selectedForm = ItrFormType.ITR_3,
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

        val max44AdTurnover = if (profile.cashReceiptsPercentage44AD <= 5.0) {
            SEC_44AD_ENHANCED_DIGITAL_LIMIT
        } else {
            SEC_44AD_STANDARD_LIMIT
        }

        if (profile.hasRegularBusinessOrIntradaySpeculativeOrFnO ||
            (profile.hasPresumptiveProfession44ADA && profile.grossReceipts44ADARupees > max44AdaTurnover) ||
            (profile.hasPresumptiveBusiness44AD && profile.grossReceipts44ADRupees > max44AdTurnover)
        ) {
            reasons.add("BUSINESS_OR_PROFESSION_REQUIRES_ITR3")
            return FormRoutingDecision(
                selectedForm = ItrFormType.ITR_3,
                routingReasonCodes = reasons,
                requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome,
                requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP
            )
        }

        val hasAnyPresumptive = profile.hasPresumptiveProfession44ADA ||
                profile.hasPresumptiveBusiness44AD ||
                profile.hasPresumptiveGoodsCarriage44AE

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
        // Gate 12B: Current year capital loss disqualifies ITR-1 / ITR-4 to protect 8-year loss carry-forward
        if (profile.hasAnyCurrentYearCapitalLoss) {
            disqualifiedFromSimpleForms.add("CURRENT_YEAR_CAPITAL_LOSS_REQUIRES_SCHEDULE_CG")
        }
        if (profile.hasOtherCapitalGains ||
            (profile.hasCapitalGainsOrLosses && profile.equity112ALtcgRupees > LTCG_112A_EXEMPT_THRESHOLD)
        ) {
            disqualifiedFromSimpleForms.add("TAXABLE_CAPITAL_GAINS_REQUIRES_SCHEDULE_CG")
        }
        if (profile.hasVirtualDigitalAssets115BBH) {
            disqualifiedFromSimpleForms.add("VDA_CRYPTO_REQUIRES_SCHEDULE_VDA")
        }

        if (disqualifiedFromSimpleForms.isNotEmpty()) {
            val targetForm = if (hasAnyPresumptive) ItrFormType.ITR_3 else ItrFormType.ITR_2
            return FormRoutingDecision(
                selectedForm = targetForm,
                routingReasonCodes = disqualifiedFromSimpleForms,
                requiresScheduleFA = profile.holdsForeignAssetsOrForeignIncome &&
                        profile.residentialStatus == ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,
                requiresScheduleAL = profile.totalIncomeRupees > ITR_1_AND_4_INCOME_CAP,
                requiresForm10IeaBeforeDueDate = hasAnyPresumptive,
                isLockedIntoNewRegimePermanently = profile.hasPriorYearForm10IeaOptOut
            )
        }

        return if (hasAnyPresumptive) {
            val presumptiveReasons = mutableListOf<String>()
            if (profile.hasPresumptiveProfession44ADA) presumptiveReasons.add("PRESUMPTIVE_44ADA_WITHIN_ELIGIBLE_LIMITS")
            if (profile.hasPresumptiveBusiness44AD) presumptiveReasons.add("PRESUMPTIVE_44AD_WITHIN_ELIGIBLE_LIMITS")
            if (profile.hasPresumptiveGoodsCarriage44AE) presumptiveReasons.add("PRESUMPTIVE_44AE_WITHIN_ELIGIBLE_LIMITS")
            FormRoutingDecision(
                selectedForm = ItrFormType.ITR_4_SUGAM,
                routingReasonCodes = presumptiveReasons,
                requiresScheduleFA = false,
                requiresScheduleAL = false,
                requiresForm10IeaBeforeDueDate = true,
                isLockedIntoNewRegimePermanently = profile.hasPriorYearForm10IeaOptOut
            )
        } else {
            FormRoutingDecision(
                selectedForm = ItrFormType.ITR_1_SAHAJ,
                routingReasonCodes = listOf("SALARIED_AND_OTHER_SOURCES_WITHIN_SAHAJ_LIMITS"),
                requiresScheduleFA = false,
                requiresScheduleAL = false,
                requiresForm10IeaBeforeDueDate = false,
                isLockedIntoNewRegimePermanently = false
            )
        }
    }
}
