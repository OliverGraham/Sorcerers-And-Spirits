package com.linklabs.sorcerersandspirits.platform

import android.app.Activity

/**
 * Holds a reference to the currently-active [Activity] so platform singletons that need to show
 * system UI (e.g. the Credential Manager account picker) can reach one. [MainActivity] populates
 * and clears it across its lifecycle, so the reference is released on destroy and never outlives
 * the Activity.
 */
class CurrentActivityHolder {
    var activity: Activity? = null
}
