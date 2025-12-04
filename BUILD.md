# Build-Anleitung für SMS Forwarder Neo A1

## Voraussetzungen

- **Android Studio** installiert mit JDK (JBR 21)
- **Git Bash** oder anderes Unix-ähnliches Terminal (für Windows)
- Android SDK (wird normalerweise mit Android Studio installiert)

## Schnellstart

### Option 1: Build-Script verwenden (Empfohlen)

Das Projekt enthält ein `build.sh`-Script, das automatisch den korrekten `JAVA_HOME`-Pfad setzt:

```bash
# Kotlin kompilieren
./build.sh

# Debug-APK bauen
./build.sh assembleDebug

# Release-APK bauen
./build.sh assembleRelease

# Debug-APK auf Gerät installieren
./build.sh installDebug

# Build bereinigen
./build.sh clean

# Tests ausführen
./build.sh test

# Beliebige Gradle-Task ausführen
./build.sh [task-name]
```

### Option 2: Gradle direkt verwenden

Wenn du Gradle direkt verwenden möchtest, stelle sicher, dass `JAVA_HOME` korrekt gesetzt ist:

```bash
# JAVA_HOME setzen (Git Bash / Windows)
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"

# Dann Gradle ausführen
./gradlew compileDebugKotlin
./gradlew assembleDebug
```

## Build-Konfiguration

### gradle.properties

Die Datei `gradle.properties` enthält die Java-Home-Konfiguration:

```properties
org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr
```

**Hinweis:** Diese Einstellung wird von Gradle Wrapper manchmal ignoriert. Das `build.sh`-Script ist daher die zuverlässigste Methode.

### local.properties

Android SDK-Pfad (automatisch von Android Studio generiert):

```properties
sdk.dir=C:\\Users\\[username]\\AppData\\Local\\Android\\Sdk
```

## Häufige Build-Befehle

```bash
# Schnelle Syntax-Prüfung
./build.sh compileDebugKotlin

# Debug-APK bauen
./build.sh assembleDebug

# Release-APK bauen (signiert)
./build.sh assembleRelease

# App auf verbundenem Gerät installieren
./build.sh installDebug

# Build-Cache löschen
./build.sh clean

# Alle Tests ausführen
./build.sh test

# Abhängigkeiten aktualisieren
./build.sh dependencies
```

## Fehlerbehebung

### Problem: "JAVA_HOME is set to an invalid directory"

**Lösung:** Verwende das `build.sh`-Script statt direktem Gradle-Aufruf:

```bash
./build.sh compileDebugKotlin
```

Das Script setzt automatisch den korrekten Pfad.

### Problem: "java: command not found"

**Ursache:** Java ist nicht im PATH oder JAVA_HOME ist falsch gesetzt.

**Lösung:** Verwende `build.sh` oder setze JAVA_HOME manuell:

```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
```

### Problem: Build nach System-Neustart schlägt fehl

**Ursache:** JAVA_HOME wird nicht persistent gespeichert.

**Lösung 1 (Empfohlen):** Verwende immer `build.sh`:

```bash
./build.sh
```

**Lösung 2:** Füge JAVA_HOME zu deiner `.bashrc` oder `.bash_profile` hinzu:

```bash
echo 'export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"' >> ~/.bashrc
source ~/.bashrc
```

### Problem: Gradle Daemon-Fehler

**Lösung:**

```bash
./build.sh --stop  # Stoppt den Gradle Daemon
./build.sh clean   # Bereinigt Build-Verzeichnisse
./build.sh         # Neuer Build
```

## Build-Ausgabe

### Debug-APK

- Pfad: `app/build/outputs/apk/debug/app-debug.apk`
- Nicht signiert für Entwicklung
- Installierbar auf Debug-Geräten

### Release-APK

- Pfad: `app/build/outputs/apk/release/app-release.apk`
- Signiert mit Keystore (falls konfiguriert)
- Bereit für Veröffentlichung

## Android Studio Integration

Du kannst das Projekt auch direkt in Android Studio öffnen:

1. Android Studio starten
2. "Open" → Projektverzeichnis wählen
3. Warten bis Gradle-Sync abgeschlossen ist
4. Build → Make Project (Strg+F9)

Android Studio verwendet automatisch das integrierte JDK.

## Systemanforderungen

- **OS:** Windows 10/11 (getestet mit Git Bash)
- **Android Studio:** Arctic Fox oder höher
- **JDK:** 21 (mitgeliefert mit Android Studio JBR)
- **Gradle:** 8.13 (Wrapper im Projekt enthalten)
- **Min Android SDK:** API 29 (Android 10)
- **Target Android SDK:** API 34 (Android 14)

## Nützliche Gradle-Tasks

```bash
# Alle verfügbaren Tasks anzeigen
./build.sh tasks --all

# Projekt-Abhängigkeiten anzeigen
./build.sh dependencies

# Build-Konfiguration anzeigen
./build.sh properties

# Nur Kotlin-Code kompilieren (schnell)
./build.sh compileDebugKotlin

# Lint-Prüfung ausführen
./build.sh lintDebug

# Test-Coverage generieren
./build.sh testDebugUnitTestCoverage
```

## Kontakt & Support

Bei Problemen mit dem Build-Prozess:

1. Überprüfe, ob Android Studio korrekt installiert ist
2. Stelle sicher, dass JDK 21 (JBR) unter `C:\Program Files\Android\Android Studio\jbr` existiert
3. Verwende `build.sh` statt direktem Gradle-Aufruf
4. Prüfe `BUILD.md` für bekannte Probleme

---

**Letzte Aktualisierung:** 2025-12-04
**Getestet mit:** Git Bash (MINGW64), Android Studio Iguana, Windows 11
