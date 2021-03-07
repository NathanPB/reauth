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
import dev.nathanpb.reauth.oauth.model.AuthorizeEndpointRequest
import dev.nathanpb.reauth.randomHex
import java.util.concurrent.TimeUnit

object SessionNoncePool {

    private val noncePool = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String, AuthorizeEndpointRequest>()

    fun put(request: AuthorizeEndpointRequest): String {
        return randomHex(4).also { noncePool.put(it, request) }
    }

    fun retrieve(nonce: String): AuthorizeEndpointRequest? {
        return noncePool.getIfPresent(nonce)?.also {
            noncePool.invalidate(nonce)
        }
    }
}
