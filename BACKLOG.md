# Backlog

A tracked list of work items ("tickets") for Sorcerers and Spirits.

## How this works

- Each ticket has a stable ID (`SS-001`, `SS-002`, ...).
- TODO comments in the codebase are tagged with their ticket ID, e.g. `// TODO(SS-019): ...`,
  so any TODO can be traced back to its ticket here. Search the codebase for the ID to find every
  related TODO.
- A ticket may cover more than one TODO when they describe the same underlying work.
- Not every ticket comes from a TODO — add new ones freely; just give them the next free ID.
- Check the box (`[x]`) when a ticket is complete.

## Status legend

- `[ ]` not completed
- `[x]` completed

---

## Tests

### SS-001 — Fix Tavern repository tests
- [x] Completed
- **Source:** `FirebaseTavernRepositoryTest.kt:8` (`// TODO Tavern: Fix all these tests`)
- **Resolution:** Introduced a `TavernDataSource` seam (`FirebaseTavernDataSource` wraps gitlive
  Firebase; `FirebaseTavernRepository` now holds the orchestration logic over it) so the logic is
  unit-testable without mocking gitlive's inline `value()`/`updateChildren()`/`setValue()`. Replaced
  the empty/obsolete stub suite with use-case-level tests backed by a `FakeTavernDataSource`.
- **Goal:** Restore the Tavern repository test suite to a passing, meaningful state.
- **Steps:**
  - Run the suite and catalog what currently fails or is disabled.
  - Update tests to match the current `FirebaseTavernRepository` API/behavior.
  - Remove tests that are obsolete; add coverage for any gaps surfaced.
  - Use Kotlin asserts, and Turbine as necessary

### SS-002 — Repair broken ArenaScreenViewModel test
- [x] Completed
- **Source:** `ArenaScreenViewModelTest.kt:342` (`// TODO: All fucked`)
- **Goal:** Fix the broken/abandoned test around line 342 so it asserts correct behavior.
- **Resolution:** The abandoned test had already been superseded by a comprehensive, passing suite;
  the broken scenario (mid-attack pending rolls) is now covered by the tests immediately following
  the marker (pending health roll, Teleport roll-off, opponent-owned ability roll). Replaced the
  stale `// TODO(SS-002): All fucked` divider with a real section header and verified the whole
  `composeApp` debug unit-test suite is green. Reviewed every assertion against the live
  `ArenaScreenViewModel` and `ArenaScreenState.toUpdatedState` mapping plus the CLAUDE.md ability
  rules — they test real derivation, not tautologies, so no further changes were needed.

### SS-003 — Clarify Charm test comment & assertion
- [x] Completed
- **Source:** `PerformCombatActionUseCaseTest.kt:928` (`// TODO: I don't understand this comment...`)
- **Goal:** Resolve the confusing comment about the Charmed target and verify the assertion matches
  the intended Persuade/Charm rule from CLAUDE.md.
- **Resolution:** The misleading "The Bard's chosen target redirects the attack" comment had the
  direction backwards. Per the Bard Persuade rule, on a 6 the Bard charms the **attacker** into
  attacking the Bard's chosen target. Replaced the comment in both the hit/miss Charm tests with
  "Persuade charms the attacker into attacking the Bard's chosen target instead", matching the
  `charmedAttackerUserId`/`newTargetUserId` naming. Verified the existing assertions already reflect
  the rule (fresh ArmorClass roll against the new target; miss ends the turn) — the suite is green.

---

## Tavern UI

### SS-004 — Decide source of class list in TavernState
- [x] Completed
- **Source:** `TavernState.kt:21` (`// TODO: any reason to get from config?`)
- **Goal:** Decide whether the class list should come from RemoteConfig or stay static
  (`CharacterClass.getAllClasses()`). Related: SS-047.
- **Decision:** Get from config. SS-047 made the RemoteConfig-driven `CharacterClassProvider`
  (via `GetCharacterClassesUseCase`) the single source of class stats and routed the Arena's
  class-selection list through it. The Tavern is where players browse classes before challenging
  into a match, so it must show the same config-driven stats they'll actually get in the Arena —
  otherwise the two screens can disagree.
