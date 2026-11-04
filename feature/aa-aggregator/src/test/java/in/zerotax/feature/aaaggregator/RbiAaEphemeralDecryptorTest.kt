package `in`.zerotax.feature.aaaggregator

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class RbiAaEphemeralDecryptorTest {

    @Test
    fun testDecryptAndExtractInterestInMemory_DecryptsAndWipesBuffers() {
        val sharedSecret = ByteArray(32) { (it + 1).toByte() }
        val fipNonce = ByteArray(32) { (it * 2).toByte() }
        val fiuNonce = ByteArray(32) { (it * 3).toByte() }

        // Replicate derivation in test to generate valid ciphertext
        val saltXorNonce = ByteArray(32)
        for (i in 0 until 32) {
            saltXorNonce[i] = (fipNonce[i].toInt() xor fiuNonce[i].toInt()).toByte()
        }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(saltXorNonce, "HmacSHA256"))
        val prk = mac.doFinal(sharedSecret)

        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(byteArrayOf(0x01))
        val okm = mac.doFinal()
        val derivedKey = ByteArray(32)
        System.arraycopy(okm, 0, derivedKey, 0, 32)

        val ivBytes = saltXorNonce.copyOfRange(20, 32)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derivedKey, "AES"), GCMParameterSpec(128, ivBytes))

        val plainXml = "<DepositAccount><Bank>HDFC Bank</Bank><SavingsInterest>14200</SavingsInterest><FdInterest>35000</FdInterest></DepositAccount>"
        val ciphertext = cipher.doFinal(plainXml.toByteArray(Charsets.UTF_8))

        // Call the decryptor
        val sharedSecretCopy = sharedSecret.clone()
        val summary = RbiAaEphemeralDecryptor.decryptAndExtractInterestInMemory(
            sharedSecretBytes = sharedSecretCopy,
            fipNonce32Bytes = fipNonce,
            fiuNonce32Bytes = fiuNonce,
            base64DecodedCiphertextWithIv = ciphertext
        ) { xml ->
            assertEquals(plainXml, xml)
            EphemeralBankInterestSummary(
                bankName = "HDFC Bank",
                maskedAccountRef = "XXXX-1234",
                totalSavingsInterestFyRupees = 14200L,
                totalFdInterestFyRupees = 35000L
            )
        }

        assertEquals("HDFC Bank", summary.bankName)
        assertEquals(14200L, summary.totalSavingsInterestFyRupees)
        assertEquals(35000L, summary.totalFdInterestFyRupees)

        // Verify that the decryptor zeroed out the passed shared secret bytes for security
        assertTrue("Shared secret buffer should be zeroized", sharedSecretCopy.all { it == 0.toByte() })
    }

    private fun assertTrue(message: String, condition: Boolean) {
        org.junit.Assert.assertTrue(message, condition)
    }
}
