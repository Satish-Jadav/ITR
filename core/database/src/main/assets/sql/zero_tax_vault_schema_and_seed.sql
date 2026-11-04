PRAGMA foreign_keys = ON;
PRAGMA secure_delete = ON;

-- ============================================================================
-- TABLE 1: TAXPAYER PROFILE & ONBOARDING IDENTITY
-- ============================================================================
CREATE TABLE IF NOT EXISTS taxpayer_profile (
    pan TEXT PRIMARY KEY NOT NULL CHECK (pan GLOB '[A-Z][A-Z][A-Z]P[A-Z][0-9][0-9][0-9][0-9][A-Z]'),
    assessment_year TEXT NOT NULL DEFAULT '2026-27' CHECK (assessment_year IN ('2025-26', '2026-27')),
    full_name TEXT NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    father_name TEXT NOT NULL DEFAULT '',
    date_of_birth_iso TEXT NOT NULL,
    citizen_category TEXT NOT NULL CHECK (citizen_category IN ('REGULAR_BELOW_60', 'SENIOR_60_TO_79', 'SUPER_SENIOR_80_PLUS')),
    residential_status TEXT NOT NULL CHECK (residential_status IN ('RES_ORDINARILY_RESIDENT', 'RES_NOT_ORDINARILY_RESIDENT', 'NON_RESIDENT')),
    employer_category TEXT NOT NULL DEFAULT 'OTH' CHECK (employer_category IN ('CGOV', 'SGOV', 'PSU', 'PE', 'PENSIONERS', 'OTH', 'NA')),
    aadhaar_masked_token TEXT NOT NULL DEFAULT '[Aadhaar Redacted]' CHECK (aadhaar_masked_token = '[Aadhaar Redacted]'),
    preferred_locale TEXT NOT NULL DEFAULT 'en-IN' CHECK (preferred_locale IN ('en-IN', 'hi-IN', 'gu-IN', 'mr-IN', 'ta-IN', 'te-IN')),
    selected_regime TEXT NOT NULL DEFAULT 'NEW_REGIME_115BAC' CHECK (selected_regime IN ('NEW_REGIME_115BAC', 'OLD_REGIME')),
    auto_routed_itr_form TEXT NOT NULL DEFAULT 'ITR-1' CHECK (auto_routed_itr_form IN ('ITR-1', 'ITR-2', 'ITR-3', 'ITR-4')),
    filing_section INTEGER NOT NULL DEFAULT 11 CHECK (filing_section IN (11, 12, 17, 21)),
    original_ack_no TEXT,
    original_filing_date_iso TEXT,
    primary_bank_ifsc TEXT NOT NULL DEFAULT '',
    primary_bank_account_no TEXT NOT NULL DEFAULT '',
    created_at_epoch_ms INTEGER NOT NULL,
    updated_at_epoch_ms INTEGER NOT NULL
);

-- ============================================================================
-- TABLE 2: RECONCILED INCOME & LEVEL-3 PROVENANCE LINEAGE
-- ============================================================================
CREATE TABLE IF NOT EXISTS income_line_items (
    line_item_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    income_head TEXT NOT NULL CHECK (income_head IN ('SALARY', 'HOUSE_PROPERTY', 'BUSINESS_44ADA', 'CAPITAL_GAINS', 'OTHER_SOURCES', 'FOREIGN_INCOME', 'EXEMPT_INCOME')),
    sub_category TEXT NOT NULL,
    employer_or_deductor_tan TEXT,
    deductor_name TEXT NOT NULL,
    amount_rupees INTEGER NOT NULL,
    tds_deducted_form16 INTEGER NOT NULL DEFAULT 0,
    tds_verified_26as INTEGER NOT NULL DEFAULT 0,
    receipt_quarter_bucket TEXT NOT NULL DEFAULT 'Q_ALL' CHECK (receipt_quarter_bucket IN ('Q_ALL', 'P1_UPTO_15JUN', 'P2_16JUN_15SEP', 'P3_16SEP_15DEC', 'P4_16DEC_15MAR', 'P5_16MAR_31MAR')),
    authoritative_source TEXT NOT NULL CHECK (authoritative_source IN ('FORM_16_PDF', 'FORM_26AS_JSON', 'AIS_JSON', 'AA_BANK_LEDGER', 'USER_OVERRIDE')),
    source_document_sha256 TEXT,
    source_page_number INTEGER,
    source_bounding_box_json TEXT,
    formula_explanation TEXT NOT NULL,
    math_sanity_verified INTEGER NOT NULL DEFAULT 1 CHECK (math_sanity_verified IN (0, 1))
);

