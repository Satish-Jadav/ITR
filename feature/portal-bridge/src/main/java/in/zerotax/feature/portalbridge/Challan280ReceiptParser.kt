package `in`.zerotax.feature.portalbridge

import android.content.Context
import android.content.Intent
import android.net.Uri
import `in`.zerotax.core.model.ScheduleItChallanEntry

object Challan280ReceiptParser {
    private val BSR_REGEX = Regex("""BSR\s*Code\s*[:\-]?\s*(\d{7})""", RegexOption.IGNORE_CASE)
    private val SERIAL_REGEX = Regex("""Challan\s*(?:No|Serial\s*Number)\s*[:\-]?\s*(\d{5})""", RegexOption.IGNORE_CASE)
    private val DATE_REGEX = Regex("""Date\s*of\s*Deposit\s*[:\-]?\s*(\d{2})[/\-](\d{2})[/\-](\d{4})""", RegexOption.IGNORE_CASE)
    private val AMT_REGEX = Regex("""Total\s*Amount\s*[:\-]?\s*(?:Rs\.?|₹)?\s*([\d,]+)""", RegexOption.IGNORE_CASE)

    fun parseChallan280PdfText(pdfText: String): Result<ScheduleItChallanEntry> {
        val bsr = BSR_REGEX.find(pdfText)?.groupValues?.get(1)
            ?: return Result.failure(IllegalArgumentException("7-digit BSR Code not found"))
        val serial = SERIAL_REGEX.find(pdfText)?.groupValues?.get(1)
            ?: return Result.failure(IllegalArgumentException("5-digit Challan Serial No not found"))
        val d = DATE_REGEX.find(pdfText)
            ?: return Result.failure(IllegalArgumentException("Date of Deposit not found"))
        val amt = AMT_REGEX.find(pdfText)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 0L

        return Result.success(ScheduleItChallanEntry(bsr, "${d.groupValues[3]}-${d.groupValues[2]}-${d.groupValues[1]}", serial, amt))
    }
}
