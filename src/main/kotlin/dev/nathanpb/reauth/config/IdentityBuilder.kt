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

import com.github.jershell.kbson.toDocument
import dev.nathanpb.reauth.resource.Identity
import org.bson.Document
import org.graalvm.polyglot.Context

class IdentityMapper(private val schema: Map<String, String>) : Map<String, String> {

    init {
        if (containsKey("uid")) {
            error("\"uid\" is a keyword. Please check your identity builder file")
        }
    }

    override val entries = schema.entries

    override val keys = schema.keys

    override val size = schema.size

    override val values = schema.values

    override fun containsKey(key: String) = schema.containsKey(key)

    override fun containsValue(value: String) = schema.containsValue(value)

    override fun get(key: String) = schema[key]

    override fun isEmpty() = schema.isEmpty()

    fun reduce(builders: List<IdentityBuilder>, providers: List<OAuth2Provider>): Document {
        val engine = Context.newBuilder().build()

        val data = providers.joinToString("\n") { provider ->
            val builder = builders.firstOrNull { it.provider == provider }
            "const ${provider.id} = ${builder?.data?.toJson() ?: "null"};"
        }

        engine.eval("js", data)

        return schema.entries.fold(Document()) { acc, (key, expression) ->
            val result = engine.eval("js", expression).asString()
            if (result != null) acc.append(key, result) else acc
        }.append("uid", builders.first().identity.uid)
    }
}

class IdentityBuilder(val identity: Identity, providers: List<OAuth2Provider>) {

    val provider = providers.firstOrNull { it.id == identity.provider }

    var data: Document = identity.data!!.toDocument()
        private set

    fun applyScopeFilter(scopes: Set<String>) {
        data = provider?.dataAccessRules?.entries?.filter {
            scopes.any { scope -> scope in it.value }
        }?.map {
            it.key
        }?.fold(Document()) { doc, key ->
            val data = data[key]
            if (data != null) doc.append(key, data) else doc
        } ?: Document()
    }
}
