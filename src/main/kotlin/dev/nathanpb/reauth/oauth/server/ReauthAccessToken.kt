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

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.nathanpb.reauth.config.ISSUER
import dev.nathanpb.reauth.config.RSA_KEYPAIR
import dev.nathanpb.reauth.utils.randomHex
import dev.nathanpb.reauth.utils.toDate
import dev.nathanpb.reauth.utils.toLocalDateTime
import java.time.LocalDateTime

data class ReauthAccessToken (
    val tokenId: String = randomHex(8),
    val clientId: String,
    val uid: String,
    val scopes: Set<String>,
    val expiresAt: LocalDateTime,
) {
    companion object {
        fun fromToken(token: String): ReauthAccessToken {
            return JWT.require(Algorithm.RSA256(RSA_KEYPAIR.public, RSA_KEYPAIR.private))
                .withIssuer(ISSUER)
                .withSubject("access_token")
                .withClaimPresence("client_id")
                .withClaimPresence("uid")
                .withClaimPresence("scope")
                .build()
                .verify(token).let { jwt ->
                    ReauthAccessToken(
                        jwt.id,
                        jwt.getClaim("client_id").asString(),
                        jwt.getClaim("uid").asString(),
                        jwt.getClaim("scope").asArray(String::class.java).toSet(),
                        jwt.expiresAt.toLocalDateTime()
                    )
                }
        }
    }

    val jwtString: String by lazy {
        JWT.create()
            .withIssuer(ISSUER)
            .withSubject("access_token")
            .withJWTId(tokenId)
            .withExpiresAt(expiresAt.toDate())
            .withClaim("uid", uid)
            .withClaim("client_id", clientId)
            .withArrayClaim("scope", scopes.toTypedArray())
            .sign(Algorithm.RSA256(RSA_KEYPAIR.public, RSA_KEYPAIR.private))
    }
}
