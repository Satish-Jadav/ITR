package `in`.zerotax.core.taxengine.advanced

import kotlin.math.abs
import kotlin.math.max

/**
 * Universal Broker Tax P&L & Trade Ingestion Engine
 *
 * Deterministically parses CSV exports from top Indian brokers (Zerodha Console, Groww,
 * Angel One, Upstox, Dhan) into statutory schedules:
 * 1. Section 111A (Short-Term Capital Gains on listed equities @ 20.0%)
 * 2. Section 112A (Long-Term Capital Gains on listed equities @ 12.5% above exemption)
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

    /**
     * VUL-16 FIX: RFC 4180-compliant CSV line parser.
     * Respects quoted tokens containing commas (e.g. "(1,25,000)" or "2,50,000")
     * so they are NOT split into fragmented column tokens.
     */
    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

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
        val headers = parseCsvLine(headerLine).map { it.replace("\"", "").lowercase() }

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
            val tokens = parseCsvLine(line).map { it.replace("\"", "").replace("₹", "").replace(" ", "") }
            if (tokens.size < 4) continue

            val pnlToken = if (pnlCol in tokens.indices) tokens[pnlCol] else tokens.last()
            // VUL-16 FIX: Strip Indian currency commas (e.g. "1,25,000") and parenthetical
            // negative format "(1,25,000)" before parsing.
            val cleanedToken = pnlToken
                .replace(",", "")
                .let { t -> if (t.startsWith("(") && t.endsWith(")")) "-${t.drop(1).dropLast(1)}" else t }
            val pnlVal = cleanedToken.toLongOrNull() ?: continue
            // VUL-14 FIX: Guard against abs(Long.MIN_VALUE) overflow (abs(MIN_VALUE) == MIN_VALUE in JVM).
            // Also guard cumulative fnoTurnover from silent overflow wrap-around.
            val absVal = if (pnlVal == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(pnlVal)

            val tradeType = if (typeCol in tokens.indices) tokens[typeCol].uppercase() else ""
            val isFno = tradeType.contains("FNO") || tradeType.contains("F&O") || tradeType.contains("FUT") || tradeType.contains("OPT") || line.contains("FNO", ignoreCase = true)
            val isLongTerm = tradeType.contains("LTCG") || tradeType.contains("LONG") || line.contains("LTCG", ignoreCase = true)

            if (isFno) {
                // VUL-14: Use pre-computed absVal; guard turnover accumulation against Long overflow.
                fnoTurnover = if (fnoTurnover > Long.MAX_VALUE - absVal) Long.MAX_VALUE else fnoTurnover + absVal
                fnoPnl += pnlVal
                parsedRows++
            } else if (isLongTerm) {
                if (pnlVal >= 0L) ltcg += pnlVal else ltcl += absVal
                parsedRows++
            } else {
                if (pnlVal >= 0L) stcg += pnlVal else stcl += absVal
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
