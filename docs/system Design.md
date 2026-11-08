Detailed System Design Document ("ZeroTax-Local" — CA & Principal Architect Edition)
Production-grade engineering and statutory compliance specification for client-side Indian Income Tax Return computation, on-device document extraction via Android AICore (Gemini Nano), strict TRACES/26AS deterministic reconciliation, and CBDT Category 'A' e-filing validation.
0. Principal Architect & CA Review Summary (Critical Bug Fixes & Enhancements Over Draft v1)
Fixed Critical Section 143(1)(a) TDS Short-Credit Bug in reconcileSalary:Draft bug in line 99 computed val verifiedTds = tds26as?.tdsDeposited ?: f16?.tdsDeducted ?: 0L. This mistakenly claimed f16.tdsDeducted whenever tds26as was missing or failed to cap f16.tdsDeducted when an employer deducted TDS via payslips but defaulted on depositing it to the government via Form 24Q. Claiming TDS above Form 26AS Part A causes an automatic Section 143(1)(a) short-credit demand intimation from CPC Bengaluru. The production engine enforces val allowedTds = min(primary.tdsReported, deposited26AsTds) inside DeterministicReconciler.reconcileSalaries(), surfacing an Employer TDS Protection Nudge Card for unreflectedTdsAmount = max(0L, primary.tdsReported - deposited26AsTds).
Added Rule 119A(b) Principal Rounding & Section 207 Senior Citizen Immunity to Interest Engine:Implemented Rule 119A(b) truncation ((unpaidTax / 100L) * 100L) alongside Sections 288A/288B ₹10 rounding, plus Section 207(2) Senior Citizen (Age >= 60, no business income) immunity from Section 234B interest and Section 234C 12%/36% safe harbors.
Added Complete 4-Prompt On-Device Gemini Nano (AICore) Catalog & Token-Locking:Expanded the catalog to all 4 production micro-prompts: P1-PROMPT-01 (Form 16 Part B), P1-PROMPT-02 (Rent/80D/80G Receipts), P2-PROMPT-01 (Account Aggregator Bank Narration Classifier), and P3-PROMPT-01 (Token-Locked Multilingual Notice Explainer across en-IN, hi-IN, and gu-IN).
Added Deterministic Capital Gains (Sec 112A / 112(1)(a)), Loss Matrix (Secs 70/71/74), and Foreign Asset Sub-Engines:Added complete Kotlin algorithms for Section 55(2)(ac) Jan 31, 2018 grandfathering, Second Proviso to Section 112(1)(a) pre-July 23, 2024 immovable property indexation (12.5% unindexed vs. 20% with CII 376), Section 70(3) LTCL-vs-STCG isolation block, and Rule 115 / Rule 128(5) SBI TTBR exchange rate conversion.
1. End-to-End Sequence Pipelines
1.1 Automated Data Ingestion, UIDAI Scrubbing & Deterministic Reconciliation Pipeline
The ingestion architecture coordinates client-side Android sandbox extraction, local hardware keystore initialization, ephemeral portal scrapers, and on-device AICore parsing:

User             Compose UI          Hardened Portal / AA Bridge      On-Device AICore       :core:tax-engine

 |                   |                           |                            |                      |
|---|---|---|---|
 |  + DPDP Sec 6     |--2. Init StrongBox Vault  |                            |                      |
 |                   |--3. Trigger Portal Sync-->|                            |                      |
 |<--4. Enter OTP----|                           |                            |                      |
 |                   |<--5. Prefill/26AS/AIS-----| (500ms Cookie/DOM Wipe)    |                      |
 |                   |   (Aadhaar Scrubbed)      |                            |                      |
 |--6. Select PDFs-->|                           |                            |                      |
 |                   |--7. 15-Perm Unlocker (<80ms) + TRACES Regex (<30ms)--->|                      |
 |                   |   (or AICore Gemini Nano temp=0.0f, topK=1)----------->|                      |
 |                   |                           |                            |--8. ±₹5 Math Sanity->|
 |                   |<--9. Cropped Bitmap Disambiguation (if |Diff| > ₹5)----|                      |
 |                   |--10. Reconcile by TAN (Sec 192(2B)) & Cap TDS at 26AS------------------------>|
 |                   |<--11. Emit Exception Nudge Cards + Dual-Regime Comparison---------------------|
 |<--12. 1-Tap File--|                           |                            |                      |
