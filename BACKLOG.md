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

### SS-256 — Rearchitect combat animation gating around a ViewModel-owned playback queue

- [ ] Not completed
- **Goal:** Fix the high-priority bug where the CombatButtons disappear permanently mid-match (game
  becomes unplayable), and prevent the whole class of bug by moving animation gating off the
  composables into a single, serialized, bounded queue in the ViewModel.
- **Root cause (the real one):** the arena message log is a rolling 30-message window
  (`appendCapped` / `MAX_RETAINED_MESSAGES` in `data/arena/utilities/Common.kt`), and every update
  reports `numberOfNewMessages`. Both the old in-composable sequencer and the first cut of the VM
  sequencer computed fresh work with `messages.drop(playedCount)`, which only holds while the log
  grows. Once a match passes 30 messages (repeated Summon Minion reaches it quickly) the drop stops
  lining up with what is actually new, playback desyncs, and the controls disappear. Moving the loop
  to the ViewModel did not fix this — the windowing mismatch was the bug the whole time.
- **Fix:** `CombatPlaybackSequencer` (`ui/screens/arena/playback/`) runs one bounded drain coroutine
  in `viewModelScope` and now derives fresh steps from `numberOfNewMessages`
  (`messages.takeLast(numberOfNewMessages)`), so windowing no longer desyncs it; every wait is
  bounded with a crash-recovery path so the controls can't be left hidden. The UI keeps its
  `rememberHeldDuringSequence` seating/stats rendering, gated on `ArenaScreenState.isPlayingBack`.
- **Remaining (owner to build the real thing):** replace the mechanical port with a plain queue of
  already-built animation steps that a single consumer polls off one at a time — no `emit`/`compose`
  indirection, no held-state gymnastics. Also drop the `ArenaScreenState` playback default args,
  unify the two "last roll" trackers (`CombatPlaybackSequencer.lastDiceRoll` vs the UI's
  `CommitDiceRolls.lastRoll`), and close the CONFLATED `diceSettled` stale-signal race.

### SS-257 — Move combat button/control state into the ViewModel; consolidate the control + argument types

- [ ] Not completed
- **Problem:** Combat button *view* state is computed inside composables instead of the VM, and the
  control/argument types are tangled. TODOs tagged `SS-257`.
- **Move to VM (`ArenaMatchState.Combat` / VM mapping), out of composables:**
    - `CombatButtons.kt` — the whole button-visibility/enablement derivation, incl. the teleport
      per-user roll-suppression latch (`hasReachedTeleportRollBeat`); a user must not be able to roll
      twice for a roll-off. (`// TODO: Move state to view model layer`, teleport note.)
- **Consolidate types (they overlap and confuse):** `ArenaCombatControl`, `CombatButtons`,
  `AttackArguments`, `CombatActionArguments`/`UseAbilityArguments`, and the two
  `*.toDomain()` `when`s in `CombatActionState.kt` (likely collapsible to one). `CombatControlActions`
  suggests `ArenaCombatControl` may be removable entirely.
- **Sources:** `model/ArenaCombatControl.kt`, `ui/CombatControlActions.kt`,
  `ui/combat/CombatButtons.kt`, `viewmodel/handlers/CombatHandler.kt`, `model/CombatActionState.kt`.
- **Related:** SS-029 is the sibling piece (move combat-action *availability* derivation
  `deriveCombatControls` into the domain + string resources). Do them together.
  [[derived-ui-state-in-state-model]].

### SS-258 — Refactor CombatAnimationCue + AvatarGesture (build gestures at map time, split files, dedupe shared gestures)

- [ ] Not completed
- **Core idea:** gestures are known at VM/message-mapping time (top & bottom seats are known) — build
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
    - Fill the missing/uncertain cue→overlay branches in `animation/common/CombatAnimationOverlays.kt`
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
      `addNewMessageDuringCombat` "combat-only", + the Setup handling) belongs in a domain MessageLog,
      not two ugly UI functions.
    - Dedupe `CharacterClassMessageInfoState` vs `ParticipantClass` (same shape) in
      `ArenaMessageState.kt`.
    - Rename the domain `NormalRoll` outcome type → `NormalRollOutcomeType` (`domain/.../ArenaMessage.kt`).
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
    - `handleScreenStateUpdateError`: decide mid-match error behavior (currently: show error + leave).
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
    - When the Illusionist impersonates an ability, overlays use the *acted class's* color; the accent
      should be the **Illusionist's** color instead (`gambler/GamblerAllIn.kt` accentColor default is
      the tell — every borrowed ability overlay has the same issue).
    - Verify the dice-kickoff path in `DiceStateHolder.kt` works for Impersonate (and other
      dice-consuming abilities). [[illusionist-borrows-weapons]], [[dice-roll-must-be-roll-bearing]].

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
  SS-262, [[illusionist-borrows-weapons]]. Ship interactive previews ([[combat-animation-previews]]).

### SS-266 — SummonMinion: Illusionist portal should be illusionist blue, not green

