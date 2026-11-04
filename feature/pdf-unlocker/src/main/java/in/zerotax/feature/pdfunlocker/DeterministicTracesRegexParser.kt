package `in`.zerotax.feature.pdfunlocker

import `in`.zerotax.core.model.EmployerSalarySource
import `in`.zerotax.core.model.SourceKind

object DeterministicTracesRegexParser {
    private val TAN_REGEX = Regex("""TAN\s+of\s+the\s+Deductor\s*[\r\n:]*\s*([A-Z]{4}[0-9]{5}[A-Z]{1})""", RegexOption.IGNORE_CASE)
    private val GROSS_17_1_REGEX = Regex("""section\s+17\(1\)[^0-9\r\n]*([\d,]+)""", RegexOption.IGNORE_CASE)
    private val PERKS_17_2_REGEX = Regex("""section\s+17\(2\)[^0-9\r\n]*([\d,]+)""", RegexOption.IGNORE_CASE)
    private val EXEMPT_10_REGEX = Regex("""section\s+10[^0-9\r\n]*([\d,]+)""", RegexOption.IGNORE_CASE)
    private val STD_DED_16IA_REGEX = Regex("""section\s+16\(ia\)[^0-9\r\n]*([\d,]+)""", RegexOption.IGNORE_CASE)
    private val PROF_TAX_16III_REGEX = Regex("""section\s+16\(iii\)[^0-9\r\n]*([\d,]+)""", RegexOption.IGNORE_CASE)
    private val NET_SALARY_REGEX = Regex("""["']?Salaries["']?[^0-9\r\n]*([\d,]+)""", RegexOption.IGNORE_CASE)
    private val TDS_PART_A_REGEX = Regex("""Total\s+Tax\s+Deposited[^0-9\r\n]*([\d,]+)""", RegexOption.IGNORE_CASE)
    private val SEC_80C_REGEX = Regex("""(?:Section\s+80C|80C\b)[^\d\r\n]*?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val SEC_80CCD1B_REGEX = Regex("""(?:80CCD\(1B\)|80CCD\s*\(1B\))[^\d\r\n]*?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val SEC_80CCD2_REGEX = Regex("""(?:80CCD\(2\)|80CCD\s*\(2\))[^\d\r\n]*?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val SEC_80D_REGEX = Regex("""(?:Section\s+80D|80D\b)[^\d\r\n]*?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val SEC_10_13A_REGEX = Regex("""(?:10\(13A\)|House\s+Rent\s+Allowance)[^\d\r\n]*?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)

    fun parseStandardTracesForm16(fullText: String): Result<EmployerSalarySource> {
        val tan = TAN_REGEX.find(fullText)?.groupValues?.get(1)?.uppercase()
            ?: return Result.failure(IllegalArgumentException("Not a standard TRACES Form 16: Deductor TAN missing"))

        val gross = extractLong(GROSS_17_1_REGEX, fullText) + extractLong(PERKS_17_2_REGEX, fullText)
        val sec10 = extractLong(EXEMPT_10_REGEX, fullText)
        val sec16 = extractLong(STD_DED_16IA_REGEX, fullText) + extractLong(PROF_TAX_16III_REGEX, fullText)
        val netPrinted = extractLong(NET_SALARY_REGEX, fullText)
        val tds = extractLong(TDS_PART_A_REGEX, fullText)

        val sec80C = extractLong(SEC_80C_REGEX, fullText)
        val sec80CCD1B = extractLong(SEC_80CCD1B_REGEX, fullText)
        val sec80CCD2 = extractLong(SEC_80CCD2_REGEX, fullText)
        val sec80D = extractLong(SEC_80D_REGEX, fullText)
        val hra1013A = extractLong(SEC_10_13A_REGEX, fullText)

        return Result.success(
            EmployerSalarySource(
                employerTan = tan,
                employerName = "TRACES Verified ($tan)",
                source = SourceKind.FORM_16,
                grossSalary = gross,
                sec10Exemptions = sec10,
                sec16DeductionsClaimedInDoc = sec16,
                netTaxableSalaryInDoc = netPrinted,
                tdsReported = tds,
                sec80C = sec80C,
                sec80CCD1B = sec80CCD1B,
                sec80CCD2EmployerNps = sec80CCD2,
                sec80D = sec80D,
                exemptHra10_13A = hra1013A
            )
        )
    }

    private fun extractLong(regex: Regex, text: String): Long =
        regex.find(text)?.groupValues?.get(1)?.replace(",", "")?.substringBefore(".")?.toLongOrNull() ?: 0L
}
