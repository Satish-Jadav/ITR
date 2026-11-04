package `in`.zerotax.core.taxengine.advanced

import kotlin.math.abs
import kotlin.math.max

/**
 * Universal Broker Tax P&L & Trade Ingestion Engine
 *
 * Deterministically parses CSV exports from top Indian brokers (Zerodha Console, Groww,
 * Angel One, Upstox, Dhan) into statutory schedules:
 * 1. Section 111A (Short-Term Capital Gains on listed equities @ 20.0%)
 * 2. Section 112A (Long-Term Capital Gains on listed equities @ 12.5% above ₹1.25L exemption)
 * 3. Short-Term & Long-Term Capital Losses (Schedule CYLA & CFL)
 * 4. F&O Derivative & Intraday Absolute Turnover (ICAI Guidance Note method for Sec 44AB)
 */
object UniversalBrokerPnLParser {

    data class ParsedTradeSummary(
        val totalStcg111AGains: Long = 0L,
        val totalStcl111ALosses: Long = 0L,
        val totalLtcg112AGains: Long = 0L,
        val totalLtcl112ALosses: Long = 0L,
        val totalFnoAbsoluteTurnover: Long = 0L,
        val totalFnoNetProfitOrLoss: Long = 0L,
        val rowCountParsed: Int = 0,
        val detectedBroker: String = "Generic Indian Broker"
    )

    fun parseBrokerCsv(csvContent: String): ParsedTradeSummary {
        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return ParsedTradeSummary()

        var broker = "Generic Indian Broker"
        if (csvContent.contains("Zerodha", ignoreCase = true) || csvContent.contains("tradingsymbol", ignoreCase = true)) {
            broker = "Zerodha Console"
        } else if (csvContent.contains("Groww", ignoreCase = true)) {
            broker = "Groww Tax Report"
        } else if (csvContent.contains("Angel", ignoreCase = true)) {
            broker = "Angel One"
        }

        val headerIndex = lines.indexOfFirst { it.contains("ISIN", ignoreCase = true) || it.contains("Symbol", ignoreCase = true) || it.contains("PnL", ignoreCase = true) }
        val headerLine = if (headerIndex >= 0) lines[headerIndex] else lines.first()
        val headers = headerLine.split(",").map { it.trim().replace("\"", "").lowercase() }

        val pnlCol = headers.indexOfFirst { it == "pnl" || it.contains("profit") || it.contains("gain") }.let { if (it >= 0) it else headers.lastIndex }
        val typeCol = headers.indexOfFirst { it.contains("type") }

        var stcg = 0L
        var stcl = 0L
        var ltcg = 0L
        var ltcl = 0L
        var fnoTurnover = 0L
        var fnoPnl = 0L
        var parsedRows = 0

        val dataLines = if (headerIndex >= 0) lines.drop(headerIndex + 1) else lines
        for (line in dataLines) {
            val tokens = line.split(",").map { it.trim().replace("\"", "").replace("₹", "").replace(" ", "") }
            if (tokens.size < 4) continue

            val pnlToken = if (pnlCol in tokens.indices) tokens[pnlCol] else tokens.last()
            val pnlVal = pnlToken.toLongOrNull() ?: continue

            val tradeType = if (typeCol in tokens.indices) tokens[typeCol].uppercase() else ""
            val isFno = tradeType.contains("FNO") || tradeType.contains("F&O") || tradeType.contains("FUT") || tradeType.contains("OPT") || line.contains("FNO", ignoreCase = true)
            val isLongTerm = tradeType.contains("LTCG") || tradeType.contains("LONG") || line.contains("LTCG", ignoreCase = true)

            if (isFno) {
                fnoTurnover += abs(pnlVal)
                fnoPnl += pnlVal
                parsedRows++
            } else if (isLongTerm) {
                if (pnlVal >= 0L) ltcg += pnlVal else ltcl += abs(pnlVal)
                parsedRows++
            } else {
                if (pnlVal >= 0L) stcg += pnlVal else stcl += abs(pnlVal)
                parsedRows++
            }
        }

        return ParsedTradeSummary(
            totalStcg111AGains = stcg,
            totalStcl111ALosses = stcl,
            totalLtcg112AGains = ltcg,
            totalLtcl112ALosses = ltcl,
            totalFnoAbsoluteTurnover = fnoTurnover,
            totalFnoNetProfitOrLoss = fnoPnl,
            rowCountParsed = parsedRows,
            detectedBroker = broker
        )
    }
}
