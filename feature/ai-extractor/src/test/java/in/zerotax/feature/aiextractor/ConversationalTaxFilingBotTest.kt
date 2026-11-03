package in.zerotax.feature.aiextractor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationalTaxFilingBotTest {

    @Test
    fun testCompleteConversationalFilingLifecycle() {
        val bot = ConversationalTaxFilingBot()

        // 1. Initial greeting
        val initialGreeting = bot.getInitialGreeting()
        assertTrue(initialGreeting.isNotEmpty())
        assertEquals(ChatStep.WELCOME, bot.currentStep)

        // 2. Start conversation -> Asks for Salary
        val res1 = bot.processUserMessage("Start Filing Return 🚀")
        assertEquals(ChatStep.ASK_SALARY, bot.currentStep)
        assertTrue(res1.first().text.contains("Gross Salary"))

        // 3. User enters salary with job switch note
        val res2 = bot.processUserMessage("Switched Jobs, total 15,00,000 across two employers")
        assertEquals(ChatStep.ASK_HOME_LOAN, bot.currentStep)
        assertEquals(15_00_000L, bot.snapshot.grossSalary)
        assertEquals(75_000L, bot.snapshot.standardDeduction)

        // 4. User provides Home Loan EMI (38,000 / month)
        val res3 = bot.processUserMessage("Yes, EMI ₹38,000/mo")
        assertEquals(ChatStep.ASK_HEALTH_INSURANCE, bot.currentStep)
        // Sec 24(b) interest capped at 2,00,000
        assertEquals(2_00_000L, bot.snapshot.homeLoanInterest24b)
        // Sec 80C principal capped at 1,50_000
        assertEquals(1_50_000L, bot.snapshot.homeLoanPrincipal80c)

        // 5. User enters Health Insurance
        val res4 = bot.processUserMessage("₹25,000 (Self & Family)")
        assertEquals(ChatStep.ASK_NPS_INVESTMENT, bot.currentStep)
        assertEquals(25_000L, bot.snapshot.healthInsurance80d)

        // 6. User enters NPS Tier 1
        val res5 = bot.processUserMessage("Yes, ₹50,000 (Full Max)")
        assertEquals(ChatStep.ASK_SAVINGS_INTEREST, bot.currentStep)
        assertEquals(50_000L, bot.snapshot.nps80ccd1b)

        // 7. User enters Savings Interest 80TTA
        val res6 = bot.processUserMessage("Yes, ₹10,000 Exemption")
        assertEquals(ChatStep.ASK_BANK_DETAILS, bot.currentStep)
        assertEquals(10_000L, bot.snapshot.savingsExemption80tta)

        // 8. User enters Bank details for CPC refund
        val res7 = bot.processUserMessage("State Bank of India (••••1234)")
        assertEquals(ChatStep.COMPLETED_AWAITING_REVIEW, bot.currentStep)

        // Invariant verification:
        // Must await user's full review on all points before any submission
        val finalMessage = res7.first()
        assertTrue(finalMessage.text.contains("Statutory Check: Before submitting to the ITD portal"))
        assertTrue(finalMessage.text.contains("requires your full review"))
        assertTrue(finalMessage.quickReplies.contains("Review All Points & Submit to Portal →"))

        // Check deductions aggregate
        // 75k standard + 200k sec24b + 150k 80c + 25k 80d + 50k nps + 10k 80tta = 5,10,000
        assertEquals(5_10_000L, bot.snapshot.totalDeductionsClaimed)
        assertEquals(9_90_000L, bot.snapshot.netTaxableIncome)
        // Since net taxable income is below 12.75L under new regime, rebate 87A results in 0 tax
        assertEquals(0L, bot.snapshot.estimatedTaxLiability)
        assertTrue(bot.snapshot.estimatedRefundDue > 0L)
    }

    @Test
    fun testConversationalBotNoDeductionsScenario() {
        val bot = ConversationalTaxFilingBot()
        bot.processUserMessage("Start")
        bot.processUserMessage("12,00,000") // Salary
        bot.processUserMessage("No Home Loan") // Skip home loan
        bot.processUserMessage("No Health Insurance") // Skip 80D
        bot.processUserMessage("No NPS") // Skip NPS
        bot.processUserMessage("No Interest") // Skip 80TTA
        bot.processUserMessage("HDFC Bank (••••8819)") // Bank

        assertEquals(ChatStep.COMPLETED_AWAITING_REVIEW, bot.currentStep)
        assertEquals(75_000L, bot.snapshot.totalDeductionsClaimed)
        assertEquals(11_25_000L, bot.snapshot.netTaxableIncome)
        assertEquals(0L, bot.snapshot.estimatedTaxLiability) // Within Section 87A rebate for AY 2026-27
    }
}
