package com.linklabs.sorcerersandspirits.data.tavern

import com.google.firebase.database.DataSnapshot
import com.linklabs.sorcerersandspirits.data.model.TravelerEntity
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

internal fun DataSnapshot.toTraveler(): Traveler? {
    val travelerEntity = getValue(TravelerEntity::class.java) ?: return null
    return travelerEntity.toTraveler()
}
