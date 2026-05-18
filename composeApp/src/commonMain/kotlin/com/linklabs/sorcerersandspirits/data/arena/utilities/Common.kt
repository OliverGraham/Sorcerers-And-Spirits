package com.linklabs.sorcerersandspirits.data.arena.utilities

import dev.gitlive.firebase.database.DatabaseReference

internal fun DatabaseReference.addNewKey(): String =
    push().key ?: error("Failed to generate Firebase push key")
