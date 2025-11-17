# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md

## Git Configuration

### GitHub Repository
- **New Repository:** https://github.com/meuse24/forwarderA1
- **Branch:** main
- **Authentication:** Personal Access Token stored in `.env` as `GITHUB_TOKEN`

### Pushing to GitHub
For future pushes, use the token from `.env`:
```bash
# Read token from .env
source .env
git push https://meuse24:$GITHUB_TOKEN@github.com/meuse24/forwarderA1.git main
```

Or configure Git credential helper once:
```bash
git config credential.helper store
# Then push normally, Git will ask for credentials once and remember them
git push
```

## Project Overview

SMS Forwarder Neo is an Android application that forwards received SMS messages via SMS and email. The app runs as a foreground service to ensure reliable message forwarding even when the app is not actively in use.

## Build Commands

### Build and Run
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build to connected device/emulator
./gradlew installDebug

# Run tests
./gradlew test

# Run Android instrumentation tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Lint code
./gradlew lint
```

### Gradle Configuration
- Target/Compile SDK: 34
- Min SDK: 29 (Android 10+)
- Java/Kotlin: JDK 17, Kotlin 1.9.0
- Uses Jetpack Compose for UI
- Version catalog in `gradle/libs.versions.toml`

### WSL Build Environment

**IMPORTANT: JAVA_HOME Configuration**

This project is developed in WSL (Windows Subsystem for Linux). The `gradlew` script often has line ending issues and requires JAVA_HOME to be set correctly.

**Common Issues and Solutions:**

1. **Line Ending Problems:**
   ```bash
   # Fix gradlew line endings (CRLF → LF)
   sed -i 's/\r$//' gradlew && chmod +x gradlew
   ```

2. **JAVA_HOME Not Set:**
   - Java is installed at: `C:\Program Files\Android\Android Studio\jbr`
   - WSL mount path: `/mnt/c/Program Files/Android/Android Studio/jbr`
   - The Windows path works better for gradle execution

3. **Building the APK:**
   ```bash
   # Method 1: Fix gradlew and set JAVA_HOME (preferred for WSL)
   sed -i 's/\r$//' gradlew && chmod +x gradlew
   export JAVA_HOME="/mnt/c/Program Files/Android/Android Studio/jbr"
   ./gradlew assembleDebug

   # Method 2: Use direct java.exe path (if Method 1 fails)
   "/mnt/c/Program Files/Android/Android Studio/jbr/bin/java.exe" -version  # Test Java
   export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
   # Then use gradlew.bat or gradle wrapper
   ```

4. **Verify Java Installation:**
   ```bash
   "/mnt/c/Program Files/Android/Android Studio/jbr/bin/java.exe" -version
   # Should output: openjdk version "21.0.8" or similar
   ```

**Note:** This is a recurring issue. Always fix gradlew line endings and set JAVA_HOME before building.

## Architecture Overview

### Core Components

**Application Layer (`SmsForwarderApplication.kt`):**
- `AppContainer` - Dependency injection container with two-stage initialization (critical → activity-dependent)
- `LoggingManager` - Centralized logging system with structured metadata
- `SnackbarManager` - Application-wide snackbar management with queuing

**Service Layer (`SmsReceiver.kt`):**
- `SmsReceiver` - BroadcastReceiver for incoming SMS (`SMS_RECEIVED_ACTION`)
- `SmsForegroundService` - Persistent foreground service handling SMS forwarding with:
  - WakeLock management for reliable processing
  - Parallel SMS and email forwarding
  - Multi-part SMS message reconstruction
  - Heartbeat monitoring and auto-restart logic

**UI Layer (`MainActivity.kt`):**
- Jetpack Compose-based activity with permission handling
- Integration with `ContactsViewModel` for contact selection
- Snackbar integration for user feedback

**Data Management (`ContactsViewModel.kt`):**
- `ContactsViewModel` - Main ViewModel handling contact selection, preferences
- `SharedPreferencesManager` - Encrypted preferences storage using `androidx.security.crypto`
- `PermissionHandler` - Runtime permission management for SMS, contacts, phone access
- `Logger` - Structured logging with metadata support

**Utility Layer (`PhoneSmsUtils.kt`):**
- `PhoneSmsUtils` - SMS sending, phone number formatting, carrier detection
- `EmailSender` - SMTP email sending with configuration management
- `CarrierTrie` - Efficient phone number carrier lookup data structure

### Data Flow

1. **SMS Reception**: `SmsReceiver` → `SmsForegroundService` (with Intent data forwarding)
2. **Processing**: Service reconstructs multi-part messages, applies parallel forwarding
3. **Output**: SMS forwarding via `PhoneSmsUtils`, email via `EmailSender`
4. **Feedback**: Status updates via `SnackbarManager`, logging via `LoggingManager`

### Key Architectural Patterns

- **Service-Oriented**: Heavy reliance on foreground service for background processing
- **Two-Stage Initialization**: Critical components (logging, prefs) initialize early, UI components later
- **Structured Logging**: All components use consistent metadata-based logging
- **Error Recovery**: Service auto-restart with cooldown and attempt limits
- **Resource Management**: WakeLock usage during critical operations with timeouts

## Development Guidelines

### Testing
- Unit tests in `app/src/test/`
- Android instrumentation tests in `app/src/androidTest/`
- Test SMS handling with multiple devices or emulator SMS simulation

### Permissions
The app requires extensive permissions for SMS/phone functionality:
- `RECEIVE_SMS`, `SEND_SMS` - Core SMS functionality
- `READ_CONTACTS` - Contact selection
- `CALL_PHONE`, `READ_PHONE_STATE` - Phone number utilities
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_PHONE_CALL` - Background processing
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Ensure service reliability

