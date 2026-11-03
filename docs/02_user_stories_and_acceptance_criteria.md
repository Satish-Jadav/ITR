02 — User Stories & Exhaustive Gherkin Acceptance Criteria (Consolidated Phases 1, 2 & 3 — 24 Production Scenarios)
Epic 1: DPDP Act 2023 Compliant Onboarding, Smart PAN Keyboard & Hardware Keystore Vault
Story 1.1: Smart Auto-Switching PAN Keyboard & Individual Status ('P') Validation
Feature: Zero-Friction Smart PAN Entry with Statutory Entity Validation

  Scenario: Keyboard dynamically switches layouts across the 10 characters of an Individual PAN

    Given the user is on Tap 1 (Identity & Consent Screen)

    When the user focuses on the PAN input field

    Then the soft keyboard opens in Uppercase Alphabet mode (`KeyboardType.Ascii`, `Characters`)

    When the user types the first 3 letters "ABC" and the 4th character "P" and the 5th character "E"

    Then the soft keyboard automatically switches to Numeric mode (`KeyboardType.NumberPassword`) for characters 6 through 9

    When the user types the 4 digits "1234"

    Then the soft keyboard automatically switches back to Uppercase Alphabet mode for the 10th check character

    When the user types "F" to complete "ABCPE1234F"

    Then the field validates the PAN checksum and green-checks "Valid Individual PAN"

  Scenario: User enters a Non-Individual PAN (HUF, Company, or Firm)

    Given the user is on Tap 1 (Identity & Consent Screen)

    When the user types "ABCHE1234F" where the 4th character is 'H' (Hindu Undivided Family) or 'C' (Company)

    Then the input field immediately highlights an inline statutory error:

      "4th character 'H' indicates an HUF PAN. ZeroTax-Local supports Individual ('P') taxpayers only."

    And the "Initialize Secure Tax Vault" CTA button remains disabled
Story 1.2: Multilingual DPDP Act Section 6 Itemized Consent & StrongBox Vault Initialization
Feature: Itemized Consent Artifact Recording and AES-256-GCM SQLCipher Initialization

  Scenario: User toggles consent notice to Gujarati and initializes the local encrypted vault

    Given the user has entered valid Individual PAN "ABCPE1234F" and DOB "15/08/1995"

    When the user taps the language pill "ગુજરાતી (gu-IN)" on the DPDP Consent Card

    Then the itemized Section 6 notice, Data Fiduciary disclosures, and Non-ERI browser authorization render in Gujarati

    When the user checks the consent box and taps "Initialize Secure Tax Vault"

    Then the app requests a 256-bit AES master key from the Android Hardware Keystore (`StrongBox` backed if supported)

    And creates the encrypted SQLCipher database `zero_tax_vault.db`

    And writes an immutable row to `user_legal_consents_and_overrides` with:

      | event_category             | DPDP_SECTION_6_CONSENT |

      | consent_or_warning_version | DPDP-2026-v1.0         |

      | language_displayed         | gu-IN                  |

    And navigates to Tap 2 (Live Automated Discovery)
Story 1.3: 1-Tap Cryptographic Right to Erasure ("Shred Local Tax Vault")
Feature: Instant Cryptographic Destruction of All Local Personal Data under Section 12(3) of DPDP Act

  Scenario: User invokes "Shred Local Tax Vault" from the Privacy Shield menu

    Given the user has synced portal JSONs and imported 3 local PDF documents into `zero_tax_vault.db`

    When the user taps the top-right "Privacy Shield" icon and selects "Shred Local Tax Vault"

    And confirms the destructive action in the modal dialog

    Then the app deletes alias `zero_tax_sqlcipher_master_key_v1` from the Android Hardware Keystore

    And overwrites `zero_tax_vault.db`, `-wal`, `-shm`, and all internal PDF/bitmap files with cryptographically random bytes via `SecureRandom` before unlinking them

    And clears all WebView cookies and volatile RAM caches

    And returns to the clean onboarding state in under 300ms


Epic 2: Assisted Portal WebView Bridge, OTP Interception & Portal Resilience
Story 2.1: Single-OTP Assisted Portal Sync & 500ms Session Destruction
Feature: Client-Side Assisted Extraction of Prefill, Form 26AS, and AIS/TIS JSONs

  Scenario: Successful single-OTP sync and immediate cookie sanitization

    Given the user is on Tap 2 (Live Automated Discovery)

    When the user taps "Sync from Income Tax Portal"

    Then an isolated, no-cache WebView session loads `https://eportal.incometax.gov.in` and pre-fills PAN "ABCPE1234F"

    And a native Material 3 bottom sheet prompts the user for the 6-digit Aadhaar/Portal OTP

    When the user enters the OTP (or grants SMS Retriever API one-tap read permission)

    Then the read-only JavaScript interceptor captures `Prefill.json`, `Form 26AS`, and `AIS/TIS` JSON payloads in memory

    And within 500ms of capturing all three payloads, `CookieManager.getInstance().removeAllCookies(null)` and `WebStorage.getInstance().deleteAllData()` execute

    And the UI checklist animates: "Portal Session Closed & Cookies Wiped — 100% Local"
