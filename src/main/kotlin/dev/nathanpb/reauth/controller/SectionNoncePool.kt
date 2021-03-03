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
import dev.nathanpb.reauth.randomHex
import java.util.concurrent.TimeUnit

object SectionNoncePool {

    private val noncePool = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String, AuthorizeEndpointParams>()

    fun put(request: AuthorizeEndpointParams): String {
        return randomHex(4).also { noncePool.put(it, request) }
    }

    fun retrieve(nonce: String): AuthorizeEndpointParams? {
        return noncePool.getIfPresent(nonce)?.also {
            noncePool.invalidate(nonce)
        }
    }
}
