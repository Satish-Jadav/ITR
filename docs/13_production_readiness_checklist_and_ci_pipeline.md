13 — Production Readiness Checklist, R8 Security Hardening & GitHub Actions CI/CD Pipeline (CA & Legal Counsel Sign-Off Edition)
1. GitHub Actions CI/CD Pipeline Specification (.github/workflows/zerotax-ci.yml)
The automated build pipeline runs across every pull request and release branch to guarantee zero regressions in tax math (GTV-01..18 and ECV-01..06), strict adherence to the pure-JVM :core:tax-engine architectural firewall, UIDAI Aadhaar redaction verification, and static analysis security checks.

name: ZeroTax-Local Production CI Pipeline

on:
  push:
    branches: [ main, release/* ]
  pull_request:
    branches: [ main ]

permissions:
  contents: read

jobs:
  validate-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Verify Pure-JVM Architectural Firewall in :core:tax-engine
        run: |
          if grep -rnE "import (android\.|androidx\.|com\.google\.ai\.edge\.aicore)" core/tax-engine/src/main/; then
            echo "FATAL: Android SDK or AICore dependency detected inside pure-JVM :core:tax-engine!"
            exit 1
          fi

      - name: Run Pure Kotlin Tax Engine JUnit 5 Suite (GTV-01..18 & ECV-01..06 — ₹0.00 Variance Gate)
        run: ./gradlew :core:tax-engine:test --info

      - name: Run UIDAI Aadhaar Verhoeff Scrubber & Canonical Warning Hasher Security Tests
        run: ./gradlew :core:security:testDebugUnitTest --info

      - name: Verify Zero Network Leakage in Local PDF Parser & AI Pipeline
        run: ./gradlew :feature:ai-extractor:lintDebug :feature:pdf-unlocker:lintDebug

      - name: Build Encrypted SQLCipher 4.5.x Room Database & Verify Schema Export
        run: ./gradlew :core:database:assembleDebug :core:security:assembleDebug

      - name: Execute Detekt Static Code Analysis (Strict OWASP MASVS Rules)
        run: ./gradlew detekt

      - name: Compile Release Signed App Bundle (AAB) with R8 Full Mode
        env:
          SIGNING_KEY_STORE_PASSWORD: ${{ secrets.KEY_STORE_PASSWORD }}
          SIGNING_KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          SIGNING_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew :app:bundleRelease

2. ProGuard / R8 Obfuscation & Security Rules (proguard-rules.pro)

# ==============================================================================
# 1. SQLCIPHER 4.5.x NATIVE JNI BINDINGS & ROOM ENTITIES
# ==============================================================================
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class in.zerotax.core.database.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# ==============================================================================
# 2. HARDENED EPHEMERAL WEBVIEW JAVASCRIPT BRIDGE
# ==============================================================================
-keepclassmembers class in.zerotax.feature.portalbridge.HardenedEphemeralPortalBridge$PortalJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# ==============================================================================
# 3. PDFBOX-ANDROID & BOUNCYCASTLE CRYPTOGRAPHIC PROVIDERS (15-PERM UNLOCKER)
# ==============================================================================
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ==============================================================================
# 4. ANDROID AICORE (GEMINI NANO ON-DEVICE NPU RUNTIME)
# ==============================================================================
-keep class com.google.ai.edge.aicore.** { *; }

# ==============================================================================
# 5. STRIP ALL VERBOSE/DEBUG/INFO/WARN LOGGING IN PRODUCTION RELEASE BUILDS
#    Prevents accidental PII, PAN, TAN, or financial figure leakage in logcat
#    (DPDP Act 2023 Sec 8(5) & OWASP MASVS-STORAGE-2 Compliance)
# ==============================================================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

3. Production Deployment & CA / Legal Counsel Sign-Off Checklist
Google Play Store Data Safety & DPDP Act Section 6 Audit: Declare explicitly in the Google Play Data Safety form that financial documents (Form 16, Form 26AS, AIS/TIS, CAMS/KFintech CAS, Zerodha/Groww P&L, E*TRADE/Schwab RSU statements) are processed 100% on-device and never uploaded to remote servers. Verify that itemized Section 6 consent notices render accurately in English (en-IN), Hindi (hi-IN), and Gujarati (gu-IN) prior to Tap 1 vault creation.
Non-ERI Client-Side Browser Assistant Verification (Section 139(1B)): Verify via packet capture (mitmproxy / Wireshark test harness) that HardenedEphemeralPortalBridge connects solely to allowlisted official CBDT/TRACES hosts (eportal.incometax.gov.in, www.incometax.gov.in, contents.tdscpc.gov.in, ais.insight.gov.in), transmits zero packets to any intermediate proxy, and executes CookieManager.removeAllCookies + WebStorage.deleteAllData within 500ms of payload capture.
Hardware Keystore StrongBox & SQLCipher 4.5.x Verification: Test key generation routines on physical devices supporting Keymaster 4.1+ / StrongBox (Google Pixel 8/9, Samsung Galaxy S23/S24) and fallback TEE devices to verify hardware-isolated AES-256-GCM key wrapping alongside PRAGMA cipher_memory_security = ON; and PRAGMA secure_delete = ON;.
DPDP Act Section 12(3) Right to Erasure ("Shred Local Tax Vault") Verification: Verify via automated instrumentation test that invoking DpdpVaultShredder.shredAllLocalTaxData() deletes Keystore alias zero_tax_sqlcipher_master_key_v1, overwrites zero_tax_vault.db, -wal, -shm, and -journal with SecureRandom bytes (rws sync), and resets the app to Tap 1 in < 300ms.
UIDAI Aadhaar Act Section 29 Zero-Storage Verification: Verify that UidaiAadhaarVerhoeffScrubber intercepts all 12-digit Verhoeff-valid Aadhaar strings across OCR, WebView JSONs, and Room DAOs, replacing them with "[Aadhaar Redacted]", and that Table 1's SQLite CHECK (aadhaar_masked_token = '[Aadhaar Redacted]') constraint rejects raw digits.
RBI Account Aggregator Ephemeral RAM Zeroization Audit (ECV-05): Verify via Android Studio Memory Heap dump inspection that sharedSecretBytes, saltXorNonce, derivedKey, prk, okm, and plaintextBytes inside RbiAaEphemeralDecryptor.decryptAndExtractInterestInMemory() are 100% zero-filled (0x00) upon exiting the finally block.
Post-AI Math Sanity Firewall & Visual Disambiguation Audit (GTV-08): Confirm that Gemini Nano runs strictly at temperature = 0.0f and topK = 1, and that any Form 16 Part B extraction where |Gross - Exemptions_u10 - Deductions_u16 - Net_Salary| > ₹5 blocks auto-ingestion and triggers OneTapPdfDisambiguationCard with the cropped PDF region bitmap.
Statutory Penalty Shield & Override Audit Trail (Section 270A & Black Money Act): Verify that any downward modification of a Form 26AS / AIS verified income figure triggers Section270AStatutoryOverrideDialog, and that any foreign asset detection triggers BlackMoneyActForeignAssetModal (warning of the ₹10,00,000 penalty under Sections 42 & 43), logging the SHA-256 warning digest into Table 13 (user_legal_consents_and_overrides).
Pre-Submission CBDT Category 'A' Defect Gate (Section 139(9)): Confirm that CbdtItr1JsonCompiler and CbdtCategoryADefectGate block JSON export if TotalTDSonSalaries > GrossSalary, TotalChapVIADeductions > GrossTotalIncome, ScheduleBA lacks a valid 11-character IFSC (^[A-Z]{4}0[A-Z0-9]{6}$) with UseForRefund = "true", or Challan 280 entries lack a 7-digit BSR code and 5-digit serial number.
Airplane-Mode Zero-Egress & Vernacular Token-Locking Gate (ECV-06): Execute the end-to-end Phase 1–3 test suite with device networking disabled (Airplane Mode ON) and verify 0 outbound network packets and 100% byte-for-byte preservation of {{NOTICE_SECTION}}, {{VARIANCE_AMOUNT}}, and {{ACTION_DEADLINE}} tokens across Hindi (hi-IN) and Gujarati (gu-IN) translations.

4. Annual Finance Act & CBDT Schema Rollover Runbook (April 1 Statutory Cutover)
Before April 1 of each new Assessment Year, the Principal Android Architect and Chartered Accountant Domain Lead must execute the following 5-step statutory rollover checklist:
Finance Act Slab & Rebate Parameter Update: Add the new AssessmentYearProfile enum constant in DeterministicIndianTaxEngine.kt, configuring the updated Section 115BAC(1A) progressive slabs, Section 87A rebate ceiling and marginal relief threshold, Section 16(ia) standard deduction, and surcharge caps as enacted by the Union Budget / Finance Act.
CBDT Cost Inflation Index (CII) Notification Seed Update: Insert the newly notified CBDT S.O. Cost Inflation Index integer for the relevant Financial Year into cost_inflation_index_cii (Table 7 in 05_database_schema_and_on_device_ai_prompts.md) to enable Second Proviso to Section 112(1)(a) property grandfathering comparisons.
Offline 10-Year SBI TTBR Database Refresh: Append the preceding Calendar Year's daily and month-end State Bank of India Telegraphic Transfer Buying Rates (TTBR) for USD, EUR, GBP, SGD, CAD, AUD, and CHF into sbi_ttbr_exchange_rates (Table 10), verifying weekend/holiday rollback queries for Schedule FA Table A3 (Rule 115) and Form 67 (Rule 128(5)).
CBDT JSON Schema Version & Validation Rules Sync: Update SchemaVer and node mappings in CbdtItr1JsonCompiler, CbdtCategoryADefectGate, and Form67AndItr2ScheduleCompiler against the official CBDT JSON Schema and Validation Rules document published on incometax.gov.in for ITR-1, ITR-2, ITR-4, and ITR-U.
Zero-Variance Golden Vector Sign-Off: Run ./gradlew :core:tax-engine:test across all 18 Golden Test Vectors (GTV-01..18) and 6 Edge-Case Vectors (ECV-01..06) against the official CBDT offline utility to certify ₹0.00 variance prior to Play Store rollout.

5. Final Release Sign-Off Certificate (CA Domain Lead, Fintech Legal Counsel & Principal Android Architect)

Sign-Off Authority
Scope of Certification
Status
Chartered Accountant (Indian Direct Tax Lead)
Certified 100% mathematical & statutory compliance across AY 2025-26 & AY 2026-27 progressive slabs, Sec 87A rebate & marginal relief, Sec 112A(6) special-rate carve-out, Sec 55(2)(ac) Jan 31 2018 grandfathering, Second Proviso to Sec 112(1)(a) property indexation (CII 376), Secs 70/71/71(3A)/74 loss matrix, Rule 2A HRA, Rule 115 / Rule 128(5) SBI TTBR foreign asset & Form 67 FTC conversions, Sec 139(8A)/140B ITR-U, and ₹0.00 variance across GTV-01..18 & ECV-01..06.
APPROVED FOR PRODUCTION
Fintech Regulatory & Privacy Counsel
Certified full compliance with Section 139(1B) Non-ERI Client-Side Browser Assistant positioning, DPDP Act 2023 Sections 6, 8(5), 12(3) & 13 (en-IN, hi-IN, gu-IN itemized notices + <300ms StrongBox vault shredder), RBI NBFC-AA Master Directions (0x00 RAM zeroization), UIDAI Aadhaar Act Section 29 Verhoeff redaction ("[Aadhaar Redacted]"), and Section 270A / Black Money Act / Section 140 audit logging.
APPROVED FOR PRODUCTION
Principal Android Architect & Staff TPM
Certified strict multi-module Gradle DAG isolation (zero Android/LLM imports in :core:tax-engine), SQLCipher 4.5.x + StrongBox AES-256-GCM encryption-at-rest, FLAG_SECURE screen protection, R8 full-mode obfuscation & log stripping, and automated GitHub Actions CI enforcement.
APPROVED FOR PRODUCTION



## 6. User-Friendliness, Proactive Guidance & Error-Proofing Audit Controls
To eliminate taxpayer friction, confusion, and accidental non-compliance, ZeroTax-Local incorporates five proactive user-friendliness gates:

1. **Real-Time PAN 4th-Character Entity Decoder (`PanEntityDecoder`)**:
   - Immediately decodes the 4th character ('P' for Individual, 'H' for HUF, 'C' for Company, 'F' for Firm, 'T' for Trust).
   - Dynamically badges form eligibility (ITR-1 / ITR-2 / ITR-4) and verifies Section 87A rebate applicability (individuals only; HUFs/corporates excluded).
   - Warns users entering non-individual PANs that ZeroTax-Local is dedicated to personal and presumptive filings before they invest time in data entry.

2. **Age Bracket & Senior Citizen Slab Exemption Selector (`CitizenCategoryAgeSelector`)**:
   - Direct toggling of age categories (<60 Regular, 60–79 Senior Citizen, 80+ Super Senior Citizen).
   - Dynamically updates the statutory basic exemption limit in Old Regime (₹2,50,000 / ₹3,00,000 / ₹5,00,000) and displays verified Section 80D & Section 80TTB enhanced interest deduction limits (up to ₹50,000).

3. **Institution PDF Password Registry & Cheatsheet (`InstitutionPdfPasswordRegistry`)**:
   - Built-in cheatsheet of official deterministic password patterns across TRACES Form 16, ITD AIS/TIS, e-Aadhaar, HDFC Bank, SBI, ICICI Bank, Axis Bank, Kotak, and CAMS/KFintech CAS.
   - Eliminates failed password attempts and user frustration during document import.

4. **Interactive 5-Point Pre-Submission Safety Checklist (`PreSubmissionSafetyChecklistCard`)**:
   - 5 mandatory verification gates before final return transmission:
     1. Direct Refund Bank Account (valid 11-char IFSC, verified ECS status).
     2. Aadhaar-PAN Linkage acknowledgment u/s 139AA.
     3. Section 205 Employer TDS Discrepancy protection.
     4. Mandatory Landlord PAN verification (if rent > ₹1,00,000 u/s Circular 08/2013).
     5. Schedule FA / Black Money Act foreign asset disclosure confirmation.

5. **Section 87A Marginal Relief Visualizer (`Section87AMarginalReliefExplainerCard`)**:
   - Transparently calculates and explains marginal relief under Section 115BAC proviso for incomes between ₹7,00,000 and ₹7,27,777.
   - Proves mathematically to taxpayers how the proviso prevents the cliff-edge tax spike, saving up to ₹25,000 in excess tax liability.

6. **Plan-Based Usability Tier Engine (`FilingTierMode` & `FilingTierSelectorCard`)**:
   - Auto-detection across three taxpayer archetypes:
     - `SAHAJ_SIMPLE` (Pure salaried and pensioners under ₹20L with single Form 16 and interest income). Provides a distraction-free, 1-tap fast-track refund filing experience.
     - `INVESTOR_PRO` (Stocks, mutual funds, capital gains 111A/112A, presumptive freelance consultants u/s 44ADA, and deductions optimizer).
     - `ENTERPRISE_AUDIT` (HNIs with gross income > ₹50L, turnover > ₹1 Cr, Schedule FA foreign assets, Section 44AB CA audit shield, and 3-way discrepancy radar).
   - Allows users to switch views with 1 tap, ensuring simple filers are never overwhelmed by audit forms while high-net-worth individuals retain audit-grade tools.

7. **Universal Tax Payment & Challan 280 Hub (`TaxPaymentAndChallanHubModal`)**:
   - Multi-channel statutory payment gateway supporting UPI Instant (GPay / PhonePe / Paytm / BHIM) with zero surcharge, NetBanking, Debit Card, RTGS/NEFT Virtual Account mandates for large tax payments (> ₹1 Lakh), and Over-the-Counter bank slips.
   - Handles all statutory tax liability conditions: Advance Tax (Minor Head 100 u/s 208) with installment calendar, Self-Assessment Tax (Minor Head 300 u/s 140A), and Regular Assessment Demand (Minor Head 400 u/s 143(1)/144).
   - Integrated Challan 280 validator verifying 7-digit BSR code, 5-digit Challan Serial Number, and deposit date with auto-injection into Schedule IT of the return.

8. **Complete Chapter VI-A Statutory Deductions Directory (`SmartDeductionsCatalog`)**:
   - Full statutory coverage of all 16 Chapter VI-A deduction codes: 80C, 80CCC, 80CCD(1), 80CCD(1B), 80CCD(2), 80D, 80DD, 80DDB, 80E, 80EEA, 80EEB, 80G, 80GG, 80GGA, 80GGC, 80TTA, 80TTB, and 80U.
   - Actionable instructions for every deduction: issuing authorities (CMO, research associations, banks, insurers), statutory limits, and required document forms (Form 10-I, Form 10-IA, Form 16A, RPwD certificates).

9. **Dual Authentication & Privacy-First Vault Access (`ZeroTaxAuthSecurityManager` & `AuthenticationScreen`)**:
   - Offers Google Sign-In, Email & Password (with PBKDF2/SHA-256 salted hashing), and 100% Offline Local Vault access.
   - Strictly conforms to DPDP Act 2023 Section 12(3) Right to Erasure, storing zero unencrypted credentials in plaintext and allowing complete one-tap vault shredding in <300ms.

10. **Interactive Section 6 Residential Status Day-Counter (`ResidentialStatusAndFamilyArbitrageEngine` — Feature F-01)**:
    - Automatically calculates days in India across current and preceding 4–10 financial years to determine ROR, RNOR, or Non-Resident status.
    - Accurately shields returning NRIs under the RNOR status, legally exempting foreign assets and foreign dividends/capital gains from Indian tax and Schedule FA Black Money Act penalties.

11. **Family & Spouse Tax Arbitrage Simulator (`ResidentialStatusAndFamilyArbitrageEngine` — Feature F-02)**:
    - Simulates legitimate family tax-saving structures: paying rent to retired parents u/s 10(13A), paying senior parent health insurance u/s 80D up to ₹50,000, and gifting capital to adult children (18+) or parents for high-yield FDs without triggering Section 64 clubbing.

12. **Section 244A Refund Interest Auditor & Freelancer GST Radar (`RefundInterest244AAndGstAuditor` — Features F-05 & F-08)**:
    - Audits Section 143(1) intimations to verify CPC Bengaluru has paid the statutory 0.5% per month (6% p.a.) simple refund interest from April 1 of the AY, auto-generating a Section 154 rectification request if short-paid.
    - Monitors cumulative gross receipts against the ₹20 Lakh GST registration threshold and tracks foreign export inward remittances (e-FIRA / LUT) to guarantee zero export tax exposure.

13. **Schedule AL Auto-Builder & Section 50C Safe-Harbor Shield (`ScheduleAlAndCapitalGainsWealthEngine` — Features F-03 & F-09)**:
    - Auto-generates statutory asset and liability balance sheets for taxpayers whose total income exceeds ₹50 Lakhs (Schedule AL).
    - Assesses real estate transfers under the Third Proviso to Section 50C(1), verifying if stamp duty circle rate is within 110% of actual agreement value to protect against arbitrary deemed capital gains assessments.

14. **CBDT Rule 26C Form 12BB Proof Exporter (`LocalStatutoryPdfGenerators` — Feature F-04)**:
    - Generates valid, bank-grade PDF 1.4 documents containing verified HRA, LTA, Section 24(b) home loan interest, and Chapter VI-A investment deductions.
    - Completely on-device generation with zero network transit, ready for payroll/HR submission before employer TDS cut-off deadlines.

15. **Section 197A / Rule 29C Form 15G & 15H Exporter (`LocalStatutoryPdfGenerators` — Feature F-07)**:
    - Generates pre-filled Form 15H (senior citizens age >= 60) and Form 15G (< 60) zero-TDS declarations for fixed deposits.
    - Formatted to statutory specifications for April 1 submission to bank branches, preventing unnecessary 10% TDS deductions on low-income fixed deposit interest.

16. **100% Offline Natural-Language "What-If" Scenario Simulator (`OfflineWhatIfTaxSimulator` — Feature F-15)**:
    - Instant (< 5ms) deterministic evaluation of hypothetical tax scenarios (e.g. "Contribute 50k in NPS", "Buy 25k health insurance", "Earn 5L freelance 44ADA").
    - Pure on-device calculation without AI server dependency or latency, providing instantaneous tax delta figures and statutory guidance.

17. Next-Gen Production Readiness & Multi-Engine Verification Sign-Off (Phases 4.4 to 4.7)
17.1 Non-Individual Corporate & Trust Entities Suite (ITR-5, ITR-6, ITR-7):
- Complete statutory implementation of Section 40(b) partner remuneration/interest, Section 115JC AMT, Section 115BAA/BAB corporate concessions, Section 115JB MAT book profit additions, and Section 11/12 trust application (85% rule) with Form 9A/10 declarations.
17.2 Plan-Based Separation & Interactive Coachmark Walkthrough:
- Segmented into SAHAJ (ITR-1 plain language), SUGAM (ITR-4 presumptive deck), and ENTERPRISE AUDIT (ITR-2 to ITR-7 executive desk), augmented with 4-step coachmark tour.
17.3 Hardware Biometric Security Gate & 1-Tap App Lock:
- AndroidX BiometricPrompt securing user returns, PAN, and bank data upon cold start and foreground resume, featuring instant "Lock" button in the top header.
17.4 Statutory Tax Timeline & Push Notifications:
- July 31 deadline countdown pill ("86 Days remaining") and 1-tap local notification scheduler for filing start, 30-day reminder, 6-day urgent penalty warning u/s 234F, and belated cutoff (Dec 31).
17.5 In-App Live ITD Portal Sync Suite:
- Direct TLS handshake via Aadhaar OTP or password pulling Form 26AS, AIS/TIS, Pre-fill JSON, and refund status in-app with 0 external browser hops.
17.6 F&O Trading ICAI Turnover & Tax Audit Engine (Section 31):
- ICAI Guidance Note turnover formula ($\sum |\text{P\&L}| + \text{Option Premium}$), Section 73 speculative ring-fence (4-year carry forward), Section 71(2A) salary loss bar (8-year carry forward u/s 72), and Section 44AB ₹10 Crore digital threshold with 44AD(4)/(5) lock-in checks.
17.7 Section 143(1) Intimation Diff Analyzer & Section 154 Rectification Builder (Section 33.1):
- Column A vs Column B line item reconciliation flagging zero discrepancy, credit mismatches, and income omissions, auto-generating CBDT Section 154 JSON payloads.
17.8 Zero-Knowledge .ztvault Encrypted Device Migration (Section 33.3):
- PBKDF2 (256,000 iterations) + AES-256-GCM encrypted backup and Quick Share transfer for 8-year CFL loss ledgers and return archives with 100% zero-server privacy.
17.9 Senior Citizen Section 194P Exemption & Form 12BBA Generator (Section 34):
- Evaluates Age 75+ resident pensioners with single-bank interest, exempting them from ITR filing u/s 139 via Form 12BBA, alongside Section 80TTB ₹50,000 deduction and Section 207(2) advance tax immunity.
17.10 Hybrid Multi-Source Income & Progressive Disclosure Engine (Section 38):
- Synthesizes Salaried + Moonlighting + Freelancing (44ADA 50%) + Creator (44AD 6%/8%) + Casual gigs (Sec 56), dynamically showing only relevant cards and auto-routing behind the scenes.
17.11 6-Menu Chip-Driven Copilot Chatbot Engine (Section 35):
- Guided quick-pick action pills eliminating empty text inputs, backed by Tier-0 deterministic regex routing (< 2ms, 0 tokens), Tier-1 memoization, and Tier-2 KV-cached micro-prompts.
