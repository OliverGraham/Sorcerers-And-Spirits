# Completed Backlog

Archive of finished tickets — one line each (ID, title, goal). Full resolution detail,
rationale, and verification live in the git history (commit + PR per ticket); search the ID.
Active work is in [BACKLOG.md](BACKLOG.md).

## Tests

- **SS-001** — Fix Tavern repository tests: Restore the Tavern repository test suite to a passing, meaningful state.
- **SS-002** — Repair broken ArenaScreenViewModel test: Fix the broken/abandoned test around line 342 so it asserts correct behavior.
- **SS-003** — Clarify Charm test comment & assertion: Resolve the confusing comment about the Charmed target and verify the assertion matches the intended Persuade/Charm rule from CLAUDE.md.

## Tavern UI

- **SS-004** — Decide source of class list in TavernState: Decide whether the class list should come from RemoteConfig or stay static (`CharacterClass.getAllClasses()`). Related: SS-047.
- **SS-005** — Move navigateToArena out of TavernState: Reconsider whether `navigateToArena` belongs in state vs. an event/effect channel.
- **SS-006** — Consolidate navigate-to-arena callback at App level: Decide whether the `onNavigateToArena` wiring should live at the App level instead of per-screen.
- **SS-007** — Real error UI for error states: Replace placeholder retry/empty handling with proper error UI shared across screens.
- **SS-008** — Reusable Tavern preview state & previews: Build reusable example/preview state for Tavern composables and add/refresh previews.
- **SS-009** — Improve TheBar: Clarify and improve `TheBar` (scope is vague — define concretely while working on it).
- **SS-010** — Reword challenge-list UI copy in TheBar: Update copy to clearly communicate "other players to challenge."
- **SS-011** — New challenge icon in TheBar: Replace the placeholder icon with the intended challenge icon.
- **SS-012** — Update TheBar styling: Finalize styling for `TheBar`.

## Arena UI

- **SS-112** — Persist dice on screen; re-roll animation on any player's roll: Lifted the dice out of the per-turn roll controls into a persistent `DiceTray` placed in `ArenaCombat` outside the turn-keyed `AnimatedContent`, so it stays on screen the whole match. The tray's faces come from `latestRolledDice` and `Die3d` now tumbles-and-settles on any value change (any player's roll), in addition to spinning while held. The roll controls keep the hold/shake/release gesture (only present on the user's turn) but no longer draw dice; they report their hold up via `onRollHoldChange` so the tray's dice spin while held and settle on release. Builds on SS-077/SS-113.
- **SS-113** — Dice physically roll around their container and bounce off the walls: `Die3d` now lives in a tray larger than the die and runs a tiny per-frame 2D physics integrator (`tumbleDieAroundTray`) — on release the die gets a random velocity, ricochets off the tray walls with restitution, tumbles in proportion to its travel, and slows under friction until it comes to rest, then rights itself onto the rolled face. Replaces the earlier scripted single-arc path.
- **SS-108** — Lock a player to one class for the whole Gauntlet: A player keeps the same class for all 3 matches of a Gauntlet; between matches the lobby skips ChoosingClasses and rebuilds Combat directly with fresh full-health classes, while "Go Again" after the final match starts a new Gauntlet back at class selection. The between-match/gauntlet-complete dialog is removed and Next Match/Go Again/Leave now live on the Finished screen.
- **SS-013** — Review ArenaEvent (empty TODO): Empty placeholder TODO — determine intent (likely additional events) or remove.
- **SS-014** — Handle match/timer expiration per state: Define and implement what happens when the timer expires in each `ArenaState`, and how the `MatchTimedOut` event is surfaced to the user.
- **SS-015** — Delete unused ArenaActions: Remove the dead code flagged for deletion (verify no usages first).
- **SS-016** — Rename/refactor ArenaActions: Rename/refactor the flagged ArenaActions construct for clarity.
- **SS-017** — Refactor ArenaScreenViewModel: Address the broad "this is all fucked" flag — clean up the ViewModel section it marks.
- **SS-018** — Better userId resolution: Establish a cleaner way to obtain the current user's id.
- **SS-019** — Track doublesBonus in ArenaState.Combat: Track and pass the real `doublesBonus` (currently hardcoded `null`) per the doubles +1 consecutive-roll rule in CLAUDE.md.
- **SS-020** — Fix Charm logic in onUseAbility: Correct how `selectedCharmTargetUserId` is resolved for Persuade/Charm. Related: SS-049.
- **SS-022** — Remove arena match when last player leaves: When the last player leaves, remove the arena match.
- **SS-023** — Resources + split functions in ArenaChoosingClasses: Extract string resources and split the composable back into separate functions.
- **SS-024** — Possibly add finished-state content: Decide whether the finished-match screen needs additional content; add it or keep as-is.
- **SS-030** — Rename/assign property in ArenaScreenState: Improve the flagged property's name and assignment.
- **SS-032** — Decide timer-start strategy: Decide and document how timers should be started (e.g. based on `NextCombatAction`).

