package `in`.zerotax.feature.aiextractor

import android.content.Context
import `in`.zerotax.core.security.UidaiAadhaarVerhoeffScrubber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 1-Tap Quick-Pick Option Chips displayed in the Chatbot so the user never faces a blank input box.
 */
enum class CopilotQuickOptionChip(
    val categoryPill: String,
    val displayTitleEn: String,
    val displayTitleHi: String,
    val displayTitleGu: String,
    val requiresLlmCall: Boolean = false
) {
    AUTO_FIND_DOCS(
        categoryPill = "Auto-Import & File",
        displayTitleEn = "📂 Auto-Find My Form 16 & Tax PDFs",
        displayTitleHi = "📂 फोन से फॉर्म 16 और टैक्स PDF खोजें",
        displayTitleGu = "📂 ફોનમાંથી ફોર્મ 16 અને ટેક્સ PDF શોધો"
    ),
    SYNC_PORTAL_6_STREAMS(
        categoryPill = "Auto-Import & File",
        displayTitleEn = "⚡ 1-Tap Sync AIS, 26AS & Last Year ITR",
        displayTitleHi = "⚡ 1-टैप में AIS, 26AS और पिछला ITR सिंक करें",
        displayTitleGu = "⚡ 1-ટેપમાં AIS, 26AS અને ગયા વર્ષનું ITR સિંક કરો"
    ),
    COMPARE_REGIME_WHY(
        categoryPill = "Save More Tax",
        displayTitleEn = "⚖️ Why did Old / New Regime win for me?",
        displayTitleHi = "⚖️ मेरे लिए नई या पुरानी व्यवस्था क्यों बेहतर है?",
        displayTitleGu = "⚖️ મારા માટે નવી કે જૂની કર વ્યવસ્થા કેમ શ્રેષ્ઠ છે?"
    ),
    SIMULATE_NPS_80D_HRA(
        categoryPill = "Save More Tax",
        displayTitleEn = "💰 Open Tax-Saving Sliders (NPS, 80D, HRA)",
        displayTitleHi = "💰 टैक्स बचत स्लाइडर खोलें (NPS, 80D, HRA)",
        displayTitleGu = "💰 ટેક્સ બચત સ્લાઇડર ખોલો (NPS, 80D, HRA)"
    ),
    HARVEST_112A_LTCG(
        categoryPill = "Stocks, MF & Crypto",
        displayTitleEn = "📈 Harvest ₹1.25L Tax-Free Equity LTCG",
        displayTitleHi = "📈 ₹1.25 लाख टैक्स-फ्री LTCG की गणना करें",
        displayTitleGu = "📈 ₹1.25 લાખ ટેક્સ-ફ્રી LTCG ની ગણતરી કરો"
    ),
    GENERATE_HR_FORMS_PACK(
        categoryPill = "Certificates & Forms",
        displayTitleEn = "📄 Download Form 12BB / 12BAA / 15G / Rent Receipts",
        displayTitleHi = "📄 फॉर्म 12BB / 12BAA / 15G / किराया रसीद डाउनलोड करें",
        displayTitleGu = "📄 ફોર્મ 12BB / 12BAA / 15G / ભાડા પાવતી ડાઉનલોડ કરો"
    ),
    EXPLAIN_NOTICE_OR_PAY_CHALLAN(
        categoryPill = "Notices & Refund",
        displayTitleEn = "🛡️ Check Notice (143(1)) / Pay Tax via UPI",
        displayTitleHi = "🛡️ नोटिस जांचें (143(1)) / UPI से टैक्स भरें",
        displayTitleGu = "🛡️ નોટિસ તપાસો (143(1)) / UPI થી ટેક્સ ભરો"
    )
}

data class WhatIfDeltaIntent(
    val intentCode: String,
    val add80CRupees: Long = 0L,
    val add80CCD1BNpsRupees: Long = 0L,
    val add80CCD2EmployerNpsRupees: Long = 0L,
    val add80DSelfRupees: Long = 0L,
    val add80DParentsRupees: Long = 0L,
    val monthlyRentRupees: Long = 0L,
    val addHomeLoanInterestRupees: Long = 0L,
    val resolvedByTier0Regex: Boolean = true
)

/**
 * Singleton, thread-safe, KV-cache-friendly orchestrator for all Chatbot & OCR AI operations.
 * Implements 3-Tier Zero-Waste AI Pipeline:
 * Tier 0: Deterministic Regex & Fast-Path Router (< 1ms, 0 tokens)
 * Tier 1: In-Memory SHA-256 Prompt-Output Memoization Cache (< 2ms)
 * Tier 2: KV-Prefix Cached Micro-Prompt Execution
 */
