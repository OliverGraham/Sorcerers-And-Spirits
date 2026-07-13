package com.linklabs.sorcerersandspirits.domain.authentication

/** Outcome of linking a permanent [SignInMethod] to the current account. */
enum class LinkAccountResult {
    SUCCESS,
    CANCELLED,
    CREDENTIAL_IN_USE,
    INVALID_CREDENTIAL,
    NO_CURRENT_USER,
    NOT_AVAILABLE,
    ERROR,
}
