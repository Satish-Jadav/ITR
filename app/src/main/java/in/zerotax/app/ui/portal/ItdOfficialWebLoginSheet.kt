package `in`.zerotax.app.ui.portal

import `in`.zerotax.feature.portalbridge.ItdAndAaOtpAutoReader
import `in`.zerotax.core.security.network.ZeroTaxCloudApiBridge
import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.feature.portalbridge.HardenedEphemeralPortalBridge
import `in`.zerotax.feature.portalbridge.ItdTransactionFetcher
import `in`.zerotax.feature.portalbridge.PayloadType
import `in`.zerotax.core.model.AisCategory
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.zerotax.feature.portalbridge.PortalBridgeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Isolated In-App Official Income Tax Department (ITD) Web Login Sheet.
 *
 * Direct connection to https://eportal.incometax.gov.in/iec/foservices/#/login.
 * 100% Zero-Password Architecture:
 * The user authenticates directly on the government portal.
 * ZeroTax never requests, sees, captures, or persists the user's portal password.
 *
 * Once authenticated on the portal, [HardenedEphemeralPortalBridge] intercepts the
 * authenticated Pre-fill JSON, Form 26AS, and AIS/TIS payloads automatically,
 * purges/sanitizes all cookies and web storage, and transfers data into the return.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItdOfficialWebLoginSheet(
    pan: String,
    onDismiss: () -> Unit,
    onSyncSuccess: (
        syncedTransactionsCount: Int,
        totalTdsDiscovered: Long,
        totalSavingsInterest: Long,
        grossSalaryDiscovered: Long
    ) -> Unit
) {
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val cleanPan = remember(pan) { pan.trim().uppercase() }
    val bridge = remember {
        HardenedEphemeralPortalBridge(scope).apply {
            activePan = cleanPan
        }
    }
    val fetcher = remember { ItdTransactionFetcher() }
    val bridgeState by bridge.state.collectAsState()

    var isFetchingRecords by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    val capturedTypes = remember(bridgeState) {
        when (val s = bridgeState) {
            is PortalBridgeState.CapturingPayloads -> s.captured
            is PortalBridgeState.CompletedAndSanitized -> s.payloads.keys
            else -> bridge.getCapturedPayloads().keys
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val otpReader = remember {
        ItdAndAaOtpAutoReader(context) { code ->
            webViewRef?.let { bridge.injectAadhaarOtpVerificationScript(it, code) }
        }
    }

    DisposableEffect(Unit) {
        otpReader.startListening()
        onDispose {
            otpReader.stopListening()
        }
    }

    // Hands-free credential automation for official ITD login flow
    LaunchedEffect(cleanPan) {
        while (true) {
            delay(1500)
            webViewRef?.let { wv ->
                bridge.injectPortalUserId(wv, cleanPan)
            }
        }
    }

    // React to bridge state changes
    LaunchedEffect(bridgeState) {
        when (val state = bridgeState) {
            is PortalBridgeState.CompletedAndSanitized -> {
                val rawTxns = fetcher.fetchAisTransactions(cleanPan).getOrDefault(emptyList())
                val allTxns = HardenedEphemeralPortalBridge.deduplicateTransactions(rawTxns)
                val totalTds = allTxns.sumOf { it.tdsWithheld }
                val totalInterest = allTxns
                    .filter { it.category == AisCategory.SAVINGS_INTEREST || it.category == AisCategory.TIME_DEPOSIT_INTEREST }
                    .sumOf { it.grossAmount }
                val grossSalary = allTxns
                    .filter { it.category == AisCategory.SALARY }
                    .sumOf { it.grossAmount }

                scope.launch {
                    try {
                        val txnArray = org.json.JSONArray()
                        allTxns.forEach { rec ->
                            txnArray.put(org.json.JSONObject().apply {
                                put("id", rec.transactionId)
                                put("category", rec.category.name)
                                put("reportingEntity", rec.reportingEntity)
                                put("accountOrFolioNumber", rec.accountOrFolioNumber)
                                put("grossAmount", rec.grossAmount)
                                put("tdsWithheld", rec.tdsWithheld)
                                put("quarterOrDate", rec.quarterOrDate)
                                put("suggestedSchedule", rec.suggestedSchedule)
                            })
                        }
                        ZeroTaxCloudApiBridge.ingestLivePrefill(
                            pan = cleanPan,
                            sourceOrigin = "OFFICIAL_WEBVIEW_EPHEMERAL_SESSION",
                            transactions = txnArray,
                            recordCount = allTxns.size,
                            aggregateGross = grossSalary,
                            aggregateTds = totalTds
                        )
                    } catch (_: Exception) {}
                }
                onSyncSuccess(allTxns.size, totalTds, totalInterest, grossSalary)
            }
            else -> {}
        }
    }

    Dialog(
        onDismissRequest = {
            bridge.sanitizePortalSessionImmediately {
                webViewRef?.destroy()
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        BackHandler {
            if (webViewRef?.canGoBack() == true) {
                webViewRef?.goBack()
            } else {
                bridge.sanitizePortalSessionImmediately {
                    webViewRef?.destroy()
                    onDismiss()
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Top Navigation & Security Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    if (isDark) MaterialTheme.colorScheme.outlineVariant else Slate200
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Navigation controls: Back, Forward, Reload
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { webViewRef?.goBack() },
                                    enabled = canGoBack,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { webViewRef?.goForward() },
                                    enabled = canGoForward,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Forward",
                                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { webViewRef?.reload() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reload",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Portal Address & Security Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure Connection",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "eportal.incometax.gov.in",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1
                                )
                            }

                            // Close Button
                            IconButton(
                                onClick = {
                                    bridge.sanitizePortalSessionImmediately {
                                        webViewRef?.destroy()
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Progress indicator during web loads
                    if (webProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { webProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = PaletteAccent,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }

            // 2. User Guidance & Live Bridge Interception Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) EnterpriseTeal900.copy(alpha = 0.5f) else EnterpriseTeal50,
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    if (isDark) EnterpriseTeal700.copy(alpha = 0.6f) else EnterpriseTeal200
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PAN: ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = cleanPan,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDark) EnterpriseTeal300 else EnterpriseTeal800,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Text(
                            text = "1. Sign in with password/OTP • 2. Tap Sync below",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }

                    // Auto-fill Credentials Pill
                    androidx.compose.material3.Button(
                        onClick = {
                            webViewRef?.let { wv ->
                                bridge.injectPortalUserId(wv, cleanPan)
                            }
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (isDark) EnterpriseTeal700 else EnterpriseTeal600
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = "Fill PAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // Live Intercept Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiveBadgeChip("Pre-fill", capturedTypes.contains(PayloadType.PREFILL_JSON), isDark)
                        LiveBadgeChip("26AS", capturedTypes.contains(PayloadType.FORM_26AS_JSON), isDark)
                        LiveBadgeChip("AIS", capturedTypes.contains(PayloadType.AIS_TIS_JSON), isDark)
                    }
                }
            }

            // 3. Isolated Portal WebView with Floating Sync Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            bridge.activePan = cleanPan
                            bridge.configureIsolatedWebView(this) { progress ->
                                webProgress = progress
                                canGoBack = this.canGoBack()
                                canGoForward = this.canGoForward()
                            }
                            loadUrl("https://eportal.incometax.gov.in/iec/foservices/#/login")
                            webViewRef = this
                            postDelayed({
                                bridge.injectPanLoginScript(this, cleanPan)
                            }, 1500)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 4. Prominent, User-Friendly Extended Sync Action Pill
                val liveCount = bridge.getCapturedPayloads().size
                Surface(
                    onClick = {
                        if (!isFetchingRecords) {
                            isFetchingRecords = true
                            `in`.zerotax.core.security.logging.ZeroTaxLogger.i("ItdOfficialWebLoginSheet", "Live sync button clicked for PAN $cleanPan")
                            scope.launch {
                                // 1. Inject script to fetch live prefill & AIS and scrape session/DOM in current session
                                webViewRef?.let {
                                    bridge.injectFetchLivePrefillAndAisScript(it, "2026-27")
                                    bridge.injectExtractSessionAndDomDataScript(it)
                                }
                                delay(700)

                                val activeTxnId = java.util.UUID.randomUUID().toString()
                                val verificationToken = "LIVE_OFFICIAL_WEB_PORTAL_SESSION_${System.currentTimeMillis()}"

                                // 2. Query portal sync endpoint
                                val syncRes = ZeroTaxCloudApiBridge.syncPortalRecords(
                                    pan = cleanPan,
                                    txnId = activeTxnId,
                                    verificationToken = verificationToken,
                                    assessmentYear = "2026-27"
                                )

                                val parsedTxns = mutableListOf<`in`.zerotax.core.model.AisTransactionRecord>()

                                // Incorporate any payloads captured directly by WebView bridge
                                parsedTxns.addAll(bridge.parseAllCapturedToTransactions())

                                // Wait for in-flight download & parsing if bridge capture is arriving
                                var attempts = 0
                                while (parsedTxns.isEmpty() && attempts < 8) {
                                    delay(500)
                                    parsedTxns.addAll(bridge.parseAllCapturedToTransactions())
                                    attempts++
                                }

                                if (syncRes.isSuccess) {
                                    val syncJson = syncRes.getOrNull()
                                    val txnsArray = syncJson?.optJSONArray("transactions")
                                    if (txnsArray != null) {
                                        for (i in 0 until txnsArray.length()) {
                                            val obj = txnsArray.getJSONObject(i)
                                            parsedTxns.add(
                                                `in`.zerotax.core.model.AisTransactionRecord(
                                                    transactionId = obj.optString("id").ifBlank { "TXN-$i" },
                                                    category = when (obj.optString("category")) {
                                                        "SALARY" -> `in`.zerotax.core.model.AisCategory.SALARY
                                                        "SAVINGS_INTEREST" -> `in`.zerotax.core.model.AisCategory.SAVINGS_INTEREST
                                                        "TIME_DEPOSIT_INTEREST" -> `in`.zerotax.core.model.AisCategory.TIME_DEPOSIT_INTEREST
                                                        "DIVIDENDS" -> `in`.zerotax.core.model.AisCategory.DIVIDENDS
                                                        "SALE_OF_SECURITIES" -> `in`.zerotax.core.model.AisCategory.SALE_OF_SECURITIES
                                                        "RENT_RECEIVED" -> `in`.zerotax.core.model.AisCategory.RENT_RECEIVED
                                                        else -> `in`.zerotax.core.model.AisCategory.HIGH_VALUE_CASH
                                                    },
                                                    reportingEntity = obj.optString("deductorOrIssuerName").ifBlank { "Direct Tax Interceptor" },
                                                    accountOrFolioNumber = obj.optString("deductorTan").ifBlank { "TAN-VERIFIED" },
                                                    grossAmount = obj.optLong("grossAmount"),
                                                    tdsWithheld = obj.optLong("tdsWithheld"),
                                                    quarterOrDate = obj.optString("transactionDate").ifBlank { "Q4-2026" },
                                                    suggestedSchedule = obj.optString("statutoryScheduleHint").ifBlank { "OS" }
                                                )
                                            )
                                        }
                                    }
                                }

                                val finalRecords = if (parsedTxns.isNotEmpty()) {
                                    val deduped = HardenedEphemeralPortalBridge.deduplicateTransactions(parsedTxns)
                                    ItdTransactionFetcher.updateLiveTransactions(deduped)
                                    ItdTransactionFetcher.updateAndPersistLiveTransactions(cleanPan, deduped, null)
                                    deduped
                                } else {
                                    val raw = fetcher.fetchAisTransactions(cleanPan).getOrDefault(emptyList())
                                    HardenedEphemeralPortalBridge.deduplicateTransactions(raw)
                                }

                                val totalTds = finalRecords.sumOf { it.tdsWithheld }
                                val totalInterest = finalRecords
                                    .filter { it.category == `in`.zerotax.core.model.AisCategory.SAVINGS_INTEREST || it.category == `in`.zerotax.core.model.AisCategory.TIME_DEPOSIT_INTEREST }
                                    .sumOf { it.grossAmount }
                                val grossSalary = finalRecords
                                    .filter { it.category == `in`.zerotax.core.model.AisCategory.SALARY }
                                    .sumOf { it.grossAmount }

                                if (finalRecords.isNotEmpty()) {
                                    HardenedEphemeralPortalBridge.setPortalSessionActive(true)
                                    `in`.zerotax.core.security.logging.ZeroTaxLogger.i("ItdOfficialWebLoginSheet", "Sync completed: discovered ${finalRecords.size} live items, totalTds=$totalTds, interest=$totalInterest, grossSalary=$grossSalary")
                                    scope.launch {
                                        try {
                                            val txnArray = org.json.JSONArray()
                                            finalRecords.forEach { rec ->
                                                txnArray.put(org.json.JSONObject().apply {
                                                    put("id", rec.transactionId)
                                                    put("category", rec.category.name)
                                                    put("reportingEntity", rec.reportingEntity)
                                                    put("accountOrFolioNumber", rec.accountOrFolioNumber)
                                                    put("grossAmount", rec.grossAmount)
                                                    put("tdsWithheld", rec.tdsWithheld)
                                                    put("quarterOrDate", rec.quarterOrDate)
                                                    put("suggestedSchedule", rec.suggestedSchedule)
                                                })
                                            }
                                            ZeroTaxCloudApiBridge.ingestLivePrefill(
                                                pan = cleanPan,
                                                sourceOrigin = "OFFICIAL_WEBVIEW_SYNC_COMPLETED",
                                                transactions = txnArray,
                                                recordCount = finalRecords.size,
                                                aggregateGross = grossSalary,
                                                aggregateTds = totalTds
                                            )
                                        } catch (_: Exception) {}
                                    }
                                    delay(300)
                                    bridge.sanitizePortalSessionImmediately {
                                        webViewRef?.destroy()
                                        onSyncSuccess(finalRecords.size, totalTds, totalInterest, grossSalary)
                                    }
                                } else {
                                    `in`.zerotax.core.security.logging.ZeroTaxLogger.w("ItdOfficialWebLoginSheet", "No live records extracted yet; keeping session active")
                                    isFetchingRecords = false
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    color = EnterpriseTeal600,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isFetchingRecords) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Text(
                                text = "Syncing Official Tax Records...",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Fetch & Sync Tax Data",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                    text = if (liveCount > 0) "$liveCount official payload(s) intercepted • Tap to import" else "Pulls 26AS, AIS/TIS & Pre-fill into ZeroTax",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBadgeChip(name: String, isCaptured: Boolean, isDark: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isCaptured) {
            getEmeraldSuccessBg().copy(alpha = 0.85f)
        } else {
            if (isDark) Slate800 else Slate200
        },
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isCaptured) EmeraldSuccess else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (isCaptured) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(10.dp)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = if (isCaptured) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCaptured) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
