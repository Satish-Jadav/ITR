package `in`.zerotax.feature.portalbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import `in`.zerotax.core.security.logging.ZeroTaxLogger
import java.util.regex.Pattern

/**
 * 1-Tap SMS OTP Auto-Reader using Google Play Services SMS Retriever API.
 * Automatically intercepts incoming 6-digit Aadhaar / ITD OTP without requiring
 * the user to manually switch to their SMS messaging app.
 */
class ItdAndAaOtpAutoReader(
    private val context: Context,
    private val onOtpReceived: (String) -> Unit
) {
    companion object {
        private const val TAG = "ZeroTaxOtpReader"
        const val SMS_RETRIEVED_ACTION = "com.google.android.gms.auth.api.phone.SMS_RETRIEVED"
        const val EXTRA_SMS_MESSAGE = "com.google.android.gms.auth.api.phone.EXTRA_SMS_MESSAGE"
        const val EXTRA_STATUS = "com.google.android.gms.auth.api.phone.EXTRA_STATUS"
        private val SIX_DIGIT_OTP_PATTERN = Pattern.compile("\\b(\\d{6})\\b")
    }

    private var isRegistered = false

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SMS_RETRIEVED_ACTION) {
                val extras = intent.extras ?: return
                val message = extras.getString(EXTRA_SMS_MESSAGE)
                if (!message.isNullOrBlank()) {
                    ZeroTaxLogger.i(TAG, "SMS received via Retriever API, scanning for 6-digit OTP")
                    val matcher = SIX_DIGIT_OTP_PATTERN.matcher(message)
                    if (matcher.find()) {
                        val otpCode = matcher.group(1)
                        if (!otpCode.isNullOrBlank() && otpCode.length == 6) {
                            ZeroTaxLogger.i(TAG, "Successfully extracted 6-digit OTP code")
                            onOtpReceived(otpCode)
                            stopListening()
                        }
                    }
                }
            }
        }
    }

    /**
     * Starts listening for the incoming Aadhaar / ITD verification SMS.
     */
    fun startListening() {
        if (isRegistered) return
        try {
            // Register broadcast receiver for SMS retriever
            val filter = IntentFilter(SMS_RETRIEVED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(smsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(smsReceiver, filter)
            }
            isRegistered = true

            // Trigger Google Play Services SMS Retriever client
            try {
                val client = com.google.android.gms.auth.api.phone.SmsRetriever.getClient(context)
                client.startSmsRetriever()
                    .addOnSuccessListener {
                        ZeroTaxLogger.i(TAG, "SmsRetriever client started successfully")
                    }
                    .addOnFailureListener { e ->
                        ZeroTaxLogger.w(TAG, "SmsRetriever client failed to start: ${e.message}")
                    }
            } catch (e: Throwable) {
                ZeroTaxLogger.w(TAG, "Play services SmsRetriever not available on device: ${e.message}")
            }

            ZeroTaxLogger.i(TAG, "ItdAndAaOtpAutoReader started listening")
        } catch (e: Exception) {
            ZeroTaxLogger.w(TAG, "Failed to register SMS receiver: ${e.message}")
        }
    }

    /**
     * Manually delivers an OTP string (e.g. for testing or clipboard auto-detection).
     */
    fun injectOtpManually(code: String) {
        val clean = code.trim()
        if (clean.length == 6 && clean.all { it.isDigit() }) {
            ZeroTaxLogger.i(TAG, "Manually injected verified OTP")
            onOtpReceived(clean)
        }
    }

    /**
     * Stops listening and unregisters broadcast receiver.
     */
    fun stopListening() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(smsReceiver)
            isRegistered = false
            ZeroTaxLogger.i(TAG, "ItdAndAaOtpAutoReader stopped listening")
        } catch (e: Exception) {
            ZeroTaxLogger.w(TAG, "Failed to unregister SMS receiver: ${e.message}")
        }
    }
}
