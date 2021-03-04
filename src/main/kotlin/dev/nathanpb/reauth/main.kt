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

import dev.nathanpb.reauth.config.PORT
import dev.nathanpb.reauth.config.PROVIDERS
import dev.nathanpb.reauth.resource.IdentityController
import dev.nathanpb.reauth.oauth.client.OAuth2ClientRouteHandler
import dev.nathanpb.reauth.oauth.server.OAuth2ServerRouteHandler
import dev.nathanpb.reauth.oauth.server.ReauthJWT
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.id.IdGenerator
import org.litote.kmongo.id.UUIDStringIdGenerator
import org.litote.kmongo.reactivestreams.KMongo

val mongoClient = KMongo.createClient(System.getenv("MONGO_CONN_STRING") ?: error("MONGO_CONN_STRING is not set")).coroutine
val mongoDb = mongoClient.getDatabase(System.getenv("MONGO_DB_NAME") ?: "reauth")

fun main() {
    println("""
                           ▄              ▄
                          ▌▒█           ▄▀▒▌
                          ▌▒▒█        ▄▀▒▒▒▐
                         ▐▄▀▒▒▀▀▀▀▄▄▄▀▒▒▒▒▒▐
                       ▄▄▀▒░▒▒▒▒▒▒▒▒▒█▒▒▄█▒▐
                     ▄▀▒▒▒░░░▒▒▒░░░▒▒▒▀██▀▒▌
                    ▐▒▒▒▄▄▒▒▒▒░░░▒▒▒▒▒▒▒▀▄▒▒▌
                    ▌░░▌█▀▒▒▒▒▒▄▀█▄▒▒▒▒▒▒▒█▒▐
                   ▐░░░▒▒▒▒▒▒▒▒▌██▀▒▒░░░▒▒▒▀▄▌
                   ▌░▒▄██▄▒▒▒▒▒▒▒▒▒░░░░░░▒▒▒▒▌
                  ▌▒▀▐▄█▄█▌▄░▀▒▒░░░░░░░░░░▒▒▒▐
                  ▐▒▒▐▀▐▀▒░▄▄▒▄▒▒▒▒▒▒░▒░▒░▒▒▒▒▌
                  ▐▒▒▒▀▀▄▄▒▒▒▄▒▒▒▒▒▒▒▒░▒░▒░▒▒▐
                   ▌▒▒▒▒▒▒▀▀▀▒▒▒▒▒▒░▒░▒░▒░▒▒▒▌
                   ▐▒▒▒▒▒▒▒▒▒▒▒▒▒▒░▒░▒░▒▒▄▒▒▐
                    ▀▄▒▒▒▒▒▒▒▒▒▒▒░▒░▒░▒▄▒▒▒▒▌
                      ▀▄▒▒▒▒▒▒▒▒▒▒▄▄▄▀▒▒▒▒▄▀
                        ▀▄▄▄▄▄▄▀▀▀▒▒▒▒▒▄▄▀
                           ▒▒▒▒▒▒▒▒▒▒▀▀
                        WOW SUCH INSOMNIA
    """.trimIndent())

    IdGenerator.defaultGenerator = UUIDStringIdGenerator
    embeddedServer(Netty, PORT) {

        install(CallLogging)
        install(ContentNegotiation) {
            json()
        }

        routing {

            route("oauth") {
                get("authorize") {
                    OAuth2ServerRouteHandler.handleAuthorize(call)
                }

                post("token") {
                    OAuth2ServerRouteHandler.handleToken(call)
                }
            }

            get("identity") {
                val authString = call.request.header("Authorization")?.split(" ") ?: return@get call.respond(HttpStatusCode.Unauthorized)

                if (authString.size != 2) {
                    return@get call.respond(HttpStatusCode.BadRequest, "malformed Authorization")
                }

                if (authString[0] != "Bearer") {
                    return@get call.respond(HttpStatusCode.UnprocessableEntity, "malformed token type")
                }

                val token = authString[1]
                val jwt = kotlin.runCatching {
                    ReauthJWT.fromToken(token)
                }.getOrNull()

                return@get if (jwt != null) {
                    val identities = IdentityController.findIdentities(jwt.uid)
                        .associateWith { it.getDataForScopes(jwt.scopes) }
                        .filter { (_, value) -> value != null && value.isNotEmpty()}

                    // cursed kotlin
                    // todo what about being a decent human being?
                    call.respond(
                        """
                            [
                                ${
                            identities.entries.joinToString(", ") { (it, data) ->
                                """
                                {
                                    "uid": "${it.id}",
                                    "provider": "${it.provider}",
                                    "data": $data
                                }
                                """.trimIndent()
                            }
                                }
                            ]
                        """.trimIndent()
                    )
                } else call.respond(HttpStatusCode.Forbidden)
            }

            route("providers") {
                PROVIDERS.forEach { provider ->
                    OAuth2ClientRouteHandler(provider).apply {
                        route(provider.id) {
                            get("authorize") {
                                handleAuthorize(call)
                            }

                            get("callback") {
                                handleCallback(call)
                            }
                        }
                    }
                }
            }

        }
    }.start()
}