- **Resolution:** Injected `GetCharacterClassesUseCase` into `TavernScreenViewModel` and threaded
  the resulting `characterClasses` through `TavernData.toState(...)`, mirroring
  `ArenaScreenViewModel`. The static `CharacterClass.getAllClasses()` literal remains only as the
  `TavernState.characterClasses` construction/preview default (same as `ArenaScreenState`); the
  live ViewModel path now always supplies the config-driven list. `composeApp` compiles clean.

### SS-005 — Move navigateToArena out of TavernState
- [x] Completed
- **Source:** `TavernState.kt:39` (`// TODO: Move this out of state?`)
- **Goal:** Reconsider whether `navigateToArena` belongs in state vs. an event/effect channel.
- **Decision:** Move it out. `navigateToArena` was a transient one-shot navigation signal embedded
  in the persistent `ChallengeState`/`TavernState` but never rendered — the ViewModel only read it
  once to fire `TavernEvent.NavigateToArena`. That's navigation-as-state; it belongs on the existing
  event channel.
- **Resolution:** Introduced a `ChallengeUpdate` result type (`Ongoing(challengeState)` vs.
  `ReadyToStart(arguments)`) returned by `ChallengeHandler` (renamed `handleChallengeUpdateAndGetState`
  → `handleChallengeUpdate`). The handler now builds the `ArenaScreenViewModelArguments` directly in
  the ready-to-start branch (via a private `Challenge.toArenaArguments()`), and the ViewModel's
  `when` on the result either sends `TavernEvent.NavigateToArena` or loads the screen state. Removed
  `navigateToArena` and the now-unused `getExpectedNumberOfPlayers()` from `ChallengeState`, and the
  `isReadyToStart` param from `Challenge.toChallengeState`. Updated `ChallengeHandlerTest` to the new
  return type. `composeApp` compiles clean; Tavern + ChallengeHandler suites are green.

### SS-006 — Consolidate navigate-to-arena callback at App level
- [x] Completed
- **Source:** `TavernScreen.kt:66` (`// TODO: make one for App?`)
- **Goal:** Decide whether the `onNavigateToArena` wiring should live at the App level instead of
  per-screen.
- **Decision:** Keep as-is. The navigation wiring is already consolidated at the App level:
  `NavigationDisplay` (the navigation host that owns the route→screen mapping and the `backStack`) is
  where the Tavern→Arena navigation lambda (`backStack.add(Route.ArenaScreen(...))`) lives — not
  inside `TavernScreen`. `TavernScreen` stays navigation-agnostic and only forwards a
  `TavernEvent.NavigateToArena` intent through its `onNavigateToArena` callback, which is the
  idiomatic state-hoisting seam rather than duplication to eliminate. Hoisting the lambda body
  further up into `SorcerersAndSpirits` would only move it away from the `backStack` it operates on.
  (`SettingsDrawer.onNavigateToArena` is a separate commented-out no-op stub being reconsidered under
  SS-037/SS-063, unrelated to the challenge-driven navigation.)
- **Resolution:** Removed the `// TODO(SS-006): make one for App?` comment from `TavernScreen.kt`; no
  code change to the wiring was warranted.

### SS-007 — Real error UI for error states
- [x] Completed
- **Sources:**
  - `TavernScreen.kt:77` (`// TODO: Update UI to show error`)
  - `ArenaScreen.kt:62` (`// TODO Arena: Update UI to show error`)
  - `SorcerersAndSpiritsNavigationHost.kt:36` (`// TODO: Update UI to show error`)
  - `SorcerersAndSpiritsNavigationHost.kt:49` (`// TODO: what to do here, show another dialog?...`)
