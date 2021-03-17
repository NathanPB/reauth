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

import com.mongodb.client.model.Updates
import dev.nathanpb.reauth.reauth
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.BsonDocument
import org.bson.Document
import org.litote.kmongo.Id
import org.litote.kmongo.eq
import org.litote.kmongo.newId

@Serializable
data class Identity(
    @Contextual @SerialName("_id") val id: Id<Identity> = newId(),
    val uid: String,
    val provider: String
) {
    companion object {
        // TODO modularize this
        val collection = reauth.mongo.db.getCollection<Identity>()
    }


    var data: BsonDocument?
        get() {
            return runBlocking {
                // TODO modularize this
                reauth.mongo.db.database.getCollection("identity")
                    .find(Identity::id eq id)
                    .awaitFirst()
                    .get("data", Document::class.java)
                    ?.toBsonDocument()
            }
        }
        set(value) {
            runBlocking {
                collection.updateOneById(id, Updates.set("data", value))
            }
        }
}