### Service Management
- Use `SmsForegroundService.startService(context)` and `stopService(context)`
- Service uses `START_STICKY` for automatic restart
- Monitor service health via heartbeat mechanism
- Handle service lifecycle in `onTaskRemoved` based on user preferences

### Data Security
- All preferences encrypted via `androidx.security.crypto`
- SMTP passwords stored securely
- Log sensitive data carefully (phone numbers are logged for debugging)

### Email Configuration
- SMTP settings required: host, port, username, password
- Support for multiple recipient addresses
- Built-in retry logic for network failures

### Multi-part SMS Handling
- Messages grouped by `sender + referenceNumber`
- Parts ordered by `sequencePosition` then `timestamp`
- Automatic message reconstruction before forwarding

## Common Development Tasks

### Adding New SMS Processing Logic
1. Modify `SmsForegroundService.processMessageGroup()`
2. Update logging in `LoggingManager` calls
3. Test with both single and multi-part messages

### Modifying Email Templates
1. Update `SmsForegroundService.buildEmailBody()`
2. Ensure proper escaping for different message content types

### Adding New Preferences
1. Add to `SharedPreferencesManager` with encryption
2. Update UI in `MainActivity` / `ContactsViewModel`
3. Handle migration if needed

### Service Reliability Improvements
1. Modify restart logic in `SmsForegroundService.restartService()`
2. Update heartbeat intervals in `monitorService()`
3. Test battery optimization scenarios

### Permission Changes
1. Update `AndroidManifest.xml`
2. Add handling in `PermissionHandler`
3. Update UI permission request flow

## Clean Architecture Refactoring (2025-11)

### Overview

A comprehensive refactoring was undertaken to transform the codebase from a monolithic structure to a Clean Architecture pattern. The refactoring was executed in careful, incremental phases with git commits for each step, enabling easy rollback if needed.

### Completed Phases

#### Phase 1: Foundation & Package Structure ✅

**Objective**: Establish Clean Architecture folder structure and extract core domain models and utilities.

**Commits**: 3 commits
- `feat: Add Clean Architecture package structure with documentation`
- `refactor: Extract domain models (Contact, LogEntry, ContactsState)`
- `refactor: Extract utility classes (PermissionHelper, Gsm7BitEncoder, CarrierTrie)`

**Package Structure Created**:
```
info.meuse24.smsforwarderneoA1/
├── data/               # Data layer (persistence, caching, external sources)
│   ├── local/         # Local data sources (SharedPreferences, databases)
│   ├── remote/        # Remote data sources (APIs, services)
│   └── repository/    # Repository implementations
├── domain/            # Business logic layer (framework-independent)
│   └── model/         # Domain models
├── presentation/      # UI layer (Activities, Composables, ViewModels)
│   └── state/         # UI state models
├── service/           # Background services (SMS receiver, foreground service)
└── util/              # Utilities and helpers
    ├── email/         # Email-related utilities
    ├── permission/    # Permission handling
    ├── phone/         # Phone number utilities
    └── sms/           # SMS-related utilities
```

**Files Created**:
1. **Domain Models** (previously embedded in `ContactsViewModel.kt`):
   - `domain/model/Contact.kt` (30 lines) - Contact data with normalized phone number equality
   - `domain/model/LogEntry.kt` (30 lines) - Structured log entries with formatting
   - `presentation/state/ContactsState.kt` (15 lines) - UI state model

2. **Utility Classes** (previously embedded in `PhoneSmsUtils.kt`):
   - `util/permission/PermissionHelper.kt` (25 lines) - Permission checking utilities
   - `util/sms/Gsm7BitEncoder.kt` (85 lines) - GSM 7-bit SMS encoding
   - `util/phone/CarrierTrie.kt` (120 lines) - Trie-based carrier prefix lookup

