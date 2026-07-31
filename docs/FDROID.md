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

Das GitLab-Konto wurde für CI verifiziert. Die danach manuell gestartete Fork-Pipeline für
`ef7c1f8c` ist erfolgreich durchgelaufen. Die frühere Merge-Request-Pipeline
`#2720019186` wurde noch vor der Verifizierung erstellt, enthält deshalb keine Jobs und
bleibt fehlgeschlagen. Der F-Droid-Reviewer wurde am 31.07.2026 gebeten, eine neue
Merge-Request-Pipeline auszulösen. Bis zu deren Ergebnis bleibt der Merge Request offen;
er darf nicht selbst zusammengeführt werden.

## Voraussetzungen

* Ein veröffentlichter Git-Tag, der exakt auf dem Release-Commit liegt. Für die erste
  Einreichung ist das aktuell `V5.2.1` mit `versionCode` 9. Der Tag dient der
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

RepoType: git
Repo: https://github.com/meuse24/forwarderA1.git
Binaries: https://github.com/meuse24/forwarderA1/releases/download/V%v/app-release.apk

Builds:
  - versionName: Barracuda 5.2.1
    versionCode: 9
    commit: 9a68b8569aff8fcf3d5b2f49703a9a79b782be76
    subdir: app
    gradle:
      - yes

AllowedAPKSigningKeys: dff4458848d98fdbc005e47159d1507cf22c58b47600ef094ab3e08b99f1c72c

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: Barracuda 5.2.1
CurrentVersionCode: 9
```

YAML ist einrückungssensitiv: Alle Felder auf oberster Ebene (`Categories`, `License`,
`Builds` usw.) müssen in Spalte 1 beginnen. Die Datei muss mit einem Zeilenumbruch enden.

`Binaries` steht unmittelbar unter `Repo` und verweist auf die APK des jeweiligen,
unveränderlichen GitHub-Releases. F-Droid ersetzt `%v` durch die Release-Version und
vergleicht die APK mit dem selbst gebauten Ergebnis. Die URL darf deshalb weder auf
`latest` zeigen noch als feste Versions-URL eingetragen werden.

`AllowedAPKSigningKeys` ist der kleingeschriebene SHA-256-Fingerabdruck ohne Doppelpunkte
des Zertifikats, mit dem die GitHub-Release-APK signiert wurde. Der oben eingetragene Wert
wurde aus `keyandroid.jks` ermittelt. Bei einem späteren Wechsel des Release-Schlüssels
muss der neue Fingerabdruck ergänzt werden, bevor die damit signierte APK übernommen wird.

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
