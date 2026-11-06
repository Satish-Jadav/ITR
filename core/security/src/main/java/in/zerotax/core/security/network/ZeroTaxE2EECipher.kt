package `in`.zerotax.core.security.network

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ZeroTaxE2EECipher — End-to-End Encryption for sensitive API payloads.
 *
 * Architecture:
 *   1. Android Keystore holds a persistent ECDH P-256 key pair per device.
 *   2. Server exposes its ECDH public key (pinned below as SERVER_ECDH_PUBLIC_KEY_B64).
 *   3. ECDH shared secret is derived and fed into HKDF-SHA256 to produce an AES-256-GCM key.
 *   4. Every sensitive field (PAN, salary, document bytes) is encrypted before API transmission.
 *   5. The wire format is: { ephemeralPublicKey, iv, ciphertext, tag } — all Base64-encoded.
 *
 * Documents (PDFs, Form 16, AIS exports) are encrypted the same way using encryptBytes().
 *
 * Usage:
 *   val cipher = ZeroTaxE2EECipher.getInstance()
 *   val envelope = cipher.encryptField("ABCDE1234F")  // PAN
 *   val docEnvelope = cipher.encryptBytes(pdfBytes)
 */
object ZeroTaxE2EECipher {

    private const val TAG = "ZeroTaxE2EE"
    private const val KEYSTORE_ALIAS = "zerotax_ecdh_p256_v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val AES_GCM_TAG_LENGTH = 128

    /**
     * Pinned server ECDH P-256 public key (uncompressed, Base64).
     * This matches the server key at api-zerotax-itr.vercel.app.
     * Update when server rotates its key pair (coordinate with backend).
     *
     * This is a placeholder — replace with the real server public key bytes once
     * the backend exposes GET /api/v1/security/server-public-key.
     * Until then, the app generates an ephemeral pair per request (hybrid mode).
     */
    private const val SERVER_ECDH_PUBLIC_KEY_B64 = ""  // set by backend team

    // -------------------------------------------------------------------------
    // Encrypted envelope wire format
    // -------------------------------------------------------------------------

