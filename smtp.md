# Umsetzungsplan: SMS→E-Mail-Weiterleitung dauerbetriebsfest machen

> Fassung 1 — Stand 29.07.2026. Grundlage ist ein Review des Pfads
> `SmsReceiver` → `SmsForegroundService.processMessageGroup` → `handleEmailForwarding` →
> `EmailSender` → `handleEmailError` inklusive `EmailRetryPolicy`, `EmailViewModel`,
> `EmailSettingsSection`, `SharedPreferencesManager` und `network_security_config.xml`.
>
> Der Plan schliesst an `sms.md` an und übernimmt dessen Leitlinie und Begriffe. Was dort für den
> SMS-Kanal entschieden wurde, wird hier **nicht** blind kopiert: Die Zustellsemantik des
> E-Mail-Kanals weicht bewusst ab (siehe „Zustellsemantik").

## Leitlinie

**Der Betrieb ist automatisch. Der Nutzer konfiguriert einmal und wird danach nur noch informiert,
nie gefragt.** Jede Stelle, an der eine Entscheidung nötig wäre, wird hier deterministisch
entschieden statt an die Oberfläche delegiert.

## Ziel

1. Jede empfangene SMS, für die die E-Mail-Weiterleitung aktiv ist, hat vor dem SMTP-Seiteneffekt
   einen persistierten, eindeutigen Zustand. Nichts verschwindet kommentarlos bei Prozessende
   oder Geräteneustart. Nach einem **Force-Stop** bleibt der Auftrag zwar erhalten, Android
   unterbindet aber jeden automatischen App-Start bis der Nutzer die App wieder öffnet; erst dann
   darf und kann die Zustellung fortgesetzt werden.
2. Ein Fehlschlag wird **nach seiner Ursache** behandelt: Vorübergehende Störungen werden
   wiederholt, dauerhafte nicht.
3. Ein Ausfall des SMTP-Servers verzögert weder den Empfangspfad noch den SMS-Kanal und erzeugt
   keine zusätzliche kostenpflichtige SMS (bereits erfüllt, bleibt erfüllt).
4. Empfänger sehen die Adressen der anderen Empfänger nicht.
5. Ein endgültig gescheiterter Versand ist beim nächsten Öffnen der App sichtbar und quittierbar —
   ohne dass sein Eintreten die Weiterleitung stoppt.

## Nicht-Ziele

- Kein OAuth2/XOAUTH2 für Gmail und Microsoft. Das ist ein eigenes Vorhaben (App-Passwörter
  funktionieren weiterhin); der Plan bereitet die Trennung von Login und Absenderadresse aber so
  vor, dass OAuth2 später ohne Datenmodellbruch nachrüstbar ist.
- Kein Anhang-, HTML- oder MMS-Support. Der Textkörper bleibt `text/plain`, UTF-8.
- Kein Umbau auf WorkManager. Begründung wie in `sms.md`: Expedited Work degradiert bei
  erschöpftem Kontingent zu verzögerbarer Arbeit; der bereits laufende Vordergrunddienst ist das
  verlässlichere Zuhause.
- Kein Room. Die Queue ist klein, ihr Inhalt maximal sensibel (SMS-Volltexte), und
  `androidx.security.crypto` ist bereits im Projekt.

---

## Prüfung der Findings

| Finding | Prüfergebnis | Konsequenz |
| --- | --- | --- |
| **Hoch:** Retry-Klassifikation faktisch ausgehebelt (`EmailSender.kt:107`, `SmsForegroundService.kt:884`) | **Bestätigt, und die Wirkung ist noch etwas grösser als beschrieben.** `EmailSender` fängt `MessagingException` und liefert eine übersetzte Zeichenkette; der Dienst verpackt sie in `IOException`. `EmailRetryPolicy.isRetryable` sieht damit *immer* eine `IOException` und ist wirkungslos — der Unit-Test `EmailRetryPolicyTest` prüft eine Regel, die im Produktivpfad nie zur Anwendung kommt. Betroffen sind auch `AuthenticationFailedException` und `AddressException` (beide erben von `MessagingException`), d. h. ein falsches Passwort erzeugt vier Anmeldeversuche in 30 s — bei Gmail und Microsoft ein Muster, das zur temporären Sperre führen kann. | AP1 |
| **Hoch:** E-Mail-Retries nur im Speicher (`SmsForegroundService.kt:906`, `:1048`) | **Bestätigt.** `emailRetryCounter` und `activeEmailRetryJobs` sind `ConcurrentHashMap` im `companion object`, die Retry-Jobs hängen am `serviceScope`, der in `onDestroy` gecancelt wird. Nach Prozessende ist der Auftrag ohne jede Spur weg — im Gegensatz zum SMS-Kanal, der genau dafür `ForwardingQueueStore` hat. Ergänzend: Das gesamte Wiederholungsfenster beträgt 5+10+15 s = 30 s. Ein Funkloch von einer Minute oder eine Doze-Phase reicht, um jede Wiederholung scheitern zu lassen. | AP2, AP3 |
| **Mittel:** Alle Empfänger im `To:` (`EmailSender.kt:95`) | **Bestätigt.** | AP4 |
| **Mittel:** Teilzustellung führt zu Doppelzustellung beim Retry | **Bestätigt.** `Transport.send` wirft `SendFailedException`, nachdem gültige Empfänger bereits beliefert wurden; die Wiederholung sendet an die vollständige Liste erneut. | AP4 |
| **Mittel:** UI lässt beliebige Ports zu, implementiert ist nur STARTTLS (`EmailSettingsSection.kt:98`, `EmailSender.kt:49`) | **Bestätigt.** Zusätzlich: `it.toIntOrNull() ?: smtpPort` verwirft ungültige Eingaben stillschweigend, ohne den Nutzer zu informieren, und lässt Werte ausserhalb 1–65535 zu. | AP5 |
| **Niedrig:** Zeitpunkt im Body ist Versand-, nicht Empfangszeit (`SmsForegroundService.kt:932`) | **Bestätigt.** Der Empfangszeitpunkt liegt bereits vor: `SmsMessagePart.timestamp` aus `smsMessage.timestampMillis`, wird aber nur zur Gruppierung verwendet. | AP7 |
| Positiv: verschlüsselte Zugangsdaten, STARTTLS mit Hostname-Prüfung, keine Folge-SMS bei SMTP-Ausfall | **Bestätigt.** Diese drei Eigenschaften sind Vorgaben für jeden Umbau, kein Verhandlungsspielraum. | — |

### Ergänzende Befunde aus derselben Durchsicht

| # | Befund | Bewertung |
| --- | --- | --- |
| E1 | **Der Deduplizierungs-Guard in `handleEmailError` ist defekt.** Der `finally`-Block (`SmsForegroundService.kt:925-927`) entfernt `retryKey` aus `activeEmailRetryJobs`, *nachdem* der im `catch` aufgerufene `handleEmailError` dort bereits den Nachfolge-Job eingetragen hat. Der Guard in Zeile 904 greift ab dem zweiten Fehlversuch nicht mehr. | Mittel. Entfällt ersatzlos mit AP2 — wird hier festgehalten, damit der Mechanismus nicht „repariert und behalten" wird. |
| E2 | **Der Erstversand läuft im Empfangspfad unter dem WakeLock-Mutex.** `processSmsData` hält `wakeLockMutex` für bis zu zwei Minuten (`SmsForegroundService.kt:397`); `handleEmailForwarding` läuft darin. Bei einem hängenden SMTP-Server blockiert das die Verarbeitung **jeder weiteren eintreffenden SMS** — inklusive ihres SMS-Zweigs. | Mittel. AP2 löst das strukturell: Der Empfangspfad schreibt nur noch den Auftrag und kehrt sofort zurück. |
| E3 | **Die Wiederholungen laufen ohne WakeLock.** Die Retry-Jobs werden im `serviceScope` gestartet, ausserhalb von `withWakeLock`. Ein `delay` im Doze-Modus wird gestreckt, und Netzwerkzugriff ist dort ohnehin gesperrt. | Mittel. AP2/AP3. |
| E4 | **Absender-Rufnummern werden unmaskiert protokolliert** (`SmsForegroundService.kt:873`, `:900`), obwohl das Projekt mit `MmiCodeMasker.maskNumber` einen Standard dafür hat. `EmailViewModel` protokolliert zusätzlich vollständige Empfängeradressen (`:273`, `:289`). Die Protokolldatei ist per PIN einsehbar und exportierbar. | Niedrig, aber billig zu beheben. AP9. |
| E5 | **Eine Snackbar pro Fehlversuch** (`SmsForegroundService.kt:902`) — bei erschöpften Versuchen fünf Meldungen für eine Nachricht, und keine davon erreicht den Nutzer, wenn die App im Hintergrund ist. | Niedrig. AP8. |
| E6 | **`mail.smtp.writetimeout` fehlt.** `timeout` und `connectiontimeout` sind gesetzt; ein während des Schreibens stehengebliebener Server blockiert damit unbegrenzt (bis zum `withTimeout` von zwei Minuten, siehe E2). | Niedrig. AP9. |
| E7 | **Die Ausnahmen in `network_security_config.xml` sind wirkungslos.** Die Konfiguration greift für Plattform-HTTP-Stacks, nicht für die rohen Sockets von JavaMail. Die drei gelisteten Domains suggerieren eine Steuerung, die nicht existiert. | Niedrig, dokumentarisch. AP9. |
| E8 | **Kein Absenderfeld.** `setFrom(InternetAddress(username))` (`EmailSender.kt:94`) setzt den Login als Absender. Bei Providern, deren Login keine E-Mail-Adresse ist, wirft das eine `AddressException` — die heute als wiederholbar gilt (siehe AP1). | Mittel. AP6. |
| E9 | **Eine neue Queue-Datei würde derzeit ins Backup geraten.** Die Backup-Regeln schliessen nur `sms_forwarder_queue.xml` und die Konfigurations-Preferences aus. Ohne Ergänzung würde `sms_forwarder_email_queue.xml` SMS-Volltexte in Cloud-Backups und Geräteübertragungen kopieren. | Hoch für Datenschutz. AP2. |

---

## Zustellsemantik — und worin sie von `sms.md` abweicht

Der SMS-Kanal entscheidet sich im mehrdeutigen Fenster **gegen** den Neuversand, weil jede
zusätzliche SMS Geld kostet und beim Empfänger als Dublette ankommt.

**Für den E-Mail-Kanal wird umgekehrt entschieden: im Zweifel wird erneut gesendet.** Begründung:

- Eine E-Mail kostet nichts.
- Der Verlust einer weitergeleiteten SMS ist der eigentliche Schadensfall dieser App; eine doppelte
  E-Mail ist eine Unbequemlichkeit.
- Die Dublette ist zusätzlich abschwächbar: Mit einer **stabilen `Message-ID` pro Auftrag**
  erkennen viele Mailserver und -clients die Wiederholung und unterdrücken sie. Das
  ist keine Zusicherung des Standards, aber ein Gewinn ohne Nachteil.

Daraus folgt: Ein Auftrag, der nach einem Prozessneustart im Zustand `ATTEMPTING` vorgefunden wird,
geht **zurück nach `QUEUED`** und wird erneut versucht — er wird *nicht*, wie beim SMS-Kanal, nach
`UNKNOWN` überführt. Der Preis ist benannt: In dem schmalen Fenster zwischen „Server hat die
Nachricht angenommen" und „Zustand geschrieben" kann eine E-Mail doppelt zugestellt werden.

---

## Arbeitspakete

### AP1 — Fehlerursachen typisieren, Wiederholung an die Ursache binden

**Neu: `domain/model/EmailFailure.kt`**

```kotlin
enum class EmailFailureKind {
    /** Netz weg, Server nicht erreichbar, Zeitüberschreitung, SMTP 4xx. Wiederholen. */
    TRANSIENT,
    /** Anmeldung abgelehnt. Nicht wiederholen — weitere Versuche riskieren eine Kontosperre. */
    AUTHENTICATION,
    /** Empfänger dauerhaft abgelehnt (SMTP 5xx, unzustellbare Adresse). Nicht wiederholen. */
    RECIPIENT,
    /** Serverseitige oder transaktionale 5xx-Ablehnung ohne Empfängerbezug. Nicht wiederholen. */
    PERMANENT,
    /** Konfiguration unbrauchbar: Absenderadresse ungültig, Host leer, Port unmöglich. */
    CONFIGURATION,
    /** TLS-Aufbau gescheitert (Zertifikat, Protokollversion, kein STARTTLS). Nicht wiederholen. */
    TRANSPORT_SECURITY
}

data class EmailFailure(
    val kind: EmailFailureKind,
    /** Für die Anzeige aufbereitet, deutsch. */
    val message: String,
    /** SMTP-Antwortcode, sofern der Server einen geliefert hat. */
    val returnCode: Int? = null
)
```

**Neu: `domain/model/SmtpFailureClassifier.kt`** — eine reine Funktion, unit-testbar **ohne**
JavaMail-Abhängigkeit; sie bekommt nur Klassennamen, Antwortcode und Meldungstext:

```kotlin
fun classify(exceptionClassName: String, returnCode: Int?, message: String?): EmailFailureKind
```

Regeln, in dieser Reihenfolge:

1. 530/534/535 oder Klassenname `AuthenticationFailedException` → `AUTHENTICATION`.
2. Ein **empfängerbezogenes** `SMTPAddressFailedException` mit 500..599 → `RECIPIENT`; mit
   400..499 → `TRANSIENT`.
3. Ein server- oder transaktionsbezogenes 500..599 (etwa `SMTPSendFailedException`) →
   `PERMANENT`, nicht `RECIPIENT`: Codes wie 552, 553 oder 554 sagen ohne Empfänger-Kontext
   nicht aus, welche Adresse falsch ist.
4. Alle übrigen 400..499 → `TRANSIENT` (SMTP-Konvention: vorübergehend).
5. Klassenname `AddressException` → `CONFIGURATION`.
6. Klassenname enthält `SSL`/`Certificate` → `TRANSPORT_SECURITY`; Meldung enthält `STARTTLS` →
   `TRANSPORT_SECURITY`.
7. Klassenname `UnknownHostException`, `SocketTimeoutException`, `ConnectException` oder eine
   beliebige `IOException` → `TRANSIENT`. (Bewusst: Ein unauflösbarer Host ist bei fehlendem Netz
   nicht von einem Tippfehler zu unterscheiden. Die Wiederholung ist billig, und AP8 macht das
   endgültige Scheitern ohnehin sichtbar.)
8. Sonst → `TRANSIENT`. Ein unbekannter Fehler wird lieber einmal zu oft wiederholt als eine
   Nachricht verloren.

**`EmailSender` liefert `EmailResult.Error(EmailFailure)`** statt einer Zeichenkette. Den
Antwortcode liefert `com.sun.mail.smtp.SMTPSendFailedException.getReturnCode()` bzw.
`SMTPAddressFailedException`; der direkte Import ist zulässig, `EmailSender` ist bereits die
Mail-nahe Schicht.

**`EmailRetryPolicy`** verliert `isRetryable(Throwable)` und bekommt
`isRetryable(kind: EmailFailureKind)`. Die Backoff-Staffel wird durch AP2 ersetzt.

**Damit entfällt** die Umverpackung in `IOException` (`SmsForegroundService.kt:884`) ersatzlos.

**Tests:** `SmtpFailureClassifierTest` mit einem Fall je Regel; `EmailRetryPolicyTest` auf die neue
Signatur umgestellt (der bisherige Test prüft eine Regel, die im Produktivpfad nie greift).

---

### AP2 — Persistente E-Mail-Queue

**Neu: `domain/model/EmailForwardingJob.kt`**

```kotlin
enum class EmailDeliveryState { QUEUED, ATTEMPTING, SENT, RETRY, FAILED, PARTIAL }

data class EmailRecipientState(
    val address: String,
    val delivered: Boolean = false,
    val failure: EmailFailure? = null
)

data class EmailForwardingJob(
    val id: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sender: String,
    /** Empfangszeitpunkt der SMS, nicht der Verarbeitungszeitpunkt (AP7). */
    val receivedAtMillis: Long,
    val body: String,
    val recipients: List<EmailRecipientState>,
    val state: EmailDeliveryState = EmailDeliveryState.QUEUED,
    val attempt: Int = 0,
    val nextAttemptAtMillis: Long? = null,
    val lastFailure: EmailFailure? = null,
    val acknowledged: Boolean = false
)
```

**Neu: `domain/model/EmailDeliveryReducer.kt`** — reine Übergänge, analog `SmsDeliveryReducer`:
`queue`, `onAttemptStart`, `onRecipientDelivered`, `onRecipientFailed`, `onAttemptFinished`,
`onProcessRestart`, `onExpiryScan`, `isDispatchDue`.

Kernregeln:

- `onAttemptStart` ist eine **Inbesitznahme** wie beim SMS-Kanal: Nur wer den Auftrag aus `QUEUED`
  oder `RETRY` übernimmt, darf senden. Schlägt das Schreiben fehl, unterbleibt der Versand.
- `onProcessRestart` überführt `ATTEMPTING` → `QUEUED` (Begründung siehe „Zustellsemantik").
- `onAttemptFinished`: alle Empfänger zugestellt → `SENT`. Mindestens einer zugestellt, Rest
  dauerhaft gescheitert → `PARTIAL` (terminal, aber sichtbar). Nur vorübergehende Fehler →
  `RETRY` mit `nextAttemptAtMillis`. Versuche erschöpft oder ausschliesslich dauerhafte Fehler →
  `FAILED`.
- **Ein Versuch zählt nur, wenn er stattgefunden hat.** Kein Netz (AP3) erhöht `attempt` nicht.
  Der Zähler wird dagegen synchron zusammen mit `ATTEMPTING` gespeichert, **bevor** eine
  SMTP-Verbindung aufgebaut wird; nach einem Prozessverlust ist damit klar, dass ein
  Seiteneffekt möglich war und `onProcessRestart` die definierte At-least-once-Regel anwenden
  muss.

**Backoff und Frist** (ersetzt 5/10/15 s):

| Versuch | Wartezeit |
| --- | --- |
| 1 | 1 min |
| 2 | 5 min |
| 3 | 15 min |
| 4 | 60 min |
| 5+ | 3 h |

Gesamtfrist **24 h** ab `createdAtMillis`; danach `FAILED`. Der 1-Minuten-Erstwert ist bewusst
länger als heute: Der häufigste vorübergehende Fehler ist ein Netzwechsel oder Funkloch, und dafür
sind 5 s nutzlos.

**Neu: `data/local/EmailQueueStore.kt`** — Aufbau eins zu eins wie `ForwardingQueueStore`:

- eigene verschlüsselte Datei `sms_forwarder_email_queue` (`MasterKey` AES256_GCM,
  `AES256_SIV`/`AES256_GCM`),
- Ausschluss von `sms_forwarder_email_queue.xml` in **beiden** bestehenden Backup-Regeln
  (`backup_rules.xml` sowie `data_extraction_rules.xml`, je Cloud-Backup und Geräteübertragung),
  bevor die Queue produktiv geschrieben wird (behebt E9),
- **`commit()`, nie `apply()`**, mit Rückmeldung an den Aufrufer,
- Korruptionsbehandlung samt `prefs.contains`-Prüfung vor `getString` — die Unterscheidung
  „nie geschrieben" gegen „nicht entschlüsselbar" ist dort teuer erkämpft worden und darf hier
  nicht erneut fehlen (siehe Commit `5109532`),
- Aufbewahrung analog `ForwardingQueueRetentionPolicy`: neue Obergrenze
  `EmailQueueRetentionPolicy` mit `MAX_ENTRIES = 100` und einer Aufbewahrung terminaler Einträge
  von 7 Tagen. Laufende Aufträge werden nie verdrängt; ist die Grenze erreicht, wird nicht
  eingereiht und der Verlust ausserhalb der Queue vermerkt (`prefsManager.recordDroppedEmail()`).

**Umbau in `SmsForegroundService`:**

- `processMessageGroup` ruft statt `handleEmailForwarding` nur noch `enqueueEmailForwarding(group)`:
  Auftrag bauen, `commit()`, bei Erfolg einen **asynchronen** Queue-Scan im `serviceScope`
  anstossen und sofort zurückkehren. Der Empfangspfad wartet weder auf Scan noch Netzwerk und
  hält den WakeLock-Mutex nicht mehr über einen Netzwerkzugriff (behebt E2).
- `scanQueue()` verarbeitet zusätzlich die E-Mail-Queue. Der SMTP-Versand erhält einen eigenen,
  zeitlich begrenzten Partial-WakeLock, darf jedoch **nicht** unter dem bestehenden
  `withWakeLock`/`wakeLockMutex` laufen: Dieser serialisiert den SMS-Empfang und würde E2 erneut
  erzeugen. Der eigene Lock muss referenzgezählt bzw. job-lokal sein und im `finally` zuverlässig
  freigegeben werden (behebt E2 und E3).
- `emailRetryCounter`, `activeEmailRetryJobs`, `handleEmailError` entfallen vollständig (behebt E1).

**Tests:** `EmailDeliveryReducerTest` (jeder Übergang, insbesondere Inbesitznahme,
`onProcessRestart`, Teilzustellung, Fristablauf); `EmailQueueStoreTest` als Instrumented Test
(Schreiben/Lesen, unlesbares Dokument, defekter Einzeleintrag, volle Queue).

---

### AP3 — Wiederholungen an die Netzverfügbarkeit binden

- `ConnectivityManager.registerDefaultNetworkCallback` im Dienst. `onAvailable` stösst einen
  Queue-Scan an; die Registrierung wird in `onDestroy` wieder gelöst.
- Vor jedem Versuch wird die Verfügbarkeit geprüft (`activeNetwork` mit
  `NET_CAPABILITY_INTERNET`). `NET_CAPABILITY_VALIDATED` wird bewusst **nicht** verlangt: Ein
  funktionierender interner SMTP-Server kann über ein Unternehmens-VPN oder LAN erreichbar sein,
  obwohl Android keine öffentliche Validierung meldet. Fehlt das Internet-Capability, bleibt der
  Auftrag `QUEUED`/`RETRY`, **ohne** dass `attempt` steigt.
- Der bestehende 5-Minuten-Scan bleibt als Rückfallebene — der Callback ist eine Beschleunigung,
  keine Voraussetzung.

**Warum kein `JobScheduler`-Constraint:** Der Dienst läuft ohnehin; ein zweiter Planer mit eigenem
Lebenszyklus und eigenem Kontingent wäre eine zusätzliche Fehlerquelle ohne Gegenwert.

---

### AP4 — Zustellung pro Empfänger

- `EmailSender` bekommt `sendToRecipient(...)`, das **einen** Empfänger im `To:` führt.
- Für einen Auftrag mit *n* Empfängern wird **eine** Transportverbindung geöffnet
  (`session.getTransport("smtp").connect(...)`), *n* Nachrichten gesendet, dann geschlossen. Das
  ist zugleich sparsamer als *n*× `Transport.send`, das jedes Mal neu anmeldet.
- Jeder Empfänger bekommt sein Ergebnis in `EmailRecipientState` geschrieben — `commit()` nach
  jedem Empfänger, damit ein Prozessende nicht die bereits zugestellten vergisst.
- Ein Wiederholungsversuch adressiert **nur noch nicht zugestellte** Empfänger.
- **Stabile Message-ID:** `<{jobId}@{absenderdomain}>`, gesetzt über
  `message.setHeader("Message-ID", …)` nach `saveChanges()` (JavaMail überschreibt den Header sonst
  in `updateMessageID`). Da jede Nachricht ohnehin nur einen Empfänger hat, ist kein
  Empfänger-Hash nötig; die Absenderdomain stammt aus der zuvor validierten From-Adresse. Damit
  ist die Wiederholung für Server und Client als solche erkennbar.

Damit sind das BCC-Problem und die Doppelzustellung bei Teilerfolg mit einer Massnahme erledigt.
Ein reines BCC-Feld hätte nur das erste der beiden gelöst.

---

### AP5 — Verschlüsselungsmodus explizit machen, Port validieren

**Neu: `domain/model/EmailTransportSecurity.kt`**

```kotlin
enum class EmailTransportSecurity {
    /** Klartextverbindung, die per STARTTLS zwingend auf TLS gehoben wird. Typisch 587. */
    STARTTLS,
    /** TLS ab dem ersten Byte (SMTPS). Typisch 465. */
    IMPLICIT_TLS
}
```

- `EmailSender` setzt bei `IMPLICIT_TLS` `mail.smtp.ssl.enable=true` und lässt
  `starttls.required` weg; `ssl.protocols` und `ssl.checkserveridentity` gelten in beiden Fällen
  unverändert. Eine unverschlüsselte Option gibt es **nicht** — dabei bleibt es.
- Neue Einstellung `KEY_SMTP_SECURITY`. **Migration:** Bestandsinstallationen mit gespeichertem
  Port 465 erhalten `IMPLICIT_TLS`, alle anderen `STARTTLS`. Das entspricht dem heutigen
  Verhalten für alle, bei denen es funktioniert hat.
- `EmailSettingsSection`: Auswahl über zwei `FilterChip` oder ein `SegmentedButton`; die Auswahl
  schlägt den passenden Port vor (587/465), überschreibt einen abweichenden Wert aber nicht.
- **Portvalidierung** in einer reinen Funktion `EmailPortPolicy.validate(text): Result` mit den
  Fällen „leer", „keine Zahl", „ausserhalb 1–65535". Das Feld zeigt `isError` und einen
  `supportingText` statt die Eingabe stillschweigend zu verwerfen.

---

### AP6 — Absenderadresse von den Zugangsdaten trennen

- Neue Einstellung `KEY_SMTP_FROM_ADDRESS`; leer bedeutet „wie Benutzername" (Verhalten der
  Bestandsinstallationen bleibt damit unverändert).
- Neues Feld „Absenderadresse" in `EmailSettingsSection`, validiert gegen
  `Patterns.EMAIL_ADDRESS` — dieselbe Prüfung, die `EmailViewModel.addEmailAddress` schon für
  Empfänger verwendet.
- `EmailSender` setzt zusätzlich `mail.smtp.from` (Envelope-Absender) auf denselben Wert, damit
  Unzustellbarkeitsmeldungen an die richtige Adresse gehen.
- Eine ungültige Absenderadresse ist ab AP1 `CONFIGURATION` und wird **nicht** wiederholt.

---

### AP7 — Empfangszeitpunkt statt Versandzeitpunkt

- `SmsForwardingComposer.Group` reicht den frühesten `SmsMessagePart.timestamp` der Gruppe durch;
  er wird als `receivedAtMillis` im Auftrag persistiert.
- Der Textkörper führt beide Zeitpunkte, sobald sie um mehr als eine Minute auseinanderliegen:

  ```
  SMS Weiterleitung

  Absender: +43…
  Empfangen: 29.07.2026 14:03:11
  Weitergeleitet: 29.07.2026 14:31:02   ← nur bei Abweichung > 1 min

  Nachricht:
  …
  ```

- `sentDate` der Nachricht bleibt der tatsächliche Versandzeitpunkt. Ihn auf die Empfangszeit zu
  setzen wäre eine Falschangabe im Mail-Header und würde die Sortierung im Postfach verfälschen.

---

### AP8 — Sichtbarkeit statt Snackbar-Flut

- **Snackbar nur noch bei terminalen Zuständen** (`FAILED`, `PARTIAL`) und beim ersten Erfolg —
  nicht mehr pro Fehlversuch (behebt E5).
- `EmailQueueStore.unacknowledgedProblems()` analog zum SMS-Kanal: nicht quittierte Aufträge in
  `FAILED` oder `PARTIAL`.
- `ForwardingWarningsCard` erhält einen Abschnitt „E-Mail-Weiterleitung" mit Absender,
  Empfangszeit, Ursache im Klartext (aus `EmailFailure.message`) und einer Quittierschaltfläche.
  Die Karte wird beim Fortsetzen des Bildschirms ohnehin neu ausgewertet (Commit `5781d58`) — es
  ist nur eine weitere Quelle einzuhängen.
- Bei `AUTHENTICATION` und `CONFIGURATION` verweist der Text direkt auf die betroffene Einstellung
  („SMTP-Passwort prüfen"), weil hier eine Nutzeraktion tatsächlich nötig ist.

---

### AP9 — Aufräumen

1. **Protokollierung entschärfen (E4):** Absender über `MmiCodeMasker.maskNumber`; Empfänger nur
   noch als Anzahl und maskierte Domain (`***@example.com`), nie vollständig. Betrifft
   `SmsForegroundService.kt:873/900` und `EmailViewModel.kt:273/289`.
2. **`mail.smtp.writetimeout = 10000` setzen (E6).**
3. **`network_security_config.xml` (E7):** Die drei Domain-Ausnahmen entfernen und durch einen
   Kommentar ersetzen, der festhält, dass die Datei den JavaMail-Transport nicht steuert. Die
   `base-config` bleibt unverändert.
4. **`EmailViewModel.sendTestEmail`** auf denselben `EmailSender`-Pfad wie die Weiterleitung
   umstellen (Sicherheitsmodus, Absenderadresse, typisierte Fehler), damit der Test aussagt, was er
   verspricht. Die Testmail läuft **nicht** über die Queue — sie ist eine Diagnose, kein Auftrag.

---

## Reihenfolge

```
AP1 ──► AP2 ──► AP3
        │  └──► AP4
        └──► AP8
AP5 ──► AP6      (unabhängig, kann parallel laufen)
AP7              (unabhängig, klein)
AP9              (zuletzt)
```

AP1 zuerst, weil AP2 die typisierte Ursache im Datenmodell benötigt. AP5/AP6 hängen nur an
`EmailSender` und der Oberfläche und sind ohne die Queue nutzbar.

## Prüfplan

**Unit** (`gradlew.bat test`)

- `SmtpFailureClassifierTest` — je ein Fall pro Regel, einschliesslich 4xx/5xx-Grenzen,
  535 → `AUTHENTICATION`, empfängerbezogenem 550 → `RECIPIENT` sowie transaktionalem
  554 → `PERMANENT`.
- `EmailRetryPolicyTest` — auf `EmailFailureKind` umgestellt.
- `EmailDeliveryReducerTest` — Inbesitznahme, doppelter Übernahmeversuch, `onProcessRestart`,
  Teilzustellung, Backoff-Staffel, 24-h-Frist, `attempt` steigt nicht ohne Netz.
- `EmailPortPolicyTest`, `EmailQueueRetentionPolicyTest`.

**Instrumented** (`gradlew.bat connectedAndroidTest`)

- `EmailQueueStoreTest` — Schreiben/Lesen, unlesbares Dokument, defekter Einzeleintrag, Erreichen
  der Obergrenze, fehlgeschlagenes `commit()`.

**Am Gerät** (Android 16, wie in `sms.md`)

| # | Szenario | Erwartung |
| --- | --- | --- |
| G1 | Flugmodus an, SMS empfangen, nach 10 min Flugmodus aus | E-Mail geht raus, sobald das Netz da ist; `attempt` ist dabei nicht aufgebraucht worden |
| G2 | Falsches SMTP-Passwort | **Genau ein** Anmeldeversuch, Auftrag `FAILED`, Warnkarte nennt das Passwort |
| G3 | Zwei Empfänger, einer ungültig | Der gültige bekommt die Mail **einmal**, Auftrag `PARTIAL`, Warnkarte nennt die gescheiterte Adresse |
| G4 | Force-Stop während einer laufenden Wiederholung, danach App öffnen | Auftrag ist noch da; nach dem vom Öffnen ausgelösten Service-Start wird er zugestellt. Kein automatischer Wiederanlauf vor dem Öffnen wird behauptet. |
| G5 | Geräteneustart mit wartendem Auftrag | Auftrag wird nach dem Boot zugestellt |
| G6 | SMTP-Server antwortet nicht (Host auf eine geblackholte IP) | Empfangspfad bleibt schnell, eine parallel eintreffende SMS wird ohne Verzögerung per SMS weitergeleitet |
| G7 | Port 465 mit implizitem TLS | Versand funktioniert |
| G8 | Zwei Empfänger, gültig | Keiner sieht die Adresse des anderen |
| G9 | 24 h Laufzeit mit stündlichen Test-SMS | Kein Auftrag bleibt liegen, Queue wächst nicht unbegrenzt |
| G10 | Rückstau mehrerer Aufträge gegen einen hängenden Server (Host auf geblackholte IP), Bildschirm aus | Jeder Auftrag sendet unter eigenem WakeLock; nach 10 min wird kein weiterer begonnen, der Rest folgt im nächsten Durchlauf. Der Durchlauf endet spätestens nach 15:40 min. Nichts bleibt dauerhaft liegen. |

## Bewusst in Kauf genommen

- **Doppelte E-Mails** im Fenster zwischen Serverannahme und Zustandsschreibung. Abgeschwächt durch
  die stabile `Message-ID`, nicht ausgeschlossen. Die Alternative wäre der Verlust — siehe
  „Zustellsemantik".
- **SMS-Volltexte liegen bis zu 7 Tage** verschlüsselt auf dem Gerät. Das ist neu gegenüber heute,
  wo sie nur im Speicher stehen. Ohne diese Persistenz ist Ziel 1 nicht erreichbar. Die Datei ist
  AES-verschlüsselt und liegt im privaten App-Verzeichnis.
- **Kein OAuth2.** Bei Gmail und Microsoft bleiben App-Passwörter nötig. Wird dort das
  Basic-Auth-Verfahren abgeschaltet, ist dieser Plan nicht die Lösung dafür.
- **Force-Stop stoppt die Automatik.** Android sperrt danach Broadcast- und Service-Starts bis der
  Nutzer die App einmal öffnet. Die persistente Queue verhindert Datenverlust, ersetzt aber diese
  Plattformgrenze nicht.
- **Ein unauflösbarer Hostname wird 24 h lang wiederholt**, obwohl er auch ein Tippfehler sein
  kann. Die Unterscheidung ist ohne Netz nicht treffbar; AP8 macht das Ergebnis sichtbar.

---

## Umsetzungsstand

**AP1–AP9 sind umgesetzt und am Gerät geprüft** (Stand 29.07.2026, Galaxy A53 / SM-A536B,
Android 16, GMX als SMTP-Anbieter). `gradlew.bat testDebugUnitTest`, `lintDebug` und
`assembleDebug` laufen durch, die Instrumented-Tests ebenfalls (9/9 auf dem Gerät). Von den
Gerätetests sind **G1–G8 und G10 bestanden**; offen bleibt allein G9 (24-Stunden-Lauf).
Ergebnisse und zwei dabei gefundene Fehler stehen unter „Gerätetests".

### Neue Dateien

| Datei | Inhalt |
| --- | --- |
| `domain/model/EmailFailure.kt` | `EmailFailureKind`, `EmailFailure` |
| `domain/model/SmtpFailureClassifier.kt` | Zuordnung Antwortcode/Klassenname → Ursache |
| `domain/model/EmailForwardingJob.kt` | `EmailDeliveryState`, `EmailRecipientState`, Auftrag |
| `domain/model/EmailDeliveryReducer.kt` | Zustandsmaschine des E-Mail-Kanals |
| `domain/model/EmailQueueRetentionPolicy.kt` | Aufbewahrung: 100 Einträge / 7 Tage |
| `domain/model/EmailTransportSecurity.kt` | STARTTLS / implizites TLS samt Portableitung |
| `domain/model/EmailPortPolicy.kt` | Portprüfung |
| `domain/model/EmailBodyComposer.kt` | Betreff und Textkörper, Empfangs- vs. Versandzeit |
| `domain/model/EmailDispatchBudget.kt` | Zeitbudget je Auftrag und Durchlauf, WakeLock-Laufzeit |
| `data/local/EmailQueueStore.kt` | verschlüsselte Queue, `commit()`, Korruptionsmeldung |
| `util/EmailFailureMessages.kt` | Ursache → String-Ressource |

### Abweichungen vom Plan

| Punkt | Plan | Umsetzung | Grund |
| --- | --- | --- | --- |
| Anzeigetext des Fehlers | `EmailFailure.message` als „deutsch, für die Anzeige aufbereitet" | `EmailFailure(kind, detail, returnCode)`; der Anzeigetext wird über `EmailFailureKind.messageRes()` aus `strings.xml` geholt | Die App ist zweisprachig (`values`/`values-de`). Ein im Modell festgeschriebener deutscher Text wäre in der englischen Fassung nicht übersetzbar. `detail` trägt die technische Serverantwort und geht **nur** ins Protokoll. |
| Message-ID setzen | `setHeader` nach `saveChanges()` | `MimeMessage.updateMessageID()` in einer anonymen Unterklasse überschrieben | `Transport.sendMessage` ruft `saveChanges()` nicht erneut auf, `Transport.send` schon — der Header wäre je nach Aufrufweg überschrieben worden. Das Überschreiben der Methode ist der von JavaMail vorgesehene Weg und in beiden Fällen wirksam. |
| SMTP-Antwortcode lesen | direkter Import von `com.sun.mail.smtp.SMTPSendFailedException` | über Reflection (`getReturnCode`) | `com.sun.mail.smtp` ist herstellerspezifisch und wird von JavaMail nicht als API geführt. Die Klassifikation bleibt so unabhängig davon, welche Unterklasse die Bibliothek liefert. |
| Zeitliche Auflösung des Backoffs | 1 min / 5 min / 15 min / 60 min / 3 h | dieselbe Staffel, aber wirksam erst beim nächsten Queue-Durchlauf (Takt: 5 min) | Ein eigener Zeitgeber je Auftrag wäre genauer, brächte aber die Job-Verwaltung zurück, deren defekter Guard (E1) gerade entfernt wurde. Der häufigste Fall — Netz weg, Netz wieder da — wird über den Netz-Callback ohnehin sofort bedient. |
| — | im Plan nicht vorgesehen | **Neue Berechtigung `ACCESS_NETWORK_STATE`** | Ohne sie sind `getActiveNetwork` und `registerDefaultNetworkCallback` nicht aufrufbar; AP3 wäre nicht umsetzbar. Installationszeit-Berechtigung, keine Nutzerabfrage. Durch `ManifestForegroundServiceTest` festgehalten. |
| — | im Plan nicht vorgesehen | Hilfetext „SMS-Inhalte werden nicht dauerhaft gespeichert" korrigiert | Die Aussage war bereits seit Einführung der SMS-Queue unzutreffend und wäre mit der E-Mail-Queue eine klare Falschangabe zum Datenschutz geworden. |

### Nachgebessert nach der Begutachtung der Umsetzung

| Befund | Prüfergebnis | Korrektur |
| --- | --- | --- |
| **Coroutine-Abbruch wurde als Versandfehler behandelt** (`EmailSender.kt`) | **Zugestimmt.** `catch (e: Exception)` fängt auch `CancellationException`. Ein Abbruch würde als `TRANSIENT` klassifiziert, einen Versuch verbrauchen und den Auftrag als `RETRY` festschreiben, statt ihn beim Wiederanlauf sauber aus `ATTEMPTING` zurückzuholen. Anzumerken ist, dass der Pfad im heutigen Stand nicht erreichbar ist: Beide `try`-Blöcke umschließen ausschließlich blockierende JavaMail-Aufrufe ohne Suspensionspunkt, und ein Abbruch während des Sendens wird erst beim Verlassen von `withContext` wirksam. Die Absicherung ist trotzdem richtig — `send` ist eine `suspend`-Funktion, und ein später hinzukommender Suspensionspunkt würde den Fehler unbemerkt scharf schalten. | `CancellationException` wird in beiden `catch`-Kaskaden vor der Klassifikation erneut geworfen. |
| **Ein fehlgeschlagenes Empfängerergebnis wurde ignoriert** (`SmsForegroundService.kt`) | **Bestätigt, echter Fehler.** Der Callback verwarf das Ergebnis von `emailQueue.update`. Bei fehlgeschlagenem `commit()` lief der Versand weiter, und der Abschluss schrieb einen Auftrag fest, dessen Zustand den tatsächlichen Versand nicht mehr abbildet: Ein zugestellter Empfänger galt als offen und hätte die Nachricht beim nächsten Versuch ein zweites Mal bekommen — genau das Fenster, das die Queue kontrollieren soll. | Der Callback gibt jetzt `Boolean` zurück; bei `NotStored` bricht `EmailSender` den Lauf ab und meldet `EmailSendOutcome.Aborted`. Der Auftrag wird dann **nicht** abgeschlossen, sondern bleibt in `ATTEMPTING`. Der Vorfall wird über `recordEmailStateWriteFailure` dauerhaft vermerkt und auf der Startseite angezeigt, samt Hinweis auf die mögliche Doppelzustellung. |

| **Der WakeLock konnte den Versand überdauern** (`SmsForegroundService.kt`) | **Bestätigt.** Der WakeLock lief mit festen zwei Minuten und umschloss **alle** fälligen Aufträge eines Durchlaufs. Der realistische Fall ist deshalb nicht der eine Auftrag mit vielen Empfängern, sondern ein Rückstau nach einem längeren Serverausfall: Schon drei Aufträge mit je zwei Empfängern an einem hängenden Server überschreiten die zwei Minuten, und der Rest sendet ohne WakeLock — ab da entscheidet der Doze-Modus. | Neues `EmailDispatchBudget`: Jeder Auftrag bekommt einen **eigenen** WakeLock, dessen Laufzeit aus seiner Empfängerzahl folgt (40 s je Empfänger, gedeckelt auf 5 min). Der Auftrag bricht ab, sobald sein Zeitbudget erschöpft ist; nach 10 min wird kein weiterer Auftrag mehr begonnen (Startgrenze, siehe unten). Die Empfängerzahl wird **nicht** begrenzt — eine stillschweigend gekürzte Empfängerliste wäre echter Verlust, während ein überschrittenes Budget nichts kostet: Wer nicht drankam, bleibt eingereiht und ist im nächsten Durchlauf an der Reihe. |

Der Budgetabbruch nutzt denselben Rückgabewert des Empfänger-Callbacks wie der Persistenzabbruch,
wird aber gegenteilig behandelt: Beim Budget ist der Zustand **vollständig**, der Auftrag wird
also regulär abgeschlossen und stellt die offenen Empfänger als Neuversuch ein — kein Verlust,
keine Dublette. Nur der Persistenzabbruch lässt den Auftrag in `ATTEMPTING` stehen.

**Das Durchlaufbudget ist eine Startgrenze, keine harte Gesamtlaufzeit** — bewusst so, und
inzwischen auch so benannt (`MAX_RUN_START_MILLIS` statt `MAX_RUN_MILLIS`). Nach Ablauf wird kein
*weiterer* Auftrag begonnen; ein bereits laufender läuft unter seinem eigenen, auf seine
Empfängerzahl bemessenen WakeLock zu Ende. Ihn mittendrin zu kappen würde nichts gewinnen: Er ist
gedeckt, und der Abbruch erzeugte nur einen zusätzlichen Neuversuch. Die Grenze soll verhindern,
dass ein Rückstau den Versandlauf unbegrenzt belegt, nicht einen gedeckten Versand abschneiden.
Der Preis ist benannt, begrenzt und geprüft: `EmailDispatchBudget.worstCaseRunMillis()` = 10 min
Startgrenze + 5 min Auftragsbudget + 40 s für den letzten Einzelversand = **15:40 min**.

Aus der zweiten Korrektur folgte eine notwendige Ergänzung: **`EmailDeliveryReducer.onStaleAttempt`.**
Ein in `ATTEMPTING` belassener Auftrag wäre sonst bis zum nächsten Prozessstart liegengeblieben —
unsichtbar, weil nicht terminal, und unbearbeitet, weil nicht sendefällig. Der Übergang holt ihn
nach 15 Minuten ohne Fortschritt zurück nach `QUEUED`. Er ist gefahrlos, weil alle Versandläufe
eines Prozesses über `emailDispatchMutex` serialisiert sind: Ist die Frist abgelaufen, läuft zu
diesem Auftrag nachweislich kein Versand mehr. Derselbe Übergang fängt auch den abgebrochenen
Versandlauf aus dem ersten Befund auf.

### Ergänzungen gegenüber der Planung

- `EmailDeliveryReducer.onConnectionFailed` — im Plan nicht als eigener Übergang genannt, aber
  nötig: Ein gescheiterter Verbindungsaufbau betrifft **alle** offenen Empfänger, während bereits
  zugestellte unberührt bleiben müssen.
- `dispatchEmailJob` bricht ab, wenn nach der Inbesitznahme kein Empfänger mehr offen ist. Dieser
  Fall entsteht real: Der letzte Empfänger wurde noch als zugestellt geschrieben, der Abschluss
  des Versuchs nicht mehr.
- `isNetworkUsable()` fällt bei fehlender Berechtigung auf „versuchen" zurück statt auf
  „aufschieben" — ein unterbliebener Versand wäre der größere Schaden als ein vergeblicher.
- `finishEmailAttempt` protokolliert ein fehlgeschlagenes `commit()`, statt es stumm zu
  verschlucken. Hier droht **keine** Dublette — die Empfängerergebnisse stehen bereits, und
  `onStaleAttempt` schließt den Auftrag mit dann leerer Empfängerliste sauber ab. Deshalb bleibt
  es beim Protokolleintrag ohne Warnkarte.

### Prüfstand

| Prüfung | Ergebnis |
| --- | --- |
| `SmtpFailureClassifierTest` (10 Fälle) | grün |
| `EmailRetryPolicyTest` (4) | grün |
| `EmailDeliveryReducerTest` (23) | grün |
| `EmailPortPolicyTest` (4), `EmailQueueRetentionPolicyTest` (5), `EmailBodyComposerTest` (5) | grün |
| `EmailDispatchBudgetTest` (7) | grün |
| `ManifestForegroundServiceTest` (3, davon 1 neu) | grün |
| `gradlew.bat lintDebug` | grün (keine neuen Fehler, Baseline unverändert) |
| `gradlew.bat assembleDebug` | grün |
| `EmailQueueStoreTest` (9, Instrumented) | grün auf SM-A536B / Android 16 |
| Gerätetests G1–G8, G10 | bestanden (siehe unten) |
| G9 (24-Stunden-Lauf) | offen |

---

## Gerätetests

Durchgeführt am 29.07.2026 auf einem Galaxy A53 (SM-A536B) unter Android 16, gegen `mail.gmx.net`
mit zwei echten Empfängern. Ausgelöst über den „Test-SMS"-Knopf, der eine SMS an die eigene SIM
schickt; sie durchläuft damit den regulären Empfangspfad. Belege stammen aus dem App-Protokoll und
dem Posteingang der Empfänger.

| # | Szenario | Ergebnis |
| --- | --- | --- |
| G1 | Kein Internet bei Empfang, danach Netzrückkehr | **Bestanden.** 07:42:19 `EMAIL_DISPATCH_DEFERRED`, erneut 07:42:46; Netz an 07:42:59; zugestellt 07:43:02 mit `attempt: 1`. Der Netz-Callback greift in 3 s, und **zweimaliges Aufschieben hat kein Wiederholungsbudget verbraucht** — der Kernzweck von AP3. |
| G2 | Falsches SMTP-Passwort | **Bestanden.** `state: FAILED`, `attempt: 1`, `AUTHENTICATION`. **Genau ein Anmeldeversuch**; die alte Fassung hätte vier in 30 s erzeugt. Warnkarte: „Der Mailserver hat die Anmeldung abgelehnt – SMTP-Passwort prüfen." |
| G3 | Drei Empfänger, einer dauerhaft unzustellbar | **Bestanden.** `state: PARTIAL`, `delivered: 2 von 3`, `attempt: 1`, `RECIPIENT` mit `return_code: 550`. Die beiden gültigen Empfänger erhielten die Nachricht **genau einmal** — keine Wiederholung wegen des dritten. Die Trennung `RECIPIENT` gegen `PERMANENT` ist damit am realen Server belegt. |
| G4 | Force-Stop während eines wartenden Auftrags | **Bestanden, mit der dokumentierten Grenze.** Nach dem Force-Stop lief 90 s lang **nichts** an, obwohl das Netz zurück war. Nach dem Öffnen der App: Dienst 08:04:06, Zustellung 08:04:07. Der Auftrag hatte 15 Minuten überdauert. |
| G5 | Geräteneustart mit wartendem Auftrag | **Bestanden.** `BootReceiver` startete den Dienst 07:47:09 von selbst (~80 s nach dem Boot, ohne die App zu öffnen), fand den Auftrag von 07:44:05 wieder und stellte ihn nach Netzrückkehr um 07:48:10 zu. |
| G6 | SMTP-Server antwortet nicht (Host auf `10.255.255.1`) | **Bestanden — der deutlichste Beleg des Umbaus.** 08:13:14 SMS ausgelöst, 08:13:16 SMS-Weiterleitung übergeben **und** Empfangsverarbeitung abgeschlossen, 08:13:26 E-Mail-Versuch im Zeitlimit gescheitert. Der Empfangspfad war nach 2 s fertig, während SMTP 10 s hing. Vorher lief beides unter derselben Sperre (Befund E2). |
| G7 | Port 465 mit implizitem TLS | **Bestanden.** Umschalten setzte den Port automatisch auf 465; Zustellung 08:06:19. Dieser Fall konnte vor dem Umbau grundsätzlich nicht funktionieren. |
| G8 | Zwei gültige Empfänger | **Bestanden.** Im `An:` steht jeweils nur die eigene Adresse. Zusätzlich im Body bestätigt: „Empfangen: 29.07.2026 07:42:17" — der Empfangszeitpunkt der SMS, und die Zeile „Weitergeleitet:" fehlt korrekt, weil die Differenz unter einer Minute lag (AP7). |
| G9 | 24-Stunden-Lauf | **Offen.** |
| G10 | Rückstau mehrerer Aufträge gegen den toten Server | **Bestanden.** Drei Aufträge wurden 08:14:39 / 08:14:49 / 08:14:59 nacheinander versucht — exakt 10 s Abstand, jeder mit eigenem WakeLock. **Dazwischen wurden um 08:14:30 und 08:14:42 zwei weitere SMS empfangen und verarbeitet.** Nach Korrektur des Hosts gingen alle drei von selbst hinaus (`attempt: 2`, `attempt: 2`, `attempt: 3`) — die Backoff-Staffel 1 min / 5 min ist damit am Gerät belegt. |

**Duplikatfreiheit über die ganze Sitzung:** Im Posteingang lag zu jeder Weiterleitung **genau eine
Nachricht je Empfänger** — trotz Funkloch, Geräteneustart, Force-Stop und Rückstau. Die
Inbesitznahme in der Queue hält am realen Gerät.

### Dabei gefundene Fehler (behoben)

| Befund | Ursache | Korrektur |
| --- | --- | --- |
| **Die App zeigte überhaupt keine Meldungen an** — weder Erfolg noch Fehler, in der ganzen App. Aufgefallen, weil eine gescheiterte Test-E-Mail wortlos blieb. | `LaunchedEffect(snackbarHostState) { setSnackbarState(hostState, this) }` in `MainActivity`: `this` ist der Scope der Effect-Coroutine. Deren Block kehrt sofort zurück, die Coroutine ist damit abgeschlossen — und ein abgeschlossener Job nimmt keine Kinder mehr an. Jedes spätere `launch { showSnackbar(…) }` startete nie. Der Fehler ist älter als dieser Umbau, entwertete aber die Sichtbarkeit, die AP8 herstellen soll. | `rememberCoroutineScope()` statt `this`. Am Gerät verifiziert: identischer Ablauf vorher ohne, nachher mit Meldung. |
| **Falsche Pluralform** in der Warnkarte: „**1** E-Mail-Weiterleitung**en** **sind** gescheitert". Derselbe Fehler steckte im bestehenden `warning_failed_operations`. | Zählwert in einen `<string>` mit fester Mehrzahl eingesetzt. | Beide auf `<plurals>` umgestellt, Anzeige über `pluralStringResource`. Am Gerät verifiziert: „Eine E-Mail-Weiterleitung ist gescheitert…". |
