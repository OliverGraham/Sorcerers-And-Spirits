package com.linklabs.sorcerersandspirits.ui.main.ui

// Deep-links to the Play Store listing by application id. The market:// scheme opens the Play app
// directly; the https fallback resolves in a browser if the Play app is unavailable.
actual fun appStoreUrl(): String = "https://play.google.com/store/apps/details?id=com.linklabs.sorcerersandspirits"
