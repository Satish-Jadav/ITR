package `in`.zerotax.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.FilingTierMode
import `in`.zerotax.core.model.InstitutionPdfPasswordRegistry
import `in`.zerotax.core.model.InstitutionPdfPasswordRule
import `in`.zerotax.core.model.Rupee
import `in`.zerotax.core.model.ScheduleItChallanEntry
import `in`.zerotax.core.model.TaxPaymentChannel
import `in`.zerotax.core.security.network.ZeroTaxCloudApiBridge
import `in`.zerotax.feature.portalbridge.ChallanTaxType
import `in`.zerotax.feature.portalbridge.UpiChallan280IntentLauncher

@Composable
fun DpdpSection6MultilingualConsentCard(
    currentLocale: String,
    onLocaleSelected: (String) -> Unit,
    onConsentGranted: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DPDP Act, 2023 — Statutory Consent", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Your financial and tax data is processed strictly on-device inside hardware-encrypted vault (StrongBox AES-256-GCM). Zero network egress.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("en-IN" to "English", "hi-IN" to "हिन्दी", "gu-IN" to "ગુજરાતી").forEach { (code, label) ->
                    FilterChip(
                        selected = currentLocale == code,
                        onClick = { onLocaleSelected(code) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConsentGranted,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Grant Local On-Device Consent")
            }
        }
    }
}

@Composable
fun OneTapPdfDisambiguationCard(
    extractedValue: Long,
    expectedNetSalary: Long,
    croppedRegionBitmap: Bitmap?,
    onConfirmUserOverridden: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Discrepancy in Form 16 Net Salary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Printed Net: ${IndianCurrencyFormatter.formatRupees(extractedValue)} vs Computed: ${IndianCurrencyFormatter.formatRupees(expectedNetSalary)}")

            croppedRegionBitmap?.let { bmp ->
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Cropped PDF region",
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConfirmUserOverridden(expectedNetSalary) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Use Computed")
                }
                OutlinedButton(
                    onClick = { onConfirmUserOverridden(extractedValue) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Use Printed")
                }
            }
        }
    }
}

@Composable
fun BlackMoneyActForeignAssetModal(
    onDismiss: () -> Unit,
    onAcknowledged: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Black Money Act Warning", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Failure to disclose foreign shares (RSU/ESPP) in Schedule FA attracts a statutory penalty of ₹10,00,000 under Section 42 & 43 of the Black Money Act, 2015, even if foreign tax was withheld.",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAcknowledged,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("I Acknowledge & Will Fill Schedule FA")
                }
            }
        }
    }
}

@Composable
fun EmployerTdsShieldCard(
    discrepancyAmount: Long,
    employerTan: String,
    onGenerateSection205Notice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Section 205 Employer TDS Shield", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Employer $employerTan deducted ${IndianCurrencyFormatter.formatRupees(discrepancyAmount)} but failed to deposit it to Govt Treasury. Under Section 205, the IT Department cannot recover this tax from you.")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onGenerateSection205Notice, modifier = Modifier.fillMaxWidth()) {
                Text("Generate Section 205 Legal Dispute Notice")
            }
        }
    }
}

@Composable
fun RegimeBreakEvenOptimizerCard(
    oldRegimeTax: Long,
    newRegimeTax: Long,
    breakEvenDeductionNeeded: Long,
    recommendedRegime: String,
    onSelectRegime: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Dual-Regime Showdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("New Regime: ${IndianCurrencyFormatter.formatRupees(newRegimeTax)}")
                Text("Old Regime: ${IndianCurrencyFormatter.formatRupees(oldRegimeTax)}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (breakEvenDeductionNeeded > 0L) {
                Text(
                    "You need ${IndianCurrencyFormatter.formatRupees(breakEvenDeductionNeeded)} more deductions (80C/80D/HRA) to make Old Regime win.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onSelectRegime(recommendedRegime) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Opt for $recommendedRegime (Saves Tax)")
            }
        }
    }
}

