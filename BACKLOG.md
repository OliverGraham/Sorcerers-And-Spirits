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
- [x] Completed
- **Sources:**
  - `ExampleTravelers.kt:6` (`// TODO: Make example state that can be reused for previews`)
  - `TavernScreen.kt:199` (`// TODO: Preview`)
  - `ChallengeDialog.kt:169` (`// TODO: Update preview data`)
- **Goal:** Build reusable example/preview state for Tavern composables and add/refresh previews.
- **Resolution:** Turned `ExampleTravelers.kt` into a shared sample-state catalog —
  `exampleUserTraveler`, `exampleOtherTravelers` (mix of `Available`/`Busy`),
  `exampleInstigatorChallengeState`/`exampleRecipientChallengeState`, and a full
  `exampleTavernState` (defaulting `characterClasses` from `CharacterClass.getAllClasses()`).
  Replaced the empty `TavernScreenPreview` stub with a real `UiState.Loaded(exampleTavernState)`
  preview wrapped in `SorcerersAndSpiritsTheme`, repointed both `ChallengeDialog` previews at the
  shared challenge states (dropping the duplicated inline `ChallengeState` literals and the now-unused
  `ChallengerStatus` import), and collapsed `TheBar`'s hand-rolled preset onto
  `exampleUserTraveler`/`exampleOtherTravelers`. `composeApp` Android debug compiles clean.

### SS-009 — Improve TheBar
- [x] Completed
- **Source:** `TheBar.kt:42` (`// TODO: Improve`)
- **Goal:** Clarify and improve `TheBar` (scope is vague — define concretely while working on it).
- **Decision:** The concrete improvement (separate from the copy/icon/styling tickets SS-010/011/012)
  was a selection-state correctness bug. Selection was remembered as a `Set<TravelerState>` of full
  data-class objects. When a selected traveler left the Tavern or flipped `Available`↔`Busy`, the
  remembered object went stale: it still inflated the `Challenge (N)` count and was passed to
  `onSendChallengerNotifications`, even though a busy/absent traveler can't be challenged. A status
  change also made the stale object unequal to the live one, so the card lost its selected highlight
  while the phantom entry lingered in the set.
- **Resolution:** Track selection by the stable `userId` (`Set<String>`) and derive the effective
  selectable set from the current `otherTravelers`, filtered to those still present and not
  `TavernStatus.Busy`. Selections of travelers who leave or go busy now drop out of both the count and
  the outgoing challenge automatically. Moved the toggle logic up to `TheBar` (single source of truth
  on the id set); `BarFolk` now takes `selectedUserIds: Set<String>` + `onToggleTraveler` and checks
  membership by `userId`. `composeApp` Android debug compiles clean.

### SS-010 — Reword challenge-list UI copy in TheBar
- [x] Completed
- **Source:** `TheBar.kt:83` (`// TODO: Change UI - should say other players to challenge...`)
- **Goal:** Update copy to clearly communicate "other players to challenge."
- **Resolution:** Replaced the two hardcoded `BarFolk` header strings with string resources reworded
  to convey that these are the other players available to challenge: the count label `"Travelers in
  Tavern: $count"` → `tavern_travelers_to_challenge` (`"Travelers to challenge: %1$s"`) and the
  subtitle `"Challenge people to fight in The Arena:"` → `tavern_challenge_instruction` (`"Tap
  travelers to challenge them into The Arena:"`). Both pulled via `stringResource(...)` (the count
  passed as the format arg). Dropped the SS-010 TODO; the loose "settings UI to show every player and
  their status" idea is left to the drawer/settings tickets (SS-037/SS-063). `composeApp` Android
  debug compiles clean.

### SS-011 — New challenge icon in TheBar
- [x] Completed
- **Source:** `TheBar.kt:92` (`// TODO: new icon`)
- **Goal:** Replace the placeholder icon with the intended challenge icon.
- **Resolution:** `TheBar`'s top-start `IconButton` (which calls `onShowChallengeDialog`) used the
  `ic_menu` drawer/hamburger glyph — the same icon `TopBar` uses to open the app drawer, so it read as
  a menu, not a challenge. Swapped it to `ic_swords`, the project's established "open challenge dialog"
  icon already used by `ChallengeDialogHandle` (the sheet drag-handle button that fires the same
  action). Also replaced the placeholder empty `contentDescription = ""` with `null` to match that
  sibling handle's convention. `composeApp` Android debug compiles clean.

### SS-012 — Update TheBar styling
- [x] Completed
- **Source:** `TheBar.kt:158` (`// TODO: Update styling`)
- **Goal:** Finalize styling for `TheBar`.
- **Resolution:** Reworked the traveler `ToCard`:
  - **Fixed the broken selected state:** the selected container was `primaryContainer.copy(alpha =
    0.1f)`, which rendered the highlight nearly invisible (only the border distinguished it). Now uses
    full `primaryContainer` with `onPrimaryContainer` text for proper contrast, plus a stronger
    selected elevation (6.dp vs 2.dp) on top of the existing primary border.
  - **Idiomatic clickable card:** replaced the `Box.clickable(enabled = !isBusy)` wrapper with the
    Material3 clickable `Card(onClick = …, enabled = !isBusy)` overload (matching `CharacterClassCard`),
    so the card gets a proper ripple and routes the Busy appearance through the Card's
    `disabledContainerColor`/`disabledContentColor` instead of manual per-state alpha branching.
  - **Surface lift + spacing:** unselected cards now sit on `surfaceContainerHigh` to lift them off
    the bottom-sheet surface, and inner padding tightened to `horizontal = 20.dp, vertical = 16.dp`.
  - Since selection is derived to exclude busy travelers (SS-009), a card is never both selected and
    busy, so the three-way color `when` collapsed to clean selected-vs-default + disabled cases.
    Removed the now-unused `clickable` import.
  - **Split selected/unselected into separate composables (per review):** `ToCard` is now a single
    `AnimatedContent(targetState = isSelected)` that switches between `SelectedTravelerCard` and
    `UnselectedTravelerCard` — one `if`, with each composable styling itself directly instead of
    branching every Card parameter inline. The busy state lives entirely in `UnselectedTravelerCard`.
    Shared pieces are factored out: a `TravelerCardLabel` composable (the centered name `Text`) and a
    `Modifier.travelerCardSize()` extension so both cards keep the same footprint and animate cleanly.
    `composeApp` Android debug compiles clean.

---

## Arena UI

