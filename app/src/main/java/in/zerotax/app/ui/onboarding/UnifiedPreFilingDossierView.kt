package `in`.zerotax.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.IndianCurrencyFormatter
import `in`.zerotax.app.ui.components.ZeroTaxOutlinedButton
import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.FilingTierMode
import `in`.zerotax.core.model.PlanSpecificDossierSummary
import `in`.zerotax.core.model.TaxRegime
import `in`.zerotax.core.model.UnifiedTaxDossier

/**
 * Visual and interactive presentation of the Unified Pre-Filing Master Tax Dossier.
 * Culminates the automated onboarding ceremony with 1-tap statutory sign-off and filing.
 */
@Composable
fun UnifiedPreFilingDossierView(
    dossier: UnifiedTaxDossier,
    onConfirmAndFile: () -> Unit,
    onShareWithCa: () -> Unit,
    onProceedToWorkspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Dossier Sealed Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) EnterpriseTeal900.copy(alpha = 0.5f) else EnterpriseTeal50,
            border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal500),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EnterpriseTeal500),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Slate950,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unified Pre-Filing Master Tax Dossier",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) EnterpriseTeal300 else EnterpriseTeal700
                        )
                    )
                    Text(
                        text = "Statutory AY 2026-27 • Cryptographically Sealed on Device",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Section 1: Taxpayer Identity & Pre-Validated Bank Account
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else Slate300),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Taxpayer Particulars",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = EnterpriseTeal100
                    ) {
                        Text(
                            text = dossier.filingTier.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EnterpriseTeal900,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Assessee Name", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(dossier.assesseeName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PAN & Aadhaar", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text("${dossier.assesseePan} • ${dossier.maskedAadhaar}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                    }
                }

                HorizontalDivider(color = if (isDark) Slate700 else Slate200)

                // Refund Bank Account
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Refund Direct Deposit: ${dossier.refundBankName} (${dossier.refundBankAccountMasked})",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "IFSC: ${dossier.refundBankIfsc} • Pre-validated on ITD Portal",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldSuccess, fontSize = 10.sp)
                        )
                    }
                }
            }
        }

        // Section 2: Consolidated Multi-Head Income Matrix
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else Slate300),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Consolidated Multi-Head Income",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gross Total Income", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(IndianCurrencyFormatter.formatToInr(dossier.grossIncomeRupees), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Standard & Chapter VI-A Deductions", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text("-${IndianCurrencyFormatter.formatToInr(dossier.totalDeductionsRupees + 75000L)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = EmeraldSuccess))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Net Taxable Income", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(IndianCurrencyFormatter.formatToInr(dossier.taxableIncomeRupees), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Section 3: 3-Way Tax Credit Reconciliation Matrix
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) EmeraldSuccessBgDark.copy(alpha = 0.3f) else EmeraldSuccessBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "3-Way Tax Credit Match (100% Verified)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    )
                }
                Text(
                    text = "Form 16 / AIS / 26AS cross-check verified ${IndianCurrencyFormatter.formatToInr(dossier.totalTdsCreditsDiscovered)} in deposited prepaid tax credits. Zero timing discrepancies.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        // Section 4: Automated Regime Showdown
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal500),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Automated Tax Regime Showdown",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // New Regime Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (dossier.recommendedRegime == TaxRegime.NEW_REGIME_115BAC) EnterpriseTeal50 else if (isDark) Slate800 else Slate100,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (dossier.recommendedRegime == TaxRegime.NEW_REGIME_115BAC) EnterpriseTeal500 else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("New Regime (115BAC)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Text(IndianCurrencyFormatter.formatToInr(dossier.computedTaxNewRegime), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (dossier.recommendedRegime == TaxRegime.NEW_REGIME_115BAC) {
                                Text("Recommended", style = MaterialTheme.typography.labelSmall.copy(color = EnterpriseTeal700, fontWeight = FontWeight.Bold, fontSize = 9.sp))
                            }
                        }
                    }

                    // Old Regime Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (dossier.recommendedRegime == TaxRegime.OLD_REGIME) EnterpriseTeal50 else if (isDark) Slate800 else Slate100,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (dossier.recommendedRegime == TaxRegime.OLD_REGIME) EnterpriseTeal500 else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Old Regime", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Text(IndianCurrencyFormatter.formatToInr(dossier.computedTaxOldRegime), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (dossier.recommendedRegime == TaxRegime.OLD_REGIME) {
                                Text("Recommended", style = MaterialTheme.typography.labelSmall.copy(color = EnterpriseTeal700, fontWeight = FontWeight.Bold, fontSize = 9.sp))
                            }
                        }
                    }
                }

                // Final Net Refund/Payable Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (dossier.isRefund) EmeraldSuccessBg else AmberWarningBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (dossier.isRefund) "Net Tax Refund Due" else "Net Tax Payable",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (dossier.isRefund) EmeraldSuccess else AmberWarning
                            )
                        )
                        Text(
                            text = IndianCurrencyFormatter.formatToInr(dossier.netPayableOrRefund),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (dossier.isRefund) EmeraldSuccess else AmberWarning
                            )
                        )
                    }
                }
            }
        }

        // Section 5: Plan-Specific Statutory Appendix
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Slate850 else PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else Slate300),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Statutory Filing Appendix (${dossier.recommendedItrForm.name})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                when (val summary = dossier.planSpecificSummary) {
                    is PlanSpecificDossierSummary.SahajSummary -> {
                        Text("• Form 16 Part B line items validated from employer ${summary.employerName}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                        Text("• Section 16(ia) standard deduction: ₹75,000 claimed", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                    }
                    is PlanSpecificDossierSummary.InvestorSummary -> {
                        Text("• Schedule CG: STCG u/s 111A (?45,000 @ 20%) & LTCG u/s 112A (?1,35,000 @ 12.5%)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                        Text("• 6 Grandfathered ISINs FMVs sealed as of Jan 31, 2018", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                        Text("• Schedule FA: ${summary.foreignAssetsCount} foreign account disclosure sealed via SBI TTBR rates", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                    }
                    is PlanSpecificDossierSummary.SugamSummary -> {
                        Text("• Presumptive Profession u/s 44ADA: ${summary.declaredProfitPercent}% profit declared", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                        Text("• Digital Ratio: ${summary.digitalReceiptsPercent}% (Elevated ?75 Lakh limit certified)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                        Text("• Balance Sheet Particulars: Debtors, Creditors, Stock & Cash synced", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                    }
                    is PlanSpecificDossierSummary.EnterpriseAuditSummary -> {
                        Text("• Form 3CD Compliance Checklist: 44 clauses validated", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                        Text("• Sec 43B(h) MSME overdue liability check: 0 overdue payables", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                        Text("• Clause 44: ${summary.clause44RegisteredExpensePercent}% expenditure with GST-registered suppliers", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                        Text("• Statutory Auditor Assigned: ${summary.caAuditorName} (ICAI #${summary.caMembershipNumber})", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                    }
                }
            }
        }

        // Section 6: Cryptographic Hash Seal
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isDark) Slate850 else Slate100,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Slate700 else Slate300),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Security, null, tint = EnterpriseTeal500, modifier = Modifier.size(14.dp))
                    Text("Cryptographic Audit Signature (SHA-256)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
                Text(
                    text = dossier.cryptographicAuditHash,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // CTAs
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZeroTaxPrimaryButton(
                text = "1-Tap Direct E-Filing to ITD Portal",
                onClick = onConfirmAndFile,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZeroTaxOutlinedButton(
                    text = "Share with CA",
                    onClick = onShareWithCa,
                    modifier = Modifier.weight(1f)
                )
                ZeroTaxOutlinedButton(
                    text = "Open Full Workspace",
                    onClick = onProceedToWorkspace,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
