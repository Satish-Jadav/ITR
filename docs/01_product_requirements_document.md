01 — Master Product Requirements Document (PRD): Project "ZeroTax-Local" (Consolidated Phases 1, 2 & 3 — CA & Legal Counsel Edition)
1. Executive Vision, Problem Statement & Architectural Mandate
1.1 The Indian Retail Tax Filing Trilemma
Every Assessment Year, over 8.5 crore Indian taxpayers confront a broken trilemma when filing their Income Tax Returns (ITR-1 through ITR-4 and ITR-U):
Government Portal Complexity & Statutory Notice Traps: The official e-Filing portal exposes hundreds of statutory schedules (Schedule S, Schedule HP, Schedule BP, Schedule CG, Schedule 112A, Schedule FA, Schedule FSI, Schedule TR, Schedule CYLA, Schedule BFLA, Schedule CFL, Schedule 80D, Schedule 80G, Schedule AL, Schedule IT, Schedule TDS1/TDS2/TCS). A single miscalculation in Section 87A marginal relief, double-counting of Section 16(ia) standard deduction across two employers under Section 192(2B), claiming Section 10(13A) HRA above ₹1,00,000 without a Landlord PAN (Rule 2A), or reporting foreign RSUs on a Financial Year (Apr 1 – Mar 31) instead of a Calendar Year (Jan 1 – Dec 31) triggers automated Centralized Processing Centre (CPC) Bengaluru defective return notices (Section 139(9)), demand intimations (Section 143(1)), or severe penalties (Section 270A / Black Money Act, 2015).
Cloud Intermediary Privacy Exploitation: Commercial cloud-based tax platforms require users to upload unredacted Form 16 PDFs, bank statements, and consolidated account statements (CAS) to remote servers, frequently harvesting salary and investment metadata to cross-sell personal loans, ULIPs, and high-commission mutual funds—violating the statutory mandate of purpose limitation and data minimization under Sections 4 and 6 of the Digital Personal Data Protection (DPDP) Act, 2023.
Unreliable Cloud LLM Wrappers: Generic cloud LLM wrappers hallucinate arithmetic, misapply slab thresholds, violate Section 112A(6) rebate exclusions, and incur high per-user token costs while exposing raw Personal Data outside the user's handset.
1.2 The "ZeroTax-Local" Architectural Mandate
ZeroTax-Local resolves this trilemma through a strict, non-negotiable bifurcation of responsibilities:
100% Deterministic Pure Kotlin Tax & Reconciliation Engine (:core:tax-engine): Every arithmetic operation, progressive slab evaluation, Section 87A marginal relief equation, Rule 2A HRA calculation, Section 112A equity grandfathering lookup, pre-July 23, 2024 immovable property indexation comparison (Second Proviso to Section 112(1)(a)), inter-head/intra-head loss set-off matrix (Sections 70, 71, 71(3A), 74), Rule 128(5) Foreign Tax Credit (Form 67) conversion, Updated Return (ITR-U Section 139(8A) read with Section 140B) additional tax math, and Sections 288A/288B & Rule 119A rounding rule is implemented in pure, deterministic, unit-tested Kotlin code (kotlin("jvm")) with zero AI involvement and zero Android SDK dependencies.
On-Device AI Helper (com.google.ai.edge.aicore / Android Gemini Nano): AI runs strictly on the handset's NPU/TPU at temperature = 0.0f and topK = 1 for four bounded helper tasks:
Role A (Unstructured Document-to-JSON Structuring): Structuring OCR text from non-standard Form 16 Part B annexures, physical rent receipts, medical insurance bills (80D), and donation receipts (80G with ARN & Donee PAN) into strict JSON—immediately gated by a deterministic Post-AI Math Sanity Firewall (±₹5 tolerance).
Role B (Contextual Sanity & Setting Reviewer): Reviewing post-computation state for contextual anomalies (e.g., explaining why Old Regime lost to New Regime due to a missing Landlord PAN on annual rent exceeding ₹1,00,000).
Role C (Bank Narration Deduction & Receipt Classifier): Scanning batched bank statement narration strings from Account Aggregator ledgers (pre-filtering out personal UPI debit rows < ₹500) to classify 80C_LIC_PPF_ELSS, 80D_HEALTH_INS, 80G_DONATION, 44ADA_FREELANCE_INFLOW, and SAVINGS_INTEREST.
Role D (Plain-Language & Vernacular Notice Copilot): Translating intimidating CPC notices (143(1), 139(9), 148, High-Value AIS e-Campaigns) and tax rules into reassuring English (en-IN), Hindi (hi-IN), and Gujarati (gu-IN) using Immutable Token-Locking so rupee values and statutory section numbers can never be altered.
1.3 Regulatory & Legal Positioning Framework (CA & Fintech Legal Counsel Mandate)
User-Initiated Client-Side Browser Assistant (Non-ERI Classification): Under the Electronic Furnishing of Return of Income Scheme, 2007 notified under Section 139(1B) of the Income-tax Act, 1961, an e-Return Intermediary (ERI) is a cloud or server-side intermediary that transmits returns or accesses taxpayer accounts via centralized infrastructure. ZeroTax-Local operates strictly as a User-Initiated Client-Side Browser Assistant executing locally inside an isolated, no-cache Android WebView on the taxpayer's own handset (https://eportal.incometax.gov.in). Authentication occurs directly between the user's device and official Income Tax Department servers via user-entered Aadhaar/Portal OTP; ZeroTax servers never receive, proxy, or store portal credentials, session tokens, or unredacted tax payloads, and all WebView cookies and DOM storage are cryptographically purged within 500ms of JSON capture.
DPDP Act, 2023 Data Fiduciary & Local Vault Boundaries: In accordance with Section 6 (Itemized Notice & Consent), Section 8(5) (Reasonable Security Safeguards), and Section 12(3) (Right to Erasure) of the Digital Personal Data Protection Act, 2023, all financial records remain inside an on-device SQLCipher 4.5.x database (zero_tax_vault.db) encrypted via a 256-bit AES-GCM master key wrapped in the Android Hardware Keystore (StrongBox). Invoking "Shred Local Tax Vault" deletes the hardware Keystore alias and overwrites all database, WAL, SHM, and cached PDF bytes with SecureRandom noise in under 300ms.
RBI Account Aggregator Master Directions & ReBIT Cryptographic Compliance: In Phase 2, live financial information fetch strictly adheres to the RBI Master Direction — Non-Banking Financial Company - Account Aggregator (Reserve Bank) Directions, 2016 (DNBR.PD.009/03.10.119/2016-17) and ReBIT Sahamati specifications. Ephemeral X25519 (Curve25519) key pairs are generated in volatile RAM, decrypted via HKDF-SHA256 and AES-256-GCM, and all raw key material and plaintext buffers are zero-wiped (0x00) inside a mandatory finally block immediately after extracting schedule totals.
UIDAI Aadhaar Act, 2016 & Zero-Storage Scrubbing: In compliance with Section 29 of the Aadhaar (Targeted Delivery of Financial and Other Subsidies, Benefits and Services) Act, 2016 and UIDAI (Sharing of Information) Regulations, 2016, UidaiAadhaarVerhoeffScrubber intercepts all OCR text, WebView JSONs, and SQLite inputs using the 12-digit Verhoeff checksum algorithm, replacing any Aadhaar number with the literal token "[Aadhaar Redacted]" before persistence or AI inference.

