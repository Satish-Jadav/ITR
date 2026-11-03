03 — Information Architecture, Screen-by-Screen Wireframes & Multilingual CBDT Taxonomy (Consolidated Phases 1–3)
1. Dual-Mode Navigation Topology & Phase 1–3 Evolution
To ensure first-time filers complete their return in under 180 seconds without ever confronting raw government tax schedules—while giving power users, investors, and MNC employees year-round portfolio and notice management—the application implements a Dual-Mode Navigation Architecture:

Mode A: The "4-Tap Linear Spine" (Phase 1–3 Guided Onboarding Funnel): A distraction-free wizard (Tap 1: Identity & Consent -> Tap 2: Automated Multi-Source Discovery -> Tap 3: Exception Nudge Cards -> Tap 4: UPI Challan 280, Regime Showdown & 1-Tap e-Verify).
Mode B: The "3-Tab Persistent Main Workspace" (Phase 2 & 3 Post-Onboarding Hub):
Tab 1 — Filing & Refund Hub: Live CPC Processing Tracker (Filed -> e-Verified -> Processed u/s 143(1) -> Refund Issued), Capital Gains & Loss Carry-Forward Summary Card, March LTCG Tax-Harvesting Radar, and 1-tap Revised Return u/s 139(5) / ITR-U launcher.
Tab 2 — Smart Tax Vault & Notice Copilot: Organized into Official Portal Artifacts (Prefill, 26AS, AIS, Challan 280), Investment & Global Broker Statements (CAMS CAS, Zerodha, E*TRADE/Schwab Schedule FA), Salary/Deduction Proofs, and the Multilingual AI Notice Explainer (EN / HI / GU).
Tab 3 — Privacy & Settings: RBI Account Aggregator Consent Manager (View/Revoke active FIP consents), Biometric Vault Lock toggle, and the 1-Tap "Shred Local Tax Vault" (DPDP Section 12(3) Right to Erasure) button.

+=========================================================================================+
|                  GLOBAL TOP BAR: [ZeroTax-Local]  [Lang: EN|हिं|ગુજ]  [Privacy Shield]   |
+=========================================================================================+
                                             |
                   +-------------------------+-------------------------+
                   | (First-Time / Active Filing)                      | (Post-Filing / Pro Hub)
                   v                                                   v
+----------------------------------------------------+ +--------------------------------+
|              4-TAP LINEAR FILING SPINE             | |   PERSISTENT 3-TAB WORKSPACE   |
| Tap 1: Smart PAN + DOB + DPDP/AA Consent           | | - Tab 1: Filing & Refund Hub   |
| Tap 2: Portal + Local PDF + AA Live Sweep          | | - Tab 2: Smart Tax Vault &     |
| Tap 3: Swipeable Exception Nudge Card Deck         | |          Notice Copilot        |
| Tap 4: UPI Challan 280 + Regime Showdown + Verify  | | - Tab 3: Privacy & AA Manager  |
+----------------------------------------------------+ +--------------------------------+

