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

package dev.nathanpb.reauth.resource

import dev.nathanpb.reauth.mongoDb
import dev.nathanpb.reauth.randomHex
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.newId

@Serializable
data class Client (
    @Contextual @SerialName("_id") val clientId: Id<Client> = newId(),
    val clientSecret: String = randomHex(128),
    val displayName: String,
    val redirectUris: List<String> = emptyList(),
    val skipConsent: Boolean = false, // TODO make skipConsent work
) {
    companion object {
        val collection = mongoDb.getCollection<Client>()
    }
}
