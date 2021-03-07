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

package dev.nathanpb.reauth.oauth.client

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import dev.nathanpb.reauth.config.BASE_URL
import dev.nathanpb.reauth.config.OAuth2Provider
import dev.nathanpb.reauth.oauth.model.AuthorizeEndpointResponse
import dev.nathanpb.reauth.oauth.model.TokenEndpointResponse
import io.ktor.http.*
import org.bson.Document

class OAuth2ClientDealer(val provider: OAuth2Provider) {

    private var code: String? = null
    private var token: TokenEndpointResponse? = null
    private var userData: Map<String, Any>? = null

    fun receiveRedirect(params: AuthorizeEndpointResponse) {
        params.verify(provider)
        this.code = params.code
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
                "redirect_uri" to URLBuilder(BASE_URL).path("providers/${provider.id}/callback").buildString()
            )
        ).awaitObjectResult<TokenEndpointResponse>(kotlinxDeserializerOf())
            .get()

    }

    private suspend fun getAccessToken(): TokenEndpointResponse {
        // TODO check if the token is expired. Maybe attempt to refresh?
        return token ?: kotlin.run {
            exchangeToken()
            token ?: error("Unexpected state: Token somehow was not set")
        }
    }

    suspend fun getUserData(): Map<String, Any> {
        return userData ?: kotlin.run {
            val token = getAccessToken()
            Fuel.get(provider.userDataURL)
                .header("Authorization", "${token.tokenType} ${token.accessToken}")
                .awaitStringResponse()
                .let { Document.parse(it.third).toMap() }
        }
    }
}
