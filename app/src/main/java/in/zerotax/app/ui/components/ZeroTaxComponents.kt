package `in`.zerotax.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.navigation.OnboardingTap
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.CitizenCategory
import `in`.zerotax.core.model.PanEntityDecoder
import `in`.zerotax.core.model.PanEntityType
import `in`.zerotax.core.model.Rupee

/**
 * Sovereign Brand Logo Component for ZeroTax.
 * Combines the Indian Rupee symbol (₹), the Sovereign Privacy Shield,
 * and the Emerald Verification Zenith in minimalist geometry.
 */
@Composable
fun ZeroTaxBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    showText: Boolean = false,
    subtitle: String? = "Private Indian Tax Vault"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Slate950),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size * 0.84f)) {
                val w = this.size.width
                val h = this.size.height

                // Shield Path
                val shieldPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.88f, h * 0.22f)
                    cubicTo(w * 0.88f, h * 0.58f, w * 0.72f, h * 0.82f, w * 0.5f, h * 0.94f)
                    cubicTo(w * 0.28f, h * 0.82f, w * 0.12f, h * 0.58f, w * 0.12f, h * 0.22f)
                    close()
                }
                drawPath(path = shieldPath, color = EnterpriseTeal900)
                drawPath(
                    path = shieldPath,
                    color = EnterpriseTeal500,
                    style = Stroke(width = w * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val strokeW = w * 0.065f

                // Rupee Top Horizontal Bar
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.32f, h * 0.30f),
                    end = Offset(w * 0.68f, h * 0.30f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )

                // Rupee Second Horizontal Bar
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.32f, h * 0.44f),
                    end = Offset(w * 0.62f, h * 0.44f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )

                // Rupee Vertical Stem & Loop
                val loopPath = Path().apply {
                    moveTo(w * 0.42f, h * 0.30f)
                    lineTo(w * 0.42f, h * 0.58f)
                    cubicTo(w * 0.66f, h * 0.58f, w * 0.66f, h * 0.36f, w * 0.42f, h * 0.36f)
                }
                drawPath(
                    path = loopPath,
                    color = Color.White,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Emerald Verification Leg / Dynamic Checkmark
                drawLine(
                    color = EmeraldSuccess,
                    start = Offset(w * 0.42f, h * 0.58f),
                    end = Offset(w * 0.68f, h * 0.78f),
                    strokeWidth = strokeW * 1.15f,
                    cap = StrokeCap.Round
                )
            }
        }

        if (showText) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Zero",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "Tax",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EnterpriseTeal600,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate500,
                            fontSize = 10.sp,
                            letterSpacing = 0.3.sp
                        )
                    )
                }
            }
        }
    }
}

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
    userProfile: `in`.zerotax.core.model.UserAuthProfile? = null,
    onSignOut: (() -> Unit)? = null,
    onOpenChatbot: (() -> Unit)? = null,
    onOpenToolkit: (() -> Unit)? = null,
    onOpenAnalytics: (() -> Unit)? = null,
    onOpenNotices: (() -> Unit)? = null,
    onOpenBusinessAudit: (() -> Unit)? = null,
    onOpenNriSuite: (() -> Unit)? = null,
    onOpenCorporateSuite: (() -> Unit)? = null,
    onLockVault: (() -> Unit)? = null,
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
                    ZeroTaxBrandLogo(size = 28.dp, showText = false)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ZeroTax",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.width(6.dp))
                    // Security dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isVaultEncrypted) EmeraldSuccess else AmberWarning)
                    )
                    Spacer(Modifier.width(6.dp))
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
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = EnterpriseTeal900,
                                    modifier = Modifier.size(13.dp)
                                )
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
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = IndigoStatutory,
                                    modifier = Modifier.size(13.dp)
                                )
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
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(13.dp)
                                )
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
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(13.dp)
                                )
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
                                Icon(
                                    imageVector = Icons.Default.Domain,
                                    contentDescription = null,
                                    tint = IndigoStatutory,
                                    modifier = Modifier.size(13.dp)
                                )
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
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = EnterpriseTeal900,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("NRI Suite", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal900))
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    if (onOpenCorporateSuite != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EnterpriseTeal50)
                                .border(0.5.dp, EnterpriseTeal600, RoundedCornerShape(4.dp))
                                .clickable { onOpenCorporateSuite() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Apartment,
                                    contentDescription = null,
                                    tint = EnterpriseTeal900,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("ITR-5/6/7", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal900))
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

                    if (onLockVault != null) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Slate100)
                                .border(0.5.dp, Slate300, RoundedCornerShape(4.dp))
                                .clickable { onLockVault() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Slate700,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("Lock", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700))
                            }
                        }
                    }

                    if (userProfile != null) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                                .clickable { onSignOut?.invoke() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = userProfile.displayName.take(10),
                                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Sign Out",
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
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
    val decoded = remember(panValue) { PanEntityDecoder.decode(panValue) }
    val dynamicKeyboardType = if (panValue.length in 5..8) KeyboardType.NumberPassword else KeyboardType.Ascii
    val isComplete = panValue.length == 10
    val isNonPersonal = panValue.length >= 4 && !decoded.entityType.isSupportedInPersonalSuite

    Column(modifier = modifier.fillMaxWidth()) {
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
            trailingIcon = {
                if (isComplete && decoded.isValidFormat) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.padding(end = 8.dp).size(20.dp)
                    )
                }
            },
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isNonPersonal) {
                        Text(
                            "${decoded.entityType.title} detected (Personal suite is for 'P' or 'H')",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (panValue.length >= 5) {
                        Text(
                            "Entity: ${decoded.entityType.title} • Surname Initial: '${decoded.surnameInitial}'",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
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
            isError = isNonPersonal,
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
            modifier = Modifier.fillMaxWidth()
        )

        // Real-time Entity & Eligibility Status Badges
        if (panValue.length >= 4) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (decoded.entityType) {
                    PanEntityType.INDIVIDUAL -> {
                        StatutoryBadge(text = "Individual Taxpayer", tone = BadgeTone.SUCCESS)
                        StatutoryBadge(text = "ITR-1 / ITR-2 / ITR-4 Eligible", tone = BadgeTone.STATUTORY)
                        if (isComplete) {
                            StatutoryBadge(text = "Sec 87A Rebate Eligible", tone = BadgeTone.SUCCESS)
                        }
                    }
                    PanEntityType.HUF -> {
                        StatutoryBadge(text = "HUF Entity", tone = BadgeTone.NEUTRAL)
                        StatutoryBadge(text = "ITR-2 / ITR-3 Eligible", tone = BadgeTone.STATUTORY)
                    }
                    PanEntityType.COMPANY -> {
                        StatutoryBadge(text = "Company Entity", tone = BadgeTone.STATUTORY)
                        StatutoryBadge(text = "ITR-6 / ITR-7 Eligible", tone = BadgeTone.STATUTORY)
                    }
                    PanEntityType.FIRM -> {
                        StatutoryBadge(text = "Partnership / LLP", tone = BadgeTone.STATUTORY)
                        StatutoryBadge(text = "ITR-5 / ITR-4 Eligible", tone = BadgeTone.STATUTORY)
                    }
                    PanEntityType.TRUST -> {
                        StatutoryBadge(text = "Trust / NGO", tone = BadgeTone.STATUTORY)
                        StatutoryBadge(text = "ITR-7 Eligible", tone = BadgeTone.STATUTORY)
                    }
                    PanEntityType.AOP, PanEntityType.BOI, PanEntityType.LOCAL_AUTHORITY, PanEntityType.ARTIFICIAL_JURIDICAL -> {
                        StatutoryBadge(text = decoded.entityType.title, tone = BadgeTone.STATUTORY)
                        StatutoryBadge(text = "ITR-5 Eligible", tone = BadgeTone.STATUTORY)
                    }
                    else -> {
                        StatutoryBadge(text = decoded.entityType.title, tone = BadgeTone.WARNING)
                    }
                }
            }
        }
    }
}

