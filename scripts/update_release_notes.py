#!/usr/bin/env python3
"""Ergaenzt die Release-Beschreibung um Angaben zur Pruefung der APK.

Berechnet den SHA-256 der Release-APK und schreibt zusammen mit dem
Herausgeber-Fingerprint einen Abschnitt "Echtheit der APK pruefen" in die
Release-Notes.

Der Abschnitt liegt zwischen Markern und wird bei wiederholtem Lauf ersetzt
statt angehaengt. Ein bereits von Hand eingefuegter Abschnitt ohne Marker wird
erkannt und mit uebernommen, damit keine Dubletten entstehen.

Der bestehende Text der Release-Notes bleibt unveraendert erhalten.

Aufruf:

    python scripts/update_release_notes.py \\
        --tag V4.1.0 --apk app-release.apk \\
        --notes-in notes.md --notes-out notes.new.md

Ohne ``--notes-out`` wird das Ergebnis auf die Standardausgabe geschrieben.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import re
import sys
from pathlib import Path

# Fingerprint des Signaturzertifikats. Konstant ueber alle Releases - er haengt am
# Schluessel (keyandroid.jks), nicht an der einzelnen APK. Nur bei einem
# Schluesselwechsel anzupassen; ein solcher Wechsel wuerde allerdings alle
# bestehenden Installationen von Updates ausschliessen, da Android nur Updates mit
# identischer Signatur zulaesst.
SIGNER_DN = "CN=Günther Meusburger"
SIGNER_SHA256 = (
    "DF:F4:45:88:48:D9:8F:DB:C0:05:E4:71:59:D1:50:7C:"
    "F2:2C:58:B4:76:00:EF:09:4A:B3:E0:8B:99:F1:C7:2C"
)

MARKER_START = "<!-- apk-verification:start -->"
MARKER_END = "<!-- apk-verification:end -->"
SECTION_HEADING = "## Echtheit der APK prüfen"

REPO_URL = "https://github.com/meuse24/forwarderA1"


def sha256_of(path: Path) -> str:
    """Berechnet den SHA-256 einer Datei speicherschonend."""
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build_section(apk_sha256: str) -> str:
    """Erzeugt den Pruefabschnitt inklusive Marker."""
    return f"""{MARKER_START}
---

{SECTION_HEADING}

Diese App wird nicht über Google Play verteilt, daher bürgt kein Store für die Herkunft der Datei. Da die App Zugriff auf SMS hat, lohnt sich die Prüfung.

**Prüfsumme dieser Datei** (`app-release.apk`):

```
SHA-256: {apk_sha256}
```

```bash
# Windows
certutil -hashfile app-release.apk SHA256
# Linux / macOS
sha256sum app-release.apk
```

**Signatur des Herausgebers** – für alle Releases identisch:

```
{SIGNER_DN}
SHA-256: {SIGNER_SHA256}
```

```bash
apksigner verify --print-certs app-release.apk
```

Weichen die Werte ab, stammt die Datei nicht aus diesem Projekt und sollte nicht installiert werden. Ausführliche Anleitung: Abschnitt „Installation der signierten App" in der [README]({REPO_URL}#installation-der-signierten-app).
{MARKER_END}"""


def strip_existing_section(notes: str) -> str:
    """Entfernt einen bereits vorhandenen Pruefabschnitt.

    Beruecksichtigt sowohl den markierten Block als auch eine aeltere, von Hand
    eingefuegte Fassung ohne Marker.
    """
    # 1) Markierter Block
    marked = re.compile(
        re.escape(MARKER_START) + r".*?" + re.escape(MARKER_END),
        re.DOTALL,
    )
    notes = marked.sub("", notes)

    # 2) Aeltere Fassung ohne Marker: ab der Ueberschrift bis zur naechsten
    #    Ueberschrift gleicher Ebene oder bis zum Ende.
    legacy = re.compile(
        r"(?:\n---\s*\n)?\n*" + re.escape(SECTION_HEADING) + r".*?(?=\n## |\Z)",
        re.DOTALL,
    )
    notes = legacy.sub("", notes)

    return notes.rstrip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", required=True, help="Release-Tag, z. B. V4.1.0")
    parser.add_argument("--apk", required=True, type=Path, help="Pfad zur Release-APK")
    parser.add_argument(
        "--notes-in", required=True, type=Path, help="Datei mit den bisherigen Notes"
    )
    parser.add_argument(
        "--notes-out", type=Path, help="Zieldatei; ohne Angabe Ausgabe auf stdout"
    )
    args = parser.parse_args()

    if not args.apk.exists():
        sys.exit(f"APK nicht gefunden: {args.apk}")
    if not args.notes_in.exists():
        sys.exit(f"Notes-Datei nicht gefunden: {args.notes_in}")

    apk_sha256 = sha256_of(args.apk)

    with io.open(args.notes_in, encoding="utf-8", newline="") as handle:
        notes = handle.read()

    result = strip_existing_section(notes) + "\n\n" + build_section(apk_sha256) + "\n"

    if args.notes_out:
        with io.open(args.notes_out, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(result)
        print(f"Release-Notes fuer {args.tag} vorbereitet: SHA-256 {apk_sha256}")
    else:
        sys.stdout.write(result)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
