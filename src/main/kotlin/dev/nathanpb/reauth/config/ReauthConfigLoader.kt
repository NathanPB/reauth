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

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText

@OptIn(ExperimentalPathApi::class)
object ReauthConfigLoader {

    private fun env(key: String, default: String? = null): String {
        return System.getenv(key) ?: default ?: error("$key is not set")
    }

    fun loadEnv() = ReauthEnvironment(
        env("PORT", "6660").toInt(),
        env("ISSUER", "reauth"),
        env("APP_CONSENT_URI"),
        env("MONGO_CONN_STRING"),
        env("MONGO_DB_NAME", "reauth"),
        Paths.get(env("DYNAMIC_DIR", "./.dynamic")),
        Paths.get(env("PROVIDERS_FILE", "./providers.json")),
        Paths.get(env("SCOPES_FILE", "./scopes.json")),
        Paths.get(env("MANAGERS_FILE", "./managers.json")),
        Paths.get(env("IDENTITY_FILE", "./identity.json")),
    )

    fun loadConfig(env: ReauthEnvironment) = ReauthConfiguration(
        Json.decodeFromString(env.scopesFile.readText()),
        Json.decodeFromString(env.providersFile.readText()),
        IdentityMapper(Json.decodeFromString(env.identityFile.readText())),
        Json.decodeFromString(env.managersFile.readText())
    )
}
