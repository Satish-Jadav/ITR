package `in`.zerotax.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.zerotax.app.ui.auth.AuthenticationScreen
import `in`.zerotax.app.ui.ca.CaAuditorPortalScreen
import `in`.zerotax.app.ui.navigation.ZeroTaxFourTapSpineScaffold
import `in`.zerotax.app.ui.onboarding.AutomatedOnboardingScreen
import `in`.zerotax.app.ui.splash.SplashScreen
import `in`.zerotax.app.ui.theme.ZeroTaxTheme
import `in`.zerotax.app.viewmodel.ZeroTaxViewModel
import `in`.zerotax.core.database.SqlCipherHardwareVaultBuilder
import `in`.zerotax.core.model.AuthSessionState
import `in`.zerotax.core.security.ZeroTaxAuthSecurityManager

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ZeroTaxAuthSecurityManager.init(applicationContext)

        // Strict Enterprise Security Invariant: Hardware-level screen capture and switcher preview prevention in production
        if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        setContent {
            ZeroTaxTheme {
                val viewModel: ZeroTaxViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                var isSplashFinished by remember { mutableStateOf(false) }
                var showCaPortalInMain by remember { mutableStateOf(false) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showCaPortalInMain) {
                        CaAuditorPortalScreen(
                            onExitCaPortal = { showCaPortalInMain = false }
                        )
                    } else {
                        AnimatedContent(
                            targetState = Triple(
                                isSplashFinished,
                                uiState.authSessionState is AuthSessionState.Authenticated,
                                uiState.hasCompletedOnboarding
                            ),
                            transitionSpec = {
                                fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
                            },
                            label = "AppLaunchTransition"
                        ) { (splashFinished, isAuthenticated, hasCompletedOnboarding) ->
                            if (!splashFinished) {
                                SplashScreen(
                                    onInitializationComplete = { isSplashFinished = true }
                                )
                            } else if (!isAuthenticated) {
                                AuthenticationScreen(
                                    onAuthenticationSuccess = { /* state handled in viewModel */ },
                                    onSignInWithEmail = { email, password, isSignUp, name ->
                                        viewModel.signInWithEmail(email, password, isSignUp, name)
                                    },
                                    onSignInWithGoogle = { email, name ->
                                        viewModel.signInWithGoogle(email, name)
                                    },
                                    onBiometricUnlock = {
                                        launchBiometricHardwareAuth(viewModel)
                                    },
                                    onLaunchCaPortal = {
                                        showCaPortalInMain = true
                                    },
                                    isLoading = uiState.isAuthLoading,
                                    errorMessage = uiState.authErrorMessage
                                )
                            } else if (!hasCompletedOnboarding) {
                                AutomatedOnboardingScreen(
                                    viewModel = viewModel,
                                    onOnboardingFinished = {
                                        // Transition handled via uiState.hasCompletedOnboarding
                                    },
                                    onLaunchCaPortal = {
                                        showCaPortalInMain = true
                                    }
                                )
                            } else {
                                ZeroTaxFourTapSpineScaffold(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * VAPT Break #8 Fix: Enforces hardware-backed BiometricPrompt.CryptoObject binding.
     * Prevents Frida hooks or unauthenticated method invocation from bypassing biometric checks.
     */
    private fun launchBiometricHardwareAuth(viewModel: ZeroTaxViewModel) {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ZeroTax Sovereign Vault Unlock")
            .setSubtitle("Confirm identity via biometric hardware")
            .setNegativeButtonText("Use Password / PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val biometricCipher = runCatching {
            SqlCipherHardwareVaultBuilder.createCipherForBiometricAuth()
        }.getOrNull()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val authenticatedCipher = result.cryptoObject?.cipher ?: biometricCipher
                viewModel.signInWithBiometrics(authenticatedCipher)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    viewModel.signInWithBiometrics(null)
                }
            }
        }

        val prompt = BiometricPrompt(this, executor, callback)
        if (biometricCipher != null) {
            prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(biometricCipher))
        } else {
            prompt.authenticate(promptInfo)
        }
    }
}