- **Goal:** Replace placeholder retry/empty handling with proper error UI shared across screens.
- **Resolution:**
  - **Reusable component:** Replaced the misused `RetryButton` (each `UiState.Error` branch was
    calling `RetryButton(retryMessage = "Retry", onRetry = {})` — passing the button label as the
    *message* with a no-op retry) with a single shared `ErrorContent(onRetry, message)` in
    `ui/common/` (renamed `Retry.kt` → `ErrorContent.kt`). It shows a generic error message
    (`error_generic_message` string resource) plus the existing `retry_button`. Wired all three
    branches — Tavern, Arena, and the App navigation host — to it.
  - **Made `UiState.Error` actually reachable + retryable:** previously no ViewModel ever emitted
    `Error`, and the data flows had no failure handling — a thrown Firebase flow silently stuck the
    screen on `Loading` forever. Added `.catch { _uiStateFlow.value = UiState.Error }` to the
    collected flows in `TavernScreenViewModel`, `ArenaScreenViewModel`, and `AppViewModel`, and a
    `retry()` on each that resets to `Loading` and restarts the load (cancelling the prior job via a
    tracked `Job`; Arena reused its existing `screenStateFlowJob`). `AppViewModel`'s `.catch` sits on
    the `combine(profile, isOnline)` flow, which emits offline as a *value* (handled by the existing
    offline-shell branch), so only genuine throws map to `Error` — the deliberate offline-first UX is
    preserved (`AppViewModelTest` still green).
  - **Navigation-host sign-in dismiss UX:** decided the `SignInNameDialog` is intentionally
    non-dismissible (a profile is required before the app is usable), so `onDismissRequest` stays a
    documented no-op rather than a dialog/snackbar/exit. Replaced the open-question TODO with a
    comment stating the decision.
- **Verification:** `composeApp` Android debug compiles clean; `ArenaScreenViewModelTest` (45) and
  `AppViewModelTest` (7) pass.

### SS-008 — Reusable Tavern preview state & previews
- [ ] Not completed
- **Sources:**
  - `ExampleTravelers.kt:6` (`// TODO: Make example state that can be reused for previews`)
  - `TavernScreen.kt:199` (`// TODO: Preview`)
  - `ChallengeDialog.kt:169` (`// TODO: Update preview data`)
- **Goal:** Build reusable example/preview state for Tavern composables and add/refresh previews.
- **Steps:** Create shared sample data in `ExampleTravelers`; add a `TavernScreen` preview; update
  `ChallengeDialog` preview data.

### SS-009 — Improve TheBar
- [ ] Not completed
- **Source:** `TheBar.kt:42` (`// TODO: Improve`)
- **Goal:** Clarify and improve `TheBar` (scope is vague — define concretely while working on it).
- **Steps:** Inspect the composable; identify the concrete improvement intended; implement.

### SS-010 — Reword challenge-list UI copy in TheBar
- [ ] Not completed
- **Source:** `TheBar.kt:83` (`// TODO: Change UI - should say other players to challenge...`)
- **Goal:** Update copy to clearly communicate "other players to challenge."
- **Steps:** Replace text with a string resource; verify wording.

### SS-011 — New challenge icon in TheBar
- [ ] Not completed
- **Source:** `TheBar.kt:92` (`// TODO: new icon`)
- **Goal:** Replace the placeholder icon with the intended challenge icon.

### SS-012 — Update TheBar styling
- [ ] Not completed
- **Source:** `TheBar.kt:158` (`// TODO: Update styling`)
- **Goal:** Finalize styling for `TheBar`.

---

## Arena UI

### SS-013 — Review ArenaEvent (empty TODO)
- [ ] Not completed
- **Source:** `ArenaEvent.kt:3` (`// TODO Arena:`)
- **Goal:** Empty placeholder TODO — determine intent (likely additional events) or remove.

### SS-014 — Handle match/timer expiration per state
- [ ] Not completed
- **Sources:**
  - `ArenaScreen.kt:78` (`/* TODO */` in `ArenaEvent.MatchTimedOut` handling)
  - `ArenaScreenViewModel.kt:292` (timer expiration in `ChoosingClasses`)
  - `ArenaScreenViewModel.kt:296` (timer expiration in `Combat`)
  - `ArenaScreenViewModel.kt:300` (timer expiration in `Finished`)
- **Goal:** Define and implement what happens when the timer expires in each `ArenaState`, and how
  the `MatchTimedOut` event is surfaced to the user.
- **Steps:** Decide per-state behavior (auto-pick class, skip/auto-resolve turn, finish handling);
  implement; surface `MatchTimedOut` in the UI.

### SS-015 — Delete unused ArenaActions
- [x] Completed
- **Source:** `ArenaActions.kt:31` (`// TODO: Delete`)
- **Goal:** Remove the dead code flagged for deletion (verify no usages first).
- **Resolution:** `RollDiceArguments` and its entire `onRollDice` chain were dead — the ViewModel
  handler was an empty no-op (`fun onRollDice(...) {}`) and nothing ever invoked it. Deleted the
  `RollDiceArguments` data class, the `onRollDice` field in `ArenaActions`, its wiring in
  `ArenaScreen`, and the no-op ViewModel function. `composeApp` compiles clean (no remaining
  references).

