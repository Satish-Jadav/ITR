package `in`.zerotax.app.ui.sahaj

import androidx.compose.material.icons.automirrored.filled.TrendingUp
import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.*
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Statutory Timeline Banner with July 31 Countdown
        StatutoryTimelineBanner()

        // 1-TAP IN-APP ITD PORTAL SYNC CARD
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = getTealHighlightBg(),
            border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal500),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPortalSync() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = getTealHighlightIcon(),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Sync Directly from Income Tax Portal",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = getTealHighlightText())
                        )
                        Text(
                            "Pull Form 26AS, AIS/TIS & prefill in-app. Never leave ZeroTax!",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        )
                    }
                }
                StatutoryBadge(text = "1-Tap Sync →", tone = BadgeTone.SUCCESS)
            }
        }

        // Guided Tour Mini-Popup Card ("Do this then click this")
        AnimatedVisibility(
            visible = showTour,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ZeroTaxCoachmarkPopup(
                currentStep = currentTourStep,
                onNext = {
                    val nextOrdinal = currentTourStep.ordinal + 1
                    if (nextOrdinal < SahajTourStep.entries.size) {
                        currentTourStep = SahajTourStep.entries[nextOrdinal]
                    } else {
                        showTour = false
                    }
                },
                onPrevious = {
                    val prevOrdinal = currentTourStep.ordinal - 1
                    if (prevOrdinal >= 0) {
                        currentTourStep = SahajTourStep.entries[prevOrdinal]
                    }
                },
                onDismiss = { showTour = false }
            )
        }

        // Tour Replay Pill if tour was dismissed
        if (!showTour) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        currentTourStep = SahajTourStep.STEP_1_SALARY
                        showTour = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EnterpriseTeal700)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = EnterpriseTeal700,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("How to File: Show Step-by-Step Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // HERO OUTCOME CARD: Clean, Plain-Language Result
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (refundDue > 0) EmeraldSuccess.copy(alpha = 0.08f) else EnterpriseTeal50
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.5.dp,
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (refundDue > 0) EmeraldSuccess else EnterpriseTeal600
                )
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (refundDue > 0) Icons.Default.CheckCircle else Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = if (refundDue > 0) EmeraldSuccess else EnterpriseTeal700,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (refundDue > 0) "Income Tax Refund Ready" else if (taxPayable == 0L) "Zero Tax Payable" else "Tax Due to Pay",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (refundDue > 0) EmeraldDark else Slate900
                            )
                        )
                    }
                    StatutoryBadge(
                        text = if (refundDue > 0) "Refund Due" else if (taxPayable == 0L) "₹0 Balance" else "Self-Assessment",
                        tone = if (refundDue > 0) BadgeTone.SUCCESS else if (taxPayable == 0L) BadgeTone.STATUTORY else BadgeTone.WARNING
                    )
                }

                Spacer(Modifier.height(10.dp))

                val displayAmount = if (refundDue > 0) refundDue else taxPayable
                Text(
                    text = "₹${Rupee.formatInr(displayAmount)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (refundDue > 0) EmeraldSuccess else if (taxPayable == 0L) EnterpriseTeal700 else AmberWarning
                    )
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (refundDue > 0)
                        "Your company deducted ₹${Rupee.formatInr(tdsClaimed)} in taxes, but your actual tax is only ₹${Rupee.formatInr(bestRegimeTax)}. The government will refund the excess directly to your bank account."
                    else if (taxPayable == 0L)
                        "Your total income qualifies for full tax rebate under Section 87A. You do not owe any income tax this year!"
                    else
                        "You have a remaining tax balance of ₹${Rupee.formatInr(taxPayable)}. Pay instantly via UPI/Challan 280 before filing.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600, lineHeight = 18.sp)
                )
            }
        }

        // STEP 1: WHAT YOU EARNED (Plain Language)
        ZeroTaxCard {
            ZeroTaxSectionHeader(
                title = "1. What You Earned (FY 2025-26)",
                subtitle = "Extracted safely in-memory from your Form 16 & AIS",
                badgeText = "Form 16 Verified",
                badgeTone = BadgeTone.SUCCESS
            )

            Spacer(Modifier.height(12.dp))

            EnterpriseLedgerRow(
                label = "Salary from Employer",
                statutoryRef = "Form 16 Part B",
                amount = grossSalary
            )
            EnterpriseLedgerRow(
                label = "Savings Bank Interest",
                statutoryRef = "Annual AIS / TIS",
                amount = bankInterest
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Slate200)

            EnterpriseLedgerRow(
                label = "Total Income Earned",
                amount = grossSalary + bankInterest,
                isHighlighted = true
            )
        }

        // STEP 2: AUTOMATIC GOVERNMENT DISCOUNT
        ZeroTaxCard {
            ZeroTaxSectionHeader(
                title = "2. Automatic Government Discount",
                subtitle = "Applied automatically by law — zero receipts required",
                badgeText = "Sec 16(ia)",
                badgeTone = BadgeTone.STATUTORY
            )

            Spacer(Modifier.height(12.dp))

            EnterpriseLedgerRow(
                label = "Standard Salary Deduction",
                statutoryRef = "Flat ₹75,000",
                amount = standardDeduction,
                isDeduction = true
            )

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EnterpriseTeal50,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = EnterpriseTeal900,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "The government gives you this ₹75,000 discount automatically. Your taxable income is reduced to ₹${Rupee.formatInr(netTaxableEarnings)}.",
                        style = MaterialTheme.typography.bodySmall.copy(color = EnterpriseTeal900, fontSize = 11.sp)
                    )
                }
            }
        }

        // STEP 3: REGIME COMPARISON (Which saves you more?)
        ZeroTaxCard {
            ZeroTaxSectionHeader(
                title = "3. Best Tax Rule Chosen For You",
                subtitle = "We compared New vs Old Tax Regime automatically",
                badgeText = "Auto-Optimized",
                badgeTone = BadgeTone.SUCCESS
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // New Regime Card (Winning)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EnterpriseTeal50,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, EnterpriseTeal600),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("New Regime", fontWeight = FontWeight.Bold, color = EnterpriseTeal900, fontSize = 13.sp)
                            StatutoryBadge(text = "Best", tone = BadgeTone.SUCCESS)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "₹${Rupee.formatInr(newRegimeTax)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal700)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("Zero paperwork needed", fontSize = 10.sp, color = Slate600)
                    }
                }

                // Old Regime Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Old Regime",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "₹${Rupee.formatInr(oldRegimeTax)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Needs 80C/80D proofs",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (regimeSavings > 0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "New Tax Regime saves you ₹${Rupee.formatInr(regimeSavings)} in tax!",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            ZeroTaxOutlinedButton(
                text = "Have Extra Deductions? (Home Loan, 80D, Rent)",
                onClick = onOpenDeductionsAdvisor
            )

            Spacer(Modifier.height(8.dp))

            ZeroTaxPrimaryButton(
                text = "Maximize Refund: Think Like a CA (Turn Tax to ₹0)",
                onClick = onOpenThinkLikeCa,
                containerColor = EmeraldSuccess,
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                }
            )
        }

        // STEP 4: REFUND BANK ACCOUNT
        ZeroTaxCard {
            ZeroTaxSectionHeader(
                title = "4. Bank Account for Direct Refund",
                subtitle = "Where the Income Tax Dept sends your money",
                badgeText = "Pre-Validated",
                badgeTone = BadgeTone.SUCCESS
            )

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate50,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        val displayBank = bankAccount?.bankName?.ifBlank { null } ?: "Direct CBDT Validated Bank"
                        val displayAcc = if (bankAccount != null && bankAccount.accountNumber.isNotBlank()) "Account: ${bankAccount.accountNumber} • IFSC: ${bankAccount.ifscCode}" else "Direct Credit to Pre-Validated Bank Account"
                        val isPreVal = bankAccount?.isPreValidated ?: true
                        Column {
                            Text(displayBank, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Text(displayAcc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                    StatutoryBadge(text = if (bankAccount?.isPreValidated == true) "Pre-Validated" else "Primary Refund", tone = BadgeTone.SUCCESS)
                }
            }
        }

        // STEP 5: 1-TAP BIOMETRIC e-FILING
        if (taxPayable > 0) {
            Button(
                onClick = onOpenTaxPaymentHub,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Pay ₹${Rupee.formatInr(taxPayable)} Tax via Instant UPI / Challan 280",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = PureWhite)
                    )
                }
            }
        }

        ZeroTaxPrimaryButton(
            text = "Authenticate with Biometrics to e-File ITR-1",
            onClick = { showBiometricFilingDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        ZeroTaxOutlinedButton(
            text = "Review AIS Tax Transactions & Form 26AS",
            onClick = onOpenAisTransactions,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = EnterpriseTeal700,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        Spacer(Modifier.height(48.dp))
    }

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

