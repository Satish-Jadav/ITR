package in.zerotax.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import in.zerotax.app.ui.navigation.OnboardingTap
import in.zerotax.app.ui.theme.*
import in.zerotax.core.model.Rupee

enum class BadgeTone {
    SUCCESS,
    WARNING,
    DANGER,
    NEUTRAL,
    STATUTORY
}

@Composable
fun StatutoryBadge(
    text: String,
    tone: BadgeTone = BadgeTone.NEUTRAL,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (tone) {
        BadgeTone.SUCCESS -> Triple(EmeraldSuccessBg, EmeraldSuccess, EmeraldSuccess.copy(alpha = 0.3f))
        BadgeTone.WARNING -> Triple(AmberWarningBg, AmberWarning, AmberWarning.copy(alpha = 0.3f))
        BadgeTone.DANGER  -> Triple(CrimsonDangerBg, CrimsonDanger, CrimsonDanger.copy(alpha = 0.3f))
        BadgeTone.STATUTORY -> Triple(IndigoStatutoryBg, IndigoStatutory, IndigoStatutory.copy(alpha = 0.3f))
        BadgeTone.NEUTRAL -> Triple(Slate100, Slate700, Slate300)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .border(0.75.dp, borderColor, RoundedCornerShape(3.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.3.sp
            )
        )
    }
}

@Composable
fun EnterpriseTopHeader(
    pan: String,
    assessmentYear: String = "2026-27",
    isVaultEncrypted: Boolean = true,
    onOpenChatbot: (() -> Unit)? = null,
    onOpenToolkit: (() -> Unit)? = null,
    onOpenAnalytics: (() -> Unit)? = null,
    onOpenNotices: (() -> Unit)? = null,
    onOpenBusinessAudit: (() -> Unit)? = null,
    onOpenNriSuite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Security dot
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isVaultEncrypted) EmeraldSuccess else AmberWarning)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ZeroTax Local",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatutoryBadge(text = "AY $assessmentYear", tone = BadgeTone.STATUTORY)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenChatbot != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EnterpriseTeal50)
                                .border(0.5.dp, EnterpriseTeal600, RoundedCornerShape(4.dp))
                                .clickable { onOpenChatbot() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🤖", fontSize = 11.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Chat & File", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal900))
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    if (onOpenToolkit != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(IndigoStatutoryBg)
                                .border(0.5.dp, IndigoStatutory, RoundedCornerShape(4.dp))
                                .clickable { onOpenToolkit() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🛠️", fontSize = 11.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Tools", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoStatutory))
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    if (onOpenAnalytics != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EmeraldSuccessBg)
                                .border(0.5.dp, EmeraldSuccess, RoundedCornerShape(4.dp))
                                .clickable { onOpenAnalytics() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📊", fontSize = 11.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Insights", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    if (onOpenNotices != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AmberWarningBg)
                                .border(0.5.dp, AmberWarning, RoundedCornerShape(4.dp))
                                .clickable { onOpenNotices() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🛡️", fontSize = 11.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Notices", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning))
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    if (onOpenBusinessAudit != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(IndigoStatutoryBg)
                                .border(0.5.dp, IndigoStatutory, RoundedCornerShape(4.dp))
                                .clickable { onOpenBusinessAudit() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🏢", fontSize = 11.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("Business Audit", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoStatutory))
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    if (onOpenNriSuite != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EnterpriseTeal50)
                                .border(0.5.dp, EnterpriseTeal600, RoundedCornerShape(4.dp))
                                .clickable { onOpenNriSuite() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌍", fontSize = 11.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("NRI Suite", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal900))
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    if (pan.isNotBlank()) {
                        Text(
                            text = pan,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    StatutoryBadge(
                        text = "SQLCipher StrongBox",
                        tone = BadgeTone.SUCCESS
                    )
                }
            }
        }
    }
}

