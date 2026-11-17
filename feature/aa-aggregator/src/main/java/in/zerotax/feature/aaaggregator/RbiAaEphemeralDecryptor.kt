package `in`.zerotax.feature.aaaggregator

import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EphemeralBankInterestSummary(
    val bankName: String,
    val maskedAccountRef: String,
    val totalSavingsInterestFyRupees: Long,
    val totalFdInterestFyRupees: Long
)

/**
 * Complies with RBI NBFC-Account Aggregator Master Directions (2016) & ReBIT Sahamati FIData Spec.
 * Decrypts FIP payloads strictly in volatile RAM and zeroes all key material in the finally block.
 */
object RbiAaEphemeralDecryptor {
    private const val GCM_TAG_BITS = 128
    private const val AES_KEY_BYTES = 32
    private const val MIN_CIPHERTEXT_BYTES = 28 // 12-byte IV + 16-byte GCM authentication tag

    fun decryptAndExtractInterestInMemory(
        sharedSecretBytes: ByteArray,
        fipNonce32Bytes: ByteArray,
        fiuNonce32Bytes: ByteArray,
        base64DecodedCiphertextWithIv: ByteArray,
        ledgerParser: (String) -> EphemeralBankInterestSummary
    ): EphemeralBankInterestSummary {
        // Guard 1: RFC 7748 Low-Order Point Rejection (Constant-time check for all-zero shared secret)
        var zeroAcc = 0
        for (b in sharedSecretBytes) {
            zeroAcc = zeroAcc or b.toInt()
        }
        if (zeroAcc == 0) {
            throw SecurityException("Invalid X25519 point: all-zero shared secret rejected per RFC 7748.")
        }

        // Guard 2: Minimum ciphertext length check (IV + Tag)
        if (base64DecodedCiphertextWithIv.size < MIN_CIPHERTEXT_BYTES) {
            throw IllegalArgumentException("Ciphertext payload too short for AES-GCM (minimum $MIN_CIPHERTEXT_BYTES bytes required).")
        }

        val saltXorNonce = ByteArray(32)
        val derivedKey = ByteArray(AES_KEY_BYTES)
        var prk = ByteArray(0)
        var okm = ByteArray(0)
        var plaintextBytes = ByteArray(0)

        try {
            for (i in 0 until 32) {
                saltXorNonce[i] = (fipNonce32Bytes[i].toInt() xor fiuNonce32Bytes[i].toInt()).toByte()
            }
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(saltXorNonce, "HmacSHA256"))
            prk = mac.doFinal(sharedSecretBytes)

            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(byteArrayOf(0x01))
            okm = mac.doFinal()
            System.arraycopy(okm, 0, derivedKey, 0, AES_KEY_BYTES)

            val ivBytes = saltXorNonce.copyOfRange(20, 32)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derivedKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, ivBytes))
            plaintextBytes = cipher.doFinal(base64DecodedCiphertextWithIv)
            return ledgerParser(String(plaintextBytes, Charsets.UTF_8))
        } finally {
            Arrays.fill(sharedSecretBytes, 0.toByte())
            Arrays.fill(saltXorNonce, 0.toByte())
            Arrays.fill(derivedKey, 0.toByte())
            if (prk.isNotEmpty()) Arrays.fill(prk, 0.toByte())
            if (okm.isNotEmpty()) Arrays.fill(okm, 0.toByte())
            if (plaintextBytes.isNotEmpty()) Arrays.fill(plaintextBytes, 0.toByte())
        }
    }
}
