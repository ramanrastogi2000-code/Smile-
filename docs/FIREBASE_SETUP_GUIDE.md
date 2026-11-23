# SMILE — Firebase Setup & Cloud Infrastructure Guide

**Firebase Project ID:** `gen-lang-client-0029743851`  
**Firestore Database ID:** `ai-studio-android-smile-33715eb6-7eef-4799-8f6b-21ffedb9e9f0`

## 1. What Is Already Provisioned Live in This Project
- **Google Sign-In Authentication (`CredentialManager` + `GetSignInWithGoogleOption`):** Active with `app/google-services.json` and SHA-1 (`37:AE:EE:92:F7:A7:F9:6C:0B:07:64:6C:0E:FB:80:AE:D4:61:20:FC`).
- **Cloud Firestore Real-Time Database:** Configured with zero-trust rules (`/firestore.rules`) and schema (`/firebase-blueprint.json`) powering 1-on-1 & group chats, E2EE key distribution (`userProfiles.publicKey`), `+91` SHA-256 phone directory (`phoneLookup`), 24-hour status updates (`statuses`), call signaling & history (`calls`), communities (`communities`), abuse reports (`reports`), and system announcements (`announcements`).

## 2. Enabling Optional Firebase Console Services for Production
1. **Firebase Phone Auth (SMS OTP):**
   - Open Firebase Console -> **Authentication -> Sign-in method -> Phone** and enable Phone Sign-In.
   - Add SHA-1 and SHA-256 fingerprints for your **Play App Signing** / upload keystore under **Project Settings -> Your Apps -> Android**.
   - Note: AI Studio's managed runtime authenticates via Google Sign-In (`CredentialManager`) and stores `+91` phone lookup digests privately under `/phoneLookup`. Enable Phone Auth when publishing standalone OTP flows outside AI Studio.
2. **Firebase Cloud Storage (Large Video/Document Binaries):**
   - Compact photos, profile avatars, and short voice notes currently sync directly inside Firestore documents within document size limits.
   - For high-resolution video files and large PDFs, open **Firebase Console -> Storage**, create a regional bucket (e.g., `asia-south1` Mumbai), and add `firebase-storage` to `app/build.gradle.kts`.
3. **Firebase Cloud Functions & FCM Push Notifications (`/backend/functions/`):**
   - Upgrade the Firebase project to the **Blaze** plan.
   - Install dependencies and deploy:
     ```bash
     cd backend/functions
     npm install
     npm run build
     firebase deploy --only functions
     ```
   - Register the Android app for FCM and save each device's `FirebaseMessaging.getInstance().token` to `users/{uid}.fcmToken`.
4. **Private Web Owner/Admin Dashboard (`/admin-dashboard/`):**
   - Register a **Web App** in Firebase Console -> **Project Settings -> Your Apps**.
   - Populate `FIREBASE_WEB_CONFIG` in `admin-dashboard/app.js`.
   - Deploy privately via **Firebase Hosting** (`firebase deploy --only hosting`) or an internal VPC/Cloudflare Access tunnel.
   - Authorized Owner email enforced by Firestore rules: `ramanrastogi2000@gmail.com`.
