package `in`.zerotax.feature.pdfunlocker

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
    fun generate15StandardCandidates(
        pan: String,
        dobDdMmYyyy: String,
        firstName: String,
        bankCustomerId: String? = null,
        bankAccountLast4: String? = null
    ): List<String> {
        val pLow = pan.lowercase().trim()
        val pUp = pan.uppercase().trim()
        val dob = dobDdMmYyyy.replace("-", "").replace("/", "").trim()
        val ddmm = dob.take(4)
        val yyyy = dob.takeLast(4)
        val ddmmyy = ddmm + dob.takeLast(2)
        val f4Low = firstName.trim().take(4).lowercase()
        val f4Up = f4Low.uppercase()

        val candidates = mutableListOf(
            pLow + dob, pUp + dob, dob, pLow, pUp,
            f4Low + dob, f4Up + dob, f4Low + ddmm, f4Up + ddmm,
            pLow + yyyy, pUp + yyyy, pLow + ddmmyy, pUp + ddmmyy,
            dob + pLow, dob + pUp
        )

        // Bank customer ID / last 4 account digit permutations
        if (!bankCustomerId.isNullOrBlank()) {
            val cust = bankCustomerId.trim()
            candidates.add(cust)
            candidates.add(cust.lowercase())
            candidates.add(cust.uppercase())
            candidates.add(cust + dob)
            candidates.add(cust + ddmm)
        }
        if (!bankAccountLast4.isNullOrBlank()) {
            val last4 = bankAccountLast4.trim()
            candidates.add(last4)
            candidates.add(f4Up + last4)
            candidates.add(f4Low + last4)
            candidates.add(pUp + last4)
            candidates.add(ddmm + last4)
        }

        return candidates
    }

    suspend fun attemptUnlock(
        pdfBytes: ByteArray,
        pan: String,
        dobDdMmYyyy: String,
        firstName: String,
        bankCustomerId: String? = null,
        bankAccountLast4: String? = null
    ): Result<Pair<ByteArray, String>> = withContext(Dispatchers.Default) {
        for (pwd in generate15StandardCandidates(pan, dobDdMmYyyy, firstName, bankCustomerId, bankAccountLast4)) {
            runCatching {
                PDDocument.load(ByteArrayInputStream(pdfBytes), pwd).use { doc ->
                    doc.setAllSecurityToBeRemoved(true)
                    val out = ByteArrayOutputStream()
                    doc.save(out)
                    return@withContext Result.success(out.toByteArray() to pwd)
                }
            }
        }
        Result.failure(SecurityException("Custom PDF password required outside standard permutations"))
    }
}
