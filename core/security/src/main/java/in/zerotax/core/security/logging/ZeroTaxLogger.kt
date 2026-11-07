package `in`.zerotax.core.security.logging

import `in`.zerotax.core.security.UidaiAadhaarVerhoeffScrubber

/**
 * Enterprise Diagnostic Logger for ZeroTax.
 * Dispatches to Android Logcat when running in Android OS, and standard output/error in JVM test environments.
 * Enforces automatic PII scrubbing (Aadhaar, Virtual ID, and PAN) prior to log emission (VAPT Break #10).
 */
object ZeroTaxLogger {

    private val PAN_REGEX = Regex("""(?i)\b[A-Z]{5}[0-9]{4}[A-Z]\b""")

    fun scrubSensitive(message: String): String {
        val scrubbedAadhaar = UidaiAadhaarVerhoeffScrubber.scrubAllAadhaarAndVirtualIdDigits(message, strictChecksumOnly = false)
        return scrubbedAadhaar.replace(PAN_REGEX) { match ->
            val p = match.value
            p.take(2) + "***" + p.takeLast(2)
        }
    }

    fun d(tag: String, message: String) {
        val safe = scrubSensitive(message)
        try {
            android.util.Log.d(tag, safe)
        } catch (_: Throwable) {
            println("[$tag] [DEBUG] $safe")
        }
    }

    fun i(tag: String, message: String) {
        val safe = scrubSensitive(message)
        try {
            android.util.Log.i(tag, safe)
        } catch (_: Throwable) {
            println("[$tag] [INFO] $safe")
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val safe = scrubSensitive(message)
        try {
            if (throwable != null) {
                android.util.Log.w(tag, safe, throwable)
            } else {
                android.util.Log.w(tag, safe)
            }
        } catch (_: Throwable) {
            println("[$tag] [WARN] $safe ${throwable?.message.orEmpty()}")
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val safe = scrubSensitive(message)
        try {
            if (throwable != null) {
                android.util.Log.e(tag, safe, throwable)
            } else {
                android.util.Log.e(tag, safe)
            }
        } catch (_: Throwable) {
            System.err.println("[$tag] [ERROR] $safe: ${throwable?.message.orEmpty()}")
            throwable?.printStackTrace()
        }
    }
}
