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

### SS-159 — Raise DiceHome and buttons toward the avatars; keep the stat column fixed

- [ ] Not completed
- **Symptom:** The `DiceHome` resting slot sits too low; it should be higher, closer to the avatars.
  The combat buttons should also move up. The stat column should **not** move up — its current
  vertical position is correct.
- **Source:** the root `Column` in `combat/ArenaCombat.kt`; `BottomControls` in
  `combat/CombatButtons.kt`; `DiceHomeSlot` in `combat/CombatAvatars.kt`. Note the stat columns live
  inside `BottomControls`, so naive "move the buttons up" must not drag the stat columns with them.
- **Goal:** Reposition `DiceHome` and the buttons higher (closer to the avatars) while leaving the
  stat columns where they are. Likely means decoupling the stat columns' vertical placement from the
  button row.
- **Related:** SS-156, SS-157, SS-068. **Confer with designer** on exact offsets.

### SS-160 — Combat buttons: 3D pressed style; ability buttons get class-color border

- [ ] Not completed
- **Goal:** Combat buttons should read as physically pressed-down (3D) when held/pressed. Ability
  buttons should carry the player's class-color border.
- **Source:** `combat/CombatButtons.kt` — shared `ArenaButton` used by `AttackButton`,
  `AbilityButton`, `ImpersonateButton`. Class color available via
  `CharacterClassType.toColor()` / `classFamilyColors` (already used by `ClassAvatarStats`/head
  icons).
- **Related:** SS-068 (Arena UI overhaul), SS-153 (class color source).

### SS-163 — Flash callout hold is too short

- [ ] Not completed
- **Symptom:** The short full-screen animation message (Hit!/Miss!/K.O.!) is a little too short.
- **Source:** `combat/CombatFlashOverlay.kt` `FLASH_HOLD_MILLIS = 850L`, used by `FlashOverlay` (
  SS-148).
  Note the Combat→Finished hold (`FINISH_HOLD_MILLIS`, SS-154) was sized against this; revisit it if
  the flash duration changes materially.
- **Goal:** Lengthen the flash hold so the callout is comfortably readable. **Confer with designer**
  on the exact duration.
- **Related:** SS-148 (flash callout), SS-154 (`FINISH_HOLD_MILLIS` sizing).

### SS-164 — One message → flash call-out + log line (two string resources)

- [ ] Not completed
- **Core model:** Every combat message flows Data → Domain → UI as **one** `ArenaMessageState` and
  is rendered **two ways from that same message**: (1) a short, punchy full-screen **flash**
  animation, and (2) a fuller line in the passive **message log**. Multiple flashes from one
  resolution play **in sequence** (e.g. `Hit!` → `3 Damage!` → `Knocked Out!`).
- **Two string resources per message:**
    - **Flash:** `arena_flash_*` — short, animated. Hit/Damage/Knockout render in the **acting
      class's color**, everything else neutral white. Built from the sealed `FlashCallout` (each
      variant carries the color + the data its resource needs) via `CombatFlashText.text()`.
    - **Log:** `arena_message_*` — the more complete, passive record, built in `ArenaMessageDisplay`.
      Final wording **TBD** (owner-refined).
- **No wrapper type:** one `ArenaMessageState` maps to both resources at the two display sites; there
  is no `FlashMessage`/pair object. `flashCallout`'s `when` is **exhaustive over every message**, so
  a new message type is a compile error until its flash is chosen.
- **Coverage (this pass):** **every** message currently flashes — round/turn/connection/info and
  every intermediate roll step included. Paring some back to log-only is **SS-185**; the nullable
  `flashCallout` + `ArenaScreenState.flashMessages` filter hold the seam for it.
- **Normal-attack flash wording (first pass):** `ArmorClassHit` → `"{total} · Hit!"`;
  `ArmorClassMiss` → `"Miss!"`; `Damage` → `"{total} Damage!"`; `Knockout` → `"Knocked Out!"`.
  `NormalRoll` carries the consecutive-doubles `bonus` through every layer so `total` is
  `firstDie + secondDie + bonus`, not just the visible dice.
- **Ability flash wording (first pass, owner refines each):** each defensive ability flashes every
  step — its name then the roll's outcome (e.g. Lucky Coin → `Lucky Coin!` → `Attack stopped!` /
  `"{die} · Attack continues!"`; Summon Minion → `Skeleton! Necromancer heals to full!`). Derived
  **only** from messages the domain already adds; no new domain structures.
- **Wording status:** both the flash and the log wording are owner-refined. All flash strings are
  `arena_flash_*` (in place); log strings are `arena_message_*` (in place, final wording TBD).