#### Phase 2: Data Layer Extraction ✅

**Objective**: Extract data management classes from monolithic files to dedicated data layer components.

**Commits**: 4 commits
- `refactor: Extract Logger to data/local layer (630 lines)`
- `refactor: Extract SharedPreferencesManager to data/local (520 lines)`
- `refactor: Extract ContactsStore to ContactsRepositoryImpl (563 lines)`
- `refactor: Extract EmailSender to util/email (83 lines)`

**Files Created**:

1. **`data/local/Logger.kt`** (630 lines):
   - Extracted from `ContactsViewModel.kt`
   - Structured logging system with XML persistence
   - Features: log rotation, highlight patterns, HTML/CSV/List export
   - Includes `LogLevel` enum and `LogMetadata` data class

2. **`data/local/SharedPreferencesManager.kt`** (520 lines):
   - Extracted from `ContactsViewModel.kt`
   - Encrypted preferences using `androidx.security.crypto`
   - Type-safe preference access (String, Boolean, Int, List)
   - Custom `PreferencesInitializationException` for error handling

3. **`data/repository/ContactsRepositoryImpl.kt`** (563 lines):
   - Extracted from `ContactsViewModel.kt` (formerly `ContactsStore`)
   - Repository pattern implementation for contacts data
   - Manages `ContentObserver` for reactive contact updates
   - Loads contacts from Android's `ContactsContract`
   - Proper coroutine lifecycle management with `SupervisorJob`

4. **`util/email/EmailSender.kt`** (83 lines):
   - Extracted from `PhoneSmsUtils.kt`
   - SMTP email sender with authentication
   - Includes `EmailResult` sealed class for result handling
   - Supports multiple recipients and UTF-8 encoding

### Impact Metrics

**File Size Reductions**:
- `ContactsViewModel.kt`: **3,545 → 1,875 lines (-47%)**
- `PhoneSmsUtils.kt`: **1,535 → 1,380 lines (-10%)**

**Code Organization**:
- **14 commits** total (including fixes)
- **10 new files** created with clear separation of concerns
- **~1,800 lines** migrated to proper architectural layers

**Benefits Achieved**:
- ✅ Clear separation between data, domain, and presentation layers
- ✅ Improved testability (utilities and repositories can be unit tested independently)
- ✅ Better code navigation and discoverability
- ✅ Reduced cognitive load in main ViewModel and utility classes
- ✅ Foundation established for dependency injection
- ✅ Easier maintenance and future feature additions

### Current Architecture State

**Main Files After Refactoring**:
1. **`ContactsViewModel.kt`** (1,875 lines):
   - Now focused on presentation logic and UI state management
   - Delegates data operations to `ContactsRepositoryImpl`
   - Uses extracted `Logger` and `SharedPreferencesManager`
   - Still contains some business logic (to be addressed in Phase 3)

2. **`PhoneSmsUtils.kt`** (1,380 lines):
   - Focused on SMS/phone utilities and operations
   - Uses extracted encoding and carrier utilities
   - Still contains multiple responsibilities (to be addressed in Phase 4)

3. **`MainActivity.kt`** (3,871 lines):
   - Unchanged, contains all Compose UI code
   - Target for Phase 5 (UI decomposition)

### Import Strategy & Lessons Learned

**TypeAlias Pattern**:
The codebase uses typealiases in `SmsForwarderApplication.kt` for backward compatibility:
```kotlin
typealias Logger = info.meuse24.smsforwarderneoA1.data.local.Logger
typealias LogLevel = info.meuse24.smsforwarderneoA1.data.local.Logger.LogLevel
typealias LogMetadata = info.meuse24.smsforwarderneoA1.data.local.Logger.LogMetadata
```

**Import Requirements**:
- Inner classes (`LogLevel`, `LogMetadata`) require explicit imports even with typealiases
- `AppContainer` access requires `AppContainer.requireXxx()` methods for lateinit properties
- Coroutines dependencies need comprehensive imports (SupervisorJob, launch, withContext, etc.)

### Future Refactoring Phases (Planned)

#### Phase 3: ViewModel Decomposition
- Break down `ContactsViewModel` into smaller, focused ViewModels
- Extract business logic to use cases/interactors
- Separate email and forwarding state management

#### Phase 4: Service & Utility Refinement
- Extract SMS operations from `PhoneSmsUtils`
- Create dedicated SMS and phone utilities
- Refactor `SmsForegroundService` for better separation

#### Phase 5: UI Decomposition
- Break down `MainActivity.kt` into screen composables
- Extract reusable UI components
- Organize screens by feature

#### Phase 6: Testing Infrastructure
- Add unit tests for extracted repositories and utilities
- Integration tests for data layer
- UI tests for key user flows

