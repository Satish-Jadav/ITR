# ZeroTax Master Figma Design Specification & UI Architecture

---

## 1. Global Design Tokens & Auto-Layout Rules

### Color System (Hex & Semantic Application)
| Token Name | Hex Code | Usage / Mapping |
| :--- | :--- | :--- |
| `CanvasWarm` | `#F4F6F5` | Global background canvas, bottom scaffold neutral fill |
| `PureWhite` | `#FFFFFF` | Primary card background, bottom sheet surface, modal dialogs |
| `Slate950` | `#0F1513` | Deep dark mode canvas / contrast headers |
| `Slate900` | `#151C19` | Primary typography, headers, high-contrast labels |
| `Slate850` | `#1E2623` | Dark card background, dark banner surfaces |
| `Slate800` | `#28322E` | Container stroke on dark, secondary dark card fill |
| `Slate700` | `#434E49` | Inactive stepper lines, disabled typography on dark |
| `Slate600` | `#6B7772` | Secondary typography, captions, metadata subtitle |
| `Slate500` | `#8E9592` | Input field placeholders, timestamp labels |
| `Slate400` | `#A5AEA9` | Inactive icon fills, secondary borders |
| `Slate300` | `#D4DAD7` | Standard input borders, card strokes (1px solid) |
| `Slate200` | `#E5EAE7` | Horizontal ledger dividers, subtle separators |
| `Slate100` | `#ECEFE8` | Light surface container, disabled button fill |
| `ForestGreenPrimary` | `#184E42` | Primary brand accent, primary CTA button fill, active ticks |
| `ForestGreenDark` | `#133E35` | Dark brand header fill, pressed button state |
| `ForestGreenAccent` | `#1E6152` | Interactive hover/accent, focused input stroke |
| `SageMintPill` | `#E4F0EB` | Verified badge fill (Text: `#184E42`) |
| `SageMintLight` | `#C0D8CF` | Progress bar indicators, chart series A |
| `AmberPillBg` | `#FEF5E7` | Warning / Review pill fill (Text: `#B46E13`) |
| `RosePillBg` | `#FCEBEB` | Discrepancy / Overdue pill fill (Text: `#D94444`) |
| `NavPillActive` | `#DEE5F5` | Active tab highlight pill in bottom navigation |

### Typography Scale (Font: Inter or SF Pro)
- **Display 1**: 28pt / Line Height 34pt / Bold (700) — Main Hero Numbers & Splash
- **Headline 1**: 22pt / Line Height 28pt / SemiBold (600) — Screen Titles
- **Headline 2**: 18pt / Line Height 24pt / SemiBold (600) — Card Section Headers
- **Subtitle 1**: 15pt / Line Height 20pt / Medium (500) — List Item Titles, Key Values
- **Body 1**: 14pt / Line Height 20pt / Regular (400) — Descriptions, Conversational Text
- **Body 2**: 13pt / Line Height 18pt / Regular (400) & Medium (500) — Input Field Content
- **Caption**: 11pt / Line Height 14pt / Medium (500) — Badges, Statutory Tags, Timestamps
- **Micro**: 9pt / Line Height 12pt / Bold (700) Uppercase — Section Tracking Labels

### Spacing, Grid & Corner Radii
- **Base Grid**: 4pt / 8pt spatial system
- **Screen Margins**: Left/Right 16pt, Top Inset (Safe Area) 44pt, Bottom Inset 34pt
- **Auto-Layout Spacing**:
  - Item Gaps: Tight = 4pt, Standard = 8pt, Medium = 12pt, Generous = 16pt
  - Card Internal Padding: 16pt (Compact) or 20pt (Standard)
  - Screen Stack Gap: 16pt to 24pt between distinct card groups
- **Corner Radii**:
  - Micro / Badges: 6pt – 8pt
  - Controls / Inputs / Buttons: 12pt
  - Surface Cards: 18pt – 20pt
  - Modals & Bottom Sheets: Top Left/Right 28pt

---

## 2. Component Variant Specifications (Figma Variants & Auto-Layout)

