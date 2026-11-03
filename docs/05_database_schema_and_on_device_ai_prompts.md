05 — Encrypted SQLCipher Database Schema, Room DAOs & On-Device Gemini Nano (AICore) Prompts (Consolidated Phases 1–3)
1. Encryption-at-Rest Architecture (SQLCipher 4.5.x + Android Hardware Keystore StrongBox)
All local tax data across Phases 1, 2, and 3 resides inside a single encrypted SQLite database (zero_tax_vault.db) managed by Room and net.zetetic:android-database-sqlcipher.

Hardware-Backed Key Wrapping: On first launch (Tap 1), the app generates a 256-bit AES-GCM key inside the Android Hardware Keystore (setIsStrongBoxBacked(true) with graceful fallback to TEE Keymaster) and uses it to encrypt a 32-byte cryptographically random SQLCipher passphrase.
Zero Raw Aadhaar Persistence: The database schema and pre-write Room interceptors enforce that no 12-digit UIDAI Aadhaar number is ever written to disk. Only the literal string "[Aadhaar Redacted]" is stored.
WAL Mode & Secure Delete: The database initializes with PRAGMA cipher_memory_security = ON;, PRAGMA secure_delete = ON;, and PRAGMA foreign_keys = ON; so deleted rows and freed SQLite pages are immediately zero-filled.


2. Complete 13-Table Consolidated SQLCipher DDL Specification (zero_tax_vault.db)


PRAGMA foreign_keys = ON;
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
    preferred_locale TEXT NOT NULL DEFAULT 'en-IN' CHECK (preferred_locale IN ('en-IN', 'hi-IN', 'gu-IN')),
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
-- TABLE 6: OFFLINE HISTORICAL JAN 31, 2018 FMV DATABASE (~1.2 MB)
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
    language_displayed TEXT NOT NULL CHECK (language_displayed IN ('en-IN', 'hi-IN', 'gu-IN')),
    affected_schedule_or_field TEXT,
    system_computed_value_rupees INTEGER,
    user_overridden_value_rupees INTEGER,
    statutory_reason_code TEXT,
    exact_warning_text_hash_sha256 TEXT NOT NULL,
    device_monotonic_timestamp_ms INTEGER NOT NULL,
    wall_clock_utc_iso TEXT NOT NULL
);

-- ============================================================================
-- 2.1 PERFORMANCE INDEXES & BUNDLED STATIC REFERENCE SEED DDL
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_income_pan_head_tan ON income_line_items(pan, income_head, employer_or_deductor_tan);
CREATE INDEX IF NOT EXISTS idx_cg_lots_pan_isin ON capital_gains_112a_lots(pan, isin);
CREATE INDEX IF NOT EXISTS idx_sbi_ttbr_date_curr ON sbi_ttbr_exchange_rates(rate_date_iso, currency_code);
CREATE INDEX IF NOT EXISTS idx_schedule_fa_pan_cy ON schedule_fa_equity_holdings(pan, calendar_year);
CREATE INDEX IF NOT EXISTS idx_audit_pan_ts ON user_legal_consents_and_overrides(pan, device_monotonic_timestamp_ms DESC);

-- SEED: COST INFLATION INDEX (CII) 25 STATUTORY YEARS (2001-02 to 2025-26)
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

-- SEED: HISTORICAL FMV AS ON JAN 31, 2018 BENCHMARK LOOKUP
INSERT OR REPLACE INTO historical_fmv_31jan2018_lookup (isin, bse_nse_scrip_code, security_name, highest_traded_price_31jan2018) VALUES
    ('INE002A01018', 'RELIANCE', 'Reliance Industries Limited', 961.50),
    ('INE467B01029', 'TCS', 'Tata Consultancy Services Limited', 1558.00),
    ('INE009A01021', 'INFY', 'Infosys Limited', 580.45),
    ('INE040A01034', 'HDFCBANK', 'HDFC Bank Limited', 1003.50);

-- SEED: SBI TELEGRAPHIC TRANSFER BUYING RATE (TTBR) BENCHMARKS
INSERT OR REPLACE INTO sbi_ttbr_exchange_rates (rate_date_iso, currency_code, tt_buying_rate_inr, is_month_end_business_day) VALUES
    ('2025-03-14', 'USD', 86.80, 0),
    ('2025-08-14', 'USD', 86.95, 0),
    ('2025-10-31', 'USD', 84.10, 1),
    ('2025-12-31', 'USD', 85.65, 1);


