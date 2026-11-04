package `in`.zerotax.feature.portalbridge

import org.junit.Assert.*
import org.junit.Test

class UpiChallan280IntentLauncherTest {

    @Test
    fun testBuildUpiPaymentUri_GeneratesValidParameters() {
        val uri = UpiChallan280IntentLauncher.buildUpiPaymentUri(
            pan = "ABCDE1234F",
            amountRupees = 25000L,
            assessmentYear = "2026-27",
            taxType = ChallanTaxType.SELF_ASSESSMENT_TAX
        )

        assertEquals("upi", uri.scheme)
        assertEquals("pay", uri.authority)
        assertEquals("25000", uri.getQueryParameter("am"))
        assertEquals("INR", uri.getQueryParameter("cu"))
        assertTrue(uri.getQueryParameter("tn")!!.contains("ABCDE1234F"))
        assertTrue(uri.getQueryParameter("tn")!!.contains("M300"))
    }

    @Test
    fun testValidateChallanReceipt_ValidatesBsrAndCinRules() {
        val valid = UpiChallan280IntentLauncher.validateChallanReceipt(
            bsrCode = "0210001",
            challanSerialNo = "01234",
            depositDateIso = "2025-06-15",
            pan = "ABCDE1234F",
            amountRupees = 50000L
        )
        assertTrue(valid.isValid)
        assertTrue(valid.errors.isEmpty())

        val invalid = UpiChallan280IntentLauncher.validateChallanReceipt(
            bsrCode = "123", // invalid (needs 7 digits)
            challanSerialNo = "12", // invalid (needs 5 digits)
            depositDateIso = "2099-01-01", // future date
            pan = "INVALID",
            amountRupees = 0L
        )
        assertFalse(invalid.isValid)
        assertTrue(invalid.errors.size >= 4)
    }
}

