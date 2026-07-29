# SMS Forwarder Neo

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-10%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-blue.svg)](https://kotlinlang.org)

Eine zuverlässige Android-Anwendung zum automatischen Weiterleiten von eingehenden SMS-Nachrichten per SMS und E-Mail. Die App läuft als Vordergrund-Dienst für zuverlässige Hintergrundverarbeitung.

**Open Source** unter der MIT-Lizenz - vollständig transparent, keine versteckten Funktionen, keine Datensammlung.

## Features

### Kernfunktionen
- **Automatische SMS-Weiterleitung**: Leitet eingehende SMS automatisch an vordefinierte Kontakte weiter
- **E-Mail-Weiterleitung**: Sendet SMS-Inhalte zusätzlich per E-Mail
- **Rufumleitung (Anrufe)**: Schaltet die Rufumleitung des Netzes per MMI-/USSD-Code auf den gewählten Kontakt — aktivieren, deaktivieren und Status abfragen direkt aus der App. Standard sind die GSM-Codes (`**21*Nummer#`, `##21#`, `*#21#`), die als USSD-Anfrage laufen und eine auswertbare Netzantwort liefern. Für den **Sonderfall A1** gibt es ein eigenes Profil (`*21*Nummer**`, `**21**`, `*021**`), das als Sprach-MMI gewählt wird; dessen Netzansage ist hörbar, aber nicht maschinell prüfbar. Eigene Codes sind ebenfalls konfigurierbar
- **Multi-Teil SMS-Unterstützung**: Rekonstruiert automatisch mehrteilige SMS-Nachrichten
- **Parallele Weiterleitung**: Gleichzeitige Verarbeitung von SMS- und E-Mail-Weiterleitung
- **Zuverlässiger Vordergrund-Dienst**: Nutzt Foreground Service mit WakeLock für stabile Hintergrundverarbeitung

### Erweiterte Funktionen
- **Kontaktverwaltung**: Intelligente Auswahl von Weiterleitungsempfängern aus Kontakten
- **SIM-Karten-Verwaltung**: Unterstützung für Dual-SIM-Geräte
- **Loop Protection**: Verhindert Endlosschleifen durch intelligente Erkennung eigener SIM-Karten
- **Strukturiertes Logging**: XML-basierte Protokollierung mit automatischer Rotation
- **Log-Export**: Exportiere Protokolle für Fehleranalyse und Monitoring
- **Verschlüsselte Einstellungen**: Sichere Speicherung von Credentials und Konfiguration
- **SMTP-Unterstützung**: STARTTLS (587) oder SSL/TLS (465), TLS 1.2+ mit Prüfung der Serveridentität; Absenderadresse getrennt vom Benutzernamen für Anbieter, deren Login keine E-Mail-Adresse ist
- **Test-Utilities**: Integrierte Tools zum Testen der SMS-Funktionalität
- **Persistente Sendewarteschlangen**: Beide Kanäle haben eigene, verschlüsselte Warteschlangen. Jede Weiterleitung hat einen gespeicherten Zustand; Fehlschläge werden wiederholt, ungeklärte Fälle sichtbar gemacht. Sie überstehen Prozessende und Geräteneustart
- **MMI-Profile und -Audit**: Codeprofil je SIM wählbar (Standard-GSM als Default, A1-Sonderprofil nur bei ausdrücklicher Vertrags-/Supportvorgabe, sonst eigene Codes); jeder Schaltvorgang landet maskiert im lokalen Audit
- **RCS-Hinweis**: Erklärt auf der Startseite und in der Hilfe, warum RCS-Chats nicht weitergeleitet werden können, und wie man den SMS-Fallback herstellt

### Bekannte Grenzen

Die App leitet **SMS** weiter. Nicht weitergeleitet werden:

- **RCS-Chats** (Google Messages): Android liefert RCS-Nachrichten grundsätzlich nicht an Dritt-Apps aus – unabhängig von Berechtigungen. Wer alle Textnachrichten weiterleiten möchte, deaktiviert RCS auf dem weiterleitenden Gerät; die Kontakte senden dann automatisch wieder SMS. Die App weist darauf hin und erklärt die Schritte in der Hilfe.
- **MMS** (Bilder, Videos, Sprachnachrichten): Die App empfängt keinen `WAP_PUSH_RECEIVED`-Broadcast.

Der RCS-Status selbst ist für eine normale App nicht auslesbar; die App behauptet ihn deshalb nirgends. Hintergrund, geprüfte Alternativen und die Begründung gegen einen `NotificationListenerService` stehen in [`rcs.md`](rcs.md).

### Systemgrenzen des Dauerbetriebs

Der Dienst läuft als Foreground Service vom Typ `specialUse`. Für diesen Typ ist kein Zeitlimit
dokumentiert – eine Zusicherung unbegrenzter Laufzeit ist das ausdrücklich nicht:

- **„Stopp erzwingen"** (App-Info) und ein **Stopp über den Task Manager** setzen die Weiterleitung
  bis zum nächsten manuellen Start der App aus. Das ist eine Systemgrenze, kein Fehler; kein
  Wächter innerhalb der App kann das umgehen, denn er wäre selbst mit beendet.
- Nach einem Prozesskill unter Speicherdruck greifen `START_STICKY`, der `BootReceiver` nach einem
  Geräteneustart und **jeder eingehende SMS-Broadcast**, der einen Neustartversuch auslöst. Ob
  dieser Versuch aus dem Hintergrund gelingt, sichert die Plattform nicht allgemein zu – die App
  behauptet deshalb keine Selbstheilung.
- Aggressive Energieverwaltung mancher Hersteller kann den Dienst beenden. Die App weist auf eine
  aktive Batterieoptimierung hin.

Zustände, die die Weiterleitung beeinträchtigen, stoppen sie **nicht**, sondern erscheinen beim
nächsten Öffnen als Hinweis auf der Startseite: unterdrückte Statusanzeige (fehlende
Benachrichtigungsberechtigung), verlorene Warteschlangeneinträge, ein Zeitlimit des Systems sowie
fehlgeschlagene oder ungeklärte Weiterleitungen.

### Zustellsemantik

Weitergeleitete SMS haben einen persistierten Zustand. Wiederholt wird **nur, wo ein Fehlschlag
belegt ist** (negative Rückmeldung des Netzes: kein Dienst, Funk aus, allgemeiner Sendefehler) –
maximal drei Wiederholungen mit 30 s / 2 min / 10 min Abstand. Wo jede Aussage fehlt – etwa wenn der
Prozess im Sendefenster stirbt oder eine Rückmeldung 15 Minuten ausbleibt –, wird **nicht** erneut
gesendet; der Vorgang wird als ungeklärt angezeigt. Diese Wahl bevorzugt den selteneren Verlust
gegenüber dem häufigeren Doppelversand. Bei mehrteiligen Nachrichten mit Teilerfolg wird der ganze
Vorgang neu versandt; bereits zugestellte Teile können dann doppelt ankommen – eine unvollständige
mehrteilige SMS wird vom Empfängergerät sonst nie zusammengesetzt. `Gesendet` heißt „vom Netz
angenommen", nicht „beim Empfänger angekommen".

Der **E-Mail-Kanal entscheidet bewusst umgekehrt: im Zweifel wird erneut gesendet.** Eine E-Mail
kostet nichts, der Verlust einer Weiterleitung ist der eigentliche Schadensfall. Auch hier hat jeder
Auftrag einen persistierten Zustand, hier aber mit dem Zustellstand **je Empfänger**:

- Wiederholt wird **nach der Ursache**, nicht pauschal: nur bei Netz- und Serverproblemen sowie
  vorübergehenden Serverantworten (SMTP 4xx). Ein abgelehntes Passwort, eine unzustellbare Adresse
  oder ein gescheiterter TLS-Aufbau ergeben **genau einen** Versuch und erscheinen als Hinweis.
- Wiederholt wird bis zu 24 Stunden lang (nach 1, 5, 15 und 60 Minuten, danach alle 3 Stunden).
  Ohne Netz wird der Versuch aufgeschoben, ohne das Wiederholungsbudget zu verbrauchen.
- Jeder Empfänger erhält eine eigene Nachricht; ein Neuversuch adressiert nur die noch offenen.
  Wird einer dauerhaft abgelehnt, bekommen die übrigen ihre Nachricht trotzdem — und kein zweites Mal.
- Endet der Prozess genau im Sendefenster, kann eine E-Mail doppelt ankommen. Eine stabile
  `Message-ID` je Auftrag sorgt dafür, dass viele Server und Mailprogramme die Wiederholung als
  solche erkennen; eine Zusicherung ist das nicht.

Hintergrund und die abweichend entschiedenen Punkte stehen in [`sms.md`](sms.md) und
[`smtp.md`](smtp.md).

## Technologie-Stack

### Plattform & Sprache
- **Kotlin**: 2.1.0
- **Target SDK**: 36 (Android 16)
- **Min SDK**: 29 (Android 10+)
- **Compile SDK**: 36
- **JDK**: 17

### UI Framework
- **Jetpack Compose**: Moderne deklarative UI
- **Compose BOM**: 2024.11.00
- **Material Design 3**: Aktuelle Design-Standards
- **Navigation Compose**: 2.8.5

### Architektur & Bibliotheken
- **Clean Architecture**: MVVM mit Repository Pattern
- **ViewModels**: Factory-Pattern für Dependency Injection
- **Coroutines & Flow**: Asynchrone Programmierung und reaktive Datenverwaltung
- **Security Crypto**: Verschlüsselte SharedPreferences (AES256-GCM)
- **libphonenumber**: 8.13.52 - Telefonnummer-Validierung
- **JavaMail**: 1.6.7 - E-Mail-Versand (SMTP)
- **Timber**: 5.0.1 - Strukturiertes Logging
- **Coil**: 2.7.0 - Bildverarbeitung

## Voraussetzungen

### Entwicklungsumgebung
- **Android Studio**: Ladybug (2024.2.1) oder neuer
- **JDK**: Version 17
- **Gradle**: 8.7+
- **Android Gradle Plugin**: 8.9.1
- **Git**: Für Versionskontrolle

### Android-Gerät/Emulator
- **Android 10 (API 29)** oder höher
- **Telefonie-Feature**: Empfohlen für volle Funktionalität
- **Dual-SIM**: Optional, aber unterstützt

## Installation der signierten App

Die App wird **nicht über Google Play** vertrieben, sondern als signierte APK unter [Releases](https://github.com/meuse24/forwarderA1/releases). Der vollständige Quellcode dieses Repositorys entspricht der veröffentlichten App.

### 1. APK herunterladen

Laden Sie `app-release.apk` aus dem aktuellen Release herunter.

### 2. Echtheit prüfen (empfohlen)

Ohne Google Play bürgt kein Store für die Herkunft der Datei. Prüfen Sie sie deshalb selbst — besonders wichtig, weil die App Zugriff auf Ihre SMS hat.

**a) Prüfsumme der Datei** – schützt vor einem manipulierten oder unvollständigen Download. Der erwartete Wert steht in der jeweiligen Release-Beschreibung.

```bash
# Windows (PowerShell oder Eingabeaufforderung)
certutil -hashfile app-release.apk SHA256

# Linux / macOS
sha256sum app-release.apk
```

**b) Signatur des Herausgebers** – belegt, dass die APK mit dem Schlüssel dieses Projekts signiert wurde. Dieser Fingerprint ist **für alle Releases identisch** und ändert sich nicht:

