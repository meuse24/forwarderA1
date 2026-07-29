# Google Play Release Checklist

> **Status: nicht der aktuelle Vertriebsweg.**
>
> Die App wird als signierte APK über [GitHub Releases](https://github.com/meuse24/forwarderA1/releases) verteilt, nicht über Google Play. Grund sind die SMS-Berechtigungen: Play lässt `RECEIVE_SMS`/`SEND_SMS` im Wesentlichen nur für Apps zu, die als Standard-SMS-App fungieren – das würde eine vollwertige Nachrichten-App voraussetzen (eigener Telephony-Provider-Zugriff, MMS-Behandlung, Konversations-UI) und liegt außerhalb des Zwecks dieser App.
>
> Für den tatsächlichen Weg – Download, Prüfung von Signatur-Fingerprint und Prüfsumme, Installation, Updates – siehe den Abschnitt „Installation der signierten App" in der `README.md`.
>
> Dieses Dokument bleibt als Referenz erhalten, falls eine Play-Veröffentlichung später doch relevant wird. Die Angaben zu Data Safety, Berechtigungen und Store-Listing sind dann weiterhin gültig.

## ✅ Bereits implementiert (Build-Verbesserungen)

### 1. **Sicherheit & Trust Signale**
- ✅ App Signing konfiguriert (`signingConfigs.release`)
- ✅ ProGuard/R8 aktiviert (Code Obfuscation)
- ✅ Resource Shrinking aktiviert
- ✅ Network Security Config (nur HTTPS, außer SMTP)
- ✅ `usesCleartextTraffic="false"`
- ✅ Keine Legacy External Storage
- ✅ Hardware Features deklariert (`telephony` required)

### 2. **Privacy Policy Implementation**
- ✅ Datenschutzerklärung VOR Berechtigungsabfrage
- ✅ Zweisprachig (DE/EN)
- ✅ Detaillierte Begründung jeder Berechtigung
- ✅ Jederzeit einsehbar im Info-Screen
- ✅ Open Source Transparenz mit GitHub-Link

### 3. **Code-Qualität**
- ✅ Clean Architecture
- ✅ Verschlüsselte Datenspeicherung
- ✅ Kein Cleartext Traffic (außer SMTP STARTTLS)
- ✅ Foreground Service mit Notification
- ✅ Proper Lifecycle Management

## 📋 Release Build Prozess

### Schritt 1: Release APK/AAB erstellen

```bash
# Release APK
./build.sh assembleRelease

# Oder Android App Bundle (empfohlen für Play Store)
./build.sh bundleRelease
```

### Schritt 2: Keystore vorbereiten

Stelle sicher dass `keystore.properties` existiert:
```properties
storeFile=path/to/your/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

### Schritt 3: APK/AAB signieren

Die APK/AAB wird automatisch signiert wenn `keystore.properties` existiert.

**Ausgabe:**
- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

## 📝 Google Play Console - Upload Checkliste

### 1. **Store Listing**

#### App Name
```
SMS Forwarder Neo
```

#### Kurzbeschreibung (80 Zeichen)
```
SMS & Anruf Weiterleitung mit Datenschutz-Transparenz. Open Source.
```

#### Vollständige Beschreibung
```
SMS Forwarder Neo leitet eingehende SMS-Nachrichten weiter und kann die Rufumleitung beim Mobilfunkanbieter per MMI/USSD konfigurieren.

🔒 DATENSCHUTZ ZUERST
• Datenschutzerklärung VOR Berechtigungsabfrage
• Alle Daten verschlüsselt lokal gespeichert
• Open Source - vollständiger Code auf GitHub
• Keine Datensammlung oder externe Server

✨ HAUPTFUNKTIONEN
• SMS-zu-SMS Weiterleitung an beliebige Nummer
• SMS-zu-Email Weiterleitung an mehrere Adressen
• Rufumleitung via MMI/USSD (inklusive A1-Sprach-MMI)
• Dual-SIM Unterstützung
• Vordergrund-Service für zuverlässige Verarbeitung

🔧 TECHNISCH
• Android 10+ (API 29+)
• Kotlin + Jetpack Compose
• Clean Architecture
• Verschlüsselte SharedPreferences

📖 OPEN SOURCE
GitHub: https://github.com/meuse24/forwarderA1
Dokumentation: https://meuse24.github.io/forwarderA1/

Die App ist für den privaten Gebrauch konzipiert und erfordert Telefon-Hardware.
```

#### App-Kategorie
```
Kategorie: Kommunikation
Tags: SMS, Weiterleitung, Forwarding, Privacy
```

### 2. **Privacy & Security**

#### Datenschutzerklärung URL
```
https://meuse24.github.io/forwarderA1/privacy-policy.html
```

Oder erstelle eine auf GitHub Pages basierend auf der in-app Policy.

#### Data Safety Formular

**Datenerfassung:**
- ❌ Keine Daten werden gesammelt oder geteilt
- ✅ Alle Daten bleiben lokal auf dem Gerät — **mit einer Ausnahme:** Bei aktiver E-Mail-Weiterleitung werden Absendernummer, Empfangszeitpunkt und Nachrichtentext an den vom Nutzer selbst angegebenen SMTP-Server übertragen. Empfänger und Server bestimmt ausschliesslich der Nutzer; es gibt keinen Server des Anbieters.
- ✅ Verschlüsselte Speicherung
- ✅ Weiterleitungen werden bis zur Zustellung verschlüsselt zwischengespeichert (SMS-Volltext, Absendernummer, bei E-Mail zusätzlich die Empfängeradressen), spätestens nach 7 Tagen gelöscht und von Cloud-Backup sowie Geräteübertragung ausgeschlossen
- ✅ Lokales MMI-Audit ist maskiert und auf 200 Einträge bzw. 30 Tage begrenzt

**Berechtigungen Begründung:**
- `RECEIVE_SMS` / `SEND_SMS`: SMS-Weiterleitung (Kernfunktion)
- `READ_CONTACTS`: Kontaktauswahl für Weiterleitungsziel
- `CALL_PHONE`: MMI-/USSD-Ausführung über die gewählte SIM, insbesondere A1-Sprach-MMI
- `READ_PHONE_STATE`: SIM-Karten Erkennung (Dual-SIM)
- `FOREGROUND_SERVICE`: Zuverlässige Hintergrundverarbeitung
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Service-Kontinuität
- `INTERNET`: E-Mail-Versand an den vom Nutzer konfigurierten SMTP-Server
- `ACCESS_NETWORK_STATE`: Prüfung vor dem E-Mail-Versand, ob überhaupt eine Verbindung besteht; ohne sie würde jeder Versuch im Funkloch das Wiederholungsbudget verbrauchen. Installationszeit-Berechtigung ohne Nutzerabfrage, kein Zugriff auf personenbezogene Daten.

**Paketsichtbarkeit (`<queries>`, keine Berechtigung):**
- `com.google.android.apps.messaging`: Die App prüft, ob Google Messages installiert bzw. Standard-SMS-App ist, um zu erklären, warum RCS-Chats nicht weitergeleitet werden können. Es wird kein Zugriff auf fremde App-Daten gewährt; die Data-Safety-Angaben ändern sich dadurch nicht.

**Bewusst NICHT angefordert:**
- **Benachrichtigungszugriff** (`BIND_NOTIFICATION_LISTENER_SERVICE`): Eine Weiterleitung von RCS über Benachrichtigungen wurde geprüft und verworfen (siehe `rcs.md`, Anhang A). Play bewertet den Zugriff auf sensible Informationen danach, ob er für eine beworbene Kernfunktion erforderlich ist – für eine Zusatzfunktion ist das ein reales Ablehnungsrisiko. Diese Entscheidung ist bei künftigen Feature-Wünschen zu berücksichtigen.

**Store-Listing-Hinweis:** Die Beschreibung muss die Grenze benennen ("leitet SMS weiter; RCS-Chats und MMS werden von Android nicht an Dritt-Apps ausgeliefert"), damit die beworbene Funktion und der tatsächliche Umfang übereinstimmen.

### 3. **App Content**

#### Zielgruppe
```
Altersfreigabe: Alle Altersgruppen
```

#### Anzeigen
```
Enthält keine Werbung
```

#### In-App Käufe
```
Keine In-App Käufe
```

### 4. **Screenshots vorbereiten**

Mindestens 2 Screenshots erforderlich:

1. **Home Screen** - Kontaktauswahl
2. **Privacy Policy** - Datenschutzerklärung Anzeige
3. **Settings** - Einstellungen Übersicht
4. **Mail Config** (optional) - Email-Weiterleitung
5. **Logs** (optional) - Protokollansicht

Format: JPEG oder PNG, 320dp - 3840dp

### 5. **App Signing**

✅ **Empfohlen: Play App Signing verwenden**

Vorteile:
- Google verwaltet den finalen Signing Key
- Automatische APK Optimierung
- Bessere Sicherheit

**Beim ersten Upload:**
1. Play Console → Release → Setup → App Signing
2. "Continue" klicken (Google erstellt signing key)
3. Upload-Zertifikat erstellen und hochladen

## 🚀 Upload-Prozess

### 1. Internal Testing Track

```bash
# AAB erstellen
./build.sh bundleRelease

# In Play Console:
# 1. Testing → Internal testing → Create new release
# 2. Upload app-release.aab
# 3. Release notes hinzufügen
# 4. Review → Start rollout
```

### 2. Closed/Open Testing

Nach erfolgreichem Internal Testing:
- Closed Alpha/Beta mit ausgewählten Testern
- Feedback sammeln
- Issues fixen

### 3. Production Release

Wenn alles stabil:
- Production release erstellen
- Staged Rollout empfohlen (5% → 10% → 50% → 100%)

## ⚠️ Häufige Ablehnungsgründe vermeiden

### ✅ Was wir richtig machen

1. **Privacy Policy VOR Berechtigungen** ✅
   - Nutzer versteht WARUM Berechtigungen nötig sind

2. **Detaillierte Begründungen** ✅
   - Jede Berechtigung ist erklärt

3. **Open Source Transparenz** ✅
   - Vollständiger Code auf GitHub einsehbar

4. **Verschlüsselte Datenspeicherung** ✅
   - androidx.security.crypto für alle sensiblen Daten

5. **Foreground Service mit Notification** ✅
   - Nutzer sieht dass App im Hintergrund läuft

6. **Keine versteckten Funktionen** ✅
   - Alle Features in Privacy Policy dokumentiert

### ❌ Was zu vermeiden ist

- ❌ Berechtigungen ohne Begründung
- ❌ SMS/Call Logs ohne klaren Zweck
- ❌ Versteckte Datensammlung
- ❌ Unverschlüsselte sensible Daten
- ❌ Background Services ohne User Awareness

## 🔍 Pre-Launch Checklist

Vor dem Upload:

- [ ] Release Build erfolgreich (`./build.sh assembleRelease`)
- [ ] Keystore konfiguriert und signiert
- [ ] Privacy Policy URL verfügbar
- [ ] Screenshots erstellt (min. 2)
- [ ] Store Listing vorbereitet
- [ ] App auf echtem Gerät getestet
- [ ] Alle Berechtigungen funktionieren
- [ ] Logs überprüft (keine Errors)
- [x] Version Code erhöht (aktuell: 5)
- [x] Version Name aussagekräftig (aktuell: "Barracuda 4.1.0")

## 📊 Nach dem Release

### Monitoring

1. **Play Console → Vitals**
   - Crash-Rate überwachen
   - ANR-Rate prüfen
   - Battery usage checken

2. **User Reviews**
   - Auf Feedback reagieren
   - Bugs zeitnah fixen

3. **Pre-Launch Reports**
   - Google testet App automatisch
   - Ergebnisse prüfen und optimieren

### Updates

```bash
# Version erhöhen in build.gradle.kts:
versionCode = 5
versionName = "NextCodename"

# Release build
./build.sh bundleRelease

# In Play Console hochladen
```

## 🆘 Support & Kontakt

- GitHub Issues: https://github.com/meuse24/forwarderA1/issues
- Website: https://www.meuse24.info
- Documentation: https://meuse24.github.io/forwarderA1/

---

**Wichtig:** Google Play Review kann 1-7 Tage dauern. Bei Ablehnung:
1. Grund genau lesen
2. Angeforderte Änderungen umsetzen
3. Neu hochladen mit Erklärung

Mit der implementierten Privacy Policy und den Security-Verbesserungen sollten die Chancen auf Approval sehr gut sein! 🎉
