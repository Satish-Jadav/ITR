package `in`.zerotax.app.ui.corporate

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.zerotax.app.ui.components.*
import `in`.zerotax.app.ui.theme.*
import `in`.zerotax.core.model.*
import `in`.zerotax.core.taxengine.corporate.CorporateAndTrustTaxEngine

enum class CorporateSuiteTab(val title: String, val subtitle: String) {
    ITR_5_FIRM("ITR-5: Firm & LLP", "Sec 40(b) & 115JC AMT"),
    ITR_6_CORPORATE("ITR-6: Company", "Sec 115BAA & 115JB MAT"),
    ITR_7_TRUST("ITR-7: Trust & NGO", "Sec 11, 12, 13 & 115BBI")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CorporateAndTrustSuiteScreen(
    pan: String = "",
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ZeroTaxTopAppBar(
                title = "Corporate Suite",
                subtitle = "ITR-5, ITR-6 & ITR-7 Statutory Suite",
                pan = pan,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        CorporateAndTrustSuiteContent(
            pan = pan,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun CorporateAndTrustSuiteContent(
    pan: String = "",
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(CorporateSuiteTab.ITR_5_FIRM) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EnterpriseTeal600
        ) {
            CorporateSuiteTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = tab.title,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (selectedTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tab.subtitle,
                                fontSize = 10.sp,
                                color = if (selectedTab == tab) EnterpriseTeal600 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "CorporateTabTransition"
        ) { tab ->
            when (tab) {
                CorporateSuiteTab.ITR_5_FIRM -> Itr5FirmCalculatorView(pan = pan)
                CorporateSuiteTab.ITR_6_CORPORATE -> Itr6CorporateCalculatorView(pan = pan)
                CorporateSuiteTab.ITR_7_TRUST -> Itr7TrustCalculatorView(pan = pan)
            }
        }
    }
}

// ============================================================================
// TAB 1: ITR-5 FIRM & LLP CALCULATOR VIEW
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Itr5FirmCalculatorView(pan: String) {
    val isDark = isSystemInDarkTheme()
    var isLlp by remember { mutableStateOf(true) }
    var bookProfitText by remember { mutableStateOf("") }
    var capitalText by remember { mutableStateOf("") }
    var interestRateText by remember { mutableStateOf("12.0") }
    var interestClaimedText by remember { mutableStateOf("") }
    var remunerationClaimedText by remember { mutableStateOf("") }
    var deductionsText by remember { mutableStateOf("0") }

    val bookProfit = bookProfitText.toLongOrNull() ?: 0L
    val capital = capitalText.toLongOrNull() ?: 0L
    val interestRate = interestRateText.toDoubleOrNull() ?: 12.0
    val interestClaimed = interestClaimedText.toLongOrNull() ?: 0L
    val remunerationClaimed = remunerationClaimedText.toLongOrNull() ?: 0L
    val deductions = deductionsText.toLongOrNull() ?: 0L

    val input = remember(isLlp, bookProfit, capital, interestRate, interestClaimed, remunerationClaimed, deductions) {
        Itr5FirmInput(
            firmPan = pan,
            entityType = if (isLlp) CorporateEntityType.LIMITED_LIABILITY_PARTNERSHIP else CorporateEntityType.PARTNERSHIP_FIRM,
            bookProfitBeforePartnerPayments = bookProfit,
            partnerCapitalTotalRupees = capital,
            partnerInterestRatePercent = interestRate,
            partnerInterestOnCapitalClaimed = interestClaimed,
            partnerRemunerationClaimed = remunerationClaimed,
            isWorkingPartnerAuthorizedByDeed = true,
            deductionsChapterVIAExcept10AA = deductions
        )
    }

    val result = remember(input) {
        CorporateAndTrustTaxEngine.computeItr5FirmTax(input)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Entity Classification Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CorporateFare,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "ITR-5 Entity Classification",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        StatutoryBadge(
                            text = if (isLlp) "LLP (Sec 44AD)" else "Partnership",
                            tone = if (isLlp) BadgeTone.WARNING else BadgeTone.STATUTORY
                        )
                    }
                    Text(
                        text = if (isLlp)
                            "Section 44AD(6) strictly prohibits LLPs from presumptive taxation. Mandated to file ITR-5 with Section 40(b) caps."
                        else
                            "Partnership firms with partner remuneration/interest or audit mandate file ITR-5 with Section 40(b) ceilings.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isLlp,
                            onClick = { isLlp = true },
                            label = { Text("Limited Liability Partnership (LLP)") }
                        )
                        FilterChip(
                            selected = !isLlp,
                            onClick = { isLlp = false },
                            label = { Text("Partnership Firm") }
                        )
                    }
                }
            }
        }

        // Dedicated Financial Input Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EnterpriseTeal600.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = EnterpriseTeal600,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Financial Books & Section 40(b) Inputs",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Enter partnership book profits, partner capital and remuneration",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

                    OutlinedTextField(
                        value = bookProfitText,
                        onValueChange = { bookProfitText = it },
                        label = { Text("Book Profit Before Partner Payments") },
                        placeholder = { Text("e.g. 15,00,000") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = capitalText,
                        onValueChange = { capitalText = it },
                        label = { Text("Partner Total Capital") },
                        placeholder = { Text("e.g. 50,00,000") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = { interestRateText = it },
                        label = { Text("Interest Rate % p.a. (Sec 40(b) max 12%)") },
                        placeholder = { Text("12.0") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = interestClaimedText,
                        onValueChange = { interestClaimedText = it },
                        label = { Text("Partner Interest Claimed") },
                        placeholder = { Text("e.g. 6,00,000") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = remunerationClaimedText,
                        onValueChange = { remunerationClaimedText = it },
                        label = { Text("Partner Remuneration Claimed") },
                        placeholder = { Text("e.g. 9,00,000") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = deductionsText,
                        onValueChange = { deductionsText = it },
                        label = { Text("Chapter VI-A Deductions (Except 10AA)") },
                        placeholder = { Text("0") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Live Statutory Result Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(EnterpriseTeal600.copy(alpha = 0.4f))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ITR-5 Statutory Assessment",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        StatutoryBadge(
                            text = if (result.isAmtPayable) "AMT PAYABLE" else "NORMAL TAX",
                            tone = if (result.isAmtPayable) BadgeTone.WARNING else BadgeTone.SUCCESS
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Admissible Partner Interest u/s 40(b)(iv):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${Rupee.formatInr(result.admissibleInterestSection40b)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    if (result.interestDisallowanceSection40b > 0L) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Interest Disallowance (> 12% p.a.):", style = MaterialTheme.typography.bodyMedium.copy(color = CrimsonDanger))
                            Text("+₹${Rupee.formatInr(result.interestDisallowanceSection40b)}", fontWeight = FontWeight.Bold, color = CrimsonDanger)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Max Allowed Remuneration u/s 40(b)(v):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${Rupee.formatInr(result.maxPermissibleRemuneration40b)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    if (result.remunerationDisallowance40b > 0L) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Excess Remuneration Disallowed:", style = MaterialTheme.typography.bodyMedium.copy(color = CrimsonDanger))
                            Text("+₹${Rupee.formatInr(result.remunerationDisallowance40b)}", fontWeight = FontWeight.Bold, color = CrimsonDanger)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Taxable Business Profit:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${Rupee.formatInr(result.taxableBusinessProfit)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Normal Tax @ 30% + Cess (4%):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${Rupee.formatInr(result.totalNormalTaxPayableRupees)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Alternate Minimum Tax (115JC @ 18.5%):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${Rupee.formatInr(result.alternateMinimumTax115JC)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Final Firm/LLP Tax Liability:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "₹${Rupee.formatInr(result.finalTaxPayableRupees)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EnterpriseTeal600)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Statutory Compliance Findings", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    result.statutoryNotes.forEach { note ->
                        Text("• $note", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(88.dp))
        }
    }
}

// ============================================================================
// TAB 2: ITR-6 CORPORATE COMPANY CALCULATOR VIEW
// ============================================================================
@Composable
fun Itr6CorporateCalculatorView(pan: String) {
    var has115Baa by remember { mutableStateOf(true) }
    var isMfg115Bab by remember { mutableStateOf(false) }
    var turnoverOver400Cr by remember { mutableStateOf(false) }
    var taxableIncomeText by remember { mutableStateOf("") }
    var matProfitsText by remember { mutableStateOf("") }
    var broughtForwardMatText by remember { mutableStateOf("") }

    val taxableIncome = taxableIncomeText.toLongOrNull() ?: 0L
    val matProfits = matProfitsText.toLongOrNull() ?: 0L
    val bfMat = broughtForwardMatText.toLongOrNull() ?: 0L

    val input = remember(has115Baa, isMfg115Bab, turnoverOver400Cr, taxableIncome, matProfits, bfMat) {
        Itr6CorporateInput(
            companyPan = pan,
            companyName = pan.takeIf { it.isNotBlank() } ?: "Corporate Assessee",
            turnoverInPriorYearExceeding400Cr = turnoverOver400Cr,
            isNewManufacturingCompany115BAB = isMfg115Bab,
            hasOptedForSection115BAA = has115Baa,
            taxableIncomeNormalRupees = taxableIncome,
            bookProfitsMatSection115JB = matProfits,
            broughtForwardMatCredit115JAA = bfMat
        )
    }

    val result = remember(input) {
        CorporateAndTrustTaxEngine.computeItr6CorporateTax(input)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Regime Selector Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Domain,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "ITR-6 Corporate Tax Regime Selector",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    Text(
                        text = "Domestic companies can elect Section 115BAA (22% base + 10% surcharge = 25.168% flat) to be permanently exempt from Section 115JB MAT.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = has115Baa,
                            onClick = {
                                has115Baa = true
                                isMfg115Bab = false
                            },
                            label = { Text("Sec 115BAA (22% MAT-Free)") }
                        )
                        FilterChip(
                            selected = isMfg115Bab,
                            onClick = {
                                has115Baa = false
                                isMfg115Bab = true
                            },
                            label = { Text("Sec 115BAB (15% Mfg)") }
                        )
                    }
                    FilterChip(
                        selected = !has115Baa && !isMfg115Bab,
                        onClick = {
                            has115Baa = false
                            isMfg115Bab = false
                        },
                        label = { Text("Normal Corporate Tax (25% / 30% with MAT)") }
                    )
                }
            }
        }

        // Dedicated Input Card for Corporate
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndigoStatutory.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = IndigoStatutory,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Corporate Financial Declarations",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Audited taxable income and Section 115JB book profit figures",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

                    OutlinedTextField(
                        value = taxableIncomeText,
                        onValueChange = { taxableIncomeText = it },
                        label = { Text("Normal Taxable Income") },
                        placeholder = { Text("e.g. 50,00,000") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    if (!has115Baa && !isMfg115Bab) {
                        OutlinedTextField(
                            value = matProfitsText,
                            onValueChange = { matProfitsText = it },
                            label = { Text("Book Profits u/s 115JB") },
                            placeholder = { Text("e.g. 60,00,000") },
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = broughtForwardMatText,
                            onValueChange = { broughtForwardMatText = it },
                            label = { Text("Brought Forward MAT Credit (Sec 115JAA)") },
                            placeholder = { Text("0") },
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Live Statutory Result Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(IndigoStatutory.copy(alpha = 0.4f))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ITR-6 Assessment Summary",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        StatutoryBadge(
                            text = "RATE: ${result.applicableTaxRatePercent.toInt()}%",
                            tone = BadgeTone.STATUTORY
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Normal Corporate Tax (incl Cess):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${Rupee.formatInr(result.totalNormalTaxRupees)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("MAT u/s 115JB (15% + Cess):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (!result.isMatApplicable) "EXEMPT u/s 115JB(5A)" else "₹${Rupee.formatInr(result.matBookProfitsTaxRupees)}",
                            fontWeight = FontWeight.Bold,
                            color = if (!result.isMatApplicable) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (result.matCreditUtilizedCurrentYear > 0L) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("MAT Credit Utilized u/s 115JAA:", style = MaterialTheme.typography.bodyMedium.copy(color = EmeraldSuccess))
                            Text("-₹${Rupee.formatInr(result.matCreditUtilizedCurrentYear)}", fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        }
                    }

                    if (result.matCreditGeneratedCurrentYear > 0L) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("New MAT Credit Created for Future Offset:", style = MaterialTheme.typography.bodyMedium.copy(color = EnterpriseTeal600))
                            Text("+₹${Rupee.formatInr(result.matCreditGeneratedCurrentYear)}", fontWeight = FontWeight.Bold, color = EnterpriseTeal600)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Corporate Tax Payable:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "₹${Rupee.formatInr(result.totalCorporateTaxPayableRupees)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = IndigoStatutory)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Statutory Compliance Findings", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    result.statutoryNotes.forEach { note ->
                        Text("• $note", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(88.dp))
        }
    }
}

// ============================================================================
// TAB 3: ITR-7 TRUST & NGO CALCULATOR VIEW
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Itr7TrustCalculatorView(pan: String) {
    var isPoliticalParty by remember { mutableStateOf(false) }
    var contributionsText by remember { mutableStateOf("") }
    var corpusText by remember { mutableStateOf("") }
    var applicationText by remember { mutableStateOf("") }
    var deemed9AText by remember { mutableStateOf("") }
    var form10Text by remember { mutableStateOf("") }
    var sec13ViolationText by remember { mutableStateOf("") }

    val contributions = contributionsText.toLongOrNull() ?: 0L
    val corpus = corpusText.toLongOrNull() ?: 0L
    val application = applicationText.toLongOrNull() ?: 0L
    val deemed9A = deemed9AText.toLongOrNull() ?: 0L
    val form10 = form10Text.toLongOrNull() ?: 0L
    val sec13Violation = sec13ViolationText.toLongOrNull() ?: 0L

    val input = remember(isPoliticalParty, contributions, corpus, application, deemed9A, form10, sec13Violation) {
        Itr7TrustInput(
            trustPan = pan,
            trustName = pan.takeIf { it.isNotBlank() } ?: "Charitable Trust",
            entityType = if (isPoliticalParty) CorporateEntityType.POLITICAL_PARTY_13A else CorporateEntityType.CHARITABLE_RELIGIOUS_TRUST_12AB,
            totalVoluntaryContributionsRupees = contributions,
            corpusDonationsExemptSec11_1_d = corpus,
            charitableApplicationInIndiaRupees = application,
            deemedApplicationForm9ARupees = deemed9A,
            accumulatedUnderForm10Rupees = form10,
            unapprovedSection13ViolationsRupees = sec13Violation
        )
    }

    val result = remember(input) {
        CorporateAndTrustTaxEngine.computeItr7TrustTax(input)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Framework Selector Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldSuccess.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolunteerActivism,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "ITR-7 Entity Framework",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    Text(
                        text = "Charitable & religious trusts registered u/s 12AB must apply 85% of income to charitable purposes in India. Corpus donations are 100% exempt u/s 11(1)(d).",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isPoliticalParty,
                            onClick = { isPoliticalParty = false },
                            label = { Text("Charitable / Religious Trust (Sec 11/12)") }
                        )
                        FilterChip(
                            selected = isPoliticalParty,
                            onClick = { isPoliticalParty = true },
                            label = { Text("Political Party (Sec 13A)") }
                        )
                    }
                }
            }
        }

        // Dedicated Input Card for Trust & NGO
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 0.5.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldSuccess.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Trust Inflow & Statutory Application",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Enter voluntary donations, corpus receipts and charitable spending",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

                    OutlinedTextField(
                        value = contributionsText,
                        onValueChange = { contributionsText = it },
                        label = { Text("Total Voluntary Contributions") },
                        placeholder = { Text("e.g. 1,00,00,000") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = corpusText,
                        onValueChange = { corpusText = it },
                        label = { Text("Corpus Donations Exempt u/s 11(1)(d)") },
                        placeholder = { Text("e.g. 25,00,000") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = applicationText,
                        onValueChange = { applicationText = it },
                        label = { Text("Charitable Application in India") },
                        placeholder = { Text("e.g. 65,00,000") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = deemed9AText,
                        onValueChange = { deemed9AText = it },
                        label = { Text("Deemed Application (Form 9A)") },
                        placeholder = { Text("0") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = form10Text,
                        onValueChange = { form10Text = it },
                        label = { Text("Accumulated Income (Form 10)") },
                        placeholder = { Text("0") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = sec13ViolationText,
                        onValueChange = { sec13ViolationText = it },
                        label = { Text("Section 13 Violations (MMR 34.944%)") },
                        placeholder = { Text("0") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Live Statutory Result Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess.copy(alpha = 0.4f))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ITR-7 Trust Exemption Status",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        StatutoryBadge(
                            text = if (result.isCompletelyTaxExempt) "100% EXEMPT" else "TAXABLE",
                            tone = if (result.isCompletelyTaxExempt) BadgeTone.SUCCESS else BadgeTone.DANGER
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("15% Statutory Accumulation u/s 11(1)(a):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${Rupee.formatInr(result.statutory15PercentAccumulationRupees)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("85% Mandatory Application Required:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${Rupee.formatInr(result.mandatory85PercentApplicationRequired)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total Effective Application Claimed:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${Rupee.formatInr(result.totalEffectiveApplicationClaimed)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    if (result.applicationShortfallRupees > 0L) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Application Shortfall (Taxable):", style = MaterialTheme.typography.bodyMedium.copy(color = CrimsonDanger))
                            Text("₹${Rupee.formatInr(result.applicationShortfallRupees)}", fontWeight = FontWeight.Bold, color = CrimsonDanger)
                        }
                    }

                    if (result.section13ViolationTaxAtMMR > 0L) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Sec 13 Forfeiture u/s 115BBI (MMR 34.944%):", style = MaterialTheme.typography.bodyMedium.copy(color = CrimsonDanger))
                            Text("₹${Rupee.formatInr(result.section13ViolationTaxAtMMR)}", fontWeight = FontWeight.Bold, color = CrimsonDanger)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Trust Tax Payable:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "₹${Rupee.formatInr(result.totalTaxPayableRupees)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (result.isCompletelyTaxExempt) EmeraldSuccess else CrimsonDanger
                            )
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Statutory Compliance Findings", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    result.statutoryNotes.forEach { note ->
                        Text("• $note", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(88.dp))
        }
    }
}
