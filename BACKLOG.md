# Backlog

Active ("not completed") work items ("tickets") for Sorcerers and Spirits. Finished tickets
are archived in [COMPLETED.md](COMPLETED.md).

## How this works

- Each ticket has a stable, globally-unique ID (`SS-001`, `SS-002`, ...). New tickets take the
  next free ID; IDs are never reused, so a `grep` for an ID finds it whether it is active here
  or archived in `COMPLETED.md`.
- TODO comments in the codebase are tagged with their ticket ID, e.g. `// TODO(SS-019): ...`.
- A ticket may cover more than one TODO when they describe the same underlying work.
- When a ticket is finished, move it to `COMPLETED.md` (as a one-line entry) rather than
  checking a box here. Full resolution detail lives in the git history (commit + PR per ticket).

---

## Release

### SS-289 — Release-readiness tracker (what gates shipping)

- [ ] Not completed — **umbrella; the single place to re-read when picking release work back up.**
- **Purpose:** Capture, in one ticket, everything that must be true before Sorcerers and Spirits
  ships, so a future session can start immediately without re-auditing the backlog. This ticket does
  no work itself; it points at the blockers and confirms what is *not* a blocker.
- **Hard blockers (must land before any release):**
    - **SS-088 / SS-089 — store auth config (iOS-gated).** Sign in with Apple is *mandatory* at App
      Store review (Guideline 4.8) once Google is offered on iOS; blocked on the $99 Apple Developer
      enrollment done at release time. Also register release/Play SHA-1 + SHA-256 fingerprints
      (Android) and finish the iOS Google `REVERSED_CLIENT_ID` URL-scheme wiring. **Android can ship
      without the iOS pieces**; only the Play SHA fingerprints block an Android store release.
    - **SS-290 — release signing + R8/minify + version bump.** (untracked until now)
    - **SS-291 — store listing assets + final app icon.** (untracked until now)
    - **SS-292 — privacy policy.** (untracked until now)
    - **SS-293 — crash reporting.** (untracked until now)
- **Explicitly NOT blockers (ship-quality, punt to post-launch):** animation polish (SS-207, SS-263,
  SS-265), the combat/message refactors (SS-257–262), stats/achievements (SS-127), credits polish
  (SS-282), audio (SS-134 + SS-217…SS-221), remaining small-phone tuning (SS-080), config
  single-source/offline-cache (SS-287, SS-288).
- **Correctly deferred:** landscape/wide reflow (SS-083, SS-286) is parked behind the portrait lock
  (SS-082) — do not reopen for launch.
- **Note:** As each child blocker closes, tick it here; SS-289 closes when every hard blocker above is
  done (per platform — an Android-first release closes the Android-relevant set).

### SS-290 — Release build config: signing, R8/minify, version bump

- [ ] Not completed
- **Problem:** `composeApp/build.gradle.kts` ships an unreleasable `release` build type —
  `isMinifyEnabled = false`, **no `signingConfig`** (release build is unsigned / falls back to debug),
  and `versionCode = 1` / `versionName = "1.0"` are untouched defaults.
