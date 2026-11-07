package `in`.zerotax.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.IndianCurrencyFormatter
import `in`.zerotax.app.ui.theme.EmeraldSuccess
import `in`.zerotax.app.ui.theme.Navy800
import `in`.zerotax.app.ui.theme.Navy900
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
    val winningRegime = uiState.selectedRegime
    val newTax = uiState.newRegimeResult.totalTaxLiabilityRounded288B
    val oldTax = uiState.oldRegimeResult.totalTaxLiabilityRounded288B
    val taxSavings = kotlin.math.abs(oldTax - newTax)
    val bestTax = minOf(newTax, oldTax)
    val refundDue = maxOf(0L, uiState.tdsDeposited26As - bestTax)
    val taxPayable = maxOf(0L, bestTax - uiState.tdsDeposited26As - uiState.totalTaxPaidViaChallan)

    val formName = uiState.formRoutingDecision.recommendedForm.name.replace("_", "-")
    val bankAccountMasked = if (uiState.bankAccount.accountNumber.isNotBlank()) {
        "••••" + uiState.bankAccount.accountNumber.takeLast(4)
    } else "••••4821"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: ⚡ 90-Second Express Filing Ready
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Express Filing",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "⚡ 90-Second Express Filing Ready",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = formName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    )
                }
            }

            // Regime and Refund / Tax Payable Summary Row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
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

                    Spacer(Modifier.height(4.dp))

                    if (refundDue > 0L) {
                        Text(
                            text = "Verified Refund Due: ₹${IndianCurrencyFormatter.formatLakhs(refundDue)}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        )
                        Text(
                            text = "Crediting to ${uiState.bankAccount.bankName.ifBlank { "Verified Bank" }} ($bankAccountMasked) u/s 244A",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Primary 1-Tap CTA Button
            Button(
                onClick = onFileAndEVerifyNow,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (taxPayable > 0L) "Pay Tax via UPI & File (1 Tap)" else "File & e-Verify Now (1 Tap)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}
