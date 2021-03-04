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

import dev.nathanpb.reauth.oauth.AuthorizationError
import dev.nathanpb.reauth.oauth.OAuth2AuthorizeException
import dev.nathanpb.reauth.config.OAuth2Provider
import io.ktor.http.*
import kotlin.jvm.Throws

data class AuthorizeEndpointResponse(
    val code: String?,
    val error: String?,
    val state: String?
) {
    companion object {
        fun receive(params: Parameters) = AuthorizeEndpointResponse (
            params["code"],
            params["error"],
            params["state"]
        )
    }

    // https://tools.ietf.org/html/rfc6749#section-4.1.2.1
    @Throws(OAuth2AuthorizeException::class)
    fun verify(provider: OAuth2Provider) {
        if (error?.isNotEmpty() == true) {
            val authError = AuthorizationError.parse(error) ?: error("$error is not a valid oAuth2 authorization error")
            throw OAuth2AuthorizeException(provider, authError)
        }

        if (code == null || code.isBlank()) {
            error("${provider.id} did not sent nighter code or error")
        }
    }
}
