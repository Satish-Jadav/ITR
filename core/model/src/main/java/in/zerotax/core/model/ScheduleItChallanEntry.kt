package in.zerotax.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleItChallanEntry(
    val bsrCode7Digits: String,
    val dateOfDepositIso: String, // YYYY-MM-DD
    val challanSerialNumber5Digits: String,
    val taxAmountPaidRupees: Long
)

@Serializable
data class TaxpayerRoutingProfile(
    val residentialStatus: ResidentialStatus = ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,
    val totalIncomeRupees: Long = 0L,
    val housePropertyCount: Int = 1,
    val hasBroughtForwardOrCarriedForwardLosses: Boolean = false,
    val hasCapitalGainsOrLosses: Boolean = false,
    val equity112ALtcgRupees: Long = 0L,
    val hasOtherCapitalGains: Boolean = false,
    val agriculturalIncomeRupees: Long = 0L,
    val isCompanyDirector: Boolean = false,
    val holdsUnlistedEquityShares: Boolean = false,
    val holdsForeignAssetsOrForeignIncome: Boolean = false,
    val hasPresumptiveProfession44ADA: Boolean = false,
    val grossReceipts44ADARupees: Long = 0L,
    val cashReceiptsPercentage44ADA: Double = 0.0,
    val declaredProfitPercentage44ADA: Double = 50.0,
    val hasRegularBusinessOrIntradaySpeculativeOrFnO: Boolean = false,
    val requiresStatutoryAudit44AB: Boolean = false
)

@Serializable
data class FormRoutingDecision(
    val selectedForm: ItrFormType,
    val routingReasonCodes: List<String>,
    val requiresScheduleFA: Boolean,
    val requiresScheduleAL: Boolean // Asset & Liability schedule if Total Income > ₹50,00,000
)