-- ============================================================================
-- TABLE 3: SCHEDULE HP — HOUSE PROPERTY & HOME LOAN INTEREST (SEC 24(b))
-- ============================================================================
CREATE TABLE IF NOT EXISTS house_property_schedule_hp (
    property_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    property_type TEXT NOT NULL CHECK (property_type IN ('SELF_OCCUPIED', 'LET_OUT', 'DEEMED_LET_OUT')),
    address_line TEXT NOT NULL,
    city_name TEXT NOT NULL,
    state_code_cbdt TEXT NOT NULL,
    pincode TEXT NOT NULL,
    assessee_share_percentage REAL NOT NULL DEFAULT 100.0,
    co_owner_pan TEXT,
    gross_rent_received_rupees INTEGER NOT NULL DEFAULT 0,
    municipal_tax_paid_rupees INTEGER NOT NULL DEFAULT 0,
    standard_deduction_30pct_24a INTEGER NOT NULL DEFAULT 0,
    interest_borrowed_capital_24b INTEGER NOT NULL DEFAULT 0,
    net_income_or_loss_hp_rupees INTEGER NOT NULL
);

-- ============================================================================
-- TABLE 4: SCHEDULE BP — SECTION 44ADA PRESUMPTIVE FREELANCE LEDGER
-- ============================================================================
CREATE TABLE IF NOT EXISTS presumptive_business_44ada (
    record_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    profession_code_cbdt TEXT NOT NULL DEFAULT '16001',
    business_name TEXT NOT NULL,
    gross_receipts_digital_bank_rupees INTEGER NOT NULL DEFAULT 0,
    gross_receipts_cash_rupees INTEGER NOT NULL DEFAULT 0,
    total_gross_receipts_rupees INTEGER NOT NULL,
    declared_presumptive_profit_rate_pct REAL NOT NULL DEFAULT 50.0 CHECK (declared_presumptive_profit_rate_pct >= 50.0),
    net_presumptive_profit_rupees INTEGER NOT NULL
);

-- ============================================================================
-- TABLE 5: SCHEDULE 112A & SCHEDULE CG — CAPITAL GAINS LOTS (PHASE 2)
-- ============================================================================
CREATE TABLE IF NOT EXISTS capital_gains_112a_lots (
    lot_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    asset_category TEXT NOT NULL CHECK (asset_category IN ('EQUITY_112A_LTCG', 'EQUITY_111A_STCG', 'DEBT_MF_50AA', 'IMMOVABLE_PROPERTY_112', 'FOREIGN_UNLISTED_EQUITY')),
    isin TEXT,
    security_or_property_name TEXT NOT NULL,
    units_quantity REAL NOT NULL DEFAULT 1.0,
    acquisition_date_iso TEXT NOT NULL,
    transfer_date_iso TEXT NOT NULL,
    transfer_quarter_bucket TEXT NOT NULL,
    actual_cost_rupees INTEGER NOT NULL,
    fmv_31_jan_2018_per_unit REAL NOT NULL DEFAULT 0.0,
    grandfathered_or_indexed_cost_rupees INTEGER NOT NULL,
    full_value_consideration_rupees INTEGER NOT NULL,
    transfer_expenses_rupees INTEGER NOT NULL DEFAULT 0,
    sec54_exemption_claimed_rupees INTEGER NOT NULL DEFAULT 0,
    net_taxable_gain_or_loss_rupees INTEGER NOT NULL
);