2. Screen-by-Screen High-Fidelity ASCII Wireframes
2.1 Tap 1: Identity, Smart PAN Input & DPDP Section 6 Consent Screen
+-----------------------------------------------------------------------+
| [Shield] ZeroTax-Local                      [English v] [हिन्दी] [ગુજરાતી] |
|-----------------------------------------------------------------------|
| FILE YOUR ITR IN 3 MINUTES — 100% ON YOUR PHONE                       |
| Zero cloud uploads. Zero spam calls. Hardware-encrypted tax vault.    |
|                                                                       |
| 1. YOUR PERMANENT ACCOUNT NUMBER (PAN)                                |
| +-------------------------------------------------------------------+ |
| | A B C P E 1 2 3 4 F                                   [Verified]  | |
| +-------------------------------------------------------------------+ |
|   [✓] Valid Individual PAN ('P'). Keyboard auto-switched layouts.     |
|                                                                       |
| 2. DATE OF BIRTH (As per PAN)                                         |
| +-------------------------------------------------------------------+ |
| | 15 / 08 / 1995                                        [Calendar]  | |
| +-------------------------------------------------------------------+ |
|   Category: Regular Citizen (Age 30 as of 31-Mar-2026)                |
|                                                                       |
| 3. DPDP ACT 2023 & CLIENT-SIDE AUTOMATION CONSENT                     |
| +-------------------------------------------------------------------+ |
| | [x] I consent to on-device processing of my Form 16, 26AS, AIS,   | |
| |     and investment statements solely for ITR preparation.         | |
| |     • Raw PDFs never leave this phone (Android AICore + SQLCipher)| |
| |     • Portal sync acts as a local client-side browser utility;    | |
| |       passwords/OTPs are never sent to ZeroTax servers.           | |
| |     • Withdraw & shred all data anytime in 1 tap.                 | |
| |                                     [Read Full Legal Notice ->]   | |
| +-------------------------------------------------------------------+ |
|                                                                       |
| +===================================================================+ |
| |          [Lock] INITIALIZE HARDWARE-ENCRYPTED TAX VAULT           | |
| +===================================================================+ |
+-----------------------------------------------------------------------+
2.2 Tap 2: Live Automated Discovery & 1-Tap Visual Cropped Disambiguation Card
+-----------------------------------------------------------------------+
| <- Back   LIVE TAX DISCOVERY (AY 2026-27)              [Shred Vault]  |
|-----------------------------------------------------------------------|
| [✓] Income Tax Portal Synced (Cookies Wiped in 310ms)                 |
|     • Prefill.json, Form 26AS & AIS/TIS verified                      |
| [✓] Unlocked Form16_TCS_PartA_B.pdf (Password #1 matched in 42ms)     |
|     • TAN: MUMT01928A | Gross: ₹10,50,000 | 26AS TDS: ₹65,000         |
| [!] 1 Action Needed on Scanned_Form16_Infosys.pdf (TAN: BLRI99887C)   |
|                                                                       |
| +===================================================================+ |
| | [!] MATH FIREWALL CHECK: CONFIRM 1 FIGURE IN 1 TAP                | |
| |-------------------------------------------------------------------| |
| | Why? Scanned PDF page had a crease over "Net Taxable Salary".     | |
| |                                                                   | |
| | +---------------------------------------------------------------+ | |
| | | [CROPPED PDF BITMAP REGION: Page 2, Bounding Box (110,480)]   | | |
| | |  "6. Income chargeable under head Salaries: Rs. 13,25,000"    | | |
| | +---------------------------------------------------------------+ | |
| |                                                                   | |
| | Gross (₹14,00,000) - Std Deduction (₹75,000) = ₹13,25,000         | |
| |                                                                   | |
| | +-------------------------------+ +-----------------------------+ | |
| | | [✓] Use ₹13,25,000 (Exact)    | | [ ] Use ₹13,85,000 (OCR)    | | |
| | +-------------------------------+ +-----------------------------+ | |
| +===================================================================+ |
+-----------------------------------------------------------------------+
2.3 Tap 3: Swipeable Exception-Only "Nudge Cards"
Card A: 80TTA / 80TTB Savings & FD Interest Auto-Claimed
+-----------------------------------------------------------------------+
| [BANK ICON] TAX-FREE BANK INTEREST CLAIMED               Card 1 of 4  |
|-----------------------------------------------------------------------|
| We identified ₹8,400 in Savings Account Interest from your AIS.       |
| • Automatically claimed under Section 80TTA: ₹8,400 (Old Regime)      |
| • For Senior Citizens (Age >= 60): Auto-switches to Section 80TTB     |
|   (up to ₹50,000, including Fixed Deposit interest).                  |
|                                                                       |
|  [ ✓ Verified & Auto-Applied ]                                        |
+-----------------------------------------------------------------------+
Card B: Regime Break-Even Optimizer Nudge
+-----------------------------------------------------------------------+
| [SCALE ICON] REGIME BREAK-EVEN OPTIMIZER                 Card 2 of 4  |
|-----------------------------------------------------------------------|
| New Regime currently saves you ₹14,200.                               |
| However, if you have additional deductions of ₹22,500 (such as unpaid |
| health insurance u/s 80D or rent receipts), Old Regime becomes better.|
|                                                                       |
| Do you have any additional 80D medical insurance bills to declare?    |
|  [ Scan Medical Bill / Receipt ]           [ Skip, Keep New Regime ]  |
+-----------------------------------------------------------------------+
Card C: Employer TDS Protection Shield (Form 16 > Form 26AS)
+-----------------------------------------------------------------------+
| [ALERT ICON] EMPLOYER TDS PROTECTION SHIELD              Card 3 of 4  |
|-----------------------------------------------------------------------|
| We prevented an automated CPC Bengaluru Demand Notice (Sec 143(1))!   |
|                                                                       |
| WHAT WE FOUND FOR EMPLOYER TAN: BLRS55443D                            |
| • TDS Printed in your Form 16 PDF:          ₹1,80,000                 |
| • TDS Actually Deposited in Form 26AS:      ₹1,20,000                 |
| • Unreflected Q4 TDS Discrepancy:           ₹  60,000                 |
|                                                                       |
| HOW WE PROTECTED YOU:                                                 |
| We capped your claimed TDS in Schedule TDS1 at ₹1,20,000 (the exact   |
| Form 26AS verified limit). Once your employer files their Q4 24Q      |
| correction, you can file a 1-tap Revised Return (Sec 139(5)) to       |
| claim the remaining ₹60,000 refund.                                   |
|                                                                       |
|  [ Draft Follow-Up Email to Employer HR ]     [ Got It, Continue -> ] |
+-----------------------------------------------------------------------+
Card D: Freelancer Section 44ADA Presumptive Profit Optimizer
+-----------------------------------------------------------------------+
| [BRIEFCASE ICON] FREELANCER & CONSULTANT 44ADA SHIELD    Card 4 of 4  |
|-----------------------------------------------------------------------|
| We detected ₹18,00,000 in Sec 194J receipts (Form 26AS) plus          |
| ₹6,00,000 in foreign Upwork/Stripe bank credits.                      |
|                                                                       |
| • Auto-Routed Form:     ITR-4 (Sugam)                                 |
| • Total Gross Receipts: ₹24,00,000 (100% Digital — Limit is ₹75L)     |
|                                                                       |
| PRESUMPTIVE TAXABLE PROFIT SLIDER (SECTION 44ADA):                    |
|   Minimum Statutory Rate without Audit: 50% (₹12,00,000 Taxable)      |
|   [=================(50%)=================|--------------------] 100% |
|                                                                       |
|   Taxable Income Added: ₹12,00,000  |  Tax-Free Expense Deduction:    |
|                                     |  ₹12,00,000 (No receipts needed)|
|                                                                       |
|  [ Confirm 50% Presumptive Profit (₹12,00,000) ]                      |
+-----------------------------------------------------------------------+
2.4 Tap 4: Regime Showdown, Level-3 Provenance Drawer & 1-Tap e-Verify
+-----------------------------------------------------------------------+
| <- Tap 3    REGIME SHOWDOWN & 1-TAP E-VERIFY            [Audit Trail] |
|-----------------------------------------------------------------------|
| +-----------------------------------+-------------------------------+ |
| | * RECOMMENDED — SAVES ₹18,720 *   |                               | |
| | NEW REGIME (Sec 115BAC Default)   | OLD TAX REGIME                | |
| |-----------------------------------|-------------------------------| |
| | Gross Salary:        ₹24,50,000 > | Gross Salary:    ₹24,50,000 > | |
| | Exempt Allowances:   ₹        0   | Exempt HRA/Sec10:₹ 1,60,000 > | |
| | Standard Deduction: -₹   75,000 > | Standard Ded:   -₹   50,000 > | |
| | Savings/FD Interest:+₹   34,200 > | Savings/FD Int: +₹   34,200 > | |
| | Employer NPS 80CCD2:-₹ 1,40,000 > | Chap VI-A (80C/D)-₹2,10,000 > | |
| |-----------------------------------|-------------------------------| |
| | Taxable Income:      ₹22,69,200   | Taxable Income:  ₹20,64,200   | |
| | Total Tax + Cess:    ₹ 3,96,030 > | Total Tax + Cess:₹ 4,14,750 > | |
| | Verified 26AS TDS:  -₹ 3,80,000 > | Verified 26AS TDS-₹3,80,000 > | |
| |===================================|===============================| |
| | NET TAX PAYABLE:     ₹   16,030   | NET TAX PAYABLE: ₹   34,750   | |
| +-----------------------------------+-------------------------------+ |
|   (i) Tap any ₹ number above to inspect source PDF crop & formula.    |
|                                                                       |
| STEP 1: CLEAR SELF-ASSESSMENT TAX (CHALLAN 280 - MINOR HEAD 300)      |
| +-------------------------------------------------------------------+ |
| | [UPI Icon] Pay ₹16,030 via GPay / PhonePe & Auto-Fill Schedule IT | |
| +-------------------------------------------------------------------+ |
|                                                                       |
| STEP 2: STATUTORY VERIFICATION DECLARATION (SECTION 140)              |
| [x] I, AARAV SHARMA (PAN: ABCPE1234F), solemnly declare that to the   |
|     best of my knowledge and belief, the information given in this    |
|     return (`ITR-1`) is correct and complete under the IT Act, 1961.  |
|                                                                       |
| +===================================================================+ |
| |        [Check] SUBMIT CBDT JSON & E-VERIFY WITH AADHAAR OTP       | |
| +===================================================================+ |
+-----------------------------------------------------------------------+
Level-3 Provenance Audit Bottom Drawer (Screen 1.4B)
+-----------------------------------------------------------------------+
| LEVEL-3 PROVENANCE & STATUTORY AUDIT DRAWER                           |
| Consolidated Gross Salary: ₹24,50,000                       [Close X] |
|-----------------------------------------------------------------------|
| [SOURCE 1 CARD: TCS]                                                  |
| • Employer: Employer 1 (TCS - TAN: MUMT01928A)                        |
| • Verification: Form16_TCS.pdf (Page 2, Box [112, 340, 490, 368])     |
| • Hash: SHA-256: 8f9c...4a1e | Gross u/s 17(1): ₹10,50,000            |
| • 26AS Deposited TDS: ₹65,000                       [View PDF Crop]   |
|                                                                       |
| [SOURCE 2 CARD: INFOSYS]                                              |
| • Employer: Employer 2 (Infosys - TAN: BLRI99887C)                    |
| • Verification: Scanned_Form16_Infosys.pdf (Math Sanity ±₹0)          |
| • Hash: SHA-256: 3b2d...9c8f | Gross u/s 17(1): ₹14,00,000            |
| • 26AS Deposited TDS: ₹1,15,000                      [View PDF Crop]   |
|                                                                       |
| +-------------------------------------------------------------------+ |
| | DE-DUPLICATION & STATUTORY FORMULA BOX                           | |
| | Consolidated Gross = ₹10,50,000 + ₹14,00,000 = ₹24,50,000         | |
| | • Duplicate AIS TDS-192 rows suppressed by TAN key.               | |
| | • Sec 16(ia) Standard Deduction ₹75,000 applied strictly ONCE.    | |
| +-------------------------------------------------------------------+ |
+-----------------------------------------------------------------------+
2.5 Phase 2 & 3 Workspace Blueprints
Level-2 Capital Gains Drill-Down Sheet (Schedule CG & 112A Visualizer)
+-------------------------------------------------------------------+
| Capital Gains & Losses Summary (ITR-2)                   [Close X]|
+-------------------------------------------------------------------+
| [Pill: Reconciled with AIS SFT-016/017 & CAMS CAS ✓]              |
|                                                                   |
| 1. Long-Term Equity & MF (Sec 112A)                               |
| • Total Gains Realized:                          ₹1,65,000        |
| • Less: Statutory Tax-Free Exemption:           -₹1,25,000        |
| • Less: Short-Term Loss Set-Off (CYLA):          -₹ 15,000        |
| • Net Taxable LTCG (12.5%):                      ₹  25,000        |
|   [View 14 ISIN-Level Grandfathering Rows ->]                     |
|                                                                   |
| 2. Carry-Forward Losses to Next 8 Years (Schedule CFL)            |
| • Unabsorbed Long-Term Loss Saved for Future:    ₹  32,400        |
+-------------------------------------------------------------------+
Foreign Asset Compliance Shield (Schedule FA Table A3 & Form 67)
+-------------------------------------------------------------------+
| [Badge: Black Money Act Compliant ✓]                              |
+-------------------------------------------------------------------+
| Source: E*TRADE Statement (Calendar Year Jan 1 – Dec 31)         |
| Exchange Rate Engine: Official SBI TT Buying Rate (Offline DB)    |
|                                                                   |
| • Vested Shares Held (GOOGL - US02079K3059):     42 Units         |
| • Initial Value in INR (TTBR on Vest Date):      ₹4,82,110        |
| • Peak Value in INR (Jan–Dec Peak Date):         ₹6,14,900        |
| • Closing Value in INR (Dec 31 TTBR):            ₹5,98,400        |
|                                                                   |
| • US Dividend Tax Withheld (25%):                ₹   8,420        |
| • DTAA Credit Claimed via Auto-Form 67:         -₹   8,420        |
|                                                                   |
|   [View Lot-by-Lot SBI TTBR Calculation Table ->]                 |
+-------------------------------------------------------------------+
Multilingual Notice Explainer (Tab 3 -> Module A)
+-------------------------------------------------------------------+
| [ English ]  [ हिन्दी ]  [ ગુજરાતી ✓ ]                             |
+-------------------------------------------------------------------+
| નોટિસ: કલમ 143(1) ઈન્ટીમેશન — આકારણી વર્ષ 2026-27                 |
|                                                                   |
| > સારાંશ (Summary):                                               |
| આવકવેરા વિભાગે તમારા રિટર્નની ગણતરી પૂર્ણ કરી છે. તમારા ફોર્મ     |
| 26AS અને રિટર્ન વચ્ચે ₹0 નો તફાવત છે. તમારું ₹24,680 નું રિફંડ    |
| મંજૂર થઈ ગયું છે અને તમારા બેંક ખાતામાં જમા થશે.                  |
|                                                                   |
|   [ ઇન્ટીમેશન ઓર્ડર ડાઉનલોડ કરો ]   [ બેંક રિફંડ ટ્રેક કરો ]     |
+-------------------------------------------------------------------+
In-App UPI Challan 280 Bottom Sheet (Screen 1.4A — Phase 2 Tax-Due Flow)
+-------------------------------------------------------------------+
| Self-Assessment Tax Due: ₹8,450                         [Close X] |
+-------------------------------------------------------------------+
| Why do I owe tax? Your AIS shows ₹62,000 FD interest where the    |
| bank only deducted 10% TDS, while your applicable slab is 20%.    |
|                                                                   |
| [Badge: AY 2026-27 • Major Head 0021 • Minor Head 300]            |
|                                                                   |
| Select UPI App to Pay Instantly:                                  |
|   [ GPay ]     [ PhonePe ]     [ Paytm ]     [ BHIM ]             |
|                                                                   |
| Post-Payment Auto-Verification State:                             |
| [✓] Payment Confirmed! BSR Code: 0510321 | Challan No: 48291      |
|     Auto-Added to Schedule IT of Your Return.                     |
+-------------------------------------------------------------------+
Phase 3 Complete 3-Tab Sitemap & AI Tax Copilot Hub (EN / हिन्दी / ગુજરાતી)
[Persistent 3-Tab Main Workspace (Phase 3 Complete State)]
  │
  ├── [Tab 1: Tax & Wealth Hub]
  │     ├── Live CPC Refund Tracker / Revised Return Trigger (Sec 139(5))
  │     ├── Foreign RSU & Schedule FA Compliance Shield (MNC Users)
  │     └── Quarterly Advance Tax & March LTCG Harvesting Radar
  │
  ├── [Tab 2: Smart Tax Vault]
  │     ├── Section A: Official Portal Artifacts (Prefill, 26AS, AIS, Challan 280)
  │     ├── Section B: Investment & Foreign Broker Statements (CAMS, Zerodha, E*TRADE, 1042-S)
  │     └── Section C: Salary Proofs, CPC Notices & AIS Dispute Receipts
  │
  └── [Tab 3: AI Tax Copilot & Privacy Manager (EN / हिंदी / ગુજરાતી)]
        ├── [Language Switcher Pill: English | हिंदी | ગુજરાતી]
        ├── Module A: Instant Tax Notice Scanner & Resolver (143(1) / 139(9) / AIS)
        ├── Module B: On-Device Conversational Tax Q&A (Grounded in Local Vault)
        ├── Module C: Next-FY Salary Restructuring & Regime Simulator
        └── Module D: RBI Account Aggregator Consent Manager & 1-Tap DPDP Vault Shredder

3. Exhaustive Multilingual Taxonomy: Official CBDT JSON Nodes to Friendly UI Labels

#
Official CBDT JSON Schema Node Path
Statutory Section / Schedule
Friendly UI Label (English en-IN)
Friendly UI Label (Hindi hi-IN)
Friendly UI Label (Gujarati gu-IN)
1
PersonalInfo.PAN
Sec 139A
Permanent Account Number (PAN)
स्थायी खाता संख्या (पैन)
પરમેનન્ટ એકાઉન્ટ નંબર (PAN)
2
PersonalInfo.AadhaarCardNo
Sec 139AA
Masked Aadhaar Status ([Aadhaar Redacted])
मास्क किया गया आधार
માસ્ક કરેલ આધાર
3
FilingStatus.ReturnFileSec
Sec 139(1) / (4) / (5)
Filing Type (On-Time / Belated / Revised)
रिटर्न का प्रकार
રિટર્નનો પ્રકાર
4
FilingStatus.NewTaxRegime
Sec 115BAC(1A)
Selected Tax Regime (New vs. Old)
चुनी गई कर व्यवस्था
પસંદ કરેલ ટેક્સ પ્રણાલી
5
ScheduleS.Salaries.GrossSalary
Sec 17(1)
Base Salary, Bonus & Commissions
मूल वेतन, बोनस और कमीशन
મૂળ પગાર, બોનસ અને કમિશન
6
ScheduleS.ValueOfPerquisites
Sec 17(2)
Company Perks (RSU Vesting, Car)
कंपनी सुविधाएं (RSU, कार)
કંપની તરફથી મળતી સુવિધાઓ
7
ScheduleS.AllwncExemptUs10.Sec10_13A
Sec 10(13A) Rule 2A
Tax-Free House Rent Allowance (HRA)
कर-मुक्त मकान किराया भत्ता
કરમુક્ત ઘરભાડા ભથ્થું (HRA)
8
ScheduleS.DeductionUs16ia
Sec 16(ia)
Flat Standard Salary Deduction
मानक वेतन कटौती (₹75k/₹50k)
પ્રમાણભૂત પગાર કપાત
9
ScheduleHP.AnnualLetableValue
Sec 23(1)
Rental Income from House Property
मकान संपत्ति से किराए की आय
મકાન મિલકતમાંથી ભાડાની આવક
10
ScheduleHP.InterestPayable
Sec 24(b)
Home Loan Interest Paid
होम लोन पर चुकाया गया ब्याज
હોમ લોન પર ચૂકવેલ વ્યાજ
11
ScheduleBP.PersumptiveInc44ADA
Sec 44ADA
Total Freelance / Consulting Receipts
कुल फ्रीलांस सकल प्राप्तियां
કુલ ફ્રીલાન્સ આવક
12
Schedule112A.Schedule112ADtls
Sec 112A
Long-Term Equity & Mutual Fund Gains
दीर्घकालिक शेयर और MF लाभ
લાંબા ગાળાનો શેર અને MF નફો
13
ScheduleCGFor23.ShortTermCapGain
Sec 111A
Short-Term Equity Gains (20%)
अल्पकालिक शेयर लाभ (20%)
ટૂંકા ગાળાનો શેર નફો (20%)
14
ScheduleCGFor23.SaleofLandBuild
Sec 112(1)(a)
Real Estate / Property Sale Profit
जमीन / प्रॉपर्टी बेचने से लाभ
મિલકત વેચાણ પર નફો
15
ScheduleCYLA & ScheduleBFLA
Sec 70, 71, 74
Investment Losses Offset Current Gains
चालू वर्ष के लाभ से समायोजित घाटा
ચાલુ વર્ષના નફા સામે સરભર ખોટ
16
ScheduleCFL
Sec 74
Losses Carried Forward (8 Years)
अगले 8 वर्षों के लिए आगे ले जाया घाटा
આગામી 8 વર્ષ માટે આગળ ખેંચાયેલ ખોટ
17
ScheduleOS.IntrstFrmSavingBank
Sec 56
Savings Bank Account Interest
बचत बैंक खातों से मिला ब्याज
બચત બેંક ખાતામાંથી વ્યાજ
18
ScheduleOS.IntrstFrmTermDeposit
Sec 56
Fixed Deposit (FD) Interest
फिक्स्ड डिपॉजिट (FD) ब्याज
ફિક્સ્ડ ડિપોઝિટ (FD) વ્યાજ
19
ScheduleOS.DividendGross
Sec 56(2)(i)
Share & Mutual Fund Dividends
लाभांश आय (Dividend)
શેર અને MF માંથી ડિવિડન્ડ
20
UsrDeductUndChapVIA.Section80C
Sec 80C
EPF, PPF, ELSS, Insurance (Max ₹1.5L)
80C निवेश (PF, PPF, ELSS, बीमा)
80C રોકાણ (PF, PPF, ELSS, વીમો)
21
UsrDeductUndChapVIA.Section80CCD2
Sec 80CCD(2)
Employer NPS (Tax-Free in Both Regimes)
नियोक्ता का एनपीएस योगदान
કંપનીનું NPS યોગદાન
22
UsrDeductUndChapVIA.Section80D
Sec 80D
Health Insurance & Medical Checkup
स्वास्थ्य बीमा (धारा 80D)
હેલ્થ ઇન્શ્યોરન્સ (કલમ 80D)
23
UsrDeductUndChapVIA.Section80TTA
Sec 80TTA / 80TTB
Tax-Free Bank Interest Deduction
कर-मुक्त बैंक ब्याज छूट
કરમુક્ત બેંક વ્યાજ કપાત
24
ScheduleFA.DtlsForeignEquity
Schedule FA
Foreign Company Shares / US RSUs (Jan–Dec)
विदेशी शेयर / RSU (जनवरी-दिसंबर)
વિદેશી શેર / RSU (જાન્યુ-ડિસે)
25
ScheduleTR1 & Form67
Sec 90 / Rule 128
Credit for Tax Deducted Abroad (FTC)
विदेश में कटे टैक्स की छूट (FTC)
વિદેશમાં કપાયેલા ટેક્સની ક્રેડિટ
26
TaxComputation.Rebate87A
Sec 87A
Zero-Tax Rebate & Marginal Relief
धारा 87A कर छूट और सीमांत राहत
કલમ 87A રિબેટ અને માર્જિનલ રિલીફ
27
Schedule80G.Don100Percent & Don50PercentApprReqd
Sec 80G(5D)
Charitable Donations (Cash > ₹2,000 Disallowed)
धर्मार्थ दान (धारा 80G — ₹2,000 से अधिक नकद अमान्य)
ચેરિટેબલ દાન (કલમ 80G — ₹2,000 થી વધુ રોકડ અમાન્ય)
28
ScheduleAL.ImmovableDetails & MovableAsset
Schedule AL
Assets & Liabilities (Mandatory if Income > ₹50L)
संपत्ति और देनदारियां (₹50 लाख से अधिक आय पर अनिवार्य)
મિલકત અને જવાબદારીઓ (₹50 લાખથી વધુ આવક પર ફરજિયાત)
29
ScheduleFSI.TotalIncomeOutsideIndia
Sec 90 / 91
Foreign Source Income (Dividends & Overseas Gains)
विदेशी स्रोत से कुल आय (लाभांश और लाभ)
વિદેશી સ્ત્રોતમાંથી કુલ આવક (ડિવિડન્ડ અને નફો)
30
PartB_TI_ITR_U.AmtPayable140B
Sec 139(8A) / 140B
Updated Return (ITR-U) Additional Tax (25% / 50%)
अपडेटेड रिटर्न (ITR-U) अतिरिक्त कर (धारा 140B)
અપડેટેડ રિટર્ન (ITR-U) વધારાનો ટેક્સ (કલમ 140B)


4. Accessibility (WCAG 2.1 AA), en-IN Lakh/Crore Number Formatting & FLAG_SECURE Screen Privacy
4.1 Indian Numbering System (en-IN Lakh/Crore Grouping & Tabular Monospace Typography)
All financial metrics across the application strictly comply with the Indian numbering format (₹XX,XX,XXX) using standard two-digit groupings beyond the first thousand. Numerical tabular layouts enforce monospace numeric glyphs via fontFeatureSettings = "tnum" to ensure decimal and comma alignment across comparison tables.
package com.zerotax.local.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object IndianCurrencyFormatter {
    private val indianLocale = Locale("en", "IN")
    private val symbols = DecimalFormatSymbols(indianLocale).apply {
        currencySymbol = "₹"
    }
    private val decimalFormat = DecimalFormat("##,##,##0", symbols)

    fun format(amount: Long, includePrefix: Boolean = true): String {
        val formatted = decimalFormat.format(amount)
        return if (includePrefix) "₹$formatted" else formatted
    }
}

4.2 WCAG 2.1 Level AA Accessibility, TalkBack Semantic Tree & Dynamic Font Scaling (sp)
The design adheres to WCAG 2.1 Level AA compliance:
• Contrast Ratios: Text contrast achieves ≥ 7.0:1 for statutory alerts and numeric values, and ≥ 4.5:1 for body copy across all dark and light surface modes.
• Touch Targets: All actionable elements, nudge card dismissals, and disambiguation pills maintain minimum dimensions of 48dp × 48dp.
• Semantic Audio Labels: TalkBack accessibility nodes provide full verbalization of Indian currency figures:
   - English (en-IN): "Rupees Twenty Four Lakh Fifty Thousand"
   - Hindi (hi-IN): "चौबीस लाख पचास हज़ार रुपये"
   - Gujarati (gu-IN): "ચોવીસ લાખ પચાસ હજાર રૂપિયા"
• Dynamic Typography: All layouts utilize scalable pixels (sp) supporting up to 200% Android OS display font enlargement without clipping or layout truncation.

4.3 Hardware-Backed Screen Privacy (WindowManager.LayoutParams.FLAG_SECURE & Recents Blur)
To protect confidential taxpayer financial records from screen scrapers, malware, and operating system multitasking snapshots, the host ComponentActivity enables FLAG_SECURE across all interactive screens. This blocks screen captures and blanks out preview thumbnails in Android Recents.
// Enforce hardware-backed screen protection
window.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)

