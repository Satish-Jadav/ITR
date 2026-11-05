package `in`.zerotax.feature.pdfunlocker

import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
        // VUL-17 FIX: Pre-validate PDF before attempting up to 25 decryption passes.
        // A corrupt or non-PDF file causes each PDDocument.load() to throw after heavy work,
        // resulting in ANR when all 25 candidates are tried sequentially.
        //
        // Guard 1: File size cap (50 MB). Reject oversized files immediately.
        val MAX_PDF_BYTES = 50 * 1024 * 1024
        if (pdfBytes.size > MAX_PDF_BYTES) {
            return@withContext Result.failure(
                SecurityException("PDF exceeds 50 MB size limit. Please ensure this is a valid Indian tax document.")
            )
        }
        // Guard 2: Check PDF magic header (%PDF). Corrupt or non-PDF bytes fail fast.
        val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
        if (pdfBytes.size < 4 || !pdfBytes.take(4).toByteArray().contentEquals(PDF_MAGIC)) {
            return@withContext Result.failure(
                IllegalArgumentException("File does not appear to be a valid PDF document.")
            )
        }

        for (pwd in generate15StandardCandidates(pan, dobDdMmYyyy, firstName, bankCustomerId, bankAccountLast4)) {
            // Guard 3: 5-second timeout per password attempt prevents hanging on corrupt PDFs.
            val result = withTimeoutOrNull(5_000L) {
                runCatching {
                    PDDocument.load(ByteArrayInputStream(pdfBytes), pwd).use { doc ->
                        doc.setAllSecurityToBeRemoved(true)
                        val out = ByteArrayOutputStream()
                        doc.save(out)
                        out.toByteArray() to pwd
                    }
                }.getOrNull()
            }
            if (result != null) {
                return@withContext Result.success(result)
            }
        }
        Result.failure(SecurityException("Custom PDF password required outside standard permutations"))
    }
}
