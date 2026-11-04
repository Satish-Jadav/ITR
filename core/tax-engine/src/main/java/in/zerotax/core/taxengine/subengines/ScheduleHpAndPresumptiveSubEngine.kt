package `in`.zerotax.core.taxengine.subengines

import `in`.zerotax.core.model.PropertyType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class HousePropertyLot(
    val propertyName: String,
    val propertyType: PropertyType = PropertyType.SELF_OCCUPIED,
    val annualRentReceived: Long = 0L,
    val municipalTaxesPaid: Long = 0L,
    val homeLoanInterestSec24b: Long = 0L,
    val preConstructionInterestTotal: Long = 0L,
    val coOwnerPercentage: Double = 100.0,
    val rentArrearsReceivedSec25A: Long = 0L,
    val tenantPan: String? = null
)

data class SinglePropertyHpResult(
    val propertyName: String,
    val propertyType: PropertyType,
    val grossAnnualValue: Long,
    val municipalTaxesDeducted: Long,
    val netAnnualValue: Long,
    val standardDeductionSec24a: Long,
    val interestDeductionSec24b: Long,
    val rentArrearsTaxableSec25A: Long,
    val netIncomeOrLossForProperty: Long
)

data class AggregateScheduleHpResult(
    val propertyBreakdowns: List<SinglePropertyHpResult>,
    val aggregateNetIncomeOrLoss: Long,
    val lossSetOffAllowedAgainstOtherHeadsSec71_3A: Long,
    val unabsorbedLossCarriedForwardSec71B: Long
)

data class Presumptive44AdaResult(
    val grossReceipts: Long,
    val deemedProfitRatePercent: Double,
    val taxableBusinessProfit: Long,
    val isAuditRequired: Boolean
)

data class Presumptive44AdResult(
    val digitalReceipts: Long,
    val cashReceipts: Long,
    val totalTurnover: Long,
    val taxableBusinessProfit: Long,
    val isAuditRequired: Boolean
)

data class Presumptive44AeResult(
    val vehicleCount: Int,
    val taxableDeemedProfit: Long,
    val isEligible: Boolean
)

object ScheduleHpAndPresumptiveSubEngine {
    private const val MAX_SELF_OCCUPIED_INTEREST_CAP = 2_00_000L
    private const val MAX_INTER_HEAD_LOSS_CAP_71_3A = 2_00_000L

    fun computeScheduleHpMultiProperty(properties: List<HousePropertyLot>): AggregateScheduleHpResult {
        val breakdowns = mutableListOf<SinglePropertyHpResult>()
        var selfOccupiedCount = 0
        var totalSelfOccupiedInterest = 0L
        var totalHpIncomeOrLoss = 0L

        for (prop in properties) {
            val coOwnerFactor = prop.coOwnerPercentage / 100.0
            val preEmiInstallment = prop.preConstructionInterestTotal / 5L
            val totalAnnualInterest = prop.homeLoanInterestSec24b + preEmiInstallment

            when (prop.propertyType) {
                PropertyType.SELF_OCCUPIED -> {
                    selfOccupiedCount++
                    // Section 23(2): Up to 2 self-occupied houses permitted at ₹0 annual value
                    if (selfOccupiedCount <= 2) {
                        val allowableInterest = min(totalAnnualInterest, MAX_SELF_OCCUPIED_INTEREST_CAP)
                        val coOwnerInterest = (allowableInterest * coOwnerFactor).roundToLong()
                        totalSelfOccupiedInterest += coOwnerInterest

                        val propertyLoss = -coOwnerInterest
                        totalHpIncomeOrLoss += propertyLoss

                        breakdowns.add(
                            SinglePropertyHpResult(
                                propertyName = prop.propertyName,
                                propertyType = prop.propertyType,
                                grossAnnualValue = 0L,
                                municipalTaxesDeducted = 0L,
                                netAnnualValue = 0L,
                                standardDeductionSec24a = 0L,
                                interestDeductionSec24b = coOwnerInterest,
                                rentArrearsTaxableSec25A = 0L,
                                netIncomeOrLossForProperty = propertyLoss
                            )
                        )
                    } else {
                        // Section 23(4): 3rd unsold/unrented house treated as DEEMED_LET_OUT
                        val gav = (prop.annualRentReceived * coOwnerFactor).roundToLong()
                        val munTaxes = (prop.municipalTaxesPaid * coOwnerFactor).roundToLong()
                        val nav = max(0L, gav - munTaxes)
                        val stdDed = (nav * 0.30).roundToLong()
                        val interest = (totalAnnualInterest * coOwnerFactor).roundToLong()
                        val net = nav - stdDed - interest
                        totalHpIncomeOrLoss += net

                        breakdowns.add(
                            SinglePropertyHpResult(
                                propertyName = prop.propertyName,
                                propertyType = PropertyType.DEEMED_LET_OUT,
                                grossAnnualValue = gav,
                                municipalTaxesDeducted = munTaxes,
                                netAnnualValue = nav,
                                standardDeductionSec24a = stdDed,
                                interestDeductionSec24b = interest,
                                rentArrearsTaxableSec25A = 0L,
                                netIncomeOrLossForProperty = net
                            )
                        )
                    }
                }
                PropertyType.LET_OUT, PropertyType.DEEMED_LET_OUT -> {
                    val gav = (prop.annualRentReceived * coOwnerFactor).roundToLong()
                    val munTaxes = (prop.municipalTaxesPaid * coOwnerFactor).roundToLong()
                    val nav = max(0L, gav - munTaxes)
                    val stdDed = (nav * 0.30).roundToLong()
                    // No statutory ceiling on interest deduction for let-out property
                    val interest = (totalAnnualInterest * coOwnerFactor).roundToLong()
                    // Section 25A: 30% statutory deduction on rent arrears
                    val rentArrearsTaxable = (prop.rentArrearsReceivedSec25A * 0.70 * coOwnerFactor).roundToLong()
                    val net = (nav - stdDed - interest) + rentArrearsTaxable
                    totalHpIncomeOrLoss += net

                    breakdowns.add(
                        SinglePropertyHpResult(
                            propertyName = prop.propertyName,
                            propertyType = prop.propertyType,
                            grossAnnualValue = gav,
                            municipalTaxesDeducted = munTaxes,
                            netAnnualValue = nav,
                            standardDeductionSec24a = stdDed,
                            interestDeductionSec24b = interest,
                            rentArrearsTaxableSec25A = rentArrearsTaxable,
                            netIncomeOrLossForProperty = net
                        )
                    )
                }
            }
        }

        // Section 71(3A): Inter-head set-off of house property loss capped strictly at ₹2,00,000
        val (setOffAllowed, carriedForward) = if (totalHpIncomeOrLoss < 0) {
            val totalLoss = -totalHpIncomeOrLoss
            val allowed = min(totalLoss, MAX_INTER_HEAD_LOSS_CAP_71_3A)
            val excess = max(0L, totalLoss - allowed)
            allowed to excess
        } else {
            0L to 0L
        }

        return AggregateScheduleHpResult(
            propertyBreakdowns = breakdowns,
            aggregateNetIncomeOrLoss = totalHpIncomeOrLoss,
            lossSetOffAllowedAgainstOtherHeadsSec71_3A = setOffAllowed,
            unabsorbedLossCarriedForwardSec71B = carriedForward
        )
    }

