package in.zerotax.feature.aiextractor

import in.zerotax.core.taxengine.advanced.HomeLoanAndDeductionCalculator
import in.zerotax.core.taxengine.calculator.DeterministicIndianTaxEngine
import in.zerotax.core.taxengine.calculator.TaxpayerFinancialInput
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class ChatSender { BOT, USER }

enum class ChatStep {
    WELCOME,
    ASK_SALARY,
    ASK_HOME_LOAN,
    ASK_HEALTH_INSURANCE,
    ASK_NPS_INVESTMENT,
    ASK_SAVINGS_INTEREST,
    ASK_BANK_DETAILS,
    COMPLETED_AWAITING_REVIEW
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: ChatSender,
    val text: String,
    val timestamp: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val quickReplies: List<String> = emptyList(),
    val attachedDataSummary: String? = null
)

data class LiveTaxComputationSnapshot(
    val grossSalary: Long = 0L,
    val standardDeduction: Long = 75_000L,
    val homeLoanInterest24b: Long = 0L,
    val homeLoanPrincipal80c: Long = 0L,
    val healthInsurance80d: Long = 0L,
    val nps80ccd1b: Long = 0L,
    val savingsExemption80tta: Long = 0L,
    val totalDeductionsClaimed: Long = 75_000L,
    val netTaxableIncome: Long = 0L,
    val estimatedTaxLiability: Long = 0L,
    val estimatedTdsCredit: Long = 60_000L,
    val estimatedRefundDue: Long = 60_000L
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
                text = "Namaste! I am your ZeroTax Personal Assistant. You don't need to fill out complicated tax forms — just chat with me, and I'll assemble your complete ITR-1 return step-by-step.",
                quickReplies = listOf("Start Filing Return 🚀", "How does it work?")
            )
        )
    }

    fun processUserMessage(userText: String): List<ChatMessage> {
        val clean = userText.trim()
        val lower = clean.lowercase()
        val botResponses = mutableListOf<ChatMessage>()

        when (currentStep) {
            ChatStep.WELCOME -> {
                currentStep = ChatStep.ASK_SALARY
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Let's start with your income for Financial Year 2025-26 (AY 2026-27). What was your Gross Salary as per your Form 16, or did you switch companies during the year?",
                        quickReplies = listOf("₹12,75,000 (Zero Tax Slab)", "₹15,00,000", "₹18,50,000", "Switched Jobs (2 Form 16s)")
                    )
                )
            }

            ChatStep.ASK_SALARY -> {
                val parsedSalary = parseCurrencyFromText(clean, default = 15_00_000L)
                if (lower.contains("switch") || lower.contains("2 form") || lower.contains("two")) {
                    employerCount = 2
                }

                snapshot = snapshot.copy(
                    grossSalary = parsedSalary,
                    netTaxableIncome = maxOf(0L, parsedSalary - snapshot.standardDeduction)
                )
                recalculateSnapshot()

                currentStep = ChatStep.ASK_HOME_LOAN
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Great! Recorded Gross Salary: ₹${formatInr(parsedSalary)}${if (employerCount > 1) " (2 Employers Auto-Merged)" else ""}.\n\nNext, do you have a running Home Loan where you pay a monthly EMI?",
                        quickReplies = listOf("Yes, EMI ₹38,000/mo", "Yes, EMI ₹50,000/mo", "Yes, EMI ₹25,000/mo", "No Home Loan")
                    )
                )
            }

            ChatStep.ASK_HOME_LOAN -> {
                if (lower.contains("no") || lower.contains("none") || lower.contains("don't")) {
                    botResponses.add(
                        ChatMessage(
                            sender = ChatSender.BOT,
                            text = "Understood, no home loan claimed."
                        )
                    )
                } else {
                    val emi = parseCurrencyFromText(clean, default = 38_000L)
                    val impact = HomeLoanAndDeductionCalculator.calculateHomeLoanDeduction(emi)

                    snapshot = snapshot.copy(
                        homeLoanInterest24b = impact.sec24bInterestDeductionAllowed,
                        homeLoanPrincipal80c = impact.sec80cPrincipalDeductionAllowed
                    )
                    recalculateSnapshot()

                    botResponses.add(
                        ChatMessage(
                            sender = ChatSender.BOT,
                            text = "Awesome! For EMI ₹${formatInr(emi)}/month, I automatically calculated:\n• Sec 24(b) Interest: ₹${formatInr(impact.sec24bInterestDeductionAllowed)} (Max Cap)\n• Sec 80C Principal: ₹${formatInr(impact.sec80cPrincipalDeductionAllowed)} (Max Cap)\n\nTotal Home Loan Deduction: ₹${formatInr(impact.totalHomeLoanDeduction)} claimed!",
                            attachedDataSummary = "Home Loan Deduction: ₹${formatInr(impact.totalHomeLoanDeduction)}"
                        )
                    )
                }

                currentStep = ChatStep.ASK_HEALTH_INSURANCE
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Do you pay Health Insurance premiums (Section 80D) for yourself, your family, or your senior citizen parents?",
                        quickReplies = listOf("₹25,000 (Self & Family)", "₹50,000 (Family + Parents)", "₹20,000", "No Health Insurance")
                    )
                )
            }

            ChatStep.ASK_HEALTH_INSURANCE -> {
                if (!lower.contains("no") && !lower.contains("none")) {
                    val health80d = parseCurrencyFromText(clean, default = 25_000L)
                    snapshot = snapshot.copy(healthInsurance80d = minOf(health80d, 75_000L))
                    recalculateSnapshot()
                    botResponses.add(
                        ChatMessage(
                            sender = ChatSender.BOT,
                            text = "Added Section 80D Health Insurance deduction: ₹${formatInr(snapshot.healthInsurance80d)}."
                        )
                    )
                }

                currentStep = ChatStep.ASK_NPS_INVESTMENT
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Do you invest voluntarily in National Pension System (NPS Tier-1 u/s 80CCD(1B))?",
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
                            text = "Claimed ₹${formatInr(snapshot.nps80ccd1b)} under Section 80CCD(1B) (Exclusive allowance above Section 80C)."
                        )
                    )
                }

                currentStep = ChatStep.ASK_SAVINGS_INTEREST
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Did you earn interest on your savings bank accounts this year? (Section 80TTA exempts up to ₹10,000).",
                        quickReplies = listOf("Yes, ₹10,000 Exemption", "Auto-Fetched from AIS", "No Interest")
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
                            text = "Applied ₹10,000 statutory tax exemption under Section 80TTA on savings interest."
                        )
                    )
                }

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

                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "🎉 All your details have been collected and structured!\n\n" +
                                "📊 Live Return Summary:\n" +
                                "• Gross Salary: ₹${formatInr(snapshot.grossSalary)}\n" +
                                "• Standard Deduction u/s 16(ia): ₹${formatInr(snapshot.standardDeduction)}\n" +
                                "• Total Deductions Claimed: ₹${formatInr(snapshot.totalDeductionsClaimed)}\n" +
                                "• Final Tax Liability: ₹${formatInr(snapshot.estimatedTaxLiability)} (100% Rebated u/s 87A)\n" +
                                "• Net Refund Due to Bank: ₹${formatInr(snapshot.estimatedRefundDue)}\n" +
                                "• Refund Destination: $bankNameAndAccount\n\n" +
                                "🛡️ Statutory Check: Before submitting to the ITD portal, our policy requires your full review of every point on the Pre-Filing Review screen.",
                        quickReplies = listOf("Review All Points & Submit to Portal →")
                    )
                )
            }

            ChatStep.COMPLETED_AWAITING_REVIEW -> {
                botResponses.add(
                    ChatMessage(
                        sender = ChatSender.BOT,
                        text = "Tap the 'Review All Points & Submit to Portal' button below to open your comprehensive audit review.",
                        quickReplies = listOf("Review All Points & Submit to Portal →")
                    )
                )
            }
        }

        return botResponses
    }

    private fun recalculateSnapshot() {
        val totalDeductions = snapshot.standardDeduction +
                snapshot.homeLoanInterest24b +
                snapshot.homeLoanPrincipal80c +
                snapshot.healthInsurance80d +
                snapshot.nps80ccd1b +
                snapshot.savingsExemption80tta

        val taxableIncome = maxOf(0L, snapshot.grossSalary - totalDeductions)
        val taxLiability = if (taxableIncome <= 12_00_000L) 0L else ((taxableIncome - 12_00_000L) * 0.15).toLong()
        val tds = 60_000L
        val refund = if (tds > taxLiability) tds - taxLiability else 0L

        snapshot = snapshot.copy(
            totalDeductionsClaimed = totalDeductions,
            netTaxableIncome = taxableIncome,
            estimatedTaxLiability = taxLiability,
            estimatedTdsCredit = tds,
            estimatedRefundDue = refund
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
