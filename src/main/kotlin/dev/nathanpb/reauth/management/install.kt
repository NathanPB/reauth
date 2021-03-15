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

package dev.nathanpb.reauth.management

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.GraphQL
import dev.nathanpb.reauth.config.MANAGERS
import dev.nathanpb.reauth.management.data.ClientInput
import dev.nathanpb.reauth.resource.Client
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.limit
import org.litote.kmongo.skip
import java.net.URI


private val AUTHORIZATION_HEADER_REGEX = "^(?i)Bearer (.*)(?-i)".toRegex()

fun Application.installManagement() {
    install(GraphQL) {
        useDefaultPrettyPrinter = true
        playground = System.getProperty("gql-dev") == "true"

        context { call ->
            runBlocking {
                val token = call.request.header("Authorization")?.let {
                    val match = AUTHORIZATION_HEADER_REGEX.findAll(it).firstOrNull()?.groupValues
                    if (match?.size == 2) match[1] else null
                }

                if (token.isNullOrBlank()) {
                    return@runBlocking
                }

                val account = MANAGERS.firstOrNull { it.token == token } ?: return@runBlocking

                +account
            }
        }

        fun Context.manager() = get<ManagerAccount>() ?: error("Not Authorized")

        fun Context.authenticate() = try {
            manager()
            null
        } catch (e: Exception) { e }

        fun validateUriList(uris: List<String>) {
            uris.map(URI::create)
                .onEach {
                    if (it.scheme != "https" && it.host != "localhost") {
                        throw IllegalArgumentException("Non-HTTPS URIs are only available on localhost")
                    }
                }.apply {
                    if (distinct().size != size) {
                        throw IllegalArgumentException("Duplicate URI found")
                    }
                }
        }


        schema {
            type<Client>()
            inputType<ClientInput>()

            query("client") {
                accessRule(Context::authenticate)

                resolver { clientId: String ->
                    Client.collection.findOneById(clientId)
                }
            }

            query("clients") {
                accessRule(Context::authenticate)

                resolver { limit: Int, offset: Int ->
                    if (limit > 50) {
                        error("Limit cannot be greater than 50")
                    }

                    Client.collection.aggregate<Client>(
                        listOf(
                            limit(limit),
                            skip(offset)
                        )
                    ).toList()
                }.withArgs {
                    arg<Int> { name = "limit"; defaultValue = 50 }
                    arg<Int> { name = "offset"; defaultValue = 0 }
                }
            }

            mutation("createClient") {
                accessRule(Context::authenticate)

                resolver { input: ClientInput ->

                    // TODO error catch https://github.com/aPureBase/KGraphQL/issues/68
                    validateUriList(input.redirectUris)

                    Client(
                        displayName = input.displayName,
                        skipConsent = input.skipConsent,
                        redirectUris = input.redirectUris
                    ).also {
                        Client.collection.save(it)
                    }
                }
            }

            mutation("updateClient") {
                accessRule(Context::authenticate)

                resolver { clientId: String, input: ClientInput ->

                    // TODO error catch https://github.com/aPureBase/KGraphQL/issues/68
                    validateUriList(input.redirectUris)

                    val client = Client.collection.findOneById(clientId) ?: throw NullPointerException()


                    client.copy(
                        displayName = input.displayName,
                        skipConsent = input.skipConsent,
                        redirectUris = input.redirectUris
                    ).also {
                        Client.collection.save(it)
                    }
                }
            }

            mutation("deleteClient") {
                accessRule(Context::authenticate)

                resolver { clientId: String ->
                    Client.collection.deleteOneById(clientId).run {
                        wasAcknowledged() && deletedCount > 0
                    }
                }
            }
        }
    }
}
