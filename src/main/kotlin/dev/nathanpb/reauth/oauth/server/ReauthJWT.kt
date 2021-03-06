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

package dev.nathanpb.reauth.oauth.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.nathanpb.reauth.config.ISSUER
import dev.nathanpb.reauth.config.RSA_KEYPAIR
import io.ktor.util.*

private val hmac256 = Algorithm.HMAC256(hex(RSA_KEYPAIR.private.encoded))

data class ReauthJWT (
    val clientId: String,
    val uid: String,
    val scopes: Set<String>
) {
    companion object {
        fun fromToken(token: String): ReauthJWT {
            return JWT.require(hmac256)
                .withIssuer(ISSUER)
                .build()
                .verify(token).run {
                    ReauthJWT(
                        getClaim("client_id").asString(),
                        getClaim("uid").asString(),
                        getClaim("scope").asArray(String::class.java).toSet()
                    )
                }
        }
    }

    val jwtString by lazy {
        JWT.create()
            .withIssuer(ISSUER)
            .withClaim("uid", uid)
            .withClaim("client_id", clientId)
            .withArrayClaim("scope", scopes.toTypedArray())
            .sign(hmac256)
    }
}
