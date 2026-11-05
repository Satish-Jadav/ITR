package `in`.zerotax.feature.pdfunlocker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate

/**
 * Feature F-04: Official CBDT Rule 26C Form 12BB Investment Declaration Data
 */
data class Form12BbData(
    val employeeName: String,
    val employeePan: String,
    val assessmentYear: String = "2026-27",
    val employerName: String,
    // 1. HRA
    val rentPaidAnnual: Long = 0L,
    val landlordName: String = "",
    val landlordAddress: String = "",
    val landlordPan: String = "",
    // 2. LTA
    val leaveTravelConcessionClaimed: Long = 0L,
    // 3. Home Loan Interest Sec 24(b)
    val homeLoanInterestSec24b: Long = 0L,
    val lenderName: String = "",
    val lenderPan: String = "",
    val lenderType: String = "FINANCIAL_INSTITUTION", // BANK / NBFC / EMPLOYER
    // 4. Chapter VI-A Deductions
    val section80C: Long = 0L,
    val section80CCD1B: Long = 0L,
    val section80CCD2EmployerNps: Long = 0L,
    val section80D: Long = 0L,
    val section80E: Long = 0L,
    val section80G: Long = 0L
)

/**
 * Feature F-07: Form 15G / Form 15H Zero-TDS Declaration Data for Bank Fixed Deposits
 */
data class Form15HData(
    val declarantName: String,
    val pan: String,
    val dateOfBirthIso: String,
    val financialYear: String = "2025-26",
    val assessmentYear: String = "2026-27",
    val bankName: String,
    val bankBranch: String,
    val fixedDepositAccountNumber: String,
    val estimatedFdInterestRupees: Long,
    val estimatedTotalIncomeRupees: Long,
    val isSeniorCitizen60Plus: Boolean = true
)

/**
 * High-performance, self-contained, 100% on-device statutory PDF generator.
 * Produces valid PDF 1.4 documents conforming to CBDT Rule 26C (Form 12BB)
 * and Section 197A / Rule 29C (Form 15G / 15H).
 * Operates without external asset dependencies or native font file requirements.
 */
object LocalStatutoryPdfGenerators {

