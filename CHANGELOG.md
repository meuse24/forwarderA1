# Changelog

Alle nennenswerten Änderungen an SMS Forwarder Neo A1.

Format angelehnt an [Keep a Changelog](https://keepachangelog.com/de/1.1.0/).

## [5.0.0] – Barracuda

Diese Fassung macht die Weiterleitung dauerbetriebsfest. Auf Android 15 und 16 stand die
Weiterleitung bisher nach sechs Stunden still und startete nach einem Geräteneustart nicht mehr
von selbst — beides ist behoben. **Ein Update ist für alle Nutzer auf Android 15 oder neuer
dringend empfohlen.**

### Behoben

- **Die Weiterleitung stand nach sechs Stunden still.** Der Vordergrunddienst lief als Typ `dataSync`. Android beendet solche Dienste nach sechs Stunden je 24 Stunden; danach schlug jeder Startversuch fehl und eingehende SMS gingen verloren — ohne jede Meldung. Der Dienst läuft jetzt als `specialUse`, für den kein Zeitlimit dokumentiert ist. Am Gerät nachgemessen: mit künstlich auf zwei Minuten gesetztem `dataSync`-Limit lief der Dienst unbeeinträchtigt weiter.
- **Nach einem Geräteneustart lief die Weiterleitung nicht mehr an.** Seit `targetSdk` 35 darf ein `BOOT_COMPLETED`-Empfänger keinen `dataSync`-Dienst mehr starten. Mit dem neuen Typ startet die Weiterleitung nach einem Neustart wieder von selbst, ohne dass die App geöffnet werden muss.
- **Ein SMTP-Ausfall verhinderte den SMS-Versand.** Beide Kanäle liefen als Geschwister in derselben Struktur; ein schnell scheiternder E-Mail-Zweig brach den noch wartenden SMS-Zweig mit ab. Die Kanäle sind jetzt voneinander unabhängig.
- **Ein SMTP-Ausfall erzeugte bis zu drei zusätzliche, kostenpflichtige SMS.** Der Wiederholungsversuch wiederholte die gesamte Verarbeitung statt nur den fehlgeschlagenen E-Mail-Versand. Wiederholt wird jetzt ausschliesslich die E-Mail.
- **Fehlgeschlagene SMS verschwanden spurlos.** Ein Funkloch führte zum endgültigen, stillen Verlust — der Fehler wurde nur protokolliert. Jede Weiterleitung hat jetzt einen gespeicherten Zustand; belegte Fehlschläge werden bis zu dreimal wiederholt (nach 30 s, 2 min, 10 min), ungeklärte Fälle in der App angezeigt.
- **Ein Prozessende während der Verarbeitung kostete die Nachricht.** Nutzdaten und Wiederholungszähler lagen nur im Arbeitsspeicher. Beides liegt jetzt verschlüsselt auf dem Gerät und wird nach einem Neustart weiterverarbeitet.
- **Absender mit Unterstrich wurden abgeschnitten** — aus `MY_BANK` wurde `MY`, sowohl im weitergeleiteten Text als auch in der Schleifenerkennung.
- **Nachrichten über 1530 Zeichen wurden vollständig verworfen.** Sie werden jetzt gekürzt weitergeleitet und die Kürzung im Text markiert.
- **Die SIM-Angabe im weitergeleiteten Text war falsch.** Ausgegeben wurde die interne Subscription-ID als „Slot".
- **Eine weggeklickte Benachrichtigungsberechtigung legte die Weiterleitung lahm.** Der Dienst beendete sich selbst, obwohl Android die Berechtigung für einen Vordergrunddienst gar nicht verlangt. Er läuft jetzt weiter; unterdrückt ist lediglich die Statusanzeige, und darauf weist die App hin.
- **Rückmeldungen des Mobilfunknetzes konnten nach einem Neustart falsch zugeordnet werden.** Die Zuordnung hing an Daten, die nicht zur Identität einer Rückmeldung zählen, und an einem Zähler, der nach jedem Neustart wieder bei null begann.
- **Eine unlesbare Weiterleitungsliste verschwand stillschweigend.** War die verschlüsselte Datei beschädigt, startete die App mit leerer Liste und ohne jeden Hinweis — alle darin vermerkten Weiterleitungen waren verloren. Der Fall wird jetzt erkannt, dauerhaft vermerkt und auf der Startseite angezeigt; der Betrieb läuft dabei weiter. Am Gerät durch gezielte Beschädigung der Datei nachgestellt.
- **Ein Absturz im Empfang blieb folgenlos protokolliert.** Der Empfänger ist jetzt vollständig abgesichert, einzelne unbrauchbare Teile verwerfen nicht mehr die ganze Nachricht, und die SIM-Abfrage erfolgt nur noch einmal je Nachricht.

### Hinzugefügt

- **Persistente Sendewarteschlange.** Jede Weiterleitung durchläuft nachvollziehbare Zustände, von der Einreihung bis zur Bestätigung durch das Netz. Sie liegt in einer eigenen verschlüsselten Datei, getrennt von der Konfiguration, ist vom Cloud-Backup ausgenommen und wird nach 50 Einträgen oder 7 Tagen bereinigt.
- **Hinweiskarte auf der Startseite.** Zeigt unterdrückte Statusanzeige, aktive Batterieoptimierung, fehlgeschlagene oder ungeklärte Weiterleitungen, verlorene Warteschlangeneinträge und ein Zeitlimit des Systems. Keiner dieser Zustände stoppt die Weiterleitung; alle sind quittierbar.
- **RCS-Hinweis (Nutzerführung).** Die App erklärt jetzt, warum RCS-Chats aus Google Messages nicht weitergeleitet werden, und führt zum SMS-Fallback.

- **RCS-Hinweis (Nutzerführung).** Die App erklärt jetzt, warum RCS-Chats aus Google Messages nicht weitergeleitet werden, und führt zum SMS-Fallback.
  - Neue Hilfe-Rubrik „RCS-Chats: Warum kommen manche Nachrichten nicht an?" mit Anleitung zum Deaktivieren von RCS, Dual-SIM-Hinweis (RCS hängt an der Rufnummer, nicht am Gerät), Warnung vor mehrfachem Umschalten und Verweis auf Googles Deregistrierungsseite.
  - Einmaliger, wegklickbarer Hinweis auf der Startseite. Er unterscheidet, ob Google Messages die Standard-SMS-App oder nur installiert ist, und wird ohne Google Messages gar nicht angezeigt.
  - „Mehr erfahren" öffnet die Hilfe direkt bei der RCS-Rubrik.
  - Schalter „RCS-Hinweis auf der Startseite" in den App-Einstellungen. Ohne ihn wäre „Verstanden" eine Sackgasse: Der Hinweis ließe sich nie wieder zurückholen. Der Schalter wirkt sofort, ohne App-Neustart, und ist deaktiviert, wenn Google Messages nicht installiert ist.
- `<queries>`-Eintrag für `com.google.android.apps.messaging` im Manifest (Paketsichtbarkeit ab Android 11). **Keine** neue Berechtigung, kein Zugriff auf fremde App-Daten.

### Geändert

- **Kompaktere Kopfzeile der Weiterleitung.** Statt vier Zeilen (`Von:`, `Zeit:`, `SIM:`, `Nachricht:`) steht jetzt eine: `Absender TT.MM. HH:MM SIM1`. Das spart rund 40 Zeichen und damit häufig die zweite, kostenpflichtige SMS. Angegeben wird der SIM-Steckplatz, nicht mehr die interne Kennung.
- **Rufumleitungs-Codes:** Neuinstallationen verwenden das dokumentierte Standard-GSM/USSD-Profil. Bestehende Installationen behalten ihre wirksamen Codes durch eine Materialisierungs-Migration unverändert.
- Das frühere allgemeine A1-Default ist nun als optionales „A1-Sonderprofil“ gekennzeichnet; die Auswahl wird bei erkannter A1-SIM erklärt und bestätigt.
- **Target- und Compile-SDK auf 36 (Android 16) angehoben**, Android Gradle Plugin von 8.7.3 auf 8.9.1 (Mindestversion für `compileSdk` 36). Gradle 8.13, Kotlin 2.1.0 und JDK 17 bleiben unverändert. `minSdk` bleibt bei 29.
- **Mehr nutzbare Höhe auf der Startseite.** Die Kopfleiste hatte 56 dp feste Höhe ohne jeden Inhalt und färbt jetzt nur noch den Statusleistenbereich ein. Zusätzlich steht die transiente Meldung „Rufumleitung angestoßen" kompakt in einer Zeile (Text links, Aktion rechts) statt als hoher Block. Zuvor wurden beim Einrichten einer Weiterleitung der Deaktivieren-Button und die Aktionsbuttons unten aus dem sichtbaren Bereich geschoben. Die Fehlerzustände behalten ihre ausführliche Darstellung.
- **Edge-to-Edge umgesetzt.** Android 16 erzwingt ab `targetSdk` 36 die randlose Darstellung; ein Opt-out gibt es nicht mehr. `WindowCompat.setDecorFitsSystemWindows(window, true)` ist durch `enableEdgeToEdge()` ersetzt. `CustomTopAppBar` ist kein Material3-`TopAppBar` und behandelt seine Statusleisten-Insets jetzt selbst; der außerhalb des Scaffolds liegende Snackbar ebenfalls. Die `BottomNavigationBar` nutzt `NavigationBar` und regelt das bereits selbst.

### Dokumentation

- README: Abschnitt „Bekannte Grenzen" benennt RCS und MMS ausdrücklich.
- `docs/ANDROID_API_ANALYSIS.md`: neues Kapitel 8 zu RCS und Dritt-App-Zugriff, inklusive der geprüften und verworfenen API-Wege.
- `docs/GOOGLE_PLAY_CHECKLIST.md`: `<queries>` als Nicht-Berechtigung vermerkt; der bewusst nicht angeforderte Benachrichtigungszugriff ist mit Begründung dokumentiert.
- `rcs.md`: Entscheidungsdokument samt verworfener Alternative (`NotificationListenerService`) und Nachweis, dass der RCS-Status nicht auslesbar ist.
- README: Die Behauptung „Heartbeat-Monitoring" ist entfernt — es gab dazu keinen Quelltext. An ihre Stelle treten die tatsächlichen Systemgrenzen des Dauerbetriebs und die Zustellsemantik.
- `sms.md`: Umsetzungsplan mit Evidenzbasis, Befundlage, Zustandsmaschine, den sieben abweichend entschiedenen Punkten und den Ergebnissen der Gerätetests.

### Hinweise

- **Zustellsemantik, ausdrücklich benannt.** Wiederholt wird nur, wo das Netz einen Fehlschlag zurückmeldet (kein Dienst, Funk aus, allgemeiner Sendefehler). Wo jede Aussage fehlt — etwa wenn der Prozess im Sendefenster endet oder eine Rückmeldung 15 Minuten ausbleibt —, wird **nicht** erneut gesendet; der Vorgang wird als ungeklärt angezeigt. Diese Wahl nimmt den selteneren Verlust in Kauf, um den häufigeren Doppelversand zu vermeiden.
- **Ausnahme bei mehrteiligen Nachrichten:** Scheitert ein Teil, wird der ganze Vorgang neu versandt; bereits zugestellte Teile können dann doppelt ankommen. Eine unvollständige mehrteilige SMS setzt das Empfängergerät sonst nie zusammen.
- „Gesendet" heisst „vom Netz angenommen", nicht „beim Empfänger angekommen". Letzteres liefert nur der optionale Zustellbericht, den viele Netze nicht senden.
- **Systemgrenzen:** „Stopp erzwingen" und ein Stopp über den Task Manager setzen die Weiterleitung bis zum nächsten Start der App aus. Dagegen hilft kein Wächter innerhalb der App — er wäre selbst mit beendet.
- Die Zustandslogik ist von Android gelöst und durch 96 Unit-Tests abgedeckt. Zusätzlich auf einem Galaxy A53 unter Android 16 geprüft: Neustart ohne Öffnen der App, Prozessende mit Wiederanlauf, ein- und mehrteilige Weiterleitung, Betrieb ohne Benachrichtigungsberechtigung.
- Eine Weiterleitung von RCS über einen `NotificationListenerService` wurde geprüft und verworfen. Wesentliche Gründe: Android 15 redigiert Einmalcodes für nicht privilegierte Listener, RCS ist in Benachrichtigungen nicht von SMS unterscheidbar, und aus einem Listener-Callback darf kein Foreground Service gestartet werden. Details in `rcs.md`, Anhang A.
- MMS (Bilder, Videos, Sprachnachrichten) werden weiterhin nicht weitergeleitet; die App empfängt keinen `WAP_PUSH_RECEIVED`-Broadcast. Neu ist, dass dies dokumentiert und in der App erklärt wird.

## [4.1.0] – Barracuda

- Gehärteter A1-MMI-Weiterleitungsablauf.
- Sicherheits- und Zuverlässigkeitsverbesserungen bei der SMS-Verarbeitung.
- Klarstellung des MMI-Implementierungsstands in der Dokumentation.
