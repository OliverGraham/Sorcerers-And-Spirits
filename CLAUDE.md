We are building a Kotlin Multiplatform Compose dice-combat game called Sorcerers and Spirits.

The app has Data, Domain, and UI layers.

Game state flows circularly through the app:

Data -> Domain -> UI -> user chooses action/targets -> Domain -> Data -> emitted/distributed to
other clients

The game uses Firebase's RealTime Database as the shared persisted game-state source.

Outstanding work is tracked as tickets in BACKLOG.md (active tickets only); finished tickets are
archived as one-line entries in COMPLETED.md, with full resolution detail in the git history (commit
+ PR per ticket). IDs are stable and globally unique, so a `grep` for an ID finds it in either file.
TODO comments in the code are tagged with their ticket ID, e.g. `// TODO(SS-019): ...`; grep an ID to
find every related TODO. When asked to work a ticket, read its entry in BACKLOG.md; when it's done,
move it from BACKLOG.md to COMPLETED.md (as a one-line entry) rather than checking a box.

Game terminology:

- "Gauntlet" A fixed run of consecutive matches (currently 3) within the same arena. It sits above
  Match in the hierarchy: Turn -> Round -> Match -> Gauntlet. A player keeps the same class for the
  whole Gauntlet; starting a new Gauntlet (e.g. "Go Again" after the final match) returns to class
  selection.
- "Match" Entire Arena battle from start to end. Players can join an arena and play for as many
  matches as they want
- "Round" A round consists of every player acting. A new round begins once every player has acted
  and the cycle starts again (the first round of a match is round 1).
- "Turn" A player attacking or using their ability
- "Activated Ability" an ability that is used instead of attacking (Impersonate and Allin are an
  exceptions and don't use up an attack).

# Always-enforced invariants

These are hard rules, not preferences. They are violated when they sit only in recall memory, so they
live here and apply every turn.

## Style

- **Documentation — write none.** No `//` comments, block comments, or KDoc, ever, unless the user
  explicitly asks in that specific request. Prefer self-documenting code (good names, small extracted
  functions) over any comment. Actively delete comments/docstrings you encounter while editing — never
  edit one in place. Sole exception: `TODO(SS-xxx)` ticket tags. When a docstring is explicitly
  requested: use KDoc `/** */` above the method (not `//`), and reference symbols with `[square
  brackets]` when resolvable / plain text otherwise — never backticks.
- **No `for` loops.** Use `.forEach`/`.forEachIndexed`/`.map`/`.withIndex()`/`indices.reversed().forEach` etc.
- **Named arguments** in Kotlin calls wherever possible, including library calls (`startsWith(prefix = x)`).
  Exceptions: varargs (`listOf`), single no-arg calls, operator-call syntax.
- **Null handling:** bind to a named `val` and `if (x != null)` (smart-cast); don't reach for `?.let`.
- **Booleans** get an `is`/`has`/`should`/`can` prefix.
- **No compound conditions in `if`** — extract to a well-named `val` (or `takeIf`), then check that.
- **`when` branches always use `is`**, even for `data object` singletons.
- **Braces on every `if` body, except a single early `return`** (stays brace-less, one line).
- **Derived values:** on a class you own, `val x: T = expr` (computed once). For extensions or types you
  don't own, write `fun T.x(): R` — never `val x get() = …` (it hides computation behind field-access
  syntax). Reserve `get()` only for interface/override props that can't be a function or initializer.
- **Compose animation labels** are `private const val`s, never inline literals.
- **Don't extract a single-use dp/size to a `val`** — inline it. Named vals only for reuse (labels excepted).
- **No constructor-arg enums.** Prefer a sealed interface, or a plain enum + exhaustive `when`. Keep a
  plain enum when it's a serialization/wire key.
- **Never name anything "Helper"/"Helpers"/"Util"/"Utils"** — name by domain concept.
- **No terse/context-free parameter names** — name for the concept (`diePosition`, not `at`).
- **Top-down code order:** callers above callees; public API above the private helpers it delegates to.
- **In tests, assert instead of casting** (`assertIs`/`assertEquals`, not `(value as Foo).bar`).
- **userId-keyed maps use `getValue(key = …)`** (fail-fast), never a `?: fallback` — a missing userId is
  a real bug, not something to paper over.

## Architecture

- **Three-layer type mirroring.** Every modeled type exists as Domain (raw — plain class, no annotation
  or suffix, the source of truth), Data (`@Serializable`, `…Entity` suffix, serializes to Firebase), and
  UI (`@Immutable`, `…State` suffix), converted by `toEntity()`/`toDomain()`/`toState()`. Enums are the
  exception: the Domain enum is reused raw across all three layers (no Entity/State mirror).
- **Domain is the source of truth.** Arena-combat domain is hand-verified; every Domain field must
  round-trip faithfully through the Data and UI mappings. When a Domain field is added or changed, update
  the entity mapping AND the UI-state mapping in the same change. When a mapping misbehaves, suspect the
  Data/UI mapping, not the Domain.
