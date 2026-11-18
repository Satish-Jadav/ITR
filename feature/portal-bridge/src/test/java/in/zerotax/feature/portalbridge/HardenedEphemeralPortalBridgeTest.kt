package `in`.zerotax.feature.portalbridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HardenedEphemeralPortalBridgeTest {

    private val testScope = CoroutineScope(Dispatchers.Default)
    private lateinit var bridge: HardenedEphemeralPortalBridge

    @Before
    fun setUp() {
        bridge = HardenedEphemeralPortalBridge(testScope)
        bridge.activePan = "ABCDE1234F"
    }

    @Test
    fun testParsePrefillJsonExtractsSalaryAndOtherIncome() {
        val prefillJson = """
            {
                "tdsOnSalaries": [
                    {
                        "id": "SAL-01",
                        "employerOrDeductorOrCollecterName": "Tech Corp India Ltd",
                        "tanOfEmployer": "MUMB12345E",
                        "incomeChargeableUnderSalaries": 1250000,
                        "totalTDSSal": 112000
                    }
                ],
                "tdsOnOthThanSalaries": [
                    {
                        "id": "BANK-01",
                        "deductorName": "State Bank of India",
                        "tanOfDeductor": "DELH09876F",
                        "grossAmount": 45000,
                        "tdsDeducted": 4500
                    }
                ]
            }
        """.trimIndent()

        val parsed = bridge.parsePayloadToTransactions(prefillJson, PayloadType.PREFILL_JSON)
        assertEquals(2, parsed.size)

        val sal = parsed.first { it.category == `in`.zerotax.core.model.AisCategory.SALARY }
        assertEquals("Tech Corp India Ltd", sal.reportingEntity)
        assertEquals(1250000L, sal.grossAmount)
        assertEquals(112000L, sal.tdsWithheld)

        val bank = parsed.first { it.category == `in`.zerotax.core.model.AisCategory.SAVINGS_INTEREST }
        assertEquals("State Bank of India", bank.reportingEntity)
        assertEquals(45000L, bank.grossAmount)
        assertEquals(4500L, bank.tdsWithheld)
    }

    @Test
    fun testParseAisTisJsonExtractsDividendAndSecurities() {
        val aisJson = """
            {
                "transactions": [
                    {
                        "id": "AIS-DIV-1",
                        "category": "DIVIDENDS",
                        "deductorOrIssuerName": "TCS Ltd",
                        "deductorTan": "MUMT00001A",
                        "grossAmount": 15000,
                        "tdsWithheld": 1500,
                        "transactionDate": "2026-01-15"
                    },
                    {
                        "id": "AIS-SEC-1",
                        "category": "SALE_OF_SECURITIES",
                        "deductorOrIssuerName": "Zerodha Broking",
                        "deductorTan": "BLRZ00002B",
                        "grossAmount": 85000,
                        "tdsWithheld": 0,
                        "transactionDate": "2026-02-20"
                    }
                ]
            }
        """.trimIndent()

        val parsed = bridge.parsePayloadToTransactions(aisJson, PayloadType.AIS_TIS_JSON)
        assertEquals(2, parsed.size)
        assertEquals(`in`.zerotax.core.model.AisCategory.DIVIDENDS, parsed[0].category)
        assertEquals(15000L, parsed[0].grossAmount)
        assertEquals(`in`.zerotax.core.model.AisCategory.SALE_OF_SECURITIES, parsed[1].category)
        assertEquals(85000L, parsed[1].grossAmount)
    }

    @Test
    fun testParseForm26asExtractsPartA() {
        val form26asJson = """
            {
                "partA": [
                    {
                        "id": "26AS-A1",
                        "deductorName": "Infosys Ltd",
                        "tan": "BLRI12345C",
                        "totalAmountCredited": 950000,
                        "totalTdsDeducted": 75000
                    }
                ]
            }
        """.trimIndent()

        val parsed = bridge.parsePayloadToTransactions(form26asJson, PayloadType.FORM_26AS_JSON)
        assertEquals(1, parsed.size)
        assertEquals("Infosys Ltd", parsed[0].reportingEntity)
        assertEquals(950000L, parsed[0].grossAmount)
        assertEquals(75000L, parsed[0].tdsWithheld)
    }

    @Test
    fun testActivePanIsConfiguredProperly() {
        assertEquals("ABCDE1234F", bridge.activePan)
    }
}