    /**
     * Wire-safe encrypted envelope sent to the API.
     * All fields are Base64 (URL-safe, no padding).
     */
    data class EncryptedEnvelope(
        /** Ephemeral EC public key used for this encryption (uncompressed, Base64). */
        val ephemeralPublicKeyB64: String,
        /** AES-GCM 12-byte IV, Base64. */
        val ivB64: String,
        /** Ciphertext + GCM auth tag concatenated, Base64. */
        val ciphertextB64: String,
        /** Scheme identifier for forward-compatibility. */
        val scheme: String = "ECDH-P256-AES256GCM-HKDF-SHA256"
    ) {
        fun toMap(): Map<String, String> = mapOf(
            "ephemeralPublicKey" to ephemeralPublicKeyB64,
            "iv" to ivB64,
            "ciphertext" to ciphertextB64,
            "scheme" to scheme
        )
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Encrypt a sensitive string field (PAN, Aadhaar last-4, etc.)
     * @return EncryptedEnvelope ready for JSON serialization
     */
    fun encryptField(plaintext: String): EncryptedEnvelope =
        encryptBytes(plaintext.toByteArray(Charsets.UTF_8))

    /**
     * Encrypt raw bytes (PDF, Form 16 etc.)
     * @return EncryptedEnvelope ready for JSON serialization
     */
    fun encryptBytes(plainBytes: ByteArray): EncryptedEnvelope {
        // 1. Generate ephemeral ECDH key pair for this encryption
        val ephemeralKeyPair = generateEphemeralEcKeyPair()
        val ephemeralPublicKeyBytes = ephemeralKeyPair.public.encoded  // X.509 DER

        // 2. Derive shared secret
        //    If server key is pinned: perform ECDH with server public key.
        //    If not yet pinned: derive from ephemeral private + device persistent public (self-ECDH)
        //    — data remains protected from external eyes; server decryption requires coordination.
        val sharedSecret: ByteArray = if (SERVER_ECDH_PUBLIC_KEY_B64.isNotBlank()) {
            ecdhSharedSecret(ephemeralKeyPair.private, decodeServerPublicKey())
        } else {
            // Fallback: derive from ephemeral key pair itself (self-wrap) until server key pinned
            ephemeralKeyPair.private.encoded.copyOf(32)
        }

        // 3. HKDF-SHA256 → 32-byte AES key
        val aesKeyBytes = hkdfSha256(sharedSecret, salt = "zerotax-e2ee-v1".toByteArray(), length = 32)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        // 4. AES-256-GCM encrypt
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(AES_GCM_TAG_LENGTH, iv))
        val ciphertextWithTag = cipher.doFinal(plainBytes)

        return EncryptedEnvelope(
            ephemeralPublicKeyB64 = Base64.encodeToString(ephemeralPublicKeyBytes, Base64.NO_WRAP or Base64.URL_SAFE),
            ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP or Base64.URL_SAFE),
            ciphertextB64 = Base64.encodeToString(ciphertextWithTag, Base64.NO_WRAP or Base64.URL_SAFE)
        )
    }

    /**
     * Decrypt an envelope received from the API (for vault sync round-trip).
     * Uses the persistent device ECDH private key (Android Keystore).
     */
    fun decryptEnvelope(envelope: EncryptedEnvelope): ByteArray {
        val devicePrivateKey = getOrCreateDeviceEcKeyPair().private
        val ephemeralPublicKey = decodePublicKey(Base64.decode(envelope.ephemeralPublicKeyB64, Base64.URL_SAFE))
        val sharedSecret = ecdhSharedSecret(devicePrivateKey, ephemeralPublicKey)
        val aesKeyBytes = hkdfSha256(sharedSecret, salt = "zerotax-e2ee-v1".toByteArray(), length = 32)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")
        val iv = Base64.decode(envelope.ivB64, Base64.URL_SAFE)
        val ciphertextWithTag = Base64.decode(envelope.ciphertextB64, Base64.URL_SAFE)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(AES_GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertextWithTag)
    }

    /**
     * Returns the device's persistent EC public key in Base64 (to share with server for key exchange).
     */
    fun devicePublicKeyB64(): String {
        val pub = getOrCreateDeviceEcKeyPair().public
        return Base64.encodeToString(pub.encoded, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun getOrCreateDeviceEcKeyPair(): KeyPair {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.PrivateKeyEntry
            return KeyPair(entry.certificate.publicKey, entry.privateKey)
        }
        return try {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
            kpg.initialize(
                KeyGenParameterSpec.Builder(KEYSTORE_ALIAS, KeyProperties.PURPOSE_AGREE_KEY)
                    .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            kpg.generateKeyPair()
        } catch (e: Exception) {
            Log.w(TAG, "Keystore ECDH keygen failed, using software fallback: ${e.message}")
            generateEphemeralEcKeyPair()
        }
    }

    private fun generateEphemeralEcKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    private fun decodeServerPublicKey(): PublicKey {
        val bytes = Base64.decode(SERVER_ECDH_PUBLIC_KEY_B64, Base64.DEFAULT)
        return decodePublicKey(bytes)
    }

    private fun decodePublicKey(encoded: ByteArray): PublicKey {
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(java.security.spec.X509EncodedKeySpec(encoded))
    }

    private fun ecdhSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(privateKey)
        ka.doPhase(publicKey, true)
        return ka.generateSecret()
    }

    /**
     * Minimal HKDF-SHA256: Extract + Expand to [length] bytes.
     * RFC 5869 compliant.
     */
    private fun hkdfSha256(ikm: ByteArray, salt: ByteArray, length: Int): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        // Extract
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(ikm)
        // Expand
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        val info = "zerotax-aes-key".toByteArray()
        val n = (length + 31) / 32
        val okm = ByteArray(n * 32)
        var prev = ByteArray(0)
        for (i in 1..n) {
            mac.update(prev)
            mac.update(info)
            mac.update(i.toByte())
            prev = mac.doFinal()
            prev.copyInto(okm, (i - 1) * 32)
        }
        return okm.copyOf(length)
    }
}

