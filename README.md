# BeFake

A standalone Android camera app that captures simultaneous front and back camera photos and merges them into a single composite image with a customizable watermark — inspired by the BeReal camera feature.

English | [日本語](README_ja.md)

## What It Does

When you press the capture button, both the front and rear cameras take a photo at the same time. The resulting image combines both: by default, the front camera photo fills the frame as the main image, and the back camera photo appears as a smaller rounded overlay in the top-left corner, similar to BeReal's signature dual-camera layout. After capture, you can swap which camera is the main image and which is the inset before saving.

## How It Stands Out from BeReal

BeFake strips away everything BeReal isn't — no social network, no notifications, no trending times, no friends feed. It is purely the camera feature, rebuilt from scratch as a standalone app with full control over the output:

- **Customizable watermark text** — set any text string to appear at the bottom of the composite image (default: "BeFake.")
- **Watermark color** — specify any color by name (e.g. `black`, `white`, `#FF5733`)
- **Watermark opacity** — adjust alpha from 0 to 100
- **Watermark font size** — set the text size to your liking
- **Border color & opacity** — customize the border around the back-camera inset photo
- **Capture quality mode** — choose between maximizing image quality or minimizing capture latency
- **Reverse layout** — swap which camera is the main image and which is the inset after capture
- **Toggle watermark on/off** — show or hide the watermark on the preview before saving

## Tech Stack

- **Language:** Kotlin
- **Minimum Android version:** 11 (API 30)
- **Target Android version:** 13 (API 33)
- **Compile SDK:** 34
- **Camera:** [CameraX](https://developer.android.com/training/camerax) with `ConcurrentCamera` for simultaneous dual-lens capture
- **Image merging:** Android `Canvas` and `Bitmap` API for compositing and watermark rendering
- **Persistence:** Android `SharedPreferences` for settings storage
- **EXIF handling:** `ExifInterface` for correct image rotation
- **Build system:** Gradle (Kotlin DSL)

## Building

Prerequisites: [Android Studio](https://developer.android.com/studio) with Android SDK 34 installed.

1. Open the project in Android Studio (`File > Open` and select this directory).
2. Sync Gradle files when prompted.
3. Connect an Android device via USB with **Developer Options** and **USB Debugging** enabled.
4. Click the run button (▶) in Android Studio to build and deploy.

No Play Store release exists — installation is via ADB/deployment from source only.

## Project Structure

```
app/src/main/java/com/georgeneokq/befake/
  MainActivity.kt          — Dual camera preview, capture logic, tap-to-focus, draggable inset
  CapturePreviewActivity.kt — Composite image rendering, watermark drawing, save/reverse controls
  SettingsActivity.kt      — Watermark and border customization UI
  Globals.kt               — App-wide constants
  util/Util.kt             — Vibration, rounded-canvas bitmap utilities, settings reset
  components/FlashOverlay.kt — White flash animation on capture
```

## License

Private / personal project. Not distributed publicly.
