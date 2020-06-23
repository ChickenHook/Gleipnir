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

import android.content.Context
import java.io.File

class ApplicationPaths {
    var nativePath: File = File("")
    var apkPath: File = File("")
    var dataDir: File = File("")
    var protectedDir: File = File("")
    var resourceApks = ArrayList<String>()

    companion object {
        fun buildDataDir(context: Context, packageName: String): File {
            return File(context.filesDir.absolutePath + "/" + packageName)
        }
    }
}