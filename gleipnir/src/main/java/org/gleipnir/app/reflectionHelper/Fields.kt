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

package org.gleipnir.app.reflectionHelper

import org.gleipnir.app.extendableLoader.log
import java.lang.reflect.Field
/*
 * Some reflection helpers
 */

fun <T> getReflective(obj: Any, name: String): T? {
    return try {
        val field = getDeclaredField(obj::class.java, name) as Field
        field.isAccessible = true
        field.get(obj) as T
    } catch (exception: Exception) {
        log("Exception while performing reflection ${obj}", exception)
        null
    }
}

fun <T> getReflective(obj: Any?, receiverType: Class<*>, name: String): T? {
    return try {
        val field = getDeclaredField(receiverType, name) as Field
        field.isAccessible = true
        field.get(obj) as T
    } catch (exception: Exception) {
        log("Exception while performing reflection ${obj}", exception)
        null
    }
}

fun setReflective(obj: Any, name: String, newValue: Any?) {
    val field = getDeclaredField(obj::class.java, name) as Field
    field.isAccessible = true
    field.set(obj, newValue)
}

fun <T> setReflective(obj: Any?, receiverType: Class<T>, name: String, newValue: Any?) {
    val field = getDeclaredField(receiverType, name) as Field
    field.isAccessible = true
    field.set(obj, newValue)
}

