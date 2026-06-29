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
- **Source:** `NewArenaCombat.kt` — the root `Column` (DiceHomeSlot ~234, `BottomControls` ~238/491)
  and `DiceHomeSlot` (~715). Note the stat columns live inside `BottomControls`, so naive "move the
  buttons up" must not drag the stat columns with them.
- **Goal:** Reposition `DiceHome` and the buttons higher (closer to the avatars) while leaving the
  stat columns where they are. Likely means decoupling the stat columns' vertical placement from the
  button row.
- **Related:** SS-156, SS-157, SS-068. **Confer with designer** on exact offsets.

### SS-160 — Combat buttons: 3D pressed style; ability buttons get class-color border

- [ ] Not completed
- **Goal:** Combat buttons should read as physically pressed-down (3D) when held/pressed. Ability
  buttons should carry the player's class-color border.
- **Source:** `NewArenaCombat.kt` — shared `ArenaButton` (~1142) used by `AttackButton`,
  `AbilityButton`, `ImpersonateButton` (~1080–1140). Class color available via
  `CharacterClassType.toColor()` / `classFamilyColors` (already used by `ClassAvatarStats`/head
  icons).
- **Related:** SS-068 (Arena UI overhaul), SS-153 (class color source).

### SS-163 — Flash callout hold is too short

- [ ] Not completed
- **Symptom:** The short full-screen animation message (Hit!/Miss!/K.O.!) is a little too short.
- **Source:** `NewArenaCombat.kt` `FLASH_HOLD_MILLIS = 850L` (~159), used by `FlashOverlay` (
  SS-148).
  Note the Combat→Finished hold (`FINISH_HOLD_MILLIS`, SS-154) was sized against this; revisit it if
  the flash duration changes materially.
- **Goal:** Lengthen the flash hold so the callout is comfortably readable. **Confer with designer**
  on the exact duration.
- **Related:** SS-148 (flash callout), SS-154 (`FINISH_HOLD_MILLIS` sizing).

### SS-164 — Flash messages: full event coverage + per-ability call-outs

- [ ] Not completed
- **Goal:** The full-screen flash (SS-148) should narrate combat in a quick, fun way while the
  message log stays the full passive record. Every flash-worthy event flashes, and multiple flashes
  from a single resolution play **in sequence** (e.g. `Hit!` → `3 Damage!` → `Knocked Out!`).
- **Normal-attack wording (this pass):**
    - Armor broken (`ArmorClassHit`) → **"Hit!"** (previously no flash).
    - Armor not broken (`ArmorClassMiss`) → **"Miss!"**.
    - Damage dealt (`Damage`) → **"X Damage!"** where X is the true damage. `NormalRoll` now carries
      the consecutive-doubles `bonus` through every layer (domain → entity → state) so the call-out
      shows `firstDie + secondDie + bonus`, not just the visible dice.
    - Knockout (`Knockout`) → **"Knocked Out!"** (renamed from "K.O.!").
- **Abilities (this pass = first-pass wording, owner to refine each):** when an ability is used it
  also flashes one of these call-outs. `ArenaMessageState.flashText` is the single extension point —
  each ability's primary outcome message returns punchy text; intermediate Trigger/Roll steps stay
  null. **Owner will refine the exact wording for every ability.**
- **Infrastructure:** `flashText` (in `ArenaMessageState.kt`) is the per-message wording source;
  `ArenaScreenState.flashMessages` exposes the ordered flash-worthy messages; `FlashOverlay`
  (`CombatFlashOverlay.kt`) queues newly-arrived flashes and plays them one at a time, skipping the
  history present on first composition.
- **Related:** SS-148 (flash callout), SS-163 (flash hold duration), SS-029 (string resources),
  SS-025 (message-log animation).

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
> by the NewArenaCombat layout — SS-144 (target-selection transition) and SS-150 (inspect a player),
> respectively — so the placeholder tickets were removed.

---

## New Arena combat layout (NewArenaCombat)

`NewArenaCombat.kt` is the in-progress rewrite that will replace the current `ArenaCombat.kt`. It
re-shapes the combat screen into two halves: a **top half** of opponent head icons + a messages
overlay, and a **bottom half** of the active player's and current target's full avatars flanking the
dice, with a button bar below. The structure is deliberately split into many small, single-purpose
composables (one per logical screen segment); keep new work in that shape rather than growing any
one
function. Each ticket below owns one segment. Parent: SS-142 (which is itself a child of SS-068).