-- ============================================================================
-- TABLE 6: OFFLINE HISTORICAL JAN 31, 2018 FMV DATABASE
-- ============================================================================
CREATE TABLE IF NOT EXISTS historical_fmv_31jan2018_lookup (
    isin TEXT PRIMARY KEY NOT NULL CHECK (length(isin) = 12),
    bse_nse_scrip_code TEXT NOT NULL,
    security_name TEXT NOT NULL,
    highest_traded_price_31jan2018 REAL NOT NULL
);

-- ============================================================================
-- TABLE 7: OFFICIAL CBDT COST INFLATION INDEX (CII) LOOKUP (2001-02 to 2025-26)
-- ============================================================================
CREATE TABLE IF NOT EXISTS cost_inflation_index_cii (
    financial_year TEXT PRIMARY KEY NOT NULL,
    start_date_iso TEXT NOT NULL,
    end_date_iso TEXT NOT NULL,
    cii_value INTEGER NOT NULL,
    cbdt_notification_ref TEXT NOT NULL
);

-- ============================================================================
-- TABLE 8: SCHEDULE CFL — 8-YEAR LOSS CARRY-FORWARD LEDGER
-- ============================================================================
CREATE TABLE IF NOT EXISTS schedule_cfl_loss_carry_forward (
    cfl_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    loss_origin_assessment_year TEXT NOT NULL,
    date_of_filing_original_return_iso TEXT NOT NULL,
    house_property_loss_bf_rupees INTEGER NOT NULL DEFAULT 0,
    short_term_capital_loss_bf_rupees INTEGER NOT NULL DEFAULT 0,
    long_term_capital_loss_bf_rupees INTEGER NOT NULL DEFAULT 0,
    offset_in_current_ay_rupees INTEGER NOT NULL DEFAULT 0,
    remaining_carried_forward_rupees INTEGER NOT NULL DEFAULT 0
);

-- ============================================================================
-- TABLE 9: CHAPTER VI-A DEDUCTIONS & SECTION 80G DONEE SCHEDULE
-- ============================================================================
CREATE TABLE IF NOT EXISTS chapter_via_deductions_and_80g (
    deduction_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    section_code TEXT NOT NULL,
    gross_amount_paid_rupees INTEGER NOT NULL,
    cash_component_rupees INTEGER NOT NULL DEFAULT 0,
    qualifying_deductible_rupees INTEGER NOT NULL,
    donee_or_insurer_name TEXT,
    donee_pan TEXT,
    donee_80g_limit_category TEXT,
    proof_document_sha256 TEXT
);

-- ============================================================================
-- TABLE 10: BUNDLED 10-YEAR OFFLINE SBI TELEGRAPHIC TRANSFER BUYING RATE (TTBR)
-- ============================================================================
CREATE TABLE IF NOT EXISTS sbi_ttbr_exchange_rates (
    rate_date_iso TEXT NOT NULL,
    currency_code TEXT NOT NULL DEFAULT 'USD',
    tt_buying_rate_inr REAL NOT NULL,
    is_month_end_business_day INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (rate_date_iso, currency_code)
);

-- ============================================================================
-- TABLE 11: SCHEDULE FA FOREIGN ASSETS (CALENDAR YEAR JAN 1 – DEC 31)
-- ============================================================================
CREATE TABLE IF NOT EXISTS schedule_fa_equity_holdings (
    holding_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    calendar_year INTEGER NOT NULL,
    country_code_cbdt TEXT NOT NULL DEFAULT '2',
    entity_name TEXT NOT NULL,
    entity_address TEXT NOT NULL,
    zip_code TEXT NOT NULL,
    nature_of_entity TEXT NOT NULL DEFAULT 'Company',
    date_of_acquiring_interest_iso TEXT NOT NULL,
    initial_value_usd REAL NOT NULL,
    initial_date_ttbr REAL NOT NULL,
    initial_value_inr INTEGER NOT NULL,
    peak_value_usd REAL NOT NULL,
    peak_date_iso TEXT NOT NULL,
    peak_date_ttbr REAL NOT NULL,
    peak_value_inr INTEGER NOT NULL,
    closing_value_usd REAL NOT NULL,
    closing_dec31_ttbr REAL NOT NULL,
    closing_value_inr INTEGER NOT NULL,
    gross_dividend_paid_inr INTEGER NOT NULL DEFAULT 0,
    foreign_tax_withheld_inr INTEGER NOT NULL DEFAULT 0,
    allowable_ftc_rule128_inr INTEGER NOT NULL DEFAULT 0,
    gross_sale_proceeds_inr INTEGER NOT NULL DEFAULT 0
);

