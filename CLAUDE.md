# CLAUDE.md

Guidance for Claude Code when working with this repository.

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines.**
@./.taskmaster/CLAUDE.md

## Git Configuration

- **Repository:** https://github.com/meuse24/forwarderA1
- **Current Branch:** `backup-broken-stand` (stable)
- **Main Branch:** `main` (contains failed Hilt DI experiment - do not use)
- **Auth:** Personal Access Token in `.env` as `GITHUB_TOKEN`

```bash
# Push to GitHub
source .env
git push https://meuse24:$GITHUB_TOKEN@github.com/meuse24/forwarderA1.git backup-broken-stand
```

## Project Overview

**SMS Forwarder Neo** - Android app that forwards received SMS messages via SMS and email. Runs as foreground service for reliable background processing.

- **Target SDK:** 34, **Min SDK:** 29 (Android 10+)
- **Stack:** Kotlin 1.9.0, Jetpack Compose, JDK 17
- **Architecture:** Clean Architecture (data/domain/presentation layers)

## Build Commands

```bash
./gradlew assembleDebug          # Debug build
./gradlew assembleRelease        # Release build
./gradlew installDebug           # Install to device
./gradlew compileDebugKotlin     # Quick compile check
./gradlew test                   # Run tests
./gradlew clean                  # Clean build
```

## Architecture

### Package Structure
```
info.meuse24.smsforwarderneoA1/
├── data/
│   └── local/              # Logger, SharedPreferencesManager
├── domain/model/           # Contact, LogEntry
├── presentation/
│   ├── ui/
│   │   ├── screens/       # home/, mail/, settings/, logs/, info/
│   │   └── components/    # dialogs/, navigation/
│   ├── viewmodel/         # All ViewModels (6 total)
│   └── state/             # UI state models
├── service/               # SmsReceiver, SmsForegroundService
└── util/                  # email/, permission/, phone/, sms/
```

### Key Files (Current State)
- `MainActivity.kt` - 835 lines (Activity core, permissions, Contact Picker)
- `ContactsViewModel.kt` - 850 lines (contact selection via picker, forwarding)
- `PhoneSmsUtils.kt` - 1,380 lines (SMS/phone utilities)
- `SmsForegroundService.kt` - Foreground service with WakeLock, parallel forwarding
- `SmsReceiver.kt` - BroadcastReceiver for incoming SMS

### ViewModels (Factory Pattern)
- `ContactsViewModel` - Core contact selection & forwarding logic
- `LogViewModel` - Logging display & filtering
- `EmailViewModel` - Email configuration & sending
- `SimManagementViewModel` - SIM card management
- `TestUtilsViewModel` - Test SMS functionality
- `NavigationViewModel` - Navigation & error state

### Data Layer
- `SharedPreferencesManager` - Encrypted preferences (androidx.security.crypto), stores contact name & number
- `Logger` - Structured XML logging with rotation & export

### Service Layer
- `SmsForegroundService` - Multi-part SMS reconstruction, parallel SMS/email forwarding, WakeLock, heartbeat monitoring
- `SmsReceiver` - Receives `SMS_RECEIVED_ACTION`, forwards to service

## Development Guidelines

### Required Permissions
- `RECEIVE_SMS`, `SEND_SMS` - SMS functionality
- `READ_CONTACTS` - Contact selection
- `CALL_PHONE`, `READ_PHONE_STATE` - USSD codes, phone utilities
- `FOREGROUND_SERVICE*` - Background processing
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Service reliability

### Security
- All preferences encrypted via `androidx.security.crypto`
- SMTP passwords stored securely
- Phone numbers logged for debugging (acceptable for private app)

### Multi-part SMS
- Grouped by `sender + referenceNumber`
- Ordered by `sequencePosition`, then `timestamp`
- Automatically reconstructed before forwarding

### Service Management
```kotlin
SmsForegroundService.startService(context)
SmsForegroundService.stopService(context)
```
- Uses `START_STICKY` for auto-restart
- Heartbeat monitoring with cooldown logic

## Common Tasks

### Add New Preference
1. Add getter/setter to `SharedPreferencesManager` (with encryption)
2. Add StateFlow to appropriate ViewModel
3. Update UI in relevant screen component

### Modify SMS Processing
1. Edit `SmsForegroundService.processMessageGroup()`
2. Add logging via `LoggingManager.log()`
3. Test with single & multi-part messages

### Add UI Screen
1. Create composable in `presentation/ui/screens/<name>/`
2. Add navigation route in `MainActivity.kt`
3. Update `BottomNavigationBar` if needed

### Add ViewModel
1. Create in `presentation/viewmodel/`
2. Implement `Factory` inner class
3. Instantiate in `MainActivity` via `viewModels { Factory(...) }`
4. Pass to composables as parameter

## Current Architecture Status

**Clean Architecture refactoring completed (Phases 1-5):**
- ✅ Package structure established
- ✅ Domain models extracted
- ✅ Data layer separated (Logger, SharedPrefs)
- ✅ MainActivity decomposed: 3,870 → 835 lines (-78%)
- ✅ ViewModels extracted: ContactsViewModel 2,341 → 850 lines (-64%)
- ✅ All critical errors resolved (permissions, null safety, lifecycle, coroutines)

**Contact Selection Simplification (2025-01-20):**
- ✅ Replaced contact list + search with Android Contact Picker
- ✅ Removed ContactsRepositoryImpl (~582 lines)
- ✅ Removed 4 UI components (FilterAndLogo, ContactList, ControlButtons, ForwardingStatus)
- ✅ Contact data stored directly in SharedPreferencesManager
- ✅ Net reduction: -1,249 lines of code
- ✅ New features: Reset button, Status query button, Test-SMS in contact card

**Contact Selection UI:**
- **No contact selected:** Large "Kontakt für Weiterleitung auswählen" button → launches Android Contact Picker
- **Contact selected:** Card showing name, number, type with buttons:
  - "Kontakt ändern" / "Test-SMS" (top row)
  - "Deaktivieren" (bottom, red, full width)
- **Always visible:**
  - "Status abfragen" button (queries MMI forwarding status)
  - "Alle Weiterleitungen zurücksetzen" button (red, stops SMS + email forwarding, queries status)

**App is stable and production-ready.**
