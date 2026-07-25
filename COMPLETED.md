# Completed Backlog

Archive of finished tickets — one line each (ID, title, goal). Full resolution detail,
rationale, and verification live in the git history (commit + PR per ticket); search the ID.
Active work is in [BACKLOG.md](BACKLOG.md).

## Tests

- **SS-001** — Fix Tavern repository tests: Restore the Tavern repository test suite to a passing,
  meaningful state.
- **SS-002** — Repair broken ArenaScreenViewModel test: Fix the broken/abandoned test around line
  342 so it asserts correct behavior.
- **SS-003** — Clarify Charm test comment & assertion: Resolve the confusing comment about the
  Charmed target and verify the assertion matches the intended Persuade/Charm rule from CLAUDE.md.

## Tavern UI

- **SS-239** — ChallengeButton slides up from off-screen, then back down to dismiss: Drove the
  `ChallengeButton` `AnimatedVisibility` in `TheBar` with `slideInVertically` (enter from below) /
  `slideOutVertically` (exit downward); label extracted to a private `const val`.
- **SS-004** — Decide source of class list in TavernState: Decide whether the class list should come
  from RemoteConfig or stay static (`CharacterClass.getAllClasses()`). Related: SS-047.
- **SS-005** — Move navigateToArena out of TavernState: Reconsider whether `navigateToArena` belongs
  in state vs. an event/effect channel.
- **SS-006** — Consolidate navigate-to-arena callback at App level: Decide whether the
  `onNavigateToArena` wiring should live at the App level instead of per-screen.
- **SS-007** — Real error UI for error states: Replace placeholder retry/empty handling with proper
  error UI shared across screens.
- **SS-008** — Reusable Tavern preview state & previews: Build reusable example/preview state for
  Tavern composables and add/refresh previews.
- **SS-009** — Improve TheBar: Clarify and improve `TheBar` (scope is vague — define concretely
  while working on it).
- **SS-010** — Reword challenge-list UI copy in TheBar: Update copy to clearly communicate "other
  players to challenge."
- **SS-011** — New challenge icon in TheBar: Replace the placeholder icon with the intended
  challenge icon.
- **SS-012** — Update TheBar styling: Finalize styling for `TheBar`.
- **SS-124** — Search for a traveler to challenge in TheBar: Added an `OutlinedTextField` search bar
  pinned above the `BarFolk` list in `TheBar`, bound to a `searchQuery` state; `otherTravelers` is
  filtered by case-insensitive `displayName` substring match (computer travelers injected in
  `TavernData.toState` filter the same way). Selection stays derived from the full `otherTravelers`
  list, so a traveler that is picked and then filtered out of view remains in the outgoing
  challenge. The in-tavern count keeps showing the unfiltered total, and an active query that
  matches nobody shows a `tavern_search_no_matches` empty state (distinct from an empty Tavern).
  Pure client-side filter over the already-streamed list; no data-layer change (cross-shard search
  deferred to SS-126).
- **SS-121** — Show the incoming-challenge dialog on every drawer screen, not just the Tavern:
  Challenge handling was owned by the route-scoped `TavernScreenViewModel` and the dialog composed
  inside `TavernScreen`, so a challenge arriving while the user was on Profile/Leaderboard/Game
  Rules (stacked above the Tavern) was received but never shown. Lifted the incoming-challenge
  lifecycle into a host-scoped `ChallengeViewModel` (obtained via `koinViewModel()` in
  `SorcerersAndSpirits`, surviving drawer navigation) that owns the `ChallengeHandler`, countdown
  timer, accept/decline, dialog visibility, and a navigate-to-arena event; added Domain
  `GetActiveChallengeFlowUseCase` (active challenge + userId, no travelers subscription); dropped
  `challenge` from `TavernData`/`GetTavernDataFlowUseCase`/`TavernState` so the Tavern keeps only
  the outgoing send (human → write challenge; computer → straight to arena). `ChallengeDialog` now
  renders at the host (a Dialog overlays any route) with challenge state + actions reaching the
  Tavern's swords re-open handle via composition locals. Challenge round-trip tests migrated to
  `GetActiveChallengeFlowUseCaseTest`.

## Arena UI

