# 📊 SMS Forwarder Neo A1 - Projektstruktur

> **Stand**: 2025-11-18 | **Nach Phase 5 Refactoring**

## 📈 Gesamtstatistik

| Metrik | Wert |
|--------|------|
| **Gesamt Zeilen** | 11,914 |
| **Kotlin Dateien** | 48 |
| **Packages** | 15 |
| **ViewModels** | 4 (1 Haupt + 3 Feature) |

---

## 🏗️ Architektur-Übersicht

```
info.meuse24.smsforwarderneoA1/
├── 📱 presentation/          # UI Layer (4,873 Zeilen)
│   ├── viewmodel/            # ViewModels (614 Zeilen)
│   ├── ui/                   # Compose UI Components
│   │   ├── screens/          # Screen Composables
│   │   └── components/       # Reusable Components
│   └── state/                # UI State Models
├── 💾 data/                  # Data Layer (1,135 Zeilen)
│   ├── local/                # Local Data Sources
│   └── repository/           # Repositories
├── 🎯 domain/                # Domain Layer (79 Zeilen)
│   └── model/                # Domain Models
├── 🔧 util/                  # Utilities (250 Zeilen)
│   ├── email/
│   ├── phone/
│   ├── sms/
│   └── permission/
├── 📡 service/               # Services (1,009 Zeilen)
└── 🎨 ui/theme/              # Theme (114 Zeilen)
```

---

## 📂 Hauptkomponenten

### Core Application Files (5,654 Zeilen)

| Datei | Zeilen | Beschreibung |
|-------|--------|--------------|
| `ContactsViewModel.kt` | 1,956 | Haupt-ViewModel für Kontakte & Weiterleitung |
| `PhoneSmsUtils.kt` | 1,360 | SMS/Phone Utility Funktionen |
| `SmsReceiver.kt` | 1,009 | BroadcastReceiver für SMS-Empfang |
| `MainActivity.kt` | 840 | Hauptaktivität (Compose UI) |
| `SmsForwarderApplication.kt` | 489 | App-Initialisierung & DI Container |

**Reduktion durch Refactoring**:
- ✅ ContactsViewModel: **2,341 → 1,956** (-385 Zeilen, -16.5%)
- ✅ MainActivity: **3,870 → 840** (-3,030 Zeilen, -79%)

---

## 🎯 ViewModels (614 Zeilen)

> **Phase 5**: Low-Risk ViewModel Extraction abgeschlossen

| ViewModel | Zeilen | Verantwortung | Status |
|-----------|--------|---------------|--------|
| `EmailViewModel.kt` | 376 | Email-Management & SMTP | ✅ Phase 5.2 |
| `LogViewModel.kt` | 133 | Logging & Log-Filtering | ✅ Phase 5.1 |
| `SimManagementViewModel.kt` | 105 | SIM-Nummern Verwaltung | ✅ Phase 5.3 |

**Benefits**:
- Zero Coupling zwischen ViewModels
- Independently testable
- Manual Factory Pattern (kein Hilt)

---

## 💾 Data Layer (1,135 Zeilen)

### Local Data Sources

| Datei | Zeilen | Beschreibung |
|-------|--------|--------------|
| `data/local/Logger.kt` | 544 | Strukturiertes Logging-System (XML/CSV/HTML) |
| `data/local/SharedPreferencesManager.kt` | 505 | Encrypted SharedPreferences |
| `data/local/PermissionHandler.kt` | 86 | Runtime Permission Management |

**Features**:
- Encrypted storage via `androidx.security.crypto`
- Structured logging with metadata
- Type-safe preference access

---

## 🎨 UI Layer (3,929 Zeilen)

### 📱 Screens (2,985 Zeilen)

