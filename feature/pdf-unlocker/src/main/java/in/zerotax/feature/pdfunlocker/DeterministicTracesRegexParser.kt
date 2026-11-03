package in.zerotax.feature.pdfunlocker

import in.zerotax.core.model.EmployerSalarySource
import in.zerotax.core.model.SourceKind

object DeterministicTracesRegexParser {
    private val TAN_REGEX = Regex("""TAN\s+of\s+the\s+Deductor\s*[\r\n:]*\s*([A-Z]{4}[0-9]{5}[A-Z]{1})""", RegexOption.IGNORE_CASE)
    private val GROSS_17_1_REGEX = Regex("""1\.\s*Gross\s+Salary[\s\S]*?\(a\)\s*Salary\s+as\s+per\s+provisions\s+contained\s+in\s+section\s+17\(1\)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val PERKS_17_2_REGEX = Regex("""\(b\)\s*Value\s+of\s+perquisites\s+under\s+section\s+17\(2\)[\s\S]*?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val EXEMPT_10_REGEX = Regex("""Total\s+amount\s+of\s+exemption\s+claimed\s+under\s+section\s+10[\s\S]*?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val STD_DED_16IA_REGEX = Regex("""Standard\s+deduction\s+under\s+section\s+16\(ia\)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val PROF_TAX_16III_REGEX = Regex("""Tax\s+on\s+employment\s+under\s+section\s+16\(iii\)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val NET_SALARY_REGEX = Regex("""Income\s+chargeable\s+under\s+the\s+head\s+["']?Salaries["']?[\s\S]*?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val TDS_PART_A_REGEX = Regex("""Total\s+Tax\s+Deposited\s*[\r\n:]*\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)

    fun parseStandardTracesForm16(fullText: String): Result<EmployerSalarySource> {
        val tan = TAN_REGEX.find(fullText)?.groupValues?.get(1)?.uppercase()
            ?: return Result.failure(IllegalArgumentException("Not a standard TRACES Form 16: Deductor TAN missing"))

        val gross = extractLong(GROSS_17_1_REGEX, fullText) + extractLong(PERKS_17_2_REGEX, fullText)
        val sec10 = extractLong(EXEMPT_10_REGEX, fullText)
        val sec16 = extractLong(STD_DED_16IA_REGEX, fullText) + extractLong(PROF_TAX_16III_REGEX, fullText)
        val netPrinted = extractLong(NET_SALARY_REGEX, fullText)
        val tds = extractLong(TDS_PART_A_REGEX, fullText)

        // TODO(Phase-2): Extract CAMS/KFintech/Zerodha/Groww ISIN-level Capital Gains tables for Schedule 112A.
        // TODO(Phase-3): Add E*TRADE / Charles Schwab / Fidelity RSU vest lot & Form 1042-S dividend table parser.
        return Result.success(
            EmployerSalarySource(
                employerTan = tan,
                employerName = "TRACES Verified ($tan)",
                source = SourceKind.FORM_16,
                grossSalary = gross,
                sec10Exemptions = sec10,
                sec16DeductionsClaimedInDoc = sec16,
                netTaxableSalaryInDoc = netPrinted,
                tdsReported = tds
            )
        )
    }

    private fun extractLong(regex: Regex, text: String): Long =
        regex.find(text)?.groupValues?.get(1)?.replace(",", "")?.substringBefore(".")?.toLongOrNull() ?: 0L
}
