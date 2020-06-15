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

package org.gleipnir.app.helpers.extensions

import android.app.job.JobInfo
import android.app.job.JobWorkItem
import org.gleipnir.app.reflectionHelper.getDeclaredMethod
import java.lang.reflect.Method

fun IJobScheuduler_enqueue(IJobScheduler: Any, p0: JobInfo, p1: JobWorkItem): Int {
    val enqueueMethod =
        getDeclaredMethod(
            IJobScheduler,
            "enqueue",
            JobInfo::class.java,
            JobWorkItem::class.java
        ) as Method
    enqueueMethod.isAccessible = true
    return enqueueMethod.invoke(IJobScheduler, p0, p1) as Int
}

fun IJobScheuduler_cancelAll(IJobScheduler: Any) {
    val enqueueMethod =
        getDeclaredMethod(
            IJobScheduler,
            "cancelAll"
        ) as Method
    enqueueMethod.isAccessible = true
    enqueueMethod.invoke(IJobScheduler)
}

fun IJobScheuduler_schedule(IJobScheduler: Any, p0: JobInfo): Int {
    val enqueueMethod =
        getDeclaredMethod(
            IJobScheduler,
            "schedule",
            JobInfo::class.java
        ) as Method
    enqueueMethod.isAccessible = true
    return enqueueMethod.invoke(IJobScheduler, p0) as Int
}

fun IJobScheuduler_getAllPendingJobs(IJobScheduler: Any): MutableList<JobInfo> {
    val enqueueMethod =
        getDeclaredMethod(
            IJobScheduler,
            "getAllPendingJobs"
        ) as Method
    enqueueMethod.isAccessible = true
    return enqueueMethod.invoke(IJobScheduler) as MutableList<JobInfo>
}

fun IJobScheuduler_getPendingJob(IJobScheduler: Any, p0: Int): JobInfo? {
    val enqueueMethod =
        getDeclaredMethod(
            IJobScheduler,
            "getAllPendingJobs",
            Integer.TYPE
        ) as Method
    enqueueMethod.isAccessible = true
    return enqueueMethod.invoke(IJobScheduler, p0) as JobInfo?
}

fun IJobScheuduler_cancel(IJobScheduler: Any, p0: Int) {
    val enqueueMethod =
        getDeclaredMethod(
            IJobScheduler,
            "cancel",
            Integer.TYPE
        ) as Method
    enqueueMethod.isAccessible = true
    enqueueMethod.invoke(IJobScheduler, p0)
}