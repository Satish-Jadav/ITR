package `in`.zerotax.feature.aiextractor

import `in`.zerotax.core.model.DonationCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class OnDeviceReceiptAndClassifierTest {

    private val mockBridge = object : LocalGenerativeModelBridge {
        override suspend fun generateContent(prompt: String): String? {
            return when {
                prompt.contains("Rent Receipts") -> """
                    {
                        "landlord_pan": "ABCDE1234F",
                        "annual_rent_rupees": 240000,
                        "landlord_name": "Ramesh Sharma",
                        "property_address": "Bengaluru",
                        "is_digital_payment": true
                    }
                """.trimIndent()
                prompt.contains("Section 80G donation") -> """
                    {
                        "donee_name": "PM CARES FUND",
                        "donee_pan": "AAATC1234A",
                        "form_10be_arn": "ARN12345678",
                        "amount_rupees": 50000,
                        "is_cash": false,
                        "category": "HUNDRED_PERCENT_NO_LIMIT"
                    }
                """.trimIndent()
                prompt.contains("Classify bank narrations") -> """
                    [
                        { "txnId": "TXN-1", "category": "SEC_80C_INVESTMENT", "confidence": 0.95, "rationale": "PPF" },
                        { "txnId": "TXN-2", "category": "SEC_80D_HEALTH_INSURANCE", "confidence": 0.90, "rationale": "Health" },
                        { "txnId": "TXN-3", "category": "SAVINGS_ACCOUNT_INTEREST", "confidence": 0.98, "rationale": "Interest" }
                    ]
                """.trimIndent()
                else -> null
            }
        }
    }

    @Test
    fun testRentReceiptExtractor_ParsesPanAndRent() = runBlocking {
        val extractor = OnDeviceReceiptAnd80GExtractor(mockBridge)
        val result = extractor.extractRentReceipt("Rent paid to Ramesh Sharma Rs 2,40,000 PAN ABCDE1234F")

        assertEquals("ABCDE1234F", result.landlordPan)
        assertEquals(240000L, result.annualRentRupees)
        assertTrue(result.isDigitalPayment)
    }

    @Test
    fun testDonation80GExtractor_ParsesArnAndCategory() = runBlocking {
        val extractor = OnDeviceReceiptAnd80GExtractor(mockBridge)
        val result = extractor.extractDonation80G("Donation to PM CARES Rs 50,000 ARN12345678")

        assertEquals("AAATC1234A", result.doneePan)
        assertEquals(50000L, result.donationAmountRupees)
        assertEquals(DonationCategory.HUNDRED_PERCENT_NO_LIMIT, result.category)
        assertEquals(50000L, result.eligibleDeductionRupees)
    }

    @Test
    fun testBankNarrationClassifier_BatchClassifiesTransactions() = runBlocking {
        val classifier = OnDeviceBankNarrationClassifier(mockBridge)
        val txns = listOf(
            BankTransactionItem("TXN-1", "2025-05-01", "PPF DEPOSIT 150000", 150000L, false),
            BankTransactionItem("TXN-2", "2025-06-01", "STAR HEALTH INSURANCE 25000", 25000L, false),
            BankTransactionItem("TXN-3", "2025-06-30", "SB INTEREST CREDIT", 1200L, true)
        )

        val results = classifier.classifyTransactionsBatch(txns)
        assertEquals(3, results.size)
        assertEquals(TaxDeductionCategory.SEC_80C_INVESTMENT, results[0].detectedCategory)
        assertEquals(TaxDeductionCategory.SEC_80D_HEALTH_INSURANCE, results[1].detectedCategory)
        assertEquals(TaxDeductionCategory.SAVINGS_ACCOUNT_INTEREST, results[2].detectedCategory)
    }
}
