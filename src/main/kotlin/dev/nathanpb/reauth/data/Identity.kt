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

package dev.nathanpb.reauth.data

import com.mongodb.client.model.Updates
import dev.nathanpb.reauth.mongoDb
import dev.nathanpb.reauth.oauth.OAuth2Token
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.BSONObject
import org.bson.BsonDocument
import org.bson.Document
import org.litote.kmongo.Id
import org.litote.kmongo.eq
import org.litote.kmongo.newId
import org.litote.kmongo.set

@Serializable
data class Identity(
    @Contextual @SerialName("_id") val id: Id<Identity> = newId(),
    val uid: String,
    val provider: String,
    val token: OAuth2Token
) {
    companion object {
        val collection = mongoDb.getCollection<Identity>()
    }


    var data: BsonDocument?
        get() {
            return runBlocking {
                mongoDb.database.getCollection("identity")
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