- **Related:** SS-185 (pare back flash coverage), SS-148 (flash callout), SS-163 (flash hold
  duration), SS-029 (string resources), SS-025 (message-log animation), SS-179 (message text
  styling/backgrounds), SS-177 (doubles flash suppression).

### SS-185 — Pare back which messages flash

- [ ] Not completed
- **Goal:** SS-164 makes **every** message flash (round/turn/connection/info and every intermediate
  roll step). Decide which messages should be **log-only** and stop them flashing, so the flash
  channel stays punchy and reads as combat narration rather than echoing the whole log.
- **Mechanism:** `ArenaMessageState.flashCallout` (in `FlashCallout.kt`) already returns
  `FlashCallout?` and `ArenaScreenState.flashMessages` already filters nulls — return `null` from the
  branches that should be log-only. No structural change needed.
- **Open for owner:** the exact keep/cut list (likely cut: `NewRound`, `NewTurn`, `ConnectedToArena`,
  `ChoseCharacterClass`, and intermediate roll steps like `Teleport.Roll`, `LuckyCoin.HighRollerTrigger`).
- **Related:** SS-164 (one message → two resources).

### SS-172 — Finished-state timer differs for Match vs Gauntlet

- [ ] Not completed
- **Goal:** Split the Finished-state countdown behavior by what just finished:
    - **Match finished (not the final match):** immediately start a **30-second** timer on entering
      Finished; on completion, **auto-ready the player** for the next match (auto-fire
      `onStayForAnotherMatch`).
    - **Gauntlet finished (final match):** keep today's behavior — a **1-minute** timer that does
      **not** start until a player declares ready for the next Gauntlet.
- **Source:** `MatchFinishedContent.kt` (`MatchDecisionControls`, `isFinalMatch`,
  `secondsRemaining`,
  `hasReadiedForNextMatch`) renders the countdown; the timer itself is owned by the VM. The
  Match-vs-Gauntlet distinction already exists (`GauntletProgress.isFinalMatch`). The auto-ready
  path
  reuses `StayForAnotherMatchUseCase`/`onStayForAnotherMatch`.
- **Notes:** Coordinate with SS-170 (don't auto-advance/ready into an impossible single-player next
  match). Related: SS-108 (Next Match/Go Again on Finished), SS-073 (Gauntlet), SS-170.

### SS-173 — Messages: full-width golden bar that unfurls down to DiceHome

- [ ] Not completed — **reopens the Messages overhaul; SS-147/SS-161 are partially superseded.**
- **Goal (owner is bringing back an earlier design):** Replace the collapsed messages *icon*
  affordance with a **golden bar the full width of the screen**, pinned at the top with all screen
  content below it. The bar always shows the **most recent message**. When the messages area is
  opened it **unfurls downward from the bar** and drops down to `DiceHome` — i.e. it covers the
  topmost large avatar but stops at the dice home slot; it does not cover `DiceHome`/the buttons.
- **Source:** `CombatMessages.kt` (`Messages`, `MessagesIcon` — SS-147/SS-161) and
  `ArenaMessageLog.kt`. Today the affordance is a circular icon with an unread tally over a gold
  panel that expands top→bottom (SS-161 reading order: newest at top). The latest message comes from
  the message list; reuse it for the always-visible bar content.
- **Notes:** This changes the affordance the owner says was never actually finished despite SS-147
  being marked complete. Keep the existing log/unread plumbing where it still applies; the bar
  becomes the new collapsed state. Related: SS-147, SS-161, SS-025, SS-068, SS-179.

### SS-174 — Tapping the inspect drawer dismisses it

- [ ] Not completed
- **Symptom:** The right-side avatar-stats inspect drawer only dismisses via the scrim; tapping on
  the opened drawer itself does nothing.
- **Goal:** Tapping anywhere on the opened drawer dismisses it (in addition to the existing scrim
  dismiss).
- **Source:** `CombatInspectDrawer.kt` (`InspectDrawer`/`InspectContent`, SS-150). Add a click on
  the
  drawer surface that clears `inspectedUserId`.
- **Related:** SS-150 (inspect drawer).

### SS-175 — 2-player: stat icons in a vertical column left of the avatar (not a bar above)

- [ ] Not completed
- **Goal:** In a 2-player game, the 4 visible stat icons/values should live in a **column down the
  left-hand side of the large avatar**, instead of as a horizontal bar above the large avatar.
