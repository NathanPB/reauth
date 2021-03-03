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

package dev.nathanpb.reauth.oauth

import com.auth0.jwt.JWT
import dev.nathanpb.reauth.ISSUER
import dev.nathanpb.reauth.hmac256
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.concurrent.TimeUnit

// https://tools.ietf.org/html/rfc6749#section-4.2.2
@Serializable
data class OAuth2Token(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null, // TODO refresh the tokens when they are about to expire
    val scope: String? = null,
    val state: String? = null,
    val createdAt: Long = Instant.now().epochSecond
) {

    companion object {
        fun newBearerToken(uid: String, clientId: String, scopes: List<String>) = OAuth2Token(
            JWT.create()
                .withIssuer(ISSUER)
                .withClaim("uid", uid)
                .withClaim("client_id", clientId)
                .withArrayClaim("scope", scopes.toTypedArray())
                .sign(hmac256),
            "Bearer",
            TimeUnit.DAYS.toSeconds(12),
            scope = scopes.joinToString(" ")
        )
    }

    fun isExpired() : Boolean {
        return if (expiresIn == null) {
            false // TODO validate with the token issuer
        } else {
            Instant.now().epochSecond >= createdAt + expiresIn
        }
    }
}
