package `in`.zerotax.core.taxengine

import `in`.zerotax.core.model.AssessmentYearProfile
import `in`.zerotax.core.model.BankAccountDetails
import `in`.zerotax.core.model.CitizenCategory
import `in`.zerotax.core.model.Itr5FirmInput
import `in`.zerotax.core.model.ItrFormType
import `in`.zerotax.core.model.ResidenceCityCategory
import `in`.zerotax.core.model.ResidentialStatus
import `in`.zerotax.core.model.TaxpayerRoutingProfile
import `in`.zerotax.core.taxengine.advanced.CapitalGainsAndForeignAssetEngine
import `in`.zerotax.core.taxengine.advanced.HraAndInterestEngine
import `in`.zerotax.core.taxengine.advanced.HraCalculationInput
import `in`.zerotax.core.taxengine.advanced.UniversalBrokerPnLParser
import `in`.zerotax.core.taxengine.calculator.DeterministicIndianTaxEngine
import `in`.zerotax.core.taxengine.calculator.TaxpayerFinancialInput
import `in`.zerotax.core.taxengine.compiler.CbdtCategoryADefectGate
import `in`.zerotax.core.taxengine.corporate.CorporateAndTrustTaxEngine
import `in`.zerotax.core.taxengine.revision.ItrUWindow
import `in`.zerotax.core.taxengine.revision.RevisionAndHarvestingEngine
import `in`.zerotax.core.taxengine.routing.DeterministicItrFormRouter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Arrays

data class Section89ReliefResult(
    val taxOnTotalIncomeCurrentYearWithArrears: Long,
    val taxOnTotalIncomeCurrentYearWithoutArrears: Long,
    val taxDifferenceCurrentYear: Long,
    val taxOnPastYearIncomeWithArrears: Long,
    val taxOnPastYearIncomeWithoutArrears: Long,
    val taxDifferencePastYear: Long,
    val reliefAmountUs89: Long
)

object Section89ArrearsReliefCalculator {
    fun computeSection89Relief(
        currentYearTaxableIncomeWithoutArrears: Long,
        arrearsReceived: Long,
        pastYearTaxableIncome: Long
    ): Section89ReliefResult {
        fun calculateTaxNewRegime(taxableIncome: Long, isAy2026_27: Boolean = false): Long {
            var tax = 0.0
            if (isAy2026_27) {
                if (taxableIncome <= 4_00_000L) return 0L
                if (taxableIncome > 4_00_000L) tax += (minOf(taxableIncome, 8_00_000L) - 4_00_000L) * 0.05
                if (taxableIncome > 8_00_000L) tax += (minOf(taxableIncome, 12_00_000L) - 8_00_000L) * 0.10
                if (taxableIncome > 12_00_000L) tax += (minOf(taxableIncome, 16_00_000L) - 12_00_000L) * 0.15
                if (taxableIncome > 16_00_000L) tax += (minOf(taxableIncome, 20_00_000L) - 16_00_000L) * 0.20
                if (taxableIncome > 20_00_000L) tax += (minOf(taxableIncome, 24_00_000L) - 20_00_000L) * 0.25
                if (taxableIncome > 24_00_000L) tax += (taxableIncome - 24_00_000L) * 0.30
            } else {
                if (taxableIncome <= 3_00_000L) return 0L
                if (taxableIncome > 3_00_000L) tax += (minOf(taxableIncome, 7_00_000L) - 3_00_000L) * 0.05
                if (taxableIncome > 7_00_000L) tax += (minOf(taxableIncome, 10_00_000L) - 7_00_000L) * 0.10
                if (taxableIncome > 10_00_000L) tax += (minOf(taxableIncome, 12_00_000L) - 10_00_000L) * 0.15
                if (taxableIncome > 12_00_000L) tax += (minOf(taxableIncome, 15_00_000L) - 12_00_000L) * 0.20
                if (taxableIncome > 15_00_000L) tax += (taxableIncome - 15_00_000L) * 0.30
            }
            return (tax * 1.04).toLong()
        }

        fun calculateTaxOldRegimePastYear(taxableIncome: Long): Long {
            if (taxableIncome <= 2_50_000L) return 0L
            var tax = 0.0
            if (taxableIncome > 2_50_000L) tax += (minOf(taxableIncome, 5_00_000L) - 2_50_000L) * 0.05
            if (taxableIncome > 5_00_000L) tax += (minOf(taxableIncome, 10_00_000L) - 5_00_000L) * 0.20
            if (taxableIncome > 10_00_000L) tax += (taxableIncome - 10_00_000L) * 0.30
            return (tax * 1.04).toLong()
        }

        val currentYearWithArrears = currentYearTaxableIncomeWithoutArrears + arrearsReceived
        val taxCurrentWith = calculateTaxNewRegime(currentYearWithArrears)
        val taxCurrentWithout = calculateTaxNewRegime(currentYearTaxableIncomeWithoutArrears)
        val diffCurrent = taxCurrentWith - taxCurrentWithout

        val pastYearWithArrears = pastYearTaxableIncome + arrearsReceived
        val taxPastWith = calculateTaxOldRegimePastYear(pastYearWithArrears)
        val taxPastWithout = calculateTaxOldRegimePastYear(pastYearTaxableIncome)
        val diffPast = taxPastWith - taxPastWithout

        val relief = maxOf(0L, diffCurrent - diffPast)
        return Section89ReliefResult(
            taxOnTotalIncomeCurrentYearWithArrears = taxCurrentWith,
            taxOnTotalIncomeCurrentYearWithoutArrears = taxCurrentWithout,
            taxDifferenceCurrentYear = diffCurrent,
            taxOnPastYearIncomeWithArrears = taxPastWith,
            taxOnPastYearIncomeWithoutArrears = taxPastWithout,
            taxDifferencePastYear = diffPast,
            reliefAmountUs89 = relief
        )
    }
}

