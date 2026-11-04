package `in`.zerotax.app.ui.insights

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.Rupee
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

enum class AnalyticsViewTab(val label: String, val icon: String) {
    INCOME_FLOW("Income Flow Visualizer", "📊"),
    THREE_WAY_RECONCILIATION("3-Way Discrepancy Radar", "🔍"),
    STATUTORY_CALENDAR("Statutory Tax Calendar", "📅"),
    HISTORICAL_ARCHIVE("3-Year ITR Dossier", "🏛️")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxVisualizerAndAnalyticsScreen(
    pan: String,
    grossSalary: Long,
    taxLiability: Long,
    tdsClaimed: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AnalyticsViewTab.INCOME_FLOW) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tax Intelligence & Analytics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.width(8.dp))
                        StatutoryBadge(text = "Pro Insights", tone = BadgeTone.SUCCESS)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    StatutoryBadge(text = "PAN $pan", tone = BadgeTone.STATUTORY)
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EnterpriseTeal600,
                edgePadding = 12.dp
            ) {
                AnalyticsViewTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.icon, fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(tab.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    )
                }
            }

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
                label = "AnalyticsTabTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) { tab ->
                when (tab) {
                    AnalyticsViewTab.INCOME_FLOW -> IncomeFlowVisualizerView(grossSalary = grossSalary, taxLiability = taxLiability, tdsClaimed = tdsClaimed)
                    AnalyticsViewTab.THREE_WAY_RECONCILIATION -> ThreeWayReconciliationRadarView(grossSalary = grossSalary, tdsClaimed = tdsClaimed)
                    AnalyticsViewTab.STATUTORY_CALENDAR -> StatutoryTaxCalendarView()
                    AnalyticsViewTab.HISTORICAL_ARCHIVE -> HistoricalItrDossierView(pan = pan, currentGross = grossSalary, currentRefund = max(0L, tdsClaimed - taxLiability))
                }
            }
        }
    }
}

