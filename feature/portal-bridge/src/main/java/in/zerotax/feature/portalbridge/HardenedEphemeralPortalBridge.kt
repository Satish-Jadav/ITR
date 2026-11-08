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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class PayloadType { PREFILL_JSON, FORM_26AS_JSON, AIS_TIS_JSON }

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
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureIsolatedWebView(webView: WebView) {
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

    private fun getReadOnlyInterceptorScript(): String = """
        (function() {
            if (window.__zeroTaxHookInstalled) return;
            window.__zeroTaxHookInstalled = true;
            const origFetch = window.fetch;
            window.fetch = async function(...args) {
                const response = await origFetch.apply(this, args);
                const url = (args[0] && args[0].url) ? args[0].url : String(args[0]);
                const clone = response.clone();
                if (url.includes('prefill') || url.includes('ais') || url.includes('26as') || url.includes('tis')) {
                    clone.text().then(body => window.ZeroTaxClientBridge.onJsonIntercepted(url, body)).catch(() => {});
                }
                return response;
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
                        else -> return@runCatching
                    }
                    synchronized(capturedPayloads) {
                        capturedPayloads[type] = sanitized
                        _state.value = PortalBridgeState.CapturingPayloads(capturedPayloads.keys.toSet())
                    }
                    if (capturedPayloads.keys.containsAll(PayloadType.entries.toSet())) {
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
                        'input[placeholder*="pan" i]',
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

    fun injectFetchLivePrefillAndAisScript(webView: WebView, assessmentYear: String = "2026-27") {
        val escapedAy = JSONObject.quote(assessmentYear)
        val script = """
            (function() {
                try {
                    const ay = $escapedAy;
                    const endpoints = [
                        '/iec/foservices/api/prefill?ay=' + ay,
                        '/iec/foservices/api/ais/details?ay=' + ay,
                        '/iec/foservices/api/26as/download?ay=' + ay
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
