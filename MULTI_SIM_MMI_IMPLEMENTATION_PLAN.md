# Multi-SIM Unterstützung für MMI-Codes (Rufumleitung) - Implementierungsplan

**Erstellt:** 2025-12-08
**Projekt:** SMS Forwarder Neo (forwarderA1)
**Feature:** Multi-SIM Auswahl für MMI-Code-Ausführung (Anrufweiterleitungen)

---

## 📋 Schnellstart für neue Session

Um die Implementierung dieses Plans in einer neuen Claude Code Session zu starten:

```bash
cd E:\Users\guent\Desktop\Forwarder_A1
```

**Dann in Claude Code:**
```
Bitte implementiere den Plan aus der Datei MULTI_SIM_MMI_IMPLEMENTATION_PLAN.md
```

**Oder spezifischer:**
```
Lies die Datei MULTI_SIM_MMI_IMPLEMENTATION_PLAN.md und implementiere Phase 1-6
für die Multi-SIM-Unterstützung bei MMI-Codes. Beginne mit Phase 1: Domain Model & Datenschicht.
```

---

## 🎯 Analyse-Ergebnis

### Aktuelle Situation

**SMS-Weiterleitung (funktioniert bereits):**
- Vollständige SIM-Auswahl-Unterstützung
- 3 Modi: "Gleiche SIM wie Eingang", "Immer SIM 1", "Immer SIM 2"
- Nutzt API: `SmsManager.getSmsManagerForSubscriptionId(subscriptionId)`

**MMI-Codes (aktuell limitiert):**
- Verwendet `Intent.ACTION_CALL` ohne SIM-Auswahl
- Android-System verwendet automatisch die Standard-Sprach-SIM
- **Keine programmatische SIM-Auswahl möglich** mit der aktuellen Implementierung

### Die Lösung: PhoneAccountHandle API

Android bietet seit API 21 (Android 5.0) eine dokumentierte Möglichkeit zur SIM-Auswahl über `PhoneAccountHandle`:

```kotlin
// PhoneAccountHandle für spezifische SIM abrufen
val phoneAccountHandle = getPhoneAccountHandleForSubscription(subscriptionId)

// Im Intent übergeben
intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle)
```

**Vorteile:**
- ✅ Offizielle, dokumentierte Android API
- ✅ Keine zusätzlichen Berechtigungen erforderlich
- ✅ Graceful Fallback wenn nicht unterstützt
- ✅ Funktioniert ab Android 5.0+ (unsere minSdk ist 29/Android 10)

**Einschränkungen:**
- ⚠️ Nicht explizit für MMI-Codes dokumentiert (hauptsächlich für normale Anrufe)
- ⚠️ Möglicherweise OEM-abhängig (manche Hersteller ignorieren den Parameter)
- ⚠️ Benötigt Tests auf echten Dual-SIM-Geräten verschiedener Hersteller

---

## ✅ Bestätigte Implementierungsentscheidungen

### 1. Separate SIM-Auswahl für MMI-Codes (nicht gekoppelt mit SMS)

**Begründung:**
- SMS-Weiterleitung hat einen kontextabhängigen Modus "Gleiche SIM wie Eingang" (für MMI nicht sinnvoll)
- MMI-Codes benötigen einen "Standard-Sprach-SIM"-Modus
- Klare Trennung der Verantwortlichkeiten
- Maximale Flexibilität für den Nutzer

**Neue Modi für MMI:**
1. **Standard-Sprach-SIM (System)** - Verwendet die in Android-Einstellungen festgelegte Sprach-SIM
2. **Immer SIM 1** - Alle Rufumleitungen über SIM 1
3. **Immer SIM 2** - Alle Rufumleitungen über SIM 2

### 2. Testing-Strategie

**Verfügbare Ressourcen:**
- Nutzer hat Zugang zu Dual-SIM-Geräten
- Direktes Testing durch Nutzer möglich
- Kein Community-Beta-Testing erforderlich (zunächst)

### 3. Fallback-Strategie mit Info-Nachricht

