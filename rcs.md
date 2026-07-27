# Umsetzungsplan: Umgang mit RCS-Nachrichten (RCS-Awareness statt RCS-Weiterleitung)

> Revision 3 – Richtungsentscheidung. Ersetzt die Revisionen 1 und 2, die eine Weiterleitung über einen `NotificationListenerService` vorsahen. Dieser Ansatz ist geprüft und bewusst verworfen; die vollständige Begründung steht in **Anhang A**, damit die Analyse nicht verloren geht und die Diskussion nicht erneut geführt werden muss.

## Entscheidung

Die App leitet weiterhin ausschließlich SMS weiter. RCS-Nachrichten werden **nicht** weitergeleitet und es wird keine Technik gebaut, die das versucht.

Stattdessen wird das Problem dort gelöst, wo es entsteht: Der Nutzer erfährt klar und rechtzeitig, dass RCS-Chats die Weiterleitung umgehen, und wird angeleitet, RCS auf dem weiterleitenden Gerät zu deaktivieren. Danach greift der Fallback des Absenders automatisch auf SMS, und die bestehende, seit Jahren stabile Weiterleitung über `Telephony.Sms.Intents.SMS_RECEIVED_ACTION` erfasst wieder alle Textnachrichten.

### Begründung

1. **Das weiterleitende Gerät braucht kein RCS.** Ein Gerät, dessen Nachrichten man woanders lesen will, ist per Definition kein Gerät, auf dem Lesebestätigungen, Tippanzeige oder hohe Bildqualität einen Wert haben. Der Verzicht kostet in diesem Anwendungsfall praktisch nichts.
2. **Der Benachrichtigungsweg versagt beim wichtigsten Fall.** Seit Android 15 redigiert die Plattform Benachrichtigungen mit Einmalcodes für nicht privilegierte Notification Listener. Ausgerechnet 2FA-Codes – der häufigste Grund, überhaupt weiterzuleiten – kämen über diesen Pfad nicht an. Über den SMS-Pfad kommen sie vollständig an.
3. **Aufwand und Risiko stehen in keinem Verhältnis.** Revision 2 verlangte Geräte-Spike, Refactoring der Weiterleitungs-Pipeline, SMS-Korrelation mit Quarantänepuffer, Wasserzeichen pro Benachrichtigung, Ratenbremse, Kürzungspolicy, neue verschlüsselte Einstellungen, erneute Datenschutz-Einwilligung und eine umfangreiche manuelle Gerätematrix – für eine Komponente, die jedes Google-Messages-Update brechen kann.
4. **Der teuerste Fehlerfall entfällt.** Doppelweiterleitung derselben SMS kostet reales Geld. Ohne Listener kann sie nicht auftreten.
5. **Ehrlichkeit gegenüber dem Nutzer.** Eine als „best effort" beworbene Weiterleitung, die Anhänge, Gruppen, Einmalcodes und Nachrichten ohne sichtbare Benachrichtigung auslässt, erzeugt mehr Supportaufwand und Vertrauensschaden als ein klarer Hinweis auf eine echte Grenze.

### Wo diese Entscheidung nicht trägt

Ein Szenario bleibt ungelöst: Das weiterleitende Gerät ist zugleich das aktiv genutzte Hauptgerät, und die Weiterleitung dient als E-Mail-Archiv statt als Zustellung an ein Zweitgerät. Dann ist das Abschalten von RCS ein echter Komfortverlust. Dieser Fall wird als bekannte Grenze dokumentiert, nicht technisch gelöst.

## Was die App tut und was nicht

| Fall | Verhalten |
|---|---|
| SMS | Unverändert weitergeleitet über `SMS_RECEIVED`. |
| RCS-Chat | Nicht weitergeleitet. Android liefert RCS-Nachrichten nicht an Dritt-Apps aus. Die App erklärt dies und zeigt den Ausweg. |
| MMS (Bild, Video, Sprachnachricht) | Nicht weitergeleitet. Die App empfängt keinen `WAP_PUSH_RECEIVED`-Broadcast. Bereits heute so, wird nun ausdrücklich dokumentiert. |
| RCS-Status in Google Messages | Nicht auslesbar. Die App behauptet keinen Status und zeigt keinen an. |
| Benachrichtigungszugriff | Wird nicht angefordert. Es kommt keine neue Berechtigung hinzu. |
| Andere Nachrichten-Apps | Nicht relevant, siehe unten. Nur Google Messages wird geprüft. |

