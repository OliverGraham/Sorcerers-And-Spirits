package com.linklabs.sorcerersandspirits.data.platform

import com.linklabs.sorcerersandspirits.domain.platform.PlatformInfoProvider

class IosPlatformInfoProvider : PlatformInfoProvider {
    override fun platformName(): String = "iOS"
}
