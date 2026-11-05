package `in`.zerotax.feature.aiextractor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimizedTaxCopilotChatbotEngineTest {

    private val engine = OptimizedTaxCopilotChatbotEngine()

    @Test
    fun parseIndianShorthandAmount_parsesVariousIndianFormats() {
        assertEquals(50_000L, engine.parseIndianShorthandAmount("invest 50k in nps"))
        assertEquals(1_50_000L, engine.parseIndianShorthandAmount("80c deduction of 1.5 lakh"))
        assertEquals(25_000L, engine.parseIndianShorthandAmount("medical insurance 25 thousand"))
        assertEquals(1_00_00_000L, engine.parseIndianShorthandAmount("business turnover 1 crore"))
    }

    @Test
    fun tryTier0FastPath_matchesNpsAndInsuranceQueries() {
        val npsIntent = engine.tryTier0FastPath("can i save tax if i put 50k in nps 80ccd1b?")
        assertNotNull(npsIntent)
        assertEquals("SIM_WHAT_IF", npsIntent!!.intentCode)
        assertEquals(50_000L, npsIntent.add80CCD1BNpsRupees)
        assertTrue(npsIntent.resolvedByTier0Regex)

        val rentIntent = engine.tryTier0FastPath("monthly rent is 28000 for hra")
        assertNotNull(rentIntent)
        assertEquals("SIM_WHAT_IF", rentIntent!!.intentCode)
        assertEquals(28_000L, rentIntent.monthlyRentRupees)

        val noticeIntent = engine.tryTier0FastPath("i received a 143(1) intimation notice from cpc")
        assertNotNull(noticeIntent)
        assertEquals("NOTICE_HELP", noticeIntent!!.intentCode)
    }

    @Test
    fun quickPickOptionChips_haveMultilingualLabels() {
        for (chip in CopilotQuickOptionChip.values()) {
            assertTrue(chip.categoryPill.isNotBlank())
            assertTrue(chip.displayTitleEn.isNotBlank())
            assertTrue(chip.displayTitleHi.isNotBlank())
            assertTrue(chip.displayTitleGu.isNotBlank())
        }
    }
}
