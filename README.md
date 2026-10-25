# Pocket Ledger — Personal Finance Tracker

Offline-first Android personal finance tracker for income, expenses, budgets, and savings.

## Stack
- Kotlin + Jetpack Compose + Material 3
- Room local database
- MVVM-friendly separation
- GitHub Actions debug build
- No mandatory account, cloud, or paid AI API

## Build
Open this repository in Android Studio with JDK 17 and Android SDK 35, sync Gradle, then run:
```bash
gradle assembleDebug
```
The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy
Core records are stored locally. Do not commit real financial data, signing keys, or credentials.

## Roadmap
The initial app scaffold starts with manual income/expense tracking. Budgets, savings goals, reports, CSV backup/restore, editing/deleting, localization, and rigorous currency conversion must be implemented and tested before calling this release-ready.

**Currency safety:** Never sum BDT, KRW, and USD as if they were one currency. Until explicit exchange-rate conversion is implemented, use one base currency for totals.
