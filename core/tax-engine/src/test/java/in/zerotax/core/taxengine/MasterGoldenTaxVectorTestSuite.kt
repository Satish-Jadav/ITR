package in.zerotax.core.taxengine

import in.zerotax.core.model.AssessmentYearProfile
import in.zerotax.core.model.CitizenCategory
import in.zerotax.core.model.EmployerSalarySource
import in.zerotax.core.model.FormRoutingDecision
import in.zerotax.core.model.ItrFormType
import in.zerotax.core.model.ReconciledSalarySummary
import in.zerotax.core.model.ResidentialStatus
import in.zerotax.core.model.SourceKind
import in.zerotax.core.model.TaxRegime
import in.zerotax.core.model.TaxpayerRoutingProfile
import in.zerotax.core.taxengine.advanced.*
import in.zerotax.core.taxengine.calculator.*
import in.zerotax.core.taxengine.compiler.CbdtItr1JsonCompiler
import in.zerotax.core.taxengine.reconciliation.*
import in.zerotax.core.taxengine.routing.DeterministicItrFormRouter
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Pure-JVM Master Indian Tax Engine Golden Verification")
class MasterGoldenTaxVectorTestSuite {

    @Nested
    @DisplayName("Deterministic Reconciler Tests")
    inner class ReconcilerTests {
        @Test
        fun `reconciliation respects Form 26AS TDS ceiling on mismatch`() {
            val sources = listOf(
                EmployerSalarySource(
                    employerTan = "BLRM01234D",
                    employerName = "Tech Corp",
                    source = SourceKind.FORM_16,
                    grossSalary = 12_00_000L,
                    sec10Exemptions = 50_000L,
                    sec16DeductionsClaimedInDoc = 50_000L,
                    netTaxableSalaryInDoc = 11_00_000L,
                    tdsReported = 1_50_000L
                )
            )
            val form26As = mapOf("BLRM01234D" to 1_20_000L)
            val result = DeterministicReconciler.reconcileSalaries(sources, form26As)

            assertEquals(1_20_000L, result.aggregateTdsAllowed)
            assertEquals(30_000L, result.totalDiscrepancyAmount)
            assertTrue(result.employers.first().hasDiscrepancy)
        }

        @Test
        fun `form 16 math sanity check passes within 5 rupee tolerance`() {
            val valid = EmployerSalarySource(
                employerTan = "BLRM01234D",
                employerName = "Tech Corp",
                grossSalary = 10_00_003L,
                sec10Exemptions = 20_000L,
                sec16DeductionsClaimedInDoc = 50_000L,
                netTaxableSalaryInDoc = 9_30_000L, // 10,00,003 - 20,000 - 50,000 = 9,30,003 (diff = 3 <= 5)
                tdsReported = 50_000L
            )
            assertTrue(DeterministicReconciler.passesForm16SanityCheck(valid))

            val invalid = valid.copy(netTaxableSalaryInDoc = 9_20_000L) // diff = 10,003 > 5
            assertFalse(DeterministicReconciler.passesForm16SanityCheck(invalid))
        }
    }

    @Nested
    @DisplayName("Section 87A & AY 2026-27 Slabs")
    inner class SlabTests {
        @Test
        fun `new regime AY 2026-27 rebate covers up to 12 lakh normal income`() {
            val input = TaxpayerFinancialInput(
                citizenCategory = CitizenCategory.REGULAR_BELOW_60,
                grossSalary = 12_75_000L, // 12,75,000 - 75,000 std ded = 12,00,000
                assessmentYearProfile = AssessmentYearProfile.AY_2026_27
            )
            val result = DeterministicIndianTaxEngine.computeNewRegime(input)

            assertEquals(12_00_000L, result.netTaxableNormalIncomeRounded288A)
            assertEquals(0L, result.totalTaxLiabilityRounded288B)
            assertTrue(result.rebate87A > 0L)
        }

        @Test
        fun `marginal relief prevents cliff edge beyond rebate threshold`() {
            val input = TaxpayerFinancialInput(
                citizenCategory = CitizenCategory.REGULAR_BELOW_60,
                grossSalary = 7_77_000L, // Std ded 75k => 7,02,000
                assessmentYearProfile = AssessmentYearProfile.AY_2025_26
            )
            val result = DeterministicIndianTaxEngine.computeNewRegime(input)

            assertEquals(7_02_000L, result.netTaxableNormalIncomeRounded288A)
            val excessOver7Lakh = 2_000L
            assertTrue(result.taxAfterRebateAndRelief <= excessOver7Lakh)
        }
    }

