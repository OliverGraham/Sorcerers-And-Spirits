package com.linklabs.sorcerersandspirits.domain.authentication

/**
 * A permanent identity provider a player can optionally link to their (anonymous) account so their
 * profile and stats survive a reinstall or move to a new device. Anonymous auth is intentionally
 * not represented here: it is the always-on default and is never presented to the player.
 */
enum class SignInMethod {
    GOOGLE,
    APPLE,
    EMAIL,
}
