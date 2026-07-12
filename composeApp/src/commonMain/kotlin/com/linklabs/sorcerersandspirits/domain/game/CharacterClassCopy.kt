package com.linklabs.sorcerersandspirits.domain.game

data class AbilityCopy(val name: String, val long: String, val short: String) {
    companion object {
        val EMPTY = AbilityCopy(name = "", long = "", short = "")
    }
}

data class CharacterClassCopy(private val entries: Map<String, AbilityCopy>) {
    operator fun get(key: String): AbilityCopy = entries[key] ?: AbilityCopy.EMPTY

    companion object {
        val EMPTY = CharacterClassCopy(entries = emptyMap())
    }
}

/**
 * Stable keys for every piece of class copy; the contract between the UI lookups and the payload.
 */
object ClassCopyKeys {
    const val HEALTH = "stat.health"
    const val ARMOR = "stat.armor"
    const val SPEED = "stat.speed"

    const val KILLING_MOMENTUM = "offensive.killingMomentum"
    const val ALL_IN = "offensive.allIn"
    const val PREPARE_ATTACK = "offensive.prepareAttack"
    const val SUMMON_MINION = "offensive.summonMinion"
    const val ENCORE = "offensive.encore"
    const val IMPERSONATE = "offensive.impersonate"
    const val SPELL_POTENCY = "offensive.spellPotency"

    const val DODGE = "defensive.dodge"
    const val LUCKY_COIN = "defensive.luckyCoin"
    const val RIPOSTE = "defensive.riposte"
    const val PERSUADE = "defensive.persuade"
    const val MIRROR_IMAGE = "defensive.mirrorImage"
    const val TELEPORT = "defensive.teleport"
    const val HOLY_SHIELD = "defensive.holyShield"

    val ALL: Set<String> =
        setOf(
            HEALTH,
            ARMOR,
            SPEED,
            KILLING_MOMENTUM,
            ALL_IN,
            PREPARE_ATTACK,
            SUMMON_MINION,
            ENCORE,
            IMPERSONATE,
            SPELL_POTENCY,
            DODGE,
            LUCKY_COIN,
            RIPOSTE,
            PERSUADE,
            MIRROR_IMAGE,
            TELEPORT,
            HOLY_SHIELD,
        )
}