#### Phase 7: Dependency Injection
- Replace manual `AppContainer` with Hilt or Koin
- Proper dependency scoping
- Improved testability with DI

#### Phase 8: Performance & Optimization
- Optimize coroutine usage
- Review memory leaks and lifecycle issues
- Performance profiling and improvements

#### Phase 3: Utility Layer Organization ✅

**Objective**: Extract remaining utility classes from PhoneSmsUtils to dedicated packages.

**Commits**: 2 commits (2025-11-17)
- `a70a75c` - refactor: Extract utility classes to dedicated packages
- `ab6776c` - fix: Add missing EmailSender and EmailResult imports to SmsReceiver

**Files Created**:

1. **`util/phone/CarrierTrie.kt`** (64 lines):
   - Extracted from `PhoneSmsUtils.kt`
   - Trie data structure for efficient carrier prefix matching
   - CarrierNode and CarrierTrie classes for O(n) lookup
   - Supports longest prefix matching for overlapping prefixes

2. **`util/email/EmailSender.kt`** (92 lines):
   - Extracted from `PhoneSmsUtils.kt`
   - SMTP email sender with JavaMail API
   - EmailResult sealed class for result handling
   - Supports STARTTLS encryption and multiple recipients

**Removed from PhoneSmsUtils.kt**:
- Inner object Gsm7BitEncoder (58 lines) - already extracted in Phase 2
- CarrierNode and CarrierTrie classes (44 lines)
- EmailSender and EmailResult (56 lines)
- Redundant javax.mail imports

**Import Fixes**:
- Added EmailResult and EmailSender imports to ContactsViewModel.kt
- Added EmailResult and EmailSender imports to SmsReceiver.kt

**Impact**:
- PhoneSmsUtils.kt: **1,535 → 1,360 lines (-11%)**
- ContactsViewModel.kt: **2,325 → 2,341 lines** (imports added)
- Total extracted: ~158 lines to dedicated utility packages
- All compilation errors resolved, project builds successfully

**Status**: Phases 1-3 complete and tested. Ready for Phase 4 (UI Decomposition).

#### Phase 4: MainActivity UI Decomposition (PLANNED - NOT YET EXECUTED)

**Objective**: Break down MainActivity.kt (3,870 lines) into modular screen composables.

**Current State Analysis** (as of 2025-11-17):
- **Total Composables**: 34 in single file
- **Main Screens**: 5 (HomeScreen, MailScreen, SettingsScreen, LogScreen, InfoScreen)
- **Dialogs**: 6 (Loading, Exit, Cleanup Progress/Error, SIM Numbers, PIN/Change PIN)
- **Navigation Components**: 2 (CustomTopAppBar, BottomNavigationBar)
- **Screen-Specific Helpers**: 21 composables distributed across screens

**Proposed Package Structure**:
```
presentation/
├── ui/
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt              # HomeScreen + Landscape/Portrait layouts
│   │   │   ├── FilterAndLogo.kt           # Filter & Logo component
│   │   │   ├── ContactList.kt             # ContactListBox + ContactItem
│   │   │   ├── CallStatusCard.kt          # Call status display
│   │   │   ├── ForwardingStatus.kt        # Forwarding status indicator
│   │   │   └── ControlButtons.kt          # Action buttons
│   │   │
│   │   ├── mail/
│   │   │   └── MailScreen.kt              # Complete Mail screen
│   │   │
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt          # Main settings container
│   │   │   ├── PhoneSettingsSection.kt    # Phone configuration
│   │   │   ├── SimManagementSection.kt    # SIM management
│   │   │   ├── AppSettingsSection.kt      # App settings
│   │   │   ├── MmiCodeSettingsSection.kt  # MMI codes
│   │   │   ├── EmailSettingsSection.kt    # Email SMTP
│   │   │   └── LogSettingsSection.kt      # Log settings
│   │   │
│   │   ├── logs/
│   │   │   ├── LogScreen.kt               # Main log screen
│   │   │   ├── LogTable.kt                # LogTable + LogEntryRow
│   │   │   └── LogButtons.kt              # Refresh, Filter, Share buttons
│   │   │
│   │   └── info/
│   │       └── InfoScreen.kt              # Info screen
│   │
│   ├── components/
│   │   ├── navigation/
│   │   │   ├── CustomTopAppBar.kt         # Top app bar
│   │   │   └── BottomNavigationBar.kt     # Bottom navigation
│   │   │
│   │   └── dialogs/
│   │       ├── LoadingScreen.kt           # Loading state
│   │       ├── ExitDialog.kt              # Exit confirmation
│   │       ├── CleanupDialogs.kt          # Cleanup progress + error
│   │       ├── SimNumbersDialog.kt        # SIM input dialog
│   │       └── PinDialogs.kt              # PIN + Change PIN
│   │
│   └── MainActivity.kt                     # Root UI + Navigation only (~200 lines)
```

