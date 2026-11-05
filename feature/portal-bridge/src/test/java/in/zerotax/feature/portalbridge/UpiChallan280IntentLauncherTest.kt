package `in`.zerotax.feature.portalbridge

import org.junit.Assert.*
import org.junit.Test

class UpiChallan280IntentLauncherTest {

    @Test
    fun testBuildUpiPaymentUri_GeneratesValidParameters() {
        val uriString = UpiChallan280IntentLauncher.buildUpiPaymentUriString(
            pan = "ABCDE1234F",
            amountRupees = 25000L,
            assessmentYear = "2026-27",
            taxType = ChallanTaxType.SELF_ASSESSMENT_TAX
        )

        assertTrue(uriString.startsWith("upi://pay"))
        assertTrue(uriString.contains("am=25000"))
        assertTrue(uriString.contains("cu=INR"))
        // VUL-10 FIX: PAN must NOT be leaked in the UPI payment URI
        assertFalse("PAN must not be leaked in UPI intent URI", uriString.contains("ABCDE1234F"))
        assertTrue(uriString.contains("M300"))
        assertTrue(uriString.contains("incometax@sbi"))
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

