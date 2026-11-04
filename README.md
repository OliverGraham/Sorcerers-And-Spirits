# Sorcerers & Spirits

A cross-platform, real-time multiplayer dice-combat game for Android and iOS, built with Kotlin
Multiplatform and Compose Multiplatform from a single shared codebase.

Players pick a class — each with a distinct offensive and defensive special ability — and battle in
an arena where combat is resolved with dice. Matches are grouped into a three-match *Gauntlet*, and
game state is shared and persisted live across devices through Firebase.

> Art and audio are placeholder assets; this repository is shared primarily to show the architecture
> and engineering.

## Tech stack

- **Kotlin Multiplatform** — one shared codebase targeting Android and iOS.
- **Compose Multiplatform** — shared declarative UI, including custom combat animations and a dice tray.
- **Firebase Realtime Database** — the shared, persisted source of truth for live game state.
- **Cloud Firestore** — player profiles and display names.
- **Firebase Auth** — Google and Apple sign-in.

## Architecture

The app is organized into three layers — **Data**, **Domain**, and **UI** — with game state flowing
circularly:

```
Data -> Domain -> UI -> (user chooses action/targets) -> Domain -> Data -> distributed to other clients
```

Two deliberate patterns shape the codebase:

- **Three-layer type mirroring.** Every modeled type exists in all three layers — Domain (the raw
  source of truth), Data (`@Serializable`, `…Entity`, persisted to Firebase), and UI (`@Immutable`,
  `…State`) — converted by `toEntity()` / `toDomain()` / `toState()`. The Domain layer is
  hand-verified and authoritative; Data and UI faithfully mirror it.
- **Derive in the domain, not the UI.** Display-ready values are computed once where the source data
  lives, so Composables only read values rather than recomputing them.

The hand-authored combat engine implements each class's abilities (Teleport, Riposte, Holy Shield,
Spell Potency, and more), the doubles-bonus rule, and turn/round/match/gauntlet sequencing. See
[GameRules.md](./GameRules.md) for the full mechanics specification.

## Project structure

```
composeApp/   Shared Kotlin Multiplatform code (Data / Domain / UI) and Compose UI
iosApp/       iOS entry point (SwiftUI host for the shared Compose UI)
backend/      Apps Script endpoint for Apple account-deletion token revocation
firebase.json, database.rules.json, firestore.rules   Firebase security rules (version-controlled)
```

Within `composeApp/src`:

- `commonMain` — code shared across all targets (the bulk of the app).
- `androidMain` / `iosMain` — platform-specific implementations.

## Build and run

### Android

```shell
./gradlew :composeApp:assembleDebug
```

Or use the run configuration from the run widget in your IDE's toolbar.

### iOS

Open [`/iosApp`](./iosApp) in Xcode and run it, or use the IDE run configuration.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html).
