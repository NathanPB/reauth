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

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.lang.Exception

fun main() {
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
                            dealer.getAccessToken()
                            call.respond(dealer.getAccessToken())
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