    @Nested
    @DisplayName("Capital Gains Grandfathering & Foreign Tax Credits")
    inner class AdvancedCapitalGainsTests {
        @Test
        fun `verify Sec 112A grandfathering FMV formula`() {
            val gain = CapitalGainsAndForeignAssetEngine.computeGrandfathered112AGain(
                acquisitionDateEpochDay = 17000L, // Prior to 31-Jan-2018
                actualCostTotal = 1_00_000L,
                fmvJan31_2018Total = 2_50_000L,
                saleConsiderationTotal = 3_00_000L,
                transferExpenses = 2_000L
            )
            // Effective Cost = min(250000, 300000) = 250000
            // Gain = 300000 - 250000 - 2000 = 48000
            assertEquals(48_000L, gain)
        }

        @Test
        fun `form 67 foreign tax credit restricted to Indian tax liability rate`() {
            val (grossInr, ftcAllowed) = CapitalGainsAndForeignAssetEngine.computeForm67ForeignTaxCredit(
                foreignDividendUsd = 1_000.0,
                foreignTaxWithheldUsd = 250.0,
                precedingMonthEndSbiTtbr = 84.0,
                indianEffectiveTaxRateOnTotalIncome = 0.20
            )
            assertEquals(84_000L, grossInr)
            assertEquals(16_800L, ftcAllowed) // 84,000 * 20% = 16,800 <= 21,000 US tax
        }
    }

    @Nested
    @DisplayName("Rule 2A HRA & Section 234 Interest")
    inner class HraAndInterestTests {
        @Test
        fun `metro HRA calculation caps at lowest of 3 conditions`() {
            val input = HraComputationInput(
                actualHraReceived = 3_00_000L,
                rentPaidTotal = 3_60_000L,
                salaryBasicPlusDa = 12_00_000L,
                isMetroCity = true
            )
            // 1. Actual: 3,00,000
            // 2. Rent - 10% Sal: 3,60,000 - 1,20,000 = 2,40,000
            // 3. 50% Sal: 6,00,000
            val exemption = HraAndInterestEngine.computeHraExemptionRule2A(input)
            assertEquals(2_40_000L, exemption)
        }

        @Test
        fun `section 234B interest triggers when advance tax is below 90 percent`() {
            val interest = HraAndInterestEngine.computeMandatoryInterests(
                assessedTaxLiability = 2_00_000L,
                advanceTaxPaidByQuarter = listOf(20_000L, 20_000L, 20_000L, 20_000L), // 80k paid < 180k (90%)
                tdsTcsCredit = 0L,
                filingDelayMonthsSec234A = 0
            )
            assertTrue(interest.interest234B > 0L)
        }
    }

    @Nested
    @DisplayName("Tax-Loss Harvesting & Revision Differential")
    inner class HarvestingTests {
        @Test
        fun `harvesting prioritizes highest loss against realized gains`() {
            val opportunities = RevisionAndHarvestingEngine.calculateHarvestingOpportunities(
                realizedShortTermGains = 1_00_000L,
                unrealizedScripLosses = mapOf("SCRIP_A" to 60_000L, "SCRIP_B" to 80_000L)
            )
            assertEquals(2, opportunities.size)
            assertEquals(60_000L, opportunities[0].harvestableAgainstGain)
            assertEquals(40_000L, opportunities[1].harvestableAgainstGain)
        }
    }

