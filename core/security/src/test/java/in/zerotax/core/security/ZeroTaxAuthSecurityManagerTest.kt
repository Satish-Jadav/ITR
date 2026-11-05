package `in`.zerotax.core.security

import `in`.zerotax.core.model.AuthProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ZeroTaxAuthSecurityManagerTest {

    @BeforeEach
    fun setUp() {
        ZeroTaxAuthSecurityManager.signOut()
    }

    @Test
    fun testSignUpAndSignInWithEmail_Success() = runBlocking {
        val email = "taxpayer@zerotax.in"
        val password = "StrongPassword123!"
        val name = "Vikram Sharma"

        // 1. Sign up new account
        val signUpResult = ZeroTaxAuthSecurityManager.signUpWithEmail(email, password, name)
        assertTrue(signUpResult.isSuccess)
        val profile = signUpResult.getOrThrow()
        assertEquals(email, profile.email)
        assertEquals(name, profile.displayName)
        assertEquals(AuthProvider.EMAIL_PASSWORD, profile.authProvider)
        assertTrue(ZeroTaxAuthSecurityManager.isAuthenticated())

        // 2. Sign out
        ZeroTaxAuthSecurityManager.signOut()
        assertFalse(ZeroTaxAuthSecurityManager.isAuthenticated())

        // 3. Sign in with same credentials
        val signInResult = ZeroTaxAuthSecurityManager.signInWithEmail(email, password)
        assertTrue(signInResult.isSuccess)
        val loggedIn = signInResult.getOrThrow()
        assertEquals(email, loggedIn.email)
        assertEquals(name, loggedIn.displayName)
    }

    @Test
    fun testSignInWithEmail_InvalidEmailOrPassword_FailsGracefully() = runBlocking {
        val invalidEmailResult = ZeroTaxAuthSecurityManager.signInWithEmail("invalid-email", "secret123")
        assertTrue(invalidEmailResult.isFailure)
        assertTrue(invalidEmailResult.exceptionOrNull()?.message?.contains("valid email") == true)

        val shortPasswordResult = ZeroTaxAuthSecurityManager.signInWithEmail("valid@test.com", "123")
        assertTrue(shortPasswordResult.isFailure)
        assertTrue(shortPasswordResult.exceptionOrNull()?.message?.contains("at least 6 characters") == true)
    }

    @Test
    fun testSignInWithGoogle_CreatesGoogleAuthProfile() = runBlocking {
        val googleEmail = "vikram.google@gmail.com"
        val displayName = "Vikram Google"
        val idToken = "eyJhbGciOiJSUzI1NiIs..."

        val result = ZeroTaxAuthSecurityManager.signInWithGoogle(idToken, googleEmail, displayName)
        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()

        assertEquals(googleEmail, profile.email)
        assertEquals(displayName, profile.displayName)
        assertEquals(AuthProvider.GOOGLE, profile.authProvider)
        assertTrue(profile.uid.startsWith("goog_"))
        assertTrue(ZeroTaxAuthSecurityManager.isAuthenticated())
    }

    @Test
    fun testSignInAsGuestOffline_CreatesLocalEncryptedVaultProfile() = runBlocking {
        val guest = ZeroTaxAuthSecurityManager.signInAsGuestOffline()

        assertEquals(AuthProvider.GUEST_LOCAL, guest.authProvider)
        assertEquals("Local Vault User", guest.displayName)
        assertTrue(guest.uid.startsWith("guest_"))
        assertTrue(ZeroTaxAuthSecurityManager.isAuthenticated())
    }

    @Test
    fun testSignInWithBiometrics_CreatesBiometricSession() = runBlocking {
        val bioResult = ZeroTaxAuthSecurityManager.signInWithBiometrics()
        assertTrue(bioResult.isSuccess)
        val profile = bioResult.getOrThrow()

        assertEquals(AuthProvider.BIOMETRIC_VAULT, profile.authProvider)
        assertTrue(profile.uid.startsWith("bio_"))
        assertTrue(ZeroTaxAuthSecurityManager.isAuthenticated())
    }

    @Test
    fun testSignInWithEmail_UnregisteredAccount_FailsExplicitly() = runBlocking {
        // VUL-03 FIX: Sign-in must NEVER auto-register unknown accounts
        val unregisteredEmail = "ghost.unregistered@zerotax.local"
        val result = ZeroTaxAuthSecurityManager.signInWithEmail(unregisteredEmail, "SomePassword123")
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("No account found", ignoreCase = true))
    }
}

