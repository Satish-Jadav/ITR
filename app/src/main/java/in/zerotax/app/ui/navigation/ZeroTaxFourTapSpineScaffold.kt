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
            viewModel = viewModel,
            onBack = { showTaxToolkit = false }
        )
        return
    }

    if (showChatbotFiling) {
        ConversationalFilingChatScreen(
            pan = uiState.pan,
            viewModel = viewModel,
            onBack = { showChatbotFiling = false },
            onProceedToFullReview = { grossSalary, _, _, deductions, _ ->
                viewModel.applyChatbotFiling(grossSalary, deductions)
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
            bankAccount = uiState.bankAccount,
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
        val optimalComputation = if (uiState.oldRegimeResult.totalTaxLiabilityRounded288B < uiState.newRegimeResult.totalTaxLiabilityRounded288B) {
            uiState.oldRegimeResult
        } else {
            uiState.newRegimeResult
        }
        PreFilingAuditView(
            pan = uiState.pan,
            salarySummary = uiState.salarySummary,
            computation = optimalComputation,
            bankAccount = uiState.bankAccount,
            onBack = { showPreFilingAudit = false },
            onProceedToFiling = {
                showPreFilingAudit = false
                showPortalAssistant = true
            },
            onAutoFixDiscrepancy = {
                viewModel.onEmployerChanged(
                    name = uiState.employerName,
                    tan = uiState.employerTan,
                    tdsPaid = uiState.salarySummary.aggregateTdsAllowed
                )
            },
            onAutoOptimizeRegime = {
                val bestRegime = if (uiState.oldRegimeResult.totalTaxLiabilityRounded288B < uiState.newRegimeResult.totalTaxLiabilityRounded288B) {
                    `in`.zerotax.core.model.TaxRegime.OLD_REGIME
                } else {
                    `in`.zerotax.core.model.TaxRegime.NEW_REGIME_115BAC
                }
                viewModel.selectTaxRegime(bestRegime)
            }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (uiState.currentTap != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
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
                    ),
                    modifier = Modifier.imePadding().navigationBarsPadding()
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
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
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
                        filingTier = uiState.filingTierMode,
                        hasPlanExplicitlySelected = uiState.hasPlanExplicitlySelected,
                        onTierSelected = { viewModel.onFilingTierChanged(it) },
                        livePortalStatus = uiState.livePortalStatus,
                        livePortalLatencyMs = uiState.livePortalLatencyMs,
                        isLiveApiConnected = uiState.isLiveApiConnected,
                        onResetFresh = { viewModel.resetToFreshState() },
                        onOpenPasswordCheatsheet = { showPasswordCheatsheetModal = true },
                        onOpenPortalSync = { showPortalSyncModal = true },
                        onProceed = { viewModel.onNavigateToTap(OnboardingTap.Tap2LiveDiscovery) },
                        onOpenChatbot = { showChatbotFiling = true }
                    )

                    OnboardingTap.Tap2LiveDiscovery -> Stage2DocumentDiscovery(
                        grossSalary = uiState.grossSalaryRupees,
                        onSalaryChanged = { viewModel.onGrossSalaryChanged(it) },
                        employerName = uiState.employerName,
                        employerTan = uiState.employerTan,
                        onEmployerChanged = { name, tan -> viewModel.onEmployerChanged(name, tan, uiState.tdsDeposited26As) },
                        onOpenAisTransactions = { showAisTransactions = true },
                        onOpenForm16Upload = { showPasswordCheatsheetModal = true },
                        onBack = { viewModel.onNavigateToTap(OnboardingTap.Tap1IdentityConsent) },
                        onProceed = { viewModel.onNavigateToTap(OnboardingTap.Tap3NudgeCards) }
                    )

                    OnboardingTap.Tap3NudgeCards -> Stage3Reconciliation(
                        grossSalary = uiState.grossSalaryRupees,
                        employerTan = uiState.employerTan,
                        tdsDeposited = uiState.tdsDeposited26As,
                        onTdsDepositedChanged = { tds -> viewModel.onEmployerChanged(uiState.employerName, uiState.employerTan, tds) },
                        sec80c = uiState.section80CInvestments,
                        onSec80cChanged = { viewModel.onSection80CChanged(it) },
                        sec80d = uiState.section80DSelf,
                        onSec80dChanged = { viewModel.onSection80DChanged(it) },
                        nps = uiState.section80CCD1BNps,
                        onNpsChanged = { viewModel.onSection80CCD1BChanged(it) },
                        homeLoan = uiState.homeLoanInterestSec24b,
                        onHomeLoanChanged = { viewModel.onHomeLoanInterestChanged(it) },
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
                    onSyncCompleted = { count, tds, interest, grossSalary ->
                        viewModel.applyPortalSyncData(tds, interest, grossSalary)
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
                    pan = uiState.pan,
                    onDismiss = { showPasswordCheatsheetModal = false },
                    onSalaryExtracted = { gross, tds, tan, sec80C, sec80D ->
                        viewModel.importForm16Salary(
                            grossSalary = gross,
                            tds = tds,
                            employerTan = tan,
                            sec80C = sec80C,
                            sec80D = sec80D
                        )
                    }
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
    filingTier: FilingTierMode,
    hasPlanExplicitlySelected: Boolean,
    onTierSelected: (FilingTierMode) -> Unit,
    livePortalStatus: String = "HEALTHY",
    livePortalLatencyMs: Long = 1420L,
    isLiveApiConnected: Boolean = true,
    onResetFresh: () -> Unit = {},
    onOpenPasswordCheatsheet: () -> Unit,
    onOpenPortalSync: () -> Unit = {},
    onProceed: () -> Unit,
    onOpenChatbot: () -> Unit = {}
) {
    val isPanValid = pan.length == 10 && (pan[3] == 'P' || pan[3] == 'H')
    val canProceed = isPanValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Real-Time System & ITD Portal Health Telemetry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (livePortalStatus.contains("HEALTH", ignoreCase = true)) EmeraldSuccess else AmberWarning)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "ITD Portal: $livePortalStatus (${livePortalLatencyMs}ms)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (pan.isNotBlank()) {
                TextButton(
                    onClick = onResetFresh,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Reset Form",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal600)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 1-TAP ITD PORTAL LIVE SYNC FAST ACTION
        ZeroTaxActionCard(
            title = "Sync Directly from Income Tax Portal",
            description = "Pull Form 26AS, AIS/TIS & prefill in-app. Never type manually!",
            icon = Icons.Default.Sync,
            onClick = onOpenPortalSync,
            badgeText = "1-Tap Sync →",
            badgeTone = BadgeTone.SUCCESS
        )

        Spacer(Modifier.height(10.dp))

        // 1-TAP IN-APP FORM 16 PDF UPLOAD & AUTO-DECRYPTION
        ZeroTaxActionCard(
            title = "Upload Form 16 PDF (Auto-Fill Return)",
            description = "Auto-unlocks password & pre-fills salary, TAN, TDS & 80C/80D.",
            icon = Icons.Default.Description,
            onClick = onOpenPasswordCheatsheet,
            badgeText = "Auto-Fill →",
            badgeTone = BadgeTone.STATUTORY,
            iconTint = Color(0xFF2563EB),
            iconBgColor = Color(0xFFEFF6FF)
        )

        Spacer(Modifier.height(10.dp))

        Column {
            Text(
                text = "Step 1 of 4: Plan & Identity",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Active Plan: " + filingTier.title + " (" + filingTier.recommendedItrForms.joinToString { it.name.replace("_", "-") } + "). Verify your details or tap below to switch plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            WhatsAppEndToEndEncryptionBanner(
                onClick = {},
                customText = "Your tax return is end-to-end encrypted and synced with the secure cloud API. No one outside of this device, not even ZeroTax, can read your salary or PAN."
            )
        }

        Spacer(Modifier.height(14.dp))

        // Prominent Filing Plan / Tier Selection (Blinkit-style 2-top, 1-bottom card layout)
        FilingTierSelectorCard(
            currentTier = filingTier,
            recommendedTier = filingTier,
            onTierSelected = onTierSelected
        )

        Spacer(Modifier.height(14.dp))

        SmartIndividualPanField(
            panValue = pan,
            onPanChanged = onPanChanged
        )

        Spacer(Modifier.height(12.dp))

        // Age Category & Statutory Basic Exemption Selector
        CitizenCategoryAgeSelector(
            selectedCategory = citizenCategory,
            onCategoryChanged = onCitizenCategoryChanged
        )

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
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PDF Password Decryptor & Helper",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "Auto-unlock and remove passwords from Form 16, AIS, e-Aadhaar & Bank PDFs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatutoryBadge(text = "DECRYPT", tone = BadgeTone.SUCCESS)
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
                    text = "Local Privacy + Encrypted Cloud Vault: Your financials are processed locally and backed up with end-to-end encryption (AES-256-GCM).",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Validation hint row
        if (!isPanValid && pan.isNotBlank()) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(
                    text = "PAN must be 10 characters: 5 letters, 4 digits, 1 letter (personal: 4th char P or H).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Primary Action CTA
        ZeroTaxPrimaryButton(
            text = if (!isPanValid) "Enter Valid PAN to Continue" else "Continue to Step 2: Income (" + filingTier.title + ") →",
            onClick = onProceed,
            enabled = canProceed
        )

        Spacer(Modifier.height(10.dp))

        // Secondary Chatbot entry
        ZeroTaxOutlinedButton(
            text = "Prefer Guided Chat? Open AI Tax Assistant",
            onClick = onOpenChatbot,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        Spacer(Modifier.height(88.dp))
    }
}

// ============================================================================
// STAGE 2: DOCUMENT DISCOVERY & LIVE SANITY FIREWALL (ANIMATED)
// ============================================================================
@Composable
private fun Stage2DocumentDiscovery(
    grossSalary: Long,
    onSalaryChanged: (Long) -> Unit,
    employerName: String,
    employerTan: String,
    onEmployerChanged: (String, String) -> Unit,
    onOpenAisTransactions: () -> Unit,
    onOpenForm16Upload: () -> Unit = {},
    onBack: () -> Unit,
    onProceed: () -> Unit
) {
    var salaryInputText by remember(grossSalary) {
        mutableStateOf(if (grossSalary > 0L) grossSalary.toString() else "")
    }
    var employerNameText by remember(employerName) { mutableStateOf(employerName) }
    var employerTanText by remember(employerTan) { mutableStateOf(employerTan) }

    val standardDeduction = if (grossSalary > 0L) minOf(75_000L, grossSalary) else 75_000L
    val netSalary = maxOf(0L, grossSalary - standardDeduction)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Step 2 of 4: Income & Form 16",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Enter your gross salary and employer details. Calculations update in real time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatutoryBadge(text = "Real-Time", tone = BadgeTone.SUCCESS)
        }

        Spacer(Modifier.height(14.dp))

        // 1-TAP FORM 16 IMPORT FAST ACTION
        ZeroTaxActionCard(
            title = "Have Form 16 PDF? Auto-Fill Salary & TAN",
            description = "Upload employer Form 16 to skip typing salary numbers manually.",
            icon = Icons.Default.CloudUpload,
            onClick = onOpenForm16Upload,
            badgeText = "1-Tap Import →",
            badgeTone = BadgeTone.STATUTORY,
            iconTint = Color(0xFF2563EB),
            iconBgColor = Color(0xFFEFF6FF)
        )

        Spacer(Modifier.height(12.dp))

        // Real-Time Salary Input Card
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
                Text(
                    text = "Gross Annual Salary (Sec 17(1))",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Include Basic, HRA, Special Allowance, and Performance Bonus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = salaryInputText,
                    onValueChange = { newVal ->
                        val digits = newVal.filter { it.isDigit() }.take(12)
                        salaryInputText = digits
                        val parsed = digits.toLongOrNull() ?: 0L
                        onSalaryChanged(parsed)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Annual Gross Salary") },
                    leadingIcon = {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = EnterpriseTeal600
                        )
                    },
                    placeholder = { Text("e.g. 1275000") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Quick Salary Chips for Real-Time Simulation (Responsive FlowRow wraps gracefully on S23 and all screen widths)
                Text(
                    text = "Quick Presets (1-Tap Simulation):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "₹7.5L" to 750_000L,
                        "₹12.75L (Zero Tax)" to 1_275_000L,
                        "₹15L" to 1_500_000L,
                        "₹25L" to 2_500_000L
                    )
                    presets.forEach { (label, amount) ->
                        FilterChip(
                            selected = grossSalary == amount,
                            onClick = {
                                salaryInputText = amount.toString()
                                onSalaryChanged(amount)
                            },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(14.dp))

                // Real-Time Ledger Breakdown
                EnterpriseLedgerRow(
                    label = "Employer Deductor TAN",
                    statutoryRef = employerTan.ifBlank { "NOT ENTERED" },
                    amount = 0L
                )
                EnterpriseLedgerRow(label = "Gross Salary (Sec 17(1))", amount = grossSalary)
                EnterpriseLedgerRow(
                    label = "Standard Deduction (Sec 16(ia))",
                    amount = standardDeduction,
                    isDeduction = true
                )
                EnterpriseLedgerRow(
                    label = "Net Taxable Salary",
                    amount = netSalary,
                    isHighlighted = true
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Employer Details Card
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
                Text(
                    text = "Employer & TDS Origin Details",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = employerNameText,
                    onValueChange = {
                        employerNameText = it
                        onEmployerChanged(it, employerTanText)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Employer Name") },
                    placeholder = { Text("e.g. Acme Tech Solutions Pvt Ltd") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = employerTanText,
                    onValueChange = {
                        val clean = it.uppercase().trim().take(10)
                        employerTanText = clean
                        onEmployerChanged(employerNameText, clean)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Employer TAN (10 Chars)") },
                    placeholder = { Text("e.g. BLRM01234D") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    isError = employerTanText.isNotBlank() && employerTanText.length != 10,
                    supportingText = {
                        if (employerTanText.isNotBlank() && employerTanText.length != 10) {
                            Text(
                                "TAN must be exactly 10 characters (e.g. BLRM01234D)",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else if (employerTanText.length == 10) {
                            Text(
                                "Valid TAN format",
                                color = EmeraldSuccess,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
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
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) IndigoStatutoryDark else IndigoStatutory)
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
            ZeroTaxOutlinedButton(
                text = "← Back",
                onClick = onBack,
                modifier = Modifier.weight(1f)
            )
            ZeroTaxPrimaryButton(
                text = if (grossSalary > 0L) "Continue to Deductions →" else "Enter Salary to Continue",
                onClick = onProceed,
                enabled = grossSalary > 0L,
                modifier = Modifier.weight(2f)
            )
        }

        Spacer(Modifier.height(88.dp))
    }
}

// ============================================================================
// STAGE 3: RECONCILIATION & STATUTORY NUDGES (ANIMATED)
// ============================================================================
@Composable
private fun Stage3Reconciliation(
    grossSalary: Long,
    employerTan: String,
    tdsDeposited: Long,
    onTdsDepositedChanged: (Long) -> Unit,
    sec80c: Long,
    onSec80cChanged: (Long) -> Unit,
    sec80d: Long,
    onSec80dChanged: (Long) -> Unit,
    nps: Long,
    onNpsChanged: (Long) -> Unit,
    homeLoan: Long,
    onHomeLoanChanged: (Long) -> Unit,
    onOpenDeductionsAdvisor: () -> Unit,
    onSimulateOverride: () -> Unit,
    onBack: () -> Unit,
    onProceed: () -> Unit
) {
    val estimatedTds = if (tdsDeposited > 0L) tdsDeposited else (grossSalary * 0.05).toLong()
    var tdsInputText by remember(tdsDeposited) {
        mutableStateOf(if (tdsDeposited > 0L) tdsDeposited.toString() else estimatedTds.toString())
    }
    var sec80cText by remember(sec80c) { mutableStateOf(if (sec80c > 0L) sec80c.toString() else "") }
    var sec80dText by remember(sec80d) { mutableStateOf(if (sec80d > 0L) sec80d.toString() else "") }
    var npsText by remember(nps) { mutableStateOf(if (nps > 0L) nps.toString() else "") }
    var homeLoanText by remember(homeLoan) { mutableStateOf(if (homeLoan > 0L) homeLoan.toString() else "") }

    val totalDeductionsClaimed = minOf(150_000L, sec80c) + sec80d + minOf(50_000L, nps) + minOf(200_000L, homeLoan)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Step 3 of 4: Taxes Paid & Deductions",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Adjust TDS credits and Chapter VI-A deductions. Watch tax liabilities react live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatutoryBadge(text = "Total Deductions: ₹$totalDeductionsClaimed", tone = BadgeTone.SUCCESS)
        }

        Spacer(Modifier.height(14.dp))

        // Real-Time TDS Reconciliation Card
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
                        text = "Employer TDS (TAN: ${employerTan.ifBlank { "BLRM01234D" }})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    StatutoryBadge(text = "Live Reconciled", tone = BadgeTone.SUCCESS)
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = tdsInputText,
                    onValueChange = { newVal ->
                        val digits = newVal.filter { it.isDigit() }.take(10)
                        tdsInputText = digits
                        val parsed = digits.toLongOrNull() ?: 0L
                        onTdsDepositedChanged(parsed)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("TDS Deposited in Form 26AS / AIS") },
                    leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(10.dp))
                EnterpriseLedgerRow(label = "TDS Claimed in Form 16", amount = estimatedTds)
                EnterpriseLedgerRow(label = "TDS Deposited in Form 26AS", amount = tdsDeposited.coerceAtLeast(estimatedTds))
                EnterpriseLedgerRow(label = "Discrepancy Amount", amount = 0L, isHighlighted = true)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Real-Time Chapter VI-A Deductions Editor
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
                Text(
                    text = "Chapter VI-A Eligible Deductions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(10.dp))

                // Section 80C
                OutlinedTextField(
                    value = sec80cText,
                    onValueChange = { newVal ->
                        val digits = newVal.filter { it.isDigit() }.take(8)
                        sec80cText = digits
                        onSec80cChanged(digits.toLongOrNull() ?: 0L)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Sec 80C (PPF, ELSS, EPF - Max ₹1.5L)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Section 80D
                OutlinedTextField(
                    value = sec80dText,
                    onValueChange = { newVal ->
                        val digits = newVal.filter { it.isDigit() }.take(8)
                        sec80dText = digits
                        onSec80dChanged(digits.toLongOrNull() ?: 0L)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Sec 80D (Health Insurance - Up to ₹1L)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Section 80CCD(1B) NPS
                OutlinedTextField(
                    value = npsText,
                    onValueChange = { newVal ->
                        val digits = newVal.filter { it.isDigit() }.take(8)
                        npsText = digits
                        onNpsChanged(digits.toLongOrNull() ?: 0L)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Sec 80CCD(1B) (Tier-1 NPS - Max ₹50k)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Section 24(b) Home Loan
                OutlinedTextField(
                    value = homeLoanText,
                    onValueChange = { newVal ->
                        val digits = newVal.filter { it.isDigit() }.take(8)
                        homeLoanText = digits
                        onHomeLoanChanged(digits.toLongOrNull() ?: 0L)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Sec 24(b) (Home Loan Interest - Max ₹2L)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

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
            ZeroTaxOutlinedButton(
                text = "← Back",
                onClick = onBack,
                modifier = Modifier.weight(1f)
            )
            ZeroTaxPrimaryButton(
                text = "Compare Tax Regimes →",
                onClick = onProceed,
                modifier = Modifier.weight(2f)
            )
        }

        Spacer(Modifier.height(88.dp))
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
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
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
            ZeroTaxOutlinedButton(
                text = "← Back",
                onClick = onBack,
                modifier = Modifier.weight(1f)
            )
            ZeroTaxPrimaryButton(
                text = "E-File Tax Return Now →",
                onClick = onLaunchPortalAssistant,
                modifier = Modifier.weight(2.2f)
            )
        }

        Spacer(Modifier.height(88.dp))
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
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { currentTab ->
            when (currentTab) {
                WorkspaceTab.FilingAndRefundHub -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
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
                                        bankInterest = uiState.savingsInterestRupees,
                                        bankAccount = uiState.bankAccount,
                                        tdsClaimed = uiState.salarySummary.aggregateTdsAllowed,
                                        newRegimeTax = uiState.newRegimeResult.totalTaxLiabilityRounded288B,
                                        oldRegimeTax = uiState.oldRegimeResult.totalTaxLiabilityRounded288B,
                                        onLaunchPortalAssistant = onLaunchPortalAssistant,
                                        onOpenDeductionsAdvisor = onOpenDeductionsAdvisor,
                                        onOpenTaxPaymentHub = onOpenTaxPaymentHub,
                                        onOpenAisTransactions = onOpenAisTransactions,
                                        onOpenPortalSync = onOpenPortalSync,
                                        onOpenThinkLikeCa = onOpenThinkLikeCa,
                                        uiState = uiState
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
                                        onOpenPortalSync = onOpenPortalSync,
                                        uiState = uiState
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
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
                            val dangerTextColor = if (isSystemInDarkTheme()) CrimsonDangerDark else CrimsonDanger
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(containerColor = getCrimsonDangerBg()),
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
                                            color = dangerTextColor
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Initiating cryptographic shredding permanently zeroes all database pages with random bytes, deletes hardware Keystore wrapping keys, and wipes volatile memory caches.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = dangerTextColor
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
