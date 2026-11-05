package `in`.zerotax.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.audit.PreFilingAuditView
import `in`.zerotax.app.ui.business.SmallBusinessAuditSuiteScreen
import `in`.zerotax.app.ui.ca.CaAuditorPortalScreen
import `in`.zerotax.app.ui.ca.ShareWithCaClientModal
import `in`.zerotax.app.ui.ca.ThinkLikeACaAdvisorView
import `in`.zerotax.app.ui.chat.ConversationalFilingChatScreen
import `in`.zerotax.app.ui.corporate.CorporateAndTrustSuiteScreen
import `in`.zerotax.app.ui.deductions.SmartDeductionsAdvisorScreen
import `in`.zerotax.app.ui.insights.TaxVisualizerAndAnalyticsScreen
import `in`.zerotax.app.ui.legal.ComprehensiveLegalAcknowledgementDialog
import `in`.zerotax.app.ui.notice.TaxNoticeDefenseAssistantScreen
import `in`.zerotax.app.ui.nri.DualRegionNriComplianceScreen
import `in`.zerotax.app.ui.portal.ItdLivePortalSyncModal
import `in`.zerotax.app.ui.portal.PortalAssistantScreen
import `in`.zerotax.app.ui.portal.SmartTransactionSuggestionsView
import `in`.zerotax.app.ui.enterprise.EnterpriseAuditFastStartView
import `in`.zerotax.app.ui.sahaj.SahajFilingExperienceView
import `in`.zerotax.app.ui.security.BiometricShieldGate
import `in`.zerotax.app.ui.security.ZeroKnowledgeTrustShieldModal
import `in`.zerotax.app.ui.security.ZeroTaxBiometricManager
import `in`.zerotax.app.ui.sugam.SugamPresumptiveFastStartView
import `in`.zerotax.app.ui.timeline.StatutoryTimelineBanner
import `in`.zerotax.app.ui.toolkit.SmartTaxToolkitScreen
import `in`.zerotax.feature.portalbridge.CloudBackupSyncManager
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.app.viewmodel.ZeroTaxViewModel
import `in`.zerotax.core.model.CitizenCategory
import `in`.zerotax.core.model.FilingTierMode
import `in`.zerotax.core.model.Rupee
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OnboardingTap(val title: String) {
    Tap1IdentityConsent("1. Details"),
    Tap2LiveDiscovery("2. Income"),
    Tap3NudgeCards("3. Deductions"),
    Tap4RegimeShowdown("4. Compare")
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
    var showMasterLegalDialog by remember { mutableStateOf(false) }
    var showTaxToolkit by remember { mutableStateOf(false) }
    var showAnalyticsScreen by remember { mutableStateOf(false) }
    var showNoticeDefense by remember { mutableStateOf(false) }
    var showBusinessAudit by remember { mutableStateOf(false) }
    var showNriSuite by remember { mutableStateOf(false) }
    var showCorporateSuite by remember { mutableStateOf(false) }
    var showPasswordCheatsheetModal by remember { mutableStateOf(false) }
    var showTaxPaymentHub by remember { mutableStateOf(false) }
    var showPortalSyncModal by remember { mutableStateOf(false) }
    var showCaPortal by remember { mutableStateOf(false) }
    var showThinkLikeCa by remember { mutableStateOf(false) }
    var showShareWithCaModal by remember { mutableStateOf(false) }
    var showTrustShieldModal by remember { mutableStateOf(false) }

    if (showCaPortal) {
        CaAuditorPortalScreen(
            onExitCaPortal = { showCaPortal = false }
        )
        return
    }

    if (showThinkLikeCa) {
        ThinkLikeACaAdvisorView(
            pan = uiState.pan,
            grossSalary = uiState.grossSalaryRupees,
            tdsPrepaid = uiState.salarySummary.aggregateTdsAllowed,
            onBack = { showThinkLikeCa = false }
        )
        return
    }

    if (showCorporateSuite) {
        CorporateAndTrustSuiteScreen(
            pan = uiState.pan,
            onBack = { showCorporateSuite = false }
        )
        return
    }

    if (showNriSuite) {
        DualRegionNriComplianceScreen(
            pan = uiState.pan,
            onBack = { showNriSuite = false }
        )
        return
    }

    if (showBusinessAudit) {
        SmallBusinessAuditSuiteScreen(
            pan = uiState.pan,
            onBack = { showBusinessAudit = false }
        )
        return
    }

    if (showNoticeDefense) {
        TaxNoticeDefenseAssistantScreen(
            pan = uiState.pan,
            onBack = { showNoticeDefense = false }
        )
        return
    }

    if (showAnalyticsScreen) {
        TaxVisualizerAndAnalyticsScreen(
            pan = uiState.pan,
            grossSalary = uiState.grossSalaryRupees,
            taxLiability = uiState.newRegimeResult.totalTaxLiabilityRounded288B,
            tdsClaimed = uiState.salarySummary.aggregateTdsAllowed,
            onBack = { showAnalyticsScreen = false }
        )
        return
    }

    if (showTaxToolkit) {
        SmartTaxToolkitScreen(
            pan = uiState.pan,
            grossSalary = uiState.grossSalaryRupees,
            taxLiability = uiState.newRegimeResult.totalTaxLiabilityRounded288B,
            tdsClaimed = uiState.salarySummary.aggregateTdsAllowed,
            onBack = { showTaxToolkit = false }
        )
        return
    }

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
            bankAccount = `in`.zerotax.core.model.BankAccountDetails(
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
            if (uiState.currentTap != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.onNavigateToTap(null) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit to Home",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Exit Filing",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Text(
                            text = uiState.currentTap!!.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EnterpriseTeal700
                            )
                        )

                        StatutoryBadge(text = "AY ${uiState.assessmentYear}", tone = BadgeTone.STATUTORY)
                    }
                }
            } else {
                EnterpriseTopHeader(
                    pan = uiState.pan,
                    assessmentYear = uiState.assessmentYear,
                    isVaultEncrypted = !uiState.isVaultShredded,
                    userProfile = (uiState.authSessionState as? `in`.zerotax.core.model.AuthSessionState.Authenticated)?.user,
                    onSignOut = { viewModel.signOut() },
                    onOpenChatbot = { showChatbotFiling = true },
                    onOpenToolkit = { showTaxToolkit = true },
                    onOpenAnalytics = { showAnalyticsScreen = true },
                    onOpenNotices = { showNoticeDefense = true },
                    onOpenBusinessAudit = { showBusinessAudit = true },
                    onOpenNriSuite = { showNriSuite = true },
                    onOpenCorporateSuite = { showCorporateSuite = true },
                    onOpenTrustShield = { showTrustShieldModal = true },
                    onOpenThinkLikeCa = { showThinkLikeCa = true },
                    onOpenCaPortal = { showCaPortal = true },
                    onLockVault = { ZeroTaxBiometricManager.lockVaultNow() }
                )
            }
        },
        bottomBar = {
            if (uiState.currentTap == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    val icons = mapOf(
                        WorkspaceTab.FilingAndRefundHub to Icons.Default.AccountBalance,
                        WorkspaceTab.SmartTaxVaultAndCopilot to Icons.Default.Security,
                        WorkspaceTab.PrivacyAndSettings to Icons.Default.Lock
                    )
                    WorkspaceTab.entries.forEach { tab ->
                        val isSelected = uiState.selectedWorkspaceTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.onSelectWorkspaceTab(tab) },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = icons[tab] ?: Icons.Default.Description,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = EnterpriseTeal100,
                                selectedIconColor = EnterpriseTeal900,
                                selectedTextColor = EnterpriseTeal700,
                                unselectedIconColor = Slate500,
                                unselectedTextColor = Slate600
                            )
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
        BiometricShieldGate(
            isUnlocked = ZeroTaxBiometricManager.isVaultBiometricallyUnlocked,
            onUnlock = { ZeroTaxBiometricManager.unlockVault() }
        ) {
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
                        citizenCategory = uiState.citizenCategory,
                        onCitizenCategoryChanged = { viewModel.onCitizenCategoryChanged(it) },
                        onOpenPasswordCheatsheet = { showPasswordCheatsheetModal = true },
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
                        onOpenTaxPaymentHub = { showTaxPaymentHub = true },
                        onBack = { viewModel.onNavigateToTap(OnboardingTap.Tap3NudgeCards) },
                        onLaunchPortalAssistant = { showPortalAssistant = true },
                        onFileReturn = { viewModel.onNavigateToTap(null) }
                    )

                    null -> ProWorkspaceView(
                        selectedTab = uiState.selectedWorkspaceTab,
                        uiState = uiState,
                        onTierSelected = { viewModel.onFilingTierChanged(it) },
                        onOpenTaxPaymentHub = { showTaxPaymentHub = true },
                        onOpenDeductionsAdvisor = { showDeductionsAdvisor = true },
                        onOpenAisTransactions = { showAisTransactions = true },
                        onRunAudit = { showPreFilingAudit = true },
                        onLaunchPortalAssistant = { showPortalAssistant = true },
                        onOpenChatbot = { showChatbotFiling = true },
                        onOpenLegalDialog = { showMasterLegalDialog = true },
                        onOpenToolkit = { showTaxToolkit = true },
                        onOpenAnalytics = { showAnalyticsScreen = true },
                        onOpenNotices = { showNoticeDefense = true },
                        onOpenBusinessAudit = { showBusinessAudit = true },
                        onOpenNriSuite = { showNriSuite = true },
                        onOpenCorporateSuite = { showCorporateSuite = true },
                        onOpenPortalSync = { showPortalSyncModal = true },
                        onOpenShareWithCa = { showShareWithCaModal = true },
                        onOpenThinkLikeCa = { showThinkLikeCa = true },
                        onOpenCaPortal = { showCaPortal = true },
                        onLoadAuditTrail = { viewModel.loadAuditTrailLazy() },
                        onShredVault = { viewModel.onShredVaultData() }
                    )
                }
            }

            if (showPortalSyncModal) {
                ItdLivePortalSyncModal(
                    pan = uiState.pan,
                    onDismiss = { showPortalSyncModal = false },
                    onSyncCompleted = { count, tds, interest ->
                        showPortalSyncModal = false
                    }
                )
            }

            if (showMasterLegalDialog) {
                ComprehensiveLegalAcknowledgementDialog(
                    pan = uiState.pan,
                    onDismiss = { showMasterLegalDialog = false },
                    onAcknowledgeAndAccept = { _, _ ->
                        showMasterLegalDialog = false
                    }
                )
            }

            if (showPasswordCheatsheetModal) {
                InstitutionPdfPasswordCheatsheetModal(
                    onDismiss = { showPasswordCheatsheetModal = false }
                )
            }

            if (showTaxPaymentHub) {
                val netPayable = max(0L, uiState.newRegimeResult.totalTaxLiabilityRounded288B - uiState.salarySummary.aggregateTdsAllowed - uiState.totalTaxPaidViaChallan)
                TaxPaymentAndChallanHubModal(
                    pan = uiState.pan,
                    taxPayableRupees = netPayable,
                    assessmentYear = uiState.assessmentYear,
                    onDismiss = { showTaxPaymentHub = false },
                    onRecordChallan = { entry ->
                        viewModel.onRecordChallanPayment(entry)
                        showTaxPaymentHub = false
                    }
                )
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

            if (showShareWithCaModal) {
                ShareWithCaClientModal(
                    pan = uiState.pan,
                    onDismiss = { showShareWithCaModal = false }
                )
            }

            if (showTrustShieldModal) {
                ZeroKnowledgeTrustShieldModal(
                    onDismiss = { showTrustShieldModal = false }
                )
            }
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
    citizenCategory: CitizenCategory,
    onCitizenCategoryChanged: (CitizenCategory) -> Unit,
    onOpenPasswordCheatsheet: () -> Unit,
    onProceed: () -> Unit,
    onOpenChatbot: () -> Unit = {}
) {
    val isPanValid = pan.length == 10 && (pan[3] == 'P' || pan[3] == 'H')
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }
        ) {
            Column {
                Text(
                    text = "Step 1 of 4: Basic Details",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Enter your PAN and age to determine your applicable tax exemption slabs.",
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

        Spacer(Modifier.height(12.dp))

        // Age Category & Statutory Basic Exemption Selector
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400, delayMillis = 150)) + slideInVertically(tween(400, delayMillis = 150)) { it / 3 }
        ) {
            CitizenCategoryAgeSelector(
                selectedCategory = citizenCategory,
                onCategoryChanged = onCitizenCategoryChanged
            )
        }

        Spacer(Modifier.height(12.dp))

        // Institution PDF Password Cheatsheet Quick Helper
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPasswordCheatsheet() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = CardDefaults.outlinedCardBorder().copy(width = 0.75.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PDF Password Helper",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "Form 16, AIS, e-Aadhaar & Bank PDF password patterns",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatutoryBadge(text = "HINTS", tone = BadgeTone.NEUTRAL)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Privacy Guarantee Card (Clean & reassuring)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = EnterpriseTeal600,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "100% On-Device Encryption: Your PAN and financials are never stored on external servers.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Primary Action CTA
        Button(
            onClick = onProceed,
            enabled = isPanValid,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EnterpriseTeal600,
                contentColor = PureWhite
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = if (isPanValid) "Continue to Step 2: Income →" else "Enter Valid PAN to Continue",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Secondary Chatbot entry
        OutlinedButton(
            onClick = onOpenChatbot,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EnterpriseTeal600),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Prefer Guided Chat? Open AI Tax Assistant",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
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
    var currentPermutationText by remember { mutableStateOf("Scanning Form 16 PDF...") }

    val unlockProgressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val permutations = listOf(
            "Reading Form 16 Part A & B...",
            "Matching salary income...",
            "Cross-verifying TDS deposited...",
            "Form 16 Verified & Ready (< 42ms)"
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Step 2 of 4: Income & Form 16",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Your salary, allowances, and tax deducted are automatically populated below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        // Document Decryption Card with animated scanning status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRACES Form 16 Part B",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    AnimatedContent(
                        targetState = isUnlocked,
                        transitionSpec = {
                            fadeIn(tween(250)) togetherWith fadeOut(tween(150))
                        },
                        label = "UnlockBadgeAnim"
                    ) { unlocked ->
                        if (unlocked) {
                            StatutoryBadge(text = "Verified", tone = BadgeTone.SUCCESS)
                        } else {
                            StatutoryBadge(text = "Scanning...", tone = BadgeTone.WARNING)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Progress Hairline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(Modifier.height(10.dp))
                EnterpriseLedgerRow(label = "Employer Deductor TAN", statutoryRef = "BLRM01234D", amount = 0L)
                EnterpriseLedgerRow(label = "Gross Salary (Sec 17(1))", amount = grossSalary)
                EnterpriseLedgerRow(label = "Standard Deduction (Sec 16(ia))", amount = 75_000L, isDeduction = true)
                EnterpriseLedgerRow(label = "Net Taxable Salary", amount = grossSalary - 75_000L, isHighlighted = true)
            }
        }

        Spacer(Modifier.height(12.dp))

        // On-Device Integrity Verification Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 0.75.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Automatic Salary Cross-Check",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    StatutoryBadge(text = "Matched", tone = BadgeTone.SUCCESS)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Gross salary and statutory standard deductions cross-verified with zero arithmetic discrepancy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // AIS / TIS Transactions Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAisTransactions() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(IndigoStatutory.copy(alpha = 0.3f))
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AIS / 26AS Tax Credits & Interest",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) Color(0xFF93C5FD) else IndigoStatutory)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Fetch bank interest, dividend TDS, and confirm prepaid taxes match ITD portal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatutoryBadge(text = "View Records →", tone = BadgeTone.STATUTORY)
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("← Back", style = MaterialTheme.typography.titleSmall)
            }
            Button(
                onClick = onProceed,
                enabled = isUnlocked,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnterpriseTeal600,
                    contentColor = PureWhite
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(2f).height(48.dp)
            ) {
                Text("Continue to Deductions →", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(Modifier.height(16.dp))
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Step 3 of 4: Taxes Paid & Deductions",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Verify the TDS tax your employer deposited and review eligible deductions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        // TDS Reconciliation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Employer TDS (TAN: BLRM01234D)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    StatutoryBadge(text = "Reconciled", tone = BadgeTone.SUCCESS)
                }
                Spacer(Modifier.height(10.dp))
                EnterpriseLedgerRow(label = "TDS Claimed in Form 16", amount = 60_000L)
                EnterpriseLedgerRow(label = "TDS Deposited in Form 26AS", amount = 60_000L)
                EnterpriseLedgerRow(label = "Unreflected Discrepancy", amount = 0L, isHighlighted = true)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Smart Deductions & Life Events Advisor Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenDeductionsAdvisor() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Deductions & Life-Events Advisor",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Running Home Loan (Sec 24b/80C), 80D Health, NPS, or Tuition Fees? Tap to discover tax deductions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
                StatutoryBadge(text = "Advisor →", tone = BadgeTone.SUCCESS)
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("← Back", style = MaterialTheme.typography.titleSmall)
            }
            Button(
                onClick = onProceed,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnterpriseTeal600,
                    contentColor = PureWhite
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(2f).height(48.dp)
            ) {
                Text("Compare Tax Regimes →", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(Modifier.height(16.dp))
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
    onOpenTaxPaymentHub: () -> Unit,
    onBack: () -> Unit,
    onLaunchPortalAssistant: () -> Unit,
    onFileReturn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Step 4 of 4: Old vs New Regime Comparison",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "We calculated both tax regimes side by side to maximize your tax savings.",
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

        Spacer(Modifier.height(12.dp))

        // Form recommendation card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 0.75.dp,
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
                        text = "Filing Form Recommendation",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    StatutoryBadge(text = selectedForm, tone = BadgeTone.STATUTORY)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Eligible for ITR-1 (Sahaj): Resident individual, salary income, single house property, below ₹50L.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Pre-filing notice risk check
        OutlinedButton(
            onClick = onRunAudit,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Pre-Filing Audit & Notice Risk Shield", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }

        if (newRegimeTax > 0L) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onOpenTaxPaymentHub,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Pay Tax via Instant UPI / Challan 280 (Zero Fee)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = PureWhite))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("← Back", style = MaterialTheme.typography.titleSmall)
            }
            Button(
                onClick = onLaunchPortalAssistant,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnterpriseTeal600,
                    contentColor = PureWhite
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(2.2f).height(50.dp)
            ) {
                Text("E-File Tax Return Now →", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ============================================================================
// PRO WORKSPACE: 3-TAB ANIMATED INSTITUTIONAL MANAGEMENT HUB
// ============================================================================
@Composable
private fun ProWorkspaceView(
    selectedTab: WorkspaceTab,
    uiState: `in`.zerotax.app.viewmodel.ZeroTaxUiState,
    onTierSelected: (FilingTierMode) -> Unit,
    onOpenTaxPaymentHub: () -> Unit,
    onOpenDeductionsAdvisor: () -> Unit,
    onOpenAisTransactions: () -> Unit,
    onRunAudit: () -> Unit,
    onLaunchPortalAssistant: () -> Unit,
    onOpenChatbot: () -> Unit = {},
    onOpenLegalDialog: () -> Unit = {},
    onOpenToolkit: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenNotices: () -> Unit = {},
    onOpenBusinessAudit: () -> Unit = {},
    onOpenNriSuite: () -> Unit = {},
    onOpenCorporateSuite: () -> Unit = {},
    onOpenPortalSync: () -> Unit = {},
    onOpenShareWithCa: () -> Unit = {},
    onOpenThinkLikeCa: () -> Unit = {},
    onOpenCaPortal: () -> Unit = {},
    onLoadAuditTrail: () -> Unit = {},
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
                            FilingTierSelectorCard(
                                currentTier = uiState.filingTierMode,
                                recommendedTier = uiState.recommendedTierMode,
                                onTierSelected = onTierSelected
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        when (uiState.filingTierMode) {
                            FilingTierMode.SAHAJ_SIMPLE -> {
                                item {
                                    SahajFilingExperienceView(
                                        pan = uiState.pan,
                                        grossSalary = uiState.grossSalaryRupees,
                                        bankInterest = 14_200L,
                                        tdsClaimed = uiState.salarySummary.aggregateTdsAllowed,
                                        newRegimeTax = uiState.newRegimeResult.totalTaxLiabilityRounded288B,
                                        oldRegimeTax = uiState.oldRegimeResult.totalTaxLiabilityRounded288B,
                                        onLaunchPortalAssistant = onLaunchPortalAssistant,
                                        onOpenDeductionsAdvisor = onOpenDeductionsAdvisor,
                                        onOpenTaxPaymentHub = onOpenTaxPaymentHub,
                                        onOpenAisTransactions = onOpenAisTransactions,
                                        onOpenPortalSync = onOpenPortalSync,
                                        onOpenThinkLikeCa = onOpenThinkLikeCa
                                    )
                                }
                            }
                            FilingTierMode.INVESTOR_PRO -> {
                                item {
                                    SugamPresumptiveFastStartView(
                                        pan = uiState.pan,
                                        onLaunchPortalAssistant = onLaunchPortalAssistant,
                                        onOpenTaxPaymentHub = onOpenTaxPaymentHub,
                                        onOpenAisTransactions = onOpenAisTransactions,
                                        onOpenPortalSync = onOpenPortalSync
                                    )
                                }
                            }
                            FilingTierMode.ENTERPRISE_AUDIT -> {
                                item {
                                    EnterpriseAuditFastStartView(
                                        pan = uiState.pan,
                                        totalIncome = uiState.newRegimeResult.netTaxableNormalIncomeRounded288A,
                                        taxLiability = uiState.newRegimeResult.totalTaxLiabilityRounded288B,
                                        tdsClaimed = uiState.salarySummary.aggregateTdsAllowed,
                                        onOpenCorporateSuite = onOpenCorporateSuite,
                                        onOpenBusinessAudit = onOpenBusinessAudit,
                                        onOpenNriSuite = onOpenNriSuite,
                                        onOpenToolkit = onOpenToolkit,
                                        onOpenAnalytics = onOpenAnalytics,
                                        onOpenNotices = onOpenNotices,
                                        onLaunchPortalAssistant = onLaunchPortalAssistant,
                                        onOpenTaxPaymentHub = onOpenTaxPaymentHub,
                                        onRunAudit = onRunAudit,
                                        onOpenPortalSync = onOpenPortalSync,
                                        onOpenShareWithCa = onOpenShareWithCa,
                                        onOpenThinkLikeCa = onOpenThinkLikeCa,
                                        onOpenCaPortal = onOpenCaPortal
                                    )
                                }
                            }
                        }
                    }
                }

                WorkspaceTab.SmartTaxVaultAndCopilot -> {
                    LaunchedEffect(Unit) {
                        onLoadAuditTrail()
                    }
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

                        if (uiState.lazyAuditTrail is `in`.zerotax.core.model.LazyLoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }

                        items(
                            items = uiState.auditTrail,
                            key = { it.auditId }
                        ) { audit ->
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = audit.description, style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "SHA-256: ${audit.warningTextHashSha256.take(24)}...",
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }

                WorkspaceTab.PrivacyAndSettings -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // Statutory Legal Protection & Cloud Shield Summary Card
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Statutory Legal Protection & Cloud Shield",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        StatutoryBadge(text = "DPDP §6 & §140", tone = BadgeTone.STATUTORY)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Comprehensive legal coverage active: Section 140 taxpayer declaration, Section 277 penal disclaimers, technology intermediary protections, and zero-knowledge cloud crash resilience for session recovery.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = onOpenLegalDialog,
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EnterpriseTeal600),
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Gavel,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Review Master Legal Terms & Declarations", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

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
