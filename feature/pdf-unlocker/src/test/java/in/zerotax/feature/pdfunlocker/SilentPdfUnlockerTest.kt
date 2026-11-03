package in.zerotax.feature.pdfunlocker

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
}
