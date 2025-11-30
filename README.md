# SMS Forwarder Neo

Eine zuverlässige Android-Anwendung zum automatischen Weiterleiten von eingehenden SMS-Nachrichten per SMS und E-Mail. Die App läuft als Vordergrund-Dienst für zuverlässige Hintergrundverarbeitung.

## Features

### Kernfunktionen
- **Automatische SMS-Weiterleitung**: Leitet eingehende SMS automatisch an vordefinierte Kontakte weiter
- **E-Mail-Weiterleitung**: Sendet SMS-Inhalte zusätzlich per E-Mail
- **Multi-Teil SMS-Unterstützung**: Rekonstruiert automatisch mehrteilige SMS-Nachrichten
- **Parallele Weiterleitung**: Gleichzeitige Verarbeitung von SMS- und E-Mail-Weiterleitung
- **Zuverlässiger Vordergrund-Dienst**: Nutzt Foreground Service mit WakeLock für stabile Hintergrundverarbeitung

### Erweiterte Funktionen
- **Kontaktverwaltung**: Intelligente Auswahl von Weiterleitungsempfängern aus Kontakten
- **SIM-Karten-Verwaltung**: Unterstützung für Dual-SIM-Geräte
- **Strukturiertes Logging**: XML-basierte Protokollierung mit automatischer Rotation
- **Log-Export**: Exportiere Protokolle für Fehleranalyse und Monitoring
- **Verschlüsselte Einstellungen**: Sichere Speicherung von Credentials und Konfiguration
- **SMTP-Unterstützung**: Flexible E-Mail-Konfiguration mit verschiedenen Anbietern
- **Test-Utilities**: Integrierte Tools zum Testen der SMS-Funktionalität
- **Heartbeat-Monitoring**: Automatische Überwachung der Dienstverfügbarkeit

## Technologie-Stack

### Plattform & Sprache
- **Kotlin**: 1.9.0
- **Target SDK**: 35 (Android 14)
- **Min SDK**: 29 (Android 10+)
- **Compile SDK**: 36
- **JDK**: 17

### UI Framework
- **Jetpack Compose**: Moderne deklarative UI
- **Material Design 3**: Aktuelle Design-Standards
- **Navigation Compose**: Typ-sichere Navigation

### Architektur & Bibliotheken
- **Clean Architecture**: Klare Trennung von Data, Domain und Presentation Layer
- **ViewModels**: Factory-Pattern für Dependency Injection
- **Coroutines**: Asynchrone Programmierung
- **StateFlow**: Reaktive Datenverwaltung
- **Security Crypto**: Verschlüsselte SharedPreferences
- **libphonenumber**: Telefonnummer-Validierung
- **JavaMail**: E-Mail-Versand (SMTP)
- **WorkManager**: Hintergrundaufgaben
- **Room**: Für zukünftige Datenbankerweiterungen

## Voraussetzungen

### Entwicklungsumgebung
- **Android Studio**: Hedgehog (2023.1.1) oder neuer
- **JDK**: Version 17
- **Gradle**: 8.x
- **Git**: Für Versionskontrolle

### Android-Gerät/Emulator
- **Android 10 (API 29)** oder höher
- **Telefonie-Feature**: Empfohlen für volle Funktionalität
- **Dual-SIM**: Optional, aber unterstützt

## Installation

### 1. Repository klonen
```bash
git clone https://github.com/meuse24/forwarderA1.git
cd forwarderA1
```

### 2. Projekt öffnen
```bash
# Der main Branch ist der stabile Produktions-Branch
# Kein Branch-Wechsel erforderlich
```

### 3. Build-Umgebung vorbereiten

#### Kommandozeile
```bash
./gradlew assembleDebug
```

#### Android Studio
1. Öffne das Projekt in Android Studio
2. Sync Project with Gradle Files
3. Run 'app' oder Build > Build Bundle(s) / APK(s)

### 4. APK installieren
```bash
# Auf verbundenem Gerät installieren
./gradlew installDebug

# Oder manuell
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Build-Befehle

```bash
# Debug Build (Entwicklung)
./gradlew assembleDebug

# Release Build (Produktion)
./gradlew assembleRelease

# Installation auf Gerät
./gradlew installDebug