- **SS-112** — Persist dice on screen; re-roll animation on any player's roll: Lifted the dice out
  of the per-turn roll controls into a persistent `DiceTray` placed in `ArenaCombat` outside the
  turn-keyed `AnimatedContent`, so it stays on screen the whole match. The tray's faces come from
  `latestRolledDice` and `Die3d` now tumbles-and-settles on any value change (any player's roll), in
  addition to spinning while held. The roll controls keep the hold/shake/release gesture (only
  present on the user's turn) but no longer draw dice; they report their hold up via
  `onRollHoldChange` so the tray's dice spin while held and settle on release. Builds on
  SS-077/SS-113.
- **SS-139** — SharedTransitionLayout foundation for the Arena: Already satisfied by the
  pre-existing `ui/common/SharedTransitionProvider.kt` (`SharedTransitionProvider` +
  `Modifier.sharedElement(key)` / `sharedBounds(key)` over a static composition local), which is
  already wired into `ArenaCombat` — combat content is wrapped in a `SharedTransitionLayout` +
  `AnimatedContent(targetState = activeUserIds)` and opponents already morph via
  `sharedBounds(key = opponent.userId)`. No new code needed; SS-140/SS-141 reuse this with a
  matching `userId` key. (Confirmed available cross-platform in Compose Multiplatform 1.10.3.)
- **SS-137** — Centralized per-class color source (color groups): Lifted per-class colors into one
  `ClassColors.kt` keyed by `CharacterClassType` — `classColorGroup` (Red/Yellow/Green/Blue
  families), `classGradient` (the two-stop light/dark fill used by the head icon, grouped), and
  `classColor` (the single per-class shade used by the wheel spokes, preserving the existing
  values). Repointed `ClassHeadIcon` (was a local `classCircleColors`) and
  `CharacterClassWheel.spokeColor()` at it so both tint from one place. Values are provisional;
  final Figma colors land via SS-084 (single edit point).
- **SS-143** — Opponent head-icon row (ClassIcons): Filled the new combat layout's top-half
  `FlowRow` (`NewArenaCombat.ClassIcons`) with one `ClassHeadIcon` (SS-136) per player who is
  neither the user nor the current target — those two are the bottom-half full avatars. The list is
  a derived `headIconPlayers` val on `ArenaState.Combat` (computed at map time alongside
  `combatControls`/`lockedTargetUserId`), `playerMap` minus `userPlayerId` minus the target; the
  target is the Domain-pinned `lockedTargetUserId`/`autoTargetedOpponentUserId` until SS-144 hoists
  interactive selection. Knocked-out players stay visible (mirror SS-123); SS-152 grays/collapses
  them. Related: SS-136, SS-144, SS-152, SS-142.
- **SS-144** — Target selection: head icon ↔ targeted avatar shared transition: `NewArenaCombat` now
  wraps its content in `SharedTransitionLayout` + `AnimatedContent` keyed on the effective target.
  Tapping a conscious opponent head icon (on the user's turn, before a roll pins a defender) sets a
  Composable-hoisted `selectedOpponentUserId`; `resolveTargetUserId` folds it with the Domain-pinned
  defender and lone auto-target (SS-101 order) into the one effective target. Each head icon and
  each bottom-half avatar carries a `userId`-keyed `sharedBounds`, so a target change animates the
  old target collapsing to a head icon while the newly tapped icon morphs into the right-hand avatar
  slot (a swap; the first selection just fills the empty slot). The `AnimatedContent` arrangement is
  derived from its own lambda target value via `headIconPlayersFor`/`targetPlayerFor` so
  outgoing/incoming compositions differ. Dropped the now-unused `ColumnScope` receiver. Note:
  DiceHome/Buttons sit inside the AnimatedContent and would crossfade on a target change once they
  render real content — SS-149/SS-151 should hoist them out. Related: SS-139, SS-143, SS-145,
  SS-142.
- **SS-145** — Bottom full-size class avatars (ClassAvatar): `NewArenaCombat.ClassAvatar` now draws
  the full-size class PNG (`classArtwork(type)`) for the active player on the left and the current
  target on the right, each `weight(1f)` flanking the dice center. The target avatar is a new
  derived `targetPlayer: PlayerState.Selected?` on `ArenaState.Combat` (the conscious
  Domain-pinned/auto target, else null), which also became the single source `deriveHeadIconPlayers`
  filters against; `ClassAvatarDisplay` takes a nullable player and stays empty until a target
  exists (SS-144). `ClassAvatar` left a `TODO(SS-068)` hook for the deferred Compottie combat
  animations. Related: SS-144, SS-146, SS-068, SS-143.
- **SS-142** — NewArenaCombat scaffold & migration to replace ArenaCombat: Stood up the new two-half
  combat layout (`combat/` package, parent of SS-143…SS-151) and finished the migration by deleting
  the old `ui/ArenaCombat.kt` and its now-orphaned `dice/DiceTray.kt`, then renaming
  `NewArenaCombat` → `ArenaCombat` (file, composable, previews, transition label,
  `LoadedArenaScreen` call site). Every behavior — hoisted target selection, optimistic target lock,
  SS-101 auto-target, the primary-roll/secondary-controls split, `onAttack`/`onUseAbility` — was
  confirmed carried over to the new composables (shared `rollActionFor`/`isImpersonateRedundant`
  live in their own files). Child of SS-068.
- **SS-152** — Knocked-out player returns to head-icon form with a disabled state: A KO'd opponent
  always shows as a collapsed head icon, never the bottom-half target avatar, and renders disabled —
  faded (`DISABLED_ALPHA`) and drained of class color to a gray gradient (`ClassHeadIcon` gained an
  `isEnabled` flag, placeholder styling). `deriveHeadIconPlayers` only treats a *conscious* opponent
  as the target avatar, so a KO'd player falls back into the `ClassIcons` row (`ClassIcon` passes
  `isEnabled = characterClass.isConscious`). This only matters mid-match: the final knockout ends
  the match (→ Finished screen), so there is no collapse to render then. The *animated*
  large-avatar→grayed-head-icon transition on knockout is future combat-animation work (
  SS-068/SS-144). Related: SS-143, SS-136, SS-123, SS-142.
- **SS-150** — Inspect a player when idle / target-locked: **Decision — the right-side drawer (
  option 2), not expanding the head.** Rationale: a separate drawer keeps the inspect view from
  colliding with the SS-144 head↔avatar shared-element morph and the bottom target avatar (option 1
  fought both), and it cleanly hosts the full avatar + SS-146 stats. `ClassIcon`'s tap is now
  context-split: a normal targeting tap only when it's a valid target (
  `isTargetable && isConscious`), otherwise (not your turn, target-locked, or a KO'd head) it
  inspects — threaded as `onInspect` down to a top-level `inspectedUserId` (reset each turn).
  `InspectDrawer` slides in from the right over a dismiss scrim; `InspectContent` shows the class
  name, full `ClassAvatar`, and every stat with its explanation (reusing the SS-146 `avatarStats`).
  Inspecting never touches `selectedOpponentUserId`, so it's distinct from targeting. Added an
  `InspectContent` `@Preview`. Realizes the former SS-141 placeholder. Related: SS-144, SS-146,
  SS-142.
- **SS-148** — Full-screen flash messages ("Hit!" / "Miss!" / "K.O.!"): A normal-attack outcome now
  flashes a big call-out in the center of the combat screen. `ArenaMessageState.flashText` derives
  the punchy text from a `NormalRoll` (ArmorClassMiss → "Miss!", Damage → "Hit!", Knockout → "
  K.O.!"; the intermediate ArmorClassHit carries none), and `ArenaScreenState.latestFlashMessage`
  reads the most recent one from the *raw* `messages` (not the throttled log) so it fires
  immediately; it's threaded
  `LoadedArenaScreen → ScreenContent → ArenaStateContent → NewArenaCombat`. `FlashOverlay` (a
  screen-spanning layer in the combat root `Box`, above the dice) springs the text in with a bouncy
  `scaleIn` spring, holds, then pops out (`scaleOut`+fade), re-keyed on the message id via a
  `MutableTransitionState`; behind it a one-shot `ConfettiBurst` Canvas flings particles outward in
  the four class-family colors (`classFamilyColors()`). Added a `@Preview`. Flash strings are
  `TODO(SS-029)` string resources; the palette/animation are first-pass for the owner's fine-tune.
  Related: SS-147, SS-025, SS-029, SS-142.
- **SS-146** — Expanding per-stat columns flanking the buttons: `BottomControls` became a `Box` with
  the `Buttons` centered underneath and a `ClassAvatarStats` column pinned to each edge (
  `CenterStart`/`CenterEnd`) on top, so an expanded stat row opens *over* the buttons rather than
  reflowing them. Each column is five `StatRow`s — Speed, Armor, Hit Points (numeric: icon + value)
  and the offensive/defensive ability markers (icon only) — on the player's class color (
  `ClassStyle.toColor`), values from `CharacterClassState` and copy from `LocalCharacterClassCopy` (
  `classCopy[ClassCopyKeys.*]` / `forAbility`). Tapping a row toggles a direction-aware
  `expand/shrinkHorizontally` reveal of the stat's name + short explanation — rightward on the
  left (active player), leftward on the right (target); the right column is hidden when there is no
  target. `DisplayDirection` (the old stub) now drives ordering and expand direction. **Deferred to
  the fine-tune pass:** exact overlay sizing/colors (gradient vs flat), explanation max-width, and
  typography. Related: SS-145, SS-137, SS-151, SS-142.
- **SS-149** — DiceHome + dice roll-over-everything fly-to-home: The roll state machine was lifted
  out of `DiceTray` into a shared `DiceRoll` controller (`rememberDiceRoll`): who's holding,
  shake-arm, the committed-roll await-tumble guard, and haptics, exposed as `startHold`/`release`
  for the gesture surface and `dieFaces`/`isRolling`/`shakeTick` for the dice. `DiceTray` (old
  `ArenaCombat`) was refactored onto it. In `NewArenaCombat` the combat buttons now drive that one
  shared controller — holding a dice button rolls (shake or double-tap to commit, like the tray); a
  choice control (Impersonate, `diceCount` null) commits immediately. The dice render in a
  screen-spanning `DiceOverlay` above all content, so a roll tumbles over everything; `Die3d` gained
  a parameterized tray (`trayWidth`/`trayHeight`) and a `restCenter` it glides back to on settle,
  homing to the measured (`boundsInRoot` minus the overlay origin) empty `DiceHomeSlot` between the
  avatars and buttons. Always shows both dice for a steady footprint; a single-die roll dims and
  stills the second. `DiceHome`/`Buttons` were hoisted out of the target-keyed `AnimatedContent` (
  SS-144/SS-151) so only the morphing head icons/avatars transition. **Follow-ups:** the full-screen
  physics (throw speed/friction across a big tray) wants device feel-tuning (SS-078); old
  `DiceTray`/`ArenaCombat` stay until the migration is deleted. Related: SS-151, SS-144,
  SS-112/SS-117, SS-078, SS-142.
- **SS-151** — Combat action button bar (Attack / Ability / Impersonate): `NewArenaCombat.Buttons`
  is the bottom action bar — `AttackButton` (the `NormalAttack` control) on top, then a row of
  ability button(s) plus the Illusionist's `AnimatedVisibility`-gated `ImpersonateButton`. The
  buttons are derived from `state.combatControls` (SS-101): the `NormalAttack` → AttackButton, the
  `Impersonate` `ActivatedAbility` → ImpersonateButton, everything else → ability button(s); a
  multi-choice roll (Mirror Image) yields a row of labelled ability buttons. Each shares one
  `ArenaButton` style (icon, optional label, disabled alpha) and rolls via a press-and-hold gesture
  that fires the action on release — the same hold-to-roll the dice tray used, so the gesture (
  `holdToRollGesture`) and the action builder (`rollActionFor`/`abilityAction`) were extracted to
  shared files (`HoldToRollGesture.kt`, `CombatControlActions.kt`) and `DiceTray`/`ArenaCombat`
  repointed at them. Icons: `ic_attack_button`, `ic_offensive_ability`, `ic_defensive_ability`,
  `ic_teleport_button` (Teleport roll-off). Target hoist gained an optimistic lock (mirrors old
  `ArenaCombat`) so a stray tap can't retarget mid-roll. Added a user's-turn `@Preview`. **Deferred:
  ** the "Impersonating: $className" copied-class label (TODO(SS-151)/SS-029 strings); the dice
  spinning-while-held + shake-to-arm + in-flight await guard land with the dice integration (
  SS-149). Related: SS-149, SS-101, SS-029, SS-142.
- **SS-155** — Combat turn-timer UI: The 45s combat turn clock ticked in the VM with no on-screen
  indication after SS-154 removed the old `ArenaMessageLogHandle` display. `NewArenaCombat` now
  takes `secondsRemaining`/`isTimerVisible`/`isTimerUrgent` (wired from `screenState` in
  `LoadedArenaScreen`) and renders a `TurnTimer` pinned to the root Box `TopEnd` (top-right corner,
  designer's call). It returns early unless `isTimerVisible` (final ~15s), reuses `AnimatedSeconds`
  for the rolling digit, and when `isTimerUrgent` (final ~10s) switches to the player's `error`
  color, bold, and a looping 1→1.3 pulse (`timerPulseScale`) — the urgent styling carried over from
  the removed `TimerText`. Presentation only; no new timer logic. Also stripped every inline comment
  from `NewArenaCombat.kt` (TODO(SS-xxx) markers kept). Related: SS-154, SS-068.
- **SS-156** — Stat column must not shift when a stat row expands: Expanding a stat row grew the
  `StatRow` inline (mainly taller, since the wrapped explanation exceeds the icon height), pushing
  sibling rows and re-centering the column. The explanation is now a true overlay: `StatRow` lays
  out only the fixed-size icon pill and, when expanded, renders the explanation in a `Popup` placed
  by `StatExplanationPositionProvider` flush against the pill's inner edge and vertically centered (
  rightward on the Left column, leftward on the Right, clamped to the window). A popup lives outside
  the layout tree, so neighbors, the column, and the buttons never move. The explanation carries its
  own class-color background and keeps its expand/shrink+fade, staying mounted through the exit
  transition. Related: SS-146.
- **SS-157** — Active player always occupies the left avatar slot: The bottom-half avatars and their
  flanking stat columns hardcoded the local user on the left and the user's target on the right. Now
  the LEFT slot is always the turn owner (whoever's turn it is) and the RIGHT slot is the targeted
  player, on every device. Added `CombatActionState.turnOwnerUserId` (the original actor, kept
  stable even when Riposte/Persuade hands the dice elsewhere) and `targetedDefenderUserId`;
  `ArenaState.Combat` exposes `turnOwnerUserId`/`turnOwnerPlayer`. `resolveTargetUserId` now
  resolves the right slot for both turns: the user's chosen/locked/auto target on their own turn (
  unchanged, SS-116 gating preserved via an early return), the in-flight action's defender
  otherwise; and for non-turn-owner devices it falls back to `turnOwnerLoneOpponentUserId` so a
  2-player default target shows on the opponent's device too (the turn owner's auto-target is local
  and never crosses the wire). `headIconPlayersFor` now excludes the turn owner (was the local
  user). In `NewArenaCombat` the avatar `AnimatedContent` keys on a new
  `AvatarPairing(activeUserId, targetUserId)` so both slots morph on a turn handoff or target
  change; `AvatarsRow` left = active player, `BottomControls` left stat column = `turnOwnerPlayer`.
  Related: SS-145, SS-156, SS-116.
- **SS-158** — Single-die rolls must animate only one die: In `DiceOverlay` the second die is
  already laid out disabled (`isActive=false` → `isRolling=false`, `enabled=false`) for a single-die
  roll, but when a following single-die roll turned the second die off mid-flight (or flipped its
  value to the resting face), `Die3d`'s settle logic still hit the
  `hasValueChanged → randomThrowVelocity` branch and threw the disabled die across the tray before
  homing. Added a top `!enabled → Offset.Zero` branch to `settleVelocity` so a disabled die never
  tumbles — it only glides back to its `DiceHome` slot and rights itself while the active die rolls.
  Related: SS-149, SS-117.
- **SS-161** — Messages affordance overhaul (icon, unread tally, scroll-unfurl, ordering): Reworked
  the top-half messages affordance (`CombatMessages.kt` + `ArenaMessageLog.kt`). The `MessagesIcon`
  is now clipped to `CircleShape` (round affordance + ripple). The unread tally keeps the existing
  flowing-gradient `Text` and now reads `9+` once unread reaches `UNREAD_DISPLAY_CAP` (10+); it
  still resets to zero on open (existing `lastSeenMessageCount` mark-all-read, keyed on
  `isExpanded`). The collapsed icon stays clear of the head-icon row via the existing
  `padding(top = MESSAGES_ICON_SIZE)` on `ClassIcons`; when expanded, `ArenaMessageLog` takes a new
  `contentTopInset = MESSAGES_ICON_SIZE` so the first line clears the pinned icon. The gold panel
  expands top→bottom on open and contracts back up on close by animating its own height —
  `animateFloatAsState` drives an `expandedFraction` (0→1) fed into `fillMaxHeight(fraction = …)`,
  grown downward from the Box top-start. Reading order: most-recent message sits at the **top**;
  `ArenaMessageLog` renders `messages.asReversed()` (real `messages` are oldest-first/newest-last)
  in a plain top-down `LazyColumn` with no auto-scroll, so new messages prepend at the top without
  the viewport jumping. The preview list was reordered oldest-first to match real data. Related:
  SS-147, SS-154, SS-025.
- **SS-147** — Messages overlay: golden expandable panel + animated unread count:
  `NewArenaCombat.Messages` is a top-half `Box` overlay — an `ic_messages` icon pinned top-start
  with the unread count drawn in its center, over an `AnimatedVisibility` gold panel (
  `expand/shrinkVertically(Top) + fade`) that grows downward over the head icons and is bounded to
  the top half (so it stops at `BottomHalfContent`). The panel reuses the existing
  `ArenaMessageLog`; the unread count is `screenState.unhandledMessagesCount` and the log content is
  `filteredMessages`, both threaded
  `LoadedArenaScreen → ScreenContent → ArenaStateContent → NewArenaCombat`. The count's digits are
  filled with a looping `TileMode.Repeated` linear gradient of the four class-family colors (new
  `classFamilyColors()` in `ClassStyle`) whose start/end shift forever via an `infiniteRepeatable`
  float, so the colors appear to flow through the number. Gold (`PLACEHOLDER_PANEL_GOLD`) is
  eyeballed. **Follow-ups:** the old bottom-sheet log + `ArenaMessageLogHandle` (which also hosts
  the turn timer) still coexist during combat — reconciling/removing the sheet and the final gold
  value are deferred (relates to SS-025, SS-142). Related: SS-025, SS-142.
- **SS-173** — Messages: full-width golden bar that unfurls to DiceHome: The collapsed messages
  affordance is now a full-width golden `MessagesBar` — the existing circular `MessagesIcon` with
  its flowing-color unread tally kept on the left, the most recent message beside it (
  `screenState.messages.lastOrNull()` via `getDisplayString`, single-line ellipsized), and the
  `TurnTimer` on the right. Tapping the messages region toggles `MessageLogOverlay`, which now
  unfurls downward from the bar and stops at the top of the dice-home slot (`diceHomeBounds.top`
  from `DiceHomeSlot.boundsInRoot`) — covering the topmost avatar but leaving `DiceHome` and the
  buttons untouched — instead of the old fixed 50%-height panel. `ArenaMessageLog` drops its
  icon-clearing left inset (the icon no longer overlaps the log) for a plain 16.dp horizontal
  padding. Related: SS-147, SS-161, SS-179.
- **SS-174** — Tapping the inspect drawer dismisses it: `InspectContent`'s drawer surface
  `clickable` was a tap-swallowing no-op; it now takes an `onClose` and calls it, so tapping
  anywhere on the opened drawer dismisses it just like the scrim. Related: SS-150.
- **SS-175** — 2-player: stat icons as a vertical column left of the avatar: The
  Speed/Armor/Hit-points markers render as a vertical `Column` (`AvatarStatsBar`) down the left side
  of each `LargeAvatar` in `CombatAvatarColumn`, with the local-player identity marker stacked
  atop — no longer a horizontal bar above the avatar. Reads correctly with the SS-157/SS-168
  attacker/target seating. Related: SS-146, SS-156, SS-157, SS-168.
- **SS-176** — Choose Your Class: animated class-color aura on the selected head + default-select
  Rogue: The centered (selected) class head in `CharacterClassWheel` now sits over a `ClassAura` — a
  radial gradient in the class color (`toColor`) that fades to transparent and pulses in
  size/brightness via an `infiniteRepeatable` scale, threaded down as
  `isSelected = index == centeredIndex` through `RotatingWheel`/`ClassAvatar` (avatar restructured
  into a Box holding the aura behind the bust image). Separately, the Choosing Classes state now
  pre-selects a class on first entry so the confirm button is enabled immediately:
  `ArenaScreenState`'s mapping falls back to `characterClasses.defaultSelectedClass(unavailable)`
  when there's no prior selection — Rogue when available, else the first still-available class (
  respecting the no-duplicate `unavailableClasses` rule). Related: SS-079, SS-084, SS-153, SS-122.
- **SS-113** — Dice physically roll around their container and bounce off the walls: `Die3d` now
  lives in a tray larger than the die and runs a tiny per-frame 2D physics integrator (
  `tumbleDieAroundTray`) — on release the die gets a random velocity, ricochets off the tray walls
  with restitution, tumbles in proportion to its travel, and slows under friction until it comes to
  rest, then rights itself onto the rolled face. Replaces the earlier scripted single-arc path.
- **SS-108** — Lock a player to one class for the whole Gauntlet: A player keeps the same class for
  all 3 matches of a Gauntlet; between matches the lobby skips ChoosingClasses and rebuilds Combat
  directly with fresh full-health classes, while "Go Again" after the final match starts a new
  Gauntlet back at class selection. The between-match/gauntlet-complete dialog is removed and Next
  Match/Go Again/Leave now live on the Finished screen.
- **SS-013** — Review ArenaEvent (empty TODO): Empty placeholder TODO — determine intent (likely
  additional events) or remove.
- **SS-014** — Handle match/timer expiration per state: Define and implement what happens when the
  timer expires in each `ArenaState`, and how the `MatchTimedOut` event is surfaced to the user.
- **SS-015** — Delete unused ArenaActions: Remove the dead code flagged for deletion (verify no
  usages first).
- **SS-016** — Rename/refactor ArenaActions: Rename/refactor the flagged ArenaActions construct for
  clarity.
- **SS-017** — Refactor ArenaScreenViewModel: Address the broad "this is all fucked" flag — clean up
  the ViewModel section it marks.
