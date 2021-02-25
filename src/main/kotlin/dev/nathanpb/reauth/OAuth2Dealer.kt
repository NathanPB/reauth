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

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import io.ktor.http.*

class OAuth2Dealer(val provider: OAuth2Provider) {

    private var code: String? = null
    private var token: OAuth2Token? = null

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

    private suspend fun exchangeToken() {
        if (code == null) {
            error("Dealer does not have an exchange code")
        }

        // https://tools.ietf.org/html/rfc6749#section-4.1.3
        token = Fuel.post(
            provider.tokenURL,
            listOf(
                "client_id" to provider.clientId,
                "client_secret" to provider.clientSecret,
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to URLBuilder(BASE_URL).path("receiver/${provider.id}").buildString()
            )
        ).awaitObjectResult<OAuth2Token>(kotlinxDeserializerOf())
            .get()

    }

    suspend fun getAccessToken(): OAuth2Token {
        if (token?.isExpired() != false) {
            exchangeToken()
        }

        return token ?: error("Unexpected state: Token somehow was not set")
    }
}
