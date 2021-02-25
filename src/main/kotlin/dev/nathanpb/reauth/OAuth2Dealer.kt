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

package dev.nathanpb.reauth

import io.ktor.http.*

class OAuth2Dealer(val provider: OAuth2Provider) {

    var code: String? = null

    fun buildRedirectURL(): Url {
        return URLBuilder(provider.authorizeURL).apply {
            parameters["response_type"] = "code"
            parameters["client_id"] = provider.clientId
            parameters["redirect_uri"] = URLBuilder(BASE_URL).path("receiver/${provider.id}").buildString()
            parameters["scope"] = provider.scopes.joinToString(" ")
        }.build()
    }

    fun receiveRedirect(code: String?, error: String?) {
        // https://tools.ietf.org/html/rfc6749#section-4.1.2.1
        if (error?.isNotEmpty() == true) {
            val authError = AuthorizationError.parse(error) ?: error("$error is not a valid oAuth2 authorization error")
            throw OAuth2AuthorizeException(provider, authError)
        }

        if (code == null || code.isBlank()) {
            error("${provider.id} did not sent nighter code or error")
        }

        this.code = code
    }
}