- **SS-018** — Better userId resolution: Establish a cleaner way to obtain the current user's id.
- **SS-019** — Track doublesBonus in ArenaState.Combat: Track and pass the real `doublesBonus` (
  currently hardcoded `null`) per the doubles +1 consecutive-roll rule in CLAUDE.md.
- **SS-020** — Fix Charm logic in onUseAbility: Correct how `selectedCharmTargetUserId` is resolved
  for Persuade/Charm. Related: SS-049.
- **SS-022** — Remove arena match when last player leaves: When the last player leaves, remove the
  arena match.
- **SS-023** — Resources + split functions in ArenaChoosingClasses: Extract string resources and
  split the composable back into separate functions.
- **SS-024** — Possibly add finished-state content: Decide whether the finished-match screen needs
  additional content; add it or keep as-is.
- **SS-026** — Add isThisPlayerTheUser to PlayerState: Decided against — the codebase already
  resolves "this user" everywhere by comparing `userId == userPlayerId` (e.g. ArenaScreenState,
  ArenaCombatControl); a per-player flag would duplicate that and need the user id threaded into the
  mapping for no UI gain. Removed the TODO.
- **SS-027** — UI for waiting players: Replaced the bare loading spinner on
  `ArenaState.WaitingForAllPlayers` with a themed waiting screen — "Gathering Challengers" title,
  pulsing dots, flavor subtitle, and a card that rotates through class/combat tips so waiting
  players have something to read. Self-contained (the state carries no player data); copy in string
  resources.
- **SS-028** — Arena previews & enum-driven preview text: Added a `@Preview` for the SS-027 waiting
  screen, and changed `ArenaStatePreviewParameterProvider` to derive preview class names from the
  `CharacterClassType` enum (`toMessageInfoState()`) instead of repeating string literals, so
  previews share the single enum source of truth. The "(using resources)" aspiration was dropped — a
  `PreviewParameterProvider` isn't `@Composable`, so it can't resolve string resources, and the
  fixtures are dev-only.
- **SS-030** — Rename/assign property in ArenaScreenState: Improve the flagged property's name and
  assignment.
- **SS-032** — Decide timer-start strategy: Decide and document how timers should be started (e.g.
  based on `NextCombatAction`).
- **SS-136** — Class head icon (collapsed) composable: Shipped `ClassHeadIcon` — the collapsed,
  class-colored circular avatar with Speed/Armor/Hitpoints badges shown for each non-active player,
  wired into the Arena combat layout. Consumed by the head-icon row and head↔avatar target morph (
  SS-143/SS-144/SS-152) and colored via `ClassStyle` (SS-137/SS-153). Related: SS-138, SS-143,
  SS-152.
- **SS-172** — Finished-state timer differs for Match vs Gauntlet: `determineTimerAction` now splits
  the Finished branch on `isFinalMatch` (threaded in from `GauntletProgress`, at both the
  `toUpdatedState` and ViewModel `advanceToFinished` call sites). A non-final match (
  `determineMatchFinishedTimerAction`) starts a 30s countdown the instant the Finished screen is
  entered — no ready required — and auto-readies each remaining player when it expires; once
  running, later updates return `None`. A lone survivor (`!canStartNextMatch`) starts no countdown,
  so nothing auto-readies into an impossible single-player match (SS-170). The final match /
  Gauntlet (`determineGauntletFinishedTimerAction`) keeps the old behavior: a 60s countdown that
  starts only once someone readies. Tests cover the split at both the `determineTimerAction` and
  ViewModel levels. Related: SS-108, SS-073, SS-170.
- **SS-191** — Extensible avatar animation system + normal-attack animations: Pure-Compose gesture
  system for the two seated combat avatars, mirroring the FlashCallout pipeline.
  `CombatAnimationCue` (model) derives per message via an exhaustive `combatAnimationCue` mapping (
  null branches carry TODO(SS-192…SS-199) tags per class ticket); `cue.gestureFor(userId)` resolves
  role → `AvatarGesture` (Lunge/LungeStrong/LungeOvershoot/Flinch/FlinchHeavy/Sidestep/Collapse);
  `attackerUserId`/`defenderUserId` threaded into `ArenaMessageState.NormalRoll`. Flash queue
  sequencing hoisted into `FlashPlaybackState` (`CombatFlashPlayback.kt`) so flash text and avatar
  gestures share one sequencer. Choreography in `CombatAvatarGesture.kt`: Animatable transforms via
  `graphicsLayer` with a feet pivot (top seat strikes down, bottom up), red hurt-tint + desaturation
  via `ColorMatrix`, defenders reacting ~170ms after the strike. Knockout collapse is a single
  `collapseFraction`; the lying pose (shrink to seat width + feet slide to seat edge) is computed in
  the graphics layer — the only place the seat size is known — so the downed avatar stays fully on
  screen and holds its downed pose through the SS-184 finish hold. 7 unit tests (
  `CombatAnimationCueTest`). Related: SS-148/SS-164, SS-184, SS-152. Children: SS-192…SS-199.
- **SS-192** — Rogue ability animations: First class built on the SS-191 seam (PR #202, commit
  871c063). Introduced the weapon-as-strike model — `CombatWeapon` enum +
  `CharacterClassType.combatWeapon` extension (`CombatWeapon.kt`, Rogue → `Dagger`, all other
  classes null and fall back to SS-191 body lunges). New `CombatWeaponStrike.kt` renders the dagger
  flying at the defender as a class-colored flash of steel while the attacker only sells the throw;
  `CombatAvatarGesture.kt` gains the Rogue's close-combat set — `stab`/`stabFlurry`/
  `stabOvershoot` (hit/miss/knockout dashes into melee, dagger overlay riding the thrust) plus
  `dashIn`/`settleBack`/`reArm`, and a `dodgeFlourish`/`dodgeBrace` weave for the **Dodge** miss
  reaction (armor-gain hardening accent). Killing Momentum re-arms toward the follow-up target for
  one continuous assault. Weapon melee strikes push defender reactions to
  `MELEE_REACTION_DELAY_MILLIS` (the dash spends time closing in). Cue sources:
  `ArenaMessageState.KillingMomentum`, `.Dodge`, `.NormalRoll` (attacker side). Tests expanded in
  `CombatAnimationCueTest`, `ArenaMessageStateTest`, `MirrorImageKeepFlowTest`. Parent: SS-191.
- **SS-231** — Hold turn-order / seating changes until resolution animations finish: the seated
  avatars, active-roller halo, and turn owner now stay on the just-resolved action until the flash
  sequencer finishes, then advance as a unit — so an acting avatar (e.g. a Duelist Prepare Attack
  miss) plays its animation through instead of reverting to a head icon while the next player's turn
  appears. New `CombatMoment`/`CombatMomentHold` (pure, tested fold) held in `rememberHeldCombatMoment`
  and fed into `ArenaCombat`'s seating/`CombatStage`. Absorbed former SS-232. Related: SS-215, SS-181,
  SS-213, SS-233 (part 1).
- **SS-233** — Encore animation must finish before Bard reverts to head icon; suppress Encore on
  final KO: part 1 (animation cut-off) resolved by SS-231's seat/turn hold; part 2 guards
  `EncoreHandler.triggerEncore` so a Bard's Encore no longer triggers when the hit defeated the last
  opponent (`consciousPlayers.size <= 1` → resolve turn end) — the leak was the Holy Shield failed-
  Prayer path. Also closes the matching SS-206 follow-up. Related: SS-231, SS-206.
- **SS-242** — Roll-off dice per combatant: the Domain assigns each Teleport roll-off die a stable
  `diePosition` (attacker = First, Mage = Second) on `TeleportDie`, threaded through the data entity
  and UI-state mappings; each player rolls only their own die and each die is tinted its owner's
  class color. `rollingDiePositions` precomputed as a construction-time val on `ArenaCombatControl`.
  Follow-ups: SS-244 (settle/color timing, independent settle, doubles badge on correct die),
  SS-245 (tie indicator).
- **SS-244** — Dice settle and are legible before results/colors change. Bug 1: flash sequencer
  commits the roll, awaits each die's real landing, then plays the combat cue (settle-before-result).
  Bugs 2-4: each die is fully independent — `DieState` carries value/tint/enabled/settleGeneration
  plus per-die isRollingLocally/isSettling (no parallel Sets on the holder); a roll only touches its
  own die, coloring it when it lands and leaving the other die's face/color/enabled alone (roll-off
  dice never white-out or disable mid-roll-off; solo single-die still greys the unused die). Tint is
  carried on the roll itself, not derived from the (already-advanced) combat action: Domain
  `DiceRoll.OneDie.rollOffOwnerUserId` set via `DiceRoller.roll1D6(isRollOff)` from `TeleportHandler`,
  persisted in `DiceRollEntity`, mapped to a class color in `DiceRoll.toState`. Bug 5: doubles badge
  sits over the die owned by the bonus holder in a roll-off. Follow-ups: SS-246 (wrongful re-roll),
  SS-247 (class-color all ability dice via a DieColor enum), SS-248 (badge persists to latest state).
- **SS-247** — Class-color all ability dice via a domain `DieColor` enum (NormalAttack, Disabled, one
  per class) with a UI `toColor()`; the roll carries its color intent Domain → Data → UI. Every
  ability roll (offensive, defensive, roll-off) tints its dice by the die owner's class; normal
  attacks stay white; solo single-die abilities grey the unused die. Replaced SS-244's
  `rollOffOwnerUserId` stopgap. Also: `DiceRoll.OneDie.disablesSibling` (renamed the wrong `isRollOff`
  concept — Teleport and MirrorImage both keep their sibling live), solo rolls default to
  `DiePosition.First` so the armed and rolled die agree (no more both-dice-moving), and a disabled die
  clears its tint (no gray-green).
- **SS-248** — Dice badge persists to latest committed state until the next roll (not just Teleport):
  the `diceBadge` is committed and held on the roll state through `DiceStateHolder` like the dice
  faces/colors (SS-244), instead of being derived reactively from the advancing combat action / active
  users, so any doubles/mirror-image badge holds until the next roll replaces it rather than blinking
  out mid-sequence.
- **SS-237** — Illusionist Mirror Image keeps the −1 die badge on the rerolled die until the next
  reroll: the −1 badge is modeled as a property of the specific die Mirror Image rerolled, carried on
  the roll state Domain → Data → UI, and cleared only when a subsequent reroll replaces the dice
  (applies to both ArmorClass and Damage/Health rerolls; not on SPC damage).

## App / Main

- **SS-035** — Random names from RemoteConfig: Source random traveler names from RemoteConfig
  instead of hardcoding.
- **SS-036** — Handle signed-in user with missing profile: Detect a signed-in account with no
  profile (when online) and route back to the name dialog to create one for the current account
  instead of hanging on the loading indicator. Also fixed `isSignedIn()` to await the first
  auth-state emission so a cold start no longer reports a returning user as signed-out.

## Navigation

- **SS-135** — Drawer navigation should pop back to a home screen: When opening a screen from the
  navigation drawer, trim trailing non-home entries off the back stack (home = Tavern, or Arena
  mid-match) before pushing the destination, so backing out returns to a home screen rather than
  whatever screen the drawer was opened from.

## Data layer

- **SS-038** — Determine if IOSNullableStringSerializer is needed: Decide whether this serializer is
  required; keep with justification or delete.
- **SS-039** — Unique user-names collection: Implement a names collection to enforce unique display
  names. Related: SS-035.
- **SS-040** — Paginate / separate node for arena messages: Reduce payload size by paginating
  messages or moving them to a separate RTDB node.
- **SS-041** — Refactor flagged arena repo code: Refactor the flagged section of
  `FirebaseArenaRepository`.
- **SS-042** — Challenge timestamp handling: Resolve how the challenge timestamp is
  represented/serialized across the data entity and domain model.
- **SS-043** — Extract class to separate file: Move the flagged class out of
  `FirebaseTavernRepository` into its own file.
- **SS-044** — Connecting messages in ConnectToArena: The
  `ArenaMessage.ConnectedToArena(displayName)` type was fully wired through every layer (entity
  round trip, UI state, `ArenaMessageDisplay`/`ArenaMessageLog`) but had no emission site, so the
  lobby never logged joins. `ConnectToArena` now appends a `ConnectedToArena` message inside the
  join transaction when a new player is added to `WaitingForAllPlayers` (id via the messages-node
  `addNewKey()`, `appendCapped` to the retained-message cap, `numberOfNewMessages` bumped by 1 — set
  to 1 on the transition into `ChoosingClasses`). Reconnects (userId already present) early-return
  unchanged, so no duplicate join message is logged.
- **SS-045** — UI when match ends due to early leave: Notify the remaining player when an opponent
  leaves the arena via a snackbar, driven by an `ArenaEvent.OpponentLeft` the ViewModel emits when a
  player drops out of the shared player map. Related: SS-098, SS-071, SS-022.
- **SS-125** — Delta-sync tavern presence with childEvents (stop streaming the whole node):
  `FirebaseTavernDataSource.travelerEntitiesFlow()` subscribed to the entire `TavernTravelers` node
  via `valueEvents`, which re-emits the whole N-element snapshot on every join/exit/status flip — O(
  N²) aggregate payload/deserialization as the Tavern fills. Switched to RTDB
  `childEvents(ADDED, CHANGED, REMOVED)` folded with `scan` into a locally-maintained
  `Map<userId, TravelerEntity>`, so each change carries only the affected traveler while the public
  `Flow<List<TravelerEntity>>` contract is unchanged (Domain/UI, `FakeTavernDataSource`, and tavern
  tests untouched; `getTavernTravelersFlow` already `distinctUntilChanged`). Cheap, high-leverage
  fix that precedes sharding (SS-126).

