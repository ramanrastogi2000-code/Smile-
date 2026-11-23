# SMILE — Build, Signing & Google Play Store Deployment Guide

## 1. App Identity
- **App Name:** `SMILE`
- **Tagline:** बात करें, मुस्कुराएं!
- **Application ID:** `com.aistudio.smile.kpmzqx`
- **Min SDK / Target SDK:** `24` / `36`

## 2. Generating a Release Keystore (Local / CI Only)
Never commit production `.jks` keystores or passwords to version control.
```bash
keytool -genkey -v -keystore smile-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```
Pass signing credentials via environment variables (used by `app/build.gradle.kts`):
```bash
export KEYSTORE_PATH=/secure/path/smile-upload-key.jks
export STORE_PASSWORD="<your-store-password>"
export KEY_PASSWORD="<your-key-password>"
```

## 3. Building Signed Release APK & Play Store AAB
```bash
# Release App Bundle (required for Google Play Store)
gradle :app:bundleRelease

# Release APK (for direct testing / distribution)
gradle :app:assembleRelease
```
Outputs are generated under `app/build/outputs/bundle/release/` and `app/build/outputs/apk/release/`.

## 4. Google Play Policy & Data Safety Checklist
SMILE is designed for Google Play policy compliance:
- **Photo Picker (`PickVisualMedia`):** Zero broad storage permissions (`READ_EXTERNAL_STORAGE` / `READ_MEDIA_*` are omitted).
- **Least-Privilege Runtime Permissions:** `CAMERA`, `RECORD_AUDIO`, `READ_CONTACTS`, `ACCESS_COARSE_LOCATION`, and `POST_NOTIFICATIONS` are requested only when the user invokes the corresponding action, with clear consent dialogs for Contacts & Location.
- **Data Safety Declaration:** Declare Encrypted in Transit (TLS + ECDH/AES-GCM), Account Deletion available inside **Settings -> Delete Account**, and optional contacts/location sharing initiated solely by user consent.
- **Owner/Admin Console Separation:** The Web Admin Dashboard resides exclusively in `/admin-dashboard/` and is never bundled inside the Android APK.
