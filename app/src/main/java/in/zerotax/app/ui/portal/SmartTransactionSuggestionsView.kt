package `in`.zerotax.app.ui.portal

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import `in`.zerotax.core.model.Form60ClientProfile
import `in`.zerotax.core.model.Rupee
import `in`.zerotax.core.model.SmartTransactionNudge
import `in`.zerotax.feature.aiextractor.OnDeviceTaxCopilot
import `in`.zerotax.feature.portalbridge.ItdTransactionFetcher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTransactionSuggestionsView(
    pan: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fetcher = remember { ItdTransactionFetcher() }
    val copilot = remember { OnDeviceTaxCopilot(context) }

    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var hasMorePages by remember { mutableStateOf(true) }
    var transactions by remember { mutableStateOf<List<AisTransactionRecord>>(emptyList()) }
    var smartNudges by remember { mutableStateOf<List<SmartTransactionNudge>>(emptyList()) }
    var showForm60Dialog by remember { mutableStateOf(false) }

    // Form 60 state
    var declarantName by remember { mutableStateOf("Satish Jadav") }
    var fatherName by remember { mutableStateOf("Ramesh Jadav") }
    var flatStreet by remember { mutableStateOf("402, Royal Residency, MG Road") }
    var city by remember { mutableStateOf("Bengaluru") }
    var state by remember { mutableStateOf("Karnataka") }
    var pincode by remember { mutableStateOf("560001") }
    var mobile by remember { mutableStateOf("9876545819") }
    var email by remember { mutableStateOf("satish.jadav@envitics.com") }
    var agriIncome by remember { mutableStateOf("0") }
    var isForm60Applied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = fetcher.fetchAisTransactionsPaged(pan = pan, page = 1, pageSize = 2)
        if (result.isSuccess) {
            val (list, more) = result.getOrNull() ?: Pair(emptyList(), false)
            transactions = list
            hasMorePages = more
            smartNudges = fetcher.generateSmartNudges(list)
        }
        isLoading = false
    }

    val loadNextPage: () -> Unit = {
        if (!isLoadingMore && hasMorePages) {
            scope.launch {
                isLoadingMore = true
                val nextPage = currentPage + 1
                val result = fetcher.fetchAisTransactionsPaged(pan = pan, page = nextPage, pageSize = 2)
                if (result.isSuccess) {
                    val (list, more) = result.getOrNull() ?: Pair(emptyList(), false)
                    transactions = transactions + list
                    currentPage = nextPage
                    hasMorePages = more
                    smartNudges = fetcher.generateSmartNudges(transactions)
                }
                isLoadingMore = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AIS Transactions & Form 60 Intelligence",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Auto-Fetched from ITD e-Filing Gateway (AIS / TIS Ledgers)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { showForm60Dialog = true },
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isForm60Applied) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = if (isForm60Applied) "Form 60 Linked" else "+ Form 60 Profile",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = EnterpriseTeal600, strokeWidth = 2.5.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Querying ITD Annual Information Statement (AIS)...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Form 60 Banner if applied
                if (isForm60Applied) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldSuccessBg),
                            border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess.copy(alpha = 0.5f)))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Form 60 Rule 114B Client KYC Linked",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                                    )
                                    Text(
                                        text = "$declarantName • $flatStreet, $city ($pincode)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Smart Nudges Card Section
                item {
                    Text(
                        text = "Auto-Applied Tax Benefits & Exemptions",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(smartNudges) { nudge ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.4f)))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = nudge.title,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                                )
                                StatutoryBadge(text = "Auto-Claimed", tone = BadgeTone.SUCCESS)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = nudge.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = EnterpriseTeal800
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Reported AIS / TIS Transaction Ledgers (${transactions.size} Records)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(
                    items = transactions,
                    key = { it.transactionId },
                    contentType = { it.category }
                ) { tx ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.reportingEntity,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = Rupee(tx.grossAmount).toString(),
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tx.category.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                                Text(
                                    text = tx.quarterOrDate,
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate500)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "→ ${tx.suggestedSchedule}",
                                    style = TextStyle(fontSize = 11.sp, color = IndigoStatutory, fontWeight = FontWeight.SemiBold)
                                )
                                if (tx.suggestedExemptionSection != null && tx.suggestedExemptionAmount > 0) {
                                    StatutoryBadge(
                                        text = "Exempt ₹${tx.suggestedExemptionAmount} u/s ${tx.suggestedExemptionSection}",
                                        tone = BadgeTone.SUCCESS
                                    )
                                } else if (tx.tdsWithheld > 0) {
                                    StatutoryBadge(
                                        text = "TDS ₹${tx.tdsWithheld} Claimable",
                                        tone = BadgeTone.STATUTORY
                                    )
                                }
                            }
                        }
                    }
                }

                if (hasMorePages) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = loadNextPage,
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            enabled = !isLoadingMore,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Lazy Loading Next Batch...", style = MaterialTheme.typography.labelMedium)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Lazy Load More Transactions (Page ${currentPage + 1})", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Apply Transactions to Return & Continue", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }

    // ========================================================================
    // FORM 60 (RULE 114B) CLIENT PROFILE DIALOG
    // ========================================================================
    if (showForm60Dialog) {
        AlertDialog(
            onDismissRequest = { showForm60Dialog = false },
            title = {
                Text(
                    text = "Form 60 (Rule 114B) Client KYC Intake",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text(
                            text = "Statutory declaration for financial transactions & address auto-population under Income-tax Rules, 1962.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = declarantName,
                            onValueChange = { declarantName = it },
                            label = { Text("Declarant Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = fatherName,
                            onValueChange = { fatherName = it },
                            label = { Text("Father's Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = flatStreet,
                            onValueChange = { flatStreet = it },
                            label = { Text("Flat / Door / Street Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pincode,
                                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pincode = it },
                                label = { Text("PIN") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = agriIncome,
                            onValueChange = { if (it.all { c -> c.isDigit() }) agriIncome = it },
                            label = { Text("Estimated Agricultural Income (Exempt u/s 10(1))") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isForm60Applied = true
                        showForm60Dialog = false
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600)
                ) {
                    Text("Save & Auto-Populate Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForm60Dialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

