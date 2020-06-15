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

import android.content.res.Resources
import java.lang.reflect.Method

fun Resources.setImpl(resourcesImpl: Any) {
    val setImpl: Method =
        Resources::class.java.getMethod(
            "setImpl",
            Class.forName("android.content.res.ResourcesImpl")
        )
    setImpl.invoke(this, resourcesImpl)

}

fun createResources(classLoader: ClassLoader, resourcesImpl: Any): Resources {

    val dynamicResources =
        Resources::class.java.getConstructor(ClassLoader::class.java)
            .newInstance(classLoader)
    dynamicResources.setImpl(resourcesImpl)
    return dynamicResources
}