2. Progressive 3-Phase Product Scope & Delivery Matrix


Phase
Target Filing Segments & Forms
Key Capabilities & Modules Delivered
Functional Requirements
Phase 1: Core MVP (Weeks 1–6)
Salaried & Presumptive Freelancers (ITR-1 Sahaj & ITR-4 Sugam)
Assisted WebView Portal Bridge (Prefill, 26AS, AIS), 15-permutation silent PDF unlocker, on-device Gemini Nano Form 16/receipt parser + ±₹5 Math Firewall, TAN salary de-duplication (Sec 192(2B)), strict 26AS TDS cap, Old vs. New Regime engine, Sec 87A + Marginal Relief, Sec 44ADA 50% toggle, CBDT JSON compiler & 1-Tap Aadhaar OTP e-Verify.
P1-FR-01 to 07, FR-INGEST-01 to 03, FR-RECON-01 to 03, FR-TAX-01 to 02
Phase 2: Scale (Weeks 7–12)
Market Investors, Multi-Property Owners & Tax-Due Filers (ITR-2)
RBI Account Aggregator (ReBITECDHE-Curve25519) live bank & CAS sync, CAMS/KFintech/Zerodha/Groww/AngelOne Tax P&L parser (500+ rows in < 1.5s), ISIN-level Schedule 112A LTCG with offline Jan 31, 2018 FMV database (~1.2 MB), Schedule CYLA/BFLA/CFL 8-year loss engine, Multi-Property Schedule HP, Gemini Nano bank narration classifier, and in-app UPI Challan 280 payment (capturing 7-digit BSR code & 5-digit Challan serial into Schedule IT).
P2-FR-01 to 07
Phase 3: Pro Moat (Weeks 13–18)
MNC Employees with US RSUs/ESOPs (Schedule FA), Global Investors & Vernacular Filers (ITR-2, ITR-3, ITR-U)
Foreign ESOP/RSU parser (E*TRADE, Fidelity, Schwab, Morgan Stanley), Schedule FA Table A3 Calendar-Year (Jan 1 – Dec 31) reporting with offline 10-year SBI TTBR database, Rule 128(5) preceding-month-end TTBR for US dividend DTAA tax credits (Schedule FSI/TR & auto-generated Form 67), Token-Locked Grounded Notice Copilot (English, Hindi, Gujarati for 143(1) & 139(9)), In-App AIS discrepancy dispute submitter, ITR-U Sec 139(8A) (25%/50% additional tax u/s 140B), and March LTCG tax-harvesting radar.
P3-FR-01 to 06