Story 2.2: Graceful Handling of Portal Downtime, Maintenance & Rate Limits
Feature: Zero-Blocker Fallback When e-Filing Portal or TRACES/AIS Server Is Down

  Scenario: Income Tax Portal returns HTTP 503 / Maintenance Banner during peak July 31 rush

    Given the user initiates Portal Sync on Tap 2

    When the e-Filing portal responds with HTTP 502/503/504 or times out after 12 seconds

    Then the app aborts the WebView session and wipes partial cookies

    And displays a reassuring fallback card:

      "The government e-Filing portal is currently experiencing heavy traffic. You can continue 100% offline right now by selecting your Form 16 PDF or previous ITR JSON."

    And enables the "Continue Offline with Local PDFs" primary button without losing progress


Epic 3: Silent PDF Unlocker, On-Device Gemini Nano & Post-AI Math Sanity Firewall
Story 3.1: Silent Unlocking Across 15 Standard Indian Financial Password Formats
Feature: Zero-Prompt Local Decryption of Password-Protected Financial PDFs

  Scenario: User imports a password-protected TRACES Form 16 and an HDFC Bank Statement

    Given the user's PAN is "ABCPE1234F", DOB is "15/08/1995", and First Name is "Aarav"

    When the user selects `Form16_ABCPE1234F.pdf` (encrypted with `abcpe1234f15081995`) and `HDFC_Stmt.pdf` (encrypted with `aara15081995`)

    Then `SilentPdfUnlocker` tests candidate permutations on `Dispatchers.Default`

    And unlocks `Form16_ABCPE1234F.pdf` on permutation #1 and `HDFC_Stmt.pdf` on permutation #6 in under 100ms total

    And never prompts the user to type a PDF password

  Scenario: User imports a PDF with a custom user-defined password outside the 15 standard permutations

    Given the user selects a PDF locked with a custom passphrase "MySecretPassword@2026"

    When all 15 deterministic permutations fail within 95ms

    Then the app displays a compact inline sheet: "This PDF uses a custom password. Enter it once to unlock locally."
Story 3.2: Post-AI Math Sanity Firewall & 1-Tap Visual Cropped Disambiguation
Feature: Deterministic Mathematical Verification of On-Device AI Extracted Figures

  Scenario: Scanned non-standard Form 16 Part B fails the mathematical identity check

    Given ML Kit OCR and on-device Gemini Nano (`temperature = 0.0f`) extract JSON from a folded physical Form 16 scan:

      | gross_salary_sec17_1        | 18,40,000 |

      | exempt_hra_sec10_13a        | 2,40,000  |

      | standard_deduction_sec16_ia | 75,000    |

      | net_taxable_salary_printed  | 15,85,000 |

    When the `DeterministicSanityFirewall` evaluates `18,40,000 - 2,40,000 - 75,000 = 15,25,000`

    And detects a discrepancy of ₹60,000 against `net_taxable_salary_printed` (₹15,85,000), exceeding the ±₹5 tolerance

    Then the firewall blocks the unverified JSON from entering the tax calculation engine

    And crops the exact bounding box of the "Net Salary" row from the rendered PDF page bitmap

    And presents a 1-Tap Visual Verification Card showing the cropped image alongside:

      "[ Tap to Confirm ₹15,25,000 (Calculated) ]  vs  [ Tap to Confirm ₹15,85,000 (Printed) ]"


Epic 4: Multi-Employer Reconciliation, TAN De-Duplication & Strict 26AS TDS Cap
Story 4.1: Mid-Year Job Switch with Two Form 16s and Overlapping AIS Records
Feature: TAN-Based Salary De-Duplication and Single Standard Deduction Enforcement

  Scenario: Reconciling 2 Form 16s and 2 AIS salary entries without double-counting

    Given the user imports Form 16 from Employer 1 (`TAN: MUMT01928A`, Gross `₹10,50,000`, Std Ded `₹75,000`, TDS `₹65,000`)

    And imports Form 16 from Employer 2 (`TAN: BLRI99887C`, Gross `₹14,00,000`, Std Ded `₹75,000`, TDS `₹1,15,000`)

    And synced `AIS` also contains `TDS-192` salary entries for `MUMT01928A` (`₹10,50,000`) and `BLRI99887C` (`₹14,00,000`)

    When `DeterministicReconciler.reconcileSalaries()` executes

    Then it groups records by normalized 10-character TAN (`MUMT01928A` and `BLRI99887C`)

    And computes Consolidated Gross Salary as `₹24,50,000` (preventing ₹49,00,000 double-counting with AIS)

    And applies `Section 16(ia)` Standard Deduction of `₹75,000` (New Regime) or `₹50,000` (Old Regime) strictly ONCE

    And recomputes combined slab tax on the aggregated income, flagging any self-assessment tax shortfall caused by both employers giving separate basic exemptions
Story 4.2: Employer Deducted TDS in Payslip/Form 16 But Failed to Deposit in Form 26AS
Feature: Strict Form 26AS Ceiling on Schedule TDS1 to Prevent Automated CPC Demand Notices

  Scenario: Start-up employer issued Form 16 showing ₹1,80,000 TDS, but Form 26AS shows only ₹1,20,000 deposited

    Given Form 16 for TAN `BLRS55443D` reports `tdsReported = ₹1,80,000`

    And synced `Form 26AS Part A` for TAN `BLRS55443D` reports `tdsDeposited = ₹1,20,000`

    When the Tax Engine compiles `Schedule TDS1`

    Then it caps the claimable TDS for `BLRS55443D` strictly at `₹1,20,000`

    And generates a High-Priority Exception Nudge Card:

      "Employer TDS Mismatch (₹60,000): Your Form 16 shows ₹1,80,000 TDS, but only ₹1,20,000 is deposited in Form 26AS. We capped your claim at ₹1,20,000 so CPC Bengaluru does not issue a Sec 143(1) defective demand. Tap to generate a pre-filled follow-up email to your employer payroll team."