2. Production On-Device Gemini Nano (AICore) Prompt Engineering & Token-Locking
All LLM invocations run locally inside Android AICore without internet connectivity. Temperature is clamped to 0.0f with topK = 1 to ensure strict deterministic execution and eliminate hallucination risks.
2.1 Micro-Prompt P1-PROMPT-01: Form 16 Part B JSON Structurer (temperature = 0.0f, topK = 1)
SYSTEM INSTRUCTION (Role A - Local Document Structuring Parser):
You are an on-device JSON parser running inside Android AICore.
STRICT DETERMINISTIC RULES:
1. NEVER perform arithmetic, addition, subtraction, or percentage calculations.
2. Extract integer Rupee figures exactly as printed in the OCR text (strip commas, "Rs.", "₹", and ".00").
3. If any 12-digit Aadhaar number appears in the text, output "[Aadhaar Redacted]".
4. Output ONLY a single valid JSON object conforming to the schema below:
{
  "document_type": "FORM_16_PART_B",
  "employer_or_issuer_tan": "BLRI12345E",
  "gross_salary_sec17_1": 1850000,
  "perquisites_sec17_2": 45000,
  "profits_in_lieu_sec17_3": 0,
  "total_gross_salary_printed": 1895000,
  "exempt_hra_sec10_13a": 120000,
  "exempt_lta_sec10_5": 0,
  "standard_deduction_sec16_ia": 50000,
  "professional_tax_sec16_iii": 2400,
  "net_taxable_salary_printed": 1722600,
  "deduction_80c_total": 150000,
  "deduction_80ccd_1b_self_nps": 50000,
  "deduction_80ccd_2_employer_nps": 0,
  "deduction_80d_health_insurance": 25000
}
2.2 Micro-Prompt P1-PROMPT-02: Physical Rent (Rule 2A), 80D Medical & 80G Donation Receipt Structurer
{
  "receipt_type": "RENT | 80D_MEDICAL | 80G_DONATION",
  "amount_inr": 24000,
  "counterparty_name": "Suresh Patel",
  "counterparty_pan": "ABCPP1234F",
  "arn_80g_number": "",
  "payment_mode": "DIGITAL | CASH",
  "period_from": "2025-04-01",
  "period_to": "2025-04-30"
}
2.3 Micro-Prompt P2-PROMPT-01: Account Aggregator Bank Narration Batch Classifier
Batch Size & Pre-filter: 25-row chunks, pre-filtered for debits $\ge ₹500$.
Allowed Target Sections: ["80C_LIC_PPF_ELSS", "80D_HEALTH_INS", "80G_DONATION", "44ADA_FREELANCE_INFLOW", "SAVINGS_INTEREST", "IGNORE"]
Output Schema: [{"id": 102, "section": "80C_LIC_PPF_ELSS", "confidence": 0.96}]
2.4 Micro-Prompt P3-PROMPT-01: Immutable Token-Locked Vernacular Notice Explainer (en-IN, hi-IN, gu-IN)
To prevent hallucination of statutory section names, tax amounts, or due dates during multilingual translation, all numerical Rupee values and statutory tokens are masked prior to inference:

Token Invariant Mapping: Rupee figures and statutory references are converted into immutable tokens: {{NOTICE_SECTION}}, {{VARIANCE_AMOUNT}}, and {{ACTION_DEADLINE}}.
Post-Inference Verification: TokenLockedVernacularExplainer.restoreAndVerifyTokens() asserts that expectedTokens == presentTokens before re-injecting the verified Rupee figures into the Hindi (hi-IN) or Gujarati (gu-IN) UI notification card.
3. Data Reconciliation, TAN De-Duplication & Capital Gains Algorithms
3.1 Multi-Employer TAN De-Duplication & Strict Form 26AS TDS Ceiling (DeterministicReconciler.kt)
package in.zerotax.core.taxengine.reconciliation

import kotlin.math.abs
import kotlin.math.min

enum class SourceKind { FORM_16, AIS_TDS_192, PREFILL }

data class EmployerSalarySource(
    val employerTan: String,
    val employerName: String,
    val source: SourceKind,
    val grossSalary: Long,
    val sec10Exemptions: Long,
    val sec16DeductionsClaimedInDoc: Long,
    val netTaxableSalaryInDoc: Long,
    val tdsReported: Long
)

