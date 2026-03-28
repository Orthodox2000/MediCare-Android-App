# MediCare Android App

MediCare is a Kotlin Android app with Firebase Authentication (email/password + Google + phone verification) and a hosted backend API for user profiles and health data.

## Tech Stack

- Android (Views/XML), Kotlin, Gradle (KTS)
- Firebase Auth (`email/password`, `google`, `phone`)
- Google Sign-In
- OkHttp for API calls
- Backend base URL: `https://medi-care-roan.vercel.app`

## Project Structure

- `app/src/main/java/com/example/medicare/ui` - Screens and app flow
- `app/src/main/java/com/example/medicare/data/Api.kt` - API client and profile sync
- `app/src/main/java/com/example/medicare/data/SessionManager.kt` - Local session cache
- `app/src/main/res/layout` - XML layouts
- `app/src/main/AndroidManifest.xml` - App manifest and activity registration
- `docs/DEVELOPMENT.md` - Detailed docs (architecture, API routes, workflow)

## Prerequisites

- Android Studio (latest stable recommended)
- JDK 11 (project is configured with Java 11 target)
- Android SDK with:
  - `compileSdk = 36`
  - `minSdk = 24`
- Firebase project configured for Android package:
  - `com.example.medicare`
- Internet connection (app uses remote API + Firebase)

## Setup

1. Clone the repository.
2. Open the project in Android Studio.
3. Add your Firebase config file to:
   - `app/google-services.json`
4. In Firebase Console, enable:
   - Email/Password sign-in
   - Google sign-in
   - Phone sign-in
5. For Google Sign-In, add SHA-1 and SHA-256 fingerprints for your debug/release keys in Firebase, then re-download `google-services.json` if needed.
6. Sync Gradle.

## Run (Android Studio)

1. Select `app` configuration.
2. Choose emulator or physical device.
3. Click Run.

## Build Commands (CLI)

From repository root:

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK (unsigned unless signing configured)
./gradlew :app:assembleRelease

# Unit tests
./gradlew :app:testDebugUnitTest
```

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:testDebugUnitTest
```

## App Flow

1. `SplashActivity` checks existing Firebase session.
2. If needed, tries Google silent sign-in.
3. `LoginRegisterActivity` handles login/register and Google sign-in.
4. If phone is missing, user is sent to `PhoneVerificationActivity`.
5. After profile sync, user reaches `DashboardActivity`.
6. From dashboard:
   - `AddHealthDataActivity` sends health metrics to backend.
   - `EmergencyActivity` shows quick emergency dial actions.

## API Overview

Implemented client routes:

- `GET /api/users`
  - Query params supported by app: `uid`, `email`, `phone`
  - Used to fetch existing profile and check phone availability.
- `POST /api/users`
  - Upserts user profile from Firebase-authenticated app session.
- `POST /api/health/add`
  - Stores health metrics from Add Health Data screen.

See full request/response documentation in:
- `docs/DEVELOPMENT.md`

## Security and Git Hygiene

- `.gitignore` is configured to avoid committing build outputs, local IDE state, and sensitive files.
- `app/google-services.json` is intentionally ignored and should be provided locally.
- Do not commit signing keys (`.jks`, `.keystore`) or local secrets files.

## Troubleshooting

- Google sign-in error code `10`:
  - Usually SHA fingerprint mismatch in Firebase project.
  - Re-check package name, SHA keys, and download updated `google-services.json`.
- Phone OTP not received:
  - Verify phone auth is enabled in Firebase and number format is valid.
- Build sync errors:
  - Confirm Android Studio SDK/API versions and Gradle sync completion.