```
Herausgeber:  CN=Günther Meusburger
SHA-256:      DF:F4:45:88:48:D9:8F:DB:C0:05:E4:71:59:D1:50:7C:
              F2:2C:58:B4:76:00:EF:09:4A:B3:E0:8B:99:F1:C7:2C
```

Prüfen lässt er sich mit `apksigner` aus den Android SDK Build-Tools:

```bash
apksigner verify --print-certs app-release.apk
```

Stimmt der ausgegebene Wert für „Signer #1 certificate SHA-256 digest" nicht mit dem obigen überein, stammt die Datei **nicht** aus diesem Projekt — dann bitte nicht installieren.

### 3. Installieren

Android blockiert die Installation aus dem Browser oder Dateimanager zunächst. Erlauben Sie die Installation für die jeweilige App, wenn Sie danach gefragt werden (*Einstellungen → Apps → Spezieller App-Zugriff → Unbekannte Apps installieren*).

Google Play Protect kann beim Installieren eine Warnung anzeigen, weil die App nicht über Play verteilt wird und SMS-Berechtigungen anfordert. Das ist bei Sideload einer SMS-App erwartbar und kein Hinweis auf Schadsoftware — die Prüfung in Schritt 2 ist der belastbare Nachweis.

### 4. Updates

Automatische Updates gibt es außerhalb von Play nicht. Um über neue Versionen informiert zu werden, können Sie das Repository auf GitHub beobachten (*Watch → Custom → Releases*). Neue Versionen lassen sich direkt über die alte installieren; Ihre Einstellungen bleiben erhalten, solange die Signatur unverändert ist.