- **Source:** `CombatStats.kt` (`avatarStats` — the Speed/Armor/Hit Points + ability rows) and the
  combat layout in `ArenaCombat.kt`/`CombatButtons.kt`/`CombatAvatars.kt` that positions the stat
  columns (SS-146/SS-156 made them flanking overlay columns; SS-157/SS-168 seat the avatars).
  Revisit
  the placement specifically for the 2-player case.
- **Notes:** Interacts with the avatar seating work — confirm it still reads correctly with the
  attacker/target seating. Related: SS-146, SS-156, SS-157, SS-168.

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

### SS-025 — Improve message-log animation

- [ ] Not completed
- **Source:** `ArenaMessageLog.kt` (was `ArenaMessageLogHandle.kt`, removed in SS-154 as orphaned).
- **Goal:** Improve the arena message-log animation.

### SS-029 — Use StringResources in ArenaCombatControl

- [ ] Not completed
- **Source:** `ArenaCombatControl.kt:164` (`// TODO: Always StringResources`)
- **Goal:** Replace hardcoded strings with string resources.

### SS-136 — Class head icon (collapsed) composable

- [ ] Not completed
- **Source:** Figma `Arena` → node `75:159` ("Group 12")
  (https://www.figma.com/design/hCcLwQRgRvNFMrzGdMiRlf/Arena?node-id=75-159). Child of SS-068.
- **Goal:** A reusable circular "class head" icon shown for each player in the Arena **while they
  are
  not the active roller** (not their turn). It is the *collapsed* at-a-glance view of a player:
  their
  class avatar ringed by a class-colored border, with three combat stats. The *expanded* detail view
  is a separate future ticket (SS-150, "the other UI").
- **Anatomy (from Figma, overall ~176×155):**
    - **Outer class-color border** — a red, scalloped/"cloud" shape (`Group 8`) tinted with the
      player's class color. Render the color in code from the class-color source (SS-137); for this
      first pass just match the specific Figma red. Per-class color *groups*: Rogue/Duelist red,
      Gambler/Bard yellow, Necromancer/Paladin green, Illusionist/Mage blue.
    - **Center avatar** — the class character art (~99×99) inside a stone ring (reuse the existing
      `class_*` drawables already used by `CharacterClassWheel`).
    - **Three stat badges** (~48×48 icon + bold white 24sp value with a drop shadow): **Speed** top,
      **Armor** left, **Hitpoints** right. **No SPC/ability icon** on the head icon. Values come
      from
      `CharacterClassState` (`speed`, `armorClass`, `hitPoints`/`hitPointsDisplay`).
- **Steps:** Build a `ClassHeadIcon` composable (likely in
  `ui/common/characterclasses/` next to `CharacterClassWheel.kt`) taking a `CharacterClassState`;
  draw the class-colored border behind a circular avatar; lay out the three stat badges around it
  using the exported SVG icons (SS-138, falls back to the existing `ic_speed`/`ic_armor`/`ic_health`
  PNGs until then); pull the border color from the class-color source (SS-137); add a `@Preview`.
  Leave hooks so SS-139's `SharedTransitionScope`/`sharedBounds` can wrap the avatar + the whole
  icon
  later. Then wire it into the Arena combat layout for every non-active player.
- **Depends on / related:** SS-137 (class colors), SS-138 (SVG icons), SS-139 (shared-transition
  foundation), SS-144 (head→targeted-avatar transition), SS-150 (head→expanded inspect view).
  Parent: SS-068.

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

---

## Domain layer

### SS-048 — Die-value enum

- [ ] Not completed
- **Source:** `DiceRoller.kt:16` (`// TODO Enums for values 1 through 6?`)
- **Goal:** Consider a type-safe enum for die faces (1–6) instead of raw Ints.

---

## Bugs

### SS-187 — Duplicate class selection: stale local selection survives a class being taken

- [~] In progress — root cause found and fixed; needs device verification + regression test.
- **Symptom:** In `ChoosingClasses`, two players entered Combat as Rogue. This breaks the core rule
  **"No duplicate classes."** (see CLAUDE.md core combat rules). Observed live on two devices.
- **Root cause (UI state, not the transaction):** every player defaults to Rogue selected on entry
  (SS-176). Selection is turn-based, but the local `selectedClass` is **sticky**:
  `ArenaScreenState.kt` derived it as `previousSelection ?: defaultSelectedClass(...)`. The `?:`
  never re-checks `previousSelection` against the freshly-derived `unavailableClasses`, so after
  another player confirms Rogue, the local user's `selectedClass` is *still* Rogue. The confirm
  button (`ArenaChoosingClasses.ConfirmSelectionButton`, `isEnabled = selectedClass != null`) stays
  enabled, so on their turn they confirm the already-taken class → duplicate. The Firebase
  transaction was never the problem.
- **Fix (applied):** drop a now-unavailable previous selection before falling back to the default —
  `previousSelection?.takeIf { it.type !in unavailable } ?: defaultSelectedClass(...)` in
  `ArenaScreenState.kt`. The selection can no longer be a taken class, so the confirm button and the
  `CharacterClassWheel` highlight both reflect an available class.
- **Remaining:** add a regression test (previous selection becomes unavailable → `selectedClass`
  re-defaults to an available class, never the taken one); verify on device that a player whose
  defaulted/held class is confirmed by someone else cannot confirm it and is moved to another class.
- **Related:** SS-176 (default-select Rogue / selection aura), SS-122 (class identity by type).

### SS-116 — All-In bugs (2-player game, observed playing Gambler + Illusionist)

- [~] In progress — root cause found and fixed; one latent sub-bug split out below.
- **Source:** Reported bug. Three symptoms seen in one 2-player session; all concern the Gambler's
  All-In activated ability (`AllInActivated.kt`).
- **Symptoms:**
    1. **Second attack not rollable in the UI.** All-In on a 5/6 (attack twice this round) resolved
       correctly in state, but the UI did not let the player roll the dice for the granted second
       attack after the first attack finished.
    2. **usedThisTurn not resetting.** All-In appeared stuck/unusable on later turns — looked like
       it
       behaved once-per-match, not once-per-turn.
    3. **Impersonated All-In unusable.** When the Illusionist Impersonated All-In, they could not
       see
       the activation through (its follow-up attack could not be rolled).
- **Root cause (FIXED 2026-06-20):** all three were the same UI bug, not domain. The domain is
  correct and fully covered (PerformCombatActionUseCaseTest: second-attack grant, finish-second-
  attack, new-round `usedThisTurn` reset, Impersonate). The break was
  `ArenaScreenState.deriveAutoTargetedOpponentUserId` (SS-101): it suppressed the 2-player lone-
  opponent auto-target whenever *any* second control was present, counting even a **disabled** one.
  After All-In resolves, the Gambler sits in an idle turn with `[NormalAttack(enabled),
  All-In(disabled)]`, so auto-target returned null and the dice stayed locked on "Tap an enemy to
  target" — the granted second attack (and the impersonated All-In's follow-up attack) could never
  be
  rolled, which also made All-In *feel* un-reset. Fix: only **enabled** controls count as a real
  choice, so a lone disabled ability no longer suppresses auto-targeting. Regression test added
  (ArenaScreenViewModelTest: disabled All-In beside attack → lone opponent auto-targeted); test 874
  (enabled Prepare Attack → no auto-target) still green, preserving the intentional "don't auto-
  attack when you have a real ability choice" behavior. Full unit suite green.
- **Follow-up done (SS-119):** the latent Impersonate-wrapper corruption found during this diagnosis
  has since been fixed and archived in COMPLETED.md.
- **Verify on device:** confirm the Gambler can roll the granted second attack and re-use All-In
  each
  round in a live 2-player match; confirm an Illusionist can impersonate All-In and play it through.
- **Notes:** UI fix in `ArenaScreenState.kt`. Related: SS-101 (auto-target), SS-104 (second-attack
  grant), SS-114 (`usedThisTurn` reset), SS-119 (Impersonate-wrapper corruption).

### SS-166 — Dice: faces, disabled die, and Mirror Image reroll

- [~] Dice-rendering rework done (awaiting device verify); two follow-ups remain (drift, Necro
  button).
- **Model (the simple mental model the dice now follow):** two real dice on a table. Each die is a
  persistent slot that keeps its last face. A roll only updates the slots it touches; an untouched
  die keeps its face and simply dims (grey), it never resets to a 1. Everything is message-driven so
  every client shows the same two dice (`ArenaMessageRollDice.kt` `RollResult` →
  `DiceRoll.commitRoll`).
- **Done:**
    1. Disabled/idle die greys but keeps its face (no longer snaps to 1) and never moves during a
       single-die roll. When displaced (multi-die roll picked up then declined) it now **glides**
       home instead of snapping (`DiceRoll.slotFaces`/`activeSlots`, `Die3d` settle path with
       `settleVelocity = 0` for `!enabled`).
    2. Mirror Image reroll throws the exact die the player chose ("Reroll the 6" rolls the 6 die)
       and dims the other; the rerolled value lands on that die. Roller uses the chosen slot;
       spectators match the replaced face (`RollResult.Reroll`, `diceSlots`), so all clients agree.
    3. Dice faces follow the authoritative roll value (slot-keyed, message-driven).
    4. DiceHome glide-to-wrong-spot jank fixed at the root: the home slot sits between two weighted
       avatars (`CombatAvatarColumn`) and genuinely moves when the target avatar appears/disappears
       or the active roller changes. The settle glide used to capture `homeCenter` once (effect
       keyed on isRolling/value/enabled, not homeCenter) and animate to that stale spot, then a snap
       corrected it. Now homing lives in its own `LaunchedEffect(isSettling, isRolling, homeCenter)`
       that animates to the **live** `homeCenter` and re-targets smoothly when the slot moves — no
       stale target, no snap.
- **Remaining (needs a concrete device repro before touching code — off the dice-render path):**
    - **Necromancer Summon Minion button disappears, possibly after the turn timer auto-acts.**
      Controls show Summon Minion whenever the Necro is `Idle` on their turn
      (`deriveCombatControls`); the timer auto-resolves with a normal attack. Suspect the
      post-summon granted-extra-attack state. Capture: exact sequence (did they summon first? did
      the timer fire? whose turn when the button vanished?).
- **Related:** SS-077 (dice tech), SS-078 (dice UI), SS-158 (single-die freeze), SS-149 (dice
  overlay), SS-119 (Impersonate/MirrorImage state).

### SS-169 — Teleport roll-off: both human combatants get the roll button at the same time

- [ ] Not completed
- **Symptom:** In a Mage Teleport roll-off (Mage vs attacker, 1d6 each), the roll button shows for
  only one player at a time. In a human-vs-human match both players should hold the button **at the
  same time** and roll simultaneously.
- **Source:** SS-162 deliberately made the roll-off **sequential** — `TeleportHandler.kt`'s
  `setNextTeleportRoll` sets `activeUserIds` to a single roller (attacker then defender) — to fix a
  human-vs-**computer** stall where `ComputerOpponentHandler` never acted while the user was
  co-active and the user was forced to roll the bot's die out of order. So the fix is conditional:
  when both combatants are **human**, both should be active at once; only when one side is a
  computer
  should it stay sequential.
- **Goal:** Restore simultaneous roll buttons for two human players while preserving the SS-162
  computer-opponent path. Likely branch on whether either roller is a computer
  (`ComputerOpponents.isComputer`).
- **Related:** SS-162 (sequential roll-off fix), SS-092 (computer opponent).

### SS-170 — Finished screen must not offer "Next Match" when only one player remains

- [ ] Not completed
- **Symptom:** If an opponent leaves the arena (e.g. forfeit) and the Finished state comes up,
  the **Next Match** button still shows — but a next match with a single player in the arena is an
  impossible state.
- **Source:** `MatchFinishedContent.kt` `MatchDecisionControls` always renders the Leave +
  Next Match/Go Again row. It needs to suppress Next Match/Go Again (and any auto-ready, SS-172)
  when
  fewer than two players remain in the arena, leaving only Leave. The remaining-player count is
  already known (an opponent leaving fires `ArenaEvent.OpponentLeft`, SS-045; the arena removes the
  match when the last player leaves, SS-022).
- **Related:** SS-108 (Next Match/Go Again on Finished), SS-045 (opponent left), SS-022 (remove
  match
  on last leave), SS-172 (Finished timer).

### SS-177 — Doubles message/flash only when the +1 bonus can actually apply

- [ ] Not completed
- **Symptom:** A Doubles message/flash fires even when the doubles can't benefit anyone (e.g.
  doubles
  rolled on the ArmorClass roll that then *fails* to break armor, so there is no consecutive roll to
  carry the +1). Per CLAUDE.md the doubles +1 only matters when the next roll is consecutive and the
  same player keeps the dice.
