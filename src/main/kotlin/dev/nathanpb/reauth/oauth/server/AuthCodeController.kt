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
import dev.nathanpb.reauth.resource.Client
import dev.nathanpb.reauth.utils.randomHex
import java.util.concurrent.TimeUnit

// TODO expire a code if requested two times
// https://tools.ietf.org/html/rfc6749#section-4.1.2
object AuthCodeController {

    private val codes = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, ReauthAccessToken>()

    fun putTokenInThePool(token: ReauthAccessToken): String {
        val code = randomHex(4)
        codes.put(code, token)
        return code
    }

    suspend fun exchangeCode(code: String, clientSecret: String): ReauthAccessToken? {
        val token = codes.getIfPresent(code) ?: return null
        val client = Client.collection.findOneById(token.clientId) ?: return null

        if (client.clientSecret == clientSecret) {
            codes.invalidate(code)
            return token
        }

        return null
    }
}