@Composable
fun DiscreteStatutoryStepper(
    currentTap: OnboardingTap,
    onStepClick: (OnboardingTap) -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = OnboardingTap.entries

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            steps.forEachIndexed { index, tap ->
                val isCurrent = tap == currentTap
                val isCompleted = tap.ordinal < currentTap.ordinal
                val targetBarColor = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isCompleted -> EmeraldSuccess
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
                val animatedBarColor by animateColorAsState(
                    targetValue = targetBarColor,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    label = "StepperBarColor_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(animatedBarColor)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STAGE ${currentTap.ordinal + 1} OF 4",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = currentTap.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun SmartIndividualPanField(
    panValue: String,
    onPanChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    val dynamicKeyboardType = if (panValue.length in 5..8) KeyboardType.NumberPassword else KeyboardType.Ascii
    val isFourthCharInvalid = panValue.length >= 4 && panValue[3] != 'P'
    val isComplete = panValue.length == 10

    OutlinedTextField(
        value = panValue,
        onValueChange = { raw ->
            if (!isReadOnly) {
                val up = raw.uppercase().take(10)
                val valid = up.indices.all { i -> if (i in 5..8) up[i].isDigit() else up[i].isLetter() }
                if (valid) onPanChanged(up)
            }
        },
        readOnly = isReadOnly,
        label = { Text("Permanent Account Number (PAN)", style = MaterialTheme.typography.bodyMedium) },
        placeholder = { Text("ABCDE1234F", style = TextStyle(fontFamily = FontFamily.Monospace, color = Slate400)) },
        supportingText = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isFourthCharInvalid) {
                    Text(
                        "4th character must be 'P' for Individual Taxpayer",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        "Alphanumeric structure: 5 letters, 4 digits, 1 letter",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }
                Text(
                    "${panValue.length}/10",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate500)
                )
            }
        },
        isError = isFourthCharInvalid,
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = dynamicKeyboardType,
            imeAction = ImeAction.Done
        ),
        shape = RoundedCornerShape(4.dp),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun EnterpriseLedgerRow(
    label: String,
    statutoryRef: String? = null,
    amount: Long,
    isDeduction: Boolean = false,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = if (isHighlighted) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        else MaterialTheme.typography.bodyMedium
            )
            if (statutoryRef != null) {
                Spacer(Modifier.width(6.dp))
                StatutoryBadge(text = statutoryRef, tone = BadgeTone.NEUTRAL)
            }
        }
        Text(
            text = (if (isDeduction && amount > 0) "- " else "") + Rupee(amount).toString(),
            style = if (isHighlighted) MonospaceFigureLargeStyle.copy(fontSize = 15.sp)
                    else MonospaceFigureStyle.copy(fontSize = 13.sp),
            color = if (isHighlighted) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun Section270AStatutoryOverrideDialog(
    fieldName: String,
    verifiedSystemValueRupees: Long,
    userProposedValueRupees: Long,
    onConfirmOverrideWithAuditLog: (statutoryReasonCode: String) -> Unit,
    onCancelAndRestoreVerifiedValue: () -> Unit
) {
    var declarationChecked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelAndRestoreVerifiedValue,
        shape = RoundedCornerShape(6.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CrimsonDanger)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Statutory Override Notice (Sec 270A)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CrimsonDanger
                )
            }
        },
        text = {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "You are reducing $fieldName from the authoritative verified figure of ${Rupee(verifiedSystemValueRupees)} down to ${Rupee(userProposedValueRupees)}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonDangerBg),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(CrimsonDanger.copy(alpha = 0.4f)))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Statutory Penalties under Income-tax Act, 1961:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CrimsonDanger
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• Section 270A(1): 50% penalty on tax payable on under-reported income.\n• Section 270A(8): 200% penalty for misreporting of income.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonDanger
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = declarationChecked,
                        onCheckedChange = { declarationChecked = it },
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "I possess verified documentary evidence justifying this difference and take legal liability under Section 140.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = declarationChecked,
                onClick = { onConfirmOverrideWithAuditLog("USER_DOCUMENTED_OVERRIDE_SEC_140") },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
            ) {
                Text("Log Reason & Override", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancelAndRestoreVerifiedValue,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Retain Verified", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

@Composable
fun RegimeComparisonInstitutionalCard(
    newRegimeTax: Long,
    oldRegimeTax: Long,
    newNormalGti: Long,
    oldNormalGti: Long,
    newDeductions: Long,
    oldDeductions: Long,
    newRebate87A: Long,
    oldRebate87A: Long,
    modifier: Modifier = Modifier
) {
    val taxDelta = oldRegimeTax - newRegimeTax
    val isNewBetter = taxDelta >= 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
        shape = RoundedCornerShape(6.dp),
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
                    text = "Statutory Regime Comparison (AY 2026-27)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                StatutoryBadge(
                    text = if (isNewBetter) "New Regime Optimal" else "Old Regime Optimal",
                    tone = BadgeTone.SUCCESS
                )
            }

            Spacer(Modifier.height(12.dp))

            // Two-column side by side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NEW REGIME (115BAC)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = newRegimeTax,
                        transitionSpec = {
                            (slideInVertically(tween(300)) { it / 2 } + fadeIn(tween(250)))
                                .togetherWith(slideOutVertically(tween(200)) { -it / 2 } + fadeOut(tween(150)))
                        },
                        label = "NewRegimeTaxAnimated"
                    ) { tax ->
                        Text(
                            text = Rupee(tax).toString(),
                            style = MonospaceFigureLargeStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (newRebate87A > 0) "Includes Sec 87A: -${Rupee(newRebate87A)}" else "Std Ded: ₹75,000",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "OLD REGIME",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate600
                    )
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = oldRegimeTax,
                        transitionSpec = {
                            (slideInVertically(tween(300)) { it / 2 } + fadeIn(tween(250)))
                                .togetherWith(slideOutVertically(tween(200)) { -it / 2 } + fadeOut(tween(150)))
                        },
                        label = "OldRegimeTaxAnimated"
                    ) { tax ->
                        Text(
                            text = Rupee(tax).toString(),
                            style = MonospaceFigureLargeStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Ch VI-A: ${Rupee(oldDeductions)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Net Benefit Callout
            val savingsText = if (isNewBetter) {
                "Selecting New Regime saves ${Rupee(taxDelta)} under AY 2026-27 ₹12 Lakh threshold"
            } else {
                "Selecting Old Regime saves ${Rupee(-taxDelta)} via itemized Chapter VI-A deductions"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isNewBetter) EmeraldSuccessBg else IndigoStatutoryBg)
                    .border(
                        0.5.dp,
                        if (isNewBetter) EmeraldSuccess.copy(alpha = 0.4f) else IndigoStatutory.copy(alpha = 0.4f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = savingsText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isNewBetter) EmeraldSuccess else IndigoStatutory
                    )
                )
            }
        }
    }
}
