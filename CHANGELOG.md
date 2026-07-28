# Changelog

## Unreleased

- Rufumleitungs-Codes: Neuinstallationen verwenden das dokumentierte Standard-GSM/USSD-Profil. Bestehende Installationen behalten ihre wirksamen Codes durch eine Materialisierungs-Migration unverändert.
- Das frühere allgemeine A1-Default ist nun als optionales „A1-Sonderprofil“ gekennzeichnet; die Auswahl wird bei erkannter A1-SIM erklärt und bestätigt.

Alle nennenswerten Änderungen an SMS Forwarder Neo A1.

Format angelehnt an [Keep a Changelog](https://keepachangelog.com/de/1.1.0/).

## [Unveröffentlicht]

### Hinzugefügt

- **RCS-Hinweis (Nutzerführung).** Die App erklärt jetzt, warum RCS-Chats aus Google Messages nicht weitergeleitet werden, und führt zum SMS-Fallback.
  - Neue Hilfe-Rubrik „RCS-Chats: Warum kommen manche Nachrichten nicht an?" mit Anleitung zum Deaktivieren von RCS, Dual-SIM-Hinweis (RCS hängt an der Rufnummer, nicht am Gerät), Warnung vor mehrfachem Umschalten und Verweis auf Googles Deregistrierungsseite.
  - Einmaliger, wegklickbarer Hinweis auf der Startseite. Er unterscheidet, ob Google Messages die Standard-SMS-App oder nur installiert ist, und wird ohne Google Messages gar nicht angezeigt.
  - „Mehr erfahren" öffnet die Hilfe direkt bei der RCS-Rubrik.
  - Schalter „RCS-Hinweis auf der Startseite" in den App-Einstellungen. Ohne ihn wäre „Verstanden" eine Sackgasse: Der Hinweis ließe sich nie wieder zurückholen. Der Schalter wirkt sofort, ohne App-Neustart, und ist deaktiviert, wenn Google Messages nicht installiert ist.
- `<queries>`-Eintrag für `com.google.android.apps.messaging` im Manifest (Paketsichtbarkeit ab Android 11). **Keine** neue Berechtigung, kein Zugriff auf fremde App-Daten.

### Geändert

- **Target- und Compile-SDK auf 36 (Android 16) angehoben**, Android Gradle Plugin von 8.7.3 auf 8.9.1 (Mindestversion für `compileSdk` 36). Gradle 8.13, Kotlin 2.1.0 und JDK 17 bleiben unverändert. `minSdk` bleibt bei 29.
- **Mehr nutzbare Höhe auf der Startseite.** Die Kopfleiste hatte 56 dp feste Höhe ohne jeden Inhalt und färbt jetzt nur noch den Statusleistenbereich ein. Zusätzlich steht die transiente Meldung „Rufumleitung angestoßen" kompakt in einer Zeile (Text links, Aktion rechts) statt als hoher Block. Zuvor wurden beim Einrichten einer Weiterleitung der Deaktivieren-Button und die Aktionsbuttons unten aus dem sichtbaren Bereich geschoben. Die Fehlerzustände behalten ihre ausführliche Darstellung.
- **Edge-to-Edge umgesetzt.** Android 16 erzwingt ab `targetSdk` 36 die randlose Darstellung; ein Opt-out gibt es nicht mehr. `WindowCompat.setDecorFitsSystemWindows(window, true)` ist durch `enableEdgeToEdge()` ersetzt. `CustomTopAppBar` ist kein Material3-`TopAppBar` und behandelt seine Statusleisten-Insets jetzt selbst; der außerhalb des Scaffolds liegende Snackbar ebenfalls. Die `BottomNavigationBar` nutzt `NavigationBar` und regelt das bereits selbst.

### Dokumentation

- README: Abschnitt „Bekannte Grenzen" benennt RCS und MMS ausdrücklich.
- `docs/ANDROID_API_ANALYSIS.md`: neues Kapitel 8 zu RCS und Dritt-App-Zugriff, inklusive der geprüften und verworfenen API-Wege.
- `docs/GOOGLE_PLAY_CHECKLIST.md`: `<queries>` als Nicht-Berechtigung vermerkt; der bewusst nicht angeforderte Benachrichtigungszugriff ist mit Begründung dokumentiert.
- `rcs.md`: Entscheidungsdokument samt verworfener Alternative (`NotificationListenerService`) und Nachweis, dass der RCS-Status nicht auslesbar ist.

### Hinweise

- Die SMS-Verarbeitung wurde **nicht** verändert. `SmsReceiver`, `SmsForegroundService` und `PhoneSmsUtils` sind unangetastet.
- Eine Weiterleitung von RCS über einen `NotificationListenerService` wurde geprüft und verworfen. Wesentliche Gründe: Android 15 redigiert Einmalcodes für nicht privilegierte Listener, RCS ist in Benachrichtigungen nicht von SMS unterscheidbar, und aus einem Listener-Callback darf kein Foreground Service gestartet werden. Details in `rcs.md`, Anhang A.
- MMS (Bilder, Videos, Sprachnachrichten) werden weiterhin nicht weitergeleitet; die App empfängt keinen `WAP_PUSH_RECEIVED`-Broadcast. Neu ist, dass dies dokumentiert und in der App erklärt wird.

## [4.1.0] – Barracuda

- Gehärteter A1-MMI-Weiterleitungsablauf.
- Sicherheits- und Zuverlässigkeitsverbesserungen bei der SMS-Verarbeitung.
- Klarstellung des MMI-Implementierungsstands in der Dokumentation.
