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

## Arena UI

### SS-025 — Improve message-log animation
- [ ] Not completed
- **Source:** `ArenaMessageLogHandle.kt:49` (`// TODO Arena: Improve animation`)
- **Goal:** Improve the arena message-log animation.

### SS-026 — Add isThisPlayerTheUser to PlayerState
- [ ] Not completed
- **Source:** `PlayerState.kt:9` (`// TODO: Add isThisPlayerTheUser ???`)
- **Goal:** Decide whether `PlayerState` needs an `isThisPlayerTheUser` flag; add if it simplifies UI.

### SS-027 — UI for waiting players
- [ ] Not completed
- **Source:** `ArenaWaitingForPlayers.kt:7` (`// TODO Arena: Loading for now, add some UI...`)
- **Goal:** Replace the loading placeholder with engaging UI for players waiting for the match.

### SS-028 — Arena previews & enum-driven preview text
- [ ] Not completed
- **Sources:**
  - `ArenaWaitingForPlayers.kt:14` (`// TODO Arena: Previews`)
  - `ArenaStatePreviewParameterProvider.kt:16` (`// TODO Arena: update when state changes to use enums...`)
- **Goal:** Add previews for waiting screen and update the preview parameter provider to use
  enum/resource-driven text.

### SS-029 — Use StringResources in ArenaCombatControl
- [ ] Not completed
- **Source:** `ArenaCombatControl.kt:164` (`// TODO: Always StringResources`)
- **Goal:** Replace hardcoded strings with string resources.

### SS-031 — Refactor ArenaScreenState mapping
- [ ] Not completed
- **Source:** `ArenaScreenState.kt:79` (`// TODO: Need to refactor this badly`)
- **Goal:** Refactor the flagged state-mapping code. Likely overlaps SS-017.

---

## Common UI

### SS-033 — Replace "slop" + resources in CharacterClassCard
- [ ] Not completed
- **Sources:**
  - `CharacterClassCard.kt:27` (`// TODO: Update Slop`)
  - `CharacterClassCard.kt:85` (`// TODO Arena: resource, update the slop`)
- **Goal:** Clean up placeholder ("slop") content and move text to string resources.

### SS-034 — Update class-selection logic
- [ ] Not completed
- **Source:** `CharacterClassCards.kt:18` (`// TODO Arena: Update class-selection logic`)
- **Goal:** Revisit the class-selection logic (no duplicate classes, selection UX).

---

## App / Main

### SS-036 — Handle signed-in user with missing profile
- [ ] Not completed
- **Source:** `AppViewModel.kt:76` (`// TODO: if user isSignedIn but their profile doesn't exist, then the app will hang`)
- **Goal:** Prevent the app hang when a signed-in user has no profile.
- **Steps:** Detect the missing-profile case; create a profile or route to an error/recovery flow.

### SS-037 — Decide SettingsDrawer content
- [ ] Not completed
- **Source:** `SettingsDrawer.kt:32` (`// TODO: What to do with this?`)
- **Goal:** Determine the SettingsDrawer's purpose/content or remove it.

---

## Data layer

### SS-044 — Connecting messages in ConnectToArena
- [ ] Not completed
- **Source:** `ConnectToArena.kt:51` (`// TODO Arena: Add messages for connecting here?`)
- **Goal:** Decide whether to emit connection messages while joining an arena; add if useful.

### SS-045 — UI when match ends due to early leave
- [ ] Not completed
- **Source:** `ExitArena.kt:8` (`// TODO Arena: When it's finished because someone left early...`)
- **Goal:** Ensure there is clear UI when a match ends because a player left early. Related: SS-071.
- **Concrete case (2-player forfeit):** In a 2-player match, when one player forfeits/leaves, the
  remaining player must be notified that the opponent left and the Arena must transition to
  `Finished` so the remaining player can still see the match outcome and any stats accrued up to that
  point — rather than being stranded in `Combat` or dropped silently. Reported bug. Related: SS-098
  (defer the `Finished` transition until end-of-match messages/animations play), SS-071, SS-022.

---

## Domain layer

### SS-058 — Ability roll values from RemoteConfig
- [ ] Not completed
- **Source:** `HolyShieldHandler.kt:114` (`// TODO(SS-058): From RemoteConfig in some way`,
  `HOLY_SHIELD_SUCCESS_RANGE = 10..12`)
- **Goal:** Drive the per-ability roll thresholds/ranges from RemoteConfig instead of hardcoding,
  across all abilities — e.g. Holy Shield's `10..12` survival range, Duelist Riposte's `6..8` success
  range, Prepare Attack's `10..12` K.O. range, All-In's outcome buckets, Summon Minion's per-face
  results, Encore/Persuade/Mirror Image/Teleport roll-offs, etc.
- **Steps:** Catalog every hardcoded ability roll value; design a config schema for them (likely a
  per-ability stats block parallel to `character_class_stats`); overlay via the existing
  RemoteConfig data source. Defer until the full set of desired values is decided.

