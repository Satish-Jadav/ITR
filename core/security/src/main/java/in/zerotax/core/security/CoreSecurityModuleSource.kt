package in.zerotax.core.security

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Lightweight UIDAI Section 29 interceptor that scrubs 12-digit Aadhaar numbers
 * using the Verhoeff checksum before text enters SQLite, WebView, or AICore.
 */
object UidaiAadhaarVerhoeffScrubber {
    private val CANDIDATE_REGEX = Regex("""\b([2-9]\d{3})[\s\-]?(\d{4})[\s\-]?(\d{4})\b""")

    private val dTable = arrayOf(
        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), intArrayOf(1, 2, 3, 4, 0, 6, 7, 8, 9, 5),
        intArrayOf(2, 3, 4, 0, 1, 7, 8, 9, 5, 6), intArrayOf(3, 4, 0, 1, 2, 8, 9, 5, 6, 7),
        intArrayOf(4, 0, 1, 2, 3, 9, 5, 6, 7, 8), intArrayOf(5, 9, 8, 7, 6, 0, 4, 3, 2, 1),
        intArrayOf(6, 5, 9, 8, 7, 1, 0, 4, 3, 2), intArrayOf(7, 6, 5, 9, 8, 2, 1, 0, 4, 3),
        intArrayOf(8, 7, 6, 5, 9, 3, 2, 1, 0, 4), intArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0)
    )

    private val pTable = arrayOf(
        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), intArrayOf(1, 5, 7, 6, 2, 8, 3, 0, 9, 4),
        intArrayOf(5, 8, 0, 3, 7, 9, 6, 1, 4, 2), intArrayOf(8, 9, 1, 6, 0, 4, 3, 5, 2, 7),
        intArrayOf(9, 4, 5, 3, 1, 2, 6, 8, 7, 0), intArrayOf(4, 2, 8, 6, 5, 7, 3, 9, 0, 1),
        intArrayOf(2, 7, 9, 3, 8, 0, 6, 4, 1, 5), intArrayOf(7, 0, 4, 6, 9, 1, 3, 2, 5, 8)
    )

    fun scrubAllAadhaarDigits(input: String, strictChecksumOnly: Boolean = false): String =
        CANDIDATE_REGEX.replace(input) { match ->
            val raw = match.groupValues[1] + match.groupValues[2] + match.groupValues[3]
            if (passesVerhoeffChecksum(raw) || !strictChecksumOnly) "[Aadhaar Redacted]" else match.value
        }

    fun passesVerhoeffChecksum(digits: String): Boolean {
        if (digits.length != 12 || !digits.all { it.isDigit() }) return false
        var c = 0
        digits.reversed().forEachIndexed { i, ch -> c = dTable[c][pTable[i % 8][ch.digitToInt()]] }
        return c == 0
    }

    fun maskAadhaar(aadhaar: String): String {
        val cleaned = aadhaar.filter { it.isDigit() }
        return if (cleaned.length >= 12) "XXXX-XXXX-${cleaned.takeLast(4)}" else "XXXX-XXXX-XXXX"
    }

    // TODO(Phase-2): Add Virtual ID (16-digit VID) pattern redaction for UIDAI e-KYC XML/PDF imports.
}

object DpdpVaultShredder {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "zero_tax_sqlcipher_master_key_v1"
    private const val DB_NAME = "zero_tax_vault.db"

    suspend fun shredAllLocalTaxData(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (ks.containsAlias(MASTER_KEY_ALIAS)) ks.deleteEntry(MASTER_KEY_ALIAS)

            val db = context.getDatabasePath(DB_NAME)
            listOf(db, File("${db.path}-wal"), File("${db.path}-shm"), File("${db.path}-journal"))
                .forEach(::secureOverwriteAndDelete)

            context.filesDir.walkBottomUp().filter { it.isFile }.forEach(::secureOverwriteAndDelete)
            context.cacheDir.walkBottomUp().filter { it.isFile }.forEach(::secureOverwriteAndDelete)
            // TODO(Phase-2): Trigger RBI Account Aggregator FIP consent revocation webhook before local wipe if online.
        }
    }

    fun performCryptographicErasure(dbPath: File) {
        secureOverwriteAndDelete(dbPath)
    }

    private fun secureOverwriteAndDelete(file: File) {
        if (!file.exists()) return
        if (file.length() > 0) {
            val rng = SecureRandom()
            val buf = ByteArray(4096)
            RandomAccessFile(file, "rws").use { raf ->
                var rem = file.length()
                while (rem > 0) {
                    rng.nextBytes(buf)
                    val n = minOf(rem, buf.size.toLong()).toInt()
                    raf.write(buf, 0, n)
                    rem -= n
                }
                raf.fd.sync()
            }
        }
        file.delete()
    }
}

object CanonicalWarningHasher {
    fun computeSha256(rawWarningText: String): String {
        val normalized = rawWarningText.trim().replace("\r\n", "\n")
        return MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun hashWarningPayload(payload: String): String = computeSha256(payload)

    // TODO(Phase-3): Sign audit log entries with a hardware-backed ECDSA key for tamper-evident export.
}
