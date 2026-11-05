package `in`.zerotax.core.security.network

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Connects ZeroTax-Local APK to the Vercel Node.js API (api.zerotax.in) with:
 * 1. SPKI SHA-256 TLS Certificate Pinning (Defeats MITM proxies like Burp Suite / Charles)
 * 2. Android Hardware Keystore ECDSA P-256 DPoP Proof Generation (RFC 9449) per HTTP request
 * 3. Anti-Replay Timestamp & UUIDv4 Request-ID Headers
 */
object ZeroTaxHardenedApiClient {

    var apiBaseUrl: String = "https://api.zerotax.in"
    private const val API_HOSTNAME = "api.zerotax.in"
    private const val DPOP_KEY_ALIAS = "zerotax_hardware_dpop_ec_p256_v1"

    // Primary & backup SPKI SHA-256 public key hashes for api.zerotax.in (Vercel TLS)
    private val certificatePinner = CertificatePinner.Builder()
        .add(API_HOSTNAME, "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        .add(API_HOSTNAME, "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
        .build()

    fun createHardenedOkHttpClient(accessTokenProvider: () -> String? = { null }): OkHttpClient {
        ensureHardwareDpopKeyExists()
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(DpopAndAntiReplayInterceptor(accessTokenProvider))

        // Only enforce certificate pinner on non-localhost/production hostname
        if (!apiBaseUrl.contains("localhost") && !apiBaseUrl.contains("10.0.2.2")) {
            builder.certificatePinner(certificatePinner)
        }

        return builder.build()
    }

    private class DpopAndAntiReplayInterceptor(
        private val accessTokenProvider: () -> String?
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val nowSec = System.currentTimeMillis() / 1000L
            val requestId = UUID.randomUUID().toString()
            val token = accessTokenProvider()

            val dpopProofJwt = buildHardwareSignedDpopProof(
                httpMethod = original.method,
                fullUrl = original.url.toString(),
                epochSeconds = nowSec,
                jti = requestId,
                accessToken = token
            )

            val builder = original.newBuilder()
                .header("X-ZeroTax-Client-Version", "2026.1.0")
                .header("X-ZeroTax-Request-Id", requestId)
                .header("X-ZeroTax-Timestamp", nowSec.toString())
                .header("DPoP", dpopProofJwt)

            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "DPoP $token")
            }

            return chain.proceed(builder.build())
        }
    }

    fun ensureHardwareDpopKeyExists() {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(DPOP_KEY_ALIAS)) return

        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            DPOP_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        kpg.initialize(spec)
        kpg.generateKeyPair()
    }

    fun buildHardwareSignedDpopProof(
        httpMethod: String,
        fullUrl: String,
        epochSeconds: Long,
        jti: String,
        accessToken: String?
    ): String {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val entry = ks.getEntry(DPOP_KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: return ""

        val pubKey = entry.certificate.publicKey as ECPublicKey
        val xBytes = pubKey.w.affineX.toByteArray().takeLast(32).toByteArray()
        val yBytes = pubKey.w.affineY.toByteArray().takeLast(32).toByteArray()

        val headerJson = JSONObject().apply {
            put("typ", "dpop+jwt")
            put("alg", "ES256")
            put("jwk", JSONObject().apply {
                put("kty", "EC")
                put("crv", "P-256")
                put("x", base64UrlEncode(xBytes))
                put("y", base64UrlEncode(yBytes))
            })
        }

        val payloadJson = JSONObject().apply {
            put("htm", httpMethod.uppercase())
            put("htu", fullUrl)
            put("iat", epochSeconds)
            put("jti", jti)
            if (!accessToken.isNullOrBlank()) {
                val athBytes = MessageDigest.getInstance("SHA-256")
                    .digest(accessToken.toByteArray(StandardCharsets.UTF_8))
                put("ath", base64UrlEncode(athBytes))
            }
        }

        val signingInput = "${base64UrlEncode(headerJson.toString().toByteArray())}.${base64UrlEncode(payloadJson.toString().toByteArray())}"
        val signatureBytes = Signature.getInstance("SHA256withECDSA").run {
            initSign(entry.privateKey)
            update(signingInput.toByteArray(StandardCharsets.UTF_8))
            sign()
        }
        return "$signingInput.${base64UrlEncode(signatureBytes)}"
    }

    private fun base64UrlEncode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