3. Production Kotlin Room Database & Hardware Keystore SQLCipher Initializer (:core:database)


package in.zerotax.core.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Entity(tableName = "user_legal_consents_and_overrides")
data class LegalAuditEntity(
    @PrimaryKey val audit_id: String,
    val pan: String,
    val event_category: String,
    val consent_or_warning_version: String,
    val language_displayed: String,
    val affected_schedule_or_field: String?,
    val system_computed_value_rupees: Long?,
    val user_overridden_value_rupees: Long?,
    val statutory_reason_code: String?,
    val exact_warning_text_hash_sha256: String,
    val device_monotonic_timestamp_ms: Long,
    val wall_clock_utc_iso: String
)

@Entity(tableName = "income_line_items")
data class IncomeLineItemEntity(
    @PrimaryKey val line_item_id: String,
    val pan: String,
    val income_head: String,
    val sub_category: String,
    val employer_or_deductor_tan: String?,
    val deductor_name: String,
    val amount_rupees: Long,
    val tds_deducted_form16: Long,
    val tds_verified_26as: Long,
    val receipt_quarter_bucket: String,
    val authoritative_source: String,
    val source_document_sha256: String?,
    val source_page_number: Int?,
    val source_bounding_box_json: String?,
    val formula_explanation: String,
    val math_sanity_verified: Int
)

@Entity(tableName = "capital_gains_112a_lots")
data class CapitalGains112ALotEntity(
    @PrimaryKey val lot_id: String,
    val pan: String,
    val asset_category: String,
    val isin: String?,
    val security_or_property_name: String,
    val units_quantity: Double,
    val acquisition_date_iso: String,
    val transfer_date_iso: String,
    val transfer_quarter_bucket: String,
    val actual_cost_rupees: Long,
    val fmv_31_jan_2018_per_unit: Double,
    val grandfathered_or_indexed_cost_rupees: Long,
    val full_value_consideration_rupees: Long,
    val transfer_expenses_rupees: Long,
    val sec54_exemption_claimed_rupees: Long,
    val net_taxable_gain_or_loss_rupees: Long
)

@Entity(tableName = "schedule_fa_equity_holdings")
data class ScheduleFaEquityHoldingEntity(
    @PrimaryKey val holding_id: String,
    val pan: String,
    val calendar_year: Int,
    val country_code_cbdt: String,
    val entity_name: String,
    val entity_address: String,
    val zip_code: String,
    val nature_of_entity: String,
    val date_of_acquiring_interest_iso: String,
    val initial_value_usd: Double,
    val initial_date_ttbr: Double,
    val initial_value_inr: Long,
    val peak_value_usd: Double,
    val peak_date_iso: String,
    val peak_date_ttbr: Double,
    val peak_value_inr: Long,
    val closing_value_usd: Double,
    val closing_dec31_ttbr: Double,
    val closing_value_inr: Long,
    val gross_dividend_paid_inr: Long,
    val foreign_tax_withheld_inr: Long,
    val allowable_ftc_rule128_inr: Long,
    val gross_sale_proceeds_inr: Long
)

@Dao
interface LegalAuditDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAuditRecord(record: LegalAuditEntity)

    @Query("SELECT * FROM user_legal_consents_and_overrides WHERE pan = :pan ORDER BY device_monotonic_timestamp_ms DESC")
    suspend fun getAuditTrailForPan(pan: String): List<LegalAuditEntity>
}

@Dao
interface SbiTtbrAndCiiLookupDao {
    @Query("""
        SELECT tt_buying_rate_inr FROM sbi_ttbr_exchange_rates
        WHERE currency_code = :currency AND rate_date_iso <= :targetIsoDate
        ORDER BY rate_date_iso DESC LIMIT 1
    """)
    suspend fun getTtbrOnOrBeforeDate(targetIsoDate: String, currency: String = "USD"): Double?

    @Query("SELECT cii_value FROM cost_inflation_index_cii WHERE financial_year = :fy LIMIT 1")
    suspend fun getCiiForFinancialYear(fy: String): Int?
}

@Dao
interface TaxVaultSummaryDao {
    @Query("SELECT * FROM income_line_items WHERE pan = :pan ORDER BY line_item_id ASC")
    suspend fun getIncomeLineItemsForPan(pan: String): List<IncomeLineItemEntity>

