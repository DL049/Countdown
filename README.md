# 倒數日 (Countdown) widget

Android app + home-screen widgets for counting down to dates.

- **App**: manage all your countdowns (title, target date) in one place, and a **Holidays** tab (named date ranges).
- **Count mode** per event: every day, or weekdays only (skip Sat/Sun → "how many school days left").
- **Skip holidays**: tick which holidays should NOT be counted for each event.
- **Widgets**: long-press home screen → Widgets → 倒數日, pick an event. Tap a widget to edit its event. Updates every midnight.

## Build
Android Studio, or `gradle :app:assembleDebug` (Gradle 8.10+, JDK 17, Android SDK 34).
GitHub Actions builds `app-debug.apk` on every push; pushing a `v*` tag attaches it to a GitHub Release.
