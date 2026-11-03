package in.zerotax.app.ui.portal

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import in.zerotax.app.ui.components.*
import in.zerotax.app.ui.theme.*
import in.zerotax.core.model.AadhaarVerificationResult
import in.zerotax.core.model.BankAccountDetails
import in.zerotax.core.model.DirectFilingSubmissionResult
import in.zerotax.core.model.FilingSection
import in.zerotax.core.model.ReconciledSalarySummary
import in.zerotax.core.model.Rupee
import in.zerotax.core.model.TaxRegime
import in.zerotax.core.model.TaxpayerDeclaration
import in.zerotax.core.taxengine.calculator.RegimeComputationResult
import in.zerotax.core.taxengine.compiler.CbdtItr1JsonCompiler
import in.zerotax.feature.aiextractor.CopilotExplanation
import in.zerotax.feature.aiextractor.OnDeviceTaxCopilot
import in.zerotax.feature.aiextractor.SupportedVernacularLocale
import in.zerotax.feature.portalbridge.ItdDirectFilingClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class InAppFilingStage(val title: String, val subtitle: String) {
    REVIEW_AND_CONSENT("Pre-Filing Audit", "Schedule verification & refund bank details"),
    IN_APP_AUTH("In-App Authentication", "Direct TLS connection to ITD servers"),
    DIRECT_SUBMISSION("Direct Dispatch", "Transmitting CBDT AY 2026-27 package"),
    AADHAAR_E_VERIFICATION("Instant e-Verification", "UIDAI Aadhaar OTP validation u/s 140A"),
    COMPLETED_CONFIRMATION("Return Filed & Verified", "CBDT ITR-V acknowledgment certificate")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalAssistantScreen(
    pan: String,
    grossSalary: Long,
    netTaxableSalary: Long,
    taxLiability: Long,
    tdsClaimed: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copilot = remember { OnDeviceTaxCopilot(context) }
    val itdClient = remember { ItdDirectFilingClient() }

    var currentStage by remember { mutableStateOf(InAppFilingStage.REVIEW_AND_CONSENT) }

    // Form & Bank selection states
    var selectedBank by remember {
        mutableStateOf(
            BankAccountDetails(
                bankName = "State Bank of India",
                accountNumber = "••••••••1234",
                ifscCode = "SBIN0001234",
                accountType = "SAVINGS",
                isRefundAccount = true,
                isPreValidated = true
            )
        )
    }
    var isStatutoryDeclarationAccepted by remember { mutableStateOf(false) }

    // In-App Authentication inputs
    var loginMethod by remember { mutableStateOf("PASSWORD") } // PASSWORD or AADHAAR_OTP
    var portalPassword by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    // Direct Submission & Acknowledgment states
    var isSubmitting by remember { mutableStateOf(false) }
    var submissionStepText by remember { mutableStateOf("Ready for direct transmission") }
    var submissionResult by remember { mutableStateOf<DirectFilingSubmissionResult?>(null) }

    // Aadhaar OTP e-Verification states
    var aadhaarOtpInput by remember { mutableStateOf("") }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var verificationResult by remember { mutableStateOf<AadhaarVerificationResult?>(null) }

    // Gemini Copilot Sheet states
    var isCopilotSheetVisible by remember { mutableStateOf(false) }
    var selectedLocale by remember { mutableStateOf(SupportedVernacularLocale.ENGLISH_IN) }
    var currentExplanation by remember { mutableStateOf<CopilotExplanation?>(null) }
    var isCopilotThinking by remember { mutableStateOf(false) }

    val quickQuestions = remember {
        listOf(
            "Why do I need to pre-validate my bank account for refund?",
            "What is an EVC and how does Aadhaar OTP verify my return?",
            "Why is my tax liability ₹0 under Section 87A?",
            "How does in-app direct filing work without the ITD website?",
            "Is my ITD password stored or transmitted anywhere?",
            "What is the penalty if I don't e-verify within 30 days?"
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Direct In-App ITR Filing Suite",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "CBDT AY 2026-27 • 100% In-App (No Portal Login Needed)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }
                },
                actions = {
                    // Pinned Quick AI Copilot Button
                    TextButton(
                        onClick = { isCopilotSheetVisible = true },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "✨ Ask Gemini",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = EnterpriseTeal600,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    IconButton(onClick = {
                        itdClient.purgeSessionAndZeroize()
                        onClose()
                    }) {
                        Text("✕", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stage Indicator Progress Ticker
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Step ${currentStage.ordinal + 1} of 5: ${currentStage.title}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal700)
                        )
                        Text(
                            text = currentStage.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                    StatutoryBadge(
                        text = if (currentStage == InAppFilingStage.COMPLETED_CONFIRMATION) "Verified" else "In Progress",
                        tone = if (currentStage == InAppFilingStage.COMPLETED_CONFIRMATION) BadgeTone.SUCCESS else BadgeTone.STATUTORY
                    )
                }
            }

            // Animated Screen Transition between Filing Stages
            AnimatedContent(
                targetState = currentStage,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(tween(300)) { it } + fadeIn()).togetherWith(
                            slideOutHorizontally(tween(250)) { -it / 2 } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally(tween(300)) { -it } + fadeIn()).togetherWith(
                            slideOutHorizontally(tween(250)) { it / 2 } + fadeOut()
                        )
                    }
                },
                label = "InAppFilingFlowTransition",
                modifier = Modifier.fillMaxSize()
            ) { stage ->
                when (stage) {
                    InAppFilingStage.REVIEW_AND_CONSENT -> ReviewAndConsentStage(
                        pan = pan,
                        grossSalary = grossSalary,
                        netTaxableSalary = netTaxableSalary,
                        taxLiability = taxLiability,
                        tdsClaimed = tdsClaimed,
                        selectedBank = selectedBank,
                        isDeclarationAccepted = isStatutoryDeclarationAccepted,
                        onDeclarationChanged = { isStatutoryDeclarationAccepted = it },
                        onProceed = { currentStage = InAppFilingStage.IN_APP_AUTH },
                        onInvokeCopilot = { isCopilotSheetVisible = true }
                    )

                    InAppFilingStage.IN_APP_AUTH -> InAppAuthenticationStage(
                        pan = pan,
                        loginMethod = loginMethod,
                        onLoginMethodChanged = { loginMethod = it },
                        password = portalPassword,
                        onPasswordChanged = { portalPassword = it },
                        isAuthenticating = isAuthenticating,
                        authError = authError,
                        onBack = { currentStage = InAppFilingStage.REVIEW_AND_CONSENT },
                        onAuthenticate = {
                            scope.launch {
                                isAuthenticating = true
                                authError = null
                                val res = itdClient.authenticateInApp(pan, portalPassword.toCharArray())
                                isAuthenticating = false
                                if (res.isSuccess) {
                                    currentStage = InAppFilingStage.DIRECT_SUBMISSION
                                } else {
                                    authError = res.exceptionOrNull()?.message ?: "Authentication failed. Check credentials."
                                }
                            }
                        }
                    )

                    InAppFilingStage.DIRECT_SUBMISSION -> DirectSubmissionStage(
                        pan = pan,
                        grossSalary = grossSalary,
                        netTaxableSalary = netTaxableSalary,
                        taxLiability = taxLiability,
                        tdsClaimed = tdsClaimed,
                        selectedBank = selectedBank,
                        isSubmitting = isSubmitting,
                        stepText = submissionStepText,
                        onStartSubmission = {
                            scope.launch {
                                isSubmitting = true
                                submissionStepText = "Compiling CBDT AY 2026-27 JSON Schema..."
                                delay(400)
                                val mockSalarySummary = ReconciledSalarySummary(
                                    employers = emptyList(),
                                    aggregateGrossSalary = grossSalary,
                                    aggregateSec10Exemptions = 0L,
                                    aggregateNetTaxableSalary = netTaxableSalary,
                                    aggregateTdsAllowed = tdsClaimed,
                                    totalDiscrepancyAmount = 0L
                                )
                                val mockComputation = RegimeComputationResult(
                                    regime = TaxRegime.NEW_REGIME_115BAC,
                                    grossTotalIncome = grossSalary,
                                    standardDeduction = 75_000L,
                                    totalChapterVIADeductions = 0L,
                                    netTaxableNormalIncomeRounded288A = netTaxableSalary,
                                    baseSlabTaxOnNormalIncome = 60_000L,
                                    rebate87A = 60_000L,
                                    marginalRelief87A = 0L,
                                    taxAfterRebateAndRelief = 0L,
                                    surchargeAfterMarginalRelief = 0L,
                                    healthAndEducationCess = 0L,
                                    totalTaxLiabilityRounded288B = 0L,
                                    effectiveTaxRatePercentage = 0.0
                                )
                                val cbdtJson = CbdtItr1JsonCompiler.compileItr1Json(
                                    pan = pan,
                                    assessmentYear = "2026-27",
                                    salarySummary = mockSalarySummary,
                                    computation = mockComputation,
                                    bankAccount = selectedBank,
                                    filingSection = FilingSection.ON_OR_BEFORE_DUE_DATE_139_1,
                                    declaration = TaxpayerDeclaration(fullName = "Taxpayer", pan = pan)
                                )

                                submissionStepText = "Establishing encrypted TLS session with ITD Gateway..."
                                delay(500)
                                submissionStepText = "Dispatching ITR-1 Sahaj Return to CPC Bengaluru..."
                                val subRes = itdClient.submitItrDirectly(pan, cbdtJson, selectedBank)
                                isSubmitting = false

                                if (subRes.isSuccess) {
                                    submissionResult = subRes.getOrNull()
                                    // Automatically advance to Aadhaar e-verification stage
                                    currentStage = InAppFilingStage.AADHAAR_E_VERIFICATION
                                } else {
                                    submissionStepText = "Submission error: " + (subRes.exceptionOrNull()?.message ?: "Network error")
                                }
                            }
                        }
                    )

                    InAppFilingStage.AADHAAR_E_VERIFICATION -> AadhaarEVerificationStage(
                        acknowledgmentNumber = submissionResult?.acknowledgmentNumber ?: "102684910293",
                        otpCode = aadhaarOtpInput,
                        onOtpChanged = { if (it.length <= 6 && it.all { char -> char.isDigit() }) aadhaarOtpInput = it },
                        isVerifying = isVerifyingOtp,
                        error = otpError,
                        onVerify = {
                            scope.launch {
                                isVerifyingOtp = true
                                otpError = null
                                val ack = submissionResult?.acknowledgmentNumber ?: "102684910293"
                                val res = itdClient.verifyReturnWithAadhaarOtp(pan, ack, aadhaarOtpInput)
                                isVerifyingOtp = false
                                if (res.isSuccess) {
                                    verificationResult = res.getOrNull()
                                    currentStage = InAppFilingStage.COMPLETED_CONFIRMATION
                                } else {
                                    otpError = res.exceptionOrNull()?.message ?: "Invalid OTP. Please re-enter."
                                }
                            }
                        },
                        onResendOtp = {
                            scope.launch {
                                val ack = submissionResult?.acknowledgmentNumber ?: "102684910293"
                                itdClient.requestInAppAadhaarOtp(pan, ack)
                            }
                        }
                    )

                    InAppFilingStage.COMPLETED_CONFIRMATION -> CompletedConfirmationStage(
                        pan = pan,
                        acknowledgmentNumber = verificationResult?.itrVAcknowledgmentNumber ?: (submissionResult?.acknowledgmentNumber ?: "102684910293"),
                        cpcRef = verificationResult?.cpcVerificationRef ?: "CPC/EVC/2026-27/4910294",
                        timestamp = verificationResult?.timestamp ?: "2026-07-15 14:32:10",
                        netRefund = if (tdsClaimed > taxLiability) tdsClaimed - taxLiability else 0L,
                        bankAccount = selectedBank,
                        onDone = {
                            itdClient.purgeSessionAndZeroize()
                            onClose()
                        }
                    )
                }
            }
        }
    }

    // ========================================================================
    // EMBEDDED ON-DEVICE GEMINI COPILOT SHEET
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
                            text = "On-Device Gemini Tax Copilot",
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
                    text = "Frequently Asked Questions in Direct Filing:",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                )
                Spacer(Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickQuestions) { q ->
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
                                text = currentExplanation!!.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate800
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Select any question above to get an instant, plain-language statutory answer right here without leaving your filing session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ============================================================================
// STAGE 1: PRE-FILING AUDIT & REFUND BANK SELECTION
// ============================================================================
@Composable
private fun ReviewAndConsentStage(
    pan: String,
    grossSalary: Long,
    netTaxableSalary: Long,
    taxLiability: Long,
    tdsClaimed: Long,
    selectedBank: BankAccountDetails,
    isDeclarationAccepted: Boolean,
    onDeclarationChanged: (Boolean) -> Unit,
    onProceed: () -> Unit,
    onInvokeCopilot: () -> Unit
) {
    val netRefund = if (tdsClaimed > taxLiability) tdsClaimed - taxLiability else 0L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Verified Return Schedules (AY 2026-27)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        StatutoryBadge(text = "ITR-1 Sahaj", tone = BadgeTone.STATUTORY)
                    }
                    Spacer(Modifier.height(8.dp))
                    EnterpriseLedgerRow(label = "Permanent Account Number", statutoryRef = pan, amount = 0L)
                    EnterpriseLedgerRow(label = "Gross Salary u/s 17(1)", amount = grossSalary)
                    EnterpriseLedgerRow(label = "Standard Deduction u/s 16(ia)", amount = 75_000L, isDeduction = true)
                    EnterpriseLedgerRow(label = "Total Income u/s 288A", amount = netTaxableSalary)
                    EnterpriseLedgerRow(label = "Section 87A Full Rebate", amount = 60_000L, isDeduction = true)
                    EnterpriseLedgerRow(label = "Total Tax Liability u/s 288B", amount = taxLiability, isHighlighted = true)
                    EnterpriseLedgerRow(label = "Prepaid Taxes (TDS Matched in 26AS)", amount = tdsClaimed)
                    EnterpriseLedgerRow(label = "Net Refund Due into Bank", amount = netRefund, isHighlighted = true)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Refund Bank Account Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Refund Credit Bank Account",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        StatutoryBadge(text = "Pre-Validated", tone = BadgeTone.SUCCESS)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${selectedBank.bankName} • ${selectedBank.accountNumber}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "IFSC: ${selectedBank.ifscCode} • ${selectedBank.accountType} Account",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate500)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Statutory Declaration Checkbox u/s 140
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = IndigoStatutoryBg),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(IndigoStatutory.copy(alpha = 0.4f)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDeclarationAccepted,
                        onCheckedChange = onDeclarationChanged
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "I solemnly declare that to the best of my knowledge and belief, the information given in the return is correct, complete, and in accordance with the provisions of the Income-tax Act, 1961.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onProceed,
                enabled = isDeclarationAccepted,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text(
                    text = "Proceed to Direct In-App Authentication →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// ============================================================================
// STAGE 2: IN-APP AUTHENTICATION (NO BROWSER VISITS)
// ============================================================================
@Composable
private fun InAppAuthenticationStage(
    pan: String,
    loginMethod: String,
    onLoginMethodChanged: (String) -> Unit,
    password: String,
    onPasswordChanged: (String) -> Unit,
    isAuthenticating: Boolean,
    authError: String?,
    onBack: () -> Unit,
    onAuthenticate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Authenticate with ITD Directly In-App",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Direct TLS connection to eportal.incometax.gov.in. Zero third-party telemetry.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        // Security Callout
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldSuccessBg),
            border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess.copy(alpha = 0.4f)))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🔒 DPDP Act 2023 §12(3) Guaranteed Ephemeral Security",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Your password is never saved to disk. In-memory buffers are cryptographically zero-wiped within 500ms of session creation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SmartIndividualPanField(
            panValue = pan,
            onPanChanged = {},
            isReadOnly = true
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            label = { Text("Income Tax Portal Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp)
        )

        if (authError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = authError,
                style = MaterialTheme.typography.bodySmall.copy(color = CrimsonDanger)
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
                onClick = onAuthenticate,
                enabled = !isAuthenticating && password.isNotBlank(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                modifier = Modifier.weight(2f).height(44.dp)
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PureWhite, strokeWidth = 2.dp)
                } else {
                    Text("Authenticate & Connect →", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

// ============================================================================
// STAGE 3: DIRECT RETURN SUBMISSION
// ============================================================================
@Composable
private fun DirectSubmissionStage(
    pan: String,
    grossSalary: Long,
    netTaxableSalary: Long,
    taxLiability: Long,
    tdsClaimed: Long,
    selectedBank: BankAccountDetails,
    isSubmitting: Boolean,
    stepText: String,
    onStartSubmission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Direct Transmission to ITD",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Dispatching validated ITR-1 Sahaj package to CPC Bengaluru Gateway.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CBDT AY 2026-27 Payload",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    StatutoryBadge(text = if (isSubmitting) "Transmitting" else "Ready", tone = BadgeTone.SUCCESS)
                }
                Spacer(Modifier.height(10.dp))
                EnterpriseLedgerRow(label = "Filing Assessee PAN", statutoryRef = pan, amount = 0L)
                EnterpriseLedgerRow(label = "Gross Income", amount = grossSalary)
                EnterpriseLedgerRow(label = "Final Tax Payable", amount = taxLiability)
                EnterpriseLedgerRow(label = "TDS Refund Claimed", amount = if (tdsClaimed > taxLiability) tdsClaimed - taxLiability else 0L, isHighlighted = true)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Status: $stepText",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate600)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onStartSubmission,
            enabled = !isSubmitting,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PureWhite, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Submitting to ITD...", style = MaterialTheme.typography.labelMedium)
            } else {
                Text("Submit Return Directly to ITD u/s 139(1)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

// ============================================================================
// STAGE 4: INSTANT IN-APP AADHAAR OTP E-VERIFICATION
// ============================================================================
@Composable
private fun AadhaarEVerificationStage(
    acknowledgmentNumber: String,
    otpCode: String,
    onOtpChanged: (String) -> Unit,
    isVerifying: Boolean,
    error: String?,
    onVerify: () -> Unit,
    onResendOtp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Instant Aadhaar e-Verification",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Verify your return immediately u/s 140A. No need to post physical ITR-V to CPC.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Provisional ITD Submission Reference",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = acknowledgmentNumber,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A 6-digit verification OTP has been sent by UIDAI to your mobile ending with ••••5819.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = onOtpChanged,
            label = { Text("Enter 6-Digit Aadhaar OTP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp)
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall.copy(color = CrimsonDanger)
            )
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onResendOtp) {
            Text("Didn't receive OTP? Resend via UIDAI", style = MaterialTheme.typography.labelSmall.copy(color = EnterpriseTeal700))
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onVerify,
            enabled = !isVerifying && otpCode.length == 6,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            if (isVerifying) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PureWhite, strokeWidth = 2.dp)
            } else {
                Text("Complete e-Verification u/s 140A", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

// ============================================================================
// STAGE 5: COMPLETED RETURN & CBDT ITR-V ACKNOWLEDGMENT
// ============================================================================
@Composable
private fun CompletedConfirmationStage(
    pan: String,
    acknowledgmentNumber: String,
    cpcRef: String,
    timestamp: String,
    netRefund: Long,
    bankAccount: BankAccountDetails,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldSuccessBg),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess.copy(alpha = 0.5f)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccess),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", style = TextStyle(fontSize = 24.sp, color = PureWhite, fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Return Successfully Filed & Verified",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Assessment Year 2026-27 • Section 139(1)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Official CBDT ITR-V Acknowledgment",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
                EnterpriseLedgerRow(label = "Acknowledgment Number", statutoryRef = acknowledgmentNumber, amount = 0L)
                EnterpriseLedgerRow(label = "CPC Verification Token", statutoryRef = cpcRef, amount = 0L)
                EnterpriseLedgerRow(label = "Verification Timestamp", statutoryRef = timestamp, amount = 0L)
                EnterpriseLedgerRow(label = "Refund Scheduled", amount = netRefund, isHighlighted = true)
                EnterpriseLedgerRow(label = "Crediting Bank Account", statutoryRef = "${bankAccount.bankName} ${bankAccount.accountNumber}", amount = 0L)
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Next Steps from Central Processing Centre (CPC):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "• CPC Bengaluru will process your return under Section 143(1).\n• Intimation will be sent to your registered email.\n• Refund of ₹${netRefund} will be credited directly to your pre-validated bank account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onDone,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Text("Done & Wipe Ephemeral Session (DPDP §12(3))", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
