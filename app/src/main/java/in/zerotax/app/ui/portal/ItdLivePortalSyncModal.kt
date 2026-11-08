package `in`.zerotax.app.ui.portal

import `in`.zerotax.core.security.network.ZeroTaxCloudApiBridge
import org.json.JSONObject
import org.json.JSONArray

import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.AisCategory
import `in`.zerotax.core.model.AisTransactionRecord
import `in`.zerotax.core.model.Rupee
import `in`.zerotax.feature.portalbridge.ItdTransactionFetcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PortalSyncStep {
    SELECT_AUTH_METHOD,
    CONNECTING_TLS,
    FETCHING_DATA,
    SYNC_SUCCESS
}

/**
 * 100% In-App Income Tax Department (ITD) Live Sync Modal.
 * Syncs Form 26AS, AIS/TIS, Pre-fill JSON, and Refund Status directly in-app.
 *
 * 100% Zero-Password Flow:
 * Never requests the taxpayer's e-filing portal password.
 * Supports Instant Aadhaar OTP (no password needed) or Official Web Portal Login
 * where the user logs in directly on the official CBDT portal in an isolated session.
 */
@Composable
fun ItdLivePortalSyncModal(
    pan: String,
    onDismiss: () -> Unit,
    onSyncCompleted: (
        syncedTransactionsCount: Int,
        totalTdsDiscovered: Long,
        totalSavingsInterest: Long,
        discoveredGrossSalary: Long
    ) -> Unit
) {
    val scope = rememberCoroutineScope()
    val fetcher = remember { ItdTransactionFetcher() }
    var assesseePan by remember(pan) { mutableStateOf(pan.trim().uppercase()) }

    var currentStep by remember { mutableStateOf(PortalSyncStep.SELECT_AUTH_METHOD) }
    var authMode by remember { mutableStateOf("AADHAAR_OTP") } // AADHAAR_OTP or OFFICIAL_WEB
    var aadhaarOtp by remember { mutableStateOf("") }
    var showOfficialWebLogin by remember { mutableStateOf(false) }
    var syncProgressAnim by remember { mutableFloatStateOf(0f) }
    var currentStatusText by remember { mutableStateOf("Connecting to eportal.incometax.gov.in...") }
    var discoveredTransactions by remember { mutableStateOf<List<AisTransactionRecord>>(emptyList()) }
    val isDark = isSystemInDarkTheme()

    var isOtpDispatched by remember { mutableStateOf(false) }
    var isGeneratingOtp by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableIntStateOf(0) }
    var maskedMobileDisplay by remember { mutableStateOf("") }
    var otpErrorMessage by remember { mutableStateOf<String?>(null) }
    var activeTxnId by remember { mutableStateOf("") }
    var activeDriverName by remember { mutableStateOf("Zero-Cost In-App Live Driver") }
    var activeDriverType by remember { mutableStateOf("IN_APP_LIVE_DRIVER") }

    LaunchedEffect(Unit) {
        val statusRes = ZeroTaxCloudApiBridge.fetchPortalDriverStatus()
        if (statusRes.isSuccess) {
            val json = statusRes.getOrNull()
            val driver = json?.optString("activeDriver") ?: "IN_APP_LIVE_DRIVER"
            activeDriverType = driver
            activeDriverName = when (driver) {
                "QUICKO_ERI" -> "Quicko B2B ERI Gateway (Sandbox.co.in)"
                "CBDT_DIRECT_ERI" -> "Official CBDT IEC 2.0 Gateway"
                else -> "Zero-Cost In-App Live Driver"
            }
        }
    }

    val context = LocalContext.current
    val otpReader = remember {
        ItdAndAaOtpAutoReader(context) { code ->
            aadhaarOtp = code
        }
    }

    DisposableEffect(isOtpDispatched) {
        if (isOtpDispatched) {
            otpReader.startListening()
        }
        onDispose {
            otpReader.stopListening()
        }
    }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds--
        }
    }

    val executeVerificationAndSync: () -> Unit = {
        if (aadhaarOtp.length == 6 && assesseePan.length == 10 && currentStep == PortalSyncStep.SELECT_AUTH_METHOD && !isGeneratingOtp) {
            currentStep = PortalSyncStep.CONNECTING_TLS
            scope.launch {
                syncProgressAnim = 0.25f
                currentStatusText = "Verifying Aadhaar OTP u/s 140A..."
                val verifyRes = ZeroTaxCloudApiBridge.verifyPortalOtp(assesseePan, activeTxnId, aadhaarOtp)
                if (verifyRes.isFailure) {
                    currentStep = PortalSyncStep.SELECT_AUTH_METHOD
                    otpErrorMessage = verifyRes.exceptionOrNull()?.message ?: "Invalid OTP entered."
                    return@launch
                }

                val verifyJson = verifyRes.getOrNull()
                val verificationToken = verifyJson?.optString("verificationToken").orEmpty()

                currentStep = PortalSyncStep.FETCHING_DATA
                syncProgressAnim = 0.60f
                currentStatusText = "Downloading Form 26AS & AIS/TIS Records..."

                val syncRes = ZeroTaxCloudApiBridge.syncPortalRecords(assesseePan, activeTxnId, verificationToken, "2026-27")
                if (syncRes.isSuccess) {
                    val syncJson = syncRes.getOrNull()
                    val txnsArray = syncJson?.optJSONArray("transactions")
                    val parsedTxns = mutableListOf<AisTransactionRecord>()
                    if (txnsArray != null) {
                        for (i in 0 until txnsArray.length()) {
                            val obj = txnsArray.getJSONObject(i)
                            parsedTxns.add(
                                AisTransactionRecord(
                                    transactionId = obj.optString("id"),
                                    category = when (obj.optString("category")) {
                                        "SAVINGS_INTEREST" -> AisCategory.SAVINGS_INTEREST
                                        "TIME_DEPOSIT_INTEREST" -> AisCategory.TIME_DEPOSIT_INTEREST
                                        "DIVIDENDS" -> AisCategory.DIVIDENDS
                                        "SALE_OF_SECURITIES" -> AisCategory.SALE_OF_SECURITIES
                                        "RENT_RECEIVED" -> AisCategory.RENT_RECEIVED
                                        else -> AisCategory.HIGH_VALUE_CASH
                                    },
                                    reportingEntity = obj.optString("deductorOrIssuerName"),
                                    accountOrFolioNumber = obj.optString("deductorTan"),
                                    grossAmount = obj.optLong("grossAmount"),
                                    tdsWithheld = obj.optLong("tdsWithheld"),
                                    quarterOrDate = obj.optString("transactionDate"),
                                    suggestedSchedule = obj.optString("statutoryScheduleHint")
                                )
                            )
                        }
                    }
                    ItdTransactionFetcher.updateLiveTransactions(parsedTxns)
                    discoveredTransactions = parsedTxns

                    syncProgressAnim = 0.90f
                    currentStatusText = "Reconciling Pre-fill Data & Bank Details..."
                    delay(400)

                    syncProgressAnim = 1.0f
                    currentStep = PortalSyncStep.SYNC_SUCCESS
                } else {
                    currentStep = PortalSyncStep.SELECT_AUTH_METHOD
                    otpErrorMessage = syncRes.exceptionOrNull()?.message ?: "Portal sync failed."
                }
            }
        }
    }

    // Automated Zero-Click Submission: As soon as SMS Retriever or manual typing captures the 6-digit OTP, auto-trigger verify & sync
    LaunchedEffect(aadhaarOtp) {
        if (aadhaarOtp.length == 6 && isOtpDispatched && currentStep == PortalSyncStep.SELECT_AUTH_METHOD) {
            executeVerificationAndSync()
        }
    }

    if (showOfficialWebLogin) {
        ItdOfficialWebLoginSheet(
            pan = assesseePan,
            onDismiss = { showOfficialWebLogin = false },
            onSyncSuccess = { count, tds, interest ->
                showOfficialWebLogin = false
                scope.launch {
                    val result = fetcher.fetchAisTransactions(assesseePan)
                    discoveredTransactions = result.getOrDefault(emptyList())
                    currentStep = PortalSyncStep.SYNC_SUCCESS
                }
            }
        )
    }

    ZeroTaxBottomSheet(
        onDismissRequest = {
            if (currentStep != PortalSyncStep.FETCHING_DATA && currentStep != PortalSyncStep.CONNECTING_TLS) {
                onDismiss()
            }
        }
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) EnterpriseTeal900 else EnterpriseTeal100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = if (isDark) EnterpriseTeal300 else EnterpriseTeal700,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Live ITD Portal Sync",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        "Direct In-App Sync • Zero Password Stored",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Active Driver Indicator Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (activeDriverType == "QUICKO_ERI") {
                if (isDark) EnterpriseTeal900.copy(alpha = 0.4f) else EnterpriseTeal50
            } else if (activeDriverType == "CBDT_DIRECT_ERI") {
                if (isDark) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFD1FAE5)
            } else {
                if (isDark) MaterialTheme.colorScheme.surfaceVariant else Slate100
            },
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (activeDriverType == "QUICKO_ERI") EnterpriseTeal600.copy(alpha = 0.5f)
                else if (activeDriverType == "CBDT_DIRECT_ERI") Color(0xFF10B981).copy(alpha = 0.5f)
                else if (isDark) MaterialTheme.colorScheme.outlineVariant else Slate300
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when (activeDriverType) {
                        "QUICKO_ERI" -> "⚡ Quicko B2B ERI Gateway (Sandbox.co.in)"
                        "CBDT_DIRECT_ERI" -> "🏛️ Official CBDT IEC 2.0 Gateway"
                        else -> "🛡️ Zero-Cost In-App Live Driver (WebView + Local Extraction)"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (activeDriverType == "QUICKO_ERI") {
                            if (isDark) EnterpriseTeal300 else EnterpriseTeal700
                        } else if (activeDriverType == "CBDT_DIRECT_ERI") {
                            if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                        } else {
                            if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Slate700
                        }
                    )
                )
            }
        }

        Spacer(Modifier.height(14.dp))

                // Method Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (authMode == "AADHAAR_OTP") (if (isDark) EnterpriseTeal900.copy(alpha = 0.35f) else EnterpriseTeal50) else (if (isDark) MaterialTheme.colorScheme.surfaceVariant else Slate100),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (authMode == "AADHAAR_OTP") EnterpriseTeal600 else if (isDark) MaterialTheme.colorScheme.outlineVariant else Slate300
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { authMode = "AADHAAR_OTP" }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = authMode == "AADHAAR_OTP",
                                    onClick = { authMode = "AADHAAR_OTP" },
                                    colors = RadioButtonDefaults.colors(selectedColor = EnterpriseTeal600)
                                )
                                Text("Aadhaar OTP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(
                                "Instant sync without portal password",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (authMode == "OFFICIAL_WEB") (if (isDark) EnterpriseTeal900.copy(alpha = 0.35f) else EnterpriseTeal50) else (if (isDark) MaterialTheme.colorScheme.surfaceVariant else Slate100),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (authMode == "OFFICIAL_WEB") EnterpriseTeal600 else if (isDark) MaterialTheme.colorScheme.outlineVariant else Slate300
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { authMode = "OFFICIAL_WEB" }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = authMode == "OFFICIAL_WEB",
                                    onClick = { authMode = "OFFICIAL_WEB" },
                                    colors = RadioButtonDefaults.colors(selectedColor = EnterpriseTeal600)
                                )
                                Text("Web Portal", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(
                                "Official isolated CBDT login sheet",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // PAN field
                OutlinedTextField(
                    value = assesseePan,
                    onValueChange = {
                        assesseePan = it.uppercase().trim().take(10)
                        if (isOtpDispatched) {
                            isOtpDispatched = false
                            aadhaarOtp = ""
                            cooldownSeconds = 0
                        }
                    },
                    label = { Text("Taxpayer PAN") },
                    placeholder = { Text("ABCDE1234F") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                )

                if (authMode == "AADHAAR_OTP") {
                    Spacer(Modifier.height(10.dp))
                    if (!isOtpDispatched) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) EnterpriseTeal900.copy(alpha = 0.25f) else EnterpriseTeal50,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, EnterpriseTeal300),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = EnterpriseTeal600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Tap 'Generate Aadhaar OTP' to dispatch a 6-digit one-time password to your Aadhaar-registered mobile number.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        if (otpErrorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = otpErrorMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = AmberWarning, fontSize = 11.sp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        ZeroTaxPrimaryButton(
                            text = "Generate Aadhaar OTP",
                            onClick = {
                                if (assesseePan.length == 10 && (assesseePan[3] == 'P' || assesseePan[3] == 'H')) {
                                    isGeneratingOtp = true
                                    otpErrorMessage = null
                                    scope.launch {
                                        val reqRes = ZeroTaxCloudApiBridge.requestPortalOtp(assesseePan, "LOGIN_PORTAL")
                                        isGeneratingOtp = false
                                        if (reqRes.isSuccess) {
                                            val json = reqRes.getOrNull()
                                            activeTxnId = json?.optString("txnId").orEmpty()
                                            maskedMobileDisplay = json?.optString("maskedMobile") ?: ("••••••" + assesseePan.takeLast(4))
                                            isOtpDispatched = true
                                            cooldownSeconds = json?.optInt("cooldownSeconds") ?: 60
                                        } else {
                                            otpErrorMessage = reqRes.exceptionOrNull()?.message ?: "Failed to dispatch OTP. Please check PAN."
                                        }
                                    }
                                } else {
                                    otpErrorMessage = "Please enter a valid 10-character PAN (e.g. ABCDE1234F)"
                                }
                            },
                            isLoading = isGeneratingOtp,
                            enabled = assesseePan.length == 10 && !isGeneratingOtp,
                            leadingIcon = {
                                Icon(Icons.Default.Send, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            }
                        )
                    } else {
                        // OTP is dispatched!
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) EmeraldSuccessBgDark else EmeraldSuccessBg,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "6-digit OTP sent to Aadhaar-registered mobile ($maskedMobileDisplay)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) EmeraldSuccess else EmeraldDark)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = aadhaarOtp,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) aadhaarOtp = it },
                            label = { Text("Enter 6-Digit OTP") },
                            placeholder = { Text("••••••") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 4.sp)
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (cooldownSeconds > 0) {
                                Text(
                                    text = "Resend OTP in " + cooldownSeconds + "s",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Resend OTP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EnterpriseTeal600,
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            isGeneratingOtp = true
                                            val reqRes = ZeroTaxCloudApiBridge.requestPortalOtp(assesseePan, "LOGIN_PORTAL")
                                            isGeneratingOtp = false
                                            if (reqRes.isSuccess) {
                                                val json = reqRes.getOrNull()
                                                activeTxnId = json?.optString("txnId").orEmpty()
                                                cooldownSeconds = json?.optInt("cooldownSeconds") ?: 60
                                            } else {
                                                otpErrorMessage = reqRes.exceptionOrNull()?.message ?: "Failed to resend OTP."
                                            }
                                        }
                                    }
                                )
                            }

                            Text(
                                text = "Change PAN",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable {
                                    isOtpDispatched = false
                                    aadhaarOtp = ""
                                    cooldownSeconds = 0
                                }
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        ZeroTaxPrimaryButton(
                            text = "Verify OTP & Sync Portal Records",
                            onClick = executeVerificationAndSync,
                            enabled = aadhaarOtp.length == 6 && assesseePan.length == 10
                        )
                    }
                } else {
                    Spacer(Modifier.height(16.dp))
                    ZeroTaxPrimaryButton(
                        text = "Open Official Web Login",
                        onClick = { showOfficialWebLogin = true },
                        enabled = assesseePan.length == 10,
                        leadingIcon = {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            PortalSyncStep.CONNECTING_TLS, PortalSyncStep.FETCHING_DATA -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = EnterpriseTeal600,
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = currentStatusText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { syncProgressAnim },
                        modifier = Modifier.fillMaxWidth(0.8f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = EnterpriseTeal600
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hardware-secured ephemeral session • Nonce pinned to CBDT gateway",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PortalSyncStep.SYNC_SUCCESS -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) EmeraldSuccessBgDark else EmeraldSuccessBg,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, EmeraldSuccess),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Form 26AS & AIS Records Synced",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) EmeraldSuccess else EmeraldDark
                                )
                                Text(
                                    "Zero password requested. Return auto-populated.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = EmeraldDark, fontSize = 11.sp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text("Discovered Records from ITD Portal:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))

                    discoveredTransactions.take(3).forEach { txn ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(txn.reportingEntity, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(txn.suggestedSchedule, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                            Text("₹" + Rupee.formatInr(txn.grossAmount), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EnterpriseTeal700)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    ZeroTaxPrimaryButton(
                        text = "Apply to My Return",
                        onClick = {
                            val totalTds = discoveredTransactions.sumOf { it.tdsWithheld }
                            val totalInterest = discoveredTransactions
                                .filter { it.category == AisCategory.SAVINGS_INTEREST }
                                .sumOf { it.grossAmount }
                            val grossSalary = discoveredTransactions
                                .filter { it.suggestedSchedule.contains("Salary", ignoreCase = true) }
                                .sumOf { it.grossAmount }
                            onSyncCompleted(discoveredTransactions.size, totalTds, totalInterest, grossSalary)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