## Aus dem Quellcode bauen

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
   - Ohne Benachrichtigungsberechtigung läuft die Weiterleitung weiter, der Dienst ist dann nur
     im Task Manager sichtbar

5. **Logs überwachen**
   - Navigiere zu "Logs" Tab
   - Filtere nach Zeitraum oder Ereignistyp
   - Exportiere Logs für Analyse

### Wichtige Hinweise

- **Multi-Teil SMS**: Werden automatisch gruppiert und rekonstruiert
- **Unabhängige Kanäle**: SMS und E-Mail laufen parallel; ein SMTP-Ausfall bricht den SMS-Versand
  nicht ab und löst keine zusätzliche SMS aus
- **E-Mail-Wiederholung**: Eine nicht zustellbare E-Mail bleibt gespeichert und wird bis zu
  24 Stunden lang erneut versucht (1, 5, 15, 60 Minuten, dann 3 Stunden). Wiederholt wird nur bei
  Netz- und Serverproblemen — ein falsches Passwort oder eine abgelehnte Adresse führt zu genau
  einem Versuch und erscheint als Hinweis auf der Startseite
- **Ein Empfänger je Nachricht**: Bei mehreren E-Mail-Empfängern sieht keiner die Adressen der
  anderen, und eine Wiederholung beliefert nur die noch offenen