-- ============================================================================
-- TABLE 12: SCHEDULE IT (CHALLAN 280) & SCHEDULE TDS1/TDS2/TCS CREDITS
-- ============================================================================
CREATE TABLE IF NOT EXISTS schedule_it_and_tds_credits (
    credit_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    schedule_kind TEXT NOT NULL CHECK (schedule_kind IN ('TDS1_SALARY', 'TDS2_OTHER', 'TDS3_194IA', 'TCS', 'IT_CHALLAN_280')),
    tan_or_pan_of_deductor TEXT,
    deductor_name TEXT,
    gross_amount_subject_to_tds_rupees INTEGER NOT NULL DEFAULT 0,
    tds_claimed_this_year_rupees INTEGER NOT NULL DEFAULT 0,
    bsr_code_7digits TEXT CHECK (bsr_code_7digits IS NULL OR length(bsr_code_7digits) = 7),
    challan_deposit_date_iso TEXT,
    challan_serial_no_5digits TEXT CHECK (challan_serial_no_5digits IS NULL OR length(challan_serial_no_5digits) = 5),
    self_assessment_or_advance_tax_rupees INTEGER NOT NULL DEFAULT 0
);

-- ============================================================================
-- TABLE 13: MANDATORY STATUTORY CONSENTS & OVERRIDE AUDIT LOG
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_legal_consents_and_overrides (
    audit_id TEXT PRIMARY KEY NOT NULL,
    pan TEXT NOT NULL REFERENCES taxpayer_profile(pan) ON DELETE CASCADE,
    event_category TEXT NOT NULL CHECK (event_category IN (
        'DPDP_SECTION_6_CONSENT',
        'CLIENT_SIDE_WEBVIEW_AUTH',
        'SECTION_270A_INCOME_OVERRIDE_WARNING',
        'BLACK_MONEY_ACT_FA_ACKNOWLEDGMENT',
        'SECTION_140_VERIFICATION_DECLARATION',
        'VAULT_SHRED_REQUEST'
    )),
    consent_or_warning_version TEXT NOT NULL,
    language_displayed TEXT NOT NULL CHECK (language_displayed IN ('en-IN', 'hi-IN', 'gu-IN', 'mr-IN', 'ta-IN', 'te-IN')),
    affected_schedule_or_field TEXT,
    system_computed_value_rupees INTEGER,
    user_overridden_value_rupees INTEGER,
    statutory_reason_code TEXT,
    exact_warning_text_hash_sha256 TEXT NOT NULL,
    device_monotonic_timestamp_ms INTEGER NOT NULL,
    wall_clock_utc_iso TEXT NOT NULL
);

-- ============================================================================
-- PERFORMANCE INDEXES
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_income_pan_head_tan ON income_line_items(pan, income_head, employer_or_deductor_tan);
CREATE INDEX IF NOT EXISTS idx_cg_lots_pan_isin ON capital_gains_112a_lots(pan, isin);
CREATE INDEX IF NOT EXISTS idx_sbi_ttbr_date_curr ON sbi_ttbr_exchange_rates(rate_date_iso, currency_code);
CREATE INDEX IF NOT EXISTS idx_schedule_fa_pan_cy ON schedule_fa_equity_holdings(pan, calendar_year);
CREATE INDEX IF NOT EXISTS idx_audit_pan_ts ON user_legal_consents_and_overrides(pan, device_monotonic_timestamp_ms DESC);

