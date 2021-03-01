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

import dev.nathanpb.reauth.oauth.OAuth2AuthorizeException
import dev.nathanpb.reauth.oauth.client.OAuth2Dealer
import dev.nathanpb.reauth.user.IdentityController
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

            route("authenticate") {
                PROVIDERS.forEach { provider ->
                    get(provider.id) {
                        val dealer = OAuth2Dealer(provider)
                        call.respondRedirect(dealer.buildRedirectURL().toString())
                    }
                }
            }

            route("receiver") {
                PROVIDERS.forEach { provider ->
                    get(provider.id) {
                        val code = call.request.queryParameters["code"]
                        val error = call.request.queryParameters["error"]
                        val dealer = OAuth2Dealer(provider)

                        try {
                            dealer.receiveRedirect(code, error)
                            call.respond(
                                IdentityController.saveIdentity(
                                    dealer.getAccessToken(),
                                    dealer.provider,
                                    dealer.getUserData()
                                )
                            )
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
    }.start()
}
