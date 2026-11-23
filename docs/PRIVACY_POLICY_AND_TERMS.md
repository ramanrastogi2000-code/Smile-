# SMILE — Privacy Policy & Terms of Service

**Effective Date:** 2026  
**Application:** SMILE (बात करें, मुस्कुराएं!)

## Privacy Policy Summary
1. **Authentication & Identity:** SMILE uses Google Sign-In via Android Credential Manager. Each user selects a display name, unique Smile ID (`@handle`), optional `+91` mobile number, and About status.
2. **End-to-End Encryption (E2EE):** Direct 1-on-1 messages use ECDH P-256 (`secp256r1`) key agreement paired with AES-256-GCM authenticated encryption. Private keys are wrapped locally via Android Keystore (`AES/GCM/NoPadding`) and never transmitted to servers.
3. **Private Phone Discovery & Contact Sync:** Mobile numbers are normalized to E.164 (`+91` default) and hashed locally with SHA-256 (`ph_...`) before directory lookup. Device contacts are never read unless you explicitly grant consent and the `READ_CONTACTS` permission.
4. **No Plaintext Secrets:** App Lock PINs and Two-Step Verification PINs are stored exclusively as salted SHA-256 digests (`saltHex:sha256`).
5. **Permissions:**
   - `CAMERA` & `RECORD_AUDIO`: Used only during voice/video calls or voice note recording.
   - `READ_CONTACTS`: Used only on explicit contact-sync consent.
   - `ACCESS_COARSE_LOCATION`: Used only when you choose to share your location in a chat.
   - `POST_NOTIFICATIONS`: Optional message/call alerts on Android 13+.
6. **Data Deletion:** You may export your chats or permanently delete your profile, Smile ID reservation, and Firebase account at any time from **Settings -> Delete Account**.

## Terms of Service Summary
- Users must comply with applicable laws and refrain from harassment, spam, or abusive content.
- Users may block or report abusive accounts or groups directly inside Chat Settings; reports are reviewed by authorized administrators via the private moderation console.
- Service announcements and moderation audit logs are maintained to keep the SMILE community safe.