# Schneller Kompilierungscheck
./gradlew compileDebugKotlin

# Tests ausführen
./gradlew test

# Clean Build
./gradlew clean
```

## Verwendung

### Erste Schritte

1. **Berechtigungen erteilen**
   - SMS empfangen/senden
   - Kontakte lesen
   - Telefonstatus
   - Batterie-Optimierung ignorieren
   - Benachrichtigungen

2. **E-Mail-Konfiguration** (optional)
   - Navigiere zu "Mail" Tab
   - SMTP-Server konfigurieren
   - Absender- und Empfänger-Adressen eingeben
   - SMTP-Credentials (Passwort wird verschlüsselt gespeichert)

3. **Kontakte auswählen**
   - Wähle Kontakte für SMS-Weiterleitung
   - Aktiviere/deaktiviere einzelne Kontakte nach Bedarf

4. **Dienst starten**
   - Der Vordergrund-Dienst startet automatisch
   - Status wird in der Benachrichtigungsleiste angezeigt
   - Heartbeat-Monitoring läuft im Hintergrund

5. **Logs überwachen**
   - Navigiere zu "Logs" Tab
   - Filtere nach Zeitraum oder Ereignistyp
   - Exportiere Logs für Analyse

### Wichtige Hinweise

- **Multi-Teil SMS**: Werden automatisch gruppiert und rekonstruiert
- **Parallele Verarbeitung**: SMS und E-Mail werden gleichzeitig gesendet
- **WakeLock**: Verhindert Sleep während der Nachrichtenverarbeitung
- **Service-Neustart**: Nutzt `START_STICKY` für automatischen Neustart

## Architektur

### Package-Struktur

```
info.meuse24.smsforwarderneoA1/
├── data/                          # Data Layer
│   ├── local/                     # Lokale Datenhaltung
│   │   ├── Logger.kt             # XML-Logging mit Rotation
│   │   └── SharedPreferencesManager.kt  # Verschlüsselte Settings
│   └── repository/                # Repositories
│       └── ContactsRepositoryImpl.kt    # Kontaktverwaltung
├── domain/                        # Domain Layer
│   └── model/                     # Domain Models
│       ├── Contact.kt            # Kontakt-Entität
│       └── LogEntry.kt           # Log-Eintrag
├── presentation/                  # Presentation Layer
│   ├── ui/
│   │   ├── screens/              # Screen Composables
│   │   │   ├── home/            # Hauptbildschirm
│   │   │   ├── mail/            # E-Mail-Konfiguration
│   │   │   ├── settings/        # Einstellungen
│   │   │   ├── logs/            # Log-Anzeige
│   │   │   └── info/            # App-Informationen
│   │   └── components/           # Wiederverwendbare Komponenten
│   │       ├── dialogs/         # Dialog-Komponenten
│   │       └── navigation/      # Navigation-Komponenten
│   ├── viewmodel/                # ViewModels
│   │   ├── ContactsViewModel.kt
│   │   ├── LogViewModel.kt
│   │   ├── EmailViewModel.kt
│   │   ├── SimManagementViewModel.kt
│   │   ├── TestUtilsViewModel.kt
│   │   └── NavigationViewModel.kt
│   └── state/                    # UI State Models
├── service/                       # Service Layer
│   ├── SmsReceiver.kt            # BroadcastReceiver für SMS
│   └── SmsForegroundService.kt   # Foreground Service
└── util/                          # Utilities
    ├── email/                     # E-Mail-Utilities
    ├── permission/                # Permission-Handling
    ├── phone/                     # Telefon-Utilities
    └── sms/                       # SMS-Utilities