Epic 5: Regime Showdown, HRA Rule 2A, Sec 80CCD(2) & Marginal Relief
Story 5.1: HRA Rule 2A Exemption Blocked by Missing Landlord PAN (Rent > ₹1,00,000)
Feature: Statutory Landlord PAN Gate for House Rent Allowance above ₹1,00,000/year

  Scenario: User paid ₹2,40,000 annual rent (`₹20,000/month`) but Landlord PAN is missing

    Given the user's Basic Salary is `₹8,00,000`, HRA Received is `₹3,20,000`, and Annual Rent Paid is `₹2,40,000` in a Non-Metro city (`40%` limit)

    And `Rule 2A` eligible HRA exemption is `MIN(₹3,20,000, ₹2,40,000 - ₹80,000, ₹3,20,000) = ₹1,60,000`

    When Landlord PAN is blank

    Then `HraAndInterestEngine` sets `exemptHraRupees = ₹0` and `blockedDueToMissingLandlordPan = true`

    And the On-Device AI Helper (Role B) surfaces a Nudge Card on Tap 3:

      "Add Landlord PAN to unlock ₹1,60,000 HRA exemption and save ₹14,560 in Old Regime—or stay in New Regime with zero paperwork."
Story 5.2: Automatic Discovery of Section 80CCD(2) Employer NPS in New Tax Regime
Feature: Claiming Employer NPS Contribution (Up to 14% of Basic+DA) Inside New Tax Regime

  Scenario: User is in Default New Regime (`Sec 115BAC(1A)`) and has Employer NPS in Form 16

    Given the user's Basic + DA is `₹12,00,000` and Employer NPS contribution u/s `80CCD(2)` is `₹1,44,000` (`12%`)

    When the Tax Engine computes New Tax Regime liability

    Then it automatically deducts the full `₹1,44,000` (since it is within the `14%` cap of `₹1,68,000` under `Sec 115BAC(1A)`)

    And displays a green badge on Tap 4: "Included ₹1,44,000 Employer NPS u/s 80CCD(2) — Legally Tax-Free even in New Regime!"


Epic 6: Phase 2 — Freelancer 44ADA, Capital Gains Grandfathering, Land Indexation & UPI Challan 280
Story 6.1: Freelancer Section 44ADA Auto-Routing & Bank Narration Classification
Feature: Presumptive Taxation under Section 44ADA for Professionals with 194J Receipts

  Scenario: Consultant has ₹18,00,000 in 194J receipts in 26AS and ₹6,00,000 in direct Upwork bank credits

    Given `Form 26AS` contains `TDS-194J` professional receipts totaling `₹18,00,000`

    And the user imports their bank statement where On-Device Gemini Nano classifies `NEFT-CR-UPWORK` (`₹6,00,000`) as `BUSINESS_RECEIPT_44ADA`

    When the user confirms the Upwork credits on the Freelancer Nudge Card

    Then the engine sets Gross Professional Receipts u/s `44ADA` to `₹24,00,000`

    And auto-routes the return form to `ITR-4 (Sugam)`

    And defaults Presumptive Taxable Profit to `50%` (`₹12,00,000`) while blocking any manual slider adjustment below `50%` unless the user acknowledges the mandatory `Section 44AB` CA Tax Audit requirement
Story 6.2: Section 112A Grandfathering & Strict LTCL vs STCG Set-Off Restriction
Feature: ISIN-Level Jan 31, 2018 Grandfathering and Section 70/74 Loss Set-Off Rules

  Scenario: Investor sells pre-2018 equity shares and has both STCG and LTCL in the same year

    Given the user imports a Zerodha/CAMS statement containing:

      1. 500 shares of `INE002A01018` bought on `10-May-2016` at `₹400/sh` (`₹2,00,000`), `Jan 31, 2018 FMV = ₹950/sh` (`₹4,75,000`), sold on `12-Nov-2025` at `₹1,200/sh` (`₹6,00,000`)

      2. Another LTCG lot resulting in a Long-Term Capital Loss (`LTCL`) of `₹2,10,000`

      3. Short-Term Capital Gains (`STCG u/s 111A`) of `₹3,00,000`

    When `CapitalGainsAndForeignAssetEngine` executes

    Then Lot 1 Grandfathered Cost is computed as `MAX(₹2,00,000, MIN(₹4,75,000, ₹6,00,000)) = ₹4,75,000`, yielding `LTCG = ₹1,25,000`

    And Lot 2 `LTCL` of `₹2,10,000` offsets Lot 1 `LTCG` of `₹1,25,000`, leaving `Net LTCG = ₹0` and `Unabsorbed LTCL = ₹85,000`

    And the engine strictly BLOCKS the remaining `₹85,000 LTCL` from offsetting the `₹3,00,000 STCG`

    And carries forward `₹85,000 LTCL` in `Schedule CFL` for 8 Assessment Years