- **WakeLock**: Verhindert Sleep während der Nachrichtenverarbeitung
- **Service-Neustart**: Nutzt `START_STICKY` für automatischen Neustart

### Rufumleitung per MMI/USSD

- Wählen Sie zuerst die SIM-Karte und die dazu passende MMI-/USSD-Konfiguration.
- Standard-GSM-Codes (`**21*Nummer#`, `##21#`, `*#21#`) werden als USSD-Anfrage ausgeführt und liefern eine Netzantwort zurück.
- Nur das optionale A1-Sonderprofil wird als Sprach-MMI über Androids Telefoniefunktion gewählt. Eine Netzansage ist hörbar, aber nicht maschinell auswertbar.
- Eine Statusabfrage ändert keine Weiterleitung. Im Flugmodus wird kein Wählvorgang gestartet.
- Das lokale MMI-Audit enthält ausschließlich maskierte Informationen und wird auf 200 Einträge beziehungsweise 30 Tage begrenzt.

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
- Entkoppelte SMS-/E-Mail-Weiterleitung
- WakeLock-Management
- Persistente Sendewarteschlange samt Ablauf-Scan
- Service-Lifecycle inklusive `onTimeout()`

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

<!-- Netzstatus: prüft vor dem E-Mail-Versand, ob überhaupt eine Verbindung besteht -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Sicherheit