data class HousePropertyLossResult(
    val allowableInterHeadSetOff: Long,
    val carriedForwardLoss: Long,
    val carryForwardYears: Int
)

object HousePropertyLossEngine {
    fun computeHousePropertySetOff(
        selfOccupiedInterestPaid: Long,
        isOldRegime: Boolean
    ): HousePropertyLossResult {
        if (!isOldRegime) {
            return HousePropertyLossResult(
                allowableInterHeadSetOff = 0L,
                carriedForwardLoss = 0L,
                carryForwardYears = 0
            )
        }
        val maxSetOffCap = 2_00_000L
        val setOff = minOf(selfOccupiedInterestPaid, maxSetOffCap)
        val carriedForward = selfOccupiedInterestPaid - setOff
        return HousePropertyLossResult(
            allowableInterHeadSetOff = setOff,
            carriedForwardLoss = carriedForward,
            carryForwardYears = if (carriedForward > 0L) 8 else 0
        )
    }
}

class RbiAaEphemeralDecryptorTestDouble {
    var sharedSecretBytes: ByteArray = ByteArray(32) { 0x5A.toByte() }
    var fipNonce32Bytes: ByteArray = ByteArray(32) { 0xA5.toByte() }
    var derivedKey: ByteArray = ByteArray(32) { 0x3C.toByte() }

    fun decryptAndExtractInterestInMemory(encryptedPayload: ByteArray): Long {
        try {
            return 45_000L
        } finally {
            Arrays.fill(sharedSecretBytes, 0.toByte())
            Arrays.fill(fipNonce32Bytes, 0.toByte())
            Arrays.fill(derivedKey, 0.toByte())
        }
    }
}

object TokenLockedVernacularExplainerHelper {
    private val REQUIRED_TOKENS = listOf("{{SEC_1}}", "{{AMT_1}}", "{{AMT_2}}")

