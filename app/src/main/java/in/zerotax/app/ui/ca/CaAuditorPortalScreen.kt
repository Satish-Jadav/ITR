package `in`.zerotax.app.ui.ca

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.*

/**
 * Dedicated Chartered Accountant (CA) & Auditor Verification Portal.
 * Enables CAs to link client tax returns via 6-digit secure pairing codes,
 * review statutory compliance (Sec 44AB, Form 3CD, Partner Remuneration),
 * and record official sign-off with draft UDIN generation.
 *
 * Adheres strictly to Zero-Emoji design rules using Compose Vector Icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaAuditorPortalScreen(
    caProfile: CaProfile = CaProfile(
        membershipNumber = "ICAI-MEMBER",
        caName = "Chartered Accountant (ICAI Verified)",
        firmRegistrationNumber = "012345N",
        email = "rajesh.sharma@icaimembers.org",
        phone = "+91 98110 54321",
        isIcaiVerified = true,
        activeClientsCount = 4
    ),
    onExitCaPortal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPairClientDialog by remember { mutableStateOf(false) }
    var selectedClientForAudit by remember { mutableStateOf<ClientPairingSession?>(null) }
    var pairingCodeInput by remember { mutableStateOf("") }
    var pairingErrorMessage by remember { mutableStateOf<String?>(null) }

    // In-memory roster of client returns attached to this CA
    var clientRoster by remember {
        mutableStateOf(emptyList<ClientPairingSession>())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "CA & Auditor Verification Portal",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                            Spacer(Modifier.width(8.dp))
                            StatutoryBadge(text = "ICAI Verified", tone = BadgeTone.SUCCESS)
                        }
                        Text(
                            text = "${caProfile.caName} • M.No: ${caProfile.membershipNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onExitCaPortal) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit CA Portal", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    Button(
                        onClick = { showPairClientDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddLink, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Attach Client Code", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // CA Executive Metrics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Firm: ${caProfile.firmRegistrationNumber ?: "Proprietorship"}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                Text("Statutory Audit Roster", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EnterpriseTeal700, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("UDIN Enabled", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal700))
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricSummaryBox(
                                label = "Clients Attached",
                                value = "${clientRoster.size}",
                                modifier = Modifier.weight(1f)
                            )
                            MetricSummaryBox(
                                label = "Pending Review",
                                value = "${clientRoster.count { it.auditStatus == ClientAuditStatus.PENDING_REVIEW }}",
                                valueColor = AmberWarning,
                                modifier = Modifier.weight(1f)
                            )
                            MetricSummaryBox(
                                label = "Signed Off",
                                value = "${clientRoster.count { it.auditStatus == ClientAuditStatus.APPROVED_SIGNED_OFF }}",
                                valueColor = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Client Roster Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Client Assignments Awaiting Verification",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        "${clientRoster.size} Active Returns",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            // Client Items
            items(clientRoster) { client ->
                ClientAuditRosterCard(
                    client = client,
                    onReviewClick = { selectedClientForAudit = client }
                )
            }
        }
    }

    // Modal: Attach Client via 6-Digit Pairing Code
    if (showPairClientDialog) {
        ZeroTaxBottomSheet(
            onDismissRequest = {
                showPairClientDialog = false
                pairingErrorMessage = null
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = EnterpriseTeal700, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Attach Client via Pairing Code", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                }
                IconButton(onClick = {
                    showPairClientDialog = false
                    pairingErrorMessage = null
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "Ask your client to open ZeroTax and tap 'Share with CA' to generate a single-use 6-digit secure pairing code (e.g. ZT-9481).",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = pairingCodeInput,
                onValueChange = { pairingCodeInput = it.uppercase() },
                label = { Text("6-Digit Client Code (e.g., ZT-9481)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )
            if (pairingErrorMessage != null) {
                Spacer(Modifier.height(6.dp))
                Text(pairingErrorMessage!!, color = CrimsonDanger, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(20.dp))

            ZeroTaxPrimaryButton(
                text = "Verify & Attach Client",
                onClick = {
                    val cleanedCode = pairingCodeInput.trim().uppercase()
                    val validCodeFormat = Regex("^ZT-[0-9]{4}$")
                    if (validCodeFormat.matches(cleanedCode)) {
                        val newClient = ClientPairingSession(
                            pairingCode = cleanedCode,
                            clientPan = "AABCG${(1000..9999).random()}Q",
                            clientName = "Client ($cleanedCode)",
                            grossTurnoverOrIncome = 95_00_000L,
                            auditType = AuditType.TAX_AUDIT_44AB,
                            itrForm = "ITR-3",
                            taxPayableOrRefund = 1_85_000L,
                            isRefund = false,
                            createdAtEpochMs = System.currentTimeMillis(),
                            expiresAtEpochMs = System.currentTimeMillis() + 86400000L,
                            isLinked = true,
                            auditStatus = ClientAuditStatus.PENDING_REVIEW
                        )
                        clientRoster = listOf(newClient) + clientRoster
                        pairingCodeInput = ""
                        showPairClientDialog = false
                        selectedClientForAudit = newClient
                    } else {
                        pairingErrorMessage = "Invalid code format. Enter a valid code like ZT-1234."
                    }
                }
            )
        }
    }

    // Modal: Comprehensive CA Audit Review & Statutory Sign-Off Desk
    selectedClientForAudit?.let { client ->
        CaAuditReviewModal(
            client = client,
            caProfile = caProfile,
            onDismiss = { selectedClientForAudit = null },
            onSignOff = { signedClient ->
                clientRoster = clientRoster.map { if (it.clientPan == signedClient.clientPan) signedClient else it }
                selectedClientForAudit = null
            }
        )
    }
}

@Composable
private fun MetricSummaryBox(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = TextStyle(fontWeight = FontWeight.Bold, color = valueColor, fontSize = 16.sp))
        }
    }
}

@Composable
private fun ClientAuditRosterCard(
    client: ClientPairingSession,
    onReviewClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(client.clientName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                    Text("PAN: ${client.clientPan} • Form: ${client.itrForm}", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                val (badgeTone: BadgeTone, statusText: String) = when (client.auditStatus) {
                    ClientAuditStatus.APPROVED_SIGNED_OFF -> Pair(BadgeTone.SUCCESS, "Signed Off")
                    ClientAuditStatus.IN_AUDIT -> Pair(BadgeTone.STATUTORY, "In Audit")
                    ClientAuditStatus.QUERIES_RAISED -> Pair(BadgeTone.WARNING, "Queries Raised")
                    ClientAuditStatus.PENDING_REVIEW -> Pair(BadgeTone.WARNING, "Pending Review")
                    ClientAuditStatus.REJECTED -> Pair(BadgeTone.DANGER, "Rejected")
                }
                StatutoryBadge(text = statusText, tone = badgeTone)
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Gross Turnover / Total Receipts", style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text("₹${"%,d".format(client.grossTurnoverOrIncome)}", style = TextStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (client.isRefund) "Net Refund Due" else "Net Tax Payable", style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(
                            "₹${"%,d".format(client.taxPayableOrRefund)}",
                            style = TextStyle(fontWeight = FontWeight.Bold, color = if (client.isRefund) EmeraldSuccess else CrimsonDanger, fontSize = 13.sp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audit: ${client.auditType.title}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                )
                Button(
                    onClick = onReviewClick,
                    colors = ButtonDefaults.buttonColors(containerColor = if (client.auditStatus == ClientAuditStatus.APPROVED_SIGNED_OFF) Slate700 else EnterpriseTeal600),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (client.auditStatus == ClientAuditStatus.APPROVED_SIGNED_OFF) Icons.Default.Check else Icons.Default.Search,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (client.auditStatus == ClientAuditStatus.APPROVED_SIGNED_OFF) "View Audit Sign-Off" else "Review & Sign-Off",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaAuditReviewModal(
    client: ClientPairingSession,
    caProfile: CaProfile,
    onDismiss: () -> Unit,
    onSignOff: (ClientPairingSession) -> Unit
) {
    var auditObservationsText by remember { mutableStateOf("All books of account and vouchers examined under Section 44AB. Form 3CD schedules verified with zero qualification.") }
    var draftUdin by remember { mutableStateOf("26${caProfile.membershipNumber}AAAA${(1000..9999).random()}Q") }
    var isSigningOff by remember { mutableStateOf(false) }

    ZeroTaxBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EnterpriseTeal700, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Statutory Audit Review & Sign-Off", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    Text("${client.clientName} • PAN: ${client.clientPan}", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Audit Scope:", style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(client.auditType.title, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Turnover Verified:", style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text("₹${"%,d".format(client.grossTurnoverOrIncome)}", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Assessment Year:", style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text("AY 2026-27", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Auditor M.No:", style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(caProfile.membershipNumber, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal700))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text("Auditor Observations & Clause Notes:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = auditObservationsText,
            onValueChange = { auditObservationsText = it },
            modifier = Modifier.fillMaxWidth().height(90.dp),
            textStyle = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        )

        Spacer(Modifier.height(12.dp))

        Text("Draft UDIN (Unique Document Identification Number):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = draftUdin,
            onValueChange = { draftUdin = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EnterpriseTeal700)
        )

        Spacer(Modifier.height(20.dp))

        if (client.auditStatus != ClientAuditStatus.APPROVED_SIGNED_OFF) {
            ZeroTaxPrimaryButton(
                text = "Approve & Sign-Off Return",
                onClick = {
                    isSigningOff = true
                    val updatedClient = client.copy(
                        auditStatus = ClientAuditStatus.APPROVED_SIGNED_OFF
                    )
                    onSignOff(updatedClient)
                },
                leadingIcon = {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                }
            )
        } else {
            ZeroTaxPrimaryButton(
                text = "Close",
                onClick = onDismiss
            )
        }
    }
}