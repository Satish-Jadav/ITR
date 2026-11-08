# System Architecture Document: Zero-Friction On-Device AI ITR Filing App ("ZeroTax-Local")

## 0. Principal Architect, CA & Legal Counsel Review Summary

* **Eliminated Cloud Vertex AI Fallback Egress Risk:** Replaced cloud LLM fallbacks with a 100% On-Device Deterministic TRACES Coordinate & Regex Fallback Parser (< 80ms) so Tier-2 mid-range devices never transmit raw financial PDFs or PII off-device (0B cloud egress, full DPDP Act 2023 Section 8(5) compliance).
* **Upgraded 12-Permutation PDF Unlocker to 15-Permutation Matrix:** Added missing Indian banking and depository password patterns (`first4+ddmm`, `PAN+yyyy`, and `ddmmyyyy+PAN`) required by HDFC, ICICI, Axis, SBI, and NSDL/CDSL CAS statements.
* **Hardened Non-ERI Client-Side Portal Bridge (Section 139(1B)):** Added strict domain allowlisting (`eportal.incometax.gov.in`, `www.incometax.gov.in`, `contents.tdscpc.gov.in`, `ais.insight.gov.in`), a 15-second watchdog timer, and a mandatory 500ms cookie/DOM storage wipe upon payload capture.
* **Integrated ReBIT Sahamati Account Aggregator Ephemeral Cryptography:** Specified the full X25519 (Curve25519) ECDHE key exchange, XORed 32-byte nonce salt, HKDF-SHA256 key derivation, AES-256-GCM decryption in volatile RAM, and mandatory `0x00` byte zeroization in a `finally` block under the RBI NBFC-AA Master Directions, 2016.
* **Enforced UIDAI Section 29 Verhoeff Checksum Scrubbing:** Added `UidaiAadhaarVerhoeffScrubber` across OCR, WebView, and SQLite boundaries to replace every 12-digit Aadhaar number with `[Aadhaar Redacted]` prior to persistence or AI inference.

---

## 1. Executive Summary & Architectural Vision

ZeroTax-Local redefines Indian Income Tax Return (ITR-1, ITR-2, ITR-4, and ITR-U) filing by moving away from legacy "50-field manual form entry" toward an **Automated On-Device Aggregator + Exception Nudge Handler** paradigm.

### Core Architectural Invariants

1. **100% On-Device AI Processing (`com.google.ai.edge.aicore` / Gemini Nano):**
   Unstructured financial documents (non-standard Form 16 Part B annexures, physical rent receipts, medical insurance bills u/s 80D, charitable donation slips u/s 80G, and Account Aggregator bank narrations) are parsed locally on the handset's NPU/TPU at `temperature = 0.0f` and `topK = 1`, immediately gated by a Post-AI Math Sanity Firewall ($\pm ₹5$ tolerance).

2. **100% Deterministic Pure-JVM Tax & Reconciliation Engine (`:core:tax-engine`):**
   Strict multi-module Gradle firewall (`kotlin("jvm")`) prohibiting Android SDK or LLM dependencies inside `:core:tax-engine`. Every progressive slab calculation (for both AY 2025-26 and AY 2026-27), Section 87A rebate and marginal relief equation, Section 112A(6) special-rate rebate exclusion, Rule 2A HRA exemption, Section 55(2)(ac) Jan 31, 2018 equity grandfathering, Second Proviso to Section 112(1)(a) immovable property indexation comparison (12.5% unindexed vs. 20% with CII 376), Sections 70/71/71(3A)/74 loss set-off matrix, Rule 115 / Rule 128(5) SBI TTBR foreign asset conversion, and Sections 288A/288B & Rule 119A rounding rules execute deterministically with ₹0.00 variance.

3. **Zero-API-Cost Client-Side Browser Assistant (Non-ERI Positioning):**
   Operates as a User-Initiated Client-Side Browser Assistant inside an isolated local Android WebView connecting directly to `https://eportal.incometax.gov.in` via user-entered Aadhaar/Portal OTP, pulling `Prefill.json`, Form 26AS, and AIS/TIS JSONs and uploading the validated CBDT JSON at ₹0 middleman API cost.

---

## 2. High-Level System Architecture & Multi-Module Gradle Topology

