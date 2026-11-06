package `in`.zerotax.app.ui.chat

import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import `in`.zerotax.app.ui.security.ZeroKnowledgeTrustShieldModal
import `in`.zerotax.app.ui.components.WhatsAppEndToEndEncryptionBanner

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
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
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val fallbackBot = remember { ConversationalTaxFilingBot() }

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

    val uiState = viewModel?.uiState?.collectAsState()?.value
    var inputText by remember { mutableStateOf("") }
    var showTrustModal by remember { mutableStateOf(false) }

    val gross = uiState?.grossSalaryRupees ?: fallbackBot.snapshot.grossSalary
    val deductions = uiState?.let {
        val newDed = it.newRegimeResult.standardDeduction + it.newRegimeResult.totalChapterVIADeductions
        val oldDed = it.oldRegimeResult.standardDeduction + it.oldRegimeResult.totalChapterVIADeductions
        minOf(newDed, oldDed)
    } ?: fallbackBot.snapshot.totalDeductionsClaimed
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ZeroTaxTopAppBar(
                title = "ZeroTax AI Copilot",
                subtitle = "Interactive Filing • Private On-Device AI",
                pan = pan,
                onBack = onBack,
                badgeText = "Private AI",
                badgeTone = BadgeTone.SUCCESS
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Quick Action Review Button when ready
                    if (isReadyForReview) {
                        Button(
                            onClick = {
                                onProceedToFullReview(
                                    gross,
                                    maxOf(0L, gross - deductions),
                                    taxLiability,
                                    deductions,
                                    refundDue
                                )
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = "Review All Points & Submit to ITR Portal →",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Context-Aware Dynamic Prompt Starters
                    val dynamicPrompts = if (gross > 0L) {
                        listOf(
                            "Compare Regimes for Me",
                            "Simulate ₹50k NPS 80CCD(1B)",
                            "What if ₹25k Health Insurance 80D?",
                            "Can I Claim HRA & Home Loan Both?",
                            "Restructure CTC with 14% NPS"
                        )
                    } else {
                        listOf(
                            "Auto-Find Form 16 & Tax PDFs",
                            "1-Tap Sync AIS & 26AS",
                            "Which Regime is Better for Me?",
                            "How to Save More Tax?",
                            "80D Health Insurance Limits"
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        items(dynamicPrompts) { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable { handleSend(prompt) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = EnterpriseTeal600,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = prompt,
                                        style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }

                    // Quick Reply Chips from latest message
                    val latestQuickReplies = messages.lastOrNull { it.sender == ChatSender.BOT }?.quickReplies.orEmpty()
                    if (latestQuickReplies.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            items(latestQuickReplies) { reply ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                        .clickable {
                                            if (reply.contains("Review All Points")) {
                                                onProceedToFullReview(gross, maxOf(0L, gross - deductions), taxLiability, deductions, refundDue)
                                            } else {
                                                handleSend(reply)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = reply,
                                        style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }

                    // Text Input Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Ask Copilot (e.g. 'What if I put 50k in NPS?')",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = { handleSend(inputText) },
                            enabled = inputText.isNotBlank() && !isBotThinking,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (inputText.isNotBlank() && !isBotThinking) EnterpriseTeal600 else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank() && !isBotThinking) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Real-Time Return Snapshot Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
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
                        Column {
                            Text("Gross Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Rupee(gross).toString(),
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                        Column {
                            Text("Deductions Claimed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Rupee(deductions).toString(),
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSystemInDarkTheme()) IndigoStatutoryDark else IndigoStatutory)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net Refund Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Rupee(refundDue).toString(),
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSystemInDarkTheme()) EnterpriseTeal300 else EmeraldSuccess)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    val recRegime = uiState?.let {
                        if (it.newRegimeResult.totalTaxLiabilityRounded288B <= it.oldRegimeResult.totalTaxLiabilityRounded288B) "New Regime (u/s 115BAC)" else "Old Regime"
                    } ?: fallbackBot.snapshot.recommendedRegime
                    val saved = uiState?.let {
                        kotlin.math.abs(it.newRegimeResult.totalTaxLiabilityRounded288B - it.oldRegimeResult.totalTaxLiabilityRounded288B)
                    } ?: fallbackBot.snapshot.taxSavedByRecommendedRegime

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Recommended: $recRegime",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            )
                        }
                        if (saved > 0L) {
                            Text(
                                text = "Saves ₹${Rupee.formatInr(saved)}",
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) EnterpriseTeal300 else EmeraldSuccess)
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
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // WhatsApp-Style End-to-End Encryption Banner at top of chat
                item {
                    WhatsAppEndToEndEncryptionBanner(
                        onClick = { showTrustModal = true },
                        modifier = Modifier.padding(vertical = 8.dp),
                        customText = "🔒 Messages & tax calculations are end-to-end encrypted. No one outside of this chat, not even ZeroTax, can read them. Tap to verify."
                    )
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
        }

        if (showTrustModal) {
            ZeroKnowledgeTrustShieldModal(onDismiss = { showTrustModal = false })
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
            modifier = Modifier.widthIn(max = 310.dp),
            horizontalAlignment = if (isBot) Alignment.Start else Alignment.End
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = if (isBot) 2.dp else 12.dp,
                    topEnd = if (isBot) 12.dp else 2.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        !isBot -> EnterpriseTeal600
                        message.isAdvisorTip -> if (isSystemInDarkTheme()) AmberWarningBgDark else AmberWarningBg
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(
                        when {
                            !isBot -> EnterpriseTeal700
                            message.isAdvisorTip -> if (isSystemInDarkTheme()) AmberWarningDark else AmberWarning
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
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
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
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
