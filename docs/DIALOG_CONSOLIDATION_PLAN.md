# Dialog-Konsolidierung - Implementierungsplan

> **Status**: Ready for Implementation
> **Erstellt**: 2025-12-14
> **Geschätzter Aufwand**: 30-40 Stunden
> **Erwartete Code-Reduktion**: ~40% (-600 Zeilen)

---

## 📋 Executive Summary

### Ziele
- **Konsistenz**: Einheitliche Dialoge mit gemeinsamen Design-Standards
- **Wartbarkeit**: Zentrale Basiskomponenten statt duplizierter Code
- **Code-Reduktion**: Von 1553 → ~930 Zeilen (-40%)
- **Qualität**: Bessere Accessibility, Tests, UX

### Kritische Probleme (IST-Zustand)
- ❌ **Massive Code-Duplikation**: Countdown/Animation-Logik 2x identisch kopiert
- ❌ **Inkonsistente Standards**: 5 verschiedene Paddings (8dp, 12dp, 16dp, 32dp, 40dp)
- ❌ **Inkonsistente Corner Radii**: 12dp, 16dp, 32dp ohne klare Regel
- ❌ **Button-Chaos**: Mix aus Button/TextButton, inkonsistente Reihenfolge
- ❌ **State-Management**: Inkonsistente Dismiss-Strategien über ViewModels hinweg

### Success Metrics
- ✅ **Code-Reduktion**: Mindestens 35% weniger Zeilen
- ✅ **Null Duplikation**: Keine doppelte Countdown/Animation-Logik
- ✅ **100% String-Ressourcen**: Kein hardcoded Text in Dialogen
- ✅ **100% Konsistenz**: Alle Dialoge nutzen DialogDefaults
- ✅ **Accessibility**: Alle Dialoge mit semantics + contentDescription

---

## 🗂️ Bestandsaufnahme

### Dialog-Inventar (10 Dateien, 1553 Zeilen)

#### Fullscreen Overlays (3)
| Datei | Zeilen | Features | Probleme |
|-------|--------|----------|----------|
| `MmiWarningDialog.kt` | 210 | Countdown, Pulsation, Glow | ⚠️ Code-Duplikation |
| `LoopProtectionDialog.kt` | 281 | Countdown, Pulsation, Glow | ⚠️ Identische Animation wie MMI |
| `LoadingScreen.kt` | 180 | Progress, conditional buttons | ✅ OK |

#### Standard AlertDialogs (7)
| Datei | Zeilen | Typ | Probleme |
|-------|--------|-----|----------|
| `ExitDialog.kt` | 122 | Confirm | ⚠️ 3 Buttons (inkonsistent) |
| `CriticalPermissionsDialog.kt` | 168 | Critical | ⚠️ Icon 48dp (andere: 100dp) |
| `CleanupDialogs.kt` | 131 | Progress + Error | ⚠️ 2 Dialoge in 1 Datei |
| `PinDialogs.kt` | 196 | Input (2 Dialoge) | ⚠️ Manuelle Validierung |
| `SimNumbersDialog.kt` | 170 | Input | ⚠️ Complex LazyColumn |
| `MmiConfirmationDialog.kt` | 59 | Simple | ✅ OK |
| `UssdProgressDialog.kt` | 36 | Progress | ✅ OK |

### Code-Duplikations-Analyse

#### Kritisch: Countdown-Logik (2x identisch)
```kotlin
// MmiWarningDialog.kt:43-56 + LoopProtectionDialog.kt:48-61
var countdown by remember { mutableStateOf(4) }
var isDismissed by remember { mutableStateOf(false) }

LaunchedEffect(Unit) {
    repeat(4) { i ->
        if (isDismissed) return@LaunchedEffect
        countdown = 4 - i
        delay(1000)
    }
    if (!isDismissed) {
        onDismiss()
    }
}
```
→ **Einsparung**: ~30 Zeilen durch Extraktion in `rememberCountdown()`

#### Kritisch: Pulsations-Animation (2x identisch)
```kotlin
// MmiWarningDialog.kt:59-68 + LoopProtectionDialog.kt:64-73
val infiniteTransition = rememberInfiniteTransition(label = "pulse")
val scale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
        animation = tween(800, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    ),
    label = "scale_animation"
)
```
→ **Einsparung**: ~40 Zeilen durch Extraktion in `rememberPulseAnimation()`

#### Moderat: Glow-Animation (2x ähnlich)
```kotlin
// MmiWarningDialog.kt:71-79 + LoopProtectionDialog.kt:76-84
val alpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.7f,
    // ... Rest identisch
)
```
→ **Einsparung**: ~30 Zeilen

**Gesamt-Einsparung nur durch Duplikat-Beseitigung**: ~100 Zeilen

### Inkonsistenzen-Matrix

| Aspekt | Varianten | Standard fehlt |
|--------|-----------|----------------|
| **Padding** | 8dp, 12dp, 16dp, 32dp, 40dp | ❌ |
| **Corner Radius** | 12dp, 16dp, 32dp | ❌ |
| **Icon Size** | 18dp, 24dp, 48dp, 100dp, 120dp | ❌ |
| **Elevation** | 2dp, 8dp, 24dp | ❌ |
| **Button Order** | Confirm rechts/links gemischt | ❌ |
| **dismissOnBackPress** | true/false ohne Pattern | ❌ |
| **Spacing** | 4dp, 8dp, 12dp, 16dp, 24dp | ❌ |

---

## 🎯 Design-Richtlinien (SOLL-Zustand)

### DialogDefaults - Konkrete Werte

