# Umsetzungsplan: SMS-Weiterleitung dauerbetriebsfest machen

> Fassung 4 — dritte Begutachtungsrunde eingearbeitet. **AP1–AP8 sind umgesetzt und am Gerät geprüft**
> (Stand 28.07.2026, Android 16). Siehe „Umsetzungsstand" und „Gerätetests" am Ende.
> Grundlage ist ein Review des Pfads `SmsReceiver` → `SmsForegroundService` → `PhoneSmsUtils.sendSmsWithSubscription`
> → `SmsSentReceiver` / `SmsDeliveredReceiver` inklusive `AndroidManifest.xml`, `BootReceiver`, `AppContainer` und `EmailSender`.

## Leitlinie

**Der Betrieb ist automatisch. Der Nutzer konfiguriert einmal und wird danach nur noch informiert, nie gefragt.**
Jede Stelle, an der eine Entscheidung nötig ist, wird in diesem Dokument deterministisch entschieden statt an die
Oberfläche delegiert. Wo eine Regel Nachteile hat, werden sie benannt und bewusst in Kauf genommen.

## Ziel

1. Die Weiterleitung funktioniert nach 6, 24 und 72 Stunden Laufzeit **und nach einem Geräteneustart** ohne
   Benutzerinteraktion genauso wie in der ersten Minute.
