# ZeroTax Official Guide: How to Obtain a CBDT e-Return Intermediary (ERI) License & Best Immediate Live-Data API Alternatives

This standalone guide covers:
1. **How to obtain your own official Income Tax Department (CBDT) e-Return Intermediary (ERI) License** (including the Individual vs. Company vs. Chartered Accountant fast-track routes, exact fees, bank details, postal address, and technical audit requirements).
2. **The 6 Official CBDT IEC 2.0 ERI REST APIs** you unlock once approved (`Login`, `AddClient`, `RequestPrefillOTP / GetPrefill`, `Validate & Submit ITR`, `e-Verify Return`, and `Acknowledgement`).
3. **The 4 Best Options Available Today** (ranked by speed, cost, and zero-license readiness) so you can run `Satish-jadav/ITR` and `Satish-jadav/ITR-API` on **100% real taxpayer data immediately** while your ERI application processes.

---

## 1. Deep Comparison: The 4 Best Ways to Get 100% Real Income Tax Data Today

Before waiting weeks for government paperwork, here is the side-by-side comparison of every real-data route available in India:

| Option | Time to Go Live | Cost | License Required? | How It Works for `ZeroTax` (`ITR` APK + `ITR-API`) |
| :--- | :--- | :--- | :--- | :--- |
| **Option A (Best to Use Today at ₹0 Cost)**<br>**In-App Authenticated ITD WebView XHR/Download Interceptor + On-Device PDFBox** | **Immediate (0 Days)** | **₹0 (Free)** | **None** (Uses the taxpayer's own personal login at [`eportal.incometax.gov.in`](https://eportal.incometax.gov.in)) | • User logs into `eportal.incometax.gov.in/iec/foservices/#/login` inside [ItdLivePortalSyncModal.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/app/src/main/java/in/zerotax/app/ui/portal/ItdLivePortalSyncModal.kt) with PAN + 1-tap SMS OTP (`ItdAndAaOtpAutoReader.kt`).<br>• [HardenedEphemeralPortalBridge.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/feature/portal-bridge/src/main/java/in/zerotax/feature/portalbridge/HardenedEphemeralPortalBridge.kt) intercepts the authenticated `iec/foservices` JSON responses & Pre-Fill/AIS downloads directly inside the WebView and pipes them into [ZeroTaxVaultDatabase.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/blob/master/core/database/src/main/java/in/zerotax/core/database/ZeroTaxVaultDatabase.kt).<br>• Uploads compiled CBDT ITR JSON via the portal's Offline Utility JSON Upload screen. |
| **Option B (Best Self-Serve REST API Without Your Own ERI License)**<br>**B2B Tax API Aggregators** ([Sandbox.co.in](https://sandbox.co.in/income-tax), [Deepvue.ai](https://docs.deepvue.ai/reference/itr/introduction), [Surepass.io](https://surepass.io/ais-verification-api/)) | **Same Day (10–30 Mins)** | Free Sandbox Trial / Pay-per-API-call (₹2–₹15 per pull) | **None** (You piggyback on the provider's existing ERI / TSP license) | • [Sandbox.co.in](https://developer.sandbox.co.in/api-reference/it/overview) (by Quicko, a licensed Type-2/Type-3 ERI) provides self-serve REST APIs for ITR Preparation, ERI Filing, Form 16/26AS OCR, PAN KYC, and GST.<br>• [Deepvue.ai](https://docs.deepvue.ai/reference/itr/introduction) & [Surepass.io](https://surepass.io/form-26as-api/) provide consent-based REST APIs (`POST` PAN + OTP -> returns live JSON for **Form 26AS**, **AIS/TIS**, and **Past ITRs**). |
| **Option C (Fastest Path to Your Own Official Type-2 ERI API Access)**<br>**Co-Apply / Partner with a Practicing Chartered Accountant (CA)** | **2 to 3 Weeks** | **₹4,720** Govt Fee + Class 3 DSC (~₹1,500) | Practicing CA's ICAI Certificate of Practice (COP) + DISA/CISA Certificate | • Under the [Protean ERI Guidelines](https://tin.tin.proteantech.in/eri), an individual Practicing CA or CA Firm is automatically eligible for ERI without corporate net-worth hurdles.<br>• If your CA partner holds an ICAI DISA/CISA qualification, they can also issue the Computing Infrastructure Due Diligence Certificate. |
| **Option D (Direct Own ERI License)**<br>**Apply Directly as Individual (Type-3) or LLP / Pvt Ltd (Type-2 & Type-3)** | **3 to 6 Weeks** | **₹4,720** Govt Fee + CISA Audit + Bank Guarantee | Individual PAN (for Type-3) or Registered LLP / Pvt Ltd (for Type-2) | • Gives you direct credentials on the [Income Tax Department IEC 2.0 ERI Gateway](https://www.incometax.gov.in/iec/foportal/api-specifications) (`API_Login_v1.1`, `API_AddClientFlow_v1.1`, `API_Prefill_v1.1`, `API_SubmitFlow_v1.1`, `API_Everify_Return_v1.1`). |

---

## 2. Understanding the 3 Official Types of CBDT ERI Licenses

Under the **Electronic Furnishing of Return of Income Scheme, 2007** (managed by the Income Tax Department and [Protean eGov Technologies Limited](https://tin.tin.proteantech.in/eri), formerly NSDL e-Governance), there are three distinct ERI categories ([ITD ERI API Specifications v1.1](https://www.incometax.gov.in/iec/foportal/sites/default/files/2021-11/ERI%20API%20Specification_v1.1.pdf)):

| ERI Category | Official CBDT Definition | What Capabilities & APIs You Get | Who Can Apply & Requirements |
| :--- | :--- | :--- | :--- |
| **Type 1 ERI** | Qualified Tax Professionals who act on behalf of taxpayers and prepare returns using ITD utilities or Type-3 software. | Portal-based client management (`Authorized Partners -> My e-Return Intermediary`). No custom software API submission. | Practicing CAs, Cost Accountants, Company Secretaries, Advocates, and DDOs. Requires a CISA/ISA Due Diligence Certificate. |
| **Type 2 ERI**<br>*(Target for Direct Server-to-Server API)* | Tax Software Providers who have **their own software application** used by taxpayers to prepare and file ITRs directly via the ITD e-Filing API gateway. | **Full REST API Access** on IEC 2.0: `Login API`, `Add Client API`, `RequestPrefillOTP & GetPrefill API`, `Validate & Submit ITR API`, `e-Verify Return API`, and `Acknowledgement API`. | Companies, LLPs, CA Firms, Practicing CAs/Advocates/CS/CMAs. Requires own software (`ZeroTax`), Class 3 DSC, CISA/ISA Due Diligence Certificate, Static Server IP, Undertaking, Bank Guarantee, and Security Audit Report ([ITD ERI Registration Help](https://www.incometax.gov.in/iec/foportal/help/eri/registration)). |
| **Type 3 ERI**<br>*(Easiest Entry for Software Developers)* | Tax Software Providers who develop **offline return preparation utilities** that generate validated CBDT ITR JSON files for taxpayers or Type-1 ERIs. | Official recognition as a Registered Type-3 ERI software utility developer (`ZeroTax` already compiles offline CBDT JSON via `CbdtItr1And4JsonExporter.kt`). | **Individuals**, LLPs, Firms, or Companies. Per [Protean ERI Guidelines](https://tin.tin.proteantech.in/eri), **Type 3 does NOT require the Type-1/Type-2 CISA Due Diligence Certificate** during initial Protean document submission, and Individuals only need PAN, Govt ID Proof, and Address Proof! |

> **Strategic Recommendation for You**:
> 1. You can apply **immediately as an Individual (or LLP/Pvt Ltd) for Type-3 ERI** (since `Satish-jadav/ITR` already includes your offline CBDT JSON compiler `CbdtItr1And4JsonExporter.kt` and `Form67AndItr2ScheduleCompiler.kt`).
> 2. To get **Type-2 ERI (Direct REST API access for Pre-Fill & Submission)**, either apply under a registered LLP/Pvt Ltd or **co-apply with a Practicing Chartered Accountant** (Type-2 ERI), which avoids heavy corporate net-worth requirements.

---

## 3. Exact Eligibility & Document Checklist by Applicant Category

Per the official [Protean Tax Information Network ERI Application Portal (`tin.tin.proteantech.in/eri`)](https://tin.tin.proteantech.in/eri) and [ITD ERI Registration Manual (`incometax.gov.in/iec/foportal/help/eri/registration`)](https://www.incometax.gov.in/iec/foportal/help/eri/registration), all supporting documents must be self-attested:

### A. If Applying as an Individual (`For Individuals`)
1. **Copy of PAN Card** issued to the applicant (must be active, valid, and registered as a taxpayer on the e-Filing portal).
2. **Identity Proof** issued by a Government Agency (Aadhaar Card / Passport / Voter ID / Driving License).
3. **Address Proof** (Aadhaar Card / Passport / Electricity or Bank Statement).
4. **Class 3 Digital Signature Certificate (DSC)** in the applicant's name registered on the ITD e-Filing portal.
5. *(If applying for Type 1 or Type 2)*: **Due Diligence Certificate** from a certified **ISA (ICAI DISA)** or **CISA (ISACA)** professional in the prescribed ITD format confirming computing infrastructure, plus the ITD Undertaking & Security Audit Report.

### B. If Applying Through a Practicing Chartered Accountant (`For Chartered Accountant` / `Firm of CAs`)
1. **Copy of PAN Card** of the CA (or CA Firm + signing partner's PAN under Sec 140).
2. **Copy of Certificate of Practice (COP)** (or Firm Registration Certificate) issued by **The Institute of Chartered Accountants of India (ICAI)**.
3. **Due Diligence Certificate** from a certified ISA (DISA) or CISA professional (required for Type 1 and Type 2).
4. **Class 3 DSC** of the CA / Principal Contact.

### C. If Applying as an LLP or Private Limited Company (`For LLP` / `For Company Incorporated in India`)
1. **Copy of PAN Card** of the LLP / Company.
2. **Copy of PAN Card of the Principal Contact / Authorized Signatory**.
3. **Copy of Certificate of Incorporation** (for LLP/Company) & **Memorandum of Association (MoA)** (for Company).
4. **Due Diligence Certificate** from a certified ISA or CISA professional (for Type 1 and Type 2).
5. **Undertaking, Bank Guarantee, and Security/Audit Report** as specified on [`incometax.gov.in/iec/foportal/help/eri/registration`](https://www.incometax.gov.in/iec/foportal/help/eri/registration) (for Type 2 and Type 3).

---

## 4. Step-by-Step Procedure to Apply for Your ERI License

Follow these exact 7 steps from the official [Protean ERI Portal](https://tin.tin.proteantech.in/eri) and [Income Tax Department Portal](https://www.incometax.gov.in/iec/foportal/help/eri/registration):

### Step 1: Prerequisite Check on `eportal.incometax.gov.in`
* Ensure your PAN is active and registered as an ordinary Taxpayer on [`https://eportal.incometax.gov.in`](https://eportal.incometax.gov.in).
* Obtain a **Class 3 Digital Signature Certificate (DSC)** token from an authorized CCA Certifying Authority (eMudhra, Capricorn, Pantasign, Protean) and register it under your profile on the e-Filing portal.

### Step 2: Submit the Online ERI Application via Protean / e-Filing Portal
1. Open the official registration portal at [`https://eportal.incometax.gov.in/iec/foservices/#/pre-login/register`](https://eportal.incometax.gov.in/iec/foservices/#/pre-login/register) (under **Register Yourself -> Others -> e-Return Intermediary**) or start via [`https://tin.tin.proteantech.in/eri`](https://tin.tin.proteantech.in/eri) (**"Click Here for e-RI Registration"**).
2. Select your ERI Category (**Type 2**, **Type 3**, or a combination of **Type 2 + Type 3**) and your Applicant Constitution (**Individual**, **Chartered Accountant**, **LLP**, or **Company**).
3. Enter your exact Name and Date of Birth/Incorporation as per PAN, Principal Contact details, DSC serial details, and computing infrastructure details.
4. Submit the online form. The screen will generate a **unique 15-digit Acknowledgment Number** (e.g., `880010100000011`). **Save and print** this acknowledgment page immediately.

### Step 3: Pay the Non-Refundable Registration Processing Fee (₹4,720)
Per [`tin.tin.proteantech.in/eri`](https://tin.tin.proteantech.in/eri), the official Protean ERI verification fee is **₹4,720/- (`₹4,000 + 18% GST`)**:
* **Payment Mode 1 (Demand Draft)**: Draw a Demand Draft for **₹4,720/-** in favor of **`PROTEAN-ERI`** payable at **Mumbai**. Write your Name/Organization Name and your **15-digit Acknowledgment Number** on the back of the DD.
* **Payment Mode 2 (Local Cheque Deposit at HDFC Bank)**: Deposit a local cheque for **₹4,720/-** in favor of **`PROTEAN-ERI`** at any HDFC Bank branch across India (except Dahej) into **HDFC Bank Account Number `05422320001766`**, and attach the stamped deposit slip.

### Step 4: Dispatch Physical Documents to Protean Within 15 Days
1. Sign the printed 15-digit Acknowledgment Sheet (and affix firm/company stamp if not applying as an Individual/CA/Advocate/CS/CMA).
2. Enclose the self-attested eligibility documents (from Section 3 above), the ISA/CISA Due Diligence Certificate (if Type 1 or Type 2), and the ₹4,720 DD / HDFC deposit slip.
3. Write on the top of the envelope:
   `APPLICATION FOR e-RETURN INTERMEDIARY - <YOUR 15-DIGIT ACKNOWLEDGMENT NUMBER>`
4. Mail/courier the packet so it reaches within **15 days** of online application to:
   > **e-Return Intermediary Cell**
   > **Protean eGov Technologies Limited** *(Formerly NSDL e-Governance Infrastructure Limited)*
   > **1st Floor, Times Tower, Kamala Mills Compound,**
   > **Senapati Bapat Marg, Lower Parel (W), Mumbai – 400013**
   > *(Helpline: `020-27218080` | Email: `eriinfo@proteantech.in`)*

### Step 5: Complete ITD e-Filing 2.0 Portal Onboarding & Upload Undertaking / Audit Report
Once Protean verifies your physical documents and forwards approval to the Income Tax Department:
1. You receive an activation link / ARN to complete registration on [`https://eportal.incometax.gov.in`](https://eportal.incometax.gov.in) ([ITD ERI Registration User Manual](https://www.incometax.gov.in/iec/foportal/help/eri/registration)).
2. Upload the signed **Undertaking**, **Bank Guarantee**, and **Security Audit Report** (for Type 2 / Type 3) signed with your Principal Contact's **Class 3 DSC**, and receive your **ERI User ID** (starting with `ERI...`).

### Step 6: Whitelist Your Static Backend IP for ERI REST API Access
To call the ITD IEC 2.0 ERI APIs from your backend ([`Register for e-Filing External Agency Manual`](https://www.incometax.gov.in/iec/foportal/help/register-for-efiling-external-agency)):
1. Provision a static outbound IPv4 address (for example, via a small AWS Mumbai `ap-south-1` Elastic IP proxy or Vercel Static IPs / Fixie proxy in front of `ITR-API`).
2. Email your static IP details and ERI User ID to **`efilingwebmanager@incometax.gov.in`** and share your **DSC RSA Public Key** so the ITD API Gateway can verify your request signatures.

---

## 5. Official CBDT IEC 2.0 ERI API Catalog (How `ITR-API` Connects Once Approved)

Once your ERI User ID is active, the Income Tax Department exposes **6 official REST APIs** documented at [`https://www.incometax.gov.in/iec/foportal/api-specifications`](https://www.incometax.gov.in/iec/foportal/api-specifications) ([`ERI API Specification v1.1 PDF`](https://www.incometax.gov.in/iec/foportal/sites/default/files/2021-11/ERI%20API%20Specification_v1.1.pdf)):

| # | Official CBDT ERI API | Specification PDF on `incometax.gov.in` | Exact Request & Consent Flow |
| :--- | :--- | :--- | :--- |
| **1** | **Login & Logout API** | [`API_Login_v1.1.pdf`](https://www.incometax.gov.in/iec/foportal/sites/default/files/2021-11/API_Login_v1.1.pdf) | Your `ITR-API` backend passes your `ERI User ID`, encrypted password, and `Signature` (signed using your ERI Class 3 DSC private key) to establish an authenticated ERI session token. |
| **2** | **Add Client API** | [`API_AddClientFlow.pdf`](https://www.incometax.gov.in/iec/foportal/sites/default/files/2021-10/API_AddClientFlow.pdf) | Registers a taxpayer (`PAN` + `DOB`) as a client of your ERI. Sends a 6-digit OTP to the taxpayer's mobile/email or Aadhaar; second call verifies the OTP to activate client linkage. |
| **3** | **Prefill API (`requestPrefillOTP` + `getPrefill`)** | [`API_Prefill.pdf`](https://www.incometax.gov.in/iec/foportal/sites/default/files/2021-10/API_Prefill.pdf) | **Call 1 (`requestPrefillOTP`)**: Requests prefill data for an added client; ITD dispatches a consent OTP to the taxpayer.<br>**Call 2 (`getPrefill`)**: Passes the taxpayer's 6-digit OTP (`ItdAndAaOtpAutoReader.kt` auto-reads it!) and returns the **complete official ITD Pre-Fill JSON** (Salary, House Property, Bank Accounts, 26AS TDS/TCS, Advance Tax Challans, Carry-Forward Losses). |
| **4** | **Validate & Submit ITR API** | [`API_SubmitFlow_v1.1.pdf`](https://www.incometax.gov.in/iec/foportal/api-specifications) | Submits the CBDT ITR JSON compiled by your [CbdtItr1And4JsonExporter.kt](https://www.google.com/search?q=https://github.com/Satish-jadav/ITR/tree/master/core/tax-engine/src/main/java/in/zerotax/core/taxengine/compiler) + ERI DSC signature to CPC Bengaluru and returns the provisional submission receipt / ARN. |
| **5** | **e-Verify Return API** | [`API_Everify_Return_v1.1.pdf`](https://www.incometax.gov.in/iec/foportal/api-specifications) | Two-step API: generates an Aadhaar OTP / EVC for the submitted return and validates the 6-digit OTP to mark the return as **e-Verified**. |
| **6** | **Acknowledgement API** | [`API_AcknowledgementFlow.pdf`](https://www.incometax.gov.in/iec/foportal/api-specifications) | Downloads the official signed **ITR-V / Acknowledgment Receipt** once the return is e-Verified. |

---

## 6. How to Architect `ZeroTax` Right Now: The "Dual-Driver" Pattern

So that you are **never blocked** while waiting for ERI approval—and never have a single line of mock data—we implement a **Dual-Driver Portal Architecture** across [`Satish-jadav/ITR`](https://github.com/Satish-jadav/ITR) and [`Satish-jadav/ITR-API`](https://github.com/Satish-jadav/ITR-API):

1. **Driver 1 (Active Today — Zero License Required)**:
   - **On-Device Authenticated WebView Session Bridge (`HardenedEphemeralPortalBridge.kt`) + Local PDF/JSON Importer (`SilentPdfUnlocker.kt` & `PriorYearItrJsonImporter.kt`)**.
   - Works for **100% of taxpayers today at ₹0 cost**: the user logs into `eportal.incometax.gov.in` inside the app's WebView (or selects their downloaded Pre-Fill `.json`, AIS `.pdf`, Form 16 `.pdf`, Bank Statement `.pdf`, or CAMS CAS `.pdf`), and the app extracts and calculates their real return.
2. **Driver 2 (Plug-and-Play Server ERI / B2B Aggregator Driver in `src/routes/portal.ts`)**:
   - Controlled by environment variables in Vercel:
     - If `SANDBOX_API_KEY` ([Sandbox.co.in](https://sandbox.co.in/income-tax)) or `DEEPVUE_API_KEY` ([Deepvue.ai](https://docs.deepvue.ai/reference/itr/introduction)) is set, `src/routes/portal.ts` fetches live 26AS / AIS / Pre-Fill via their B2B REST API.
     - Once your own `ITD_ERI_USER_ID` and `ITD_ERI_DSC_PRIVATE_KEY` are approved by Protean/ITD, `src/routes/portal.ts` switches directly to the official CBDT IEC 2.0 ERI endpoints (`requestPrefillOTP` -> `getPrefill` -> `submitItr` -> `eVerifyReturn`) with **zero UI changes required in the Android APK**.
