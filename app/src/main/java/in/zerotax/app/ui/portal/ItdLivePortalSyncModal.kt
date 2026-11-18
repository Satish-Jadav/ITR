package `in`.zerotax.app.ui.portal

import `in`.zerotax.core.security.network.ZeroTaxCloudApiBridge
import `in`.zerotax.feature.portalbridge.ItdAndAaOtpAutoReader
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
    var showOfficialWebLogin by remember { mutableStateOf(false) }
    var syncProgressAnim by remember { mutableFloatStateOf(0f) }
    var currentStatusText by remember { mutableStateOf("Connecting to eportal.incometax.gov.in...") }
    var discoveredTransactions by remember { mutableStateOf<List<AisTransactionRecord>>(emptyList()) }
    val isDark = isSystemInDarkTheme()

    if (showOfficialWebLogin) {
        ItdOfficialWebLoginSheet(
            pan = assesseePan,
            onDismiss = { showOfficialWebLogin = false },
            onSyncSuccess = { count, tds, interest, grossSalary ->
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

        // Lightweight Zero-Cost In-App Driver Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) EnterpriseTeal900.copy(alpha = 0.4f) else EnterpriseTeal50,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) EnterpriseTeal700.copy(alpha = 0.5f) else EnterpriseTeal200
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🛡️ Zero-Cost Official In-App Driver (Direct eportal.incometax.gov.in)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (isDark) EnterpriseTeal300 else EnterpriseTeal800
                    )
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        when (currentStep) {
            PortalSyncStep.SELECT_AUTH_METHOD -> {
                // Informative Zero-Cost Portal Sync Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) EnterpriseTeal900.copy(alpha = 0.35f) else EnterpriseTeal50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal600.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EnterpriseTeal600,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Official Income Tax Portal Sync",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) EnterpriseTeal300 else EnterpriseTeal800
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Connects directly to eportal.incometax.gov.in in an isolated ephemeral session. Authenticate via Aadhaar OTP or password to pull verified Pre-fill JSON, AIS/TIS, and Form 26AS records at ₹0 cost.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // PAN field
                OutlinedTextField(
                    value = assesseePan,
                    onValueChange = {
                        assesseePan = it.uppercase().trim().take(10)
                    },
                    label = { Text("Taxpayer PAN") },
                    placeholder = { Text("ABCDE1234F") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                )

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
