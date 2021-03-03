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

package dev.nathanpb.reauth.data

import io.ktor.http.*
import java.security.InvalidParameterException

// https://tools.ietf.org/html/rfc6749#section-4.1.3
data class TokenEndpointParams (
    val grantType: String,
    val code: String,
    val redirectUri: String,
    val clientId: String,
    val clientSecret: String? // why is it not in the specification? Guess I'm missing something
) {
    companion object {
        fun receive(params: Parameters) = TokenEndpointParams(
            params["grant_type"] ?: throw InvalidParameterException("missing grant_type"),
            params["code"] ?: throw InvalidParameterException("missing code"),
            params["redirect_uri"] ?: throw InvalidParameterException("missing redirect_uri"),
            params["client_id"] ?: throw InvalidParameterException("missing client_id"),
            params["client_secret"]
        )
    }
}
