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

package dev.nathanpb.reauth

import com.mongodb.internal.HexUtils
import dev.nathanpb.reauth.config.*
import dev.nathanpb.reauth.management.installManagement
import dev.nathanpb.reauth.oauth.client.OAuth2ClientRouteHandler
import dev.nathanpb.reauth.oauth.server.OAuth2ServerRouteHandler
import dev.nathanpb.reauth.oauth.server.ReauthAccessToken
import dev.nathanpb.reauth.resource.IdentityController
import dev.nathanpb.reauth.utils.md5Hex
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.litote.kmongo.id.IdGenerator
import org.litote.kmongo.id.UUIDStringIdGenerator
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class ReauthServerImpl(env: ReauthEnvironment, config: ReauthConfiguration) : ReauthServer(env, config, MongoDriver()) {

    override val keypair = readKeyPair(env.dynamicDir)

    @OptIn(ExperimentalPathApi::class)
    override fun prepare() {
        env.dynamicDir.apply {
            if (exists()) {
                if (!isDirectory()) {
                    error(".dynamic dir could not be created")
                }
            } else {
                createDirectory()
            }
        }

        config.validate()

        IdGenerator.defaultGenerator = UUIDStringIdGenerator
    }

    override fun start() {
        mongo.initialize(env)
        embeddedServer(Netty, env.port) {

            install(CallLogging)
            install(ContentNegotiation) {
                json()
            }

            installManagement(this@ReauthServerImpl)

            routing {

                route("oauth") {
                    config.providers.forEach { provider ->
                        get("authorize/${provider.id}") {
                            OAuth2ServerRouteHandler.handleAuthorize(call, provider, this@ReauthServerImpl)
                        }
                    }

                    get("consent") {
                        OAuth2ServerRouteHandler.handleConsent(call)
                    }

                    post("token") {
                        OAuth2ServerRouteHandler.handleToken(call)
                    }
                }

                get("public_key.pub") {
                    val key = """
                    -----BEGIN RSA PUBLIC KEY-----
                    ${Base64.getEncoder().encodeToString(keypair.public.encoded)}
                    -----END RSA PUBLIC KEY-----
                """.trimIndent()

                    val md5 = md5Hex(key)
                    val sha256 = HexUtils.toHex(MessageDigest.getInstance("SHA-256").digest(key.toByteArray()))
                    val sha512 = HexUtils.toHex(MessageDigest.getInstance("SHA-512").digest(key.toByteArray()))

                    call.response.header("Digest", "md5=$md5,sha-256=$sha256,sha-512=$sha512")
                    call.respond(key)
                }

                get("identity") {
                    val authString = call.request.header("Authorization")?.split(" ") ?: return@get call.respond(
                        HttpStatusCode.Unauthorized)

                    if (authString.size != 2) {
                        return@get call.respond(HttpStatusCode.BadRequest, "malformed Authorization")
                    }

                    if (authString[0] != "Bearer") {
                        return@get call.respond(HttpStatusCode.UnprocessableEntity, "malformed token type")
                    }

                    val token = authString[1]
                    val jwt = kotlin.runCatching {
                        ReauthAccessToken.fromToken(token)
                    }.getOrNull()

                    return@get if (jwt != null) {
                        val identity = IdentityController.findIdentities(jwt.uid)
                            .filter { config.providers.any { p -> p.id == it.provider } && it.data != null }
                            .map { IdentityBuilder(it, config.providers) }
                            .onEach { it.applyScopeFilter(jwt.scopes) }
                            .let { config.identityMapper.reduce(it, config.providers) }

                        call.respond(identity)
                    } else call.respond(HttpStatusCode.Forbidden)
                }

                route("providers") {
                    config.providers.forEach { provider ->
                        get("${provider.id}/callback") {
                            OAuth2ClientRouteHandler.handleCallback(call, this@ReauthServerImpl)
                        }
                    }
                }

            }
        }.start()
    }

    override fun postStart() {
        println("           ▄              ▄\n          ▌▒█           ▄▀▒▌\n          ▌▒▒█        ▄▀▒▒▒▐\n         ▐▄▀▒▒▀▀▀▀▄▄▄▀▒▒▒▒▒▐\n       ▄▄▀▒░▒▒▒▒▒▒▒▒▒█▒▒▄█▒▐\n     ▄▀▒▒▒░░░▒▒▒░░░▒▒▒▀██▀▒▌\n    ▐▒▒▒▄▄▒▒▒▒░░░▒▒▒▒▒▒▒▀▄▒▒▌\n    ▌░░▌█▀▒▒▒▒▒▄▀█▄▒▒▒▒▒▒▒█▒▐\n   ▐░░░▒▒▒▒▒▒▒▒▌██▀▒▒░░░▒▒▒▀▄▌\n   ▌░▒▄██▄▒▒▒▒▒▒▒▒▒░░░░░░▒▒▒▒▌\n  ▌▒▀▐▄█▄█▌▄░▀▒▒░░░░░░░░░░▒▒▒▐\n  ▐▒▒▐▀▐▀▒░▄▄▒▄▒▒▒▒▒▒░▒░▒░▒▒▒▒▌\n  ▐▒▒▒▀▀▄▄▒▒▒▄▒▒▒▒▒▒▒▒░▒░▒░▒▒▐\n   ▌▒▒▒▒▒▒▀▀▀▒▒▒▒▒▒░▒░▒░▒░▒▒▒▌\n   ▐▒▒▒▒▒▒▒▒▒▒▒▒▒▒░▒░▒░▒▒▄▒▒▐\n    ▀▄▒▒▒▒▒▒▒▒▒▒▒░▒░▒░▒▄▒▒▒▒▌\n      ▀▄▒▒▒▒▒▒▒▒▒▒▄▄▄▀▒▒▒▒▄▀\n        ▀▄▄▄▄▄▄▀▀▀▒▒▒▒▒▄▄▀\n           ▒▒▒▒▒▒▒▒▒▒▀▀\n        WOW SUCH INSOMNIA")
        println("ReAuth was initialized successfully")
        println("Loaded ${config.managers.size} manager accounts. ${config.managers.joinToString { it.id }}")
    }

}
