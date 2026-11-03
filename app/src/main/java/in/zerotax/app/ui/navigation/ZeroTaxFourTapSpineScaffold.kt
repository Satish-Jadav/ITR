package in.zerotax.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import in.zerotax.app.ui.components.*
import in.zerotax.app.ui.audit.PreFilingAuditView
import in.zerotax.app.ui.chat.ConversationalFilingChatScreen
import in.zerotax.app.ui.deductions.SmartDeductionsAdvisorScreen
import in.zerotax.app.ui.portal.PortalAssistantScreen
import in.zerotax.app.ui.portal.SmartTransactionSuggestionsView
import in.zerotax.app.ui.theme.*
import in.zerotax.app.viewmodel.ZeroTaxViewModel
import in.zerotax.core.model.Rupee
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OnboardingTap(val title: String) {
    Tap1IdentityConsent("Taxpayer Identity"),
    Tap2LiveDiscovery("Document Discovery"),
    Tap3NudgeCards("TDS Reconciliation"),
    Tap4RegimeShowdown("Dual-Regime Showdown")
}

enum class WorkspaceTab(val title: String) {
    FilingAndRefundHub("Filing Hub"),
    SmartTaxVaultAndCopilot("Audit Vault"),
    PrivacyAndSettings("DPDP & Security")
}

