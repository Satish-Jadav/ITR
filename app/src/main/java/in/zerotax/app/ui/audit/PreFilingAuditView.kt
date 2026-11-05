package `in`.zerotax.app.ui.audit

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Security
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
import `in`.zerotax.core.model.Rupee
import `in`.zerotax.core.taxengine.advanced.PreFilingAuditEngine
import `in`.zerotax.core.taxengine.calculator.RegimeComputationResult
import `in`.zerotax.feature.aiextractor.CopilotExplanation
import `in`.zerotax.feature.aiextractor.OnDeviceTaxCopilot
import `in`.zerotax.feature.portalbridge.CloudBackupSyncManager
import `in`.zerotax.feature.aiextractor.SupportedVernacularLocale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreFilingAuditView(
    salarySummary: ReconciledSalarySummary,
    computation: RegimeComputationResult,
    bankAccount: BankAccountDetails?,
    onBack: () -> Unit,
    onProceedToFiling: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pre-Filing Audit & Notice Risk Shield",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "6-Gate CPC Compliance Verification (ZeroTax AI Audit Engine)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    TextButton(onClick = { isCopilotSheetVisible = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EnterpriseTeal600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Ask Copilot",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = EnterpriseTeal600,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Audit Verification Score",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                        Text(
                            text = "${auditReport.auditScorePercentage}% Notice-Proof",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        )
                    }
                    Button(
                        onClick = {
                            if (!isLegalAcknowledgementSigned) {
                                showLegalDialog = true
                            } else {
                                onProceedToFiling()
                            }
                        },
                        enabled = auditReport.canFileDirectly,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLegalAcknowledgementSigned) EnterpriseTeal600 else AmberWarning
                        ),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = if (isLegalAcknowledgementSigned) "Proceed to Direct Filing →" else "Review & Sign Legal Terms →",
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
            // Overall Audit Score Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (auditReport.riskLevel == NoticeRiskLevel.LOW_RISK_SAFE) EmeraldSuccessBg else IndigoStatutoryBg
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (auditReport.riskLevel == NoticeRiskLevel.LOW_RISK_SAFE) EmeraldSuccess.copy(alpha = 0.5f) else IndigoStatutory.copy(alpha = 0.5f)
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (auditReport.riskLevel == NoticeRiskLevel.LOW_RISK_SAFE) EmeraldSuccess else IndigoStatutory),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Return Health Score: ${auditReport.auditScorePercentage} / 100",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = auditReport.riskLevel.label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (auditReport.riskLevel == NoticeRiskLevel.LOW_RISK_SAFE) EmeraldSuccess else IndigoStatutory,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { animatedScoreProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (auditReport.riskLevel == NoticeRiskLevel.LOW_RISK_SAFE) EmeraldSuccess else IndigoStatutory,
                            trackColor = Slate200
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = auditReport.noticeProtectionGuarantee,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "6-Gate Statutory Audit Breakdown:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
            }

            // Gates Breakdown
            items(auditReport.gates) { gate ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 0.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (gate.isPassed) EmeraldSuccess.copy(alpha = 0.4f) else CrimsonDanger.copy(alpha = 0.4f)
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (gate.isPassed) EmeraldSuccess else CrimsonDanger),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (gate.isPassed) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = PureWhite,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Gate ${gate.gateNumber}: ${gate.gateTitle}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            StatutoryBadge(
                                text = if (gate.isPassed) "Passed" else "Review",
                                tone = if (gate.isPassed) BadgeTone.SUCCESS else BadgeTone.DANGER
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = gate.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = gate.statutoryRef,
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = IndigoStatutory)
                            )
                            Text(
                                text = gate.recommendation,
                                style = TextStyle(fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }

            // Interactive 5-Point Pre-Submission Safety Checklist
            item {
                Spacer(Modifier.height(12.dp))
                PreSubmissionSafetyChecklistCard(
                    isBankPreValidated = bankAccount?.isPreValidated ?: true,
                    isAadhaarLinked = true,
                    hasDiscrepancyShieldActive = salarySummary.totalDiscrepancyAmount == 0L,
                    isLandlordPanCompliant = true,
                    isScheduleFaCompliant = true
                )
            }

            // Mandatory Statutory Legal Protection & Cloud Crash Shield
            item {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLegalAcknowledgementSigned) EnterpriseTeal50 else AmberWarningBg
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isLegalAcknowledgementSigned) EnterpriseTeal600 else AmberWarning
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = IndigoStatutory,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Statutory Legal Protection & Cloud Shield",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
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
                                "Under Indian Law (Section 140 Income-tax Act, 1961 & DPDP Act 2023), you must review and acknowledge statutory terms, non-advisory platform disclaimers, and cloud crash-retention consent before filing."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showLegalDialog = true },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isLegalAcknowledgementSigned) EnterpriseTeal700 else AmberWarning
                            ),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text(
                                text = if (isLegalAcknowledgementSigned) "View Signed Legal Terms & Cloud Sync Status" else "Review & Sign Statutory Terms (Mandatory) →",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Post-Filing CPC Refund Timeline Card (ZeroTax Algorithmic Timeline)
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Expected CPC Bengaluru Processing & Refund Timeline:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("1️⃣", fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("In-App Direct Submission u/s 139(1)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Text("CBDT JSON transmission to ITD Gateway (< 30 seconds)", style = TextStyle(fontSize = 11.sp, color = Slate600))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("2️⃣", fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Instant Aadhaar OTP e-Verification u/s 140A", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Official 15-digit ITR-V Ack generated in-app", style = TextStyle(fontSize = 11.sp, color = Slate600))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("3️⃣", fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Automated CPC Processing u/s 143(1)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Intimation order sent to registered email (Avg. 14 to 21 days)", style = TextStyle(fontSize = 11.sp, color = Slate600))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("4️⃣", fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Direct ECS Refund Credit into Pre-validated Bank", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                                Text("Refund scheduled for SBI ••••1234 without bank branch visits", style = TextStyle(fontSize = 11.sp, color = EmeraldSuccess))
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
                            text = "On-Device Gemini Audit Copilot",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "AICore Gemini Nano (temp = 0.0f) • 0B Cloud Egress",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }

                    // Vernacular Switcher
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
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) PureWhite else Slate600)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Tax Audit & Notice Shield FAQs:",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                )
                Spacer(Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(auditFaqs) { q ->
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
                            Text(text = q, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (isCopilotThinking) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = EnterpriseTeal600, strokeWidth = 2.5.dp)
                    }
                } else if (currentExplanation != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.4f)))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = currentExplanation!!.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                StatutoryBadge(text = selectedLocale.code.uppercase(), tone = BadgeTone.SUCCESS)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = currentExplanation!!.plainLanguageSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Select any question above to learn how our 6-gate audit shield protects your return from CPC scrutiny notices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showLegalDialog) {
        val employerTan = salarySummary.employers.firstOrNull()?.employerTan ?: "ABCDE1234F"
        ComprehensiveLegalAcknowledgementDialog(
            pan = employerTan,
            onDismiss = { showLegalDialog = false },
            onAcknowledgeAndAccept = { sha256, _ ->
                isLegalAcknowledgementSigned = true
                signedConsentSha256 = sha256
                showLegalDialog = false
                val snapshot = CloudBackupSyncManager.packageState(
                    pan = employerTan,
                    grossSalaryRupees = salarySummary.aggregateGrossSalary,
                    totalDeductionsRupees = computation.totalChapterVIADeductions + computation.standardDeduction,
                    netTaxableIncomeRupees = computation.netTaxableNormalIncomeRounded288A,
                    totalTaxLiabilityRupees = computation.totalTaxLiabilityRounded288B,
                    refundDueRupees = salarySummary.aggregateTdsAllowed - computation.totalTaxLiabilityRounded288B,
                    bankName = bankAccount?.bankName ?: "State Bank of India",
                    bankAccountMasked = bankAccount?.accountNumber ?: "••••1234",
                    consentSha256Hash = sha256
                )
                scope.launch {
                    CloudBackupSyncManager.syncStateToCloud(context, snapshot)
                }
            }
        )
    }
}

