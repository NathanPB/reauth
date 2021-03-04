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

package dev.nathanpb.reauth.config

import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class OAuth2Provider (
    val id: String,
    val clientId: String,
    val clientSecret: String,
    val scopes: Set<String>,
    val authorizeURL: String,
    val userDataURL: String,
    val tokenURL: String,
    val linkageField: String = "email",
    val idField: String = "id",
    val dataAccessRules: HashMap<String, Set<String>>
) {

    init {
        val invalidScopes = dataAccessRules.values.flatten().filterNot { it in SCOPES }.toSet()
        if (invalidScopes.isNotEmpty()) {
            error("Invalid scopes found. Did you forget to add it to the $SCOPES_FILE file? Scopes: ${invalidScopes.joinToString(", ")}")
        }
    }

    fun buildAuthorizeUrl(sessionId: String): String {
        return URLBuilder(authorizeURL).apply {
            parameters["response_type"] = "code"
            parameters["client_id"] = clientId
            parameters["redirect_uri"] = URLBuilder(BASE_URL).path("providers/$id/callback").buildString()
            parameters["scope"] = scopes.joinToString(" ")
            parameters["state"] = sessionId
        }.buildString()
    }
}