- **Do:**
    - Add a release `signingConfig` sourced from a keystore via env/`local.properties` (never commit
      the keystore or its passwords); wire it into `buildTypes.release`. For Android store submission
      rely on Play App Signing — this is the upload key.
    - Turn on `isMinifyEnabled = true` (+ `isShrinkResources = true`) and add
      `proguard-rules.pro`; keep rules for Firebase/GitLive, Ktor, kotlinx-serialization, Koin, and
      any reflectively-accessed types (`default_web_client_id` is read reflectively —
      `AndroidSocialSignInProvider`). Verify a release build runs end-to-end (sign-in, arena match,
      config fetch) before trusting the shrink.
    - Bump `versionCode`/`versionName` to the intended first-release values.
    - iOS: confirm the Release configuration's bundle id, signing team, and marketing/build version
      are release-ready (coordinate with SS-089's Apple enrollment).
- **Source:** `composeApp/build.gradle.kts` (`buildTypes`, `defaultConfig`), new
  `composeApp/proguard-rules.pro`. Related: SS-088 (SHA fingerprints for the release key), SS-289.

### SS-291 — Store listing assets + final app icon

- [ ] Not completed
- **Problem:** No store-submission assets exist, and the Android launcher icon
  (`androidMain/res/mipmap-*/ic_launcher*`, adaptive `mipmap-anydpi-v26`) has not been confirmed as
  final art vs. a template.
- **Do:**
    - Confirm/replace the app icon with final art across all densities + the adaptive
      foreground/background layers; add the iOS `AppIcon` asset set (all required sizes).
    - Produce Play Store listing assets: 512×512 icon, feature graphic, phone screenshots (portrait,
      per the SS-082 lock), short + full description, category, content rating questionnaire.
    - Produce App Store listing assets: screenshots per required device sizes, name/subtitle,
      keywords, description, support URL, age rating.
- **Source:** `composeApp/src/androidMain/res/mipmap-*`, `iosApp` asset catalog; external store
  consoles. Asset-prep discipline per [[downsample-figma-raster-assets]]. Related: SS-289.

### SS-292 — Privacy policy (required by both stores)

- [ ] Not completed
- **Problem:** The app collects auth identity (anonymous/Google/Apple) and persists per-user profile
  + game stats to Firebase; both Play and the App Store require a reachable privacy policy URL and
  accurate data-safety / privacy-nutrition-label declarations. None exist.
- **Do:**
    - Write a privacy policy covering: anonymous + Google + Apple sign-in, the userId/displayName +
      GameStats stored in Firebase (RealTime DB + Firestore), and any crash-reporting data (see
      SS-293). Host it at a stable public URL.
    - Fill the Play **Data safety** form and the App Store **App Privacy** ("nutrition label") to
      match what the app actually collects.
- **Source:** external (hosted policy + store consoles); cross-check against
  `FirebaseAuthenticationRepository`, the profile/stats writes, and SS-293's reporting scope.
  Related: SS-289, SS-293.

### SS-293 — Crash & error reporting

- [ ] Not completed
- **Problem:** No crash/error reporting is wired in (no Crashlytics/Sentry/etc. anywhere in the
  build). A first release with no crash visibility means field crashes are invisible — especially the
  device-specific auth/session edge cases we already know exist.
- **Do:**
    - Add a KMP-friendly crash-reporting path. Firebase Crashlytics is the natural fit (Firebase is
      already the backend) — Android has first-class support; for iOS/KMP confirm the integration
      approach (Crashlytics via the iOS Firebase SDK, or a KMP wrapper). Evaluate before committing.
    - Ensure release builds upload symbols/mapping (ties to R8 mapping from SS-290) so stack traces
      deobfuscate.
    - Keep reporting privacy-accurate — reflect whatever it collects in SS-292's policy + data-safety
      declarations.
- **Source:** `composeApp/build.gradle.kts`, `gradle/libs.versions.toml`, platform DI
  (`PlatformModule`). Related: SS-290 (mapping upload), SS-292 (privacy), SS-289.

---

## Arena UI

### SS-274 — Arena Finished: scoreboard-style outcome chips (winner / loser / knocked-out order)

- [ ] Not completed
- **Goal:** In the Finished state, each player's recap row reads like a **scoreboard** line:
    - A `fadedBlackBackground` behind the chip (the faded-black scrim style already used in the
      Finished UI).
    - **Left:** the player's `classAvatarBust`, then their `displayName`.
    - **Right (outcome, varies):**
        - **Winner →** "Winner!" in the **all-class-color animating text** (same treatment as the
          "Next match starting soon!" text — see `NextMatchBounceOverlay` in
          `MatchFinishedContent.kt`).
        - **2-player game →** the loser reads "LOSER" in **grey** letters (depressing).
        - **Multiplayer →** "knocked out {Xth}" per eliminated player, and the **final** player
          knocked out reads **"last"** instead of a numeral. Reads top-to-bottom like a scoreboard.
- **Data needed (open):** there is currently **no knockout-order/placement data** on the domain
  player or `PlayerState.Selected`. Thread a knockout order (or final placement) Domain→Data→UI so
  the
  UI can render "Xth"/"last" — display-ready per [[domain-provides-display-ready-values]]; the UI
  only
  plugs the ordinal into a string. Decide the exact field (elimination index vs final placement)
  during work.
- **Source:** `ui/screens/arena/ui/MatchFinishedContent.kt` (`MatchRecap`/`PlayerRecapRow`,
  `WinnerBanner`, `NextMatchBounceOverlay` for the animating-text treatment),
  `ui/screens/arena/model/PlayerState.kt`, and the domain arena player + its Entity/State mappings
  for
  the new order field. Related: SS-179 (finished text styling), SS-097 (class-color class names),
  SS-275 (winner dance).

### SS-257 — Move combat button/control state into the ViewModel; consolidate the control + argument types

- [ ] Not completed
- **Problem:** Combat button *view* state is computed inside composables instead of the VM, and the
  control/argument types are tangled. TODOs tagged `SS-257`.
- **Move to VM (`ArenaMatchState.Combat` / VM mapping), out of composables:**
    - `CombatButtons.kt` — the whole button-visibility/enablement derivation.
      (`// TODO: Move state to view model layer`.)
- **Consolidate types (they overlap and confuse):** `ArenaCombatControl`, `CombatButtons`,
  `AttackArguments`, `CombatActionArguments`/`UseAbilityArguments`, and the two
  `*.toDomain()` `when`s in `CombatActionState.kt` (likely collapsible to one).
  `CombatControlActions`
  suggests `ArenaCombatControl` may be removable entirely.
- **Sources:** `model/ArenaCombatControl.kt`, `ui/CombatControlActions.kt`,
  `ui/combat/CombatButtons.kt`, `viewmodel/handlers/CombatHandler.kt`, `model/CombatActionState.kt`.
- **Related:** SS-029 is the sibling piece (move combat-action *availability* derivation
  `deriveCombatControls` into the domain + string resources). Do them together.
  [[derived-ui-state-in-state-model]].

### SS-258 — Refactor CombatAnimationCue + AvatarGesture (build gestures at map time, split files, dedupe shared gestures)

- [ ] Not completed
- **Core idea:** gestures are known at VM/message-mapping time (top & bottom seats are known) —
  build
  them into the message instead of querying from the composable. TODOs tagged `SS-258`.
- **Do:**
    - Compute the per-message `gestures` map when mapping `ArenaMessage → ArenaMessageState`, not in
      `CombatAvatarGesture`/composables. (`ArenaMessageState.gestures` TODO;
      `CombatAvatarGesture.rememberAvatarGesture` "determined at VM mapping time".)
    - Split `CombatAvatarGesture.kt` into a package: a shared/utilities file + per-class files. The
      bare `// TODO(SS-258): Common` / `Duelist` markers in `CombatAvatarGesture.kt` and
      `AvatarGesture.kt` tag which gestures are shared vs class-specific — dedupe the shared ones.
    - Rename misleading APIs: `rememberAvatarGesture` also *plays* the animation (should just
      remember); rename `AvatarGestureState` → `AvatarGestureStateHolder`; continue the
      `CombatAnimationCue` rename/move started in `CombatAvatars.kt`; clarify the confusing
      `userPlayerId` pass-through there.
    - Fill the missing/uncertain cue→overlay branches in
      `animation/common/CombatAnimationOverlays.kt`
      (KillingMomentum, Dodge, the "Where is it?" / "should never happen" cases).
    - Consider collapsing `ArenaAnimationState` to a plain enum if it now carries no data.
- **Sources:** `ui/combat/CombatAvatarGesture.kt`, `ui/combat/CombatAvatars.kt`,
  `model/AvatarGesture.kt`, `model/ArenaAnimationState.kt`,
  `animation/common/AvatarGestureState.kt`, `animation/common/CombatAnimationOverlays.kt`,
  `model/ArenaMessageState.kt`. [[sequence-animations-from-domain-data]], [[file-size-and-organization]].

### SS-259 — Message pipeline: move UI-computed message values + grouping into the domain (display-ready)

- [ ] Not completed
- **Rule:** the domain provides display-ready values; the UI must not re-derive them
  ([[message-pipeline-layering]], [[domain-provides-display-ready-values]]). TODOs tagged `SS-259`.
- **Do:**
    - `SummonMinionOutcome` / `AllInOutcome` are recomputed from the single die in
      `ArenaMessageState.kt` (`summonMinionOutcomeFor` / `allInOutcomeFor`) — move these back to the
      domain and pass them through on the message.
    - Message *grouping* (`ArenaMessageGroupState.appendNewMessages` "not-combat" vs
      `addNewMessageDuringCombat` "combat-only", + the Setup handling) belongs in a domain
      MessageLog,
      not two ugly UI functions.
    - Dedupe `CharacterClassMessageInfoState` vs `ParticipantClass` (same shape) in
      `ArenaMessageState.kt`.
    - Rename the domain `NormalRoll` outcome type → `NormalRollOutcomeType` (
      `domain/.../ArenaMessage.kt`).
- **Sources:** `model/ArenaMessageState.kt`, `model/ArenaMessageGroupState.kt`,
  `domain/arena/model/ArenaMessage.kt`.

### SS-260 — Extract ArenaScreenViewModel responsibilities into handlers; tidy the combat-update flow

- [ ] Not completed
- **Do (TODOs tagged `SS-260`):**
    - Pull `sessionStats` (`ArenaSessionStats`) into its own handler and refactor
      `ArenaSessionStats.kt` ("refactor all this").
    - Pull all timer operations (the `Timer` wiring + tick/expire callbacks) into a timer handler.
    - `updateCombatState`: confirm `selectedOpponentUserId` is carried correctly across a combat
      update (`// TODO: Correct?`); consider passing the resolved `arenaMatchState` into
      `notifyDepartedOpponents` during a branching update.
    - `handleScreenStateUpdateError`: decide mid-match error behavior (currently: show error +
      leave).
- **Sources:** `viewmodel/ArenaScreenViewModel.kt`, `ArenaSessionStats.kt`.

### SS-261 — ArenaScreenStateUpdate finished-transition gaps

- [ ] Not completed
- **Gaps (TODOs tagged `SS-261`, all in `model/ArenaScreenStateUpdate.kt`):**
    - `transitionCombatToFinished` uses `MATCH_FINISHED_TIMER_DURATION_SECONDS` unconditionally —
      the final match of a Gauntlet should use a GAUNTLET_FINISHED duration. Distinguish them.
    - `transitionChoosingClassesToFinished` fires when someone leaves *during class selection* —
      decide whether that should kick everyone out.
    - `handleImpossibleScreenUpdate` is a no-op for ArenaMatch/ArenaScreenState combos that should
      never occur — decide the real error behavior.

### SS-262 — Illusionist Impersonate: accent color + dice flow for copied abilities

- [ ] Not completed
- **Do (TODOs tagged `SS-262`):**
    - When the Illusionist impersonates an ability, overlays use the *acted class's* color; the
      accent
      should be the **Illusionist's** color instead (`gambler/GamblerAllIn.kt` accentColor default
      is
      the tell — every borrowed ability overlay has the same issue).
    - Verify the dice-kickoff path in `DiceStateHolder.kt` works for Impersonate (and other
      dice-consuming
      abilities). [[illusionist-borrows-weapons]], [[dice-roll-must-be-roll-bearing]].