Wichtig für die Formulierung aller Texte: Die App **kann nicht feststellen**, ob RCS auf dem Gerät aktiv ist. Jeder Hinweis muss deshalb als Möglichkeit formuliert sein („falls Sie RCS nutzen"), nie als Feststellung („RCS ist aktiv"). Die geprüften Gründe dafür stehen in **Anhang B**.

## Warum nur Google Messages betrachtet wird

Auf Android ist Google Messages seit 2026 praktisch der einzige RCS-Client. Google betreibt mit Jibe das Backend, das die Mobilfunkanbieter nutzen; eigenständige RCS-Implementierungen sind verschwunden. Samsung Messages hat den RCS-Support bereits Anfang 2025 eingestellt und wurde zum **6. Juli 2026** vollständig abgekündigt, mit ausdrücklicher Verweisung der Nutzer auf Google Messages. Ehemalige Carrier-Apps stützen sich, soweit sie überhaupt noch existieren, auf dieselbe Google-Infrastruktur; im Markt A1/spusu spielen sie keine Rolle.

Andere Messenger wie WhatsApp, Signal oder Telegram sind kein RCS und für diesen Plan bedeutungslos: Sie ersetzen keine SMS und erzeugen deshalb auch keine Lücke in der Weiterleitung.

**Konsequenz:** Die Beschränkung auf `com.google.android.apps.messaging` ist korrekt und muss nicht erweitert werden. Sollte künftig ein zweiter relevanter RCS-Client entstehen, ist nur die Paketliste der Prüfung in Baustein 2 zu ergänzen – die Architektur ändert sich dadurch nicht.

## Umsetzung

Drei Bausteine, geschätzter Gesamtaufwand rund ein Arbeitstag inklusive Übersetzungen.

### 1. Hilfe-Rubrik „Warum kommen manche Nachrichten nicht an?"

`presentation/ui/screens/help/HelpScreen.kt` erzeugt die Hilfe als HTML über `getHelpHtmlContent()`; alle Texte liegen als `help_*`-Ressourcen in `res/values/strings.xml` (EN) und `res/values-de/strings.xml` (DE). Die neue Rubrik folgt diesem Muster – kein neuer Screen, kein neues Layout.

Inhalt der Rubrik (neue String-Keys `help_rcs_*`):

1. **Erklärung.** Google Messages versendet Nachrichten zwischen Android-Geräten als RCS-Chat statt als SMS. Android liefert RCS-Nachrichten aus technischen Gründen nicht an andere Apps aus. Die App kann sie deshalb nicht weiterleiten – unabhängig von Berechtigungen und Einstellungen.
2. **Selbst prüfen, ob RCS aktiv ist.** Die App kann das technisch nicht feststellen, der Nutzer aber sehr wohl – und das ist der einzige verlässliche Weg. Zwei Prüfungen nennen: (a) Google Messages → Profilbild → Nachrichteneinstellungen → RCS-Chats; dort steht ein Verbindungsstatus. (b) Im Alltag erkennbar an den Chat-Merkmalen in der Unterhaltung – Lesebestätigung, Tippanzeige, Hinweis auf eine Chat- statt SMS-Nachricht im Eingabefeld.
3. **Lösung.** RCS auf dem weiterleitenden Gerät deaktivieren: Google Messages → Profilbild → Nachrichteneinstellungen → RCS-Chats → deaktivieren.
4. **Dual-SIM-Hinweis (`highlight`-Formatierung).** RCS ist an die Rufnummer gebunden, nicht an das Gerät. Bei zwei SIM-Karten muss RCS **für jede** Rufnummer einzeln deaktiviert werden. Eine halb abgeschaltete Konfiguration ist die häufigste Ursache dafür, dass Nachrichten weiterhin unerklärlich fehlen.
5. **Geduld statt Wiederholung (`highlight`-Formatierung).** Die Abmeldung wirkt nicht sofort; sendende Geräte merken sich die RCS-Fähigkeit einer Nummer eine Zeit lang. Ein Test direkt nach dem Abschalten ist nicht aussagekräftig. RCS nicht mehrfach hintereinander ein- und ausschalten – das kann zu einer temporären Sperre führen.
6. **Wenn es hängen bleibt.** Googles Deregistrierungsseite `https://messages.google.com/disable-chat` meldet eine Rufnummer serverseitig von RCS ab. Als Text nennen, nicht als klickbaren Deep-Link in einer WebView öffnen.
7. **Was danach anders ist.** Bilder und Videos kommen als MMS und werden ebenfalls nicht weitergeleitet; nur Textnachrichten werden erfasst. Die Ende-zu-Ende-Verschlüsselung von RCS entfällt – bei einer Weiterleitung per SMS oder E-Mail besteht sie ohnehin nicht.
8. **Test.** Mit einem zweiten Gerät eine Textnachricht senden und prüfen, ob die Weiterleitung ankommt. Ein Test zwischen den beiden SIM-Karten desselben Geräts ist nicht aussagekräftig.