### A. Headers & Navigation
1. **`EnterpriseTopHeader`**:
   - Layout: Auto-layout Horizontal, Width: Fill Container (361pt), Height: 48pt, Space-between, Padding: 0 4pt.
   - Left Element: `ZeroTaxBrandLogo` (28×28pt Forest Green shield with dual-lock motif) + "ZeroTax" (16pt SemiBold `#151C19`).
   - Right Element: Auto-layout Horizontal (Gap 8pt):
     - `StatutoryBadge`: Height 24pt, Pill 12pt, Fill `#E4F0EB`, Text: "AY 2026-27" (11pt `#184E42`).
     - AES Lock Icon: 16×16pt `#184E42` in circle container.
     - Profile Avatar: 32×32pt Circle `#28322E`, white user initial "SJ".
2. **`ZeroTaxFourTapSpineScaffold` (Bottom Navigation Bar)**:
   - Layout: Auto-layout Horizontal, Width: 393pt, Height: 76pt (includes safe area), Fill: `#FFFFFF`, Border Top: 1px `#E5EAE7`.
   - 4 Tabs (Width: 25% each):
     - Tab 1: Home (`Home` icon + label "Filing")
     - Tab 2: Copilot (`Chat` icon + label "Assistant")
     - Tab 3: Vault (`FolderLock` icon + label "Documents")
     - Tab 4: Toolkit (`Calculator` icon + label "Calculators")
   - Active Variant: Selected icon wrapped in `NavPillActive` pill (Height 32pt, Width 60pt, Radius 16pt), text `#151C19` SemiBold. Inactive: `#8E9592`.

### B. Buttons & Interactive Controls
1. **`ZeroTaxPrimaryButton`**:
   - Variants: `State = Default | Pressed | Disabled | Loading`.
   - Layout: Width: Fill (361pt) or Hug, Height: 52pt, Radius: 12pt, Alignment: Center.
   - Fills: Default `#184E42`, Pressed `#133E35`, Disabled `#ECEFE8`.
   - Typography: 15pt SemiBold, White (Disabled: `#A5AEA9`).
   - Optional Slots: Leading Icon (20×20pt), Trailing Arrow / Lock (20×20pt).
2. **`ZeroTaxOutlinedButton`**:
   - Layout: Height: 48pt, Radius: 12pt, Stroke: 1.5px `#D4DAD7`, Fill: Transparent.
   - Typography: 14pt SemiBold `#151C19`.
3. **`ZeroTaxSecondaryButton`**:
   - Layout: Height: 44pt, Radius: 12pt, Fill: `#E4F0EB`, Text: 14pt Medium `#184E42`.

### C. Inputs & Statutory Selectors
1. **`SmartIndividualPanField`**:
   - Layout: Width: Fill, Height: 56pt, Radius: 12pt, Fill: `#FFFFFF`, Stroke: 1px `#D4DAD7` (Active: 2px `#184E42`).
   - Padding: Horizontal 16pt, Vertical 8pt.
   - Left: Label "PERMANENT ACCOUNT NUMBER (PAN)" floating micro 10pt `#6B7772` + Formatted input "ABCDE1234F" (16pt SemiBold `#151C19`, Letter-spacing 2pt).
   - Right: Verified Pill: Height 24pt, Fill `#E4F0EB`, Text: "VALID" with checkmark icon.
2. **`ZeroTaxAmountField`**:
   - Layout: Width: Fill, Height: 60pt, Radius: 12pt, Fill: `#FFFFFF`, Stroke: 1px `#D4DAD7`.
   - Left: Currency Prefix `₹` (20pt Bold `#184E42`).
   - Center: Value "12,50,000" (20pt Bold `#151C19`).
   - Right: "INR" label (12pt `#8E9592`).
3. **`CitizenCategoryAgeSelector`**:
   - Layout: Width: Fill, Height: 44pt, Radius: 10pt, Fill: `#ECEFE8`, Padding: 4pt, Auto-layout Horizontal (Equal 3 segments).
   - Segments: `General (<60)`, `Senior (60-80)`, `Super Senior (>80)`.
   - Active Segment: Fill `#FFFFFF`, Shadow Y=2 Blur=4 Opacity=0.08, Text 13pt SemiBold `#151C19`.
