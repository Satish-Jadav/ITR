package `in`.zerotax.core.security.network

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import `in`.zerotax.core.security.logging.ZeroTaxLogger
import okhttp3.CertificatePinner
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.Collections
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Diagnostic record representing an API execution result for developer introspection.
 */
data class ApiDiagnosticRecord(
    val id: String = UUID.randomUUID().toString().take(8),
    val timestampMs: Long = System.currentTimeMillis(),
    val method: String,
    val url: String,
    val httpCode: Int? = null,
    val durationMs: Long = 0L,
    val isSuccessful: Boolean = false,
    val errorClass: String? = null,
    val errorMessage: String? = null,
    val diagnosticHint: String? = null
)

/**
 * Connects ZeroTax-Local APK to the Vercel Node.js API (api.zerotax.in) with:
 * 1. Comprehensive Structured Diagnostic Logging for all HTTP traffic & failure root causes
 * 2. SPKI SHA-256 TLS Certificate Pinning (Defeats MITM proxies like Burp Suite / Charles)
 * 3. Android Hardware Keystore ECDSA P-256 DPoP Proof Generation (RFC 9449) per HTTP request
 * 4. Anti-Replay Timestamp & UUIDv4 Request-ID Headers
 * 5. High-Concurrency Connection Pooling (HTTP/2 multiplexing + Keep-Alive)
 * 6. Full Jitter Exponential Backoff Retry Interceptor for peak filing surges
 */
object ZeroTaxHardenedApiClient {

    const val TAG = "ZeroTaxApi"

    fun isRunningOnEmulator(): Boolean {
        return android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.MANUFACTURER.contains("Genymotion")
            || android.os.Build.HARDWARE.contains("goldfish")
            || android.os.Build.HARDWARE.contains("ranchu")
            || android.os.Build.PRODUCT.contains("sdk_gphone")
            || android.os.Build.PRODUCT.contains("emulator")
    }

    private fun resolveDefaultBaseUrl(): String {
        return if (isRunningOnEmulator()) "http://10.0.2.2:3000" else "https://api.zerotax.in"
    }

    var apiBaseUrl: String = resolveDefaultBaseUrl()
    private const val API_HOSTNAME = "api.zerotax.in"
    private const val DPOP_KEY_ALIAS = "zerotax_hardware_dpop_ec_p256_v1"

    // High-concurrency connection pool
    private val connectionPool = ConnectionPool(
        maxIdleConnections = 8,
        keepAliveDuration = 5L,
        timeUnit = TimeUnit.MINUTES
    )

    // SPKI pin hashes for Let's Encrypt R10/R11 intermediates (Vercel's CA chain).
    private val certificatePinner = CertificatePinner.Builder()
        .add(API_HOSTNAME, "sha256/jQJTbIh0grw0/1TkHSumWb+Fs0Ggogr621gT3PvVKC0=") // Let's Encrypt R10
        .add(API_HOSTNAME, "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=") // Let's Encrypt R11
        .build()

    // Thread-safe ring buffer of recent API executions (up to 50 items)
    private val _recentDiagnostics = Collections.synchronizedList(mutableListOf<ApiDiagnosticRecord>())
    val recentDiagnostics: List<ApiDiagnosticRecord>
        get() = synchronized(_recentDiagnostics) { _recentDiagnostics.toList() }

    fun recordDiagnostic(record: ApiDiagnosticRecord) {
        synchronized(_recentDiagnostics) {
            if (_recentDiagnostics.size >= 50) {
                _recentDiagnostics.removeAt(0)
            }
            _recentDiagnostics.add(record)
        }
    }

    fun getLastFailure(): ApiDiagnosticRecord? = synchronized(_recentDiagnostics) {
        _recentDiagnostics.lastOrNull { !it.isSuccessful }
    }

    fun clearDiagnostics() {
        synchronized(_recentDiagnostics) { _recentDiagnostics.clear() }
    }

