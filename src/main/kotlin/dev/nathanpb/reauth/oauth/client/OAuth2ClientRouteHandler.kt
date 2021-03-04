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
import dev.nathanpb.reauth.config.OAuth2Provider
import dev.nathanpb.reauth.controller.AuthCodeController
import dev.nathanpb.reauth.controller.DealerSessionController
import dev.nathanpb.reauth.controller.IdentityController
import dev.nathanpb.reauth.controller.SessionNoncePool
import dev.nathanpb.reauth.data.AuthorizeEndpointResponse
import dev.nathanpb.reauth.oauth.OAuth2AuthorizeException
import dev.nathanpb.reauth.oauth.OAuth2Token
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import java.lang.Exception

class OAuth2ClientRouteHandler(private val provider: OAuth2Provider) {

    suspend fun handleAuthorize(call: ApplicationCall) {
        val nonceParam = call.request.queryParameters["nonce"] ?: return call.respond(HttpStatusCode.BadRequest, "missing \"nonce\" parameter")
        val nonce = SessionNoncePool.retrieve(nonceParam) ?: return call.respond(HttpStatusCode.NotFound, "session not found")

        val session = DealerSessionController.new(provider, nonce.client, nonce)
        call.respondRedirect(provider.buildAuthorizeUrl(session.id))
    }

    suspend fun handleCallback(call: ApplicationCall) {
        val params = AuthorizeEndpointResponse.receive(call.request.queryParameters)

        try {
            if (params.state == null) {
                return call.respond(HttpStatusCode.BadRequest, "missing state")
            }

            val session = DealerSessionController.find(params.state) ?: return call.respond(HttpStatusCode.UnprocessableEntity, "dealer not found. Maybe the time is out?")
            DealerSessionController.finalize(session.id)

            session.dealer.receiveRedirect(params)
            val uid = IdentityController.saveIdentity(
                session.dealer.getAccessToken(),
                session.dealer.provider,
                session.dealer.getUserData()
            ).uid


            val token = OAuth2Token.newBearerToken(uid, session.client.clientId.toString(), session.initialParams.scope.orEmpty().split(" ").toSet())

            // TODO implement the "state" parameter. https://tools.ietf.org/html/rfc6749#section-4.1.2
            val redirectURL = URLBuilder(session.initialParams.redirectUri!!).apply {
                parameters["code"] = AuthCodeController.putTokenInThePool(token)
            }

            call.respondRedirect(redirectURL.buildString(), false)
        } catch (e: OAuth2AuthorizeException) {
            with(HttpStatusCode) {
                if (e.error.statusCode in listOf(NotImplemented, BadRequest, BadGateway)) {
                    e.printStackTrace()
                }
            }

            return call.respond(e.error.statusCode, e.message.orEmpty())
        } catch (e: Exception) {
            e.printStackTrace()
            return call.respond(HttpStatusCode.InternalServerError)
        }
    }

}
