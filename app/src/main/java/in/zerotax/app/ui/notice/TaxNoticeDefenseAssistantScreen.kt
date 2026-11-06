package `in`.zerotax.app.ui.notice

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class NoticeCategory(
    val sectionTitle: String,
    val statutoryRef: String,
    val urgencyLevel: String,
    val statutoryDaysLimit: Int,
    val shortSummary: String
) {
    SEC_143_1_DEMAND(
        sectionTitle = "Section 143(1) Intimation & Tax Demand",
        statutoryRef = "Sec 143(1)(a) Income-tax Act",
        urgencyLevel = "HIGH",
        statutoryDaysLimit = 30,
        shortSummary = "CPC Bengaluru calculated tax demand or reduced refund due to TDS mismatch, disallowed 80C/80D, or interest u/s 234A/B/C."
    ),
    SEC_139_9_DEFECTIVE(
        sectionTitle = "Section 139(9) Defective Return Notice",
        statutoryRef = "Sec 139(9) Income-tax Act",
        urgencyLevel = "CRITICAL",
        statutoryDaysLimit = 15,
        shortSummary = "Return treated as defective due to missing schedules, gross mismatch between turnover and profit, or incomplete balance sheet."
    ),
    SEC_245_REFUND_ADJUSTMENT(
        sectionTitle = "Section 245 Refund Adjustment Notice",
        statutoryRef = "Sec 245 Income-tax Act",
        urgencyLevel = "MEDIUM",
        statutoryDaysLimit = 30,
        shortSummary = "CBDT proposing to adjust current year approved refund against an unverified or disputed past assessment year demand."
    ),
    SEC_148A_REOPENING_INQUIRY(
        sectionTitle = "Section 148A Inquiry for Reopening",
        statutoryRef = "Sec 148A(b) Finance Act 2021",
        urgencyLevel = "CRITICAL",
        statutoryDaysLimit = 14,
        shortSummary = "Assessing Officer seeking explanation regarding high-value cash deposits, property purchase, shares, or foreign remittance in AIS."
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxNoticeDefenseAssistantScreen(
    pan: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedNotice by remember { mutableStateOf(NoticeCategory.SEC_143_1_DEMAND) }
    var dinNumber by remember { mutableStateOf("CPC/2526/1431/98765432") }
    var demandAmountStr by remember { mutableStateOf("14250") }
    var selectedDisallowanceReason by remember { mutableStateOf("TDS Mismatch (Form 26AS credit not matched by employer deductor)") }
    var copyStatusMessage by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val responseMarkdown = remember(selectedNotice, dinNumber, demandAmountStr, selectedDisallowanceReason) {
        generateNoticeDefenseDraft(
            pan = pan,
            category = selectedNotice,
            dinNumber = dinNumber,
            demandAmount = demandAmountStr.toLongOrNull() ?: 0L,
            reason = selectedDisallowanceReason
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ZeroTaxTopAppBar(
                title = "Tax Notice Defense",
                subtitle = "CPC Defective 139(9), Intimation 143(1) & Demand Rectification",
                pan = pan,
                onBack = onBack,
                badgeText = "CPC Defense Shield",
                badgeTone = BadgeTone.WARNING
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Notice Category Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(NoticeCategory.entries) { category ->
                    val isSelected = selectedNotice == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) EnterpriseTeal50 else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) EnterpriseTeal600 else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                selectedNotice = category
                                copyStatusMessage = null
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = category.statutoryRef,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) EnterpriseTeal900 else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(Modifier.width(6.dp))
                                StatutoryBadge(
                                    text = "${category.statutoryDaysLimit}d Limit",
                                    tone = if (category.urgencyLevel == "CRITICAL") BadgeTone.DANGER else BadgeTone.WARNING
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Notice Overview Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) AmberWarningBgDark.copy(alpha = 0.6f) else AmberWarningBg.copy(alpha = 0.6f)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(AmberWarning.copy(alpha = 0.5f))
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedNotice.sectionTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                StatutoryBadge(
                                    text = "STATUTORY DEADLINE: ${selectedNotice.statutoryDaysLimit} DAYS",
                                    tone = BadgeTone.DANGER
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = selectedNotice.shortSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Safe Period:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "File rectification before statutory deadline to prevent automatic penalty interest u/s 220(2).",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSystemInDarkTheme()) CrimsonDangerDark else CrimsonDanger
                                )
                            }
                        }
                    }
                }

                // Interactive Notice Inputs
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
                                text = "Notice Particulars (From CPC Intimation PDF)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = dinNumber,
                                    onValueChange = { dinNumber = it },
                                    label = { Text("Notice DIN / Reference No") },
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                )
                                OutlinedTextField(
                                    value = demandAmountStr,
                                    onValueChange = { demandAmountStr = it },
                                    label = { Text("Demand (₹)") },
                                    modifier = Modifier.weight(0.8f),
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "Primary Reason for Mismatch / Disallowance:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(Modifier.height(6.dp))

                            val reasons = when (selectedNotice) {
                                NoticeCategory.SEC_143_1_DEMAND -> listOf(
                                    "TDS Mismatch (Form 26AS credit not matched by employer deductor)",
                                    "Section 80C / 80D deduction disallowed due to Schedule VI-A omission",
                                    "Interest u/s 234A/B/C erroneously computed despite full advance tax payment",
                                    "Section 87A rebate omitted for taxable income under ₹7 Lakhs"
                                )
                                NoticeCategory.SEC_139_9_DEFECTIVE -> listOf(
                                    "TDS claimed in ITR but corresponding income missing in schedules",
                                    "Tax audit report u/s 44AB not uploaded prior to filing",
                                    "Gross Turnover and Profit & Loss balance sheet mismatch",
                                    "Mandatory Schedule CG / F&O balance missing in ITR-3"
                                )
                                NoticeCategory.SEC_245_REFUND_ADJUSTMENT -> listOf(
                                    "Prior assessment year demand was already paid via Challan 280 (BSR code verified)",
                                    "Old demand is disputed and currently pending before CIT(Appeals)",
                                    "Arrear demand relates to clerical duplicate entry in CPC systems",
                                    "Past demand is time-barred under Section 149 limitation period"
                                )
                                NoticeCategory.SEC_148A_REOPENING_INQUIRY -> listOf(
                                    "High-value cash deposit originated from verified agricultural income / bank withdrawal",
                                    "Property purchase consideration paid fully via banking channels and disclosed in AIS",
                                    "Share trading turnover already reported in Schedule CG of original ITR",
                                    "Gift received from lineal ascendant relative exempt u/s 56(2)(x)"
                                )
                            }

                            reasons.forEach { reason ->
                                val isSelectedReason = selectedDisallowanceReason == reason
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelectedReason) EnterpriseTeal50 else Color.Transparent)
                                        .border(
                                            0.5.dp,
                                            if (isSelectedReason) EnterpriseTeal600 else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { selectedDisallowanceReason = reason }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelectedReason,
                                        onClick = { selectedDisallowanceReason = reason },
                                        colors = RadioButtonDefaults.colors(selectedColor = EnterpriseTeal600)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelectedReason) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 3-Step Action Plan
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
                                text = "Recommended Action Protocol (CBDT Standard)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(Modifier.height(8.dp))

                            val steps = when (selectedNotice) {
                                NoticeCategory.SEC_143_1_DEMAND -> listOf(
                                    "Step 1: Log in to e-filing portal -> Pending Actions -> 'Response to Outstanding Demand'.",
                                    "Step 2: Select 'Demand is incorrect' and specify 'Error in computation / TDS mismatch'.",
                                    "Step 3: Submit Section 154 Rectification Request online with Form 26AS annexure."
                                )
                                NoticeCategory.SEC_139_9_DEFECTIVE -> listOf(
                                    "Step 1: Go to e-filing portal -> Pending Actions -> 'e-Proceedings' -> Defective Return.",
                                    "Step 2: Correct the defective schedule XML/JSON in ZeroTax offline.",
                                    "Step 3: Upload the rectified JSON within 15 days under Section 139(9)."
                                )
                                NoticeCategory.SEC_245_REFUND_ADJUSTMENT -> listOf(
                                    "Step 1: Go to Pending Actions -> 'Response to Outstanding Demand'.",
                                    "Step 2: Select 'Demand is incorrect' -> Attach proof of previous payment (Challan 280 CIN).",
                                    "Step 3: Submit within 30 days to halt immediate adjustment against current refund."
                                )
                                NoticeCategory.SEC_148A_REOPENING_INQUIRY -> listOf(
                                    "Step 1: Access e-Proceedings -> Notice u/s 148A(b) -> View Notice DIN.",
                                    "Step 2: Prepare factual point-by-point reply enclosing relevant bank statement extracts.",
                                    "Step 3: Upload signed response letter in PDF format before Assessing Officer cutoff."
                                )
                            }

                            steps.forEach { stepText ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(stepText, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Formal Written Defense Generator
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
                                    text = "CBDT-Compliant Written Response Draft",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(responseMarkdown))
                                        copyStatusMessage = "Response copied to clipboard!"
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
                                        Text("Copy Response", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
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
                                    text = responseMarkdown,
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
            }
        }
    }
}

