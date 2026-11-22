# Countdown widget

Android home-screen widget that counts down to a date, either:

- **Every day** – calendar days left, or
- **Weekdays only** – Mon–Fri days left (weekends skipped), handy for "how many school days until break".

Add it from *long-press home screen → Widgets → Countdown*. Tap a widget to edit it. Each widget has its own title/date/mode. Updates every midnight.

"Count today too" includes today in the total (weekdays mode only counts it if today is Mon–Fri).

## Build
Open in Android Studio and run, or `gradle :app:assembleDebug` (Gradle 8.10+, JDK 17, Android SDK 34).
GitHub Actions (`.github/workflows/build.yml`) builds `app-debug.apk` on every push — download it from the run's artifacts and sideload it.
