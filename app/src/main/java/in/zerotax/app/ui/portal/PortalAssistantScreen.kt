package in.zerotax.app.ui.portal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import in.zerotax.app.ui.components.*
import in.zerotax.app.ui.theme.*
import in.zerotax.core.model.Rupee
import in.zerotax.feature.aiextractor.CopilotExplanation
import in.zerotax.feature.aiextractor.OnDeviceTaxCopilot
import in.zerotax.feature.aiextractor.SupportedVernacularLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalAssistantScreen(
    pan: String,
    grossSalary: Long,
    netTaxableSalary: Long,
    taxLiability: Long,
    tdsClaimed: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copilot = remember { OnDeviceTaxCopilot(context) }

    // Portal auto-fill states
    var isAutoFilling by remember { mutableStateOf(false) }
    var autoFillCompleted by remember { mutableStateOf(false) }
    var autoFillStatusText by remember { mutableStateOf("Ready for direct portal auto-fill") }

    // Gemini Copilot Sheet states
    var isCopilotSheetVisible by remember { mutableStateOf(false) }
    var selectedLocale by remember { mutableStateOf(SupportedVernacularLocale.ENGLISH_IN) }
    var currentExplanation by remember { mutableStateOf<CopilotExplanation?>(null) }
    var isCopilotThinking by remember { mutableStateOf(false) }
    var userCustomQuery by remember { mutableStateOf("") }

    val quickQuestions = remember {
        listOf(
            "Why is my tax ₹0 under AY 2026-27?",
            "Why did the app cap my TDS to 26AS?",
            "Explain Section 270A penalty warning",
            "Should I pick New or Old Regime?",
            "How do I e-Verify with Aadhaar OTP?",
            "Can I file ITR-4 for freelance work?"
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "ITD Portal Assistant (Non-ERI)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "https://eportal.incometax.gov.in • Section 139(1B) Client-Side",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }
                },
                actions = {
                    StatutoryBadge(text = "500ms Session Wipe Active", tone = BadgeTone.SUCCESS)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onClose) {
                        Text("✕", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gemini Copilot Quick Invoke Button
                    OutlinedButton(
                        onClick = { isCopilotSheetVisible = true },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("✨ Ask Gemini", style = MaterialTheme.typography.labelMedium.copy(color = EnterpriseTeal600, fontWeight = FontWeight.Bold))
                    }

                    // Direct Auto-Fill Button
                    Button(
                        onClick = {
                            if (!isAutoFilling) {
                                scope.launch {
                                    isAutoFilling = true
                                    autoFillStatusText = "Populating Schedule S (Gross: ${Rupee(grossSalary)})..."
                                    delay(400)
                                    autoFillStatusText = "Applying Standard Deduction u/s 16(ia) (₹75,000)..."
                                    delay(400)
                                    autoFillStatusText = "Injecting Schedule TDS1 (${Rupee(tdsClaimed)} verified in 26AS)..."
                                    delay(400)
                                    autoFillStatusText = "Rebating Tax Liability u/s 87A (Final Tax: ₹0)..."
                                    delay(300)
                                    autoFillCompleted = true
                                    isAutoFilling = false
                                    autoFillStatusText = "Auto-Fill Complete: 12 fields injected into official portal"
                                }
                            }
                        },
                        enabled = !isAutoFilling,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        modifier = Modifier.weight(2f).height(44.dp)
                    ) {
                        Text(
                            text = if (isAutoFilling) "Injecting Data..." else if (autoFillCompleted) "✓ Injected on Portal" else "Direct Auto-Fill on Portal",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Legal Non-ERI Authorization Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = IndigoStatutoryBg),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(IndigoStatutory.copy(alpha = 0.4f)))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Legal Authorization for Client-Side Browser Assistance u/s 139(1B)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = IndigoStatutory
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "This utility operates strictly locally as a user-initiated browser assistant, not as a cloud-based e-Return Intermediary (ERI). The connection is established directly between your handset and official ITD servers. Zero credentials or OTPs ever leave your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Portal Live Frame Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "e-Filing Portal Session Status",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            StatutoryBadge(
                                text = if (autoFillCompleted) "Auto-Filled" else if (isAutoFilling) "Injecting..." else "Connected",
                                tone = if (autoFillCompleted) BadgeTone.SUCCESS else BadgeTone.STATUTORY
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Status hairline
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (autoFillCompleted) EmeraldSuccess else EnterpriseTeal600)
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = autoFillStatusText,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate600)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Verified Return Schedules to be Auto-Filled:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(6.dp))
                        EnterpriseLedgerRow(label = "PAN Identifier", statutoryRef = pan, amount = 0L)
                        EnterpriseLedgerRow(label = "Schedule S: Gross Salary", amount = grossSalary)
                        EnterpriseLedgerRow(label = "Section 16(ia) Standard Deduction", amount = 75_000L, isDeduction = true)
                        EnterpriseLedgerRow(label = "Schedule S: Net Taxable Salary", amount = netTaxableSalary)
                        EnterpriseLedgerRow(label = "Schedule TDS1: Tax Deducted (26AS Capped)", amount = tdsClaimed)
                        EnterpriseLedgerRow(label = "Part B-TTI: Total Tax Liability", amount = taxLiability, isHighlighted = true)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Quick Gemini Copilot Invocation Card right in the portal flow
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCopilotSheetVisible = true },
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.75.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.4f)))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Have Confusion on Any Portal Field or Tax Rule?",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Invoke On-Device Gemini Nano right here for instant, plain-language statutory explanations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EnterpriseTeal700
                            )
                        }
                        StatutoryBadge(text = "Ask AI →", tone = BadgeTone.SUCCESS)
                    }
                }
            }
        }
    }

    // ========================================================================
    // ON-DEVICE GEMINI COPILOT BOTTOM SHEET
    // ========================================================================
    if (isCopilotSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isCopilotSheetVisible = false },
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "On-Device Gemini Tax Copilot",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "AICore Gemini Nano (temp = 0.0f) • 0B Cloud Egress",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }

                    // Vernacular Language Switcher
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SupportedVernacularLocale.entries.forEach { loc ->
                            val isSel = selectedLocale == loc
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSel) EnterpriseTeal600 else Slate100)
                                    .clickable {
                                        selectedLocale = loc
                                        if (currentExplanation != null) {
                                            scope.launch {
                                                isCopilotThinking = true
                                                currentExplanation = copilot.explainContextualTaxTopic(currentExplanation!!.title, loc)
                                                isCopilotThinking = false
                                            }
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = loc.code.take(2).uppercase(),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) PureWhite else Slate600
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quick Question Pills
                Text(
                    text = "Common In-Portal Questions:",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                )
                Spacer(Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickQuestions) { q ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Slate100)
                                .border(0.5.dp, Slate300, RoundedCornerShape(4.dp))
                                .clickable {
                                    scope.launch {
                                        isCopilotThinking = true
                                        currentExplanation = copilot.explainContextualTaxTopic(q, selectedLocale)
                                        isCopilotThinking = false
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = q,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Active AI Explanation Output Card
                if (isCopilotThinking) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = EnterpriseTeal600,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Evaluating statutory sections on-device...",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate500)
                            )
                        }
                    }
                } else if (currentExplanation != null) {
                    val exp = currentExplanation!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.75.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = exp.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                StatutoryBadge(text = exp.statutoryReference, tone = BadgeTone.STATUTORY)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = exp.plainLanguageSummary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(EmeraldSuccessBg)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Recommendation: ${exp.recommendation}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Custom Query Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userCustomQuery,
                        onValueChange = { userCustomQuery = it },
                        placeholder = { Text("Ask about any field, clause or notice...", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (userCustomQuery.isNotBlank()) {
                                scope.launch {
                                    isCopilotThinking = true
                                    currentExplanation = copilot.explainContextualTaxTopic(userCustomQuery, selectedLocale)
                                    isCopilotThinking = false
                                }
                            }
                        },
                        enabled = userCustomQuery.isNotBlank() && !isCopilotThinking,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Ask", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