```

### Schlüsselkomponenten

#### MainActivity.kt (819 Zeilen)
- Activity-Kern mit Permission-Handling
- Navigation zwischen Screens
- ViewModel-Initialisierung
- Lifecycle-Management

#### ContactsViewModel.kt (1.278 Zeilen)
- Kontaktauswahl und -verwaltung
- Weiterleitungslogik
- State-Management für UI

#### PhoneSmsUtils.kt (1.380 Zeilen)
- SMS-Sende- und Empfangsfunktionen
- Telefonnummer-Validierung
- USSD-Code-Ausführung
- SIM-Karten-Management

#### SmsForegroundService.kt
- Multi-Teil SMS-Rekonstruktion
- Parallele SMS/E-Mail-Weiterleitung
- WakeLock-Management
- Heartbeat-Monitoring
- Service-Lifecycle

#### SmsReceiver.kt
- Empfängt `SMS_RECEIVED_ACTION`
- Leitet Nachrichten an Service weiter
- Broadcast-Handling

## Berechtigungen

### Kritische Berechtigungen
```xml
<!-- SMS-Funktionalität -->
<uses-permission android:name="android.permission.RECEIVE_SMS"/>
<uses-permission android:name="android.permission.SEND_SMS"/>

<!-- Kontaktzugriff -->
<uses-permission android:name="android.permission.READ_CONTACTS"/>

<!-- Telefonfunktionen -->
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_NUMBERS" />

<!-- Hintergrunddienst -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL" />

<!-- Batterie-Optimierung -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Benachrichtigungen -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Internet (für E-Mail) -->
<uses-permission android:name="android.permission.INTERNET" />
```

## Sicherheit

### Datenverschlüsselung
- Alle Einstellungen werden mit `androidx.security.crypto` verschlüsselt
- SMTP-Passwörter werden niemals im Klartext gespeichert
- Verschlüsselte SharedPreferences mit AES256-GCM

### Datenschutz
- Telefonnummern werden nur lokal verarbeitet
- Logs können exportiert und gelöscht werden
- Keine Cloud-Synchronisation oder Datenübertragung an Dritte
- App ist für privaten Gebrauch konzipiert

### Best Practices
- Sichere Intent-Filter
- FileProvider für URI-Zugriff
- Runtime-Permissions
- Foreground Service Notification

## Entwicklung

### Code-Stil
- **Kotlin Coding Conventions**: Offizielle Kotlin-Standards
- **Clean Architecture**: Strikte Layer-Trennung
- **SOLID-Prinzipien**: Single Responsibility, Open/Closed, etc.
- **Dependency Injection**: Factory-Pattern für ViewModels

### Neue Präferenz hinzufügen
```kotlin
// 1. SharedPreferencesManager
fun getMyPreference(): Boolean =
    encryptedPrefs.getBoolean("my_preference", false)

fun setMyPreference(value: Boolean) =
    encryptedPrefs.edit().putBoolean("my_preference", value).apply()

// 2. ViewModel
private val _myPreference = MutableStateFlow(false)
val myPreference: StateFlow<Boolean> = _myPreference.asStateFlow()

// 3. UI Component
val myPref by viewModel.myPreference.collectAsState()
Switch(checked = myPref, onCheckedChange = { viewModel.updatePreference(it) })
```

### SMS-Verarbeitung modifizieren
```kotlin
// SmsForegroundService.kt
private fun processMessageGroup(messages: List<SmsMessage>) {
    // 1. Multi-Teil SMS rekonstruieren
    val fullMessage = messages.joinToString("") { it.messageBody }

    // 2. Logging
    LoggingManager.log("SMS empfangen: $fullMessage")

    // 3. Weiterleitung (parallel)
    coroutineScope.launch { forwardViaSms(fullMessage) }
    coroutineScope.launch { forwardViaEmail(fullMessage) }
}
```

### Neuen Screen hinzufügen
```kotlin
// 1. Erstelle Screen in presentation/ui/screens/myscreen/
@Composable
fun MyScreen(viewModel: MyViewModel) { /* ... */ }

// 2. Route in MainActivity.kt
when (currentRoute) {
    "myscreen" -> MyScreen(myViewModel)
    // ...
}

// 3. Navigation in BottomNavigationBar
NavigationBarItem(
    icon = { Icon(Icons.Default.MyIcon, "My Screen") },
    selected = currentRoute == "myscreen",
    onClick = { navViewModel.navigate("myscreen") }
)
```

### Neues ViewModel erstellen
```kotlin
// 1. ViewModel mit Factory
class MyViewModel(
    private val prefsManager: SharedPreferencesManager
) : ViewModel() {

    // State
    private val _myState = MutableStateFlow<MyState>(MyState.Initial)
    val myState: StateFlow<MyState> = _myState.asStateFlow()

    // Factory
    class Factory(
        private val prefsManager: SharedPreferencesManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyViewModel(prefsManager) as T
        }
    }
}

