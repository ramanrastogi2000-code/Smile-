package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.SmileCryptoManager
import com.example.ui.i18n.stringsForLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `app name matches SMILE brand`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SMILE", appName)
    }

    @Test
    fun `ecdh key exchange and aes gcm encryption round trip`() {
        val alice = SmileCryptoManager.generateEcKeyPair()
        val bob = SmileCryptoManager.generateEcKeyPair()
        val eve = SmileCryptoManager.generateEcKeyPair()

        val alicePubB64 = Base64.encodeToString(alice.public.encoded, Base64.NO_WRAP)
        val bobPubB64 = Base64.encodeToString(bob.public.encoded, Base64.NO_WRAP)

        val aliceShared = SmileCryptoManager.deriveSharedAesKey(alice.private.encoded, bobPubB64)
        val bobShared = SmileCryptoManager.deriveSharedAesKey(bob.private.encoded, alicePubB64)
        val eveShared = SmileCryptoManager.deriveSharedAesKey(eve.private.encoded, alicePubB64)

        assertNotNull(aliceShared)
        assertNotNull(bobShared)
        assertNotNull(eveShared)

        val plaintext = "Namaste from SMILE! बात करें, मुस्कुराएं!"
        val encrypted = SmileCryptoManager.encryptWithSharedKey(plaintext, aliceShared!!)
        val decryptedByBob = SmileCryptoManager.decryptWithSharedKey(
            encrypted.ciphertextBase64,
            encrypted.ivBase64,
            bobShared!!
        )
        val decryptedByEve = SmileCryptoManager.decryptWithSharedKey(
            encrypted.ciphertextBase64,
            encrypted.ivBase64,
            eveShared!!
        )

        assertEquals(plaintext, decryptedByBob)
        assertNull(decryptedByEve)

        assertEquals(
            SmileCryptoManager.computeSafetyNumber(alicePubB64, bobPubB64),
            SmileCryptoManager.computeSafetyNumber(bobPubB64, alicePubB64)
        )
    }

    @Test
    fun `private key wrapping and unwrapping works cleanly`() {
        val kp = SmileCryptoManager.generateEcKeyPair()
        val wrapped = SmileCryptoManager.wrapPrivateKeyForStorage(kp.private.encoded)
        val unwrapped = SmileCryptoManager.unwrapPrivateKeyFromStorage(wrapped)
        assertNotNull(unwrapped)
        assertTrue(kp.private.encoded.contentEquals(unwrapped))
    }

    @Test
    fun `phone normalization defaults to India +91 and hashes deterministically`() {
        assertEquals("+919876543210", SmileCryptoManager.normalizePhoneE164("98765 43210"))
        assertEquals("+919876543210", SmileCryptoManager.normalizePhoneE164("09876543210"))
        assertEquals("+919876543210", SmileCryptoManager.normalizePhoneE164("+91-98765-43210"))

        val h1 = SmileCryptoManager.hashPhoneForLookup("9876543210")
        val h2 = SmileCryptoManager.hashPhoneForLookup("+91 98765-43210")
        assertTrue(h1.startsWith("ph_"))
        assertEquals(h1, h2)
    }

    @Test
    fun `salted pin hashing verifies valid pin and rejects wrong pin`() {
        val hash = SmileCryptoManager.hashPinWithSalt("2580")
        assertTrue(hash.contains(":"))
        assertTrue(SmileCryptoManager.verifyPin("2580", hash))
        assertFalse(SmileCryptoManager.verifyPin("0000", hash))
    }

    @Test
    fun `hindi and english localizations return expected strings`() {
        val en = stringsForLanguage("EN")
        val hi = stringsForLanguage("HI")
        assertEquals("Chats", en.chats)
        assertEquals("चैट्स", hi.chats)
        assertEquals("बात करें, मुस्कुराएं!", hi.tagline)
    }
}
