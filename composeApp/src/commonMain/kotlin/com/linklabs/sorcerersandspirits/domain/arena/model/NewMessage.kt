package com.linklabs.sorcerersandspirits.domain.arena.model

sealed interface OutgoingMessage {

    data class NewRound(val roundNumber: Int) : OutgoingMessage

    data class NewTurn(val characterClassInfo: CharacterClassMessageInfo) : OutgoingMessage

    data class NormalRoll(
        val attackerUserId: String,
        val defenderUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val type: NormalRollMessageType,
    ) : OutgoingMessage

    data class Dodge(val didArmorIncrease: Boolean) : OutgoingMessage {
        // if didArmorIncrease, "Rogue dodged the attack and increased their AC by 1!"
        // else, "Rogue already has the maximum Dodge bonus"
    }
}

enum class NormalRollMessageType {
    // "A targeted D, rolled R and hit/missed"
    ArmorClassHit,
    ArmorClassMiss,

    // "A damaged D for R (if KO) and knocked out D!"
    Damage,
    Knockout,
}
