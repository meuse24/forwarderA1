# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines.**
@./.taskmaster/CLAUDE.md

## Git Configuration

### GitHub Repository
- **Repository:** https://github.com/meuse24/forwarderA1
- **Branch:** main
- **Authentication:** Personal Access Token in `.env` as `GITHUB_TOKEN`

### Pushing to GitHub
```bash
source .env
git push https://meuse24:$GITHUB_TOKEN@github.com/meuse24/forwarderA1.git main
```

## Project Overview

SMS Forwarder Neo is an Android application that forwards received SMS messages via SMS and email. The app runs as a foreground service to ensure reliable message forwarding.

## Build Commands

```bash
# Build debug APK (most common)
./gradlew assembleDebug

# Other commands
./gradlew assembleRelease        # Release build
./gradlew installDebug           # Install to device
./gradlew test                   # Run tests
./gradlew clean                  # Clean build
```

### Gradle Configuration
- Target SDK: 34, Min SDK: 29 (Android 10+)
- JDK 17, Kotlin 1.9.0, Jetpack Compose
- Version catalog: `gradle/libs.versions.toml`

### WSL Build Environment

**CRITICAL: This is a recurring issue. Always fix before building.**

```bash
# Fix line endings and set JAVA_HOME
sed -i 's/\r$//' gradlew && chmod +x gradlew
export JAVA_HOME="/mnt/c/Program Files/Android/Android Studio/jbr"
./gradlew assembleDebug
```

**Java Location:**
- WSL path: `/mnt/c/Program Files/Android/Android Studio/jbr`
- Verify: `"/mnt/c/Program Files/Android/Android Studio/jbr/bin/java.exe" -version`

## Architecture Overview

### Package Structure (Clean Architecture)

```
info.meuse24.smsforwarderneoA1/
├── data/
│   ├── local/              # Logger, SharedPreferencesManager
│   └── repository/         # ContactsRepositoryImpl
├── domain/
│   └── model/             # Contact, LogEntry
├── presentation/
│   ├── ui/
│   │   ├── screens/       # home/, mail/, settings/, logs/, info/
│   │   └── components/    # dialogs/, navigation/
│   ├── viewmodel/         # LogViewModel, EmailViewModel, SimManagementViewModel
│   └── state/             # ContactsState
├── service/               # SmsReceiver, SmsForegroundService
└── util/
    ├── email/             # EmailSender
    ├── permission/        # PermissionHelper
    ├── phone/             # CarrierTrie
    └── sms/               # Gsm7BitEncoder
```

### Core Components

**Application Layer:**
- `SmsForwarderApplication.kt` - AppContainer (DI), LoggingManager, SnackbarManager

**Service Layer:**
- `SmsReceiver.kt` - BroadcastReceiver for incoming SMS
- `SmsForegroundService.kt` - Foreground service with WakeLock, parallel forwarding, multi-part SMS reconstruction

**UI Layer:**
- `MainActivity.kt` (819 lines) - Activity core, permission handling
- `presentation/ui/screens/` - Modular screen components (home, mail, settings, logs, info)
- `presentation/ui/components/` - Dialogs and navigation

**Data Layer:**
- `ContactsViewModel.kt` (~1,955 lines) - Main ViewModel
- `LogViewModel.kt`, `EmailViewModel.kt`, `SimManagementViewModel.kt` - Extracted ViewModels
- `SharedPreferencesManager.kt` - Encrypted preferences
- `ContactsRepositoryImpl.kt` - Contacts data repository
- `Logger.kt` - Structured logging with XML persistence

**Utilities:**
- `PhoneSmsUtils.kt` - SMS sending, phone number formatting, carrier detection
- `EmailSender.kt` - SMTP email sending
- `CarrierTrie.kt` - Carrier prefix lookup

### Data Flow
1. SMS Reception: `SmsReceiver` → `SmsForegroundService`
2. Processing: Multi-part reconstruction, parallel forwarding
3. Output: SMS via `PhoneSmsUtils`, email via `EmailSender`
4. Feedback: `SnackbarManager`, `LoggingManager`

## Development Guidelines

### Permissions Required
- `RECEIVE_SMS`, `SEND_SMS` - Core SMS functionality
- `READ_CONTACTS` - Contact selection
- `CALL_PHONE`, `READ_PHONE_STATE` - Phone number utilities
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_PHONE_CALL` - Background processing
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Service reliability

### Data Security
- All preferences encrypted via `androidx.security.crypto`
- SMTP passwords stored securely
- Phone numbers logged for debugging

### Service Management
- Use `SmsForegroundService.startService(context)` / `stopService(context)`
- Service uses `START_STICKY` for automatic restart
- Heartbeat monitoring with auto-restart logic

### Multi-part SMS Handling
- Messages grouped by `sender + referenceNumber`
- Parts ordered by `sequencePosition` then `timestamp`
- Automatic reconstruction before forwarding

## Common Development Tasks

### Adding New Preferences
1. Add to `SharedPreferencesManager` with encryption
2. Update UI in appropriate screen component
3. Handle migration if needed

### Modifying SMS Processing Logic
1. Modify `SmsForegroundService.processMessageGroup()`
2. Update logging in `LoggingManager` calls
3. Test with single and multi-part messages

### Adding UI Components
- Add screens to `presentation/ui/screens/`
- Add reusable components to `presentation/ui/components/`
- Update `MainActivity.kt` navigation if needed

## Refactoring Status

### Completed Clean Architecture Refactoring (2025-11)

The codebase was transformed from monolithic files to Clean Architecture:

**Phase 1-3: Foundation & Data Layer ✅**
- Established package structure (data/, domain/, presentation/, service/, util/)
- Extracted domain models (Contact, LogEntry, ContactsState)
- Extracted data layer (Logger, SharedPreferencesManager, ContactsRepositoryImpl)
- Extracted utilities (EmailSender, PermissionHelper, Gsm7BitEncoder, CarrierTrie)

**Phase 4: MainActivity UI Decomposition ✅**
- Reduced MainActivity from 3,870 → 819 lines (-79%)
- Created 24 modular UI components in screens/ and components/
- Improved maintainability and testability

**Phase 5: ViewModel Decomposition ✅**
- Extracted LogViewModel (134 lines) - logging logic
- Extracted EmailViewModel (377 lines) - email management
- Extracted SimManagementViewModel (107 lines) - SIM management
- Reduced ContactsViewModel by 386 lines (-16.5%)

**Current State:**
- `MainActivity.kt`: 819 lines (focused activity core)
- `ContactsViewModel.kt`: ~1,955 lines (still contains forwarding logic)
- `PhoneSmsUtils.kt`: ~1,380 lines (SMS/phone utilities)
- All critical fatal errors resolved (permissions, null safety, lifecycle, coroutines)

**⚠️ Branch Note:**
- Current branch: `backup-broken-stand` (stable)
- `main` branch contains failed Hilt DI experiment (do not use)

## Critical Issues - All Resolved ✅

All 7 critical fatal errors fixed (2025-09-17):
1. ✅ USSD permission logic corrected
2. ✅ Lateinit property guards added
3. ✅ MainActivity initialization race condition fixed
4. ✅ System service null checks added
5. ✅ ContentObserver lifecycle fixed
6. ✅ Unsafe type casts replaced with safe casts
7. ✅ Comprehensive exception handling in coroutines

Application is stable and production-ready.