// 2. In MainActivity instanziieren
val myViewModel: MyViewModel by viewModels {
    MyViewModel.Factory(prefsManager)
}
```

### Testing
```bash
# Unit Tests
./gradlew test

# Instrumented Tests
./gradlew connectedAndroidTest

# Test-Coverage
./gradlew jacocoTestReport
```

## Git-Workflow

### Branches
- **`main`**: Stabiler Produktions-Branch (empfohlen)

### Commits
```bash
# Änderungen commiten
git add .
git commit -m "feat: Neue Feature-Beschreibung"

# Push mit PAT
source .env
git push https://meuse24:$GITHUB_TOKEN@github.com/meuse24/forwarderA1.git main
```

### Commit-Konventionen
- `feat:` - Neue Features
- `fix:` - Bugfixes
- `refactor:` - Code-Umstrukturierung
- `docs:` - Dokumentation
- `test:` - Tests
- `chore:` - Build/Tooling

## Projekt-Status

### Abgeschlossene Phasen (Clean Architecture Refactoring)

✅ **Phase 1-5 komplett**:
- Package-Struktur etabliert
- Domain Models extrahiert
- Data Layer separiert (Logger, SharedPrefs, Repository)
- MainActivity: 3.870 → 819 Zeilen (-79%)
- ContactsViewModel: 2.341 → 1.278 Zeilen (-45%)
- Alle kritischen Fehler behoben (Permissions, Null-Safety, Lifecycle, Coroutines)

**Die App ist stabil und produktionsbereit.**

### Bekannte Limitierungen
- Keine Datenbank-Persistierung (nur SharedPreferences)
- Keine Cloud-Synchronisation
- E-Mail-Versand erfordert SMTP-Konfiguration
- Logs werden lokal in XML gespeichert

### Roadmap
- [ ] Migration zu Room Database
- [ ] Backup/Restore-Funktion
- [ ] Erweiterte Filteroptionen
- [ ] UI/UX-Verbesserungen
- [ ] Automatisierte Tests

## Fehlerbehebung

### Build-Probleme
```bash
# Problem: "Permission denied" beim gradlew
chmod +x gradlew

# Problem: Gradle Daemon startet nicht
./gradlew --stop
./gradlew assembleDebug --no-daemon
```

### App-Probleme
```bash
# Service startet nicht
- Prüfe Batterie-Optimierung (deaktivieren)
- Prüfe Berechtigungen in Android-Einstellungen
- Logs in App überprüfen

# SMS werden nicht weitergeleitet
- Prüfe SMS-Berechtigungen
- Prüfe ausgewählte Kontakte
- Prüfe Service-Status
- Logs exportieren und analysieren

# E-Mail-Versand fehlschlägt
- SMTP-Credentials prüfen
- Internet-Verbindung prüfen
- Firewall/VPN-Einstellungen
- SMTP-Server-Logs prüfen
```

## Lizenz

Dieses Projekt ist für den privaten Gebrauch bestimmt. Alle Rechte vorbehalten.

## Kontakt

- **Repository**: https://github.com/meuse24/forwarderA1
- **Issues**: https://github.com/meuse24/forwarderA1/issues

## Danksagungen

- **Jetpack Compose**: Für das moderne UI-Framework
- **libphonenumber**: Für robuste Telefonnummer-Validierung
- **JavaMail**: Für SMTP-Funktionalität
- **AndroidX Security**: Für Verschlüsselung

---

**Version**: Anchovy (versionCode 3)
**Build**: Debug/Release
**Letzte Aktualisierung**: 2025-01-30

### Neueste Änderungen (2025-01-30)
- ✅ Verbesserte Benutzerführung bei MMI-Code-Wählvorgängen
- ✅ 4-Sekunden-Warnung vor jedem Wählvorgang mit zentrierter Formatierung
- ✅ Deutliche Warnung: "⚠️ BITTE WARTEN ⚠️ - NICHT BEDIENEN!"
- ✅ Klare Anweisungen zum Abwarten des Wählvorgangs
- ✅ Automatische Rückkehr zum Hauptbildschirm
