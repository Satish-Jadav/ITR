package `in`.zerotax.core.model

import kotlinx.serialization.Serializable

@Serializable
data class InstitutionPdfPasswordRule(
    val institutionId: String,
    val institutionName: String,
    val documentCategory: String, // "Tax & Identity", "Banking", "Investments"
    val passwordPatternRule: String,
    val exampleDescription: String,
    val exampleInput: String,
    val officialHelplineHint: String
)

/**
 * Authoritative registry of standard Indian financial institution PDF password conventions.
 * Enables zero-friction document unlocking when automated permutation matching requires user hint disambiguation.
 */
object InstitutionPdfPasswordRegistry {

    private val dynamicRules = mutableListOf<InstitutionPdfPasswordRule>()

    val defaultRules: List<InstitutionPdfPasswordRule> = listOf(
        InstitutionPdfPasswordRule(
            institutionId = "TRACES_FORM16",
            institutionName = "Income Tax Department / TRACES",
            documentCategory = "Tax & Identity",
            passwordPatternRule = "PAN in LOWERCASE + Date of Birth (DDMMYYYY)",
            exampleDescription = "If PAN is ABCDE1234F and DOB is 15-Aug-1990",
            exampleInput = "abcde1234f15081990",
            officialHelplineHint = "Default TRACES Form 16 / 16A password standard"
        ),
        InstitutionPdfPasswordRule(
            institutionId = "ITD_AIS_TIS",
            institutionName = "Income Tax Portal (AIS / TIS / 26AS)",
            documentCategory = "Tax & Identity",
            passwordPatternRule = "PAN in LOWERCASE + Date of Birth (DDMMYYYY)",
            exampleDescription = "If PAN is ABCDE1234F and DOB is 15-Aug-1990",
            exampleInput = "abcde1234f15081990",
            officialHelplineHint = "Annual Information Statement / Taxpayer Information Summary"
        ),
        InstitutionPdfPasswordRule(
            institutionId = "UIDAI_EAADHAAR",
            institutionName = "UIDAI (e-Aadhaar)",
            documentCategory = "Tax & Identity",
            passwordPatternRule = "First 4 letters of name in CAPITAL + Birth Year (YYYY)",
            exampleDescription = "If name is SATISH and birth year is 1990",
            exampleInput = "SATI1990",
            officialHelplineHint = "Standard UIDAI e-Aadhaar PDF password"
        ),
        InstitutionPdfPasswordRule(
            institutionId = "HDFC_BANK",
            institutionName = "HDFC Bank",
            documentCategory = "Banking",
            passwordPatternRule = "Customer ID (NetBanking ID) OR PAN in CAPITAL",
            exampleDescription = "If Cust ID is 12345678 or PAN is ABCDE1234F",
            exampleInput = "12345678 or ABCDE1234F",
            officialHelplineHint = "HDFC Bank statement standard format"
        ),
        InstitutionPdfPasswordRule(
            institutionId = "SBI_BANK",
            institutionName = "State Bank of India (SBI)",
            documentCategory = "Banking",
            passwordPatternRule = "Last 5 digits of Mobile Number + DOB in DDMMYY or DDMMYYYY",
            exampleDescription = "If Mobile ends in 98765 and DOB is 15-08-1990",
            exampleInput = "98765150890 or 9876515081990",
            officialHelplineHint = "SBI Online statement password formula"
        ),
        InstitutionPdfPasswordRule(
            institutionId = "ICICI_BANK",
            institutionName = "ICICI Bank",
            documentCategory = "Banking",
            passwordPatternRule = "First 4 letters of name in LOWERCASE + Birth Date & Month (DDMM)",
            exampleDescription = "If name is SATISH and DOB is 15-Aug",
            exampleInput = "sati1508",
            officialHelplineHint = "ICICI Bank e-statement password standard"
        ),
        InstitutionPdfPasswordRule(
            institutionId = "AXIS_BANK",
            institutionName = "Axis Bank",
            documentCategory = "Banking",
            passwordPatternRule = "First 4 letters of name in CAPITAL + Last 4 digits of Customer ID",
            exampleDescription = "If name is SATISH and Cust ID ends in 5678",
            exampleInput = "SATI5678",
            officialHelplineHint = "Axis Bank statement password format"
        ),
        InstitutionPdfPasswordRule(
            institutionId = "KOTAK_BANK",
            institutionName = "Kotak Mahindra Bank",
            documentCategory = "Banking",
            passwordPatternRule = "Customer Relationship Number (CRN) or Date of Birth (DDMMYYYY)",
            exampleDescription = "If CRN is 98765432 or DOB is 15-Aug-1990",
            exampleInput = "98765432 or 15081990",
            officialHelplineHint = "Printed on Kotak Debit Card below name"
        ),
        InstitutionPdfPasswordRule(
            institutionId = "CAMS_KFINTECH",
            institutionName = "CAMS / KFintech (Mutual Fund CAS)",
            documentCategory = "Investments",
            passwordPatternRule = "Password chosen by you when requesting statement from CAMS/KFintech",
            exampleDescription = "Custom password entered during web generation",
            exampleInput = "YourChosenCasPassword",
            officialHelplineHint = "Consolidated Account Statement (CAS) email security key"
        ),
        InstitutionPdfPasswordRule(
            institutionId = "ZERODHA_GROWW",
            institutionName = "Zerodha / Groww (Tax P&L / Capital Gains)",
            documentCategory = "Investments",
            passwordPatternRule = "PAN in CAPITAL",
            exampleDescription = "If PAN is ABCDE1234F",
            exampleInput = "ABCDE1234F",
            officialHelplineHint = "Annual tax computation P&L report"
        )
    )

    val rules: List<InstitutionPdfPasswordRule>
        get() = synchronized(this) {
            if (dynamicRules.isNotEmpty()) dynamicRules.toList() else defaultRules
        }

    fun updateRules(newRules: List<InstitutionPdfPasswordRule>) {
        synchronized(this) {
            dynamicRules.clear()
            dynamicRules.addAll(newRules)
        }
    }

    fun findRuleByInstitution(idOrKeyword: String): InstitutionPdfPasswordRule? {
        val query = idOrKeyword.lowercase().trim()
        return rules.firstOrNull {
            it.institutionId.lowercase().contains(query) ||
            it.institutionName.lowercase().contains(query)
        }
    }
}
