package `in`.zerotax.app.ui.onboarding

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

    Scaffold(
        topBar = {
            OnboardingTopBar(
                currentStep = uiState.onboardingStep,
                onBack = {
                    when (uiState.onboardingStep) {
                        OnboardingWizardStep.TRUST_AND_AUTH -> viewModel.setOnboardingStep(OnboardingWizardStep.SELECT_PLAN)
                        OnboardingWizardStep.PLAN_SPECIFIC_INPUTS -> viewModel.setOnboardingStep(OnboardingWizardStep.STREAM_HYDRATION_RADAR)
                        OnboardingWizardStep.UNIFIED_DOSSIER_REVIEW -> viewModel.setOnboardingStep(OnboardingWizardStep.PLAN_SPECIFIC_INPUTS)
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
                            onPanChanged = { viewModel.onPanChanged(it) },
                            onStartHydration = { viewModel.startDynamicHydrationRadar() },
                            onLaunchWebLogin = { showWebLoginSheet = true }
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
                    pan = uiState.pan.ifBlank { "AAAPA1234A" },
                    onDismiss = { showWebLoginSheet = false },
                    onSyncSuccess = { count, tds, interest ->
                        showWebLoginSheet = false
                        viewModel.applyPortalSyncData(tds, interest)
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

    Surface(
        color = if (isDark) Slate900 else PureWhite,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Slate800 else Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentStep != OnboardingWizardStep.SELECT_PLAN && currentStep != OnboardingWizardStep.STREAM_HYDRATION_RADAR) {
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

            // Step Indicator Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Slate800 else Slate100
            ) {
                Text(
                    text = "Step ${currentStep.stepNumber} of 5",
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
                    if (isDark) EnterpriseTeal900.copy(alpha = 0.5f) else EnterpriseTeal50
                } else {
                    if (isDark) Slate850 else PureWhite
                },
                border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) EnterpriseTeal500 else if (isDark) Slate700 else Slate300
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
                            .background(if (isSelected) EnterpriseTeal500 else if (isDark) Slate800 else Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (tier) {
                                FilingTierMode.SAHAJ_SIMPLE -> Icons.Default.Eco
                                FilingTierMode.INVESTOR_PRO -> Icons.Default.TrendingUp
                                FilingTierMode.ENTERPRISE_AUDIT -> Icons.Default.Shield
                            },
                            contentDescription = null,
                            tint = if (isSelected) Slate950 else MaterialTheme.colorScheme.onSurface,
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
                                Icon(Icons.Default.CheckCircle, null, tint = EnterpriseTeal500, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = tier.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = EnterpriseTeal600)
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
            color = if (isDark) Slate850 else Slate100,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else Slate300),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.VpnKeyOff, null, tint = EnterpriseTeal500, modifier = Modifier.size(20.dp))
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
    onPanChanged: (String) -> Unit,
    onStartHydration: () -> Unit,
    onLaunchWebLogin: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()
    val panEntity = remember(pan) { PanEntityDecoder.decode(pan) }
    val isPanValid = remember(pan) { pan.matches(Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Radical Trust Shield Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) EnterpriseTeal900.copy(alpha = 0.4f) else EnterpriseTeal50,
            border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal500),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Security, null, tint = EnterpriseTeal500, modifier = Modifier.size(24.dp))
                    Text(
                        text = "Client-Side Zero-Knowledge Shield",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = if (isDark) EnterpriseTeal300 else EnterpriseTeal700)
                    )
                }
                Text(
                    text = "• Android KeyStore Hardware Encryption (AES-256-GCM / SQLCipher)\n" +
                           "• Digital Personal Data Protection (DPDP) Act 2023 Statutory Consent\n" +
                           "• 1-Tap Complete Data Shredding at any time",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        // PAN Input Section
        Text(
            text = "Enter Permanent Account Number (PAN)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = pan,
            onValueChange = { onPanChanged(it.take(10).uppercase()) },
            label = { Text("PAN Number (e.g. ABCDE1234F)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            supportingText = {
                if (pan.length == 10) {
                    Text(
                        text = "Detected: ${panEntity.entityType.title} (${panEntity.statusSummary})",
                        color = if (isPanValid) EmeraldSuccess else CrimsonDanger
                    )
                }
            }
        )

        // Auth Method Selector
        Text(
            text = "Automated Data Ingestion Method",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else Slate300),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStartHydration() }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.PhonelinkRing, null, tint = EnterpriseTeal500, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Instant Government Aadhaar OTP", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Pulls prefilled salary, 26AS, and AIS in 30 seconds directly from CBDT.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                Icon(Icons.Default.ArrowForward, null, tint = Slate400, modifier = Modifier.size(18.dp))
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else Slate300),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLaunchWebLogin() }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Language, null, tint = EnterpriseTeal500, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Official In-App Web Portal Login", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Log in securely on eportal.incometax.gov.in in an isolated ephemeral session.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                Icon(Icons.Default.ArrowForward, null, tint = Slate400, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        ZeroTaxPrimaryButton(
            text = "Start Automated Data Hydration",
            onClick = onStartHydration,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
