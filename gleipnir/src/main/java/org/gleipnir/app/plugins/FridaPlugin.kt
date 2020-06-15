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

package org.gleipnir.app.plugins

import android.app.Activity
import android.app.Application
import android.util.Log
import org.gleipnir.app.extendableLoader.ApplicationPaths
import org.gleipnir.app.extendableLoader.IPlugin
import org.gleipnir.app.extendableLoader.LoaderContext

class FridaPlugin : IPlugin {
    override fun description(): String {
        return "FridaPlugin"
    }

    override fun onBindApp(loaderContext: LoaderContext): Boolean {
        loadFrida()
        return false
    }

    override fun onPrepareClassLoader(loaderContext: LoaderContext, classLoader: ClassLoader) {

    }

    override fun onCreateApplication(loaderContext: LoaderContext, application: Application) {

    }

    override fun onLaunchMainActivity(loaderContext: LoaderContext) {

    }

    override fun onNewActivity(loaderContext: LoaderContext, activity: Activity) {

    }

    override fun onPreCreateActivity(loaderContext: LoaderContext, activity: Activity) {

    }

    override fun onPostCreateActivity(loaderContext: LoaderContext, activity: Activity) {

    }

    override fun onError(loaderContext: LoaderContext, exception: Exception) {

    }

    override fun onCreatePaths(loaderContext: LoaderContext, paths: ApplicationPaths): Boolean {
        return false
    }

    fun loadFrida() {
            Log.d("FridaPlugin", "Load Frida library")
            System.loadLibrary("frida-gadget")
    }
}