4. **`DiscreteStatutoryStepper`**:
   - Layout: Auto-layout Horizontal, Width: Fill, Height: 40pt, Align: Center, Gap: 8pt.
   - 4 Step Nodes: Node 1 (Checkmark `#184E42`), Line (`#184E42`), Node 2 (Filled `#184E42`), Line (`#D4DAD7`), Node 3 (Circle `#D4DAD7`), Line, Node 4.

### D. Cards & Data Components
1. **`RegimeComparisonInstitutionalCard`**:
   - Layout: Auto-layout Vertical, Width: Fill, Radius: 20pt, Fill: `#FFFFFF`, Stroke: 1px `#D4DAD7`, Padding: 20pt, Gap: 16pt.
   - Header: Horizontal: Title "Regime Comparison (Sec 115BAC)" + Badge "AUTO-OPTIMIZED".
   - Split Columns (Auto-layout Horizontal, Gap 12pt):
     - Col 1 (Old Regime): Width 50%, Fill `#F4F6F5`, Radius 12pt, Padding 12pt. Label "Old Regime", Tax "₹ 1,42,800", Deductions "Claimed ₹2.5L".
     - Col 2 (New Regime - Selected): Width 50%, Fill `#E4F0EB`, Radius 12pt, Stroke 1.5px `#184E42`, Padding 12pt. Label "New Regime (Default)", Tax "₹ 1,18,200", Badge "SAVE ₹ 24,600".
2. **`ZeroTaxStatCard`**:
   - Layout: Width: Fill, Radius: 16pt, Fill: `#FFFFFF`, Stroke: 1px `#D4DAD7`, Padding: 16pt, Gap 8pt.
   - Top Row: Icon Container (36×36pt `#E4F0EB` with `#184E42` icon) + Label "Net Tax Payable" (13pt `#6B7772`).
   - Middle: Big Number "₹ 48,250" (24pt Bold `#151C19`).
   - Bottom: Status Pill "Due by 31st July" (Amber pill `#FEF5E7`, text `#B46E13`).
3. **`EnterpriseLedgerRow`**:
   - Layout: Auto-layout Horizontal, Width: Fill, Height: 48pt, Space-between, Align: Center, Border Bottom: 1px `#E5EAE7`.
   - Left: Row Title "Section 80C (PPF, ELSS, EPF)" (14pt Medium `#151C19`) + Subtitle "Max statutory cap: ₹1,50,000" (11pt `#6B7772`).
   - Right: Claimed Amount "₹ 1,50,000" (14pt SemiBold `#184E42`) + Green Check icon.
4. **`HardwareVaultStatusCard`**:
   - Layout: Auto-layout Horizontal, Width: Fill, Radius: 14pt, Fill: `#1E2623`, Padding: 16pt, Align: Center, Gap: 12pt.
   - Left: 32×32pt Shield icon `#C0D8CF`.
   - Center: Title "Hardware Keystore Secured" (14pt SemiBold `#FFFFFF`) + "Zero plain-text cloud transmission" (12pt `#8E9592`).
   - Right: Biometric toggle switch / Active status dot.

---

## 3. Screen-by-Screen Exhaustive UI Blueprints (393 × 852 pt)

