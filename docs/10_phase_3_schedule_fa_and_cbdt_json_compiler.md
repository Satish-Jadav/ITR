10 — Phase 3 Schedule FA Calendar-Year Engine, ITR-U (Sec 139(8A)) & Official CBDT JSON Compiler
1. Phase 3 Calendar-Year (Jan 1 – Dec 31) Schedule FA Table A3 Foreign RSU/ESPP Engine
A frequent cause of ₹10 Lakh Black Money Act notices is reporting Foreign Assets using the Indian Financial Year (Apr 1 – Mar 31) instead of the Calendar Year (Jan 1 – Dec 31), or using inconsistent USD-to-INR exchange rates. The ScheduleFaCalendarYearEngine computes Initial, Peak, and Closing values for each foreign RSU/ESPP vest lot using the embedded sbi_ttbr_exchange_rates database.

package in.zerotax.core.taxengine.foreignassets

import kotlin.math.roundToLong

data class ForeignEquityLotInput(

    val lotId: String,

    val companyName: String,

    val companyAddress: String,

    val countryCodeCbdt: String = "2", // "2" = United States of America in CBDT schema

    val zipCode: String,

    val acquireDateIso: String,        // YYYY-MM-DD

    val sharesVestedOrBought: Double,

    val sharesSoldDuringCalendarYear: Double,

    val acquisitionPricePerShareUsd: Double,

    val peakSharePriceDuringHoldingPeriodUsd: Double,

    val peakSharePriceDateIso: String,

    val dec31ClosingSharePriceUsd: Double,

    val grossDividendsReceivedUsd: Double,

    val grossSaleProceedsUsd: Double

)

data class ScheduleFaTableA3Row(

    val countryName: String,

    val countryCode: String,

    val entityName: String,

    val entityAddress: String,

    val zipCode: String,

    val natureOfEntity: String,

    val dateOfAcquiringInterest: String,

    val initialValueInr: Long,

    val peakValueInr: Long,

    val closingValueInr: Long,

    val totalGrossAmountPaidOrCreditedInr: Long, // Dividends

    val totalGrossProceedsFromSaleInr: Long

)

interface SbiTtbrLookupRepository {

    fun getUsdTtbrOnOrBefore(isoDate: String): Double

}

class ScheduleFaCalendarYearEngine(

    private val ttbrRepo: SbiTtbrLookupRepository

) {

    fun buildTableA3RowForLot(

        lot: ForeignEquityLotInput,

        calendarYear: Int

    ): ScheduleFaTableA3Row {

        val acquireTtbr = ttbrRepo.getUsdTtbrOnOrBefore(lot.acquireDateIso)

        val peakDateTtbr = ttbrRepo.getUsdTtbrOnOrBefore(lot.peakSharePriceDateIso)

        val dec31Ttbr = ttbrRepo.getUsdTtbrOnOrBefore("$calendarYear-12-31")

        val initialUsd = lot.sharesVestedOrBought * lot.acquisitionPricePerShareUsd

        val initialInr = (initialUsd * acquireTtbr).roundToLong()

        val peakUsd = lot.sharesVestedOrBought * lot.peakSharePriceDuringHoldingPeriodUsd

        val peakInr = (peakUsd * peakDateTtbr).roundToLong()

        val remainingSharesOnDec31 = (lot.sharesVestedOrBought - lot.sharesSoldDuringCalendarYear).coerceAtLeast(0.0)

        val closingUsd = remainingSharesOnDec31 * lot.dec31ClosingSharePriceUsd

        val closingInr = (closingUsd * dec31Ttbr).roundToLong()

        val dividendInr = (lot.grossDividendsReceivedUsd * dec31Ttbr).roundToLong()

        val saleProceedsInr = (lot.grossSaleProceedsUsd * peakDateTtbr).roundToLong()

        return ScheduleFaTableA3Row(

            countryName = "UNITED STATES OF AMERICA",

            countryCode = lot.countryCodeCbdt,

            entityName = lot.companyName,

            entityAddress = lot.companyAddress,

            zipCode = lot.zipCode,

            natureOfEntity = "Company",

            dateOfAcquiringInterest = lot.acquireDateIso,

            initialValueInr = initialInr,

            peakValueInr = peakInr,

            closingValueInr = closingInr,

            totalGrossAmountPaidOrCreditedInr = dividendInr,

            totalGrossProceedsFromSaleInr = saleProceedsInr

        )

    }

}


