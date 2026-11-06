package `in`.zerotax.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.security.ZeroKnowledgeTrustShieldModal
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.UserAuthProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(
    onAuthenticationSuccess: (UserAuthProfile) -> Unit,
    onSignInWithEmail: (email: String, password: String, isSignUp: Boolean, name: String) -> Unit,
    onSignInWithGoogle: (email: String, name: String) -> Unit,
    onBiometricUnlock: () -> Unit,
    onLaunchCaPortal: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var showCaSection by remember { mutableStateOf(false) }
    var showTrustShieldModal by remember { mutableStateOf(false) }
    var showGoogleAccountPicker by remember { mutableStateOf(false) }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var fullNameInput by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var caMembershipInput by remember { mutableStateOf("") }
    var caPasswordInput by remember { mutableStateOf("••••••••") }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Trust Pill Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSystemInDarkTheme()) Color(0xFF232014) else Color(0xFFFEF9C3),
                border = androidx.compose.foundation.BorderStroke(0.75.dp, if (isSystemInDarkTheme()) Color(0xFF713F12) else Color(0xFFFDE047)),
                modifier = Modifier.clickable { showTrustShieldModal = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFD97706),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "🔒 Local Privacy + Encrypted Cloud Vault",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSystemInDarkTheme()) Color(0xFFFDE68A) else Color(0xFF713F12)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Brand & Welcome Title
            ZeroTaxBrandLogo(size = 64.dp, showText = false)

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Zero",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = "Tax",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = EnterpriseTeal600,
                        letterSpacing = (-0.5).sp
                    )
                )
            }

            Text(
                text = "File your tax return in 3 simple steps",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "No legal jargon. We calculate your highest refund automatically.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Step-by-Step Overview (Progressive Disclosure Cards)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StepPreviewCard(
                    stepNumber = "1",
                    icon = Icons.Default.Badge,
                    title = "Your Basic Details",
                    description = "Enter your PAN to check your eligible tax slabs."
                )
                StepPreviewCard(
                    stepNumber = "2",
                    icon = Icons.Default.Description,
                    title = "Upload Form 16 or Salary",
                    description = "Auto-extracts your earnings and TDS in seconds."
                )
                StepPreviewCard(
                    stepNumber = "3",
                    icon = Icons.Default.AutoGraph,
                    title = "Compare & Maximize Refund",
                    description = "Picks Old vs New Regime to save you the most tax."
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Primary: Google One-Tap Sign In
            ZeroTaxPrimaryButton(
                text = "Continue with Google",
                onClick = { showGoogleAccountPicker = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Divider: OR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "  OR USE EMAIL  ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Mandatory Email/Password Authentication Card
            ZeroTaxCard(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Text(
                    text = if (isSignUpMode) "Create ZeroTax Account" else "Sign In to Your Account",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Your data will be End-to-End Encrypted and synced to the cloud API.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(Modifier.height(12.dp))

                if (isSignUpMode) {
                    ZeroTaxTextField(
                        value = fullNameInput,
                        onValueChange = { fullNameInput = it },
                        label = "Full Name"
                    )
                    Spacer(Modifier.height(8.dp))
                }

                ZeroTaxTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = "Email Address",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(Modifier.height(8.dp))

                ZeroTaxTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = "Password",
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    }
                )

                Spacer(Modifier.height(14.dp))

                ZeroTaxPrimaryButton(
                    text = if (isSignUpMode) "Register & Sync Vault" else "Sign In & Sync Vault",
                    onClick = {
                        onSignInWithEmail(emailInput, passwordInput, isSignUpMode, fullNameInput)
                    },
                    enabled = emailInput.isNotBlank() && passwordInput.isNotBlank() && !isLoading
                )

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = { isSignUpMode = !isSignUpMode },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account? Sign In" else "New to ZeroTax? Create Account",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = EnterpriseTeal600,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Error display if any
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CA / Auditor link
            TextButton(
                onClick = { showCaSection = !showCaSection }
            ) {
                Text(
                    text = if (showCaSection) "Hide CA Portal" else "Looking for Tax Auditor / CA Portal?",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            // Expandable CA Auditor Portal Login Form
            AnimatedVisibility(visible = showCaSection) {
                val isDark = isSystemInDarkTheme()
                val caHeaderColor = if (isDark) IndigoStatutoryDark else IndigoStatutory
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) IndigoStatutoryBgDark.copy(alpha = 0.6f) else IndigoStatutoryBg.copy(alpha = 0.6f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoStatutory.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = caHeaderColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Chartered Accountant / Auditor Portal", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = caHeaderColor))
                        }
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = caMembershipInput,
                            onValueChange = { caMembershipInput = it },
                            label = { Text("ICAI 6-Digit Membership No.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = caPasswordInput,
                            onValueChange = { caPasswordInput = it },
                            label = { Text("Auditor Portal Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = onLaunchCaPortal,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoStatutory)
                        ) {
                            Text("Launch CA Auditor Suite")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy and offline assurance badge at bottom
            Text(
                text = "Zero-Knowledge Encryption • Local Compute + E2EE Cloud Vault",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Trust Shield Modal
        if (showTrustShieldModal) {
            ZeroKnowledgeTrustShieldModal(onDismiss = { showTrustShieldModal = false })
        }

        // Google Account Picker Sheet
        if (showGoogleAccountPicker) {
            ZeroTaxBottomSheet(
                onDismissRequest = { showGoogleAccountPicker = false }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = EnterpriseTeal600,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In with Google", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { showGoogleAccountPicker = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Choose a Google account to securely backup your tax drafts with zero-knowledge encryption:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showGoogleAccountPicker = false
                            onSignInWithGoogle("taxpayer@gmail.com", "Taxpayer")
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EnterpriseTeal100),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("T", fontWeight = FontWeight.Bold, color = EnterpriseTeal700, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Taxpayer", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("taxpayer@gmail.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = { showGoogleAccountPicker = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Clean visual card for previewing each step in a friendly, approachable way.
 */
@Composable
private fun StepPreviewCard(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    ZeroTaxCard(
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EnterpriseTeal50),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EnterpriseTeal700,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Step " + stepNumber + ": ",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = EnterpriseTeal700
                    )
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
