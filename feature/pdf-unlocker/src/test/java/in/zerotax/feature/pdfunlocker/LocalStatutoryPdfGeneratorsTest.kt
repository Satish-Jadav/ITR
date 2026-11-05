package `in`.zerotax.feature.pdfunlocker

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalStatutoryPdfGeneratorsTest {

    @Test
    fun testGenerateForm12BbPdf_GeneratesValidPdfBytes() = runBlocking {
        val form12BbData = Form12BbData(
            employeeName = "Rohan Sharma",
            employeePan = "ABCDE1234F",
            employerName = "Tata Consultancy Services Ltd",
            rentPaidAnnual = 3_60_000L,
            landlordName = "Suresh Patel",
            landlordPan = "XYZPA9999K",
            section80C = 1_50_000L,
            section80CCD1B = 50_000L,
            section80D = 25_000L
        )

        val pdfBytes = LocalStatutoryPdfGenerators.generateForm12BbPdf(form12BbData)
        assertNotNull(pdfBytes)
        assertTrue(pdfBytes.isNotEmpty())
        // PDF header magic bytes "%PDF-"
        val header = String(pdfBytes.copyOfRange(0, 5))
        assertEquals("%PDF-", header)
    }

    @Test
    fun testGenerateForm15HPdf_GeneratesValidSeniorCitizenPdf() = runBlocking {
        val form15HData = Form15HData(
            declarantName = "Ramakant Joshi",
            pan = "ABCDE5678G",
            dateOfBirthIso = "1954-06-15",
            bankName = "State Bank of India",
            bankBranch = "Indiranagar, Bengaluru",
            fixedDepositAccountNumber = "39485729102",
            estimatedFdInterestRupees = 48_000L,
            estimatedTotalIncomeRupees = 2_80_000L,
            isSeniorCitizen60Plus = true
        )

        val pdfBytes = LocalStatutoryPdfGenerators.generateForm15GOr15HPdf(form15HData)
        assertNotNull(pdfBytes)
        assertTrue(pdfBytes.isNotEmpty())
        val header = String(pdfBytes.copyOfRange(0, 5))
        assertEquals("%PDF-", header)
    }

    private fun assertEquals(expected: String, actual: String) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}