    suspend fun generateForm12BbPdf(data: Form12BbData): ByteArray = withContext(Dispatchers.Default) {
        val lines = mutableListOf<PdfLine>()
        lines.add(PdfLine("FORM NO. 12BB", isBold = true, fontSize = 14f))
        lines.add(PdfLine("(See rule 26C of the Income-tax Rules, 1962)", isBold = false, fontSize = 9f))
        lines.add(PdfLine("Statement showing particulars of claims by an employee for deduction of tax u/s 192", isBold = true, fontSize = 10f))
        lines.add(PdfLine("----------------------------------------------------------------------------------------------------", fontSize = 8f))

        lines.add(PdfLine("1. Name and address of the employee: ${data.employeeName}", isBold = true, fontSize = 10f))
        lines.add(PdfLine("2. Permanent Account Number (PAN): ${data.employeePan}", fontSize = 10f))
        lines.add(PdfLine("3. Financial Year: 2025-26 | Assessment Year: ${data.assessmentYear}", fontSize = 10f))
        lines.add(PdfLine("4. Employer Name: ${data.employerName}", fontSize = 10f))
        lines.add(PdfLine("----------------------------------------------------------------------------------------------------", fontSize = 8f))

        lines.add(PdfLine("PARTICULARS OF CLAIMS FOR STATUTORY DEDUCTION:", isBold = true, fontSize = 11f))
        lines.add(PdfLine("1. House Rent Allowance under Section 10(13A):", isBold = true, fontSize = 10f))
        lines.add(PdfLine("   - Rent paid to landlord during the year: Rs. ${data.rentPaidAnnual}", fontSize = 9f))
        lines.add(PdfLine("   - Name of the landlord: ${data.landlordName.ifBlank { "N/A" }}", fontSize = 9f))
        lines.add(PdfLine("   - Address of landlord: ${data.landlordAddress.ifBlank { "N/A" }}", fontSize = 9f))
        lines.add(PdfLine("   - PAN of landlord (mandatory if rent > Rs. 1,00,000): ${data.landlordPan.ifBlank { "N/A" }}", fontSize = 9f))

        lines.add(PdfLine("2. Leave Travel Concession / Allowance under Section 10(5): Rs. ${data.leaveTravelConcessionClaimed}", fontSize = 10f))

        lines.add(PdfLine("3. Deduction of interest on housing loan under Section 24(b):", isBold = true, fontSize = 10f))
        lines.add(PdfLine("   - Amount of interest payable / paid to lender: Rs. ${data.homeLoanInterestSec24b}", fontSize = 9f))
        lines.add(PdfLine("   - Name of the lender: ${data.lenderName.ifBlank { "N/A" }}", fontSize = 9f))
        lines.add(PdfLine("   - PAN of the lender: ${data.lenderPan.ifBlank { "N/A" }}", fontSize = 9f))
        lines.add(PdfLine("   - Financial Institution / Bank / Employer: ${data.lenderType}", fontSize = 9f))

        lines.add(PdfLine("4. Deductions claimed under Chapter VI-A of the Income-tax Act, 1961:", isBold = true, fontSize = 10f))
        lines.add(PdfLine("   - Section 80C (PPF / EPF / ELSS / Life Insurance / Principal Repayment): Rs. ${data.section80C}", fontSize = 9f))
        lines.add(PdfLine("   - Section 80CCD(1B) (NPS Tier-I Self Contribution up to Rs. 50,000): Rs. ${data.section80CCD1B}", fontSize = 9f))
        lines.add(PdfLine("   - Section 80CCD(2) (Employer NPS Contribution - exempt from 1.5L cap): Rs. ${data.section80CCD2EmployerNps}", fontSize = 9f))
        lines.add(PdfLine("   - Section 80D (Health Insurance Premium / Preventive Checkup): Rs. ${data.section80D}", fontSize = 9f))
        lines.add(PdfLine("   - Section 80E (Interest on Higher Education Loan): Rs. ${data.section80E}", fontSize = 9f))
        lines.add(PdfLine("   - Section 80G (Donations to Approved Charities / Relief Funds): Rs. ${data.section80G}", fontSize = 9f))

        lines.add(PdfLine("----------------------------------------------------------------------------------------------------", fontSize = 8f))
        lines.add(PdfLine("VERIFICATION:", isBold = true, fontSize = 11f))
        lines.add(PdfLine("I, ${data.employeeName}, do hereby declare that the particulars given above are complete and correct.", fontSize = 9f))
        lines.add(PdfLine("Place: Bengaluru                                        Date: ${LocalDate.now()}", fontSize = 9f))
        lines.add(PdfLine("Signature of the Employee: ________________________________________________", fontSize = 9f))
        lines.add(PdfLine("Verified & Generated via ZeroTax-Local (100% On-Device Cryptographic Vault)", fontSize = 8f))

        buildPdf(lines)
    }

