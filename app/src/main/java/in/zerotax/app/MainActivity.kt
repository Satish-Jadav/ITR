package in.zerotax.app

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
import in.zerotax.app.ui.navigation.ZeroTaxFourTapSpineScaffold
import in.zerotax.app.ui.splash.SplashScreen
import in.zerotax.app.ui.theme.ZeroTaxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Strict Enterprise Security Invariant: Hardware-level screen capture and switcher preview prevention
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            ZeroTaxTheme {
                var isSplashFinished by remember { mutableStateOf(false) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = isSplashFinished,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
                        },
                        label = "AppLaunchTransition"
                    ) { finished ->
                        if (!finished) {
                            SplashScreen(
                                onInitializationComplete = { isSplashFinished = true }
                            )
                        } else {
                            ZeroTaxFourTapSpineScaffold()
                        }
                    }
                }
            }
        }
    }
}