- [ ] Not completed
- **Goal:** When the Illusionist impersonates Summon Minion, the summoning portal should render in
  Illusionist blue rather than the Necromancer green. Currently the portal color is baked to the
  necromancer palette regardless of the actor.
- **Source:** `animation/necromancer/NecromancerSummonMinion.kt` (`summonMinionScene` / portal
  draw). The actor's real class is available on the message via `actingClassType` (SS-262); thread a
  portal color off it (necro green vs illusionist blue). Related: SS-262,
  [[illusionist-borrows-weapons]]. Ship interactive previews ([[combat-animation-previews]]).

### SS-185 — Pare back which messages flash

- [ ] Not completed
- **Goal:** SS-164 makes **every** message flash (round/turn/connection/info and every intermediate
  roll step). Decide which messages should be **log-only** and stop them flashing, so the flash
  channel stays punchy and reads as combat narration rather than echoing the whole log.
- **Mechanism:** `ArenaMessageState.flashCallout` (in `FlashCallout.kt`) already returns
  `FlashCallout?` and `ArenaScreenState.flashMessages` already filters nulls — return `null` from
  the
  branches that should be log-only. No structural change needed.
- **Open for owner:** the exact keep/cut list (likely cut: `NewRound`, `NewTurn`,
  `ConnectedToArena`,
  `ChoseCharacterClass`, and intermediate roll steps like `Teleport.Roll`,
  `LuckyCoin.HighRollerTrigger`).
- **Related:** SS-164 (one message → two resources).

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

### SS-179 — Message text styling: backgrounds, neutral text color, class-color class names everywhere

- [ ] Not completed
- **Goal:** A consistent text-styling pass across **all** messaging — Finished-state floating text,
  the message log, and the flash overlay:
    - Floating text in the Finished state needs a **background** (black at ~50% opacity or less) so
      it's readable without hiding the background art.
    - Text bodies use a **theme-neutral color**; any **class name** within a message renders in
      **that class's color**. This applies everywhere a class name appears (message log, flash,
      Finished, etc.).
- **Source:** `MatchFinishedContent.kt` (the floating `Text`s — headline, winner banner, recap),
  `ArenaMessageLog.kt`/`ArenaMessageDisplay`, and `CombatFlashOverlay.kt` (`FlashText`). Class
  names already resolve via the className map (SS-097); class colors via `ClassStyle`/
  `classFamilyColors`. Likely an `AnnotatedString` builder that colors class-name spans from one
  shared place.
- **Related:** SS-097 (class names in messages), SS-153 (class colors), SS-164 (flash
  wording/color),
  SS-029 (string resources).

### SS-029 — Move combat-action availability to the domain + StringResources in ArenaCombatControl

- [ ] Not completed
- **Source:** `ArenaCombatControl.kt` (`// TODO(SS-029): Always StringResources`), `deriveCombatControls`
- **Goal (Phase 2 of the ArenaScreenState TODO cleanup):**
  - Move the combat-action *availability* logic (`ArenaState.Combat.deriveCombatControls`) into the
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

### SS-226 — MessageLog must stop above DiceHome, not be covered by it

- [ ] Not completed
- **Symptom:** The message log is partially covered by the DiceHome / dice tray at the bottom of the
  arena, hiding the most recent lines behind the dice.
- **Goal:** The log's lower bound sits **above** DiceHome so no line is ever occluded by the dice
  tray. Keep the log as an overlay over the combat content ([[message-log-overlays-not-pushes]]) —
  it must not push combat content up — but bottom-constrain it to end above DiceHome.
- **Source:** `ui/screens/arena/ui/message/ArenaMessageLog.kt` and its host in the arena layout
  (`LoadedArenaScreen.kt` / bottom-controls / DiceHome). Anchor the log's max height/offset to the
  DiceHome top.
- **Related:** SS-225 (auto-scroll), SS-159 (raise DiceHome/buttons), SS-182 (top inset).

### SS-252 — Finished state: show a version of the arena message log (UI + location TBD)

- [ ] Not completed
- **Goal:** Surface a version of the arena message log in the **Finished (match-over)** state so
  players can review what happened after the match ends. The **exact UI and placement are to be
  specified by the owner at the start of this work** — do not build until the spec is given.
- **Source:** `ui/screens/arena/ui/MatchFinishedContent.kt` for the host; reuse the grouped log
  rendering from SS-251 (`ui/screens/arena/ui/message/ArenaMessageLog.kt`) rather than duplicating
  it.
- **Notes:** Follow-up to SS-251 (build that grouped log first). During combat the log lives behind a
  tap-to-open handle; the Finished-state version likely wants a different affordance/location — hence
  owner-to-spec. Related: SS-227 (Finished-state readiness), SS-179 (Finished-state text styling).

### SS-253 — Class-color every className across all arena messages

- [ ] Not completed
- **Goal:** Every message that contains a class name should render that name in the class's family
  color, everywhere it appears in the log and flashes — not just the handful (NewTurn header,
  ChoseCharacterClass) that do today.
