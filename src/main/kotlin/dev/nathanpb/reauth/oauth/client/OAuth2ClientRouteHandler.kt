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
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.nathanpb.reauth.config.*
import dev.nathanpb.reauth.oauth.server.AuthCodeController
import dev.nathanpb.reauth.resource.IdentityController
import dev.nathanpb.reauth.oauth.server.SessionNoncePool
import dev.nathanpb.reauth.oauth.model.AuthorizeEndpointResponse
import dev.nathanpb.reauth.oauth.OAuth2AuthorizeException
import dev.nathanpb.reauth.oauth.model.OAuth2Token
import dev.nathanpb.reauth.oauth.server.ConsentController
import dev.nathanpb.reauth.oauth.server.OAuth2ServerRouteHandler
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.date.*
import java.lang.Exception
import java.time.Instant
import java.util.*

class OAuth2ClientRouteHandler(private val provider: OAuth2Provider) {

    suspend fun handleAuthorize(call: ApplicationCall) {
        val nonceParam = call.request.queryParameters["nonce"] ?: return call.respond(HttpStatusCode.BadRequest, "missing \"nonce\" parameter")
        val nonce = SessionNoncePool.retrieve(nonceParam) ?: return call.respond(HttpStatusCode.NotFound, "session not found")

        val session = ClientDealerSessionController.new(provider, nonce.client, nonce)
        call.respondRedirect(provider.buildAuthorizeUrl(session.id))
    }

    suspend fun handleCallback(call: ApplicationCall) {
        val params = AuthorizeEndpointResponse.receive(call.request.queryParameters)

        try {
            if (params.state == null) {
                return call.respond(HttpStatusCode.BadRequest, "missing state")
            }

            val session = ClientDealerSessionController.find(params.state) ?: return call.respond(HttpStatusCode.UnprocessableEntity, "dealer not found. Maybe the time is out?")
            ClientDealerSessionController.finalize(session.id)

            session.dealer.receiveRedirect(params)
            val uid = IdentityController.saveIdentity(
                session.dealer.getAccessToken(),
                session.dealer.provider,
                session.dealer.getUserData()
            ).uid

            if (!session.client.skipConsent) {
                val redirectURL = URLBuilder(APP_CONSENT_URL).apply {
                    parameters["token"] = JWT.create()
                        .withIssuer(ISSUER)
                        .withJWTId(ConsentController.createConsentWaiter(uid, session))
                        .withExpiresAt(Instant.now().plusSeconds(600).toGMTDate().toJvmDate())
                        .withSubject("resource_owner_consent")
                        .withNotBefore(Date())
                        .withAudience(uid)

                        .withClaim("client_display_name", session.client.displayName)
                        .withClaim("client_id", session.client.clientId.toString())
                        .withClaim("scope", session.initialRequest.scope)
                        .sign(Algorithm.RSA256(PUBLIC_KEY, PRIVATE_KEY))
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