/**
 * Visual selector for Taxpayer Age Bracket and Citizen Category,
 * dynamically updating the statutory basic exemption limits (₹2.5L / ₹3.0L / ₹5.0L) under Old Regime
 * and unlocking Section 80TTB interest deductions for Senior Citizens.
 */
@Composable
fun CitizenCategoryAgeSelector(
    selectedCategory: CitizenCategory,
    onCategoryChanged: (CitizenCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Age Category & Basic Exemption",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                val exemptionText = when (selectedCategory) {
                    CitizenCategory.REGULAR_BELOW_60 -> "₹2.5L Exemption (Old Regime)"
                    CitizenCategory.SENIOR_60_TO_79 -> "₹3.0L Exemption (Old Regime)"
                    CitizenCategory.SUPER_SENIOR_80_PLUS -> "₹5.0L Exemption (Old Regime)"
                }
                StatutoryBadge(
                    text = exemptionText,
                    tone = if (selectedCategory == CitizenCategory.REGULAR_BELOW_60) BadgeTone.NEUTRAL else BadgeTone.SUCCESS
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(
                    Triple(CitizenCategory.REGULAR_BELOW_60, "< 60 Yrs", "Normal"),
                    Triple(CitizenCategory.SENIOR_60_TO_79, "60-79 Yrs", "Senior"),
                    Triple(CitizenCategory.SUPER_SENIOR_80_PLUS, "80+ Yrs", "Super Senior")
                )
                options.forEach { (cat, ageLabel, roleLabel) ->
                    val isSelected = selectedCategory == cat
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCategoryChanged(cat) },
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) {
                            CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                        } else CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = roleLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Slate600
                                )
                            )
                            Text(
                                text = ageLabel,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
            if (selectedCategory != CitizenCategory.REGULAR_BELOW_60) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Enhanced Section 80D limit (₹50,000) & Section 80TTB interest deduction (up to ₹50,000) auto-applied.",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSuccess
                    )
                }
            }
        }
    }
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

