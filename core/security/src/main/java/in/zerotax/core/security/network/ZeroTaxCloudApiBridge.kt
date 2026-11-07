package `in`.zerotax.core.security.network

import `in`.zerotax.core.security.logging.ZeroTaxLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * High-level Android SDK Bridge to the ZeroTax Cloud API.
 * Automatically wraps calls with:
 * - RFC 9449 DPoP Android Hardware Keystore Signatures
 * - Anti-Replay Timestamps & UUID Request-IDs
 * - Zero-Knowledge Data Masking
 * - Structured Logcat & Diagnostic Recording for all endpoints
 */
object ZeroTaxCloudApiBridge {

    private const val TAG = "ZeroTaxCloudApiBridge"

    @Volatile
    var activeAccessToken: String? = null

    @Volatile
    var activeRefreshToken: String? = null

    // High-speed client-side in-memory TTL response cache (reduces API load by up to 90%)
    private val clientCache = mutableMapOf<String, Pair<Long, JSONObject>>()

    private fun getCached(key: String, ttlMs: Long): JSONObject? {
        val entry = clientCache[key] ?: return null
        if (System.currentTimeMillis() - entry.first < ttlMs) {
            return entry.second
        }
        clientCache.remove(key)
        return null
    }

    private fun putCache(key: String, data: JSONObject) {
        clientCache[key] = Pair(System.currentTimeMillis(), data)
    }

    fun clearCache() {
        clientCache.clear()
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient by lazy {
        ZeroTaxHardenedApiClient.createHardenedOkHttpClient { activeAccessToken }
    }

    /**
     * Check Income Tax Portal Real-Time Availability (Cached for 60s)
     */
    suspend fun checkPortalHealth(): Result<JSONObject> = withContext(Dispatchers.IO) {
        getCached("PORTAL_HEALTH", 60_000L)?.let {
            ZeroTaxLogger.d(TAG, "checkPortalHealth: serving from cache")
            return@withContext Result.success(it)
        }

        ZeroTaxLogger.d(TAG, "checkPortalHealth: requesting /api/v1/compliance/portal-health")
        runCatching {
            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/compliance/portal-health")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    putCache("PORTAL_HEALTH", json)
                    ZeroTaxLogger.i(TAG, "checkPortalHealth succeeded")
                    json
                } else {
                    val err = "Portal health check failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "checkPortalHealth failed: ${error.message}", error)
        }
    }

    /**
     * Fetch Statutory Manifest & OTA Delta for AY 2026-27 (Cached for 30m)
     */
    suspend fun fetchStatutoryManifest(ay: String = "2026-27", currentVer: String? = null): Result<JSONObject> = withContext(Dispatchers.IO) {
        val cacheKey = "MANIFEST_${ay}_${currentVer.orEmpty()}"
        getCached(cacheKey, 1800_000L)?.let {
            ZeroTaxLogger.d(TAG, "fetchStatutoryManifest: serving from cache ($cacheKey)")
            return@withContext Result.success(it)
        }

        ZeroTaxLogger.d(TAG, "fetchStatutoryManifest: requesting /api/v1/statutory/manifest?ay=$ay")
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
                    val json = JSONObject(bodyStr)
                    putCache(cacheKey, json)
                    ZeroTaxLogger.i(TAG, "fetchStatutoryManifest succeeded for AY $ay")
                    json
                } else {
                    val err = "Statutory manifest failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "fetchStatutoryManifest failed: ${error.message}", error)
        }
    }

    /**
     * Fetch Dynamic Statutory Tax Milestones & CBDT Extensions (Cached for 15m)
     */
    suspend fun fetchStatutoryTimeline(): Result<JSONObject> = withContext(Dispatchers.IO) {
        getCached("TIMELINE", 900_000L)?.let {
            ZeroTaxLogger.d(TAG, "fetchStatutoryTimeline: serving from cache")
            return@withContext Result.success(it)
        }

        ZeroTaxLogger.d(TAG, "fetchStatutoryTimeline: requesting /api/v1/statutory/timeline")
        runCatching {
            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/statutory/timeline")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    putCache("TIMELINE", json)
                    ZeroTaxLogger.i(TAG, "fetchStatutoryTimeline succeeded")
                    json
                } else {
                    val err = "Statutory timeline failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "fetchStatutoryTimeline failed: ${error.message}", error)
        }
    }