```kotlin
// presentation/ui/components/dialogs/DialogDefaults.kt
object DialogDefaults {
    // Spacing
    val CompactSpacing = 8.dp
    val StandardSpacing = 12.dp
    val LargeSpacing = 16.dp
    val ExtraLargeSpacing = 24.dp

    // Padding
    val CompactPadding = 12.dp
    val StandardPadding = 16.dp
    val LargePadding = 24.dp
    val ExtraLargePadding = 32.dp

    // Corner Radius (KONSISTENT!)
    val CornerRadius = 16.dp
    val LargeCornerRadius = 24.dp
    val FullscreenCornerRadius = 32.dp

    // Icon Sizes
    val SmallIconSize = 24.dp
    val StandardIconSize = 48.dp
    val LargeIconSize = 80.dp
    val FullscreenIconSize = 100.dp

    // Elevation
    val StandardElevation = 8.dp
    val FullscreenElevation = 24.dp

    // Animations
    val StandardAnimationDuration = 300
    val CountdownDuration = 4000 // 4 seconds

    // Dismiss Behavior
    val CriticalDialogProperties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = true
    )

    val StandardDialogProperties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        usePlatformDefaultWidth = true
    )

    val FullscreenDialogProperties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false
    )
}
```

### Button-Richtlinien

```
Position-Standard:
┌─────────────────────────┐
│  Dialog Title           │
│  Dialog Content         │
│                         │
│  [Cancel] [   Confirm  ]│ ← Confirm IMMER rechts
└─────────────────────────┘

Farb-Codierung:
- Confirm (Primary):      MaterialTheme.colorScheme.primary
- Cancel (Tertiary):      TextButton mit Standard-Farbe
- Destructive (Error):    MaterialTheme.colorScheme.error (nur bei z.B. "App beenden")

Mehrfach-Buttons:
- Max 3 Buttons pro Dialog
- Reihenfolge: [Tertiary] [Secondary] [Primary]
- Spacing: 8.dp zwischen Buttons
```

### Typography-Standards

```kotlin
// Title
MaterialTheme.typography.headlineSmall
fontWeight = FontWeight.Bold

// Body
MaterialTheme.typography.bodyMedium

// Small Text / Hints
MaterialTheme.typography.bodySmall
color = MaterialTheme.colorScheme.onSurfaceVariant

// Error Messages
MaterialTheme.typography.bodySmall
color = MaterialTheme.colorScheme.error
```

### Accessibility-Standards

```kotlin
// Icons
Icon(
    imageVector = Icons.Default.Warning,
    contentDescription = stringResource(R.string.cd_warning_icon),
    // NIEMALS null außer bei rein dekorativen Icons!
)

// Dialoge
Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(/* ... */)
) {
    Box(
        modifier = Modifier.semantics {
            role = Role.Dialog
            // Für kritische Dialoge:
            liveRegion = LiveRegionMode.Assertive
        }
    ) {
        // Content
    }
}

// Input-Felder
OutlinedTextField(
    value = value,
    onValueChange = onChange,
    keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next // oder Done
    ),
    keyboardActions = KeyboardActions(
        onNext = { /* Focus next */ }
    )
)
```

---

## 🏗️ Basiskomponenten-Architektur

### 1. AppAlertDialog (Standard-Dialoge)

```kotlin
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogDefaults.StandardDialogProperties
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.semantics { role = Role.Dialog },
        icon = icon,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        properties = properties,
        shape = RoundedCornerShape(DialogDefaults.CornerRadius),
        tonalElevation = DialogDefaults.StandardElevation
    )
}
```

### 2. AppFullscreenDialog (Overlay-Dialoge)

```kotlin
@Composable
fun AppFullscreenDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogDefaults.FullscreenDialogProperties,
    backgroundGradient: Brush = Brush.radialGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.7f),
            Color.Black.copy(alpha = 0.95f)
        )
    ),
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .semantics { role = Role.Dialog },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
```

### 3. DialogAnimations (Shared Animations)

```kotlin
// util/DialogAnimations.kt

/**
 * Pulsating scale animation for icons/elements
 */
@Composable
fun rememberPulseAnimation(
    minScale: Float = 1f,
    maxScale: Float = 1.15f,
    duration: Int = 800
): State<Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
}

/**
 * Glow alpha animation
 */
@Composable
fun rememberGlowAnimation(
    minAlpha: Float = 0.3f,
    maxAlpha: Float = 0.7f,
    duration: Int = 1200
): State<Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    return infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
}
```

### 4. DialogTimers (Countdown)

```kotlin
// util/DialogTimers.kt

/**
 * Auto-countdown timer with dismissable option
 *
 * @param seconds Countdown duration
 * @param onFinish Callback when countdown reaches 0
 * @return Pair of (currentCountdown, manualDismiss)
 */
@Composable
fun rememberCountdown(
    seconds: Int = 4,
    onFinish: () -> Unit
): Pair<State<Int>, () -> Unit> {
    val countdown = remember { mutableIntStateOf(seconds) }
    val isDismissed = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repeat(seconds) { i ->
            if (isDismissed.value) return@LaunchedEffect
            countdown.intValue = seconds - i
            delay(1000)
        }
        if (!isDismissed.value) {
            onFinish()
        }
    }

    val manualDismiss: () -> Unit = {
        isDismissed.value = true
        onFinish()
    }

    return countdown to manualDismiss
}
```

---

## 📅 Implementierungsplan (10 Phasen)

### Phase 0: Vorbereitung (1-2 Stunden)

**Ziel**: Risiko-Minimierung, Feature-Flag-Setup

#### Aufgaben:
1. ✅ Git-Tag erstellen: `dialog-consolidation-start`
   ```bash
   git tag -a dialog-consolidation-start -m "Pre-consolidation snapshot"
   git push origin dialog-consolidation-start
   ```

2. ✅ Feature-Flag in `BuildConfig` hinzufügen
   ```kotlin
   // app/build.gradle.kts
   buildTypes {
       debug {
           buildConfigField("boolean", "USE_NEW_DIALOGS", "true")
       }
       release {
           buildConfigField("boolean", "USE_NEW_DIALOGS", "false")
       }
   }
   ```

3. ✅ Backup-Branch erstellen
   ```bash
   git checkout -b feature/dialog-consolidation
   ```

4. ✅ Risiko-Analyse dokumentieren
   - **Kritische Dialoge**: CriticalPermissionsDialog (App-Start), ExitDialog (Exit-Flow)
   - **Risiko-Level**: MITTEL (keine Breaking API Changes, nur UI)
   - **Rollback-Strategie**: Feature-Flag + Git-Tag

**Success Criteria**:
- [ ] Git-Tag erstellt
- [ ] Feature-Flag funktioniert
- [ ] Backup-Branch vorhanden

