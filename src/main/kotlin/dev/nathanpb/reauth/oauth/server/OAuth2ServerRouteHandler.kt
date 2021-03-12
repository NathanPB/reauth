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

import dev.nathanpb.reauth.config.OAuth2Provider
import dev.nathanpb.reauth.config.SCOPES
import dev.nathanpb.reauth.oauth.client.ClientDealerSessionController
import dev.nathanpb.reauth.oauth.client.DealerSession
import dev.nathanpb.reauth.oauth.model.AuthorizeEndpointRequest
import dev.nathanpb.reauth.oauth.model.TokenEndpointRequest
import dev.nathanpb.reauth.oauth.model.TokenEndpointResponse
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import kotlinx.serialization.SerializationException
import java.security.InvalidParameterException
import java.time.LocalDateTime

object OAuth2ServerRouteHandler {

    suspend fun redirectToCallback(uid: String, session: DealerSession, call: ApplicationCall) {
        val token = ReauthAccessToken(
            uid = uid,
            clientId = session.client.clientId.toString(),
            scopes =  session.initialRequest.scope.orEmpty().split(" ").toSet(),
            expiresAt = LocalDateTime.now().plusDays(12) // TODO maybe "unhardcode" this?
        )

        val redirectURL = URLBuilder(session.initialRequest.redirectUri!!).apply {
            parameters["code"] = AuthCodeController.putTokenInThePool(token)

            if (session.initialRequest.state != null) {
                parameters["state"] = session.initialRequest.state
            }
        }

        call.respondRedirect(redirectURL.buildString(), false)
    }

    suspend fun handleAuthorize(call: ApplicationCall, provider: OAuth2Provider) {
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

        val origin = URLBuilder().apply {
            protocol = when (call.request.origin.scheme) {
                "http" -> URLProtocol.HTTP
                "https" -> URLProtocol.HTTPS
                else -> error("protocol ${call.request.origin.scheme} not supported")
            }
            host = call.request.host()
            port = call.request.port()
        }

        val session = ClientDealerSessionController.new(
            provider,
            params.client,
            params,
            origin.buildString()
        )
        call.respondRedirect(provider.buildAuthorizeUrl(session))
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
        token ?: return call.respond(HttpStatusCode.MultiStatus, listOf(401, 403, 404)) // TODO make this response more accurate

        call.respond(TokenEndpointResponse(token))
    }

    suspend fun handleConsent(call: ApplicationCall) {
        val nonce = call.request.queryParameters["code"] ?: return call.respond(HttpStatusCode.Unauthorized, "missing \"code\" parameter with the consent jwt id")
        val (uid, session) = ConsentController.receiveConsent(nonce) ?: return call.respond(HttpStatusCode.Gone, "has your session timed out?")
        return redirectToCallback(uid, session, call)
    }
}
