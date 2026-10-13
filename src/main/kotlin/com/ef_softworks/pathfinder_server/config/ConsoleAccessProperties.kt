// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.config

import com.ef_softworks.pathfinder_server.consoleuser.ConsoleRole
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Who may enter the console and for how long a session lasts. Checked at start-up: a weak setup never boots.
 *
 * Users come straight from the environment as comma-separated "login:password" pairs, one list per role. The
 * pair is split at the first colon, so a password may contain colons but not commas.
 */
@ConfigurationProperties(prefix = "pathfinder.console")
data class ConsoleAccessProperties(
    /** HMAC key of the access and refresh tokens. Changing it invalidates every issued token. */
    val tokenSecret: String,
    val accessTokenTtl: Duration = Duration.ofMinutes(15),
    val refreshTokenTtl: Duration = Duration.ofDays(7),
    val adminUsers: List<String> = emptyList(),
    val viewerUsers: List<String> = emptyList(),
) {
    val seededUsers: List<SeededConsoleUser> =
        adminUsers.map { parsePair(it, ConsoleRole.ADMIN) } + viewerUsers.map { parsePair(it, ConsoleRole.VIEWER) }

    init {
        require(tokenSecret.length >= MIN_TOKEN_SECRET_LENGTH) {
            "PF_JWT_SECRET must be at least $MIN_TOKEN_SECRET_LENGTH characters (HS256 needs a 256-bit key)"
        }
        require(!accessTokenTtl.isNegative && !accessTokenTtl.isZero) { "PF_ACCESS_TOKEN_TTL must be positive" }
        require(!refreshTokenTtl.isNegative && !refreshTokenTtl.isZero) { "PF_REFRESH_TOKEN_TTL must be positive" }
        require(seededUsers.any { it.role == ConsoleRole.ADMIN }) {
            "PF_ADMIN_USERS must name at least one administrator as login:password"
        }
        val logins = seededUsers.map { it.login }
        require(logins.size == logins.toSet().size) {
            "PF_ADMIN_USERS / PF_VIEWER_USERS list the same login twice: ${logins.groupBy { it }.filterValues { it.size > 1 }.keys}"
        }
    }

    private fun parsePair(pair: String, role: ConsoleRole): SeededConsoleUser {
        val separator = pair.indexOf(':')
        require(separator > 0 && separator < pair.length - 1) {
            "Console user entries are login:password pairs, got '${pair.take(16)}'"
        }
        val login = pair.substring(0, separator).trim()
        val password = pair.substring(separator + 1)
        require(LOGIN_PATTERN.matches(login)) {
            "Console login '$login' must be 3-64 characters of latin letters, digits, '.', '_' or '-'"
        }
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "Console password of '$login' must be at least $MIN_PASSWORD_LENGTH characters"
        }
        return SeededConsoleUser(login, password, role)
    }

    companion object {
        private const val MIN_TOKEN_SECRET_LENGTH = 32
        private const val MIN_PASSWORD_LENGTH = 8
        private val LOGIN_PATTERN = Regex("^[A-Za-z0-9._-]{3,64}$")
    }
}

/** One console account as the environment declares it; the password is hashed before it reaches the database. */
data class SeededConsoleUser(
    val login: String,
    val password: String,
    val role: ConsoleRole,
)