Story 6.3: Immovable Property Grandfathering (12.5% Without Indexation vs 20% With Indexation)
Feature: Resident Individual Land/Building LTCG Tax Cap for Pre-July 23, 2024 Acquisitions

  Scenario: Resident taxpayer sells an apartment acquired before July 23, 2024

    Given a Resident Individual bought a residential flat on `15-Jun-2015` (FY 2015-16, `CII = 254`) for `₹50,00,000`

    And sold the flat on `20-Nov-2025` (FY 2025-26, `CII = 376`) for `₹80,00,000`

    When the engine evaluates `Section 112(1)(a)` proviso for property acquired prior to `23-Jul-2024`:

      - Option A (New Rule — 12.5% without indexation): Gain = `₹80,00,000 - ₹50,00,000 = ₹30,00,000` -> Tax at 12.5% = `₹3,75,000`

      - Option B (Grandfathered Rule — 20% with CII indexation): Indexed Cost = `₹50,00,000 * (376 / 254) = ₹74,01,575` -> Indexed Gain = `₹5,98,425` -> Tax at 20% = `₹1,19,685`

    Then the engine deterministically selects Option B (`₹1,19,685`), saving the taxpayer `₹2,55,315` in LTCG tax
Story 6.4: In-App UPI Challan 280 Payment & Schedule IT BSR/Serial Auto-Capture
Feature: Seamless Self-Assessment Tax Payment and Schedule IT Population

  Scenario: User has ₹18,400 Balance Tax Payable on Tap 4

    Given the user's net tax liability exceeds TDS credit by `₹18,400`

    When the user taps "Pay ₹18,400 via UPI (Challan 280 - Head 300)"

    And completes the payment and imports the generated e-Pay Tax Challan PDF receipt

    Then `Challan280ReceiptParser` extracts the 7-digit `BSRCode`, `DateOfDeposit`, 5-digit `ChallanSerialNo`, and `₹18,400` amount

    And injects the entry directly into `Schedule IT` of the CBDT JSON

    And updates `BalTaxPayable` to `₹0` so the return is ready for immediate e-Verification


Epic 7: Phase 3 — Foreign RSU Schedule FA, Form 67 FTC & Vernacular Notice Explainer
Story 7.1: Calendar-Year (Jan–Dec) Schedule FA with Holiday SBI TTBR Rollback
Feature: Accurate Foreign Equity Reporting and Rule 115 / Rule 128 Exchange Rate Conversion

  Scenario: US RSU vested on a Sunday and paid US dividend with 25% DTAA tax withheld

    Given the user imports a Charles Schwab RSU statement for Calendar Year 2025 showing:

      - 100 shares vested on Sunday `16-Mar-2025` at `$150.00/sh` (`$15,000`)

      - Peak price during 2025 was `$190.00/sh` on `14-Aug-2025` (`$19,000`)

      - Closing price on `31-Dec-2025` was `$175.00/sh` (`$17,500`)

    When `ScheduleFaCalendarYearEngine` queries `sbi_ttbr_exchange_rates` for `2025-03-16` (Sunday)

    Then it rolls back deterministically to Friday `2025-03-14` (`TTBR = ₹86.80`)

    And computes `Initial Value = ₹13,02,000`, `Peak Value`, and `Closing Value` in INR for `Schedule FA Table A3`

    And auto-generates `Form 67` Foreign Tax Credit payload using the preceding-month-end SBI TTBR under `Rule 128(5)`
Story 7.2: Token-Locked Vernacular Explanation of a Section 143(1) Intimation Notice
Feature: Zero-Hallucination Hindi and Gujarati Translation of Income Tax Notices

  Scenario: User imports a CPC Bengaluru Section 143(1) notice and requests a Gujarati explanation

    Given the deterministic parser extracts from the notice:

      | {{SEC_1}} | Section 143(1)(a) |

      | {{AMT_1}} | ₹24,600           |

      | {{AMT_2}} | ₹18,200           |

    When `TokenLockedVernacularExplainer` sends the tokenized template to Gemini Nano (`AICore`) for `Gujarati` translation

    Then Gemini Nano translates the surrounding explanation into natural Gujarati while keeping `{{SEC_1}}`, `{{AMT_1}}`, and `{{AMT_2}}` intact

    And `restoreAndVerifyTokens()` verifies 100% token preservation before substituting `₹24,600` and `₹18,200` back into the UI
Story 7.3: In-App AIS Discrepancy Dispute & Feedback Submission
Feature: Official AIS Feedback Submission via Portal Bridge

  Scenario: User disputes a duplicate high-value transaction in AIS

    Given the user taps "Dispute Entry" on an AIS Nudge Card

    When the user selects reason "Information is duplicate / relates to another FY"

    Then the Assisted Portal Bridge submits the feedback payload to the AIS endpoint

    And saves the AIS Feedback Acknowledgment Receipt in the local Tax Vault
Story 7.4: March Tax-Loss & LTCG Harvesting Radar
Feature: Proactive March LTCG Tax-Harvesting Radar

  Scenario: User has unused headroom in their ₹1,25,000 Section 112A LTCG threshold in March

    Given the current date is between Feb 15 and March 28

    When the local portfolio analyzer checks realized LTCG (e.g., ₹65,000) and unrealized LTCG (> ₹20,000)

    Then it surfaces a Nudge Card showing exact units to redeem and re-buy to harvest up to ₹60,000 tax-free gain, saving up to ₹7,500 in future tax
