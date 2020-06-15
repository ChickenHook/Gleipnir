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

package org.gleipnir.app.helpers

import java.io.*

// native libs


fun copyAllFiles(srcDir: File, destDir: File) {
    srcDir.listFiles()?.forEach {
        val src = it
        val dest = File(destDir.absolutePath + File.separator + it.name)
        copy(src, dest)
    }
}

fun copy(src: File, dest: File): Unit {
    var `is`: InputStream? = null
    var os: OutputStream? = null
    try {
        `is` = FileInputStream(src)
        os = FileOutputStream(dest)
        // buffer size 1K
        val buf = ByteArray(2048)
        var bytesRead: Int = 0
        while (`is`.read(buf).also({ bytesRead = it }) > 0) {
            os.write(buf, 0, bytesRead)
        }
    } finally {
        `is`?.close()
        os?.close()
    }
}