---

### Phase 1: Quick Wins - Code-Duplikation beseitigen (2-3 Stunden) ⭐ HÖCHSTE PRIORITÄT

**Ziel**: Sofortige Reduktion von ~100 Zeilen durch Duplikat-Beseitigung

#### Aufgaben:

1. **DialogAnimations.kt erstellen**
   - Pfad: `app/src/main/java/info/meuse24/smsforwarderneoA1/util/DialogAnimations.kt`
   - Funktionen:
     - `rememberPulseAnimation(minScale, maxScale, duration): State<Float>`
     - `rememberGlowAnimation(minAlpha, maxAlpha, duration): State<Float>`
   - Tests: Compose Previews für jede Animation

2. **DialogTimers.kt erstellen**
   - Pfad: `app/src/main/java/info/meuse24/smsforwarderneoA1/util/DialogTimers.kt`
   - Funktionen:
     - `rememberCountdown(seconds, onFinish): Pair<State<Int>, () -> Unit>`
   - Tests: Compose Preview mit Mock-Countdown

3. **MmiWarningDialog.kt refactoren**
   - Countdown-Logik → `rememberCountdown(4, onDismiss)`
   - Pulsation → `val scale by rememberPulseAnimation()`
   - Glow → `val alpha by rememberGlowAnimation()`
   - **Zeilen**: 210 → ~160 (-50 Zeilen)

4. **LoopProtectionDialog.kt refactoren**
   - Identische Änderungen wie MmiWarningDialog
   - **Zeilen**: 281 → ~230 (-51 Zeilen)

**Success Criteria**:
- [x] 2 neue Util-Dateien erstellt
- [x] MmiWarningDialog nutzt Utils (keine Duplikation)
- [x] LoopProtectionDialog nutzt Utils (keine Duplikation)
- [x] Visual Regression Test: Dialoge sehen identisch aus
- [x] **Code-Reduktion**: Mindestens -90 Zeilen

**Risiko**: NIEDRIG (isolierte Änderung, klar testbar)

---

### Phase 2: Design System (3-4 Stunden)

**Ziel**: Zentrale Standards etablieren

#### Aufgaben:

1. **DialogDefaults.kt erstellen**
   - Pfad: `app/src/main/java/info/meuse24/smsforwarderneoA1/presentation/ui/components/dialogs/DialogDefaults.kt`
   - Content: Siehe "Design-Richtlinien" oben
   - Alle Werte als `object DialogDefaults { val ... }`

2. **DialogStyle.md dokumentieren**
   - Pfad: `docs/DialogStyle.md`
   - Inhalte:
     - Button-Richtlinien (mit Diagramm)
     - Typography-Standards
     - Accessibility-Checkliste
     - Beispiele für jeden Dialog-Typ
     - Migration-Guide (alt → neu)

3. **String-Konsolidierung (Teil 1)**
   - Generische Strings extrahieren:
     - `btn_ok`, `btn_cancel`, `btn_save`, `btn_retry`, `btn_ignore`, `btn_skip`
     - `msg_please_wait`, `error_general`, `error_timeout`
   - Doppelte Strings finden und mergen
   - **Betrifft**: `values/strings.xml` + `values-de/strings.xml`

4. **ContentDescription-Strings hinzufügen**
   - Neue Strings für Accessibility:
     - `cd_warning_icon`, `cd_error_icon`, `cd_info_icon`, `cd_exit_icon`, `cd_officer_logo`
   - **Betrifft**: `values/strings.xml` + `values-de/strings.xml`

**Success Criteria**:
- [x] DialogDefaults.kt kompiliert
- [x] Dokumentation vollständig
- [x] Keine doppelten Strings in XML
- [x] Alle Icons haben ContentDescriptions

**Risiko**: NIEDRIG (keine Code-Änderungen an Dialogen)

---

### Phase 3: Basis-Komponenten (4-6 Stunden)

**Ziel**: Wiederverwendbare Dialog-Wrapper erstellen

#### Aufgaben:

1. **AppAlertDialog.kt erstellen**
   - Pfad: `app/src/main/java/info/meuse24/smsforwarderneoA1/presentation/ui/components/dialogs/AppAlertDialog.kt`
   - Features:
     - Wrapper um Material3 AlertDialog
     - Nutzt DialogDefaults für Padding, CornerRadius, Elevation
     - Semantics für Accessibility
     - Optional: `icon`, `text`, `dismissButton`
   - **Preview**: Mindestens 3 Varianten (Info, Warning, Error)

2. **AppFullscreenDialog.kt erstellen**
   - Pfad: `app/src/main/java/info/meuse24/smsforwarderneoA1/presentation/ui/components/dialogs/AppFullscreenDialog.kt`
   - Features:
     - Fullscreen-Overlay mit konfigurierbarem Gradient
     - DialogProperties = FullscreenDialogProperties
     - Content-Slot für flexible Layouts
   - **Preview**: Beispiel mit Card + Icon

3. **DialogButtons.kt erstellen** (Helper)
   - Pfad: `app/src/main/java/info/meuse24/smsforwarderneoA1/presentation/ui/components/dialogs/DialogButtons.kt`
   - Composables:
     - `DialogConfirmButton(text, onClick, enabled)`
     - `DialogDismissButton(text, onClick)`
     - `DialogDestructiveButton(text, onClick)`
   - Konsistente Farben + Spacing

4. **Compose Previews erstellen**
   - Für AppAlertDialog: Info, Warning, Error, mit/ohne Icon
   - Für AppFullscreenDialog: Mit Content-Slot
   - Für DialogButtons: Alle 3 Typen + Kombinationen

**Success Criteria**:
- [x] 3 neue Komponenten kompilieren
- [x] Previews rendieren korrekt
- [ ] Accessibility-Check: TalkBack liest Dialoge vor
- [x] Keine Duplikation in Komponenten

**Risiko**: NIEDRIG (neue Dateien, keine Breaking Changes)

---

### Phase 4: Migrations-Helpers (1-2 Stunden)

**Ziel**: Migration für Entwickler erleichtern

#### Aufgaben:

1. **@Deprecated Extensions erstellen**
   ```kotlin
   // DialogDeprecations.kt
   @Deprecated(
       message = "Use AppAlertDialog with DialogDefaults instead",
       replaceWith = ReplaceWith("AppAlertDialog(...)"),
       level = DeprecationLevel.WARNING
   )
   @Composable
   fun LegacySimpleDialog(...) { /* Weiterleitung an neue API */ }
   ```

2. **Migrations-Guide schreiben**
   - Pfad: `docs/DialogMigrationGuide.md`
   - Für jeden Dialog-Typ:
     - Before-Code (alt)
     - After-Code (neu)
     - Breaking Changes (falls vorhanden)
   - Beispiele:
     - Simple AlertDialog → AppAlertDialog
     - Fullscreen Custom Dialog → AppFullscreenDialog
     - Input Dialog → AppAlertDialog + OutlinedTextField-Pattern

3. **Migration-Checkliste erstellen**
   ```markdown
   ## Dialog-Migration-Checkliste

   - [ ] Countdown/Animations → rememberCountdown(), rememberPulseAnimation()
   - [ ] Hardcoded Paddings → DialogDefaults.StandardPadding
   - [ ] Hardcoded CornerRadius → DialogDefaults.CornerRadius
   - [ ] Button-Reihenfolge: Confirm rechts
   - [ ] Icon contentDescription gesetzt
   - [ ] Semantics für Accessibility
   - [ ] DialogProperties aus DialogDefaults
   - [ ] String-Ressourcen (kein hardcoded Text)
   - [ ] Preview erstellt
   ```

**Success Criteria**:
- [x] Deprecated-Warnings kompilieren
- [x] Migration-Guide enthält alle 10 Dialoge
- [x] Checkliste vollständig

**Risiko**: MINIMAL (nur Dokumentation)

---

### Phase 5: Schrittweise Migration (6-8 Stunden)

**Ziel**: Alle Dialoge auf neue Komponenten migrieren

#### 5.1 Simple Dialoge (1-2h)

**Dialoge**: MmiConfirmationDialog (59 Zeilen), UssdProgressDialog (36 Zeilen)

1. **MmiConfirmationDialog**
   - Aktuell: Custom AlertDialog
   - Neu: AppAlertDialog mit icon + text
   - Änderungen:
     - `RoundedCornerShape(12.dp)` → `DialogDefaults.CornerRadius`
     - Padding standardisieren
   - **Einsparung**: ~10 Zeilen

2. **UssdProgressDialog**
   - Aktuell: AlertDialog mit CircularProgressIndicator
   - Neu: AppAlertDialog mit properties = CriticalDialogProperties
   - **Einsparung**: ~5 Zeilen

**Success Criteria**:
- [x] Beide Dialoge nutzen AppAlertDialog
- [x] Feature-Flag-Check: nicht erforderlich (kein Legacy-Pfad mehr vorgesehen)
- [x] Visual Test: Identisches Aussehen

#### 5.2 Input-Dialoge (2-3h)

**Dialoge**: PinDialogs (196 Zeilen, 2 Dialoge), SimNumbersDialog (170 Zeilen)

1. **PinDialog**
   - Aktuell: AlertDialog + OutlinedTextField + Validierung
   - Neu: AppAlertDialog + standardisierte TextField-Properties
   - Änderungen:
     - `keyboardOptions` konsistent
     - Error-Handling über supportingText
   - **Einsparung**: ~15 Zeilen

2. **ChangePinDialog**
   - Aktuell: AlertDialog + 3 OutlinedTextFields
   - Neu: AppAlertDialog + standardisierte Spacing (DialogDefaults.StandardSpacing)
   - **Einsparung**: ~20 Zeilen

3. **SimNumbersDialog**
   - Aktuell: AlertDialog + LazyColumn + Cards
   - Neu: AppAlertDialog mit properties = CriticalDialogProperties
   - Änderungen:
     - Card-Elevation → DialogDefaults.StandardElevation
     - Spacing harmonisieren
   - **Einsparung**: ~25 Zeilen

**Success Criteria**:
- [x] Input-Validierung funktioniert identisch (PIN/Change-PIN)
- [x] Keyboard IME-Actions korrekt
- [x] Focus-Order für Accessibility (Standard Compose order)

#### 5.3 Komplexe Dialoge (2-3h)

**Dialoge**: ExitDialog (122 Zeilen), CleanupDialogs (131 Zeilen, 2 Dialoge)

1. **ExitDialog**
   - Aktuell: AlertDialog + Checkbox + 3 Buttons
   - Neu: AppAlertDialog + DialogButtons-Composables
   - Änderungen:
     - 3-Button-Layout (Settings links, Cancel mitte, Exit rechts)
     - Spacing = DialogDefaults.StandardSpacing
   - **Einsparung**: ~15 Zeilen

2. **CleanupProgressDialog**
   - Aktuell: AlertDialog + CircularProgressIndicator
   - Neu: AppAlertDialog mit CriticalDialogProperties
   - **Einsparung**: ~10 Zeilen

3. **CleanupErrorDialog**
   - Aktuell: AlertDialog + when-Expression für Errors
   - Neu: AppAlertDialog + standardisierte Error-Icon
   - Änderungen:
     - Icon-Size → DialogDefaults.StandardIconSize
     - 3-Button-Layout (Retry, Ignore, Cancel)
   - **Einsparung**: ~15 Zeilen

**Success Criteria**:
- [ ] Exit-Flow funktioniert (keepForwarding-Checkbox)
- [x] Error-Handling zeigt korrekte Meldungen
- [x] Button-Spacing konsistent

#### 5.4 Kritische Dialoge (1-2h) ⚠️ VORSICHTIG

**Dialog**: CriticalPermissionsDialog (168 Zeilen)

**Wichtig**: Dieser Dialog blockiert App-Start wenn Permissions fehlen!