```
+---------------------------------------------------------------------------------------------------+
|                           USER ANDROID DEVICE (100% On-Device Processing)                         |
+---------------------------------------------------------------------------------------------------+
|  +-------------------------+    +--------------------------+    +------------------------------+  |
|  |   Jetpack Compose UI    |    |  Smart Local Doc Sweep   |    | Hardened Ephemeral WebView   |  |
|  | - 4-Tap Linear Spine    |    | - SAF / MediaStore PDF   |    | - ITD Portal Sync (<500ms    |  |
|  | - 3-Tab Pro Workspace   |    | - 15-Perm PDF Unlocker   |    |   Cookie/DOM Wipe)           |  |
|  | - FLAG_SECURE Host      |    | - ML Kit Doc Scanner v2  |    | - UPI Challan 280 Bridge     |  |
|  +------------+------------+    +-------------+------------+    +--------------+---------------+  |
|               |                               |                                |                  |
|               v                               v                                v                  |
|  +---------------------------------------------------------------------------------------------+  |
|  |                UIDAI Aadhaar Verhoeff Scrubber (Redacts 12-Digit Aadhaar ->                 |  |
|  |                "[Aadhaar Redacted]" Across All OCR, WebView & Room Streams)                 |  |
|  +------------------------+----------------------------------------+---------------------------+  |
|                           |                                        |                              |
|                           v                                        v                              |
|  +------------------------------------------------+   +----------------------------------------+  |
|  |   On-Device Intelligence Layer (:feature:ai)   |   |  RBI Account Aggregator Crypto Engine  |  |
|  | - Android AICore (Gemini Nano temp=0.0, topK=1)|   | - Ephemeral X25519 (Curve25519) ECDHE  |  |
|  | - Deterministic TRACES Coordinate/Regex Parser |   | - HKDF-SHA256 + AES-256-GCM in RAM     |  |
|  | - Post-AI Math Sanity Firewall (±₹5 Tolerance) |   | - Mandatory finally-block 0x00 Wipe    |  |
|  | - Token-Locked Vernacular Copilot (EN/HI/GU)   |   +--------------------+-------------------+  |
|  +------------------------+-----------------------+                        |                      |
|                           |                                                |                      |
|                           +-----------------------+------------------------+                      |
|                                                   |                                               |
|                                                   v                                               |
|  +---------------------------------------------------------------------------------------------+  |
|  |                  Pure-JVM Deterministic Tax Engine (:core:tax-engine)                       |  |
|  | - 14-Gate ITR Form Auto-Router (ITR-1, ITR-4, ITR-2, ITR-3, 44AB Audit Gate)                |  |
|  | - Multi-Employer TAN De-Duplicator (Sec 192(2B)) & Strict Form 26AS TDS Ceiling             |  |
|  | - Dual-Regime Comparator (AY 2025-26 ₹7L & AY 2026-27 ₹12L 87A + Surcharge Marginal Relief) |  |
|  | - Capital Gains (111A, 112A Grandfathering, 112(1)(a) Property CII 376, CYLA/BFLA/CFL)      |  |
|  | - Schedule FA Calendar-Year (Jan-Dec) SBI TTBR Engine & Form 67 Rule 128(5) FTC Calculator  |  |
|  | - CBDT Category 'A' Defect Gate & Official JSON Schema Compiler (ITR-1/2/4/U)               |  |
|  +------------------------------------------------+--------------------------------------------+  |
|                                                   |                                               |
|                                                   v                                               |
|  +---------------------------------------------------------------------------------------------+  |
|  |       Hardware-Backed Encrypted Vault (:core:database — Room + SQLCipher 4.5.x)             |  |
|  | - Android Keystore StrongBox AES-256-GCM Wrapped Passphrase                                 |  |
|  | - PRAGMA cipher_memory_security = ON; PRAGMA secure_delete = ON;                            |  |
|  | - Bundled Offline Jan 31, 2018 FMV DB (~1.2 MB), CII Table & 10-Year SBI TTBR DB            |  |
|  | - 1-Tap DPDP Act Sec 12(3) Cryptographic Shredder (<300ms Key Destruction + Random Wipe)    |  |
|  +---------------------------------------------------------------------------------------------+  |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. Core Architectural Layers & Statutory Pipelines

### 3.1 Layer 1: The 3-Pillar Data Ingestion Layer

#### Pillar A — Hardened Client-Side ITD Portal Bridge (`:feature:portal-bridge`)
* **Legal & Architectural Mechanism:** Runs inside a no-cache, isolated Android WebView (`WebSettings.LOAD_NO_CACHE`, `allowFileAccess = false`, `saveFormData = false`) restricted strictly to `ALLOWED_HOSTS = ["eportal.incometax.gov.in", "www.incometax.gov.in", "contents.tdscpc.gov.in", "ais.insight.gov.in"]`.
* **Statutory Artifacts Captured:**
  * `Prefill.json`: Personal profile, employer salary schedules, bank accounts (Schedule BA), and brought-forward loss schedules (Schedule CFL from prior 8 AYs).
  * `Form 26AS` (Part A–G): Authoritative tax credit statement for TDS-192 (Salary), TDS-194A (Interest), TDS-194J/194C (Professional/Contract), TDS-194IA (Property), TCS-206C(1G) (Foreign Remittance LRS), and Advance/Self-Assessment tax (Challan 280).
  * `AIS` & `TIS`: SFT-005 (Savings Bank Interest), SFT-006 (Dividends), SFT-016/017/018 (Mutual Fund & Equity Off-Market/On-Market Redemptions), and INT-244A (Income Tax Refund Interest).
* **500ms Ephemeral Session Destruction:** Within 500ms of capturing the three payloads in memory, `CookieManager.getInstance().removeAllCookies(null)` and `WebStorage.getInstance().deleteAllData()` execute automatically.

#### Pillar B — RBI Account Aggregator Ephemeral Cryptography (`:feature:aa-aggregator`)
* **Regulatory Compliance:** Conforms to the RBI Master Direction — Non-Banking Financial Company - Account Aggregator (Reserve Bank) Directions, 2016 and ReBIT Sahamati FIData specifications.
* **In-Memory Forward Secrecy (`RbiAaEphemeralDecryptor`):**
  * Generates an ephemeral X25519 (Curve25519) key pair in volatile RAM per consent session.
  * Computes the 32-byte session salt by XORing `fipNonce32Bytes` and `fiuNonce32Bytes`.
  * Executes RFC 5869 HKDF-SHA256 (`HmacSHA256` Extract-and-Expand) over the ECDHE shared secret to derive a 256-bit AES key, using bytes `[20..31]` of the XORed nonce as the 96-bit GCM IV.
  * Decrypts FIData in volatile memory, extracts interest and capital gain lots, and unconditionally overwrites `sharedSecretBytes`, `saltXorNonce`, `derivedKey`, and `plaintextBytes` with `0x00` inside a `finally` block.

#### Pillar C — 15-Permutation Silent PDF Unlocker (`:feature:pdf-unlocker`)
* Tests all 15 standard password permutations in memory on `Dispatchers.Default` in < 80ms without prompting the user:
  1. `lowercase(PAN) + ddmmyyyy`
  2. `UPPERCASE(PAN) + ddmmyyyy`
  3. `ddmmyyyy`
  4. `lowercase(PAN)`
  5. `UPPERCASE(PAN)`
  6. `lowercase(first4Name) + ddmmyyyy`
  7. `UPPERCASE(first4Name) + ddmmyyyy`
  8. `lowercase(first4Name) + ddmm`
  9. `UPPERCASE(first4Name) + ddmm`
  10. `lowercase(PAN) + yyyy`
  11. `UPPERCASE(PAN) + yyyy`
  12. `lowercase(PAN) + ddmmyy`
  13. `UPPERCASE(PAN) + ddmmyy`
  14. `ddmmyyyy + lowercase(PAN)`
  15. `ddmmyyyy + UPPERCASE(PAN)`

---

## 4. Deterministic Pure-JVM Tax Engines & Statutory Sub-Engines

| Component | Statutory References | Core Architectural Invariant |
|---|---|---|
| `DeterministicReconciler` | Sec 192(2B), Sec 16(ia), Sec 143(1)(a) | Groups multi-employer salary records by normalized 10-char TAN, applies Sec 16(ia) Std Deduction strictly once, and caps Schedule TDS1 strictly at $\min(\text{Form 16 TDS}, \text{Form 26AS Part A Deposited TDS})$. |
| `DeterministicIndianTaxEngine` | Sec 115BAC(1A), Sec 87A, Sec 80CCD(2), Sec 80G | Evaluates both AY 2025-26 (₹7L 87A ceiling) and AY 2026-27 (₹12L 87A ceiling) New Regime profiles alongside Old Regime, Sec 87A marginal relief, Sec 112A(6) rebate bar on Equity LTCG, 4-tier Surcharge marginal relief, and Sec 80G caps. |
| `CapitalGainsAndForeignAssetEngine` | Secs 111A, 112A, 50AA, 55(2)(ac), 112(1)(a) | Computes ISIN-level Jan 31, 2018 grandfathered cost $\max(\text{Actual}, \min(\text{FMV}_{2018}, \text{Sale}))$, compares property tax $\min(12.5\%\text{ unindexed}, 20\%\text{ CII }376)$, blocks LTCL from offsetting STCG u/s 70(3), and tracks 8-year loss carry forwards. |
| `HraAndInterestEngine` | Sec 10(13A) Rule 2A, Secs 207, 234A/B/C, Rule 119A | Enforces ₹1,00,000 rent Landlord PAN gate, truncates unpaid principal to nearest ₹100 u/s Rule 119A(b), exempts senior citizens u/s 207(2), and computes quarterly 234C safe harbors. |
| `ScheduleFaCalendarYearEngine` | Black Money Act Secs 42 & 43, Rule 115, Rule 128(5) | Computes Calendar Year (Jan 1 – Dec 31) Initial, Peak, and Dec 31 Closing values in INR for foreign RSUs/ESPPs (Schedule FA Table A3) with weekend/holiday SBI TTBR rollback, and computes Form 67 FTC using preceding-month-end SBI TTBR. |
| `RevisionAndHarvestingEngine` | Sec 139(5), Sec 139(8A), Sec 140B(3) | Computes Updated Return (ITR-U) additional tax at 25% ($\le 12\text{m}$), 50% ($12\text{–}24\text{m}$), 60% ($24\text{–}36\text{m}$), and 70% ($36\text{–}48\text{m}$), and evaluates March Sec 112A LTCG tax-free harvesting headroom. |

---

## 5. Enterprise Design System & Micro-Animation Architecture

* **Sovereign Slate & Deep Teal Color Hierarchy:** Palette strictly adheres to institutional financial design systems (Slate 950/900 background, Slate 50 surface, Enterprise Teal 600/500 brand accent, Emerald Success for verified records, Amber Warning for reconciliation mismatches, Crimson Danger for Section 270A penalties).
* **Hardware Vault Cryptographic Splash Screen:** Animated rotating perimeter ring (`infiniteRepeatable`), inner pulsing padlock, radial ambient teal glow, and live sequential security verification ticker ("StrongBox Keymaster verified", "SQLCipher 4.5.4 mounted", "Aadhaar Verhoeff scrubber armed").
* **Directional Horizontal Slide Transitions:** Stages 1 through 4 transition via directional horizontal slide-and-fade (`slideInHorizontally` + `fadeIn` / `slideOutHorizontally` + `fadeOut` with `FastOutSlowInEasing`), dynamically reversing direction when navigating backward.
* **Tabular Figures & Animated Currency Counters:** Numbers in the Regime Showdown and ledger cards animate with vertical slide transitions (`AnimatedContent`) as inputs update, while stepper segments smoothly morph colors with `animateColorAsState`.
* **Pure-JVM Firewall Verification in CI:** GitHub Actions CI pipeline enforces `grep -rnE "import (android\.|androidx\.|com\.google\.ai\.edge\.aicore)" core/tax-engine/src/main/` rejecting any Android or AI SDK imports into the deterministic calculation core.

---

## 6. Next-Gen Architecture & Sub-Engines (Phases 4.4 to 4.7)

| Component | Module | Statutory / Technical Scope |
|---|---|---|
| `CorporateAndTrustTaxEngine` | `:core:tax-engine` | Non-individual statutory processing: ITR-5 (Partnership/LLP Sec 40(b) & AMT), ITR-6 (Company Sec 115BAA/MAT), ITR-7 (Trust Sec 11/12 application & MMR). |
| `FnOTradingAndTaxAuditEngine` | `:core:tax-engine` | ICAI Guidance Note absolute turnover, Section 73 speculative loss ring-fence (4-year carry forward), Section 71(2A) salary loss bar (8-year carry forward u/s 72), and Section 44AB ₹10 Cr digital audit tree. |
| `Section143_1IntimationDiffAnalyzer` | `:core:tax-engine` | Column A (As Filed) vs Column B (As Computed) diff analyzer, detecting zero discrepancy, credit mismatches, and income additions; generates CBDT Section 154 JSON payloads. |
| `EncryptedZtVaultMigrationEngine` | `:core:security` | Zero-knowledge `.ztvault` backup and Quick Share transfer: PBKDF2 (256,000 iter) key derivation + AES-256-GCM authenticated encryption on GZIP payload. |
| `SeniorCitizenTaxAndExemptionEngine` | `:core:tax-engine` | Section 194P Age 75+ ITR filing exemption check and Form 12BBA declaration generator, alongside Section 80TTB ₹50,000 deduction and Section 207(2) advance tax immunity. |
| `HybridIncomeAndProgressiveDisclosureEngine` | `:core:tax-engine` | Multi-source income combinator (Salary + Moonlighting + 44ADA 50% + 44AD 6%/8% + Casual 56) and progressive disclosure state machine auto-routing to ITR-1, ITR-4, ITR-2, or ITR-3 Lite. |
| `OptimizedTaxCopilotChatbotEngine` | `:feature:ai-extractor` | 6 Guided Quick-Pick Action menus with 3-Tier AI optimization: Tier-0 regex (< 2ms, 0 tokens), Tier-1 memoization, and Tier-2 KV-cached micro-prompts. |
| `ItdLivePortalSyncModal` | `:app` | Direct TLS handshake pulling Form 26AS, AIS/TIS, Pre-fill JSON, and refund status with 0 external browser redirects. |
| `ZeroTaxBiometricShield` | `:app` | AndroidX BiometricPrompt gate securing sensitive return records with 1-tap lock and July 31 countdown notifications. |


---

## 7. 100% Real-Time & Zero-Mock Architecture (The 5 Live Integration Pipelines)
- **Pipeline 1 — Real-Time ITD Portal Pre-fill, AIS & e-Filing**:
  - Live pre-fill ingestion and filing via `HardenedEphemeralPortalBridge.kt` and `ItdDirectFilingClient.kt` with 1-Tap Aadhaar OTP u/s 140A for instant e-verification.
- **Pipeline 2 — Real-Time Document Extraction (On-Device + Cloud AI)**:
  - Form 16 Part A/B, CAMS/KFintech CAS, Broker P&L, and CPC notices extracted via on-device Gemini Nano with cloud fallback to Google Document AI / Gemini 2.0 API.
- **Pipeline 3 — Real-Time RBI Account Aggregator (Setu / Finvu FIU)**:
  - Ephemeral ECDHE-Curve25519 key negotiation with HKDF-SHA256 and AES-256-GCM in-memory decryption for live bank interest and deduction discovery.
- **Pipeline 4 — Live Public Statutory Feeds**:
  - Real-time AMFI Mutual Fund NAV, RBI IFSC validation, and SBI TTBR foreign exchange lookup with automatic offline cached fallback.
- **Pipeline 5 — Unified Reactive Vault State**:
  - Unifies all 8 toolkit wizards, what-if simulator, and Conversational Copilot with real-time `zero_tax_vault.db` state.

---

## 8. Dual-Repository Cloud Topology & 3-Tier Driver Gateway Priority
- **Repository Structure**:
  - `ITR`: Kotlin / Jetpack Compose Android client with local SQLCipher encrypted vault and on-device AI.
  - `ITR-API`: Node.js / Hono / Supabase cloud backend with Edge VAPT Firewall and B2B gateway integration.
- **3-Tier Driver Priority**:
  - **QUICKO_ERI**: Sandbox.co.in / Quicko B2B Tax API Gateway (live token via `POST /authenticate`, 23-hour cache).
  - **CBDT_DIRECT_ERI**: Direct CBDT IEC 2.0 Gateway using corporate DSC private key.
  - **IN_APP_LIVE_DRIVER**: Zero-Cost In-App Live Driver on-device fallback.