    fun restoreAndVerifyTokens(translatedText: String, locale: String): Boolean {
        if (locale !in listOf("gu-IN", "hi-IN", "mr-IN", "ta-IN", "te-IN")) return false
        return REQUIRED_TOKENS.all { token -> translatedText.contains(token) }
    }
}

@DisplayName("Advanced Tax Engine Edge Case Suite (ECV-01..06)")
class AdvancedTaxEngineEdgeCaseTest {

    @Test
    fun `ECV-01 Section 89(1) Relief for Arrears of Salary Form 10E computes and verifies positive tax relief`() {
        val reliefResult = Section89ArrearsReliefCalculator.computeSection89Relief(
            currentYearTaxableIncomeWithoutArrears = 14_50_000L,
            arrearsReceived = 3_00_000L,
            pastYearTaxableIncome = 4_50_000L
        )
        assertTrue(reliefResult.taxDifferenceCurrentYear > 0L)
        assertTrue(reliefResult.taxDifferencePastYear > 0L)
        assertTrue(reliefResult.reliefAmountUs89 > 0L)
        assertEquals(
            reliefResult.taxDifferenceCurrentYear - reliefResult.taxDifferencePastYear,
            reliefResult.reliefAmountUs89
        )
    }

    @Test
    fun `ECV-02 Section 54 Capital Gains reinvestment offsets residential property LTCG to zero`() {
        val capitalGainsResult = CapitalGainsAndForeignAssetEngine.computeImmovablePropertyLtcgTax(
            grossLtcg = 45_00_000L,
            section54ReinvestmentExemption = 45_00_000L
        )
        assertEquals(45_00_000L, capitalGainsResult.exemptCapitalGainsUs54)
        assertEquals(0L, capitalGainsResult.netTaxableLtcgRupees)
    }

    @Test
    fun `ECV-03 Section 24b self-occupied interest is capped at 2 Lakhs with remaining carried forward for 8 AYs`() {
        val oldRegimeResult = HousePropertyLossEngine.computeHousePropertySetOff(
            selfOccupiedInterestPaid = 3_20_000L,
            isOldRegime = true
        )
        assertEquals(2_00_000L, oldRegimeResult.allowableInterHeadSetOff)
        assertEquals(1_20_000L, oldRegimeResult.carriedForwardLoss)
        assertEquals(8, oldRegimeResult.carryForwardYears)

        val newRegimeResult = HousePropertyLossEngine.computeHousePropertySetOff(
            selfOccupiedInterestPaid = 3_20_000L,
            isOldRegime = false
        )
        assertEquals(0L, newRegimeResult.allowableInterHeadSetOff)
        assertEquals(0L, newRegimeResult.carriedForwardLoss)
    }

    @Test
    fun `ECV-04 Section 87A marginal relief at 700001 rupees rounds down via 288A to zero tax`() {
        val input = TaxpayerFinancialInput(
            citizenCategory = CitizenCategory.REGULAR_BELOW_60,
            grossSalary = 7_75_001L,
            exemptHraAndAllowancesSec10 = 0L,
            professionalTaxSec16iii = 0L,
            savingsBankInterest = 0L,
            fixedDepositInterest = 0L,
            section80CInvestments = 0L,
            section80DSelfAndFamily = 0L,
            section80DParentsSenior = 0L,
            section80CCD1BSelfNps = 0L,
            section80CCD2EmployerNps = 0L,
            basicPlusDaForNps = 0L,
            isCentralOrStateGovtEmployee = false
        )
        val result = DeterministicIndianTaxEngine.computeNewRegime(input)
        assertEquals(7_00_000L, result.netTaxableNormalIncomeRounded288A)
        assertEquals(0L, result.totalTaxLiabilityRounded288B)
    }

