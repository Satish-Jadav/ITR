package `in`.zerotax.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.ZeroTaxBrandLogo
import `in`.zerotax.app.ui.components.ZeroTaxPrimaryButton
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.IngestionStreamItem
import `in`.zerotax.core.model.IngestionStreamStatus

/**
 * Animated Multi-Stream Ingestion Radar displaying parallel automated data streams
 * fetching taxpayer records directly into device volatile memory.
 * Optimized for full responsiveness across all Android screen sizes and densities.
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

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Compact Radar Visualization Hub
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(if (isDark) Slate850 else Slate100),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Static concentric circles
                drawCircle(
                    color = EnterpriseTeal500.copy(alpha = 0.2f),
                    radius = 28f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
                drawCircle(
                    color = EnterpriseTeal500.copy(alpha = 0.15f),
                    radius = 42f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // Pulsing dynamic wave
                if (isIngesting) {
                    drawCircle(
                        color = EnterpriseTeal400.copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = center,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }
            }

            // Central Sovereign Core
            ZeroTaxBrandLogo(size = 38.dp)
        }

        // 2. Live Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isIngesting) "Automated Ingestion Radar Active" else "Data Hydration Complete",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) EnterpriseTeal300 else EnterpriseTeal700
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Parallel streams syncing statutory records in volatile memory",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }

        // 3. Streams List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            streams.forEach { stream ->
                StreamStatusCard(stream = stream, isDark = isDark)
            }
        }

        // 4. Action Button
        if (!isIngesting && onProceed != null) {
            ZeroTaxPrimaryButton(
                text = "Continue to Master Dossier (Step 3)",
                trailingIcon = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                },
                onClick = onProceed,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 5. Hardware Vault Bottom Badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isDark) Slate850 else Slate100,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Slate700 else Slate300
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = EnterpriseTeal500,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Encrypted E2EE Cloud Vault • Real-Time Server Synchronization",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StreamStatusCard(
    stream: IngestionStreamItem,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = when (stream.status) {
        IngestionStreamStatus.SUCCESS -> EmeraldSuccess.copy(alpha = 0.5f)
        IngestionStreamStatus.IN_PROGRESS -> EnterpriseTeal500
        IngestionStreamStatus.FAILED -> CrimsonDanger.copy(alpha = 0.5f)
        else -> if (isDark) Slate700 else Slate200
    }

    val cardBg = when (stream.status) {
        IngestionStreamStatus.SUCCESS -> if (isDark) EmeraldSuccessBgDark.copy(alpha = 0.35f) else EmeraldSuccessBg
        IngestionStreamStatus.IN_PROGRESS -> if (isDark) EnterpriseTeal900.copy(alpha = 0.4f) else EnterpriseTeal50
        else -> if (isDark) Slate850 else PureWhite
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when (stream.status) {
                            IngestionStreamStatus.SUCCESS -> EmeraldSuccess.copy(alpha = 0.15f)
                            IngestionStreamStatus.IN_PROGRESS -> EnterpriseTeal500.copy(alpha = 0.15f)
                            IngestionStreamStatus.FAILED -> CrimsonDanger.copy(alpha = 0.15f)
                            else -> if (isDark) Slate800 else Slate100
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (stream.status) {
                    IngestionStreamStatus.SUCCESS -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IngestionStreamStatus.IN_PROGRESS -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = EnterpriseTeal500
                        )
                    }
                    IngestionStreamStatus.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Failed",
                            tint = CrimsonDanger,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = "Pending",
                            tint = Slate500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Stream Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stream.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (stream.badgeText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDark) EnterpriseTeal900 else EnterpriseTeal100,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = stream.badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    color = if (isDark) EnterpriseTeal300 else EnterpriseTeal900
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = if (stream.details.isNotBlank()) stream.details else stream.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (stream.details.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
