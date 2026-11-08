# ZeroTax 100% Real-Time & Zero-Mock Architecture Plan (ITR Android APK + ITR-API Backend)

This master engineering plan details every code change, data pipeline, and external integration required to transition [`Satish-jadav/ITR`](https://github.com/Satish-jadav/ITR) (Android APK) and [`Satish-jadav/ITR-API`](https://github.com/Satish-jadav/ITR-API) (Serverless Backend) from simulated/stubbed data to a **100% Real-Time, Live-Data Production FinTech Platform**.

---

## 1. Root-Cause Audit: Every Location Where Mock or Disconnected Data Still Exists Today

Even though [ZeroTaxViewModel.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/viewmodel/ZeroTaxViewModel.kt) initializes with `0L` rupee values and `app/ui/developer/*` was removed in commit [5e178f2](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/commit/5e178f22a02cff25b43d3e81d4dc4437ac86b79f), the app still feels "mocked" during end-to-end usage because **8 critical action paths** either generate synthetic local strings or fail to persist live extracted data into [ZeroTaxVaultDatabase.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/core/database/src/main/java/in/zerotax/core/database/ZeroTaxVaultDatabase.kt):

| # | Repository & Exact File | Exact Mock / Stubbed Behavior in Code Today | Real-Time Replacement Required |
| :--- | :--- | :--- | :--- |
| **1** | **ITR (APK)**<br>[ItdDirectFilingClient.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/portal-bridge/src/main/java/in/zerotax/feature/portalbridge/ItdDirectFilingClient.kt) | `authenticateInApp()`, `submitItrDirectly()`, `requestInAppAadhaarOtp()`, and `verifyAadhaarOtp()` perform **zero network I/O**. They generate random hex tokens, synthetic acknowledgment numbers (`"2627" + sha256.filter { it.isDigit() }.take(11)`), fake transaction IDs (`"CBDT-TXN-" + sha256`), and fake DINs (`"DIN-2026-" + sha256`). | Replace local SHA-256 fabrication with real HTTP calls to `POST /api/v1/portal/*` on `ITR-API` AND wire the UI directly to the real WebView session interceptor in [HardenedEphemeralPortalBridge.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/portal-bridge/src/main/java/in/zerotax/feature/portalbridge/HardenedEphemeralPortalBridge.kt). |
| **2** | **ITR (APK)**<br>[HardenedEphemeralPortalBridge.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/portal-bridge/src/main/java/in/zerotax/feature/portalbridge/HardenedEphemeralPortalBridge.kt) & [ItdLivePortalSyncModal.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/ui/portal/ItdLivePortalSyncModal.kt) | Contains real `WebView.evaluateJavascript(...)` injectors for `eportal.incometax.gov.in`, but does not execute the authenticated in-WebView `fetch()` calls to pull the taxpayer's live **ITD Pre-Fill JSON**, **AIS/TIS JSON**, and **26AS**, nor does it write captured JSON payloads into [ZeroTaxVaultDatabase.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/core/database/src/main/java/in/zerotax/core/database/ZeroTaxVaultDatabase.kt). | Add authenticated in-WebView `fetch()` interceptors inside `HardenedEphemeralPortalBridge.kt` that capture the real Pre-Fill & AIS JSON from the user's live ITD login session and immediately hydrate `ZeroTaxVaultDatabase.kt` + `ZeroTaxViewModel.kt`. |
| **3** | **ITR (APK)**<br>[OnDeviceTaxCopilot.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/ai-extractor/src/main/java/in/zerotax/feature/aiextractor/OnDeviceTaxCopilot.kt) *(Lines 16, 31–64)* | Contains `SystemAiCoreModelBridge` stub and 4 hardcoded string blocks (`Section 87A`, `Form 26AS`, `Section 270A`, `New vs Old Regime`). Any other question returns a static template string. | Delete all 4 static string blocks. Connect `OnDeviceTaxCopilot.kt` to `POST /api/v1/chat/stream` via `okhttp-sse` (`4.12.0`, already in `app/build.gradle.kts`) passing the user's live `TaxpayerAnonymizedSnapshot`. |
| **4** | **ITR (APK)**<br>[OfflineWhatIfTaxSimulator.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/ai-extractor/src/main/java/in/zerotax/feature/aiextractor/OfflineWhatIfTaxSimulator.kt) *(Lines 36–41)* | Uses hardcoded fallback numbers (`150_000L` for 80C, `100_000L` for 80D parents, `50_000L` for 80D self, `50_000L` for 80CCD(1B), `200_000L` for 24(b)) whenever a prompt doesn't specify a rupee figure. | Query the user's real claimed deductions from [ZeroTaxVaultDatabase.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/core/database/src/main/java/in/zerotax/core/database/ZeroTaxVaultDatabase.kt) and calculate `remainingHeadroom = statutoryCap - claimedInVault`. |
| **5** | **ITR (APK)**<br>8 Toolkit Screens in `app/ui/toolkit/*` | `HraExemptionCalculatorScreen.kt`, `CapitalGainsWizardScreen.kt`, `CryptoTaxLedgerScreen.kt`, `Presumptive44ADAWizardScreen.kt`, `AdvanceTaxCalculatorScreen.kt`, `ItdNoticeParserScreen.kt`, `PdfPasswordUnlockerScreen.kt`, and `RegimeComparatorView.kt` maintain isolated local `remember { mutableStateOf("") }` inputs instead of reading/writing [ZeroTaxVaultDatabase.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/core/database/src/main/java/in/zerotax/core/database/ZeroTaxVaultDatabase.kt). | Bind all 8 Toolkit screens to `ZeroTaxViewModel.kt` so they open pre-populated with the user's live vault figures and write calculated outputs directly back into the active return. |
| **6** | **ITR-API**<br>[src/routes/aa.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/aa.ts) | `GET /api/v1/aa/fi/fetch/:sessionId` returns a hardcoded simulated `HDFC-FIP-01` / `XXXXXXXX4821` encrypted payload instead of calling a live Sahamati/Setu/Finvu Account Aggregator FIU endpoint. | Wire `POST /api/v1/aa/consent`, `POST /api/v1/aa/webhook/fi-ready`, and `GET /api/v1/aa/fi/fetch/:sessionId` to a live Setu AA / Finvu FIU Sandbox & Production API (`AA_FIU_BASE_URL`, `AA_CLIENT_ID`, `AA_CLIENT_SECRET`). |
| **7** | **ITR-API**<br>[src/routes/portal.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/portal.ts) | Stores OTP session rows in PostgreSQL, but generates synthetic OTP verification tokens instead of calling a live e-Return Intermediary (ERI) API or receiving the real WebView-extracted ITD payload. | Add `POST /api/v1/portal/ingest-live-prefill` to validate, scrub, and persist real ITD Pre-Fill & AIS JSON captured by the Android WebView bridge, plus optional licensed ERI/Sandbox.co.in API proxy endpoints. |
| **8** | **ITR-API**<br>[src/routes/statutory.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/statutory.ts) & [src/routes/cron.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/cron.ts) | `statutory-sync` only counts existing seeded rows in `statutory_filing_milestones` and `statutory_cii_rates_master` instead of fetching live external feeds (AMFI NAVs, SBI TTBR forex rates, RBI IFSCs). | Upgrade `/api/v1/cron/statutory-sync` to fetch live daily AMFI mutual fund NAVs (`amfiindia.com`), live SBI TT Buying Rates (`sbi.co.in`), and live IFSC lookups (`ifsc.razorpay.com`). |

---

## 2. How Real-Time Data Reaches the App: The 5 Live Integration Pipelines

Because the Indian Income Tax Department (ITD) restricts direct server-to-server REST API access to licensed **Type-2/Type-3 e-Return Intermediaries (ERIs)**, a production consumer tax app in India uses a **Dual-Mode Real-Time Ingestion Architecture** that works immediately at **zero licensing cost** while supporting drop-in B2B API keys as you scale:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                    ZEROTAX 100% REAL-TIME LIVE DATA ARCHITECTURE                       │
├───────────────────────────┬────────────────────────────────────────────────────────────┤
│ PIPELINE 1 (Zero-Cost)    │ On-Device Authenticated ITD WebView Session Bridge         │
│ Live ITD Portal Sync      │ • User logs into real https://eportal.incometax.gov.in     │
│ (26AS, AIS/TIS, Pre-Fill) │ • `ItdAndAaOtpAutoReader.kt` auto-reads real SMS OTP       │
│                           │ • `HardenedEphemeralPortalBridge.kt` runs in-page JS       │
│                           │   `fetch()` using the user's live portal session to pull   │
│                           │   real Pre-Fill JSON, AIS/TIS JSON & 26AS into SQLCipher   │
├───────────────────────────┼────────────────────────────────────────────────────────────┤
│ PIPELINE 2 (Zero-Cost)    │ On-Device PDF Unlocker + Deterministic & Gemini 2.5 OCR    │
│ Real Document Ingestion   │ • User picks/scans real Form 16, CAS, Broker P&L, Notice   │
│ (PDFs, Photos, QR Codes)  │ • `SilentPdfUnlocker.kt` unlocks password via PAN/DOB      │
│                           │ • `DeterministicTracesRegexParser.kt` & `/api/v1/ai/*`     │
│                           │   (Gemini 2.5 Flash) extract 100% real figures into Vault  │
├───────────────────────────┼────────────────────────────────────────────────────────────┤
│ PIPELINE 3 (Free Sandbox  │ RBI Account Aggregator (Setu AA / Finvu FIU API)           │
│ + Prod FIU Keys)          │ • `src/routes/aa.ts` calls live Setu AA v2.0 API           │
│ Live Bank, FD, MF & Stocks│ • `RbiAaEphemeralDecryptor.kt` decrypts real FIP streams   │
│                           │   on-device via X25519 ECDH + AES-GCM (Fidelius protocol)  │
├───────────────────────────┼────────────────────────────────────────────────────────────┤
│ PIPELINE 4 (Zero-Cost)    │ Live Public Statutory & Financial Feeds (`cron.ts`)        │
│ Daily Statutory Master    │ • Live AMFI NAV feed (`https://www.amfiindia.com/...`)     │
│ Sync                      │ • Live Razorpay/RBI IFSC lookup (`https://ifsc.razorpay.com`)│
│                           │ • Live ITD e-Portal, AIS & TRACES latency probes           │
├───────────────────────────┼────────────────────────────────────────────────────────────┤
│ PIPELINE 5 (Optional B2B) │ Licensed B2B Tax / PAN / GST APIs (Sandbox.co.in / Protean)│
│ Server-to-Server ERI API  │ • Drop-in env vars (`SANDBOX_API_KEY`, `SANDBOX_SECRET`)   │
│                           │   for server-side PAN verification, TAN lookup & ERI filing│
└───────────────────────────┴────────────────────────────────────────────────────────────┘
```

---

## 3. Pipeline-by-Pipeline Technical Implementation Plan

### Pipeline 1: Real-Time ITD Portal Pre-Fill, AIS & e-Filing via `HardenedEphemeralPortalBridge.kt` (Zero API Cost)

Instead of [ItdDirectFilingClient.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/portal-bridge/src/main/java/in/zerotax/feature/portalbridge/ItdDirectFilingClient.kt) fabricating fake acknowledgment numbers, wire [ItdLivePortalSyncModal.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/ui/portal/ItdLivePortalSyncModal.kt) and [AutomatedOnboardingScreen.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/ui/onboarding/AutomatedOnboardingScreen.kt) directly to [HardenedEphemeralPortalBridge.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/portal-bridge/src/main/java/in/zerotax/feature/portalbridge/HardenedEphemeralPortalBridge.kt):

1. **Step 1 — Real Portal Authentication & 1-Tap OTP Auto-Fill**:
   - Load `https://eportal.incometax.gov.in/iec/foservices/#/login` inside the hardened, origin-locked `WebView` in `ItdLivePortalSyncModal.kt`.
   - Automatically pre-fill the user's PAN into the portal input field via `HardenedEphemeralPortalBridge.injectPanLoginScript(webView, pan)`.
   - When the ITD portal sends the 6-digit SMS OTP to the user's phone, `ItdAndAaOtpAutoReader.kt` (added in commit [134611a](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/commit/134611aa6976c38e81dee31c614e45f72ea6dd00)) intercepts the SMS and calls `HardenedEphemeralPortalBridge.injectAadhaarOtpVerificationScript(webView, otp)`.

2. **Step 2 — In-Session Live Pre-Fill JSON & AIS Extraction**:
   - Once the portal redirects to `/#/dashboard`, inject `injectFetchLivePrefillAndAisScript(webView, assessmentYear = "2026-27")` in [HardenedEphemeralPortalBridge.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/portal-bridge/src/main/java/in/zerotax/feature/portalbridge/HardenedEphemeralPortalBridge.kt):
     - Reads the authenticated session state inside `eportal.incometax.gov.in`.
     - Captures the taxpayer's real **ITD Pre-Fill JSON** (containing Personal Info, Employer Salary, Bank Accounts, TDS26AS Part A/B/C, Self-Assessment/Advance Tax Challans, and Carry-Forward Loss schedules) and **AIS/TIS Summary JSON**.
     - Sends the captured JSON over `WebViewCompat.addWebMessageListener` (locked strictly to `setOf("https://eportal.incometax.gov.in")`) to Kotlin.
   - Immediately call `sanitizePortalSessionImmediately()` to wipe WebView cookies and WebStorage.

3. **Step 3 — Parse & Persist into SQLCipher (`ZeroTaxVaultDatabase.kt`)**:
   - Pass the captured ITD Pre-Fill JSON directly into [PriorYearItrJsonImporter.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/tree/master/core/tax-engine/src/main/java/in/zerotax/core/taxengine/compiler) and [DeterministicReconciler.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/tree/master/core/tax-engine/src/main/java/in/zerotax/core/taxengine/reconciliation).
   - Write the parsed `ReconciledSalarySummary`, `TaxpayerFinancialInput`, `ScheduleItChallanEntry` list, and verified Bank Accounts into [ZeroTaxVaultDatabase.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/core/database/src/main/java/in/zerotax/core/database/ZeroTaxVaultDatabase.kt) and emit the new state in [ZeroTaxViewModel.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/viewmodel/ZeroTaxViewModel.kt).

4. **Step 4 — Real ITD JSON Upload & e-Verification (`submitItrDirectly`)**:
   - Replace the mock SHA-256 generator in [ItdDirectFilingClient.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/portal-bridge/src/main/java/in/zerotax/feature/portalbridge/ItdDirectFilingClient.kt) so `submitItrDirectly(compiledItrJson)` uploads the validated CBDT schema JSON (compiled by [CbdtItr1And4JsonExporter.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/tree/master/core/tax-engine/src/main/java/in/zerotax/core/taxengine/compiler) / [Form67AndItr2ScheduleCompiler.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/tree/master/core/tax-engine/src/main/java/in/zerotax/core/taxengine/compiler)) into the authenticated ITD portal session (or licensed ERI endpoint) and parses the **actual 15-digit CBDT Acknowledgment Number** and **real Aadhaar e-Verify DIN** returned by CPC Bengaluru.

---

### Pipeline 2: Real-Time On-Device + Cloud AI Document Extraction (Form 16, CAS, Broker P&L, Notices)

Your APK already has `pdfbox-android:2.0.27.0`, ML Kit Document Scanner, Text Recognition (Latin + Devanagari), and Barcode Scanning in [app/build.gradle.kts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/build.gradle.kts) (added in commit [5e178f2](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/commit/5e178f22a02cff25b43d3e81d4dc4437ac86b79f)). To make document scanning 100% real-time:

1. **Wire the File Picker & Camera Scanner in [AutomatedOnboardingScreen.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/ui/onboarding/AutomatedOnboardingScreen.kt) & [DocumentVaultScreen.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/ui/vault/DocumentVaultScreen.kt)**:
   - Register `rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments())` for PDFs/CSVs/JSONs and `GmsDocumentScanning.getClient(options).getStartScanIntent(activity)` for camera scans.
2. **Execute Real On-Device PDF Unlocking & Text Extraction**:
   - Pass every selected PDF `Uri` to [SilentPdfUnlocker.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/pdf-unlocker/src/main/java/in/zerotax/feature/pdfunlocker/SilentPdfUnlocker.kt), which uses `PDDocument.load(inputStream, candidatePassword)` from `com.tom_roush.pdfbox.pdmodel.PDDocument` to unlock password-protected Form 16, CAMS CAS, and AIS PDFs using the user's lowercase PAN + DOB combinations from [InstitutionPdfPasswordGuide.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/core/model/src/main/java/in/zerotax/core/model/InstitutionPdfPasswordGuide.kt), then extracts raw text via `PDFTextStripper()`.
3. **Parse Deterministically First, Then via Live Gemini 2.5 Flash (`/api/v1/ai/*`)**:
   - If the PDF is a digital TRACES Form 16, CAMS CAS, or Foreign Broker statement, run [DeterministicTracesRegexParser.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/pdf-unlocker/src/main/java/in/zerotax/feature/pdfunlocker/DeterministicTracesRegexParser.kt), [DomesticCapitalGainsPdfParser.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/pdf-unlocker/src/main/java/in/zerotax/feature/pdfunlocker/DomesticCapitalGainsPdfParser.kt), or [ForeignBrokerRsuAnd1042SParser.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/pdf-unlocker/src/main/java/in/zerotax/feature/pdfunlocker/ForeignBrokerRsuAnd1042SParser.kt) on-device in `< 300ms`.
   - For scanned images, non-standard employer annexures, rent receipts, or CPC notices, scrub PII via `UidaiAadhaarVerhoeffScrubber` and send the text to `POST /api/v1/ai/form16-extract`, `POST /api/v1/ai/notice-radar`, or `POST /api/v1/ai/lifecycle-and-nri-scan` in [src/routes/ai.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/ai.ts) (which uses live `@google/genai` `gemini-2.5-flash`).
   - Immediately call `zeroTaxViewModel.ingestExtractedDocument(result)` to update [ZeroTaxVaultDatabase.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/core/database/src/main/java/in/zerotax/core/database/ZeroTaxVaultDatabase.kt) and recompute [DeterministicIndianTaxEngine.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/tree/master/core/tax-engine/src/main/java/in/zerotax/core/taxengine/calculator).

---

### Pipeline 3: Real-Time RBI Account Aggregator (Setu AA / Finvu FIU Integration)

To eliminate the hardcoded `HDFC-FIP-01` / `XXXXXXXX4821` payload in [src/routes/aa.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/aa.ts):

1. **Create a Free Setu Account Aggregator Sandbox / Production Account ([`bridge.setu.co`](https://bridge.setu.co))**:
   - Add 3 environment variables in Vercel and [src/config/env.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/config/env.ts):
     - `AA_FIU_BASE_URL` (e.g., `https://fiu-sandbox.setu.co/v2` for live Setu AA testing or `https://fiu.setu.co/v2` for production)
     - `AA_CLIENT_ID`
     - `AA_CLIENT_SECRET`
2. **Upgrade [src/routes/aa.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/aa.ts) to Call Live ReBIT v2.0.0 Endpoints**:
   - `POST /api/v1/aa/consent`: Calls `POST ${env.AA_FIU_BASE_URL}/consents` requesting `DEPOSIT`, `TERM-DEPOSIT`, `MUTUAL_FUNDS`, `EQUITIES`, and `GSTN_GSTR` for the assessment year (`2025-04-01` to `2026-03-31`), returning the real Setu/Sahamati `consentHandleId` and `redirectUrl` alongside your client's ephemeral X25519 public key (`KeyMaterials`).
   - `POST /api/v1/aa/webhook/fi-ready`: Verifies the incoming Sahamati/Setu detached JWS signature (`x-jws-signature`) using `jose.flattenedVerify` against the FIU JWK Set, updates `aa_fiu_consent_sessions` in Supabase, and triggers an FCM data push to the user's Android device.
   - `GET /api/v1/aa/fi/fetch/:sessionId`: Proxies `GET ${env.AA_FIU_BASE_URL}/sessions/${sessionId}` and returns the real ECDH-encrypted FIP financial packets directly to the Android APK—with **zero hardcoded `HDFC-FIP-01` fallback**.
3. **On-Device Decryption & Bank Narration Classification**:
   - [RbiAaEphemeralDecryptor.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/aa-aggregator/src/main/java/in/zerotax/feature/aaaggregator/RbiAaEphemeralDecryptor.kt) decrypts the FIP payload in RAM using the device's ephemeral X25519 private key.
   - [OnDeviceBankNarrationClassifier.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/ai-extractor/src/main/java/in/zerotax/feature/aiextractor/OnDeviceBankNarrationClassifier.kt) classifies savings/FD interest (Sec 80TTA/80TTB), 44ADA/44AD professional/business receipts, and medical/insurance/donation payments, persisting them into [ZeroTaxVaultDatabase.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/core/database/src/main/java/in/zerotax/core/database/ZeroTaxVaultDatabase.kt).

---

### Pipeline 4: Live Public Statutory Feeds (AMFI Mutual Fund NAV, RBI IFSC, SBI TTBR Forex)

Upgrade [src/routes/cron.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/cron.ts) and [src/routes/statutory.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/statutory.ts) so your backend syncs real market and banking data every day:

1. **Live RBI IFSC Bank & Branch Verification (`GET /api/v1/statutory/ifsc/:code` in [src/routes/statutory.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/statutory.ts))**:
   - First checks your PostgreSQL `ifsc_directory_master` cache; on cache miss, fetches `https://ifsc.razorpay.com/${code}` (the official RBI IFSC dataset mirror), caches the bank name, branch, MICR, and UPI/NEFT capability in `ifsc_directory_master`, and returns it in `< 80ms` for instant refund bank account verification in the APK.
2. **Live AMFI Mutual Fund ISIN & NAV Sync (`GET|POST /api/v1/cron/statutory-sync` in [src/routes/cron.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/cron.ts))**:
   - Fetches the official AMFI India daily text feed (`https://www.amfiindia.com/spages/NAVAll.txt`), parses `ISIN Div Payout/ISIN Growth;ISIN Div Reinvestment;Scheme Name;Net Asset Value;Date`, and updates `isin_jan2018_fmv_master` / live NAV tables so capital gains and tax-loss harvesting use real daily NAVs.
3. **Live SBI TT Buying Rate Sync for NRI / Foreign RSU (`Schedule FA` & `Form 67`)**:
   - Fetches the official SBI Forex Card Rate feed (`https://sbi.co.in/documents/16012/1400784/FOREX_CARD_RATES.pdf` or RBI reference rate API) during `/api/v1/cron/statutory-sync` and upserts USD, EUR, GBP, SGD, AUD, CAD, and AED TT Buying Rates into `sbi_ttbr_rates_master`.

---

### Pipeline 5: Unifying the 8 Toolkit Wizards, Simulator & Chatbot with Live Vault State

To ensure that **zero screens in the APK ever show dummy or disconnected numbers**:

1. **Replace `OnDeviceTaxCopilot.kt` & `OfflineWhatIfTaxSimulator.kt` Stubs**:
   - In [OnDeviceTaxCopilot.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/ai-extractor/src/main/java/in/zerotax/feature/aiextractor/OnDeviceTaxCopilot.kt): Delete lines 31–64 (the 4 hardcoded `Section 87A`, `Form 26AS`, `Section 270A`, `New vs Old Regime` strings). Connect `askCopilotStream(query, snapshot)` via `okhttp3.sse.EventSources` to `POST /api/v1/chat/stream` on your Vercel backend.
   - In [OfflineWhatIfTaxSimulator.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/ai-extractor/src/main/java/in/zerotax/feature/aiextractor/OfflineWhatIfTaxSimulator.kt): Delete lines 36–41 (`150_000L`, `100_000L`, `50_000L`, `200_000L` fallback constants). Compute all "what-if" deltas strictly from `val remaining80C = (150_000L - currentInput.section80CDeductions).coerceAtLeast(0L)` using the user's real `TaxpayerFinancialInput` from [ZeroTaxViewModel.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/viewmodel/ZeroTaxViewModel.kt).
2. **Bind All 8 `app/ui/toolkit/*` Screens to `ZeroTaxViewModel.kt`**:
   - Pass `uiState: ZeroTaxUiState` and `onApplyToDossier: (TaxpayerFinancialInput) -> Unit` into:
     1. `HraExemptionCalculatorScreen.kt` (pre-fills Basic Salary & HRA from Form 16 in Vault)
     2. `CapitalGainsWizardScreen.kt` (pre-fills STCG 111A & LTCG 112A from CAMS/Broker parser in Vault)
     3. `CryptoTaxLedgerScreen.kt` (pre-fills 194S VDA TDS entries from AIS in Vault)
     4. `Presumptive44ADAWizardScreen.kt` (pre-fills 194J/194C gross receipts from 26AS/AA in Vault)
     5. `AdvanceTaxCalculatorScreen.kt` (pre-fills net tax liability & Challan 280 credits from Vault)
     6. `ItdNoticeParserScreen.kt` (launches real PDF picker -> `SilentPdfUnlocker` -> `/api/v1/ai/notice-radar`)
     7. `PdfPasswordUnlockerScreen.kt` (unlocks real PDF via `pdfbox-android` and saves unlocked copy)
     8. `RegimeComparatorView.kt` (displays live `uiState.newRegimeResult` vs `uiState.oldRegimeResult`)

---

## 4. Environment Variables & API Keys Matrix (What You Need to Configure)

| Variable Name | Where to Set It | Cost / Provider | Purpose in Real-Time Architecture |
| :--- | :--- | :--- | :--- |
| `DATABASE_URL` | Vercel Env Vars | Free / Pro ([Supabase](https://supabase.com)) | Stores live encrypted vaults, portal telemetry, statutory masters, and RAG vectors. |
| `GEMINI_API_KEY` | Vercel Env Vars | Free Tier / Pay-as-you-go ([Google AI Studio](https://aistudio.google.com)) | Powers real-time `@google/genai` (`gemini-2.5-flash`) document OCR, notice analysis, and streaming chat in `ai.ts` & `chat.ts`. |
| `CRON_SECRET` | Vercel + GitHub Secrets | Self-Generated (`openssl rand -hex 32`) | Authenticates GitHub Actions (`cron-scheduler.yml`) calling `/api/v1/cron/*`. |
| `ZEROTAX_API_BASE_URL` | GitHub Secrets + APK `BuildConfig` | Your Vercel URL (`https://...vercel.app`) | Connects the Android APK (`ZeroTaxHardenedApiClient.kt`) and GitHub Actions cron runner to your live Vercel backend. |
| `AA_FIU_BASE_URL`, `AA_CLIENT_ID`, `AA_CLIENT_SECRET` | Vercel Env Vars | Free Sandbox / Live FIU ([Setu AA](https://bridge.setu.co) / [Finvu](https://finvu.in)) | Enables real-time RBI Account Aggregator bank, FD, mutual fund, and equity fetching in `src/routes/aa.ts`. |
| `SANDBOX_API_KEY`, `SANDBOX_API_SECRET` *(Optional)* | Vercel Env Vars | Optional B2B API ([Sandbox.co.in](https://sandbox.co.in)) | Optional server-to-server PAN verification, TAN lookup, and ERI e-Filing (WebView bridge works with zero cost if omitted). |

---

## 5. Step-by-Step Execution Sequence (4 Concrete Implementation Batches)

To implement this cleanly without breaking builds, we should generate and commit the code in **4 focused batches**:

- **Batch 1 — `ITR-API` Live Backend Endpoints (`aa.ts`, `portal.ts`, `statutory.ts`, `cron.ts`)**:
  1. Remove the hardcoded `HDFC-FIP-01` mock in [src/routes/aa.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/aa.ts) and wire it to live Setu/Sahamati ReBIT v2.0 endpoints + cryptographic `jose` JWS webhook verification.
  2. Add `GET /api/v1/statutory/ifsc/:code` (live RBI/Razorpay IFSC lookup with Postgres caching) and live AMFI NAV + SBI TTBR sync in [src/routes/statutory.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/statutory.ts) and [src/routes/cron.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/cron.ts).
  3. Add `POST /api/v1/portal/ingest-live-prefill` in [src/routes/portal.ts](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR-API/blob/master/src/routes/portal.ts) to persist encrypted portal snapshots with `pan_blind_index_hmac`.
- **Batch 2 — `ITR` APK Live Portal Bridge & Zero-Mock Filing Client (`HardenedEphemeralPortalBridge.kt`, `ItdDirectFilingClient.kt`, `ItdLivePortalSyncModal.kt`)**:
  1. Delete all local SHA-256 fake acknowledgment/DIN generators in [ItdDirectFilingClient.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/portal-bridge/src/main/java/in/zerotax/feature/portalbridge/ItdDirectFilingClient.kt).
  2. Add authenticated in-WebView `fetch()` capture for ITD Pre-Fill JSON, AIS/TIS JSON, and 26AS in [HardenedEphemeralPortalBridge.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/portal-bridge/src/main/java/in/zerotax/feature/portalbridge/HardenedEphemeralPortalBridge.kt) and wire [ItdLivePortalSyncModal.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/ui/portal/ItdLivePortalSyncModal.kt) to hydrate [ZeroTaxVaultDatabase.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/core/database/src/main/java/in/zerotax/core/database/ZeroTaxVaultDatabase.kt) + [ZeroTaxViewModel.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/viewmodel/ZeroTaxViewModel.kt).
- **Batch 3 — `ITR` APK Live PDF/Camera Document Pipeline (`SilentPdfUnlocker.kt`, `AutomatedOnboardingScreen.kt`, `DocumentVaultScreen.kt`)**:
  1. Wire Android `ActivityResultContracts.OpenMultipleDocuments()` and ML Kit Document Scanner in `AutomatedOnboardingScreen.kt` and `DocumentVaultScreen.kt` through `SilentPdfUnlocker.kt` (`pdfbox-android`), `DeterministicTracesRegexParser.kt`, and `ZeroTaxHardenedApiClient.kt` (`/api/v1/ai/form16-extract`) directly into `ZeroTaxVaultDatabase.kt`.
- **Batch 4 — `ITR` APK Live Copilot, Simulator & 8 Toolkit Wizards (`OnDeviceTaxCopilot.kt`, `OfflineWhatIfTaxSimulator.kt`, `app/ui/toolkit/*`)**:
  1. Delete the 4 static string blocks in `OnDeviceTaxCopilot.kt` and connect it to `POST /api/v1/chat/stream` via `okhttp-sse`.
  2. Replace the hardcoded `150_000L`..`200_000L` sample defaults in `OfflineWhatIfTaxSimulator.kt` with live vault headroom queries.
  3. Bind all 8 `app/ui/toolkit/*` calculators to read from and write back to `ZeroTaxViewModel.kt` / `ZeroTaxVaultDatabase.kt`.


---

## 6. Zero-License Personal / Indie Testing Mode (100% Real Data With Zero Government or Regulator Licenses)

If you are testing and running ZeroTax as an individual developer **without** a corporate CBDT e-Return Intermediary (ERI) license or an RBI Account Aggregator (FIU) regulator license, every feature can still run on **100% real personal tax data** using these license-free pipelines:

| Feature / Data Source | Why Official B2B APIs Require a License | How ZeroTax Gets 100% Real Data With **Zero Licenses** |
| :--- | :--- | :--- |
| **1. Income Tax Portal Sync (26AS, AIS/TIS, Pre-Fill JSON)** | Direct server-to-server ITD APIs require a CBDT Type-2/Type-3 ERI license. | **In-App WebView Download Interceptor + JSON Importer**: You log into your real account at `https://eportal.incometax.gov.in` inside [ItdLivePortalSyncModal.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/ui/portal/ItdLivePortalSyncModal.kt). When you tap *"Download Pre-Fill JSON"* or *"AIS JSON"* in the portal WebView, Android's `WebView.setDownloadListener` intercepts the real `.json` file stream in memory and passes it to [PriorYearItrJsonImporter.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/tree/master/core/tax-engine/src/main/java/in/zerotax/core/taxengine/compiler) to populate your entire return. |
| **2. Real ITR Filing & Aadhaar e-Verification** | Direct REST submission requires an ERI client certificate. | **Official CBDT Offline JSON Upload via WebView**: [CbdtItr1And4JsonExporter.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/tree/master/core/tax-engine/src/main/java/in/zerotax/core/taxengine/compiler) exports the official CBDT schema `.json` file onto the device and opens the ITD Portal's **"Offline Utility JSON Upload"** screen inside [HardenedEphemeralPortalBridge.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/portal-bridge/src/main/java/in/zerotax/feature/portalbridge/HardenedEphemeralPortalBridge.kt), attaching the compiled JSON and completing real Aadhaar OTP e-Verification on the official portal. |
| **3. Bank Statements, FD Interest & 44AD/44ADA Credits** | Production RBI Account Aggregator FIU access requires an RBI/SEBI/IRDAI regulated entity license. | **On-Device Bank Statement PDF/CSV Parser**: Tap *"Import Bank Statement (PDF/CSV)"* in onboarding or vault. [SilentPdfUnlocker.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/pdf-unlocker/src/main/java/in/zerotax/feature/pdfunlocker/SilentPdfUnlocker.kt) (`pdfbox-android`) unlocks password-protected HDFC/SBI/ICICI/Axis PDFs using your PAN/DOB, and [OnDeviceBankNarrationClassifier.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/ai-extractor/src/main/java/in/zerotax/feature/aiextractor/OnDeviceBankNarrationClassifier.kt) classifies real savings interest (80TTA), FD interest (80TTB), business receipts, and tax-saving debits. |
| **4. Mutual Funds, Stocks & F&O Capital Gains** | Depository/Broker APIs require partner agreements. | **Real CAMS/KFintech CAS & Broker P&L PDF Parser**: Import your real CAMS/KFintech Consolidated Account Statement PDF or Zerodha/Groww/Upstox Tax P&L PDF. [DomesticCapitalGainsPdfParser.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/pdf-unlocker/src/main/java/in/zerotax/feature/pdfunlocker/DomesticCapitalGainsPdfParser.kt) extracts real ISINs, buy/sell dates, STCG (111A), and LTCG (112A) directly on-device. |
| **5. Form 16, Rent Receipts & Tax Notices (143(1))** | N/A | **On-Device PDFBox + Free Personal Gemini API Key**: Digital TRACES Form 16 PDFs are parsed offline by [DeterministicTracesRegexParser.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/pdf-unlocker/src/main/java/in/zerotax/feature/pdfunlocker/DeterministicTracesRegexParser.kt); scanned PDFs/photos use a free personal `GEMINI_API_KEY` from [Google AI Studio](https://aistudio.google.com). |
| **6. Live IFSC, AMFI NAVs & Portal Health** | N/A | **100% Public Unauthenticated Feeds**: Uses `https://ifsc.razorpay.com/{IFSC}`, `https://www.amfiindia.com/spages/NAVAll.txt`, and direct `HEAD` requests to `https://eportal.incometax.gov.in`—zero API keys or licenses required. |