3. Deep-Dive Target User Personas
P1: "Aarav" — Mid-Year Job Switcher (Phase 1 / ITR-1): 28 yo Senior Software Engineer in Bengaluru. Switched from TCS (MUMT01928A) to Infosys (BLRI99887C) in October. Has 2 Form 16s, 2 savings accounts, EPF + ELSS 80C, employer NPS 80CCD(2), rented flat (₹28,000/mo). Solves double Sec 16(ia) standard deduction traps, dual basic exemption shortfall under Section 192(2B), and missing advance tax interest under Sections 234B/234C.
P2: "Priya" — Digital Consultant & Freelancer (Phase 1 / ITR-4): 32 yo UI/UX Consultant in Ahmedabad. Domestic clients deducting 10% TDS u/s 194J and 2% u/s 194C in Form 26AS, foreign Upwork/Stripe inward remittances (FIRC/e-FIRA), health insurance for senior citizen parents (Sec 80D). Auto-routes to ITR-4 (Sugam) and applies 50% presumptive profit under Section 44ADA (enhanced ₹75 Lakh limit where cash receipts ≤ 5%), plus single-installment March 15 advance tax rule under Section 234C.
P3: "Rohan" — Retail Stock & Mutual Fund Investor (Phase 2 / ITR-2): 41 yo VP Operations in Mumbai. Salary (₹32L), Zerodha Equity STCG (Sec 111A @ 20%), CAMS MF LTCG (Sec 112A @ 12.5% above ₹1.25L), legacy shares bought in 2016 (Jan 31, 2018 grandfathering under Section 55(2)(ac)), 2 residential properties. Enforces Sec 112A grandfathering without creating artificial loss and strictly blocks LTCL from offsetting STCG under Sections 70(3) and 74 (Schedule CYLA/CFL).
P4: "Vikram" — MNC Staff Engineer with Foreign RSUs (Phase 3 / ITR-2 + Form 67): 36 yo Staff Engineer in Hyderabad. Salary (₹58L, Surcharge 10% with Marginal Relief), US RSUs/ESPPs in Charles Schwab/E*TRADE, US dividends with 25% DTAA withholding (Form 1042-S). Auto-converts Calendar Year (Jan 1 – Dec 31) asset values using historical SBI TTBR and computes Form 67 FTC under Rule 128(5) preceding-month-end rates to protect against the ₹10,00,000 Black Money Act penalty (Sections 42 & 43).
P5: "Harshadbhai" — Senior Citizen Pensioner (Phases 1–3 / ITR-1 or ITR-2): 67 yo Retired Bank Officer in Vadodara. Pension (₹6.2L), SCSS & Bank FD interest (₹2.8L), medical bills for uninsured super-senior mother (Sec 80D second proviso), Gujarati UI preference (gu-IN). Evaluates Section 80TTB (₹50,000 deduction on FD + savings interest for Resident Senior Citizens aged ≥ 60 in Old Regime) vs. New Regime rebate, and applies Section 207 statutory immunity from Section 234B advance tax interest.

4. Statutory Tax Rule & Rate Matrix (AY 2025-26 & AY 2026-27)
4.1 Comparative Statutory Parameter Matrix (Income-tax Act, 1961 & Finance Acts)

