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

package dev.nathanpb.reauth.user

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import dev.nathanpb.reauth.identities
import dev.nathanpb.reauth.md5Hex
import dev.nathanpb.reauth.oauth.OAuth2Token
import dev.nathanpb.reauth.oauth.client.OAuth2Provider
import org.bson.Document
import java.util.*

object IdentityController {

    fun saveIdentity(token: OAuth2Token, provider: OAuth2Provider, data: Map<String, Any>): Document {

        // Asserts that the id and linkage fields that are going to be used exists
        val idData = data[provider.idField] ?: error("No id field '${provider.idField}' found")
        val linkageData = data[provider.linkageField] ?: error("No linkage field '${provider.linkageField}' found")

        // Generates a user identifier based in the linkage field (usually email)
        // MD5 because its fast, appropriated size, and this should not be really secret
        val uid = md5Hex(data[provider.linkageField]?.toString().orEmpty())

        // Attempt to find and update an existing identity
        // Looks for documents with matching uid, data[idField] or data[linkageField] (usually email)
        // Only matches documents that belongs to the same provider
        // If any found, replaces the data and the token and returns the identity
        val updateExistingIdentify = identities.findOneAndUpdate(
            Filters.and(
                Filters.eq("provider", provider.id),
                Filters.or(
                    Filters.eq("uid", uid),
                    Filters.eq("data.${provider.idField}", idData),
                    Filters.eq("data.${provider.linkageField}", linkageData)
                )
            ),
            // Do not replace uid even if its out of sync with the current linkage field
            //   e.g. User logs in for the first time with Discord, then changes its Discord email and attempts to log in again
            //   in this situation, the old uid (based in the old email address) will be kept
            Updates.combine(
                Updates.set("data", Document(data)),
                Updates.set("token", token.document())
            )
        )

        if (updateExistingIdentify != null) {
            return updateExistingIdentify
        }

        // If not already existing, create a brand new identity
        return Document()
            .append("_id", UUID.randomUUID().toString())
            .append("uid", uid)
            .append("data", Document(data))
            .append("token", token.document())
            .also(identities::insertOne)
    }

    fun findIdentities(uid: String): List<Document> {
        return identities.find(Filters.eq("uid", uid)).toList()
    }

    fun removeSensitiveData(identity: Document) = Document(identity).apply {
        remove("token")
    }
}