package `in`.zerotax.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

// 1. Taxpayer Profile
@Entity(tableName = "taxpayer_profile")
data class TaxpayerProfileEntity(
    @PrimaryKey val pan: String,
    val assessment_year: String = "2026-27",
    val full_name: String,
    val first_name: String = "",
    val last_name: String = "",
    val father_name: String = "",
    val date_of_birth_iso: String,
    val citizen_category: String = "REGULAR_BELOW_60",
    val residential_status: String = "RES_ORDINARILY_RESIDENT",
    val employer_category: String = "OTH",
    val aadhaar_masked_token: String = "[Aadhaar Redacted]",
    val preferred_locale: String = "en-IN",
    val selected_regime: String = "NEW_REGIME_115BAC",
    val auto_routed_itr_form: String = "ITR-1",
    val filing_section: Int = 11,
    val original_ack_no: String? = null,
    val original_filing_date_iso: String? = null,
    val primary_bank_ifsc: String = "",
    val primary_bank_account_no: String = "",
    val created_at_epoch_ms: Long = 0L,
    val updated_at_epoch_ms: Long = 0L
)

// 2. Income Line Items
@Entity(tableName = "income_line_items")
data class IncomeLineItemEntity(
    @PrimaryKey val line_item_id: String,
    val pan: String,
    val income_head: String,
    val sub_category: String = "",
    val employer_or_deductor_tan: String? = null,
    val deductor_name: String,
    val amount_rupees: Long,
    val tds_deducted_form16: Long = 0L,
    val tds_verified_26as: Long = 0L,
    val receipt_quarter_bucket: String = "Q_ALL",
    val authoritative_source: String,
    val source_document_sha256: String? = null,
    val source_page_number: Int? = null,
    val source_bounding_box_json: String? = null,
    val formula_explanation: String = "",
    val math_sanity_verified: Int = 1
)

// 3. House Property Schedule HP
@Entity(tableName = "house_property_schedule_hp")
data class HousePropertyScheduleHpEntity(
    @PrimaryKey val property_id: String,
    val pan: String,
    val property_type: String,
    val address_line: String = "",
    val city_name: String = "",
    val state_code_cbdt: String = "",
    val pincode: String = "",
    val assessee_share_percentage: Double = 100.0,
    val co_owner_pan: String? = null,
    val gross_rent_received_rupees: Long = 0L,
    val municipal_tax_paid_rupees: Long = 0L,
    val standard_deduction_30pct_24a: Long = 0L,
    val interest_borrowed_capital_24b: Long = 0L,
    val net_income_or_loss_hp_rupees: Long = 0L
)

// 4. Presumptive Business 44ADA
@Entity(tableName = "presumptive_business_44ada")
data class PresumptiveBusiness44AdaEntity(
    @PrimaryKey val record_id: String,
    val pan: String,
    val profession_code_cbdt: String = "16001",
    val business_name: String,
    val gross_receipts_digital_bank_rupees: Long = 0L,
    val gross_receipts_cash_rupees: Long = 0L,
    val total_gross_receipts_rupees: Long,
    val declared_presumptive_profit_rate_pct: Double = 50.0,
    val net_presumptive_profit_rupees: Long
)

// 5. Capital Gains 112A Lots
@Entity(tableName = "capital_gains_112a_lots")
data class CapitalGains112ALotEntity(
    @PrimaryKey val lot_id: String,
    val pan: String,
    val asset_category: String,
    val isin: String?,
    val security_or_property_name: String,
    val units_quantity: Double = 1.0,
    val acquisition_date_iso: String,
    val transfer_date_iso: String,
    val transfer_quarter_bucket: String = "",
    val actual_cost_rupees: Long,
    val fmv_31_jan_2018_per_unit: Double = 0.0,
    val grandfathered_or_indexed_cost_rupees: Long = 0L,
    val full_value_consideration_rupees: Long,
    val transfer_expenses_rupees: Long = 0L,
    val sec54_exemption_claimed_rupees: Long = 0L,
    val net_taxable_gain_or_loss_rupees: Long = 0L
)

// 6. Historical Jan 31 2018 FMV Lookup
@Entity(tableName = "historical_fmv_31jan2018_lookup")
data class HistoricalFmv31Jan2018Entity(
    @PrimaryKey val isin: String,
    val bse_nse_scrip_code: String,
    val security_name: String,
    val highest_traded_price_31jan2018: Double
)

// 7. Cost Inflation Index (CII)
@Entity(tableName = "cost_inflation_index_cii")
data class CostInflationIndexCiiEntity(
    @PrimaryKey val financial_year: String,
    val start_date_iso: String,
    val end_date_iso: String,
    val cii_value: Int,
    val cbdt_notification_ref: String
)

// 8. Schedule CFL - Loss Carry-Forward Ledger
@Entity(tableName = "schedule_cfl_loss_carry_forward")
data class ScheduleCflLossCarryForwardEntity(
    @PrimaryKey val cfl_id: String,
    val pan: String,
    val loss_origin_assessment_year: String,
    val date_of_filing_original_return_iso: String,
    val house_property_loss_bf_rupees: Long = 0L,
    val short_term_capital_loss_bf_rupees: Long = 0L,
    val long_term_capital_loss_bf_rupees: Long = 0L,
    val offset_in_current_ay_rupees: Long = 0L,
    val remaining_carried_forward_rupees: Long = 0L
)

