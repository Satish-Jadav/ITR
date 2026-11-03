package in.zerotax.feature.portalbridge

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBackupSyncManagerTest {

    @Test
    fun testPackageStateAndSerialization() {
        val snapshot = CloudBackupSyncManager.packageState(
            pan = "ABCDE1234F",
            grossSalaryRupees = 15_00_000L,
            totalDeductionsRupees = 3_50_000L,
            netTaxableIncomeRupees = 11_50_000L,
            totalTaxLiabilityRupees = 0L,
            refundDueRupees = 60_000L,
            bankName = "State Bank of India",
            bankAccountMasked = "••••1234",
            consentSha256Hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )

        assertEquals("ABCDE1234F", snapshot.pan)
        assertEquals(15_00_000L, snapshot.grossSalaryRupees)
        assertEquals(3_50_000L, snapshot.totalDeductionsRupees)
        assertEquals(60_000L, snapshot.refundDueRupees)
        assertTrue(snapshot.encryptedSessionPayloadBase64.isNotEmpty())

        val json = snapshot.toJsonString()
        val restored = CloudReturnStateSnapshot.fromJsonString(json)

        assertEquals(snapshot.pan, restored.pan)
        assertEquals(snapshot.grossSalaryRupees, restored.grossSalaryRupees)
        assertEquals(snapshot.totalDeductionsRupees, restored.totalDeductionsRupees)
        assertEquals(snapshot.refundDueRupees, restored.refundDueRupees)
        assertEquals(snapshot.bankName, restored.bankName)
        assertEquals(snapshot.consentSha256Hash, restored.consentSha256Hash)
    }

    @Test
    fun testCloudSyncPreparation() = runBlocking {
        val snapshot = CloudBackupSyncManager.packageState(
            pan = "XYZPE5678K",
            grossSalaryRupees = 12_75_000L,
            totalDeductionsRupees = 75_000L,
            netTaxableIncomeRupees = 12_00_000L,
            totalTaxLiabilityRupees = 0L,
            refundDueRupees = 45_000L,
            bankName = "HDFC Bank",
            bankAccountMasked = "••••8819",
            consentSha256Hash = "8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4"
        )

        val result = CloudBackupSyncManager.syncStateToCloud(
            context = null,
            snapshot = snapshot,
            targetApiEndpoint = "https://api.zerotax.in/v1/tax/backup-sync"
        )

        assertTrue(result.isSuccessful)
        assertTrue(result.recoveryReady)
        assertNotNull(result.syncId)
        assertTrue(result.syncId!!.startsWith("SYNC-"))
        assertEquals("https://api.zerotax.in/v1/tax/backup-sync", result.endpointUrl)

        val recovered = CloudBackupSyncManager.checkCrashRecoveryState(null, "XYZPE5678K")
        assertNotNull(recovered)
        assertEquals("XYZPE5678K", recovered?.pan)

        val purged = CloudBackupSyncManager.purgeCloudStateOnShred(null)
        assertTrue(purged)
    }
}
