package `in`.zerotax.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AuthProvider(val displayName: String, val iconName: String) {
    EMAIL_PASSWORD("Email & Password", "mail"),
    GOOGLE("Google Account", "globe"),
    BIOMETRIC_VAULT("Device Biometrics (Fingerprint / Face / PIN)", "fingerprint"),
    GUEST_LOCAL("Local Encrypted Vault (Zero Registration)", "shield")
}

@Serializable
data class UserAuthProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val authProvider: AuthProvider,
    val isEmailVerified: Boolean = true,
    val lastLoginEpochMs: Long = System.currentTimeMillis(),
    val primaryPanLinked: String? = null
)

@Serializable
sealed class AuthSessionState {
    @Serializable
    object Unauthenticated : AuthSessionState()

    @Serializable
    data class Authenticating(val progressMessage: String) : AuthSessionState()

    @Serializable
    data class Authenticated(val user: UserAuthProfile) : AuthSessionState()

    @Serializable
    data class AuthError(val errorMessage: String, val isRecoverable: Boolean = true) : AuthSessionState()
}

sealed class AuthCredentialRequest {
    data class EmailPasswordSignIn(val email: String, val passwordRaw: String) : AuthCredentialRequest()
    data class EmailPasswordSignUp(val email: String, val passwordRaw: String, val fullName: String) : AuthCredentialRequest()
    data class GoogleSignInToken(
        val idToken: String,
        val email: String,
        val displayName: String,
        val photoUrl: String? = null
    ) : AuthCredentialRequest()
    object BiometricUnlock : AuthCredentialRequest()
    object GuestOfflineAccess : AuthCredentialRequest()
}