// Scoped 5-minute ephemeral override for official ITR-V export
fun allowTemporaryExportSnapshot(lifecycleScope: CoroutineScope) {
    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    lifecycleScope.launch {
        delay(300_000L) // 5 minutes
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}

5. CA & Legal Counsel Statutory UX Guardrail Specification
• Tap 1 Identity & DPDP Section 6 Gate: Blocks non-individual PANs (4th char != 'P') immediately at keyboard input to prevent Section 139(9) defective filings, and records an immutable SHA-256 hash of the localized DPDP Act Section 6 consent notice (en-IN, hi-IN, gu-IN) in user_legal_consents_and_overrides before initializing zero_tax_vault.db.
• Tap 2 Post-AI Math Sanity Firewall & Visual Cropped Disambiguation: Never allows unverified OCR or AI-extracted numbers into :core:tax-engine. Whenever |Gross_Salary - Exemptions_u10 - Deductions_u16 - Net_Salary| > ₹5, the UI presents the exact cropped PDF bounding box alongside a 1-tap disambiguation selector.
• Tap 3 Exception Nudge Cards as Statutory Penalty Shields:
   - Employer TDS Protection Shield: Caps Schedule TDS1 strictly at Form 26AS Part A deposited amounts to prevent automated Section 143(1)(a) short-credit tax demands.
   - Rule 2A Landlord PAN Gate: Automatically sets Sec 10(13A) HRA exemption to ₹0 when annual rent exceeds ₹1,00,000 without a valid 10-character Landlord PAN, protecting against Section 270A(9) 200% misreporting penalties.
   - Section 44ADA 50% Presumptive Floor: Locks the presumptive profit slider to [50%..100%] unless the taxpayer explicitly acknowledges mandatory Section 44AB Chartered Accountant tax audit requirements.
• Tap 4 Pre-Submission Category 'A' Defect Blocker & Section 140 Verification Gate: Verifies all CBDT Category 'A' rules (TDS1 <= GrossSalary, TotalChapVIA <= GTI, valid 11-char IFSC with UseForRefund = "true", and AadhaarCardNo = "[Aadhaar Redacted]") and requires explicit Section 140 solemn verification before enabling 1-Tap Aadhaar OTP e-Verification.

6. End-to-End Navigation State Machine & Statutory Gate Invariants
6.1 Deterministic Transition Guards Across the 4-Tap Linear Spine
• Transition Tap 1 -> Tap 2 (Identity & Consent Gate): Blocked until PAN matches ^[A-Z]{3}P[A-Z][0-9]{4}[A-Z]$ (Individual status 'P'), DOB is valid, and DPDP_SECTION_6_CONSENT is written to Table 13 (user_legal_consents_and_overrides) with the SHA-256 hash of the active locale (en-IN, hi-IN, or gu-IN).
• Transition Tap 2 -> Tap 3 (Ingestion & Math Sanity Gate): Blocked if any imported Form 16 Part B fails |Gross - Sec10 - Sec16 - Net| <= ₹5 until the user resolves the OneTapPdfDisambiguationCard with the cropped PDF region bitmap.
• Transition Tap 3 -> Tap 4 (Exception Nudge & Statutory Shield Gate): Automatically enforces the Form 26AS Part A TDS cap (Schedule TDS1), the Rule 2A Landlord PAN gate for annual rent > ₹1,00,000, the Section 44ADA >= 50% presumptive profit floor, and logs any downward income modification via Section270AStatutoryOverrideDialog or foreign asset holding via BlackMoneyActForeignAssetModal.
• Transition Tap 4 -> Post-Filing 3-Tab Pro Workspace (Category 'A' Defect & Section 140 Gate): Enabled only after CbdtCategoryADefectGate passes all pre-submission checks (BalTaxPayable == 0 via Schedule IT Challan 280 if tax was due, valid 11-character refund IFSC, and AadhaarCardNo == "[Aadhaar Redacted]") and the taxpayer checks the Section 140 verification declaration.

6.2 Smart Tax Toolkit Value-Add Architecture (HRA, Advance Tax, LTCG Harvesting, Statement Exporter)
• HRA Exemption Maximizer & Rent Receipts Generator (Rule 2A): Real-time least-of-three calculation (Actual HRA, Rent - 10% Basic, 50%/40% Metro cap) with automated 12-month Rent Receipt package generator formatted with revenue stamp layout for employer HR submission.
• Advance Tax Radar & Instant Challan 280 UPI Assistant (Sections 208, 234B, 234C): Continuous tracking of net tax payable above the ₹10,000 threshold, quarterly calendar milestone countdowns (15%, 45%, 75%, 100%), interest penalty avoidance calculator, and pre-filled Challan 280 (Major Head 0021, Minor Head 100/300) with NPCI UPI intent generator.
• March 31 LTCG Tax-Harvesting Radar (Section 112A): Visual gauge monitoring the annual ₹1,25,000 tax-free long-term capital gains headroom, automated tax-harvesting profit booking recommendations before March 31, and STCG short-term loss harvesting against realized gains.
• Bank-Ready "Statement of Total Income & Tax Computation" Exporter: Formats formal bank-grade ITR summaries with breakdown of Gross Salary, Chapter VI-A deductions, Section 87A rebate, TDS, and refund due, stamped with a tamper-evident SHA-256 seal for Home Loan, Car Loan, and Visa documentation.

6.3 Tax Intelligence & Visual Analytics Architecture (Income Flow, 3-Way Radar, Calendar & Dossier)
• Income Flow Waterfall Visualizer: Provides complete visual clarity on how Gross Salary transitions into Net Taxable Income through statutory deductions (Standard Deduction u/s 16(ia), Chapter VI-A relief), progressive slab tax, Section 87A rebate (100% tax relief up to ₹12.75L salary), and prepaid TDS resulting in net direct bank refund.
• 3-Way Discrepancy Triangulation Radar (Form 16 vs 26AS vs AIS): Side-by-side comparison across employer Form 16, TRACES Form 26AS, and ITD AIS/TIS with 1-tap auto-healing to eliminate Section 143(1)(a) tax demand notices.
• Statutory Tax Calendar & Compliance Reminders (AY 2026-27): Tracks all 6 statutory deadlines with dynamic countdown timers (July 31 filing deadline, quarterly advance tax milestones, December 31 revised return deadline, and March 31 tax-saving/harvesting cutoff) with explicit penalty warnings (Sections 234A/B/C/F).
• Multi-Year Historical ITR Dossier: 3-Year comparative record of filed income, growth trajectories, and CPC refunds formatted with digital verification stamps for bank loans and visa approvals.