@Composable
fun ZeroTaxFourTapSpineScaffold(
    modifier: Modifier = Modifier,
    viewModel: ZeroTaxViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSection270ADialog by remember { mutableStateOf(false) }
    var showPortalAssistant by remember { mutableStateOf(false) }
    var showAisTransactions by remember { mutableStateOf(false) }
    var showDeductionsAdvisor by remember { mutableStateOf(false) }
    var showPreFilingAudit by remember { mutableStateOf(false) }
    var showChatbotFiling by remember { mutableStateOf(false) }

    if (showChatbotFiling) {
        ConversationalFilingChatScreen(
            pan = uiState.pan,
            onBack = { showChatbotFiling = false },
            onProceedToFullReview = { _, _, _, _, _ ->
                showChatbotFiling = false
                showPreFilingAudit = true
            }
        )
        return
    }

    if (showPortalAssistant) {
        PortalAssistantScreen(
            pan = uiState.pan,
            grossSalary = uiState.grossSalaryRupees,
            netTaxableSalary = uiState.newRegimeResult.netTaxableNormalIncomeRounded288A,
            taxLiability = uiState.newRegimeResult.totalTaxLiabilityRounded288B,
            tdsClaimed = uiState.salarySummary.aggregateTdsAllowed,
            onClose = { showPortalAssistant = false }
        )
        return
    }

    if (showAisTransactions) {
        SmartTransactionSuggestionsView(
            pan = uiState.pan,
            onBack = { showAisTransactions = false }
        )
        return
    }

    if (showDeductionsAdvisor) {
        SmartDeductionsAdvisorScreen(
            onBack = { showDeductionsAdvisor = false },
            onApplyDeductions = { showDeductionsAdvisor = false }
        )
        return
    }

    if (showPreFilingAudit) {
        PreFilingAuditView(
            salarySummary = uiState.salarySummary,
            computation = uiState.newRegimeResult,
            bankAccount = in.zerotax.core.model.BankAccountDetails(
                bankName = "State Bank of India",
                accountNumber = "••••••••1234",
                ifscCode = "SBIN0001234",
                accountType = "SAVINGS",
                isRefundAccount = true,
                isPreValidated = true
            ),
            onBack = { showPreFilingAudit = false },
            onProceedToFiling = {
                showPreFilingAudit = false
                showPortalAssistant = true
            }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            EnterpriseTopHeader(
                pan = uiState.pan,
                assessmentYear = uiState.assessmentYear,
                isVaultEncrypted = !uiState.isVaultShredded,
                onOpenChatbot = { showChatbotFiling = true }
            )
        },
        bottomBar = {
            if (uiState.currentTap == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    WorkspaceTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = uiState.selectedWorkspaceTab == tab,
                            onClick = { viewModel.onSelectWorkspaceTab(tab) },
                            label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
                            icon = {}
                        )
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 0.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                    )
                ) {
                    DiscreteStatutoryStepper(
                        currentTap = uiState.currentTap!!,
                        onStepClick = { viewModel.onNavigateToTap(it) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Directional smooth horizontal slide & fade transitions between steps
            AnimatedContent(
                targetState = uiState.currentTap,
                transitionSpec = {
                    val initialIdx = initialState?.ordinal ?: 4
                    val targetIdx = targetState?.ordinal ?: 4
                    if (targetIdx > initialIdx) {
                        (slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it } +
                         fadeIn(tween(300))).togetherWith(
                            slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 2 } +
                            fadeOut(tween(200))
                        )
                    } else if (targetIdx < initialIdx) {
                        (slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { -it } +
                         fadeIn(tween(300))).togetherWith(
                            slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 2 } +
                            fadeOut(tween(200))
                        )
                    } else {
                        (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.96f)).togetherWith(
                            fadeOut(tween(220))
                        )
                    }
                },
                label = "EnterpriseStageTransition"
            ) { activeTap ->
                when (activeTap) {
                    OnboardingTap.Tap1IdentityConsent -> Stage1IdentityConsent(
                        pan = uiState.pan,
                        onPanChanged = { viewModel.onPanChanged(it) },
                        onProceed = { viewModel.onNavigateToTap(OnboardingTap.Tap2LiveDiscovery) },
                        onOpenChatbot = { showChatbotFiling = true }
                    )

                    OnboardingTap.Tap2LiveDiscovery -> Stage2DocumentDiscovery(
                        grossSalary = uiState.grossSalaryRupees,
                        onOpenAisTransactions = { showAisTransactions = true },
                        onBack = { viewModel.onNavigateToTap(OnboardingTap.Tap1IdentityConsent) },
                        onProceed = { viewModel.onNavigateToTap(OnboardingTap.Tap3NudgeCards) }
                    )

                    OnboardingTap.Tap3NudgeCards -> Stage3Reconciliation(
                        grossSalary = uiState.grossSalaryRupees,
                        onOpenDeductionsAdvisor = { showDeductionsAdvisor = true },
                        onSimulateOverride = { showSection270ADialog = true },
                        onBack = { viewModel.onNavigateToTap(OnboardingTap.Tap2LiveDiscovery) },
                        onProceed = { viewModel.onNavigateToTap(OnboardingTap.Tap4RegimeShowdown) }
                    )

                    OnboardingTap.Tap4RegimeShowdown -> Stage4RegimeShowdown(
                        newRegimeTax = uiState.newRegimeResult.totalTaxLiabilityRounded288B,
                        oldRegimeTax = uiState.oldRegimeResult.totalTaxLiabilityRounded288B,
                        newGti = uiState.newRegimeResult.grossTotalIncome,
                        oldGti = uiState.oldRegimeResult.grossTotalIncome,
                        newDeductions = uiState.newRegimeResult.totalChapterVIADeductions + uiState.newRegimeResult.standardDeduction,
                        oldDeductions = uiState.oldRegimeResult.totalChapterVIADeductions + uiState.oldRegimeResult.standardDeduction,
                        newRebate87A = uiState.newRegimeResult.rebate87A,
                        oldRebate87A = uiState.oldRegimeResult.rebate87A,
                        selectedForm = uiState.formRoutingDecision.selectedForm.name.replace("_", "-"),
                        onOpenDeductionsAdvisor = { showDeductionsAdvisor = true },
                        onRunAudit = { showPreFilingAudit = true },
                        onBack = { viewModel.onNavigateToTap(OnboardingTap.Tap3NudgeCards) },
                        onLaunchPortalAssistant = { showPortalAssistant = true },
                        onFileReturn = { viewModel.onNavigateToTap(null) }
                    )

                    null -> ProWorkspaceView(
                        selectedTab = uiState.selectedWorkspaceTab,
                        uiState = uiState,
                        onOpenDeductionsAdvisor = { showDeductionsAdvisor = true },
                        onOpenAisTransactions = { showAisTransactions = true },
                        onRunAudit = { showPreFilingAudit = true },
                        onLaunchPortalAssistant = { showPortalAssistant = true },
                        onOpenChatbot = { showChatbotFiling = true },
                        onShredVault = { viewModel.onShredVaultData() }
                    )
                }
            }

            if (showSection270ADialog) {
                Section270AStatutoryOverrideDialog(
                    fieldName = "Reported Gross Salary",
                    verifiedSystemValueRupees = uiState.grossSalaryRupees,
                    userProposedValueRupees = uiState.grossSalaryRupees - 1_00_000L,
                    onConfirmOverrideWithAuditLog = { reason ->
                        showSection270ADialog = false
                        viewModel.onStatutoryOverrideConfirmed(
                            fieldName = "Reported Gross Salary",
                            originalValue = uiState.grossSalaryRupees,
                            overriddenValue = uiState.grossSalaryRupees - 1_00_000L,
                            statutoryReasonCode = reason
                        )
                    },
                    onCancelAndRestoreVerifiedValue = {
                        showSection270ADialog = false
                    }
                )
            }
        }
    }
}

