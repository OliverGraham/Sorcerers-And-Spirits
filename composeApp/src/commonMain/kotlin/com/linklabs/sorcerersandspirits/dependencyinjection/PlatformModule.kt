package com.linklabs.sorcerersandspirits.dependencyinjection

import org.koin.core.module.Module

/** Platform-specific dependencies (e.g. connectivity) wired into [commonModule]. */
expect fun platformModule(): Module
