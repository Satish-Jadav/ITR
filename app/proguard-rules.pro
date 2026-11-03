# SQLCipher & Encrypted Room Vault
-keep class net.sqlcipher.** { *; }
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

# Remove Android Log calls in release builds for zero telemetry leakage
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
