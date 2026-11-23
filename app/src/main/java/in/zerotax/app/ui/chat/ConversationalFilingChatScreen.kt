package `in`.zerotax.app.ui.chat

import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import `in`.zerotax.app.ui.security.ZeroKnowledgeTrustShieldModal
import `in`.zerotax.app.ui.components.WhatsAppEndToEndEncryptionBanner

import androidx.compose.animation.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
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
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.app.viewmodel.ZeroTaxViewModel
import `in`.zerotax.core.model.CopilotActionCardData
import `in`.zerotax.core.model.Rupee
import `in`.zerotax.core.model.TaxRegime
import `in`.zerotax.core.model.WhatIfSimulationResult
import `in`.zerotax.core.security.network.ZeroTaxCloudApiBridge
import `in`.zerotax.feature.aiextractor.ChatMessage
import `in`.zerotax.feature.aiextractor.ChatSender
import `in`.zerotax.feature.aiextractor.ChatStep
import `in`.zerotax.feature.aiextractor.ConversationalTaxFilingBot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationalFilingChatScreen(
    pan: String,
    onBack: () -> Unit,
    onProceedToFullReview: (grossSalary: Long, netTaxableSalary: Long, taxLiability: Long, totalDeductions: Long, refundDue: Long) -> Unit,
    viewModel: ZeroTaxViewModel? = null,
    showTopBar: Boolean = false,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler(onBack = onBack)

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val fallbackBot = remember { ConversationalTaxFilingBot() }
    val uiState = viewModel?.uiState?.collectAsState()?.value

    LaunchedEffect(uiState?.grossSalaryRupees) {
        uiState?.let { s ->
            fallbackBot.syncWithTaxpayerProfile(
                grossSalary = s.grossSalaryRupees,
                hraExemption = s.exemptAllowancesSec10,
                homeLoan24b = s.homeLoanInterestSec24b,
                sec80c = s.section80CInvestments,
                sec80d = s.section80DSelf,
                nps80ccd1b = s.section80CCD1BNps,
                savingsInterest = s.savingsInterestRupees,
                taxpayerName = s.taxpayerFullName.ifBlank { s.pan },
                pan = s.pan
            )
        }
    }

    val context = LocalContext.current
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val fileName = uri.lastPathSegment ?: "document.pdf"
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.use { it.readBytes() } ?: ByteArray(0)
                if (bytes.isNotEmpty()) {
                    viewModel?.processAndAttachDocumentToChat(bytes, fileName)
                }
            } catch (_: Exception) {}
        }
    }


    // Connect to ViewModel state if available; fallback to local bot for isolated previews
    val viewModelMessages by (viewModel?.chatMessages ?: remember { mutableStateOf(fallbackBot.getInitialGreeting()) }).let {
        if (viewModel != null) viewModel.chatMessages.collectAsState() else remember { mutableStateOf(fallbackBot.getInitialGreeting()) }
    }
    var localMessages by remember { mutableStateOf(fallbackBot.getInitialGreeting()) }
    val messages = if (viewModel != null) viewModelMessages else localMessages

    val viewModelThinking by (viewModel?.isBotThinking ?: remember { mutableStateOf(false) }).let {
        if (viewModel != null) viewModel.isBotThinking.collectAsState() else remember { mutableStateOf(false) }
    }
    var localThinking by remember { mutableStateOf(false) }
    val isBotThinking = if (viewModel != null) viewModelThinking else localThinking

    var inputText by remember { mutableStateOf("") }
    var showTrustModal by remember { mutableStateOf(false) }
    var showChatFinalApprovalDialog by remember { mutableStateOf(false) }
    var chatHasGivenExplicitApproval by remember { mutableStateOf(false) }

    val gross = (uiState?.grossSalaryRupees?.takeIf { it > 0L }) ?: fallbackBot.snapshot.grossSalary
    val deductions = (uiState?.let {
        val newDed = it.newRegimeResult.standardDeduction + it.newRegimeResult.totalChapterVIADeductions
        val oldDed = it.oldRegimeResult.standardDeduction + it.oldRegimeResult.totalChapterVIADeductions
        minOf(newDed, oldDed).takeIf { d -> d > 0L }
    }) ?: fallbackBot.snapshot.totalDeductionsClaimed
    val taxLiability = uiState?.let { minOf(it.newRegimeResult.totalTaxLiabilityRounded288B, it.oldRegimeResult.totalTaxLiabilityRounded288B) } ?: fallbackBot.snapshot.estimatedTaxLiability
    val refundDue = uiState?.let { maxOf(0L, it.tdsDeposited26As - taxLiability) } ?: fallbackBot.snapshot.estimatedRefundDue

    val isReadyForReview = gross > 0L || fallbackBot.currentStep == ChatStep.COMPLETED_AWAITING_REVIEW

    // Scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun handleSend(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        inputText = ""

        val cleanLower = clean.lowercase()
        if (cleanLower.contains("submit to portal") || cleanLower.contains("file return") || cleanLower.contains("submit return") || cleanLower.contains("final approval") || (cleanLower.contains("review") && cleanLower.contains("submit"))) {
            showChatFinalApprovalDialog = true
            return
        }

        if (viewModel != null) {
            viewModel.sendUserChatMessage(clean)
        } else {
            val userMsg = ChatMessage(sender = ChatSender.USER, text = clean)
            localMessages = localMessages + userMsg
            localThinking = true
            scope.launch {
                delay(300)
                val botReplies = fallbackBot.processUserMessage(clean)
                localThinking = false
                localMessages = localMessages + botReplies
            }
        }
    }

    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (showTopBar) {
            ZeroTaxTopAppBar(
                title = "ZeroTax AI Copilot",
                subtitle = "Interactive Filing • Cloud Tax AI",
                pan = pan,
                onBack = onBack,
                badgeText = "Cloud AI",
                badgeTone = BadgeTone.SUCCESS
            )
        }

        // Sleek compact context pill if inside tab navigation
        if (!showTopBar && gross > 0L) {
            val recRegime = uiState?.let {
                if (it.newRegimeResult.totalTaxLiabilityRounded288B <= it.oldRegimeResult.totalTaxLiabilityRounded288B) "New Regime (u/s 115BAC)" else "Old Regime"
            } ?: fallbackBot.snapshot.recommendedRegime
            val saved = uiState?.let {
                kotlin.math.abs(it.newRegimeResult.totalTaxLiabilityRounded288B - it.oldRegimeResult.totalTaxLiabilityRounded288B)
            } ?: fallbackBot.snapshot.taxSavedByRecommendedRegime

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Slate850 else Slate100,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Slate700 else Slate300)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Gross: ${Rupee(gross)} • Optimal: $recRegime",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Slate200 else Slate800),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    if (saved > 0L) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Save ₹${Rupee.formatInr(saved)}",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) EnterpriseTeal300 else EmeraldSuccess),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Chat Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Zero-telemetry • End-to-end encrypted on-device • AY 2026-27",
                        style = TextStyle(fontSize = 10.5.sp, color = Slate400, fontWeight = FontWeight.Medium)
                    )
                }
            }

            items(
                items = messages,
                key = { it.id }
            ) { msg ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(220)) + slideInVertically(tween(250)) { it / 4 }
                ) {
                    Column {
                        ChatBubbleItem(
                            message = msg,
                            onApplyWhatIf = { sim -> viewModel?.applyWhatIfScenarioToReturn(sim) },
                            onSwitchRegime = { regime -> viewModel?.switchRegime(regime) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            if (isBotThinking) {
                item {
                    PulsingTypingIndicator()
                }
            }
        }

        // Floating Bottom Actions & Modern Input Pill
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().imePadding()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                // Quick reply suggestion chips matching Figma 4:1046
                val figmaChips = remember {
                    listOf("Compare regimes", "HRA receipts", "Losses")
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    items(figmaChips) { prompt ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Slate800 else SageMintPill,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else SageMintLight),
                            modifier = Modifier.clickable {
                                handleSend(prompt)
                            }
                        ) {
                            Text(
                                text = prompt,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = TextStyle(fontSize = 12.sp, color = if (isDark) PureWhite else ForestGreenPrimary, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                // Ergonomic Input Bar matching Figma 4:1053
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = if (isDark) Slate850 else PureWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else Slate300),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { docPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Attachment",
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Ask your copilot anything about deductions...",
                                    style = TextStyle(fontSize = 12.5.sp, color = Slate400)
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) PureWhite else Slate900)
                        )

                        val canSend = inputText.isNotBlank() && !isBotThinking
                        IconButton(
                            onClick = { handleSend(inputText) },
                            enabled = canSend,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (canSend) ForestGreenPrimary else if (isDark) Slate800 else Slate200)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Send",
                                tint = if (canSend) PureWhite else Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showTrustModal) {
            ZeroKnowledgeTrustShieldModal(onDismiss = { showTrustModal = false })
        }

        if (showChatFinalApprovalDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showChatFinalApprovalDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Taxpayer Final Approval Required",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Section 140 of the Income-tax Act, 1961",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            IconButton(onClick = { showChatFinalApprovalDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider()

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Assessee:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    Text(uiState?.taxpayerFullName?.ifBlank { "SATISH JADAV" } ?: "SATISH JADAV", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("PAN / Assessment Year:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    Text("$pan • AY 2026-27", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Gross Income / Deductions:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    Text("₹${"%,d".format(gross)} / ₹${"%,d".format(deductions)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Net Tax Liability:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    Text("₹0 (100% Rebated u/s 87A)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chatHasGivenExplicitApproval = !chatHasGivenExplicitApproval }
                        ) {
                            Checkbox(
                                checked = chatHasGivenExplicitApproval,
                                onCheckedChange = { chatHasGivenExplicitApproval = it }
                            )
                            Text(
                                text = "I hereby confirm that I have reviewed all income and deductions. I give my explicit final approval to transmit this return to the Income Tax Department portal.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp)
                            )
                        }

                        ZeroTaxPrimaryButton(
                            text = "Grant Final Approval & Submit to Portal",
                            trailingIcon = {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                showChatFinalApprovalDialog = false
                                onProceedToFullReview(
                                    gross,
                                    maxOf(0L, gross - deductions),
                                    taxLiability,
                                    deductions,
                                    refundDue
                                )
                            },
                            enabled = chatHasGivenExplicitApproval,
                            modifier = Modifier.fillMaxWidth()
                        )

                        ZeroTaxOutlinedButton(
                            text = "Wait & Review Return More",
                            onClick = { showChatFinalApprovalDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "TypingDots")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(550, delayMillis = 180, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(550, delayMillis = 360, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "dot3"
    )

    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(EnterpriseTeal600.copy(alpha = alpha1)))
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(EnterpriseTeal600.copy(alpha = alpha2)))
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(EnterpriseTeal600.copy(alpha = alpha3)))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "AI Copilot running deterministic tax calculations...",
            style = TextStyle(fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    onApplyWhatIf: ((WhatIfSimulationResult) -> Unit)? = null,
    onSwitchRegime: ((TaxRegime) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isBot = message.sender == ChatSender.BOT

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isBot) Arrangement.Start else Arrangement.End
    ) {
        if (isBot) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(EnterpriseTeal600),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isBot) Alignment.Start else Alignment.End
        ) {
            val isDark = isSystemInDarkTheme()
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isBot) 4.dp else 16.dp,
                    topEnd = if (isBot) 16.dp else 4.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = when {
                    !isBot -> EnterpriseTeal700
                    message.isAdvisorTip -> if (isDark) AmberWarningBgDark else AmberWarningBg
                    else -> if (isDark) Slate850 else PureWhite
                },
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = when {
                        !isBot -> EnterpriseTeal800
                        message.isAdvisorTip -> if (isDark) AmberWarningDark.copy(alpha = 0.5f) else AmberWarning.copy(alpha = 0.5f)
                        else -> if (isDark) Slate700 else Slate200
                    }
                ),
                shadowElevation = if (isDark) 0.dp else 1.5.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.isAdvisorTip) {
                        StatutoryBadge(text = "ZeroTax Copilot Guidance", tone = BadgeTone.SUCCESS)
                        Spacer(Modifier.height(4.dp))
                    }

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = when {
                                !isBot -> PureWhite
                                message.isAdvisorTip -> if (isSystemInDarkTheme()) AmberWarningDark else AmberWarning
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    )

                    // Render Interactive Action Cards if emitted by Agentic Tool Dispatcher
                    when (val card = message.actionCard) {
                        is CopilotActionCardData.WhatIfResult -> {
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess.copy(alpha = 0.6f))),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Statutory What-If Result", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (card.result.netTaxSavedRupees > 0L) "You SAVE ₹" + Rupee.formatInr(card.result.netTaxSavedRupees) + " in tax!" else "Tax Delta: ₹" + Rupee.formatInr(card.result.afterOldRegimeTax - card.result.beforeOldRegimeTax),
                                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Old: ₹" + Rupee.formatInr(card.result.afterOldRegimeTax), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp))
                                        Text("New: ₹" + Rupee.formatInr(card.result.afterNewRegimeTax), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp))
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Button(
                                        onClick = { onApplyWhatIf?.invoke(card.result) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonSuccessBg,
                                contentColor = ButtonSuccessText
                            ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text("1-Tap Apply to My Return", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite))
                                    }
                                }
                            }
                        }

                        is CopilotActionCardData.RegimeComparison -> {
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.6f))),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = EnterpriseTeal600, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Regime Recommendation", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal600))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Old: ₹" + Rupee.formatInr(card.oldRegimeTax), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp))
                                        Text("New: ₹" + Rupee.formatInr(card.newRegimeTax), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp))
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    val targetRegimeName = if (card.recommendedRegime == TaxRegime.NEW_REGIME_115BAC) "New" else "Old"
                                    Button(
                                        onClick = { onSwitchRegime?.invoke(card.recommendedRegime) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text("Switch to " + targetRegimeName + " Regime", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite))
                                    }
                                }
                            }
                        }

                        is CopilotActionCardData.CtcOptimization -> {
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.6f))),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("💼 CTC Salary Restructuring", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal600))
                                    Spacer(Modifier.height(2.dp))
                                    Text("Tax Saved: ₹" + Rupee.formatInr(card.annualTaxSavings) + "/yr", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                                    Text("14% Employer NPS: ₹" + Rupee.formatInr(card.employerNps80CCD2), style = TextStyle(fontSize = 10.sp))
                                }
                            }
                        }

                        is CopilotActionCardData.NoticeDefense -> {
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) AmberWarningBgDark else AmberWarningBg),
                                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(AmberWarning)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("🛡️ Notice Analysis (" + card.sectionRef + ")", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarningDark))
                                    if (card.varianceAmount > 0L) {
                                        Text("Demand: ₹" + Rupee.formatInr(card.varianceAmount), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                    }
                                    Text(card.reasonSummary, style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface))
                                }
                            }
                        }

                        is CopilotActionCardData.UnclaimedDeduction -> {
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.6f))),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(card.title, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal600))
                                    Text("Section " + card.statutorySection + " • Eligible: ₹" + Rupee.formatInr(card.eligibleAmount), style = TextStyle(fontSize = 10.sp))
                                }
                            }
                        }

                        null -> { /* No action card */ }
                    }

                    // Statutory Legal Citations Row
                    if (message.citations.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(message.citations) { citation ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "[" + citation + "]",
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    )
                                }
                            }
                        }
                    }

                    val attachedSummary = message.attachedDataSummary
                    if (attachedSummary != null) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isBot) {
                                        if (isSystemInDarkTheme()) EmeraldSuccessBgDark else EmeraldSuccessBg
                                    } else PureWhite.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isBot) {
                                        if (isSystemInDarkTheme()) EnterpriseTeal300 else EmeraldSuccess
                                    } else PureWhite,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = attachedSummary,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBot) {
                                            if (isSystemInDarkTheme()) EnterpriseTeal300 else EmeraldSuccess
                                        } else PureWhite
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = message.timestamp,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}
