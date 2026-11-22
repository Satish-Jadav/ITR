package `in`.zerotax.app.ui.business

import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.taxengine.advanced.Form10IeaBusinessRegimeEngine
import `in`.zerotax.core.taxengine.advanced.Form10IeaBusinessRegimeEngine.Form10IeaFilingReason
import `in`.zerotax.core.taxengine.advanced.Form10IeaBusinessRegimeEngine.Form10IeaInput
import `in`.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine
import `in`.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.BusinessType
import `in`.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.BusinessFinancialsInput
import `in`.zerotax.core.taxengine.advanced.SmallBusinessAuditAndPresumptiveEngine.AuditVerdict

enum class AuditEntryStatus(val label: String, val badgeTone: BadgeTone) {
    VERIFIED_BUSINESS("Verified Business Expense", BadgeTone.SUCCESS),
    UNIDENTIFIED("Needs Client Clarification", BadgeTone.WARNING),
    PERSONAL_DRAWINGS("Classified: Personal Drawings", BadgeTone.STATUTORY),
    DISALLOWED_40A3("Disallowed u/s 40A(3) (>₹10k Cash)", BadgeTone.DANGER),
    DISALLOWED_43BH("Disallowed u/s 43B(h) (MSME Overdue)", BadgeTone.DANGER)
}

