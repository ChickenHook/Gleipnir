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
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.GridView
import android.widget.TextView
import org.gleipnir.app.extendableLoader.IPlugin
import org.gleipnir.app.plugins.FridaPlugin
import org.gleipnir.app.plugins.Input2Toast
import org.gleipnir.app.plugins.mPowerPlugin
import java.util.concurrent.CopyOnWriteArrayList

/**
 * This activity manages the currently enabled Plug-Ins
 *
 * The static enabledPlugins represents the currently enabled Plug-Ins to be enabled during launch
 * of a victim App.
 *
 * TODO give enabled Plug-Ins via onActivityResult
 * TODO save the enable / disable state
 */
class Plugtivity : Activity() {

    companion object {
        /**
         * Add Plug-In here
         */
        val pluginsList = arrayListOf<() -> IPlugin>(
            ::FridaPlugin,
            ::Input2Toast,
            ::mPowerPlugin
        )

        val enabledPlugins = CopyOnWriteArrayList<IPlugin>()
    }

    var pluginsGrid: GridView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.grid_layout)
        initList()
    }

    /**
     * Init the visible Plug-In list
     */
    fun initList() {
        pluginsGrid = findViewById(R.id.gridview)
        pluginsGrid?.numColumns = 2
        val plugins = getPluginList()


        pluginsGrid?.let {
            val adapter: ArrayAdapter<() -> IPlugin> =
                object : ArrayAdapter<() -> IPlugin>(this, R.layout.plugin_item, plugins) {
                    override fun getView(
                        position: Int,
                        cv: View?,
                        parent: ViewGroup
                    ): View {
                        var convertView = cv
                        var viewHolder: ViewHolderItem?

                        if (convertView == null) {
                            convertView = layoutInflater.inflate(
                                R.layout.plugin_item, parent, false
                            )
                            viewHolder = ViewHolderItem()
                            viewHolder.checkbox =
                                convertView!!.findViewById(R.id.plugin_checkbox) as CheckBox
                            viewHolder.name = convertView.findViewById(R.id.plugin_name) as TextView
                            viewHolder.label =
                                convertView.findViewById(R.id.plugin_label) as TextView
                            convertView.tag = viewHolder
                        } else {
                            viewHolder = convertView.tag as ViewHolderItem
                        }

                        val plugin = plugins[position]()
                        viewHolder!!.checkbox?.isChecked = false
                        viewHolder!!.checkbox?.setOnCheckedChangeListener { view, state ->
                            if (view.isChecked) {
                                enabledPlugins.add(plugin)
                            } else {
                                enabledPlugins.remove(plugin)
                            }
                        }
                        viewHolder.label?.setText(plugin.description())
                        viewHolder.name?.setText("")
                        return convertView
                    }
                }
            it.adapter = adapter

        }
    }

    class ViewHolderItem {
        var checkbox: CheckBox? = null
        var label: TextView? = null
        var name: TextView? = null
    }

    fun getPluginList(): List<() -> IPlugin> {
        return pluginsList
    }
}