package `in`.zerotax.feature.portalbridge

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.time.LocalDate

enum class ChallanTaxType(val code: String, val minorHead: String, val description: String) {
    ADVANCE_TAX("100", "100", "Advance Tax (100)"),
    SELF_ASSESSMENT_TAX("300", "300", "Self-Assessment Tax (300)"),
    REGULAR_ASSESSMENT("400", "400", "Tax on Regular Assessment (400)")
}

data class ChallanValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

object UpiChallan280IntentLauncher {
    private const val MAJOR_HEAD_0021 = "0021" // Income Tax (Other than Companies)
    private val BSR_REGEX = Regex("""^\d{7}$""")
    private val CIN_REGEX = Regex("""^\d{5}$""")
    private val PAN_REGEX = Regex("""^[A-Z]{5}[0-9]{4}[A-Z]$""")

    fun buildUpiPaymentUriString(
        pan: String,
        amountRupees: Long,
        assessmentYear: String = "2026-27",
        taxType: ChallanTaxType = ChallanTaxType.SELF_ASSESSMENT_TAX,
        merchantVpa: String = "incometax@sbi"
    ): String {
        // VUL-10 FIX: Removed PAN from tr= parameter to prevent PAN leakage to UPI apps.
        // Note: in production, launchUpiPaymentIntent() should use setPackage() to restrict
        // to a known verified UPI app (e.g. BHIM) or present a chooser with whitelisted apps.
        val note = "ITD-CHALLAN280-AY$assessmentYear-M${taxType.minorHead}"
        val txnRef = "ITR_CHALLAN_${taxType.code}_${System.currentTimeMillis()}"
        return "upi://pay?pa=$merchantVpa&pn=Income+Tax+Department&mc=9311&tr=$txnRef&tn=$note&am=$amountRupees&cu=INR"
    }

    fun buildUpiPaymentUri(
        pan: String,
        amountRupees: Long,
        assessmentYear: String = "2026-27",
        taxType: ChallanTaxType = ChallanTaxType.SELF_ASSESSMENT_TAX,
        merchantVpa: String = "incometax@sbi"
    ): Uri {
        return Uri.parse(buildUpiPaymentUriString(pan, amountRupees, assessmentYear, taxType, merchantVpa))
    }

    fun launchUpiPaymentIntent(context: Context, upiUri: Uri): Result<Unit> {
        return runCatching {
            // VUL-10 FIX: Use BHIM (official government UPI app) as the preferred package.
            // Falls back to implicit intent if BHIM is not installed (user can then select from chooser).
            // This prevents rogue third-party apps from silently intercepting the payment intent.
            val BHIM_PACKAGE = "in.org.npci.upiapp"
            val intent = Intent(Intent.ACTION_VIEW, upiUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage(BHIM_PACKAGE)
            }
            val bhimAvailable = context.packageManager.resolveActivity(intent, 0) != null
            if (bhimAvailable) {
                context.startActivity(intent)
            } else {
                // Fallback: implicit intent with chooser (user sees all UPI apps)
                val fallbackIntent = Intent(Intent.ACTION_VIEW, upiUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Pay via UPI").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    fun createUpiPaymentIntent(
        taxAmountRupees: Long,
        pan: String,
        assessmentYear: String
    ): Intent {
        val upiUri = buildUpiPaymentUri(
            pan = pan,
            amountRupees = taxAmountRupees,
            assessmentYear = assessmentYear,
            taxType = ChallanTaxType.SELF_ASSESSMENT_TAX
        )
        return Intent(Intent.ACTION_VIEW, upiUri)
    }

    fun validateChallanReceipt(
        bsrCode: String,
        challanSerialNo: String,
        depositDateIso: String,
        pan: String,
        amountRupees: Long
    ): ChallanValidationResult {
        val errors = mutableListOf<String>()

        if (!BSR_REGEX.matches(bsrCode.trim())) {
            errors.add("BSR code must be exactly 7 numeric digits")
        }
        if (!CIN_REGEX.matches(challanSerialNo.trim())) {
            errors.add("Challan Serial Number must be exactly 5 numeric digits")
        }
        if (!PAN_REGEX.matches(pan.trim().uppercase())) {
            errors.add("Invalid 10-character PAN format")
        }
        if (amountRupees <= 0L) {
            errors.add("Challan amount must be greater than zero")
        }

        runCatching {
            val date = LocalDate.parse(depositDateIso)
            if (date.isAfter(LocalDate.now())) {
                errors.add("Challan deposit date cannot be in the future")
            }
        }.onFailure {
            errors.add("Invalid deposit date format, expected YYYY-MM-DD")
        }

        return ChallanValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

