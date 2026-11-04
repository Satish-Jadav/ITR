package `in`.zerotax.feature.pdfunlocker

import `in`.zerotax.core.model.Equity112ALot

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
    private val SGB_REGEX = Regex("""\bSGB\b|Sovereign\s+Gold\s+Bond""", RegexOption.IGNORE_CASE)
    private val BUYBACK_REGEX = Regex("""Buyback|Tender\s+Offer""", RegexOption.IGNORE_CASE)

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

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            ISIN_REGEX.find(trimmed)?.let {
                currentIsin = it.value
            }

            val isSgb = SGB_REGEX.containsMatchIn(trimmed)
            val isBuyback = BUYBACK_REGEX.containsMatchIn(trimmed)

            // Look for numeric columns: Qty, Buy Date, Sell Date, Buy Value, Sell Value
            val numbers = Regex("""[\d,]+\.?\d*""").findAll(trimmed).map {
                it.value.replace(",", "").toDoubleOrNull() ?: 0.0
            }.filter { it > 0.0 }.toList()

            if (numbers.size >= 3) {
                val qty = numbers[0]
                val buyVal = (numbers[1] * 100).toLong()
                val sellVal = (numbers[2] * 100).toLong()
                val gain = sellVal - buyVal

                val isStcg = trimmed.contains("STCG", ignoreCase = true) || trimmed.contains("Short", ignoreCase = true)

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
                            acquisitionDateEpochDay = 16000L,
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
                        purchaseDateIso = "2024-04-01",
                        saleDateIso = "2025-01-15",
                        purchaseCostPaise = buyVal,
                        saleProceedsPaise = sellVal,
                        isSgbRedemption = isSgb,
                        isShareBuyback = isBuyback,
                        isShortTerm111A = isStcg
                    )
                )
            }
        }

        return DomesticCapitalGainsReport(
            brokerOrRtaName = brokerName,
            pan = "ABCDE1234F",
            equity112ALots = equity112AList,
            tradeLots = lots,
            totalShortTermGainPaise = totalStcg,
            totalLongTermGainPaise = totalLtcg,
            sgbExemptProceedsPaise = sgbExempt,
            shareBuybackDeemedDividendPaise = buybackProceeds
        )
    }
}

