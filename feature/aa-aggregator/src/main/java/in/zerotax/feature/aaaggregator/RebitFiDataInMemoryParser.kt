package `in`.zerotax.feature.aaaggregator

import `in`.zerotax.core.model.Equity112ALot
import org.json.JSONObject

data class RebitDepositExtraction(
    val bankName: String,
    val accountMasked: String,
    val savingsInterestTotalPaise: Long,
    val termDepositInterestTotalPaise: Long,
    val candidateDeductionTransactions: List<Pair<String, Long>>
)

data class RebitMutualFundEquityExtraction(
    val registrarOrDepository: String,
    val lots: List<Equity112ALot>,
    val totalRealizedGainRupees: Long
)

object RebitFiDataInMemoryParser {

    fun parseDepositJson(rawPayload: String): RebitDepositExtraction {
        return try {
            val json = JSONObject(rawPayload)
            val bank = json.optString("bank_name", "Scheduled Commercial Bank")
            val acc = json.optString("masked_account", "XXXX-XXXX-0000")

            var sbInterest = 0L
            var tdInterest = 0L
            val deductions = mutableListOf<Pair<String, Long>>()

            val txns = json.optJSONArray("transactions")
            if (txns != null) {
                for (i in 0 until txns.length()) {
                    val t = txns.getJSONObject(i)
                    val nar = t.optString("narration", "").uppercase()
                    val amt = (t.optDouble("amount", 0.0) * 100).toLong()
                    val type = t.optString("type", "DEBIT").uppercase()

                    if (type == "CREDIT" && (nar.contains("INT") || nar.contains("INTEREST"))) {
                        if (nar.contains("FD") || nar.contains("TERM")) {
                            tdInterest += amt
                        } else {
                            sbInterest += amt
                        }
                    } else if (amt >= 50000L) { // >= ₹500
                        deductions.add(nar to amt / 100L)
                    }
                }
            }

            RebitDepositExtraction(
                bankName = bank,
                accountMasked = acc,
                savingsInterestTotalPaise = sbInterest,
                termDepositInterestTotalPaise = tdInterest,
                candidateDeductionTransactions = deductions
            )
        } catch (_: Throwable) {
            RebitDepositExtraction("Unknown Bank", "XXXX", 0L, 0L, emptyList())
        }
    }

    fun parseMutualFundAndEquityJson(rawPayload: String): RebitMutualFundEquityExtraction {
        return try {
            val json = JSONObject(rawPayload)
            val rta = json.optString("depository_or_rta", "CAMS/KFintech")
            val lots = mutableListOf<Equity112ALot>()
            var totalGains = 0L

            val holdings = json.optJSONArray("holdings")
            if (holdings != null) {
                for (i in 0 until holdings.length()) {
                    val h = holdings.getJSONObject(i)
                    val isin = h.optString("isin", "INE000000000")
                    val cost = h.optLong("cost_rupees", 0L)
                    val value = h.optLong("current_or_sale_value_rupees", 0L)
                    val gain = value - cost

                    // VUL-18 FIX: Only include REALIZED gains in totalRealizedGainRupees and Schedule 112A lots.
                    // Unrealized MTM (mark-to-market) gains on holdings NOT yet redeemed must NOT be taxed.
                    // Income Tax Act only taxes capital gains on actual transfer/sale (Sec 2(47)).
                    // Check for explicit redemption markers: "is_redeemed" flag or presence of "sale_date".
                    val isRedeemed = h.optBoolean("is_redeemed", false)
                    val hasSaleDate = h.has("sale_date") && !h.optString("sale_date").isNullOrBlank()

                    if (isRedeemed || hasSaleDate) {
                        // Realized gain/loss — include in Schedule 112A and total
                        totalGains += gain
                        lots.add(
                            Equity112ALot(
                                isin = isin,
                                acquisitionDateEpochDay = 16000L,
                                actualCostTotal = cost,
                                fmvJan31_2018Total = cost,
                                saleConsiderationTotal = value,
                                transferExpenses = 0L
                            )
                        )
                    }
                    // Unrealized holdings: silently skip — no tax event has occurred.
                }
            }

            RebitMutualFundEquityExtraction(
                registrarOrDepository = rta,
                lots = lots,
                totalRealizedGainRupees = totalGains
            )
        } catch (_: Throwable) {
            RebitMutualFundEquityExtraction("Unknown", emptyList(), 0L)
        }
    }
}