**Extraction Strategy** (bottom-up approach):

1. **Step 1: Dialogs** (lowest dependencies)
   - 6 dialogs → 5 files
   - Mostly independent, easy to extract
   - Expected: ~400 lines extracted

2. **Step 2: Log Screen** (low complexity)
   - 5 composables → 3 files (LogScreen.kt, LogTable.kt, LogButtons.kt)
   - Clear boundaries, minimal state
   - Expected: ~400 lines extracted

3. **Step 3: Info Screen** (minimal)
   - 1 composable → 1 file (InfoScreen.kt)
   - Standalone, no dependencies
   - Expected: ~100 lines extracted

4. **Step 4: Mail Screen** (medium complexity)
   - 1 screen → 1 file (MailScreen.kt)
   - Standalone, direct ViewModel binding
   - Expected: ~200 lines extracted

5. **Step 5: Settings Screen** (high complexity)
   - 8 composables → 7 files (1 per section + main)
   - Many sections, state management
   - Expected: ~1,200 lines extracted

6. **Step 6: Home Screen** (highest complexity)
   - 8 composables → 6 files
   - Core feature, many dependencies
   - Expected: ~1,000 lines extracted

7. **Step 7: Navigation Components**
   - 2 composables → 2 files (CustomTopAppBar.kt, BottomNavigationBar.kt)
   - Expected: ~100 lines extracted

8. **Step 8: MainActivity Cleanup**
   - Keep only: setContent + NavHost + UI()
   - Final size: ~200 lines

**Expected Results**:
- MainActivity.kt: 3,870 → ~200 lines (-95%)
- New screen files: ~25 files
- Better organization, testability, and maintainability

**Composable Dependencies** (for extraction reference):

*Home Screen Hierarchy:*
```
HomeScreen (Line 1310)
├── LandscapeLayout (Line 1367)
│   ├── ContactListBox (Line 1606) → ContactItem (Line 1647)
│   ├── FilterAndLogo (Line 1487)
│   ├── CallStatusCard (Line 1676)
│   ├── ForwardingStatus (Line 1719)
│   └── ControlButtons (Line 1793)
└── PortraitLayout (Line 1430)
    ├── FilterAndLogo (Line 1487)
    ├── ContactListBox (Line 1606) → ContactItem (Line 1647)
    ├── CallStatusCard (Line 1676)
    ├── ForwardingStatus (Line 1719)
    └── ControlButtons (Line 1793)
```

*Settings Screen Hierarchy:*
```
SettingsScreen (Line 2038)
├── PhoneSettingsSection (Line 2210)
├── SimManagementSection (Line 2287)
├── AppSettingsSection (Line 2599)
├── MmiCodeSettingsSection (Line 2738)
├── EmailSettingsSection (Line 2847)
├── LogSettingsSection (Line 2131)
├── PinDialog (Line 3357) [conditional]
└── ChangePinDialog (Line 3411) [conditional]
```

*Log Screen Hierarchy:*
```
LogScreen (Line 2974)
├── LogTable (Line 3170)
│   └── LogEntryRow (Line 3222) [repeated]
├── FilterLogButton (Line 3273)
├── ShareLogIconButton (Line 3288)
└── RefreshLogButton (Line 3259)
```

**Git Commit Strategy**:
- One commit per extraction step (8 commits total)
- Test compilation after each commit
- Ensure app runs correctly after each step

**Status**: Phase 4 analyzed and planned. Ready to execute when approved.

## Recent Changes

### Mail Screen Visibility Toggle Implementation (2025)

The application now includes a configurable Mail Screen that can be hidden/shown via the setup screen:

**Key Components Modified:**

1. **SharedPreferencesManager** (`ContactsViewModel.kt:2341-2346,2375`):
   - Added `KEY_MAIL_SCREEN_VISIBLE` constant
   - Added `setMailScreenVisible(Boolean)` and `isMailScreenVisible()` methods
   - Default state: Mail Screen is hidden (`false`)

2. **ContactsViewModel** (`ContactsViewModel.kt:155-156,1346-1358`):
   - Added `mailScreenVisible` StateFlow for reactive UI updates
   - Added `updateMailScreenVisibility(Boolean)` method with structured logging
   - Integrated with encrypted preferences storage

3. **Setup Screen UI** (`MainActivity.kt:1724,1796-1821`):
   - Added Mail Screen visibility toggle in App Settings section
   - Toggle includes descriptive text and Material 3 Switch component
   - Reactive UI updates via StateFlow observation