## Domain layer

- **SS-216** — Encapsulate the Riposte turn-owner divergence: Collapse the near-duplicate
  `attackerUserId`/`originalTurnUserId` pair on the combat actions into one polymorphic `AttackTurn`
  (`Standard` = one id; `RiposteReversal` = both), across domain/data/UI + entity mirror
  (`AttackTurnEntity`). SS-029 follow-up (field-collapse only; availability/StringResources still
  open under SS-029).
- **SS-046** — Add maximum HP: Introduce a max-HP concept so healing/shields can't exceed a cap (
  interacts with Necromancer heal-to-full, shields).
- **SS-047** — Class stat values from RemoteConfig: Drive class stat tuning values (`hitPoints`/
  `maxHitPoints`/`armorClass`/`speed`) from RemoteConfig instead of hardcoding. Related: SS-004,
  SS-035. Ability roll values split out to SS-058.
- **SS-058** — Ability roll values from RemoteConfig: Externalize every ability roll
  threshold/range/bucket (Holy Shield 10–12, Riposte 6–8, Prepare Attack 10–12 + bypass 12, All-In,
  Summon Minion faces, Encore, Persuade lull/charm, Lucky Coin high-roller, Dodge cap, Spell Potency
  splash) into a domain `AbilityConfig` (with `DEFAULT`) backed by `ability_config.json` +
  RemoteConfig, threaded through `PerformCombatActionUseCase` → `PerformCombatAction` →
  `CombatMutator` → handlers via `AbilityConfigProvider`. Teleport (pure roll-off), Mirror Image
  −1 (serialized model), and the doubles +1 left out of scope. Related: SS-047, SS-085.
- **SS-081** — Class ability/stat copy in RemoteConfig (long + short variants): Move all class
  stat + ability copy out of code into a domain `CharacterClassCopy` (name + long + short per key)
  backed by `character_class_copy.json` + RemoteConfig via `CharacterClassCopyProvider`/
  `GetCharacterClassCopyUseCase`. GameRules consumes the long variant; the class wheel consumes the
  short variant through `LocalCharacterClassCopy`. Related: SS-067, SS-079, SS-033, SS-047.
- **SS-059** — Single-source class stats via bundled JSON (drop factory stat literals): Make the
  version-controlled `character_class_stats.json` the single source of default stats: bundle it as a
  `commonMain` resource and use it as both the RemoteConfig in-app default (`setDefaults`) and the…
- **SS-111** — Make RemoteConfig the only production source of class stats: Delete the
  `CharacterClass.getRogue()`/`getAllClasses()` stat factories so the bundled
  `character_class_stats.json` (+ Firebase) is the sole stat source; abilities wired in code via a
  `create(type, stats…)` builder with an exhaustive `when`. Tests keep factory-style builders in a
  `commonTest` fixture. Related: SS-047, SS-059, SS-004.
- **SS-085** — Bundled-JSON in-app defaults for all RemoteConfig keys: One small loader reads each
  parameter's version-controlled JSON from
  `composeApp/src/commonMain/composeResources/files/remote_config/<key>.json` and uses it as both
  the RemoteConfig in-app default (`setDefaults`) and…
- **SS-049** — Persuade: pass selectedCharmTargetUserId from UI: Ensure the UI supplies the Charm
  target id for Persuade. Related: SS-020.
- **SS-050** — Implement KillingMomentum: Implement the Rogue "Killing Momentum" rule — on a K.O.,
  attack another chosen player.
- **SS-054** — SpellPotency splash-damage attacker case: Handle the Paladin Holy Shield retaliation
  when the incoming damage was SpellPotency splash damage (who is the "attacker"?).

## Dependency injection

- **SS-055** — Scope DI definitions instead of single: Introduce appropriate Koin scopes rather than
  `single` for everything.
- **SS-056** — Name DI definition: Give the flagged DI definition a meaningful name.

## Tavern timing

- **SS-057** — Challenge timer must use the communal backend deadline: Drive the challenge countdown
  from the shared backend deadline instead of a hardcoded 60s anchored to when each client first
  observed the challenge. Related: SS-042.

## Bugs

- **SS-224** — Teleport roll-off crash / stuck roll-off / button hidden: the domain kept both
  combatants active for simultaneous rolling but the computer re-selected an already-rolled Mage
  (crashing on the uninitialized `nextCombatAction`), and `isSequencing` hid the second combatant's
  still-owed Teleport button while the first's roll animated. Fixes: `TeleportHandler` re-asserts the
  roll-off instead of crashing on a stale/duplicate dispatch; `ComputerOpponentHandler` skips a
  combatant whose die is already rolled (`isUserTheRoller`); and the pending roll-off button stays
  visible through sequencing (`ArenaState.Combat.isAwaitingTeleportRoll`). Simultaneous-dice tray
  rendering split to SS-242; tie re-roll to SS-243.
- **SS-243** — Teleport tie re-rolls until a winner: a tied roll-off no longer resolves in the
  attacker's favor; `TeleportHandler.handleUpdatedTeleport` now detects an equal roll and resets both
  dice (clearing the spent attacker doubles bonus) to re-run the roll-off until one side rolls higher.
- **SS-228** — Using an activated ability must restart the turn timer: All-In and Impersonate keep
  the turn with the same Idle player, so `combatActionState` was unchanged and `determineTimerAction`
  left the pre-ability countdown running. It now also restarts the combat clock when the active
  player's own state changed while their Idle turn held (`hasActivePlayerActedWhileStillActive`).
- **SS-229** — Lucky Coin HighRoller payback coin showed 6 instead of the damage rolled: the dice
  tray was already correct; the thrown payback coin in `GamblerLuckyCoin.kt` drew a hardcoded
  `LUCKY_FACE` (6) while the actual damage was carried unused on the cue. `throwScene` now uses
  `cue.damage`, so the flung coin reads the resolving roll.
- **SS-230** — Finished-arena Leave button popped the whole back stack and exited the app: arena exit
  routed through `popWithDust`'s non-idempotent single `removeLastOrNull()`, so a second `NavigateBack`
  (the arena-deleted stream path racing the leave) popped past the Tavern root. Added an idempotent
  `NavBackStack.popToTavern()` and route `ArenaScreen`'s `onBack` through `leaveArenaWithDust`, landing
  deterministically on the Tavern so repeated leave events can't drain the stack.
- **SS-222** — Behavioral regressions surfaced by the dice-architecture refactor: resolved the 8
  post-refactor test failures (seating `resolveTargetUserId`, Mirror Image reroll button order, and
  Mirror Image domain Doubles-message emission), deciding per-case between production fix and test
  update to get the suite back to green.
- **SS-202** — All-In button stuck disabled because `hasActedThisRound` was dropped in the UI
  round-trip: `CharacterClassState` omitted `hasActedThisRound`, so `CharacterClassState.toDomain()`
  (used to build every dispatched action's `playerMap`) reset it to false via the domain default —
  rounds never completed, so a Gambler's `usedThisTurn` never reset. Fix: carry the field in the UI
  state and map it both ways, and remove the `= false` default on `CharacterClass.hasActedThisRound`
  so every mapping is compile-enforced (recurrence of the SS-120 class of bug).

- **SS-170** — Finished screen must not offer "Next Match" when only one player remains: A player
  only leaves the shared player map by exiting the arena, so `ArenaState.Finished` now derives
  `canStartNextMatch = players.size >= 2`. When only one player remains, `MatchDecisionControls` (
  `MatchFinishedContent.kt`) suppresses the Next Match/Go Again button and its countdown, showing
  an "everyone else has left" note with only Leave — a single-player next match is impossible. No
  auto-ready can fire because the Finished countdown only starts once someone readies, and the lone
  player has no ready control. Tests cover both the two-player and lone-survivor derived states.
  Related: SS-108, SS-045, SS-022, SS-172.
- **SS-190** — Doubles on defensive-ability 2d6 rolls silently set a bonus with no message:
  `RealDiceRoller.roll2D6` sets the doubles bonus for every 2d6 roll (the universal doubles
  mechanic, kept as-is), but ability rolls emitted no `ArenaMessage.Doubles`, so a
  consecutively-consumed +1 applied invisibly. Decided ability 2d6 rolls do earn the bonus, and now
  message it exactly when the roller's next roll is consecutive: Riposte on success (Duelist keeps
  the dice for the reversal ArmorClass roll) and Holy Shield prayer on survival (Paladin keeps the
  dice for the Wrath roll). On failure/turn-end the next roll changes hands and nullifies the bonus,
  so no message — Prepare Attack ends the turn and stays unannounced too. Related: SS-019, SS-177,
  SS-107.
- **SS-189** — Teleport roll-off dispatch order can nullify the attacker's doubles bonus: the
  roll-off is simultaneous but dispatched one die at a time, and `RealDiceRoller.consumeBonus`
  clears the stored bonus on the next roll by anyone — so if the Mage's die was dispatched first,
  the attacker's carried +1 was silently consumed before the attacker rolled, breaking the promise
  SS-177's Doubles message makes. Following the SS-188 pattern, `AbilityAction.Passive.Teleport` now
  carries `attackerDoublesBonus`, captured at trigger from the dice roller (domain, entity round
  trip default-0 for in-flight actions, and UI state); the roll-off applies it to the attacker's die
  from the persisted action (`rollAndMessage` uses the raw die face + carried bonus) instead of the
  order-sensitive roller bonus, so the +1 lands regardless of which combatant rolls first. Domain
  test covers the Mage-die-first dispatch order. Related: SS-019, SS-177 (found during), SS-188.
- **SS-188** — Mirror Image re-check drops the attacker's doubles bonus: The original armor/damage
  roll total baked in the attacker's carried doubles +1 (
  `NormalAttackRollHandler.diceTotal = firstDie + secondDie + bonus`), but the Mirror Image
  keep/reroll re-check recomputed the total from the two `finalValue`s alone, so a decisive +1 could
  silently flip a "Hit!" back into a miss on Keep (and shift a reroll's threshold by one).
  `AbilityAction.Passive.MirrorImage` now carries `attackerDoublesBonus` (domain, entity round trip
  default-0 for in-flight actions persisted before the field, and UI state);
  `triggerMirrorImageIfApplicable` passes the in-scope bonus into the purpose, and
  `resolveModifiedRoll` adds it to `firstDie.finalValue + secondDie.finalValue`. Domain tests cover
  a decisive bonus on both the keep and reroll ArmorClass paths. Related: SS-181 (found during),
  SS-019, SS-118.

- **SS-060** — Holy Shield: suppress KnockedOut dialog during the survival roll: When a Paladin is
  knocked out but is about to roll (or is rolling) for Holy Shield, the KnockedOut/match-decision
  dialog must NOT show. The dialog should only appear once Holy Shield has resolved and the Paladin
  is…
- **SS-090** — First-launch "Continue with Google" must recover an existing account, not error
- **SS-095** — Lock target selection once an attack roll is committed: After a player has rolled
  against a target for a normal attack (i.e. committed the Armor-class roll), they must not be able
  to change targets or switch to an activated ability mid-action. The UI must also not highlight…
- **SS-096** — Keep the rolled-against opponent targeted through ability resolution: Ensure the
  originally-targeted opponent remains the target through the full resolution of an in-flight
  attack (including defensive-ability roll-offs that bounce back to the attacker).
- **SS-093** — Ability outcome messages must reflect the actual roll result: Give Summon Minion (
  fizzle/skeleton/skeleton+heal/golem+heal) and All-In (self-KO/no-effect/double-attack) per-outcome
  message copy keyed off the rolled die; every other multi-outcome ability already carried its
  specific outcome.
- **SS-103** — ArenaMessages permanently disappearing in some flows: Clamp the running
  unhandled-message count to the (capped/reset) log size in toUpdatedState so it can't outrun the
  messages that remain — otherwise latestMessage resolved to null and filteredMessages to empty,
  wiping the log with no way to recover (seen after big combat results and on match transitions).
- **SS-097** — ArenaMessages must use the character class name, never the displayName: The
  userId→name resolution map fed to ArenaMessage.toState now holds each player's class name (built
  from Selected players, persisted across leaves), so all combat messages read as "Rogue"/"Bard";
  only the pre-class lifecycle messages (ChoseCharacterClass, ConnectedToArena) still carry their
  own displayName.
- **SS-098** — Defer the Finished transition until end-of-match messages/animations play: When a
  match finishes while closing messages remain unhandled, toUpdatedState holds the combat view (
  updated to show the final knockout) and stashes the finished state as pendingFinishedState (
  stopping the combat timer); once the messages drain, onIncomingMessageHandled advances arenaState
  to Finished — so the closing knockout/animation is no longer cut off.
- **SS-154** — Stop the message log from driving screen-state logic (match stranded, never reached
  Finished): The deferred Finished transition (SS-098) was clocked by a UI message-drain that
  vanished when combat UI moved to NewArenaCombat (SS-142), stranding matches on the combat view.
  Removed all message-driven reveal/defer machinery (`unhandledMessagesCount`/`latestMessage`/
  `filteredMessages`, the VM reveal loop, `onIncomingMessageHandled`, the orphaned
  `ArenaMessageLogHandle`); messages now just append to the log. Combat→Finished is held
  unconditionally for a fixed `FINISH_HOLD_MILLIS` by a VM `finishHoldJob` so the SS-148 flash
  plays, then advances on the VM's own clock. Unread badge tracked locally in the UI. Supersedes
  SS-098/SS-103's count mechanism. Combat timer UI follow-up split to SS-155.
