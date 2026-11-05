package `in`.zerotax.core.taxengine.senior

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SeniorCitizenTaxAndExemptionEngineTest {

    @Test
    fun evaluateSeniorCitizenBenefits_age75PlusPensionAndSameBank_eligibleForSection194P() {
        val result = SeniorCitizenTaxAndExemptionEngine.evaluateSeniorCitizenBenefits(
            ageYears = 77,
            isResident = true,
            seniorCitizenName = "Rameshchandra Patel",
            pan = "ABCDE1234F",
            assessmentYear = "2026-27",
            dateOfBirth = "12/03/1949",
            specifiedBankName = "State Bank of India",
            specifiedBankBranch = "Navrangpura Branch",
            pensionAccountNumber = "SBI1234567890",
            pensionIncomeRupees = 4_80_000L,
            bankInterestFromPensionBankRupees = 1_20_000L,
            interestFromOtherBanksRupees = 0L,
            hasBusinessIncome = false,
            hasCapitalGainsOrRentalIncome = false
        )

        assertEquals(SeniorCitizenAgeCategory.SENIOR_CITIZEN_60_TO_79, result.ageCategory)
        assertTrue(result.isEligibleForSection194PExemption)
        assertTrue(result.section207AdvanceTaxImmune)
        assertEquals(50_000L, result.section80TtbDeductionAllowedRupees)
        assertNotNull(result.form12Bba)
        assertTrue(result.form12Bba!!.isEligibleFor194PExemption)
    }

    @Test
    fun evaluateSeniorCitizenBenefits_age82SuperSeniorWithMultipleBanks_ineligibleFor194P_butReceivesSuperSeniorBenefits() {
        val result = SeniorCitizenTaxAndExemptionEngine.evaluateSeniorCitizenBenefits(
            ageYears = 82,
            isResident = true,
            seniorCitizenName = "Savitaben Shah",
            pan = "BCDEF2345G",
            assessmentYear = "2026-27",
            dateOfBirth = "01/01/1944",
            specifiedBankName = "Bank of Baroda",
            specifiedBankBranch = "Alkapuri Branch",
            pensionAccountNumber = "BOB987654321",
            pensionIncomeRupees = 6_00_000L,
            bankInterestFromPensionBankRupees = 80_000L,
            interestFromOtherBanksRupees = 40_000L, // Has interest from other bank
            hasBusinessIncome = false,
            hasCapitalGainsOrRentalIncome = false
        )

        assertEquals(SeniorCitizenAgeCategory.SUPER_SENIOR_80_PLUS, result.ageCategory)
        assertFalse(result.isEligibleForSection194PExemption) // Ineligible because other bank exists
        assertTrue(result.section207AdvanceTaxImmune)
        assertEquals(50_000L, result.section80TtbDeductionAllowedRupees)
        assertEquals(50_000L, result.section80DMedicalLimitRupees)
        assertNotNull(result.form12Bba)
        assertFalse(result.form12Bba!!.isEligibleFor194PExemption)
    }

    @Test
    fun evaluateSeniorCitizenBenefits_age45RegularIndividual_noSeniorBenefits() {
        val result = SeniorCitizenTaxAndExemptionEngine.evaluateSeniorCitizenBenefits(
            ageYears = 45,
            isResident = true,
            seniorCitizenName = "Aarav Sharma",
            pan = "CDEFG3456H",
            assessmentYear = "2026-27",
            dateOfBirth = "10/10/1981",
            specifiedBankName = "HDFC Bank",
            specifiedBankBranch = "Whitefield",
            pensionAccountNumber = "HDFC112233",
            pensionIncomeRupees = 0L,
            bankInterestFromPensionBankRupees = 25_000L
        )

        assertEquals(SeniorCitizenAgeCategory.REGULAR_BELOW_60, result.ageCategory)
        assertFalse(result.isEligibleForSection194PExemption)
        assertFalse(result.section207AdvanceTaxImmune)
        assertEquals(0L, result.section80TtbDeductionAllowedRupees)
        assertNull(result.form12Bba)
    }
}