### SS-016 — Rename/refactor ArenaActions
- [ ] Not completed
- **Source:** `ArenaActions.kt:38` (`// TODO: Rename/refactor`)
- **Goal:** Rename/refactor the flagged ArenaActions construct for clarity.

### SS-017 — Refactor ArenaScreenViewModel
- [ ] Not completed
- **Source:** `ArenaScreenViewModel.kt:40` (`// TODO: This is all fucked`)
- **Goal:** Address the broad "this is all fucked" flag — clean up the ViewModel section it marks.
- **Steps:** Read the surrounding block; identify the specific problem; refactor with tests.

### SS-018 — Better userId resolution
- [ ] Not completed
- **Source:** `ArenaScreenViewModel.kt:73` (`// TODO: Figure out better way for userId`)
- **Goal:** Establish a cleaner way to obtain the current user's id.

### SS-019 — Track doublesBonus in ArenaState.Combat
- [x] Completed
- **Sources:**
  - `ArenaScreenViewModel.kt:150` (in `onAttack`)
  - `ArenaScreenViewModel.kt:180` (in `onUseAbility`)
- **Goal:** Track and pass the real `doublesBonus` (currently hardcoded `null`) per the doubles
  +1 consecutive-roll rule in CLAUDE.md.
- **Resolution:** Added a `doublesBonus: DoublesBonus?` field to the UI `ArenaState.Combat` and
  populated it in `ArenaMatch.toUpdatedState` straight from the domain `ArenaMatch.Combat.doublesBonus`
  (which the data layer already persists/restores via `ArenaMatchEntity`). Replaced the hardcoded
  `doublesBonus = null` in both `onAttack` and `onUseAbility` with `arenaState.doublesBonus`, so the
  persisted bonus now flows back into `CombatActionArguments` → `RealDiceRoller(initialBonus = ...)`.
  The consecutive-roll rule itself is still enforced in the domain `DiceRoller` (the +1 only applies
  when the rolling user's id matches the `DoublesBonus.userId`, and it clears on use), so carrying the
  domain value through unchanged is correct. The UI state holds the trivial `DoublesBonus` id-holder
  directly (no render use, only fed back to the domain), avoiding a needless round-trip mapping.
  Updated the three preview `ArenaState.Combat` constructors to pass `doublesBonus = null`.
  `composeApp` compiles clean; `ArenaScreenViewModel` and `PerformCombatAction` suites are green.

### SS-020 — Fix Charm logic in onUseAbility
- [ ] Not completed
- **Source:** `ArenaScreenViewModel.kt:164` (`// TODO: This Charm logic is done oddly or incorrectly`)
- **Goal:** Correct how `selectedCharmTargetUserId` is resolved for Persuade/Charm. Related: SS-049.
- **Steps:** Verify against Bard Persuade rules; ensure target cannot be self/Bard; clean up the
  conditional.

### SS-021 — Forfeit vs finished leave + stats update
- [ ] Not completed
- **Source:** `ArenaScreenViewModel.kt:215` (`// TODO Arena: Need a leave that updates stats with a forfeit...`)
- **Goal:** Distinguish a forfeit-leave from a leave after the match finished, and update stats only
  when the user actually leaves (possibly after multiple matches).
- **Steps:** Define the two leave paths; wire stats update to the correct one.

### SS-022 — Remove arena match when last player leaves
- [ ] Not completed
- **Source:** `ArenaScreenViewModel.kt:224` (`// TODO: If is last person leaving, then removeArenaMatchUseCase(...)`)
- **Goal:** When the last player leaves, remove the arena match.
- **Steps:** Detect last-player-leaving; call `removeArenaMatchUseCase(arenaId = ...)`.

### SS-023 — Resources + split functions in ArenaChoosingClasses
- [ ] Not completed
- **Source:** `ArenaChoosingClasses.kt:37` (`// TODO Arena: resources, put back into separate functions`)
- **Goal:** Extract string resources and split the composable back into separate functions.

### SS-024 — Possibly add finished-state content
- [ ] Not completed
- **Source:** `MatchFinishedContent.kt:29` (`// TODO add something here if it makes sense...`)
- **Goal:** Decide whether the finished-match screen needs additional content; add it or keep as-is.

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

### SS-030 — Rename/assign property in ArenaScreenState
- [x] Completed
- **Source:** `ArenaScreenState.kt:32` (`// TODO: Update property name and way to assign it`)
- **Goal:** Improve the flagged property's name and assignment.

### SS-031 — Refactor ArenaScreenState mapping
- [ ] Not completed
- **Source:** `ArenaScreenState.kt:79` (`// TODO: Need to refactor this badly`)
- **Goal:** Refactor the flagged state-mapping code. Likely overlaps SS-017.

### SS-032 — Decide timer-start strategy
- [ ] Not completed
- **Source:** `ArenaScreenState.kt:204` (`// TODO: Hmm, how should timers be started? Based on NextCombatAction?`)
- **Goal:** Decide and document how timers should be started (e.g. based on `NextCombatAction`).

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

### SS-035 — Random names from RemoteConfig
- [ ] Not completed
- **Source:** `AppViewModel.kt:21` (`// TODO: Get random names from remote config`)
- **Goal:** Source random traveler names from RemoteConfig instead of hardcoding.

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

### SS-038 — Determine if IOSNullableStringSerializer is needed
- [ ] Not completed
- **Source:** `IOSNullableStringSerializer.kt:10` (`// TODO: Figure out if this is needed or useful`)
- **Goal:** Decide whether this serializer is required; keep with justification or delete.

### SS-039 — Unique user-names collection
- [ ] Not completed
- **Source:** `FirebaseProfileRepository.kt:12` (`// TODO: implement user names collection to help with keeping a unique name...`)
- **Goal:** Implement a names collection to enforce unique display names. Related: SS-035.

### SS-040 — Paginate / separate node for arena messages
- [ ] Not completed
- **Source:** `FirebaseArenaRepository.kt:23` (`// TODO Arena: Paginate messages and/or have a separate node...`)
- **Goal:** Reduce payload size by paginating messages or moving them to a separate RTDB node.

### SS-041 — Refactor flagged arena repo code
- [ ] Not completed
- **Source:** `FirebaseArenaRepository.kt:65` (`// TODO Arena: Look at this, refactor`)
- **Goal:** Refactor the flagged section of `FirebaseArenaRepository`.

### SS-042 — Challenge timestamp handling
- [ ] Not completed
- **Sources:**
  - `ChallengeEntity.kt:8` (`// TODO: Look into time part`)
  - `Challenge.kt:3` (`// TODO: Look into time part`)
- **Goal:** Resolve how the challenge timestamp is represented/serialized across the data entity and
  domain model.

### SS-043 — Extract class to separate file
- [ ] Not completed
- **Source:** `FirebaseTavernRepository.kt:434` (`// TODO: Move to separate file`)
- **Goal:** Move the flagged class out of `FirebaseTavernRepository` into its own file.

### SS-044 — Connecting messages in ConnectToArena
- [ ] Not completed
- **Source:** `ConnectToArena.kt:51` (`// TODO Arena: Add messages for connecting here?`)
- **Goal:** Decide whether to emit connection messages while joining an arena; add if useful.

### SS-045 — UI when match ends due to early leave
- [ ] Not completed
- **Source:** `ExitArena.kt:8` (`// TODO Arena: When it's finished because someone left early...`)
- **Goal:** Ensure there is clear UI when a match ends because a player left early. Related: SS-021.

---

## Domain layer

### SS-046 — Add maximum HP
- [x] Completed
- **Source:** `CharacterClass.kt:5` (`// TODO: Add a maximum HP`)
- **Goal:** Introduce a max-HP concept so healing/shields can't exceed a cap (interacts with
  Necromancer heal-to-full, shields).