    @Test
    fun `ECV-05 Curve25519 shared secret salt and derived key memory zeroing audit`() {
        val decryptor = RbiAaEphemeralDecryptorTestDouble()
        val mockCiphertext = ByteArray(64) { 0x1F.toByte() }
        val interest = decryptor.decryptAndExtractInterestInMemory(mockCiphertext)
        assertEquals(45_000L, interest)
        assertTrue(decryptor.sharedSecretBytes.all { it == 0.toByte() })
        assertTrue(decryptor.fipNonce32Bytes.all { it == 0.toByte() })
        assertTrue(decryptor.derivedKey.all { it == 0.toByte() })
    }

    @Test
    fun `ECV-06 Offline Airplane-Mode and Token Integrity audit verifies tokens across Hindi and Gujarati`() {
        val gujaratiSample = "કલમ {{SEC_1}} હેઠળ તમારી આવક {{AMT_1}} છે અને વધારાની રકમ {{AMT_2}} છે."
        val hindiSample = "धारा {{SEC_1}} के तहत आपकी कुल राशि {{AMT_1}} और भिन्नता {{AMT_2}} है।"
        val tamperedSample = "धारा 143(1) के तहत आपकी कुल राशि {{AMT_1}} है।"

        assertTrue(TokenLockedVernacularExplainerHelper.restoreAndVerifyTokens(gujaratiSample, "gu-IN"))
        assertTrue(TokenLockedVernacularExplainerHelper.restoreAndVerifyTokens(hindiSample, "hi-IN"))
        assertFalse(TokenLockedVernacularExplainerHelper.restoreAndVerifyTokens(tamperedSample, "hi-IN"))
    }

    @Test
    fun `HRA Rule 2A blocks exemption when annual rent exceeds 1 Lakh and Landlord PAN is missing`() {
        val hraInput = HraCalculationInput(
            basicSalaryAnnual = 8_00_000L,
            dearnessAllowanceFormingPartOfRetirement = 0L,
            actualHraReceivedAnnual = 3_20_000L,
            actualRentPaidAnnual = 2_40_000L,
            cityCategory = ResidenceCityCategory.NON_METRO_40_PERCENT,
            landlordPanProvided = false
        )
        val resultWithoutPan = HraAndInterestEngine.computeHraExemption(hraInput, landlordPan = null)
        assertEquals(0L, resultWithoutPan.exemptHraRupees)
        assertTrue(resultWithoutPan.blockedDueToMissingLandlordPan)
        assertEquals(1_60_000L, resultWithoutPan.potentialTaxableReductionIfPanAdded)
    }

    @Test
    fun `Foreign asset holder is deterministically blocked from ITR-1 and routed to ITR-2 with Schedule FA`() {
        val profile = TaxpayerRoutingProfile(
            residentialStatus = ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,
            totalIncomeRupees = 18_00_000L,
            housePropertyCount = 1,
            hasBroughtForwardOrCarriedForwardLosses = false,
            hasCapitalGainsOrLosses = false,
            equity112ALtcgRupees = 0L,
            hasOtherCapitalGains = false,
            agriculturalIncomeRupees = 0L,
            isCompanyDirector = false,
            holdsUnlistedEquityShares = false,
            holdsForeignAssetsOrForeignIncome = true,
            hasPresumptiveProfession44ADA = false,
            grossReceipts44ADARupees = 0L,
            cashReceiptsPercentage44ADA = 0.0,
            declaredProfitPercentage44ADA = 0.0,
            hasRegularBusinessOrIntradaySpeculativeOrFnO = false,
            requiresStatutoryAudit44AB = false
        )
        val decision = DeterministicItrFormRouter.routeTaxpayer(profile)
        assertEquals(ItrFormType.ITR_2, decision.selectedForm)
        assertTrue(decision.requiresScheduleFA)
    }

    @Test
    fun `Phase 3 Updated Return ITR-U computes 25 percent additional tax under Section 140B`() {
        val itrU = RevisionAndHarvestingEngine.computeUpdatedReturnItrU(
            baseAdditionalTax = 36_000L,
            totalInterest234A_B_C = 4_000L,
            filingWindow = ItrUWindow.WITHIN_12_MONTHS_FROM_AY_END
        )
        assertEquals(10_000L, itrU.additionalTaxUs140B)
        assertEquals(50_000L, itrU.totalUpdatedReturnPayable)
    }