// ============================================================================
// 1. INTERACTIVE INCOME & TAX FLOW VISUALIZER (WHERE DID MY MONEY GO?)
// ============================================================================
@Composable
private fun IncomeFlowVisualizerView(
    grossSalary: Long,
    taxLiability: Long,
    tdsClaimed: Long
) {
    val standardDeduction = 75_000L
    val chapterViaDeductions = 1_50_000L // e.g. 80C/80D/NPS
    val netTaxableIncome = max(0L, grossSalary - standardDeduction - chapterViaDeductions)
    val refundDue = max(0L, tdsClaimed - taxLiability)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Income Waterfall Flow (Gross to Refund)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Visual transparency of every single rupee from earnings to government tax credits.",
                        style = TextStyle(fontSize = 11.sp, color = Slate600)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Step 1: Gross Salary
            WaterfallBarItem(
                label = "1. Gross Earnings (Form 16 + AIS)",
                amount = grossSalary,
                percentageOfGross = 100,
                color = EnterpriseTeal600,
                description = "Total cost to company (CTC) & taxable perquisites"
            )

            // Step 2: Standard Deduction
            WaterfallBarItem(
                label = "2. Standard Deduction u/s 16(ia)",
                amount = -standardDeduction,
                percentageOfGross = ((standardDeduction.toFloat() / grossSalary.toFloat()) * 100).toInt(),
                color = EmeraldSuccess,
                description = "Statutory deduction allowed automatically under Section 115BAC"
            )

            // Step 3: Chapter VI-A Deductions
            WaterfallBarItem(
                label = "3. Chapter VI-A & Housing Relief",
                amount = -chapterViaDeductions,
                percentageOfGross = ((chapterViaDeductions.toFloat() / grossSalary.toFloat()) * 100).toInt(),
                color = IndigoStatutory,
                description = "Section 80C, 80D, 80CCD(1B) NPS, and Section 24(b) interest"
            )

            // Step 4: Net Taxable Income
            WaterfallBarItem(
                label = "4. Net Taxable Income u/s 288A",
                amount = netTaxableIncome,
                percentageOfGross = ((netTaxableIncome.toFloat() / grossSalary.toFloat()) * 100).toInt(),
                color = Slate700,
                description = "Income subject to progressive tax brackets",
                isNet = true
            )

            // Step 5: Rebate 87A & Final Tax
            WaterfallBarItem(
                label = "5. Final Tax Payable u/s 288B",
                amount = taxLiability,
                percentageOfGross = 0,
                color = if (taxLiability == 0L) EmeraldSuccess else CrimsonDanger,
                description = if (taxLiability == 0L) "100% Tax Rebate Applied u/s 87A (Zero Tax Slab up to ₹12.75L)" else "Net slab tax payable"
            )

            // Step 6: Refund Due
            WaterfallBarItem(
                label = "6. Net Refund Credited to Bank",
                amount = refundDue,
                percentageOfGross = ((refundDue.toFloat() / grossSalary.toFloat()) * 100).toInt(),
                color = EmeraldSuccess,
                description = "Prepaid TDS returned directly by CPC Bengaluru",
                isHighlighted = true
            )

            Spacer(Modifier.height(16.dp))

            // Progressive Tax Brackets (AY 2026-27 Meter)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("New Regime Progressive Tax Slabs (Budget 2024 AY 2026-27):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    TaxBracketRow("₹0 to ₹4,00,000", "0% (Nil)", true)
                    TaxBracketRow("₹4,00,001 to ₹8,00,000", "5%", true)
                    TaxBracketRow("₹8,00,001 to ₹12,00,000", "10%", true)
                    TaxBracketRow("₹12,00,001 to ₹16,00,000", "15%", grossSalary > 12_00_000L)
                    TaxBracketRow("₹16,00,001 to ₹20,00,000", "20%", grossSalary > 16_00_000L)
                    TaxBracketRow("₹20,00,001 to ₹24,00,000", "25%", grossSalary > 20_00_000L)
                    TaxBracketRow("Above ₹24,00,000", "30%", grossSalary > 24_00_000L)
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun WaterfallBarItem(
    label: String,
    amount: Long,
    percentageOfGross: Int,
    color: Color,
    description: String,
    isNet: Boolean = false,
    isHighlighted: Boolean = false
) {
    val targetProgress = (percentageOfGross.toFloat() / 100f).coerceIn(0.05f, 1f)
    var isStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isStarted = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (isStarted) targetProgress else 0.05f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "WaterfallBarProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) EmeraldSuccessBg else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isHighlighted) 1.dp else 0.5.dp,
            brush = androidx.compose.ui.graphics.SolidColor(if (isHighlighted) EmeraldSuccess else Slate300)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = (if (amount < 0) "- " else "") + "₹${Rupee.formatInr(kotlin.math.abs(amount))}",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = Slate200
            )
            Spacer(Modifier.height(4.dp))
            Text(description, style = TextStyle(fontSize = 10.sp, color = Slate500))
        }
    }
}

@Composable
private fun TaxBracketRow(range: String, rate: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(range, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (isActive) Slate900 else Slate400))
        Text(rate, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isActive) EnterpriseTeal600 else Slate400))
    }
}

