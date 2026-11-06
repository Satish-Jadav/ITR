package `in`.zerotax.app.ui.toolkit

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.BadgeTone
import `in`.zerotax.app.ui.components.StatutoryBadge
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.taxengine.advanced.*
import `in`.zerotax.core.taxengine.calculator.TaxpayerFinancialInput
import `in`.zerotax.core.taxengine.reconciliation.FreelancerGstInput
import `in`.zerotax.core.taxengine.reconciliation.Refund244AAuditInput
import `in`.zerotax.core.taxengine.reconciliation.RefundInterest244AAndGstAuditor
import `in`.zerotax.core.taxengine.subengines.FamilyTaxArbitrageInput
import `in`.zerotax.core.taxengine.subengines.ResidencyDayCountInput
import `in`.zerotax.core.taxengine.subengines.ResidentialStatusAndFamilyArbitrageEngine
import `in`.zerotax.feature.aiextractor.OfflineWhatIfTaxSimulator
import `in`.zerotax.feature.aiextractor.WhatIfSimulationResult
import `in`.zerotax.feature.pdfunlocker.Form12BbData
import `in`.zerotax.feature.pdfunlocker.Form15HData
import `in`.zerotax.feature.pdfunlocker.LocalStatutoryPdfGenerators
import kotlinx.coroutines.launch
import java.time.LocalDate

