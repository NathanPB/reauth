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

package dev.nathanpb.reauth.oauth

import io.ktor.http.*

// https://tools.ietf.org/html/rfc6749#section-4.1.2.1
enum class AuthorizationError(val statusCode: HttpStatusCode, errorString: String? = null) {

    INVALID_REQUEST(HttpStatusCode.BadRequest),
    UNAUTHORIZED_CLIENT(HttpStatusCode.Forbidden),
    ACCESS_DENIED(HttpStatusCode.Unauthorized),
    UNSUPPORTED_RESPONSE_TYPE(HttpStatusCode.NotImplemented),
    INVALID_SCOPE(HttpStatusCode.BadRequest),
    SERVER_ERROR(HttpStatusCode.ServiceUnavailable),
    TEMPORARILY_UNAVAILABLE(HttpStatusCode.ServiceUnavailable);

    companion object {
        fun parse(error: String) = values().firstOrNull { it.errorString == error }
    }

    val errorString = errorString ?: name.toLowerCase()
}