- **No default constructor args to paper over mappings.** Prefer required params for any field that
  persists or crosses a layer boundary — a default lets mapping code compile with the field silently
  unmapped, turning a compile error into runtime data loss. Add a default only for genuine optionality or
  deserialization back-compat. On a "state didn't persist/reset" bug, suspect a mapping gap masked by a
  default before sync/timing.
- **Derive in the state model / domain, never in Composables.** First ask whether the derivation is
  needed at all — raw data plus an existing predicate usually answers it; don't materialize a redundant
  parallel property. If it is needed, compute it once at construction as a property on the type that owns
  the source data (thread inputs like `userPlayerId` into the builder); the Composable only reads it.
  Extension funcs and `remember{}` blocks called from a Composable are still deriving in the UI — same
  violation. Per-user derivations (current userId) belong in the domain: the userId comes from Data via
  Domain, not the UI.
- **Domain hands the UI display-ready values.** The UI only plugs values into strings — no arithmetic
  (`total - bonus`) or branching at a display site. Carry separate display-ready fields (e.g. `baseRoll`
  + `doublesBonus`); keep an effective total too when game logic needs it — don't collapse the two.
- **No raw user-facing strings in data/model types.** The type carries identity; the UI resolves
  type→stringResource at render. A raw String belongs in a type only when it comes from the backend.
- **No alias/rename-only properties when mapping.** Keep the same field name across Domain↔Data↔UI; use
  the existing name at call sites instead of adding an aliased accessor.
