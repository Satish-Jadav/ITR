package `in`.zerotax.feature.portalbridge

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import `in`.zerotax.core.model.AisCategory
import `in`.zerotax.core.model.AisTransactionRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class PayloadType { PREFILL_JSON, FORM_26AS_JSON, AIS_TIS_JSON, PORTAL_PROFILE_JSON }

data class PortalAutoFillPayload(
    val pan: String,
    val assessmentYear: String = "2026-27",
    val optOutNewRegime: Boolean = false,
    val grossSalary: Long,
    val sec10Exemptions: Long = 0L,
    val standardDeduction: Long = 75_000L,
    val netSalary: Long,
    val deductions80C: Long = 0L,
    val deductions80D: Long = 0L,
    val deductions80CCD2: Long = 0L,
    val tdsClaimed: Long = 0L,
    val selfAssessmentTaxPaid: Long = 0L,
    val bankAccountNo: String = "",
    val bankIfsc: String = ""
)

sealed interface PortalBridgeState {
    data object Idle : PortalBridgeState
    data object AwaitingUserOtp : PortalBridgeState
    data class CapturingPayloads(val captured: Set<PayloadType>) : PortalBridgeState
    data class CompletedAndSanitized(val payloads: Map<PayloadType, String>) : PortalBridgeState
    data class AutoFillCompleted(val fieldsPopulatedCount: Int) : PortalBridgeState
    data class OfflineFallbackTriggered(val reason: String) : PortalBridgeState
}

