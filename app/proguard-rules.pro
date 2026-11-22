# SQLCipher & Encrypted Room Vault
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }
-keep class in.zerotax.core.database.** { *; }

# Ephemeral WebView JavaScript Interface
-keepclassmembers class in.zerotax.feature.portalbridge.HardenedEphemeralPortalBridge$PortalJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# PDFBox Android Parser - Tree-shake unused fonts, 3D annotations & forms
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.tom_roush.fontbox.**
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-keep class com.tom_roush.pdfbox.android.PDFBoxResourceLoader { *; }
-keep class com.tom_roush.pdfbox.pdmodel.PDDocument { *; }
-keep class com.tom_roush.pdfbox.pdmodel.PDPage { *; }
-keep class com.tom_roush.pdfbox.pdmodel.PDPageContentStream { *; }
-keep class com.tom_roush.pdfbox.pdmodel.common.PDRectangle { *; }
-keep class com.tom_roush.pdfbox.pdmodel.font.PDType1Font { *; }
-keep class com.tom_roush.pdfbox.text.PDFTextStripper { *; }
-keep class com.tom_roush.pdfbox.io.** { <init>(...); }

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
-optimizationpasses 2
-allowaccessmodification
-repackageclasses ''
-dontnote **

# Kotlin Metadata & Coroutine Debugger Stripping (Saves ~1.5MB)
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-assumenosideeffects class kotlinx.coroutines.DebugKt {
    public static boolean getDEBUG();
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullParameter(...);
    public static void checkExpressionValueIsNotNull(...);
}

# Jetpack Compose Size Hardening
-dontwarn androidx.compose.**
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView {
    *** getText();
}

