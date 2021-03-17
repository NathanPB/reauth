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

import dev.nathanpb.reauth.management.ManagerAccount

data class ReauthConfiguration(
    val scopes: Set<String>,
    val providers: List<OAuth2Provider>,
    val identityMapper: IdentityMapper,
    val managers: List<ManagerAccount>
) {
    fun validate() {
        managers.apply {
            if (distinctBy { it.id }.size != size) {
                error("managers account have duplicate IDs")
            }

            forEach {
                if (!it.id.matches("[0-9a-z-]+".toRegex())) {
                    error("manager ${it.id} does not matches required identifier rules")
                }

                if (it.displayName.isBlank()) {
                    error("manager ${it.id} display name is empty")
                }

                if (it.token.length < 64) {
                    error("manager ${it.id} token does not have the minimum size of 64 characters")
                }
            }
        }

        providers.forEach { provider ->
            val invalidScopes = provider.dataAccessRules.values.flatten().filterNot { it in scopes }.toSet()
            if (invalidScopes.isNotEmpty()) {
                error("Invalid scopes found. Did you forget to add it to the scopes file? Scopes: ${invalidScopes.joinToString(", ")}")
            }
        }
    }
}