// 9. Chapter VI-A & 80G Donee Schedule
@Entity(tableName = "chapter_via_deductions_and_80g")
data class ChapterViaDeductionsAnd80GEntity(
    @PrimaryKey val deduction_id: String,
    val pan: String,
    val section_code: String,
    val gross_amount_paid_rupees: Long,
    val cash_component_rupees: Long = 0L,
    val qualifying_deductible_rupees: Long,
    val donee_or_insurer_name: String? = null,
    val donee_pan: String? = null,
    val donee_80g_limit_category: String? = null,
    val proof_document_sha256: String? = null
)

// 10. SBI TTBR Exchange Rates
@Entity(tableName = "sbi_ttbr_exchange_rates", primaryKeys = ["rate_date_iso", "currency_code"])
data class SbiTtbrExchangeRateEntity(
    val rate_date_iso: String,
    val currency_code: String = "USD",
    val tt_buying_rate_inr: Double,
    val is_month_end_business_day: Int = 0
)

// 11. Schedule FA Foreign Assets
@Entity(tableName = "schedule_fa_equity_holdings")
data class ScheduleFaEquityHoldingEntity(
    @PrimaryKey val holding_id: String,
    val pan: String,
    val calendar_year: Int,
    val country_code_cbdt: String = "2",
    val entity_name: String,
    val entity_address: String,
    val zip_code: String,
    val nature_of_entity: String = "Company",
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
    val gross_dividend_paid_inr: Long = 0L,
    val foreign_tax_withheld_inr: Long = 0L,
    val allowable_ftc_rule128_inr: Long = 0L,
    val gross_sale_proceeds_inr: Long = 0L
)

// 12. Schedule IT & TDS Credits
@Entity(tableName = "schedule_it_and_tds_credits")
data class ScheduleItAndTdsCreditEntity(
    @PrimaryKey val credit_id: String,
    val pan: String,
    val schedule_kind: String,
    val tan_or_pan_of_deductor: String? = null,
    val deductor_name: String? = null,
    val gross_amount_subject_to_tds_rupees: Long = 0L,
    val tds_claimed_this_year_rupees: Long = 0L,
    val bsr_code_7digits: String? = null,
    val challan_deposit_date_iso: String? = null,
    val challan_serial_no_5digits: String? = null,
    val self_assessment_or_advance_tax_rupees: Long = 0L
)

// 13. Legal Audit Trail
@Entity(tableName = "user_legal_consents_and_overrides")
data class LegalAuditEntity(
    @PrimaryKey val audit_id: String,
    val pan: String,
    val event_category: String,
    val consent_or_warning_version: String,
    val language_displayed: String,
    val affected_schedule_or_field: String? = null,
    val system_computed_value_rupees: Long? = null,
    val user_overridden_value_rupees: Long? = null,
    val statutory_reason_code: String? = null,
    val exact_warning_text_hash_sha256: String,
    val device_monotonic_timestamp_ms: Long,
    val wall_clock_utc_iso: String = ""
)

// DAOs
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

@Dao
interface TaxVaultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: TaxpayerProfileEntity)

    @Query("SELECT * FROM taxpayer_profile WHERE pan = :pan LIMIT 1")
    suspend fun getProfile(pan: String): TaxpayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncomeItems(items: List<IncomeLineItemEntity>)

    @Query("SELECT * FROM income_line_items WHERE pan = :pan")
    suspend fun getIncomeItems(pan: String): List<IncomeLineItemEntity>

    @Query("DELETE FROM income_line_items WHERE line_item_id = :lineItemId")
    suspend fun deleteIncomeItemById(lineItemId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAuditRecord(record: LegalAuditEntity)

    @Query("SELECT * FROM user_legal_consents_and_overrides WHERE pan = :pan ORDER BY device_monotonic_timestamp_ms ASC")
    suspend fun getAuditTrailForPan(pan: String): List<LegalAuditEntity>

    @Query("SELECT highest_traded_price_31jan2018 FROM historical_fmv_31jan2018_lookup WHERE isin = :isin LIMIT 1")
    suspend fun getHistoricalFmvForIsin(isin: String): Double?

    @Query("SELECT cii_value FROM cost_inflation_index_cii WHERE financial_year = :fy LIMIT 1")
    suspend fun getCiiForFinancialYear(fy: String): Int?

    @Query("""
        SELECT tt_buying_rate_inr FROM sbi_ttbr_exchange_rates
        WHERE currency_code = :currency AND rate_date_iso <= :targetIsoDate
        ORDER BY rate_date_iso DESC LIMIT 1
    """)
    suspend fun getTtbrOnOrBeforeDate(targetIsoDate: String, currency: String = "USD"): Double?
}

@Database(
    entities = [
        TaxpayerProfileEntity::class,
        IncomeLineItemEntity::class,
        HousePropertyScheduleHpEntity::class,
        PresumptiveBusiness44AdaEntity::class,
        CapitalGains112ALotEntity::class,
        HistoricalFmv31Jan2018Entity::class,
        CostInflationIndexCiiEntity::class,
        ScheduleCflLossCarryForwardEntity::class,
        ChapterViaDeductionsAnd80GEntity::class,
        SbiTtbrExchangeRateEntity::class,
        ScheduleFaEquityHoldingEntity::class,
        ScheduleItAndTdsCreditEntity::class,
        LegalAuditEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ZeroTaxVaultDatabase : RoomDatabase() {
    abstract fun taxVaultDao(): TaxVaultDao
    abstract fun legalAuditDao(): LegalAuditDao
    abstract fun sbiTtbrAndCiiLookupDao(): SbiTtbrAndCiiLookupDao
    abstract fun taxVaultSummaryDao(): TaxVaultSummaryDao
}