Epic 8: Statutory Warning Modals & Legal Audit Trail (`Sec 270A`, `Black Money Act`, `Sec 140`)
Story 8.1: Mandatory Audit Logging When User Overrides Verified Income Downward
Feature: Cryptographic Audit Trail for Statutory Risk Warnings

  Scenario: User acknowledges Section 270A modal to adjust joint FD interest

    Given `AIS` reports `₹92,000` FD interest and the user edits it to `₹46,000` (50% joint account share)

    When the `Section 270A Statutory Penalty Warning` modal appears

    And the user checks the statutory affirmation checkbox, selects reason `JOINT_ACCOUNT_PRIMARY_SPOUSE`, and taps "Log Reason & Override"

    Then the app writes an immutable record to `user_legal_consents_and_overrides` containing `system_computed_value_rupees = 92000`, `user_overridden_value_rupees = 46000`, the SHA-256 hash of the warning text, and the monotonic timestamp

Epic 9: Phase 1–3 Extended Acceptance Scenarios (Completing the 24-Scenario Master Suite & Subfolder Story Traceability)
Story 9.1 (P1-US-05): Pre-Submission CBDT JSON Schema Gate & 1-Tap Aadhaar OTP e-Verification
Feature: Zero-Defect CBDT Schema Validation and Electronic Return Verification

  Scenario: Validating ITR-1/ITR-4 JSON against Category 'A' Defect Rules and e-Verifying via Aadhaar OTP

    Given all Nudge Cards are resolved and the user is on Tap 4 (Regime Showdown)

    When the user checks the Section 140 declaration and taps "Submit CBDT JSON & e-Verify with Aadhaar OTP"

    Then `CbdtItr1JsonCompiler` validates that `TDS1.TotalTDSonSalaries <= GrossSalary`, `TotalChapVIADeductions <= GrossTotalIncome`, and `ScheduleBA` contains a valid 11-character IFSC (`^[A-Z]{4}0[A-Z0-9]{6}$`) with `UseForRefund = "true"`

    And verifies that `PersonalInfo.AadhaarCardNo` is strictly redacted as `"[Aadhaar Redacted]"`

    And uploads the validated JSON via the Assisted Portal Bridge, completes Aadhaar OTP e-Verification, and stores `ITR-V.pdf` in `zero_tax_vault.db`
Story 9.2 (P2-US-01): One-OTP RBI Account Aggregator (AA) Bank & CAMS/KFintech CAS Fetch
Feature: Ephemeral ECDHE-Curve25519 Decryption of FIP Financial Information

  Scenario: User approves AA consent and decrypts bank and mutual fund statements in volatile RAM

    Given the user is on Tap 2 and AIS indicates SFT-005 bank interest and SFT-016/017 mutual fund redemptions

    When the user approves the RBI Account Aggregator consent artifact via OTP

    Then `RbiAaEphemeralDecryptor` performs ECDHE-Curve25519 key agreement, HKDF-SHA256 key derivation, and AES-256-GCM decryption strictly in volatile RAM

    And reconciles bank savings/FD interest and CAMS/KFintech redemption lots against AIS totals

    And immediately zeroes out `sharedSecretBytes`, `derivedKey`, and `plaintextBytes` (`0x00`) in memory
Story 9.3 (P2-US-04): On-Device Gemini Nano Bank Ledger Narration Deduction Discovery
Feature: Local Batch Classification of Bank Statement Narrations for Missed Chapter VI-A Deductions

  Scenario: Discovering missed Section 80D health insurance and Section 80C PPF investments from AA bank narrations

    Given a 12-month bank statement JSON is decrypted in memory via Account Aggregator

    When the pre-filter discards personal UPI debit rows below ₹500 and batches unique merchant narration strings to on-device Gemini Nano (`temperature = 0.0f`)

    Then Gemini Nano classifies `"ACH-DR-STAR HEALTH AND ALLIED INS"` (`₹28,400`) as `80D_HEALTH_INS` and `"NEFT-PPF-SBI"` (`₹50,000`) as `80C_LIC_PPF_ELSS`

    And surfaces a swipeable Nudge Card on Tap 3: "We found ₹28,400 paid to Star Health Insurance in your bank statement. Add to Section 80D?"
Story 9.4 (P2-FR-05): Multi-House Property (Schedule HP) & Section 71(3A) ₹2,00,000 Loss Set-Off Cap
Feature: Schedule HP Computation for Self-Occupied and Let-Out Properties

  Scenario: Taxpayer owns a self-occupied house with ₹3,20,000 home loan interest and a let-out apartment

    Given the user declares Property 1 as `SELF_OCCUPIED` with Section 24(b) home loan interest of `₹3,20,000`

    And declares Property 2 as `LET_OUT` with Gross Rent `₹3,00,000` and Municipal Taxes `₹20,000`

    When the Tax Engine computes `Schedule HP` under Old Regime

    Then it applies the 30% statutory deduction (`₹84,000`) u/s 24(a) on Property 2 (`Net Annual Value = ₹2,80,000`, `Net Income = ₹1,96,000`)

    And caps the overall House Property loss set-off against Salary/Other Heads strictly at `₹2,00,000` u/s 71(3A), carrying forward the unabsorbed `₹1,20,000` loss in `Schedule CFL` for 8 Assessment Years
