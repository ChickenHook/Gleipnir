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

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer

object GleipnirLog {
    val logCallback: (String) -> Unit = {
        Log.d("LogExtension", it)
    }
}

fun log(message: String) {
    GleipnirLog.logCallback(message)
}

fun log(message: String, throwable: Throwable) {
    val writer: Writer = StringWriter()
    throwable.printStackTrace(PrintWriter(writer))
    val s: String = writer.toString()
    log("$message\n$s")
}