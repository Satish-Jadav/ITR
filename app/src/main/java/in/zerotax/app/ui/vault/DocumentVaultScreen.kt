package `in`.zerotax.app.ui.vault

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.EnterpriseTopHeader
import `in`.zerotax.app.ui.security.HardwareVaultStatusCard
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.app.viewmodel.ZeroTaxViewModel

data class FigmaVaultDocument(
    val id: String,
    val title: String,
    val statusBadge: String,
    val badgeToneIsMint: Boolean,
    val sourceSubtitle: String,
    val metricLine: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun DocumentVaultScreen(
    viewModel: ZeroTaxViewModel,
    onBack: () -> Unit = {},
    showTopBar: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            uris.forEach { uri ->
                val fileName = uri.lastPathSegment ?: "Tax_Document.pdf"
                viewModel.ingestDocumentFromUri(
                    context = context,
                    uri = uri,
                    fileName = fileName,
                    onSuccess = { msg ->
                        notificationMessage = msg
                        isProcessing = false
                    },
                    onError = { err ->
                        notificationMessage = "Error: $err"
                        isProcessing = false
                    }
                )
            }
        }
    }

    val cards = remember(uiState) {
        listOf(
            FigmaVaultDocument(
                id = "form16",
                title = "Form 16 (Part A & B)",
                statusBadge = "Parsed",
                badgeToneIsMint = true,
                sourceSubtitle = if (uiState.employerName.isNotBlank()) "Employer: ${uiState.employerName}" else "Employer: Infosys Ltd",
                metricLine = if (uiState.grossSalaryRupees > 0L) "₹${uiState.grossSalaryRupees / 100000}L Gross • Verified" else "₹18.5L Gross • Verified",
                icon = Icons.Default.Description
            ),
            FigmaVaultDocument(
                id = "ais",
                title = "AIS Dossier 2026-27",
                statusBadge = "Verified",
                badgeToneIsMint = true,
                sourceSubtitle = "Income Tax Dept",
                metricLine = "48 Transactions • 100% Match",
                icon = Icons.Default.Security
            ),
            FigmaVaultDocument(
                id = "capital_gains",
                title = "Capital Gains P&L",
                statusBadge = "Verified",
                badgeToneIsMint = true,
                sourceSubtitle = "Zerodha Broking",
                metricLine = "Net Gain: ₹1,42,000",
                icon = Icons.AutoMirrored.Filled.TrendingUp
            ),
            FigmaVaultDocument(
                id = "home_loan",
                title = "Home Loan Certificate",
                statusBadge = "Interest",
                badgeToneIsMint = false,
                sourceSubtitle = "HDFC Ltd",
                metricLine = "Interest: ₹1.85L (Sec 24b)",
                icon = Icons.Default.Home
            )
        )
    }

    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) cards
        else cards.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.sourceSubtitle.contains(searchQuery, ignoreCase = true) ||
            it.metricLine.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Slate950 else CanvasWarm)
    ) {
        if (showTopBar) {
            EnterpriseTopHeader(
                pan = uiState.pan,
                assessmentYear = uiState.assessmentYear,
                userProfile = null,
                onSignOut = { viewModel.signOut() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // 1. HardwareVaultStatusCard (Exact Figma component 4:341)
            HardwareVaultStatusCard()

            Spacer(Modifier.height(14.dp))

            // 2. Document Search & Filter row (Figma 4:1157)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Slate850 else PureWhite)
                        .border(1.dp, if (isDark) Slate700 else Slate300, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isDark) Slate400 else Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search forms, tax slips, capital gains...",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = if (isDark) Slate500 else Slate400
                                )
                            )
                        }
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            color = if (isDark) PureWhite else Slate900
                        ),
                        modifier = Modifier.fillMaxWidth().padding(start = 26.dp)
                    )
                }

                // Filter icon button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Slate850 else PureWhite)
                        .border(1.dp, if (isDark) Slate700 else Slate300, RoundedCornerShape(12.dp))
                        .clickable { /* filter dialog or toggle */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = if (isDark) PureWhite else Slate800,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3. Document 2x2 Grid (Figma 4:1166)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredCards) { doc ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Slate850 else PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(if (isDark) Slate700 else Slate300)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Row: Icon badge + Status pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Slate800 else SageMintPill),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = doc.icon,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (doc.badgeToneIsMint) SageMintPill else AmberPillBg)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = doc.statusBadge,
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (doc.badgeToneIsMint) ForestGreenPrimary else Color(0xFFB46E13)
                                        )
                                    )
                                }
                            }

                            // Middle: Document Title
                            Column {
                                Text(
                                    text = doc.title,
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) PureWhite else Slate900,
                                        lineHeight = 17.sp
                                    ),
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = doc.sourceSubtitle,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = if (isDark) Slate400 else Slate600
                                    ),
                                    maxLines = 1
                                )
                            }

                            // Bottom: Metric Line
                            Text(
                                text = doc.metricLine,
                                style = TextStyle(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) SageMintLight else ForestGreenPrimary
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                // 4. Secure Upload Banner (Figma 4:1203)
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    val strokeColor = if (isDark) Slate700 else Slate300
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(86.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Slate850 else PureWhite)
                            .drawBehind {
                                drawRoundRect(
                                    color = strokeColor,
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                    ),
                                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                                )
                            }
                            .clickable {
                                documentPickerLauncher.launch(
                                    arrayOf("application/pdf", "text/csv", "application/json")
                                )
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Slate800 else SageMintPill),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Upload",
                                    tint = ForestGreenPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Upload Secure Tax Documents",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) PureWhite else Slate900
                                    )
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "PDF, scanned image, or password-protected statement",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = if (isDark) Slate400 else Slate600
                                    )
                                )
                            }
                        }
                    }
                }

                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}