**Wenn PhoneAccountHandle nicht funktioniert:**
- App zeigt Info-Nachricht in den Einstellungen
- Erklärt dass SIM-Auswahl auf diesem Gerät nicht unterstützt wird
- Bietet direkten Link zu Android-Systemeinstellungen

### 4. Code-Format-Kompatibilität (NICHT implementiert)

**Bewusste Entscheidung:**
- Keine automatische Umschaltung zwischen BMI (`**`) und Standard (`#`) Code-Formaten
- Keine Pro-SIM-Code-Konfiguration
- Nutzer muss manuell Code-Preset wechseln wenn SIM-Karten unterschiedliche Formate verwenden

---

## 📝 Implementierungsplan

### Phase 1: Domain Model & Datenschicht

**1.1 Neues Enum erstellen**

Datei: `app/src/main/java/info/meuse24/smsforwarderneoA1/domain/model/MmiSimSelectionMode.kt`

```kotlin
package info.meuse24.smsforwarderneoA1.domain.model

enum class MmiSimSelectionMode(val displayName: String) {
    DEFAULT_VOICE_SIM("Standard-Sprach-SIM (System)"),
    ALWAYS_SIM_1("Immer SIM 1"),
    ALWAYS_SIM_2("Immer SIM 2");

    companion object {
        fun fromString(value: String?): MmiSimSelectionMode {
            return values().find { it.name == value } ?: DEFAULT_VOICE_SIM
        }
    }
}
```

**1.2 SharedPreferencesManager erweitern**

Datei: `app/src/main/java/info/meuse24/smsforwarderneoA1/data/local/SharedPreferencesManager.kt`

Hinzufügen:
- Konstante: `private const val KEY_MMI_SIM_SELECTION_MODE = "mmi_sim_selection_mode"`
- Getter: `fun getMmiSimSelectionMode(): MmiSimSelectionMode`
- Setter: `fun setMmiSimSelectionMode(mode: MmiSimSelectionMode)`

### Phase 2: Utility-Funktionen

**2.1 PhoneAccountHandle-Unterstützung**

Datei: `app/src/main/java/info/meuse24/smsforwarderneoA1/PhoneSmsUtils.kt`

Neue Funktionen:

```kotlin
@SuppressLint("MissingPermission")
fun getPhoneAccountHandleForSubscription(
    context: Context,
    subscriptionId: Int
): PhoneAccountHandle? {
    // TelecomManager abrufen
    // Alle Phone Accounts mit getCallCapablePhoneAccounts() laden
    // Über ICC-ID die richtige SIM zuordnen
    // PhoneAccountHandle zurückgeben
}

fun determineTargetSubscriptionIdForMmi(
    context: Context,
    mmiSimSelectionMode: MmiSimSelectionMode
): Int {
    // Analog zu determineTargetSubscriptionId() für SMS
    // aber mit MmiSimSelectionMode statt SimSelectionMode
}
```

### Phase 3: ViewModel-Erweiterungen

**3.1 ContactsViewModel erweitern**

Datei: `app/src/main/java/info/meuse24/smsforwarderneoA1/ContactsViewModel.kt`

Hinzufügen:
```kotlin
private val _mmiSimSelectionMode = MutableStateFlow(MmiSimSelectionMode.DEFAULT_VOICE_SIM)
val mmiSimSelectionMode: StateFlow<MmiSimSelectionMode> = _mmiSimSelectionMode.asStateFlow()

private val _defaultVoiceSubscriptionId = MutableStateFlow(-1)
val defaultVoiceSubscriptionId: StateFlow<Int> = _defaultVoiceSubscriptionId.asStateFlow()

fun setMmiSimSelectionMode(mode: MmiSimSelectionMode) {
    _mmiSimSelectionMode.value = mode
    prefsManager.setMmiSimSelectionMode(mode)
}
```

**3.2 SIM-Info aktualisieren**

In `refreshSimInfo()`: Auch `defaultVoiceSubscriptionId` aktualisieren

### Phase 4: MainActivity MMI-Logik

**4.1 dialCodeNow() Funktion erweitern**

Datei: `app/src/main/java/info/meuse24/smsforwarderneoA1/MainActivity.kt` (Zeilen 501-598)

**Änderung:**