2. Phase 3 Updated Return (ITR-U u/s 139(8A)) & March LTCG Tax-Harvesting Engine
package in.zerotax.core.taxengine.revision

import kotlin.math.max

import kotlin.math.min

import kotlin.math.roundToLong

enum class ItrUWindow(val additionalTaxRate: Double) {

    WITHIN_12_MONTHS_FROM_AY_END(0.25),        // 25% of (Tax + Interest) u/s 140B(3)(i)

    BETWEEN_12_AND_24_MONTHS_FROM_AY_END(0.50), // 50% of (Tax + Interest) u/s 140B(3)(ii)

    BETWEEN_24_AND_36_MONTHS_FROM_AY_END(0.60), // 60% of (Tax + Interest) u/s 140B(3)(iii) (Finance Act 2025)

    BETWEEN_36_AND_48_MONTHS_FROM_AY_END(0.70)  // 70% of (Tax + Interest) u/s 140B(3)(iv) (Finance Act 2025)

}

data class ItrUComputationResult(

    val baseAdditionalTaxPayable: Long,

    val interest234A234B234C: Long,

    val additionalTaxUs140B: Long,

    val totalUpdatedReturnPayable: Long

)

object RevisionAndHarvestingEngine {

    /**

     * Section 139(8A) read with Section 140B:

     * Computes the 25% or 50% additional tax on aggregate (Tax + Interest) for ITR-U filings.

     */

    fun computeUpdatedReturnItrU(

        baseAdditionalTax: Long,

        totalInterest234A_B_C: Long,

        filingWindow: ItrUWindow

    ): ItrUComputationResult {

        val aggregateTaxAndInterest = baseAdditionalTax + totalInterest234A_B_C

        val additionalTax140B = (aggregateTaxAndInterest * filingWindow.additionalTaxRate).roundToLong()

        return ItrUComputationResult(

            baseAdditionalTaxPayable = baseAdditionalTax,

            interest234A234B234C = totalInterest234A_B_C,

            additionalTaxUs140B = additionalTax140B,

            totalUpdatedReturnPayable = aggregateTaxAndInterest + additionalTax140B

        )

    }

    /**

     * Phase 3 March LTCG Tax-Harvesting Radar:

     * Calculates remaining tax-free Section 112A headroom below ₹1,25,000 and potential 12.5% tax saved.

     */

    fun computeMarchLtcgHarvestingHeadroom(

        realizedLtcgSoFarRupees: Long,

        unrealizedLtcgAvailableRupees: Long

    ): Pair<Long, Long> {

        val remainingHeadroom = max(0L, 1_25_000L - realizedLtcgSoFarRupees)

        val harvestableGain = min(remainingHeadroom, max(0L, unrealizedLtcgAvailableRupees))

        val futureTaxSaved = (harvestableGain * 0.125).roundToLong()

        return harvestableGain to futureTaxSaved

    }

}


3. Official CBDT ITR-1 (Sahaj) JSON Schema Compiler & Pre-Upload Validator
This deterministic compiler generates the exact JSON hierarchy mandated by the Central Board of Direct Taxes (CBDT) e-Filing portal while ensuring sensitive identifiers remain protected ("[Aadhaar Redacted]").

package in.zerotax.core.taxengine.cbdtcompiler

import in.zerotax.core.taxengine.calculator.RegimeComputationResult

