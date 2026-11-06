package `in`.zerotax.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.zerotax.app.ui.auth.AuthenticationScreen
import `in`.zerotax.app.ui.ca.CaAuditorPortalScreen
import `in`.zerotax.app.ui.navigation.ZeroTaxFourTapSpineScaffold
import `in`.zerotax.app.ui.splash.SplashScreen
import `in`.zerotax.app.ui.theme.ZeroTaxTheme
import `in`.zerotax.app.viewmodel.ZeroTaxViewModel
import `in`.zerotax.core.model.AuthSessionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                            targetState = isSplashFinished to (uiState.authSessionState is AuthSessionState.Authenticated),
                            transitionSpec = {
                                fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
                            },
                            label = "AppLaunchTransition"
                        ) { (splashFinished, isAuthenticated) ->
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
                                        viewModel.signInWithBiometrics()
                                    },
                                    onLaunchCaPortal = {
                                        showCaPortalInMain = true
                                    },
                                    isLoading = uiState.isAuthLoading,
                                    errorMessage = uiState.authErrorMessage
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
}
