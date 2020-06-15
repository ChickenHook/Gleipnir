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

interface ILoader {
    /**
     * Load and start the given App represented by the PackageInfo
     *
     * @param hostActivity The host activity (a Gleipnir Activity)
     * @param plugins list of enabled Plug-Ins
     * @param targetApplication the PackageInfo of the target Application
     */
    fun loadAndStart(
        hostActivity: Activity,
        plugins: List<IPlugin>,
        targetApplication: PackageInfo
    ): Boolean

    /**
     * Load and start the given App represented by the PackageInfo
     *
     * @param hostActivity The host activity (a Gleipnir Activity)
     * @param plugins list of enabled Plug-Ins
     * @param targetApplication the PackageInfo of the target Application
     * @param onFinish callback to be triggered when start process has finished
     */
    fun loadAndStartAsync(
        hostActivity: Activity,
        plugins: List<IPlugin>,
        targetApplication: PackageInfo,
        onFinish: () -> Unit
    ): Boolean

    /**
     * The loader context representing all necessary elements of the Attack
     */
    fun getLoaderContext(): LoaderContext
}