package `in`.zerotax.core.security

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Section 33.3: Zero-Knowledge .ztvault Encrypted Device-to-Device Migration & CA Handover Bundle.
 *
 * Guaranteed 100% Zero-Cloud Privacy:
 * 1. Takes canonical database records and serializes to compressed JSON.
 * 2. Derives 256-bit AES key using PBKDF2 (HMAC-SHA512 or HMAC-SHA256) with 256,000 iterations
 *    from: user PAN + Date of Birth + user-chosen Transfer Passcode + 32-byte random salt.
 * 3. Encrypts payload with AES-256-GCM (12-byte IV, 128-bit authentication tag).
 * 4. Outputs binary .ztvault archive with "ZTVAULT1" magic header for Quick Share / Nearby Share.
 */
data class ZtVaultBundleMetadata(
    val pan: String,
    val dob: String,
    val assessmentYearsIncluded: List<String>,
    val payloadJson: String,
    val exportedAtUtcMillis: Long = System.currentTimeMillis()
)

data class ZtVaultDecryptionResult(
    val isSuccess: Boolean,
    val payloadJson: String? = null,
    val errorMessage: String? = null
)

object EncryptedZtVaultMigrationEngine {

    private val MAGIC_HEADER = "ZTVAULT1".toByteArray(StandardCharsets.US_ASCII)
    private const val SALT_LENGTH_BYTES = 32
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PBKDF2_ITERATIONS = 256_000
    private const val AES_KEY_LENGTH_BITS = 256

    /**
     * Derives an AES-256 key from PAN, DOB, user passcode, and a random salt.
     */
    private fun deriveKey(pan: String, dob: String, passcode: String, salt: ByteArray): SecretKeySpec {
        val passphrase = "${pan.trim().uppercase()}:${dob.trim()}:${passcode.trim()}"
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH_BITS)
        val algorithm = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            "PBKDF2WithHmacSHA512"
        } catch (_: Exception) {
            "PBKDF2WithHmacSHA256"
        }
        val factory = SecretKeyFactory.getInstance(algorithm)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    /**
     * Compresses raw string into GZIP bytes.
     */
    private fun compressGzip(raw: String): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gz ->
            gz.write(raw.toByteArray(StandardCharsets.UTF_8))
        }
        return baos.toByteArray()
    }

    // VUL-06 FIX: Hard cap to block GZIP-bomb decompression attacks (OOM).
    private const val MAX_DECOMPRESSED_BYTES = 50L * 1024L * 1024L // 50 MB

    /**
     * Decompresses GZIP bytes into raw string.
     * Throws SecurityException if decompressed output exceeds 50 MB (GZIP bomb guard).
     */
    private fun decompressGzip(compressed: ByteArray): String {
        val bais = ByteArrayInputStream(compressed)
        GZIPInputStream(bais).use { gz ->
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var totalRead = 0L
            var n: Int
            while (gz.read(buffer).also { n = it } != -1) {
                totalRead += n
                if (totalRead > MAX_DECOMPRESSED_BYTES) {
                    throw SecurityException(
                        "Vault archive exceeds maximum allowed size (50 MB). Possible decompression attack."
                    )
                }
                baos.write(buffer, 0, n)
            }
            return baos.toString(StandardCharsets.UTF_8.name())
        }
    }

    /**
     * Exports a secure .ztvault encrypted binary bundle.
     */
    fun exportVaultBundle(
        metadata: ZtVaultBundleMetadata,
        userPasscode: String
    ): ByteArray {
        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

        val aesKey = deriveKey(metadata.pan, metadata.dob, userPasscode, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)

        val compressedPayload = compressGzip(metadata.payloadJson)
        val encryptedBytes = cipher.doFinal(compressedPayload)

        // Structure: MAGIC(8) + SALT(32) + IV(12) + CIPHERTEXT
        val buffer = ByteBuffer.allocate(MAGIC_HEADER.size + salt.size + iv.size + encryptedBytes.size)
        buffer.put(MAGIC_HEADER)
        buffer.put(salt)
        buffer.put(iv)
        buffer.put(encryptedBytes)
        return buffer.array()
    }

    /**
     * Decrypts and unpacks a .ztvault binary bundle.
     */
    fun importVaultBundle(
        vaultBytes: ByteArray,
        pan: String,
        dob: String,
        userPasscode: String
    ): ZtVaultDecryptionResult {
        if (vaultBytes.size < (MAGIC_HEADER.size + SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16)) {
            return ZtVaultDecryptionResult(
                isSuccess = false,
                errorMessage = "Invalid vault file: Size too small or corrupted"
            )
        }

        val buffer = ByteBuffer.wrap(vaultBytes)
        val magic = ByteArray(MAGIC_HEADER.size)
        buffer.get(magic)
        if (!magic.contentEquals(MAGIC_HEADER)) {
            return ZtVaultDecryptionResult(
                isSuccess = false,
                errorMessage = "Invalid vault header: Expected ZTVAULT1 file format"
            )
        }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        buffer.get(salt)

        val iv = ByteArray(IV_LENGTH_BYTES)
        buffer.get(iv)

        val cipherBytes = ByteArray(buffer.remaining())
        buffer.get(cipherBytes)

        return try {
            val aesKey = deriveKey(pan, dob, userPasscode, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec)

            val decryptedCompressed = cipher.doFinal(cipherBytes)
            val jsonString = decompressGzip(decryptedCompressed)

            ZtVaultDecryptionResult(
                isSuccess = true,
                payloadJson = jsonString
            )
        } catch (e: Exception) {
            ZtVaultDecryptionResult(
                isSuccess = false,
                errorMessage = "Authentication failed: Incorrect PAN, Date of Birth, or Passcode. (${e.message})"
            )
        }
    }
}

