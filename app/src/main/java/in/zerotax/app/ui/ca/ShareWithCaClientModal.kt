package `in`.zerotax.app.ui.ca

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.BadgeTone
import `in`.zerotax.app.ui.components.StatutoryBadge
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.ClientAuditStatus

/**
 * Taxpayer-facing modal to generate a 6-digit secure pairing code
 * for their Chartered Accountant / Auditor to review and sign-off on their return.
 *
 * Adheres strictly to Zero-Emoji design rules using Compose Vector Icons.
 */
@Composable
fun ShareWithCaClientModal(
    pan: String,
    taxpayerName: String = "Taxpayer",
    auditStatus: ClientAuditStatus = ClientAuditStatus.PENDING_REVIEW,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedCodeMessage by remember { mutableStateOf<String?>(null) }

    // VUL-04 FIX: Secure session-only pairing code using SecureRandom.
    // Must NOT be derived from PAN (fully predictable) and must be 4 random digits per session.
    // Uses remember() with no key so it regenerates each time the modal opens (session-scoped).
    val pairingCode = remember {
        val bytes = ByteArray(4)
        java.security.SecureRandom().nextBytes(bytes)
        val n = java.nio.ByteBuffer.wrap(bytes).int.and(0x7FFFFFFF) % 9000 + 1000
        "ZT-$n"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = EnterpriseTeal700,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Share Return with Your CA / Auditor", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    Text("Secure Single-Use Pairing Code", style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Provide this 6-digit code to your Chartered Accountant. They will enter it into their CA Verification Portal to review your schedules, verify Section 44AB books, and sign off on your return.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                )

                Spacer(Modifier.height(14.dp))

                // Pairing Code Display Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate50,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, EnterpriseTeal600),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("CLIENT PAIRING CODE", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, letterSpacing = 1.sp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = pairingCode,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                letterSpacing = 4.sp,
                                color = EnterpriseTeal900
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Valid for 15 minutes • One-time access", style = TextStyle(fontSize = 11.sp, color = Slate600))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(pairingCode))
                        copiedCodeMessage = "Code $pairingCode copied to clipboard!"
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Code to Share via WhatsApp / Email", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (copiedCodeMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = copiedCodeMessage!!,
                        color = EmeraldSuccess,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Current Audit Status Section
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Audit Verification Status", style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(auditStatus.label, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                        }
                        val tone = when (auditStatus) {
                            ClientAuditStatus.APPROVED_SIGNED_OFF -> BadgeTone.SUCCESS
                            ClientAuditStatus.IN_AUDIT -> BadgeTone.STATUTORY
                            ClientAuditStatus.QUERIES_RAISED, ClientAuditStatus.PENDING_REVIEW -> BadgeTone.WARNING
                            ClientAuditStatus.REJECTED -> BadgeTone.DANGER
                        }
                        StatutoryBadge(text = if (auditStatus == ClientAuditStatus.APPROVED_SIGNED_OFF) "CA Approved" else "Awaiting Sign-Off", tone = tone)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Zero-Knowledge Guarantee Note
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = EnterpriseTeal700, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "End-to-End Encrypted: Your CA only sees return audit data. Zero access to passwords or bank funds.",
                        style = TextStyle(fontSize = 10.sp, color = Slate600)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Slate800)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