#### Home Screen (705 Zeilen)
```
screens/home/
├── HomeScreen.kt                 201 lines  # Main screen composable
├── FilterAndLogo.kt              164 lines  # Search filter & logo
├── ForwardingStatus.kt            97 lines  # Status indicator
├── ControlButtons.kt              92 lines  # Action buttons
├── ContactList.kt                 89 lines  # Contact list & items
└── CallStatusCard.kt              62 lines  # Call state display
```

#### Settings Screen (1,146 Zeilen)
```
screens/settings/
├── SettingsScreen.kt             130 lines  # Main settings container
├── SimManagementSection.kt       354 lines  # SIM card management
├── AppSettingsSection.kt         167 lines  # App configuration
├── EmailSettingsSection.kt       157 lines  # SMTP settings
├── MmiCodeSettingsSection.kt     138 lines  # MMI codes config
├── PhoneSettingsSection.kt       100 lines  # Phone settings
└── LogSettingsSection.kt         100 lines  # Log settings
```

#### Mail Screen (219 Zeilen)
```
screens/mail/
└── MailScreen.kt                 219 lines  # Email management UI
```

#### Log Screen (520 Zeilen)
```
screens/logs/
├── LogTable.kt                   264 lines  # Log entry table
├── LogButtons.kt                 148 lines  # Refresh/Filter/Share buttons
└── LogScreen.kt                  108 lines  # Main log screen
```

#### Info Screen (395 Zeilen)
```
screens/info/
└── InfoScreen.kt                 395 lines  # About/Info screen
```

### 🔲 Components (944 Zeilen)

#### Dialogs (817 Zeilen)
```
components/dialogs/
├── LoadingScreen.kt              211 lines  # Loading state
├── PinDialogs.kt                 193 lines  # PIN & Change PIN
├── SimNumbersDialog.kt           168 lines  # SIM number input
├── CleanupDialogs.kt             125 lines  # Cleanup progress/error
└── ExitDialog.kt                 120 lines  # Exit confirmation
```

#### Navigation (127 Zeilen)
```
components/navigation/
├── BottomNavigationBar.kt        101 lines  # Bottom nav
└── CustomTopAppBar.kt             26 lines  # Top app bar
```

---

## 🎯 Domain Layer (79 Zeilen)

### Domain Models

| Model | Zeilen | Beschreibung |
|-------|--------|--------------|
| `domain/model/LogEntry.kt` | 37 | Log entry mit Timestamp & Formatierung |
| `domain/model/Contact.kt` | 27 | Kontakt mit normalisierter Nummer |
| `domain/model/SimInfo.kt` | 15 | SIM-Karten Informationen |

**Features**:
- Framework-unabhängig
- Business logic
- Value Objects

---

## 🔧 Utilities (250 Zeilen)

### Utility Packages

| Package | Datei | Zeilen | Beschreibung |
|---------|-------|--------|--------------|
| `util/email/` | EmailSender.kt | 91 | SMTP Email Versand (JavaMail) |
| `util/phone/` | CarrierTrie.kt | 64 | Carrier Prefix Lookup (Trie) |
| `util/sms/` | Gsm7BitEncoder.kt | 60 | GSM 7-bit Encoding |
| `util/permission/` | PermissionHelper.kt | 35 | Permission Utilities |

---

## 🎨 Theme (114 Zeilen)

```
ui/theme/
├── Color.kt                       45 lines  # Material 3 colors
├── Theme.kt                       41 lines  # Theme configuration
└── Type.kt                        28 lines  # Typography
```

---

## 📊 Refactoring-Historie

### Phase 4: UI Decomposition (2025-11-17)
**Ziel**: MainActivity in modulare Screens aufteilen

| Metrik | Vorher | Nachher | Reduktion |
|--------|--------|---------|-----------|
| MainActivity | 3,870 Zeilen | 840 Zeilen | **-79%** |
| Neue Dateien | 1 | 25 | +24 |
| Komponenten | Monolith | Modular | ✅ |

**Ergebnis**:
- 24 neue UI-Komponenten
- Screen-basierte Organisation
- Bessere Wartbarkeit