Statutory Provision & Section
Default New Regime (Sec 115BAC(1A)) — AY 2025-26
Default New Regime (Sec 115BAC(1A)) — AY 2026-27
Old Tax Regime (Optional Opt-Out)
Normal Income Tax Slabs
₹0–3L: 0% | ₹3–7L: 5% | ₹7–10L: 10% | ₹10–12L: 15% | ₹12–15L: 20% | >₹15L: 30%
₹0–4L: 0% | ₹4–8L: 5% | ₹8–12L: 10% | ₹12–16L: 15% | ₹16–20L: 20% | ₹20–24L: 25% | >₹24L: 30%
Basic Exemption (<60: ₹2.5L; 60–79: ₹3L; >=80: ₹5L) | Up to ₹5L: 5% | ₹5–10L: 20% | >₹10L: 30%
Rebate u/s 87A (Resident Individual)
Up to ₹25,000 if Total Income ≤ ₹7,00,000 (Marginal Relief up to ₹7,27,770)
Up to ₹60,000 if Normal Taxable Income ≤ ₹12,00,000 (Marginal Relief up to ₹12,75,000)
Up to ₹12,500 if Total Income ≤ ₹5,00,000 (No Marginal Relief above ₹5,00,000)
Sec 87A Exclusion on Special Rates
Barred against Sec 112A LTCG tax under Sec 112A(6)
Barred against Sec 112A LTCG and special rate incomes
Barred against Sec 112A LTCG tax under Sec 112A(6)
Standard Deduction (Sec 16(ia))
₹75,000 (applied once across all employers)
₹75,000 (applied once across all employers)
₹50,000 (applied once across all employers)
Family Pension Deduction (Sec 57(iia))
min(33.33% of Pension, ₹25,000)
min(33.33% of Pension, ₹25,000)
min(33.33% of Pension, ₹15,000)
Employer NPS (Sec 80CCD(2))
Up to 14% of (Basic + DA) for Govt & Private sector employees
Up to 14% of (Basic + DA) for Govt & Private sector employees
Up to 14% (Central/State Govt) or 10% (Private sector) of (Basic + DA)
Self-Occupied HP Interest (Sec 24(b))
₹0 (Not allowable under Sec 115BAC(2))
₹0 (Not allowable under Sec 115BAC(2))
Up to ₹2,00,000; inter-head loss set-off capped at ₹2,00,000 u/s 71(3A)
Bank Interest (Sec 80TTA / 80TTB)
₹0 (Disallowed in New Regime)
₹0 (Disallowed in New Regime)
80TTA: ₹10,000 (Savings, Age <60) | 80TTB: ₹50,000 (Savings + FD, Age >=60)
Highest Surcharge Cap (> ₹5 Cr)
Capped at 25% (Effective max rate 39.0%)
Capped at 25% (Effective max rate 39.0%)
37% on normal income (Effective max rate 42.744%); 15% cap on 111A/112A
4.2 Detailed Capital Gains, Loss Set-Off & Foreign Asset Statutory Rules
Listed Equity & Equity-Oriented Mutual Funds (STT Paid):
Short-Term Capital Gains (Section 111A): Held ≤ 12 months; taxed at 20% (for transfers on or after July 23, 2024).
Long-Term Capital Gains (Section 112A): Held > 12 months; taxed at 12.5% on aggregate gains exceeding the ₹1,25,000 annual exemption threshold.
Grandfathering Formula (Section 55(2)(ac)): For shares/units acquired on or before January 31, 2018, the Deemed Cost of Acquisition is strictly: Deemed Cost = max(Actual Cost, min(FMV_31Jan2018, Full Value of Consideration)). This prevents artificial capital loss when the sale price falls below the January 31, 2018 Fair Market Value while preserving grandfathering protection for gains accrued up to January 31, 2018.
Specified Mutual Funds / Debt Funds (Section 50AA): Units of Specified Mutual Funds (>65% debt/money market allocation) acquired on or after April 1, 2023 are treated as Deemed Short-Term Capital Assets under Section 50AA, taxed at the taxpayer's normal applicable slab rates regardless of holding period.
Immovable Property (Land or Building) Grandfathering (Second Proviso to Section 112(1)(a)):
Standard LTCG rate (held > 24 months) for transfers on or after July 23, 2024 is 12.5% without CII indexation.
Grandfathering Protection for Resident Individuals/HUFs: Where land or building was acquired prior to July 23, 2024, the tax payable (before cess) is capped at the lower of: Tax Payable = min(12.5% × Unindexed Gain, 20% × Indexed Gain using CII 376 for FY 2025-26). Note that if the indexed computation produces a loss while the unindexed computation produces a gain, the relief reduces the tax payable to ₹0 but does not generate a carry-forward indexed loss.
Strict Capital Loss Set-Off & 8-Year Carry-Forward (Sections 70, 71, and 74):
Short-Term Capital Loss (STCL): Under Section 70(2), STCL can offset both Short-Term Capital Gains (STCG) and Long-Term Capital Gains (LTCG).
Long-Term Capital Loss (LTCL): Under Section 70(3), LTCL can offset ONLY Long-Term Capital Gains (LTCG) and is strictly barred from offsetting STCG.
Inter-Head Bar (Section 71(3)): Capital losses (STCL or LTCL) can never be set off against Salary, House Property, Business/Profession, or Other Sources. Unabsorbed STCL and LTCL are carried forward in Schedule CFL for up to 8 succeeding Assessment Years under Section 74, provided the original return was filed on or before the Section 139(1) due date (read with Section 80).
Foreign Assets (Schedule FA) & Foreign Tax Credit (Rule 128 / Form 67):
Calendar Year Reporting Mandate: Under the ITR-2/ITR-3 instructions read with Sections 42 & 43 of the Black Money Act, 2015, Resident and Ordinarily Resident (ROR) taxpayers must report all foreign assets held between January 1 and December 31 of the preceding calendar year in Schedule FA (Table A3 for Foreign Equity/RSUs/ESPPs).
Exchange Rate Rules (Rule 115 & Rule 128(5)): Schedule FA converts Initial Value (vest/purchase date), Peak Value (date of peak balance/price during Jan–Dec), and Closing Value (Dec 31) using the State Bank of India Telegraphic Transfer Buying Rate (SBI TTBR) on the respective dates (rolling back to the immediately preceding business day if the date falls on a weekend/bank holiday). For Foreign Tax Credit (Form 67 and Schedule TR/FSI), Explanation to Rule 128(5) mandates conversion using the SBI TTBR on the last day of the month immediately preceding the month in which foreign tax was deducted or paid.

