package `in`.zerotax.feature.pdfunlocker

import `in`.zerotax.core.model.ForeignEquityLotInput
import `in`.zerotax.core.model.Form1042SDividendStatement
import `in`.zerotax.core.model.ParsedForeignRsuLot

data class ForeignBrokerStatementReport(
    val brokerName: String,
    val rsuLots: List<ParsedForeignRsuLot>,
    val scheduleFaLots: List<ForeignEquityLotInput>,
    val form1042S: Form1042SDividendStatement?
)

object ForeignBrokerRsuAnd1042SParser {

    private val USD_REGEX = Regex("""\$\s*([\d,]+\.?\d*)""")
    private val DATE_ISO_REGEX = Regex("""\b(\d{4})[-/](\d{2})[-/](\d{2})\b""")
    private val FORM_1042S_TAG = Regex("""1042-S|Foreign\s+Person's\s+U\.S\.\s+Source\s+Income""", RegexOption.IGNORE_CASE)

    fun parseStatementText(fullText: String, broker: String = "E*TRADE"): ForeignBrokerStatementReport {
        val rsuLots = mutableListOf<ParsedForeignRsuLot>()
        val faLots = mutableListOf<ForeignEquityLotInput>()
        var form1042S: Form1042SDividendStatement? = null

        if (FORM_1042S_TAG.containsMatchIn(fullText)) {
            // Box 1: Gross Income, Box 7: Federal Tax Withheld
            val amounts = USD_REGEX.findAll(fullText).map {
                it.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            }.filter { it > 0.0 }.toList()

            val gross = amounts.getOrNull(0) ?: 0.0
            val tax = amounts.getOrNull(1) ?: 0.0
            val extractedPan = Regex("[A-Z]{5}[0-9]{4}[A-Z]").find(fullText)?.value.orEmpty()
            if (gross > 0.0) {
                form1042S = Form1042SDividendStatement(
                    recipientPanOrSsn = extractedPan,
                    withholdingAgentName = broker,
                    grossDividendIncomeUsd = gross,
                    federalTaxWithheldUsd = tax,
                    calendarYear = 2025
                )
            }
        }

        // Parse RSU lots
        val rsuRegex = Regex("""Shares\s*[:\-]?\s*([\d,]+\.?\d*)\s+Price\s*[:\-]?\s*\$?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
        val vestDateRegex = Regex("""Vest\s*Date\s*[:\-]?\s*(\d{4}[-/]\d{2}[-/]\d{2})""", RegexOption.IGNORE_CASE)
        val vestMatch = rsuRegex.find(fullText)
        if (vestMatch != null) {
            val shares = vestMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            val price = vestMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
            val vestDate = vestDateRegex.find(fullText)?.groupValues?.get(1) ?: DATE_ISO_REGEX.find(fullText)?.value.orEmpty()

            if (shares > 0.0 && price > 0.0) {
                val companyName = if (fullText.contains("Alphabet", ignoreCase = true)) "Alphabet Inc."
                    else if (fullText.contains("Apple", ignoreCase = true)) "Apple Inc."
                    else if (fullText.contains("Microsoft", ignoreCase = true)) "Microsoft Corp."
                    else "$broker Listed Issuer"

                rsuLots.add(
                    ParsedForeignRsuLot(
                        companyName = companyName,
                        vestDateIso = vestDate,
                        sharesVested = shares,
                        vestPriceUsd = price,
                        totalVestValueUsd = shares * price
                    )
                )

                faLots.add(
                    ForeignEquityLotInput(
                        lotId = "LOT-${rsuLots.size}",
                        companyName = companyName,
                        companyAddress = "United States",
                        countryCodeCbdt = "2",
                        zipCode = "00000",
                        acquireDateIso = vestDate,
                        sharesVestedOrBought = shares,
                        sharesSoldDuringCalendarYear = 0.0,
                        acquisitionPricePerShareUsd = price,
                        peakSharePriceDuringHoldingPeriodUsd = price,
                        peakSharePriceDateIso = vestDate,
                        dec31ClosingSharePriceUsd = price
                    )
                )
            }
        }

        return ForeignBrokerStatementReport(
            brokerName = broker,
            rsuLots = rsuLots,
            scheduleFaLots = faLots,
            form1042S = form1042S
        )
    }
}
