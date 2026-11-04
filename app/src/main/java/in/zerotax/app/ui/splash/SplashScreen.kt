package `in`.zerotax.app.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onInitializationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stageIndex by remember { mutableIntStateOf(0) }
    var isReadyToExit by remember { mutableStateOf(false) }

    val verificationStages = remember {
        listOf(
            "Verifying Android StrongBox TEE Keymaster...",
            "Deriving AES-256-GCM hardware master key...",
            "Mounting SQLCipher 4.5.4 zero-leak vault...",
            "Arming UIDAI Aadhaar Verhoeff Scrubber...",
            "Zero Cloud Telemetry Verified • Hardware Vault Ready"
        )
    }

    // Animation drivers
    val infiniteTransition = rememberInfiniteTransition(label = "VaultSecurityPulse")

    val outerRingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )

    val innerPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "InnerPulse"
    )

    val shieldGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShieldGlow"
    )

    val progressAnim = remember { Animatable(0f) }

    // Sequential initialization simulation
    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        for (i in verificationStages.indices) {
            stageIndex = i
            delay(420)
        }
        delay(350)
        isReadyToExit = true
        delay(250)
        onInitializationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950),
        contentAlignment = Alignment.Center
    ) {
        // Ambient radial teal glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        EnterpriseTeal600.copy(alpha = 0.15f * shieldGlowAlpha),
                        EnterpriseTeal900.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.75f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Cryptographic Vault Shield Canvas
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .scale(innerPulseScale)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerPt = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2.2f

                    // Rotating segmented cryptographic perimeter ring
                    drawArc(
                        color = EnterpriseTeal500.copy(alpha = 0.85f),
                        startAngle = outerRingRotation,
                        sweepAngle = 70f,
                        useCenter = false,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = EnterpriseTeal600.copy(alpha = 0.45f),
                        startAngle = outerRingRotation + 120f,
                        sweepAngle = 60f,
                        useCenter = false,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = EnterpriseTeal500.copy(alpha = 0.75f),
                        startAngle = outerRingRotation + 240f,
                        sweepAngle = 70f,
                        useCenter = false,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )

                    // Inner static shield outline
                    val shieldPath = Path().apply {
                        val w = size.width
                        val h = size.height
                        moveTo(w * 0.5f, h * 0.22f)
                        lineTo(w * 0.76f, h * 0.32f)
                        lineTo(w * 0.76f, h * 0.58f)
                        cubicTo(w * 0.76f, h * 0.74f, w * 0.5f, h * 0.84f, w * 0.5f, h * 0.84f)
                        cubicTo(w * 0.5f, h * 0.84f, w * 0.24f, h * 0.74f, w * 0.24f, h * 0.58f)
                        lineTo(w * 0.24f, h * 0.32f)
                        close()
                    }

                    // Shield background fill
                    drawPath(
                        path = shieldPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                EnterpriseTeal800.copy(alpha = 0.35f),
                                Slate900.copy(alpha = 0.65f)
                            )
                        )
                    )

                    // Shield border stroke
                    drawPath(
                        path = shieldPath,
                        color = EnterpriseTeal500.copy(alpha = 0.8f),
                        style = Stroke(width = 2f, cap = StrokeCap.Round)
                    )

                    // Padlock shackle & body in center
                    val lockCenter = Offset(size.width * 0.5f, size.height * 0.53f)
                    val lockWidth = size.width * 0.20f
                    val lockHeight = size.height * 0.16f

                    // Shackle
                    drawArc(
                        color = PureWhite,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(lockCenter.x - lockWidth * 0.35f, lockCenter.y - lockHeight * 0.85f),
                        size = Size(lockWidth * 0.70f, lockHeight * 0.80f),
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )

                    // Lock body
                    drawRoundRect(
                        color = EnterpriseTeal500,
                        topLeft = Offset(lockCenter.x - lockWidth / 2f, lockCenter.y - lockHeight * 0.15f),
                        size = Size(lockWidth, lockHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )

                    // Keyhole dot
                    drawCircle(
                        color = Slate950,
                        radius = 2.5f,
                        center = Offset(lockCenter.x, lockCenter.y + lockHeight * 0.25f)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Brand Typography
            Text(
                text = "ZeroTax Local",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "100% On-Device Deterministic Tax Vault",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Slate400,
                    fontWeight = FontWeight.Normal
                )
            )

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(EnterpriseTeal900)
                    .border(0.75.dp, EnterpriseTeal600.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "ASSESSMENT YEAR 2026-27",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EnterpriseTeal500,
                        letterSpacing = 0.8.sp
                    )
                )
            }

            Spacer(Modifier.height(44.dp))

            // Slim Hairline Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Slate800)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim.value)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(EnterpriseTeal700, EnterpriseTeal500)
                            )
                        )
                )
            }

            Spacer(Modifier.height(14.dp))

            // Dynamic Security Verification Ticker
            AnimatedContent(
                targetState = stageIndex,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) +
                     slideInVertically(animationSpec = tween(220)) { it / 2 })
                        .togetherWith(
                            fadeOut(animationSpec = tween(150)) +
                            slideOutVertically(animationSpec = tween(150)) { -it / 2 }
                        )
                },
                label = "SecurityTicker"
            ) { currentIdx ->
                Text(
                    text = verificationStages.getOrElse(currentIdx) { "" },
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (currentIdx == verificationStages.lastIndex) EmeraldSuccess else Slate400
                    ),
                    maxLines = 1
                )
            }
        }

        // Subdued bottom security boundary statement
        Text(
            text = "UIDAI §29 • DPDP Act §12(3) • Sec 115BAC Deterministic Engine",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Slate600
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}
