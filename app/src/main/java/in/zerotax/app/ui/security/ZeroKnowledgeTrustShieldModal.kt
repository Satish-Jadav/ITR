package `in`.zerotax.app.ui.security

import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.ZeroTaxBottomSheet
import `in`.zerotax.app.ui.components.BadgeTone
import `in`.zerotax.app.ui.components.StatutoryBadge
import `in`.zerotax.app.ui.theme.*

/**
 * Zero-Knowledge Trust Shield Modal & Transparency Agreement.
 * Provides explicit, verifiable assurance under Section 6 of the Digital Personal Data Protection
 * (DPDP) Act, 2023 that ZeroTax servers have ZERO access to taxpayer financial data.
 *
 * Adheres strictly to Zero-Emoji design rules using Compose Vector Icons.
 */
@Composable
fun ZeroKnowledgeTrustShieldModal(
    onDismiss: () -> Unit,
    onAcknowledge: () -> Unit = onDismiss,
    modifier: Modifier = Modifier
) {
    var dpdpConsentGiven by remember { mutableStateOf(true) }

    ZeroTaxBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = EnterpriseTeal700,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "End-to-End Encrypted",
                        style = TextStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    )
                    Text(
                        "WhatsApp-Grade Zero-Knowledge Privacy Guarantee",
                        style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Text(
                    text = "As a modern privacy-first tax technology, ZeroTax is built on a mathematical zero-knowledge principle: We cannot see, store, or sell your financial data even if we wanted to.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
                )

                // Side-by-side Transparency Matrix
                // 1. WHAT ZEROTAX SERVERS CAN SEE
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSystemInDarkTheme()) EmeraldSuccessBgDark else EmeraldSuccessBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "What ZeroTax Cloud Servers Can See: ZERO UNENCRYPTED DATA",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isSystemInDarkTheme()) Color(0xFF6EE7B7) else EmeraldDark)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• All financial numbers, salary, bank accounts, and PAN are AES-256-GCM encrypted locally before cloud backup.\n" +
                                   "• Zero telemetry or third-party behavioral ad trackers.\n" +
                                   "• Zero unencrypted remote server databases storing your tax draft.",
                            style = TextStyle(fontSize = 11.sp, color = if (isSystemInDarkTheme()) Slate200 else Slate700, lineHeight = 16.sp)
                        )
                    }
                }

                // 2. WHAT STAYS HARDWARE-ENCRYPTED ON YOUR DEVICE
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EnterpriseTeal50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal300),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = EnterpriseTeal700, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "What Stays On-Device & Hardware Encrypted",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isSystemInDarkTheme()) EnterpriseTeal200 else EnterpriseTeal900)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• Permanent Account Number (PAN) & Aadhaar Masked VID\n" +
                                   "• Form 16, Bank Interest, AIS/26AS tax credits\n" +
                                   "• Encrypted under AES-256-GCM via Android StrongBox KeyStore\n" +
                                   "• Protected by device biometric signature (Fingerprint / Face ID).",
                            style = TextStyle(fontSize = 11.sp, color = if (isSystemInDarkTheme()) Slate200 else Slate700, lineHeight = 16.sp)
                        )
                    }
                }

                // 3. SHARING WITH YOUR CA / AUDITOR
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "When Sharing with Your CA",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "A single-use 6-digit pairing code securely encrypts your audit dossier. Only your designated CA can decrypt it for review and sign-off.",
                            style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                        )
                    }
                }

                // 4. STATUTORY CONSENT & ACKNOWLEDGEMENT
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().clickable { dpdpConsentGiven = !dpdpConsentGiven }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = dpdpConsentGiven,
                            onCheckedChange = { dpdpConsentGiven = it },
                            colors = CheckboxDefaults.colors(checkedColor = EnterpriseTeal600)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "I acknowledge that my financial returns are computed locally with end-to-end encrypted cloud backup, and ZeroTax does not store unencrypted tax records.",
                            style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 14.sp)
                        )
                    }
                }
            }
        Spacer(Modifier.height(16.dp))
        ZeroTaxPrimaryButton(
            text = "I Acknowledge & Trust",
            onClick = {
                if (dpdpConsentGiven) {
                    onAcknowledge()
                }
            },
            enabled = dpdpConsentGiven,
            leadingIcon = {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
            }
        )
    }
}

/**
 * Compact inline trust badge for screen headers and filing steps.
 */
@Composable
fun ZeroKnowledgeTrustBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = EnterpriseTeal50,
        border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal200),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = EnterpriseTeal700, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("Zero-Knowledge Local + Cloud", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal900))
        }
    }
}

