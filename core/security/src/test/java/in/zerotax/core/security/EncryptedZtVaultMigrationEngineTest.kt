package `in`.zerotax.core.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EncryptedZtVaultMigrationEngineTest {

    @Test
    fun exportAndImportVault_successfulRoundTrip() {
        val originalJson = """{"pan":"ABCDE1234F","cflLosses":[{"ay":"2024-25","stcl":45000}],"note":"ZeroTax Local Vault"}"""
        val metadata = ZtVaultBundleMetadata(
            pan = "ABCDE1234F",
            dob = "15/08/1990",
            assessmentYearsIncluded = listOf("2024-25", "2025-26", "2026-27"),
            payloadJson = originalJson
        )

        val passcode = "SecretPasscode@2026"

        // Export bundle
        val encryptedBundle = EncryptedZtVaultMigrationEngine.exportVaultBundle(metadata, passcode)
        assertTrue(encryptedBundle.isNotEmpty())

        // Import bundle with correct credentials
        val result = EncryptedZtVaultMigrationEngine.importVaultBundle(
            vaultBytes = encryptedBundle,
            pan = "ABCDE1234F",
            dob = "15/08/1990",
            userPasscode = passcode
        )

        assertTrue(result.isSuccess)
        assertEquals(originalJson, result.payloadJson)
    }

    @Test
    fun importVault_wrongPasscode_failsAuthentication() {
        val originalJson = """{"pan":"ABCDE1234F","balance":100000}"""
        val metadata = ZtVaultBundleMetadata(
            pan = "ABCDE1234F",
            dob = "15/08/1990",
            assessmentYearsIncluded = listOf("2026-27"),
            payloadJson = originalJson
        )

        val passcode = "CorrectPasscode"
        val encryptedBundle = EncryptedZtVaultMigrationEngine.exportVaultBundle(metadata, passcode)

        // Attempt import with wrong passcode
        val result = EncryptedZtVaultMigrationEngine.importVaultBundle(
            vaultBytes = encryptedBundle,
            pan = "ABCDE1234F",
            dob = "15/08/1990",
            userPasscode = "WrongPasscode"
        )

        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("Authentication failed") == true)
    }

    @Test
    fun importVault_invalidHeader_failsValidation() {
        val corruptedBytes = "CORRUPTED_NOT_ZTVAULT".toByteArray()
        val result = EncryptedZtVaultMigrationEngine.importVaultBundle(
            vaultBytes = corruptedBytes,
            pan = "ABCDE1234F",
            dob = "15/08/1990",
            userPasscode = "AnyPasscode"
        )
        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("Invalid vault") == true)
    }

    @Test
    fun importVault_truncatedBytes_failsValidation() {
        val tinyBytes = byteArrayOf(0x01, 0x02, 0x03)
        val result = EncryptedZtVaultMigrationEngine.importVaultBundle(
            vaultBytes = tinyBytes,
            pan = "ABCDE1234F",
            dob = "15/08/1990",
            userPasscode = "AnyPasscode"
        )
        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("Size too small") == true)
    }
}
