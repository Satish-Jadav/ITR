package `in`.zerotax.core.taxengine

import `in`.zerotax.core.model.ResidentialStatus
import `in`.zerotax.core.taxengine.subengines.FamilyTaxArbitrageInput
import `in`.zerotax.core.taxengine.subengines.ResidencyDayCountInput
import `in`.zerotax.core.taxengine.subengines.ResidentialStatusAndFamilyArbitrageEngine
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResidentialStatusAndFamilyArbitrageEngineTest {

    @Test
    fun testRegularResidentOrdinarilyResident() {
        val input = ResidencyDayCountInput(
            daysInIndiaCurrentFinancialYear = 250,
            daysInIndiaPreceding4Years = 1460,
            daysInIndiaPreceding7Years = 2500,
            nonResidentYearsInPreceding10Years = 0
        )
        val decision = ResidentialStatusAndFamilyArbitrageEngine.determineResidentialStatusSec6(input)

        assertEquals(ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT, decision.status)
        assertTrue(decision.isScheduleFaMandatory)
        assertFalse(decision.areForeignDividendsAndGainsExemptInIndia)
    }

    @Test
    fun testReturningNriQualifiesForRnorAndScheduleFaImmunity() {
        // Returning NRI who spent 190 days in India in CY, but was NR for 9 of the last 10 years
        val input = ResidencyDayCountInput(
            daysInIndiaCurrentFinancialYear = 190,
            daysInIndiaPreceding4Years = 120,
            daysInIndiaPreceding7Years = 300,
            nonResidentYearsInPreceding10Years = 9
        )
        val decision = ResidentialStatusAndFamilyArbitrageEngine.determineResidentialStatusSec6(input)

        assertEquals(ResidentialStatus.RESIDENT_NOT_ORDINARILY_RESIDENT, decision.status)
        // Critical legal protection: RNOR is exempt from Schedule FA foreign asset disclosure!
        assertFalse(decision.isScheduleFaMandatory)
        assertTrue(decision.areForeignDividendsAndGainsExemptInIndia)
        assertTrue(decision.detailedExplanation.contains("RNOR") || decision.statutorySection.contains("RNOR"))
    }

    @Test
    fun testNonResidentStatus() {
        val input = ResidencyDayCountInput(
            daysInIndiaCurrentFinancialYear = 45,
            daysInIndiaPreceding4Years = 100,
            daysInIndiaPreceding7Years = 200,
            nonResidentYearsInPreceding10Years = 10
        )
        val decision = ResidentialStatusAndFamilyArbitrageEngine.determineResidentialStatusSec6(input)

        assertEquals(ResidentialStatus.NON_RESIDENT, decision.status)
        assertFalse(decision.isScheduleFaMandatory)
        assertTrue(decision.areForeignDividendsAndGainsExemptInIndia)
    }

    @Test
    fun testFamilyTaxArbitrageSimulator() {
        val arbitrageInput = FamilyTaxArbitrageInput(
            taxpayerSlabRatePercent = 0.30,
            annualRentPaidToParentsRupees = 3_60_000L, // ₹30k/month
            parentOwnsHouseProperty = true,
            parentsAnnualTaxableIncomeRupees = 4_00_000L,
            parentAge = 66,
            parentHealthInsurancePaidRupees = 50_000L,
            fdPrincipalGiftedToAdultChildOrParentsRupees = 10_00_000L, // ₹10L @ 7.5% = ₹75k
            expectedFdInterestRatePercent = 0.075
        )

        val report = ResidentialStatusAndFamilyArbitrageEngine.simulateFamilyTaxArbitrage(arbitrageInput)

        assertTrue(report.totalFamilyTaxSavedAnnualRupees > 0L)
        assertEquals(3, report.opportunities.size)

        // Rent to parents check
        val rentOpp = report.opportunities.first { it.title.contains("Rent") }
        assertTrue(rentOpp.annualTaxSavedRupees > 0L)
        assertTrue(rentOpp.legalComplianceRules.any { it.contains("Section 10(13A)") || it.contains("parent") })

        // Health insurance check
        val healthOpp = report.opportunities.first { it.title.contains("Health Insurance") }
        // 50k * 30% = 15k
        assertEquals(15_000L, healthOpp.annualTaxSavedRupees)

        // FD Gift check
        val fdOpp = report.opportunities.first { it.title.contains("Gift") }
        assertTrue(fdOpp.annualTaxSavedRupees > 0L)
        assertTrue(fdOpp.legalComplianceRules.any { it.contains("Section 64") })
    }
}

