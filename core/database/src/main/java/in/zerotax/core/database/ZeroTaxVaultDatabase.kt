package in.zerotax.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "taxpayer_profile")
data class TaxpayerProfileEntity(
    @PrimaryKey val pan: String,
    val assessment_year: String = "2026-27",
    val full_name: String,
    val date_of_birth_iso: String,
    val citizen_category: String = "REGULAR_BELOW_60",
    val aadhaar_masked_token: String = "[Aadhaar Redacted]",
    val selected_regime: String = "NEW_REGIME_115BAC",
    val auto_routed_itr_form: String = "ITR-1",
    val primary_bank_ifsc: String = "",
    val primary_bank_account_no: String = ""
)

@Entity(tableName = "income_line_items")
data class IncomeLineItemEntity(
    @PrimaryKey val line_item_id: String,
    val pan: String,
    val income_head: String,
    val employer_or_deductor_tan: String?,
    val deductor_name: String,
    val amount_rupees: Long,
    val tds_deducted_form16: Long,
    val tds_verified_26as: Long,
    val authoritative_source: String,
    val math_sanity_verified: Int = 1
)

@Entity(tableName = "user_legal_consents_and_overrides")
data class LegalAuditEntity(
    @PrimaryKey val audit_id: String,
    val pan: String,
    val event_category: String,
    val consent_or_warning_version: String,
    val language_displayed: String,
    val system_computed_value_rupees: Long?,
    val user_overridden_value_rupees: Long?,
    val statutory_reason_code: String?,
    val exact_warning_text_hash_sha256: String,
    val device_monotonic_timestamp_ms: Long
)

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

    // TODO(Phase-2): Add @Query for historical_fmv_31jan2018_lookup (ISIN -> Jan 31, 2018 FMV) and cost_inflation_index_cii.
    // TODO(Phase-2): Add @Dao methods for capital_gains_112a_lots and schedule_cfl_loss_carry_forward (8-year loss ledger).
    // TODO(Phase-3): Add @Query("SELECT tt_buying_rate_inr FROM sbi_ttbr_exchange_rates WHERE rate_date_iso <= :date ORDER BY rate_date_iso DESC LIMIT 1") for Schedule FA & Rule 128(5) Form 67.
}

@Database(
    entities = [
        TaxpayerProfileEntity::class,
        IncomeLineItemEntity::class,
        LegalAuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ZeroTaxVaultDatabase : RoomDatabase() {
    abstract fun taxVaultDao(): TaxVaultDao
}
