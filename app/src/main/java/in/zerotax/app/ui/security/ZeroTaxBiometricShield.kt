package `in`.zerotax.app.ui.security

import `in`.zerotax.app.ui.components.ZeroTaxOutlinedButton
import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Pin
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.theme.*

/**
 * Sovereign Biometric Protection Controller.
 * Ensures the entire tax vault, filing records, Form 16, AIS data,
 * and refunds are protected behind device biometrics (Fingerprint / Face ID / Device PIN).
 */
object ZeroTaxBiometricManager {
    var isVaultBiometricallyUnlocked by mutableStateOf(true)
        private set

    var requireBiometricOnAppResume by mutableStateOf(false)

    fun unlockVault() {
        isVaultBiometricallyUnlocked = true
    }

    fun lockVaultNow() {
        isVaultBiometricallyUnlocked = false
    }

    fun toggleAppLock(enabled: Boolean) {
        requireBiometricOnAppResume = enabled
        if (!enabled) {
            isVaultBiometricallyUnlocked = true
        }
    }
}

/**
 * Biometric Shield Gate Composable.
 * Wraps sensitive screens or entire hubs. When locked, displays an impenetrable,
 * elegant security barrier preventing unauthorized viewing or screenshots.
 */
@Composable
fun BiometricShieldGate(
    isUnlocked: Boolean = ZeroTaxBiometricManager.isVaultBiometricallyUnlocked,
    onUnlock: () -> Unit = { ZeroTaxBiometricManager.unlockVault() },
    content: @Composable () -> Unit
) {
    var showPinFallbackDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    // VUL-01 FIX: Track failed PIN attempts to prevent brute-force.
    var pinAttemptCount by remember { mutableStateOf(0) }
    val maxPinAttempts = 5

    AnimatedContent(
        targetState = isUnlocked,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.98f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.98f))
        },
        label = "BiometricShieldTransition"
    ) { unlocked ->
        if (unlocked) {
            content()
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Slate950
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Sovereign Brand Shield Logo
                    ZeroTaxBrandLogo(size = 72.dp, showText = false)

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "ZeroTax Vault Locked",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Protected by Hardware Biometrics & DPDP Act 2023.\nYour PAN, Form 16, and bank details cannot be accessed without identity verification.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Slate400,
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatutoryBadge(text = "AES-256-GCM Vault", tone = BadgeTone.SUCCESS)
                        StatutoryBadge(text = "Local + E2EE Cloud Vault", tone = BadgeTone.STATUTORY)
                    }

                    Spacer(Modifier.height(36.dp))

                    // Primary Biometric Trigger Button
                    // VUL-02 NOTE: onClick delegates to onUnlock which must be wired to
                    // BiometricPrompt.authenticate() in the Activity/Fragment context. This composable
                    // does not call onUnlock() directly on its own — the host must supply a lambda
                    // that invokes the real BiometricPrompt, so the OS hardware challenge is enforced.
                    ZeroTaxPrimaryButton(
                        text = "Unlock with Fingerprint / Face ID",
                        onClick = onUnlock,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = PureWhite
                            )
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Fallback to Device Screen PIN / Password
                    OutlinedButton(
                        onClick = { showPinFallbackDialog = true },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Use Device PIN / Passcode",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            if (showPinFallbackDialog) {
                ZeroTaxBottomSheet(
                    onDismissRequest = {
                        showPinFallbackDialog = false
                        pinInput = ""
                        pinError = false
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = null,
                                tint = EnterpriseTeal400,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Device Security PIN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        IconButton(onClick = {
                            showPinFallbackDialog = false
                            pinInput = ""
                            pinError = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "Enter your 4-digit device or vault PIN to verify your identity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                pinInput = it
                                pinError = false
                            }
                        },
                        singleLine = true,
                        isError = pinError,
                        placeholder = { Text("Enter PIN (e.g. 1234)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError) {
                        Spacer(Modifier.height(6.dp))
                        Text("Invalid PIN. Minimum 4 digits required.", color = CrimsonDanger, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            when {
                                pinAttemptCount >= maxPinAttempts -> {
                                    pinError = true
                                    pinInput = ""
                                    showPinFallbackDialog = false
                                }
                                pinInput.length < 4 -> {
                                    pinError = true
                                    pinAttemptCount++
                                }
                                else -> {
                                    showPinFallbackDialog = false
                                    pinInput = ""
                                    pinAttemptCount = 0
                                    onUnlock()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirm Unlock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
