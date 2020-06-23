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
import java.util.zip.ZipOutputStream


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


    fun zipIt(zipFile: File, node: File) {
        val fileList = generateFileList(node)
        val buffer = ByteArray(1024)
        val source: String = node.getName()
        var fos: FileOutputStream? = null
        var zos: ZipOutputStream? = null
        try {
            fos = FileOutputStream(zipFile)
            zos = ZipOutputStream(fos)
            println("Output to Zip : $zipFile")
            var `in`: FileInputStream? = null
            for (file in fileList) {
                println("File Added : $file")
                val ze =
                    ZipEntry(file)
                zos.putNextEntry(ze)
                try {
                    `in` = FileInputStream(node.absolutePath + File.separator.toString() + file)
                    var len: Int
                    while (`in`!!.read(buffer).also { len = it } > 0) {
                        zos.write(buffer, 0, len)
                    }
                } finally {
                    `in`!!.close()
                }
            }
            zos.closeEntry()
            println("Folder successfully compressed")
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            try {
                zos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    fun generateFileList(root: File, node: File = root): List<String> {
        val fileList = ArrayList<String>()
        // add file only
        if (node.isFile) {
            generateZipEntry(root, node.absolutePath)?.let {
                fileList.add(it)
            }
        }
        if (node.isDirectory) {
            val subNote = node.list()
            for (filename in subNote) {
                var list = generateFileList(root, File(node, filename))
                fileList.addAll(list)
            }
        }
        return fileList
    }

    private fun generateZipEntry(node: File, file: String): String? {
        return file.substring(node.absolutePath.length + 1, file.length)
    }


    fun unzip(source: File?, out: File?) {
        FileInputStream(source).use { input ->
            unzip(input, out)
        }
    }

    fun unzip(source: InputStream?, out: File?) {
        ZipInputStream(source).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(out, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    val parent = file.parentFile
                    if (!parent.exists()) {
                        parent.mkdirs()
                    }
                    BufferedOutputStream(FileOutputStream(file)).use { bos ->
                        val bufferSize = Math.toIntExact(entry!!.size)
                        val buffer =
                            ByteArray(if (bufferSize > 0) bufferSize else 1)
                        var location: Int
                        while (zis.read(buffer).also { location = it } != -1) {
                            bos.write(buffer, 0, location)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}