<div align="center">

<img src="docs/icon.png" width="128" alt="倒數日 app icon" />

# 倒數日 · Countdown

**算還剩幾個上課日、也看事情過了多久的 Android 倒數 App 與桌面小工具**
**An Android countdown app & home-screen widgets that count school days — and how long ago things happened.**

[![Build APK](https://github.com/DL049/Countdown/actions/workflows/build.yml/badge.svg)](https://github.com/DL049/Countdown/actions/workflows/build.yml)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)

[繁體中文](#繁體中文) · [English](#english)

</div>

---

## 繁體中文

### 特色

| | |
|---|---|
| 🗓 **只算上課日** | 每天、只算平日，或自訂星期（例如週五沒課就取消勾選），週末和停課日不會被算進去 |
| 🏖 **假期可以不算** | 建立假期（一段日期或單日停課），每個倒數各自勾選要略過哪些；可從學校行事曆 `.ics` 一次匯入 |
| ⏳ **過去的日期** | 過了的日期會顯示「N 天前」，到當天顯示「就是今天 🎉」 |
| 🔁 **每年重複** | 生日、紀念日自動算到下一次 |
| 📊 **進度條** | 從建立事件到目標日，一眼看出走了多少 |
| 🔔 **整數日通知** | 剩 10、20、30… 天，以及最後 7、5、3、2、1 天和當天各通知一次，時間自訂 |
| 🧩 **三種桌面小工具** | 小（2×2）、長條（4×1）、清單（4×2，一次列出最近 4 個）；可自訂背景圖片、遮罩深淺和文字顏色 |
| 📌 **整理事件** | 置頂、分類、5 種顏色標籤 |
| 💾 **備份與還原** | 匯出成單一 JSON 檔（含背景圖片），換手機或重裝後匯入即可 |
| 🎨 **Soft UI 介面** | 柔和雙向陰影、凹陷區塊，符合 WCAG AA 對比；支援深色模式、繁體中文與英文 |

### 安裝

1. 到 [Releases](https://github.com/DL049/Countdown/releases) 下載最新的 `app-debug.apk`；沒有 Release 的話，到 [Actions](https://github.com/DL049/Countdown/actions) 點最新一次成功的執行，在最下方 **Artifacts** 下載 `countdown-debug-apk`（zip，解壓後是 APK）。
2. 傳到手機，允許「安裝未知來源的應用程式」後安裝。
3. 想讓之後的新版可以**直接覆蓋更新**（不用刪除重裝），請先設定固定的簽名金鑰，見下方[簽名與更新](#簽名與更新)。

### 使用方式

1. 打開 App →「倒數」分頁 → 右下角 **＋**，輸入名稱和目標日期。
2. 在「計入的星期」選擇要算的星期；在「不計算這些假期」勾選要略過的假期（假期在「假期」分頁建立）。
3. 長按桌面 → 小工具 → **倒數日**，選擇小、長條或清單，再挑一個倒數事件。點小工具可以直接編輯它。
4. 「設定」分頁可以開啟通知、匯入 `.ics` 行事曆、備份與還原。

### 怎麼計算

從**明天**算到目標日（含目標日）；勾選「今天也算進去」會把今天也算進去（今天必須是計入的星期，且不在假期內）。

| 設定 | 例子（今天是週一） | 結果 |
|---|---|---|
| 每天 | 倒數到 10 天後 | 10 |
| 只算平日 | 倒數到下週一 | 5（週二～週五＋下週一） |
| 只算平日＋週三、四放假 | 倒數到下週一 | 3 |
| 自訂（週一、二、四） | 倒數到兩週後 | 6 |

過去的日期用日曆天數顯示「N 天前」。

### 整數日通知規則

每天在設定的時間檢查一次，剩餘數量為 **1、2、3、5、7**，或 **10 以上的 10 的倍數**，或當天時通知。同一個數字只通知一次（週末、假期數字不變時不會重複）。每個事件可以個別關閉。

### 簽名與更新

Android 只允許**簽名相同**的 APK 覆蓋安裝。GitHub Actions 預設每次會用隨機的 debug 金鑰，所以每次都要先刪除舊版。要固定簽名：

```bash
# 在自己的電腦產生金鑰（請妥善備份，弄丟就無法再更新同一個 App）
keytool -genkeypair -keystore countdown.keystore -alias countdown \
        -keyalg RSA -keysize 2048 -validity 36500
base64 -w0 countdown.keystore      # macOS: base64 -i countdown.keystore
```

到 repo 的 **Settings → Secrets and variables → Actions** 新增三個 secret：

| 名稱 | 內容 |
|---|---|
| `KEYSTORE_BASE64` | 上面 `base64` 輸出的字串 |
| `KEYSTORE_PASSWORD` | 金鑰密碼 |
| `KEY_ALIAS` | `countdown` |

設定後的建置會用同一把金鑰簽名，`versionCode` 也會隨 Actions 執行次數遞增。第一次換成固定金鑰時要先刪除舊版一次。**請不要把金鑰檔提交到 repo。**

### 建置

需要 JDK 17、Android SDK 34、Gradle 8.10+。

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

或直接用 Android Studio 開啟專案並執行。推送 `v*` 標籤（例如 `v1.0`）時，Actions 會自動建立 GitHub Release 並附上 APK。

### 專案結構

```
app/src/main/java/com/example/countdown/
├── CountdownCalc.kt        資料模型與日期計算（純 Kotlin，有單元測試）
├── IcsParser.kt            .ics 行事曆解析（有單元測試）
├── Repo.kt                 事件、假期、設定、備份還原（SharedPreferences + JSON）
├── Fmt.kt                  文字、顏色與日期格式
├── SoftViews.kt            Soft UI 元件：SoftFrame（凸起/凹陷）、SoftButton、SoftMeter
├── MainActivity.kt         底部分頁：倒數 / 假期 / 設定
├── Fragments.kt            倒數與假期清單
├── SettingsFragment.kt     通知、.ics 匯入、備份
├── EventEditActivity.kt    編輯倒數（含小工具背景圖片）
├── HolidayEditActivity.kt  編輯假期
├── WidgetUpdater.kt        三種小工具的繪製與午夜更新
├── CountdownWidgetProvider.kt  小 / 長條 / 清單 小工具
├── WidgetConfigActivity.kt 新增小工具時選擇事件
├── WidgetBg.kt             小工具背景圖片處理
└── Notifier.kt             整數日通知
```

純 Android 框架加 Material Components，沒有其他第三方函式庫；資料全部存在手機內，App 不會連網。

---

## English

### Features

| | |
|---|---|
| 🗓 **School days only** | Count every day, weekdays only, or any custom set of weekdays (e.g. untick Friday). Weekends and days off are skipped. |
| 🏖 **Skippable holidays** | Define holidays (date ranges or single days) and choose per countdown which ones to skip. Import them from a school calendar (`.ics`). |
| ⏳ **Past dates** | Dates that have passed show "N days ago"; on the day it says "It's today 🎉". |
| 🔁 **Yearly repeat** | Birthdays and anniversaries roll to the next occurrence. |
| 📊 **Progress bar** | From when you created the event to its target date. |
| 🔔 **Round-number notifications** | At 10, 20, 30… left, then 7, 5, 3, 2, 1 and on the day — once each, at a time you choose. |
| 🧩 **Three widgets** | Small (2×2), wide (4×1), list (4×2, next 4 events). Custom background image with adjustable overlay and text colour. |
| 📌 **Organise** | Pin, category and 5 colour tags. |
| 💾 **Backup & restore** | Export everything (including background images) to one JSON file. |
| 🎨 **Soft UI** | Soft dual shadows and inset wells, WCAG AA contrast, light & dark theme, 繁體中文 and English. |

### Install

1. Download `app-debug.apk` from [Releases](https://github.com/DL049/Countdown/releases), or open the latest green run in [Actions](https://github.com/DL049/Countdown/actions) and download the **`countdown-debug-apk`** artifact (a zip containing the APK).
2. Sideload it (allow "install unknown apps").
3. To make future builds **update in place**, set up a fixed signing key first — see [Signing](#signing).

### How counting works

Days are counted from **tomorrow** up to and including the target date; "Count today too" adds today (if it is a counted weekday and not a holiday). Only the weekdays you tick count, and any day inside a skipped holiday does not. Dates in the past show calendar days since.

### Round-number notifications

Checked once a day at the chosen time: a notification fires when the remaining count is **1, 2, 3, 5, 7**, or a **multiple of 10 from 10 up**, or on the day itself. Each value is announced once. Each countdown can opt out.

### Signing

Android only installs an update over an app signed with the **same key**, but Actions generates a random debug key on every run. To fix this, generate a keystore on your own machine (`keytool -genkeypair -keystore countdown.keystore -alias countdown -keyalg RSA -keysize 2048 -validity 36500`), then add the repository secrets `KEYSTORE_BASE64` (`base64 -w0 countdown.keystore`), `KEYSTORE_PASSWORD` and `KEY_ALIAS` (`countdown`). Keep the keystore backed up and **never commit it**. The first switch to the fixed key needs one uninstall.

### Build

JDK 17, Android SDK 34, Gradle 8.10+:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

Pushing a `v*` tag (e.g. `v1.0`) makes Actions publish a GitHub Release with the APK attached.

Plain Android framework + Material Components, no other third-party libraries. All data stays on the device; the app never goes online.
