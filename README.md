# Edit Master — Android starter project

**Tagline:** Create More. Edit Better. All in One Place.

This is a native Android Kotlin + Jetpack Compose starter for the Edit Master app concept. It is deliberately transparent about scope: it is not yet a complete production video editor.

## Implemented in this starter
- Native Android app structure and premium-style home dashboard.
- Bottom navigation for Home, Discover, Create, Projects and Profile.
- Local image selection through Android's system picker.
- Working photo preview with brightness and saturation controls.
- Save adjusted photo as a new JPEG under `Pictures/EditMaster` using MediaStore.
- Local video selection and playback preview.
- Basic empty/error handling for image import and photo save.
- Starter UI cards for Today's Highlights and creative tool categories.

## Not implemented yet
- Video trimming/splitting/timeline/export, transitions, audio mixing and project serialization.
- Crop/rotate/text/stickers/layers for photos; undo/redo and full project persistence.
- Actual online trending data, templates, user accounts, cloud sync or community publishing.
- AI features (these require a chosen local model or secure backend/API and credentials).
- Ads, subscriptions, analytics, account deletion and Google Play production compliance.

Cards for future-stage tools are clearly identified in the UI and do not pretend to perform those operations.

## Build
1. Install Android Studio with Android SDK Platform 35 and JDK 17.
2. Open this folder in Android Studio and allow Gradle sync.
3. Run the `app` configuration on an Android device or emulator (Android 8.0/API 26 or later).
4. To build a debug APK, run `./gradlew assembleDebug` after generating a Gradle wrapper in Android Studio, or use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

The current execution environment did not have Gradle or the Android SDK installed, so an APK could not be built or tested here. Treat this source as an initial implementation stage, not a verified release build.

## Recommended next engineering stage
Implement the video pipeline using AndroidX Media3 Transformer after checking its current API and license; then add Room-based project metadata, robust project asset handling, crop/rotate and text overlays, instrumentation tests, and a real backend before adding online/AI functionality.