### SS-263 — Duelist Riposte animation visual polish

- [ ] Not completed
- **Goal:** the Riposte animation "needs some work, visually" — polish pass on the reversal overlay.
- **Source:** `animation/duelist/DuelistRiposte.kt`. Parent: SS-191. Domain Riposte work is done
  (SS-165, SS-216); this is the animation-only follow-up. Ship interactive previews
  ([[combat-animation-previews]]).

### SS-265 — SpellPotency: possible accent/color palette update when impersonated

- [ ] Not completed
- **Goal:** SpellPotency's overlay tints from a random *elemental* palette (`element.palette().glow`
  in `animation/mage/MageSpellPotency.kt`), not the acting class color, so the borrowed-ability
  accent fix (SS-262) does not touch it. Decide whether an impersonating Illusionist should get a
  distinct palette (e.g. Illusionist-blue elemental variant) instead of the standard Mage elements.
- **Source:** `animation/mage/MageSpellPotency.kt` (`mageElementFor`, `MageSpellElement.palette()`).
  The actor's real class is now available on the message via `actingClassType` (SS-262). Related:
  SS-262, [[illusionist-borrows-weapons]]. Ship interactive
  previews ([[combat-animation-previews]]).

### SS-178 — One reusable button for the whole app (text + icon, incl. combat)

- [ ] Not completed
- **Goal:** A single reusable button component every screen uses — no per-screen re-implementations.
  It must cover both **text** buttons and **icon** buttons (icon fills the face minus a small inner
  border), and it must cover the **combat** buttons, which are the reason a shared button was wanted
  in the first place.
- **Context / why this exists:** `AppButton` (`ui/common/AppButton.kt`) was meant to be that one
  button, but it shipped as a text-only rounded-rect and never actually covered the combat buttons.
  Those still live separately in `ArenaButton` (`ui/screens/arena/ui/combat/CombatButtons.kt`) and
  differ in ways the current `AppButton` can't express: a **circular** shape (vs. 14.dp rounded
  rect), a **hold-to-roll gesture** (vs. a plain click) that drives the shared dice tray with
  per-die `diceSlots`, an optional **label** under the face, and class-colored base darkening. So
  "one button everywhere" needs `AppButton` to grow a configurable shape + a press/gesture model,
  then `CombatButtons.kt` converted onto it and its private button deleted.
- **Done so far (SS-178, partial):** combat icons now fill their face — `ARENA_BUTTON_ICON_SIZE`
  was replaced with `Modifier.fillMaxSize().padding(all = 8.dp)` in `ArenaButton`. This fixes the
  small-icon symptom but does NOT consolidate the components.
- **Remaining:** make `AppButton` shape- and interaction-configurable enough to host the combat
  buttons, migrate `CombatButtons.kt` onto it, and delete the duplicate button. Confirm the
  hold-to-roll/dice behavior is preserved (combat-regression risk).
- **Related:** SS-160 (3D button style), and the shared-AppButton work (PR #181).

### SS-029 — Move combat-action availability to the domain + StringResources in ArenaCombatControl

- [ ] Not completed
- **Source:** `ArenaCombatControl.kt` (`// TODO(SS-029): Always StringResources`),
  `deriveCombatControls`
- **Goal (Phase 2 of the ArenaScreenState TODO cleanup):**
    - Move the combat-action *availability* logic (`ArenaState.Combat.deriveCombatControls`) into
      the
      domain, producing plain domain action types for the current user (no UI types, no labels). The
      domain already has the userId + playerMap to compute this.
    - Remove the hardcoded `label: String` from `ArenaCombatControl`; the UI maps action type →
      `stringResource` at render time (the type carries the identity). Dynamic labels (e.g. Mirror
      Image "Reroll the 3") carry the die value as data and use a parameterized string resource.
- **Note:** Phase 1 (turn owner / locked target / attacker resolution) already moved to the domain;
  this is the remaining piece.

### SS-138 — Export head-icon stat icons as SVG (speed, armor, hitpoints)

- [ ] Not completed — **blocked on Figma MCP rate limit** (Starter plan) at time of writing; retry
  the asset download once the limit resets.
- **Source:** Figma `Arena` node `75:159`. The three stat icons in the head icon
  (speed chevron, armor, hitpoints/heart) are currently raster PNGs in the repo
  (`composeResources/drawable/ic_speed.png`, `ic_armor.png`, `ic_health.png`).
- **Goal:** Export each stat icon **individually as SVG** from Figma and add them as vector
  drawables
  so the head icon (SS-136) renders crisp at any size. (The class avatar itself stays raster art;
  the
  class-color border is drawn in code, not exported.)
- **Steps:** Use `download_assets` with `defaultFormat: svg` on the icon child nodes; add the
  vectors
  under `commonMain/composeResources/drawable`; update SS-136 to reference the SVGs instead of the
  PNGs; remove the superseded PNGs if nothing else depends on them.
- **Notes:** Related: SS-136. Asset-prep discipline per [[downsample-figma-raster-assets]].

> The two head-icon transition targets originally tracked here as SS-140 (head → large targeted
> avatar) and SS-141 (head → expanded stat view) were placeholders; both are now realized concretely
> by the ArenaCombat layout — SS-144 (target-selection transition) and SS-150 (inspect a player),
> respectively — so the placeholder tickets were removed.

### SS-255 — Message log open/insert animation reads wrong (topmost line doesn't move)

- [ ] Not completed
- **Symptom:** When the log opens (and when a new line is revealed), the **topmost** message stays
  put
  while everything below it slides — it should all move together as the content shifts.
- **Goal:** Make the log content animate as a unit on open and on insert (the whole column slides),
  rather than the first item anchoring while the rest animate via `animateItem()`.
- **Source:** `ui/screens/arena/ui/message/ArenaMessageLog.kt` (per-item `animateItem()` +
  most-recent-first ordering + auto-scroll-to-top) and the unfurl height animation in
  `MessageLogOverlay` (`CombatMessages.kt`). Likely needs the unfurl to translate the content down
  rather than reveal it top-anchored.
- **Notes:** Follow-up to SS-251; needs on-device animation iteration.

## Combat animations

Pure-Compose avatar animations for the two seated large avatars during ArenaCombat. The system
(SS-191) is the extensibility seam; the per-class tickets (SS-192…SS-199 normal attacks,
SS-203…SS-212
abilities) fill in their animations on top of it. No Lottie/Compottie — plain Compose `Animatable`/
`graphicsLayer` transforms plus
`ColorMatrix` color effects on the existing avatar art (SS-068 dropped Lottie).

Owner direction (2026-07-02): every class gets a **unique attacker-side animation set for normal
attacks** (its hit / miss / knockout strikes); the **defender-side reactions stay shared** (Flinch /
Sidestep / Collapse — may be revisited later). Each class now has a dedicated normal-attack ticket
plus one ticket per ability, worked one at a time. The seam is ready:
`ArenaMessageState.NormalRoll` already carries `attackerClassType`, so a class ticket threads it
into the `CombatAnimationCue.NormalAttack` cue and branches the attacker gesture on it —
SS-191's generic lunges remain the fallback for classes not yet done.

### SS-207 — Bard Persuade animation

- [ ] Follow-up pending
- **Goal:** the moment the Bard's armor is passed a lute appears in front of them and holds through
  the roll. On the roll the strings vibrate as if just strummed and music notes appear around the
  lute; a not-stopped attack turns the lute and everything gray and drops the notes sideways
  (failure), a stopped attack keeps everything in color and gives the notes a flourish. On a
  charming 6 (multiplayer with a legal redirect target) the lute plays the same success animation as
  a 5, then crossfades into a flute; once Persuade's redirect resolves the flute looks played (air
  moving through it) wreathed in swirly, hypnotic notes — snake-charming visuals. Instruments should
  look 3D.