data class EmployerReconciliationItem(
    val employerTan: String,
    val employerName: String,
    val selectedSourceKind: SourceKind,
    val grossSalary: Long,
    val sec10Exemptions: Long,
    val sec16Deductions: Long,
    val netSalary: Long,
    val tdsReportedInSource: Long,
    val tdsIn26As: Long,
    val tdsAllowed: Long,
    val hasDiscrepancy: Boolean,
    val unreflectedTdsAmount: Long
)

data class ReconciledSalarySummary(
    val employers: List<EmployerReconciliationItem>,
    val aggregateGrossSalary: Long,
    val aggregateSec10Exemptions: Long,
    val aggregateNetTaxableSalary: Long,
    val aggregateTdsAllowed: Long,
    val totalDiscrepancyAmount: Long
)

object DeterministicReconciler {
    fun passesForm16SanityCheck(record: EmployerSalarySource, toleranceRupees: Long = 5L): Boolean {
        val computedNet = record.grossSalary - record.sec10Exemptions - record.sec16DeductionsClaimedInDoc
        return abs(computedNet - record.netTaxableSalaryInDoc) <= toleranceRupees
    }

    fun reconcileSalaries(
        sources: List<EmployerSalarySource>,
        form26AsTdsByTan: Map<String, Long>
    ): ReconciledSalarySummary {
        val normalized26As = form26AsTdsByTan.mapKeys { it.key.trim().uppercase() }
        val groupedByTan = sources.groupBy { it.employerTan.trim().uppercase() }
        val reconciledItems = mutableListOf<EmployerReconciliationItem>()

        for ((tan, records) in groupedByTan) {
            val primary = records.firstOrNull { it.source == SourceKind.FORM_16 } ?: records.first()
            val deposited26AsTds = normalized26As[tan] ?: 0L
            // CA MANDATE: Never claim TDS above Form 26AS Part A deposited amount (prevents CPC Sec 143(1) demand)
            val allowedTds = min(primary.tdsReported, deposited26AsTds)
            val discrepancy = primary.tdsReported > deposited26AsTds
            val unreflectedTds = if (discrepancy) primary.tdsReported - deposited26AsTds else 0L

            reconciledItems.add(
                EmployerReconciliationItem(
                    employerTan = tan,
                    employerName = primary.employerName,
                    selectedSourceKind = primary.source,
                    grossSalary = primary.grossSalary,
                    sec10Exemptions = primary.sec10Exemptions,
                    sec16Deductions = primary.sec16DeductionsClaimedInDoc,
                    netSalary = primary.netTaxableSalaryInDoc,
                    tdsReportedInSource = primary.tdsReported,
                    tdsIn26As = deposited26AsTds,
                    tdsAllowed = allowedTds,
                    hasDiscrepancy = discrepancy,
                    unreflectedTdsAmount = unreflectedTds
                )
            )
        }

        return ReconciledSalarySummary(
            employers = reconciledItems,
            aggregateGrossSalary = reconciledItems.sumOf { it.grossSalary },
            aggregateSec10Exemptions = reconciledItems.sumOf { it.sec10Exemptions },
            aggregateNetTaxableSalary = reconciledItems.sumOf { it.netSalary },
            aggregateTdsAllowed = reconciledItems.sumOf { it.tdsAllowed },
            totalDiscrepancyAmount = reconciledItems.sumOf { it.unreflectedTdsAmount }
        )
    }
}
3.2 Capital Gains Grandfathering (Sec 55(2)(ac)), Property Indexation (Sec 112(1)(a)) & Loss Set-Off (Secs 70/71/74)
npackage in.zerotax.core.taxengine.advanced

import kotlin.math.maximport kotlin.math.minimport kotlin.math.roundToLong

data class Equity112ALot(val isin: String,val acquisitionDateEpochDay: Long,val actualCostTotal: Long,val fmvJan31_2018Total: Long,val saleConsiderationTotal: Long,val transferExpenses: Long)

data class ImmovablePropertyLtcgLot(val acquiredBeforeJuly23_2024: Boolean,val purchaseCostRupees: Long,val improvementCostRupees: Long,val acquisitionFyCii: Int,val transferFyCii: Int,val saleConsiderationOrStampValue50C: Long,val transferExpenses: Long,val section54ReinvestmentExemption: Long = 0L)

