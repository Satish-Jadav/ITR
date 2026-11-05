package `in`.zerotax.app.ui.guide

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.BadgeTone
import `in`.zerotax.app.ui.components.StatutoryBadge
import `in`.zerotax.app.ui.theme.*

/**
 * Steps in the Guided Coachmark Walkthrough for Everyday Filers.
 */
enum class SahajTourStep(
    val stepNumber: Int,
    val title: String,
    val actionInstruction: String,
    val detailExplanation: String
) {
    STEP_1_SALARY(
        stepNumber = 1,
        title = "Check Your Salary & Earnings",
        actionInstruction = "Do this: Review your auto-filled salary from Form 16 below.",
        detailExplanation = "We decrypted your Form 16 PDF in private memory without uploading anything to the internet. Your gross salary and TDS are pre-filled."
    ),
    STEP_2_STANDARD_DEDUCTION(
        stepNumber = 2,
        title = "Government Standard Discount",
        actionInstruction = "Do this: Notice the ₹75,000 standard deduction automatically subtracted.",
        detailExplanation = "Section 16(ia) gives every salaried citizen a flat ₹75,000 deduction. You don't need bills or receipts — it reduces your tax automatically."
    ),
    STEP_3_REGIME_CHOICE(
        stepNumber = 3,
        title = "Smart Regime Comparison",
        actionInstruction = "Do this: Check the New vs Old Regime savings badge.",
        detailExplanation = "We calculated your tax under both regimes. We automatically selected the regime that saves you the highest money."
    ),
    STEP_4_BIOMETRIC_FILE(
        stepNumber = 4,
        title = "Biometric 1-Tap e-Filing",
        actionInstruction = "Do this: Tap 'Authenticate with Biometrics to File' at the bottom.",
        detailExplanation = "Verify with your fingerprint or face to sign your ITR package and securely submit it directly to the Income Tax Department."
    )
}

/**
 * Interactive Coachmark Mini-Popup View.
 * Displays a floating, contextual guide card with step arrows and clear instructions.
 */
@Composable
fun ZeroTaxCoachmarkPopup(
    currentStep: SahajTourStep,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSteps = SahajTourStep.entries.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, EnterpriseTeal400)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Step Pill + Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EnterpriseTeal600
                    ) {
                        Text(
                            text = "Step ${currentStep.stepNumber} of $totalSteps",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = currentStep.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Slate400,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Action Instruction Callout
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EnterpriseTeal900.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, EnterpriseTeal700.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = EnterpriseTeal200,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = currentStep.actionInstruction,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EnterpriseTeal200
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Detail Explanation in Plain English
            Text(
                text = currentStep.detailExplanation,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate300,
                    lineHeight = 18.sp
                )
            )

            Spacer(Modifier.height(14.dp))

            // Step Progress Dots & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Progress Dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SahajTourStep.entries.forEach { step ->
                        Box(
                            modifier = Modifier
                                .size(if (step == currentStep) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step == currentStep) EnterpriseTeal400
                                    else if (step.stepNumber < currentStep.stepNumber) EmeraldSuccess
                                    else Slate700
                                )
                        )
                    }
                }

                // Control Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentStep.stepNumber > 1) {
                        OutlinedButton(
                            onClick = onPrevious,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("← Back", color = Slate300, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EnterpriseTeal600),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (currentStep.stepNumber == totalSteps) "Finish Tour" else "Next Step →",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

