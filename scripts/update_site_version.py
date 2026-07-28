#!/usr/bin/env python3
"""Haelt die Versionsangabe der Projektseite (index.html) aktuell.

Quelle der Wahrheit ist ``versionName`` aus ``app/build.gradle.kts``. Das Skript
ersetzt damit die Zeile ``<p>Version: ...</p>`` in ``index.html``.

Der Download-Button auf der Seite wird bewusst NICHT angefasst: Er zeigt auf
``/releases/latest/download/app-release.apk`` und leitet dadurch immer auf das
neueste Release um. Die Versionszeile ist der einzige Ort, der pro Release
nachgezogen werden muss.

Aufruf lokal (vor dem Anlegen eines Releases):

    python scripts/update_site_version.py --tag V4.1.0

Ohne ``--tag`` wird nur der versionName eingetragen. ``--check`` aendert nichts
und meldet ueber den Exit-Code, ob die Seite aktuell ist (0 = aktuell,
1 = veraltet) - nutzbar in einer Pruefung vor dem Release.
"""

from __future__ import annotations

import argparse
import io
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
GRADLE_FILE = REPO_ROOT / "app" / "build.gradle.kts"
SITE_FILE = REPO_ROOT / "index.html"

VERSION_NAME_PATTERN = re.compile(r'versionName\s*=\s*"([^"]+)"')
SITE_VERSION_PATTERN = re.compile(r"(<p>Version:\s*)([^<]*)(</p>)")


def read_version_name() -> str:
    """Liest versionName aus der Gradle-Konfiguration."""
    if not GRADLE_FILE.exists():
        sys.exit(f"Nicht gefunden: {GRADLE_FILE}")

    match = VERSION_NAME_PATTERN.search(GRADLE_FILE.read_text(encoding="utf-8"))
    if not match:
        sys.exit(f"versionName nicht gefunden in {GRADLE_FILE}")
    return match.group(1).strip()


def build_label(version_name: str, tag: str | None) -> str:
    """Erzeugt den anzuzeigenden Text, z. B. 'Barracuda 4.1.0 (V4.1.0)'."""
    tag = (tag or "").strip()
    return f"{version_name} ({tag})" if tag else version_name


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", help="Release-Tag, z. B. V4.1.0")
    parser.add_argument(
        "--check",
        action="store_true",
        help="Nur pruefen, nichts schreiben. Exit 1, wenn die Seite veraltet ist.",
    )
    args = parser.parse_args()

    if not SITE_FILE.exists():
        sys.exit(f"Nicht gefunden: {SITE_FILE}")

    desired = build_label(read_version_name(), args.tag)

    # newline="" erhaelt die vorhandenen Zeilenenden (die Datei nutzt CRLF).
    with io.open(SITE_FILE, encoding="utf-8", newline="") as handle:
        content = handle.read()

    match = SITE_VERSION_PATTERN.search(content)
    if not match:
        sys.exit(
            f"Versionszeile nicht gefunden in {SITE_FILE.name}. "
            'Erwartet wird ein Abschnitt der Form "<p>Version: ...</p>".'
        )

    current = match.group(2).strip()
    if current == desired:
        print(f"index.html ist bereits aktuell: {desired}")
        return 0

    if args.check:
        print(f"index.html ist veraltet: '{current}' -> erwartet '{desired}'")
        return 1

    updated = SITE_VERSION_PATTERN.sub(
        lambda m: f"{m.group(1)}{desired}{m.group(3)}", content, count=1
    )
    with io.open(SITE_FILE, "w", encoding="utf-8", newline="") as handle:
        handle.write(updated)

    print(f"index.html aktualisiert: '{current}' -> '{desired}'")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