### 2. Hinweiskarte auf der Startseite

`presentation/ui/screens/home/HomeScreen.kt` enthält bereits das Muster kompakter Statuskarten (`PendingForwardingCard`, `ForwardingVerificationCard` über `AnimatedCard`). Analog dazu eine `RcsHintCard`.

* **Anzeigebedingung, dreistufig.** Die naheliegende Prüfung „Google Messages ist Standard-SMS-App" greift zu kurz: RCS ist an die **Rufnummer** gebunden und serverseitig registriert. Wer Google Messages einmal genutzt und dann die Standard-App gewechselt hat, kann weiterhin registriert sein – genau die Konstellation, in der Nachrichten unerklärlich fehlen. Deshalb:

  | Zustand | Hinweis |
  |---|---|
  | Google Messages ist Standard-SMS-App | „Falls RCS-Chats aktiv sind, werden diese Nachrichten nicht weitergeleitet" + Anleitung zum Deaktivieren |
  | installiert, aber **nicht** Standard-App | „Ihre Rufnummer könnte noch bei RCS registriert sein" + Hinweis auf die Deregistrierungsseite |
  | nicht installiert | kein Hinweis |

  Ermittlung: `Telephony.Sms.getDefaultSmsPackage(context)` für den Standard-Status, `PackageManager.getPackageInfo()` für die Installation.
* **Manifest-Ergänzung (zwingend).** Seit Android 11 ist die Paketsichtbarkeit gefiltert; `getPackageInfo()` auf ein fremdes Paket wirft ohne Deklaration `NameNotFoundException`. Erforderlich ist deshalb:

  ```xml
  <queries>
      <package android:name="com.google.android.apps.messaging" />
  </queries>
  ```

  Das ist die einzige Manifest-Änderung des gesamten Plans. Sie fügt **keine** Berechtigung hinzu und ist für die Play-Data-Safety-Angaben unerheblich. Der Aufruf bleibt trotzdem in `try/catch` gekapselt.
* **Kein Rückschluss auf den RCS-Status** – nur auf die installierte bzw. verwendete Nachrichten-App. Das ist die einzige belastbare Information und rechtfertigt die Formulierung „falls".
* **Inhalt:** eine Zeile Text, Aktion „Mehr erfahren" (öffnet die Hilfe bei der RCS-Rubrik) und Aktion „Verstanden" (blendet dauerhaft aus). Im Zustand „installiert, aber nicht Standard-App" nennt der Text zusätzlich die Deregistrierungsseite, damit der Ausweg ohne Umweg über die Hilfe erreichbar ist.
* **Einstieg in die Hilfe:** Die Hilfe ist eine WebView mit bewusst deaktiviertem JavaScript; ein Anker-Sprung (`#id`/`scrollTo`) ist damit nicht möglich, und JavaScript nur zum Scrollen zu aktivieren wäre in einer SMS-App unverhältnismäßig. Stattdessen erhält `HelpScreen` einen Parameter `initialSection`: Beim Einstieg über die Karte wird die RCS-Rubrik an den Anfang des Dokuments gestellt, die übrige Hilfe folgt vollständig und unverändert darunter.
* **Persistenz:** ein Flag `rcsHintDismissed` in `SharedPreferencesManager`, analog zu den vorhandenen Booleans. Kein neuer Speicher, keine Verschlüsselungsänderung.
* **Rückholbar:** Ein Schalter „RCS-Hinweis auf der Startseite" in `AppSettingsSection` macht das Ausblenden umkehrbar. Weil die Startseite in einem Pager liegt und beim Zurückwechseln nicht neu aufgebaut wird, liegt der Zustand als beobachtbarer `RcsHintVisibility`-Singleton vor; die verschlüsselten Preferences bleiben die Quelle der Wahrheit. Ohne diesen Schalter wäre „Verstanden" eine Sackgasse.
* **Nicht blockierend:** kein Dialog, keine Systembenachrichtigung, keine Wiederholung nach dem Ausblenden. Der Hinweis darf die Kernfunktion nie überlagern.
* **Barrierefreiheit:** Text und Icon, nie nur Farbe; Touch-Ziele mindestens 48 dp; Prüfung mit großer Schrift und TalkBack.

