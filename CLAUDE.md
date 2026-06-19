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

- "Match" Entire Arena battle from start to end. Players can join an arena and play for as many
  matches as they want
- "Round" A round consists of every player acting
- "Turn" A player attacking or using their ability
- "Activated Ability" an ability that is used instead of attacking (Impersonate and Allin are an
  exceptions and don't use up an attack).

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