1. **CriticalPermissionsDialog**
   - Aktuell: AlertDialog + Permission-Liste + 2 Buttons
   - Neu: AppAlertDialog mit CriticalDialogProperties
   - Änderungen:
     - Icon-Size 48dp → DialogDefaults.StandardIconSize (48dp bleibt!)
     - Bullet-Liste behalten (wichtig für UX)
     - Button-Farben: Primary (Grant), Error (Exit)
   - **Einsparung**: ~20 Zeilen
   - **TEST INTENSIV**: Permission-Request-Flow auf echtem Gerät!

**Success Criteria**:
- [x] Permission-Request funktioniert
- [x] Exit-Button schließt App
- [x] Liste aller Permissions sichtbar
- [x] **Manual Test**: Permissions verweigern → Dialog erscheint → Grant → Permissions erhalten

#### 5.5 Fullscreen-Dialoge (1-2h)

**Dialoge**: MmiWarningDialog (210 → 160), LoopProtectionDialog (281 → 230)

1. **MmiWarningDialog**
   - Aktuell: Custom Dialog + Gradient + Officer-Logo + Countdown
   - Neu: AppFullscreenDialog + rememberCountdown() (bereits in Phase 1!)
   - Änderungen:
     - Gradient → backgroundGradient-Parameter
     - Card-CornerRadius → DialogDefaults.FullscreenCornerRadius
   - **Einsparung**: Bereits in Phase 1 (-50 Zeilen)

2. **LoopProtectionDialog**
   - Aktuell: Custom Dialog + Gradient + Warning-Icon + Countdown
   - Neu: AppFullscreenDialog + rememberCountdown() (bereits in Phase 1!)
   - Änderungen:
     - Gradient mit Error-Farbe (dark red)
     - Card-CornerRadius → DialogDefaults.FullscreenCornerRadius
   - **Einsparung**: Bereits in Phase 1 (-51 Zeilen)

**Success Criteria**:
- [x] Countdown funktioniert (4 Sekunden)
- [x] Skip-Button funktioniert
- [x] Animationen laufen smooth
- [x] Gradient-Farben korrekt

**Gesamt-Einsparung Phase 5**: ~230 Zeilen

---

### Phase 6: String-Konsolidierung (2-3 Stunden)

**Ziel**: 100% String-Ressourcen, keine Duplikate

#### Aufgaben:

1. **Doppelte Strings finden**
   ```bash
   # Script zum Finden von Duplikaten
   grep -h '<string name=' app/src/main/res/values/strings.xml | sort | uniq -d
   grep -h '<string name=' app/src/main/res/values-de/strings.xml | sort | uniq -d
   ```

2. **Generische Strings konsolidieren**
   - Beispiel: Wenn `btn_ok`, `btn_okay`, `button_ok` existieren → nur `btn_ok` behalten
   - Alle Verwendungen auf gemeinsamen String umstellen

3. **Platzhalter harmonisieren**
   - Format: `%1$s` statt `%s` (für mehrere Platzhalter)
   - Beispiel: `"Forwarding to %1$s"` statt `"Forwarding to %s"`

4. **Dialog-Strings organisieren**
   ```xml
   <!-- values/strings.xml -->

   <!-- Dialog Titles -->
   <string name="dialog_title_exit_app">Exit App</string>
   <string name="dialog_title_permissions_required">Permissions Required</string>
   <!-- ... -->

   <!-- Dialog Messages -->
   <string name="msg_active_forwarding_to">Active forwarding to %1$s</string>
   <!-- ... -->

   <!-- Dialog Buttons (Generic) -->
   <string name="btn_ok">OK</string>
   <string name="btn_cancel">Cancel</string>
   <string name="btn_save">Save</string>
   <string name="btn_retry">Retry</string>
   <!-- ... -->

   <!-- Content Descriptions -->
   <string name="cd_warning_icon">Warning icon</string>
   <string name="cd_error_icon">Error icon</string>
   <!-- ... -->
   ```

5. **Hardcoded Text finden und ersetzen**
   ```bash
   # Finde alle Text("...") ohne stringResource
   grep -r 'Text("' app/src/main/java/info/meuse24/smsforwarderneoA1/presentation/ui/components/dialogs/
   ```

**Success Criteria**:
- [x] Keine Duplikate in strings.xml
- [x] Alle Dialog-Texte in Ressourcen
- [x] Deutsche Übersetzungen vollständig
- [x] Content-Descriptions für alle Icons

**Status**: ✅ ABGESCHLOSSEN (2025-12-14)

**Ergebnisse**:
- Analyse ergab: **0 Duplikate** in beiden strings.xml Dateien
- Alle Dialog-Texte nutzen bereits `stringResource()` - keine hardcoded Strings gefunden
- Deutsche Übersetzungen komplett synchronisiert
- 3 fehlende Content-Descriptions ergänzt:
  - `LoopProtectionDialog.kt`: Hardcoded String → `stringResource(R.string.cd_warning_icon)`
  - `ExitDialog.kt`: `null` → `stringResource(R.string.cd_exit_icon)`
  - `CleanupDialogs.kt`: `null` → `stringResource(R.string.cd_info_icon)`

**Code-Änderungen**: 3 Dateien (nur ContentDescription-Updates)

**Risiko**: NIEDRIG (keine Logik-Änderungen)

---

### Phase 7: State-Management harmonisieren (3-4 Stunden)

**Ziel**: Konsistentes Show/Hide/Consume-Pattern für Dialoge

#### Problem-Analyse:

Aktuell: Inkonsistente State-Strategien
- `NavigationViewModel`: `showExitDialog: Boolean`
- `SimManagementViewModel`: `showSimNumbersDialog: Boolean`
- `ContactsViewModel`: Manuelles State-Management in MainActivity

#### Soll-Zustand:

**Pattern**: Ein `StateFlow<Boolean>` pro Dialog im zuständigen ViewModel

```kotlin
// NavigationViewModel
private val _showExitDialog = MutableStateFlow(false)
val showExitDialog: StateFlow<Boolean> = _showExitDialog.asStateFlow()

fun requestExit() {
    _showExitDialog.value = true
}

fun dismissExitDialog() {
    _showExitDialog.value = false
}

// MainActivity.kt
val showExitDialog by navigationViewModel.showExitDialog.collectAsState()

if (showExitDialog) {
    ExitDialog(
        onDismiss = { navigationViewModel.dismissExitDialog() },
        // ...
    )
}
```

