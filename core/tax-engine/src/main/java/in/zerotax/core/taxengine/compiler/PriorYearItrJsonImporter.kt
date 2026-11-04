package `in`.zerotax.core.taxengine.compiler

import `in`.zerotax.core.model.BankAccountDetails
import org.json.JSONObject

data class BroughtForwardLossRecord(
    val assessmentYear: String,
    val lossType: String, // "STCL", "LTCL", "HOUSE_PROPERTY"
    val originalLossAmount: Long,
    val previouslySetOffAmount: Long,
    val unabsorbedCarriedForwardAmount: Long,
    val filedBeforeSec139_1DueDate: Boolean
)

data class PriorYearImportedProfile(
    val pan: String,
    val fullName: String,
    val dateOfBirth: String,
    val mobileNumber: String,
    val email: String,
    val primaryBank: BankAccountDetails?,
    val landlordPan: String?,
    val broughtForwardLosses: List<BroughtForwardLossRecord>,
    val priorAssessmentYear: String
)

object PriorYearItrJsonImporter {

    fun parsePriorYearItrJson(jsonString: String): PriorYearImportedProfile? {
        return try {
            val root = JSONObject(jsonString)
            // Can be ITR1, ITR2, ITR4, or Prefill
            val formObj = when {
                root.has("ITR1") -> root.getJSONObject("ITR1")
                root.has("ITR2") -> root.getJSONObject("ITR2")
                root.has("ITR4") -> root.getJSONObject("ITR4")
                root.has("ITR") -> root.getJSONObject("ITR")
                else -> root
            }

            val personalInfo = formObj.optJSONObject("PersonalInfo") ?: formObj
            val pan = personalInfo.optString("PAN", personalInfo.optString("pan", ""))
            val firstName = personalInfo.optString("FirstName", "")
            val surName = personalInfo.optString("SurNameOrOrgName", personalInfo.optString("LastName", ""))
            val fullName = "$firstName $surName".trim().ifBlank { personalInfo.optString("Name", "") }
            val dob = personalInfo.optString("DOB", personalInfo.optString("dob", ""))
            val mobile = personalInfo.optString("MobileNo", personalInfo.optString("mobile", ""))
            val email = personalInfo.optString("EmailAddress", personalInfo.optString("email", ""))

            // Parse Bank Account
            var bank: BankAccountDetails? = null
            val bankObj = formObj.optJSONObject("BankAccountDetails") ?: formObj.optJSONObject("Bank")
            if (bankObj != null) {
                bank = BankAccountDetails(
                    bankName = bankObj.optString("BankName", "Bank"),
                    accountNumber = bankObj.optString("AccountNumber", bankObj.optString("accountNo", "")),
                    ifscCode = bankObj.optString("IFSCCode", bankObj.optString("ifsc", "")),
                    accountType = bankObj.optString("AccountType", "SAVINGS"),
                    isRefundAccount = true,
                    isPreValidated = true
                )
            }

            // Parse Landlord PAN
            val landlordPan = formObj.optJSONObject("ScheduleHP")?.optString("TenantPAN")
                ?: formObj.optJSONObject("RentReceipts")?.optString("LandlordPAN")

            // Parse Schedule CFL (Losses)
            val cflList = mutableListOf<BroughtForwardLossRecord>()
            val cflArray = formObj.optJSONObject("ScheduleCFL")?.optJSONArray("LossDetails")
            if (cflArray != null) {
                for (i in 0 until cflArray.length()) {
                    val item = cflArray.getJSONObject(i)
                    val ay = item.optString("AssessmentYear", "")
                    val lossType = item.optString("LossType", "STCL")
                    val orig = item.optLong("OriginalLoss", 0L)
                    val setOff = item.optLong("SetOffAmount", 0L)
                    val unabsorbed = item.optLong("UnabsorbedLoss", orig - setOff)
                    val timely = item.optBoolean("FiledOnOrBeforeDueDate", true)

                    cflList.add(
                        BroughtForwardLossRecord(
                            assessmentYear = ay,
                            lossType = lossType,
                            originalLossAmount = orig,
                            previouslySetOffAmount = setOff,
                            unabsorbedCarriedForwardAmount = unabsorbed,
                            filedBeforeSec139_1DueDate = timely
                        )
                    )
                }
            }

            val ay = formObj.optJSONObject("Form_ITR1")?.optString("AssessmentYear")
                ?: formObj.optString("AssessmentYear", "2024-25")

            PriorYearImportedProfile(
                pan = pan,
                fullName = fullName,
                dateOfBirth = dob,
                mobileNumber = mobile,
                email = email,
                primaryBank = bank,
                landlordPan = landlordPan,
                broughtForwardLosses = cflList,
                priorAssessmentYear = ay
            )
        } catch (e: Exception) {
            null
        }
    }
}

