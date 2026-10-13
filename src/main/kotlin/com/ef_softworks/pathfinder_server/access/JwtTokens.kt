// SPDX-License-Identifier: BUSL-1.1
// Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
package com.ef_softworks.pathfinder_server.access

import com.ef_softworks.pathfinder_server.config.ConsoleAccessProperties
import com.ef_softworks.pathfinder_server.consoleuser.ConsoleRole
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Date
import javax.crypto.SecretKey

/** What a token says about its holder; both the access and the refresh token carry the same claims. */
data class TokenClaims(
    val login: String,
    val role: ConsoleRole,
    val displayName: String,
)

/** Signs and verifies the HS256 tokens of the console. Expiry is part of the signature check. */
@Component
class JwtTokens(consoleAccessProperties: ConsoleAccessProperties) {

    private val signingKey: SecretKey = Keys.hmacShaKeyFor(consoleAccessProperties.tokenSecret.toByteArray(Charsets.UTF_8))

    fun issue(claims: TokenClaims, ttl: Duration): String {
        val now = Date()
        return Jwts.builder()
            .subject(claims.login)
            .claim(ROLE_CLAIM, claims.role.name)
            .claim(NAME_CLAIM, claims.displayName)
            .issuedAt(now)
            .expiration(Date(now.time + ttl.toMillis()))
            .signWith(signingKey)
            .compact()
    }

    /** The claims of a token with a valid signature that has not expired, otherwise null. */
    fun parse(token: String): TokenClaims? =
        try {
            val payload = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).payload
            val login = payload.subject ?: return null
            val role = payload.get(ROLE_CLAIM, String::class.java)?.let { ConsoleRole.valueOf(it) } ?: return null
            TokenClaims(login, role, payload.get(NAME_CLAIM, String::class.java) ?: login)
        } catch (e: Exception) {
            // Malformed, forged or expired: the caller treats every one of them as "no session"
            null
        }

    companion object {
        private const val ROLE_CLAIM = "role"
        private const val NAME_CLAIM = "name"
    }
}
