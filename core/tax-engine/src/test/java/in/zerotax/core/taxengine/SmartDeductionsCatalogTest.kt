package `in`.zerotax.core.taxengine

import `in`.zerotax.core.model.DeductionRegimeScope
import `in`.zerotax.core.model.LifeEventTag
import `in`.zerotax.core.taxengine.advanced.SmartDeductionsCatalog
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SmartDeductionsCatalogTest {

    @Test
    fun testAllDeductionsCountAndIntegrity() {
        val allSpecs = SmartDeductionsCatalog.ALL_DEDUCTION_SPECS
        assertTrue(allSpecs.size >= 16, "Must contain all major Chapter VI-A statutory deduction entries")

        // Ensure every entry has statutory details populated
        allSpecs.forEach { spec ->
            assertTrue(spec.sectionCode.isNotBlank(), "Section code cannot be blank")
            assertTrue(spec.statutoryRef.contains("Income-tax Act"), "Statutory ref must reference Income-tax Act: ${spec.statutoryRef}")
            assertTrue(spec.deductionTitle.isNotBlank())
            assertTrue(spec.maxLimitRupees > 0L, "Max limit must be greater than zero for ${spec.sectionCode}")
            assertTrue(spec.requiredDocumentName.isNotBlank())
            assertTrue(spec.whereToFetchInstructions.isNotBlank())
            assertTrue(spec.howItAppliesExplanation.isNotBlank())
        }
    }

    @Test
    fun testSpecificStatutoryDeductionsLookups() {
        // 80U - Self Disability
        val sec80U = SmartDeductionsCatalog.getBySection("80U")
        assertNotNull(sec80U)
        assertEquals(LifeEventTag.SELF_DISABILITY, sec80U?.lifeEventTag)
        assertEquals(1_25_000L, sec80U?.maxLimitRupees)
        assertEquals(DeductionRegimeScope.OLD_REGIME_ONLY, sec80U?.regimeScope)
        assertTrue(sec80U!!.requiredDocumentName.contains("Form 10-IA"))

        // 80DDB - Chronic Illness
        val sec80DDB = SmartDeductionsCatalog.getBySection("80DDB")
        assertNotNull(sec80DDB)
        assertEquals(LifeEventTag.CRITICAL_ILLNESS, sec80DDB?.lifeEventTag)
        assertEquals(1_00_000L, sec80DDB?.maxLimitRupees)
        assertTrue(sec80DDB!!.howItAppliesExplanation.contains("Senior Citizens"))

        // 80TTB - Senior Citizen Bank / FD Interest
        val sec80TTB = SmartDeductionsCatalog.getBySection("80TTB")
        assertNotNull(sec80TTB)
        assertEquals(LifeEventTag.SENIOR_DEPOSITS, sec80TTB?.lifeEventTag)
        assertEquals(50_000L, sec80TTB?.maxLimitRupees)
        assertTrue(sec80TTB!!.howItAppliesExplanation.contains("Senior Citizens (60+ yrs)"))

        // 80GGC - Political Contributions
        val sec80GGC = SmartDeductionsCatalog.getBySection("80GGC")
        assertNotNull(sec80GGC)
        assertEquals(LifeEventTag.POLITICAL_DONATION, sec80GGC?.lifeEventTag)
        assertTrue(sec80GGC!!.howItAppliesExplanation.contains("Cash donations are completely prohibited"))

        // 80GGA - Scientific Research
        val sec80GGA = SmartDeductionsCatalog.getBySection("80GGA")
        assertNotNull(sec80GGA)
        assertEquals(LifeEventTag.POLITICAL_DONATION, sec80GGA?.lifeEventTag)

        // 80CCC - Annuity / Pension fund
        val sec80CCC = SmartDeductionsCatalog.getBySection("80CCC")
        assertNotNull(sec80CCC)
        assertEquals(LifeEventTag.NPS_INVESTMENT, sec80CCC?.lifeEventTag)
        assertEquals(1_50_000L, sec80CCC?.maxLimitRupees)
    }

    @Test
    fun testRegimeFiltering() {
        val oldRegimeSpecs = SmartDeductionsCatalog.getDeductionsForRegime(DeductionRegimeScope.OLD_REGIME_ONLY)
        assertTrue(oldRegimeSpecs.isNotEmpty())
        assertTrue(oldRegimeSpecs.any { it.sectionCode.startsWith("80C") })
        assertTrue(oldRegimeSpecs.any { it.sectionCode.startsWith("80D") })
        assertTrue(oldRegimeSpecs.any { it.sectionCode.startsWith("80U") })

        val bothRegimeSpecs = SmartDeductionsCatalog.getDeductionsForRegime(DeductionRegimeScope.BOTH_REGIMES)
        assertTrue(bothRegimeSpecs.any { it.sectionCode.contains("80CCD(2)") }, "80CCD(2) employer NPS must apply to both regimes")
    }

    @Test
    fun testTagFiltering() {
        val healthSpecs = SmartDeductionsCatalog.getDeductionsForTags(setOf(LifeEventTag.HEALTH_INSURANCE))
        assertTrue(healthSpecs.any { it.sectionCode.contains("80D") })

        val homeLoanSpecs = SmartDeductionsCatalog.getDeductionsForTags(setOf(LifeEventTag.HOME_LOAN))
        assertTrue(homeLoanSpecs.any { it.sectionCode.contains("24(b)") })
        assertTrue(homeLoanSpecs.any { it.sectionCode.contains("80EEA") })

        val multiTags = SmartDeductionsCatalog.getDeductionsForTags(setOf(LifeEventTag.SELF_DISABILITY, LifeEventTag.SENIOR_DEPOSITS))
        assertEquals(2, multiTags.size)
        assertTrue(multiTags.any { it.sectionCode == "80U" })
        assertTrue(multiTags.any { it.sectionCode == "80TTB" })
    }
}