4. **Navigation Logic** (`MainActivity.kt:769-787`):
   - Dynamic navigation items list based on Mail Screen visibility
   - Automatic navigation away from Mail Screen when it becomes hidden
   - LaunchedEffect handles navigation state consistency

**Usage:**
- Mail Screen is hidden by default in bottom navigation
- Users can enable it via Setup → App Settings → "Mail-Tab anzeigen" toggle
- Changes persist across app restarts via encrypted SharedPreferences
- If user is on Mail Screen when it's disabled, they're automatically navigated to Start Screen

**Technical Details:**
- Uses Jetpack Compose StateFlow for reactive UI
- Integrates with existing encrypted preferences system
- Maintains navigation consistency with LaunchedEffect
- Structured logging for debugging and monitoring

### Configurable MMI Codes Implementation (2025)

The application now allows customization of MMI codes for call forwarding, replacing hardcoded values:

**Key Components Modified:**

1. **SharedPreferencesManager** (`ContactsViewModel.kt:2420-2442,2465-2470`):
   - Added `KEY_MMI_ACTIVATE_PREFIX`, `KEY_MMI_ACTIVATE_SUFFIX`, and `KEY_MMI_DEACTIVATE_CODE` constants
   - Added defaults: `DEFAULT_MMI_ACTIVATE_PREFIX = "*21*"`, `DEFAULT_MMI_ACTIVATE_SUFFIX = "#"`, `DEFAULT_MMI_DEACTIVATE_CODE = "##21#"`
   - New methods: `getMmiActivatePrefix()`, `setMmiActivatePrefix()`, `getMmiActivateSuffix()`, `setMmiActivateSuffix()`, `getMmiDeactivateCode()`, `setMmiDeactivateCode()`, `resetMmiCodesToDefault()`

2. **ContactsViewModel** (`ContactsViewModel.kt:158-166,1371-1431`):
   - Added `mmiActivatePrefix`, `mmiActivateSuffix`, and `mmiDeactivateCode` StateFlows for reactive UI
   - Added `updateMmiActivatePrefix()`, `updateMmiActivateSuffix()`, `updateMmiDeactivateCode()`, and `resetMmiCodesToDefault()` methods
   - Integrated structured logging for all MMI code changes

3. **Setup Screen UI** (`MainActivity.kt:1857-1920`):
   - Added dedicated MMI Code Settings section with user-friendly labels
   - Three OutlinedTextField components: activation prefix, activation suffix, and deactivation code
   - Reset button with refresh icon to restore default values
   - Real-time validation and focus management for all three fields

4. **Call Forwarding Logic** (`ContactsViewModel.kt:348,396`):
   - Replaced hardcoded `"*21*"` with dynamic `prefsManager.getMmiActivatePrefix()`
   - Replaced hardcoded `"#"` with dynamic `prefsManager.getMmiActivateSuffix()`
   - Replaced hardcoded `"##21#"` with dynamic `prefsManager.getMmiDeactivateCode()`
   - Dynamic code construction: `"${prefix}${phoneNumber}${suffix}"`

5. **Logging Enhancement** (`PhoneSmsUtils.kt:524-534`):
   - Added `getUssdCodeType()` helper function for dynamic code type detection
   - Uses current preferences instead of hardcoded string matching
   - Better logging categorization for custom MMI codes

**Usage:**
- Default MMI codes: Activate Prefix `*21*`, Activate Suffix `#`, Deactivate `##21#`
- Configurable via Setup → MMI-Codes (Anrufweiterleitung) section with three separate fields
- Full activation code structure: `{prefix}{phoneNumber}{suffix}` (e.g., "*21*+4367612345#")
- Changes persist across app restarts via encrypted SharedPreferences
- Reset button restores all factory defaults
- Real-time UI updates via StateFlow observation

**Technical Integration:**
- Uses existing encrypted preferences infrastructure
- Follows established reactive UI patterns
- Maintains backward compatibility with default codes
- Comprehensive logging for troubleshooting
- Dynamic USSD code type detection for improved telemetry

### Improved Request vs Confirmation Messaging (2025)

The application messaging has been updated to accurately reflect the asynchronous nature of USSD/MMI requests:

**Key Changes:**

1. **User Messaging** (`ContactsViewModel.kt:377,427,1223,1227`):
   - Changed from "Weiterleitung aktiviert" → "Aktivierung der Weiterleitung wurde angefordert"
   - Changed from "Weiterleitung deaktiviert" → "Deaktivierung der Weiterleitung wurde angefordert"
   - Switch operations now show "Umschaltung der Weiterleitung wurde angefordert"

2. **Service Notifications** (`ContactsViewModel.kt:364,414`):
   - Activation: "Weiterleitung angefordert zu {contact} ({number})"
   - Deactivation: "Weiterleitung-Deaktivierung angefordert"