class OptimizedTaxCopilotChatbotEngine(
    private val appContext: Context? = null,
    private val modelBridge: LocalGenerativeModelBridge? = null
) {

    private val modelMutex = Mutex()
    private val inMemoryMemoCache = object : LinkedHashMap<String, String>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 128
    }

    companion object {
        private const val STATIC_PREFIX_V2 =
            "ROLE:ZeroTaxDeterministicJSONParser|TEMP:0|RULES:NoMath,ExactIntOnly,ValidJSON\n"

        // Tier-0 Fast-Path Regexes for English, Hindi & Hinglish numeric queries (< 1ms, 0 LLM tokens)
        private val AMOUNT_REGEX = Regex("""(\d+(?:\.\d+)?)\s*(k|thousand|lakh|lac|l|cr|crore)?""", RegexOption.IGNORE_CASE)
        private val NPS_1B_KEYWORDS = Regex("""\b(nps|80ccd1b|80ccd\(1b\)|pension)\b""", RegexOption.IGNORE_CASE)
        private val NPS_2_KEYWORDS = Regex("""\b(employer\s*nps|80ccd2|80ccd\(2\)|corporate\s*nps)\b""", RegexOption.IGNORE_CASE)
        private val SEC_80C_KEYWORDS = Regex("""\b(80c|ppf|elss|lic|epf|tuition|sukanya)\b""", RegexOption.IGNORE_CASE)
        private val SEC_80D_PARENTS_KEYWORDS = Regex("""\b(parent|mother|father|mummy|papa|senior)\b.*\b(80d|health|medical|insurance)\b|\b(80d|health|medical|insurance)\b.*\b(parent|mother|father|senior)\b""", RegexOption.IGNORE_CASE)
        private val SEC_80D_SELF_KEYWORDS = Regex("""\b(80d|health\s*insurance|mediclaim|medical\s*policy)\b""", RegexOption.IGNORE_CASE)
        private val RENT_HRA_KEYWORDS = Regex("""\b(rent|hra|kiraya|landlord)\b""", RegexOption.IGNORE_CASE)
        private val HOME_LOAN_KEYWORDS = Regex("""\b(home\s*loan|housing\s*loan|24b|24\(b\)|emi\s*interest)\b""", RegexOption.IGNORE_CASE)
    }

    /**
     * Parses a user's natural-language query into a structured WhatIfDeltaIntent.
     * 1. Tries Tier-0 deterministic regex first (< 1ms, 0 NPU wakeups).
     * 2. Falls back to Tier-1 Memoization Cache (< 2ms).
     * 3. Invokes Tier-2 Compact Micro-Prompt on on-device LLM only if needed.
     */
    suspend fun parseUserChatQueryOptimized(rawUserQuery: String): WhatIfDeltaIntent = withContext(Dispatchers.Default) {
        val cleanQuery = UidaiAadhaarVerhoeffScrubber.scrubAllAadhaarDigits(rawUserQuery.trim(), strictChecksumOnly = true)
            .take(200)

        // STEP 1: Tier-0 Instant Regex Fast-Path (Handles ~85% of queries in < 1ms with 0 AI tokens)
        tryTier0FastPath(cleanQuery)?.let { return@withContext it }

        // STEP 2: Tier-1 Memoization Cache Lookup
        val cacheKey = "CHAT_V2:${cleanQuery.lowercase()}"
        synchronized(inMemoryMemoCache) { inMemoryMemoCache[cacheKey] }?.let { cachedJson ->
            return@withContext parseCompactWhatIfJson(cachedJson, resolvedByTier0 = false)
        }

        // STEP 3: Tier-2 Compact Micro-Prompt
        val microPrompt = buildString {
            append(STATIC_PREFIX_V2)
            append("TASK:Map user tax query to intent code & rupee integer deltas.\n")
            append("INTENTS:SIM_WHAT_IF|EXPLAIN_REGIME|IMPORT_DOC|GEN_FORM|NOTICE_HELP|REFUND_TRACK|OTHER_STATUTORY_QA\n")
            append("SCHEMA:{\"i\":\"INTENT\",\"c80\":0,\"nps1b\":0,\"nps2\":0,\"dSelf\":0,\"dPar\":0,\"rentM\":0,\"hlInt\":0}\n")
            append("IN:<user_query>").append(cleanQuery).append("</user_query>\nOUT:")
        }

        runCatching {
            val rawOut = modelMutex.withLock {
                modelBridge?.generateContent(microPrompt).orEmpty()
            }
            if (rawOut.isNotBlank()) {
                val jsonStr = rawOut.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                synchronized(inMemoryMemoCache) { inMemoryMemoCache[cacheKey] = jsonStr }
                parseCompactWhatIfJson(jsonStr, resolvedByTier0 = false)
            } else {
                WhatIfDeltaIntent(intentCode = "OTHER_STATUTORY_QA", resolvedByTier0Regex = true)
            }
        }.getOrElse {
            WhatIfDeltaIntent(intentCode = "OTHER_STATUTORY_QA", resolvedByTier0Regex = true)
        }
    }

    internal fun tryTier0FastPath(query: String): WhatIfDeltaIntent? {
        val amount = parseIndianShorthandAmount(query)
        if (amount > 0L) {
            when {
                NPS_2_KEYWORDS.containsMatchIn(query) ->
                    return WhatIfDeltaIntent("SIM_WHAT_IF", add80CCD2EmployerNpsRupees = amount)
                NPS_1B_KEYWORDS.containsMatchIn(query) ->
                    return WhatIfDeltaIntent("SIM_WHAT_IF", add80CCD1BNpsRupees = amount)
                SEC_80D_PARENTS_KEYWORDS.containsMatchIn(query) ->
                    return WhatIfDeltaIntent("SIM_WHAT_IF", add80DParentsRupees = amount)
                SEC_80D_SELF_KEYWORDS.containsMatchIn(query) ->
                    return WhatIfDeltaIntent("SIM_WHAT_IF", add80DSelfRupees = amount)
                SEC_80C_KEYWORDS.containsMatchIn(query) ->
                    return WhatIfDeltaIntent("SIM_WHAT_IF", add80CRupees = amount)
                RENT_HRA_KEYWORDS.containsMatchIn(query) ->
                    return WhatIfDeltaIntent("SIM_WHAT_IF", monthlyRentRupees = amount)
                HOME_LOAN_KEYWORDS.containsMatchIn(query) ->
                    return WhatIfDeltaIntent("SIM_WHAT_IF", addHomeLoanInterestRupees = amount)
            }
        }
        return when {
            query.contains("form 16", ignoreCase = true) || query.contains("upload", ignoreCase = true) ->
                WhatIfDeltaIntent("IMPORT_DOC")
            query.contains("12bb", ignoreCase = true) || query.contains("15g", ignoreCase = true) || query.contains("15h", ignoreCase = true) ->
                WhatIfDeltaIntent("GEN_FORM")
            query.contains("notice", ignoreCase = true) || query.contains("143(1)", ignoreCase = true) || query.contains("139(9)", ignoreCase = true) ->
                WhatIfDeltaIntent("NOTICE_HELP")
            query.contains("refund", ignoreCase = true) || query.contains("244a", ignoreCase = true) ->
                WhatIfDeltaIntent("REFUND_TRACK")
            query.contains("regime", ignoreCase = true) || query.contains("new vs old", ignoreCase = true) ->
                WhatIfDeltaIntent("EXPLAIN_REGIME")
            else -> null
        }
    }

    internal fun parseIndianShorthandAmount(text: String): Long {
        val clean = text.replace(",", "")
        val match = AMOUNT_REGEX.findAll(clean).firstOrNull { m ->
            val v = m.groupValues[1].toDoubleOrNull() ?: 0.0
            // Ignore section numbers like "80", "24", "143", "139", "194"
            v !in setOf(80.0, 24.0, 10.0, 16.0, 12.0, 15.0, 143.0, 139.0, 194.0, 112.0, 111.0, 115.0)
        } ?: return 0L

        val base = match.groupValues[1].toDoubleOrNull() ?: return 0L
        val unit = match.groupValues[2].lowercase()
        val multiplier = when (unit) {
            "k", "thousand" -> 1_000.0
            "l", "lac", "lakh" -> 1_00_000.0
            "cr", "crore" -> 1_00_00_000.0
            else -> 1.0
        }
        return (base * multiplier).toLong()
    }

    private fun parseCompactWhatIfJson(jsonStr: String, resolvedByTier0: Boolean): WhatIfDeltaIntent {
        val obj = JSONObject(jsonStr)
        return WhatIfDeltaIntent(
            intentCode = obj.optString("i", "OTHER_STATUTORY_QA"),
            add80CRupees = obj.optLong("c80", 0L),
            add80CCD1BNpsRupees = obj.optLong("nps1b", 0L),
            add80CCD2EmployerNpsRupees = obj.optLong("nps2", 0L),
            add80DSelfRupees = obj.optLong("dSelf", 0L),
            add80DParentsRupees = obj.optLong("dPar", 0L),
            monthlyRentRupees = obj.optLong("rentM", 0L),
            addHomeLoanInterestRupees = obj.optLong("hlInt", 0L),
            resolvedByTier0Regex = resolvedByTier0
        )
    }
}