## App / Main

- **SS-035** — Random names from RemoteConfig: Source random traveler names from RemoteConfig instead of hardcoding.

## Data layer

- **SS-038** — Determine if IOSNullableStringSerializer is needed: Decide whether this serializer is required; keep with justification or delete.
- **SS-039** — Unique user-names collection: Implement a names collection to enforce unique display names. Related: SS-035.
- **SS-040** — Paginate / separate node for arena messages: Reduce payload size by paginating messages or moving them to a separate RTDB node.
- **SS-041** — Refactor flagged arena repo code: Refactor the flagged section of `FirebaseArenaRepository`.
- **SS-042** — Challenge timestamp handling: Resolve how the challenge timestamp is represented/serialized across the data entity and domain model.
- **SS-043** — Extract class to separate file: Move the flagged class out of `FirebaseTavernRepository` into its own file.

## Domain layer

- **SS-046** — Add maximum HP: Introduce a max-HP concept so healing/shields can't exceed a cap (interacts with Necromancer heal-to-full, shields).
- **SS-047** — Class stat values from RemoteConfig: Drive class stat tuning values (`hitPoints`/`maxHitPoints`/`armorClass`/`speed`) from RemoteConfig instead of hardcoding. Related: SS-004, SS-035. Ability roll values split out to SS-058.
- **SS-059** — Single-source class stats via bundled JSON (drop factory stat literals): Make the version-controlled `character_class_stats.json` the single source of default stats: bundle it as a `commonMain` resource and use it as both the RemoteConfig in-app default (`setDefaults`) and the…
- **SS-111** — Make RemoteConfig the only production source of class stats: Delete the `CharacterClass.getRogue()`/`getAllClasses()` stat factories so the bundled `character_class_stats.json` (+ Firebase) is the sole stat source; abilities wired in code via a `create(type, stats…)` builder with an exhaustive `when`. Tests keep factory-style builders in a `commonTest` fixture. Related: SS-047, SS-059, SS-004.
- **SS-085** — Bundled-JSON in-app defaults for all RemoteConfig keys: One small loader reads each parameter's version-controlled JSON from `composeApp/src/commonMain/composeResources/files/remote_config/<key>.json` and uses it as both the RemoteConfig in-app default (`setDefaults`) and…
- **SS-049** — Persuade: pass selectedCharmTargetUserId from UI: Ensure the UI supplies the Charm target id for Persuade. Related: SS-020.
- **SS-050** — Implement KillingMomentum: Implement the Rogue "Killing Momentum" rule — on a K.O., attack another chosen player.
- **SS-054** — SpellPotency splash-damage attacker case: Handle the Paladin Holy Shield retaliation when the incoming damage was SpellPotency splash damage (who is the "attacker"?).

## Dependency injection

- **SS-055** — Scope DI definitions instead of single: Introduce appropriate Koin scopes rather than `single` for everything.
- **SS-056** — Name DI definition: Give the flagged DI definition a meaningful name.

## Tavern timing

- **SS-057** — Challenge timer must use the communal backend deadline: Drive the challenge countdown from the shared backend deadline instead of a hardcoded 60s anchored to when each client first observed the challenge. Related: SS-042.

## Bugs