- **SS-177** — Doubles message/flash only when the +1 bonus can actually apply:
  `ArenaMessage.Doubles` is now suppressed whenever the attacker's next roll is not consecutive —
  armor miss with turn end, defender abilities that take the dice (Riposte, Persuade, Lucky Coin,
  Holy Shield prayer), turn-ending damage rolls, and match-ending knockouts. It still fires when the
  dice stay with the attacker: armor hit into the Health roll, Teleport roll-off, Killing Momentum,
  the Encore die, and pending All-In second/minion attacks. Rules live in `DoublesMessageRules.kt` (
  `CombatMutator` extensions); an unused Mirror Image defers the message to keep-time in
  `MirrorImageHandler`, which also gained the missing ≤1-conscious guard so Killing Momentum/Encore
  can't trigger off a match-ending mirror-imaged knockout. Related: SS-107, SS-019, SS-164, SS-188.
- **SS-171** — Hold the Combat→Finished transition until end-of-match flash animations finish (+2s):
  SS-154's fixed `FINISH_HOLD_MILLIS` cut off the closing flash queue whenever the final resolution
  produced more than one callout. Replaced it with a dynamic hold in `ArenaScreenViewModel` (
  `finishHoldMillis`) that counts the flash callouts arriving with the closing resolution (new ids
  vs the previous state), multiplies by the overlay's per-callout wall time (new internal
  `FLASH_MESSAGE_DURATION_MILLIS` = hold + exit in `CombatFlashOverlay`), then adds a 2s
  `FINISH_HOLD_TAIL_MILLIS`. Also withheld the combat controls during the hold:
  `ArenaScreenState.isFinishPending` (true while `pendingFinishedState != null`) gates
  `BottomControls` in `ArenaCombat`, so no attack/ability button is tappable after the match is
  over. Related: SS-154, SS-164, SS-148.
- **SS-162** — Combat stalled after a Mage Teleport roll-off: The roll-off set `activeUserIds` to
  both combatants at once, but the domain rolls the two dice by null-order (attacker first) and
  `ComputerOpponentHandler` never acts while the user is co-active — so the user was forced to roll
  the opponent's die out of order and the computer never rolled its half, stalling the turn.
  `TeleportHandler` now hands the roll-off to one combatant at a time (`setNextTeleportRoll` sets
  `activeUserIds` to the owner of the next unrolled die, attacker then defender), so the computer
  rolls its own die and the sequence advances on the first resolution.
- **SS-169** — Teleport roll-off: both human combatants get the roll button at the same time: SS-162
  had made the roll-off sequential (one active roller at a time) to unstall the human-vs-computer
  case, which also stole the simultaneous buttons from a human-vs-human roll-off. Restored
  simultaneous rolling for everyone by rolling the *rolling* player's own die instead of the
  first-unrolled one: `CurrentCombatAction.AbilityRoll` now carries `rollingUserId` (the
  dispatcher), matched against the existing `TeleportDie.userId`, and `TeleportHandler` sets both
  combatants active (`setBothCombatantsRolling`) while `rollActingUserDieOrNull` only ever rolls the
  dispatcher's own die — so neither side can roll the other's and no one double-rolls. The roller is
  surfaced to the domain as `CombatMutator.rollingUserId` (a normal attack's roller is just the
  attacker); no field was added to `NormalAttack`. The computer no longer needs a domain
  special-case: `ComputerOpponentHandler.pendingComputerActorId` returns the co-active computer (
  dropped the "user co-active → null" guard) so it rolls its half on its normal thinking delay,
  exactly like a human. Related: SS-162, SS-092.
- **SS-165** — Riposte reversal collapsed both avatars onto the original attacker: A successful
  Duelist Riposte reverses the attack (Domain emits
  `NormalAttack(attacker=Duelist, defender=Rogue, originalTurnUserId=Rogue)` — correctly, it stays
  the Rogue's turn). But the UI seated the left avatar by `turnOwnerUserId` (Rogue) while the right
  target resolved to the in-flight `defenderUserId` (Rogue too), so both bottom avatars showed the
  Rogue and the Duelist dropped to a head-icon `ClassIcon`. SS-157 had conflated "whose turn" with "
  who attacks", which only diverge after a Riposte. Added `CombatActionState.attackingUserId` (the
  in-flight action's attacker, falling back to the turn owner only when Idle) and
  `ArenaState.Combat.attackingPlayer`; `NewArenaCombat`'s `AvatarPairing` and `CombatButtons`' left
  stat column now seat the attacker instead of the turn owner. `turnOwnerUserId` is kept for the
  still-correct "whose turn" uses (`resolveTargetUserId`, auto-target, lone-opponent). Related:
  SS-157, SS-152, SS-143.
- **SS-099** — Disable Impersonate for a class the Illusionist has already copied:
  ArenaState.Combat.isImpersonateRedundant compares the currently-impersonated offensive ability (by
  type) against the selected target's class offensive ability; AbilityControls folds that into the
  Impersonate button's enabled state, so re-copying the same class (a no-op) is now disabled while
  that opponent is targeted.
- **SS-101** — Auto-target the lone opponent in a 2-player attack-only turn:
  ArenaState.Combat.autoTargetedOpponentUserId resolves to the single opponent when it's the user's
  free-choice (Idle) turn and a normal attack is the only control offered (no activated ability);
  ArenaCombat falls back to it when no opponent is explicitly selected, so the dice can roll without
  a manual target tap. Null when a real choice exists (more than one opponent, an activated ability
  is available, or it's not the user's turn).
- **SS-102** — Track Necromancer minion shields as effective hit points (display + absorption):
  SummonMinionAbility.shieldPoints (skeletons*1 + golems*2) and CharacterClass.takeDamage model
  minions as effective HP — damage drains the shield total before base hitPoints (surviving shield
  re-expressed as minions, golems first, so a half-spent golem becomes a skeleton-equivalent),
  routed through every regular HP-damage site (normal Health roll, Spell Potency direct+splash,
  Lucky Coin retaliation); Holy Shield's Wrath stays unblockable. CharacterClassState exposes
  shieldPoints + hitPointsDisplay ("7 + 2") shown in ArenaCombat and CharacterClassCard.
- **SS-183** — Rogue Dodge bonus must show in the ArmorClass stat (base + bonus): The domain folds
  the earned Dodge bonus into `CharacterClass.armorClass` (`TriggerDodge`) while tracking the raw
  amount in `Dodge.armorBonus`, so the combat armor stat displayed only the plain number with no
  sign of the earned bonus. Mirroring SS-102's `hitPointsDisplay`, `CharacterClassState` now exposes
  `armorClassDisplay` — base + bonus split as "13 + 1" (base recovered as `armorClass - dodgeBonus`)
  when a Dodge bonus is active, plain otherwise — shown by the combat ArmorClass stat in
  `CombatStats` and the `CombatAvatars` StatPill. Related: SS-102, SS-050.
- **SS-104** — All-In success must grant the Gambler a second attack: All-In set hasAnotherAttack on
  a 5/6 but nothing consumed it, so the Gambler only got one attack. CombatMutator.resolveTurnEnd
  now intercepts the turn-end of the original turn player when their AllIn has hasAnotherAttack: it
  clears the flag and keeps control (Idle on the Gambler) for one more attack instead of advancing —
  so the second attack is granted after any first-attack outcome (hit/miss/KO/defensive redirect),
  guarded by the existing one-player and conscious checks so a match-ending or self-KO first attack
  ends the turn.
- **SS-167** — Summon Minion skeletons/golems must grant the extra attack: The shield half was
  modelled (SS-102) but the extra-attack half was never wired, so a summoner attacked once and the
  turn ended. Mirroring the All-In precedent (SS-104), CombatMutator.resolveTurnEnd now runs
  grantMinionAttackIfApplicable after the All-In check: a summoner attacks once per minion held,
  control staying on them (Idle) while attacksTakenThisTurn counts up against the live
  minionAttacks (skeletonCount + golemCount), so a minion consumed as a shield mid-turn no longer
  grants its attack; resetMinionAttacks clears the count as a turn begins and SummonMinionActivated
  spends it on a summon turn so a fresh minion first attacks on a later turn. New
  attacksTakenThisTurn field round-trips through the SummonMinionAbility interface (
  domain/entity/state). Once a summoner has begun their follow-ups, only a normal attack is
  offered — OffensiveAbilityState.isMinionAttackInProgress gates the activated controls in
  deriveCombatControls and the AI's favorableOffensiveAbility, reading through the Impersonate
  wrapper. Related: SS-104, SS-102, SS-119, SS-093.
- **SS-168** — Stop swapping the large avatars between slots when the turn passes: With the
  combatant avatars now stacked vertically, seating them by role (attacker top / target bottom) and
  keying the avatar `AnimatedContent` on the ordered `(active, target)` pair made the two players
  swap slots every time the turn passed between them, when only the halo should move. The transition
  key is now `seatedAvatarUserIds` — an order-independent `Set` of who is drawn as a full avatar —
  so a turn passing between the same two seated players is no longer a change and triggers no swap,
  while a genuine change of who is seated (multiplayer) still drives the head-icon↔large-avatar
  `sharedBounds` morph; no ordering logic is imposed since slot position is meaningless. Also fixed
  a latent double-seat: a self-targeted ability (e.g. Summon Minion) names the actor as its own
  `defenderUserId`, so attacker and target resolved to the same player and the actor filled both
  slots (colliding the `userId` shared-bounds keys) while the real opponent dropped to a head icon —
  new `seatedTargetPlayer` returns null when the target is the attacker, so only the actor is
  seated. Related: SS-157, SS-165, SS-144/SS-149, SS-142.
- **SS-181** — Illusionist Mirror Image ("Keep Your Roll") corrupts large-avatar seating (full
  rework): Two-part fix. **(1) The actual corruption was shared state, not rendering:** the combat
  buttons stay live while a dispatch's network round trip is in flight, and
  `UseAbilityArguments.toDomain`'s fallback branch fabricated an action from the *user's own ids*
  whenever the live state was no longer the expected `AbilityRoll` — so a late/second Keep-Your-Roll
  tap (the local transaction applies optimistically, advancing the state within milliseconds of the
  first tap) dispatched Mirror Image with attacker = defender = Illusionist, which the Domain
  resolved as the Illusionist attacking themselves and wrote for everyone: Illusionist seated alone,
  opponent collapsed to a head icon (the same hole corrupted Riposte). Hardened both dispatch
  mappers to return null for stale input — a passive ability payload must match the ability step the
  state currently owes, an activated ability/attack can only start on the user's own idle turn, and
  an in-flight attack can only be continued by its attacker — and the ViewModel drops null
  dispatches; combat buttons also disable from the moment an action dispatches until the next
  shared-state update (`isActionInFlight` in `ArenaCombat` → `BottomControls`). Verified end-to-end
  by `MirrorImageKeepFlowTest` (armor hit → mirror trigger → entity round trip → Keep dispatch →
  resolution, plus the stale-second-tap drop) and `CombatActionDispatchTest`. **(2) Seating rework:
  ** replaced the live-state-vs-stale-snapshot pane mixing (SS-168's residue) with an explicit
  `CombatSeating` snapshot (`topSeatUserId`/`bottomSeatUserId`/`headIconUserIds`) derived by
  `seatingFor`; seats are identity-assigned (user always bottom, lone opponent top, two opponents in
  fixed id order) so an unchanged pair never rearranges — only the halo moves through Mirror
  Image/Riposte/turn handoffs; the avatar `AnimatedContent` keys on the whole `CombatSeating` and
  `CombatStage` renders purely from its snapshot, so a player leaving a seat cleanly `sharedBounds`
  -morphs to their head icon. Superseded `seatedAvatarUserIds`/`seatedTargetPlayer`/
  `targetPlayerFor`/`headIconPlayersFor`; seating tests in `CombatSeatingTest`. **(3)** Surviving to
  the second (Health-roll) mirror prompt exposed a pre-existing gap: `handleHealthRoll` triggers
  Mirror Image *before* emitting its `NormalRoll` message, so the resting dice kept showing the
  armor roll while the "Reroll the X" labels named the new Health dice.
  `ArenaMessage.MirrorImage.Trigger` now carries the interrupted roll's `firstDie`/`secondDie` (
  domain → entity, nullable-with-default for messages persisted before the fields existed → state)
  and `rolledDice()` maps the trigger to those faces, so the table always shows the roll the labels
  refer to. Found SS-188 (mirror re-check drops the doubles bonus) along the way. Related: SS-168,
  SS-165, SS-157, SS-152, SS-118, SS-169.
- **SS-184** — Don't collapse the final knocked-out player to a head icon: On a match-ending
  knockout the downed target still morphed to the small `ClassIcon` head — SS-152's "the final KO
  ends the match, so no collapse renders then" assumption missed the SS-171 finish hold, which keeps
  showing the closing KO in the combat view. `seatingFor`/`seatedTargetUserId` now take an
  `isMatchEnding` flag (threaded from `ArenaScreenState.isFinishPending` at the `ArenaCombat` call
  site); a KO'd target only drops to a head icon when knocked out *mid-match*, so during the hold
  the downed player keeps its large-avatar seat through the transition to Finished. Mid-match KO
  collapse (SS-152) is unchanged. Ties into future large-avatar KO animations. Test in
  `CombatSeatingTest`. Related: SS-152, SS-171, SS-168, SS-181, SS-123.
- **SS-114** — Reset All-In's usedThisTurn so it can be used again on later turns: All-In set
  usedThisTurn but nothing ever cleared it, so the control (enabled = !usedThisTurn) stayed disabled
  for the rest of the match after one use. CombatMutator's new-round reset (resetPlayersForNewRound,
  alongside hasActedThisRound) now clears per-turn ability state via
  OffensiveAbility.resetForNewTurn, releasing AllIn.usedThisTurn when the owner's next turn begins
  so All-In is once-per-turn, not once-per-match.