3. **Logging Actions** (`ContactsViewModel.kt:368,418,1214,2150,2167`):
   - REQUEST_ACTIVATE_FORWARDING, REQUEST_DEACTIVATE_FORWARDING, REQUEST_SWITCH_CONTACT
   - STORE_ACTIVATE_FORWARDING, STORE_DEACTIVATE_FORWARDING for preference storage
   - Messages clarify "angefordert" vs "gespeichert" distinction

**Rationale:**
- MMI/USSD codes are requests to the mobile provider, not immediate confirmations
- Actual activation/deactivation depends on provider response (not captured by app)
- User expectations now align with technical reality
- Clearer distinction between app state changes and provider requests

4. **USSD Transmission Feedback** (`PhoneSmsUtils.kt:491`):
   - Changed from generic "USSD-Anfrage wurde gesendet"
   - Now shows actual code: "Folgende MMI-Anfrage wurde gesendet: {actual_code}"
   - Examples: "*21*+4367612345#" or "##21#"
   - Provides transparency about exactly what was sent to the provider

### UI/UX Improvements Implementation (2025)

The application has undergone several UI refinements and feature enhancements to improve user experience:

**Key Components Modified:**

1. **Internal Email Server Button Removal** (`MainActivity.kt:2090-2134`):
   - Removed "Intern" button from email settings section that configured hardcoded SMTP credentials
   - Changed button layout from `Arrangement.SpaceBetween` to `Arrangement.SpaceEvenly` for proper spacing
   - Now only Gmail and GMX template buttons remain for user configuration
   - Eliminates security concern of hardcoded credentials in UI

2. **BMI/A1 MMI Codes Support** (`MainActivity.kt:1992-2017`):
   - Added "BMI/A1-Codes" button in MMI code settings section alongside existing reset button
   - Button sets specific MMI codes for BMI/A1 networks: Prefix "*21*", Suffix "**", Deactivation "**21**"
   - Reorganized button layout from single centered to Row with SpaceEvenly arrangement
   - Shortened reset button text from "Standardwerte wiederherstellen" to "Standard" for better fit
   - Uses existing ViewModel methods: `updateMmiActivatePrefix()`, `updateMmiActivateSuffix()`, `updateMmiDeactivateCode()`

3. **Obsolete Checkbox Removal** (`MainActivity.kt:1099-1129`):
   - Removed redundant SMS/phone forwarding checkbox from start screen top section
   - Eliminated entire Surface component containing checkbox and associated spacing
   - Retained ForwardingStatus component lower in UI for cleaner, non-redundant status display
   - Function signature preserved to maintain compatibility with existing calls

4. **Enhanced ForwardingStatus Component** (`MainActivity.kt:1254-1304`):
   - **Extended Parameters**: Added `forwardSmsToEmail: Boolean` and `emailAddresses: List<String>`
   - **Conditional Email Display**: Shows email forwarding status only when active (`forwardSmsToEmail && emailAddresses.isNotEmpty()`)
   - **No Inactive Email Mention**: Completely omits email forwarding information when deactivated
   - **Multi-Line Layout**: Changed from single Text to Column layout supporting both SMS and email status
   - **Dynamic Color Logic**: Surface color based on any forwarding activity (green if SMS OR email active, red if none)
   - **Status Messages**:
     - SMS: "SMS-Weiterleitung aktiv zu [phone_number]" (only when active)
     - Email: "Email-Weiterleitung aktiv an X Adresse(n)" (only when active)
     - Inactive: "Weiterleitung inaktiv" (only when no forwarding active)
   - **Complete Integration**: Updated all calling functions (LandscapeLayout, PortraitLayout) and added email StateFlows to HomeScreen

**Usage Examples:**
- Only SMS active: Shows SMS info only (green)
- Only Email active: Shows email info only (green)
- Both active: Shows both info lines (green)
- None active: Shows "Weiterleitung inaktiv" (red)
- Email disabled/no addresses: No email mention

**Technical Details:**
- Maintains existing function signatures where possible to prevent breaking changes
- Uses existing ViewModel StateFlows and update methods
- Follows Material 3 design patterns for consistent UI
- Reactive UI updates via StateFlow observation
- Structured logging integration maintained

## Critical Issues Status (Analysis Date: 2025-09-17, Resolution Date: 2025-09-17)

### ✅ ALL CRITICAL FATAL ERRORS RESOLVED

**Risk Assessment: RESOLVED** - All 7 fatal errors that could cause immediate app crashes have been systematically fixed and tested.

