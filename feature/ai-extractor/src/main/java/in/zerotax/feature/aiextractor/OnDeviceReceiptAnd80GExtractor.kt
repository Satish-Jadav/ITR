package `in`.zerotax.feature.aiextractor

import `in`.zerotax.core.model.DonationCategory
import `in`.zerotax.core.model.Section80GDonationInput
import `in`.zerotax.core.security.UidaiAadhaarVerhoeffScrubber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ExtractedRentReceipt(
    val landlordPan: String?,
    val annualRentRupees: Long,
    val landlordName: String,
    val propertyAddress: String,
    val isDigitalPayment: Boolean
)

data class ExtractedMedicalInsurance80D(
    val policyHolderName: String,
    val isSeniorCitizenCovered: Boolean,
    val premiumAmountRupees: Long,
    val isPreventiveHealthCheckup: Boolean,
    val paymentModeDigital: Boolean
)

data class ExtractedDonation80G(
    val doneeName: String,
    val doneePan: String?,
    val form10BeArn: String?,
    val donationAmountRupees: Long,
    val isCashPayment: Boolean,
    val category: DonationCategory,
    val eligibleDeductionRupees: Long
)

class OnDeviceReceiptAnd80GExtractor(
    private val modelBridge: LocalGenerativeModelBridge
) {
    suspend fun extractRentReceipt(ocrText: String): ExtractedRentReceipt = withContext(Dispatchers.Default) {
        val sanitized = UidaiAadhaarVerhoeffScrubber.scrubAllAadhaarDigits(ocrText)
        val prompt = """
            You are an on-device deterministic extractor for Indian House Rent Receipts.
            Extract strictly valid JSON with keys:
            "landlord_pan": string or null,
            "annual_rent_rupees": integer,
            "landlord_name": string,
            "property_address": string,
            "is_digital_payment": boolean
            
            OCR_INPUT:
            $sanitized
        """.trimIndent()

        val rawOutput = modelBridge.generateContent(prompt)
        val json = runCatching {
            JSONObject(rawOutput?.trim()?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim() ?: "{}")
        }.getOrDefault(JSONObject())

        val pan = json.optString("landlord_pan", "").takeIf { it.isNotBlank() }
        val rent = json.optLong("annual_rent_rupees", 0L)
        val name = json.optString("landlord_name", "Landlord")
        val addr = json.optString("property_address", "")
        val isDigital = json.optBoolean("is_digital_payment", true)

        ExtractedRentReceipt(pan, rent, name, addr, isDigital)
    }

    suspend fun extractDonation80G(ocrText: String): ExtractedDonation80G = withContext(Dispatchers.Default) {
        val sanitized = UidaiAadhaarVerhoeffScrubber.scrubAllAadhaarDigits(ocrText)
        val prompt = """
            Extract Section 80G donation / Form 10BE certificate details into JSON:
            "donee_name": string,
            "donee_pan": string or null,
            "form_10be_arn": string or null,
            "amount_rupees": integer,
            "is_cash": boolean,
            "category": "HUNDRED_PERCENT_NO_LIMIT" | "FIFTY_PERCENT_NO_LIMIT" | "FIFTY_PERCENT_WITH_10PCT_CAP"
            
            OCR_INPUT:
            $sanitized
        """.trimIndent()

        val rawOutput = modelBridge.generateContent(prompt)
        val json = runCatching {
            JSONObject(rawOutput?.trim()?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim() ?: "{}")
        }.getOrDefault(JSONObject())

        val name = json.optString("donee_name", "Registered Charitable Trust")
        val pan = json.optString("donee_pan", "").takeIf { it.isNotBlank() }
        val arn = json.optString("form_10be_arn", "").takeIf { it.isNotBlank() }
        val amount = json.optLong("amount_rupees", 0L)
        val isCash = json.optBoolean("is_cash", false)
        val catStr = json.optString("category", "FIFTY_PERCENT_WITH_10PCT_CAP")
        val category = runCatching { DonationCategory.valueOf(catStr) }.getOrDefault(DonationCategory.FIFTY_PERCENT_WITH_10PCT_CAP)

        // Section 80G(5D): Cash donations exceeding ₹2,000 are disallowed
        val eligible = if (isCash && amount > 2_000L) 0L else amount

        ExtractedDonation80G(name, pan, arn, amount, isCash, category, eligible)
    }
}

