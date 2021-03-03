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

import dev.nathanpb.reauth.controller.AuthCodeController
import dev.nathanpb.reauth.oauth.OAuth2AuthorizeException
import dev.nathanpb.reauth.oauth.client.OAuth2Dealer
import dev.nathanpb.reauth.controller.IdentityController
import dev.nathanpb.reauth.controller.SectionNoncePool
import dev.nathanpb.reauth.data.AuthorizeEndpointParams
import dev.nathanpb.reauth.oauth.OAuth2Token
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.id.IdGenerator
import org.litote.kmongo.id.UUIDStringIdGenerator
import org.litote.kmongo.reactivestreams.KMongo
import java.lang.Exception
import java.security.InvalidParameterException

val mongoClient = KMongo.createClient(System.getenv("MONGO_CONN_STRING") ?: error("MONGO_CONN_STRING is not set")).coroutine
val mongoDb = mongoClient.getDatabase(System.getenv("MONGO_DB_NAME") ?: "reauth")

fun main() {
    IdGenerator.defaultGenerator = UUIDStringIdGenerator
    embeddedServer(Netty, PORT) {

        install(CallLogging)
        install(ContentNegotiation) {
            json()
        }

        routing {

            route("oauth") {
                get("authorize") {
                    val params = try {
                        AuthorizeEndpointParams.receive(call.request.queryParameters).apply {
                            if (responseType != "code") {
                                return@get call.respond(HttpStatusCode.NotImplemented, "\"${responseType}\" code is invalid or not implemented")
                            }

                            if (clientId != CLIENT_ID) {
                                return@get call.respond(HttpStatusCode.NotFound, "client not found")
                            }

                            // TODO check if the scopes are valid
                            if (scope?.split(" ").isNullOrEmpty()) {
                                return@get call.respond(HttpStatusCode.BadRequest, "invalid or missing \"scope\"")
                            }

                            if (redirectUri != REDIRECT_URL) {
                                return@get call.respond(HttpStatusCode.NotAcceptable, "\"redirect_uri\" is not set or do not conform with the pre-defined URIs")
                            }
                        }

                    } catch (e: InvalidParameterException) {
                        return@get call.respond(HttpStatusCode.BadRequest, e.message.orEmpty())
                    }

                    call.respondRedirect(
                        URLBuilder(APP_AUTHORIZE_URL).apply {
                            parameters["nonce"] = SectionNoncePool.put(params)
                        }.buildString(),
                        false
                    )
                }
            }

            route("providers") {
                PROVIDERS.forEach { provider ->
                    route(provider.id) {
                        get("authorize") {
                            val nonceParam = call.request.queryParameters["nonce"] ?: return@get call.respond(HttpStatusCode.BadRequest, "missing \"nonce\" parameter")
                            SectionNoncePool.retrieve(nonceParam) ?: return@get call.respond(HttpStatusCode.NotFound, "session not found")

                            val dealer = OAuth2Dealer(provider)
                            call.respondRedirect(dealer.buildAuthorizeURL().toString())
                        }

                        get("callback") {
                            val code = call.request.queryParameters["code"]
                            val error = call.request.queryParameters["error"]
                            val dealer = OAuth2Dealer(provider)

                            try {
                                dealer.receiveRedirect(code, error)
                                val uid = IdentityController.saveIdentity(
                                    dealer.getAccessToken(),
                                    dealer.provider,
                                    dealer.getUserData()
                                ).uid

                                val token = OAuth2Token.newBearerToken(uid, CLIENT_ID, emptyList())

                                // TODO implement the "state" parameter. https://tools.ietf.org/html/rfc6749#section-4.1.2
                                val redirectURL = URLBuilder(REDIRECT_URL).apply {
                                    parameters["code"] = AuthCodeController.putTokenInThePool(token)
                                }

                                call.respondRedirect(redirectURL.buildString(), false)
                            } catch (e: OAuth2AuthorizeException) {
                                with(HttpStatusCode) {
                                    if (e.error.statusCode in listOf(NotImplemented, BadRequest, BadGateway)) {
                                        e.printStackTrace()
                                    }
                                }

                                return@get call.respond(e.error.statusCode, e.message.orEmpty())
                            } catch (e: Exception) {
                                e.printStackTrace()
                                return@get call.respond(HttpStatusCode.InternalServerError)
                            }
                        }
                    }
                }
            }

        }
    }.start()
}
