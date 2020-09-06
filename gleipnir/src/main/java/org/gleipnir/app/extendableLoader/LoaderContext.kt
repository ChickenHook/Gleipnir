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
import android.content.pm.PackageInfo

interface LoaderContext {
    /**
     * Attach loader to host
     *
     * @param hostActivity The activity representing the host app
     * @param plugins The plugins to be used
     * @param packageInfo The target application
     */
    fun attach(
        hostActivity: Activity,
        plugins: List<IPlugin>,
        packageInfo: PackageInfo,
        profile: String
    )

    /**
     * Bind target application
     *
     * Reinit the ActivityThread using the target applicationInfo
     *
     * This can be optionally. If a plugin takes the call, we return.
     */
    fun bind()

    /**
     * Prepare the android stack for launching the target application
     *
     * Patch classloaders, resources and paths
     */
    fun prepareAndroidStack()

    /**
     * Launch Application, ContentProviders, Services and main activity
     */
    fun launch()

    /**
     * Get target package info (patched)
     */
    fun getTargetPackgeInfo(): PackageInfo

}
