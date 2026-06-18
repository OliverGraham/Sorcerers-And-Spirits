package com.linklabs.sorcerersandspirits.domain.authentication

/** Outcome of linking a permanent [SignInMethod] to the current account. */
enum class LinkAccountResult {
    SUCCESS,

    /** The player dismissed the provider's account picker before choosing an identity. */
    CANCELLED,

    /**
     * The chosen identity already belongs to another account, so it can't be linked to this one.
     * For email this also covers "this email already has a password".
     */
    CREDENTIAL_IN_USE,

    /** The supplied email/password was malformed or the password was too weak. */
    INVALID_CREDENTIAL,

    /** There is no current user to link to (no anonymous session was ever established). */
    NO_CURRENT_USER,

    /**
     * The provider isn't wired up on this platform yet (native SDK / Firebase console setup
     * pending). See SS-061 follow-ups.
     */
    NOT_AVAILABLE,
    ERROR,
}
