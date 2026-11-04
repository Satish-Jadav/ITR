package `in`.zerotax.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

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