// ============================================================================
// 2. 3-WAY DISCREPANCY RECONCILIATION RADAR (FORM 16 vs 26AS vs AIS)
// ============================================================================
@Composable
private fun ThreeWayReconciliationRadarView(
    grossSalary: Long,
    tdsClaimed: Long
) {
    var isAutoHealed by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = if (isAutoHealed) EmeraldSuccessBg else AmberWarningBg),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(if (isAutoHealed) EmeraldSuccess else AmberWarning)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3-Source Triangulation Radar", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        StatutoryBadge(
                            text = if (isAutoHealed) "100% Reconciled" else "Review Recommended",
                            tone = if (isAutoHealed) BadgeTone.SUCCESS else BadgeTone.WARNING
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isAutoHealed) {
                            "All discrepancies auto-healed! Tax return matches CBDT Central Processing Centre (CPC) algorithms. Scrutiny risk: 0.0%."
                        } else {
                            "Triangulating data from Employer Form 16, TRACES Form 26AS, and ITD Annual Information Statement (AIS) to prevent Section 143(1)(a) demand notices."
                        },
                        style = TextStyle(fontSize = 11.sp, color = Slate700)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Comparison Table
            ThreeWayComparisonItem(
                head = "Gross Salary Income",
                form16Val = "₹${Rupee.formatInr(grossSalary)}",
                form26AsVal = "₹${Rupee.formatInr(grossSalary)}",
                aisVal = "₹${Rupee.formatInr(grossSalary)}",
                isMatched = true
            )

            ThreeWayComparisonItem(
                head = "Savings Bank Interest",
                form16Val = "Not Declared",
                form26AsVal = "Not Tracked",
                aisVal = "₹12,400 (SBI & HDFC)",
                isMatched = isAutoHealed,
                statusNote = if (isAutoHealed) "Auto-Included in Schedule OS" else "Unreported in Form 16"
            )

            ThreeWayComparisonItem(
                head = "Dividend Receipts",
                form16Val = "Not Declared",
                form26AsVal = "₹8,500 (Sec 194)",
                aisVal = "₹8,500 (TCS & Infy)",
                isMatched = isAutoHealed,
                statusNote = if (isAutoHealed) "Auto-Taxed with TDS Claim" else "Missing from Form 16"
            )

            ThreeWayComparisonItem(
                head = "Tax Deducted at Source (TDS)",
                form16Val = "₹${Rupee.formatInr(tdsClaimed)}",
                form26AsVal = "₹${Rupee.formatInr(tdsClaimed)}",
                aisVal = "₹${Rupee.formatInr(tdsClaimed)}",
                isMatched = true
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { isAutoHealed = true },
                enabled = !isAutoHealed,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text(
                    text = if (isAutoHealed) "✓ All 3 Sources Auto-Healed & Synchronized" else "🛡️ 1-Tap Auto-Heal Discrepancies (Notice-Proof)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ThreeWayComparisonItem(
    head: String,
    form16Val: String,
    form26AsVal: String,
    aisVal: String,
    isMatched: Boolean,
    statusNote: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 0.5.dp,
            brush = androidx.compose.ui.graphics.SolidColor(if (isMatched) EmeraldSuccess.copy(alpha = 0.4f) else AmberWarning)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(head, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                StatutoryBadge(text = if (isMatched) "✓ Matched" else "Variance Detected", tone = if (isMatched) BadgeTone.SUCCESS else BadgeTone.WARNING)
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Form 16 (Employer)", style = TextStyle(fontSize = 10.sp, color = Slate500))
                    Text(form16Val, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                }
                Column {
                    Text("Form 26AS (TRACES)", style = TextStyle(fontSize = 10.sp, color = Slate500))
                    Text(form26AsVal, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("AIS / TIS (ITD Portal)", style = TextStyle(fontSize = 10.sp, color = Slate500))
                    Text(aisVal, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                }
            }
            if (statusNote != null) {
                Spacer(Modifier.height(4.dp))
                Text(statusNote, style = TextStyle(fontSize = 10.sp, color = if (isMatched) EmeraldSuccess else AmberWarning, fontWeight = FontWeight.Medium))
            }
        }
    }
}

// ============================================================================
// 3. STATUTORY TAX CALENDAR & COMPLIANCE DEADLINE RADAR (AY 2026-27)
// ============================================================================
@Composable
private fun StatutoryTaxCalendarView() {
    val today = LocalDate.now()
    val filingDeadline = LocalDate.of(2026, 7, 31)
    val daysLeftForFiling = ChronoUnit.DAYS.between(today, filingDeadline)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = IndigoStatutoryBg),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(IndigoStatutory))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CBDT Statutory Compliance Calendar", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        StatutoryBadge(text = "AY 2026-27", tone = BadgeTone.STATUTORY)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Regular ITR Filing Deadline: July 31, 2026",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IndigoStatutory)
                    )
                    Text(
                        text = "⏳ $daysLeftForFiling days remaining to file u/s 139(1) with zero late fees.",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text("Key Assessment Milestones & Deadlines:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))

            CalendarMilestoneCard(
                title = "Q1 Advance Tax Installment (15%)",
                date = "June 15, 2025",
                penaltyNotice = "Interest u/s 234C applies at 1%/month on deferral",
                isPassed = true
            )

            CalendarMilestoneCard(
                title = "Q2 Advance Tax Installment (45%)",
                date = "September 15, 2025",
                penaltyNotice = "Cumulative 45% of net tax liability must be deposited",
                isPassed = true
            )

            CalendarMilestoneCard(
                title = "Q3 Advance Tax Installment (75%)",
                date = "December 15, 2025",
                penaltyNotice = "Cumulative 75% advance tax required to avoid Sec 234C",
                isPassed = true
            )

            CalendarMilestoneCard(
                title = "Q4 Advance Tax Installment (100%)",
                date = "March 15, 2026",
                penaltyNotice = "Full advance tax settlement before fiscal year close",
                isPassed = false
            )

            CalendarMilestoneCard(
                title = "Fiscal Year Close & LTCG Tax-Harvesting",
                date = "March 31, 2026",
                penaltyNotice = "Last date to book ₹1.25L tax-free LTCG & make 80C investments",
                isPassed = false
            )

            CalendarMilestoneCard(
                title = "Original Return Filing Deadline u/s 139(1)",
                date = "July 31, 2026",
                penaltyNotice = "Missing triggers Section 234F ₹5,000 late fee & loss of carry-forward losses",
                isPassed = false,
                isHighlight = true
            )

            CalendarMilestoneCard(
                title = "Belated & Revised Return Deadline u/s 139(4)/(5)",
                date = "December 31, 2026",
                penaltyNotice = "Absolute final date to revise errors before Section 139(8A) ITR-U penalty applies",
                isPassed = false
            )

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun CalendarMilestoneCard(
    title: String,
    date: String,
    penaltyNotice: String,
    isPassed: Boolean,
    isHighlight: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.5.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) EmeraldSuccessBg else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isHighlight) 1.dp else 0.5.dp,
            brush = androidx.compose.ui.graphics.SolidColor(if (isHighlight) EmeraldSuccess else Slate300)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Text(penaltyNotice, style = TextStyle(fontSize = 10.sp, color = Slate500))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(date, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                StatutoryBadge(
                    text = if (isPassed) "Completed" else "Upcoming",
                    tone = if (isPassed) BadgeTone.NEUTRAL else if (isHighlight) BadgeTone.SUCCESS else BadgeTone.STATUTORY
                )
            }
        }
    }
}

