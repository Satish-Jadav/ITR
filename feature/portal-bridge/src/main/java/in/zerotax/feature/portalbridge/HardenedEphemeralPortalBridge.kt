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
            "ais.insight.gov.in"
        )
        const val PORTAL_TIMEOUT_MS = 15_000L
        val AADHAAR_SCRUB_REGEX = Regex("""\b\d{4}\s?\d{4}\s?\d{4}\b""")
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
                return !ALLOWED_HOSTS.contains(host)
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
     */
    fun autoFillPortalFields(webView: WebView, payload: PortalAutoFillPayload) {
        val script = """
            (function() {
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
                setInputValue(['input[name*="grossSalary"]', 'input[id*="grossSalary"]', 'input[formcontrolname="grossSalary"]'], '${payload.grossSalary}');
                setInputValue(['input[name*="sec10"]', 'input[id*="sec10"]', 'input[formcontrolname="sec10"]'], '${payload.sec10Exemptions}');
                setInputValue(['input[name*="stdDed"]', 'input[id*="stdDed"]', 'input[formcontrolname="stdDed"]', 'input[name*="16ia"]'], '${payload.standardDeduction}');
                setInputValue(['input[name*="netSalary"]', 'input[id*="netSalary"]'], '${payload.netSalary}');

                // Auto-fill Deductions
                if (${payload.deductions80C} > 0) {
                    setInputValue(['input[name*="80c"]', 'input[id*="80c"]', 'input[formcontrolname="sec80c"]'], '${payload.deductions80C}');
                }
                if (${payload.deductions80D} > 0) {
                    setInputValue(['input[name*="80d"]', 'input[id*="80d"]', 'input[formcontrolname="sec80d"]'], '${payload.deductions80D}');
                }
                if (${payload.deductions80CCD2} > 0) {
                    setInputValue(['input[name*="80ccd2"]', 'input[id*="80ccd2"]'], '${payload.deductions80CCD2}');
                }

                // Auto-fill TDS Claimed
                setInputValue(['input[name*="tdsClaimed"]', 'input[id*="tdsClaimed"]', 'input[formcontrolname="tdsClaimed"]'], '${payload.tdsClaimed}');

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
                        window.ZeroTaxBridge.onAutoFillComplete(1);
                    }
                } catch(e) {
                    console.error('CBDT JSON Injection error', e);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    fun injectAadhaarOtpVerificationScript(webView: WebView, otp6Digits: String) {
        val script = """
            (function() {
                try {
                    const otpInput = document.querySelector('input[name*="otp" i]') || document.querySelector('input[id*="otp" i]');
                    if (otpInput) {
                        otpInput.value = '$otp6Digits';
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
        val script = """
            (function() {
                try {
                    const statusRow = document.querySelector('tr[data-ack="$ackNo"]') || document.querySelector('.cpc-status-card');
                    if (statusRow) {
                        const text = statusRow.innerText || '';
                        window.ZeroTaxBridge.onCpcStatusPolled('$ackNo', text, 0);
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