Story 9.5 (P3-FR-05): Updated Return (ITR-U u/s 139(8A)) with Section 140B Additional Tax
Feature: Automated Computation of 25% / 50% Additional Tax for Updated Returns

  Scenario: Taxpayer files ITR-U within 12 months of the end of the relevant Assessment Year to declare omitted income

    Given the user selects "File Updated Return (ITR-U u/s 139(8A))" and enters their Original Acknowledgment Number and Filing Date

    And the additional base tax payable on omitted income is `₹1,00,000` with `₹20,000` interest u/s 234A/B/C

    When `RevisionAndHarvestingEngine.computeUpdatedReturnItrU()` runs for the `WITHIN_12_MONTHS_FROM_AY_END` window

    Then it computes `25%` additional tax u/s `140B` on `(₹1,00,000 + ₹20,000)` = `₹30,000`

    And sets `Total Updated Return Payable = ₹1,50,000` in `Part B-TI-ITR-U`
Story 9.6 (Epic 8): Black Money Act 2015 Mandatory Disclosure Gate for Foreign Assets
Feature: Statutory ₹10 Lakh Penalty Prevention Gate When Foreign Equity Is Detected

  Scenario: Resident Ordinarily Resident (ROR) user holds US RSUs and attempts to proceed to filing

    Given `ScheduleFaCalendarYearEngine` or `AIS TCS-206C(1G)` detects foreign equity holdings during the Calendar Year

    When `DeterministicItrFormRouter` blocks `ITR-1`/`ITR-4` and routes the user to `ITR-2` with `Schedule FA`

    Then the app displays the `Black Money Act, 2015 Mandatory Foreign Asset Disclosure Modal` warning of the ₹10,00,000 penalty under Sections 42 & 43

    And logs `BLACK_MONEY_ACT_FA_ACKNOWLEDGMENT` to `user_legal_consents_and_overrides` upon confirmation
Story 9.7 (Epic 8): UIDAI Verhoeff Checksum Scrubber Blocking Raw 12-Digit Aadhaar Persistence
Feature: Zero-Disclosure Redaction of Aadhaar Numbers Across OCR, WebView, and SQLite

  Scenario: Scanned PDF or portal JSON contains an unmasked 12-digit Aadhaar number

    Given an imported document or JSON payload contains a 12-digit string matching the UIDAI pattern

    When `UidaiAadhaarVerhoeffScrubber.scrubAllAadhaarDigits()` intercepts the text stream prior to AICore inference or Room insertion

    Then the 12-digit sequence is immediately replaced with `"[Aadhaar Redacted]"`

    And `taxpayer_profile.aadhaar_masked_token` enforces `CHECK (aadhaar_masked_token = '[Aadhaar Redacted]')` with zero raw digits ever written to disk or logcat
Story 9.8 (P1-FR-07 / GTV-03 & GTV-04): New Tax Regime Section 87A Rebate & Marginal Relief Boundary Verification (AY 2025-26 ₹7L & AY 2026-27 ₹12L Profiles)
Feature: New Tax Regime Section 87A Rebate, Marginal Relief, and Special Rate Capital Gains Carve-Out

  Scenario: Resident individual with taxable salary of ₹12,10,000 receives Marginal Relief just above the ₹12,00,000 Rebate Ceiling (AY 2026-27)

    Given the user is a Resident Individual (`citizenCategory = REGULAR_BELOW_60`) filing for `AY 2026-27` under Default New Tax Regime (`Sec 115BAC(1A)`)

    And the user's Gross Salary is `₹12,85,000` with `Section 16(ia)` Standard Deduction of `₹75,000` (`Net Taxable Normal Income = ₹12,10,000`)

    When `DeterministicIndianTaxEngine.computeNewRegime()` evaluates slab tax and `Section 87A` Marginal Relief

    Then it computes raw progressive slab tax as `₹61,500` and excess income over `₹12,00,000` as `₹10,000`

    And grants `Marginal Relief u/s 87A` of `₹51,500` (`₹61,500 - ₹10,000`) so tax before cess equals `₹10,000`

    And adds 4% Health & Education Cess (`₹400`) to yield `totalTaxLiabilityRounded288B = ₹10,400`

  Scenario: Resident individual with Normal Income of ₹4,00,000 and Section 112A Equity LTCG of ₹2,25,000 is barred from applying Section 87A Rebate to LTCG under Section 112A(6)

    Given the user is a Resident Individual with Net Taxable Normal Salary of `₹4,00,000` and `Section 112A` Equity LTCG of `₹2,25,000` (`Total Income = ₹6,25,000`)

    When the tax engine evaluates `Section 87A` rebate eligibility

    Then it applies `Section 87A` rebate (`₹5,000`) strictly against the normal slab tax (`₹5,000`), reducing normal tax to `₹0`

    And strictly excludes the `₹12,500` special-rate tax on `Section 112A` LTCG (`(₹2,25,000 - ₹1,25,000) * 12.5%`) from `Section 87A` rebate under `Section 112A(6)`

    And computes final tax payable including 4% cess as `₹13,000`
