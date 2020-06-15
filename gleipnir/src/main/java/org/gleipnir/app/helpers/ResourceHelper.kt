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

import android.content.res.*
import android.graphics.Movie
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import org.gleipnir.app.extendableLoader.log
import java.io.InputStream


class ResourceHelper(
    assetManager: AssetManager,
    metrics: DisplayMetrics,
    configuration: Configuration,
    val originalResources: Resources,
    val targetPackageName: String,
    val hostPackageName: String
) : Resources(assetManager, metrics, configuration) {


    override fun getTextArray(id: Int): Array<CharSequence> {
        return originalResources.getTextArray(id)
    }

    override fun obtainTypedArray(id: Int): TypedArray {
        return originalResources.obtainTypedArray(id)
    }

    override fun getAnimation(id: Int): XmlResourceParser {
        return originalResources.getAnimation(id)
    }

    override fun getText(id: Int): CharSequence {
        return originalResources.getText(id)
    }

    override fun getText(id: Int, def: CharSequence?): CharSequence {
        return originalResources.getText(id, def)
    }

    override fun getDisplayMetrics(): DisplayMetrics {
        return originalResources.getDisplayMetrics()
    }

    override fun getDrawableForDensity(id: Int, density: Int): Drawable? {
        return originalResources.getDrawableForDensity(id, density)
    }

    override fun getDrawableForDensity(id: Int, density: Int, theme: Theme?): Drawable? {
        return originalResources.getDrawableForDensity(id, density, theme)
    }

    /*override fun getFloat(id: Int): Float {
        return originalResources.getFloat(id)
    }*/

    override fun getConfiguration(): Configuration {
        return originalResources.getConfiguration()
    }

    override fun obtainAttributes(set: AttributeSet?, attrs: IntArray?): TypedArray {
        return originalResources.obtainAttributes(set, attrs)
    }

    override fun getDimensionPixelSize(id: Int): Int {
        try {
            return originalResources.getDimensionPixelSize(id)
        } catch (exception: Exception) {
            log("ResourceHelper [-] error while getDimenstionPixelSize", exception)
            return 0;
        }
    }

    override fun getIntArray(id: Int): IntArray {
        return originalResources.getIntArray(id)
    }

    override fun getValue(id: Int, outValue: TypedValue?, resolveRefs: Boolean) {
        originalResources.getValue(id, outValue, resolveRefs)
    }

    override fun getValue(name: String?, outValue: TypedValue?, resolveRefs: Boolean) {
        originalResources.getValue(name, outValue, resolveRefs)
    }

    override fun getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any?): String {
        return originalResources.getQuantityString(id, quantity, *formatArgs)
    }

    override fun getQuantityString(id: Int, quantity: Int): String {
        return originalResources.getQuantityString(id, quantity)
    }

    override fun getResourcePackageName(resid: Int): String {
        try {
            return originalResources.getResourcePackageName(resid)
        } catch (exception: java.lang.Exception) {
            log("ResourceHelper [-] error while retrieve target package name", exception)
            return targetPackageName
        }
    }

    override fun getStringArray(id: Int): Array<String> {
        return originalResources.getStringArray(id)
    }

    override fun openRawResourceFd(id: Int): AssetFileDescriptor {
        return originalResources.openRawResourceFd(id)
    }

    override fun getDimension(id: Int): Float {
        return originalResources.getDimension(id)
    }

    override fun getColorStateList(id: Int): ColorStateList {
        return originalResources.getColorStateList(id)
    }

    override fun getColorStateList(id: Int, theme: Theme?): ColorStateList {
        return originalResources.getColorStateList(id, theme)
    }

    override fun getBoolean(id: Int): Boolean {
        return originalResources.getBoolean(id)
    }

    override fun getIdentifier(name: String?, defType: String?, defPackage: String?): Int {
        Log.d(
            "ResourceHelper",
            "ResourceHelper [-] received getIdentifier call $name for package $defPackage $hostPackageName $targetPackageName"
        )
        var identifier = if (defPackage == hostPackageName) {
            originalResources.getIdentifier(name, defType, targetPackageName)
        } else {
            originalResources.getIdentifier(name, defType, defPackage)
        }
        Log.d(
            "ResourceHelper",
            "ResourceHelper [-] got identifier $identifier"
        )
        return identifier
    }

    override fun getQuantityText(id: Int, quantity: Int): CharSequence {
        return originalResources.getQuantityText(id, quantity)
    }

    override fun getColor(id: Int): Int {
        return originalResources.getColor(id)
    }

    override fun getColor(id: Int, theme: Theme?): Int {
        return originalResources.getColor(id, theme)
    }

    override fun openRawResource(id: Int): InputStream {
        return originalResources.openRawResource(id)
    }

    override fun openRawResource(id: Int, value: TypedValue?): InputStream {
        return originalResources.openRawResource(id, value)
    }

    override fun getMovie(id: Int): Movie {
        return originalResources.getMovie(id)
    }

    override fun getInteger(id: Int): Int {
        try {
            return originalResources.getInteger(id)
        } catch (exception: Exception) {
            log("ResourceHelper [-] error while call getInteger", exception)
            return 0;
        }
    }

    override fun parseBundleExtras(parser: XmlResourceParser?, outBundle: Bundle?) {
        originalResources.parseBundleExtras(parser, outBundle)
    }

    override fun getDrawable(id: Int): Drawable {
        return originalResources.getDrawable(id)
    }

    override fun getDrawable(id: Int, theme: Theme?): Drawable {
        return originalResources.getDrawable(id, theme)
    }

    override fun getResourceTypeName(resid: Int): String {
        return originalResources.getResourceTypeName(resid)
    }

    override fun getLayout(id: Int): XmlResourceParser {
        return originalResources.getLayout(id)
    }

    override fun getFont(id: Int): Typeface {
        return originalResources.getFont(id)
    }

    override fun updateConfiguration(config: Configuration?, metrics: DisplayMetrics?) {
        originalResources.updateConfiguration(config, metrics)
    }

    override fun getXml(id: Int): XmlResourceParser {
        return originalResources.getXml(id)
    }

    override fun getString(id: Int): String {
        return originalResources.getString(id)
    }

    override fun getString(id: Int, vararg formatArgs: Any?): String {
        return originalResources.getString(id, *formatArgs)
    }

    override fun getResourceName(resid: Int): String {
        return originalResources.getResourceName(resid)
    }

    override fun parseBundleExtra(tagName: String?, attrs: AttributeSet?, outBundle: Bundle?) {
        originalResources.parseBundleExtra(tagName, attrs, outBundle)
    }

    override fun getDimensionPixelOffset(id: Int): Int {
        try {
            return originalResources.getDimensionPixelOffset(id)
        } catch (exception: Exception) {
            log("ResourceHelper [-] error while getDimensionPixelOffset", exception)
            return 0;
        }
    }

    override fun getValueForDensity(
        id: Int,
        density: Int,
        outValue: TypedValue?,
        resolveRefs: Boolean
    ) {
        originalResources.getValueForDensity(id, density, outValue, resolveRefs)
    }

    override fun getResourceEntryName(resid: Int): String {
        return originalResources.getResourceEntryName(resid)
    }

    override fun getFraction(id: Int, base: Int, pbase: Int): Float {
        return originalResources.getFraction(id, base, pbase)
    }
}