# ZeroTax-Local (`Satish-Jadav/ITR`) — Complete Developer ITR Implementation Bible, Exhaustive Deduction & Tax Math Reference, Competitor Gap Matrix & Next-Gen Feature Specification

## 0. How to Use This Document (For Software Developers with Zero Prior Tax Knowledge)

This document is written so that **any Android or Kotlin backend/frontend developer—even with zero background in Indian Income Tax or Chartered Accountancy—can implement 100% of the Indian Income Tax Return (`ITR-1`, `ITR-2`, `ITR-4`, and `ITR-U`) rules, deductions, exemptions, tax calculations, reconciliations, and next-generation features without ever having to read the raw Income-tax Act, 1961**.

Every concept is defined in plain engineering terms (`input variables -> boundary conditions -> exact integer/floating-point math formula -> worked Rupee example -> target Kotlin file`).

**Reference Links:**

* **GitHub Repository:** [Satish-Jadav/ITR](https://github.com/Satish-Jadav/ITR)

* **Codebase & Specification Mirror:** [ITR Google Drive Folder](https://drive.google.com/drive/folders/1xv6_irMLs_qPXqXyl93nhOgLxAjtgS8T) & [ZeroTax-Local-Android-Codebase](https://www.google.com/search?q=https://drive.google.com/drive/folders/1UJ_v13O7uAaaCOM7zjPyKelGqPEx6GcJ)

## 1. Indian Income Tax 101 for Software Engineers (Domain Primer)

Before writing a single line of tax calculation code, a developer needs to understand **8 core domain concepts**:

### 1.1 Financial Year (FY) vs. Assessment Year (AY) vs. Calendar Year (CY)

* **Financial Year (`FY` — e.g., `FY 2025-26`):** The 12-month period from **April 1, 2025 to March 31, 2026** in which the user actually earns money, pays rent, buys insurance, and trades stocks.

* **Assessment Year (`AY` — e.g., `AY 2026-27`):** The 12-month period **immediately following the FY** (April 1, 2026 to March 31, 2027) in which the user files the tax return for the previous FY.

  * *Developer Rule:* `AY_START_YEAR = FY_START_YEAR + 1`. When the government says *"ITR for AY 2026-27"*, all salary, bank interest, and domestic capital gains belong to **April 1, 2025 – March 31, 2026**.

* **Calendar Year (`CY` — e.g., `Jan 1, 2025 to Dec 31, 2025`):** Used **ONLY** for **Schedule FA (Foreign Assets)** reporting (such as US RSUs/ESPPs held in E\*TRADE, Charles Schwab, or Fidelity).

  * *Critical Trap:* While RSU perquisite salary (`Sec 17(2)`) and foreign dividends (`Schedule OS`) are taxed on the **Financial Year (Apr 1 – Mar 31)**, the disclosure table of foreign shares held (`Schedule FA Table A3`) is strictly reported for the **Calendar Year (Jan 1 – Dec 31)**.

### 1.2 The 5 Statutory "Heads of Income" (`Section 14`)

Every rupee a user earns is bucketed into one of **5 buckets (Heads)**:

1. **`Income from Salaries` (`Schedule S` — Sections 15 to 17):** Base salary, dearness allowance (DA), bonus, commissions, perquisites (company car, vested RSUs/ESOPs), gratuity, leave encashment, and pension (including regular retirement pension and family pension—though family pension is taxed under Head #5).

2. **`Income from House Property` (`Schedule HP` — Sections 22 to 27):** Rental income from residential/commercial buildings owned by the user, minus municipal taxes, a flat 30% repair deduction (`Sec 24(a)`), and home loan interest (`Sec 24(b)`). If the user lives in their own house (`Self-Occupied`), rent is `₹0`, so home loan interest creates a **negative income (Loss from House Property)** up to `-₹2,00,000` in the Old Regime.

3. **`Profits and Gains of Business or Profession` (`Schedule BP` — Sections 28 to 44DB):** Income earned by freelancers, doctors, lawyers, software consultants (`Sec 44ADA`), YouTubers, creators, shopkeepers, traders (`Sec 44AD`), or commercial vehicle owners (`Sec 44AE`), as well as F&O / Intraday trading.

4. **`Capital Gains` (`Schedule CG` & `Schedule 112A` — Sections 45 to 55A):** Profit or loss from selling a capital asset—stocks, mutual funds, land, apartments/houses, gold, bonds, or foreign shares. Divided into **Short-Term (`STCG`)** and **Long-Term (`LTCG`)** based on holding period (12 months for listed stocks/equity MFs; 24 months for real estate, gold, and unlisted shares).

5. **`Income from Other Sources` (`Schedule OS` — Sections 56 to 59):** The residual bucket for Savings Bank Interest, Fixed Deposit (FD) / Recurring Deposit (RD) Interest, Dividends from shares/mutual funds, Family Pension, EPF interest on employee contributions > ₹2.5L, lottery/gaming winnings, and taxable gifts (> ₹50,000 from non-relatives).

### 1.3 Exemptions (`Sec 10`) vs. Deductions (`Chapter VI-A`) vs. Rebates (`Sec 87A`) vs. Relief (`Sec 89`)

Developers often confuse these 4 terms, but they apply at **completely different stages** of the pipeline:

* **Exemption (`Section 10` / `Section 54`):** Subtracted **inside the specific Head of Income** before computing Gross Total Income (e.g., `HRA u/s 10(13A)` is subtracted directly from Gross Salary; `Sec 54` house reinvestment is subtracted directly from Long-Term Capital Gains).

* **Deduction (`Chapter VI-A` — `Sections 80C to 80U`):** Subtracted **from Gross Total Income (Normal Income)** to arrive at Net Taxable Income. *Crucial Rule (`Sec 112A(3)` & `Sec 111A(2)`):* Chapter VI-A deductions (`80C`, `80D`, `80CCD(2)`, etc.) can **ONLY** reduce Normal Income (Salary + House Property + Business + Other Sources) and can **NEVER** reduce Special-Rate Capital Gains (`Sec 111A` STCG or `Sec 112A` LTCG)!

* **Rebate (`Section 87A`):** Subtracted **from calculated Base Tax** (before 4% Cess) if the resident individual's taxable income is $\le \text{₹12,00,000}$ (New Regime AY 2026-27) or $\le \text{₹7,00,000}$ (New Regime AY 2025-26) or $\le \text{₹5,00,000}$ (Old Regime).

* **Tax Credits (`TDS`, `TCS`, `Advance Tax`, `Self-Assessment Tax`, `Foreign Tax Credit u/s 90/91`):** Subtracted at the **very end** from `Total Tax Liability (including Cess & Interest)` because this is tax the user has *already paid* to the government during the year. If `Credits > Total Tax Liability`, the difference is the user's **Income Tax Refund (`RefundDue`)**.

### 1.4 Normal Rate Income vs. Special Rate Income (`Schedule SI`)

* **Normal Rate Income:** Taxed using the user's progressive slab brackets (`0%, 5%, 10%, 15%, 20%, 25%, 30%` in New Regime; `0%, 5%, 20%, 30%` in Old Regime). Includes Salary, House Property, Business/Profession, Savings/FD Interest, Dividends, and Debt Mutual Fund gains (`Sec 50AA`).

* **Special Rate Income:** Taxed at **fixed flat percentages** regardless of which slab the user is in:

  * **`Sec 111A` (Listed Equity / Equity MF STCG, STT paid):** Flat **20%** (for transfers on/after July 23, 2024).

  * **`Sec 112A` (Listed Equity / Equity MF LTCG, STT paid):** Flat **12.5%** on gains exceeding the **₹1,25,000** annual tax-free threshold.

  * **`Sec 112` (Real Estate / Land / Building / Gold / Unlisted Shares LTCG):** Flat **12.5% without indexation** (or **20% with CII indexation** if it is land/building bought before July 23, 2024 by a Resident Individual/HUF and yields lower tax).

  * **`Sec 115BBH` (Virtual Digital Assets / Crypto / NFTs):** Flat **30%** (zero deductions except cost of acquisition; zero loss set-off).

  * **`Sec 115BB` / `115BBJ` (Lottery, Crossword, Online Gaming Winnings):** Flat **30%**.

### 1.5 The 3 Official Government Data Files (`Prefill.json`, `Form 26AS`, `AIS / TIS`)

1. **`Form 26AS (Annual Tax Statement)`:** The **authoritative ledger of actual tax credits (TDS & TCS)** deposited against the user's PAN by employers, banks, clients, and buyers. *Golden Rule:* Never claim more TDS in the ITR than what appears in `Form 26AS`, or the CPC Bengaluru computer will automatically reject the excess and send a `Section 143(1)(a)` demand notice.

2. **`AIS (Annual Information Statement)` & `TIS (Taxpayer Information Summary)`:** The government's 360-degree surveillance ledger containing not only TDS items, but also **SFT (Statement of Financial Transactions)** reported by banks, mutual fund RTAs (CAMS/KFintech), stockbrokers (Zerodha/Groww), property registrars, and credit card issuers—even where ₹0 TDS was deducted (e.g., Savings Bank Interest `SFT-005`, Mutual Fund/Stock Sales `SFT-016/017/018`, Foreign Remittances `TCS-206C(1G)`).

3. **`Prefill.json`:** A JSON file provided by the e-Filing portal containing the user's personal profile, address, pre-validated bank accounts, prior-year carried-forward losses (`Schedule CFL`), and partial salary/interest figures.

## 2. Competitor Gap & Weakness Analysis (Why Existing Indian Tax Apps Fail Users)

To make **ZeroTax-Local** the undisputed best product on the market, developers must understand every flaw in existing commercial platforms (**ClearTax, TaxBuddy, Quicko, myITreturn, Cashflo**) and the **Official Income Tax Portal (`eportal.incometax.gov.in`)**:

| Dimension / Pain Point | ClearTax / Quicko | TaxBuddy / Assisted Apps | Official Govt Portal (`eportal`) | **ZeroTax-Local (`Satish-Jadav/ITR`) Advantage** | 
| ----- | ----- | ----- | ----- | ----- | 
| **1. Data Privacy & DPDP Act 2023 Compliance** | Uploads unredacted Form 16 PDFs, CAS, and bank statements to AWS/cloud servers; uses salary/investment data to cross-sell mutual funds, loans, and insurance. | Human agents & cloud servers view unmasked PAN, Aadhaar, salary, and bank statements; frequent spam/upsell calls. | Private, but session times out every 15 minutes and loses unsaved schedule edits. | **100% On-Device Processing (`SQLCipher 4.5.4 + StrongBox AES-256-GCM` + Android AICore).** Raw PDFs never leave the handset; 1-Tap `< 300ms` cryptographic vault shredder (`Sec 12(3)` DPDP Act). | 
| **2. Pricing & Paywall Traps** | Free only for basic returns; charges **₹1,500 – ₹7,500+** the moment `Schedule CG` (stocks/MFs), `Schedule FA` (foreign RSUs), or `ITR-4` (`44ADA`) is detected—often *after* the user has spent 30 minutes entering data! | Charges **₹1,499 – ₹6,999** per return; slow turnaround during the July 25–31 rush. | ₹0, but exposes 40+ raw statutory schedules with cryptic Category 'A' validation errors. | **₹0 Server/API Cost across ITR-1, ITR-2, ITR-4, and ITR-U** via our local Pure-JVM Tax Engine + Non-ERI Client-Side Ephemeral WebView Bridge. | 
| **3. Multi-Employer Job-Switch Trap (`Sec 192(2B)`)** | Often double-counts `AIS TDS-192` and `Form 16` if employer names differ slightly (e.g., `"TATA CONSULTANCY"` vs `"TCS LTD"`). | Manual data entry errors by seasonal data-entry operators. | Pre-fills raw `TDS-192` without warning the user about unpaid self-assessment tax from dual basic exemptions. | **Deterministic 10-Char TAN De-Duplication (`DeterministicReconciler.kt`):** Deduplicates strictly by `employerTan`, applies ₹75k/₹50k `Sec 16(ia)` once, and flags the exact `Sec 192(2B)` tax gap. | 
| **4. Employer Unreflected TDS Trap (`Form 16 > Form 26AS`)** | Blindly copies `Form 16` TDS into `Schedule TDS1`, causing an **automatic `Section 143(1)(a)` short-credit demand notice** from CPC Bengaluru 60 days later! | Rarely reconciles `Form 16 Part A` vs `Form 26AS Part A` line-by-line before filing. | Allows typing any TDS figure, then sends a demand notice during processing. | **Employer TDS Protection Shield:** Strictly caps `Schedule TDS1` at `min(Form 16 TDS, 26AS Deposited TDS)` and generates a **1-Tap `Section 205` Legal Notice to Employer HR**. | 
| **5. Foreign RSU / ESPP `Schedule FA` & `Form 67` Miscalculation** | Charges ₹5,000+ for Foreign Asset plans or requires manual CSV formatting of Initial, Peak, and Closing INR values. | Frequent errors mixing Financial Year (Apr–Mar) with Calendar Year (Jan–Dec) or using Google/RBI rates instead of **SBI TT Buying Rate (TTBR)**! | Requires manual entry of every vest lot and manual SBI TTBR lookup across 10 years. | **Automated `ScheduleFaCalendarYearEngine` + Bundled 10-Year Offline SBI TTBR Database** with automatic weekend/holiday rollback (`Rule 115` & `Rule 128(5)`), preventing the **₹10 Lakh Black Money Act penalty**. | 
| **6. Pre-July 23, 2024 Property LTCG Grandfathering (`Sec 112(1)(a)`)** | Many calculators only apply the new 12.5% unindexed rate or require manual CII lookup. | Manual spreadsheet calculation prone to CII year errors. | Requires user to manually calculate both options in `Schedule CG`. | **Deterministic Dual-Option Property Engine (`GTV-10`):** Automatically computes `min(12.5% Unindexed, 20% CII-376 Indexed)` and adds the **10% `Sec 50C` Stamp Duty Safe-Harbor check**. | 
| **7. Hidden New-Regime Deductions & Basic Exemption Absorption** | Treats New Regime as "Standard Deduction only," missing `80CCD(2)` (14%), Let-Out Property `24(b)` interest, Post Office `10(15)(i)`, and Disabled Transport `10(14)`. | Misses `Proviso to Sec 111A/112A` basic exemption absorption for retirees/investors. | Does not nudge users on unclaimed deductions or regime breakeven thresholds. | **7-Deduction New Regime Maximizer + Unexhausted Basic Exemption Absorption + Actionable Breakeven Nudge Engine.** | 
| **8. Bank Statement & Non-Salary TCS Discovery** | Requires manual entry of 80C/80D/80G receipts; ignores `206C(1G)` Foreign Travel/LRS TCS (20%) and `206C(1F)` Car TCS (1%). | Asks user to email bank PDFs; misses small deductions like ₹5,000 preventive health checkup (`80D(2A)`). | Shows TCS in a separate tab that users often forget to pull into return schedules. | **On-Device Gemini Nano Bank Narration Classifier (`P2-PROMPT-01`) + Automated `Schedule TCS` Credit Sweeper.** | 

## 3. Complete All-User-Personas Use-Case Catalog (12 Real-World Taxpayer Archetypes)

To ensure our code handles **every type of Indian taxpayer**, the engine and UI must support all **12 archetypes** below:

 1. **Archetype U-01: First-Time Salaried Filer / Fresh Graduate (`ITR-1`, Income** $\le \text{₹12.75L}$**)**

    * *Profile:* Age 23, Gross Salary ₹11,50,000, TDS deducted in Q1–Q2 ₹18,000.

    * *Use Case:* Syncs portal/Form 16 -> `Std Ded = ₹75,000` -> `Taxable = ₹10,75,000` -> `Sec 87A Rebate (AY 2026-27) = 100%` -> **Final Tax = ₹0** -> **Recovers full ₹18,000 TDS as Refund in 3 minutes**.

 2. **Archetype U-02: Mid-Year Job Switcher (`ITR-1`, Two Employers, `Sec 192(2B)` Shortfall)**

    * *Profile:* Age 28 (`Persona P1 — Aarav`), Employer 1 (`TCS`: ₹10.5L, TDS ₹65k) + Employer 2 (`Infosys`: ₹14.0L, TDS ₹1.15L) + `AIS` duplicate rows + Rented flat ₹28,000/month.

    * *Use Case:* Deduplicates by TAN (`MUMT01928A` & `BLRI99887C`), applies ₹75,000 Standard Deduction **once**, compares New vs. Old Regime (with Rule 2A HRA), calculates exact balance tax payable via **UPI Challan 280 (`Head 0021 / 300`)**, and auto-populates `Schedule IT`.

 3. **Archetype U-03: Salaried Employee Victim of Startup TDS Default (`Form 16 TDS > Form 26AS`)**

    * *Profile:* Age 30, Startup deducted ₹1,80,000 TDS from payslips/Form 16, but only deposited ₹1,20,000 in `Form 26AS Part A`.

    * *Use Case:* Caps `Schedule TDS1` at ₹1,20,000 to prevent an automated `Sec 143(1)(a)` demand notice, and generates a **1-Tap `Section 205` Legal Demand Letter to Employer Payroll** plus a reminder to file a 1-tap `Sec 139(5)` Revised Return once Q4 Form 24Q is updated.

 4. **Archetype U-04: Digital Freelancer, Doctor, Lawyer, or Architect (`ITR-4 Sugam` — `Sec 44ADA`)**

    * *Profile:* Age 32 (`Persona P2 — Priya`), ₹18L domestic client receipts (`TDS-194J` @ 10%) + ₹6L foreign Upwork/Stripe receipts (100% digital, $\le 5\%$ cash -> eligible up to **₹75,00,000** cap).

    * *Use Case:* Auto-routes to `ITR-4`, applies **50% Presumptive Profit (`₹12,00,000` taxable, `₹12,00,000` tax-free expense deduction with zero receipt bookkeeping)**, applies the **Single March 15 Advance Tax Rule (`Sec 234C(1)(b)`)**, and checks `Form 10-IEA` regime lock-in.

 5. **Archetype U-05: YouTuber, Content Creator, Tuition Teacher, or E-Commerce Seller (`ITR-4 Sugam` — `Sec 44AD`)**

    * *Profile:* Age 26, ₹45,00,000 Google AdSense / brand sponsorships / Amazon seller payouts received 100% digitally via bank NEFT/UPI (`TDS-194C` / `194-O`).

    * *Use Case:* Classifies under **Section 44AD (eligible up to ₹3 Crore digital turnover!)**, where statutory minimum presumptive profit on digital receipts is only **6% (`₹2,70,000` taxable income -> ₹0 tax after Sec 87A rebate!)**.

 6. **Archetype U-06: Active Retail Stock, Mutual Fund & Crypto Investor (`ITR-2` — `Schedule CG`, `112A`, `CFL`, `VDA`)**

    * *Profile:* Age 41 (`Persona P3 — Rohan`), Zerodha + Groww + CAMS CAS with 500+ trade lots, pre-2018 grandfathered equity (`Sec 55(2)(ac)`), current-year `LTCL` + `STCG`, Debt MF (`Sec 50AA`), Sovereign Gold Bond maturity (`Sec 47(viic)`), and Crypto trades (`194S`).

    * *Use Case:* Parses all 500+ lots in `< 1.5s`, looks up Jan 31, 2018 FMV from the bundled SQLite table, blocks `LTCL` from offsetting `STCG` (`Sec 70(3)`), carries forward unabsorbed losses for 8 years (`Schedule CFL`), exempts SGB maturity, and surfaces the **March ₹1,25,000 LTCG Tax-Harvesting Radar**.

 7. **Archetype U-07: Homeowner / Landlord with Multiple Properties or Property Sale (`ITR-2` — `Schedule HP` & `Sec 112(1)(a)` / `Sec 54`)**

    * *Profile:* Age 45, owns 1 self-occupied house (jointly with spouse, ₹3.2L loan interest) + 1 rented flat + sold an apartment bought in 2015 for ₹80L.

    * *Use Case:* Applies co-owner split (`Sec 26`), claims let-out loan interest **even in the New Regime** up to Net Annual Value, compares **12.5% unindexed vs. 20% CII-376 indexed property LTCG** (`saving ₹2,55,315`), and calculates **Section 54 / 54EC bond reinvestment exemption**.

 8. **Archetype U-08: MNC Software Engineer with Foreign RSUs / ESPPs & US Dividends (`ITR-2` — `Schedule FA`, `FSI`, `TR`, `Form 67`)**

    * *Profile:* Age 36 (`Persona P4 — Vikram`), ₹58L salary (10% Surcharge + Marginal Relief) + Alphabet/Microsoft/Amazon RSUs in Charles Schwab/E\*TRADE + US dividends with 25% DTAA tax withheld (`Form 1042-S`).

    * *Use Case:* Builds **Calendar-Year (`Jan 1 – Dec 31`) `Schedule FA Table A3`** using the bundled 10-year SBI TTBR database (rolling back Sunday/holiday vest dates to Friday TTBR), avoids double-taxing `Sec 17(2)` perquisites, and compiles **Form 67 Foreign Tax Credit** using preceding-month-end SBI TTBR (`Rule 128(5)`).

 9. **Archetype U-09: Senior & Super-Senior Citizen Pensioner (`ITR-1` / `ITR-2` — `Sec 80TTB`, `Sec 207`, `Form 15H`, `gu-IN`/`hi-IN`)**

    * *Profile:* Age 67 (`Persona P5 — Harshadbhai`), Pension ₹6.5L + Bank FD & SCSS Interest ₹2.8L + Uninsured Super-Senior Mother's medical bills ₹48,000.

    * *Use Case:* Claims **₹50,000 `Sec 80TTB`** on FD + savings interest, claims **₹48,000 uninsured senior parent medical bills u/s `80D(2)(d)`**, shifts unused basic exemption against any capital gains, enforces **₹0 `Sec 234B` interest under `Sec 207(2)`**, and generates **April 1 `Form 15H` PDFs** for banks.

10. **Archetype U-10: Returning NRI / Seafarer / Global Consultant (`ITR-2` — `Sec 6(6)` RNOR & `Sec 89A`)**

    * *Profile:* Returned to India after 6 years in the US/UAE; holds US 401(k)/brokerage accounts.

    * *Use Case:* Runs the **Interactive `Sec 6` Day-Counter**, establishes **RNOR (Resident Not Ordinarily Resident)** status—making foreign dividends and foreign capital gains **100% tax-free in India** and legally exempting them from `Schedule FA`—or claims `Sec 89A` deferral on 401(k) accruals.

11. **Archetype U-11: Retiree / Layoff / VRS Recipient (`ITR-1` / `ITR-2` — `Sec 10(10)`, `10(10AA)`, `10(10B)/(10C)`, `Sec 89(1)`)**

    * *Profile:* Received ₹18L Gratuity, ₹22L Leave Encashment, and ₹4L Salary Arrears.

    * *Use Case:* Exempts full ₹18L Gratuity (`Sec 10(10)` ₹20L cap) and full ₹22L Leave Encashment (`Sec 10(10AA)` ₹25L cap) **in both New and Old Regimes**, and computes `Sec 89(1)` `Form 10E` relief on arrears.

12. **Archetype U-12: Missed-Deadline or Notice-Resolution Filer (`ITR-U u/s 139(8A)` / `Sec 139(5)` Revised / `Sec 143(1)` Copilot)**

    * *Profile:* Missed filing for AY 2024-25 / AY 2025-26, or received an intimidating Hindi/Gujarati/English `Sec 143(1)` / `Sec 139(9)` notice from CPC Bengaluru.

    * *Use Case:* Computes **ITR-U (`25% / 50% / 60% / 70%` additional tax u/s `140B`)**, audits `Sec 244A` refund interest, or translates the CPC notice into plain Gujarati/Hindi/English with 100% token-locked rupee accuracy.

## 4. Complete 12-Stage Deterministic Tax Calculation Pipeline (Developer Step-by-Step Math Spec)

Below is the **exact 12-stage mathematical execution order** that `:core:tax-engine` must execute for every taxpayer. Developers must never reorder these stages, because statutory caps (`80G` 10% Adjusted GTI cap, `Sec 288A` ₹10 rounding, `Sec 87A` marginal relief, and `Surcharge` marginal relief) depend on the exact output of the preceding stage:

```
+---------------------------------------------------------------------------------------------------+
| STAGE 1: Reconcile & De-Duplicate Raw Inputs (TAN De-Dup u/s 192(2B), 26AS TDS Cap, High-Water OS)|
+---------------------------------------------------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------+
| STAGE 2: Compute Net Income Under Each of the 5 Heads (Applying Sec 10, 16, 24, 44ADA/AD, 54, 57) |
|   • Head 1: Net Salary = max(0, GrossSalary - Sec10Allowances - StdDed16ia - ProfTax16iii - Ent16)|
|   • Head 2: Net House Property (Self-Occupied capped at -₹2L in Old, ₹0 in New; Let-Out NAV - 24b)|
|   • Head 3: Net Presumptive Business/Profession (44ADA >= 50%, 44AD >= 6% digital / 8% cash)      |
|   • Head 4: Net Capital Gains (Split into STCG_111A @ 20%, LTCG_112A @ 12.5%, LTCG_112, Slab_50AA)|
|   • Head 5: Net Other Sources (Savings + FD + Dividends - Sec57i + NetFamilyPension57iia)         |
+---------------------------------------------------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------+
| STAGE 3: Apply Intra-Head (Sec 70), Inter-Head (Sec 71/71(3A)), & Brought-Forward (Sec 74) Losses |
|   • STCL offsets STCG first, then LTCG. LTCL offsets LTCG ONLY (NEVER STCG!).                     |
|   • Old Regime HP Loss offsets Normal Heads up to ₹2,00,000 u/s 71(3A) (₹0 inter-head in New).    |
|   • Output: Normal_GTI and Special_Rate_Gains (STCG_111A, LTCG_112A, LTCG_112, VDA_115BBH)        |
+---------------------------------------------------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------+
| STAGE 4: Subtract Chapter VI-A Deductions (Strictly from Normal_GTI ONLY — Never from Special CG!)|
|   • Step 4A: Subtract all non-80G deductions (80C+80CCC+80CCD1 <= 1.5L, 80CCD1B <= 50k, 80CCD2,   |
|              80D, 80DD, 80DDB, 80E, 80EE/EEA/EEB, 80TTA/TTB, 80U).                                |
|   • Step 4B: Compute Adjusted_GTI = max(0, Normal_GTI - Prior_Chapter_VIA).                       |
|   • Step 4C: Compute Sec 80G (capping Category 3 & 4 donations at 10% of Adjusted_GTI) & Sec 80GG.|
+---------------------------------------------------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------+
| STAGE 5: Round Net Taxable Income to Nearest ₹10 Under Section 288A                               |
|   • Normal_Taxable_288A = roundToNearestTen(max(0, Normal_GTI - Total_Chapter_VIA))               |
|   • Total_Taxable_288A  = roundToNearestTen(Normal_Taxable_288A + Total_Special_Rate_Income)      |
+---------------------------------------------------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------+
| STAGE 6: Shift Unexhausted Basic Exemption Limit Against Special Rate CG (Proviso to 111A & 112A) |
|   • If Resident Individual AND Normal_Taxable_288A < Basic_Exemption_Limit:                       |
|     Unexhausted = Basic_Exemption_Limit - Normal_Taxable_288A                                     |
|     1st Priority: Absorb STCG_111A (saves 20%!) -> 2nd Priority: Absorb LTCG_112A > ₹1.25L (12.5%)|
+---------------------------------------------------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------+
| STAGE 7: Compute Progressive Slab Tax on Normal Income + Flat Tax on Special Rate Incomes         |
|   • Normal Slab Tax (including Partial Agricultural Integration if Agri > ₹5,000)                 |
|   • Special Tax = (Effective_STCG_111A * 20%) + (max(0, Effective_LTCG_112A - 1,25,000) * 12.5%)  |
|                   + Property_LTCG_Tax_min_12.5_or_20_CII + (VDA_115BBH * 30%)                     |
+---------------------------------------------------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------+
| STAGE 8: Apply Section 87A Rebate & Section 87A Marginal Relief (Strictly on Normal Slab Tax!)    |
|   • Rebate 87A NEVER reduces Sec 112A LTCG or Sec 111A STCG tax!                                  |
|   • Apply Marginal Relief just above ₹12,00,000 (AY 2026-27) or ₹7,00,000 (AY 2025-26).           |
+---------------------------------------------------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------+
| STAGE 9: Compute Surcharge (with 15% Cap on CG/Dividends) & Surcharge Marginal Relief (> ₹50L)    |
+---------------------------------------------------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------+
| STAGE 10: Add 4% Health & Education Cess & Subtract Sec 89(1) Relief + Form 67 Foreign Tax Credit |
+---------------------------------------------------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------+
| STAGE 11: Compute Mandatory Interest & Late Fees (Secs 234A, 234B, 234C, 234F, 140B ITR-U)        |
|   • Truncate unpaid tax principal down to multiple of ₹100 under Rule 119A(b)!                    |
+---------------------------------------------------------------------------------------------------+
                                                 |
                                                 v
+---------------------------------------------------------------------------------------------------+
| STAGE 12: Subtract Verified Tax Credits (TDS1 + TDS2 + TDS3 + TCS + Advance/Self-Assessment IT)   |
|   • Round final payable/refundable to nearest ₹10 under Section 288B.                             |
+---------------------------------------------------------------------------------------------------+

```

### 4.1 Exact Progressive Slab Tables (`Stage 7`)

#### A. Default New Tax Regime (`Section 115BAC(1A)`) — Same for All Ages (`Regular`, `Senior`, `Super-Senior`)

* **For `AY 2026-27` (`FY 2025-26` — Finance Act, 2025):**

  * `₹0 to ₹4,00,000` -> **0%**

  * `₹4,00,001 to ₹8,00,000` -> **5%** (Max tax in slab = `₹20,000`)

  * `₹8,00,001 to ₹12,00,000` -> **10%** (Max tax in slab = `₹40,000` | Cumulative at ₹12L = `₹60,000`)

  * `₹12,00,001 to ₹16,00,000` -> **15%** (Max tax in slab = `₹60,000` | Cumulative at ₹16L = `₹1,20,000`)

  * `₹16,00,001 to ₹20,00,000` -> **20%** (Max tax in slab = `₹80,000` | Cumulative at ₹20L = `₹2,00,000`)

  * `₹20,00,001 to ₹24,00,000` -> **25%** (Max tax in slab = `₹1,00,000` | Cumulative at ₹24L = `₹3,00,000`)

  * `Above ₹24,00,000` -> **30%**

* **For `AY 2025-26` (`FY 2024-25` — Finance (No. 2) Act, 2024):**

  * `₹0 to ₹3,00,000` -> **0%**

  * `₹3,00,001 to ₹7,00,000` -> **5%** (Max = `₹20,000`)

  * `₹7,00,001 to ₹10,00,000` -> **10%** (Max = `₹30,000` | Cumulative = `₹50,000`)

  * `₹10,00,001 to ₹12,00,000` -> **15%** (Max = `₹30,000` | Cumulative = `₹80,000`)

  * `₹12,00,001 to ₹15,00,000` -> **20%** (Max = `₹60,000` | Cumulative = `₹1,40,000`)

  * `Above ₹15,00,000` -> **30%**

#### B. Old Tax Regime (Depends on Resident Age as of March 31 of the FY)

* **Basic Exemption Limit (`E`):**

  * `REGULAR_BELOW_60` (Born after April 1, 1966 for FY 2025-26): **`E = ₹2,50,000`**

  * `SENIOR_60_TO_79` (Resident aged 60 to 79 at any time during FY): **`E = ₹3,00,000`**

  * `SUPER_SENIOR_80_PLUS` (Resident aged 80+ at any time during FY): **`E = ₹5,00,000`**

  * *Important CBDT Birthday Edge Case (Circular No. 28/2016):* A person born on **April 1, 1966** legally attains age 60 on **March 31, 2026** (the midnight preceding their birthday) and qualifies as a Senior Citizen for `FY 2025-26` (`AY 2026-27`)!

* **Old Regime Slabs:**

  * `₹0 to E` -> **0%**

  * `E to ₹5,00,000` -> **5%** (Max = `₹12,500` for Regular, `₹10,000` for Senior, `₹0` for Super-Senior)

  * `₹5,00,001 to ₹10,00,000` -> **20%** (Max = `₹1,00,000`)

  * `Above ₹10,00,000` -> **30%**

### 4.2 Exact Section 87A Rebate & Marginal Relief Equations (`Stage 8`)

1. **Eligibility:** Available **ONLY** to a **Resident Individual (`residentialStatus != NON_RESIDENT`)**. (NRIs and HUFs get ₹0 Rebate u/s 87A).

2. **Threshold (`T_87A`) & Max Rebate (`R_max`):**

   * **New Regime `AY 2026-27`:** `T_87A = ₹12,00,000`, `R_max = ₹60,000`

   * **New Regime `AY 2025-26`:** `T_87A = ₹7,00,000`, `R_max = ₹25,000`

   * **Old Regime (All AYs):** `T_87A = ₹5,00,000`, `R_max = ₹12,500`

3. **Rebate Formula (When `normalTaxable288A <= T_87A`):**
   

   $$
   \text{Rebate}_{87A} = \min(\text{baseNormalTax}, R_{\max})
   $$

   
   *(Note: Under Section 112A(6) and Finance Act 2025, `specialRateTax` on Sec 112A LTCG and Sec 111A STCG is strictly excluded from `Rebate_87A`!)*

4. **Marginal Relief Formula (New Regime ONLY, when `normalTaxable288A > T_87A`):**

   * Why it exists: In `AY 2026-27`, at `₹12,00,000` taxable income, tax is **₹0** (₹60,000 slab tax minus ₹60,000 rebate). At `₹12,10,000` (just ₹10,000 more income!), raw slab tax jumps to `₹61,500` with `₹0` rebate. Without Marginal Relief, earning ₹10,000 extra would cost ₹61,500 in tax!

   * **Exact Equation:**
     

     $$
     \text{ExcessIncome} = \text{normalTaxable288A} - T_{87A}
     $$

     $$
     \text{MarginalRelief}_{87A} = \max(0, \text{baseNormalTax} - \text{ExcessIncome})
     $$

     $$
     \text{NetNormalTaxAfterRelief} = \text{baseNormalTax} - \text{MarginalRelief}_{87A}
     $$

   * *Worked Example (`GTV-04`, AY 2026-27 at `₹12,10,000`):*

     * `baseNormalTax` = `₹61,500`

     * `ExcessIncome` = `12,10,000 - 12,00,000 = ₹10,000`

     * `MarginalRelief_87A` = `61,500 - 10,000 = ₹51,500`

     * `NetNormalTaxAfterRelief` = `61,500 - 51,500 = ₹10,000` (+ 4% Cess `₹400` = **`₹10,400`**).

   * *Breakeven Ceiling where Marginal Relief reaches ₹0:*

     * For `AY 2026-27` (`₹12L` threshold): Marginal relief applies up to **`₹12,75,000`** (where `SlabTax = ₹71,250 <= ExcessIncome ₹75,000`).

     * For `AY 2025-26` (`₹7L` threshold): Marginal relief applies up to **`₹7,27,770`**.

### 4.3 Exact Surcharge, 15% Special-Rate Cap & Surcharge Marginal Relief Equations (`Stage 9`)

When `Total_Taxable_288A > ₹50,00,000`:

1. **Determine Normal Surcharge Rate (`S_norm`) and Previous Threshold (`T_prev`, `S_prev`):**

   * `₹50,00,001 to ₹1,00,00,000`: `S_norm = 10%`, `T_prev = ₹50,00,000`, `S_prev = 0%`

   * `₹1,00,00,001 to ₹2,00,00,000`: `S_norm = 15%`, `T_prev = ₹1,00,00,000`, `S_prev = 10%`

   * `₹2,00,00,001 to ₹5,00,00,000`: `S_norm = 25%`, `T_prev = ₹2,00,00,000`, `S_prev = 15%`

   * `Above ₹5,00,00,000`: `S_norm = 25%` (in **New Regime**) or `37%` (in **Old Regime**), `T_prev = ₹5,00,00,000`, `S_prev = 25%`

2. **Apply the 15% Surcharge Cap on Special-Rate Capital Gains (`Sec 111A`, `112A`, `112`) & Dividends:**
   

   $$
   \text{S}_{\text{special}} = \min(S_{\text{norm}}, 0.15)
   $$

   $$
   \text{RawSurcharge} = \text{round}(\text{Tax}_{\text{normal}} \times S_{\text{norm}}) + \text{round}(\text{Tax}_{\text{special}} \times S_{\text{special}})
   $$

3. **Apply Surcharge Marginal Relief (`GTV-13`):**

   * Ensures that `(TotalTax + RawSurcharge)` never exceeds `(TaxAtThreshold + SurchargeAtThreshold + (TotalIncome - T_prev))`.

## 5. Complete Developer Formula & Edge-Case Reference for Every Deduction & Allowance

This section gives developers the **exact input variables, validation regexes, statutory caps, and step-by-step math** for every deduction in the Indian Income-tax Act so they can code each one with zero ambiguity.

### 5.1 House Rent Allowance (`Section 10(13A)` read with `Rule 2A` — Old Regime Only)

* **Inputs:**

  * `basicSalaryAnnual: Long`

  * `daFormingPartOfRetirementBenefitsAnnual: Long` *(Note: Only DA that forms part of retirement/superannuation benefits counts!)*

  * `commissionAsFixedPercentageOfTurnoverAnnual: Long` *(Usually `0L`)*

  * `actualHraReceivedAnnual: Long`

  * `actualRentPaidAnnual: Long`

  * `isMetroCity: Boolean` *(`true` ONLY for **Mumbai, Delhi (New Delhi), Kolkata, Chennai**; `false` for Bengaluru, Hyderabad, Pune, Ahmedabad, Gurugram, Noida, etc.)*

  * `landlordPan: String?`

* **Step-by-Step Math:**

  1. `salaryForHra = basicSalaryAnnual + daFormingPartOfRetirementBenefitsAnnual + commissionAsFixedPercentageOfTurnoverAnnual`

  2. `limb1 = actualHraReceivedAnnual`

  3. `limb2 = max(0L, actualRentPaidAnnual - (salaryForHra * 0.10).roundToLong())`

  4. `limb3 = (salaryForHra * (if (isMetroCity) 0.50 else 0.40)).roundToLong()`

  5. `rawExemptHra = minOf(limb1, limb2, limb3)`

  6. **Statutory Landlord PAN Gate (`CBDT Circular No. 8/2013`):**

     * If `actualRentPaidAnnual > 1_00_000L` AND (`landlordPan == null` OR `!landlordPan.matches(Regex("^[A-Z]{5}[0-9]{4}[A-Z]$"))` OR `landlordPan == taxpayerPan`):

       * `finalExemptHra = 0L`

       * `blockedDueToMissingLandlordPan = true`

       * `potentialTaxableReductionIfPanAdded = rawExemptHra`

     * Else:

       * `finalExemptHra = rawExemptHra`

* **Crucial Developer Edge Case (Period-Wise / Monthly Calculation when Rent or Salary Changes Mid-Year):**

  * Under `Rule 2A`, if an employee lived in a rented flat for only 8 months (or switched from Bengaluru to Mumbai in October), the 3 limbs (`actualHra`, `rent - 10% salary`, `40%/50% salary`) must be evaluated **for the exact months during which the rented accommodation was occupied by the assessee**!

### 5.2 House Rent Paid Without HRA (`Section 80GG` — Old Regime Only)

* **Who Qualifies:** Freelancers (`ITR-4`), self-employed professionals, or salaried employees whose salary package does **not** include an HRA component (`actualHraReceivedAnnual == 0L`).

* **Disqualification Rule:** Neither the taxpayer, nor their spouse, nor their minor child, nor their HUF may own any residential accommodation in the city where they currently reside and work (or own any self-occupied house property in any other city whose value is claimed as ₹0 under `Sec 23(2)(a)`).

* **Step-by-Step Math:**

  1. `AdjustedGTI_80GG = Normal_GTI - (All Chapter VI-A Deductions EXCEPT Sec 80GG)`

  2. `limb1 = 5_000L * monthsRentPaid` *(Max **₹60,000/year**)*

  3. `limb2 = (AdjustedGTI_80GG * 0.25).roundToLong()` *(25% of Adjusted Total Income)*

  4. `limb3 = max(0L, actualRentPaidAnnual - (AdjustedGTI_80GG * 0.10).roundToLong())` *(Rent paid minus 10% of Adjusted Total Income)*

  5. `allowed80GG = minOf(limb1, limb2, limb3)`

  6. *Mandatory Portal Form:* Requires generating **Form 10BA** fields (Landlord Name, Address, City, Rental Amount, Mode of Payment).

### 5.3 Combined Section 80C + 80CCC + 80CCD(1) + 80CCD(1B) + 80CCD(2) Optimization Algorithm

* **The Developer Problem:** Users often contribute to both EPF/PPF/ELSS (`Sec 80C`) and NPS Tier-I (`Sec 80CCD(1)` & `80CCD(1B)`). Under **Section 80CCE**, the aggregate deduction across `80C + 80CCC + 80CCD(1)` cannot exceed **₹1,50,000**, while `Sec 80CCD(1B)` gives an **extra ₹50,000** for self-NPS Tier-I contributions, and `Sec 80CCD(2)` gives **14% (New Regime / Govt Old Regime) or 10% (Private Old Regime)** of `(Basic + DA)` for Employer NPS with **no ₹1.5L cap** (subject only to the ₹7,50,000 combined employer retiral ceiling u/s `17(2)(vii)`).

* **Exact Optimal Allocation Algorithm (Maximizes User Deduction Automatically):**

  ```
  // 1. Validate Sec 80C(3A) Life Insurance Premium Cap (10% of Sum Assured, or 15% if disabled u/s 80U/80DDB)
  val maxLifeInsRate = if (has80UOr80DdbDisability) 0.15 else 0.10
  val eligibleLifeIns80C = min(lifeInsurancePremiumPaid, (lifeInsuranceSumAssured * maxLifeInsRate).roundToLong())
  val raw80C = other80CInvestments + eligibleLifeIns80C // EPF + PPF + ELSS + Tuition + Principal + StampDuty + LifeIns
  
  // 2. Allocate Self NPS Tier-I to 80CCD(1B) FIRST (up to ₹50,000) so 80C room is preserved!
  val allowed80CCD1B = min(totalSelfNpsTier1Contribution, 50_000L)
  val remainingSelfNpsFor80CCD1 = max(0L, totalSelfNpsTier1Contribution - allowed80CCD1B)
  val statutoryCap80CCD1 = if (isSalaried) (basicPlusDa * 0.10).roundToLong() else (grossTotalIncome * 0.20).roundToLong()
  val eligible80CCD1 = min(remainingSelfNpsFor80CCD1, statutoryCap80CCD1)
  
  // 3. Apply Section 80CCE Combined ₹1,50,000 Cap across (80C + 80CCC + 80CCD(1))
  val allowed80CCE = min(1_50_000L, raw80C + sec80CCCContribution + eligible80CCD1)
  
  // 4. Employer NPS u/s 80CCD(2) — Allowed in BOTH New Regime (14%) and Old Regime (14% Govt / 10% Pvt)
  val employerNpsRateCap = if (regime == TaxRegime.NEW_REGIME_115BAC || isCentralOrStateGovtEmployee) 0.14 else 0.10
  val allowed80CCD2 = min(employerNpsContribution, (basicPlusDa * employerNpsRateCap).roundToLong())
  
  ```

### 5.4 Complete Section 80D Health Insurance & Medical Expenditure Matrix (All 4 Buckets)

Developers must model **two independent buckets** (`Self/Spouse/Children` vs. `Parents`) with **4 sub-components**:

* **Bucket 1 — Self, Spouse & Dependent Children:**

  * `capBucket1 = if (isSelfOrSpouseSeniorCitizen60Plus) 50_000L else 25_000L`

  * `preventiveCheckupSelf = min(preventiveCheckupSelfPaid, 5_000L)` *(Cash allowed!)*

  * `uninsuredSeniorMedicalSelf = if (isSelfOrSpouseSeniorCitizen60Plus && healthInsPremiumSelf == 0L) medicalBillsSelfPaidDigitally else 0L`

  * `allowedBucket1 = min(capBucket1, healthInsPremiumSelf + preventiveCheckupSelf + uninsuredSeniorMedicalSelf)`

* **Bucket 2 — Father & Mother (Whether Dependent or Not!):**

  * *Important Statutory Rule:* Under Section 80D, **parents do NOT need to be financially dependent** on the taxpayer for the taxpayer to claim up to ₹25,000 / ₹50,000 for paying their health insurance or senior medical bills!

  * `capBucket2 = if (isEitherParentSeniorCitizen60Plus) 50_000L else 25_000L`

  * `preventiveCheckupParents = min(preventiveCheckupParentsPaid, max(0L, 5_000L - preventiveCheckupSelf))` *(Overall Preventive Checkup cap across Bucket 1 + Bucket 2 is strictly **₹5,000** under `Sec 80D(2A)`!)*

  * `uninsuredSeniorMedicalParents = if (isEitherParentSeniorCitizen60Plus && healthInsPremiumParents == 0L) medicalBillsParentsPaidDigitally else 0L`

  * `allowedBucket2 = min(capBucket2, healthInsPremiumParents + preventiveCheckupParents + uninsuredSeniorMedicalParents)`

* **Total Allowed Section 80D:** `allowedBucket1 + allowedBucket2` *(Maximum possible = **`₹1,00,000`** when both Taxpayer and Parent are Senior Citizens* $\ge 60$*)*.

### 5.5 Disability & Critical Illness Deductions (`Sec 80DD`, `Sec 80U`, `Sec 80DDB`)

1. **`Section 80U` (Taxpayer Himself/Herself Has a Certified Disability):**

   * If `disabilityPercentage in 40..79` -> Flat **`₹75,000`**

   * If `disabilityPercentage >= 80` (Severe Disability) -> Flat **`₹1,25,000`**

   * *Rule:* Flat amount—even if the taxpayer spent ₹0 on medical treatment during the year! Requires `Form 10-IA` acknowledgment number (for autism, cerebral palsy, or multiple disabilities) or UDID number.

2. **`Section 80DD` (Taxpayer Maintains a Disabled Dependent — Spouse, Child, Parent, Brother, or Sister):**

   * If `dependentDisabilityPercentage in 40..79` -> Flat **`₹75,000`**

   * If `dependentDisabilityPercentage >= 80` -> Flat **`₹1,25,000`**

   * *Mutual Exclusion Guardrail:* If the disabled dependent already claimed `Sec 80U` in their own ITR, the taxpayer cannot claim `Sec 80DD` for that same person.

3. **`Section 80DDB` (Actual Expenditure on Specified Critical Illnesses — Cancer, Renal Failure, Parkinson's, etc.):**

   * `statutoryCap = if (patientAge >= 60) 1_00_000L else 40_000L`

   * `allowed80DDB = max(0L, min(actualTreatmentSpendRupees, statutoryCap) - insuranceAndEmployerReimbursementReceived)`

### 5.6 Loan Interest Deductions (`Sec 80E`, `Sec 80EE`, `Sec 80EEA`, `Sec 80EEB`)

1. **`Section 80E` (Higher Education Loan Interest):**

   * `allowed80E = if (repaymentYearIndex in 1..8) actualInterestPaidToApprovedBankOrFinancialInstitution else 0L`

   * *No Upper Cap:* Even if interest is ₹8,00,000/year, 100% is deductible in the Old Regime.

2. **`Section 80EE` vs. `Section 80EEA` (First-Time Homebuyer Extra Home Loan Interest):**

   * *Mutual Exclusion:* A taxpayer can claim **either** `80EE` (max `₹50,000`) **or** `80EEA` (max `₹1,50,000`), never both.

   * *How it stacks with `Section 24(b)`:* First, claim up to `₹2,00,000` under `Schedule HP -> Section 24(b)`. Any **unexhausted home loan interest above ₹2,00,000** (`excessInterest = max(0L, totalHomeLoanInterest - 2_00_000L)`) flows into `Sec 80EEA` (up to `₹1,50,000`), giving a total deduction of up to **`₹3,50,000/year`**!

3. **`Section 80EEB` (Electric Vehicle Loan Interest):**

   * `allowed80EEB = min(evLoanInterestPaid, 1_50_000L)` (for loans sanctioned between `2019-04-01` and `2023-03-31`).

### 5.7 Interest Income Deductions (`Section 80TTA` vs. `Section 80TTB` vs. `Section 10(15)(i)`)

* **Step 1 — Post Office Savings Bank Exemption (`Section 10(15)(i)` — Allowed in BOTH New & Old Regimes!):**

  * `exemptPostOfficeInterest = min(postOfficeSavingsInterest, if (isJointPostOfficeAccount) 7_000L else 3_500L)`

  * Subtract `exemptPostOfficeInterest` from `savingsBankInterest` *before* adding to `Schedule OS` (and report `exemptPostOfficeInterest` in `Schedule EI`).

* **Step 2 — `Section 80TTA` (Old Regime Only, Resident Individual `< 60` years):**

  * `allowed80TTA = min(netTaxableSavingsBankInterest, 10_000L)` *(Never includes FD or RD interest)*.

* **Step 3 — `Section 80TTB` (Old Regime Only, Resident Senior Citizen** $\ge 60$ **years):**

  * `allowed80TTB = min(netTaxableSavingsBankInterest + fixedDepositAndRdAndScssInterest, 50_000L)` *(Includes Savings + FD + RD + SCSS interest)*.

---

## 6. Module-by-Module Codebase Audit: Implemented Baseline vs. Missing `// TODO` Extensions (From Previous Plan)

A line-by-line comparison between the active source files in [Satish-Jadav/ITR](https://github.com/Satish-Jadav/ITR) (mirrored in [ZeroTax-Local-Android-Codebase](https://www.google.com/search?q=https://drive.google.com/drive/folders/1UJ_v13O7uAaaCOM7zjPyKelGqPEx6GcJ)) and the 13 master specification documents (`01_product_requirements_document.md` through `13_production_readiness_checklist_and_ci_pipeline.md`) reveals the exact current state and remaining gaps across all 8 Gradle modules:

| Gradle Module | Currently Implemented in Codebase | Missing / Stubbed Features & `// TODO` Hooks to Implement |
| :--- | :--- | :--- |
| **`:core:model`** | `ScheduleItChallanEntry.kt` | Shared domain models duplicated across modules (`TaxpayerRoutingProfile`, `EmployerSalarySource`, `Equity112ALot`, `ImmovablePropertyLtcgLot`, `ForeignEquityLotInput`, `SftAndTcs206CEntry`) consolidated into `TaxDomainModels.kt`. |
| **`:core:tax-engine`** | `DeterministicReconciler.kt`, `DeterministicIndianTaxEngine.kt`, `DeterministicItrFormRouter.kt` (14 gates), basic `CapitalGainsAndForeignAssetEngine.kt`, basic `HraAndInterestEngine.kt`, basic `ScheduleFaCalendarYearEngine.kt`, basic `RevisionAndHarvestingEngine.kt`, basic `CbdtItr1JsonCompiler.kt` | 1. **Pre-July 23, 2024 Property LTCG Indexation (`Sec 112(1)(a)`):** 12.5% unindexed vs. 20% CII-indexed comparison + Section 54/54EC/54F reinvestment exemptions.<br>2. **Capital Loss Set-Off Matrix (`Secs 70, 71, 74`):** Blocking LTCL against STCG and 8-year carry-forward computation.<br>3. **Rule 2A Landlord PAN Gate & `Sec 207(2)` Senior Citizen Immunity:** Blocking HRA when rent > ₹1L without Landlord PAN; truncating principal via Rule 119A(b) (`(unpaid / 100) * 100`) and exempting Senior Citizens from Sec 234B.<br>4. **Multi-Property `ScheduleHP` & `Sec 234C` Windfall Carve-Out:** `ScheduleHpAndPresumptiveSubEngine` and quarterly advance tax safe-harbor (12% Q1 / 36% Q2) + windfall capital gains adjustments.<br>5. **Section 89(1) Salary Arrears Relief (`Form 10E`):** `Section89ArrearsReliefCalculator`.<br>6. **Updated Return (`ITR-U u/s 139(8A) & 140B`) & March LTCG Harvesting Radar:** 25%/50%/60%/70% additional tax windows and ₹1.25L tax-free headroom calculator.<br>7. **Full CBDT JSON Compilers & Category 'A' Gate:** `CbdtCategoryADefectGate`, full `CbdtItr1JsonCompiler`, `Form67AndItr2ScheduleCompiler`, and `PriorYearItrJsonImporter`.<br>8. **Complete 24+ Vector Test Suite:** Full `MasterGoldenTaxVectorTestSuite` (`GTV-01..18`) and `AdvancedTaxEngineEdgeCaseTest` (`ECV-01..06`). |
| **`:core:database`** | 3-table Room setup (`taxpayer_profile`, `income_line_items`, `user_legal_consents_and_overrides`) + `SqlCipherHardwareVaultBuilder.kt` | 1. **10 Missing Tables in Room & SQLCipher DDL:** Expand from 3 tables to the full **13-table schema** (`house_property_schedule_hp`, `presumptive_business_44ada`, `capital_gains_112a_lots`, `historical_fmv_31jan2018_lookup`, `cost_inflation_index_cii`, `schedule_cfl_loss_carry_forward`, `chapter_via_deductions_and_80g`, `sbi_ttbr_exchange_rates`, `schedule_fa_equity_holdings`, `schedule_it_and_tds_credits`).<br>2. **Bundled Offline Seed Data:** 25-year CII table (`2001-02` to `2025-26` CII = 376), Jan 31, 2018 ISIN FMV benchmark seeds, and 10-year SBI TTBR USD-INR rates.<br>3. **DAOs & Holiday Rollback Query:** `SbiTtbrAndCiiLookupDao` (`rate_date_iso <= :targetIsoDate ORDER BY rate_date_iso DESC LIMIT 1`), `TaxVaultSummaryDao`, and `LegalAuditDao`.<br>4. **Biometric & Memory Hardening:** `BiometricPrompt.CryptoObject` vault unlock binding and zeroing `rawPassphrase` bytes immediately after opening SQLCipher. |
| **`:core:security`** | `UidaiAadhaarVerhoeffScrubber.kt`, `DpdpVaultShredder.kt`, `CanonicalWarningHasher.kt` | 1. **16-Digit UIDAI Virtual ID (VID) Redaction:** Extend `UidaiAadhaarVerhoeffScrubber` to redact 16-digit VID patterns in e-KYC XML/PDF imports.<br>2. **Hardware ECDSA Audit Log Signing:** Sign `user_legal_consents_and_overrides` rows with a hardware-backed `ECDSA` (`secp256r1`) key for tamper-evident non-repudiation.<br>3. **Pre-Shred AA Revocation Hook:** Trigger RBI Account Aggregator FIP consent revocation callback prior to local vault destruction when online. |
| **`:feature:pdf-unlocker`** | `SilentPdfUnlocker.kt` (15 permutations), `DeterministicTracesRegexParser.kt` (Form 16 Part A & B) | 1. **Bank Customer-ID / Account Permutations:** Extend `SilentPdfUnlocker` with bank-specific password patterns (customer ID, last 4 account digits).<br>2. **Domestic Capital Gains Statement Parser:** Deterministic table/regex parser for **CAMS / KFintech CAS** and **Zerodha / Groww** tax P&L statements (ISIN-level `Schedule 112A` & `111A` lots, SGB redemptions, and share buybacks).<br>3. **Foreign Broker RSU & Dividend Parser:** Deterministic parser for **E\*TRADE, Charles Schwab, and Fidelity** RSU/ESPP vest lots and **Form 1042-S** US dividend withholding statements.<br>4. **Offline CA Tax Computation & Provenance PDF Exporter:** `LocalTaxComputationPdfExporter.kt`. |
| **`:feature:ai-extractor`** | `OnDeviceForm16AiExtractor.kt` (`P1-PROMPT-01`), `TokenLockedVernacularExplainer.kt` (`en-IN`, `hi-IN`, `gu-IN`) | 1. **`OnDeviceBankNarrationClassifier.kt` (`P2-PROMPT-01`):** Batching 25 Account Aggregator bank rows ($\ge \text{₹500}$ or credit) to classify `80C_LIC_PPF_ELSS_TUITION`, `80D_HEALTH_INS`, `80D_PREVENTIVE_OR_SENIOR_MEDICAL`, `80G_DONATION`, `44ADA_FREELANCE_INFLOW`, `44AD_BUSINESS_RECEIPT`, and `SAVINGS_INTEREST`.<br>2. **Physical Rent & 80D/80G Receipt Extractor (`P1-PROMPT-02`):** Extracting Landlord PAN, Donee PAN, 80G ARN (`Form 10BE`), and cash vs. digital payment mode.<br>3. **Extended Indic Vernacular Locales:** Expand `SupportedVernacularLocale` to Marathi (`mr-IN`), Tamil (`ta-IN`), and Telugu (`te-IN`). |
| **`:feature:portal-bridge`** | `HardenedEphemeralPortalBridge.kt` (read-only fetch hook), `Challan280ReceiptParser.kt` | 1. **`UpiChallan280IntentLauncher.kt`:** Deep-link builder (`upi://pay` with Major Head `0021`, Minor Head `300`) and deposit validator (`^\\d{7}$` BSR, `^\\d{5}$` CIN, non-future ISO date).<br>2. **1-Tap ITR JSON Multipart Upload & e-Verify Bridge:** Client-side WebView action to upload compiled CBDT JSON and complete Aadhaar OTP e-Verification.<br>3. **AIS Discrepancy Feedback Submitter & CPC Tracker:** Client-side bridge to submit AIS feedback on `ais.insight.gov.in` and poll live CPC `Sec 143(1)` refund status. |
| **`:feature:aa-aggregator`** | `RbiAaEphemeralDecryptor.kt` (Curve25519 + HKDF-SHA256 + AES-256-GCM + `0x00` RAM wipe) | 1. **ReBIT Sahamati XML/JSON FIData Ledger Parser:** In-memory parser for `DEPOSIT` (savings/FD interest sweep & narration extraction) and `MUTUAL_FUNDS` / `EQUITIES` (CAMS/NSDL CAS lot extraction).<br>2. **AA Consent Artifact Manager:** Consent status polling and 1-tap FIP consent revocation manager for Privacy Tab 3. |
| **`:app`** | `MainActivity.kt` (`FLAG_SECURE`), `SmartIndividualPanField`, `Section270AStatutoryOverrideDialog`, skeleton `ZeroTaxFourTapSpineScaffold.kt` | 1. **Interactive Tap 1–4 Screens:** Multilingual DPDP Section 6 Consent Card (`en-IN`, `hi-IN`, `gu-IN`), Live Discovery checklist, `OneTapPdfDisambiguationCard` (cropped PDF bitmap viewer), Swipeable Exception Nudge Card Deck, `Presumptive44AdaAnd44AdSliderCard`, Dual-Regime Showdown Card, and UPI Challan 280 payment flow.<br>2. **Interactive 3-Tab Pro Workspace:** Tab 1 (Live CPC Processing Tracker, March LTCG Tax-Harvesting Radar, ITR-U launcher, Next-FY CTC Simulator, Sec 54 Exemption Planner), Tab 2 (Smart Tax Vault, `Level3ProvenanceBottomSheet`, Multilingual AI Notice Explainer), Tab 3 (AA Consent Manager, Biometric Lock toggle, 1-Tap DPDP Vault Shredder).<br>3. **UI Utilities & Scoped Security:** `BlackMoneyActForeignAssetModal` (₹10L penalty warning u/s 42 & 43), `IndianCurrencyFormatter` (`₹XX,XX,XXX`), and 5-minute scoped `FLAG_SECURE` override for exporting official `ITR-V` PDF receipts. |

---

## 7. Deep Line-by-Line Audit of Currently Implemented Code (8 Critical Bugs & Statutory Traps Found)

| File & Component | Current Code Implementation | Statutory Trap / Bug Found in Current Code | Deep Fix & Enhancement |
| :--- | :--- | :--- | :--- |
| **`DeterministicIndianTaxEngine.kt`** (`:core:tax-engine`) | `val rebate87A = if (input.isResidentIndividual && totalIncome288A <= rebateThreshold87A) { min(baseNormalTax + stcgTax, maxRebate87A) } else 0L` | **Critical Statutory Bug #1 (Sec 111A & Sec 87A):** Under the Finance Act, 2025 (effective AY 2026-27) and CBDT utility schema rules, **Rebate u/s 87A is strictly barred against Special Rate incomes** (both Sec 112A LTCG *and* Sec 111A STCG). Current code adds `+ stcgTax` into `rebate87A`, causing a variance failure when a user has STCG u/s 111A. | Restrict `rebate87A` strictly to `min(baseNormalTax, maxRebate87A)` and evaluate the ₹12,00,000 (AY 2026-27) / ₹7,00,000 (AY 2025-26) threshold against `normalTaxable288A`. *(Implemented in WP-01)* |
| **`DeterministicIndianTaxEngine.kt`** (`:core:tax-engine`) | No Basic Exemption absorption logic when `normalTaxable288A < exemptionLimit` and taxpayer has `111A` STCG or `112A` LTCG. | **Critical Tax-Saving Gap #2 (Proviso to Sec 112A(1) & 111A(1) — Basic Exemption Absorption):** If a resident individual has ₹1,50,000 normal income and ₹5,00,000 equity LTCG/STCG, the unexhausted basic exemption limit (`₹4,00,000 - ₹1,50,000 = ₹2,50,000` in New Regime AY 2026-27) legally **absorbs ₹2,50,000 of special-rate STCG/LTCG** before taxing the remainder! Without this, the user is overcharged up to **₹31,200 – ₹62,400**. | Implement **Unexhausted Basic Exemption Absorption (`Proviso to Sec 111A & 112A`)** inside `computeNewRegime()` and `computeOldRegime()`, shifting unused basic exemption slab against STCG (20%) first and then LTCG (12.5%) above ₹1.25L. *(Implemented in WP-01)* |
| **`DeterministicIndianTaxEngine.kt`** (`:core:tax-engine`) | `computeSurchargeWithMarginalRelief()` applies `rate` (`0.10`, `0.15`, `0.25`, `0.37`) uniformly to `baseTax`. | **Statutory Bug #3 (Proviso to Finance Act Surcharge — 15% Surcharge Cap on Sec 111A, 112A & Dividend Income):** Even if a high-income taxpayer earns > ₹2 Crore (25% or 37% surcharge bracket), surcharge on **Dividend income, Sec 111A STCG, and Sec 112A LTCG is statutorily capped at 15%**. | Split `baseTax` into `normalTax` and `specialCapitalGainsTax`, capping surcharge on `specialCapitalGainsTax` at `min(normalSurchargeRate, 0.15)`. *(Implemented in WP-01)* |
| **`DeterministicIndianTaxEngine.kt`** (`:core:tax-engine`) | Missing `compareRegimes()` in `CoreTaxEngineModuleSource.kt`. | **Missing Feature #4 (Regime Breakeven & Actionable Deduction Gap Engine):** Users want to know **"How much more 80C / 80D / HRA / NPS do I need to make Old Regime win, or how much can I save in New Regime via Sec 80CCD(2)?"** | Add `compareRegimes()` returning exact `breakEvenAdditionalDeductionForOldRegime` and itemized unused deduction headroom (`80CCD(2)`, `80D` Preventive Health Checkup ₹5,000, Senior Parents Uninsured Medical ₹50,000). *(Implemented in WP-01)* |
| **`HraAndInterestEngine.kt`** (`:core:tax-engine`) | `val int234B = if (totalAdvancePaid < (netAssessedTax * 0.90).toLong()) { (shortfall234B * 0.01 * 12).toLong() } else 0L` | **Statutory Bug #5 (Hardcoded 12 Months in Sec 234B & Missing Rule 119A(b) ₹100 Truncation):** Hardcodes `* 12` months for Sec 234B, lacks `Rule 119A(b)` truncation (`(unpaid / 100L) * 100L`), lacks the `assessedTax < 10_000L` immunity floor (`Sec 208`), and lacks `Sec 207(2)` Senior Citizen immunity. | Replace with full statutory `HraAndInterestEngine` + `computeSection234CQuarterlyInstallmentInterest()` in **WP-02**. |
| **`ScheduleFaCalendarYearEngine.kt`** (`:core:tax-engine`) | `initialValueInr = max(0L, closingInr - salesInr)` and uses `sbiTtbrClosingDateRate` for everything except peak. | **Critical Statutory Bug #6 (Black Money Act Schedule FA Table A3 Valuation):** `initialValueInr` is erroneously approximated as `closingInr - salesInr` instead of `sharesVestedOrBought * acquisitionPriceUsd * acquireDateSbiTtbr`. | Upgrade `ScheduleFaCalendarYearEngine` to lot-level `acquireDateIso` TTBR for Initial Value, `peakDateIso` TTBR for Peak Value, and `Dec 31` TTBR for Closing Value with weekend/holiday rollback in **WP-04**. |
| **`DeterministicTracesRegexParser.kt`** & **`OnDeviceForm16AiExtractor.kt`** | `tdsReported = 0L` in `OnDeviceForm16AiExtractor.kt`; regex parser discards `80C`, `80CCD(1B)`, `80CCD(2)` (Employer NPS), and `80D` from Form 16 Part B. | **Data Loss Gap #7 (Dropped Chapter VI-A & Employer NPS from Form 16 Part B):** Discards `80C`, `80CCD(1B)`, `80CCD(2)` (Employer NPS — tax-free in New Regime!), `80D`, and `Sec 10(13A)` HRA breakdown. | Expanded `EmployerSalarySource` in `TaxDomainModels.kt` *(WP-01)*; wire regex & AI extractors in **WP-07 & WP-08**. |
| **`zerotax-ci.yml`** (`.github/workflows`) | Triggers only on `branches: [ main, release/* ]` | **CI/CD Bug #8 (Branch Mismatch):** [Satish-Jadav/ITR](https://github.com/Satish-Jadav/ITR) uses `master` as its default branch, so `branches: [ main ]` prevents GitHub Actions from running on pushes to `master`. | Update `.github/workflows/zerotax-ci.yml` to trigger on `branches: [ master, main, release/* ]` in **WP-12**. |

---

## 8. Exhaustive Salary, Perquisites & Section 10 Allowances Master Table (`Schedule S`)

| Statutory Section | Allowance / Exemption Name | Old Regime Limit & Rule | New Regime (`Sec 115BAC(1A)`) Rule | How ZeroTax-Local Enhances User Benefit |
| :--- | :--- | :--- | :--- | :--- |
| **Sec 16(ia)** | **Standard Deduction on Salary / Pension** | **₹50,000** (or Gross Salary, whichever is lower) | **₹75,000** (or Gross Salary, whichever is lower) | **Multi-Employer De-Duplication (`Sec 192(2B)`):** Automatically applies ₹75k/₹50k strictly once across multiple Form 16s so job-switchers never get a `Sec 143(1)(a)` demand notice. |
| **Sec 16(ii)** | **Entertainment Allowance** | Lowest of: (a) Actual received, (b) 20% of Basic Salary, or (c) **₹5,000** (*Strictly for Central/State Govt employees*) | ₹0 (Disallowed) | Auto-unlocks up to **₹5,000** whenever `employerCategory` is `CGOV` or `SGOV` in Old Regime. |
| **Sec 16(iii)** | **Professional Tax (Tax on Employment)** | Actual paid up to **₹2,500/year** (e.g., ₹2,400/₹2,500 in Maharashtra, Karnataka, Gujarat, WB, Telangana) | ₹0 (Disallowed) | Auto-extracts from Form 16 Part B; if missing on a non-Form-16 payslip, auto-detects state PT slab (₹200/month). |
| **Sec 10(13A) & Rule 2A** | **House Rent Allowance (HRA)** | Lowest of:<br>1. Actual HRA received<br>2. Actual Rent paid minus 10% of (`Basic + DA`)<br>3. **50%** of (`Basic + DA`) for **Mumbai, Delhi, Kolkata, Chennai** or **40%** for all other cities (including Bengaluru, Hyderabad, Ahmedabad, Pune) | ₹0 (Disallowed) | **1. Landlord PAN Gate (`Circular 8/2013`):** Blocks claim if annual rent > ₹1,00,000 without valid 10-char Landlord PAN, showing exact tax saved if PAN is added.<br>**2. HRA + Home Loan Dual-Claim Shield:** Legally allows claiming **both Sec 10(13A) HRA and Sec 24(b) Home Loan Interest** when the taxpayer's owned house is in another city or rented out due to work posting. |
| **Sec 10(5) & Rule 2B** | **Leave Travel Allowance / Concession (LTA / LTC)** | Actual domestic travel fare (economy air / 1st class AC rail) for self & family (2 journeys in a 4-year block: `2022–2025` / `2026–2029`) | ₹0 (Disallowed) | Extracts LTA from Form 16 Part B; if employer didn't give exemption due to missed submission deadline, allows claiming directly in ITR with boarding pass / rail ticket scan in Local Vault. |
| **Sec 10(14)(i) & Rule 2BB** | **Official Duty Allowances (Conveyance, Helper, Research, Uniform, Daily Tour)** | Exempt up to the **actual amount incurred** for official duties | **Exempt in BOTH Old & New Regimes!** (Conveyance for official duty & tour/transfer travel allowances remain exempt u/s 115BAC) | Highlights official tour/transfer/daily allowances that remain **100% tax-free even in the Default New Regime**. |
| **Sec 10(14)(ii) — Transport for Disabled** | **Transport Allowance for Specially-Abled Employees** | **₹3,200/month (₹38,400/year)** for blind, deaf-and-dumb, or orthopaedically handicapped employees | **Exempt up to ₹3,200/month (₹38,400/year) in New Regime too!** | Automatically claims **₹38,400/year** in *both* New and Old Regimes when taxpayer has a `Sec 80U` disability flag. |
| **Sec 10(14)(ii)** | **Children Education & Hostel Expenditure Allowance** | • Education: **₹100/month/child** (max 2 children = **₹2,400/yr**)<br>• Hostel: **₹300/month/child** (max 2 children = **₹7,200/yr**) | ₹0 (Disallowed) | Auto-calculates **₹2,400 + ₹7,200 = ₹9,600** deduction in Old Regime when user indicates school/college-going children. |
| **Sec 10(10)** | **Retirement / Death Gratuity Exemption** | • **Govt Employees:** **100% Exempt**<br>• **Private Employees:** Lowest of Actual Gratuity, `15/26 × Last Drawn (Basic+DA) × Completed Years`, or **₹20,00,000** lifetime cap | **Exempt in BOTH Old & New Regimes up to ₹20,00,000** (100% for Govt) | Essential for retirees & laid-off/switching employees: verifies that Gratuity up to ₹20L is excluded from taxable salary in both regimes. |
| **Sec 10(10AA)** | **Leave Encashment on Retirement / Resignation** | • **Govt Employees:** **100% Exempt**<br>• **Non-Govt Employees:** Lowest of Actual, 10 months avg salary, cash equivalent of unavailed leave (max 30 days/yr), or **₹25,00,000** enhanced lifetime cap | **Exempt in BOTH Old & New Regimes up to ₹25,00,000!** | Prevents retirees or job-switchers from accidentally paying 30% tax on Leave Encashment payouts up to the ₹25,00,000 statutory limit. |
| **Sec 10(10B) & 10(10C)** | **Retrenchment Compensation & VRS (Voluntary Retirement)** | • Retrenchment (`10(10B)`): Exempt up to **₹5,00,000**<br>• VRS (`10(10C)`): Exempt up to **₹5,00,000** | **Retrenchment & VRS up to ₹5,00,000 Exempt in New Regime too** | Protects tech/corporate layoff and VRS recipients from overpaying tax on severance/VRS packages. |
| **Sec 10(10A)** | **Commuted Pension (Lump-Sum Pension Payout)** | • **Govt / PSU:** **100% Exempt**<br>• **Non-Govt:** **1/3rd** of total pension is exempt if Gratuity is also received; **1/2 (50%)** is exempt if no Gratuity is received | **Exempt in Old Regime; in New Regime, family pension deduction u/s 57(iia) up to ₹25,000 applies** | Automatically computes the exact 1/3rd or 1/2 tax-free commuted pension split for senior citizen retirees (`Persona P5`). |
| **Sec 10(11) & 10(12) & Rule 9D** | **EPF & PPF Interest Exemption + Rule 9D Taxable EPF Split (> ₹2.5L)** | • **PPF Interest:** 100% Exempt<br>• **EPF Employee Contribution Interest:** Exempt on contributions up to **₹2,50,000/year** (or **₹5,00,000/year** if employer makes no contribution) | **Same in BOTH Old & New Regimes** | **Critical CPC Notice Shield:** If AIS (`TDS-194A` / EPFO) shows interest on employee EPF contributions exceeding ₹2,50,000 under **Rule 9D**, automatically maps the taxable portion to `Schedule OS` and claims the 10% TDS credit while keeping the first ₹2.5L bucket 100% exempt in `Schedule EI`. |
| **Sec 10(10CC)** | **Tax Paid by Employer on Non-Monetary Perquisites** | Exempt in employee's hands | **Exempt in BOTH Old & New Regimes** | Ensures employer-borne perquisite tax is never double-taxed. |
| **Sec 89(1) & Rule 21A** | **Salary Arrears / Advance Salary Relief (`Form 10E`)** | Tax difference between current year slab rate and past year slab rate where arrears accrued | **Allowed in BOTH Old & New Regimes!** | Computes year-by-year **Table A relief u/s 89(1)** and enforces a hard **Form 10E Acknowledgment Number check** before filing so CPC does not disallow the relief u/s 143(1). |

---

## 9. Exhaustive 22-Section Chapter VI-A Deductions Master Table (`Section 80C` to `Section 80U`)

| Section | Deduction Category & Statutory Purpose | Old Regime Statutory Limit | New Regime (`115BAC(1A)`) Limit | Smart Discovery & User-Benefit Enhancement in ZeroTax-Local |
| :--- | :--- | :--- | :--- | :--- |
| **Sec 80C** | EPF, VPF, PPF, ELSS Mutual Funds (3-yr lock-in), Life Insurance Premium (capped at 10% of sum assured u/s 80C(3A) or 15% for 80U/80DDB), NSC, 5-Yr Tax-Saver FD, Sukanya Samriddhi (SSA), Senior Citizen Savings Scheme (SCSS), Home Loan Principal Repayment, Stamp Duty & Registration Charges on new house, and **Children's School/College Tuition Fees (up to 2 children)** | Combined **₹1,50,000** cap (`Sec 80CCE` across `80C + 80CCC + 80CCD(1)`) | ₹0 | **Smart 80C Gap Filler:** If Form 16 shows < ₹1,50,000 in 80C, the bank narration classifier (`P2-PROMPT-01`) automatically scans Account Aggregator / PDF statements for **School Tuition Fees, Home Loan Principal EMIs, Stamp Duty, LIC/PPF/SSY debits, and CAMS ELSS purchases** to top up the ₹1.5L limit automatically. |
| **Sec 80CCC & 80CCD(1)** | Pension Fund Contributions (LIC Jeevan Suraksha) & Employee/Self-Employed NPS Tier-I Contribution (up to 10% of Salary or 20% of GTI for self-employed) | Part of overall **₹1,50,000** `Sec 80CCE` cap | ₹0 | Automatically routes any employee NPS contribution overflowing the ₹1,50,000 `Sec 80CCE` cap into **Section 80CCD(1B)** (extra ₹50,000 cap) so ₹0 is wasted! |
| **Sec 80CCD(1B)** | **Additional Self Contribution to NPS Tier-I** (over and above the ₹1,50,000 `Sec 80C` limit) | Exclusive **₹50,000** deduction (saves up to ₹15,600 in 30% slab) | ₹0 | Auto-splits NPS Tier-I PRAN contributions between `80CCD(1)` and `80CCD(1B)` to maximize total deduction up to **₹2,00,000** (`₹1.5L + ₹50k`). |
| **Sec 80CCD(2)** | **Employer's Contribution to NPS Tier-I** (`Hidden Gem of New Regime`) | Up to **14%** of (`Basic + DA`) for Central/State Govt; **10%** for Private Sector employees | Up to **14% of (`Basic + DA`) for ALL Employees (Private & Govt) under Finance Act 2024/2025!** (Up to ₹7,50,000 combined employer retiral cap u/s 17(2)(vii)) | **1. Auto-Claim in New Regime:** Extracts `80CCD(2)` from Form 16 Part B and applies up to 14% in the New Regime.<br>**2. Next-FY CTC Nudge:** Shows every salaried user how much tax they will save next year (up to ₹43,680 per ₹10L Basic) by asking HR to enable 14% Corporate NPS. |
| **Sec 80CCH** | **Agniveer Corpus Fund Contribution** | **100%** of employee + Central Govt contribution | **100% Deductible in New Regime too (`Sec 115BAC(2)`)!** | Supports defense personnel enrolled in the Agnipath scheme in *both* regimes. |
| **Sec 80D** | **Health Insurance Premium + Preventive Health Checkup + Uninsured Senior Citizen Medical Bills** | • **Self, Spouse & Children:** **₹25,000** (or **₹50,000** if Self/Spouse is Senior Citizen $\ge 60$)<br>• **Parents (Additional):** **₹25,000** (if parents < 60) or **₹50,000** (if either parent is Senior Citizen $\ge 60$)<br>• **Max Total (`Self Senior + Parents Senior`):** **₹1,00,000** | ₹0 | **3 Smart Boosters:**<br>1. **₹5,000 Preventive Health Checkup (`Sec 80D(2A)`):** Auto-nudges users with unused 80D room to claim up to ₹5,000 for diagnostic blood tests/checkups (even if paid in cash!).<br>2. **Uninsured Senior Citizen Parents Medical Bills (`Proviso to Sec 80D(2)(d)`):** Allows claiming up to **₹50,000** of digital pharmacy, doctor, and hospital bills if senior parents have no health insurance.<br>3. **Multi-Year Lump-Sum Policy (`Sec 80D(4A)`):** Proportionately divides 2-year or 3-year health insurance premiums across eligible years. |
| **Sec 80DD** | **Maintenance & Medical Treatment of a Dependent with Disability** (Spouse, Child, Parent, or Sibling) | • Flat **₹75,000** (for 40%–79% disability)<br>• Flat **₹1,25,000** (for Severe Disability $\ge 80\%$, regardless of actual expenditure!) | ₹0 | Flat statutory deduction (no bill-by-bill math needed); includes a pre-submission gate reminding the user to link **Form 10-IA / UDID Card acknowledgment number**. |
| **Sec 80DDB** | **Medical Treatment of Specified Critical Diseases** (Cancer, Chronic Renal Failure, Neurological Diseases, Parkinson's, Thalassaemia, AIDS — Rule 11DD) for Self or Dependent | • **₹40,000** (if patient is < 60 years)<br>• **₹1,00,000** (if patient is Senior Citizen $\ge 60$ years), minus any insurance reimbursement | ₹0 | Auto-calculates net deduction (`min(Actual Spend, ₹40k/₹1L) - Insurance Payout`) when hospital/oncology/nephrology bills are detected. |
| **Sec 80E** | **Interest on Education Loan for Higher Studies** (for Self, Spouse, or Children after Class 12 — Domestic or Foreign University) | **100% of Interest Paid with NO Upper Monetary Ceiling (`Unlimited`)** for up to **8 consecutive Assessment Years** starting from the year interest repayment begins | ₹0 | **High-Value Regime Flipper:** For users paying ₹2L–₹6L/year in US/UK/Indian MBA or MS education loan interest, `Sec 80E` alone often flips the optimal choice to the Old Regime, saving ₹60,000–₹1,80,000! |
| **Sec 80EE & 80EEA** | **Additional Home Loan Interest for First-Time Homebuyers** (Over and above the ₹2,00,000 `Sec 24(b)` limit!) | • **`Sec 80EE`:** Up to **₹50,000** (loan sanctioned FY 2016-17, loan $\le ₹35L$, property $\le ₹50L$)<br>• **`Sec 80EEA`:** Up to **₹1,50,000** (affordable housing loan sanctioned 2019–2022, stamp value $\le ₹45L$) | ₹0 | When a user's home loan interest certificate exceeds ₹2,00,000 (e.g., ₹3,50,000), checks sanction date to unlock up to **₹1,50,000 extra deduction** (`₹2L u/s 24(b) + ₹1.5L u/s 80EEA = ₹3.5L total`). |
| **Sec 80EEB** | **Interest on Electric Vehicle (EV) Loan** (2-wheeler or 4-wheeler EV loan sanctioned between Apr 1, 2019 and Mar 31, 2023) | Up to **₹1,50,000** per year on EV loan interest | ₹0 | Auto-detects ongoing EV auto loan interest certificates and claims up to ₹1,50,000. |
| **Sec 80G** | **Donations to Charitable Trusts, Relief Funds & Religious Institutions** | • **100% Without Limit:** PM CARES, PM National Relief Fund, National Defence Fund<br>• **50% Without Limit:** Prime Minister's Drought Relief Fund<br>• **100% With 10% Adjusted GTI Cap:** Approved Family Planning / Olympic Association<br>• **50% With 10% Adjusted GTI Cap:** All registered NGOs/Trusts/Temples holding valid `Form 10BE` & 80G PAN | ₹0 | **1. `Sec 80G(5D)` Cash Guardrail:** Automatically disallows any single donation paid in cash > **₹2,000**.<br>**2. `Form 10BE` Donee PAN & ARN Validator:** Extracts the 10-char Donee PAN, address, and Unique Registration Number (ARN) from donation receipts so `Schedule 80G` never fails CBDT validation. |
| **Sec 80GG** | **House Rent Paid by Non-HRA Salaried Employees or Freelancers/Self-Employed** (Who do not receive HRA from an employer) | Lowest of:<br>1. **₹5,000/month (₹60,000/year)**<br>2. **25%** of Adjusted Total Income<br>3. Actual Rent paid minus **10%** of Adjusted Total Income | ₹0 | **Crucial for Freelancers (`ITR-4`) & Startups without HRA:** Automatically claims up to **₹60,000/year** for rent paid when no HRA component exists in salary, and generates the mandatory **Form 10BA** declaration parameters! |
| **Sec 80GGA** | **Donations for Scientific Research or Rural Development** (to approved universities/institutions u/s 35(1)(ii)/(iii)) | **100%** of donation (Cash donations > **₹2,000** strictly disallowed; available to non-business taxpayers) | ₹0 | Auto-validates non-cash payment mode and populates `Schedule 80GGA`. |
| **Sec 80GGC** | **Contributions to Registered Political Parties or Electoral Trusts** | **100%** of contribution (Cash donations strictly disallowed; must be via Bank/UPI/Cheque) | ₹0 | **Strict Audit & Section 270A Warning Shield:** Because `Sec 80GGC` claims are heavily scrutinized by CPC Bengaluru e-Verification campaigns, requires the bank transaction reference number (UTR), date, and Political Party PAN + Registration Number. |
| **Sec 80JJAA** | **Deduction for Employment of New Regular Employees** (for business/profession subject to audit) | **30%** of additional employee cost for 3 assessment years | **Allowed in New Regime too (`Sec 115BAC(2)`)!** | Preserved in domain model for `ITR-3` routing. |
| **Sec 80QQB & 80RRB** | **Royalty Income of Authors of Books (`80QQB`) & Patent Permittees/Inventors (`80RRB`)** | Up to **₹3,00,000** per year (for non-textbook literary/scientific/artistic book royalties or registered Indian patents) | ₹0 | Protects authors, researchers, and patent holders earning `194J` royalty income. |
| **Sec 80TTA** | **Savings Bank Account Interest Deduction** (for Resident Individuals < 60 years, including Post Office & Co-op Bank savings) | Up to **₹10,000** (Strictly excludes FD / Term Deposit / Recurring Deposit interest) | ₹0 (*Note: Post Office Savings Interest remains exempt up to **₹3,500** single / **₹7,000** joint u/s **Sec 10(15)(i)** in BOTH regimes!*) | **Post Office Sec 10(15)(i) Bonus:** Even in the **New Tax Regime** where `80TTA` is disallowed, automatically claims up to **₹3,500 (individual) / ₹7,000 (joint)** on Post Office Savings Account interest under **Section 10(15)(i)**! |
| **Sec 80TTB** | **Senior Citizen Interest Income Deduction** (for Resident Senior Citizens aged $\ge 60$ years) | Up to **₹50,000** across **Savings Interest + Fixed Deposits (FD) + Recurring Deposits (RD) + Senior Citizen Savings Scheme (SCSS) + Co-op Bank Interest** | ₹0 | Automatically switches from `80TTA` to `80TTB` the moment DOB indicates age $\ge 60$ at any time during the FY (`GTV-06`). |
| **Sec 80U** | **Flat Deduction for Taxpayer with Physical / Mental Disability** | • Flat **₹75,000** (Disability $\ge 40\%$) <br>• Flat **₹1,25,000** (Severe Disability $\ge 80\%$) | ₹0 | Pairs with `Sec 10(14)(ii)` ₹38,400 transport allowance and tracks `Form 10-IA` / UDID medical certificate details. |

---

## 10. Exhaustive House Property (`Schedule HP`) & Co-Ownership Developer Formula Reference (Fully Expanded)

### 10.1 Domain Concept for Developers
Under **Sections 22 to 27 of the Income-tax Act, 1961**, every residential or commercial building owned by the taxpayer is evaluated separately in `Schedule HP` before aggregating the net result. A property is classified into one of **3 types**:
1. **`SELF_OCCUPIED` (`Section 23(2)`):** The taxpayer (or their family/parents) lives in the house, or it is kept vacant and not let out.
   - **2-House Statutory Rule:** A taxpayer can claim **up to 2 residential houses as `SELF_OCCUPIED`**, and their Gross Annual Value (GAV) and Net Annual Value (NAV) are statutorily fixed at **`₹0`**.
   - **Self-Occupied Home Loan Interest (`Section 24(b)`):**
     - **In Old Tax Regime:** Deductible up to **`₹2,00,000`** combined across both self-occupied properties (if loan was taken on/after April 1, 1999 for purchase/construction completed within 5 years; otherwise capped at **`₹30,000`** for repairs/renovation). Because `NAV = ₹0`, this creates a **negative income (Loss from House Property)** up to **`-₹2,00,000`**.
     - **In Default New Tax Regime (`Section 115BAC(2)(i)`):** Self-occupied `Section 24(b)` interest deduction is strictly **`₹0`**.
2. **`LET_OUT` (`Section 23(1)`):** The property was rented out for all or part of the year.
3. **`DEEMED_LET_OUT` (`Section 23(4)`):** If a taxpayer owns **3 or more houses** and none of the extra houses are actually rented out, from the **3rd house onward**, the law treats them as if they were rented out at fair market/municipal rent (`Expected Rent`).

### 10.2 Step-by-Step Developer Formula for Each Property (`ScheduleHpAndPresumptiveSubEngine.kt`)

#### Step 1: Apply Co-Ownership Split (`Section 26`)
When a property is jointly owned (e.g., Husband 50% + Wife 50%) with definite and ascertainable shares:
- Multiply the property's `GrossRentReceived`, `MunicipalTaxesPaid`, and `HomeLoanInterestPaid` by `(assesseeSharePercentage / 100.0)` **before** applying statutory caps!
- **Why This Matters for Users (`Co-Ownership Multiplier`):** In a self-occupied house with a joint home loan paying `₹4,00,000` total interest/year, Husband (`50%`) claims **`₹2,00,000`** u/s `24(b)` in his ITR, and Wife (`50%`) claims **`₹2,00,000`** u/s `24(b)` in her ITR—unlocking **`₹4,00,000` combined deduction** instead of losing ₹2L to the single-person cap!

#### Step 2: Compute Pre-Construction / Pre-EMI Interest (`Explanation to Section 24(b)` — The 1/5th Rule)
- While a flat/house is under construction (before the FY in which construction is completed or possession is taken), banks charge "Pre-EMI Interest". That interest cannot be claimed while under construction.
- Instead, the **total Pre-Construction Interest accumulated from the date of borrowing until March 31 immediately preceding the date of completion/possession** is divided into **5 equal annual installments (`20% per year`)** and added to the regular current-year interest for the first 5 Assessment Years starting from the year of completion:
  $$\text{PreConstructionAnnualInstallment} = \begin{cases} \text{round}\left(\frac{\text{TotalPreConstructionInterest}}{5.0}\right) & \text{if } (\text{CurrentFY} - \text{CompletionFY}) \in \{0, 1, 2, 3, 4\} \\ 0 & \text{otherwise} \end{cases}$$
  $$\text{TotalEligibleInterest}_{24b} = \text{CurrentYearInterestPaid} + \text{PreConstructionAnnualInstallment}$$

#### Step 3: Compute Net Annual Value (`NAV`), 30% Standard Deduction (`Sec 24(a)`), and Net Property Income/Loss
- **Case A — If `propertyType == SELF_OCCUPIED`:**
  - `NAV = 0L`
  - `standardDeduction24a = 0L`
  - In **Old Regime**: `allowedInterest24b = min(TotalEligibleInterest_24b, remainingSelfOccupiedCapOutOf2Lakhs)`
  - In **New Regime**: `allowedInterest24b = 0L`
  - `netIncomeFromProperty = -allowedInterest24b`
- **Case B — If `propertyType == LET_OUT` or `DEEMED_LET_OUT`:**
  - `grossRentAfterVacancy = grossRentReceivedForAssesseeShare - unrealizedRentDeductibleRule4`
  - `municipalTaxDeductible = min(grossRentAfterVacancy, municipalTaxesActuallyPaidByOwnerDuringFy)` *(Note: Municipal/Property tax is deductible ONLY if borne by the owner and actually paid during the FY!)*
  - `NAV = max(0L, grossRentAfterVacancy - municipalTaxDeductible)`
  - `standardDeduction24a = (NAV * 0.30).roundToLong()` *(Flat **30%** statutory deduction for repairs/maintenance—no bills required, allowed in **BOTH Old and New Regimes**!)*
  - **Let-Out Loan Interest (`Section 24(b)`):**
    - In **Old Regime**: **100% of `TotalEligibleInterest_24b` is deductible** with no ₹2L cap at the individual property level (`netIncome = NAV - standardDeduction24a - TotalEligibleInterest_24b`).
    - In **New Regime (`Sec 115BAC(2)`):** `TotalEligibleInterest_24b` is deductible **up to `(NAV - standardDeduction24a)`** so net income from let-out properties is floored at `₹0` (cannot create an inter-head set-off loss against Salary in New Regime).

#### Step 4: Add Arrears of Rent / Unrealized Rent Received (`Section 25A`)
- If a tenant pays back-rent from an earlier year (`arrearsOfRentReceived > 0`):
  $$\text{TaxableArrears}_{25A} = \text{arrearsOfRentReceived} - \text{round}(\text{arrearsOfRentReceived} \times 0.30)$$

#### Step 5: Aggregate Across All Properties & Apply the `Section 71(3A)` ₹2,00,000 Inter-Head Loss Cap
- Sum `netIncomeFromProperty` across all properties + `TaxableArrears_25A` = `aggregateHpIncomeOrLoss`.
- If `aggregateHpIncomeOrLoss >= 0L`: Add full positive amount to `Normal_GTI`.
- If `aggregateHpIncomeOrLoss < 0L` (Net House Property Loss):
  - **In Old Regime:**
    - `lossAllowedForInterHeadSetOff_71_3A = min(abs(aggregateHpIncomeOrLoss), 2_00_000L)` *(Subtracts up to ₹2,00,000 from Salary / Business / Other Sources!)*
    - `unabsorbedHpLossCarriedForward_71B = max(0L, abs(aggregateHpIncomeOrLoss) - 2_00_000L)` *(Carried forward in `Schedule CFL` for **8 Assessment Years** to offset future House Property income).*
    - **Bonus Check (`Section 80EE` / `Section 80EEA`):** If the user had self-occupied interest > ₹2,00,000 and qualifies as a first-time homebuyer under `Sec 80EEA` (up to ₹1,50,000) or `Sec 80EE` (up to ₹50,000), route the excess interest into Chapter VI-A!
  - **In New Regime:**
    - `lossAllowedForInterHeadSetOff_71_3A = 0L` (No inter-head loss set-off in New Regime).

---

## 11. Exhaustive Freelancer, Creator & Small Business Presumptive Rules (`ITR-4 Sugam` — `Sec 44ADA`, `44AD`, `44AE` — Fully Expanded)

### 11.1 Why Presumptive Taxation Is a Game-Changer for Users
Normally, anyone earning freelance, consulting, creator, or business income must maintain detailed accounting ledgers (`Section 44AA`: Profit & Loss statement, Balance Sheet, expense invoices, depreciation schedules) and file the complex `ITR-3` form. To relieve small professionals and businesses, the Income-tax Act provides **3 Presumptive Schemes (`Sections 44ADA, 44AD, 44AE`)** in **`ITR-4 (Sugam)`** where the user simply declares a fixed statutory percentage of their Gross Receipts as their Net Taxable Profit—**zero expense receipts needed, zero Balance Sheet needed, and zero quarterly Advance Tax needed for Q1/Q2/Q3!**

### 11.2 Complete Comparison & Developer Formulas for `44ADA`, `44AD`, and `44AE`

| Parameter | `Section 44ADA` (Professionals) | `Section 44AD` (Small Business, Creators, Traders) | `Section 44AE` (Goods Transporters) |
| :--- | :--- | :--- | :--- |
| **Who Is Eligible?** | Resident Individual or Partnership Firm (not LLP) engaged in a specified profession u/s `44AA(1)`: **Software Development / IT Consulting (`16001`), Medical/Doctors (`14001`), Engineering (`13001`), Legal/Lawyers (`11001`), Accountancy/CA (`12001`), Architecture (`15001`), Technical Consultancy (`16002`), Interior Decoration (`17001`), Film Artists/Actors/Directors (`18001`)**. | Resident Individual, HUF, or Partnership Firm (not LLP) carrying on any eligible business: **YouTubers, Social Media Influencers, Tuition Teachers, E-Commerce Sellers (Amazon/Flipkart/Meesho), Retail Shopkeepers, Wholesale Traders, Civil/General Contractors (`194C`)**. *(Excludes commission/brokerage agents u/s `194H` and agency businesses).* | Any taxpayer owning **not more than 10 Goods Carriages (trucks/lorries/commercial delivery vehicles)** at any time during the previous year. |
| **Standard Gross Receipts / Turnover Cap** | **`₹50,00,000` (₹50 Lakhs)** | **`₹2,00,00,000` (₹2 Crore)** | **Up to `10` Goods Vehicles** |
| **Enhanced Digital Turnover Cap (When Cash Receipts $\le 5\%$ of Total Gross Receipts)** | **`₹75,00,000` (₹75 Lakhs)** (`Proviso to Sec 44ADA(1)`) | **`₹3,00,00,000` (₹3 Crore)** (`Proviso to Sec 44AD(1)`) | Up to `10` Goods Vehicles |
| **Exact Cash-Receipt 5% Check Formula** | `val cashPct = (cashReceipts.toDouble() / totalGrossReceipts.toDouble()) * 100.0`<br>`val maxCap = if (cashPct <= 5.0) 75_00_000L else 50_00_000L` | `val cashPct = (cashReceipts.toDouble() / totalTurnover.toDouble()) * 100.0`<br>`val maxCap = if (cashPct <= 5.0) 3_00_00_000L else 2_00_00_000L` | `vehicleCount <= 10` |
| **Statutory Minimum Taxable Profit Formula** | **50%** of Total Gross Receipts:<br>`minProfit = (totalGrossReceipts * 0.50).roundToLong()` | • **6%** on Digital/Bank Receipts (`UPI, NEFT, RTGS, IMPS, Credit/Debit Card, Account Payee Cheque`)<br>• **8%** on Cash/Bearer Receipts:<br>`minProfit = (digitalReceipts * 0.06).roundToLong() + (cashReceipts * 0.08).roundToLong()` | • **Heavy Goods Vehicle (`> 12 MT` gross weight):** `₹1,000 × GrossWeightTons × MonthsOwned` per vehicle<br>• **Other Goods Vehicle (`<= 12 MT`):** `₹7,500 × MonthsOwned` per vehicle |
| **What Happens If User Declares Profit Below the Minimum (`< 50%` or `< 6%/8%`)?** | Under `Sec 44ADA(4)`, if declared profit `< 50%` **AND `TotalIncome > BasicExemptionLimit`**, the taxpayer must maintain books u/s `44AA` and undergo a mandatory **CA Tax Audit u/s `44AB`** (`ITR-3`). | Under `Sec 44AD(4) & (5)`, if a user declares `< 6%/8%` (or opts out of `44AD` in any subsequent year within 5 years), they are **barred from re-entering `Sec 44AD` for the next 5 Assessment Years** and require a `Sec 44AB` audit if `TotalIncome > BasicExemptionLimit`! | Requires books u/s `44AA` and audit u/s `44AB` (`ITR-3`). |
| **Advance Tax Rule (`Section 234C(1)(b)`)** | **Single 100% installment on or before March 15!** (`₹0` interest for June 15, Sept 15, and Dec 15 installments). | **Single 100% installment on or before March 15!** (`₹0` interest for June 15, Sept 15, and Dec 15 installments). | Standard 4 quarterly installments (June 15, Sept 15, Dec 15, March 15). |
| **Mandatory `Schedule BP` Balance Sheet Particulars in `ITR-4`** | Must populate the 4 summary fields in `ITR-4`: (1) `Sundry Debtors`, (2) `Sundry Creditors`, (3) `Stock-in-Trade` (`0` for services), and (4) `Cash Balance` as of March 31. | Same 4 summary fields in `ITR-4` (`Sundry Debtors`, `Sundry Creditors`, `Stock-in-Trade`, `Cash Balance`). | Same 4 summary fields in `ITR-4`. |

---

## 12. Exhaustive Capital Gains, Reinvestment Exemptions (`Sec 54` Series), Buyback Loss & VDA Developer Formula Reference (`Schedule CG`, `112A`, `VDA` — Fully Expanded)

### 12.1 Master Asset Holding-Period & Tax-Rate Matrix (Post-Finance (No. 2) Act, 2024 & Finance Act, 2025)

| Asset Class | Short-Term (`STCG`) Holding Period & Tax Rate | Long-Term (`LTCG`) Holding Period & Tax Rate | Special Statutory Rules |
| :--- | :--- | :--- | :--- |
| **1. Listed Equity Shares, Equity-Oriented Mutual Funds ($\ge 65\%$ domestic equity), & Business Trusts (REIT/InvIT) — STT Paid** | Held **$\le 12$ months**:<br>**20%** flat u/s **`Section 111A`** | Held **$> 12$ months**:<br>**12.5%** flat u/s **`Section 112A`** on aggregate LTCG exceeding **`₹1,25,000`** | • **Jan 31, 2018 Grandfathering (`Sec 55(2)(ac)`)** applies to units/shares bought on/before `2018-01-31`.<br>• **No `Sec 87A` Rebate** allowed against `112A` or `111A` tax.<br>• **Unexhausted Basic Exemption Absorption** applies for Resident Individuals. |
| **2. Immovable Property (Land, Residential House, Apartment, Commercial Building)** | Held **$\le 24$ months**:<br>Taxed at **Normal Slab Rates** | Held **$> 24$ months**:<br>• **Non-Residents / Companies:** **12.5% without indexation**<br>• **Resident Individuals & HUFs (for property acquired BEFORE July 23, 2024):** Lower of **12.5% without indexation** OR **20% with CII Indexation (`CII = 376` for FY 2025-26)** (`Second Proviso to Sec 112(1)(a)`)! | • **`Section 50C` 10% Stamp Duty Safe-Harbor:** If `StampDutyValue <= ActualSalePrice * 1.10`, use `ActualSalePrice`!<br>• Eligible for **`Section 54`**, **`Section 54EC`**, and **`Section 54F`** exemptions. |
| **3. Specified Debt Mutual Funds ($\le 35\%$ equity), Market-Linked Debentures (MLDs), & Unlisted Bonds/Debentures** | **Regardless of holding period** (for Debt MFs acquired on/after `2023-04-01` under **`Section 50AA`**):<br>Deemed Short-Term Capital Gain taxed at **Normal Slab Rates** | *(Only Debt MFs acquired BEFORE `2023-04-01` and held $> 24$ months qualify as LTCG at **12.5% without indexation**)* | Can be offset by both `STCL` (and `LTCL` if held $> 24$ months). |
| **4. Physical Gold, Gold ETFs / Gold Mutual Funds (acquired before Apr 1, 2023 or held $> 12$/$24$ mos), & Unlisted Company Shares (Indian or Foreign RSUs)** | • Listed Gold ETFs: Held $\le 12$ mos -> Slab Rate<br>• Physical Gold & Unlisted Shares / Foreign RSUs: Held **$\le 24$ months** -> **Normal Slab Rates** | Held **$> 24$ months** (or $> 12$ mos for listed ETFs):<br>**12.5% without indexation** u/s **`Section 112`** | Eligible for **`Section 54F`** exemption when net consideration is reinvested in a residential house in India. |
| **5. Sovereign Gold Bonds (`SGB`) Issued by RBI** | If sold on stock exchange $\le 12$ mos: Slab Rate | • **If redeemed on maturity (after 8 years) or RBI premature redemption window (after 5th year) by an Individual:** **100% TAX-FREE under `Section 47(viic)`!**<br>• If sold on secondary stock exchange $> 12$ mos: **12.5%** u/s `112`. | Tag any CAMS/NSDL redemption from `RBI SOVEREIGN GOLD BOND` as `EXEMPT_SEC_47_VIIC`. |
| **6. Virtual Digital Assets (`VDA` — Cryptocurrency, Bitcoin, Ethereum, USDT, NFTs)** | **Regardless of holding period (`Section 115BBH`):**<br>Flat **30%** (+ 4% Cess = **31.2%**) | **Flat 30%** u/s **`Section 115BBH`** | • **Zero Deductions** except `Cost of Acquisition` (mining cost/transfer gas fees disallowed).<br>• **Zero Loss Set-Off (`Sec 115BBH(2)(b)`):** Loss on Coin A CANNOT offset Profit on Coin B! |

### 12.2 Exact Developer Formulas for Capital Gains Sub-Engines (`CapitalGainsAndForeignAssetEngine.kt`)

#### Formula A: Section 112A Jan 31, 2018 Equity Grandfathering (`Section 55(2)(ac)` — `GTV-09`)
When `acquisitionDateEpochDay <= 17562L` (`2018-01-31`):
$$\text{DeemedCostOfAcquisition} = \max\Big(\text{ActualCostTotal}, \min(\text{FMV}_{\text{31Jan2018Total}}, \text{FullValueOfConsideration})\Big)$$
$$\text{NetGainOrLoss}_{112A} = \text{FullValueOfConsideration} - \text{DeemedCostOfAcquisition} - \text{TransferExpenses}$$
*(Note: Brokerage, exchange transaction charges, and SEBI turnover fees are allowable `TransferExpenses`, whereas **Securities Transaction Tax (STT) is strictly disallowed** under the fifth proviso to Section 48!)*

#### Formula B: Pre-July 23, 2024 Immovable Property Dual-Tax Comparison (`Second Proviso to Section 112(1)(a)` — `GTV-10`)
1. **Check `Section 50C` 10% Safe-Harbor Tolerance first:**
   $$\text{EffectiveSaleConsideration} = \begin{cases} \text{ActualSalePrice} & \text{if } \text{StampDutyValue}_{50C} \le \text{ActualSalePrice} \times 1.10 \\ \text{StampDutyValue}_{50C} & \text{otherwise} \end{cases}$$
2. **Compute Option 1 (New 12.5% Unindexed Rule):**
   $$\text{UnindexedGain} = \max(0, \text{EffectiveSaleConsideration} - \text{TransferExpenses} - \text{PurchaseCost} - \text{ImprovementCost} - \text{Exemptions}_{54/54EC})$$
   $$\text{Tax}_{\text{12.5\%}} = \text{round}(\text{UnindexedGain} \times 0.125)$$
3. **Compute Option 2 (Grandfathered 20% CII-Indexed Rule — `CII = 376` for FY 2025-26):**
   $$\text{IndexedPurchaseCost} = \text{round}\left(\text{PurchaseCost} \times \frac{\text{CII}_{\text{TransferFY}}}{\text{CII}_{\text{AcquisitionFY}}}\right)$$
   $$\text{IndexedImprovementCost} = \text{round}\left(\text{ImprovementCost} \times \frac{\text{CII}_{\text{TransferFY}}}{\text{CII}_{\text{ImprovementFY}}}\right)$$
   $$\text{IndexedGain} = \max(0, \text{EffectiveSaleConsideration} - \text{TransferExpenses} - \text{IndexedPurchaseCost} - \text{IndexedImprovementCost} - \text{Exemptions}_{54/54EC})$$
   $$\text{Tax}_{\text{20\%Indexed}} = \text{round}(\text{IndexedGain} \times 0.20)$$
4. **Select Optimal Tax for Resident Individual/HUF:**
   $$\text{FinalPropertyLtcgTax} = \begin{cases} \min(\text{Tax}_{\text{12.5\%}}, \text{Tax}_{\text{20\%Indexed}}) & \text{if } \text{isResidentIndividual} \land \text{acquiredBeforeJuly23\_2024} \\ \text{Tax}_{\text{12.5\%}} & \text{otherwise} \end{cases}$$

#### Formula C: Section 54, Section 54F & Section 54EC Exemption Equations
1. **`Section 54` (Residential House LTCG -> New Residential House in India):**
   - `eligibleInvestment = min(amountReinvestedInNewHouse + amountDepositedInCgas1988BeforeDueDate, 10_00_00_000L)` *(Capped at **₹10 Crore**)*
   - `exemptLtcg54 = min(grossPropertyLtcg, eligibleInvestment)`
2. **`Section 54F` (Any Non-Residential LTCG like Stocks/MFs/Gold/Plot -> New Residential House in India):**
   - `netConsideration = fullValueConsideration - transferExpenses`
   - `cappedReinvestment = min(amountReinvestedInNewHouse + amountDepositedInCgas1988, 10_00_00_000L)`
   - `exemptLtcg54F = if (cappedReinvestment >= netConsideration) grossLtcg else (grossLtcg.toDouble() * (cappedReinvestment.toDouble() / netConsideration.toDouble())).roundToLong()`
3. **`Section 54EC` (Land/Building LTCG -> NHAI / REC / PFC / IRFC 5-Year Capital Gains Bonds within 6 Months):**
   - `exemptLtcg54EC = min(remainingLandOrBuildingLtcg, min(amountInvestedIn54EcBondsWithin6Months, 50_00_000L))` *(Capped at **₹50,00,000**)*

#### Formula D: Share Buyback Dual-Entry Rule (`Section 2(22)(f)` & `Section 46A` — Effective Oct 1, 2024)
When a domestic company buys back shares from a shareholder on or after October 1, 2024:
1. **`Schedule OS` (Deemed Dividend u/s `2(22)(f)`):** Add `100% of Total Buyback Proceeds Received` to `grossDividendIncome` (taxed at slab rates, with 10% TDS credit u/s `194` in `Schedule TDS2`).
2. **`Schedule CG` (Deemed Capital Loss u/s `46A`):** Set `FullValueOfConsideration = ₹0` and `CostOfAcquisition = OriginalPurchaseCostOfBoughtBackShares`, generating a **100% Capital Loss (`-OriginalPurchaseCost`)** (`STCL` if held $\le 12$ months, `LTCL` if held $> 12$ months) that offsets other capital gains or carries forward for 8 years!

#### Formula E: Strict Capital Loss Set-Off Matrix (`Sections 70, 71, 74` — `GTV-11`)
- **Rule 1 (`Sec 70(2)`):** Current-year `STCL` and Brought-Forward `STCL` offset **1st:** `STCG at Slab Rate (30%)`, **2nd:** `STCG u/s 111A (20%)`, **3rd:** `LTCG u/s 112 (12.5%/20%)`, and **4th:** `LTCG u/s 112A (12.5%)`.
- **Rule 2 (`Sec 70(3)`):** Current-year `LTCL` and Brought-Forward `LTCL` offset **ONLY `LTCG` (`Sec 112` and `Sec 112A`)** and are **STRICTLY BARRED from offsetting any `STCG`**!
- **Rule 3 (`Sec 71(3)` & `Sec 74`):** Any unabsorbed `STCL` or `LTCL` is carried forward in `Schedule CFL` for **8 succeeding Assessment Years** (provided the return is filed on or before the `Sec 139(1)` due date under `Section 80`).

---

## 13. Other Sources (`Schedule OS`), Clubbing (`Sec 64`), Full 4-Schedule TDS/TCS Recovery, Foreign Assets (`Schedule FA` / `Form 67`) & Interest (`234A/B/C/F`) Developer Formula Reference (Fully Expanded)

### 13.1 `Schedule OS` & `Rule 9D` Taxable EPF Interest Split (> ₹2.5L / ₹5L)
- Under the **First & Second Proviso to Section 10(12) read with Rule 9D**, EPFO maintains two separate accounts inside a member's PF passbook:
  1. **Non-Taxable Account:** All accumulations up to `2021-03-31` + current employee contributions up to **₹2,50,000/year** (or **₹5,00,000/year** if there is zero employer contribution, as in GPF) + all employer contributions. Interest on this bucket is **100% Exempt in `Schedule EI`**.
  2. **Taxable Account (`Rule 9D`):** Employee contributions exceeding **₹2,50,000/year** (`₹5,00,000` if no employer contribution). Interest credited on this excess bucket is reported in `AIS` under **`TDS-194A`** (with 10% TDS if interest > ₹5,000) and must be added to `Schedule OS` while claiming the `194A` TDS credit in `Schedule TDS2`.

### 13.2 Minor Child & Spouse Clubbing (`Sections 64(1A)`, `64(1)(iv)` & `Section 10(32)`)
- **Minor Child Passive Income (`Sec 64(1A)`):**
  - Passive income (FD/savings interest, rental income, capital gains) of a minor child (`age < 18`) is clubbed with the parent whose total income (excluding the minor's income) is greater.
  - **Exception 1:** Income earned by a minor from manual work or an activity involving their own skill, talent, or specialized knowledge (e.g., child actor, sports winner, coder) is **NOT clubbed**; the minor files their own return.
  - **Exception 2:** Income of a minor child suffering from a severe disability specified under `Section 80U` is **NOT clubbed**.
  - **`Section 10(32)` Exemption (Old Regime):** For each minor child whose income is clubbed, subtract **`min(minorIncomeClubbed, 1_500L)`** (`₹1,500 per child per year`).
  - **TDS Credit Transfer (`Rule 37BA(2)`):** Pull any TDS deducted on the minor's PAN in `Form 26AS` into the parent's `Schedule TDS2` by tagging `tdsDeductedInHandsOfMinorPan = minorPan`.

### 13.3 Complete 4-Schedule TDS & TCS Credit Sweeper (`Schedule TDS1`, `TDS2`, `TDS3`, `TCS`)
1. **`Schedule TDS1` (`Sec 192` Salary TDS):** Keyed by 10-char Employer TAN. `allowedTds1 = min(form16ReportedTds, form26AsDepositedTds)`.
2. **`Schedule TDS2` (Non-Salary TAN Deductors — `194A`, `194`, `194K`, `194J`, `194C`, `194S`):** Verifies that the corresponding gross receipt (`grossAmountSubjectToTds`) is included in `Schedule OS`, `Schedule BP`, or `Schedule CG`.
3. **`Schedule TDS3` (PAN-Based Buyer/Tenant Deductors — `Form 26QB` u/s `194-IA` 1% on Property Sale, `Form 26QC` u/s `194-IB` 2%/5% on Rent > ₹50k/month):** Keyed by **10-character Buyer/Tenant PAN** from `Form 26AS Part A2`.
4. **`Schedule TCS` (Tax Collected at Source — `Form 26AS Part B`):**
   - **`Sec 206C(1G)`:** 5% / 20% TCS collected by banks/Authorized Dealers on Foreign Remittances under LRS (above ₹7,00,000/yr) and Overseas Tour Program Packages.
   - **`Sec 206C(1F)`:** 1% TCS collected by automobile dealers on purchase of a car / motor vehicle costing > ₹10,00,000.
   - *Developer Rule:* `TotalTaxesPaid = sum(TDS1) + sum(TDS2) + sum(TDS3) + sum(TCS) + sum(ScheduleIT_Challan280)`. Every rupee of `TCS` directly reduces tax payable or increases the user's cash refund!

### 13.4 Foreign Assets (`Schedule FA Table A3`), `Form 67` (`Rule 128(5)`), and `Section 234A/B/C/F` Statutory Interest Math

#### A. `ScheduleFaCalendarYearEngine.kt` (`GTV-15`)
For every foreign equity/RSU/ESPP lot held at any time between **January 1 and December 31** of the preceding Calendar Year:
1. `acquireTtbr = sbiTtbrRepo.getUsdTtbrOnOrBefore(lot.acquireDateIso)` *(Rolls back Sunday/Saturday/Holiday to Friday SBI TT Buying Rate)*
2. `peakTtbr = sbiTtbrRepo.getUsdTtbrOnOrBefore(lot.peakSharePriceDateIso)`
3. `dec31Ttbr = sbiTtbrRepo.getUsdTtbrOnOrBefore("$calendarYear-12-31")`
4. `initialValueInr = (lot.sharesVestedOrBought * lot.acquisitionPricePerShareUsd * acquireTtbr).roundToLong()`
5. `peakValueInr = (lot.sharesVestedOrBought * lot.peakSharePriceDuringHoldingPeriodUsd * peakTtbr).roundToLong()`
6. `remainingSharesDec31 = max(0.0, lot.sharesVestedOrBought - lot.sharesSoldDuringCalendarYear)`
7. `closingValueInr = (remainingSharesDec31 * lot.dec31ClosingSharePriceUsd * dec31Ttbr).roundToLong()`
8. `grossDividendsInr = (lot.grossDividendsReceivedUsd * dec31Ttbr).roundToLong()`
9. `grossSaleProceedsInr = (lot.grossSaleProceedsUsd * peakTtbr).roundToLong()`

#### B. `Form 67` Foreign Tax Credit (`Rule 128(5)` — `GTV-16`)
- Exchange rate is the SBI TTBR on the **last day of the month immediately preceding the month in which foreign tax was deducted or paid** (e.g., for US dividend tax withheld on `2025-11-15`, use the SBI TTBR of `2025-10-31`):
  $$\text{GrossForeignIncome}_{\text{INR}} = \text{round}(\text{ForeignIncome}_{\text{USD}} \times \text{TTBR}_{\text{PrevMonthEnd}})$$
  $$\text{ForeignTaxPaid}_{\text{INR}} = \text{round}(\text{ForeignTaxWithheld}_{\text{USD}} \times \text{TTBR}_{\text{PrevMonthEnd}})$$
  $$\text{IndianEffectiveTaxRate} = \frac{\text{TotalTaxLiabilityAfterCess}}{\text{TotalTaxableIncome}_{288A}}$$
  $$\text{IndianTaxOnForeignIncome} = \text{round}(\text{GrossForeignIncome}_{\text{INR}} \times \text{IndianEffectiveTaxRate})$$
  $$\text{AllowableForm67FTC} = \min(\text{ForeignTaxPaid}_{\text{INR}}, \text{IndianTaxOnForeignIncome})$$

#### C. Mandatory Interest (`Sections 234A, 234B, 234C`), Late Fee (`Section 234F`), and `Rule 119A(b)` ₹100 Truncation (`GTV-14`)
1. **`AssessedTax` (`Section 234B(1) Explanation 1`):**
   $$\text{AssessedTax} = \max(0, \text{TotalTaxAfterCessAndRelief} - \text{TotalTDS} - \text{TotalTCS} - \text{AllowableForm67FTC})$$
2. **`Section 208` ₹10,000 Immunity Floor:** If `AssessedTax < 10_000L`, then `Interest234A = 0L`, `Interest234B = 0L`, and `Interest234C = 0L`!
3. **`Rule 119A(b)` ₹100 Principal Truncation:** Any unpaid tax principal on which interest is calculated must first drop fractions of ₹100:
   $$\text{RoundedPrincipal}_{119A} = \left\lfloor \frac{\text{UnpaidTax}}{100} \right\rfloor \times 100$$
4. **`Section 234A` (Late Filing Interest — 1% per month or part of a month after July 31 due date):**
   $$\text{Interest}_{234A} = \text{RoundedPrincipal}_{119A} \times 0.01 \times \text{MonthsDelayedAfterDueDate}$$
5. **`Section 234B` (Default in Advance Tax — 1% per month from April 1 if Advance Tax paid by March 31 $< 90\%$ of `AssessedTax`):**
   - **Senior Citizen Immunity (`Section 207(2)`):** If `citizenCategory != REGULAR_BELOW_60` AND `hasBusinessOrProfessionIncome == false`, **`Interest234B = ₹0`**!
   - Otherwise, if `advanceTaxPaidByMarch31 < (AssessedTax * 0.90).roundToLong()`:
     $$\text{Interest}_{234B} = \text{RoundedPrincipal}_{119A} \times 0.01 \times \text{MonthsFromApril1ToFilingMonth}$$
6. **`Section 234C` (Deferment of Quarterly Advance Tax Installments — with 12% Q1 / 36% Q2 Safe Harbors & Windfall Capital Gains Carve-Out):**
   - **If `isPresumptive44ADAOr44AD == true`:** Zero interest for Q1, Q2, Q3; only 1% for 1 month on shortfall below 100% as on March 15.
   - **Otherwise:**
     - **Q1 (June 15):** Target = `15%` of `(AssessedTax - WindfallCgTaxAfterJune15)`. If paid $< \mathbf{12\%}$ safe harbor, interest = `3%` (`1% × 3 mos`) on shortfall below 15% (truncated to ₹100).
     - **Q2 (Sept 15):** Target = `45%` of `(AssessedTax - WindfallCgTaxAfterSept15)`. If cumulative paid $< \mathbf{36\%}$ safe harbor, interest = `3%` (`1% × 3 mos`) on shortfall below 45%.
     - **Q3 (Dec 15):** Target = `75%` of `(AssessedTax - WindfallCgTaxAfterDec15)`. If cumulative paid $< 75\%$, interest = `3%` (`1% × 3 mos`) on shortfall below 75%.
     - **Q4 (March 15):** Target = `100%` of `AssessedTax`. If cumulative paid $< 100\%$, interest = `1%` (`1% × 1 mo`) on shortfall below 100%.
7. **`Section 234F` (Late Filing Fee for Belated Return u/s `139(4)` after July 31):**
   - If `TotalTaxableIncome <= BasicExemptionLimit`: **`₹0`** (`Proviso to Sec 234F`)
   - Else if `TotalTaxableIncome <= 5_00_000L`: **`₹1,000`**
   - Else (`TotalTaxableIncome > 5_00_000L`): **`₹5,000`**

---

## 14. Part A — Method-by-Method Deep Upgrades to Current-Version Source Files

A fresh re-inspection of the 9 active source files in [Satish-Jadav/ITR](https://github.com/Satish-Jadav/ITR) ([ZeroTax-Local-Android-Codebase](https://www.google.com/search?q=https://drive.google.com/drive/folders/1UJ_v13O7uAaaCOM7zjPyKelGqPEx6GcJ)) identified the following deep upgrades to existing methods:

### 14.1 Enhancing `DeterministicItrFormRouter.kt` (`:core:tax-engine`)
1. **Gate 3B — Section 115BAC(6) & `Form 10-IEA` Business Regime Lock-In Guardrail (`Once-in-a-Lifetime Switch Warning`):**
   - Whenever `selectedForm == ITR_4_SUGAM || selectedForm == ITR_3` and `recommendedRegime == OLD_REGIME`, emit `requiresForm10IeaBeforeDueDate = true` and warn the user that switching back to the New Regime in any future year **permanently locks them out of the Old Regime for life** (unless business income ceases).
2. **Gate 12B — Section 112A LTCG in `ITR-1` / `ITR-4` "Zero Capital Loss" Rule (`Rule 12`):**
   - Even if `equity112ALtcgRupees <= 1_25_000L`, if `hasAnyCurrentYearCapitalLoss == true` (or brought-forward loss exists), disqualify `ITR-1`/`ITR-4` and route to `ITR-2` so the user does not violate Rule 12 or forfeit their 8-year loss carry-forward.
3. **Gate 2B — Section 44ADA(4) & Section 44AD(5) "Sub-Threshold Profit Exemption When Total Income $\le$ Basic Exemption Limit":**
   - Under `Sec 44ADA(4)` and `Sec 44AD(5)`, declaring profit below 50% (or 6%/8%) triggers a mandatory CA audit under `Sec 44AB` **only if `totalIncomeRupees > basicExemptionLimit`**. Update Gate 2 to check `profile.totalIncomeRupees > basicExemptionLimit`.
4. **Gate 10B — Partial Integration of Agricultural Income (`> ₹5,000` Slab Push-Up Engine):**
   - When `agriculturalIncomeRupees > 5_000L` and `normalTaxable288A > basicExemptionLimit`:
     - `taxA = calculateProgressiveSlabs(normalTaxable288A + agriculturalIncomeRupees)`
     - `taxB = calculateProgressiveSlabs(basicExemptionLimit + agriculturalIncomeRupees)`
     - `baseNormalTax = taxA - taxB`
5. **Gate 9B — Startup Eligible ESOP Tax Deferral (`Section 192(1C)` / `Section 156(2)`):**
   - Route DPIIT startup ESOP holders to `ITR-2` `Schedule TTI - Deferred Tax on ESOPs`.

### 14.2 Enhancing `SilentPdfUnlocker.kt` & `DeterministicTracesRegexParser.kt` (`:feature:pdf-unlocker`)
1. **Expanded 28-Permutation Smart Password Matrix + Per-Issuer Memory Cache:**
   - Add NSDL/CDSL, SBI (`last4Account + ddmmYy`), Axis/Kotak (`first4Name + yyyy`), and Aadhaar e-KYC (`FIRST4UPPER + YYYY`) password formats, and cache winning password patterns per issuer in encrypted RAM.
2. **TRACES Form 16 Part A & Part B Digital Signature (`PDSignature`) & 7-Char Certificate Verifier:**
   - Verify the 7-character alphanumeric TRACES Certificate Number and X.509 signature dictionary to flag manual/unverified employer Form 16s before they cause a `Sec 143(1)(a)` TDS rejection.
3. **Coordinate-Sorted Multi-Column PDF Text Stripper (`PDFTextStripper.setSortByPosition(true)`):**
   - Prevents multi-column table interleaving and captures exact `Rect` bounding boxes in a single pass.

### 14.3 Enhancing `HardenedEphemeralPortalBridge.kt` (`:feature:portal-bridge`)
1. **Dual `window.fetch` + `XMLHttpRequest.prototype.open/send` Interceptor** (captures Angular `HttpClient` XHR calls alongside `fetch`).
2. **Refund Bank Account "Validated & EVC-Enabled" Pre-Check** in `Prefill.json` (`isValidated == true && isEvcEnabled == true`).
3. **Outstanding Prior-Year Tax Demand (`Section 245`) Check** to prevent surprise refund freezes.
4. **Unified `:core:security` Verhoeff + 16-Digit VID Scrubber** across all intercepted JSON strings.

### 14.4 Enhancing `SqlCipherHardwareVaultBuilder.kt` (`:core:database`)
1. **Fix `SupportFactory(passphrase, null, false)` Re-Open Crash:** Prevent premature passphrase array zeroing when Room re-opens pooled connections.
2. **Multi-PAN Family Vault Profiles (`Self | Spouse | Senior Parents`):** Switch active `pan` in 1 tap.
3. **Offline Encrypted `.ztvault` Backup & Device-to-Device Migration (`PBKDF2WithHmacSHA512 + AES-256-GCM`).**

---

## 15. Part B — 15 Next-Generation Features That Maximize User Value (`F-01` to `F-15`)

| # | Feature Name | Target Persona & Problem Solved | Exact Deterministic / On-Device Logic | Target Module |
| :---: | :--- | :--- | :--- | :--- |
| **F-01** | **Interactive NRI / RNOR / ROR Residential Status Day-Counter (`Section 6(1)`, `6(1A)`, `6(6)`)** | Seafarers, onsite engineers (US/UK/UAE/Singapore), and returning NRIs confused by the 182-day, 120-day (> ₹15L Indian income), and 60-day rules. | Evaluates: (1) Days in India in current FY, (2) Days in India in preceding 4 FYs (`365+` rule), (3) Days in preceding 7 FYs (`<= 729` RNOR rule), (4) Indian citizen leaving for employment abroad / crew member (`182-day` exception), and (5) `Sec 6(1A)` Deemed Resident (`> ₹15L` Indian income and not taxed abroad). **Huge Benefit:** Proving `RNOR` status makes **foreign dividends and foreign capital gains 100% tax-free in India** and **exempts the user from `Schedule FA` Black Money Act reporting** for 2–3 years after returning to India! | `:core:tax-engine` (`ResidentialStatusCalculator.kt`) |
| **F-02** | **Family & Spouse Tax Arbitrage Simulator (`Legitimate Family Tax Optimization`)** | Married couples & adult children supporting Senior Citizen parents. | Simulates legal family tax-saving moves:<br>1. **Paying Rent to Parents (`Sec 10(13A)`):** If user lives in a house owned by retired parents (who are in the 0% slab up to ₹12L in New Regime!), shows exact HRA tax saved.<br>2. **Health Insurance Paid for Parents (`Sec 80D`):** Shows ₹50,000 deduction.<br>3. **Adult Child / Senior Parent FD Allocation:** Gifting money to **Parents or Adult Children (Age $\ge 18$)** does **NOT** trigger `Sec 64` clubbing, shifting 30%-taxed FD interest into a 0%-taxed family member's hands! | `:core:tax-engine` (`FamilyTaxArbitrageEngine.kt`) |
| **F-03** | **Section 50C & Section 43CA Real Estate 10% Stamp-Duty Safe-Harbor Shield** | Property buyers and sellers where Circle Rate / Jantri / Ready Reckoner value is slightly higher than actual agreement value. | Under the **Third Proviso to Section 50C(1)**, if the Stamp Duty Valuation (Circle Rate) does not exceed **110% (10% safe-harbor tolerance)** of the actual sale consideration, the **actual sale price is accepted as Full Value of Consideration** with ₹0 deemed capital gains addition! | `:core:tax-engine` (`CapitalGainsAndForeignAssetEngine.kt`) |
| **F-04** | **Mid-Year Employer `Form 12BB` Investment Declaration PDF Generator** | Salaried employees in Jan–Feb whose HR asks for Form 12BB + proofs to prevent excess TDS in Feb/March payslips. | Generates a ready-to-sign, official **CBDT Rule 26C `Form 12BB` PDF** pre-filled with HRA (Rent, Landlord Name, Landlord PAN), LTA, Home Loan Interest (`Sec 24(b)` Lender Name & PAN), and Chapter VI-A deductions (`80C`, `80CCD(1B)`, `80D`, `80E`, `80G`). | `:feature:pdf-unlocker` (`Form12BbGenerator.kt`) |
| **F-05** | **Section 244A Income Tax Refund Interest Verifier (`0.5% per Month Audit`)** | Taxpayers receiving a refund under `Sec 143(1)` where CPC Bengaluru under-calculates interest u/s 244A. | Under **Section 244A(1)(a)**, when a return is filed on or before the `Sec 139(1)` due date, the government must pay **0.5% simple interest per month (6% p.a.) from April 1 of the AY** to the date of refund grant (if refund $\ge 10\%$ of tax). Audits `143(1)` intimations and flags short-paid interest! | `:core:tax-engine` (`RefundInterest244AAuditor.kt`) |
| **F-06** | **Section 154 Automated Rectification JSON / Grievance Builder** | Users who received a `Sec 143(1)` intimation with a TDS mismatch or arithmetic error. | Generates the exact **Section 154 Rectification („Reprocess the Return“ vs. „Tax Credit Mismatch Correction“)** workflow and payload when an employer updates `Form 26AS` after processing. | `:core:tax-engine` & `:feature:portal-bridge` |
| **F-07** | **Form 15G / Form 15H Senior Citizen Zero-TDS Generator (April 1 Reminder)** | Senior Citizens (`Persona P5` — Harshadbhai) & low-income investors whose banks deduct 10% TDS u/s `194A` on FDs even though their total tax is ₹0. | Generates pre-filled **Form 15H (for Senior Citizens $\ge 60$ with zero net tax up to ₹12L in New Regime!)** and **Form 15G** PDFs every April 1 to submit to banks and stop FD TDS at source. | `:feature:pdf-unlocker` (`Form15G15HGenerator.kt`) |
| **F-08** | **Freelancer / Creator GST & LUT Export Compliance Radar (`₹20L Threshold & e-FIRA Tracker`)** | Freelancers (`Persona P2` — Priya) earning domestic `194J` + foreign Upwork/Stripe income under `Sec 44ADA`/`44AD`. | Tracks cumulative gross receipts against the **₹20,00,000 GST registration threshold**, checks `FIRC / e-FIRA` references, and verifies `ITR-4` turnover against `GSTR-1 / GSTR-3B`. | `:core:tax-engine` (`FreelancerGstReconciler.kt`) |
| **F-09** | **Schedule AL (Assets & Liabilities at Cost) Auto-Builder for Income > ₹50,00,000** | High-earning salaried/freelance filers (`totalIncome > ₹50,00,000`) in `ITR-2` / `ITR-3` where `Schedule AL` is mandatory (`Gate 5`). | Automatically aggregates **Cost of Immovable Properties (`Schedule HP`), Cost of Equity/MF Holdings (`CAMS/NSDL CAS`), Bank Closing Balances as of March 31 (`AA Deposit Summary`), and Loans** into `Schedule AL`. | `:core:tax-engine` (`ScheduleAlAutoBuilder.kt`) |
| **F-10** | **Unlisted Equity & Startup Angel / ESOP Holding Tracker (`Part A-General Shareholding Table`)** | Startup employees, founders, and angel investors holding unlisted shares (`Gate 9`). | Tracks year-by-year `Opening Balance`, `Shares Acquired`, `Shares Transferred`, and `Closing Balance` along with **Company Name, Company PAN, and DIN** required in `ITR-2 Part A-General`. | `:core:database` & `:core:tax-engine` |
| **F-11** | **New Tax Regime "7 Hidden Deductions" Maximizer Card (`Sec 115BAC(2)` Complete Set)** | Users who assume "New Regime = Zero Deductions". | Automatically claims all **7 deductions legally allowed in the New Regime**: (1) `Sec 16(ia)` ₹75,000 Std Ded, (2) `Sec 80CCD(2)` 14% Employer NPS, (3) `Sec 57(iia)` ₹25,000 Family Pension Ded, (4) **Let-Out House Property `Sec 24(b)` Home Loan Interest up to Net Annual Value**, (5) `Sec 10(14)(ii)` ₹38,400 Disabled Transport Allowance, (6) `Sec 10(15)(i)` ₹3,500/₹7,000 Post Office Savings Interest, and (7) `Sec 10(10)` ₹20L Gratuity & `Sec 10(10AA)` ₹25L Leave Encashment. | `:core:tax-engine` (`DeterministicIndianTaxEngine.kt`) |
| **F-12** | **Advance Tax Quarterly Smart Calendar & UPI Pay-Now Nudge (`June 15, Sept 15, Dec 15, Mar 15`)** | Investors, job-switchers, and landlords with non-salary tax > ₹10,000. | Local `WorkManager` job runs 5 days before each quarterly deadline, estimates cumulative shortfall, shows exact **Section 234C interest saved**, and opens `UpiChallan280IntentLauncher` (`Minor Head 100`). | `:feature:portal-bridge` & `:app` |
| **F-13** | **AIS SFT-005 Accrual vs. Cash Basis FD Interest Reconciliation Wizard** | Taxpayers whose banks report cumulative FD interest in `AIS SFT-005` while the taxpayer accounts on maturity. | Prevents double-taxation upon FD maturity in future years by storing an **Already-Offered Accrual Ledger** in `zero_tax_vault.db`. | `:core:database` & `:core:tax-engine` |
| **F-14** | **Section 80C(3A) Life Insurance "10% / 15% Sum-Assured" & `Sec 10(10D)` ₹2.5L ULIP / ₹5L Traditional Cap Guardrail** | Taxpayers buying high-premium ULIPs or traditional endowment policies. | Enforces the `10%/15%` sum-assured cap u/s `80C(3A)` and flags taxable maturity proceeds on ULIPs (> ₹2.5L/yr premium) and traditional policies (> ₹5L/yr premium). | `:core:tax-engine` |
| **F-15** | **100% Offline Natural-Language "What-If" Tax Simulator (`Grounded in Local Vault`)** | Users asking *"If I sell 200 shares of TCS today and buy health insurance for my parents, what happens to my tax?"* | Uses on-device Gemini Nano (`temperature = 0.0f`) to parse the "what-if" question into a `TaxpayerFinancialInput` delta object and runs `DeterministicIndianTaxEngine.compareRegimes()` in `< 5ms`. | `:feature:ai-extractor` & `:core:tax-engine` |

---

## 16. Complete File-by-File, Method-by-Method 12-Step Sequential Execution Plan (`WP-01` through `WP-12` — Un-Abbreviated Developer Blueprint)

Every work package below lists the **exact file paths, Kotlin class & method signatures, algorithmic specifications, and verification gates** so a developer can pick them up sequentially one by one.

---

### Work Package 1 (WP-01): Domain Model Consolidation & Core Tax Engine Bug Fixes (`:core:model` & `:core:tax-engine`) — **COMPLETED ✓**

- **Objective:** Consolidate shared domain models into `:core:model` and fix the critical statutory bugs in `DeterministicIndianTaxEngine.kt` while adding Unexhausted Basic Exemption Absorption, Surcharge 15% Cap on Capital Gains/Dividends, and the Actionable Regime Breakeven Engine.
- **Files Delivered (Available in Canvas):**
  1. **`core/model/src/main/kotlin/in/zerotax/core/model/TaxDomainModels.kt`**
     - Consolidated `AssessmentYearProfile`, `CitizenCategory`, `TaxRegime`, `ResidentialStatus`, `ItrFormType`, `SourceKind`, `ResidenceCityCategory`, `PropertyType`, `DonationCategory`, `EmployerSalarySource` (upgraded with `basicPlusDa`, `exemptHra10_13A`, `sec80CReportedInPartB`, `sec80CCD1BReportedInPartB`, `sec80CCD2EmployerNpsReportedInPartB`, `sec80DReportedInPartB`), `ScheduleItChallanEntry`, `Section80GDonationInput`, `HousePropertyInput`, `AdvanceTaxQuarterlyPayments`, `CapitalGainsWindfallDates`, `Equity112ALot`, `ImmovablePropertyLtcgLot`, `ForeignEquityLotInput`, and `SftAndTcs206CEntry`.
  2. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/calculator/DeterministicIndianTaxEngine.kt`**
     - Fixed **Bug #1 (`Sec 87A` Special-Rate Exclusion)**: Clamped `rebate87A` strictly to `min(baseNormalTax, maxRebate87A)`.
     - Added **Enhancement #2 (`Proviso to Sec 111A & 112A` Unexhausted Basic Exemption Absorption)**: Shifts unused basic exemption (`₹4L` in AY 2026-27 New Regime; `₹3L` in AY 2025-26; `₹2.5L/₹3L/₹5L` in Old Regime) against 20% `STCG` first, and 12.5% `LTCG` above ₹1.25L second.
     - Fixed **Bug #3 (`15%` Surcharge Cap on `Sec 111A`, `112A` & Dividends)**: Implemented `computeSurchargeWithSpecialRateCapAndMarginalRelief()`.
     - Added **Enhancement #4 (`compareRegimes` Actionable Breakeven & Headroom Engine)**: Computes `breakEvenAdditionalDeductionForOldRegime`, `unused80CCD2EmployerNpsHeadroomInNewRegime`, `potentialNewRegimeTaxSavedVia80CCD2`, `unused80DPreventiveCheckup5kHeadroom`, and `seniorParentsUninsuredMedical80DHeadroom`.

---

### Work Package 2 (WP-02): Statutory Sub-Engines — Full Sec 10/16 Matrix, 22-Section Chapter VI-A, Rule 119A/234A/B/C/F Interest, Multi-Property Schedule HP, Presumptive Trio (`44ADA`/`44AD`/`44AE`), RNOR Day-Counter & Sec 89(1) (`:core:tax-engine`) — **NEXT UP (READY TO START)**

- **Objective:** Upgrade `:core:tax-engine` with every statutory sub-engine for Salary Allowances (`Sec 10`), the complete 22-section Chapter VI-A engine (`80C`..`80U`), Rule 2A HRA, Rule 119A(b) / Sec 207 / Sec 234C windfall interest, Multi-Property `Schedule HP`, Presumptive `44ADA`/`44AD`/`44AE`, `Sec 6` RNOR Day-Counter, Family Tax Arbitrage, and `Sec 89(1)` Arrears Relief.
- **Exact Files & Methods to Create / Modify:**
  1. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/routing/DeterministicItrFormRouter.kt`** *(Update)*
     - Add `hasAnyCurrentYearCapitalLoss: Boolean`, `hasPresumptiveBusiness44AD: Boolean`, `grossTurnover44ADRupees: Long`, `cashTurnoverPercentage44AD: Double`, `declaredProfitPercentage44AD: Double`, `goodsCarriageCount44AE: Int`, `hasVdaCryptoIncome115BBH: Boolean`, and `hasPriorYearForm10IeaOptOut: Boolean` to `TaxpayerRoutingProfile`.
     - **Gate 2B Fix:** Trigger `UNSUPPORTED_REQUIRES_CA_AUDIT_44AB` for sub-50% `44ADA` or sub-6%/8% `44AD` **only when `profile.totalIncomeRupees > basicExemptionLimit`**.
     - **Gate 3B Enhancement:** Emit `requiresForm10IeaBeforeDueDate = true` and `isLockedIntoNewRegimePermanentlyUnder115BAC6` in `FormRoutingDecision` when business/profession income is present.
     - **Gate 12B Fix:** Route to `ITR_2` immediately if `hasAnyCurrentYearCapitalLoss == true` even when `equity112ALtcgRupees <= 1_25_000L`.
  2. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/subengines/HraAndInterestEngine.kt`** *(Update)*
     - `fun computeHraExemption(input: HraCalculationInput, landlordPan: String?, taxpayerPan: String? = null): HraCalculationResult` — Enforces the 3 limbs of Rule 2A (`50%` for Mumbai/Delhi/Kolkata/Chennai, `40%` for all other cities), blocks exemption when `actualRentPaidAnnual > 1_00_000L` without valid 10-char `landlordPan` (`!= taxpayerPan`), and supports monthly period-wise calculation.
     - `fun computeStatutoryInterestAndLateFee(...) : StatutoryInterestBreakdown` — Enforces `AssessedTax < 10_000L` immunity (`Sec 208`), `Rule 119A(b)` ₹100 principal truncation (`(unpaidTax / 100L) * 100L`), `Sec 207(2)` Senior Citizen `Sec 234B` immunity, and `Sec 234F` late fee (`₹0` if income $\le$ basic exemption, `₹1,000` if $\le \text{₹5L}$, `₹5,000` if $> \text{₹5L}$).
     - `fun computeSection234CQuarterlyInstallmentInterest(assessedTax: Long, payments: AdvanceTaxQuarterlyPayments, isPresumptive44ADAOr44AD: Boolean, windfalls: CapitalGainsWindfallDates): Section234CResult` — Enforces `12%` Q1 and `36%` Q2 safe harbors, single March 15 installment for `44ADA`/`44AD`, and windfall capital gains exclusion.
  3. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/subengines/ScheduleHpAndPresumptiveSubEngine.kt`** *(New)*
     - `fun computeScheduleHpMultiProperty(properties: List<HousePropertyInput>, regime: TaxRegime): ScheduleHpAggregateResult` — Supports up to 2 `SELF_OCCUPIED` properties at ₹0 GAV (`Sec 23(2)`), `DEEMED_LET_OUT` from 3rd house (`Sec 23(4)`), 1/5th Pre-Construction interest (`Sec 24(b)`), Co-Owner % split (`Sec 26`), 30% rent arrears deduction (`Sec 25A`), New Regime let-out `Sec 24(b)` interest deduction up to NAV (`F-11 #4`), Old Regime ₹2,00,000 inter-head loss set-off cap (`Sec 71(3A)`), and 8-year carry-forward (`Sec 71B`).
     - `fun computePresumptive44Ada(grossReceipts: Long, cashReceipts: Long, declaredProfitRatio: Double): Presumptive44AdaResult`
     - `fun computePresumptive44Ad(digitalReceipts: Long, cashReceipts: Long, declaredDigitalRate: Double = 0.06, declaredCashRate: Double = 0.08): Presumptive44AdResult`
     - `fun computePresumptive44Ae(vehicles: List<GoodsVehicle44AeInput>): Presumptive44AeResult`
  4. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/subengines/SalaryAllowancesAndChapterViaSubEngine.kt`** *(New)*
     - `fun computeSec10AllowancesAndRetirementExemptions(...)` — Computes `Sec 10(10)` Gratuity (₹20L cap), `Sec 10(10AA)` Leave Encashment (₹25L cap), `Sec 10(10B)/(10C)` Retrenchment/VRS (₹5L cap), `Sec 10(10A)` Commuted Pension (1/3rd or 1/2), `Sec 10(14)(ii)` Disabled Transport (`₹38,400` in both regimes) + Children Education/Hostel (`₹9,600`), and `Sec 16(ii)` Govt Entertainment (`₹5,000`).
     - `fun computeCompleteChapterViaDeductions(...)` — Implements all 22 sections (`80C` with `80C(3A)` 10%/15% life insurance cap, `80CCC`, `80CCD(1)` + `80CCD(1B)` ₹50k auto-split, `80CCD(2)` 14%, `80CCH`, 4-bucket `80D` up to ₹1L, `80DD`, `80DDB`, `80E` unlimited education loan, `80EE`/`80EEA`/`80EEB`, `80G` with ₹2k cash gate & 10% Adjusted GTI cap, `80GG` non-HRA rent, `80GGA`, `80GGC`, `80QQB`/`80RRB`, `10(15)(i)` Post Office ₹3.5k/₹7k, `80TTA` ₹10k, `80TTB` ₹50k, and `80U`).
  5. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/subengines/ResidentialStatusAndFamilyArbitrageEngine.kt`** *(New — Features `F-01` & `F-02`)*
     - `fun determineResidentialStatusSec6(...) : ResidentialStatusDecision` — Evaluates `Sec 6(1)` (182-day / 60+365-day / 120-day > ₹15L rules), `Sec 6(1A)` Deemed Resident, and `Sec 6(6)` `RNOR` (9-out-of-10 NR or $\le 729$ days in 7 preceding FYs).
     - `fun simulateFamilyTaxArbitrage(...) : FamilyArbitrageRecommendationList` — Computes exact rupee savings from paying rent to retired parents (`Sec 10(13A)`), paying senior parents' health insurance (`Sec 80D`), and shifting FD principal to adult children/parents.
  6. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/edgecases/Section89ArrearsReliefCalculator.kt`** *(New)*
     - `fun computeSection89Relief(currentYearTaxableIncomeWithoutArrears: Long, arrearsReceived: Long, pastYearTaxableIncome: Long): Section89ReliefResult` (`ECV-01`).
- **Verification Gate:** `./gradlew :core:tax-engine:test` passes all unit tests for `Rule 2A`, `GTV-06`, `GTV-12`, `GTV-14`, `ECV-01`, `ECV-03`, `RNOR`, and `44AD`.

---

### Work Package 3 (WP-03): 3-Way AIS/26AS/Form 16 Reconciler, Rule 9D EPF Split, Full TDS1/TDS2/TDS3/TCS Credit Sweeper, Sec 244A Refund Auditor & Sec 205 Employer Dispute Generator (`:core:tax-engine`)

- **Objective:** Upgrade `DeterministicReconciler.kt` from a salary-only reconciler into a full 7-head 3-Way Reconciliation, TDS/TCS Credit Sweeper, and Post-Intimation Refund Interest Auditor.
- **Exact Files & Methods to Create / Modify:**
  1. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/reconciliation/DeterministicReconciler.kt`** *(Update)*
     - Retain `passesForm16SanityCheck()` and `reconcileSalaries()` (Sec 192(2B) TAN de-duplication + strict Form 26AS Part A TDS ceiling).
     - `fun reconcileOtherSourcesAndRule9dEpf(...) : ReconciledOtherSourcesSummary` — High-water mark across `AIS SFT-005`, `Form 26AS 194A`, and `AA Bank Ledger`; `Proviso to Sec 57(i)` 20% dividend expense cap; `Rule 9D` taxable EPF interest (> ₹2.5L / ₹5L) split; and `Sec 64(1A)` minor child clubbing with `₹1,500/child Sec 10(32)` exemption.
     - `fun sweepAllTdsAndTcsSchedules(...) : ConsolidatedTaxCreditsSummary` — Aggregates `Schedule TDS1` (`192`), `Schedule TDS2` (`194A`, `194`, `194K`, `194J`, `194C`, `194S`), `Schedule TDS3` (`194-IA 26QB`, `194-IB 26QC`), and `Schedule TCS` (`206C(1G)` 5%/20% LRS/Foreign Travel & `206C(1F)` 1% Car TCS), plus SFT High-Value Spend Mismatch warnings (`SFT-012` Credit Card, `SFT-003` Property).
     - `fun generateEmployerTdsDiscrepancyNotice(item: EmployerReconciliationItem, pan: String, employeeName: String, assessmentYear: String): String` — Generates a statutory legal demand letter to Employer HR/Payroll citing **Sections 199, 201(1A), and 205 of the Income-tax Act, 1961**.
  2. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/reconciliation/RefundInterest244AAndGstAuditor.kt`** *(New — Features `F-05`, `F-06`, `F-08`)*
     - `fun auditSection244ARefundInterest(refundPrincipal: Long, taxDetermined: Long, filedOnOrBeforeDueDate: Boolean, monthsFromApril1ToIntimation: Int, cpcGranted244AInterest: Long): Refund244AAuditResult` — Verifies the `0.5% per month` (`6% p.a.`) government refund interest from April 1 and builds a `Section 154` rectification payload if underpaid.
     - `fun reconcileFreelancerGstAndEfira(grossReceipts44AdaOrAd: Long, gstTurnoverReported: Long, foreignRemittancesWithoutEfira: Long): FreelancerGstComplianceReport` — Flags the ₹20L GST threshold and missing `e-FIRA` certificates.

---

### Work Package 4 (WP-04): Capital Gains Indexation, Sec 50C 10% Safe-Harbor, Sec 54/54EC/54F & CGAS Planner, Buyback Loss (`Sec 46A`), SGB Exemption, Loss Matrix, Schedule VDA, Schedule FA, ITR-U & Next-Year CTC Simulator (`:core:tax-engine`)

- **Objective:** Complete all Phase 2 and Phase 3 investment, property, foreign asset, revision, and wealth-simulation sub-engines in `:core:tax-engine`.
- **Exact Files & Methods to Create / Modify:**
  1. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/advanced/CapitalGainsAndForeignAssetEngine.kt`** *(Update)*
     - `fun computeGrandfathered112AGain(lot: Equity112ALot): Long` (`GTV-09`).
     - `fun computeImmovablePropertyLtcgTax(lot: ImmovablePropertyLtcgLot, isResidentIndividual: Boolean = true): ImmovablePropertyTaxComparison` — Applies **Feature `F-03` (`Sec 50C` 10% Stamp Duty Safe-Harbor: `stampDutyValue <= actualSalePrice * 1.10`)** and compares **12.5% unindexed vs. 20% CII-376 indexed** (`GTV-10`, `ECV-02`).
     - `fun computeSection54SeriesAndCgasPlan(grossLtcg: Long, netConsideration: Long, isResidentialHouseSold: Boolean, saleDateIso: String, reinvestedInHouse: Long, depositedInCgas: Long, investedIn54EcBonds: Long): CapitalGainsExemptionPlan` — Enforces the ₹10 Crore `Sec 54`/`54F` cap, once-in-a-lifetime 2-house `Sec 54` option (for LTCG $\le \text{₹2 Cr}$), ₹50 Lakh `Sec 54EC` bond cap, 6-month bond deadline, and CGAS 1988 deposit alert.
     - `fun computeShareBuybackPostOct2024(buybackProceedsReceived: Long, originalPurchaseCost: Long, heldOver12Months: Boolean): ShareBuybackTaxTreatment` — Maps proceeds to `Schedule OS` Deemed Dividend u/s `2(22)(f)` and records `-originalPurchaseCost` as `LTCL`/`STCL` u/s `46A`.
     - `fun applyCapitalLossSetOff(...) : CapitalLossSetOffSummary` — Enforces `Sec 70(2)/(3)` and `Sec 74` (`LTCL` strictly blocked against `STCG`; 8-year carry-forward in `Schedule CFL`, `GTV-11`).
     - `fun computeScheduleVda115BBH(vdaLots: List<VdaTradeLotInput>, tds194SCredited: Long): ScheduleVdaResult` — Enforces flat 30% tax on profitable tokens, blocks loss set-off across tokens (`Sec 115BBH(2)(b)`), and claims 1% `194S` TDS.
     - `fun computeForm67ForeignTaxCredit(...) : Pair<Long, Long>` (`Rule 128(5)` preceding-month-end SBI TTBR, `GTV-16`).
  2. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/foreignassets/ScheduleFaCalendarYearEngine.kt`** *(Update)*
     - `interface SbiTtbrLookupRepository { fun getUsdTtbrOnOrBefore(isoDate: String): Double }`
     - `class ScheduleFaCalendarYearEngine(private val ttbrRepo: SbiTtbrLookupRepository)` with `fun buildTableA3RowForLot(lot: ForeignEquityLotInput, calendarYear: Int): ScheduleFaTableA3Row` (`GTV-15`), plus `Sec 49(2AA)` RSU vest-date FMV cost basis tracking so perquisites u/s `17(2)` are never double-taxed.
  3. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/revision/RevisionAndHarvestingEngine.kt`** *(Update)*
     - `enum class ItrUWindow(val additionalTaxRate: Double)` (`25%`, `50%`, `60%`, `70%` u/s `140B(3)`).
     - `fun computeUpdatedReturnItrU(baseAdditionalTax: Long, totalInterest234A_B_C: Long, filingWindow: ItrUWindow): ItrUComputationResult` (`GTV-17`).
     - `fun computeMarchLtcgHarvestingHeadroom(realizedLtcgSoFarRupees: Long, unrealizedLtcgAvailableRupees: Long): Pair<Long, Long>` (`GTV-18`).
     - `fun simulateNextYearCtcRestructuring(...) : CtcRestructuringBlueprint` — Calculates exact New & Old Regime savings from shifting salary into 14% Corporate NPS (`Sec 80CCD(2)`).
     - `fun buildScheduleAlAssetsAndLiabilities(...) : ScheduleAlPayload` (`Feature F-09` for Total Income > ₹50L).

---

### Work Package 5 (WP-05): Official CBDT JSON Compilers, 6 Category 'A' Defect Gates, Prior-Year ITR JSON Importer & 27+ JUnit 5 Test Suite (`:core:tax-engine`)

- **Objective:** Implement the production CBDT JSON schema compilers (`ITR-1`, `ITR-2`, `ITR-4`, `Form 67`, `ITR-U`), the 6 Category 'A' pre-submission defect gates, the offline Prior-Year ITR JSON Carry-Forward Importer, and the complete JUnit 5 regression test suite (`GTV-01..18` + `ECV-01..06` + Deep Enhancement Tests).
- **Exact Files & Methods to Create / Modify:**
  1. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/cbdtcompiler/CbdtItr1JsonCompiler.kt`** *(Update)*
     - `fun compileAndValidateItr1Json(payload: CbdtItr1ExportPayload): Result<String>` — Generates official CBDT `ITR.ITR1` JSON (`Form_ITR1`, `PersonalInfo` with `AadhaarCardNo = "[Aadhaar Redacted]"`, `FilingStatus`, `ITR1_IncomeDeductions`, `ITR1_TaxComputation`, `TaxPaid`, `Refund.BankAccountDtls`, `ScheduleIT`, `TDSonSalaries`, `TDSonOthThanSals`, `ScheduleTCS`, `Verification`).
  2. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/cbdtcompiler/CbdtCategoryADefectGate.kt`** *(New)*
     - Enforces all 6 Category 'A' blockers:
       - `GATE-CAT-A-01`: Individual PAN format (`^[A-Z]{3}P[A-Z][0-9]{4}[A-Z]$`)
       - `GATE-CAT-A-02`: `AadhaarCardNo == "[Aadhaar Redacted]"`
       - `GATE-CAT-A-03`: `TotalTDSSal <= GrossSalary` and capped at Form 26AS Part A
       - `GATE-CAT-A-04`: `TotalChapVIADeductions <= GrossTotalIncome` (`Sec 80A(2)`)
       - `GATE-CAT-A-05`: Valid 11-char IFSC (`^[A-Z]{4}0[A-Z0-9]{6}$`) with `UseForRefund == "true"`
       - `GATE-CAT-A-06`: `Form 10E` acknowledgment number present when `reliefUs89 > 0L` & `Form 10-IEA` ack present when `ITR-4` opts for Old Regime
  3. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/cbdtcompiler/Form67AndItr2ScheduleCompiler.kt`** *(New)*
     - `fun compileForm67Rule128Payload(...) : JSONObject`
     - `fun compileItr2ForeignSchedulesAndItrU(...) : JSONObject` (`ScheduleFA.DtlsForeignEquity`, `ScheduleFSI`, `ScheduleTR1`, `ScheduleAL`, `PartA_Gen_UnlistedEquity`, `PartB_TI_ITR_U`).
  4. **`core/tax-engine/src/main/kotlin/in/zerotax/core/taxengine/cbdtcompiler/PriorYearItrJsonImporter.kt`** *(New)*
     - `fun importPriorYearCbdtItrJson(rawJson: String): Result<PriorYearCarryForwardBundle>` — Extracts `TaxpayerProfile`, primary bank `IFSC` + Account No., Landlord PAN, Unlisted Share opening balances (`F-10`), and 8-year `Schedule CFL` losses with `Sec 139(1)` timely filing check (`Sec 80`).
  5. **`core/tax-engine/src/test/kotlin/in/zerotax/core/taxengine/MasterGoldenTaxVectorTestSuite.kt`** & **`AdvancedTaxEngineEdgeCaseTest.kt`** *(Update)*
     - Full JUnit 5 verification suite covering `GTV-01` through `GTV-18` and `ECV-01` through `ECV-06`, plus unit tests for Basic Exemption Absorption, 15% Surcharge Cap, `Sec 50C` 10% safe harbor, `Sec 44AD` 6% digital rate, `RNOR`, and `Sec 244A` refund interest.

---

### Work Package 6 (WP-06): 13-Table Encrypted SQLCipher Vault, Seed Databases & Hardware Security (`:core:database` & `:core:security`)

- **Objective:** Expand `:core:database` from the 3-table prototype to the full 13-table Room + SQLCipher 4.5.4 schema with bundled offline seed data (25-year CII table, Jan 31, 2018 ISIN FMV table, and 10-year SBI TTBR table), fix the `SupportFactory` connection re-open crash, add Multi-PAN Family Vault support and `.ztvault` encrypted backup, and upgrade `:core:security` with 16-digit UIDAI VID scrubbing and hardware ECDSA audit signing.
- **Exact Files & Methods to Create / Modify:**
  1. **`core/database/src/main/assets/sql/zero_tax_vault_schema_and_seed.sql`** *(Update)*
     - Active `CREATE TABLE IF NOT EXISTS` DDL for all **13 tables**:
       1. `taxpayer_profile`
       2. `income_line_items` (with Level-3 provenance: `source_document_sha256`, `source_page_number`, `source_bounding_box_json`, `formula_explanation`)
       3. `house_property_schedule_hp`
       4. `presumptive_business_44ada`
       5. `capital_gains_112a_lots`
       6. `historical_fmv_31jan2018_lookup`
       7. `cost_inflation_index_cii`
       8. `schedule_cfl_loss_carry_forward`
       9. `chapter_via_deductions_and_80g`
       10. `sbi_ttbr_exchange_rates`
       11. `schedule_fa_equity_holdings`
       12. `schedule_it_and_tds_credits`
       13. `user_legal_consents_and_overrides`
     - `INSERT OR REPLACE` seed rows for all **25 Cost Inflation Index years (`2001-02` = 100 through `2025-26` = 376)**, benchmark Jan 31, 2018 ISIN FMVs, and 10-year SBI TTBR USD-INR rates.
  2. **`core/database/src/main/kotlin/in/zerotax/core/database/ZeroTaxVaultDatabase.kt`** & **`SqlCipherHardwareVaultBuilder.kt`** *(Update)*
     - Add Room `@Entity` classes for all 13 tables and `@Dao` interfaces (`TaxVaultDao`, `LegalAuditDao`, `SbiTtbrAndCiiLookupDao` with `WHERE currency_code = :currency AND rate_date_iso <= :targetIsoDate ORDER BY rate_date_iso DESC LIMIT 1`, `TaxVaultSummaryDao`).
     - Fix `SupportFactory(passphrase, null, false)` so connection pool re-opens never fail, add `BiometricPrompt.CryptoObject` unlock binding, add `getAllFamilyProfiles(): List<TaxpayerProfileEntity>` for the Multi-PAN Family Switcher, and add `EncryptedVaultBackupManager.exportAndImportZtVault()` (`PBKDF2WithHmacSHA512 + AES-256-GCM`).
  3. **`core/security/src/main/kotlin/in/zerotax/core/security/UidaiAadhaarVerhoeffScrubber.kt`** & **`CanonicalWarningHasher.kt`** *(Update)*
     - Add 16-digit UIDAI Virtual ID (`VID`) regex + Verhoeff checksum redaction in `UidaiAadhaarVerhoeffScrubber`.
     - Add hardware-backed `SHA256withECDSA` (`AndroidKeyStore`) signature generation over `LegalAuditEntity` rows in `CanonicalWarningHasher`.
     - Add pre-shred RBI AA consent revocation hook in `DpdpVaultShredder`.

---

### Work Package 7 (WP-07): 28-Permutation PDF Unlocker, TRACES Signature Verifier, Domestic CAS/Broker Parser, Foreign RSU Parser & Offline PDF Generators (`:feature:pdf-unlocker`)

- **Objective:** Upgrade `:feature:pdf-unlocker` with the 28-permutation smart password matrix, TRACES digital signature & certificate authenticity verification, `sortByPosition = true` multi-column table parsing, CAMS/Zerodha/Groww Capital Gains extraction, E\*TRADE/Schwab/Fidelity RSU + Form 1042-S extraction, and local PDF generators for `Form 12BB` (`F-04`), `Form 15G/15H` (`F-07`), and the 2-Page CA Tax Computation Dossier.
- **Exact Files & Methods to Create / Modify:**
  1. **`feature/pdf-unlocker/src/main/kotlin/in/zerotax/feature/pdfunlocker/SilentPdfUnlocker.kt`** *(Update)*
     - `fun generateSmart28Candidates(pan: String, dobDdMmYyyy: String, firstName: String, lastName: String = "", mobileLast4: String = "", accountLast4: String = "", customerId: String = ""): List<String>` + in-memory per-issuer password pattern cache.
  2. **`feature/pdf-unlocker/src/main/kotlin/in/zerotax/feature/pdfunlocker/DeterministicTracesRegexParser.kt`** *(Update)*
     - Enforce `PDFTextStripper().apply { sortByPosition = true }`, verify the 7-character TRACES Certificate Number and `PDSignature` dictionary (`isAuthenticTracesCertificate`), and extract full Part B Chapter VI-A deductions (`80C`, `80CCD(1B)`, `80CCD(2)` Employer NPS, `80D`, `Sec 10(13A)` HRA, `Basic + DA`) + quarterly Part A receipt numbers.
  3. **`feature/pdf-unlocker/src/main/kotlin/in/zerotax/feature/pdfunlocker/DomesticCapitalGainsPdfParser.kt`** & **`ForeignBrokerRsuAnd1042SParser.kt`** *(New)*
     - Parse CAMS/KFintech CAS and Zerodha/Groww Tax P&L tables into `Equity112ALot` records with automatic Section 234C transfer quarter bucketing (`P1_UPTO_15JUN`..`P5_16MAR_31MAR`), `Sec 47(viic)` SGB maturity tagging, and `Sec 46A` buyback tagging.
     - Parse E\*TRADE, Charles Schwab, and Fidelity RSU/ESPP confirmations and IRS Form 1042-S statements into `ForeignEquityLotInput`.
  4. **`feature/pdf-unlocker/src/main/kotlin/in/zerotax/feature/pdfunlocker/LocalStatutoryPdfGenerators.kt`** *(New — Features `F-04`, `F-07` & CA Dossier)*
     - `fun generateForm12BbPdf(...) : ByteArray` (Mid-year employer investment declaration).
     - `fun generateForm15GOr15HPdf(...) : ByteArray` (April 1 Senior Citizen / zero-tax bank FD declaration).
     - `fun generateCaTaxComputationAndProvenancePdf(...) : ByteArray` (2-page offline Tax Computation & Level-3 Provenance Dossier).

---

### Work Package 8 (WP-08): On-Device AICore Receipt Structurer, 25-Row Bank Narration Classifier, 6-Locale Vernacular Copilot & Offline "What-If" Simulator (`:feature:ai-extractor`)

- **Objective:** Complete all four bounded on-device Gemini Nano (`com.google.ai.edge.aicore`, `temperature = 0.0f`, `topK = 1`) roles plus the Natural-Language "What-If" Tax Simulator (`F-15`), gated by the $\pm\text{₹5}$ Post-AI Math Sanity Firewall and `{{TOKEN}}` locking.
- **Exact Files & Methods to Create / Modify:**
  1. **`feature/ai-extractor/src/main/kotlin/in/zerotax/feature/aiextractor/OnDeviceForm16AiExtractor.kt`** *(Update)*
     - Populate `tdsReported`, `sec80CReportedInPartB`, `sec80CCD1BReportedInPartB`, `sec80CCD2EmployerNpsReportedInPartB`, `sec80DReportedInPartB`, `exemptHra10_13A`, and `basicPlusDa` from `P1-PROMPT-01`.
  2. **`feature/ai-extractor/src/main/kotlin/in/zerotax/feature/aiextractor/OnDeviceReceiptAnd80GExtractor.kt`** *(New)*
     - Implement **Prompt `P1-PROMPT-02`** for physical Rent Receipts (Landlord Name, Landlord PAN, Address, Monthly Rent), `Sec 80D` Medical Insurance & Uninsured Senior Citizen Medical Bills, and `Sec 80G` Donation Receipts (Donee Name, Donee PAN, `Form 10BE` ARN, Cash vs. Digital mode).
  3. **`feature/ai-extractor/src/main/kotlin/in/zerotax/feature/aiextractor/OnDeviceBankNarrationClassifier.kt`** *(New)*
     - Implement **Prompt `P2-PROMPT-01`** batching Account Aggregator bank rows in chunks of 25 (filtering out personal UPI debits < ₹500) into `SECTION_80C_LIC_PPF_ELSS_TUITION`, `SECTION_80D_HEALTH_INS`, `SECTION_80D_PREVENTIVE_OR_SENIOR_MEDICAL`, `SECTION_80E_EDUCATION_LOAN`, `SECTION_80G_DONATION`, `SECTION_44ADA_FREELANCE_INFLOW`, `SECTION_44AD_BUSINESS_RECEIPT`, `SAVINGS_INTEREST`, and `PERSONAL_EXPENSE`.
  4. **`feature/ai-extractor/src/main/kotlin/in/zerotax/feature/aiextractor/TokenLockedVernacularExplainer.kt`** *(Update)*
     - Expand `SupportedVernacularLocale` to 6 languages (`en-IN`, `hi-IN`, `gu-IN`, `mr-IN`, `ta-IN`, `te-IN`), implement **Prompt `P3-PROMPT-01`** for CPC notices (`143(1)`, `139(9)`, `148`, `245`, `AIS e-Campaign`) with strict byte-for-byte `{{TOKEN}}` preservation (`ECV-06`), and add `OfflineWhatIfTaxSimulator.parseWhatIfQueryToInputDelta()` (`Feature F-15`).

---

### Work Package 9 (WP-09): RBI Account Aggregator In-Memory CAS/Deposit Parser & Consent Manager (`:feature:aa-aggregator`)

- **Objective:** Parse decrypted ReBIT Sahamati `DEPOSIT`, `MUTUAL_FUNDS`, and `EQUITIES` payloads inside `RbiAaEphemeralDecryptor` in volatile RAM before `0x00` byte zero-wiping, and provide the Consent Artifact revocation manager for Privacy Tab 3.
- **Exact Files & Methods to Create / Modify:**
  1. **`feature/aa-aggregator/src/main/kotlin/in/zerotax/feature/aaaggregator/RebitFiDataInMemoryParser.kt`** *(New)*
     - Parse decrypted ReBIT Sahamati `DEPOSIT` XML/JSON in memory to extract savings interest, FD interest, March 31 closing balance (for `Schedule AL`), and high-value narration rows ($\ge\text{₹500}$).
     - Parse decrypted `MUTUAL_FUNDS` and `EQUITIES` CAS payloads into `Equity112ALot` records with offline Jan 31, 2018 FMV lookup.
  2. **`feature/aa-aggregator/src/main/kotlin/in/zerotax/feature/aaaggregator/AaConsentArtifactManager.kt`** *(New)*
     - Track active FIP consents (`consentId`, `fipIds`, `purposeCode = "101"`, `dataLife = 0`, `fetchType = "ONETIME"`) and expose `revokeActiveConsents()` for Privacy Tab 3 and `DpdpVaultShredder`.

---

### Work Package 10 (WP-10): Dual Fetch/XHR Portal Bridge, Refund Bank Pre-Validation, UPI Challan 280 Launcher, 1-Tap JSON Upload, AIS Dispute Bridge & Advance Tax Worker (`:feature:portal-bridge`)

- **Objective:** Upgrade `:feature:portal-bridge` with dual `fetch + XMLHttpRequest` interception, Refund Bank EVC pre-validation, `Sec 245` demand check, UPI Challan 280 (`0021`/`300` & `100`), 1-tap CBDT JSON upload + Aadhaar OTP e-Verification, AIS discrepancy dispute submission, and the Quarterly Advance Tax Calendar Worker (`F-12`).
- **Exact Files & Methods to Create / Modify:**
  1. **`feature/portal-bridge/src/main/kotlin/in/zerotax/feature/portalbridge/UpiChallan280IntentLauncher.kt`** *(New)*
     - `fun buildChallan280UpiUri(vpa: String, payeeName: String, amountRupees: Long, pan: String, assessmentYear: String, minorHead: String = "300"): Uri` (supports `300` Self-Assessment Tax and `100` Advance Tax).
     - `fun launchChallanPayment(context: Context, upiUri: Uri): Result<Unit>`
     - `fun validateChallanDepositRecord(entry: ScheduleItChallanEntry): Result<Unit>` (enforcing `^\d{7}$` BSR Code, `^\d{5}$` Challan Serial Number, and non-future ISO deposit date).
  2. **`feature/portal-bridge/src/main/kotlin/in/zerotax/feature/portalbridge/HardenedEphemeralPortalBridge.kt`** *(Update)*
     - Hook both `window.fetch` and `XMLHttpRequest.prototype.open/send` in `getReadOnlyInterceptorScript()`, replace local Aadhaar regex with `UidaiAadhaarVerhoeffScrubber.scrubAllAadhaarDigits()`, verify Refund Bank Account `isValidated && isEvcEnabled` status in `Prefill.json`, check for outstanding `Sec 245` demands, and add `uploadCompiledItrJsonAndVerify()`, `submitAisDiscrepancyFeedback()`, and `fetchPostFilingCpcStatus()` with mandatory 500ms cookie/DOM wipe.

---

### Work Package 11 (WP-11): Full Jetpack Compose 4-Tap Linear Spine, 3-Tab Pro Workspace, Family Switcher & Wealth Simulators (`:app`)

- **Objective:** Wire the complete Jetpack Compose UI across all 4 Onboarding Taps and all 3 Pro Workspace Tabs, integrating every statutory nudge card, provenance sheet, Multi-PAN Family Switcher, and tax-saving simulator.
- **Exact Files & Methods to Create / Modify:**
  1. **`app/src/main/kotlin/in/zerotax/app/ui/utils/IndianCurrencyFormatter.kt`** & **`ScopedFlagSecureHelper.kt`** *(New)*
     - Format all rupee amounts in Indian Lakh/Crore grouping (`₹24,50,000`) with `tnum` tabular monospace numerals and TalkBack accessibility labels, plus a 5-minute scoped `FLAG_SECURE` override in `MainActivity` when exporting `ITR-V`, `Form 12BB`, `Form 15G/15H`, or the CA Computation Dossier.
  2. **`app/src/main/kotlin/in/zerotax/app/ui/components/StatutoryCardsAndModals.kt`** *(New)*
     - `DpdpSection6MultilingualConsentCard` (`en-IN`, `hi-IN`, `gu-IN`)
     - `OneTapPdfDisambiguationCard` (cropped PDF region bitmap viewer when Form 16 math sanity $> \pm\text{₹5}$)
     - `EmployerTdsShieldCard` (with 1-tap `Sec 205` HR Dispute Letter generator)
     - `RegimeBreakEvenOptimizerCard` (showing exact additional `80C`/`80D`/`HRA`/`80CCD(2)` needed)
     - `Presumptive44AdaAnd44AdSliderCard` (`50%..100%` for `44ADA`; `6%/8%..100%` for `44AD` + `Form 10-IEA` lock-in alert)
     - `BlackMoneyActForeignAssetModal` (₹10L penalty shield u/s 42 & 43)
     - `Level3ProvenanceBottomSheet` (SHA-256 document hash, page/box coordinates, statutory formula)
     - `InteractiveRnorDayCounterCard` (`F-01`), `FamilyTaxArbitrageCard` (`F-02`), `NextYearCtcRestructuringSimulatorCard`, `Section54ExemptionPlannerCard`, and `OfflineWhatIfSimulatorCard` (`F-15`).
  3. **`app/src/main/kotlin/in/zerotax/app/ui/navigation/ZeroTaxFourTapSpineScaffold.kt`** *(Update)*
     - Wire the Top-Bar Multi-PAN Family Switcher (`Self | Spouse | Senior Parents`), Tap 1 through Tap 4, and Persistent Workspace Tabs 1, 2, and 3 end-to-end.

---

### Work Package 12 (WP-12): Root README, Detekt OWASP MASVS Rules & CI/CD Branch Alignment (`.github/workflows` & Root)

- **Objective:** Add a comprehensive root `README.md`, configure Detekt static analysis rules enforcing the pure-JVM `:core:tax-engine` boundary and zero-logging OWASP MASVS rules, and update `.github/workflows/zerotax-ci.yml` to trigger on `master`, `main`, and `release/*`.

---

## 17. Master Progress Tracker

| Step | Work Package | Target Module(s) | Status |
| :---: | :--- | :--- | :---: |
| **1** | **WP-01** | `:core:model`, `:core:tax-engine` | **Completed ✓** (`TaxDomainModels.kt` & `DeterministicIndianTaxEngine.kt`) |
| **2** | **WP-02** | `:core:tax-engine` | **Ready to Start** |
| **3** | **WP-03** | `:core:tax-engine` | Pending |
| **4** | **WP-04** | `:core:tax-engine` | Pending |
| **5** | **WP-05** | `:core:tax-engine` | Pending |
| **6** | **WP-06** | `:core:database`, `:core:security` | Pending |
| **7** | **WP-07** | `:feature:pdf-unlocker` | Pending |
| **8** | **WP-08** | `:feature:ai-extractor` | Pending |
| **9** | **WP-09** | `:feature:aa-aggregator` | Pending |
| **10** | **WP-10** | `:feature:portal-bridge` | Pending |
| **11** | **WP-11** | `:app` | Pending |
| **12** | **WP-12** | Root, `.github/workflows` | Pending |

---

## 18. Official CBDT JSON Schema Blueprints (`ITR-1`, `ITR-4`, `ITR-2`, `Form 67`, `ITR-U`) & All 25 Category 'A' Defect Validation Rules

A developer with no prior experience with the Income Tax Department (ITD) e-Filing portal needs the exact JSON structure accepted by the CBDT Offline Utility / e-Filing API (`AY 2026-27`, Schema Version `1.2`) and the exact 25 pre-flight validation gates that prevent a defective return notice under **Section 139(9)**.

### 18.1 Master CBDT JSON Envelope & Header (`Form_ITR1`, `Form_ITR2`, `Form_ITR4`)
Every CBDT ITR JSON payload wraps the return inside a root `ITR` object containing the specific form key (`ITR1`, `ITR2`, or `ITR4`). All monetary amounts in the final exported JSON MUST be **non-negative whole integers (`Long`)** rounded according to statutory rules (`Sec 288A` / `Sec 288B` for totals, nearest rupee for line items), with zero floating-point decimals.

```json
{
  "ITR": {
    "ITR1": {
      "CreationInfo": {
        "SWVersionNo": "2026.1.0",
        "SWCreatedBy": "SW20000001",
        "JSONCreatedBy": "SW20000001",
        "JSONCreationDate": "2026-07-15",
        "IntermediaryCity": "Ahmedabad",
        "Digest": "SHA256_HEX_DIGEST_OF_CANONICAL_PAYLOAD"
      },
      "Form_ITR1": {
        "FormName": "ITR-1",
        "Description": "For Individuals having Income from Salaries, one house property, other sources (Interest etc.) and agricultural income upto Rs.5 thousand",
        "AssessmentYear": "2026",
        "SchemaVer": "Ver1.2",
        "FormVer": "Ver1.0"
      },
      "PersonalInfo": {
        "AssesseeName": {
          "FirstName": "SATISH",
          "MiddleName": "",
          "SurNameOrOrgName": "JADAV"
        },
        "PAN": "ABCDE1234F",
        "Address": {
          "ResidenceNo": "402",
          "ResidenceName": "SHIVALIK HIGH STREET",
          "RoadOrStreet": "SG HIGHWAY",
          "LocalityOrArea": "BODAKDEV",
          "CityOrTownOrDistrict": "AHMEDABAD",
          "StateCode": "11",
          "CountryCode": "91",
          "PinCode": 380054,
          "CountryCodeMobile": 91,
          "MobileNo": 9876543210,
          "EmailAddress": "taxpayer@example.com"
        },
        "DOB": "1992-05-14",
        "EmployerCategory": "OTH",
        "AadhaarCardNo": "REDACTED_UNTIL_EXPORT"
      },
      "FilingStatus": {
        "ReturnFileSec": 11,
        "SeventhProviso139": "N",
        "NewTaxRegime": "Y",
        "OptOutNewTaxRegime115BAC": "N",
        "Form10IEADetails": null
      }
    }
  }
}
```

#### Statutory Code Reference Tables for `PersonalInfo` & `FilingStatus`
- **`EmployerCategory` Enum:**
  - `"CGOV"`: Central Government
  - `"SGOV"`: State Government
  - `"PSU"`: Public Sector Undertaking
  - `"PE"`: Pensioners (Central/State/PSU/Others)
  - `"OTH"`: Private Sector / Other Employers
  - `"NA"`: Not Applicable (Non-salaried)
- **`ReturnFileSec` Enum (Section 139 Filing Type):**
  - `11`: On or before due date u/s `139(1)` (July 31, 2026)
  - `12`: Belated Return u/s `139(4)` (Aug 1, 2026 – Dec 31, 2026)
  - `17`: Revised Return u/s `139(5)` (Requires `"OrigRetFiledDate"` and 15-digit `"ReceiptNo"` / Acknowledgment Number)
  - `18`: Defective Return response u/s `139(9)` (Requires `"NoticeNo"` and `"NoticeDate"`)
  - `20`: Updated Return (`ITR-U`) u/s `139(8A)`

---

### 18.2 `ITR-1 (Sahaj)` Complete Income, Deduction & Tax Computation Nodes
```json
{
  "ITR1_IncomeDeductions": {
    "GrossSalary": 2400000,
    "Salary": 2350000,
    "PerquisitesValue": 50000,
    "ProfitsInSalary": 0,
    "AllwncExemptUs10": {
      "AllwncExemptUs10Dtls": [
        {
          "SalNatureDesc": "10(13A)",
          "SalOthAmount": 180000
        }
      ],
      "TotalAllwncExemptUs10": 180000
    },
    "NetSalary": 2220000,
    "DeductionUs16": 50000,
    "DeductionUs16ia": 50000,
    "EntertainmentAlw16ii": 0,
    "ProfessionalTaxUs16iii": 2400,
    "IncomeFromSal": 2167600,
    "TypeOfHP": "S",
    "GrossRentReceived": 0,
    "TaxPaidlocalAuth": 0,
    "AnnualValue": 0,
    "StandardDeduction": 0,
    "InterestPayable": 200000,
    "ArrearsUnrealizedRentRcvd": 0,
    "TotalIncomeOfHP": -200000,
    "IncomeOthSrc": 42000,
    "OthersInc": {
      "OthersIncDtlsOthSrc": [
        { "OthSrcNatureDesc": "SAV", "OthSrcOthAmount": 18000 },
        { "OthSrcNatureDesc": "FD", "OthSrcOthAmount": 14000 },
        { "OthSrcNatureDesc": "DIV", "OthSrcOthAmount": 10000 }
      ]
    },
    "DeductionUs57iia": 0,
    "GrossTotIncome": 2009600,
    "UsrDeductUndChapVIA": {
      "Section80C": 150000,
      "Section80CCC": 0,
      "Section80CCDEmployeeOrSE": 0,
      "Section80CCD1B": 50000,
      "Section80CCDEmployer": 120000,
      "Section80D": 75000,
      "Section80DD": 0,
      "Section80DDB": 0,
      "Section80E": 40000,
      "Section80EE": 0,
      "Section80EEA": 0,
      "Section80EEB": 0,
      "Section80G": 25000,
      "Section80GG": 0,
      "Section80GGA": 0,
      "Section80GGC": 0,
      "Section80U": 0,
      "Section80TTA": 10000,
      "Section80TTB": 0,
      "AnyOthSec80CCH": 0,
      "TotalChapVIADeductions": 470000
    },
    "DeductUndChapVIA": {
      "Section80C": 150000,
      "Section80CCC": 0,
      "Section80CCDEmployeeOrSE": 0,
      "Section80CCD1B": 50000,
      "Section80CCDEmployer": 120000,
      "Section80D": 75000,
      "Section80E": 40000,
      "Section80G": 25000,
      "Section80TTA": 10000,
      "TotalChapVIADeductions": 470000
    },
    "TotalIncome": 1539600,
    "ExemptIncAgriOthUs10": {
      "ExemptIncAgriOthUs10Dtls": [
        { "NatureDesc": "AGRI", "OthAmount": 4000 }
      ],
      "ExemptIncAgriOthUs10Total": 4000
    }
  },
  "ITR1_TaxComputation": {
    "TotalTaxPayable": 274380,
    "Rebate87A": 0,
    "TaxPayableOnRebate": 274380,
    "EducationCess": 10975,
    "GrossTaxLiability": 285355,
    "Section89": 0,
    "NetTaxLiability": 285355,
    "TotalIntrstPay": 0,
    "IntrstPay": {
      "IntrstPayUs234A": 0,
      "IntrstPayUs234B": 0,
      "IntrstPayUs234C": 0,
      "LateFilingFee234F": 0
    },
    "TotTaxPlusIntrstPay": 285355
  }
}
```

---

### 18.3 `ITR-4 (Sugam)` Presumptive Business & Profession Schema (`ScheduleBP`, `Form10IEA`)
When `DeterministicItrFormRouter` selects `ITR_4_SUGAM`, the compiler outputs `ScheduleBP` (Presumptive Business & Profession) and `PartB-TI` alongside the mandatory `ScheduleBP.PersumptiveInc44AD` and `ScheduleBP.PersumptiveInc44ADA` blocks:
```json
{
  "ScheduleBP": {
    "PersumptiveInc44AD": {
      "GrsTrnOverBank": 8500000,
      "GrsTrnOverAnyOthMode": 300000,
      "TotPersumptiveInc44AD": 534000
    },
    "PersumptiveInc44ADA": {
      "GrsReceipt": 4200000,
      "TotPersumptiveInc44ADA": 2100000
    },
    "PersumptiveInc44AE": {
      "TotPersumptiveInc44AE": 0,
      "SalInterestByFirm": 0
    },
    "NoBooksOfAccBS": {
      "TotSundryDbtAmt": 150000,
      "TotSundryCrdAmt": 45000,
      "TotStkInTradAmt": 0,
      "CashBalAmt": 25000
    }
  },
  "Form10IEADetails": {
    "AcknowledgementNo": "123456789012345",
    "DateOfFiling": "2026-07-10"
  }
}
```
- **Critical Developer Rule for `NoBooksOfAccBS`:** Even though `Sec 44AD` / `Sec 44ADA` exempts the taxpayer from maintaining detailed books of account u/s `44AA`, the CBDT `ITR-4` JSON schema marks the 4 summary Balance Sheet fields (`TotSundryDbtAmt`, `TotSundryCrdAmt`, `TotStkInTradAmt`, `CashBalAmt`) as **mandatory non-null integers** (`>= 0`). If the user leaves them blank in the UI, the compiler MUST serialize `0` for each field rather than `null`, or the portal will reject the JSON with a Schema Validation Failure.

---

### 18.4 `ITR-2` Complex Schedules (`ScheduleCG`, `Schedule112A`, `ScheduleVDA`, `ScheduleFA`, `ScheduleFSI`, `ScheduleTR1`, `ScheduleAL`, `ScheduleSPI`, `ScheduleHP`)

```json
{
  "Schedule112A": {
    "Schedule112ADtls": [
      {
        "ShareOnOrBefore": "BE",
        "ISINCode": "INE002A01018",
        "ShareUnitName": "RELIANCE INDUSTRIES LTD",
        "NumSharesUnits": 100,
        "SalePricePerShareUnit": 2950,
        "TotSaleVal": 295000,
        "CostAcqWithoutIndx": 145000,
        "AcquisitionCost": 95000,
        "LTCGBeforeLowerB1B2": 145000,
        "FairMktValPerShareunit": 1450,
        "TotFairMktVal": 145000,
        "ExpExclCnctTransfer": 250,
        "TotalDeductions": 145250,
        "Balance": 149750
      }
    ],
    "SaleValue112A": 295000,
    "CostAcqWithoutIndx112A": 145000,
    "AcquisitionCost112A": 95000,
    "LTCGBeforeLowerB1B2112A": 145000,
    "FairMktVal112A": 145000,
    "ExpExclCnctTransfer112A": 250,
    "TotalDeductions112A": 145250,
    "Balance112A": 149750
  },
  "ScheduleVDA": {
    "ScheduleVDADtls": [
      {
        "DateOfAcq": "2025-05-10",
        "DateOfTransfer": "2025-11-18",
        "HeadOfInc": "CG",
        "CostOfAcq": 100000,
        "ConsidReceived": 160000,
        "IncChrgblVDATransfer": 60000
      }
    ],
    "TotIncCapGain": 60000,
    "TotIncBusProf": 0
  },
  "ScheduleFA": {
    "DetailsForeignEquityDebtInterest": [
      {
        "CountryName": "UNITED STATES OF AMERICA",
        "CountryCode": "2",
        "NameOfEntity": "ALPHABET INC",
        "AddressOfEntity": "1600 AMPHITHEATRE PKWY MOUNTAIN VIEW",
        "ZipCode": "94043",
        "NatureOfEntity": "COMPANY",
        "DateOfAcquiring": "2023-04-15",
        "InitialValueOfInvestment": 415000,
        "PeakValueDuringPeriod": 580000,
        "ClosingValue": 560000,
        "TotalGrossAmountPaidCredited": 4200,
        "TotalGrossProceedsFromSale": 0
      }
    ]
  },
  "ScheduleFSI": {
    "ScheduleFSIDtls": [
      {
        "CountryName": "UNITED STATES OF AMERICA",
        "CountryCode": "2",
        "TaxIdentificationNo": "999-00-1234",
        "IncFromSal": 0,
        "IncFromHP": 0,
        "IncFromBus": 0,
        "IncCapGain": 0,
        "IncOthSrc": 4200,
        "TotalCountryWise": 4200,
        "TaxPaidOutsideIndia": 1050,
        "TaxPayableInIndia": 1260,
        "TaxReliefAvailable": 1050,
        "DTAAArticle": "ARTICLE 10(2)"
      }
    ]
  },
  "ScheduleAL": {
    "ImmovableDetails": [
      {
        "Description": "RESIDENTIAL FLAT",
        "Address": {
          "ResidenceNo": "402",
          "RoadOrStreet": "SG HIGHWAY",
          "CityOrTownOrDistrict": "AHMEDABAD",
          "StateCode": "11",
          "CountryCode": "91",
          "PinCode": 380054
        },
        "Amount": 8500000
      }
    ],
    "MovableAsset": {
      "JewelleryBullion": 450000,
      "ArchCollDrawPaintSulpArt": 0,
      "VehiclYachtsBoatsAircrafts": 1200000,
      "FinancialAsset": {
        "Bank": 620000,
        "SharesAndSecurities": 2400000,
        "InsurancePolicies": 300000,
        "LoansAndAdvancesGiven": 0,
        "CashInHand": 35000
      }
    },
    "LiabilityInRelatAssets": 1850000
  }
}
```

---

### 18.5 `Form 67` Foreign Tax Credit & `ITR-U (Part B-ATI)` JSON Schemas

#### `Form 67` JSON Payload (Must Be Submitted BEFORE `ITR-2` to Claim `Sec 90/91` Relief)
```json
{
  "Form67": {
    "FormName": "FORM67",
    "AssessmentYear": "2026",
    "PersonalInfo": {
      "PAN": "ABCDE1234F",
      "AssesseeName": "SATISH JADAV"
    },
    "ForeignTaxCreditDtls": [
      {
        "CountryCode": "2",
        "CountryName": "UNITED STATES OF AMERICA",
        "SourceOfIncome": "DIVIDEND_1042S",
        "IncomeOutsideIndiaInINR": 4200,
        "TaxPaidOutsideIndiaForeignCurr": 12.60,
        "ExchangeRateSbiTtbr": 83.33,
        "TaxPaidOutsideIndiaInINR": 1050,
        "RateOfTaxOutsideIndiaPct": 25.00,
        "TaxPayableUnderNormalProvisionsINR": 1260,
        "RateOfIndianTaxPct": 30.00,
        "ArtOfDTAA": "ARTICLE 10(2)",
        "CreditClaimedUs90_90A": 1050,
        "CreditClaimedUs91": 0
      }
    ],
    "TotalCreditClaimed": 1050
  }
}
```

#### `ITR-U` (`Part B-ATI`) Additional Tax Liability Schema (`Sec 139(8A)` & `Sec 140B`)
```json
{
  "PartB_ATI": {
    "UpdatedReturnFilingWindow": "WITHIN_12_TO_24_MONTHS",
    "AdditionalTaxPct": 50,
    "PreviouslyDeclaredIncome": 900000,
    "UpdatedTotalIncome": 1350000,
    "TaxPayableOnUpdatedIncome": 225000,
    "PreviousTaxAndRefundAdjustment": 92500,
    "DifferentialTaxPayable": 132500,
    "DifferentialInterest234A_B_C": 19800,
    "LateFee234FDiff": 5000,
    "AggregateTaxAndInterestPayable": 157300,
    "AdditionalIncomeTaxUs140B": 76150,
    "TotalUpdatedTaxPayable": 233450,
    "Challan280DtlsUs140B": [
      {
        "BSRCode": "0240123",
        "DateDep": "2026-07-14",
        "SrlNoOfChaln": 45120,
        "Amt": 233450
      }
    ]
  }
}
```

---

### 18.6 Complete 25-Rule Category 'A' Defective Return (`Sec 139(9)`) Firewall Matrix
Every compiled JSON payload MUST pass all 25 deterministic rules in `CbdtItrJsonCompiler.runPreFlightCategoryAAudit()` before export is unlocked:

| Rule ID | `CategoryADefectCode` | Statutory Condition Checked | Exact Blocking Error Message Shown to User |
| :---: | :--- | :--- | :--- |
| **R-01** | `DEFECT_01_INVALID_PAN_REGEX` | `PAN` matches `^[A-Z]{3}[PCHFATBLJG][A-Z][0-9]{4}[A-Z]$` and 4th char is `'P'` for Individual or `'H'` for HUF | `"Invalid PAN format. Individual PAN must have 'P' as the 4th character."` |
| **R-02** | `DEFECT_02_UNVALIDATED_REFUND_BANK` | At least 1 bank account in `ScheduleBA` has `UseForRefund == true && IsValidated == true && IsEvcEnabled == true` | `"No EVC-enabled & pre-validated refund bank account found. Please enable EVC on your bank account via the portal bridge."` |
| **R-03** | `DEFECT_03_INVALID_IFSC_REGEX` | Every bank account IFSC matches `^[A-Z]{4}0[A-Z0-9]{6}$` | `"Bank IFSC code must be 11 characters with '0' as the 5th character."` |
| **R-04** | `DEFECT_04_80G_MISSING_DONEE_PAN_OR_ARN` | Every `Schedule80G` entry has valid Donee PAN (`!= taxpayer PAN`), valid `Form 10BE` ARN, and `cashAmount <= 2000` | `"Section 80G claim requires a valid Donee PAN, Form 10BE ARN, and non-cash mode if donation exceeds ₹2,000."` |
| **R-05** | `DEFECT_05_HRA_MISSING_LANDLORD_PAN` | If `exemptHra10_13A > 0` and annual rent paid `> ₹1,00,000`, `landlordPan` matches valid PAN regex and `!= taxpayer PAN` | `"Landlord PAN is mandatory when annual rent exceeds ₹1,00,000 and cannot match your own PAN."` |
| **R-06** | `DEFECT_06_TDS_exceeds_GROSS_RECEIPT` | In `ScheduleTDS1` / `ScheduleTDS2`, `tdsClaimedThisYear <= grossIncomeOfferedThisYear` for each TAN | `"TDS claimed for a deductor cannot exceed the corresponding gross income offered to tax."` |
| **R-07** | `DEFECT_07_INVALID_TAN_REGEX` | Every deductor TAN in `ScheduleTDS1`/`TDS2` matches `^[A-Z]{4}[0-9]{5}[A-Z]$` | `"Invalid Employer/Deductor TAN format (expected 4 letters, 5 digits, 1 letter)."` |
| **R-08** | `DEFECT_08_CHALLAN_BSR_OR_SERIAL_INVALID` | Every `ScheduleIT` entry has `bsrCode` matching `^\d{7}$`, `serialNo` matching `^\d{5}$`, and `depositDate <= today` | `"Challan 280 requires a 7-digit BSR code, 5-digit serial number, and a non-future deposit date."` |
| **R-09** | `DEFECT_09_UNPAID_TAX_WITHOUT_CHALLAN` | `TotTaxPlusIntrstPay - TotalTaxesPaid <= 0` (unless user explicitly acknowledges "Pay Later" defective risk) | `"Net tax payable exceeds total taxes paid. Pay via Challan 280 UPI before exporting to avoid a Section 139(9) defective notice."` |
| **R-10** | `DEFECT_10_NEW_REGIME_ILLEGAL_CHAPTER_VIA` | When `NewTaxRegime == "Y"`, all Chapter VI-A fields except `Section80CCDEmployer` (`80CCD(2)`), `80CCH`, and `80JJAA` are `== 0` | `"New Tax Regime u/s 115BAC disallows Chapter VI-A deductions except Employer NPS u/s 80CCD(2)."` |
| **R-11** | `DEFECT_11_NEW_REGIME_ILLEGAL_HP_SELF_OCCUPIED_LOSS` | When `NewTaxRegime == "Y"`, self-occupied house property `TotalIncomeOfHP >= 0` | `"Self-occupied home loan interest loss u/s 24(b) is not allowed under the New Tax Regime."` |
| **R-12** | `DEFECT_12_HP_LOSS_SET_OFF_EXCEEDS_2L_CAP` | Under Old Regime, inter-head House Property loss set-off against Salary/Other Heads `<= ₹2,00,000` (`Sec 71(3A)`) | `"House Property loss set-off against other heads cannot exceed ₹2,00,000 u/s 71(3A)."` |
| **R-13** | `DEFECT_13_80TTA_AND_80TTB_BOTH_CLAIMED` | `!(Section80TTA > 0 && Section80TTB > 0)` and `Section80TTB > 0` only if Resident Senior Citizen (`age >= 60`) | `"Section 80TTA and 80TTB are mutually exclusive; 80TTB is restricted to Resident Senior Citizens (age 60+)."` |
| **R-14** | `DEFECT_14_80GG_CLAIMED_WITH_HRA_OR_OWNED_HOUSE` | If `Section80GG > 0`, `exemptHra10_13A == 0` and `Form 10BA` acknowledgement number is populated | `"Section 80GG cannot be claimed alongside HRA and requires a Form 10BA declaration."` |
| **R-15** | `DEFECT_15_112A_MISSING_ISIN_FOR_GRANDFATHERED_LOT` | In `Schedule112A`, every row with `ShareOnOrBefore == "BE"` (acquired on/before Jan 31, 2018) has a valid 12-char `ISINCode` (`^IN[A-Z0-9]{10}$`) and `FairMktValPerShareunit > 0` | `"Grandfathered Equity LTCG lots acquired on or before Jan 31, 2018 require a 12-character ISIN and Jan 31, 2018 FMV."` |
| **R-16** | `DEFECT_16_VDA_NEGATIVE_NET_CONSIDERATION` | In `ScheduleVDA`, every row has `IncChrgblVDATransfer == max(0, ConsidReceived - CostOfAcq)` | `"Section 115BBH disallows negative income or loss set-off on Virtual Digital Assets."` |
| **R-17** | `DEFECT_17_44ADA_GROSS_RECEIPTS_EXCEED_75L` | In `ITR-4`, `PersumptiveInc44ADA.GrsReceipt <= 7500000` (and if `> 5000000`, cash receipts `<= 5%`) and `TotPersumptiveInc44ADA >= 0.50 * GrsReceipt` | `"Section 44ADA requires gross receipts <= ₹75L (with <=5% cash) and declared profit >= 50%."` |
| **R-18** | `DEFECT_18_44AD_TURNOVER_OR_PROFIT_FLOOR_VIOLATION` | In `ITR-4`, `TotTurnover <= 30000000` and `TotPersumptiveInc44AD >= (0.06 * GrsTrnOverBank + 0.08 * GrsTrnOverAnyOthMode)` | `"Section 44AD requires declared profit >= 6% of digital turnover plus >= 8% of cash turnover."` |
| **R-19** | `DEFECT_19_MISSING_FORM_10IEA_FOR_ITR4_OLD_REGIME` | In `ITR-4`, if `OptOutNewTaxRegime115BAC == "Y"`, `Form10IEADetails.AcknowledgementNo` (`^\d{15}$`) and `DateOfFiling` are non-null | `"Opting for the Old Tax Regime with business/professional income requires a 15-digit Form 10-IEA Acknowledgement Number."` |
| **R-20** | `DEFECT_20_MISSING_SCHEDULE_AL_ABOVE_50L` | In `ITR-2`, if `TotalIncome > 5000000`, `ScheduleAL` is populated and non-null | `"Schedule AL (Assets & Liabilities) is mandatory when Total Income exceeds ₹50,00,000."` |
| **R-21** | `DEFECT_21_ROR_FOREIGN_ASSET_MISSING_SCHEDULE_FA` | If `ResStatus == "ROR"` and user holds foreign stocks/accounts or `ScheduleFSI` is non-empty, `ScheduleFA` is populated | `"Resident & Ordinarily Resident (ROR) taxpayers with foreign income or assets must populate Schedule FA."` |
| **R-22** | `DEFECT_22_FSI_RELIEF_MISSING_FORM_67` | If `ScheduleTR1.TaxReliefClaimed > 0`, `Form67` acknowledgement metadata is present | `"Foreign Tax Credit u/s 90/91 requires Form 67 to be filed on or before the return filing date."` |
| **R-23** | `DEFECT_23_REVISED_RETURN_MISSING_ORIGINAL_ACK` | If `ReturnFileSec == 17` (`139(5)`), `OrigRetFiledDate` and 15-digit `ReceiptNo` are populated | `"Revised return u/s 139(5) requires the 15-digit Original Acknowledgement Number and Original Filing Date."` |
| **R-24** | `DEFECT_24_ARREARS_89_1_MISSING_FORM_10E` | If `Section89 > 0`, `Form10EDetails` acknowledgement number is populated | `"Section 89(1) salary arrears relief requires Form 10E to be submitted before filing the return."` |
| **R-25** | `DEFECT_25_LET_OUT_HP_MISSING_TENANT_OR_MUNICIPAL_TAX_PROOF` | In `ScheduleHP`, if `TypeOfHP == "L"` and `TaxPaidlocalAuth > GrossRentReceived`, block compilation | `"Municipal taxes deducted u/s 23 cannot exceed gross rent received or were not paid during the financial year."` |

---

## 19. Complete 13-Table SQLCipher 4.5.4 Production DDL, Indexes, Constraints & Offline Seed SQL

To ensure a developer can initialize the encrypted Room + SQLCipher `4.5.4` database (`PRAGMA cipher_page_size = 4096; PRAGMA kdf_iter = 256000; PRAGMA cipher_hmac_algorithm = HMAC_SHA512; PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA512; PRAGMA foreign_keys = ON;`) without guessing a single column name, type, or constraint, here is the complete 13-table production DDL and seed script.

### 19.1 Production SQLite / SQLCipher DDL (`schema_v2_ay2026_27.sql`)

```sql
-- Enable strict foreign key enforcement for cascading DPDP vault shredding
PRAGMA foreign_keys = ON;

-- 1. Master Taxpayer & Family Member Profile (Supports up to 5 local PAN vaults)
CREATE TABLE IF NOT EXISTS taxpayer_profile (
    pan TEXT PRIMARY KEY NOT NULL CHECK (pan GLOB '[A-Z][A-Z][A-Z][PCHFATBLJG][A-Z][0-9][0-9][0-9][0-9][A-Z]'),
    assessment_year TEXT NOT NULL DEFAULT '2026-27',
    full_name TEXT NOT NULL,
    date_of_birth_iso TEXT NOT NULL, -- YYYY-MM-DD
    taxpayer_category TEXT NOT NULL CHECK (taxpayer_category IN ('INDIVIDUAL', 'HUF')),
    relationship_tag TEXT NOT NULL DEFAULT 'SELF' CHECK (relationship_tag IN ('SELF', 'SPOUSE', 'FATHER', 'MOTHER', 'HUF')),
    residential_status TEXT NOT NULL DEFAULT 'RESIDENT_ORDINARILY_RESIDENT'
        CHECK (residential_status IN ('RESIDENT_ORDINARILY_RESIDENT', 'RESIDENT_BUT_NOT_ORDINARILY_RESIDENT', 'NON_RESIDENT')),
    employer_category TEXT NOT NULL DEFAULT 'OTH' CHECK (employer_category IN ('CGOV', 'SGOV', 'PSU', 'PE', 'OTH', 'NA')),
    selected_regime TEXT NOT NULL DEFAULT 'AUTO_OPTIMAL' CHECK (selected_regime IN ('AUTO_OPTIMAL', 'NEW_115BAC', 'OLD_REGIME')),
    has_business_income INTEGER NOT NULL DEFAULT 0 CHECK (has_business_income IN (0, 1)),
    form_10iea_ack_no TEXT,
    form_10iea_filing_date_iso TEXT,
    refund_bank_ifsc TEXT,
    refund_bank_account_masked TEXT,
    is_refund_bank_evc_validated INTEGER NOT NULL DEFAULT 0 CHECK (is_refund_bank_evc_validated IN (0, 1)),
    created_at_epoch_ms INTEGER NOT NULL,
    updated_at_epoch_ms INTEGER NOT NULL
);

-- 2. Level-3 Cryptographic Provenance & Audit Lineage Node
CREATE TABLE IF NOT EXISTS provenance_audit_node (
    provenance_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL,
    source_type TEXT NOT NULL CHECK (source_type IN ('FORM_16_PDF', 'AIS_JSON', 'FORM_26AS_JSON', 'REBIT_AA_XML', 'MANUAL_RECEIPT_OCR', 'USER_MANUAL_OVERRIDE')),
    source_document_name TEXT NOT NULL,
    source_sha256_hex TEXT NOT NULL,
    pdf_page_number INTEGER,
    bbox_left_pt REAL,
    bbox_top_pt REAL,
    bbox_right_pt REAL,
    bbox_bottom_pt REAL,
    raw_extracted_snippet TEXT NOT NULL,
    statutory_section_ref TEXT NOT NULL,
    extraction_confidence_bps INTEGER NOT NULL DEFAULT 10000 CHECK (extraction_confidence_bps BETWEEN 0 AND 10000),
    created_at_epoch_ms INTEGER NOT NULL,
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_provenance_pan ON provenance_audit_node(pan);

-- 3. Income Head Ledger (Salary, House Property, 44ADA, 44AD, Other Sources)
CREATE TABLE IF NOT EXISTS income_head_ledger (
    entry_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL,
    assessment_year TEXT NOT NULL DEFAULT '2026-27',
    income_head TEXT NOT NULL CHECK (income_head IN ('SALARY', 'HOUSE_PROPERTY', 'BUSINESS_44ADA', 'BUSINESS_44AD', 'OTHER_SOURCES', 'EXEMPT_INCOME')),
    sub_category_code TEXT NOT NULL, -- e.g., 'BASIC_DA', 'HRA_EXEMPT_10_13A', 'HP_SELF_OCCUPIED_24B', 'SAVINGS_INT', 'FD_INT', 'DIVIDEND'
    gross_amount_rupees INTEGER NOT NULL CHECK (gross_amount_rupees >= 0),
    exempt_or_deduction_rupees INTEGER NOT NULL DEFAULT 0 CHECK (exempt_or_deduction_rupees >= 0),
    net_taxable_rupees INTEGER NOT NULL,
    employer_tan_or_counterparty_pan TEXT,
    provenance_id TEXT NOT NULL,
    is_reconciled_with_ais INTEGER NOT NULL DEFAULT 1 CHECK (is_reconciled_with_ais IN (0, 1)),
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    FOREIGN KEY (provenance_id) REFERENCES provenance_audit_node(provenance_id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_income_pan_head ON income_head_ledger(pan, income_head);

-- 4. Chapter VI-A & Section 10 Deduction Claim Ledger
CREATE TABLE IF NOT EXISTS deduction_claim_ledger (
    claim_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL,
    assessment_year TEXT NOT NULL DEFAULT '2026-27',
    section_code TEXT NOT NULL CHECK (section_code IN (
        '10_13A_HRA', '10_5_LTA', '10_14_ALLOWANCES', '80C', '80CCC', '80CCD_1', '80CCD_1B', '80CCD_2',
        '80D_SELF', '80D_PARENTS', '80DD', '80DDB', '80E', '80EE', '80EEA', '80EEB',
        '80G_100_NO_LIMIT', '80G_50_NO_LIMIT', '80G_100_QUALIFYING', '80G_50_QUALIFYING',
        '80GG', '80GGA', '80GGC', '80TTA', '80TTB', '80U', '80CCH'
    )),
    raw_invested_or_paid_rupees INTEGER NOT NULL CHECK (raw_invested_or_paid_rupees >= 0),
    statutorily_allowed_old_regime_rupees INTEGER NOT NULL CHECK (statutorily_allowed_old_regime_rupees >= 0),
    statutorily_allowed_new_regime_rupees INTEGER NOT NULL CHECK (statutorily_allowed_new_regime_rupees >= 0),
    counterparty_pan TEXT, -- Landlord PAN for HRA or Donee PAN for 80G
    form_10be_arn_or_10ba_ack TEXT, -- Mandatory for 80G / 80GG
    payment_mode TEXT NOT NULL DEFAULT 'DIGITAL' CHECK (payment_mode IN ('DIGITAL', 'CASH')),
    provenance_id TEXT NOT NULL,
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    FOREIGN KEY (provenance_id) REFERENCES provenance_audit_node(provenance_id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_deduction_pan_sec ON deduction_claim_ledger(pan, section_code);

-- 5. Form 26AS Part A/B/C TDS & TCS Credit Ledger (With Section 205 Shield Support)
CREATE TABLE IF NOT EXISTS tds_26as_credit_entry (
    tds_entry_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL,
    assessment_year TEXT NOT NULL DEFAULT '2026-27',
    schedule_type TEXT NOT NULL CHECK (schedule_type IN ('TDS1_SALARY', 'TDS2_OTHER_THAN_SALARY', 'TDS3_26QC_26QB', 'TCS')),
    deductor_tan_or_pan TEXT NOT NULL,
    deductor_name TEXT NOT NULL,
    section_code TEXT NOT NULL, -- '192', '194A', '194J', '194', '194S', '194IA', '194IB', '206C1G'
    gross_amount_credited_26as_rupees INTEGER NOT NULL CHECK (gross_amount_credited_26as_rupees >= 0),
    tds_deposited_26as_rupees INTEGER NOT NULL CHECK (tds_deposited_26as_rupees >= 0),
    tds_deducted_payslip_or_form16_rupees INTEGER NOT NULL DEFAULT 0 CHECK (tds_deducted_payslip_or_form16_rupees >= 0),
    sec_205_unpaid_employer_tds_gap_rupees INTEGER NOT NULL DEFAULT 0 CHECK (sec_205_unpaid_employer_tds_gap_rupees >= 0),
    provenance_id TEXT NOT NULL,
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    FOREIGN KEY (provenance_id) REFERENCES provenance_audit_node(provenance_id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_tds_pan_tan ON tds_26as_credit_entry(pan, deductor_tan_or_pan);

-- 6. AIS / TIS Line-Item Reconciliation & Discrepancy Dispute Ledger
CREATE TABLE IF NOT EXISTS ais_tis_item_ledger (
    ais_item_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL,
    info_category_code TEXT NOT NULL, -- 'TDS-192', 'SFT-005', 'SFT-016', 'SFT-017', 'VDA-194S', 'TCS-206C1G'
    information_source_name TEXT NOT NULL,
    information_source_pan_or_tan TEXT,
    ais_reported_amount_rupees INTEGER NOT NULL,
    tis_derived_amount_rupees INTEGER NOT NULL,
    local_reconciled_amount_rupees INTEGER NOT NULL,
    reconciliation_status TEXT NOT NULL DEFAULT 'MATCHED'
        CHECK (reconciliation_status IN ('MATCHED', 'DUPLICATE_ELIMINATED', 'DISCREPANCY_DETECTED', 'FEEDBACK_SUBMITTED')),
    dispute_feedback_code TEXT CHECK (dispute_feedback_code IN ('A_ACCEPTED', 'B1_PARTIALLY_CORRECT', 'B2_ANOTHER_YEAR', 'B3_DUPLICATE_OR_JOINT', 'C_DENIED')),
    provenance_id TEXT NOT NULL,
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    FOREIGN KEY (provenance_id) REFERENCES provenance_audit_node(provenance_id) ON DELETE RESTRICT
);

-- 7. Schedule CG & Schedule 112A / VDA Capital Gains Lot Ledger
CREATE TABLE IF NOT EXISTS schedule_cg_lot (
    lot_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL,
    asset_category TEXT NOT NULL CHECK (asset_category IN (
        'LISTED_EQUITY_111A_STCG', 'LISTED_EQUITY_112A_LTCG', 'REAL_ESTATE_LAND_BUILDING',
        'SPECIFIED_DEBT_MF_50AA', 'UNLISTED_SHARES_OR_GOLD', 'VDA_CRYPTO_115BBH'
    )),
    isin_code TEXT, -- Mandatory for 112A lots acquired on/before 2018-01-31
    asset_name TEXT NOT NULL,
    acquisition_date_iso TEXT NOT NULL,
    transfer_date_iso TEXT NOT NULL,
    quantity REAL NOT NULL CHECK (quantity > 0),
    full_value_consideration_rupees INTEGER NOT NULL CHECK (full_value_consideration_rupees >= 0),
    actual_cost_of_acquisition_rupees INTEGER NOT NULL CHECK (actual_cost_of_acquisition_rupees >= 0),
    jan_31_2018_fmv_total_rupees INTEGER NOT NULL DEFAULT 0 CHECK (jan_31_2018_fmv_total_rupees >= 0),
    effective_cost_of_acquisition_rupees INTEGER NOT NULL CHECK (effective_cost_of_acquisition_rupees >= 0),
    indexed_cost_of_acquisition_rupees INTEGER NOT NULL DEFAULT 0 CHECK (indexed_cost_of_acquisition_rupees >= 0),
    transfer_expenses_rupees INTEGER NOT NULL DEFAULT 0 CHECK (transfer_expenses_rupees >= 0),
    rollover_exemption_section TEXT CHECK (rollover_exemption_section IN ('NONE', 'SEC_54', 'SEC_54EC', 'SEC_54F', 'SEC_54B')),
    rollover_exemption_rupees INTEGER NOT NULL DEFAULT 0 CHECK (rollover_exemption_rupees >= 0),
    net_gain_or_loss_rupees INTEGER NOT NULL,
    q1_upto_jun15_gain_rupees INTEGER NOT NULL DEFAULT 0,
    q2_jun16_to_sep15_gain_rupees INTEGER NOT NULL DEFAULT 0,
    q3_sep16_to_dec15_gain_rupees INTEGER NOT NULL DEFAULT 0,
    q4_dec16_to_mar15_gain_rupees INTEGER NOT NULL DEFAULT 0,
    q5_mar16_to_mar31_gain_rupees INTEGER NOT NULL DEFAULT 0,
    provenance_id TEXT NOT NULL,
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    FOREIGN KEY (provenance_id) REFERENCES provenance_audit_node(provenance_id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_cg_pan_cat ON schedule_cg_lot(pan, asset_category);

-- 8. Schedule FA & FSI Foreign Asset & DTAA Relief Ledger
CREATE TABLE IF NOT EXISTS schedule_fa_foreign_asset (
    fa_entry_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL,
    country_code_cbdt TEXT NOT NULL DEFAULT '2', -- '2' = USA
    country_name TEXT NOT NULL DEFAULT 'UNITED STATES OF AMERICA',
    entity_name TEXT NOT NULL,
    entity_address TEXT NOT NULL,
    zip_code TEXT NOT NULL,
    acquisition_date_iso TEXT NOT NULL,
    initial_value_inr INTEGER NOT NULL CHECK (initial_value_inr >= 0),
    peak_value_calendar_year_inr INTEGER NOT NULL CHECK (peak_value_calendar_year_inr >= 0),
    closing_value_dec31_inr INTEGER NOT NULL CHECK (closing_value_dec31_inr >= 0),
    gross_dividend_or_interest_inr INTEGER NOT NULL DEFAULT 0 CHECK (gross_dividend_or_interest_inr >= 0),
    foreign_tax_withheld_inr INTEGER NOT NULL DEFAULT 0 CHECK (foreign_tax_withheld_inr >= 0),
    dtaa_article_ref TEXT NOT NULL DEFAULT 'ARTICLE 10(2)',
    sbi_tt_buying_rate_used REAL NOT NULL CHECK (sbi_tt_buying_rate_used > 0),
    provenance_id TEXT NOT NULL,
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    FOREIGN KEY (provenance_id) REFERENCES provenance_audit_node(provenance_id) ON DELETE RESTRICT
);

-- 9. Schedule AL Assets & Liabilities Net-Worth Ledger (Mandatory if Total Income > ₹50L)
CREATE TABLE IF NOT EXISTS schedule_al_net_worth (
    pan TEXT PRIMARY KEY NOT NULL,
    immovable_property_cost_rupees INTEGER NOT NULL DEFAULT 0 CHECK (immovable_property_cost_rupees >= 0),
    jewellery_bullion_cost_rupees INTEGER NOT NULL DEFAULT 0 CHECK (jewellery_bullion_cost_rupees >= 0),
    vehicles_cost_rupees INTEGER NOT NULL DEFAULT 0 CHECK (vehicles_cost_rupees >= 0),
    bank_deposits_mar31_rupees INTEGER NOT NULL DEFAULT 0 CHECK (bank_deposits_mar31_rupees >= 0),
    shares_and_securities_cost_rupees INTEGER NOT NULL DEFAULT 0 CHECK (shares_and_securities_cost_rupees >= 0),
    insurance_policies_paid_rupees INTEGER NOT NULL DEFAULT 0 CHECK (insurance_policies_paid_rupees >= 0),
    cash_in_hand_mar31_rupees INTEGER NOT NULL DEFAULT 0 CHECK (cash_in_hand_mar31_rupees >= 0),
    related_liabilities_loans_rupees INTEGER NOT NULL DEFAULT 0 CHECK (related_liabilities_loans_rupees >= 0),
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE
);

-- 10. Schedule IT Challan 280 Advance Tax & Self-Assessment Tax Payments
CREATE TABLE IF NOT EXISTS challan_280_payment (
    challan_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL,
    bsr_code TEXT NOT NULL CHECK (bsr_code GLOB '[0-9][0-9][0-9][0-9][0-9][0-9][0-9]'),
    deposit_date_iso TEXT NOT NULL,
    challan_serial_no TEXT NOT NULL CHECK (challan_serial_no GLOB '[0-9][0-9][0-9][0-9][0-9]'),
    major_head_code TEXT NOT NULL DEFAULT '0021' CHECK (major_head_code = '0021'),
    minor_head_code TEXT NOT NULL DEFAULT '300' CHECK (minor_head_code IN ('100', '300', '400')),
    amount_paid_rupees INTEGER NOT NULL CHECK (amount_paid_rupees > 0),
    upi_utr_reference TEXT,
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE
);

-- 11. ReBIT Sahamati Account Aggregator Consent Artifact Registry
CREATE TABLE IF NOT EXISTS aa_consent_artifact (
    consent_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL,
    aa_handle TEXT NOT NULL,
    purpose_code TEXT NOT NULL DEFAULT '101' CHECK (purpose_code = '101'),
    fetch_type TEXT NOT NULL DEFAULT 'ONETIME' CHECK (fetch_type = 'ONETIME'),
    data_life_value INTEGER NOT NULL DEFAULT 0 CHECK (data_life_value = 0),
    consent_status TEXT NOT NULL CHECK (consent_status IN ('ACTIVE', 'USED_AND_WIPED', 'REVOKED', 'EXPIRED')),
    granted_at_epoch_ms INTEGER NOT NULL,
    wiped_or_revoked_at_epoch_ms INTEGER,
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE
);

-- 12. Section 6 Residential Status & RNOR 10-Year / 7-Year Travel History Ledger
CREATE TABLE IF NOT EXISTS rnor_travel_history (
    pan TEXT NOT NULL,
    financial_year TEXT NOT NULL, -- e.g., '2025-26', '2024-25' .. '2015-16'
    days_in_india INTEGER NOT NULL CHECK (days_in_india BETWEEN 0 AND 366),
    PRIMARY KEY (pan, financial_year),
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE
);

-- 13. Section 64 Clubbing of Income & Section 56(2)(x) Gift Exemption Ledger
CREATE TABLE IF NOT EXISTS clubbing_64_ledger (
    clubbing_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL,
    transferor_or_minor_name TEXT NOT NULL,
    transferor_or_minor_pan TEXT,
    relationship_to_assessee TEXT NOT NULL CHECK (relationship_to_assessee IN ('SPOUSE_64_1_IV', 'MINOR_CHILD_64_1A', 'SONS_WIFE_64_1_VI', 'RELATIVE_GIFT_EXEMPT_56_2_X')),
    gross_income_or_gift_rupees INTEGER NOT NULL CHECK (gross_income_or_gift_rupees >= 0),
    sec_10_32_minor_exemption_rupees INTEGER NOT NULL DEFAULT 0 CHECK (sec_10_32_minor_exemption_rupees <= 1500),
    net_clubbed_taxable_rupees INTEGER NOT NULL CHECK (net_clubbed_taxable_rupees >= 0),
    FOREIGN KEY (pan) REFERENCES taxpayer_profile(pan) ON DELETE CASCADE
);
```

### 19.2 Static Reference & Seed Tables (`cii_seed.sql`, `sbi_ttbr_seed.sql`)

```sql
CREATE TABLE IF NOT EXISTS statutory_cii_index (
    financial_year TEXT PRIMARY KEY NOT NULL,
    cii_value INTEGER NOT NULL CHECK (cii_value >= 100)
);

INSERT OR REPLACE INTO statutory_cii_index (financial_year, cii_value) VALUES
('2001-02', 100), ('2002-03', 105), ('2003-04', 109), ('2004-05', 113), ('2005-06', 117),
('2006-07', 122), ('2007-08', 129), ('2008-09', 137), ('2009-10', 148), ('2010-11', 167),
('2011-12', 184), ('2012-13', 200), ('2013-14', 220), ('2014-15', 240), ('2015-16', 254),
('2016-17', 264), ('2017-18', 272), ('2018-19', 280), ('2019-20', 289), ('2020-21', 301),
('2021-22', 317), ('2022-23', 331), ('2023-24', 348), ('2024-25', 363), ('2025-26', 376);

CREATE TABLE IF NOT EXISTS grandfathered_112a_fmv_20180131 (
    isin_code TEXT PRIMARY KEY NOT NULL CHECK (isin_code GLOB 'IN[A-Z0-9][A-Z0-9][A-Z0-9][A-Z0-9][A-Z0-9][A-Z0-9][A-Z0-9][A-Z0-9][A-Z0-9][A-Z0-9]'),
    bse_nse_symbol TEXT NOT NULL,
    company_name TEXT NOT NULL,
    highest_price_jan31_2018_paise INTEGER NOT NULL CHECK (highest_price_jan31_2018_paise > 0)
);
```

---

## 20. Complete Verbatim On-Device Gemini Nano (`AICore`) Production Prompt Catalog & Sanity Firewalls

Every on-device AI call inside `:feature:ai-extractor` MUST use `com.google.ai.edge.aicore` with deterministic decoding (`temperature = 0.0f`, `topK = 1`) and is strictly restricted to **structuring messy text into JSON** or **translating pre-computed `{{TOKEN}}` strings**. Under no circumstances may Gemini Nano calculate tax slabs, rebates, or deductions.

### 20.1 `P1-PROMPT-01`: Form 16 Part A & Part B Deterministic OCR Structurer
```text
SYSTEM ROLE: You are an offline, deterministic Indian Form 16 TRACES PDF layout parser.
CRITICAL RULE: Do NOT perform any addition, subtraction, or tax calculation. Extract the exact rupee integers printed on the page. Strip commas and '.00' decimals. Return ONLY valid JSON matching the schema below.

INPUT OCR TEXT WITH BOUNDING BOXES:
{{SANITIZED_FORM16_OCR_TEXT}}

OUTPUT JSON SCHEMA:
{
  "employerTan": "string (10-char TAN regex ^[A-Z]{4}[0-9]{5}[A-Z]$)",
  "employerName": "string",
  "grossSalarySection17_1": 0,
  "perquisitesSection17_2": 0,
  "profitsInLieuSection17_3": 0,
  "totalGrossSalary": 0,
  "exemptHraSection10_13A": 0,
  "exemptLtaSection10_5": 0,
  "totalExemptAllowancesSection10": 0,
  "standardDeductionSection16ia": 0,
  "professionalTaxSection16iii": 0,
  "incomeChargeableUnderHeadSalaries": 0,
  "sec80CReported": 0,
  "sec80CCD1BReported": 0,
  "sec80CCD2EmployerNpsReported": 0,
  "sec80DReported": 0,
  "totalTaxDeductedPartA": 0
}
```
- **Post-AI Math Sanity Firewall ($\pm\text{₹5}$):**
  $$\left|\left(\text{totalGrossSalary} - \text{totalExemptAllowancesSection10} - \text{standardDeductionSection16ia} - \text{professionalTaxSection16iii}\right) - \text{incomeChargeableUnderHeadSalaries}\right| \le 5$$
  If the discrepancy exceeds $\text{₹5}$, `OnDeviceForm16AiExtractor` sets `requiresHumanDisambiguation = true` and passes the exact PDF page and bounding box coordinates to `OneTapPdfDisambiguationCard`.

---

### 20.2 `P1-PROMPT-02`: Rent Receipt, Section 80D Medical Bill & Section 80G Donation Receipt Structurer
```text
SYSTEM ROLE: You are an offline Indian tax receipt parser for Section 10(13A) Rent, Section 80D Medical Insurance/Senior Citizen Medical Bills, and Section 80G Donations.
CRITICAL RULE: Extract only literal fields present in the OCR text. Do not invent PANs or ARNs. Return ONLY valid JSON.

INPUT OCR TEXT:
{{SANITIZED_RECEIPT_OCR_TEXT}}

OUTPUT JSON SCHEMA:
{
  "receiptCategory": "RENT_10_13A | MEDICAL_80D | DONATION_80G",
  "issuerOrLandlordOrDoneeName": "string",
  "counterpartyPan": "string or null",
  "form10BeArn": "string or null",
  "paymentMode": "DIGITAL | CASH",
  "receiptDateIso": "YYYY-MM-DD",
  "amountRupees": 0,
  "isPreventiveHealthCheckup": false,
  "isForSeniorCitizenParent": false
}
```

---

### 20.3 `P2-PROMPT-01`: 25-Row Batch Bank Narration Classifier (Account Aggregator RAM Pipeline)
```text
SYSTEM ROLE: Classify each sanitized Indian bank statement narration row into exactly one statutory tax category.
CRITICAL RULE: Personal UPI debits under Rs. 500 have already been stripped. Do not modify rowId or amountRupees. Return a JSON array of objects.

ALLOWED CATEGORIES:
- "SECTION_80C_LIC_PPF_ELSS_TUITION"
- "SECTION_80D_HEALTH_INS"
- "SECTION_80D_PREVENTIVE_OR_SENIOR_MEDICAL"
- "SECTION_80E_EDUCATION_LOAN"
- "SECTION_80G_DONATION"
- "SECTION_44ADA_FREELANCE_INFLOW"
- "SECTION_44AD_BUSINESS_RECEIPT"
- "SAVINGS_INTEREST"
- "FD_INTEREST"
- "DIVIDEND_CREDIT"
- "PERSONAL_OR_NON_DEDUCTIBLE"

INPUT 25-ROW JSON BATCH:
{{SANITIZED_25_BANK_ROWS_JSON}}

OUTPUT JSON SCHEMA:
[
  { "rowId": "string", "category": "ALLOWED_CATEGORY", "confidenceBps": 9500 }
]
```

---

### 20.4 `P3-PROMPT-01`: Token-Locked 6-Locale Vernacular Notice & Deduction Explainer
```text
SYSTEM ROLE: Translate and explain the following Indian Income Tax summary into {{TARGET_LOCALE}} (supported: en-IN, hi-IN, gu-IN, mr-IN, ta-IN, te-IN) in warm, clear, everyday language.
ABSOLUTE SECURITY RULE: Every placeholder token enclosed in double curly braces (such as {{TAX_SAVED}}, {{SEC_80D_GAP}}, {{NOTICE_SECTION}}, {{RESPONSE_DEADLINE}}) MUST be copied byte-for-byte into your output without translating, removing, or altering the characters inside {{...}}.

INPUT TEMPLATE:
{{TOKEN_LOCKED_TEMPLATE}}
```
- **Deterministic Post-AI Token Verifier (`ECV-06`):** Before displaying any output from `TokenLockedVernacularExplainer`, the Kotlin caller runs:
  `val allTokensIntact = expectedTokens.all { token -> generatedText.contains(token) }`
  If `allTokensIntact == false`, the AI output is immediately discarded and replaced with the deterministic localized `strings.xml` template.

---

### 20.5 `P3-PROMPT-02`: Offline Natural-Language "What-If" Tax Simulator Intent Parser (`Feature F-15`)
```text
SYSTEM ROLE: Convert a user's natural-language Indian tax question (in English, Hindi, or Hinglish) into a structured JSON delta object for the deterministic Kotlin tax engine.
CRITICAL RULE: Never calculate the tax savings yourself. Only extract the numerical parameter changes requested by the user.

USER QUERY:
{{USER_WHAT_IF_QUERY}}

OUTPUT JSON SCHEMA:
{
  "additional80CInvestmentRupees": 0,
  "additional80CCD1BNpsRupees": 0,
  "additional80CCD2EmployerNpsRupees": 0,
  "additional80DSelfRupees": 0,
  "additional80DParentsRupees": 0,
  "parentsSeniorCitizen": false,
  "monthlyRentPaidRupees": 0,
  "isMetroCity": false,
  "additionalHomeLoanInterestSelfOccupiedRupees": 0
}
```

---

## 21. Complete 24-Vector Golden & Edge-Case Verification Matrix (`GTV-01..18` & `ECV-01..06`)

A production CI pipeline requires 100% pass rate on all **18 Golden Tax Vectors (`GTV-01..18`)** and **6 Edge-Case Vectors (`ECV-01..06`)**. Every developer pull request is blocked by `.github/workflows/zerotax-ci.yml` if any vector deviates by even `₹1`.

### 21.1 Master 18 Golden Tax Vectors (`GTV-01` through `GTV-18`)

| Vector ID | Scenario Description | Exact Input Parameters (`TaxCalculationInput`) | Expected Statutory Engine Output (Exact Rupees) |
| :---: | :--- | :--- | :--- |
| **`GTV-01`** | **New Regime `87A` Full Rebate Boundary (`₹12,75,000` Gross Salary)** | `grossSalary = 12,75,000`, `regime = NEW_115BAC` | `StdDed = 75,000`, `TaxableIncome = 12,00,000`, `SlabTax = 60,000`, `Rebate87A = 60,000`, `Cess = 0`, **`TotalTax = ₹0`** |
| **`GTV-02`** | **New Regime `87A` Marginal Relief Cliff (`₹12,85,000` Gross Salary)** | `grossSalary = 12,85,000`, `regime = NEW_115BAC` | `StdDed = 75,000`, `TaxableIncome = 12,10,000`, `RawSlabTax = 61,500`, `ExcessOver12L = 10,000`, `MarginalRelief = 51,500`, `TaxAfterRelief = 10,000`, `Cess (4%) = 400`, **`TotalTax = ₹10,400`** |
| **`GTV-03`** | **New Regime `87A` Marginal Relief Exhaustion (`₹13,35,000` Gross Salary)** | `grossSalary = 13,35,000`, `regime = NEW_115BAC` | `TaxableIncome = 12,60,000`, `RawSlabTax = 69,000`, `ExcessOver12L = 60,000`, `MarginalRelief = 9,000`, `TaxAfterRelief = 60,000`, `Cess = 2,400`, **`TotalTax = ₹62,400`** |
| **`GTV-04`** | **Old vs New Regime Crossover with Full Deductions (`₹18,00,000` Gross Salary)** | `grossSalary = 18,00,000`, `exemptHra = 2,40,000`, `sec24bSelfOccupied = 2,00,000`, `sec80C = 1,50,000`, `sec80CCD1B = 50,000`, `sec80D = 75,000` | **Old:** `TI = 10,35,000`, `SlabTax = 1,23,000`, `Cess = 4,920`, **`OldTotal = ₹1,27,920`**<br>**New:** `TI = 17,25,000`, `SlabTax = 1,42,500`, `Cess = 5,700`, **`NewTotal = ₹1,48,200`**<br>`Recommended = OLD_REGIME` (`Savings = ₹20,280`) |
| **`GTV-05`** | **Employer NPS `Sec 80CCD(2)` 14% New Regime vs 10% Old Regime (`₹25,00,000` Salary)** | `grossSalary = 25,00,000`, `basicPlusDa = 15,00,000`, `employerNps = 2,10,000`, `employerCat = OTH` | `New80CCD2Allowed = 2,10,000 (14%)`, `Old80CCD2Allowed = 1,50,000 (10%)`, `NewTI = 22,15,000`, `NewSlabTax = 2,64,500`, `Cess = 10,580`, **`NewTotal = ₹2,75,080`** |
| **`GTV-06`** | **`Sec 112A` Equity LTCG (`₹2,25,000`) + `Sec 111A` STCG (`₹1,00,000`) Excluded from `87A`** | `grossSalary = 8,75,000` (`NormalTI = 8,00,000`), `stcg111A = 1,00,000`, `ltcg112A = 2,25,000` (all post-July 23, 2024) | `NormalTax = 0 (covered by 87A)`, `Tax111A (20%) = 20,000`, `Taxable112A = 2,25,000 - 1,25,000 = 1,00,000`, `Tax112A (12.5%) = 12,500`, `SpecialTax = 32,500`, `Cess = 1,300`, **`TotalTax = ₹33,800`** |
| **`GTV-07`** | **Chapter VI-A + Basic Exemption Soak Against Normal Income Before `112A` (`Sec 112A(3)`)** | `normalIncome = 2,50,000`, `sec80C = 1,00,000` (Old Regime, age 35), `ltcg112A = 3,50,000` | `NormalAfter80C = 1,50,000`, `UnexhaustedBasicExemption = 2,50,000 - 1,50,000 = 1,00,000`, `Net112A = 3,50,000 - 1,00,000 (unexhausted) - 1,25,000 (exemption) = 1,25,000`, `Tax112A (12.5%) = 15,625`, `Cess = 625`, **`TotalTax = ₹16,250`** |
| **`GTV-08`** | **`Sec 115BBH` Virtual Digital Asset (Crypto) Loss Ring-Fencing + 30% Flat Tax** | `lot1Gain = +1,50,000`, `lot2Loss = -90,000`, `stakingIncome = 20,000`, `transferFee = 2,500` | `lot2LossIgnored = 0`, `transferFeeIgnored = 0`, `TaxableVDA = 1,70,000`, `Tax115BBH (30%) = 51,000`, `Cess = 2,040`, **`TotalTax = ₹53,040`** |
| **`GTV-09`** | **Surcharge 15% Cap on `111A`/`112A` & Marginal Relief at `₹50,50,000` Total Income** | `normalTI = 50,50,000` (`New Regime`) | `BaseSlabTax = 11,15,000`, `RawSurcharge (10%) = 1,11,500`, `TaxAt50L = 11,00,000`, `MaxPayable = 11,00,000 + 50,000 = 11,50,000`, `SurchargeMarginalRelief = 76,500`, `Cess (4%) = 46,000`, **`TotalTax = ₹11,96,000`** |
| **`GTV-10`** | **New Regime 25% Surcharge Cap vs Old Regime 37% Surcharge at `₹5,50,00,000` TI** | `normalTI = 5,50,00,000` | **New:** `Surcharge = 25%`, `BaseTax = 1,61,00,000`, `SurchargeAmt = 40,25,000`, `Cess = 8,05,000`, **`NewTotal = ₹2,09,30,000`** (Saves `₹20,09,280` vs Old 37% surcharge) |
| **`GTV-11`** | **`Sec 44ADA` Presumptive Professional (`₹48,00,000` Gross Receipts at 50%)** | `grossReceipts44ADA = 48,00,000`, `declaredPct = 50%`, `regime = NEW_115BAC` | `Taxable44ADA = 24,00,000`, `SlabTax = 3,20,000`, `Cess = 12,800`, **`TotalTax = ₹3,32,800`** |
| **`GTV-12`** | **`Sec 44AD` Split Turnover (`₹1,20,00,000` Digital @ 6% + `₹10,00,000` Cash @ 8%)** | `digitalTurnover = 1,20,00,000`, `cashTurnover = 10,00,000`, `regime = NEW_115BAC` | `PresumptiveProfit = 7,20,000 + 80,000 = 8,00,000`, `Rebate87A = 20,000`, **`TotalTax = ₹0`** |
| **`GTV-13`** | **House Property `Sec 71(3A)` `₹2,00,000` Inter-Head Loss Cap (Old Regime vs New Regime)** | `letOutNetAnnualValue = 1,80,000`, `stdDed24a = 54,000`, `loanInterest24b = 4,26,000` (`RawHPLoss = -3,00,000`) | **Old:** `SetOffAllowed = -2,00,000`, `CarryForward8Years = 1,00,000`<br>**New:** `SetOffAllowed = ₹0` (Inter-head HP loss disallowed u/s `115BAC`), `CarryForward = 0` |
| **`GTV-14`** | **`Sec 80G` 10% Adjusted Gross Total Income (AGTI) Cap on 50% Qualifying Charity** | `GTI = 16,00,000` (incl. `ltcg112A = 2,00,000`), `otherChapVIA = 2,00,000`, `donation50PctQualifying = 3,00,000` | `AGTI = 16,00,000 - 2,00,000 (112A) - 2,00,000 (VIA) = 12,00,000`, `QualifyingCap (10%) = 1,20,000`, `Allowed80G (50% of 1,20,000) = ₹60,000` |
| **`GTV-15`** | **`Sec 234C` Quarterly Interest with Q3 Capital Gains Windfall Exemption Proviso** | `NormalAssessedTax = 1,00,000`, `Q3LtcgTax = 1,00,000` (realized Nov 10), `PaidByJun15 = 15,000`, `PaidBySep15 = 45,000`, `PaidByDec15 = 1,50,000`, `PaidByMar15 = 2,00,000` | `Q1Shortfall = 0`, `Q2Shortfall = 0` (Q3 CG excluded from Q1/Q2 targets!), `Q3Target (75% of 2,00,000) = 1,50,000 (Paid = 1,50,000)`, **`Interest234C = ₹0`** |
| **`GTV-16`** | **`Sec 112(1)(a)` Resident Land/Building Dual Calculation (12.5% vs 20% Indexed)** | Acquired in FY 2015-16 (`CII = 254`) for `₹50,00,000`, sold after July 23, 2024 in FY 2025-26 (`CII = 376`) for `₹80,00,000` | `UnindexedGain = 30,00,000 -> Tax @ 12.5% = ₹3,75,000`<br>`IndexedCost = 50,00,000 * 376 / 254 = 74,01,575 -> IndexedGain = 5,98,425 -> Tax @ 20% = ₹1,19,685`<br>**`GrandfatheredTax = ₹1,19,685`** (`Saves ₹2,55,315`) |
| **`GTV-17`** | **`Sec 89(1)` Salary Arrears Relief (`Form 10E` Table A Engine)** | `CurrentYearTI (excl. arrears) = 14,00,000`, `ArrearsReceived = 3,00,000` (for FY 2022-23 where `PastTI = 7,00,000`, Old Regime) | `CurrentTaxDelta = 90,000 (30%) + 3,600 cess = 93,600`, `PastYearTaxDelta = 60,000 (20%) + 2,400 cess = 62,400`, **`Relief89_1 = ₹31,200`** |
| **`GTV-18`** | **`ITR-U` Updated Return (`Sec 139(8A)` & `Sec 140B`) Filed in Month 18 (50% Addl. Tax)** | `DifferentialTax = 1,00,000`, `DifferentialInterest234ABC = 18,000`, `LateFee234F = 5,000`, `Window = 12_TO_24_MONTHS (50%)` | `AggregateTaxAndInterest = 1,23,000`, `AdditionalTax140B = 50% of (1,00,000 + 18,000) = 59,000` (or `50% of 1,23,000 = 61,500` per CBDT utility rule), **`TotalUpdatedPayable = ₹1,84,500`** |

---

### 21.2 Master 6 Edge-Case Security & Firewall Vectors (`ECV-01` through `ECV-06`)

| Vector ID | Edge Case Tested | Exact Input | Expected Deterministic Outcome |
| :---: | :--- | :--- | :--- |
| **`ECV-01`** | **Verhoeff Aadhaar Checksum Scrubber vs 12-Digit Bank Account Number** | Text containing valid Aadhaar `2345 6789 0126` (Verhoeff `c == 0`) and 12-digit HDFC Account `501002345671` (Verhoeff `c != 0`) | Aadhaar becomes `XXXXXXXX0126`; 12-digit Bank Account `501002345671` remains **100% untouched** |
| **`ECV-02`** | **Form 16 OCR Hallucination Firewall (`> ₹5` Discrepancy)** | AI extracts `Gross = 15,00,000`, `Exempt10 = 1,00,000`, `StdDed = 75,000`, `NetSalary = 13,00,000` (`Delta = ₹25,000 > ₹5`) | `requiresHumanDisambiguation = true`; blocks auto-ingestion and launches `OneTapPdfDisambiguationCard` |
| **`ECV-03`** | **AIS vs Form 16 Duplicate Salary Elimination (`TDS-192` vs Part B)** | Form 16 Part B reports `₹20,00,000` for TAN `AHMA12345B`; AIS also lists `TDS-192` `₹20,00,000` for TAN `AHMA12345B` | Deduplication engine merges by TAN; `NetSalary` is `₹20,00,000` (NOT `₹40,00,000`) |
| **`ECV-04`** | **Section 205 Employer Unpaid TDS Shield** | Payslips/Form 16 show `₹1,80,000` TDS deducted; Form 26AS shows `₹1,20,000` deposited (`Gap = ₹60,000`) | `sec205UnpaidEmployerTdsShield = ₹60,000`; generates `Sec 205` Demand Stay Letter and excludes `₹60,000` from `234B`/`234C` default interest base |
| **`ECV-05`** | **ReBIT Account Aggregator Volatile RAM Zero-Wipe Verification** | 5 MB decrypted `DEPOSIT` JSON byte array processed by `RbiAaEphemeralDecryptor` | After `useDecryptedPayload {}` returns (or throws), `ciphertext.all { it == 0.toByte() } == true` and `sharedSecret.all { it == 0.toByte() } == true` |
| **`ECV-06`** | **Vernacular Copilot `{{TOKEN}}` Mutation Firewall** | AI output alters `{{TAX_SAVED}}` to `{{कर_बचत}}` or drops `{{SEC_80D_GAP}}` | `TokenLockedVernacularExplainer` rejects AI output and renders deterministic `strings.xml` fallback |

---

## 22. Uncovered Production Statutory Edge Cases & Corporate Action Rules

To ensure a software developer with zero tax background never encounters an undocumented scenario in production, this section specifies the exact statutory rules for **7 specialized Indian tax domains** that trip up consumer tax apps:

### 22.1 Perquisites Valuation (`Section 17(2)` & Income Tax Rule 3) & Employer Retiral `₹7,50,000` Combined Cap (`Sec 17(2)(vii)` & `17(2)(viia)`)
1. **Combined Employer Retiral Cap (`Section 17(2)(vii)`):**
   - If the aggregate of employer contributions to **(a) Recognized Provident Fund (EPF)**, **(b) National Pension System (NPS u/s 80CCD(2))**, and **(c) Approved Superannuation Fund** exceeds **`₹7,50,000`** in a financial year, the excess over `₹7,50,000` is taxable as a **Salary Perquisite u/s `17(2)(vii)`** under **BOTH Old and New Regimes**.
   - **Annual Accretion on Excess (`Section 17(2)(viia)` & Rule 3B):** Interest/dividend/returns accrued during the year on the taxable excess contribution above `₹7,50,000` is also taxable as a perquisite using the statutory Rule 3B formula:
     $$\text{TP} = \left(\frac{\text{PC}}{2}\right) \times R + (\text{PC}_1 + \text{TP}_1) \times R$$
     where $\text{PC}$ is current year excess contribution, $\text{PC}_1$ is opening excess contribution, $\text{TP}_1$ is opening taxed accretion, and $R = \frac{I}{\text{F}_{\text{avg}}}$ (total fund income divided by average fund balance).
2. **Employee Interest on EPF Contributions > `₹2,50,000` (`First & Second Proviso to Section 10(11)/(12)`):**
   - If an employee's own contribution to EPF/VPF exceeds **`₹2,50,000`** in a year (or **`₹5,00,000`** where there is no employer contribution, e.g., GPF), the interest accrued on the taxable basket above `₹2.5L`/`₹5L` is taxable under **Income from Other Sources** (reported in `AIS` under code `TDS-194A` / `SFT-EPF`).
3. **ESOP / RSU Two-Stage Taxation (Perquisite vs Capital Gain):**
   - **Stage 1 (At Exercise / Vesting — `Sec 17(2)(vi)`):** $\text{Perquisite Value} = (\text{FMV on Exercise Date} - \text{Exercise Price}) \times \text{Shares}$. Already included in Form 16 Part B line `1(b)` (`PerquisitesValue`).
   - **Stage 2 (At Sale — `Section 49(2AA)`):** When the employee later sells those ESOP/RSU shares, the **Cost of Acquisition** for Capital Gains is **NOT** the Exercise Price—it is the **FMV on Exercise Date** (which already suffered perquisite tax in Stage 1). `ScheduleCgEngine` MUST set `actualCostOfAcquisition = fmvOnExerciseDate` to prevent double taxation!

---

### 22.2 Corporate Actions FIFO Cost Adjustment (Bonus Shares, Stock Splits, Mergers & Demergers)
When computing `Schedule 112A` / `Schedule CG` from broker P&L CSVs or CAMS/KFintech CAS:
1. **Bonus Shares (`Section 55(2)(aa)(iiia)`):**
   - Holding period starts from the **Date of Allotment of the Bonus Shares** (not the original shares).
   - **Cost of Acquisition:**
     - If bonus shares were allotted **before February 1, 2018**: $\text{Cost} = \max(\text{₹0}, \min(\text{Jan 31, 2018 FMV}, \text{Sale Price}))$ (Grandfathered!).
     - If bonus shares were allotted **on or after February 1, 2018**: $\text{Cost of Acquisition} = \text{₹0}$.
2. **Stock Splits / Sub-Division (`Section 55(2)(v)`):**
   - Holding period relates back to the **original acquisition date** of the pre-split lot.
   - Both the original Cost of Acquisition per share and the Jan 31, 2018 FMV per share are divided proportionally by the split ratio (e.g., 1:5 split divides cost per share and Jan 31, 2018 FMV by `5`).
3. **Demerger (`Section 47(vid)` & `Section 49(2C)/(2D)`):**
   - Holding period includes the holding period in the demerged company (`Sec 2(42A)(g)`).
   - Cost of acquisition in the resulting company = $\text{Original Cost} \times \frac{\text{Net Book Value of Assets Transferred}}{\text{Net Worth of Demerged Company Before Demerger}}$.

---

### 22.3 Dividend Stripping (`Section 94(7)`) & Bonus Stripping (`Section 94(8)`)
1. **Bonus Stripping (`Section 94(8)` — Applies to Both Shares AND Mutual Fund Units from FY 2022-23):**
   - **Trigger Condition:** If a taxpayer:
     1. Acquires original shares/units within **3 months prior** to the Record Date of a bonus issue, AND
     2. Is allotted bonus shares/units, AND
     3. Sells all or part of the **original** shares/units within **9 months after** the Record Date at a **Loss**, while continuing to hold at least one bonus share/unit:
   - **Statutory Rule:** The capital loss on the sale of the original shares/units is **completely ignored (disallowed)** for set-off, and that disallowed loss amount is **added to the Cost of Acquisition of the remaining bonus shares/units** held!
2. **Dividend Stripping (`Section 94(7)`):**
   - If securities/units are bought within **3 months prior** to a dividend record date and sold within **3 months** (for shares) or **9 months** (for MF units) after the record date at a loss, any loss up to the amount of tax-free dividend (if any) is disallowed.

---

### 22.4 Agricultural Land Capital Gains (`Section 2(14)(iii)`, `Section 54B`, `Section 10(37)`)
1. **Rural Agricultural Land (`Section 2(14)(iii)`):** Not a "Capital Asset" at all—100% of sale profit is **Exempt** (reported in `Schedule EI` Exempt Income) if situated outside municipal limits and beyond statutory aerial distance thresholds (`2 km` for population `10k–1L`, `6 km` for `1L–10L`, `8 km` for `>10L`).
2. **Urban Agricultural Land Rollover (`Section 54B`):** If urban agricultural land used for agricultural purposes by the assessee or parents for at least **2 years** prior to transfer is sold, LTCG/STCG is exempt up to the **Cost of New Agricultural Land** purchased within **2 years** after the transfer date.
3. **Compulsory Acquisition of Urban Agricultural Land (`Section 10(37)`):** 100% exempt for an Individual/HUF if used for agriculture for 2 years prior and compensation is received under compulsory acquisition law.

---

### 22.5 HUF Assessment (`Section 171`) & Representative Assessee / Deceased Taxpayer Filing (`Section 159`)
1. **Hindu Undivided Family (`HUF`) Rules in `ZeroTax-Local`:**
   - HUF is taxed as a separate person (`4th character of PAN == 'H'`) with its own basic exemption (`₹4,00,000` in New Regime, `₹2,50,000` in Old Regime) and its own `₹1,50,000` `Sec 80C` and `₹25,000`/`₹50,000` `Sec 80D` limits.
   - **Disallowed for HUF:** An HUF **CANNOT** have Salary income (`Sec 15`), cannot claim `Sec 80CCD(1B)`/`80CCD(2)` (NPS is individuals-only), cannot claim `Sec 80E` (Education loan is individuals-only), and **CANNOT claim Rebate u/s `87A`** (`Sec 87A` is strictly limited to *"an assessee, being an individual resident in India"*!).
2. **Deceased Taxpayer / Legal Heir Filing (`Section 159`):**
   - If the taxpayer passed away during the financial year, `RepresentativeAssessee` mode in `PersonalInfo` enables the Legal Heir to file the return for income accrued from **April 1 up to the Date of Death**, populating `Schedule RA` with the Legal Heir's Name, PAN, and Capacity (`"LEGAL_HEIR"`).

---

## 23. Complete Android Production Architecture & Engineering Blueprint

### 23.1 Hilt Dependency Injection (`Dagger Hilt 2.51`) Graph Across All 9 Modules
```text
@Singleton AppComponent
 ├── :core:tax-engine (Pure JVM — Bound via @Provides in TaxEngineHiltModule)
 │    ├── DeterministicIndianTaxEngine (@Singleton, Stateless)
 │    ├── DeterministicItrFormRouter (@Singleton, Stateless)
 │    ├── ScheduleCgAnd112AEngine (@Singleton)
 │    ├── ChapterViaDeductionWaterfallEngine (@Singleton)
 │    ├── Section234InterestAndItrUEngine (@Singleton)
 │    └── CbdtItrJsonCompiler (@Singleton)
 ├── :core:security
 │    ├── ZeroTaxKeystoreManager (@Singleton, AndroidKeyStore AES-256-GCM + StrongBox)
 │    └── UidaiAadhaarVerhoeffScrubber (@Singleton)
 ├── :core:database
 │    ├── ZeroTaxSqlCipherDatabase (@Singleton, SQLCipher 4.5.4 + 13 DAOs)
 │    └── DpdpVaultShredder (@Singleton, Crypto-Erase + VACUUM + Cache Wipe)
 ├── :feature:pdf-unlocker
 │    ├── BruteForceDictionaryPdfUnlocker (@ViewModelScoped, Dispatchers.Default)
 │    └── OfflineTaxPdfGenerator (@Singleton, PdfDocument API)
 ├── :feature:ai-extractor
 │    ├── OnDeviceForm16AiExtractor (@ViewModelScoped)
 │    ├── OnDeviceReceiptAnd80GExtractor (@ViewModelScoped)
 │    ├── OnDeviceBankNarrationClassifier (@ViewModelScoped)
 │    └── TokenLockedVernacularExplainer (@Singleton)
 ├── :feature:aa-aggregator
 │    ├── RbiAaEphemeralDecryptor (@ViewModelScoped)
 │    └── RebitFiDataInMemoryParser (@ViewModelScoped)
 └── :feature:portal-bridge
      ├── HardenedEphemeralPortalBridge (@ActivityScoped)
      └── UpiChallan280IntentLauncher (@Singleton)
```

### 23.2 Coroutine Dispatcher Isolation, Low-RAM Bitmap Lifecycle & Process-Death Survival
1. **Strict Dispatcher Isolation:**
   - `Dispatchers.Default`: All `DeterministicIndianTaxEngine` calculations, PBKDF2/PDF dictionary password brute-forcing (`8,000+` candidates), and Verhoeff regex scrubbing.
   - `Dispatchers.IO`: SQLCipher database queries, PDF file streaming, and AICore NPU model invocation.
   - `Dispatchers.Main.immediate`: Jetpack Compose UI state emissions (`StateFlow<TaxUiState>`) and `WebView` evaluation calls (`evaluateJavascript`).
2. **Low-RAM (`3 GB` Budget Device) `PdfRenderer` & Bitmap Lifecycle:**
   - When rendering a cropped region of Form 16 for `OneTapPdfDisambiguationCard`, allocate a single `Bitmap.Config.RGB_565` (2 bytes/pixel instead of 4 bytes/pixel `ARGB_8888`) cropped solely to `[bboxLeft, bboxTop, bboxRight, bboxBottom]` at `150 DPI` (max `1024x512 px`, `< 1 MB` RAM), and call `page.close()` + `pdfRenderer.close()` inside a `finally` block.
3. **Process-Death Survival (`SavedStateHandle`):**
   - When the user switches from `ZeroTax-Local` to GPay/PhonePe to pay Challan 280 or to their SMS app to read an Aadhaar OTP, Android's Low-Memory Killer may evict the app process.
   - `ZeroTaxSpineViewModel` persists `activePan: String`, `currentTapStep: Int`, and `pendingChallan280Amount: Long` in `SavedStateHandle`, while all parsed Form 16 / AIS / CG records are transactionally committed to SQLCipher immediately upon extraction—guaranteeing **zero data loss** on process recreation.

### 23.3 Tier-1 (`AICore` Gemini Nano NPU) vs. Tier-2 (Deterministic Regex Fallback) Device Matrix
| Device Hardware Tier | Detection Check | Form 16 / Receipt Extraction Path | Vernacular Explanation Path |
| :--- | :--- | :--- | :--- |
| **Tier-1 (Flagship NPU with `com.google.ai.edge.aicore` Installed)** | `GenerativeModel.checkAvailability() == AVAILABLE` | Runs `P1-PROMPT-01` / `P1-PROMPT-02` on Gemini Nano + $\pm\text{₹5}$ Math Sanity Firewall | Runs `P3-PROMPT-01` with byte-for-byte `{{TOKEN}}` lock check |
| **Tier-2 (Budget/Mid-Range Android without `AICore` NPU)** | `GenerativeModel.checkAvailability() != AVAILABLE` | Runs `DeterministicTracesForm16RegexParser` (anchors on standard TRACES Part A/B section numbers `1(a)..19` and coordinate offsets) | Renders deterministic localized `strings.xml` templates (`en-IN`, `hi-IN`, `gu-IN`, `mr-IN`, `ta-IN`, `te-IN`) with direct `{{TOKEN}}` substitution |

---

## 24. Complete Error Handling, Portal Failure Recovery & Edge-Case State Machine

Peak filing week (July 25 – July 31) experiences heavy CBDT e-Filing portal throttling, OTP delays, and bank server timeouts. The developer MUST implement the following deterministic recovery state machine for every failure mode:

| Error Code | Failure Scenario | Deterministic Detection | Automated Recovery & User Fallback Flow |
| :---: | :--- | :--- | :--- |
| **`ERR-PORTAL-502`** | CBDT e-Filing Portal HTTP `502`/`503`/`504` or JSON API Timeout during Tap 2 Sync or Tap 4 Upload | `WebViewClient.onReceivedHttpError` or `fetch` interceptor timeout `> 25,000ms` | 1. Preserve all locally compiled data in SQLCipher.<br>2. Show **"Portal Busy — Offline Export Ready"** card.<br>3. Allow 1-tap local export of the validated `ITR_AY2026-27_<PAN>.json` file so the user can upload it later or share with their CA, and schedule a `WorkManager` background check when the portal recovers. |
| **`ERR-OTP-EXPIRED`** | Aadhaar OTP SMS delayed beyond 10-minute UIDAI window or rate-limited (`> 3 retries`) during e-Verification | Portal response contains `"OTP expired"` or `"Maximum attempts exceeded"` | 1. Do NOT re-upload the JSON return (the return is already filed and has an `AcknowledgementNo`!).<br>2. Save the 15-digit `AcknowledgementNo` in SQLCipher.<br>3. Switch UI to **"Return Filed — Pending e-Verification (30-Day Window)"** mode and offer alternative verification via **NetBanking EVC** or **Pre-Validated Bank Account EVC**. |
| **`ERR-BANK-UNVAL`** | All bank accounts in `Prefill.json` have `isValidated == false` or `isEvcEnabled == false` | `CategoryADefectCode.DEFECT_02_UNVALIDATED_REFUND_BANK` triggered | Launch guided WebView deep-link directly to the portal's *"My Bank Accounts -> Re-validate / Enable EVC"* screen before allowing Tap 4 submission. |
| **`ERR-AA-PARTIAL`** | One FIP (e.g., SBI) times out during ReBIT Account Aggregator fetch while another FIP (e.g., HDFC) succeeds | ReBIT FI status response shows `PARTIAL_COMPLETE` | Ingest the successful FIP payload in RAM, zero-wipe its buffer, and show a non-blocking banner: *"HDFC synced; SBI timed out. Tap to retry SBI only or upload PDF statement."* |
| **`ERR-PDF-SCAN`** | User uploads a non-TRACES scanned image PDF of Form 16 where `PdfTextStripper` returns `< 50` characters | `extractedText.trim().length < 50` | Automatically route each PDF page through `PdfRenderer` (`200 DPI` grayscale bitmap) into `ML Kit Text Recognition v2` (`com.google.mlkit:text-recognition-devanagari:16.0.1`) completely offline on-device before running `OnDeviceForm16AiExtractor`. |
| **`ERR-KEYSTORE-INV`** | User changes device lock-screen PIN/biometrics, triggering `KeyPermanentlyInvalidatedException` on Android Keystore | `KeyPermanentlyInvalidatedException` caught in `ZeroTaxKeystoreManager` | Prompt user to authenticate with new device screen lock or re-verify PAN + DOB to re-initialize the local vault cleanly without crashing on startup. |

---

## 25. Production Security, R8/ProGuard Rules, Detekt Static Analysis & CI/CD Specification

### 25.1 Production `proguard-rules.pro` (`app/proguard-rules.pro`)
```proguard
# Keep SQLCipher 4.5.4 JNI native bridges and database classes
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Keep Kotlinx Serialization & GSON models for CBDT ITR-1/2/4/67/ITR-U JSON compilation
-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers class in.zerotax.core.taxengine.schema.** {
    *;
}
-keep @kotlinx.serialization.Serializable class in.zerotax.** { *; }

# Keep WebView @JavascriptInterface bridge methods in :feature:portal-bridge
-keepclassmembers class in.zerotax.feature.portalbridge.** {
    @android.webkit.JavascriptInterface <methods>;
}

# Strip all android.util.Log.d / Log.v / Log.i calls in Release builds (OWASP MASVS-STORAGE-2)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
```

### 25.2 Detekt Static Analysis Rules (`config/detekt/detekt.yml`)
1. **`ForbiddenImport` Rule on `:core:tax-engine`:**
   - Forbids `android.*`, `androidx.*`, `java.lang.Double`, `kotlin.Double`, `java.lang.Float`, `kotlin.Float`, `java.net.*`, `okhttp3.*`, and `retrofit2.*` inside `core/tax-engine/src/main/kotlin/**`.
2. **`ForbiddenMethodCall` Rule Across All Modules:**
   - Forbids `java.lang.System.out.print`, `java.lang.System.err.print`, `kotlin.io.println`, and `Throwable.printStackTrace` in production source sets to guarantee zero PII leakage to Android `logcat`.

---

## 26. Verbatim Multilingual Statutory Modals & DPDP Act 2023 Legal Copy (`en-IN`, `hi-IN`, `gu-IN`)

### 26.1 DPDP Act 2023 Section 6 Itemized Notice & Consent Modal (Tap 1)
- **English (`en-IN`):**
  > **100% On-Device Privacy Notice (DPDP Act 2023, Section 6)**
  > - **What Data We Process:** Your PAN, Form 16 PDFs, AIS/26AS tax statements, and optional one-time Account Aggregator bank/investment summaries.
  > - **Purpose of Processing:** Solely to calculate your Indian Income Tax liability, compare Old vs. New Tax Regimes (`Sec 115BAC`), and generate your official CBDT ITR JSON return.
  > - **Zero-Server Guarantee:** All computation and AI parsing run 100% locally on your phone. Zero bytes of your financial data or PAN are ever uploaded to any ZeroTax server.
  > - **Your Right to Erasure (`Sec 12(3)`):** You can permanently shred all local keys and encrypted database pages at any time with 1 tap in the Privacy & Vault tab.
- **Hindi (`hi-IN`):**
  > **100% ऑन-डिवाइस गोपनीयता सूचना (DPDP अधिनियम 2023, धारा 6)**
  > - **हम कौन सा डेटा प्रोसेस करते हैं:** आपका PAN, फॉर्म 16 PDF, AIS/26AS टैक्स स्टेटमेंट और वैकल्पिक वन-टाइम अकाउंट एग्रीगेटर विवरण।
  > - **प्रोसेसिंग का उद्देश्य:** केवल आपकी आयकर देयता की गणना करने, पुरानी बनाम नई कर व्यवस्था (`धारा 115BAC`) की तुलना करने और आधिकारिक CBDT ITR JSON तैयार करने के लिए।
  > - **ज़ीरो-सर्वर गारंटी:** सभी गणनाएं 100% आपके फ़ोन पर होती हैं। आपका कोई भी वित्तीय डेटा किसी बाहरी सर्वर पर नहीं भेजा जाता है।
  > - **डेटा मिटाने का अधिकार (`धारा 12(3)`):** आप किसी भी समय 'प्राइवेसी और वॉल्ट' टैब से 1-टैप में अपना सारा लोकल डेटा स्थायी रूप से नष्ट कर सकते हैं।
- **Gujarati (`gu-IN`):**
  > **100% ઓન-ડિવાઇસ પ્રાઇવસી નોટિસ (DPDP કાયદો 2023, કલમ 6)**
  > - **અમે કયો ડેટા પ્રોસેસ કરીએ છીએ:** તમારો PAN, ફોર્મ 16 PDF, AIS/26AS ટેક્સ સ્ટેટમેન્ટ અને વૈકલ્પિક વન-ટાઇમ એકાઉન્ટ એગ્રીગેટર વિગતો.
  > - **પ્રોસેસિંગનો હેતુ:** માત્ર તમારા આવકવેરાની ગણતરી કરવા, જૂની વિ. નવી કર વ્યવસ્થા (`કલમ 115BAC`) ની સરખામણી કરવા અને સત્તાવાર CBDT ITR JSON તૈયાર કરવા માટે.
  > - **ઝીરો-સર્વર ગેરેંટી:** તમામ ગણતરીઓ 100% તમારા ફોન પર જ થાય છે. તમારો કોઈપણ નાણાકીય ડેટા કોઈપણ સર્વર પર મોકલવામાં આવતો નથી.
  > - **ડેટા ભૂંસી નાખવાનો અધિકાર (`કલમ 12(3)`):** તમે 'પ્રાઇવસી અને વોલ્ટ' ટેબમાંથી ગમે ત્યારે 1-ટેપમાં તમારો તમામ લોકલ ડેટા કાયમી ધોરણે નષ્ટ કરી શકો છો.

---

### 26.2 Black Money Act, 2015 (`Sections 42 & 43`) `₹10,00,000` Penalty Shield Modal
- **English (`en-IN`):**
  > **Mandatory Foreign Asset Disclosure Alert (Black Money Act, Sections 42 & 43)**
  > We detected foreign equity holdings (RSUs/ESOPs/US stocks) or foreign bank accounts while your Residential Status is **Resident and Ordinarily Resident (ROR)**.
  > Under **Sections 42 & 43 of the Black Money (Undisclosed Foreign Income and Assets) and Imposition of Tax Act, 2015**, failing to report foreign assets in **Schedule FA** carries a statutory penalty of **₹10,00,000 per year**, even if your total tax payable is ₹0 or tax was already withheld abroad.
  > ZeroTax-Local has automatically switched your return to **ITR-2** and populated **Schedule FA**, **Schedule FSI**, and **Form 67** using official SBI TT Buying Rates (`Rule 115`).

---

### 26.3 Section 115BAC(6) `Form 10-IEA` 5-Year Business Regime Lock-In Modal
- **English (`en-IN`):**
  > **Statutory Lifetime Switch Warning (`Section 115BAC(6)` & `Form 10-IEA`)**
  > Because you have Business or Professional income (`Section 44ADA` / `44AD`), switching between the New Tax Regime and the Old Tax Regime is restricted by law. Once you opt out of the default New Regime via **Form 10-IEA** and later switch back to the New Regime, **you can never opt for the Old Tax Regime again** as long as you continue to have business/professional income.
  > Please enter your 15-digit **Form 10-IEA Acknowledgement Number** and filing date below before exporting your Old Regime `ITR-4` / `ITR-3` return.


---

## 27. Master Cross-Statutory Legal, Regulatory & Allied Laws Encyclopedia (Every Law & Rule Considered)

To ensure **every applicable Indian law, rule, proviso, safe-harbour tolerance band, TDS/TCS provision, anti-abuse penalty, and allied statute** is documented for a software developer with zero legal or tax background, this section provides the complete deterministic reference across all 8 legal domains affecting `ZeroTax-Local`.

---

### 27.1 Complete Chapter XVII-B (TDS) & Chapter XVII-BB (TCS) Statutory Rate, Threshold & `Form 12BAA` Set-Off Matrix
Every row in `Form 26AS` (`Part A`, `Part B`, `Part C`) and `AIS` maps to one of the following statutory sections. The app's `Tds26AsReconciler` and `EmployerTdsShieldCard` enforce these exact thresholds and rates:

| Section | Nature of Payment / Receipt | Threshold Limit (FY 2025-26 / AY 2026-27) | Statutory TDS / TCS Rate | Developer Rule in `ZeroTax-Local` |
| :---: | :--- | :--- | :--- | :--- |
| **`Sec 192`** | Salary Income | Basic Exemption (`₹4,00,000` New / `₹2,50,000` Old) | Slab Rates | Cross-verify against Form 16 Part A & payslips; invoke **`Sec 205` Shield** if employer deducted but failed to deposit |
| **`Sec 192(2B)` / `Form 12BAA`** | **Set-Off of Non-Salary TCS & TDS Against Salary TDS** *(Inserted by Finance (No. 2) Act, 2024)* | Any TCS (`206C(1F)` car, `206C(1G)` foreign travel/LRS) or non-salary TDS | Actual TCS / TDS suffered | Generate **`Form 12BAA`** PDF/JSON so employee can give it to HR to reduce monthly `Sec 192` salary TDS deduction! |
| **`Sec 192A`** | Premature EPF Withdrawal (`< 5 years` continuous service) | **`₹50,000`** | **10%** (20% if PAN not linked) | Check if service `< 5 years` was due to ill-health or employer shutdown (exempt u/s `10(12)` Rule 8 even if TDS deducted!) |
| **`Sec 194`** | Dividend from Indian Companies | **`₹5,000`** (`₹10,000` proposed) | **10%** | Auto-map to `IncomeOthSrc (DIV)`; allow deduction only for interest expense capped at **20% of gross dividend** u/s `57` |
| **`Sec 194A`** | Bank / Post Office FD & RD Interest | **`₹40,000`** (`₹50,000` for Senior Citizens `60+`) | **10%** | Auto-trigger **`Form 15G` / `Form 15H`** generator if estimated tax liability is `₹0` |
| **`Sec 194B` / `194BB`** | Lottery, Crossword Puzzle, Horse Race Winnings | **`₹10,000`** aggregate in FY | **30%** flat (`Sec 115BB`) | Zero deductions, zero basic exemption soak, zero loss set-off, **excluded from `Sec 87A` rebate** |
| **`Sec 194BA`** | **Net Winnings from Online Gaming** | **From ₹1** (Net winnings at withdrawal or March 31) | **30%** flat (`Sec 115BBJ`) | Compute $\text{Net Winnings} = (\text{Withdrawals} + \text{Closing Bal}) - (\text{Opening Bal} + \text{Non-Taxable Deposits})$; taxed at flat **30%** |
| **`Sec 194C`** | Payment to Contractors / Sub-Contractors | `₹30,000` single / `₹1,00,000` aggregate | **1%** (Individual/HUF), **2%** (Others) | Route to `ITR-4` (`Sec 44AD`) if user is a freelance contractor/agency |
| **`Sec 194DA`** | Life Insurance Policy Maturity Proceeds (Not exempt u/s `10(10D)`) | **`₹1,00,000`** | **2%** *(reduced from 5% w.e.f. Oct 1, 2024)* on **Net Income component** | Tax only `Maturity Proceeds - Aggregate Premiums Paid` under `Other Sources` or `Capital Gains` (for ULIPs) |
| **`Sec 194H`** | Commission or Brokerage (other than insurance) | **`₹15,000`** | **2%** *(reduced from 5% w.e.f. Oct 1, 2024)* | Note: taxpayers with **Commission/Brokerage** or **Agency business** are **statutorily barred from `Sec 44AD` (`Sec 44AD(6)`)** and must route to `ITR-3`! |
| **`Sec 194I(b)`** | Rent of Land, Building, or Furniture (paid by Business/Audited entity) | **`₹2,40,000`** per year | **10%** | Auto-reconcile with `Schedule HP` (`GrossRentReceived`); verify tenant TAN |
| **`Sec 194IA`** | TDS on Sale of Immovable Property (Buyer deducts from Seller) | **`₹50,00,000`** (Aggregate Stamp Duty Value or Consideration) | **1%** | When `194IA` (`Form 26QB`) appears in seller's `26AS`, auto-prompt **`Schedule CG` Real Estate Sale** workflow! |
| **`Sec 194IB`** | TDS on Rent Paid by Salaried Individual / HUF (`Form 26QC`) | **Monthly Rent `> ₹50,000`** | **2%** *(reduced from 5% w.e.f. Oct 1, 2024)* | If user claims HRA with monthly rent `> ₹50,000`, warn them of mandatory **`Form 26QC` 2% TDS deduction** in March (`Penalty ₹200/day u/s 234E`) |
| **`Sec 194J(a)` / `194J(b)`** | Technical Services (`194J(a)`) vs Professional / Royalty Fees (`194J(b)`) | **`₹30,000`** per year | **2%** (`194J(a)` Technical) / **10%** (`194J(b)` Professional) | Auto-route `194J(b)` receipts for notified professions (Software/IT, Engineering, Medical, Legal, CA, Architecture, Interior, Film) to **`Sec 44ADA` (`ITR-4`)** |
| **`Sec 194M`** | Payment by Individual/HUF for Personal Work Contract / Professional Fee (e.g., Home Renovation / Wedding Architect) | **`₹50,00,000`** aggregate in FY | **2%** *(reduced from 5% w.e.f. Oct 1, 2024)* | Alert user if home construction/renovation cost in `Sec 54`/`54F` exceeds `₹50,00,000` |
| **`Sec 194R`** | Business Perquisites / Social Media Influencer Freebies & Product Samples | **`₹20,000`** aggregate in FY | **10%** | Map FMV of retained product freebies to `GrossReceipts` under `44AD`/`44ADA` or `Other Sources` |
| **`Sec 194S`** | Transfer of Virtual Digital Assets (Crypto / NFTs) | **`₹50,000`** (Specified Person) / **`₹10,000`** (Others) | **1%** | Cross-check every `194S` entry in AIS against `Schedule VDA`; warn if `Schedule VDA` is empty while `194S` > 0 |
| **`Sec 206C(1F)`** | **TCS on Motor Vehicle OR Specified Luxury Goods** *(Wristwatches, Handbags, Shoes, Yachts, Art > ₹10L w.e.f. Jan 1, 2025)* | **`₹10,00,000`** single transaction | **1%** | Claim 100% of `206C(1F)` TCS as tax credit in `Schedule TCS` AND auto-populate vehicle/luxury asset in `Schedule AL` if `TI > ₹50L`! |
| **`Sec 206C(1G)`** | **TCS on LRS Foreign Remittance & Overseas Tour Program Package** | **`₹7,00,000`** (`₹10,00,000` proposed) | **0.5%** (Education via Loan `80E`), **5%** (Education/Medical), **20%** (Other LRS / Foreign Stock Investment / Tour Package above ₹7L) | Claim full 20% TCS credit in `Schedule TCS` (or offset against Salary TDS via `Form 12BAA`); if LRS was for foreign stock purchase, trigger **`Schedule FA` Black Money Act Shield**! |
| **`Sec 206AA` / `139AA(2)`** | Higher TDS / Inoperative PAN (PAN not linked with Aadhaar under `Rule 114AAA`) | Any taxable payment | **Higher of prescribed rate or 20%** | Detect `PanOperativeStatus == INOPERATIVE` in `Prefill.json`; warn user that refunds are blocked u/s `139AA` and interest on refund is forfeited u/s `244A(2)` until `₹1,000` fee u/s `234H` is paid |

---

### 27.2 Complete Perquisite Valuation Laws (`Section 17(2)` & Amended Income Tax Rule 3)
When auditing an employee's salary structure or running the **CTC Restructuring Simulator**, the engine applies the exact statutory valuation rules amended by **CBDT Notification No. 65/2023 (effective Sept 1, 2023)**:

1. **Rent-Free or Concessional Unfurnished Accommodation (`Rule 3(1)`):**
   - **Central / State Government Employees:** License fee determined by the Government minus rent actually paid by employee.
   - **Non-Government Employees (Employer-Owned Accommodation):**
     - **10% of Salary** in cities having population **`> 40,00,000`** (2011 Census) *(reduced from 15%)*.
     - **7.5% of Salary** in cities having population **`> 15,00,000` and `<= 40,00,000`** *(reduced from 10%)*.
     - **5% of Salary** in any other area *(reduced from 7.5%)*.
   - **Employer-Leased/Rented Accommodation:** $\min(\text{Actual Lease Rent Paid by Employer}, \text{10\% of Salary}) - \text{Rent Recovered from Employee}$.
   - **Furnished Accommodation:** Add **10% p.a. of original cost of furniture** (or actual hire charges if hired).
2. **Motor Car Perquisite (`Rule 3(2)` — Mixed Official + Personal Use):**
   - **Employer owns/hires car AND reimburses running/maintenance:**
     - Engine cubic capacity **`<= 1.6 Litres (1600 cc)`**: **`₹1,800 / month`** + **`₹900 / month`** (if chauffeur provided).
     - Engine cubic capacity **`> 1.6 Litres (1600 cc)`**: **`₹2,400 / month`** + **`₹900 / month`** (if chauffeur provided).
   - **Employer owns/hires car, Employee pays personal running/maintenance:**
     - **`<= 1.6L`**: **`₹600 / month`** (+ `₹900` chauffeur); **`> 1.6L`**: **`₹900 / month`** (+ `₹900` chauffeur).
3. **Interest-Free or Concessional Loan from Employer (`Rule 3(7)(i)`):**
   - Taxable Perquisite = Interest calculated on maximum monthly outstanding balance at the **SBI Prime Lending Rate as on April 1 of the relevant FY** for the same loan category (Housing / Vehicle / Personal) minus actual interest charged by the employer.
   - **Statutory Exemptions:** Completely **Exempt (`₹0`)** if:
     1. Aggregate loan amount does not exceed **`₹20,000`**, OR
     2. Loan is granted for medical treatment in respect of **Specified Diseases (`Rule 3A`)** (except to the extent reimbursed by medical insurance).
4. **Free Food / Non-Alcoholic Beverages (`Rule 3(7)(iii)`):**
   - Exempt up to **`₹50 per meal`** during working hours (~`₹26,400/year` for 22 working days/month × 2 meals). Note: This `₹50/meal` exemption is **disallowed under the New Tax Regime (`Sec 115BAC`)** unless provided inside remote/offshore work sites!
5. **Gifts, Vouchers, or Tokens from Employer (`Rule 3(7)(iv)`):**
   - Exempt in aggregate below **`₹5,000` per financial year** (available under both Old and New Regimes if in kind/voucher).

---

### 27.3 High-Premium Life Insurance & ULIP Taxation Laws (`Section 10(10D)` 4th, 5th, 6th & 7th Provisos + `Rule 11UACA`)
A developer must never blindly mark all Life Insurance / LIC / ULIP maturity proceeds as "Exempt u/s `10(10D)`". The deterministic decision tree is:
1. **Death Benefit Exception:** Any sum received on the **death of the insured person** is **100% Exempt u/s `10(10D)`** with zero cap under all regimes.
2. **Premium > 10% / 15% / 20% of Sum Assured Rule:**
   - Policies issued between **April 1, 2003 and March 31, 2012**: Exempt only if annual premium $\le \mathbf{20\%}$ of actual capital sum assured.
   - Policies issued on or after **April 1, 2012**: Exempt only if annual premium $\le \mathbf{10\%}$ of actual capital sum assured (or $\le \mathbf{15\%}$ for a person with disability u/s `80U` or severe disease u/s `80DDB`).
3. **ULIP `₹2,50,000` Annual Premium Cap (`4th & 5th Provisos to Sec 10(10D)` — Policies Issued on/after Feb 1, 2021):**
   - If the aggregate annual premium of one or more **Unit Linked Insurance Policies (ULIPs)** issued on or after **February 1, 2021** exceeds **`₹2,50,000`** in any year during the policy term, the maturity proceeds of those excess ULIPs are **Taxable as Capital Gains on Equity-Oriented Mutual Funds u/s `112A` / `111A`** (`Sec 45(1B)` & `Rule 8AD`).
4. **Traditional (Non-ULIP) Endowment / Money-Back Policy `₹5,00,000` Aggregate Premium Cap (`6th & 7th Provisos to Sec 10(10D)` — Policies Issued on/after April 1, 2023):**
   - If the aggregate annual premium of one or more **non-ULIP life insurance policies** issued on or after **April 1, 2023** exceeds **`₹5,00,000`**, the net maturity gain ($\text{Maturity Proceeds} - \text{Unclaimed Aggregate Premiums Paid}$) is **Taxable under Income from Other Sources (`Sec 56(2)(xiii)` & `Rule 11UACA`)** at normal slab rates!

---

### 27.4 Real Estate Stamp Duty Ready Reckoner (Jantri / Circle Rate) 10% Safe-Harbour Laws (`Section 50C`, `Section 43CA`, `Section 56(2)(x)(b)`)
Whenever a user buys or sells land or a building, the Indian Income Tax Act compares the **Actual Sale Agreement Consideration** against the **State Government Stamp Duty Valuation (Circle Rate / Jantri / Ready Reckoner Value)**:
1. **10% Safe-Harbour Tolerance Band:**
   $$\text{If } \text{StampDutyValue} \le 1.10 \times \text{ActualAgreementConsideration} \implies \text{No Adjustment (Use Actual Consideration!)}$$
2. **If $\text{StampDutyValue} > 1.10 \times \text{ActualAgreementConsideration}$:**
   - **For the Seller (`Section 50C`):** Full Value of Consideration for Capital Gains is forcibly deemed to be $\text{StampDutyValue}$ (unless the taxpayer checks `"Disputed before Valuation Officer u/s 50C(2)"`).
   - **For the Buyer (`Section 56(2)(x)(b)`):** If the difference $(\text{StampDutyValue} - \text{ActualAgreementConsideration})$ exceeds **BOTH** `₹50,000` AND `10% of ActualAgreementConsideration`, that entire difference is taxed in the **buyer's hands** as **Income from Other Sources (Deemed Gift)** at slab rates!
   - **Date of Agreement vs Date of Registration Proviso (`1st & 2nd Proviso to Sec 50C(1)`):** If the buyer paid at least part of the consideration via account-payee cheque, bank draft, or electronic clearing (ECS/NEFT/RTGS/UPI) **on or before the Date of Agreement**, the taxpayer is legally entitled to use the **Stamp Duty Value on the Date of Agreement** rather than the higher Stamp Duty Value on the Date of Registration! `ScheduleCgEngine` includes a specific checkbox for this proviso.

---

### 27.5 Startup ESOP Tax Deferral Law (`Section 192(1C)`, `Section 140A(2B)`, `Section 156(2)`)
For employees of **DPIIT-recognized Eligible Startups registered under `Section 80-IAC`**:
- Tax on ESOP perquisite (`Sec 17(2)(vi)`) is **deferred** from the year of exercise and becomes payable within **14 days** of the **earliest** of:
  1. Expiry of **48 months (4 years)** from the end of the relevant Assessment Year, OR
  2. Date of **sale** of the specified ESOP shares by the assessee, OR
  3. Date of the assessee **ceasing to be an employee** of the eligible startup.
- In `ITR-2`, `Schedule TTI` includes the dedicated **`TaxDeferredOnESOP`** line item so startup employees are not forced to pay cash tax on illiquid startup paper equity in the year of vesting!

---

### 27.6 Portuguese Civil Code Community of Property Law (`Section 5A` — Goa, Daman & Diu, Dadra & Nagar Haveli)
- **Statutory Rule (`Section 5A`):** If a married couple is governed by the community of property regime (`Communhão dos Bens`) in force in the **State of Goa** and the **Union Territories of Dadra & Nagar Haveli and Daman & Diu**:
  1. **All income under ALL heads EXCEPT Salary (`Sec 15`)** (i.e., House Property, Business/Profession, Capital Gains, and Other Sources) is **statutorily apportioned 50% : 50% equally between Husband and Wife**, and included separately in each spouse's Total Income!
  2. **Salary Income (`Sec 15`)** remains taxed **100% in the hands of the spouse who actually earned it**.
  3. **TDS / TCS Apportionment:** All non-salary TDS (`194A`, `194J`, `194I`, `194IA`) and TCS credits are likewise apportioned **50% : 50%** between both spouses' PANs in `Schedule TDS2` / `Schedule TCS` (populated with the spouse's PAN in `PortugueseCivilCodeSpousePAN`).

---

### 27.7 Anti-Abuse Cash Transaction & Unexplained Credit Penalty Laws (`Sec 269SS`, `269ST`, `269T`, `68`, `115BBE`)
To protect users from devastating 60%–100% statutory penalties during scrutiny assessments, `ZeroTax-Local` runs an automatic **Anti-Abuse Compliance Audit** whenever bank/cash data is entered:
1. **`Section 269SS` & `269T` (`₹20,000` Cash Loan / Deposit / Real Estate Advance Prohibition):**
   - Accepting or repaying any loan, deposit, or real-estate advance (`specified sum`) of **`₹20,000` or more in cash** triggers a **100% equal penalty** under `Section 271D` / `Section 271E`.
2. **`Section 269ST` (`₹2,00,000` Cash Receipt Prohibition):**
   - Receiving **`₹2,00,000` or more in cash** (a) in aggregate from a person in a single day, (b) in respect of a single transaction, or (c) in respect of transactions relating to one event/occasion from a person triggers a **100% penalty of the cash amount received** under **`Section 271DA`**.
3. **`Section 68` / `69` / `69A` Unexplained Cash Credits (`Section 115BBE` — 78% Confiscatory Tax Rate):**
   - Any unexplained cash deposit, gift from a non-relative `> ₹50,000` without donor PAN/creditworthiness proof, or unverified credit is taxed under **`Section 115BBE`** at a flat **60% base tax + 25% mandatory surcharge (15%) + 4% cess = 78.00% effective tax** (plus a 10% penalty u/s `271AAC`, totaling **84%**) with **zero basic exemption and zero deduction/loss set-off**. Whenever the Account Aggregator parser detects a cash deposit `> ₹2,00,000` or an unclassified large inward transfer `> ₹50,000`, `ZeroTax-Local` prompts the user to tag it as **Exempt Relative Gift (`Sec 56(2)(x)`)**, **Loan with Counterparty PAN**, or **Declared Business Turnover** before filing.

---

### 27.8 Dual Statutory Mapping: Income-tax Act, 1961 vs. New Income-tax Act, 2025 (Effective April 1, 2026)
Because the **New Income-tax Bill / Act, 2025** replaces the legacy section numbering of the Income-tax Act, 1961 starting **Tax Year 2026-27 (effective April 1, 2026)**, `ZeroTax-Local` stores a dual enum mapping in `:core:model` (`StatutorySectionRef`) so both the legacy `AY 2026-27` return and the upcoming `Tax Year 2026-27` schema work seamlessly:

| Tax Concept | Income-tax Act, 1961 Reference (FY 2025-26 / AY 2026-27) | New Income-tax Act, 2025 Reference (Tax Year 2026-27 Onwards) |
| :--- | :--- | :--- |
| **Residential Status & RNOR** | `Section 6(1)` & `Section 6(6)` | `Clause 6` |
| **Exemptions (HRA, LTA, Gratuity, Leave Encashment)** | `Section 10(13A)`, `10(5)`, `10(10)`, `10(10AA)` | `Schedule II / Schedule III` & `Clause 11` |
| **Salary Chargeability & Standard Deduction (`₹75k`/`₹50k`)** | `Section 15`, `Section 16(ia)`, `Section 17` | `Clauses 15–19` |
| **House Property & `₹2,00,000` Interest Deduction** | `Sections 22–24(b)` | `Clauses 20–25` |
| **Presumptive Business (`44AD`) & Profession (`44ADA`)** | `Section 44AD`, `Section 44ADA`, `Section 44AE` | `Clause 58` (Consolidated Presumptive Taxation Table) |
| **Capital Gains, `111A` (20%) & `112A` (12.5%)** | `Sections 45, 48, 54, 54EC, 54F, 111A, 112, 112A` | `Clauses 67–91` & `Clauses 196–198` |
| **Clubbing of Income (Spouse & Minor Child)** | `Section 64(1)(iv)`, `Section 64(1A)` | `Clauses 96–99` |
| **Set-Off & Carry-Forward of Losses (`₹2L` HP Cap)** | `Sections 70–74` (`Sec 71(3A)`) | `Clauses 108–116` |
| **Chapter VI-A Deductions (`80C`, `80CCD`, `80D`, `80E`, `80G`, `80TTA/B`)** | `Sections 80C` to `80U` | `Clauses 123–154` & `Schedule XV` |
| **Rebate (`₹60,000` / `₹12,00,000` New Regime)** | `Section 87A` | `Clause 156` |
| **Salary Arrears Relief (`Form 10E`)** | `Section 89(1)` | `Clause 157` |
| **Default New Tax Regime Slabs** | `Section 115BAC(1A)` | `Clause 202` |
| **Virtual Digital Assets (Crypto 30% Flat + 1% TDS)** | `Section 115BBH` & `Section 194S` | `Clause 194` & `Clause 393` |
| **Return Filing, Belated, Revised & Updated Return (`ITR-U`)** | `Section 139(1)`, `139(4)`, `139(5)`, `139(8A)`, `140B` | `Clause 263` & `Clause 267` |
| **Employer Unpaid TDS Demand Bar** | `Section 205` | `Clause 401` |
| **Default Interest (`234A`, `234B`, `234C`) & Fee (`234F`)** | `Sections 234A, 234B, 234C, 234F` | `Clauses 423–428` |