### SS-048 — Die-value enum
- [ ] Not completed
- **Source:** `DiceRoller.kt:16` (`// TODO Enums for values 1 through 6?`)
- **Goal:** Consider a type-safe enum for die faces (1–6) instead of raw Ints.

### SS-051 — Review ArenaMessage field (Maybe?)
- [ ] Not completed
- **Source:** `ArenaMessage.kt:18` (`// TODO: Maybe?`)
- **Goal:** Resolve the uncertain message field — keep or remove based on actual usage.

### SS-052 — LuckyCoin message approach
- [ ] Not completed
- **Source:** `ArenaMessage.kt:41` (`// TODO LuckyCoin is this the right approach for messages?`)
- **Goal:** Confirm the messaging approach for the Gambler Lucky Coin ability is correct.
- **Notes:** Related: SS-093 (per-outcome ability message copy — the broader fix this LuckyCoin
  question is an instance of) and SS-105 (Lucky Coin on a 6 retaliation mechanic bug).

### SS-053 — Docstrings for SpellPotencyRoll
- [ ] Not completed
- **Source:** `SpellPotencyRoll.kt:7` (`// TODO: Docstring, etc.`)
- **Goal:** Add docstrings/cleanup to `SpellPotencyRoll`.

---

## Bugs

### SS-091 — TEMPORARY combat dice debug overlay (remove before release)
- [ ] Not completed — *overlay is implemented and live; this ticket stays open as the reminder to
  delete it once combat is stable.*
- **Goal:** While hunting combat bugs, surface every die as soon as it is rolled instead of waiting
  for the throttled message log to reveal it. Show each roll's individual dice *and* their combined
  total directly on the ArenaCombat screen.
- **Why temporary:** This is a throwaway testing aid. It must be trivially removable — all of its
  code lives behind the `SS-091` tag in a single self-contained file plus one call site, and the
  whole thing gets deleted once combat is stable. Do NOT build it into the real UI.
- **Source of truth:** `ArenaScreenState.messages` already holds the full, un-throttled list of
  rolls (each `ArenaMessageState` carries `firstDie`/`secondDie`/`singleDie`/etc). The message log
  only lags because it renders `filteredMessages`; the overlay reads the raw `messages` list.
- **Steps:** Add a `TemporaryDiceDebugOverlay` composable that extracts dice from recent
  dice-bearing messages and renders them (individual dice + sum); show it on the combat screen.
  Remove the file and its call site when done.
- **What was added:** `TemporaryDiceDebugOverlay.kt` (self-contained: dice extraction +
  rendering) shown as a top-aligned overlay in `LoadedArenaScreen` only while
  `arenaState is ArenaState.Combat`. It reads the raw `screenState.messages` list, so dice show the
  instant they are emitted. **To remove:** delete `TemporaryDiceDebugOverlay.kt` and the single
  `TODO(SS-091)` block in `LoadedArenaScreen.kt` (grep `SS-091`).

### SS-094 — Reword the Persuade ability message copy
- [ ] Not completed
- **Source:** Reported bug.
- **Goal:** Replace the current Persuade copy ("Bard begins strumming a lute") with clearer,
  finalized wording for the Bard Persuade outcomes.
- **Notes:** Related: SS-093 (per-outcome ability copy), SS-097 (class-name usage in messages).

### SS-097 — ArenaMessages must use the character class name, never the displayName
- [ ] Not completed
- **Source:** Reported bug.
- **Goal:** All ArenaMessages must refer to players by their character class name (e.g. "Rogue",
  "Bard"), never the user's `displayName`. The **only** exception is the class-selection message
  during `ChoosingClasses` (e.g. "PlayerX chose the Rogue"), which legitimately uses the displayName.
- **Notes:** Audit every ArenaMessage construction site. Related: SS-093, SS-094, SS-100.

### SS-098 — Defer the Finished transition until end-of-match messages/animations play
- [ ] Not completed
- **Source:** Reported bug.
- **Goal:** When a match ends, the Arena must not transition immediately to the `Finished` state.
  The final messages and animations (the knockout that ends the match, etc.) must play out first,
  then the state advances to `Finished`. Today the abrupt transition can cut off the closing
  messages/animations.
- **Notes:** Related: SS-025 (message-log animation), SS-068 (animation pass), SS-045 (early-leave /
  2-player forfeit finish), SS-103 (messages disappearing).

### SS-099 — Disable Impersonate for a class the Illusionist has already copied
- [ ] Not completed
- **Source:** Reported bug.
- **Goal:** When the Illusionist has already impersonated the targeted class's offensive ability, the
  Impersonate button for that class must be disabled (re-copying the same class is a no-op).
