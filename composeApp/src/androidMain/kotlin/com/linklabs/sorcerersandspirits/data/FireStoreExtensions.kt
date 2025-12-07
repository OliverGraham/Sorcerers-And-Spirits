package com.linklabs.sorcerersandspirits.data

import com.google.firebase.firestore.QuerySnapshot
import com.linklabs.sorcerersandspirits.data.model.TavernEntity
import com.linklabs.sorcerersandspirits.data.model.TravelerEntity
import com.linklabs.sorcerersandspirits.domain.tavern.Tavern

internal fun QuerySnapshot?.tavernCollectionToTavern(): Tavern? {
    if (this == null) return null

    val travelerEntities = this.mapNotNull { travelerDocument ->
        travelerDocument.toObject(TravelerEntity::class.java)
    }

    val tavernEntity = TavernEntity(travelerEntities = travelerEntities)
    return tavernEntity.toTavern()
}