data class AuditLedgerEntry(
    val id: String,
    val date: String,
    val particulars: String,
    val amountRupees: Long,
    val isDebit: Boolean,
    val initialCategory: String,
    val status: AuditEntryStatus = AuditEntryStatus.UNIDENTIFIED,
    val clientNote: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallBusinessAuditSuiteScreen(
    pan: String,
    caName: String = "CA Rajesh Sharma, FCA",
    caMembership: String = "124856",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler(onBack = onBack)

    var selectedBusinessType by remember { mutableStateOf(BusinessType.ELIGIBLE_BUSINESS_44AD) }
    var turnoverText by remember { mutableStateOf("") } // ₹1.5 Cr
    var digitalTurnoverText by remember { mutableStateOf("") } // ₹1.45 Cr
    var cashTurnoverText by remember { mutableStateOf("") } // ₹5 Lakhs
    var declaredProfitText by remember { mutableStateOf("") } // ₹10.5 Lakhs (7%)
    var totalPaymentsText by remember { mutableStateOf("") }
    var cashPaymentsText by remember { mutableStateOf("") }
    var optedOutInPast5Years by remember { mutableStateOf(false) }

    // Form 3CD Disallowances
    var msmeOverdueText by remember { mutableStateOf("0") }
    var cashOver10kText by remember { mutableStateOf("0") }
    var expensesWithoutTdsText by remember { mutableStateOf("0") }
    var unpaidStatutoryDuesText by remember { mutableStateOf("0") }

    // Interactive Transaction Audit & Reconciliation Desk
    var showFullAuditReportDialog by remember { mutableStateOf(false) }
    var selectedEntryForClarification by remember { mutableStateOf<AuditLedgerEntry?>(null) }
    var clarificationNoteInput by remember { mutableStateOf("") }

    var auditEntries by remember {
        mutableStateOf(
            listOf(
                AuditLedgerEntry("TXN-01", "12-Apr-2025", "Client Direct RTGS Settlement", 450000L, false, "Digital Revenue", AuditEntryStatus.VERIFIED_BUSINESS),
                AuditLedgerEntry("TXN-02", "18-May-2025", "GST Registered Raw Materials Supplier", 185000L, true, "Vendor COGS", AuditEntryStatus.VERIFIED_BUSINESS),
                AuditLedgerEntry("TXN-03", "04-Jun-2025", "NEFT-7819 Unidentified Payment", 45000L, true, "Unexplained Debit", AuditEntryStatus.UNIDENTIFIED),
                AuditLedgerEntry("TXN-04", "15-Jul-2025", "Cash Withdrawal - Self / Counter", 25000L, true, "Cash Withdrawal", AuditEntryStatus.UNIDENTIFIED),
                AuditLedgerEntry("TXN-05", "22-Aug-2025", "Subcontractor Labour Charges", 30000L, true, "Contractor", AuditEntryStatus.VERIFIED_BUSINESS),
                AuditLedgerEntry("TXN-06", "10-Sep-2025", "MSME Vendor Overdue >45 Days", 60000L, true, "Vendor Outstanding", AuditEntryStatus.DISALLOWED_43BH, "Clause 22 MSME delay")
            )
        )
    }

    // Form 10-IEA Regime Governance
    var chosenRegimeIsOld by remember { mutableStateOf(false) }
    var hasOptedBackInLifetimeBefore by remember { mutableStateOf(false) }

    var copyStatusMessage by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val input = remember(
        selectedBusinessType, turnoverText, digitalTurnoverText, cashTurnoverText,
        declaredProfitText, totalPaymentsText, cashPaymentsText, optedOutInPast5Years,
        msmeOverdueText, cashOver10kText, expensesWithoutTdsText, unpaidStatutoryDuesText
    ) {
        val turnover = turnoverText.toLongOrNull() ?: 0L
        val digital = digitalTurnoverText.toLongOrNull() ?: 0L
        val cash = cashTurnoverText.toLongOrNull() ?: 0L

        BusinessFinancialsInput(
            businessType = selectedBusinessType,
            totalTurnoverRupees = turnover,
            digitalTurnoverRupees = digital,
            cashTurnoverRupees = cash,
            totalPaymentsRupees = totalPaymentsText.toLongOrNull() ?: 0L,
            cashPaymentsRupees = cashPaymentsText.toLongOrNull() ?: 0L,
            declaredNetProfitRupees = declaredProfitText.toLongOrNull() ?: 0L,
            optedOut44ADInPast5Years = optedOutInPast5Years,
            msmeOverdueUnpaid43Bh = msmeOverdueText.toLongOrNull() ?: 0L,
            cashExpensesOver10k40A3 = cashOver10kText.toLongOrNull() ?: 0L,
            expensesWithoutTds40aIa = expensesWithoutTdsText.toLongOrNull() ?: 0L,
            unpaidStatutoryDues43B = unpaidStatutoryDuesText.toLongOrNull() ?: 0L
        )
    }

    val report = remember(input) {
        SmallBusinessAuditAndPresumptiveEngine.evaluateBusinessAudit(input)
    }

    val form10IeaDecision = remember(chosenRegimeIsOld, hasOptedBackInLifetimeBefore, report.isTaxAuditMandatory) {
        Form10IeaBusinessRegimeEngine.processForm10Iea(
            hasBusinessOrProfessionIncome = true,
            chosenRegimeIsOld = chosenRegimeIsOld,
            input = Form10IeaInput(
                pan = pan,
                taxpayerName = pan.takeIf { it.isNotBlank() } ?: "Authorized Assessee",
                filingReason = Form10IeaFilingReason.OPT_OUT_OF_NEW_REGIME,
                hasOptedBackInLifetimeBefore = hasOptedBackInLifetimeBefore
            ),
            requiresTaxAudit = report.isTaxAuditMandatory
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ZeroTaxTopAppBar(
                title = "Small Business Tax Audit",
                subtitle = "Presumptive Limits & Form 3CD Auditor",
                pan = pan,
                onBack = onBack,
                badgeText = "Sec 44AB & 44AD",
                badgeTone = BadgeTone.STATUTORY
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Business Type Selector Tabs (Horizontally scrollable for small screens)
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    listOf(
                        Triple(BusinessType.ELIGIBLE_BUSINESS_44AD, "Sec 44AD Business", "Trader / Shop / Contractor"),
                        Triple(BusinessType.SPECIFIED_PROFESSION_44ADA, "Sec 44ADA Profession", "Doctor / Freelancer / CA"),
                        Triple(BusinessType.OTHER_REGULAR_BUSINESS, "Regular Business", "Turnover > ₹1 Cr / ₹10 Cr")
                    )
                ) { (type, title, subtitle) ->
                    val isSelected = selectedBusinessType == type
                    val isDark = isSystemInDarkTheme()
                    Box(
                        modifier = Modifier
                            .widthIn(min = 140.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) {
                                    if (isDark) EnterpriseTeal900.copy(alpha = 0.35f) else EnterpriseTeal50
                                } else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) EnterpriseTeal600 else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                selectedBusinessType = type
                                copyStatusMessage = null
                            }
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        if (isDark) EnterpriseTeal300 else EnterpriseTeal900
                                    } else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = subtitle,
                                style = TextStyle(fontSize = 10.sp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Slate500)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Statutory Audit Verdict Hero Card
                item {
                    val isMandatory = report.isTaxAuditMandatory
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMandatory) getAmberWarningBg() else getEmeraldSuccessBg()
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isMandatory) AmberWarning else EmeraldSuccess
                            )
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
                                        imageVector = if (isMandatory) Icons.Default.Warning else Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (isMandatory) AmberWarning else EmeraldSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isMandatory) "MANDATORY TAX AUDIT REQUIRED" else "ZERO AUDIT & ZERO BOOKS REQUIRED",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMandatory) AmberWarning else EmeraldSuccess
                                        )
                                    )
                                }
                                StatutoryBadge(
                                    text = "Form: ${report.applicableItrForm}",
                                    tone = if (isMandatory) BadgeTone.WARNING else BadgeTone.SUCCESS
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = report.statutoryAuditReason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isMandatory) {
                                Spacer(Modifier.height(10.dp))
                                HorizontalDivider(color = AmberWarning.copy(alpha = 0.3f), thickness = 0.5.dp)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Audit Due Date (Form 3CD):", style = TextStyle(fontSize = 10.sp, color = Slate600))
                                        Text(report.statutoryAuditDueDate, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = CrimsonDanger))
                                    }
                                    Column {
                                        Text("ITR Filing Due Date:", style = TextStyle(fontSize = 10.sp, color = Slate600))
                                        Text(report.itrDueDateWithAudit, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Column {
                                        Text("Sec 271B Penalty Shield:", style = TextStyle(fontSize = 10.sp, color = Slate600))
                                        Text("Up to ₹${"%,d".format(report.potentialSection271BPenalty)}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = CrimsonDanger))
                                    }
                                }
                            }
                        }
                    }
                }

                // Turnover, Banking vs Cash & 5% Test
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
                                    text = "Turnover & Digital vs Cash Breakdown",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                val cashPct = input.cashReceiptPercentage
                                StatutoryBadge(
                                    text = "Cash Receipts: ${"%.1f".format(cashPct)}% ${if (cashPct <= 5.0) "(Passes 5% Rule)" else "(Exceeds 5%)"}",
                                    tone = if (cashPct <= 5.0) BadgeTone.SUCCESS else BadgeTone.WARNING
                                )
                            }
                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = turnoverText,
                                onValueChange = { turnoverText = it },
                                label = { Text("Total Gross Turnover / Receipts (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            )

                            Spacer(Modifier.height(8.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = digitalTurnoverText,
                                    onValueChange = { digitalTurnoverText = it },
                                    label = { Text("Digital (UPI/Bank ₹)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                )
                                OutlinedTextField(
                                    value = cashTurnoverText,
                                    onValueChange = { cashTurnoverText = it },
                                    label = { Text("Cash Receipts (₹)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                )
                                OutlinedTextField(
                                    value = declaredProfitText,
                                    onValueChange = { declaredProfitText = it },
                                    label = { Text("Declared Net Profit (₹)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                )
                                OutlinedTextField(
                                    value = totalPaymentsText,
                                    onValueChange = { totalPaymentsText = it },
                                    label = { Text("Total Payments / Expenses (₹)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                )
                            }

                            if (selectedBusinessType == BusinessType.ELIGIBLE_BUSINESS_44AD) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Section 44AD(4) 5-Year Lock-In:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("Did you opt out of 44AD in any of the last 5 assessment years?", style = TextStyle(fontSize = 11.sp, color = Slate600))
                                    }
                                    Switch(
                                        checked = optedOutInPast5Years,
                                        onCheckedChange = { optedOutInPast5Years = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = CrimsonDanger)
                                    )
                                }
                            }
                        }
                    }
                }

                // Form 3CD Statutory Disallowances Scanner (MSME 43B(h), 40A(3), 40(a)(ia))
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
                                    text = "Form 3CD Statutory Disallowances Scanner",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                StatutoryBadge(
                                    text = "Addbacks: ₹${"%,d".format(report.disallowances.totalStatutoryAddbacks)}",
                                    tone = if (report.disallowances.totalStatutoryAddbacks > 0L) BadgeTone.WARNING else BadgeTone.SUCCESS
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Under Tax Audit, these unallowed deductions are added back directly to your business profit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = msmeOverdueText,
                                onValueChange = { msmeOverdueText = it },
                                label = { Text("Clause 22 / Sec 43B(h): MSME Overdue Dues >45 Days (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                            Spacer(Modifier.height(8.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cashOver10kText,
                                    onValueChange = { cashOver10kText = it },
                                    label = { Text("Sec 40A(3): Cash >₹10k/day (₹)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                )
                                OutlinedTextField(
                                    value = expensesWithoutTdsText,
                                    onValueChange = { expensesWithoutTdsText = it },
                                    label = { Text("Sec 40(a)(ia): Missing TDS (₹)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = unpaidStatutoryDuesText,
                                onValueChange = { unpaidStatutoryDuesText = it },
                                label = { Text("Clause 26 / Sec 43B: Unpaid GST, PF, ESIC Dues (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                        }
                    }
                }

                // Interactive Transaction Audit & Unidentified Entries Reconciler Desk
                item {
                    val unidentifiedCount = auditEntries.count { it.status == AuditEntryStatus.UNIDENTIFIED }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 0.5.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(if (unidentifiedCount > 0) AmberWarning else EnterpriseTeal600)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Transaction Audit & Entry Reconciler",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Audits every entry. Ambiguous items require client clarification.",
                                        style = TextStyle(fontSize = 10.sp, color = Slate600)
                                    )
                                }
                                StatutoryBadge(
                                    text = if (unidentifiedCount > 0) "$unidentifiedCount Unidentified" else "100% Reconciled",
                                    tone = if (unidentifiedCount > 0) BadgeTone.WARNING else BadgeTone.SUCCESS
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            auditEntries.forEach { entry ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (entry.status == AuditEntryStatus.UNIDENTIFIED) {
                                        if (isSystemInDarkTheme()) AmberWarning.copy(alpha = 0.12f) else AmberWarningBg
                                    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (entry.status == AuditEntryStatus.UNIDENTIFIED) AmberWarning.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = entry.particulars,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = "${entry.date} • ${if (entry.isDebit) "Debit (Expense)" else "Credit (Receipt)"} • ${entry.initialCategory}",
                                                    style = TextStyle(fontSize = 10.sp, color = Slate600)
                                                )
                                            }
                                            Text(
                                                text = "${if (entry.isDebit) "-" else "+"}₹${"%,d".format(entry.amountRupees)}",
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (entry.isDebit) MaterialTheme.colorScheme.onSurface else EmeraldSuccess
                                                )
                                            )
                                        }

                                        Spacer(Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            StatutoryBadge(text = entry.status.label, tone = entry.status.badgeTone)
                                            Button(
                                                onClick = {
                                                    selectedEntryForClarification = entry
                                                    clarificationNoteInput = entry.clientNote
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (entry.status == AuditEntryStatus.UNIDENTIFIED) AmberWarning else EnterpriseTeal600
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(
                                                    text = if (entry.status == AuditEntryStatus.UNIDENTIFIED) "Clarify Entry" else "Adjust",
                                                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                                                )
                                            }
                                        }

                                        if (entry.clientNote.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "Client Note: \"${entry.clientNote}\"",
                                                style = TextStyle(fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Slate600)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Perfect Statutory Audit Report Ready for User Review Hero Card
                item {
                    val unidentifiedCount = auditEntries.count { it.status == AuditEntryStatus.UNIDENTIFIED }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess)
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
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Audited Tax Report Ready",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess
                                        )
                                    )
                                }
                                StatutoryBadge(text = "Form 3CD Verified", tone = BadgeTone.SUCCESS)
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "All books and vouchers reconciled with attached Chartered Accountant. Disallowances adjusted u/s 40A(3), 40(a)(ia), and 43B(h).",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )

                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Attached Auditor:", style = TextStyle(fontSize = 11.sp, color = Slate600))
                                        Text("$caName (M.No: $caMembership)", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Reconciliation Status:", style = TextStyle(fontSize = 11.sp, color = Slate600))
                                        Text(if (unidentifiedCount == 0) "100% Entries Reconciled" else "$unidentifiedCount Entries Pending Explanation", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (unidentifiedCount == 0) EmeraldSuccess else AmberWarning))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Statutory Add-backs (Form 3CD):", style = TextStyle(fontSize = 11.sp, color = Slate600))
                                        Text("₹${"%,d".format(report.disallowances.totalStatutoryAddbacks)}", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Net Audited Business Income:", style = TextStyle(fontSize = 11.sp, color = Slate600))
                                        Text("₹${"%,d".format(report.effectiveBusinessNetProfit)}", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { showFullAuditReportDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Review Perfect Audit Report & Sign-Off", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                }
                            }
                        }
                    }
                }

                // CA Handover Package & 1-Tap Dossier Export
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
                                    text = "Chartered Accountant Pre-Audit Dossier",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(report.caHandoverDossierMarkdown))
                                        copyStatusMessage = "CA Audit Pack copied! Share with your Chartered Accountant."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Copy CA Pack", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                    }
                                }
                            }

                            if (copyStatusMessage != null) {
                                Spacer(Modifier.height(6.dp))
                                StatutoryBadge(text = copyStatusMessage!!, tone = BadgeTone.SUCCESS)
                            }

                            Spacer(Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = report.caHandoverDossierMarkdown,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                // Form 10-IEA Business Regime Governance Card
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
                                    text = "Form 10-IEA: Business Tax Regime Governance",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                StatutoryBadge(
                                    text = "Sec 115BAC(3)",
                                    tone = if (chosenRegimeIsOld) BadgeTone.WARNING else BadgeTone.SUCCESS
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Under Finance Act 2023/2024, New Regime is default. Taxpayers with business income wishing to opt out into Old Regime MUST file Form 10-IEA on or before ${form10IeaDecision.statutoryFilingDeadline}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Opt Out of New Regime into Old Regime:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("Requires electronic submission of Form 10-IEA", style = TextStyle(fontSize = 10.sp, color = Slate600))
                                }
                                Switch(
                                    checked = chosenRegimeIsOld,
                                    onCheckedChange = { chosenRegimeIsOld = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AmberWarning)
                                )
                            }

                            if (chosenRegimeIsOld) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("1-Time Lifetime Lock-In Check:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("Have you ever switched back to New Regime previously?", style = TextStyle(fontSize = 10.sp, color = CrimsonDanger))
                                    }
                                    Switch(
                                        checked = hasOptedBackInLifetimeBefore,
                                        onCheckedChange = { hasOptedBackInLifetimeBefore = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = CrimsonDanger)
                                    )
                                }

                                Spacer(Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (form10IeaDecision.isEligibleToOptOut) getAmberWarningBg() else getCrimsonDangerBg()
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = form10IeaDecision.lockInWarning,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                if (form10IeaDecision.isEligibleToOptOut) {
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(form10IeaDecision.cbdtJsonPayload))
                                            copyStatusMessage = "Form 10-IEA JSON copied! File on e-filing portal before ${form10IeaDecision.statutoryFilingDeadline}."
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Copy Form 10-IEA CBDT JSON Payload", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(88.dp))
                }
            }
        }
    }

    selectedEntryForClarification?.let { entry ->
        ZeroTaxBottomSheet(
            onDismissRequest = { selectedEntryForClarification = null }
        ) {
            Text(
                text = "Clarify & Adjust Business Entry",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${entry.particulars} • ₹${"%,d".format(entry.amountRupees)}",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Slate600)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = clarificationNoteInput,
                onValueChange = { clarificationNoteInput = it },
                label = { Text("What was this money given for?") },
                placeholder = { Text("e.g., Raw material advance, Personal loan, Machinery repair") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                textStyle = TextStyle(fontSize = 12.sp)
            )
            Spacer(Modifier.height(14.dp))
            Text("Select Classification & Statutory Treatment:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        auditEntries = auditEntries.map {
                            if (it.id == entry.id) it.copy(status = AuditEntryStatus.VERIFIED_BUSINESS, clientNote = clarificationNoteInput)
                            else it
                        }
                        selectedEntryForClarification = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("Approve as Allowable Business Expense", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        auditEntries = auditEntries.map {
                            if (it.id == entry.id) it.copy(status = AuditEntryStatus.PERSONAL_DRAWINGS, clientNote = clarificationNoteInput.ifBlank { "Personal Drawings / Non-business" })
                            else it
                        }
                        val currProf = declaredProfitText.toLongOrNull() ?: 0L
                        declaredProfitText = (currProf + entry.amountRupees).toString()
                        selectedEntryForClarification = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("Classify as Personal Drawings (Adjust Profit)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        auditEntries = auditEntries.map {
                            if (it.id == entry.id) it.copy(status = AuditEntryStatus.DISALLOWED_40A3, clientNote = clarificationNoteInput.ifBlank { "Cash > ₹10k disallowance u/s 40A(3)" })
                            else it
                        }
                        val currCash = cashOver10kText.toLongOrNull() ?: 0L
                        cashOver10kText = (currCash + entry.amountRupees).toString()
                        selectedEntryForClarification = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("Flag Disallowance u/s 40A(3) (Cash > ₹10k)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }

                Button(
                    onClick = {
                        auditEntries = auditEntries.map {
                            if (it.id == entry.id) it.copy(status = AuditEntryStatus.DISALLOWED_43BH, clientNote = clarificationNoteInput.ifBlank { "MSME Overdue > 45 Days" })
                            else it
                        }
                        val currMsme = msmeOverdueText.toLongOrNull() ?: 0L
                        msmeOverdueText = (currMsme + entry.amountRupees).toString()
                        selectedEntryForClarification = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("Flag Disallowance u/s 43B(h) (MSME Delay)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showFullAuditReportDialog) {
        ZeroTaxBottomSheet(
            onDismissRequest = { showFullAuditReportDialog = false }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Comprehensive Statutory Audit Report", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                IconButton(onClick = { showFullAuditReportDialog = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Text("Taxpayer PAN: $pan • Assessment Year 2026-27", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate600))
            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("1. AUDITOR PARTICULARS & CERTIFICATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal700))
                    Text("Chartered Accountant: $caName", style = TextStyle(fontSize = 11.sp))
                    Text("ICAI Membership No.: $caMembership", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Text("Audit Engagement: Section 44AB / Form 3CD & 3CB", style = TextStyle(fontSize = 11.sp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                    Text("2. FINANCIAL PARTICULARS & FORM 3CD SUMMARY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal700))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Turnover / Receipts:", style = TextStyle(fontSize = 11.sp, color = Slate600))
                        Text("₹${"%,d".format(input.totalTurnoverRupees)}", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Reported Net Profit:", style = TextStyle(fontSize = 11.sp, color = Slate600))
                        Text("₹${"%,d".format(input.declaredNetProfitRupees)}", style = TextStyle(fontSize = 11.sp))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sec 40A(3) Cash >₹10k Add-back:", style = TextStyle(fontSize = 11.sp, color = Slate600))
                        Text("₹${"%,d".format(report.disallowances.section40A3CashExpenseAddback)}", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sec 43B(h) MSME Delay Add-back:", style = TextStyle(fontSize = 11.sp, color = Slate600))
                        Text("₹${"%,d".format(report.disallowances.section43BhMsmeAddback)}", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CrimsonDanger))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Final Audited Taxable Business Income:", style = TextStyle(fontSize = 11.sp, color = Slate600))
                        Text("₹${"%,d".format(report.effectiveBusinessNetProfit)}", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) EnterpriseTeal900.copy(alpha = 0.3f) else EnterpriseTeal50),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Assisted Client Review Mode Active",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal700)
                    )
                    Text(
                        text = "This report has been reconciled for user and CA review. As per security rules, no return or audit form will be submitted to the ITD portal without explicit final authorization.",
                        style = TextStyle(fontSize = 10.sp, color = Slate600)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            ZeroTaxPrimaryButton(
                text = "Close & Retain Audited Figures",
                onClick = { showFullAuditReportDialog = false }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

