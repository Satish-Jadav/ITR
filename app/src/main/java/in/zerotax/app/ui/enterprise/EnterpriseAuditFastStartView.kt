package `in`.zerotax.app.ui.enterprise

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.app.ui.timeline.StatutoryTimelineBanner
import `in`.zerotax.core.model.Rupee

/**
 * Fast-Start Executive Hub for Enterprise, Corporate & Tax Audit Filers.
 * Covers ITR-2 (Capital Gains & Foreign Assets), ITR-3 (Business & CA Audit),
 * ITR-5 (Partnership & LLP u/s 40(b)), ITR-6 (Company MAT u/s 115JB),
 * and ITR-7 (Trust & NGO u/s 11/12).
 */
@Composable
fun EnterpriseAuditFastStartView(
    pan: String,
    totalIncome: Long,
    taxLiability: Long,
    tdsClaimed: Long,
    onOpenCorporateSuite: () -> Unit,
    onOpenBusinessAudit: () -> Unit,
    onOpenNriSuite: () -> Unit,
    onOpenToolkit: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenNotices: () -> Unit,
    onLaunchPortalAssistant: () -> Unit,
    onOpenTaxPaymentHub: () -> Unit,
    onRunAudit: () -> Unit,
    onOpenPortalSync: () -> Unit = {},
    onOpenShareWithCa: () -> Unit = {},
    onOpenThinkLikeCa: () -> Unit = {},
    onOpenCaPortal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatutoryTimelineBanner()

        // 1-TAP IN-APP ITD PORTAL SYNC CARD
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = EnterpriseTeal50,
            border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal500),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPortalSync() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = EnterpriseTeal700,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Sync Corporate 26AS, AIS/TIS & Advance Tax",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                        )
                        Text(
                            "Pull verified corporate prepaid taxes, Challan 280 & MAT credits in-app.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.sp)
                        )
                    }
                }
                StatutoryBadge(text = "1-Tap Sync →", tone = BadgeTone.SUCCESS)
            }
        }

        // CHARTERED ACCOUNTANT (CA) AUDIT & CLIENT PAIRING DESK
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate300)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = IndigoStatutory, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Chartered Accountant & Auditor Desk", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                    }
                    StatutoryBadge(text = "Sec 44AB Ready", tone = BadgeTone.SUCCESS)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Pair with your CA using an on-device 6-digit code for Form 3CD/3CA verification, or maximize your legal deductions to get a full refund.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenShareWithCa,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IndigoStatutory)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = IndigoStatutory, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share with CA", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoStatutory))
                        }
                    }

                    Button(
                        onClick = onOpenThinkLikeCa,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Think Like a CA", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite))
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenCaPortal,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal600)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = EnterpriseTeal700, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CA Portal", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal700))
                        }
                    }
                }
            }
        }

        // EXECUTIVE SUITE LAUNCHER CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, EnterpriseTeal500)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Enterprise & Statutory Audit Hub",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PureWhite)
                            )
                            Text(
                                "ITR-2 to ITR-7 • Sec 44AB, MAT 115JB, Sec 40(b) & Trusts",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 11.sp)
                            )
                        }
                    }
                    StatutoryBadge(text = "Audit Ready", tone = BadgeTone.SUCCESS)
                }

                Spacer(Modifier.height(14.dp))

                // Fast Launch Action Grid (2-column layout)
                Text(
                    text = "Fast-Start Statutory Desks:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate300)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Desk 1: Corporate & Trust Suite (ITR-5, 6, 7)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate800,
                        border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal600),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenCorporateSuite() }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Apartment,
                                    contentDescription = null,
                                    tint = EnterpriseTeal300,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Corporate & Trust", fontWeight = FontWeight.Bold, color = EnterpriseTeal300, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text("ITR-5 Firm, ITR-6 MAT, ITR-7 Trust", color = Slate300, fontSize = 10.sp)
                            Spacer(Modifier.height(6.dp))
                            StatutoryBadge(text = "Launch Desk →", tone = BadgeTone.STATUTORY)
                        }
                    }

                    // Desk 2: Tax Audit & 44AB Suite
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate800,
                        border = androidx.compose.foundation.BorderStroke(1.dp, IndigoStatutory),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenBusinessAudit() }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = IndigoStatutoryBg,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Sec 44AB Tax Audit", fontWeight = FontWeight.Bold, color = IndigoStatutoryBg, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text("Form 3CD 44 Clauses & Balance Sheet", color = Slate300, fontSize = 10.sp)
                            Spacer(Modifier.height(6.dp))
                            StatutoryBadge(text = "Open 3CD →", tone = BadgeTone.SUCCESS)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Desk 3: NRI & DTAA Desk
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate800,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenNriSuite() }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("NRI & DTAA Relief", fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text("Sec 90/91, Form 10F, Schedule FA", color = Slate400, fontSize = 10.sp)
                        }
                    }

                    // Desk 4: Notice Defense Desk
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate800,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenNotices() }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Notice Defense", fontWeight = FontWeight.Bold, color = AmberWarning, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text("Sec 143(1), 139(9), 154 Rectification", color = Slate400, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // STATUTORY RECONCILIATION SUMMARY
        ZeroTaxCard {
            ZeroTaxSectionHeader(
                title = "Statutory Return Summary (CBDT Schema)",
                subtitle = "Deterministic multi-head aggregation under Chapter IV & VI-A",
                badgeText = "AY 2026-27",
                badgeTone = BadgeTone.STATUTORY
            )

            Spacer(Modifier.height(12.dp))

            EnterpriseLedgerRow(label = "Total Income u/s 288A", amount = totalIncome)
            EnterpriseLedgerRow(label = "Tax Liability u/s 288B", amount = taxLiability)
            EnterpriseLedgerRow(label = "Prepaid Taxes (TDS / TCS)", amount = tdsClaimed)
            EnterpriseLedgerRow(
                label = if (tdsClaimed >= taxLiability) "Net Refund Due" else "Net Tax Payable",
                amount = kotlin.math.abs(tdsClaimed - taxLiability),
                isHighlighted = true
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenToolkit,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal600)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = EnterpriseTeal700,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Tax Toolkit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EnterpriseTeal700)
                    }
                }
                OutlinedButton(
                    onClick = onOpenAnalytics,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Radar Analytics", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    }
                }
            }
        }

        // FAST-ACTION BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenTaxPaymentHub,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Pay Challan 280", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                }
            }

            Button(
                onClick = onRunAudit,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Run 6-Gate Audit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PureWhite))
                }
            }
        }

        ZeroTaxPrimaryButton(
            text = "Direct In-App ITR e-Filing Suite & Gemini Copilot",
            onClick = onLaunchPortalAssistant,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )
    }
}

