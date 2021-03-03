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

import dev.nathanpb.reauth.APP_AUTHORIZE_URL
import dev.nathanpb.reauth.CLIENT_ID
import dev.nathanpb.reauth.REDIRECT_URL
import dev.nathanpb.reauth.controller.SessionNoncePool
import dev.nathanpb.reauth.data.AuthorizeEndpointParams
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import java.security.InvalidParameterException

object OAuth2ServerRouteHandler {
    suspend fun handleAuthorize(call: ApplicationCall) {
        val params = try {
            AuthorizeEndpointParams.receive(call.request.queryParameters).apply {
                if (responseType != "code") {
                    return call.respond(HttpStatusCode.NotImplemented, "\"${responseType}\" code is invalid or not implemented")
                }

                if (clientId != CLIENT_ID) {
                    return call.respond(HttpStatusCode.NotFound, "client not found")
                }

                // TODO check if the scopes are valid
                if (scope?.split(" ").isNullOrEmpty()) {
                    return call.respond(HttpStatusCode.BadRequest, "invalid or missing \"scope\"")
                }

                if (redirectUri != REDIRECT_URL) {
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
}