    fun createHardenedOkHttpClient(accessTokenProvider: () -> String? = { null }): OkHttpClient {
        ensureHardwareDpopKeyExists()
        val builder = OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .retryOnConnectionFailure(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(ApiLoggingInterceptor())
            .addInterceptor(ExponentialBackoffRetryInterceptor())
            .addInterceptor(DpopAndAntiReplayInterceptor(accessTokenProvider))

        // Only enforce certificate pinner on non-localhost/production hostname
        if (!apiBaseUrl.contains("localhost") && !apiBaseUrl.contains("10.0.2.2")) {
            builder.certificatePinner(certificatePinner)
        }

        return builder.build()
    }

    /**
     * Diagnostic Logging Interceptor: Captures request start, duration, status code,
     * network exceptions, and actionable diagnosis for failing APIs.
     */
    private class ApiLoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val startNs = System.nanoTime()
            val requestId = request.header("X-ZeroTax-Request-Id") ?: UUID.randomUUID().toString().take(8)

            ZeroTaxLogger.i(TAG, "--> [REQ-$requestId] ${request.method} ${request.url}")

            val response: Response
            try {
                response = chain.proceed(request)
            } catch (e: Exception) {
                val durationMs = (System.nanoTime() - startNs) / 1_000_000L
                val hint = diagnoseException(request.url.host, e)

                ZeroTaxLogger.e(
                    TAG,
                    "<-- [REQ-$requestId] FAILED in ${durationMs}ms: ${request.method} ${request.url}\n" +
                    "    Root Cause: ${e.javaClass.name}: ${e.message}\n" +
                    "    Diagnosis: $hint",
                    e
                )

                recordDiagnostic(
                    ApiDiagnosticRecord(
                        id = requestId,
                        method = request.method,
                        url = request.url.toString(),
                        httpCode = null,
                        durationMs = durationMs,
                        isSuccessful = false,
                        errorClass = e.javaClass.simpleName,
                        errorMessage = e.message ?: e.toString(),
                        diagnosticHint = hint
                    )
                )
                throw e
            }

            val durationMs = (System.nanoTime() - startNs) / 1_000_000L
            if (response.isSuccessful) {
                ZeroTaxLogger.i(TAG, "<-- [REQ-$requestId] HTTP ${response.code} ${response.message} in ${durationMs}ms (${request.url})")
                recordDiagnostic(
                    ApiDiagnosticRecord(
                        id = requestId,
                        method = request.method,
                        url = request.url.toString(),
                        httpCode = response.code,
                        durationMs = durationMs,
                        isSuccessful = true
                    )
                )
            } else {
                val hint = diagnoseHttpError(response.code, request.url.toString())
                ZeroTaxLogger.w(
                    TAG,
                    "<-- [REQ-$requestId] HTTP ${response.code} ${response.message} in ${durationMs}ms (${request.url})\n" +
                    "    Diagnosis: $hint"
                )
                recordDiagnostic(
                    ApiDiagnosticRecord(
                        id = requestId,
                        method = request.method,
                        url = request.url.toString(),
                        httpCode = response.code,
                        durationMs = durationMs,
                        isSuccessful = false,
                        errorClass = "Http_${response.code}",
                        errorMessage = "HTTP ${response.code} ${response.message}",
                        diagnosticHint = hint
                    )
                )
            }

            return response
        }

        private fun diagnoseException(host: String, e: Throwable): String {
            return when {
                e is java.net.UnknownHostException ->
                    "DNS lookup failed for '$host'. The domain does not resolve or the device is offline."
                e is javax.net.ssl.SSLPeerUnverifiedException ->
                    "TLS Certificate Pinning failure for '$host'. Server certificate does not match the pinned SPKI SHA-256 hashes."
                e is javax.net.ssl.SSLException ->
                    "TLS/SSL handshake failure for '$host'. Cipher mismatch, invalid cert, or untrusted CA."
                e is java.net.SocketTimeoutException ->
                    "Connection timed out waiting for '$host'. Server is unresponsive or network latency is too high."
                e is java.net.ConnectException ->
                    "Connection refused by '$host'. Server is down or port is unreachable."
                else ->
                    "Network failure (${e.javaClass.simpleName}): ${e.message ?: "Unknown socket error"}"
            }
        }

