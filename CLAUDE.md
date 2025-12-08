# CLAUDE.md

Guidance for Claude Code when working with this repository.

## Git Configuration

- **Repository:** https://github.com/meuse24/forwarderA1
- **Current Branch:** `main` (stable, production-ready)
- **Auth:** Personal Access Token in `.env` as `GITHUB_TOKEN`

```bash
# Push to GitHub
source .env
git push https://meuse24:$GITHUB_TOKEN@github.com/meuse24/forwarderA1.git main
```

## Project Overview

**SMS Forwarder Neo** - Android app that forwards received SMS messages via SMS and email. Runs as foreground service for reliable background processing.

- **Target SDK:** 34, **Min SDK:** 29 (Android 10+)
- **Stack:** Kotlin 1.9.0, Jetpack Compose, JDK 17
- **Architecture:** Clean Architecture (data/domain/presentation layers)

## Build Commands

**IMPORTANT:** Use `build.sh` script instead of direct `./gradlew` commands to ensure correct JAVA_HOME is set:

```bash
./build.sh                       # Quick compile (compileDebugKotlin)
./build.sh assembleDebug         # Debug build
./build.sh assembleRelease       # Release build
./build.sh installDebug          # Install to device
./build.sh test                  # Run tests
./build.sh clean                 # Clean build
```

**Why `build.sh`?**
- Automatically sets `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`
- Works reliably in Git Bash on Windows
- Prevents "JAVA_HOME is set to an invalid directory" errors
- Persists across terminal sessions and system restarts

See `BUILD.md` for detailed build instructions and troubleshooting.

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

**International Dial Prefix Implementation (2025-01-29):**
- ✅ Configurable international dial prefix in App Settings (default: "00" for Austria)
- ✅ Applies to both MMI codes and SMS sending
- ✅ Replaces "+" with configured prefix before transmission
- ✅ Loop detection normalized with dial prefix for accurate comparison
- **Components:**
  - `SharedPreferencesManager`: `getInternationalDialPrefix()` / `setInternationalDialPrefix()`
  - `AppSettingsSection`: Editable text field with validation (digits only, max 3 chars)
  - `MainActivity.kt`: MMI code normalization uses configured prefix
  - `PhoneSmsUtils.kt`: SMS functions normalize phone numbers with configured prefix
  - `PhoneNumberValidator.kt`: `areSameNumber()` normalizes both numbers for comparison

**MMI Warning Control (2025-01-31):**
- ✅ Configurable 4-second warning before MMI code execution in MMI Code Settings
- ✅ Toggle switch to enable/disable warning message (default: enabled)
- ✅ Skips delay when disabled for immediate MMI code execution
- ✅ Interactive countdown button dialog (2025-12-05)
  - Full-screen overlay with gradient background
  - Pulsating officer logo with glow effect
  - Countdown button showing remaining seconds
  - User can skip countdown by clicking "Jetzt wählen" button
  - Auto-dismiss after 4 seconds OR manual skip
- **Components:**
  - `SharedPreferencesManager`: `isMmiWarningEnabled()` / `setMmiWarningEnabled()`
  - `ContactsViewModel`: `mmiWarningEnabled` StateFlow
  - `MmiCodeSettingsSection`: Switch control with description
  - `MainActivity.kt`: Conditional warning display and delay based on setting
  - `MmiWarningDialog.kt`: Modern dialog component with interactive countdown button

**UI Cleanup (2025-01-31):**
- ✅ Removed obsolete "Telefonnummern formatieren" setting (no longer used since Contact Picker implementation)
- ✅ Removed redundant "Weiterleitung aktiv" checkbox from Phone Settings (status already shown in Home Screen)
- **Removed Components:**
  - Phone number formatting preference, StateFlow, and UI toggle
  - Unused `formatPhoneNumber()` function and helper methods (~80 lines)
  - Read-only forwarding status checkbox in PhoneSettingsSection
- **Result:** Cleaner Settings UI showing only relevant, actionable options

**Dependency Updates (2025-12-07):**
- ✅ Updated to stable, production-ready versions
- ✅ AGP 8.7.3 (stable) instead of 8.13.1 beta
- ✅ Compose BOM 2024.11.00, Core-KTX 1.15.0, libphonenumber 8.13.52
- ✅ Navigation Compose 2.8.5, WorkManager 2.10.0
- ✅ Activity-Compose 1.9.3 (compatible with AGP 8.7.3)
- ✅ Fixed deprecation warnings: AutoMirrored icons, TRIM_MEMORY suppression
- ✅ Version references in libs.versions.toml prevent auto-updates

**LoadingScreen Improvements (2025-12-07):**
- ✅ Buttons ("Neu versuchen", "App beenden") only shown on errors
- ✅ Clean loading state without buttons during initialization
- ✅ Conditional button display via nullable callbacks

**UI Design Enhancements (2025-12-08):**
- ✅ Custom warm golden gradient (`WarmContactGradient`) matching wallpaper colors
  - Light golden yellow (#E8C547, 70% opacity) → Darker golden/tan (#D4A853, 60% opacity)
  - Applied to all 4 main action buttons on Home Screen
- ✅ Consistent button styling across Home Screen:
  - Contact selection button (75% width, 120dp height)
  - 3 action buttons (Help, Status, Reset) in horizontal row
  - All buttons: RoundedCornerShape(12.dp), black border (2dp, 80% opacity)
  - All buttons grouped in rotated container (2° counter-clockwise)
- ✅ Rotation consistency via parent container instead of individual button rotation
- **Components:**
  - `Gradient.kt`: New `WarmContactGradient` definition
  - `AnimatedButton.kt`: `GradientButton` component with border support
  - `HomeScreen.kt`: Unified button container with consistent rotation

**App is stable and production-ready.**