- **SS-105** — Lucky Coin on a 6 must give the Gambler a retaliation damage die: Investigated and
  confirmed working — flipLuckyCoin on a 6 triggers HighRoller, which surfaces a single retaliation
  die to the active Gambler that deals unblockable damage to the attacker. Verified in play (Lucky
  Coin + High Roller both correct) after a static audit found every layer correct and test-covered (
  domain flip→HighRoller, entity serialization, deriveCombatControls, message flow). No fix needed.
- **SS-119** — Using an impersonated activated ability destroys the Impersonate wrapper: Playing a
  copied All-In or Summon Minion overwrote the Illusionist's `offensiveAbility` with the bare copied
  ability, dropping the `Impersonate` wrapper (so they could no longer re-impersonate, and per-use
  state landed on a bare ability). Added `OffensiveAbility.withEffective` (re-wraps a mutated
  effective ability inside an `Impersonate`, identity otherwise) and routed every copied-ability
  write through it: `AllInActivated`, `SummonMinionActivated`,
  `CombatMutator.grantAllInSecondAttackIfApplicable` (now reads `.effective` too) and its
  `resetForNewTurn`, plus `CharacterClass.takeDamage` (reads `.effective` minions, writes back
  wrapped). Mirrored a UI-side `OffensiveAbilityState.effective` so the minion shield in
  `hitPointsDisplay` shows for an impersonated Necromancer. Domain tests added for impersonated
  All-In (roll 6, granted second attack, new-round reset), impersonated Summon Minion, and
  impersonated-minion damage absorption. Found while fixing SS-116.
- **SS-120** — Turn order regresses to fastest-first because hasActedThisRound is not persisted:
  `CharacterClass.hasActedThisRound` (Domain) had no counterpart on `CharacterClassEntity` (Data),
  so it was dropped on every Firebase write and read back as `false`; after the first persisted turn
  every player looked "not acted" and `CombatMutator.calculateNextPlayerInRound` re-selected the
  highest-speed unacted player instead of the next in the round. Added `hasActedThisRound` to
  `CharacterClassEntity` (default `false` for backward-compatible reads), carried through
  `toEntity()`/`toDomain()`, and reset to `false` in `resetForNewMatch()`; round-trip tests cover
  true/false survival and the reset.
