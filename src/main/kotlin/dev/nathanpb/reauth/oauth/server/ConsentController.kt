/*
 * Copyright (c) 2021 - Nathan P. Bombana
 *
 * This file is part of Reauth.
 *
 * ReAuth is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReAuth is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReAuth.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nathanpb.reauth.oauth.server

import com.github.benmanes.caffeine.cache.Caffeine
import dev.nathanpb.reauth.oauth.client.DealerSession
import dev.nathanpb.reauth.utils.randomHex
import java.util.concurrent.TimeUnit

private val requireConsentPool = Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .build<String, Pair<String, DealerSession>>()

object ConsentController {

    fun createConsentWaiter(uid: String, session: DealerSession): String {
        return randomHex(4).also {
            requireConsentPool.put(it, uid to session)
        }
    }

    fun receiveConsent(nonce: String): Pair<String, DealerSession>? {
        return requireConsentPool.getIfPresent(nonce)?.also {
            requireConsentPool.invalidate(nonce)
        }
    }

}