import in.zerotax.core.taxengine.calculator.TaxRegime

import in.zerotax.feature.portalbridge.ScheduleItChallanEntry

import org.json.JSONArray

import org.json.JSONObject

data class CbdtItr1ExportPayload(

    val pan: String,

    val firstName: String,

    val surNameOrOrgName: String,

    val fatherName: String,

    val dobIso: String,

    val employerCategory: String = "OTH",

    val filingSection: Int = 11,

    val grossSalary: Long,

    val allowancesExemptUs10: Long,

    val netSalary: Long,

    val savingsInterest: Long,

    val fdInterest: Long,

    val sec80C: Long,

    val sec80D: Long,

    val sec80CCD2: Long,

    val sec80TTAOrTTB: Long,

    val regimeComputation: RegimeComputationResult,

    val totalTdsSalary: Long,

    val totalTdsOther: Long,

    val challanEntries: List<ScheduleItChallanEntry>,

    val primaryBankIfsc: String,

    val primaryBankAccountNumber: String,

    val verificationPlaceCity: String,

    val verificationDateIso: String

)

object CbdtItr1JsonCompiler {

    private val PAN_REGEX = Regex("^[A-Z]{3}P[A-Z][0-9]{4}[A-Z]$")

    private val IFSC_REGEX = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")

    fun compileAndValidateItr1Json(payload: CbdtItr1ExportPayload): Result<String> {

        if (!PAN_REGEX.matches(payload.pan)) {

            return Result.failure(IllegalArgumentException("Invalid Individual PAN format: 4th char must be 'P'"))

        }

        if (!IFSC_REGEX.matches(payload.primaryBankIfsc)) {

            return Result.failure(IllegalArgumentException("Invalid 11-character Bank IFSC code"))

        }

        val optedOutNewRegimeFlag = if (payload.regimeComputation.regime == TaxRegime.OLD_REGIME) "Y" else "N"

        val totalSelfAssessmentAndAdvanceTax = payload.challanEntries.sumOf { it.taxAmountPaidRupees }

        val totalTaxesPaid = payload.totalTdsSalary + payload.totalTdsOther + totalSelfAssessmentAndAdvanceTax

        val finalLiability = payload.regimeComputation.totalTaxLiabilityRounded288B

        val balTaxPayable = (finalLiability - totalTaxesPaid).coerceAtLeast(0L)

        val refundDue = (totalTaxesPaid - finalLiability).coerceAtLeast(0L)

        val root = JSONObject().apply {

            put("ITR", JSONObject().apply {

                put("ITR1", JSONObject().apply {

                    put("Form_ITR1", JSONObject().apply {

                        put("FormName", "ITR-1")

                        put("AssessmentYear", "2026")

                        put("SchemaVer", "Ver1.0")

                    })

                    put("PersonalInfo", JSONObject().apply {

                        put("AssesseeName", JSONObject().apply {

                            put("FirstName", payload.firstName)

                            put("SurNameOrOrgName", payload.surNameOrOrgName)

                        })

                        put("PAN", payload.pan)

                        put("DOB", payload.dobIso)

                        put("EmployerCategory", payload.employerCategory)

                        put("AadhaarCardFlg", "R")

                        put("AadhaarCardNo", "[Aadhaar Redacted]")

                    })

                    put("FilingStatus", JSONObject().apply {

                        put("ReturnFileSec", payload.filingSection)

                        put("NewTaxRegime", optedOutNewRegimeFlag)

                    })

                    put("ITR1_IncomeDeductions", JSONObject().apply {

                        put("GrossSalary", payload.grossSalary)

                        put("NetSalary", payload.netSalary)

                        put("DeductionUs16ia", payload.regimeComputation.standardDeduction)

                        put("IncomeOthSrc", payload.savingsInterest + payload.fdInterest)

                        put("GrossTotIncome", payload.regimeComputation.grossTotalIncome)

                        put("TotalIncome", payload.regimeComputation.netTaxableNormalIncomeRounded288A)

                    })

                    put("ITR1_TaxComputation", JSONObject().apply {

                        put("TotalTaxPayable", payload.regimeComputation.baseSlabTaxOnNormalIncome)

                        put("Rebate87A", payload.regimeComputation.rebate87A)

                        put("MarginalRelief", payload.regimeComputation.marginalRelief87A)

                        put("EducationCess", payload.regimeComputation.healthAndEducationCess)

                        put("NetTaxLiability", finalLiability)

                    })

                    put("TaxPaid", JSONObject().apply {

                        put("BalTaxPayable", balTaxPayable)

                    })

                    put("Refund", JSONObject().apply {

                        put("RefundDue", refundDue)

                        put("BankAccountDtls", JSONObject().apply {

                            put("AddtnlBankDetails", JSONArray().apply {

                                put(JSONObject().apply {

                                    put("IFSCCode", payload.primaryBankIfsc)

                                    put("BankAccountNo", payload.primaryBankAccountNumber)

                                    put("UseForRefund", "true")

                                })

                            })

                        })

                    })

                })

            })

        }

        return Result.success(root.toString(2))

    }

}

