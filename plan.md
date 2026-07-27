# Umsetzungsplan: A1-MMI-Rufumleitung mit automatischem Fortsetzen und nachvollziehbarem Status

## Ziel

Die App soll den A1-spezifischen MMI-Sprachanruf zuverlässig auslösen, die SMS-Weiterleitung ohne Benutzereingabe
zeitnah aktivieren und dabei transparent unterscheiden zwischen:

- technisch bestätigtem Erfolg,
- plausibel angenommenem Erfolg,
- eindeutig fehlgeschlagenem Wählvorgang,
- vom Nutzer gemeldetem Fehler.

Der Ablauf darf weder auf eine Dialogbestätigung noch auf ein IDLE-Call-State-Ereignis unbegrenzt warten.

## Ist-Zustand (verifiziert am Code)

| Befund | Fundstelle |
| --- | --- |
| Unbegrenztes `callState.first { IDLE }` ohne Watchdog | `MainActivity.kt:858`, `MainActivity.kt:469` |
| Dialog-Timeout wird als Erfolg gewertet (`MMI_TIMEOUT_SUCCESS`) | `MainActivity.kt:820-825` |
| MMI/USSD-Unterscheidung per `endsWith("#")`, dreifach dupliziert | `ContactsViewModel.kt:650`, `:685`, `MainActivity.kt:451` |
| `awaitingMmiConfirmation` ist ein flüchtiges Activity-Feld | `MainActivity.kt:124` |
| `_pendingForwardingRequest` ist ein nicht persistierter Single-Slot ohne ID | `ContactsViewModel.kt:131`, `:157-162` |
| `resolvePendingForwardingResult` gleicht das Ergebnis nicht gegen den Vorgang ab | `ContactsViewModel.kt:425-440` |
| `TelecomManager.placeCall` wird nur bei `phoneAccountHandle != null` versucht | `MainActivity.kt:703` |
| Zielrufnummer wird im Klartext geloggt – auch als Bestandteil des MMI-Codes | `ContactsViewModel.kt:477`, `MainActivity.kt:464/478/545/590/716/755` |
| A1-Defaults enden nie auf `#`, der USSD-Callback-Pfad ist damit unerreichbar | `SharedPreferencesManager.kt:676-679` |
| Keine Unit-Tests vorhanden | `app/src/test/.../ExampleUnitTest.kt` |

## Fachliche Entscheidung

Die SMS-Weiterleitung wird nach erfolgreich übergebenem MMI-Wählauftrag aktiviert. Das ist bewusst ein
Betriebsentscheid, kein technischer Nachweis der Netzrufumleitung.

**Tragende Begründung:** Die SMS-Weiterleitung ist technisch von der MMI-Rufumleitung entkoppelt. Sie wird durch
`SMS_RECEIVED_ACTION` am Gerät plus `isForwardingActive()` ausgelöst (`SmsForegroundService.kt:317, 419`); der
MMI-Code setzt demgegenüber die *Anruf*-Umleitung im Netz. Ein falsch angenommener MMI-Erfolg beeinträchtigt die
SMS-Weiterleitung also nicht, während ein blockiertes Aktivieren reale SMS verliert. Die Fehlerkosten sind
asymmetrisch, und zwar eindeutig zugunsten des sofortigen Aktivierens.

**Reichweite dieser Begründung:** Sie gilt ausschließlich für die SMS-Weiterleitung. Für die Netz-Rufumleitung ist
ein falsch angenommener Erfolg sehr wohl folgenreich — erwartete *Anrufe* erreichen dann das Ziel nicht. Genau
deshalb wird der Nachweisgrad separat geführt, dauerhaft sichtbar gehalten und bei Deaktivierung mit einer eigenen
Warnung versehen. „Sofort aktivieren" ist eine Aussage über die SMS-Weiterleitung, keine Entwarnung zur
Rufumleitung.

Die bisherige Bedeutung von `forwardingActive` bleibt:

> Die App soll SMS an den gewählten Kontakt weiterleiten.

Der neue Verifikationsstatus beschreibt separat:

> Wie belastbar ist die Annahme, dass die A1-Rufumleitung im Netz gesetzt wurde?

