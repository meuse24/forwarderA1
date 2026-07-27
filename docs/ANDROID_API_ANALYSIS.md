# Analyse: Selbstgebaute Funktionen vs. Android-System-APIs

**Datum:** 2025-12-08
**Kontext:** Bewertung nach erfolgreicher Migration von Contact Picker, System Dialer und SmsManager

---

## Executive Summary

Nach Analyse aller selbstgebauten Komponenten: **Die meisten Custom-Implementierungen sind NOTWENDIG und sollten NICHT ersetzt werden**. Es gibt aber 2-3 kleinere Optimierungsmöglichkeiten.

| Komponente | Status | Android-Alternative? | Empfehlung |
|------------|--------|---------------------|------------|
| **EmailSender** | ✅ Behalten | ❌ Keine native API | NOTWENDIG |
| **Logger (FileLoggingTree)** | ✅ Optimal | ✅ Timber + Custom Tree | MIGRIERT |
| **PhoneNumberValidator** | ✅ Behalten | ✅ libphonenumber (verwendet!) | OPTIMAL |
| **SharedPreferencesManager** | ✅ Behalten | ⚠️ DataStore (Migration möglich) | NICE-TO-HAVE |
| **CarrierTrie** | ✅ Behalten | ❌ Keine API | NOTWENDIG |
| **PermissionHelper** | ✅ Behalten | ⚠️ Activity Result API (teilweise) | AKZEPTABEL |

**Fazit:** Nur 1 sinnvolle Migration: **SharedPreferencesManager → DataStore** (niedrige Priorität)

---

## 1. EmailSender (JavaMail/SMTP)

### ❌ **KEINE Android-Alternative - Behalten!**

**Aktuell (util/email/EmailSender.kt):**
```kotlin
class EmailSender(host, port, username, password) {
    suspend fun sendEmail(to: List<String>, subject: String, body: String)
}
```

**Android-Alternativen:**
1. **Intent.ACTION_SENDTO** (Email-Client öffnen)
   - ❌ Benötigt User-Interaktion
   - ❌ Keine automatische Versendung
   - ❌ Ungeeignet für Hintergrund-Forwarding

2. **WorkManager + HTTP API** (z.B. SendGrid, Mailgun)
   - ⚠️ Benötigt externen Service
   - ⚠️ Kosten pro Email
   - ❌ Komplexer Setup

**Bewertung:**
- ✅ **JavaMail ist die richtige Lösung** für Server-to-Server SMTP
- ✅ Keine User-Interaktion nötig
- ✅ TLS/STARTTLS Security korrekt implementiert
- ✅ Funktioniert in Foreground Service

**Empfehlung:** ✅ **BEHALTEN** - Keine bessere Alternative

---

## 2. Logger (Timber + FileLoggingTree)

### ✅ **Timber mit Custom FileLoggingTree - Optimal!**

**Aktuell (data/local/FileLoggingTree.kt):**
```kotlin
class FileLoggingTree : Timber.Tree() {
    - JSON Lines Format (.jsonl)
    - File rotation (5MB default)
    - CSV export
    - Async writing mit Coroutines
    - Structured metadata
}
```

**Update 2025-12-13:** Migration von Custom XML Logger zu Timber + FileLoggingTree

**Bewertung:**
- ✅ **Timber + Custom Tree ist die beste Lösung**
- ✅ Persistentes Logging für User-Debugging
- ✅ CSV Export für Analyse
- ✅ JSON Lines Format für einfaches Parsing
- ✅ Structured Metadata (component, action, details)
- ✅ -350 Zeilen Code durch Migration

**Empfehlung:** ✅ **OPTIMAL** - Bereits migriert auf Timber

---

## 3. PhoneNumberValidator (libphonenumber)

### ✅ **Bereits optimal mit libphonenumber - Perfekt!**

**Aktuell (PhoneNumberValidator.kt):**
```kotlin
class PhoneNumberValidator {
    private val phoneUtil = PhoneNumberUtil.getInstance()  // Google's libphonenumber

    fun validatePhoneNumber(phoneNumber: String, defaultRegion: String = "AT")
    fun areSameNumber(number1: String, number2: String)
    fun extractCountryCode(phoneNumber: String)
}
```