### SS-013 — Review ArenaEvent (empty TODO)
- [x] Completed
- **Source:** `ArenaEvent.kt:3` (`// TODO Arena:`)
- **Goal:** Empty placeholder TODO — determine intent (likely additional events) or remove.
- **Resolution:** The placeholder was an open-ended musing ("use for one-time animations… what other
  events are needed?"). The existing `ArenaEvent` (`MatchTimedOut`, `NavigateBack`) covers current
  needs, and concrete future events belong to their own tickets (e.g. timer/phase handling under
  SS-014, message-log animation under SS-025) rather than a speculative note here. Removed the TODO;
  no code change to the interface.

### SS-014 — Handle match/timer expiration per state
- [x] Completed
- **Sources:**
  - `ArenaScreen.kt:78` (`/* TODO */` in `ArenaEvent.MatchTimedOut` handling)
  - `ArenaScreenViewModel.kt:292` (timer expiration in `ChoosingClasses`)
  - `ArenaScreenViewModel.kt:296` (timer expiration in `Combat`)
  - `ArenaScreenViewModel.kt:300` (timer expiration in `Finished`)
- **Goal:** Define and implement what happens when the timer expires in each `ArenaState`, and how
  the `MatchTimedOut` event is surfaced to the user.
- **Decision (per state):**
  - **ChoosingClasses:** auto-pick for the user. The active player's client auto-selects the class
    they had highlighted (`selectedClass`) if any, otherwise a random still-available class, routed
    through the existing `onChooseCharacterClass` flow. Guarded on `isItThisUsersTurn` so only the
    owing client acts.
  - **Combat:** auto-resolve the owed action (`autoResolveCombatForUser`), guarded on
    `combatControls.isEmpty()` so only the client that owes a roll acts. `Idle` (turn start) →
    auto-attack a random *living* opponent; `NormalAttack`/`AbilityRoll` (mid-action) → auto-roll
    the owed die via the existing `onAttack`/`onUseAbility` paths (Persuade/Charm gets a random
    target from the ability's own `targetableUserIds`). No domain change.
  - **WaitingForAllPlayers:** the match never filled, so `leaveTimedOutMatch()` exits the arena,
    frees the traveler, and emits `ArenaEvent.MatchTimedOut`, which `ArenaScreen` now handles by
    navigating back to the Tavern.
  - **Finished:** no auto-action — the timer is stopped in `Finished` and the user stays/leaves
    through the match-finished decision dialog.
- **Also (timer UX):** timer durations lengthened (Waiting/Choosing 30s, Combat 45s) and the
  countdown is hidden until 15s remaining (`isTimerVisible`) and turns red + pulses from 10s
  (`isTimerUrgent`). Timer-start strategy resolved under SS-032.
- **Verification:** `composeApp` Android debug compiles clean; all `*Arena*` unit tests pass.

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
- [x] Completed
- **Source:** `ArenaActions.kt:38` (`// TODO: Rename/refactor`)
- **Goal:** Rename/refactor the flagged ArenaActions construct for clarity.
- **Decision:** The field names (`targetedOpponentUserId`/`userPlayerId`) are already the
  project-wide convention (`ArenaScreenState`, `ArenaCombat`, etc.), so renaming wasn't the win. The
  real defect was an **asymmetry**: `AttackArguments` had a clean `AttackArguments.toDomain()` mapper
  in `CombatActionState.kt`, while `UseAbilityArguments`'s equivalent input→domain mapping was
  sprawled inline in `ArenaScreenViewModel.onUseAbility()` (Charm resolution + a combatant-id `when`),
  making the two combat-input handlers look unrelated.
- **Resolution:** Extracted a parallel `UseAbilityArguments.toDomain(combatActionState)` extension
  into `CombatActionState.kt` (next to `AttackArguments.toDomain()`), carrying the in-flight
  `CombatActionState` as a parameter since an ability's combatant ids come from the ongoing action
  (not from the argument itself, unlike an attack). `onUseAbility()` now mirrors `onAttack()` —
  build `CombatActionArguments`, map via `toDomain(...)`, dispatch. Removed the now-unused
  `CurrentCombatAction` import from the ViewModel. The Charm-resolution block moved verbatim and keeps
  its own `// TODO(SS-020)` marker (behavior unchanged; SS-020 still owns that fix).
- **Verification:** `composeApp` Android debug compiles clean; `ArenaScreenViewModelTest` is green.

### SS-017 — Refactor ArenaScreenViewModel
- [x] Completed
- **Source:** `ArenaScreenViewModel.kt:42` (`// TODO(SS-017): This is all fucked`)
- **Goal:** Address the broad "this is all fucked" flag — clean up the ViewModel section it marks.
- **Resolution:** The blanket marker predated the targeted cleanups already done across SS-015
  (delete dead actions), SS-016 (combat-input mapper symmetry), SS-019 (doublesBonus), and SS-032
  (timer-start strategy), which left the ViewModel mostly tidy. The one real remaining smell was the
  duplicated combat-dispatch boilerplate: `onAttack` and `onUseAbility` each re-fetched the loaded
  state, re-cast `arenaState` to `ArenaState.Combat`, and rebuilt an identical `CombatActionArguments`
  before dispatching. Extracted that shared path into two private helpers — `currentCombatState()`
  (the loaded → `Combat` cast) and an `ArenaState.Combat.performCombatAction(currentAction:
  CurrentCombatAction)` extension that builds the args and calls `performCombatActionUseCase`. Both
  handlers now read as a two-line "resolve the domain action, dispatch it" mirror of each other,
  matching the symmetry SS-016 established for the input→domain mappers. Removed the stale top-of-file
  TODO. The broader state-mapping refactor remains owned by SS-031.
- **Verification:** `composeApp` Android debug compiles clean; all `*Arena*` unit tests pass.

### SS-018 — Better userId resolution
- [x] Completed
- **Source:** `ArenaScreenViewModel.kt:87` (`// TODO(SS-018): Figure out better way for userId`)
- **Goal:** Establish a cleaner way to obtain the current user's id.
- **Resolution:** The smell was `getUserIdUseCase().orEmpty()` — a null id (no signed-in user)
  silently became `""`, which then flowed through `updateScreenState`/`toUpdatedState` as the
  current-user id. An empty id never matches any player, so the whole Arena would misbehave silently
  (no combat controls, wrong "is it this user's turn", etc.) instead of failing. Replaced the
  `.orEmpty()` swallow with an explicit null check that emits `UiState.Error` and bails out of the
  load. By the time the Arena loads the user is always signed in (anonymous auth at startup), so a
  null id is a genuine error — and `UiState.Error` is already user-retryable via the existing
  `retry()`/`ErrorContent` path (SS-007).
- **Verification:** `composeApp` Android debug compiles clean; all `*Arena*` unit tests pass.

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
- [x] Completed
- **Source:** `CombatActionState.kt:142` (`// TODO(SS-020): This Charm logic is done oddly or
  incorrectly`) — the Charm-resolution block was moved here from `ArenaScreenViewModel.onUseAbility`
  under SS-016.
- **Goal:** Correct how `selectedCharmTargetUserId` is resolved for Persuade/Charm. Related: SS-049.
- **Resolution:** The inline conditional in `UseAbilityArguments.toDomain` set the Charm target
  whenever `targetedOpponentUserId.isNotEmpty()` — a magic empty-string sentinel that performed **no
  validation** that the selected id was actually a legal target. Per the Bard Persuade rule a charmed
  attacker cannot be redirected at the Bard or at the attacker themselves, and the Domain already
  encodes this: `PersuadeHandler.strumLuteToCharm` builds `targetableUserIds` as the conscious
  players minus the attacker minus the defender (Bard). Extracted a named
  `AbilityActionState.withSelectedCharmTarget(targetUserId)` that only applies the target when
  `targetUserId in targetableUserIds`, so an illegal/self/Bard/empty id leaves
  `selectedCharmTargetUserId` unset (the Domain `charm()` then no-ops via its `?: return`). Every
  non-Charm ability ignores the target, as before.
- **Verification:** `composeApp` Android debug compiles clean; `ArenaScreenViewModelTest` is green,
  including a new case asserting an untargetable (Bard self) selection is rejected.

### SS-022 — Remove arena match when last player leaves
- [x] Completed
- **Source:** `ArenaScreenViewModel.kt` (`// TODO(SS-022): If is last person leaving, then removeArenaMatchUseCase(...)`)
- **Goal:** When the last player leaves, remove the arena match.
- **Resolution:** Handled atomically inside the `ExitArena` Firebase transaction rather than via a
  separate ViewModel call. A ViewModel-side `removeArenaMatchUseCase` after `exitArena` would be
  racy (another client could join in the gap) and would miss the `WaitingForAllPlayers` case
  entirely. Added a single guard at the top of `exitArenaTransaction`: if removing the leaving user
  empties `playerEntitiesMap`, return `null` so the same transaction that removes the player also
  deletes the match — extending the pattern the `Finished` branch already used to every state. This
  also closes a real orphan-match leak: previously the last player leaving from
  `WaitingForAllPlayers` left an empty match behind (it only `copy`'d the emptied map), and
  Choosing/Combat could orphan an empty `Finished` match.
- **Cleanup:** `removeArenaMatchUseCase` existed only for this TODO and was never called, so removed
  the dead chain — the ViewModel injection, the `RemoveArenaMatchUseCase` use case, its DI
  registration, and `ArenaRepository.removeArenaMatch` / its Firebase impl (which unconditionally
  nulled the match regardless of remaining players).
- **Verification:** `composeApp` Android debug compiles clean; `ArenaScreenViewModelTest` and
  `ExitArenaUseCaseTest` pass. Related: SS-045 (left-early UI), SS-071 (stats flush on leave).

### SS-023 — Resources + split functions in ArenaChoosingClasses
- [x] Completed
- **Source:** `ArenaChoosingClasses.kt:37` (`// TODO Arena: resources, put back into separate functions`)
- **Goal:** Extract string resources and split the composable back into separate functions.
- **Resolution:** Replaced the three hardcoded UI strings with string resources —
  `arena_choosing_classes_title` ("Choose Your Class"),
  `arena_choosing_classes_waiting` ("%1$s is choosing their class…") and
  `arena_choosing_classes_confirm` ("Confirm Selection") — pulled via `stringResource(...)`. Split the
  one monolithic `ArenaChoosingClasses` body into focused composables: `ChoosingClassesTitle`
  (turn-aware heading), `ChoosingClassesCards` (the class pager) and `ConfirmSelectionButton`, leaving
  the top-level composable as a readable three-call `Column`. The `Modifier.weight(1f)` is applied at
  the call site so it stays in `ColumnScope`; the cards/button stay `ArenaState.ChoosingClasses`
  extensions so they read the in-flight selection state directly. Removed the SS-023 TODO.
- **Verification:** `composeApp` Android debug compiles clean.

### SS-024 — Possibly add finished-state content
- [x] Completed
- **Source:** `MatchFinishedContent.kt:29` (`// TODO add something here if it makes sense...`)
- **Goal:** Decide whether the finished-match screen needs additional content; add it or keep as-is.
- **Decision:** Add a match outcome. The finished screen previously showed only the
  headline + "Match X of Y" — it never told players who actually won. The data was already there
  (`ArenaMatch.Finished.playerMap` carries every player with their class and HP), it just wasn't
  threaded into the UI state.
- **Resolution:**
  - Converted `ArenaState.Finished` from a bare `data object` into a `data class` carrying
    `players: List<PlayerState.Selected>`, with derived `survivors`/`winner` (the single conscious
    player, or `null` for a mutual-KO draw). Populated it in `toUpdatedState` by mapping the domain
    `playerMap`. Added a derived `isConscious` (`hitPoints > 0`) to `CharacterClassState` so the UI
    computes survival from existing state rather than re-deriving inline (per the derived-UI-state
    convention).
  - `MatchFinishedContent` now renders a **WinnerBanner** ("{name} wins as the {Class}!" or
    "No survivors — it's a draw") plus a **MatchRecap** — one card per player showing class and a
    Survived / Knocked-out badge — under the existing Gauntlet/Match-progress header. Wrapped in a
    vertical scroll for larger rosters.
  - Updated the preview provider's Finished example and the `ArenaScreenStateTest` construction to
    the new signature.
- **Verification:** `:composeApp:compileDebugKotlinAndroid` compiles clean; `ArenaScreenStateTest`
  and the full `*Arena*` unit suite pass.

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
- [x] Completed
- **Source:** `ArenaScreenState.kt:204` (`// TODO: Hmm, how should timers be started? Based on NextCombatAction?`)
- **Goal:** Decide and document how timers should be started (e.g. based on `NextCombatAction`).
- **Decision:** The timer belongs to the action a player currently owes, so it (re)starts only when
  that owed action changes and otherwise keeps running across unrelated state updates (e.g. an
  incoming message). In `determineTimerAction`:
  - **Combat:** start when entering combat or when `combatActionState` changes (derived from
    `NextCombatAction` — a new turn or roll); otherwise `TimerAction.None`. This replaces the old
    behavior that restarted the combat timer on every emission.
  - **ChoosingClasses:** start when entering the phase or when `activePlayerUserId` changes, so each
    player gets their own fresh clock for their turn (previously a single timer spanned the whole
    phase).
- **Verification:** `composeApp` Android debug compiles clean; all `*Arena*` unit tests pass.

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
- [x] Completed
- **Source:** `AppViewModel.kt:21` (`// TODO: Get random names from remote config`)
- **Goal:** Source random traveler names from RemoteConfig instead of hardcoding.
- **Resolution:** Added `RemoteConfigKeys.RANDOM_TRAVELER_NAMES` (`random_traveler_names`), a JSON
  string-array value. The name pool now lives **only** in the version-controlled bundled resource
  `composeResources/files/remote_config/random_traveler_names.json`; `FirebaseRemoteConfigDataSource`
  reads it at `initialize()` and uses it for both `setDefaults` and the offline/first-launch
  fallback (a single `FALLBACK_TRAVELER_NAME` const guards only the pre-initialize window). A domain
  `RandomNameProvider` (`RemoteConfigRandomNameProvider`) and `GetRandomTravelerNameUseCase` feed
  `AppViewModel`, which no longer hardcodes the list. Establishes the bundled-default pattern that
  SS-085 generalizes. Related: SS-004, SS-059, SS-085.

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
- [x] Completed
- **Source:** `IOSNullableStringSerializer.kt:10` (`// TODO: Figure out if this is needed or useful`)
- **Goal:** Decide whether this serializer is required; keep with justification or delete.
- **Decision:** Delete it. The serializer sanitized gitlive's iOS RTDB "fake null" decode (the literal
  `"<null>"`/`"null"` string → real Kotlin `null`), but it was **dead code** — never wired to any
  field anywhere in git history (no `@Serializable(with = …)` ever referenced it). It was added
  speculatively during the "Get iOS working!" migration (86bb4cf) that replaced the old Swift
  `TavernCodable`/`TravelerCodable` bridges.
- **Why it isn't needed:** The only persisted nullable-`String` entity field is
  `CharmEntity.selectedCharmTargetUserId` (the `Int?` roll fields are unaffected). It is `null`-defaulted
  and RTDB omits null keys on write, so on read the key is absent and kotlinx-serialization falls back
  to the `= null` default — the fake-null-string path never triggers. iOS has shipped (SS-087/SS-090)
  with no `"<null>"` corruption observed.
- **Resolution:** Removed `IOSNullableStringSerializer.kt`. Consistent with the project's delete-dead-code
  convention (SS-015, SS-022); the ~10-line sanitizer is trivial to reintroduce and apply precisely to a
  specific field if a real case ever surfaces.
- **Verification:** `:composeApp:compileDebugKotlinAndroid` + `:composeApp:compileKotlinIosSimulatorArm64`
  compile clean.

### SS-039 — Unique user-names collection
- [x] Completed
- **Source:** `FirebaseProfileRepository.kt:12` (`// TODO: implement user names collection to help with keeping a unique name...`)
- **Goal:** Implement a names collection to enforce unique display names. Related: SS-035.
- **Resolution:** Added a `DisplayNames` Firestore collection (`FireStoreKeys.DISPLAY_NAMES`) whose
  documents are keyed by the normalized display name (trim + lowercase, `/` → `_`) and hold the
  owning `userId` (`DisplayNameEntity`). `createProfile` and `updateDisplayName` now run inside
  `runTransaction`: they read the target name doc first, return `DisplayNameResult.NAME_TAKEN` if it
  is owned by another user, otherwise reserve the name and write the profile in the same atomic
  transaction. `updateDisplayName` also releases the user's previous reservation. The repository
  methods and their use cases (`CreateProfileUseCase`, `UpdateProfileDisplayNameUseCase`) return the
  new `DisplayNameResult` enum (`SUCCESS`/`NAME_TAKEN`/`ERROR`); `AppViewModel.createProfile` surfaces
  `NAME_TAKEN` by keeping the sign-in dialog up with a "name is already taken" notice (`AppState.nameTaken`).
- **Notes:** Reservation cleanup on profile deletion was completed in SS-062; consuming the update
  result in the ProfileScreen edit flow stays with SS-064.

### SS-040 — Paginate / separate node for arena messages
- [x] Completed
- **Source:** `FirebaseArenaRepository.kt:23` (`// TODO Arena: Paginate messages and/or have a separate node...`)
- **Goal:** Reduce payload size by paginating messages or moving them to a separate RTDB node.
- **Outcome:** Cap the inline message log to the most recent `MAX_RETAINED_MESSAGES` (30) via
  `List<ArenaMessageEntity>.appendCapped` (`Common.kt`), applied at every append site
  (`FirebaseArenaRepository.applyCombatResult`, `ChooseCharacterClass`). This bounds both the
  transaction read/write size and each `getArenaMatchFlow` snapshot payload as a match runs long.
  No domain/UI changes were needed: the message-log UI only displays recent messages and tracks
  unhandled ones relative to the tail of the list, so they stay correct after older entries drop.
- **Notes:** Chose capping over a separate RTDB node — there is no full-history viewing UI to
  justify archiving older messages on demand. A separate node (or `limitToLast` pagination) remains
  a future option if message history is ever surfaced.

### SS-041 — Refactor flagged arena repo code
- [x] Completed
- **Source:** `FirebaseArenaRepository.kt` (`// TODO(SS-041) Arena: Look at this, refactor`)
- **Goal:** Refactor the flagged section of `FirebaseArenaRepository`.
- **Resolution:** `applyCombatResult` was the asymmetric repository method — every other operation
  (`chooseCharacterClass`, `connectToArena`, `exitArena`, `stayForAnotherMatch`) delegates to a
  dedicated invokable class in `data.arena.utilities`, but `applyCombatResult`'s entire `runTransaction`
  body was inlined in the repository. Extracted it into a new `ApplyCombatResult` class following the
  same convention (input in the constructor, `suspend operator fun invoke(arenaReference)`, transaction
  logic in a private `ArenaMatchEntity` extension). Decomposed the monolithic lambda into a guarded
  `applyCombatResultTransaction` plus two builder methods, `ArenaMatchEntity.Combat.toFinished` and
  `toUpdatedCombat`, mirroring the helper-method style of the sibling utilities. The repository method
  is now a two-line delegation like its peers; removed the now-unused `addNewKey` / `appendCapped`
  imports from the repository.
- **Verification:** `:composeApp:compileDebugKotlinAndroid` compiles clean; `*Arena*` + `*Combat*`
  unit tests pass.

### SS-042 — Challenge timestamp handling
- [x] Completed
- **Sources:**
  - `ChallengeEntity.kt` (`// TODO(SS-042): Look into time part`)
  - `Challenge.kt` (`// TODO(SS-042): Look into time part`)
- **Goal:** Resolve how the challenge timestamp is represented/serialized across the data entity and
  domain model.
- **Decision:** SS-057 already settled persistence (the backend stamps a single `startTimeMillis` via
  RTDB `ServerValue.TIMESTAMP`; the deadline is derived). What these TODOs still flagged was the
  *modeling*: the raw start time leaked into the domain and the countdown fallback was coupled to the
  wrong constant.
- **Resolution:**
  - **One representation per layer.** `ChallengeEntity` keeps the single server-stamped
    `startTimeMillis: Long?` as the authoritative source (documented with KDoc explaining the
    `ServerValue.TIMESTAMP` origin and that the deadline is derived). The **domain** `Challenge` now
    carries only the derived `endTimeMillis` deadline — the field it actually reasons about. Dropped
    the redundant `startTimeMillis` from the domain model: it was set by `toDomain()` but never read in
    production (only the deadline drives `secondsRemaining`/the countdown), so holding both the raw
    start and its derived deadline was duplication.
  - **Decoupled the duration constant.** `secondsRemaining`'s no-deadline fallback used the generic
    `SIXTY_SECONDS` (also the seconds-per-minute divisor and the default `Timer` duration), so a change
    to the challenge length would silently desync the fallback from the deadline. Added
    `CHALLENGE_DURATION_SECONDS` as the single source the challenge length is built from
    (`CHALLENGE_DURATION_MILLIS = CHALLENGE_DURATION_SECONDS * ONE_SECOND`), and pointed the fallback at
    it. Also converted `secondsRemaining`'s `?.let { } ?: …` to an explicit null check
    ([[prefer-null-checks-over-let]]).
  - Updated the two domain-`Challenge` test constructors (`ChallengeTest`, `ChallengeHandlerTest`) to
    drop `startTimeMillis`; the `SendChallengeUseCaseTest` server-timestamp assertion is on the
    *entity* and is unchanged. Decided against modeling time as `kotlin.time.Instant`/`Duration` —
    RTDB stores raw `Long` millis and `ServerValue.TIMESTAMP` resolves to one, so `Long` at the
    persistence boundary is correct and an `Instant` wrapper would only add mapping churn.
- **Verification:** Android + iOS (`compileKotlinIosSimulatorArm64`) compile clean; `*Challenge*` /
  `*Tavern*` / `*SendChallenge*` / `*ConnectTravelerPresence*` unit tests pass. Related: SS-057.

### SS-043 — Extract class to separate file
- [x] Completed
- **Source:** `FirebaseTavernRepository.kt` (`// TODO(SS-043): Move to separate file`)
- **Goal:** Move the flagged class out of `FirebaseTavernRepository` into its own file.
- **Resolution:** The flagged block was not a class but a cluster of file-level helper functions left
  at the bottom of `FirebaseTavernRepository.kt`: the RTDB update-map mutators (`removeChallenge`,
  `clearMailbox`, `clearMailboxes`), the `ChallengeEntity.isExpired()` expiry predicate, and the
  `String.toChallengesPath()` / `toChallengeMailBoxesPath()` path builders (the latter two already
  `internal` and shared with `FirebaseTavernDataSource`). Moved all of them into a new
  `FirebaseTavernChallenges.kt` in the same `data.tavern` package, promoting the three `private` mutators
  + `isExpired` to `internal` (they now live in a separate file but are only used within the module).
  Removed the now-unused `CHALLENGES_PATH`, `CHALLENGE_MAILBOXES_PATH`, `CHALLENGE_DURATION_MILLIS`, and
  `Clock` imports from the repository (`ExperimentalTime` stays — still used by `getChallengeEntity`).
  Converted `isExpired`'s `?.let { } == true` to an explicit null check while relocating it.
- **Verification:** `:composeApp:compileDebugKotlinAndroid` compiles clean.

### SS-044 — Connecting messages in ConnectToArena
- [ ] Not completed
- **Source:** `ConnectToArena.kt:51` (`// TODO Arena: Add messages for connecting here?`)
- **Goal:** Decide whether to emit connection messages while joining an arena; add if useful.

### SS-045 — UI when match ends due to early leave
- [ ] Not completed
- **Source:** `ExitArena.kt:8` (`// TODO Arena: When it's finished because someone left early...`)
- **Goal:** Ensure there is clear UI when a match ends because a player left early. Related: SS-071.

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
- [x] Completed (core) — factory-literal removal intentionally deferred, see below
- **Source:** Design follow-on to SS-047. Default class stat numbers used to exist in three places:
  the `CharacterClass` factory literals, the `defaultCharacterClassStats()` payload derived from them,
  and the Firebase Console value (`remote-config/character_class_stats.json`).
- **Goal:** Make the version-controlled `character_class_stats.json` the single source of default
  stats: bundle it as a `commonMain` resource and use it as both the RemoteConfig in-app default
  (`setDefaults`) and the offline/first-launch fallback.
- **Resolution:** `character_class_stats.json` moved into the bundled
  `composeResources/files/remote_config/` dir (same place as the names file).
  `FirebaseRemoteConfigDataSource` reads it at `initialize()` via the shared `loadBundledDefault`
  helper and uses it for `setDefaults` and the offline fallback — class stats now follow the exact
  same bundled-default path as `random_traveler_names` (SS-085). Drift between the bundled JSON and
  the synchronous in-app values is now impossible: `BundledRemoteConfigDefaultsTest` asserts the
  bundled JSON equals `defaultCharacterClassStats()`.
- **Deferred (deliberate):** the `CharacterClass` factory stat literals were **kept** as the
  provider's last-resort per-class fallback rather than removed. Production never relies on them
  (in-match classes come from `CharacterClassEntity`; the selection list is JSON-overlaid by the
  provider), and removing them forces ~130 combat-test/preview references off synchronous factory
  access onto an async/provider or test-helper source — high churn, pure test risk, near-zero
  product value now that the drift guard prevents divergence. Reopen as its own ticket if the
  factories ever need to shrink to type + abilities only. Related: SS-085, SS-047.

### SS-085 — Bundled-JSON in-app defaults for all RemoteConfig keys
- [x] Completed
- **Source:** Generalized from SS-035/SS-059. We don't want per-parameter default values duplicated
  as hardcoded Kotlin lists/maps that must be hand-synced with the Firebase Console value.
- **Goal:** One small loader reads each parameter's version-controlled JSON from
  `composeApp/src/commonMain/composeResources/files/remote_config/<key>.json` and uses it as both the
  RemoteConfig in-app default (`setDefaults`) and the offline/first-launch fallback, so the bundled
  file is the single source of truth for every key. No hardcoded Kotlin default copies.
- **Resolution:** Both current keys (`random_traveler_names` via SS-035, `character_class_stats` via
  SS-059) load through one reusable `loadBundledDefault(path, fallback)` helper in
  `FirebaseRemoteConfigDataSource.initialize()`, reused for `setDefaults` and the offline fallback.
  New keys: drop a `<key>.json` in the bundled `remote_config/` dir, add a `BUNDLED_*_PATH` const, a
  seed fallback, and one `loadBundledDefault` line. The `**Constraint**` below is satisfied per key:
  class stats seed from the factory-derived map, names from a single `FALLBACK_TRAVELER_NAME` const.
  - **Constraint (resolved):** resource reads are `suspend` (`Res.readBytes`), but the data-source
    getters are synchronous and may run before `initialize()` finishes (e.g. `AppViewModel`
    generating a name before config init). Each key seeds a crash-safe pre-init value, so a getter
    that races ahead of the load still returns a sane non-empty default.
- **Deploy side (done):** `remote-config/remoteconfig.template.json` is the single combined Firebase
  template (all parameters, stringified values), generated from the per-key source files by
  `remote-config/build-template.py` and deployed with `firebase deploy --only remoteconfig`. As new
  keys are added, register their source file in that script's `SOURCES` map.
- **Notes:** Keep `remote-config/README.md` in step as keys migrate. Related: SS-035, SS-059, SS-047.

### SS-048 — Die-value enum
- [ ] Not completed
- **Source:** `DiceRoller.kt:16` (`// TODO Enums for values 1 through 6?`)
- **Goal:** Consider a type-safe enum for die faces (1–6) instead of raw Ints.

### SS-049 — Persuade: pass selectedCharmTargetUserId from UI
- [x] Completed
- **Source:** `AbilityAction.kt:34` (`// TODO Persuade: In UI - need to pass in selectedCharmTargetUserId...`)
- **Goal:** Ensure the UI supplies the Charm target id for Persuade. Related: SS-020.
- **Resolution:** The end-to-end wiring was already in place from SS-020; this ticket verified it and
  removed the stale TODO. The Charm flow: `ArenaCombatControl.requiresTarget` marks Charm as needing
  a target so `ArenaCombat` shows the opponent picker; `AbilityControl` passes the chosen
  `targetedOpponentUserId` into `UseAbilityArguments`; `UseAbilityArguments.toDomain` →
  `withSelectedCharmTarget` validates the id against the Domain-vetted `targetableUserIds` and only
  then sets `selectedCharmTargetUserId`; `PersuadeHandler.charm` consumes it as the redirected
  attack's `defenderUserId` (no-ops via `?: return` when unset). Auto-resolve and
  `ComputerOpponentHandler` supply a random charmable target the same way.
- **Verification:** `ArenaScreenViewModelTest` covers both the chosen-target happy path and the
  untargetable (Bard self) rejection.

### SS-050 — Implement KillingMomentum
- [x] Completed
- **Source:** `NormalAttackRollHandler.kt:93` (`// TODO KillingMomentum`)
- **Goal:** Implement the Rogue "Killing Momentum" rule — on a K.O., attack another chosen player.
- **Resolution:** The behavior was already implemented; this ticket verified it and removed the stale
  TODO. On a knockout by a `KillingMomentum` Rogue, `TriggerKillingMomentum` sets the next action to
  `NextCombatAction.Idle` with the attacker as the sole active user, so the Rogue chooses another
  target and takes a fresh normal attack (chaining if it KOs again). It is invoked from all three
  knockout-resolution sites — `NormalAttackRollHandler.resolveKnockout`, `MirrorImageHandler`, and
  `HolyShieldHandler` — and short-circuits before Encore. The consecutive doubles-bonus carry (the
  Rogue keeps the dice, per CLAUDE.md) flows through unchanged.
- **Verification:** `PerformCombatActionUseCaseTest` covers the basic trigger (Rogue acts again in a
  3-player game) and the doubles-bonus preservation across the Killing Momentum follow-up.

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
- [x] Completed
- **Source:** `HolyShieldHandler.kt:88` (`// TODO SpellPotency - what if is splash damage?`)
- **Goal:** Handle the Paladin Holy Shield retaliation when the incoming damage was SpellPotency
  splash damage (who is the "attacker"?).
- **Resolution:** The "attacker" is the Mage in both the direct-hit and splash cases, so Wrath
  already retaliates against `attackerUserId` correctly — no behavior change needed. The
  `setNextAbilityRoll` `defenderUserId` override already promotes a splash-KO'd Paladin to defender
  for the Prayer/Wrath rolls while leaving the Mage as the attacker. Replaced the TODO with a
  docstring documenting the decision, and extended the Spell Potency splash Holy Shield test to
  drive the full splash → Prayer → Wrath chain, asserting the Mage takes the unblockable Wrath
  damage and the Paladin survives with 1 HP.

---

## Dependency injection

### SS-055 — Scope DI definitions instead of single
- [x] Completed
- **Source:** `Modules.kt:42` (`// TODO: Someday, scope these instead of single for everything`)
- **Goal:** Introduce appropriate Koin scopes rather than `single` for everything.
- **Resolution:** Use cases are stateless orchestrators, so `initializeUseCases` now binds
  them with `factoryOf` (a fresh instance per resolution, released with the consuming
  ViewModel) instead of `singleOf`. Data-layer roots (repositories, data sources, providers,
  Firebase clients, the shared IO scope) stay `single` because they hold Firebase references /
  caches and back shared flows; duplicating them would spawn duplicate listeners. ViewModels
  remain `viewModelOf`, scoped to the screen/ViewModelStore.

### SS-056 — Name DI definition
- [x] Completed
- **Source:** `Modules.kt:52` (`// TODO: What to name this?`)
- **Goal:** Give the flagged DI definition a meaningful name.
- **Resolution:** Renamed `initializeParameters` to `initializeExternalDependencies` — it
  provides the framework-owned singletons (Firebase database/firestore/remoteConfig clients and
  the shared `Dispatchers.IO` CoroutineScope) that the rest of the graph is built on, as opposed
  to our own repositories/use-cases/view-models.

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

### SS-090 — First-launch "Continue with Google" must recover an existing account, not error
- [x] Completed
- **Source:** Reported bug. Repro: sign in with Google (links to anonymous account A) → delete the
  app → reopen → first-launch dialog → "Continue with Google" with the same address → got *"That
  account is already linked to another player. Try a different one."* instead of signing in.
- **Cause:** The first-launch dialog's social/email path created a throwaway **anonymous** account
  first, then `linkWithCredential`. On reinstall the Google credential already belonged to account A,
  so the link collided (`FirebaseAuthUserCollisionException` → `CREDENTIAL_IN_USE`). It also leaked an
  orphan anonymous account + profile + display-name reservation on every attempt.
- **Resolution:** Split the two intents. The **dialog now signs in** (recover-or-create) instead of
  linking; **ProfileScreen still links** (attach a provider to existing anonymous data, where a
  collision genuinely is an error). Added `AuthenticationRepository.signInWith{Google,Apple,Email}`
  (Google/Apple via `signInWithCredential`; email via `createUser…` then `signIn…` on collision — which
  also dodges email-enumeration protection), a `SignInWithSocialUseCase`, plus `GetProfileUseCase` and
  `CreateProfileForCurrentUserUseCase` (creates a profile for the just-signed-in user *without*
  re-authenticating anonymously). `AppViewModel` now signs in, then enters the app — recovered
  accounts keep their existing profile (no new profile created); brand-new provider accounts get one
  with the typed name. No anonymous account is created on this path, so the orphan leak is gone.
- **Verification:** Android + iOS compile clean; full unit suite passes, incl. a new regression test
  (returning player recovers their profile without creating a new one) + `SignInWithSocialUseCaseTest`.
  On-device tap-through of the original repro still to be confirmed by the owner.

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

---

## Profile & Auth

### SS-061 — Investigate Firebase Authentication for user profiles
- [x] Completed
- **Goal:** Choose the easiest and most robust way for users to create and own a profile. Evaluate
  Firebase Auth providers — Google Sign-In and phone (mobile) auth are the leading candidates — for a
  KMP (Android + iOS) app, and decide which to support. This unblocks tying profiles/stats to a real
  authenticated identity.
- **Steps:** Survey gitlive `firebase-auth` support across Android/iOS; compare Google Sign-In vs.
  phone auth vs. anonymous-then-link for friction, account recovery, and abuse resistance; consider
  anonymous auth as the first-launch default with later upgrade/link to a permanent provider; document
  a recommendation and the migration path for existing anonymous profiles. Related: SS-036 (signed-in
  user with missing profile), SS-064 (ProfileScreen).
- **Findings:** gitlive `firebase-auth` 2.4.0 exposes the *credential* layer cross-platform
  (`signInWithCredential`, `FirebaseUser.linkWithCredential`, and the `GoogleAuthProvider` /
  `OAuthProvider` / `EmailAuthProvider` credential builders). What is **not** cross-platform is
  *obtaining* a federated token: that needs platform UI (Credential Manager on Android, the
  GoogleSignIn / AuthenticationServices SDKs on iOS). Email/password (and email-link) are fully
  pure-gitlive. Phone auth was rejected: per-SMS cost, SMS-pumping fraud risk, and the heaviest
  setup (Play Integrity / APNs). Email-*link* is awkward post-Firebase-Dynamic-Links-sunset.
- **Decision:** Anonymous-first (unchanged silent default), then **link** a permanent provider onto
  the existing anonymous account via `linkWithCredential`, which preserves the `userId` and every
  profile/stats record already keyed to it — so there is no migration for existing anonymous
  profiles. Chosen providers: **Google + Apple** (Apple is mandatory on iOS once Google is offered)
  **+ Email/password**.
- **Resolution (reusable scaffolding built):**
  - Domain: `SignInMethod`, `LinkAccountResult`, `SocialSignInProvider` (+ `SocialTokenResult`), and
    a reusable `LinkAccountUseCase` (`invoke(method)` for Google/Apple, `withEmail(...)` for email).
    `AuthenticationRepository` gained `linkWithGoogle` / `linkWithApple` / `linkWithEmail`.
  - Data: `FirebaseAuthenticationRepository` implements all three by building the gitlive credential
    and calling `currentUser.linkWithCredential`, mapping collisions/invalid creds to
    `LinkAccountResult`.
  - UI: reusable `SignInMethodsSection` (the three buttons + inline email form + the "signing in is
    optional…" copy, which never mentions the anonymous account) wired into the first-launch
    `SignInNameDialog`; ready to drop into ProfileScreen (SS-064). `AppViewModel` orchestrates
    create-profile-then-link and surfaces `LinkAccountResult` back into the dialog.
  - Email/password linking is **fully functional and unit-tested**. Google/Apple compile end-to-end
    but their native token acquisition is stubbed (`Android/IosSocialSignInProvider` return
    `NotAvailable`) pending the native SDK + Firebase console setup below.
- **Remaining (native + console, follow-ups):** SS-086 (Android Google via Credential Manager),
  SS-087 (iOS Google + Apple via GoogleSignIn / AuthenticationServices), SS-088 (Firebase console:
  enable Google/Apple providers, OAuth client IDs, SHA fingerprints, Apple Services ID). Grep
  `TODO(SS-061)` for the exact insertion points. Also unblocks SS-036 and feeds SS-064.

### SS-062 — Profile deletion in Firestore
- [x] Completed
- **Source:** `FirebaseProfileRepository.kt` (profile data layer).
- **Goal:** Add the ability to delete a user's profile from Firestore, with the corresponding
  domain use case and repository method.
- **Steps:** Add a `DeleteProfileUseCase` + `ProfileRepository.deleteProfile(...)`; ensure related
  data is cleaned up (the unique display-name reservation from SS-039, any stats records, the auth
  account if appropriate per SS-061); surface it from the ProfileScreen (SS-064) behind a
  confirmation.
- **Resolution:** Made `FirebaseProfileRepository.deleteProfile` atomic via `runTransaction`: it
  reads the profile's reserved display name first (Firestore requires reads before writes), deletes
  the profile document (which carries the nested `stats`, so stats are cleaned up with it), and
  releases the SS-039 display-name reservation in the same transaction. Added `DeleteProfileUseCase`
  (resolves the signed-in `userId` via `AuthenticationRepository`, no-ops when unauthenticated) and
  registered it in DI. Surfaced it in `ProfileScreen` as a "Delete Profile" outlined button gated
  behind an `AlertDialog` confirmation; on confirm, `AppViewModel.onDeleteProfile` cancels the
  profile-collection job, runs the use case, then **silently creates a fresh profile** with a random
  traveler name (reusing `createProfileUseCase`) rather than forcing the sign-in dialog — the
  anonymous identity persists, so this is a smoother UX. The sign-in dialog remains only as a
  fallback if that silent creation fails (e.g. a rare random-name collision).
- **Out of scope:** Deleting/converting the Firebase Auth account and a deliberate
  profile-creation / sign-in UI are left to SS-061 — auth is still anonymous, so the `userId`
  persists and a new profile is created immediately after deletion.
- **Verification:** `:composeApp:compileDebugKotlinAndroid` compiles clean; new
  `DeleteProfileUseCaseTest` + an `AppViewModel` deletion test pass.

### SS-063 — App Drawer cleanup
- [x] Completed
- **Source:** `SettingsDrawer.kt` (also flagged by `// TODO(SS-037): What to do with this?`).
- **Goal:** The drawer currently holds ad-hoc items (a raw Display Name field, Tavern/Arena links).
  Replace it with a deliberate set of entries. The name-change capability moves out of the drawer and
  into the ProfileScreen (SS-064); the drawer should instead link to Profile, Game Rules (SS-067),
  Leaderboard (SS-065), etc.
- **Steps:** Decide the final drawer entry list; remove the inline display-name editor; add
  navigation entries to the new screens; tidy styling. Related: SS-037, SS-064.
- **Resolution:** Rewrote `SettingsDrawer` to a deliberate list of navigation entries — **Profile**,
  **Leaderboard**, **Game Rules** — dropping the inline display-name editor and the ad-hoc
  Tavern/Arena links (clears the `TODO(SS-037)`). Added `Route.ProfileScreen`/`LeaderboardScreen`/
  `GameRulesScreen` (registered in `navigationRoutesConfiguration`) and wired them into
  `NavigationDisplay`, which now threads `displayName`/`onUpdateDisplayName` from the host. Created
  placeholder screens: `ProfileScreen` (carries the moved display-name edit flow so the capability
  isn't lost; `TODO(SS-064)`), `LeaderboardScreen` (`TODO(SS-065)`), `GameRulesScreen`
  (`TODO(SS-067)`). Drawer entries push onto the back stack so the system back returns to the prior
  screen.
- **Verification:** `:composeApp:compileDebugKotlinAndroid` compiles clean.

### SS-064 — ProfileScreen
- [x] Completed
- **Goal:** Add a ProfileScreen, accessed from the app drawer (SS-063). It contains the name-change
  capability (moved out of `SettingsDrawer`), profile deletion (SS-062), and, once game-stat handling
  exists, the user's own game statistics.
- **Steps:** Create the route + screen + ViewModel; move the display-name edit flow here (reusing
  `UpdateProfileDisplayNameUseCase`); wire profile deletion (SS-062) behind a confirmation; leave a
  placeholder for per-user stats. Lay the screen out in a `LazyColumn` (it is currently a plain
  `Column` and will grow past one screen once stats land) and replace the placeholder copy — the
  section labels, button text, and the bare `"Profile"`/`"Display Name"`/`"Save"`/`"Cancel"`
  strings are all stand-ins and need finalized, localized wording moved into `strings.xml`. The
  reusable `SignInMethodsSection` + `LinkAccountUseCase` (SS-061) is **already wired** into the
  placeholder ProfileScreen (links the current account in place via `AppViewModel.onLinkProfileWith*`,
  status surfaced through `profileLinkResultFlow`); carry it over when promoting the screen to its
  own route + ViewModel. Related: SS-035, SS-039, SS-061, SS-065.
- **Resolution:** Promoted ProfileScreen to its own route-scoped `ProfileScreenViewModel`
  (`@KoinViewModel` + manual `viewModelOf(::ProfileScreenViewModel)` in `Modules.kt`, matching the
  App/Arena/Tavern convention). The VM owns everything the screen needs, so the whole
  `displayName` / `onUpdateDisplayName` / `onDeleteProfile` / `onLinkProfileWith*` /
  `profileLinkResultFlow` thread was **removed** from `AppViewModel` → `SorcerersAndSpiritsNavigationHost`
  → `NavigationDisplay`; `ProfileScreen()` now resolves its VM via `koinViewModel()` and only takes an
  `onProfileDeleted` callback (the back-stack pop). The VM:
  - observes `GetProfileFlowUseCase` for the display name, exposing `UiState<ProfileState>`;
  - edits the name via `UpdateProfileDisplayNameUseCase`;
  - on delete, runs `DeleteProfileUseCase` then silently re-provisions a fresh anonymous profile
    (`CreateProfileUseCase` + `GetRandomTravelerNameUseCase`) and emits `ProfileEvent.ProfileDeleted`
    so the screen pops back — `AppViewModel`'s live profile flow reactively picks up the new name (no
    app-level reload needed). The delete-and-recreate orchestration moved off `AppViewModel` entirely;
  - links the current account via `LinkAccountUseCase`, surfacing the result on a separate
    `linkResultFlow` (kept off `ProfileState` so a profile re-emission can't wipe the feedback).
  Rebuilt the screen as a `LazyColumn` with a stats-placeholder card (`TODO(SS-064)` → SS-072/074) and
  moved all copy into `strings.xml` (`profile_*`). Migrated the deletion + link tests out of
  `AppViewModelTest` into a new `ProfileScreenViewModelTest`; trimmed the now-unused `AppViewModel`
  deps. **Also fixed in passing:** this project's KSP-generated Koin module isn't included
  (registration is manual `viewModelOf`), and `GameRulesScreenViewModel` (SS-067) was annotated
  `@KoinViewModel` but missing from the list — it would have crashed when opened from the drawer.
  Registered it too.
- **Verification:** `:composeApp:compileDebugKotlinAndroid` compiles clean; full
  `:composeApp:testDebugUnitTest` suite green (new `ProfileScreenViewModelTest` + trimmed
  `AppViewModelTest`).

### SS-086 — Android: native Google Sign-In token acquisition
- [x] Completed (dormant until SS-088)
- **Source:** SS-061 follow-up. `AndroidSocialSignInProvider` previously returned `NotAvailable`.
- **Goal:** Implement the Google account picker on Android via `androidx.credentials`
  (`CredentialManager` + `GetSignInWithGoogleOption` using the Firebase web client ID) and return
  `SocialTokenResult.Google(idToken)`. `LinkAccountUseCase` + `linkWithGoogle` already consume it.
- **Steps:** Add the `androidx.credentials` + `googleid` deps; thread an Activity/context into the
  provider; map cancel/error to `SocialTokenResult.Cancelled`/`Error`. Depends on SS-088. Grep
  `TODO(SS-061)` in `AndroidSocialSignInProvider`.
- **Resolution:** Added `androidx.credentials` + `credentials-play-services-auth` (1.5.0) and
  `googleid` (1.1.1). `AndroidSocialSignInProvider` now drives the Credential Manager
  `GetSignInWithGoogleOption` flow: parses the returned `GoogleIdTokenCredential` into
  `SocialTokenResult.Google(idToken)`, maps `GetCredentialCancellationException` /
  `NoCredentialException` → `Cancelled` and parse/other `GetCredentialException` → `Error`. The
  account picker needs an Activity, so a `CurrentActivityHolder` (Koin singleton) is populated/cleared
  across `MainActivity`'s `onCreate`/`onDestroy` and injected into the provider. The web client ID is
  read **reflectively** from the google-services-generated `default_web_client_id` string resource,
  so it stays `NotAvailable` (no behaviour change) until SS-088 adds the OAuth web client to
  `google-services.json` — at which point Google sign-in **flips on with no code change**. Apple on
  Android stays `NotAvailable` (needs a web OAuth flow, not Credential Manager).
- **Verification:** SS-088 done for Android — `google-services.json` now carries the WEB (type 3)
  oauth client, the build generates `default_web_client_id`
  (`848564605750-agimo9…apps.googleusercontent.com`), which is exactly what the provider reads
  reflectively, and `:composeApp:assembleDebug` builds clean. **Google sign-in is now live (no longer
  dormant).** Remaining: an on-device tap-through with a Google account, and release/Play SHA
  fingerprints before a store build.

### SS-087 — iOS: native Google + Apple Sign-In token acquisition
- [x] Completed (code) — needs the manual Xcode + SS-088 steps below to go live
- **Source:** SS-061 follow-up. `IosSocialSignInProvider` previously returned `NotAvailable`.
- **Goal:** Implement Google (GoogleSignIn SDK → `SocialTokenResult.Google(idToken, accessToken)`)
  and Apple (`AuthenticationServices` / `ASAuthorizationAppleIDProvider` →
  `SocialTokenResult.Apple(idToken, rawNonce)` with a SHA256-hashed nonce) on iOS.
- **Resolution (code):** Native sign-in lives in Swift via **Swift Package Manager** (no CocoaPods —
  consistent with the existing `firebase-ios-sdk` SPM setup):
  - Shared `iosMain` defines `IosSocialSignInBridge` (+ `IosGoogleSignInResult` / `IosAppleSignInResult`
    DTOs — objects, not multi-arg closures, so the Swift side gets plain `Bool`s rather than boxed
    `KotlinBoolean`). `IosSocialSignInProvider` delegates Google/Apple to the bridge via
    `suspendCancellableCoroutine`, mapping to `SocialTokenResult`. The bridge is injected from Swift
    through `initializeKoin(socialSignInBridge:)` and resolved in `PlatformModule.ios.kt`.
  - `iosApp/iosApp/SocialSignInBridge.swift` implements it: GoogleSignIn (reads the Firebase
    `clientID`, returns id + access token) and Apple (`ASAuthorizationController` with a CryptoKit
    SHA-256-hashed nonce, returning the identity token + raw nonce). `iOSApp.swift` builds the bridge
    and adds the `GIDSignIn.handle(url)` callback. The new `.swift` file is picked up automatically
    (Xcode 16 synchronized file group).
  - Domain change rippled from this: `SocialTokenResult.Google` / `linkWithGoogle` now carry an
    `accessToken` (required by iOS `credentialWithIDToken:accessToken:`); Android passes `null`.
  - Like Android, both paths stay dormant until configured: Google no-ops (treated as cancel) until
    the Firebase `clientID` exists, and Apple linking needs the console provider — both SS-088.
- **Remaining — manual Xcode (GUI, not hand-editable safely):**
  1. **Add the GoogleSignIn SPM package:** Xcode → File → Add Package Dependencies →
     `https://github.com/google/GoogleSignIn-iOS` → add the **GoogleSignIn** product to the `iosApp`
     target (mirrors how `firebase-ios-sdk` is already added).
  2. **Add the "Sign in with Apple" capability** to the `iosApp` target (Signing & Capabilities tab;
     creates the entitlement).
  3. After SS-088 supplies a Google-enabled `GoogleService-Info.plist`, add the **`REVERSED_CLIENT_ID`**
     as a URL scheme in the iOS target (Info → URL Types).
- **Verification:** `:composeApp:compileKotlinIosSimulatorArm64` + the Android target compile clean;
  full unit suite passes (incl. the updated `LinkAccountUseCaseTest`). The Swift bridge and full
  iOS app build are **not verifiable here** — they need Xcode with the SPM package, the Apple
  capability, SS-088, and a device/simulator with a Google account.

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

---

## Game Stats & Leaderboard

Game statistics are recorded on each user's Firestore profile and will be a large part of the game —
directly driving the Leaderboard and other features. The work is staged deliberately: **first** build
the generic infrastructure for persisting stats to a profile (SS-021), keeping it stat-agnostic;
**then** decide which concrete stats to record (SS-072) and wire them into match outcomes (SS-071);
and finally surface them (SS-064 ProfileScreen, SS-065 Leaderboard).

### SS-021 — Infrastructure for saving game stats to user profiles (Firestore)
- [x] Completed
- **Goal:** Establish the generic mechanism for persisting game statistics onto a user's Firestore
  profile, independent of which specific stats are recorded. This is the foundation the rest of the
  stats work builds on (forfeit/finished updates SS-071, the stats schema SS-072, the Leaderboard
  SS-065), so keep it stat-agnostic — we just need a way to save stats before deciding what they are.
- **Resolution:** Added a typed `ProfileStats` data class (`wins`/`losses` `Long`s for now, growing
  to more fields and nested data classes under SS-072) to the `Profile` domain model and a mirroring
  `ProfileStatsEntity` on `ProfileEntity` (serialization defaults `ProfileStatsEntity()`, so in-flight
  profiles still deserialize), persisted under a nested `stats` map field on the profile document
  (`FireStoreKeys.STATS`).
  - **Write path:** `ProfileRepository.incrementStats(userId, delta: ProfileStats)`, implemented in
    `FirebaseProfileRepository`. The data layer maps the delta to per-field increments
    (`ProfileStats.toStatIncrements()`, skipping zero fields) and writes them via
    `updateFields { FieldPath(STATS, key) to FieldValue.increment(amount) }`. Using per-field
    `FieldValue.increment` (a) updates only the touched counters so concurrent match writes don't
    clobber each other, and (b) auto-creates the nested `stats.<key>` field from 0, so no
    read-modify-write or pre-seeding is needed. An all-zero delta short-circuits.
  - **Domain use case:** `UpdateProfileStatsUseCase(delta: ProfileStats)` resolves the signed-in user
    via `AuthenticationRepository.getUserId()` (no-ops if absent or `delta.isEmpty`) and forwards to
    the repository. Registered in DI (`Modules.kt`).
  - **Extensible by design:** `ProfileStats` doubles as the stored totals and the per-write delta, so
    SS-072 can add fields (and nested data classes) and SS-071 can call this on match outcomes with
    only a new line in `toStatIncrements()`/`isEmpty`. Read totals are `Long` (Firestore 64-bit
    integers); per-write deltas are narrowed to `Int` (what gitlive's `FieldValue.increment` accepts).
- **Verification:** `composeApp` Android debug compiles clean; new `UpdateProfileStatsUseCaseTest`
  (forwards increments / no signed-in user / empty increments) passes, and the Profile/Tavern/
  AppViewModel suites remain green.
- **Notes:** Depends on the authenticated profile identity (Related: SS-061). Consumers: SS-071,
  SS-072. Surfaced by: SS-064 (ProfileScreen), SS-065 (Leaderboard).

### SS-073 — "Gauntlet" concept: group 3 consecutive matches as one unit
- [x] Completed
- **Goal:** Introduce a new game-term, **Gauntlet**, sitting above `Match` in the hierarchy
  (`Turn` → `Round` → `Match` → `Gauntlet`). A Gauntlet is a fixed run of `MATCHES_PER_GAUNTLET`
  (= 3) consecutive matches in the same Arena. Because outcomes are highly random and matches are
  quick, 3 is the minimum unit of play before stats are recorded — a Gauntlet is what gets a single
  stats write (SS-072/SS-071). This is the prerequisite that gives the stats tickets a real unit to
  attribute wins/losses to.
- **Behavior:**
  - Track the current match number within the Gauntlet (1-based, 1..3) on the shared
    `ArenaMatchEntity` so every client sees the same progress; it persists across the
    intra-match state transitions and increments when a match resets to the next.
  - When a match finishes, surface Gauntlet progress (Match 1/3, 2/3, 3/3). For non-final matches the
    decision dialog offers only **Continue** (no leave option — players can still forfeit via back).
  - Only on the final match (3/3) are players asked **Go Again** (start a fresh Gauntlet, resetting
    the counter to 1) or **Leave**.
- **Decisions (this pass):** Count is a fixed constant (`MATCHES_PER_GAUNTLET = 3`); making it
  configurable per Arena — and a rating multiplier for longer Gauntlets — is deferred.
- **Steps:** Add the `MATCHES_PER_GAUNTLET` constant (domain) + `matchNumberInGauntlet` on
  `ArenaMatch`/`ArenaMatchEntity` (default 1, threaded through the cross-variant transitions:
  ConnectToArena, ChooseCharacterClass, FirebaseArenaRepository, ExitArena; incremented/wrapped in
  StayForAnotherMatch). Expose `GauntletProgress` on `ArenaScreenState`; gate the match-finished
  dialog/content on `isFinalMatch`.
- **Notes:** Prerequisite for SS-071 (forfeit vs finished — stats per completed Gauntlet) and SS-072
  (stat schema). The new term must be explained on the GameRulesScreen (SS-067) alongside
  Turn/Round/Match. Related: SS-021, SS-071, SS-072, SS-067.

### SS-065 — LeaderboardScreen
- [x] Completed
- **Goal:** Once game stats are recorded on user profiles (SS-021 infrastructure + SS-072 schema),
  add a screen showing the top players. Accessible from the drawer (SS-063).
- **Resolution:** Built the route-driven screen on top of the persisted stats. Ranks by overall
  **Gauntlets won** — the unit a stats write is attributed to (SS-073) — so the leaderboard reflects
  completed runs rather than individual matches.
  - **Data layer:** `ProfileRepository.getTopProfiles(limit)` + `FirebaseProfileRepository` impl
    queries the `Profiles` collection with a single `orderBy(stats.overall.gauntletsWon,
    DESCENDING).limit(limit)` (single-field order needs no composite index) and maps the documents
    through the existing `ProfileEntity.toDomain()`. The dotted field path is a compile-time const
    built from the existing `FireStoreKeys`.
  - **Domain:** `GetLeaderboardUseCase` reads the top profiles (size constant = 50), assigns 1-based
    ranks by order, and tags the signed-in user's own row (`isCurrentUser`, via
    `AuthenticationRepository.getUserId()`) so the screen can highlight it. New `LeaderboardEntry`
    domain model (rank, displayName, gauntletsWon, matchesWon, isCurrentUser).
  - **UI:** `LeaderboardScreenViewModel` (`UiState.Loading/Loaded/Error` with `onRetry`) +
    `LeaderboardState`/`LeaderboardRowState`. `LeaderboardScreen` renders a ranked `LazyColumn`
    (rank, name + "You" badge for the current user, match-wins subtitle, Gauntlet-wins headline),
    an empty state, and the shared `ErrorContent`. Wired in DI (`Modules.kt`); the drawer entry
    (SS-063) and `Route.LeaderboardScreen` already existed.
- **Verification:** `composeApp` Android debug + unit-test sources compile clean; new
  `GetLeaderboardUseCaseTest` (3 cases) and `LeaderboardScreenViewModelTest` (3 cases) pass.
- **Notes:** Depends on stats existing/being persisted (Related: SS-021, SS-072; SS-071 forfeit/
  finished stats update). Future work: pagination beyond the top 50, alternate ranking metrics, and
  jumping to the current user's row when they fall outside the top slice.

### SS-071 — Forfeit vs finished leave + stats update
- [x] Completed
- **Source:** `ArenaScreenViewModel.kt:195` (`// TODO(SS-071) Arena: Need a leave that updates stats with a forfeit...`)
- **Goal:** Distinguish a forfeit-leave from a leave after the match finished, and update stats only
  when the user actually leaves (possibly after multiple matches). First concrete consumer of the
  SS-021 stats infrastructure — the forfeit/finished distinction matters so the stats record a
  forfeit vs a completed-match outcome.
- **Resolution:** Added `ArenaSessionStats`, a mutable accumulator the `ArenaScreenViewModel` holds
  for the whole Arena session (across multiple matches/Gauntlets). It folds per-write `ProfileStats`
  deltas and is flushed once, via `UpdateProfileStatsUseCase`, on every leave path
  (`onConfirmLeaveArena`, the waiting-room timeout, and the match-removed flow). `consumeDelta()`
  resets after a flush so re-entrant leaves never double-write.
  - **Forfeit vs finished:** a match that reaches `ArenaMatch.Finished` is scored win/loss by
    `onMatchUpdate` (user won iff their player is still conscious). Leaving with a match still in
    progress is scored by `onLeave`: a **loss** if the player was knocked out, otherwise a
    **forfeit**. Leaving after the final match has finished records nothing new.
  - **New stat fields:** added `matchesForfeited` + `gauntletsForfeited` to `GameStats` /
    `GameStatsEntity` (plus `FireStoreKeys` constants and `toFieldIncrements`) following the SS-072
    extension recipe, so a forfeit is distinct from a loss in the persisted data. Added `plus`
    operators on `GameStats`/`ProfileStats` for accumulation.
  - **Gauntlet outcome (chosen semantics):** a Gauntlet is **won only on a clean 3/3 sweep**, else
    lost; it is credited to a class only when the player used that same class for all three matches
    (mixed-class runs credit `overall` only — keeps per-class `gauntletsWon` meaningful for SS-074).
    Joining mid-Gauntlet (first observed match ≠ 1) suppresses Gauntlet-outcome writes. Per-match
    outcomes and `timesSelected` are always credited to the class played that match.
  - **Scope:** wired the match/Gauntlet outcomes (played/won/lost/forfeited) + `timesSelected`. The
    per-action combat counters (`knockoutsDealt`/`attacksMade`/`abilitiesUsed`) are deferred to a
    follow-up.
- **Verification:** `composeApp` Android debug unit-test sources compile clean; new
  `ArenaSessionStatsTest` (11 cases) and the updated `ArenaScreenViewModelTest` (46 cases) pass.
- **Steps:** ~~Define the two leave paths; wire the stats update (via SS-021's
  `UpdateProfileStatsUseCase`) to the correct one.~~ Done. Related: SS-021, SS-045, SS-022.

### SS-072 — Define which game stats to record
- [x] Completed
- **Goal:** Decide the concrete set of game statistics to persist per profile (e.g. matches played,
  wins, losses, forfeits, K.O.s dealt, per-class records, etc.). Deferred until the SS-021
  infrastructure exists — design the stats schema once the mechanism for saving them is in place,
  since the stats will directly influence the Leaderboard.
- **Resolution:** Defined the stats schema as an **extensible, scope-uniform** structure rather than
  an exhaustive enumeration — the priority is that a new stat (or a future achievement) can be added
  with minimal edits, since many more are anticipated (per SS-074).
  - **`GameStats` (domain):** one reusable counter bundle reused for every scope, with per-match and
    per-gauntlet pairs plus shared counters: `matchesPlayed/Won/Lost`, `gauntletsPlayed/Won/Lost`,
    `knockoutsDealt`, `attacksMade`, `abilitiesUsed`, `timesSelected`. `isEmpty` is derived from
    equality with an all-zero instance, so adding a counter never touches it.
  - **`ProfileStats` (domain):** now `overall: GameStats` + `perClass: Map<CharacterClassType,
    GameStats>`. The Gauntlet (SS-073) is the unit a stats write is attributed to. Per-class records
    are rich enough to derive class milestones (e.g. "win a Gauntlet with every class") without
    enumerating achievements here. Replaces the old flat `wins`/`losses` fields.
  - **Data layer:** `ProfileStatsEntity`/`GameStatsEntity` mirror the domain shape (serialization
    defaults keep in-flight profiles deserializable; unknown `perClass` keys are dropped on read).
    `ProfileStats.toStatIncrements()` flattens non-zero counters into nested Firestore field paths
    (`stats.overall.<stat>`, `stats.perClass.<ClassName>.<stat>`), and
    `FirebaseProfileRepository.incrementStats` writes each via a spread `FieldPath(*segments)` +
    `FieldValue.increment` — so concurrent writes only touch their own counters and nested fields
    auto-create from 0. **Adding a stat = add a field to `GameStats` + `GameStatsEntity`, one line in
    `toFieldIncrements()`, the two entity mappings, and a `FireStoreKeys` constant.**
- **Verification:** `composeApp` Android debug + unit-test sources compile clean; new
  `ProfileStatsMappingTest` (5 cases: nested flattening, zero-omission, empty, entity round-trip,
  unknown-class drop) and the updated `UpdateProfileStatsUseCaseTest` (3 cases) pass.
- **Steps:** ~~Enumerate candidate stats and how each is derived from match outcomes; finalize the
  Firestore field schema on top of SS-021's generic payload; define how they feed the Leaderboard
  (SS-065).~~ Done. Match-outcome derivation/wiring of deltas is SS-071. Related: SS-021, SS-065,
  SS-064, SS-073, SS-074.

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

### SS-067 — GameRulesScreen
- [x] Completed
- **Goal:** A screen, accessible from the drawer (SS-063), where users can read the game rules and
  class abilities.
- **Decision (content source):** Static inline copy for now, consistent with how SS-079 shipped the
  class wheel's ability/stat literals. SS-081 owns moving the per-class ability/stat copy to
  RemoteConfig (long variant) — when it lands, this screen consumes the long variant. Core combat
  rules + the game-terms hierarchy are stable game definitions and stay static.
- **Resolution:** Replaced the placeholder `GameRulesScreen` with a scrollable `LazyColumn` of three
  sections: **How Combat Works** (2–8 players, the 2d6 Armor-then-Health attack sequence, knockout/
  skip/end conditions, the doubles +1 consecutive-roll rule), **Game Terms** (Turn → Round → Match →
  Gauntlet, the Gauntlet definition from SS-073), and **Classes & Abilities** — one `Card` per class
  driven by `CharacterClass.getAllClasses().map { it.toState() }`, showing each class's Health/Armor/
  Speed and its offensive + defensive ability name and description.
  - **De-duplicated copy:** the per-class ability name/description functions and the Health/Armor/
    Speed stat descriptions were previously private literals in `CharacterClassWheel.kt`. Extracted
    them into a shared `ui/common/characterclasses/CharacterClassCopy.kt` (`internal`), now consumed
    by both the wheel and the rules screen — single source for the class copy (and the single place
    SS-081 will point at RemoteConfig). Tagged with a `TODO(SS-081)`.
  - No ViewModel/route changes needed — the screen is already registered as `Route.GameRulesScreen`
    and wired from `SettingsDrawer` → `NavigationDisplay` (SS-063); the static class list keeps it
    ViewModel-free like the sibling placeholder screens.
- **Verification:** `:composeApp:compileDebugKotlinAndroid` compiles clean.
- **Deferred:** rules-chatbot link (SS-066) when/if it exists; moving ability/stat copy to
  RemoteConfig (SS-081).

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

### SS-069 — Shake-to-roll dice UX
- [x] Completed — gesture mechanics done; final dice visuals deferred to SS-077/SS-078.
- **Done:** `ShakeDetector` interface (`domain/sensor`) with Android `SensorManager` + iOS
  `CMMotionManager` actuals, wired via `platformModule()`. `ArenaCombatControl.diceCount` derives 1
  vs. 2 dice per step (non-roll choices Impersonate/Charm stay plain buttons). `HoldToRollDice`
  composable rolls three ways: press-and-hold + shake (any duration) then release; double-tap the
  dice; or press the Roll button — all invoke the existing `onAttack`/`onUseAbility`. The hold
  gesture consumes its touch events (`awaitEachGesture` + `down.consume()`) so the surrounding combat
  `LazyColumn` can't steal the press and cancel the hold. A `Haptics` interface (`domain/haptics`,
  Android `Vibrator` + iOS `UIImpactFeedbackGenerator` actuals, wired via `platformModule()`) buzzes
  MEDIUM on each detected shake and HEAVY on the roll. Double-tap / Roll button keep turns playable
  on emulators / sensorless devices; final UX placement is a SS-078 product decision. Covered by
  `CombatControlDiceCountTest`.
- **Goal:** Investigate a dice-rolling interaction where the user taps and holds a pair of dice (or a
  single die, depending on what the current step requires), shakes the phone to simulate rolling, and
  on release the existing ViewModel roll handler is invoked.
- **Steps:** Determine which die count the current combat step expects (1 vs. 2 dice) to drive what's
  held; add accelerometer/shake detection (platform `expect`/`actual` for Android + iOS); wire the
  release gesture to the existing roll entry point; design the press/shake/release visual feedback.
- **Notes:** Scoped to the *interaction* (gesture detection + roll wiring). The dice's visual
  representation is split out: how the dice are rendered/animated is SS-077, and where they live on
  screen + final styling is SS-078. Related: SS-077, SS-078.

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

## Release & versioning

### SS-070 — Investigate app version control & force-upgrade
- [x] Completed
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
- **Outcome:** RemoteConfig is the version source — `minimum_supported_version` (below it = hard
  force-upgrade block; this is the lever to bump when a breaking RTDB-schema change ships) and
  `latest_version` (below it but at/above the minimum = dismissible "update available" nudge). Both
  default to `0` so a missing/failed config never gates anyone. Comparison is an integer build-code
  compare (`CheckAppVersionUseCase`) using `CurrentAppVersionProvider` (Android `versionCode` /
  iOS `CFBundleVersion`) vs. `SupportedAppVersionProvider` (RemoteConfig). Runs in `AppViewModel`
  after config fetch/activate; `AppUpgradeDialog` overlays every UI state and deep-links the store
  via `LocalUriHandler`.
- **Follow-up:** iOS `appStoreUrl()` uses a placeholder App Store id (app unpublished) — swap in the
  real id at publish time (TODO tagged SS-070 in `AppStoreUrl.ios.kt`).