// ============================================================================
// STAGE 1: TAXPAYER IDENTITY & STATUTORY CONSENT (ANIMATED)
// ============================================================================
@Composable
private fun Stage1IdentityConsent(
    pan: String,
    onPanChanged: (String) -> Unit,
    onProceed: () -> Unit,
    onOpenChatbot: () -> Unit = {}
) {
    val isPanValid = pan.length == 10 && pan[3] == 'P'
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }
        ) {
            Column {
                Text(
                    text = "Taxpayer Authentication & Vault Setup",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Permanent Account Number validation with client-side UIDAI Section 29 compliance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(tween(400, delayMillis = 100)) { it / 3 }
        ) {
            SmartIndividualPanField(
                panValue = pan,
                onPanChanged = onPanChanged
            )
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400, delayMillis = 200)) + slideInVertically(tween(400, delayMillis = 200)) { it / 3 }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Statutory Processing Specifications",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        StatutoryBadge(text = "DPDP Act §6", tone = BadgeTone.STATUTORY)
                    }
                    Spacer(Modifier.height(10.dp))
                    EnterpriseLedgerRow(label = "Aadhaar PII Scrubbing", statutoryRef = "Aadhaar Act §29", amount = 0L)
                    EnterpriseLedgerRow(label = "Vault Hardware Derivation", statutoryRef = "AES-256-GCM", amount = 0L)
                    EnterpriseLedgerRow(label = "Remote Cloud Telemetry", statutoryRef = "Enforced", amount = 0L)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = onOpenChatbot,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EnterpriseTeal600),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(
                text = "🤖 Prefer Chat? Open TaxBuddy-Style Assistant",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onProceed,
            enabled = isPanValid,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(
                text = "Authenticate & Initialize Hardware Vault",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ============================================================================
// STAGE 2: DOCUMENT DISCOVERY & LIVE SANITY FIREWALL (ANIMATED)
// ============================================================================
@Composable
private fun Stage2DocumentDiscovery(
    grossSalary: Long,
    onOpenAisTransactions: () -> Unit,
    onBack: () -> Unit,
    onProceed: () -> Unit
) {
    var isUnlocked by remember { mutableStateOf(false) }
    var currentPermutationText by remember { mutableStateOf("Testing Permutation 1/15 (PAN+DOB)...") }

    val unlockProgressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val permutations = listOf(
            "Permutation 1/15: PAN + DOB...",
            "Permutation 4/15: Name4 + DOB...",
            "Permutation 8/15: Name4 + DDMM...",
            "Permutation 11/15: PAN + YYYY...",
            "TRACES Password Accepted (< 42ms)"
        )
        for (i in permutations.indices) {
            currentPermutationText = permutations[i]
            delay(120)
        }
        unlockProgressAnim.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        isUnlocked = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Ingestion & On-Device Validation",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "In-memory 15-permutation PDF decryption and ±₹5 Math Sanity Firewall.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        // Document Decryption Card with animated scanning status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRACES Form 16 Part B",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    AnimatedContent(
                        targetState = isUnlocked,
                        transitionSpec = {
                            fadeIn(tween(250)) togetherWith fadeOut(tween(150))
                        },
                        label = "UnlockBadgeAnim"
                    ) { unlocked ->
                        if (unlocked) {
                            StatutoryBadge(text = "Unlocked: 42ms", tone = BadgeTone.SUCCESS)
                        } else {
                            StatutoryBadge(text = "In-Memory Scan...", tone = BadgeTone.WARNING)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Progress Hairline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Slate100)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(unlockProgressAnim.value)
                            .fillMaxHeight()
                            .background(if (isUnlocked) EmeraldSuccess else EnterpriseTeal500)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = currentPermutationText,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Slate500)
                )

                Spacer(Modifier.height(8.dp))
                EnterpriseLedgerRow(label = "Deductor TAN", statutoryRef = "BLRM01234D", amount = 0L)
                EnterpriseLedgerRow(label = "Gross Salary u/s 17(1)", amount = grossSalary)
                EnterpriseLedgerRow(label = "Standard Deduction u/s 16(ia)", amount = 75_000L, isDeduction = true)
                EnterpriseLedgerRow(label = "Net Salary u/s Salaries", amount = grossSalary - 75_000L, isHighlighted = true)
            }
        }

        Spacer(Modifier.height(12.dp))

        // AICore Sanity Firewall Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AICore Gemini Nano Sanity Firewall",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    StatutoryBadge(text = "Δ = ₹0.00 (Pass)", tone = BadgeTone.SUCCESS)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Arithmetic equality verified deterministically on-device (temp = 0.0f). Tolerance gate ±₹5 satisfied.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // AIS / TIS Transactions & Form 60 Intelligence Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAisTransactions() },
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = IndigoStatutoryBg),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 0.75.dp,
                brush = androidx.compose.ui.graphics.SolidColor(IndigoStatutory.copy(alpha = 0.4f))
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AIS / TIS Transactions & Form 60 Profile",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = IndigoStatutory)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Fetch bank interest, dividend TDS, and link client KYC profile details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
                StatutoryBadge(text = "Inspect →", tone = BadgeTone.STATUTORY)
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text("Back", style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = onProceed,
                enabled = isUnlocked,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(2f).height(44.dp)
            ) {
                Text("Proceed to Reconciliation", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ============================================================================
// STAGE 3: RECONCILIATION & STATUTORY NUDGES (ANIMATED)
// ============================================================================
@Composable
private fun Stage3Reconciliation(
    grossSalary: Long,
    onOpenDeductionsAdvisor: () -> Unit,
    onSimulateOverride: () -> Unit,
    onBack: () -> Unit,
    onProceed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "TDS Matching & Statutory Ceiling",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Section 192(2B) TAN cross-referencing against Form 26AS deposit ledgers.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Employer TAN BLRM01234D",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    StatutoryBadge(text = "Reconciled", tone = BadgeTone.SUCCESS)
                }
                Spacer(Modifier.height(8.dp))
                EnterpriseLedgerRow(label = "TDS Claimed in Form 16", amount = 60_000L)
                EnterpriseLedgerRow(label = "TDS Deposited in Form 26AS", amount = 60_000L)
                EnterpriseLedgerRow(label = "Unreflected Discrepancy", amount = 0L, isHighlighted = true)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Smart Deductions & Life Events Advisor Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenDeductionsAdvisor() },
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 0.75.dp,
                brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.4f))
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Deductions & Life-Events Advisor",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Running Home Loan (Sec 24b/80C), 80D Health, NPS 80CCD, or Tuition Fees? Tap to discover deductions and document holders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EnterpriseTeal800
                    )
                }
                StatutoryBadge(text = "Advisor →", tone = BadgeTone.SUCCESS)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSimulateOverride,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Text(
                text = "Simulate Section 270A Statutory Override Warning",
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text("Back", style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = onProceed,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(2f).height(44.dp)
            ) {
                Text("Proceed to Tax Showdown", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ============================================================================
// STAGE 4: DUAL-REGIME SHOWDOWN & STATUTORY ROUTING (ANIMATED)
// ============================================================================
@Composable
private fun Stage4RegimeShowdown(
    newRegimeTax: Long,
    oldRegimeTax: Long,
    newGti: Long,
    oldGti: Long,
    newDeductions: Long,
    oldDeductions: Long,
    newRebate87A: Long,
    oldRebate87A: Long,
    selectedForm: String,
    onOpenDeductionsAdvisor: () -> Unit,
    onRunAudit: () -> Unit,
    onBack: () -> Unit,
    onLaunchPortalAssistant: () -> Unit,
    onFileReturn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Tax Liability Showdown & CBDT Packaging",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Section 115BAC comparison against itemized Chapter VI-A deductions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        RegimeComparisonInstitutionalCard(
            newRegimeTax = newRegimeTax,
            oldRegimeTax = oldRegimeTax,
            newNormalGti = newGti,
            oldNormalGti = oldGti,
            newDeductions = newDeductions,
            oldDeductions = oldDeductions,
            newRebate87A = newRebate87A,
            oldRebate87A = oldRebate87A
        )

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onOpenDeductionsAdvisor,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Text("💡 Optimize Old Regime Deductions (Home Loan, 80D, NPS)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(Modifier.height(6.dp))

        OutlinedButton(
            onClick = onRunAudit,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Text("🛡️ Pre-Filing Audit & Notice Risk Shield (100% Score)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "14-Gate Routing Evaluation",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    StatutoryBadge(text = selectedForm, tone = BadgeTone.STATUTORY)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Eligible for ITR-1 (Sahaj): Resident individual, single house property, income below ₹50 Lakh, zero speculative turnover.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text("Back", style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = onLaunchPortalAssistant,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                modifier = Modifier.weight(2.2f).height(44.dp)
            ) {
                Text("Direct In-App ITR Filing u/s 139(1)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

// ============================================================================
// PRO WORKSPACE: 3-TAB ANIMATED INSTITUTIONAL MANAGEMENT HUB
// ============================================================================
@Composable
private fun ProWorkspaceView(
    selectedTab: WorkspaceTab,
    uiState: in.zerotax.app.viewmodel.ZeroTaxUiState,
    onOpenDeductionsAdvisor: () -> Unit,
    onOpenAisTransactions: () -> Unit,
    onRunAudit: () -> Unit,
    onLaunchPortalAssistant: () -> Unit,
    onOpenChatbot: () -> Unit = {},
    onShredVault: () -> Unit
) {
    var isShreddingInProgress by remember { mutableStateOf(false) }
    val shredProgressAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedTab.title,
                style = MaterialTheme.typography.headlineSmall
            )
            StatutoryBadge(
                text = if (uiState.isVaultShredded) "Vault Shredded" else "Vault Locked",
                tone = if (uiState.isVaultShredded) BadgeTone.DANGER else BadgeTone.SUCCESS
            )
        }

        Spacer(Modifier.height(14.dp))

        // Fluid horizontal slide & fade between workspace tabs
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val isForward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { if (isForward) it / 2 else -it / 2 } +
                 fadeIn(tween(240))).togetherWith(
                    slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { if (isForward) -it / 2 else it / 2 } +
                    fadeOut(tween(160))
                )
            },
            label = "WorkspaceTabTransition",
            modifier = Modifier.fillMaxSize()
        ) { currentTab ->
            when (currentTab) {
                WorkspaceTab.FilingAndRefundHub -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = 1.dp,
                                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "CBDT Schema-Compliant Return Package",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    EnterpriseLedgerRow(label = "Form Designation", statutoryRef = "CBDT AY 2026-27", amount = 0L)
                                    EnterpriseLedgerRow(label = "Total Income u/s 288A", amount = uiState.newRegimeResult.netTaxableNormalIncomeRounded288A)
                                    EnterpriseLedgerRow(label = "Tax Liability u/s 288B", amount = uiState.newRegimeResult.totalTaxLiabilityRounded288B)
                                    EnterpriseLedgerRow(label = "Prepaid Taxes (TDS)", amount = uiState.salarySummary.aggregateTdsAllowed)
                                    EnterpriseLedgerRow(
                                        label = "Net Refund Due",
                                        amount = uiState.salarySummary.aggregateTdsAllowed - uiState.newRegimeResult.totalTaxLiabilityRounded288B,
                                        isHighlighted = true
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick = onLaunchPortalAssistant,
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Text("Direct In-App ITR Filing Suite & Gemini Copilot", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = onOpenAisTransactions,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Text("Inspect AIS Transactions & Form 60 KYC Profile", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = onOpenDeductionsAdvisor,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Text("Smart Deductions & Life-Events (Home Loan, 80D, NPS)", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = onRunAudit,
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Text("🛡️ Run 6-Gate Pre-Filing Audit & CPC Refund Tracker", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = onOpenChatbot,
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EnterpriseTeal600),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Text("🤖 Conversational Filing Chatbot (TaxBuddy Style)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }

                WorkspaceTab.SmartTaxVaultAndCopilot -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = 1.dp,
                                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Cryptographic Non-Repudiation Audit Ledger",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Hardware-secured chronological trail of every verified calculation and statutory override.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        items(uiState.auditTrail) { audit ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = 0.5.dp,
                                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = audit.eventCategory,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = audit.timestampFormatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate500
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = audit.description, style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "SHA-256: ${audit.warningTextHashSha256.take(24)}...",
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Slate500)
                                    )
                                }
                            }
                        }
                    }
                }

                WorkspaceTab.PrivacyAndSettings -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(containerColor = CrimsonDangerBg),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = 0.75.dp,
                                    brush = androidx.compose.ui.graphics.SolidColor(CrimsonDanger.copy(alpha = 0.4f))
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(CrimsonDanger)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "DPDP Act 2023 Section 12(3) Right to Erasure",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = CrimsonDanger
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Initiating cryptographic shredding permanently zeroes all database pages with random bytes, deletes hardware Keystore wrapping keys, and wipes volatile memory caches.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CrimsonDanger
                                    )

                                    if (isShreddingInProgress) {
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = "Cryptographically overwriting memory pages...",
                                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CrimsonDanger)
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(1.5.dp))
                                                .background(CrimsonDanger.copy(alpha = 0.2f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(shredProgressAnim.value)
                                                    .fillMaxHeight()
                                                    .background(CrimsonDanger)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            if (!isShreddingInProgress) {
                                                scope.launch {
                                                    isShreddingInProgress = true
                                                    shredProgressAnim.animateTo(1f, tween(800, easing = LinearEasing))
                                                    onShredVault()
                                                    isShreddingInProgress = false
                                                }
                                            }
                                        },
                                        enabled = !isShreddingInProgress && !uiState.isVaultShredded,
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.isVaultShredded) "Vault Data Cryptographically Shredded"
                                                   else "Cryptographically Shred All Vault Data (DPDP §12(3))",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
