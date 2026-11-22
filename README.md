# 倒數日 (Countdown)

Android app + home-screen widgets for counting down to dates — and counting how long ago they were.

**Counting**
- Count every day, weekdays only, or any custom set of weekdays (e.g. skip Fridays).
- Holidays: named date ranges (or single days) you can skip per event; import them from a school calendar (`.ics`).
- Past dates show "N days ago"; yearly events (birthdays) roll to the next occurrence; progress bar from creation to target.
- Pin, category and colour tag per event.

**Widgets** (small 2×2, wide 4×1, list 4×2) — pick an event, optional custom background image with adjustable overlay and text colour.

**Notifications** at round numbers (10, 20, 30… then 7, 5, 3, 2, 1 and the day itself), once per value, at a time you choose.

**Backup / restore** to a JSON file (includes background images). Light and dark theme, English and 繁體中文.

## Build
Android Studio, or `gradle :app:assembleDebug` (Gradle 8.10+, JDK 17, Android SDK 34).
GitHub Actions builds `app-debug.apk` on every push; pushing a `v*` tag attaches it to a GitHub Release.
To make APKs update in place, add the repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` (see `.github/workflows/build.yml`).