### 3. Dokumentation

* **`README.md`:** ein Absatz im Funktionsumfang – „Leitet SMS weiter. RCS-Chats aus Google Messages und MMS werden von Android nicht an Dritt-Apps ausgeliefert und können nicht weitergeleitet werden; auf dem weiterleitenden Gerät sollte RCS deaktiviert werden." Erwartung vorab setzen statt im Support korrigieren.
* **`docs/ANDROID_API_ANALYSIS.md`:** Abschnitt „RCS und Dritt-App-Zugriff" mit den technischen Befunden aus Anhang A. Das ist der richtige Ort für die API-Analyse.
* **Changelog/Release Notes:** Hinweis als Verbesserung der Nutzerführung aufführen, nicht als neues Feature.
* **`docs/GOOGLE_PLAY_CHECKLIST.md`:** unverändert – es kommt keine Berechtigung hinzu, die Data-Safety-Angaben ändern sich nicht. Ausdrücklich vermerken, dass dies eine Folge der Entscheidung gegen den Benachrichtigungszugriff ist.

## Textentwürfe

Alle Texte in DE (`values-de`) und EN (`values`). Entwurf DE:

**Hinweiskarte:**

> Sie nutzen Google Messages. Falls dort RCS-Chats aktiv sind, werden diese Nachrichten nicht weitergeleitet – Android liefert sie nicht an andere Apps aus. So stellen Sie die Weiterleitung sicher: [Mehr erfahren]

**Hilfe, Einleitung der Rubrik:**

> Kommen einzelne Nachrichten nicht an, liegt das meist an RCS-Chats. Google Messages sendet Nachrichten zwischen Android-Geräten als RCS statt als SMS. Android liefert RCS-Nachrichten aus Sicherheitsgründen nicht an andere Apps aus – auch nicht mit zusätzlichen Berechtigungen. Diese App kann sie deshalb nicht weiterleiten. Wenn Sie RCS auf diesem Gerät deaktivieren, senden Ihre Kontakte automatisch wieder klassische SMS, und die Weiterleitung erfasst alle Textnachrichten.

## Tests und Abnahmekriterien

Der Umfang ist bewusst klein, weil keine Nachrichtenverarbeitung verändert wird.

* **Compose-Test `RcsHintCard`:** je ein Fall für die drei Zustände (Standard-App, installiert aber nicht Standard, nicht installiert) mit dem jeweils passenden Text; unsichtbar nach „Verstanden"; Persistenz über Neustart; Navigation zur Hilfe. Der `PackageManager`-Zugriff ist dafür hinter einer schmalen, testbaren Abstraktion zu kapseln.
* **Manifest-Test:** `<queries>`-Eintrag vorhanden; `getPackageInfo()` wirft bei fehlendem Paket keine unbehandelte `NameNotFoundException`.
* **Unit-Test:** `rcsHintDismissed` in `SharedPreferencesManager` inklusive Standardwert `false`.
* **Ressourcen-Test:** alle neuen Keys in DE und EN vorhanden, keine ungenutzten Keys.
* **Manuelle Prüfung:** Hilfe-Rubrik in beiden Sprachen, hell und dunkel, mit großer Schrift und TalkBack.
* **Regression:** keine – an `SmsReceiver`, `SmsForegroundService` und `PhoneSmsUtils` wird nichts geändert. Die bestehenden Tests müssen unverändert grün bleiben; ist das nicht der Fall, wurde zu viel angefasst.