- **SS-123** — Knocked-out players can be targeted and attacked: In a 3-player match the user could
  select a knocked-out opponent and attack them — `GridPlayerCard` gated targeting only on
  `isItThisUsersTurn && !isTargetLocked`, with no consciousness check, so a knocked-out opponent's
  card stayed tappable and the roll dispatched against them. Now also gated on
  `characterClass.isConscious`, so a knocked-out opponent still renders (grayed/KO'd) but cannot be
  tapped or selected; the Domain already skips knocked-out players for turn order.
- **SS-122** — Computer opponent (and any later chooser) could pick a duplicate class:
  Class-availability checks compared full `CharacterClassState` by value equality (
  `it in unavailableClasses`), but a selected player's state carries mutable stats (HP, ability
  counters) that diverge from the master template, so the match failed and the class stayed "
  available" — a second chooser (the bot in a 2-player match, or a human choosing later in 3+) could
  duplicate it. Changed `unavailableClasses` to `Set<CharacterClassType>` and keyed
  `classSelections` by `CharacterClassType` in `ArenaScreenState.prepareClassSelectionInfo`; all
  four consumers (`ComputerOpponentHandler`, the user timeout auto-pick in `ArenaScreenViewModel`,
  the `CharacterClassWheel` selection guard / grayed spokes, and the wheel's "chosen by" label) now
  compare by class type. Regression test in `ArenaScreenStateTest` (an opponent's diverged-stats
  Gambler still makes the Gambler type unavailable). Child of SS-092.
- **SS-118** — Mirror Image: show the rerolled value and its -1 in the UI: The Tempt Fate message
  only showed the post-penalty value ("rerolls a die to 3"), hiding what was actually rolled.
  `ArenaMessageState.MirrorImage.TemptFate` now also carries the raw reroll (`rawReroll`) and a
  derived `rerollDisplay` ("4 - 1", mirroring Summon Minion's "7 + 2"); the Tempt Fate strings show
  that display so the player sees the roll and the -1 penalty. Also fixed the dice tray to settle on
  the raw rolled face (`ArenaMessageRollDice` now returns `rawReroll`, the value the die physically
  showed, instead of the penalised `finalRoll`). Tests cover the mapping/derived display including a
  1 → 0 result.
- **SS-094** — Reword the Persuade ability message copy: Finalized the Persuade trigger line to lead
  with the lute (the artwork features a bard with one) — `arena_message_persuade_trigger` is now "
  Persuade! The Bard begins strumming a lute…", consistent with the "Ability!" trigger prefix used
  elsewhere; the existing "Strummed the lute and rolled X" roll lines and charm/lull copy were
  already clear and kept as-is.
- **SS-100** — Holy Shield trigger copy should read as damage, not a knockout: A normal attack that
  downs a Paladin with an unused Holy Shield no longer logs a knockout while the survival roll is
  still pending — `NormalAttackRollHandler.handleHealthRoll` now emits a `Damage`-typed message ("
  Rogue damaged Paladin for 11") instead of `Knockout` when
  `HolyShieldHandler.isHolyShieldAvailable` (a new contract-backed predicate, also reused by
  trigger/bypass) is true; the knockout still resolves via Holy Shield. Other KO-into-Holy-Shield
  paths (Spell Potency, Prepare Attack) already read as damage/"prepared", so were untouched. Domain
  test asserts the Damage message type.
- **SS-115** — Spell Potency message implies splash in a 2-player match: `SpellPotencyRoll` now
  computes the splash recipients first and carries `splashDamage = 0` in the `SpellPotency` message
  when there are none (2-player), and `ArenaMessageDisplay` branches on `splashDamage > 0` against a
  new `arena_message_spell_potency_no_splash` string ("X hit Y for N damage") so the "and splashed
  others" clause is dropped; state was always correct. spellPotencyTest now expects 0 splash in the
  2-player case.
- **SS-106** — Verify SpellPotency emits messages and state updates correctly: Audited the Mage
  Spell Potency flow end-to-end (SpellPotencyRoll, message domain/entity/state/display mappings) and
  confirmed it is correct — full diceTotal damage to the main target, diceTotal/2 splash to every
  other conscious opponent (casting mage excluded, SS-054), then resolveTurnEnd unless a KO triggers
  Holy Shield. Locked in the previously-untested message emission: spellPotencyTest now asserts the
  SpellPotency ArenaMessage's mage/target/mainDamage/splashDamage for the armor-doubles,
  health-doubles, and 3-player splash cases, plus a guard that the mage takes no self-splash. Found
  one cosmetic gap split out as SS-115 (2-player message still says "splashed others").
- **SS-107** — Add a Doubles ArenaMessage and UI indication: The doubles +1 consecutive-roll bonus (
  SS-019) was tracked but never surfaced in the combat log. Added `ArenaMessage.Doubles(userId)`
  through the full pipeline (domain, entity round trip, UI state/display, string
  `arena_message_doubles`), emitted from `NormalAttackRollHandler.addNormalRollMessage` whenever a
  normal attack roll is doubles (right after the NormalRoll message; SpellPotency short-circuits
  before this so the Mage's doubles keeps its own message). Message copy is factual ("Doubles! X
  rolled a matching pair") rather than promising "+1 next roll", since doubles on a turn-ending roll
  never carries. Tests: round-trip coverage and domain emission (doubles emits / non-doubles
  doesn't). Deep dice-total/bonus display deferred with the Arena UI overhaul (SS-068).
- **SS-051** — Review ArenaMessage field (Maybe?): The `// TODO(SS-051): Maybe?` sat above a
  commented-out `Doubles` ArenaMessage stub, deferring the decision of whether to surface the
  doubles bonus in the combat log. SS-107 resolved it by fully implementing
  `ArenaMessage.Doubles(userId)` across the pipeline, so the field was kept and both the TODO and
  the commented stub are gone. No code change — confirmation only.
- **SS-053** — Docstrings for SpellPotencyRoll: Replaced the `// TODO(SS-053): Docstring, etc.` with
  a class-level KDoc on `SpellPotencyRoll` describing the Mage Spell Potency business logic — full
  diceTotal to the main target, half (rounded down) splash to every other conscious opponent (mage
  and main target excluded), turn ends unless a downed player's Holy Shield triggers. Code was
  already clean and self-documenting; no behavior change.
- **SS-052** — LuckyCoin message approach: Confirmed the Gambler Lucky Coin messaging is correct and
  removed the stale "is this the right approach?" TODO. `ArenaMessage.LuckyCoin` (Trigger / Flip /
  HighRollerTrigger / HighRollerDamage) mirrors the established sealed-interface pattern shared by
  every other multi-step ability (Teleport, Riposte, Persuade, HolyShield), with a complete Domain →
  Data entity round trip → UI state → `ArenaMessageDisplay` string-resource mapping, all emitted by
  `LuckyCoinHandler`. No code change beyond deleting the comment. Broader per-outcome copy work
  stays in SS-093; the on-6 retaliation mechanic was verified separately in SS-105.
- **SS-091** — TEMPORARY combat dice debug overlay (remove before release): Removed the throwaway
  dice debug overlay now that combat is stable — deleted `TemporaryDiceDebugOverlay.kt` and its
  single call site in `LoadedArenaScreen` (plus the now-unused `Box`/`Alignment` imports), reverting
  the screen to the plain `ScreenContent`.
- **SS-186** — Cold reconnect crash from a departed player's messages (`getValue`): Closed, not
  reachable. The fail-fast `participants.getValue(userId)` could only `NoSuchElementException` on a
  cold ViewModel (`previousState == null`) loading a match whose log references a since-departed
  player. Per the app's rules a player never re-enters a live match cold — a long disconnect or
  closing the app is a forfeit, and the ViewModel is never lost mid-match — so the carry-forward
  `participants` map is always present and `getValue` is safe. No code change; `getValue` stays.
- **SS-117** — Dice UI: correct dice count and keep rolling through the roll's round trip: The
  DiceTray inferred its die count from the previous roll's result size, so a gesture-committed roll
  tumbled the wrong number of dice (e.g. a 1d6 ability while the last roll was 2d6, or a normal
  attack after a 1d6 ability) and the dice settled the instant the user released — before the
  Domain→Data→Domain→UI round trip returned the result. The tray now takes an `expectedDiceCount` (
  the primary roll control's `diceCount`) and, while a committed roll is in flight, shows that
  count; otherwise it mirrors the latest roll, whose own count is already correct (button abilities
  still animate via Die3d's value-change throw). The released dice now keep tumbling —
  `LocalLatestRoll` carries the roll's message id so a fresh result is told apart from the previous
  one even on identical faces — re-impulsing on a cadence until the result lands (with an
  impulse-count safety cap), then settle on it; the double-tap path commits immediately rather than
  after a fixed delay.
- **SS-187** — Duplicate class selection: stale local selection survives a class being taken: Every
  player defaults to Rogue on entry (SS-176) and `selectedClass` was sticky (
  `previousSelection ?: default`), so after another player confirmed Rogue the local user's
  selection stayed Rogue and they could confirm the already-taken class — breaking the
  no-duplicate-classes rule. Fixed in `ArenaScreenState` by dropping a now-unavailable previous
  selection before the default fallback (
  `previousSelection?.takeIf { it.type !in unavailable } ?: default`), so the confirm button and
  wheel highlight always reflect an available class. The Firebase transaction was never the problem.
  Related: SS-176, SS-122.
- **SS-116** — All-In bugs (second attack unrollable, `usedThisTurn` stuck, impersonated All-In):
  All three 2-player symptoms were one UI bug, not domain —
  `ArenaScreenState.deriveAutoTargetedOpponentUserId` suppressed the lone-opponent auto-target
  whenever any second control was present, counting even a disabled one, so after All-In resolved
  the Gambler sat idle with `[NormalAttack, All-In(disabled)]` and the dice stayed locked on "Tap an
  enemy". Fix: only enabled controls count as a real choice, so a lone disabled ability no longer
  suppresses auto-targeting. Regression test added; the intentional "don't auto-attack when a real
  ability choice exists" case stays green. Domain was correct and already covered. Related: SS-101,
  SS-104, SS-114, SS-119.
- **SS-166** — Dice: faces, disabled die, and Mirror Image reroll: Reworked the dice onto a
  persistent two-slot "dice on a table" model — a roll only updates the slots it touches; an
  untouched/disabled die dims but keeps its face (no snap to 1) and glides home rather than
  snapping; Mirror Image rerolls the exact die the player chose and dims the other; faces follow the
  authoritative slot-keyed roll value so every client agrees; and the DiceHome glide-to-stale-spot
  jank was fixed by homing to the live `homeCenter`. Related: SS-158, SS-149, SS-119.

## Profile & Auth

- **SS-061** — Investigate Firebase Authentication for user profiles: Choose the easiest and most
  robust way for users to create and own a profile. Evaluate Firebase Auth providers — Google
  Sign-In and phone (mobile) auth are the leading candidates — for a KMP (Android + iOS) app, and…
- **SS-062** — Profile deletion in Firestore: Add the ability to delete a user's profile from
  Firestore, with the corresponding domain use case and repository method.
- **SS-063** — App Drawer cleanup: The drawer currently holds ad-hoc items (a raw Display Name
  field, Tavern/Arena links). Replace it with a deliberate set of entries. The name-change
  capability moves out of the drawer and into the ProfileScreen…
- **SS-064** — ProfileScreen: Add a ProfileScreen, accessed from the app drawer (SS-063). It
  contains the name-change capability (moved out of `SettingsDrawer`), profile deletion (SS-062),
  and, once game-stat handling exists, the user's own game…
- **SS-086** — Android: native Google Sign-In token acquisition: Implement the Google account picker
  on Android via `androidx.credentials` (`CredentialManager` + `GetSignInWithGoogleOption` using the
  Firebase web client ID) and return `SocialTokenResult.Google(idToken)`.…
- **SS-087** — iOS: native Google + Apple Sign-In token acquisition: Implement Google (GoogleSignIn
  SDK → `SocialTokenResult.Google(idToken, accessToken)`) and Apple (`AuthenticationServices` /
  `ASAuthorizationAppleIDProvider` → `SocialTokenResult.Apple(idToken, rawNonce)` with a…
- **SS-109** — ProfileScreen name-change success/failure feedback: Surface the `DisplayNameResult`
  of a name save to the UI as a one-shot `ProfileEvent.DisplayNameUpdated` event and show a
  snackbar (success / name-taken / error) from a `Scaffold`-hosted `SnackbarHost`.

## Game Stats & Leaderboard

- **SS-021** — Infrastructure for saving game stats to user profiles (Firestore): Establish the
  generic mechanism for persisting game statistics onto a user's Firestore profile, independent of
  which specific stats are recorded. This is the foundation the rest of the stats work builds on…
- **SS-073** — "Gauntlet" concept: group 3 consecutive matches as one unit: Introduce a new
  game-term, **Gauntlet**, sitting above `Match` in the hierarchy (`Turn` → `Round` → `Match` →
  `Gauntlet`). A Gauntlet is a fixed run of `MATCHES_PER_GAUNTLET` (= 3) consecutive matches in the
  same Arena.…
- **SS-065** — LeaderboardScreen: Once game stats are recorded on user profiles (SS-021
  infrastructure + SS-072 schema), add a screen showing the top players. Accessible from the
  drawer (SS-063).
- **SS-071** — Forfeit vs finished leave + stats update: Distinguish a forfeit-leave from a leave
  after the match finished, and update stats only when the user actually leaves (possibly after
  multiple matches). First concrete consumer of the SS-021 stats infrastructure — the…
- **SS-072** — Define which game stats to record: Decide the concrete set of game statistics to
  persist per profile (e.g. matches played, wins, losses, forfeits, K.O.s dealt, per-class records,
  etc.). Deferred until the SS-021 infrastructure exists — design the stats…
- **SS-076** — Durable local pending-stats store for crash resilience: Persist the unflushed session
  stat delta in a DataStore as it grows during a match, and replay it on next launch (
  `FlushPendingStatsUseCase` in `AppViewModel`), so a crash/kill mid-match can no longer lose
  recorded outcomes — at-least-once stat writes that don't depend on Firestore's in-process queue.
  Also shrinks the SS-075 loss window. Related: SS-075, SS-071, SS-021.
- **SS-075** — Flush stat outcomes incrementally (shrink the loss window): Closed as superseded by
  SS-076 — the process-death loss window this targeted (only the current in-progress match survives
  a crash) is now covered by SS-076's incrementally-saved durable local delta + launch replay,
  making a per-match Firestore flush unnecessary. Related: SS-076, SS-071, SS-021.
- **SS-074** — Achievements engine + base outcome achievements: Type-safe `AchievementId` enum +
  `Achievement` sealed catalog (unlock predicate over `ProfileStats`), the four base outcome
  achievements (win match/gauntlet, win each with every class), `unlockedAchievements` on `Profile`/
  `ProfileEntity`, and `UnlockAchievementsUseCase` run after every stats write (gauntlet flush +
  SS-076 replay). Copy lives in string resources resolved in the UI; achievements kept fully in app
  code (RemoteConfig deferred after weighing metadata-only / override / rule-engine options).
  Event-based per-ability set is SS-127. Related: SS-072, SS-021, SS-127, SS-131.
- **SS-128** — Player Rating: A single `ProfileStats.rating` (derived `Long` over `overall`)
  summarizing a player's record — weighted wins + knockouts minus weighted losses + forfeits,
  floored at 0 (`Wgw=100, Wmw=25, Wko=2, Wgl=15, Wml=5, Wf=20`). Additive points reward skill and
  volume; weights are in-code const vals (RemoteConfig tuning deferred, consistent with SS-074).
  Related: SS-129, SS-130, SS-065.
- **SS-129** — ProfileScreen stats display: Surface the signed-in player's record on the
  ProfileScreen (the deferred half of SS-064), replacing the placeholder. `ProfileState` became a
  presentation model via `Profile.toProfileState()` (rating, overall `GameStats`, non-empty
  per-class records ordered by playable type, achievements unlocked/total); the screen shows rating,
  achievement progress, an overall record card, and a per-class breakdown, with an empty-state hint
  before any match. Related: SS-064, SS-072, SS-128, SS-074.
- **SS-132** — LeaderboardScreen client-side sort/filter controls: Let the player re-order the (
  already-fetched) leaderboard by Rating, Gauntlet wins, or Match wins via `FilterChip`s, with no
  extra network reads. Ranking moved to display time — `rank` removed from `LeaderboardEntry`;
  `LeaderboardScreenViewModel` holds the fetched entries + selected sort and re-derives
  `LeaderboardState` (rows re-sorted descending and re-ranked 1-based, stable ties) on
  `onSortSelected`. Sorts only the fetched top-N (Firestore orders by gauntlets won); a
  globally-correct "top by rating" would need a denormalized stored rating field. Related: SS-130,
  SS-128, SS-065.
- **SS-131** — Achievements collection screen: A drawer-accessible screen (`AchievementsScreen` +
  `AchievementsScreenViewModel`) that lists every `AchievementCatalog` entry with an Unlocked/Locked
  state and "X of Y unlocked" progress, driven by the SS-074 engine + string-resource copy (
  `AchievementId.displayNameResource`/`descriptionResource`). The ProfileScreen achievements card
  taps through to it. Per-achievement state is binary (fractional progress would need `Achievement`
  to expose a numeric value). Related: SS-074, SS-129, SS-063.
- **SS-130** — Surface Rating across the app: Show the player Rating on the LeaderboardScreen rows
  and on Tavern/TheBar traveler cards. Rating is denormalized into the traveler presence record (
  `TravelerEntity.rating`, written via `ConnectTravelerPresenceUseCase` → `connectTravelerPresence`
  from `profile.stats.rating`) so the tavern needs no extra profile reads; the leaderboard maps it
  through `GetLeaderboardUseCase`/`LeaderboardEntry`. Tavern cards show rating only when > 0 (
  computer opponents/unrated players show just the name). Leaderboard ranking still by gauntlets
  won; client-side sort/filter split out to SS-132. Related: SS-128, SS-065, SS-124, SS-132.

## Rules & Help

- **SS-066** — Explore an in-app game-rules chatbot: Investigated backends for a rules chatbot
  grounded on the CLAUDE.md / GameRulesScreen (SS-067) rules. Three options — (1) cloud LLM API (
  pay-per-use, needs a key-holding backend), (2) on-device LLM (free but platform-specific in
  KMP), (3) deterministic retrieval/FAQ (zero cost, most accurate). Conclusion: if implemented, take
  the free path — start with option 3, optionally layer option 2 later; reserve option 1 only if
  free-form quality demands it.
- **SS-067** — GameRulesScreen: A screen, accessible from the drawer (SS-063), where users can read
  the game rules and class abilities.

## UI overhaul & input

- **SS-069** — Shake-to-roll dice UX: Investigate a dice-rolling interaction where the user taps and
  holds a pair of dice (or a single die, depending on what the current step requires), shakes the
  phone to simulate rolling, and on release the existing…
- **SS-110** — Screen-transition (navigation) animations: Randomly-chosen forward transitions (
  portal / fade-through / slide+fade) plus a signature "burn to dust" disintegration on back, built
  cross-platform in commonMain via GraphicsLayer capture + Canvas particles.
- **SS-153** — Extract `ClassStyle` for class artwork + color lookups: Renamed `ClassColors.kt` →
  `ClassStyle.kt` (the single source of per-class artwork *and* color) and converted every lookup to
  `.to…` `CharacterClassType` extensions — `toBustResource`/`toAvatarResource`, `toColorGroup`,
  `toGradient`, `toColor`. `ClassGradient`'s `light`/`dark` fields became positional `topStop`/
  `bottomStop`. Colors are now defined per family (two classes share each), so `toColor` returns
  `toGradient().topStop`. Repointed `ClassHeadIcon` + `CharacterClassWheel`. Applied the SS-084
  Figma family gradients in the same pass. Related: SS-137, SS-084, SS-136.
- **SS-077** — Decide dice rendering & animation tech: Resolved — the dice are Compose-drawn 3D
  dice (`Die3d`) driven by physics and value-settling (not Lottie/Compottie), landing on the
  authoritative rolled value the ViewModel returns; shipped through the persistent-dice work (
  SS-112/SS-117/SS-149). Related: SS-069, SS-078.
- **SS-078** — Finalize dice UI (placement, size, color): Finalized through the persistent-dice and
  roll-over-everything work — a screen-spanning `DiceOverlay` with the resting `DiceHomeSlot`
  between the avatars, an always-two-dice footprint (unused die dimmed), and the hold/shake/release
  gesture living on the combat buttons. Related: SS-069, SS-077, SS-112, SS-117, SS-149.
- **SS-079** — Spinning-wheel class selector (replace CharacterClassCards): `CharacterClassWheel`
  shipped and wired into TavernScreen + ArenaChoosingClasses (replacing the `CharacterClassCards`
  pager) — single-`Animatable` rotation as source of truth, angular drag, snap-to-nearest spoke,
  click-to-select, per-class spokes/labels, and an expandable stat card; the dead pager was deleted.
  Remaining polish is tracked separately: visual fidelity (SS-084) and responsive scaling (SS-080).
  Related: SS-084, SS-080, SS-153.

## Computer opponent

- **SS-092** — Computer enemy: challenge from the Tavern, fight in the Arena: Shipped the
  client-driven computer opponent — a permanent, always-Available bot traveler in TheBar whose
  challenge short-circuits the human Firebase handshake into a direct computer-match start;
  `CoordinateArenaMatchUseCase` seeds the bot(s) into the arena before the user (so the user chooses
  first); the challenging human's client drives the bot after each match-state emission on a
  randomized "thinking" delay via `ComputerOpponentHandler` (biased policy: attack the weakest
  living opponent, activate an offensive ability only when clearly favorable, force owed
  reaction/continuation rolls); match cleanup deletes a human-less match and the
  stay-for-another-match path readies the bot too. Domain `ComputerOpponent`/`ComputerOpponents`
  registry keyed off a `computer-` id prefix so more bots and human/bot detection extend cleanly.
  Related: SS-122, SS-014, SS-101.

## Release & versioning

- **SS-070** — Investigate app version control & force-upgrade: Establish a way to gate clients by
  app version so an outdated client can be soft- or force-upgraded — important since all clients
  share one RTDB game-state schema and a stale client could read/write incompatible data.

## Dropped (won't do)

- **SS-031** — Refactor ArenaScreenState mapping: Dropped as unnecessary/outdated.
- **SS-033** — Replace "slop" + resources in CharacterClassCard: Dropped as unnecessary/outdated.
- **SS-034** — Update class-selection logic: Dropped as unnecessary/outdated.
- **SS-037** — Decide SettingsDrawer content: Dropped as unnecessary/outdated.
- **SS-068** — Lottie animation pass & UI overhaul (Arena + Tavern): Dropped — not using Lottie. The
  Arena/Tavern overhaul proceeded through concrete child tickets instead (SS-142 combat scaffold,
  SS-143–SS-152 head icons/avatars, SS-148/SS-164 flash, SS-149 dice overlay, SS-079 wheel, SS-110
  transitions).
- **SS-025** — Improve message-log animation: Dropped as outdated — the arena message log is now
  passive and no longer drives screen-state logic (SS-154), and the combat message experience moved
  to the flash overlay (SS-148/SS-164); a dedicated log-animation pass is no longer wanted.
- **SS-159** — Raise DiceHome and buttons toward the avatars; keep the stat column fixed: Dropped as
  outdated — superseded by the reworked two-half combat layout and the dice overlay/home
  positioning (SS-142/SS-149).
- **SS-160** — Combat buttons: 3D pressed style; ability buttons get class-color border: Dropped as
  outdated — folded into the shared-button direction (SS-178) and the current combat-button styling.
- **SS-163** — Flash callout hold is too short: Dropped as outdated — flash timing is now handled by
  the SS-171 dynamic finish-hold together with the SS-164/SS-185 flash work.
- **SS-203** — Duelist Prepare Attack animation: paired rapiers appear at the Duelist's sides,
  twirl a full revolution around the hands, gleam, and thrust into the target — K.O. burst on
  success, sail-past on a whiff; effect overlays hoisted out of the seating transition so K.O. cues
  survive re-seating (follow-up: SS-213).
- **SS-204** — Duelist Riposte animation: paired blades rise upright and side-by-side into a waiting
  parry stance the moment the Duelist's armor is passed (on the Riposte.Trigger message) and hold
  through the roll-off; once the roll resolves the forward blade flicks a quick deflecting beat,
  then
  the rear blade swings down into a counter-thrust — landing on the attacker with a class-color
  burst
  on a successful reversal, or sailing past on a failure. Split across Trigger/EnGarde with the cue
  held live between them; Trigger/EnGarde plumbed with duelist + attacker userIds through all
  layers.
- **SS-194** — Duelist normal-attack animation: twin swords (CombatWeapon.TwinSwords) rise into an
  erect ready pose, hold, then slash the defender one after the other — lead from the left, follow
  from the right ~half a second later — in smooth fluid arcs, each flaring a class-color burst on
  contact (armor holds → the cuts sweep past while the defender sidesteps), then both blades fade
  out
  together; attacker body plays a two-beat fence (Slash / SlashFinish / SlashWhiff). Sequencer waits
  out each cue's playbackDurationMillis (SS-201) so the long combo completes and clears itself.
- **SS-201** — Combat animation completes before its flash call-out plays: the sequencer now runs a
  message's seated-avatar cue to completion, then shows the flash callout, so an effect is never
  flashed over. Every cue reports its true full length via playbackDurationMillis (the max of its
  weapon/effect overlay and its seated-avatar gesture, including the reactor's delayed collapse tail
  that outlasts the weapon); the cue is held separately from the flash message so it can go live
  ahead of the callout (follow-up: SS-213 finish-hold undercount).
- **SS-213** — Combat animations must survive re-seating and finish transitions: a K.O. re-seated
  the stage the same frame its cue went live, so the downed player snapped to a head icon and the
  Collapse (and attacker's strike) never played; a two-player match-ending K.O. also wrongly became
  a head icon. The just-downed player is now force-seated via CombatAnimationCue.collapsingUserId /
  seatingFor's collapsingTargetUserId, derived same-frame from flashMessages so no pane swaps out
  from under the cue, released once the sequencer finishes the collapse (3+ players morph to a head
  icon after the animation) or held through the finish hold when the match ends. Finish transition
  also now sums each closing cue's real playbackDurationMillis so long closers play in full.
- **SS-195** — Necromancer normal-attack animation: a scythe summoned by magic — a long, slim,
  subtly jagged green energy crescent on an S-curved snath, wrapped in a green aura radiating off
  the blade. It manifests and glows at the caster, rises to the defender holding its upright pose
  (handle down, blade up), then winds back and reaps a menacing arc, the concave inner edge leading
  so the cut pulls inward; a glowing slash trail rides the swept arc, a class-color burst flares on
  a landed cut (held back on a miss, which still carries across), and the whole construct fades out.
  New small Cast body gesture (a gentle lean, no lunge, since the scythe carries the attack); bowed
  slash-arc math extracted from DuelistNormalAttack into shared CombatSlashArc; wired into the
  overlay host, playback-duration, and defender-reaction-delay registries; ships interactive Compose
  previews per outcome.
- **SS-196** — Bard normal-attack animation: a wooden snare drum pops up in front of the Bard, a
  wooden drumstick appears above it and then pivots on a fixed fulcrum to swing down and strike the
  white batter head (which dimples into a shaded bowl and springs back with an underdamped bounce),
  sending bard-colored sound rings rippling toward the opponent; a landed strike answers with
  bard-colored music notes rising off the drum, a miss stays silent. Shell is stained wood with
  bard-gold counterhoops (bottom hoop drawn front-arc-only so it can't show through) and tension
  rods. New Cast body gesture (drum/stick overlay carries the attack, no lunge); wired into the
  overlay host, playback-duration, and defender-reaction-delay registries; ships interactive Compose
  previews per outcome (Knockout/Damage/ArmorClassHit/ArmorClassMiss).
- **SS-197** — Illusionist normal-attack animation: the Illusionist has no weapon of its own, so it
  borrows another class's — impersonating a class plays that class's weapon, while impersonating no
  one (or a class with no built weapon yet) plays a random built weapon, fresh each attack — always
  recolored to Illusionist blue (incl. a blue variant of the Necromancer scythe via a parameterized
  ScythePalette). A shared weaponClassType resolver (impersonated, else a per-cue-id deterministic
  random from builtWeaponClassTypes) drives the overlay, playback-duration, defender-reaction-delay,
  and attacker body gesture so the weapon, its timing, and its motion agree; the flash stays blue.
  Impersonated class threaded UI-only by tracking the last Impersonate message in toStates (no
  domain/data changes); the five weapon renderers widened to internal and reused by a new
  Illusionist
  dispatch overlay; ships interactive previews per borrowed weapon plus a random one. Abilities
  split
  to SS-208 (Impersonate) and SS-209 (Mirror Image).
- **SS-199** — Paladin normal-attack animation: a massive two-handed maul (D&D-maul silhouette — a
  heavy hexagonal steel head with gold reinforcement bands, rivets, and a bright striking face, on a
  long dark-iron haft with gold langets, a bound grip, and a gold pommel) hoisted into a cocked
  overhead windup and swung down onto the defender along a bowed over-the-top arc, the blunt face
  leading. A green essence force-plume streams off the back of the head, its reach and glow scaling
  with the outcome tier (Knockout brightest → ArmorClassMiss faintest); a landed swing flares a
  green
  burst on contact, a miss whiffs past while the defender sidesteps. New paladin/ package
  (PaladinMace draws the weapon, PaladinNormalAttack the choreography + four outcome previews);
  Paladin joins builtWeaponClassTypes (Illusionist can borrow the maul) with a Slash-family body
  gesture, and is wired into the overlay host, playback-duration, and defender-reaction-delay
  registries. Parent: SS-191. Ability split out to SS-212 (Holy Shield).
- **ss-208** - The Illusionist pulls out a personal 3D hand mirror with a handle. It first shows no
  reflection, then the Illusionist's own class head icon for a moment, then crossfades through a
  blurry morph into the head icon of the class it is impersonating — the settled reflection washed
  in
  an Illusionist-blue tint that comes in after the change. Offensive ability: copy a living
  opponent's offensive ability until end of match or another is chosen.
- **Done (first pass):** the full choreography — mirror rise, empty glass, own-face reveal,
  blurred crossfade, blue tint, and mirror lower — in `ImpersonateOverlay.kt`, with a new
  `CombatAnimationCue.Impersonate` cue carrying the Illusionist's userId and impersonated class,
  `ArenaMessageState.Impersonate` plumbed with `illusionistUserId`/`impersonatedClassType`, duration
  wired into the flash sequencer, and per-class interactive previews. The hand mirror is a
  hand-drawn
  Canvas shape (rim/glass/handle) and the blur is a jittered-copies fake for now.
- **Follow-up:** polish/tune proportions, colors, and timing once seen on-device; consider a truer
  3D
  mirror and a real blur if a portable one lands. Then close the ticket.
- **Cue sources:** `ArenaMessageState.Impersonate`.
- **Parent:** SS-191. Split out of SS-197.
- **SS-209** — Illusionist Mirror Image animation: when struck, the seated Illusionist splits into
  two blurry, translucent copies of itself, held while the reroll is chosen; on the resolving roll
  the struck copy poofs (fades, rises, grows) while the real copy slides back to center,
  re-solidifies, and reacts to the outcome — dodging a foiled attack, or flinching/collapsing under
  one that lands. Built into the seat itself (not an overlay) so the avatar art literally splits:
  `GestureClassAvatar` renders the art twice via a new `MirrorSplitClassAvatar` whenever
  `AvatarGestureState.mirrorSplit > 0`, driven by three new channels (`mirrorSplit`, `mirrorReturn`,
  `mirrorFakeVanish`) and a `playMirrorImage` path in `rememberAvatarGesture` that runs the poof,
  return, and reaction in parallel without the `snapToRest` that would drop the split mid-resolve. A
  new `CombatAnimationCue.MirrorImage` (phase + outcome) is derived from the three
  `ArenaMessageState.MirrorImage.*` states, each now carrying `illusionistUserId` resolved from the
  preceding armor-break roll (the Dodge pattern); `collapsingUserId` keeps a knocked-out Illusionist
  seated. Ships three interactive previews (miss/hit/knockout) that stand a real seated avatar and
  loop the Split→Resolve cue. Tunable knobs (`MIRROR_SPLIT_OFFSET_FRACTION`, `MIRROR_COPY_ALPHA`,
  timings) at the top of `CombatAvatarGesture.kt` for on-device polish.
- **Cue sources:** `ArenaMessageState.MirrorImage.*`.
- **Parent:** SS-191. Split out of SS-197.
- **ss-198** - The Mage's normal attack is a cast projectile with several elemental variations,
  picked
  at random per attack (deterministic from the cue id). Each is a focused, subtle projectile that
  flies from the Mage to the target: an **Icebolt** (ice bolt with a snowflake/ice trail, shatter
  on hit), a **Lightningbolt** (a single arc of lightning), and a **Fireball** (a flaming ball that
  explodes on hit). Three elements this pass; the framework is built to grow toward six.
- **Tie to SS-210:** the normal attack and Spell Potency never co-occur — the domain resolves a
  Mage's doubles roll as Spell Potency and returns without a `NormalRoll` message, so a non-doubles
  attack shows the focused projectile (SS-198) and a doubles attack shows the amplified AoE
  (SS-210). SS-210 reuses the same element renderers, amplified.
- **Done:** the `mage/` overlay package, the `MageSpellElement` enum + a deterministic per-cue-id
  element picker, and all three focused projectiles — Icebolt, Lightningbolt, Fireball — each with
  its own trail, head, and impact, dispatched through element-agnostic draw seams
  (`drawMageBolt`/`drawMageImpact`/`drawMageAtmosphere`). Adding elements 4–6 is one enum entry plus
  its draw branches.
- **Cue sources:** `ArenaMessageState.NormalRoll` (attacker side).
- **Parent:** SS-191. Abilities split into SS-210 (Spell Potency) and SS-211 (Teleport).
- **ss-210** - The amplified version of the SS-198 normal-attack projectile. A Mage's doubles roll
  triggers Spell Potency (dice-total to the main target, half splashing to every other conscious
  opponent, then end turn). The amplified cast incorporates the focused projectile — the singular
  bolt is fired **X** times at the main target, where **X** is the total roll (`mainDamage`) — and
  wraps it in a large, extravagant area-of-effect: for Ice, a **Blizzard** — a swirling, dome-shaped
  cloud of icy wind over the arena. Lightning gets a **Thunderstorm** (dark cloud with lightning),
  Fire an **Inferno** (pillars of fire, red glow).
- **Done:** the `CombatAnimationCue.SpellPotency` cue (mage + main-target seats, `mainDamage`
  copies), all three amplified AoEs — Blizzard, Thunderstorm, Inferno — each filling the whole
  battlefield centered on the **opponent** (never the Mage), and the Mage-Cast / main-target-flinch
  seated gestures wired into the sequencer, reaction-delay, and duration registries. The barrage
  fires in staggered **waves** (up to 4 bolts at once), gathered across a horizontal band at the
  Mage and blasted across the opponent, each bolt bowed sideways so the volley reads alive rather
  than single-file.
- **Splash HP:** decrementing the splashed opponents' class-head icons is **SS-214**.
- **Known first-pass gap:** the main target plays a heavy flinch, not a collapse, even when the
  cast is lethal — the domain resolves Spell Potency without a `NormalRoll`/knockout message, so the
  cue carries no outcome and `collapsingUserId` is null. A lethal cast therefore morphs the target
  straight to a head icon rather than collapsing in-seat. Revisit alongside SS-214 if it reads
  poorly on-device.
- **Cue sources:** `ArenaMessageState.SpellPotency`.
- **Parent:** SS-191. Split out of SS-198.
- **ss-211** - Portal animation. Defensive ability: when armor is passed, roll off 1d6
  with the attacker; if the Mage rolls higher, escape the attack.
- **Cue sources:** `ArenaMessageState.Teleport.*`.
- **Parent:** SS-191. Split out of SS-198.
- **ss212** - Defensive ability: once per match, when K.O.'d, roll
  2d6; on 10/11/12 the Paladin survives with 1 Health and deals unblockable 1d6 to the attacker.
- **Cue sources:** `ArenaMessageState.HolyShield.*`.
- **Parent:** SS-191. Split out of SS-199.
- **SS-200** — Attacks-remaining stat in AvatarStatsBar (turn owner only): domain-owned
  `CharacterClass.attacksRemaining` shown as a stat + icon for the current turn owner, reset at turn
  start and decremented/incremented as attacks are spent or bonus attacks are granted.
- **SS-193** — Gambler ability animations: All-In and Lucky Coin ability animations built on the
  SS-191 seam. Cue sources: `ArenaMessageState.AllIn`, `ArenaMessageState.LuckyCoin.*`.
- **SS-214** — Spell Potency splash: decrement HP off the class-head icons: splashed conscious
  opponents get a per-head-icon hit-point decrement animation so a multiplayer Spell Potency reads
  as hitting the whole table, not just the seated main target. Cue: `ArenaMessageState.SpellPotency`.