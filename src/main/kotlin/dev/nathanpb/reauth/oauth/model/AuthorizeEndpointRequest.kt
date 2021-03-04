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

package dev.nathanpb.reauth.oauth.model

import dev.nathanpb.reauth.resource.ClientController
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.security.InvalidParameterException

// https://tools.ietf.org/html/rfc6749#section-4.1.1
data class AuthorizeEndpointRequest(
    val clientId: String,
    val responseType: String,
    val redirectUri: String?,
    val scope: String?,
    val state: String?
) {
    companion object {
        fun receive(parameters: Parameters): AuthorizeEndpointRequest {
            return AuthorizeEndpointRequest(
                parameters["client_id"] ?: throw InvalidParameterException("missing client_id"),
                parameters["response_type"] ?: throw InvalidParameterException("missing response_type"),
                parameters["redirect_uri"],
                parameters["scope"],
                parameters["state"],
            )
        }
    }

    val client by lazy {
        runBlocking {
            ClientController.findClientById(clientId) ?: error("Client $clientId could not be found")
        }
    }

    fun clientExists() = kotlin.runCatching { client }.isSuccess
}