- **Source:** `ui/screens/arena/ui/message/ArenaMessageLog.kt` (`classHighlightedText` /
  `ArenaMessageItem`) and `ui/screens/arena/ui/message/ArenaMessageDisplay.kt` (`getDisplayString`).
  The message states carry `attackerClassType` / participant class info already; thread the class
  type(s) into a colored `AnnotatedString` builder rather than plain strings.
- **Notes:** Follow-up to SS-251 (owner flagged this as a separate ticket). Watch messages with two
  class names (e.g. attacker + defender) — both should be colored. Reuse the class color source
  (`CharacterClassType.toColor()`).

### SS-254 — Message log: gradual drag-to-dismiss (ModalBottomSheet-style), not all-or-nothing

- [ ] Not completed
- **Symptom:** The open message log dismisses via an overscroll threshold
  (`rememberOverscrollDismissConnection` in `ArenaMessageLog.kt`): scrolling past the end snaps it
  closed all at once.
- **Goal:** Replace this with a **gradual, scroll-linked dismiss** like a `ModalBottomSheet` — as the
  user scrolls/drags past the edge the log translates with the gesture and only dismisses once past a
  distance/velocity, and springs back otherwise. Reuse the built-in bottom-sheet nested-scroll /
  anchored-draggable behavior rather than the custom threshold accumulator.
- **Source:** `ui/screens/arena/ui/message/ArenaMessageLog.kt` and the unfurl host
  `MessageLogOverlay` in `ui/screens/arena/ui/combat/CombatMessages.kt`.
- **Notes:** Follow-up to SS-251.

### SS-255 — Message log open/insert animation reads wrong (topmost line doesn't move)

- [ ] Not completed
- **Symptom:** When the log opens (and when a new line is revealed), the **topmost** message stays put
  while everything below it slides — it should all move together as the content shifts.
- **Goal:** Make the log content animate as a unit on open and on insert (the whole column slides),
  rather than the first item anchoring while the rest animate via `animateItem()`.
- **Source:** `ui/screens/arena/ui/message/ArenaMessageLog.kt` (per-item `animateItem()` +
  most-recent-first ordering + auto-scroll-to-top) and the unfurl height animation in
  `MessageLogOverlay` (`CombatMessages.kt`). Likely needs the unfurl to translate the content down
  rather than reveal it top-anchored.
- **Notes:** Follow-up to SS-251; needs on-device animation iteration.

### SS-227 — Finished state: show which players have readied up (like ChallengeDialog)

