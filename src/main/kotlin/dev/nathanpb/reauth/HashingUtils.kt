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

package dev.nathanpb.reauth

import com.mongodb.internal.HexUtils
import java.security.MessageDigest
import java.security.SecureRandom


private val md5 = MessageDigest.getInstance("MD5")

fun md5Hex(input: String) = HexUtils.toHex(md5.digest(input.toByteArray()))

fun randomHex(byteSize: Int) : String {
    val array = ByteArray(byteSize)
    SecureRandom().nextBytes(array)
    return HexUtils.toHex(array)
}
