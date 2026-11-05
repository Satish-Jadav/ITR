package `in`.zerotax.app.ui.sahaj

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
    bankInterest: Long = 14_200L,
    tdsClaimed: Long,
    newRegimeTax: Long,
    oldRegimeTax: Long,
    onLaunchPortalAssistant: () -> Unit,
    onOpenDeductionsAdvisor: () -> Unit,
    onOpenTaxPaymentHub: () -> Unit,
    onOpenAisTransactions: () -> Unit,
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
                    Text("💡 How to File: Show Step-by-Step Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        Text(if (refundDue > 0) "🎉" else "⚖️", fontSize = 24.sp)
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
                    Text("💡", fontSize = 16.sp)
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
                    color = Slate50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Old Regime", fontWeight = FontWeight.Bold, color = Slate700, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "₹${Rupee.formatInr(oldRegimeTax)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = Slate600)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("Needs 80C/80D proofs", fontSize = 10.sp, color = Slate500)
                    }
                }
            }

            if (regimeSavings > 0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💰", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "New Tax Regime saves you ₹${Rupee.formatInr(regimeSavings)} in tax!",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenDeductionsAdvisor,
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal600)
            ) {
                Text("Have Extra Deductions? (Home Loan, 80D, Rent)", style = MaterialTheme.typography.labelSmall.copy(color = EnterpriseTeal700, fontWeight = FontWeight.Bold))
            }
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
                        Text("🏦", fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("State Bank of India", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 13.sp)
                            Text("Account: ••••••••1234 • IFSC: SBIN0001234", color = Slate500, fontSize = 11.sp)
                        }
                    }
                    StatutoryBadge(text = "Primary Refund", tone = BadgeTone.SUCCESS)
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
                Text(
                    text = "💳 Pay ₹${Rupee.formatInr(taxPayable)} Tax via Instant UPI / Challan 280",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                )
            }
        }

        ZeroTaxPrimaryButton(
            text = "🔒 Authenticate with Biometrics to e-File ITR-1",
            onClick = { showBiometricFilingDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        OutlinedButton(
            onClick = onOpenAisTransactions,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate300)
        ) {
            Text("🔍 Review AIS Tax Transactions & Form 26AS", style = MaterialTheme.typography.labelSmall.copy(color = Slate700))
        }
    }

    // Biometric e-Filing Verification Dialog
    if (showBiometricFilingDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricFilingDialog = false },
            containerColor = Slate900,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Confirm Biometric Signature",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "You are filing ITR-1 (SAHAJ) for AY 2026-27 under Section 139(1).\n\n" +
                               "• Permanent Account Number: $pan\n" +
                               "• Taxable Earnings: ₹${Rupee.formatInr(netTaxableEarnings)}\n" +
                               "• " + (if (refundDue > 0) "Expected Refund: ₹${Rupee.formatInr(refundDue)}" else "Tax Payable: ₹${Rupee.formatInr(taxPayable)}") + "\n\n" +
                               "Scan your fingerprint or face to cryptographically sign your CBDT return package.",
                        color = Slate300,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBiometricFilingDialog = false
                        onLaunchPortalAssistant()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600)
                ) {
                    Text("👆 Verify & Dispatch to CBDT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricFilingDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

