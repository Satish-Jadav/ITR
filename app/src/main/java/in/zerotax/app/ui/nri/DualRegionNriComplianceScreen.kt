package `in`.zerotax.app.ui.nri

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
import `in`.zerotax.core.taxengine.advanced.Form10IeaBusinessRegimeEngine
import `in`.zerotax.core.taxengine.advanced.NonResidentTaxAndResidencyEngine
import `in`.zerotax.core.taxengine.advanced.NonResidentTaxAndResidencyEngine.ResidencyEvaluationInput
import `in`.zerotax.core.taxengine.advanced.NonResidentTaxAndResidencyEngine.ResidencyStatus
import `in`.zerotax.core.taxengine.advanced.NonResidentTaxAndResidencyEngine.NriTaxPortfolioInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualRegionNriComplianceScreen(
    pan: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Section 6 Residency Inputs
    var daysCurrentFyText by remember { mutableStateOf("85") }
    var daysPast4FysText by remember { mutableStateOf("150") }
    var daysPast7FysText by remember { mutableStateOf("300") }
    var isCitizenVisiting by remember { mutableStateOf(true) }
    var isLeavingForJob by remember { mutableStateOf(false) }
    var indianIncomeOver15L by remember { mutableStateOf(false) }

    // NRI Portfolio Inputs
    var nreInterestText by remember { mutableStateOf("250000") } // ₹2.5L NRE interest (Tax-free)
    var fcnrInterestText by remember { mutableStateOf("120000") } // ₹1.2L FCNR interest (Tax-free)
    var nroInterestText by remember { mutableStateOf("180000") } // ₹1.8L NRO interest (30% TDS)
    var foreignIncomeText by remember { mutableStateOf("4500000") } // ₹45L Foreign salary (Exempt)
    var selectedCountry by remember { mutableStateOf("United States") }
    var treatyRateText by remember { mutableStateOf("15.0") }

    var copyStatusMessage by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val residencyInput = remember(
        daysCurrentFyText, daysPast4FysText, daysPast7FysText,
        isCitizenVisiting, isLeavingForJob, indianIncomeOver15L
    ) {
        ResidencyEvaluationInput(
            daysInIndiaCurrentFy = daysCurrentFyText.toIntOrNull() ?: 0,
            daysInIndiaPast4Fys = daysPast4FysText.toIntOrNull() ?: 0,
            daysInIndiaPast7Fys = daysPast7FysText.toIntOrNull() ?: 0,
            isIndianCitizenOrPersonOfIndianOrigin = isCitizenVisiting,
            isVisitingIndia = isCitizenVisiting,
            isLeavingIndiaForEmploymentOrCrew = isLeavingForJob,
            totalIndianIncomeExcludingForeignRupees = if (indianIncomeOver15L) 2000000L else 500000L
        )
    }

    val residencyDecision = remember(residencyInput) {
        NonResidentTaxAndResidencyEngine.evaluateResidency(residencyInput)
    }

    val portfolioInput = remember(
        residencyDecision, nreInterestText, fcnrInterestText, nroInterestText,
        foreignIncomeText, selectedCountry, treatyRateText
    ) {
        NriTaxPortfolioInput(
            residencyStatus = residencyDecision.status,
            nreAccountInterestRupees = nreInterestText.toLongOrNull() ?: 0L,
            fcnrDepositInterestRupees = fcnrInterestText.toLongOrNull() ?: 0L,
            nroAccountInterestRupees = nroInterestText.toLongOrNull() ?: 0L,
            foreignSourcedIncomeRupees = foreignIncomeText.toLongOrNull() ?: 0L,
            dtaaCountry = selectedCountry,
            dtaaTreatyRatePercent = treatyRateText.toDoubleOrNull() ?: 15.0
        )
    }

    val taxResult = remember(portfolioInput) {
        NonResidentTaxAndResidencyEngine.computeNriTax(portfolioInput, pan)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ZeroTaxTopAppBar(
                title = "Dual-Region NRI Tax Suite",
                subtitle = "Section 6 Residency, Section 90 DTAA & Form 10F Relief",
                pan = pan,
                onBack = onBack,
                badgeText = residencyDecision.status.shortCode,
                badgeTone = BadgeTone.SUCCESS
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 6 Residency Verdict Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (residencyDecision.status == ResidencyStatus.NON_RESIDENT_INDIAN) {
                            if (isSystemInDarkTheme()) EnterpriseTeal900.copy(alpha = 0.35f) else EnterpriseTeal50
                        } else getIndigoStatutoryBg()
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (residencyDecision.status == ResidencyStatus.NON_RESIDENT_INDIAN) EnterpriseTeal600 else IndigoStatutory
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Section 6 Residential Status:", style = MaterialTheme.typography.labelSmall, color = Slate600)
                                Text(
                                    text = residencyDecision.status.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            StatutoryBadge(
                                text = residencyDecision.statutoryClause,
                                tone = BadgeTone.STATUTORY
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = residencyDecision.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700
                        )

                        Spacer(Modifier.height(10.dp))
                        Divider(color = Slate200, thickness = 0.5.dp)
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Global Income Taxable?", style = TextStyle(fontSize = 10.sp, color = Slate600))
                                Text(
                                    if (residencyDecision.isGlobalIncomeTaxableInIndia) "YES (All Worldwide)" else "NO (India Only)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (residencyDecision.isGlobalIncomeTaxableInIndia) CrimsonDanger else EmeraldSuccess
                                    )
                                )
                            }
                            Column {
                                Text("NRE 100% Tax Exemption?", style = TextStyle(fontSize = 10.sp, color = Slate600))
                                Text(
                                    if (residencyDecision.isEligibleForNreTaxExemption) "YES (Sec 10(4))" else "NO (Resident NRO/SB)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (residencyDecision.isEligibleForNreTaxExemption) EmeraldSuccess else CrimsonDanger
                                    )
                                )
                            }
                            Column {
                                Text("Sec 44AD Presumptive?", style = TextStyle(fontSize = 10.sp, color = Slate600))
                                Text(
                                    if (residencyDecision.isEligibleForSection44AD) "ELIGIBLE" else "BARRED (Resident Only)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (residencyDecision.isEligibleForSection44AD) EmeraldSuccess else AmberWarning
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 6 Physical Presence Calculator Inputs
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 0.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Section 6 Physical Stay Calculator (FY 2025-26)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = daysCurrentFyText,
                                onValueChange = { daysCurrentFyText = it },
                                label = { Text("Days in India (Current FY)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                            OutlinedTextField(
                                value = daysPast4FysText,
                                onValueChange = { daysPast4FysText = it },
                                label = { Text("Days in Preceding 4 FYs") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Indian Citizen / PIO Visiting India:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Relaxed 120-day or 182-day threshold", style = TextStyle(fontSize = 10.sp, color = Slate600))
                            }
                            Switch(
                                checked = isCitizenVisiting,
                                onCheckedChange = { isCitizenVisiting = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = EnterpriseTeal600)
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Indian Sourced Income > ₹15 Lakhs:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Triggers 120-day rule u/s 6(1)(c) / 6(1A)", style = TextStyle(fontSize = 10.sp, color = Slate600))
                            }
                            Switch(
                                checked = indianIncomeOver15L,
                                onCheckedChange = { indianIncomeOver15L = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = AmberWarning)
                            )
                        }
                    }
                }
            }

            // NRE / FCNR Tax-Free Vault & NRO 30% TDS Reclaimer
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
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
                                text = "NRE, FCNR & NRO Bank Portfolios",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            StatutoryBadge(
                                text = "NRE 100% Tax-Free",
                                tone = BadgeTone.SUCCESS
                            )
                        }
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nreInterestText,
                                onValueChange = { nreInterestText = it },
                                label = { Text("NRE Savings Interest (₹)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                            OutlinedTextField(
                                value = fcnrInterestText,
                                onValueChange = { fcnrInterestText = it },
                                label = { Text("FCNR Deposit Interest (₹)") },
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
                                value = nroInterestText,
                                onValueChange = { nroInterestText = it },
                                label = { Text("NRO Interest (30% TDS ₹)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                            OutlinedTextField(
                                value = foreignIncomeText,
                                onValueChange = { foreignIncomeText = it },
                                label = { Text("Foreign Sourced Salary (₹)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = getEmeraldSuccessBg())
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Sec 10(4) Exempted", style = TextStyle(fontSize = 10.sp, color = EmeraldSuccess))
                                    Text("₹${"%,d".format(taxResult.nreInterestExemptUnderSec10_4 + taxResult.fcnrInterestExemptUnderSec10_4)}", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Slate100)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("NRO TDS Withheld", style = TextStyle(fontSize = 10.sp, color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else Slate600))
                                    Text("₹${"%,d".format(taxResult.nroTdsDeductedAt30Percent)}", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = if (taxResult.estimatedRefundOrBalancePayable >= 0) getEmeraldSuccessBg() else getAmberWarningBg())
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(if (taxResult.estimatedRefundOrBalancePayable >= 0) "Estimated Refund" else "Balance Payable", style = TextStyle(fontSize = 10.sp, color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else Slate600))
                                    Text("₹${"%,d".format(kotlin.math.abs(taxResult.estimatedRefundOrBalancePayable))}", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }

            // DTAA Relief Section 90 & Electronic Form 10F Generator
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
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
                                text = "DTAA Section 90 & Form 10F Self-Declaration",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(taxResult.form10FMarkdown))
                                    copyStatusMessage = "Form 10F copied to clipboard!"
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
                                    Text("Copy Form 10F", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        if (copyStatusMessage != null) {
                            Spacer(Modifier.height(6.dp))
                            StatutoryBadge(text = copyStatusMessage!!, tone = BadgeTone.SUCCESS)
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedCountry,
                                onValueChange = { selectedCountry = it },
                                label = { Text("Country of Residence") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 12.sp)
                            )
                            OutlinedTextField(
                                value = treatyRateText,
                                onValueChange = { treatyRateText = it },
                                label = { Text("DTAA Rate (%)") },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
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
                                text = taxResult.form10FMarkdown,
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

            item {
                Spacer(Modifier.height(88.dp))
            }
        }
    }
}