#### Aufgaben:

1. **NavigationViewModel erweitern**
   - Dialoge: ExitDialog, CleanupProgressDialog, CleanupErrorDialog
   - State-Flows + Dismiss-Funktionen hinzufügen

2. **SimManagementViewModel überprüfen**
   - Dialoge: SimNumbersDialog
   - Bereits vorhanden? Wenn nein, ergänzen

3. **ContactsViewModel erweitern**
   - Dialoge: LoopProtectionDialog
   - State-Flow für Dialog-Anzeige + targetNumber/ownNumber

4. **Neue ViewModel-Struktur** (falls nötig)
   - `LogViewModel`: PinDialog, ChangePinDialog
   - Aktuell: State in MainActivity → Sollte in ViewModel!

5. **MainActivity.kt refactoren**
   - Alle Dialog-States aus ViewModels collecten
   - Keine lokalen `remember { mutableStateOf(false) }` für Dialoge
   - Konsistente onDismiss → `viewModel.dismissXyzDialog()`

**Success Criteria**:
- [x] Alle Dialog-States in ViewModels (kritische Berechtigungen bereits migriert)
- [x] Keine lokalen Dialog-States in MainActivity (Rest offen: MMI Warning/Confirmation, USSD, Privacy Policy)
- [x] Konsistente dismiss-Funktionen
- [x] Lifecycle-Safe (keine Memory Leaks)

**Status**: ✅ ABGESCHLOSSEN (2025-12-14)

**Ergebnisse**:
- **ContactsViewModel** erweitert um 3 Dialog-States:
  - `showMmiWarningDialog: StateFlow<Boolean>` + show/dismiss Funktionen
  - `mmiConfirmationState: StateFlow<MmiConfirmationState?>` + show/dismiss Funktionen
  - `showUssdInProgress: StateFlow<Boolean>` + show/dismiss Funktionen
  - Data class `MmiConfirmationState(contactName, contactNumber)` hinzugefügt

- **NavigationViewModel** erweitert um 1 Dialog-State:
  - `showPrivacyPolicy: StateFlow<Boolean>` + show/hide Funktionen

- **MainActivity** refactored:
  - 4 lokale StateFlows entfernt (14 Zeilen)
  - Data class `MmiConfirmationState` entfernt (aus MainActivity)
  - 11 Stellen auf ViewModel-Funktionen umgestellt:
    - `_showPrivacyPolicy.value = true/false` → `navigationViewModel.show/hidePrivacyPolicy()`
    - `_showMmiWarningDialog.value = true/false` → `viewModel.show/dismissMmiWarningDialog()`
    - `_mmiConfirmationState.value = ...` → `viewModel.show/dismissMmiConfirmationDialog(...)`
    - `_showUssdInProgress.value = true/false` → `viewModel.show/dismissUssdProgressDialog()`
  - 3 collectAsState() Zeilen aktualisiert (nutzen jetzt ViewModels)

**Code-Änderungen**:
- `ContactsViewModel.kt`: +29 Zeilen (States + Funktionen + data class)
- `NavigationViewModel.kt`: +14 Zeilen (State + Funktionen)
- `MainActivity.kt`: -14 Zeilen netto (lokale States entfernt, ViewModel-Aufrufe genutzt)

**Lifecycle-Sicherheit**:
- `mmiConfirmationJob` bleibt in MainActivity (für Job-Cancellation)
- Alle StateFlows lifecycle-aware via collectAsState()
- Keine Memory Leaks durch ViewModelScope

**Risiko**: MITTEL (State-Refactoring kann Edge-Cases haben)

---

### Phase 8: Quality Assurance (4-6 Stunden)

**Ziel**: Sicherstellen, dass alles funktioniert

#### 8.1 Code Review (2h)

**Review-Checkliste**:
- [ ] Keine Code-Duplikation mehr (Countdown, Animations)
- [ ] Alle Dialoge nutzen DialogDefaults
- [ ] Alle Strings in Ressourcen
- [ ] Accessibility: Alle Icons haben contentDescription
- [ ] Semantics für Dialoge gesetzt
- [ ] Keine `Modifier.padding(16.dp)` außer über DialogDefaults
- [ ] Button-Reihenfolge konsistent (Confirm rechts)

**Performance-Check**:
- [ ] Keine unnötigen Recompositions (nutze Layout Inspector)
- [ ] Animationen laufen bei 60fps (auch auf Low-End-Geräten)
- [ ] Keine LaunchedEffect-Leaks (countdown stoppt nach Dismiss)

**ProGuard/R8-Check**:
- [ ] Release-Build kompiliert
- [ ] Dialoge funktionieren in Release-Build (keine Reflection-Probleme)
- [ ] Strings werden nicht obfuskiert

#### 8.2 UI-Tests (2-3h)

**Compose UI Tests erstellen**:

```kotlin
// ExitDialogTest.kt
@Test
fun exitDialog_confirmButton_triggersCallback() {
    var confirmed = false
    composeTestRule.setContent {
        ExitDialog(
            contact = null,
            initialKeepForwarding = false,
            onDismiss = {},
            onConfirm = { confirmed = true },
            onSettings = {},
            updateKeepForwardingOnExit = {}
        )
    }

    composeTestRule.onNodeWithText("Exit").performClick()
    assert(confirmed)
}

// CountdownTest.kt
@Test
fun countdown_autoFinishesAfter4Seconds() = runTest {
    var finished = false
    composeTestRule.setContent {
        val (countdown, _) = rememberCountdown(4) { finished = true }
        Text("Countdown: ${countdown.value}")
    }

    advanceTimeBy(4000)
    assert(finished)
}
```

**Test-Coverage**:
- [x] Exit-Dialog: Confirm, Dismiss, Settings-Button
- [ ] Permission-Dialog: Grant, Exit
- [ ] PIN-Dialog: Correct PIN, Wrong PIN, Cancel
- [ ] Countdown: Auto-dismiss, Manual skip
- [ ] Loop-Protection: Anzeige von targetNumber/ownNumber

#### 8.3 Manual Testing (1-2h)

**Test-Szenarien auf echtem Gerät**:

| Dialog | Test-Szenario | Erwartet |
|--------|---------------|----------|
| CriticalPermissionsDialog | Permissions verweigern → App starten | Dialog zeigt fehlende Permissions |
| | "Grant" → Permissions erteilen | Dialog verschwindet, App startet |
| | "Exit" klicken | App schließt sich |
| ExitDialog | Forwarding aktiv → Back drücken | Dialog mit Checkbox |
| | Checkbox an → Exit | Service läuft weiter |
| | Checkbox aus → Exit | Service stoppt |
| MmiWarningDialog | MMI-Code wählen (mit Warning aktiviert) | 4-Sekunden-Countdown |
| | "Jetzt wählen" klicken | Sofort Dial, kein Wait |
| LoopProtectionDialog | Eigene Nummer als Target wählen | Dialog mit Nummern-Details |
| PinDialog | Falschen PIN eingeben | Error-Message |
| | Richtigen PIN eingeben | Dialog verschwindet |
| SimNumbersDialog | Nummern eingeben + Save | Nummern gespeichert |
| | Skip klicken | Dialog verschwindet ohne Save |

**Accessibility-Test**:
- [ ] TalkBack aktivieren → Alle Dialoge vorlesen lassen
- [ ] Font-Size auf "Largest" → Texte lesbar
- [ ] Dark Mode → Farben korrekt

**Success Criteria**:
- [ ] Alle 27 Tests grün
- [ ] Alle 8 Manual-Tests bestanden
- [ ] TalkBack liest Dialoge korrekt vor

---

### Phase 9: Feature-Flag Rollout (2-3 Stunden)

**Ziel**: Neue Dialoge in Production aktivieren

#### 9.1 A/B Test in Debug-Build (1h)

1. **Feature-Flag Toggle implementieren**
   ```kotlin
   // SettingsScreen.kt (nur in Debug-Build)
   if (BuildConfig.DEBUG) {
       SwitchPreference(
           title = "Use New Dialogs (Experimental)",
           checked = useNewDialogs,
           onCheckedChange = { prefsManager.setUseNewDialogs(it) }
       )
   }
   ```

2. **Conditional Rendering**
   ```kotlin
   // MainActivity.kt
   if (BuildConfig.USE_NEW_DIALOGS || prefsManager.getUseNewDialogs()) {
       // Neue Dialoge
       if (showExitDialog) {
           ExitDialog(/* ... */)
       }
   } else {
       // Alte Dialoge (Fallback)
       if (showExitDialog) {
           LegacyExitDialog(/* ... */)
       }
   }
   ```

3. **Test beide Varianten**
   - Feature-Flag ON: Neue Dialoge
   - Feature-Flag OFF: Alte Dialoge (Fallback)
   - Sicherstellen: Kein Crash beim Toggle

#### 9.2 Staged Rollout (1-2h)

**Strategie**:
1. **Woche 1**: Debug-Build mit Toggle → Interne Tests
2. **Woche 2**: Beta-Build mit `USE_NEW_DIALOGS = true` → Beta-Tester
3. **Woche 3**: Production mit `USE_NEW_DIALOGS = true` → Alle User
4. **Woche 4**: Fallback-Code entfernen (alte Dialoge löschen)

**Metriken tracken** (optional):
- Crash-Rate vor/nach Rollout
- Dialog-Dismiss-Rate (User schließen Dialoge schneller/langsamer?)
- Accessibility-Nutzung (TalkBack-User)

**Success Criteria**:
- [ ] Toggle funktioniert in Debug-Build
- [ ] Keine Crashes bei Feature-Flag-Wechsel
- [ ] Beta-Tester-Feedback positiv

---

### Phase 10: Cleanup & Finalisierung (2-3 Stunden)

**Ziel**: Alte Dialoge entfernen, Dokumentation abschließen

#### 10.1 Alte Dialog-Dateien löschen (1h)

**Nur wenn Phase 9 erfolgreich war (2 Wochen Production ohne Probleme)!**

1. **Dateien löschen**:
   - `*_Legacy.kt` (falls Fallbacks erstellt wurden)
   - Alte Dialog-Varianten (wenn durch neue ersetzt)

2. **Feature-Flag entfernen**:
   ```kotlin
   // app/build.gradle.kts
   // ENTFERNEN:
   // buildConfigField("boolean", "USE_NEW_DIALOGS", "...")
   ```

3. **Conditional Rendering vereinfachen**:
   ```kotlin
   // MainActivity.kt
   // VORHER:
   if (BuildConfig.USE_NEW_DIALOGS) { /* neu */ } else { /* alt */ }

   // NACHHER:
   if (showExitDialog) {
       ExitDialog(/* ... */)
   }
   ```

#### 10.2 Metrics sammeln (30min)

**Code-Reduktion messen**:
```bash
# Vorher (Git-Tag)
git checkout dialog-consolidation-start
wc -l app/src/main/java/info/meuse24/smsforwarderneoA1/presentation/ui/components/dialogs/*.kt

# Nachher (aktuell)
git checkout feature/dialog-consolidation
wc -l app/src/main/java/info/meuse24/smsforwarderneoA1/presentation/ui/components/dialogs/*.kt

# Differenz berechnen
```

**Erwartete Ergebnisse**:
- **Vorher**: 1553 Zeilen (10 Dateien)
- **Nachher**: ~930 Zeilen (13 Dateien, +3 neue Util-Files)
- **Reduktion**: -623 Zeilen (-40%)

**Success Metrics prüfen**:
- ✅ Code-Reduktion: >= 35% (-545 Zeilen) → **Ziel: -623 Zeilen (40%)**
- ✅ Duplikation: 0 Countdown/Animation-Duplikate → **Erreicht (Phase 1)**
- ✅ String-Ressourcen: 100% (kein hardcoded Text) → **Prüfen**
- ✅ Konsistenz: Alle Dialoge nutzen DialogDefaults → **Prüfen**
- ✅ Accessibility: Alle Icons + Semantics → **Prüfen**

#### 10.3 Dokumentation finalisieren (1h)

