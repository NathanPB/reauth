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
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
val DYNAMIC_DIR = Paths.get(System.getenv("DYNAMIC_DIR") ?: "./.dynamic").apply {
    if (exists()) {
        if (!isDirectory()) {
            error(".dynamic dir could not be created")
        }
    } else {
        createDirectory()
    }
}

val PORT = System.getenv("PORT")?.toIntOrNull() ?: 6660
val PROVIDERS_FILE: Path = Paths.get(System.getenv("PROVIDERS_FILE") ?: "./providers.json")
val SCOPES_FILE: Path = Paths.get(System.getenv("PROVIDERS_FILE") ?: "./scopes.json")
val BASE_URL = System.getenv("BASE_URL") ?: error("BASE_URL is not set")
val ISSUER = System.getenv("ISSUER") ?: "reauth"

val APP_CONSENT_URL = System.getenv("APP_CONSENT_URL") ?: error("APP_CONSENT_URI is not set")

val RSA_KEYPAIR = readKeyPair(DYNAMIC_DIR)

@OptIn(ExperimentalPathApi::class)
val SCOPES = Json.decodeFromString<Set<String>>(SCOPES_FILE.readText())

@OptIn(ExperimentalPathApi::class)
val PROVIDERS = Json.decodeFromString<List<OAuth2Provider>>(PROVIDERS_FILE.readText())
