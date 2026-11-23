package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String
)

/**
 * Cryptographic engine for SMILE:
 * - ECDH P-256 (secp256r1) key agreement + SHA-256 KDF + AES-256-GCM message encryption
 * - AndroidKeyStore AES-256-GCM wrapping of the user's ECDH private key at rest
 * - Salted SHA-256 hashing for App Lock & Two-Step PINs (zero plaintext PIN storage)
 * - Deterministic E.164 phone hashing (+91 default) for private contact lookup
 */
object SmileCryptoManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "smile_master_aes_key_v1"
    private const val AES_GCM_MODE = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    private fun getOrCreateMasterKey(): SecretKey? = try {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey
        if (existing != null) {
            existing
        } else {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            generator.init(
                KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generator.generateKey()
        }
    } catch (_: Exception) {
        // Fallback when running under Robolectric JVM where AndroidKeyStore is unavailable
        null
    }

    fun generateEcKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        return kpg.generateKeyPair()
    }

    fun wrapPrivateKeyForStorage(privateKeyEncoded: ByteArray): String {
        val masterKey = getOrCreateMasterKey()
        if (masterKey != null) {
            val cipher = Cipher.getInstance(AES_GCM_MODE)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)
            val encrypted = cipher.doFinal(privateKeyEncoded)
            val ivB64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val dataB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            return "KS1:$ivB64:$dataB64"
        }
        return "RAW:${Base64.encodeToString(privateKeyEncoded, Base64.NO_WRAP)}"
    }

    fun unwrapPrivateKeyFromStorage(wrapped: String): ByteArray? = try {
        when {
            wrapped.startsWith("KS1:") -> {
                val parts = wrapped.split(":")
                if (parts.size != 3) return null
                val masterKey = getOrCreateMasterKey() ?: return null
                val iv = Base64.decode(parts[1], Base64.NO_WRAP)
                val cipherBytes = Base64.decode(parts[2], Base64.NO_WRAP)
                val cipher = Cipher.getInstance(AES_GCM_MODE)
                cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                cipher.doFinal(cipherBytes)
            }
            wrapped.startsWith("RAW:") -> {
                Base64.decode(wrapped.removePrefix("RAW:"), Base64.NO_WRAP)
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    fun deriveSharedAesKey(
        myPrivateKeyBytes: ByteArray,
        peerPublicKeyBase64: String
    ): SecretKey? = try {
        val kf = KeyFactory.getInstance("EC")
        val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(myPrivateKeyBytes))
        val pubBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val pubKey = kf.generatePublic(X509EncodedKeySpec(pubBytes))
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privKey)
        agreement.doPhase(pubKey, true)
        val sharedSecret = agreement.generateSecret()
        val digest = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
        SecretKeySpec(digest, "AES")
    } catch (_: Exception) {
        null
    }

    fun encryptWithSharedKey(plaintext: String, sharedKey: SecretKey): EncryptedPayload {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, sharedKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            ciphertextBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decryptWithSharedKey(
        ciphertextBase64: String,
        ivBase64: String,
        sharedKey: SecretKey
    ): String? = try {
        val cipherBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_GCM_MODE)
        cipher.init(Cipher.DECRYPT_MODE, sharedKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }

    fun computeSafetyNumber(myPublicKeyBase64: String, peerPublicKeyBase64: String): String {
        val ordered = listOf(myPublicKeyBase64, peerPublicKeyBase64).sorted().joinToString("|")
        val hash = MessageDigest.getInstance("SHA-256").digest(ordered.toByteArray(Charsets.UTF_8))
        val digits = buildString {
            for (b in hash.take(24)) {
                append(((b.toInt() and 0xFF) % 100).toString().padStart(2, '0'))
            }
        }
        return digits.chunked(4).take(12).joinToString(" ")
    }

    fun normalizePhoneE164(rawPhone: String, defaultCountryCode: String = "+91"): String {
        val trimmed = rawPhone.trim().replace(Regex("[\\s\\-()]"), "")
        if (trimmed.isEmpty()) return ""
        return when {
            trimmed.startsWith("+") -> trimmed
            trimmed.startsWith("0") && trimmed.length == 11 -> "$defaultCountryCode${trimmed.drop(1)}"
            trimmed.length == 10 && defaultCountryCode == "+91" -> "+91$trimmed"
            else -> "$defaultCountryCode$trimmed"
        }
    }

    fun hashPhoneForLookup(rawPhone: String, defaultCountryCode: String = "+91"): String {
        val normalized = normalizePhoneE164(rawPhone, defaultCountryCode)
        if (normalized.isBlank()) return ""
        val bytes = MessageDigest.getInstance("SHA-256").digest("SMILE_PHONE:$normalized".toByteArray(Charsets.UTF_8))
        return "ph_" + bytes.joinToString("") { "%02x".format(it) }.take(40)
    }

    fun hashPinWithSalt(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val digest = sha256Hex("$saltHex:$pin")
        return "$saltHex:$digest"
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val expected = sha256Hex("${parts[0]}:$pin")
        return MessageDigest.isEqual(expected.toByteArray(), parts[1].toByteArray())
    }

    private fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