Story 9.9 (P1-FR-05 / GTV-06): Senior Citizen Section 80TTB (₹50,000 Cap) & Section 207 Advance Tax Interest Exemption
Feature: Senior Citizen Deduction under Section 80TTB and Section 207 Immunity from Section 234B Interest

  Scenario: Senior Citizen (Age 67) claims ₹50,000 deduction on combined Savings & FD interest under Section 80TTB and is exempted from Section 234B interest under Section 207

    Given the user's Date of Birth indicates Age `67` (`SENIOR_60_TO_79`) with no business/professional income

    And the user has Pension Income of `₹6,50,000`, Savings Interest of `₹15,000`, and Fixed Deposit Interest of `₹45,000` (`Total Interest = ₹60,000`)

    When the Tax Engine evaluates Old Regime deductions and statutory interest

    Then it automatically switches from `Section 80TTA` (`₹10,000` savings-only cap) to `Section 80TTB` and deducts `₹50,000` against combined Savings + FD interest

    And sets `Section 234B` default in advance tax interest strictly to `₹0` under `Section 207` regardless of unpaid self-assessment tax exceeding `₹10,000`


10. Master 26-Scenario Traceability Matrix (Stories -> FRs -> Golden Test Vectors)
The following matrix establishes bidirectional traceability across all 26 production scenarios spanning Epic 1 through Epic 9, mapping each user story to its corresponding Functional Requirement and verification Test Vector.

Epic
Story ID
Scenario Description
FR Mapping
Test Vector
Epic 1
Story 1.1 (1/2)
Keyboard dynamically switches layouts across 10 PAN characters
P1-FR-01
GTV-01
Epic 1
Story 1.1 (2/2)
User enters Non-Individual PAN (HUF, Company, Firm)
P1-FR-01
ECV-01
Epic 1
Story 1.2
Gujarati DPDP consent notice and StrongBox vault initialization
P1-FR-02
GTV-02
Epic 1
Story 1.3
1-Tap Cryptographic Right to Erasure (Shred Local Vault)
P1-FR-03
ECV-02
Epic 2
Story 2.1
Single-OTP sync and 500ms cookie/storage sanitization
P1-FR-04
GTV-05
Epic 2
Story 2.2
Portal HTTP 503/downtime graceful fallback to local files
P1-FR-04
ECV-03
Epic 3
Story 3.1 (1/2)
Silent unlocking of Form 16 & HDFC bank statement
P1-FR-06
GTV-07
Epic 3
Story 3.1 (2/2)
Custom PDF password prompt fallback after 15 permutations
P1-FR-06
ECV-04
Epic 3
Story 3.2
Post-AI math sanity firewall & cropped visual disambiguation
P1-FR-08
GTV-08
Epic 4
Story 4.1
Mid-year job switch reconciliation with dual Form 16 & AIS
P1-FR-09
GTV-09
Epic 4
Story 4.2
Form 26AS TDS ceiling cap on Form 16 discrepancy
P1-FR-10
GTV-10
Epic 5
Story 5.1
HRA Rule 2A blocked above ₹1,00,000 without Landlord PAN
P1-FR-11
GTV-11
Epic 5
Story 5.2
Employer NPS u/s 80CCD(2) auto-deduction in New Regime
P1-FR-12
GTV-12
Epic 6
Story 6.1
Freelancer Section 44ADA auto-routing and 50% profit floor
P2-FR-01
GTV-13
Epic 6
Story 6.2
Section 112A Jan 31 2018 grandfathering & LTCL set-off block
P2-FR-02
GTV-14
Epic 6
Story 6.3
Pre-July 23 2024 property grandfathering (12.5% vs 20% CII)
P2-FR-03
GTV-15
Epic 6
Story 6.4
In-app UPI Challan 280 receipt BSR auto-population
P2-FR-04
GTV-16
Epic 7
Story 7.1
Calendar-year Schedule FA with Sunday SBI TTBR rollback
P3-FR-01
GTV-17
Epic 7
Story 7.2
Token-locked vernacular Section 143(1) intimation notice translation
P3-FR-02
GTV-18
Epic 7
Story 7.3
In-App AIS discrepancy dispute & official feedback submission
P3-FR-03
ECV-05
Epic 7
Story 7.4
March tax-loss & Section 112A LTCG tax-harvesting radar
P3-FR-04
ECV-06
Epic 8
Story 8.1
Mandatory audit logging on Sec 270A joint interest reduction
P1-FR-13
P1-GTV-04
Epic 9
Story 9.1
Pre-submission CBDT JSON Category 'A' gate & Aadhaar OTP
P1-US-05
GTV-Master-01
Epic 9
Story 9.2
RBI Account Aggregator ECDHE-Curve25519 ephemeral RAM fetch
P2-US-01
GTV-Master-02
Epic 9
Story 9.3
Gemini Nano bank narration Chapter VI-A deduction discovery
P2-US-04
GTV-Master-03
Epic 9
Story 9.4
Multi-House Property (Schedule HP) & ₹2,00,000 loss set-off cap
P2-FR-05
GTV-Master-04
Epic 9
Story 9.5
Updated Return (ITR-U u/s 139(8A)) with 25% Section 140B tax
P3-FR-05
GTV-Master-05
Epic 9
Story 9.6
Black Money Act 2015 ₹10L penalty disclosure gate for RSUs
P3-FR-06
GTV-Master-06
Epic 9
Story 9.7
UIDAI Verhoeff scrubber blocking raw 12-digit Aadhaar storage
P1-FR-14
GTV-Master-07
Epic 9
Story 9.8 (1/2)
New Regime Section 87A Marginal Relief at ₹12,10,000 threshold
P1-FR-07
GTV-03
Epic 9
Story 9.8 (2/2)
Section 112A LTCG special rate exclusion from Section 87A rebate
P1-FR-07
GTV-04
Epic 9
Story 9.9
Senior Citizen Section 80TTB ₹50,000 & Section 207 234B exemption
P1-FR-05
GTV-06

