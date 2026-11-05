package `in`.zerotax.feature.pdfunlocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SilentPdfUnlockerTest {

    @Test
    fun testGenerate15StandardCandidates_GeneratesAllKeyPermutations() {
        val unlocker = SilentPdfUnlocker()
        val candidates = unlocker.generate15StandardCandidates(
            pan = "ABCDE1234F",
            dobDdMmYyyy = "15-08-1990",
            firstName = "Rajesh"
        )

        assertEquals(15, candidates.size)
        // Check pan lowercase + dob
        assertTrue(candidates.contains("abcde1234f15081990"))
        // Check pan uppercase + dob
        assertTrue(candidates.contains("ABCDE1234F15081990"))
        // Check dob alone
        assertTrue(candidates.contains("15081990"))
        // Check name first 4 + ddmm
        assertTrue(candidates.contains("raje1508"))
        assertTrue(candidates.contains("RAJE1508"))
        // Check pan + yyyy
        assertTrue(candidates.contains("abcde1234f1990"))
        assertTrue(candidates.contains("ABCDE1234F1990"))
    }

    @Test
    fun testAttemptUnlock_NonPdfBytes_FailsFast() = kotlinx.coroutines.runBlocking {
        // VUL-17 FIX: Non-PDF bytes fail fast on magic header check without attempting 15-25 decryptions
        val unlocker = SilentPdfUnlocker()
        val nonPdfBytes = "NOT_A_PDF_DOCUMENT_CONTENT".toByteArray()
        val result = unlocker.attemptUnlock(
            pdfBytes = nonPdfBytes,
            pan = "ABCDE1234F",
            dobDdMmYyyy = "15-08-1990",
            firstName = "Rajesh"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testAttemptUnlock_OversizedBytes_FailsFast() = kotlinx.coroutines.runBlocking {
        // VUL-17 FIX: >50MB files rejected immediately to protect against ANR/OOM attacks
        val unlocker = SilentPdfUnlocker()
        val oversizedHeader = ByteArray(51 * 1024 * 1024)
        val result = unlocker.attemptUnlock(
            pdfBytes = oversizedHeader,
            pan = "ABCDE1234F",
            dobDdMmYyyy = "15-08-1990",
            firstName = "Rajesh"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }
}
