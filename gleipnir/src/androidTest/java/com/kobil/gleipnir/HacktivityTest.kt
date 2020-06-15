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

import android.os.SystemClock
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class HacktivityTest {


    @get:Rule
    var activityRule: ActivityTestRule<Hacktivity>
            = ActivityTestRule(Hacktivity::class.java)


    @Test
    fun runMPower(){
        onData(hasToString(endsWith("org.gleipnir.mpower.app"))).perform(scrollTo(),click());
        onView(withId(R.id.application_start)).perform(click())
        SystemClock.sleep(1200*1000)
    }
    @Test
    fun runHardening(){
        onData(hasToString(endsWith("org.gleipnir.hardening"))).perform(scrollTo(),click());
        onView(withId(R.id.application_start)).perform(click())
        SystemClock.sleep(1200*1000)
    }

    @Test
    fun runMPowerTse03(){
        onData(hasToString(startsWith("org.gleipnir.mpower.app.tse03_labnet"))).perform(scrollTo(),click());
        onView(withId(R.id.application_start)).perform(click())
        SystemClock.sleep(1200*1000)
    }


}