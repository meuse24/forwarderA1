# Dialog Migration Guide

## Ziele
- Alte, individuell gestylte Dialoge auf die neuen Basiskomponenten umstellen.
- Duplikate (Countdown/Animation) eliminieren und DialogDefaults nutzen.

## Komponenten-Übersicht
- `AppAlertDialog`: Standarddialog mit DialogDefaults (Shape/Elevation/Properties).
- `AppFullscreenDialog`: Overlay mit Gradient und Fullscreen-Properties.
- `DialogButtons`: Confirm/Dismiss/Destructive Buttons + Spacing-Helfer.
- Utils: `rememberCountdown`, `rememberPulseAnimation`, `rememberGlowAnimation`.

## Migration: Simple AlertDialog → AppAlertDialog
**Alt**
```kotlin
AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Exit") },
    text = { Text("Really exit?") },
    confirmButton = { Button(onClick = onConfirm) { Text("OK") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    shape = RoundedCornerShape(12.dp)
)
```
**Neu**
```kotlin
AppAlertDialog(
    onDismissRequest = onDismiss,
    title = stringResource(R.string.dialog_title_exit_app),
    text = { Text(stringResource(R.string.dialog_body_exit)) },
    confirmButton = { DialogConfirmButton(stringResource(R.string.btn_ok), onConfirm) },
    dismissButton = { DialogDismissButton(stringResource(R.string.btn_cancel), onDismiss) },
)
```
**Anpassungen**
- Shape/Elevation kommen aus DialogDefaults.
- Strings aus Ressourcen; Button-Reihenfolge: Cancel links, Confirm rechts.

## Migration: Fullscreen → AppFullscreenDialog
**Alt**
```kotlin
Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Box(Modifier.fillMaxSize().background(brush)) { /* content */ }
}
```
**Neu**
```kotlin
AppFullscreenDialog(
    onDismissRequest = onDismiss,
    backgroundGradient = customGradient
) {
    // Card + content here, use DialogDefaults.FullscreenCornerRadius
}
```
**Anpassungen**
- Nutze DialogDefaults.FullscreenDialogProperties und FullscreenCornerRadius.
- Animations/Countdown über Utils einbinden.

## Migration: Input Dialog → AppAlertDialog + Patterns
**Alt**: Mehrere `OutlinedTextField` mit manuellem Padding/Corner.

**Neu**
```kotlin
AppAlertDialog(
    onDismissRequest = onDismiss,
    title = stringResource(R.string.dialog_title_pin),
    text = {
        Column(verticalArrangement = Arrangement.spacedBy(DialogDefaults.StandardSpacing)) {
            OutlinedTextField(
                value = pin,
                onValueChange = onPinChange,
                isError = pinError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                supportingText = { if (pinError) Text(stringResource(R.string.error_pin)) }
            )
        }
    },
    confirmButton = { DialogConfirmButton(stringResource(R.string.btn_save), onConfirm, enabled = pinValid) },
    dismissButton = { DialogDismissButton(stringResource(R.string.btn_cancel), onDismiss) }
)
```
**Anpassungen**
- Spacing über DialogDefaults.StandardSpacing.
- Fehlermeldungen über `supportingText`; IME-Actions setzen.

## Deprecated Shims
- `LegacySimpleDialog(...)` → leitet an `AppAlertDialog` weiter (Deprecation WARNING).
- `LegacyFullscreenDialog(...)` → leitet an `AppFullscreenDialog` weiter.

## Migration-Checkliste (pro Dialog)
- [ ] DialogDefaults für Padding/Shapes/Elevation nutzen
- [ ] Buttons in Standard-Reihenfolge, max. 3
- [ ] Icons mit `contentDescription` aus Strings
- [ ] Strings in Ressourcen (keine Hardcodes)
- [ ] Countdown/Animation via Utils statt Inline-Code
- [ ] Passende DialogProperties (Standard/Critical/Fullscreen)
- [ ] Previews aktualisiert und überprüft
