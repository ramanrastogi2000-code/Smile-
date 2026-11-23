# SMILE — Verification & Testing Report

## 1. Android Compilation & Build
- **Command:** `gradle :app:compileDebugKotlin` & `compile_applet`
- **Status:** **PASSED** (`BUILD SUCCESSFUL`, 0 compilation errors)
- **Notes:**
  - Verified `applicationId = "com.aistudio.smile.kpmzqx"` while keeping `namespace = "com.example"`.
  - Verified Firestore database ID binding via `@string/firestore_database_id` (`ai-studio-android-smile-33715eb6-7eef-4799-8f6b-21ffedb9e9f0`) and Credential Manager Google Sign-In (`GetSignInWithGoogleOption`).

## 2. Unit & Robolectric JVM Test Suite
- **Command:** `gradle :app:testDebugUnitTest`
- **Total:** **9 tests executed, 9 passed, 0 failed, 0 skipped**

| Suite | Tests | Passed | Coverage |
| --- | --- | --- | --- |
| `ExampleRobolectricTest` | 6 | 6 | `R.string.app_name == "SMILE"`, ECDH P-256 + AES-256-GCM encryption/decryption & unauthorized key rejection, symmetric safety fingerprint, private key wrapping/unwrapping, India `+91` phone normalization & SHA-256 `ph_` lookup digest, salted PIN hashing/verification, English & Hindi (`HI`) localization |
| `SmileCryptoAndModelTest` | 2 | 2 | Enum fallback safety (`VisibilityOption`, `ChatType`, `MessageType`, `DeliveryStatus`, `CallType`, `CallStatus`), `ChatRoom.peerUid` |
| `ExampleUnitTest` | 1 | 1 | Baseline JVM runner verification |

## 3. Firestore Security Rules Emulator Test Suite
- **Command:** `FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js`
- **Total:** **5 test suites executed, 5 passed, 0 failed**
- **Verified Invariants:**
  1. Unauthenticated requests rejected across all collections.
  2. `/users/{userId}` strictly owner-isolated; rejects ghost/unlisted fields.
  3. `/userProfiles`, `/smileIds` uniqueness & ownership, and `/phoneLookup` hash lookups enforced.
  4. `/chats` and `/chats/{chatId}/messages` enforce `memberUids` membership, sender integrity, and reaction/delete-for-everyone rules.
  5. `/statuses`, `/calls`, `/communities`, `/reports`, and `/adminUsers` enforce strict RBAC & isolation.
- **Deployment:** Rules deployed to live Firestore database via `DeployRules`.

## 4. Known Runtime Scope Notes
- **Emulator / Physical Device Verification:** Live Google Sign-In UI and physical CameraX/AudioManager hardware tests require running the built APK on a real Android device or interactive streaming emulator.
- **External Cloud Services Templated in `/backend/` & `/admin-dashboard/`:** Firebase Cloud Messaging push triggers, scheduled 24h status cleanup, and `coturn` TURN media relay require Blaze-plan Cloud Functions / TURN VM deployment as detailed in `/docs/FIREBASE_SETUP_GUIDE.md` and `/docs/WEBRTC_TURN_SETUP.md`.
