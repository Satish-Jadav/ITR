-- ZeroTax Offline Encrypted SQLite/SQLCipher DDL
-- Target: SQLCipher 4.5.4 / SQLite 3.x with Room Mapping

PRAGMA foreign_keys = ON;
PRAGMA secure_delete = ON;

-- 1. Taxpayer Identity and Default Regimes
CREATE TABLE IF NOT EXISTS `taxpayer_profile` (
    `pan` TEXT NOT NULL PRIMARY KEY,
    `assessment_year` TEXT NOT NULL DEFAULT '2026-27',
    `full_name` TEXT NOT NULL,
    `date_of_birth_iso` TEXT NOT NULL,
    `citizen_category` TEXT NOT NULL DEFAULT 'REGULAR_BELOW_60',
    `aadhaar_masked_token` TEXT NOT NULL DEFAULT '[Aadhaar Redacted]',
    `selected_regime` TEXT NOT NULL DEFAULT 'NEW_REGIME_115BAC',
    `auto_routed_itr_form` TEXT NOT NULL DEFAULT 'ITR-1',
    `primary_bank_ifsc` TEXT NOT NULL DEFAULT '',
    `primary_bank_account_no` TEXT NOT NULL DEFAULT ''
);

-- 2. Line-by-Line Canonical Income & Pre-Filled Records
CREATE TABLE IF NOT EXISTS `income_line_items` (
    `line_item_id` TEXT NOT NULL PRIMARY KEY,
    `pan` TEXT NOT NULL,
    `income_head` TEXT NOT NULL,
    `employer_or_deductor_tan` TEXT,
    `deductor_name` TEXT NOT NULL,
    `amount_rupees` INTEGER NOT NULL,
    `tds_deducted_form16` INTEGER NOT NULL DEFAULT 0,
    `tds_verified_26as` INTEGER NOT NULL DEFAULT 0,
    `authoritative_source` TEXT NOT NULL,
    `math_sanity_verified` INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (`pan`) REFERENCES `taxpayer_profile`(`pan`) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS `index_income_line_items_pan` 
ON `income_line_items` (`pan`);

CREATE INDEX IF NOT EXISTS `index_income_line_items_head` 
ON `income_line_items` (`income_head`);

-- 3. Non-Repudiation, Legal Consents & Regulatory Audit Trail
CREATE TABLE IF NOT EXISTS `user_legal_consents_and_overrides` (
    `audit_id` TEXT NOT NULL PRIMARY KEY,
    `pan` TEXT NOT NULL,
    `event_category` TEXT NOT NULL,
    `consent_or_warning_version` TEXT NOT NULL,
    `language_displayed` TEXT NOT NULL,
    `system_computed_value_rupees` INTEGER,
    `user_overridden_value_rupees` INTEGER,
    `statutory_reason_code` TEXT,
    `exact_warning_text_hash_sha256` TEXT NOT NULL,
    `device_monotonic_timestamp_ms` INTEGER NOT NULL,
    FOREIGN KEY (`pan`) REFERENCES `taxpayer_profile`(`pan`) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS `index_legal_audit_pan_timestamp` 
ON `user_legal_consents_and_overrides` (`pan`, `device_monotonic_timestamp_ms`);

-- ============================================================================
-- EXPANSION HOOKS (DDL RESERVATIONS)
-- ============================================================================

-- TODO(Phase-2): CREATE TABLE `historical_fmv_31jan2018_lookup` (
--     `isin` TEXT PRIMARY KEY,
--     `scrip_name` TEXT NOT NULL,
--     `nse_closing_price` INTEGER,
--     `bse_closing_price` INTEGER,
--     `applicable_fmv_paise` INTEGER NOT NULL
-- );

-- TODO(Phase-2): CREATE TABLE `capital_gains_112a_lots` (
--     `lot_id` TEXT PRIMARY KEY,
--     `pan` TEXT NOT NULL,
--     `isin` TEXT NOT NULL,
--     `units` REAL NOT NULL,
--     `purchase_date` TEXT NOT NULL,
--     `sale_date` TEXT NOT NULL,
--     `sale_price_paise` INTEGER NOT NULL,
--     `fmv_31jan2018_paise` INTEGER,
--     `calculated_gain_paise` INTEGER NOT NULL,
--     FOREIGN KEY (`pan`) REFERENCES `taxpayer_profile`(`pan`)
-- );

-- TODO(Phase-2): CREATE TABLE `schedule_cfl_loss_carry_forward` (
--     `ledger_id` TEXT PRIMARY KEY,
--     `pan` TEXT NOT NULL,
--     `origination_assessment_year` TEXT NOT NULL,
--     `loss_type` TEXT NOT NULL,
--     `original_loss_amount` INTEGER NOT NULL,
--     `set_off_current_year` INTEGER NOT NULL DEFAULT 0,
--     `carried_forward_balance` INTEGER NOT NULL,
--     `statutory_expiry_ay` TEXT NOT NULL
-- );

-- TODO(Phase-3): CREATE TABLE `sbi_ttbr_exchange_rates` (
--     `currency_code` TEXT NOT NULL,
--     `rate_date_iso` TEXT NOT NULL,
--     `tt_buying_rate_inr` REAL NOT NULL,
--     PRIMARY KEY (`currency_code`, `rate_date_iso`)
-- );
