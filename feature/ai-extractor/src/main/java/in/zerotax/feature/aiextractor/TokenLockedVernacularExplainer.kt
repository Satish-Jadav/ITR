package `in`.zerotax.feature.aiextractor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SupportedVernacularLocale(val code: String, val displayName: String) {
    ENGLISH_IN("en-IN", "English (India)"),
    HINDI_IN("hi-IN", "हिन्दी (Hindi)"),
    GUJARATI_IN("gu-IN", "ગુજરાતી (Gujarati)"),
    MARATHI_IN("mr-IN", "मराठी (Marathi)"),
    TAMIL_IN("ta-IN", "தமிழ் (Tamil)"),
    TELUGU_IN("te-IN", "తెలుగు (Telugu)")
}

class TokenLockedVernacularExplainer(
    private val appContext: Context,
    private val modelBridge: LocalGenerativeModelBridge = SystemAiCoreModelBridge(appContext)
) {
    private val tokenRegex = Regex("""\{\{([A-Z0-9_]+)\}\}""")

    suspend fun translateWithTokenLock(
        templateText: String,
        replacements: Map<String, String>,
        targetLocale: SupportedVernacularLocale
    ): Result<String> = withContext(Dispatchers.Default) {
        if (targetLocale == SupportedVernacularLocale.ENGLISH_IN) {
            return@withContext Result.success(restoreTokens(templateText, replacements))
        }

        val prompt = """
Translate the following tax summary into locale ${targetLocale.code}.
CRITICAL INVARIANT: You MUST retain every token enclosed in {{...}} EXACTLY as written without changing, translating, or omitting it.
Source Text:
$templateText
""".trimIndent()

        runCatching {
            val out = modelBridge.generateContent(prompt)?.trim().orEmpty()
            if (out.isBlank()) {
                // Graceful fallback to English with tokens restored
                return@runCatching restoreTokens(templateText, replacements)
            }
            val expectedTokens = tokenRegex.findAll(templateText).map { it.value }.toSet()
            val presentTokens = tokenRegex.findAll(out).map { it.value }.toSet()
            val missingTokens = expectedTokens - presentTokens

            check(missingTokens.isEmpty()) { "Security invariant violated: Dropped tokens: $missingTokens" }
            restoreTokens(out, replacements)
        }.recover {
            // Guarantee user sees accurate numbers even if local LLM fails
            restoreTokens(templateText, replacements)
        }
    }

    private fun restoreTokens(text: String, replacements: Map<String, String>): String =
        replacements.entries.fold(text) { acc, (key, value) ->
            acc.replace("{{$key}}", value)
        }

    // TODO(Phase-3): Expand SupportedVernacularLocale to Marathi (mr-IN), Tamil (ta-IN), and Telugu (te-IN).
}
