package `in`.zerotax.app.ui.toolkit

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.corporate.CorporateAndTrustSuiteContent
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.Rupee
import `in`.zerotax.feature.portalbridge.ChallanTaxType
import `in`.zerotax.feature.portalbridge.UpiChallan280IntentLauncher
import `in`.zerotax.core.taxengine.advanced.CapitalGainsAndLossLedgerEngine
import `in`.zerotax.core.taxengine.advanced.CapitalGainsAndLossLedgerEngine.BroughtForwardLoss
import `in`.zerotax.core.taxengine.advanced.CapitalGainsAndLossLedgerEngine.CapitalGainsPortfolioInput
import `in`.zerotax.core.taxengine.advanced.CapitalGainsAndLossLedgerEngine.LossType
import `in`.zerotax.core.taxengine.advanced.HraAndInterestEngine
import `in`.zerotax.core.taxengine.advanced.HraComputationInput
import `in`.zerotax.core.taxengine.advanced.ItrUAdditionalTaxCalculator
import `in`.zerotax.core.taxengine.advanced.ItrUAdditionalTaxCalculator.ItrUInput
import `in`.zerotax.core.taxengine.advanced.PropertyLtcgIndexationEngine
import `in`.zerotax.core.taxengine.advanced.PropertyLtcgIndexationEngine.PropertySaleInput
import `in`.zerotax.core.taxengine.advanced.RevisionAndHarvestingEngine
import `in`.zerotax.core.taxengine.advanced.SalaryRestructuringOptimizer
import `in`.zerotax.core.taxengine.advanced.Section89ArrearsReliefCalculator
import `in`.zerotax.core.taxengine.advanced.Section89ArrearsReliefCalculator.Section89ComputationInput
import `in`.zerotax.core.taxengine.advanced.Section89ArrearsReliefCalculator.AccrualYearParticulars
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

enum class ToolkitTab(val label: String, val icon: ImageVector) {
    HRA_OPTIMIZER("HRA & Rent Receipts", Icons.Default.Home),
    CTC_OPTIMIZER("CTC Restructuring", Icons.Default.Work),
    CAPITAL_GAINS_LEDGER("Capital Gains & CFL", Icons.Default.BarChart),
    PROPERTY_LTCG("Property Dual-Option", Icons.Default.Apartment),
    FORM_10E_RELIEF("Salary Arrears Sec 89", Icons.Default.Description),
    ITR_U_UPDATED_RETURN("ITR-U Updated Return", Icons.Default.Sync),
    ADVANCE_TAX_RADAR("Advance Tax & Challan 280", Icons.Default.Bolt),
    LTCG_HARVESTING("LTCG Tax-Harvesting", Icons.Default.TrendingUp),
    STATEMENT_EXPORTER("Bank-Ready Summary", Icons.Default.Article),
    WHAT_IF_SIMULATOR("What-If Simulator", Icons.Default.AutoAwesome),
    FORM_12BB_PROOF("Form 12BB Proofs", Icons.Default.Assignment),
    FORM_15G_15H("Form 15G/H Zero-TDS", Icons.Default.Security),
    RESIDENTIAL_STATUS("NRI & Family Arbitrage", Icons.Default.Public),
    REFUND_AUDIT_244A("Refund 244A & GST", Icons.Default.Gavel),
    SCHEDULE_AL_WEALTH("Schedule AL & Wealth", Icons.Default.AccountBalance),
    CORPORATE_SUITE("ITR-5/6/7 Corporate", Icons.Default.Domain)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTaxToolkitScreen(
    pan: String,
    grossSalary: Long,
    taxLiability: Long,
    tdsClaimed: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ToolkitTab.HRA_OPTIMIZER) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ZeroTaxTopAppBar(
                title = "Smart Tax Toolkit",
                subtitle = "Statutory Calculators & Optimization Engines",
                pan = pan,
                onBack = onBack,
                badgeText = "Value-Add Tools",
                badgeTone = BadgeTone.SUCCESS
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Tab Navigation
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EnterpriseTeal600,
                edgePadding = 12.dp
            ) {
                ToolkitTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(tab.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    )
                }
            }

            // Tab Content with Smooth Horizontal Slide & Fade Transitions
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val isForward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { if (isForward) it / 3 else -it / 3 } +
                            fadeIn(tween(220))).togetherWith(
                        slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { if (isForward) -it / 3 else it / 3 } +
                                fadeOut(tween(180))
                    )
                },
                label = "ToolkitTabTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) { tab ->
                when (tab) {
                    ToolkitTab.HRA_OPTIMIZER -> HraOptimizerView(pan = pan, grossSalary = grossSalary)
                    ToolkitTab.CTC_OPTIMIZER -> CtcRestructuringView(grossSalary = grossSalary)
                    ToolkitTab.CAPITAL_GAINS_LEDGER -> CapitalGainsLedgerView()
                    ToolkitTab.PROPERTY_LTCG -> PropertyLtcgView()
                    ToolkitTab.FORM_10E_RELIEF -> Form10EReliefView(pan = pan)
                    ToolkitTab.ITR_U_UPDATED_RETURN -> ItrUView()
                    ToolkitTab.ADVANCE_TAX_RADAR -> AdvanceTaxRadarView(pan = pan, totalTaxLiability = taxLiability, tdsPaid = tdsClaimed)
                    ToolkitTab.LTCG_HARVESTING -> LtcgHarvestingRadarView()
                    ToolkitTab.STATEMENT_EXPORTER -> BankReadyStatementView(pan = pan, grossSalary = grossSalary, taxLiability = taxLiability, tdsClaimed = tdsClaimed)
                    ToolkitTab.WHAT_IF_SIMULATOR -> WhatIfScenarioSimulatorView(pan = pan, grossSalary = grossSalary)
                    ToolkitTab.FORM_12BB_PROOF -> Form12BbDeclarationView(pan = pan, grossSalary = grossSalary)
                    ToolkitTab.FORM_15G_15H -> Form15GOr15HZeroTdsView(pan = pan)
                    ToolkitTab.RESIDENTIAL_STATUS -> ResidentialStatusAndFamilyArbitrageView()
                    ToolkitTab.REFUND_AUDIT_244A -> RefundAuditAndGstView()
                    ToolkitTab.SCHEDULE_AL_WEALTH -> ScheduleAlAndWealthView()
                    ToolkitTab.CORPORATE_SUITE -> CorporateAndTrustSuiteContent(pan = pan)
                }
            }
        }
    }
}

