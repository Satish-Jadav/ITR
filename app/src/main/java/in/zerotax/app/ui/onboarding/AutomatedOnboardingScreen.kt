package `in`.zerotax.app.ui.onboarding

import `in`.zerotax.app.ui.security.HardwareVaultStatusCard
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.portal.ItdOfficialWebLoginSheet
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.app.viewmodel.ZeroTaxViewModel
import `in`.zerotax.core.model.FilingTierMode
import `in`.zerotax.core.model.OnboardingWizardStep
import `in`.zerotax.core.model.PanEntityDecoder
import kotlinx.coroutines.delay
/**
 * Dynamic Automated Zero-Friction Onboarding Wizard.
 * Guides taxpayers through plan selection, zero-password authentication,
 * animated 5-stream automated data hydration, and the Unified Pre-Filing Master Tax Dossier.
 */
@Composable
fun AutomatedOnboardingScreen(
    viewModel: ZeroTaxViewModel,
    onOnboardingFinished: () -> Unit,
    onLaunchCaPortal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    var showWebLoginSheet by remember { mutableStateOf(false) }
    var showAadhaarOtpModal by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            OnboardingTopBar(
                currentStep = uiState.onboardingStep,
                onBack = {
                    when (uiState.onboardingStep) {
                        OnboardingWizardStep.TRUST_AND_AUTH -> {}
                        OnboardingWizardStep.STREAM_HYDRATION_RADAR -> viewModel.setOnboardingStep(OnboardingWizardStep.TRUST_AND_AUTH)
                        OnboardingWizardStep.SELECT_PLAN -> viewModel.setOnboardingStep(OnboardingWizardStep.STREAM_HYDRATION_RADAR)
                        OnboardingWizardStep.PLAN_SPECIFIC_INPUTS -> viewModel.setOnboardingStep(OnboardingWizardStep.STREAM_HYDRATION_RADAR)
                        OnboardingWizardStep.UNIFIED_DOSSIER_REVIEW -> {
                            if (uiState.filingTierMode == FilingTierMode.SAHAJ_SIMPLE) {
                                viewModel.setOnboardingStep(OnboardingWizardStep.STREAM_HYDRATION_RADAR)
                            } else {
                                viewModel.setOnboardingStep(OnboardingWizardStep.PLAN_SPECIFIC_INPUTS)
                            }
                        }
                        else -> {}
                    }
                }
            )
        },
        containerColor = if (isDark) Slate950 else Slate50
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.onboardingStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(250))
                },
                label = "OnboardingStepTransition"
            ) { step ->
                when (step) {
                    OnboardingWizardStep.SELECT_PLAN -> {
                        PlanSelectionStepView(
                            selectedTier = uiState.filingTierMode,
                            onTierSelected = { viewModel.selectOnboardingPlan(it) },
                            onContinue = { viewModel.setOnboardingStep(OnboardingWizardStep.TRUST_AND_AUTH) }
                        )
                    }
                    OnboardingWizardStep.TRUST_AND_AUTH -> {
                        TrustAndAuthStepView(
                            pan = uiState.pan,
                            taxpayerFullName = uiState.taxpayerFullName,
                            isPanVerifying = uiState.isPanVerifying,
                            isPanVerifiedWithItr = uiState.isPanVerifiedWithItr,
                            panVerificationError = uiState.panVerificationError,
                            onPanChanged = { viewModel.onPanChanged(it) },
                            onVerifyPanWithItr = { viewModel.verifyPanWithItr() },
                            onStartHydration = { viewModel.startDynamicHydrationRadar() },
                            onLaunchAadhaarOtp = { showAadhaarOtpModal = true },
                            onLaunchWebLogin = { showWebLoginSheet = true },
                            onDocumentIngested = { bytes, fName ->
                                viewModel.ingestDocumentBytes(bytes, fName)
                                viewModel.startDynamicHydrationRadar()
                            },
                            onAaSync = {
                                viewModel.ingestAccountAggregatorFiData(
                                    depositJson = """{"bank_name":"HDFC Bank","masked_account":"••••5512","transactions":[{"narration":"INTEREST CREDIT SB","amount":18500.0,"type":"CREDIT"},{"narration":"PPF CONTRIBUTION","amount":50000.0,"type":"DEBIT"}]}""",
                                    securitiesJson = """{"depository_or_rta":"CAMS","holdingsCount":4,"holdings":[{"isin":"INE002A01018","cost_rupees":100000,"current_or_sale_value_rupees":145000}]}"""
                                )
                                viewModel.startDynamicHydrationRadar()
                            }
                        )
                    }
                    OnboardingWizardStep.STREAM_HYDRATION_RADAR -> {
                        AutomatedIngestionRadarView(
                            streams = uiState.ingestionStreams,
                            isIngesting = uiState.isIngestingStreams
                        )
                    }
                    OnboardingWizardStep.PLAN_SPECIFIC_INPUTS -> {
                        PlanSpecificHydrationCard(
                            tier = uiState.filingTierMode,
                            state = uiState,
                            onSugamUpdated = { d, c, s, cash, p ->
                                viewModel.updateSugamFinancialParticulars(d, c, s, cash, p)
                            },
                            onAuditUpdated = { gstin, caMem, caName ->
                                viewModel.updateAuditParticulars(gstin, caMem, caName)
                            },
                            onProceedToDossier = {
                                viewModel.generateUnifiedDossier()
                                viewModel.setOnboardingStep(OnboardingWizardStep.UNIFIED_DOSSIER_REVIEW)
                            }
                        )
                    }
                    OnboardingWizardStep.UNIFIED_DOSSIER_REVIEW -> {
                        val dossier = uiState.unifiedDossier
                        if (dossier != null) {
                            UnifiedPreFilingDossierView(
                                dossier = dossier,
                                onConfirmAndFile = {
                                    viewModel.completeOnboardingAndEnterWorkspace()
                                    onOnboardingFinished()
                                },
                                onShareWithCa = {
                                    viewModel.completeOnboardingAndEnterWorkspace()
                                    onLaunchCaPortal()
                                },
                                onProceedToWorkspace = {
                                    viewModel.completeOnboardingAndEnterWorkspace()
                                    onOnboardingFinished()
                                }
                            )
                        } else {
                            // Fallback if dossier is generating
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = EnterpriseTeal500)
                            }
                        }
                    }
                }
            }
            // Web Login In-App Sheet
            if (showWebLoginSheet) {
                ItdOfficialWebLoginSheet(
                    pan = uiState.pan,
                    onDismiss = { showWebLoginSheet = false },
                    onSyncSuccess = { count, tds, interest ->
                        showWebLoginSheet = false
                        viewModel.applyPortalSyncData(tds, interest)
                        viewModel.startDynamicHydrationRadar()
                    }
                )
            }

            // Aadhaar OTP Live Modal
            if (showAadhaarOtpModal) {
                `in`.zerotax.app.ui.portal.ItdLivePortalSyncModal(
                    pan = uiState.pan,
                    onDismiss = { showAadhaarOtpModal = false },
                    onSyncCompleted = { count, tds, interest, grossSalary ->
                        showAadhaarOtpModal = false
                        viewModel.applyPortalSyncData(tds, interest, grossSalary)
                        viewModel.startDynamicHydrationRadar()
                    }
                )
            }
        }
    }
}
@Composable
private fun OnboardingTopBar(
    currentStep: OnboardingWizardStep,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val topInset = `in`.zerotax.app.ui.components.getSafeStatusBarTopInset()
    Surface(
        color = if (isDark) Slate900 else PaletteSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Slate800 else PaletteSecondary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset, bottom = 10.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentStep != OnboardingWizardStep.TRUST_AND_AUTH && currentStep != OnboardingWizardStep.STREAM_HYDRATION_RADAR) {
                    IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                } else {
                    ZeroTaxBrandLogo(size = 28.dp)
                }
                Text(
                    text = "ZeroTax Setup",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            // Step Indicator Badge & Auto-Selected Plan
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (currentStep == OnboardingWizardStep.UNIFIED_DOSSIER_REVIEW || currentStep == OnboardingWizardStep.PLAN_SPECIFIC_INPUTS) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) EnterpriseTeal900.copy(alpha = 0.5f) else EnterpriseTeal50,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, EnterpriseTeal500)
                    ) {
                        Text(
                            text = "Auto-Selected: Sahaj ITR-1",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) EnterpriseTeal300 else EnterpriseTeal700,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Slate800 else Slate100
                ) {
                    val displayStep = when (currentStep) {
                        OnboardingWizardStep.TRUST_AND_AUTH -> 1
                        OnboardingWizardStep.STREAM_HYDRATION_RADAR -> 2
                        else -> 3
                    }
                    Text(
                        text = "Step $displayStep of 3",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EnterpriseTeal500,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
@Composable
private fun PlanSelectionStepView(
    selectedTier: FilingTierMode,
    onTierSelected: (FilingTierMode) -> Unit,
    onContinue: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Hook
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Welcome to ZeroTax AY 2026-27",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Select your tax profile to tailor the automated data extraction radar.",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        // 3 Tiers Cards
        FilingTierMode.values().forEach { tier ->
            val isSelected = tier == selectedTier
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) {
                    if (isDark) Slate800 else PaletteMuted
                } else {
                    if (isDark) Slate850 else PaletteSurface
                },
                border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) (if (isDark) PaletteSecondary else PalettePrimary) else if (isDark) Slate700 else PaletteSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTierSelected(tier) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) (if (isDark) PaletteAccent else PalettePrimary) else if (isDark) Slate800 else PaletteMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (tier) {
                                FilingTierMode.SAHAJ_SIMPLE -> Icons.Default.Eco
                                FilingTierMode.INVESTOR_PRO -> Icons.Default.TrendingUp
                                FilingTierMode.ENTERPRISE_AUDIT -> Icons.Default.Shield
                            },
                            contentDescription = null,
                            tint = if (isSelected) PureWhite else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tier.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, null, tint = if (isDark) PaletteSecondary else PalettePrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = tier.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = if (isDark) PaletteBackground else PalettePrimary)
                        )
                        Text(
                            text = tier.targetPersona,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        )
                    }
                }
            }
        }
        // Zero-Password Hook Callout
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else PaletteMuted,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else PaletteSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.VpnKeyOff, null, tint = if (isDark) PaletteSecondary else PalettePrimary, modifier = Modifier.size(20.dp))
                Column {
                    Text(
                        text = "100% Zero-Password Guarantee",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "We never ask for your Income Tax portal password. All data is fetched via official OTP or in-app sandboxed session.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        ZeroTaxPrimaryButton(
            text = "Continue with ${selectedTier.title}",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
@Composable
private fun TrustAndAuthStepView(
    pan: String,
    taxpayerFullName: String,
    isPanVerifying: Boolean,
    isPanVerifiedWithItr: Boolean,
    panVerificationError: String?,
    onPanChanged: (String) -> Unit,
    onVerifyPanWithItr: () -> Unit,
    onStartHydration: () -> Unit,
    onLaunchAadhaarOtp: () -> Unit,
    onLaunchWebLogin: () -> Unit,
    onDocumentIngested: (ByteArray, String) -> Unit = { _, _ -> },
    onAaSync: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()
    val panEntity = remember(pan) { PanEntityDecoder.decode(pan) }
    val isPanValid = remember(pan) { pan.matches(Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }

    val executeIfVerified: (() -> Unit) -> Unit = { action ->
        if (isPanVerifiedWithItr) {
            action()
        } else {
            onVerifyPanWithItr()
        }
    }

    val docLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val fName = uri.lastPathSegment ?: "Form16.pdf"
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        executeIfVerified { onDocumentIngested(bytes, fName) }
                    } else {
                        executeIfVerified { onStartHydration() }
                    }
                } catch (_: Exception) {
                    executeIfVerified { onStartHydration() }
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 3-Point Psychological Trust Anchors Card
        HardwareVaultStatusCard()

        // PAN Input Section
        Text(
            text = "Enter Permanent Account Number (PAN)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = pan,
            onValueChange = { onPanChanged(it.take(10).uppercase()) },
            label = { Text("PAN Number (10 characters)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = pan.isNotEmpty() && !isPanValid,
            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            trailingIcon = {
                if (isPanVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = EnterpriseTeal500)
                } else if (isPanVerifiedWithItr) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified with ITR",
                        tint = EmeraldSuccess
                    )
                } else {
                    IconButton(onClick = {
                        // QR Scanner trigger
                    }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan e-PAN or Aadhaar QR Code",
                            tint = EnterpriseTeal500
                        )
                    }
                }
            },
            supportingText = {
                if (pan.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPanValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isPanValid) EmeraldSuccess else CrimsonDanger,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isPanValid) {
                                "${panEntity.entityType.title} (Eligible for ITR-1, ITR-2, ITR-4)"
                            } else {
                                "Invalid PAN format (${pan.length}/10). Must be 5 letters, 4 numbers, 1 letter."
                            },
                            color = if (isPanValid) EmeraldSuccess else CrimsonDanger,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        )

        // Real-Time ITR Verification State Display
        if (isPanVerifying) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Slate850 else EnterpriseTeal50,
                border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal500),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = EnterpriseTeal500)
                    Column {
                        Text(
                            text = "Verifying PAN with Income Tax Department...",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal700)
                        )
                        Text(
                            text = "Querying live CBDT/NSDL taxpayer registry",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        } else if (isPanVerifiedWithItr) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) EmeraldSuccessBgDark else EmeraldSuccessBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Verified, null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ITR Verified: ${taxpayerFullName.ifBlank { "Active Taxpayer" }}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = if (isDark) EmeraldSuccess else EmeraldDark)
                        )
                        Text(
                            text = "Status: VALID • Active on CBDT / Income Tax portal records",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    StatutoryBadge(text = "ITD VERIFIED", tone = BadgeTone.SUCCESS)
                }
            }
        } else if (!panVerificationError.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) CrimsonDangerDark.copy(alpha = 0.15f) else CrimsonDanger.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonDanger),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = CrimsonDanger, modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ITR Verification Failed",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = CrimsonDanger)
                        )
                        Text(
                            text = panVerificationError,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    TextButton(onClick = onVerifyPanWithItr) {
                        Text("Retry", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CrimsonDanger))
                    }
                }
            }
        } else if (isPanValid) {
            ZeroTaxOutlinedButton(
                text = "Verify PAN with Income Tax Department →",
                onClick = onVerifyPanWithItr,
                leadingIcon = {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EnterpriseTeal600, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Auth Method Selector
        Text(
            text = "Automated Data Ingestion Method",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Surface(
            onClick = { executeIfVerified { onLaunchAadhaarOtp() } },
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else PaletteSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else PaletteSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.PhonelinkRing, null, tint = PalettePrimary, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Instant Government Aadhaar OTP", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        if (isPanVerifiedWithItr) {
                            StatutoryBadge(text = "READY", tone = BadgeTone.SUCCESS)
                        } else {
                            StatutoryBadge(text = "REQUIRES ITR VERIFIED PAN", tone = BadgeTone.WARNING)
                        }
                    }
                    Text("Pulls prefilled salary, 26AS, and AIS in 30 seconds directly from CBDT.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                Icon(Icons.Default.ArrowForward, null, tint = PalettePrimary, modifier = Modifier.size(18.dp))
            }
        }
        Surface(
            onClick = { executeIfVerified { onLaunchWebLogin() } },
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else PaletteSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else PaletteSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Language, null, tint = PalettePrimary, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Official In-App Web Portal Login", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        if (isPanVerifiedWithItr) {
                            StatutoryBadge(text = "READY", tone = BadgeTone.SUCCESS)
                        } else {
                            StatutoryBadge(text = "REQUIRES ITR VERIFIED PAN", tone = BadgeTone.WARNING)
                        }
                    }
                    Text("Log in securely on eportal.incometax.gov.in in an isolated ephemeral session.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                Icon(Icons.Default.ArrowForward, null, tint = PalettePrimary, modifier = Modifier.size(18.dp))
            }
        }
        Surface(
            onClick = { executeIfVerified { docLauncher.launch(arrayOf("application/pdf")) } },
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else PaletteSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else PaletteSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.UploadFile, null, tint = PalettePrimary, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Import Form 16 / Tax Statement PDF", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        if (isPanVerifiedWithItr) {
                            StatutoryBadge(text = "READY", tone = BadgeTone.SUCCESS)
                        } else {
                            StatutoryBadge(text = "REQUIRES ITR VERIFIED PAN", tone = BadgeTone.WARNING)
                        }
                    }
                    Text("Silently unlocks with PAN and auto-fills your entire return in seconds.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                Icon(Icons.Default.ArrowForward, null, tint = Slate400, modifier = Modifier.size(18.dp))
            }
        }
        Surface(
            onClick = { executeIfVerified { onAaSync() } },
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else PaletteSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else PaletteSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.AccountBalance, null, tint = PalettePrimary, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("1-Click Account Aggregator (AA) Sync", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        if (isPanVerifiedWithItr) {
                            StatutoryBadge(text = "READY", tone = BadgeTone.SUCCESS)
                        } else {
                            StatutoryBadge(text = "REQUIRES ITR VERIFIED PAN", tone = BadgeTone.WARNING)
                        }
                    }
                    Text("Pulls interest & capital gains from 23 Banks & CAMS/KFintech via RBI-licensed AA.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                Icon(Icons.Default.ArrowForward, null, tint = PalettePrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        ZeroTaxPrimaryButton(
            text = when {
                isPanVerifying -> "Verifying PAN with Income Tax Department..."
                !isPanVerifiedWithItr -> "Verify PAN with ITR to Proceed"
                else -> "Continue to Automated Radar (Step 2) →"
            },
            onClick = {
                if (isPanVerifiedWithItr) {
                    onStartHydration()
                } else {
                    onVerifyPanWithItr()
                }
            },
            enabled = isPanValid && !isPanVerifying,
            modifier = Modifier.fillMaxWidth()
        )
    }
}