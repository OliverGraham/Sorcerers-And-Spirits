package com.linklabs.sorcerersandspirits.ui.screens.tavern.ui.preview

import com.linklabs.sorcerersandspirits.domain.traveler.TravelerStatus
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TravelerState

// TODO: Make example state that can be reused for previews

val exampleTravelerState =
    TravelerState(
        id = "-1",
        displayName = "User",
        isThisTravelerTheUser = true,
        status = TravelerStatus.Available,
    )
