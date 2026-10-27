# Orbit: remaining setup and verification

This repository is an Android-only prototype. The code cannot be fully validated until it is built with Android SDK dependencies and connected to a Firebase project. Do not present it as fully tested until the checklist below is completed.

## 1. Open and build on Windows

1. Extract this repository to a short path such as `D:\Projects\Orbit`.
2. Install the latest stable Android Studio and its Android SDK Platform 35 / Build Tools. Use JDK 17 for the Gradle build (Android Studio's bundled JBR is usually suitable; set Gradle JDK to 17 in Settings if needed).
3. Open the `geofence-groups` directory in Android Studio. It is the folder containing `settings.gradle.kts`.
4. Wait for Gradle sync. The included `gradlew.bat` bootstraps Gradle 8.9, so first run requires internet access.
5. Before attempting an APK build, create `local.properties` in the repository root and set a Maps key (or leave it blank for non-map flows):

   ```properties
   MAPS_API_KEY=your_restricted_maps_sdk_key
   ```

6. For local Gradle configuration, add the actual `app/google-services.json` from your Firebase project (see below). Never commit the real file.
7. In the Android Studio terminal, run:

   ```powershell
   .\gradlew.bat :app:testDebugUnitTest
   .\gradlew.bat :app:lintDebug
   .\gradlew.bat :app:assembleDebug
   ```

8. Save the first full error from Build Output if any command fails; fix the earliest root cause before trying subsequent tasks.

## 2. Configure Firebase (requires your account)

1. Create a Firebase project.
2. Register an Android app with package `com.example.geofencegroups`.
3. Enable Email/Password in Authentication.
4. Create a Cloud Firestore database.
5. Download the real `google-services.json` into `app/`.
6. Install Node.js 20 and Firebase CLI (`npm install -g firebase-tools`).
7. From the repository root, run `firebase login`, then `firebase use --add` and select the project.
8. Review billing before enabling Cloud Functions. Firebase may require the Blaze plan for deployment; set a budget alert first.
9. Run backend tests and deploy:

   ```powershell
   cd functions
   npm ci
   npm test
   cd ..
   firebase deploy --only firestore:rules,firestore:indexes,functions
   ```

Do not deploy until the Firebase project selected by `firebase use` is the intended one. Never add service-account keys or real API credentials to Git.

## 3. Two-account device test

Use two Android devices, or two emulators with Google Play services and distinct test accounts.

- [ ] Account A can sign up, sign in, and create a group.
- [ ] Account A can retrieve and share the invite code.
- [ ] Account B can join using that code.
- [ ] A group admin/owner can set the boundary; an ordinary member cannot.
- [ ] Both devices receive the boundary sync and show an honest monitoring status.
- [ ] After allowing background location and notifications, leaving the boundary creates one timeline event and notifies B, not A.
- [ ] Re-entering creates one event and notifies B, not A.
- [ ] A non-member cannot read group, member, event, or invite-code data.
- [ ] Permission revocation, loss of connectivity, app restart, and reboot produce understandable status/recovery behaviour.

Android geofences are not instant; test with a sufficiently large radius and allow several minutes for Play Services to report a crossing. Real-device verification is mandatory before claiming background behavior works.

## 4. Assignment scope warning

The original brief requested Kotlin Multiplatform plus a Swift/iOS app. This repository currently contains Android only. Confirm that the reviewer has approved Android-only scope or implement the missing iOS/KMP deliverable before final submission.
