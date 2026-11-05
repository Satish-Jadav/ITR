package `in`.zerotax.core.security.logging

/**
 * Enterprise Diagnostic Logger for ZeroTax-Local.
 * Dispatches to Android Logcat when running in Android OS, and standard output/error in JVM test environments.
 */
object ZeroTaxLogger {

    fun d(tag: String, message: String) {
        try {
            android.util.Log.d(tag, message)
        } catch (_: Throwable) {
            println("[$tag] [DEBUG] $message")
        }
    }

    fun i(tag: String, message: String) {
        try {
            android.util.Log.i(tag, message)
        } catch (_: Throwable) {
            println("[$tag] [INFO] $message")
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                android.util.Log.w(tag, message, throwable)
            } else {
                android.util.Log.w(tag, message)
            }
        } catch (_: Throwable) {
            println("[$tag] [WARN] $message ${throwable?.message.orEmpty()}")
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                android.util.Log.e(tag, message, throwable)
            } else {
                android.util.Log.e(tag, message)
            }
        } catch (_: Throwable) {
            System.err.println("[$tag] [ERROR] $message: ${throwable?.message.orEmpty()}")
            throwable?.printStackTrace()
        }
    }
}