@Composable
fun Presumptive44AdaAnd44AdSliderCard(
    grossReceipts: Long,
    declaredRatePercent: Double,
    is44AdaProfession: Boolean,
    onRateChanged: (Double) -> Unit
) {
    val floorRate = if (is44AdaProfession) 50.0 else 6.0
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (is44AdaProfession) "Section 44ADA Presumptive (50% Floor)" else "Section 44AD Presumptive (6%/8% Floor)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Gross Receipts: ${IndianCurrencyFormatter.formatRupees(grossReceipts)}")
            Text("Declared Profit Rate: ${"%.1f".format(declaredRatePercent)}% (Min: ${floorRate}%)")
            Slider(
                value = declaredRatePercent.toFloat(),
                onValueChange = { onRateChanged(it.toDouble().coerceAtLeast(floorRate)) },
                valueRange = floorRate.toFloat()..100f
            )
            val deemedProfit = (grossReceipts * (declaredRatePercent / 100.0)).toLong()
            Text("Deemed Taxable Business Profit: ${IndianCurrencyFormatter.formatRupees(deemedProfit)}", fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Interactive helper dialog providing institutional password patterns for Form 16, AIS, e-Aadhaar, banks, and CAS.
 */
@Composable
fun InstitutionPdfPasswordCheatsheetModal(
    onDismiss: () -> Unit,
    onSelectHint: ((String) -> Unit)? = null
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Tax & Identity", "Banking", "Investments")
    var allRules by remember { mutableStateOf(InstitutionPdfPasswordRegistry.rules) }

    LaunchedEffect(Unit) {
        runCatching {
            val json = ZeroTaxCloudApiBridge.fetchInstitutionPdfRules().getOrNull()
            val array = json?.optJSONArray("rules")
            if (array != null && array.length() > 0) {
                val list = mutableListOf<InstitutionPdfPasswordRule>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    list.add(
                        InstitutionPdfPasswordRule(
                            institutionId = o.optString("institutionId"),
                            institutionName = o.optString("institutionName"),
                            documentCategory = o.optString("documentCategory", "Tax & Identity"),
                            passwordPatternRule = o.optString("passwordPatternRule"),
                            exampleDescription = o.optString("exampleDescription"),
                            exampleInput = o.optString("exampleInput"),
                            officialHelplineHint = o.optString("officialHelplineHint")
                        )
                    )
                }
                InstitutionPdfPasswordRegistry.updateRules(list)
                allRules = list
            }
        }
    }

    val filteredRules = remember(selectedCategory, allRules) {
        if (selectedCategory == "All") allRules
        else allRules.filter { it.documentCategory == selectedCategory }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = EnterpriseTeal600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "PDF Password Cheatsheet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                Text(
                    "Standard deterministic password conventions used across Indian tax authorities and financial institutions:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Category chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredRules) { rule ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate50),
                            border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate200)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectHint?.invoke(rule.exampleInput) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rule.institutionName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    )
                                    StatutoryBadge(
                                        text = rule.documentCategory,
                                        tone = when (rule.documentCategory) {
                                            "Tax & Identity" -> BadgeTone.STATUTORY
                                            "Banking" -> BadgeTone.SUCCESS
                                            else -> BadgeTone.NEUTRAL
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Formula: ${rule.passwordPatternRule}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = EnterpriseTeal700)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${rule.exampleDescription} -> ${rule.exampleInput}",
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate600)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Got It")
                }
            }
        }
    }
}

/**
 * 5-point Interactive Pre-Submission Safety Checklist before CPC transmission.
 */