- **Notes:** Follows the recent "Wire Illusionist Mirror Image and Impersonate in the UI" work
  (PR #90). Related: SS-093.

### SS-100 — Holy Shield trigger copy should read as damage, not a knockout
- [ ] Not completed
- **Source:** Reported bug.
- **Goal:** When an attack triggers the Paladin's Holy Shield, the message must not read as "someone
  knocked out Paladin" — thematically the Paladin is not knocked out yet (the survival roll is still
  pending). It should instead read like a damage line, e.g. "Rogue damaged Paladin for 11!".
- **Notes:** Related: SS-060 (suppress KnockedOut dialog during the Holy Shield roll), SS-093
  (per-outcome ability copy), SS-097 (class-name usage).

### SS-101 — Auto-target the lone opponent in a 2-player attack-only turn
- [ ] Not completed
- **Source:** Reported bug.
- **Goal:** In a 2-player match, when the acting player can only make a normal attack (their class
  has no activated ability available), the single opponent should be auto-targeted rather than
  requiring a manual target selection.
- **Notes:** Related: SS-095 (target locking), SS-096 (target persistence).

### SS-102 — Track Necromancer minion shields as effective hit points (display + absorption)
- [ ] Not completed
- **Source:** Reported bug.
- **Goal:** Summon Minion users need their skeleton count, golem count, and the resulting shield
  state fully tracked. Skeletons/golems are stored **separately** from the Necromancer's base hit
  points, but they **count as effective hit points** — they add to the Necromancer's effective health
  pool and are consumed before base HP.
  - **Shield values:** each skeleton is a +1 Health shield, each golem is a +2 Health shield (per the
    Summon Minion rules), so the shield total is `skeletons * 1 + golems * 2`.
  - **Display:** the Necromancer's hit points must show the base plus the shield contribution
    distinctly, e.g. `7 + 2` (base 7, with +2 of shields from minions) rather than collapsing to a
    single `9` — so the player can see how much of their health is base vs. minion shield.
  - **Damage absorption:** when the Necromancer takes damage, shields (skeletons/golems) are
    destroyed *first*, absorbing damage, before any damage reaches base hit points. Example: with 2
    skeletons (+2 shield), 4 incoming damage destroys both skeletons and then deals 2 to the
    Necromancer's base HP.
- **Notes:** SS-046 added `maxHitPoints` and noted shields are tracked as separate counts; this
  ticket covers modeling them as effective HP, the absorption ordering, and the `base + shield` UI
  display. Domain + UI. Related: SS-046.

### SS-103 — ArenaMessages permanently disappearing in some flows
- [ ] Not completed
- **Source:** Reported bug. Observed: all messages vanished (never to return) after using All-In and
  attacking the Bard and missing; also seen when a match advances from match 1 to match 2 (not every
  time).
- **Goal:** Identify and fix the conditions under which the ArenaMessage log is wiped and never
  repopulates.
- **Notes:** Investigate the message-cap/append path and the match-reset transition. Related: SS-040
  (message cap / retention), SS-025, SS-098, SS-104 (All-In).

### SS-104 — All-In success must grant the Gambler a second attack
- [ ] Not completed
- **Source:** Reported bug.
- **Goal:** On an All-In success (5 or 6), the Gambler should get to perform two attacks this round
  (provided they survive the first). Currently the extra attack is not granted.
- **Notes:** Domain layer. Related: SS-103 (messages disappearing after All-In), SS-093.

### SS-105 — Lucky Coin on a 6 must give the Gambler a retaliation damage die
- [ ] Not completed
- **Source:** Reported bug.
- **Goal:** When the Gambler's Lucky Coin rolls a 6, it should immediately give the Gambler a single
  die to roll for damage dealt directly to the attacker. This is currently not working correctly.
- **Notes:** Domain layer. Related: SS-052 (LuckyCoin message approach), SS-093.

### SS-106 — Verify SpellPotency emits messages and state updates correctly
- [ ] Not completed
- **Source:** Reported bug (suspected).
- **Goal:** Audit the Mage Spell Potency flow end-to-end and confirm it produces the correct
  ArenaMessages and state updates — full dice-total damage to the target, half to all other
  opponents, then end turn.
- **Notes:** Related: SS-053 (SpellPotencyRoll docstrings), SS-054 (splash-damage attacker case),
  SS-093.

### SS-107 — Add a Doubles ArenaMessage and UI indication
- [ ] Not completed
- **Source:** Reported bug.
- **Goal:** Doubles handling appears implemented (the +1 consecutive-roll bonus, SS-019), but there
  is no ArenaMessage for rolling doubles and no UI indication of the bonus. Ensure a Doubles message
  exists. Deep UI work can wait since the Arena UI is slated to change.
- **Notes:** Related: SS-019 (doublesBonus tracking), SS-068 (UI overhaul), SS-093.

---

## Profile & Auth

### SS-088 — Firebase console: enable Google/Apple auth providers
- [~] Partially done — **Android Google: DONE & verified live.** iOS Google + Apple: pending.
- **Source:** SS-061 follow-up (console + Apple Developer portal; no app code beyond re-downloading
  the Firebase config files). Project: `sorcerers-and-spirits`. Blocks SS-086 (Android Google) and
  SS-087 (iOS Google + Apple).