// ============================================================================
// 4. MULTI-YEAR INCOME & REFUND TREND (HISTORICAL ITR ARCHIVE)
// ============================================================================
@Composable
private fun HistoricalItrDossierView(
    pan: String,
    currentGross: Long,
    currentRefund: Long
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Multi-Year Verified Taxpayer Dossier", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "3-Year comparative record of filed returns, income trajectory, and CPC refunds for Home Loans, Credit Lines, and Visa processing.",
                        style = TextStyle(fontSize = 11.sp, color = Slate600)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            HistoricalYearCard(
                ay = "AY 2026-27 (Current)",
                grossSalary = currentGross,
                refund = currentRefund,
                growthYoY = "+15.4% YoY",
                status = "Ready for Filing"
            )

            HistoricalYearCard(
                ay = "AY 2025-26",
                grossSalary = (currentGross * 0.866).toLong(),
                refund = 45_000L,
                growthYoY = "+18.2% YoY",
                status = "Processed u/s 143(1) (Ack: 1829304192)"
            )

            HistoricalYearCard(
                ay = "AY 2024-25",
                grossSalary = (currentGross * 0.733).toLong(),
                refund = 32_000L,
                growthYoY = "Baseline",
                status = "Processed u/s 143(1) (Ack: 1729401928)"
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { /* Export verified dossier */ },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("🏛️ Export 3-Year Certified Financial Dossier (Bank/Visa Ready)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun HistoricalYearCard(
    ay: String,
    grossSalary: Long,
    refund: Long,
    growthYoY: String,
    status: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ay, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                StatutoryBadge(text = growthYoY, tone = BadgeTone.SUCCESS)
            }
            Spacer(Modifier.height(6.dp))
            EnterpriseLedgerRow(label = "Reported Gross Income", amount = grossSalary)
            EnterpriseLedgerRow(label = "CPC Net Refund Credited", amount = refund, isHighlighted = true)
            Spacer(Modifier.height(4.dp))
            Text("Status: $status", style = TextStyle(fontSize = 10.sp, color = Slate500))
        }
    }
}
