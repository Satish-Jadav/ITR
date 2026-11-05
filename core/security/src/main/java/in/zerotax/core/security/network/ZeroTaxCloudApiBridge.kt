package `in`.zerotax.core.security.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * High-level Android SDK Bridge to the ZeroTax Vercel API.
 * Automatically wraps calls with:
 * - RFC 9449 DPoP Android Hardware Keystore Signatures
 * - Anti-Replay Timestamps & UUID Request-IDs
 * - Zero-Knowledge Data Masking
 */
object ZeroTaxCloudApiBridge {

    @Volatile
    var activeAccessToken: String? = null

    @Volatile
    var activeRefreshToken: String? = null

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient by lazy {
        ZeroTaxHardenedApiClient.createHardenedOkHttpClient { activeAccessToken }
    }

    /**
     * Check Income Tax Portal Real-Time Availability
     */
    suspend fun checkPortalHealth(): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/compliance/portal-health")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    JSONObject(bodyStr)
                } else {
                    throw IllegalStateException("Portal health check failed (${response.code}): $bodyStr")
                }
            }
        }
    }

    /**
     * Fetch Statutory Manifest & OTA Delta for AY 2026-27
     */
    suspend fun fetchStatutoryManifest(ay: String = "2026-27", currentVer: String? = null): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val urlBuilder = StringBuilder("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/statutory/manifest?ay=$ay")
            if (!currentVer.isNullOrBlank()) {
                urlBuilder.append("&currentVersion=$currentVer")
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    JSONObject(bodyStr)
                } else {
                    throw IllegalStateException("Statutory manifest failed (${response.code}): $bodyStr")
                }
            }
        }
    }

    /**
     * Query Tier-2 Sanitized AI Copilot Intent on Vercel Backend
     */
    suspend fun queryAiCopilot(cleanQuery: String, locale: String = "en-IN"): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("sanitizedQuery", cleanQuery)
                put("targetLocale", locale)
            }

            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/ai/copilot-intent")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    JSONObject(bodyStr)
                } else {
                    throw IllegalStateException("AI query failed (${response.code}): $bodyStr")
                }
            }
        }
    }

    /**
     * Uploads an E2EE encrypted .ztvault snapshot to Supabase Storage via Vercel API
     */
    suspend fun syncEncryptedVault(
        panBlindIndexHmac: String,
        assessmentYear: String = "2026-27",
        relationshipTag: String = "SELF",
        expectedPreviousVersion: Int = 0,
        pbkdf2SaltHex: String,
        aesGcmIvHex: String,
        aesGcmAuthTagHex: String,
        ciphertextPayloadBase64: String,
        ciphertextSha256Hex: String
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("panBlindIndexHmac", panBlindIndexHmac)
                put("assessmentYear", assessmentYear)
                put("relationshipTag", relationshipTag)
                put("expectedPreviousVersion", expectedPreviousVersion)
                put("pbkdf2SaltHex", pbkdf2SaltHex)
                put("aesGcmIvHex", aesGcmIvHex)
                put("aesGcmAuthTagHex", aesGcmAuthTagHex)
                put("ciphertextPayloadBase64", ciphertextPayloadBase64)
                put("ciphertextSha256Hex", ciphertextSha256Hex)
            }

            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/vault/sync")
                .put(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    JSONObject(bodyStr)
                } else {
                    throw IllegalStateException("Vault sync failed (${response.code}): $bodyStr")
                }
            }
        }
    }

    /**
     * Restores an E2EE encrypted .ztvault snapshot from Supabase Storage via Vercel API
     */
    suspend fun restoreEncryptedVault(
        panBlindIndexHmac: String,
        assessmentYear: String = "2026-27"
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/vault/sync/$panBlindIndexHmac?ay=$assessmentYear")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    JSONObject(bodyStr)
                } else {
                    throw IllegalStateException("Vault restore failed (${response.code}): $bodyStr")
                }
            }
        }
    }

    /**
     * DPDP Act Section 12(3) Right to Erasure - Shred all cloud backups and tokens
     */
    suspend fun shredCloudAccount(): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("confirmErasurePhrase", "SHRED_MY_ZEROTAX_DATA")
                put("revokeActiveAaConsents", true)
            }

            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/auth/dpdp-shred-account")
                .delete(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    activeAccessToken = null
                    activeRefreshToken = null
                    JSONObject(bodyStr)
                } else {
                    throw IllegalStateException("DPDP shred failed (${response.code}): $bodyStr")
                }
            }
        }
    }
}
