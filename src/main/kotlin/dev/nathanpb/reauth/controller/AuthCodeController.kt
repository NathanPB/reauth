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
import dev.nathanpb.reauth.CLIENT_ID
import dev.nathanpb.reauth.CLIENT_SECRET
import dev.nathanpb.reauth.data.TokenCodeMapper
import dev.nathanpb.reauth.oauth.OAuth2Token
import dev.nathanpb.reauth.verifyJwt
import java.util.concurrent.TimeUnit

// TODO expire a code if requested two times
// https://tools.ietf.org/html/rfc6749#section-4.1.2
object AuthCodeController {

    private val codes = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, TokenCodeMapper>()

    fun putTokenInThePool(token: OAuth2Token): String {
        return TokenCodeMapper(token).apply {
            codes.put(code, this)
        }.code
    }

    fun exchangeCode(code: String, clientSecret: String): OAuth2Token? {
        if (clientSecret == CLIENT_SECRET) {
            val token = codes.getIfPresent(code) ?: return null
            if (verifyJwt(token.token.accessToken, null, CLIENT_ID)) {
                codes.invalidate(code)
                return token.token
            }
        }

        return null
    }
}
