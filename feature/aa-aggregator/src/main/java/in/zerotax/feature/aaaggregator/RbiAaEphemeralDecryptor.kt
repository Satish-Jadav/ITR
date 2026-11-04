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

    fun decryptAndExtractInterestInMemory(
        sharedSecretBytes: ByteArray,
        fipNonce32Bytes: ByteArray,
        fiuNonce32Bytes: ByteArray,
        base64DecodedCiphertextWithIv: ByteArray,
        ledgerParser: (String) -> EphemeralBankInterestSummary
    ): EphemeralBankInterestSummary {
        val saltXorNonce = ByteArray(32)
        val derivedKey = ByteArray(AES_KEY_BYTES)
        var plaintextBytes = ByteArray(0)
        try {
            for (i in 0 until 32) {
                saltXorNonce[i] = (fipNonce32Bytes[i].toInt() xor fiuNonce32Bytes[i].toInt()).toByte()
            }
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(saltXorNonce, "HmacSHA256"))
            val prk = mac.doFinal(sharedSecretBytes)

            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(byteArrayOf(0x01))
            val okm = mac.doFinal()
            System.arraycopy(okm, 0, derivedKey, 0, AES_KEY_BYTES)
            Arrays.fill(prk, 0.toByte())
            Arrays.fill(okm, 0.toByte())

            val ivBytes = saltXorNonce.copyOfRange(20, 32)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derivedKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, ivBytes))
            plaintextBytes = cipher.doFinal(base64DecodedCiphertextWithIv)
            return ledgerParser(String(plaintextBytes, Charsets.UTF_8))
        } finally {
            Arrays.fill(sharedSecretBytes, 0.toByte())
            Arrays.fill(saltXorNonce, 0.toByte())
            Arrays.fill(derivedKey, 0.toByte())
            if (plaintextBytes.isNotEmpty()) Arrays.fill(plaintextBytes, 0.toByte())
        }
    }

    // TODO(Phase-2): Add CAMS/KFintech & NSDL/CDSL Depository CAS FIP XML/JSON parser for ISIN-level Schedule 112A lots.
    // TODO(Phase-3): Add Consent Artifact status polling & 1-tap FIP consent revocation manager in Privacy Tab 3.
}