// ============================================================================
// 1. OFFLINE NATURAL-LANGUAGE WHAT-IF SCENARIO SIMULATOR (FEATURE F-15)
// ============================================================================
@Composable
fun WhatIfScenarioSimulatorView(
    pan: String,
    grossSalary: Long
) {
    var queryText by remember { mutableStateOf("What if I contribute 50000 in NPS Tier-I?") }
    var simulationResult by remember {
        mutableStateOf<WhatIfSimulationResult?>(
            OfflineWhatIfTaxSimulator.simulateWhatIfScenario(
                TaxpayerFinancialInput(grossSalary = grossSalary, section80CInvestments = 1_50_000L),
                "What if I contribute 50000 in NPS Tier-I?"
            )
        )
    }

    val suggestedQueries = listOf(
        "Contribute 50k in NPS",
        "Buy 25k health insurance",
        "Pay 2 lakh home loan interest",
        "Earn 5 lakh freelance consulting 44ADA",
        "Invest 1.5 lakh in ELSS"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal100)
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
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EnterpriseTeal700,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Offline What-If Tax Simulator",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                            )
                        }
                        StatutoryBadge(text = "< 5ms Pure On-Device", tone = BadgeTone.SUCCESS)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Hypothetical decision engine. Instant simulation with 0 network calls and 100% DPDP Act compliance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
        }

        // Query input box
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
                    Text("Type any financial scenario in plain English:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. What if I invest 50k in NPS or buy 25k mediclaim?") },
                        singleLine = false,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quick scenarios:", style = TextStyle(fontSize = 11.sp, color = Slate500))
                        Button(
                            onClick = {
                                simulationResult = OfflineWhatIfTaxSimulator.simulateWhatIfScenario(
                                    TaxpayerFinancialInput(grossSalary = grossSalary, section80CInvestments = 1_50_000L),
                                    queryText
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600)
                        ) {
                            Text("Simulate")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    // Quick tags
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedQueries.take(3).forEach { queryItem ->
                            SuggestionChip(
                                onClick = {
                                    queryText = queryItem
                                    simulationResult = OfflineWhatIfTaxSimulator.simulateWhatIfScenario(
                                        TaxpayerFinancialInput(grossSalary = grossSalary, section80CInvestments = 1_50_000L),
                                        queryItem
                                    )
                                },
                                label = { Text(queryItem, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Simulation result card
        simulationResult?.let { result ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isTaxSaved) EmeraldSuccessBg else CrimsonDangerBg
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (result.isTaxSaved) EmeraldSuccess else CrimsonDanger
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
                                Text(
                                    text = if (result.isTaxSaved) "ESTIMATED TAX SAVINGS" else "ESTIMATED TAX IMPACT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (result.isTaxSaved) EmeraldSuccess else CrimsonDanger
                                )
                                Text(
                                    text = if (result.isTaxSaved) "SAVE ₹${"%,d".format(kotlin.math.abs(result.taxDeltaRupees))}"
                                    else if (result.taxDeltaRupees > 0) "+₹${"%,d".format(result.taxDeltaRupees)}"
                                    else "₹0 (No Tax Change)",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            StatutoryBadge(
                                text = "Regime: ${if (result.recommendedRegimeAfter == `in`.zerotax.core.model.TaxRegime.NEW_REGIME_115BAC) "New Regime" else "Old Regime"}",
                                tone = BadgeTone.STATUTORY
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = result.naturalLanguageSummary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Slate200, thickness = 0.5.dp)
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Tax Liability:", style = MaterialTheme.typography.bodySmall)
                            Text("₹${"%,d".format(result.originalTotalTaxRupees)}", style = TextStyle(fontFamily = FontFamily.Monospace))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Simulated Tax Liability:", style = MaterialTheme.typography.bodySmall)
                            Text("₹${"%,d".format(result.simulatedTotalTaxRupees)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                        }

                        if (result.actionableGuidance.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("Actionable Guidance:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            result.actionableGuidance.forEach { tip ->
                                Text("• $tip", style = TextStyle(fontSize = 11.sp, color = Slate700))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. FORM 12BB EMPLOYEE INVESTMENT DECLARATION EXPORTER (FEATURE F-04)
// ============================================================================
@Composable
fun Form12BbDeclarationView(
    pan: String,
    grossSalary: Long
) {
    val coroutineScope = rememberCoroutineScope()
    var employerName by remember { mutableStateOf("") }
    var rentPaidText by remember { mutableStateOf("300000") }
    var landlordName by remember { mutableStateOf("") }
    var landlordPan by remember { mutableStateOf("") }
    var sec80CText by remember { mutableStateOf("150000") }
    var sec80CCD1BText by remember { mutableStateOf("50000") }
    var sec80DText by remember { mutableStateOf("25000") }
    var homeLoanInterestText by remember { mutableStateOf("180000") }
    var pdfGeneratedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal100)
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
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = EnterpriseTeal700,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "CBDT Form 12BB Proof Generator",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                            )
                        }
                        StatutoryBadge(text = "Rule 26C", tone = BadgeTone.STATUTORY)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Official statement showing claims by employee for deduction of tax u/s 192. Submit to employer HR/Payroll to prevent excessive TDS deduction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
        }

        // Form Inputs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Particulars of Claims & Employer", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

                    OutlinedTextField(
                        value = employerName,
                        onValueChange = { employerName = it },
                        label = { Text("Employer Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rentPaidText,
                            onValueChange = { rentPaidText = it },
                            label = { Text("Annual Rent (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = landlordPan,
                            onValueChange = { landlordPan = it },
                            label = { Text("Landlord PAN") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sec80CText,
                            onValueChange = { sec80CText = it },
                            label = { Text("Section 80C (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sec80CCD1BText,
                            onValueChange = { sec80CCD1BText = it },
                            label = { Text("NPS 80CCD(1B) (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sec80DText,
                            onValueChange = { sec80DText = it },
                            label = { Text("Health 80D (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = homeLoanInterestText,
                            onValueChange = { homeLoanInterestText = it },
                            label = { Text("Home Loan 24(b) (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            isGenerating = true
                            coroutineScope.launch {
                                val data = Form12BbData(
                                    employeeName = "Authorized Assessee",
                                    employeePan = pan,
                                    employerName = employerName,
                                    rentPaidAnnual = rentPaidText.toLongOrNull() ?: 0L,
                                    landlordName = landlordName,
                                    landlordPan = landlordPan,
                                    homeLoanInterestSec24b = homeLoanInterestText.toLongOrNull() ?: 0L,
                                    section80C = sec80CText.toLongOrNull() ?: 0L,
                                    section80CCD1B = sec80CCD1BText.toLongOrNull() ?: 0L,
                                    section80D = sec80DText.toLongOrNull() ?: 0L
                                )
                                pdfGeneratedBytes = LocalStatutoryPdfGenerators.generateForm12BbPdf(data)
                                isGenerating = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        enabled = !isGenerating
                    ) {
                        Text(if (isGenerating) "Generating Form 12BB PDF..." else "Export Official Form 12BB PDF")
                    }
                }
            }
        }

        pdfGeneratedBytes?.let { bytes ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EmeraldSuccessBg),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Form 12BB PDF Ready", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                            }
                            StatutoryBadge(text = "${bytes.size / 1024} KB", tone = BadgeTone.SUCCESS)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Form 12BB successfully generated on-device with official Rule 26C declaration and signature block.", style = MaterialTheme.typography.bodySmall, color = Slate700)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. FORM 15G / 15H ZERO-TDS DECLARATION GENERATOR (FEATURE F-07)
// ============================================================================
@Composable
fun Form15GOr15HZeroTdsView(pan: String) {
    val coroutineScope = rememberCoroutineScope()
    var isSeniorCitizen by remember { mutableStateOf(true) }
    var declarantName by remember { mutableStateOf("Ramakant Joshi") }
    var dobIso by remember { mutableStateOf("1954-08-15") }
    var bankName by remember { mutableStateOf("State Bank of India") }
    var branchName by remember { mutableStateOf("MG Road, Bengaluru") }
    var fdAccountNumber by remember { mutableStateOf("39485729102") }
    var estimatedFdInterestText by remember { mutableStateOf("48000") }
    var estimatedTotalIncomeText by remember { mutableStateOf("280000") }
    var pdfGeneratedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal100)
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
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EnterpriseTeal700,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isSeniorCitizen) "Form 15H (Senior Citizen)" else "Form 15G (Below 60)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                            )
                        }
                        StatutoryBadge(text = "Section 197A", tone = BadgeTone.STATUTORY)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Submit to bank branches on April 1 to prevent 10% TDS deduction on Fixed Deposit interest if your total estimated income is within tax-free limits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Senior Citizen (Age 60+)?", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Switch(checked = isSeniorCitizen, onCheckedChange = { isSeniorCitizen = it })
                    }

                    OutlinedTextField(
                        value = declarantName,
                        onValueChange = { declarantName = it },
                        label = { Text("Declarant Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = branchName,
                            onValueChange = { branchName = it },
                            label = { Text("Branch") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = fdAccountNumber,
                        onValueChange = { fdAccountNumber = it },
                        label = { Text("Fixed Deposit Account Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = estimatedFdInterestText,
                            onValueChange = { estimatedFdInterestText = it },
                            label = { Text("Estimated FD Interest (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = estimatedTotalIncomeText,
                            onValueChange = { estimatedTotalIncomeText = it },
                            label = { Text("Total Est. Income (₹)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            isGenerating = true
                            coroutineScope.launch {
                                val data = Form15HData(
                                    declarantName = declarantName,
                                    pan = pan,
                                    dateOfBirthIso = dobIso,
                                    bankName = bankName,
                                    bankBranch = branchName,
                                    fixedDepositAccountNumber = fdAccountNumber,
                                    estimatedFdInterestRupees = estimatedFdInterestText.toLongOrNull() ?: 0L,
                                    estimatedTotalIncomeRupees = estimatedTotalIncomeText.toLongOrNull() ?: 0L,
                                    isSeniorCitizen60Plus = isSeniorCitizen
                                )
                                pdfGeneratedBytes = LocalStatutoryPdfGenerators.generateForm15GOr15HPdf(data)
                                isGenerating = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        enabled = !isGenerating
                    ) {
                        Text(if (isGenerating) "Generating..." else if (isSeniorCitizen) "Export Form 15H PDF" else "Export Form 15G PDF")
                    }
                }
            }
        }

        pdfGeneratedBytes?.let { bytes ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EmeraldSuccessBg),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Official Zero-TDS PDF Generated (${bytes.size / 1024} KB)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Submit this declaration to your bank manager in April to ensure 0% TDS is deducted on your fixed deposit interest.", style = MaterialTheme.typography.bodySmall, color = Slate700)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. SECTION 6 RESIDENTIAL STATUS & FAMILY ARBITRAGE (FEATURES F-01 & F-02)
// ============================================================================
@Composable
fun ResidentialStatusAndFamilyArbitrageView() {
    var daysCurrentYearText by remember { mutableStateOf("110") }
    var daysPreceding4YearsText by remember { mutableStateOf("380") }
    var indianIncomeOver15L by remember { mutableStateOf(false) }

    val daysCurrentYear = daysCurrentYearText.toIntOrNull() ?: 0
    val daysPreceding4Years = daysPreceding4YearsText.toIntOrNull() ?: 0

    val residentialEvaluation = remember(daysCurrentYear, daysPreceding4Years, indianIncomeOver15L) {
        ResidentialStatusAndFamilyArbitrageEngine.determineResidentialStatusSec6(
            ResidencyDayCountInput(
                daysInIndiaCurrentFinancialYear = daysCurrentYear,
                daysInIndiaPreceding4Years = daysPreceding4Years,
                totalIndianTaxableIncomeExceeds15Lakhs = indianIncomeOver15L
            )
        )
    }

    val arbitrageResult = remember {
        ResidentialStatusAndFamilyArbitrageEngine.simulateFamilyTaxArbitrage(
            FamilyTaxArbitrageInput(
                annualRentPaidToParentsRupees = 3_60_000L,
                parentOwnsHouseProperty = true,
                parentsAnnualTaxableIncomeRupees = 2_00_000L,
                parentAge = 65,
                parentHealthInsurancePaidRupees = 50_000L,
                fdPrincipalGiftedToAdultChildOrParentsRupees = 10_00_000L
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 6 Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal100))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = EnterpriseTeal700,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Section 6 NRI/RNOR & Family Arbitrage", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Verify physical presence in India, check Schedule FA foreign asset disclosure immunity, and legally reduce household tax.", style = MaterialTheme.typography.bodySmall, color = Slate700)
                }
            }
        }

        // Section 6 Evaluation Result
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Status: ${residentialEvaluation.status.name.replace('_', ' ')}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        StatutoryBadge(
                            text = if (residentialEvaluation.isScheduleFaMandatory) "Schedule FA Mandatory" else "Schedule FA Exempt",
                            tone = if (residentialEvaluation.isScheduleFaMandatory) BadgeTone.WARNING else BadgeTone.SUCCESS
                        )
                    }
                    Text(residentialEvaluation.detailedExplanation, style = MaterialTheme.typography.bodySmall, color = Slate700)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = daysCurrentYearText,
                            onValueChange = { daysCurrentYearText = it },
                            label = { Text("Days in India FY 25-26") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = daysPreceding4YearsText,
                            onValueChange = { daysPreceding4YearsText = it },
                            label = { Text("Days in Preceding 4 FYs") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Family Arbitrage Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate50),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(IndigoStatutory))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = IndigoStatutory,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Family Tax Arbitrage (Sec 10(13A), 80D, Gifting)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = IndigoStatutory))
                    }
                    Spacer(Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Family Tax Saved:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("₹${"%,d".format(arbitrageResult.totalFamilyTaxSavedAnnualRupees)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = EmeraldSuccess, fontSize = 16.sp))
                    }

                    Spacer(Modifier.height(8.dp))
                    arbitrageResult.opportunities.forEach { opp ->
                        Text("• ${opp.title}: Saves ₹${"%,d".format(opp.annualTaxSavedRupees)}", style = TextStyle(fontSize = 11.sp, color = Slate700))
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. SECTION 244A REFUND INTEREST & FREELANCER GST AUDITOR (FEATURES F-05, F-08)
// ============================================================================
@Composable
fun RefundAuditAndGstView() {
    val auditResult = remember {
        RefundInterest244AAndGstAuditor.auditSection244ARefundInterest(
            Refund244AAuditInput(
                refundPrincipalDetermined = 1_50_000L,
                totalTaxLiabilityDetermined = 5_00_000L,
                filedOnOrBeforeDueDateSec139_1 = true,
                monthsElapsedFromApril1OfAy = 11,
                cpcGrantedInterestRupees = 4_500L
            )
        )
    }

    val gstRadar = remember {
        RefundInterest244AAndGstAuditor.reconcileFreelancerGstAndEfira(
            FreelancerGstInput(
                domesticReceipts194JRupees = 12_00_000L,
                foreignExportReceiptsRupees = 18_00_000L,
                hasValidLutArnForCurrentFy = true,
                eFiraReceiptCount = 10,
                totalForeignExportInvoicesCount = 10
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal100))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = EnterpriseTeal700,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Refund Interest Audit & Freelancer GST Radar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Audit CPC Section 244A interest (0.5%/month) and track export freelancer GST/LUT thresholds.", style = MaterialTheme.typography.bodySmall, color = Slate700)
                }
            }
        }

        // Section 244A Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (auditResult.isUnderPaid) CrimsonDangerBg else EmeraldSuccessBg
                ),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(if (auditResult.isUnderPaid) CrimsonDanger else EmeraldSuccess))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Section 244A Audit Result", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        StatutoryBadge(
                            text = if (auditResult.isUnderPaid) "Deficit Identified" else "Accurate Interest",
                            tone = if (auditResult.isUnderPaid) BadgeTone.DANGER else BadgeTone.SUCCESS
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Statutory Interest Due (0.5%/mo):", style = MaterialTheme.typography.bodySmall)
                        Text("₹${"%,d".format(auditResult.expectedStatutoryInterestRupees)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Actual Interest Credited by CPC:", style = MaterialTheme.typography.bodySmall)
                        Text("₹${"%,d".format(auditResult.cpcGrantedInterestRupees)}", style = TextStyle(fontFamily = FontFamily.Monospace))
                    }
                    if (auditResult.isUnderPaid) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Recoverable Deficit u/s 154:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = CrimsonDanger))
                            Text("₹${"%,d".format(auditResult.shortPaidInterestRupees)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CrimsonDanger))
                        }
                    }
                }
            }
        }

        // GST Freelancer Radar Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Freelancer GST Threshold Radar", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Total Receipts: ₹${"%,d".format(gstRadar.totalGrossReceiptsRupees)}", style = MaterialTheme.typography.bodySmall)
                    StatutoryBadge(
                        text = if (gstRadar.isGstRegistrationMandatory) "GST Registration Mandatory" else "Below ₹20L Threshold",
                        tone = if (gstRadar.isGstRegistrationMandatory) BadgeTone.DANGER else BadgeTone.SUCCESS
                    )
                    Spacer(Modifier.height(4.dp))
                    gstRadar.actionableDirectives.forEach { directive ->
                        Text("• $directive", style = TextStyle(fontSize = 11.sp, color = Slate700))
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. SCHEDULE AL ASSET-LIABILITY & SAFE-HARBOR (FEATURES F-03, F-09)
// ============================================================================
@Composable
fun ScheduleAlAndWealthView() {
    val scheduleAl = remember {
        ScheduleAlAndCapitalGainsWealthEngine.buildScheduleAl(
            totalIncomeRupees = 65_00_000L,
            immovableAssets = listOf(
                ImmovableAssetCostEntry(
                    description = "Residential Flat 402, Whitefield",
                    address = "Bengaluru, Karnataka",
                    costOfAcquisitionRupees = 85_00_000L
                )
            ),
            movableAssets = listOf(
                MovableAssetCostEntry(assetCategory = "SHARES_AND_MFS", costOrBalanceAsOnMarch31Rupees = 45_00_000L),
                MovableAssetCostEntry(assetCategory = "BANK_DEPOSITS_CASH", costOrBalanceAsOnMarch31Rupees = 18_00_000L),
                MovableAssetCostEntry(assetCategory = "JEWELRY_BULLION", costOrBalanceAsOnMarch31Rupees = 25_00_000L),
                MovableAssetCostEntry(assetCategory = "VEHICLES_BOATS_AIRCRAFT", costOrBalanceAsOnMarch31Rupees = 12_00_000L)
            ),
            liabilities = listOf(
                LiabilityCostEntry(liabilityType = "HOUSING_LOAN", outstandingPrincipalAsOnMarch31Rupees = 35_00_000L)
            )
        )
    }

    val safeHarbor = remember {
        ScheduleAlAndCapitalGainsWealthEngine.verifySection50CSafeHarbor(
            actualAgreementConsideration = 1_00_00_000L,
            stampDutyCircleRateValue = 1_08_00_000L
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal100))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = EnterpriseTeal700,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Schedule AL & Section 50C Safe-Harbor Shield", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Auto-generate Asset & Liability disclosure for GTI > ₹50L and check 10% circle rate safe-harbor tolerance.", style = MaterialTheme.typography.bodySmall, color = Slate700)
                }
            }
        }

        // Section 50C Safe Harbor Shield
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (safeHarbor.isProtectedUnder10PercentSafeHarbor) EmeraldSuccessBg else CrimsonDangerBg
                ),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(if (safeHarbor.isProtectedUnder10PercentSafeHarbor) EmeraldSuccess else CrimsonDanger))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Section 50C 10% Safe-Harbor", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        StatutoryBadge(
                            text = if (safeHarbor.isProtectedUnder10PercentSafeHarbor) "Protected (Third Proviso)" else "Deemed Sale Triggered",
                            tone = if (safeHarbor.isProtectedUnder10PercentSafeHarbor) BadgeTone.SUCCESS else BadgeTone.DANGER
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(text = safeHarbor.statutoryExplanation, style = MaterialTheme.typography.bodySmall, color = Slate700)
                    Spacer(Modifier.height(6.dp))
                    Text("Full Value of Consideration for Capital Gains: ₹${"%,d".format(safeHarbor.effectiveFullValueOfConsiderationAdopted)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                }
            }
        }

        // Schedule AL Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Schedule AL Statutory Balance Sheet", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Gross Assets at Cost:", style = MaterialTheme.typography.bodySmall)
                        Text("₹${"%,d".format(scheduleAl.totalAssetsAtCost)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Liabilities / Loans:", style = MaterialTheme.typography.bodySmall)
                        Text("₹${"%,d".format(scheduleAl.totalLiabilitiesAtCost)}", style = TextStyle(fontFamily = FontFamily.Monospace))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Asset Wealth at Cost:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Text("₹${"%,d".format(scheduleAl.netWealthAtCostRupees)}", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = EnterpriseTeal700))
                    }
                }
            }
        }
    }
}