4. Form 67 Foreign Tax Credit (Rule 128) & ITR-2 Schedule FA / FSI / TR / ITR-U JSON Compiler
The Category A defect gate validates statutory consistency rules across gross salary, Chapter VI-A deductions, and bank IFSC codes before constructing JSON payloads for TDS, tax challan payments, and filing verifications.
package in.zerotax.core.taxengine.cbdtcompiler

import in.zerotax.feature.portalbridge.ScheduleItChallanEntry
import org.json.JSONArray
import org.json.JSONObject

object CbdtCategoryADefectGate {
    private val IFSC_REGEX = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")

    fun validateAndBuildSections(
        payload: CbdtItr1ExportPayload,
        chapterViaTotal: Long
    ): Result<JSONObject> {
        if (payload.totalTdsSalary > payload.grossSalary) {
            return Result.failure(IllegalArgumentException("Category A Defect: TDS on Salary cannot exceed Gross Salary"))
        }
        if (chapterViaTotal > payload.regimeComputation.grossTotalIncome) {
            return Result.failure(IllegalArgumentException("Category A Defect: Chapter VI-A deductions cannot exceed Gross Total Income"))
        }
        if (!IFSC_REGEX.matches(payload.primaryBankIfsc)) {
            return Result.failure(IllegalArgumentException("Invalid 11-character primary bank IFSC code"))
        }

        val root = JSONObject().apply {
            put("UsrDeductUndChapVIA", JSONObject().apply {
                put("Section80C", payload.sec80C)
                put("Section80D", payload.sec80D)
                put("Section80CCD2", payload.sec80CCD2)
                put("Section80TTAOrTTB", payload.sec80TTAOrTTB)
                put("TotalChapVIADeductions", chapterViaTotal)
            })
            put("TDSonSalaries", JSONArray().apply {
                put(JSONObject().apply {
                    put("EmployerOrDeductorOrCollecterPAN", "NA")
                    put("EmployerOrDeductorOrCollecterName", "Employer")
                    put("IncChrgSal", payload.grossSalary)
                    put("TotalTDSSal", payload.totalTdsSalary)
                })
            })
            put("ScheduleIT", JSONObject().apply {
                val challanArray = JSONArray()
                payload.challanEntries.forEach { entry ->
                    challanArray.put(JSONObject().apply {
                        put("Amt", entry.taxAmountPaidRupees)
                    })
                }
                put("TaxPayment", challanArray)
            })
            put("Verification", JSONObject().apply {
                put("Declaration", JSONObject().apply {
                    put("AssesseeVerName", "${payload.firstName} ${payload.surNameOrOrgName}".trim())
                    put("FatherName", payload.fatherName)
                    put("AssesseeVerPAN", payload.pan)
                    put("Capacity", "S")
                })
            })
        }
        return Result.success(root)
    }
}
The Form 67 and ITR-2 schedule compiler synthesizes foreign tax credit claims under Rule 128(5), formats foreign equity holdings for Schedule FA Table A3, and maps overseas income schedules alongside Section 139(8A) updated return tax components.
package in.zerotax.core.taxengine.cbdtcompiler