- **Pass decisions through the layers as explicit properties; never scan the message log (or any list)
  to reconstruct one.** The message log is a passive, append-only display feed — it dictates no logic,
  pacing, or screen-state transitions. Screen-state advancement lives in the ViewModel on its own clock;
  only an animation ever justifies a wait. (Don't add a duplicative field when the decision is already
  derivable from state that's passed down.)
- **Apply DRY proactively.** Extract byte-identical logic across sibling files yourself, during the work
  — don't wait to be asked. Leave a genuine outlier alone and say why.
- **Keep files small; pass state objects, not 5-6 args.** Split distinct UI parts into purpose-named
  packages (one cohesive component + its private helpers per file) before a file bloats past a few
  hundred lines. Bundle a long param list into an existing state holder.

## Resilience

- **An uncaught exception must NEVER crash the app — absolute.** Catch at the data layer and
  propagate a `Result`/error type up through Domain to a UI error message; a backend/permission error
  is a handled result, never fatal. Always rethrow `CancellationException`.

## Communication

- **Plain, professional language — no absorbed jargon.** Never say "clobber" (use "overwrite"/"replace"/
  "tear down") or "drain" a queue (say "dequeue" / "the queue is empty"). Scan replies for these before
  sending; both are zero-tolerance bans.

## Workflow

- **The user compiles; you don't build.** Never run `./gradlew` build/compile tasks or
  `mcp__ide__getDiagnostics` to verify — both are unreliable or time out, and the user compiles. Make
  the edits, reason about correctness, and hand off.
- **Trace before changing; never claim it works.** Read every file involved in a mechanism end-to-end
  before editing, then make the complete fix — no partial or guessed changes. You can't run the app, so
  state exactly what changed and ask the user to verify; never say "works"/"works perfectly."
- **Don't build what you suspect is wrong.** If a design smells off or you're rationalizing a compromise,
  stop and trace the real architecture first; ask a precise question if still unsure. Never proceed on a
  hunch you don't believe. Never dismiss a known edge case as "rare" — handle it correctly or surface the
  cost and let the user decide.
- **Fix all siblings in the same pass.** When a change touches one of a set of parallel cases (sealed
  branches, variants), extend identical treatment to every sibling — never a partial fix, never "want the
  others too?" (the answer is yes).
- **Do the simplest thing.** Reach for the most boring construct that literally satisfies the request;
  don't add animations, wrappers, optimizations, or abstractions a ticket didn't ask for (they cause
  regressions). Keep a small request small — state the plan and get a yes before any multi-file or
  cross-layer refactor. Prefer the platform's built-in behavior (e.g. Firebase offline/reconnect) over
  re-implementing it.
- **Never remove existing functionality without an explicit user instruction.** A ticket's wording
  ("replace X") is not permission to delete UI the user didn't mention — default to additive/preserving
  changes.
- **Never delete a TODO** (even a bare `// TODO:`) just because you're editing nearby. Either do the work
  it describes or leave it verbatim — the one exception to the delete-comments rule.
- **No tests until asked.** No regression/guardrail tests, no proactive assertion updates. If existing
  tests break from a change, fix the compile/signature fallout minimally; wait for an explicit "add
  tests."
- **Act on sensible defaults; don't ask permission.** Pick the reasonable interpretation and proceed,
  surfacing decisions in the summary. Reserve questions for genuinely destructive/irreversible actions.
- **Verify gate before committing; ship only on the explicit word.** After changes, pause and let the
  user verify; commit only when they signal (ship/commit/lgtm). "Ship" then runs the full autonomous
  chain — format → branch → move the closed ticket from BACKLOG.md to a one-line COMPLETED.md entry →
  commit → push → PR → merge → sync → delete branch. Canonical steps in `.claude/commands/ship.md`.

# Game rules reference

The canonical spec for the game's mechanics. The domain layer is the source of truth for these; keep
it authoritative and mirror it faithfully in Data/UI.

Core normal combat rules:

- 2 to 8 players.
- Each player chooses one class.
- No duplicate classes.
- On a player's turn, they choose a target.
- Normal attack uses 2d6.
- First roll is against the target's ArmorClass.
- If the roll is greater than or equal to the target's ArmorClass, armor is passed.
- Then the attacker rolls again against the target's Health.
- If the roll is greater than or equal to the target's Health, the target is Knocked-out.
- Regardless of whether the target lives or is knocked-out, the turn proceeds to the next living
  player.
- Knocked-out players are skipped.
- The game ends when one or zero non-knocked-out players remain.
- When a player rolls two dice and each die is the same value, the roll is considered "doubles".
    - "Doubles" dice give the player who rolled them a +1 bonus to their next consecutive roll.
    - If a player rolls doubles but then it's another player's turn, or their ability has them roll
      both of the dice, then the player who rolled doubles no longer gets a +1 bonus, because the
      roll is not consecutive.
    - For example, if an attacker rolls doubles against the Mage and breaks armor, then the attacker
      DOES get the +1 bonus to that Teleport roll off because it's a consecutive roll. If the
      attacker attacks the Duelist and breaks armor with a doubles roll, then it would go to
      Duelist's Riposte ability and this would nullify the attacker's double roll, because the
      Duelist got the dice and the doubles roll is no longer consecutive.

Class abilities:

Rogue:

- Offensive: Killing Momentum
    - If you K.O. an opponent, attack another player of your choice.
- Defensive: Dodge
    - If an opponent fails to break your armor, gain +1 Armor, up to +2, for the rest of the match.

Gambler:

- Offensive: All-In
    - Activated choice - Does not use Gambler's normal attack roll
    - Roll 1d6.
        - on 1, Gambler knocks themselves out. on 5 or 6, Gambler attacks twice this round. on 2, 3
          or 4, there's no effect.
- Defensive: Lucky Coin
    - When attacked and armor is broken, add 1d6 to Armor until turn end.
        - On 6, deal 1d6 damage to attacker.

Duelist:

- Offensive: Prepare Attack
    - Activated - Used instead of normal attack roll
    - Roll 2d6.
        - 10, 11 or 12 K.O.s the target.
- Defensive: Riposte
    - When opponent passes your armor, roll 2d6.
        - on 6, 7 or 8, Duelist turns the attack around so defender attacks opponent instead.

Necromancer:

- Offensive: Summon Minion
    - Instead of attacking, roll 1d6.
        - On 1, no effect ("fizzle"). On 2 and 3, gain 1 skeleton. on 4 and 5, gain skeleton and
          necromancer heals to full. on 6, gain 1 golem and necromancer heals to full
    - Skeleton gives +1 Health shield and an extra attack on next turn.
    - Golem gives +2 Health shield and an extra attack on your turn.

Bard:

- Offensive: Encore
    - After dealing damage, roll 1d6.
        - On 5 or 6, Bard attacks again. On 1, 2, 3 or 4 no effect.
- Defensive: Persuade
    - When armor is passed, roll 1d6.
        - on 5 the attack is stopped. on 6 the attack is stopped and Bard forces attacker to attack
          another target (targets cannot attack themselves and cannot attack the Bard, so this is
          only
          possible with 2 or more alive, non-Bard players)

Illusionist:

- Offensive: Impersonate - Does not use Illusionist's normal attack roll
    - Copy an offensive SPC ability from a living opponent until end of match or until another is
      chosen.
- Defensive: Mirror Image
    - Once per attack phase, reroll 1d6 and give the result -1. Result can be 0. This reroll can be
      done during ArmorClass rolls or Damage rolls against the defender who has the MirrorImage
      ability
    - Not usable on SPC damage.

Mage:

- Offensive: Spell Potency
    - If you roll doubles to pass Armor or deal damage, do dice total damage to target and half to
      all other opponents, then end turn.
- Defensive: Teleport
    - When armor is passed, roll off 1d6 with attacker.
    - If Mage rolls higher, escape the attack.

Paladin:

- Defensive: Holy Shield
    - Once per match, when K.O.'d, roll 2d6.
        - On 10, 11 or 12 Paladin survive with 1 Health, otherwise no effect and Paladin is
          KnockedOut.
            - If Paladin survives, roll 1d6 damage to attacker (no ArmorClass roll - this damage is
              unblockable and goes directly to attacker's health).
        - Also: 12 stops Duelist's Prepare Attack.
