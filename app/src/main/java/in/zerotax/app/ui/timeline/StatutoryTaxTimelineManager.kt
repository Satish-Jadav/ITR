package `in`.zerotax.app.ui.timeline

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.BadgeTone
import `in`.zerotax.app.ui.components.StatutoryBadge
import `in`.zerotax.app.ui.theme.*
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

/**
 * Statutory Indian Tax Timeline Milestone.
 */
data class TaxMilestone(
    val title: String,
    val dateText: String,
    val targetDate: LocalDate,
    val description: String,
    val statutorySection: String,
    val isCritical: Boolean = false
)

object StatutoryTaxTimelineManager {
    val currentAssessmentYear = "AY 2026-27"
    val currentFinancialYear = "FY 2025-26"

    // Statutory CBDT Milestones for Individuals (ITR-1 SAHAJ & ITR-4 SUGAM)
    fun getMilestones(referenceDate: LocalDate = LocalDate.now()): List<TaxMilestone> {
        val currentYear = referenceDate.year
        return listOf(
            TaxMilestone(
                title = "ITR Filing Window Opens",
                dateText = "1st April $currentYear",
                targetDate = LocalDate.of(currentYear, Month.APRIL, 1),
                description = "CBDT opens e-filing portal. Employers issue Form 16 Part A & B.",
                statutorySection = "Sec 139(1)"
            ),
            TaxMilestone(
                title = "Q1 Advance Tax Due",
                dateText = "15th June $currentYear",
                targetDate = LocalDate.of(currentYear, Month.JUNE, 15),
                description = "15% advance tax payable if net tax liability exceeds ₹10,000.",
                statutorySection = "Sec 208 / 211"
            ),
            TaxMilestone(
                title = "Mid-Season 30-Day Reminder",
                dateText = "1st July $currentYear",
                targetDate = LocalDate.of(currentYear, Month.JULY, 1),
                description = "Form 16 fully reflected in AIS/TIS. Ideal time to file for quick refunds.",
                statutorySection = "Sec 139(1)"
            ),
            TaxMilestone(
                title = "Urgent 6-Day Penalty Warning",
                dateText = "25th July $currentYear",
                targetDate = LocalDate.of(currentYear, Month.JULY, 25),
                description = "Final countdown before mandatory ₹5,000 late fee u/s 234F kicks in.",
                statutorySection = "Sec 234F",
                isCritical = true
            ),
            TaxMilestone(
                title = "Statutory Filing Deadline (ITR-1 / ITR-4)",
                dateText = "31st July $currentYear",
                targetDate = LocalDate.of(currentYear, Month.JULY, 31),
                description = "Last day to file timely original return without interest or late fee.",
                statutorySection = "Sec 139(1)",
                isCritical = true
            ),
            TaxMilestone(
                title = "Tax Audit Filing Deadline",
                dateText = "31st October $currentYear",
                targetDate = LocalDate.of(currentYear, Month.OCTOBER, 31),
                description = "Due date for businesses & professionals subject to audit u/s 44AB.",
                statutorySection = "Sec 44AB / 139(1)"
            ),
            TaxMilestone(
                title = "Belated / Revised Return Closes",
                dateText = "31st December $currentYear",
                targetDate = LocalDate.of(currentYear, Month.DECEMBER, 31),
                description = "Final statutory cutoff for belated u/s 139(4) or revised u/s 139(5).",
                statutorySection = "Sec 139(4) / 139(5)",
                isCritical = true
            )
        )
    }

    fun daysUntilJuly31(referenceDate: LocalDate = LocalDate.now()): Long {
        val target = LocalDate.of(referenceDate.year, Month.JULY, 31)
        return ChronoUnit.DAYS.between(referenceDate, target).coerceAtLeast(0)
    }
}

/**
 * Statutory Timeline Banner with real-time countdown and reminder scheduler modal.
 */
@Composable
fun StatutoryTimelineBanner(
    modifier: Modifier = Modifier,
    onOpenReminderModal: () -> Unit = {}
) {
    var showReminderModal by remember { mutableStateOf(false) }
    var remindersSubscribed by remember { mutableStateOf(true) }
    val daysLeft = remember { StatutoryTaxTimelineManager.daysUntilJuly31() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showReminderModal = true },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (daysLeft <= 15) AmberWarning.copy(alpha = 0.08f) else EnterpriseTeal50
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(
                if (daysLeft <= 15) AmberWarning.copy(alpha = 0.5f) else EnterpriseTeal300
            )
        )
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
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (daysLeft <= 15) AmberWarning.copy(alpha = 0.2f) else EnterpriseTeal100),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (daysLeft <= 15) "⚠️" else "🗓️", fontSize = 16.sp)
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ITR Due Date: July 31, 2026",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (daysLeft <= 15) AmberWarning else EnterpriseTeal900
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        StatutoryBadge(
                            text = "Sec 139(1)",
                            tone = if (daysLeft <= 15) BadgeTone.WARNING else BadgeTone.STATUTORY
                        )
                    }
                    Text(
                        text = "Filing window open • Avoid ₹5,000 late fee u/s 234F",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate600,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Countdown Pill
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (daysLeft <= 15) AmberWarning else EnterpriseTeal600
            ) {
                Text(
                    text = "⏱️ $daysLeft Days Left",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (daysLeft <= 15) Slate900 else PureWhite
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    if (showReminderModal) {
        StatutoryReminderModal(
            remindersEnabled = remindersSubscribed,
            onToggleReminders = { remindersSubscribed = it },
            onDismiss = { showReminderModal = false }
        )
    }
}

/**
 * Detailed Statutory Milestones & Notification Schedule Modal.
 */
@Composable
fun StatutoryReminderModal(
    remindersEnabled: Boolean,
    onToggleReminders: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val milestones = remember { StatutoryTaxTimelineManager.getMilestones() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏰", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "CBDT Tax Calendar & Reminders",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Assessment Year 2026-27 (FY 2025-26)",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Toggle notification switch
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EnterpriseTeal50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Local Device Notifications",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal900)
                            )
                            Text(
                                "Alerts at start, 30-day, and final 6-day deadlines. Zero cloud sync.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 11.sp)
                            )
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = onToggleReminders,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = EnterpriseTeal600
                            )
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Key Statutory Due Dates:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate700)
                )

                Spacer(Modifier.height(8.dp))

                milestones.take(5).forEach { milestone ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (milestone.isCritical) "🔴" else "🔵",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = milestone.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                )
                                Text(
                                    text = milestone.dateText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (milestone.isCritical) CrimsonDanger else EnterpriseTeal700
                                    )
                                )
                            }
                            Text(
                                text = milestone.description,
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 10.sp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600)
            ) {
                Text("Got It", fontWeight = FontWeight.Bold)
            }
        }
    )
}

