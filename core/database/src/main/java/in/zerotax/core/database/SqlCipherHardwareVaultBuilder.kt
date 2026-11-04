package `in`.zerotax.core.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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

object SqlCipherHardwareVaultBuilder {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "zero_tax_sqlcipher_master_key_v1"
    private const val PREFS_FILE = "zero_tax_wrapped_passphrase_prefs"
    private const val KEY_CIPHERTEXT = "ct"
    private const val KEY_IV = "iv"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PASSPHRASE_BYTE_COUNT = 32

    fun build(context: Context): ZeroTaxVaultDatabase {
        val passphrase = getOrCreateWrappedPassphrase(context)
        val factory = SupportFactory(passphrase.copyOf(), null, true)
        // Memory hardening: Immediately wipe local passphrase byte array
        passphrase.fill(0)

        return Room.databaseBuilder(
            context.applicationContext,
            ZeroTaxVaultDatabase::class.java,
            "zero_tax_vault.db"
        )
            .openHelperFactory(factory)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Zero out cleared pages in system memory immediately
                    db.execSQL("PRAGMA cipher_memory_security = ON;")
                    // Overwrite deleted data with zeros to prevent disk recovery
                    db.execSQL("PRAGMA secure_delete = ON;")
                    // Enforce referential integrity
                    db.execSQL("PRAGMA foreign_keys = ON;")
                }
            })
            .build()
    }

    fun createCipherForBiometricAuth(): Cipher {
        val hardwareKey = getOrCreateHardwareKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, hardwareKey)
        return cipher
    }

    @Synchronized
    private fun getOrCreateWrappedPassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val hardwareKey = getOrCreateHardwareKey()
        
        val hexCipher = prefs.getString(KEY_CIPHERTEXT, null)
        val hexIv = prefs.getString(KEY_IV, null)

        if (hexCipher != null && hexIv != null) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, hexToBytes(hexIv))
            cipher.init(Cipher.DECRYPT_MODE, hardwareKey, spec)
            return cipher.doFinal(hexToBytes(hexCipher))
        }

        val rawPassphrase = ByteArray(PASSPHRASE_BYTE_COUNT).also {
            SecureRandom().nextBytes(it)
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, hardwareKey)
        val encryptedPassphrase = cipher.doFinal(rawPassphrase)
        val iv = cipher.iv

        prefs.edit()
            .putString(KEY_CIPHERTEXT, bytesToHex(encryptedPassphrase))
            .putString(KEY_IV, bytesToHex(iv))
            .apply()

        return rawPassphrase
    }

    private fun getOrCreateHardwareKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        
        keyStore.getKey(MASTER_KEY_ALIAS, null)?.let {
            return it as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val specBuilder = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        // Prefer StrongBox Keymaster; fallback gracefully to standard hardware TEE
        return runCatching {
            keyGenerator.init(specBuilder.setIsStrongBoxBacked(true).build())
            keyGenerator.generateKey()
        }.getOrElse {
            keyGenerator.init(specBuilder.setIsStrongBoxBacked(false).build())
            keyGenerator.generateKey()
        }
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray =
        hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    // TODO(Phase-2): Add BiometricPrompt CryptoObject binding for optional user-authenticated vault unlock.
    // TODO(Phase-2): Add memory zeroing/scrubbing hook for rawPassphrase byte arrays immediately following DB open.
}
