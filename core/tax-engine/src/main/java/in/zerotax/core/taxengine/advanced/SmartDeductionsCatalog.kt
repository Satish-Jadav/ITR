package `in`.zerotax.core.taxengine.advanced

import `in`.zerotax.core.model.DeductionRegimeScope
import `in`.zerotax.core.model.LifeEventTag
import `in`.zerotax.core.model.StatutoryDeductionSpec

object SmartDeductionsCatalog {

    val ALL_DEDUCTION_SPECS: List<StatutoryDeductionSpec> = listOf(
        // 1. RUNNING HOME LOAN
        StatutoryDeductionSpec(
            sectionCode = "24(b)",
            statutoryRef = "Section 24(b) of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.HOME_LOAN,
            deductionTitle = "Home Loan Interest (Self-Occupied House)",
            maxLimitRupees = 2_00_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Lending Bank / Housing Finance Company (e.g., SBI, HDFC, ICICI, LIC HFL)",
            requiredDocumentName = "Annual Home Loan Provisional / Final Interest Certificate",
            whereToFetchInstructions = "Download from your NetBanking Home Loan portal or collect in person from your loan home branch.",
            howItAppliesExplanation = "Deducted directly from Gross Income under Head 'Income from House Property' as loss up to ₹2 Lakhs.",
            defaultEligibleAmount = 1_80_000L
        ),
        StatutoryDeductionSpec(
            sectionCode = "80C (Principal)",
            statutoryRef = "Section 80C(2)(xviii) of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.HOME_LOAN,
            deductionTitle = "Home Loan Principal Repayment",
            maxLimitRupees = 1_50_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Lending Bank / Housing Finance Company",
            requiredDocumentName = "Home Loan Annual Repayment Schedule (Principal Breakdown)",
            whereToFetchInstructions = "Available in your Bank NetBanking under 'Home Loan Tax Certificate' showing the principal repaid component.",
            howItAppliesExplanation = "Counts towards the overall ₹1,50,000 umbrella limit under Section 80C. Property must not be sold within 5 years.",
            defaultEligibleAmount = 80_000L
        ),
        StatutoryDeductionSpec(
            sectionCode = "80EEA",
            statutoryRef = "Section 80EEA of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.HOME_LOAN,
            deductionTitle = "First-Time Affordable Home Buyer Additional Interest",
            maxLimitRupees = 1_50_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Lending Bank / Sub-Registrar",
            requiredDocumentName = "Loan Sanction Letter (dated 1-Apr-2019 to 31-Mar-2022) & Value Certificate",
            whereToFetchInstructions = "Obtain loan sanction letter from your lender showing loan date and stamp duty valuation ≤ ₹45 Lakhs.",
            howItAppliesExplanation = "Additional deduction of up to ₹1,50,000 OVER AND ABOVE the ₹2,00,000 limit of Section 24(b).",
            defaultEligibleAmount = 0L
        ),

        // 2. HEALTH INSURANCE & PREVENTIVE CHECKUP
        StatutoryDeductionSpec(
            sectionCode = "80D (Self & Family)",
            statutoryRef = "Section 80D(2)(a) of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.HEALTH_INSURANCE,
            deductionTitle = "Medical Insurance Premium (Self, Spouse & Children)",
            maxLimitRupees = 25_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Health / General Insurance Provider (e.g., Star Health, HDFC ERGO, Care, Niva Bupa)",
            requiredDocumentName = "Section 80D Tax Exemption Premium Certificate",
            whereToFetchInstructions = "Download from your insurer's customer portal / mobile app under 'Tax Certificates'.",
            howItAppliesExplanation = "Deduction for non-cash premium payments. Includes up to ₹5,000 for preventive health checkups.",
            defaultEligibleAmount = 22_000L
        ),
        StatutoryDeductionSpec(
            sectionCode = "80D (Parents)",
            statutoryRef = "Section 80D(2)(b) of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.HEALTH_INSURANCE,
            deductionTitle = "Medical Insurance / Expenditure for Parents",
            maxLimitRupees = 50_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Parents' Health Insurance Company / Medical Invoices",
            requiredDocumentName = "80D Premium Receipt or Medical Expenditure Invoices (for Senior Citizens)",
            whereToFetchInstructions = "Download insurer certificate or retain hospital/pharmacy bills if parents are senior citizens without insurance.",
            howItAppliesExplanation = "₹25,000 if parents are below 60 years; ₹50,000 if parents are Senior Citizens (aged 60+).",
            defaultEligibleAmount = 35_000L
        ),

        // 3. NPS / PENSION SCHEME
        StatutoryDeductionSpec(
            sectionCode = "80CCD(1B)",
            statutoryRef = "Section 80CCD(1B) of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.NPS_INVESTMENT,
            deductionTitle = "Voluntary NPS Investment (Tier 1 Account)",
            maxLimitRupees = 50_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Central Recordkeeping Agency (CRA - Protean / KFintech / NSDL)",
            requiredDocumentName = "NPS Statement of Transaction (SOT) / PRAN Contribution Receipt",
            whereToFetchInstructions = "Login to npscra.nsdl.co.in or cra-kfintech.com using PRAN and download Statement of Transaction (SOT).",
            howItAppliesExplanation = "Exclusive ₹50,000 deduction OVER AND ABOVE the ₹1,50,000 Section 80C ceiling.",
            defaultEligibleAmount = 50_000L
        ),
        StatutoryDeductionSpec(
            sectionCode = "80CCD(2)",
            statutoryRef = "Section 80CCD(2) of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.NPS_INVESTMENT,
            deductionTitle = "Employer NPS Contribution (Available in BOTH Regimes)",
            maxLimitRupees = 1_40_000L,
            regimeScope = DeductionRegimeScope.BOTH_REGIMES,
            documentHolder = "Your Employer (HR / Payroll Department)",
            requiredDocumentName = "Form 16 Part B / Monthly Salary Slip",
            whereToFetchInstructions = "Check Line Item 'Employer contribution to pension scheme u/s 80CCD(2)' in Form 16 Part B.",
            howItAppliesExplanation = "Up to 14% of Basic for Central/State Govt employees or 10% for Private Sector. Allowed in NEW REGIME also!",
            defaultEligibleAmount = 60_000L
        ),

        // 4. HIGHER EDUCATION LOAN
        StatutoryDeductionSpec(
            sectionCode = "80E",
            statutoryRef = "Section 80E of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.EDUCATION_LOAN,
            deductionTitle = "Interest on Higher Education Loan",
            maxLimitRupees = 10_00_000L, // No upper cap under IT Act
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Scheduled Bank / Approved Financial Institution (SBI, Canara, Credila, Avanse)",
            requiredDocumentName = "Annual Education Loan Interest Certificate",
            whereToFetchInstructions = "Request education loan interest statement from the bank where the education loan is running.",
            howItAppliesExplanation = "100% of interest paid is deductible without any upper limit for up to 8 consecutive assessment years.",
            defaultEligibleAmount = 75_000L
        ),

        // 5. CHILDREN TUITION FEES
        StatutoryDeductionSpec(
            sectionCode = "80C (Tuition Fees)",
            statutoryRef = "Section 80C(2)(xvii) of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.CHILDREN_FEES,
            deductionTitle = "School / College Tuition Fees for Children",
            maxLimitRupees = 1_50_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Registered School / University / College Authority",
            requiredDocumentName = "Original Fee Receipts specifically showing 'Tuition Fee' breakdown",
            whereToFetchInstructions = "Collect fee receipts from school account office or download digital receipts from school fee portal.",
            howItAppliesExplanation = "Covers full-time education for up to 2 children. Transport, hostel, library, and development fees are excluded.",
            defaultEligibleAmount = 45_000L
        ),

        // 6. RENT PAYER WITHOUT HRA
        StatutoryDeductionSpec(
            sectionCode = "80GG",
            statutoryRef = "Section 80GG of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.RENT_NO_HRA,
            deductionTitle = "House Rent Paid by Self-Employed or Non-HRA Salaried",
            maxLimitRupees = 60_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Your Landlord & Form 10BA e-Filing Declaration",
            requiredDocumentName = "Rent Agreement, Monthly Rent Receipts & Form 10BA",
            whereToFetchInstructions = "Collect rent receipts from landlord. File Form 10BA on Income Tax portal before submitting ITR.",
            howItAppliesExplanation = "Deduction capped at least of: (a) ₹5,000/month (₹60,000/yr), (b) 25% of total income, or (c) Rent paid minus 10% of total income.",
            defaultEligibleAmount = 60_000L
        ),

        // 7. CHARITABLE & PM CARES DONATIONS
        StatutoryDeductionSpec(
            sectionCode = "80G",
            statutoryRef = "Section 80G of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.CHARITABLE_DONATION,
            deductionTitle = "Donations to Approved Relief Funds & Charitable Trusts",
            maxLimitRupees = 5_00_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Donee Institution / Charitable Trust / PM CARES Fund",
            requiredDocumentName = "Form 10BE Certificate of Donation & Receipt with Trust 80G Registration",
            whereToFetchInstructions = "Ask the NGO / institution for their Form 10BE certificate generated from the Income Tax portal.",
            howItAppliesExplanation = "50% or 100% deduction depending on category (e.g. PM CARES is 100% without limit). Cash donations capped at ₹2,000.",
            defaultEligibleAmount = 25_000L
        ),

        // 8. ELECTRIC VEHICLE (EV) LOAN
        StatutoryDeductionSpec(
            sectionCode = "80EEB",
            statutoryRef = "Section 80EEB of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.EV_CAR_LOAN,
            deductionTitle = "Interest on Loan for Purchase of Electric Vehicle (EV)",
            maxLimitRupees = 1_50_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Vehicle Financing Bank / NBFC (e.g., SBI Green Car Loan)",
            requiredDocumentName = "EV Auto Loan Interest Certificate",
            whereToFetchInstructions = "Download the annual interest statement from your auto loan lender.",
            howItAppliesExplanation = "Up to ₹1,50,000 deduction on interest for EV loans sanctioned between 1-Apr-2019 and 31-Mar-2023.",
            defaultEligibleAmount = 40_000L
        ),

        // 9. SAVINGS BANK INTEREST
        StatutoryDeductionSpec(
            sectionCode = "80TTA",
            statutoryRef = "Section 80TTA of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.SAVINGS_INTEREST,
            deductionTitle = "Interest on Savings Bank Accounts",
            maxLimitRupees = 10_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Your Savings Banks / Post Office (SBI, HDFC, ICICI, etc.)",
            requiredDocumentName = "Bank Passbook / Annual Account Statement / AIS Summary",
            whereToFetchInstructions = "Directly auto-fetched by ZeroTax from your Annual Information Statement (AIS).",
            howItAppliesExplanation = "Exempts up to ₹10,000 of interest earned across all your savings bank accounts.",
            defaultEligibleAmount = 10_000L
        ),

        // 10. DISABLED DEPENDENT CARE
        StatutoryDeductionSpec(
            sectionCode = "80DD",
            statutoryRef = "Section 80DD of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.DISABLED_DEPENDENT,
            deductionTitle = "Medical Treatment & Maintenance of Disabled Dependent",
            maxLimitRupees = 1_25_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Government Medical Authority / Hospital",
            requiredDocumentName = "Form 10-IA & Disability Certificate issued by Medical Board",
            whereToFetchInstructions = "Issued by government civil hospital medical board specifying percentage of disability.",
            howItAppliesExplanation = "Flat deduction of ₹75,000 for disability 40%-80%; Flat ₹1,25,000 for severe disability (>80%).",
            defaultEligibleAmount = 75_000L
        ),

        // 11. PERSON WITH DISABILITY (SELF)
        StatutoryDeductionSpec(
            sectionCode = "80U",
            statutoryRef = "Section 80U of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.SELF_DISABILITY,
            deductionTitle = "Deduction for Taxpayer with Disability (Self)",
            maxLimitRupees = 1_25_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Government Civil Hospital / Authorized Medical Authority",
            requiredDocumentName = "Form 10-IA & Certificate of Disability issued under RPwD Act",
            whereToFetchInstructions = "Obtain from Chief Medical Officer (CMO) or Government Civil Hospital Medical Board.",
            howItAppliesExplanation = "Flat deduction: ₹75,000 for disability >= 40% to 80%; ₹1,25,000 for severe disability > 80%.",
            defaultEligibleAmount = 75_000L
        ),

        // 12. SPECIFIED CHRONIC DISEASES (CANCER, RENAL FAILURE, ETC.)
        StatutoryDeductionSpec(
            sectionCode = "80DDB",
            statutoryRef = "Section 80DDB of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.CRITICAL_ILLNESS,
            deductionTitle = "Medical Treatment of Specified Chronic Diseases",
            maxLimitRupees = 1_00_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Specialist Doctor (Oncologist, Neurologist, Nephrologist)",
            requiredDocumentName = "Prescription in Form 10-I & Treatment Invoices from Registered Hospital",
            whereToFetchInstructions = "Collect medical prescription containing specialist registration number and hospital invoices.",
            howItAppliesExplanation = "Up to ₹40,000 for individuals below 60; up to ₹1,00,000 for Senior Citizens (aged 60+), minus insurance reimbursement.",
            defaultEligibleAmount = 40_000L
        ),

        // 13. SENIOR CITIZEN INTEREST ON SAVINGS & FIXED DEPOSITS
        StatutoryDeductionSpec(
            sectionCode = "80TTB",
            statutoryRef = "Section 80TTB of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.SENIOR_DEPOSITS,
            deductionTitle = "Senior Citizen Interest on Bank & Post Office Deposits",
            maxLimitRupees = 50_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Scheduled Banks / Post Office / Cooperative Banks",
            requiredDocumentName = "Bank Passbook / Fixed Deposit Interest Certificates (Form 16A)",
            whereToFetchInstructions = "Directly reflected in your Annual Information Statement (AIS) or bank interest certificate.",
            howItAppliesExplanation = "Exclusive to Senior Citizens (60+ yrs). Covers both Savings Bank AND Term/Fixed/Recurring deposit interest up to ₹50,000.",
            defaultEligibleAmount = 50_000L
        ),

        // 14. POLITICAL PARTIES & ELECTORAL TRUST DONATIONS
        StatutoryDeductionSpec(
            sectionCode = "80GGC",
            statutoryRef = "Section 80GGC of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.POLITICAL_DONATION,
            deductionTitle = "Contributions to Registered Political Parties & Electoral Trusts",
            maxLimitRupees = 10_00_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Registered Political Party / Electoral Trust",
            requiredDocumentName = "Official Contribution Receipt & Bank Statement verifying non-cash payment",
            whereToFetchInstructions = "Collect digital receipt from the party treasurer indicating party PAN and Election Commission registration.",
            howItAppliesExplanation = "100% of contribution is deductible. Cash donations are completely prohibited u/s 80GGC.",
            defaultEligibleAmount = 20_000L
        ),

        // 15. SCIENTIFIC RESEARCH & RURAL DEVELOPMENT DONATIONS
        StatutoryDeductionSpec(
            sectionCode = "80GGA",
            statutoryRef = "Section 80GGA of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.POLITICAL_DONATION,
            deductionTitle = "Donations for Scientific Research or Rural Development",
            maxLimitRupees = 5_00_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Approved Research Association, University or Rural Dev Fund",
            requiredDocumentName = "Receipt with Section 35(1)(ii) / 35CCA notification reference",
            whereToFetchInstructions = "Obtain from the approved research institute or university receiving the grant.",
            howItAppliesExplanation = "100% deduction for taxpayers with no business income. Cash donation capped at ₹2,000.",
            defaultEligibleAmount = 15_000L
        ),

        // 16. ANNUITY / PENSION FUNDS
        StatutoryDeductionSpec(
            sectionCode = "80CCC",
            statutoryRef = "Section 80CCC of the Income-tax Act, 1961",
            lifeEventTag = LifeEventTag.NPS_INVESTMENT,
            deductionTitle = "Contribution to LIC or Other Insurer Pension / Annuity Fund",
            maxLimitRupees = 1_50_000L,
            regimeScope = DeductionRegimeScope.OLD_REGIME_ONLY,
            documentHolder = "Life Insurance Corporation of India (LIC) / Approved Insurers",
            requiredDocumentName = "Annuity Premium Paid Certificate",
            whereToFetchInstructions = "Download from insurer portal under Pension Plan Premium receipts.",
            howItAppliesExplanation = "Deduction for receiving pension from the annuity fund, subject to the overall ₹1,50,000 ceiling under Section 80CCE.",
            defaultEligibleAmount = 50_000L
        )
    )

    fun getDeductionsForTags(tags: Set<LifeEventTag>): List<StatutoryDeductionSpec> {
        return ALL_DEDUCTION_SPECS.filter { it.lifeEventTag in tags }
    }

    fun getBySection(sectionCode: String): StatutoryDeductionSpec? {
        return ALL_DEDUCTION_SPECS.firstOrNull { it.sectionCode.startsWith(sectionCode, ignoreCase = true) }
    }

    fun getDeductionsForRegime(regimeScope: DeductionRegimeScope): List<StatutoryDeductionSpec> {
        return ALL_DEDUCTION_SPECS.filter { it.regimeScope == regimeScope || it.regimeScope == DeductionRegimeScope.BOTH_REGIMES }
    }
}
