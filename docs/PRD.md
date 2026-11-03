Product Requirements Document: Zero-Friction On-Device AI ITR Filing App ("ZeroTax-Local" — CA & Legal Edition)
0. CA & Fintech Legal Counsel Review Summary
The following architectural and statutory foundations have been updated to ensure full compliance across Chartered Accountancy standards, the Income-tax Act, 1961, the Digital Personal Data Protection (DPDP) Act, 2023, and UIDAI security baselines:

Explicit Non-ERI & DPDP Act Legal Positioning: Established statutory classification as a User-Initiated Client-Side Browser Assistant under Section 139(1B) of the Income-tax Act, 1961. This bypasses intermediary classifications. DPDP Act (Sections 6 & 12(3)) compliance provides explicit, itemized user consent alongside a 1-Tap "Shred Local Tax Vault" hardware key erasure.
Expanded Target Personas (P1–P5): Extended coverage to include high-complexity retail cases: P4 (MNC SWE with foreign RSUs requiring Calendar-Year Schedule FA reporting and Rule 128(5) Foreign Tax Credit calculations) and P5 (Senior Citizens under Section 80TTB with Section 207 advance tax interest immunity).
Deterministic Tax Engine Matrix (AY 2025-26 & AY 2026-27): Codified strict mathematical logic covering progressive tax brackets, Section 87A rebate and marginal relief formulas, Section 112A(6) special rate carve-outs, Section 55(2)(ac) grandfathering computations, and pre-July 23, 2024 unindexed vs. indexed real estate capital gains optimization.
Statutory Penalty Shields: Integrated in-flow protection mechanisms including the Employer TDS Protection Shield (capping claims at Form 26AS Part A values), the Rule 2A Landlord PAN Gate, Section 270A penalty advisories, and Section 140 verification checks.
1. Product Vision, Problem Statement & Regulatory Positioning
1.1 Product Vision
Enable any Indian individual taxpayer ('P' status PAN) to file an accurate, statutorily compliant, maximum-refund Income Tax Return (ITR-1, ITR-2, ITR-4, or ITR-U) in under 3 minutes and 4 taps, with near-zero manual data entry and 100% on-device data execution.
1.2 The Indian Retail Tax Filing Trilemma
Schedule Complexity & Automated CPC Notices: The official e-Filing portal exposes interdependent schedules (Schedule S, HP, BP, CG, 112A, FA, FSI, TR, CYLA, BFLA, CFL, 80D, 80G, AL, IT, TDS1/TDS2/TCS). Double-counting Section 16(ia) standard deductions across successive employers (Section 192(2B)), claiming TDS unreflected in Form 26AS Part A, or misaligning foreign asset reporting periods triggers automated Section 139(9) defective return notices, Section 143(1) demand adjustments, or Section 270A under-reporting penalties.
Cloud Intermediary Privacy Exploitation: Web-based tax aggregators ingest unredacted Form 16 documents, financial PDFs, and investment statements into remote server environments to profile users for third-party loan and insurance underwriting—violating data minimization rules under Sections 4 and 6 of the DPDP Act, 2023.
Cloud LLM Arithmetic Hallucinations: Generative natural language models frequently misapply progressive marginal slab math, fail to apply Section 87A relief boundaries, and overlook Section 112A(6) non-rebate exceptions.
1.3 Statutory & Regulatory Positioning
Non-ERI Client-Side Browser Assistant (Section 139(1B)): ZeroTax-Local operates as an automated browser assistant executing directly within an isolated, sandboxed Android WebView. Connections route straight from the client device to https://eportal.incometax.gov.in. ZeroTax backend infrastructure maintains a zero-knowledge profile: passwords, session cookies, and multi-factor auth tokens are discarded within 500ms of JSON synchronization.
DPDP Act, 2023 On-Device Vault (Sections 6, 8(5) & 12(3)): Itemized granular consents (en-IN, hi-IN, gu-IN) are logged to local storage prior to initializing the encrypted datastore (zero_tax_vault.db using SQLCipher 4.5.x and Android Keystore StrongBox). Users retain unilateral execution rights over the "Shred Local Tax Vault" routine, which securely zeroes the cryptographic keys and rewires target memory addresses via SecureRandom bytes in $< 300\text{ms}$.
UIDAI Aadhaar Act Section 29 Compliance: The embedded UidaiAadhaarVerhoeffScrubber routine redacts all 12-digit Aadhaar instances to mask patterns across optical text capture, dynamic web document trees, and local persistence layers.
2. Deep-Dive Target User Personas (P1 through P5)
Persona
Profile & Income Sources
Statutory Pain Point Solved by ZeroTax-Local
P1: "Aarav" — Job Switcher (ITR-1)
Age 28, Senior SWE in Bengaluru. Switched employers in October. Holds two Form 16s, Section 80C instruments, Section 80CCD(2) Corporate NPS, and active monthly rent expenses.
Deduplicates salary records by distinct 10-character TAN under Section 192(2B), restricts Section 16(ia) Standard Deduction to a single ₹75,000 allocation, factors Section 80CCD(2) Employer NPS under the New Regime, and generates Challan 280 tax values.
P2: "Priya" — Independent Consultant (ITR-4)
Age 32, UI/UX Consultant in Ahmedabad. Domestic Section 194J/194C receipts recorded in Form 26AS, cross-border freelance platform payouts, parent healthcare expenses under Section 80D.
Routes directly to ITR-4 (Sugam), parses foreign freelance credits, enforces 50% minimum presumptive profit margins under Section 44ADA up to the ₹75 Lakh digital threshold, and schedules Section 234C advance tax timelines.
P3: "Rohan" — Multi-Asset Investor (ITR-2)
Age 41, Operations VP in Mumbai. Salary ₹32L, equity short-term capital gains (Section 111A @ 20%), mutual fund LTCG (Section 112A @ 12.5%), pre-2018 holdings, two residential units.
Calculates ISIN-level January 31, 2018 grandfathered cost bases under Section 55(2)(ac) via bundled offline FMV indices, restricts Long-Term Capital Loss offsets against STCG under Section 70(3), and limits Section 71(3A) property loss set-offs to ₹2,00,000.
P4: "Vikram" — MNC Lead Engineer (ITR-2 + Form 67)
Age 36, Principal Architect in Hyderabad. Salary ₹58L, US equity incentive plans (RSUs/ESPPs), US dividend receipts subject to 25% tax withholding under DTAA provisions.
Mitigates Section 42 & 43 Black Money Act liabilities by compiling Schedule FA Table A3 on a strict Calendar Year (Jan 1 – Dec 31) basis using bundled SBI TTBR tables (Rule 115) and auto-populates Form 67 foreign tax credits under Rule 128(5).
P5: "Harshadbhai" — Senior Citizen (ITR-1 / ITR-2)
Age 67, Retired Banking Professional in Vadodara. Monthly pension ₹6.2L, bank fixed deposit interest ₹2.8L, non-insured medical expenses for super-senior dependent (Section 80D).
Switches deductions from Section 80TTA to Section 80TTB (₹50,000 aggregate FD/savings interest allowance), flags Section 207(2) exemptions shielding non-business seniors from Section 234B interest, and localizes alerts via on-device translation.
3. Statutory Tax Rule & Rate Matrix (AY 2025-26 & AY 2026-27)
Statutory Rule & Section
Default New Regime (Sec 115BAC(1A)) — AY 2025-26
Default New Regime (Sec 115BAC(1A)) — AY 2026-27
Old Tax Regime (Optional Opt-Out)
Progressive Income Slabs
₹0–3L: 0%₹3–7L: 5%₹7–10L: 10%₹10–12L: 15%₹12–15L: 20%>₹15L: 30%
₹0–4L: 0%₹4–8L: 5%₹8–12L: 10%₹12–16L: 15%₹16–20L: 20%₹20–24L: 25%>₹24L: 30%
Basic Exemption Thresholds:<60: ₹2.5L; 60–79: ₹3L; ≥80: ₹5L₹2.5L–5L: 5%₹5L–10L: 20%>₹10L: 30%
Section 87A Rebate & Marginal Relief
Up to ₹25,000 if Taxable Income $\le ₹7,00,000$. Marginal relief formula applies to ₹7,27,770.
Up to ₹60,000 if Taxable Income $\le ₹12,00,000$. Marginal relief formula applies to ₹12,75,000.
Up to ₹12,500 if Taxable Income $\le ₹5,00,000$. No marginal relief extension above threshold.
Section 112A(6) Special Rate Exclusion
Section 87A rebate does not offset tax calculated under Section 112A.
Section 87A rebate does not offset tax calculated under Section 112A.
Section 87A rebate does not offset tax calculated under Section 112A.
Standard Deduction (Sec 16(ia))
₹75,000 single aggregate allowance across all employers.
₹75,000 single aggregate allowance across all employers.
₹50,000 single aggregate allowance across all employers.
Family Pension (Sec 57(iia))
Capped at $\min(33.33%\text{ of receipts}, ₹25,000)$.
Capped at $\min(33.33%\text{ of receipts}, ₹25,000)$.
Capped at $\min(33.33%\text{ of receipts}, ₹15,000)$.
Employer NPS (Sec 80CCD(2))
Up to 14% of (Basic Salary + Dearness Allowance).
Up to 14% of (Basic Salary + Dearness Allowance).
Up to 14% (Govt) or 10% (Private) of (Basic Salary + Dearness Allowance).
House Property Loss (Sec 24(b) / 71(3A))
Nil deduction for self-occupied properties (Section 115BAC(2)).
Nil deduction for self-occupied properties (Section 115BAC(2)).
Maximum ₹2,00,000 deduction against current-year heads; unabsorbed amounts carry forward 8 AYs.
Capital Gains Taxation Engine
111A: 20%112A: 12.5% exceeding ₹1.25L50AA Debt: Slab ratesPre-July 23, 2024 Real Estate: $\min(12.5%\text{ unindexed}, 20%\text{ CII }376\text{ indexed})$
111A: 20%112A: 12.5% exceeding ₹1.25L50AA Debt: Slab ratesPre-July 23, 2024 Real Estate: $\min(12.5%\text{ unindexed}, 20%\text{ CII }376\text{ indexed})$
Unchanged statutory definitions under applicable Finance Act provisions.
4. The "4-Tap" User Journey & Statutory Exception Nudge Cards
Tap 1: Identity, Smart PAN Keyboard & DPDP Section 6 Consent (< 15 sec)
Dynamic PAN Input (Section 139A): The input keyboard enforces structured character constraints, shifting automatically across character subsets: Alphabetic (positions 1–5) $\rightarrow$ Numeric (positions 6–9) $\rightarrow$ Alphabetic (position 10). It asserts that the 4th index character strictly matches 'P' (Individual).
DPDP Section 6 Consent Management: Displays explicit, granular processing terms translated across regional locales (en-IN, hi-IN, gu-IN). Upon agreement, generates a local cryptographic SHA-256 ledger record in user_legal_consents_and_overrides and configures the hardware-isolated SQLite environment.
Tap 2: Live Automated Discovery & $\pm ₹5$ Math Sanity Firewall (< 45 sec)
Direct Authentication & Session Clean-up: Establishes a direct-to-portal session to download Prefill.json, Form 26AS, and AIS/TIS source records. Discards active browser context and network session artifacts within 500ms of payload capture.
Multi-Format PDF Processor & Reconciliation: Evaluates 15 structural password permutations against locked Form 16 and financial reports in $< 80\text{ms}$. If an OCR-processed Form 16 introduces arithmetic variances greater than $\pm ₹5$, the UI activates an inline review card for bounding-box resolution.
Tap 3: Swipeable Statutory Exception "Nudge Cards" (< 60 sec)
Card A — Employer TDS Protection Shield: Cross-checks declared Form 16 credits against Form 26AS Part A actual deposits. Automatically caps claimed lines in Schedule TDS1 to the government-verified figure and generates a follow-up communication draft for employer reconciliation.
Card B — Rule 2A HRA Landlord PAN Gate: Identifies annual declared rent claims exceeding ₹1,00,000. Requires landlord PAN input before exposing Old Regime HRA benefits, eliminating Section 270A(9) misreporting assessment risks.
Card C — Section 44ADA Presumptive Declaration Slider: Directs qualifying independent professionals to ITR-4 (Sugam). Constrains gross net income reporting to a default minimum threshold of 50%, requiring structured CA audit confirmation flags to set values below statutory baselines.
Card D — Foreign Holdings Compliance Shield: Converts US equity holdings to domestic currency valuations over standard Calendar Year (Jan 1 – Dec 31) intervals via offline SBI TTBR datasets, generating matching Form 67 filings for foreign tax relief.
Tap 4: Regime Comparison, Provenance Drawer, UPI Bridge & e-Verification (< 40 sec)
Comparative Regime Breakdown: Delivers a deterministic comparative schedule of Old vs. New tax obligations. Every output metric links directly to the Level-3 Provenance Drawer, tracing figures back to raw source files, line items, and statutory definitions.
Integrated UPI Challan 280 Payment Path: Facilitates direct payment linking to registered UPI applications for self-assessment tax dues (Major Head 0021, Minor Head 300). Parses completed counterfoil PDFs to populate BSR codes and Challan references directly into Schedule IT.
Validation Check & Execution: Audits filing payloads against CBDT Category 'A' JSON schema rules, checks the Section 140 formal taxpayer declaration, and completes submission via Aadhaar-linked OTP authentication.
5. Consolidated Functional Requirements & Non-Functional SLAs
Requirement ID
Implementation Phase
Statutory & Architectural Scope
Performance & Compliance SLA
P1-FR-01 to 07
Phase 1 (MVP)
Dynamic individual PAN validation; DPDP Sec 6 consent management; isolated portal bridge with 500ms session cleanup; 15-permutation PDF decryption; dual Form 16 deduplication under Section 192(2B); Form 26AS Part A TDS claim ceiling; Section 44ADA presumptive parameters; AY 2025-26 & 2026-27 dual-regime tax calculation engine; CBDT JSON schema serialization.
PDF decryption $< 80\text{ms}$; regex processing $< 30\text{ms}$; local on-device OCR inference $< 1.5\text{s/page}$; cookie purge $< 500\text{ms}$; zero cloud transmission costs.
P2-FR-01 to 07
Phase 2 (Scale)
RBI Account Aggregator protocol integration (Curve25519 payload decryption with direct RAM zeroization); equity trading summary extraction; Section 112A historical FMV reference engine; pre-July 23, 2024 real estate indexation analyzer (CII 376); loss carry-forward matrix (Schedules CYLA/BFLA/CFL); multi-unit property calculations; UPI Challan 280 integration.
Reconciliation of 500+ trade lots in $< 1,500\text{ms}$; AA payload processing and memory clearing executed in $< 400\text{ms}$.
P3-FR-01 to 06
Phase 3 (Moat)
Schedule FA Table A3 foreign asset reporting (Calendar Year basis) with historical SBI TTBR tables (Rule 115); Form 67 Foreign Tax Credit compilation (Rule 128(5)); on-device notice analysis module for Section 143(1) and 139(9) communications; AIS dispute logging; updated return processing under Section 139(8A) / 140B; equity tax-loss harvesting tracking.
Zero variance (₹0.00) against all baseline validation test suites; verified operational integrity under isolated offline handset execution (Airplane Mode).