### SS-142 — NewArenaCombat scaffold & migration to replace ArenaCombat

- [~] In progress — **swap is live:** `LoadedArenaScreen` renders `NewArenaCombat` for
  `ArenaState.Combat`; `NewArenaCombat` now roots its own `SharedTransitionLayout` (SS-144), so it
  is
  a plain composable rather than a `ColumnScope` extension. `ArenaCombat.kt` is intentionally **kept
  **
  as the reference to migrate logic from; delete it only at the very end, once every behavior has
  been
  confirmed carried over and nothing was missed.
- **Source:** `NewArenaCombat.kt` top-level (file header + `NewArenaCombat` orchestration).
- **Goal:** Stand up the new two-half combat layout, fill in its child segments by migrating the
  behavior from `ArenaCombat`, and only then delete the old `ArenaCombat.kt`.
- **Shape (rearranged 2026-06-26):** `TopHalfContent` = `Box { ClassIcons(); Messages() }` (icons
  sit
  below the messages overlay); `BottomHalfContent` = `Column { Row { ClassAvatar(user);
  ClassAvatar(target) }; DiceHome(); Row { statsColumn(Left); Buttons(); statsColumn(Right) } }` —
  the
  two full avatars now sit side by side *above* `DiceHome`, and a bottom row flanks the `Buttons`
  with
  a per-player stat column on each side (SS-146, moved off the avatars). Keep each segment its own
  small composable.
- **Wiring to preserve:** the existing `ArenaState.Combat` contract `ArenaCombat` already honors —
  target selection hoisted as one source of truth (`selectedOpponentUserId` / `lockedTargetUserId` /
  `autoTargetedOpponentUserId`, optimistic target lock, SS-101 auto-target), `primaryRollControl` /
  `secondaryControls` split, `onAttack` / `onUseAbility` from `ArenaActionsProvider`. Thread these
  through the new composables rather than reinventing them.
- **Wrap-up:** wrap the screen in `SharedTransitionLayout` + `AnimatedContent` so the per-segment
  shared-element transitions (SS-144, SS-149) work; keep `UserStatsRow` or fold it into the new
  avatar/stat segments as those land.
- **Notes:** Tracking/parent ticket for SS-143…SS-151. Child of SS-068. Until the migration lands,
  `NewArenaCombat` is unreferenced scaffolding and the live screen stays on `ArenaCombat`.

---

## Domain layer

### SS-048 — Die-value enum

- [ ] Not completed
- **Source:** `DiceRoller.kt:16` (`// TODO Enums for values 1 through 6?`)
- **Goal:** Consider a type-safe enum for die faces (1–6) instead of raw Ints.

---

## Bugs

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

### SS-166 — Dice faces don't match the rolled number shown in the message

- [ ] Not completed
- **Symptom:** The value reported in a combat message did not match the faces shown on the dice.
  The message number is the authoritative roll; the dice rendered different pips. Recurring — has
  been observed more than once. A regression of the single disabled die rolling when it shouldn't
  has also happened. This has been "fixed" many times - let's please stop fixing and breaking.
- **Suspected culprit:** the dice rendering, not the message/domain. Likely the dice faces shown
  are not driven by (or drift from) the same roll values the resolution/message uses — a display
  bug rather than a wrong roll. Confirm whether the message value is correct and only the dice
  faces are wrong (vs. the reverse).
- **Source (to investigate):** the dice roll pipeline — `DiceRoll`/`rememberDiceRoll`,
  `CombatDiceOverlay.kt` (`DiceOverlay`/`Die3d` `dieFaces`/`displayedDiceCount`), and how rolled
  values flow from the domain resolution into both the message and the dice faces. Check for a
  race between the animated roll and the committed result, or stale/mismatched face state.
- **Repro notes:** capture the message text + the dice faces shown + class/ability when it next
  happens; note whether it was a normal attack or an ability roll, and single- vs two-die.
- **Related:** SS-077 (dice tech), SS-078 (dice UI), SS-158 (single-die freeze), SS-149 (dice
  overlay).

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
