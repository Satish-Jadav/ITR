# Master Documentation Index & Statutory Roadmap (18-Document Architecture)

This master index provides the consolidated architecture, product specifications, deterministic engineering sub-engines, and regulatory compliance blueprints for **ZeroTax-Local**.

---

## Complete 18-Document Master Inventory

| Doc # | Filename | Scope & Primary Focus |
|:---:|---|---|
| **00** | [`00-Master-Index-And-Roadmap.md`](file:///D:/projects/ITR/docs/00-Master-Index-And-Roadmap.md) | Master Phase-by-Phase Roadmap, Form Support (ITR-1/2/4/U), and Document Directory Map |
| **01** | [`01_product_requirements_document.md`](file:///D:/projects/ITR/docs/01_product_requirements_document.md) | Consolidated Product Requirements Document (PRD): Core personas, functional specifications & KPIs |
| **02** | [`02_user_stories_and_acceptance_criteria.md`](file:///D:/projects/ITR/docs/02_user_stories_and_acceptance_criteria.md) | Exhaustive Gherkin BDD Acceptance Criteria for all 4 filing spine stages and pro hub workflows |
| **03** | [`03_information_architecture_and_wireframes.md`](file:///D:/projects/ITR/docs/03_information_architecture_and_wireframes.md) | Screen-by-screen information architecture, ASCII wireframes & multilingual CBDT taxonomy |
| **04** | [`04_technical_architecture_and_system_design.md`](file:///D:/projects/ITR/docs/04_technical_architecture_and_system_design.md) | Multi-module Gradle topology, pure-JVM firewall gate, and production Kotlin sub-engine blueprints |
| **05** | [`05_database_schema_and_on_device_ai_prompts.md`](file:///D:/projects/ITR/docs/05_database_schema_and_on_device_ai_prompts.md) | 13-table SQLCipher 4.5.4 schema, Room DAOs, and on-device AICore Gemini Nano system prompts |
| **06** | [`06_qa_golden_test_vectors_and_security.md`](file:///D:/projects/ITR/docs/06_qa_golden_test_vectors_and_security.md) | Parameterized Golden Tax Calculation Test Vectors (GTV-01..18) and OWASP MASVS security benchmarks |
| **07** | [`07_legal_regulatory_and_compliance.md`](file:///D:/projects/ITR/docs/07_legal_regulatory_and_compliance.md) | Statutory compliance framework: DPDP Act 2023, Income-tax Act 1961, and Aadhaar Act Section 29 |
| **08** | [`08_deterministic_tax_subengines.md`](file:///D:/projects/ITR/docs/08_deterministic_tax_subengines.md) | 14-Gate ITR Form Auto-Router, Rule 2A HRA exemptions, and Section 234A/B/C statutory interest |
| **09** | [`09_on_device_aicore_and_aa_crypto.md`](file:///D:/projects/ITR/docs/09_on_device_aicore_and_aa_crypto.md) | On-Device Gemini Nano wrapper, ReBIT AA X25519 RAM decryptor, and Challan 280 parser |
| **10** | [`10_phase_3_schedule_fa_and_cbdt_json_compiler.md`](file:///D:/projects/ITR/docs/10_phase_3_schedule_fa_and_cbdt_json_compiler.md) | Schedule FA Calendar-Year Engine, Form 67 Rule 128(5) FTC, ITR-U Sec 140B, and CBDT JSON compiler |
| **11** | [`11_jetpack_compose_ui_4_tap_spine_and_modals.md`](file:///D:/projects/ITR/docs/11_jetpack_compose_ui_4_tap_spine_and_modals.md) | Jetpack Compose UI ("4-Tap Spine", Smart Individual PAN keypad, Sec 270A warning dialog & DPDP modal) |
| **12** | [`12_comprehensive_qa_edge_cases_and_unit_tests.md`](file:///D:/projects/ITR/docs/12_comprehensive_qa_edge_cases_and_unit_tests.md) | Comprehensive QA edge-case matrix (ECV-01..06), airplane-mode verification & extended JUnit 5 suite |
| **13** | [`13_production_readiness_checklist_and_ci_pipeline.md`](file:///D:/projects/ITR/docs/13_production_readiness_checklist_and_ci_pipeline.md) | Production release readiness checklist, R8 Full Mode hardening & GitHub Actions CI pipeline |
| **14** | [`Architecture.md`](file:///D:/projects/ITR/docs/Architecture.md) | Master System Architecture: 3-Pillar ingestion layer, pure-JVM firewall, and enterprise animation suite |
| **15** | [`MVP Tech Doc.md`](file:///D:/projects/ITR/docs/MVP%20Tech%20Doc.md) | MVP Technical Specification: CA & Principal Architect edition for salaried & freelance filing |
| **16** | [`PRD.md`](file:///D:/projects/ITR/docs/PRD.md) | Foundational Product Requirements Document: Zero-friction filing UX and on-device privacy moat |
| **17** | [`system Design.md`](file:///D:/projects/ITR/docs/system%20Design.md) | Detailed System Design: Module-by-module data contracts, IPC boundaries, and memory lifecycle |

---

## Master Phase-by-Phase Execution Roadmap

| Phase | Target Release Window | Primary Focus | ITR Forms & Schedules | External API Cost |
|---|---|---|---|---|
| **Phase 1 (MVP)** | Weeks 1 – 6 | Salaried & Freelance Zero-Friction Filing | `ITR-1 (Sahaj)`, `ITR-4 (Sugam - 44ADA/44AD)` | **₹0** (Assisted WebView + On-Device `AICore`) |
| **Phase 2 (Scale)** | Weeks 7 – 12 | Investors, Account Aggregator & UPI Tax Pay | `ITR-2`, `Schedule CG`, `Schedule 112A`, `CYLA/BFLA/CFL`, `Challan 280` | AA per-consent fee (or CAMS PDF local parse at ₹0) |
| **Phase 3 (Moat)** | Weeks 13 – 18 | Foreign RSUs, Vernacular AI Copilot & Revisions | `Schedule FA`, `Form 67`, `Sec 139(5) Revised`, `ITR-U`, `EN/HI/GU` Notice AI | **₹0** (Bundled SBI TTBR DB + On-Device `AICore`) |
| **Phase 4 (Enterprise)** | Weeks 19 – 24 | Corporate Entities, LLPs, Trusts & Institutional Audits | `ITR-5 (LLP/Firm Sec 40(b))`, `ITR-6 (Corporate Sec 115BAA/MAT)`, `ITR-7 (Trust Sec 11/12/13/13A/115BBI)` | **₹0** (Deterministic Local Pure-JVM Engine) |

---

## 4. Master Feature Extension & Work Package Architecture (WP-01 to WP-12 — 100% COMPLETED ✓)

### 4.1 Module-by-Module Audit: Baseline vs. Delivered Extensions
- **`:core:model`**: Consolidated domain models in `TaxDomainModels.kt` (`EmployerSalarySource` with full Chapter VI-A & `80CCD(2)` breakdown, `ResidenceCityCategory`, `PropertyType`, `HousePropertyInput`, `AdvanceTaxQuarterlyPayments`, `CapitalGainsWindfallDates`, `Equity112ALot`, `ImmovablePropertyLtcgLot`, `ForeignEquityLotInput`, `SftAndTcs206CEntry`, `RegimeComputationResult`, `DualRegimeComparison`).
- **`:core:tax-engine`**: Pure-JVM statutory sub-engines (`DeterministicIndianTaxEngine`, `DeterministicReconciler`, `DeterministicItrFormRouter`, `HraAndInterestEngine`, `ScheduleHpAndPresumptiveSubEngine`, `SalaryAllowancesAndChapterViaSubEngine`, `CapitalGainsAndForeignAssetEngine`, `ScheduleFaCalendarYearEngine`, `RevisionAndHarvestingEngine`, `CbdtCategoryADefectGate`, `CbdtItr1JsonCompiler`, `Form67AndItr2ScheduleCompiler`, `PriorYearItrJsonImporter`).
- **`:core:database`**: 13-table Room schema with SQLCipher 4.5.4 hardware AES-256-GCM encryption, seed CII 2001-2026 table (376 for 2025-26), ISIN FMV 2018 benchmark seeds, 10-year SBI TTBR foreign exchange lookup with holiday rollback query, and RAM passphrase zeroing.
- **`:core:security`**: UIDAI Aadhaar Verhoeff checksum byte scrubber (`"[Aadhaar Redacted]"`), 16-digit Virtual ID (VID) redaction, hardware ECDSA audit signing in `CanonicalWarningHasher`, DPDP Act Section 12(3) 1-tap vault shredder with pre-shred AA revocation hook, and hardware-backed StrongBox Keystore.
- **`:feature:pdf-unlocker`**: PdfBox-Android with 15+ candidate silent unlocker (including bank customer ID & account permutations), TRACES Form 16 Part A/B regex parser with Chapter VI-A & `80CCD(2)` extraction, `DomesticCapitalGainsPdfParser` (CAMS/KFintech/Zerodha/Groww with SGB & buyback detection), `ForeignBrokerRsuAnd1042SParser` (E*TRADE/Schwab/Fidelity & Form 1042-S), and `LocalTaxComputationPdfExporter`.
- **`:feature:ai-extractor`**: Google Android AICore (Gemini Nano) running strictly at `temperature = 0.0f` and `topK = 1` for natural-language chat ingestion, `OnDeviceForm16AiExtractor`, `OnDeviceReceiptAnd80GExtractor` (Rent, 80D, 80G Form 10BE), `OnDeviceBankNarrationClassifier` (25-row batching), and `TokenLockedVernacularExplainer` covering 6 Indic locales (`en-IN`, `hi-IN`, `gu-IN`, `mr-IN`, `ta-IN`, `te-IN`), protected by the deterministic post-AI math sanity firewall (±₹5 tolerance).
- **`:feature:portal-bridge`**: Ephemeral portal bridge with domain allowlisting, `UpiChallan280IntentLauncher` (Major Head 0021, Minor Head 300, BSR/CIN validation), 1-tap CBDT JSON upload, Aadhaar OTP verification, AIS discrepancy feedback submitter, live CPC status poller, and 500ms cookie/DOM destruction.
- **`:feature:aa-aggregator`**: RBI Account Aggregator in-memory ephemeral decryptor (Curve25519 + HKDF-SHA256 + AES-GCM + RAM wipe), `RebitFiDataInMemoryParser` for `DEPOSIT` and `MUTUAL_FUNDS`/`EQUITIES`, and `AaConsentArtifactManager` (1-tap FIP consent revocation).
- **`:app`**: Jetpack Compose 4-Tap Filing Spine, Smart Tax Toolkit (9 interactive tools), Dual-Region NRI suite, Small Business Audit Suite, Section 140 statutory acknowledgement gate, `IndianCurrencyFormatter`, 5-minute scoped `FLAG_SECURE` override helper, and `StatutoryCardsAndModals`.

### 4.2 12-Work Package Execution Matrix
| WP # | Work Package Scope | Status | Deliverables & Verified Components |
|:---:|---|:---:|---|
| **WP-01** | Domain Model Consolidation & Core Tax Engine Bugs | **Completed ✓** | Unified `TaxDomainModels.kt`, fixed Sec 87A special-rate exclusion, implemented Unexhausted Basic Exemption Absorption (`Proviso to Sec 111A/112A`), capped surcharge on capital gains/dividends at 15%, added `compareRegimes()` breakeven engine. |
| **WP-02** | Statutory Sub-Engines (Sec 10/16, Chapter VI-A, HP, Presumptive, 234) | **Completed ✓** | Multi-property `Schedule HP` (2 self-occupied u/s 23(2), 1/5th pre-EMI interest u/s 24(b), co-owner split u/s 26, ₹2L Sec 71(3A) cap), Presumptive trio (`44ADA`/`44AD`/`44AE`), Rule 2A Landlord PAN gate (>₹1L rent), Rule 119A(b) ₹100 truncation, Sec 207 Senior Citizen immunity, Sec 234C 12%/36% safe harbors, 22-section Chapter VI-A (`80C`..`80U`). |
| **WP-03** | 3-Way Reconciler, Rule 9D EPF Split & Credits | **Completed ✓** | 7-head reconciliation, high-water mark, Sec 57(i) 20% dividend expense cap, TCS 206C(1G)/(1F) sweeper, Rule 9D EPF taxable interest split (>₹2.5L), Sec 64(1A) minor child clubbing with ₹1,500 exemption, Sec 205 Employer Dispute Letter generator. |
| **WP-04** | Advanced Capital Gains, Reinvestment & Foreign Assets | **Completed ✓** | Immovable property LTCG indexation comparison (12.5% vs 20% CII=376), Sec 54/54EC/54F + CGAS 1988 planner, Secs 70/71/74 loss set-off matrix, Sec 115BBH VDA 30% flat tax, Sec 46A buyback loss, Sec 47(viic) SGB exemption, Schedule FA Table A3 LOT valuation with weekend/holiday TTBR rollback, ITR-U 25%/50% additional tax windows. |
| **WP-05** | Official CBDT JSON Compilers & 24+ Golden Test Suite | **Completed ✓** | Category 'A' 6-defect pre-submission gate, Prior-year ITR JSON importer, Schedule FSI/TR & Form 67 compiler, and full 24-vector test suite (`GTV-01..18` + `ECV-01..06`) passing with ₹0.00 variance. |
| **WP-06** | 13-Table Encrypted SQLCipher Vault & Hardware Security | **Completed ✓** | 13 Room tables with 25-yr CII seed (`2001-02` to `2025-26` = 376), Jan 31 2018 ISIN FMV benchmarks, 10-yr SBI TTBR rates, RAM passphrase zeroing, 16-digit UIDAI VID scrubbing, hardware ECDSA audit signing, and pre-shred AA revocation hook. |
| **WP-07** | Enhanced PDF Unlocker & Parsers | **Completed ✓** | Bank customer-ID/account password permutations, Form 16 Part B Chapter VI-A extraction, CAMS/KFintech/Zerodha CAS parser, E*TRADE/Schwab/1042-S parser, and offline CA computation PDF exporter. |
| **WP-08** | On-Device AICore Extractor & 6-Locale Copilot | **Completed ✓** | Form 16 Part B parser, Rent / 80D / 80G Form 10BE receipt extractor, 25-row batch bank narration classifier, and 6 Indic vernacular locales (`en-IN`, `hi-IN`, `gu-IN`, `mr-IN`, `ta-IN`, `te-IN`). |
| **WP-09** | RBI Account Aggregator In-Memory Parser & Consent | **Completed ✓** | In-memory ReBIT Sahamati `DEPOSIT` and `MUTUAL_FUNDS`/`EQUITIES` parser with volatile RAM zeroing and 1-tap FIP consent revocation manager. |
| **WP-10** | UPI Challan 280 & Hardened Ephemeral Portal Bridge | **Completed ✓** | Deep-link builder (`upi://pay` Major 0021 / Minor 300) with BSR/CIN validation, 1-tap CBDT JSON upload, Aadhaar OTP e-Verify bridge, AIS feedback submitter, and post-filing CPC status poller. |
| **WP-11** | Full Compose UI Integration & Statutory Modals | **Completed ✓** | `IndianCurrencyFormatter`, 5-minute scoped `FLAG_SECURE` override helper, DPDP Section 6 multilingual consent card, cropped PDF disambiguation card, Section 205 employer TDS shield card, and presumptive slider. |
| **WP-12** | Multi-Branch CI/CD Pipeline & Master Documentation | **Completed ✓** | Updated GitHub Actions CI workflow triggering on `master`, `main`, and `release/*`, updated root `README.md`, verified 0 test failures, and removed temporary plan files. |

---

### 4.3 Next-Gen Feature Suite & User Authentication System (F-01 to F-15)
- **User Authentication System (`AuthDomainModels.kt`, `ZeroTaxAuthSecurityManager.kt`, `AuthenticationScreen.kt`)**:
  - Dual Email & Password authentication with PBKDF2/SHA-256 salted hashing and input validation.
  - "Continue with Google" sign-in integration with Google account picker and profile synchronization.
  - "Use Local Offline Vault" zero-account privacy alternative for complete on-device autonomy.
  - Device Biometrics integration (Fingerprint / PIN / Face) via AndroidX Biometric.
  - DPDP Act 2023 Section 12(3) compliance: User credentials securely bind to SQLCipher hardware Keystore master key without cloud PII exposure.
  - Seamless entry pipeline in `MainActivity.kt`: `SplashScreen` $\to$ `AuthenticationScreen` $\to$ `ZeroTaxFourTapSpineScaffold`.
- **Section 6 Residential Status Engine (`ResidentialStatusAndFamilyArbitrageEngine.kt` — Feature F-01)**:
  - Comprehensive day-counter evaluating 182-day, 60+365-day, and 120-day (> ₹15L) rules for ROR, RNOR, and NR.
  - Legal shielding: Proving RNOR status legally exempts returning NRIs from Schedule FA Black Money Act reporting and shields foreign dividends/capital gains from Indian tax for 2–3 years.
- **Family Tax Arbitrage Simulator (`ResidentialStatusAndFamilyArbitrageEngine.kt` — Feature F-02)**:
  - Rent to retired parents u/s 10(13A) (saving 30% slab tax while parents enjoy 0% slab up to ₹12L in New Regime).
  - Senior parent health insurance u/s 80D up to ₹50,000.
  - Capital gifting to adult children (18+) or parents to invest in high-yield FDs completely avoiding Section 64 clubbing.
- **Section 244A Refund Interest Auditor & Section 154 Builder (`RefundInterest244AAndGstAuditor.kt` — Features F-05 & F-06)**:
  - Audits 0.5% per month (6% p.a.) simple interest from April 1 of AY on refunds $\ge 10\%$ of assessed tax.
  - Automatic Section 154 rectification payload generation for CPC Bengaluru interest shortfalls.
- **Freelancer GST & e-FIRA Radar (`RefundInterest244AAndGstAuditor.kt` — Feature F-08)**:
  - ₹20L gross receipts threshold tracking and export LUT / e-FIRA inward remittance compliance checks.
- **Section 50C 10% Stamp Duty Safe-Harbor Shield (`ScheduleAlAndCapitalGainsWealthEngine.kt` — Feature F-03)**:
  - Third Proviso to Section 50C(1) tolerance check: adopts actual agreement consideration with ₹0 deemed addition when circle rate is within 110% of sale price.
- **Post-Oct 1, 2024 Share Buyback Tax Engine (`ScheduleAlAndCapitalGainsWealthEngine.kt`)**:
  - Computes Deemed Dividend u/s 2(22)(f) in Schedule OS and records original purchase cost as Capital Loss u/s 46A in Schedule CG.
- **Schedule AL Auto-Builder (`ScheduleAlAndCapitalGainsWealthEngine.kt` — Feature F-09)**:
  - Automated aggregation of immovable/movable assets and liabilities for incomes > ₹50 Lakhs into CBDT JSON format.
- **Rule 26C Form 12BB Proof Generator (`LocalStatutoryPdfGenerators.kt` — Feature F-04)**:
  - Generates bank-grade PDF 1.4 investment declarations (HRA, 80C, 80CCD, 80D, 24(b)) on-device for employer HR/payroll submission.
- **Form 15G / 15H Zero-TDS FD Declaration Generator (`LocalStatutoryPdfGenerators.kt` — Feature F-07)**:
  - Generates CBDT-compliant Form 15H (senior citizens) and Form 15G (under 60) declarations to eliminate 10% TDS deductions on bank fixed deposits.
- **100% Offline Natural-Language "What-If" Scenario Simulator (`OfflineWhatIfTaxSimulator.kt` — Feature F-15)**:
  - Real-time (< 5ms) deterministic tax calculation evaluating investment scenarios without network latency or cloud data leakage.
- **Interactive Compose Smart Tax Toolkit Views (`StatutoryToolkitViews.kt` & `SmartTaxToolkitScreen.kt`)**:
  - Expanded `ToolkitTab` to 15 specialized tabs with smooth horizontal slide/fade animated transitions surfacing all next-gen features directly to end-users.

---

### 4.4 Non-Individual Corporate & Trust Entities Suite (ITR-5, ITR-6, ITR-7)
- **ITR-5 Partnership & LLP Suite**:
  - Full statutory implementation of Section 40(b) interest (12% simple interest cap) and partner remuneration (90% up to ₹3 Lakhs book profit, 60% thereafter).
  - Alternative Minimum Tax (AMT) u/s 115JC @ 18.5% with Form 29C calculation and Section 115JD AMT credit tracking.
- **ITR-6 Corporate Company Suite**:
  - Concessional regime evaluation: Section 115BAA @ 22% (MAT-exempt domestic company) and Section 115BAB @ 15% (new manufacturing company).
  - Minimum Alternate Tax (MAT) u/s 115JB @ 15% on Book Profits with 14 statutory additions/deductions and Section 115JAA MAT credit carry-forward (15 AYs).
- **ITR-7 Trust, NGO & Political Party Suite**:
  - Section 11 & 12 statutory application of income (85% minimum application rule, Form 9A deemed application, Form 10 accumulation for 5 years).
  - Section 13 statutory violations taxed at Maximum Marginal Rate (MMR) 34.944% under Section 115BBI.
- **Deterministic Form Router (`DeterministicItrFormRouter.kt`)**:
  - Routes 4th PAN character: `'C'` $\to$ `ITR-6`/`ITR-7`, `'F'` $\to$ `ITR-5`/`ITR-4`, `'T'` $\to$ `ITR-7`, `'A'/'B'/'L'/'J'` $\to$ `ITR-5`.

---

### 4.5 Plan-Based Fast-Start Dashboards, Biometric Shield Gate & Statutory Reminders
- **Plan-Based User Experience Separation**:
  - **SAHAJ (ITR-1)**: Plain-language, jargon-free interface for first-time filers (*"What You Earned"*, *"Automatic ₹75,000 Government Discount"*, *"Taxes Deducted by Employer"*, *"Your Refund Result"*).
  - **SUGAM (ITR-4)**: 2-minute presumptive deck for freelancers and small businesses (`44ADA` @ 50% / `44AD` @ 6% digital / 8% cash) with zero books of accounts required.
  - **ENTERPRISE AUDIT (ITR-2 to ITR-7)**: Executive statutory audit desk with 1-tap fast launchers for Corporate Suite (`ITR-5/6/7`), 44AB Form 3CD, Dual-Region NRI DTAA, and Notice Defense.
- **Interactive Coachmark Walkthrough ("Do this then click this" Mini-Popup Tour)**:
  - Step-by-step guidance anchored cleanly with visual pointer cards (`Step 1 of 4` $\to$ `Step 4 of 4`) guiding non-expert users through Form 16 verification, regime selection, and biometric filing. Replayable at any time via the `"How to File"` button.
- **Full Biometric Security Shield (`ZeroTaxBiometricShield.kt`)**:
  - Device biometric gate (Fingerprint / Face ID / PIN fallback) protecting the tax vault, filing records, PAN, bank accounts, and exports from unauthorized access. Includes an instant 1-tap `"Lock"` button in the top bar.
- **Statutory Tax Timeline & Reminders (`StatutoryTaxTimelineManager.kt`)**:
  - Real-time statutory milestone countdown engine tracking the **July 31** deadline, countdown timer pill (*"86 Days remaining"*), and 1-tap local notification scheduler for filing start, 30-day reminder, 6-day urgent penalty warning u/s 234F, and belated cutoff (Dec 31).

---

### 4.6 In-App Live ITD Portal Sync Suite & Zero-External-Login Architecture (`ItdLivePortalSyncModal.kt`)
- **Zero External Login Guarantee:**
  - Users authenticate directly inside ZeroTax via **1-Tap Aadhaar OTP** or **Portal Password**; they never need to open a separate web browser or log into `eportal.incometax.gov.in`.
- **4-Point In-App Live Sync Handshake:**
  1. **Form 26AS Sync:** Fetches all verified TDS/TCS entries deposited by employers, banks, and clients u/s 192, 194A, 194J, 194C, and 206C.
  2. **AIS / TIS Live Extraction:** Automatically captures interest from all savings accounts, time deposits, dividends, and securities transactions without user data entry.
  3. **Pre-fill Profile & Bank Accounts:** Pulls verified demographic details and pre-validated bank accounts for direct refund deposit.
  4. **Filing & Refund Status Tracking:** Queries CPC processing status, intimations u/s 143(1), and refund dispatch numbers.
- **DPDP Act 2023 Volatile Security:**
  - Credentials and session tokens reside exclusively in volatile memory during the direct TLS handshake and are zeroized immediately following payload retrieval.

---

### 4.7 F&O Trading & Tax Audit, Post-Filing Lifecycle, Senior Citizen Exemption & Micro-Prompt AI Copilot Suite
- **F&O Trading, Turnover & Tax Audit Engine (`FnOTradingAndTaxAuditEngine.kt` — Section 31)**:
  - **ICAI Guidance Note Absolute Turnover Formula**: Computes statutory turnover as $\sum |\text{P\&L}_i| + \text{Premium Received on Sale of Options}$.
  - **Speculative Intraday Equity Trading u/s 43(5)**: Set-off ring-fence u/s 73 (only against speculative profit; 4-year carry forward u/s 73(4)).
  - **Non-Speculative F&O Trading u/s 43(5) Proviso (d)**: Loss set-off u/s 70 & 71 (offsets HP, CG, Other Sources; strictly barred from setting off Salary u/s 71(2A); 8-year carry forward u/s 72).
  - **Section 44AB Tax Audit Decision Tree**: Evaluates the ₹10 Crore threshold for $\ge 95\%$ digital transactions and Section 44AD(4)/(5) 5-year lock-in opt-out rules.
- **Automated Section 143(1) Intimation Diff Analyzer & Section 154 Rectification Builder (`Section143_1IntimationDiffAnalyzer.kt` — Section 33.1)**:
  - Line-by-line comparison of Column A ("As Filed by Taxpayer") vs Column B ("As Computed under Section 143(1)").
  - Detects zero discrepancy, tax credit mismatches (TDS/TCS/Challan dropped by CPC), and unreported AIS income additions.
  - Generates CBDT Section 154 Rectification JSON payload and Section 139(5) Revised Return action guidance.
- **Zero-Knowledge `.ztvault` Encrypted Migration Engine (`EncryptedZtVaultMigrationEngine.kt` — Section 33.3)**:
  - Portable, zero-cloud backup and transfer protocol for multi-year CFL losses and return data across devices.
  - Key derivation: PBKDF2 with 256,000 iterations from PAN, DOB, and user passcode with 32-byte salt.
  - AES-256-GCM authenticated encryption (12-byte IV, 128-bit auth tag) with gzip compression.
- **Senior Citizen Tax Deductions & Section 194P Exemption Engine (`SeniorCitizenTaxAndExemptionEngine.kt` — Section 34)**:
  - **Section 194P Exemption**: Evaluates age $\ge 75$ resident pensioners receiving interest only from the specified pension bank; statutorily exempts them from filing ITR u/s 139 upon Form 12BBA submission.
  - **Form 12BBA Generator**: Creates structured declaration payload for the specified bank.
  - **Senior Citizen Deductions**: Implements Section 80TTB ₹50,000 bank interest deduction, Section 207(2) Advance Tax immunity (₹0 Sec 234B/C interest), and Section 80D up to ₹50,000.
- **Hybrid Multi-Source Income & Progressive Disclosure Engine (`HybridIncomeAndProgressiveDisclosureEngine.kt` — Section 38)**:
  - Combines primary salary with moonlighting jobs, freelancing u/s 44ADA (50% presumptive deduction), digital creators u/s 44AD (6% digital / 8% cash), commissions u/s 194H, and casual gigs u/s 56.
  - Dynamic progressive disclosure state machine showing only relevant cards, seamlessly auto-routing to ITR-1, ITR-4, ITR-2, or ITR-3 Lite behind the scenes.
- **Optimized Tax Copilot Chatbot Engine (`OptimizedTaxCopilotChatbotEngine.kt` — Section 35)**:
  - 6 Guided Quick-Pick Action menus with 1-tap chips (`AUTO_FIND_DOCS`, `SYNC_PORTAL_6_STREAMS`, `COMPARE_REGIME_WHY`, `SIMULATE_NPS_80D_HRA`, `HARVEST_112A_LTCG`, `GENERATE_HR_FORMS_PACK`, `EXPLAIN_NOTICE_OR_PAY_CHALLAN`).
  - 3-Tier Zero-Waste AI Pipeline: Tier-0 Deterministic Regex Router (< 2ms, 0 tokens), Tier-1 Memoization Cache (< 2ms), Tier-2 KV-Prefix Cached Micro-Prompts.