### Phase 5: ViewModel Decomposition (2025-11-18)
**Ziel**: Low-Risk ViewModels aus ContactsViewModel extrahieren

| Metrik | Vorher | Nachher | Reduktion |
|--------|--------|---------|-----------|
| ContactsViewModel | 2,341 Zeilen | 1,956 Zeilen | **-16.5%** |
| ViewModels | 1 | 4 | +3 |
| Test-Coverage | Schwer | Einfach | ✅ |

**Extrahierte ViewModels**:
1. **LogViewModel** (133 Zeilen) - Logging-Logik
2. **EmailViewModel** (376 Zeilen) - Email-Management
3. **SimManagementViewModel** (105 Zeilen) - SIM-Verwaltung

**Ergebnis**:
- 618 Zeilen gut organisierter Code
- Zero Coupling zwischen ViewModels
- Manual Factory Pattern erfolgreich

---

## 🎯 Code-Qualität Metriken

### Separation of Concerns

| Layer | Zeilen | % vom Gesamt | Status |
|-------|--------|--------------|--------|
| Presentation | 4,873 | 40.9% | ✅ Modular |
| Data | 1,135 | 9.5% | ✅ Isoliert |
| Domain | 79 | 0.7% | ✅ Pure |
| Utilities | 250 | 2.1% | ✅ Reusable |
| Services | 1,009 | 8.5% | ✅ Standalone |
| Core | 4,454 | 37.4% | 🔄 Verbesserbar |

### Durchschnittliche Dateigröße

| Kategorie | Ø Zeilen | Ziel | Status |
|-----------|----------|------|--------|
| ViewModels | 153 | < 400 | ✅ Gut |
| Screens | 149 | < 500 | ✅ Gut |
| Components | 118 | < 300 | ✅ Gut |
| Dialogs | 163 | < 400 | ✅ Gut |
| Utilities | 62 | < 200 | ✅ Exzellent |

---

## 🚀 Nächste Schritte (Empfehlungen)

### Phase 6: Testing Infrastructure (Geplant)
- [ ] Unit Tests für ViewModels
- [ ] Integration Tests für Data Layer
- [ ] UI Tests für kritische Flows

### Phase 7: Medium-Risk Refactoring (Optional)
- [ ] SettingsViewModel extrahieren (~350 Zeilen)
- [ ] ForwardingViewModel extrahieren (~400 Zeilen)
- [ ] ContactsViewModel weiter reduzieren

### Phase 8: Performance Optimierung (Optional)
- [ ] Coroutine-Optimierung
- [ ] Memory Leak Checks
- [ ] Profiling & Benchmarks

---

## 📚 Technologie-Stack

### Framework & Libraries
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Clean Architecture
- **DI**: Manual Factory Pattern (kein Hilt)
- **Async**: Kotlin Coroutines + Flow
- **Storage**: Encrypted SharedPreferences
- **Logging**: Custom XML/CSV Logger
- **Email**: JavaMail API
- **Phone**: libphonenumber (Google)

### Build System
- **Build Tool**: Gradle (Kotlin DSL)
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 29 (Android 10)
- **Language**: Kotlin 1.9.0
- **JDK**: Java 17

---

## 📝 Notizen

### Design Decisions
- **Manual Factory Pattern**: Bewusste Entscheidung gegen Hilt wegen Komplexität
- **Bottom-Up Refactoring**: Low-Risk zuerst, High-Risk später
- **Incremental Approach**: Ein ViewModel pro Commit für easy rollback
- **Zero New Dependencies**: Keine zusätzlichen Libraries hinzugefügt

### Lessons Learned
- ✅ Manual Factory Pattern funktioniert gut für kleine/mittlere Projekte
- ✅ Incremental refactoring verhindert große Breaking Changes
- ✅ Git Commits pro Feature ermöglichen easy rollback
- ✅ Bottom-up approach reduziert Risiko

---

**Generiert**: 2025-11-18 | **Tool**: Claude Code
