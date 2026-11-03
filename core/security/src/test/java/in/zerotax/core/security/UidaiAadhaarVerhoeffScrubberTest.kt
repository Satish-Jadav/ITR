package in.zerotax.core.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UIDAI Aadhaar Verhoeff Scrubber & Security Tests")
class UidaiAadhaarVerhoeffScrubberTest {

    @Test
    fun `valid Aadhaar with Verhoeff checksum is correctly detected and scrubbed`() {
        // Known valid Aadhaar numbers passing Verhoeff algorithm
        val validAadhaar = "2345 6789 0120"
        val sampleText = "Employee Aadhaar is 2345 6789 0120 registered with UIDAI."
        val scrubbed = UidaiAadhaarVerhoeffScrubber.scrubAllAadhaarDigits(sampleText)

        assertFalse(scrubbed.contains("2345 6789 0120"))
        assertTrue(scrubbed.contains("[Aadhaar Redacted]"))
    }

    @Test
    fun `canonical warning hasher produces deterministic SHA-256 hex string`() {
        val warningText = "Statutory Penalty Warning: Section 270A misreporting penalty up to 200%."
        val hash1 = CanonicalWarningHasher.computeSha256(warningText)
        val hash2 = CanonicalWarningHasher.computeSha256(warningText)

        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }

    @Test
    fun `masking leaves only last 4 digits visible`() {
        val masked = UidaiAadhaarVerhoeffScrubber.maskAadhaar("234567890123")
        assertEquals("XXXX-XXXX-0123", masked)
    }
}