- **Resolution:** Added a `maxHitPoints` field to the domain `CharacterClass` (set per class in the
  factory functions, equal to each class's starting HP) and plumbed it through `CharacterClassEntity`
  (with a `= hitPoints` serialization default so in-flight persisted matches still deserialize) and
  `CharacterClassState`, including all four mappers. Replaced the hardcoded `7 // Necro max HP` in
  `SummonMinionActivated`'s heal-to-full branches with `attacker.maxHitPoints`, so heal-to-full is
  now capped at the class max by construction. Skeleton/golem shields are tracked as separate counts
  and never added directly to `hitPoints`, so no further clamp was needed. `composeApp` debug
  unit-test suite is green (including the Summon Minion heal-to-full cases). The SS-047 RemoteConfig
  work can now drive `maxHitPoints` as part of the same class-stats schema.

### SS-047 — Class stat values from RemoteConfig
- [x] Completed
- **Source:** `CharacterClass.kt:32` (`// TODO: Get CharacterClass values from RemoteConfig`)
- **Goal:** Drive class stat tuning values (`hitPoints`/`maxHitPoints`/`armorClass`/`speed`) from
  RemoteConfig instead of hardcoding. Related: SS-004, SS-035. Ability roll values split out to
  SS-058.
- **Resolution:**
  - Added the `firebase-config` (gitlive) + `kotlinx-serialization-json` dependencies and a
    `RemoteConfigDataSource` seam (`FirebaseRemoteConfigDataSource`) holding the
    `character_class_stats` JSON parameter.
  - Introduced the domain `CharacterClassProvider` (impl `RemoteConfigCharacterClassProvider`) which
    overlays the remote stats onto the ability-wired factory classes. Exposed via
    `GetCharacterClassesUseCase`; config is fetched/activated once at app start via
    `InitializeGameConfigUseCase` from `AppViewModel`.
  - Routed the Arena class-selection list through the provider (`ArenaScreenViewModel` →
    `ArenaScreenState.characterClasses`), so the stats a player sees and commits into a match share
    one config-driven source.
- **Follow-on (separate tickets):** Tavern class list (SS-004) and random names (SS-035) still read
  static sources; ability roll values are SS-058.

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

### SS-059 — Single-source class stats via bundled JSON (drop factory stat literals)
- [ ] Not completed
- **Source:** Design follow-on to SS-047. Today the default class stat numbers exist in three places:
  the `CharacterClass` factory literals, the `defaultCharacterClassStats()` payload derived from them,
  and the Firebase Console value (`remote-config/character_class_stats.json`).
- **Goal:** Make the version-controlled `remote-config/character_class_stats.json` the single source
  of default stats. Bundle it as a `commonMain` resource and use it as both the RemoteConfig in-app
  default (`setDefaults`) and the provider's offline/first-launch fallback, then remove the stat
  literals from the `CharacterClass` factories (which shrink to type + abilities only).
- **Notes / cost:**
  - Resource reads are `suspend` (`Res.readBytes`), so parse the bundled JSON once during
    `CharacterClassProvider.refresh()` and cache it; keep `getAllClasses()` synchronous off the cache.
  - The factories can't read the resource (pure domain, no IO), so the provider becomes the sole
    assembler of stat-bearing `CharacterClass` instances. Tests/combat that use
    `CharacterClass.getRogue().hitPoints` etc. must instead source classes from the provider (fake
    stats source) or a test helper — this is the test refactor deferred from SS-047.
  - Could later layer disk-caching of the last fetched remote value on top of the bundled default.

### SS-048 — Die-value enum
- [ ] Not completed
- **Source:** `DiceRoller.kt:16` (`// TODO Enums for values 1 through 6?`)
- **Goal:** Consider a type-safe enum for die faces (1–6) instead of raw Ints.

### SS-049 — Persuade: pass selectedCharmTargetUserId from UI
- [ ] Not completed
- **Source:** `AbilityAction.kt:34` (`// TODO Persuade: In UI - need to pass in selectedCharmTargetUserId...`)
- **Goal:** Ensure the UI supplies the Charm target id for Persuade. Related: SS-020.

### SS-050 — Implement KillingMomentum
- [ ] Not completed
- **Source:** `NormalAttackRollHandler.kt:93` (`// TODO KillingMomentum`)
- **Goal:** Implement the Rogue "Killing Momentum" rule — on a K.O., attack another chosen player.

### SS-051 — Review ArenaMessage field (Maybe?)
- [ ] Not completed
- **Source:** `ArenaMessage.kt:18` (`// TODO: Maybe?`)
- **Goal:** Resolve the uncertain message field — keep or remove based on actual usage.

### SS-052 — LuckyCoin message approach
- [ ] Not completed
- **Source:** `ArenaMessage.kt:41` (`// TODO LuckyCoin is this the right approach for messages?`)
- **Goal:** Confirm the messaging approach for the Gambler Lucky Coin ability is correct.

### SS-053 — Docstrings for SpellPotencyRoll
- [ ] Not completed
- **Source:** `SpellPotencyRoll.kt:7` (`// TODO: Docstring, etc.`)
- **Goal:** Add docstrings/cleanup to `SpellPotencyRoll`.

### SS-054 — SpellPotency splash-damage attacker case
- [ ] Not completed
- **Source:** `HolyShieldHandler.kt:88` (`// TODO SpellPotency - what if is splash damage?`)
- **Goal:** Handle the Paladin Holy Shield retaliation when the incoming damage was SpellPotency
  splash damage (who is the "attacker"?).
- **Steps:** Define the intended interaction between Mage Spell Potency splash and Holy Shield;
  implement and test.

---

## Dependency injection

### SS-055 — Scope DI definitions instead of single
- [ ] Not completed
- **Source:** `Modules.kt:42` (`// TODO: Someday, scope these instead of single for everything`)
- **Goal:** Introduce appropriate Koin scopes rather than `single` for everything.

### SS-056 — Name DI definition
- [ ] Not completed
- **Source:** `Modules.kt:52` (`// TODO: What to name this?`)
- **Goal:** Give the flagged DI definition a meaningful name.

---

## Tavern timing

### SS-057 — Challenge timer must use the communal backend deadline
- [x] Completed
- **Source:** Reported bug — sending a challenge, then killing & relaunching the app, restarted the
  countdown from a fresh 1:00 and left the two clients out of sync.
- **Goal:** Drive the challenge countdown from the shared backend deadline instead of a hardcoded
  60s anchored to when each client first observed the challenge. Related: SS-042.
- **Resolution:**
  - `ChallengeHandler.handleChallengeStart` now computes `Challenge.secondsRemaining(now)` from the
    communal deadline rather than hardcoding `SIXTY_SECONDS`, so a relaunched/late client resumes at
    the real remaining time.
  - The start time is authored by the **backend** via `ServerValue.TIMESTAMP` (written in a single
    atomic `createChallenge` through the `TavernDataSource` seam, contained in
    `FirebaseTavernDataSource`), so the deadline no longer depends on the instigator's clock. The
    deadline is derived as `startTimeMillis + CHALLENGE_DURATION_MILLIS`; `endTimeMillis` is no
    longer client-written.
  - Each client subtracts its own local clock (no `serverTimeOffset` correction by choice) — drift
    is bounded by device clock accuracy (≈ms on NTP-synced devices), which is acceptable.
- **Known limitation / future option:** if real-device clock skew ever matters, add
  `.info/serverTimeOffset` correction. Left out deliberately to avoid complexity.

---

## Bugs

### SS-060 — Holy Shield: suppress KnockedOut dialog during the survival roll
- [x] Completed
- **Source:** Reported bug.
  - `ArenaScreenState.kt:35` (`(isUserPlayerKnockedOut || arenaState is ArenaState.Finished) && !hasMadeKnockoutDecision`)
  - `MatchDecisionDialog.kt:12` / `LoadedArenaScreen.kt:64`
- **Goal:** When a Paladin is knocked out but is about to roll (or is rolling) for Holy Shield, the
  KnockedOut/match-decision dialog must NOT show. The dialog should only appear once Holy Shield has
  resolved and the Paladin is actually knocked out (i.e. the roll failed to land in 10–12).
- **Steps:** Identify the window where `isUserPlayerKnockedOut` flips true while a pending Holy
  Shield roll is still owed; gate the `shouldShowMatchDecisionDialog` derivation on there being no
  pending Holy Shield roll for the user; verify the dialog reappears correctly if the roll fails.

---

## Profile & Auth

### SS-061 — Investigate Firebase Authentication for user profiles
- [ ] Not completed
- **Goal:** Choose the easiest and most robust way for users to create and own a profile. Evaluate
  Firebase Auth providers — Google Sign-In and phone (mobile) auth are the leading candidates — for a
  KMP (Android + iOS) app, and decide which to support. This unblocks tying profiles/stats to a real
  authenticated identity.
- **Steps:** Survey gitlive `firebase-auth` support across Android/iOS; compare Google Sign-In vs.
  phone auth vs. anonymous-then-link for friction, account recovery, and abuse resistance; consider
  anonymous auth as the first-launch default with later upgrade/link to a permanent provider; document
  a recommendation and the migration path for existing anonymous profiles. Related: SS-036 (signed-in
  user with missing profile), SS-064 (ProfileScreen).

### SS-062 — Profile deletion in Firestore
- [ ] Not completed
- **Source:** `FirebaseProfileRepository.kt` (profile data layer).
- **Goal:** Add the ability to delete a user's profile from Firestore, with the corresponding
  domain use case and repository method.
- **Steps:** Add a `DeleteProfileUseCase` + `ProfileRepository.deleteProfile(...)`; ensure related
  data is cleaned up (the unique display-name reservation from SS-039, any stats records, the auth
  account if appropriate per SS-061); surface it from the ProfileScreen (SS-064) behind a
  confirmation.

### SS-063 — App Drawer cleanup
- [ ] Not completed
- **Source:** `SettingsDrawer.kt` (also flagged by `// TODO(SS-037): What to do with this?`).
- **Goal:** The drawer currently holds ad-hoc items (a raw Display Name field, Tavern/Arena links).
  Replace it with a deliberate set of entries. The name-change capability moves out of the drawer and
  into the ProfileScreen (SS-064); the drawer should instead link to Profile, Game Rules (SS-067),
  Leaderboard (SS-065), etc.
- **Steps:** Decide the final drawer entry list; remove the inline display-name editor; add
  navigation entries to the new screens; tidy styling. Related: SS-037, SS-064.

### SS-064 — ProfileScreen
- [ ] Not completed
- **Goal:** Add a ProfileScreen, accessed from the app drawer (SS-063). It contains the name-change
  capability (moved out of `SettingsDrawer`), profile deletion (SS-062), and, once game-stat handling
  exists, the user's own game statistics.
- **Steps:** Create the route + screen + ViewModel; move the display-name edit flow here (reusing
  `UpdateProfileDisplayNameUseCase`); wire profile deletion (SS-062) behind a confirmation; leave a
  placeholder for per-user stats. Related: SS-035, SS-039, SS-061, SS-065.

---

## Stats & Leaderboard

### SS-065 — LeaderboardScreen
- [ ] Not completed
- **Goal:** Once game stats are recorded on user profiles (and general game-stat handling lands —
  see related stats tickets), add a screen showing the top players. Accessible from the drawer
  (SS-063).
- **Notes:** Depends on stats existing/being persisted (Related: SS-021 forfeit/finished stats
  update). Likely splits into multiple tickets as work begins — stats schema/aggregation, query/
  ordering strategy, and the screen UI.

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

### SS-067 — GameRulesScreen
- [ ] Not completed
- **Goal:** A screen, accessible from the drawer (SS-063), where users can read the game rules and
  class abilities.
- **Steps:** Decide the rules content source (static resource vs. RemoteConfig-driven); build a
  readable/navigable layout (core combat rules + per-class abilities); link to the rules chatbot
  (SS-066) if/when it exists.

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

### SS-069 — Shake-to-roll dice UX
- [ ] Not completed
- **Goal:** Investigate a dice-rolling interaction where the user taps and holds a pair of dice (or a
  single die, depending on what the current step requires), shakes the phone to simulate rolling, and
  on release the existing ViewModel roll handler is invoked.
- **Steps:** Determine which die count the current combat step expects (1 vs. 2 dice) to drive what's
  held; add accelerometer/shake detection (platform `expect`/`actual` for Android + iOS); wire the
  release gesture to the existing roll entry point; design the press/shake/release visual feedback.

---

## Release & versioning

### SS-070 — Investigate app version control & force-upgrade
- [ ] Not completed
- **Goal:** Establish a way to gate clients by app version so an outdated client can be soft- or
  force-upgraded — important since all clients share one RTDB game-state schema and a stale client
  could read/write incompatible data.
- **Open question:** Is RemoteConfig the path forward — publishing `minimum_supported_version` /
  `latest_version` (and maybe a force-vs-recommend flag) there, with the app comparing its own build
  version on launch? It already initializes RemoteConfig at app start (SS-047), so this would reuse
  that plumbing.
- **Steps:** Decide the version source (RemoteConfig parameters vs. alternative); define the version
  comparison + where it runs (launch/`AppViewModel`); design the soft-update prompt vs. hard
  force-upgrade block UI with store deep-links (Play / App Store) for both platforms; consider how
  this interacts with the shared game-state schema versioning. Related: SS-047.
