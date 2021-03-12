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

package dev.nathanpb.reauth.oauth.model

import dev.nathanpb.reauth.utils.epochSeconds
import dev.nathanpb.reauth.oauth.server.ReauthAccessToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

// https://tools.ietf.org/html/rfc6749#section-4.1.4
@Serializable
data class TokenEndpointResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String? = null,
    val state: String? = null,
) {
    constructor(token: ReauthAccessToken, state: String? = null): this(
        accessToken = token.jwtString,
        tokenType = "Bearer",
        expiresIn = token.expiresAt.epochSeconds - Instant.now().epochSecond,
        scope = token.scopes.map(String::toLowerCase).toSet().joinToString(" "),
        state = state
    )
}