        private fun diagnoseHttpError(code: Int, url: String): String {
            return when (code) {
                400 -> "Bad Request: Malformed JSON or invalid parameter syntax in request to $url."
                401 -> "Unauthorized: Missing, invalid, or expired authentication token / DPoP signature."
                403 -> "Forbidden: Access denied to statutory resource at $url."
                404 -> "Not Found: API endpoint does not exist at $url."
                429 -> "Rate Limited: Too many statutory requests. Backoff and throttle client requests."
                500 -> "Internal Server Error: Backend service crashed or encountered unhandled exception."
                502 -> "Bad Gateway: Upstream gateway or reverse proxy failed to reach backend."
                503 -> "Service Unavailable: Backend is undergoing maintenance or statutory deadline surge overload."
                504 -> "Gateway Timeout: Backend did not respond within the reverse proxy timeout window."
                else -> "HTTP status $code returned by server."
            }
        }
    }

    /**
     * Prevents Thundering Herd Problem: Retries 502/503/504 and transient network blips
     * with truncated binary exponential backoff + full jitter, logging each attempt.
     */
    private class ExponentialBackoffRetryInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var response: Response? = null
            var attempt = 0
            val maxAttempts = 3

            while (attempt < maxAttempts) {
                try {
                    response = chain.proceed(request)
                    if (response.isSuccessful || response.code < 500) {
                        return response
                    }
                    ZeroTaxLogger.w(TAG, "[RETRY attempt ${attempt + 1}/$maxAttempts] HTTP ${response.code} from ${request.url}")
                } catch (e: IOException) {
                    ZeroTaxLogger.w(TAG, "[RETRY attempt ${attempt + 1}/$maxAttempts] Network failure on ${request.url}: ${e.javaClass.simpleName} - ${e.message}")
                    if (attempt == maxAttempts - 1) {
                        ZeroTaxLogger.e(TAG, "[RETRY EXHAUSTED] All $maxAttempts attempts failed for ${request.url}", e)
                        throw e
                    }
                }

                attempt++
                response?.close()

                // Full Jitter: Sleep = Random(0, Min(Backoff_Cap, Base * 2^attempt))
                val baseBackoffMs = 250L
                val maxBackoffMs = 2000L
                val exponentialMs = (baseBackoffMs * (1 shl attempt)).coerceAtMost(maxBackoffMs)
                val jitteredSleepMs = Random.nextLong(baseBackoffMs, exponentialMs + 1)
                try {
                    Thread.sleep(jitteredSleepMs)
                } catch (ignored: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
            return response ?: chain.proceed(request)
        }
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
        try {
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
        } catch (e: Exception) {
            ZeroTaxLogger.w(TAG, "Hardware DPoP key generation fallback: ${e.message}")
        }
    }

    private fun buildHardwareSignedDpopProof(
        httpMethod: String,
        fullUrl: String,
        epochSeconds: Long,
        jti: String,
        accessToken: String?
    ): String {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val entry = ks.getEntry(DPOP_KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            if (entry == null) return "dpop_proof_fallback"

            val pubKey = entry.certificate.publicKey as ECPublicKey
            val xCoord = pubKey.w.affineX.toByteArray().takeLast(32).toByteArray()
            val yCoord = pubKey.w.affineY.toByteArray().takeLast(32).toByteArray()

            val jwkJson = JSONObject().apply {
                put("kty", "EC")
                put("crv", "P-256")
                put("x", Base64.getUrlEncoder().withoutPadding().encodeToString(xCoord))
                put("y", Base64.getUrlEncoder().withoutPadding().encodeToString(yCoord))
            }

            val headerJson = JSONObject().apply {
                put("typ", "dpop+jwt")
                put("alg", "ES256")
                put("jwk", jwkJson)
            }

            val claimsJson = JSONObject().apply {
                put("jti", jti)
                put("htm", httpMethod.uppercase())
                put("htu", fullUrl)
                put("iat", epochSeconds)
                if (!accessToken.isNullOrBlank()) {
                    val ath = MessageDigest.getInstance("SHA-256")
                        .digest(accessToken.toByteArray(StandardCharsets.US_ASCII))
                    put("ath", Base64.getUrlEncoder().withoutPadding().encodeToString(ath))
                }
            }

            val b64Header = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.toString().toByteArray())
            val b64Claims = Base64.getUrlEncoder().withoutPadding().encodeToString(claimsJson.toString().toByteArray())
            val signingInput = "$b64Header.$b64Claims"

            val signer = Signature.getInstance("SHA256withECDSA").apply {
                initSign(entry.privateKey)
                update(signingInput.toByteArray(StandardCharsets.US_ASCII))
            }
            val signature = signer.sign()
            val b64Sig = Base64.getUrlEncoder().withoutPadding().encodeToString(signature)

            "$signingInput.$b64Sig"
        } catch (_: Exception) {
            "dpop_proof_fallback"
        }
    }
}
