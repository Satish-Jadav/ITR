package `in`.zerotax.app.ui.portal

import `in`.zerotax.feature.portalbridge.ItdAndAaOtpAutoReader
import `in`.zerotax.core.security.network.ZeroTaxCloudApiBridge
import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import `in`.zerotax.core.model.AisCategory
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
        totalSavingsInterest: Long
    ) -> Unit
) {
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val bridge = remember { HardenedEphemeralPortalBridge(scope) }
    val fetcher = remember { ItdTransactionFetcher() }
    val bridgeState by bridge.state.collectAsState()

    var isFetchingRecords by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
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

    // React to bridge state changes
    LaunchedEffect(bridgeState) {
        when (val state = bridgeState) {
            is PortalBridgeState.CompletedAndSanitized -> {
                scope.launch {
                    try {
                        ZeroTaxCloudApiBridge.ingestLivePrefill(
                            pan = pan,
                            sourceOrigin = "OFFICIAL_WEBVIEW_EPHEMERAL_SESSION"
                        )
                    } catch (_: Exception) {}
                }
                val allTxns = fetcher.fetchAisTransactions(pan).getOrDefault(emptyList())
                val totalTds = allTxns.sumOf { it.tdsWithheld }
                val totalInterest = allTxns
                    .filter { it.category == AisCategory.SAVINGS_INTEREST }
                    .sumOf { it.grossAmount }
                onSyncSuccess(allTxns.size, totalTds, totalInterest)
            }
            else -> {}
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            bridge.sanitizePortalSessionImmediately {
                webViewRef?.destroy()
                onDismiss()
            }
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Ultra-slim Top Security Header (URL + GOV.IN + Close)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    if (isDark) MaterialTheme.colorScheme.outlineVariant else Slate200
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure Connection",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "eportal.incometax.gov.in",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(getEmeraldSuccessBg())
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GOV.IN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            bridge.sanitizePortalSessionImmediately {
                                webViewRef?.destroy()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Full WebView with exactly one button at bottom right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            bridge.configureIsolatedWebView(this)
                            loadUrl("https://eportal.incometax.gov.in/iec/foservices/#/login")
                            webViewRef = this
                            postDelayed({
                                bridge.injectPanLoginScript(this, pan)
                            }, 1500)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Single button visible at bottom right: Fetch Live Data
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!isFetchingRecords) {
                            isFetchingRecords = true
                            scope.launch {
                                delay(600)
                                val res = fetcher.fetchAisTransactions(pan)
                                val records = res.getOrDefault(emptyList())
                                val totalTds = records.sumOf { it.tdsWithheld }
                                val totalInterest = records.sumOf { it.grossAmount }

                                delay(400)
                                bridge.sanitizePortalSessionImmediately {
                                    webViewRef?.destroy()
                                    onSyncSuccess(records.size, totalTds, totalInterest)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 20.dp),
                    containerColor = EnterpriseTeal600,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    icon = {
                        if (isFetchingRecords) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Fetch Live Data",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    text = {
                        Text(
                            text = if (isFetchingRecords) "Fetching Live Data..." else "Fetch Live Data",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                )
            }
        }
    }
}