```
================================================================================
SCREEN 1: SPLASH SCREEN (SplashScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #0F1513 (Dark Sovereign Theme)]
├─ [Safe Area Top: 44pt]
├─ [Center Container: Auto-layout Vertical, Align: Center, Gap: 20pt, Y: 280pt]
│  ├─ ZeroTaxBrandLogo: 84×84pt, Forest Green Shield (#184E42) with Gold Core
│  ├─ App Title: "ZeroTax" (Display 28pt Bold, Fill: #FFFFFF)
│  ├─ Subtitle: "Institutional Tax Intelligence Platform" (13pt Medium, Fill: #8E9592)
│  └─ Animated Circular Progress: 32×32pt, Stroke: 3px #184E42
└─ [Bottom Anchor: Auto-layout Vertical, Align: Center, Gap: 12pt, Y: 720pt]
   ├─ Keystore Status: "Initializing On-Device Hardware Keystore..." (11pt #6B7772)
   └─ Trust Badges Row: Auto-layout Horizontal, Gap 8pt
      ├─ Pill Badge: "ITD Direct API Certified" (#1E2623, Text #C0D8CF)
      ├─ Pill Badge: "DPDP 2023 Compliant" (#1E2623, Text #C0D8CF)
      └─ Pill Badge: "AES-256 HSM" (#1E2623, Text #C0D8CF)

================================================================================
SCREEN 2: AUTHENTICATION & BIOMETRIC GATE (AuthenticationScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5 (CanvasWarm)]
├─ [Top Bar: Height 56pt, Horizontal, Space-between, Padding: 0 16pt]
│  ├─ Back Arrow Button (24×24pt #151C19)
│  └─ Security Shield Badge: "256-Bit Encrypted Session" (#E4F0EB, #184E42)
├─ [Header Section: Gap 6pt, Margin Top: 16pt, Padding: 0 16pt]
│  ├─ "Sign In to Your Dossier" (Headline 22pt SemiBold #151C19)
│  └─ "Enter PAN or registered mobile number to proceed" (Body 14pt #6B7772)
├─ [Segmented Tab Control: Height 44pt, Margin Top: 20pt, Padding: 0 16pt]
│  └─ Tabs: [Active: "PAN ID"] | [Inactive: "Mobile Number"]
├─ [Form Container: Margin Top: 20pt, Padding: 0 16pt, Gap: 16pt]
│  ├─ SmartIndividualPanField: "ABCDE1234F" (Active Stroke #184E42, Verified Check)
│  ├─ OTP Section:
│  │  ├─ Label: "Enter 6-digit Aadhaar / ITD OTP" (13pt Medium #151C19)
│  │  ├─ 6 Discrete OTP Input Boxes (Width 48pt, Height 54pt each, Radius 10pt)
│  │  └─ Timer Row: "Resend code in 00:45" (#6B7772) + "Resend via SMS" (Disabled)
│  ├─ ZeroTaxPrimaryButton: "Verify & Decrypt Dossier →" (Height 52pt #184E42)
│  └─ ZeroTaxOutlinedButton: Leading FaceID icon + "Authenticate with Biometrics"
└─ [Footer Trust Note: Align Center, Margin Top: 32pt]
   └─ "Government registered entities only. Zero external credential caching."

================================================================================
SCREEN 3: DATA INGESTION & HYDRATION RADAR (AutomatedOnboardingScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ EnterpriseTopHeader: Logo + "AY 2026-27" + Avatar
├─ DiscreteStatutoryStepper: Step 1 active ("1. Ingestion" -> "2. Compute" -> "3. Audit" -> "4. File")
├─ Screen Title Section: "Automated Data Ingestion Radar"
├─ [Ingestion Card 1: Account Aggregator (AA)]
│  ├─ Auto-layout Vertical, Fill: #FFFFFF, Radius: 18pt, Padding: 16pt, Stroke: 1px #D4DAD7
│  ├─ Top Row: AA Logo + "Reserve Bank Account Aggregator" + Status: "CONNECTED" (Mint)
│  ├─ Bank Pill Row: HDFC Bank (**** 4812) • ICICI Bank (**** 9021) • SBI
│  └─ Data Found: "142 Interest credits & dividends detected" (13pt #184E42)
├─ [Ingestion Card 2: AIS & TIS Official Sync]
│  ├─ Auto-layout Vertical, Fill: #FFFFFF, Radius: 18pt, Padding: 16pt, Stroke: 1px #D4DAD7
│  ├─ Top Row: Government Emblem + "Annual Information Statement" + Status: "SYNCED"
│  ├─ Breakdown: Salaries: ₹18.5L • Securities: ₹4.2L • High Value Cash: ₹0
│  └─ Statutory Match: "100% matched with 26AS Tax Credits" (Mint Pill)
└─ Bottom Sticky CTA: ZeroTaxPrimaryButton: "Generate Unified Pre-Filing Dossier"

================================================================================
SCREEN 4: CONVERSATIONAL TAX COPILOT (ConversationalFilingChatScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ Header: Title "ZeroTax Copilot", Green Active Dot, Encryption Shield
├─ [Chat Scroll Container: Auto-layout Vertical, Gap: 16pt, Padding: 16pt]
│  ├─ System Timestamp: "Today, 10:14 AM • Encrypted Session"
│  ├─ Assistant Bubble (Left, Fill: #FFFFFF, Radius: 18pt, Stroke: 1px #D4DAD7, Padding: 14pt):
│  │  ├─ "I analyzed your Form 16 and AIS records. We found ₹1,50,000 eligible for Section 80C, but you have ₹25,000 unused in 80D (Health Insurance)."
│  │  └─ Inline Interactive Card (Fill: #E4F0EB, Radius: 12pt, Padding: 12pt):
│  │     ├─ Title: "Section 80D Health Insurance Deduction"
│  │     ├─ Input: ZeroTaxAmountField ("₹ 25,000")
│  │     └─ Action: ZeroTaxPrimaryButton ("Claim ₹25,000 Relief")
│  ├─ User Bubble (Right, Fill: #184E42, Radius: 18pt, Padding: 14pt, Text: #FFFFFF):
│  │  └─ "Please add ₹25,000 for family health insurance premium."
│  └─ Assistant Response Bubble:
│     └─ "Applied. Your taxable income is reduced to ₹14,25,000. Potential tax saved: ₹7,800."
├─ [Quick Reply Suggestion Chips: Horizontal Scroll, Gap 8pt, Padding: 0 16pt]
│  ├─ Chip 1: "Compare Old vs New Regime"
│  ├─ Chip 2: "Upload Rent Receipts for HRA"
│  └─ Chip 3: "Add Capital Losses"
└─ [Bottom Input Bar: Height 64pt, Fill: #FFFFFF, Stroke Top: 1px #E5EAE7]
   ├─ Plus Action Icon (Attachment / Receipt OCR)
   ├─ Input Placeholder: "Ask your copilot anything about deductions..."
   └─ Send Button: 36×36pt Circle #184E42 with White Arrow

================================================================================
SCREEN 5: TAX VISUALIZER & REGIME ANALYTICS (TaxVisualizerAndAnalyticsScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ EnterpriseTopHeader
├─ Screen Title: "Tax Visualizer & Analytics"
├─ RegimeComparisonInstitutionalCard (Highlighted: New Regime saves ₹24,600)
├─ [Donut Chart Visualization Card: Fill: #FFFFFF, Radius: 20pt, Padding: 20pt]
│  ├─ Donut Chart: 160×160pt (Salary 68% #184E42, Capital Gains 18% #C0D8CF, Others 14% #28322E)
│  ├─ Center Metric: "Total Income" / "₹ 24,50,000"
│  └─ Chart Legend: 3 columns with section numbers and exact figures
├─ [Summary Metric Rows (EnterpriseLedgerRow)]
│  ├─ Gross Total Income: ₹ 24,50,000
│  ├─ Total Chapter VI-A Exemptions: - ₹ 2,25,000
│  ├─ Net Taxable Income: ₹ 22,25,000
│  └─ Final Tax Liability (incl. 4% Health & Education Cess): ₹ 2,86,400
└─ Bottom Fixed Bar: ZeroTaxPrimaryButton: "Lock Regime & Review Audit"

================================================================================
SCREEN 6: SMART DEDUCTIONS ADVISOR (SmartDeductionsAdvisorScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ EnterpriseTopHeader
├─ [Hero Card: Fill: #184E42, Text: #FFFFFF, Radius: 20pt, Padding: 20pt]
│  ├─ Label: "TOTAL OPTIMIZED TAX SAVINGS" (11pt #C0D8CF)
│  ├─ Value: "₹ 54,600" (Display 28pt Bold)
│  └─ Subtitle: "Based on 4 statutory optimization rules applied"
├─ [Deduction Progress Trackers: Fill: #FFFFFF, Radius: 20pt, Padding: 16pt]
│  ├─ Section 80C: Progress Bar 100% (Mint #184E42) — ₹1,50,000 / ₹1,50,000 (Maxed Out)
│  ├─ Section 80D: Progress Bar 50% (Amber #FEF5E7) — ₹25,000 / ₹50,000 (₹25,000 available)
│  ├─ Section 80CCD(1B) NPS: Progress Bar 0% (Slate) — ₹0 / ₹50,000 (Unclaimed)
│  └─ Section 24(b) Home Loan: Progress Bar 100% — ₹2,00,000 Claimed
├─ [Action Discovery Card (ZeroTaxActionCard)]
│  ├─ Left: NPS Shield icon
│  ├─ Title: "Invest ₹50,000 in Tier-1 NPS"
│  ├─ Subtitle: "Save an additional ₹15,600 in 30% slab under Section 80CCD(1B)"
│  └─ Right: "Claim Now →" Link
└─ Sticky Bottom Button: ZeroTaxPrimaryButton: "Confirm Deductions & Proceed"

================================================================================
SCREEN 7: ENCRYPTED DOCUMENT VAULT (DocumentVaultScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ EnterpriseTopHeader
├─ HardwareVaultStatusCard: "Hardware Keystore Active • Biometric Guard ON"
├─ [Search & Filter Row: Height 44pt, Gap 8pt, Padding: 0 16pt]
│  ├─ Search Input: "Search forms, tax slips, capital gains..."
│  └─ Filter Icon Button (44×44pt #FFFFFF, Radius 10pt)
├─ [Document Grid (2×2 Cards): Gap 12pt, Padding: 0 16pt]
│  ├─ Card 1: Form 16 (Part A & B) • "Parsed" (Mint) • Employer: Infosys Ltd
│  ├─ Card 2: AIS Dossier 2026-27 • "Verified" (Mint) • 48 Transactions
│  ├─ Card 3: Capital Gains P&L • "Zerodha Broking" • Net Gain: ₹1,42,000
│  └─ Card 4: Home Loan Certificate • "HDFC Ltd" • Interest: ₹1.85L
├─ [Upload Banner: Fill: #FFFFFF, Radius: 16pt, Border: 1.5px Dashed #D4DAD7]
│  └─ Drag & Drop / Tap to Upload: "PDF, scanned image, or password-protected statement"
└─ Bottom Navigation: ZeroTaxFourTapSpineScaffold (Vault Tab Active)

================================================================================
SCREEN 8: SMART TAX TOOLKIT & CALCULATORS (SmartTaxToolkitScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ EnterpriseTopHeader
├─ Screen Title: "Statutory Tax Toolkit"
├─ [Toolkit Grid: 2 Columns, Gap 12pt, Padding: 0 16pt]
│  ├─ Tile 1: Advance Tax Calculator (Sec 208) • Due Dates: 15 Jun, 15 Sep, 15 Dec, 15 Mar
│  ├─ Tile 2: HRA Exemption Engine (Rule 2A) • Metro 50% vs Non-Metro 40% comparator
│  ├─ Tile 3: Capital Gains Indexation • Cost Inflation Index (CII) base multiplier
│  └─ Tile 4: Penal Interest Estimator • Calculate Sec 234A, 234B, and 234C interest
├─ [Featured Tool Card: "HRA Quick Exemption Calculator"]
│  ├─ Basic Salary Input: ₹ 80,000 / month
│  ├─ HRA Received: ₹ 35,000 / month
│  ├─ Actual Rent Paid: ₹ 30,000 / month (Metro: Mumbai)
│  └─ Computed Tax-Free HRA: "₹ 22,000 / month exempt" (Mint Pill)
└─ Bottom Navigation: ZeroTaxFourTapSpineScaffold (Toolkit Tab Active)

================================================================================
SCREEN 9: OFFICIAL ITD PORTAL BRIDGE (PortalAssistantScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ Top Bar: "Income Tax Department Portal Assistant" + Official Seal
├─ [Session Card: Fill: #1E2623, Radius: 18pt, Text: #FFFFFF, Padding: 16pt]
│  ├─ Status: "Live Session Connected to e-Filing 2.0" (Green Pulsing Dot)
│  ├─ PAN: "ABCDE1234F" • Aadhaar Link: Verified
│  └─ Countdown: "Session expires in 08:32"
├─ [Pre-Filing Validation Checklist (4 Cards)]:
│  ├─ Row 1: Validated Bank Account for Refund (HDFC A/C **8912 - Pre-validated)
│  ├─ Row 2: Outstanding Tax Demand (No pending demands u/s 156)
│  ├─ Row 3: E-Filing Form Schema Validation (JSON Schema v1.4 Valid)
│  └─ Row 4: Digital Verification Method (Aadhaar OTP / EVC Ready)
└─ Bottom Actions:
   ├─ ZeroTaxPrimaryButton: "Transmit & E-Verify Return via Aadhaar OTP"
   └─ ZeroTaxOutlinedButton: "Download Official ITR JSON File"

================================================================================
SCREEN 10: PRESUMPTIVE BUSINESS AUDIT SUITE (SmallBusinessAuditSuiteScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ EnterpriseTopHeader
├─ Header: "Section 44AD / 44ADA Presumptive Business Suite"
├─ [Turnover Input Section: Fill: #FFFFFF, Radius: 18pt, Padding: 16pt]
│  ├─ Label: "Gross Annual Receipts / Turnover"
│  ├─ ZeroTaxAmountField: "₹ 65,00,000"
│  ├─ Digital vs Cash Split Slider:
│  │  ├─ Digital (Bank/UPI): 80% (Presumptive Rate: 6% = ₹3,12,000)
│  │  └─ Cash: 20% (Presumptive Rate: 8% = ₹1,04,000)
│  └─ Total Declared Presumptive Profit: "₹ 4,16,000" (6.4% of turnover)
├─ [Audit Immunity Banner: Fill: #E4F0EB, Radius: 12pt, Padding: 14pt]
│  └─ "Tax Audit Exemption Active: You are exempt from maintaining books of accounts u/s 44AA and tax audit u/s 44AB."
└─ Bottom Action: ZeroTaxPrimaryButton: "Save 44AD Schedule & Continue"

================================================================================
SCREEN 11: DUAL-REGION NRI COMPLIANCE (DualRegionNriComplianceScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ EnterpriseTopHeader
├─ Screen Title: "NRI Compliance & Global Assets"
├─ [Residency Test Wizard Card: Fill: #FFFFFF, Radius: 18pt, Padding: 16pt]
│  ├─ Question: "Days spent in India during FY 2025-26?"
│  ├─ Stepper Counter: [ - ]  94 Days  [ + ]
│  ├─ Statutory Outcome: "NON-RESIDENT (NR) for tax purposes" (Mint Pill)
│  └─ Note: "Global income outside India is not taxable in India."
├─ [Schedule FA (Foreign Assets Disclosure) Accordion]
│  ├─ Item 1: Foreign Depository Accounts (US Brokerage - Charles Schwab)
│  ├─ Item 2: Overseas Equity & Debt Interest (ESPP / RSUs)
│  └─ Status: "All foreign assets reported as per Black Money Act requirements"
├─ [DTAA Relief (Form 67) Card]
│  └─ Foreign Tax Credit (FTC) Claimed: "$ 3,400 (₹ 2,82,200) under Article 25"
└─ Bottom Action: ZeroTaxPrimaryButton: "Generate Schedule FA & Form 67"

================================================================================
SCREEN 12: CORPORATE & TRUST AUDIT SUITE (CorporateAndTrustSuiteScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ [Entity Selector Dropdown: Height 52pt, Fill: #FFFFFF, Stroke: 1px #D4DAD7]
│  └─ "ZeroTax Fintech Private Limited (CIN: U72200MH2024PTC123456)"
├─ [ITR-6 Statutory Module List: Gap 12pt]
│  ├─ Card 1: Balance Sheet & P&L Schedule (IND-AS Compliant) • Ready
│  ├─ Card 2: Minimum Alternate Tax (MAT - Sec 115JB) • Computed: ₹4,12,000
│  ├─ Card 3: Transfer Pricing (Sec 92E / Form 3CEB) • "Not Applicable"
│  └─ Card 4: Corporate Dividend & Buyback Tax Assessment • "Verified"
├─ [Digital Signature Certificate (DSC) Card: Fill: #1E2623, Text: #FFFFFF]
│  ├─ DSC Status: "Class-3 USB Token Detected" (Token Owner: Satish Jadav, Director)
│  └─ Valid Until: "18 December 2027"
└─ Bottom Action: ZeroTaxPrimaryButton: "Sign with Class-3 DSC & Transmit"

================================================================================
SCREEN 13: CA & AUDITOR COMMAND CENTER (CaAuditorPortalScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ Top Header: "CA Auditor Command Portal" • "ICAI Reg: 049218"
├─ [Roster Metrics: 3 Stat Cards in Row]
│  ├─ Card 1: "42 Assigned"
│  ├─ Card 2: "3 Discrepancies" (Rose Text)
│  └─ Card 3: "39 Ready to File" (Mint Text)
├─ [Client Audit Queue: Vertical List, Gap 10pt]
│  ├─ Client 1: "Dr. Arvind Patel" (ITR-3) • Tax: ₹3.4L • Status: "AIS Mismatch ₹45,000" (Rose Pill)
│  ├─ Client 2: "TechLogix Solutions LLP" (ITR-5) • Tax: ₹8.2L • Status: "Ready for DSC" (Mint Pill)
│  └─ Client 3: "Pooja Singhania" (ITR-1) • Tax: ₹64,000 • Status: "Verified" (Mint Pill)
├─ [Side-by-Side Review Drawer Preview (Opened for Client 1)]
│  ├─ Left: Client Form 16 TDS = ₹ 3,45,000
│  ├─ Right: ITD 26AS TDS = ₹ 3,00,000 (Mismatch: -₹45,000)
│  └─ Action: "Send Request to Deductor for Revised TDS Return"
└─ Bottom Floating Action: "Bulk E-Sign & Transmit (39 Verified Returns) →"

================================================================================
SCREEN 14: TAX NOTICE DEFENSE ASSISTANT (TaxNoticeDefenseAssistantScreen.kt)
================================================================================
[Frame: 393 × 852 pt, Fill: #F4F6F5]
├─ EnterpriseTopHeader
├─ Screen Title: "Notice Defense & Dispute Resolution"
├─ [Notice Header Card: Fill: #FFFFFF, Radius: 18pt, Padding: 16pt, Stroke: 1px #D4DAD7]
│  ├─ Notice Category: Statutory Notice u/s 143(1)(a)
│  ├─ DIN (Document Identification Number): 202610293847291029
│  ├─ Demand Amount: "₹ 24,800" (Rose Display 22pt Bold)
│  └─ Response Deadline: "12 Days Remaining" (Amber Pill #FEF5E7)
├─ [AI Legal Defense Analysis Card: Fill: #E4F0EB, Radius: 16pt, Padding: 16pt]
│  ├─ Cause of Notice: "Section 80D Mediclaim exemption disallowed due to PAN name typo."
│  └─ Resolution Strategy: "Submit rectification u/s 154 with original insurer premium certificate."
├─ [Legal Response Draft (Editable Preview): Fill: #FFFFFF, Radius: 14pt, Padding: 14pt]
│  └─ "To the Assessing Officer, CPC Bengaluru: In reference to DIN ... the assessee submits proof of payment..."
└─ Bottom Actions (2-Button Stack):
   ├─ ZeroTaxPrimaryButton: "Submit Defense Reply to ITD Portal"
   └─ ZeroTaxOutlinedButton: "Download Form 154 Legal Dossier (PDF)"
================================================================================
```
