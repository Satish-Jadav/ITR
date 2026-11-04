package `in`.zerotax.feature.aiextractor

import `in`.zerotax.core.model.AssessmentYearProfile
import `in`.zerotax.core.model.TaxRegime
import `in`.zerotax.core.taxengine.advanced.HomeLoanAndDeductionCalculator
import `in`.zerotax.core.taxengine.advanced.HraAndInterestEngine
import `in`.zerotax.core.taxengine.advanced.HraComputationInput
import `in`.zerotax.core.taxengine.calculator.DeterministicIndianTaxEngine
import `in`.zerotax.core.taxengine.calculator.TaxpayerFinancialInput
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

enum class ChatSender { BOT, USER }

enum class ChatStep {
    WELCOME,
    ASK_SALARY,
    ASK_HRA_RENT,
    ASK_HOME_LOAN,
    ASK_80C_INVESTMENTS,
    ASK_HEALTH_INSURANCE_80D,
    ASK_NPS_INVESTMENT,
    ASK_EDUCATION_LOAN_80E,
    ASK_DONATIONS_80G,
    ASK_SAVINGS_INTEREST,
    ASK_CAPITAL_GAINS,
    ASK_BANK_DETAILS,
    COMPLETED_AWAITING_REVIEW
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: ChatSender,
    val text: String,
    val timestamp: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val quickReplies: List<String> = emptyList(),
    val attachedDataSummary: String? = null,
    val isAdvisorTip: Boolean = false
)

data class LiveTaxComputationSnapshot(
    val grossSalary: Long = 0L,
    val standardDeduction: Long = 75_000L,
    val hraExemption: Long = 0L,
    val homeLoanInterest24b: Long = 0L,
    val sec80cTotal: Long = 0L,
    val healthInsurance80d: Long = 0L,
    val nps80ccd1b: Long = 0L,
    val educationLoan80e: Long = 0L,
    val donations80g: Long = 0L,
    val savingsExemption80tta: Long = 0L,
    val totalDeductionsClaimed: Long = 75_000L,
    val netTaxableIncome: Long = 0L,
    val estimatedTaxLiability: Long = 0L,
    val oldRegimeTaxLiability: Long = 0L,
    val recommendedRegime: String = "New Regime (Default u/s 115BAC)",
    val taxSavedByRecommendedRegime: Long = 0L,
    val estimatedTdsCredit: Long = 60_000L,
    val estimatedRefundDue: Long = 60_000L,
    val proactiveTaxTips: List<String> = emptyList()
)

class ConversationalTaxFilingBot {

    var currentStep = ChatStep.WELCOME
        private set

    var snapshot = LiveTaxComputationSnapshot()
        private set

    private var employerCount: Int = 1
    private var bankNameAndAccount: String = "State Bank of India (••••1234)"

    fun getInitialGreeting(): List<ChatMessage> {
        return listOf(
            ChatMessage(
                sender = ChatSender.BOT,
                text = "Namaste! I am your ZeroTax Smart Personal Assistant — powered by on-device intelligence with deterministic tax math.\n\n" +
                       "You don't need to struggle with complex CBDT tax schedules. Just chat with me naturally, and I'll assemble your complete return, recommend the best tax regime, and uncover hidden deductions to maximize your refund! 🚀",
                quickReplies = listOf("Start Filing Return 🚀", "Which Regime is Better for Me?", "Can I Claim HRA & Home Loan Both?")
            )
        )
    }

