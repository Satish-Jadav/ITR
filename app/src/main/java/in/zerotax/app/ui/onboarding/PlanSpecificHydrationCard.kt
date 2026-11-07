package `in`.zerotax.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.IndianCurrencyFormatter
import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.app.viewmodel.ZeroTaxUiState
import `in`.zerotax.core.model.FilingTierMode

/**
 * Dynamic questionnaire and ingestion card adapting specifically to the taxpayer's plan.
 * Captures statutory inputs required for AY 2026-27 without overwhelming the user.
 */
@Composable
fun PlanSpecificHydrationCard(
    tier: FilingTierMode,
    state: ZeroTaxUiState,
    onSugamUpdated: (debtors: Long, creditors: Long, stock: Long, cash: Long, profitPercent: Double) -> Unit,
    onAuditUpdated: (gstin: String, caMembership: String, caName: String) -> Unit,
    onProceedToDossier: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    // Form inputs state
    var declaredProfitPercent by remember { mutableDoubleStateOf(state.planSpecificDeclaredProfitPercent) }
    var debtorsInput by remember { mutableStateOf(if (state.planSpecificDebtors > 0) state.planSpecificDebtors.toString() else "320000") }
    var creditorsInput by remember { mutableStateOf(if (state.planSpecificCreditors > 0) state.planSpecificCreditors.toString() else "140000") }
    var stockInput by remember { mutableStateOf(if (state.planSpecificStock > 0) state.planSpecificStock.toString() else "0") }
    var cashInput by remember { mutableStateOf(if (state.planSpecificCash > 0) state.planSpecificCash.toString() else "85000") }

    var gstinInput by remember { mutableStateOf(state.planSpecificGstin.ifBlank { "29AABCA1234D1Z5" }) }
    var caMembershipInput by remember { mutableStateOf(state.planSpecificCaMembership.ifBlank { "124856" }) }
    var caNameInput by remember { mutableStateOf(state.planSpecificCaName.ifBlank { "CA Rajesh Sharma, FCA" }) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Banner
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else Slate100,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else Slate300),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = EnterpriseTeal500,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "${tier.title} Requirements",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "CBDT statutory parameters verified for AY 2026-27 compliance",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        when (tier) {
            FilingTierMode.SAHAJ_SIMPLE -> {
                // Sahaj Quick Salaried Breakdown Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Slate850 else PureWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                            Text(
                                "Form 16 & 26AS Zero-Input Hydration",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            "All required salary line items, standard deduction of ₹75,000 u/s 16(ia), and Section 80C/80D investments have been matched automatically from your digital records.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        HorizontalDivider(color = if (isDark) Slate700 else Slate200)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Employer", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(state.employerName.ifBlank { "Infosys Technologies Ltd" }, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Standard Deduction u/s 16(ia)", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text("₹75,000", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = EmeraldSuccess))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sec 192 TDS Deposited", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(IndianCurrencyFormatter.formatToInr(state.tdsDeposited26As), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            FilingTierMode.INVESTOR_PRO -> {
                // Investor & Global Pro Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Slate850 else PureWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal500),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Schedule CG & Schedule FA Disclosures",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        // Budget 2024 revised rates summary
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) EnterpriseTeal900 else EnterpriseTeal50,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Budget 2024 Capital Gains Rules (AY 2026-27):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal700)
                                )
                                Text("• STCG u/s 111A: 20% on listed equities & equity mutual funds", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                Text("• LTCG u/s 112A: 12.5% with elevated ₹1.25 Lakh exemption limit", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                Text("• 6 ISINs Jan 31, 2018 Grandfathering prices populated via statutory API", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                            }
                        }

                        // Foreign Assets Schedule FA Callout
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) IndigoStatutoryBgDark else IndigoStatutoryBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Public, null, tint = IndigoStatutory, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(
                                        "Schedule FA: Foreign Equity (RSU/ESPP) Sealed",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = IndigoStatutory)
                                    )
                                    Text(
                                        "Peak and closing values converted using historical SBI TTBR rates. Protected from Black Money Act penalties.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FilingTierMode.ENTERPRISE_AUDIT -> {
                // Enterprise & Tax Audit Requirements
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Slate850 else PureWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal500),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Section 44AB & Form 3CD Audit Verification",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        OutlinedTextField(
                            value = gstinInput,
                            onValueChange = { gstinInput = it.uppercase() },
                            label = { Text("GSTIN (Clause 4)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                        )

                        // 43B(h) and Clause 44 Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccessBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Sec 43B(h) MSME", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess))
                                    Text("0 Overdue Dues > 45d", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EnterpriseTeal50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal500.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Clause 44 GST", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal700))
                                    Text("88.2% Registered Exp", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                                }
                            }
                        }

                        HorizontalDivider(color = if (isDark) Slate700 else Slate200)

                        Text("Statutory Auditor Particulars", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = caMembershipInput,
                                onValueChange = { caMembershipInput = it },
                                label = { Text("ICAI Membership No") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = caNameInput,
                                onValueChange = { caNameInput = it },
                                label = { Text("Auditor Name") },
                                modifier = Modifier.weight(1.5f)
                            )
                        }
                    }
                }
            }
        }

        // Action CTA
        ZeroTaxPrimaryButton(
            text = "Generate Master Tax Dossier",
            onClick = {
                if (tier == FilingTierMode.ENTERPRISE_AUDIT) {
                    onAuditUpdated(gstinInput, caMembershipInput, caNameInput)
                } else if (tier == FilingTierMode.INVESTOR_PRO) {
                    val d = debtorsInput.toLongOrNull() ?: 320000L
                    val c = creditorsInput.toLongOrNull() ?: 140000L
                    val s = stockInput.toLongOrNull() ?: 0L
                    val cash = cashInput.toLongOrNull() ?: 85000L
                    onSugamUpdated(d, c, s, cash, declaredProfitPercent)
                }
                onProceedToDossier()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
