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

import dev.nathanpb.reauth.config.APP_AUTHORIZE_URL
import dev.nathanpb.reauth.config.SCOPES
import dev.nathanpb.reauth.oauth.model.AuthorizeEndpointRequest
import dev.nathanpb.reauth.oauth.model.TokenEndpointRequest
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import kotlinx.serialization.SerializationException
import java.security.InvalidParameterException

object OAuth2ServerRouteHandler {

    suspend fun handleAuthorize(call: ApplicationCall) {
        val params = try {
            AuthorizeEndpointRequest.receive(call.request.queryParameters).apply {
                if (responseType != "code") {
                    return call.respond(HttpStatusCode.NotImplemented, "\"${responseType}\" is invalid or not implemented")
                }

                if (!clientExists()) {
                    return call.respond(HttpStatusCode.NotFound, "client not found")
                }

                val scopes = scope?.split(" ").orEmpty().map(String::toLowerCase).toSet()
                if (scopes.isEmpty() || scopes.any { it !in SCOPES }) {
                    return call.respond(HttpStatusCode.BadRequest, "invalid or missing \"scope\"")
                }

                if (redirectUri !in client.redirectUris) {
                    return call.respond(HttpStatusCode.NotAcceptable, "\"redirect_uri\" is not set or do not conform with the pre-defined URIs")
                }
            }

        } catch (e: InvalidParameterException) {
            return call.respond(HttpStatusCode.BadRequest, e.message.orEmpty())
        }

        call.respondRedirect(
            URLBuilder(APP_AUTHORIZE_URL).apply {
                parameters["nonce"] = SessionNoncePool.put(params)
            }.buildString(),
            false
        )
    }

    suspend fun handleToken(call: ApplicationCall) {
        val params = try {
            call.receive<TokenEndpointRequest>().apply {
                if (grantType != "authorization_code") {
                    return call.respond(HttpStatusCode.NotImplemented, "\"${grantType}\" is invalid or not implemented")
                }

                if (clientSecret == null) {
                    return call.respond(HttpStatusCode.Unauthorized, "\"client_secret\" is not present")
                }
            }
        } catch (e: SerializationException) {
            return call.respond(HttpStatusCode.BadRequest, e.message.orEmpty())
        }

        val token = AuthCodeController.exchangeCode(params.code, params.clientSecret!!) // This is asserted to not be null in the statements above
        token ?: return call.respond(HttpStatusCode.MultiStatus, listOf(401, 403, 404))

        call.respond(token)
    }
}