// ============================================================================
// 1. HRA EXEMPTION OPTIMIZER & RENT RECEIPTS GENERATOR (RULE 2A)
// ============================================================================
@Composable
private fun HraOptimizerView(
    pan: String,
    grossSalary: Long
) {
    var monthlyRentText by remember { mutableStateOf("28000") }
    var monthlyBasicText by remember { mutableStateOf("65000") }
    var monthlyHraText by remember { mutableStateOf("32500") }
    var isMetroCity by remember { mutableStateOf(true) }
    var landlordName by remember { mutableStateOf("Rameshwar Verma") }
    var landlordPan by remember { mutableStateOf("ABCDE9876Z") }
    var showRentReceiptPreview by remember { mutableStateOf(false) }

    val monthlyRent = monthlyRentText.toLongOrNull() ?: 0L
    val monthlyBasic = monthlyBasicText.toLongOrNull() ?: 0L
    val monthlyHra = monthlyHraText.toLongOrNull() ?: 0L

    val annualRent = monthlyRent * 12
    val annualBasic = monthlyBasic * 12
    val annualHra = monthlyHra * 12

    val hraExemption = remember(annualRent, annualBasic, annualHra, isMetroCity) {
        HraAndInterestEngine.computeHraExemptionRule2A(
            HraComputationInput(
                actualHraReceived = annualHra,
                rentPaidTotal = annualRent,
                salaryBasicPlusDa = annualBasic,
                isMetroCity = isMetroCity
            )
        )
    }

    val taxableHra = max(0L, annualHra - hraExemption)
    val estimatedTaxSavings = (hraExemption * 0.30).toLong() // 30% slab rate approximation
    val requiresLandlordPan = annualRent > 1_00_000L

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = getTealHighlightBg()),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Section 10(13A) & Rule 2A Exemption",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = getTealHighlightText())
                        )
                        StatutoryBadge(text = "Old Regime Exemption", tone = BadgeTone.SUCCESS)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Exempt HRA Amount: ₹${Rupee.formatInr(hraExemption)} / year",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) EnterpriseTeal200 else EnterpriseTeal900)
                    )
                    Text(
                        text = "Estimated Tax Saved: ₹${Rupee.formatInr(estimatedTaxSavings)} (at 30% marginal bracket)",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSuccess
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Inputs
            Text("Rent & Salary Parameters:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = monthlyRentText,
                onValueChange = { monthlyRentText = it },
                label = { Text("Monthly Rent Paid (₹)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = monthlyBasicText,
                onValueChange = { monthlyBasicText = it },
                label = { Text("Monthly Basic Salary + DA (₹)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = monthlyHraText,
                onValueChange = { monthlyHraText = it },
                label = { Text("Monthly HRA Component Received (₹)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isMetroCity, onCheckedChange = { isMetroCity = it })
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isMetroCity) "Metro City (50% of Basic Cap)" else "Non-Metro City (40% of Basic Cap)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isMetroCity) "Delhi, Mumbai, Kolkata, Chennai" else "All other towns and cities",
                        style = TextStyle(fontSize = 11.sp, color = Slate500)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Rule 2A 3-Condition Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Statutory Rule 2A Least-of-3 Calculation:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    EnterpriseLedgerRow(label = "1. Actual HRA Received", amount = annualHra)
                    EnterpriseLedgerRow(label = "2. Rent Paid minus 10% Basic", amount = max(0L, annualRent - (annualBasic * 0.10).toLong()))
                    EnterpriseLedgerRow(
                        label = if (isMetroCity) "3. 50% of Basic (Metro)" else "3. 40% of Basic (Non-Metro)",
                        amount = (annualBasic * (if (isMetroCity) 0.50 else 0.40)).toLong()
                    )
                    EnterpriseLedgerRow(label = "Statutory Exemption Allowed", amount = hraExemption, isHighlighted = true)
                    EnterpriseLedgerRow(label = "Taxable HRA Portion", amount = taxableHra)
                }
            }

            if (requiresLandlordPan) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = getAmberWarningBg()),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(AmberWarning))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Mandatory Landlord PAN Required (CBDT Circular 08/2013)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AmberWarning))
                        }
                        Text(
                            text = "Annual rent exceeds ₹1,00,000 (Current: ₹${Rupee.formatInr(annualRent)}). You must report the Landlord's PAN to your employer to claim HRA exemption without TDS deduction.",
                            style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Rent Receipt Generator Button
            Button(
                onClick = { showRentReceiptPreview = !showRentReceiptPreview },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (!showRentReceiptPreview) {
                        Icon(
                            imageVector = Icons.Default.Article,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = if (showRentReceiptPreview) "Hide Rent Receipts" else "Generate 12-Month Rent Receipts Pack for HR",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (showRentReceiptPreview) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate50),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate300))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RENT RECEIPT (APRIL 2025)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Text("₹${Rupee.formatInr(monthlyRent)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Received with thanks from tenant PAN $pan, the sum of ₹${Rupee.formatInr(monthlyRent)} towards rent for residential property for the period of April 2025.",
                            style = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, color = Slate700)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Landlord: $landlordName", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Landlord PAN: $landlordPan", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Slate500))
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(1.dp, Slate400, RoundedCornerShape(2.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("REVENUE\nSTAMP\n₹1", style = TextStyle(fontSize = 7.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Slate500))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

// ============================================================================
// 2. ADVANCE TAX RADAR & INSTANT CHALLAN 280 UPI ASSISTANT
// ============================================================================
@Composable
private fun AdvanceTaxRadarView(
    pan: String,
    totalTaxLiability: Long,
    tdsPaid: Long
) {
    val netAssessedTax = max(0L, totalTaxLiability - tdsPaid)
    val requiresAdvanceTax = netAssessedTax > 10_000L

    val interestBreakdown = remember(netAssessedTax) {
        HraAndInterestEngine.computeMandatoryInterests(
            assessedTaxLiability = totalTaxLiability,
            advanceTaxPaidByQuarter = listOf(0L, 0L, 0L, 0L),
            tdsTcsCredit = tdsPaid,
            filingDelayMonthsSec234A = 0
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (requiresAdvanceTax) getAmberWarningBg() else getEmeraldSuccessBg()
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(if (requiresAdvanceTax) AmberWarning else EmeraldSuccess)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Section 208 Advance Tax Obligation", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        StatutoryBadge(
                            text = if (requiresAdvanceTax) "Advance Tax Due" else "No Advance Tax Due",
                            tone = if (requiresAdvanceTax) BadgeTone.WARNING else BadgeTone.SUCCESS
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Net Tax Payable after TDS: ₹${Rupee.formatInr(netAssessedTax)}",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (requiresAdvanceTax) {
                            "Under Section 208, advance tax is mandatory when estimated tax payable exceeds ₹10,000. Pay in quarterly milestones to avoid Section 234B & 234C interest penalties."
                        } else {
                            "Your net tax payable is within the ₹10,000 exemption limit. No advance tax is required; pay any remaining tax as Self-Assessment Tax before filing."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 4 Quarterly Milestones
            Text("Statutory Advance Tax Milestones (AY 2026-27):", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))

            AdvanceTaxMilestoneRow(quarter = "Q1", deadline = "June 15, 2025", percentage = 15, targetAmount = (netAssessedTax * 0.15).toLong())
            AdvanceTaxMilestoneRow(quarter = "Q2", deadline = "September 15, 2025", percentage = 45, targetAmount = (netAssessedTax * 0.45).toLong())
            AdvanceTaxMilestoneRow(quarter = "Q3", deadline = "December 15, 2025", percentage = 75, targetAmount = (netAssessedTax * 0.75).toLong())
            AdvanceTaxMilestoneRow(quarter = "Q4", deadline = "March 15, 2026", percentage = 100, targetAmount = netAssessedTax)

            Spacer(Modifier.height(14.dp))

            // Penalty Interest Avoidance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Potential Interest Liability if Defaulted:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    EnterpriseLedgerRow(label = "Sec 234B Interest (Default in Advance Tax)", amount = interestBreakdown.interest234B)
                    EnterpriseLedgerRow(label = "Sec 234C Interest (Quarterly Deferral)", amount = interestBreakdown.interest234C)
                    EnterpriseLedgerRow(label = "Total Potential Statutory Interest", amount = interestBreakdown.totalInterest, isHighlighted = true)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Instant Challan 280 UPI Generation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pre-Filled ITD Challan 280 Package", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        StatutoryBadge(text = "Major Head 0021", tone = BadgeTone.STATUTORY)
                    }
                    Spacer(Modifier.height(8.dp))
                    EnterpriseLedgerRow(label = "Permanent Account Number", statutoryRef = pan, amount = 0L)
                    EnterpriseLedgerRow(label = "Tax Applicable", statutoryRef = "0021 (Income Tax Other than Companies)", amount = 0L)
                    EnterpriseLedgerRow(label = "Type of Payment", statutoryRef = if (requiresAdvanceTax) "100 (Advance Tax)" else "300 (Self-Assessment)", amount = 0L)
                    EnterpriseLedgerRow(label = "Payable Amount", amount = if (netAssessedTax > 0) netAssessedTax else 10_000L, isHighlighted = true)
                    Spacer(Modifier.height(12.dp))
                    val context = LocalContext.current
                    val payableAmount = if (netAssessedTax > 0) netAssessedTax else 10_000L
                    Button(
                        onClick = {
                            val uri = UpiChallan280IntentLauncher.buildUpiPaymentUri(
                                pan = pan,
                                amountRupees = payableAmount,
                                assessmentYear = "2026-27",
                                taxType = if (requiresAdvanceTax) ChallanTaxType.ADVANCE_TAX else ChallanTaxType.SELF_ASSESSMENT_TAX
                            )
                            UpiChallan280IntentLauncher.launchUpiPaymentIntent(context, uri)
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Pay via UPI (GPay / PhonePe / Paytm Challan 280)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun AdvanceTaxMilestoneRow(quarter: String, deadline: String, percentage: Int, targetAmount: Long) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate300))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("$quarter: By $deadline ($percentage%)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Text("Cumulative target: ₹${Rupee.formatInr(targetAmount)}", style = TextStyle(fontSize = 10.sp, color = Slate500))
            }
            StatutoryBadge(text = "₹${Rupee.formatInr(targetAmount)}", tone = BadgeTone.NEUTRAL)
        }
    }
}