5. Ingestion Pipelines, Reconciliation & Non-Functional SLAs
5.1 Pipeline 1: Assisted Portal Bridge & AIS/26AS Mapping
Ingests official CBDT categories: TDS-192 (Salary), SFT-005 / TDS-194A (Savings/FD interest), SFT-006 / TDS-194 (Dividends), TDS-194J / TDS-194C (Freelance/Contract), SFT-017 / SFT-018 (Securities sale), SFT-003 / SFT-004 / TDS-194-IA (Property sale), TCS-206C(1G) (LRS foreign remittance), and INT-244A (Refund interest).
5.2 Pipeline 2: 15-Permutation Silent PDF Unlocker
Evaluates 15 deterministic combinations in <100ms: lowercase(PAN)+ddmmyyyy, UPPERCASE(PAN)+ddmmyyyy, ddmmyyyy, lowercase(PAN), UPPERCASE(PAN), lowercase(first4)+ddmmyyyy, UPPERCASE(first4)+ddmmyyyy, lowercase(first4)+ddmm, UPPERCASE(first4)+ddmm, lowercase(PAN)+yyyy, UPPERCASE(PAN)+yyyy, lowercase(PAN)+ddmmyy, UPPERCASE(PAN)+ddmmyy, ddmmyyyy+lowercase(PAN), ddmmyyyy+UPPERCASE(PAN).
5.3 Deterministic Source-of-Truth Conflict Resolution
Gross Salary: Math-verified Form 16 Part B per Employer TAN (Sec 192(2B) de-duplicated).
TDS on Salary: Strictly capped at Form 26AS Part A deposited amount (min(Form 16 TDS, 26AS Deposited TDS)) to prevent automated Section 143(1) short-credit demand notices.
Interest Income: High-water-mark: max(AIS_SFT_005, AA_Ledger_Sum, Form_26AS_194A).
Chapter VI-A: min(Statutory_Cap, Form_16 union Verified_Receipts).
5.4 Hardware NPU Compatibility & Non-Functional SLAs
Tier 1 (AICore NPU): Google Tensor G3/G4, Snapdragon 8 Gen 2/3/Elite, Dimensity 9300/9400. Latency < 1,500ms per PDF page; 0B cloud egress.
Tier 2 (Mid-Range): Snapdragon 7s Gen 2, Snapdragon 695, Dimensity 7050. Deterministic Coordinate & Regex Parser runs in < 80ms; JVM Heap < 65MB.
Zero Cloud AI / API Cost: ₹0.00 external API fees for Phase 1 MVP.
Deterministic Math Variance: ₹0.00 variance against official CBDT schema utility across all 18 Golden Test Vectors (GTV-01 to GTV-18).

