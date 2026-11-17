package `in`.zerotax.feature.pdfunlocker

import `in`.zerotax.core.model.Equity112ALot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class ParsedBrokerTradeLot(
    val isin: String,
    val securityName: String,
    val units: Double,
    val purchaseDateIso: String,
    val saleDateIso: String,
    val purchaseCostPaise: Long,
    val saleProceedsPaise: Long,
    val fmvJan31_2018Paise: Long = 0L,
    val isSgbRedemption: Boolean = false,
    val isShareBuyback: Boolean = false,
    val isShortTerm111A: Boolean = false
)

data class DomesticCapitalGainsReport(
    val brokerOrRtaName: String,
    val pan: String,
    val equity112ALots: List<Equity112ALot>,
    val tradeLots: List<ParsedBrokerTradeLot>,
    val totalShortTermGainPaise: Long,
    val totalLongTermGainPaise: Long,
    val sgbExemptProceedsPaise: Long,
    val shareBuybackDeemedDividendPaise: Long
)

object DomesticCapitalGainsPdfParser {
    private val ISIN_REGEX = Regex("""[A-Z]{2}[A-Z0-9]{9}[0-9]""")
    private val DATE_REGEX = Regex("""\b(\d{2})[-/](\d{2})[-/](\d{4})\b""")
    private val PAN_REGEX = Regex("""[A-Z]{5}[0-9]{4}[A-Z]""")
    private val SGB_REGEX = Regex("""\bSGB\b|Sovereign\s+Gold\s+Bond""", RegexOption.IGNORE_CASE)
    private val BUYBACK_REGEX = Regex("""Buyback|Tender\s+Offer""", RegexOption.IGNORE_CASE)

    private fun parseDateToIso(day: String, month: String, year: String): String {
        return "%04d-%02d-%02d".format(year.toInt(), month.toInt(), day.toInt())
    }

    fun parseStatementText(fullText: String, brokerName: String = "Universal CAS"): DomesticCapitalGainsReport {
        val lines = fullText.lines()
        val lots = mutableListOf<ParsedBrokerTradeLot>()
        val equity112AList = mutableListOf<Equity112ALot>()

        var totalStcg = 0L
        var totalLtcg = 0L
        var sgbExempt = 0L
        var buybackProceeds = 0L

        var currentIsin = "INE000000000"
        var currentScrip = "EQUITY"
        var isCurrentSgb = false
        var isCurrentBuyback = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            ISIN_REGEX.find(trimmed)?.let {
                currentIsin = it.value
            }

            if (trimmed.contains("Security:", ignoreCase = true)) {
                isCurrentSgb = SGB_REGEX.containsMatchIn(trimmed)
                isCurrentBuyback = BUYBACK_REGEX.containsMatchIn(trimmed)
                currentScrip = trimmed.substringAfter("Security:").substringBefore("ISIN:").trim()
            }

            val isSgb = isCurrentSgb || SGB_REGEX.containsMatchIn(trimmed)
            val isBuyback = isCurrentBuyback || BUYBACK_REGEX.containsMatchIn(trimmed)

            // 1. Extract dates before numeric column parsing
            val dateMatches = DATE_REGEX.findAll(trimmed).toList()
            val parsedDates = dateMatches.mapNotNull { match ->
                runCatching {
                    parseDateToIso(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                }.getOrNull()
            }

            // 2. Strip date tokens so date digits do not pollute numerical parsing
            var lineWithoutDates = trimmed
            for (dm in dateMatches) {
                lineWithoutDates = lineWithoutDates.replace(dm.value, " ")
            }

            // Look for numeric columns: Qty, Buy Value, Sell Value
            val numbers = Regex("""[\d,]+\.?\d*""").findAll(lineWithoutDates).map {
                it.value.replace(",", "").toDoubleOrNull() ?: 0.0
            }.filter { it > 0.0 }.toList()

            if (numbers.size >= 3) {
                val qty = numbers[0]
                val buyVal = (numbers[1] * 100).toLong()
                val sellVal = (numbers[2] * 100).toLong()
                val gain = sellVal - buyVal

                val purchaseDateIso = parsedDates.getOrNull(0) ?: "2024-04-01"
                val saleDateIso = parsedDates.getOrNull(1) ?: parsedDates.getOrNull(0) ?: "2025-01-15"

                val holdingDays = runCatching {
                    val pDate = LocalDate.parse(purchaseDateIso, DateTimeFormatter.ISO_LOCAL_DATE)
                    val sDate = LocalDate.parse(saleDateIso, DateTimeFormatter.ISO_LOCAL_DATE)
                    ChronoUnit.DAYS.between(pDate, sDate)
                }.getOrDefault(366L)

                val isExplicitStcg = trimmed.contains("STCG", ignoreCase = true) || trimmed.contains("Short", ignoreCase = true)
                val isStcg = isExplicitStcg || holdingDays <= 365

                val epochDay = runCatching {
                    LocalDate.parse(purchaseDateIso, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay()
                }.getOrDefault(16000L)

                if (isSgb) {
                    sgbExempt += sellVal
                } else if (isBuyback) {
                    buybackProceeds += sellVal
                } else if (isStcg) {
                    totalStcg += gain
                } else {
                    totalLtcg += gain
                    equity112AList.add(
                        Equity112ALot(
                            isin = currentIsin,
                            acquisitionDateEpochDay = epochDay,
                            actualCostTotal = buyVal / 100L,
                            fmvJan31_2018Total = buyVal / 100L,
                            saleConsiderationTotal = sellVal / 100L,
                            transferExpenses = 0L
                        )
                    )
                }

                lots.add(
                    ParsedBrokerTradeLot(
                        isin = currentIsin,
                        securityName = currentScrip,
                        units = qty,
                        purchaseDateIso = purchaseDateIso,
                        saleDateIso = saleDateIso,
                        purchaseCostPaise = buyVal,
                        saleProceedsPaise = sellVal,
                        isSgbRedemption = isSgb,
                        isShareBuyback = isBuyback,
                        isShortTerm111A = isStcg
                    )
                )
            }
        }

        val extractedPan = PAN_REGEX.find(fullText)?.value.orEmpty()

        return DomesticCapitalGainsReport(
            brokerOrRtaName = brokerName,
            pan = extractedPan,
            equity112ALots = equity112AList,
            tradeLots = lots,
            totalShortTermGainPaise = totalStcg,
            totalLongTermGainPaise = totalLtcg,
            sgbExemptProceedsPaise = sgbExempt,
            shareBuybackDeemedDividendPaise = buybackProceeds
        )
    }
}