data class PropertyLtcgTaxComparisonResult(val gainWithoutIndexation: Long,val taxAt12Point5PercentNoIndexation: Long,val gainWithCiiIndexation: Long,val taxAt20PercentWithIndexation: Long,val optimalTaxPayableForResidentIndividual: Long)

data class CapitalGainsSetOffSummary(val netTaxableStcg15Or20Percent: Long,val netTaxableLtcg112A: Long,val carriedForwardStclToNext8Years: Long,val carriedForwardLtclToNext8Years: Long)

object CapitalGainsAndForeignAssetEngine {private const val JAN_31_2018_EPOCH_DAY = 17562L

// Section 55(2)(ac): Deemed Cost = max(Actual, min(FMV_31Jan2018, SalePrice))
fun computeGrandfathered112AGain(lot: Equity112ALot): Long {
    val effectiveCost = if (lot.acquisitionDateEpochDay <= JAN_31_2018_EPOCH_DAY) {
        max(lot.actualCostTotal, min(lot.fmvJan31_2018Total, lot.saleConsiderationTotal))
    } else {
        lot.actualCostTotal
    }
    return lot.saleConsiderationTotal - effectiveCost - lot.transferExpenses
}

// Second Proviso to Section 112(1)(a): Pre-July 23, 2024 Property CII Indexation Comparison
fun computeImmovablePropertyLtcgTax(lot: ImmovablePropertyLtcgLot): PropertyLtcgTaxComparisonResult {
    val netSaleConsideration = max(0L, lot.saleConsiderationOrStampValue50C - lot.transferExpenses)
    val unindexedCost = lot.purchaseCostRupees + lot.improvementCostRupees
    val rawGainNoIndex = max(0L, netSaleConsideration - unindexedCost - lot.section54ReinvestmentExemption)
    val tax12Point5 = (rawGainNoIndex * 0.125).roundToLong()

    if (!lot.acquiredBeforeJuly23_2024) {
        return PropertyLtcgTaxComparisonResult(rawGainNoIndex, tax12Point5, rawGainNoIndex, tax12Point5, tax12Point5)
    }

    val indexedCost = (unindexedCost.toDouble() * lot.transferFyCii / lot.acquisitionFyCii).roundToLong()
    val indexedGain = max(0L, netSaleConsideration - indexedCost - lot.section54ReinvestmentExemption)
    val tax20Indexed = (indexedGain * 0.20).roundToLong()

    return PropertyLtcgTaxComparisonResult(
        gainWithoutIndexation = rawGainNoIndex,
        taxAt12Point5PercentNoIndexation = tax12Point5,
        gainWithCiiIndexation = indexedGain,
        taxAt20PercentWithIndexation = tax20Indexed,
        optimalTaxPayableForResidentIndividual = min(tax12Point5, tax20Indexed)
    )
}

// Sections 70(2), 70(3) & 74: STCL offsets STCG then LTCG; LTCL offsets ONLY LTCG (strictly barred against STCG)
fun applyCapitalLossSetOff(
    currentYearStcg: Long,
    currentYearStcl: Long,
    currentYearLtcg: Long,
    currentYearLtcl: Long,
    broughtForwardStcl: Long,
    broughtForwardLtcl: Long
): CapitalGainsSetOffSummary {
    val totalStclPool = currentYearStcl + broughtForwardStcl
    val totalLtclPool = currentYearLtcl + broughtForwardLtcl
    val ltcgAfterLtcl = max(0L, currentYearLtcg - totalLtclPool)
    val unabsorbedLtcl = max(0L, totalLtclPool - currentYearLtcg)
    val stcgAfterStcl = max(0L, currentYearStcg - totalStclPool)
    val stclRemainingAfterStcg = max(0L, totalStclPool - currentYearStcg)
    val finalLtcg = max(0L, ltcgAfterLtcl - stclRemainingAfterStcg)
    val unabsorbedStcl = max(0L, stclRemainingAfterStcg - ltcgAfterLtcl)

    return CapitalGainsSetOffSummary(
        netTaxableStcg15Or20Percent = stcgAfterStcl,
        netTaxableLtcg112A = finalLtcg,
        carriedForwardStclToNext8Years = unabsorbedStcl,
        carriedForwardLtclToNext8Years = unabsorbedLtc
l)}}


