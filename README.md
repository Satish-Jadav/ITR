# ZeroTax-Local — 100% On-Device Deterministic Indian Tax Filing & Intelligence Suite

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Android SDK](https://img.shields.io/badge/Android%20SDK-35%20(VanillaIceCream)-green.svg)](https://developer.android.com)
[![JDK](https://img.shields.io/badge/JDK-17-blue.svg)](https://openjdk.org)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device%20(0B%20Egress)-brightgreen.svg)](#privacy--statutory-compliance)
[![Tax Engine](https://img.shields.io/badge/Tax%20Engine-Pure--JVM%20Deterministic-orange.svg)](#core-tax-engine-zero-llm-math-policy)
[![CBDT Forms](https://img.shields.io/badge/CBDT%20AY%202026--27-ITR--1%20%7C%20ITR--2%20%7C%20ITR--4%20%7C%20ITR--U-teal.svg)](#supported-forms--schedules)

> **ZeroTax-Local** is a privacy-first, on-device Android application designed to prepare, audit, optimize, and directly file Indian Income Tax Returns (ITR-1, ITR-2, ITR-4, and ITR-U) for **AY 2026-27 (FY 2025-26)** with **zero cloud uploads**, **zero spam calls**, and **100% deterministic pure-JVM tax calculations**.

---

## 🏆 Key Features & Architectural Capabilities

### 1. 🤖 TaxBuddy-Style Conversational Filing Chatbot
- **Interactive Natural-Language Ingestion:** Chat naturally to share your salary, job switches, home loan EMIs, health insurance, NPS, and bank accounts without dealing with complicated tax schedules.
- **In-Flight Tax Q&A Interception:** Ask questions like *"Which regime is better?"*, *"Can I claim both HRA and Home Loan?"*, *"What are the 80D limits?"*, or *"How to save more tax?"* and receive instant CA-grade answers with statutory citations.
- **Live Dual-Regime Optimizer:** Concurrently calculates New Regime (Section 115BAC) and Old Regime after every entry, highlighting the winning regime and exact rupee savings.
- **Mandatory Statutory Gate u/s 140:** Direct submission to the portal from chat is strictly prohibited. The conversation locks into an awaiting-review state requiring the taxpayer's full review before submission.

### 2. 🛡️ 6-Gate Pre-Filing Audit & Notice Risk Shield
- **100% CPC Bengaluru Algorithmic Alignment:** Benchmarked against market leaders (JioFinance, ClearTax, TaxBuddy, EZTax) to achieve `< 0.1%` scrutiny notice risk.
- **The 6 Verification Gates:**
  1. *TDS Reconciliation & 26AS Part A Ceiling:* Reconciles Form 16 TDS against deposited Form 26AS credits.
  2. *High-Value AIS / SFT Cross-Check:* Identifies unreported interest and dividend receipts.
  3. *Section 115BAC vs. Chapter VI-A Deduction Verification:* Enforces statutory deduction rules.
  4. *Sections 288A & 288B Rounding Invariant:* Asserts nearest ₹10 statutory rounding.
  5. *Direct Refund Bank Pre-Validation:* Verifies that designated refund bank accounts are active for ECS credit.
  6. *Deductions Cap vs. Gross Total Income:* Asserts Section 80A(2) compliance.

### 3. 🌐 Direct In-App ITD Portal Filing Suite & Instant Aadhaar OTP e-Verification
- **Section 139(1) Direct Submission:** Transmits schema-compliant CBDT JSON directly from the device to the official ITD gateway (`https://eportal.incometax.gov.in`) over hardware TLS in `< 30 seconds`.
- **Instant Aadhaar OTP e-Verification u/s 140A:** Fetches OTP and completes verification entirely within the app, generating the official 15-digit ITR-V acknowledgement number without requiring external browser logins.
- **Ephemeral Session Security:** Auth tokens and session cookies are cryptographically wiped from RAM within 500ms of transaction completion per DPDP Act Section 12(3).

### 4. 🛠️ Smart Tax Toolkit (6 Value-Add Industry Tools)
- **HRA Exemption Maximizer & Rent Receipt Generator (Rule 2A):** Real-time Least-of-3 calculation (Actual HRA, Rent minus 10% Basic, 50%/40% Metro cap) with mandatory Landlord PAN alerts (>₹1L/year) and 1-tap 12-month revenue-stamped rent receipt pack generation for employer HR.
- **CTC Salary Restructuring & Flexi-Benefits Optimizer (Section 80CCD(2), Rule 3):** Simulates employer corporate NPS up to 14% of Basic (exempt in BOTH New and Old Regimes), broadband/telecom (Rule 3(7)(ix)), books & periodicals (Sec 10(14)(i)), and food coupons (Rule 3(7)(iii)) to save ₹30,000 to ₹1,50,000+ in taxes with 1-tap HR payroll proposal letter generation.
- **Multi-Broker Capital Gains & 8-Year Loss Carry-Forward Ledger (Schedule CG, CYLA & CFL):** Computes Finance (No. 2) Act 2024 rates (STCG 111A @ 20%, LTCG 112A @ 12.5% >₹1.25L), intra-head set-off u/s 70/71, and 8-year historical loss bucket tracking with Section 139(1) timely filing verification.
- **Advance Tax Radar & Instant Challan 280 UPI Assistant (Sections 208, 234B, 234C):** Monitors the ₹10,000 net liability threshold, quarterly milestone countdowns (15%, 45%, 75%, 100%), interest penalty avoidance calculator, and pre-filled Challan 280 (Major Head 0021, Minor Head 100/300) with NPCI UPI intent strings.
- **March 31 Equity LTCG Tax-Harvesting Radar (Section 112A):** Tracks annual ₹1,25,000 tax-free LTCG headroom, automated profit-booking recommendations before March 31, and STCG short-term loss harvesting.
- **Bank-Ready "Statement of Total Income" Exporter:** Formats formal bank-grade ITR summaries with tamper-evident SHA-256 seal for Home Loans, Car Loans, and Visa applications.

### 5. 📊 Tax Intelligence & Visual Analytics Suite
- **Income Flow Waterfall Visualizer ("Where Did My Money Go?"):** Visual transparency from Gross Earnings down to Standard Deduction, Chapter VI-A relief, Net Taxable Income, Section 87A rebate, and Direct Bank Refund.
- **3-Way Discrepancy Triangulation Radar:** Side-by-side comparison across Employer Form 16, TRACES Form 26AS, and ITD AIS/TIS with 1-tap auto-healing to eliminate Section 143(1)(a) tax demand notices.
- **Statutory Tax Calendar & Compliance Reminders:** Live countdown timers for the July 31 filing deadline and all quarterly compliance milestones.
- **Multi-Year Historical ITR Dossier:** 3-Year comparative trajectory of Gross Income, Taxes Paid, and Refunds Credited with Year-on-Year growth tracking.

### 6. 🛡️ Tax Notice Defense & CPC Rectification Assistant
- **Automated CPC Bengaluru Notice Scanner:** Covers Section 143(1) Tax Demand / Discrepancy notices, Section 139(9) Defective Returns, Section 245 Past Refund Adjustments, and Section 148A High-Value Inquiry notices.
- **Statutory Deadline Tracking & Penalty Shield:** Alerts taxpayers to statutory windows (15 to 30 days) to prevent automatic Section 220(2) penal interest and Section 221 penalty orders.
- **1-Tap CBDT-Compliant Written Submission Generator:** Generates legally structured formal written replies quoting supreme court precedents, CBDT circulars, and Form 26AS annexures ready for e-Proceedings / Section 154 online rectification.

### 7. 🏢 Small Business Tax Audit & Presumptive Suite
- **Section 44AB Mandatory Tax Audit Analyzer:** Evaluates turnover thresholds—standard ₹1 Crore limit vs enhanced ₹10 Crore limit (under the Proviso to Section 44AB(a) for cash receipts and cash payments $\le$ 5%).
- **Section 44AD & 44ADA Presumptive Calculator:** Validates minimum deemed profits (6% digital, 8% cash for eligible businesses up to ₹3 Crores; 50% for specified professionals up to ₹75 Lakhs) with Section 44AD(4) 5-year lock-in debarment warnings.
- **Form 3CD Statutory Disallowances Pre-Audit Scanner:** Flags and adds back Section 43B(h) MSME overdue payments (>45 days), Section 40A(3) single-day cash expenses (>₹10,000), Section 40(a)(ia) 30% TDS non-deduction, and Section 43B unpaid statutory liabilities.
- **Chartered Accountant Handover Pack:** 1-tap exports a structured pre-audit compliance dossier (trial balance, 5% cash ratio, disallowances) to streamline CA tax audit sign-off and shield small business clients against Section 271B penalties.

### 8. 🌍 Dual-Region NRI & Cross-Border Tax Suite
- **Section 6 Physical Presence Calculator:** Deterministically evaluates the 182-day, 60-day (+365 days in 4 years), and 120-day (visiting citizens with Indian income > ₹15L) rules, plus Section 6(1A) deemed residency to classify taxpayers as ROR, RNOR, or Non-Resident Indian (NRI).
- **100% Foreign Income Exclusion:** Automatically exempts global/foreign-sourced income from Indian taxation for NRIs and RNORs.
- **Section 10(4)(ii) Tax-Free Interest Vault:** Grants 100% tax exemption on interest earned in Non-Resident External (NRE) savings/FD accounts and FCNR foreign currency deposits.
- **NRO 30% TDS Reclaimer & DTAA Section 90 Relief:** Reconciles Section 195 withholding on NRO accounts against domestic slab rates and generates electronic **Form 10F** self-declarations for claiming beneficial bilateral treaty rates.

### 9. 📋 Dual-Regime & Form 10-IEA Business Governance
- **Statutory Regime Switching under Section 115BAC(3):** Salaried filers switch regimes directly in ITR-1/2, while business/professional filers (ITR-3/4) wanting to opt out into the Old Regime are provided with the mandatory CBDT **Form 10-IEA** generator before the Section 139(1) due date.
- **1-Time Lifetime Lock-In Safeguard:** Tracks the statutory rule barring taxpayers who have switched back to the New Regime from ever re-entering the Old Regime while holding business income.

### 10. ⚖️ Comprehensive Statutory Legal Framework & Cloud Crash Resilience
- **Statutory Self-Declaration u/s 140:** Taxpayers solemnly declare return authenticity, acknowledging Section 277 penal consequences (up to 7 years imprisonment) and Section 270A penalties (50% to 200%).
- **Non-Advisory Platform Disclaimer:** Clarifies that ZeroTax is an automated client-side software utility, NOT a Chartered Accountant firm (CA Act 1949), legal practice, or SEBI RIA.
- **Cloud Crash-Resilience State Retention Manager (`CloudBackupSyncManager`):** Forward-compatible with remote cloud backup APIs (`/api/v1/tax/backup-sync`). Maintains an encrypted, zero-knowledge local journal and synchronizes session state so that if the Android OS terminates the app or the device shuts down, the user's return draft is safely recovered.

---

## 🏗️ Multi-Module Gradle Topology

The application enforces a strict directed acyclic graph (DAG) across modules with **zero circular dependencies**:

```
                                    :app (Android Application)
           Jetpack Compose 4-Tap Spine + Pro Workspace, Hilt DI, FLAG_SECURE Host
                                              │
         ┌──────────────────┬─────────────────┼──────────────────┬──────────────────┐
         ▼                  ▼                 ▼                  ▼                  ▼
  :feature:          :feature:         :feature:          :feature:          :core:security
  portal-bridge      pdf-unlocker      ai-extractor       aa-aggregator      Keystore StrongBox +
  Ephemeral          PdfBox 15-Perm    Android AICore     ReBIT Curve25519   DPDP Shredder +
  WebView +          + TRACES Regex    (Gemini Nano) +    + HKDF-SHA256      Aadhaar Verhoeff
  Challan 280        + Broker P&L      Chatbot + Copilot  RAM Decryptor      Scrubber
         │                  │                 │                  │                  │
         └──────────────────┴────────┐        ▼        ┌─────────┴──────────────────┘
                                     ▼                 ▼
                              :core:database (SQLCipher 4.5.4)
                              Hardware-Encrypted zero_tax_vault.db
                                              │
                                              ▼
                              :core:tax-engine (PURE KOTLIN JVM)
                              Stateless, 100% Deterministic (Zero Android SDK)
                              - DeterministicIndianTaxEngine
                              - DeterministicReconciler
                              - SalaryRestructuringOptimizer
                              - CapitalGainsAndLossLedgerEngine
                              - SmallBusinessAuditAndPresumptiveEngine
                              - NonResidentTaxAndResidencyEngine
                              - Form10IeaBusinessRegimeEngine
                              - PreFilingAuditEngine
                              - HomeLoanAndDeductionCalculator
                              - HraAndInterestEngine
                              - RevisionAndHarvestingEngine
                              - CbdtItr1JsonCompiler
                                              │
                                              ▼
                              :core:model (Pure Kotlin Models)
                              Immutable Data Classes & Rupee Value Object
```

---

## 🔒 Privacy & Statutory Compliance

- **Zero Cloud Egress (0B):** All PDF parsing, optical character structuring, and tax calculations execute entirely within the mobile device. No financial data is ever transmitted to marketing servers or third-party brokers.
- **UIDAI Section 29 Compliance:** Aadhaar numbers are redacted at byte-level using the Verhoeff checksum algorithm into `[Aadhaar Redacted]` before entering SQLite or memory caches.
- **DPDP Act 2023 Section 12(3) Right to Erasure:** A 1-tap *"Shred Local Tax Vault"* action executes a DOD 5220.22-M compliant random byte overwrite across all database pages, journals, and cache files, followed by deletion of the Android Keystore master wrapping key in `< 300ms`.
- **Hardware-Backed Screen Protection:** `WindowManager.LayoutParams.FLAG_SECURE` is active across all screens, blocking screenshots and blanking out preview thumbnails in Android Recents.

---

## 💻 Tech Stack & Requirements

| Layer | Technologies |
| :--- | :--- |
| **Language** | Kotlin 2.0.21 (100% Kotlin codebase) |
| **UI Framework** | Jetpack Compose (Material3, AnimatedContent, Coroutine Animations) |
| **Min / Target SDK** | Android SDK 26 (Android 8.0 Oreo) / Android SDK 35 (Android 15 Vanilla Ice Cream) |
| **JDK Version** | Java Development Kit (JDK) 17 LTS |
| **Local Database** | SQLCipher 4.5.4 with Room 2.6.1 (AES-256-GCM Hardware Keystore wrapping) |
| **On-Device AI** | Google Android AICore (`com.google.ai.edge.aicore`) with Gemini Nano |
| **PDF Processing** | PdfBox-Android 2.0.27.0 with TRACES Regex Engine |
| **Cryptographic Standards** | Curve25519 (X25519) ECDHE, HKDF-SHA256, AES-GCM-256, Verhoeff Checksum |
| **Architecture Pattern** | MVI / Clean Architecture with Unidirectional Data Flow (UDF) |

---

## 📚 Complete 18-Document Master Inventory (`docs/`)

The repository maintains an authoritative 18-document architectural and regulatory specification in [`docs/`](file:///D:/projects/ITR/docs):

1. [`00-Master-Index-And-Roadmap.md`](file:///D:/projects/ITR/docs/00-Master-Index-And-Roadmap.md) — Master Phase-by-Phase Roadmap & Documentation Directory
2. [`01_product_requirements_document.md`](file:///D:/projects/ITR/docs/01_product_requirements_document.md) — Product Requirements Document (PRD), Personas & KPIs
3. [`02_user_stories_and_acceptance_criteria.md`](file:///D:/projects/ITR/docs/02_user_stories_and_acceptance_criteria.md) — Gherkin BDD User Stories & Acceptance Criteria
4. [`03_information_architecture_and_wireframes.md`](file:///D:/projects/ITR/docs/03_information_architecture_and_wireframes.md) — Wireframes, 4-Tap Spine, Toolkit & Analytics UX
5. [`04_technical_architecture_and_system_design.md`](file:///D:/projects/ITR/docs/04_technical_architecture_and_system_design.md) — Multi-Module Topology, Sub-Engines & Chatbot Design
6. [`05_database_schema_and_on_device_ai_prompts.md`](file:///D:/projects/ITR/docs/05_database_schema_and_on_device_ai_prompts.md) — 13-Table SQLCipher Schema & On-Device AI Prompts
7. [`06_qa_golden_test_vectors_and_security.md`](file:///D:/projects/ITR/docs/06_qa_golden_test_vectors_and_security.md) — Golden Calculation Test Vectors (GTV-01..18)
8. [`07_legal_regulatory_and_compliance.md`](file:///D:/projects/ITR/docs/07_legal_regulatory_and_compliance.md) — Legal Framework, Disclaimers, DPDP 2023 & Cloud Retention
9. [`08_deterministic_tax_subengines.md`](file:///D:/projects/ITR/docs/08_deterministic_tax_subengines.md) — 14-Gate Form Router, Rule 2A HRA & Sec 234A/B/C
10. [`09_on_device_aicore_and_aa_crypto.md`](file:///D:/projects/ITR/docs/09_on_device_aicore_and_aa_crypto.md) — AICore Gemini Nano, ReBIT AA RAM Decryptor
11. [`10_phase_3_schedule_fa_and_cbdt_json_compiler.md`](file:///D:/projects/ITR/docs/10_phase_3_schedule_fa_and_cbdt_json_compiler.md) — Schedule FA, Form 67 Rule 128(5) & CBDT JSON Compiler
12. [`11_jetpack_compose_ui_4_tap_spine_and_modals.md`](file:///D:/projects/ITR/docs/11_jetpack_compose_ui_4_tap_spine_and_modals.md) — Jetpack Compose 4-Tap Spine & Dialogs
13. [`12_comprehensive_qa_edge_cases_and_unit_tests.md`](file:///D:/projects/ITR/docs/12_comprehensive_qa_edge_cases_and_unit_tests.md) — QA Edge-Case Matrix (ECV-01..06) & Unit Tests
14. [`13_production_readiness_checklist_and_ci_pipeline.md`](file:///D:/projects/ITR/docs/13_production_readiness_checklist_and_ci_pipeline.md) — Production Readiness, R8 Full Mode & CI/CD
15. [`Architecture.md`](file:///D:/projects/ITR/docs/Architecture.md) — Master System Architecture & Pure-JVM Firewall
16. [`MVP Tech Doc.md`](file:///D:/projects/ITR/docs/MVP%20Tech%20Doc.md) — MVP Technical Specification (CA & Principal Architect Edition)
17. [`PRD.md`](file:///D:/projects/ITR/docs/PRD.md) — Foundational Product Requirements Document
18. [`system Design.md`](file:///D:/projects/ITR/docs/system%20Design.md) — Module-by-Module Data Contracts & Memory Lifecycle

---

## 🛠️ Building & Testing

### Prerequisites
- JDK 17 (set `JAVA_HOME` to JDK 17)
- Android SDK 35 (Build-Tools 35.0.0)

### Running Unit Tests
All tax calculation and statutory deduction sub-engines reside in `:core:tax-engine` and feature test suites without requiring an Android emulator:

```bash
# Run pure-JVM master tax engine golden test vectors
./gradlew :core:tax-engine:test

# Run security and Aadhaar Verhoeff scrubber tests
./gradlew :core:security:test

# Run conversational chatbot state machine tests
./gradlew :feature:ai-extractor:test

# Run cloud backup and crash recovery tests
./gradlew :feature:portal-bridge:test
```

---

## ⚖️ License & Statutory Disclaimer

ZeroTax-Local is a self-directed client software utility. It is not a registered Chartered Accountant firm under the Chartered Accountants Act, 1949, nor an advocate practice under the Advocates Act, 1961. The user acts as a self-filer under Section 140 of the Income-tax Act, 1961 and assumes sole responsibility for all declarations and returns filed.

