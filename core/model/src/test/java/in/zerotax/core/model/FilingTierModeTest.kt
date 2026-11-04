package `in`.zerotax.core.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FilingTierModeTest {

    @Test
    fun autoDetectTier_smallSalariedTaxpayer_resolvesSahajSimple() {
        val tier = FilingTierMode.autoDetectTier(
            grossIncome = 12_00_000L,
            hasCapitalGains = false,
            hasPresumptiveBusiness = false,
            hasForeignAssets = false,
            hasAuditExposure = false
        )
        assertEquals(FilingTierMode.SAHAJ_SIMPLE, tier)
        assertEquals("Sahaj Simple", tier.title)
        assertEquals("🌱", tier.badgeIcon)
        assertTrue(tier.recommendedItrForms.contains(ItrFormType.ITR_1_SAHAJ))
    }

    @Test
    fun autoDetectTier_investorWithCapitalGains_resolvesInvestorPro() {
        val tier = FilingTierMode.autoDetectTier(
            grossIncome = 15_00_000L,
            hasCapitalGains = true,
            hasPresumptiveBusiness = false,
            hasForeignAssets = false,
            hasAuditExposure = false
        )
        assertEquals(FilingTierMode.INVESTOR_PRO, tier)
        assertEquals("Growth & Investor", tier.title)
        assertEquals("📈", tier.badgeIcon)
        assertTrue(tier.recommendedItrForms.contains(ItrFormType.ITR_2))
    }

    @Test
    fun autoDetectTier_presumptiveConsultant44ADA_resolvesInvestorPro() {
        val tier = FilingTierMode.autoDetectTier(
            grossIncome = 18_00_000L,
            hasCapitalGains = false,
            hasPresumptiveBusiness = true,
            hasForeignAssets = false,
            hasAuditExposure = false
        )
        assertEquals(FilingTierMode.INVESTOR_PRO, tier)
        assertTrue(tier.recommendedItrForms.contains(ItrFormType.ITR_4_SUGAM))
    }

    @Test
    fun autoDetectTier_highEarnerAbove20Lakhs_resolvesInvestorPro() {
        val tier = FilingTierMode.autoDetectTier(
            grossIncome = 35_00_000L,
            hasCapitalGains = false,
            hasPresumptiveBusiness = false,
            hasForeignAssets = false,
            hasAuditExposure = false
        )
        assertEquals(FilingTierMode.INVESTOR_PRO, tier)
    }

    @Test
    fun autoDetectTier_superHniAbove50Lakhs_resolvesEnterpriseAudit() {
        val tier = FilingTierMode.autoDetectTier(
            grossIncome = 75_00_000L,
            hasCapitalGains = false,
            hasPresumptiveBusiness = false,
            hasForeignAssets = false,
            hasAuditExposure = false
        )
        assertEquals(FilingTierMode.ENTERPRISE_AUDIT, tier)
        assertEquals("Enterprise Audit Shield", tier.title)
        assertEquals("🛡️", tier.badgeIcon)
    }

    @Test
    fun autoDetectTier_foreignAssetsScheduleFA_resolvesEnterpriseAuditRegardlessOfIncome() {
        val tier = FilingTierMode.autoDetectTier(
            grossIncome = 8_00_000L,
            hasCapitalGains = false,
            hasPresumptiveBusiness = false,
            hasForeignAssets = true,
            hasAuditExposure = false
        )
        assertEquals(FilingTierMode.ENTERPRISE_AUDIT, tier)
        assertTrue(tier.targetPersona.contains("Schedule FA foreign assets"))
    }

    @Test
    fun autoDetectTier_auditExposure44AB_resolvesEnterpriseAudit() {
        val tier = FilingTierMode.autoDetectTier(
            grossIncome = 15_00_000L,
            hasCapitalGains = false,
            hasPresumptiveBusiness = false,
            hasForeignAssets = false,
            hasAuditExposure = true
        )
        assertEquals(FilingTierMode.ENTERPRISE_AUDIT, tier)
        assertTrue(tier.recommendedItrForms.contains(ItrFormType.UNSUPPORTED_REQUIRES_CA_AUDIT_44AB))
    }

    @Test
    fun taxPaymentChannel_verifiesAllPaymentModesAndAttributes() {
        val channels = TaxPaymentChannel.values()
        assertEquals(5, channels.size)

        val upi = TaxPaymentChannel.UPI_INSTANT
        assertEquals("Instant UPI (GPay / PhonePe / BHIM / Paytm)", upi.title)
        assertEquals("⚡", upi.iconEmoji)
        assertEquals("Instant (Real-time)", upi.processingTime)

        val netBanking = TaxPaymentChannel.NET_BANKING
        assertEquals("🏦", netBanking.iconEmoji)

        val debitCard = TaxPaymentChannel.DEBIT_CARD
        assertEquals("💳", debitCard.iconEmoji)

        val rtgsNeft = TaxPaymentChannel.RTGS_NEFT
        assertEquals("🏛️", rtgsNeft.iconEmoji)

        val otc = TaxPaymentChannel.OVER_THE_COUNTER
        assertEquals("📝", otc.iconEmoji)
    }
}

