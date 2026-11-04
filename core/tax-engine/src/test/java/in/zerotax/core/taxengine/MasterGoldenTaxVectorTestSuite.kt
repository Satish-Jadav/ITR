package `in`.zerotax.core.taxengine

import `in`.zerotax.core.model.AssessmentYearProfile
import `in`.zerotax.core.model.CitizenCategory
import `in`.zerotax.core.model.EmployerSalarySource
import `in`.zerotax.core.model.FormRoutingDecision
import `in`.zerotax.core.model.ItrFormType
import `in`.zerotax.core.model.ReconciledSalarySummary
import `in`.zerotax.core.model.ResidentialStatus
import `in`.zerotax.core.model.SourceKind
import `in`.zerotax.core.model.TaxRegime
import `in`.zerotax.core.model.TaxpayerRoutingProfile
import `in`.zerotax.core.model.Equity112ALot
import `in`.zerotax.core.model.ImmovablePropertyLtcgLot
import `in`.zerotax.core.model.ForeignEquityLotInput
import `in`.zerotax.core.model.DonationCategory
import `in`.zerotax.core.model.Section80GDonationInput
import `in`.zerotax.core.taxengine.foreignassets.SbiTtbrLookupRepository
import `in`.zerotax.core.taxengine.foreignassets.ScheduleFaCalendarYearEngine
import `in`.zerotax.core.taxengine.revision.ItrUWindow
import `in`.zerotax.core.taxengine.revision.RevisionAndHarvestingEngine
import `in`.zerotax.core.taxengine.advanced.*
import `in`.zerotax.core.taxengine.calculator.*
import `in`.zerotax.core.taxengine.compiler.CbdtItr1JsonCompiler
import `in`.zerotax.core.taxengine.reconciliation.*
import `in`.zerotax.core.taxengine.routing.DeterministicItrFormRouter
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

    @Nested
    @DisplayName("Pre-Filing 6-Gate Audit Shield Tests")
    inner class PreFilingAuditShieldTests {
        @Test
        fun `audit return passes with 100 score and low risk when all gates clear`() {
            val salarySummary = ReconciledSalarySummary(
                employers = listOf(
                    `in`.zerotax.core.model.EmployerReconciliationItem(
                        employerTan = "BLRM01234D",
                        employerName = "Infosys",
                        selectedSourceKind = SourceKind.FORM_16,
                        grossSalary = 1200000L,
                        sec10Exemptions = 0L,
                        sec16Deductions = 75000L,
                        netSalary = 1125000L,
                        tdsReportedInSource = 50000L,
                        tdsIn26As = 50000L,
                        tdsAllowed = 50000L,
                        hasDiscrepancy = false,
                        unreflectedTdsAmount = 0L
                    )
                ),
                aggregateGrossSalary = 1200000L,
                aggregateSec10Exemptions = 0L,
                aggregateNetTaxableSalary = 1125000L,
                aggregateTdsAllowed = 50000L,
                totalDiscrepancyAmount = 0L
            )
            val computation = RegimeComputationResult(
                regime = TaxRegime.NEW_REGIME_115BAC,
                grossTotalIncome = 1200000L,
                standardDeduction = 75000L,
                totalChapterVIADeductions = 0L,
                netTaxableNormalIncomeRounded288A = 1125000L,
                netTaxableSpecialRateIncome = 0L,
                baseSlabTaxOnNormalIncome = 50000L,
                specialRateTax111AAnd112A = 0L,
                rebate87A = 0L,
                marginalRelief87A = 0L,
                taxAfterRebateAndRelief = 50000L,
                surchargeAfterMarginalRelief = 0L,
                healthAndEducationCess = 2000L,
                totalTaxLiabilityRounded288B = 52000L
            )
            val bank = `in`.zerotax.core.model.BankAccountDetails(
                bankName = "State Bank of India",
                accountNumber = "123456789012",
                ifscCode = "SBIN0001234",
                accountType = "SAVINGS",
                isPreValidated = true,
                isRefundAccount = true
            )

            val report = PreFilingAuditEngine.auditReturn(salarySummary, computation, bank, hasOverridesWithoutProof = false)

            assertEquals(100, report.auditScorePercentage)
            assertEquals(`in`.zerotax.core.model.NoticeRiskLevel.LOW_RISK_SAFE, report.riskLevel)
            assertTrue(report.canFileDirectly)
            assertTrue(report.noticeProtectionGuarantee.contains("< 0.1%"))
        }

        @Test
        fun `audit return flags TDS mismatch and unvalidated bank account`() {
            val summaryWithMismatch = ReconciledSalarySummary(
                employers = emptyList(),
                aggregateGrossSalary = 1000000L,
                aggregateSec10Exemptions = 0L,
                aggregateNetTaxableSalary = 925000L,
                aggregateTdsAllowed = 20000L,
                totalDiscrepancyAmount = 15000L // ₹15,000 TDS mismatch
            )
            val computation = RegimeComputationResult(
                regime = TaxRegime.NEW_REGIME_115BAC,
                grossTotalIncome = 1000000L,
                standardDeduction = 75000L,
                totalChapterVIADeductions = 0L,
                netTaxableNormalIncomeRounded288A = 925000L,
                netTaxableSpecialRateIncome = 0L,
                baseSlabTaxOnNormalIncome = 30000L,
                specialRateTax111AAnd112A = 0L,
                rebate87A = 0L,
                marginalRelief87A = 0L,
                taxAfterRebateAndRelief = 30000L,
                surchargeAfterMarginalRelief = 0L,
                healthAndEducationCess = 1200L,
                totalTaxLiabilityRounded288B = 31200L
            )

            val report = PreFilingAuditEngine.auditReturn(summaryWithMismatch, computation, bankAccount = null)

            assertFalse(report.gates.first { it.gateNumber == 1 }.isPassed) // Gate 1 fails
            assertFalse(report.gates.first { it.gateNumber == 5 }.isPassed) // Gate 5 fails (bank null)
            assertTrue(report.auditScorePercentage < 100)
        }
    }

    @Nested
    @DisplayName("Schedule FA Foreign Asset & Calendar Year Engine Tests")
    inner class ScheduleFaCalendarYearTests {
        @Test
        fun `compiles Schedule FA Table A3 entries with SBI TTBR conversion`() {
            val holdings = listOf(
                ForeignEquityHolding(
                    companyName = "Alphabet Inc (GOOGL)",
                    countryCode = "2", // USA
                    dateOfAcquisitionEpochDay = 19000L,
                    peakValueUsd = 10000.0,
                    closingValueUsd = 9500.0,
                    grossDividendsUsd = 200.0,
                    grossSalesProceedsUsd = 1000.0
                )
            )

            val tableA3 = ScheduleFaCalendarYearEngine.compileScheduleFaEntries(
                holdings = holdings,
                sbiTtbrClosingDateRate = 85.0,
                sbiTtbrPeakDateRate = 86.0
            )

            assertEquals(1, tableA3.size)
            val row = tableA3.first()
            assertEquals("Alphabet Inc (GOOGL)", row.entityName)
            assertEquals("2", row.countryCode)
            assertEquals(860000L, row.peakValueInr) // 10,000 * 86.0
            assertEquals(807500L, row.closingValueInr) // 9,500 * 85.0
            assertEquals(17000L, row.grossDividendsPaidInr) // 200 * 85.0
            assertEquals(85000L, row.proceedsFromSaleInr) // 1000 * 85.0
        }
    }

    @Nested
    @DisplayName("Smart Deductions Catalog Statutory Coverage Tests")
    inner class SmartDeductionsCatalogTests {
        @Test
        fun `catalog includes statutory specs for all major sections with accurate limits`() {
            val specs = SmartDeductionsCatalog.ALL_DEDUCTION_SPECS

            val sec80C = specs.firstOrNull { it.sectionCode.contains("80C") }
            assertNotNull(sec80C)
            assertEquals(150000L, sec80C!!.maxLimitRupees)

            val sec24b = specs.firstOrNull { it.sectionCode == "24(b)" }
            assertNotNull(sec24b)
            assertEquals(200000L, sec24b!!.maxLimitRupees)

            val sec80D = specs.firstOrNull { it.sectionCode.contains("80D") }
            assertNotNull(sec80D)
            assertTrue(sec80D!!.maxLimitRupees >= 25000L)
        }
    }

    @Nested
    @DisplayName("GTV-01 through GTV-18 Master Golden Test Vectors")
    inner class MasterGoldenVectors {

        private fun baseInput(
            assessmentYearProfile: AssessmentYearProfile = AssessmentYearProfile.AY_2025_26,
            citizenCategory: CitizenCategory = CitizenCategory.REGULAR_BELOW_60,
            grossSalary: Long = 0L,
            savingsBankInterest: Long = 0L,
            fixedDepositInterest: Long = 0L,
            section80CInvestments: Long = 0L,
            section80DSelfAndFamily: Long = 0L,
            section80CCD2EmployerNps: Long = 0L,
            basicPlusDaForNps: Long = 0L,
            netTaxableLtcg112ARupees: Long = 0L
        ) = TaxpayerFinancialInput(
            citizenCategory = citizenCategory,
            isResidentIndividual = true,
            grossSalary = grossSalary,
            exemptHraAndAllowancesSec10 = 0L,
            professionalTaxSec16iii = 0L,
            selfOccupiedHomeLoanInterestSec24b = 0L,
            savingsBankInterest = savingsBankInterest,
            fixedDepositInterest = fixedDepositInterest,
            familyPensionReceived = 0L,
            section80CInvestments = section80CInvestments,
            section80DSelfAndFamily = section80DSelfAndFamily,
            section80DParentsSenior = 0L,
            section80CCD1BSelfNps = 0L,
            section80CCD2EmployerNps = section80CCD2EmployerNps,
            basicPlusDaForNps = basicPlusDaForNps,
            isCentralOrStateGovtEmployee = false,
            donations80G = emptyList(),
            presumptiveGrossReceipts44ADA = 0L,
            presumptiveDeclaredProfitRatePercent = 50.0,
            netTaxableStcg111ARupees = 0L,
            netTaxableLtcg112ARupees = netTaxableLtcg112ARupees,
            assessmentYearProfile = assessmentYearProfile
        )

        @Test
        fun `GTV-01 New Regime Sec 87A Rebate ceiling at 7L for AY 2025-26`() {
            val input = baseInput(
                assessmentYearProfile = AssessmentYearProfile.AY_2025_26,
                grossSalary = 7_75_000L
            )
            val result = DeterministicIndianTaxEngine.computeNewRegime(input)
            assertEquals(75_000L, result.standardDeduction)
            assertEquals(7_00_000L, result.netTaxableNormalIncomeRounded288A)
            assertEquals(20_000L, result.baseSlabTaxOnNormalIncome)
            assertEquals(20_000L, result.rebate87A)
            assertEquals(0L, result.totalTaxLiabilityRounded288B)
        }

        @Test
        fun `GTV-02 New Regime Sec 87A Marginal Relief at 7_10L for AY 2025-26`() {
            val input = baseInput(
                assessmentYearProfile = AssessmentYearProfile.AY_2025_26,
                grossSalary = 7_85_000L
            )
            val result = DeterministicIndianTaxEngine.computeNewRegime(input)
            assertEquals(7_10_000L, result.netTaxableNormalIncomeRounded288A)
            assertEquals(21_000L, result.baseSlabTaxOnNormalIncome)
            assertEquals(11_000L, result.marginalRelief87A)
            assertEquals(10_000L, result.taxAfterRebateAndRelief)
            assertEquals(400L, result.healthAndEducationCess)
            assertEquals(10_400L, result.totalTaxLiabilityRounded288B)
        }

        @Test
        fun `GTV-03 New Regime Sec 87A Enhanced Rebate ceiling at 12L for AY 2026-27`() {
            val input = baseInput(
                assessmentYearProfile = AssessmentYearProfile.AY_2026_27,
                grossSalary = 12_75_000L
            )
            val result = DeterministicIndianTaxEngine.computeNewRegime(input)
            assertEquals(75_000L, result.standardDeduction)
            assertEquals(12_00_000L, result.netTaxableNormalIncomeRounded288A)
            assertEquals(60_000L, result.baseSlabTaxOnNormalIncome)
            assertEquals(60_000L, result.rebate87A)
            assertEquals(0L, result.totalTaxLiabilityRounded288B)
        }

        @Test
        fun `GTV-04 New Regime Sec 87A Marginal Relief at 12_10L for AY 2026-27`() {
            val input = baseInput(
                assessmentYearProfile = AssessmentYearProfile.AY_2026_27,
                grossSalary = 12_85_000L
            )
            val result = DeterministicIndianTaxEngine.computeNewRegime(input)
            assertEquals(12_10_000L, result.netTaxableNormalIncomeRounded288A)
            assertEquals(61_500L, result.baseSlabTaxOnNormalIncome)
            assertEquals(51_500L, result.marginalRelief87A)
            assertEquals(10_000L, result.taxAfterRebateAndRelief)
            assertEquals(400L, result.healthAndEducationCess)
            assertEquals(10_400L, result.totalTaxLiabilityRounded288B)
        }

        @Test
        fun `GTV-05 Multi-Employer TAN De-Duplication and Single Std Deduction`() {
            val sources = listOf(
                EmployerSalarySource("MUMT11111A", "Emp 1", SourceKind.FORM_16, 9_00_000L, 0L, 75_000L, 8_25_000L, 45_000L),
                EmployerSalarySource("BLRI22222B", "Emp 2", SourceKind.FORM_16, 11_00_000L, 0L, 75_000L, 10_25_000L, 75_000L),
                EmployerSalarySource("MUMT11111A", "Emp 1 AIS", SourceKind.AIS_TDS_192, 9_00_000L, 0L, 0L, 9_00_000L, 45_000L)
            )
            val reconciled = DeterministicReconciler.reconcileSalaries(
                sources = sources,
                form26AsTdsByTan = mapOf("MUMT11111A" to 45_000L, "BLRI22222B" to 75_000L)
            )
            assertEquals(20_00_000L, reconciled.aggregateGrossSalary)
            val result = DeterministicIndianTaxEngine.computeNewRegime(
                baseInput(grossSalary = reconciled.aggregateGrossSalary)
            )
            assertEquals(75_000L, result.standardDeduction)
            assertEquals(19_25_000L, result.netTaxableNormalIncomeRounded288A)
        }

        @Test
        fun `GTV-06 Senior Citizen Sec 80TTB 50000 deduction on Savings and FD interest`() {
            val input = baseInput(
                citizenCategory = CitizenCategory.SENIOR_60_TO_79,
                grossSalary = 6_50_000L,
                savingsBankInterest = 15_000L,
                fixedDepositInterest = 42_000L,
                section80CInvestments = 1_50_000L,
                section80DSelfAndFamily = 50_000L
            )
            val result = DeterministicIndianTaxEngine.computeOldRegime(input)
            assertEquals(50_000L, result.standardDeduction)
            assertEquals(6_57_000L, result.grossTotalIncome)
            assertEquals(2_50_000L, result.totalChapterVIADeductions)
            assertEquals(4_07_000L, result.netTaxableNormalIncomeRounded288A)
            assertEquals(0L, result.totalTaxLiabilityRounded288B)
        }

        @Test
        fun `GTV-07 New Regime Sec 80CCD2 Employer NPS 14 pct deduction`() {
            val input = baseInput(
                grossSalary = 16_75_000L,
                basicPlusDaForNps = 10_00_000L,
                section80CCD2EmployerNps = 1_40_000L
            )
            val result = DeterministicIndianTaxEngine.computeNewRegime(input)
            assertEquals(75_000L, result.standardDeduction)
            assertEquals(16_00_000L, result.grossTotalIncome)
            assertEquals(1_40_000L, result.totalChapterVIADeductions)
            assertEquals(14_60_000L, result.netTaxableNormalIncomeRounded288A)
        }

        @Test
        fun `GTV-08 Section 87A Rebate never offsets Section 112A LTCG tax under Section 112A(6)`() {
            val input = baseInput(
                assessmentYearProfile = AssessmentYearProfile.AY_2025_26,
                grossSalary = 4_75_000L,
                netTaxableLtcg112ARupees = 2_25_000L
            )
            val result = DeterministicIndianTaxEngine.computeNewRegime(input)
            assertEquals(5_000L, result.rebate87A)
            assertEquals(12_500L, result.specialRateTax111AAnd112A)
            assertEquals(13_000L, result.totalTaxLiabilityRounded288B)
        }

        @Test
        fun `GTV-09 Sec 112A Grandfathering Dip prevents artificial loss`() {
            val equityLot = Equity112ALot(
                isin = "INE002A01018",
                acquisitionDateEpochDay = 16500L, // Prior to Jan 31, 2018 (17562L)
                actualCostTotal = 2_00_000L,
                fmvJan31_2018Total = 5_50_000L,
                saleConsiderationTotal = 4_80_000L,
                transferExpenses = 0L
            )
            val gain = CapitalGainsAndForeignAssetEngine.computeGrandfathered112AGain(equityLot)
            assertEquals(0L, gain)
        }

        @Test
        fun `GTV-10 Pre-July 23 2024 Immovable Property LTCG compares 12_5 pct unindexed vs 20 pct CII 376 indexed`() {
            val propertyLot = ImmovablePropertyLtcgLot(
                acquiredBeforeJuly23_2024 = true,
                purchaseCostRupees = 50_00_000L,
                improvementCostRupees = 0L,
                acquisitionFyCii = 254,
                transferFyCii = 376,
                saleConsiderationOrStampValue50C = 80_00_000L,
                transferExpenses = 0L,
                section54ReinvestmentExemption = 0L
            )
            val comparison = CapitalGainsAndForeignAssetEngine.computeImmovablePropertyLtcgTax(propertyLot)
            assertEquals(3_75_000L, comparison.taxAt12Point5PercentNoIndexation)
            assertEquals(1_19_685L, comparison.taxAt20PercentWithIndexation)
            assertEquals(1_19_685L, comparison.optimalTaxPayableForResidentIndividual)
        }

        @Test
        fun `GTV-11 Capital Loss Matrix blocks LTCL against STCG and carries forward`() {
            val summary = CapitalGainsAndForeignAssetEngine.applyCapitalLossSetOff(
                currentYearStcg = 3_00_000L,
                currentYearStcl = 0L,
                currentYearLtcg = 0L,
                currentYearLtcl = 1_80_000L,
                broughtForwardStcl = 50_000L,
                broughtForwardLtcl = 0L
            )
            assertEquals(2_50_000L, summary.netTaxableStcg15Or20Percent)
            assertEquals(1_80_000L, summary.carriedForwardLtclToNext8Years)
        }

        @Test
        fun `GTV-12 Section 80G disallows cash donations above 2000 and caps qualifying donations at 10 pct of AGTI`() {
            val donations = listOf(
                Section80GDonationInput("AAATC1234A", 5_000L, 0L, DonationCategory.FIFTY_PERCENT_NO_LIMIT),
                Section80GDonationInput("AAATC5678B", 0L, 1_50_000L, DonationCategory.FIFTY_PERCENT_WITH_10PCT_CAP)
            )
            val allowed80G = DeterministicIndianTaxEngine.computeSection80GDeduction(
                donations = donations,
                adjustedGti = 10_00_000L
            )
            assertEquals(50_000L, allowed80G)
        }

        @Test
        fun `GTV-13 Surcharge Marginal Relief just above 50L threshold`() {
            val input50L = baseInput(grossSalary = 50_75_000L)
            val input51L = baseInput(grossSalary = 51_75_000L)
            val res50L = DeterministicIndianTaxEngine.computeNewRegime(input50L)
            val res51L = DeterministicIndianTaxEngine.computeNewRegime(input51L)
            assertEquals(51_00_000L, res51L.netTaxableNormalIncomeRounded288A)
            val incrementalTaxPlusSurcharge = (res51L.taxAfterRebateAndRelief + res51L.surchargeAfterMarginalRelief) - res50L.taxAfterRebateAndRelief
            assertEquals(1_00_000L, incrementalTaxPlusSurcharge)
        }

        @Test
        fun `GTV-14 Rule 119A rounds down unpaid principal to nearest 100 and exempts Senior Citizens under Section 207`() {
            val regularBreakdown = HraAndInterestEngine.computeStatutoryInterestAndLateFee(
                totalTaxLiabilityAfterCess = 85_689L,
                totalTdsAndTcsCredit = 40_000L,
                advanceTaxPaidByMarch31 = 0L,
                monthsDelayedAfterDueDate234A = 0,
                monthsFromApril1ToFilingMonth234B = 3,
                netTaxableIncome = 12_00_000L,
                isSeniorCitizenWithNoBusinessIncome = false
            )
            assertEquals(1_368L, regularBreakdown.interest234B)

            val seniorBreakdown = HraAndInterestEngine.computeStatutoryInterestAndLateFee(
                totalTaxLiabilityAfterCess = 85_689L,
                totalTdsAndTcsCredit = 40_000L,
                advanceTaxPaidByMarch31 = 0L,
                monthsDelayedAfterDueDate234A = 0,
                monthsFromApril1ToFilingMonth234B = 3,
                netTaxableIncome = 12_00_000L,
                isSeniorCitizenWithNoBusinessIncome = true
            )
            assertEquals(0L, seniorBreakdown.interest234B)
        }

        @Test
        fun `GTV-15 Schedule FA Sunday vest date rolls back to Friday SBI TTBR`() {
            val repo = object : SbiTtbrLookupRepository {
                override fun getUsdTtbrOnOrBefore(isoDate: String): Double = when (isoDate) {
                    "2025-03-16" -> 86.80 // Rolled back to Friday 2025-03-14
                    "2025-08-14" -> 86.95
                    else -> 85.65
                }
            }
            val engine = ScheduleFaCalendarYearEngine(repo)
            val row = engine.buildTableA3RowForLot(
                lot = ForeignEquityLotInput(
                    lotId = "LOT-1",
                    companyName = "Alphabet Inc.",
                    companyAddress = "1600 Amphitheatre Pkwy",
                    zipCode = "94043",
                    acquireDateIso = "2025-03-16",
                    sharesVestedOrBought = 100.0,
                    sharesSoldDuringCalendarYear = 0.0,
                    acquisitionPricePerShareUsd = 150.0,
                    peakSharePriceDuringHoldingPeriodUsd = 190.0,
                    peakSharePriceDateIso = "2025-08-14",
                    dec31ClosingSharePriceUsd = 175.0,
                    grossDividendsReceivedUsd = 0.0,
                    grossSaleProceedsUsd = 0.0
                ),
                calendarYear = 2025
            )
            assertEquals(13_02_000L, row.initialValueInr)
        }

        @Test
        fun `GTV-16 Rule 128 Foreign Dividend TTBR rollback and Form 67 FTC cap`() {
            val (grossDividendInr, allowableFtcInr) = CapitalGainsAndForeignAssetEngine.computeForm67ForeignTaxCredit(
                foreignDividendUsd = 1_000.0,
                foreignTaxWithheldUsd = 250.0,
                precedingMonthEndSbiTtbr = 84.10,
                indianEffectiveTaxRateOnTotalIncome = 0.2250
            )
            assertEquals(84_100L, grossDividendInr)
            assertEquals(18_923L, allowableFtcInr)
        }

        @Test
        fun `GTV-17 Updated Return ITR-U Sec 139 8A 25 pct additional tax within 12 months`() {
            val itrUResult = RevisionAndHarvestingEngine.computeUpdatedReturnItrU(
                baseAdditionalTax = 1_00_000L,
                totalInterest234A_B_C = 20_000L,
                filingWindow = ItrUWindow.WITHIN_12_MONTHS_FROM_AY_END
            )
            assertEquals(30_000L, itrUResult.additionalTaxUs140B)
            assertEquals(1_50_000L, itrUResult.totalUpdatedReturnPayable)
        }

        @Test
        fun `GTV-18 March LTCG Tax-Free Harvesting Radar detects 60000 headroom and saves 7500`() {
            val (recommendedHarvest, taxSaved) = RevisionAndHarvestingEngine.computeMarchLtcgHarvestingHeadroom(
                realizedLtcgSoFarRupees = 65_000L,
                unrealizedLtcgAvailableRupees = 80_000L
            )
            assertEquals(60_000L, recommendedHarvest)
            assertEquals(7_500L, taxSaved)
        }
    }
}
