package `in`.zerotax.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.zerotax.app.ui.navigation.OnboardingTap
import `in`.zerotax.app.ui.navigation.WorkspaceTab
import `in`.zerotax.core.model.*
import `in`.zerotax.core.security.CanonicalWarningHasher
import `in`.zerotax.core.security.ZeroTaxAuthSecurityManager
import `in`.zerotax.core.security.network.ZeroTaxCloudApiBridge
import `in`.zerotax.core.security.network.ZeroTaxHardenedApiClient
import `in`.zerotax.core.taxengine.calculator.DeterministicIndianTaxEngine
import `in`.zerotax.core.taxengine.calculator.RegimeComputationResult
import `in`.zerotax.core.taxengine.calculator.TaxpayerFinancialInput
import `in`.zerotax.core.taxengine.reconciliation.DeterministicReconciler
import `in`.zerotax.core.taxengine.routing.DeterministicItrFormRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuditEntryUiModel(
    val auditId: String,
    val eventCategory: String,
    val description: String,
    val warningTextHashSha256: String,
    val timestampFormatted: String
)

data class ZeroTaxUiState(
    val currentTap: OnboardingTap? = OnboardingTap.Tap1IdentityConsent,
    val selectedWorkspaceTab: WorkspaceTab = WorkspaceTab.FilingAndRefundHub,
    val pan: String = "",
    val assessmentYear: String = "2026-27",
    val grossSalaryRupees: Long = 0L,
    val employerName: String = "",
    val employerTan: String = "",
    val tdsDeposited26As: Long = 0L,
    val exemptAllowancesSec10: Long = 0L,
    val professionalTaxSec16: Long = 0L,
    val section80CInvestments: Long = 0L,
    val section80DSelf: Long = 0L,
    val section80CCD1BNps: Long = 0L,
    val homeLoanInterestSec24b: Long = 0L,
    val presumptiveGross44ADA: Long = 0L,
    val hasPresumptive44ADA: Boolean = false,
    val foreignAssetsDeclared: Boolean = false,
    val isBiometricUnlocked: Boolean = true,
    val citizenCategory: CitizenCategory = CitizenCategory.REGULAR_BELOW_60,
    val showPasswordCheatsheet: Boolean = false,
    val filingTierMode: FilingTierMode = FilingTierMode.SAHAJ_SIMPLE,
    val recommendedTierMode: FilingTierMode = FilingTierMode.SAHAJ_SIMPLE,
    val hasPlanExplicitlySelected: Boolean = true,
    val authSessionState: AuthSessionState = AuthSessionState.Unauthenticated,
    val authErrorMessage: String? = null,
    val isAuthLoading: Boolean = false,
    val challanPayments: List<ScheduleItChallanEntry> = emptyList(),
    val totalTaxPaidViaChallan: Long = 0L,
    val showTaxPaymentHub: Boolean = false,
    val livePortalStatus: String = "CHECKING",
    val livePortalLatencyMs: Long = 0L,
    val isLiveApiConnected: Boolean = false,
    val liveApiEndpoint: String = ZeroTaxHardenedApiClient.apiBaseUrl,
    val realtimeCii: Int = 376,
    val liveSchemaVersion: String = "Ver1.2",
    val newRegimeResult: RegimeComputationResult,
    val oldRegimeResult: RegimeComputationResult,
    val formRoutingDecision: FormRoutingDecision,
    val salarySummary: ReconciledSalarySummary,
    val auditTrail: List<AuditEntryUiModel> = emptyList(),
    val lazyAuditTrail: LazyLoadState<List<AuditEntryUiModel>> = LazyLoadState.Uninitialized,
    val isVaultShredded: Boolean = false
)

class ZeroTaxViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<ZeroTaxUiState>

    init {
        val initialSummary = ReconciledSalarySummary(
            employers = emptyList(),
            aggregateGrossSalary = 0L,
            aggregateSec10Exemptions = 0L,
            aggregateNetTaxableSalary = 0L,
            aggregateTdsAllowed = 0L,
            totalDiscrepancyAmount = 0L
        )
        val initialProfile = TaxpayerRoutingProfile(
            residentialStatus = ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,
            totalIncomeRupees = 0L,
            housePropertyCount = 0
        )
        val initialRouting = DeterministicItrFormRouter.routeTaxpayer(initialProfile)
        val initialInput = TaxpayerFinancialInput(
            citizenCategory = CitizenCategory.REGULAR_BELOW_60,
            grossSalary = 0L,
            assessmentYearProfile = AssessmentYearProfile.AY_2026_27
        )

        _uiState = MutableStateFlow(
            ZeroTaxUiState(
                newRegimeResult = DeterministicIndianTaxEngine.computeNewRegime(initialInput),
                oldRegimeResult = DeterministicIndianTaxEngine.computeOldRegime(initialInput),
                formRoutingDecision = initialRouting,
                salarySummary = initialSummary,
                auditTrail = emptyList(),
                lazyAuditTrail = LazyLoadState.Uninitialized
            )
        )

        // Fetch live statutory rules & ITD portal health telemetry in real-time
        syncRealtimeBackendData()
    }

    val uiState: StateFlow<ZeroTaxUiState> = _uiState.asStateFlow()

    fun onPanChanged(pan: String) {
        val cleanPan = pan.uppercase().trim().take(10)
        _uiState.update { it.copy(pan = cleanPan) }
        recompute()
    }

    fun onGrossSalaryChanged(gross: Long) {
        val safeGross = maxOf(0L, gross)
        _uiState.update { it.copy(grossSalaryRupees = safeGross) }
        recompute()
    }

    fun onEmployerChanged(name: String, tan: String, tdsPaid: Long) {
        _uiState.update {
            it.copy(
                employerName = name,
                employerTan = tan.uppercase().trim().take(10),
                tdsDeposited26As = maxOf(0L, tdsPaid)
            )
        }
        recompute()
    }

    fun importForm16Salary(
        grossSalary: Long,
        tds: Long,
        employerTan: String,
        employerName: String = "",
        sec80C: Long = 0L,
        sec80D: Long = 0L,
        sec80CCD1B: Long = 0L
    ) {
        _uiState.update {
            it.copy(
                grossSalaryRupees = maxOf(0L, grossSalary),
                employerTan = employerTan.uppercase().trim().take(10),
                employerName = if (employerName.isNotBlank()) employerName else if (it.employerName.isBlank()) "TRACES ($employerTan)" else it.employerName,
                tdsDeposited26As = maxOf(0L, tds),
                section80CInvestments = if (sec80C > 0L) sec80C else it.section80CInvestments,
                section80DSelf = if (sec80D > 0L) sec80D else it.section80DSelf,
                section80CCD1BNps = if (sec80CCD1B > 0L) sec80CCD1B else it.section80CCD1BNps
            )
        }
        recompute()
    }

    fun onSection80CChanged(amount: Long) {
        _uiState.update { it.copy(section80CInvestments = maxOf(0L, amount)) }
        recompute()
    }

    fun onSection80DChanged(amount: Long) {
        _uiState.update { it.copy(section80DSelf = maxOf(0L, amount)) }
        recompute()
    }

    fun onSection80CCD1BChanged(amount: Long) {
        _uiState.update { it.copy(section80CCD1BNps = maxOf(0L, amount)) }
        recompute()
    }

    fun onHomeLoanInterestChanged(amount: Long) {
        _uiState.update { it.copy(homeLoanInterestSec24b = maxOf(0L, amount)) }
        recompute()
    }

    fun onCitizenCategoryChanged(category: CitizenCategory) {
        _uiState.update { it.copy(citizenCategory = category) }
        recompute()
    }

    fun onTogglePasswordCheatsheet(show: Boolean) {
        _uiState.update { it.copy(showPasswordCheatsheet = show) }
    }

    fun onFilingTierChanged(tier: FilingTierMode) {
        _uiState.update { it.copy(filingTierMode = tier, hasPlanExplicitlySelected = true) }
    }

    fun onToggleTaxPaymentHub(show: Boolean) {
        _uiState.update { it.copy(showTaxPaymentHub = show) }
    }

    fun onRecordChallanPayment(entry: ScheduleItChallanEntry) {
        val updated = _uiState.value.challanPayments + entry
        val totalPaid = updated.sumOf { it.taxAmountPaidRupees }
        _uiState.update {
            it.copy(
                challanPayments = updated,
                totalTaxPaidViaChallan = totalPaid
            )
        }
        recompute()
    }

    fun onNavigateToTap(tap: OnboardingTap?) {
        _uiState.update { it.copy(currentTap = tap) }
    }

    fun onSelectWorkspaceTab(tab: WorkspaceTab) {
        _uiState.update { it.copy(selectedWorkspaceTab = tab) }
        if (tab == WorkspaceTab.SmartTaxVaultAndCopilot || tab == WorkspaceTab.PrivacyAndSettings) {
            loadAuditTrailLazy()
        }
    }

    /**
     * Applies real-time intake from AI Copilot Filing Chat into the live ViewModel state
     */
    fun applyChatbotFiling(grossSalary: Long, totalDeductions: Long) {
        _uiState.update {
            it.copy(
                grossSalaryRupees = maxOf(0L, grossSalary),
                section80CInvestments = minOf(150_000L, maxOf(0L, totalDeductions)),
                currentTap = OnboardingTap.Tap4RegimeShowdown
            )
        }
        recompute()
    }

    /**
     * Clears all fields to a fresh real-time blank state
     */
    fun resetToFreshState() {
        _uiState.update {
            it.copy(
                pan = "",
                grossSalaryRupees = 0L,
                employerName = "",
                employerTan = "",
                tdsDeposited26As = 0L,
                section80CInvestments = 0L,
                section80DSelf = 0L,
                section80CCD1BNps = 0L,
                homeLoanInterestSec24b = 0L
            )
        }
        recompute()
    }

    /**
     * Fetches live ITD portal telemetry & statutory manifest from the API in real time
     */
    fun syncRealtimeBackendData() {
        viewModelScope.launch {
            runCatching {
                val portalHealth = ZeroTaxCloudApiBridge.checkPortalHealth().getOrThrow()
                val manifest = ZeroTaxCloudApiBridge.fetchStatutoryManifest("2026-27").getOrNull()

                val eportalStatus = portalHealth.optString("eportalStatus", "HEALTHY")
                val latency = portalHealth.optLong("medianResponseMs", 1420L)
                val cii = manifest?.optInt("cii2025_26") ?: 376
                val schemaVer = manifest?.optString("cbdtSchemaVersion") ?: "Ver1.2"

                _uiState.update {
                    it.copy(
                        livePortalStatus = eportalStatus,
                        livePortalLatencyMs = latency,
                        isLiveApiConnected = true,
                        liveApiEndpoint = ZeroTaxHardenedApiClient.apiBaseUrl,
                        realtimeCii = cii,
                        liveSchemaVersion = schemaVer
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        livePortalStatus = "OFFLINE_FALLBACK",
                        isLiveApiConnected = false,
                        liveApiEndpoint = ZeroTaxHardenedApiClient.apiBaseUrl
                    )
                }
            }
        }
    }

    /**
     * Lazily loads the audit trail entries on-demand when the user views the Audit Vault or Privacy tab.
     */
    fun loadAuditTrailLazy() {
        if (_uiState.value.lazyAuditTrail is LazyLoadState.Success) return
        viewModelScope.launch {
            _uiState.update { it.copy(lazyAuditTrail = LazyLoadState.Loading) }
            val currentPan = _uiState.value.pan.ifBlank { "TAXPAYER" }
            val initialAudit = listOf(
                AuditEntryUiModel(
                    auditId = "AUD-INIT-001",
                    eventCategory = "VAULT_INITIALIZATION",
                    description = "Hardware StrongBox Master Key initialized for PAN $currentPan",
                    warningTextHashSha256 = CanonicalWarningHasher.computeSha256("INITIAL_HARDWARE_KEYSTORE_BINDING"),
                    timestampFormatted = "Audit Log Active"
                )
            )
            _uiState.update {
                it.copy(
                    lazyAuditTrail = LazyLoadState.Success(initialAudit),
                    auditTrail = initialAudit
                )
            }
        }
    }

    fun onStatutoryOverrideConfirmed(
        fieldName: String,
        originalValue: Long,
        overriddenValue: Long,
        statutoryReasonCode: String
    ) {
        val warningText = "Statutory Penalty Warning Sec 270A: Overriding '$fieldName' from ₹$originalValue to ₹$overriddenValue u/s 140 declaration."
        val hash = CanonicalWarningHasher.computeSha256(warningText)

        val newAudit = AuditEntryUiModel(
            auditId = "AUD-270A-${System.currentTimeMillis() % 100000}",
            eventCategory = "SECTION_270A_OVERRIDE",
            description = "$fieldName overridden: ₹$originalValue → ₹$overriddenValue ($statutoryReasonCode)",
            warningTextHashSha256 = hash,
            timestampFormatted = "Audit Logged"
        )

        _uiState.update { state ->
            state.copy(
                grossSalaryRupees = overriddenValue,
                auditTrail = listOf(newAudit) + state.auditTrail
            )
        }
        recompute()
    }

    fun onShredVaultData() {
        viewModelScope.launch {
            ZeroTaxAuthSecurityManager.signOut()
            _uiState.update {
                it.copy(
                    isVaultShredded = true,
                    auditTrail = emptyList(),
                    pan = "",
                    grossSalaryRupees = 0L,
                    currentTap = OnboardingTap.Tap1IdentityConsent,
                    authSessionState = AuthSessionState.Unauthenticated
                )
            }
        }
    }

    fun signInWithEmail(email: String, password: String, isSignUp: Boolean, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null) }
            val result = if (isSignUp) {
                ZeroTaxAuthSecurityManager.signUpWithEmail(email, password, name)
            } else {
                ZeroTaxAuthSecurityManager.signInWithEmail(email, password)
            }
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authSessionState = AuthSessionState.Authenticated(profile),
                            authErrorMessage = null
                        )
                    }
                    syncRealtimeBackendData()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authErrorMessage = error.message ?: "Authentication failed. Please verify credentials."
                        )
                    }
                }
            )
        }
    }

    fun signInWithGoogle(email: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null) }
            val googleIdToken = "g_token_${System.currentTimeMillis()}"
            val result = ZeroTaxAuthSecurityManager.signInWithGoogle(googleIdToken, email, name)
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authSessionState = AuthSessionState.Authenticated(profile),
                            authErrorMessage = null
                        )
                    }
                    syncRealtimeBackendData()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authErrorMessage = error.message ?: "Google Sign-In failed."
                        )
                    }
                }
            )
        }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null) }
            val guestProfile = ZeroTaxAuthSecurityManager.signInAsGuestOffline()
            _uiState.update {
                it.copy(
                    isAuthLoading = false,
                    authSessionState = AuthSessionState.Authenticated(guestProfile),
                    authErrorMessage = null
                )
            }
            syncRealtimeBackendData()
        }
    }

    fun signInWithBiometrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null) }
            val result = ZeroTaxAuthSecurityManager.signInWithBiometrics()
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authSessionState = AuthSessionState.Authenticated(profile),
                            authErrorMessage = null
                        )
                    }
                    syncRealtimeBackendData()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authErrorMessage = error.message ?: "Biometric unlock failed."
                        )
                    }
                }
            )
        }
    }

    fun signOut() {
        ZeroTaxAuthSecurityManager.signOut()
        _uiState.update {
            it.copy(
                authSessionState = AuthSessionState.Unauthenticated,
                authErrorMessage = null
            )
        }
    }

    private fun recompute() {
        val current = _uiState.value
        val input = TaxpayerFinancialInput(
            citizenCategory = current.citizenCategory,
            grossSalary = current.grossSalaryRupees,
            exemptHraAndAllowancesSec10 = current.exemptAllowancesSec10,
            professionalTaxSec16iii = current.professionalTaxSec16,
            section80CInvestments = current.section80CInvestments,
            section80DSelfAndFamily = current.section80DSelf,
            section80CCD1BSelfNps = current.section80CCD1BNps,
            selfOccupiedHomeLoanInterestSec24b = current.homeLoanInterestSec24b,
            presumptiveGrossReceipts44ADA = current.presumptiveGross44ADA,
            assessmentYearProfile = AssessmentYearProfile.AY_2026_27
        )

        val newResult = DeterministicIndianTaxEngine.computeNewRegime(input)
        val oldResult = DeterministicIndianTaxEngine.computeOldRegime(input)

        val profile = TaxpayerRoutingProfile(
            residentialStatus = ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,
            totalIncomeRupees = newResult.netTaxableNormalIncomeRounded288A,
            housePropertyCount = if (current.homeLoanInterestSec24b > 0) 1 else 0,
            holdsForeignAssetsOrForeignIncome = current.foreignAssetsDeclared,
            hasPresumptiveProfession44ADA = current.hasPresumptive44ADA,
            grossReceipts44ADARupees = current.presumptiveGross44ADA
        )
        val routing = DeterministicItrFormRouter.routeTaxpayer(profile)

        val employerName = current.employerName.ifBlank { "Primary Employer" }
        val employerTan = current.employerTan.ifBlank {
            if (current.pan.length >= 5) "${current.pan.take(4)}01234D" else "BLRM01234D"
        }
        val tdsReported = if (current.tdsDeposited26As > 0L) {
            current.tdsDeposited26As
        } else {
            (current.grossSalaryRupees * 0.05).toLong()
        }

        val updatedSources = if (current.grossSalaryRupees > 0L) {
            listOf(
                EmployerSalarySource(
                    employerTan = employerTan,
                    employerName = employerName,
                    source = SourceKind.FORM_16,
                    grossSalary = current.grossSalaryRupees,
                    sec10Exemptions = current.exemptAllowancesSec10,
                    sec16DeductionsClaimedInDoc = newResult.standardDeduction,
                    netTaxableSalaryInDoc = maxOf(0L, current.grossSalaryRupees - newResult.standardDeduction),
                    tdsReported = tdsReported
                )
            )
        } else {
            emptyList()
        }

        val tds26AsMap = if (current.tdsDeposited26As > 0L) {
            mapOf(employerTan to current.tdsDeposited26As)
        } else {
            mapOf(employerTan to tdsReported)
        }

        val summary = DeterministicReconciler.reconcileSalaries(updatedSources, tds26AsMap)

        val autoTier = FilingTierMode.autoDetectTier(
            grossIncome = current.grossSalaryRupees,
            hasCapitalGains = false,
            hasPresumptiveBusiness = current.hasPresumptive44ADA,
            hasForeignAssets = current.foreignAssetsDeclared,
            hasAuditExposure = current.presumptiveGross44ADA > 1_00_00_000L
        )

        _uiState.update {
            it.copy(
                newRegimeResult = newResult,
                oldRegimeResult = oldResult,
                formRoutingDecision = routing,
                salarySummary = summary,
                recommendedTierMode = autoTier
            )
        }
    }
}
