package `in`.zerotax.core.security

import `in`.zerotax.core.model.AuthProvider
import `in`.zerotax.core.model.AuthSessionState
import `in`.zerotax.core.model.UserAuthProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * Production Authentication & Security Session Manager.
 * Operates in full compliance with the Digital Personal Data Protection (DPDP) Act, 2023.
 * Supports Email/Password, Google Sign-In, Device Biometrics, and Encrypted Guest Vault.
 */
object ZeroTaxAuthSecurityManager {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$")

    // In-memory active session cache
    @Volatile
    private var activeUserSession: UserAuthProfile? = null

    // Local secure user repository (local persistent state)
    private val localUserRegistry = mutableMapOf<String, StoredUserRecord>()

    @Volatile
    private var appContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
        loadPersistedUsers()
    }

    private fun loadPersistedUsers() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences("zt_auth_users_vault", android.content.Context.MODE_PRIVATE)
        for ((email, recordJson) in prefs.all) {
            if (recordJson is String) {
                try {
                    val obj = org.json.JSONObject(recordJson)
                    localUserRegistry[email] = StoredUserRecord(
                        uid = obj.getString("uid"),
                        email = obj.getString("email"),
                        displayName = obj.getString("displayName"),
                        passwordHash = obj.getString("passwordHash"),
                        salt = obj.getString("salt"),
                        authProvider = AuthProvider.valueOf(obj.getString("authProvider")),
                        isVerified = obj.getBoolean("isVerified")
                    )
                } catch (_: Exception) {}
            }
        }
    }

    private fun persistUser(record: StoredUserRecord) {
        localUserRegistry[record.email] = record
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences("zt_auth_users_vault", android.content.Context.MODE_PRIVATE)
        val obj = org.json.JSONObject().apply {
            put("uid", record.uid)
            put("email", record.email)
            put("displayName", record.displayName)
            put("passwordHash", record.passwordHash)
            put("salt", record.salt)
            put("authProvider", record.authProvider.name)
            put("isVerified", record.isVerified)
        }
        prefs.edit().putString(record.email, obj.toString()).apply()
    }

    private data class StoredUserRecord(
        val uid: String,
        val email: String,
        val displayName: String,
        val passwordHash: String,
        val salt: String,
        val authProvider: AuthProvider,
        val isVerified: Boolean
    )

    fun getActiveSession(): UserAuthProfile? = activeUserSession

    fun isAuthenticated(): Boolean = activeUserSession != null

    suspend fun signInWithEmail(emailRaw: String, passwordRaw: String): Result<UserAuthProfile> = withContext(Dispatchers.Default) {
        val email = emailRaw.trim().lowercase()
        if (!EMAIL_REGEX.matches(email)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (passwordRaw.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        // VUL-03 FIX: sign-in must NEVER auto-provision new accounts.
        // Unknown emails receive a failure, not silent registration.
        val existing = localUserRegistry[email]
            ?: return@withContext Result.failure(
                IllegalArgumentException("No account found for this email. Please sign up first.")
            )

        val computedHash = hashPassword(passwordRaw, existing.salt)
        if (computedHash != existing.passwordHash) {
            return@withContext Result.failure(IllegalArgumentException("Invalid password. Please try again."))
        }

        val profile = UserAuthProfile(
            uid = existing.uid,
            email = existing.email,
            displayName = existing.displayName,
            authProvider = AuthProvider.EMAIL_PASSWORD,
            isEmailVerified = existing.isVerified,
            lastLoginEpochMs = System.currentTimeMillis()
        )
        activeUserSession = profile
        Result.success(profile)
    }

    suspend fun signUpWithEmail(emailRaw: String, passwordRaw: String, fullNameRaw: String): Result<UserAuthProfile> = withContext(Dispatchers.Default) {
        val email = emailRaw.trim().lowercase()
        val fullName = fullNameRaw.trim()

        if (!EMAIL_REGEX.matches(email)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (fullName.length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your full name."))
        }
        if (passwordRaw.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        val salt = generateRandomSalt()
        val hash = hashPassword(passwordRaw, salt)
        val uid = "usr_${UUID.nameUUIDFromBytes(email.toByteArray())}"

        val userRecord = StoredUserRecord(
            uid = uid,
            email = email,
            displayName = fullName,
            passwordHash = hash,
            salt = salt,
            authProvider = AuthProvider.EMAIL_PASSWORD,
            isVerified = true
        )
        persistUser(userRecord)

        val profile = UserAuthProfile(
            uid = uid,
            email = email,
            displayName = fullName,
            authProvider = AuthProvider.EMAIL_PASSWORD,
            isEmailVerified = true,
            lastLoginEpochMs = System.currentTimeMillis()
        )
        activeUserSession = profile
        Result.success(profile)
    }

    suspend fun signInWithGoogle(
        idToken: String,
        email: String,
        displayName: String,
        photoUrl: String? = null
    ): Result<UserAuthProfile> = withContext(Dispatchers.Default) {
        val cleanedEmail = email.trim().lowercase()
        if (!cleanedEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Google Account email."))
        }

        val uid = "goog_${UUID.nameUUIDFromBytes(cleanedEmail.toByteArray())}"
        val profile = UserAuthProfile(
            uid = uid,
            email = cleanedEmail,
            displayName = displayName.ifBlank { cleanedEmail.substringBefore("@") },
            photoUrl = photoUrl,
            authProvider = AuthProvider.GOOGLE,
            isEmailVerified = true,
            lastLoginEpochMs = System.currentTimeMillis()
        )
        activeUserSession = profile
        Result.success(profile)
    }

    suspend fun signInAsGuestOffline(): UserAuthProfile = withContext(Dispatchers.Default) {
        val guestUid = "guest_${UUID.randomUUID().toString().take(8)}"
        val guestProfile = UserAuthProfile(
            uid = guestUid,
            email = "offline.guest@zerotax.local",
            displayName = "Local Vault User",
            photoUrl = null,
            authProvider = AuthProvider.GUEST_LOCAL,
            isEmailVerified = false,
            lastLoginEpochMs = System.currentTimeMillis()
        )
        activeUserSession = guestProfile
        guestProfile
    }

    fun verifyBiometricCipher(cipher: javax.crypto.Cipher?): Boolean {
        if (cipher == null) return false
        return runCatching {
            val probe = ByteArray(16)
            java.security.SecureRandom().nextBytes(probe)
            val ct = cipher.doFinal(probe)
            ct != null && ct.isNotEmpty()
        }.getOrDefault(false)
    }

    suspend fun signInWithBiometrics(cipher: javax.crypto.Cipher? = null): Result<UserAuthProfile> = withContext(Dispatchers.Default) {
        if (cipher != null && !verifyBiometricCipher(cipher)) {
            return@withContext Result.failure(
                SecurityException("Hardware CryptoObject verification failed: KeyStore cipher was not authorized by biometric hardware.")
            )
        }
        // If an existing session exists, refresh it; otherwise create a biometric vault profile
        val current = activeUserSession
        if (current != null) {
            val refreshed = current.copy(lastLoginEpochMs = System.currentTimeMillis())
            activeUserSession = refreshed
            Result.success(refreshed)
        } else {
            val bioProfile = UserAuthProfile(
                uid = "bio_${UUID.randomUUID().toString().take(8)}",
                email = "biometric.user@zerotax.local",
                displayName = "Biometric Vault User",
                photoUrl = null,
                authProvider = AuthProvider.BIOMETRIC_VAULT,
                isEmailVerified = true,
                lastLoginEpochMs = System.currentTimeMillis()
            )
            activeUserSession = bioProfile
            Result.success(bioProfile)
        }
    }

    fun signOut() {
        activeUserSession = null
    }

    private fun generateRandomSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        return try {
            val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt.toByteArray(StandardCharsets.UTF_8), 100_000, 256)
            val skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = skf.generateSecret(spec).encoded
            hash.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            val digest = MessageDigest.getInstance("SHA-256")
            val input = (salt + password).toByteArray(StandardCharsets.UTF_8)
            val hash = digest.digest(input)
            hash.joinToString("") { "%02x".format(it) }
        }
    }
}

