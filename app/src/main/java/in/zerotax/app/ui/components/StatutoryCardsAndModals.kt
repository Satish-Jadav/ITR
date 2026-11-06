package `in`.zerotax.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.EmployerSalarySource
import `in`.zerotax.core.model.FilingTierMode
import `in`.zerotax.core.model.InstitutionPdfPasswordRegistry
import `in`.zerotax.core.model.InstitutionPdfPasswordRule
import `in`.zerotax.core.model.Rupee
import `in`.zerotax.core.model.ScheduleItChallanEntry
import `in`.zerotax.core.model.TaxPaymentChannel
import `in`.zerotax.core.security.network.ZeroTaxCloudApiBridge
import `in`.zerotax.feature.pdfunlocker.DeterministicTracesRegexParser
import `in`.zerotax.feature.pdfunlocker.SilentPdfUnlocker
import `in`.zerotax.feature.portalbridge.ChallanTaxType
import `in`.zerotax.feature.portalbridge.UpiChallan280IntentLauncher

@Composable
fun DpdpSection6MultilingualConsentCard(
    currentLocale: String,
    onLocaleSelected: (String) -> Unit,
    onConsentGranted: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DPDP Act, 2023 — Statutory Consent", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Your financial and tax data is processed strictly on-device inside hardware-encrypted vault (StrongBox AES-256-GCM). Zero network egress.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("en-IN" to "English", "hi-IN" to "हिन्दी", "gu-IN" to "ગુજરાતી").forEach { (code, label) ->
                    FilterChip(
                        selected = currentLocale == code,
                        onClick = { onLocaleSelected(code) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConsentGranted,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Grant Local On-Device Consent")
            }
        }
    }
}

@Composable
fun OneTapPdfDisambiguationCard(
    extractedValue: Long,
    expectedNetSalary: Long,
    croppedRegionBitmap: Bitmap?,
    onConfirmUserOverridden: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Discrepancy in Form 16 Net Salary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Printed Net: ${IndianCurrencyFormatter.formatRupees(extractedValue)} vs Computed: ${IndianCurrencyFormatter.formatRupees(expectedNetSalary)}")

            croppedRegionBitmap?.let { bmp ->
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Cropped PDF region",
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConfirmUserOverridden(expectedNetSalary) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Use Computed")
                }
                OutlinedButton(
                    onClick = { onConfirmUserOverridden(extractedValue) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Use Printed")
                }
            }
        }
    }
}

@Composable
fun BlackMoneyActForeignAssetModal(
    onDismiss: () -> Unit,
    onAcknowledged: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Black Money Act Warning", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Failure to disclose foreign shares (RSU/ESPP) in Schedule FA attracts a statutory penalty of ₹10,00,000 under Section 42 & 43 of the Black Money Act, 2015, even if foreign tax was withheld.",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAcknowledged,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("I Acknowledge & Will Fill Schedule FA")
                }
            }
        }
    }
}

@Composable
fun EmployerTdsShieldCard(
    discrepancyAmount: Long,
    employerTan: String,
    onGenerateSection205Notice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Section 205 Employer TDS Shield", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Employer $employerTan deducted ${IndianCurrencyFormatter.formatRupees(discrepancyAmount)} but failed to deposit it to Govt Treasury. Under Section 205, the IT Department cannot recover this tax from you.")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onGenerateSection205Notice, modifier = Modifier.fillMaxWidth()) {
                Text("Generate Section 205 Legal Dispute Notice")
            }
        }
    }
}

@Composable
fun RegimeBreakEvenOptimizerCard(
    oldRegimeTax: Long,
    newRegimeTax: Long,
    breakEvenDeductionNeeded: Long,
    recommendedRegime: String,
    onSelectRegime: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Dual-Regime Showdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("New Regime: ${IndianCurrencyFormatter.formatRupees(newRegimeTax)}")
                Text("Old Regime: ${IndianCurrencyFormatter.formatRupees(oldRegimeTax)}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (breakEvenDeductionNeeded > 0L) {
                Text(
                    "You need ${IndianCurrencyFormatter.formatRupees(breakEvenDeductionNeeded)} more deductions (80C/80D/HRA) to make Old Regime win.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onSelectRegime(recommendedRegime) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Opt for $recommendedRegime (Saves Tax)")
            }
        }
    }
}