Damit blockiert eine nicht beantwortete Rückfrage nicht die Arbeit, während Audit/Support trotzdem erkennen können, ob
ein Erfolg bestätigt oder nur angenommen wurde.

### Deaktivierung: umgekehrte Asymmetrie

Beim Deaktivieren kehrt sich die Fehlerrichtung um. Ein angenommener Erfolg führt dazu, dass die App die
SMS-Weiterleitung abschaltet, während die Rufumleitung im Netz möglicherweise noch steht — der Nutzer hält den
Vorgang für abgeschlossen, Anrufe laufen aber weiter zum fremden Ziel.

Regel: Auch bei Deaktivierung wird `forwardingActive` sofort auf `false` gesetzt (die App soll nicht ungewollt
weiterleiten), aber der Verifikationsstatus `ASSUMED_SUCCESS` erzeugt hier eine **dauerhafte, sichtbare Warnung**
mit Handlungsaufforderung „Rufumleitung im Netz prüfen“ und Direktzugriff auf den Statuscode `*021**`. Die Warnung
verschwindet nur durch bestätigte Prüfung oder erneuten Deaktivierungsversuch.

## Datenmodell

Ergebnis und Evidenz werden getrennt modelliert. Ein Vorgang kann gleichzeitig „angenommen erfolgreich“ und „kein
Anruf beobachtet“ sein — beides in einen Enum zu pressen erzwingt eine falsche Entweder-oder-Entscheidung.

```kotlin
/** Ergebnis: wie belastbar ist die Annahme? Genau ein Wert pro Vorgang. */
enum class ForwardingVerification {
    NOT_CHECKED,           // nur Migration bestehender Datenstände
    ASSUMED_SUCCESS,       // Wählauftrag übergeben, plausibles Erfolgssignal
    UNKNOWN_NO_RESPONSE,   // Rückmeldung erwartet, aber ausgeblieben
    CONFIRMED_SUCCESS,
    DIAL_FAILED,
    USER_REPORTED_FAILURE
}

/** Evidenz: unabhängig beobachtete Signale. Mehrere gleichzeitig möglich. */
data class MmiEvidence(
    val callObserved: Boolean = false,      // OFFHOOK gesehen
    val callDurationMs: Long? = null,       // OFFHOOK -> IDLE
    val watchdogExpired: Boolean = false,   // erwartetes Signal im Fenster ausgeblieben
    val ussdResponse: String? = null
)
```

`CALL_NOT_OBSERVED` entfällt als Status und wird zu `watchdogExpired = true`.

**Abgrenzung `ASSUMED_SUCCESS` / `UNKNOWN_NO_RESPONSE` / `NOT_CHECKED`:** `ASSUMED_SUCCESS` setzt ein positives,
wenn auch schwaches Indiz voraus — bei Sprach-MMI den entgegengenommenen Wählauftrag und idealerweise ein
beobachtetes OFFHOOK. Wo überhaupt kein solches Indiz existiert, weil die einzige vorgesehene Rückmeldung
ausgeblieben ist, gilt `UNKNOWN_NO_RESPONSE`. Der Unterschied zu `NOT_CHECKED` ist die Warnwirkung:
`UNKNOWN_NO_RESPONSE` erzeugt eine dauerhafte Warnung, `NOT_CHECKED` bewusst nicht, weil es lediglich aus der
Migration stammt.

### Lebenszyklus des Vorgangs

```kotlin
enum class MmiOperationState {
    DIALING,   // Wählauftrag übergeben, Ergebnis noch offen
    SETTLED    // Verifikationsstatus endgültig gesetzt
}
```