#### 1. **✅ RESOLVED: Duplicate Permission Check Logic Error**
**Location**: `PhoneSmsUtils.kt:273-290,408`
**Issue**: Double permission check for SEND_SMS instead of proper CALL_PHONE permission for USSD codes
**Impact**: SecurityException leading to app crash when sending USSD codes
**Resolution**: Fixed permission logic to use correct CALL_PHONE permission for USSD operations and removed duplicate checks

#### 2. **✅ RESOLVED: Lateinit Property Access Without Initialization Guards**
**Location**: `SmsForwarderApplication.kt` - `AppContainer` object
**Issue**: Multiple lateinit properties can be accessed before initialization
**Impact**: `UninitializedPropertyAccessException` causing app crash
**Resolution**: Added comprehensive safe access methods and initialization guards for all lateinit properties

#### 3. **✅ RESOLVED: Race Condition in App Initialization**
**Location**: `MainActivity.onCreate():155-188`
**Issue**: UI content set before verifying initialization success
**Impact**: Crashes when UI tries to access uninitialized components
**Resolution**: Reordered setContent call to occur only after successful initialization and added retry mechanism with proper error handling

#### 4. **✅ RESOLVED: Missing Null Checks in System Service Access**
**Location**: `PhoneSmsUtils.kt:106-160`, `SmsReceiver.kt`, multiple files
**Issue**: System services and network capabilities accessed without null checks
**Impact**: NullPointerException during network operations
**Resolution**: Added comprehensive null safety checks for all system service operations including ConnectivityManager, TelephonyManager, SmsManager, NotificationManager, and PowerManager

#### 5. **✅ RESOLVED: ContentObserver Lifecycle Management**
**Location**: `ContactsViewModel.kt:1586-1599`
**Issue**: ContentObserver uses main looper handler without proper activity lifecycle binding
**Impact**: Memory leak and potential crash when observer outlives activity
**Resolution**: Fixed handler lifecycle management and cleanup ordering to properly bind observer lifecycle to activity/fragment lifecycle

#### 6. **✅ RESOLVED: Unsafe Type Casting Throughout Codebase**
**Location**: Multiple files with `as` operator
**Issue**: Direct casting without null checks or safe casting
**Impact**: ClassCastException leading to app crash
**Resolution**: Replaced all unsafe `as` casts with safe `as?` casting and proper null handling throughout PhoneSmsUtils.kt, SmsReceiver.kt, and other files

#### 7. **✅ RESOLVED: Missing Exception Handling in Coroutines**
**Location**: `ContactsViewModel.kt` - all viewModelScope.launch operations
**Issue**: Coroutine operations lack comprehensive exception handling
**Impact**: Unhandled exceptions crashing the app
**Resolution**: Added comprehensive try-catch blocks to ALL viewModelScope.launch operations including addEmailAddress(), removeEmailAddress(), setTestContacts(), init block, updateFilterText(), contacts collection, state restoration, onCleared(), reloadLogs(), loadOwnPhoneNumber(), saveCurrentState(), updateServiceNotification(), and toggleContactSelection()

### 🔶 HIGH-PRIORITY THREADING & RESOURCE ISSUES

#### 8. **Threading Issues**
- UI updates potentially happening on background threads without proper dispatching
- ContentObserver changes not properly synchronized with main thread operations

#### 9. **Resource Leaks**
- Coroutine scopes not properly cancelled in cleanup scenarios
- File handles and network connections potentially not closed properly
- Background observers and listeners may outlive their intended lifecycle

### ✅ STABLE AREAS (No Critical Issues)

- **Permission Handling Framework**: Generally well-structured with proper permission checks
- **Logging System**: Comprehensive error logging throughout the application
- **State Management**: Proper use of StateFlow and MutableStateFlow for reactive UI
- **Error Recovery**: Good error handling in most SMS sending operations
- **Service Architecture**: Well-designed foreground service with restart mechanisms

### 🛠️ RECOMMENDED FIX PRIORITY ORDER

1. **Fix permission logic in USSD operations** (highest crash risk)
2. **Add initialization guards for lateinit properties** (startup crash prevention)
3. **Fix MainActivity initialization race condition** (UI stability)
4. **Add null safety for system services** (runtime stability)
5. **Fix ContentObserver lifecycle** (memory leak prevention)
6. **Replace unsafe type casts** (runtime crash prevention)
7. **Add comprehensive exception handling** (background operation stability)

### 📋 IMPLEMENTATION STRATEGY

Each fix should be:
- Implemented as a separate git commit for easy rollback
- Manually tested on device before proceeding to next fix
- Verified through multiple app startup/shutdown cycles
- Tested under low memory conditions
- Validated with permission grant/deny scenarios

**Status**: ✅ ALL CRITICAL FATAL ERRORS RESOLVED (2025-09-17). Application is now significantly more stable and production-ready with comprehensive crash prevention measures implemented.