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

package dev.nathanpb.reauth.oauth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// https://tools.ietf.org/html/rfc6749#section-4.1.3
@Serializable
data class TokenEndpointRequest (
    @SerialName("grant_type")    val grantType: String,
    @SerialName("code")          val code: String,
    @SerialName("redirect_uri")  val redirectUri: String,
    @SerialName("client_id")     val clientId: String,
    @SerialName("client_secret") val clientSecret: String? = null // why is it not in the specification? Guess I'm missing something
)