-- ============================================================================
-- SEED: COST INFLATION INDEX (CII) 25 STATUTORY YEARS (2001-02 to 2025-26)
-- ============================================================================
INSERT OR REPLACE INTO cost_inflation_index_cii (financial_year, start_date_iso, end_date_iso, cii_value, cbdt_notification_ref) VALUES
    ('2001-02', '2001-04-01', '2002-03-31', 100, 'CBDT Notification No. 44/2017'),
    ('2002-03', '2002-04-01', '2003-03-31', 105, 'CBDT Notification No. 44/2017'),
    ('2003-04', '2003-04-01', '2004-03-31', 109, 'CBDT Notification No. 44/2017'),
    ('2004-05', '2004-04-01', '2005-03-31', 113, 'CBDT Notification No. 44/2017'),
    ('2005-06', '2005-04-01', '2006-03-31', 117, 'CBDT Notification No. 44/2017'),
    ('2006-07', '2006-04-01', '2007-03-31', 122, 'CBDT Notification No. 44/2017'),
    ('2007-08', '2007-04-01', '2008-03-31', 129, 'CBDT Notification No. 44/2017'),
    ('2008-09', '2008-04-01', '2009-03-31', 137, 'CBDT Notification No. 44/2017'),
    ('2009-10', '2009-04-01', '2010-03-31', 148, 'CBDT Notification No. 44/2017'),
    ('2010-11', '2010-04-01', '2011-03-31', 167, 'CBDT Notification No. 44/2017'),
    ('2011-12', '2011-04-01', '2012-03-31', 184, 'CBDT Notification No. 44/2017'),
    ('2012-13', '2012-04-01', '2013-03-31', 200, 'CBDT Notification No. 44/2017'),
    ('2013-14', '2013-04-01', '2014-03-31', 220, 'CBDT Notification No. 44/2017'),
    ('2014-15', '2014-04-01', '2015-03-31', 240, 'CBDT Notification No. 44/2017'),
    ('2015-16', '2015-04-01', '2016-03-31', 254, 'CBDT Notification No. 44/2017'),
    ('2016-17', '2016-04-01', '2017-03-31', 264, 'CBDT Notification No. 44/2017'),
    ('2017-18', '2017-04-01', '2018-03-31', 272, 'CBDT Notification No. 44/2017'),
    ('2018-19', '2018-04-01', '2019-03-31', 280, 'CBDT Notification No. 25/2018'),
    ('2019-20', '2019-04-01', '2020-03-31', 289, 'CBDT Notification No. 63/2019'),
    ('2020-21', '2020-04-01', '2021-03-31', 301, 'CBDT Notification No. 85/2020'),
    ('2021-22', '2021-04-01', '2022-03-31', 317, 'CBDT Notification No. 101/2021'),
    ('2022-23', '2022-04-01', '2023-03-31', 331, 'CBDT Notification No. 62/2022'),
    ('2023-24', '2023-04-01', '2024-03-31', 348, 'CBDT Notification No. 39/2023'),
    ('2024-25', '2024-04-01', '2025-03-31', 363, 'CBDT Notification No. 41/2024'),
    ('2025-26', '2025-04-01', '2026-03-31', 376, 'CBDT Notification S.O. 2025');

-- ============================================================================
-- SEED: HISTORICAL FMV AS ON JAN 31, 2018 BENCHMARK LOOKUP
-- ============================================================================
INSERT OR REPLACE INTO historical_fmv_31jan2018_lookup (isin, bse_nse_scrip_code, security_name, highest_traded_price_31jan2018) VALUES
    ('INE002A01018', 'RELIANCE', 'Reliance Industries Limited', 961.50),
    ('INE467B01029', 'TCS', 'Tata Consultancy Services Limited', 1558.00),
    ('INE009A01021', 'INFY', 'Infosys Limited', 580.45),
    ('INE040A01034', 'HDFCBANK', 'HDFC Bank Limited', 1003.50);

-- ============================================================================
-- SEED: SBI TELEGRAPHIC TRANSFER BUYING RATE (TTBR) BENCHMARKS
-- ============================================================================
INSERT OR REPLACE INTO sbi_ttbr_exchange_rates (rate_date_iso, currency_code, tt_buying_rate_inr, is_month_end_business_day) VALUES
    ('2025-03-14', 'USD', 86.80, 0),
    ('2025-08-14', 'USD', 86.95, 0),
    ('2025-10-31', 'USD', 84.10, 1),
    ('2025-12-31', 'USD', 85.65, 1);