@Composable
fun PreSubmissionSafetyChecklistCard(
    isBankPreValidated: Boolean,
    isAadhaarLinked: Boolean,
    hasDiscrepancyShieldActive: Boolean,
    isLandlordPanCompliant: Boolean,
    isScheduleFaCompliant: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EnterpriseTeal50.copy(alpha = 0.6f)),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.4f)))
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
                        tint = EnterpriseTeal900,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Pre-Filing Safety & Notice Shield (5 Gates)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                    )
                }
                val allPassed = isBankPreValidated && isAadhaarLinked && hasDiscrepancyShieldActive && isLandlordPanCompliant && isScheduleFaCompliant
                StatutoryBadge(
                    text = if (allPassed) "5/5 VERIFIED" else "ACTION RECOMMENDED",
                    tone = if (allPassed) BadgeTone.SUCCESS else BadgeTone.WARNING
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SafetyChecklistItem(
                title = "Direct Refund Bank Account",
                description = if (isBankPreValidated) "Pre-validated on ITD portal for electronic refund credit" else "Verify IFSC & active ECS status to avoid refund delays",
                isPassed = isBankPreValidated,
                statutoryRef = "Rule 12"
            )

            SafetyChecklistItem(
                title = "Aadhaar-PAN Linkage Status",
                description = if (isAadhaarLinked) "PAN is fully operative u/s 139AA with instant e-Verification" else "Ensure Aadhaar is linked to prevent PAN from turning inoperative",
                isPassed = isAadhaarLinked,
                statutoryRef = "Sec 139AA"
            )

            SafetyChecklistItem(
                title = "Section 205 Employer TDS Shield",
                description = if (hasDiscrepancyShieldActive) "No uncredited TDS demand risks from employer non-deposit" else "Employer TDS difference shielded from recovery u/s 205",
                isPassed = hasDiscrepancyShieldActive,
                statutoryRef = "Sec 205"
            )

            SafetyChecklistItem(
                title = "Mandatory Landlord PAN Reporting",
                description = if (isLandlordPanCompliant) "Landlord PAN compliant or annual rent <= ₹1,00,000" else "Rent > ₹1 Lakh/yr requires landlord PAN under Circular 08/2013",
                isPassed = isLandlordPanCompliant,
                statutoryRef = "Circular 08/2013"
            )

            SafetyChecklistItem(
                title = "Schedule FA / Foreign Assets Gate",
                description = if (isScheduleFaCompliant) "Foreign ESOP/RSU/US equity holdings fully compliant" else "Foreign assets must be declared to avoid ₹10L penalty u/s 42/43",
                isPassed = isScheduleFaCompliant,
                statutoryRef = "Black Money Act"
            )
        }
    }
}

@Composable
private fun SafetyChecklistItem(
    title: String,
    description: String,
    isPassed: Boolean,
    statutoryRef: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (isPassed) EmeraldSuccess else AmberWarning),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPassed) Icons.Default.Check else Icons.Default.PriorityHigh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = statutoryRef,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Transparent explainer showing how Section 87A Marginal Relief protects
 * taxpayers earning between ₹7,00,000 and ₹7,27,777 under the New Tax Regime.
 */
@Composable
fun Section87AMarginalReliefExplainerCard(
    taxableIncome: Long,
    taxBeforeRelief: Long,
    marginalReliefAmount: Long,
    finalTaxBeforeCess: Long,
    modifier: Modifier = Modifier
) {
    val excessOver7Lakh = (taxableIncome - 7_00_000L).coerceAtLeast(0L)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSuccessBg.copy(alpha = 0.5f)),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess.copy(alpha = 0.4f)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Section 87A Marginal Relief Shield",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    )
                }
                StatutoryBadge(text = "SAVED ₹${Rupee.formatInr(marginalReliefAmount)}", tone = BadgeTone.SUCCESS)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Under the proviso to Section 87A (New Regime), the tax payable on income exceeding ₹7,00,000 cannot exceed the excess income itself (₹${Rupee.formatInr(excessOver7Lakh)}).",
                style = MaterialTheme.typography.bodySmall,
                color = Slate800
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Standard Slab Tax", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    Text("₹${Rupee.formatInr(taxBeforeRelief)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Marginal Relief", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                    Text("-₹${Rupee.formatInr(marginalReliefAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Actual Tax Payable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("₹${Rupee.formatInr(finalTaxBeforeCess)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                }
            }
        }
    }
}

/**
 * Plan-based UI Tier Selector enabling tailored workflows:
 * - Sahaj Simple: Zero-jargon, 1-tap filing for salaried / small money
 * - Growth & Investor: Capital gains, 44ADA, and deductions optimizer
 * - Enterprise Audit Shield: Big money, HNI, Schedule FA, and Section 44AB audit
 */
@Composable
fun FilingTierSelectorCard(
    currentTier: FilingTierMode,
    recommendedTier: FilingTierMode,
    onTierSelected: (FilingTierMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentTier.badgeIcon, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Filing Plan: ${currentTier.title}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }
                if (currentTier == recommendedTier) {
                    StatutoryBadge(text = "RECOMMENDED", tone = BadgeTone.SUCCESS)
                } else {
                    StatutoryBadge(text = "ACTIVE PLAN", tone = BadgeTone.NEUTRAL)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: Top two plans side by side (spacious 50% width each)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val topTwoTiers = listOf(FilingTierMode.SAHAJ_SIMPLE, FilingTierMode.INVESTOR_PRO)
                topTwoTiers.forEach { tier ->
                    val isSelected = currentTier == tier
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTierSelected(tier) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = if (isSelected) {
                            CardDefaults.outlinedCardBorder().copy(width = 1.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                        } else CardDefaults.outlinedCardBorder().copy(width = 0.75.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(tier.badgeIcon, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = tier.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Bottom 3rd tier: Enterprise Audit (wrapped full-width so it is completely spacious)
            val isEnterpriseSelected = currentTier == FilingTierMode.ENTERPRISE_AUDIT
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTierSelected(FilingTierMode.ENTERPRISE_AUDIT) },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnterpriseSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = if (isEnterpriseSelected) {
                    CardDefaults.outlinedCardBorder().copy(width = 1.5.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
                } else CardDefaults.outlinedCardBorder().copy(width = 0.75.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(FilingTierMode.ENTERPRISE_AUDIT.badgeIcon, fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = FilingTierMode.ENTERPRISE_AUDIT.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isEnterpriseSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isEnterpriseSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "ITR-2, ITR-3, Form 3CD/3CA Audit & Trusts",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                    if (isEnterpriseSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${currentTier.subtitle} • ${currentTier.targetPersona}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Comprehensive Tax Payment & Challan 280 Hub covering all payment conditions (Advance Tax, Self-Assessment,
 * Demand Assessment) across all official payment channels (UPI, NetBanking, Debit Card, RTGS/NEFT, Over-the-Counter).
 */
@Composable
fun TaxPaymentAndChallanHubModal(
    pan: String,
    taxPayableRupees: Long,
    assessmentYear: String = "2026-27",
    onDismiss: () -> Unit,
    onRecordChallan: (ScheduleItChallanEntry) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var selectedTaxType by remember {
        mutableStateOf(if (taxPayableRupees > 10_000L) ChallanTaxType.ADVANCE_TAX else ChallanTaxType.SELF_ASSESSMENT_TAX)
    }
    var selectedChannel by remember { mutableStateOf(TaxPaymentChannel.UPI_INSTANT) }
    var paymentAmountInput by remember { mutableStateOf(if (taxPayableRupees > 0L) taxPayableRupees.toString() else "10000") }

    // Challan 280 Verification States
    var showReceiptVerification by remember { mutableStateOf(false) }
    var bsrCodeInput by remember { mutableStateOf("") }
    var challanSerialInput by remember { mutableStateOf("") }
    var depositDateInput by remember { mutableStateOf("2026-03-15") }
    var verificationError by remember { mutableStateOf<String?>(null) }
    var isVerificationSuccess by remember { mutableStateOf(false) }

    val amountLong = paymentAmountInput.filter { it.isDigit() }.toLongOrNull() ?: taxPayableRupees

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = EnterpriseTeal600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Tax Payment & Challan 280 Hub",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "PAN: $pan • Major Head 0021",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate500)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Payment Condition / Head
                    item {
                        Text("1. Select Payment Condition & Head:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            ChallanTaxType.entries.forEach { type ->
                                FilterChip(
                                    selected = selectedTaxType == type,
                                    onClick = { selectedTaxType = type },
                                    label = { Text(type.description, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // 2. Amount Input
                    item {
                        OutlinedTextField(
                            value = paymentAmountInput,
                            onValueChange = { paymentAmountInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Tax Amount to Pay (₹)") },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 3. Payment Channels
                    item {
                        Text("2. Choose Payment Method (CBDT TIN 2.0 Authorized):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        TaxPaymentChannel.entries.forEach { channel ->
                            val isSelected = selectedChannel == channel
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { selectedChannel = channel },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) EnterpriseTeal50 else Slate50
                                ),
                                border = if (isSelected) {
                                    CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600))
                                } else CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate200))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val channelIcon = when (channel.iconName) {
                                        "bolt" -> Icons.Default.Bolt
                                        "account_balance" -> Icons.Default.AccountBalance
                                        "credit_card" -> Icons.Default.CreditCard
                                        "domain" -> Icons.Default.Domain
                                        else -> Icons.Default.Description
                                    }
                                    Icon(
                                        imageVector = channelIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) EnterpriseTeal700 else Slate600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = channel.title,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        )
                                        Text(
                                            text = "${channel.description} • Fee: ${channel.statutoryFee}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Action Button for Channel
                    item {
                        when (selectedChannel) {
                            TaxPaymentChannel.UPI_INSTANT -> {
                                Button(
                                    onClick = {
                                        val uri = UpiChallan280IntentLauncher.buildUpiPaymentUri(
                                            pan = pan,
                                            amountRupees = amountLong,
                                            assessmentYear = assessmentYear,
                                            taxType = selectedTaxType
                                        )
                                        UpiChallan280IntentLauncher.launchUpiPaymentIntent(context, uri)
                                        showReceiptVerification = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Launch UPI App (GPay / PhonePe / Paytm)")
                                    }
                                }
                            }
                            TaxPaymentChannel.NET_BANKING, TaxPaymentChannel.DEBIT_CARD -> {
                                Button(
                                    onClick = {
                                        uriHandler.openUri("https://eportal.incometax.gov.in/iec/foservices/#/e-pay-tax-prelogin/user-details")
                                        showReceiptVerification = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open e-Filing Portal TIN 2.0 Gateway")
                                    }
                                }
                            }
                            TaxPaymentChannel.RTGS_NEFT -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AmberWarningBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("RTGS / NEFT Mandate Instructions:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            "1. Login to eportal.incometax.gov.in -> e-Pay Tax -> Create New Challan.\n2. Choose 'RTGS/NEFT' to generate your unique Virtual Account Number (VAN) starting with 'ITD...'.\n3. Transfer ₹${Rupee.formatInr(amountLong)} via your bank NetBanking RTGS to this VAN.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            TaxPaymentChannel.OVER_THE_COUNTER -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Slate100),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Over-The-Counter (Cheque / Cash) Instructions:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            "Generate Form CRN Challan 280 on the e-Filing portal, print the physical mandate form, and present it with Cheque/DD at any authorized bank branch (SBI, HDFC, ICICI, PNB).",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Challan Receipt Validator & Schedule IT Auto-Injector
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Record Paid Challan to Return (Schedule IT)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    StatutoryBadge(text = "CBDT Verified", tone = BadgeTone.STATUTORY)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Enter the BSR code & Challan Serial Number from your payment receipt to offset your return tax liability:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate600
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = bsrCodeInput,
                                        onValueChange = { bsrCodeInput = it.take(7) },
                                        label = { Text("BSR Code (7 Digits)") },
                                        placeholder = { Text("0002134") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = challanSerialInput,
                                        onValueChange = { challanSerialInput = it.take(5) },
                                        label = { Text("Challan No (5 Digits)") },
                                        placeholder = { Text("01234") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = depositDateInput,
                                    onValueChange = { depositDateInput = it },
                                    label = { Text("Date of Deposit (YYYY-MM-DD)") },
                                    placeholder = { Text("2026-03-15") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                if (verificationError != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(verificationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                }

                                if (isVerificationSuccess) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Challan 280 verified & added to Schedule IT!",
                                            color = EmeraldSuccess,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val validation = UpiChallan280IntentLauncher.validateChallanReceipt(
                                            bsrCode = bsrCodeInput,
                                            challanSerialNo = challanSerialInput,
                                            depositDateIso = depositDateInput,
                                            pan = pan,
                                            amountRupees = amountLong
                                        )
                                        if (validation.isValid) {
                                            verificationError = null
                                            isVerificationSuccess = true
                                            val entry = ScheduleItChallanEntry(
                                                bsrCode7Digits = bsrCodeInput,
                                                challanSerialNumber5Digits = challanSerialInput,
                                                dateOfDepositIso = depositDateInput,
                                                taxAmountPaidRupees = amountLong
                                            )
                                            onRecordChallan(entry)
                                        } else {
                                            verificationError = validation.errors.firstOrNull() ?: "Invalid Challan"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Verify & Save to Schedule IT")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