- **Goal:** Turn on the Google and Apple sign-in providers so the credentials linked by SS-086/SS-087
  are accepted, and get the generated `default_web_client_id` into the app so SS-086 flips on.
- **Status:** Android Google completed — Google enabled in console, debug SHA-1/SHA-256 registered,
  `google-services.json` re-downloaded (now has the type-3 web client), build generates
  `default_web_client_id`, `assembleDebug` clean → Android Google sign-in is live. **Outstanding:**
  release/Play SHA fingerprints (at store-release time), the whole iOS Google sub-section, and the
  whole Apple sub-section.
- **Apple is deferred until App Store release:** Sign in with Apple requires a **paid Apple Developer
  Program membership** ($99/yr), which the owner plans to buy at release time. It is also **mandatory**
  per **App Store Review Guideline 4.8** — once we offer Google on iOS, we MUST offer Sign in with
  Apple — but that's only enforced at App Store review, which coincides with enrollment. So Apple
  (and the iOS Google portal work) stays parked until then; the Apple button is dormant meanwhile and
  nothing breaks. See SS-089.
- **Steps — Android Google (unblocks SS-086) — ✅ DONE:**
  1. Firebase console → **Authentication → Sign-in method → Add new provider → Google → Enable**, set
     a support email, **Save**. Enabling Google auto-creates an OAuth **Web client** ("Web client
     (auto created by Google Service)").
  2. Register the app's signing-cert fingerprints so Credential Manager trusts the build: console →
     **Project settings → Your apps → Android app → Add fingerprint**, add both **SHA-1** and
     **SHA-256** for every keystore that ships a build:
     - Debug: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
     - Release / Play App Signing: copy the SHA-1 + SHA-256 from **Play Console → Release → Setup →
       App signing** (and the upload key too).
  3. **Re-download `google-services.json`** and replace `composeApp/src/google-services.json`. Confirm
     it now contains an `oauth_client` with `client_type: 3` (the web client). The google-services
     Gradle plugin then generates the `default_web_client_id` string resource, which
     `AndroidSocialSignInProvider` reads reflectively — Google sign-in goes live with no code change.
  4. Verify: clean build, tap **Continue with Google** on a device/emulator with Play Services + a
     Google account; the account picker should appear and linking should succeed.
- **Steps — Apple (unblocks SS-087's Apple path):**
  1. Apple Developer portal → create a **Services ID**, enable **Sign in with Apple**, and configure
     the return URL to the Firebase handler (`https://sorcerers-and-spirits.firebaseapp.com/__/auth/handler`).
  2. Create a **Sign in with Apple key** (.p8) and note the Key ID + Team ID.
  3. Firebase console → **Authentication → Sign-in method → Apple → Enable**, enter the Services ID,
     Apple Team ID, Key ID, and the .p8 private key, **Save**.
  4. Add the **Sign in with Apple** capability to the iOS app target in Xcode (SS-087).
- **Steps — iOS Google (unblocks SS-087's Google path):** the iOS Firebase config carries a
  `REVERSED_CLIENT_ID`; re-download `GoogleService-Info.plist` after enabling Google above and add
  the `REVERSED_CLIENT_ID` as a URL scheme in the iOS target (SS-087 already added the GoogleSignIn
  SPM package wiring in code).
- **Note:** Anonymous auth is already enabled and unaffected; existing anonymous accounts upgrade in
  place via `linkWithCredential` (no user migration).

### SS-089 — Enable Sign in with Apple before App Store release
- [ ] Not completed — **blocking for iOS App Store release; parked until Apple Developer enrollment.**
- **Source:** SS-061 / SS-087 follow-up. The Apple *code* is done (SS-087); this ticket tracks the
  account-gated config that can only happen with a paid membership.
- **Why mandatory:** **App Store Review Guideline 4.8** — offering Google sign-in on iOS requires
  also offering Sign in with Apple. Enforced at App Store review.
- **Blocked by:** a **paid Apple Developer Program membership** ($99/yr), planned for release time.
  Until then the iOS Apple button is dormant and the app still works.
- **Steps (do together once enrolled):**
  1. Apple Developer portal: Services ID + Sign in with Apple key (.p8) — see SS-088 "Steps — Apple".
  2. Firebase console: enable the Apple provider with those values — see SS-088 "Steps — Apple".
  3. Xcode: add the **Sign in with Apple** capability to the `iosApp` target — see SS-087.
  4. Verify on device/simulator: the Apple button presents the sheet and linking succeeds.

### SS-109 — ProfileScreen name-change success/failure feedback
- [ ] Not completed
- **Goal:** After a name change on the ProfileScreen, show a confirmation message (likely a
  snackbar) reporting whether the change succeeded or failed.
- **Steps:** Surface the result of the name-change operation to the UI; show a success snackbar on
  success and an error message on failure.

---

## Game Stats & Leaderboard

Game statistics are recorded on each user's Firestore profile and will be a large part of the game —
directly driving the Leaderboard and other features. The work is staged deliberately: **first** build
the generic infrastructure for persisting stats to a profile (SS-021), keeping it stat-agnostic;
**then** decide which concrete stats to record (SS-072) and wire them into match outcomes (SS-071);
and finally surface them (SS-064 ProfileScreen, SS-065 Leaderboard).

### SS-074 — Achievements
- [ ] Not completed
- **Goal:** Award players achievements derived from their recorded stats and match events. Examples:
  win a Match with every class; win a Gauntlet with every class; win a Match/Gauntlet as the
  Illusionist after Impersonating every class. The set will keep growing, so the priority is an
  architecture where a new achievement can be added in one place — not enumerating every one now.
- **Notes:** Builds on the SS-072 stats schema. Most class-based achievements are derivable from the
  per-class `GameStats` already persisted (e.g. `CharacterClassType.entries.all { perClass[it]?...}`),
  so the likely shape is a list of achievement definitions (predicate over `ProfileStats` + any extra
  event state) plus an `achievements` field on `Profile`/`ProfileEntity` recording unlock state.
  Illusionist-impersonation achievements need extra per-impersonated-class tracking beyond today's
  counters. Related: SS-072, SS-021, SS-065, SS-067.

### SS-075 — Flush stat outcomes incrementally (shrink the loss window)
- [ ] Not completed
- **Source:** `ArenaSessionStats`/`ArenaScreenViewModel` (SS-071) accumulate the whole session in
  memory and only flush on a leave path.
- **Goal:** Flush each match's outcome to Firestore the moment it finishes (and each `timesSelected`
  as it happens) instead of holding the full session delta until the user leaves, so a process
  death loses at most the current in-progress match rather than every match played this session.
- **Notes:** Firestore offline persistence is on by default (`single { Firebase.firestore }`) and
  `incrementStats` uses commutative `FieldValue.increment`, so an *issued* write already survives
  network loss and app restarts — the gap SS-071 leaves is only the in-memory delta that was never
  handed to Firestore. Per-match flush leans on that existing durability. The `consumeDelta()`/reset
  pattern already lets a per-match flush and the on-leave flush compose without double-counting; the
  forfeit determination still happens on leave for the one match still in progress. Likely makes
  SS-076 unnecessary for most cases. Related: SS-071, SS-021.

### SS-076 — Durable local pending-stats store for crash resilience
- [ ] Not completed
- **Goal:** Persist the unflushed stat delta locally (Room or DataStore) and replay it on next
  launch, so a crash or kill mid-match cannot lose outcomes — an at-least-once guarantee for stat
  writes that does not depend on Firestore's in-process write queue.
- **Notes:** Scope is specifically the window Firestore offline persistence does *not* cover: a delta
  computed in memory but never issued as a write before the process dies. Depends on / may be reduced
  to a thin remaining case by SS-075 (which removes the multi-match in-memory window using Firestore's
  own durable queue). Decide Room vs DataStore based on whether other features want an offline-first
  local store; if stats are the only consumer, a small DataStore-backed pending-delta may be enough.
  Replay must stay idempotent under `FieldValue.increment` (clear the local record only after the
  write is acknowledged). Related: SS-075, SS-071, SS-021.

---

## Rules & Help

### SS-066 — Explore an in-app game-rules chatbot
- [ ] Not completed
- **Goal:** Explore a chatbot that answers questions about the game rules — including for players
  using the app alongside a physical game to settle rules edge-cases. The rules in CLAUDE.md / the
  GameRulesScreen content (SS-067) form the knowledge base.
- **Steps:** Decide the backend (default to the Claude API with a current Claude model, grounded on
  the rules text); scope question types (rules lookups vs. ability-interaction edge cases); design the
  entry point (likely from the GameRulesScreen or drawer). Investigation only for now.

### SS-081 — Class ability/stat copy in RemoteConfig (long + short variants)
- [ ] Not completed
- **Source:** SS-079 hardcoded the stat/ability descriptions as string literals in
  `CharacterClassWheel.kt` (`offensiveAbilityDescription`/`defensiveAbilityDescription` +
  `HEALTH/ARMOR/SPEED_DESCRIPTION`) to ship the expandable stat card.
- **Goal:** Move all class ability + stat copy out of code and into RemoteConfig, with two registers:
  a **full** description (for the GameRulesScreen, SS-067) and a **short** one (for space-constrained
  in-UI surfaces like the wheel's stat card and combat). Lets us tune copy without a release and keeps
  the tight surfaces from overflowing.
- **Steps:** Define the RemoteConfig schema (per ability/stat: long + short text); thread through the
  existing `CharacterClassProvider`/`GetCharacterClassesUseCase` (or a parallel copy provider) the way
  stats are in SS-047; replace the literals in `CharacterClassWheel` with the short variant; have
  GameRulesScreen consume the long variant; pick a static fallback for offline/first-run.
- **Notes:** Overlaps SS-033 (move CharacterClassCard text to resources) and SS-047 (RemoteConfig
  class stats). Related: SS-067, SS-079, SS-033, SS-047.

---

## UI overhaul & input

### SS-068 — Lottie animation pass & UI overhaul (Arena + Tavern)
- [ ] Not completed
- **Source:** Existing local branch with Lottie animation work. Related: combat-animation foundation.
- **Goal:** Track the broader UI overhaul. Primary focus is the ArenaScreen, with knock-on effects on
  the TavernScreen. The CharacterClassCards pager becomes a spinning-wheel selector (a pager may still
  back it). Arena combat animations should map roughly 1-to-1 to `ArenaMessage`s.
- **Notes:** Large; expected to spawn many child tickets. Likely overlaps SS-025 (message-log
  animation), SS-033/SS-034 (CharacterClassCard/selection), and the existing Compottie foundation.
  Captured now to anchor the work; break out concrete tickets as the overhaul begins.

### SS-077 — Decide dice rendering & animation tech
- [ ] Not completed
- **Goal:** Choose how the dice are drawn and animated for the shake-to-roll interaction (SS-069):
  Lottie/Compottie files, Compose Canvas-drawn dice, or another approach. This is a tech-selection
  ticket only — it precedes finalizing the on-screen UI (SS-078).
- **Considerations:** Need to show 1 or 2 dice depending on the combat step; convey press/shake/
  release states and a settling-to-face animation; land on the rolled value the ViewModel returns
  (so the animation must be able to resolve to a specific face). KMP-friendly (Android + iOS).
  Compottie is already a foundation dependency in this project (combat-animation work) — weigh
  reusing it vs. Canvas for crisp, value-driven die faces.
- **Steps:** Prototype the leading option(s); decide; record the choice and rationale here.
- **Notes:** Blocks SS-078. Related: SS-069, SS-078, SS-068 (broader animation pass).

### SS-078 — Finalize dice UI (placement, size, color)
- [ ] Not completed
- **Goal:** Once the rendering tech is chosen (SS-077), finalize the dice presentation: where the
  dice sit on the ArenaScreen, their size, colors/theming, and resting vs. active layout.
- **Steps:** Decide on-screen location relative to the combat controls; pick size/scale across form
  factors; choose colors/theme alignment; define resting state vs. held/shaking state appearance.
- **Notes:** Depends on SS-077. Related: SS-069, SS-077, SS-068.

### SS-079 — Spinning-wheel class selector (replace CharacterClassCards)
- [ ] In progress — interaction + structure built; Figma visual fidelity remains.
- **Progress:** `CharacterClassWheel.kt` built and wired into TavernScreen + ArenaChoosingClasses
  (replacing `CharacterClassCards` at both call sites). Single `Animatable`/float rotation as the
  source of truth; **angular drag** (rotate by the angle the finger sweeps around the centre, so the
  grabbed point tracks the thumb at any clock position); snap-to-nearest spoke on release; each
  avatar is `clickable` to spin-and-select that class. Per-class spoke colors, labels rotated inline
  with their spoke, downsampled avatar/icon assets (see [[downsample-figma-raster-assets]]).
  Per-frame recomposition avoided via `derivedStateOf`. Bottom stats are an expandable `ElevatedCard`
  with `animateIntAsState` value animations and ability/stat descriptions. Removed the top selection
  pointer. Also deleted the now-dead `CharacterClassCards.kt` pager (the singular `CharacterClassCard`
  stays — still used by ArenaCombat). Remaining is split out: visual fidelity → SS-084, responsive
  scaling → SS-080, copy source → SS-081.
- **Source:** Figma `Android Medium - 1`
  (https://www.figma.com/design/hCcLwQRgRvNFMrzGdMiRlf/appSorcerers-spirits?node-id=1-2). Child of
  SS-068; supersedes the current `HorizontalPager`-based `CharacterClassCards`/`CharacterClassCard`.
- **Goal:** Replace the card pager with a circular, rotatable wheel selector. Eight class spokes
  radiate from a center hub at 45° increments, each with its class name aligned to the spoke and its
  character art ringing the outside. The user spins the wheel to bring a class to a fixed selection
  pointer; the selected class's stats render in the bottom stat bar.
- **Design notes:**
  - 8 classes (matches `CharacterClass.getAllClasses()`): Rogue, Gambler, Duelist, Necromancer,
    Bard, Illusionist, Mage, Paladin. Each spoke is a rounded "capsule" (~173dp long, 27dp thick)
    with a per-class color (e.g. Rogue red `#9D2920`→`#581310`, Gambler gold `#DAA73B`,
    Illusionist blue `#1F3A5D`, Bard/Necromancer greens `#3C461E`/`#49533B`, Mage `#243346`).
  - Selected spoke is visually highlighted (Figma shows `#852B1A`); names rotate to follow their
    spoke angle.
  - Bottom stat bar shows the selected class's stats with icons: Health (heart), Armor
    (CornerArmor), Speed, and SPC/ability markers — sourced from `CharacterClassState`.
  - Top bar is a themed header band.
- **Steps:** Build a rotatable wheel composable (drag-to-spin + snap-to-nearest, with a fixed
  pointer); drive selection off rotation angle → class index; render per-class spoke colors and
  ringed avatars from `characterClasses`; render the selected class's stat bar; thread the existing
  `selectedClass` / `unavailableClasses` / `classSelections` / `onClassSelected` contract through so
  the Arena/Tavern selection flow is unchanged; obtain/slice the avatar + stat-icon assets
  (download from Figma); add a `@Preview`.
- **Notes:** Likely lets us retire SS-033 (CharacterClassCard "slop"/resources) and fold SS-034
  (selection logic) into the wheel. Keep the no-duplicate-class rule (`unavailableClasses`). A pager
  may still back the wheel internally per SS-068. Related: SS-068, SS-033, SS-034.

### SS-084 — Spinning-wheel visual fidelity (match Figma)
- [ ] Not completed
- **Source:** Child of SS-079. The wheel's interaction + structure shipped under SS-079; this ticket
  is the look. Figma will keep iterating on icons/colors, so this is intentionally split out.
- **Goal:** Bring `CharacterClassWheel` to Figma fidelity:
  - Themed top header band and bottom stat band (the radial-gradient bars from the Figma frame).
  - Background image behind the wheel.
  - Spoke geometry/proportions — length, thickness, corner radius, gap from hub — tuned to the Figma
    `SPOKE_*` / `AVATAR_*` / `HUB_*` fractions instead of the current eyeballed values.
  - Final per-class spoke colors and the final avatar + stat icons (`ic_health`/`ic_armor`/`ic_speed`/
    `ic_spc`) once delivered from Figma (placeholders downsampled today — see
    [[downsample-figma-raster-assets]]).
  - Selected-spoke highlight treatment and hub styling; label typography/placement polish.
- **Steps:** Pull updated Figma assets/colors when ready; replace placeholder drawables; tune the
  layout fraction constants; add the header/stat bands and background; revisit the selected-state
  highlight.
- **Notes:** Gated on Figma updates. Coordinate with SS-080 (responsive) and SS-081 (copy). Related:
  SS-079, SS-068.

### SS-110 — Screen-transition (navigation) animations
- [ ] Not completed
- **Goal:** Improve the enter/exit animations when navigating between screens. The current default
  is most noticeably rough on back navigation.
- **Options to evaluate:** Navigation Compose's per-destination `enterTransition` / `exitTransition`
  / `popEnterTransition` / `popExitTransition` (slide/fade/scale via `AnimatedContentTransitionScope`);
  shared-element / shared-bounds transitions for continuity; predictive-back gesture support on
  Android. Pick a consistent transition language and apply it across the nav graph.
- **Steps:** Survey the current nav setup; decide the transition style per screen pair (especially
  forward vs. pop/back); implement and tune durations/easing; verify back/predictive-back feels
  right. Related: SS-068.

---

## Responsive & form factors

### SS-080 — Responsive scaling for small/medium phones
- [ ] Not completed
- **Source:** Observed on the spinning-wheel selector (SS-079): on a Small Phone the bottom stats
  card is cut off; on a Medium phone the *expanded* stats card overflows (description copy clipped).
- **Goal:** Make the Tavern/Arena class-selection layout (wheel + stats card) fit within the smallest
  supported phone, collapsed and expanded, without clipping.
- **Steps:** Audit the class-selection screens across the small/medium phone reference sizes; make
  the wheel size and the stats card height adapt (e.g. cap/scale the wheel by available height, let
  the expanded card scroll or size to content within a constrained viewport); verify on Small +
  Medium previews/emulators. Short copy (SS-081) will also reduce overflow.
- **Notes:** Distinct from tablet (SS-083) and landscape (SS-082). Related: SS-079, SS-081.

### SS-082 — Decide on landscape support
- [ ] Not completed
- **Goal:** Make a product + technical decision on whether the app supports landscape orientation,
  and to what extent (all screens, or lock to portrait). The wheel selector and Arena combat layouts
  assume portrait today.
- **Steps:** Decide scope (lock portrait vs. full landscape vs. per-screen); if supported, define how
  the wheel + stats card and ArenaScreen reflow; otherwise set the orientation lock and remove the
  ambiguity. Record the decision here.
- **Notes:** Interacts with SS-080 (responsive) and SS-083 (tablet, where landscape is more expected).

### SS-083 — Tablet support
- [ ] Not completed
- **Goal:** Define and implement how the app presents on tablets / large screens (the layouts are
  phone-first today).
- **Steps:** Decide the target (basic "scaled-up phone" vs. adaptive multi-pane layouts); set max
  content widths / adaptive breakpoints; review the wheel selector, Tavern, Arena, and profile/stats
  screens at expanded window sizes. The `~/.android/skills/jetpack-compose/adaptive` skill is the
  reference for adaptive layouts.
- **Notes:** Likely depends on SS-082 (landscape) since tablets are commonly used landscape. Related:
  SS-080, SS-082.

---

## Computer opponent

### SS-092 — Computer enemy: challenge from the Tavern, fight in the Arena
- [ ] Not completed
- **Goal:** Let a player challenge a computer-controlled opponent from The Tavern and fight it in the
  Arena. Start with a single possible computer enemy; design so a human can later set up multiple
  computer opponents. The computer must take a short "thinking" delay before each decision —
  reacting, choosing who to attack, or rolling an ability.
- **Design:**
  - **Domain `ComputerOpponent`** (`domain/computer/`): a `ComputerOpponent(userId, displayName)`
    plus a `ComputerOpponents` registry holding the single `default` bot and an `isComputer(userId)`
    predicate keyed off a `"computer-"` id prefix (so multiple bots and human/bot detection extend
    cleanly).
  - **Tavern (bot as a traveler):** the computer shows up as a permanent, always-`Available`
    traveler in `TheBar` (injected in the UI `TavernData.toState`, not Firebase presence). Selecting
    it and pressing Challenge does **not** run the human Firebase challenge handshake (the bot has no
    presence to claim/mailbox, so `claimChallengers` would reject it). Instead
    `TavernScreenViewModel` short-circuits to a direct computer-match start: it builds
    `ArenaScreenViewModelArguments` with a freshly generated `arenaId`, `expectedNumberOfPlayers`,
    and the selected `computerOpponents`, and fires `TavernEvent.NavigateToArena`. MVP scope: a
    selection containing the computer starts a user-vs-computer(s) match (human co-challengees in the
    same match are deferred).
  - **Seeding the match:** `ArenaScreenViewModelArguments` / its navigation-serializable mirror carry
    the `computerOpponents`. `CoordinateArenaMatchUseCase` connects each computer into the arena
    (reusing the idempotent `connectToArena`) **before** connecting the user, so the user connects
    last and becomes the first active chooser.
  - **Driving the bot (the human's client):** in a user-vs-computer match only the human's client is
    present, so it drives the bot. After each match-state emission `ArenaScreenViewModel` checks
    whether a computer currently owes the action (it is the active class-chooser, or its id is in the
    combat `activeUserIds` for a turn or an owed reaction roll). If so it schedules the action after a
    randomized "thinking" delay, keyed on an action signature so unrelated emissions (e.g. incoming
    messages) don't re-trigger or double-fire. The decision engine reuses the existing SS-014 timeout
    auto-resolve, generalized from "the user" to an arbitrary acting player id: pick a random
    available class; attack a random living opponent; auto-roll any owed ability/reaction (random
    legal Charm target). Session stats stay attributed to the human only.
  - **Cleanup / lifecycle:** `ExitArena` deletes the whole match when no **human** players remain
    (a computer-only match is dead), preventing an orphaned match the bot can never leave.
    `StayForAnotherMatchUseCase` gains an explicit player-id param so the human's client also readies
    up the computer(s) when staying, letting a computer match restart instead of hanging on the
    never-ready bot.
  - **Lightly-smart combat:** the bot's free turn-start choice (`CombatActionState.Idle`) is biased
    rather than random — it attacks the **weakest (lowest-HP) living opponent** (ties broken at
    random) to secure kills, and activates an offensive ability only when **clearly favorable**
    (currently just the Necromancer's Summon Minion while below max HP or with no minions; the risky
    gambits — All-In self-knockout, Prepare Attack forgoing a hit, Impersonate — fall back to a
    normal attack). Owed reaction/continuation rolls remain forced. The user's own timed-out turn
    keeps the simple random-target behavior; only the computer uses the biased policy, injected as a
    turn-action strategy into the shared resolver.
- **Architecture note:** the bot is **client-driven** (the challenging human's client owns and drives
  it), because this game has no game-logic backend — Firebase RTDB only stores shared state; all
  rules run in Kotlin `commonMain` on clients. A true backend-driven bot (the authoritative-server
  model most games use) would mean a Cloud Function reimplementing/sharing the combat rules, which is
  only worth it once *all* combat resolution moves server-side (whole-game anti-cheat) or bots must
  persist with zero humans connected. The decision *policy* is portable, so it can move to a backend
  driver later with minimal rewiring. Client-driving also gives precise control over the "thinking"
  delay (vs. Cloud Function cold-start latency).
- **Deferred (follow-ups):** richer combat heuristics (use Prepare Attack vs. tanky targets, All-In
  when behind, Impersonate the strongest opponent); mixed human+computer matches in one arena
  (needs a deterministic "which client drives the bot" rule with failover); per-bot
  difficulty/personality; Tavern UI to configure multiple computer opponents.
