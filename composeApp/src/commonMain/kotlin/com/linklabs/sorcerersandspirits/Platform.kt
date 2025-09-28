package com.linklabs.sorcerersandspirits

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform