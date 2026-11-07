package `in`.zerotax.feature.pdfunlocker

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class TaxComputationPdfData(
    val pan: String,
    val taxpayerName: String,
    val assessmentYear: String = "2026-27",
    val selectedRegime: String = "NEW_REGIME_115BAC",
    val grossSalary: Long,
    val standardDeduction: Long,
    val netSalary: Long,
    val otherIncome: Long,
    val grossTotalIncome: Long,
    val chapterViaDeductions: Long,
    val totalTaxableIncome: Long,
    val normalTax: Long,
    val rebate87A: Long,
    val surcharge: Long,
    val cess: Long,
    val totalTaxLiability: Long,
    val tdsAndTcsPaid: Long,
    val advanceTaxPaid: Long,
    val netPayableOrRefund: Long,
    val isRefund: Boolean,
    val verificationAuditSha256: String
)

object LocalTaxComputationPdfExporter {

    suspend fun generateComputationPdf(data: TaxComputationPdfData): ByteArray = withContext(Dispatchers.Default) {
        val document = PDDocument()
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)

        PDPageContentStream(document, page).use { cs ->
            var yPosition = 800f
            val leftMargin = 50f

            fun sanitizePdfText(raw: String): String {
                return raw.map { ch ->
                    if (ch.code in 32..126) ch
                    else if (ch == '\n' || ch == '\r') ' '
                    else '?'
                }.joinToString("").trim()
            }

            fun writeLine(text: String, isHeader: Boolean = false, fontSize: Float = 11f) {
                val safeText = sanitizePdfText(text)
                cs.beginText()
                if (isHeader) {
                    cs.setFont(PDType1Font.HELVETICA_BOLD, fontSize)
                } else {
                    cs.setFont(PDType1Font.HELVETICA, fontSize)
                }
                cs.newLineAtOffset(leftMargin, yPosition)
                cs.showText(safeText)
                cs.endText()
                yPosition -= (fontSize + 7f)
            }

            writeLine("ZEROTAX — STATEMENT OF TAX COMPUTATION", isHeader = true, fontSize = 16f)
            writeLine("Assessment Year: ${data.assessmentYear} | Regime: ${data.selectedRegime}", isHeader = false, fontSize = 11f)
            writeLine("Taxpayer: ${data.taxpayerName} | PAN: ${data.pan}", isHeader = false, fontSize = 11f)
            writeLine("------------------------------------------------------------------------------------------------", isHeader = false, fontSize = 10f)

            writeLine("1. Gross Salary / Income from Salary: Rs. ${data.grossSalary}")
            writeLine("   Less: Standard Deduction u/s 16(ia): Rs. ${data.standardDeduction}")
            writeLine("   Net Income chargeable under head Salaries: Rs. ${data.netSalary}", isHeader = true)

            writeLine("2. Income from Other Sources (Interest / Dividend): Rs. ${data.otherIncome}")
            writeLine("3. Gross Total Income (GTI): Rs. ${data.grossTotalIncome}", isHeader = true)

            writeLine("4. Total Deductions under Chapter VI-A: Rs. ${data.chapterViaDeductions}")
            writeLine("5. Total Taxable Income (Rounded u/s 288A): Rs. ${data.totalTaxableIncome}", isHeader = true)
            writeLine("------------------------------------------------------------------------------------------------", isHeader = false, fontSize = 10f)

            writeLine("TAX COMPUTATION & RELIEF:", isHeader = true, fontSize = 12f)
            writeLine("• Base Normal Slab Tax: Rs. ${data.normalTax}")
            writeLine("• Less: Rebate u/s 87A: Rs. ${data.rebate87A}")
            writeLine("• Surcharge: Rs. ${data.surcharge}")
            writeLine("• Health and Education Cess (4%): Rs. ${data.cess}")
            writeLine("• Total Tax Liability (Rounded u/s 288B): Rs. ${data.totalTaxLiability}", isHeader = true)

            writeLine("------------------------------------------------------------------------------------------------", isHeader = false, fontSize = 10f)
            writeLine("TAXES PAID & NET LIABILITY / REFUND:", isHeader = true, fontSize = 12f)
            writeLine("• Total TDS / TCS Credits (Form 26AS / AIS): Rs. ${data.tdsAndTcsPaid}")
            writeLine("• Advance Tax / Self-Assessment Paid: Rs. ${data.advanceTaxPaid}")

            val statusText = if (data.isRefund) "NET REFUND DUE TO ASSESSEE" else "NET TAX PAYABLE TO GOVT"
            writeLine("$statusText: Rs. ${data.netPayableOrRefund}", isHeader = true, fontSize = 13f)

            yPosition -= 20f
            writeLine("PROVENANCE & INTEGRITY ASSURANCE:", isHeader = true, fontSize = 10f)
            writeLine("Computed locally by ZeroTax deterministic engine with End-to-End Encrypted Cloud Vault security.", fontSize = 9f)
            writeLine("Cryptographic Audit Signature: ${data.verificationAuditSha256}", fontSize = 8f)
        }

        val out = ByteArrayOutputStream()
        document.save(out)
        document.close()
        out.toByteArray()
    }
}

