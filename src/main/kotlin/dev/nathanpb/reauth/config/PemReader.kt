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

package dev.nathanpb.reauth.config

import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText

private val keyFactor = KeyFactory.getInstance("RSA")

@OptIn(ExperimentalPathApi::class)
fun readX509PublicKey(file: Path): RSAPublicKey {
    val text = file.readText()
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace(System.lineSeparator(), "")
        .replace("-----END PUBLIC KEY-----", "")

    val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(text))
    return keyFactor.generatePublic(keySpec) as RSAPublicKey
}

@OptIn(ExperimentalPathApi::class)
fun readPKCS8PrivateKey(file: Path): RSAPrivateKey {
    val text = file.readText()
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace(System.lineSeparator(), "")
        .replace("-----END PRIVATE KEY-----", "")

    val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(text))
    return keyFactor.generatePrivate(keySpec) as RSAPrivateKey
}