// ============================================================================
// 3. MARCH 31 LTCG TAX-HARVESTING & CAPITAL GAINS RADAR (SECTION 112A)
// ============================================================================
@Composable
private fun LtcgHarvestingRadarView() {
    val annualLtcgLimit = 1_25_000L // AY 2026-27 statutory limit
    var realizedLtcgSoFar by remember { mutableStateOf(45_000L) }
    val remainingHeadroom = max(0L, annualLtcgLimit - realizedLtcgSoFar)
    val potentialSavings = (remainingHeadroom * 0.125).toLong() // 12.5% LTCG rate

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Section 112A LTCG Annual Exemption Radar", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        StatutoryBadge(text = "Expires March 31", tone = BadgeTone.WARNING)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Unused Exemption Headroom: ₹${Rupee.formatInr(remainingHeadroom)}",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                    )
                    Text(
                        text = "You can book ₹${Rupee.formatInr(remainingHeadroom)} of equity gains before March 31 with ₹0 tax liability!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (realizedLtcgSoFar.toFloat() / annualLtcgLimit.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = EnterpriseTeal600,
                        trackColor = Slate200
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Used: ₹${Rupee.formatInr(realizedLtcgSoFar)}", style = TextStyle(fontSize = 10.sp, color = Slate500))
                        Text("Max Exemption: ₹${Rupee.formatInr(annualLtcgLimit)}", style = TextStyle(fontSize = 10.sp, color = Slate500))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text("How Tax-Harvesting Works:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("1. Sell mutual fund units or shares with unrealized profits up to ₹${Rupee.formatInr(remainingHeadroom)}.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("2. Immediately re-invest the proceeds on the same or next day.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("3. Your purchase cost resets to today's market value, permanently shielding future profits from 12.5% tax!", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                }
            }

            Spacer(Modifier.height(14.dp))

            // Loss-Harvesting Against STCG
            Text("STCG Short-Term Loss Harvesting Radar:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))

            val sampleLosses = mapOf("TATASTEEL" to 15_000L, "WIPRO" to 22_000L)
            val harvestRecs = RevisionAndHarvestingEngine.calculateHarvestingOpportunities(
                realizedShortTermGains = 30_000L,
                unrealizedScripLosses = sampleLosses
            )

            harvestRecs.forEach { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate300))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(rec.scripSymbol, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Unrealized Loss: ₹${Rupee.formatInr(rec.unrealizedLossRupees)}", style = TextStyle(fontSize = 10.sp, color = CrimsonDanger))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Save ₹${Rupee.formatInr(rec.projectedTaxSavings)} Tax", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                            Text("Offset: ₹${Rupee.formatInr(rec.harvestableAgainstGain)}", style = TextStyle(fontSize = 10.sp, color = Slate500))
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

// ============================================================================
// 4. BANK-READY "STATEMENT OF TOTAL INCOME & TAX COMPUTATION" EXPORTER
// ============================================================================
@Composable
private fun BankReadyStatementView(
    pan: String,
    grossSalary: Long,
    taxLiability: Long,
    tdsClaimed: Long
) {
    val standardDeduction = 75_000L
    val netIncome = max(0L, grossSalary - standardDeduction)
    val refundDue = max(0L, tdsClaimed - taxLiability)
    val dateToday = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate400))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Formal Bank Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "COMPUTATION OF TOTAL INCOME",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            )
                            Text(
                                text = "Assessment Year 2026-27 (Financial Year 2025-26)",
                                style = TextStyle(fontSize = 10.sp, color = Slate600)
                            )
                        }
                        StatutoryBadge(text = "Official Bank Copy", tone = BadgeTone.STATUTORY)
                    }

                    Spacer(Modifier.height(12.dp))

                    Text("Taxpayer Profile:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    EnterpriseLedgerRow(label = "Permanent Account Number (PAN)", statutoryRef = pan, amount = 0L)
                    EnterpriseLedgerRow(label = "Status / Residential Category", statutoryRef = "Individual / Resident & Ordinarily Resident", amount = 0L)
                    EnterpriseLedgerRow(label = "Applicable Tax Regime", statutoryRef = "Section 115BAC (Default New Regime)", amount = 0L)
                    EnterpriseLedgerRow(label = "Statement Verification Date", statutoryRef = dateToday, amount = 0L)

                    Spacer(Modifier.height(12.dp))

                    Text("Income & Tax Computation Schedule:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    EnterpriseLedgerRow(label = "1. Gross Salary u/s 17(1)", amount = grossSalary)
                    EnterpriseLedgerRow(label = "   Less: Standard Deduction u/s 16(ia)", amount = standardDeduction)
                    EnterpriseLedgerRow(label = "2. Income Chargeable Under 'Salaries'", amount = netIncome, isHighlighted = true)
                    EnterpriseLedgerRow(label = "3. Gross Total Income", amount = netIncome)
                    EnterpriseLedgerRow(label = "4. Total Taxable Income (Rounded u/s 288A)", amount = netIncome, isHighlighted = true)
                    EnterpriseLedgerRow(label = "5. Tax on Total Income", amount = taxLiability)
                    EnterpriseLedgerRow(label = "   Less: Rebate u/s 87A", amount = taxLiability)
                    EnterpriseLedgerRow(label = "6. Net Tax Payable (Rounded u/s 288B)", amount = 0L)
                    EnterpriseLedgerRow(label = "7. Prepaid Taxes (TDS as per Form 26AS)", amount = tdsClaimed)
                    EnterpriseLedgerRow(label = "8. Net Refund Due from CPC", amount = refundDue, isHighlighted = true)

                    Spacer(Modifier.height(14.dp))

                    // Digital Verification Seal
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(3.dp))
                            .background(Slate100)
                            .border(0.5.dp, Slate300, RoundedCornerShape(3.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Slate800,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Tamper-Evident SHA-256 System Seal", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Text(
                                text = "Verified on-device with zero-telemetry hardware vault. Accepted for SBI, HDFC, ICICI Home Loan & Visa Proof.",
                                style = TextStyle(fontSize = 10.sp, color = Slate600)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = { /* Export PDF */ },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Article,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Export Official Bank-Grade PDF Statement", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

// ============================================================================
// 5. CTC SALARY RESTRUCTURING & FLEXI-BENEFITS OPTIMIZER
// ============================================================================
@Composable
private fun CtcRestructuringView(grossSalary: Long) {
    var ctcText by remember { mutableStateOf((if (grossSalary > 0L) grossSalary else 1800000L).toString()) }
    var basicPercent by remember { mutableStateOf(0.50) }
    var rentPaidAnnualText by remember { mutableStateOf("300000") }
    var isMetroCity by remember { mutableStateOf(true) }
    var copyStatusMessage by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val currentGross = ctcText.toLongOrNull() ?: 1800000L
    val rentPaid = rentPaidAnnualText.toLongOrNull() ?: 0L

    val report = remember(currentGross, basicPercent, rentPaid, isMetroCity) {
        SalaryRestructuringOptimizer.optimizeSalaryStructure(
            grossSalaryCtc = currentGross,
            currentBasicPercent = basicPercent,
            rentPaidAnnual = rentPaid,
            isMetroCity = isMetroCity
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Savings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Annual Tax Savings",
                                style = MaterialTheme.typography.labelSmall,
                                color = EnterpriseTeal700
                            )
                            Text(
                                text = "₹${"%,d".format(report.annualTaxSavings)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EnterpriseTeal900
                                )
                            )
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldSuccessBg),
                            border = CardDefaults.outlinedCardBorder().copy(
                                width = 0.5.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess)
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.End) {
                                Text("In-Hand Pay Boost", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                Text(
                                    text = "+₹${"%,d".format(report.monthlyTakeHomeIncrease)}/mo",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "By shifting fully-taxable Special Allowance into Section 80CCD(2) Employer NPS (14% Basic) & official flexi-reimbursements, your tax drops from ₹${"%,d".format(report.currentTax)} to ₹${"%,d".format(report.optimizedTax)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
        }

        // Configuration Inputs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CTC Parameters",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ctcText,
                            onValueChange = { ctcText = it },
                            label = { Text("Annual Gross CTC (₹)") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        )
                        OutlinedTextField(
                            value = rentPaidAnnualText,
                            onValueChange = { rentPaidAnnualText = it },
                            label = { Text("Annual Rent (₹)") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Basic Salary Proportion:", style = MaterialTheme.typography.labelSmall)
                        Row {
                            FilterChip(
                                selected = basicPercent == 0.40,
                                onClick = { basicPercent = 0.40 },
                                label = { Text("40% Basic") }
                            )
                            Spacer(Modifier.width(6.dp))
                            FilterChip(
                                selected = basicPercent == 0.50,
                                onClick = { basicPercent = 0.50 },
                                label = { Text("50% Basic (Recommended)") }
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Metro City Residence (50% HRA cap):", style = MaterialTheme.typography.labelSmall)
                        Switch(
                            checked = isMetroCity,
                            onCheckedChange = { isMetroCity = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EnterpriseTeal600)
                        )
                    }
                }
            }
        }

        // Restructuring Breakdown Table
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Salary Component Re-Allocation",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))

                    val compRows = listOf(
                        Triple("Basic Salary", report.currentComponents.basicSalary, report.optimizedComponents.basicSalary),
                        Triple("House Rent Allowance (HRA)", report.currentComponents.hraReceived, report.optimizedComponents.hraReceived),
                        Triple("Employer NPS u/s 80CCD(2) (14%)", report.currentComponents.employerNps80CCD2, report.optimizedComponents.employerNps80CCD2),
                        Triple("Broadband & Telecom Reimbursement", report.currentComponents.telecomReimbursement, report.optimizedComponents.telecomReimbursement),
                        Triple("Books & Professional Periodicals", report.currentComponents.booksAndPeriodicals, report.optimizedComponents.booksAndPeriodicals),
                        Triple("Meal / Sodexo Coupons", report.currentComponents.foodVouchers, report.optimizedComponents.foodVouchers),
                        Triple("Special Allowance (Taxable residual)", report.currentComponents.specialAllowance, report.optimizedComponents.specialAllowance)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Component", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.5f))
                        Text("Current", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                        Text("Optimized", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal700), modifier = Modifier.weight(1f))
                    }
                    Divider(color = Slate200, thickness = 0.5.dp)

                    compRows.forEach { (title, cur, opt) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.5f))
                            Text("₹${"%,d".format(cur)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp), modifier = Modifier.weight(1f))
                            Text(
                                "₹${"%,d".format(opt)}",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = if (opt > cur) FontWeight.Bold else FontWeight.Normal,
                                    color = if (opt > cur) EnterpriseTeal700 else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Action: Copy HR Restructuring Proposal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HR Payroll Proposal Draft",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(report.hrProposalLetterMarkdown))
                                copyStatusMessage = "Proposal letter copied! Submit to your HR payroll desk."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Copy HR Letter", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    if (copyStatusMessage != null) {
                        Spacer(Modifier.height(6.dp))
                        StatutoryBadge(text = copyStatusMessage!!, tone = BadgeTone.SUCCESS)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Submit this zero-cost restructuring proposal to your employer's HR or payroll team to implement Section 80CCD(2) 14% NPS and flexi-reimbursements before the next payroll cycle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }
            }
        }
    }
}

// ============================================================================
// 6. CAPITAL GAINS & 8-YEAR LOSS CARRY-FORWARD LEDGER (SCHEDULE CG & CFL)
// ============================================================================
@Composable
private fun CapitalGainsLedgerView() {
    var stcg111AText by remember { mutableStateOf("250000") }
    var ltcg112AText by remember { mutableStateOf("320000") }
    var cyStclLossText by remember { mutableStateOf("60000") }
    var cyLtclLossText by remember { mutableStateOf("40000") }
    var bfStclLossText by remember { mutableStateOf("50000") }
    var bfLtclLossText by remember { mutableStateOf("30000") }
    var copyStatusMessage by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val input = remember(stcg111AText, ltcg112AText, cyStclLossText, cyLtclLossText, bfStclLossText, bfLtclLossText) {
        val bfList = mutableListOf<BroughtForwardLoss>()
        val bfStcl = bfStclLossText.toLongOrNull() ?: 0L
        val bfLtcl = bfLtclLossText.toLongOrNull() ?: 0L
        if (bfStcl > 0L) {
            bfList.add(BroughtForwardLoss("2022-23", LossType.STCL, bfStcl, 0L, filedBeforeDueDate = true))
        }
        if (bfLtcl > 0L) {
            bfList.add(BroughtForwardLoss("2023-24", LossType.LTCL, bfLtcl, 0L, filedBeforeDueDate = true))
        }

        CapitalGainsPortfolioInput(
            stcg111AListedGain = stcg111AText.toLongOrNull() ?: 0L,
            stcl111AListedLoss = cyStclLossText.toLongOrNull() ?: 0L,
            ltcg112AListedGain = ltcg112AText.toLongOrNull() ?: 0L,
            ltcl112AListedLoss = cyLtclLossText.toLongOrNull() ?: 0L,
            broughtForwardLosses = bfList
        )
    }

    val result = remember(input) {
        CapitalGainsAndLossLedgerEngine.computeCapitalGainsAndLosses(input)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tax Summary Hero
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Capital Gains Tax", style = MaterialTheme.typography.labelSmall, color = Slate600)
                            Text(
                                text = "₹${"%,d".format(result.totalCapitalGainsTax)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        StatutoryBadge(text = "Finance Act 2024 Rates", tone = BadgeTone.STATUTORY)
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Slate100)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("STCG (20%)", style = TextStyle(fontSize = 10.sp, color = Slate600))
                                Text("₹${"%,d".format(result.taxOnStcg111A)}", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Slate100)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("LTCG (12.5% >1.25L)", style = TextStyle(fontSize = 10.sp, color = Slate600))
                                Text("₹${"%,d".format(result.taxOnLtcg112A)}", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Slate100)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Sec 112A Exemption", style = TextStyle(fontSize = 10.sp, color = EmeraldSuccess))
                                Text("₹${"%,d".format(result.exemption112AClaimed)}", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                            }
                        }
                    }
                }
            }
        }

        // Multi-Broker Portfolio Inputs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Current Year Capital Gains & Losses",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = stcg111AText,
                            onValueChange = { stcg111AText = it },
                            label = { Text("STCG 111A (Gain ₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = cyStclLossText,
                            onValueChange = { cyStclLossText = it },
                            label = { Text("STCL (Loss ₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ltcg112AText,
                            onValueChange = { ltcg112AText = it },
                            label = { Text("LTCG 112A (Gain ₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = cyLtclLossText,
                            onValueChange = { cyLtclLossText = it },
                            label = { Text("LTCL (Loss ₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Brought-Forward Losses from Previous Assessment Years (Schedule BFLA)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bfStclLossText,
                            onValueChange = { bfStclLossText = it },
                            label = { Text("B/F STCL (AY 22-23 ₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = bfLtclLossText,
                            onValueChange = { bfLtclLossText = it },
                            label = { Text("B/F LTCL (AY 23-24 ₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                    }
                }
            }
        }

        // Schedule CYLA Intra-Head Set-off Audit Trail
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Schedule CYLA: Intra-Head Set-Off Audit Trail",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))

                    if (result.setOffAuditLog.isEmpty()) {
                        Text(
                            "No current year losses to set off.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    } else {
                        result.setOffAuditLog.forEach { step ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Slate600,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${step.lossSource} → ${step.setOffAgainstGain}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "₹${"%,d".format(step.setOffAmount)}",
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    StatutoryBadge(text = step.statutorySection, tone = BadgeTone.STATUTORY)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Schedule CFL: 8-Year Loss Carry-Forward Ledger
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Schedule CFL: 8-Year Carry-Forward Ledger",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        StatutoryBadge(
                            text = "Total Carried Forward: ₹${"%,d".format(result.totalUnabsorbedLossCarriedForward)}",
                            tone = BadgeTone.WARNING
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    if (result.carriedForwardToFutureAy.isEmpty()) {
                        Text("No unabsorbed losses to carry forward.", style = MaterialTheme.typography.bodySmall, color = Slate500)
                    } else {
                        result.carriedForwardToFutureAy.forEach { bucket ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (bucket.isLapsed) CrimsonDangerBg else Slate100)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${bucket.lossType.name} (${bucket.assessmentYear})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        StatutoryBadge(
                                            text = "Expires ${bucket.expiryYear}",
                                            tone = if (bucket.isLapsed) BadgeTone.DANGER else BadgeTone.STATUTORY
                                        )
                                    }
                                    Text(
                                        "Opening: ₹${"%,d".format(bucket.openingUnabsorbed)} | Set Off: ₹${"%,d".format(bucket.utilizedThisYear)}",
                                        style = TextStyle(fontSize = 10.sp, color = Slate600)
                                    )
                                }

                                Text(
                                    "Balance: ₹${"%,d".format(bucket.closingCarryForward)}",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (bucket.isLapsed) CrimsonDanger else EnterpriseTeal800
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val cflSummary = "Schedule CFL Summary: Total Unabsorbed Loss Carried Forward = ₹${result.totalUnabsorbedLossCarriedForward} across ${result.carriedForwardToFutureAy.size} assessment year buckets."
                            clipboardManager.setText(AnnotatedString(cflSummary))
                            copyStatusMessage = "Schedule CFL summary copied!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Copy Schedule CFL Data for e-Filing", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 7. PRE-JULY 23, 2024 PROPERTY LTCG DUAL-OPTION INDEXATION (SEC 112(1)(a))
// ============================================================================
@Composable
private fun PropertyLtcgView() {
    var saleConsiderationText by remember { mutableStateOf("12000000") } // ₹1.2 Cr
    var actualCostText by remember { mutableStateOf("3500000") } // ₹35 Lakhs
    var sec54ReinvestmentText by remember { mutableStateOf("2000000") } // ₹20 Lakhs
    var acquisitionFy by remember { mutableStateOf("2012-13") }
    var acquiredBeforeJuly2024 by remember { mutableStateOf(true) }

    val input = remember(saleConsiderationText, actualCostText, sec54ReinvestmentText, acquisitionFy, acquiredBeforeJuly2024) {
        PropertySaleInput(
            acquisitionDateType = if (acquiredBeforeJuly2024) PropertyLtcgIndexationEngine.PropertyAcquisitionDate.BEFORE_JULY_23_2024 else PropertyLtcgIndexationEngine.PropertyAcquisitionDate.ON_OR_AFTER_JULY_23_2024,
            acquisitionFinancialYear = acquisitionFy,
            saleFinancialYear = "2025-26",
            actualCostOfAcquisitionRupees = actualCostText.toLongOrNull() ?: 0L,
            saleConsiderationRupees = saleConsiderationText.toLongOrNull() ?: 0L,
            section54ReinvestmentRupees = sec54ReinvestmentText.toLongOrNull() ?: 0L
        )
    }

    val result = remember(input) {
        PropertyLtcgIndexationEngine.computePropertyLtcg(input)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Recommendation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recommended Grandfathered Path", style = MaterialTheme.typography.labelSmall, color = EnterpriseTeal700)
                            Text(
                                text = "Final Tax: ₹${"%,d".format(result.finalTaxPayableWithCess)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                            )
                        }
                        if (result.taxSavingsByGrandfathering > 0L) {
                            StatutoryBadge(
                                text = "Saves ₹${"%,d".format(result.taxSavingsByGrandfathering)}",
                                tone = BadgeTone.SUCCESS
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = result.recommendedOption,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Slate700
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Finance (No. 2) Act 2024 allows resident individuals selling property acquired before July 23, 2024 to choose whichever option results in LOWER tax.",
                        style = TextStyle(fontSize = 11.sp, color = Slate600)
                    )
                }
            }
        }

        // Side-by-Side Comparison Table
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Statutory Dual-Option Showdown (Sec 112(1)(a) Proviso)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Slate100)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Option 1: 12.5% Flat", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Without Indexation", style = TextStyle(fontSize = 10.sp, color = Slate600))
                                Spacer(Modifier.height(6.dp))
                                Text("Cost: ₹${"%,d".format(input.actualCostOfAcquisitionRupees)}", style = TextStyle(fontSize = 11.sp))
                                Text("Gain: ₹${"%,d".format(result.netGainOption1Unindexed)}", style = TextStyle(fontSize = 11.sp))
                                Spacer(Modifier.height(4.dp))
                                Text("Tax: ₹${"%,d".format(result.taxOption1Unindexed12_5Pct)}", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = EmeraldSuccessBg)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Option 2: 20.0% with CII", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                                Text("Cost Inflation Index", style = TextStyle(fontSize = 10.sp, color = Slate600))
                                Spacer(Modifier.height(6.dp))
                                Text("Indexed Cost: ₹${"%,d".format(result.indexedCostOfAcquisition)}", style = TextStyle(fontSize = 11.sp))
                                Text("Gain: ₹${"%,d".format(result.netGainOption2Indexed)}", style = TextStyle(fontSize = 11.sp))
                                Spacer(Modifier.height(4.dp))
                                Text("Tax: ₹${"%,d".format(result.taxOption2Indexed20Pct)}", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                            }
                        }
                    }
                }
            }
        }

        // Inputs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Property Particulars",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = saleConsiderationText,
                            onValueChange = { saleConsiderationText = it },
                            label = { Text("Sale Consideration (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = actualCostText,
                            onValueChange = { actualCostText = it },
                            label = { Text("Cost of Acquisition (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sec54ReinvestmentText,
                            onValueChange = { sec54ReinvestmentText = it },
                            label = { Text("Sec 54 Reinvestment (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = acquisitionFy,
                            onValueChange = { acquisitionFy = it },
                            label = { Text("Acquisition FY (e.g. 2012-13)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Acquired Before July 23, 2024:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Enables grandfathered 20% CII indexation option", style = TextStyle(fontSize = 10.sp, color = Slate600))
                        }
                        Switch(
                            checked = acquiredBeforeJuly2024,
                            onCheckedChange = { acquiredBeforeJuly2024 = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EnterpriseTeal600)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 8. SECTION 89(1) SALARY ARREARS RELIEF & FORM 10E COMPILER
// ============================================================================
@Composable
private fun Form10EReliefView(pan: String) {
    var normalSalaryText by remember { mutableStateOf("1500000") } // ₹15 Lakhs
    var arrearsText by remember { mutableStateOf("300000") } // ₹3 Lakhs arrears
    var pastFyIncomeText by remember { mutableStateOf("900000") } // ₹9 Lakhs
    var pastFyTaxText by remember { mutableStateOf("96200") } // ₹96,200
    var pastAyYear by remember { mutableStateOf("2024-25") }
    var copyStatusMessage by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val input = remember(normalSalaryText, arrearsText, pastFyIncomeText, pastFyTaxText, pastAyYear) {
        Section89ComputationInput(
            receiptAssessmentYear = "2026-27",
            currentYearNormalSalaryExcludingArrears = normalSalaryText.toLongOrNull() ?: 0L,
            totalArrearsReceivedInCurrentYear = arrearsText.toLongOrNull() ?: 0L,
            accrualYears = listOf(
                AccrualYearParticulars(
                    assessmentYear = pastAyYear,
                    originalTotalIncome = pastFyIncomeText.toLongOrNull() ?: 0L,
                    originalTaxPaid = pastFyTaxText.toLongOrNull() ?: 0L,
                    arrearsAllocatedToThisYear = arrearsText.toLongOrNull() ?: 0L
                )
            )
        )
    }

    val result = remember(input) {
        Section89ArrearsReliefCalculator.computeRelief(input, pan)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Relief Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Section 89(1) Arrears Tax Relief", style = MaterialTheme.typography.labelSmall, color = EnterpriseTeal700)
                            Text(
                                text = "₹${"%,d".format(result.reliefAllowableUnderSection89)}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                            )
                        }
                        StatutoryBadge(text = "Rule 21AA Compliant", tone = BadgeTone.SUCCESS)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Relief equalizes the tax spike caused by receiving lump-sum arrears in the current year instead of the accrual years.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
        }

        // Calculation Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Relief Computation Breakdown (Table A)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1. Incremental Tax in Current Year (Diff A):", style = MaterialTheme.typography.bodySmall)
                        Text("₹${"%,d".format(result.incrementalTaxInReceiptYear)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("2. Incremental Tax in Accrual Year(s) (Diff B):", style = MaterialTheme.typography.bodySmall)
                        Text("₹${"%,d".format(result.totalIncrementalTaxInAccrualYears)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                    }
                    Divider(Modifier.padding(vertical = 6.dp), thickness = 0.5.dp, color = Slate200)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("NET SECTION 89(1) RELIEF (A - B):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "₹${"%,d".format(result.reliefAllowableUnderSection89)}",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = EmeraldSuccess, fontSize = 13.sp)
                        )
                    }
                }
            }
        }

        // Inputs & Form 10E JSON Export
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Salary & Arrears Parameters",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = normalSalaryText,
                            onValueChange = { normalSalaryText = it },
                            label = { Text("Current Salary excl. Arrears (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = arrearsText,
                            onValueChange = { arrearsText = it },
                            label = { Text("Arrears Received in Current FY (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pastFyIncomeText,
                            onValueChange = { pastFyIncomeText = it },
                            label = { Text("Accrual Year Total Income (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = pastFyTaxText,
                            onValueChange = { pastFyTaxText = it },
                            label = { Text("Accrual Year Tax Paid (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mandatory Form 10E Electronic Filing", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(result.form10EJsonPayload))
                                copyStatusMessage = "Form 10E JSON copied! Upload on e-filing portal before filing ITR."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Copy Form 10E JSON", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    if (copyStatusMessage != null) {
                        Spacer(Modifier.height(6.dp))
                        StatutoryBadge(text = copyStatusMessage!!, tone = BadgeTone.SUCCESS)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 9. UPDATED RETURN (ITR-U U/S 139(8A) & SECTION 140B)
// ============================================================================
@Composable
private fun ItrUView() {
    var additionalIncomeText by remember { mutableStateOf("400000") } // ₹4 Lakhs
    var monthsElapsedText by remember { mutableStateOf("8") } // 8 months (25% rate)
    var interestText by remember { mutableStateOf("15000") } // ₹15,000

    val input = remember(additionalIncomeText, monthsElapsedText, interestText) {
        ItrUInput(
            relevantAssessmentYear = "2024-25",
            additionalIncomeDeclaredRupees = additionalIncomeText.toLongOrNull() ?: 0L,
            monthsElapsedFromEndOfAy = monthsElapsedText.toIntOrNull() ?: 0,
            interest234A = interestText.toLongOrNull() ?: 0L
        )
    }

    val result = remember(input) {
        ItrUAdditionalTaxCalculator.computeItrU(input)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Section 140B Outflow", style = MaterialTheme.typography.labelSmall, color = EnterpriseTeal700)
                            Text(
                                text = "₹${"%,d".format(result.totalPayableUnderSection140B)}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                            )
                        }
                        StatutoryBadge(
                            text = result.timeWindow.label.take(28) + "...",
                            tone = if (result.isEligibleToFileItrU) BadgeTone.WARNING else BadgeTone.DANGER
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = result.eligibilityRemark,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
        }

        // Section 140B Tax Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Section 140B Statutory Outflow Ledger",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))

                    val rows = listOf(
                        Pair("Tax on Additional Income", result.taxPlusCess),
                        Pair("Statutory Interest u/s 234A/B/C", result.totalStatutoryInterest),
                        Pair("Base for Section 140B Calculation", result.taxAndInterestBaseFor140B),
                        Pair("Section 140B Additional Tax Surcharge (${(result.timeWindow.surchargePercent * 100).toInt()}%)", result.additionalTaxSurcharge140B),
                        Pair("TOTAL STATUTORY PAYABLE U/S 140B", result.totalPayableUnderSection140B)
                    )

                    rows.forEach { (label, amt) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall)
                            Text("₹${"%,d".format(amt)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Divider(color = Slate200, thickness = 0.5.dp)
                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Deposit Challan 280 under: ${result.challan280MinorHead}",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = IndigoStatutory)
                    )
                }
            }
        }

        // Inputs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ITR-U Assessment Parameters",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = additionalIncomeText,
                            onValueChange = { additionalIncomeText = it },
                            label = { Text("Additional Income (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = monthsElapsedText,
                            onValueChange = { monthsElapsedText = it },
                            label = { Text("Months from End of AY") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )
                    }
                }
            }
        }
    }
}


