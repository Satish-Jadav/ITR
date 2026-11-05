package `in`.zerotax.feature.aaaggregator

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RebitFiDataInMemoryParserTest {

    @Test
    fun testParseDepositJson_SweepsInterestAndDeductions() {
        val sampleJson = """
            {
                "bank_name": "HDFC Bank",
                "masked_account": "XXXX-XXXX-1234",
                "transactions": [
                    { "narration": "SB INT RECEIVED", "amount": 1500.0, "type": "CREDIT" },
                    { "narration": "TERM DEPOSIT INT", "amount": 4200.0, "type": "CREDIT" },
                    { "narration": "LIC PREMIUM PAYMENT", "amount": 25000.0, "type": "DEBIT" }
                ]
            }
        """.trimIndent()

        val result = RebitFiDataInMemoryParser.parseDepositJson(sampleJson)
        assertEquals("HDFC Bank", result.bankName)
        assertEquals(150000L, result.savingsInterestTotalPaise) // ₹1,500
        assertEquals(420000L, result.termDepositInterestTotalPaise) // ₹4,200
        assertEquals(1, result.candidateDeductionTransactions.size)
    }

    @Test
    fun testAaConsentArtifactManager_RegisterAndRevokeConsent() = runBlocking {
        val manager = AaConsentArtifactManager()
        val artifact = FipConsentArtifact(
            consentHandleId = "CONSENT-101",
            financialInformationProviderName = "State Bank of India",
            fipId = "SBI-FIP",
            fipCategory = "BANK",
            consentExpiryIso = "2026-12-31",
            state = ConsentLifeCycleState.ACTIVE_GRANTED
        )

        manager.registerConsent(artifact)
        assertEquals(1, manager.activeConsents.value.size)

        val revokeResult = manager.revokeConsentOneTap("CONSENT-101")
        assertTrue(revokeResult.isSuccess)
        assertEquals(ConsentLifeCycleState.REVOKED_BY_USER, manager.activeConsents.value.first().state)
    }

    @Test
    fun testParseMutualFundJson_UnrealizedHoldingsExcludedFromRealizedGains() {
        // VUL-18 FIX: Only redeemed or sold holdings generate capital gains; MTM unrealized holdings are excluded
        val rawPayload = """
            {
                "depository_or_rta": "CAMS",
                "holdings": [
                    {
                        "isin": "INF209K01157",
                        "cost_rupees": 100000,
                        "current_or_sale_value_rupees": 140000,
                        "is_redeemed": false
                    },
                    {
                        "isin": "INF179K01BE2",
                        "cost_rupees": 200000,
                        "current_or_sale_value_rupees": 275000,
                        "is_redeemed": true,
                        "sale_date": "2025-11-20"
                    }
                ]
            }
        """.trimIndent()

        val extraction = RebitFiDataInMemoryParser.parseMutualFundAndEquityJson(rawPayload)
        assertEquals("CAMS", extraction.registrarOrDepository)
        // Only the redeemed lot (275000 - 200000 = 75000) should be included!
        assertEquals(75000L, extraction.totalRealizedGainRupees)
        assertEquals(1, extraction.lots.size)
        assertEquals("INF179K01BE2", extraction.lots.first().isin)
    }
}

