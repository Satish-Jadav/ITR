package in.zerotax.feature.aiextractor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import in.zerotax.core.model.EmployerSalarySource
import in.zerotax.core.model.SourceKind
import in.zerotax.core.security.UidaiAadhaarVerhoeffScrubber
import in.zerotax.core.taxengine.reconciliation.DeterministicReconciler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed interface Form16ExtractionOutcome {
    data class VerifiedSuccess(val salaryRecord: EmployerSalarySource) : Form16ExtractionOutcome
    data class RequiresOneTapVisualConfirmation(
        val candidateRecord: EmployerSalarySource,
        val computedExpectedNetSalary: Long,
        val croppedRegionBitmap: Bitmap
    ) : Form16ExtractionOutcome
    data class FallbackToDeterministicRegex(val reason: String) : Form16ExtractionOutcome
}

interface LocalGenerativeModelBridge {
    suspend fun generateContent(prompt: String): String?
}

class SystemAiCoreModelBridge(private val context: Context) : LocalGenerativeModelBridge {
    override suspend fun generateContent(prompt: String): String? {
        return try {
            val genConfigClass = Class.forName("com.google.ai.edge.aicore.GenerationConfigKt")
            // Reflection-based safe call to AICore if present on system image
            null
        } catch (_: Throwable) {
            null
        }
    }
}

class OnDeviceForm16AiExtractor(
    private val appContext: Context,
    private val modelBridge: LocalGenerativeModelBridge = SystemAiCoreModelBridge(appContext)
) {

    suspend fun extractAndValidateForm16(
        ocrText: String,
        renderedPageBitmap: Bitmap,
        netSalaryBoundingBox: Rect,
        systemPrompt: String = DEFAULT_FORM16_EXTRACTION_PROMPT
    ): Form16ExtractionOutcome = withContext(Dispatchers.Default) {
        try {
            val sanitizedOcr = UidaiAadhaarVerhoeffScrubber.scrubAllAadhaarDigits(ocrText)
            val fullPrompt = "$systemPrompt\n\nOCR_TEXT_INPUT:\n$sanitizedOcr"
            val rawOutput = modelBridge.generateContent(fullPrompt)
                ?: return@withContext Form16ExtractionOutcome.FallbackToDeterministicRegex("AICore unavailable or returned null")

            val rawJson = rawOutput.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val json = JSONObject(rawJson)

            val gross = json.optLong("gross_salary_sec17_1", 0L) +
                json.optLong("perquisites_sec17_2", 0L) +
                json.optLong("profits_in_lieu_sec17_3", 0L)
            val sec10 = json.optLong("exempt_hra_sec10_13a", 0L) + json.optLong("exempt_lta_sec10_5", 0L)
            val sec16 = json.optLong("standard_deduction_sec16_ia", 0L) + json.optLong("professional_tax_sec16_iii", 0L)
            val printedNet = json.optLong("net_taxable_salary_printed", 0L)
            val tan = json.optString("employer_or_issuer_tan", "UNKN00000A").uppercase()

            val candidate = EmployerSalarySource(
                employerTan = tan,
                employerName = "AICore Extracted ($tan)",
                source = SourceKind.FORM_16,
                grossSalary = gross,
                sec10Exemptions = sec10,
                sec16DeductionsClaimedInDoc = sec16,
                netTaxableSalaryInDoc = printedNet,
                tdsReported = json.optLong("tds_reported", 0L)
            )

            // Strict ±₹5 Math Sanity Firewall check
            if (DeterministicReconciler.passesForm16SanityCheck(candidate, toleranceRupees = 5L)) {
                Form16ExtractionOutcome.VerifiedSuccess(candidate)
            } else {
                val safeLeft = netSalaryBoundingBox.left.coerceIn(0, renderedPageBitmap.width - 1)
                val safeTop = netSalaryBoundingBox.top.coerceIn(0, renderedPageBitmap.height - 1)
                val safeWidth = netSalaryBoundingBox.width().coerceIn(1, renderedPageBitmap.width - safeLeft)
                val safeHeight = netSalaryBoundingBox.height().coerceIn(1, renderedPageBitmap.height - safeTop)

                val cropped = Bitmap.createBitmap(
                    renderedPageBitmap,
                    safeLeft,
                    safeTop,
                    safeWidth,
                    safeHeight
                )
                Form16ExtractionOutcome.RequiresOneTapVisualConfirmation(
                    candidateRecord = candidate,
                    computedExpectedNetSalary = (gross - sec10 - sec16).coerceAtLeast(0L),
                    croppedRegionBitmap = cropped
                )
            }
        } catch (e: Exception) {
            Form16ExtractionOutcome.FallbackToDeterministicRegex(e.message ?: "AICore extraction failed")
        }
    }

    companion object {
        const val DEFAULT_FORM16_EXTRACTION_PROMPT = """
You are an offline deterministic Form 16 Part B tax structure assistant.
Parse the OCR text and return ONLY valid JSON with keys:
- gross_salary_sec17_1 (number)
- perquisites_sec17_2 (number)
- profits_in_lieu_sec17_3 (number)
- exempt_hra_sec10_13a (number)
- exempt_lta_sec10_5 (number)
- standard_deduction_sec16_ia (number)
- professional_tax_sec16_iii (number)
- net_taxable_salary_printed (number)
- employer_or_issuer_tan (string)
- tds_reported (number)
"""
    }

    // TODO(Phase-2): Add OnDeviceBankNarrationClassifier batching 25 Account Aggregator bank rows (>= ₹500) to classify 80C/80D/80G/44ADA.
    // TODO(Phase-2): Add physical rent receipt (Rule 2A) & Section 80G donation receipt ARN/Donee PAN extractor (Prompt P1-PROMPT-02).
}
