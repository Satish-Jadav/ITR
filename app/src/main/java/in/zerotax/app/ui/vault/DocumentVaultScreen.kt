package in.zerotax.app.ui.vault

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import in.zerotax.app.ui.components.*
import in.zerotax.app.ui.theme.*
import in.zerotax.app.viewmodel.ZeroTaxViewModel

data class VaultDocumentItem(
    val id: String,
    val title: String,
    val description: String,
    val isAttached: Boolean,
    val badgeText: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentVaultScreen(
    viewModel: ZeroTaxViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Multi-document file picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            uris.forEach { uri ->
                val fileName = uri.lastPathSegment ?: "Tax_Document.pdf"
                viewModel.ingestDocumentFromUri(
                    context = context,
                    uri = uri,
                    fileName = fileName,
                    onSuccess = { msg ->
                        notificationMessage = msg
                        isProcessing = false
                    },
                    onError = { err ->
                        notificationMessage = "Error: $err"
                        isProcessing = false
                    }
                )
            }
        }
    }

    // Camera / Single file scan launcher
    val cameraScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            viewModel.ingestDocumentFromUri(
                context = context,
                uri = uri,
                fileName = "Camera_Scan_Doc.pdf",
                onSuccess = { msg ->
                    notificationMessage = msg
                    isProcessing = false
                },
                onError = { err ->
                    notificationMessage = "Error: $err"
                    isProcessing = false
                }
            )
        }
    }

    val documents = remember(uiState) {
        listOf(
            VaultDocumentItem(
                id = "form16",
                title = "Form 16 Part A & B",
                description = if (uiState.employerName.isNotBlank()) "Employer: ${uiState.employerName} (TAN: ${uiState.employerTan})" else "Salary Certificate u/s 203 with TDS deduction",
                isAttached = uiState.grossSalaryRupees > 0L,
                badgeText = if (uiState.grossSalaryRupees > 0L) "₹${uiState.grossSalaryRupees} Verified" else "Required",
                icon = Icons.Default.Description
            ),
            VaultDocumentItem(
                id = "traces26as",
                title = "TRACES Form 26AS Tax Credit",
                description = if (uiState.tdsDeposited26As > 0L) "TDS u/s 192/194 Deposited: ₹${uiState.tdsDeposited26As}" else "Tax deducted at source and advance tax deposits",
                isAttached = uiState.tdsDeposited26As > 0L,
                badgeText = if (uiState.tdsDeposited26As > 0L) "Synced" else "Pending",
                icon = Icons.Default.ReceiptLong
            ),
            VaultDocumentItem(
                id = "ais_tis",
                title = "Annual Information Statement (AIS)",
                description = if (uiState.savingsInterestRupees > 0L) "Savings Interest: ₹${uiState.savingsInterestRupees}" else "Bank interest, dividends & securities transactions",
                isAttached = uiState.savingsInterestRupees > 0L,
                badgeText = if (uiState.savingsInterestRupees > 0L) "Reconciled" else "Optional",
                icon = Icons.Default.Security
            ),
            VaultDocumentItem(
                id = "cams_cas",
                title = "CAMS / KFintech CAS Statement",
                description = "Consolidated mutual fund & stock transactions ledger",
                isAttached = false,
                badgeText = "Auto-Unlock",
                icon = Icons.Default.TrendingUp
            ),
            VaultDocumentItem(
                id = "bank_stmt",
                title = "Bank Account Statements (PDF/CSV)",
                description = if (uiState.bankAccount.accountNumber.isNotBlank()) "Bank: ${uiState.bankAccount.bankName} (••••${uiState.bankAccount.accountNumber.takeLast(4)})" else "Categorizes savings interest u/s 80TTA/80TTB",
                isAttached = uiState.bankAccount.accountNumber.isNotBlank(),
                badgeText = if (uiState.bankAccount.accountNumber.isNotBlank()) "Validated" else "Add Bank",
                icon = Icons.Default.AccountBalance
            )
        )
    }

    Scaffold(
        topBar = {
            ZeroTaxTopAppBar(
                title = "Tax Document Vault",
                subtitle = "Hardware-Encrypted • Silent Auto-Unlock",
                pan = uiState.pan,
                onBack = onBack,
                badgeText = "SQLCipher Vault",
                badgeTone = BadgeTone.SUCCESS
            )
        },
        containerColor = if (isDark) Slate950 else Slate50
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Processing banner
            if (isProcessing) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EnterpriseTeal50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal500),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = EnterpriseTeal600)
                        Text("Decrypting with PAN/DOB & parsing deterministically...", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = EnterpriseTeal700))
                    }
                }
            }

            // Notification message
            notificationMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (msg.startsWith("Error")) CrimsonDangerBg else EmeraldSuccessBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (msg.startsWith("Error")) CrimsonDanger else EmeraldSuccess),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(msg, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = if (msg.startsWith("Error")) CrimsonDanger else EmeraldDark), modifier = Modifier.weight(1f))
                        IconButton(onClick = { notificationMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { documentPickerLauncher.launch(arrayOf("application/pdf", "text/csv", "application/json")) },
                    colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import PDF / CAS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = { cameraScanLauncher.launch("application/pdf") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Document", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            Text("Archived Tax Records", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(documents) { doc ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Slate850 else PureWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (doc.isAttached) EnterpriseTeal500 else if (isDark) Slate700 else Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (doc.isAttached) EnterpriseTeal500.copy(alpha = 0.15f) else Slate200),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(doc.icon, null, tint = if (doc.isAttached) EnterpriseTeal600 else Slate600, modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(doc.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    if (doc.isAttached) {
                                        Icon(Icons.Default.CheckCircle, null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(doc.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (doc.isAttached) EmeraldSuccessBg else Slate100,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (doc.isAttached) EmeraldSuccess else Slate300)
                            ) {
                                Text(
                                    text = doc.badgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (doc.isAttached) EmeraldDark else Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