6. Consolidated Functional Requirements Traceability Matrix (Phases 1, 2 & 3)
6.1 Phase 1 Functional Requirements (Core MVP — ITR-1 & ITR-4)
P1-FR-01 (Onboarding): Capture PAN (Individual 'P' status) + DOB with dynamic keyboard switching (AAAAA9999A) and Android Keystore StrongBox + SQLCipher (zero_tax_vault.db) vault initialization.
P1-FR-02 (Portal Bridge): Headless/Assisted WebView session to authenticate with ITD Portal via Aadhaar OTP and download Prefill.json, Form 26AS, and AIS/TIS JSON at ₹0 ERI API cost, followed by immediate 500ms cookie sanitization.
P1-FR-03 (PDF Unlocker): Local background worker testing 15 standard PAN/DOB/First-Name password permutations (pan_lower+ddmmyyyy, etc.) in < 100ms without prompting the user.
P1-FR-04 (On-Device AI): Extract Form 16 Part B fields (Sec 17(1), 10(13A) HRA, 16(ia) Std Deduction, 80C, 80CCD(1B), 80CCD(2), 80D) via Android AICore Gemini Nano (temperature = 0.0f, topK = 1) with deterministic TRACES regex fallback and ±₹5 Post-AI Math Sanity Firewall.
P1-FR-05 (Reconciliation): De-duplicate multiple employers by 10-character TAN; cap claimed TDS strictly at Form 26AS deposited values; auto-populate Schedule OS savings interest from AIS SFT-005 and claim Sec 80TTA (<= ₹10,000) or Sec 80TTB (<= ₹50,000 for Senior Citizens).
P1-FR-06 (44ADA Engine): Detect Sec 194J / 194C receipts in Form 26AS/AIS and auto-route to ITR-4 (Sugam) with 50% presumptive profit default under Sec 44ADA (up to ₹50L standard limit or ₹75L enhanced limit with >= 95% digital receipts).
P1-FR-07 (Regime & Export): Compute Old vs. New Regime side-by-side (including Sec 87A rebate, marginal relief, and Sec 80CCD(2)), validate generated ITR-1 / ITR-4 JSON against official CBDT Schema, upload via WebView Bridge, and trigger Aadhaar OTP e-Verification.
6.2 Phase 2 Functional Requirements (Scale — ITR-2, Account Aggregator & UPI Challan 280)
P2-FR-01 (Account Aggregator): Integrate RBI-regulated AA flow (ReBIT ECDHE-Curve25519 + HKDF-SHA256 + AES-256-GCM) to fetch 12-month Bank Ledgers and CAMS/KFintech/Depository CAS JSONs via single OTP consent with immediate RAM zero-wiping.
P2-FR-02 (Broker Tax P&L Parser): Local Excel/PDF parser + On-Device Gemini Nano structurer for CAMS, KFintech, Zerodha, Groww, AngelOne, and ICICI Direct Tax P&L statements (500+ rows in < 1.5s).
P2-FR-03 (Schedule 112A & CG Engine): Deterministic ISIN-level calculation for Equity LTCG (Sec 112A above ₹1.25L exemption), STCG (Sec 111A @ 20%), Debt MF (Sec 50AA slab rate), Pre-July 23, 2024 Immovable Property LTCG (Sec 112(1)(a) min(12.5% unindexed, 20% CII 376 indexed)), and offline Jan 31, 2018 Grandfathering FMV database lookup (~1.2 MB).
P2-FR-04 (Loss Set-Off & Carry Forward): Auto-compute Schedule CYLA (strictly blocking LTCL from offsetting STCG under Sec 70(3)) and pull prior 8 years' brought-forward losses from Prefill.json into Schedule BFLA & Schedule CFL.
P2-FR-05 (Multi-House Property — Schedule HP): Support 2+ properties, auto-calculating 30% standard deduction under Sec 24(a), home loan interest caps (₹2,00,000 in Old Regime u/s 24(b) & 71(3A)), and co-owner percentage splits.
P2-FR-06 (UPI Challan 280 Bridge): When Net Tax Payable > ₹0, initiate Challan 280 (Major Head 0021, Minor Head 300) on ITD portal, invoke Android UPI Intent (upi://pay), download Challan receipt PDF, and auto-inject 7-digit BSR Code, Date of Deposit, and 5-digit Challan Serial No into Schedule IT.
P2-FR-07 (On-Device Bank Classifier): Use Gemini Nano to batch-classify bank narration strings from AA statements (pre-filtering out UPI debits < ₹500) to surface missed 80C, 80D, 80G, 44ADA, and SAVINGS_INTEREST items.
6.3 Phase 3 Functional Requirements (Pro Moat — Schedule FA, Form 67, Vernacular Copilot & ITR-U)
P3-FR-01 (Foreign ESOP / Schedule FA Engine): Parse E*TRADE, Fidelity, Charles Schwab, and Morgan Stanley statements; compute Calendar Year (Jan 1 – Dec 31) Initial Value, Peak Value, and Closing Value in INR using the bundled 10-year offline SBI Telegraphic Transfer Buying Rate (TTBR) SQLite database (with weekend/holiday rollback) for Schedule FA Table A3.
P3-FR-02 (DTAA & Form 67 Automator): Calculate Foreign Tax Credit (Schedule TR & Schedule FSI) for US dividend/capital gain withholding tax (Form 1042-S) using Rule 128(5) preceding-month-end SBI TTBR and auto-generate Form 67 payload capped at min(Foreign_Tax_Paid, Indian_Tax_Payable).
P3-FR-03 (Multilingual AI Notice Explainer): On-device Gemini Nano token-locked pipeline translating complex CPC notices (Sec 143(1), Sec 139(9), Sec 148, High-Value AIS e-Campaigns) into plain English (en-IN), Hindi (hi-IN), and Gujarati (gu-IN) with 100% byte-for-byte preservation of rupee and section tokens.
P3-FR-04 (AIS Discrepancy Feedback Submitter): Allow users to submit structured AIS feedback ("Information is correct", "Information is not fully correct", "Information relates to other PAN/Year", "Information is duplicate") directly through the Portal Bridge and store the AIS Feedback Acknowledgment Receipt in the local vault.
P3-FR-05 (Revised 139(5) & Updated ITR-U 139(8A) Returns): Auto-prefill Original Acknowledgment Number & Filing Date for Revised Returns (Sec 139(5)) and compute 25% (within 12 months) or 50% (12–24 months) additional tax u/s 140B for Updated Returns (ITR-U Sec 139(8A)).
P3-FR-06 (Advance Tax & March LTCG Harvesting Radar): Quarterly local background worker (June 15, Sept 15, Dec 15, March 15) alerting users if estimated tax liability exceeds ₹10,000 (preventing Sec 234B/234C interest) and flagging unused headroom below the ₹1,25,000 Sec 112A LTCG exemption between Feb 15 and March 28.

7. End-to-End Sub-3-Minute Latency Budget & Non-Functional Architecture Matrix

Tap Step
Pipeline & Sub-Module
User Budget
Engine Latency SLA
Architectural Guarantee
Tap 1
Identity & Hardware Vault Init
< 15 sec
< 120 ms
Android Keystore StrongBox AES-256-GCM + SQLCipher initialization
Tap 2
Portal Sync, 15-Perm PDF Unlocker & AICore Extraction
< 45 sec
< 80 ms (Unlock)
< 30 ms (Regex)
< 1,500 ms/pg (NPU)
15-permutation unlocker; AICore Gemini Nano (temp=0.0f, topK=1); < 500ms cookie wipe
Tap 3
Exception Nudge Card Resolution & AA Narration Sweep
< 60 sec
< 1,500 ms (500 rows)
< 400 ms (AA Decrypt)
Offline Jan 31 2018 FMV lookup; Curve25519 + HKDF-SHA256 + AES-256-GCM zero-wipe
Tap 4
Regime Showdown, UPI Challan 280 & 1-Tap e-Verify
< 40 sec
< 15 ms (Dual-Regime)
< 50 ms (JSON schema)
Deterministic pure Kotlin tax calculation & CBDT Category 'A' JSON schema verification

Tap 1 (Identity & Hardware Vault Init): Budgeted for < 15 seconds of user interaction. Engine executes StrongBox hardware-backed AES-256-GCM master key generation and initializes the encrypted SQLCipher local database (zero_tax_vault.db) in < 120ms.
Tap 2 (Portal Sync, 15-Perm PDF Unlocker & AICore Extraction): Budgeted for < 45 seconds total time including Aadhaar OTP entry. Executes the 15-permutation silent PDF unlocker via PdfBox-Android in < 80ms, deterministic TRACES coordinate/regex extraction in < 30ms, on-device Tier-1 NPU Gemini Nano extraction at < 1,500ms per page (temperature = 0.0f, topK = 1), and strictly enforces session termination with cookie wiping in < 500ms post-ingestion.
Tap 3 (Exception Nudge Card Resolution & AA Narration Sweep): Budgeted for < 60 seconds user verification. Runs high-throughput lot parsing for 500+ Broker Tax P&L and CAMS CAS rows with offline Jan 31, 2018 FMV lookups in < 1,500ms, accompanied by < 400ms Ephemeral Curve25519 + HKDF-SHA256 + AES-256-GCM Account Aggregator decryption with immediate in-memory zero-filling.
Tap 4 (Regime Showdown, UPI Challan 280 & 1-Tap e-Verify): Budgeted for < 40 seconds final review and payment authorization. Deterministic Dual-Regime math plus Schedule CG, CYLA, CFL, and FA calculations finish in < 15ms, followed by CBDT Category 'A' JSON payload compilation and schema validation in < 50ms.

8. Senior Chartered Accountant & Fintech Legal Counsel Statutory Audit & Compliance Sign-Off Ledger
8.1 Direct Tax Statutory Audit Matrix (Income-tax Act, 1961 & Finance Acts)
Section 115BAC(1A) Dual-AY Slab & Section 87A Marginal Relief Verification: Audited for both AY 2025-26 (₹7,00,000 rebate ceiling; ₹25,000 max rebate; marginal relief up to ₹7,27,770) and AY 2026-27 (₹12,00,000 normal income rebate ceiling; ₹60,000 max rebate; marginal relief up to ₹12,75,000). Under Section 112A(6), equity LTCG taxed at 12.5% is strictly excluded from Section 87A rebate eligibility.
Section 192(2B), Section 16(ia) & Section 143(1)(a) Protection: Multi-employer salary aggregation is keyed by 10-character Deductor TAN, enforcing a single ₹75,000 (New Regime) or ₹50,000 (Old Regime) Standard Deduction and capping Schedule TDS1 claims strictly at Form 26AS Part A deposited amounts.
Capital Gains Grandfathering & Loss Set-Off Matrix: Enforces Section 55(2)(ac) ISIN-level January 31, 2018 grandfathering (Deemed Cost = max(Actual Cost, min(FMV_2018, Sale Price))), Second Proviso to Section 112(1)(a) pre-July 23, 2024 immovable property LTCG tax comparison (min(12.5% unindexed, 20% CII-indexed at CII 376)), and Section 70(3)/Section 74 isolation blocking Long-Term Capital Loss (LTCL) from offsetting Short-Term Capital Gains (STCG).
Black Money Act, 2015 (Sections 42 & 43) & Rule 128(5) Foreign Tax Credit: Enforces Calendar Year (Jan 1 – Dec 31) disclosure in Schedule FA Table A3 using historical SBI TTBR with weekend/holiday rollback, and preceding-month-end SBI TTBR for Form 67 FTC claims.
8.2 Fintech Regulatory & Privacy Compliance Sign-Off (DPDP Act, RBI AA & UIDAI)
Section 139(1B) Non-ERI Browser Assistant Classification: Executes strictly inside an isolated, no-cache Android WebView connecting directly to eportal.incometax.gov.in with 500ms session cookie destruction and zero server-side credential custody.
DPDP Act, 2023 (Sections 6, 8(5) & 12(3)): Enforces localized itemized consent (en-IN, hi-IN, gu-IN), hardware-backed StrongBox AES-256-GCM + SQLCipher 4.5.x storage, and 1-Tap cryptographic erasure (Shred Local Tax Vault) in under 300ms.
UIDAI Aadhaar Act Section 29 & RBI NBFC-AA Master Directions: Redacts all 12-digit Verhoeff-valid Aadhaar strings to "[Aadhaar Redacted]" prior to AI inference or SQLite persistence, and zero-wipes (0x00) all Curve25519/AES-256-GCM buffers in volatile RAM inside a mandatory finally block.

8.3 Small Business, MSME & Professional Tax Audit Sign-Off (Sections 44AB, 44AD, 44ADA & Form 3CD)
Section 44AB Statutory Threshold Automation: Evaluates the ₹10 Crore threshold versus the ₹1 Crore threshold by computing the 5% cash receipt and 5% cash payment test. Enforces Section 271B penalty protection (0.5% capped at ₹1,50,000) with September 30 audit report and October 31 ITR filing deadlines.
Presumptive Compliance & Lock-In Protection (Sections 44AD & 44ADA): Enforces the 6% digital and 8% cash deemed profit rates for businesses up to ₹3 Crores, the 50% deemed profit rate for professionals up to ₹75 Lakhs, and monitors the Section 44AD(4) 5-year lock-in debarment penalty.
Form 3CD Clause Add-Back Verification: Scans and adds back Section 43B(h) MSME overdue payments (>45 days), Section 40A(3) single-day cash expenses (>₹10,000), Section 40(a)(ia) 30% TDS non-deduction, and Section 43B unpaid statutory dues to compile verified Chartered Accountant handover dossiers.

8.4 Next-Gen Feature Expansions & Statutory Automation Sign-Off (Phases 4.4 to 4.7)
Non-Individual Corporate & Trust Entities (ITR-5, ITR-6, ITR-7): Enforces Section 40(b) partner remuneration and interest caps, Section 115JC AMT, Section 115JB MAT book profit additions, Section 115BAA/BAB concessional rates, and Section 11/12 trust application (85% rule) with Form 9A/10 declarations and MMR taxation u/s 115BBI.
Plan-Based Fast-Start Dashboards & Biometric Shield Gate: Full separation into SAHAJ (ITR-1 plain language), SUGAM (ITR-4 presumptive deck), and ENTERPRISE AUDIT (ITR-2 to ITR-7 corporate desk) with biometric authentication gate (fingerprint/face/PIN) and July 31 timeline countdown notification engine.
In-App Live ITD Portal Sync Suite: Direct TLS handshake via Aadhaar OTP or password pulling 26AS, AIS/TIS, Pre-fill JSON, and refund status in-app with 0 external browser logins.
F&O Trading & Tax Audit Engine (Section 31): Implements ICAI Guidance Note turnover math (Sum(|P&L|) + Option Premium), Section 73 speculative loss ring-fence (4-year carry forward), Section 71(2A) salary set-off bar (8-year carry forward u/s 72), and Section 44AB ₹10 Crore digital threshold with 44AD(4)/(5) lock-in checks.
Section 143(1) Intimation Diff & Section 154 Rectification Builder (Section 33.1): Automatic Column A vs Column B reconciliation flagging zero discrepancy, tax credit mismatches, and income omissions, generating instant CBDT Section 154 JSON payloads.
Zero-Knowledge .ztvault Encrypted Device Migration (Section 33.3): PBKDF2-HMAC-SHA512 (256,000 iterations) + AES-256-GCM encrypted backup and Quick Share transfer for 8-year CFL loss ledgers and return archives with 100% zero-server privacy.
Senior Citizen Section 194P Exemption & Form 12BBA Generator (Section 34): Evaluates Age 75+ resident pensioners with single-bank interest, exempting them from ITR filing u/s 139 via Form 12BBA, alongside Section 80TTB ₹50,000 deduction and Section 207(2) advance tax immunity.
Hybrid Multi-Source Income & Progressive Disclosure Engine (Section 38): Synthesizes Salaried + Moonlighting + Freelancing (44ADA 50%) + Creator (44AD 6%/8%) + Casual gigs (Sec 56), dynamically showing only relevant cards and seamlessly auto-routing to ITR-1, ITR-4, ITR-2, or ITR-3 Lite behind the scenes.
6-Menu Chip-Driven Copilot & 3-Tier AI Pipeline (Section 35): Guided quick-pick action pills eliminating empty text inputs, backed by Tier-0 deterministic regex routing (< 2ms, 0 tokens), Tier-1 memoization, and Tier-2 KV-cached micro-prompts.

8.5 Chartered Accountant & Auditor Portal, Think Like a CA Deduction Maximizer & Zero-Knowledge Trust Shield Sign-Off
Dedicated Auditor & Chartered Accountant Portal: Dedicated login flow supporting ICAI membership authentication, client pairing management, and statutory audit review. Clients generate single-use 6-digit cryptographic pairing tokens (format: ZT-XXXX) valid for 15 minutes, allowing their designated CA to attach client returns to their roster without sharing raw passwords or cloud credentials.
Statutory Audit Review & UDIN Sign-Off: CAs can review client income schedules, deduction claims, and computed tax liability across both Old and New Tax Regimes. Upon completing statutory verification, the CA records observations, generates a compliant UDIN (Unique Document Identification Number) draft, and issues a cryptographically timestamped statutory audit approval payload.
Think Like a CA Deduction Maximizer Engine: Systematic heuristic deduction optimization under Chapter VI-A and Head IV-B to aggressively convert tax payable into an Income Tax Refund. Covers Section 80CCD(2) employer NPS (up to 14% of basic + DA in both regimes), Section 80D health insurance (up to ₹1,00,000 for senior citizen parents + self/family), Section 24(b) housing loan interest (₹2,00,000 cap), Section 80CCD(1B) Tier 1 NPS (₹50,000), Section 80C statutory investments (₹1,50,000), Section 80E higher education loan interest, Section 80GG rent paid where no HRA is received, Section 80TTA/TTB savings/deposit interest exemptions, Section 56(2)(x) & 10(13A) family tax arbitrage, and Section 87A rebate compression ensuring zero net tax payable where feasible.
DPDP Act 2023 Section 6 Zero-Knowledge Trust Shield: Comprehensive user transparency and brand assurance modal detailing on-device cryptographic boundaries. Establishes a strict Transparency Matrix distinguishing what ZeroTax servers see (Absolute Zero - 0 bytes, no storage, no telemetry) versus what remains protected on-device (AES-256-GCM hardware-backed vault, biometric protection, ephemeral session tokens). Includes an interactive user acknowledgement flow compliant with DPDP Act 2023 Section 6 consent guidelines.

