# MediCare Development Guide

## 1. Overview

MediCare is an Android app that combines Firebase Authentication with a hosted REST API.

Core goals:
- Authenticate users (email/password, Google, phone verification)
- Ensure every authenticated user has a synced profile in backend
- Allow users to submit daily health data
- Provide quick emergency call actions

## 2. Architecture

High-level layers:
- UI Layer: Activities under `ui/`
- Data Layer:
  - `Api.kt`: HTTP requests to backend
  - `SessionManager.kt`: SharedPreferences session cache
- Auth Layer: Firebase Auth + Google Sign-In + Phone OTP linking

Navigation path:
1. `SplashActivity` (session bootstrap)
2. `LoginRegisterActivity` (auth entry)
3. `PhoneVerificationActivity` (if phone required)
4. `DashboardActivity`
5. Feature screens (`AddHealthDataActivity`, `EmergencyActivity`)

## 3. Backend API Routes Used by App

Base URL:
- `https://medi-care-roan.vercel.app`

### 3.1 GET `/api/users`

Purpose:
- Lookup existing user profile.

Query params used:
- `uid` (optional)
- `email` (optional)
- `phone` (optional)

Where used:
- `SplashActivity`, `LoginRegisterActivity`, `DashboardActivity`

App behavior:
- If profile exists and phone is available, session is completed and user is routed to dashboard.
- If phone is missing, user is routed to phone verification.

Expected response handling in app:
- Handles shapes with either top-level user fields or `data` object.
- Handles `exists: false` and HTTP 404 as "profile not found".

### 3.2 POST `/api/users`

Purpose:
- Create or update user profile after successful Firebase auth.

Request body fields sent:
- `uid`
- `name`
- `email`
- `phone`
- `provider` (`google`, `phone`, or `password`)
- `photo`
- `createdAt`

Where used:
- `LoginRegisterActivity` and `PhoneVerificationActivity`

App behavior:
- On success: saves local session and enters dashboard.
- On failure:
  - In some flows app may still proceed to dashboard (graceful fallback) if local data is sufficient.

### 3.3 POST `/api/health/add`

Purpose:
- Save health metrics entered by user.

Request body fields sent:
- `userId` (current user email)
- `date` (`yyyy-MM-dd`)
- `weight` (Int)
- `bloodPressure` (Int, default 0 if blank)
- `sugar` (Int)
- `heartRate` (Int, default 0 if blank)

Where used:
- `AddHealthDataActivity`

App behavior:
- On success: toast + close screen.
- On failure: show error toast.

## 4. Build and Run

## Android Studio

1. Open project.
2. Add `app/google-services.json` (Firebase config).
3. Sync Gradle.
4. Run `app` on emulator/device.

## CLI

Linux/macOS:
```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :app:testDebugUnitTest
```

Windows PowerShell:
```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:testDebugUnitTest
```

## 5. Firebase Setup Notes

Required:
- Add Android app in Firebase with package `com.example.medicare`
- Enable sign-in methods:
  - Email/Password
  - Google
  - Phone
- Add SHA-1 and SHA-256 fingerprints
- Download and place `google-services.json` in `app/`

Common issue:
- Google sign-in code `10` indicates project/signing mismatch (SHA/package/client config mismatch).

## 6. How to Work on This Project

Suggested development workflow:
1. Create a feature branch.
2. Implement UI/data changes in small commits.
3. Test auth flows:
   - Email login/register
   - Google login
   - Phone verification route
4. Test backend flows:
   - `/api/users` fetch and upsert
   - `/api/health/add`
5. Run unit tests and build debug APK.
6. Open PR with screenshots and testing notes.

When editing auth flow:
- Keep `SessionManager` and backend profile sync logic aligned.
- Preserve phone verification gate before dashboard access.

When editing API calls:
- Reuse `Api.http` (OkHttp client) for consistent timeouts.
- Keep UI thread updates inside callbacks (as current code does via handler or `runOnUiThread`).

## 7. Git Ignore Policy (Important)

The project ignores:
- Local IDE and Gradle caches (`.idea/`, `.gradle/`, `.android/`, `.gradle-user/`)
- Build outputs (`**/build/`, APK/AAB artifacts)
- Signing files (`*.jks`, `*.keystore`, `key.properties`)
- Firebase local config (`app/google-services.json`)
- Local env/temp/log files

This helps prevent accidental pushes of machine-specific or sensitive files to GitHub.
