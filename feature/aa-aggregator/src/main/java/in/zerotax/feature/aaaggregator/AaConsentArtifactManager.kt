package `in`.zerotax.feature.aaaggregator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConsentLifeCycleState {
    PENDING_AUTHORIZATION,
    ACTIVE_GRANTED,
    REVOKED_BY_USER,
    EXPIRED
}

data class FipConsentArtifact(
    val consentHandleId: String,
    val financialInformationProviderName: String,
    val fipId: String,
    val fipCategory: String,
    val consentExpiryIso: String,
    val state: ConsentLifeCycleState
)

class AaConsentArtifactManager {
    private val _activeConsents = MutableStateFlow<List<FipConsentArtifact>>(emptyList())
    val activeConsents: StateFlow<List<FipConsentArtifact>> = _activeConsents.asStateFlow()

    fun registerConsent(consent: FipConsentArtifact) {
        _activeConsents.value = _activeConsents.value.filter { it.consentHandleId != consent.consentHandleId } + consent
    }

    suspend fun revokeConsentOneTap(consentHandleId: String): Result<Unit> {
        return runCatching {
            val current = _activeConsents.value
            val target = current.find { it.consentHandleId == consentHandleId }
                ?: error("Consent handle not found: $consentHandleId")

            // Update state locally
            _activeConsents.value = current.map {
                if (it.consentHandleId == consentHandleId) it.copy(state = ConsentLifeCycleState.REVOKED_BY_USER) else it
            }
        }
    }

    suspend fun revokeAllActiveConsents(): Result<Int> {
        return runCatching {
            val count = _activeConsents.value.count { it.state == ConsentLifeCycleState.ACTIVE_GRANTED }
            _activeConsents.value = _activeConsents.value.map {
                it.copy(state = ConsentLifeCycleState.REVOKED_BY_USER)
            }
            count
        }
    }
}

