package in.zerotax.feature.pdfunlocker

import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SilentPdfUnlocker {
    /**
     * Generates the 15 standard permutations for Indian Form 16 / AIS / Bank Statement password unlocking:
     * PAN (lower/upper) + DOB (DDMMYYYY), DOB, First 4 of name + DOB, PAN + YYYY, PAN + DDMMYY, etc.
     */
    fun generate15StandardCandidates(pan: String, dobDdMmYyyy: String, firstName: String): List<String> {
        val pLow = pan.lowercase().trim()
        val pUp = pan.uppercase().trim()
        val dob = dobDdMmYyyy.replace("-", "").replace("/", "").trim()
        val ddmm = dob.take(4)
        val yyyy = dob.takeLast(4)
        val ddmmyy = ddmm + dob.takeLast(2)
        val f4Low = firstName.trim().take(4).lowercase()
        val f4Up = f4Low.uppercase()

        return listOf(
            pLow + dob, pUp + dob, dob, pLow, pUp,
            f4Low + dob, f4Up + dob, f4Low + ddmm, f4Up + ddmm,
            pLow + yyyy, pUp + yyyy, pLow + ddmmyy, pUp + ddmmyy,
            dob + pLow, dob + pUp
        )
    }

    suspend fun attemptUnlock(
        pdfBytes: ByteArray,
        pan: String,
        dobDdMmYyyy: String,
        firstName: String
    ): Result<Pair<ByteArray, String>> = withContext(Dispatchers.Default) {
        for (pwd in generate15StandardCandidates(pan, dobDdMmYyyy, firstName)) {
            runCatching {
                PDDocument.load(ByteArrayInputStream(pdfBytes), pwd).use { doc ->
                    doc.setAllSecurityToBeRemoved(true)
                    val out = ByteArrayOutputStream()
                    doc.save(out)
                    return@withContext Result.success(out.toByteArray() to pwd)
                }
            }
        }
        // TODO(Phase-2): Add bank-specific customer-ID / last-4-account-digit permutation hook if AA metadata is available.
        Result.failure(SecurityException("Custom PDF password required outside standard permutations"))
    }
}
