# Veröffentlichung auf F-Droid

Die App-ID lautet `info.meuse24.smsforwarderneoA1`. F-Droid baut jede Version aus dem
getaggten, öffentlichen Quellcode und signiert sie mit einem eigenen Schlüssel. Die über
GitHub Releases verbreitete APK und die F-Droid-APK können deshalb nicht gegenseitig als
Update installiert werden.

## Status der Einreichung (30.07.2026)

Die erste Einreichung wurde vorbereitet und als Merge Request an F-Droid übermittelt:

* Quellcode-Release: `Barracuda 5.2.1` (`versionCode` 9), Git-Tag
  [`V5.2.1`](https://github.com/meuse24/forwarderA1/tree/V5.2.1).
* Store-Metadaten und Screenshots liegen im Quellcode unter
  `fastlane/metadata/android/` auf Deutsch und Englisch.
* F-Droid-Metadaten: `metadata/info.meuse24.smsforwarderneoA1.yml` im persönlichen
  `fdroiddata`-Fork.
* Einreichung: [F-Droid Merge Request !44367](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/44367).

Die GitLab-Pipeline des persönlichen Forks konnte nicht starten, weil GitLab eine
Telefonnummer-Verifizierung verlangt. Es wurden daher keine Build-Jobs ausgeführt. Der
Merge Request enthält einen Hinweis mit der Bitte, dass ein F-Droid-Teammitglied die CI
anstoßen soll. Bis zur Rückmeldung bleibt der Merge Request offen; er darf nicht selbst
zusammengeführt werden.

## Voraussetzungen

* Ein veröffentlichter Git-Tag, der exakt auf dem Release-Commit liegt. Für die erste
  Einreichung ist das aktuell `V5.2.1` mit `versionCode` 9.
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

Builds:
  - versionName: Barracuda 5.2.1
    versionCode: 9
    commit: V5.2.1
    subdir: app
    gradle:
      - yes

CurrentVersion: Barracuda 5.2.1
CurrentVersionCode: 9
```

Die Build-Zeile ist vor dem Merge mit `fdroid build -v -l info.meuse24.smsforwarderneoA1`
oder der CI-Pipeline des Forks zu validieren. Falls F-Droid beim Signieren eine lokale
`signingConfig` beanstandet, muss die Release-Signierung im Gradle-Skript so angepasst
werden, dass sie nur aktiviert wird, wenn alle vier Keystore-Werte vorhanden sind.

## Einreichung

1. Die Metadaten-Datei im Fork committen und pushen.
2. Sicherstellen, dass die Pipeline des Forks erfolgreich ist und eine APK erzeugt.
3. Einen Merge Request an `fdroid/fdroiddata` öffnen und die Vorlage „App inclusion"
   vollständig ausfüllen. Als Urheber kannst du die Zustimmung zur Aufnahme selbst bestätigen.
4. Rückfragen der F-Droid-Packager im Merge Request beantworten.
5. Nach dem Merge den Build-Status und anschließend den Store-Eintrag prüfen.

Für jede spätere Version: `versionCode` erhöhen, Version im Gradle-Skript setzen, einen
unveränderlichen Git-Tag veröffentlichen und den nächsten Eintrag unter `Builds` sowie
`CurrentVersion` und `CurrentVersionCode` aktualisieren. Automatische Updates sollten erst
später mit einer Regel aktiviert werden, die auch das Präfix `Barracuda ` in `versionName`
korrekt berücksichtigt.
