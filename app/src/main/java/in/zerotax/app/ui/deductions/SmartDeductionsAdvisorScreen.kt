package in.zerotax.app.ui.deductions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import in.zerotax.app.ui.components.*
import in.zerotax.app.ui.theme.*
import in.zerotax.core.model.DeductionRegimeScope
import in.zerotax.core.model.LifeEventTag
import in.zerotax.core.model.Rupee
import in.zerotax.core.model.StatutoryDeductionSpec
import in.zerotax.core.taxengine.advanced.SmartDeductionsCatalog
import in.zerotax.feature.aiextractor.CopilotExplanation
import in.zerotax.feature.aiextractor.OnDeviceTaxCopilot
import in.zerotax.feature.aiextractor.SupportedVernacularLocale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartDeductionsAdvisorScreen(
    onBack: () -> Unit,
    onApplyDeductions: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copilot = remember { OnDeviceTaxCopilot(context) }

    // Selected Life Events
    var selectedLifeEvents by remember {
        mutableStateOf(setOf(LifeEventTag.HOME_LOAN, LifeEventTag.HEALTH_INSURANCE, LifeEventTag.NPS_INVESTMENT))
    }

    // Editable state of deductions
    var deductionSpecs by remember {
        mutableStateOf(
            SmartDeductionsCatalog.ALL_DEDUCTION_SPECS.map { spec ->
                if (spec.lifeEventTag in selectedLifeEvents) {
                    spec.copy(isSelected = true, userClaimedAmount = spec.defaultEligibleAmount)
                } else {
                    spec
                }
            }
        )
    }

    // AI Copilot sheet
    var isCopilotSheetVisible by remember { mutableStateOf(false) }
    var selectedLocale by remember { mutableStateOf(SupportedVernacularLocale.ENGLISH_IN) }
    var currentExplanation by remember { mutableStateOf<CopilotExplanation?>(null) }
    var isCopilotThinking by remember { mutableStateOf(false) }

    val deductionFaqs = remember {
        listOf(
            "Can I claim both HRA exemption and Home Loan Section 24(b)?",
            "What is the difference between 80C principal and 24(b) interest?",
            "Can I claim Section 80CCD(2) employer NPS in New Tax Regime?",
            "Do I need landlord PAN for HRA or Section 80GG?",
            "Can I claim Section 80E education loan for my child?",
            "What documents are needed for Section 80D health insurance?"
        )
    }

    val visibleDeductions = deductionSpecs.filter { it.lifeEventTag in selectedLifeEvents }
    val totalClaimedDeductions = visibleDeductions.filter { it.isSelected }.sumOf { it.userClaimedAmount }
    val estimatedTaxSavings = (totalClaimedDeductions * 0.208).toLong() // Approx 20% slab + 4% cess

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart Deduction Advisor & Life-Events",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Statutory Rules, Proof Holders & Document Fetching Guide",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { isCopilotSheetVisible = true },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "✨ Ask Copilot",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = EnterpriseTeal600,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Identified Deductions",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                        Text(
                            text = Rupee(totalClaimedDeductions).toString(),
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                        )
                        Text(
                            text = "Est. Tax Saved: ~₹${estimatedTaxSavings}",
                            style = TextStyle(fontSize = 11.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Button(
                        onClick = {
                            onApplyDeductions(totalClaimedDeductions)
                            onBack()
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Apply to Return →", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Life-Event Multi-Select Horizontal Carousel
            item {
                Text(
                    text = "Select Your Running Scenarios / Life-Events:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Pick what applies to you. ZeroTax automatically identifies the exact deductions, limits, and document holders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(LifeEventTag.entries) { tag ->
                        val isSelected = tag in selectedLifeEvents
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) EnterpriseTeal600 else Slate100)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) EnterpriseTeal700 else Slate300,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    val next = if (isSelected) selectedLifeEvents - tag else selectedLifeEvents + tag
                                    selectedLifeEvents = next
                                    deductionSpecs = deductionSpecs.map { spec ->
                                        if (spec.lifeEventTag == tag) {
                                            spec.copy(isSelected = !isSelected, userClaimedAmount = if (!isSelected) spec.defaultEligibleAmount else 0L)
                                        } else spec
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tag.icon, fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = tag.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) PureWhite else Slate800,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Recommended Deductions for Selected Life-Events (${visibleDeductions.size}):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
            }

            // Deductions Detail List
            items(visibleDeductions) { spec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = if (spec.isSelected) 1.dp else 0.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (spec.isSelected) EnterpriseTeal600 else MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spec.deductionTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = spec.statutoryRef,
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = IndigoStatutory)
                                )
                            }
                            StatutoryBadge(
                                text = if (spec.regimeScope == DeductionRegimeScope.BOTH_REGIMES) "Old & New Regime" else "Old Regime Only",
                                tone = if (spec.regimeScope == DeductionRegimeScope.BOTH_REGIMES) BadgeTone.SUCCESS else BadgeTone.STATUTORY
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Document Holder Callout
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(3.dp))
                                .background(Slate100)
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏛️", fontSize = 12.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Who Holds This: ${spec.documentHolder}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate800)
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Required Certificate: ${spec.requiredDocumentName}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "How to Fetch: ${spec.whereToFetchInstructions}",
                                    style = TextStyle(fontSize = 11.sp, color = Slate600)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = spec.howItAppliesExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700
                        )

                        Spacer(Modifier.height(10.dp))

                        // Amount Claimed Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Statutory Max Limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500
                                )
                                Text(
                                    text = Rupee(spec.maxLimitRupees).toString(),
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = spec.userClaimedAmount.toString(),
                                    onValueChange = { newVal ->
                                        val parsed = newVal.filter { it.isDigit() }.toLongOrNull() ?: 0L
                                        val capped = minOf(parsed, spec.maxLimitRupees)
                                        deductionSpecs = deductionSpecs.map {
                                            if (it.sectionCode == spec.sectionCode) it.copy(userClaimedAmount = capped, isSelected = capped > 0) else it
                                        }
                                    },
                                    label = { Text("Claim ₹") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.width(130.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                TextButton(
                                    onClick = {
                                        deductionSpecs = deductionSpecs.map {
                                            if (it.sectionCode == spec.sectionCode) it.copy(userClaimedAmount = spec.maxLimitRupees, isSelected = true) else it
                                        }
                                    }
                                ) {
                                    Text("Max", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // ========================================================================
    // ON-DEVICE GEMINI TAX COPILOT SHEET
    // ========================================================================
    if (isCopilotSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isCopilotSheetVisible = false },
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "On-Device Gemini Deduction Advisor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "AICore Gemini Nano (temp = 0.0f) • 0B Cloud Egress",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }

                    // Vernacular Switcher
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SupportedVernacularLocale.entries.forEach { loc ->
                            val isSel = selectedLocale == loc
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSel) EnterpriseTeal600 else Slate100)
                                    .clickable {
                                        selectedLocale = loc
                                        if (currentExplanation != null) {
                                            scope.launch {
                                                isCopilotThinking = true
                                                currentExplanation = copilot.explainContextualTaxTopic(currentExplanation!!.title, loc)
                                                isCopilotThinking = false
                                            }
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = loc.code.take(2).uppercase(),
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) PureWhite else Slate600)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Deductions & Home Loan Questions:",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                )
                Spacer(Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(deductionFaqs) { q ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Slate100)
                                .border(0.5.dp, Slate300, RoundedCornerShape(4.dp))
                                .clickable {
                                    scope.launch {
                                        isCopilotThinking = true
                                        currentExplanation = copilot.explainContextualTaxTopic(q, selectedLocale)
                                        isCopilotThinking = false
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = q, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (isCopilotThinking) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = EnterpriseTeal600, strokeWidth = 2.5.dp)
                    }
                } else if (currentExplanation != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.4f)))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = currentExplanation!!.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                StatutoryBadge(text = selectedLocale.code.uppercase(), tone = BadgeTone.SUCCESS)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = currentExplanation!!.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate800
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Select any question above to learn how home loan interest, HRA, 80D, or NPS deductions apply to your return.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