- **SS-060** — Holy Shield: suppress KnockedOut dialog during the survival roll: When a Paladin is knocked out but is about to roll (or is rolling) for Holy Shield, the KnockedOut/match-decision dialog must NOT show. The dialog should only appear once Holy Shield has resolved and the Paladin is…
- **SS-090** — First-launch "Continue with Google" must recover an existing account, not error
- **SS-095** — Lock target selection once an attack roll is committed: After a player has rolled against a target for a normal attack (i.e. committed the Armor-class roll), they must not be able to change targets or switch to an activated ability mid-action. The UI must also not highlight…
- **SS-096** — Keep the rolled-against opponent targeted through ability resolution: Ensure the originally-targeted opponent remains the target through the full resolution of an in-flight attack (including defensive-ability roll-offs that bounce back to the attacker).
- **SS-093** — Ability outcome messages must reflect the actual roll result: Give Summon Minion (fizzle/skeleton/skeleton+heal/golem+heal) and All-In (self-KO/no-effect/double-attack) per-outcome message copy keyed off the rolled die; every other multi-outcome ability already carried its specific outcome.
- **SS-103** — ArenaMessages permanently disappearing in some flows: Clamp the running unhandled-message count to the (capped/reset) log size in toUpdatedState so it can't outrun the messages that remain — otherwise latestMessage resolved to null and filteredMessages to empty, wiping the log with no way to recover (seen after big combat results and on match transitions).
- **SS-097** — ArenaMessages must use the character class name, never the displayName: The userId→name resolution map fed to ArenaMessage.toState now holds each player's class name (built from Selected players, persisted across leaves), so all combat messages read as "Rogue"/"Bard"; only the pre-class lifecycle messages (ChoseCharacterClass, ConnectedToArena) still carry their own displayName.
- **SS-098** — Defer the Finished transition until end-of-match messages/animations play: When a match finishes while closing messages remain unhandled, toUpdatedState holds the combat view (updated to show the final knockout) and stashes the finished state as pendingFinishedState (stopping the combat timer); once the messages drain, onIncomingMessageHandled advances arenaState to Finished — so the closing knockout/animation is no longer cut off.
- **SS-099** — Disable Impersonate for a class the Illusionist has already copied: ArenaState.Combat.isImpersonateRedundant compares the currently-impersonated offensive ability (by type) against the selected target's class offensive ability; AbilityControls folds that into the Impersonate button's enabled state, so re-copying the same class (a no-op) is now disabled while that opponent is targeted.

## Profile & Auth

- **SS-061** — Investigate Firebase Authentication for user profiles: Choose the easiest and most robust way for users to create and own a profile. Evaluate Firebase Auth providers — Google Sign-In and phone (mobile) auth are the leading candidates — for a KMP (Android + iOS) app, and…
- **SS-062** — Profile deletion in Firestore: Add the ability to delete a user's profile from Firestore, with the corresponding domain use case and repository method.
- **SS-063** — App Drawer cleanup: The drawer currently holds ad-hoc items (a raw Display Name field, Tavern/Arena links). Replace it with a deliberate set of entries. The name-change capability moves out of the drawer and into the ProfileScreen…
- **SS-064** — ProfileScreen: Add a ProfileScreen, accessed from the app drawer (SS-063). It contains the name-change capability (moved out of `SettingsDrawer`), profile deletion (SS-062), and, once game-stat handling exists, the user's own game…
- **SS-086** — Android: native Google Sign-In token acquisition: Implement the Google account picker on Android via `androidx.credentials` (`CredentialManager` + `GetSignInWithGoogleOption` using the Firebase web client ID) and return `SocialTokenResult.Google(idToken)`.…
- **SS-087** — iOS: native Google + Apple Sign-In token acquisition: Implement Google (GoogleSignIn SDK → `SocialTokenResult.Google(idToken, accessToken)`) and Apple (`AuthenticationServices` / `ASAuthorizationAppleIDProvider` → `SocialTokenResult.Apple(idToken, rawNonce)` with a…

## Game Stats & Leaderboard

- **SS-021** — Infrastructure for saving game stats to user profiles (Firestore): Establish the generic mechanism for persisting game statistics onto a user's Firestore profile, independent of which specific stats are recorded. This is the foundation the rest of the stats work builds on…
- **SS-073** — "Gauntlet" concept: group 3 consecutive matches as one unit: Introduce a new game-term, **Gauntlet**, sitting above `Match` in the hierarchy (`Turn` → `Round` → `Match` → `Gauntlet`). A Gauntlet is a fixed run of `MATCHES_PER_GAUNTLET` (= 3) consecutive matches in the same Arena.…
- **SS-065** — LeaderboardScreen: Once game stats are recorded on user profiles (SS-021 infrastructure + SS-072 schema), add a screen showing the top players. Accessible from the drawer (SS-063).
- **SS-071** — Forfeit vs finished leave + stats update: Distinguish a forfeit-leave from a leave after the match finished, and update stats only when the user actually leaves (possibly after multiple matches). First concrete consumer of the SS-021 stats infrastructure — the…
- **SS-072** — Define which game stats to record: Decide the concrete set of game statistics to persist per profile (e.g. matches played, wins, losses, forfeits, K.O.s dealt, per-class records, etc.). Deferred until the SS-021 infrastructure exists — design the stats…

## Rules & Help

- **SS-067** — GameRulesScreen: A screen, accessible from the drawer (SS-063), where users can read the game rules and class abilities.

## UI overhaul & input

- **SS-069** — Shake-to-roll dice UX: Investigate a dice-rolling interaction where the user taps and holds a pair of dice (or a single die, depending on what the current step requires), shakes the phone to simulate rolling, and on release the existing…

## Release & versioning

- **SS-070** — Investigate app version control & force-upgrade: Establish a way to gate clients by app version so an outdated client can be soft- or force-upgraded — important since all clients share one RTDB game-state schema and a stale client could read/write incompatible data.