    fun computePresumptive44Ada(
        grossReceipts: Long,
        declaredProfitPercentage: Double = 50.0,
        cashReceiptsPercentage: Double = 0.0
    ): Presumptive44AdaResult {
        val maxTurnover = if (cashReceiptsPercentage <= 5.0) 75_00_000L else 50_00_000L
        val isAuditRequired = declaredProfitPercentage < 50.0 || grossReceipts > maxTurnover
        val deemedRate = max(50.0, declaredProfitPercentage)
        val taxableProfit = (grossReceipts * (deemedRate / 100.0)).roundToLong()

        return Presumptive44AdaResult(
            grossReceipts = grossReceipts,
            deemedProfitRatePercent = deemedRate,
            taxableBusinessProfit = taxableProfit,
            isAuditRequired = isAuditRequired
        )
    }

    fun computePresumptive44Ad(
        digitalReceipts: Long,
        cashReceipts: Long,
        declaredDigitalRate: Double = 0.06,
        declaredCashRate: Double = 0.08
    ): Presumptive44AdResult {
        val totalTurnover = digitalReceipts + cashReceipts
        val cashPct = if (totalTurnover > 0) (cashReceipts.toDouble() / totalTurnover) * 100.0 else 0.0
        val maxTurnover = if (cashPct <= 5.0) 3_00_00_000L else 2_00_00_000L

        val isAuditRequired = declaredDigitalRate < 0.06 || declaredCashRate < 0.08 || totalTurnover > maxTurnover

        val profitDigital = (digitalReceipts * max(0.06, declaredDigitalRate)).roundToLong()
        val profitCash = (cashReceipts * max(0.08, declaredCashRate)).roundToLong()
        val totalProfit = profitDigital + profitCash

        return Presumptive44AdResult(
            digitalReceipts = digitalReceipts,
            cashReceipts = cashReceipts,
            totalTurnover = totalTurnover,
            taxableBusinessProfit = totalProfit,
            isAuditRequired = isAuditRequired
        )
    }

    fun computePresumptive44Ae(
        heavyVehiclesWeightInTons: List<Double>,
        otherVehiclesCount: Int,
        monthsOperated: Int = 12
    ): Presumptive44AeResult {
        val totalVehicles = heavyVehiclesWeightInTons.size + otherVehiclesCount
        val isEligible = totalVehicles <= 10

        // Heavy vehicle (> 12MT): ₹1,000 per ton per month
        var heavyProfit = 0L
        for (weight in heavyVehiclesWeightInTons) {
            heavyProfit += (weight * 1000.0 * monthsOperated).roundToLong()
        }

        // Other vehicle (<= 12MT): ₹7,500 per month
        val otherProfit = otherVehiclesCount * 7500L * monthsOperated

        return Presumptive44AeResult(
            vehicleCount = totalVehicles,
            taxableDeemedProfit = heavyProfit + otherProfit,
            isEligible = isEligible
        )
    }
}