11. CA & Legal Counsel Statutory Audit Matrix for Acceptance Criteria
Section 139A & Rule 114B (PAN Structural Validation — Story 1.1): The 4th character of the 10-character alphanumeric PAN denotes the assessee's legal status (`'P'` = Individual, `'H'` = HUF, `'C'` = Company, `'F'` = Firm/LLP, `'T'` = Trust). Enforcing `'P'` at Tap 1 prevents defective filings under Section 139(9) where non-individual entities attempt to file `ITR-1` or `ITR-2`.
DPDP Act, 2023 Sections 6 & 12(3) (Itemized Consent & Cryptographic Erasure — Stories 1.2 & 1.3): Every consent grant is immutably logged in `user_legal_consents_and_overrides` with the exact language code (`en-IN`, `hi-IN`, `gu-IN`) and SHA-256 notice digest. Right to Erasure under Section 12(3) destroys the Android Hardware Keystore `StrongBox` master key and overwrites `zero_tax_vault.db`, `-wal`, and `-shm` files with `SecureRandom` bytes prior to file unlinking.
Section 139(1B) Non-ERI Client-Side Browser Assistant (Stories 2.1 & 2.2): Portal synchronization runs exclusively inside an ephemeral handset `WebView` with zero server-side credential custody and a hard 500ms cookie/DOM storage wipe (`CookieManager.removeAllCookies` + `WebStorage.deleteAllData`).
Section 192(2B) & Section 16(ia) Multi-Employer Consolidation (Stories 4.1 & 4.2): De-duplicating salary streams by 10-character Deductor TAN prevents double-counting between `Form 16` and `AIS TDS-192` while ensuring the Section 16(ia) Standard Deduction (₹75,000 in New Regime / ₹50,000 in Old Regime) is claimed strictly once. Capping `Schedule TDS1` claims at Form 26AS Part A deposited amounts protects the taxpayer from automated Section 143(1)(a) short-credit demand intimations.
Section 10(13A), Rule 2A & CBDT Circular No. 8/2013 (Landlord PAN Gate — Story 5.1): Claiming HRA exemption where aggregate annual rent paid exceeds ₹1,00,000 without furnishing a valid 10-character Landlord PAN is blocked at the engine level to prevent Section 270A(9) misreporting penalties (200% of tax sought to be evaded).
Section 55(2)(ac), Section 70(3), Section 74 & Second Proviso to Section 112(1)(a) (Capital Gains & Losses — Stories 6.2 & 6.3): Enforces ISIN-level January 31, 2018 grandfathering without generating artificial capital loss, strictly blocks Long-Term Capital Loss (`LTCL`) from offsetting Short-Term Capital Gains (`STCG`), and compares 12.5% unindexed vs. 20% CII-indexed tax (`CII = 376` for FY 2025-26) for resident individuals selling land or building acquired before July 23, 2024.
Rule 115, Rule 128(5) & Black Money Act Sections 42 & 43 (Foreign Assets & FTC — Stories 7.1 & 9.6): Enforces Calendar Year (`Jan 1 – Dec 31`) disclosure in `Schedule FA Table A3` using historical SBI Telegraphic Transfer Buying Rates (rolling back to the immediately preceding business day on weekends/holidays) and preceding-month-end SBI TTBR for `Form 67` Foreign Tax Credit, shielding Resident and Ordinarily Resident (`ROR`) taxpayers from the flat ₹10,00,000 penalty under Sections 42 & 43 of the Black Money Act, 2015.
12. Automated BDD Test Harness & Statutory Penalty Defense Verification Matrix
12.1 Gherkin-to-JUnit 5 Execution Binding
Deterministic Tax & Sub-Engine Scenarios (Stories 1.1, 4.1, 4.2, 5.1, 5.2, 6.1–6.4, 7.1, 7.4, 9.1, 9.4, 9.5, 9.8, 9.9): Executed via pure-JVM JUnit 5 parameterized tests in :core:tax-engine (MasterGoldenTaxVectorTestSuite.kt and AdvancedTaxEngineEdgeCaseTest.kt) with a strict ₹0.00 variance gate against the official CBDT ITR schema utility.
On-Device Security, Cryptography & Privacy Scenarios (Stories 1.2, 1.3, 2.1, 2.2, 3.1, 3.2, 7.2, 7.3, 8.1, 9.2, 9.3, 9.6, 9.7): Executed via Android instrumentation and security unit tests in :core:security, :feature:ai-extractor, :feature:portal-bridge, and :feature:aa-aggregator, asserting 500ms cookie sanitization, <300ms StrongBox key destruction, 0x00 RAM zeroization, Verhoeff Aadhaar redaction ("[Aadhaar Redacted]"), and 100% byte-for-byte vernacular token preservation ({{SEC_1}}, {{AMT_1}}, {{AMT_2}}).