// ============================================================================
// STANDARDIZED ZERO TAX ENTERPRISE BRAND COMPONENTS
// ============================================================================

/**
 * Standardized Top App Bar for all secondary and tool screens.
 * Embodies the Sovereign Shield logo, high-contrast titles, and PAN identity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeroTaxTopAppBar(
    title: String,
    subtitle: String? = null,
    pan: String? = null,
    onBack: (() -> Unit)? = null,
    badgeText: String? = null,
    badgeTone: BadgeTone = BadgeTone.STATUTORY,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ZeroTaxBrandLogo(size = 28.dp, showText = false)
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                        if (badgeText != null) {
                            Spacer(Modifier.width(8.dp))
                            StatutoryBadge(text = badgeText, tone = badgeTone)
                        }
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Slate800))
                }
            }
        },
        actions = {
            if (!pan.isNullOrBlank()) {
                StatutoryBadge(text = "PAN $pan", tone = BadgeTone.STATUTORY)
                Spacer(Modifier.width(8.dp))
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Standardized Primary Button adhering strictly to the Enterprise Teal 600 palette.
 */
@Composable
fun ZeroTaxPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EnterpriseTeal600,
            contentColor = PureWhite,
            disabledContainerColor = Slate300,
            disabledContentColor = Slate500
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 2.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            trailingIcon()
        }
    }
}

/**
 * Standardized Outlined Button with subtle Enterprise Teal borders and hover states.
 */
@Composable
fun ZeroTaxOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) EnterpriseTeal600 else Slate300),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = EnterpriseTeal600,
            disabledContentColor = Slate400
        )
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

/**
 * Standardized Sovereign Slate Card with unified radius and elevated borders.
 */
@Composable
fun ZeroTaxCard(
    modifier: Modifier = Modifier,
    accentBorderColor: Color? = null,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accentBorderColor ?: MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Standardized Financial Stat Card for displaying tax figures, refunds, or deductions.
 */
@Composable
fun ZeroTaxStatCard(
    title: String,
    amountRupees: Long,
    subtitle: String? = null,
    tone: BadgeTone = BadgeTone.NEUTRAL,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    ZeroTaxCard(
        modifier = modifier,
        accentBorderColor = when (tone) {
            BadgeTone.SUCCESS -> EmeraldSuccess.copy(alpha = 0.35f)
            BadgeTone.WARNING -> AmberWarning.copy(alpha = 0.35f)
            BadgeTone.DANGER -> CrimsonDanger.copy(alpha = 0.35f)
            BadgeTone.STATUTORY -> IndigoStatutory.copy(alpha = 0.35f)
            BadgeTone.NEUTRAL -> null
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(color = Slate600, fontWeight = FontWeight.Medium)
            )
            if (badgeText != null) {
                StatutoryBadge(text = badgeText, tone = tone)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "₹${Rupee.formatInr(amountRupees)}",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = when (tone) {
                    BadgeTone.SUCCESS -> EmeraldSuccess
                    BadgeTone.DANGER -> CrimsonDanger
                    BadgeTone.WARNING -> AmberWarning
                    BadgeTone.STATUTORY -> EnterpriseTeal700
                    BadgeTone.NEUTRAL -> Slate900
                }
            )
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
            )
        }
    }
}

/**
 * Standardized Section Header with Brand Indicator Pill.
 */
@Composable
fun ZeroTaxSectionHeader(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeTone: BadgeTone = BadgeTone.STATUTORY,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(EnterpriseTeal600)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
            }
            if (badgeText != null) {
                StatutoryBadge(text = badgeText, tone = badgeTone)
            }
        }
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 12.sp),
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