2. Jede empfangene SMS hat jederzeit einen persistierten, eindeutigen Zustand. Nichts verschwindet kommentarlos.
3. Die Zustellsemantik ist explizit festgelegt und in ihren Grenzen benannt (siehe „Zustellsemantik").
4. Der Ausfall eines Weiterleitungskanals (E-Mail/SMTP) legt den anderen Kanal (SMS) nicht lahm.
5. Zustände, die die Weiterleitung beeinträchtigen, sind beim nächsten Öffnen der App sichtbar — **ohne** dass ihr
   Eintreten die Weiterleitung selbst stoppt.

## Nicht-Ziele

- RCS und MMS bleiben ausserhalb des Funktionsumfangs (`rcs.md`).
- Kein Umbau auf Clean Architecture / Hilt / Room um seiner selbst willen.
- Keine Änderung am MMI/USSD-Teil (`plan.md`).
- **Weder „genau einmal" noch „mindestens einmal".** Beides ist mit `SmsManager` nicht erreichbar. Es gibt ein
  unvermeidbares Zeitfenster, in dem nach einem Prozessverlust nicht feststellbar ist, ob gesendet wurde. Der Plan
  entscheidet sich für dieses Fenster gegen den Neuversand — und macht bei mehrteiligen Nachrichten eine bewusst
  benannte Ausnahme davon. Begründung und Preis stehen unten.

---

## Änderungen in Fassung 4

| Punkt der Zweitmeinung | Prüfergebnis | Konsequenz |
| --- | --- | --- |
| `RETRY` ist nicht allgemein duplikatfrei | **Bestätigt — das Dokument widersprach sich selbst.** Die Multipart-Begründung sagte korrekt „doppelte Segmente sind das kleinere Übel", die Abgrenzung zu `UNKNOWN` behauptete zugleich „Neuversand ist dort duplikatfrei". Beides kann nicht stimmen. Zusätzlich zutreffend: `RESULT_ERROR_GENERIC_FAILURE` ist ein Sammelcode und belegt nicht, dass nichts hinausging. | Semantik neu gefasst als **Regel mit einer benannten Ausnahme**: Ein negativer Callback belegt den Fehlschlag *dieses Teils*. Bei einteiligen Nachrichten ist der Neuversand der vorgesehene Automatismus (mit benanntem Restrisiko bei `GENERIC_FAILURE`); bei mehrteiligen kann er bereits bestätigte Teile duplizieren — bewusst in Kauf genommen, nicht mehr als duplikatfrei ausgegeben. |
| `ATTEMPTING` → `FAILED` bei „Exception aus dem Sendeaufruf" ist zu stark | **Bestätigt.** Eine Exception beweist nicht allgemein, dass keine Übergabe stattfand — etwa `DeadObjectException` aus einem teilweise abgearbeiteten Binder-Call. | Zwei Änderungen: Alle Vorbedingungen (Berechtigung, Zielnummer, Text, `SmsManager`-Beschaffung) werden **vor** dem `commit()` von `ATTEMPTING` geprüft. Danach führt nur noch eine explizit als Vorbedingungsverletzung klassifizierte Exception zu `FAILED`; **jede andere Exception ergibt `UNKNOWN`.** |
| Queue-Korruption darf nicht nur geloggt werden | **Zugestimmt** — widerspricht sonst Ziel 2. | AP5.1 verlangt einen persistenten, in der Oberfläche sichtbaren und vom Nutzer quittierbaren Korruptionswarnzustand — abgelegt **ausserhalb** der Queue-Datei, da diese im Ereignisfall gerade unlesbar ist. |

<details>
<summary>Änderungen in Fassung 3 (zweite Begutachtungsrunde, bereits eingearbeitet)</summary>

| Punkt der Zweitmeinung | Prüfergebnis | Konsequenz |
| --- | --- | --- |
| Die `HANDED_OVER`-Regel schliesst das Duplikatfenster nicht | **Bestätigt — mein Fehler.** In Fassung 2 wurde `HANDED_OVER` *nach* dem `SmsManager`-Aufruf geschrieben. Stirbt der Prozess zwischen erfolgreichem Aufruf und dem Schreiben, bleibt `QUEUED` stehen. Die Regel „`QUEUED` = sicher nicht gesendet → Retry" erzeugt dann genau die Dublette, die sie verhindern sollte. | Neuer Zustand `ATTEMPTING`, **vor** dem Aufruf geschrieben. Beim Auffinden nach einem Neustart wird er zu `UNKNOWN` ohne Neuversand. Die Semantik heisst jetzt korrekt „Neuversuch nur bei bewiesenem Nichtsenden" statt „mindestens einmal". |
| „Persistieren" muss synchron und haltbar sein | **Bestätigt.** `apply()` schreibt asynchron; bei `am kill` oder LMK-Kill kann der Schreibvorgang verloren gehen. Ein In-Memory-Mutex serialisiert nur, er macht nichts haltbar. | AP5.1 schreibt `commit()` für **jeden** Zustandsübergang verbindlich vor, und zwar vor dem jeweiligen Seiteneffekt. Fehlschlagendes `commit()` verhindert den Sendeversuch. |
| B13-Wirkung zu absolut formuliert | **Zugestimmt.** Der Plattformverstoss ist belegt, die Folge „danach sicher tot bis zum Öffnen" folgt daraus nicht — ein späterer SMS-Broadcast kann über die temporäre Allowlist einen Start ermöglichen. | B13 umformuliert: Der **garantierte** Autostart fällt aus, das Verhalten danach ist unbestimmt und wird in AP0 gemessen. |
| AP7 behauptet Selbstheilung | **Zugestimmt**, konsistent zum vorigen Punkt. | Formulierung auf „eingehende SMS löst einen Neustart**versuch** aus" geändert; keine Zusicherung. |
| Callback-Korrelation richtig, Request-Code muss nicht kollisionsfrei sein; explizites Intent empfohlen | **Zugestimmt.** Die Daten-URI ist Teil der PendingIntent-Identität, der Request-Code ebenfalls — Kollisionsfreiheit ist damit keine eigene Anforderung mehr. | AP5.2 entschärft die Request-Code-Anforderung und ergänzt `setClass()` auf den jeweiligen Receiver. |
| `UNKNOWN` braucht einen expliziten Auslöser | **Bestätigt** — Fassung 2 nannte den Zustand, aber nicht den Erzeuger. | AP5.3 ergänzt einen periodischen Ablauf-Scan im Service, getrennt vom E-Mail-Retry. |

</details>

<details>
<summary>Änderungen in Fassung 2 (erste Begutachtungsrunde, bereits eingearbeitet)</summary>

- Zustellsemantik überhaupt erst explizit gemacht; „genau einmal" als nicht erreichbar verworfen.
- Multipart: `expectedParts` / `confirmedParts` / `failedParts`, `SENT` erst bei Vollzähligkeit.
- Testmatrix korrigiert: `am kill` für Prozessverlust, `force-stop` als eigener Test mit dem Ergebnis „ruht bis Nutzerstart".
- B11 umgekehrt: Der Service stoppt sich ohne `POST_NOTIFICATIONS` nicht mehr selbst (E12).
- AP0 mit vollständigem `device_config`-Befehl inklusive Zurücksetzen (E4).
- „Unbegrenzte Laufzeit" → „kein dokumentiertes Zeitlimit"; Variante A als Risikoabwägung statt als Beweis.
- B14 neu: PendingIntent-Identität und prozesslokaler Request-Code-Zähler.
- Queue in eigener verschlüsselter Datei **mit** neuem Backup-Exclude in beiden XML-Dateien.
- B13 neu: `dataSync` aus `BOOT_COMPLETED` bei `targetSdk ≥ 35` verboten (E11).
- B12: README korrigieren statt Watchdog bauen.
</details>

---

## Evidenzbasis

| # | Regel | Quelle |
| --- | --- | --- |
| E1 | „The system permits an app's `dataSync` services to run for a total of 6 hours in a 24-hour period, after which the system calls the running service's `Service.onTimeout(int, int)` method." | [Behavior changes: Android 15](https://developer.android.com/about/versions/15/behavior-changes-15) |
| E2 | „If the service does not call `Service.stopSelf()`, the system throws an internal exception" — `RemoteServiceException: … did not stop within its timeout`. | ebd. |
| E3 | „you cannot start another `dataSync` foreground service _unless_ the user has brought your app to the foreground" → `ForegroundServiceStartNotAllowedException`. | ebd. |
| E4 | Timer läuft im Hintergrund weiter, Reset nur durch Vordergrund. Testbar über `adb shell device_config put activity_manager data_sync_fgs_timeout_duration <ms>`. Keine dokumentierte Ausnahme für Apps ohne Batterieoptimierung. | [Foreground service timeouts](https://developer.android.com/develop/background-work/services/fgs/timeout) |
| E5 | Zeitlimits nur für `dataSync`, `mediaProcessing` (je 6 h/24 h) und `shortService` (ca. 3 min). Für `specialUse` ist **kein** Zeitlimit dokumentiert. | [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types) |
| E6 | `specialUse` erfordert `FOREGROUND_SERVICE_SPECIAL_USE` und ein `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE">`; Prüfung **bei Einreichung in der Play Console**. | ebd. |
| E7 | Der SMS-Empfang steht **nicht** in der dokumentierten Ausnahmeliste für FGS-Start aus dem Hintergrund. | [Restrictions on starting FGS from the background](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start) |
| E8 | Eine App kommt temporär auf die Power-Allowlist, wenn sie „a broadcast, such as an SMS/MMS message" verarbeitet; „While an app is on the allowlist, it can launch services without limitation". | [Background Execution Limits](https://developer.android.com/about/versions/oreo/background) |
| E9 | `goAsync()`: „the system expects you to finish with the broadcast very quickly (under 10 seconds)". | [Broadcasts](https://developer.android.com/develop/background-work/background-tasks/broadcasts) |
| E10 | Expedited Work unterliegt einem Kontingent; bei Erschöpfung wird die Arbeit verworfen oder als **normale, verzögerbare** Arbeit ausgeführt. | [Define work](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) |
| E11 | Bei `targetSdk ≥ 35` darf ein `BOOT_COMPLETED`-Receiver folgende Typen **nicht** starten: `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection`, `microphone`. `specialUse` steht nicht auf der Liste. | [Behavior changes: Android 15](https://developer.android.com/about/versions/15/behavior-changes-15) |
| E12 | „Apps don't need to request the `POST_NOTIFICATIONS` permission in order to launch a foreground service." Bei Verweigerung bleibt der Dienst „in the Task Manager" sichtbar, nur nicht im Drawer. | [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission) |

Projektspezifische Randbedingungen:

- `app/build.gradle.kts:71` → `targetSdk = 36`, `minSdk = 29`. E1–E4 und E11 greifen **heute**.
- `AndroidManifest.xml:65` → `android:foregroundServiceType="dataSync"`.
- `docs/GOOGLE_PLAY_CHECKLIST.md:5`, `README.md:83` → Verteilung als signierte APK über GitHub Releases, nicht über
  Google Play. Damit entfällt das Review-Risiko aus E6.
- Weder WorkManager noch Room noch AlarmManager im Projekt. Vorhanden: `androidx.security.crypto`, dauerhafter FGS.
- `backup_rules.xml` / `data_extraction_rules.xml` schliessen `sms_forwarder_secure_prefs.xml` und `logs` aus,
  inkludieren aber über `<include domain="sharedpref" path="."/>` alle übrigen SharedPreferences.

---

## Befundlage

| # | Befund | Fundstelle | Wirkung im Dauerbetrieb |
| --- | --- | --- | --- |
| B13 | Der `BootReceiver` startet einen `dataSync`-FGS. Bei `targetSdk ≥ 35` aus `BOOT_COMPLETED` **verboten** (E11). | `BootReceiver.kt:57`, `AndroidManifest.xml:65` | **Der garantierte Autostart nach einem Neustart fällt aus.** Ob eine später eintreffende SMS den Dienst über die temporäre Allowlist (E8) doch noch startet, ist unbestimmt und wird in AP0 gemessen. In jedem Fall ist der Bootpfad defekt. |
| B1 | `dataSync`-FGS läuft nach 6 h in den Timeout; `onTimeout()` nicht implementiert. | `AndroidManifest.xml:65`, `SmsForegroundService.kt` | Totalausfall nach 6 h plus `RemoteServiceException`; danach schlägt jeder neue FGS-Start fehl (E3) → `SmsReceiver.kt:191` landet im Catch, SMS verloren. |
| B2 | SMS- und E-Mail-Zweig als Geschwister in einem `coroutineScope`; E-Mail wirft bei jedem SMTP-Fehler. | `:418-511`, Rethrow `:924` | Ein schneller SMTP-Fehler cancelt den noch suspendierten SMS-Zweig (`:476`). |
| B3 | Der Retry wiederholt die **gesamte** Gruppe, obwohl `shouldRetry` nur E-Mail-Fehler akzeptiert. | `:550`, `:564`, `:630` | Bis zu 3 zusätzliche kostenpflichtige SMS nach einem reinen SMTP-Ausfall. |
| B4 | Echte SMS-Sendefehler werden nur geloggt. | `SmsSentReceiver.kt:43-107` | Funkloch = endgültiger, stiller Verlust. |
| B5 | Keine Persistenz: Nutzdaten, `retryCounter`, `activeRetryJobs` nur im Prozess. | `:88-92` | Prozess-Kill während der Verarbeitung = Verlust; `START_STICKY` liefert keinen Intent nach. |
| B6 | Absender wird aus dem Gruppierungs-Key rückgeparst. | `:363-369` | Absender mit `_` (GSM-7 zulässig, z. B. `MY_BANK`) werden abgeschnitten — im Text **und** in der Loop-Protection. |
| B7 | `onReceive` ohne umschliessendes `try/catch`; synchrone Binder-Calls. | `SmsReceiver.kt:108-149` | Crash = stiller Verlust plus ANR-Risiko im 10-s-Fenster (E9). |
| B8 | `serviceScope.launch { … }` ohne `CoroutineExceptionHandler`. | `:262` | Exceptions ausserhalb des inneren `try` reissen den Prozess mit. |
| B9 | Nachrichten > 1530 Zeichen werden verworfen statt gekürzt. | `:778` | Vollständiger Verlust statt Teilzustellung. |
| B10 | `"SIM: Slot $subscriptionId"` verwechselt Subscription-ID mit Slot-Index. | `:670` | Falsche Angabe im weitergeleiteten Text. |
| B11 | Ohne `POST_NOTIFICATIONS` **stoppt sich der Service selbst**, obwohl die Plattform das nicht verlangt (E12). | `:137-152`, `BootReceiver.kt:40-53` | Selbstverschuldeter Totalausfall bei weggeklickter Berechtigungsabfrage. |
| B12 | Das im README beworbene „Heartbeat-Monitoring" existiert im Quelltext nicht. | `README.md` | Erwartungshaltung ohne Gegenstück. |
| B14 | PendingIntent-Korrelation nur über Extras; Request-Code aus prozesslokalem Zähler ab 0. | `PhoneSmsUtils.kt:61`, `:422-494` | Extras zählen nicht zur PendingIntent-Identität; nach Prozessneustart Fehlzuordnung von Callbacks. Blockiert AP5. |

---

## Architekturentscheidung

**Variante A — Dauer-FGS beibehalten, Typ auf `specialUse` wechseln.** ✅

- Für `specialUse` ist **kein Zeitlimit dokumentiert** (E5) und der Typ steht **nicht** auf der
  BOOT_COMPLETED-Sperrliste (E11). Eine Manifest-Änderung behebt B1 und B13 gemeinsam. Das ist keine Zusicherung
  unbegrenzter Laufzeit: Nutzer-Stopp über den Task Manager, aggressive OEM-Prozessverwaltung und Prozesskill unter
  Speicherdruck bleiben möglich — dagegen hilft AP5, nicht AP1.
- Das übliche Gegenargument (Play-Console-Prüfung, E6) greift hier nicht, weil per GitHub-Release verteilt wird.
- Die Typwahl ist inhaltlich ehrlich: `dataSync` beschreibt die Tätigkeit falsch — es wird nichts synchronisiert.
- Der laufende FGS ist der Grund, warum der Start aus `SmsReceiver` heute funktioniert: Läuft der Dienst bereits im
  Vordergrund, greift die Background-Start-Restriction nicht. Ohne Dauer-FGS müsste **jeder** SMS-Empfang einen FGS
  aus dem Hintergrund starten; der SMS-Empfang steht nicht in der Ausnahmeliste (E7) und trägt dort nur über die
  temporäre Allowlist (E8).

**Ehrliche Einordnung:** Der letzte Punkt ist eine Risikoabwägung, kein Nachweis, dass Variante B auf aktuellen
Geräten scheitert — sie funktioniert bei anderen Apps in der Praxis. Variante A tauscht ein belegtes, sicher
eintretendes Problem gegen keines; Variante B gegen ein undokumentiertes, geräteabhängiges Restrisiko bei deutlich
grösserem Umbauumfang.

**Variante B (kein Dauer-FGS, `goAsync()` + WorkManager)** — verworfen: FGS-Start-Risiko pro SMS (E7/E8), Degradierung
von Expedited Work zu verzögerbarer Arbeit (E10), Wegfall der Statusnotification, grösster Umfang.
**Variante C (`shortService` pro SMS)** — verworfen: gleiche Background-Start-Frage, dazu harte Beendigungspflicht und
keine Dauernotification.

---

## Zustellsemantik

**Festlegung: automatischer Neuversuch nur dort, wo ein Fehlschlag belegt ist. Wo jede Aussage fehlt, kein
Neuversuch.** Vollständig automatisch, ohne Rückfrage an den Nutzer. **Eine benannte Ausnahme** — mehrteilige
Nachrichten mit Teilerfolg — ist unten begründet.

`SmsManager` erlaubt keine saubere Garantie in beide Richtungen. Zwischen dem Sendeaufruf und dem `sentIntent`-Callback
kann der Prozess sterben, und beim Neustart ist aus dem persistierten Zustand allein nicht ableitbar, ob das
Telefonie-Framework die Nachricht bereits angenommen hat. Genau ein Übergang lässt sich absichern: Der Zustand
`ATTEMPTING` wird **vor** dem Sendeaufruf mit `commit()` geschrieben. Danach gilt:

- Ein vorgefundenes `QUEUED` beweist, dass der Sendeaufruf **nicht** stattgefunden hat → Neuversuch ist duplikatfrei.
- Ein vorgefundenes `ATTEMPTING` ist mehrdeutig → **kein** Neuversuch.

**Der Preis dieser Wahl, ausdrücklich:** Stirbt der Prozess zwischen dem `commit()` von `ATTEMPTING` und dem
Sendeaufruf, wird eine SMS nicht weitergeleitet, obwohl ein Neuversuch sicher gewesen wäre. Dieses Fenster ist wenige
Anweisungen breit, aber real. Die Alternative — im Zweifel senden — würde im deutlich häufigeren Fall (Prozesstod
*nach* erfolgreicher Übergabe) Dubletten erzeugen. Der Plan bevorzugt den seltenen Verlust gegenüber dem häufigeren
Doppelversand und macht beide Fälle in der Oberfläche sichtbar.

| Zustand | Bedeutung | Automatische Regel |
| --- | --- | --- |
| `QUEUED` | Vorgang samt Zieldaten und `expectedParts` persistiert; Sendeaufruf nachweislich noch nicht erfolgt. | Neuversuch, duplikatfrei. |
| `ATTEMPTING` | Unmittelbar vor dem Sendeaufruf geschrieben. | Im laufenden Prozess Übergangszustand. Nach einem Neustart vorgefunden → `UNKNOWN`. |
| `HANDED_OVER` | Sendeaufruf ohne Exception zurückgekehrt. | Warten auf Callbacks; Ablaufprüfung nach 15 min. |
| `SENT` | `confirmedParts == expectedParts`, alle `RESULT_OK`. | Terminal. Nach Aufbewahrungsfrist löschen. |
| `RETRY` | Mindestens ein Teil meldet per Callback einen transienten Fehler (`RESULT_ERROR_NO_SERVICE`, `RESULT_ERROR_RADIO_OFF`, `RESULT_ERROR_GENERIC_FAILURE`). | Vollständiger Neuversand, max. 3 Versuche, Backoff 30 s / 2 min / 10 min, zusätzlicher Anstoss bei jedem Service-Start. Duplikatrisiko siehe unten. |
| `FAILED` | Retries erschöpft, terminaler Fehler (`RESULT_ERROR_NULL_PDU`) oder eine **explizit als Vorbedingungsverletzung klassifizierte** Exception aus dem Sendeaufruf. | Terminal, sichtbar. |
| `UNKNOWN` | Jede fehlende oder mehrdeutige Aussage: `ATTEMPTING` nach Neustart vorgefunden, `HANDED_OVER` 15 Minuten ohne vollständige Rückmeldung, **oder eine nicht eindeutig vor der Übergabe entstandene Exception**. | **Kein Neuversand.** Sichtbar in der App. |

**Warum `RETRY` neu sendet, `UNKNOWN` aber nicht:** Ein negativer Callback belegt den Fehlschlag **dieses Teils** —
das ist eine positive Aussage des Frameworks. `UNKNOWN` ist gerade das Fehlen jeder Aussage. Die Unterscheidung ist
qualitativ, nicht graduell.

**Wo der Neuversand trotzdem duplizieren kann — zwei benannte Fälle:**

1. **Mehrteilige Nachricht mit Teilerfolg.** Hat Teil 1 `RESULT_OK` und Teil 2 einen transienten Fehler, wird der
   gesamte Vorgang neu versandt und Teil 1 kann beim Empfänger doppelt ankommen. Das ist die **bewusste Ausnahme**
   von der Leitregel: Eine unvollständige mehrteilige SMS wird vom Empfängergerät nicht zusammengesetzt und ist
   wertlos; einzelne Segmente lassen sich über `sendMultipartTextMessage` nicht gezielt nachsenden. Ein verwaistes
   Fragment plus eine vollständige Nachricht ist besser als eine Nachricht, die nie lesbar wird.
2. **`RESULT_ERROR_GENERIC_FAILURE`.** Das ist ein Sammelcode; er belegt einen Fehler, aber nicht, dass die Nachricht
   das Gerät sicher nicht verlassen hat. Er wird trotzdem als Retry-Grund geführt, weil er der mit Abstand häufigste
   transiente Fehler ist und ein Ausschluss den wichtigsten Wiederholungsfall entwerten würde. Das Restrisiko einer
   Dublette wird hier bewusst getragen.

Für **einteilige** Nachrichten mit `NO_SERVICE` oder `RADIO_OFF` — dem Regelfall — ist der Neuversand nach heutigem
Kenntnisstand duplikatfrei.

**Abgrenzung:** `SENT` heisst „vom Netz angenommen", nicht „beim Empfänger angekommen". Letzteres liefert nur der
optionale Delivery-Report, den viele Netze nicht oder verzögert senden. `SmsDeliveredReceiver` bleibt rein informativ
und steuert **keine** Zustandsübergänge.

---

## Arbeitspakete

### AP0 — B1 und B13 empirisch bestätigen (vor jeder Codeänderung)

```
adb shell device_config put activity_manager data_sync_fgs_timeout_duration 120000
# App im Hintergrund laufen lassen, SMS senden, Logcat beobachten
adb shell device_config delete activity_manager data_sync_fgs_timeout_duration   # Ausgangswert wiederherstellen
```
Erwartet: (a) `RemoteServiceException … did not stop within its timeout`, (b) danach
`ForegroundServiceStartNotAllowedException` beim nächsten SMS-Empfang.

Für B13 zusätzlich: Gerät mit der aktuellen `dataSync`-Deklaration neu starten. Erwartet wird
`ForegroundServiceStartNotAllowedException` aus `BootReceiver.kt:57`. **Danach ohne Öffnen der App eine SMS schicken
und protokollieren, ob der Dienst über die temporäre Allowlist (E8) doch startet.** Das Ergebnis entscheidet, wie
gravierend B13 im Feld ist, ändert aber nichts an der Behebung.
**Aufwand:** klein. **Risiko:** keines — das Flag wird zurückgesetzt.

### AP1 — FGS-Typ auf `specialUse`, `onTimeout()` implementieren  *(behebt B1, B13)*

- `AndroidManifest.xml`: `FOREGROUND_SERVICE_DATA_SYNC` → `FOREGROUND_SERVICE_SPECIAL_USE`;
  `android:foregroundServiceType="specialUse"`; `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"`
  mit wahrheitsgemässer Begründung.
- `SmsForegroundService`: `override fun onTimeout(startId, fgsType)` → protokollieren, Warnzustand persistieren,
  `stopSelf()`. Bleibt auch ohne dokumentiertes Limit sinnvoll: Es verwandelt eine künftige Plattformverschärfung von
  einem Absturz in eine kontrollierte Meldung.
- `tools:ignore="ForegroundServicePermission"` (`AndroidManifest.xml:50`) entfernen, sofern entbehrlich.
**Nachweis (beide verpflichtend):** AP0-Aufbau wiederholen → kein Timeout mehr. **Und ein echter Geräteneustart** →
Dienst läuft nach dem Boot, SMS wird ohne Öffnen der App weitergeleitet.

### AP2 — SMS- und E-Mail-Zweig entkoppeln  *(behebt B2)*

`coroutineScope` → `supervisorScope`; jeder `launch`-Block bekommt sein eigenes `try/catch`.
**Begründung:** Die Kanäle sind fachlich unabhängig; die Cancellation von Geschwister-Coroutinen ist hier ein
unerwünschter Nebeneffekt der Structured Concurrency.
**Nachweis:** Unit-Test mit sofort werfendem `EmailSender`-Fake → SMS-Versand trotzdem genau einmal aufgerufen.

### AP3 — Retry auf den E-Mail-Kanal beschränken  *(behebt B3)*

`scheduleRetry` ruft eine ausschliesslich für E-Mail zuständige Wiederholung statt `processMessageGroup`.
Retry-Key aus `intentId` statt `parts.hashCode()`.
**Nachweis:** SMTP wirft dreimal → SMS-Versand genau einmal, E-Mail-Versuche viermal.

### AP4 — Härtung des Empfangspfads  *(behebt B6, B7, B8, B9, B10)*

- Absender aus `parts.first().sender` statt aus dem Gruppierungs-Key (B6).
- `onReceive` vollständig in `try/catch`; `getAllSimInfo()` je Broadcast einmal ermitteln (B7, E9).
- `CoroutineExceptionHandler` am `serviceScope` (B8).
- Überlange Nachrichten kürzen und markieren statt verwerfen (B9).
- `slotIndex` statt `subscriptionId`; Kopfzeile kompakter (B10 — ~40 Header-Zeichen machen aus einer einteiligen SMS
  sonst eine zweiteilige).
- `isSmsIntentValid`: leere Teile filtern statt den ganzen Intent zu verwerfen.

### AP5 — Persistente Weiterleitungs-Queue  *(behebt B4, B5, B14)*

#### AP5.1 — Speicher und Haltbarkeit

Eigene verschlüsselte Datei `sms_forwarder_queue` über `EncryptedSharedPreferences`, ein JSON-Dokument als einziger Wert.

**Haltbarkeitsregel (nicht verhandelbar):** Jeder Zustandsübergang wird mit **`commit()`** geschrieben, nie mit
`apply()`. `apply()` schreibt asynchron und kann bei `am kill` oder LMK-Kill verlorengehen — genau in dem Szenario, für
das die Queue existiert. `commit()` kehrt erst nach dem synchronen Schreiben zurück. Konkret:

- `commit()` von `QUEUED` **vor** allem Weiteren.
- `commit()` von `ATTEMPTING` **vor** dem Sendeaufruf. **Schlägt dieses `commit()` fehl, unterbleibt der Sendeaufruf**
  und der Vorgang bleibt `QUEUED` — sonst entstünde genau die Mehrdeutigkeit, die die Semantik vermeiden soll.
- `commit()` bei jedem Callback-Übergang. Ein verlorener `SENT`-Übergang würde sonst zu einem falschen `RETRY` und
  damit zu einer Dublette führen.

Der Mutex serialisiert das Read-modify-write, ersetzt aber `commit()` nicht — er macht nichts haltbar.

Weiter:
- **Schemaversion** im Dokument für spätere Migrationen.
- **Defektbehandlung pro Eintrag:** unlesbare Einträge überspringen, nicht die Queue verwerfen. Ist das Dokument als
  Ganzes unlesbar, neu anlegen.
- **Korruptionswarnzustand (nicht nur Logging):** Beide Fälle setzen ein persistentes, in der Oberfläche sichtbares
  und vom Nutzer quittierbares Warnkennzeichen mit Zeitpunkt und — soweit ermittelbar — Anzahl verlorener Einträge.
  Ein reiner Logeintrag genügt nicht: Ziel 2 verlangt, dass nichts kommentarlos verschwindet, und ein
  Totalverlust der Queue ist genau der Fall, in dem das sonst passiert. **Das Kennzeichen wird ausserhalb der
  Queue-Datei abgelegt** (in den bestehenden `sms_forwarder_secure_prefs`), da die Queue-Datei im Ereignisfall gerade
  die unlesbare ist.
- **Aufbewahrung:** 50 Einträge oder 7 Tage, was zuerst greift; terminale Zustände zuerst verdrängen.
- **Backup-Ausschluss:** neuer `<exclude domain="sharedpref" path="sms_forwarder_queue.xml"/>` in
  `backup_rules.xml` **und** `data_extraction_rules.xml`. Ohne diesen Schritt landet die Datei über
  `<include domain="sharedpref" path="."/>` im Cloud-Backup — SMS-Volltexte inklusive.

**Warum nicht Room:** kleine Queue, maximal sensibler Inhalt. `androidx.security.crypto` ist bereits im Projekt; eine
verschlüsselte Preferences-Datei ist datenschutzseitig besser als eine unverschlüsselte Room-DB und braucht keine neue
Abhängigkeit. **Warum eine eigene Datei:** Trennung von Konfiguration und Laufzeitdaten — eine defekte Queue darf die
Konfiguration nicht mitreissen.

#### AP5.2 — Stabile Callback-Korrelation *(behebt B14)*

- Vorgangs-ID und Teilindex in die PendingIntent-Identität: `Intent.setData(Uri.parse("smsfwd://op/<id>/part/<n>"))`.
  Extras zählen nicht zur Identität und dürfen die Zuordnung nicht tragen.
- **Explizites Intent** via `setClass()` auf `SmsSentReceiver` bzw. `SmsDeliveredReceiver`, damit der Callback
  eindeutig innerhalb der App bleibt.
- Request-Code deterministisch aus der URI ableiten. Kollisionsfreiheit ist **keine** eigene Anforderung, da die
  Daten-URI bereits Teil der PendingIntent-Identität ist; der prozesslokale Zähler (`PhoneSmsUtils.kt:61`) entfällt
  trotzdem, weil er nach einem Neustart wieder bei 0 beginnt.
- `FLAG_IMMUTABLE` bleibt; **kein** `FLAG_UPDATE_CURRENT`, damit ausstehende Callbacks nicht überschrieben werden.

#### AP5.3 — Zustandsführung und Ablauf-Scan

- Übergänge exakt nach der Semantik-Tabelle, `expectedParts` bereits beim Anlegen aus `divideMessage()` ermitteln,
  damit `ATTEMPTING` vollständig ist.
- **Vorbedingungen vor `ATTEMPTING` prüfen:** `SEND_SMS`-Berechtigung, nicht-leere Zielnummer, nicht-leerer Text,
  erfolgreiche Beschaffung des `SmsManager`. Schlägt eine davon fehl, geht der Vorgang direkt auf `FAILED`, ohne dass
  `ATTEMPTING` je geschrieben wird.
- **Exception-Klassifikation nach `ATTEMPTING`:** Nur eine explizit als Vorbedingungsverletzung erkannte Exception
  (z. B. `IllegalArgumentException` aus der Argumentprüfung, `SecurityException` wegen fehlender Berechtigung) führt zu
  `FAILED`. **Jede andere Exception ergibt `UNKNOWN`** — etwa alles, was aus dem Binder-Aufruf selbst stammt
  (`DeadObjectException`, `RemoteException`). Eine Exception beweist für sich genommen nicht, dass keine Übergabe
  stattgefunden hat; sie als `FAILED` darzustellen wäre eine falsche Aussage und würde einen späteren manuellen
  Umgang mit dem Fall verbauen. Die Liste der als `FAILED` behandelten Typen steht im Code als benannte Konstante, nicht
  als verstreute `catch`-Zweige.
- `SmsSentReceiver` zählt `confirmedParts` / `failedParts` und führt die Übergänge aus.
- **Ablauf-Scan (eigener Mechanismus, nicht der E-Mail-Retry):** Der dauerhaft laufende Dienst prüft beim Start und
  danach alle 5 Minuten alle `HANDED_OVER`-Einträge auf Überschreitung der 15-Minuten-Frist und setzt sie auf
  `UNKNOWN`. Derselbe Durchlauf überführt beim Start vorgefundene `ATTEMPTING`-Einträge nach `UNKNOWN` und stösst
  fällige `RETRY`-Einträge an.
- `SmsDeliveredReceiver` bleibt rein informativ.

**Warum kein WorkManager als Retry-Träger:** E10 — bei erschöpftem Kontingent degradiert Expedited Work zu
verzögerbarer Arbeit. Nach AP1 läuft ohnehin ein Dienst ohne dokumentiertes Zeitlimit.
**Risiko:** höchstes Paket im Plan, eigener PR mit eigener Testmatrix.

### AP6 — Ausfallzustände sichtbar machen, ohne den Betrieb zu stoppen  *(behebt B11)*

**Kehrt das heutige Verhalten um:** Fehlt `POST_NOTIFICATIONS`, **läuft der Dienst weiter**.
- `setupService()` ruft kein `stopSelf()` mehr, sondern startet den Foreground Service regulär.
- `BootReceiver` startet den Dienst auch ohne die Berechtigung.
- Der Zustand wird persistiert und beim nächsten Öffnen als Warnhinweis angezeigt („Statusanzeige unterdrückt —
  Weiterleitung läuft, ist aber nur im Task Manager sichtbar").

**Begründung:** Die Plattform verlangt `POST_NOTIFICATIONS` für einen FGS nicht (E12). Das bisherige Selbst-Stoppen
erzeugt einen Totalausfall, den Android gar nicht erzwingt, und trifft genau die Nutzer, die eine Berechtigungsabfrage
wegklicken. **Nebeneffekt:** Der bisherige Pfad (`startForegroundService()` ohne folgendes `startForeground()`) barg
zusätzlich das Risiko einer `ForegroundServiceDidNotStartInTimeException`; er entfällt.

Ebenfalls hier: Warnzustände für aktive Batterieoptimierung, `FAILED`/`UNKNOWN`-Einträge, den Queue-Korruptionsfall
aus AP5.1 und den `onTimeout()`-Fall — alle als Anzeige, keiner als Blockade.

### AP7 — README korrigieren  *(behebt B12)*

Die Behauptung „Heartbeat-Monitoring" wird entfernt. Ein zusätzlicher Watchdog wird **nicht** gebaut: Ein
AlarmManager-Wächter wäre nach `force-stop` selbst tot, und nach AP1 gibt es keinen bekannten Pfad mehr, auf dem der
Dienst unbemerkt endet. Dokumentiert wird stattdessen — bewusst ohne Zusicherung —, dass `START_STICKY`, der
`BootReceiver` und **jeder eingehende SMS-Broadcast einen Neustartversuch auslösen** (`SmsReceiver.kt:176-194`). Ob
dieser Versuch aus dem Hintergrund gelingt, ist nicht allgemein zugesichert (E7/E8) und wird nicht als Selbstheilung
behauptet. Ergänzt wird ein Absatz zu den echten Systemgrenzen: „Stopp erzwingen" und Task-Manager-Stopp setzen die
Weiterleitung bis zum nächsten manuellen Start aus.

### AP8 — Testabdeckung

Die Weiterleitungsentscheidung wandert — analog zu `ForwardingResolutionReducer` und `MmiOperationReducer` — in eine
Android-freie, auf der JVM testbare Einheit: Gruppierung, Absenderermittlung, Loop-Protection, Kürzungsregel,
Retry-Entscheidung und **die vollständige Zustandsmaschine aus AP5**. `SmsForegroundService` behält nur die
Plattformanbindung. Die Zustandsmaschine wäre ohne JVM-Tests nicht verantwortbar.

---

## Reihenfolge und Schnitt in PRs

| PR | Inhalt | Begründung |
| --- | --- | --- |
| 1 | AP0 + AP1 | Zwei Blocker (B1, B13) mit einer Manifest-Änderung; klein und isoliert prüfbar. |
| 2 | AP2 + AP3 | Gemeinsamer Umbau der Fehlerbehandlung. |
| 3 | AP4 + AP6 | Härtung und Sichtbarkeit, ohne Zustandsmaschine testbar. AP6 ist die kleinste Änderung mit der grössten Wirkung auf die Ausfallwahrscheinlichkeit im Feld. |
| 4 | AP5 + AP8 (Zustandsübergänge) | Grösstes Paket, eigener PR mit eigener Testmatrix. |
| 5 | AP7 + Rest AP8 | Dokumentation und verbleibende Tests. |

## Testmatrix

| Szenario | Vorgehen | Erwartung |
| --- | --- | --- |
| Dauerbetrieb | 24 h, App nie geöffnet | Durchgehend aktiv, kein `RemoteServiceException` |
| Geräteneustart | echter Reboot, App nicht öffnen | Dienst läuft, SMS wird weitergeleitet (B13) |
| SMTP tot | Server unerreichbar | SMS geht raus, genau einmal; E-Mail 3× erneut versucht |
| Funkloch | Flugmodus beim Eintreffen | `RETRY`; nach Netzrückkehr genau eine Zustellung |
| Prozessverlust vor dem Senden | `am kill` bei injiziertem Halt in `QUEUED` | Neuversuch, genau eine Zustellung |
| Prozessverlust im Sendefenster | `am kill` bei injiziertem Halt in `ATTEMPTING` | Zustand `UNKNOWN`, **kein** Neuversand, sichtbar in der App |
| Force-Stop | `am force-stop <package>` | **Erwartet: keine Verarbeitung** bis zum Nutzerstart — Systemgrenze, kein Fehler. Danach wird die Queue abgearbeitet. |
| Fehlschlagendes `commit()` | Schreibfehler injizieren | Sendeaufruf unterbleibt, Vorgang bleibt `QUEUED` |
| Mehrteilig, ein Teil scheitert | Fehler-Injektion im Callback für Teil 2 | Kein `SENT`; `RETRY`, vollständiger Neuversand. **Erwartet und dokumentiert: Teil 1 kann beim Empfänger doppelt ankommen.** |
| Exception aus dem Sendeaufruf | `DeadObjectException` injizieren | Zustand `UNKNOWN`, **nicht** `FAILED`; kein Neuversand |
| Vorbedingung verletzt | Zielnummer leeren | `FAILED` ohne vorheriges `ATTEMPTING` |
| Mehrteilig, alle Teile OK | 900 Zeichen | `SENT` erst nach dem letzten Callback; genau eine Nachricht beim Empfänger |
| Callback bleibt aus | Callback unterdrücken | Nach 15 min durch den Ablauf-Scan `UNKNOWN`, kein Neuversand |
| Absender `MY_BANK` | Test-SMS mit `_` | Absender vollständig, Loop-Protection korrekt |
| > 1530 Zeichen | überlange Nachricht | Gekürzt zugestellt mit Marker |
| Dual-SIM | Empfang SIM 2, Modus „gleiche SIM" | Versand über SIM 2, korrekter Slot in der Kopfzeile |
| `POST_NOTIFICATIONS` entzogen | verweigern, neu starten | **Weiterleitung läuft weiter**; Warnhinweis beim nächsten Öffnen |
| Queue-Eintrag beschädigt | einzelnen Eintrag manipulieren | Eintrag übersprungen, Betrieb läuft weiter, Warnkennzeichen gesetzt |
| Queue-Dokument unlesbar | ganze Datei manipulieren | Queue neu angelegt, **Warnkennzeichen in der App sichtbar** und quittierbar |
| Backup | `bmgr` Backup/Restore | Queue-Datei **nicht** enthalten |

## Verbleibende offene Punkte

1. **`UNKNOWN` ohne Neuversand** (betrifft `ATTEMPTING` nach Neustart und ausbleibende Callbacks). Die Regel bevorzugt
   den seltenen Verlust gegenüber dem häufigeren Doppelversand. Wer Vollständigkeit über Duplikatfreiheit stellt,
   dreht diesen einen Übergang um — die Zustandsmaschine bleibt sonst unverändert.
2. **Die Multipart-Ausnahme.** Bei Teilerfolg wird der gesamte Vorgang neu versandt und kann bestätigte Teile
   duplizieren. Wer Duplikatfreiheit strikt über Lesbarkeit stellt, würde stattdessen auf `FAILED` gehen und den
   Vorgang nur anzeigen. Meine Abwägung: Ein Fragment, das der Empfänger nie zusammengesetzt sieht, ist der grössere
   Schaden.
3. **Aufbewahrung 7 Tage / 50 Einträge.** Unzustellbare SMS-Volltexte liegen so lange verschlüsselt auf dem Gerät.
   Die Grenze wirkt nur auf terminale Einträge (siehe Umsetzungsstand, Punkt 5); laufende Vorgänge werden nie
   verdrängt.
4. **Kompaktere Kopfzeile.** Spart pro Weiterleitung potenziell eine zweite SMS, ändert aber ein sichtbares Format.
   Umgesetzt ist **nur** die kompakte Form (`Absender TT.MM. HH:MM SIMn`); die vorgeschlagene Option auf das alte
   Format wurde bewusst nicht gebaut, weil sie eine Einstellung erzeugt, die der Leitlinie „einmal konfigurieren"
   zuwiderläuft. Wer das alte Format braucht, holt es als eigene Entscheidung nach.

---

## Umsetzungsstand

**Umgesetzt im Quelltext:**

| AP | Wesentliche Fundstellen |
| --- | --- |
| AP1 | `AndroidManifest.xml` (`specialUse`, `FOREGROUND_SERVICE_SPECIAL_USE`, `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`), `SmsForegroundService.onTimeout()`, `ManifestForegroundServiceTest` |
| AP2 | `SmsForegroundService.processMessageGroup()` — `supervisorScope`, eigenes `try/catch` je Zweig |
| AP3 | `SmsForegroundService.handleEmailError()`, `EmailRetryPolicy` — Retry-Key aus `intentId`, kein Neuversand per SMS |
| AP4 | `SmsForwardingComposer` (Gruppierung, Absender, Kopfzeile, Kürzung), `SmsReceiver` (try/catch, einmalige SIM-Abfrage, Teilfilter), `CoroutineExceptionHandler` am `serviceScope` |
| AP5 | `ForwardingOperation`, `ForwardingState`, `SmsDeliveryReducer`, `SmsCallbackUri`, `ForwardingQueueStore` (eigene verschlüsselte Datei, `commit()`), `PhoneSmsUtils.prepareForwardSms/dispatchForwardSms`, `SmsSentReceiver`, Backup-Ausschlüsse |
| AP6 | `SmsForegroundService.setupService()` (kein `stopSelf()` mehr), `BootReceiver`, `ForwardingWarningsCard(Host)`, Warnzustände in `SharedPreferencesManager` |
| AP7 | `README.md` — „Heartbeat-Monitoring" entfernt, Abschnitte „Systemgrenzen des Dauerbetriebs" und „Zustellsemantik" ergänzt |
| AP8 | `SmsDeliveryReducerTest`, `SmsForwardingComposerTest`, `LoopProtectionPolicyTest`, `EmailRetryPolicyTest`, `ForwardingQueueRetentionPolicyTest`, `SmsCallbackUriTest`, `SmsSentReceiverClassificationTest` |

**Sieben Präzisierungen gegenüber dem Plan, bewusst so entschieden:**

1. **Versuchsnummer in der Callback-URI.** Die URI lautet `smsfwd://op/<id>/attempt/<a>/part/<n>`. Ohne die
   Versuchsnummer würden Rückmeldungen eines bereits abgeschlossenen Versuchs die Zähler des laufenden verfälschen —
   und damit genau die Dublette erzeugen, die die Semantik verhindern soll.
2. **`expectedParts` entsteht in der Vorbedingungsprüfung.** `divideMessage()` braucht den `SmsManager`, dessen
   Beschaffung selbst Vorbedingung ist. Die Teilzahl steht damit fest, *bevor* `ATTEMPTING` geschrieben wird — die
   Forderung des Plans („`ATTEMPTING` ist vollständig") ist erfüllt, der Zeitpunkt ist nur genauer benannt.
3. **`ATTEMPTING` ist eine Inbesitznahme, kein blosser Übergang.** Der Übergang ist nur aus einem sendefälligen
   Zustand erlaubt; aus jedem anderen bleibt der Vorgang unverändert und der Aufrufer sendet **nicht**. Ohne diese
   Bedingung könnten der Versand unmittelbar nach dem Einreihen und ein gleichzeitiger Queue-Scan denselben Vorgang
   beanspruchen — beide hätten `ATTEMPTING` geschrieben (Versuch 1 und 2) und beide gesendet. Damit der Aufrufer eine
   gescheiterte Übernahme von einem gescheiterten `commit()` unterscheiden kann, meldet der Speicher `Applied` /
   `Rejected` / `NotStored` statt nur „null oder nicht".
4. **Rückmeldungen werden als Mengen von Teilindizes geführt, nicht als Zähler.** Der Teilindex steht ohnehin in der
   Callback-URI. Mit blossen Zählern hätte eine doppelt zugestellte Bestätigung desselben Teils einen zweiteiligen
   Vorgang als `SENT` ausgewiesen, obwohl der zweite Teil nie bestätigt wurde. Ein bereits gemeldeter Teil, ein Index
   ausserhalb der erwarteten Teilzahl und eine widersprüchliche Nachmeldung sind jetzt folgenlos.
5. **Die Obergrenze von 50 Einträgen verdrängt nur terminale Vorgänge.** Ein laufender Vorgang (`QUEUED`,
   `ATTEMPTING`, `HANDED_OVER`, `RETRY`) würde durch Verdrängung kommentarlos verschwinden — genau das, was Ziel 2
   verbietet. Reichen die terminalen Einträge nicht aus, wächst die Queue vorübergehend über die Grenze hinaus.
   Begrenzt wird stattdessen der Zulauf: Ist die Zahl laufender Vorgänge erreicht, wird **nicht** gesendet.
6. **Der Verlust wegen voller Warteschlange steht ausserhalb der Queue.** Ein Vermerk *in* der Queue wäre bei genau
   50 laufenden Vorgängen der einzige terminale Eintrag — und damit exakt der, den die Aufbewahrungsregel im selben
   Schreibvorgang wieder verdrängt; der Hinweis löschte sich selbst. Gezählt wird deshalb in den
   Konfigurations-Preferences, gleiche Bauart wie der Korruptionswarnzustand, sichtbar und quittierbar in der
   Warnkarte. Ein Snackbar allein genügt nicht: Er ist weg, bevor jemand hinsieht.
   **Die Haltbarkeitsregel aus AP5.1 gilt für diese Warnzustände mit:** Sie werden unter einer Sperre mit `commit()`
   geschrieben und melden Erfolg oder Misserfolg zurück. Eine Einstellung kann der Nutzer jederzeit erneut setzen —
   die Meldung über einen Verlust ist dagegen dessen einziger Beleg; ginge sie bei einem Prozesskill verloren, wäre
   der Verlust wieder kommentarlos. Die Sperre macht zugleich das Read-modify-write der Zähler atomar, sonst
   untererfassten zwei gleichzeitige Vorfälle den Schaden. Ausgenommen bleibt die unterdrückte Statusanzeige: Sie
   spiegelt nur den aktuellen Berechtigungsstand und wird bei jedem Dienststart neu ermittelt.
7. **Auch die Vorbedingungs-Ablehnung gilt nur aus einem sendefälligen Zustand.** Sie läuft vor der Inbesitznahme;
   ohne dieselbe Bedingung könnte ein veralteter Durchlauf — dessen Vorbereitung scheiterte, während ein anderer
   bereits sendet — den laufenden Versand auf `FAILED` setzen. Dessen Callbacks wären danach wirkungslos, und eine
   tatsächlich hinausgegangene SMS stünde als Fehlschlag da.

## Gerätetests

Durchgeführt am 28.07.2026 auf einem Samsung Galaxy A53 5G (SM-A536B), **Android 16 / SDK 36**, zwei SIMs
(Slot 0 = Subscription 1, Slot 1 = Subscription 2, A1 AT), Modus `SAME_AS_INCOMING`. Installiert war der Build mit
allen Korrekturen (MD5 der APK auf dem Gerät identisch mit dem lokalen Artefakt).

| Szenario der Testmatrix | Ergebnis | Beleg |
| --- | --- | --- |
| Geräteneustart, App **nicht** geöffnet | ✅ bestanden | 14:55:59 Reboot → 14:59:46 `BootReceiver` zugestellt (in 180 ms abgearbeitet) → Dienst läuft, `isForeground=true`, `types=0x40000000` (`specialUse`). Mit `dataSync` hätte derselbe Aufruf bei `targetSdk 36` eine `ForegroundServiceStartNotAllowedException` geworfen — **B13 belegt behoben.** |
| Kein `dataSync`-Zeitlimit für `specialUse` | ✅ bestanden | `device_config put activity_manager data_sync_fgs_timeout_duration 120000`, App im Hintergrund, 4 Minuten gewartet: kein `RemoteServiceException`, kein `onTimeout`, kein Absturz. Flag danach zurückgesetzt. |
| Prozessverlust (`kill -9`) | ✅ bestanden | Zweimal erzwungen; das System startet den Dienst jeweils neu (`restartCount=1`, `crashCount=0`), wieder als `specialUse`. Nebenbefund: `am kill` greift bei laufendem FGS gar nicht. |
| Einteilige Weiterleitung | ✅ bestanden | `DISPATCH_FORWARD_SMS {attempt: 1, parts: 1}` → Callback `{part: 0}` → `state: SENT, confirmed: 1, expected: 1`. |
| Mehrteilig, alle Teile OK | ✅ bestanden | 2 Teile: nach Teil 0 bleibt der Zustand `HANDED_OVER {confirmed: 1, expected: 2}`, erst nach Teil 1 `SENT {confirmed: 2}`. **`SENT` wird nicht voreilig gesetzt.** Am Empfängergerät kamen genau zwei Nachrichten an — keine Dublette, kein Fragment. |
| Mehrteiliger **Empfang** korrekt gruppiert | ✅ bestanden | 221 Zeichen in 2 Teilen → `groups_count: 1`, `total_length: 221`. |
| Loop-Protection | ✅ bestanden | Absender `+436644440286` vs. Ziel `+43 6644440286`: trotz abweichender Schreibweise erkannt, Weiterleitung gestoppt. |
| Dual-SIM | ✅ bestanden | Empfang auf Subscription 1 erkannt, Versand über dieselbe SIM (`SAME_AS_INCOMING`). |
| `POST_NOTIFICATIONS` entzogen | ✅ bestanden | Nach `pm revoke` und erzwungenem Prozesstod startet der Dienst weiter: `notifications_suppressed: true`, „Statusanzeige unterdrueckt - Weiterleitung laeuft weiter", `isForeground=true`. **Der alte Code hätte hier `stopSelf()` gerufen.** |
| Fehlerfreiheit über den gesamten Lauf | ✅ | Kein einziger `ERROR`-Eintrag, kein `QUEUE_FULL`, keine `QUEUE_CORRUPTION`, kein `ATTEMPTING_COMMIT_FAILED`, kein `DISPATCH_SKIPPED`. |

**Weiterhin offen — mit vertretbarem Aufwand nicht am Gerät herstellbar:**

- **Funkloch → `RETRY`**: Im Flugmodus kann keine SMS eintreffen, die den Vorgang erst erzeugt. Ohne Fehlerinjektion
  im Sendepfad nicht auslösbar. Die Zustandsübergänge sind durch JVM-Tests gedeckt, die Netzseite nicht.
- **Ausbleibender Callback → `UNKNOWN` nach 15 Minuten** und der **Ablauf-Scan**: braucht Unterdrückung des Callbacks.
- **`ATTEMPTING` beim Prozesstod vorgefunden**: braucht einen injizierten Halt zwischen `commit()` und Sendeaufruf.
- **Queue-Korruption, volle Queue, fehlschlagendes `commit()`**: brauchen Manipulation der verschlüsselten Datei
  bzw. Fehlerinjektion.
- **`bmgr`-Backup**: Die Ausschlussregeln sind statisch geprüft, ein Restore-Durchlauf steht aus.
- **Dauerbetrieb über 24/72 Stunden**: bisher nur ~20 Minuten am Stück beobachtet.