**Bewertung:**
- ✅ **Verwendet bereits Google's libphonenumber** (Industry Standard!)
- ✅ Wrapper-Klasse mit App-spezifischer Logik (Dial-Prefix-Normalisierung)
- ✅ Fehlerbehandlung und Validation
- ✅ Keine bessere Alternative

**Empfehlung:** ✅ **BEHALTEN** - Bereits optimal

---

## 4. SharedPreferencesManager (Encrypted Preferences)

### ⚠️ **DataStore als moderne Alternative - Migration möglich (Optional)**

**Aktuell (data/local/SharedPreferencesManager.kt):**
```kotlin
class SharedPreferencesManager {
    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(...)  // androidx.security.crypto
    }
}
```

**Android-Alternative: Jetpack DataStore**
```kotlin
val dataStore = context.createDataStore(name = "settings")

// Proto DataStore (type-safe)
dataStore.data.map { preferences ->
    preferences[KEY_PHONE_NUMBER]
}
```

**Vergleich:**

| Feature | SharedPreferences | DataStore |
|---------|------------------|-----------|
| **Synchron API** | ✅ Ja | ❌ Nur Async (Flow) |
| **Type Safety** | ⚠️ Manuell | ✅ Proto (generiert) |
| **Encryption** | ✅ EncryptedSharedPreferences | ⚠️ Manuell (Custom) |
| **Crash Safety** | ⚠️ apply() nicht atomar | ✅ Transaktional |
| **Migration** | ❌ Manuell | ✅ Built-in Migration |
| **Performance** | ✅ Gut | ✅ Besser (große Daten) |

**Bewertung:**
- ⚠️ **DataStore ist moderner**, aber **keine Killer-Feature**
- ✅ EncryptedSharedPreferences funktioniert einwandfrei
- ⚠️ Migration aufwendig (alle ViewModels umstellen auf Flow)
- ⚠️ Encryption in DataStore komplizierter zu implementieren

**Empfehlung:** ⚠️ **OPTIONAL** - Migration nur wenn:
- App skaliert auf große Settings-Dateien
- Mehr Type-Safety gewünscht
- Sowieso größere Refactoring-Phase

**Priorität:** NIEDRIG (Nice-to-have, nicht notwendig)

---

## 5. CarrierTrie (Carrier Prefix Lookup)

### ❌ **KEINE Android-API - Behalten!**

**Aktuell (util/phone/CarrierTrie.kt):**
```kotlin
class CarrierTrie {
    fun insert(prefix: String, carrier: String)
    fun findLongestPrefix(number: String): Pair<String?, String>
}
```

**Verwendet für:**
- Identifikation von Mobilfunk-Providern (A1, T-Mobile, Drei, etc.)
- Longest-Prefix-Matching für überlappende Prefixes

**Android-Alternativen:**
1. **TelephonyManager.getNetworkOperatorName()**
   - ❌ Gibt nur **eigenen** Carrier zurück
   - ❌ Kann nicht **fremde** Nummern identifizieren

2. **PhoneNumberUtil.getCarrierForNumber()**
   - ❌ Existiert nicht in libphonenumber
   - ❌ API gibt nur Land, nicht Carrier

**Bewertung:**
- ✅ **Custom Trie ist perfekt für den Use-Case**
- ✅ Effiziente O(n) Lookup-Zeit (n = Ziffern)
- ✅ Unterstützt österreichische Carrier-Prefixes
- ❌ Android hat KEINE API für Carrier-Erkennung fremder Nummern

**Empfehlung:** ✅ **BEHALTEN** - Keine Alternative

---

## 6. PermissionHelper (Permission Checking)

### ⚠️ **Activity Result API als moderne Alternative - Akzeptabel**

**Aktuell (util/permission/PermissionHelper.kt):**
```kotlin
object PermissionHelper {
    fun checkPermission(context: Context, permission: String): Boolean
    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int)
}
```

