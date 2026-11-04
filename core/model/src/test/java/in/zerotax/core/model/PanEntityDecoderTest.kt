package `in`.zerotax.core.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PanEntityDecoderTest {

    @Test
    fun decode_validIndividualPan_returnsCorrectMetadata() {
        val decoded = PanEntityDecoder.decode("ABCPS1234F")
        assertTrue(decoded.isValidFormat)
        assertEquals(PanEntityType.INDIVIDUAL, decoded.entityType)
        assertEquals('S', decoded.surnameInitial)
        assertTrue(decoded.isEligibleFor87ARebate)
        assertTrue(decoded.entityType.isSupportedInPersonalSuite)
        assertTrue(decoded.entityType.eligibleItrForms.contains(ItrFormType.ITR_1_SAHAJ))
    }

    @Test
    fun decode_validHufPan_returnsCorrectMetadata() {
        val decoded = PanEntityDecoder.decode("ABCHS1234F")
        assertTrue(decoded.isValidFormat)
        assertEquals(PanEntityType.HUF, decoded.entityType)
        assertEquals('H', decoded.entityType.code)
        assertFalse(decoded.isEligibleFor87ARebate) // HUFs don't get 87A rebate
        assertTrue(decoded.entityType.isSupportedInPersonalSuite)
        assertTrue(decoded.entityType.eligibleItrForms.contains(ItrFormType.ITR_2))
    }

    @Test
    fun decode_companyPan_flagsUnsupportedInPersonalSuite() {
        val decoded = PanEntityDecoder.decode("ABCCE1234D")
        assertTrue(decoded.isValidFormat)
        assertEquals(PanEntityType.COMPANY, decoded.entityType)
        assertFalse(decoded.isEligibleFor87ARebate)
        assertFalse(decoded.entityType.isSupportedInPersonalSuite)
        assertTrue(decoded.statusSummary.contains("Company"))
    }

    @Test
    fun decode_partnershipFirmPan_flagsUnsupported() {
        val decoded = PanEntityDecoder.decode("ABCFE1234G")
        assertTrue(decoded.isValidFormat)
        assertEquals(PanEntityType.FIRM, decoded.entityType)
        assertFalse(decoded.isEligibleFor87ARebate)
        assertFalse(decoded.entityType.isSupportedInPersonalSuite)
    }

    @Test
    fun decode_incompletePan_returnsIncompleteStatus() {
        val decoded = PanEntityDecoder.decode("ABCD")
        assertFalse(decoded.isValidFormat)
        assertEquals(PanEntityType.INDIVIDUAL, PanEntityType.fromChar('P'))
        assertTrue(decoded.statusSummary.contains("Entering PAN (4/10)"))
    }

    @Test
    fun decode_malformedPan_returnsInvalidSyntax() {
        val decoded = PanEntityDecoder.decode("12345ABCDE")
        assertFalse(decoded.isValidFormat)
        assertTrue(decoded.statusSummary.contains("Invalid PAN syntax"))
    }

    @Test
    fun institutionRegistry_findsForm16AndAisRules() {
        val rule = InstitutionPdfPasswordRegistry.findRuleByInstitution("TRACES")
        assertNotNull(rule)
        assertTrue(rule!!.passwordPatternRule.contains("PAN in LOWERCASE"))

        val aisRule = InstitutionPdfPasswordRegistry.findRuleByInstitution("AIS")
        assertNotNull(aisRule)
        assertEquals("ITD_AIS_TIS", aisRule!!.institutionId)
    }
}