- **Done (first pass, shipped):** the full choreography, gray/fall-vs-flourish split, lute→flute
  crossfade, air-wisps, and hypnotic swirl all landed, plus the cue/phase wiring, `bardUserId`
  plumbed through the Persuade domain/entity/state messages, and per-phase previews
  (failed/stopped/charm). The lute and flute are hand-drawn Canvas shapes for now (`BardLute.kt`).
- **Follow-up:** replace the hand-drawn lute and flute in `BardLute.kt` with image assets for a
  truer 3D look (keep the strum vibration, note behaviors, gray drain, crossfade, air, and swirl
  choreography); do a polish/tuning pass on proportions, colors, and timing once seen on-device; and
  consider making the charm swirl read more literally as a snake-charm coil. Then close the ticket.
- **Cue sources:** `ArenaMessageState.Persuade.*`.
- **Parent:** SS-191. Split out of SS-196.

---

## Config & Infrastructure

### SS-287 — Single source of truth for config JSON (stop hand-maintaining two copies)

- [ ] Not completed
- **Context:** SS-277 landed with the live config in the public repo
  `OliverGraham/sorcerers-and-spirits-config` and an identical *bundled* copy under
  `composeApp/src/commonMain/composeResources/files/remote_config/*.json` (the offline/first-launch
  default baked into the APK). Some baked-in snapshot is unavoidable, but hand-maintaining both
  copies lets them silently drift. Owner does not want two hand-edited copies.
- **Goal:** Make the **public config repo the single source of truth** and derive the bundled copy
  automatically, so config is edited in exactly one place. Preferred approach: a Gradle task (or CI
  step) that fetches the config repo's JSON into `composeResources/files/remote_config/` before
  packaging, so the bundled files become a generated build artifact, not hand-edited. Consider a git
  submodule as an alternative, but weigh the Compose-resources pathing/CI friction.
- **Constraints:** the offline default must still ship in the APK (no network on first launch); the
  fetch must be pinned/reproducible (a specific ref/commit, not a moving `main`, for release builds)
  so a build is deterministic. Keep `GithubJsonRemoteConfigDataSource.RAW_BASE_URL` as the runtime
  source.
- **Source:** `GithubJsonRemoteConfigDataSource`, `composeApp/build.gradle.kts`, the bundled
  `remote_config/*.json`. [[config-hosted-in-public-repo]], [[connectivity-resilience-priority]].

---

### SS-288 — Persist last-fetched config across launches (ETag/304 offline cache)

- [ ] Not completed
- **Context:** SS-277 does fetch-per-launch, in-memory only. On an offline launch the app falls back
  to the *bundled* (build-time) config rather than the *last successfully fetched* config.
- **Goal:** Persist each fetched payload + its `ETag` (DataStore) and send `If-None-Match`; treat
  `304` as "keep the cached body". An offline launch then uses the last-fetched config instead of the
  older bundled default — better freshness under intermittent connectivity, and less bandwidth.
- **Constraints:** bundled JSON remains the ultimate fallback when nothing has ever been fetched;
  keep the fetch-per-launch cadence (no throttle) unless a reason emerges.
- **Source:** `GithubJsonRemoteConfigDataSource`, `androidx-datastore-preferences-core`.
  [[config-hosted-in-public-repo]], [[connectivity-resilience-priority]].

---

## Credits

### SS-282 — Finalize CreditsScreen text types (header / body / etc.) so animations are complete

- [ ] Not completed
- **Goal:** Formalize the credits **text types** (header, body, and any others) in the Remote Config
  schema and the domain/UI model, replacing SS-281's placeholder `title + entries` shape, and render
  each type with its own styling so the scroll reads properly (section headers vs body lines,
  spacing,
  emphasis).
- **Source:** builds on SS-281 — the `credits.json` schema, `CreditsEntity` / `CreditsSection`
  (domain), `CreditsState`, and the rendering in `CreditsScreen.kt` (`CreditsSection`). Decide the
  type set (at least header + body) and mirror it Domain→Data→UI. **Depends on SS-281.**

---

## Domain layer

### SS-048 — Die-value enum

- [ ] Not completed
- **Source:** `DiceRoller.kt:16` (`// TODO Enums for values 1 through 6?`)
- **Goal:** Consider a type-safe enum for die faces (1–6) instead of raw Ints.

---

## Bugs

### SS-235 — Persuade: crash + multiple UI bugs (owner to spec the intended UI before work)

- [ ] Core work done and verified by owner (3-player auto-target + 4+ player choose); ticket stays
  open only for the small follow-up TODOs below.
- **Done:** Fixed the crash (Charm resolution left `CombatMutator.nextCombatAction` unset →
  `getOutcome()` lateinit throw → Bard client crash / "Bard left"). Implemented per owner spec:
  single legal target auto-redirects; ≥2 targets → Bard taps a stands avatar to choose (no Charm
  button); lute strum → flute crossfade on the Bard → swirling air travels to the chosen head icon →
  avatars swap last. Fixed the redirect-target propagation (redirect now clears
  `currentTargetUserId` so seats hold during the animation and swap from `combatActionState`
  afterward, on all clients — also removed the SS-267-style gesture hang on repeat redirects).
- **Remaining follow-up TODOs (not blockers):**
    - `ArenaCombatControl.toAbilityRollControls` (Charm → emptyList): decide whether choose-a-target
      should be confirmed with a button instead of commit-on-tap.
    - `CombatClassIcons` ClassIcon `onClick` branch: clean up the charm/target/inspect if-else.
    - `BardPersuade` (~line 83): make the flute animation more dramatic.
    - `BardPersuade` (~line 120): make `charmTargetBounds` more explicit via the message type.
- **Source:** `domain/.../ability/passive/PersuadeHandler`, `CombatMutator`, Persuade combat controls,
  `bard/BardPersuade.kt` / `bardUserId` plumbing (SS-207).
- **Related:** SS-207 (Persuade animation), SS-267 (gesture-key hang), game rule: Persuade redirect
  needs ≥2 alive non-Bard players.

### SS-267 — Combat queue can hang when a gesture participant isn't a seated avatar (SpellPotency splash targets)

