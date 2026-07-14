package com.linklabs.sorcerersandspirits.data.audio

import platform.Foundation.NSBundle

internal fun bundlePathForAudioAsset(assetFileName: String): String? {
    val resourceName = assetFileName.substringBeforeLast(delimiter = '.')
    val resourceExtension = assetFileName.substringAfterLast(delimiter = '.')
    return NSBundle.mainBundle.pathForResource(name = resourceName, ofType = resourceExtension)
}
