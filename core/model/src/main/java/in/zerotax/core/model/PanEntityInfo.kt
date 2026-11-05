package `in`.zerotax.core.model

import kotlinx.serialization.Serializable

/**
 * 4th character entity type mapping under Section 139A of the Income-tax Act, 1961.
 */
@Serializable
enum class PanEntityType(
    val code: Char,
    val title: String,
    val description: String,
    val isSupportedInPersonalSuite: Boolean,
    val isSupportedInCorporateSuite: Boolean = false,
    val eligibleItrForms: List<ItrFormType>
) {
    INDIVIDUAL(
        code = 'P',
        title = "Individual",
        description = "Natural Person / Salaried Employee / Professional / Sole Proprietor",
        isSupportedInPersonalSuite = true,
        isSupportedInCorporateSuite = false,
        eligibleItrForms = listOf(ItrFormType.ITR_1_SAHAJ, ItrFormType.ITR_2, ItrFormType.ITR_4_SUGAM)
    ),
    HUF(
        code = 'H',
        title = "Hindu Undivided Family (HUF)",
        description = "Family entity assessed under Hindu law through Karta",
        isSupportedInPersonalSuite = true,
        isSupportedInCorporateSuite = false,
        eligibleItrForms = listOf(ItrFormType.ITR_2, ItrFormType.ITR_3)
    ),
    COMPANY(
        code = 'C',
        title = "Company",
        description = "Private Limited / Public Limited / OPC Corporate Body",
        isSupportedInPersonalSuite = false,
        isSupportedInCorporateSuite = true,
        eligibleItrForms = listOf(ItrFormType.ITR_6, ItrFormType.ITR_7)
    ),
    FIRM(
        code = 'F',
        title = "Partnership Firm / LLP",
        description = "Partnership Firm or Limited Liability Partnership",
        isSupportedInPersonalSuite = false,
        isSupportedInCorporateSuite = true,
        eligibleItrForms = listOf(ItrFormType.ITR_5, ItrFormType.ITR_4_SUGAM)
    ),
    AOP(
        code = 'A',
        title = "Association of Persons (AOP)",
        description = "Association of individuals or entities pooling resources",
        isSupportedInPersonalSuite = false,
        isSupportedInCorporateSuite = true,
        eligibleItrForms = listOf(ItrFormType.ITR_5)
    ),
    BOI(
        code = 'B',
        title = "Body of Individuals (BOI)",
        description = "Conglomeration of individuals without formal partnership",
        isSupportedInPersonalSuite = false,
        isSupportedInCorporateSuite = true,
        eligibleItrForms = listOf(ItrFormType.ITR_5)
    ),
    TRUST(
        code = 'T',
        title = "Trust / Charitable Institution",
        description = "Charitable, Religious, or Political Entity u/s 139(4A)-(4D)",
        isSupportedInPersonalSuite = false,
        isSupportedInCorporateSuite = true,
        eligibleItrForms = listOf(ItrFormType.ITR_7)
    ),
    LOCAL_AUTHORITY(
        code = 'L',
        title = "Local Authority",
        description = "Municipal Corporation, Cantonment Board, or Panchayat",
        isSupportedInPersonalSuite = false,
        isSupportedInCorporateSuite = true,
        eligibleItrForms = listOf(ItrFormType.ITR_5)
    ),
    ARTIFICIAL_JURIDICAL(
        code = 'J',
        title = "Artificial Juridical Person",
        description = "Statutory Corporations, Universities, or Deities",
        isSupportedInPersonalSuite = false,
        isSupportedInCorporateSuite = true,
        eligibleItrForms = listOf(ItrFormType.ITR_5, ItrFormType.ITR_7)
    ),
    GOVERNMENT_AGENCY(
        code = 'G',
        title = "Government Agency",
        description = "Central / State Government Department or Undertaking",
        isSupportedInPersonalSuite = false,
        isSupportedInCorporateSuite = false,
        eligibleItrForms = listOf(ItrFormType.UNSUPPORTED_REQUIRES_CA_AUDIT_44AB)
    ),
    UNKNOWN(
        code = '?',
        title = "Unknown Entity",
        description = "Unrecognized entity classification",
        isSupportedInPersonalSuite = false,
        eligibleItrForms = emptyList()
    );

    companion object {
        fun fromChar(char: Char): PanEntityType {
            val upper = char.uppercaseChar()
            return entries.firstOrNull { it.code == upper } ?: UNKNOWN
        }
    }
}

/**
 * Result of real-time PAN syntax and entity decoding.
 */
@Serializable
data class PanDecodedDetails(
    val rawPan: String,
    val entityType: PanEntityType,
    val surnameInitial: Char?,
    val isValidFormat: Boolean,
    val isEligibleFor87ARebate: Boolean,
    val statusSummary: String
)

/**
 * High-performance deterministic decoder for Indian Permanent Account Numbers (PAN).
 * Strictly evaluates 10-character alphanumeric structure under CBDT rules:
 * - Chars 1..3: Alphabetic series (AAA to ZZZ)
 * - Char 4: Status / Entity category ('P', 'H', 'C', 'F', etc.)
 * - Char 5: First character of taxpayer surname/last name
 * - Chars 6..9: Sequential numeric digits (0001 to 9999)
 * - Char 10: Alphabetic check digit
 */
object PanEntityDecoder {
    private val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")

    fun decode(pan: String): PanDecodedDetails {
        val cleanPan = pan.trim().uppercase()
        val isValid = PAN_REGEX.matches(cleanPan)
        val fourthChar = if (cleanPan.length >= 4) cleanPan[3] else '?'
        val fifthChar = if (cleanPan.length >= 5) cleanPan[4] else null
        val entity = PanEntityType.fromChar(fourthChar)

        val isEligible87A = entity == PanEntityType.INDIVIDUAL
        val summary = when {
            cleanPan.isEmpty() -> "Please enter your 10-character PAN"
            !isValid && cleanPan.length < 10 -> "Entering PAN (${cleanPan.length}/10)..."
            !isValid -> "Invalid PAN syntax. Must be 5 letters, 4 numbers, 1 letter (e.g. ABCDE1234F)"
            entity == PanEntityType.INDIVIDUAL -> "Valid Individual PAN (Eligible for ITR-1, ITR-2, ITR-4 & Section 87A rebate)"
            entity == PanEntityType.HUF -> "Valid HUF PAN (Eligible for ITR-2 & ITR-3. Sec 87A rebate is restricted to individuals)"
            entity == PanEntityType.COMPANY -> "Valid Company PAN (Eligible for ITR-6 Corporate / ITR-7 Section 115BAA/BAB return)"
            entity == PanEntityType.FIRM -> "Valid Partnership / LLP PAN (Eligible for ITR-5 / ITR-4 Sugam u/s 40(b))"
            entity == PanEntityType.TRUST -> "Valid Trust / NGO PAN (Eligible for ITR-7 Section 11/12 85% application return)"
            entity in listOf(PanEntityType.AOP, PanEntityType.BOI, PanEntityType.LOCAL_AUTHORITY, PanEntityType.ARTIFICIAL_JURIDICAL) ->
                "Valid ${entity.title} PAN (Eligible for ITR-5 Entity Return)"
            else -> "${entity.title} detected (Eligible for specialized entity return)"
        }

        return PanDecodedDetails(
            rawPan = cleanPan,
            entityType = entity,
            surnameInitial = fifthChar,
            isValidFormat = isValid,
            isEligibleFor87ARebate = isEligible87A,
            statusSummary = summary
        )
    }
}
