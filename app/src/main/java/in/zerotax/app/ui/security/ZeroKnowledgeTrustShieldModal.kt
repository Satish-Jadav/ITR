package `in`.zerotax.app.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        title = {
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
                        "Zero-Knowledge Hardware Vault",
                        style = TextStyle(fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                    )
                    Text(
                        "100% On-Device End-to-End Encryption Guarantee",
                        style = TextStyle(fontSize = 11.sp, color = Slate500)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "As a modern privacy-first tax technology, ZeroTax is built on a mathematical zero-knowledge principle: We cannot see, store, or sell your financial data even if we wanted to.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate700, fontSize = 12.sp, lineHeight = 16.sp)
                )

                // Side-by-side Transparency Matrix
                // 1. WHAT ZEROTAX SERVERS CAN SEE
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldSuccessBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "What ZeroTax Servers Can See: ABSOLUTELY NOTHING",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate900)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• Zero financial numbers, salary, bank accounts, or PAN sent to our cloud.\n" +
                                   "• Zero telemetry or third-party behavioral ad trackers.\n" +
                                   "• Zero remote server databases storing your tax draft.",
                            style = TextStyle(fontSize = 11.sp, color = Slate700, lineHeight = 16.sp)
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
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EnterpriseTeal900)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• Permanent Account Number (PAN) & Aadhaar Masked VID\n" +
                                   "• Form 16, Bank Interest, AIS/26AS tax credits\n" +
                                   "• Encrypted under AES-256-GCM via Android StrongBox KeyStore\n" +
                                   "• Protected by device biometric signature (Fingerprint / Face ID).",
                            style = TextStyle(fontSize = 11.sp, color = Slate700, lineHeight = 16.sp)
                        )
                    }
                }

                // 3. SHARING WITH YOUR CA / AUDITOR
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "When Sharing with Your CA",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate900)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "A single-use 6-digit pairing code securely encrypts your audit dossier. Only your designated CA can decrypt it for review and sign-off.",
                            style = TextStyle(fontSize = 11.sp, color = Slate600, lineHeight = 15.sp)
                        )
                    }
                }

                // 4. STATUTORY CONSENT & ACKNOWLEDGEMENT
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate100,
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
                            text = "I acknowledge that my financial returns are encrypted 100% on my device and ZeroTax does not possess or store my unencrypted tax records.",
                            style = TextStyle(fontSize = 10.sp, color = Slate800, lineHeight = 14.sp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dpdpConsentGiven) {
                        onAcknowledge()
                    }
                },
                enabled = dpdpConsentGiven,
                colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("I Acknowledge & Trust", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Slate600)
            }
        }
    )
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
            Text("Zero-Knowledge Vault", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal900))
        }
    }
}