    @Nested
    @DisplayName("CBDT ITR-1 JSON Compiler")
    inner class CompilerTests {
        @Test
        fun `compiler generates compliant JSON output`() {
            val computation = RegimeComputationResult(
                regime = TaxRegime.NEW_REGIME_115BAC,
                grossTotalIncome = 10_00_000L,
                standardDeduction = 75_000L,
                totalChapterVIADeductions = 0L,
                netTaxableNormalIncomeRounded288A = 9_25_000L,
                netTaxableSpecialRateIncome = 0L,
                baseSlabTaxOnNormalIncome = 42_500L,
                specialRateTax111AAnd112A = 0L,
                rebate87A = 0L,
                marginalRelief87A = 0L,
                taxAfterRebateAndRelief = 42_500L,
                surchargeAfterMarginalRelief = 0L,
                healthAndEducationCess = 1_700L,
                totalTaxLiabilityRounded288B = 44_200L
            )
            val summary = ReconciledSalarySummary(
                employers = emptyList(),
                aggregateGrossSalary = 10_00_000L,
                aggregateSec10Exemptions = 0L,
                aggregateNetTaxableSalary = 9_25_000L,
                aggregateTdsAllowed = 44_200L,
                totalDiscrepancyAmount = 0L
            )
            val jsonStr = CbdtItr1JsonCompiler.compileItr1Json("ABCDE1234F", "2025-26", summary, computation)
            val json = JSONObject(jsonStr)

            assertTrue(json.has("ITR1"))
            assertEquals("ABCDE1234F", json.getJSONObject("ITR1").getJSONObject("PersonalInfo").getString("PAN"))
            assertEquals(44_200L, json.getJSONObject("ITR1").getJSONObject("TaxComputation").getLong("TotalTaxLiability"))
        }
    }

    @Nested
    @DisplayName("Deterministic ITR Form Router Tests")
    inner class RouterTests {
        @Test
        fun `salaried individual below 50L routes to ITR-1 Sahaj`() {
            val profile = TaxpayerRoutingProfile(
                residentialStatus = ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,
                totalIncomeRupees = 25_00_000L,
                housePropertyCount = 1
            )
            val decision = DeterministicItrFormRouter.routeTaxpayer(profile)
            assertEquals(ItrFormType.ITR_1_SAHAJ, decision.selectedForm)
        }

        @Test
        fun `foreign asset holding forces ITR-2 and Schedule FA`() {
            val profile = TaxpayerRoutingProfile(
                residentialStatus = ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,
                totalIncomeRupees = 30_00_000L,
                holdsForeignAssetsOrForeignIncome = true
            )
            val decision = DeterministicItrFormRouter.routeTaxpayer(profile)
            assertEquals(ItrFormType.ITR_2, decision.selectedForm)
            assertTrue(decision.requiresScheduleFA)
        }

        @Test
        fun `eligible freelance profession routes to ITR-4 Sugam`() {
            val profile = TaxpayerRoutingProfile(
                residentialStatus = ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,
                totalIncomeRupees = 20_00_000L,
                hasPresumptiveProfession44ADA = true,
                grossReceipts44ADARupees = 40_00_000L,
                declaredProfitPercentage44ADA = 50.0
            )
            val decision = DeterministicItrFormRouter.routeTaxpayer(profile)
            assertEquals(ItrFormType.ITR_4_SUGAM, decision.selectedForm)
        }
    }

    @Nested
    @DisplayName("Home Loan EMI & Taxable Income Simulator Tests")
    inner class HomeLoanSimulatorTests {
        @Test
        fun `verify EMI split enforces Section 24b and Section 80C caps`() {
            // ₹40,000/mo EMI => Annual ₹4,80,000
            val impact = HomeLoanAndDeductionCalculator.calculateHomeLoanDeduction(monthlyEmi = 40_000L)

            assertEquals(4_80_000L, impact.annualTotalEmiPaid)
            // Sec 24(b) interest capped at ₹2,00,000
            assertEquals(2_00_000L, impact.sec24bInterestDeductionAllowed)
            // Sec 80C principal capped at ₹1,50,000
            assertEquals(1_50_000L, impact.sec80cPrincipalDeductionAllowed)
            // Combined home loan deduction capped at ₹3,50,000
            assertEquals(3_50_000L, impact.totalHomeLoanDeduction)
            assertTrue(impact.unclaimedExcessInterest > 0L)
        }

        @Test
        fun `verify home loan deduction reduces taxable income and saves cash tax`() {
            val homeLoanImpact = HomeLoanAndDeductionCalculator.calculateHomeLoanDeduction(monthlyEmi = 38_000L)
            val taxImpact = HomeLoanAndDeductionCalculator.computeTaxImpact(
                grossSalary = 15_00_000L,
                otherDeductions = 50_000L,
                homeLoanImpact = homeLoanImpact
            )

            assertEquals(15_00_000L, taxImpact.grossSalary)
            // New taxable income must be strictly less than original taxable income
            assertTrue(taxImpact.newTaxableIncome < taxImpact.originalTaxableIncome)
            // Tax saved must be positive and direct cash relief
            assertTrue(taxImpact.netCashTaxSaved > 50_000L)
        }
    }
}
