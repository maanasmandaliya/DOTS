# DOTS

A minimalist Android countdown wallpaper. Every dot is a unit of time: filled dots have passed, empty dots are still ahead, and today's dot is highlighted. The wallpaper updates itself every day.

## Build

Requires the Android SDK (platform 37) and JDK 17+ (Android Studio's bundled JBR works).

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests: countdown math, colours, grid layout
```

Open the folder in Android Studio to run it on a device or emulator.

## Architecture

A single `app` module with clean-architecture layers in separate packages. Dependencies point inward: `ui → data → domain`, and `render` depends only on `domain`.

| Layer | Package | Responsibility |
|---|---|---|
| Domain | `domain.model`, `domain.countdown`, `domain.color` | Pure Kotlin. Serializable config, countdown math, HEX/HSV/contrast math and palette suggestions. No Android types, so it is fully unit-tested. |
| Render | `render` | `WallpaperRenderer`, the one renderer used by both the preview and the generator. `DotGridLayout` (pure geometry) and `BoxBlur`. |
| Data | `data` | DataStore persistence, photo import, full-resolution generation, `WallpaperManager`, gallery/share export, and the `WallpaperUpdater` use case. |
| Work | `work` | WorkManager scheduling, the update worker, and the time/timezone/boot receiver. |
| UI | `ui.*` | Jetpack Compose and Material 3 screens, one `DotsViewModel`, and the design system in `ui.theme` and `ui.components`. |

Dependency injection is manual (`AppContainer`), since the object graph is small.

```
app/src/main/java/com/dots/app/
├── DotsApplication.kt, AppContainer.kt, MainActivity.kt (navigation)
├── domain/
│   ├── model/        WallpaperConfig (+ settings), Preset, HistoryEntry, BuiltInPresets
│   ├── countdown/    CountdownCalculator, CountdownProgress
│   └── color/        ColorMath (HEX parse/validate, HSV, WCAG contrast), PaletteSuggester
├── render/           WallpaperRenderer, DotGridLayout, BoxBlur
├── data/             SettingsRepository, ImageRepository, WallpaperGenerator,
│                     WallpaperExporter, WallpaperUpdater
├── work/             UpdateScheduler, WallpaperUpdateWorker, SystemEventReceiver
└── ui/
    ├── theme/        DotsTheme (dark by default), typography, shapes
    ├── components/   DotMatrixText, Segmented, LabeledSlider, ColorPickerSheet, …
    ├── preview/      WallpaperCanvas, PhoneFrame, home/lock screen overlays
    ├── dashboard/    DashboardScreen
    ├── editor/       EditorScreen + Countdown/Background/Dots/Layout/Presets sections
    ├── crop/         CropScreen (pinch-zoom / drag)
    ├── history/      HistoryScreen
    └── settings/     SettingsScreen
```

## Rendering approach

- **Resolution-independent config.** Every size in `WallpaperConfig` is in *units*, where 1 unit = 1/1000 of the wallpaper width. The image crop is stored as fit mode, zoom and a normalised pan (−1..1). A 300 px preview and a 1440 px wallpaper therefore come from the same numbers. A unit test (`layout is resolution independent`) checks this.
- **One renderer.** The Compose preview calls `WallpaperRenderer.render()` on `drawIntoCanvas { it.nativeCanvas }`. The generator calls the same function on a full-size `Bitmap`. Preview and output can't drift apart.
- **Real-time and cheap.** Brightness, contrast and saturation are a single `ColorMatrixColorFilter` applied while drawing, so they cost no extra pixel work. Opacity is the paint alpha, and overlays are one `drawColor` each. Blur is the only pixel operation. It runs on a layer downscaled so the effective radius is about 6 px, and that layer is cached by image transform plus blur. Changing colours or dots never re-blurs.
- **No distortion.** Images are only scaled uniformly (Fill, Fit, Fit width, Fit height, plus zoom). If the dot grid doesn't fit inside the padding, it shrinks uniformly.
- **Exact screen size.** The wallpaper is rendered at the physical portrait resolution (`WindowMetrics`), so there is no scaling at apply time.
- **Memory.** Imported photos are decoded once with `ImageDecoder` (EXIF rotation applied), capped at 4096 px on the long side and 8 MP, and copied into app storage. Photo-picker URIs expire, but the daily worker needs the image indefinitely. The preview loads a copy of about 1600 px. The generator loads the full copy and releases it right after rendering.

## Automatic updates

- **Midnight job.** A one-time job runs at 00:01 local time and re-schedules the next one, so the next dot fills right after the day changes.
- **Safety net.** A 6-hour periodic job covers Doze deferral or a device that was switched off.
- **On demand.** A refresh runs on app resume, timezone change, clock change, boot and app update (`SystemEventReceiver`; these broadcasts are exempt from Android's background-broadcast limits).
- **Cheap when idle.** The worker compares `CountdownProgress.renderKey` (mode, total and elapsed) with what was last applied and exits without rendering if nothing changed. No service runs in the background.
- **Design preserved.** Updates re-render the *applied* config. Edits that haven't been applied never reach the wallpaper behind the user's back.
- **Calendar-day maths.** All maths runs on `LocalDate` in the current zone, so it isn't affected by daylight-saving changes. A timezone change only changes which day is "today".

## Permissions

| Permission | Why |
|---|---|
| `SET_WALLPAPER` | `WallpaperManager.setBitmap` (normal permission, granted at install). |
| `RECEIVE_BOOT_COMPLETED` | Re-align the midnight update after a reboot. |

None are needed for photos or the gallery. The Android Photo Picker needs no storage permission, and saving uses `MediaStore` with `minSdk 29`.

## Dependencies

AGP 9.5 with built-in Kotlin 2.4, Compose BOM 2026.09 with Material 3, Navigation Compose, Lifecycle (ViewModel, `collectAsStateWithLifecycle`), DataStore Preferences, WorkManager, AndroidX Palette (dominant-colour extraction), kotlinx.serialization (persisting configs, presets and history as JSON), and JUnit 4. Versions are in `gradle/libs.versions.toml`.

## Features

- **Countdown modes.** Year (365/366), Month (28–31), Week (7, with a choice of first day), Life (weeks, from birth date and life expectancy) and Goal (custom start and end dates, counted in days or weeks).
- **Photo backgrounds.** Photo-picker import; crop by pinch-zoom and drag; four fit modes; opacity with quick 100/75/50/25% buttons and a background colour underneath; brightness, contrast, saturation and blur; dark and colour overlays.
- **Dots.** Filled, empty, today-highlight and accent/label colours. Separate opacity for filled dots, empty dots and all dots. Circle, rounded or square shapes; solid or outline empty dots; size and spacing controls.
- **Colour picker.** An 84-colour palette, an HSV spectrum, RGB sliders, a validated HEX field (#RGB, #RRGGBB or #AARRGGBB), preset combinations and saved custom colours.
- **Colour suggestions.** Five palettes extracted from the photo (Contrast, Matching, Complementary, Soft, Analogous), all contrast-checked against the image. The best one is applied on import and can be overridden by hand.
- **Layout.** Automatic or manual columns, last-row alignment, separate horizontal and vertical spacing, top/centre/bottom and free positioning, side and top/bottom padding, and an optional progress label.
- **Previews.** Live full-wallpaper, home screen (Classic, Minimal and Widgets launcher layouts) and lock screen views inside a phone frame at the device's real aspect ratio.
- **Themes and saving.** Midnight, Snow, Ocean, Sunset and Forest themes, plus "Custom image". Saved designs can be applied in one tap. Restore defaults is available.
- **Applying and exporting.** Apply to the home screen, lock screen or both. Save to the gallery (Pictures/DOTS), share, and reapply anything from the wallpaper history.