**Android-Alternative: Activity Result API (Jetpack)**
```kotlin
val requestPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) { /* Permission granted */ }
}

// Verwendung
requestPermission.launch(Manifest.permission.SEND_SMS)
```

**Vergleich:**

| Feature | PermissionHelper | Activity Result API |
|---------|-----------------|-------------------|
| **Deprecated API** | ⚠️ Ja (requestPermissions) | ✅ Moderne API |
| **Callback-Handling** | ⚠️ Manuell (requestCode) | ✅ Type-safe Callback |
| **Compose-Integration** | ❌ Umständlich | ✅ rememberLauncherForActivityResult |
| **Funktionalität** | ✅ Funktioniert | ✅ Funktioniert |

**Bewertung:**
- ⚠️ **Activity Result API ist moderner**
- ✅ Aber: PermissionHelper funktioniert einwandfrei
- ⚠️ Migration aufwendig (alle Permission-Checks umstellen)
- ✅ Kein Funktionalitätsverlust aktuell

**Empfehlung:** ⚠️ **OPTIONAL** - Migration bei nächster größerer UI-Refactoring

**Priorität:** NIEDRIG (funktioniert, aber veraltet)

---

## 7. Weitere selbstgebaute Komponenten (Behalten!)

### ✅ **Diese sind ALLE notwendig:**

1. **SmsForegroundService** (service/SmsForegroundService.kt)
   - ✅ Multi-part SMS Reassembly
   - ✅ Parallel SMS/Email forwarding
   - ✅ WakeLock management
   - ❌ KEINE Android-Alternative für Custom-Logic

2. **SmsReceiver** (service/SmsReceiver.kt)
   - ✅ BroadcastReceiver für SMS_RECEIVED_ACTION
   - ❌ MUSS selbst implementiert werden (Android Requirement)

3. **BootReceiver** (service/BootReceiver.kt)
   - ✅ Service-Restart nach Reboot
   - ❌ MUSS selbst implementiert werden

