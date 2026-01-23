# Repository Guidelines

## Project Structure & Module Organization
- Android app in `app/src/main/java/info/meuse24/smsforwarderneoA1/`; Compose UI and dialogs live under `presentation/ui/components/dialogs/`.
- Shared utils under `app/src/main/java/info/meuse24/smsforwarderneoA1/util/`; resources in `app/src/main/res/`.
- Tests: JVM unit tests in `app/src/test/`, instrumented/Compose UI tests in `app/src/androidTest/`.
- Documentation lives in `docs/` (plans, analyses). Agent configs/scripts belong in `.claude/agents/`; local agent settings in `.claude/settings.local.json` (do not commit secrets).

## Build, Test, and Development Commands
- `./build.sh` — wrapper that sets `JAVA_HOME` correctly on Windows; prefix any Gradle task with it.
- `./build.sh assembleDebug` / `./build.sh assembleRelease` — build unsigned debug / signed release APKs.
- `./build.sh test` — run JVM unit tests.
- `./build.sh connectedDebugAndroidTest` — run instrumented tests on a connected device/emulator (if available).
- `./build.sh lintDebug` — run static checks; use before PRs to catch styling and resource issues.

## Coding Style & Naming Conventions
- Kotlin/Compose defaults: 4-space indent, trailing commas where helpful, camelCase for functions/properties, PascalCase for classes/composables, UPPER_SNAKE_CASE for constants.
- Prefer string resources over hardcoded text; keep contentDescription on icons and semantics for dialogs.
- Reuse shared helpers (e.g., `DialogAnimations.kt`, `DialogTimers.kt`), avoid duplicating countdown/animation logic.
- Keep repo-relative paths in docs; never commit secrets (use env vars or local config).

## Testing Guidelines
- Add unit tests alongside code in `app/src/test/`; UI/Compose tests in `app/src/androidTest/`.
- Name tests after behavior (e.g., `ExitDialogConfirmTriggersCallback`), and keep Arrange-Act-Assert structure.
- Aim to cover new dialog logic and countdown/animation helpers; verify accessibility labels where applicable.

## Commit & Pull Request Guidelines
- Follow the existing Conventional Commit style seen in history: `feat: ...`, `fix: ...`, `docs: ...` (add scope if helpful).
- PRs should include: clear summary of changes, linked issue/ticket, test evidence (`./build.sh test` and UI test results if touched), and screenshots/GIFs for UI changes.
- Note any feature flags (e.g., `USE_NEW_DIALOGS`) and how to toggle them for reviewers.