```kotlin
private fun dialCodeNow(normalizedCode: String, originalCode: String) {
    try {
        // 1. MMI SIM-Modus abrufen
        val mmiSimMode = prefsManager.getMmiSimSelectionMode()

        // 2. Ziel-Subscription-ID bestimmen
        val targetSubscriptionId = PhoneSmsUtils.determineTargetSubscriptionIdForMmi(
            this, mmiSimMode
        )

        // 3. PhoneAccountHandle abrufen (wenn nicht Standard-SIM)
        val phoneAccountHandle = if (targetSubscriptionId != -1 &&
                                     mmiSimMode != MmiSimSelectionMode.DEFAULT_VOICE_SIM) {
            PhoneSmsUtils.getPhoneAccountHandleForSubscription(this, targetSubscriptionId)
        } else {
            null
        }

        // 4. Intent mit PhoneAccountHandle erstellen
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = "tel:${Uri.encode(normalizedCode)}".toUri()
            putExtra("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", true)

            if (phoneAccountHandle != null) {
                putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE",
                         phoneAccountHandle as Parcelable)
            }
        }

        // [Rest der Funktion unverändert - Audio Focus, startActivity, Logging]
    } catch (e: Exception) {
        // [Fehlerbehandlung]
    }
}
```

**Imports hinzufügen:**
- `import android.os.Parcelable`
- `import android.telecom.TelecomManager`
- `import android.telecom.PhoneAccountHandle`
- `import info.meuse24.smsforwarderneoA1.domain.model.MmiSimSelectionMode`

### Phase 5: UI-Komponenten

**5.1 Neue Settings-Sektion erstellen**

Datei: `app/src/main/java/info/meuse24/smsforwarderneoA1/presentation/ui/screens/settings/MmiSimSelectionSection.kt`

Composable-Komponente mit:
- Radio Buttons für die 3 Modi
- Dynamische Labels (zeigt welche SIM die Standard-Sprach-SIM ist)
- Warnung wenn SIM 2 gewählt aber nicht verfügbar
- Fallback Info-Card wenn PhoneAccountHandle nicht unterstützt wird

**5.2 In SettingsScreen integrieren**

Datei: `app/src/main/java/info/meuse24/smsforwarderneoA1/presentation/ui/screens/settings/SettingsScreen.kt`

Nach der bestehenden `SimSelectionSection` einfügen:
```kotlin
HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

MmiSimSelectionSection(
    viewModel = viewModel,
    sectionTitleStyle = sectionTitleStyle
)
```

### Phase 6: String-Ressourcen

**Datei:** `app/src/main/res/values/strings.xml` und `values-de/strings.xml`

```xml
<!-- MMI SIM Selection -->
<string name="section_mmi_sim_selection">SIM-Auswahl für Rufumleitung (MMI-Codes)</string>
<string name="desc_mmi_sim_selection">Bestimmt, mit welcher SIM-Karte Rufumleitungen aktiviert/deaktiviert werden.</string>
<string name="mmi_sim_selection_default_voice">Standard-Sprach-SIM verwenden</string>
<string name="mmi_sim_selection_always_sim1">Immer SIM 1 verwenden</string>
<string name="mmi_sim_selection_always_sim2">Immer SIM 2 verwenden</string>
<string name="suffix_default_voice">(Standard-Sprach-SIM)</string>
<string name="suffix_not_available">(nicht verfügbar)</string>

<!-- Fallback Info Card -->
<string name="mmi_device_limitation_title">Geräte-Einschränkung</string>
<string name="mmi_device_limitation_desc">Auf Ihrem Gerät wird die SIM-Auswahl für MMI-Codes möglicherweise nicht unterstützt. In diesem Fall können Sie die Standard-Sprach-SIM in den Android-Systemeinstellungen ändern.</string>
<string name="btn_open_android_settings">Zu Android-Einstellungen</string>
```

---

## 🧪 Testing-Strategie

### ⚠️ WICHTIG: Unterschiedliche Code-Formate pro SIM-Karte

**Nutzer-Testumgebung:**
- **SIM 1:** A1 Austria BMI-Format (endet mit `*`)
  - Aktivierung: `*21*<Nummer>**`
  - Deaktivierung: `**21**`
  - Status: `*021**`
  - **Mit Voice-Rückmeldung** (Lautsprecher!)

