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
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import kotlin.io.path.*

private val keyFactor = KeyFactory.getInstance("RSA")

data class RSAKeyPair(val public: RSAPublicKey, val private: RSAPrivateKey)

@OptIn(ExperimentalPathApi::class)
fun readKeyPair(dir: Path): RSAKeyPair {
    val public = dir.resolve("public_key.pub")
    val private = dir.resolve("private_key.key")

    if (!public.isRegularFile() || !private.isRegularFile()) {
        if (public.exists() || private.exists()) {
            error("Cannot read or generate RSA key pair")
        } else {
            return generateRSAKeyPair().also {
                public.writeLines(listOf(
                    "-----BEGIN RSA PUBLIC KEY-----",
                    Base64.getEncoder().encodeToString(it.public.encoded),
                    "-----END RSA PUBLIC KEY-----\n"
                ))

                private.writeLines(listOf(
                    "-----BEGIN RSA PRIVATE KEY-----",
                    Base64.getEncoder().encodeToString(it.private.encoded),
                    "-----END RSA PRIVATE KEY-----\n"
                ))
            }
        }
    }

    return RSAKeyPair(
        readX509PublicKey(public),
        readPKCS8PrivateKey(private)
    )
}

private fun generateRSAKeyPair() : RSAKeyPair {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048)
    val pair = generator.generateKeyPair()
    return RSAKeyPair(
        pair.public as RSAPublicKey,
        pair.private as RSAPrivateKey
    )
}

@OptIn(ExperimentalPathApi::class)
private fun readX509PublicKey(file: Path): RSAPublicKey {
    val text = file.readText()
        .replace("-----BEGIN RSA PUBLIC KEY-----", "")
        .replace(System.lineSeparator(), "")
        .replace("-----END RSA PUBLIC KEY-----", "")

    val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(text))
    return keyFactor.generatePublic(keySpec) as RSAPublicKey
}

@OptIn(ExperimentalPathApi::class)
private fun readPKCS8PrivateKey(file: Path): RSAPrivateKey {
    val text = file.readText()
        .replace("-----BEGIN RSA PRIVATE KEY-----", "")
        .replace(System.lineSeparator(), "")
        .replace("-----END RSA PRIVATE KEY-----", "")

    val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(text))
    return keyFactor.generatePrivate(keySpec) as RSAPrivateKey
}