- [ ] Not completed
- **Goal:** In the Finished (match-over) state, once a player signals they are ready (e.g. "Go
  Again" / rematch), surface **who is ready** to every client, the same way the ChallengeDialog
  displays readiness. A player readying up should be reflected in real time for all participants.
- **Source:** `ui/screens/arena/ui/MatchFinishedContent.kt` for the display; mirror the readiness
  presentation already used by the ChallengeDialog (find its ready-state rendering and reuse it).
  Ready state flows through the shared arena state (Domain → Data → UI), not derived from the
  message log ([[never-search-messages-for-decisions]]).
- **Notes:** Gauntlet rule — after the final match, "Go Again" starts a new Gauntlet (back to class
  selection); confirm the readiness display is correct for both between-match and end-of-Gauntlet
  rematch. Related: SS-179 (Finished-state text styling).

### SS-234 — Multiplayer no-target avatar: silhouette, "?" gray stats, slanted "Choose a target"

- [ ] Not completed
- **Goal:** In a multiplayer match, when the current player has **no opponent selected**, the target
  seat renders a placeholder instead of a real avatar:
    - A new PNG avatar art, `silhouette_avatar` (a plain silhouette).
    - The stat numbers (speed / armor / hit points) shown as **question marks** in a **gray** color
      (not the class colors).
    - The text **"Choose a target"** written **across** the avatar, angled/slanted a bit.
- **Source:** The target-seat rendering in `ui/screens/arena/ui/combat/ArenaCombat.kt` /
  `AvatarStatsBar`; stat icons per SS-136/SS-138. Add a no-target branch that swaps the avatar art,
  grays + "?"s the stats, and overlays the rotated "Choose a target" label.
- **Notes:** The `silhouette_avatar` PNG **may not exist in the repo yet** at ticket-writing time —
  when working this ticket, confirm the asset is present (downsample per
  [[downsample-figma-raster-assets]]) or request/export it first. Related: SS-144 (target-selection
  transition), SS-136 (head icon).

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

### SS-205 — Necromancer Summon Minion animation

- [ ] Follow-up pending
- **Goal:** every outcome opens the same green glowing portal at the summoner's feet, then diverges:
  a fizzle drains the portal gray and closes it; a skeleton (white, green-eyed) or a larger brownish
  clay golem (green runes/eyes) rises up out of the portal and then fades; a heal (skeleton-and-heal
  or golem) plays a red-green burst off the Necromancer with a few `ic_health` icons floating up and
  fading. Portal + creatures + heal drawn in the overlay; summoner plays the existing Cast gesture.
- **Done (first pass, shipped):** portal/fizzle/heal/float-up and the choreography, wiring, and
  per-outcome previews all landed; the skeleton and golem are hand-drawn Canvas shapes for now.
- **Follow-up:** replace the hand-drawn skeleton and golem in `NecromancerMinions.kt` with an image
  asset of each (keep the portal, heal burst, and rise/fade choreography). Then close the ticket.
- **Cue sources:** `ArenaMessageState.SummonMinion`.
- **Parent:** SS-191. Split out of SS-195.

### SS-206 — Bard Encore animation

- [ ] Follow-up pending
- **Goal:** the moment Encore triggers (the Bard has dealt damage or scored a K.O.) the whole arena
  drops into concert dark — the screen darkens on all sides to a spotlit stage with a 3D standing
  microphone, and the crowd is made of numerous small copies of every class avatar packed into the
  distance. The crowd and the light off the mic keep moving so the scene is alive while the roll is
  awaited. On the roll the mic throws off sound and music notes; on a landing the crowd goes wild
  (success) or boos (failure). A failed Encore grays the music the way the other Bard failures do.
  After Encore finishes the lights come up and the darkness lifts.
- **Done (first pass, shipped):** full-screen concert scene with the darkness fall/lift, spotlight
  cone + pool, receding crowd of all eight avatars (ambient sway, wild-jump vs boo-shake), 3D
  chrome microphone (radial-shaded head with sweeping glint, banded stand, domed base), music-note
  burst + sound rings on the roll, the gray drain on failure, the vignette frame, and the cue/phase
  wiring (`EncorePhase` SetStage/Success/Failure) plus success/failure previews. Everything is
  hand-drawn Canvas over the avatar PNGs (`BardEncore.kt`, `EncoreStage.kt`).
- **Note:** the concert scene now covers the **whole screen** (`EncoreFullScreenOverlay`, hosted in
  the outer arena `Box` **below** `BottomControls`), so the darkness reaches the edges while the roll
  buttons float on top of it and stay visible/usable — the arena darkens to a smooth gradient behind
  the buttons rather than stopping at the stage region.
- **Follow-up:** Replace the hand-drawn microphone with an image asset for a truer 3D look (keep
  the
  glint, note behaviours, gray drain, and darkness fall/lift choreography); polish/tune the crowd
  density, proportions, colours, and timing once seen on-device; and consider a real crowd-noise
  cue for the wild/boo beat. Then close the ticket.
- **Cue sources:** `ArenaMessageState.Encore.*`.
- **Parent:** SS-191. Split out of SS-196.

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

## Domain layer

### SS-048 — Die-value enum

- [ ] Not completed
- **Source:** `DiceRoller.kt:16` (`// TODO Enums for values 1 through 6?`)
- **Goal:** Consider a type-safe enum for die faces (1–6) instead of raw Ints.

---

## Bugs

### SS-180 — Random character choice almost always picks Necromancer

- [ ] Not completed
- **Symptom:** The random "choose a character" path has landed on Necromancer 5+ times in a row,
  i.e. it isn't actually random (or is biased to one class).
- **Source:** The random class picks use `availableClasses.randomOrNull()` —
  `ArenaScreenViewModel.kt:511` (user timeout auto-pick) and `ComputerOpponentHandler.kt`. Suspect a
  non-uniform/iteration-order source (a `Set` iterated deterministically, a stale RNG, or selecting
  off a fixed-order collection). Confirm which affordance the owner means (manual "random" control
  vs. timeout auto-pick) and verify the selection is drawn uniformly from the live available
  classes.
- **Notes:** Necromancer is index 3 in `getAllClasses()` order — check for an off-by-index or
  first/last-of-collection bug masquerading as random.
- **Related:** SS-122 (class availability by type), SS-092 (computer opponent).

### SS-246 — Teleport re-rolls when it shouldn't (roll-off resolving before rolls settle)

- [ ] Not completed
- **Symptom (intermittent):** In a Teleport roll-off the defender re-rolls even when they clearly
  won. Observed: Mage rolled a 2, attacker (Bard) rolled a 1, no doubles bonus in play, yet the
  Mage was made to roll again. 2 vs 1 with no bonus should let the Mage escape outright — a strict
  higher roll, not a tie — so the re-roll is wrong. Does not reproduce every time.
- **Suspected cause (owner's read):** it is possible to roll for Teleport before the other
  combatant's roll has resolved, so the outcome is evaluated against a stale/incomplete roll value
  (a race). Likely a regression around the settle-before-result / roll-off sequencing work.
- **Not a display-only issue:** this is an actual extra roll being performed, not just a missing
  badge — confirmed there was no doubles bonus, so it is not the legitimate tie/reroll path.
- **Investigate:** the roll-off gating (can a roll be submitted before the opponent's roll settles?)
  and how the domain compares attacker vs defender rolls (`TeleportHandler.handleTeleportOutcome` /
  `rerollTeleport`) relative to when each roll-off roll is committed/emitted.
- **Related:** SS-244 (settle-before-result timing, per-die roll-off sequencing), SS-245 (tie →
  reroll communication), SS-242 (separate roll-off dice), [[dice-roll-must-be-roll-bearing]],
  [[message-log-is-passive]].

### SS-235 — Persuade: crash + multiple UI bugs (owner to spec the intended UI before work)

- [ ] Not completed — **do not start until the owner describes the intended UI.**
- **Symptom:** Bard's Persuade is badly broken — multiple bugs plus a **crash**, to the point the
  exact misbehavior can't be pinned down from observation alone. The owner's read is that the lack
  of a clearly-defined intended UI is part of the problem.
- **Action when picked up:** the **owner will first fully describe how the Persuade UI should
  behave** (armor-passed prompt, the 1d6 roll, 5 = stop, 6 = stop + redirect to a legal non-Bard
  target, and the multiplayer-only redirect constraint). Only then implement against that spec.
  Reproduce and capture the crash stack as step one.
- **Source:** `domain/.../ability/passive/` Persuade handler + Persuade combat controls +
  `bard/BardLute.kt` / `bardUserId` plumbing (SS-207).
- **Related:** SS-207 (Persuade animation), game rule: Persuade redirect needs ≥2 alive non-Bard
  players.

### SS-267 — Combat queue can hang when a gesture participant isn't a seated avatar (SpellPotency splash targets)

- [ ] Not completed
- **Introduced by:** the keyed animation-completion rework in `ArenaMatchCombatQueue`
  (`MessageAnimationCoordinator`). Each message's expected-animator set is seeded up front from the
  message: `AnimationKey.FlashMessage`, `AnimationKey.AvatarOverlay`, and one
  `AnimationKey.AvatarGesture(userId)` per entry in `gestures.keys`. The coordinator only resolves
  once **every** expected key has reported via `onAnimationComplete`.
- **Symptom / hazard:** a gesture is only reported by a mounted, seated `GestureClassAvatar` (top =
  targeted seat, bottom = attacker seat). If a message's `gestures` map names a userId that is **not**
  seated — rendered only in the stands (`ClassIcons`), or otherwise not composing a
  `GestureClassAvatar` — that `AvatarGesture(userId)` key is never reported, the coordinator's
  `completion` never completes, `await()` blocks forever, and the queue stalls. That's the same
  end-state SS-256 fought (combat controls never return, match becomes unplayable).
- **Identified case:** SpellPotency. Its `gestures` map carries the mage (`Cast`) and the main target
  (`Collapse`/`FlinchHeavy`). Confirm whether the main target (and any secondary/splash target that
  ever receives a gesture) is guaranteed to be one of the two on-screen seats; splash victims live in
  the stands and must never end up as an expected gesture key while unseated.
- **Fix direction:** restrict the expected-animator set to participants that will actually mount and
  report (only the seated attacker/target), or give the coordinator a bounded fallback so an
  unreported animator can't hang the whole queue. Decide which in `getExpectedAnimationKeys`.
- **Related:** SS-256 (the queue/gating rearchitecture this rides on), [[never-drain-loop-combat-queue]],
  [[combat-queue-signal-reentrancy]], [[combat-button-visibility-isanimating]].

## Profile & Auth

### SS-249 — Keep users signed in with a local session record (fix re-login + duplicate accounts)

- [ ] Not completed — **fixes the SS-223 symptom for good.**
- **The bug (user-facing):** on some devices Firebase Auth loses the saved session between app
  launches. When that happens the app (1) shows the "choose your name" sign-in screen again, and
  (2) if the user continues, creates a **brand-new anonymous account** — orphaning their existing
  profile and stats. Confirmed on the Galaxy S9 (see SS-223). The user must never be asked to sign
  in again once they already have, and must never lose their stats.
- **The fix:** keep our own tiny durable record of the signed-in user in local storage (DataStore —
  already used in the app), and use it alongside Firebase to decide the launch screen. Never trust
  Firebase's session alone.
    1. **On successful sign-in / profile load**, save locally: `userId`, `displayName`, and sign-in
       method (guest vs Google).
    2. **On launch**, if Firebase has a live session → behave as today. If Firebase has **no**
       session but a local record exists:
        - **Google:** silently re-authenticate with the stored Google account (Credential Manager
          can return the already-authorized credential with no prompt) → same account restored, no
          dialog.
        - **Guest:** skip the name dialog, keep the stored `displayName`, and re-establish the
          session **without** discarding the old account. (An anonymous token can't be resurrected
          from just the uid; decide during implementation whether to re-link or do a one-time silent
          repair — the hard rule is: do NOT create a new account when a local record exists.)
    3. **Never mint a new anonymous user when a local record is present** — this is what stops the
       stat-orphaning today.
- **Done when:** on the S9, after signing in once (guest or Google), relaunching the app goes
  straight in — no name dialog, no duplicate account, stats intact — every time.
- **Source:** local record in DataStore; `AppViewModel.loadScreenState` (the launch gating);
  `FirebaseAuthenticationRepository`; `CreateProfileUseCase` (the code path that currently mints a
  new anonymous user); `AndroidSocialSignInProvider` (silent Google re-auth).
- **Related:** SS-223 (root-cause investigation), [[sign-in-not-cached-device-specific]],
  [[connectivity-resilience-priority]].

### SS-223 — Google Sign-In no longer cached on a physical device (regression)

- [~] Diagnosed 2026-07-25 — **device-specific, not a code regression.** Reproduces only on the old
  Galaxy S9 (Android 10); does **not** occur on a Pixel 7a. **Fix tracked in SS-249** (local session
  record); this entry keeps the root-cause investigation.
- **Symptom:** On a physical Android device, sign-in is **not cached** — the user is not kept signed
  in across launches and must re-authenticate. Affects **both Google and guest/anonymous** (so it is
  auth-level, not a Google/Credential-Manager/SHA issue).
- **Findings (traced live on the S9 via adb):**
    - Sign-in always succeeds — guest and Google both create the user + profile (`createProfile
      result=SUCCESS`); the **encrypted session is written to disk**
      (`shared_prefs/com.google.firebase.auth.api.Store.*.xml` → `FIREBASE_USER=ENCRYPTED:…`, with a
      self-contained Tink AES-GCM keyset, so it is not an Android-Keystore decryption failure).
    - **Persistence is a timing race on this device:** kill the app ~2s after sign-in → session lost,
      dialog on relaunch; kill ~15s after → session survives into the app. The encrypted session is
      not durable for several seconds after sign-in and the S9 loses it if the process dies inside
      that window. The emulator and Pixel 7a persist correctly with short gaps.
    - Secondary damage: on a lost session the app treats the user as new and mints a **fresh
      anonymous user, orphaning prior stats** (gate: `isSignedIn=false`, or `isSignedIn=true` but
      `getProfile==null`). This orphaning is the part worth fixing in code regardless of the device.
    - Ruled out: `firebase-bom` pin (it is `androidInstrumentedTest`-only, not app runtime); any
      `signOut`/persistence-disable (none exist); the `isSignedIn` gate logic. GMS log noise
      (`SecurityException: Unknown calling package name 'com.google.android.gms'`, Phenotype
      `DEVELOPER_ERROR`) appears even in working runs → benign.
- **Dependency update — tried, does NOT fix it (2026-07-25):** GitLive `2.4.0→2.5.0` is a no-op for
  this (both pin native `firebase-auth 23.2.1`). Forcing native `firebase-auth 24.2.0` (app-runtime
  `firebase-bom 34.16.0`) builds and runs (GitLive 2.4.0 runtime-compatible with native 24 on the
  auth path), but a definitive S9 test — sign in, wait 20s, force-stop, relaunch — still **lost** the
  session (the encrypted `Store` file intermittently isn't even written). Reverted; it's an
  unsupported GitLive+native-24 combo with no benefit here.
- **Options if picked up:** (a) accept as an old-device quirk (Pixel 7a fine, app unreleased); (b)
  app-side resilience — stop orphaning accounts on a lost session and/or persist a durable local
  record to detect+repair.
- **Source:** `FirebaseAuthenticationRepository` (auth/session persistence), `AppViewModel.loadScreenState`
  (the two dialog gates), `CreateProfileUseCase` (mints a new anon user when not signed in).
- **Related:** SS-088 (Android Google enablement), SS-061/SS-086 (Google sign-in),
  [[auth-restore-race-bounded-wait]], the owner's standing emphasis on session/connectivity
  resilience ([[connectivity-resilience-priority]]).

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

## UI overhaul & input

### SS-084 — Spinning-wheel visual fidelity (match Figma)

- [ ] Not completed
- **Source:** Child of SS-079. The wheel's interaction + structure shipped under SS-079; this ticket
  is the look. Figma will keep iterating on icons/colors, so this is intentionally split out.
- **Goal:** Bring `CharacterClassWheel` to Figma fidelity:
    - Themed top header band and bottom stat band (the radial-gradient bars from the Figma frame).
    - Background image behind the wheel.
    - Spoke geometry/proportions — length, thickness, corner radius, gap from hub — tuned to the
      Figma
      `SPOKE_*` / `AVATAR_*` / `HUB_*` fractions instead of the current eyeballed values.
    - ~~Final per-class spoke colors~~ **DONE (2026-06-26, SS-153):** the exact Figma per-family
      gradients (Rogue/Duelist `#9D2920→#581310`, Gambler/Bard `#DAA73B→#74591F`,
      Necromancer/Paladin
      `#4B5F0D→#9BC51B`, Illusionist/Mage `#1D406D→#387CD3`) are applied in `ClassStyle.kt` (
      `toGradient`/
      `toColor`), feeding both the wheel spokes and the head icon. **Still to do here:** the final
      avatar
        + stat icon art polish — the new `*_avatar`/`*_bust` art and stat icons (`ic_armor_class`,
          `ic_hit_points`, `ic_spc`, `ic_speed`) are imported but their sizing/placement on the
          wheel still
          needs the fidelity pass. The wheel currently uses one flat `toColor()` per spoke (the
          gradient's top
          stop) — decide whether spokes should render the full gradient. See
          [[downsample-figma-raster-assets]] for the raster art.
    - Selected-spoke highlight treatment and hub styling; label typography/placement polish.
- **Steps:** Pull updated Figma assets/colors when ready; replace placeholder drawables; tune the
  layout fraction constants; add the header/stat bands and background; revisit the selected-state
  highlight.
- **Notes:** Gated on Figma updates. Coordinate with SS-080 (responsive) and SS-081 (copy). Related:
  SS-079, SS-068.

### SS-133 — Refine screen-transition feel

- [ ] Not completed
- **Source:** Follow-up to SS-110 (which delivered the randomized forward presets + "burn to dust"
  pop). The mechanism works, but the *feel* isn't quite right yet.
- **Goal:** Tune the transitions so they feel intentional and polished rather than busy/off.
- **Areas to explore:**
    - Disintegration tuning in `DisintegrationEffect.kt` — particle density (`SAMPLE_STEP_PIXELS`),
      duration (`EFFECT_DURATION_MILLIS`), drift/rise/wobble constants, and the left-to-right
      `SWEEP_FRACTION`. Consider gravity/easing on the dust, a subtle glow/ember tint, and a
      burn-edge front rather than a uniform strip fade.
    - Whether random-per-navigation forward transitions actually feel good, or whether a curated
      per-screen mapping (or fewer presets) reads as more deliberate.
    - Forward/back coherence — the forward presets and the dust pop should feel like one language.
    - Predictive-back: currently silent during the gesture, dust plays on commit (documented
      SS-110 limitation). Investigate driving particles from live predictive-back progress.
    - Performance pass on the dust (first-play jank from pixel sampling) on lower-end devices/iOS.
- **Steps:** Run on device, iterate on the constants, decide selection strategy, verify
  forward/back/predictive-back all feel cohesive. Related: SS-110, SS-068.

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
  bounds the height it caps the wheel to `WHEEL_HEIGHT_FRACTION` (0.6) of it so the stats card always
  keeps room, and the stats card takes the remaining space via `weight(fill = false)` with the card's
  content in a `verticalScroll` so an expanded card scrolls instead of clipping. Unbounded-height
  hosts keep the old wrap-content behavior. The wheel is being moved off `TavernScreen` (overhaul
  pending), so Tavern-specific fit is deferred to that work.
  Remaining: on-device verify on Small + Medium phones (combat screen, dialogs, dice tray, wheel
  collapsed + expanded); tune the `dpScale`/`textScale` factors against real devices if needed.
- **Notes:** Distinct from tablet (SS-083) and landscape (SS-082). Related: SS-079, SS-081. The
  `ScreenSizeClass.Large` bucket also seeds SS-083 (tablet).

### SS-082 — Decide on landscape support

- [ ] Not completed
- **Goal:** Make a product + technical decision on whether the app supports landscape orientation,
  and to what extent (all screens, or lock to portrait). The wheel selector and Arena combat layouts
  assume portrait today.
- **Steps:** Decide scope (lock portrait vs. full landscape vs. per-screen); if supported, define
  how
  the wheel + stats card and ArenaScreen reflow; otherwise set the orientation lock and remove the
  ambiguity. Record the decision here.
- **Notes:** Interacts with SS-080 (responsive) and SS-083 (tablet, where landscape is more
  expected).

### SS-083 — Tablet support

- [ ] Not completed
- **Goal:** Define and implement how the app presents on tablets / large screens (the layouts are
  phone-first today).
- **Steps:** Decide the target (basic "scaled-up phone" vs. adaptive multi-pane layouts); set max
  content widths / adaptive breakpoints; review the wheel selector, Tavern, Arena, and profile/stats
  screens at expanded window sizes. The `~/.android/skills/jetpack-compose/adaptive` skill is the
  reference for adaptive layouts.
- **Notes:** Likely depends on SS-082 (landscape) since tablets are commonly used landscape.
  Related:
  SS-080, SS-082.

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
    - **Open questions:** rebalancing when shards drain unevenly; whether the computer traveler(s)
      appear in every shard; how the in-tavern count is computed across shards; whether a global
      population threshold gates sharding on/off so small taverns stay single-node.
- **Note:** this is the larger, speculative piece — don't build it until population actually
  warrants
  it. SS-125 is expected to carry the load until then.

### SS-264 — Aborting the "mark traveler busy" transaction crashes the app instead of surfacing a retryable error

- [ ] Not completed
- **Severity:** High — an app crash is never acceptable. Intermittent (does not reproduce every
  time), but reproducible enough to have been seen in the wild.
- **Symptom / crash:**
  `com.google.firebase.database.DatabaseException: Firebase Database error: User code called from the
  Firebase Database runloop threw an exception:
  com.linklabs.sorcerersandspirits.data.tavern.FirebaseTavernDataSource$AbortTransactionException`,
  originating at `FirebaseTavernDataSource.markTravelerBusyIfAvailable` (the `traveler ?: throw
  AbortTransactionException()` / status-not-Available abort path).
- **Root cause:** `markTravelerBusyIfAvailable` aborts the transaction by *throwing* an exception
  from inside the `runTransaction` lambda. Although the call site wraps `runTransaction` in a
  `try/catch (_: AbortTransactionException)`, the throw happens on Firebase's own runloop thread; the
  Android SDK catches user-code exceptions there, re-wraps them as a `DatabaseException`, and
  rethrows on that thread — so it never reaches our `catch` and crashes the process as an uncaught
  exception. Throwing to abort is fundamentally unsafe with this SDK.
- **Fix direction:**
    - Stop using an exception to model "don't write". Abort by returning the value unchanged (a
      no-op write) or via whatever non-throwing abort the gitlive wrapper exposes, and detect the
      "was I the one who claimed it" result by re-reading committed state / the transaction result
      rather than by catching a throw. `markTravelerBusyIfAvailable` should still return `false` when
      the traveler is missing or already busy — just without ever throwing across the runloop.
    - Audit the sibling transactions in the same file (`upsertTravelerPresence`,
      `setTravelerStatusIfPresent`, `releaseTravelerIfBusy`) and any other `runTransaction` usage
      (e.g. arena transactions) for the same throw-to-abort pattern and apply the same treatment.
- **UX requirement:** when claiming a traveler genuinely fails (already busy, transient DB error),
  the caller must land in a recoverable UI error state with a retry affordance — never a crash and
  never a silent dead-end. Thread the failure up from the data source through the tavern domain/VM to
  a surfaced, retryable error.

---

## Audio

### SS-134 — Music & sound-effect playback (KMP)

- [ ] Not completed
- **Status (foundation shipped):** The KMP audio plumbing is built and merged — `commonMain`
  `MusicPlayer` / `SoundEffectPlayer` interfaces + `MusicTrack` / `SoundEffect` sealed catalogs (no
  entries yet), Android `AndroidMusicPlayer` (Media3 ExoPlayer, gapless loop + volume cross-fade) and
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
  `SoundEffect` implementations (each a `data object` overriding `assetFileName`) and drop the encoded
  files: SFX as 44.1 kHz / 16-bit mono WAV, music as 44.1 kHz stereo ~128–160 kbps VBR AAC in an
  `.m4a` container (gapless-loop metadata intact). Android SFX resolve from `androidMain` assets under
  `assets/audio/`; Android music loads via the `asset:///audio/…` URI; iOS resolves both from the app
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

---

## Tavern UI

### SS-238 — Animate search-result filtering in TheBar (fade + content swap)

- [ ] Not completed
- **Goal:** When the traveler list is filtered by the name search in `TheBar`, transition the
  results with a smooth **fade + content-swap** rather than an instant hard cut, so the list
  reads as smoothly re-populating as the query changes.
- **Source:** The filtered-list rendering in
  `ui/screens/tavern/ui/TheBar.kt` — `BarFolk` renders `visibleTravelers`, which is derived from
  `searchQuery`/`trimmedQuery`. Wrap the results in an `AnimatedContent` (or crossfade) keyed on
  the query/visible set so filtering fades the outgoing content out and the new matches in.
- **Notes:** Keep selection behavior intact — a picked traveler filtered out of view stays in the
  outgoing challenge (existing comment in `TheBar`). Extract the animation label to a private
  `const val` per [[animation-labels-as-const-vals]]. Related: SS-239 (ChallengeButton animation),
  SS-240 (full Tavern overhaul).

### SS-240 — Complete UI overhaul of TavernScreen (owner to spec before work)

- [ ] Not completed
- **Goal:** Full visual/structural overhaul of `TavernScreen`. Known direction so far: the
  **class wheel (`CharacterClassWheel`) will no longer be used here**, and **`TheBar` will likely
  be modified substantially**. The rest of the new design is to be described by the owner **at the
  start of the work** — do not begin building until the spec is given.
- **Source:** `ui/screens/tavern/ui/TavernScreen.kt` (`LoadedTavernScreen`, `ScreenContent` — which
  currently hosts `CharacterClassWheel`) and `ui/screens/tavern/ui/TheBar.kt`.
- **Notes:** This is an epic; carve child tickets once the design is specified. Don't remove
  existing functionality without an explicit instruction in the spec ([[never-remove-without-instruction]]).
  Related: SS-238, SS-239 (fold in or supersede as the new design dictates).

---
