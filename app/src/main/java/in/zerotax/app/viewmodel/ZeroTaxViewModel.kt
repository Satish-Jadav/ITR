package `in`.zerotax.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.zerotax.app.ui.navigation.OnboardingTap
import `in`.zerotax.app.ui.navigation.WorkspaceTab
import `in`.zerotax.core.model.*
import `in`.zerotax.core.security.CanonicalWarningHasher
import `in`.zerotax.core.security.ZeroTaxAuthSecurityManager
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
    val pan: String = "ABCDE1234F",
    val assessmentYear: String = "2026-27",
    val grossSalaryRupees: Long = 12_75_000L,
    val exemptAllowancesSec10: Long = 0L,
    val professionalTaxSec16: Long = 0L,
    val section80CInvestments: Long = 1_50_000L,
    val section80DSelf: Long = 25_000L,
    val section80CCD1BNps: Long = 50_000L,
    val homeLoanInterestSec24b: Long = 0L,
    val presumptiveGross44ADA: Long = 0L,
    val hasPresumptive44ADA: Boolean = false,
    val foreignAssetsDeclared: Boolean = false,
    val isBiometricUnlocked: Boolean = true,
    val citizenCategory: CitizenCategory = CitizenCategory.REGULAR_BELOW_60,
    val showPasswordCheatsheet: Boolean = false,
    val filingTierMode: FilingTierMode = FilingTierMode.SAHAJ_SIMPLE,
    val recommendedTierMode: FilingTierMode = FilingTierMode.SAHAJ_SIMPLE,
    val authSessionState: AuthSessionState = AuthSessionState.Unauthenticated,
    val authErrorMessage: String? = null,
    val isAuthLoading: Boolean = false,
    val challanPayments: List<ScheduleItChallanEntry> = emptyList(),
    val totalTaxPaidViaChallan: Long = 0L,
    val showTaxPaymentHub: Boolean = false,
    val newRegimeResult: RegimeComputationResult,
    val oldRegimeResult: RegimeComputationResult,
    val formRoutingDecision: FormRoutingDecision,
    val salarySummary: ReconciledSalarySummary,
    val auditTrail: List<AuditEntryUiModel> = emptyList(),
    val lazyAuditTrail: LazyLoadState<List<AuditEntryUiModel>> = LazyLoadState.Uninitialized,
    val isVaultShredded: Boolean = false
)

class ZeroTaxViewModel : ViewModel() {

    private val initialSources = listOf(
        EmployerSalarySource(
            employerTan = "BLRM01234D",
            employerName = "Tech Innovations Pvt Ltd",
            source = SourceKind.FORM_16,
            grossSalary = 12_75_000L,
            sec10Exemptions = 0L,
            sec16DeductionsClaimedInDoc = 75_000L,
            netTaxableSalaryInDoc = 12_00_000L,
            tdsReported = 60_000L
        )
    )
    private val initial26AsTds = mapOf("BLRM01234D" to 60_000L)

    private val _uiState: MutableStateFlow<ZeroTaxUiState>

    init {
        val initialSummary = DeterministicReconciler.reconcileSalaries(initialSources, initial26AsTds)
        val initialProfile = TaxpayerRoutingProfile(
            residentialStatus = ResidentialStatus.RESIDENT_ORDINARILY_RESIDENT,
            totalIncomeRupees = 12_00_000L,
            housePropertyCount = 1
        )
        val initialRouting = DeterministicItrFormRouter.routeTaxpayer(initialProfile)
        val initialInput = TaxpayerFinancialInput(
            citizenCategory = CitizenCategory.REGULAR_BELOW_60,
            grossSalary = 12_75_000L,
            section80CInvestments = 1_50_000L,
            section80DSelfAndFamily = 25_000L,
            section80CCD1BSelfNps = 50_000L,
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
    }

    val uiState: StateFlow<ZeroTaxUiState> = _uiState.asStateFlow()

    fun onPanChanged(pan: String) {
        _uiState.update { it.copy(pan = pan) }
        recompute()
    }

    fun onGrossSalaryChanged(gross: Long) {
        _uiState.update { it.copy(grossSalaryRupees = gross) }
        recompute()
    }

    fun onSection80CChanged(amount: Long) {
        _uiState.update { it.copy(section80CInvestments = amount) }
        recompute()
    }

    fun onSection80DChanged(amount: Long) {
        _uiState.update { it.copy(section80DSelf = amount) }
        recompute()
    }

    fun onSection80CCD1BChanged(amount: Long) {
        _uiState.update { it.copy(section80CCD1BNps = amount) }
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
        _uiState.update { it.copy(filingTierMode = tier) }
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
     * Lazily loads the audit trail entries on-demand when the user views the Audit Vault or Privacy tab.
     * Prevents eager keystore/disk operations during application launch.
     */
    fun loadAuditTrailLazy() {
        if (_uiState.value.lazyAuditTrail is LazyLoadState.Success) return
        viewModelScope.launch {
            _uiState.update { it.copy(lazyAuditTrail = LazyLoadState.Loading) }
            val initialAudit = listOf(
                AuditEntryUiModel(
                    auditId = "AUD-INIT-001",
                    eventCategory = "VAULT_INITIALIZATION",
                    description = "Hardware StrongBox Master Key initialized for PAN ABCDE1234F",
                    warningTextHashSha256 = CanonicalWarningHasher.computeSha256("INITIAL_HARDWARE_KEYSTORE_BINDING"),
                    timestampFormatted = "14:57:27 IST"
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
            val fakeIdToken = "g_token_${System.currentTimeMillis()}"
            val result = ZeroTaxAuthSecurityManager.signInWithGoogle(fakeIdToken, email, name)
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authSessionState = AuthSessionState.Authenticated(profile),
                            authErrorMessage = null
                        )
                    }
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
            housePropertyCount = 1,
            holdsForeignAssetsOrForeignIncome = current.foreignAssetsDeclared,
            hasPresumptiveProfession44ADA = current.hasPresumptive44ADA,
            grossReceipts44ADARupees = current.presumptiveGross44ADA
        )
        val routing = DeterministicItrFormRouter.routeTaxpayer(profile)

        val updatedSources = listOf(
            EmployerSalarySource(
                employerTan = "BLRM01234D",
                employerName = "Tech Innovations Pvt Ltd",
                source = SourceKind.FORM_16,
                grossSalary = current.grossSalaryRupees,
                sec10Exemptions = current.exemptAllowancesSec10,
                sec16DeductionsClaimedInDoc = newResult.standardDeduction,
                netTaxableSalaryInDoc = current.grossSalaryRupees - newResult.standardDeduction,
                tdsReported = 60_000L
            )
        )
        val summary = DeterministicReconciler.reconcileSalaries(updatedSources, initial26AsTds)

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
