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

import org.gleipnir.app.extendableLoader.log
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


object ApkZipHelper {


    /**
     * Extract all native libraries of the given apk file
     */
    fun unpackLibraries(targetPath: String, apkPath: String): Boolean {
        val `is`: InputStream
        val zis: ZipInputStream
        try {
            var filename: String
            `is` = FileInputStream(apkPath)
            zis = ZipInputStream(BufferedInputStream(`is`))
            lateinit var ze: ZipEntry
            val buffer = ByteArray(1024)
            while (zis.nextEntry?.also { ze = it } != null) {
                filename = ze.getName()
                // Need to create directories if not exists, or
// it will generate an Exception...
                /*if (ze.isDirectory()) {
                    val fmd = File(path + filename)
                    fmd.mkdirs()
                    continue
                }*/

                val targetFile = File(filename).name

                if (filename.endsWith(".so")) {
                    log("ApkZipHelper [-] unpackLibraries [-] unpack <$targetFile>")
                    val fout = FileOutputStream(targetPath + File.separator + targetFile)
                    var count = 0
                    while (zis.read(buffer).also({ count = it }) != -1) {
                        fout.write(buffer, 0, count)
                    }
                    fout.close()
                }

                zis.closeEntry()
            }
            zis.close()
        } catch (e: IOException) {
            log("ApkZipHelper [-] unpackLibraries [-] error", e)
            return false
        }
        return true
    }

}