- [ ] Not completed
- **Introduced by:** the keyed animation-completion rework in `ArenaMatchCombatQueue`
  (`MessageAnimationCoordinator`). Each message's expected-animator set is seeded up front from the
  message: `AnimationKey.FlashMessage`, `AnimationKey.AvatarOverlay`, and one
  `AnimationKey.AvatarGesture(userId)` per entry in `gestures.keys`. The coordinator only resolves
  once **every** expected key has reported via `onAnimationComplete`.
- **Symptom / hazard:** a gesture is only reported by a mounted, seated `GestureClassAvatar` (top =
  targeted seat, bottom = attacker seat). If a message's `gestures` map names a userId that is **not
  **
  seated — rendered only in the stands (`ClassIcons`), or otherwise not composing a
  `GestureClassAvatar` — that `AvatarGesture(userId)` key is never reported, the coordinator's
  `completion` never completes, `await()` blocks forever, and the queue stalls. That's the same
  end-state SS-256 fought (combat controls never return, match becomes unplayable).
- **Identified case:** SpellPotency. Its `gestures` map carries the mage (`Cast`) and the main
  target
  (`Collapse`/`FlinchHeavy`). Confirm whether the main target (and any secondary/splash target that
  ever receives a gesture) is guaranteed to be one of the two on-screen seats; splash victims live
  in
  the stands and must never end up as an expected gesture key while unseated.
- **Fix direction:** restrict the expected-animator set to participants that will actually mount and
  report (only the seated attacker/target), or give the coordinator a bounded fallback so an
  unreported animator can't hang the whole queue. Decide which in `getExpectedAnimationKeys`.
- **Related:** SS-256 (the queue/gating rearchitecture this rides
  on), [[never-drain-loop-combat-queue]],
  [[combat-queue-signal-reentrancy]], [[combat-button-visibility-isanimating]].

## Profile & Auth

### SS-088 — Firebase console: enable Google/Apple auth providers

- [~] Partially done — **Android Google: DONE & verified live.** iOS Google + Apple: pending.
- **Source:** SS-061 follow-up (console + Apple Developer portal; no app code beyond re-downloading
  the Firebase config files). Project: `sorcerers-and-spirits`. Blocks SS-086 (Android Google) and
  SS-087 (iOS Google + Apple).
- **Goal:** Turn on the Google and Apple sign-in providers so the credentials linked by
  SS-086/SS-087
  are accepted, and get the generated `default_web_client_id` into the app so SS-086 flips on.
- **Status:** Android Google completed — Google enabled in console, debug SHA-1/SHA-256 registered,
  `google-services.json` re-downloaded (now has the type-3 web client), build generates
  `default_web_client_id`, `assembleDebug` clean → Android Google sign-in is live. **Outstanding:**
  release/Play SHA fingerprints (at store-release time), the whole iOS Google sub-section, and the
  whole Apple sub-section.
- **Apple is deferred until App Store release:** Sign in with Apple requires a **paid Apple
  Developer
  Program membership** ($99/yr), which the owner plans to buy at release time. It is also *
  *mandatory**
  per **App Store Review Guideline 4.8** — once we offer Google on iOS, we MUST offer Sign in with
  Apple — but that's only enforced at App Store review, which coincides with enrollment. So Apple
  (and the iOS Google portal work) stays parked until then; the Apple button is dormant meanwhile
  and
  nothing breaks. See SS-089.