**Definition of Done**

* Keine neue Berechtigung im `AndroidManifest.xml`. Die einzige Manifest-Änderung ist der `<queries>`-Eintrag, der keine Berechtigung darstellt.
* Kein Codepfad liest fremde Benachrichtigungen.
* Nachrichtenverarbeitung unverändert; bestehende Tests unverändert grün.
* Hilfe, Hinweiskarte, README und API-Analyse sind DE/EN aktuell und benennen RCS **und** MMS als Grenze.
* Kein Text behauptet einen RCS-Status, den die App nicht kennt.

## Nebenbefund außerhalb dieses Themas

`targetSdk` steht auf 35. Google Play verlangt bis **31. August 2026** das Erreichen des aktuellen Target-API-Levels. Diese Anhebung ist unabhängig von diesem Plan einzuplanen und zeitlich dringend.

---

## Anhang A: Geprüfte und verworfene Option – Weiterleitung über `NotificationListenerService`

Dokumentiert, damit die Analyse erhalten bleibt und die Option nicht ohne neue Erkenntnisse erneut aufgegriffen wird.

**Idee.** Ein `NotificationListenerService` liest Benachrichtigungen von `com.google.android.apps.messaging` und leitet daraus erkannte eingehende Nachrichten in die bestehende Pipeline weiter. Nutzer erteilt den Benachrichtigungszugriff optional in den Android-Einstellungen.

**Warum verworfen – die fünf harten Befunde:**

1. **RCS ist aus einer Benachrichtigung nicht positiv erkennbar.** Google Messages postet für SMS, MMS und RCS dieselbe `MessagingStyle`-Benachrichtigung. Es existiert kein öffentliches, stabiles Unterscheidungsfeld. Die RCS-APIs unter `android.telephony.ims` sind `@hide`/`@SystemApi` und stehen Dritt-Apps nicht zur Verfügung. Eine Erkennung wäre nur über lokalisierte UI-Texte oder unbestätigte private Extras möglich – beides untragbar fragil.
   *Der einzige tragfähige Ausweg wäre eine Erkennung per Ausschluss gewesen: Eine Benachrichtigung ohne passenden Fingerprint einer soeben empfangenen SMS ist eine Nicht-SMS-Nachricht. Das setzt einen Quarantänepuffer von einigen Sekunden gegen Race-Bedingungen, ein Präfix-Matching gegen gekürzte Benachrichtigungstexte und eine Kurzzeit-Registry empfangener SMS voraus – ein erheblicher Apparat, dessen Fehlerfall doppelte, kostenpflichtige SMS sind.*
2. **Android 15 redigiert Einmalcodes.** Als sensibel eingestufte Benachrichtigungen werden nicht privilegierten Listenern nur als Platzhalter zugestellt; die nötige Berechtigung `RECEIVE_SENSITIVE_NOTIFICATIONS` ist auf System-signierte Apps und bestimmte Rollen beschränkt. Der Hauptanwendungsfall wäre auf aktuellen Android-Versionen nicht bedienbar.
3. **Aus dem Listener darf kein Foreground Service gestartet werden.** Der SMS-Broadcast stellt die App kurzzeitig auf die Power-Management-Allowlist – deshalb funktioniert `startForegroundService()` in `SmsReceiver`. Ein Notification-Listener-Callback steht nicht auf der Ausnahmeliste; der naheliegende Aufbau hätte `ForegroundServiceStartNotAllowedException` ausgelöst. Es wäre ein Umbau der Pipeline auf einen vom Service unabhängigen Prozessor nötig gewesen.
4. **`onNotificationPosted` feuert auch bei Aktualisierungen**, und `EXTRA_MESSAGES` enthält dann die gesamte in der Benachrichtigung geführte Historie. Ohne ein Wasserzeichen pro `sbn.key` wäre bei jedem neuen Chat-Beitrag der komplette Verlauf erneut weitergeleitet worden.
5. **Länge, Kosten und Play-Risiko.** `forwardSmsWithSubscription()` verwirft heute Nachrichten über 1530 Zeichen stillschweigend; RCS-Nachrichten sind deutlich länger und emoji-lastig, wobei Emojis UCS-2 erzwingen und die Segmentlänge auf 67 Zeichen senken. Ein aktiver Gruppenchat hätte ohne Ratenbremse unbegrenzt SMS-Kosten erzeugt. Zusätzlich bewertet Google Play den Zugriff auf sensible Informationen danach, ob er für eine beworbene Kernfunktion erforderlich ist – Benachrichtigungszugriff als Zusatzfunktion ist ein reales Ablehnungsrisiko.

