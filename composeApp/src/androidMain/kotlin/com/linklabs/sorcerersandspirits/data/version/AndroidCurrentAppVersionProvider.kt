package com.linklabs.sorcerersandspirits.data.version

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.linklabs.sorcerersandspirits.domain.version.CurrentAppVersionProvider

class AndroidCurrentAppVersionProvider(private val context: Context) : CurrentAppVersionProvider {

    override fun versionCode(): Long {
        val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }
}