    @Query("SELECT * FROM capital_gains_112a_lots WHERE pan = :pan ORDER BY transfer_date_iso ASC")
    suspend fun getCapitalGainsLotsForPan(pan: String): List<CapitalGains112ALotEntity>

    @Query("SELECT * FROM schedule_fa_equity_holdings WHERE pan = :pan AND calendar_year = :calendarYear")
    suspend fun getScheduleFaHoldingsForYear(pan: String, calendarYear: Int): List<ScheduleFaEquityHoldingEntity>

    @Query("SELECT highest_traded_price_31jan2018 FROM historical_fmv_31jan2018_lookup WHERE isin = :isin LIMIT 1")
    suspend fun getHistoricalFmvForIsin(isin: String): Double?
}

@Database(
    entities = [
        LegalAuditEntity::class,
        IncomeLineItemEntity::class,
        CapitalGains112ALotEntity::class,
        ScheduleFaEquityHoldingEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class ZeroTaxVaultDatabase : RoomDatabase() {
    abstract fun legalAuditDao(): LegalAuditDao
    abstract fun sbiTtbrAndCiiLookupDao(): SbiTtbrAndCiiLookupDao
    abstract fun taxVaultSummaryDao(): TaxVaultSummaryDao
}

object SqlCipherHardwareVaultBuilder {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS = "zero_tax_sqlcipher_master_key_v1"
    private const val PREFS_FILE = "zero_tax_wrapped_passphrase_prefs"
    private const val PREFS_KEY_CIPHERTEXT = "wrapped_sqlcipher_passphrase"
    private const val PREFS_KEY_IV = "wrapped_sqlcipher_iv"

    fun buildEncryptedDatabase(context: Context, klass: Class<out RoomDatabase>): RoomDatabase {
        val rawPassphrase = getOrCreateHardwareWrappedPassphrase(context)
        val supportFactory = SupportFactory(rawPassphrase, null, true)

        return Room.databaseBuilder(context.applicationContext, klass, "zero_tax_vault.db")
            .openHelperFactory(supportFactory)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA cipher_memory_security = ON;")
                    db.execSQL("PRAGMA secure_delete = ON;")
                    db.execSQL("PRAGMA foreign_keys = ON;")
                }
            })
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    private fun getOrCreateHardwareWrappedPassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val secretKey = getOrCreateHardwareSecretKey()

        val existingHexCipher = prefs.getString(PREFS_KEY_CIPHERTEXT, null)
        val existingHexIv = prefs.getString(PREFS_KEY_IV, null)

        if (existingHexCipher != null && existingHexIv != null) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, hexToBytes(existingHexIv)))
            return cipher.doFinal(hexToBytes(existingHexCipher))
        }

        val newPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val wrappedBytes = cipher.doFinal(newPassphrase)
        val ivBytes = cipher.iv

        prefs.edit()
            .putString(PREFS_KEY_CIPHERTEXT, bytesToHex(wrappedBytes))
            .putString(PREFS_KEY_IV, bytesToHex(ivBytes))
            .commit()

        return newPassphrase
    }

    private fun getOrCreateHardwareSecretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        ks.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val specBuilder = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        return try {
            specBuilder.setIsStrongBoxBacked(true)
            keyGenerator.init(specBuilder.build())
            keyGenerator.generateKey()
        } catch (_: Exception) {
            specBuilder.setIsStrongBoxBacked(false)
            keyGenerator.init(specBuilder.build())
            keyGenerator.generateKey()
        }
    }

    private fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    private fun hexToBytes(hex: String): ByteArray =
        hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}


4. Consolidated On-Device Gemini Nano (AICore) Prompt Catalog
4.1 Prompt P1-PROMPT-01: Form 16 Part B JSON Structurer (temperature = 0.0f, topK = 1)

SYSTEM INSTRUCTION (Role A - Local Document Structuring Parser):
You are an on-device JSON parser running inside Android AICore.
STRICT DETERMINISTIC RULES:
1. NEVER perform arithmetic, addition, subtraction, or percentage calculations.
2. Extract integer Rupee figures exactly as printed in the OCR text (strip commas, "Rs.", "₹", and ".00").
3. If any 12-digit Aadhaar number appears in the text, output "[Aadhaar Redacted]".
4. Output ONLY a single valid JSON object conforming to the schema below. Do not wrap in markdown explanations.

