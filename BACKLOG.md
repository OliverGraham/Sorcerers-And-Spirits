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
    - **Log:** `arena_message_*` — the more complete, passive record, built in
      `ArenaMessageDisplay`.
      Final wording **TBD** (owner-refined).
- **No wrapper type:** one `ArenaMessageState` maps to both resources at the two display sites;
  there
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

### SS-029 — Use StringResources in ArenaCombatControl

- [ ] Not completed
- **Source:** `ArenaCombatControl.kt:164` (`// TODO: Always StringResources`)
- **Goal:** Replace hardcoded strings with string resources.

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

### SS-200 — Attacks-remaining stat in AvatarStatsBar (turn owner only)

- [ ] Not completed
- **Goal:** Add an attacks-remaining stat + icon to `AvatarStatsBar` so the current turn owner sees
  how many attacks they still have this turn, decrementing as they spend them and bumping up when an
  ability grants a bonus attack.
- **Count is domain-owned:** `CharacterClass.attacksRemaining` (persisted via `CharacterClassEntity`
  so every client agrees). Encore/Killing Momentum keep no persistent flag, so a UI derivation
  can't see their bonus — the domain must track it. Maintained in `CombatMutator`:
    - **Reset** at each turn start (beside `resetMinionAttacks`) to `1 + minionAttacks`.
    - **Decrement** optimistically the instant dice are rolled — the turn owner's opening
      Armor-class roll (`NormalAttackRollHandler`) — floored at 0.
    - **Increment** on each bonus-attack grant: All-In attack-twice, Encore success, Killing
      Momentum trigger. (Minion follow-ups are already in the reset total; All-In does not decrement
      since it doesn't use up an attack.)
- **Visibility:** shown only for the avatar whose player is the current `turnOwnerUserId`; hidden
  otherwise. `turnOwnerUserId` threaded from `ArenaState.Combat` through `CombatAvatarColumn`.
- **Animation:** the pill enters/exits with `AnimatedVisibility` when the turn owner changes; the
  number swaps with `AnimatedContent` when `attacksRemaining` updates.
- **Icon:** reuses the existing `ic_attack_button` drawable (rename the resource later).

> The two head-icon transition targets originally tracked here as SS-140 (head → large targeted
> avatar) and SS-141 (head → expanded stat view) were placeholders; both are now realized concretely
> by the ArenaCombat layout — SS-144 (target-selection transition) and SS-150 (inspect a player),
> respectively — so the placeholder tickets were removed.

---

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

### SS-193 — Gambler ability animations

- [ ] Not completed
- **Goal:** TBD — animations to be described.
- **Cue sources:** `ArenaMessageState.AllIn`, `ArenaMessageState.LuckyCoin.*`,
  `ArenaMessageState.NormalRoll` (attacker side).
- **Parent:** SS-191.

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
  (success) or boos (failure). A failed Encore greys the music the way the other Bard failures do.
  After Encore finishes the lights come up and the darkness lifts.
- **Done (first pass, shipped):** full-screen concert scene with the darkness fall/lift, spotlight
  cone + pool, receding crowd of all eight avatars (ambient sway, wild-jump vs boo-shake), 3D
  chrome microphone (radial-shaded head with sweeping glint, banded stand, domed base), music-note
  burst + sound rings on the roll, the grey drain on failure, the vignette frame, and the cue/phase
  wiring (`EncorePhase` SetStage/Success/Failure) plus success/failure previews. Everything is
  hand-drawn Canvas over the avatar PNGs (`BardEncore.kt`, `EncoreStage.kt`).
- **Note:** the concert scene is hosted inside the weighted stage region (above the seating, below
  `BottomControls`) rather than over the whole screen, so the roll buttons stay visible/usable while
  the darkened stage holds waiting for the Encore roll — it darkens the stage, not the controls bar.
- **Follow-up:** Make sure Encore is not triggered in incorrect scenarios (such as knocking out the
  last opponent). Replace the hand-drawn microphone with an image asset for a truer 3D look (keep the
  glint, note behaviours, grey drain, and darkness fall/lift choreography); polish/tune the crowd
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

### SS-197 — Illusionist normal-attack animation

- [ ] Not completed
- **Goal:** TBD — attacker-side normal-attack animation set to be described.
- **Cue sources:** `ArenaMessageState.NormalRoll` (attacker side).
- **Parent:** SS-191. Abilities split into SS-208 (Impersonate) and SS-209 (Mirror Image).

### SS-208 — Illusionist Impersonate animation

- [ ] Not completed
- **Goal:** TBD — animation to be described. Offensive ability: copy a living opponent's offensive
  ability until end of match or another is chosen.
- **Cue sources:** `ArenaMessageState.Impersonate`.
- **Parent:** SS-191. Split out of SS-197.

### SS-209 — Illusionist Mirror Image animation

- [ ] Not completed
- **Goal:** TBD — animation to be described. Defensive ability: once per attack phase, reroll 1d6
  and
  give the result -1 (during ArmorClass or Damage rolls against the defender).
- **Cue sources:** `ArenaMessageState.MirrorImage.*`.
- **Parent:** SS-191. Split out of SS-197.

### SS-198 — Mage normal-attack animation

- [ ] Not completed
- **Goal:** TBD — attacker-side normal-attack animation set to be described.
- **Cue sources:** `ArenaMessageState.NormalRoll` (attacker side).
- **Parent:** SS-191. Abilities split into SS-210 (Spell Potency) and SS-211 (Teleport).

### SS-210 — Mage Spell Potency animation

- [ ] Not completed
- **Goal:** TBD — animation to be described. Offensive ability: roll doubles to pass armor or deal
  damage → dice-total damage to target and half to all other opponents, then end turn.
- **Cue sources:** `ArenaMessageState.SpellPotency`.
- **Parent:** SS-191. Split out of SS-198.

### SS-211 — Mage Teleport animation

- [ ] Not completed
- **Goal:** TBD — animation to be described. Defensive ability: when armor is passed, roll off 1d6
  with the attacker; if the Mage rolls higher, escape the attack.
- **Cue sources:** `ArenaMessageState.Teleport.*`.
- **Parent:** SS-191. Split out of SS-198.

### SS-199 — Paladin normal-attack animation

- [ ] Not completed
- **Goal:** TBD — attacker-side normal-attack animation set to be described.
- **Cue sources:** `ArenaMessageState.NormalRoll` (attacker side).
- **Parent:** SS-191. Ability split into SS-212 (Holy Shield).

### SS-212 — Paladin Holy Shield animation

- [ ] Not completed
- **Goal:** TBD — animation to be described. Defensive ability: once per match, when K.O.'d, roll
  2d6; on 10/11/12 the Paladin survives with 1 Health and deals unblockable 1d6 to the attacker (a
  12
  also stops Duelist's Prepare Attack).
- **Cue sources:** `ArenaMessageState.HolyShield.*`.
- **Parent:** SS-191. Split out of SS-199.

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