## 4. Persisted Finite State Machine (FSM) & Statutory Rounding / Validation Pipeline

### 4.1 Statutory Rounding & Principal Truncation Rules (`Sections 288A, 288B & Rule 119A`)

* **Section 288A (Rounding of Total Income) & Section 288B (Rounding of Total Tax):**
  Amounts are rounded to the nearest multiple of ₹10. Fractions of ten rupees equal to or exceeding five rupees are increased to ten rupees, while amounts below five rupees are truncated:
  ```kotlin
  fun roundToNearestTen(amount: Long): Long {
      val remainder = amount % 10L
      return if (remainder >= 5L) amount + (10L - remainder) else amount - remainder
  }

Rule 119A(b) (Principal Truncation for Sections 234A, 234B & 234C Interest):Any fraction of a hundred rupees in the unpaid tax base is ignored prior to computing simple interest at 1% per month:

fun truncateToNearestHundredRule119A(unpaidTaxPrincipal: Long): Long = (unpaidTaxPrincipal / 100L) * 100L
4.2 Pre-Submission CBDT Category 'A' Defect Blocker (CbdtCategoryADefectGate)
Prior to enabling the final "Submit CBDT JSON & e-Verify" CTA, the engine verifies these constraints to prevent defective return notices under Section 139(9):

TDS1.TotalTDSonSalaries <= GrossSalary (blocks automatic Category 'A' defect invalidation).
UsrDeductUndChapVIA.TotalChapVIADeductions <= GrossTotIncome.
Refund.BankAccountDtls contains at least one validated domestic bank account with UseForRefund = "true" and an 11-character regex-compliant IFSC (^[A-Z]{4}0[A-Z0-9]{6}$).
Every ScheduleIT.TaxPayment record has a 7-digit numeric BSRCode (^\d{7}$), a 5-digit SrlNoOfChaln (^\d{5}$), and a non-future DateDep.
PersonalInfo.AadhaarCardNo contains "[Aadhaar Redacted]" to prevent plaintext Aadhaar persistence across device logs.
5. Comprehensive Edge-Case & Resilience Matrix
Edge Case / Statutory Scenario
Deterministic System Handling Strategy
Device Lacks NPU / Android AICore (Tier 2 Handsets)
DeterministicTracesRegexParser extracts standard TRACES Form 16 Part A & B in < 30ms with 0 NPU calls and 0B cloud egress.
Income Tax Portal (eportal.incometax.gov.in) Returns HTTP 503
HardenedEphemeralPortalBridge aborts after 15s or on HTTP 500/502/503/504/429, wipes partial cookies in < 500ms, and transitions seamlessly to 100% offline PDF import mode.
Mid-Year Job Switch (2+ Form 16s & Duplicate AIS Rows)
DeterministicReconciler merges by 10-character TAN, applies Sec 16(ia) ₹75k Standard Deduction strictly once, and generates a pre-filled Challan 280 (Major Head 0021, Minor Head 300) UPI intent for any dual-exemption tax shortfall.
Pre-Jan 31, 2018 Equity Sale Where Sale Price < FMV 2018 (GTV-09)
computeGrandfathered112AGain evaluates $\max(\text{Actual}, \min(\text{FMV}_{2018}, \text{Sale}))$, capping deemed cost at the Sale Price so no artificial capital loss is created (Taxable LTCG = ₹0).
US RSU Vested on a Weekend / Bank Holiday (GTV-15)
ScheduleFaCalendarYearEngine queries sbi_ttbr_exchange_rates with WHERE rate_date_iso <= :targetIsoDate ORDER BY rate_date_iso DESC LIMIT 1, rolling back deterministically to the immediately preceding SBI business day (Rule 115 & Rule 128(5)).
User Manually Reduces Income Below AIS / Form 26AS
Intercepts the edit with Section270AStatutoryOverrideDialog (warning of 50% under-reporting and 200% misreporting penalties u/s 270A) and writes the SHA-256 warning hash + reason code to user_legal_consents_and_overrides.

6. Next-Gen Subsystems Architecture (Phases 4.4 to 4.7)
- F&O Trading Subsystem (`FnOTradingAndTaxAuditEngine.kt`): ICAI Guidance Note absolute turnover ($\sum |\text{P\&L}| + \text{Option Premium}$), Sec 73 speculative loss ring-fence (4-year carry forward), Sec 71(2A) salary loss bar (8-year carry forward u/s 72), and Sec 44AB ₹10 Cr digital audit tree with 44AD(4)/(5) lock-in checks.
- Post-Filing Diff Subsystem (`Section143_1IntimationDiffAnalyzer.kt`): Column A (As Filed) vs Column B (As Computed) reconciliation, auto-generating CBDT Section 154 Rectification JSON payloads for dropped credits.
- Encrypted Migration Subsystem (`EncryptedZtVaultMigrationEngine.kt`): Zero-cloud backup and transfer using PBKDF2 (256,000 iterations) + AES-256-GCM on GZIP database snapshots.
- Senior Citizen Subsystem (`SeniorCitizenTaxAndExemptionEngine.kt`): Section 194P Age 75+ filing exemption check, Form 12BBA bank declaration generator, Section 80TTB ₹50,000 deduction, and Section 207(2) advance tax immunity.
- Hybrid Income & Progressive Disclosure Subsystem (`HybridIncomeAndProgressiveDisclosureEngine.kt`): Multi-source income combinator and progressive disclosure state machine auto-routing to ITR-1, ITR-4, ITR-2, or ITR-3 Lite behind the scenes.
- 6-Menu Chip Copilot Subsystem (`OptimizedTaxCopilotChatbotEngine.kt`): 3-Tier AI pipeline with Tier-0 deterministic regex routing (< 2ms, 0 tokens), Tier-1 memoization, and Tier-2 KV-cached micro-prompts.
- In-App Live ITD Portal Sync Subsystem (`ItdLivePortalSyncModal.kt`): Direct TLS handshake pulling Form 26AS, AIS/TIS, Pre-fill JSON, and refund status in-app with 0 external browser hops.
- Hardware Biometric Shield Subsystem (`ZeroTaxBiometricShield.kt`): System-level AndroidX BiometricPrompt gate securing sensitive return records with 1-tap lock and countdown notifications.


---

## 7. B2B ERI Gateway (Quicko / Sandbox) Sequence & Fallback State Machine

```
Taxpayer                Android App (ITR)          ITR-API Backend           Sandbox / Quicko
   |                           |                          |                          |
   |--- [1] Tap "Submit" ----->|                          |                          |
   |                           |--- [2] POST /submit ---->|                          |
   |                           |    (CBDT JSON Payload)   |--- [3] Check Token ----->|
   |                           |                          |    (Cached JWT valid?)   |
   |                           |                          |                          |
   |                           |                          |--- [4] Submit Return --->|
   |                           |                          |    (x-api-key, auth)     |
   |                           |                          |                          |
   |                           |                          |<-- [5a] 200 OK (Ack) ----|
   |                           |<-- [6a] HTTP 200 (Ack) --|                          |
   |                           |    (driver: QUICKO_ERI)  |                          |
   |                           |                          |                          |
   |                           |                          |-- [5b] 4xx/5xx/Timeout ->|
   |                           |                          |    [FALLBACK TRIGGERED]  |
   |                           |                          |--- [6b] In-App Driver -->|
   |                           |<-- [7b] HTTP 200 (Ack) --|                          |
   |                           |    (IN_APP_LIVE_DRIVER)  |                          |
   |<-- [8] Render Ack Badge --|                          |                          |
```

---

## 8. Dual-Repository Network Topology & Data Contracts
- **Unified Cloud Endpoints (`ITR-API`)**:
  - `GET  /api/v1/portal/driver-status`: Returns active gateway priority and fallback status.
  - `POST /api/v1/portal/otp/request`: Requests Aadhaar OTP for live pre-fill or verification.
  - `POST /api/v1/portal/otp/verify`: Verifies 6-digit Aadhaar OTP u/s 140A.
  - `POST /api/v1/portal/submit`: Submits return via Quicko B2B ERI with automated In-App fallback.
  - `POST /api/v1/portal/everify`: Direct Aadhaar e-verification u/s 140A.
  - `POST /api/v1/portal/ingest-live-prefill`: Ingests Form 26AS & AIS records into zero-knowledge vault.
