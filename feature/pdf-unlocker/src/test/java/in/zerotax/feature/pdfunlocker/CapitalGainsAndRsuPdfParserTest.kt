package `in`.zerotax.feature.pdfunlocker

import org.junit.Assert.*
import org.junit.Test

class CapitalGainsAndRsuPdfParserTest {

    @Test
    fun testDomesticCapitalGainsPdfParser_ParsesLotsAndDetectsSgbAndBuyback() {
        val sampleCasText = """
            MUTUAL FUND & EQUITY CAPITAL GAINS STATEMENT
            PAN: ABCDE1234F
            Security: RELIANCE INDUSTRIES LTD ISIN: INE002A01018
            Qty: 10.0  Buy Value: 20,000.00  Sell Value: 25,000.00  LTCG
            
            Security: SOVEREIGN GOLD BOND 2016-17 SERIES I ISIN: IN0020160018 SGB
            Qty: 5.0  Buy Value: 15,000.00  Sell Value: 32,000.00
            
            Security: TATA CONSULTANCY SERVICES Buyback
            Qty: 8.0  Buy Value: 24,000.00  Sell Value: 36,000.00
        """.trimIndent()

        val report = DomesticCapitalGainsPdfParser.parseStatementText(sampleCasText)
        assertNotNull(report)
        assertTrue(report.tradeLots.isNotEmpty())
        assertTrue(report.sgbExemptProceedsPaise > 0L)
        assertTrue(report.shareBuybackDeemedDividendPaise > 0L)
    }

    @Test
    fun testForeignBrokerRsuAnd1042SParser_ParsesRsuAndForm1042S() {
        val sampleForeignText = """
            FORM 1042-S Foreign Person's U.S. Source Income
            Recipient: ABCDE1234F
            Gross Income: $ 1,000.00
            Federal Tax Withheld: $ 250.00
            
            RSU VEST RELEASE CONFIRMATION
            Vest Date: 2025-03-16
            Shares: 100.0  Price: $ 150.00
        """.trimIndent()

        val report = ForeignBrokerRsuAnd1042SParser.parseStatementText(sampleForeignText)
        assertNotNull(report.form1042S)
        assertEquals(1000.0, report.form1042S!!.grossDividendIncomeUsd, 0.01)
        assertEquals(250.0, report.form1042S!!.federalTaxWithheldUsd, 0.01)
        assertTrue(report.rsuLots.isNotEmpty())
        assertEquals(100.0, report.rsuLots.first().sharesVested, 0.01)
    }
}

