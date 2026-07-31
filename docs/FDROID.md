# Veröffentlichung auf F-Droid

Die App-ID lautet `info.meuse24.smsforwarderneoA1`. F-Droid baut jede Version aus dem
getaggten, öffentlichen Quellcode und signiert sie mit einem eigenen Schlüssel. Die über
GitHub Releases verbreitete APK und die F-Droid-APK können deshalb nicht gegenseitig als
Update installiert werden.

## Status der Einreichung (31.07.2026)

Die erste Einreichung wurde vorbereitet und als Merge Request an F-Droid übermittelt:

* Quellcode-Release: `Barracuda 5.2.1` (`versionCode` 9), Git-Tag
  [`V5.2.1`](https://github.com/meuse24/forwarderA1/tree/V5.2.1) auf Commit
  `9a68b8569aff8fcf3d5b2f49703a9a79b782be76`.
* Store-Metadaten und Screenshots liegen im Quellcode unter
  `fastlane/metadata/android/` auf Deutsch und Englisch.
* F-Droid-Metadaten: `metadata/info.meuse24.smsforwarderneoA1.yml` im persönlichen
  `fdroiddata`-Fork.
* Einreichung: [F-Droid Merge Request !44367](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/44367).
* Review vom 30.07.2026: Der bestehende Build-Eintrag muss den vollständigen Commit-Hash
  statt des Tags verwenden; die Metadaten müssen automatische Updates aktivieren.
* Umgesetzt im Fork: `commit` verweist auf
  `9a68b8569aff8fcf3d5b2f49703a9a79b782be76`, `UpdateCheckMode: Tags` und
  `AutoUpdateMode: Version` sind gesetzt.
* Die korrigierte Metadaten-Datei wurde mit Commit `ef7c1f8c`
  (`Fix F-Droid metadata YAML indentation`) gepusht. Dabei wurden die YAML-Einrückung,
  eine versehentlich umgebrochene `Changelog`-URL und der abschließende Zeilenumbruch
  korrigiert.
* Nachtrag des Reviewers vom 31.07.2026: Die veröffentlichte APK zur reproduzierbaren
  Binary-Verifikation, ihr erlaubter Signaturschlüssel und die generische Binary-URL wurden
  ergänzt.
* Die F-Droid-Pipeline für `V5.2.1` scheitert beim Quellcode-Scan am Foojay-Toolchain-
  Resolver. Dieser ist für das nächste Patch-Release entfernt; `V5.2.1` bleibt wegen seines
  unveränderlichen Commit-Hashes nicht nachträglich reparierbar.
* Als Ersatz ist `V5.2.3` (`versionCode` 11) auf Commit
  `c5cc87cb4556ce64b3b51d5a00b1cf3ae8c0ce5d` in den Metadaten eingetragen. Die numerische
  `versionName` erlaubt die generische Binary-URL mit `%v`; `UpdateCheckData` ist daher nicht
  mehr erforderlich.
* Der Build aus diesem Commit läuft in der F-Droid-CI erfolgreich durch. Der anschließende
  Binary-Vergleich scheiterte zunächst an einer ungültig hochgeladenen Referenz-APK; nach dem
  Neu-Upload meldet `apksigner` in der CI `Verifies` (v2). Damit ist das Signaturproblem erledigt.
* Pipeline `#2721364453` vom 31.07.2026 scheiterte danach am eigentlichen Reproducible-Build-
  Vergleich: F-Droid kopiert die Signatur der Referenz-APK auf das selbst gebaute Ergebnis, und
  dessen Verifikation schlägt fehl, weil der Inhalt abweicht. Der Log nennt genau fünf
  Unterschiede — `META-INF/services/a3.t`, `b3.a` und `javax.mail.Provider` mit CRLF statt LF
  sowie binär abweichende `classes.dex` und `assets/dexopt/baseline.prof`. Die CRLF-Zeilenenden
  sind der Fingerabdruck eines Windows-Builds: R8 schreibt die Service-Dateien mit dem
  Zeilentrenner der Plattform, F-Droid baut unter Linux. Eine unter Windows erzeugte
  Referenz-APK kann deshalb nicht bit-identisch sein.
* Konsequenz: `Binaries` und `AllowedAPKSigningKeys` wurden aus den Metadaten entfernt. F-Droid
  baut und signiert selbst — der Regelfall für Neuaufnahmen. Reproducible Builds lassen sich
  später nachreichen, sobald die Release-APK aus einem Linux-Container stammt.
* Der Job `checkupdates` scheiterte zusätzlich daran, dass `fdroid checkupdates` das Feld
  `AutoName: SMSForwarderNeo A1` ergänzt und der Job anschließend `git diff --exit-code` prüft.
  Das Feld ist jetzt in der Metadaten-Datei eingetragen. `CurrentVersion` wurde von
  `Barracuda 5.2.3` auf den tatsächlichen `versionName` `5.2.3` korrigiert.

Das GitLab-Konto wurde für CI verifiziert. Die danach manuell gestartete Fork-Pipeline für
`ef7c1f8c` ist erfolgreich durchgelaufen. Die frühere Merge-Request-Pipeline
`#2720019186` wurde noch vor der Verifizierung erstellt, enthält deshalb keine Jobs und
bleibt fehlgeschlagen. Der F-Droid-Reviewer wurde am 31.07.2026 gebeten, eine neue
Merge-Request-Pipeline auszulösen. Bis zu deren Ergebnis bleibt der Merge Request offen;
er darf nicht selbst zusammengeführt werden.

## Voraussetzungen

* Ein veröffentlichter Git-Tag, der exakt auf dem Release-Commit liegt. Für den aktuellen
  Einreichungsstand ist das `V5.2.3` mit `versionCode` 11. Der Tag dient der
  Update-Erkennung; der konkrete Build referenziert stets den unveränderlichen vollen
  Commit-Hash.
* Das Repository, der Tag, die MIT-Lizenz und GitHub Issues bleiben öffentlich erreichbar.
* Der Build darf keine lokale Datei, kein Geheimnis und keinen eigenen Signierschlüssel
  voraussetzen. F-Droid stellt Android SDK, JDK und die Signatur selbst bereit.

Die beschreibenden Store-Metadaten liegen in `fastlane/metadata/android/`. Vor der
Einreichung sollen außerdem mindestens zwei echte Gerätescreenshots unter
`fastlane/metadata/android/en-US/images/phoneScreenshots/` ergänzt werden.

## Metadaten für fdroiddata

Forke `https://gitlab.com/fdroid/fdroiddata`, lege den Branch an und erstelle dort die Datei
`metadata/info.meuse24.smsforwarderneoA1.yml` mit diesem Ausgangspunkt:

```yaml
Categories:
  - Connectivity
  - Internet
License: MIT
AuthorName: Günther Meusburger
WebSite: https://meuse24.github.io/forwarderA1/
SourceCode: https://github.com/meuse24/forwarderA1
IssueTracker: https://github.com/meuse24/forwarderA1/issues
Changelog: https://github.com/meuse24/forwarderA1/blob/main/CHANGELOG.md

AutoName: SMSForwarderNeo A1

RepoType: git
Repo: https://github.com/meuse24/forwarderA1.git

Builds:
  - versionName: 5.2.3
    versionCode: 11
    commit: c5cc87cb4556ce64b3b51d5a00b1cf3ae8c0ce5d
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 5.2.3
CurrentVersionCode: 11
```

YAML ist einrückungssensitiv: Alle Felder auf oberster Ebene (`Categories`, `License`,
`Builds` usw.) müssen in Spalte 1 beginnen. Die Datei muss mit einem Zeilenumbruch enden.

`AutoName` muss eingetragen sein und exakt dem Wert entsprechen, den `fdroid checkupdates`
aus dem Manifest liest (`SMSForwarderNeo A1`). Der CI-Job `checkupdates` führt das Werkzeug
aus und prüft danach mit `git diff --exit-code`; ein fehlendes oder abweichendes Feld lässt
ihn fehlschlagen.

`CurrentVersion` muss dem `versionName` aus `app/build.gradle.kts` entsprechen — also `5.2.3`,
nicht `Barracuda 5.2.3`. Ein nicht-numerischer `versionName` war schon der Grund, warum die
frühere `Binaries`-URL mit `%v` ins Leere lief.

## Reproducible Builds: derzeit bewusst nicht aktiviert

Mit `Binaries` und `AllowedAPKSigningKeys` würde F-Droid die selbst gebaute APK mit der
GitHub-Release-APK vergleichen und bei Gleichheit die vom Entwickler signierte Datei
verteilen. Beide Felder sind entfernt, weil dieser Vergleich mit einer unter **Windows**
gebauten Referenz-APK nicht gelingen kann: R8 schreibt `META-INF/services/*` mit dem
Zeilentrenner der Plattform, also CRLF statt LF, und auch `classes.dex` sowie
`assets/dexopt/baseline.prof` weichen ab. F-Droid baut und signiert daher selbst — das ist
der Regelfall und für die Aufnahme ausreichend.

Wer den Vergleich später aktivieren will, muss die Release-APK in einem Linux-Container mit
derselben Toolchain (JDK 17, Gradle-Wrapper des Projekts) bauen, sie als
`app-release.apk` an das unveränderliche GitHub-Release hängen und beide Felder wieder
eintragen. `AllowedAPKSigningKeys` ist dabei der kleingeschriebene SHA-256-Fingerabdruck
ohne Doppelpunkte des Zertifikats aus `keyandroid.jks`
(`dff4458848d98fdbc005e47159d1507cf22c58b47600ef094ab3e08b99f1c72c`).

Unabhängig davon gilt für jede veröffentlichte GitHub-APK: Nach dem Gradle-Build darf sie
nicht mehr verändert werden. `zipalign` muss **vor** `apksigner` laufen; ein nachträgliches
Auspacken, Neu-Packen oder `zipalign` zerstört die v2-Signatur. Bei einer bereits
hochgeladenen, ungültigen Datei genügt kein Überschreiben — das Asset zuerst löschen, dann
die neue Datei hochladen. Prüfung vor dem Upload:

```powershell
./gradlew.bat clean assembleRelease
$apk = 'app/build/outputs/apk/release/app-release.apk'
apksigner verify --verbose --print-certs $apk
Get-FileHash $apk -Algorithm SHA256
```

Für `V5.2.3` ergab der geprüfte lokale Build vom Release-Commit den SHA-256-Wert
`9D78DB68D48E6B1653A8FFCF4EA244FE8631335AA63CA343DC438F00280D7E3A`; das GitHub-Asset trägt
denselben Digest.

Die Build-Zeile ist vor dem Merge mit `fdroid build -v -l info.meuse24.smsforwarderneoA1`
oder einer von F-Droid ausgelösten Merge-Request-Pipeline zu validieren. Eine erfolgreich
gestartete Fork-Pipeline bestätigt zunächst nur ihre eigenen Prüfungen; sie ersetzt keine
fehlgeschlagene oder nicht gestartete Merge-Request-Pipeline. `UpdateCheckMode: Tags`
prüft die veröffentlichten Git-Tags; mit `AutoUpdateMode: Version` erzeugt F-Droid neue
Build-Einträge automatisch. Falls F-Droid beim Signieren eine lokale
`signingConfig` beanstandet, muss die Release-Signierung im Gradle-Skript so angepasst
werden, dass sie nur aktiviert wird, wenn alle vier Keystore-Werte vorhanden sind.

## Einreichung

1. Die Metadaten-Datei im Fork committen und pushen.
2. GitLab-CI gegebenenfalls durch die Konto-Verifizierung freischalten und die
   Fork-Pipeline ausführen.
3. Einen Merge Request an `fdroid/fdroiddata` öffnen und die Vorlage „App inclusion"
   vollständig ausfüllen. Als Urheber kannst du die Zustimmung zur Aufnahme selbst bestätigen.
4. Rückfragen der F-Droid-Packager im Merge Request beantworten. Kann eine frühere
   Merge-Request-Pipeline wegen fehlender Verifizierung keine Jobs enthalten, nach der
   Verifizierung einen F-Droid-Maintainer um das erneute Auslösen der MR-Pipeline bitten.
5. Nach dem Merge den Build-Status und anschließend den Store-Eintrag prüfen.

Für jede spätere Version: `versionCode` erhöhen, Version im Gradle-Skript setzen und einen
unveränderlichen Release-Tag veröffentlichen. F-Droid erkennt daraus mit der aktivierten
Update-Regel die neue Version und erstellt den nächsten Build-Eintrag. Vor dem Merge sind
der daraus erzeugte Commit-Wert sowie `CurrentVersion` und `CurrentVersionCode` zu prüfen.
Für manuell ergänzte oder korrigierte Build-Einträge ist stets der vollständige Commit-Hash
zu verwenden.
