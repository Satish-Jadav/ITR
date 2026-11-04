package `in`.zerotax.feature.portalbridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Challan280ReceiptParserTest {

    @Test
    fun testParseChallan280PdfText_ExtractsAllMandatoryChallanAttributes() {
        val sampleChallanText = """
            INCOME TAX DEPARTMENT - TAXPAYER'S COUNTERFOIL
            CHALLAN NO. / ITNS 280
            Major Head: 0021 (Income Tax Other than Companies)
            Minor Head: 300 (Self Assessment Tax)
            
            BSR Code: 0002145
            Date of Deposit: 15/07/2026
            Challan Serial Number: 01429
            Total Amount: Rs. 24,500
        """.trimIndent()

        val result = Challan280ReceiptParser.parseChallan280PdfText(sampleChallanText)
        assertTrue(result.isSuccess)

        val entry = result.getOrThrow()
        assertEquals("0002145", entry.bsrCode)
        assertEquals("2026-07-15", entry.dateOfDepositIso)
        assertEquals("01429", entry.challanSerialNumber)
        assertEquals(24500L, entry.taxPaidRupees)
    }

    @Test
    fun testParseChallan280PdfText_FailsGracefullyWhenBsrMissing() {
        val invalidText = "Corrupted Challan without BSR code and serial."
        val result = Challan280ReceiptParser.parseChallan280PdfText(invalidText)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("BSR Code not found") == true)
    }
}