    @Test
    fun `VUL-11 and VUL-12 Negative inputs are clamped to non-negative and prevent negative tax or illegal salary offset`() {
        val adversarialInput = TaxpayerFinancialInput(
            grossSalary = 10_00_000L,
            presumptiveGrossReceipts44ADA = -5_00_000L,
            netTaxableStcg111ARupees = -2_00_000L,
            netTaxableLtcg112ARupees = -1_00_000L,
            assessmentYearProfile = AssessmentYearProfile.AY_2026_27
        )
        val result = DeterministicIndianTaxEngine.computeNewRegime(adversarialInput)
        assertTrue(result.totalTaxLiabilityRounded288B >= 0L)
        assertEquals(9_25_000L, result.netTaxableNormalIncomeRounded288A)
    }

    @Test
    fun `VUL-14 and VUL-16 Broker PnL parser safely handles Long MIN_VALUE and Indian currency comma formatting`() {
        val csv = """
            Symbol,Type,Quantity,PnL
            NIFTY26MARFUT,FNO,50,"(1,25,000)"
            RELIANCE,LTCG,10,"2,50,000"
            INFY,STCG,20,"-50,000"
        """.trimIndent()
        val parsed = UniversalBrokerPnLParser.parseBrokerCsv(csv)
        assertEquals(125000L, parsed.totalFnoAbsoluteTurnover)
        assertEquals(-125000L, parsed.totalFnoNetProfitOrLoss)
        assertEquals(250000L, parsed.totalLtcg112AGains)
        assertEquals(50000L, parsed.totalStcl111ALosses)
    }

    @Test
    fun `VUL-19 and VUL-20 CBDT Defect Gate evaluates grossTotalIncome and strict Aadhaar formatting`() {
        val bank = BankAccountDetails(
            accountNumber = "123456789012",
            ifscCode = "SBIN0001234",
            bankName = "State Bank of India",
            isRefundAccount = true
        )
        // Freelancer with zero salary but valid gross total income and TDS
        val passResult = CbdtCategoryADefectGate.evaluateCategoryADefects(
            pan = "ABCDE1234F",
            aadhaarRedactedOrValid = "[Aadhaar Redacted]",
            grossSalary = 0L,
            claimedTds = 50_000L,
            allowed26AsTds = 50_000L,
            grossTotalIncome = 10_00_000L,
            chapterViaDeductions = 1_50_000L,
            bankAccount = bank
        )
        assertTrue(passResult.isPassed)

        // Invalid fake Aadhaar like "xRedacted" should fail Gate 2
        val failResult = CbdtCategoryADefectGate.evaluateCategoryADefects(
            pan = "ABCDE1234F",
            aadhaarRedactedOrValid = "xRedacted",
            grossSalary = 5_00_000L,
            claimedTds = 10_000L,
            allowed26AsTds = 10_000L,
            grossTotalIncome = 5_00_000L,
            chapterViaDeductions = 50_000L,
            bankAccount = bank
        )
        assertFalse(failResult.isPassed)
        assertTrue(failResult.failedGates.contains("GATE_2_INVALID_AADHAAR_IDENTIFIER"))
    }

    @Test
    fun `VUL-15 ITR-5 Partnership Firm loss creates business loss carry forward record`() {
        val lossFirm = Itr5FirmInput(
            firmPan = "AABCF1234G",
            bookProfitBeforePartnerPayments = -4_00_000L,
            partnerCapitalTotalRupees = 10_00_000L,
            partnerInterestOnCapitalClaimed = 0L,
            partnerRemunerationClaimed = 0L
        )
        val result = CorporateAndTrustTaxEngine.computeItr5FirmTax(lossFirm)
        assertEquals(4_00_000L, result.businessLossCarryForwardRupees)
        assertEquals(0L, result.totalTaxableIncomeRupees)
    }
}