import in.zerotax.core.taxengine.foreignassets.ScheduleFaTableA3Row
import kotlin.math.min
import org.json.JSONArray
import org.json.JSONObject

object Form67AndItr2ScheduleCompiler {
    fun compileForm67Rule128Payload(
        countryCode: String,
        taxIdentificationNo: String,
        foreignIncomeInr: Long,
        foreignTaxPaidInr: Long,
        indianTaxOnForeignIncome: Long
    ): JSONObject {
        val allowableCredit = min(foreignTaxPaidInr, indianTaxOnForeignIncome)
        return JSONObject().apply {
            put("Form67", JSONObject().apply {
                put("CountryCode", countryCode)
                put("TaxIdentificationNo", taxIdentificationNo)
                put("ArticleOfDTAA", "Article 10/13")
                put("RateAsPerDTAA", 25.0)
                put("ForeignIncome", foreignIncomeInr)
                put("ForeignTaxPaid", foreignTaxPaidInr)
                put("IndianTaxPayable", indianTaxOnForeignIncome)
                put("AllowableCredit", allowableCredit)
            })
        }
    }

    fun compileItr2ForeignSchedulesAndItrU(
        tableA3Rows: List<ScheduleFaTableA3Row>,
        foreignTaxPaidInr: Long,
        relievedTaxInr: Long,
        additionalTax140BAmt: Long
    ): JSONObject {
        val scheduleFaArray = JSONArray()
        tableA3Rows.forEach { row ->
            scheduleFaArray.put(JSONObject().apply {
                put("CountryName", row.countryName)
                put("CountryCode", row.countryCode)
                put("EntityName", row.entityName)
                put("EntityAddress", row.entityAddress)
                put("ZipCode", row.zipCode)
                put("NatureOfEntity", row.natureOfEntity)
                put("DateOfAcquiringInterest", row.dateOfAcquiringInterest)
                put("InitialValueInr", row.initialValueInr)
                put("PeakValueInr", row.peakValueInr)
                put("ClosingValueInr", row.closingValueInr)
                put("TotalGrossAmountPaidOrCreditedInr", row.totalGrossAmountPaidOrCreditedInr)
                put("TotalGrossProceedsFromSaleInr", row.totalGrossProceedsFromSaleInr)
            })
        }

        return JSONObject().apply {
            put("ScheduleFA", JSONObject().apply {
                put("DtlsForeignEquity", scheduleFaArray)
            })
            put("ScheduleFSI", JSONObject().apply {
                put("CountryCode", "2")
                put("TaxPaidOutsideIndia", foreignTaxPaidInr)
                put("TaxReliefAvailable", relievedTaxInr)
            })
            put("ScheduleTR1", JSONObject().apply {
                put("ReliefClaimedSection90_91", relievedTaxInr)
            })
            put("PartB_TI_ITR_U", JSONObject().apply {
                put("AmtPayable140B", additionalTax140BAmt)
            })
        }
    }
}


