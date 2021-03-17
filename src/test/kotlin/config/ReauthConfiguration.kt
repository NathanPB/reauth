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

package config

import dev.nathanpb.reauth.config.IdentityMapper
import dev.nathanpb.reauth.config.OAuth2Provider
import dev.nathanpb.reauth.config.ReauthConfiguration
import dev.nathanpb.reauth.management.ManagerAccount
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ReauthConfiguration {

    private val emptyConfig = ReauthConfiguration(emptySet(), emptyList(), IdentityMapper(emptyMap()), emptyList())

    private val fakeProvider = OAuth2Provider("id", "clientId", "clientSecret", "a b", "authorize", "data", "token", "email", "id", HashMap())
    private val fakeToken = "a".repeat(64)

    @Test
    fun `should not accept manager accounts with duplicated IDs`() {
        val invalid = emptyConfig.copy(managers = listOf(
                ManagerAccount("a", "Manager A", fakeToken),
                ManagerAccount("a", "Manager B", fakeToken),
                ManagerAccount("c", "Manager C", fakeToken),
                ManagerAccount("d", "Manager D", fakeToken)
            )
        )

        val valid = emptyConfig.copy(managers = listOf(
            ManagerAccount("a", "ManagerA", fakeToken),
            ManagerAccount("b", "ManagerB", fakeToken)
        ))

        Assertions.assertThrows(IllegalStateException::class.java) {
            invalid.validate()
        }

        Assertions.assertDoesNotThrow {
            valid.validate()
        }
    }

    @Test
    fun `should not accept managers with invalid IDs`() {
        val valid = emptyConfig.copy(managers = listOf(ManagerAccount("foo-bar", "Manager A", fakeToken)))
        val invalid = emptyConfig.copy(managers = listOf(ManagerAccount("FooBar", "Manager A", fakeToken)))

        Assertions.assertDoesNotThrow {
            valid.validate()
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            invalid.validate()
        }
    }

    @Test
    fun `should not accept managers with blank display name`() {
        val valid = emptyConfig.copy(managers = listOf(ManagerAccount("a", "Manager A", fakeToken)))
        val invalid = emptyConfig.copy(managers = listOf(ManagerAccount("a", "\t", fakeToken)))
        val invalid2 = emptyConfig.copy(managers = listOf(ManagerAccount("a", "", fakeToken)))
        val invalid3 = emptyConfig.copy(managers = listOf(ManagerAccount("a", " ", fakeToken)))

        Assertions.assertDoesNotThrow {
            valid.validate()
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            invalid.validate()
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            invalid2.validate()
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            invalid3.validate()
        }
    }

    @Test
    fun `should not accept managers with a token that does not have at least 64 characters`() {
        val valid = emptyConfig.copy(managers = listOf(ManagerAccount("a", "Manager A", "a".repeat(64))))
        val valid2 = emptyConfig.copy(managers = listOf(ManagerAccount("a", "Manager A", "a".repeat(128))))
        val invalid = emptyConfig.copy(managers = listOf(ManagerAccount("a", "Manager A", "a".repeat(63))))
        val invalid2 = emptyConfig.copy(managers = listOf(ManagerAccount("a", "Manager A", "a".repeat(32))))

        Assertions.assertDoesNotThrow {
            valid.validate()
            valid2.validate()
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            invalid.validate()
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            invalid2.validate()
        }
    }

    @Test
    fun `should not accept scopes that does not exists in the dataAccessRules`() {
        val valid = emptyConfig.copy(
            scopes = setOf("identify", "email"),
            providers = listOf(
                fakeProvider.copy(
                    dataAccessRules = HashMap(mapOf(
                        "id" to setOf("identify", "email"),
                        "email" to setOf("email")
                    ))
                )
            )
        )

        val invalid = emptyConfig.copy(
            scopes = setOf("identify", "email"),
            providers = listOf(
                fakeProvider.copy(
                    dataAccessRules = HashMap(mapOf(
                        "id" to setOf("identify", "email"),
                        "email" to setOf("email"),
                        "friends" to setOf("contacts")
                    ))
                )
            )
        )

        Assertions.assertDoesNotThrow {
            valid.validate()
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            invalid.validate()
        }
    }
}