private fun generateNoticeDefenseDraft(
    pan: String,
    category: NoticeCategory,
    dinNumber: String,
    demandAmount: Long,
    reason: String
): String {
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    return """
FORMAL WRITTEN SUBMISSION UNDER THE INCOME-TAX ACT, 1961
=========================================================
Date of Submission: $today
To: The Assessing Officer / Centralized Processing Centre (CPC), Bengaluru
Taxpayer PAN: $pan
Statutory Notice Reference (DIN): $dinNumber
Notice Category: ${category.statutoryRef} (${category.sectionTitle})
Demand Amount in Dispute: ₹${"%,d".format(demandAmount)}

RESPECTED SIR/MADAM,

Sub: Formal Objection and Rectification Request in Response to Notice DIN $dinNumber

In response to the intimation/notice received under ${category.statutoryRef}, the assessee respectfully submits this formal objection on the following statutory grounds:

1. STATEMENT OF FACTS & DISPUTE:
The subject intimation has raised a demand of ₹${"%,d".format(demandAmount)} alleging discrepancy under:
"$reason".
It is submitted with utmost respect that the assessee has filed a true and complete return of income under Section 139, complying with all statutory disclosure norms.

2. STATUTORY SUBMISSIONS & MERITS:
(a) The apparent variance arises from timing or reporting mismatches between deductor filings and CPC automated reconciliation algorithms, rather than any failure of disclosure by the assessee.
(b) Under the established jurisprudence of the Hon'ble Supreme Court and CBDT Instruction No. 2/2014, credit for taxes actually deducted at source (TDS) cannot be denied to an employee/assessee merely due to clerical defaults by the withholding agent.
(c) The underlying transactions and deduction entitlements are fully evidenced by supporting statutory certificates, Form 16/16A, and banking records.

3. PRAYER & RELIEF SOUGHT:
In light of the above facts, it is prayed that:
(i) The proposed demand of ₹${"%,d".format(demandAmount)} be canceled / stayed pending rectification;
(ii) The return be processed under Section 154 without any prejudicial modification;
(iii) Any recovery proceedings or coercive action under Section 220 / Section 221 be withheld.

Enclosures:
1. Copy of Intimation DIN $dinNumber
2. Form 26AS / AIS reconciliation sheet
3. Bank statements and original deductor TDS certificates

Yours faithfully,  
Taxpayer PAN: $pan  
Assessee / Authorised Representative  
Submitted electronically via e-Filing Portal (e-Proceedings / Rectification Tab)
    """.trimIndent()
}