@Composable
fun Presumptive44AdaAnd44AdSliderCard(
    grossReceipts: Long,
    declaredRatePercent: Double,
    is44AdaProfession: Boolean,
    onRateChanged: (Double) -> Unit
) {
    val floorRate = if (is44AdaProfession) 50.0 else 6.0
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (is44AdaProfession) "Section 44ADA Presumptive (50% Floor)" else "Section 44AD Presumptive (6%/8% Floor)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Gross Receipts: ${IndianCurrencyFormatter.formatRupees(grossReceipts)}")
            Text("Declared Profit Rate: ${"%.1f".format(declaredRatePercent)}% (Min: ${floorRate}%)")
            Slider(
                value = declaredRatePercent.toFloat(),
                onValueChange = { onRateChanged(it.toDouble().coerceAtLeast(floorRate)) },
                valueRange = floorRate.toFloat()..100f
            )
            val deemedProfit = (grossReceipts * (declaredRatePercent / 100.0)).toLong()
            Text("Deemed Taxable Business Profit: ${IndianCurrencyFormatter.formatRupees(deemedProfit)}", fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Active PDF Password Decryptor, Form 16 Data Importer & Institutional Password Calculator.
 * Enables real in-memory decryption, password removal, document export, and 1-tap statutory salary import.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstitutionPdfPasswordCheatsheetModal(
    pan: String = "",
    onDismiss: () -> Unit,
    onSelectHint: ((String) -> Unit)? = null,
    onSalaryExtracted: ((grossSalary: Long, tds: Long, employerTan: String, sec80C: Long, sec80D: Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val unlocker = remember { SilentPdfUnlocker() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Decrypt File", "My Passwords", "Directory")

    // User inputs for dynamic password calculation and decryption
    var assesseePan by remember { mutableStateOf(pan.uppercase()) }
    var dateOfBirth by remember { mutableStateOf("") }
    var assesseeFirstName by remember { mutableStateOf("") }
    var bankAccountLast4 by remember { mutableStateOf("") }
    var manualPassword by remember { mutableStateOf("") }

    // File picking and decryption state
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by remember { mutableStateOf("") }
    var pdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var decryptStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var unlockedPassword by remember { mutableStateOf<String?>(null) }
    var decryptedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var extractedForm16 by remember { mutableStateOf<EmployerSalarySource?>(null) }
    var isSalaryImported by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var copiedPasswordFeedback by remember { mutableStateOf<String?>(null) }

    // Directory rules
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Tax & Identity", "Banking", "Investments")
    var allRules by remember { mutableStateOf(InstitutionPdfPasswordRegistry.rules) }

    LaunchedEffect(Unit) {
        runCatching {
            SilentPdfUnlocker.initPdfBox(context)
            val json = ZeroTaxCloudApiBridge.fetchInstitutionPdfRules().getOrNull()
            val array = json?.optJSONArray("rules")
            if (array != null && array.length() > 0) {
                val list = mutableListOf<InstitutionPdfPasswordRule>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    list.add(
                        InstitutionPdfPasswordRule(
                            institutionId = o.optString("institutionId"),
                            institutionName = o.optString("institutionName"),
                            documentCategory = o.optString("documentCategory", "Tax & Identity"),
                            passwordPatternRule = o.optString("passwordPatternRule"),
                            exampleDescription = o.optString("exampleDescription"),
                            exampleInput = o.optString("exampleInput"),
                            officialHelplineHint = o.optString("officialHelplineHint")
                        )
                    )
                }
                InstitutionPdfPasswordRegistry.updateRules(list)
                allRules = list
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            var name = "document.pdf"
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) {
                        name = cursor.getString(nameIdx)
                    }
                }
            }
            pdfFileName = name
            isProcessing = true
            decryptStatusMessage = "Loading document into memory..."
            decryptedBytes = null
            unlockedPassword = null
            extractedForm16 = null
            isSalaryImported = false
            saveSuccessMessage = null
            isSuccess = false

            coroutineScope.launch(Dispatchers.IO) {
                runCatching {
                    SilentPdfUnlocker.initPdfBox(context)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        pdfBytes = stream.readBytes()
                    }
                }.fold(
                    onSuccess = {
                        decryptStatusMessage = "Document loaded (${pdfBytes?.size ?: 0} bytes). Ready to unlock."
                    },
                    onFailure = { err ->
                        decryptStatusMessage = "Unable to read file: ${err.localizedMessage}"
                    }
                )
                isProcessing = false
            }
        }
    }

    val pdfSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && decryptedBytes != null) {
            coroutineScope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(decryptedBytes)
                    }
                }.fold(
                    onSuccess = {
                        saveSuccessMessage = "Decrypted PDF saved successfully without password."
                    },
                    onFailure = {
                        saveSuccessMessage = "Save failed: ${it.localizedMessage}"
                    }
                )
            }
        }
    }

    fun executeAutoUnlock() {
        val bytes = pdfBytes ?: return
        isProcessing = true
        decryptStatusMessage = "Evaluating deterministic password permutations..."
        saveSuccessMessage = null
        isSuccess = false

        coroutineScope.launch {
            SilentPdfUnlocker.initPdfBox(context)
            val result = unlocker.attemptUnlock(
                pdfBytes = bytes,
                pan = assesseePan,
                dobDdMmYyyy = dateOfBirth,
                firstName = assesseeFirstName,
                bankCustomerId = bankAccountLast4,
                bankAccountLast4 = bankAccountLast4
            )
            result.fold(
                onSuccess = { (unlockedPdf, winningPwd) ->
                    decryptedBytes = unlockedPdf
                    unlockedPassword = winningPwd
                    isSuccess = true
                    decryptStatusMessage = "Document unlocked successfully."

                    val textRes = unlocker.extractTextFromDecrypted(unlockedPdf)
                    textRes.onSuccess { text ->
                        val parsed = DeterministicTracesRegexParser.parseStandardTracesForm16(text).getOrNull()
                        extractedForm16 = parsed
                    }
                    isProcessing = false
                },
                onFailure = {
                    isSuccess = false
                    decryptStatusMessage = "Auto-unlock could not match standard rules. Enter your custom password below."
                    isProcessing = false
                }
            )
        }
    }

    fun executeManualUnlock() {
        val bytes = pdfBytes ?: return
        if (manualPassword.isBlank()) return
        isProcessing = true
        decryptStatusMessage = "Verifying custom password..."
        saveSuccessMessage = null
        isSuccess = false

        coroutineScope.launch {
            SilentPdfUnlocker.initPdfBox(context)
            val result = unlocker.unlockWithCustomPassword(bytes, manualPassword.trim())
            result.fold(
                onSuccess = { unlockedPdf ->
                    decryptedBytes = unlockedPdf
                    unlockedPassword = manualPassword.trim()
                    isSuccess = true
                    decryptStatusMessage = "Unlocked with custom password."

                    val textRes = unlocker.extractTextFromDecrypted(unlockedPdf)
                    textRes.onSuccess { text ->
                        val parsed = DeterministicTracesRegexParser.parseStandardTracesForm16(text).getOrNull()
                        extractedForm16 = parsed
                    }
                    isProcessing = false
                },
                onFailure = {
                    isSuccess = false
                    decryptStatusMessage = "Decryption failed: Incorrect password provided."
                    isProcessing = false
                }
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = EnterpriseTeal600,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "PDF Decryptor & Password Hub",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Unlock, decrypt, export, and extract Form 16 / AIS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // TAB 1: ACTIVE DECRYPT FILE
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // File selection card
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "1. Select Encrypted Document",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (selectedPdfUri == null) {
                                            OutlinedButton(
                                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Choose Protected PDF")
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Description, contentDescription = null, tint = EnterpriseTeal600, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            pdfFileName,
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            "${(pdfBytes?.size ?: 0) / 1024} KB Loaded",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                TextButton(onClick = { pdfPickerLauncher.launch("application/pdf") }) {
                                                    Text("Change")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Assessee parameters for permutation engine
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "2. Taxpayer Credentials for Auto-Unlock",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = assesseePan,
                                                onValueChange = { assesseePan = it.uppercase().take(10) },
                                                label = { Text("PAN", fontSize = 11.sp) },
                                                placeholder = { Text("e.g. ABCDE1234F", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            )
                                            OutlinedTextField(
                                                value = dateOfBirth,
                                                onValueChange = { dateOfBirth = it.replace(Regex("[^0-9]"), "").take(8) },
                                                label = { Text("DOB (DDMMYYYY)", fontSize = 11.sp) },
                                                placeholder = { Text("15081992", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = assesseeFirstName,
                                                onValueChange = { assesseeFirstName = it.take(20) },
                                                label = { Text("First Name (Optional)", fontSize = 11.sp) },
                                                placeholder = { Text("for Aadhaar/ICICI", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = TextStyle(fontSize = 12.sp)
                                            )
                                            OutlinedTextField(
                                                value = bankAccountLast4,
                                                onValueChange = { bankAccountLast4 = it.take(10) },
                                                label = { Text("Bank / Cust ID (Optional)", fontSize = 11.sp) },
                                                placeholder = { Text("Last 4 digits", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = TextStyle(fontSize = 12.sp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Unlock CTA
                            item {
                                Button(
                                    onClick = { executeAutoUnlock() },
                                    enabled = pdfBytes != null && !isProcessing,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Decrypting in Secure Memory...")
                                    } else {
                                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Unlock & Remove Security")
                                    }
                                }
                            }

                            // Status message banner
                            if (decryptStatusMessage != null) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                decryptStatusMessage ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }

                            // Decryption result card & export actions
                            if (isSuccess && unlockedPassword != null) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Decryption Successful",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal700)
                                                )
                                                StatutoryBadge(text = "UNLOCKED", tone = BadgeTone.SUCCESS)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "Decrypted Password:",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
                                                    ) {
                                                        Text(
                                                            text = unlockedPassword ?: "",
                                                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = {
                                                            clipboardManager.setText(AnnotatedString(unlockedPassword ?: ""))
                                                            copiedPasswordFeedback = "Copied to clipboard"
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    val cleanName = pdfFileName.substringBeforeLast(".")
                                                    pdfSaverLauncher.launch("${cleanName}_decrypted.pdf")
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Save Decrypted PDF (No Password)")
                                            }
                                            if (saveSuccessMessage != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    saveSuccessMessage ?: "",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EnterpriseTeal700
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Form 16 extraction result and statutory return import
                            if (extractedForm16 != null) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "TRACES Form 16 Data Detected",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                                StatutoryBadge(text = "FORM 16", tone = BadgeTone.STATUTORY)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Deductor TAN: ${extractedForm16?.employerTan}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Text(
                                                "Gross Salary: ₹${Rupee.formatInr(extractedForm16?.grossSalary ?: 0L)}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                "TDS Deposited: ₹${Rupee.formatInr(extractedForm16?.tdsReported ?: 0L)}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if ((extractedForm16?.sec80C ?: 0L) > 0L) {
                                                Text(
                                                    "Section 80C Deductions: ₹${Rupee.formatInr(extractedForm16?.sec80C ?: 0L)}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            if (!isSalaryImported) {
                                                Button(
                                                    onClick = {
                                                        val f16 = extractedForm16
                                                        if (f16 != null) {
                                                            onSalaryExtracted?.invoke(
                                                                f16.grossSalary,
                                                                f16.tdsReported,
                                                                f16.employerTan,
                                                                f16.sec80C,
                                                                f16.sec80D
                                                            )
                                                            isSalaryImported = true
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Import Salary & TDS into Return")
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = EnterpriseTeal700.copy(alpha = 0.15f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = EnterpriseTeal700, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            "Imported into active return computation",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = EnterpriseTeal700)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Manual password fallback section
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Manual Custom Password (Fallback)",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "If auto-unlock failed, enter your specific password to decrypt:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = manualPassword,
                                                onValueChange = { manualPassword = it },
                                                placeholder = { Text("Enter PDF password", fontSize = 12.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = TextStyle(fontSize = 12.sp)
                                            )
                                            Button(
                                                onClick = { executeManualUnlock() },
                                                enabled = pdfBytes != null && manualPassword.isNotBlank() && !isProcessing,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Unlock")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 2: MY PASSWORDS CALCULATOR
                        val cleanPan = assesseePan.uppercase().trim()
                        val cleanDob = dateOfBirth.replace(Regex("[^0-9]"), "").trim()
                        val cleanName = assesseeFirstName.trim()
                        val cleanLast4 = bankAccountLast4.trim()

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Taxpayer credentials input header
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Taxpayer Parameters",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Enter details once to generate all your institutional passwords:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = assesseePan,
                                                onValueChange = { assesseePan = it.uppercase().take(10) },
                                                label = { Text("PAN", fontSize = 11.sp) },
                                                placeholder = { Text("ABCDE1234F", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            )
                                            OutlinedTextField(
                                                value = dateOfBirth,
                                                onValueChange = { dateOfBirth = it.replace(Regex("[^0-9]"), "").take(8) },
                                                label = { Text("DOB (DDMMYYYY)", fontSize = 11.sp) },
                                                placeholder = { Text("15081992", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = assesseeFirstName,
                                                onValueChange = { assesseeFirstName = it.take(20) },
                                                label = { Text("First Name", fontSize = 11.sp) },
                                                placeholder = { Text("Taxpayer First Name", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = TextStyle(fontSize = 12.sp)
                                            )
                                            OutlinedTextField(
                                                value = bankAccountLast4,
                                                onValueChange = { bankAccountLast4 = it.take(10) },
                                                label = { Text("Bank Last 4 / Cust ID", fontSize = 11.sp) },
                                                placeholder = { Text("1234", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = TextStyle(fontSize = 12.sp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (copiedPasswordFeedback != null) {
                                item {
                                    Text(
                                        text = copiedPasswordFeedback ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EnterpriseTeal700,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }

                            // Computed list of items
                            val items = listOf(
                                Triple(
                                    "TRACES Form 16 (Part A & B)",
                                    "Tax & Identity",
                                    if (cleanPan.length == 10 && cleanDob.length == 8) cleanPan.uppercase() + cleanDob else null
                                ) to "Formula: PAN in UPPERCASE + Date of Birth (DDMMYYYY)",
                                Triple(
                                    "AIS / TIS (Annual Information Statement)",
                                    "Tax & Identity",
                                    if (cleanPan.length == 10 && cleanDob.length == 8) cleanPan.lowercase() + cleanDob else null
                                ) to "Formula: PAN in lowercase + Date of Birth (DDMMYYYY)",
                                Triple(
                                    "e-Aadhaar (UIDAI Masked / Regular)",
                                    "Tax & Identity",
                                    if (cleanName.length >= 4 && cleanDob.length == 8) cleanName.take(4).uppercase() + cleanDob.takeLast(4) else null
                                ) to "Formula: First 4 letters of Name (UPPERCASE) + Year of Birth (YYYY)",
                                Triple(
                                    "ICICI Bank Statement",
                                    "Banking",
                                    if (cleanName.length >= 4 && cleanDob.length >= 4) cleanName.take(4).lowercase() + cleanDob.take(4) else null
                                ) to "Formula: First 4 letters of Name (lowercase) + DOB (DDMM)",
                                Triple(
                                    "Axis Bank Statement",
                                    "Banking",
                                    if (cleanName.length >= 4 && cleanLast4.length >= 4) cleanName.take(4).uppercase() + cleanLast4.takeLast(4) else null
                                ) to "Formula: First 4 letters of Name (UPPERCASE) + Last 4 digits of Account",
                                Triple(
                                    "Zerodha Tax P&L / Statement",
                                    "Investments",
                                    if (cleanPan.length == 10) cleanPan.uppercase() else null
                                ) to "Formula: PAN in UPPERCASE",
                                Triple(
                                    "SBI Bank Statement",
                                    "Banking",
                                    if (cleanLast4.length >= 4 && cleanDob.length >= 6) cleanLast4.takeLast(5) + cleanDob.take(4) + cleanDob.takeLast(2) else null
                                ) to "Formula: Last 5 digits of mobile + DOB (DDMMYY) or 11-digit account number",
                                Triple(
                                    "HDFC Bank Statement",
                                    "Banking",
                                    if (cleanLast4.isNotBlank()) cleanLast4 else null
                                ) to "Formula: NetBanking Customer ID",
                                Triple(
                                    "Kotak Mahindra Bank",
                                    "Banking",
                                    if (cleanDob.length == 8) cleanDob else null
                                ) to "Formula: Date of Birth (DDMMYYYY) or CRN Number",
                                Triple(
                                    "CAMS / KFintech Consolidated Account Statement",
                                    "Investments",
                                    "Defined during email request"
                                ) to "Formula: Password specified by investor during mail CAS generation"
                            )

                            items(items) { (info, formula) ->
                                val (title, category, computedPwd) = info
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            StatutoryBadge(
                                                text = category,
                                                tone = when (category) {
                                                    "Tax & Identity" -> BadgeTone.STATUTORY
                                                    "Banking" -> BadgeTone.SUCCESS
                                                    else -> BadgeTone.NEUTRAL
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            formula,
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (computedPwd != null) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
                                                ) {
                                                    Text(
                                                        text = computedPwd,
                                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EnterpriseTeal700),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(computedPwd))
                                                        copiedPasswordFeedback = "Copied \"$title\" password to clipboard"
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Copy", fontSize = 11.sp)
                                                }
                                            }
                                        } else {
                                            Text(
                                                "Fill PAN, DOB or Name above to calculate your exact password.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 3: SEARCHABLE DIRECTORY
                        val filteredRules = remember(selectedCategory, allRules) {
                            if (selectedCategory == "All") allRules
                            else allRules.filter { it.documentCategory == selectedCategory }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            // Category chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                items(categories) { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredRules) { rule ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                clipboardManager.setText(AnnotatedString(rule.exampleInput))
                                                onSelectHint?.invoke(rule.exampleInput)
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = rule.institutionName,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                )
                                                StatutoryBadge(
                                                    text = rule.documentCategory,
                                                    tone = when (rule.documentCategory) {
                                                        "Tax & Identity" -> BadgeTone.STATUTORY
                                                        "Banking" -> BadgeTone.SUCCESS
                                                        else -> BadgeTone.NEUTRAL
                                                    }
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Formula: ${rule.passwordPatternRule}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = EnterpriseTeal700)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${rule.exampleDescription} -> ${rule.exampleInput}",
                                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

/**
 * 5-point Interactive Pre-Submission Safety Checklist before CPC transmission.
 */
@Composable
fun PreSubmissionSafetyChecklistCard(
    isBankPreValidated: Boolean,
    isAadhaarLinked: Boolean,
    hasDiscrepancyShieldActive: Boolean,
    isLandlordPanCompliant: Boolean,
    isScheduleFaCompliant: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50.copy(alpha = 0.6f)),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.4f)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EnterpriseTeal900,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Pre-Filing Safety & Notice Shield (5 Gates)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                    )
                }
                val allPassed = isBankPreValidated && isAadhaarLinked && hasDiscrepancyShieldActive && isLandlordPanCompliant && isScheduleFaCompliant
                StatutoryBadge(
                    text = if (allPassed) "5/5 VERIFIED" else "ACTION RECOMMENDED",
                    tone = if (allPassed) BadgeTone.SUCCESS else BadgeTone.WARNING
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SafetyChecklistItem(
                title = "Direct Refund Bank Account",
                description = if (isBankPreValidated) "Pre-validated on ITD portal for electronic refund credit" else "Verify IFSC & active ECS status to avoid refund delays",
                isPassed = isBankPreValidated,
                statutoryRef = "Rule 12"
            )

            SafetyChecklistItem(
                title = "Aadhaar-PAN Linkage Status",
                description = if (isAadhaarLinked) "PAN is fully operative u/s 139AA with instant e-Verification" else "Ensure Aadhaar is linked to prevent PAN from turning inoperative",
                isPassed = isAadhaarLinked,
                statutoryRef = "Sec 139AA"
            )

            SafetyChecklistItem(
                title = "Section 205 Employer TDS Shield",
                description = if (hasDiscrepancyShieldActive) "No uncredited TDS demand risks from employer non-deposit" else "Employer TDS difference shielded from recovery u/s 205",
                isPassed = hasDiscrepancyShieldActive,
                statutoryRef = "Sec 205"
            )

            SafetyChecklistItem(
                title = "Mandatory Landlord PAN Reporting",
                description = if (isLandlordPanCompliant) "Landlord PAN compliant or annual rent <= ₹1,00,000" else "Rent > ₹1 Lakh/yr requires landlord PAN under Circular 08/2013",
                isPassed = isLandlordPanCompliant,
                statutoryRef = "Circular 08/2013"
            )

            SafetyChecklistItem(
                title = "Schedule FA / Foreign Assets Gate",
                description = if (isScheduleFaCompliant) "Foreign ESOP/RSU/US equity holdings fully compliant" else "Foreign assets must be declared to avoid ₹10L penalty u/s 42/43",
                isPassed = isScheduleFaCompliant,
                statutoryRef = "Black Money Act"
            )
        }
    }
}

@Composable
private fun SafetyChecklistItem(
    title: String,
    description: String,
    isPassed: Boolean,
    statutoryRef: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (isPassed) EmeraldSuccess else AmberWarning),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPassed) Icons.Default.Check else Icons.Default.PriorityHigh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = statutoryRef,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Transparent explainer showing how Section 87A Marginal Relief protects
 * taxpayers earning between ₹7,00,000 and ₹7,27,777 under the New Tax Regime.
 */
@Composable
fun Section87AMarginalReliefExplainerCard(
    taxableIncome: Long,
    taxBeforeRelief: Long,
    marginalReliefAmount: Long,
    finalTaxBeforeCess: Long,
    modifier: Modifier = Modifier
) {
    val excessOver7Lakh = (taxableIncome - 7_00_000L).coerceAtLeast(0L)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSuccessBg.copy(alpha = 0.5f)),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess.copy(alpha = 0.4f)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Section 87A Marginal Relief Shield",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    )
                }
                StatutoryBadge(text = "SAVED ₹${Rupee.formatInr(marginalReliefAmount)}", tone = BadgeTone.SUCCESS)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Under the proviso to Section 87A (New Regime), the tax payable on income exceeding ₹7,00,000 cannot exceed the excess income itself (₹${Rupee.formatInr(excessOver7Lakh)}).",
                style = MaterialTheme.typography.bodySmall,
                color = Slate800
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Standard Slab Tax", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    Text("₹${Rupee.formatInr(taxBeforeRelief)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Marginal Relief", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                    Text("-₹${Rupee.formatInr(marginalReliefAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Actual Tax Payable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("₹${Rupee.formatInr(finalTaxBeforeCess)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                }
            }
        }
    }
}

/**
 * Plan-based UI Tier Selector — rich detail cards showing full coverage per plan.
 * Each plan renders as an expanded Card with forms covered, eligibility, key features,
 * and income limit so the user can make an informed selection.
 */
@Composable
fun FilingTierSelectorCard(
    currentTier: FilingTierMode,
    recommendedTier: FilingTierMode,
    onTierSelected: (FilingTierMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Plan metadata displayed inside each detail card
    data class PlanDetail(
        val tier: FilingTierMode,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val eligibility: String,
        val incomeLimit: String,
        val forms: List<String>,
        val features: List<String>
    )

    val plans = listOf(
        PlanDetail(
            tier = FilingTierMode.SAHAJ_SIMPLE,
            icon = Icons.Default.Speed,
            eligibility = "Salaried, pensioners, one house property, interest income only",
            incomeLimit = "Total income up to Rs. 50 Lakh",
            forms = listOf("ITR-1 Sahaj"),
            features = listOf(
                "Salary & pension income (1 or 2 employers)",
                "Standard deduction Rs. 75,000",
                "Home loan interest (Sec 24b) — one property",
                "Chapter VI-A deductions: 80C, 80D, 80TTA",
                "Tax refund via pre-validated bank account",
                "New Tax Regime (NTR) vs Old Regime comparison"
            )
        ),
        PlanDetail(
            tier = FilingTierMode.INVESTOR_PRO,
            icon = Icons.Default.TrendingUp,
            eligibility = "Investors, freelancers, multiple properties, capital gains",
            incomeLimit = "No upper income limit (domestic income)",
            forms = listOf("ITR-2", "ITR-4 Sugam (44ADA/44AD)"),
            features = listOf(
                "Short-term capital gains Sec 111A (15% / 20%)",
                "Long-term capital gains Sec 112A (equity/MFs)",
                "Mutual funds, stocks & crypto gains",
                "Presumptive income Sec 44ADA (freelancers/professionals)",
                "Multiple house properties & rental income",
                "Foreign income (NRE interest, DTAA relief)",
                "Full Schedule 80C–80U deduction suite",
                "RSU & ESOP perquisite income"
            )
        ),
        PlanDetail(
            tier = FilingTierMode.ENTERPRISE_AUDIT,
            icon = Icons.Default.Shield,
            eligibility = "HNIs, business owners, audit cases, foreign assets",
            incomeLimit = "Turnover above Rs. 1 Crore or audit exposure",
            forms = listOf("ITR-2", "ITR-3", "ITR-5/6/7 (via CA dossier)", "Form 3CA/3CB/3CD (Tax Audit)"),
            features = listOf(
                "Business & professional income Sec 44AB",
                "Schedule FA — foreign assets & FEMA compliance",
                "Black Money Act (BMA) protection notices",
                "3-way AIS/26AS/Form 16 discrepancy radar",
                "Partnership firm & LLP income (Schedule BP)",
                "Advance tax installment planner",
                "CA collaboration dossier & audit trail",
                "Transfer pricing & international transactions"
            )
        )
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Filing Plan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldSuccessBg,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, EmeraldSuccess)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Selected: " + currentTier.title,
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                            )
                        }
                    }
                }
                Text(
                    text = "Tap any plan below to switch forms and coverage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatutoryBadge(text = "AY 2026-27", tone = BadgeTone.NEUTRAL)
        }

        plans.forEach { plan ->
            val isSelected = currentTier == plan.tier
            val isRecommended = recommendedTier == plan.tier

            val borderColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            val containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .animateContentSize()
                    .clickable { onTierSelected(plan.tier) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = if (isSelected) 2.dp else 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(borderColor)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header row: icon + title + badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = plan.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = plan.tier.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = plan.eligibility,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (isRecommended) {
                                Spacer(Modifier.height(2.dp))
                                StatutoryBadge(text = "RECOMMENDED", tone = BadgeTone.SUCCESS)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(10.dp))

                    // Income limit chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = plan.incomeLimit,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (isSelected) {
                        // Forms covered
                        Text(
                            text = "Forms Covered",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(plan.forms) { form ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = form,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Key features list
                        Text(
                            text = "What's Covered",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        plan.features.forEach { feature ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(13.dp).padding(top = 1.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        // Compact preview for Galaxy S23 viewport efficiency
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = plan.forms.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "Tap to switch →",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Comprehensive Tax Payment & Challan 280 Hub covering all payment conditions (Advance Tax, Self-Assessment,
 * Demand Assessment) across all official payment channels (UPI, NetBanking, Debit Card, RTGS/NEFT, Over-the-Counter).
 */
@Composable
fun TaxPaymentAndChallanHubModal(
    pan: String,
    taxPayableRupees: Long,
    assessmentYear: String = "2026-27",
    onDismiss: () -> Unit,
    onRecordChallan: (ScheduleItChallanEntry) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var selectedTaxType by remember {
        mutableStateOf(if (taxPayableRupees > 10_000L) ChallanTaxType.ADVANCE_TAX else ChallanTaxType.SELF_ASSESSMENT_TAX)
    }
    var selectedChannel by remember { mutableStateOf(TaxPaymentChannel.UPI_INSTANT) }
    var paymentAmountInput by remember { mutableStateOf(if (taxPayableRupees > 0L) taxPayableRupees.toString() else "10000") }

    // Challan 280 Verification States
    var showReceiptVerification by remember { mutableStateOf(false) }
    var bsrCodeInput by remember { mutableStateOf("") }
    var challanSerialInput by remember { mutableStateOf("") }
    var depositDateInput by remember { mutableStateOf("2026-03-15") }
    var verificationError by remember { mutableStateOf<String?>(null) }
    var isVerificationSuccess by remember { mutableStateOf(false) }

    val amountLong = paymentAmountInput.filter { it.isDigit() }.toLongOrNull() ?: taxPayableRupees

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = EnterpriseTeal600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Tax Payment & Challan 280 Hub",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "PAN: $pan • Major Head 0021",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate500)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Payment Condition / Head
                    item {
                        Text("1. Select Payment Condition & Head:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            ChallanTaxType.entries.forEach { type ->
                                FilterChip(
                                    selected = selectedTaxType == type,
                                    onClick = { selectedTaxType = type },
                                    label = { Text(type.description, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // 2. Amount Input
                    item {
                        OutlinedTextField(
                            value = paymentAmountInput,
                            onValueChange = { paymentAmountInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Tax Amount to Pay (₹)") },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 3. Payment Channels
                    item {
                        Text("2. Choose Payment Method (CBDT TIN 2.0 Authorized):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        TaxPaymentChannel.entries.forEach { channel ->
                            val isSelected = selectedChannel == channel
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { selectedChannel = channel },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) getTealHighlightBg() else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) {
                                    CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600))
                                } else CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val channelIcon = when (channel.iconName) {
                                        "bolt" -> Icons.Default.Bolt
                                        "account_balance" -> Icons.Default.AccountBalance
                                        "credit_card" -> Icons.Default.CreditCard
                                        "domain" -> Icons.Default.Domain
                                        else -> Icons.Default.Description
                                    }
                                    Icon(
                                        imageVector = channelIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) EnterpriseTeal700 else Slate600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = channel.title,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        )
                                        Text(
                                            text = "${channel.description} • Fee: ${channel.statutoryFee}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Action Button for Channel
                    item {
                        when (selectedChannel) {
                            TaxPaymentChannel.UPI_INSTANT -> {
                                Button(
                                    onClick = {
                                        val uri = UpiChallan280IntentLauncher.buildUpiPaymentUri(
                                            pan = pan,
                                            amountRupees = amountLong,
                                            assessmentYear = assessmentYear,
                                            taxType = selectedTaxType
                                        )
                                        UpiChallan280IntentLauncher.launchUpiPaymentIntent(context, uri)
                                        showReceiptVerification = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Launch UPI App (GPay / PhonePe / Paytm)")
                                    }
                                }
                            }
                            TaxPaymentChannel.NET_BANKING, TaxPaymentChannel.DEBIT_CARD -> {
                                Button(
                                    onClick = {
                                        uriHandler.openUri("https://eportal.incometax.gov.in/iec/foservices/#/e-pay-tax-prelogin/user-details")
                                        showReceiptVerification = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open e-Filing Portal TIN 2.0 Gateway")
                                    }
                                }
                            }
                            TaxPaymentChannel.RTGS_NEFT -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AmberWarningBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("RTGS / NEFT Mandate Instructions:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            "1. Login to eportal.incometax.gov.in -> e-Pay Tax -> Create New Challan.\n2. Choose 'RTGS/NEFT' to generate your unique Virtual Account Number (VAN) starting with 'ITD...'.\n3. Transfer ₹${Rupee.formatInr(amountLong)} via your bank NetBanking RTGS to this VAN.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            TaxPaymentChannel.OVER_THE_COUNTER -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Slate100),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Over-The-Counter (Cheque / Cash) Instructions:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            "Generate Form CRN Challan 280 on the e-Filing portal, print the physical mandate form, and present it with Cheque/DD at any authorized bank branch (SBI, HDFC, ICICI, PNB).",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Challan Receipt Validator & Schedule IT Auto-Injector
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Record Paid Challan to Return (Schedule IT)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    StatutoryBadge(text = "CBDT Verified", tone = BadgeTone.STATUTORY)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Enter the BSR code & Challan Serial Number from your payment receipt to offset your return tax liability:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate600
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = bsrCodeInput,
                                        onValueChange = { bsrCodeInput = it.take(7) },
                                        label = { Text("BSR Code (7 Digits)") },
                                        placeholder = { Text("0002134") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = challanSerialInput,
                                        onValueChange = { challanSerialInput = it.take(5) },
                                        label = { Text("Challan No (5 Digits)") },
                                        placeholder = { Text("01234") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = depositDateInput,
                                    onValueChange = { depositDateInput = it },
                                    label = { Text("Date of Deposit (YYYY-MM-DD)") },
                                    placeholder = { Text("2026-03-15") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                if (verificationError != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(verificationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                }

                                if (isVerificationSuccess) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Challan 280 verified & added to Schedule IT!",
                                            color = EmeraldSuccess,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val validation = UpiChallan280IntentLauncher.validateChallanReceipt(
                                            bsrCode = bsrCodeInput,
                                            challanSerialNo = challanSerialInput,
                                            depositDateIso = depositDateInput,
                                            pan = pan,
                                            amountRupees = amountLong
                                        )
                                        if (validation.isValid) {
                                            verificationError = null
                                            isVerificationSuccess = true
                                            val entry = ScheduleItChallanEntry(
                                                bsrCode7Digits = bsrCodeInput,
                                                challanSerialNumber5Digits = challanSerialInput,
                                                dateOfDepositIso = depositDateInput,
                                                taxAmountPaidRupees = amountLong
                                            )
                                            onRecordChallan(entry)
                                        } else {
                                            verificationError = validation.errors.firstOrNull() ?: "Invalid Challan"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Verify & Save to Schedule IT")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


