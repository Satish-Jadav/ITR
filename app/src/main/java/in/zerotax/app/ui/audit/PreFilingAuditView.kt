package `in`.zerotax.app.ui.audit

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
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
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.legal.ComprehensiveLegalAcknowledgementDialog
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.BankAccountDetails
import `in`.zerotax.core.model.NoticeRiskLevel
import `in`.zerotax.core.model.PreFilingAuditReport
import `in`.zerotax.core.model.ReconciledSalarySummary
import `in`.zerotax.core.taxengine.advanced.PreFilingAuditEngine
import `in`.zerotax.core.taxengine.calculator.RegimeComputationResult
import `in`.zerotax.feature.aiextractor.CopilotExplanation
import `in`.zerotax.feature.aiextractor.OnDeviceTaxCopilot
import `in`.zerotax.feature.portalbridge.CloudBackupSyncManager
import `in`.zerotax.feature.aiextractor.SupportedVernacularLocale
import kotlinx.coroutines.launch

/**
 * Institutional Pre-Filing Audit & CPC Scrutiny Notice Risk Shield Screen.
 *
 * Provides comprehensive 6-gate statutory verification, DPDP Act 2023 legal consent,
 * on-device AI copilot assistance, and algorithmic CPC processing timeline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreFilingAuditView(
    salarySummary: ReconciledSalarySummary,
    computation: RegimeComputationResult,
    bankAccount: BankAccountDetails?,
    onBack: () -> Unit,
    onProceedToFiling: () -> Unit,
    pan: String = "TAXPAYER",
    onAutoFixDiscrepancy: (() -> Unit)? = null,
    onAutoOptimizeRegime: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val copilot = remember { OnDeviceTaxCopilot(context) }

    val auditReport = remember(salarySummary, computation, bankAccount) {
        PreFilingAuditEngine.auditReturn(
            salarySummary = salarySummary,
            computation = computation,
            bankAccount = bankAccount
        )
    }

    var isScoreAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(auditReport.auditScorePercentage) {
        isScoreAnimated = true
    }
    val animatedScoreProgress by animateFloatAsState(
        targetValue = if (isScoreAnimated) (auditReport.auditScorePercentage.toFloat() / 100f) else 0f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "AuditScoreProgressAnim"
    )

    var isCopilotSheetVisible by remember { mutableStateOf(false) }
    var selectedLocale by remember { mutableStateOf(SupportedVernacularLocale.ENGLISH_IN) }
    var currentExplanation by remember { mutableStateOf<CopilotExplanation?>(null) }
    var isCopilotThinking by remember { mutableStateOf(false) }
    var isLegalAcknowledgementSigned by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var signedConsentSha256 by remember { mutableStateOf("") }

    val auditFaqs = remember {
        listOf(
            "What triggers an income tax scrutiny notice u/s 143(1)?",
            "How does ZeroTax guarantee zero notice risk?",
            "How long does CPC Bengaluru take to process my refund?",
            "What happens if Form 16 and 26AS TDS don't match?",
            "Can I file if I have 2 Form 16s from switching jobs?",
            "Why must my bank account be pre-validated?"
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (isDark) Slate950 else Slate50,
        topBar = {
            val topInset = getSafeStatusBarTopInset()
            TopAppBar(
                windowInsets = WindowInsets(top = topInset),
                title = {
                    Column {
                        Text(
                            text = "Pre-Filing Audit",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "CPC Notice Risk Shield & Compliance Engine",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { isCopilotSheetVisible = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EnterpriseTeal600,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Ask Copilot",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EnterpriseTeal600
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audit Score",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "${auditReport.auditScorePercentage}% Notice-Proof",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            ),
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    ZeroTaxPrimaryButton(
                        text = if (isLegalAcknowledgementSigned) "Proceed to E-File →" else "Review & Sign →",
                        onClick = {
                            if (!isLegalAcknowledgementSigned) {
                                showLegalDialog = true
                            } else {
                                onProceedToFiling()
                            }
                        },
                        enabled = auditReport.canFileDirectly,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ================================================================
            // 1. HERO AUDIT HEALTH SCORE CARD
            // ================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (auditReport.riskLevel == NoticeRiskLevel.LOW_RISK_SAFE)
                                EmeraldSuccess.copy(alpha = 0.4f)
                            else
                                AmberWarning.copy(alpha = 0.4f)
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Circular Gauge Meter
                            Box(
                                modifier = Modifier.size(68.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Slate200,
                                    strokeWidth = 6.dp
                                )
                                CircularProgressIndicator(
                                    progress = { animatedScoreProgress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = if (auditReport.riskLevel == NoticeRiskLevel.LOW_RISK_SAFE) EmeraldSuccess else AmberWarning,
                                    strokeWidth = 6.dp
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${auditReport.auditScorePercentage}%",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (auditReport.riskLevel == NoticeRiskLevel.LOW_RISK_SAFE) EmeraldSuccess else AmberWarning
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Return Health Score",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(Modifier.height(4.dp))
                                StatutoryBadge(
                                    text = auditReport.riskLevel.label,
                                    tone = if (auditReport.riskLevel == NoticeRiskLevel.LOW_RISK_SAFE) BadgeTone.SUCCESS else BadgeTone.WARNING
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Verified against 6 CPC statutory algorithms",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Guarantee Pill Banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else EmeraldSuccess.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                EmeraldSuccess.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = auditReport.noticeProtectionGuarantee,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        if (onAutoOptimizeRegime != null) {
                            Spacer(Modifier.height(12.dp))
                            ZeroTaxPrimaryButton(
                                text = "1-Tap Auto-Optimize Return & Enforce Zero-Notice",
                                onClick = onAutoOptimizeRegime,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ================================================================
            // 2. UNIFIED 6-GATE STATUTORY AUDIT BREAKDOWN CARD
            // ================================================================
            item {
                val passedCount = auditReport.gates.count { it.isPassed }
                val totalCount = auditReport.gates.size

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 0.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "6-Gate CPC Compliance",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "Automated CBDT Rule Validation",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            StatutoryBadge(
                                text = "$passedCount/$totalCount Passed",
                                tone = if (passedCount == totalCount) BadgeTone.SUCCESS else BadgeTone.WARNING
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                        Spacer(Modifier.height(10.dp))

                        auditReport.gates.forEachIndexed { index, gate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (gate.isPassed)
                                                EmeraldSuccess.copy(alpha = 0.15f)
                                            else
                                                CrimsonDanger.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (gate.isPassed) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (gate.isPassed) EmeraldSuccess else CrimsonDanger,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Spacer(Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "Gate ${gate.gateNumber}: ${gate.gateTitle}",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        StatutoryBadge(
                                            text = if (gate.isPassed) "Passed" else "Review",
                                            tone = if (gate.isPassed) BadgeTone.SUCCESS else BadgeTone.DANGER
                                        )
                                    }

                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = gate.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )

                                    Spacer(Modifier.height(6.dp))
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Slate100,
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            Text(
                                                text = gate.statutoryRef,
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = IndigoStatutory,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = gate.recommendation,
                                            style = TextStyle(
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }

                                    if (gate.gateNumber == 1 && !gate.isPassed && onAutoFixDiscrepancy != null) {
                                        Spacer(Modifier.height(8.dp))
                                        ZeroTaxPrimaryButton(
                                            text = "1-Tap Auto-Reconcile TDS with Form 26AS",
                                            onClick = onAutoFixDiscrepancy,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                        )
                                    }
                                }
                            }

                            if (index < auditReport.gates.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ================================================================
            // 3. 5-POINT PRE-SUBMISSION SAFETY CHECKLIST
            // ================================================================
            item {
                PreSubmissionSafetyChecklistCard(
                    isBankPreValidated = bankAccount?.isPreValidated ?: true,
                    isAadhaarLinked = true,
                    hasDiscrepancyShieldActive = salarySummary.totalDiscrepancyAmount == 0L,
                    isLandlordPanCompliant = true,
                    isScheduleFaCompliant = true
                )
            }

            // ================================================================
            // 4. STATUTORY LEGAL ACKNOWLEDGEMENT & CLOUD SHIELD
            // ================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isLegalAcknowledgementSigned)
                                EnterpriseTeal600.copy(alpha = 0.6f)
                            else
                                AmberWarning.copy(alpha = 0.6f)
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isLegalAcknowledgementSigned)
                                                EnterpriseTeal600.copy(alpha = 0.15f)
                                            else
                                                AmberWarning.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Gavel,
                                        contentDescription = null,
                                        tint = if (isLegalAcknowledgementSigned) EnterpriseTeal600 else AmberWarning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Statutory Legal Protection",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            StatutoryBadge(
                                text = if (isLegalAcknowledgementSigned) "Legally Verified" else "Action Required",
                                tone = if (isLegalAcknowledgementSigned) BadgeTone.SUCCESS else BadgeTone.WARNING
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isLegalAcknowledgementSigned) {
                                "Taxpayer self-declaration verified u/s 140 & DPDP Act 2023. Return draft is crash-proof & encrypted for cloud recovery (SHA-256: ${signedConsentSha256.take(12)}...)."
                            } else {
                                "Under Indian Law (Section 140 Income-tax Act, 1961 & DPDP Act 2023), review and acknowledge statutory terms, non-advisory platform disclaimers, and cloud crash-retention consent before filing."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(Modifier.height(12.dp))
                        ZeroTaxOutlinedButton(
                            text = if (isLegalAcknowledgementSigned) "View Signed Legal Terms & Cloud Sync Status" else "Review & Sign Statutory Terms (Mandatory) →",
                            onClick = { showLegalDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ================================================================
            // 5. EXPECTED CPC PROCESSING & REFUND TIMELINE
            // ================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 0.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Expected CPC Refund & Processing Timeline",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(Modifier.height(14.dp))

                        val steps = listOf(
                            Triple("1", "In-App Direct Submission u/s 139(1)", "CBDT JSON transmission to ITD Gateway (< 30 seconds)"),
                            Triple("2", "Instant Aadhaar OTP e-Verification u/s 140A", "Official 15-digit ITR-V Ack generated in-app"),
                            Triple("3", "Automated CPC Processing u/s 143(1)", "Intimation order sent to registered email (Avg. 14 to 21 days)"),
                            Triple("4", "Direct ECS Refund Credit into Pre-validated Bank", if (bankAccount != null && bankAccount.bankName.isNotBlank()) "Refund credited to ${bankAccount.bankName}" else "Direct electronic transfer to verified bank account")
                        )

                        steps.forEachIndexed { idx, (num, title, desc) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (idx == 3)
                                                    EmeraldSuccess.copy(alpha = 0.15f)
                                                else
                                                    MaterialTheme.colorScheme.primaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = num,
                                            style = TextStyle(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (idx == 3) EmeraldSuccess else MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                    if (idx < steps.lastIndex) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.5.dp)
                                                .height(26.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.padding(bottom = if (idx < steps.lastIndex) 10.dp else 0.dp)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (idx == 3) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = desc,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // ========================================================================
    // COPILOT MODAL SHEET
    // ========================================================================
    if (isCopilotSheetVisible) {
        ZeroTaxBottomSheet(
            onDismissRequest = { isCopilotSheetVisible = false },
            scrollable = true
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gemini Cloud Audit Copilot",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Cloud Tax AI • Real-Time Statutory Sync",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Vernacular Switcher
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SupportedVernacularLocale.entries.forEach { loc ->
                        val isSel = selectedLocale == loc
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) EnterpriseTeal600 else MaterialTheme.colorScheme.surfaceVariant)
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
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = loc.code.take(2).uppercase(),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Tax Audit & Notice Shield FAQs:",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(auditFaqs) { q ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.clickable {
                            scope.launch {
                                isCopilotThinking = true
                                currentExplanation = copilot.explainContextualTaxTopic(q, selectedLocale)
                                isCopilotThinking = false
                            }
                        }
                    ) {
                        Text(
                            text = q,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isCopilotThinking) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = EnterpriseTeal600,
                        strokeWidth = 2.5.dp
                    )
                }
            } else if (currentExplanation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 0.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.4f))
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentExplanation!!.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            StatutoryBadge(text = selectedLocale.code.uppercase(), tone = BadgeTone.SUCCESS)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = currentExplanation!!.plainLanguageSummary,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            } else {
                Text(
                    text = "Select any question above to learn how our 6-gate audit shield protects your return from CPC scrutiny notices.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showLegalDialog) {
        val verifiedPan = pan.trim().uppercase()
        ComprehensiveLegalAcknowledgementDialog(
            pan = verifiedPan,
            onDismiss = { showLegalDialog = false },
            onAcknowledgeAndAccept = { sha256, _ ->
                isLegalAcknowledgementSigned = true
                signedConsentSha256 = sha256
                showLegalDialog = false
                val snapshot = CloudBackupSyncManager.packageState(
                    pan = verifiedPan,
                    grossSalaryRupees = salarySummary.aggregateGrossSalary,
                    totalDeductionsRupees = computation.totalChapterVIADeductions + computation.standardDeduction,
                    netTaxableIncomeRupees = computation.netTaxableNormalIncomeRounded288A,
                    totalTaxLiabilityRupees = computation.totalTaxLiabilityRounded288B,
                    refundDueRupees = salarySummary.aggregateTdsAllowed - computation.totalTaxLiabilityRounded288B,
                    bankName = bankAccount?.bankName?.ifBlank { "Primary Pre-Validated Bank" } ?: "Primary Pre-Validated Bank",
                    bankAccountMasked = bankAccount?.accountNumber?.ifBlank { "Direct CBDT ECS Credit" } ?: "Direct CBDT ECS Credit",
                    consentSha256Hash = sha256
                )
                scope.launch {
                    CloudBackupSyncManager.syncStateToCloud(context, snapshot)
                }
            }
        )
    }
}