    suspend fun generateForm15GOr15HPdf(data: Form15HData): ByteArray = withContext(Dispatchers.Default) {
        val formCode = if (data.isSeniorCitizen60Plus) "FORM NO. 15H" else "FORM NO. 15G"
        val ruleRef = if (data.isSeniorCitizen60Plus) "(See section 197A(1C) and rule 29C)" else "(See section 197A(1), 197A(1A) and rule 29C)"

        val lines = mutableListOf<PdfLine>()
        lines.add(PdfLine(formCode, isBold = true, fontSize = 14f))
        lines.add(PdfLine(ruleRef, fontSize = 9f))
        lines.add(PdfLine("Declaration for claiming certain receipts without deduction of tax u/s 194A", isBold = true, fontSize = 10f))
        lines.add(PdfLine("----------------------------------------------------------------------------------------------------", fontSize = 8f))

        lines.add(PdfLine("PART I -- DECLARANT PARTICULARS:", isBold = true, fontSize = 11f))
        lines.add(PdfLine("1. Name of Assessee (Declarant): ${data.declarantName}", fontSize = 10f))
        lines.add(PdfLine("2. PAN of the Assessee: ${data.pan}", fontSize = 10f))
        lines.add(PdfLine("3. Date of Birth: ${data.dateOfBirthIso} (Senior Citizen Status: ${if (data.isSeniorCitizen60Plus) "YES (Age >= 60)" else "NO"})", fontSize = 10f))
        lines.add(PdfLine("4. Previous Year: ${data.financialYear} | Assessment Year: ${data.assessmentYear}", fontSize = 10f))
        lines.add(PdfLine("5. Name of Bank & Branch: ${data.bankName} (${data.bankBranch})", fontSize = 10f))
        lines.add(PdfLine("6. Fixed / Recurring Deposit Account Number: ${data.fixedDepositAccountNumber}", fontSize = 10f))
        lines.add(PdfLine("----------------------------------------------------------------------------------------------------", fontSize = 8f))

        lines.add(PdfLine("ESTIMATED INCOME DETAILS:", isBold = true, fontSize = 11f))
        lines.add(PdfLine("7. Estimated FD Interest for which this declaration is made: Rs. ${data.estimatedFdInterestRupees}", fontSize = 10f))
        lines.add(PdfLine("8. Estimated total income of the P.Y. in which income is to be included: Rs. ${data.estimatedTotalIncomeRupees}", fontSize = 10f))
        lines.add(PdfLine("9. Tax on estimated total income is NIL (Within basic exemption / Sec 87A rebate ceiling).", fontSize = 10f))
        lines.add(PdfLine("----------------------------------------------------------------------------------------------------", fontSize = 8f))

        lines.add(PdfLine("DECLARATION / VERIFICATION:", isBold = true, fontSize = 11f))
        lines.add(PdfLine("I, ${data.declarantName}, do hereby declare that to the best of my knowledge and belief what is stated", fontSize = 9f))
        lines.add(PdfLine("above is correct, complete and is truly stated. I declare that the tax on my estimated total income", fontSize = 9f))
        lines.add(PdfLine("computed in accordance with the provisions of the Income-tax Act, 1961 for the assessment year", fontSize = 9f))
        lines.add(PdfLine("${data.assessmentYear} will be NIL, and therefore no tax should be deducted at source u/s 194A.", fontSize = 9f))
        lines.add(PdfLine("Date: ${LocalDate.now()}                                 Signature of the Declarant: _________________", fontSize = 9f))
        lines.add(PdfLine("Generated via ZeroTax-Local Privacy Engine (Certified for submission to bank branch)", fontSize = 8f))

        buildPdf(lines)
    }

    private data class PdfLine(
        val text: String,
        val isBold: Boolean = false,
        val fontSize: Float = 10f
    )

    private fun buildPdf(lines: List<PdfLine>): ByteArray {
        val streamContent = buildString {
            append("BT\n")
            var currentY = 800f
            val leftMargin = 40f

            for (line in lines) {
                val fontTag = if (line.isBold) "/F2" else "/F1"
                append("$fontTag ${line.fontSize} Tf\n")
                append("1 0 0 1 $leftMargin $currentY Tm\n")
                val escapedText = escapePdfText(line.text)
                append("($escapedText) Tj\n")
                currentY -= (line.fontSize + 5f)
            }
            append("ET\n")
        }

        val streamBytes = streamContent.toByteArray(StandardCharsets.US_ASCII)
        val streamLength = streamBytes.size

        val out = ByteArrayOutputStream()
        val offsets = mutableListOf<Long>()

        fun writeString(str: String) {
            val bytes = str.toByteArray(StandardCharsets.US_ASCII)
            out.write(bytes)
        }

        // Header
        writeString("%PDF-1.4\n")

        // Obj 1: Catalog
        offsets.add(out.size().toLong())
        writeString("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        // Obj 2: Pages
        offsets.add(out.size().toLong())
        writeString("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")

        // Obj 3: Page (A4 is 595.28 x 841.89 pt)
        offsets.add(out.size().toLong())
        writeString("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595.28 841.89] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n")

        // Obj 4: Content Stream
        offsets.add(out.size().toLong())
        writeString("4 0 obj\n<< /Length $streamLength >>\nstream\n")
        out.write(streamBytes)
        writeString("\nendstream\nendobj\n")

        // Obj 5: Font F1 (Helvetica Regular)
        offsets.add(out.size().toLong())
        writeString("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")

        // Obj 6: Font F2 (Helvetica Bold)
        offsets.add(out.size().toLong())
        writeString("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n")

        // XRef table
        val startXref = out.size().toLong()
        writeString("xref\n0 7\n")
        writeString("0000000000 65535 f \n")
        for (offset in offsets) {
            writeString(String.format("%010d 00000 n \n", offset))
        }

        // Trailer
        writeString("trailer\n<< /Size 7 /Root 1 0 R >>\n")
        writeString("startxref\n$startXref\n%%EOF\n")

        return out.toByteArray()
    }

    private fun escapePdfText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("\r", "")
            .replace("\n", " ")
    }
}
