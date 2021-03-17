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

package dev.nathanpb.reauth.oauth.client
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.nathanpb.reauth.ReauthServer
import dev.nathanpb.reauth.oauth.OAuth2AuthorizeException
import dev.nathanpb.reauth.oauth.model.AuthorizeEndpointResponse
import dev.nathanpb.reauth.oauth.server.ConsentController
import dev.nathanpb.reauth.oauth.server.OAuth2ServerRouteHandler
import dev.nathanpb.reauth.resource.IdentityController
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.date.*
import java.time.Instant
import java.util.*

object OAuth2ClientRouteHandler {

    suspend fun handleCallback(call: ApplicationCall, server: ReauthServer) {
        val params = AuthorizeEndpointResponse.receive(call.request.queryParameters)

        try {
            if (params.state == null) {
                return call.respond(HttpStatusCode.BadRequest, "missing state")
            }

            val session = ClientDealerSessionController.find(params.state) ?: return call.respond(HttpStatusCode.UnprocessableEntity, "dealer not found. Maybe the time is out?")
            ClientDealerSessionController.finalize(session.id)

            params.verify(session.dealer.provider)
            session.dealer.receiveCode(params.code!!)

            val uid = IdentityController.saveIdentity(
                session.dealer.provider,
                session.dealer.getUserData()
            ).uid

            if (!session.client.skipConsent) {
                val redirectURL = URLBuilder(server.env.appConsentUri).apply {
                    parameters["token"] = JWT.create()
                        .withIssuer(server.env.issuer)
                        .withJWTId(ConsentController.createConsentWaiter(uid, session))
                        .withExpiresAt(Instant.now().plusSeconds(600).toGMTDate().toJvmDate())
                        .withSubject("resource_owner_consent")
                        .withNotBefore(Date())
                        .withAudience(uid)

                        .withClaim("client_display_name", session.client.displayName)
                        .withClaim("client_id", session.client.clientId)
                        .withClaim("scope", session.initialRequest.scope)
                        .sign(Algorithm.RSA256(server.keypair.public, server.keypair.private))
                }

                return call.respondRedirect(redirectURL.buildString(), false)
            }

            return OAuth2ServerRouteHandler.redirectToCallback(uid, session, call)
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
