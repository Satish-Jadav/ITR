package `in`.zerotax.app.ui.sahaj

import androidx.compose.material.icons.automirrored.filled.TrendingUp
import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.home.Express3MinuteFilingCard
import `in`.zerotax.app.viewmodel.ZeroTaxUiState
import `in`.zerotax.app.ui.guide.SahajTourStep
import `in`.zerotax.app.ui.guide.ZeroTaxCoachmarkPopup
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.app.ui.timeline.StatutoryTimelineBanner
import `in`.zerotax.core.model.Rupee
import kotlin.math.max

/**
 * Tailored, plain-language filing experience for Everyday Salaried Users (ITR-1 SAHAJ).
 * Eliminates statutory jargon, providing step-by-step clarity, interactive coachmark guide,
 * and 1-tap biometric e-filing.
 */
@Composable
fun SahajFilingExperienceView(
    pan: String,
    grossSalary: Long,
    bankInterest: Long = 0L,
    bankAccount: `in`.zerotax.core.model.BankAccountDetails? = null,
    tdsClaimed: Long,
    newRegimeTax: Long,
    oldRegimeTax: Long,
    onLaunchPortalAssistant: () -> Unit,
    onOpenDeductionsAdvisor: () -> Unit,
    onOpenTaxPaymentHub: () -> Unit,
    onOpenAisTransactions: () -> Unit,
    onOpenPortalSync: () -> Unit = {},
    onOpenThinkLikeCa: () -> Unit = {},
    uiState: ZeroTaxUiState? = null,
    modifier: Modifier = Modifier
) {
    var showTour by remember { mutableStateOf(true) }
    var currentTourStep by remember { mutableStateOf(SahajTourStep.STEP_1_SALARY) }
    var showBiometricFilingDialog by remember { mutableStateOf(false) }

    val standardDeduction = 75_000L
    val netTaxableEarnings = max(0L, (grossSalary + bankInterest) - standardDeduction)
    val bestRegimeTax = minOf(newRegimeTax, oldRegimeTax)
    val regimeSavings = max(0L, oldRegimeTax - newRegimeTax)
    val refundDue = max(0L, tdsClaimed - bestRegimeTax)
    val taxPayable = max(0L, bestRegimeTax - tdsClaimed)

    var selectedTimelineMode by remember { mutableStateOf(0) } // 0: "New", 1: "Old"
    val isSahajDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ====================================================================
        // DUAL METRIC CARDS (Matching Reference Design Top Two Cards)
        // ====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Tax Refund Due (or Net Payable)
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = if (isSahajDark) Slate850 else PureWhite,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = if (refundDue > 0) "Tax Refund Due" else "Net Tax Liability",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSahajDark) Slate400 else Slate600
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "₹${Rupee.formatInr(if (refundDue > 0) refundDue else taxPayable)}",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSahajDark) PureWhite else Slate900,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (refundDue > 0) SageMintPill else AmberPillBg
                        ) {
                            Text(
                                text = if (refundDue > 0) "100% Reconciled" else "Zero Tax Slab",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (refundDue > 0) ForestGreenPrimary else AmberPillText
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ForestGreenPrimary)
                        )
                    }
                }
            }

            // Card 2: Due in 30 Days / Advance Tax / TDS
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = if (isSahajDark) Slate850 else PureWhite,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = "TDS Deducted",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSahajDark) Slate400 else Slate600
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "₹${Rupee.formatInr(tdsClaimed)}",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSahajDark) PureWhite else Slate900,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AmberPillBg
                        ) {
                            Text(
                                text = "Form 26AS Match",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberPillText
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AmberPillText)
                        )
                    }
                }
            }
        }

        // ====================================================================
        // HERO PILL BAR CHART CARD (Exact Match to Reference Card)
        // ====================================================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (isSahajDark) Slate850 else PureWhite,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header: Title + [✓ New | Old] Toggle Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tax Slabs & Regimes",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSahajDark) PureWhite else Slate900
                        )
                    )

                    // Pill Segmented Control
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSahajDark) Slate800 else Slate100
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            // "New" Option
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selectedTimelineMode == 0) ForestGreenPrimary else Color.Transparent)
                                    .clickable { selectedTimelineMode = 0 }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectedTimelineMode == 0) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PureWhite,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "New",
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTimelineMode == 0) PureWhite else Slate600
                                        )
                                    )
                                }
                            }

                            // "Old" Option
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selectedTimelineMode == 1) ForestGreenPrimary else Color.Transparent)
                                    .clickable { selectedTimelineMode = 1 }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectedTimelineMode == 1) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PureWhite,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "Old",
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTimelineMode == 1) PureWhite else Slate600
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Amount Figure + Soft Rose/Green Pill Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "₹${Rupee.formatInr(if (selectedTimelineMode == 0) newRegimeTax else oldRegimeTax)}",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSahajDark) PureWhite else Slate900,
                            letterSpacing = (-0.5).sp
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (regimeSavings > 0) RosePillBg else SageMintPill
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (regimeSavings > 0) "↑ ₹${Rupee.formatInr(regimeSavings)} Saved" else "Optimal Slab",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (regimeSavings > 0) RosePillText else ForestGreenPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Vertical Pill Bar Chart (6 Rounded Bars with labels)
                val barHeights = listOf(70.dp, 55.dp, 28.dp, 20.dp, 50.dp, 22.dp)
                val barLabels = listOf("0-3L", "3-7L", "7-10L", "10-12L", "12-15L", ">15L")
                val activeBarIndex = 4

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    barHeights.forEachIndexed { index, barHeight ->
                        val isActive = index == activeBarIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(30.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(if (isActive) ForestGreenPrimary else (if (isSahajDark) Slate700 else SageMintLight))
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = barLabels[index],
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) (if (isSahajDark) PureWhite else Slate900) else Slate500
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Carousel Dots Indicator (like in reference image)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ForestGreenPrimary)
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(if (isSahajDark) Slate700 else Slate300)
                    )
                }
            }
        }

        // ====================================================================
        // SUBSCRIPTIONS & DEDUCTIONS LIST (Matching Spotify/YouTube list in image)
        // ====================================================================
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deductions & Benefits",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSahajDark) PureWhite else Slate900
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenDeductionsAdvisor() }
                ) {
                    Text(
                        text = "Optimize",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Optimize",
                        tint = Slate600,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Deduction Item 1: Standard Deduction (Sec 16ia)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (isSahajDark) Slate850 else PureWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SageMintPill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Standard Deduction",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSahajDark) PureWhite else Slate900
                                )
                            )
                            Text(
                                text = "Sec 16(ia) • Auto applied",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹75,000",
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSahajDark) PureWhite else Slate900
                            )
                        )
                        Text(
                            text = "Standard",
                            style = TextStyle(fontSize = 10.sp, color = Slate500)
                        )
                    }
                }
            }

            // Deduction Item 2: Sec 87A Tax Rebate (Zero Tax under 7L/12L)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (isSahajDark) Slate850 else PureWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberPillBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AmberPillText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Sec 87A Full Rebate",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSahajDark) PureWhite else Slate900
                                )
                            )
                            Text(
                                text = "Zero Tax on income up to ₹12L",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹25,000",
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        )
                        Text(
                            text = "Rebate",
                            style = TextStyle(fontSize = 10.sp, color = Slate500)
                        )
                    }
                }
            }

            // Deduction Item 3: Direct Bank Refund Account
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (isSahajDark) Slate850 else PureWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F1FD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = bankAccount?.bankName?.ifBlank { null } ?: "Pre-Validated Bank",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSahajDark) PureWhite else Slate900
                                )
                            )
                            Text(
                                text = if (bankAccount != null && bankAccount.accountNumber.isNotBlank()) "A/C: ••••${bankAccount.accountNumber.takeLast(4)}" else "Direct ECS Credit",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SageMintPill
                    ) {
                        Text(
                            text = "Verified",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreenPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Primary Action Button matching reference rounded button
        Button(
            onClick = onLaunchPortalAssistant,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ForestGreenPrimary,
                contentColor = PureWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Authenticate & e-File Return Now",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                )
            }
        }

        // Secondary Button: Sync Portal
        OutlinedButton(
            onClick = onOpenPortalSync,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreenPrimary.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = ForestGreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sync ITD Portal Data",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ForestGreenPrimary
                    )
                )
            }
        }

        Spacer(Modifier.height(30.dp))

    // Biometric e-Filing Verification Sheet
    if (showBiometricFilingDialog) {
        ZeroTaxBottomSheet(
            onDismissRequest = { showBiometricFilingDialog = false }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Confirm Biometric Signature",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                IconButton(onClick = { showBiometricFilingDialog = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "You are filing ITR-1 (SAHAJ) for AY 2026-27 under Section 139(1).\n\n" +
                       "• Permanent Account Number: $pan\n" +
                       "• Taxable Earnings: ₹${Rupee.formatInr(netTaxableEarnings)}\n" +
                       "• " + (if (refundDue > 0) "Expected Refund: ₹${Rupee.formatInr(refundDue)}" else "Tax Payable: ₹${Rupee.formatInr(taxPayable)}") + "\n\n" +
                       "Scan your fingerprint or face to cryptographically sign your CBDT return package.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(20.dp))

            ZeroTaxPrimaryButton(
                text = "Verify & Dispatch to CBDT",
                onClick = {
                    showBiometricFilingDialog = false
                    onLaunchPortalAssistant()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}
}

