# Dialog Style Guide

## Button-Richtlinien
- Reihenfolge: `[Tertiary] [Secondary] [Primary]`, Confirm immer rechts.
- Spacing zwischen Buttons: 8dp. Max. 3 Buttons.
- Farben: Primary = `MaterialTheme.colorScheme.primary`; Cancel als TextButton/Tertiary; Destructive = `colorScheme.error`.
- Beispiel (Confirm + Cancel):
  ```
  [Cancel] [Confirm]
  ```

## Typography
- Title: `MaterialTheme.typography.headlineSmall` + `FontWeight.Bold`.
- Body: `typography.bodyMedium`.
- Hints/Fehler: `typography.bodySmall`; Fehler in `colorScheme.error`.

## Spacing, Padding, Shapes (DialogDefaults)
- Spacing: 8dp (Compact), 12dp (Standard), 16dp (Large), 24dp (XL).
- Padding: 12dp, 16dp, 24dp, 32dp.
- CornerRadius: 16dp (Standard), 24dp (Large), 32dp (Fullscreen).
- Icon-Größen: 24dp, 48dp, 80dp, 100dp.
- Elevation: 8dp (Standard), 24dp (Fullscreen).
- DialogProperties: `Critical` (kein Dismiss), `Standard` (Back/Outside erlaubt), `Fullscreen` (kein Platform-Width).

## Accessibility
- Alle Icons mit `contentDescription` (siehe `cd_*` Strings).
- Dialog-Container mit `semantics { role = Role.Dialog }`; kritische Dialoge ggf. `liveRegion = Assertive`.
- Fokus-Reihenfolge prüfen; IME-Actions für Inputs setzen.
- Keine rein dekorativen Icons ohne explizit `null` gesetzte Beschreibung.

## Beispiele (Pattern)
- AlertDialog: Nutzt `AppAlertDialog` + `DialogDefaults.CornerShape`, Buttons rechts ausgerichtet.
- Fullscreen: Nutzt `AppFullscreenDialog`, Gradient optional, CornerRadius 32dp auf Cards.
- Countdown/Animation: Verwende `rememberCountdown`, `rememberPulseAnimation`, `rememberGlowAnimation`; kein Duplikat-Code.

## Strings & Ressourcen
- Keine Hardcoded-Texte in Dialogen; immer `stringResource`.
- Generische Buttons: `btn_ok`, `btn_cancel`, `btn_save`, `btn_retry`, `btn_ignore`, `btn_skip`.
- ContentDescriptions: `cd_warning_icon`, `cd_error_icon`, `cd_info_icon`, `cd_exit_icon`, `cd_officer_logo`.

## Migration-Kurzcheck (pro Dialog)
- [ ] DialogDefaults für Padding/Shapes/Elevation genutzt
- [ ] Buttons in Standard-Reihenfolge, max. 3
- [ ] Icons mit `contentDescription`
- [ ] Strings aus Ressourcen
- [ ] Countdown/Animation über Utils statt Inline-Code
- [ ] DialogProperties passend (Critical/Standard/Fullscreen)