- **Steps — Android Google (unblocks SS-086) — ✅ DONE:**
    1. Firebase console → **Authentication → Sign-in method → Add new provider → Google → Enable**,
       set
       a support email, **Save**. Enabling Google auto-creates an OAuth **Web client** ("Web client
       (auto created by Google Service)").
    2. Register the app's signing-cert fingerprints so Credential Manager trusts the build:
       console →
       **Project settings → Your apps → Android app → Add fingerprint**, add both **SHA-1** and
       **SHA-256** for every keystore that ships a build:
        - Debug:
          `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
        - Release / Play App Signing: copy the SHA-1 + SHA-256 from **Play Console → Release →
          Setup →
          App signing** (and the upload key too).
    3. **Re-download `google-services.json`** and replace `composeApp/src/google-services.json`.
       Confirm
       it now contains an `oauth_client` with `client_type: 3` (the web client). The google-services
       Gradle plugin then generates the `default_web_client_id` string resource, which
       `AndroidSocialSignInProvider` reads reflectively — Google sign-in goes live with no code
       change.
    4. Verify: clean build, tap **Continue with Google** on a device/emulator with Play Services + a
       Google account; the account picker should appear and linking should succeed.
- **Steps — Apple (unblocks SS-087's Apple path):**
    1. Apple Developer portal → create a **Services ID**, enable **Sign in with Apple**, and
       configure
       the return URL to the Firebase handler (
       `https://sorcerers-and-spirits.firebaseapp.com/__/auth/handler`).
    2. Create a **Sign in with Apple key** (.p8) and note the Key ID + Team ID.
    3. Firebase console → **Authentication → Sign-in method → Apple → Enable**, enter the Services
       ID,
       Apple Team ID, Key ID, and the .p8 private key, **Save**.
    4. Add the **Sign in with Apple** capability to the iOS app target in Xcode (SS-087).
- **Steps — iOS Google (unblocks SS-087's Google path):** the iOS Firebase config carries a
  `REVERSED_CLIENT_ID`; re-download `GoogleService-Info.plist` after enabling Google above and add
  the `REVERSED_CLIENT_ID` as a URL scheme in the iOS target (SS-087 already added the GoogleSignIn
  SPM package wiring in code).
- **Note:** Anonymous auth is already enabled and unaffected; existing anonymous accounts upgrade in
  place via `linkWithCredential` (no user migration).

### SS-089 — Enable Sign in with Apple before App Store release

- [ ] Not completed — **blocking for iOS App Store release; parked until Apple Developer enrollment.
  **
- **Source:** SS-061 / SS-087 follow-up. The Apple *code* is done (SS-087); this ticket tracks the
  account-gated config that can only happen with a paid membership.
- **Why mandatory:** **App Store Review Guideline 4.8** — offering Google sign-in on iOS requires
  also offering Sign in with Apple. Enforced at App Store review.
- **Blocked by:** a **paid Apple Developer Program membership** ($99/yr), planned for release time.
  Until then the iOS Apple button is dormant and the app still works.
- **Steps (do together once enrolled):**
    1. Apple Developer portal: Services ID + Sign in with Apple key (.p8) — see SS-088 "Steps —
       Apple".
    2. Firebase console: enable the Apple provider with those values — see SS-088 "Steps — Apple".
    3. Xcode: add the **Sign in with Apple** capability to the `iosApp` target — see SS-087.
    4. Verify on device/simulator: the Apple button presents the sheet and linking succeeds.

---

## Game Stats & Leaderboard

Game statistics are recorded on each user's Firestore profile and will be a large part of the game —
directly driving the Leaderboard and other features. The work is staged deliberately: **first**
build
the generic infrastructure for persisting stats to a profile (SS-021), keeping it stat-agnostic;
**then** decide which concrete stats to record (SS-072) and wire them into match outcomes (SS-071);
and finally surface them (SS-064 ProfileScreen, SS-065 Leaderboard).

### SS-127 — Per-ability event achievements (thematic two-per-ability set)

- [ ] Not completed
- **Goal:** Add the in-match, event-driven achievements that can't be derived from cumulative
  counters alone — two thematic achievements per ability — building on the SS-074 engine.
- **Approach:** These need in-match event detection and/or new per-ability `GameStats` counters
  (e.g. `ripostesTriggered`, `encoresTriggered`, …) plus transient in-match event state (e.g.
  KOs-in-a-row, triggers-this-turn/round, "won after self-KO earlier this match"). Follow the SS-074
  pattern: new `AchievementId` + data object + string resources. To add a counter: field on
  `GameStats` + mirror on `GameStatsEntity` + map in `toFieldIncrements`.
- **Starter set (two per ability):**
    - Rogue · Killing Momentum: **Chain Reaction** (3 KOs in a row) · **Table Sweeper** (5 in a row)
    - Rogue · Dodge: **Untouchable** (gain Dodge armor) · **Slippery Target** (Dodge 3× in a round)
    - Gambler · All-In: **High Roller** (attack twice via All-In) · **Against the Odds** (win after
      self-KO'ing earlier in the match)
    - Gambler · Lucky Coin: **Coin Flip Justice** (roll 6, damage attacker) · **Paid in Full** (KO
      attacker with Lucky Coin damage)
    - Duelist · Prepare Attack: **Opening Strike** (KO via Prepare Attack) · **No Duel Required** (
      win
      on a Prepare Attack KO)
    - Duelist · Riposte: **Reversal** (turn an attack around) · **Back At You** (KO attacker via
      Riposte)
    - Necromancer · Summon Minion: **Bring Out the Golem** (summon a Golem) · **Death's Entourage**
      (win after summoning a Skeleton and a Golem)
    - Bard · Encore: **Final Chorus** (KO with an Encore attack) · **Standing Ovation** (Encore
      twice
      in one turn)
    - Bard · Persuade: **Change of Plans** (redirect an attacker) · **Friendly Fire** (redirect into
      a
      KO of another player)
    - Illusionist · Impersonate: **Anything You Can Do** (KO with a copied ability) · **Identity
      Theft** (win using a copied ability)
    - Illusionist · Mirror Image: **Trick of the Light** (prevent damage) · **Impossible Target**
      (survive 3 attacks in one game after using it)
    - Mage · Spell Potency: **Chain Explosion** (KO with Spell Potency) · **Cataclysm** (KO multiple
      in one trigger)
    - Mage · Teleport: **Blink Away** (escape an attack) · **Gone Before Impact** (escape a would-be
      KO)
    - Paladin · Holy Shield: **Divine Intervention** (survive a KO) · **Martyr's Revenge** (KO
      attacker with Holy Shield damage)
- **Notes:** Depends on SS-074. Domain Arena-combat is the source of truth — event hooks must read
  real combat resolution, not re-derive it. Related: SS-074, SS-072, SS-131.

---

## Responsive & form factors

### SS-080 — Responsive scaling for small/medium phones

- [ ] Not completed
- **Source:** Observed on the spinning-wheel selector (SS-079): on a Small Phone the bottom stats
  card is cut off; on a Medium phone the *expanded* stats card overflows (description copy clipped).
- **Goal:** Make the Tavern/Arena class-selection layout (wheel + stats card) fit within the
  smallest
  supported phone, collapsed and expanded, without clipping.
- **Steps:** Audit the class-selection screens across the small/medium phone reference sizes; make
  the wheel size and the stats card height adapt (e.g. cap/scale the wheel by available height, let
  the expanded card scroll or size to content within a constrained viewport); verify on Small +
  Medium previews/emulators. Short copy (SS-081) will also reduce overflow.
- **Progress:** Responsive foundation landed in the `theme` package: `ScreenSizeClass`
  (Small/Medium/Large) derived from the live window width via cross-platform
  `LocalWindowInfo.containerSize` (Android + iOS, no adaptive-lib dependency — its actual-size API
  is Android-only and its multiplatform buckets can't split small vs medium phones), exposed as
  `LocalScreenSizeClass`. Medium is the unscaled baseline.
  Scaling is now **app-wide by construction**: `SorcerersAndSpiritsTheme` overrides `LocalDensity`
  once — `density *= dpScale`, `fontScale = fontScale * textScale / dpScale` — so **every `dp` and
  `sp` in the whole app scales from one place**, no per-widget annotation. Text scales harder than
  dp chrome: `textScale` Small 0.75×/Large 1.1×, `dpScale` Small 0.85×/Large 1.1× (both tunable in
  `ScreenSizeClass.kt`). The window bucket is read from the real density before the override, so it
  can't feed back. Replaced the earlier `Typography.scaledBy` + per-widget `Dp.scaledBy` approach
  (deleted `ScaledTypography.kt`) — it was unrealistic across ~426 dp literals in ~60 files and left
  things like the arena combat buttons (`arenaButtonSize = 72.dp`) oversized on small phones; those
  now shrink automatically. Combat avatars are proportional (`BoxWithConstraints`) so density leaves
  them untouched; window insets round-trip (same density both directions).
  Wheel + stats-card fit: `CharacterClassWheel` is now a root `BoxWithConstraints`; when the parent
  bounds the height it caps the wheel to `WHEEL_HEIGHT_FRACTION` (0.6) of it so the stats card
  always
  keeps room, and the stats card takes the remaining space via `weight(fill = false)` with the
  card's
  content in a `verticalScroll` so an expanded card scrolls instead of clipping. Unbounded-height
  hosts keep the old wrap-content behavior. The wheel is being moved off `TavernScreen` (overhaul
  pending), so Tavern-specific fit is deferred to that work.
  Remaining: on-device verify on Small + Medium phones (combat screen, dialogs, dice tray, wheel
  collapsed + expanded); tune the `dpScale`/`textScale` factors against real devices if needed.
- **Notes:** Distinct from tablet (SS-083) and landscape (SS-082). Related: SS-079, SS-081. The
  `ScreenSizeClass.Large` bucket also seeds SS-083 (tablet).

### SS-082 — Adaptive layout foundation (no orientation lock)

- [ ] Not completed
- **Decision (2026-09-20 — supersedes 2026-09-15):** Orientation is now **locked to portrait** on both
  platforms to get closer to shipping. Android: `android:screenOrientation="portrait"` on
  `MainActivity`. iOS: `INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone`/`_iPad` set to
  `UIInterfaceOrientationPortrait` only (both Debug + Release configs). This defers all landscape/wide
  reflow work — SS-083 and SS-286 are blocked until the lock is lifted. The prior "no orientation lock,
  ever" goal is a post-ship aspiration, not a shipping requirement.
- **Prior decision (2026-09-15, superseded):** No orientation lock, ever, on any platform. The app must
  adapt correctly to any window — portrait/landscape phones, foldables, split-screen/multi-window,
  tablets, desktop. Locking to portrait was explicitly rejected: the target is doing it right, not the
  easy way out.
- **Goal:** Build the adaptive-layout foundation and apply the cheap, shared treatment to all flow/list
  screens, so nothing stretches full-bleed and every screen survives free rotation without crashing.
- **Key insight:** The existing `ScreenSizeClass` (SS-080) is **width-only and only drives *scaling***
  (the `LocalDensity` override) — it makes things bigger/smaller but does **not reflow**. That is a
  separate concern and is exactly why ArenaCombat looks bad in landscape. This ticket adds the *reflow*
  signal; it does not touch the scaling foundation.
- **Steps:**
  - Add a cross-platform window-metrics signal carrying width **and** height (→ orientation/aspect),
    sourced from `LocalWindowInfo.current.containerSize` (the iOS-safe source per SS-080's findings).
    Provide it as a `LocalWindowSizeClass` alongside `LocalScreenSizeClass`. Screens read it to pick a
    layout.
  - Do NOT use the Google `adaptive` lib / Nav3 `NavigationSuiteScaffold` / `ListDetailPaneScaffold`
    machinery — Android-only, and this is a KMP single-view-per-state game, not a nav/list-detail
    productivity app. Roll the thin signal ourselves (SS-080 already established the adaptive lib fails
    on iOS).
  - Screen categorization: **flow/list screens** (Tavern, Profile, Leaderboard, Achievements,
    GameRules, Credits) need one *shared* reusable treatment — a centered **max content width**
    container (optional multi-column at wide widths). No per-screen design.
  - Verify every screen survives rotation / window resize without crashing or losing state (state is
    already hoisted in ViewModels).
- **Out of scope (own tickets):** the two *spatial* screens that need genuinely designed second
  layouts — class wheel + stats card → SS-083; ArenaCombat → SS-286.
- **Notes:** Builds on SS-080 (scaling). Reference: `~/.android/skills/jetpack-compose/adaptive`
  (Android-centric; adapt, don't follow literally). Related: SS-079, SS-081, SS-083, SS-286.

### SS-083 — Class wheel + stats card: wide/landscape reflow

- [ ] Not completed
- **Blocked (2026-09-20):** Orientation is locked to portrait for shipping (SS-082). This landscape/wide
  reflow is deferred until the lock is lifted.
- **Goal:** Give the class-selection wheel + stats card a designed landscape/wide layout: **stacked
  (tall)** vs **side-by-side (wide)** — wheel beside the stats card instead of above it.
- **Depends on:** SS-082 (the `LocalWindowSizeClass` signal). This is the tablet/large-screen half of
  landscape support (tablets are commonly held landscape).
- **Steps:** Using SS-082's signal, branch the class-selection layout between the current stacked
  portrait arrangement and a side-by-side wide arrangement; set sensible max widths so the wheel/card
  don't over-stretch on very wide windows. Review at foldable, tablet, and desktop window sizes.
- **Notes:** The wheel is being moved off `TavernScreen` (overhaul pending, per SS-080 notes) — keep
  the reflow in the wheel/stats-card components so it follows the wheel to its new home. Related:
  SS-080, SS-082, SS-286.

### SS-286 — ArenaCombat: wide/landscape reflow

- [ ] Not completed
- **Blocked (2026-09-20):** Orientation is locked to portrait for shipping (SS-082). This landscape/wide
  reflow is deferred until the lock is lifted.
- **Goal:** Give the ArenaCombat screen a designed landscape/wide layout. Today it stacks
  arena/avatars over the dice tray + action buttons and looks bad in landscape.
- **Depends on:** SS-082 (the `LocalWindowSizeClass` signal). Hardest of the three spatial reflows;
  broken out into its own ticket.
- **Steps:** In `LoadedArenaScreen`, each `ArenaMatchState` branch (`ChoosingClasses`, `Combat`,
  `Finished`) owns its composable, so the reflow lives cleanly inside each. For `Combat`: side-by-side
  in wide windows — arena board/avatars on one side, dice tray + action buttons on the other — instead
  of stacked. Ship an interactive Compose preview for the new wide layout per the combat-preview
  convention.
- **Notes:** Verify the combat animation/sequencing overlays (weapon travel, message-log overlay,
  seat-hold) still land correctly under the reflowed geometry — avatar bounds are proportional
  (`BoxWithConstraints`) so should follow, but confirm. Related: SS-082, SS-083.

---

## Tavern

### SS-126 — Shard the Tavern so presence churn stays bounded at scale

- [ ] Not completed
- **Depends on:** SS-125 (delta-sync, done) — sharding caps how many travelers a client subscribes
  to; delta-sync caps the cost per change. Only build this once a single shard's churn is actually
  the bottleneck.
- **Goal:** Keep per-client presence cost bounded regardless of total Tavern population. Even with
  delta-sync, a single `TavernTravelers` node with ~50 people coming and going means every client
  still receives every join/leave/status delta for everyone. Sharding partitions travelers into
  capped rooms so each client only subscribes to its own shard.
- **Design (to be refined):**
    - Partition presence under `TavernTravelers/{shardId}/{userId}` with a soft cap per shard (e.g.
      15–20). Assign a traveler to a shard on entry (fill-then-open: join the least-full non-full
      shard,
      create a new shard when all are full). Store the assigned `shardId` so disconnect cleanup and
      status writes target the right child; the existing `onDisconnect().removeValue()` registration
      moves to the sharded path.
    - Each client subscribes (via the SS-125 child-event delta sync, already in place) only to its
      own
      shard, so fan-out is bounded by shard size, not total population.
    - **Challenge across shards:** challenges already work by `userId` through the mailbox/challenge
      paths (`ChallengeMailboxes`, `Challenges`), which are not shard-scoped, so cross-shard
      challenges
      keep working as long as the challenge handshake addresses travelers by id, not by shared list
      membership. Verify `claimChallengers` / presence transactions still resolve a traveler in
      another
      shard.
    - **Search across shards (ties to SS-124):** once a traveler isn't necessarily in your shard,
      name search can no longer be a pure local filter. Add a lightweight name/presence index (e.g.
      a
      flat `TavernTravelerIndex/{userId} -> {displayName, shardId, status}`) to look up and jump
      to /
      challenge a traveler in another shard. Decide whether the index is worth it or whether search
      stays shard-local.
    - **Open questions:** rebalancing when shards drain unevenly; how the in-tavern count is computed
      across shards; whether a global
      population threshold gates sharding on/off so small taverns stay single-node.
- **Note:** this is the larger, speculative piece — don't build it until population actually
  warrants
  it. SS-125 is expected to carry the load until then.

---

## Audio

### SS-134 — Music & sound-effect playback (KMP)

- [ ] Not completed
- **Status (foundation shipped):** The KMP audio plumbing is built and merged — `commonMain`
  `MusicPlayer` / `SoundEffectPlayer` interfaces + `MusicTrack` / `SoundEffect` sealed catalogs (no
  entries yet), Android `AndroidMusicPlayer` (Media3 ExoPlayer, gapless loop + volume cross-fade)
  and
  `AndroidSoundEffectPlayer` (SoundPool), iOS `IosMusicPlayer` (`AVAudioPlayer` fade) and
  `IosSoundEffectPlayer` (pooled `AVAudioPlayer`), and Koin DI in both `PlatformModule`s. Nothing
  plays yet: no asset files, no catalog entries, no trigger wiring, no mute UI, no remote-URL
  override. Those are decomposed into SS-217…SS-221. This umbrella closes once its children land.
- **Goal:** Add an audio layer to the app covering two distinct kinds of playback:
    1. **Long-running music tracks** — background music that plays on a screen and persists/loops
       while
       the user is there. When navigation switches the active track, the outgoing track **fades out
       ** and
       the incoming one **fades in** (cross-fade) so transitions aren't abrupt.
    2. **Short sound effects (SFX)** — quick, fire-and-forget, low-latency one-shots triggered by
       game
       events (e.g. a player landing a hit in the Arena, armor breaking, a K.O., dice settling).
       Many can
       fire in rapid succession and must overlap without cutting each other off.
- **Two playback paths / two formats (decided):** the two kinds have different runtime needs, so
  they
  use different formats and different players. The deciding constraint is what **both** Android and
  iOS
  decode **natively** (no bundled third-party codec):
    - **Music → AAC in an `.m4a` container.** Compressed, streamed/decoded-on-the-fly to keep memory
      and
      app size down; native on both platforms (iOS's preferred format) with the best
      quality-per-byte,
      and the `.m4a` container's encoder-delay/padding metadata gives **gapless loops** (important
      for
      repeating background tracks). Encode **44.1 kHz, stereo, ~128–160 kbps VBR**. *Why not OGG
      Vorbis*
      (the obvious small/Android-native pick): iOS has no native Vorbis decoder, so it'd need a
      bundled
      decoder — not worth it. MP3 is an acceptable equal-footing fallback but loops less cleanly, so
      AAC
      wins for looping music.
    - **SFX → WAV (16-bit PCM).** Short, low-latency, decoded-into-memory so they trigger instantly
      and
      overlap. Uncompressed means **zero decode latency and no codec priming silence** clipping the
      attack of the sound (e.g. a hit landing), and one WAV asset plays natively on both platforms.
      Encode **44.1 kHz, 16-bit, mono** (stereo only if the sound is genuinely spatial); trim tight.
      Because the clips are sub-second, uncompressed is still tiny in absolute bytes — hitting the
      *clear, high-quality, low-size* goal without compression artifacts. Audio-asset prep mirrors
      the
      raster-asset discipline in [[downsample-figma-raster-assets]]: get format/size right before
      committing so files stay small.
- **Bundled vs. remote sources:**
    - **SFX are always bundled** with the app — non-negotiable. They need to play instantly with no
      network dependency, and must ship in the correct small/high-quality format.
    - **Music is bundled by default**, but a long track *may* optionally be sourced from a **remote
      URL**
      (e.g. hosted elsewhere and pointed at via RemoteConfig). Treat the remote URL as an **optional
      override, not the default path** — keep an in-app bundled track as the source of truth and
      fallback
      so the app always has music with no network,
      per [[remoteconfig-optional-override-not-required-map]].
      Remote music must degrade gracefully (fall back to the bundled track) on fetch
      failure/offline,
      consistent with [[connectivity-resilience-priority]].
- **KMP architecture:** this is a Kotlin Multiplatform app (Android + iOS), so audio needs an
  `expect`/`actual` (or interface-backed) abstraction in `commonMain` with platform
  implementations —
  e.g. Android `ExoPlayer`/`MediaPlayer` for music + `SoundPool` for SFX; iOS `AVAudioPlayer`/
  `AVPlayer` for music + a low-latency path (`AVAudioPlayer` pool / `AVAudioEngine` /
  `AudioServices`) for SFX. Evaluate whether a KMP audio library covers both before hand-rolling.
- **Design (to refine):**
    - A common-layer audio API: e.g. a music controller (`play(track)`, `stop()`, cross-fade on
      track
      change, loop, respect a global mute/volume) and an SFX player (`play(effect)`, allow overlap).
    - Model tracks/effects as a closed set the app owns (sealed interface or plain enum + exhaustive
      `when`, NOT constructor-arg enums per [[no-enums-with-properties]]) mapping each id to its
      bundled
      asset (and, for music, an optional RemoteConfig URL override).
    - Drive music changes off navigation so entering/leaving a screen triggers the cross-fade; drive
      SFX
      off domain combat events surfaced to the UI (Arena messages already map ~1:1 to events —
      relate to
      SS-068/SS-025). Keep audio triggering in the UI/state layer, not the Domain combat logic
      ([[domain-is-source-of-truth]]).
    - Respect system/lifecycle: pause/duck music on backgrounding, on phone audio focus loss
      (Android `AudioFocus`), and on a user mute setting; clean up players on disposal.
- **Steps:** Choose the KMP playback approach and the two formats (prototype if needed); build the
  `commonMain` audio abstraction + platform `actual`s; define the bundled track/effect catalog; wire
  music to navigation (with cross-fade) and SFX to Arena combat events; add a mute/volume control;
  add the optional RemoteConfig music-URL override with bundled fallback. Source/author the audio
  assets in the correct formats.
- **Notes:** Child tickets carved out of this epic: SS-217 (catalog entries + asset files), SS-218
  (SFX ← Arena combat events), SS-219 (music + cross-fade ← navigation), SS-220 (mute/volume UI +
  audio-focus/ducking), SS-221 (RemoteConfig music-URL override). Related: SS-068 (UI/animation
  overhaul), SS-025 (Arena message events).

### SS-217 — Audio catalog entries + bundled asset files

- [ ] Not completed
- **Goal:** Populate the SS-134 audio foundation with real content. Add `MusicTrack` /
  `SoundEffect` implementations (each a `data object` overriding `assetFileName`) and drop the
  encoded
  files: SFX as 44.1 kHz / 16-bit mono WAV, music as 44.1 kHz stereo ~128–160 kbps VBR AAC in an
  `.m4a` container (gapless-loop metadata intact). Android SFX resolve from `androidMain` assets
  under
  `assets/audio/`; Android music loads via the `asset:///audio/…` URI; iOS resolves both from the
  app
  bundle via `NSBundle`. Prep assets to the correct format/size before committing, per
  [[downsample-figma-raster-assets]] discipline.
- **Related:** SS-134 (foundation).

### SS-218 — Trigger SFX from Arena combat events

- [ ] Not completed
- **Goal:** Fire `SoundEffectPlayer.play(effect = …)` off domain combat events surfaced to the UI
  (e.g. hit landing, armor break, K.O., dice settling). Inject the player via Koin and keep all
  triggering in the UI/state layer, never in Domain combat logic ([[domain-is-source-of-truth]],
  [[message-log-is-passive]]). Arena messages map ~1:1 to events (SS-025/SS-068).
- **Related:** SS-134 (foundation), SS-217 (needs catalog entries + assets).

### SS-219 — Drive background music + cross-fade from navigation

- [ ] Not completed
- **Goal:** Call `MusicPlayer.play(track = …)` / `stop()` from navigation so entering/leaving a
  screen cross-fades the active track. Map each screen/route to its `MusicTrack`. Respect
  lifecycle: pause/duck on backgrounding and clean up on disposal.
- **Related:** SS-134 (foundation), SS-217 (needs catalog entries + assets).

### SS-220 — Mute/volume setting + audio-focus/ducking polish

- [ ] Not completed
- **Goal:** Add a user mute/volume control (settings UI + persistence) wired to
  `MusicPlayer.setMuted` / `SoundEffectPlayer.setMuted`. Add Android `AudioFocus` handling
  (duck/pause on focus loss and backgrounding) and iOS `AVAudioSession` category + interruption
  handling.
- **Related:** SS-134 (foundation).

### SS-221 — Optional RemoteConfig music-URL override with bundled fallback

- [ ] Not completed
- **Goal:** Let a music track optionally source from a remote URL via RemoteConfig, treated as an
  optional override over the bundled `.m4a` source of truth
  ([[remoteconfig-optional-override-not-required-map]]). Degrade gracefully to the bundled track on
  fetch failure/offline ([[connectivity-resilience-priority]]).
- **Related:** SS-134 (foundation), SS-217 (bundled track is the fallback).
