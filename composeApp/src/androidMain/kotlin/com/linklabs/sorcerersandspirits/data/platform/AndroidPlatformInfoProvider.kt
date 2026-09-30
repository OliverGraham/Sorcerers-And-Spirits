package com.linklabs.sorcerersandspirits.data.platform

import com.linklabs.sorcerersandspirits.domain.platform.PlatformInfoProvider

class AndroidPlatformInfoProvider : PlatformInfoProvider {
    override fun platformName(): String = "Android"
}
