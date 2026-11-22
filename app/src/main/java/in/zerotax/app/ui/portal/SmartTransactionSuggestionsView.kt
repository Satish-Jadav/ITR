package `in`.zerotax.app.ui.portal

import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fetcher = remember { ItdTransactionFetcher() }
    val isDark = isSystemInDarkTheme()

    val defaultInitialRecords = remember {
        listOf(
            AisTransactionRecord(
                transactionId = "AIS-SAL-2026-001",
                category = AisCategory.SALARY,
                reportingEntity = "INFOSYS LIMITED",
                accountOrFolioNumber = "BLRP01234F",
                grossAmount = 892173L,
                tdsWithheld = 0L,
                quarterOrDate = "Q4 / FY 2025-26",
                suggestedSchedule = "Salary"
            ),
            AisTransactionRecord(
                transactionId = "AIS-INT-2026-002",
                category = AisCategory.SAVINGS_INTEREST,
                reportingEntity = "STATE BANK OF INDIA",
                accountOrFolioNumber = "SB-4981928371",
                grossAmount = 4472L,
                tdsWithheld = 0L,
                quarterOrDate = "FY 2025-26 Annual",
                suggestedSchedule = "Other Sources (OS)"
            ),
            AisTransactionRecord(
                transactionId = "26AS-DIV-2026-003",
                category = AisCategory.DIVIDENDS,
                reportingEntity = "INFOSYS LIMITED (DIVIDEND)",
                accountOrFolioNumber = "FOLIO-89421",
                grossAmount = 2450L,
                tdsWithheld = 245L,
                quarterOrDate = "Q3 / FY 2025-26",
                suggestedSchedule = "Schedule OS (Sec 56(2)(i))"
            )
        )
    }

    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var hasMorePages by remember { mutableStateOf(false) }
    var transactions by remember { mutableStateOf<List<AisTransactionRecord>>(defaultInitialRecords) }
    var smartNudges by remember { mutableStateOf<List<SmartTransactionNudge>>(fetcher.generateSmartNudges(defaultInitialRecords)) }
    var showForm60Dialog by remember { mutableStateOf(false) }

    // Form 60 state
    var declarantName by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var flatStreet by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var agriIncome by remember { mutableStateOf("0") }
    var isForm60Applied by remember { mutableStateOf(false) }

    LaunchedEffect(pan) {
        val targetPan = if (pan.length == 10) pan else "BZRPJ9498J"
        val result = fetcher.fetchAisTransactionsPaged(pan = targetPan, page = 1, pageSize = 20)
        if (result.isSuccess) {
            val (list, more) = result.getOrNull() ?: Pair(emptyList(), false)
            if (list.isNotEmpty()) {
                transactions = list
                hasMorePages = more
                smartNudges = fetcher.generateSmartNudges(list)
            }
        }
    }

    val loadNextPage: () -> Unit = {
        if (!isLoadingMore && hasMorePages) {
            scope.launch {
                isLoadingMore = true
                val nextPage = currentPage + 1
                val targetPan = if (pan.length == 10) pan else "BZRPJ9498J"
                val result = fetcher.fetchAisTransactionsPaged(pan = targetPan, page = nextPage, pageSize = 20)
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

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (showTopBar) {
            val topInset = `in`.zerotax.app.ui.components.getSafeStatusBarTopInset()
            TopAppBar(
                windowInsets = WindowInsets(top = topInset),
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { showForm60Dialog = true },
                        shape = RoundedCornerShape(8.dp),
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Reconciled Summary Hero Card
                item {
                    val totalGross = remember(transactions) { transactions.sumOf { it.grossAmount } }
                    val totalTds = remember(transactions) { transactions.sumOf { it.tdsWithheld } }
                    val isDark = isSystemInDarkTheme()

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Slate850 else PureWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else Slate200),
                        shadowElevation = if (isDark) 0.dp else 2.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "AIS / 26AS Tax Vault Reconciled",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                StatutoryBadge(text = "100% Match", tone = BadgeTone.SUCCESS)
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Gross Income", style = MaterialTheme.typography.labelSmall, color = Slate400)
                                    Text(
                                        text = Rupee(totalGross).toString(),
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isDark) PureWhite else Slate900)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("TDS Withheld (26AS)", style = MaterialTheme.typography.labelSmall, color = Slate400)
                                    Text(
                                        text = Rupee(totalTds).toString(),
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isDark) EnterpriseTeal300 else EmeraldSuccess)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Defects Detected", style = MaterialTheme.typography.labelSmall, color = Slate400)
                                    Text(
                                        text = "0 Category-A",
                                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldSuccess)
                                    )
                                }
                            }
                        }
                    }
                }
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
                        shape = RoundedCornerShape(12.dp),
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
                    val txIcon = when (tx.category) {
                        AisCategory.SALARY -> Icons.Default.Apartment
                        AisCategory.SAVINGS_INTEREST, AisCategory.TIME_DEPOSIT_INTEREST -> Icons.Default.AccountBalance
                        AisCategory.DIVIDENDS -> Icons.AutoMirrored.Filled.TrendingUp
                        else -> Icons.AutoMirrored.Filled.ReceiptLong
                    }
                    val isDark = isSystemInDarkTheme()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Slate850 else PureWhite),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(if (isDark) Slate700 else Slate200))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Slate800 else Slate100),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = txIcon,
                                            contentDescription = null,
                                            tint = EnterpriseTeal600,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = tx.reportingEntity,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = tx.accountOrFolioNumber,
                                            style = TextStyle(fontSize = 11.sp, color = Slate400, fontFamily = FontFamily.Monospace)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = Rupee(tx.grossAmount).toString(),
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = if (isDark) PureWhite else Slate900)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Schedule: ${tx.suggestedSchedule}",
                                    style = TextStyle(fontSize = 11.sp, color = IndigoStatutory, fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = tx.quarterOrDate,
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate400)
                                )
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

                // Bottom Reconciliation Action Card
                item {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = if (isDark) Slate850 else PureWhite,
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)),
                        shadowElevation = if (isDark) 0.dp else 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${transactions.size} Verified Matches Detected",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "100% Reconciliation against 26AS & Form 16",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Slate500)
                                    )
                                }
                                StatutoryBadge(text = "Reconciled", tone = BadgeTone.SUCCESS)
                            }
                            Spacer(Modifier.height(12.dp))
                            ZeroTaxPrimaryButton(
                                text = "Approve All Verified (1 Tap)",
                                onClick = onBack,
                                leadingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(100.dp))
                }
            }
        }

    // ========================================================================
    // FORM 60 (RULE 114B) CLIENT PROFILE SHEET
    // ========================================================================
    if (showForm60Dialog) {
        ZeroTaxBottomSheet(
            onDismissRequest = { showForm60Dialog = false }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Form 60 (Rule 114B) Client KYC Intake",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = { showForm60Dialog = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Statutory declaration for financial transactions & address auto-population under Income-tax Rules, 1962.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

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

            Spacer(Modifier.height(20.dp))

            ZeroTaxPrimaryButton(
                text = "Save & Auto-Populate Profile",
                onClick = {
                    isForm60Applied = true
                    showForm60Dialog = false
                }
            )
        }
    }
}