- **Goal:** Only add the `ArenaMessage.Doubles` (SS-107) — and therefore the flash (SS-164) — when
  the doubles will actually contribute to a next consecutive roll. This is fully determinable from
  existing domain structure; **no new structures needed**.
- **Process (owner ask — do this first):** Before writing any code, **list every place doubles
  should NOT be added** as a message; the owner will confirm / add / subtract. Candidate cases to
  enumerate from CLAUDE.md and `NormalAttackRollHandler.addNormalRollMessage` (the emission site):
    - Doubles on the ArmorClass roll that fails to break armor (no consecutive damage roll).
    - Doubles on the Health/damage roll (turn-ending — nothing consecutive follows).
    - Doubles whose dice are then handed to another player by a defensive ability (Riposte, Persuade
      redirect, etc.), breaking the consecutive chain.
    - Doubles on a roll that ends the turn / match.
      Confirm this list with the owner, then suppress emission accordingly.
- **Source:** `NormalAttackRollHandler.kt` (`addNormalRollMessage`, emits `Doubles` after a doubles
  normal roll); `ArenaMessageState.flashText` (`Doubles → "Doubles!"`); doubles bonus tracked via
  SS-019. SpellPotency already short-circuits before the doubles message.
- **Related:** SS-107 (Doubles message), SS-019 (doublesBonus), SS-164 (flash).

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

