package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.game.AbilityCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import kotlinx.serialization.Serializable

/**
 * One stat/ability's copy as carried in the RemoteConfig payload. The class copy is the whole keyed
 * map; see [toCharacterClassCopy].
 */
@Serializable
data class AbilityCopyEntity(val name: String, val long: String, val short: String) {
    fun toDomain() = AbilityCopy(name = name, long = long, short = short)
}

// TODO: Why a map? Using hardcoded keys is wrong
fun Map<String, AbilityCopyEntity>.toCharacterClassCopy() =
    CharacterClassCopy(entries = mapValues { (_, entity) -> entity.toDomain() })
