package `in`.zerotax.app.ui.sugam

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.app.ui.timeline.StatutoryTimelineBanner
import `in`.zerotax.core.model.Rupee
import kotlin.math.max

enum class PresumptiveType(val section: String, val title: String, val threshold: String, val defaultRatePercent: Int) {
    SEC_44ADA_PROFESSIONAL("Sec 44ADA", "Freelancer & Professional", "Up to ₹75 Lakhs", 50),
    SEC_44AD_DIGITAL_BIZ("Sec 44AD", "Small Business (Digital Receipts ≥ 95%)", "Up to ₹3 Crores", 6),
    SEC_44AD_CASH_BIZ("Sec 44AD", "Small Business (Cash / Retail)", "Up to ₹2 Crores", 8)
}

/**
 * Fast-Start Interactive Deck for SUGAM (ITR-4) Presumptive Taxpayers.
 * Enables freelancers, gig economy professionals, and small businesses
 * to compute presumptive profit and file in under 2 minutes.
 */
@Composable
fun SugamPresumptiveFastStartView(
    pan: String,
    onLaunchPortalAssistant: () -> Unit,
    onOpenTaxPaymentHub: () -> Unit,
    onOpenAisTransactions: () -> Unit,
    onOpenPortalSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(PresumptiveType.SEC_44ADA_PROFESSIONAL) }
    var grossReceiptsInput by remember { mutableStateOf("2400000") }
    var declaredProfitPercent by remember { mutableIntStateOf(selectedType.defaultRatePercent) }
    var showBiometricFilingDialog by remember { mutableStateOf(false) }

    val grossReceipts = grossReceiptsInput.toLongOrNull() ?: 0L
    val minProfitPercent = selectedType.defaultRatePercent
    val effectiveProfitPercent = max(minProfitPercent, declaredProfitPercent)
    val presumptiveProfit = (grossReceipts * effectiveProfitPercent) / 100L

    // Rough estimated tax computation under New Tax Regime FY 25-26
    val estimatedTax = remember(presumptiveProfit) {
        when {
            presumptiveProfit <= 4_00_000L -> 0L
            presumptiveProfit <= 7_00_000L -> ((presumptiveProfit - 4_00_000L) * 5) / 100L
            presumptiveProfit <= 10_00_000L -> 15_000L + ((presumptiveProfit - 7_00_000L) * 10) / 100L
            presumptiveProfit <= 12_00_000L -> 45_000L + ((presumptiveProfit - 10_00_000L) * 15) / 100L
            presumptiveProfit <= 15_00_000L -> 75_000L + ((presumptiveProfit - 12_00_000L) * 20) / 100L
            else -> 1_35_000L + ((presumptiveProfit - 15_00_000L) * 30) / 100L
        }.let { tax ->
            // Sec 87A rebate for income up to ₹7 Lakhs / ₹12 Lakhs threshold
            if (presumptiveProfit <= 12_00_000L) 0L else (tax * 104L) / 100L // with 4% cess
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Sync 26AS Client TDS (Sec 194J/194C)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = getTealHighlightText())
                        )
                        Text(
                            "Pull verified TDS credits & turnover directly from ITD portal in-app.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        )
                    }
                }
                StatutoryBadge(text = "1-Tap Sync", tone = BadgeTone.SUCCESS)
            }
        }

        // FAST-START HERO CARD: 2-Minute ITR-4 SUGAM Presumptive Deck
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = getTealHighlightBg()),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.5.dp,
                brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = getTealHighlightIcon(),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Fast-Start Presumptive Deck",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = getTealHighlightText())
                            )
                            Text(
                                "ITR-4 (SUGAM) • Zero Books of Accounts Required",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            )
                        }
                    }
                    StatutoryBadge(text = selectedType.section, tone = BadgeTone.SUCCESS)
                }

                Spacer(Modifier.height(14.dp))

                // Presumptive Scheme Selector Chips
                Text("Select your activity:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                Spacer(Modifier.height(6.dp))
                PresumptiveType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) EnterpriseTeal600 else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) EnterpriseTeal600 else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                selectedType = type
                                declaredProfitPercent = type.defaultRatePercent
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = type.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PureWhite else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Limit: ${type.threshold} • Min Rate: ${type.defaultRatePercent}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) EnterpriseTeal100 else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // STEP 1: ENTER GROSS RECEIPTS
        ZeroTaxCard {
            ZeroTaxSectionHeader(
                title = "1. Gross Receipts / Annual Turnover",
                subtitle = "Total payments received via bank, UPI, or invoices",
                badgeText = "No Bills Needed",
                badgeTone = BadgeTone.STATUTORY
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = grossReceiptsInput,
                onValueChange = { grossReceiptsInput = it.filter { char -> char.isDigit() } },
                label = { Text("Annual Gross Receipts (₹)") },
                prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EnterpriseTeal600,
                    unfocusedBorderColor = Slate300
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Statutory Benefit: u/s ${selectedType.section}, you do not need to maintain ledgers, balance sheets, or expense vouchers.",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
            )
        }

        // STEP 2: AUTO-COMPUTED PRESUMPTIVE PROFIT
        ZeroTaxCard {
            ZeroTaxSectionHeader(
                title = "2. Presumptive Net Profit & Estimated Tax",
                subtitle = "Deemed taxable profit after statutory expenses",
                badgeText = "${effectiveProfitPercent}% Rate Applied",
                badgeTone = BadgeTone.SUCCESS
            )

            Spacer(Modifier.height(12.dp))

            EnterpriseLedgerRow(
                label = "Gross Receipts Received",
                amount = grossReceipts
            )
            EnterpriseLedgerRow(
                label = "Deemed Statutory Expenses",
                statutoryRef = "${100 - effectiveProfitPercent}% Allowance",
                amount = grossReceipts - presumptiveProfit,
                isDeduction = true
            )
            EnterpriseLedgerRow(
                label = "Net Taxable Presumptive Profit",
                amount = presumptiveProfit,
                isHighlighted = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Slate200)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estimated Tax Liability:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = "₹${Rupee.formatInr(estimatedTax)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (estimatedTax == 0L) EmeraldSuccess else AmberWarning
                    )
                )
            }
        }

        // STEP 3: 1-TAP FAST FILING
        if (estimatedTax > 0) {
            Button(
                onClick = onOpenTaxPaymentHub,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                modifier = Modifier.fillMaxWidth().height(46.dp)
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
                        text = "Pay Advance Tax / Self-Assessment (Challan 280)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = PureWhite)
                    )
                }
            }
        }

        ZeroTaxPrimaryButton(
            text = "Authenticate with Biometrics to e-File ITR-4 (SUGAM)",
            onClick = { showBiometricFilingDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        OutlinedButton(
            onClick = onOpenAisTransactions,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate300)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Slate700,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Verify Client TDS u/s 194J / 194C in AIS", style = MaterialTheme.typography.labelSmall.copy(color = Slate700))
            }
        }
    }

    if (showBiometricFilingDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricFilingDialog = false },
            containerColor = Slate900,
            title = {
                Text("Confirm Biometric Signature for ITR-4", color = PureWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "You are filing ITR-4 (SUGAM) for PAN $pan.\n\n" +
                    "• Section: ${selectedType.section}\n" +
                    "• Gross Turnover: ₹${Rupee.formatInr(grossReceipts)}\n" +
                    "• Presumptive Profit (${effectiveProfitPercent}%): ₹${Rupee.formatInr(presumptiveProfit)}\n" +
                    "• Estimated Tax: ₹${Rupee.formatInr(estimatedTax)}\n\n" +
                    "Scan fingerprint or face to dispatch to CBDT.",
                    color = Slate300,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBiometricFilingDialog = false
                        onLaunchPortalAssistant()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600)
                ) {
                    Text("Verify & Submit to CBDT", fontWeight = FontWeight.Bold)
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

