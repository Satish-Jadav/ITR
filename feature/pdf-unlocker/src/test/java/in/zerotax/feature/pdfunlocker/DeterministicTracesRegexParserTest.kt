package `in`.zerotax.feature.pdfunlocker

import `in`.zerotax.core.model.SourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicTracesRegexParserTest {

    @Test
    fun testParseStandardTracesForm16_ExtractsAllStatutorySalaryFields() {
        val sampleTracesText = """
            FORM NO. 16
            Certificate under section 203 of the Income-tax Act, 1961
            TAN of the Deductor: BLRM01234D
            PAN of the Employee: ABCDE1234F
            
            PART B (Annexure)
            1. Gross Salary
            (a) Salary as per provisions contained in section 17(1): 12,50,000.00
            (b) Value of perquisites under section 17(2): 50,000.00
            
            Total amount of exemption claimed under section 10: 60,000.00
            
            Standard deduction under section 16(ia): 75,000.00
            Tax on employment under section 16(iii): 2,400.00
            
            Income chargeable under the head "Salaries": 11,62,600.00
            
            PART A (Summary of Tax Deducted)
            Total Tax Deposited: 85,000.00
        """.trimIndent()

        val result = DeterministicTracesRegexParser.parseStandardTracesForm16(sampleTracesText)
        assertTrue(result.isSuccess)

        val salarySource = result.getOrThrow()
        assertEquals("BLRM01234D", salarySource.employerTan)
        assertEquals(SourceKind.FORM_16, salarySource.source)
        // Gross = 12,50,000 + 50,000 = 13,00,000
        assertEquals(1300000L, salarySource.grossSalary)
        assertEquals(60000L, salarySource.sec10Exemptions)
        // Sec 16 deductions = 75,000 + 2,400 = 77,400
        assertEquals(77400L, salarySource.sec16DeductionsClaimedInDoc)
        assertEquals(1162600L, salarySource.netTaxableSalaryInDoc)
        assertEquals(85000L, salarySource.tdsReported)
    }

    @Test
    fun testParseStandardTracesForm16_FailsGracefullyWhenTanMissing() {
        val invalidText = "Random non-Form 16 document with no TAN details."
        val result = DeterministicTracesRegexParser.parseStandardTracesForm16(invalidText)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("TAN missing") == true)
    }
}
