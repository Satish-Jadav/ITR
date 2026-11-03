package in.zerotax.feature.aiextractor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationalTaxFilingBotTest {

    @Test
    fun testCompleteConversationalFilingLifecycleWithHraAndLifeEvents() {
        val bot = ConversationalTaxFilingBot()

        // 1. Initial greeting
        val initialGreeting = bot.getInitialGreeting()
        assertTrue(initialGreeting.isNotEmpty())
        assertEquals(ChatStep.WELCOME, bot.currentStep)

        // 2. User asks a contextual tax question first (TaxBuddy smart intent interception)
        val queryAnswers = bot.processUserMessage("Can I claim both HRA and Home Loan?")
        assertTrue(queryAnswers.any { it.text.contains("Can you claim BOTH HRA and Home Loan?") })
        assertTrue(queryAnswers.any { it.isAdvisorTip })

        // 3. Start conversation -> Asks for Salary
        val res1 = bot.processUserMessage("Start Filing Return 🚀")
        assertEquals(ChatStep.ASK_SALARY, bot.currentStep)
        assertTrue(res1.first().text.contains("Gross Salary"))

        // 4. User enters salary with job switch note
        val res2 = bot.processUserMessage("Switched Jobs, total 15,00,000 across two employers")
        assertEquals(ChatStep.ASK_HRA_RENT, bot.currentStep)
        assertEquals(15_00_000L, bot.snapshot.grossSalary)
        assertEquals(75_000L, bot.snapshot.standardDeduction)

        // 5. User enters Rent & HRA (Metro city)
        val resHra = bot.processUserMessage("Yes, Rent ₹25,000/mo (Metro)")
        assertEquals(ChatStep.ASK_HOME_LOAN, bot.currentStep)
        assertTrue(bot.snapshot.hraExemption > 0L)

        // 6. User provides Home Loan EMI (38,000 / month)
        val res3 = bot.processUserMessage("Yes, EMI ₹38,000/mo")
        assertEquals(ChatStep.ASK_80C_INVESTMENTS, bot.currentStep)
        assertEquals(2_00_000L, bot.snapshot.homeLoanInterest24b) // Sec 24(b) capped at 2,00,000
        assertTrue(bot.snapshot.sec80cTotal > 0L)

        // 7. User enters Section 80C investments
        val res80c = bot.processUserMessage("Yes, ₹1,50,000 (Full 80C Cap)")
        assertEquals(ChatStep.ASK_HEALTH_INSURANCE_80D, bot.currentStep)
        assertEquals(1_50_000L, bot.snapshot.sec80cTotal)

        // 8. User enters Health Insurance
        val res4 = bot.processUserMessage("₹25,000 (Self & Family)")
        assertEquals(ChatStep.ASK_NPS_INVESTMENT, bot.currentStep)
        assertEquals(25_000L, bot.snapshot.healthInsurance80d)

        // 9. User enters NPS Tier 1
        val res5 = bot.processUserMessage("Yes, ₹50,000 (Full Max)")
        assertEquals(ChatStep.ASK_EDUCATION_LOAN_80E, bot.currentStep)
        assertEquals(50_000L, bot.snapshot.nps80ccd1b)

        // 10. Education loan
        val resEdu = bot.processUserMessage("No Education Loan")
        assertEquals(ChatStep.ASK_DONATIONS_80G, bot.currentStep)

        // 11. Donations 80G
        val resDon = bot.processUserMessage("No Donations")
        assertEquals(ChatStep.ASK_SAVINGS_INTEREST, bot.currentStep)

        // 12. User enters Savings Interest 80TTA
        val res6 = bot.processUserMessage("Yes, ₹10,000 Exemption")
        assertEquals(ChatStep.ASK_CAPITAL_GAINS, bot.currentStep)
        assertEquals(10_000L, bot.snapshot.savingsExemption80tta)

        // 13. Capital gains
        val resCg = bot.processUserMessage("No Stock Sales")
        assertEquals(ChatStep.ASK_BANK_DETAILS, bot.currentStep)

        // 14. User enters Bank details for CPC refund
        val res7 = bot.processUserMessage("State Bank of India (••••1234)")
        assertEquals(ChatStep.COMPLETED_AWAITING_REVIEW, bot.currentStep)

        // Invariant verification:
        val finalMessage = res7.first()
        assertTrue(finalMessage.text.contains("Statutory Gate"))
        assertTrue(finalMessage.text.contains("Pre-Filing Review screen before final submission"))
        assertTrue(finalMessage.quickReplies.contains("Review All Points & Submit to Portal →"))

        // Dual regime recommendation
        assertNotNull(bot.snapshot.recommendedRegime)
        assertTrue(bot.snapshot.totalDeductionsClaimed > 75_000L)
        assertTrue(bot.snapshot.estimatedRefundDue > 0L)
    }

    @Test
    fun testConversationalBotNoDeductionsScenario() {
        val bot = ConversationalTaxFilingBot()
        bot.processUserMessage("Start")
        bot.processUserMessage("12,00,000") // Salary
        bot.processUserMessage("No Rented Home / Own House") // Skip HRA
        bot.processUserMessage("No Home Loan") // Skip home loan
        bot.processUserMessage("No Other 80C") // Skip 80C
        bot.processUserMessage("No Health Insurance") // Skip 80D
        bot.processUserMessage("No NPS") // Skip NPS
        bot.processUserMessage("No Education Loan") // Skip 80E
        bot.processUserMessage("No Donations") // Skip 80G
        bot.processUserMessage("No Bank Interest") // Skip 80TTA
        bot.processUserMessage("No Stock Sales") // Skip CG
        bot.processUserMessage("HDFC Bank (••••8819)") // Bank

        assertEquals(ChatStep.COMPLETED_AWAITING_REVIEW, bot.currentStep)
        assertEquals(75_000L, bot.snapshot.standardDeduction)
        assertEquals(11_25_000L, bot.snapshot.netTaxableIncome)
        // With income 11.25L, within Section 87A rebate for AY 2026-27 (up to 12L) -> 0 tax!
        assertEquals(0L, bot.snapshot.estimatedTaxLiability)
        assertEquals("New Regime (Default u/s 115BAC)", bot.snapshot.recommendedRegime)
    }
}