Schema:
{
  "document_type": "FORM_16_PART_B",
  "employer_or_issuer_tan": "string",
  "employer_or_issuer_pan": null,
  "gross_salary_sec17_1": 0,
  "perquisites_sec17_2": 0,
  "profits_in_lieu_sec17_3": 0,
  "total_gross_salary_printed": 0,
  "exempt_hra_sec10_13a": 0,
  "exempt_lta_sec10_5": 0,
  "standard_deduction_sec16_ia": 0,
  "professional_tax_sec16_iii": 0,
  "net_taxable_salary_printed": 0,
  "deduction_80c_total": 0,
  "deduction_80ccd_1b_self_nps": 0,
  "deduction_80ccd_2_employer_nps": 0,
  "deduction_80d_health_insurance": 0
}

-- FEW-SHOT INPUT EXAMPLE --
Certificate No: F16-2025-091
Employer TAN: BLRI12345E
Employee Aadhaar: 9876 5432 1098
Gross Salary u/s 17(1): Rs. 18,50,000.00
Perquisites u/s 17(2): Rs. 45,000.00
Profits in lieu u/s 17(3): 0
Total Gross: 18,95,000
HRA Exemption u/s 10(13A): Rs. 1,20,000
Standard Deduction u/s 16(ia): 50,000
Professional Tax u/s 16(iii): 2,400
Net Taxable Salary: 17,22,600
Deductions under Section 80C: 1,50,000
Deductions under Section 80CCD(1B): 50,000
Section 80D: 25,000

-- EXACT EXPECTED JSON OUTPUT --
{
  "document_type": "FORM_16_PART_B",
  "employer_or_issuer_tan": "BLRI12345E",
  "employer_or_issuer_pan": null,
  "gross_salary_sec17_1": 1850000,
  "perquisites_sec17_2": 45000,
  "profits_in_lieu_sec17_3": 0,
  "total_gross_salary_printed": 1895000,
  "exempt_hra_sec10_13a": 120000,
  "exempt_lta_sec10_5": 0,
  "standard_deduction_sec16_ia": 50000,
  "professional_tax_sec16_iii": 2400,
  "net_taxable_salary_printed": 1722600,
  "deduction_80c_total": 150000,
  "deduction_80ccd_1b_self_nps": 50000,
  "deduction_80ccd_2_employer_nps": 0,
  "deduction_80d_health_insurance": 25000
}
4.2 Prompt P2-PROMPT-01: Account Aggregator Bank Statement Narration Classifier

Task: Classify Indian bank statement narration strings into Income Tax sections.
Pre-filter: Disregard personal UPI debit transactions below ₹500.
Allowed Sections: ["80C_LIC_PPF_ELSS", "80D_HEALTH_INS", "80G_DONATION", "44ADA_FREELANCE_INFLOW", "SAVINGS_INTEREST", "IGNORE"]
Output ONLY a JSON array of objects: [{"id": number, "section": "string", "confidence": 0.0}]

Input Transactions:
{{BATCHED_BANK_NARRATIONS_JSON}}

-- FEW-SHOT INPUT EXAMPLE --
[
  {"id": 101, "narration": "UPI/502931/TeaStall/45.00/DR"},
  {"id": 102, "narration": "ACH D- SBI LIFE INS PREMIUM 002938192"},
  {"id": 103, "narration": "NEFT INW: ACME INC CONSULTING FEB25"},
  {"id": 104, "narration": "INT.PD: 01-10-2024 TO 31-12-2024"},
  {"id": 105, "narration": "BILLDESK STAR HEALTH AND ALLIED INSUR"}
]

-- EXACT EXPECTED JSON OUTPUT --
[
  {"id": 101, "section": "IGNORE", "confidence": 0.99},
  {"id": 102, "section": "80C_LIC_PPF_ELSS", "confidence": 0.96},
  {"id": 103, "section": "44ADA_FREELANCE_INFLOW", "confidence": 0.92},
  {"id": 104, "section": "SAVINGS_INTEREST", "confidence": 0.98},
  {"id": 105, "section": "80D_HEALTH_INS", "confidence": 0.95}
]
4.3 Prompt P3-PROMPT-01: Grounded Multilingual CPC Notice Explainer (English / Hindi / Gujarati)

Task: Explain this Income Tax Notice variance in simple, reassuring {{TARGET_LANGUAGE}} (Gujarati / Hindi / English).
Strict Rule: Keep the exact token strings {{NOTICE_SECTION}}, {{VARIANCE_AMOUNT}}, and {{ACTION_DEADLINE}} unchanged so the deterministic UI renderer can inject verified numbers.

Input Variance JSON:
{
  "notice_type": "{{NOTICE_SECTION}}",
  "discrepancy_reason": "AIS reported FD Interest from bank not included in Schedule OS",
  "tax_demand_amount": "{{VARIANCE_AMOUNT}}",
  "recommended_fix": "File Revised Return under Section 139(5) including FD interest and TDS credit"
}

Output JSON Schema:
{
  "headline_translated": "string",
  "what_happened_simple": "string",
  "step_by_step_fix": "string"
}

-- FEW-SHOT TOKEN-LOCKED OUTPUT EXAMPLE 1 (Hindi hi-IN) --
{
  "headline_translated": "आयकर नोटिस {{NOTICE_SECTION}} संबंधी स्पष्टीकरण",
  "what_happened_simple": "आयकर विभाग के रिकॉर्ड (AIS) में आपके बैंक फिक्स्ड डिपॉजिट (FD) के ब्याज की जानकारी मिली है जो आपके दाखिल रिटर्न में दर्ज नहीं थी। इस कारण {{VARIANCE_AMOUNT}} का टैक्स अंतर आया है। घबराने की कोई बात नहीं है।",
  "step_by_step_fix": "कृपया {{ACTION_DEADLINE}} से पहले धारा 139(5) के तहत संशोधित रिटर्न (Revised Return) दाखिल करें और ब्याज आय तथा उससे कटे टीडीएस (TDS) को जोड़ लें।"
}

-- FEW-SHOT TOKEN-LOCKED OUTPUT EXAMPLE 2 (Gujarati gu-IN) --
{
  "headline_translated": "આવકવેરા નોટિસ {{NOTICE_SECTION}} ની સમજૂતી",
  "what_happened_simple": "આવકવેરા વિભાગના AIS રેકોર્ડમાં તમારી બેંક એફડીનું વ્યાજ દેખાય છે જે રિટર્નમાં ઉમેરાયું ન હતું. તેના કારણે {{VARIANCE_AMOUNT}} ની તફાવત રકમ દર્શાવવામાં આવી છે. ચિંતા કરવાની જરૂર નથી.",
  "step_by_step_fix": "તમે {{ACTION_DEADLINE}} પહેલા કલમ 139(5) હેઠળ સુધારેલું રિટર્ન (Revised Return) સબમિટ કરીને આ વ્યાજ અને તેનું ટીડીએસ ક્રેડિટ જોડી શકો છો."
}

4.4 Prompt P1-PROMPT-02: Physical Rent, 80D Medical & 80G Donation Receipt Extractor (temperature = 0.0f, topK = 1)

Task: Extract tax deduction receipt metadata into strict JSON.
Rules:
1. Output ONLY valid JSON. Do not include markdown fences or explanations.
2. Round all numbers to nearest integer INR. Output 0 if a field is missing.
3. If any 12-digit Aadhaar number appears in the text, output "[Aadhaar Redacted]".

Schema:
{
  "receipt_type": "RENT | 80D_MEDICAL | 80G_DONATION",
  "amount_inr": 0,
  "counterparty_name": "string",
  "counterparty_pan": "string",
  "arn_80g_number": "string",
  "payment_mode": "DIGITAL | CASH",
  "period_from": "YYYY-MM-DD",
  "period_to": "YYYY-MM-DD"
}

Receipt OCR Text:
{{RECEIPT_OCR_TEXT}}

-- FEW-SHOT INPUT EXAMPLE --
RENT RECEIPT
Received a sum of INR 24,000 from Rohit Verma towards house rent for Flat 402, Green Meadows
Period: 01/04/2024 to 30/04/2024
Landlord: Suresh Patel (PAN: ABCPP1234F)
Paid via UPI / NetBanking on 02-04-2024
Landlord Aadhaar: 1234 5678 9012

