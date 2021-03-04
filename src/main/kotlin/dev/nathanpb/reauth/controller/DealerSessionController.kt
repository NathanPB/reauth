/*
 * Copyright (c) 2021 - Nathan P. Bombana
 *
 * This file is part of Reauth.
 *
 * Wheres My Duo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wheres My Duo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Wheres My Duo.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nathanpb.reauth.controller

import com.github.benmanes.caffeine.cache.Caffeine
import dev.nathanpb.reauth.data.AuthorizeEndpointParams
import dev.nathanpb.reauth.data.Client
import dev.nathanpb.reauth.oauth.client.OAuth2Dealer
import dev.nathanpb.reauth.oauth.client.OAuth2Provider
import dev.nathanpb.reauth.randomHex
import java.util.concurrent.TimeUnit

class DealerSession internal constructor(
    val id: String,
    val dealer: OAuth2Dealer,
    val client: Client,
    val initialParams: AuthorizeEndpointParams
)

object DealerSessionController {
    private val sessionPool = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String, DealerSession>()

    fun new(provider: OAuth2Provider, client: Client, params: AuthorizeEndpointParams): DealerSession {
        val sessionId = randomHex(4)
        return DealerSession(sessionId, OAuth2Dealer(provider), client, params).also {
            sessionPool.put(sessionId, it)
        }
    }

    fun find(sessionId: String): DealerSession? {
        return sessionPool.getIfPresent(sessionId)
    }

    fun finalize(sessionId: String) {
        sessionPool.invalidate(sessionId)
    }
}