class HardenedEphemeralPortalBridge(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<PortalBridgeState>(PortalBridgeState.Idle)
    val state: StateFlow<PortalBridgeState> = _state
    private val capturedPayloads = mutableMapOf<PayloadType, String>()
    private var watchdogJob: Job? = null
    var activePan: String = ""

    companion object {
        val ALLOWED_HOSTS = setOf(
            "eportal.incometax.gov.in",
            "www.incometax.gov.in",
            "contents.tdscpc.gov.in",
            "ais.insight.gov.in",
            "digilocker.meripehchan.gov.in",
            "api.digitallocker.gov.in",
            "accounts.google.com"
        )
        const val PORTAL_TIMEOUT_MS = 15_000L
        // VUL-09 FIX: Extend Aadhaar scrub regex to also cover hyphen and dot separators.
        // Original \s? only matched spaces; attackers could bypass with "1234-5678-9012" format.
        val AADHAAR_SCRUB_REGEX = Regex("""\b\d{4}[\s\-.]?\d{4}[\s\-.]?\d{4}\b""")

        private var _isSessionActive = false

        fun isPortalSessionActive(): Boolean = _isSessionActive

        fun setPortalSessionActive(active: Boolean) {
            _isSessionActive = active
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureIsolatedWebView(webView: WebView, onProgress: ((Int) -> Unit)? = null) {
        WebView.setWebContentsDebuggingEnabled(true)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = false
            saveFormData = false
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgress?.invoke(newProgress)
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val host = request?.url?.host?.lowercase() ?: return true
                val isAllowed = ALLOWED_HOSTS.contains(host) ||
                    host.endsWith(".incometax.gov.in") ||
                    host.endsWith(".gov.in") ||
                    host.endsWith(".bank.sbi") ||
                    host.endsWith(".onlinesbi.sbi") ||
                    host.endsWith(".hdfcbank.com") ||
                    host.endsWith(".icicibank.com")
                return !isAllowed
            }
            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                val code = errorResponse?.statusCode ?: 0
                if (request?.isForMainFrame == true && code in listOf(500, 502, 503, 504, 429)) {
                    triggerOfflineFallback("Portal HTTP $code Maintenance/Overload")
                }
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    triggerOfflineFallback(error?.description?.toString() ?: "Network unreachable")
                }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(getReadOnlyInterceptorScript(), null)
                _state.value = PortalBridgeState.AwaitingUserOtp
                if (activePan.isNotBlank()) {
                    view?.let { injectPanLoginScript(it, activePan) }
                }
                if (url?.contains("dashboard", ignoreCase = true) == true || url?.contains("efile", ignoreCase = true) == true) {
                    view?.let { injectFetchLivePrefillAndAisScript(it, "2026-27") }
                }
            }
        }
        webView.setDownloadListener { url, _, _, mimeType, _ ->
            if (url.endsWith(".json", ignoreCase = true) || mimeType.contains("json", ignoreCase = true)) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val connection = java.net.URL(url).openConnection()
                        val cookies = CookieManager.getInstance().getCookie(url)
                        if (!cookies.isNullOrBlank()) {
                            connection.setRequestProperty("Cookie", cookies)
                        }
                        val content = connection.getInputStream().bufferedReader().use { it.readText() }
                        withContext(Dispatchers.Default) {
                            PortalJsBridge().onJsonIntercepted(url, content)
                        }
                    } catch (e: Exception) {}
                }
            }
        }
        webView.addJavascriptInterface(PortalJsBridge(), "ZeroTaxClientBridge")
    }

    fun getCapturedPayloads(): Map<PayloadType, String> = synchronized(capturedPayloads) { HashMap(capturedPayloads) }

    fun parsePayloadToTransactions(sanitizedJson: String, type: PayloadType): List<AisTransactionRecord> {
        val results = mutableListOf<AisTransactionRecord>()
        try {
            val root = JSONObject(sanitizedJson)

            // 1. Direct official ITR Filing JSON (ITR-1 / ITR-2 / ITR-4 downloaded from portal)
            val itrObj = root.optJSONObject("ITR") ?: root
            val itrDoc = itrObj.optJSONObject("ITR1")
                ?: itrObj.optJSONObject("ITR2")
                ?: itrObj.optJSONObject("ITR4")
                ?: (if (root.has("ITR1_IncomeDeductions") || root.has("IncomeDeductions")) root else null)

            if (itrDoc != null) {
                val incDed = itrDoc.optJSONObject("ITR1_IncomeDeductions")
                    ?: itrDoc.optJSONObject("IncomeDeductions")
                    ?: itrDoc
                val grossSal = incDed.optLong("GrossSalary", incDed.optLong("Salary", 0L))
                val employerCategory = itrDoc.optJSONObject("PersonalInfo")?.optString("EmployerCategory", "OTH") ?: "OTH"

                if (grossSal > 0L) {
                    results.add(
                        AisTransactionRecord(
                            transactionId = "PORTAL-ITR-SAL-${System.currentTimeMillis()}",
                            category = AisCategory.SALARY,
                            reportingEntity = "Portal Verified Employment ($employerCategory)",
                            accountOrFolioNumber = "ITD-VERIFIED-SALARY",
                            grossAmount = grossSal,
                            tdsWithheld = 0L,
                            quarterOrDate = "AY-2026-27",
                            suggestedSchedule = "Salary"
                        )
                    )
                }

                val othersInc = incDed.optJSONObject("OthersInc")
                val otherList = othersInc?.optJSONArray("OthersIncDtlsOthSrc")
                if (otherList != null) {
                    for (i in 0 until otherList.length()) {
                        val item = otherList.getJSONObject(i)
                        val desc = item.optString("OthSrcNatureDesc", "")
                        val amt = item.optLong("OthSrcOthAmount", 0L)
                        if (amt > 0L) {
                            val cat = if (desc == "SAV") AisCategory.SAVINGS_INTEREST else AisCategory.TIME_DEPOSIT_INTEREST
                            val label = if (desc == "SAV") "Savings Bank Interest (ITR-1)" else "Term Deposit Interest (ITR-1)"
                            results.add(
                                AisTransactionRecord(
                                    transactionId = "PORTAL-ITR-INT-$desc-$i",
                                    category = cat,
                                    reportingEntity = label,
                                    accountOrFolioNumber = "ITD-VERIFIED-INT",
                                    grossAmount = amt,
                                    tdsWithheld = 0L,
                                    quarterOrDate = "AY-2026-27",
                                    suggestedSchedule = "OS"
                                )
                            )
                        }
                    }
                } else {
                    val incOthSrc = incDed.optLong("IncomeOthSrc", 0L)
                    if (incOthSrc > 0L) {
                        results.add(
                            AisTransactionRecord(
                                transactionId = "PORTAL-ITR-OTH-${System.currentTimeMillis()}",
                                category = AisCategory.SAVINGS_INTEREST,
                                reportingEntity = "Income from Other Sources (ITR-1)",
                                accountOrFolioNumber = "ITD-VERIFIED-OTH",
                                grossAmount = incOthSrc,
                                tdsWithheld = 0L,
                                quarterOrDate = "AY-2026-27",
                                suggestedSchedule = "OS"
                            )
                        )
                    }
                }
                if (results.isNotEmpty()) {
                    return results
                }
            }

            when (type) {
                PayloadType.PREFILL_JSON -> {
                    val tdsSalArray = root.optJSONArray("tdsOnSalaries")
                        ?: root.optJSONObject("TDSonSalaries")?.optJSONArray("TDSonSalary")
                        ?: root.optJSONArray("TDSonSalaries")
                    if (tdsSalArray != null) {
                        for (i in 0 until tdsSalArray.length()) {
                            val item = tdsSalArray.getJSONObject(i)
                            results.add(
                                AisTransactionRecord(
                                    transactionId = item.optString("id", "PREFILL-SAL-$i"),
                                    category = AisCategory.SALARY,
                                    reportingEntity = item.optString("employerOrDeductorOrCollecterName", item.optString("EmployerName", "Employer")),
                                    accountOrFolioNumber = item.optString("tanOfEmployer", item.optString("EmployerTAN", "TAN-SALARY")),
                                    grossAmount = item.optLong("incomeChargeableUnderSalaries", item.optLong("TotalGrossSalary", 0L)),
                                    tdsWithheld = item.optLong("totalTDSSal", item.optLong("TotalTDSDeducted", 0L)),
                                    quarterOrDate = "FY-2025-26",
                                    suggestedSchedule = "Salary"
                                )
                            )
                        }
                    }
                    val tdsOtherArray = root.optJSONArray("tdsOnOthThanSalaries")
                        ?: root.optJSONObject("TDSonOthThanSals")?.optJSONArray("TDSonOthThanSal")
                        ?: root.optJSONArray("TDSonOthThanSals")
                    if (tdsOtherArray != null) {
                        for (i in 0 until tdsOtherArray.length()) {
                            val item = tdsOtherArray.getJSONObject(i)
                            results.add(
                                AisTransactionRecord(
                                    transactionId = item.optString("id", "PREFILL-OTH-$i"),
                                    category = AisCategory.SAVINGS_INTEREST,
                                    reportingEntity = item.optString("deductorName", item.optString("EmployerOrDeductorOrCollecterName", "Financial Institution")),
                                    accountOrFolioNumber = item.optString("tanOfDeductor", item.optString("DeductorTAN", "TAN-BANK")),
                                    grossAmount = item.optLong("grossAmount", item.optLong("TotGrossAmt", 0L)),
                                    tdsWithheld = item.optLong("tdsDeducted", item.optLong("TotalTDSDeducted", 0L)),
                                    quarterOrDate = "FY-2025-26",
                                    suggestedSchedule = "OS"
                                )
                            )
                        }
                    }
                }
                PayloadType.AIS_TIS_JSON -> {
                    val txnsArray = root.optJSONArray("transactions")
                        ?: root.optJSONArray("aisDetails")
                        ?: root.optJSONArray("informationDetails")
                    if (txnsArray != null) {
                        for (i in 0 until txnsArray.length()) {
                            val obj = txnsArray.getJSONObject(i)
                            val catStr = obj.optString("category", obj.optString("infoDesc", "SAVINGS_INTEREST")).uppercase()
                            val category = when {
                                catStr.contains("SALARY") -> AisCategory.SALARY
                                catStr.contains("SAVING") -> AisCategory.SAVINGS_INTEREST
                                catStr.contains("DEPOSIT") || catStr.contains("TIME") -> AisCategory.TIME_DEPOSIT_INTEREST
                                catStr.contains("DIVIDEND") -> AisCategory.DIVIDENDS
                                catStr.contains("SECURIT") || catStr.contains("SHARE") || catStr.contains("MUTUAL") -> AisCategory.SALE_OF_SECURITIES
                                catStr.contains("RENT") -> AisCategory.RENT_RECEIVED
                                else -> AisCategory.HIGH_VALUE_CASH
                            }
                            results.add(
                                AisTransactionRecord(
                                    transactionId = obj.optString("id", obj.optString("txnId", "AIS-$i")),
                                    category = category,
                                    reportingEntity = obj.optString("deductorOrIssuerName", obj.optString("sourceName", "Reporting Entity")),
                                    accountOrFolioNumber = obj.optString("deductorTan", obj.optString("accountNo", "ACCT-REF")),
                                    grossAmount = obj.optLong("grossAmount", obj.optLong("amount", 0L)),
                                    tdsWithheld = obj.optLong("tdsWithheld", obj.optLong("taxDeducted", 0L)),
                                    quarterOrDate = obj.optString("transactionDate", "Q4-2026"),
                                    suggestedSchedule = if (category == AisCategory.SALARY) "Salary" else "OS"
                                )
                            )
                        }
                    }
                }
                PayloadType.FORM_26AS_JSON -> {
                    val parts = listOf("partA", "partA1", "partB")
                    parts.forEach { partKey ->
                        val partArray = root.optJSONArray(partKey)
                        if (partArray != null) {
                            for (i in 0 until partArray.length()) {
                                val item = partArray.getJSONObject(i)
                                results.add(
                                    AisTransactionRecord(
                                        transactionId = item.optString("id", "26AS-$partKey-$i"),
                                        category = if (partKey == "partA") AisCategory.SALARY else AisCategory.SAVINGS_INTEREST,
                                        reportingEntity = item.optString("deductorName", "Deductor"),
                                        accountOrFolioNumber = item.optString("tan", "TAN-26AS"),
                                        grossAmount = item.optLong("totalAmountCredited", 0L),
                                        tdsWithheld = item.optLong("totalTdsDeducted", 0L),
                                        quarterOrDate = "FY-2025-26",
                                        suggestedSchedule = if (partKey == "partA") "Salary" else "OS"
                                    )
                                )
                            }
                        }
                    }
                }
                PayloadType.PORTAL_PROFILE_JSON -> {
                    val grossSalary = root.optLong("grossSalary",
                        root.optLong("grossTotalIncome",
                            root.optLong("totalIncome",
                                root.optLong("salary",
                                    root.optLong("incomeChargeableUnderSalaries", 0L)))))
                    val tdsPaid = root.optLong("tdsWithheld",
                        root.optLong("totalTds",
                            root.optLong("totalTaxPaid",
                                root.optLong("tds", 0L))))
                    val interest = root.optLong("savingsInterest",
                        root.optLong("interestIncome", 0L))

                    if (grossSalary > 0L) {
                        results.add(
                            AisTransactionRecord(
                                transactionId = "PORTAL-SAL-${System.currentTimeMillis()}",
                                category = AisCategory.SALARY,
                                reportingEntity = root.optString("employerName", root.optString("employerOrDeductorName", "Portal Verified Employer")),
                                accountOrFolioNumber = root.optString("employerTan", "TAN-SALARY"),
                                grossAmount = grossSalary,
                                tdsWithheld = tdsPaid,
                                quarterOrDate = "FY-2025-26",
                                suggestedSchedule = "Salary"
                            )
                        )
                    }
                    if (interest > 0L) {
                        results.add(
                            AisTransactionRecord(
                                transactionId = "PORTAL-INT-${System.currentTimeMillis()}",
                                category = AisCategory.SAVINGS_INTEREST,
                                reportingEntity = "Savings Bank Interest",
                                accountOrFolioNumber = "SB-PORTAL",
                                grossAmount = interest,
                                tdsWithheld = 0L,
                                quarterOrDate = "FY-2025-26",
                                suggestedSchedule = "OS"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return results
    }

    fun parseAllCapturedToTransactions(): List<AisTransactionRecord> {
        val all = mutableListOf<AisTransactionRecord>()
        synchronized(capturedPayloads) {
            capturedPayloads.forEach { (type, json) ->
                all.addAll(parsePayloadToTransactions(json, type))
            }
        }
        return all
    }

    private fun getReadOnlyInterceptorScript(): String = """
        (function() {
            if (window.__zeroTaxHookInstalled) return;
            window.__zeroTaxHookInstalled = true;

            function checkAndForward(url, text) {
                if (!text || typeof text !== 'string') return;
                const lowerUrl = (url || '').toLowerCase();
                const isTarget = lowerUrl.includes('prefill') ||
                                 lowerUrl.includes('ais') ||
                                 lowerUrl.includes('26as') ||
                                 lowerUrl.includes('tis') ||
                                 lowerUrl.includes('foservices') ||
                                 lowerUrl.includes('itr') ||
                                 lowerUrl.includes('filing') ||
                                 lowerUrl.includes('profile') ||
                                 lowerUrl.includes('tax') ||
                                 lowerUrl.includes('salary') ||
                                 lowerUrl.includes('return') ||
                                 lowerUrl.includes('downloadfile') ||
                                 lowerUrl.includes('income');
                const hasItrSignature = text.includes('"ITR"') || text.includes('"ITR1"') || text.includes('"GrossSalary"') || text.includes('"GrossTotIncome"');
                if ((isTarget || hasItrSignature) && window.ZeroTaxClientBridge && window.ZeroTaxClientBridge.onJsonIntercepted) {
                    try {
                        const trimmed = text.trim();
                        if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
                            window.ZeroTaxClientBridge.onJsonIntercepted(url, text);
                        }
                    } catch(e) {}
                }
            }

            // Hook window.fetch
            const origFetch = window.fetch;
            if (origFetch) {
                window.fetch = async function(...args) {
                    const response = await origFetch.apply(this, args);
                    const url = (args[0] && args[0].url) ? args[0].url : String(args[0]);
                    try {
                        const clone = response.clone();
                        clone.text().then(body => checkAndForward(url, body)).catch(() => {});
                    } catch(e) {}
                    return response;
                };
            }

            // Hook XMLHttpRequest (Angular HttpClient)
            const origOpen = XMLHttpRequest.prototype.open;
            const origSend = XMLHttpRequest.prototype.send;
            XMLHttpRequest.prototype.open = function(method, url, ...rest) {
                this.__zeroTaxUrl = url;
                return origOpen.apply(this, [method, url, ...rest]);
            };
            XMLHttpRequest.prototype.send = function(...args) {
                this.addEventListener('load', function() {
                    try {
                        const url = this.__zeroTaxUrl || '';
                        if (this.responseType === 'blob' && this.response instanceof Blob) {
                            this.response.text().then(function(txt) {
                                checkAndForward(url, txt);
                            }).catch(function() {});
                        } else {
                            checkAndForward(url, this.responseText);
                        }
                    } catch(e) {}
                });
                return origSend.apply(this, args);
            };
        })();
    """.trimIndent()

    /**
     * Client-Side Assisted Filing u/s 139(1B):
     * Directly injects verified return numbers into the official e-filing portal DOM fields.
     *
     * VUL-08 FIX: All payload values are passed via a single JSON object encoded by JSONObject.quote().
     * This prevents Kotlin string-template injection of arbitrary JS code if any Long field were
     * somehow manipulated to contain JS-breaking characters. Values are parsed in JS from the JSON object.
     */
    fun autoFillPortalFields(webView: WebView, payload: PortalAutoFillPayload) {
        // Build a JSON object; JSONObject handles all escaping internally.
        val dataJson = JSONObject().apply {
            put("grossSalary", payload.grossSalary)
            put("sec10Exemptions", payload.sec10Exemptions)
            put("standardDeduction", payload.standardDeduction)
            put("netSalary", payload.netSalary)
            put("deductions80C", payload.deductions80C)
            put("deductions80D", payload.deductions80D)
            put("deductions80CCD2", payload.deductions80CCD2)
            put("tdsClaimed", payload.tdsClaimed)
        }.toString()
        // JSONObject.quote wraps the JSON string safely for JS consumption.
        val escapedDataJson = JSONObject.quote(dataJson)
        val script = """
            (function() {
                const data = JSON.parse($escapedDataJson);
                let populated = 0;
                function setInputValue(selectors, val) {
                    for (let sel of selectors) {
                        let el = document.querySelector(sel);
                        if (el) {
                            el.value = val;
                            el.dispatchEvent(new Event('input', { bubbles: true }));
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                            populated++;
                            return true;
                        }
                    }
                    return false;
                }

                // Auto-fill Salary & Allowances
                setInputValue(['input[name*="grossSalary"]', 'input[id*="grossSalary"]', 'input[formcontrolname="grossSalary"]'], String(data.grossSalary));
                setInputValue(['input[name*="sec10"]', 'input[id*="sec10"]', 'input[formcontrolname="sec10"]'], String(data.sec10Exemptions));
                setInputValue(['input[name*="stdDed"]', 'input[id*="stdDed"]', 'input[formcontrolname="stdDed"]', 'input[name*="16ia"]'], String(data.standardDeduction));
                setInputValue(['input[name*="netSalary"]', 'input[id*="netSalary"]'], String(data.netSalary));

                // Auto-fill Deductions
                if (data.deductions80C > 0) {
                    setInputValue(['input[name*="80c"]', 'input[id*="80c"]', 'input[formcontrolname="sec80c"]'], String(data.deductions80C));
                }
                if (data.deductions80D > 0) {
                    setInputValue(['input[name*="80d"]', 'input[id*="80d"]', 'input[formcontrolname="sec80d"]'], String(data.deductions80D));
                }
                if (data.deductions80CCD2 > 0) {
                    setInputValue(['input[name*="80ccd2"]', 'input[id*="80ccd2"]'], String(data.deductions80CCD2));
                }

                // Auto-fill TDS Claimed
                setInputValue(['input[name*="tdsClaimed"]', 'input[id*="tdsClaimed"]', 'input[formcontrolname="tdsClaimed"]'], String(data.tdsClaimed));

                if (window.ZeroTaxClientBridge && window.ZeroTaxClientBridge.onAutoFillComplete) {
                    window.ZeroTaxClientBridge.onAutoFillComplete(populated);
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }

    inner class PortalJsBridge {
        @JavascriptInterface
        fun onJsonIntercepted(url: String, rawBody: String) {
            scope.launch(Dispatchers.Default) {
                runCatching {
                    val sanitized = rawBody.replace(AADHAAR_SCRUB_REGEX, "[Aadhaar Redacted]")
                    JSONObject(sanitized)
                    val type = when {
                        url.contains("prefill", true) -> PayloadType.PREFILL_JSON
                        url.contains("26as", true) -> PayloadType.FORM_26AS_JSON
                        url.contains("ais", true) || url.contains("tis", true) -> PayloadType.AIS_TIS_JSON
                        url.contains("downloadfile", true) || sanitized.contains("\"ITR\"", true) || sanitized.contains("\"GrossSalary\"", true) -> PayloadType.PREFILL_JSON
                        else -> PayloadType.PORTAL_PROFILE_JSON
                    }
                    synchronized(capturedPayloads) {
                        capturedPayloads[type] = sanitized
                        _state.value = PortalBridgeState.CapturingPayloads(capturedPayloads.keys.toSet())
                    }
                    val parsed = parsePayloadToTransactions(sanitized, type)
                    if (parsed.isNotEmpty()) {
                        ItdTransactionFetcher.updateLiveTransactions(parsed)
                        if (activePan.isNotBlank()) {
                            ItdTransactionFetcher.updateAndPersistLiveTransactions(activePan, parsed, null)
                        }
                    }
                    if (capturedPayloads.keys.containsAll(setOf(PayloadType.PREFILL_JSON, PayloadType.FORM_26AS_JSON, PayloadType.AIS_TIS_JSON))) {
                        watchdogJob?.cancel()
                        val snapshot = HashMap(capturedPayloads)
                        sanitizePortalSessionImmediately {
                            _state.value = PortalBridgeState.CompletedAndSanitized(snapshot)
                        }
                    }
                }
            }
        }

        @JavascriptInterface
        fun onAutoFillComplete(fieldsCount: Int) {
            scope.launch(Dispatchers.Main) {
                _state.value = PortalBridgeState.AutoFillCompleted(fieldsCount)
            }
        }

        @JavascriptInterface
        fun onCpcStatusPolled(ackNo: String, statusText: String, refundAmount: Long) {
            scope.launch(Dispatchers.Main) {
                // Post-filing CPC processing status callback
            }
        }
    }

    fun injectCbdtJsonUploadScript(webView: WebView, cbdtJsonString: String) {
        val escapedJson = JSONObject.quote(cbdtJsonString)
        val script = """
            (function() {
                try {
                    const jsonStr = $escapedJson;
                    const fileInput = document.querySelector('input[type="file"]') || document.querySelector('input[name="file"]');
                    if (fileInput) {
                        const blob = new Blob([jsonStr], { type: 'application/json' });
                        const file = new File([blob], 'ITR_Filing.json', { type: 'application/json' });
                        const dt = new DataTransfer();
                        dt.items.add(file);
                        fileInput.files = dt.files;
                        fileInput.dispatchEvent(new Event('change', { bubbles: true }));
                        window.ZeroTaxClientBridge.onAutoFillComplete(1);
                    }
                } catch(e) {
                    console.error('CBDT JSON Injection error', e);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    fun injectAadhaarOtpVerificationScript(webView: WebView, otp6Digits: String) {
        // VUL-08: Sanitize OTP to strictly 6 digits before injecting; reject any non-numeric input.
        val safeOtp = otp6Digits.filter { it.isDigit() }.take(6)
        if (safeOtp.length != 6) return // Abort injection for malformed OTP
        val escapedOtp = JSONObject.quote(safeOtp)
        val script = """
            (function() {
                try {
                    const otpInput = document.querySelector('input[name*="otp" i]') || document.querySelector('input[id*="otp" i]');
                    if (otpInput) {
                        otpInput.value = $escapedOtp;
                        otpInput.dispatchEvent(new Event('input', { bubbles: true }));
                        otpInput.dispatchEvent(new Event('change', { bubbles: true }));
                        const submitBtn = document.querySelector('button[type="submit"]') || document.querySelector('button.e-verify-btn');
                        if (submitBtn) submitBtn.click();
                    }
                } catch(e) {
                    console.error('OTP verification script error', e);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    fun injectAisDiscrepancyFeedbackScript(webView: WebView, feedbackNote: String) {
        val escaped = JSONObject.quote(feedbackNote)
        val script = """
            (function() {
                try {
                    const feedbackArea = document.querySelector('textarea[name*="feedback" i]');
                    if (feedbackArea) {
                        feedbackArea.value = $escaped;
                        feedbackArea.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    fun injectPollCpcProcessingStatusScript(webView: WebView, ackNo: String) {
        // VUL-08: Sanitize ackNo to strictly alphanumeric before injecting into JS selector.
        val safeAckNo = ackNo.filter { it.isLetterOrDigit() || it == '-' }.take(30)
        val escapedAck = JSONObject.quote(safeAckNo)
        val script = """
            (function() {
                try {
                    const ack = $escapedAck;
                    const statusRow = document.querySelector('tr[data-ack="' + ack + '"]') || document.querySelector('.cpc-status-card');
                    if (statusRow) {
                        const text = statusRow.innerText || '';
                        window.ZeroTaxClientBridge.onCpcStatusPolled(ack, text, 0);
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun triggerOfflineFallback(reason: String) {
        watchdogJob?.cancel()
        sanitizePortalSessionImmediately { _state.value = PortalBridgeState.OfflineFallbackTriggered(reason) }
    }

    
    fun injectPanLoginScript(webView: WebView, pan: String) {
        val safePan = pan.trim().uppercase().filter { it.isLetterOrDigit() }.take(10)
        if (safePan.length != 10) return
        val escapedPan = JSONObject.quote(safePan)
        val script = """
            (function() {
                try {
                    const selectors = [
                        'input[name*="pan" i]',
                        'input[id*="pan" i]',
                        'input[formcontrolname="pan" i]',
                        'input[formcontrolname="userId" i]',
                        'input[placeholder*="pan" i]',
                        'input[placeholder*="user id" i]',
                        'input[id="pansearch"]',
                        'input.mat-input-element'
                    ];
                    for (let sel of selectors) {
                        let el = document.querySelector(sel);
                        if (el) {
                            el.value = $escapedPan;
                            el.dispatchEvent(new Event('input', { bubbles: true }));
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                            break;
                        }
                    }
                    const continueBtns = [
                        'button[type="submit"]',
                        'button.continue-btn',
                        'button.mat-flat-button'
                    ];
                    for (let sel of continueBtns) {
                        let btn = document.querySelector(sel);
                        if (btn && !btn.disabled) {
                            btn.click();
                            break;
                        }
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    fun injectPortalLoginCredentials(webView: WebView, pan: String, password: String) {
        val safePan = pan.trim().uppercase().filter { it.isLetterOrDigit() }.take(10)
        val escapedPan = JSONObject.quote(safePan)
        val escapedPassword = JSONObject.quote(password)
        val script = """
            (function() {
                try {
                    // 1. Fill PAN if User ID input is present
                    const panSelectors = [
                        'input[name*="pan" i]',
                        'input[id*="pan" i]',
                        'input[formcontrolname="pan" i]',
                        'input[formcontrolname="userId" i]',
                        'input[placeholder*="user id" i]',
                        'input[placeholder*="pan" i]',
                        'input[id="pansearch"]'
                    ];
                    for (let sel of panSelectors) {
                        let el = document.querySelector(sel);
                        if (el && !el.disabled) {
                            if (el.value !== $escapedPan) {
                                el.value = $escapedPan;
                                el.dispatchEvent(new Event('input', { bubbles: true }));
                                el.dispatchEvent(new Event('change', { bubbles: true }));
                            }
                            break;
                        }
                    }

                    // 2. Check "Please confirm your secure access message" checkbox if present
                    const chkSelectors = [
                        'input[type="checkbox"]',
                        'mat-checkbox input',
                        'input[id*="terms" i]',
                        'input[id*="secure" i]',
                        '#passwordCheckBox-input'
                    ];
                    for (let sel of chkSelectors) {
                        let chk = document.querySelector(sel);
                        if (chk && !chk.checked && !chk.disabled) {
                            chk.checked = true;
                            chk.dispatchEvent(new Event('change', { bubbles: true }));
                            chk.click();
                        }
                    }

                    // 3. Fill Password if password input is present
                    const pwdSelectors = [
                        '#loginPasswordField',
                        'input[type="password"]',
                        'input[name*="password" i]',
                        'input[id*="password" i]',
                        'input[formcontrolname*="password" i]'
                    ];
                    let pwdFilled = false;
                    for (let sel of pwdSelectors) {
                        let pwd = document.querySelector(sel);
                        if (pwd && !pwd.disabled) {
                            if (pwd.value !== $escapedPassword) {
                                pwd.focus();
                                pwd.value = $escapedPassword;
                                pwd.dispatchEvent(new Event('input', { bubbles: true }));
                                pwd.dispatchEvent(new Event('change', { bubbles: true }));
                            }
                            pwdFilled = true;
                            break;
                        }
                    }

                    // 4. Handle "Dual Login Detected" dialog if present
                    const allButtons = Array.from(document.querySelectorAll('button'));
                    const dualLoginBtn = allButtons.find(b => b.innerText && b.innerText.includes('Login Here') && b.offsetParent !== null);
                    if (dualLoginBtn) {
                        dualLoginBtn.click();
                        return;
                    }

                    // 5. Click Continue / Submit button if enabled
                    const continueBtn = allButtons.find(b => {
                        const txt = (b.innerText || '').toLowerCase().trim();
                        return (txt === 'continue' || txt === 'login' || txt === 'submit') && !b.disabled && b.offsetParent !== null;
                    });
                    if (continueBtn && (pwdFilled || !document.querySelector('input[type="password"]'))) {
                        continueBtn.click();
                    }

                    // 6. When authenticated on dashboard, navigate to view filed returns / itrStatus
                    if (window.location.hash.includes('dashboard') && !window.location.hash.includes('itrStatus')) {
                        const filedBtn = Array.from(document.querySelectorAll('a, span')).find(el => (el.innerText || '').trim() === 'View Filed Returns');
                        if (filedBtn) {
                            filedBtn.click();
                        } else {
                            window.location.hash = '#/dashboard/itrStatus';
                        }
                    }

                    // 7. When on itrStatus, click Download JSON for latest filed return to fetch genuine return data
                    if (window.location.hash.includes('itrStatus')) {
                        const dlBtn = document.querySelector('button.dxmlback') || Array.from(document.querySelectorAll('button')).find(b => (b.innerText || '').trim() === 'Download JSON');
                        if (dlBtn && !window.__downloadTriggered) {
                            window.__downloadTriggered = true;
                            dlBtn.click();
                        }
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    fun injectExtractSessionAndDomDataScript(webView: WebView) {
        val script = """
            (function() {
                try {
                    let extracted = {
                        source: 'DOM_AND_STORAGE',
                        ay: '2026-27'
                    };

                    const storages = [window.sessionStorage, window.localStorage];
                    storages.forEach(store => {
                        if (!store) return;
                        for (let i = 0; i < store.length; i++) {
                            const key = store.key(i);
                            try {
                                const val = store.getItem(key);
                                if (val && (val.startsWith('{') || val.startsWith('['))) {
                                    const parsed = JSON.parse(val);
                                    if (parsed && typeof parsed === 'object') {
                                        if (parsed.grossTotalIncome || parsed.totalIncome || parsed.grossSalary) {
                                            extracted.grossSalary = parsed.grossSalary || parsed.grossTotalIncome || parsed.totalIncome;
                                        }
                                        if (parsed.tds || parsed.totalTds || parsed.taxPaid) {
                                            extracted.tdsWithheld = parsed.tds || parsed.totalTds || parsed.taxPaid;
                                        }
                                        if (parsed.fullName || parsed.name || parsed.taxpayerName) {
                                            extracted.fullName = parsed.fullName || parsed.name || parsed.taxpayerName;
                                        }
                                    }
                                }
                            } catch(e) {}
                        }
                    });

                    const textNodes = document.body ? document.body.innerText : '';
                    const grossMatch = textNodes.match(/(?:Gross Total Income|Total Gross Income|Gross Income)[^\d₹]*₹?\s*([\d,]+)/i);
                    if (grossMatch && grossMatch[1]) {
                        const rawVal = parseInt(grossMatch[1].replace(/,/g, ''), 10);
                        if (!isNaN(rawVal) && rawVal > 0) {
                            extracted.grossSalary = rawVal;
                        }
                    }
                    const tdsMatch = textNodes.match(/(?:Total Tax Paid|Tax Deducted|TDS Deposited)[^\d₹]*₹?\s*([\d,]+)/i);
                    if (tdsMatch && tdsMatch[1]) {
                        const rawVal = parseInt(tdsMatch[1].replace(/,/g, ''), 10);
                        if (!isNaN(rawVal) && rawVal > 0) {
                            extracted.tdsWithheld = rawVal;
                        }
                    }

                    if (extracted.grossSalary || extracted.tdsWithheld || extracted.fullName) {
                        if (window.ZeroTaxClientBridge && window.ZeroTaxClientBridge.onJsonIntercepted) {
                            window.ZeroTaxClientBridge.onJsonIntercepted('https://eportal.incometax.gov.in/iec/foservices/api/dom-summary', JSON.stringify(extracted));
                        }
                    }

                    // Trigger Download JSON for latest return if available
                    const dlBtn = document.querySelector('button.dxmlback') || Array.from(document.querySelectorAll('button')).find(b => (b.innerText || '').trim() === 'Download JSON');
                    if (dlBtn) {
                        dlBtn.click();
                    } else if (window.location.hash.includes('dashboard') && !window.location.hash.includes('itrStatus')) {
                        const filedBtn = Array.from(document.querySelectorAll('a, span')).find(el => (el.innerText || '').trim() === 'View Filed Returns');
                        if (filedBtn) filedBtn.click();
                        else window.location.hash = '#/dashboard/itrStatus';
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    fun injectFetchLivePrefillAndAisScript(webView: WebView, assessmentYear: String = "2026-27") {
        val script = """
            (function() {
                try {
                    const ays = ['2026-27', '2025-26', '2024-25'];
                    ays.forEach(ay => {
                        const endpoints = [
                            '/iec/foservices/api/prefill?ay=' + ay,
                            '/iec/foservices/api/ais/details?ay=' + ay,
                            '/iec/foservices/api/26as/download?ay=' + ay,
                            '/iec/foservices/api/user-profile',
                            '/iec/foservices/api/filing/details?ay=' + ay
                        ];
                        endpoints.forEach(ep => {
                            window.fetch(ep, {
                                method: 'GET',
                                headers: { 'Accept': 'application/json' }
                            }).then(r => {
                                if (r.ok) return r.text();
                                return null;
                            }).then(body => {
                                if (body && window.ZeroTaxClientBridge) {
                                    window.ZeroTaxClientBridge.onJsonIntercepted(ep, body);
                                }
                            }).catch(() => {});
                        });
                    });
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    fun sanitizePortalSessionImmediately(onComplete: () -> Unit = {}) {
        scope.launch(Dispatchers.Main) {
            delay(500) // Ensure in-flight rendering completes before wiping
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            capturedPayloads.clear()
            onComplete()
        }
    }
}