-- EXACT EXPECTED JSON OUTPUT --
{
  "receipt_type": "RENT",
  "amount_inr": 24000,
  "counterparty_name": "Suresh Patel",
  "counterparty_pan": "ABCPP1234F",
  "arn_80g_number": "",
  "payment_mode": "DIGITAL",
  "period_from": "2024-04-01",
  "period_to": "2024-04-30"
}
4.5 Subfolder-to-Master Schema Consolidation Cross-Reference
To guarantee 100% traceability across the phase subfolder specifications and the unified 13-table zero_tax_vault.db schema:
Phase 1 Subfolder Tables: taxpayer_profile -> Table 1 (taxpayer_profile); salary_employers & income_other_heads -> Table 2 (income_line_items), Table 3 (house_property_schedule_hp), and Table 4 (presumptive_business_44ada); deductions_chap_via -> Table 9 (chapter_via_deductions_and_80g); nudge_cards & statutory overrides -> Table 13 (user_legal_consents_and_overrides).
Phase 2 Subfolder Tables: capital_gain_lots -> Table 5 (capital_gains_112a_lots); ref_fmv_31jan2018 -> Table 6 (historical_fmv_31jan2018_lookup) + Table 7 (cost_inflation_index_cii); loss_carry_forward_schedule -> Table 8 (schedule_cfl_loss_carry_forward); tax_challan_payments -> Table 12 (schedule_it_and_tds_credits).
Phase 3 Subfolder Tables: ref_sbi_ttbr_rates -> Table 10 (sbi_ttbr_exchange_rates); foreign_asset_holdings_fa & foreign_tax_credit_dtaa -> Table 11 (schedule_fa_equity_holdings).

5. CA & Legal Counsel Data Governance & Schema Audit Matrix
DDL-Level Statutory Integrity Constraints:
Table 1 (taxpayer_profile): Enforces CHECK (pan GLOB '[A-Z][A-Z][A-Z]P[A-Z][0-9][0-9][0-9][0-9][A-Z]') so non-individual assessees (HUF, Firm, Company) can never be inserted, and CHECK (aadhaar_masked_token = '[Aadhaar Redacted]') so raw 12-digit Aadhaar numbers are rejected at the SQLite engine layer (Section 29 of the Aadhaar Act, 2016).
Table 2 (income_line_items): Captures receipt_quarter_bucket (P1_UPTO_15JUN through P5_16MAR_31MAR) required by CBDT Schedule CG and Schedule OS to compute accurate Section 234C quarterly installment interest and apply the First Proviso windfall capital gains exemption.
Table 4 (presumptive_business_44ada): Enforces CHECK (declared_presumptive_profit_rate_pct >= 50.0) at the database level so Section 44ADA filings cannot drop below the 50% statutory presumptive profit floor without routing out to a Section 44AB tax audit.
Table 12 (schedule_it_and_tds_credits): Enforces length(bsr_code_7digits) = 7 and length(challan_serial_no_5digits) = 5 to prevent Section 139(9) Category 'A' defective return notices on self-assessment tax challans.
Table 13 (user_legal_consents_and_overrides): Provides an append-only cryptographic audit trail (exact_warning_text_hash_sha256 + device_monotonic_timestamp_ms) proving itemized consent under Section 6 of the DPDP Act, 2023 and explicit taxpayer warning acknowledgment prior to any Section 270A downward income override.
Cryptographic Memory & Deletion Hygiene (SQLCipher 4.5.x):
Executing PRAGMA cipher_memory_security = ON; and PRAGMA secure_delete = ON; inside RoomDatabase.Callback.onOpen() ensures that in-memory page caches are wiped upon release and deleted records are overwritten with zeroes inside the encrypted database file.

6. Deterministic Prompt Guardrails & Offline Reference Database Maintenance
6.1 On-Device AICore (Gemini Nano) Non-Hallucination Invariants
Greedy Decoding Enforcement: All four production prompts (P1-PROMPT-01, P1-PROMPT-02, P2-PROMPT-01, P3-PROMPT-01) execute strictly with temperature = 0.0f and topK = 1 inside com.google.ai.edge.aicore.GenerativeModel.
Pre-Inference UIDAI Scrubbing: Raw OCR text is passed through UidaiAadhaarVerhoeffScrubber.scrubAllAadhaarDigits() prior to prompt construction so no 12-digit Aadhaar number ever enters the NPU context window.
Post-Inference Validation: P1-PROMPT-01 outputs are gated by DeterministicReconciler.passesForm16SanityCheck() (±₹5 tolerance), and P3-PROMPT-01 vernacular notice translations are gated by TokenLockedVernacularExplainer.restoreAndVerifyTokens() to guarantee 100% preservation of {{NOTICE_SECTION}}, {{VARIANCE_AMOUNT}}, and {{ACTION_DEADLINE}} tokens.
