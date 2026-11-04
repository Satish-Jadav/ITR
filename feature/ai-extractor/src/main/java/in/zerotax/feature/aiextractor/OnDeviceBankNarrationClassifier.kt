package `in`.zerotax.feature.aiextractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class TaxDeductionCategory {
    SEC_80C_INVESTMENT,
    SEC_80D_HEALTH_INSURANCE,
    SEC_80D_PREVENTIVE_HEALTH,
    SEC_80G_DONATION,
    SEC_44ADA_FREELANCE_INCOME,
    SEC_44AD_BUSINESS_TURNOVER,
    SAVINGS_ACCOUNT_INTEREST,
    UNCLASSIFIED
}

data class BankTransactionItem(
    val txnId: String,
    val dateIso: String,
    val narration: String,
    val amountRupees: Long,
    val isCredit: Boolean
)

data class ClassifiedTaxTransaction(
    val originalTransaction: BankTransactionItem,
    val detectedCategory: TaxDeductionCategory,
    val confidenceScore: Double,
    val rationale: String
)

class OnDeviceBankNarrationClassifier(
    private val modelBridge: LocalGenerativeModelBridge
) {
    suspend fun classifyTransactionsBatch(
        transactions: List<BankTransactionItem>
    ): List<ClassifiedTaxTransaction> = withContext(Dispatchers.Default) {
        // Filter transactions >= ₹500 or interest credits
        val eligible = transactions.filter { it.amountRupees >= 500L || it.isCredit }
        if (eligible.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<ClassifiedTaxTransaction>()
        // Process in batches of 25 to respect local NPU window
        eligible.chunked(25).forEach { chunk ->
            val promptBuilder = StringBuilder()
            promptBuilder.append("Classify bank narrations for Indian Income Tax deductions/inflows. Valid categories:\n")
            promptBuilder.append("SEC_80C_INVESTMENT, SEC_80D_HEALTH_INSURANCE, SEC_80D_PREVENTIVE_HEALTH, SEC_80G_DONATION, SEC_44ADA_FREELANCE_INCOME, SEC_44AD_BUSINESS_TURNOVER, SAVINGS_ACCOUNT_INTEREST, UNCLASSIFIED\n\n")
            promptBuilder.append("Return a JSON array of objects with keys: txnId, category, confidence, rationale.\n\nTransactions:\n")

            chunk.forEach { txn ->
                promptBuilder.append("ID: ${txn.txnId} | ${if (txn.isCredit) "CR" else "DR"} Rs.${txn.amountRupees} | ${txn.narration}\n")
            }

            val rawOutput = modelBridge.generateContent(promptBuilder.toString())
            val classifiedMap = mutableMapOf<String, Pair<TaxDeductionCategory, Double>>()

            if (!rawOutput.isNullOrBlank()) {
                runCatching {
                    val cleanJson = rawOutput.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val array = JSONArray(cleanJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = obj.getString("txnId")
                        val catStr = obj.optString("category", "UNCLASSIFIED")
                        val cat = runCatching { TaxDeductionCategory.valueOf(catStr) }.getOrDefault(TaxDeductionCategory.UNCLASSIFIED)
                        val conf = obj.optDouble("confidence", 0.9)
                        classifiedMap[id] = cat to conf
                    }
                }
            }

            chunk.forEach { txn ->
                val (cat, conf) = classifiedMap[txn.txnId] ?: heuristicFallback(txn)
                results.add(
                    ClassifiedTaxTransaction(
                        originalTransaction = txn,
                        detectedCategory = cat,
                        confidenceScore = conf,
                        rationale = "Categorized via on-device AI/rule matcher"
                    )
                )
            }
        }

        results
    }

    private fun heuristicFallback(txn: BankTransactionItem): Pair<TaxDeductionCategory, Double> {
        val nar = txn.narration.uppercase()
        return when {
            nar.contains("LIC") || nar.contains("PPF") || nar.contains("ELSS") || nar.contains("TUITION") || nar.contains("SUKANYA") ->
                TaxDeductionCategory.SEC_80C_INVESTMENT to 0.85
            nar.contains("STAR HEALTH") || nar.contains("CARE HEALTH") || nar.contains("HDFC ERGO") || nar.contains("NIVA BUPA") ->
                TaxDeductionCategory.SEC_80D_HEALTH_INSURANCE to 0.90
            nar.contains("DIAGNOSTICS") || nar.contains("PATHOLOGY") || nar.contains("HEALTH CHECK") ->
                TaxDeductionCategory.SEC_80D_PREVENTIVE_HEALTH to 0.80
            nar.contains("PM CARES") || nar.contains("CRY") || nar.contains("DONATION") || nar.contains("RELIEF FUND") ->
                TaxDeductionCategory.SEC_80G_DONATION to 0.90
            txn.isCredit && (nar.contains("INT") || nar.contains("INTEREST") || nar.contains("SB INT")) ->
                TaxDeductionCategory.SAVINGS_ACCOUNT_INTEREST to 0.95
            txn.isCredit && (nar.contains("UPWORK") || nar.contains("FIVERR") || nar.contains("FREELANCE") || nar.contains("CONSULT")) ->
                TaxDeductionCategory.SEC_44ADA_FREELANCE_INCOME to 0.85
            else -> TaxDeductionCategory.UNCLASSIFIED to 0.50
        }
    }
}