### SS-181 — Illusionist Mirror Image ("Keep Your Roll") corrupts large-avatar seating (full rework)

- [ ] Not completed — **prior fixes did not resolve this; treat as a rework.**
- **Symptom:** When a player simply attacks the Illusionist and the Illusionist taps the Mirror
  Image / "Keep Your Roll" button, the avatar layout breaks badly. Previously two Illusionists
  showed
  and the other player collapsed to a small head icon; the last fix changed it to one Illusionist
  but
  now the **other large avatar disappears and collapses to a small head (`ClassIcon`)**.
- **Goal:** During an Illusionist Mirror Image reroll/keep, both combatants must remain correctly
  seated as large avatars (attacker and the Illusionist defender); neither should duplicate nor drop
  to a head icon.
- **Source:** The avatar seating pipeline — `CombatActionState` (`attackingUserId`,
  `targetedDefenderUserId`, `turnOwnerUserId`), `ArenaState.Combat` (`attackingPlayer`,
  `seatedTargetPlayer`, `seatedAvatarUserIds`), `resolveTargetUserId`, and `ArenaCombat`'s avatar
  `AnimatedContent` (SS-157/SS-165/SS-168). Mirror Image keeps the dice on the defender (SS-118) — a
  self-referential reroll likely makes attacker and target resolve to the same id (the SS-168
  double-seat class of bug) or mis-resolves who attacks. Reproduce the exact state during the keep
  step and trace which of attacker/target/turnOwner each avatar slot reads.