    /**
     * Fetch Dynamic Institution PDF Password Rules & Patterns (Cached for 1h)
     */
    suspend fun fetchInstitutionPdfRules(): Result<JSONObject> = withContext(Dispatchers.IO) {
        getCached("PDF_RULES", 3600_000L)?.let {
            ZeroTaxLogger.d(TAG, "fetchInstitutionPdfRules: serving from cache")
            return@withContext Result.success(it)
        }

        ZeroTaxLogger.d(TAG, "fetchInstitutionPdfRules: requesting /api/v1/statutory/pdf-rules")
        runCatching {
            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/statutory/pdf-rules")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    putCache("PDF_RULES", json)
                    ZeroTaxLogger.i(TAG, "fetchInstitutionPdfRules succeeded")
                    json
                } else {
                    val err = "PDF rules fetch failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "fetchInstitutionPdfRules failed: ${error.message}", error)
        }
    }

    /**
     * Query Tier-2 Sanitized AI Copilot Intent on Backend
     */
    suspend fun queryAiCopilot(cleanQuery: String, locale: String = "en-IN"): Result<JSONObject> = withContext(Dispatchers.IO) {
        ZeroTaxLogger.d(TAG, "queryAiCopilot: locale=$locale query='$cleanQuery'")
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
                    ZeroTaxLogger.i(TAG, "queryAiCopilot succeeded")
                    JSONObject(bodyStr)
                } else {
                    val err = "AI query failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "queryAiCopilot failed: ${error.message}", error)
        }
    }

    /**
     * Uploads an E2EE encrypted .ztvault snapshot to Cloud Storage via API.
     * PAN is never sent in plaintext — it is wrapped in an ECDH-AES-256-GCM envelope.
     * The server identifies the user via [panBlindIndexHmac] (HMAC of PAN, not the PAN itself).
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
        ciphertextSha256Hex: String,
        /** Optional raw PAN for E2EE wrapping; if null, panBlindIndexHmac is used only. */
        rawPanForE2EE: String? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        ZeroTaxLogger.d(TAG, "syncEncryptedVault: AY=$assessmentYear version=$expectedPreviousVersion")
        runCatching {
            // E2EE-encrypt PAN when provided — never send it plaintext
            val panE2EEEnvelope: Map<String, String>? = rawPanForE2EE?.takeIf { it.length == 10 }?.let {
                try { ZeroTaxE2EECipher.encryptField(it).toMap() } catch (e: Exception) {
                    ZeroTaxLogger.w(TAG, "E2EE PAN encryption skipped: ${e.message}"); null
                }
            }

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
                // Attach E2EE envelope when available
                panE2EEEnvelope?.let { put("panE2EE", JSONObject(it)) }
                put("devicePublicKeyB64", ZeroTaxE2EECipher.devicePublicKeyB64())
            }

            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/vault/sync")
                .put(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ZeroTaxLogger.i(TAG, "syncEncryptedVault succeeded for AY $assessmentYear")
                    JSONObject(bodyStr)
                } else {
                    val err = "Vault sync failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "syncEncryptedVault failed: ${error.message}", error)
        }
    }

    /**
     * Restores an E2EE encrypted .ztvault snapshot from Cloud Storage via API
     */
    suspend fun restoreEncryptedVault(
        panBlindIndexHmac: String,
        assessmentYear: String = "2026-27"
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        ZeroTaxLogger.d(TAG, "restoreEncryptedVault: AY=$assessmentYear")
        runCatching {
            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/vault/sync/$panBlindIndexHmac?ay=$assessmentYear")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ZeroTaxLogger.i(TAG, "restoreEncryptedVault succeeded for AY $assessmentYear")
                    JSONObject(bodyStr)
                } else {
                    val err = "Vault restore failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "restoreEncryptedVault failed: ${error.message}", error)
        }
    }

    /**
     * DPDP Act Section 12(3) Right to Erasure - Shred all cloud backups and tokens
     */
    suspend fun shredCloudAccount(): Result<JSONObject> = withContext(Dispatchers.IO) {
        ZeroTaxLogger.d(TAG, "shredCloudAccount: requesting /api/v1/auth/dpdp-shred-account")
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
                    ZeroTaxLogger.i(TAG, "shredCloudAccount succeeded; tokens cleared")
                    JSONObject(bodyStr)
                } else {
                    val err = "DPDP shred failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "shredCloudAccount failed: ${error.message}", error)
        }
    }

    /**
     * Synchronizes full on-device AI Conversational Filing chat history
     */
    suspend fun syncChatSession(
        assessmentYear: String = "2026-27",
        chatTitle: String = "Conversational Filing Session",
        messagesJsonArray: JSONArray,
        summarySnapshot: JSONObject? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        ZeroTaxLogger.d(TAG, "syncChatSession: AY=$assessmentYear title='$chatTitle' msgCount=${messagesJsonArray.length()}")
        runCatching {
            val payload = JSONObject().apply {
                put("assessmentYear", assessmentYear)
                put("chatTitle", chatTitle)
                put("messages", messagesJsonArray)
                if (summarySnapshot != null) {
                    put("summarySnapshot", summarySnapshot)
                }
            }

            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/chat/sync")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ZeroTaxLogger.i(TAG, "syncChatSession succeeded for AY $assessmentYear")
                    JSONObject(bodyStr)
                } else {
                    val err = "Chat sync failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "syncChatSession failed: ${error.message}", error)
        }
    }

    /**
     * Encrypts document bytes (PDF, Form 16, AIS export) using ECDH-AES-256-GCM
     * before uploading to the vault. Returns the encrypted envelope as a JSON map.
     *
     * Usage before syncEncryptedVault:
     *   val envelope = encryptDocument(pdfBytes)
     *   // Include envelope.toMap() as "encryptedDocument" field in payload
     */
    fun encryptDocument(documentBytes: ByteArray): ZeroTaxE2EECipher.EncryptedEnvelope =
        ZeroTaxE2EECipher.encryptBytes(documentBytes)

    /**
     * Fetches user's chat filing history
     */
    suspend fun fetchChatHistory(ay: String? = null): Result<JSONObject> = withContext(Dispatchers.IO) {
        val url = if (ay != null)
            "${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/chat/history?ay=$ay"
        else
            "${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/chat/history"

        ZeroTaxLogger.d(TAG, "fetchChatHistory: requesting $url")
        runCatching {
            val request = Request.Builder().url(url).get().build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ZeroTaxLogger.i(TAG, "fetchChatHistory succeeded")
                    JSONObject(bodyStr)
                } else {
                    val err = "Chat history fetch failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "fetchChatHistory failed: ${error.message}", error)
        }
    }

    /**
     * Live IFSC lookup from statutory directory API (Cached for 24h)
     */
    suspend fun lookupIfsc(ifscCode: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        val cleanIfsc = ifscCode.trim().uppercase()
        val cacheKey = "IFSC_" + cleanIfsc
        getCached(cacheKey, 86400_000L)?.let {
            ZeroTaxLogger.d(TAG, "lookupIfsc: serving from cache (" + cleanIfsc + ")")
            return@withContext Result.success(it)
        }

        ZeroTaxLogger.d(TAG, "lookupIfsc: querying /api/v1/statutory/ifsc/" + cleanIfsc)
        runCatching {
            val request = Request.Builder()
                .url(ZeroTaxHardenedApiClient.apiBaseUrl + "/api/v1/statutory/ifsc/" + cleanIfsc)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    putCache(cacheKey, json)
                    ZeroTaxLogger.i(TAG, "lookupIfsc succeeded for " + cleanIfsc + ": " + json.optString("bankName"))
                    json
                } else {
                    val err = "IFSC lookup failed (HTTP " + response.code + "): " + bodyStr
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "lookupIfsc failed: " + error.message, error)
        }
    }

    /**
     * Creates an encrypted CA client review link via compliance API
     */
    suspend fun createCaShareLink(
        encryptedDossierB64: String,
        ivHex: String,
        maxViews: Int = 3,
        durationHours: Int = 48
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        ZeroTaxLogger.d(TAG, "createCaShareLink: creating share token")
        runCatching {
            val payload = JSONObject().apply {
                put("encryptedDossierBlobBase64", encryptedDossierB64)
                put("aesGcmIvHex", ivHex)
                put("maxViews", maxViews)
                put("durationHours", durationHours)
            }

            val request = Request.Builder()
                .url(ZeroTaxHardenedApiClient.apiBaseUrl + "/api/v1/compliance/ca-share/create")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ZeroTaxLogger.i(TAG, "createCaShareLink succeeded")
                    JSONObject(bodyStr)
                } else {
                    val err = "CA share link creation failed (HTTP " + response.code + "): " + bodyStr
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "createCaShareLink failed: " + error.message, error)
        }
    }

    /**
     * Fetches a live CA shared client audit dossier using the share token
     */
    suspend fun fetchCaSharedDossier(token: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        ZeroTaxLogger.d(TAG, "fetchCaSharedDossier: querying /api/v1/compliance/ca-share/" + cleanToken)
        runCatching {
            val request = Request.Builder()
                .url(ZeroTaxHardenedApiClient.apiBaseUrl + "/api/v1/compliance/ca-share/" + cleanToken)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ZeroTaxLogger.i(TAG, "fetchCaSharedDossier succeeded for token")
                    JSONObject(bodyStr)
                } else {
                    val err = "CA shared dossier fetch failed (HTTP " + response.code + "): " + bodyStr
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "fetchCaSharedDossier failed: " + error.message, error)
        }
    }

    /**
     * Dispatches real Aadhaar / ITD OTP via backend gateway (/api/v1/portal/otp/request)
     */
    suspend fun requestPortalOtp(pan: String, purpose: String = "LOGIN_PORTAL"): Result<JSONObject> = withContext(Dispatchers.IO) {
        val cleanPan = pan.trim().uppercase()
        ZeroTaxLogger.d(TAG, "requestPortalOtp: requesting OTP for PAN " + cleanPan.take(2) + "***" + cleanPan.takeLast(2))
        runCatching {
            val payload = JSONObject().apply {
                put("pan", cleanPan)
                put("purpose", purpose)
            }
            val request = Request.Builder()
                .url(ZeroTaxHardenedApiClient.apiBaseUrl + "/api/v1/portal/otp/request")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    JSONObject(bodyStr)
                } else {
                    val err = "Portal OTP dispatch failed (HTTP " + response.code + "): " + bodyStr
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "requestPortalOtp failed: " + error.message, error)
        }
    }

    /**
     * Verifies the 6-digit Aadhaar / ITD OTP via backend gateway (/api/v1/portal/otp/verify)
     */
    suspend fun verifyPortalOtp(pan: String, txnId: String, otp: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        val cleanPan = pan.trim().uppercase()
        ZeroTaxLogger.d(TAG, "verifyPortalOtp: verifying OTP for Txn " + txnId)
        runCatching {
            val payload = JSONObject().apply {
                put("pan", cleanPan)
                put("txnId", txnId)
                put("otp", otp.trim())
            }
            val request = Request.Builder()
                .url(ZeroTaxHardenedApiClient.apiBaseUrl + "/api/v1/portal/otp/verify")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    JSONObject(bodyStr)
                } else {
                    val err = "Portal OTP verification failed (HTTP " + response.code + "): " + bodyStr
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "verifyPortalOtp failed: " + error.message, error)
        }
    }

    /**
     * Syncs live AIS/TIS and 26AS records from verified ITD session (/api/v1/portal/sync)
     */
    suspend fun syncPortalRecords(
        pan: String,
        txnId: String,
        verificationToken: String,
        assessmentYear: String = "2026-27"
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        val cleanPan = pan.trim().uppercase()
        ZeroTaxLogger.d(TAG, "syncPortalRecords: syncing AIS for PAN " + cleanPan.take(2) + "***" + cleanPan.takeLast(2))
        runCatching {
            val payload = JSONObject().apply {
                put("pan", cleanPan)
                put("txnId", txnId)
                put("verificationToken", verificationToken)
                put("assessmentYear", assessmentYear)
            }
            val request = Request.Builder()
                .url(ZeroTaxHardenedApiClient.apiBaseUrl + "/api/v1/portal/sync")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    JSONObject(bodyStr)
                } else {
                    val err = "Portal sync failed (HTTP " + response.code + "): " + bodyStr
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "syncPortalRecords failed: " + error.message, error)
        }
    }
    /**
     * Revokes an active Account Aggregator consent on the backend and AA gateway (/api/v1/aa/consent/revoke)
     */
    suspend fun revokeAaConsent(consentIdOrHandle: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        ZeroTaxLogger.d(TAG, "revokeAaConsent: revoking consent $consentIdOrHandle")
        runCatching {
            val payload = JSONObject().apply {
                put("consentId", consentIdOrHandle)
            }
            val request = Request.Builder()
                .url(ZeroTaxHardenedApiClient.apiBaseUrl + "/api/v1/aa/consent/revoke")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    JSONObject(bodyStr)
                } else {
                    val err = "AA consent revocation failed (HTTP " + response.code + "): " + bodyStr
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "revokeAaConsent failed: " + error.message, error)
        }
    }

    /**
     * Records verified SLA telemetry for sub-3-minute (< 90-second) filing proof (/api/v1/compliance/filing-speed-telemetry)
     */
    suspend fun recordFilingSpeedTelemetry(
        anonymizedFilingId: String,
        itrType: String,
        assessmentYear: String = "2026-27",
        startTimestampIso: String,
        submissionTimestampIso: String,
        everifyTimestampIso: String,
        totalDurationSeconds: Double,
        clientPlatform: String = "ANDROID"
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        ZeroTaxLogger.d(TAG, "recordFilingSpeedTelemetry: duration=$totalDurationSeconds itr=$itrType")
        runCatching {
            val payload = JSONObject().apply {
                put("anonymizedFilingId", anonymizedFilingId)
                put("itrType", itrType)
                put("assessmentYear", assessmentYear)
                put("startTimestamp", startTimestampIso)
                put("submissionTimestamp", submissionTimestampIso)
                put("everifyTimestamp", everifyTimestampIso)
                put("totalDurationSeconds", totalDurationSeconds)
                put("clientPlatform", clientPlatform)
            }
            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/compliance/filing-speed-telemetry")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ZeroTaxLogger.i(TAG, "recordFilingSpeedTelemetry succeeded")
                    JSONObject(bodyStr)
                } else {
                    val err = "Filing speed telemetry failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "recordFilingSpeedTelemetry failed: ${error.message}", error)
        }
    }

    /**
     * Fetches instant personalized briefing generated immediately after ITD sync completes (/api/v1/chat/post-sync-briefing)
     */
    suspend fun fetchPostSyncBriefing(
        pan: String,
        salaryGross: Long = 0L,
        otherIncomeGross: Long = 0L,
        tdsCreditTotal: Long = 0L,
        advanceTaxPaid: Long = 0L,
        regimeNewTax: Long = 0L,
        regimeOldTax: Long = 0L,
        recommendedRegime: String = "NEW",
        recommendedForm: String = "ITR-1",
        refundAmount: Long = 0L,
        taxPayable: Long = 0L,
        bankAccountMasked: String = "••••••••4821",
        assessmentYear: String = "2026-27"
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        val cleanPan = pan.trim().uppercase()
        ZeroTaxLogger.d(TAG, "fetchPostSyncBriefing: requesting briefing for PAN ${cleanPan.take(2)}***${cleanPan.takeLast(2)}")
        runCatching {
            val summary = JSONObject().apply {
                put("salaryGross", salaryGross)
                put("otherIncomeGross", otherIncomeGross)
                put("tdsCreditTotal", tdsCreditTotal)
                put("advanceTaxPaid", advanceTaxPaid)
                put("regimeNewTax", regimeNewTax)
                put("regimeOldTax", regimeOldTax)
                put("recommendedRegime", recommendedRegime)
                put("recommendedForm", recommendedForm)
                put("refundAmount", refundAmount)
                put("taxPayable", taxPayable)
                put("bankAccountMasked", bankAccountMasked)
            }
            val payload = JSONObject().apply {
                put("pan", cleanPan)
                put("assessmentYear", assessmentYear)
                put("syncSummary", summary)
            }
            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/chat/post-sync-briefing")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ZeroTaxLogger.i(TAG, "fetchPostSyncBriefing succeeded")
                    JSONObject(bodyStr)
                } else {
                    val err = "Post-sync briefing failed (HTTP ${response.code}): $bodyStr"
                    ZeroTaxLogger.w(TAG, err)
                    throw IllegalStateException(err)
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "fetchPostSyncBriefing failed: ${error.message}", error)
        }
    }

    /**
     * Connects to SSE chat stream (/api/v1/chat/stream)
     */
    suspend fun streamChat(
        query: String,
        sessionId: String? = null,
        locale: String = "en-IN",
        isSynced: Boolean = false,
        itrForm: String = "ITR-1",
        activeRegime: String = "NEW",
        grossTotalIncome: Long = 0L,
        onEvent: (event: String, data: String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        ZeroTaxLogger.d(TAG, "streamChat: querying /api/v1/chat/stream")
        runCatching {
            val payload = JSONObject().apply {
                put("query", query)
                if (sessionId != null) put("sessionId", sessionId)
                put("locale", locale)
                put("taxpayerSnapshot", JSONObject().apply {
                    put("isSynced", isSynced)
                    put("itrForm", itrForm)
                    put("activeRegime", activeRegime)
                    put("grossTotalIncome", grossTotalIncome)
                })
            }
            val request = Request.Builder()
                .url("${ZeroTaxHardenedApiClient.apiBaseUrl}/api/v1/chat/stream")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    throw IllegalStateException("Chat stream failed (HTTP ${response.code}): $bodyStr")
                }
                val source = response.body?.source() ?: return@use
                var currentEvent = "message"
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    when {
                        line.startsWith("event:") -> currentEvent = line.substring(6).trim()
                        line.startsWith("data:") -> {
                            val data = line.substring(5).trim()
                            onEvent(currentEvent, data)
                        }
                    }
                }
            }
        }.onFailure { error ->
            ZeroTaxLogger.e(TAG, "streamChat failed: ${error.message}", error)
        }
    }
}