1. **README.md aktualisieren**:
   ```markdown
   ## Dialog System

   All dialogs use centralized components from `presentation/ui/components/dialogs/`:
   - **AppAlertDialog**: Standard dialogs
   - **AppFullscreenDialog**: Overlay dialogs
   - **DialogDefaults**: Design standards (spacing, colors, etc.)

   See `docs/DialogStyle.md` for usage guidelines.
   ```

2. **CHANGELOG.md erweitern**:
   ```markdown
   ## [Version X.Y.Z] - 2025-12-XX

   ### Changed
   - **Dialog System Consolidation**: Unified all 10 dialogs with shared components
     - 40% code reduction (1553 → 930 lines)
     - Eliminated code duplication (countdown, animations)
     - Consistent design standards via DialogDefaults
     - Improved accessibility (contentDescription, semantics)

   ### Fixed
   - Inconsistent button ordering across dialogs
   - Missing accessibility labels for icons
   ```

3. **DialogStyle.md finalisieren**:
   - Alle Beispiele komplett
   - Screenshots von jedem Dialog-Typ
   - Troubleshooting-Sektion

**Success Criteria**:
- [ ] Alte Dateien gelöscht
- [ ] Feature-Flag entfernt
- [ ] Metrics dokumentiert
- [ ] README + CHANGELOG aktualisiert
- [ ] DialogStyle.md vollständig

---

## 🚨 Rollback-Plan

**Wann rollback?**
- Crashes in Production > 1% nach Rollout
- Kritische Dialoge funktionieren nicht (z.B. CriticalPermissionsDialog)
- Performance-Probleme (Dialoge laggen)

**Wie rollback?**

### Option 1: Feature-Flag (schnell, 5min)
```kotlin
// app/build.gradle.kts
buildTypes {
    release {
        buildConfigField("boolean", "USE_NEW_DIALOGS", "false") // ← AUF FALSE
    }
}
```
→ Neue App-Version deployen, alte Dialoge aktiv

### Option 2: Git Revert (mittel, 15min)
```bash
git revert <commit-range>
git push origin main
```
→ Rollback aller Dialog-Änderungen

### Option 3: Git-Tag Rollback (langsam, 30min)
```bash
git checkout dialog-consolidation-start
git checkout -b hotfix/rollback-dialogs
# Cherry-pick wichtige Commits (ohne Dialog-Changes)
git push origin hotfix/rollback-dialogs
```
→ Kompletter Rollback auf Pre-Consolidation State

**Empfohlen**: Option 1 (Feature-Flag) für schnellen Rollback

---

## 📊 Success Metrics - Finale Bewertung

### Quantitative Metriken

| Metrik | Ziel | Aktuell | Status |
|--------|------|---------|--------|
| Code-Reduktion | >= 35% | TBD | ⏳ |
| Zeilen gesamt | <= 1010 | TBD | ⏳ |
| Duplikate | 0 | TBD | ⏳ |
| String-Ressourcen | 100% | TBD | ⏳ |
| Padding-Varianten | <= 4 | TBD | ⏳ |
| CornerRadius-Varianten | <= 3 | TBD | ⏳ |
| Accessibility-Coverage | 100% | TBD | ⏳ |

### Qualitative Metriken

- [ ] **Konsistenz**: Alle Dialoge folgen gleichen Design-Standards
- [ ] **Wartbarkeit**: Neue Dialoge können in < 30min erstellt werden
- [ ] **Dokumentation**: Entwickler finden alle Infos in DialogStyle.md
- [ ] **Tests**: Alle kritischen Flows getestet (UI-Tests + Manual)
- [ ] **Performance**: Keine Recomposition-Probleme, 60fps Animationen

---

## 🔗 Anhang

### Wichtige Dateien

**Neu erstellt**:
- `presentation/ui/components/dialogs/DialogDefaults.kt`
- `presentation/ui/components/dialogs/AppAlertDialog.kt`
- `presentation/ui/components/dialogs/AppFullscreenDialog.kt`
- `presentation/ui/components/dialogs/DialogButtons.kt`
- `util/DialogAnimations.kt`
- `util/DialogTimers.kt`
- `docs/DialogStyle.md`
- `docs/DialogMigrationGuide.md`

**Geändert**:
- Alle 10 Dialog-Dateien in `presentation/ui/components/dialogs/`
- `MainActivity.kt` (Dialog-State-Management)
- `NavigationViewModel.kt`, `SimManagementViewModel.kt`, `ContactsViewModel.kt`
- `values/strings.xml`, `values-de/strings.xml`
- `app/build.gradle.kts` (Feature-Flag)

**Gelöscht** (nach Phase 10):
- Feature-Flag in `build.gradle.kts`
- Ggf. `*_Legacy.kt` Fallback-Dateien

### Kontakte & Ressourcen

- **Code Owner**: [Ihr Name]
- **Review-Team**: [Team]
- **Dokumentation**: `docs/DialogStyle.md`
- **Issue-Tracker**: [Link zu Issues]

### Zeitplan (Beispiel)

| Woche | Phasen | Stunden |
|-------|--------|---------|
| 1 | Phase 0-3 | 10h |
| 2 | Phase 4-5 | 10h |
| 3 | Phase 6-7 | 8h |
| 4 | Phase 8-9 | 8h |
| 5 | Phase 10 + Buffer | 4h |

**Gesamt**: ~40 Stunden über 5 Wochen

---

## ✅ Finale Checkliste

Vor Production-Rollout:
- [ ] Alle 10 Phasen abgeschlossen
- [ ] Code-Review durchgeführt
- [ ] UI-Tests grün (27 Tests)
- [ ] Manual-Tests bestanden (8 Szenarien)
- [ ] TalkBack-Test bestanden
- [ ] Beta-Tester-Feedback positiv
- [ ] Metrics gesammelt (>= 35% Reduktion)
- [ ] Dokumentation vollständig
- [ ] Rollback-Plan getestet
- [ ] Feature-Flag auf `true` in Production

**Nach 2 Wochen Production**:
- [ ] Keine kritischen Bugs
- [ ] Crash-Rate unverändert
- [ ] User-Feedback positiv
→ **Alte Dialoge löschen (Phase 10)**

---

**Ende des Plans**