- **Related:** SS-168 (double-seat fix), SS-157 (active=left seating), SS-165 (Riposte seating),
  SS-152 (KO head icon), SS-118/SS-119 (Mirror Image state).

### SS-183 — Rogue Dodge bonus must show in the ArmorClass stat (base +1 / +2)

- [ ] Not completed
- **Symptom:** When a Rogue earns a Dodge bonus (+1 Armor up to +2 for the match), the ArmorClass
  stat still shows the base value, not base + Dodge bonus.
- **Goal:** The displayed ArmorClass reflects base + active Dodge bonus, the same way Summon Minion
  shields surface in the hit-points display (SS-102's `hitPointsDisplay`).
- **Source:** `CombatStats.kt` reads `characterClass.armorClass` for the Armor `AvatarStat`;
  `CharacterClassState` (`ui/common/characterclasses/`) is the UI mapping. Confirm the Dodge bonus
  is
  carried Domain → entity → `CharacterClassState` and that `armorClass` (or a derived display)
  includes it — mirror the SS-102 shield/`hitPointsDisplay` pattern; this is most likely a mapping
  gap, not a domain bug ([[domain-is-source-of-truth]], [[no-default-args-hide-mapping-gaps]]).
- **Related:** SS-102 (minion shield display), SS-050 (Killing Momentum/Dodge), SS-019.

### SS-184 — Don't collapse the final knocked-out player to a head icon

- [ ] Not completed
- **Symptom:** When the final player is knocked out (2-player match, or the last KO of a multiplayer
  match), the large avatar still morphs to the small head icon (`ClassIcon`). On a match-ending KO
  there should be **no** collapse-to-head transition.
- **Goal:** Suppress the large-avatar → head-icon morph for the knockout that ends the match; the
  downed player stays a large avatar through the transition to Finished.
- **Source:** `CombatClassIcons.kt`/`CombatAvatars.kt` seating + `deriveHeadIconPlayers` (SS-152
  collapses a KO'd opponent to a disabled head icon). SS-152 assumed "the final knockout ends the
  match (→ Finished), so there is no collapse to render then" — this symptom says a collapse *is*
  rendering on the final KO. Gate the collapse off when the KO is match-ending (held during the
  SS-171 finish hold).
- **Notes:** The owner notes large-avatar animations are coming and this will tie into them.
  Related:
  SS-152 (KO head icon), SS-168 (seating), SS-123, SS-171 (finish hold).

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

## UI overhaul & input

### SS-068 — Lottie animation pass & UI overhaul (Arena + Tavern)

- [ ] Not completed
- **Source:** Existing local branch with Lottie animation work. Related: combat-animation
  foundation.
- **Goal:** Track the broader UI overhaul. Primary focus is the ArenaScreen, with knock-on effects
  on
  the TavernScreen. The CharacterClassCards pager becomes a spinning-wheel selector (a pager may
  still
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
- **Notes:** Depends on SS-077; do after SS-112. Related: SS-069, SS-077, SS-068, SS-112, SS-113.
- **Post-SS-112 follow-up (do here):**
    - **Feel-tuning, not yet validated on device:** the shake impulse strength
      (`MIN/MAX_SHAKE_IMPULSE_DP` in `Die3d.kt`) and the await-tumble impulse cadence
      (`ROLL_TUMBLE_IMPULSE_MILLIS` in `DiceTray.kt`, added in SS-117) are first-guess constants.
      Confirm/tune by hand on a real device.
    - **Single-tap jostle:** holding starts the live physics so shaking shows a live bounce, so a
      stray single tap on the dice makes them throw-and-settle without rolling. Harmless feedback;
      if
      it reads as a bug, gate the physics behind a brief minimum hold before it kicks in.
    - **Placement:** the dice tray (`DiceTray.kt`) currently sits in a plain persistent row between
      the combat content and `UserStatsRow`, drawn at `TRAY_WIDTH_DP`/`TRAY_HEIGHT_DP` per die.
      Size,
      spacing, color/theming, and the caption styling are all still placeholder — this ticket owns
      finalizing them. The dice are the ONLY roll control now (no roll button), so factor the
      shake/double-tap caption into the final visual.
    - **Dice-count transition (1 ↔ 2 dice):** SS-117 made the tray show the correct number of dice
      for
      the current step, but the count still pops abruptly — e.g. a single-die ability roll followed
      by
      the player's two-die attack snaps from one die to two, which reads as a bug. Design how the
      tray
      should add/remove a die gracefully (slide/fade the second die in/out, hold a consistent
      footprint,
      or always show two dice and dim the unused one, etc.). UX/design decision, owned here.
    - **Latency reveal — resolved in SS-117:** the dice used to settle on the previous value on
      release
      and only re-tumble once the result arrived (a visible rest on the old value under network
      latency). SS-117 now keeps them tumbling for the whole round trip and settles only on the
      result.
      Tied to [[connectivity-resilience-priority]]; nothing left to do here unless device testing
      shows
      the await needs a visible "waiting" treatment when latency is high.

### SS-079 — Spinning-wheel class selector (replace CharacterClassCards)

- [ ] In progress — interaction + structure built; Figma visual fidelity remains.
- **Progress:** `CharacterClassWheel.kt` built and wired into TavernScreen + ArenaChoosingClasses
  (replacing `CharacterClassCards` at both call sites). Single `Animatable`/float rotation as the
  source of truth; **angular drag** (rotate by the angle the finger sweeps around the centre, so the
  grabbed point tracks the thumb at any clock position); snap-to-nearest spoke on release; each
  avatar is `clickable` to spin-and-select that class. Per-class spoke colors, labels rotated inline
  with their spoke, downsampled avatar/icon assets (see [[downsample-figma-raster-assets]]).
  Per-frame recomposition avoided via `derivedStateOf`. Bottom stats are an expandable
  `ElevatedCard`
  with `animateIntAsState` value animations and ability/stat descriptions. Removed the top selection
  pointer. Also deleted the now-dead `CharacterClassCards.kt` pager (the singular
  `CharacterClassCard`
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
- **Notes:** Distinct from tablet (SS-083) and landscape (SS-082). Related: SS-079, SS-081.

### SS-182 — ArenaScreen top padding should sit just below the system UI

- [ ] Not completed
- **Symptom:** The ArenaScreen's topmost padding is too large; it should sit only a hair or two
  below
  the system UI (status bar / display cutout).
- **Goal:** Tighten the top inset so combat content starts just below the system bars. May require
  platform-specific insets (`expect`/`actual`) to get right on both Android and iOS.
- **Source:** `ArenaScreen.kt` / `LoadedArenaScreen.kt` root layout (no explicit `WindowInsets`/
  status-bar padding found today — confirm where the top spacing comes from). Use the safe-content /
  status-bar insets rather than a fixed padding.
- **Related:** SS-080 (responsive), SS-159 (raise DiceHome/buttons).

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

## Computer opponent

### SS-092 — Computer enemy: challenge from the Tavern, fight in the Arena

- [ ] Not completed
- **Goal:** Let a player challenge a computer-controlled opponent from The Tavern and fight it in
  the
  Arena. Start with a single possible computer enemy; design so a human can later set up multiple
  computer opponents. The computer must take a short "thinking" delay before each decision —
  reacting, choosing who to attack, or rolling an ability.
- **Design:**
    - **Domain `ComputerOpponent`** (`domain/computer/`): a `ComputerOpponent(userId, displayName)`
      plus a `ComputerOpponents` registry holding the single `default` bot and an
      `isComputer(userId)`
      predicate keyed off a `"computer-"` id prefix (so multiple bots and human/bot detection extend
      cleanly).
    - **Tavern (bot as a traveler):** the computer shows up as a permanent, always-`Available`
      traveler in `TheBar` (injected in the UI `TavernData.toState`, not Firebase presence).
      Selecting
      it and pressing Challenge does **not** run the human Firebase challenge handshake (the bot has
      no
      presence to claim/mailbox, so `claimChallengers` would reject it). Instead
      `TavernScreenViewModel` short-circuits to a direct computer-match start: it builds
      `ArenaScreenViewModelArguments` with a freshly generated `arenaId`, `expectedNumberOfPlayers`,
      and the selected `computerOpponents`, and fires `TavernEvent.NavigateToArena`. MVP scope: a
      selection containing the computer starts a user-vs-computer(s) match (human co-challengees in
      the
      same match are deferred).
    - **Seeding the match:** `ArenaScreenViewModelArguments` / its navigation-serializable mirror
      carry
      the `computerOpponents`. `CoordinateArenaMatchUseCase` connects each computer into the arena
      (reusing the idempotent `connectToArena`) **before** connecting the user, so the user connects
      last and becomes the first active chooser.
    - **Driving the bot (the human's client):** in a user-vs-computer match only the human's client
      is
      present, so it drives the bot. After each match-state emission `ArenaScreenViewModel` checks
      whether a computer currently owes the action (it is the active class-chooser, or its id is in
      the
      combat `activeUserIds` for a turn or an owed reaction roll). If so it schedules the action
      after a
      randomized "thinking" delay, keyed on an action signature so unrelated emissions (e.g.
      incoming
      messages) don't re-trigger or double-fire. The decision engine reuses the existing SS-014
      timeout
      auto-resolve, generalized from "the user" to an arbitrary acting player id: pick a random
      available class; attack a random living opponent; auto-roll any owed ability/reaction (random
      legal Charm target). Session stats stay attributed to the human only.
    - **Cleanup / lifecycle:** `ExitArena` deletes the whole match when no **human** players remain
      (a computer-only match is dead), preventing an orphaned match the bot can never leave.
      `StayForAnotherMatchUseCase` gains an explicit player-id param so the human's client also
      readies
      up the computer(s) when staying, letting a computer match restart instead of hanging on the
      never-ready bot.
    - **Lightly-smart combat:** the bot's free turn-start choice (`CombatActionState.Idle`) is
      biased
      rather than random — it attacks the **weakest (lowest-HP) living opponent** (ties broken at
      random) to secure kills, and activates an offensive ability only when **clearly favorable**
      (currently just the Necromancer's Summon Minion while below max HP or with no minions; the
      risky
      gambits — All-In self-knockout, Prepare Attack forgoing a hit, Impersonate — fall back to a
      normal attack). Owed reaction/continuation rolls remain forced. The user's own timed-out turn
      keeps the simple random-target behavior; only the computer uses the biased policy, injected as
      a
      turn-action strategy into the shared resolver.
- **Architecture note:** the bot is **client-driven** (the challenging human's client owns and
  drives
  it), because this game has no game-logic backend — Firebase RTDB only stores shared state; all
  rules run in Kotlin `commonMain` on clients. A true backend-driven bot (the authoritative-server
  model most games use) would mean a Cloud Function reimplementing/sharing the combat rules, which
  is
  only worth it once *all* combat resolution moves server-side (whole-game anti-cheat) or bots must
  persist with zero humans connected. The decision *policy* is portable, so it can move to a backend
  driver later with minimal rewiring. Client-driving also gives precise control over the "thinking"
  delay (vs. Cloud Function cold-start latency).
- **Deferred (follow-ups):** richer combat heuristics (use Prepare Attack vs. tanky targets, All-In
  when behind, Impersonate the strongest opponent); mixed human+computer matches in one arena
  (needs a deterministic "which client drives the bot" rule with failover); per-bot
  difficulty/personality; Tavern UI to configure multiple computer opponents.

---

## Audio

### SS-134 — Music & sound-effect playback (KMP)

- [ ] Not completed
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
- **Notes:** Likely spawns child tickets (asset authoring, per-screen track mapping, mute/settings
  UI,
  audio-focus/ducking polish). Related: SS-068 (UI/animation overhaul), SS-025 (Arena message
  events).