Der Zustand ist Teil des persistierten Vorgangs und dient zugleich als Nebenläufigkeitssperre (siehe
„Nebenläufigkeit und Doppelauslösung").

### Persistenter Vorgang

Der laufende Vorgang muss Process Death überleben — beim Wechsel in den Dialer kann die Activity getötet werden,
und heute verschwindet der Vorgang dann lautlos mitsamt `awaitingMmiConfirmation`.

```kotlin
data class MmiOperation(
    val id: String,                       // UUID, Korrelationsschlüssel
    val action: ForwardingAction,         // ACTIVATE | DEACTIVATE
    val targetSubscriptionId: Int,
    val executionMode: MmiExecutionMode,
    val dialedAtMillis: Long,
    val dialPath: DialPath,               // TELECOM_MANAGER | ACTION_CALL
    val state: MmiOperationState,
    val verification: ForwardingVerification,
    val evidence: MmiEvidence,
    val userMessage: String? = null
)
```

Persistiert wird in `SharedPreferencesManager` (JSON, ein Slot für den laufenden Vorgang plus ein begrenzter,
maskierter Audit-Ringpuffer). Beim App-Start wird ein noch offener Vorgang geladen und über den Watchdog aufgelöst,
statt ihn zu verlieren.

**ID-Abgleich:** `resolvePendingForwardingResult` erhält die `operationId` als Pflichtparameter und ignoriert
Ergebnisse, deren ID nicht zum aktuellen Vorgang passt. Das schließt die heutige Lücke, dass ein verspätetes
USSD-Callback oder ein zweiter Klick einen fremden Vorgang auflöst.

### Logging und Datenschutz

Die Zielrufnummer steckt nicht nur im Feld `number`, sondern **im MMI-Code selbst** (`*21*<Nummer>**`), und dieser
Code wird an mindestens sechs Stellen im Klartext geloggt. Maskierung muss deshalb am Code ansetzen, nicht nur am
Nummernfeld:

- Eine zentrale Funktion `maskMmiCode(code: String)` gibt `*21*+43664****89**` zurück (Präfix, Ländervorwahl und
  die letzten zwei Stellen sichtbar).
- Alle Logaufrufe in `MainActivity.dialCode`/`dialCodeNow` und `ContactsViewModel` gehen darüber.
- Das App-Audit enthält keine unmaskierte Zielrufnummer, keinen unmaskierten MMI-Code und keine rohe
  MMI-/USSD-Netzantwort.

### Verbindliche Audit- und Aufbewahrungsentscheidung

Das lokale Audit dient ausschließlich der technischen Fehleranalyse und der Unterstützung im Einsatz. Es ist kein
Beweis- oder Aktenbestand.

- Der Ringpuffer speichert höchstens 30 Tage und löscht ältere Einträge automatisch.
- Gespeichert werden nur Zeitstempel, Vorgangs-ID, Aktion, Ausführungs-/Wählweg, Verifikationsstatus, Evidenz,
  Fehlerquelle sowie maskierte Zielnummern, MMI-Codes und Netzantworten.
- Die aktive Zielrufnummer wird nur für die laufende SMS-Weiterleitung gespeichert; sie wird beim Deaktivieren
  oder Wechsel der Zielnummer gelöscht bzw. überschrieben.
- Vollständige Zielrufnummern und unmaskierte Netzantworten werden nicht in den lokalen Audit-Ringpuffer
  aufgenommen. Falls sie im Einzelfall beweisrelevant sind, gehören sie ausschließlich in das dafür freigegebene
  Fall- bzw. Aktenverfahren.

## Code-Ablauf

### 1. Expliziter Ausführungsmodus statt Suffix-Heuristik

```kotlin
enum class MmiExecutionMode { USSD_CALLBACK, VOICE_MMI_CALL }
```

Der Modus wird **einmal** aus der Code-Konfiguration abgeleitet und im `MmiOperation` mitgeführt. Die heutige
Dreifach-Prüfung `endsWith("#")` entfällt ersatzlos — sonst kann `pending.isUssd` vom tatsächlich gewählten Weg
abweichen. Zuständig ist eine Funktion in `SharedPreferencesManager` bzw. einem neuen `MmiCodeConfig`, nicht die
Activity.

Für die A1-Defaults (`*21*` / `**`, `**21**`, `*021**`) ergibt das durchgehend `VOICE_MMI_CALL`. Konsequenz, die
explizit akzeptiert wird: **`CONFIRMED_SUCCESS` ist bei A1-Konfiguration nicht erreichbar.** Es gibt bei
Sprach-MMI keine Rückmeldung, die die App auswerten könnte. Der Status existiert für die generische Konfiguration
(`##21#` o. ä.) und für spätere Statusabfragen.

### 2. USSD_CALLBACK

Der Callback bleibt maßgeblich.

- Callback erfolgreich → `CONFIRMED_SUCCESS`, `evidence.ussdResponse` gesetzt
- Callback fehlgeschlagen → `DIAL_FAILED`
- Kein Callback innerhalb 30 s → `UNKNOWN_NO_RESPONSE` mit `watchdogExpired = true`, `UssdProgressDialog`
  schließen (heute bleibt er in diesem Fall unbegrenzt stehen)

**Ausdrücklich nicht `ASSUMED_SUCCESS`.** Bei USSD ist der Callback die einzige vorgesehene Rückmeldung; bleibt er
aus, existiert kein positives Indiz — anders als bei Sprach-MMI, wo der entgegengenommene Wählauftrag und ein
beobachtetes OFFHOOK als schwache Evidenz taugen. Ein Timeout hier als „angenommen erfolgreich" zu werten wäre
derselbe Denkfehler wie das heutige `MMI_TIMEOUT_SUCCESS`, nur an anderer Stelle.

Die SMS-Weiterleitung wird nach der fachlichen Entscheidung trotzdem aktiviert — der Nachweisgrad bleibt aber
„unbekannt" und erzeugt eine dauerhafte Warnung.

Zusätzlich: `getUssdCodeType` (`PhoneSmsUtils.kt:725`) klassifiziert über `startsWith(activatePrefix)` bzw.
Gleichheit mit dem Deaktiviercode. Bei überlappender Nutzerkonfiguration ist das mehrdeutig — die Klassifizierung
wird durch die `operationId` ersetzt, die ohnehin mitgeführt wird.

### 3. VOICE_MMI_CALL

- Code normalisieren (`+` → konfigurierte internationale Verkehrsausscheidungsziffer).
- Ziel-SIM über `EXTRA_PHONE_ACCOUNT_HANDLE` auswählen.
- `TelecomManager.placeCall()` versuchen; bei Exception `ACTION_CALL` als Fallback. Die heutige Bedingung
  `phoneAccountHandle != null` (`MainActivity.kt:703`) entfällt — `placeCall` funktioniert auch ohne Handle und ist
  für MMI der verlässlichere Weg; der Handle bleibt optionales Extra.
- Wenn der Aufruf ohne Exception an Telecom/Dialer übergeben wurde:
  - Vorgang persistieren, SMS-Weiterleitung **sofort** aktivieren.
  - `ASSUMED_SUCCESS`, Quelle `MMI_ASSUMED_SUCCESS` — nie `MMI_TIMEOUT_SUCCESS`, nie `CONFIRMED`.
- Bei Exception:
  - Keine automatische Aktivierung, `DIAL_FAILED`, klare Fehlermeldung, erneuter Versuch möglich.

Anmerkung zur Belastbarkeit: Weder `placeCall` noch `startActivity` werfen, wenn der Dialer den MMI-Code später
ablehnt. „Keine Exception“ bedeutet ausschließlich „Auftrag zugestellt“. Genau deshalb heißt der Status
`ASSUMED_SUCCESS` und nicht mehr.

### 4. Call-State-Beobachtung als reine Evidenz

- OFFHOOK beobachtet → `evidence.callObserved = true`.
- Danach IDLE → `evidence.callDurationMs` erfassen.
- Kein OFFHOOK innerhalb 20 s → `evidence.watchdogExpired = true`, Hinweis anbieten.
- Die Beobachtung darf die SMS-Weiterleitung nie blockieren. Jedes `first { IDLE }` wird in `withTimeoutOrNull`
  gekapselt (`MainActivity.kt:469` und `:858`).

**Die beiden Watchdogs haben unterschiedliche Konsequenzen und dürfen nicht gleich behandelt werden:**

| Wartestelle | Timeout | Verhalten bei Ablauf |
| --- | --- | --- |
| *Vor* dem Wählen: warten, bis ein laufender Anruf endet (`MainActivity.kt:459-469`) | 30 s | **Vorgang abbrechen, nicht wählen.** `DIAL_FAILED`, Grund „laufender Anruf nicht beendet". SMS-Weiterleitung wird **nicht** aktiviert, Vorgang bleibt manuell wiederholbar. |
| *Nach* dem Wählen: Call-State beobachten (`MainActivity.kt:858`) | 20 s | Nur Evidenz vermerken. Vorgang bleibt `ASSUMED_SUCCESS`, SMS-Weiterleitung bleibt aktiv. |

Der erste Fall ist der kritische: `withTimeoutOrNull` führt den Folgecode nach Ablauf einfach weiter aus. Ohne
expliziten Abbruch würde der MMI-Anruf in einen noch laufenden regulären Anruf hineinfallen — der Watchdog
verschlimmerte dann genau das Problem, das er verhindern soll. Der Rückgabewert muss also ausgewertet und der
Vorgang bei `null` beendet werden.

### 5. Dialog ersetzen

- Kein modales Ja/Nein-Fenster als Pflichtschritt; `MmiConfirmationDialog` und der 4-Sekunden-Job entfallen.
- Nach Rückkehr aus dem Anruf: Hinweis in der Statuskarte — „Rufumleitung wurde angestoßen. Nur bei Fehler tippen.“
- Primäre Aktion: **Fehler melden**.
- Kein Pflichtschritt: Der Hinweis bleibt acht Sekunden sichtbar und verschwindet dann ohne Eingabe. Der
  Nachweisgrad bleibt intern sowie in der Foreground-Benachrichtigung erhalten.
- Bei „Fehler melden“: `USER_REPORTED_FAILURE`, Warnhinweis, Optionen *Erneut versuchen* / *Bewusst fortsetzen*.

Umsetzung als Statuskarte statt zusätzlicher Notification: Die App hält bereits eine Foreground-Service-Notification
(`SmsForegroundService`), eine zweite konkurriert damit, und `POST_NOTIFICATIONS` kann ab API 33 verweigert sein.
Der Foreground-Text wird stattdessen um den Nachweisgrad ergänzt.

## Nebenläufigkeit und Doppelauslösung

Der ID-Abgleich löst nur die Korrelation: er verhindert, dass ein verspätetes Ergebnis den falschen Vorgang
abschließt. Er verhindert **nicht**, dass zwei rasch nacheinander gestartete MMI-Anrufe im Netz in unbestimmter
Reihenfolge ankommen — bei Aktivieren/Deaktivieren wäre das Endergebnis dann schlicht zufällig. Das ist eine
Fachregel und gehört ins Zustandsmodell, nicht in die Testfälle:

- Solange ein Vorgang im Zustand `DIALING` ist oder ein MMI-Anruf aktiv beobachtet wird, wird keine weitere
  Aktivierung oder Deaktivierung angenommen. Die auslösenden Bedienelemente sind in diesem Zeitraum gesperrt und
  zeigen den laufenden Vorgang an.
- Ein neuer Vorgang entsteht danach nur über eine bewusste Aktion („Erneut versuchen" bzw. regulär über die
  Statuskarte), nie implizit durch einen zweiten Klick.
- Erst mit `SETTLED` wird die Sperre aufgehoben.

**Die Sperre braucht selbst einen garantierten Ausgang, sonst blockiert sie die App dauerhaft.** Da der Vorgang
persistiert wird, überlebt ein hängender `DIALING`-Zustand auch Process Death und Neustart. Deshalb:

- Jeder `DIALING`-Vorgang trägt seinen Watchdog-Ablaufzeitpunkt (`dialedAtMillis` + Fenster) mit sich.
- Beim App-Start wird ein geladener `DIALING`-Vorgang sofort geprüft: ist das Fenster abgelaufen, wird er ohne
  weiteres Warten nach den Regeln aus Abschnitt 4 auf `SETTLED` gesetzt.
- Zusätzlich gilt eine harte Obergrenze: ein Vorgang bleibt nie länger als 60 s in `DIALING`, unabhängig davon,
  welche Signale ausbleiben.

## Umgang mit einer Fehlermeldung

Eine Nutzerfehlermeldung stoppt die SMS-Weiterleitung **nicht** automatisch.

Begründung: Da die SMS-Weiterleitung technisch nicht vom MMI-Erfolg abhängt (siehe oben), würde ein Stopp einen
funktionierenden Dienst wegen eines Fehlers in einem anderen Dienst abschalten. Im Einsatz führt das zum Verlust
weiterzuleitender SMS. Stattdessen wird deutlich gewarnt und eine explizite Bedienaktion verlangt.

Falls organisatorisch erforderlich, kann eine Einstellung „bei gemeldetem MMI-Fehler SMS-Weiterleitung stoppen“
ergänzt werden; Standard ist sie nicht.

## Migrations- und UI-Schritte

- Bestehende gespeicherte aktive Weiterleitungen erhalten beim Update `NOT_CHECKED`, nicht rückwirkend
  `CONFIRMED_SUCCESS`. `NOT_CHECKED` erzeugt bewusst **keine** Warnung — der Zustand ist Folge der Migration, nicht
  eines beobachteten Problems.
- Home-Screen zeigt bei aktivierter Weiterleitung klein den Nachweisgrad: „Angenommen” / „Bestätigt” /
  „Unbekannt” / „Fehler gemeldet”.
- Bei `USER_REPORTED_FAILURE`, `DIAL_FAILED`, `UNKNOWN_NO_RESPONSE` oder `watchdogExpired` dauerhafte Warnung, bis
  erneuter Versuch oder bewusste Bestätigung erfolgt.
- Während `DIALING` zeigt die Statuskarte den laufenden Vorgang; Aktivieren/Deaktivieren sind gesperrt.
- Bei Deaktivierung mit `ASSUMED_SUCCESS`: dauerhafte Warnung „Rufumleitung im Netz nicht bestätigt“ mit
  Statusabfrage-Shortcut.

## Testbarkeit

Aktuell existieren keine Unit-Tests (nur die generierten Platzhalter). Sieben rein manuelle Abnahmekriterien machen
die Zustandslogik nicht regressionssicher. Deshalb:

- Die Zustandsmaschine (Vorgang + Ergebnis + Evidenz → neuer Zustand) wird als Android-freie Klasse extrahiert und
  mit JVM-Unit-Tests abgedeckt: beide Watchdogs mit ihren unterschiedlichen Konsequenzen, USSD-Timeout →
  `UNKNOWN_NO_RESPONSE`, verspätetes Callback mit fremder ID, Sperre bei `DIALING`, Auflösung eines beim Neustart
  geladenen `DIALING`-Vorgangs, harte 60-s-Grenze, ACTIVATE- und DEACTIVATE-Pfad.
- Nur Wählweg, SIM-Auswahl und Dialer-Verhalten bleiben Gerätetests.

### Abnahmekriterien (Gerät)

- A1-Gerät/SIM, Aktivierungscode mit `**` am Ende: Anruf startet auf der richtigen SIM; SMS-Weiterleitung wird ohne
  Eingabe aktiv.
- Kein Mobilfunk / keine Berechtigung / kein Telecom-Dienst: kein Aktivieren, Status `DIAL_FAILED`.
- Dialer kehrt nicht zurück oder Call-State bleibt aus: App hängt nicht; nach Watchdog liegt ein nachvollziehbarer
  Status vor.
- Laufender regulärer Anruf beim Auslösen: nach 30 s bricht der Vorgang mit `DIAL_FAILED` ab; es wird **kein**
  MMI-Anruf in das laufende Gespräch hinein gewählt und die SMS-Weiterleitung nicht aktiviert.
- App wird während des Dialer-Anrufs vom System beendet: Vorgang wird beim Neustart wiederhergestellt und aufgelöst,
  kein doppeltes Aktivieren, keine dauerhaft blockierende `DIALING`-Sperre.
- Zwei schnelle Klicks auf Aktivieren/Deaktivieren: nur ein MMI-Anruf wird ausgelöst.
- Nutzer meldet Fehler: Status wechselt zuverlässig zu `USER_REPORTED_FAILURE`; die SMS-Weiterleitung läuft weiter.
- USSD-Code (generische Konfiguration): Callback-Erfolg/-Fehler ändert den Status korrekt; ohne Callback greift nach
  30 s der Watchdog mit `UNKNOWN_NO_RESPONSE` und der Fortschrittsdialog schließt.
- Dual-SIM: ausgewählter `PhoneAccountHandle` wird auf den Zielgeräten tatsächlich genutzt.
- Logs enthalten keine unmaskierte Zielrufnummer.

## Umsetzungsreihenfolge

1. `MmiCodeConfig` mit `MmiExecutionMode`; die drei `endsWith("#")`-Stellen entfernen.
2. `MmiOperation` mit ID, Zustand und Persistenz; `resolvePendingForwardingResult` auf ID-Abgleich umstellen;
   `DIALING`-Sperre inklusive garantiertem Ausgang.
3. Beide Watchdogs mit ihren unterschiedlichen Konsequenzen; alle `first { IDLE }` kapseln und den Rückgabewert
   auswerten.
4. Sofort-Aktivierung nach `placeCall`; `MmiConfirmationDialog` und `MMI_TIMEOUT_SUCCESS` entfernen;
   USSD-Timeout auf `UNKNOWN_NO_RESPONSE`.
5. Statuskarte mit Nachweisgrad und „Fehler melden“.
6. `maskMmiCode` und Umstellung der Logaufrufe.
7. Unit-Tests der Zustandsmaschine.

Schritte 1–4 sind die funktionale Korrektur und ergeben für sich genommen eine auslieferbare Version. Schritte 5–7
sind Transparenz, Datenschutz und Absicherung.

## Offene Punkte

1. ~~Ist sofortiges Aktivieren fachlich vertretbar?~~ **Ja** — begründet unter „Fachliche Entscheidung“: die
   SMS-Weiterleitung ist technisch unabhängig vom MMI-Erfolg.
2. ~~Fehlermeldung: nur markieren oder stoppen?~~ **Nur markieren** — Begründung unter „Umgang mit einer
   Fehlermeldung“.
3. ~~Reichen 30–60 Sekunden für „Fehler melden“?~~ **Entschieden: acht Sekunden** — die große Statuskarte
   verschwindet danach ohne Eingabe. Der Nachweisgrad bleibt im Hintergrund und in der Foreground-Benachrichtigung
   erhalten; der Arbeitsablauf wird nicht blockiert.
4. ~~Welche Audit- und Aufbewahrungsvorgaben gelten für Zielrufnummern und MMI-/USSD-Antworten?~~
   **Entschieden:** lokaler, maskierter Audit-Ringpuffer für höchstens 30 Tage; vollständige Zielrufnummern und
   rohe Netzantworten gehören nicht in das App-Audit, sondern gegebenenfalls in das freigegebene
   Fall-/Aktenverfahren.
5. **Offen (Entscheidung durch Auftraggeber):** Auf welchen konkreten Geräten und Android-Versionen muss die
   SIM-Auswahl verbindlich getestet werden? Ohne diese Liste bleibt Kriterium „Dual-SIM“ nicht abnehmbar.

## Verbindliche manuelle Abnahme vor Auslieferung

Die technische Umsetzung ersetzt keine Netz- und Geräteabnahme. Vor der Auslieferung wird je Zielgerät und
Android-Version dokumentiert geprüft:

1. A1-Sprach-MMI aktivieren und deaktivieren, jeweils mit der vorgesehenen SIM.
2. Generische `#`-Konfiguration aktivieren, deaktivieren und Status abfragen; der USSD-Callback muss sichtbar
   verarbeitet werden.
3. Dual-SIM: abweichende Standard-Sprach-SIM und explizit konfigurierte MMI-SIM testen.
4. Während eines bestehenden Gesprächs aktivieren: nach 30 Sekunden darf kein zweiter Wählauftrag entstehen.
5. App während eines MMI-Vorgangs beenden und neu starten: keine dauerhafte Sperre; der Vorgang wird nach den
   Watchdog-Regeln abgeschlossen.
6. Bei fehlender Netzantwort prüfen, dass SMS-Weiterleitung weiterläuft, Nachweisgrad aber
   `UNKNOWN_NO_RESPONSE` bleibt.
7. Prüfen, dass der lokale Audit-Ringpuffer nach 30 Tagen automatisch bereinigt ist und keine unmaskierte Nummer
   oder rohe Netzantwort enthält.
