# SQLCipher & Encrypted Room Vault
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }
-keep class in.zerotax.core.database.** { *; }

# Ephemeral WebView JavaScript Interface
-keepclassmembers class in.zerotax.feature.portalbridge.HardenedEphemeralPortalBridge$PortalJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# PDFBox Android Parser
-keep class com.tom_roush.pdfbox.** { *; }

# On-Device Android AICore
-keep class com.google.ai.edge.aicore.** { *; }

# Security and Cryptographic Models
-keep class in.zerotax.core.model.** { *; }
-keep class in.zerotax.core.security.** { *; }

# Remove Android Log calls in release builds for zero telemetry leakage & size minimization
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# R8 Full Mode Aggressive Size Optimizations
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Kotlin Metadata & Coroutine Debugger Stripping (Saves ~1.5MB)
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-assumenosideeffects class kotlinx.coroutines.DebugKt {
    public static boolean getDEBUG();
}

# Jetpack Compose Size Hardening
-dontwarn androidx.compose.**
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView {
    *** getText();
}