**Unter welchen Bedingungen neu zu bewerten:** wenn Google eine öffentliche RCS-API für Nachrichten-Apps bereitstellt, oder wenn sich der Anwendungsfall so verschiebt, dass RCS auf dem weiterleitenden Gerät zwingend aktiv bleiben muss. Beides ist derzeit nicht absehbar.

## Anhang B: Warum der RCS-Status nicht programmatisch ermittelbar ist

Vier Wege geprüft, alle für eine normale Drittanbieter-App verschlossen. Dokumentiert, damit die Frage nicht erneut untersucht wird.

| Weg | Ergebnis |
|---|---|
| `ImsRcsManager` / `RcsUceAdapter` (`android.telephony.ims`, API 30+) | Öffentlich dokumentiert, aber sämtliche aussagekräftigen Methoden verlangen `READ_PRIVILEGED_PHONE_STATE`, `READ_PRECISE_PHONE_STATE` oder Carrier Privileges. Diese Berechtigungen sind auf System-signierte bzw. vom Netzbetreiber autorisierte Apps beschränkt und für diese App nicht erreichbar. |
| `RcsMessageStore` / `content://rcs` | In Android 10 angelegt, nie freigegeben, `@hide`. Kein Bestandteil des öffentlichen SDK. |
| `CarrierConfigManager` | Liefert allenfalls, ob der Netzbetreiber RCS-Provisioning verlangt – nicht, ob RCS auf dem Gerät registriert oder in Google Messages eingeschaltet ist. Zudem seit Android 12 für Dritt-Apps gefiltert. |
| Google Messages selbst | Exportiert keinen Content Provider und keine API zum RCS-Status. Der einzige Auswertungspunkt wäre der verworfene Benachrichtigungszugriff – und der zeigt nur, dass etwas ankam, nicht ob RCS aktiv ist. |

**Zusätzlicher, oft übersehener Punkt:** Selbst mit privilegiertem Zugriff wäre die Antwort nicht die gesuchte. Die IMS-APIs beschreiben den RCS-Stack des Netzbetreibers. Google Messages nutzt jedoch überwiegend Googles Jibe-Backend über die Datenverbindung. Ein „IMS-RCS nicht registriert" schließt einen aktiven RCS-Chat in Google Messages also nicht aus.

**Folgerung für die Umsetzung:** Der einzige verlässliche Detektor ist der Nutzer selbst. Deshalb steht in der Hilfe-Rubrik eine Prüfanleitung (Baustein 1, Punkt 2), und die Hinweiskarte wertet ausschließlich aus, welche Nachrichten-App installiert bzw. als Standard gesetzt ist.

## Quellen

* Google Messages – RCS-Chats FAQ (DE): https://support.google.com/messages/answer/9487020?hl=de
* Google – RCS für eine Rufnummer serverseitig deaktivieren: https://messages.google.com/disable-chat
* Android 15 – Verhaltensänderungen für alle Apps (Schutz sensibler Benachrichtigungen): https://developer.android.com/about/versions/15/behavior-changes-all
* Einschränkungen beim Start von Foreground Services aus dem Hintergrund: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
* Android `NotificationListenerService`: https://developer.android.com/reference/android/service/notification/NotificationListenerService
* Play – Berechtigungen und APIs für sensible Informationen: https://support.google.com/googleplay/android-developer/answer/16558241
* Google – RCS Messages Archival (nur vollständig verwaltete Enterprise-Geräte): https://developer.android.com/work/dpc/rcs-messages-archival
* Android `ImsRcsManager` (privilegierte Berechtigungen): https://developer.android.com/reference/android/telephony/ims/ImsRcsManager
* Paketsichtbarkeit ab Android 11 (`<queries>`): https://developer.android.com/training/package-visibility
* Samsung – Einstellung von Samsung Messages zum 6. Juli 2026: https://www.samsung.com/us/support/troubleshoot/TSG10010566/
