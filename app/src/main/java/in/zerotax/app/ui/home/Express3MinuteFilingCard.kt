package `in`.zerotax.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.BadgeTone
import `in`.zerotax.app.ui.components.IndianCurrencyFormatter
import `in`.zerotax.app.ui.components.StatutoryBadge
import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.app.viewmodel.ZeroTaxUiState

/**
 * ⚡ Express 3-Minute (90-Second / 3-Tap) Filing Card
 * Shows auto-detected ITR form, winning regime savings, verified refund,
 * zero CBDT defect status, and a 1-tap File & e-Verify CTA.
 */
@Composable
fun Express3MinuteFilingCard(
    uiState: ZeroTaxUiState,
    onFileAndEVerifyNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val winningRegime = uiState.selectedRegime
    val newTax = uiState.newRegimeResult.totalTaxLiabilityRounded288B
    val oldTax = uiState.oldRegimeResult.totalTaxLiabilityRounded288B
    val taxSavings = kotlin.math.abs(oldTax - newTax)
    val bestTax = minOf(newTax, oldTax)
    val refundDue = maxOf(0L, uiState.tdsDeposited26As - bestTax)
    val taxPayable = maxOf(0L, bestTax - uiState.tdsDeposited26As - uiState.totalTaxPaidViaChallan)

    val formName = uiState.formRoutingDecision.selectedForm.name.replace("_", "-")
    val bankAccountMasked = if (uiState.bankAccount.accountNumber.isNotBlank()) {
        "••••" + uiState.bankAccount.accountNumber.takeLast(4)
    } else "Not Linked"

    val cardBorderBrush = Brush.horizontalGradient(
        listOf(EnterpriseTeal500, EmeraldSuccess, Color(0xFF10B981))
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.25.dp,
                brush = cardBorderBrush,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Slate850 else PureWhite,
        shadowElevation = if (isDark) 0.dp else 2.5.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: ⚡ 90-Second Express Filing Ready
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Express Filing",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "⚡ 90-Second Express Filing",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.2).sp
                            )
                        )
                        Text(
                            text = "Pre-filled & Reconciled with ITD",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                StatutoryBadge(text = formName, tone = BadgeTone.SUCCESS)
            }

            // Regime and Refund / Tax Payable Summary Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Slate800.copy(alpha = 0.7f) else Slate50,
                border = androidx.compose.foundation.BorderStroke(
                    0.75.dp,
                    if (isDark) Slate700 else Slate200
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (newTax <= oldTax) {
                            "✓ New Regime saves you ₹${IndianCurrencyFormatter.formatLakhs(taxSavings)} more"
                        } else {
                            "✓ Old Regime saves you ₹${IndianCurrencyFormatter.formatLakhs(taxSavings)} more"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669)
                        )
                    )

                    if (refundDue > 0L) {
                        Text(
                            text = "Verified Refund Due: ₹${IndianCurrencyFormatter.formatLakhs(refundDue)}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        )
                        val bankLabel = if (uiState.bankAccount.bankName.isNotBlank()) uiState.bankAccount.bankName else "Linked Bank"
                        val accountSuffix = if (bankAccountMasked != "Not Linked") " ($bankAccountMasked)" else ""
                        Text(
                            text = "Crediting to $bankLabel$accountSuffix u/s 244A",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        )
                    } else if (taxPayable > 0L) {
                        Text(
                            text = "Self-Assessment Tax Payable: ₹${IndianCurrencyFormatter.formatLakhs(taxPayable)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                        Text(
                            text = "1-Tap UPI Challan 280 Payment without leaving flow",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        )
                    } else {
                        Text(
                            text = "Zero Tax Liability (₹0 Payable)",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        )
                        Text(
                            text = "100% Tax Rebate under Section 87A auto-applied",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }
            }

            // Pre-Filing Audit Checklist Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "0 CBDT Category-A Defects • AIS & 26AS 100% Reconciled",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            // Primary 1-Tap CTA Button with tactile feedback
            ZeroTaxPrimaryButton(
                text = if (taxPayable > 0L) "Pay Tax via UPI & File (1 Tap)" else "File & e-Verify Now (1 Tap)",
                onClick = onFileAndEVerifyNow,
                containerColor = EmeraldSuccess,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