### Datenverschlüsselung
- Alle Einstellungen werden mit `androidx.security.crypto` verschlüsselt
- SMTP-Passwörter werden niemals im Klartext gespeichert
- Verschlüsselte SharedPreferences mit AES256-GCM
- Weiterzuleitende Nachrichten liegen bis zur Zustellung in zwei getrennten, ebenfalls
  verschlüsselten Warteschlangen (SMS-Volltext, Absendernummer, bei E-Mail zusätzlich die
  Empfängeradressen). Erledigte Einträge werden nach spätestens 7 Tagen gelöscht; beide Dateien
  sind vom Cloud-Backup und von der Geräteübertragung ausgeschlossen
- Der SMTP-Versand ist immer verschlüsselt: STARTTLS (erzwungen, kein Rückfall auf Klartext) oder
  TLS ab dem ersten Byte, jeweils TLS 1.2+ mit Prüfung der Serveridentität

### Datenschutz
- Zielnummern und Einstellungen werden nur lokal und verschlüsselt verarbeitet
- Das lokale MMI-Audit enthält höchstens 200 maskierte Einträge für maximal 30 Tage; vollständige Zielnummern und rohe Netzantworten werden darin nicht gespeichert
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

Dieses Projekt ist unter der **MIT-Lizenz** lizenziert.

```
MIT License

Copyright (c) 2026 Günther Meusburger

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Kontakt

- **Repository**: https://github.com/meuse24/forwarderA1
- **Issues**: https://github.com/meuse24/forwarderA1/issues

## Danksagungen

- **Jetpack Compose**: Für das moderne UI-Framework
- **libphonenumber**: Für robuste Telefonnummer-Validierung
- **JavaMail**: Für SMTP-Funktionalität
- **AndroidX Security**: Für Verschlüsselung

---

**Version**: Barracuda 5.1.0
**Build**: Debug/Release
**Letzte Aktualisierung**: 2026-07-29

### Neueste Änderungen (2026-07-29)
- ✅ **SMS-Weiterleitung dauerbetriebsfest**: Vordergrunddienst als `specialUse` (kein 6-Stunden-Limit), persistente Sendewarteschlange, Autostart nach Neustart. Einzelheiten in `sms.md`
- ✅ **E-Mail-Weiterleitung dauerbetriebsfest**: eigene verschlüsselte Warteschlange, Wiederholung nach Fehlerursache statt pauschal, Zustellung pro Empfänger, STARTTLS **und** SSL/TLS, getrennte Absenderadresse, Empfangszeitpunkt im Text. Einzelheiten in `smtp.md`
- ✅ **Hinweiskarte auf der Startseite**: fehlgeschlagene Weiterleitungen, verlorene Einträge, unterdrückte Statusanzeige und Batterieoptimierung — sichtbar und quittierbar
- ✅ **RCS-Hinweis**: erklärt, warum RCS-Chats aus Google Messages nicht weitergeleitet werden
- ✅ **Android 16**: `targetSdk`/`compileSdk` 36, Edge-to-Edge umgesetzt

### Änderungen 2026-01-23
- ✅ **Open Source**: Vollständige MIT-Lizenz Dokumentation
- ✅ **Info Screen**: Open Source Card mit GitHub-Links und Lizenzhinweis
- ✅ **Hilfe Screen**: Technische Details zu Loop-Schutz, Multi-SIM und Datensicherheit
- ✅ **Datenschutzerklärung**: Loop Protection Section hinzugefügt
- ✅ **Copyright**: Aktualisiert auf 2026
- ✅ **System Info**: Erweitert mit Kotlin 2.1, Compose BOM, JDK 17
- ✅ **Android 15/16**: Unterstützung in Version-Erkennung hinzugefügt
- ✅ **Lint**: abortOnError aktiviert mit Baseline
- ✅ **Baseline Profile**: Für verbesserte App-Startzeit

### Frühere Änderungen
- ✅ **Loop Protection**: Kritischer Schutzmechanismus gegen SMS-Endlosschleifen (Sender- und Own-SIM-Check)
- ✅ **Timber Logging**: Strukturiertes Logging mit JSON Lines Format
- ✅ **Dialog Consolidation**: Vereinheitlichtes Dialog-System
- ✅ **Manual SIM Number Editing**: Manuelle SIM-Nummern-Bearbeitung
