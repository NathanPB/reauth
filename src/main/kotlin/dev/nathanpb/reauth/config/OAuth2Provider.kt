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

package dev.nathanpb.reauth.config

import dev.nathanpb.reauth.oauth.client.DealerSession
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class OAuth2Provider (
    val id: String,
    val clientId: String,
    val clientSecret: String,
    val scope: String,
    val authorizeURL: String,
    val userDataURL: String,
    val tokenURL: String,
    val linkageField: String = "email",
    val idField: String = "id",
    val dataAccessRules: HashMap<String, Set<String>>
) {

    fun buildRedirectUrl(originUrl: String): String {
        return URLBuilder(originUrl).apply {
            path(listOf(encodedPath.takeLast(encodedPath.length-1), "providers/$id/callback").filterNot { it.isBlank() })
        }.buildString()
    }

    fun buildAuthorizeUrl(session: DealerSession): String {
        return URLBuilder(authorizeURL).apply {
            parameters["response_type"] = "code"
            parameters["client_id"] = clientId
            parameters["redirect_uri"] = buildRedirectUrl(session.origin)
            parameters["scope"] = scope
            parameters["state"] = session.id
        }.buildString()
    }
}
