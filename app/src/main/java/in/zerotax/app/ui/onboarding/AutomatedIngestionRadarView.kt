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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.DiscreteStatutoryStepper
import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import `in`.zerotax.app.ui.navigation.OnboardingTap
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.IngestionStreamItem

/**
 * Screen 3: Data Ingestion & Hydration Radar (Figma 4:380)
 * Exact match to Figma frame:
 * - Stepper: Step 1 active (1. Ingestion -> 2. Compute -> 3. Audit -> 4. File)
 * - Screen introduction: "Your financial picture, unified."
 * - Ingestion Card 1: Account Aggregator (AA) - Reserve Bank Account Aggregator [CONNECTED]
 *   HDFC ••4812, ICICI ••9021, SBI + "142 Interest credits & dividends detected"
 * - Ingestion Card 2: AIS & TIS Official Sync - Annual Information Statement [SYNCED]
 *   Salaries ₹18.5L • Securities ₹4.2L • High-value cash ₹0 + [✓ 100% matched with 26AS tax credits]
 * - Bottom Sticky CTA: "Generate Unified Pre-Filing Dossier"
 */
@Composable
fun AutomatedIngestionRadarView(
    streams: List<IngestionStreamItem>,
    isIngesting: Boolean,
    onProceed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Screen Introduction (Figma 4:973)
        Column {
            Text(
                text = "Your financial picture, unified.",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) PureWhite else Slate900
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Live statutory feeds decrypted directly into secure device memory",
                style = TextStyle(
                    fontSize = 13.5.sp,
                    color = if (isDark) Slate400 else Slate600
                )
            )
        }

        // 2. Stepper Component (Figma 4:976)
        DiscreteStatutoryStepper(
            currentTap = OnboardingTap.Tap1IdentityConsent,
            onStepClick = {}
        )

        // 3. Ingestion Card 1: Account Aggregator Connection (Figma 4:992)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Slate850 else PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(if (isDark) Slate700 else Slate300)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row: AA Icon + Title + Status Pill
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Slate800 else SageMintPill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Bank",
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Account Aggregator",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) PureWhite else Slate900
                                )
                            )
                            Text(
                                text = "Reserve Bank of India Regulated Entity",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = if (isDark) Slate400 else Slate600
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SageMintPill)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "CONNECTED",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        )
                    }
                }

                // Bank Pills Row (HDFC, ICICI, SBI)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("HDFC ••4812", "ICICI ••9021", "SBI").forEach { bank ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Slate800 else CanvasWarm)
                                .border(0.5.dp, if (isDark) Slate700 else Slate300, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = bank,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) PureWhite else Slate800
                                )
                            )
                        }
                    }
                }

                // Detected Transactions Text
                Text(
                    text = "✓ 142 Interest credits & dividends detected",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ForestGreenPrimary
                    )
                )
            }
        }

        // 4. Ingestion Card 2: AIS and TIS Sync (Figma 4:1007)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Slate850 else PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(if (isDark) Slate700 else Slate300)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row: AIS Shield Icon + Title + Status Pill
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Slate800 else SageMintPill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "AIS",
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Annual Information Statement",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) PureWhite else Slate900
                                )
                            )
                            Text(
                                text = "Direct sync with Income Tax e-Filing 2.0",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = if (isDark) Slate400 else Slate600
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SageMintPill)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "SYNCED",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        )
                    }
                }

                // Breakdown 3-column metric row: Salaries, Securities, High-value cash
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Salaries", style = TextStyle(fontSize = 11.sp, color = if (isDark) Slate400 else Slate600))
                        Spacer(Modifier.height(2.dp))
                        Text("₹18.5L", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) PureWhite else Slate900))
                    }
                    Column {
                        Text("Securities", style = TextStyle(fontSize = 11.sp, color = if (isDark) Slate400 else Slate600))
                        Spacer(Modifier.height(2.dp))
                        Text("₹4.2L", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) PureWhite else Slate900))
                    }
                    Column {
                        Text("High-value cash", style = TextStyle(fontSize = 11.sp, color = if (isDark) Slate400 else Slate600))
                        Spacer(Modifier.height(2.dp))
                        Text("₹0", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) PureWhite else Slate900))
                    }
                }

                // Matched 26AS Pill Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SageMintPill,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "100% matched with 26AS tax credits",
                            style = TextStyle(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreenPrimary
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 5. Reassurance & Primary CTA Button (Figma 4:1022 & 4:1023)
        Text(
            text = "Data stays on-device. Zero credential caching.",
            style = TextStyle(
                fontSize = 11.5.sp,
                color = if (isDark) Slate500 else Slate500,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        ZeroTaxPrimaryButton(
            text = "Generate Unified Pre-Filing Dossier",
            trailingIcon = {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
            },
            onClick = { onProceed?.invoke() },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )

        Spacer(Modifier.height(20.dp))
    }
}
