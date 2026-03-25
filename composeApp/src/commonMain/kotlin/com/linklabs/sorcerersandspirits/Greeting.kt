package com.linklabs.sorcerersandspirits

// TODO: Remove
class Greeting {
    private val platform = getPlatform()

    fun greet(): String = "Hello, ${platform.name}!"
}
