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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.InstitutionPdfPasswordRegistry
import `in`.zerotax.core.model.InstitutionPdfPasswordRule
import `in`.zerotax.core.model.Rupee

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
    val allRules = remember { InstitutionPdfPasswordRegistry.rules }
    val filteredRules = remember(selectedCategory) {
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
                        Text("🔑", fontSize = 18.sp)
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
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
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
                                    text = "${rule.exampleDescription} ➔ ${rule.exampleInput}",
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
                    Text("🛡️", fontSize = 16.sp)
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
            Text(
                text = if (isPassed) "✓" else "!",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Slate900)
                )
                Text(
                    text = statutoryRef,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Slate500)
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = Slate600
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
                    Text("💡", fontSize = 16.sp)
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
                    Text("₹${Rupee.formatInr(taxBeforeRelief)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Slate700))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Marginal Relief", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                    Text("-₹${Rupee.formatInr(marginalReliefAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Actual Tax Payable", style = MaterialTheme.typography.labelSmall, color = Slate900)
                    Text("₹${Rupee.formatInr(finalTaxBeforeCess)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
                }
            }
        }
    }
}


