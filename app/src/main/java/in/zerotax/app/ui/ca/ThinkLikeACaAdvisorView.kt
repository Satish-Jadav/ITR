package `in`.zerotax.app.ui.ca

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.*
import `in`.zerotax.core.taxengine.ca.ThinkLikeACaDeductionMaximizerEngine
import `in`.zerotax.core.taxengine.ca.ThinkLikeACaDeductionMaximizerEngine.CaReviewInput

/**
 * "Think Like a CA" Interactive Statutory Deduction & Refund Maximizer Screen.
 * Uncovers unexhausted deductions across all 5 income heads, bringing taxable
 * income below Section 87A rebate ceilings to maximize tax refund instead of paying tax.
 *
 * Adheres strictly to Zero-Emoji design rules using Compose Vector Icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThinkLikeACaAdvisorView(
    pan: String,
    grossSalary: Long,
    tdsPrepaid: Long,
    onBack: () -> Unit,
    onApplyDeduction: (DeductionOpportunity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<DeductionCategory?>(null) }
    var claimedSections by remember { mutableStateOf(setOf<String>()) }

    // Run the CA Heuristic Optimization Engine
    val report = remember(grossSalary, tdsPrepaid, claimedSections) {
        val claimedMap = claimedSections.associateWith { 50_000L }
        ThinkLikeACaDeductionMaximizerEngine.optimizeTaxAndMaximizeRefund(
            CaReviewInput(
                pan = pan,
                grossSalary = grossSalary,
                tdsPrepaid = tdsPrepaid,
                employerOffersNps80CCD2 = true,
                hasHomeLoan = true,
                homeLoanInterestPaid = 2_00_000L,
                paysRent = true,
                annualRentPaid = 3_00_000L,
                parentsAreSeniorCitizens = true,
                otherSourcesInterest = 18_000L,
                existingDeductionsClaimed = claimedMap
            )
        )
    }

    val filteredRecommendations = if (selectedCategory != null) {
        report.recommendations.filter { it.category == selectedCategory }
    } else {
        report.recommendations
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Think Like a CA: Maximizer",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                            Spacer(Modifier.width(8.dp))
                            StatutoryBadge(text = "Refund First", tone = BadgeTone.SUCCESS)
                        }
                        Text(
                            text = "Aggressive Legal Statutory Tax Planning u/s 87A",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Slate700)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate100)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // HERO CARD: Current vs CA Optimized Outcome
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600)
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
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = EnterpriseTeal700,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "CA Refund Strategy Summary",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                                )
                            }
                            if (report.bringsBelowRebate87A) {
                                StatutoryBadge(text = "Rebate 87A: ZERO TAX", tone = BadgeTone.SUCCESS)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Metric Comparison Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Current Tax Liability", style = TextStyle(fontSize = 11.sp, color = Slate500))
                                    Spacer(Modifier.height(4.dp))
                                    Text("₹${"%,d".format(report.baselineTaxLiability)}", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CrimsonDanger))
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccessBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Optimized Tax Liability", style = TextStyle(fontSize = 11.sp, color = EmeraldSuccess))
                                    Spacer(Modifier.height(4.dp))
                                    Text("₹${"%,d".format(report.optimizedTaxLiability)}", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldSuccess))
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Expected Refund & Total Saved Highlight
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EnterpriseTeal50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("TDS Prepaid by Employer:", style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    Text("₹${"%,d".format(report.baselineTdsPrepaid)}", style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(if (report.optimizedRefundDue > 0) "Your Bank Account Refund:" else "Total Tax Saved:", style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                                    Text(
                                        text = if (report.optimizedRefundDue > 0) "₹${"%,d".format(report.optimizedRefundDue)}" else "₹${"%,d".format(report.totalTaxSavedRupees)}",
                                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EmeraldSuccess)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = report.executiveSummary,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        )
                    }
                }
            }

            // Category Filter Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Deduction Avenues Discovered (${report.recommendations.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    if (selectedCategory != null) {
                        Text(
                            text = "Clear Filter",
                            style = MaterialTheme.typography.labelSmall.copy(color = EnterpriseTeal700, fontWeight = FontWeight.Bold),
                            modifier = Modifier.clickable { selectedCategory = null }
                        )
                    }
                }
            }

            // Recommendation Cards
            items(filteredRecommendations) { opportunity ->
                val isClaimed = claimedSections.contains(opportunity.sectionCode)
                DeductionOpportunityCard(
                    opportunity = opportunity,
                    isClaimed = isClaimed,
                    onToggleClaim = {
                        claimedSections = if (isClaimed) {
                            claimedSections - opportunity.sectionCode
                        } else {
                            claimedSections + opportunity.sectionCode
                        }
                        onApplyDeduction(opportunity)
                    }
                )
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun DeductionOpportunityCard(
    opportunity: DeductionOpportunity,
    isClaimed: Boolean,
    onToggleClaim: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isClaimed) 1.5.dp else 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(if (isClaimed) EmeraldSuccess else Slate200)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = EnterpriseTeal50,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, EnterpriseTeal600)
                    ) {
                        Text(
                            text = opportunity.sectionCode,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EnterpriseTeal900)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = opportunity.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                        maxLines = 1
                    )
                }
                if (opportunity.isApplicableInNewRegime) {
                    StatutoryBadge(text = "New Regime Valid", tone = BadgeTone.SUCCESS)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = opportunity.legalExplanation,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
            )

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Max Eligible Claim", style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text("₹${"%,d".format(opportunity.maxEligibleAmount)}", style = TextStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Estimated Tax Saved", style = TextStyle(fontSize = 10.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold))
                        Text("+ ₹${"%,d".format(opportunity.estimatedTaxSavedRupees)}", style = TextStyle(fontWeight = FontWeight.Bold, color = EmeraldSuccess, fontSize = 14.sp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = Slate500, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Proof: ${opportunity.requiredDocumentation}",
                    style = TextStyle(fontSize = 10.sp, color = Slate500),
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onToggleClaim,
                colors = ButtonDefaults.buttonColors(containerColor = if (isClaimed) EmeraldSuccess else EnterpriseTeal600),
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isClaimed) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isClaimed) "Deduction Claim Applied" else "Add Deduction to My Return",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