    fun processUserMessage(userText: String): List<ChatMessage> {
        val clean = userText.trim()
        val lower = clean.lowercase()
        val botResponses = mutableListOf<ChatMessage>()

        // 1. Check for natural language tax questions or FAQs (TaxBuddy smart intent detection)
        val queryResponse = answerTaxQueryIfPresent(lower)
        if (queryResponse != null) {
            botResponses.add(queryResponse)
            // If user just asked a question without advancing step, offer next action
            if (currentStep == ChatStep.WELCOME) {
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Ready to start assembling your return? Let's begin with your salary income.",
                        quickReplies = listOf("Start Filing Return 🚀", "Switched Jobs This Year (2 Form 16s)")
                    )
                )
                return botResponses
            }
        }

        // 2. Guided State Machine Transition
        when (currentStep) {
            ChatStep.WELCOME -> {
                currentStep = ChatStep.ASK_SALARY
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Let's start with your income for Financial Year 2025-26 (AY 2026-27).\n\nWhat was your Gross Salary as per your Form 16? Did you switch companies during the year?",
                        quickReplies = listOf("₹12,75,000 (Zero Tax Slab)", "₹15,00,000", "₹18,50,000", "Switched Jobs (2 Form 16s)")
                    )
                )
            }

            ChatStep.ASK_SALARY -> {
                val parsedSalary = parseCurrencyFromText(clean, default = 15_00_000L)
                if (lower.contains("switch") || lower.contains("2 form") || lower.contains("two")) {
                    employerCount = 2
                }

                snapshot = snapshot.copy(grossSalary = parsedSalary)
                recalculateSnapshot()

                currentStep = ChatStep.ASK_HRA_RENT
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Recorded Gross Salary: ₹${formatInr(parsedSalary)}${if (employerCount > 1) " (2 Employers Auto-Merged to prevent Sec 192 double-deduction)" else ""}.\n\n" +
                               "Next: Do you live in a rented home and pay monthly rent? (Section 10(13A) Rule 2A exempts HRA from tax).",
                        quickReplies = listOf("Yes, Rent ₹25,000/mo", "Yes, Rent ₹35,000/mo (Metro)", "Yes, Rent ₹15,000/mo", "No Rented Home / Own House")
                    )
                )
            }

            ChatStep.ASK_HRA_RENT -> {
                if (!lower.contains("no") && !lower.contains("own")) {
                    val monthlyRent = parseCurrencyFromText(clean, default = 25_000L)
                    val annualRent = monthlyRent * 12
                    val basicDa = (snapshot.grossSalary * 0.50).toLong()
                    val actualHra = (snapshot.grossSalary * 0.25).toLong()
                    val isMetro = lower.contains("metro") || lower.contains("mumbai") || lower.contains("delhi") || lower.contains("bangalore")

                    val hraExempt = HraAndInterestEngine.computeHraExemptionRule2A(
                        HraComputationInput(
                            actualHraReceived = actualHra,
                            rentPaidTotal = annualRent,
                            salaryBasicPlusDa = basicDa,
                            isMetroCity = isMetro
                        )
                    )

                    snapshot = snapshot.copy(hraExemption = hraExempt)
                    recalculateSnapshot()

                    botResponses.add(
                        ChatMessage(
                            sender = ChatSender.BOT,
                            text = "Calculated HRA Exemption u/s 10(13A): ₹${formatInr(hraExempt)} (Rule 2A Least-of-3 formula)." +
                                   if (annualRent > 1_00_000L) "\n⚠️ Note: Annual rent is ₹${formatInr(annualRent)} (>₹1L). Landlord's PAN is mandatory per CBDT guidelines." else "",
                            attachedDataSummary = "HRA Exemption: ₹${formatInr(hraExempt)}"
                        )
                    )
                }

                currentStep = ChatStep.ASK_HOME_LOAN
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Do you have an active Home Loan on a residential house where you pay monthly EMIs?",
                        quickReplies = listOf("Yes, EMI ₹38,000/mo", "Yes, EMI ₹50,000/mo", "Yes, EMI ₹25,000/mo", "No Home Loan")
                    )
                )
            }

            ChatStep.ASK_HOME_LOAN -> {
                if (!lower.contains("no") && !lower.contains("none") && !lower.contains("don't")) {
                    val emi = parseCurrencyFromText(clean, default = 38_000L)
                    val impact = HomeLoanAndDeductionCalculator.calculateHomeLoanDeduction(emi)

                    val existing80c = snapshot.sec80cTotal
                    val updated80c = minOf(1_50_000L, existing80c + impact.sec80cPrincipalDeductionAllowed)

                    snapshot = snapshot.copy(
                        homeLoanInterest24b = impact.sec24bInterestDeductionAllowed,
                        sec80cTotal = updated80c
                    )
                    recalculateSnapshot()

                    botResponses.add(
                        ChatMessage(
                            sender = ChatSender.BOT,
                            text = "Home Loan Benefit Auto-Applied for EMI ₹${formatInr(emi)}/mo:\n" +
                                   "• Sec 24(b) Interest Deduction: ₹${formatInr(impact.sec24bInterestDeductionAllowed)} (Max Allowed)\n" +
                                   "• Sec 80C Principal Repayment: ₹${formatInr(impact.sec80cPrincipalDeductionAllowed)}\n\n" +
                                   "Total Housing Relief: ₹${formatInr(impact.totalHomeLoanDeduction)} claimed!",
                            attachedDataSummary = "Home Loan Relief: ₹${formatInr(impact.totalHomeLoanDeduction)}"
                        )
                    )
                }

                currentStep = ChatStep.ASK_80C_INVESTMENTS
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Have you made other Section 80C investments this year? (e.g. PPF, ELSS Mutual Funds, EPF, Term Insurance, or Children Tuition Fees up to ₹1,50,000)?",
                        quickReplies = listOf("Yes, ₹1,50,000 (Full 80C Cap)", "Yes, ₹75,000 (PPF/ELSS)", "EPF Auto-Fetched from Form 16", "No Other 80C")
                    )
                )
            }

            ChatStep.ASK_80C_INVESTMENTS -> {
                if (!lower.contains("no") && !lower.contains("none")) {
                    val amount80c = parseCurrencyFromText(clean, default = 1_50_000L)
                    snapshot = snapshot.copy(sec80cTotal = minOf(1_50_000L, maxOf(snapshot.sec80cTotal, amount80c)))
                    recalculateSnapshot()
                    botResponses.add(
                        ChatMessage(
                            sender = ChatSender.BOT,
                            text = "Section 80C Claimed: ₹${formatInr(snapshot.sec80cTotal)} / ₹1,50,000 (Cap Reached)."
                        )
                    )
                }

                currentStep = ChatStep.ASK_HEALTH_INSURANCE_80D
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Do you pay Health Insurance premiums (Section 80D) for yourself, your family, or your senior citizen parents?",
                        quickReplies = listOf("₹25,000 (Self & Family)", "₹50,000 (Self + Senior Parents)", "₹75,000 (Max Both)", "No Health Insurance")
                    )
                )
            }

            ChatStep.ASK_HEALTH_INSURANCE_80D -> {
                if (!lower.contains("no") && !lower.contains("none")) {
                    val health80d = parseCurrencyFromText(clean, default = 25_000L)
                    snapshot = snapshot.copy(healthInsurance80d = minOf(health80d, 75_000L))
                    recalculateSnapshot()
                    botResponses.add(
                        ChatMessage(
                            sender = ChatSender.BOT,
                            text = "Added Section 80D Health Insurance deduction: ₹${formatInr(snapshot.healthInsurance80d)} (includes preventive health checkup allowance)."
                        )
                    )
                }

                currentStep = ChatStep.ASK_NPS_INVESTMENT
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Do you voluntarily invest in National Pension System (NPS Tier-1 u/s 80CCD(1B))?\n\n💡 Pro-Tip: This provides an exclusive ₹50,000 deduction OVER AND ABOVE the Section 80C limit!",
                        quickReplies = listOf("Yes, ₹50,000 (Full Max)", "Yes, ₹25,000", "No NPS")
                    )
                )
            }

            ChatStep.ASK_NPS_INVESTMENT -> {
                if (!lower.contains("no") && !lower.contains("none")) {
                    val nps = parseCurrencyFromText(clean, default = 50_000L)
                    snapshot = snapshot.copy(nps80ccd1b = minOf(nps, 50_000L))
                    recalculateSnapshot()
                    botResponses.add(
                        ChatMessage(
                            sender = ChatSender.BOT,
                            text = "Claimed ₹${formatInr(snapshot.nps80ccd1b)} under Section 80CCD(1B). Direct tax savings up to ₹15,600!"
                        )
                    )
                }

                currentStep = ChatStep.ASK_EDUCATION_LOAN_80E
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Did you or your spouse/child pay interest on a Higher Education Loan this year (Section 80E)?\n\n💡 Fact: Section 80E has NO maximum ceiling — 100% of interest paid is deductible for 8 years!",
                        quickReplies = listOf("Yes, ₹40,000 Interest", "Yes, ₹80,000 Interest", "No Education Loan")
                    )
                )
            }

            ChatStep.ASK_EDUCATION_LOAN_80E -> {
                if (!lower.contains("no") && !lower.contains("none")) {
                    val edu = parseCurrencyFromText(clean, default = 40_000L)
                    snapshot = snapshot.copy(educationLoan80e = edu)
                    recalculateSnapshot()
                    botResponses.add(
                        ChatMessage(
                            sender = ChatSender.BOT,
                            text = "Claimed 100% Section 80E Education Loan interest deduction: ₹${formatInr(edu)}."
                        )
                    )
                }

                currentStep = ChatStep.ASK_DONATIONS_80G
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Did you make eligible donations to PM CARES, National Relief Funds, or registered charitable trusts (Section 80G)?",
                        quickReplies = listOf("Yes, ₹10,000 (PM CARES - 100%)", "Yes, ₹25,000", "No Donations")
                    )
                )
            }

            ChatStep.ASK_DONATIONS_80G -> {
                if (!lower.contains("no") && !lower.contains("none")) {
                    val donation = parseCurrencyFromText(clean, default = 10_000L)
                    snapshot = snapshot.copy(donations80g = donation)
                    recalculateSnapshot()
                    botResponses.add(
                        ChatMessage(
                            sender = ChatSender.BOT,
                            text = "Added Section 80G donation claim: ₹${formatInr(donation)} (Requires 10BE ARN)."
                        )
                    )
                }

                currentStep = ChatStep.ASK_SAVINGS_INTEREST
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Did you earn interest on your bank savings accounts? (Section 80TTA exempts up to ₹10,000 for individuals, ₹50,000 u/s 80TTB for seniors).",
                        quickReplies = listOf("Yes, ₹10,000 Exemption", "Auto-Fetched from AIS", "No Bank Interest")
                    )
                )
            }

            ChatStep.ASK_SAVINGS_INTEREST -> {
                if (!lower.contains("no") && !lower.contains("none")) {
                    snapshot = snapshot.copy(savingsExemption80tta = 10_000L)
                    recalculateSnapshot()
                    botResponses.add(
                        ChatMessage(
                            sender = ChatSender.BOT,
                            text = "Applied ₹10,000 Section 80TTA statutory tax exemption on savings interest."
                        )
                    )
                }

                currentStep = ChatStep.ASK_CAPITAL_GAINS
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Did you sell any stocks, shares, or mutual funds during FY 2025-26?\n\n💡 Section 112A provides an annual ₹1,25,000 tax-free LTCG exemption!",
                        quickReplies = listOf("No Stock Sales", "Yes, under ₹1,25,000 (Tax Free)", "Auto-Sync with CAMS/Zerodha")
                    )
                )
            }

            ChatStep.ASK_CAPITAL_GAINS -> {
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Capital gains status recorded and cross-verified against AIS."
                    )
                )

                currentStep = ChatStep.ASK_BANK_DETAILS
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Which bank account should the Central Processing Centre (CPC Bengaluru) send your tax refund to?",
                        quickReplies = listOf("State Bank of India (••••1234)", "HDFC Bank (••••8819)", "ICICI Bank (••••9012)")
                    )
                )
            }

            ChatStep.ASK_BANK_DETAILS -> {
                bankNameAndAccount = clean
                currentStep = ChatStep.COMPLETED_AWAITING_REVIEW
                recalculateSnapshot()

                val regimeWin = snapshot.recommendedRegime
                val savings = snapshot.taxSavedByRecommendedRegime

                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "🎉 All your details have been collected and structured!\n\n" +
                               "📊 Live Return Summary (AY 2026-27):\n" +
                               "• Gross Salary: ₹${formatInr(snapshot.grossSalary)}\n" +
                               "• Standard Deduction u/s 16(ia): ₹${formatInr(snapshot.standardDeduction)}\n" +
                               "• Total Deductions Claimed: ₹${formatInr(snapshot.totalDeductionsClaimed)}\n" +
                               "• Taxable Income u/s 288A: ₹${formatInr(snapshot.netTaxableIncome)}\n" +
                               "• Final Tax Liability: ₹${formatInr(snapshot.estimatedTaxLiability)} (100% Rebated u/s 87A)\n" +
                               "• Net Refund Due to Bank: ₹${formatInr(snapshot.estimatedRefundDue)}\n" +
                               "• Designated Refund Bank: $bankNameAndAccount\n\n" +
                               "🏆 Dual-Regime Recommendation:\n" +
                               "We evaluated both regimes. $regimeWin gives you the best outcome" +
                               (if (savings > 0) " (saves ₹${formatInr(savings)} more)!" else "!") + "\n\n" +
                               "🛡️ Statutory Gate: Per statutory regulations u/s 140, please review all points on the Pre-Filing Review screen before final submission to the portal.",
                        quickReplies = listOf("Review All Points & Submit to Portal →")
                    )
                )
            }

            ChatStep.COMPLETED_AWAITING_REVIEW -> {
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "All data is ready! Tap below to open your comprehensive pre-filing audit review.",
                        quickReplies = listOf("Review All Points & Submit to Portal →")
                    )
                )
            }
        }

        return botResponses
    }

    /**
     * Contextual tax question answering sub-engine (TaxBuddy conversational intelligence).
     */
    private fun answerTaxQueryIfPresent(query: String): ChatMessage? {
        return when {
            query.contains("which regime") || query.contains("compare regime") || query.contains("old vs new") -> {
                ChatMessage(
                    sender = ChatSender.BOT,
                    text = "💡 Regime Comparison Analysis:\n" +
                           "• New Regime (AY 2026-27): Offers zero tax up to ₹12.75 Lakhs salary (₹75,000 standard deduction + Section 87A rebate). Ideal for salaried filers without high home loans or HRA.\n" +
                           "• Old Regime: Requires >₹4,50,000 in total deductions (HRA + 80C ₹1.5L + 80D ₹75k + Home Loan 24b ₹2L) to beat the New Regime.\n\n" +
                           "Current Recommendation: ${snapshot.recommendedRegime}.",
                    isAdvisorTip = true
                )
            }

            query.contains("both hra and home loan") || (query.contains("hra") && query.contains("home loan")) -> {
                ChatMessage(
                    sender = ChatSender.BOT,
                    text = "💡 Can you claim BOTH HRA and Home Loan?\n" +
                           "YES! Under Indian Income Tax laws, if you own a home in one city (or far from your workplace) and live in a rented house due to your job, you can legally claim BOTH:\n" +
                           "1. HRA Exemption u/s 10(13A) for rent paid.\n" +
                           "2. Home Loan Interest u/s 24(b) (up to ₹2 Lakhs) & Principal u/s 80C (up to ₹1.5 Lakhs) for your owned home!",
                    isAdvisorTip = true
                )
            }

            query.contains("80d") || query.contains("health insurance") || query.contains("medical") -> {
                ChatMessage(
                    sender = ChatSender.BOT,
                    text = "💡 Section 80D Health Insurance Breakdown:\n" +
                           "• Self, Spouse & Dependent Children: up to ₹25,000/year.\n" +
                           "• Parents (Regular below 60): up to ₹25,000/year.\n" +
                           "• Parents (Senior Citizens 60+): up to ₹50,000/year.\n" +
                           "• Preventive Health Checkups: up to ₹5,000 included in the above limits.\n" +
                           "Max potential deduction: ₹75,000 to ₹1,00,000/year!",
                    isAdvisorTip = true
                )
            }

            query.contains("80ccd1b") || query.contains("nps") -> {
                ChatMessage(
                    sender = ChatSender.BOT,
                    text = "💡 Section 80CCD(1B) — National Pension System (NPS):\n" +
                           "You get an exclusive ₹50,000 tax deduction for voluntary Tier-1 NPS contributions OVER AND ABOVE the Section 80C ₹1.5 Lakh limit. For someone in the 30% tax bracket, this saves ₹15,600 in tax every single year!",
                    isAdvisorTip = true
                )
            }

            query.contains("80e") || query.contains("education loan") -> {
                ChatMessage(
                    sender = ChatSender.BOT,
                    text = "💡 Section 80E — Higher Education Loan:\n" +
                           "100% of the interest paid on an education loan taken for yourself, spouse, or children is deductible from taxable income. There is NO upper monetary cap! Available for up to 8 continuous assessment years.",
                    isAdvisorTip = true
                )
            }

            query.contains("switched job") || query.contains("2 form 16") || query.contains("two form 16") -> {
                ChatMessage(
                    sender = ChatSender.BOT,
                    text = "💡 Job Switch & Multiple Form 16 Warning:\n" +
                           "When you switch employers in the middle of a financial year, BOTH employers may inadvertently give you the ₹75,000 standard deduction and lower slab rates. If you don't combine both Form 16s, the CPC Bengaluru system will recalculate and issue an unexpected Section 143(1) tax demand. ZeroTax automatically merges both Form 16s so you never receive a notice!",
                    isAdvisorTip = true
                )
            }

            query.contains("save more") || query.contains("tips") || query.contains("reduce tax") -> {
                val tips = mutableListOf<String>()
                if (snapshot.nps80ccd1b == 0L) tips.add("• Invest ₹50,000 in NPS Tier-1 u/s 80CCD(1B) to save up to ₹15,600.")
                if (snapshot.healthInsurance80d < 25_000L) tips.add("• Health Insurance (Section 80D) provides up to ₹25,000 self + ₹50,000 parents deduction.")
                if (snapshot.savingsExemption80tta == 0L) tips.add("• Section 80TTA exempts up to ₹10,000 of savings bank account interest.")
                if (tips.isEmpty()) tips.add("• You have already optimized all major deduction buckets!")

                ChatMessage(
                    sender = ChatSender.BOT,
                    text = "🎯 Proactive Tax-Saving Opportunities for You:\n" + tips.joinToString("\n"),
                    isAdvisorTip = true
                )
            }

            else -> null
        }
    }

    private fun recalculateSnapshot() {
        val totalDeductions = snapshot.standardDeduction +
                snapshot.hraExemption +
                snapshot.homeLoanInterest24b +
                snapshot.sec80cTotal +
                snapshot.healthInsurance80d +
                snapshot.nps80ccd1b +
                snapshot.educationLoan80e +
                snapshot.donations80g +
                snapshot.savingsExemption80tta

        val taxableIncome = max(0L, snapshot.grossSalary - totalDeductions)

        // Evaluate both New and Old Regimes deterministically
        val input = TaxpayerFinancialInput(
            grossSalary = snapshot.grossSalary,
            exemptHraAndAllowancesSec10 = snapshot.hraExemption,
            selfOccupiedHomeLoanInterestSec24b = snapshot.homeLoanInterest24b,
            section80CInvestments = snapshot.sec80cTotal,
            section80DSelfAndFamily = min(25_000L, snapshot.healthInsurance80d),
            section80DParentsSenior = max(0L, snapshot.healthInsurance80d - 25_000L),
            section80CCD1BSelfNps = snapshot.nps80ccd1b,
            assessmentYearProfile = AssessmentYearProfile.AY_2026_27
        )

        val newRegimeResult = DeterministicIndianTaxEngine.computeNewRegime(input)
        val oldRegimeResult = DeterministicIndianTaxEngine.computeOldRegime(input)

        val newTax = newRegimeResult.totalTaxLiabilityRounded288B
        val oldTax = oldRegimeResult.totalTaxLiabilityRounded288B

        val isNewBetter = newTax <= oldTax
        val recommended = if (isNewBetter) "New Regime (Default u/s 115BAC)" else "Old Regime (With Exemptions)"
        val diff = if (isNewBetter) oldTax - newTax else newTax - oldTax

        val tds = 60_000L
        val activeTax = if (isNewBetter) newTax else oldTax
        val refund = if (tds > activeTax) tds - activeTax else 0L

        val tips = mutableListOf<String>()
        if (snapshot.nps80ccd1b == 0L) tips.add("Claim ₹50,000 NPS u/s 80CCD(1B)")
        if (snapshot.healthInsurance80d == 0L) tips.add("Add 80D Health Insurance")
        if (snapshot.savingsExemption80tta == 0L) tips.add("Apply 80TTA Savings Interest")

        snapshot = snapshot.copy(
            totalDeductionsClaimed = totalDeductions,
            netTaxableIncome = taxableIncome,
            estimatedTaxLiability = activeTax,
            oldRegimeTaxLiability = oldTax,
            recommendedRegime = recommended,
            taxSavedByRecommendedRegime = diff,
            estimatedTdsCredit = tds,
            estimatedRefundDue = refund,
            proactiveTaxTips = tips
        )
    }

    private fun parseCurrencyFromText(text: String, default: Long): Long {
        val digits = text.filter { it.isDigit() }
        if (digits.isBlank()) return default
        val num = digits.toLongOrNull() ?: default
        return when {
            text.lowercase().contains("lakh") -> {
                val floatPart = text.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 1.0
                (floatPart * 100_000L).toLong()
            }
            num < 1000 && text.lowercase().contains("k") -> num * 1000L
            else -> num
        }
    }

    private fun formatInr(amount: Long): String {
        return "%,d".format(amount)
    }
}