4. **ViewModels** (presentation/viewmodel/*)
   - ✅ MVVM Architecture Pattern
   - ✅ State Management
   - ❌ KEINE Android-Alternative (ist bereits Jetpack Standard)

5. **Compose UI Components** (presentation/ui/*)
   - ✅ Custom UI für App-spezifische Funktionen
   - ❌ KEINE vorgefertigten Komponenten für diese Use-Cases

---

## 8. RCS und Dritt-App-Zugriff (Analyse 2026-07)

### ❌ **Keine nutzbare Android-API - Feature bewusst nicht gebaut!**

**Fragestellung:** Kann die App RCS-Nachrichten aus Google Messages weiterleiten oder wenigstens den RCS-Status auslesen?

**Antwort: Nein, in beiden Fällen.** Umgesetzt wurde stattdessen ein erklärender Hinweis (Startseite + Hilfe), der den Nutzer zum SMS-Fallback führt.

#### 8.1 Nachrichtenzugriff

| Weg | Ergebnis |
|-----|----------|
| `SMS_RECEIVED_ACTION` | ✅ funktioniert - aber ausschließlich für SMS. RCS läuft nicht über diesen Broadcast. |
| `RcsMessageStore` / `content://rcs` | ❌ In Android 10 angelegt, nie freigegeben, `@hide`. Kein Bestandteil des öffentlichen SDK. |
| `NotificationListenerService` | ❌ Geprüft und verworfen, siehe 8.3. |
| RCS Messages Archival (Enterprise) | ❌ Setzt vollständig verwaltete Android-Enterprise-Geräte voraus. |

#### 8.2 Statusabfrage

| Weg | Ergebnis |
|-----|----------|
| `ImsRcsManager` / `RcsUceAdapter` (API 30+) | ❌ Öffentlich dokumentiert, aber alle aussagekräftigen Methoden verlangen `READ_PRIVILEGED_PHONE_STATE`, `READ_PRECISE_PHONE_STATE` oder Carrier Privileges. |
| `CarrierConfigManager` | ❌ Liefert allenfalls, ob der Netzbetreiber RCS-Provisioning verlangt; seit Android 12 für Dritt-Apps gefiltert. |
| Google Messages | ❌ Exportiert keinen Content Provider und keine API zum RCS-Status. |
| `Telephony.Sms.getDefaultSmsPackage()` + `<queries>` | ✅ **Verwendet.** Zeigt nur, welche Nachrichten-App installiert bzw. Standard ist - nicht den RCS-Status. |

**Wichtig:** Selbst mit privilegiertem Zugriff wäre die Antwort nicht die gesuchte. Die IMS-APIs beschreiben den RCS-Stack des Netzbetreibers, während Google Messages überwiegend Googles Jibe-Backend über die Datenverbindung nutzt.

**Konsequenz für den Code:** Kein Text der App darf einen RCS-Status behaupten. Alle Formulierungen sind als Möglichkeit gehalten („falls RCS aktiv ist").

#### 8.3 Warum kein NotificationListenerService

Fünf harte Befunde, ausführlich in `rcs.md` (Anhang A):

1. **Keine positive RCS-Erkennung.** Google Messages postet für SMS, MMS und RCS dieselbe `MessagingStyle`-Benachrichtigung; kein öffentliches Unterscheidungsfeld.
2. **Android 15 redigiert Einmalcodes** für nicht privilegierte Listener (`RECEIVE_SENSITIVE_NOTIFICATIONS` ist unerreichbar) - ausgerechnet der wichtigste Anwendungsfall.
3. **Kein Foreground-Service-Start aus dem Listener.** Der SMS-Broadcast stellt die App kurzzeitig auf die Power-Management-Allowlist, ein Listener-Callback nicht → `ForegroundServiceStartNotAllowedException`.
4. **`onNotificationPosted` feuert auch bei Updates**, und `EXTRA_MESSAGES` enthält die gesamte Historie → ohne Wasserzeichen pro `sbn.key` mehrfache Weiterleitung.
5. **Kosten und Play-Risiko.** RCS-Nachrichten sind lang und emoji-lastig (UCS-2 → 67 Zeichen/Segment); Play bewertet Benachrichtigungszugriff für eine Zusatzfunktion kritisch.

#### 8.4 Umgesetzte Komponenten

- `domain/model/GoogleMessagesState.kt` - Enum + reine `resolveGoogleMessagesState()`, ohne Android-Abhängigkeit und damit unit-testbar
- `util/GoogleMessagesDetector.kt` - Context-Zugriff, fällt bei jedem Fehler auf `NOT_INSTALLED` zurück
- `presentation/ui/screens/home/RcsHintCard.kt` - stateless Karte + Host mit Preference-Anbindung
- `AndroidManifest.xml` - `<queries>`-Eintrag (Paketsichtbarkeit ab Android 11, **keine** Berechtigung)

---

## Zusammenfassung & Empfehlungen

### ✅ **BEHALTEN (100% richtig so):**

1. **EmailSender** - Keine Alternative für automatischen SMTP-Versand
2. **FileLoggingTree** - Timber + JSON Lines für persistentes Logging
3. **PhoneNumberValidator** - Verwendet bereits libphonenumber (optimal!)
4. **CarrierTrie** - Keine API für Carrier-Erkennung
5. **Alle Service-Komponenten** - App-spezifische Logik, muss custom sein

### ⚠️ **OPTIONAL (Nice-to-have, niedrige Priorität):**

1. **SharedPreferencesManager → DataStore**
   - **Vorteil:** Modernere API, bessere Type-Safety
   - **Nachteil:** Aufwendige Migration, Encryption komplizierter
   - **Priorität:** NIEDRIG
   - **Empfehlung:** Nur wenn App skaliert oder größere Refactoring-Phase

2. **PermissionHelper → Activity Result API**
   - **Vorteil:** Moderne API, bessere Compose-Integration
   - **Nachteil:** Funktioniert aktuell, Migration aufwendig
   - **Priorität:** SEHR NIEDRIG
   - **Empfehlung:** Bei nächster UI-Refactoring

---

## Detaillierte Migrationsanalyse (falls gewünscht)

### Migration 1: DataStore (Optional)

**Aufwand:** ~5-8 Stunden
**Komplexität:** Mittel
**Risiko:** Niedrig (Fallback auf SharedPreferences möglich)

**Schritte:**
1. Proto-Definition für Settings erstellen
2. DataStore-Migration für bestehende Preferences
3. Encryption-Layer für DataStore implementieren
4. Alle ViewModels auf Flow-basierte API umstellen
5. Tests und Verifizierung

**Vorteile:**
- ✅ Type-safe Settings
- ✅ Bessere Performance bei großen Daten
- ✅ Atomare Transaktionen

**Nachteile:**
- ❌ Aufwendig
- ❌ Encryption komplizierter
- ❌ Keine Sync-API (nur Flow)

**Empfehlung:** Nur wenn ohnehin größere Refactoring-Phase oder Performance-Probleme

---

### Migration 2: Activity Result API (Optional)

**Aufwand:** ~2-3 Stunden
**Komplexität:** Niedrig
**Risiko:** Sehr niedrig

**Schritte:**
1. PermissionHelper durch `rememberLauncherForActivityResult` ersetzen
2. Alle Permission-Requests in Compose anpassen
3. Tests

**Vorteile:**
- ✅ Moderne API
- ✅ Bessere Compose-Integration

**Nachteile:**
- ❌ Funktioniert aktuell auch so

**Empfehlung:** Bei nächster UI-Refactoring mitnehmen

---

## Abschließende Bewertung

### **Ranking: Qualität der Custom-Implementierungen**

| Komponente | Qualität | Standard-Konformität | Notwendigkeit |
|------------|----------|---------------------|---------------|
| EmailSender | ⭐⭐⭐⭐⭐ | ✅ JavaMail Standard | 🔴 KRITISCH |
| FileLoggingTree | ⭐⭐⭐⭐⭐ | ✅ Timber Standard | 🔴 KRITISCH |
| PhoneNumberValidator | ⭐⭐⭐⭐⭐ | ✅ libphonenumber | 🔴 KRITISCH |
| CarrierTrie | ⭐⭐⭐⭐⭐ | ✅ Algorithmus korrekt | 🟠 WICHTIG |
| SharedPreferencesManager | ⭐⭐⭐⭐☆ | ✅ EncryptedSharedPrefs | 🟢 AKZEPTABEL |
| PermissionHelper | ⭐⭐⭐☆☆ | ⚠️ Deprecated API | 🟢 FUNKTIONIERT |

**Gesamtbewertung:** ⭐⭐⭐⭐⭐ (5/5)

Die App verwendet **genau die richtigen Custom-Implementierungen** an den richtigen Stellen. Die erfolgreich migrierten Komponenten (Contact Picker, Dialer, SmsManager) waren auch die einzigen, die sinnvoll durch System-APIs ersetzbar waren.

---

## Fazit

**Nach erfolgreicher Migration von:**
- ✅ Contact Picker (statt eigene Kontaktverwaltung)
- ✅ System Dialer (statt eigene MMI-Codes)
- ✅ SmsManager (statt eigene GSM-7 Kodierung)

**Verbleibende Custom-Implementierungen sind ALLE gerechtfertigt:**

1. **EmailSender** - KEINE Android-API für automatischen SMTP-Versand
2. **FileLoggingTree** - Timber + JSON Lines für strukturiertes Logging
3. **PhoneNumberValidator** - Verwendet bereits Google's libphonenumber
4. **CarrierTrie** - KEINE API für Carrier-Erkennung
5. **SharedPreferencesManager** - Funktioniert, DataStore wäre Nice-to-have
6. **PermissionHelper** - Funktioniert, Activity Result API wäre moderner

**Empfehlung:** ✅ **KEINE zwingenden Änderungen notwendig**

Die App ist jetzt optimal strukturiert mit einer guten Balance zwischen:
- ✅ Nutzung von Android-System-APIs wo sinnvoll
- ✅ Custom-Implementierungen wo notwendig

**Einzige optionale Migration (niedrige Priorität):**
- SharedPreferencesManager → DataStore (nur wenn Performance-Probleme oder größere Refactoring-Phase)

Die Custom-Implementierungen sind **hochwertig, notwendig und sollten beibehalten werden**.