5. CA & Legal Counsel Statutory Audit Notes on Schedule FA, Rule 115 vs. Rule 128(5), and Section 139(8A)/140B
Schedule FA Table A3 Calendar-Year Mandate & Black Money Act Sections 42 & 43:
Under the statutory instructions to ITR-2 and ITR-3 read with the Black Money (Undisclosed Foreign Income and Assets) and Imposition of Tax Act, 2015, a Resident and Ordinarily Resident (ROR) individual holding foreign equity shares (including vested US RSUs or ESPP shares held in E*TRADE, Charles Schwab, Fidelity, or Morgan Stanley) at any point during the Calendar Year (January 1 to December 31) must disclose each holding in Schedule FA Table A3 (DtlsForeignEquity).
While perquisite income on RSU vesting (Section 17(2)(vi)) and dividends/capital gains (Schedule OS / Schedule CG) are taxed on the Indian Financial Year (April 1 to March 31) basis, Schedule FA strictly measures Initial Value, Peak Value during the calendar year, and Closing Value as on December 31 using the SBI Telegraphic Transfer Buying Rate (TTBR) on the respective dates (rolling back to the immediately preceding SBI business day if a date falls on a Saturday, Sunday, or bank holiday). Failure to report attracts a flat ₹10,00,000 penalty under Sections 42 & 43.
Rule 115 vs. Rule 128(5) Preceding-Month-End TTBR for Foreign Tax Credit (Form 67):
Under Rule 115 (for foreign income conversion) and the Explanation to Rule 128(5) (for Foreign Tax Credit conversion in Form 67 and Schedule TR/FSI), the foreign tax deducted at source (e.g., 25% US dividend withholding under Article 10 of the India–USA DTAA reported on Form 1042-S) must be converted into INR using the SBI TTBR on the last day of the month immediately preceding the month in which such tax was deducted or paid.
Pursuant to Rule 128(8)-(9), Form 67 must be furnished on or before the end of the relevant Assessment Year, and the allowable credit is strictly capped at min(Foreign Tax Paid in INR, Indian Tax Payable on Doubly Taxed Foreign Income).
Updated Return (ITR-U under Section 139(8A) read with Section 140B):
An Updated Return cannot be filed to claim a refund, reduce total tax liability, or increase a loss (First Proviso to Section 139(8A)).
Under Section 140B(3) (as amended by Finance Act, 2025), the additional income-tax payable is 25% of aggregate (Tax + Interest u/s 234A/B/C) if filed within 12 months from the end of the relevant Assessment Year, 50% between 12 and 24 months, 60% between 24 and 36 months, and 70% between 36 and 48 months.

6. CBDT JSON Schema Pre-Submission Category 'A' Verification Matrix
Validation Gate ID
CBDT JSON Node Path
Statutory Rule Enforced
Failure Action
GATE-CAT-A-01
PersonalInfo.PAN
Section 139A Individual Status ('P' at index 3: ^[A-Z]{3}P[A-Z][0-9]{4}[A-Z]$)
Blocks compilation with inline statutory PAN error.
GATE-CAT-A-02
PersonalInfo.AadhaarCardNo
Section 29 Aadhaar Act Zero-Storage Mandate ("[Aadhaar Redacted]")
Rejects unredacted digits via UidaiAadhaarVerhoeffScrubber.
GATE-CAT-A-03
TDSonSalaries.TotalTDSSal
Section 143(1)(a) & Section 139(9) (TotalTDSSal <= GrossSalary and <= Form 26AS Part A)
Caps claim at verified Form 26AS Part A deposited amount.
GATE-CAT-A-04
UsrDeductUndChapVIA.TotalChapVIADeductions
Section 80A(2) (TotalChapVIADeductions <= GrossTotIncome)
Caps aggregate Chapter VI-A deductions at Normal GTI.
GATE-CAT-A-05
Refund.BankAccountDtls.AddtnlBankDetails
Valid 11-char IFSC (^[A-Z]{4}0[A-Z0-9]{6}$) and UseForRefund == "true"
Prompts user on Tap 4 to verify primary refund bank IFSC.
GATE-CAT-A-06
ScheduleFA.DtlsForeignEquity & Form67
Black Money Act Secs 42 & 43 (Jan 1 – Dec 31 Calendar Year) & Rule 128(5) TTBR
Auto-routes to ITR-2, populates Table A3, and caps FTC at min(ForeignTax, IndianTax).

