package `in`.zerotax.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import `in`.zerotax.core.model.Rupee
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
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val bot = remember { ConversationalTaxFilingBot() }

    var messages by remember { mutableStateOf(bot.getInitialGreeting()) }
    var inputText by remember { mutableStateOf("") }
    var isBotThinking by remember { mutableStateOf(false) }

    val currentSnapshot = bot.snapshot
    val isReadyForReview = bot.currentStep == ChatStep.COMPLETED_AWAITING_REVIEW

    // Scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EnterpriseTeal600),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ZeroTax Smart Chatbot",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Interactive TaxBuddy-Style Filing • On-Device AICore",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    StatutoryBadge(text = "PAN $pan", tone = BadgeTone.STATUTORY)
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    currentSnapshot.grossSalary,
                                    currentSnapshot.netTaxableIncome,
                                    currentSnapshot.estimatedTaxLiability,
                                    currentSnapshot.totalDeductionsClaimed,
                                    currentSnapshot.estimatedRefundDue
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

                    // Contextual Tax Advice FAQs (TaxBuddy Intelligence)
                    val faqPrompts = listOf(
                        "Which Regime is Better?",
                        "Can I Claim HRA & Home Loan Both?",
                        "80D Health Insurance Limits",
                        "NPS 80CCD(1B) ₹50k",
                        "How to Save More Tax?"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        items(faqPrompts) { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Slate100)
                                    .border(0.5.dp, Slate300, RoundedCornerShape(12.dp))
                                    .clickable {
                                        sendMessage(
                                            userText = prompt,
                                            bot = bot,
                                            messages = messages,
                                            onUpdateMessages = { messages = it },
                                            onThinkingChanged = { isBotThinking = it }
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "💡 $prompt",
                                    style = TextStyle(fontSize = 10.sp, color = Slate700, fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }

                    // Quick Reply Chips
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
                                        .background(EnterpriseTeal50)
                                        .border(0.75.dp, EnterpriseTeal600.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                        .clickable {
                                            if (reply.contains("Review All Points")) {
                                                onProceedToFullReview(
                                                    currentSnapshot.grossSalary,
                                                    currentSnapshot.netTaxableIncome,
                                                    currentSnapshot.estimatedTaxLiability,
                                                    currentSnapshot.totalDeductionsClaimed,
                                                    currentSnapshot.estimatedRefundDue
                                                )
                                            } else {
                                                sendMessage(
                                                    userText = reply,
                                                    bot = bot,
                                                    messages = messages,
                                                    onUpdateMessages = { messages = it },
                                                    onThinkingChanged = { isBotThinking = it }
                                                )
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = reply,
                                        style = TextStyle(fontSize = 12.sp, color = EnterpriseTeal900, fontWeight = FontWeight.SemiBold)
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
                            placeholder = { Text("Type salary, EMI, deductions, or queries...", style = TextStyle(fontSize = 13.sp, color = Slate400)) },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val txt = inputText
                                    inputText = ""
                                    sendMessage(
                                        userText = txt,
                                        bot = bot,
                                        messages = messages,
                                        onUpdateMessages = { messages = it },
                                        onThinkingChanged = { isBotThinking = it }
                                    )
                                }
                            },
                            enabled = inputText.isNotBlank() && !isBotThinking
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (inputText.isNotBlank()) EnterpriseTeal600 else Slate200),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("➤", color = PureWhite, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                            }
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Live Real-Time Return Snapshot Card with Dual-Regime Optimizer & Fluid Resizing
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
                            Text("Gross Income", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(
                                text = Rupee(currentSnapshot.grossSalary).toString(),
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            )
                        }
                        Column {
                            Text("Deductions Claimed", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(
                                text = Rupee(currentSnapshot.totalDeductionsClaimed).toString(),
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = IndigoStatutory)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net Refund Due", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(
                                text = Rupee(currentSnapshot.estimatedRefundDue).toString(),
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldSuccess)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(3.dp))
                            .background(EnterpriseTeal50)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆 Recommended: ${currentSnapshot.recommendedRegime}",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                        )
                        if (currentSnapshot.taxSavedByRecommendedRegime > 0) {
                            Text(
                                text = "Saves ₹${Rupee.formatInr(currentSnapshot.taxSavedByRecommendedRegime)}",
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                            )
                        }
                    }
                }
            }

            // Chat Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { msg ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(220)) + slideInVertically(tween(250)) { it / 4 }
                    ) {
                        Column {
                            ChatBubbleItem(message = msg)
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
            text = "ZeroTax is assembling return schedules...",
            style = TextStyle(fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun ChatBubbleItem(message: `in`.zerotax.feature.aiextractor.ChatMessage) {
    val isBot = message.sender == ChatSender.BOT

    Row(
        modifier = Modifier.fillMaxWidth(),
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
                Text("🤖", fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
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
                        message.isAdvisorTip -> AmberWarningBg
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(
                        when {
                            !isBot -> EnterpriseTeal700
                            message.isAdvisorTip -> AmberWarning
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (message.isAdvisorTip) {
                        StatutoryBadge(text = "💡 TaxBuddy Expert Guidance", tone = BadgeTone.WARNING)
                        Spacer(Modifier.height(4.dp))
                    }

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isBot) Slate800 else PureWhite
                        )
                    )

                    if (message.attachedDataSummary != null) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isBot) EmeraldSuccessBg else PureWhite.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✓ ${message.attachedDataSummary}",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBot) EmeraldSuccess else PureWhite
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = message.timestamp,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Slate400)
            )
        }
    }
}

private fun sendMessage(
    userText: String,
    bot: ConversationalTaxFilingBot,
    messages: List<`in`.zerotax.feature.aiextractor.ChatMessage>,
    onUpdateMessages: (List<`in`.zerotax.feature.aiextractor.ChatMessage>) -> Unit,
    onThinkingChanged: (Boolean) -> Unit
) {
    val userMsg = `in`.zerotax.feature.aiextractor.ChatMessage(
        sender = ChatSender.USER,
        text = userText
    )
    val nextMessages = messages + userMsg
    onUpdateMessages(nextMessages)

    onThinkingChanged(true)
    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
        delay(400) // Realistic conversational latency
        val botReplies = bot.processUserMessage(userText)
        onThinkingChanged(false)
        onUpdateMessages(nextMessages + botReplies)
    }
}