- **SIM 2:** Standard-Format (endet mit `#`)
  - Aktivierung: `*21*<Nummer>#`
  - Deaktivierung: `##21#`
  - Status: `*#21#`
  - **Ohne Voice-Rückmeldung** (Display-Popup)

**Wichtig:**
- Die App verwendet einen einzigen Code-Satz für alle Operationen
- Bei unterschiedlichen Formaten muss der Nutzer manuell Code-Preset UND SIM-Auswahl umstellen

### Test-Szenarien

**Basis-Tests (gleiche Code-Formate):**
1. DEFAULT_VOICE_SIM → MMI auf System-Standard
2. ALWAYS_SIM_1 → MMI auf SIM 1
3. ALWAYS_SIM_2 → MMI auf SIM 2
4. Persistenz über App-Neustart
5. Graceful Fallback bei fehlender SIM

**Erweiterte Tests (unterschiedliche Formate):**
- BMI-Codes + ALWAYS_SIM_1 → ✅ Funktioniert
- BMI-Codes + ALWAYS_SIM_2 → ❌ Falsches Format
- Standard-Codes + ALWAYS_SIM_1 → ❌ Falsches Format
- Standard-Codes + ALWAYS_SIM_2 → ✅ Funktioniert

---

## 📁 Kritische Dateien

**Neu erstellen:**
1. `app/src/main/java/info/meuse24/smsforwarderneoA1/domain/model/MmiSimSelectionMode.kt`
2. `app/src/main/java/info/meuse24/smsforwarderneoA1/presentation/ui/screens/settings/MmiSimSelectionSection.kt`

**Modifizieren:**
1. `app/src/main/java/info/meuse24/smsforwarderneoA1/MainActivity.kt` (dialCodeNow)
2. `app/src/main/java/info/meuse24/smsforwarderneoA1/PhoneSmsUtils.kt` (neue Utilities)
3. `app/src/main/java/info/meuse24/smsforwarderneoA1/ContactsViewModel.kt` (StateFlows)
4. `app/src/main/java/info/meuse24/smsforwarderneoA1/data/local/SharedPreferencesManager.kt` (Preferences)
5. `app/src/main/java/info/meuse24/smsforwarderneoA1/presentation/ui/screens/settings/SettingsScreen.kt` (UI Integration)
6. `app/src/main/res/values/strings.xml` und `values-de/strings.xml`

---

## ⚠️ Einschränkungen & Risiken

### Code-Format-Kompatibilität

**NICHT implementiert:**
- Automatisches Wechseln zwischen BMI (`**`) und Standard (`#`) Formaten
- Pro-SIM-Code-Konfiguration

**Workaround für Nutzer mit unterschiedlichen Formaten:**
1. SIM-Auswahl auf "ALWAYS_SIM_1" + Code-Preset "A1 Austria" → SIM 1 funktioniert
2. SIM-Auswahl auf "ALWAYS_SIM_2" + Code-Preset "Standard" → SIM 2 funktioniert

### Technische Risiken

| Risiko | Wahrscheinlichkeit | Mitigation |
|--------|-------------------|------------|
| PhoneAccountHandle ignoriert für MMI | Mittel | Graceful Fallback + Info-Card |
| OEM-spezifisches Verhalten | Mittel | Tests auf mehreren Herstellern |
| Zukünftige Android-Version bricht API | Niedrig | Dokumentierte API |

---

## 🎯 Zusammenfassung

**Aufwand:** ~6-8 Stunden (Entwicklung + Testing)

**Nutzen:** Hoch (löst echtes Problem für Dual-SIM-Nutzer)

**Erwartetes Ergebnis:**
- Best Case: Volle SIM-Auswahl für MMI-Codes
- Realistisch: Funktioniert auf vielen modernen Geräten
- Worst Case: Graceful Fallback auf Standard-Sprach-SIM

---

**Erstellt von:** Claude Code (Plan Mode)
**Datum:** 2025-12-08
**Plan-ID:** shimmying-churning-duckling
